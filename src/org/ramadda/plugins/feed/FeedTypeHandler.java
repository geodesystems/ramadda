/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.feed;



import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;
import org.ramadda.repository.util.SelectInfo;

import org.ramadda.util.AtomUtil;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.RssUtil;
import org.ramadda.util.Utils;


import org.w3c.dom.*;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.net.URL;

import java.text.SimpleDateFormat;


import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;


/**
 */
@SuppressWarnings("unchecked")
public class FeedTypeHandler extends ExtensibleGroupTypeHandler {

    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public FeedTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }



    public void initializeEntryFromForm(Request request, Entry entry,
                                        Entry parent, boolean newEntry)
	throws Exception {
        super.initializeEntryFromForm(request, entry, parent, newEntry);
	if(!newEntry) return;
        String      url   = entry.getResource().getPath();
        if (Utils.stringDefined(entry.getName()) || !stringDefined(url))  return;
	Element root = readRoot(entry);
	if(root==null) return;

        if (root.getTagName().equals(RssUtil.TAG_RSS)) {
	    Element channel = XmlUtil.getElement(root, RssUtil.TAG_CHANNEL);
	    if(channel!=null) {
		String title = XmlUtil.getGrandChildText(channel, RssUtil.TAG_TITLE,
						     "");
		entry.setName(title);
	    }
        } else if (root.getTagName().equals(AtomUtil.TAG_FEED)) {
	    //TODO
	}
    }





    /**
     * _more_
     *
     * @param request _more_
     * @param mainEntry _more_
     * @param parentEntry _more_
     * @param synthId _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public List<String> getSynthIds(Request request, SelectInfo select, Entry mainEntry,
                                    Entry parentEntry, String synthId)
            throws Exception {
        List<String> ids = mainEntry.getChildIds();
        if (ids != null) {
            return ids;
        }
        ids = new ArrayList<String>();
        if (synthId != null) {
            return ids;
        }

        for (Entry item : getFeedEntries(request, mainEntry)) {
            ids.add(item.getId());
        }
        mainEntry.setChildIds(ids);

        return ids;
    }



    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public TypeHandler getTypeHandlerForCopy(Entry entry) throws Exception {
        if ( !getEntryManager().isSynthEntry(entry.getId())) {
            return getRepository().getTypeHandler(TypeHandler.TYPE_GROUP);
        }

        return getRepository().getTypeHandler(TypeHandler.TYPE_FILE);
    }

    /*
<title>Heading west</title>
    <link>http://scripting.com/stories/2011/01/25/headingWest.html</link>
    <guid>http://scripting.com/stories/2011/01/25/headingWest.html</guid>
    <comments>http://scripting.com/stories/2011/01/25/headingWest.html#disqus_thread</comments>
      <description>
    </description>
        <pubDate>Tue, 25 Jan 2011 14:26:27 GMT</pubDate>
</item>
    */



    /**
     * _more_
     *
     * @param request _more_
     * @param mainEntry _more_
     * @param items _more_
     * @param root _more_
     *
     * @throws Exception _more_
     */
    public void processRss(Request request, Entry mainEntry,
                           List<Entry> items, Element root)
            throws Exception {
        //        Thu, 14 Jun 2012 14:50:14 -05:00
        SimpleDateFormat[] sdfs =
            new SimpleDateFormat[] {
                new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz"),
                new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z"), };
        Element channel = XmlUtil.getElement(root, RssUtil.TAG_CHANNEL);
        if (channel == null) {
            throw new IllegalArgumentException("No channel tag");
        }
        NodeList children = XmlUtil.getElements(channel, RssUtil.TAG_ITEM);
        HashSet  seen     = new HashSet();
        for (int childIdx = 0; childIdx < children.getLength(); childIdx++) {
            Element item = (Element) children.item(childIdx);
            String title = XmlUtil.getGrandChildText(item, RssUtil.TAG_TITLE,
                               "");

            String link = XmlUtil.getGrandChildText(item, RssUtil.TAG_LINK,
                              "");



            String guid = XmlUtil.getGrandChildText(item, RssUtil.TAG_GUID,
                              link);
            if (seen.contains(guid)) {
                continue;
            }

            seen.add(guid);
            String desc = XmlUtil.getGrandChildText(item,
                              RssUtil.TAG_DESCRIPTION, "");



            String pubDate = XmlUtil.getGrandChildText(item,
                                 RssUtil.TAG_PUBDATE, "").trim();

            Entry entry =
                new Entry(getEntryManager().createSynthId(mainEntry, guid),
                          getRepository().getTypeHandler("link"), false);
            entry.setMasterTypeHandler(this);
            Date dttm = new Date();
            for (SimpleDateFormat sdf : sdfs) {
                try {
                    dttm = sdf.parse(pubDate);

                    break;
                } catch (Exception exc) {}
            }

            if (dttm == null) {
                dttm = Utils.parseDate(pubDate);
            }


            setLocation(item, entry);


            //Tue, 25 Jan 2011 05:00:00 GMT
            Resource resource = new Resource(link);
            entry.initEntry(title, desc, mainEntry, mainEntry.getUser(),
                            resource, "", Entry.DEFAULT_ORDER,
                            dttm.getTime(), dttm.getTime(), dttm.getTime(),
                            dttm.getTime(), null);

            items.add(entry);
            getEntryManager().cacheSynthEntry(entry);
        }
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param mainEntry _more_
     * @param items _more_
     * @param root _more_
     *
     * @throws Exception _more_
     */
    public void processAtom(Request request, Entry mainEntry,
                            List<Entry> items, Element root)
            throws Exception {
        //        Thu, 14 Jun 2012 14:50:14 -05:00
        SimpleDateFormat[] sdfs =
            new SimpleDateFormat[] {
                new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz"),
                new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z"), };
        NodeList children = XmlUtil.getElements(root, AtomUtil.TAG_ENTRY);
        HashSet  seen     = new HashSet();
        for (int childIdx = 0; childIdx < children.getLength(); childIdx++) {
            Element item = (Element) children.item(childIdx);
            String title = XmlUtil.getGrandChildText(item,
                               AtomUtil.TAG_TITLE, "");
            String guid = XmlUtil.getGrandChildText(item, AtomUtil.TAG_ID,
                              "" + childIdx);
            if (seen.contains(guid)) {
                continue;
            }
            seen.add(guid);
            String desc = XmlUtil.getGrandChildText(item,
                              AtomUtil.TAG_CONTENT, null);

            if (desc == null) {
                desc = XmlUtil.getGrandChildText(item, AtomUtil.TAG_SUMMARY,
                        "");
            }

            String pubDate = XmlUtil.getGrandChildText(item,
                                 AtomUtil.TAG_PUBLISHED, "").trim();

            if ( !Utils.stringDefined(pubDate)) {
                pubDate = XmlUtil.getGrandChildText(item,
                        AtomUtil.TAG_UPDATED, "").trim();

            }


            /**
             *   NodeList categories = XmlUtil.getElements(item, AtomUtil.TAG_CATEGORY);
             *   for (int childIdx = 0; childIdx < categories.getLength(); childIdx++) {
             *        Element cat= (Element) categories.item(childIdx);
             *   }
             */


            Entry entry =
                new Entry(getEntryManager().createSynthId(mainEntry, guid),
                          getRepository().getTypeHandler("link"), false);
            entry.setMasterTypeHandler(this);

            Date dttm       = null;
            Date changeDate = null;
            for (SimpleDateFormat sdf : sdfs) {
                try {
                    //                    dttm = sdf.parse(pubDate);
                    break;
                } catch (Exception exc) {}
            }


            if (dttm == null) {
                dttm = Utils.parseDate(pubDate);
            }

            setLocation(item, entry);
            String resourcePath = XmlUtil.getGrandChildText(item,
                                      "feedburner:origLink", "");
            NodeList links = XmlUtil.getElements(item, AtomUtil.TAG_LINK);
            for (int linkIdx = 0; linkIdx < links.getLength(); linkIdx++) {
                Element link = (Element) links.item(linkIdx);
                if (XmlUtil.getAttribute(link, AtomUtil.ATTR_TYPE,
                                         "").equals("text/html")) {
                    resourcePath = XmlUtil.getAttribute(link,
                            AtomUtil.ATTR_HREF, "");

                    break;
                }
            }

            Resource resource = new Resource(resourcePath);
            entry.initEntry(title, desc, mainEntry, mainEntry.getUser(),
                            resource, "", Entry.DEFAULT_ORDER,
                            dttm.getTime(), dttm.getTime(), dttm.getTime(),
                            dttm.getTime(), null);

            items.add(entry);
            getEntryManager().cacheSynthEntry(entry);
        }
    }


    /**
     * _more_
     *
     * @param item _more_
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    private void setLocation(Element item, Entry entry) throws Exception {

        String lat = XmlUtil.getGrandChildText(item, RssUtil.TAG_GEOLAT,
                         "").trim();
        if (lat.length() == 0) {
            lat = XmlUtil.getGrandChildText(item, "lat", "").trim();
        }
        String lon = XmlUtil.getGrandChildText(item, RssUtil.TAG_GEOLON,
                         "").trim();
        if (lon.length() == 0) {
            lon = XmlUtil.getGrandChildText(item, "long", "").trim();
        }


        if ( !Utils.stringDefined(lat)) {
            String point = XmlUtil.getGrandChildText(item,
                               RssUtil.TAG_GEORSS_POINT, "").trim();

            if (Utils.stringDefined(point)) {
                List<String> toks = StringUtil.split(point, " ", true, true);
                if (toks.size() == 2) {
                    lat = toks.get(0);
                    lon = toks.get(1);
                }
            }
        }


        if ( !Utils.stringDefined(lat)) {
            String polygon = XmlUtil.getGrandChildText(item,
                                 RssUtil.TAG_GEORSS_POLYGON, "").trim();
            if (Utils.stringDefined(polygon)) {
                StringBuffer[] sb = new StringBuffer[] { new StringBuffer(),
                        new StringBuffer(), new StringBuffer(),
                        new StringBuffer() };

                List<String> toks = StringUtil.split(polygon, " ", true,
                                        true);
                int    idx = 0;
                double
                    north  = -90,
                    west   = 180,
                    south  = 90,
                    east   = -180;
                for (int i = 0; i < toks.size(); i += 2) {
                    double latv = Double.parseDouble(toks.get(i));
                    double lonv = Double.parseDouble(toks.get(i + 1));
                    north = Math.max(north, latv);
                    south = Math.min(south, latv);
                    west  = Math.min(west, lonv);
                    east  = Math.max(east, lonv);
                    String toAdd = latv + "," + lonv + ";";
                    if ((sb[idx].length() + toAdd.length())
                            >= (Metadata.MAX_LENGTH - 100)) {
                        idx++;
                        if (idx >= sb.length) {
                            break;
                        }
                    }
                    sb[idx].append(toAdd);
                    //TODO: add a closing point???
                }
                //For now don't add the polygon.
                Metadata polygonMetadata =
                    new Metadata(getRepository().getGUID(), entry.getId(),
                                 getMetadataManager().findType(MetadataHandler.TYPE_SPATIAL_POLYGON),
                                 DFLT_INHERITED, sb[0].toString(),
                                 sb[1].toString(), sb[2].toString(),
                                 sb[3].toString(), Metadata.DFLT_EXTRA);
                //                getMetadataManager().addMetadata(entry, polygonMetadata, false);
                entry.setNorth(north);
                entry.setWest(west);
                entry.setSouth(south);
                entry.setEast(east);
            }
        }




        String elev = XmlUtil.getGrandChildText(item,
                          RssUtil.TAG_GEORSS_ELEV, "").trim();


        if (Utils.stringDefined(elev)) {
            entry.setAltitude(Double.parseDouble(elev));
        }

        if ((lat.length() > 0) && (lon.length() > 0)) {
            entry.setLocation(Double.parseDouble(lat),
                              Double.parseDouble(lon), 0);
        }

    }


    private Element readRoot(Entry entry) throws Exception {
        String      url   = entry.getResource().getPath();
        if ((url == null) || (url.trim().length() == 0)) {
            return null;
        }

        String  xml = "";
        Element root;
        try {
            xml = Utils.readUrl(url);
        } catch (Exception firstExc) {
            //Sleep a bit then try again
            ucar.unidata.util.Misc.sleepSeconds(3);
            try {
                xml = IOUtil.readContents(url, getClass());
            } catch (Exception secondExc) {
                logError("Error reading feed:" + url + " xml:" + xml,
                         secondExc);

                return null;
            }
        }
	return XmlUtil.getRoot(xml);
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param mainEntry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Entry> getFeedEntries(Request request, Entry mainEntry)
            throws Exception {
        List<Entry> items = new ArrayList<Entry>();
        Element root = readRoot(mainEntry);
        if (root == null) {
            return items;
        }
        if (root.getTagName().equals(RssUtil.TAG_RSS)) {
            processRss(request, mainEntry, items, root);
        } else if (root.getTagName().equals(AtomUtil.TAG_FEED)) {
            processAtom(request, mainEntry, items, root);
        } else {
            throw new IllegalArgumentException("Unknown feed type:"
                    + root.getTagName());
            //            getRepository().getLogManager().logError("Unknown feed type:" + root.getTagName()); 
        }

        return items;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param mainEntry _more_
     * @param id _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public Entry makeSynthEntry(Request request, Entry mainEntry, String id)
            throws Exception {
        id = getEntryManager().createSynthId(mainEntry, id);
        for (Entry item : getFeedEntries(request, mainEntry)) {
            if (item.getId().equals(id)) {
                return item;
            }
        }

        return null;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isSynthType() {
        return true;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public String getEntryIconUrl(Request request, Entry entry)
            throws Exception {
        return getIconUrl("/feed/blog_icon.png");
    }

    /**
     * _more_
     *
     * @param id _more_
     *
     * @return _more_
     */
    @Override
    public Entry createEntry(String id) {
        return new Entry(id, this, true);
    }


}
