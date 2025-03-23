/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;


/**
 *
 * @param <T>
 * @param <U>
 */
public class MapList<T, U> {

    /** _more_ */
    private Hashtable<T, U> map = new Hashtable<T, U>();

    /** _more_ */
    private List<T> keys = new ArrayList<T>();


    /**
     * _more_
     *
     * @return _more_
     */
    public List<T> getKeys() {
        return keys;
    }

    /**
     * _more_
     *
     * @param key _more_
     *
     * @return _more_
     */
    public U get(T key) {
        return map.get(key);
    }



    /**
     * _more_
     *
     * @param key _more_
     * @param value _more_
     */
    public void put(T key, U value) {
        if ( !map.contains(key)) {
            keys.add(key);
        }
        map.put(key, value);
    }

}
