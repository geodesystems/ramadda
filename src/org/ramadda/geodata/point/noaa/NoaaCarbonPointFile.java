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

package org.ramadda.geodata.point.noaa;

import org.ramadda.util.IO;
import org.ramadda.data.point.*;
import org.ramadda.data.point.text.*;
import org.ramadda.data.record.*;

import org.ramadda.util.Station;

import ucar.unidata.util.StringUtil;

import java.io.*;

import java.util.List;


/**
 */
public class NoaaCarbonPointFile extends NoaaPointFile {

    /** _more_ */
    private static int IDX = 1;

    /** _more_ */
    public static final int IDX_SITE_CODE = IDX++;

    /** _more_ */
    public static final int IDX_LATITUDE = IDX++;

    /** _more_ */
    public static final int IDX_LONGITUDE = IDX++;

    /** _more_ */
    public static final int IDX_YEAR = IDX++;

    /** _more_ */
    public static final int IDX_MONTH = IDX++;

    /** _more_ */
    public static final int IDX_DAY = IDX++;

    /** _more_ */
    public static final int IDX_HOUR = IDX++;



    /** _more_ */
    public static final int TYPE_HOURLY = 1;

    /** _more_ */
    public static final int TYPE_DAILY = 2;

    /** _more_ */
    public static final int TYPE_MONTHLY = 3;

    /** _more_ */
    int type = TYPE_HOURLY;


    /**
     * ctor
     *
     *
     *
     * @throws IOException On badness
     */
    public NoaaCarbonPointFile(IO.Path path) throws IOException {
        super(path);
    }


    /**
     * _more_
     *
     * @param visitInfo _more_
     *
     * @return _more_
     *
     *
     * @throws Exception _more_
     */
    @Override
    public VisitInfo prepareToVisit(VisitInfo visitInfo) throws Exception {
        super.prepareToVisit(visitInfo);
        String filename = getOriginalFilename(getFilename());

        if (filename.indexOf("_hour") >= 0) {
            type = TYPE_HOURLY;
        } else if (filename.indexOf("_month") >= 0) {
            type = TYPE_MONTHLY;
        } else if (filename.indexOf("_day") >= 0) {
            type = TYPE_DAILY;
        } else {
            throw new IllegalArgumentException("Unknown file:" + filename);
        }

        //[parameter]_[site]_[project]_[lab ID number]_[measurement group]_[optional qualifiers].txt
        List<String> toks = StringUtil.split(filename, "_", true, true);
        Station      station          = setLocation(toks.get(1));
        String       parameter        = toks.get(0);
        String       measurementGroup = toks.get(4);
        setFileMetadata(new Object[] { station.getId(), parameter,
                                       toks.get(2),  //project
                                       toks.get(3),  //lab id number
                                       measurementGroup, });
        if (type == TYPE_HOURLY) {
            setYMDHMSIndices(new int[] { IDX_YEAR, IDX_MONTH, IDX_DAY,
                                         IDX_HOUR });
            putFields(new String[] {
                makeField(FIELD_SITE_ID, attrType(RecordField.TYPE_STRING)),
                makeField(FIELD_LATITUDE,
                          attrValue("" + station.getLatitude())),
                makeField(FIELD_LONGITUDE,
                          attrValue("" + station.getLongitude())),
                makeField(FIELD_YEAR, ""), makeField(FIELD_MONTH, ""),
                makeField(FIELD_DAY, ""),
                makeField(FIELD_HOUR, attrType(RecordField.TYPE_STRING)),
                makeField(parameter, attrSortOrder(10), attrChartable(),
                          attrMissing(-999.990)),
                makeField(FIELD_STANDARD_DEVIATION, attrChartable(),
                          attrMissing(-99.990)),
                makeField(FIELD_NUMBER_OF_MEASUREMENTS, attrSortOrder(5),
                          attrChartable()),
                makeField(FIELD_QC_FLAG, attrType(RecordField.TYPE_STRING)),
                makeField(FIELD_INTAKE_HEIGHT),
                makeField(FIELD_INSTRUMENT, attrType(RecordField.TYPE_STRING)),
            });
        } else if (type == TYPE_DAILY) {
            setYMDHMSIndices(new int[] { IDX_YEAR, IDX_MONTH, IDX_DAY });
            putFields(new String[] {
                makeField(FIELD_SITE_ID, attrType(RecordField.TYPE_STRING)),
                makeField(FIELD_LATITUDE,
                          attrValue("" + station.getLatitude())),
                makeField(FIELD_LONGITUDE,
                          attrValue("" + station.getLongitude())),
                makeField(FIELD_YEAR, ""), makeField(FIELD_MONTH, ""),
                makeField(FIELD_DAY, ""),
                makeField(parameter, attrChartable(), attrSortOrder(5),
                          attrMissing(-999.990)),
                makeField(FIELD_STANDARD_DEVIATION, attrChartable(),
                          attrSortOrder(4), attrMissing(-99.990)),
                makeField(FIELD_NUMBER_OF_MEASUREMENTS, attrChartable(),
                          attrSortOrder(3)),
                makeField(FIELD_QC_FLAG, attrType(RecordField.TYPE_STRING)),
            });
        } else {
            setYMDHMSIndices(new int[] { IDX_YEAR, IDX_MONTH });
            putFields(new String[] {
                makeField(FIELD_SITE_ID, attrType(RecordField.TYPE_STRING)),
                makeField(FIELD_LATITUDE,
                          attrValue("" + station.getLatitude())),
                makeField(FIELD_LONGITUDE,
                          attrValue("" + station.getLongitude())),
                makeField(FIELD_YEAR, ""), makeField(FIELD_MONTH, ""),
                makeField(parameter, attrChartable(), attrMissing(-999.990)),
                makeField(FIELD_STANDARD_DEVIATION, attrChartable(),
                          attrMissing(-99.990)),
                makeField(FIELD_NUMBER_OF_MEASUREMENTS, attrChartable()),
                makeField(FIELD_QC_FLAG, attrType(RecordField.TYPE_STRING)),
            });
        }

        return visitInfo;
    }

    /**
     * _more_
     *
     * @param args _more_
     */
    public static void main(String[] args) {
        PointFile.test(args, NoaaCarbonPointFile.class);
    }

}
