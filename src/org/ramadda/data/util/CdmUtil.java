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

package org.ramadda.data.util;


import ucar.nc2.time.CalendarDate;

import ucar.unidata.util.DateUtil;

import java.util.Date;


/**
 * Utility class for netCDF-Java Common Data Model (CDM) stuff
 */
public class CdmUtil {

    /** _more_ */
    public static final String ATTR_MINLAT = "geospatial_lat_min";

    /** _more_ */
    public static final String ATTR_MAXLAT = "geospatial_lat_max";

    /** _more_ */
    public static final String ATTR_MINLON = "geospatial_lon_min";

    /** _more_ */
    public static final String ATTR_MAXLON = "geospatial_lon_max";

    /** _more_ */
    public static final String ATTR_KEYWORDS = "keywords";

    /** _more_ */
    public static final String ATTR_TITLE = "title";

    /** _more_ */
    public static final String ATTR_DESCRIPTION = "description";

    /** _more_ */
    public static final String ATTR_ABSTRACT = "abstract";

    /** _more_ */
    public static final String ATTR_SUMMARY = "summary";

    //level3

    /** _more_ */
    public static final String ATTR_RADAR_STATIONID = "ProductStation";

    /** _more_ */
    public static final String ATTR_RADAR_STATIONNAME = "ProductStationName";

    /** _more_ */
    public static final String ATTR_RADAR_LATITUDE = "RadarLatitude";

    /** _more_ */
    public static final String ATTR_RADAR_LONGITUDE = "RadarLongitude";

    /** _more_ */
    public static final String ATTR_RADAR_ALTITUDE = "RadarAltitude";

    /** _more_ */
    public static final String ATTR_KEYWORDS_VOCABULARY =
        "keywords_vocabulary";

    /** _more_ */
    public static final String ATTR_TIME_START = "time_coverage_start";

    //level2

    /** _more_ */
    public static final String ATTR_STATIONID = "Station";

    /** _more_ */
    public static final String ATTR_STATIONNAME = "StationName";

    /** _more_ */
    public static final String ATTR_STATION_LATITUDE = "StationLatitude";

    /** _more_ */
    public static final String ATTR_STATION_LONGITUDE = "StationLongitude";

    /** _more_ */
    public static final String ATTR_STATION_ALTITUDE =
        "StationElevationInMeters";

    /**
     * Make a date from a calendar date.  This should probably be in a utility
     * class.
     *
     * @param cd  the CalendarDate
     * @return the corresponding date.
     */
    public static Date makeDate(CalendarDate cd) {
        Date d = null;
        try {
            d = DateUtil.parse(cd.toString());
        } catch (Exception e) {
            d = cd.toDate();  // not correct for non-standard calendars
        }

        return d;
    }

}
