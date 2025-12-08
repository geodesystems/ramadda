/**
Copyright (c) 2008-2026 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util.geo;

import java.util.ArrayList;
import java.util.List;

public class Point {
    private double latitude;
    private double longitude;

    public Point() {}

    public Point(double lat, double lon) {
        this.latitude  = lat;
        this.longitude = lon;
    }

    public static float[][] getCoordinates(List<Point> points) {
        float[][] coordinates = new float[2][points.size()];
        for (int i = 0; i < points.size(); i++) {
            coordinates[0][i] = (float) points.get(i).getLatitude();
            coordinates[1][i] = (float) points.get(i).getLongitude();

        }

        return coordinates;
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

}
