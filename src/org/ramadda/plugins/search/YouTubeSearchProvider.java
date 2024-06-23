/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.search;


import org.json.*;

import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.search.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;

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
 * Proxy that searches youtube
 *
 */
public class YouTubeSearchProvider extends SearchProvider {

    /** _more_ */
    private static final String ID = "youtube";

    /** _more_ */
    private static final String URL =
        "https://www.googleapis.com/youtube/v3/search?part=snippet";


    /** _more_ */
    private static final String ARG_API_KEY = "key";

    /** _more_ */
    private static final String ARG_Q = "q";

    /** _more_ */
    private static final String ARG_MIN_TAKEN_DATE = "min_taken_date";

    /** _more_ */
    private static final String ARG_MAX_TAKEN_DATE = "max_taken_date";

    /** _more_ */
    private static final String ARG_TAGS = "tags";

    /** _more_ */
    private static final String ARG_BBOX = "bbox";




    /**
     * _more_
     *
     * @param repository _more_
     */
    public YouTubeSearchProvider(Repository repository) {
        super(repository, ID, "YouTube Search");
    }


    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public String getSiteUrl() {
        return "http://www.youtube.com/";
    }



    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public String getSearchProviderIconUrl() {
        return "${root}/search/youtube.png";
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

        List<Entry> entries = new ArrayList<Entry>();
        URL  url     =  new URL(HtmlUtils.url(URL, ARG_API_KEY, getApiKey(), ARG_Q,
					      request.getString(ARG_TEXT, "")));
	//        System.err.println(getName() + " search url:" + url);
	IO.Result result = IO.doGetResult(url);
	if(result.getError()) {
	    String message  = JsonUtil.readValue(result.getResult(),
						 "error.message",result.getResult());
	    getLogManager().logError(getName()+":" + message);
	    return entries;
	}
        JSONObject obj = new JSONObject(new JSONTokener(result.getResult()));
        if (!obj.has("items")) {
	    getLogManager().logError("YouTube SearchProvider: no items field in json:" + result.getResult());

            return entries;
        }

        JSONArray searchResults = obj.getJSONArray("items");
        Entry     parent        = getSynthTopLevelEntry();
        TypeHandler typeHandler =
            getRepository().getTypeHandler("media_youtubevideo");

        for (int i = 0; i < searchResults.length(); i++) {
            JSONObject item    = searchResults.getJSONObject(i);
            JSONObject snippet = item.getJSONObject("snippet");
            String     kind    = JsonUtil.readValue(item, "id.kind", "");
            if ( !kind.equals("youtube#video")) {
                System.err.println("? Youtube kind:" + kind);

                continue;
            }
            String id   = JsonUtil.readValue(item, "id.videoId", "");
            String name = snippet.getString("title");
            String desc = snippet.getString("description");

            Date   dttm = new Date();
            Date fromDate = Utils.parseDate(JsonUtil.readValue(snippet,
                                "publishedAt", null));
            Date   toDate  = fromDate;

            String itemUrl = "https://www.youtube.com/watch?v=" + id;

            Entry newEntry = new Entry(Repository.ID_PREFIX_SYNTH + getId()
                                       + TypeHandler.ID_DELIMITER
                                       + id, typeHandler);
            entries.add(newEntry);

            String thumb = JsonUtil.readValue(snippet, "thumbnails.default.url",
                                          null);

            if (thumb != null) {
                Metadata thumbnailMetadata =
                    new Metadata(getRepository().getGUID(), newEntry.getId(),
                                 getMetadataManager().findType(ContentMetadataHandler.TYPE_THUMBNAIL),
                                 false, thumb, null, null, null, null);
                getMetadataManager().addMetadata(request,newEntry, thumbnailMetadata);
            }


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
