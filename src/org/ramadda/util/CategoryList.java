/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;



/**
 * A collection of utilities for rss feeds xml.
 *
 * @author Jeff McWhirter
 *
 * @param <T>
 */

public class CategoryList<T> {

    /** _more_ */
    List<String> categories = new ArrayList<String>();

    /** _more_ */
    Hashtable<String, List<T>> map = new Hashtable<String, List<T>>();

    /**
     * _more_
     */
    public CategoryList() {}

    /**
     * _more_
     *
     * @param category _more_
     *
     * @return _more_
     */
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

    /**
     * _more_
     *
     * @param category _more_
     * @param object _more_
     */
    public void add(String category, T object) {
        get(category).add(object);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List<String> getCategories() {
        return categories;
    }


}
