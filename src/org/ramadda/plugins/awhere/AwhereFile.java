/*
* Copyright (c) 2008-2018 Geode Systems LLC
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

import org.ramadda.data.point.*;
import org.ramadda.data.point.text.*;
import org.ramadda.data.record.*;
import org.ramadda.repository.Entry;
import org.ramadda.repository.RepositoryUtil;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Json;
import org.ramadda.util.Utils;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.StringUtil;




import java.io.*;

import java.net.URL;

import java.text.SimpleDateFormat;

import java.util.ArrayList;

import java.util.Date;
import java.util.List;
import java.util.TimeZone;


/**
 */
public class AwhereFile extends CsvFile {

    /** _more_ */
    private byte[] buffer;

    /**
     *     ctor
     *
     *     @param filename _more_
     *
     *     @throws IOException _more_
     */
    public AwhereFile(String filename) throws IOException {
        super(filename);
    }

    /**
     * _more_
     *
     * @param buffered _more_
     *
     * @return _more_
     *
     * @throws IOException _more_
     */
    @Override
    public InputStream doMakeInputStream(boolean buffered)
            throws IOException {
        try {
            System.err.println("AwhereFile.doMakeInputStream");
            byte[] data = readData();
            if (data == null) {
                System.err.println(
                    "AwhereFile.doMakeInputStream - data is null");

                return null;
            }

            return new ByteArrayInputStream(data);
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }


    /**
     * Gets called when first reading the file. Parses the header
     *
     * @param visitInfo visit info
     *
     * @return the visit info
     *
     *
     * @throws Exception _more_
     */
    @Override
    public VisitInfo prepareToVisit(VisitInfo visitInfo) throws Exception {
        if (getProperty(PROP_FIELDS, (String) null) == null) {
            String format = "yyyy-MM-dd";
            putFields(new String[] {
                makeField(FIELD_DATE, attrType("date"), attrFormat(format)),
                makeField("value", attrLabel("Value"), attrChartable(),
                          attrMissing(-999999.99)), });
        }
        super.prepareToVisit(visitInfo);

        return visitInfo;
    }

    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private byte[] readData() throws Exception {

        if (buffer != null) {
            return buffer;
        }
        System.err.println("AwhereFile.readData");
        Entry        entry  = (Entry) getProperty("entry");
        String       format = "yyyy-MM-dd'T'HH:mm:ss";
        List<String> fields = new ArrayList<String>();
        fields.add(makeField(FIELD_DATE, attrType("date"),
                             attrFormat(format)));
        fields.add(makeField("conditions_code", attrLabel("Conditions Code"),
                             attrType("string")));
        fields.add(makeField("conditions", attrLabel("Conditions"),
                             attrType("string")));
        fields.add(makeField("temperature_max", attrLabel("Max Temperature"),
                             attrChartable(), attrUnit("C")));
        fields.add(makeField("temperature_min", attrLabel("Min Temperature"),
                             attrChartable(), attrUnit("C")));
        fields.add(makeField("precipitation_amount",
                             attrLabel("Precipitation"), attrChartable(),
                             attrUnit("mm")));
        fields.add(makeField("precipitation_chance",
                             attrLabel("Precipitation Chance"),
                             attrChartable(), attrUnit("%")));
        fields.add(makeField("cloudCover", attrLabel("Cloud Cover"),
                             attrChartable(), attrUnit("")));
        fields.add(makeField("sunshine", attrLabel("Sunshine"),
                             attrChartable(), attrUnit("")));
        fields.add(makeField("solar_amount", attrLabel("Solar Amount"),
                             attrChartable(), attrUnit("Wh/m^2")));
        fields.add(makeField("rh_average", attrLabel("RH Avg"),
                             attrChartable(), attrUnit("%")));
        fields.add(makeField("rh_max", attrLabel("RH Max"), attrChartable(),
                             attrUnit("%")));
        fields.add(makeField("rh_min", attrLabel("RH Min"), attrChartable(),
                             attrUnit("%")));
        fields.add(makeField("wind_average", attrLabel("Wind Avg"),
                             attrChartable(), attrUnit("m/s")));
        fields.add(makeField("wind_max", attrLabel("Wind Max"),
                             attrChartable(), attrUnit("m/s")));
        fields.add(makeField("wind_min", attrLabel("Wind Min"),
                             attrChartable(), attrUnit("m/s")));
        fields.add(makeField("dewPoint", attrLabel("Dewpoint"),
                             attrChartable(), attrUnit("C")));

        String fieldId = entry.getValue(AwhereFieldTypeHandler.IDX_FIELD_ID,
                                        null);
        //        System.err.println("Reading aWhere time series for field: "   + fieldId);
        StringBuilder sb = new StringBuilder();
        if ( !Utils.stringDefined(fieldId)) {
            fieldId = "";
            //            return null;
        }

        double[] center = entry.getCenter();
        System.err.println("forecast URL:"
                           + Awhere.getForecastsUrl(fieldId, center[0],
                               center[1]));
        String json = Awhere.doGet(entry,
                                   new URL(Awhere.getForecastsUrl(fieldId,
                                       center[0], center[1])));
        //        System.err.println("forecast json:" + json);
        JSONObject obj       = new JSONObject(new JSONTokener(json));
        JSONArray  forecasts = obj.getJSONArray("forecasts");
        for (int i = 0; i < forecasts.length(); i++) {
            JSONArray times =
                forecasts.getJSONObject(i).getJSONArray("forecast");
            for (int j = 0; j < times.length(); j++) {
                List<String> row  = new ArrayList<String>();
                JSONObject   fcst = times.getJSONObject(j);
                //                Date end = DateUtil.parse(Json.readValue(fcst, "endTime",""));
                row.add(Json.readValue(fcst, "startTime", ""));
                row.add(HtmlUtils.quote(Json.readValue(fcst,
                        "conditionsCode", "")));
                row.add(HtmlUtils.quote(Json.readValue(fcst,
                        "conditionsText", "")));
                row.add(Json.readValue(fcst, "temperatures.max", ""));
                row.add(Json.readValue(fcst, "temperatures.min", ""));
                row.add(Json.readValue(fcst, "precipitation.amount", ""));
                row.add(Json.readValue(fcst, "precipitation.chance", ""));
                row.add(Json.readValue(fcst, "sky.cloudCover", ""));
                row.add(Json.readValue(fcst, "sky.sunshine", ""));
                row.add(Json.readValue(fcst, "solar.amount", ""));
                row.add(Json.readValue(fcst, "relativeHumidity.average", ""));
                row.add(Json.readValue(fcst, "relativeHumidity.max", ""));
                row.add(Json.readValue(fcst, "relativeHumidity.min", ""));
                row.add(Json.readValue(fcst, "wind.average", ""));
                row.add(Json.readValue(fcst, "wind.max", ""));
                row.add(Json.readValue(fcst, "wind.min", ""));
                row.add(Json.readValue(fcst, "dewPoint.amount", ""));
                sb.append(StringUtil.join(",", row));
                sb.append("\n");
            }
        }

        //        System.err.println(sb);

        buffer = sb.toString().getBytes();


        putProperty(PROP_FIELDS, makeFields(fields));

        return buffer;

    }

    /*
forecast":[{
"startTime":"{startTime}",
"endTime":"{endTime}",
"conditionsCode":"{conditionsCode}",
"conditionsText":"{conditionsText}",
"temperatures":{
"max":{maxTemp},
"min":{minTemp},
"units":"{tempUnits}"
},
"precipitation":{
"amount":{precipitation},
"units":"{precipUnits}",
"chance":{chanceOfRain}"
                       },
             "sky":{
                 "cloudCover":{skyCover},
                     "sunshine":{skyClear},
                         },
             "solar":{
                 "amount":{solar},
                     "units":"{solarUnits}"
                         },
             "relativeHumidity":{
                 "average":{humidity},
                     "max": {maxHumidity},
                         "min":{minHumidity}
             },
             "wind":{
                 "average":{averageWind},
                     "max": {maxWind},
                         "min":{minWind},
                             "units":"{windUnits}"
                                 },
             "dewPoint": {
                 "amount":{dewPoint},
                     "units":"{dewPointUnits}"
                         }

             },{

 JSONObject view = Json.readObject(obj, "meta.view");
 JSONArray    cols   = view.getJSONArray("columns");
 JSONArray    data   = obj.getJSONArray("data");

 List<String> types  = new ArrayList<String>();
 List<String> fields = new ArrayList<String>();
 for (int i = 0; i < cols.length(); i++) {
     JSONObject col  = cols.getJSONObject(i);
     String     name = col.get("name").toString();
     }
 }
 **/








    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        PointFile.test(args, AwhereFile.class);
    }

}
