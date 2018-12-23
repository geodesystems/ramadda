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
     * ctor
     *
     * @param timeThresholdInMilliseconds time to live
     */
    public TTLObject(long timeThresholdInMilliseconds) {
        this(null, timeThresholdInMilliseconds);
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
     * ctor.
     *
     * @param object object to store
     * @param timeThresholdInMilliseconds time in cache
     */
    public TTLObject(VALUE object, long timeThresholdInMilliseconds) {
        cache = new TTLCache<String, VALUE>(timeThresholdInMilliseconds);
        if (object != null) {
            put(object);
        }
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
