/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;

import ucar.unidata.xml.XmlUtil;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


public class WadlUtil {
    public static final String XMLNS = "http://wadl.dev.java.net/2009/02";
    public static final String XMLNS_TNS = "urn:yahoo:yn";
    public static final String XMLNS_XSD = "http://www.w3.org/2001/XMLSchema";
    public static final String XMLNS_XSI =
        "http://www.w3.org/2001/XMLSchema-instance";

    public static final String XMLNS_XMLNS_YA = "urn:yahoo:api";
    public static final String XMLNS_XMLNS_YN = "urn:yahoo:yn";
    public static final String TAG_APPLICATION = "application";
    public static final String TAG_GRAMMARS = "grammars";
    public static final String TAG_INCLUDE = "include";
    public static final String TAG_RESOURCES = "resources";
    public static final String TAG_RESOURCE = "resource";
    public static final String TAG_METHOD = "method";
    public static final String TAG_REQUEST = "request";
    public static final String TAG_PARAM = "param";
    public static final String TAG_OPTION = "option";
    public static final String TAG_RESPONSE = "response";
    public static final String TAG_REPRESENTATION = "representation";
    public static final String ATTR_HREF = "href";
    public static final String ATTR_BASE = "base";
    public static final String ATTR_PATH = "path";
    public static final String ATTR_ID = "id";
    public static final String ATTR_NAME = "name";
    public static final String ATTR_REQUIRED = "required";
    public static final String ATTR_STYLE = "style";
    public static final String ATTR_TYPE = "type";
    public static final String ATTR_DEFAULT = "default";
    public static final String ATTR_VALUE = "value";
    public static final String ATTR_STATUS = "status";
    public static final String ATTR_ELEMENT = "element";
    public static final String ATTR_MEDIATYPE = "mediaType";

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

    public static void closeTag(StringBuffer sb) {
        sb.append(XmlUtil.closeTag(TAG_RESOURCES));
        sb.append(XmlUtil.closeTag(TAG_APPLICATION));
    }

    public static void paramString(StringBuffer sb, String name) {
        paramString(sb, name, "xsd:string", "query", false);
    }

    public static void paramString(StringBuffer sb, String name, String type,
                                   String style, boolean required) {
        sb.append(XmlUtil.tag(TAG_APPLICATION,
                              XmlUtil.attrs(ATTR_NAME, name, ATTR_TYPE, type,
                                            ATTR_STYLE, style, ATTR_REQUIRED,
                                            "" + required)));
    }
    //  <param name="appid" type="xsd:string"   style="query" required="true"/> 

}
