/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util.seesv;

/**
 */
public class SearchField {

    private String name;

    private String label;

    private String value;

    public SearchField(String name) {
        this.name = name;
    }

    public SearchField(String name, String value) {
        this.name  = name;
        this.value = value;
    }

    public String toString() {
        return "search field: name=" + name + " value=" + value;
    }

    public String getUrlArg() {
        return "search_table_" + name;
    }

    /**
     *  Set the Name property.
     *
     *  @param value The new value for Name
     */
    public void setName(String value) {
        name = value;
    }

    /**
     *  Get the Name property.
     *
     *  @return The Name
     */
    public String getName() {
        return name;
    }

    /**
     *  Set the Value property.
     *
     *  @param value The new value for Value
     */
    public void setValue(String value) {
        value = value;
    }

    /**
     *  Get the Value property.
     *
     *  @return The Value
     */
    public String getValue() {
        return value;
    }

    /**
     * Set the Label property.
     *
     * @param value The new value for Label
     */
    public void setLabel(String value) {
        label = value;
    }

    /**
     * Get the Label property.
     *
     * @return The Label
     */
    public String getLabel() {
        if (label == null) {
            return name;
        }

        return label;
    }

}
