/**
   Copyright (c) 2008-2021 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util.geo;


/**
 *
 */
public class Point {

    /** _more_ */
    private double latitude;

    /** _more_ */
    private double longitude;

    /**
     * _more_
     */
    public Point() {}


    /**
     * _more_
     *
     * @param north _more_
     * @param west _more_
     * @param south _more_
     * @param east _more_
     */
    public Point(double lat, double lon) {
        this.latitude = lat;
	this.longitude = lon;
    }



    /**
       Set the Latitude property.

       @param value The new value for Latitude
    **/
    public void setLatitude (double value) {
	latitude = value;
    }

    /**
       Get the Latitude property.

       @return The Latitude
    **/
    public double getLatitude () {
	return latitude;
    }

    /**
       Set the Longitude property.

       @param value The new value for Longitude
    **/
    public void setLongitude (double value) {
	longitude = value;
    }

    /**
       Get the Longitude property.

       @return The Longitude
    **/
    public double getLongitude () {
	return longitude;
    }



}
