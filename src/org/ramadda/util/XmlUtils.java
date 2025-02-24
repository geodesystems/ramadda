/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;


import org.w3c.dom.*;

import org.xml.sax.*;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.*;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.security.SignatureException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import javax.xml.parsers.*;


/**
 * A collection of utilities for xml.
 *
 */

@SuppressWarnings("unchecked")
public abstract class XmlUtils {


    public static List findChildren(Node parent, String... tagList) {
	ArrayList found    = new ArrayList();
        NodeList  children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
	    if(tagList.length==0) {
		found.add(child);
		continue;
	    }
	    for(String tag: tagList) {
		if (XmlUtil.isTag(child, tag)) {
		    found.add(child);
		    break;
		}
            }
        }
        return found;
    }



    /**
     * _more_
     *
     * @param sb _more_
     * @param bytes _more_
     *
     * @throws Exception _more_
     */
    public static void appendCdataBytes(Appendable sb, byte[] bytes)
            throws Exception {
        sb.append("<![CDATA[");
        sb.append(XmlUtil.encodeBase64(bytes));
        sb.append("]]>");
    }

    /**
     * _more_
     *
     * @param sb _more_
     * @param s _more_
     *
     * @throws Exception _more_
     */
    public static void appendCdata(Appendable sb, String s) throws Exception {
        sb.append("<![CDATA[");
        sb.append(s);
        sb.append("]]>");
    }


}
