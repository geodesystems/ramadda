/**
Copyright (c) 2008-2026 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.util;

import org.ramadda.util.MyXmlUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

/**
 * Holds information for generating entries in entries.xml
 */
public class EntryInfo {

    private String id;
    private String parentId;
    private String name;
    private String type;
    private String childXml;

    public EntryInfo(String id, String name, String parentId, String type,
                     String childXml) {
        this.id       = id;
        this.name     = name;
        this.parentId = parentId;
        this.type     = type;
        this.childXml = childXml;
    }

    public static void appendEntries(StringBuffer xml,
                                     List<EntryInfo> entries,
                                     Hashtable<String, EntryInfo> entryMap,
                                     HashSet<String> processed) {
        List<EntryInfo> tmp = new ArrayList<EntryInfo>(entries);
        entries = new ArrayList<EntryInfo>();
        for (int i = 0; i < tmp.size(); i++) {
            EntryInfo entryInfo = tmp.get(i);
            EntryInfo parent    = entryMap.get(entryInfo.getParentId());
            if (parent == null) {
                if ( !processed.contains(entryInfo.getParentId())
                        && (entryInfo.getParentId().length() > 0)) {
                    System.err.println("No parent for entry:"
                                       + entryInfo.getName() + " parent="
                                       + entryInfo.getParentId());

                    continue;
                }
            }

            /*            System.err.println("Entry:" + entryInfo.getName() + " parentId:"
                               + entryInfo.getParentId() +" found:" + parent);
            */
            entries.add(entryInfo);
        }

        for (EntryInfo entryInfo : entries) {
            EntryInfo.appendEntries(xml, entryInfo, processed, entryMap);
        }
    }

    public static void appendEntries(StringBuffer xml, EntryInfo entryInfo,
                                     HashSet<String> processed,
                                     Hashtable<String, EntryInfo> entryMap) {
        if (processed.contains(entryInfo.getId())) {
            return;
        }
        if ( !processed.contains(entryInfo.getParentId())) {
            EntryInfo parent = entryMap.get(entryInfo.getParentId());
            if (parent == null) {
                if (entryInfo.getParentId().length() > 0) {
                    return;
                }
            } else {
                appendEntries(xml, parent, processed, entryMap);
            }
        }
        processed.add(entryInfo.getId());

        xml.append(
            MyXmlUtil.tag(
                "entry",
                MyXmlUtil.attrs(
                    "type", entryInfo.getType(), "name", entryInfo.getName(),
                    "id", entryInfo.getId(), "parent",
                    entryInfo.getParentId()), entryInfo.getChildXml()));
    }

    public void setId(String value) {
        id = value;
    }

    public String getId() {
        return id;
    }

    public void setType(String value) {
        type = value;
    }

    public String getType() {
        return type;
    }

    public void setParentId(String value) {
        parentId = value;
    }

    public String getParentId() {
        return parentId;
    }

    public void setName(String value) {
        name = value;
    }

    public String getName() {
        return name;
    }

    public void setChildXml(String value) {
        childXml = value;
    }

    public String getChildXml() {
        return childXml;
    }

}
