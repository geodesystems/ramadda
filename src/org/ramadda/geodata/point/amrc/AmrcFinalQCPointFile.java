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

package org.ramadda.geodata.point.amrc;

import org.ramadda.util.IO;
import org.ramadda.util.MyDateFormat;

import org.ramadda.data.point.*;
import org.ramadda.data.point.text.*;
import org.ramadda.data.record.*;

import ucar.unidata.util.StringUtil;

import java.io.*;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.List;


/**
 * This class reads the QC'ed meteorolgical data from the Antarctic Meteorology Research Center (AMRC).
 *
 * The AMRC data is of the form:
 * <pre>
 * Year: 2011  Month: 08  ID: KMS  ARGOS: 21364  Name: Kominko-Slade
 * Lat: 79.47S  Lon: 112.11W  Elev: 1801m
 * 2011 213  8  1 0000  -19.5  806.9  444.0  444.0   88.3   -0.2
 * </pre>
 *
 * This class overwrites prepareToVisit to read the metadata from the header
 * and define the fields (i.e.,  the data dictionary).
 * The data that the API produces looks like:
 * site_id,   latitude, longitude, elevation, year, julian day, month, day, ...
 *
 * It overwrites processAfterReading to set the Date/time of the observation
 *
 * You can run this from the command line with:
 * java org.ramadda.data.point.amrc.AmrcFinalQCFile file.txt
 *
 * There is a example data file in the source:
 * org/ramadda/geodata/point/amrc/exampleamrcqc.txt
 *
 * To make the data type available to RAMADDA define a type definition plugin file like:
 * org/ramadda/geodata/point/amrc/amrctypes.xml
 *
 * You should then be able to harvest or upload a point file of your choice.
 */
public class AmrcFinalQCPointFile extends CsvFile {

    /** base index to count from */
    private static int IDX = 1;

    /** data index */
    public static final int IDX_SITE_ID = IDX++;

    /** data index */
    public static final int IDX_LATITUDE = IDX++;

    /** data index */
    public static final int IDX_LONGITUDE = IDX++;

    /** data index */
    public static final int IDX_ELEVATION = IDX++;

    /** data index */
    public static final int IDX_YEAR = IDX++;

    /** data index */
    public static final int IDX_JULIAN_DAY = IDX++;

    /** data index */
    public static final int IDX_MONTH = IDX++;

    /** data index */
    public static final int IDX_DAY = IDX++;

    /** data index */
    public static final int IDX_TIME = IDX++;

    /** missing value */
    public static final double MISSING = 444.0;

    /** date formatter */
    private MyDateFormat sdf = makeDateFormat("yyyy-MM-dd HHmm");

    /** buffer */
    private StringBuffer dttm = new StringBuffer();

    /**
     * The constructor
     *
     * @throws IOException On badness
     */
    public AmrcFinalQCPointFile(IO.Path path) throws IOException {
        super(path);
    }


    /**
     * This  gets called before the file is visited. It reads the header and defines the fields
     *
     * @param visitInfo visit info
     * @return possible new visitinfo
     *
     * @throws Exception on badness
     */
    @Override
    public VisitInfo prepareToVisit(VisitInfo visitInfo) throws Exception {
        //Set the delimiter and how many lines in the header to skip
        putProperty(PROP_DELIMITER, " ");
        putProperty(PROP_SKIPLINES, "2");

        //Read the header and make sure things are cool
        super.prepareToVisit(visitInfo);
        List<String> headerLines = getHeaderLines();
        if (headerLines.size() != getSkipLines(visitInfo)) {
            throw new IllegalArgumentException("Bad number of header lines:"
                    + headerLines.size());
        }

        //The header looks like:
        //        Year: 2012  Month: 01  ID: AG4  ARGOS:  8927  Name: AGO-4               
        //            Lat: 82.01S  Lon:  96.76E  Elev: 3597m

        //Extract the metadata
        String siteId = StringUtil.findPattern(headerLines.get(0),
                            "ID:\\s(.*)ARGOS:");
        String argosId = StringUtil.findPattern(headerLines.get(0),
                             "ARGOS:\\s*(.*)Name:");
        String siteName = StringUtil.findPattern(headerLines.get(0),
                              "Name:\\s(.*)");
        if ((siteName == null) || (siteId == null)) {
            throw new IllegalArgumentException("Could not read header:"
                    + headerLines + " site name:" + siteName + " site id:"
                    + siteId);
        }

        //This needs to be in the same order as the amrctypes.xml defines in the point plugin
        setFileMetadata(new Object[] { siteId, siteName, argosId });

        //Note: The first fields (site, lat, lon, elev) aren't in the data rows
        //We define that there are fields but they have a fixed value derived from
        //a regexp pattern applied to the header 
        putFields(new String[] {
            makeField(FIELD_SITE_ID, attrType(RecordField.TYPE_STRING),
                      attrPattern("ID:\\s(.*)ARGOS:")),
            makeField(FIELD_LATITUDE, attrPattern("Lat:\\s(.*)Lon:")),
            makeField(FIELD_LONGITUDE, attrPattern("Lon:\\s(.*)Elev:")),
            makeField(FIELD_ELEVATION, attrPattern("Elev:(.*)")),
            makeField(FIELD_YEAR, ""), makeField(FIELD_JULIAN_DAY, ""),
            makeField(FIELD_MONTH, ""), makeField(FIELD_DAY, ""),
            makeField(FIELD_TIME, attrType(RecordField.TYPE_STRING)),
            makeField(FIELD_TEMPERATURE, attrUnit(UNIT_CELSIUS),
                      attrChartable(), attrMissing(MISSING)),
            makeField(FIELD_PRESSURE, attrUnit(UNIT_HPA), attrChartable(),
                      attrMissing(MISSING)),
            makeField(FIELD_WIND_SPEED, attrUnit(UNIT_M_S), attrChartable(),
                      attrMissing(MISSING)),
            makeField(FIELD_WIND_DIRECTION, attrUnit(UNIT_DEGREES),
                      attrMissing(MISSING)),
            makeField(FIELD_RELATIVE_HUMIDITY, attrUnit(UNIT_PERCENT),
                      attrChartable(), attrMissing(MISSING)),
            makeField(FIELD_DELTA_T, attrUnit(UNIT_CELSIUS), attrChartable(),
                      attrMissing(MISSING)),
        });

        return visitInfo;
    }


    /**
     * This gets called after a record has been read.
     * The base TextRecord/CsvFile can handle [date,time] and [year,month,day,hour,minute,second] columns.
     * Just not [year,month,day,hhmm] for now
     *
     * @param visitInfo Contains record visit information
     * @param record The record
     *
     * @return Should we continue processing the file
     *
     * @throws Exception on badness
     */
    @Override
    public boolean processAfterReading(VisitInfo visitInfo, BaseRecord record)
            throws Exception {
        if ( !super.processAfterReading(visitInfo, record)) {
            return false;
        }
        TextRecord textRecord = (TextRecord) record;
        dttm.setLength(0);
        dttm.append((int) textRecord.getValue(IDX_YEAR));
        dttm.append("-");
        dttm.append((int) textRecord.getValue(IDX_MONTH));
        dttm.append("-");
        dttm.append((int) textRecord.getValue(IDX_DAY));
        dttm.append(" ");
        dttm.append(textRecord.getStringValue(IDX_TIME));
        Date date = sdf.parse(dttm.toString());
        record.setRecordTime(date.getTime());

        return true;
    }

    /**
     * command line test
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        PointFile.test(args, AmrcFinalQCPointFile.class);
    }

}
