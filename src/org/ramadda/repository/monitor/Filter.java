/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.monitor;

import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.io.File;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

import java.util.Hashtable;
import java.util.List;

/**
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.30 $
 */
@SuppressWarnings("unchecked")
public class Filter implements Constants {

    public static final String[] FIELD_TYPES = {
        ARG_TEXT, ARG_TYPE, ARG_USER, ARG_FILESUFFIX, ARG_ANCESTOR, ARG_AREA
    };

    private String field;

    private Object value;

    private boolean doNot = false;

    private Hashtable properties = new Hashtable();

    public Filter() {}

    public Filter(String field, Object value) {
        this(field, value, false);
    }

    public Filter(String field, Object value, boolean doNot) {
        this.field = field;
        this.value = value;
        this.doNot = doNot;
    }

    public Object getProperty(Object key) {
        return properties.get(key);
    }

    public void putProperty(Object key, Object value) {
        properties.put(key, value);
    }

    public void clearProperties() {
        properties = new Hashtable();
    }

    public String toString() {
        return field + "=" + value;
    }

    /**
     * Set the Field property.
     *
     * @param value The new value for Field
     */
    public void setField(String value) {
        field = value;
    }

    /**
     * Get the Field property.
     *
     * @return The Field
     */
    public String getField() {
        return field;
    }

    /**
     * Set the Value property.
     *
     * @param value The new value for Value
     */
    public void setValue(Object value) {
        this.value = value;
    }

    /**
     * Get the Value property.
     *
     * @return The Value
     */
    public Object getValue() {
        return value;
    }

    /**
     * Set the DoNot property.
     *
     * @param value The new value for DoNot
     */
    public void setDoNot(boolean value) {
        doNot = value;
    }

    /**
     * Get the DoNot property.
     *
     * @return The DoNot
     */
    public boolean getDoNot() {
        return doNot;
    }

}
