/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;


import ucar.unidata.util.Misc;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;


/**
 * @param <VALUE>
 */
@SuppressWarnings("unchecked")
public class SortableObject<VALUE> implements Comparable {
    public static final int MAX_PRIORITY = 10000;
    int priority;
    VALUE value;


    /**
     * default ctor. 1 hour in cache. No time reset. No size limit
     */
    public SortableObject(int priority, VALUE value) {
	this.priority = priority;
	this.value = value;
    }

    public SortableObject(VALUE value) {
	this(MAX_PRIORITY, value);
    }


    @Override
    public int compareTo(Object o) {
	SortableObject<VALUE> po  = (SortableObject<VALUE>)o;
	return this.priority - po.priority;
    }

    @Override
    public boolean equals(Object o) {
	if (!(o instanceof SortableObject<?>)) return false;
	SortableObject<VALUE> po  = (SortableObject<VALUE>)o;
	return this.value.equals(po.value);
    }

    public VALUE getValue() {
	return value;
    }

    public String toString() {
	return value.toString() +" " + priority;
    }

}
