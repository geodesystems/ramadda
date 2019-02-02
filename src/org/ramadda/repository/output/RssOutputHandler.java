/*
* Copyright (c) 2008-2019 Geode Systems LLC
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
*     http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.ramadda.repository.output;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;

import org.ramadda.util.RssUtil;
import org.ramadda.util.Utils;

import org.ramadda.util.sql.SqlUtil;


import org.w3c.dom.*;


import ucar.unidata.util.IOUtil;
import ucar.unidata.util.StringBufferCollection;

import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;


import java.io.*;

import java.io.File;



import java.net.*;



import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;



import java.util.regex.*;

import java.util.zip.*;


/**
 *
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
public class RssOutputHandler extends OutputHandler {




    /** _more_ */
    public static final String ICON_RSS = "/icons/rss.png";

    /** _more_ */
    public static String MIME_RSS = "application/rss+xml";


    /** _more_ */
    SimpleDateFormat rssSdf =
        new SimpleDateFormat("EEE dd, MMM yyyy HH:mm:ss Z");

    /** _more_ */
    public static final OutputType OUTPUT_RSS_FULL =
        new OutputType("Full RSS Feed", "rss.full", OutputType.TYPE_FEEDS,
                       "", ICON_RSS);

    /** _more_ */
    public static final OutputType OUTPUT_RSS_SUMMARY =
        new OutputType("RSS Feed", "rss.summary",
                       OutputType.TYPE_FEEDS | OutputType.TYPE_FORSEARCH, "",
                       ICON_RSS);


    /**
     * _more_
     *
     * @param repository _more_
     * @param element _more_
     * @throws Exception _more_
     */
    public RssOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        addType(OUTPUT_RSS_FULL);
        addType(OUTPUT_RSS_SUMMARY);
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param state _more_
     * @param links _more_
     *
     * @throws Exception _more_
     */
    public void getEntryLinks(Request request, State state, List<Link> links)
            throws Exception {

        if (state.getEntry() != null) {
            links.add(
                makeLink(
                    request, state.getEntry(), OUTPUT_RSS_FULL,
                    "/" + IOUtil.stripExtension(state.getEntry().getName())
                    + ".rss"));
        }
    }





    /**
     * _more_
     *
     * @param output _more_
     *
     * @return _more_
     */
    public String getMimeType(OutputType output) {
        if (output.equals(OUTPUT_RSS_FULL)
                || output.equals(OUTPUT_RSS_SUMMARY)) {
            return repository.getMimeTypeFromSuffix(".rss");
        } else {
            return super.getMimeType(output);
        }
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param outputType _more_
     * @param group _more_
     * @param subGroups _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputGroup(Request request, OutputType outputType,
                              Entry group, List<Entry> subGroups,
                              List<Entry> entries)
            throws Exception {
        entries.addAll(subGroups);

        return outputEntries(request, group, entries);
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param outputType _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputEntry(Request request, OutputType outputType,
                              Entry entry)
            throws Exception {
        List<Entry> entries = new ArrayList<Entry>();
        entries.add(entry);

        return outputEntries(request, entry, entries);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param parentEntry _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Result outputEntries(Request request, Entry parentEntry,
                                 List<Entry> entries)
            throws Exception {

        StringBuffer sb = new StringBuffer();
        sb.append(XmlUtil.XML_HEADER + "\n");
        sb.append(XmlUtil.openTag(RssUtil.TAG_RSS,
                                  XmlUtil.attrs(RssUtil.ATTR_VERSION, "2.0",
                                      RssUtil.ATTR_XMLNS_GEORSS,
                                      RssUtil.VALUE_XMLNS_GEORSS)));
        sb.append(XmlUtil.openTag(RssUtil.TAG_CHANNEL));
        sb.append(XmlUtil.tag(RssUtil.TAG_TITLE, "", parentEntry.getName()));
        StringBufferCollection sbc    = new StringBufferCollection();
        OutputType             output = request.getOutput();
        request.setMakeAbsoluteUrls(true);
        request.put(ARG_OUTPUT, OutputHandler.OUTPUT_HTML);
        for (Entry entry : entries) {
            StringBuffer extra    = new StringBuffer();
            String       resource = entry.getResource().getPath();
            if (Utils.isImage(resource)) {
                String imageUrl = request.getAbsoluteUrl(
                                      HtmlUtils.url(
                                          getRepository().URL_ENTRY_GET
                                          + entry.getId()
                                          + IOUtil.getFileExtension(
                                              resource), ARG_ENTRYID,
                                                  entry.getId()
                /*,                                                                    ARG_IMAGEWIDTH, "75"*/
                ));
                extra.append(HtmlUtils.br());
                extra.append(HtmlUtils.img(imageUrl));
            }

            sb.append(XmlUtil.openTag(RssUtil.TAG_ITEM));
            sb.append(
                XmlUtil.tag(
                    RssUtil.TAG_PUBDATE, "",
                    rssSdf.format(new Date(entry.getStartDate()))));
            sb.append(XmlUtil.tag(RssUtil.TAG_TITLE, "", entry.getName()));
            String url = request.makeUrl(repository.URL_ENTRY_SHOW,
                                         ARG_ENTRYID, entry.getId());
            sb.append(XmlUtil.tag(RssUtil.TAG_LINK, "", url));
            sb.append(XmlUtil.tag(RssUtil.TAG_GUID, "", url));

            sb.append(XmlUtil.openTag(RssUtil.TAG_DESCRIPTION, ""));
            String content;

            if (output.equals(OUTPUT_RSS_FULL)) {
                content = entry.getTypeHandler().getEntryContent(request,
                        entry, true, false).toString();
                content = content.replace("class=\"formlabel\"",
                                          "style=\" font-weight: bold;\"");
                content = content.replace("cellpadding=\"0\"",
                                          " cellpadding=\"5\" ");
                content = content.replace(
                    "class=\"formgroupheader\"",
                    "style=\"   background-color : #eee; border-bottom: 1px #ccc solid;    padding-left: 8px;   padding-top: 4px;   font-weight: bold;\"");
                content = getRepository().translate(request, content);
            } else {
                content = entry.getTypeHandler().getEntryText(entry) + extra;
            }
            XmlUtil.appendCdata(sb, content);

            sb.append(XmlUtil.closeTag(RssUtil.TAG_DESCRIPTION));
            if (entry.hasLocationDefined()) {
                sb.append(XmlUtil.tag(RssUtil.TAG_GEOLAT, "",
                                      "" + entry.getSouth()));
                sb.append(XmlUtil.tag(RssUtil.TAG_GEOLON, "",
                                      "" + entry.getEast()));
            } else if (entry.hasAreaDefined()) {
                //For now just include the southeast point
                sb.append(XmlUtil.tag(RssUtil.TAG_GEOBOX, "",
                                      entry.getSouth() + " "
                                      + entry.getWest() + " "
                                      + entry.getNorth() + " "
                                      + entry.getEast()));
            }


            sb.append(XmlUtil.closeTag(RssUtil.TAG_ITEM));
        }

        request.put(ARG_OUTPUT, output);
        sb.append(XmlUtil.closeTag(RssUtil.TAG_CHANNEL));
        sb.append(XmlUtil.closeTag(RssUtil.TAG_RSS));
        Result result = new Result("Query Results", sb,
                                   getMimeType(OUTPUT_RSS_SUMMARY));




        return result;

    }


}
