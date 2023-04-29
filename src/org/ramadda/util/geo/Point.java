/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util.geo;


import java.util.ArrayList;

import java.util.List;


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
     *
     * @param lat _more_
     * @param lon _more_
     */
    public Point(double lat, double lon) {
        this.latitude  = lat;
        this.longitude = lon;
    }



    /**
     *
     * @param points _more_
      * @return _more_
     */
    public static float[][] getCoordinates(List<Point> points) {
        float[][] coordinates = new float[2][points.size()];
        for (int i = 0; i < points.size(); i++) {
            coordinates[0][i] = (float) points.get(i).getLatitude();
            coordinates[1][i] = (float) points.get(i).getLongitude();

        }

        return coordinates;
    }


    /**
     *
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



}
