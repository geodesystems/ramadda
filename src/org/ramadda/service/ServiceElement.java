/*
* Copyright (c) 2008-2018 Geode Systems LLC
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

package org.ramadda.service;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.job.JobManager;
import org.ramadda.repository.output.OutputHandler;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;


import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.PatternFileFilter;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.lang.reflect.*;

import java.net.*;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;

import java.util.Enumeration;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;


import java.util.regex.*;
import java.util.zip.*;




/**
 * Class description
 *
 *
 * @version        $version$, Fri, Oct 10, '14
 * @author         Enter your name here...
 */
public class ServiceElement implements Constants {


    /** _more_ */
    private Hashtable<String, String> map = new Hashtable<String, String>();


    /**
     * _more_
     *
     * @param node _more_
     */
    public ServiceElement(Element node) {
        NodeList nodes = XmlUtil.getElements(node, "replace");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element mapNode = (Element) nodes.item(i);
            map.put(XmlUtil.getAttribute(mapNode, "from"),
                    XmlUtil.getAttribute(mapNode, "to"));
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public Hashtable<String, String> getMap() {
        return map;
    }


}
