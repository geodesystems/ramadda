/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.data.point.binary;

import org.ramadda.util.IO;
import org.ramadda.data.record.*;

import java.io.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;





/** This is generated code from generate.tcl. Do not edit it! */
public class DoubleLatLonAltIntensityRecord extends org.ramadda.data.point
    .PointRecord {

    /**  */
    public static final int ATTR_FIRST =
        org.ramadda.data.point.PointRecord.ATTR_LAST;

    /**  */
    public static final List<RecordField> FIELDS =
        new ArrayList<RecordField>();

    /**  */
    public static final int ATTR_LATITUDE = ATTR_FIRST + 1;

    /**  */
    public static final RecordField RECORDATTR_LATITUDE;

    /**  */
    public static final int ATTR_LONGITUDE = ATTR_FIRST + 2;

    /**  */
    public static final RecordField RECORDATTR_LONGITUDE;

    /**  */
    public static final int ATTR_ALTITUDE = ATTR_FIRST + 3;

    /**  */
    public static final RecordField RECORDATTR_ALTITUDE;

    /**  */
    public static final int ATTR_INTENSITY = ATTR_FIRST + 4;

    /**  */
    public static final RecordField RECORDATTR_INTENSITY;

    /**  */
    public static final int ATTR_LAST = ATTR_FIRST + 5;


    static {
        FIELDS.add(RECORDATTR_LATITUDE = new RecordField("latitude",
                "latitude", "", ATTR_LATITUDE, "", "double", "double", 0,
                SEARCHABLE_NO, CHARTABLE_NO));
        RECORDATTR_LATITUDE.setValueGetter(new ValueGetter() {
            public double getValue(BaseRecord record, RecordField field,
                                   VisitInfo visitInfo) {
                return (double) ((DoubleLatLonAltIntensityRecord) record)
                    .latitude;
            }
            public String getStringValue(BaseRecord record,
                                         RecordField field,
                                         VisitInfo visitInfo) {
                return "" + ((DoubleLatLonAltIntensityRecord) record)
                    .latitude;
            }
        });
        FIELDS.add(RECORDATTR_LONGITUDE = new RecordField("longitude",
                "longitude", "", ATTR_LONGITUDE, "", "double", "double", 0,
                SEARCHABLE_NO, CHARTABLE_NO));
        RECORDATTR_LONGITUDE.setValueGetter(new ValueGetter() {
            public double getValue(BaseRecord record, RecordField field,
                                   VisitInfo visitInfo) {
                return (double) ((DoubleLatLonAltIntensityRecord) record)
                    .longitude;
            }
            public String getStringValue(BaseRecord record,
                                         RecordField field,
                                         VisitInfo visitInfo) {
                return "" + ((DoubleLatLonAltIntensityRecord) record)
                    .longitude;
            }
        });
        FIELDS.add(RECORDATTR_ALTITUDE = new RecordField("altitude",
                "altitude", "", ATTR_ALTITUDE, "", "double", "double", 0,
                SEARCHABLE_NO, CHARTABLE_NO));
        RECORDATTR_ALTITUDE.setValueGetter(new ValueGetter() {
            public double getValue(BaseRecord record, RecordField field,
                                   VisitInfo visitInfo) {
                return (double) ((DoubleLatLonAltIntensityRecord) record)
                    .altitude;
            }
            public String getStringValue(BaseRecord record,
                                         RecordField field,
                                         VisitInfo visitInfo) {
                return "" + ((DoubleLatLonAltIntensityRecord) record)
                    .altitude;
            }
        });
        FIELDS.add(RECORDATTR_INTENSITY = new RecordField("intensity",
                "intensity", "", ATTR_INTENSITY, "", "double", "double", 0,
                SEARCHABLE_NO, CHARTABLE_NO));
        RECORDATTR_INTENSITY.setValueGetter(new ValueGetter() {
            public double getValue(BaseRecord record, RecordField field,
                                   VisitInfo visitInfo) {
                return (double) ((DoubleLatLonAltIntensityRecord) record)
                    .intensity;
            }
            public String getStringValue(BaseRecord record,
                                         RecordField field,
                                         VisitInfo visitInfo) {
                return "" + ((DoubleLatLonAltIntensityRecord) record)
                    .intensity;
            }
        });

    }


    /**  */
    double latitude;

    /**  */
    double longitude;

    /**  */
    double altitude;

    /**  */
    double intensity;


    /**
     *
     *
     * @param that _more_
     */
    public DoubleLatLonAltIntensityRecord(
            DoubleLatLonAltIntensityRecord that) {
        super(that);
        this.latitude  = that.latitude;
        this.longitude = that.longitude;
        this.altitude  = that.altitude;
        this.intensity = that.intensity;


    }



    /**
     *
     *
     * @param file _more_
     */
    public DoubleLatLonAltIntensityRecord(RecordFile file) {
        super(file);
    }



    /**
     *
     *
     * @param file _more_
     * @param bigEndian _more_
     */
    public DoubleLatLonAltIntensityRecord(RecordFile file,
                                          boolean bigEndian) {
        super(file, bigEndian);
    }



    /**
     *  @return _more_
     */
    public int getLastAttribute() {
        return ATTR_LAST;
    }



    /**
     *
     * @param object _more_
     *  @return _more_
     */
    public boolean equals(Object object) {
        if ( !super.equals(object)) {
            System.err.println("bad super");

            return false;
        }
        if ( !(object instanceof DoubleLatLonAltIntensityRecord)) {
            return false;
        }
        DoubleLatLonAltIntensityRecord that =
            (DoubleLatLonAltIntensityRecord) object;
        if (this.latitude != that.latitude) {
            System.err.println("bad latitude");

            return false;
        }
        if (this.longitude != that.longitude) {
            System.err.println("bad longitude");

            return false;
        }
        if (this.altitude != that.altitude) {
            System.err.println("bad altitude");

            return false;
        }
        if (this.intensity != that.intensity) {
            System.err.println("bad intensity");

            return false;
        }

        return true;
    }




    /**
     *
     * @param fields _more_
     */
    protected void addFields(List<RecordField> fields) {
        super.addFields(fields);
        fields.addAll(FIELDS);
    }



    /**
     *
     * @param attrId _more_
     *  @return _more_
     */
    public double getValue(int attrId) {
        if (attrId == ATTR_LATITUDE) {
            return latitude;
        }
        if (attrId == ATTR_LONGITUDE) {
            return longitude;
        }
        if (attrId == ATTR_ALTITUDE) {
            return altitude;
        }
        if (attrId == ATTR_INTENSITY) {
            return intensity;
        }

        return super.getValue(attrId);

    }



    /**
     *  @return _more_
     */
    public int getRecordSize() {
        return super.getRecordSize() + 32;
    }



    /**
     *
     * @param recordIO _more_
     *  @return _more_
     *
     * @throws Exception _more_
     */
    public ReadStatus read(RecordIO recordIO) throws Exception {
        DataInputStream dis    = recordIO.getDataInputStream();
        ReadStatus      status = super.read(recordIO);
        if (status != ReadStatus.OK) {
            return status;
        }
        latitude  = readDouble(dis);
        longitude = readDouble(dis);
        altitude  = readDouble(dis);
        intensity = readDouble(dis);


        return ReadStatus.OK;
    }



    /**
     *
     * @param recordIO _more_
     *
     * @throws IOException _more_
     */
    public void write(RecordIO recordIO) throws IOException {
        DataOutputStream dos = recordIO.getDataOutputStream();
        super.write(recordIO);
        writeDouble(dos, latitude);
        writeDouble(dos, longitude);
        writeDouble(dos, altitude);
        writeDouble(dos, intensity);

    }



    /**
     *
     * @param visitInfo _more_
     * @param pw _more_
     *  @return _more_
     */
    public int doPrintCsv(VisitInfo visitInfo, PrintWriter pw) {
        boolean includeVector = visitInfo.getProperty(PROP_INCLUDEVECTOR,
                                    false);
        int superCnt = super.doPrintCsv(visitInfo, pw);
        int myCnt    = 0;
        if (superCnt > 0) {
            pw.print(',');
        }
        pw.print(getStringValue(RECORDATTR_LATITUDE, latitude));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_LONGITUDE, longitude));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_ALTITUDE, altitude));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_INTENSITY, intensity));
        myCnt++;

        return myCnt + superCnt;

    }



    /**
     *
     * @param visitInfo _more_
     * @param pw _more_
     *  @return _more_
     */
    public int doPrintCsvHeader(VisitInfo visitInfo, PrintWriter pw) {
        int superCnt = super.doPrintCsvHeader(visitInfo, pw);
        int myCnt    = 0;
        boolean includeVector = visitInfo.getProperty(PROP_INCLUDEVECTOR,
                                    false);
        if (superCnt > 0) {
            pw.print(',');
        }
        RECORDATTR_LATITUDE.printCsvHeader(visitInfo, pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_LONGITUDE.printCsvHeader(visitInfo, pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_ALTITUDE.printCsvHeader(visitInfo, pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_INTENSITY.printCsvHeader(visitInfo, pw);
        myCnt++;

        return myCnt + superCnt;

    }



    /**
     *
     * @param buff _more_
     *
     * @throws Exception _more_
     */
    public void print(Appendable buff) throws Exception {
        super.print(buff);
        buff.append(" latitude: " + latitude + " \n");
        buff.append(" longitude: " + longitude + " \n");
        buff.append(" altitude: " + altitude + " \n");
        buff.append(" intensity: " + intensity + " \n");

    }



    /**
     *  @return _more_
     */
    public double getLatitude() {
        return latitude;
    }


    /**
     *
     * @param newValue _more_
     */
    public void setLatitude(double newValue) {
        latitude = newValue;
    }


    /**
     *  @return _more_
     */
    public double getLongitude() {
        return longitude;
    }


    /**
     *
     * @param newValue _more_
     */
    public void setLongitude(double newValue) {
        longitude = newValue;
    }


    /**
     *  @return _more_
     */
    public double getAltitude() {
        return altitude;
    }


    /**
     *
     * @param newValue _more_
     */
    public void setAltitude(double newValue) {
        altitude = newValue;
    }


    /**
     *  @return _more_
     */
    public double getIntensity() {
        return intensity;
    }


    /**
     *
     * @param newValue _more_
     */
    public void setIntensity(double newValue) {
        intensity = newValue;
    }



}
