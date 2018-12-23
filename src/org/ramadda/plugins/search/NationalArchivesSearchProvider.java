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

package org.ramadda.plugins.search;


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
import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.net.URL;
import java.net.URLConnection;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * Proxy that searches
 *
 */
public class NationalArchivesSearchProvider extends SearchProvider {

    /** _more_ */
    private static final String ID = "nationalarchives";

    /** _more_ */
    private static final String URL =
        "https://catalog.archives.gov/api/v1?rows=100&q=";





    /**
     * _more_
     *
     * @param repository _more_
     */
    public NationalArchivesSearchProvider(Repository repository) {
        super(repository, ID, "National Archives Search");
    }


    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public String getSiteUrl() {
        return "https://catalog.archives.gov";
    }



    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public String getSearchProviderIconUrl() {
        return "${root}/search/nationalarchives.png";
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isEnabled() {
        return true;
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
        String      url     = URL + request.getString(ARG_TEXT, "");
        System.err.println(getName() + " search url:" + url);
        InputStream is   = getInputStream(url);
        String      json = IOUtil.readContents(is);
        IOUtil.close(is);
        System.out.println(json);
        JSONObject obj = new JSONObject(new JSONTokener(json));
        if ( !obj.has("items")) {
            System.err.println(
                "NationalArchives SearchProvider: no items field in json:"
                + json);

            return entries;
        }


        JSONArray searchResults = obj.getJSONArray("items");
        Entry     parent        = getSynthTopLevelEntry();
        TypeHandler typeHandler =
            getRepository().getTypeHandler("media_youtubevideo");

        for (int i = 0; i < searchResults.length(); i++) {
            JSONObject item    = searchResults.getJSONObject(i);
            JSONObject snippet = item.getJSONObject("snippet");
            String     kind    = Json.readValue(item, "id.kind", "");
            if ( !kind.equals("youtube#video")) {
                System.err.println("? Youtube kind:" + kind);

                continue;
            }
            String id   = Json.readValue(item, "id.videoId", "");
            String name = snippet.getString("title");
            String desc = snippet.getString("description");

            Date   dttm = new Date();
            Date fromDate = DateUtil.parse(Json.readValue(snippet,
                                "publishedAt", null));
            Date   toDate  = fromDate;

            String itemUrl = "https://www.youtube.com/watch?v=" + id;

            Entry newEntry = new Entry(Repository.ID_PREFIX_SYNTH + getId()
                                       + TypeHandler.ID_DELIMITER
                                       + id, typeHandler);
            entries.add(newEntry);

            String thumb = Json.readValue(snippet, "thumbnails.default.url",
                                          null);

            if (thumb != null) {
                Metadata thumbnailMetadata =
                    new Metadata(getRepository().getGUID(), newEntry.getId(),
                                 ContentMetadataHandler.TYPE_THUMBNAIL,
                                 false, thumb, null, null, null, null);
                getMetadataManager().addMetadata(newEntry, thumbnailMetadata);
            }


            newEntry.initEntry(name, desc, parent,
                               getUserManager().getLocalFileUser(),
                               new Resource(new URL(itemUrl)), "",
                               dttm.getTime(), dttm.getTime(),
                               fromDate.getTime(), toDate.getTime(), null);
            getEntryManager().cacheSynthEntry(newEntry);
        }

        return entries;
    }



}
