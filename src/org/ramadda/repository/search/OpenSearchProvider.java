/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.search;

import org.json.*;

import org.ramadda.repository.*;
import org.ramadda.repository.search.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.AtomUtil;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;

import org.w3c.dom.*;

import ucar.unidata.util.DateUtil;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Proxy that searches google
 */
public class OpenSearchProvider extends SearchProvider {
    private String baseUrl;
    private String icon;

    public OpenSearchProvider(Repository repository, List<String> args) {
        super(repository, args.get(0), args.get(2));
        baseUrl = args.get(1);
        if (args.size() > 3) {
            icon = args.get(3);
        }
    }

    public OpenSearchProvider(Repository repository, String id, String url,
                              String name, String icon) {
        super(repository, id, name);
        this.baseUrl = url;
        this.icon    = icon;
    }

    @Override
    public String getCategory() {
        return CATEGORY_SCIENCE;
    }

    @Override
    public String getSearchProviderIconUrl() {
        return "${root}/favicon.png";
    }

    @Override
    public List<Entry> getEntries(Request request, org.ramadda.repository.util.SelectInfo searchInfo)
            throws Exception {

        List<Entry> entries = new ArrayList<Entry>();
        String      url     = baseUrl;
        url = url.replace("${searchterms}",
                          request.getString(ARG_TEXT, "").trim());
        //TODO:
        url = url.replace("${time:start}", "");
        url = url.replace("${time:end}", "");
        url = url.replace("${geo:box}", "");

        URLConnection connection = new URL(url).openConnection();
        connection.setRequestProperty("User-Agent", "ramadda");
        InputStream is  = connection.getInputStream();
        String      xml = IOUtil.readContents(is);
        System.out.println("xml:" + xml);

        Element     root        = XmlUtil.getRoot(xml);
        Entry       parent      = getSynthTopLevelEntry();
        TypeHandler typeHandler = getLinkTypeHandler();
        NodeList    children = XmlUtil.getElements(root, AtomUtil.TAG_ENTRY);
        for (int childIdx = 0; childIdx < children.getLength(); childIdx++) {
            Element item = (Element) children.item(childIdx);
            Element link = XmlUtil.findChild(item, AtomUtil.TAG_LINK);
            String name = XmlUtil.getGrandChildText(item, AtomUtil.TAG_TITLE,
                              "");
            String desc = XmlUtil.getGrandChildText(item,
                              AtomUtil.TAG_SUMMARY, "");
            String id = XmlUtil.getGrandChildText(item, AtomUtil.TAG_ID, "");
            Date   dttm     = new Date();
            Date   fromDate = null,
                   toDate   = null;
            String dateString;

            dateString = XmlUtil.getGrandChildText(item, "dc:date",
                    (String) null);
            if (Utils.stringDefined(dateString)) {
                List<String> toks = Utils.splitUpTo(dateString, "/", 2);
                fromDate = Utils.parseDate(toks.get(0));
                if (toks.size() > 1) {
                    toDate = Utils.parseDate(toks.get(1));
                } else {
                    toDate = fromDate;
                }
            }

            if (fromDate == null) {
                dateString = XmlUtil.getGrandChildText(item,
                        AtomUtil.TAG_TIME_START, (String) null);
                if (Utils.stringDefined(dateString)) {
                    fromDate = Utils.parseDate(dateString);
                }
            }

            if (toDate == null) {
                dateString = XmlUtil.getGrandChildText(item,
                        AtomUtil.TAG_TIME_END, (String) null);
                if (Utils.stringDefined(dateString)) {
                    toDate = Utils.parseDate(dateString);
                }
            }

            if (fromDate == null) {
                fromDate = dttm;
            }
            if (toDate == null) {
                toDate = dttm;
            }

            String itemUrl = XmlUtil.getAttribute(link, AtomUtil.ATTR_HREF,
                                 "");
            Entry newEntry = new Entry(Repository.ID_PREFIX_SYNTH + getId()
                                       + TypeHandler.ID_DELIMITER
                                       + id, typeHandler);

            if (icon != null) {
                newEntry.setIcon(icon);
            }
            entries.add(newEntry);

            newEntry.initEntry(name, desc, parent,
                               getUserManager().getLocalFileUser(),
                               new Resource(new URL(itemUrl)), "",
                               Entry.DEFAULT_ORDER, dttm.getTime(),
                               dttm.getTime(), fromDate.getTime(),
                               toDate.getTime(), null);

            initOpenSearchEntry(request, newEntry, item);

            getEntryManager().cacheSynthEntry(newEntry);
        }

        return entries;
    }

    public void initOpenSearchEntry(Request request, Entry entry,
                                    Element item)
            throws Exception {}

}
