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

package org.ramadda.plugins.map;


import org.ramadda.repository.metadata.Metadata;
import org.ramadda.util.ColorTable;

import org.ramadda.util.Json;
import org.ramadda.util.KmlUtil;
import org.ramadda.util.Utils;

import org.w3c.dom.CDATASection;
import org.w3c.dom.Element;

import ucar.unidata.gis.shapefile.*;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.awt.Color;
import java.awt.geom.Rectangle2D;


import java.text.DecimalFormat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;


/**
 * Class description
 *
 *
 * @version        $version$, Mon, Nov 26, '18
 * @author         Enter your name here...
 */
public class DbaseDataWrapper {

    /** _more_ */
    DbaseData data;

    /** _more_ */
    String name;

    /** _more_ */
    String lcname;

    /** _more_ */
    String label;

    /** _more_ */
    DbaseDataWrapper keyWrapper;

    /** _more_ */
    List<DbaseDataWrapper> combine;

    /** _more_ */
    Properties properties;

    /**
     * _more_
     *
     * @param name _more_
     * @param data _more_
     * @param properties _more_
     * @param pluginProperties _more_
     */
    public DbaseDataWrapper(String name, DbaseData data,
                            Properties properties,
                            Properties pluginProperties) {
        this.name       = name;
        this.data       = data;
        this.properties = properties;
        if (properties != null) {
            this.label = (String) properties.get("map." + name.toLowerCase()
                    + ".label");
        }
        if ((this.label == null) && (pluginProperties != null)) {
            this.label = (String) pluginProperties.get("map."
                    + name.toLowerCase() + ".label");
        }


    }

    /**
     * _more_
     *
     * @param name _more_
     * @param keyWrapper _more_
     * @param properties _more_
     * @param pluginProperties _more_
     */
    public DbaseDataWrapper(String name, DbaseDataWrapper keyWrapper,
                            Properties properties,
                            Properties pluginProperties) {
        this.name       = name;
        this.keyWrapper = keyWrapper;
        this.properties = properties;
        if (properties != null) {
            this.label = (String) properties.get("map." + name.toLowerCase()
                    + ".label");
        }
        if ((this.label == null) && (pluginProperties != null)) {
            this.label = (String) pluginProperties.get("map."
                    + name.toLowerCase() + ".label");
        }
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
     * @param c _more_
     */
    public void setCombine(List<DbaseDataWrapper> c) {
        this.combine = c;
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
    public String getLowerCaseName() {
        if (lcname == null) {
            lcname = name.toLowerCase();
        }

        return lcname;
    }

    /**
     * _more_
     *
     * @param l _more_
     */
    public void setLabel(String l) {
        this.label = l;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getLabel() {
        if (label == null) {
            label = Utils.makeLabel(name);
        }

        return label;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getType() {
        if (data == null) {
            return DbaseData.TYPE_CHAR;
        }

        return data.getType();
    }

    public boolean  isNumeric() {
        return this.getType() == DbaseData.TYPE_NUMERIC;
    }

    /**
     * _more_
     *
     * @param index _more_
     *
     * @return _more_
     */
    public double getDouble(int index) {
        if (data == null) {
            String s = getString(index);
            if (s != null) {
                return Double.parseDouble(s);
            }

            return 1.0;
        }

        return data.getDouble(index);
    }

    /**
     * _more_
     *
     * @param index _more_
     *
     * @return _more_
     */
    public String getString(int index) {
        if (data == null) {
            if (keyWrapper != null) {
                String key = keyWrapper.getData(index).toString();
                String v = (String) properties.get("map." + key + "." + name);
                if (v != null) {
                    return v;
                }
            } else if (combine != null) {
                StringBuilder sb = new StringBuilder();
                for (DbaseDataWrapper dbd : combine) {
                    sb.append(dbd.getString(index));
                }

                return sb.toString();
            }

            return "NA";
        }

        return data.getString(index);
    }

    /**
     * _more_
     *
     * @param index _more_
     *
     * @return _more_
     */
    public Object getData(int index) {
        if (data == null) {
            return getString(index);
        }

        return data.getData(index);
    }

}
