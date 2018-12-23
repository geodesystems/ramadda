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


import java.io.*;

import java.net.URL;
import java.net.URLConnection;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * Proxy that searches a specified CKAN repository
 *
 */
public class CkanSearchProvider extends SearchProvider {

    /** ckan repository url */
    private String baseUrl;

    // e.g.:   http://catalog.data.gov/api/action/package_search?q=spending

    /** _more_ */
    public static final String URL_ROOT = "/api/action/package_search";


    /**
     * _more_
     *
     * @param repository _more_
     * @param args _more_
     */
    public CkanSearchProvider(Repository repository, List<String> args) {
        super(repository,
              args.get(0).replaceAll("^.*//", "").replaceAll("/", ""),
              args.get(1));
        baseUrl = args.get(0);
        /*
        String url = baseUrl +"/api/action/package_search?q=data";
        try {
            URLConnection connection = new URL(url).openConnection();
            connection.setRequestProperty("User-Agent", "ramadda");
            InputStream is  = connection.getInputStream();
            String json = IOUtil.readContents(is);
            if(json.indexOf("\"results\"")<0) {
                System.err.println("bad json:"+ url);
                if(json.length()>500) json = json.substring(0,499);
                System.err.println(json);
                System.exit(0);
            } else {
                String name = getLabel();
                List<String> toks = StringUtil.splitUpTo(getLabel(),"-",2);
                if(toks.size()==2) {
                    String location = toks.get(1);
                    List<String> toks2= StringUtil.splitUpTo(location,",",2);
                    if(toks2.size() == 2) {
                        location = toks2.get(1) +" - " + toks2.get(0);
                    }
                    name = location +" - " + toks.get(0);
                }
                System.out.println(name+";" + "org.ramadda.plugins.search.CkanSearchProvider;" + baseUrl);
                //                System.out.println("good:" + url +  " " + json);
            }
            IOUtil.close(is);
        } catch(Exception exc) {
            System.err.println("bad url:"+ url);
        }
        */


    }


    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public String getCategory() {
        return "CKAN Repositories";
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
    public String getSearchProviderIconUrl() {
        return "${root}/search/ckan.png";
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
        String url = HtmlUtils.url(baseUrl + URL_ROOT, "q",
                                   request.getString(ARG_TEXT, ""));
        JSONObject obj  = null;
        String     json = null;

        try {
            try {
                URLConnection connection = new URL(url).openConnection();
                connection.setRequestProperty("User-Agent", "ramadda");
                InputStream is = connection.getInputStream();
                json = IOUtil.readContents(is);
                IOUtil.close(is);
            } catch (Exception exc) {
                if (url.startsWith("http:")) {
                    System.err.println(
                        "CKAN: Failed with http, trying https");
                    url = url.replace("http:", "https:");
                    URLConnection connection = new URL(url).openConnection();
                    connection.setRequestProperty("User-Agent", "ramadda");
                    InputStream is = connection.getInputStream();
                    json = IOUtil.readContents(is);
                    IOUtil.close(is);
                } else {
                    try {
                        String tmp =
                            getRepository().getStorageManager().getTmpFile(
                                request, "out.json").toString();
                        System.err.println("Trying via wget -" + tmp);
                        List<String> commands = new ArrayList<String>();
                        commands.add("/usr/bin/wget");
                        commands.add("-O");
                        commands.add(tmp);
                        commands.add(url);
                        getRepository().getJobManager()
                            .executeCommand(commands,
                                            getRepository()
                                                .getStorageManager()
                                                    .getRepositoryDir());

                        json = IOUtil.readContents(tmp, getClass());
                    } catch (Exception wgetexc) {
                        System.err.println("error via wget:" + wgetexc);
                    }
                    if (json == null) {
                        throw exc;
                    }
                }
            }
            obj = Json.readObject(new JSONObject(new JSONTokener(json)),
                                  "result");
            if (obj == null) {
                System.err.println(
                    "CKAN SearchProvider: no result field in json");

                return entries;
            }
        } catch (Exception exc) {
            exc.printStackTrace();
            System.out.println("ckan bad:" + url + " " + exc);
            System.out.println("JSON:" + json);

            return entries;
        }
        System.out.println("ckan:" + url);

        JSONArray searchResults = Json.readArray(obj, "results");
        if (searchResults == null) {
            System.err.println(
                "CKAN SearchProvider: no results field in json");

            return entries;
        }

        Entry       parent       = getSynthTopLevelEntry();
        TypeHandler typeHandler  = getRepository().getTypeHandler("group");
        TypeHandler rtypeHandler = getRepository().getTypeHandler("link");

        for (int i = 0; i < searchResults.length(); i++) {
            JSONObject item     = searchResults.getJSONObject(i);
            String     id       = Json.readValue(item, "id", "");
            String     desc     = Json.readValue(item, "notes", "");
            String     title    = Json.readValue(item, "title", "");
            String     name     = Json.readValue(item, "name", "");
            Date       dttm     = new Date();
            Date       fromDate = dttm;
            //DateUtil.parse(Json.readValue(snippet, "publishedAt", null));
            Date   toDate  = fromDate;

            String itemUrl = baseUrl + "/dataset/" + name;
            Entry newEntry = new Entry(Repository.ID_PREFIX_SYNTH + getId()
                                       + TypeHandler.ID_DELIMITER
                                       + id, typeHandler);
            newEntry.setMasterTypeHandler(this);
            newEntry.setIcon("/search/ckan.png");
            entries.add(newEntry);

            /*
                Metadata thumbnailMetadata =
                    new Metadata(getRepository().getGUID(), newEntry.getId(),
                                 ContentMetadataHandler.TYPE_THUMBNAIL,
                                 false, thumb, null, null, null, null);
                                 getMetadataManager().addMetadata(newEntry, thumbnailMetadata);
            */

            newEntry.initEntry(title, desc, parent,
                               getUserManager().getLocalFileUser(),
                               new Resource(new URL(itemUrl)), "",
                               dttm.getTime(), dttm.getTime(),
                               fromDate.getTime(), toDate.getTime(), null);


            List<String> ids      = new ArrayList<String>();
            List<Entry>  children = new ArrayList<Entry>();



            JSONArray    tags     = Json.readArray(item, "tags");
            if (tags != null) {
                for (int tagIdx = 0; tagIdx < tags.length(); tagIdx++) {
                    JSONObject tag = tags.getJSONObject(tagIdx);
                    getMetadataManager().addMetadata(newEntry,
                            new Metadata(getRepository().getGUID(),
                                         newEntry.getId(), "enum_tag", false,
                                         Json.readValue(tag, "display_name",
                                             ""), null, null, null, null));
                }
            }


            JSONArray resources = Json.readArray(item, "resources");
            if (resources != null) {
                for (int resourceIdx = 0; resourceIdx < resources.length();
                        resourceIdx++) {
                    JSONObject  rObj = resources.getJSONObject(resourceIdx);
                    String      rname = Json.readValue(rObj, "name", "");
                    String      rid = Json.readValue(rObj, "id", "");
                    String rdesc = Json.readValue(rObj, "description", "");
                    String      dataUrl = Json.readValue(rObj, "url", null);
                    String      format = Json.readValue(rObj, "format", "");
                    String      rUrl = itemUrl + "/resource/" + rid;

                    TypeHandler typeHandlerToUse = rtypeHandler;

                    if (format.equals("CSV") && (dataUrl != null)) {
                        typeHandlerToUse = getRepository().getTypeHandler(
                            "type_document_csv");
                        rdesc += HtmlUtils.br() + HtmlUtils.href(rUrl, rUrl);
                        rUrl  = dataUrl;
                    }

                    Entry rEntry = new Entry(Repository.ID_PREFIX_SYNTH
                                             + getId()
                                             + TypeHandler.ID_DELIMITER
                                             + rid, typeHandlerToUse);
                    rEntry.setIcon("/search/ckan.png");

                    rEntry.initEntry(rname, rdesc, newEntry,
                                     getUserManager().getLocalFileUser(),
                                     new Resource(new URL(rUrl)), "",
                                     dttm.getTime(), dttm.getTime(),
                                     fromDate.getTime(), toDate.getTime(),
                                     null);

                    ids.add(rEntry.getId());
                    children.add(rEntry);
                    getEntryManager().cacheSynthEntry(rEntry);
                }
                newEntry.setChildIds(ids);
                newEntry.setChildren(children);
            }
            getEntryManager().cacheSynthEntry(newEntry);
        }

        return entries;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param mainEntry _more_
     * @param parentEntry _more_
     * @param synthId _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public List<String> getSynthIds(Request request, Entry mainEntry,
                                    Entry parentEntry, String synthId)
            throws Exception {
        return parentEntry.getChildIds();
    }


}
