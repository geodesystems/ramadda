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

package org.ramadda.geodata.point.above;


import org.ramadda.util.IO;
import org.ramadda.util.MyDateFormat;
import org.ramadda.data.point.*;
import org.ramadda.data.point.text.*;

import org.ramadda.data.record.*;
import org.ramadda.util.Utils;
import org.ramadda.util.geo.GeoUtils;


import ucar.unidata.util.IOUtil;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.io.*;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;


/**
 * A file reader for the Level 2 Ameriflux CSV data format
 */
@SuppressWarnings("unchecked")
public class AmerifluxLevel2PointFile extends CsvFile {

    /** _more_ */
    private MyDateFormat sdf = makeDateFormat("yyyy-D HHmm");

    /**
     * ctor
     *
     *
     * @throws IOException _more_
     */
    public AmerifluxLevel2PointFile(IO.Path path) throws IOException {
        super(path);
    }

    /**
     *
     * @param visitInfo holds visit info
     *
     * @return visit info
     *
     *
     * @throws Exception _more_
     */
    @Override
    public VisitInfo prepareToVisit(VisitInfo visitInfo) throws Exception {

        putProperty(PROP_DELIMITER, ",");
        putProperty(PROP_SKIPLINES, "20");
        super.prepareToVisit(visitInfo);

        //Example file at examples/AMF_USBar_2004_L2_WG_V004.example.csv
        List<String> header = getHeaderLines();

        //        System.err.println("DESC:"
        //                           + StringUtil.join("\n", header.subList(5, 16)));
        setDescriptionFromFile(StringUtil.join("\n", header.subList(5, 16)));


        //        Sitename: UCI 1930 Canada
        List<String> toks   = StringUtil.splitUpTo(header.get(0), ":", 2);
        String       siteId = toks.get(1);

        //        Location: Latitude: 55.9058  Longitude: -98.5247  Elevation (masl): 257
        String locationLine = header.get(1);
        String latString = StringUtil.findPattern(locationLine,
                               "Latitude:\\s*([\\-0-9\\.]+)\\s+");
        String lonString = StringUtil.findPattern(locationLine,
                               ".*Longitude:\\s*([\\-0-9\\.]+)\\s+");
        String elevationString = StringUtil.findPattern(locationLine,
                                     "Elevation\\s*\\(.*\\):\\s+(\\d+)");

        if (latString == null) {
            throw new IllegalArgumentException("Could not read latitude:"
                    + locationLine);
        }

        if (lonString == null) {
            throw new IllegalArgumentException("Could not read longitude:"
                    + locationLine);
        }
        if (elevationString == null) {
            throw new IllegalArgumentException("Could not read elevation:"
                    + locationLine);
        }


        double lat       = GeoUtils.decodeLatLon(latString);
        double lon       = GeoUtils.decodeLatLon(lonString);
        double elevation = Double.parseDouble(elevationString);
        setLocation(lat, lon, elevation);

        //        Principal investigator: Marc Fischer and Margaret Torn
        String contact = StringUtil.splitUpTo(header.get(2), ":", 2).get(1);
        String ecosystemType = StringUtil.splitUpTo(header.get(3), ":",
                                   2).get(1);
        //LOOK: this needs to be in the same order as the aontypes.xml defines in the point plugin
        setFileMetadata(new Object[] { siteId, contact, ecosystemType });

        StringBuffer fields = new StringBuffer(makeFields(new String[] {
                                  makeField(FIELD_SITE_ID,
                                            attrType(RecordField.TYPE_STRING),
                                            attrValue(siteId.trim())),
                                  makeField("Ecosystem_Type",
                                            attrType(RecordField.TYPE_STRING),
                                            attrValue(ecosystemType)),
                                  makeField(FIELD_LATITUDE, attrValue(lat)),
                                  makeField(FIELD_LONGITUDE, attrValue(lon)),
                                  makeField(FIELD_ELEVATION,
                                            attrValue(elevation)), }));

        List<String> fieldsFromFile = StringUtil.split(header.get(17), ",");
        List<String> unitsFromFile = StringUtil.split(header.get(18), ",");


        Hashtable<String, String> props = getClassProperties();

        for (int fieldIdx = 0; fieldIdx < fieldsFromFile.size(); fieldIdx++) {
            String field = fieldsFromFile.get(fieldIdx);
            String id    = cleanFieldName(field);
            id = Misc.getProperty(props, id + ".id", id);

            StringBuffer attrs = new StringBuffer();
            String       unit  = unitsFromFile.get(fieldIdx).trim();
            if ( !Utils.stringDefined(unit)) {
                unit = Misc.getProperty(props, id + ".unit", unit);
            }
            String label = Misc.getProperty(props, id + ".label",
                                            StringUtil.camelCase(field));

            if (Misc.getProperty(props, id + ".chartable", true)) {
                attrs.append(attrChartable());
            }
            if (Misc.getProperty(props, id + ".searchable", false)) {
                attrs.append(attrSearchable());
            }

            attrs.append(attrLabel(label));
            if (Utils.stringDefined(unit)) {
                attrs.append(attrUnit(unit));
            }
            fields.append(",");
            fields.append(makeField(id, attrs.toString()));
        }

        putProperty(PROP_FIELDS, fields.toString());

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
     * @throws Exception _more_
     */
    @Override
    public boolean processAfterReading(VisitInfo visitInfo, BaseRecord record)
            throws Exception {
        int        offset     = 5;
        TextRecord textRecord = (TextRecord) record;
        int        year       = (int) textRecord.getValue(offset + 1);
        int        doy        = (int) textRecord.getValue(offset + 4);
        String     hhmm       = "" + (int) textRecord.getValue(offset + 5);
        hhmm = StringUtil.padLeft(hhmm, 4, "0");
        String dttm = year + "-" + doy + " " + hhmm;
        Date   date = sdf.parse(dttm);
        record.setRecordTime(date.getTime());

        return super.processAfterReading(visitInfo, record);
    }

    /**
     * _more_
     *
     * @param record _more_
     * @param field _more_
     * @param v _more_
     *
     * @return _more_
     */
    public boolean isMissingValue(BaseRecord record, RecordField field,
                                  double v) {
        if ((v == -9999) || (v == -6999)) {
            return true;
        }

        return false;
    }


    /**
     * _more_
     *
     * @param args _more_
     */
    public static void main(String[] args) {
        PointFile.test(args, AmerifluxLevel2PointFile.class);
    }

}
