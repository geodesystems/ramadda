/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.data.point;

import org.ramadda.data.record.*;
import ucar.unidata.geoloc.*;

import java.io.*;

/**
 */
public abstract class PointRecord extends GeoRecord {

    /** _more_ */
    protected double latitude = Double.NaN;

    /** _more_ */
    protected double longitude = Double.NaN;

    /** _more_ */
    protected double altitude = Double.NaN;

    /** _more_ */
    protected boolean dataHasLocation = false;

    /** _more_ */
    protected double x = Double.NaN;

    /** _more_ */
    protected double y = Double.NaN;

    /** _more_ */
    protected double z = Double.NaN;


    /** _more_ */
    double locWorkBuffer[] = new double[] { 0, 0, 0 };

    /** _more_ */
    ProjectionPointImpl fromPoint = new ProjectionPointImpl();

    /** _more_ */
    LatLonPointImpl toPoint = new LatLonPointImpl();

    /**
     * _more_
     */
    public PointRecord() {}


    /**
     * _more_
     *
     * @param recordFile _more_
     */
    public PointRecord(RecordFile recordFile) {
        super(recordFile);
    }

    /**
     * _more_
     *
     * @param that _more_
     */
    public PointRecord(PointRecord that) {
        super(that);
    }

    /**
     * _more_
     *
     *
     * @param recordFile _more_
     * @param bigEndian _more_
     */
    public PointRecord(RecordFile recordFile, boolean bigEndian) {
        super(recordFile, bigEndian);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public ProjectionPointImpl getFromPoint() {
        return fromPoint;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public LatLonPointImpl getToPoint() {
        return toPoint;
    }


    /** _more_ */
    static int cnt = 0;

    /**
     * _more_
     *
     * @param x _more_
     * @param y _more_
     * @param z _more_
     */

    public void setLocation(double x, double y, double z) {
        setLocation(x, y, z, false);
    }

    /**
     * _more_
     *
     * @param x _more_
     * @param y _more_
     * @param z _more_
     * @param dataHasLocation _more_
     */
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


    /**
     * _more_
     *
     * @return _more_
     */
    public PointFile getPointFile() {
        return (PointFile) getRecordFile();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public float[] getAltitudes() {
        return null;
    }




    /**
     * _more_
     */
    public void convertXYZToLatLonAlt() {
        x = getLongitude();
        y = getLatitude();
        z = getAltitude();
    }

    /**
     * _more_
     *
     * @param pointFile _more_
     */
    public void recontextualize(PointFile pointFile) {}

    /**
     * _more_
     *
     *
     * @param visitInfo _more_
     * @param pw _more_
     */
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


    /**
     * _more_
     *
     * @param visitInfo _more_
     * @param pw _more_
     */
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

    /**
     * _more_
     */
    public void clearPosition() {
        latitude        = Double.NaN;
        longitude       = Double.NaN;
        altitude        = Double.NaN;
        dataHasLocation = false;
    }

    /**
     * _more_
     *
     * @param gpsTime _more_
     *
     * @return _more_
     */
    public long convertGpsTime(double gpsTime) {
        return 0;
    }


    /**
     * _more_
     *
     * @param recordIO _more_
     *
     * @return _more_
     *
     *
     * @throws Exception _more_
     */
    @Override
    public ReadStatus read(RecordIO recordIO) throws Exception {
        ReadStatus status = super.read(recordIO);
        if (status != ReadStatus.OK) {
            return status;
        }
        clearPosition();

        return ReadStatus.OK;
    }

    /**
     *  Set the Latitude property.
     *
     *  @param value The new value for Latitude
     */
    public void setLatitude(double value) {
        this.latitude = value;
    }

    /**
     *  Get the Latitude property.
     *
     *  @return The Latitude
     */
    public double getLatitude() {
        return this.latitude;
    }

    /**
     *  Set the Longitude property.
     *
     *  @param value The new value for Longitude
     */
    public void setLongitude(double value) {
        this.longitude = value;
    }

    /**
     *  Get the Longitude property.
     *
     *  @return The Longitude
     */
    public double getLongitude() {
        return this.longitude;
    }

    /**
     *  Set the Altitude property.
     *
     *  @param value The new value for Altitude
     */
    public void setAltitude(double value) {
        this.altitude = value;
    }

    /**
     *  Get the Altitude property.
     *
     *  @return The Altitude
     */
    public double getAltitude() {
        return this.altitude;
    }


    /**
     * Set the DataHasLocation property.
     *
     * @param value The new value for DataHasLocation
     */
    public void setDataHasLocation(boolean value) {
        dataHasLocation = value;
    }

    /**
     * Get the DataHasLocation property.
     *
     * @return The DataHasLocation
     */
    public boolean getDataHasLocation() {
        return dataHasLocation;
    }




    /**
     * _more_
     *
     * @return _more_
     */
    public String[] getWaveformNames() {
        return null;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public Waveform getWaveform() {
        return getWaveform(null);
    }

    /**
     * _more_
     *
     *
     * @param name _more_
     * @return _more_
     */
    public Waveform getWaveform(String name) {
        return null;
    }


}
