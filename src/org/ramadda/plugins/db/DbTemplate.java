/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.db;


import org.w3c.dom.*;

import ucar.unidata.xml.XmlUtil;



/**
 * Class description
 *
 *
 * @version        $version$, Wed, Jul 17, '19
 * @author         Enter your name here...
 */
public class DbTemplate {

    /** _more_ */
    String id;

    /** _more_ */
    String name;

    /** _more_ */
    String entry;

    /** _more_ */
    String prefix;

    /** _more_ */
    String suffix;

    /** _more_ */
    String mime;

    /**
     * _more_
     *
     * @param node _more_
     */
    public DbTemplate(Element node) {
        id     = XmlUtil.getAttribute(node, "id");
        name   = XmlUtil.getAttribute(node, "name", id);
        entry  = XmlUtil.getGrandChildText(node, "contents");
        prefix = XmlUtil.getGrandChildText(node, "prefix");
        suffix = XmlUtil.getGrandChildText(node, "suffix");
        mime   = XmlUtil.getAttribute(node, "mimetype", (String) null);
    }
}
