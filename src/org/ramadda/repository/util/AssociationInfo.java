/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.util;


import ucar.unidata.xml.XmlUtil;

import java.util.HashSet;
import java.util.List;


/**
 * Holds information for generating associations in entries.xml
 */
public class AssociationInfo {

    /** _more_ */
    private String fromId;

    /** _more_ */
    private String toId;

    /** _more_ */
    private String type;

    /**
     * ctor
     *
     * @param fromId _more_
     * @param toId _more_
     * @param type _more_
     */
    public AssociationInfo(String fromId, String toId, String type) {
        this.fromId = fromId;
        this.toId   = toId;
        this.type   = type;
    }

    /**
     * _more_
     *
     * @param xml _more_
     * @param links _more_
     * @param entryMap _more_
     */
    public static void appendAssociations(StringBuffer xml,
                                          List<AssociationInfo> links,
                                          HashSet<String> entryMap) {
        for (AssociationInfo link : links) {
            String from = link.getFromId();
            String to   = link.getToId();
            String type = link.getType();
            if ( !entryMap.contains(to)) {
                System.err.println("Unknown to link:" + from + " " + to);

                continue;
            }
            if ( !entryMap.contains(from)) {
                System.err.println("Unknown from link:" + from + " " + to);

                continue;
            }
            xml.append(XmlUtil.tag("association",
                                   XmlUtil.attrs("from", from, "to", to,
                                       "type", type)));
            xml.append("\n");
        }

    }


    /**
     *  Set the FromId property.
     *
     *  @param value The new value for FromId
     */
    public void setFromId(String value) {
        fromId = value;
    }

    /**
     *  Get the FromId property.
     *
     *  @return The FromId
     */
    public String getFromId() {
        return fromId;
    }

    /**
     *  Set the ToId property.
     *
     *  @param value The new value for ToId
     */
    public void setToId(String value) {
        toId = value;
    }

    /**
     *  Get the ToId property.
     *
     *  @return The ToId
     */
    public String getToId() {
        return toId;
    }

    /**
     *  Set the Type property.
     *
     *  @param value The new value for Type
     */
    public void setType(String value) {
        type = value;
    }

    /**
     *  Get the Type property.
     *
     *  @return The Type
     */
    public String getType() {
        return type;
    }


}
