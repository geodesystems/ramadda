/*
* Copyright (c) 2008-2021 Geode Systems LLC
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
*     http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.ramadda.data.docs;



/**
 */
public class TabularSearchField {

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
    public TabularSearchField(String name) {
        this.name = name;
    }


    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     */
    public TabularSearchField(String name, String value) {
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
