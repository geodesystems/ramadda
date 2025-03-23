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


/**
 *
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
@SuppressWarnings("unchecked")
public class EntryGroup {

    /** _more_ */
    private Object key;

    /** _more_ */
    private String name;

    /** _more_ */
    private List children = new ArrayList();


    /** _more_ */
    private List keys = new ArrayList();

    /** _more_ */
    private Hashtable map = new Hashtable();

    /**
     * _more_
     *
     * @param key _more_
     */
    public EntryGroup(Object key) {
        this.key = key;
    }




    /**
     * _more_
     *
     * @param key _more_
     *
     * @return _more_
     */
    public EntryGroup find(Object key) {
        EntryGroup group = (EntryGroup) map.get(key);
        if (group == null) {
            group = new EntryGroup(key);
            map.put(key, group);
            keys.add(key);
        }

        return group;
    }

    /**
     * _more_
     *
     * @param obj _more_
     */
    public void add(Object obj) {
        children.add(obj);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List keys() {
        return keys;
    }


    /**
     * Set the Key property.
     *
     * @param value The new value for Key
     */
    public void setKey(Object value) {
        key = value;
    }

    /**
     * Get the Key property.
     *
     * @return The Key
     */
    public Object getKey() {
        return key;
    }

    /**
     * Set the Name property.
     *
     * @param value The new value for Name
     */
    public void setName(String value) {
        name = value;
    }

    /**
     * Get the Name property.
     *
     * @return The Name
     */
    public String getName() {
        return name;
    }

    /**
     * Set the Children property.
     *
     * @param value The new value for Children
     */
    public void setChildren(List value) {
        children = value;
    }

    /**
     * Get the Children property.
     *
     * @return The Children
     */
    public List getChildren() {
        return children;
    }


    /**
     * Set the Keys property.
     *
     * @param value The new value for Keys
     */
    public void setKeys(List value) {
        keys = value;
    }

    /**
     * Get the Keys property.
     *
     * @return The Keys
     */
    public List getKeys() {
        return keys;
    }

    /**
     * Set the Map property.
     *
     * @param value The new value for Map
     */
    public void setMap(Hashtable value) {
        map = value;
    }

    /**
     * Get the Map property.
     *
     * @return The Map
     */
    public Hashtable getMap() {
        return map;
    }


}
