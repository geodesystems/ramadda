/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.socrata;


import org.json.*;

import org.ramadda.data.point.text.CsvFile;
import org.ramadda.data.point.text.TextRecord;

import org.ramadda.data.record.*;
import org.ramadda.data.services.PointTypeHandler;

import org.ramadda.data.services.RecordTypeHandler;


import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.TTLCache;
import org.ramadda.util.Utils;

import org.w3c.dom.*;
import org.w3c.dom.*;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.io.*;

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
    //        "https://${hostname}/resource/${series_id}.csv?$limit=${limit}&$offset=${offset}";
    "https://${hostname}/api/views/${series_id}/rows.csv?accessType=DOWNLOAD&limit=${limit}&offset=${offset}";



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
     * @param properties _more_
     * @param requestProperties _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public RecordFile doMakeRecordFile(Request request, Entry entry,
                                       Hashtable properties,
                                       Hashtable requestProperties)
            throws Exception {

        String repository = entry.getStringValue(request,IDX_REPOSITORY, (String) null);
        String seriesId   = entry.getStringValue(request,IDX_SERIES_ID, (String) null);
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
        SimpleSocrataFile file = new SimpleSocrataFile(new IO.Path(url));
        String fields          = (String) entry.getProperty("socrata.fields");
        Integer locationIndex =
            (Integer) entry.getProperty("socrata.locationIndex");
        if (locationIndex != null) {
            file.locationIndex = locationIndex.intValue();
        }

        if (fields == null) {
            String metadataUrl = URL_METADATA.replace("${hostname}",
                                     repository).replace("${series_id}",
                                         seriesId);
            //            System.err.println("Socrata metadata URL: " + metadataUrl);
            String       json = IOUtil.readContents(metadataUrl);
            JSONObject view = new JSONObject(new JSONTokener(json));

            JSONArray    cols      = view.getJSONArray("columns");
            List<String> types     = new ArrayList<String>();
            List<String> fieldList = new ArrayList<String>();
            List<String> names     = new ArrayList<String>();
            Hashtable<String, Integer> indexMap = new Hashtable<String,
                                                      Integer>();
            int fieldCnt = 0;
            for (int i = 0; i < cols.length(); i++) {
                JSONObject col = cols.getJSONObject(i);
                String     id  = col.get("fieldName").toString();
                names.add(id);
                if ( !id.startsWith(":")) {
                    indexMap.put(id, Integer.valueOf(col.optInt("position", -1)));
                    fieldCnt++;
                }
            }
            //RecordField []fieldArray = new RecordField[fieldCnt];


            /*
            int idx = 0;
            idx = 0;
            for (String name : (List<String>) Misc.sort(names)) {
                if (name.startsWith(":")) {
                    continue;
                }
                //                System.err.println("name:" + name +" index:" + idx);
                indexMap.put(name, Integer.valueOf(idx));
                idx++;
            }
            */

            boolean didLocation = false;
            for (int i = 0; i < cols.length(); i++) {
                JSONObject col = cols.getJSONObject(i);
                String     id  = col.get("fieldName").toString();
                if (id.startsWith(":")) {
                    continue;
                }

                String name = col.get("name").toString().trim();
                name = name.replaceAll(",", " ");
                name = name.replaceAll("\"", "'");


                String type = col.get("dataTypeName").toString();
                if (type.equals("meta_data")) {
                    continue;
                }
                types.add(type);

                Integer index = indexMap.get(id);
                if ((index != null) && !didLocation
                        && type.equals("location")) {
                    didLocation        = true;
                    file.locationIndex = index.intValue() - 1;
                    entry.putProperty("socrata.locationIndex",
                                      Integer.valueOf(file.locationIndex));
                    Utils.add(
                        fieldList, "location[label=Location type=string]",
                        "latitude[label=Latitude type=double islatitude=true]",
                        "longitude[label=Longitude type=double islongitude=true]");
                } else {
                    StringBuilder sb = new StringBuilder();
                    sb.append(id);
                    sb.append("[");
                    sb.append(file.attrLabel(name));

                    //Not now as socrata doesn't sort the CSV anymore
                    //                    sb.append(file.attr("index", index.toString()));

                    if (type.equals("text")) {
                        sb.append(file.attrType(RecordField.TYPE_STRING));
                    } else if (type.equals("location")) {
                        //For now
                        sb.append(file.attrType(RecordField.TYPE_STRING));
                    } else if (type.equals("number")) {
                        sb.append(file.attrChartable());
                    } else if (type.equals("percent")) {
                        sb.append(file.attrChartable());
                        sb.append(file.attrUnit("%"));
                    } else if (type.equals("money")) {
                        sb.append(file.attrChartable());
                    } else if (type.equals("calendar_date")) {
                        sb.append(file.attrType(RecordField.TYPE_DATE));
                        sb.append(file.attrFormat("yyyy-MM-dd'T'HH:mm:ss"));
                        //                        sb.append(file.attrFormat("MM/dd/yyyy"));
                    } else {
                        sb.append(file.attrType(RecordField.TYPE_STRING));
                    }
                    sb.append("]");
                    fieldList.add(sb.toString());
                }

            }
            //            System.err.println(fieldList);
            fields = file.makeFields(fieldList);
            entry.putProperty("socrata.fields", fields);
        }
        file.putProperty("fields", fields);
        file.putProperty("picky", "false");
        //        file.putProperty("skiplines", "1");
        file.putProperty("matchupColumns", "true");
        file.putProperty("skiplines", "0");
        file.putProperty("output.latlon", "false");

        return file;


    }


    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Jan 4, '19
     * @author         Enter your name here...
     */
    private static class SimpleSocrataFile extends CsvFile {

        /** _more_ */
        int locationIndex = -1;

        /**
         * ctor
         *
         * @param filename _more_
         *
         * @throws IOException _more_
         */
        public SimpleSocrataFile(IO.Path path) throws IOException {
            super(path);
        }

        /**
         * _more_
         *
         * @param record _more_
         * @param toks _more_
         * @param header _more_
         *
         * @return _more_
         */
        @Override
        public List<String> processTokens(TextRecord record,
                                          List<String> toks, boolean header) {
            if (locationIndex < 0) {
                return super.processTokens(record, toks, header);
            }
            List<String> newToks = new ArrayList<String>();
            for (int i = 0; i < toks.size(); i++) {
                if (i != locationIndex) {
                    newToks.add(toks.get(i));
                } else {
                    if (header) {
                        newToks.add("location");
                        newToks.add("latitude");
                        newToks.add("longitude");

                        continue;
                    }

                    String tok   = toks.get(i);
                    int    index = tok.indexOf("(");
                    if (index < 0) {
                        newToks.add(tok);
                        newToks.add("NaN");
                        newToks.add("NaN");

                        //                        System.err.println("bad index1:" + tok);
                        continue;
                    }
                    newToks.add(tok);
		    String location = tok.replace("(","").replace(")","");
		    List<String> locToks = Utils.split(location,",");
                    if (locToks.size()!=2) {
                        newToks.add("NaN");
                        newToks.add("NaN");
                        continue;
                    }
                    newToks.add(locToks.get(0).trim());
                    newToks.add(locToks.get(1).trim());
                }
            }

            return newToks;
        }

    }

}
