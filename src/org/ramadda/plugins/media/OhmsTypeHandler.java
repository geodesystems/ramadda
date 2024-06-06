/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.media;


import org.json.*;

import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.Utils;

import org.ramadda.util.WikiUtil;
import org.ramadda.util.geo.Bounds;

import org.w3c.dom.*;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;
import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.net.URL;


import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;


/**
 */
public class OhmsTypeHandler extends MediaTypeHandler {

    /**  */
    private static int IDX = MediaTypeHandler.IDX_LAST+1;

    /**  */
    public static final int IDX_TYPE = IDX++;

    /**  */
    public static final int IDX_INTERVIEWEE = IDX++;

    /**  */
    public static final int IDX_INTERVIEWER = IDX++;

    /**  */
    public static final int IDX_COLLECTION = IDX++;

    /**  */
    public static final int IDX_SERIES = IDX++;

    /**  */
    public static final int IDX_REPOSITORY = IDX++;

    /**  */
    public static final int IDX_REPOSITORY_URL = IDX++;

    /**  */
    public static final int IDX_RIGHTS = IDX++;

    /**  */
    public static final int IDX_USAGE = IDX++;

    /**  */
    public static final int IDX_MEDIA_TYPE = IDX++;

    /**  */
    public static final int IDX_MEDIA_URL = IDX++;

    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public OhmsTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }

    /**
     *
     * @param entry _more_
      * @return _more_
     *
     * @throws Exception _more_
     */
    private Element getRoot(Entry entry) throws Exception {
        InputStream fis = getStorageManager().getFileInputStream(
                              entry.getResource().getPath());
        String  xml  = IOUtil.readContents(fis);
        Element root = XmlUtil.getRoot(xml);

        return root;
    }


    /**
     *
     * @param request _more_
     * @param entry _more_
     * @param fromImport _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void initializeNewEntry(Request request, Entry entry,NewType newType)
            throws Exception {

        super.initializeNewEntry(request, entry, newType);
	if(!isNew(newType)) return;
        Element root   = getRoot(entry);
        Element record = XmlUtil.findChild(root, "record");
        //<title>SAMPLE 001: Interview with Georgia Davis Powers, April 26, 2013</title>
        String title = XmlUtil.getGrandChildText(record, "title", null);
	entry.setName(title);
        //<date format="yyyy-mm-dd" value="2013-05-29"/>
        Element date = XmlUtil.findChild(record, "date");
        String  dt   = XmlUtil.getAttribute(date, "value");
        String  fmt  = XmlUtil.getAttribute(date, "format");
        //Fix the format
        fmt = fmt.replaceAll("m", "M");
        Date dttm = null;

        if (dt.length() == 4) {
            dttm = new SimpleDateFormat("yyyy").parse(dt);
        } else {
            dttm = new SimpleDateFormat(fmt).parse(dt);
        }

        entry.setStartDate(dttm.getTime());
        entry.setEndDate(dttm.getTime());

        String[] props = new String[] {
            "interviewee", "interviewer", "collection_name", "series_name",
            "repository", "repository_url"
        };

        String type = XmlUtil.getGrandChildText(record, "type", null);
        if (type != null) {
            entry.setValue(IDX_TYPE, type);
        }
        if ( !Utils.stringDefined(entry.getDescription())) {
            entry.setDescription(XmlUtil.getGrandChildText(record,
                    "description", ""));
        }
        entry.setValue(IDX_RIGHTS,
                       XmlUtil.getGrandChildText(record, "rights", ""));
        entry.setValue(IDX_USAGE,
                       XmlUtil.getGrandChildText(record, "usage", ""));

        for (int i = 0; i < props.length; i++) {
            String v = XmlUtil.getGrandChildText(record, props[i], null);
            if (v != null) {
                entry.setValue(IDX_INTERVIEWEE + i, v);
            }
        }

        addProperties(request,entry, root, "gps_text", "content.location");
        addProperties(request,entry, root, "keywords",
                      ContentMetadataHandler.TYPE_KEYWORD);
        addProperties(request,entry, root, "subjects", "content.subject");

        Bounds bounds = null;
        for (Object o : XmlUtil.findDescendants(root, "gps")) {
            Element gps = (Element) o;
            String  t   = XmlUtil.getChildText(gps);
            if ( !Utils.stringDefined(t)) {
                continue;
            }
            List<String> toks = StringUtil.splitUpTo(t, ",", 2);
            if (toks.size() != 2) {
                continue;
            }
            double lat = Double.parseDouble(toks.get(0));
            double lon = Double.parseDouble(toks.get(1));
            if (bounds == null) {
                bounds = new Bounds();
            }
            bounds.expand(lat, lon);
        }
        if (bounds != null) {
            entry.setBounds(bounds);
        }

        Element media   = XmlUtil.findChild(record, "mediafile");
        String  host    = XmlUtil.getGrandChildText(media, "host", "");
        String mediaUrl = XmlUtil.getGrandChildText(record, "media_url",
                              null);

        entry.setValue(IDX_MEDIA_TYPE, host);
        if (host.equals("YouTube")) {
            String youTubeId = YouTubeVideoTypeHandler.getYouTubeId(mediaUrl);
            YouTubeVideoTypeHandler.addThumbnail(getRepository(), request,
                    entry, youTubeId);
        } else if (host.equals("Vimeo")) {
            String embed = XmlUtil.getGrandChildText(record, "kembed", null);
            if (Utils.stringDefined(embed)) {
                String[] toks = Utils.findPatterns(embed,
                                    "player.vimeo.com/video/([0-9]+)");
                if (toks != null) {
                    String id = toks[0];
		    if(!Utils.stringDefined(mediaUrl)) {
			mediaUrl = "https://player.vimeo.com/video/" + id;
		    }
                    String jsonUrl =
                        "https://vimeo.com/api/oembed.json?url=https://vimeo.com/"
                        + toks[0];
                    JSONObject json =
                        new JSONObject(IOUtil.readContents(jsonUrl,
                            getClass()));
                    if (json.has("thumbnail_url")) {
                        String url = json.getString("thumbnail_url");
                        getMetadataManager().addThumbnailUrl(request, entry,
                                url, "thumnail.jpg");
                    }
                }
            }
        } else if(host.equals("SoundCloud") && mediaUrl!=null) {
	    //For now use this client_id since SC isn't accepting new app registrations now
	    try {
		String client_id="Iy5e1Ri4GTNgrafaXe4mLpmJLXbXEfBR";
		String jsonUrl =
		    "https://api-widget.soundcloud.com/resolve?format=json&url=" + mediaUrl+"&client_id="+client_id;
		String jsons = IOUtil.readContents(jsonUrl, getClass());
		JSONObject json =
		    new JSONObject(jsons);

		String thumbnail =  json.optString("artwork_url",null);
		
		if(!Utils.stringDefined(thumbnail)) {
		    thumbnail = JsonUtil.readValue(json,"user.avatar_url",null);
		}
		
		if(Utils.stringDefined(thumbnail)) {
		    getMetadataManager().addThumbnailUrl(request, entry,
							 thumbnail, "thumnail.jpg");
		}
	    } catch(Exception exc) {
		System.err.println("Err:" + exc);
	    }
	} else {
	    /**
	       Don't do this for now
	    getMetadataManager().addThumbnailUrl(request, entry,
						 Utils.getInputStream("/org/ramadda/plugins/media/htdocs/media/audio.png", getClass()),
						 "thumnail.png");
	    */
	}


	entry.setValue(IDX_MEDIA_URL, mediaUrl);

    }

    @Override
    public boolean canAddTranscription(Request request, Entry entry) {
	return false;
    }



    @Override
    public String getTranscriptions(Request request, Entry entry) throws Exception {
        Element       root   = getRoot(entry);
        Element       record = XmlUtil.findChild(root, "record");
        Element       media  = XmlUtil.findChild(record, "mediafile");
        String mediaType   = XmlUtil.getGrandChildText(media, "host", "");
        Element       index  = XmlUtil.findChild(record, "index");
        List<String>  points = new ArrayList<String>();
        if (index != null) {
            for (Object o : XmlUtil.findDescendants(index, "point")) {
                Element point = (Element) o;
                String  time  = XmlUtil.getGrandChildText(point, "time",
                                    null);
                if (time == null) {
                    continue;
                }
                String title = XmlUtil.getGrandChildText(point, "title", "");
                List attrs =
                    JsonUtil.quoteList(
                        Utils.makeListFromValues(
                            "time", time, "title", title, "transcript",
                            XmlUtil.getGrandChildText(
                                point, "partial_transcript", ""), "synopsis",
                                    XmlUtil.getGrandChildText(
							      point, "synopsis", "")));
		Utils.add(attrs,"keywords",
			    JU.quoteAll(Utils.split(XmlUtil.getGrandChildText(
								  point, "keywords",
								  ""), ";",true,true)));
		Utils.add(attrs, "subjects",
			  JU.quoteAll(Utils.split(XmlUtil.getGrandChildText(
									point, "subjects",
									""),";",true,true)));
                Utils.add(points, JsonUtil.map(attrs));
            }
        }

	return JU.list(points);
    }


    /**
     *
     * @param entry _more_
     * @param root _more_
     * @param prop _more_
     * @param metadata _more_
     *
     * @throws Exception _more_
     */
    private void addProperties(Request request,Entry entry, Element root, String prop,
                               String metadataType)
            throws Exception {
        HashSet<String> seen     = new HashSet<String>();
        List            keywords = XmlUtil.findDescendants(root, prop);
        for (Object o : keywords) {
            Element k    = (Element) o;
            String  text = XmlUtil.getChildText(k);
            for (String word : Utils.split(text, ";", true, true)) {
                if (seen.contains(word)) {
                    continue;
                }
                seen.add(word);
                getMetadataManager().addMetadata(request,entry, metadataType, true, word);
            }
        }
    }


    /**
     * _more_
     *
     * @param wikiUtil _more_
     * @param request _more_
     * @param originalEntry _more_
     * @param entry _more_
     * @param tag _more_
     * @param props _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props)
            throws Exception {

        if ( !tag.equals("ohms_viewer")) {
            return super.getWikiInclude(wikiUtil, request, originalEntry,
                                        entry, tag, props);
        }
        if ( !getAccessManager().canDownload(request, entry)) {
            return getPageHandler().showAccessRestricted(entry);
        }


        Element       root   = getRoot(entry);
        Element       record = XmlUtil.findChild(root, "record");
        Element       media  = XmlUtil.findChild(record, "mediafile");
        String mediaType   = XmlUtil.getGrandChildText(media, "host", "");
        Element       index  = XmlUtil.findChild(record, "index");
        List<String>  points = new ArrayList<String>();
        if (index != null) {
            for (Object o : XmlUtil.findDescendants(index, "point")) {
                Element point = (Element) o;
                String  time  = XmlUtil.getGrandChildText(point, "time",
                                    null);
                if (time == null) {
                    continue;
                }
                String title = XmlUtil.getGrandChildText(point, "title", "");
                List attrs =
                    JsonUtil.quoteList(
                        Utils.makeListFromValues(
                            "time", time, "title", title, "transcript",
                            XmlUtil.getGrandChildText(
                                point, "partial_transcript", ""), "synopsis",
                                    XmlUtil.getGrandChildText(
                                        point, "synopsis", ""), "keywords",
                                            XmlUtil.getGrandChildText(
                                                point, "keywords",
                                                ""), "subjects",
                                                    XmlUtil.getGrandChildText(
                                                        point, "subjects",
                                                        "")));
                Utils.add(points, JsonUtil.map(attrs));

            }
        }

        String mediaUrl = XmlUtil.getGrandChildText(record, "media_url",  null);
        String embed     = XmlUtil.getGrandChildText(record, "kembed", null);
	return  addMedia(request, entry, props, mediaType,  embed, mediaUrl,  points);
    }



}
