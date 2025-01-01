/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.search;


import org.json.*;

import org.ramadda.repository.*;
import org.ramadda.repository.search.*;
import org.ramadda.repository.type.*;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;

import ucar.unidata.util.IOUtil;

import java.io.*;

import java.net.URL;
import java.net.URLConnection;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * Proxy that searches google
 *
 */
public class DuckDuckGoSearchProvider extends SearchProvider {


    /** _more_ */
    public static final String URL =
        "https://api.duckduckgo.com/?format=json";

    /** _more_ */
    public static final String SEARCH_ID = "duckduckgo";

    /**
     * _more_
     *
     * @param repository _more_
     */
    public DuckDuckGoSearchProvider(Repository repository) {
        super(repository, SEARCH_ID, "Duck Duck Go");
    }

    /**
     * _more_
     *
     * @param repository _more_
     * @param args _more_
     */
    public DuckDuckGoSearchProvider(Repository repository,
                                    List<String> args) {
        super(repository, SEARCH_ID, "Duck Duck Go");
    }



    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public String getSiteUrl() {
        return "https://duckduckgo.com/";
    }


    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public String getSearchProviderIconUrl() {
        return "${root}/search/duckduckgo.png";
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

        List<Entry> entries = new ArrayList<Entry>();
        String      url     = URL;
        url += "&";
        url += HtmlUtils.arg("q", request.getString(ARG_TEXT, ""));
        System.err.println(getName() + " search url:" + url);
        URLConnection connection = new URL(url).openConnection();
        //        connection.setRequestProperty("User-Agent","curl/7.37.1");
        connection.setRequestProperty("User-Agent", "ramadda");
        InputStream is   = connection.getInputStream();
        String      json = IOUtil.readContents(is);
        //        System.out.println("Json:" + json);
        JSONObject obj = new JSONObject(new JSONTokener(json));
        if ( !obj.has("RelatedTopics")) {
            System.err.println(
                "DuckDuckGo SearchProvider: no RelatedTopics field in json:"
                + json);

            return entries;
        }

        JSONArray   searchResults = obj.getJSONArray("RelatedTopics");
        TypeHandler typeHandler   = getRepository().getTypeHandler("link");
        Entry       parent        = getSynthTopLevelEntry();
        for (int i = 0; i < searchResults.length(); i++) {
            JSONObject result = searchResults.getJSONObject(i);
            if ( !result.has("Text")) {
                continue;
            }
            String name      = result.getString("Text");
            String desc      = result.getString("Result");
            String resultUrl = result.getString("FirstURL");
            Entry newEntry = new Entry(Repository.ID_PREFIX_SYNTH + getId()
                                       + TypeHandler.ID_DELIMITER
                                       + resultUrl, typeHandler);
            entries.add(newEntry);
            newEntry.setIcon("/search/duckduckgo.png");
            Date dttm = new Date();
            newEntry.initEntry(name, "<snippet>" + desc + "</snippet>",
                               parent, getUserManager().getLocalFileUser(),
                               new Resource(new URL(resultUrl)), "",
                               Entry.DEFAULT_ORDER, dttm.getTime(),
                               dttm.getTime(), dttm.getTime(),
                               dttm.getTime(), null);
            getEntryManager().cacheSynthEntry(newEntry);
        }

        return entries;
    }

    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        String url =
            "https://api.duckduckgo.com/?format=json&t=ramadda&q=zoom";
        String json = IOUtil.readContents(new URL(url));
        System.err.println(json);
    }


}
