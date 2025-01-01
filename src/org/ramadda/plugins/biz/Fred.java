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
public class Fred {

    /** _more_ */
    public static final String TAG_CATEGORIES = "categories";

    /** _more_ */
    public static final String TAG_CATEGORY = "category";

    /** _more_ */
    public static final String TAG_SERIES = "series";

    /** _more_ */
    public static final String TAG_OBSERVATION = "observation";

    /** _more_ */
    public static final String ATTR_ID = "id";

    /** _more_ */
    public static final String ATTR_VALUE = "value";

    /** _more_ */
    public static final String ATTR_DATE = "date";

    /** _more_ */
    public static final String ATTR_NAME = "name";

    /** _more_ */
    public static final String ATTR_NOTES = "notes";

    /** _more_ */
    public static final String ATTR_UNITS = "units";

    /** _more_ */
    public static final String ATTR_FREQUENCY_SHORT = "frequency_short";

    /** _more_ */
    public static final String ATTR_OBSERVATION_START = "observation_start";

    /** _more_ */
    public static final String ATTR_OBSERVATION_END = "observation_end";

    /** _more_ */
    public static final String ATTR_SEASONAL_ADJUSTMENT =
        "seasonal_adjustment";


    /** _more_ */
    public static final String ATTR_TITLE = "title";

    /** _more_ */
    public static final String ATTR_PARENT_ID = "parent_id";


    /** _more_ */
    public static final String URL_BASE = "https://api.stlouisfed.org/fred";

    /** _more_ */
    public static final String URL_CATEGORY = URL_BASE + "/category";

    /** _more_ */
    public static final String URL_CATEGORY_CHILDREN = URL_BASE
                                                       + "/category/children";

    /** _more_ */
    public static final String URL_CATEGORY_SERIES = URL_BASE
                                                     + "/category/series";

    /** _more_ */
    public static final String URL_CATEGORY_RELATED = URL_BASE
                                                      + "/category/related";

    /** _more_ */
    public static final String URL_SERIES = URL_BASE + "/series";

    /** _more_ */
    public static final String URL_SERIES_CATEGORIES = URL_BASE
                                                       + "/series/categories";

    /** _more_ */
    public static final String URL_SERIES_OBSERVATIONS =
        URL_BASE + "/series/observations";

    /** _more_ */
    public static final String URL_SERIES_RELEASE = URL_BASE
                                                    + "/series/release";

    /** _more_ */
    public static final String URL_SERIES_SEARCH = URL_BASE
                                                   + "/series/search";

    /** _more_ */
    public static final String ARG_API_KEY = "api_key";

    /** _more_ */
    public static final String ARG_CATEGORY_ID = "category_id";

    /** _more_ */
    public static final String ARG_SERIES_ID = "series_id";

    /** _more_ */
    public static final String ARG_FILE_TYPE = "file_type";


    /** _more_ */
    public static final String PROP_API_KEY = "fred.api.key";

    /** _more_ */
    public static final String PREFIX_CATEGORY = "category";

    /** _more_ */
    public static final String PREFIX_SERIES = "series";


    /** _more_ */
    public static final String TYPE_CATEGORY = "type_fred_category";


    /** _more_ */
    public static final String TYPE_SERIES = "type_fred_series";


}
