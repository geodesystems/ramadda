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

package org.ramadda.plugins.socrata;


import org.json.*;

import org.ramadda.data.point.text.CsvFile;

import org.ramadda.data.record.*;
import org.ramadda.data.services.PointTypeHandler;

import org.ramadda.data.services.RecordTypeHandler;


import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Json;
import org.ramadda.util.TTLCache;
import org.ramadda.util.Utils;

import org.w3c.dom.*;
import org.w3c.dom.*;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.net.URL;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;


/**
 */
public class SocrataSeriesTypeHandler extends PointTypeHandler {


    /** _more_ */
    public static final String URL_METADATA =
        "https://${hostname}/api/views/${series_id}.json";


    /** _more_ */
    public static final String URL_CSV =
        "https://${hostname}/resource/${series_id}.csv?$limit=${limit}&$offset=${offset}";


    /** _more_ */
    public static final String URL_TEMPLATE =
        "https://${hostname}/api/views/${series_id}/rows.json?accessType=DOWNLOAD";

    /** _more_ */
    public static final String TYPE_SERIES = "type_socrata_series";

    /** _more_ */
    public static final int IDX_FIRST = RecordTypeHandler.IDX_LAST + 1;

    /** _more_ */
    public static final int IDX_REPOSITORY = IDX_FIRST;

    /** _more_ */
    public static final int IDX_SERIES_ID = IDX_FIRST + 1;


    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public SocrataSeriesTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public RecordFile doMakeRecordFile(Request request, Entry entry)
            throws Exception {

        String repository = entry.getValue(IDX_REPOSITORY, (String) null);
        String seriesId   = entry.getValue(IDX_SERIES_ID, (String) null);
        if ( !Utils.stringDefined(seriesId)
                || !Utils.stringDefined(repository)) {
            return null;
        }
        //Cap it for now at 10000
        int max = request.get(ARG_MAX, 1000);
        String url = URL_CSV.replace("${hostname}",
                                     repository).replace("${series_id}",
                                         seriesId).replace("${limit}",
                                             "" + max).replace("${offset}",
                                                 "0");
        //        System.err.println("Socrata data URL: " + url);
        CsvFile file   = new CsvFile(url);
        String  fields = (String) entry.getProperty("socrata.fields");
        if (fields == null) {
            String metadataUrl = URL_METADATA.replace("${hostname}",
                                     repository).replace("${series_id}",
                                         seriesId);
            //            System.err.println("Socrata metadata URL: " + metadataUrl);
            String       json      = IOUtil.readContents(metadataUrl);
            JSONObject   view      = new JSONObject(new JSONTokener(json));

            JSONArray    cols      = view.getJSONArray("columns");
            List<String> types     = new ArrayList<String>();
            List<String> fieldList = new ArrayList<String>();
            List<String> names     = new ArrayList<String>();
            for (int i = 0; i < cols.length(); i++) {
                JSONObject col = cols.getJSONObject(i);
                String     id  = col.get("fieldName").toString();
                names.add(id);
            }

            Hashtable<String, Integer> indexMap = new Hashtable<String,
                                                      Integer>();




            int idx = 0;
            idx = 0;
            for (String name : (List<String>) Misc.sort(names)) {
                if (name.startsWith(":")) {
                    continue;
                }
                //                System.err.println("name:" + name +" index:" + idx);
                indexMap.put(name, new Integer(idx));
                idx++;
            }

            for (int i = 0; i < cols.length(); i++) {
                JSONObject col = cols.getJSONObject(i);
                String     id  = col.get("fieldName").toString();
                if (id.startsWith(":")) {
                    continue;
                }

                String name = col.get("name").toString();
                name = name.replaceAll(",", " ");
                name = name.replaceAll("\"", "'");
                Integer index = indexMap.get(id);

                String  type  = col.get("dataTypeName").toString();
                if (type.equals("meta_data")) {
                    continue;
                }
                types.add(type);

                StringBuilder sb = new StringBuilder();
                if (false && type.equals("location")) {
                    //not now
                    sb.append(
                        "latitude[label=Latitude],longitude[label=Longitude]");
                } else {
                    sb.append(id);
                    sb.append("[");
                    sb.append(file.attrLabel(name));

                    //Not now as socrata doesn't sort the CSV anymore
                    //                    sb.append(file.attr("index", index.toString()));

                    if (type.equals("text")) {
                        sb.append(file.attrType(file.TYPE_STRING));
                    } else if (type.equals("location")) {
                        //For now
                        sb.append(file.attrType(file.TYPE_STRING));
                    } else if (type.equals("number")) {
                        sb.append(file.attrChartable());
                    } else if (type.equals("percent")) {
                        sb.append(file.attrChartable());
                        sb.append(file.attrUnit("%"));
                    } else if (type.equals("money")) {
                        sb.append(file.attrChartable());
                    } else if (type.equals("calendar_date")) {
                        sb.append(file.attrType(file.TYPE_DATE));
                        sb.append(file.attrFormat("yyyy-MM-dd'T'HH:mm:ss"));
                        //                        sb.append(file.attrFormat("MM/dd/yyyy"));
                    } else {
                        sb.append(file.attrType(file.TYPE_STRING));
                    }
                    sb.append("]");
                }
                //                System.err.println("Field:" + sb);
                fieldList.add(sb.toString());
            }
            //            System.err.println("Fields:" + fieldList);
            fields = file.makeFields(fieldList);
            entry.putProperty("socrata.fields", fields);
        }

        //fieldToIndex


        //        System.err.println("Fields:" + fields);
        file.putProperty("fields", fields);
        file.putProperty("picky", "false");
        //        file.putProperty("skiplines", "1");
        file.putProperty("matchupColumns", "true");
        file.putProperty("skiplines", "0");
        file.putProperty("output.latlon", "false");

        return file;


    }



}
