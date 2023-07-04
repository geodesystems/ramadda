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
import org.ramadda.data.point.*;
import org.ramadda.data.point.text.*;


import org.ramadda.data.record.*;
import org.ramadda.util.Station;

import ucar.unidata.util.StringUtil;

import java.io.*;

import java.util.List;


/**
 */
public class FreewavePointFile extends CsvFile {

    /**
     * ctor
     *
     *
     *
     * @throws IOException _more_
     */
    public FreewavePointFile(IO.Path path) throws IOException {
        super(path);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getStationsPath() {
        return "/org/ramadda/geodata/point/amrc/freewavestations.txt";
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
        putProperty(PROP_DELIMITER, ",");
        putProperty(PROP_SKIPLINES, "4");

        super.prepareToVisit(visitInfo);
        List<String> headerLines = getHeaderLines();
        if (headerLines.size() != getSkipLines(visitInfo)) {
            throw new IllegalArgumentException("Bad number of header lines:"
                    + headerLines.size());
        }
        /*
        TOA5 = Campbell Scientific data format, this is the ascii text format
CapeBird = station name as defined in the data logger support software
CR1000 = datalogger model
38966 = datalogger serial number
CR1000.Std.22 = this is the current firmware installed on the datalogger
            CPU:newaws . . . = the program that is running on the data logger, CPU indicates the program is stored on the data logger (it could be stored on a compact flash card)
42717 = datalogger program signature
OutCard = tablename in the program on the data logger where the data was collected
        */
        //"TOA5","CapeBird","CR1000","38966","CR1000.Std.22","CPU:newawsFWv31_CB.CR1","42171","OutCard"
        String line1 = headerLines.get(0);
        line1 = line1.replaceAll("\"", "");

        List<String> toks = StringUtil.split(line1, ",", true, true);
        String       format           = toks.get(0);
        String       siteId           = toks.get(1);
        String       dataLoggerModel  = toks.get(2);
        String       dataLoggerSerial = toks.get(3);
        String       firmware         = toks.get(4);

        Station      station          = getStation(siteId);
        if (station == null) {
            throw new IllegalArgumentException(
                "Unable to find location for site:" + siteId);
        }

        setLocation(station.getLatitude(), station.getLongitude(),
                    station.getElevation());

        //LOOK: this needs to be in the same order as the amrctypes.xml defines in the point plugin
        setFileMetadata(new Object[] { siteId, format, dataLoggerModel,
                                       dataLoggerSerial, });


        String fields = makeFields(new String[] {
                            makeField(FIELD_SITE_ID, attrType(RecordField.TYPE_STRING),
                                      attrValue(siteId.trim())),
                            makeField(FIELD_LATITUDE,
                                      attrValue(station.getLatitude())),
                            makeField(FIELD_LONGITUDE,
                                      attrValue(station.getLongitude())),
                            makeField(FIELD_ELEVATION,
                                      attrValue(station.getElevation())) });

        fields += getFieldsFileContents();
        putProperty(PROP_FIELDS, fields);

        return visitInfo;
    }


    /**
     * _more_
     *
     * @param args _more_
     */
    public static void main(String[] args) {
        PointFile.test(args, FreewavePointFile.class);
    }

}
