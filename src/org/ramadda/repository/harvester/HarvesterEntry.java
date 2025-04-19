/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.harvester;

import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.output.OutputHandler;
import org.ramadda.repository.type.*;

import org.w3c.dom.*;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.regex.*;

public class HarvesterEntry {
    public static final String TAG_URLENTRY = "urlentry";
    public static final String ATTR_URL = "url";
    public static final String ATTR_NAME = "name";
    public static final String ATTR_DESCRIPTION = "description";
    public static final String ATTR_GROUP = "group";
    String url;
    String name;
    String description;
    String baseGroupId;
    String group;

    private Hashtable properties = new Hashtable();

    public HarvesterEntry() {}

    public HarvesterEntry(String url, String name, String description,
                          String group, String baseGroupId) {
        this.url         = url;
        this.name        = name;
        this.description = description;
        this.group       = group;
        this.baseGroupId = baseGroupId;

    }

    public HarvesterEntry(Element node) {
        this.url         = XmlUtil.getAttribute(node, ATTR_URL, "");
        this.name        = XmlUtil.getAttribute(node, ATTR_NAME, "");
        this.description = XmlUtil.getAttribute(node, ATTR_DESCRIPTION, "");
        this.group       = XmlUtil.getAttribute(node, ATTR_GROUP, "");
        this.baseGroupId = XmlUtil.getAttribute(node,
                Harvester.ATTR_BASEGROUP, "");
    }

    public Element toXml(Element element) throws Exception {
        Element node = XmlUtil.create(element.getOwnerDocument(),
                                      TAG_URLENTRY, element, new String[] {
            ATTR_URL, url, ATTR_NAME, name, ATTR_DESCRIPTION, description,
            ATTR_GROUP, group, Harvester.ATTR_BASEGROUP, baseGroupId
        });

        return node;
    }

    public String toString() {
        return url;
    }

    public void setUrl(String value) {
        url = value;
    }

    public String getUrl() {
        return url;
    }

    public void setName(String value) {
        name = value;
    }

    public String getName() {
        return name;
    }

    public void setDescription(String value) {
        description = value;
    }

    public String getDescription() {
        return description;
    }

    public void setBaseGroupId(String value) {
        baseGroupId = value;
    }

    public String getBaseGroupId() {
        return baseGroupId;
    }

    public void setGroup(String value) {
        group = value;
    }

    public String getGroup() {
        return group;
    }

    public void setProperties(Hashtable value) {
        properties = value;
    }

    public Hashtable getProperties() {
        return properties;
    }

}
