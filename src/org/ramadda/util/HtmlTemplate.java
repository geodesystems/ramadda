/*
* Copyright (c) 2008-2019 Geode Systems LLC
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

package org.ramadda.util;

import org.ramadda.util.HtmlUtils;
import org.w3c.dom.*;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.StringUtil;
import java.io.*;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.*;
import java.util.regex.*;
import java.util.zip.*;


/**
 * Class HtmlTemplate _more_
 *
 *
 * @author IDV Development Team
 */
public class HtmlTemplate {

    /** _more_ */
    public static final String PROP_PROPERTIES =
        "ramadda.template.properties";


    /** _more_ */
    private PropertyProvider propertyProvider;

    /** _more_ */
    private String name;

    /** _more_ */
    private String id;

    /** _more_ */
    private String template;

    /** _more_ */
    private String path;

    boolean wikify;

    /** _more_ */
    private Hashtable properties = new Hashtable();


    /** _more_ */
    private List<String> propertyIds = new ArrayList<String>();

    /** _more_ */
    private List<String> toks;

    /** _more_ */
    private HashSet hasMacro = new HashSet();

    /**
     * _more_
     *
     *
     *
     * @param propertyProvider _more_
     * @param path _more_
     * @param t _more_
     */
    public HtmlTemplate(PropertyProvider propertyProvider, String path,
                        String t) {
        try {

            this.propertyProvider = propertyProvider;
            this.path             = path;
            Pattern pattern =
                Pattern.compile("(?s)(.*)<properties>(.*)</properties>(.*)");
            Matcher matcher = pattern.matcher(t);
            if (matcher.find()) {
                template = matcher.group(1) + matcher.group(3);
                Properties p = new Properties();
                p.load(new ByteArrayInputStream(matcher.group(2).getBytes()));
                properties.putAll(p);
                //                System.err.println ("got props " + properties);
            } else {
                template = t;
            }
            name = (String) properties.get("name");
            id   = (String) properties.get("id");
	    wikify  =Utils.getProperty(properties,"wikify",false);
            String tmp = (String) properties.get(PROP_PROPERTIES);
            if (tmp != null) {
                propertyIds = StringUtil.split(tmp, ",", true, true);
            }

            if (name == null) {
                name = IOUtil.stripExtension(IOUtil.getFileTail(path));
            }
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }

    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List<String> getPropertyIds() {
        return propertyIds;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getTemplate() {
        return template;
    }

    public void setTemplate(String t) {
         template = t;
    }    

    /**
     * _more_
     *
     * @return _more_
     */
    public List<String> getToks() {
        if (toks == null) {
	    toks = getToks(template);
	}
	return toks;
    }

    public List<String> getToks(String what) {	
	List<String> toks = StringUtil.splitMacros(what);
	for (int i = 0; i < toks.size(); i++) {
	    if (2 * (i / 2) != i) {
		hasMacro.add(toks.get(i));
	    }
        }
        return toks;
    }

    /**
     * _more_
     *
     * @param m _more_
     *
     * @return _more_
     */
    public boolean hasMacro(String m) {
        getToks();

        return hasMacro.contains(m);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getId() {
        return id;
    }


    /**
       Set the Wikify property.

       @param value The new value for Wikify
    **/
    public void setWikify (boolean value) {
	wikify = value;
    }

    /**
       Get the Wikify property.

       @return The Wikify
    **/
    public boolean getWikify () {
	return wikify;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getName() {
        return name;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return name;
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public String getTemplateProperty(String name, String dflt) {
        String value = (String) properties.get(name);
        if (value != null) {
            return value;
        }

        return propertyProvider.getProperty(name, dflt);
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public boolean getTemplateProperty(String name, boolean dflt) {
        String v = getTemplateProperty(name, null);
        if (v == null) {
            return dflt;
        }

        return v.equals("true");
    }


}
