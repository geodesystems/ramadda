/**
Copyright (c) 2008-2023 Geode Systems LLC
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

    /**  */
    public static final int MAX_PRIORITY = 10000;

    /**  */
    int priority;

    boolean increasing = true;

    /**  */
    VALUE value;


    /**
     * default ctor. 1 hour in cache. No time reset. No size limit
     *
     * @param priority _more_
     * @param value _more_
     */
    public SortableObject(int priority, VALUE value) {
        this.priority = priority;
        this.value    = value;
    }


    /**
     *
     * @param value _more_
     */
    public SortableObject(VALUE value) {
        this(MAX_PRIORITY, value);
    }


    /**
     *
     * @param o _more_
      * @return _more_
     */
    @Override
    public int compareTo(Object o) {
        SortableObject<VALUE> po = (SortableObject<VALUE>) o;

        return this.priority - po.priority;
    }

    /**
     *
     * @param o _more_
      * @return _more_
     */
    @Override
    public boolean equals(Object o) {
        if ( !(o instanceof SortableObject<?>)) {
            return false;
        }
        SortableObject<VALUE> po = (SortableObject<VALUE>) o;

        return this.value.equals(po.value);
    }

    public int getPriority() {
	return priority;
    }

    /**
      * @return _more_
     */
    public VALUE getValue() {
        return value;
    }

    /**
      * @return _more_
     */
    public String toString() {
        return value.toString() + " " + priority;
    }

}
