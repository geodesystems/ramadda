/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;



/**
 */
public class MapRegion {

    /** _more_ */
    private String id;


    /** _more_ */
    private String name;

    /** _more_ */
    private String group;

    /** _more_ */
    private double north;

    /** _more_ */
    private double west;

    /** _more_ */
    private double south;

    /** _more_ */
    private double east;

    /**
     * _more_
     *
     *
     * @param id _more_
     * @param name _more_
     * @param group _more_
     * @param north _more_
     * @param west _more_
     * @param south _more_
     * @param east _more_
     */
    public MapRegion(String id, String name, String group, double north,
                     double west, double south, double east) {
        this.id    = id;
        this.name  = name;
        this.group = group;
        this.north = north;
        this.west  = west;
        this.south = south;
        this.east  = east;
    }

    /**
     *
     *
     * @param id _more_
     * @param name _more_
     * @param group _more_
     * @param lat _more_
     * @param lon _more_
     */
    public MapRegion(String id, String name, String group, double lat,
                     double lon) {
        this.id    = id;
        this.name  = name;
        this.group = group;
        double pad = 0.5;
        this.north = lat + pad;
        this.west  = lon - pad;
        this.south = lat - pad;
        this.east  = lon + pad;
    }




    /**
     * Set the North property.
     *
     * @param value The new value for North
     */
    public void setNorth(double value) {
        north = value;
    }

    /**
     * Get the North property.
     *
     * @return The North
     */
    public double getNorth() {
        return north;
    }

    /**
     * Set the West property.
     *
     * @param value The new value for West
     */
    public void setWest(double value) {
        west = value;
    }

    /**
     * Get the West property.
     *
     * @return The West
     */
    public double getWest() {
        return west;
    }

    /**
     * Set the South property.
     *
     * @param value The new value for South
     */
    public void setSouth(double value) {
        south = value;
    }

    /**
     * Get the South property.
     *
     * @return The South
     */
    public double getSouth() {
        return south;
    }

    /**
     * Set the East property.
     *
     * @param value The new value for East
     */
    public void setEast(double value) {
        east = value;
    }

    /**
     * Get the East property.
     *
     * @return The East
     */
    public double getEast() {
        return east;
    }

    /**
     * Set the Name property.
     *
     * @param value The new value for Name
     */
    public void setName(String value) {
        name = value;
    }

    /**
     * Get the Name property.
     *
     * @return The Name
     */
    public String getName() {
        return name;
    }


    /**
     * Set the Group property.
     *
     * @param value The new value for Group
     */
    public void setGroup(String value) {
        group = value;
    }

    /**
     * Get the Group property.
     *
     * @return The Group
     */
    public String getGroup() {
        return group;
    }


    /**
     * _more_
     *
     * @param group _more_
     *
     * @return _more_
     */
    public boolean isGroup(String group) {
        if (group == null) {
            return true;
        }

        return this.group.equals(group);
    }

    /**
     * Set the Id property.
     *
     * @param value The new value for Id
     */
    public void setId(String value) {
        id = value;
    }

    /**
     * Get the Id property.
     *
     * @return The Id
     */
    public String getId() {
        return id;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return name + "," + id + "," + group + "," + north + "," + west + ","
               + south + "," + east;

    }

}
