/**
Copyright (c) 2008-2026 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class CategoryList<T> {
    List<String> categories = new ArrayList<String>();
    Hashtable<String, List<T>> map = new Hashtable<String, List<T>>();
    public CategoryList() {}

    public List<T> get(String category) {
        if (category == null) {
            category = "";
        }
        List<T> sb = map.get(category);
        if (sb == null) {
            sb = new ArrayList<T>();
            map.put(category, sb);
            categories.add(category);
        }

        return sb;
    }

    public void add(String category, T object) {
        get(category).add(object);
    }

    public List<String> getCategories() {
        return categories;
    }

}
