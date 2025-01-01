/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.ogc;


import org.json.*;

import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.search.*;
import org.ramadda.repository.util.SelectInfo;

import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JsonUtil;
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
public class EsriSearchProvider extends SearchProvider {

    /** repository url */
    private String baseUrl;

    /** _more_ */
    private static final String URL =
        "/search?num=10&start=0&sortField=title&sortOrder=desc&q=(%20title:%query%%20OR%20tags:%query%%20OR%20typeKeywords:%query%%20OR%20snippet:%query%%20)%20%20&v=1&f=json";


    /**
     * _more_
     *
     * @param repository _more_
     * @param args _more_
     */
    public EsriSearchProvider(Repository repository, List<String> args) {
        super(repository,
              args.get(0).replaceAll("^.*//", "").replaceAll("/", ""),
              args.get(1));
        baseUrl = args.get(0);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public String getCategory() {
        return "Esri Repository";
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
        return "${root}/ogc/esri.png";
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
        String      url     = baseUrl + URL;
        url = url.replaceAll("%query%", request.getString(ARG_TEXT, ""));
        System.err.println(url);
        String json = null;
        try {
            Entry parent = getSynthTopLevelEntry();
            TypeHandler typeHandler =
                getRepository().getTypeHandler("type_esri_resource");
            URLConnection connection = new URL(url).openConnection();
            connection.setRequestProperty("User-Agent", "ramadda");
            InputStream is = connection.getInputStream();
            json = IOUtil.readContents(is);
            IOUtil.close(is);
            JSONTokener tokenizer = new JSONTokener(json);
            JSONObject  obj       = new JSONObject(tokenizer);
            JSONArray   results   = obj.getJSONArray("results");
            for (int i = 0; i < results.length(); i++) {
                JSONObject result = results.getJSONObject(i);
                String     id     = result.getString("id");
                String     name   = (result.isNull("name")
                                     ? null
                                     : result.getString("name"));
                String     title  = result.getString("title");
                String     desc   = result.isNull("description")
                                    ? null
                                    : result.getString("description");
                if (desc == null) {
                    desc = result.isNull("snippet")
                           ? null
                           : result.getString("snippet");
                }
                if (desc == null) {
                    desc = "";
                }
                String type     = result.getString("type");
                String thumb    = result.isNull("thumbnail")
                                  ? null
                                  : result.getString("thumbnail");


                String file     = baseUrl + "/content/items/" + id + "/data";
                Date   dttm     = new Date();
                Date   fromDate = dttm;
                Date   toDate   = fromDate;
                String itemUrl  = baseUrl + "/dataset/" + name;
                Entry newEntry = new Entry(Repository.ID_PREFIX_SYNTH
                                           + getId()
                                           + TypeHandler.ID_DELIMITER
                                           + id, typeHandler);
                //                newEntry.setMasterTypeHandler(this);
                newEntry.setIcon("/ogc/esri.png");
                entries.add(newEntry);

                if (thumb != null) {
                    thumb = baseUrl + "/content/items/" + id + "/info/"
                            + thumb;
                    Metadata thumbnailMetadata =
                        new Metadata(getRepository().getGUID(),
                                     newEntry.getId(),
                                     getMetadataManager().findType(ContentMetadataHandler.TYPE_THUMBNAIL),
                                     false, thumb, null, null, null, null);
                    getMetadataManager().addMetadata(request,newEntry,
                            thumbnailMetadata);
                }

                newEntry.initEntry(title, desc, parent,
                                   getUserManager().getLocalFileUser(),
                                   new Resource(new URL(file)), "",
                                   Entry.DEFAULT_ORDER, dttm.getTime(),
                                   dttm.getTime(), fromDate.getTime(),
                                   toDate.getTime(), null);
                Object[] values =
                    newEntry.getTypeHandler().getEntryValues(newEntry);
                values[0] = type;
                getEntryManager().cacheSynthEntry(newEntry);


                /*
            List<String> ids      = new ArrayList<String>();
            List<Entry>  children = new ArrayList<Entry>();
            JSONArray    tags     = JsonUtil.readArray(item, "tags");
            if (tags != null) {
                for (int tagIdx = 0; tagIdx < tags.length(); tagIdx++) {
                    JSONObject tag = tags.getJSONObject(tagIdx);
                    getMetadataManager().addMetadata(request,newEntry,
                        new Metadata(
                            getRepository().getGUID(), newEntry.getId(),
                            getMetadataManager().findType("enum_tag"), false,
                            JsonUtil.readValue(tag, "display_name", ""), null,
                            null, null, null));
                }
            }
                */


            }

            return entries;
        } catch (Exception exc) {
            exc.printStackTrace();
            System.out.println("esri bad:" + url + " " + exc);
            System.out.println("json:" + json);

            return entries;
        }

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
    public List<String> getSynthIds(Request request, SelectInfo select, Entry mainEntry,
                                    Entry parentEntry, String synthId)
            throws Exception {
        return parentEntry.getChildIds();
    }


}
