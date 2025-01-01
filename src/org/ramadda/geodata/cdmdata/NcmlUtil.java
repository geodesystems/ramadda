/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.cdmdata;


import org.ramadda.repository.Entry;

import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;


import java.util.ArrayList;
import java.util.List;


/**
 * A utility class for Ncml for handling NcML generation for
 * aggregations.
 */
public class NcmlUtil {

    /** JoinExisting Aggregation type */
    public static final String AGG_JOINEXISTING = "joinExisting";

    /** JoinNew Aggregation type */
    public static final String AGG_JOINNEW = "joinNew";

    /** Union Aggregation type */
    public static final String AGG_UNION = "union";

    /** Ensemble Aggregation type */
    public static final String AGG_ENSEMBLE = "ensemble";

    /** The NcML XML namespace identifier */
    public static final String XMLNS_XMLNS =
        "http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2";

    /** The netcdf tag */
    public static final String TAG_NETCDF = "netcdf";

    /** The variable tag */
    public static final String TAG_VARIABLE = "variable";

    /** The attribute tag */
    public static final String TAG_ATTRIBUTE = "attribute";

    /** The aggregation tag */
    public static final String TAG_AGGREGATION = "aggregation";

    /** The variableAgg tag */
    public static final String TAG_VARIABLEAGG = "variableAgg";

    /** The name attribute */
    public static final String ATTR_NAME = "name";

    /** The shape attribute */
    public static final String ATTR_SHAPE = "shape";

    /** The type attribute */
    public static final String ATTR_TYPE = "type";

    /** The value attribute */
    public static final String ATTR_VALUE = "value";

    /** The dimName attribute */
    public static final String ATTR_DIMNAME = "dimName";

    /** The coordValue attribute */
    public static final String ATTR_COORDVALUE = "coordValue";

    /** The location attribute */
    public static final String ATTR_LOCATION = "location";

    /** The enhance attribute */
    public static final String ATTR_ENHANCE = "enhance";

    /** The timeUnitsChange attribute */
    public static final String ATTR_TIMEUNITSCHANGE = "timeUnitsChange";

    /** the aggregation type */
    private String aggType;

    /**
     * Create a new NcML utility with the aggregation type
     *
     * @param aggType  the aggregation type
     */
    public NcmlUtil(String aggType) {
        this.aggType = aggType;
    }

    /**
     * Create a String identifier for this object
     *
     * @return  the String identifier
     */
    public String toString() {
        return aggType;
    }

    /**
     * Is this a JoinExisting aggregation?
     *
     * @return true if this is a JoinExisting aggregation
     */
    public boolean isJoinExisting() {
        return aggType.equalsIgnoreCase(AGG_JOINEXISTING);
    }

    /**
     * Is this a JoinNew aggregation?
     *
     * @return  true if JoinNew
     */
    public boolean isJoinNew() {
        return aggType.equalsIgnoreCase(AGG_JOINNEW);
    }

    /**
     * Is this an Union aggregation?
     *
     * @return true if Union
     */
    public boolean isUnion() {
        return aggType.equalsIgnoreCase(AGG_UNION);
    }

    /**
     * Is this an Ensemble aggregation?
     *
     * @return true if ensemble aggregation
     */
    public boolean isEnsemble() {
        return aggType.equalsIgnoreCase(AGG_ENSEMBLE);
    }


    /**
     * Create an open Ncml tag
     *
     * @param sb  the StringBuilder to add to
     */
    public static void openNcml(StringBuilder sb) {
        sb.append(XmlUtil.openTag(TAG_NETCDF,
                                  XmlUtil.attrs(new String[] { "xmlns",
                XMLNS_XMLNS })));
    }

    /**
     * Add the ensemble variable and attributes
     *
     * @param sb the StringBuilder to add to
     * @param name  the name of the ensemble variable
     */
    public static void addEnsembleVariables(StringBuilder sb, String name) {
        addEnsembleVariables(sb, name, null);
    }

    /**
     * Add the ensemble variable and attributes
     *
     * @param sb the StringBuilder to add to
     * @param name  the name of the ensemble variable
     * @param entries list of Entry's
     */
    public static void addEnsembleVariables(StringBuilder sb, String name,
                                            List<Entry> entries) {
        /*
 <variable name='ens' type='String' shape='ens'>
   <attribute name='long_name' value='ensemble coordinate' />
   <attribute name='_CoordinateAxisType' value='Ensemble' />
   <values>run1 run2 run3 run4 run5 run6 run7 run8</values>
 </variable>
        */
        // Get the ensemble names from the entry names
        String values = "";
        if ((entries != null) && !entries.isEmpty()) {
            StringBuilder buf    = new StringBuilder();
            int           ensNum = 1;
            for (Entry e : entries) {
                // for now use integer ensemble numbers
                //buf.append(e.getName());
                //buf.append(" ");
                buf.append(ensNum + " ");
                ensNum++;
            }
            values = XmlUtil.tag("values", "", buf.toString().trim());
        }

        sb.append(XmlUtil.tag(TAG_VARIABLE, XmlUtil.attrs(new String[] {
            ATTR_NAME, name, ATTR_TYPE, "int", ATTR_SHAPE, name
        }), XmlUtil.tag(TAG_ATTRIBUTE, XmlUtil.attrs(new String[] { ATTR_NAME,
                "long_name", ATTR_VALUE,
                "ensemble coordinate" })) + XmlUtil.tag(TAG_ATTRIBUTE,
                    XmlUtil.attrs(new String[] { ATTR_NAME,
                "_CoordinateAxisType", ATTR_VALUE, "Ensemble" })) + values));
    }


}
