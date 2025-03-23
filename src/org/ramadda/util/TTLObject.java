/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;



import java.util.Date;
import java.util.Hashtable;



/**
 * Keep the given object in memory for only a time threshold
 *
 *
 *
 * @param <VALUE>
 */
public class TTLObject<VALUE> {

    /** holds the object */
    private TTLCache<String, VALUE> cache;


    /**
     
     *
     * @param timeThresholdInMilliseconds _more_
     */
    public TTLObject(long timeThresholdInMilliseconds) {
        this(null, timeThresholdInMilliseconds, null);
    }


    /**
     * ctor
     *
     * @param timeThresholdInMilliseconds time to live
     * @param name _more_
     */
    public TTLObject(long timeThresholdInMilliseconds, String name) {
        this(null, timeThresholdInMilliseconds, name);
    }

    /**
     * default ctor. 1 hour in cache. No time reset. No size limit
     *
     * @param object object to store
     */
    public TTLObject(VALUE object) {
        this(object, 1000 * 60 * 60);
    }

    /**
     
     *
     * @param object _more_
     * @param name _more_
     */
    public TTLObject(VALUE object, String name) {
        this(object, 1000 * 60 * 60, name);
    }

    /**
     * ctor.
     *
     * @param object object to store
     * @param timeThresholdInMilliseconds time in cache
     */
    public TTLObject(VALUE object, long timeThresholdInMilliseconds) {
        this(object, timeThresholdInMilliseconds, null);
    }

    /**
     
     *
     * @param object _more_
     * @param timeThresholdInMilliseconds _more_
     * @param name _more_
     */
    public TTLObject(VALUE object, long timeThresholdInMilliseconds,
                     String name) {
        cache = new TTLCache<String, VALUE>(timeThresholdInMilliseconds, -1,
                             name);
        if (object != null) {
            put(object);
        }
    }

    /**
     */
    public synchronized void clearCache() {
        cache.clearCache();
    }

    /**
     * store a new object
     *
     * @param value new object_
     */
    public void put(VALUE value) {
        cache.put("", value);
    }

    /**
     * _more_
     *
     * @param t _more_
     */
    public void setTimeThreshold(long t) {
        cache.setTimeThreshold(t);
    }

    /**
     * get the object or null if its expired
     *
     * @return object
     */
    public VALUE get() {
        return cache.get("");
    }

}
