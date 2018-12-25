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

package org.ramadda.data.point.hydro;


import org.ramadda.data.point.*;
import org.ramadda.data.point.text.*;
import org.ramadda.data.record.*;

import org.ramadda.util.WaterMLUtil;

import org.w3c.dom.*;



import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * Reads waterml xml files
 */
public class WaterMLFile extends PointFile {

    /** date parser */
    private SimpleDateFormat sdf;

    /** How many fields (lat,lon,...) before the data fields */
    private static final int OFFSET = 4;

    /** read from file */
    private double latitude  = 0,
                   longitude = 0,
                   altitude  = 0;

    /** _more_ */
    private List<RecordField> fields;

    /** time series values */
    private double[][] values;

    /** dates */
    private List<Date> dates;

    /**
     * The constructor
     *
     * @param filename file
     * @throws IOException On badness
     */
    public WaterMLFile(String filename) throws IOException {
        super(filename);
        sdf = makeDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    }


    /**
     * _more_
     *
     * @param visitInfo _more_
     *
     * @return _more_
     */
    public Record doMakeRecord(VisitInfo visitInfo) {
        DataRecord dataRecord = new DataRecord(this);
        if (fields == null) {
            doQuickVisit();
        }
        dataRecord.initFields(fields);

        return dataRecord;
    }


    /**
     * This  gets called before the file is visited. It reads the header and defines the fields
     *
     * @param visitInfo visit info
     * @return possible new visitinfo
     * @throws Exception On badness
     */
    @Override
    public VisitInfo prepareToVisit(VisitInfo visitInfo) throws Exception {

        if (fields != null) {
            return visitInfo;
        }
        fields = new ArrayList<RecordField>();
        Element root = XmlUtil.getRoot(getFilename(), getClass());

        List timeseriesNodes = XmlUtil.findChildren(root,
                                   WaterMLUtil.TAG_TIMESERIES);

        if (timeseriesNodes.size() == 0) {
            //TODO???
            return visitInfo;
        }

        int                numParams  = timeseriesNodes.size();
        int                numValues  = 0;
        List<List<Double>> valuesList = new ArrayList<List<Double>>();

        /*
      <geoLocation>
        <geogLocation xsi:type="LatLonPointType" srs="EPSG:4269">
          <latitude>41.643457</latitude>
          <longitude>-111.917649</longitude>
        </geogLocation>
        */

        Object[]   fileMetadata = null;

        String     latitude     = null;
        String     longitude    = null;
        String     elevation    = "0";
        List<List> listOfValues = new ArrayList<List>();

        for (int i = 0; i < timeseriesNodes.size(); i++) {
            Element timeSeriesNode = (Element) timeseriesNodes.get(i);
            Element sourceInfo = XmlUtil.findChild(timeSeriesNode,
                                     WaterMLUtil.TAG_SOURCEINFO);

            elevation = XmlUtil.getGrandChildText(sourceInfo,
                    WaterMLUtil.TAG_ELEVATION_M, elevation);

            if (latitude == null) {
                Element latitudeNode = XmlUtil.findDescendant(sourceInfo,
                                           WaterMLUtil.TAG_LATITUDE);
                Element longitudeNode = XmlUtil.findDescendant(sourceInfo,
                                            WaterMLUtil.TAG_LONGITUDE);
                if (latitudeNode != null) {
                    latitude = XmlUtil.getChildText(latitudeNode);
                }
                if (longitudeNode != null) {
                    longitude = XmlUtil.getChildText(longitudeNode);
                }
            }



            Element valuesNode = XmlUtil.findChild(timeSeriesNode,
                                     WaterMLUtil.TAG_VALUES);
            Element variable = XmlUtil.findChild(timeSeriesNode,
                                   WaterMLUtil.TAG_VARIABLE);

            //    <variable>
            //      <variableCode vocabulary="LBR" default="true" variableID="3">USU3</variableCode>
            //      <variableName>Battery voltage</variableName>
            //      <valueType>Field Observation</valueType>
            //      <dataType>Minimum</dataType>
            String variableCode = XmlUtil.getGrandChildText(variable,
                                      WaterMLUtil.TAG_VARIABLECODE, "");
            String variableName = XmlUtil.getGrandChildText(variable,
                                      WaterMLUtil.TAG_VARIABLENAME, "");
            String valueType = XmlUtil.getGrandChildText(variable,
                                   WaterMLUtil.TAG_VALUETYPE, "");
            String dataType = XmlUtil.getGrandChildText(variable,
                                  WaterMLUtil.TAG_DATATYPE, "");



            List values = XmlUtil.findChildren(valuesNode,
                              WaterMLUtil.TAG_VALUE);

            listOfValues.add(values);
            //            <siteName>Little Bear River at Mendon Road near Mendon, Utah</siteName>
            //<siteCode network="LBR" siteID="1">USU-LBR-Mendon</siteCode>
            String siteName = XmlUtil.getGrandChildText(sourceInfo,
                                  WaterMLUtil.TAG_SITENAME, "");
            String siteCode = XmlUtil.getGrandChildText(sourceInfo,
                                  WaterMLUtil.TAG_SITECODE, "");

            if (fileMetadata == null) {
                fileMetadata = new Object[] { siteCode, siteName };
                setFileMetadata(fileMetadata);
            }

            String varName = variableName + "-" + dataType;
            String var     = variableName + "-" + dataType;
            RecordField field = new RecordField(varName, var, var,
                                    i + 1 + OFFSET, "");
            field.setType(RecordField.TYPE_DOUBLE);
            field.setChartable(true);
            field.setSearchable(true);
            fields.add(field);
            numValues = Math.max(numValues, values.size());
        }

        RecordField dateField = new RecordField("date", "Date", "Date", 4,
                                    "");
        dateField.setType(RecordField.TYPE_DATE);
        fields.add(0, dateField);

        RecordField elevField = new RecordField("elevation", "Elevation",
                                    "Elevation", 3, "m");
        elevField.setDefaultDoubleValue(altitude =
            Double.parseDouble(elevation));
        elevField.setType(RecordField.TYPE_DOUBLE);
        fields.add(0, elevField);

        RecordField lonField = new RecordField("longitude", "longitude",
                                   "longitude", 2, "degrees");
        lonField.setDefaultDoubleValue(this.longitude =
            Double.parseDouble(longitude));
        lonField.setType(RecordField.TYPE_DOUBLE);
        fields.add(0, lonField);

        RecordField latField = new RecordField("latitude", "latitude",
                                   "latitude", 1, "degrees");
        latField.setDefaultDoubleValue(this.latitude =
            Double.parseDouble(latitude));
        latField.setType(RecordField.TYPE_DOUBLE);
        fields.add(0, latField);


        //This sets the getters
        for (RecordField recordField : fields) {
            DataRecord.initField(recordField);
        }
        setFields(fields);

        values = new double[numParams][];
        for (int i = 0; i < numParams; i++) {
            values[i] = new double[numValues];
        }

        for (int i = 0; i < listOfValues.size(); i++) {
            if (i == 0) {
                dates = new ArrayList<Date>();
            }
            List valueNodes = listOfValues.get(i);
            for (int valueIdx = 0; valueIdx < valueNodes.size(); valueIdx++) {
                //<value censorCode="nc" dateTime="2007-11-05T14:30:00" timeOffset="-07:00" dateTimeUTC="2007-11-05T21:30:00" methodCode="4" sourceCode="2" qualityControlLevelCode="0">13.33616</value>
                Element valueNode = (Element) valueNodes.get(valueIdx);

                if (i == 0) {
                    dates.add(sdf.parse(XmlUtil.getAttribute(valueNode,
                            WaterMLUtil.ATTR_DATETIMEUTC, "")));
                }
                double value =
                    Double.parseDouble(XmlUtil.getChildText(valueNode));
                values[i][valueIdx] = value;
            }
        }

        return visitInfo;


    }

    /**
     * _more_
     *
     * @param visitInfo _more_
     * @param record _more_
     *
     * @return _more_
     *
     * @throws IOException _more_
     */
    public Record.ReadStatus readNextRecord(VisitInfo visitInfo,
                                            Record record)
            throws IOException {
        visitInfo.addRecordIndex(1);
        int        currentIdx = visitInfo.getRecordIndex();
        DataRecord dataRecord = (DataRecord) record;
        if (currentIdx >= values[0].length) {
            return Record.ReadStatus.EOF;
        }
        for (int i = 0; i < values.length; i++) {
            dataRecord.setValue(i + 1 + OFFSET, values[i][currentIdx]);
        }

        dataRecord.setValue(4, dates.get(currentIdx));
        dataRecord.setLatitude(latitude);
        dataRecord.setLongitude(longitude);
        dataRecord.setAltitude(altitude);

        return Record.ReadStatus.OK;
    }


    /**
     * _more_
     *
     * @param action _more_
     *
     * @return _more_
     */
    public boolean isCapable(String action) {
        if (action.equals(ACTION_TIME)) {
            return true;
        }

        return super.isCapable(action);
    }


    /**
     * _more_
     *
     * @param args _more_
     */
    public static void main(String[] args) {
        PointFile.test(args, WaterMLFile.class);
    }

}
