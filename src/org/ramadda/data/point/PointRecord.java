/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.data.point;

import org.ramadda.data.record.*;
import ucar.unidata.geoloc.*;

import java.io.*;

public abstract class PointRecord extends GeoRecord {
    protected double latitude = Double.NaN;
    protected double longitude = Double.NaN;
    protected double altitude = Double.NaN;
    protected boolean dataHasLocation = false;
    protected double x = Double.NaN;
    protected double y = Double.NaN;
    protected double z = Double.NaN;
    double locWorkBuffer[] = new double[] { 0, 0, 0 };
    ProjectionPointImpl fromPoint = new ProjectionPointImpl();
    LatLonPointImpl toPoint = new LatLonPointImpl();

    public PointRecord() {}

    public PointRecord(RecordFile recordFile) {
        super(recordFile);
    }

    public PointRecord(PointRecord that) {
        super(that);
    }

    public PointRecord(RecordFile recordFile, boolean bigEndian) {
        super(recordFile, bigEndian);
    }

    public ProjectionPointImpl getFromPoint() {
        return fromPoint;
    }

    public LatLonPointImpl getToPoint() {
        return toPoint;
    }

    static int cnt = 0;

    public void setLocation(double x, double y, double z) {
        setLocation(x, y, z, false);
    }

    public void setLocation(double x, double y, double z,
                            boolean dataHasLocation) {
        locWorkBuffer = getPointFile().getLatLonAlt(this, y, x, z,
                locWorkBuffer);
        //      if(cnt++<10) {
        //            System.err.println("xyz:" + x + " " + y +" " + z +"   alt:" + locWorkBuffer[PointFile.IDX_ALT]);
        //      }
        this.setLatitude(locWorkBuffer[PointFile.IDX_LAT]);
        this.setLongitude(locWorkBuffer[PointFile.IDX_LON]);
        this.setAltitude(locWorkBuffer[PointFile.IDX_ALT]);
        this.setDataHasLocation(dataHasLocation);
    }

    public PointFile getPointFile() {
        return (PointFile) getRecordFile();
    }

    public float[] getAltitudes() {
        return null;
    }

    public void convertXYZToLatLonAlt() {
        x = getLongitude();
        y = getLatitude();
        z = getAltitude();
    }

    public void recontextualize(PointFile pointFile) {}

    public void printLatLonAltCsv(VisitInfo visitInfo, Appendable pw) {
        try {
            pw.append(getLatitude() + "");
            pw.append(',');
            pw.append(getLongitude() + "");
            pw.append(',');
            pw.append(getAltitude() + "");
            pw.append("\n");
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    public void printLonLatAltCsv(VisitInfo visitInfo, Appendable pw) {
        try {
            pw.append(getLongitude() + "");
            pw.append(',');
            pw.append(getLatitude() + "");
            pw.append(',');
            pw.append(getAltitude() + "");
            pw.append("\n");
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    public void clearPosition() {
        latitude        = Double.NaN;
        longitude       = Double.NaN;
        altitude        = Double.NaN;
        dataHasLocation = false;
    }

    public long convertGpsTime(double gpsTime) {
        return 0;
    }

    @Override
    public ReadStatus read(RecordIO recordIO) throws Exception {
        ReadStatus status = super.read(recordIO);
        if (status != ReadStatus.OK) {
            return status;
        }
        clearPosition();

        return ReadStatus.OK;
    }

    public void setLatitude(double value) {
        this.latitude = value;
    }

    public double getLatitude() {
        return this.latitude;
    }

    public void setLongitude(double value) {
        this.longitude = value;
    }

    public double getLongitude() {
        return this.longitude;
    }

    public void setAltitude(double value) {
        this.altitude = value;
    }

    public double getAltitude() {
        return this.altitude;
    }

    public void setDataHasLocation(boolean value) {
        dataHasLocation = value;
    }

    public boolean getDataHasLocation() {
        return dataHasLocation;
    }

    public String[] getWaveformNames() {
        return null;
    }

    public Waveform getWaveform() {
        return getWaveform(null);
    }

    public Waveform getWaveform(String name) {
        return null;
    }

}
