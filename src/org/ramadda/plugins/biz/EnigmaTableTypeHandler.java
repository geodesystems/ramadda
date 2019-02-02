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

import org.ramadda.data.point.*;
import org.ramadda.data.point.text.*;
import org.ramadda.data.record.*;

import org.ramadda.data.services.PointTypeHandler;


import org.ramadda.repository.*;
import org.ramadda.repository.database.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.FormInfo;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Json;
import org.ramadda.util.Utils;

import org.ramadda.util.sql.*;
import org.ramadda.util.text.Row;


import org.w3c.dom.*;


import ucar.unidata.util.StringUtil;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.URL;

import java.sql.*;

import java.util.ArrayList;

import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.*;



/**
 *
 *
 */
public class EnigmaTableTypeHandler extends PointTypeHandler {

    /** _more_ */
    public static final int IDX_TABLE_ID = 2;


    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public EnigmaTableTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    public void initializeNewEntry(Request request, Entry entry)
            throws Exception {
        //        super.initializeNewEntry(request, entry);
        String apiKey = getRepository().getProperty("enigma.api.key",
                            (String) null);
        if (apiKey == null) {
            return;
        }
        String tableId = (String) entry.getValue(IDX_TABLE_ID, null);
        if (tableId == null) {
            return;
        }
        entry.setResource(new Resource(new URL("https://app.enigma.io/table/"
                + tableId)));

        StringBuilder props = new StringBuilder();
        String url = "https://api.enigma.io/v2/meta/" + apiKey + "/"
                     + tableId + "?conjunction=and";
        JSONObject   meta     = Json.readUrl(url);
        JSONArray    columns  = Json.readArray(meta, "result.columns");
        JSONArray    path     = Json.readArray(meta, "result.path");
        List<String> ids      = new ArrayList<String>();
        List<String> fields   = new ArrayList<String>();

        JSONObject   pathItem = path.getJSONObject(path.length() - 1);
        entry.setName(pathItem.getString("label"));
        entry.setDescription(pathItem.getString("description"));

        if (columns != null) {
            for (int i = 0; i < columns.length(); i++) {
                JSONObject    col    = columns.getJSONObject(i);
                String        id     = col.getString("id");
                String        label  = col.getString("label");
                String        type   = col.getString("type");
                String        myType = RecordField.TYPE_STRING;
                StringBuilder extra  = new StringBuilder();
                if (type.equals("type_varchar")) {
                    myType = RecordField.TYPE_STRING;
                } else if (type.equals("type_numeric")
                           || type.equals("type_integer")) {
                    myType = RecordField.TYPE_DOUBLE;
                    extra.append(TextFile.attrChartable());
                } else if (type.equals("type_date")) {
                    myType = RecordField.TYPE_DATE;
                    //                    1989-04-12T00:00:00.000Z
                    extra.append(
                        TextFile.attrFormat("yyyy-MM-dd'T'HH:mm:ss.SSS"));
                } else {
                    myType = RecordField.TYPE_STRING;
                    System.err.println("Unknown type:" + type);
                }
                ids.add(id);

                fields.add(TextFile.makeField(id, TextFile.attrType(myType),
                        TextFile.attrLabel(label), extra.toString()));
            }
            //            System.err.println("fields:" + fields);
            props.append(TextFile.PROP_FIELDS);
            props.append("=");
            props.append(StringUtil.join(",", fields));
        }

        Object[] values = getEntryValues(entry);
        values[IDX_PROPERTIES] = props.toString();

    }


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public String getPathForEntry(Request request, Entry entry)
            throws Exception {
        String tableId = entry.getValue(IDX_TABLE_ID, (String) null);
        //        System.err.println("getPathForEntry:"+ tableId);
        if (tableId == null) {
            return null;
        }
        String apiKey = getRepository().getProperty("enigma.api.key",
                            (String) null);
        if (apiKey == null) {
            System.err.println("Enignma:no api key");

            return null;
        }
        String url = "https://api.enigma.io/v2/data/" + apiKey + "/"
                     + tableId + "?conjunction=and";
        System.err.println("enignma url:" + url);

        return url;
    }




}
