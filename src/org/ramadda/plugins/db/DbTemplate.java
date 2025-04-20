/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.db;

import org.w3c.dom.*;

import ucar.unidata.xml.XmlUtil;

public class DbTemplate {
    String id;
    String name;
    String entry;
    String prefix;
    String suffix;
    String mime;

    public DbTemplate(Element node) {
        id     = XmlUtil.getAttribute(node, "id");
        name   = XmlUtil.getAttribute(node, "name", id);
        entry  = XmlUtil.getGrandChildText(node, "contents");
        prefix = XmlUtil.getGrandChildText(node, "prefix");
        suffix = XmlUtil.getGrandChildText(node, "suffix");
        mime   = XmlUtil.getAttribute(node, "mimetype", (String) null);
    }
}
