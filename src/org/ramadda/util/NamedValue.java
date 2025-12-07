/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;


public class NamedValue<T> {
    private String name;
    private T value;

    public NamedValue(String name, T value) {
        this.name  = name;
        this.value = value;
    }

    public static Object getValue(String name, NamedValue[] list) {
        for (NamedValue v : list) {
            if (v.getName().equals(name)) {
                return v.getValue();
            }
        }

        return null;
    }

    public String getName() {
        return name;
    }

    public T getValue() {
        return value;
    }

}
