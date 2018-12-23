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

package org.ramadda.plugins.awhere;


import org.json.*;

import org.ramadda.data.services.*;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.metadata.Metadata;
import org.ramadda.repository.type.TypeHandler;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Json;
import org.ramadda.util.Utils;

import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;


import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import java.net.URL;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 */
public class AwhereImportHandler extends ImportHandler {

    /**
     * _more_
     */
    public AwhereImportHandler() {
        super(null);
    }

    /**
     * _more_
     *
     * @param repository _more_
     */
    public AwhereImportHandler(Repository repository) {
        super(repository);
    }

    /**
     * _more_
     *
     * @param importTypes _more_
     * @param formBuffer _more_
     */
    @Override
    public void addImportTypes(List<TwoFacedObject> importTypes,
                               Appendable formBuffer) {
        super.addImportTypes(importTypes, formBuffer);
        importTypes.add(new TwoFacedObject("aWhere Data", "awhere"));
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param repository _more_
     * @param url _more_
     * @param parentEntry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public Result handleUrlRequest(Request request, Repository repository,
                                   String url, Entry parentEntry)
            throws Exception {

        if ( !request.getString(ARG_IMPORT_TYPE, "").equals("awhere")) {
            return null;
        }
        List<String> toks = StringUtil.split(request.getString("extra", ""),
                                             ":");
        String      key     = (toks.size() >= 1)
                              ? toks.get(0)
                              : null;
        String      secret  = (toks.size() >= 2)
                              ? toks.get(1)
                              : null;

        List<Entry> entries = new ArrayList<Entry>();
        List<File>  files   = new ArrayList<File>();


        AwhereFarmTypeHandler farmType =
            (AwhereFarmTypeHandler) repository.getTypeHandler(
                "type_awhere_farm");
        AwhereFieldTypeHandler fieldType =
            (AwhereFieldTypeHandler) repository.getTypeHandler(
                "type_awhere_field");

        String json = Awhere.doGet(parentEntry, new URL(Awhere.URL_FIELDS));
        if (json == null) {
            throw new RuntimeException("Failed to authorize");
        }

        System.err.println("Import:" + json);

        JSONObject               js    =
            new JSONObject(new JSONTokener(json));
        Hashtable<String, Entry> farms = new Hashtable<String, Entry>();
        Date                     now   = new Date();
        User                     user  = request.getUser();
        Object[]                 values;

        JSONArray                jsa = js.getJSONArray("fields");
        //{"fields":[{"name":"fieldName","acres":null,"centerPoint":{"latitude":40.0,"longitude":-107.0},"farmId":"farmId","id":"fieldId"
        StringBuilder sb = new StringBuilder();
        sb.append("+section\n");
        sb.append(":title aWhere Import Results\n");
        if (jsa.length() == 0) {
            sb.append(
                getPageHandler().showDialogWarning("No fields were found\n"));
            sb.append("\n");
        }
        sb.append("<ul> ");
        for (int i = 0; i < jsa.length(); i++) {
            JSONObject item      = jsa.getJSONObject(i);
            String     fieldName = item.getString("name");
            String     fieldId   = item.getString("id");
            String     acres     = item.optString("acres", "0");
            String     farmId    = item.getString("farmId");
            String     farmName  = farmId.replaceAll("_", " ");
            double lat = Double.parseDouble(Json.readValue(item,
                             "centerPoint.latitude", "0.0"));
            double lon = Double.parseDouble(Json.readValue(item,
                             "centerPoint.longitude", "0.0"));
            Entry farmEntry = farms.get(farmId);
            if (farmEntry == null) {
                farmEntry = farmType.createEntry(repository.getGUID());
                values = farmType.getEntryValues(farmEntry);
                values[farmType.IDX_FARM_ID] = farmId;
                if (Utils.stringDefined(key)) {
                    values[farmType.IDX_KEY] = key;
                }
                if (Utils.stringDefined(secret)) {
                    values[farmType.IDX_SECRET] = secret;
                }

                farmEntry.initEntry(farmName, "", parentEntry, user,
                                    new Resource(), "", now.getTime(),
                                    now.getTime(), now.getTime(),
                                    now.getTime(), values);
                entries.add(farmEntry);
                farms.put(farmId, farmEntry);
                sb.append("<li> ");
                sb.append(getEntryManager().getEntryLink(request, farmEntry,
                        true));
            }
            Entry fieldEntry = fieldType.createEntry(repository.getGUID());
            values = fieldType.getEntryValues(fieldEntry);
            values[fieldType.IDX_FIELD_ID] = fieldId;
            values[fieldType.IDX_ACRES]    = new Double(acres);
            fieldEntry.initEntry(fieldName, "", farmEntry, user,
                                 new Resource(), "", now.getTime(),
                                 now.getTime(), now.getTime(), now.getTime(),
                                 values);
            fieldEntry.setLatitude(lat);
            fieldEntry.setLongitude(lon);
            entries.add(fieldEntry);
            sb.append("<li> ");
            sb.append(getEntryManager().getEntryLink(request, fieldEntry,
                    true));
            sb.append("\n");
        }

        sb.append("</ul>\n");
        //        this.token = js.optString("access_token");

        for (Entry entry : entries) {
            entry.setUser(request.getUser());
        }
        getEntryManager().addNewEntries(request, entries);
        sb.append("-section\n");
        String html = getWikiManager().wikifyEntry(request, parentEntry,
                          sb.toString());

        return getEntryManager().addEntryHeader(request, parentEntry,
                new Result("", new StringBuilder(html)));

    }


}
