/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;


import ucar.unidata.xml.XmlUtil;


import java.text.DateFormat;

import java.text.SimpleDateFormat;

import java.util.Date;


/**
 * A collection of utilities for wadl  xml.
 *
 * @author Jeff McWhirter
 */

public class WadlUtil {

    /** _more_ */
    public static final String XMLNS = "http://wadl.dev.java.net/2009/02";

    /** _more_ */
    public static final String XMLNS_TNS = "urn:yahoo:yn";

    /** _more_ */
    public static final String XMLNS_XSD = "http://www.w3.org/2001/XMLSchema";

    /** _more_ */
    public static final String XMLNS_XSI =
        "http://www.w3.org/2001/XMLSchema-instance";

    /** _more_ */
    public static final String XMLNS_XMLNS_YA = "urn:yahoo:api";

    /** _more_ */
    public static final String XMLNS_XMLNS_YN = "urn:yahoo:yn";

    /** _more_ */
    public static final String TAG_APPLICATION = "application";

    /** _more_ */
    public static final String TAG_GRAMMARS = "grammars";

    /** _more_ */
    public static final String TAG_INCLUDE = "include";

    /** _more_ */
    public static final String TAG_RESOURCES = "resources";

    /** _more_ */
    public static final String TAG_RESOURCE = "resource";

    /** _more_ */
    public static final String TAG_METHOD = "method";

    /** _more_ */
    public static final String TAG_REQUEST = "request";

    /** _more_ */
    public static final String TAG_PARAM = "param";

    /** _more_ */
    public static final String TAG_OPTION = "option";

    /** _more_ */
    public static final String TAG_RESPONSE = "response";

    /** _more_ */
    public static final String TAG_REPRESENTATION = "representation";

    /** _more_ */
    public static final String ATTR_HREF = "href";

    /** _more_ */
    public static final String ATTR_BASE = "base";

    /** _more_ */
    public static final String ATTR_PATH = "path";

    /** _more_ */
    public static final String ATTR_ID = "id";

    /** _more_ */
    public static final String ATTR_NAME = "name";

    /** _more_ */
    public static final String ATTR_REQUIRED = "required";

    /** _more_ */
    public static final String ATTR_STYLE = "style";

    /** _more_ */
    public static final String ATTR_TYPE = "type";

    /** _more_ */
    public static final String ATTR_DEFAULT = "default";

    /** _more_ */
    public static final String ATTR_VALUE = "value";

    /** _more_ */
    public static final String ATTR_STATUS = "status";

    /** _more_ */
    public static final String ATTR_ELEMENT = "element";

    /** _more_ */
    public static final String ATTR_MEDIATYPE = "mediaType";


    /**
     * _more_
     *
     * @param sb _more_
     */
    public static void openTag(StringBuffer sb) {
        sb.append(XmlUtil.openTag(TAG_APPLICATION,
                                  XmlUtil.attrs("xmlns", XMLNS, "xmlns:xsd",
                                      XMLNS_XSD, "xmlns:xsi", XMLNS_XSI)));
        sb.append(
            XmlUtil.openTag(
                TAG_RESOURCES,
                XmlUtil.attrs(
                    ATTR_BASE,
                    "http://api.search.yahoo.com/NewsSearchService/V1/")));
    }

    /**
     * _more_
     *
     * @param sb _more_
     */
    public static void closeTag(StringBuffer sb) {
        sb.append(XmlUtil.closeTag(TAG_RESOURCES));
        sb.append(XmlUtil.closeTag(TAG_APPLICATION));
    }


    /**
     * _more_
     *
     * @param sb _more_
     * @param name _more_
     */
    public static void paramString(StringBuffer sb, String name) {
        paramString(sb, name, "xsd:string", "query", false);
    }

    /**
     * _more_
     *
     * @param sb _more_
     * @param name _more_
     * @param type _more_
     * @param style _more_
     * @param required _more_
     */
    public static void paramString(StringBuffer sb, String name, String type,
                                   String style, boolean required) {
        sb.append(XmlUtil.tag(TAG_APPLICATION,
                              XmlUtil.attrs(ATTR_NAME, name, ATTR_TYPE, type,
                                            ATTR_STYLE, style, ATTR_REQUIRED,
                                            "" + required)));
    }
    //  <param name="appid" type="xsd:string"   style="query" required="true"/> 

}
