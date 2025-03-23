/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;


/**
 * Class description
 *
 *
 * @version        $version$, Fri, Aug 23, '13
 * @author         Enter your name here...
 */
public class Station {

    /** _more_ */
    private String id;

    /** _more_ */
    private String name;

    /** _more_ */
    private double latitude;

    /** _more_ */
    private double longitude;

    /** _more_ */
    private double elevation;

    /**
     * _more_
     *
     * @param id _more_
     * @param name _more_
     * @param latitude _more_
     * @param longitude _more_
     * @param elevation _more_
     */
    public Station(String id, String name, double latitude, double longitude,
                   double elevation) {
        this.id        = id;
        this.name      = name;
        this.latitude  = latitude;
        this.longitude = longitude;
        this.elevation = elevation;
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
     *  Set the Elevation property.
     *
     *  @param value The new value for Elevation
     */
    public void setElevation(double value) {
        elevation = value;
    }

    /**
     *  Get the Elevation property.
     *
     *  @return The Elevation
     */
    public double getElevation() {
        return elevation;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return id + " lat:" + latitude + " lon:" + longitude;
    }
}
