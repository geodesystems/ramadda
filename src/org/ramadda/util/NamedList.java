/**
Copyright (c) 2008-2026 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;

import java.util.ArrayList;
import java.util.List;

public class NamedList<T> {
    private String name;
    List<T> list = new ArrayList<T>();

    public NamedList(String name) {
        this.name = name;
    }

    public NamedList(String name, List<T> list) {
        this.name = name;
        this.list = list;
    }

    public String getName() {
        return name;
    }

    public List<T> getList() {
        return list;
    }

    public void add(T item) {
        list.add(item);
    }

    public String toString() {
        return name + " #:" + list.size();
    }
}
