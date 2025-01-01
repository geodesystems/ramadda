/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
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
