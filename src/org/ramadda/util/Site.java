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


/**
 * Class description
 *
 *
 * @version        $version$, Fri, Aug 23, '13
 * @author         Enter your name here...
 */
public class Site {

    /** _more_ */
    private String id;

    /** _more_ */
    private double latitude;

    /** _more_ */
    private double longitude;

    /** _more_ */
    private String address;

    /** _more_ */
    private String city;

    /** _more_ */
    private String state;

    /** _more_ */
    private String zipCode;


    /**
     * _more_
     *
     * @param id _more_
     * @param latitude _more_
     * @param longitude _more_
     */
    public Site(String id, double latitude, double longitude) {
        this.id        = id;
        this.latitude  = latitude;
        this.longitude = longitude;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /**
     * _more_
     *
     * @param o _more_
     *
     * @return _more_
     */
    @Override
    public boolean equals(Object o) {
        if ( !(o instanceof Site)) {
            return false;
        }

        return id.equals(((Site) o).id);
    }


    /**
     *  Set the Latitude property.
     *
     *  @param value The new value for Latitude
     */
    public void setLatitude(double value) {
        latitude = value;
    }

    /**
     *  Get the Latitude property.
     *
     *  @return The Latitude
     */
    public double getLatitude() {
        return latitude;
    }

    /**
     *  Set the Longitude property.
     *
     *  @param value The new value for Longitude
     */
    public void setLongitude(double value) {
        longitude = value;
    }

    /**
     *  Get the Longitude property.
     *
     *  @return The Longitude
     */
    public double getLongitude() {
        return longitude;
    }

    /**
     *  Set the Id property.
     *
     *  @param value The new value for Id
     */
    public void setId(String value) {
        id = value;
    }

    /**
     *  Get the Id property.
     *
     *  @return The Id
     */
    public String getId() {
        return id;
    }


    /**
     *  Set the Address property.
     *
     *  @param value The new value for Address
     */
    public void setAddress(String value) {
        address = value;
    }

    /**
     *  Get the Address property.
     *
     *  @return The Address
     */
    public String getAddress() {
        return address;
    }

    /**
     *  Set the City property.
     *
     *  @param value The new value for City
     */
    public void setCity(String value) {
        city = value;
    }

    /**
     *  Get the City property.
     *
     *  @return The City
     */
    public String getCity() {
        return city;
    }


    /**
     *  Set the State property.
     *
     *  @param value The new value for State
     */
    public void setState(String value) {
        state = value;
    }

    /**
     *  Get the State property.
     *
     *  @return The State
     */
    public String getState() {
        return state;
    }

    /**
     *  Set the ZipCode property.
     *
     *  @param value The new value for ZipCode
     */
    public void setZipCode(String value) {
        zipCode = value;
    }

    /**
     *  Get the ZipCode property.
     *
     *  @return The ZipCode
     */
    public String getZipCode() {
        return zipCode;
    }



}
