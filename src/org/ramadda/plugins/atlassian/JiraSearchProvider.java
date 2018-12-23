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

package org.ramadda.plugins.atlassian;


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
 * Proxy that searches jira
 *
 */
public class JiraSearchProvider extends SearchProvider {


    /** _more_ */
    private static final String ARG_JQL = "jql";

    /** _more_ */
    private String baseUrl;


    /**
     * _more_
     *
     * @param repository _more_
     * @param args _more_
     */
    public JiraSearchProvider(Repository repository, List<String> args) {
        super(repository, args.get(0), args.get(2));
        baseUrl = args.get(1);
    }

    /**
     * _more_
     *
     * @param repository _more_
     */
    public JiraSearchProvider(Repository repository) {
        super(repository, "jira", "Jira Search");
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
        return "${root}/atlassian/jira.png";
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
    public List<Entry> getEntries(Request request, SearchInfo searchInfo)
            throws Exception {

        List<Entry> entries = new ArrayList<Entry>();
        String url = HtmlUtils.url(baseUrl + "/rest/api/2/search", ARG_JQL,
                                   "text~" + request.getString(ARG_TEXT, ""));
        System.err.println(getName() + " search url:" + url);
        InputStream is   = getInputStream(url);
        String      json = IOUtil.readContents(is);
        IOUtil.close(is);
        //        System.out.println("jira json:" + json);
        JSONObject obj = new JSONObject(new JSONTokener(json));
        if ( !obj.has("issues")) {
            System.err.println(
                "Jira SearchProvider: no issues field in json:" + json);

            return entries;
        }


        JSONArray   issues      = obj.getJSONArray("issues");
        Entry       parent      = getSynthTopLevelEntry();
        TypeHandler typeHandler =
            getRepository().getTypeHandler("jira_issue");

        for (int i = 0; i < issues.length(); i++) {
            JSONObject issue    = issues.getJSONObject(i);
            JSONObject fields   = issue.getJSONObject("fields");
            String     id       = Json.readValue(issue, "id", "");
            String     key      = Json.readValue(issue, "key", "");
            String     issueUrl = baseUrl + "/browse/" + key;
            String     name     = Json.readValue(fields, "summary", "");
            String     desc     = Json.readValue(fields, "description", "");


            Date       dttm     = new Date();
            Date fromDate = DateUtil.parse(Json.readValue(fields, "created",
                                null));
            Date toDate = fromDate;


            Entry newEntry = new Entry(Repository.ID_PREFIX_SYNTH + getId()
                                       + TypeHandler.ID_DELIMITER
                                       + id, typeHandler);
            entries.add(newEntry);

            Object[] values = typeHandler.makeEntryValues(null);
            values[0] = Json.readValue(fields, "issuetype.name", "");
            values[1] = Json.readValue(fields, "priority.name", "");

            /*
            String thumb = Json.readValue(snippet, "thumbnails.default.url",
                                          null);

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
                               new Resource(new URL(issueUrl)), "",
                               dttm.getTime(), dttm.getTime(),
                               fromDate.getTime(), toDate.getTime(), values);
            getEntryManager().cacheSynthEntry(newEntry);
        }

        return entries;
    }



}
