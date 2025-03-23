/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;


import java.text.DateFormat;

import java.text.SimpleDateFormat;

import java.util.Date;


/**
 * A collection of utilities for rss feeds xml.
 *
 * @author Jeff McWhirter
 */

public class WaterMLUtil {

    /** _more_ */
    public static final String XMLNS_XMLNS =
        "http://www.cuahsi.org/waterML/1.1/";

    /** _more_ */
    public static final String XMLNS_XMLNS_XSD =
        "http://www.w3.org/2001/XMLSchema";

    /** _more_ */
    public static final String XMLNS_XMLNS_XSI =
        "http://www.w3.org/2001/XMLSchema-instance";

    /** _more_ */
    public static final String TAG_TIMESERIESRESPONSE = "timeSeriesResponse";

    /** _more_ */
    public static final String TAG_QUERYINFO = "queryInfo";

    /** _more_ */
    public static final String TAG_CREATIONTIME = "creationTime";

    /** _more_ */
    public static final String TAG_CRITERIA = "criteria";

    /** _more_ */
    public static final String TAG_PARAMETER = "parameter";

    /** _more_ */
    public static final String TAG_TIMESERIES = "timeSeries";

    /** _more_ */
    public static final String TAG_SOURCEINFO = "sourceInfo";

    /** _more_ */
    public static final String TAG_SITENAME = "siteName";

    /** _more_ */
    public static final String TAG_SITECODE = "siteCode";

    /** _more_ */
    public static final String TAG_GEOLOCATION = "geoLocation";

    /** _more_ */
    public static final String TAG_GEOGLOCATION = "geogLocation";

    /** _more_ */
    public static final String TAG_LATITUDE = "latitude";

    /** _more_ */
    public static final String TAG_LONGITUDE = "longitude";

    /** _more_ */
    public static final String TAG_LOCALSITEXY = "localSiteXY";

    /** _more_ */
    public static final String TAG_X = "X";

    /** _more_ */
    public static final String TAG_Y = "Y";

    /** _more_ */
    public static final String TAG_ELEVATION_M = "elevation_m";

    /** _more_ */
    public static final String TAG_VERTICALDATUM = "verticalDatum";

    /** _more_ */
    public static final String TAG_SITEPROPERTY = "siteProperty";

    /** _more_ */
    public static final String TAG_VARIABLE = "variable";

    /** _more_ */
    public static final String TAG_VARIABLECODE = "variableCode";

    /** _more_ */
    public static final String TAG_VARIABLENAME = "variableName";

    /** _more_ */
    public static final String TAG_VALUETYPE = "valueType";

    /** _more_ */
    public static final String TAG_DATATYPE = "dataType";

    /** _more_ */
    public static final String TAG_GENERALCATEGORY = "generalCategory";

    /** _more_ */
    public static final String TAG_SAMPLEMEDIUM = "sampleMedium";

    /** _more_ */
    public static final String TAG_UNIT = "unit";

    /** _more_ */
    public static final String TAG_UNITNAME = "unitName";

    /** _more_ */
    public static final String TAG_UNITTYPE = "unitType";

    /** _more_ */
    public static final String TAG_UNITABBREVIATION = "unitAbbreviation";

    /** _more_ */
    public static final String TAG_UNITCODE = "unitCode";

    /** _more_ */
    public static final String TAG_NODATAVALUE = "noDataValue";

    /** _more_ */
    public static final String TAG_TIMESCALE = "timeScale";

    /** _more_ */
    public static final String TAG_TIMESUPPORT = "timeSupport";

    /** _more_ */
    public static final String TAG_SPECIATION = "speciation";

    /** _more_ */
    public static final String TAG_VALUES = "values";

    /** _more_ */
    public static final String TAG_VALUE = "value";

    /** _more_ */
    public static final String TAG_QUALITYCONTROLLEVEL =
        "qualityControlLevel";

    /** _more_ */
    public static final String TAG_QUALITYCONTROLLEVELCODE =
        "qualityControlLevelCode";

    /** _more_ */
    public static final String TAG_DEFINITION = "definition";

    /** _more_ */
    public static final String TAG_EXPLANATION = "explanation";

    /** _more_ */
    public static final String TAG_METHOD = "method";

    /** _more_ */
    public static final String TAG_METHODCODE = "methodCode";

    /** _more_ */
    public static final String TAG_METHODDESCRIPTION = "methodDescription";

    /** _more_ */
    public static final String TAG_METHODLINK = "methodLink";

    /** _more_ */
    public static final String TAG_SOURCE = "source";

    /** _more_ */
    public static final String TAG_SOURCECODE = "sourceCode";

    /** _more_ */
    public static final String TAG_ORGANIZATION = "organization";

    /** _more_ */
    public static final String TAG_SOURCEDESCRIPTION = "sourceDescription";

    /** _more_ */
    public static final String TAG_CONTACTINFORMATION = "contactInformation";

    /** _more_ */
    public static final String TAG_CONTACTNAME = "contactName";

    /** _more_ */
    public static final String TAG_TYPEOFCONTACT = "typeOfContact";

    /** _more_ */
    public static final String TAG_EMAIL = "email";

    /** _more_ */
    public static final String TAG_PHONE = "phone";

    /** _more_ */
    public static final String TAG_ADDRESS = "address";

    /** _more_ */
    public static final String TAG_SOURCELINK = "sourceLink";

    /** _more_ */
    public static final String TAG_CITATION = "citation";

    /** _more_ */
    public static final String TAG_CENSORCODE = "censorCode";

    /** _more_ */
    public static final String TAG_CENSORCODEDESCRIPTION =
        "censorCodeDescription";

    /** _more_ */
    public static final String ATTR_METHODCALLED = "MethodCalled";

    /** _more_ */
    public static final String ATTR_NAME = "name";

    /** _more_ */
    public static final String ATTR_VALUE = "value";

    /** _more_ */
    public static final String ATTR_NETWORK = "network";

    /** _more_ */
    public static final String ATTR_SITEID = "siteID";

    /** _more_ */
    public static final String ATTR_SRS = "srs";

    /** _more_ */
    public static final String ATTR_PROJECTIONINFORMATION =
        "projectionInformation";

    /** _more_ */
    public static final String ATTR_DEFAULT = "default";

    /** _more_ */
    public static final String ATTR_VARIABLEID = "variableID";

    /** _more_ */
    public static final String ATTR_VOCABULARY = "vocabulary";

    /** _more_ */
    public static final String ATTR_ISREGULAR = "isRegular";

    /** _more_ */
    public static final String ATTR_CENSORCODE = "censorCode";

    /** _more_ */
    public static final String ATTR_DATETIME = "dateTime";

    /** _more_ */
    public static final String ATTR_DATETIMEUTC = "dateTimeUTC";

    /** _more_ */
    public static final String ATTR_METHODCODE = "methodCode";

    /** _more_ */
    public static final String ATTR_QUALITYCONTROLLEVELCODE =
        "qualityControlLevelCode";

    /** _more_ */
    public static final String ATTR_SOURCECODE = "sourceCode";

    /** _more_ */
    public static final String ATTR_TIMEOFFSET = "timeOffset";

    /** _more_ */
    public static final String ATTR_QUALITYCONTROLLEVELID =
        "qualityControlLevelID";

    /** _more_ */
    public static final String ATTR_METHODID = "methodID";

    /** _more_ */
    public static final String ATTR_SOURCEID = "sourceID";


}
