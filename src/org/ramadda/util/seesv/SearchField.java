/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util.seesv;

/**
 */
public class SearchField {

    /** _more_ */
    private String name;

    /** _more_ */
    private String label;

    /** _more_ */
    private String value;

    /**
     * _more_
     *
     * @param name _more_
     */
    public SearchField(String name) {
        this.name = name;
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     */
    public SearchField(String name, String value) {
        this.name  = name;
        this.value = value;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return "search field: name=" + name + " value=" + value;
    }

    /**
     * _more_
     *
     * @return _more_
     */
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
