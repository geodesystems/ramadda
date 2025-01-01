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
public class GoogleGraphSearchProvider extends SearchProvider {

    /** _more_ */
    public static final String URL =
        "https://kgsearch.googleapis.com/v1/entities:search";





    /** _more_ */
    public static final String ID = "google.graph";


    /**
     * _more_
     *
     * @param repository _more_
     */
    public GoogleGraphSearchProvider(Repository repository) {
        super(repository, ID, "Google Graph");
    }

    /**
     * _more_
     *
     * @param repository _more_
     * @param args _more_
     */
    public GoogleGraphSearchProvider(Repository repository,
                                     List<String> args) {
        super(repository, ID, "Google Graph");
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
        String url = HtmlUtils.url(URL, "limit",
                                   "" + getRequestLimit(request), "indent",
                                   "True", "query",
                                   request.getString(ARG_TEXT, ""), "key",
                                   getApiKey());

        System.out.println("google search url:" + url);
        String json = IOUtil.readContents(url);
        System.out.println("Json:" + json);
        JSONObject obj = new JSONObject(new JSONTokener(json));
        if ( !obj.has("itemListElement")) {
            System.err.println(
                "GoogleGraphSearchProvider: no itemListElement in json:"
                + json);

            return entries;
        }

        TypeHandler typeHandler = getRepository().getTypeHandler("link");
        Entry       parent      = getSynthTopLevelEntry();
        JSONArray   items       = obj.getJSONArray("itemListElement");
        for (int i = 0; i < items.length(); i++) {
            JSONObject    item     = items.getJSONObject(i);
            JSONObject    result   = item.getJSONObject("result");
            JSONArray     types    = result.getJSONArray("@type");

            String        lastType = types.getString(types.length() - 1);

            String        id       = result.getString("@id");
            StringBuilder desc     = new StringBuilder();
            desc.append(result.optString("description", ""));

            String name = result.optString("name", desc.toString());

            String ex   = " - ";

            String icon = null;
            for (int j = types.length() - 1; (icon == null) && (j >= 0);
                    j--) {
                String type = types.getString(j);
                String key  = "graph." + type + ".icon";
                icon = getRepository().getProperty(key, null);
                ex   += type + " ";
            }
            if (icon == null) {
                icon = "/search/google-icon.png";
            } else {
                ex = "";
            }
            ex = "";

            if (result.has("image")) {
                JSONObject img = result.getJSONObject("image");
                //                String label = dd.getString("articleBody");
                String curl = img.getString("contentUrl");
                String iurl = img.getString("url");
                if (desc.length() > 0) {
                    desc.append("<br>");
                }
                HtmlUtils.href(desc, iurl, HtmlUtils.img(curl));
            }

            if (result.has("detailedDescription")) {
                JSONObject dd = result.getJSONObject("detailedDescription");
                String     ddBody = dd.getString("articleBody");
                String     ddUrl  = dd.getString("url");
                if (desc.length() > 0) {
                    desc.append("<br>");
                }
                desc.append(ddBody);
                desc.append("<br>");
                HtmlUtils.href(desc, ddUrl, ddUrl);
            }



            String resultUrl = result.optString("url", null);
            Entry newEntry = new Entry(Repository.ID_PREFIX_SYNTH + getId()
                                       + TypeHandler.ID_DELIMITER
                                       + id, typeHandler);
            Resource resource = null;
            if (resultUrl != null) {
                resource = new Resource(new URL(resultUrl));
            } else {
                resource = new Resource();
            }
            entries.add(newEntry);




            newEntry.setIcon(icon);
            Date dttm = new Date();
            newEntry.initEntry(name + ex, desc.toString(), parent,
                               getUserManager().getLocalFileUser(), resource,
                               "", Entry.DEFAULT_ORDER, dttm.getTime(),
                               dttm.getTime(), dttm.getTime(),
                               dttm.getTime(), null);
            getEntryManager().cacheSynthEntry(newEntry);
        }

        return entries;
    }



}
