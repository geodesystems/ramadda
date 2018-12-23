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

package org.ramadda.plugins.ontology;


import ucar.unidata.util.IOUtil;

import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;

import java.util.List;


/**
 * Class description
 *
 *
 * @version        $version$, Thu, Nov 25, '10
 * @author         Enter your name here...
 */
public class OboUtil {

    /** _more_ */
    public static final String TAG_FORMAT_VERSION = "format-version";

    /** _more_ */
    public static final String TAG_DATA_VERSION = "data-version";

    /** _more_ */
    public static final String TAG_DATE = "date";

    /** _more_ */
    public static final String TAG_SUBSETDEF = "subsetdef";

    /** _more_ */
    public static final String TAG_SYNONYMTYPEDEF = "synonymtypedef";

    /** _more_ */
    public static final String TAG_DEFAULT_NAMESPACE = "default-namespace";

    /** _more_ */
    public static final String TAG_REMARK = "remark";

    /** _more_ */
    public static final String TAG_ID = "id";

    /** _more_ */
    public static final String TAG_NAME = "name";

    /** _more_ */
    public static final String TAG_NAMESPACE = "namespace";

    /** _more_ */
    public static final String TAG_DEF = "def";

    /** _more_ */
    public static final String TAG_SYNONYM = "synonym";

    /** _more_ */
    public static final String TAG_IS_A = "is_a";

    /** _more_ */
    public static final String TAG_ALT_ID = "alt_id";

    /** _more_ */
    public static final String TAG_SUBSET = "subset";

    /** _more_ */
    public static final String TAG_XREF = "xref";

    /** _more_ */
    public static final String TAG_COMMENT = "comment";

    /** _more_ */
    public static final String TAG_IS_OBSOLETE = "is_obsolete";

    /** _more_ */
    public static final String TAG_CONSIDER = "consider";

    /** _more_ */
    public static final String TAG_RELATIONSHIP = "relationship";

    /** _more_ */
    public static final String TAG_PROPERTY_VALUE = "property_value";

    /** _more_ */
    public static final String TAG_REPLACED_BY = "replaced_by";

    /** _more_ */
    public static final String TAG_DISJOINT_FROM = "disjoint_from";

    /** _more_ */
    public static final String TAG_IS_TRANSITIVE = "is_transitive";

    /** _more_ */
    public static final String TAG_TRANSITIVE_OVER = "transitive_over";


}
