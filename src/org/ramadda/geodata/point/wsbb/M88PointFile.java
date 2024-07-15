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


import org.ramadda.data.point.*;
import org.ramadda.data.point.text.*;

import org.ramadda.util.IO;
import org.ramadda.util.MyDateFormat;

import org.ramadda.data.record.*;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.io.*;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.List;



/**
 */
public class M88PointFile extends CsvFile {


    /** _more_ */
    private MyDateFormat sdfShort = makeDateFormat("yyyyMMdd");

    /** _more_ */
    private MyDateFormat sdfLong = makeDateFormat("yyyyMMdd HHmmss S");

    /** _more_ */
    public static final String FIELD_SURVEY_ID = "SURVEY_ID";

    /** _more_ */
    public static final String FIELD_ALT_BAROM = "ALT_BAROM";

    /** _more_ */
    public static final String FIELD_ALT_GPS = "ALT_GPS";

    /** _more_ */
    public static final String FIELD_ALT_RADAR = "ALT_RADAR";

    /** _more_ */
    public static final String FIELD_POS_TYPE = "POS_TYPE";

    /** _more_ */
    public static final String FIELD_LINEID = "LINEID";

    /** _more_ */
    public static final String FIELD_FIDUCIAL = "FIDUCIAL";

    /** _more_ */
    public static final String FIELD_TRK_DIR = "TRK_DIR";

    /** _more_ */
    public static final String FIELD_NAV_QUALCO = "NAV_QUALCO";

    /** _more_ */
    public static final String FIELD_MAG_TOTOBS = "MAG_TOTOBS";

    /** _more_ */
    public static final String FIELD_MAG_TOTCOR = "MAG_TOTCOR";

    /** _more_ */
    public static final String FIELD_MAG_RES = "MAG_RES";

    /** _more_ */
    public static final String FIELD_MAG_DECLIN = "MAG_DECLIN";

    /** _more_ */
    public static final String FIELD_MAG_HORIZ = "MAG_HORIZ";

    /** _more_ */
    public static final String FIELD_MAG_X_NRTH = "MAG_X_NRTH";

    /** _more_ */
    public static final String FIELD_MAG_Y_EAST = "MAG_Y_EAST";

    /** _more_ */
    public static final String FIELD_MAG_Z_VERT = "MAG_Z_VERT";

    /** _more_ */
    public static final String FIELD_MAG_INCLIN = "MAG_INCLIN";

    /** _more_ */
    public static final String FIELD_MAG_DICORR = "MAG_DICORR";

    /** _more_ */
    public static final String FIELD_IGRF_CORR = "IGRF_CORR";

    /** _more_ */
    public static final String FIELD_MAG_QUALCO = "MAG_QUALCO";

    /** _more_ */
    public static final String[] STRING_FIELDS = { FIELD_SURVEY_ID,
            FIELD_LINEID, FIELD_FIDUCIAL };

    /** _more_ */
    public static final String[] NOT_CHARTABLE_FIELDS = {
        FIELD_DATE, FIELD_TIME, FIELD_LATITUDE, FIELD_LONGITUDE,
        FIELD_ALT_BAROM, FIELD_ALT_GPS, FIELD_ALT_RADAR
    };

    /** _more_ */
    private int dateIdx = -1;

    /** _more_ */
    private int timeIdx = -1;

    /**
     * The constructor
     *
     * @throws IOException On badness
     */
    public M88PointFile(IO.Path path) throws IOException {
        super(path);
    }

    /**
     * Tell the record not to be picky about the tuples
     *
     * @param visitInfo _more_
     *
     * @return _more_
     */
    public BaseRecord doMakeRecord(VisitInfo visitInfo) {
        TextRecord record = (TextRecord) super.doMakeRecord(visitInfo);
        record.setBePickyAboutTokens(false);

        return record;
    }


    /**
     * This  gets called before the file is visited.
     * It reads the header and defines the fields
     *
     * @param visitInfo visit info
     * @return possible new visitinfo
     *
     * @throws Exception _more_
     */
    @Override
    public VisitInfo prepareToVisit(VisitInfo visitInfo) throws Exception {
        //Set the delimiter and how many lines in the header to skip
        putProperty(PROP_DELIMITER, "tab");
        putProperty(PROP_SKIPLINES, "1");
        super.prepareToVisit(visitInfo);


        //Read the header and make sure things are cool
        List<String> headerLines = getHeaderLines();
        if (headerLines.size() != getSkipLines(visitInfo)) {
            throw new IllegalArgumentException("Bad number of header lines:"
                    + headerLines.size());
        }

        List<String> fields = StringUtil.split(headerLines.get(0), "\t",
                                  true, false);
        StringBuffer sb       = new StringBuffer();
        int          fieldCnt = 0;
        for (String field : fields) {
            if (field.equals(FIELD_DATE)) {
                dateIdx = fieldCnt + 1;
            } else if (field.equals(FIELD_TIME)) {
                timeIdx = fieldCnt + 1;
            }
            fieldCnt++;
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(field);
            sb.append("[");
            boolean isString = false;
            for (String stringField : STRING_FIELDS) {
                if (field.equals(stringField)) {
                    sb.append(" type=string ");
                    isString = true;

                    break;
                }
            }
            boolean chartable = true;
            for (String stringField : NOT_CHARTABLE_FIELDS) {
                if (field.equals(stringField)) {
                    chartable = false;

                    break;
                }
            }

            if ( !isString && chartable) {
                sb.append(" chartable=true searchable=true ");
            }
            sb.append("]");
        }
        putProperty(PROP_FIELDS, sb.toString());

        return visitInfo;
    }

    /**
     * _more_
     *
     * @param action _more_
     *
     * @return _more_
     */
    public boolean isCapable(String action) {
        if (action.equals(ACTION_MAPINCHART)) {
            return true;
        }
        if (action.equals(ACTION_BOUNDINGPOLYGON)) {
            return true;
        }

        return super.isCapable(action);
    }



    /**
     * This gets called after a record has been read
     *
     * @param visitInfo _more_
     * @param record _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean processAfterReading(VisitInfo visitInfo, BaseRecord record)
            throws Exception {
        if ( !super.processAfterReading(visitInfo, record)) {
            return false;
        }
        if (dateIdx < 0) {
            return true;
        }
        TextRecord textRecord = (TextRecord) record;
        double     value      = textRecord.getValue(dateIdx);
        if (Double.isNaN(value)) {
            return true;
        }
        StringBuffer dttm = new StringBuffer();
        dttm.append((int) value);
        MyDateFormat sdf = sdfShort;
        if (timeIdx >= 0) {
            value = textRecord.getValue(timeIdx);
            if ( !Double.isNaN(value)) {
                System.err.println(value);
                int    hhmmss = (int) value;
                double rem    = value - hhmmss;
                dttm.append(" ");
                dttm.append(StringUtil.padLeft("" + hhmmss, 6, "0"));
                dttm.append(" ");
                dttm.append((int) (rem * 1000));
                sdf = sdfLong;
            }
        }

        Date date = sdfLong.parse(dttm.toString());
        record.setRecordTime(date.getTime());

        return true;
    }

    /**
     * _more_
     *
     * @param args _more_
     */
    public static void main(String[] args) {
        PointFile.test(args, M88PointFile.class);
    }

}
