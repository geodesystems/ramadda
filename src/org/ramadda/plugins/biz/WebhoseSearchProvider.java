/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.biz;


import org.json.*;

import org.ramadda.repository.*;
import org.ramadda.repository.search.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JsonUtil;
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
 * Proxy that searches webhose
 *
 */
public class WebhoseSearchProvider extends SearchProvider {

    /** _more_ */
    private static final String ID = "webhose";

    /** _more_ */
    private static final String URL = "https://webhose.io/search";

    /** _more_ */

    private static final String ARG_TOKEN = "token";

    /** _more_ */
    private static final String ARG_Q = "q";



    /**
     * _more_
     *
     * @param repository _more_
     */
    public WebhoseSearchProvider(Repository repository) {
        super(repository, ID, "Webhose Search");
    }


    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public String getSiteUrl() {
        return "https://wehose.io";
    }

    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public String getSearchProviderIconUrl() {
        return "${root}/biz/webhose.png";
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

        String url = URL;
        //        int    max  = request.get(ARG_MAX, 100);
        //        int    skip = request.get(ARG_SKIP, 0);
        url = HtmlUtils.url(url, ARG_TOKEN, getApiKey(), ARG_Q, text);
        //        System.err.println(getName() + " search url:" + url);
        URLConnection connection = new URL(url).openConnection();
        connection.setRequestProperty("User-Agent", "ramadda");
        InputStream is   = connection.getInputStream();
        String      json = IOUtil.readContents(is);
        IOUtil.close(is);


        JSONObject obj = new JSONObject(new JSONTokener(json));
        if ( !obj.has("posts")) {
            System.err.println("Webhose: no posts field in json:" + json);

            return entries;
        }

        JSONArray   searchResults = obj.getJSONArray("posts");
        Entry       parent        = getSynthTopLevelEntry();
        TypeHandler typeHandler   = getRepository().getTypeHandler("link");

        for (int i = 0; i < searchResults.length(); i++) {
            JSONObject item    = searchResults.getJSONObject(i);
            String     id      = JsonUtil.readValue(item, "uuid", "");
            String     name    = item.getString("title");
            String     desc    = item.getString("text");
            String     itemUrl = item.getString("url");

            Date       dttm    = new Date();
            Date fromDate = Utils.parseDate(JsonUtil.readValue(item, "published",
                                null));
            Date  toDate   = fromDate;

            Entry newEntry = new Entry(makeSynthId(id), typeHandler);
            entries.add(newEntry);
            newEntry.setIcon("/biz/webhose.png");

            //TODO: add external_links, persons, locations, organizations

            /*
            if (thumb != null) {
                Metadata thumbnailMetadata =
                    new Metadata(getRepository().getGUID(), newEntry.getId(),
                                 ContentMetadataHandler.TYPE_THUMBNAIL,
                                 false, thumb, null, null, null, null);
                                 getMetadataManager().addMetadata(newEntry, thumbnailMetadata);
            }
            */

            newEntry.initEntry(name, desc, parent,
                               getUserManager().getLocalFileUser(),
                               new Resource(new URL(itemUrl)), "",
                               Entry.DEFAULT_ORDER, dttm.getTime(),
                               dttm.getTime(), fromDate.getTime(),
                               toDate.getTime(), null);
            getEntryManager().cacheSynthEntry(newEntry);
        }

        return entries;
    }



}
