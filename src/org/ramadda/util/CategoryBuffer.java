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
 */
@SuppressWarnings("unchecked")
public class CategoryBuffer {

    /** _more_ */
    List<SortableObject<String>> categories =
        new ArrayList<SortableObject<String>>();

    /** _more_ */
    Hashtable<String, StringBuilder> buffers = new Hashtable<String,
                                                   StringBuilder>();

    /**
     * _more_
     */
    public CategoryBuffer() {}

    /**
     * _more_
     *
     * @param category _more_
     *
     * @return _more_
     */
    public StringBuilder get(String category) {
        return get(category, false);
    }


    /**
     * _more_
     *
     * @param category _more_
     * @param addToFront _more_
     *
     * @return _more_
     */
    public StringBuilder get(String category, boolean addToFront) {
        return get(SortableObject.MAX_PRIORITY, category, addToFront);
    }

    /**
     *
     * @param priority _more_
     * @param category _more_
      * @return _more_
     */
    public StringBuilder get(int priority, String category) {
        return get(priority, category, false);
    }

    /**
     *
     * @param priority _more_
     * @param category _more_
     * @param addToFront _more_
      * @return _more_
     */
    public StringBuilder get(int priority, String category,
                             boolean addToFront) {
        if (category == null) {
            category = "";
        }
        StringBuilder sb = buffers.get(category);
        if (sb == null) {
            sb = new StringBuilder();
            buffers.put(category, sb);
            SortableObject<String> po = new SortableObject<String>(priority,
                                            category);
            if (addToFront) {
                categories.add(0, po);
            } else {
                categories.add(po);
            }
        }

        return sb;
    }

    /**
     * _more_
     *
     * @param category _more_
     */
    public void moveToFront(String category) {
        SortableObject<String> po = new SortableObject<String>(category);
        categories.remove(po);
        categories.add(0, po);
    }


    /**
     * _more_
     *
     * @param category _more_
     * @param object _more_
     */
    public void append(String category, Object object) {
        get(category).append(object);
    }

    /**
     * _more_
     *
     * @param category _more_
     *
     * @return _more_
     */
    public boolean contains(String category) {
        return buffers.get(category) != null;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List<String> getCategories() {
        java.util.Collections.sort(categories);
        List<String> cats = new ArrayList<String>();
        for (SortableObject<String> po : categories) {
            cats.add(po.getValue());
        }

        return cats;
    }

    /**
      * @return _more_
     */
    public List<SortableObject<String>> getRawCategories() {
        return categories;
    }


}
