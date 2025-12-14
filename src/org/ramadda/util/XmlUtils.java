/**
   Copyright (c) 2008-2026 Geode Systems LLC
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

import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;

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

    public static void appendCdataBytes(Appendable sb, byte[] bytes)
	throws Exception {
        sb.append("<![CDATA[");
        sb.append(XmlUtil.encodeBase64(bytes));
        sb.append("]]>");
    }

    public static void appendCdata(Appendable sb, String s) throws Exception {
        sb.append("<![CDATA[");
        sb.append(s);
        sb.append("]]>");
    }

    public static String elementToString(Element element) {
	try { 
	    removeWhitespaceTextNodes(element);
	    Transformer t =TransformerFactory.newInstance().newTransformer();
 	    t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
	    t.setOutputProperty(OutputKeys.INDENT, "yes");
	    t.setOutputProperty(OutputKeys.METHOD, "xml");
	    t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
	    t.setOutputProperty("{http://xml.apache.org/xalan}indent-amount", "2");
	    StringWriter writer = new StringWriter();
	    t.transform(new DOMSource(element), new StreamResult(writer));
	    return writer.toString();
	} catch (TransformerException e) {
	    throw new RuntimeException(e);
	}
    }



    public static void removeWhitespaceTextNodes(Node node) {
	NodeList children = node.getChildNodes();
	for (int i = children.getLength() - 1; i >= 0; i--) {
	    Node child = children.item(i);
	    if (child.getNodeType() == Node.TEXT_NODE) {
		if (child.getTextContent().trim().isEmpty()) {
		    node.removeChild(child);
		}
	    } else if (child.getNodeType() == Node.ELEMENT_NODE) {
		removeWhitespaceTextNodes(child);
	    }
	}
    }

}
