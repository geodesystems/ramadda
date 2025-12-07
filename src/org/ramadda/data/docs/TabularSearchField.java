/**
Copyright (c) 2008-2026 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.data.docs;

public class TabularSearchField {
    private String name;
    private String label;
    private String value;

    public TabularSearchField(String name) {
        this.name = name;
    }

    public TabularSearchField(String name, String value) {
        this.name  = name;
        this.value = value;
    }

    public String toString() {
        return "search field: name=" + name + " value=" + value;
    }

    public String getUrlArg() {
        return "search_table_" + name;
    }

    public void setName(String value) {
        name = value;
    }

    public String getName() {
        return name;
    }

    public void setValue(String value) {
        value = value;
    }

    public String getValue() {
        return value;
    }

    public void setLabel(String value) {
        label = value;
    }

    public String getLabel() {
        if (label == null) {
            return name;
        }

        return label;
    }

}
