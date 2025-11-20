/**
Copyright (c) 2008-2026 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.data.point;

import org.ramadda.data.record.*;
import java.io.*;

public class Waveform {
    private float[] waveform;
    private float threshold = 0.0f;
    private float[] range;
    private double altitude0 = Float.NaN;
    private double altitudeN;
    private double latitude0;
    private double longitude0;
    private double latitudeN;
    private double longitudeN;

    public Waveform(float[] waveform, float[] range, float threshold,
                    double altitude0, double altitudeN, double latitude0,
                    double longitude0, double latitudeN, double longitudeN) {
        this.waveform   = waveform;
        this.range      = range;
        this.threshold  = threshold;
        this.altitude0  = altitude0;
        this.altitudeN  = altitudeN;
        this.latitude0  = latitude0;
        this.longitude0 = longitude0;
        this.latitudeN  = latitudeN;
        this.longitudeN = longitudeN;
    }

    public Waveform(float[] waveform) {
        this.waveform = waveform;
        range         = new float[] { waveform[0], waveform[0] };
        for (int i = 0; i < waveform.length; i++) {
            range[0] = Math.min(range[0], waveform[i]);
            range[1] = Math.max(range[1], waveform[i]);
        }
    }

    public Waveform(float[] waveform, float[] range, float threshold) {
        this.waveform  = waveform;
        this.range     = range;
        this.threshold = threshold;
    }

    public boolean hasAltitude() {
        return altitude0 == altitude0;
    }

    public void setWaveform(float[] value) {
        waveform = value;
    }

    public float[] getWaveform() {
        return waveform;
    }

    public float[] getRange() {
        return range;
    }

    public float getThreshold() {
        return threshold;
    }

    public void setAltitude0(double value) {
        altitude0 = value;
    }

    public double getAltitude0() {
        return altitude0;
    }

    public void setAltitudeN(double value) {
        altitudeN = value;
    }

    public double getAltitudeN() {
        return altitudeN;
    }

    public void setLatitude0(double value) {
        latitude0 = value;
    }

    public double getLatitude0() {
        return latitude0;
    }

    public void setLongitude0(double value) {
        longitude0 = value;
    }

    public double getLongitude0() {
        return longitude0;
    }

    public void setLatitudeN(double value) {
        latitudeN = value;
    }

    public double getLatitudeN() {
        return latitudeN;
    }

    public void setLongitudeN(double value) {
        longitudeN = value;
    }

    public double getLongitudeN() {
        return longitudeN;
    }

}
