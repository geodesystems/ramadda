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

package org.ramadda.plugins.map;


import org.ramadda.repository.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;


import org.w3c.dom.*;


/**
 */
public class GpxUtil {

    /** _more_ */
    public static final String TAG_GPX = "gpx";

    /** _more_ */
    public static final String TAG_METADATA = "metadata";

    /** _more_ */
    public static final String TAG_LINK = "link";

    /** _more_ */
    public static final String TAG_TEXT = "text";

    /** _more_ */
    public static final String TAG_BOUNDS = "bounds";

    /** _more_ */
    public static final String TAG_EXTENSIONS = "extensions";

    /** _more_ */
    public static final String TAG_TIME = "time";

    /** _more_ */
    public static final String TAG_WPT = "wpt";

    /** _more_ */
    public static final String TAG_ELE = "ele";

    /** _more_ */
    public static final String TAG_NAME = "name";

    /** _more_ */
    public static final String TAG_CMT = "cmt";

    /** _more_ */
    public static final String TAG_DESC = "desc";

    /** _more_ */
    public static final String TAG_SYM = "sym";

    /** _more_ */
    public static final String TAG_LABEL = "label";

    /** _more_ */
    public static final String TAG_LABEL_TEXT = "label_text";

    /** _more_ */
    public static final String TAG_AUTHOR = "author";

    /** _more_ */
    public static final String TAG_EMAIL = "email";

    /** _more_ */
    public static final String TAG_URL = "url";

    /** _more_ */
    public static final String TAG_URLNAME = "urlname";

    /** _more_ */
    public static final String TAG_KEYWORDS = "keywords";


    /** _more_ */
    public static final String TAG_TYPE = "type";

    /** _more_ */
    public static final String TAG_RTE = "rte";

    /** _more_ */
    public static final String TAG_RTEPT = "rtept";

    /** _more_ */
    public static final String TAG_TRK = "trk";

    /** _more_ */
    public static final String TAG_NUMBER = "number";

    /** _more_ */
    public static final String TAG_TRKSEG = "trkseg";

    /** _more_ */
    public static final String TAG_TRKPT = "trkpt";



    /** _more_ */
    public static final String ATTR_CREATOR = "creator";

    /** _more_ */
    public static final String ATTR_VERSION = "version";

    /** _more_ */
    public static final String ATTR_HREF = "href";

    /** _more_ */
    public static final String ATTR_MAXLAT = "maxlat";

    /** _more_ */
    public static final String ATTR_MAXLON = "maxlon";

    /** _more_ */
    public static final String ATTR_MINLAT = "minlat";

    /** _more_ */
    public static final String ATTR_MINLON = "minlon";

    /** _more_ */
    public static final String ATTR_LAT = "lat";

    /** _more_ */
    public static final String ATTR_LON = "lon";


}
