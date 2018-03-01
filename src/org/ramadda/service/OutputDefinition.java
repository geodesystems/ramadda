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
import org.ramadda.repository.output.OutputHandler;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;




import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.lang.reflect.*;

import java.net.*;

import java.util.ArrayList;
import java.util.Date;
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
 * @version        $version$, Thu, Sep 4, '14
 * @author         Enter your name here...
 */
public class OutputDefinition extends ServiceElement {

    /** _more_ */
    private String entryType;

    /** _more_ */
    private String pattern;

    /** _more_ */
    private String depends;

    /** _more_ */
    private boolean notDepends;

    /** _more_ */
    private boolean useStdout = false;

    /** _more_ */
    private String filename;

    /** _more_ */
    private boolean showResults = false;



    /**
     * _more_
     *
     * @param node _more_
     */
    public OutputDefinition(Element node) {
        super(node);
        entryType = XmlUtil.getAttribute(node, Service.ATTR_TYPE,
                                         TypeHandler.TYPE_FILE);
        pattern     = XmlUtil.getAttribute(node, "pattern", (String) null);
        useStdout   = XmlUtil.getAttribute(node, "stdout", useStdout);
        filename    = XmlUtil.getAttribute(node, "filename", (String) null);
        depends     = XmlUtil.getAttribute(node, "depends", (String) null);
        showResults = XmlUtil.getAttribute(node, "showResults", showResults);
    }


    /**
     * _more_
     *
     * @param xml _more_
     *
     * @throws Exception _more_
     */
    public void toXml(Appendable xml) throws Exception {
        StringBuilder attrs = new StringBuilder();
        Service.attr(attrs, "pattern", pattern);
        Service.attr(attrs, "stdout", useStdout);
        Service.attr(attrs, "filename", filename);
        Service.attr(attrs, "depends", depends);
        Service.attr(attrs, "showResults", showResults);
        xml.append(XmlUtil.tag(Service.TAG_OUTPUT, attrs.toString()));
    }


    /**
     *  Set the EntryType property.
     *
     *  @param value The new value for EntryType
     */
    public void setEntryType(String value) {
        entryType = value;
    }

    /**
     *  Get the EntryType property.
     *
     *  @return The EntryType
     */
    public String getEntryType() {
        return entryType;
    }

    /**
     *  Set the Pattern property.
     *
     *  @param value The new value for Pattern
     */
    public void setPattern(String value) {
        pattern = value;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getFilename() {
        return filename;
    }


    /**
     *  Get the Pattern property.
     *
     *  @return The Pattern
     */
    public String getPattern() {
        return pattern;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getShowResults() {
        return showResults;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getUseStdout() {
        return useStdout;
    }

    /**
     *  Set the Depends property.
     *
     *  @param value The new value for Depends
     */
    public void setDepends(String value) {
        depends = value;
    }

    /**
     *  Get the Depends property.
     *
     *  @return The Depends
     */
    public String getDepends() {
        return depends;
    }


}
