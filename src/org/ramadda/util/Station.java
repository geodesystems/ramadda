/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;

public class Station {

    private String id;

    private String name;

    private double latitude;

    private double longitude;

    private double elevation;

    public Station(String id, String name, double latitude, double longitude,
                   double elevation) {
        this.id        = id;
        this.name      = name;
        this.latitude  = latitude;
        this.longitude = longitude;
        this.elevation = elevation;
    }

    public void setId(String value) {
        id = value;
    }

    public String getId() {
        return id;
    }

    public void setName(String value) {
        name = value;
    }

    public String getName() {
        return name;
    }

    public void setLatitude(double value) {
        latitude = value;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLongitude(double value) {
        longitude = value;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setElevation(double value) {
        elevation = value;
    }

    public double getElevation() {
        return elevation;
    }

    public String toString() {
        return id + " lat:" + latitude + " lon:" + longitude;
    }
}
