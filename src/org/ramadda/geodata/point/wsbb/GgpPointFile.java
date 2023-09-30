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

package org.ramadda.geodata.point.wsbb;


import org.ramadda.util.IO;
import org.ramadda.data.point.*;
import org.ramadda.data.point.text.*;
import org.ramadda.data.record.*;
import org.ramadda.util.Utils;
import org.ramadda.util.geo.GeoUtils;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.io.*;

import java.util.List;


/**
 * A file reader for the Global Geodynamics GGP format
 * This is defined as an entry type in the point plugin
 * Look in org/ramadda/geodata/point/wsbb/wsbbtypes.xml
 */
public class GgpPointFile extends CsvFile {

    /** what to use for missing numeric metadata */
    public static final double DOUBLE_UNDEFINED = 0;

    /**
     * header delimiter.
     *   If the delimiter starts with "starts:" then the TextFile
     *   checks the line accordingly. Else it checks for equality.
     */
    public static final String HEADER_DELIMITER = "starts:C***********";

    /** skip lines that equal */
    public static final String BLOCK_START = "77777777";

    /** skip lines that equal */
    public static final String BLOCK_END = "88888888";

    /** skip lines that equal */
    public static final String FILE_END = "99999999";

    /** missing value */
    public static final double MISSING = 99999.999;

    /**
     * The constructor
     *
     * @throws IOException On badness
     */
    public GgpPointFile(IO.Path path) throws IOException {
        super(path);
    }

    /**
     * In the GGP format there can be blocks of delimited data
     *
     * @param line line of text
     *
     * @return is this good data
     */
    @Override
    public boolean isLineValidData(String line) {
        if (line.startsWith(BLOCK_START)) {
            return false;
        }
        if (line.startsWith(BLOCK_END)) {
            return false;
        }
        if (line.startsWith("INSTR")) {
            return false;
        }
        if (line.startsWith(FILE_END)) {
            return false;
        }

        return true;
    }

    /**
     * This  gets called before the file is visited. It reads the header and defines the fields
     *
     * @param visitInfo visit info
     * @return possible new visitinfo
     *
     * @throws Exception On badness
     */
    @Override
    public VisitInfo prepareToVisit(VisitInfo visitInfo) throws Exception {

        StringBuffer desc = new StringBuffer();
        //Set some of the properties
        putProperty(PROP_DELIMITER, " ");
        putProperty(PROP_HEADER_DELIMITER, HEADER_DELIMITER);
        putProperty(PROP_DATEFORMAT, "yyyyMMdd HHmmss");

        //Read the header
        super.prepareToVisit(visitInfo);

        //Pull the metadata from the header
        List<String> headerLines         = getHeaderLines();
        String       station             = "",
                     instrument          = "",
                     author              = "";
        double       timeDelay           = 0,
                     gravityCalibration  = 0,
                     pressureCalibration = 0;
        double       latitude            = 0,
                     longitude           = 0,
                     elevation           = 0;

        String       gravityUnit         = "V";
        String       pressureUnit        = "hPa";

        //The header lines can be in different order so look at each one
        for (String line : headerLines) {
            if (line.indexOf("yyyymmdd") >= 0) {
                gravityUnit = StringUtil.findPattern(line,
                        ".*gravity\\(([^\\)]+)\\).*");
                pressureUnit = StringUtil.findPattern(line,
                        ".*pressure\\(([^\\)]+)\\).*");
                if (gravityUnit == null) {
                    System.err.println("ggp: could not read gravity unit:"
                                       + line);
                    gravityUnit = "V";
                }
                if (pressureUnit == null) {
                    System.err.println("ggp: could not read pressure unit:"
                                       + line);
                    pressureUnit = "hPa";
                }

                continue;
            }

            List<String> toks = StringUtil.splitUpTo(line, ":", 2);
            if (toks.size() == 2) {
                String name  = toks.get(0);
                String value = toks.get(1).trim();
                if (name.indexOf("Station") >= 0) {
                    station = value;
                    //clean up the station
                    station = station.replaceAll(",", " - ");
                } else if (name.indexOf("Instrument") >= 0) {
                    instrument = value;
                    //clean up the instrument
                    instrument = instrument.replaceAll(",", " - ");
                } else if (name.indexOf("Author") >= 0) {
                    author = value;
                    author = author.replaceAll(",", " - ");
                } else if (name.indexOf("Latitude") >= 0) {
                    latitude = GeoUtils.decodeLatLon(value);
                } else if (name.indexOf("Longitude") >= 0) {
                    longitude = GeoUtils.decodeLatLon(value);
                } else if (name.indexOf("Gravity Cal") >= 0) {
                    gravityCalibration = parseDouble(value);
                } else if (name.indexOf("Pressure Cal") >= 0) {
                    pressureCalibration = parseDouble(value);
                } else if (name.indexOf("Height") >= 0) {
                    elevation = parseDouble(value);
                } else {
                    //System.err.println("Unknown:" + line);
                }
            } else {
                //I've seen some files with comment lines. 
                if (line.startsWith("#")) {
                    line = line.substring(1);
                    desc.append(line);
                    desc.append("\n");
                }
            }
        }

        setDescriptionFromFile(desc.toString());
        setLocation(latitude, longitude, elevation);

        //this needs to be in the same order as the wsbbtypes.xml in the point plugin
        setFileMetadata(new Object[] {
            station, instrument, author, Double.valueOf(timeDelay),
            Double.valueOf(gravityCalibration),  Double.valueOf(pressureCalibration)
        });

        //Define the fields
        //Note: The first fields (site, lat, lon, elev) aren't in the data rows
        //We define that there are fields but they have a fixed value.

        putFields(new String[] {
            //Embed the values for site, lat, lon and elevation
            makeField(FIELD_STATION, attrType(RecordField.TYPE_STRING),
                      attrValue(station.trim())),
            makeField(FIELD_LATITUDE, attrValue(latitude)),
            makeField(FIELD_LONGITUDE, attrValue(longitude)),
            makeField(FIELD_ELEVATION, attrValue(elevation)),
            makeField(FIELD_DATE,
                      attrType(RecordField.TYPE_STRING) + attr("isdate", "true")
                      + attrWidth(8)),
            makeField(FIELD_TIME,
                      attrType(RecordField.TYPE_STRING) + attr("istime", "true")
                      + attrWidth(7)),
            //TODO: What is the unit for gravity and pressure
            makeField("gravity", attrUnit(gravityUnit), attrChartable(),
                      attrMissing(MISSING) + attrWidth(10)),
            makeField("pressure", attrUnit(pressureUnit), attrChartable(),
                      attrMissing(MISSING) + attrWidth(10)),
        });

        return visitInfo;

    }


    /**
     * Override the base file reader method that gets the date-time string from the field values
     * We do this because some of the GGP files did not pad their hhmmss column with "0"
     * @param record data record that got read
     * @param dttm buffer to set the date string with
     * @param dateIndex the date column
     * @param timeIndex the time column
     *
     * @throws Exception on badness
     */
    @Override
    public void getDateTimeString(BaseRecord record, StringBuffer dttm,
                                  int dateIndex, int timeIndex)
            throws Exception {
        dttm.append(getString(record, dateIndex));
        dttm.append(" ");
        String timeField = getString(record, timeIndex).trim();
        //Account for one of the non padded hhmmss formats
        while (timeField.length() < 6) {
            timeField = "0" + timeField;
        }
        dttm.append(timeField);

    }

    /**
     * utility to parse a double from the header
     *
     * @param s string value
     *
     * @return double value if s is defined. else the DOUBLE_DEFINED
     */
    private double parseDouble(String s) {
        if (Utils.stringDefined(s)) {
            int index = s.indexOf(" ");
            if (index >= 0) {
                s = s.substring(0, index).trim();
            }

            return Double.parseDouble(s);
        }

        return DOUBLE_UNDEFINED;
    }


    /**
     * Test main
     *
     * @param args cmd line args
     */
    public static void main(String[] args) {
        PointFile.test(args, GgpPointFile.class);
    }

}
