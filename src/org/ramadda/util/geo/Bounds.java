/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util.geo;

import java.awt.geom.Rectangle2D;

/**
 *
 */
public class Bounds {

    /** _more_ */
    private double north = Double.NaN;

    /** _more_ */
    private double west = Double.NaN;

    /** _more_ */
    private double south = Double.NaN;

    /** _more_ */
    private double east = Double.NaN;

    private Rectangle2D.Double rect2D;

    /**
     * _more_
     */
    public Bounds() {}


    /**
     * _more_
     *
     * @param north _more_
     * @param west _more_
     * @param south _more_
     * @param east _more_
     */
    public Bounds(double north, double west, double south, double east) {
        this.north = north;
        this.west  = west;
        this.south = south;
        this.east  = east;
    }


    public Bounds(double north, double west) {
	this(north,west,north,west);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public Point getCenter() {
        return new Point(south + (north - south) / 2,
                         west + (east - west) / 2);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return "north:" + north + " west:" + west + " south:" + south
               + " east:" + east;
    }

    /**
     * _more_
     *
     * @param lat _more_
     * @param lon _more_
     *
     * @return _more_
     */
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


    /**
     *
     * @param point _more_
     */
    public void expand(Point point) {
        expand(point.getLatitude(), point.getLongitude());
    }

    /**
     * _more_
     *
     * @param lat _more_
     * @param lon _more_
     */
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




}
