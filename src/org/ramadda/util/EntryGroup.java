/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;

import org.ramadda.util.HtmlUtils;

import org.w3c.dom.*;

import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

@SuppressWarnings("unchecked")
public class EntryGroup {
    private Object key;
    private String name;
    private List children = new ArrayList();
    private List keys = new ArrayList();
    private Hashtable map = new Hashtable();

    public EntryGroup(Object key) {
        this.key = key;
    }

    public EntryGroup find(Object key) {
        EntryGroup group = (EntryGroup) map.get(key);
        if (group == null) {
            group = new EntryGroup(key);
            map.put(key, group);
            keys.add(key);
        }

        return group;
    }

    public void add(Object obj) {
        children.add(obj);
    }

    public List keys() {
        return keys;
    }

    public void setKey(Object value) {
        key = value;
    }

    public Object getKey() {
        return key;
    }

    public void setName(String value) {
        name = value;
    }

    public String getName() {
        return name;
    }

    public void setChildren(List value) {
        children = value;
    }

    public List getChildren() {
        return children;
    }

    public void setKeys(List value) {
        keys = value;
    }

    public List getKeys() {
        return keys;
    }

    public void setMap(Hashtable value) {
        map = value;
    }

    public Hashtable getMap() {
        return map;
    }

}
