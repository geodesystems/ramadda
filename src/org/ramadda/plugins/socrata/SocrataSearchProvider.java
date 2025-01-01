/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.socrata;


import org.json.*;

import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.search.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;

import org.ramadda.util.JsonUtil;
import org.ramadda.util.Utils;
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
public class SocrataSearchProvider extends SearchProvider {

    /** _more_ */
    private static final String SOCRATA_TYPE_DATASET = "dataset";

    /** _more_ */
    private static final String SOCRATA_ID = "socrata";

    /** _more_ */
    private static final String URL_ALL =
        "http://api.us.socrata.com/api/catalog/v1?";

    /** _more_ */
    private String hostname;


    /**
     * _more_
     *
     * @param repository _more_
     */
    public SocrataSearchProvider(Repository repository) {
        this(repository, new ArrayList<String>());
    }


    /**
     * _more_
     *
     * @param repository _more_
     * @param args _more_
     */
    public SocrataSearchProvider(Repository repository, List<String> args) {
        super(repository, ((args.size() == 0)
                           ? SOCRATA_ID
                           : args.get(0)), ((args.size() > 1)
                                            ? args.get(1)
                                            : ((args.size() == 0)
                ? "All Socrata Sites"
                : args.get(0))));
        if (args.size() > 0) {
            hostname = args.get(0);
        }
    }


    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public String getCategory() {
        return "Socrata Search Providers";
    }

    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public String getSiteUrl() {
        if (hostname != null) {
            return "https://" + hostname;
        }

        return "http://www.socrata.com/";
    }


    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public String getSearchProviderIconUrl() {
        return "${root}/socrata/socrata.png";
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

        //For now  the search all does a hostname search as well
        if (true) {
            //        if (hostname == null) {
            return doSearchAll(request, searchInfo);
        }

        List<Entry> entries = new ArrayList<Entry>();


        String      server  = "https://" + hostname;
        String url = server + "/api/search/views.json?q="
                     + HtmlUtils.urlEncodeSpace(request.getString(ARG_TEXT,
                         ""));
        //        System.err.println(getName() + " search url:" + url);

        String json = new String(IOUtil.readBytes(getInputStream(url)));
        //        System.out.println("json:" + json);
        JSONObject obj = new JSONObject(new JSONTokener(json));
        if ( !obj.has("results")) {
            System.err.println(
                "Socrata SearchProvider: no items field in json:" + json);

            return entries;
        }

        JSONArray searchResults = obj.getJSONArray("results");
        Entry     parent        = getSynthTopLevelEntry();
        TypeHandler seriesTypeHandler =
            getRepository().getTypeHandler(
                SocrataSeriesTypeHandler.TYPE_SERIES);
        TypeHandler fileTypeHandler =
            getRepository().getTypeHandler(TypeHandler.TYPE_FILE);

        for (int i = 0; i < searchResults.length(); i++) {
            JSONObject wrapper = searchResults.getJSONObject(i);
            JSONObject item    = wrapper.getJSONObject("view");
            if (JsonUtil.readValue(item, "url", "").equals("https:/-/-/")) {
                continue;
            }

            String id   = JsonUtil.readValue(item, "id", "");
            String name = JsonUtil.readValue(item, "name", "");
            StringBuilder desc = new StringBuilder(JsonUtil.readValue(item,
                                     "description", ""));
            String      displayType = JsonUtil.readValue(item, "displayType", "");
            String      viewType    = JsonUtil.readValue(item, "viewType", "");

            TypeHandler typeHandler = (viewType.equals("tabular")
                                       ? seriesTypeHandler
                                       : fileTypeHandler);


            Date        dttm        = new Date();
            Date        fromDate    = dttm,
                        toDate      = dttm;
            String      itemUrl = "https://" + hostname + "/dataset/-/" + id;
            Resource    resource    = new Resource(new URL(itemUrl));
            Entry newEntry = new Entry(Repository.ID_PREFIX_SYNTH + getId()
                                       + TypeHandler.ID_DELIMITER
                                       + id, typeHandler);
            Object[] values = typeHandler.makeEntryValues(null);
            System.err.println("view type:" + viewType);
            values[SocrataSeriesTypeHandler.IDX_REPOSITORY] = server;
            if (viewType.equals("tabular")) {
                values[SocrataSeriesTypeHandler.IDX_SERIES_ID] = id;
            } else if (viewType.equals("blobby")) {
                String mimeType = StringUtil.splitUpTo(JsonUtil.readValue(item,
                                      "blobMimeType", ";"), ";",
                                          2).get(0).trim();
                String getFileUrl = "https://" + hostname + "/download/" + id
                                    + "/" + mimeType;
                resource = new Resource(new URL(getFileUrl));
                desc.append(HtmlUtils.br());
                desc.append(HtmlUtils.href(itemUrl, "View file at Socrata"));
            }

            newEntry.setIcon("/socrata/socrata.png");
            entries.add(newEntry);
            newEntry.initEntry(name,
                               "<snippet>" + desc.toString() + "</snippet>",
                               parent, getUserManager().getLocalFileUser(),
                               resource, "", Entry.DEFAULT_ORDER,
                               dttm.getTime(), dttm.getTime(),
                               fromDate.getTime(), toDate.getTime(), values);
            getEntryManager().cacheSynthEntry(newEntry);
        }

        return entries;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param parentEntry _more_
     * @param id _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public Entry makeSynthEntry(Request request, Entry parentEntry, String id)
            throws Exception {
        List<String> toks = StringUtil.splitUpTo(id,
                                TypeHandler.ID_DELIMITER, 3);
        String     domain   = toks.get(0);
        String     type     = toks.get(1);
        String     seriesId = toks.get(2);
        String url = "https://" + domain + "/api/views/" + seriesId + ".json";
        String     json = new String(IOUtil.readBytes(getInputStream(url)));
        JSONObject obj      = new JSONObject(new JSONTokener(json));
        String     name     = obj.getString("name");

        return createEntry(request, domain, seriesId, name, "", type);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param searchInfo _more_
     * @return _more_
     *
     * @throws Exception _more_
     */
    private List<Entry> doSearchAll(Request request, org.ramadda.repository.util.SelectInfo searchInfo)
            throws Exception {

        List<Entry> entries = new ArrayList<Entry>();

        String url = URL_ALL + "&"
                     + HtmlUtils.arg(
                         "q",
                         HtmlUtils.urlEncodeSpace(
                             request.getString(ARG_TEXT, "")), false);
        url += "&" + HtmlUtils.arg("limit", "" + request.get(ARG_MAX, 100));
        url += "&" + HtmlUtils.arg("offset", "" + request.get(ARG_SKIP, 0));
        if (hostname != null) {
            url += "&" + HtmlUtils.arg("domains", hostname);
        }

        System.err.println(getName() + " search url:" + url);
        String json = new String(IOUtil.readBytes(getInputStream(url)));
        //        System.out.println("json:" + json);
        JSONObject obj = new JSONObject(new JSONTokener(json));
        if ( !obj.has("results")) {
            System.err.println(
                "Socrata SearchProvider: no items field in json:" + json);

            return entries;
        }

        JSONArray searchResults = obj.getJSONArray("results");
        Entry     parent        = getSynthTopLevelEntry();




        for (int i = 0; i < searchResults.length(); i++) {
            JSONObject wrapper = searchResults.getJSONObject(i);
            JSONObject item    = JsonUtil.readObject(wrapper, "resource");
            String     id      = JsonUtil.readValue(item, "id", "");
            String     name    = JsonUtil.readValue(item, "name", "");
            String domain = JsonUtil.readValue(wrapper, "metadata.domain",
                                           (String) null);
            if (domain == null) {
                domain = hostname;
            }

            List<String> tmp        = StringUtil.splitUpTo(domain, ".", 2);



            String       domainName = (tmp.size() > 1)
                                      ? tmp.get(1)
                                      : domain;
            name += " - " + domain;

            StringBuilder desc = new StringBuilder(JsonUtil.readValue(item,
                                     "description", ""));
            String type = JsonUtil.readValue(item, "type", "");



            Entry newEntry = createEntry(request, domain, id, name,
                                         desc.toString(), type);
            entries.add(newEntry);
        }

        return entries;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param domain _more_
     * @param id _more_
     * @param name _more_
     * @param desc _more_
     * @param type _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Entry createEntry(Request request, String domain, String id,
                              String name, String desc, String type)
            throws Exception {

        TypeHandler typeHandler =
            getRepository().getTypeHandler(type.equals(SOCRATA_TYPE_DATASET)
                                           ? SocrataSeriesTypeHandler
                                               .TYPE_SERIES
                                           : TypeHandler.TYPE_FILE);


        Date     dttm     = new Date();
        Date     fromDate = dttm,
                 toDate   = dttm;
        String   itemUrl  = "https://" + domain + "/-/-/" + id;

        Resource resource = new Resource(new URL(itemUrl));
        Entry newEntry = new Entry(Repository.ID_PREFIX_SYNTH + getId()
                                   + TypeHandler.ID_DELIMITER + domain
                                   + TypeHandler.ID_DELIMITER + type
                                   + TypeHandler.ID_DELIMITER
                                   + id, typeHandler);
        Object[] values = typeHandler.makeEntryValues(null);

        if (type.equals(SOCRATA_TYPE_DATASET)) {
            values[SocrataSeriesTypeHandler.IDX_REPOSITORY] = domain;
            values[SocrataSeriesTypeHandler.IDX_SERIES_ID]  = id;
        } else if (true || type.equals("blobby")) {
            //            System.err.println("Socrata - new Type:" + type);
            /*
            String mimeType = StringUtil.splitUpTo(JsonUtil.readValue(item,
                                  "blobMimeType", ";"), ";",
                                      2).get(0).trim();
            String getFileUrl = "https://" + domain + "/download/" + id
                             + "/" + mimeType;
            resource = new Resource(new URL(getFileUrl));
            //            https://www.opendatanyc.com/download/ewq6-p8b6/application/pdf
            desc.append(HtmlUtils.br());
            desc.append(HtmlUtils.href(itemUrl, "View file at Socrata"));
            */
        }

        newEntry.setIcon("/socrata/socrata.png");
        Entry parent = getSynthTopLevelEntry();
        newEntry.initEntry(name, "<snippet>" + desc + "</snippet>", parent,
                           getUserManager().getLocalFileUser(), resource, "",
                           Entry.DEFAULT_ORDER, dttm.getTime(),
                           dttm.getTime(), fromDate.getTime(),
                           toDate.getTime(), values);
        getEntryManager().cacheSynthEntry(newEntry);

        return newEntry;

    }

}
