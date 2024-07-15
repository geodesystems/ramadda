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
import org.ramadda.util.MyDateFormat;

import org.ramadda.data.point.*;
import org.ramadda.data.point.text.*;


import org.ramadda.data.record.*;
import org.ramadda.util.Utils;
import org.ramadda.util.geo.GeoUtils;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.io.*;

import java.util.Date;
import java.util.List;


/**
 */
public class SwitchyardPointFile extends SingleSiteTextFile {

    /**
     * ctor
     *
     * @throws IOException
     */
    public SwitchyardPointFile(IO.Path path) throws IOException {
        super(path);
    }


    /**
     * @param visitInfo holds visit info
     * @return visit info
     * @throws Exception _more_
     */
    @Override
    public VisitInfo prepareToVisit(VisitInfo visitInfo) throws Exception {

        try {
            putProperty(PROP_DELIMITER, " ");
            putProperty(PROP_HEADER_DELIMITER, "#");
            visitInfo = super.prepareToVisit(visitInfo);

            String   hdr          = StringUtil.join("\n", getHeaderLines());

            String   latString    = null;
            String   lonString    = null;

            String[] datePatterns = {
                ".*(\\d?\\d/\\d\\d/\\d\\d\\d\\d\\s*(_|/\\s)\\s*\\d\\d\\d\\d)\\s*UTC.*",
                ".*(\\d?\\d/\\d?\\d/\\d\\d\\d\\d\\s+\\d\\d\\d\\d)\\s*UTC.*",
                ".*\\s+(\\d?\\d\\s+[^\\s]+\\s+\\d\\d\\d\\d\\s+\\d\\d\\d\\d\\s+UTC).*",
                //5/3/2009 _ 1542 UTC
                ".*\\s+(\\d+/\\d+/\\d+\\s*_\\s*\\d\\d\\d\\d\\s+UTC)",
                //2012-5-21/1801 UTC
                ".*(\\d\\d\\d\\d-\\d?\\d-\\d?\\d/\\d\\d\\d\\d\\s+UTC).*",
                //2011-5-4/1439
                ".*(\\d\\d\\d\\d-\\d?\\d-\\d?\\d/\\d\\d\\d\\d).*",
                //2010-5-5 / 1706 UTC
                ".*(\\d\\d\\d\\d-\\d?\\d-\\d?\\d\\s*/\\s*\\d\\d\\d\\d\\s*UTC).*"
            };


            String[] dateFormats = {
                "MM/dd/yyyyHHmm", "MM/dd/yyyyHHmm", "dd MMMM yyyy HHmm Z",
                "MM/dd/yyyy HHmm Z", "yyyy-MM-dd/HHmm", "yyyy-MM-dd/HHmm",
                "yyyy-MM-dd/HHmm Z"
            };

            Date date = Utils.findDate(hdr, datePatterns, dateFormats);
            if (date == null) {
                throw new IllegalArgumentException("no date  in header");
            }

            hdr = hdr.replaceAll(" deg ", " degrees ");
            String[] lats =
                Utils.findPatterns(
                    hdr,
                    ".*\\s+([\\d\\.]+)\\s*degrees\\s*([\\d\\.]+)\\s*min\\s*North.*");
            if (lats != null) {
                latString = lats[0] + ":" + lats[1];
            }
            String[] lons =
                Utils.findPatterns(
                    hdr,
                    ".*\\s+([\\d\\.]+)\\s*degrees\\s*([\\d\\.]+)\\s*min\\s*West.*");
            if (lons != null) {
                lonString = lons[0] + ":" + lons[1];
            }


            if (latString == null) {
                String[] pts =
                    Utils.findPatterns(
                        hdr,
                        ".*\\s+([\\d\\.]+)\\s*deg\\s*([\\d\\.]+)\\s*min\\s*N .*([\\d\\.]+)\\s*deg\\s*([\\d\\.]+)\\s*min");
                if (pts != null) {
                    latString = pts[0] + ":" + pts[1];
                    lonString = pts[2] + ":" + pts[3];
                }
            }


            if (latString == null) {
                //  84.122 degrees North  _  058.008 degrees West      
                latString = StringUtil.findPattern(hdr,
                        ".*\\s+([0-9\\.]+)\\s+degrees North.*");
                lonString = StringUtil.findPattern(hdr,
                        ".*\\s+([0-9\\.]+)\\s+degrees West.*");
            }

            if ((latString == null) || (lonString == null)) {
                throw new IllegalArgumentException(
                    "Could not read location from:" + hdr);
            }

            double lat = GeoUtils.decodeLatLon(latString);
            double lon = GeoUtils.decodeLatLon(lonString);
            setLocation(lat, lon, 0);

            String cast = StringUtil.findPattern(hdr, "\\s+Cast\\s+(\\d+).*");
            String siteId = StringUtil.findPattern(hdr,
                                "\\s+Station\\s+([^\\s]+)\\s+.*");

            if (siteId == null) {
                siteId = "";
            }

            if (cast == null) {
                cast = "";
            }

            //LOOK: this needs to be in the same order as the aontypes.xml defines in the point plugin
            setFileMetadata(new Object[] { siteId, cast, });
            String  dttm = makeDateFormat("yyyy-MM-dd HH:mm").format(date);
            int     year             = Utils.getYear(date);
            boolean addPotentialTemp = year == 2005;
            String  attrs            = attrChartable() + attrSearchable();
            putFields(new String[] {
                makeField(FIELD_SITE_ID, attrType(RecordField.TYPE_STRING),
                          attrValue(siteId.trim())),
                makeField("Cast", attrValue(cast), attrType(RecordField.TYPE_STRING)),
                makeField(FIELD_LATITUDE, attrValue(lat)),
                makeField(FIELD_LONGITUDE, attrValue(lon)),
                makeField(FIELD_DATE, attrType(RecordField.TYPE_DATE), attrValue(dttm),
                          attrFormat("yyyy-MM-dd HH:mm")),
                makeField(FIELD_DEPTH, attrs + attrUnit(UNIT_METERS)),
                makeField(FIELD_PRESSURE, attrs + attrUnit("dbar")),
                makeField("In_Situ_Temperature",
                          attrSortOrder(10) + attrs + attrUnit(UNIT_CELSIUS)),
                ( !addPotentialTemp
                  ? null
                  : makeField(FIELD_POTENTIAL_TEMPERATURE,
                              attrs + attrUnit(UNIT_CELSIUS))),
                makeField(FIELD_CONDUCTIVITY,
                          attrSortOrder(10) + attrs + attrUnit("S/m")),
                makeField(FIELD_SALINITY,
                          attrSortOrder(10) + attrs + attrUnit("psu")),
                makeField(FIELD_SIGMA,
                          attrSortOrder(10) + attrs + attrUnit("-theta")),
                /*
                makeField("Dissolved_Oxygen_ML_L",  attrs +  attrUnit("ml/l")),
                makeField("Dissolved_Oxygen_MG_L",  attrs +  attrUnit("mg/l")),
                makeField("Dissolved_Oxygen_Sat",  attrs +  attrUnit("%sat")),
                makeField("Dissolved_Oxygen_MMOL_KG",  attrs +  attrUnit("Mmol/kg")),*/
            });

            return visitInfo;
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }


    }


    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        String s =
            "Station 1 (Cast 2)              84deg 01.763min N   65deg 09.247min W              2003-5-6/1730 GMT";
        PointFile.test(args, SwitchyardPointFile.class);
    }

}
