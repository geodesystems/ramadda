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
    List<SortableObject<String>> categories =      new ArrayList<SortableObject<String>>();

    Hashtable<String, StringBuilder> buffers = new Hashtable<String,
                                                   StringBuilder>();

    public CategoryBuffer() {}

    public StringBuilder get(String category) {
        return get(category, false);
    }

    public StringBuilder get(String category, boolean addToFront) {
        return get(SortableObject.MAX_PRIORITY, category, addToFront);
    }

    public StringBuilder get(int priority, String category) {
        return get(priority, category, false);
    }


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

    public void moveToFront(String category) {
        SortableObject<String> po = new SortableObject<String>(category);
        categories.remove(po);
        categories.add(0, po);
    }

    public void append(String category, Object object) {
        get(category).append(object);
    }

    public boolean contains(String category) {
        return buffers.get(category) != null;
    }

    public List<String> getCategories() {
        java.util.Collections.sort(categories);
        List<String> cats = new ArrayList<String>();
        for (SortableObject<String> po : categories) {
            cats.add(po.getValue());
        }

        return cats;
    }

    public List<SortableObject<String>> getRawCategories() {
        return categories;
    }

}
