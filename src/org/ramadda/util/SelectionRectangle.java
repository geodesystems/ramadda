/**
Copyright (c) 2008-2025 Geode Systems LLC
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


public class SelectionRectangle {

    private double north = Double.NaN;

    private double west = Double.NaN;

    private double south = Double.NaN;

    private double east = Double.NaN;

    public SelectionRectangle() {}

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

    public String toString() {
        return "north:" + north + " west:" + west + " south:" + south
               + " east:" + east;
    }

    public boolean crossesDateLine() {
        boolean haveLongitudeRange = !Double.isNaN(getWest())
                                     && !Double.isNaN(getEast());
        if (haveLongitudeRange && (getWest() > getEast())) {
            return true;
        }

        return false;
    }

    public boolean allDefined() {
        return hasNorth() && hasWest() && hasSouth() && hasEast();
    }

    public boolean anyDefined() {
        return hasNorth() || hasWest() || hasSouth() || hasEast();
    }

    public double[] getValues() {
        return new double[] { north, west, south, east };
    }

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

    public void normalizeLongitude() {
        if ( !Double.isNaN(west)) {
            west = Misc.normalizeLongitude(west);
        }
        if ( !Double.isNaN(east)) {
            east = Misc.normalizeLongitude(east);
        }

    }

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

    
    public void setNorth(double value) {
        north = value;
    }

    public boolean hasNorth() {
        return !Double.isNaN(north);
    }

    public boolean hasSouth() {
        return !Double.isNaN(south);
    }

    public boolean hasWest() {
        return !Double.isNaN(west);
    }

    public boolean hasEast() {
        return !Double.isNaN(east);
    }

    public double getNorth(double dflt) {
        return hasNorth()
               ? north
               : dflt;
    }

    public double getWest(double dflt) {
        return hasWest()
               ? west
               : dflt;
    }

    public double getSouth(double dflt) {
        return hasSouth()
               ? south
               : dflt;
    }

    public double getEast(double dflt) {
        return hasEast()
               ? east
               : dflt;
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
