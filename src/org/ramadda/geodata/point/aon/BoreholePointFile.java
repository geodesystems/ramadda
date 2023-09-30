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

package org.ramadda.geodata.point.aon;

import org.ramadda.util.IO;

import org.ramadda.data.point.*;
import org.ramadda.data.point.text.*;
import org.ramadda.data.record.*;

import org.ramadda.util.Utils;
import org.ramadda.util.geo.GeoUtils;



import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.io.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;


/**
 */
public class BoreholePointFile extends SingleSiteTextFile {

    /**
     * _more_
     */
    public BoreholePointFile() {}

    /**
     * ctor
     *
     *
     *
     * @throws IOException _more_
     */
    public BoreholePointFile(IO.Path path) throws IOException {
        super(path);
    }

    /**
     * _more_
     *
     * @param filename _more_
     * @param properties _more_
     *
     * @throws IOException _more_
     */
    public BoreholePointFile(IO.Path path, Hashtable properties)
            throws IOException {
        super(path, properties);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getDelimiter() {
        return ",";
    }

    /**
     *  Overwrite the record making method so we can check for empty lines
     *
     * @param visitInfo _more_
     *
     * @return _more_
     */
    @Override
    public BaseRecord doMakeRecord(VisitInfo visitInfo) {
        TextRecord record = new TextRecord(this, getFields()) {
            public boolean lineOk(String line) {
                if ( !super.lineOk(line)) {
                    return false;
                }
                if (line.startsWith(",")) {
                    return false;
                }

                return true;
            }

        };
        record.setDelimiter(getDelimiter());

        return record;
    }

    /**
     * _more_
     *
     * @param visitInfo _more_
     *
     * @return _more_
     */
    @Override
    public int getSkipLines(VisitInfo visitInfo) {
        return 14;
    }

    /*
http://www.aoncadis.org/dataset/network_of_permafrost_observatories_in_north_america_and_russia__deep_boreholes_.html
Soil temperature  at different depths,,,,NSF projects ARC-0520578_COMMA_ ARC-0632400 and ARC 0856864,,,
Ilu_COMMA_ Greenland,,,,PI/Data contact =Vladimir E. Romanovsky,,,
Location: N 69.2390  W 51.0623,,,,Professor,,,
Elevation (meters):,,,,Geophysical Institute UAF      tel.: (907)474-7459,,,
Slope: flat,,,,903 Koyukuk Drive              FAX : (907)474-7290,,,
Aspect: flat,,,,P.O.Box 757320,,,
,,,,Fairbanks_COMMA_ AK 99775-7320    e-mail: veromanovsky@alaska.edu,,,
,,,,Data provided byThomas Ingeman-Nielsen_COMMA_ Department of Civil Engineering Technical University of Denmark,,,
,,,,,,,
Data dates: 09/03/2007-07/04/2010,,,,,
WARNING: "999" fields mean not valid data;  "-999" fields mean data are absent; all temperatures in grad C,,,,,
,,,,,
,,Temperature,Temperature,Temperature,Temperature,Temperature,Temperature,Temperature,Temperature
YEAR,DATE,0.0,0.25,0.5,0.75,1.0,2.0,3.0,4.0
2007.0,03-Sep-2007,2.10,2.32,1.64,0.50,-0.39,-2.20,-3.04,-3.27
    */


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
        super.prepareToVisit(visitInfo);
        List<String> header = getHeaderLines();

        List<String> toks   = StringUtil.split(header.get(1), ",");
        String       siteId = toks.get(0);
        siteId = siteId.replaceAll("_COMMA_", " - ");
        //PI/Data contact =Vladimir E. Romanovsky
        String contact = toks.get(4);
        if (contact.indexOf("=") > 0) {
            contact = StringUtil.findPattern(contact, ".*=(.*)");
        }

        //Location: N 67.41069 E 63.39578,,,Professor,,,,,,,,,,,
        String locationString = StringUtil.findPattern(header.get(2),
                                    "Location:\\s(.*),");
        if (locationString == null) {
            throw new IllegalArgumentException(
                "Could not read location from:" + header.get(2));
        }

        toks = StringUtil.split(locationString, " ", true, true);
        if (toks.size() != 4) {
            throw new IllegalArgumentException(
                "Could not read location from:" + header.get(2));
        }
        double lat = GeoUtils.decodeLatLon(toks.get(1) + toks.get(0));
        double lon = GeoUtils.decodeLatLon(toks.get(3) + toks.get(2));
        setLocation(lat, lon, 0);

        //LOOK: this needs to be in the same order as the aontypes.xml defines in the point plugin
        setFileMetadata(new Object[] { siteId, contact });
        String tempAttrs = attrChartable() + attrSearchable()
                           + attrUnit("Celsius");
        String fields = makeFields(new String[] {
            makeField(FIELD_SITE_ID, attrType(RecordField.TYPE_STRING),
                      attrValue(siteId.trim())),
            makeField(FIELD_LATITUDE, attrValue(lat)),
            makeField(FIELD_LONGITUDE, attrValue(lon)),
            makeField(FIELD_YEAR, ""),
            makeField(FIELD_DATE, attrType(RecordField.TYPE_DATE),
                      attrFormat("dd-MMM-yyyy")),
            makeField("Temperature_0_Meter", tempAttrs),
            makeField("Temperature_025_Meter", tempAttrs),
            makeField("Temperature_05_Meter", tempAttrs),
            makeField("Temperature_075_Meter", tempAttrs),
            makeField("Temperature_1_Meter", tempAttrs),
            makeField("Temperature_2_Meter", tempAttrs),
            makeField("Temperature_3_Meter", tempAttrs),
            makeField("Temperature_4_Meter", tempAttrs),
        });
        putProperty(PROP_FIELDS, fields);

        return visitInfo;
    }


    /**
     * _more_
     *
     * @param args _more_
     */
    public static void main(String[] args) {
        PointFile.test(args, BoreholePointFile.class);
    }

}
