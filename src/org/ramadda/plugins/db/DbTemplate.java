/*
* Copyright (c) 2008-2021 Geode Systems LLC
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
