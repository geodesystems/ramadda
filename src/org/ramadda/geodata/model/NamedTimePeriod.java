/*
* Copyright (c) 2008-2019 Geode Systems LLC
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

package org.ramadda.geodata.model;


/**
 * Class to hold properties of a named time period
 */
public class NamedTimePeriod {

    /** the id */
    private String id;

    /** the name */
    private String name;

    /** the group */
    private String group;

    /** start month number */
    private int startMonth;

    /** end month number */
    private int endMonth;

    /** the years */
    private String years;

    /**
     * Create a new named time period
     *
     * @param id   the id
     * @param name the name
     * @param group  the group
     * @param startMonth  start month number
     * @param endMonth    end month number
     * @param years       years
     */
    public NamedTimePeriod(String id, String name, String group,
                           int startMonth, int endMonth, String years) {
        this.id         = id;
        this.name       = name;
        this.group      = group;
        this.startMonth = startMonth;
        this.endMonth   = endMonth;
        this.years      = years;
    }

    /**
     * Get the id
     *
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * Get the name
     *
     * @return  the name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the group
     *
     * @return  the group
     */
    public String getGroup() {
        return group;
    }

    /**
     * Get the start month
     *
     * @return  the start month
     */
    public int getStartMonth() {
        return startMonth;
    }

    /**
     * Get the end month
     *
     * @return the end month
     */
    public int getEndMonth() {
        return endMonth;
    }

    /**
     * Get the years
     *
     * @return  the years
     */
    public String getYears() {
        return years;
    }

    /**
     * Is this in the group?
     *
     * @param group group
     *
     * @return true if in group or group is null
     */
    public boolean isGroup(String group) {
        if ((group == null) || group.equals("all")) {
            return true;
        }

        return this.group.equals(group);
    }

    /**
     * Get a string representation of this
     *
     * @return a string representation of this
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(id);
        sb.append(";");
        sb.append(name);
        sb.append(";");
        sb.append(startMonth);
        sb.append(";");
        sb.append(endMonth);
        sb.append(";");
        sb.append(years);

        return sb.toString();
    }
}
