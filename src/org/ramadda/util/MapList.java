/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

public class MapList<T, U> {
    private Hashtable<T, U> map = new Hashtable<T, U>();
    private List<T> keys = new ArrayList<T>();

    public List<T> getKeys() {
        return keys;
    }

    public U get(T key) {
        return map.get(key);
    }

    public void put(T key, U value) {
        if ( !map.contains(key)) {
            keys.add(key);
        }
        map.put(key, value);
    }

}
