/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util.geo;


import org.ramadda.util.JsonUtil;
import org.ramadda.util.Utils;

import ucar.unidata.gis.shapefile.*;


import java.text.DecimalFormat;

import java.util.ArrayList;
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
    Hashtable properties;

    /**
     * _more_
     *
     * @param name _more_
     * @param data _more_
     * @param properties _more_
     * @param pluginProperties _more_
     */
    public DbaseDataWrapper(String name, DbaseData data,
                            Hashtable properties,
                            Hashtable pluginProperties) {
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
                            Hashtable properties,
                            Hashtable pluginProperties) {
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
     * @param fieldDatum _more_
     *
     * @return _more_
     */
    public static Hashtable<String, String[]> getSchema(
            List<DbaseDataWrapper> fieldDatum) {
        Hashtable<String, String[]> schema = new Hashtable<String,
                                                 String[]>();
        if (fieldDatum == null) {
            return schema;
        }
        for (DbaseDataWrapper dbd : fieldDatum) {
            String[] attrs = new String[4];
            String   dtype = null;
            switch (dbd.getType()) {

              case DbaseData.TYPE_BOOLEAN :
                  dtype = "bool";

                  break;

              case DbaseData.TYPE_CHAR :
                  dtype = "string";

                  break;

              case DbaseData.TYPE_NUMERIC :
                  dtype = "double";

                  break;
            }
            attrs[0] = KmlUtil.ATTR_TYPE;
            attrs[1] = dtype;
            attrs[2] = KmlUtil.ATTR_NAME;
            attrs[3] = dbd.getName();
            schema.put(dbd.getName(), attrs);
        }

        return schema;
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

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isNumeric() {
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
     * _more_a
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
