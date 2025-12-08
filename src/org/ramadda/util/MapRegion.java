/**
Copyright (c) 2008-2026 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;

public class MapRegion {
    private String id;
    private String name;
    private String group;
    private double north;
    private double west;
    private double south;
    private double east;

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

    public void setNorth(double value) {
        north = value;
    }

    public double getNorth() {
        return north;
    }

    public void setWest(double value) {
        west = value;
    }

    public double getWest() {
        return west;
    }

    public void setSouth(double value) {
        south = value;
    }

    public double getSouth() {
        return south;
    }

    public void setEast(double value) {
        east = value;
    }

    public double getEast() {
        return east;
    }

    public void setName(String value) {
        name = value;
    }

    public String getName() {
        return name;
    }

    public void setGroup(String value) {
        group = value;
    }

    public String getGroup() {
        return group;
    }

    public boolean isGroup(String group) {
        if (group == null) {
            return true;
        }

        return this.group.equals(group);
    }

    public void setId(String value) {
        id = value;
    }

    public String getId() {
        return id;
    }

    public String toString() {
        return name + "," + id + "," + group + "," + north + "," + west + ","
               + south + "," + east;

    }

}
