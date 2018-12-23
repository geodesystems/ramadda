/*
* Copyright (c) 2008-2019 Geode Systems LLC
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
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.search.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Json;
import org.ramadda.util.Utils;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.StringUtil;


import java.io.*;

import java.net.URL;
import java.net.URLConnection;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 *
 */
public class QuandlSearchProvider extends SearchProvider {



    /** _more_ */
    public static final String URL_ROOT = "https://www.quandl.com/api/v1";

    /**
     * _more_
     *
     * @param repository _more_
     */
    public QuandlSearchProvider(Repository repository) {
        super(repository, "quandl", "Quandl Data");
    }



    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public String getSiteUrl() {
        return "https://www.quandl.com";
    }

    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public String getSearchProviderIconUrl() {
        return "${root}/biz/quandl.png";
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

        List<Entry> entries = new ArrayList<Entry>();
        String url =
            HtmlUtils.url(URL_ROOT + "/datasets.json", "query",
                          request.getString(ARG_TEXT, ""), "auth_token",
                          getRepository().getProperty("quandl.api.key", ""));
        JSONArray docs = null;
        String    json = null;

        try {
            URLConnection connection = new URL(url).openConnection();
            connection.setRequestProperty("User-Agent", "ramadda");
            InputStream is = connection.getInputStream();
            json = IOUtil.readContents(is);
            IOUtil.close(is);
            docs = Json.readArray(new JSONObject(new JSONTokener(json)),
                                  "docs");
            if (docs == null) {
                System.err.println(
                    "Quandl SearchProvider: no docs field in json");

                return entries;
            }
        } catch (Exception exc) {
            System.out.println("quandl bad:" + url);

            return entries;
        }

        //        System.out.println("quandl:" + url);

        Entry parent = getSynthTopLevelEntry();
        TypeHandler seriesTypeHandler =
            getRepository().getTypeHandler("type_quandl_series");
        TypeHandler linkTypeHandler = getRepository().getTypeHandler("link");
        for (int i = 0; i < docs.length(); i++) {
            JSONObject    item       = docs.getJSONObject(i);
            String        id         = Json.readValue(item, "id", "");
            String        sourceName = Json.readValue(item, "source_name",
                                           "");
            String        sourceCode = Json.readValue(item, "source_code",
                                           "");
            String        code       = Json.readValue(item, "code", "");
            String        name       = Json.readValue(item, "name", "");
            String        urlizeName = Json.readValue(item, "urlize_name",
                                           "");
            StringBuilder desc       = new StringBuilder();
            String        displayUrl = Json.readValue(item, "display_url",
                                           "");
            if (displayUrl != null) {
                //                desc.append(HtmlUtils.href(displayUrl, displayUrl));
                //                desc.append(HtmlUtils.br());
            }

            boolean premium = Json.readValue(item, "premium",
                                             "").equals("true");
            //For now assume the auth code gives us access
            premium = false;

            TypeHandler typeHandler = premium
                                      ? linkTypeHandler
                                      : seriesTypeHandler;
            desc.append(Json.readValue(item, "description", ""));
            String pageUrl = "https://www.quandl.com/data/" + sourceCode
                             + "/" + code + "-" + urlizeName;

            String   entryName = sourceName + " - " + name;
            Resource resource  = new Resource(pageUrl);

            Date     dttm      = new Date();
            Date fromDate = DateUtil.parse(Json.readValue(item, "from_date",
                                "2015-01-01"));
            Date toDate = DateUtil.parse(Json.readValue(item, "to_date",
                              "2015-01-01"));
            Entry newEntry = new Entry(makeSynthId(sourceCode, code),
                                       typeHandler);

            Object[] values = null;

            if ( !premium) {
                values = typeHandler.makeEntryValues(null);
                values[QuandlSeriesTypeHandler.IDX_SOURCE_CODE] = sourceCode;
                values[QuandlSeriesTypeHandler.IDX_SERIES_CODE] = code;
            } else {
                entryName = "Premium: " + entryName;
            }

            entries.add(newEntry);
            newEntry.initEntry(entryName, desc.toString(), parent,
                               getUserManager().getLocalFileUser(), resource,
                               "", dttm.getTime(), dttm.getTime(),
                               fromDate.getTime(), toDate.getTime(), values);
            getEntryManager().cacheSynthEntry(newEntry);
        }

        return entries;
    }


}
