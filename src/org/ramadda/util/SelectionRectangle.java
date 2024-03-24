/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;


import ucar.unidata.util.Misc;

import java.io.*;

import java.net.*;

import java.util.ArrayList;
import java.util.Arrays;

import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import java.util.zip.*;


/**
 *
 */
public class SelectionRectangle {

    /** _more_ */
    private double north = Double.NaN;

    /** _more_ */
    private double west = Double.NaN;

    /** _more_ */
    private double south = Double.NaN;

    /** _more_ */
    private double east = Double.NaN;

    /**
     * _more_
     */
    public SelectionRectangle() {}


    /**
     * _more_
     *
     * @param north _more_
     * @param west _more_
     * @param south _more_
     * @param east _more_
     */
    public SelectionRectangle(double north, double west, double south,
                              double east) {
        this.north = north;
        this.west  = west;
        this.south = south;
        this.east  = east;
    }

    public SelectionRectangle(double[] nwse) {
        this.north = nwse[0];
        this.west  = nwse[1];
        this.south = nwse[2];
        this.east  = nwse[3];
    }    


    /**
     * _more_
     *
     * @return _more_
     */
    public SelectionRectangle[] splitOnDateLine() {
        if ( !allDefined() || !crossesDateLine()) {
            return new SelectionRectangle[] {
                new SelectionRectangle(getNorth(), getWest(), getSouth(),
                                       getEast()), };
        }

        return new SelectionRectangle[] {
            new SelectionRectangle(getNorth(), getWest(), getSouth(), 180),
            new SelectionRectangle(getNorth(), -180, getSouth(), getEast()) };
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
     * @return _more_
     */
    public boolean crossesDateLine() {
        boolean haveLongitudeRange = !Double.isNaN(getWest())
                                     && !Double.isNaN(getEast());
        if (haveLongitudeRange && (getWest() > getEast())) {
            return true;
        }

        return false;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean allDefined() {
        return hasNorth() && hasWest() && hasSouth() && hasEast();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean anyDefined() {
        return hasNorth() || hasWest() || hasSouth() || hasEast();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public double[] getValues() {
        return new double[] { north, west, south, east };
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String[] getStringArray() {
        double[] values = getValues();
        String[] nwse   = { "", "", "", "" };
        for (int i = 0; i < nwse.length; i++) {
            if ( !Double.isNaN(values[i])) {
                nwse[i] = "" + values[i];
            }
        }

        return nwse;
    }

    /**
     * _more_
     */
    public void normalizeLongitude() {
        if ( !Double.isNaN(west)) {
            west = Misc.normalizeLongitude(west);
        }
        if ( !Double.isNaN(east)) {
            east = Misc.normalizeLongitude(east);
        }

    }

    /**
     * _more_
     *
     * @param idx _more_
     *
     * @return _more_
     */
    public double get(int idx) {
        if (idx == 0) {
            return north;
        }
        if (idx == 1) {
            return west;
        }
        if (idx == 2) {
            return south;
        }
        if (idx == 3) {
            return east;
        }

        throw new IllegalArgumentException("Bad index:" + idx);
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
     * _more_
     *
     * @return _more_
     */
    public boolean hasNorth() {
        return !Double.isNaN(north);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean hasSouth() {
        return !Double.isNaN(south);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean hasWest() {
        return !Double.isNaN(west);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean hasEast() {
        return !Double.isNaN(east);
    }




    /**
     * _more_
     *
     * @param dflt _more_
     *
     * @return _more_
     */
    public double getNorth(double dflt) {
        return hasNorth()
               ? north
               : dflt;
    }

    /**
     * _more_
     *
     * @param dflt _more_
     *
     * @return _more_
     */
    public double getWest(double dflt) {
        return hasWest()
               ? west
               : dflt;
    }

    /**
     * _more_
     *
     * @param dflt _more_
     *
     * @return _more_
     */
    public double getSouth(double dflt) {
        return hasSouth()
               ? south
               : dflt;
    }

    /**
     * _more_
     *
     * @param dflt _more_
     *
     * @return _more_
     */
    public double getEast(double dflt) {
        return hasEast()
               ? east
               : dflt;
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
