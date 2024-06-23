/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.biz;


import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.search.*;
import org.ramadda.repository.type.*;
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
 * Proxy that searches fred
 *
 */
public class FredSearchProvider extends SearchProvider {

    /** _more_ */
    private static final String ID = "fred";

    /**  */
    private static final String[] TAGS = { "frequency", "units",
                                           "seasonal_adjustment" };


    /** _more_ */
    private static final String URL =
        "https://api.stlouisfed.org/fred/series/search";



    /** _more_ */
    private static final String ARG_API_KEY = "api_key";

    /** _more_ */
    private static final String ARG_SEARCH_TEXT = "search_text";

    /** _more_ */
    private static final String ARG_LIMIT = "limit";

    /** _more_ */
    private static final String ARG_OFFSET = "offset";


    /**
     * _more_
     *
     * @param repository _more_
     */
    public FredSearchProvider(Repository repository) {
        super(repository, ID, "Federal Reserve Data Search");
    }


    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public String getSiteUrl() {
        return "https://api.stlouisfed.org/docs/fred/";
    }

    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public String getSearchProviderIconUrl() {
        return "${root}/biz/fred.png";
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isEnabled() {
        return getApiKey() != null;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param searchInfo _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public List<Entry> getEntries(Request request, org.ramadda.repository.util.SelectInfo searchInfo)
            throws Exception {

        String      text    = request.getString(ARG_TEXT, "");
        List<Entry> entries = new ArrayList<Entry>();
        if ( !Utils.stringDefined(text)) {
            return entries;
        }

        String url  = URL;
        int    max  = request.get(ARG_MAX, 100);
        int    skip = request.get(ARG_SKIP, 0);
        url = HtmlUtils.url(url, ARG_API_KEY, getApiKey(), ARG_SEARCH_TEXT,
                            text, ARG_LIMIT, "" + max, ARG_OFFSET, "" + skip);
        System.err.println(getName() + " search url:" + url);
        URLConnection connection = new URL(url).openConnection();
        connection.setRequestProperty("User-Agent", "ramadda");
        InputStream is  = connection.getInputStream();
        String      xml = IOUtil.readContents(is);
        //<series id="MSIM2" realtime_start="2013-08-14" realtime_end="2013-08-14" title="Monetary Services Index: M2 (preferred)" observation_start="1967-01-01" observation_end="2013-06-01" frequency="Monthly" frequency_short="M" units="Billions of Dollars" units_short="Bil. of $" seasonal_adjustment="Seasonally Adjusted" seasonal_adjustment_short="SA" last_updated="2013-07-12 11:01:06-05" popularity="52" notes="..."/>


        Element  root     = XmlUtil.getRoot(xml);
        NodeList children = XmlUtil.getElements(root, "series");
        Entry    parent   = getSynthTopLevelEntry();
        TypeHandler typeHandler =
            getRepository().getTypeHandler("type_fred_series");

        for (int childIdx = 0; childIdx < children.getLength(); childIdx++) {
            Element item = (Element) children.item(childIdx);
            String  name = XmlUtil.getAttribute(item, "title", "");
            String  desc = XmlUtil.getAttribute(item, "notes", "");
            String  id   = XmlUtil.getAttribute(item, "id", "");
            Date    dttm = null;


            if (dttm == null) {
                dttm = new Date();
            }

            Date fromDate = dttm,
                 toDate   = dttm;
            String start = XmlUtil.getAttribute(item, "observation_start",
                               (String) null);
            String end = XmlUtil.getAttribute(item, "observation_end",
                             (String) null);
            if (start != null) {
                fromDate = Utils.parseDate(start);
            }
            if (end != null) {
                toDate = Utils.parseDate(end);
            }
            String entryUrl = "https://research.stlouisfed.org/fred2/series/"
                              + id;
            Entry newEntry = new Entry(Repository.ID_PREFIX_SYNTH + getId()
                                       + ":" + id, typeHandler);
            Object[] values = typeHandler.makeEntryValues(null);
            values[FredSeriesTypeHandler.IDX_SERIES_ID] = id;
            entries.add(newEntry);
            newEntry.initEntry(name, makeSnippet(desc), parent,
                               getUserManager().getLocalFileUser(),
                               new Resource(new URL(entryUrl)), "",
                               Entry.DEFAULT_ORDER, dttm.getTime(),
                               dttm.getTime(), fromDate.getTime(),
                               toDate.getTime(), values);
            for (String attr : TAGS) {
                String v = XmlUtil.getAttribute(item, attr, (String) null);
                if (v != null) {
                    Metadata metadata =
                        new Metadata(getRepository().getGUID(),
                                     newEntry.getId(), getMetadataManager().findType("property"),
				     false,
                                     attr, v, null, null, null);
                    getMetadataManager().addMetadata(request,newEntry, metadata);
                }

            }



            getEntryManager().cacheSynthEntry(newEntry);
        }

        return entries;
    }



}
