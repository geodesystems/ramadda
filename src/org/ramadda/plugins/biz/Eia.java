/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.biz;


import org.json.*;


import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.TTLCache;
import org.ramadda.util.Utils;

import org.w3c.dom.*;
import org.w3c.dom.*;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.net.URL;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;


/**
 */
public class Eia {

    /** _more_ */
    public static final String TYPE_CATEGORY = "type_eia_category";


    /** _more_ */
    public static final String TYPE_SERIES = "type_eia_series";

    /** _more_ */
    public static final String PROP_API_KEY = "eia.api.key";

    /** _more_ */
    public static final String PREFIX_CATEGORY = "category";

    /** _more_ */
    public static final String PREFIX_SERIES = "series";


    /** _more_ */
    public static final String URL_BASE = "https://api.eia.gov/v2";


    /** _more_ */
    public static final String URL_CATEGORY = URL_BASE + "/category/";

    /** _more_ */
    public static final String URL_SERIES = URL_BASE + "/seriesid";


    /** _more_ */
    public static final String ARG_API_KEY = "api_key";

    /** _more_ */
    public static final String ARG_CATEGORY_ID = "category_id";

    /** _more_ */
    public static final String ARG_OUT = "out";

    /** _more_ */
    public static final String ARG_NUM = "num";

    /** _more_ */
    public static final String ARG_SERIES_ID = "series_id";




    /** _more_ */
    public static final String ATTR_NAME = "name";

    /** _more_ */
    public static final String ATTR_DESCRIPTION = "description";


    /** _more_ */
    public static final String TAG_CATEGORY = "category";

    /** _more_ */
    public static final String TAG_CATEGORY_ID = "category_id";

    /** _more_ */
    public static final String TAG_CHILDCATEGORIES = "childcategories";

    /** _more_ */
    public static final String TAG_CHILDSERIES = "childseries";

    /** _more_ */
    public static final String TAG_COMMAND = "command";

    /** _more_ */
    public static final String TAG_COPYRIGHT = "copyright";

    /** _more_ */
    public static final String TAG_DATA = "data";

    /** _more_ */
    public static final String TAG_DATE = "date";

    /** _more_ */
    public static final String TAG_DESCRIPTION = "description";

    /** _more_ */
    public static final String TAG_EIA_API = "eia_api";

    /** _more_ */
    public static final String TAG_END = "end";

    /** _more_ */
    public static final String TAG_F = "f";

    /** _more_ */
    public static final String TAG_GEOGRAPHY = "geography";

    /** _more_ */
    public static final String TAG_ISO3166 = "iso3166";

    /** _more_ */
    public static final String TAG_NAME = "name";

    /** _more_ */
    public static final String TAG_NOTES = "notes";

    /** _more_ */
    public static final String TAG_PARENT_CATEGORY_ID = "parent_category_id";

    /** _more_ */
    public static final String TAG_REQUEST = "request";

    /** _more_ */
    public static final String TAG_ROW = "row";

    /** _more_ */
    public static final String TAG_SERIES = "series";

    /** _more_ */
    public static final String TAG_SERIES_ID = "series_id";

    /** _more_ */
    public static final String TAG_SOURCE = "source";

    /** _more_ */
    public static final String TAG_START = "start";

    /** _more_ */
    public static final String TAG_UNITS = "units";

    /** _more_ */
    public static final String TAG_UPDATED = "updated";

    /** _more_ */
    public static final String TAG_VALUE = "value";



}
