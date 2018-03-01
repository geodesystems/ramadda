/*
* Copyright (c) 2008-2018 Geode Systems LLC
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
