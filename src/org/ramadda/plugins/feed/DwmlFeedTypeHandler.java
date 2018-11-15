/*
* Copyright (c) 2008-2018 Geode Systems LLC
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

package org.ramadda.plugins.feed;



import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;
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
public class DwmlFeedTypeHandler extends GenericTypeHandler {

    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public DwmlFeedTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
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
                dttm = DateUtil.parse(pubDate);
            }

            //Tue, 25 Jan 2011 05:00:00 GMT
            Resource resource = new Resource(link);
            entry.initEntry(title, desc, mainEntry, mainEntry.getUser(),
                            resource, "", dttm.getTime(), dttm.getTime(),
                            dttm.getTime(), dttm.getTime(), null);

            items.add(entry);
            getEntryManager().cacheSynthEntry(entry);
        }
    }




    public Result getHtmlDisplay(Request request, Entry entry)
            throws Exception {
        StringBuffer sb = new StringBuffer();
        String      url   = entry.getResource().getPath();
        if ((url == null) || (url.trim().length() == 0)) {
            sb.append("No URL defined");
            return new Result(msg("NWS Weather Forecast"), sb);
        }
        String xml = Utils.readUrl(url);
        Element root = XmlUtil.getRoot(xml);

        Element forecast = XmlUtil.findElement(XmlUtil.getElements(root,"data"), "type","forecast");
        if (forecast == null) {
            sb.append("No Forecast defined");
            return new Result(msg("NWS Weather Forecast"), sb);
        }
        sb.append("<table>");
        Hashtable<String,Element> times = new Hashtable<String,Element>();
        NodeList children = XmlUtil.getElements(forecast,"time-layout");
        for (int childIdx = 0; childIdx < children.getLength(); childIdx++) {
            Element timeNode = (Element) children.item(childIdx);
            String key = XmlUtil.getGrandChildText(timeNode,"layout-key","");
            times.put(key, timeNode);
        }
        Element params = XmlUtil.getElement(forecast,"parameters");
        if (params == null) {
            sb.append("No Parameters defined");
            return new Result(msg("NWS Weather Forecast"), sb);
        }
        Element icons = XmlUtil.getElement(params,"conditions-icon");
        if(icons!=null) {
            sb.append("<tr>");
            NodeList iconList = XmlUtil.getElements(forecast,"icon-link");
            for (int childIdx = 0; childIdx < iconList.getLength(); childIdx++) {
                Element icon = (Element) iconList.item(childIdx);
                sb.append("<td>");
                sb.append(HtmlUtils.img(XmlUtil.getChildText(icon)));
                sb.append("</td>");
            }
            sb.append("</tr>");
        }

        return new Result(msg("NWS Weather Forecast"), sb);

    }



}
