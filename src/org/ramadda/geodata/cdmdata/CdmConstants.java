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

package org.ramadda.geodata.cdmdata;


import org.ramadda.repository.Constants;


/**
 */
public interface CdmConstants {

    /** _more_ */
    public static final String ICON_OPENDAP = "/cdmdata/opendap.gif";

    /** CSV format */
    public static final String FORMAT_CSV = "csv";

    /** KML format */
    public static final String FORMAT_KML = "kml";

    /** NCML format */
    public static final String FORMAT_NCML = "ncml";

    /** _more_ */
    public static final String FORMAT_JSON = "json";

    /** NCML suffix */
    public static final String SUFFIX_NCML = ".ncml";

    /** netcdf suffix */
    public static final String SUFFIX_NC = ".nc";

    /** netcdf4 suffix */
    public static final String SUFFIX_NC4 = ".nc4";

    /** GrADS CTL suffix */
    public static final String SUFFIX_CTL = ".ctl";

    /** CSV suffix */
    public static final String SUFFIX_CSV = ".csv";

    /** _more_ */
    public static final String SUFFIX_JSON = ".json";

    /** CSV suffix */
    public static final String SUFFIX_XML = ".xml";

    /** bounding box argument */
    public static final String ARG_POINT_BBOX = "bbox";

    /** Variable prefix */
    public static final String VAR_PREFIX = Constants.ARG_VARIABLE + ".";

    /** add lat lon argument */
    public static final String ARG_ADDLATLON = "addlatlon";


    /** horizontal stride */
    public static final String ARG_HSTRIDE = "hstride";

    /** level */
    public static final String ARG_LEVEL = "level";

    /** format */
    public static final String ARG_FORMAT = "format";

    /** format */
    public static final String ARG_IMAGE_WIDTH = "image_width";

    /** format */
    public static final String ARG_IMAGE_HEIGHT = "image_height";

    /** calendar */
    public static final String ARG_CALENDAR = "calendar";

    /** spatial arguments */
    public static final String[] SPATIALARGS = new String[] {
                                                   Constants.ARG_AREA_NORTH,
            Constants.ARG_AREA_WEST, Constants.ARG_AREA_SOUTH,
            Constants.ARG_AREA_EAST, };

    /** chart format */
    public static final String FORMAT_TIMESERIES = "timeseries";

    /** chart format */
    public static final String FORMAT_TIMESERIES_CHART = "timeserieschart";


    /** chart format */
    public static final String FORMAT_TIMESERIES_CHART_DATA =
        "timeserieschartdata";

    /** chart image format */
    public static final String FORMAT_TIMESERIES_IMAGE = "timeseriesimage";

    /** Data group */
    public static final String GROUP_DATA = "Data";

    /** CDM Type */
    public static final String TYPE_CDM = "cdm";

    /** GRID type */
    public static final String TYPE_GRID = "grid";

    /** TRAJECTORY type */
    public static final String TYPE_TRAJECTORY = "trajectory";

    /** POINT_TYPE */
    public static final String TYPE_POINT = "point";

    /** GrADS type */
    public static final String TYPE_GRADS = "gradsbinary";


}
