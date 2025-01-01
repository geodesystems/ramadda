/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.atlassian;


import org.json.*;

import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.search.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;

import org.ramadda.util.JsonUtil;
import org.ramadda.util.Utils;



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
 * Proxy that searches confluence
 *
 */
public class ConfluenceSearchProvider extends SearchProvider {

    /** _more_ */
    public static final String URL_SEARCH = "/rest/api/content/search";

    //    https://confluence.atlassian.com/rest/api/content/search?cql=title~bug&expand=history

    /** _more_ */
    private static final String ARG_CQL = "cql";


    /** _more_ */
    private static final String ARG_EXPAND = "expand";

    /** _more_ */
    private String baseUrl;


    /**
     * _more_
     *
     * @param repository _more_
     * @param args _more_
     */
    public ConfluenceSearchProvider(Repository repository,
                                    List<String> args) {
        super(repository, args.get(0), args.get(2));
        baseUrl = args.get(1);
    }

    /**
     * _more_
     *
     * @param repository _more_
     */
    public ConfluenceSearchProvider(Repository repository) {
        super(repository, "confluence", "Confluence Search");
    }


    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public String getSiteUrl() {
        return baseUrl;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public String getCategory() {
        return "Atlassian";
    }




    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public String getSearchProviderIconUrl() {
        return "${root}/atlassian/confluence.png";
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isEnabled() {
        return true;
        //        return getApiKey() != null;
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
        String url = HtmlUtils.url(baseUrl + URL_SEARCH, ARG_CQL,
                                   "text~" + request.getString(ARG_TEXT, ""),
                                   ARG_EXPAND, "history");
        System.err.println(getName() + " search url:" + url);
        InputStream is   = getInputStream(url);
        String      json = IOUtil.readContents(is);
        IOUtil.close(is);
        //        System.out.println("confluence json:" + json);
        JSONObject obj = new JSONObject(new JSONTokener(json));
        if ( !obj.has("results")) {
            System.err.println(
                "Confluence SearchProvider: no resultsfield in json:" + json);

            return entries;
        }


        JSONArray results = obj.getJSONArray("results");
        Entry     parent  = getSynthTopLevelEntry();
        TypeHandler typeHandler =
            getRepository().getTypeHandler("confluence_page");

        for (int i = 0; i < results.length(); i++) {
            JSONObject result   = results.getJSONObject(i);
            String     name     = JsonUtil.readValue(result, "title", "");
            String     id       = JsonUtil.readValue(result, "id", "");
            String     type     = JsonUtil.readValue(result, "type", "");
            JSONObject links    = result.getJSONObject("_links");
            String resultUrl    = baseUrl
                                  + JsonUtil.readValue(links, "webui", "");
            String     desc     = "";
            Date       dttm     = new Date();
            Date       fromDate = dttm;
            Date       toDate   = dttm;

            if (result.has("history")) {
                JSONObject history = result.getJSONObject("history");
                if (fromDate != null) {
                    dttm = fromDate;
                } else {
                    fromDate = dttm;
                }
                fromDate = Utils.parseDate(JsonUtil.readValue(history,
                        "createdDate", null));
                toDate = fromDate;
            }


            Entry newEntry = new Entry(Repository.ID_PREFIX_SYNTH + getId()
                                       + TypeHandler.ID_DELIMITER
                                       + id, typeHandler);
            entries.add(newEntry);

            Object[] values = typeHandler.makeEntryValues(null);
            //            values[0] = JsonUtil.readValue(fields, "resulttype.name", "");
            //            values[1] = JsonUtil.readValue(fields, "priority.name", "");

            /*
            String thumb = JsonUtil.readValue(snippet, "thumbnails.default.url",
                                          null);

            if (thumb != null) {
                Metadata thumbnailMetadata =
                    new Metadata(getRepository().getGUID(), newEntry.getId(),
                                 ContentMetadataHandler.TYPE_THUMBNAIL,
                                 false, thumb, null, null, null, null);
                                 getMetadataManager().addMetadata(newEntry, thumbnailMetadata);
            }
            */

            newEntry.initEntry(name, makeSnippet(desc, true), parent,
                               getUserManager().getLocalFileUser(),
                               new Resource(new URL(resultUrl)), "",
                               Entry.DEFAULT_ORDER, dttm.getTime(),
                               dttm.getTime(), fromDate.getTime(),
                               toDate.getTime(), values);
            getEntryManager().cacheSynthEntry(newEntry);
        }

        return entries;
    }



}
