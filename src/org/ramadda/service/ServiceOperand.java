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

package org.ramadda.service;


import org.ramadda.repository.Entry;

import ucar.unidata.util.Misc;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;


/**
 * Class to hold a set of entries for a ServiceInput or Output
 */
public class ServiceOperand {

    /** The list of entries for this operand */
    private List<Entry> entries;

    /** the description */
    private String description;

    /** Properties */
    private Hashtable properties = new Hashtable();

    /**
     * Create an operand from the entry
     * @param entry the entry
     */
    public ServiceOperand(Entry entry) {
        this(Misc.newList(entry));
    }

    /**
     * _more_
     *
     * @param description _more_
     * @param entry _more_
     */
    public ServiceOperand(String description, Entry entry) {
        this(description, Misc.newList(entry));
    }

    /**
     * Create an operand with a description and list of entries
     *
     * @param entries      the entries
     */
    public ServiceOperand(List<Entry> entries) {
        this("", entries);
    }

    /**
     * Create an operand with a description and list of entries
     *
     * @param description  the description
     * @param entries      the entries
     */
    public ServiceOperand(String description, List<Entry> entries) {
        this.description = description;
        this.entries     = entries;
    }

    /**
     * _more_
     *
     * @param entries _more_
     *
     * @return _more_
     */
    public static List<ServiceOperand> makeOperands(List<Entry> entries) {
        List<ServiceOperand> operands = new ArrayList<ServiceOperand>();
        if (entries == null) {
            return operands;
        }
        for (Entry entry : entries) {
            operands.add(new ServiceOperand(entry));
        }

        return operands;
    }

    /**
     * _more_
     *
     * @param key _more_
     * @param value _more_
     */
    public void putProperty(Object key, Object value) {
        properties.put(key, value);
    }

    /**
     * _more_
     *
     * @param key _more_
     *
     * @return _more_
     */
    public Object getProperty(Object key) {
        return getProperty(key, null);
    }

    /**
     * _more_
     *
     * @param key _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public Object getProperty(Object key, Object dflt) {
        Object value = properties.get(key);
        if (value == null) {
            return dflt;
        }

        return value;
    }


    /**
     * Get the entries
     *
     * @return  the entries
     */
    public List<Entry> getEntries() {
        return entries;
    }

    /**
     * Set the entries
     *
     * @param newEntries  the new entries
     */
    public void setEntries(List<Entry> newEntries) {
        entries = newEntries;
    }

    /**
     * Get the description of this operand
     *
     * @return  the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set the description of the operand
     *
     * @param description  the description
     */
    public void setDescription(String description) {
        this.description = description;
    }

}
