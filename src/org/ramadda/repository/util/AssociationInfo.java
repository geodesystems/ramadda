/**
Copyright (c) 2008-2026 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.util;

import org.ramadda.util.MyXmlUtil;

import java.util.HashSet;
import java.util.List;

/**
 * Holds information for generating associations in entries.xml
 */
public class AssociationInfo {
    private String fromId;
    private String toId;
    private String type;

    public AssociationInfo(String fromId, String toId, String type) {
        this.fromId = fromId;
        this.toId   = toId;
        this.type   = type;
    }

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
            xml.append(MyXmlUtil.tag("association",
                                   MyXmlUtil.attrs("from", from, "to", to,
                                       "type", type)));
            xml.append("\n");
        }

    }

    public void setFromId(String value) {
        fromId = value;
    }

    public String getFromId() {
        return fromId;
    }

    public void setToId(String value) {
        toId = value;
    }

    public String getToId() {
        return toId;
    }

    public void setType(String value) {
        type = value;
    }

    public String getType() {
        return type;
    }

}
