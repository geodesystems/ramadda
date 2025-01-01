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

import java.net.URL;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * Proxy that searches google
 *
 */
public class GoogleImageSearchProvider extends SearchProvider {

    /** _more_ */
    public static final String URL =
        "https://ajax.googleapis.com/ajax/services/search/images?v=1.0";

    /** _more_ */
    public static final String SEARCH_ID = "googleimage";


    /**
     * _more_
     *
     * @param repository _more_
     */
    public GoogleImageSearchProvider(Repository repository) {
        super(repository, SEARCH_ID, "Google Image");
    }

    /**
     * _more_
     *
     * @param repository _more_
     * @param args _more_
     */
    public GoogleImageSearchProvider(Repository repository,
                                     List<String> args) {
        super(repository, SEARCH_ID, "Google Image");
    }



    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public String getSiteUrl() {
        return "http://www.google.com";
    }


    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public String getSearchProviderIconUrl() {
        return "${root}/search/google-icon.png";
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
        //        System.err.println("google search url:" + url);
        String json = IOUtil.readContents(url);
        //        System.err.println("Json:" + json);
        JSONObject obj = new JSONObject(new JSONTokener(json));
        if ( !obj.has("responseData")) {
            System.err.println(
                "GoogleImageSearchProvider: no response field in json:"
                + json);

            return entries;
        }

        JSONObject  response      = obj.getJSONObject("responseData");
        JSONArray   searchResults = response.getJSONArray("results");
        TypeHandler typeHandler   = getRepository().getTypeHandler("link");
        Entry       parent        = getSynthTopLevelEntry();
        for (int i = 0; i < searchResults.length(); i++) {
            JSONObject result    = searchResults.getJSONObject(i);
            String     name      = result.getString("titleNoFormatting");
            String     desc      = result.getString("content");
            String     resultUrl = result.getString("url");
            Entry newEntry = new Entry(Repository.ID_PREFIX_SYNTH + getId()
                                       + TypeHandler.ID_DELIMITER
                                       + resultUrl, typeHandler);
            entries.add(newEntry);
            newEntry.setIcon("/search/google-icon.png");
            Date dttm = new Date();
            newEntry.initEntry(name, desc, parent,
                               getUserManager().getLocalFileUser(),
                               new Resource(new URL(resultUrl)), "",
                               Entry.DEFAULT_ORDER, dttm.getTime(),
                               dttm.getTime(), dttm.getTime(),
                               dttm.getTime(), null);
            getEntryManager().cacheSynthEntry(newEntry);
        }

        return entries;
    }



}
