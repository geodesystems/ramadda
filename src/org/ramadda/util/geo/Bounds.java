/**
Copyright (c) 2008-2026 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util.geo;

import java.awt.geom.Rectangle2D;

public class Bounds {
    private double north = Double.NaN;
    private double west = Double.NaN;
    private double south = Double.NaN;
    private double east = Double.NaN;
    private Rectangle2D.Double rect2D;
    public Bounds() {}

    public Bounds(double north, double west, double south, double east) {
        this.north = north;
        this.west  = west;
        this.south = south;
        this.east  = east;
    }

    public Bounds(double north, double west) {
	this(north,west,north,west);
    }

    public Point getCenter() {
        return new Point(south + (north - south) / 2,
                         west + (east - west) / 2);
    }

    public String toString() {
        return "north:" + north + " west:" + west + " south:" + south
               + " east:" + east;
    }

    public boolean contains(double lat, double lon) {
        return (Double.isNaN(north) || lat <= north) &&
	    (Double.isNaN(south)|| lat >= south) &&
	    (Double.isNaN(west) || lon >= west) &&
	    (Double.isNaN(east) || lon <= east);

    }

    public boolean contains(Bounds b) {
        return contains(b.north,b.west) &&
	    contains(b.north,b.east) &&
	    contains(b.south,b.east) &&
	    contains(b.south,b.west);
    }

    public Rectangle2D.Double getRectangle2D() {
	if(rect2D==null) {
	    rect2D  =  new Rectangle2D.Double(getWest(), getSouth(),
					      getEast() - getWest(),
					      getNorth()
					      - getSouth());
	}
	return rect2D;
    }

    public boolean intersects(Bounds other) {
	Rectangle2D.Double entryRect = other.getRectangle2D();
	Rectangle2D.Double queryRect = this.getRectangle2D();	
	return  (entryRect.intersects(queryRect)
		 || entryRect.contains(queryRect)
		 || queryRect.contains(entryRect));
    }

    public void expand(Point point) {
        expand(point.getLatitude(), point.getLongitude());
    }

    public void expand(double lat, double lon) {
        this.north = Double.isNaN(north)
                     ? lat
                     : Math.max(this.north, lat);
        this.south = Double.isNaN(south)
                     ? lat
                     : Math.min(this.south, lat);
        this.west  = Double.isNaN(west)
                     ? lon
                     : Math.min(this.west, lon);
        this.east  = Double.isNaN(east)
                     ? lon
                     : Math.max(this.east, lon);
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

}
