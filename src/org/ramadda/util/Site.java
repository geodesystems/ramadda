/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;

import org.ramadda.util.Utils;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.StringUtil;

import java.io.File;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class Site {

    private String id;

    private double latitude;

    private double longitude;

    private String address;

    private String city;

    private String state;

    private String zipCode;

    public Site(String id, double latitude, double longitude) {
        this.id        = id;
        this.latitude  = latitude;
        this.longitude = longitude;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if ( !(o instanceof Site)) {
            return false;
        }

        return id.equals(((Site) o).id);
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

    public void setId(String value) {
        id = value;
    }

    public String getId() {
        return id;
    }

    public void setAddress(String value) {
        address = value;
    }

    public String getAddress() {
        return address;
    }

    public void setCity(String value) {
        city = value;
    }

    public String getCity() {
        return city;
    }

    public void setState(String value) {
        state = value;
    }

    public String getState() {
        return state;
    }

    public void setZipCode(String value) {
        zipCode = value;
    }

    public String getZipCode() {
        return zipCode;
    }

}
