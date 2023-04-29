/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util.grid;


import org.ramadda.util.geo.Bounds;


/**
 */
public class Grid {

    /** number of rows in grid */
    private int height;

    /** number of columns in grid */
    private int width;

    /** bounds */
    private double north;

    /** bounds */
    private double west;

    /** bounds */
    private double south;

    /** bounds */
    private double east;

    /** degrees width */
    private double gridWidth;

    /** degrees height */
    private double gridHeight;

    /** _more_ */
    private double cellWidth;

    /** degrees height */
    private double cellHeight;

    /** _more_ */
    private double cellWidth2;

    /** degrees height */
    private double cellHeight2;



    /**
     * ctor
     *
     * @param width number of columns
     * @param height number of rows
     * @param north northern bounds
     * @param west west bounds
     * @param south southern bounds
     * @param east east bounds
     */
    public Grid(int width, int height, double north, double west,
                double south, double east) {
        this.width      = width;
        this.height     = height;
        this.north      = north;
        this.west       = west;
        this.south      = south;
        this.east       = east;
        this.gridWidth  = east - west;
        this.gridHeight = north - south;
        cellWidth       = gridWidth / width;
        cellHeight      = gridHeight / height;
        cellWidth2      = cellWidth / 2;
        cellHeight2     = cellHeight / 2;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return north + " " + west + " " + south + " " + east;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getHeight() {
        return height;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getWidth() {
        return width;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public double getGridHeight() {
        return gridHeight;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public double getGridWidth() {
        return gridWidth;
    }

    /**
     *  Get the Bounds property.
     *
     *  @return The Bounds
     */
    public Bounds getBounds() {
        return new Bounds(north, west, south, east);
    }


    /**
     *  Set the South property.
     *
     *  @param value The new value for South
     */
    public void setSouth(double value) {
        south = value;
    }

    /**
     *  Get the South property.
     *
     *  @return The South
     */
    public double getSouth() {
        return south;
    }

    /**
     *  Set the East property.
     *
     *  @param value The new value for East
     */
    public void setEast(double value) {
        east = value;
    }

    /**
     *  Get the East property.
     *
     *  @return The East
     */
    public double getEast() {
        return east;
    }

    /**
     *  Set the North property.
     *
     *  @param value The new value for North
     */
    public void setNorth(double value) {
        north = value;
    }

    /**
     *  Get the North property.
     *
     *  @return The North
     */
    public double getNorth() {
        return north;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public double getCellHeight() {
        return cellHeight;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public double getCellWidth() {
        return cellWidth;
    }



    /**
     *  Set the West property.
     *
     *  @param value The new value for West
     */
    public void setWest(double value) {
        west = value;
    }

    /**
     *  Get the West property.
     *
     *  @return The West
     */
    public double getWest() {
        return west;
    }



    /**
     * get the y index corresponding to the given latitude
     *
     * @param lat latitude
     *
     * @return y index
     */
    public int getLatitudeIndex(double lat) {
        if (lat > north) {
            return 0;
        }
        if (lat < south) {
            return height - 1;
        }
        int idx = (int) ((north - lat) / cellHeight);
        if (idx < 0) {
            idx = 0;
        } else if (idx >= height) {
            idx = height - 1;
        }

        //        System.err.println("Lat:" + lat +" index:" + idx);
        return idx;
    }


    /**
     * get the x index corresponding to the given longitude
     *
     * @param lon longitude
     *
     * @return x index
     */
    public int getLongitudeIndex(double lon) {
        if (lon < west) {
            return 0;
        }
        if (lon > east) {
            return width - 1;
        }
        int idx = (int) ((lon - west) / cellWidth);
        if (idx < 0) {
            idx = 0;
        } else if (idx >= width) {
            idx = width - 1;
        }

        //        System.err.println("Lon:" + lon +" index:" + idx);
        return idx;
    }

    /**
     * _more_
     *
     * @param rowIndex _more_
     *
     * @return _more_
     */
    public double getLatitude(int rowIndex) {
        //Offset to get the center
        return north - cellHeight2 - rowIndex * cellHeight;
    }

    /**
     * _more_
     *
     * @param colIndex _more_
     *
     * @return _more_
     */
    public double getLongitude(int colIndex) {
        //Offset to get the center
        return west + cellWidth2 + colIndex * cellWidth;
    }



}
