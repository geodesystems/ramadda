/**
Copyright (c) 2008-2026 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.db;

import org.w3c.dom.*;

import org.ramadda.util.MyXmlUtil;

public class DbTemplate {
    String id;
    String name;
    String entry;
    String prefix;
    String suffix;
    String mime;

    public DbTemplate(Element node) {
        id     = MyXmlUtil.getAttribute(node, "id");
        name   = MyXmlUtil.getAttribute(node, "name", id);
        entry  = MyXmlUtil.getGrandChildText(node, "contents");
        prefix = MyXmlUtil.getGrandChildText(node, "prefix");
        suffix = MyXmlUtil.getGrandChildText(node, "suffix");
        mime   = MyXmlUtil.getAttribute(node, "mimetype", (String) null);
    }
}
