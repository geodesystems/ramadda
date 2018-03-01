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

package org.ramadda.plugins.biz;


import org.json.*;

import org.ramadda.repository.*;
import org.ramadda.repository.search.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Json;
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
 * Proxy that searches eia
 *
 */
public class EiaSearchProvider extends SearchProvider {

    /** _more_ */
    private static final String ID = "eia";

    /** _more_ */
    private static final String BASE_URL = "http://api.eia.gov/search/";


    /** _more_ */
    private static final String ARG_SEARCH_TERM = "search_term";

    /** _more_ */
    private static final String ARG_SEARCH_VALUE = "search_value";

    /** _more_ */
    private static final String ARG_PAGE_NUM = "page_num";

    /** _more_ */
    private static final String ARG_ROWS_PER_PAGE = "rows_per_page";


    /**
     * _more_
     *
     * @param repository _more_
     */
    public EiaSearchProvider(Repository repository) {
        super(repository, ID, "Energy Information Agency (EIA) Search");
    }


    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public String getSiteUrl() {
        return "http://www.eia.gov/beta/api/";
    }

    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public String getIconUrl() {
        return "${root}/biz/eia.png";
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
    public List<Entry> getEntries(Request request, SearchInfo searchInfo)
            throws Exception {

        String      text    = request.getString(ARG_TEXT, "");
        List<Entry> entries = new ArrayList<Entry>();
        if ( !Utils.stringDefined(text)) {
            return entries;
        }


        int max  = request.get(ARG_MAX, 100);
        int skip = request.get(ARG_SKIP, 0);
        String url = HtmlUtils.url(BASE_URL, ARG_SEARCH_TERM, "name",
                                   ARG_SEARCH_VALUE, text, ARG_ROWS_PER_PAGE,
                                   "" + max, ARG_PAGE_NUM,
                                   "" + ((int) (skip / max)));
        System.err.println(getName() + " search url:" + url);
        URLConnection connection = new URL(url).openConnection();
        connection.setRequestProperty("User-Agent", "ramadda");
        InputStream is   = connection.getInputStream();
        String      json = IOUtil.readContents(is);

        JSONObject  obj  = new JSONObject(new JSONTokener(json));
        if ( !obj.has("response")) {
            return entries;
        }


        JSONObject response = obj.getJSONObject("response");
        JSONArray  docs     = response.getJSONArray("docs");
        Entry      parent   = getSynthTopLevelEntry();
        TypeHandler typeHandler =
            getRepository().getTypeHandler("type_eia_series");
        for (int i = 0; i < docs.length(); i++) {
            JSONObject item      = docs.getJSONObject(i);
            String     id        = item.getString("series_id");
            String     name      = item.getString("name");
            String     units     = item.getString("units");
            String     frequency = item.getString("frequency");
            String     desc      = "";
            Date       dttm      = new Date();
            Date       fromDate  = dttm,
                       toDate    = dttm;
            String entryUrl = "http://www.eia.gov/beta/api/qb.cfm?sdid=" + id;
            Entry newEntry = new Entry(Repository.ID_PREFIX_SYNTH + getId()
                                       + ":" + id, typeHandler);
            Object[] values = typeHandler.makeEntryValues(null);
            values[EiaSeriesTypeHandler.IDX_SERIES_ID] = id;
            entries.add(newEntry);
            newEntry.initEntry(name, desc, parent,
                               getUserManager().getLocalFileUser(),
                               new Resource(new URL(entryUrl)), "",
                               dttm.getTime(), dttm.getTime(),
                               fromDate.getTime(), toDate.getTime(), values);
            getEntryManager().cacheSynthEntry(newEntry);
        }

        return entries;
    }



}
