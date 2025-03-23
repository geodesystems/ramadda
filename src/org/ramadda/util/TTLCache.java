/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;


import ucar.unidata.util.Misc;



import java.util.ArrayList;
import java.util.Date;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;


/**
 * Supports a cache that holds time limited entries (time to live)
 * Note: this only removes items from the cache when a get is performed and the item has expired
 *
 *
 * @param <KEY>
 * @param <VALUE_TYPE>
 */
@SuppressWarnings("unchecked")
public class TTLCache<KEY, VALUE_TYPE> {

    /** _more_ */
    private static Object MUTEX = new Object();

    /** _more_ */
    private static List<TTLCache> caches = new ArrayList<TTLCache>();

    /** _more_ */
    private static Runnable ttlRunnable;

    /** helper for ttl */
    public static long MS_IN_A_MINUTE = 1000 * 60;

    /** _more_ */
    public static long MS_IN_AN_HOUR = 1000 * 60 * 60;

    /** helper for ttl */
    public static long MS_IN_A_DAY = MS_IN_AN_HOUR * 24;

    /** the cache */
    private Hashtable<KEY, CacheEntry<VALUE_TYPE>> cache =
        new Hashtable<KEY, CacheEntry<VALUE_TYPE>>();

    /** how long should the objects be in the cache */
    private long timeThreshold;

    /** should we update the time when a get is performed */
    private boolean updateTimeOnGet = false;

    /** how big should the cache become until its cleared */
    private int sizeLimit = -1;

    /** _more_ */
    public boolean debug = false;

    /**  */
    private String name;


    /**
     * default ctor. 1 hour in cache. No time reset. No size limit
     */
    public TTLCache() {
        this(MS_IN_AN_HOUR, -1, false);
    }

    /**
     * ctor. No time reset. No size limit
     *
     *
     * @param timeThresholdInMilliseconds time in cache
     */
    public TTLCache(long timeThresholdInMilliseconds) {
        this(timeThresholdInMilliseconds, -1, false);
    }

    /**
     
     *
     * @param timeThresholdInMilliseconds _more_
     * @param name _more_
     */
    public TTLCache(long timeThresholdInMilliseconds, String name) {
        this(timeThresholdInMilliseconds, -1, false, name);
    }

    /**
     * ctor. No time reset.
     *
     *
     * @param timeThresholdInMilliseconds time in cache
     * @param sizeLimit cache size limit
     */
    public TTLCache(long timeThresholdInMilliseconds, int sizeLimit) {
        this(timeThresholdInMilliseconds, sizeLimit, false);
    }

    /**
     
     *
     * @param timeThresholdInMilliseconds _more_
     * @param sizeLimit _more_
     * @param name _more_
     */
    public TTLCache(long timeThresholdInMilliseconds, int sizeLimit,
                    String name) {
        this(timeThresholdInMilliseconds, sizeLimit, false, name);
    }



    /**
     * ctor. No time reset.
     *
     *
     * @param timeThresholdInMilliseconds time in cache
     * @param sizeLimit cache size limit
     * @param updateTimeOnGet if true then on a get reset the time to current time
     */
    public TTLCache(long timeThresholdInMilliseconds, int sizeLimit,
                    boolean updateTimeOnGet) {
        this(timeThresholdInMilliseconds, sizeLimit, updateTimeOnGet, null);

    }

    /**
     
     *
     * @param timeThresholdInMilliseconds _more_
     * @param sizeLimit _more_
     * @param updateTimeOnGet _more_
     * @param name _more_
     */
    public TTLCache(long timeThresholdInMilliseconds, int sizeLimit,
                    boolean updateTimeOnGet, String name) {
        if (name == null) {
            name = Utils.getStack(1, "TTL").replaceAll("\n",
                                  " ").replaceAll(".*\\((.*)\\.java.*", "$1");
        }
        this.name            = name;
        this.timeThreshold   = timeThresholdInMilliseconds;
        this.sizeLimit       = sizeLimit;
        this.updateTimeOnGet = updateTimeOnGet;
        synchronized (MUTEX) {
            caches.add(this);
            //      System.err.println("new TTLCache #caches:" + caches.size() +" cache:" + name);
            if (ttlRunnable == null) {
                ttlRunnable = new Runnable() {
                    public void run() {
                        while (true) {
                            //Check every 60 seconds
                            Misc.sleepSeconds(60);
                            for (TTLCache cache : caches) {
                                cache.checkCache();
                            }
                        }
                    }
                };
                Misc.run(ttlRunnable);
            }
        }
    }


    public String getName() {
	return name;
    }

    /**
     */
    public static void clearCaches() {
        synchronized (MUTEX) {
            for (TTLCache cache : caches) {
                cache.clearCache();
            }
        }
    }

    /**
     *
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public static void getInfo(Appendable sb) throws Exception {
        synchronized (MUTEX) {
            for (TTLCache cache : caches) {
                sb.append(cache.name + " size:" + cache.size() + "<br>");
            }
        }
    }



    /**
     *
     * @param cache _more_
     */
    public static void finishedWithCache(TTLCache cache) {
        if (cache != null) {
            cache.finishedWithCache();
        }
    }

    /**
     */
    private void finishedWithCache() {
        synchronized (MUTEX) {
            int sizeBefore = caches.size();
            caches.remove(this);
            cache = null;
            System.err.println("TTLCache.finished:" + sizeBefore + " "
                               + caches.size());
        }
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public int size() {
        return cache.size();
    }

    /**
     * _more_
     *
     * @param t _more_
     */
    public void setTimeThreshold(long t) {
        this.timeThreshold = t;
    }



    /**
     * _more_
     */
    public synchronized void clearCache() {
        List toRemove = new ArrayList();
        for (Enumeration keys = cache.keys(); keys.hasMoreElements(); ) {
            toRemove.add(keys.nextElement());
        }
        for (Object o : toRemove) {
            remove(o);
        }
        cache = new Hashtable<KEY, CacheEntry<VALUE_TYPE>>();
    }

    /**
     * _more_
     *
     * @param value _more_
     */
    public void cacheRemove(VALUE_TYPE value) {}

    /**
     * _more_
     */
    private synchronized void checkCache() {
        if (debug) {
            System.err.println("checkCache");
        }
        List toRemove = new ArrayList();
        Date now      = new Date();
        for (Enumeration keys = cache.keys(); keys.hasMoreElements(); ) {
            Object key = keys.nextElement();
            if (debug) {
                System.err.println("\tkey:" + key);
            }
            CacheEntry cacheEntry = (CacheEntry) cache.get(key);
            long       timeDiff   = now.getTime() - cacheEntry.time;
            if (timeDiff > timeThreshold) {
                toRemove.add(key);
            }
        }
        for (Object o : toRemove) {
            if (debug) {
                System.err.println("\tremoving:" + o);
            }
            remove(o);
        }

    }

    /**
     * put the value
     *
     * @param key key
     * @param value value
     */
    public synchronized void put(KEY key, VALUE_TYPE value) {
        if ((sizeLimit > 0) && (cache.size() > sizeLimit)) {
            clearCache();
        }
        remove(key);
	//	if(isDebug()) System.err.println("PUT:" + key);
        cache.put(key, new CacheEntry<VALUE_TYPE>(value));
    }

    /**
     * _more_
     *
     * @param key _more_
     */
    public synchronized void remove(Object key) {
	//	if(isDebug()) System.err.println("REMOVE:" + key);
        CacheEntry<VALUE_TYPE> entry = cache.get(key);
        if (entry != null) {
            cacheRemove(entry.object);
        }
        cache.remove(key);
    }

    public boolean isDebug() {
	return false;
    }

    /**
     * get the value
     *
     * @param key key
     *
     * @return value or null if not in cache or entry has expired
     */
    public synchronized VALUE_TYPE get(Object key) {
        CacheEntry cacheEntry = cache.get(key);
        if (cacheEntry == null) {
	    //if(isDebug())System.err.println("GET-null:" + key);
            return null;
        }

	if(!cacheValueOk((VALUE_TYPE)cacheEntry.object)) {
            cache.remove(key);
	    return null;
	}

        Date now      = new Date();
        long timeDiff = now.getTime() - cacheEntry.time;
        if (timeDiff > timeThreshold) {
            cache.remove(key);
	    //if(isDebug())System.err.println("GET-TIME:" + "now:" + now +" then:" + new Date(cacheEntry.time) +" thr:" + timeThreshold);
            return null;
        }
        if (updateTimeOnGet) {
            cacheEntry.resetTime();
        }

	//	if(isDebug()) System.err.println("GET-OK:" + key);
        return (VALUE_TYPE) cacheEntry.object;
    }


    public  boolean cacheValueOk(VALUE_TYPE v) {
	return true;
    }


    /**
     * Class description
     *
     *
     * @author     Jeff McWhirter
     *
     * @param <VALUE_TYPE> Type of object
     */
    private class CacheEntry<VALUE_TYPE> {

        /** time put in cache */
        long time;

        /** the object */
        VALUE_TYPE object;

        /**
         * ctor
         *
         * @param object the object
         */
        public CacheEntry(VALUE_TYPE object) {
            this.object = object;
            resetTime();
        }

        /**
         * reset time in cache
         */
        public void resetTime() {
            this.time = new Date().getTime();
        }


    }


}
