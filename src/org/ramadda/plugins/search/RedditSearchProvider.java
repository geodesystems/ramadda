/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.search;


import org.json.*;

import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.search.*;
import org.ramadda.repository.type.*;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JsonUtil;
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
public class RedditSearchProvider extends SearchProvider {


    /** _more_ */
    public static final String URL = "https://www.reddit.com/search.json";


    /** _more_ */
    public static final String SEARCH_ID = "reddit";

    /**
     * _more_
     *
     * @param repository _more_
     */
    public RedditSearchProvider(Repository repository) {
        super(repository, SEARCH_ID, "Reddit");
    }



    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public String getSiteUrl() {
        return "https://reddit.com/";
    }


    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public String getSearchProviderIconUrl() {
        return "${root}/search/reddit.png";
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
        String      q       = request.getString(ARG_TEXT, "").trim();
        String      url     = null;
        if (q.startsWith("/r")) {
            url = "https://www.reddit.com" + q + ".json";
        } else {
            url = HtmlUtils.url(URL, "q", q);
            url += "&limit=" + request.get(ARG_MAX, 100);
        }


        System.err.println(getName() + " search url:" + url);
        URLConnection connection = new URL(url).openConnection();
        connection.setRequestProperty("User-Agent", "ramadda");
        InputStream is   = connection.getInputStream();
        String      json = IOUtil.readContents(is);
        //        System.out.println("Json:" + json);
        JSONObject obj = new JSONObject(new JSONTokener(json));
        if ( !obj.has("data")) {
            System.err.println(
                "Reddit SearchProvider: no RelatedTopics field in json:"
                + json);

            return entries;
        }

        JSONArray   children    = JsonUtil.readArray(obj, "data.children");
        TypeHandler typeHandler = getRepository().getTypeHandler("link");
        Entry       parent      = getSynthTopLevelEntry();
        for (int i = 0; i < children.length(); i++) {
            JSONObject result  = children.getJSONObject(i);
            JSONObject data    = result.getJSONObject("data");

            long       created = 1000 * data.getLong("created_utc");
            Date       dttm    = new Date(created);
            String     author  = data.getString("author");
            String     sub     = data.getString("subreddit");
            String     name    = data.getString("title");
            String     desc    = data.optString("selftext_html", "");
            desc = desc.replaceAll("\\&lt;", "<").replaceAll("\\&gt;",
                                   ">").replaceAll("\\&#39;", "'");
            desc = HU.href("https://www.reddit.com/r/" + sub, "/r/" + sub,
                           "target=_news") + "<br>" + desc;
            String id = data.getString("id");
            String resultUrl = "https://www.reddit.com"
                               + data.getString("permalink");
            Entry newEntry = new Entry(Repository.ID_PREFIX_SYNTH + getId()
                                       + TypeHandler.ID_DELIMITER
                                       + id, typeHandler);
            entries.add(newEntry);
            newEntry.setIcon("/search/reddit.png");
            newEntry.initEntry(name, makeSnippet(desc), parent,
                               getUserManager().getLocalFileUser(),
                               new Resource(new URL(resultUrl)), "",
                               Entry.DEFAULT_ORDER, dttm.getTime(),
                               dttm.getTime(), dttm.getTime(),
                               dttm.getTime(), null);
            String thumbnail = data.optString("thumbnail");
            if (Utils.stringDefined(thumbnail)
                    && thumbnail.startsWith("https:")) {
                Metadata metadata =
                    new Metadata(getRepository().getGUID(), newEntry.getId(),
                                 getMetadataManager().findType(ContentMetadataHandler.TYPE_THUMBNAIL),
                                 false, thumbnail, "image", null, null, null);
                getMetadataManager().addMetadata(request,newEntry, metadata);
            }

            getEntryManager().cacheSynthEntry(newEntry);
        }

        return entries;
    }

}
