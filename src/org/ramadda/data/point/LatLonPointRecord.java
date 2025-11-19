/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.data.point;

import org.ramadda.data.record.*;

import java.io.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/** This is generated code from generate.tcl. Do not edit it! */
public class LatLonPointRecord extends org.ramadda.data.record.GeoRecord {

    public static final int ATTR_FIRST =
        org.ramadda.data.record.GeoRecord.ATTR_LAST;

    public static final List<RecordField> FIELDS =
        new ArrayList<RecordField>();

    public static final int ATTR_LATITUDE = ATTR_FIRST + 1;
    public static final RecordField RECORDATTR_LATITUDE;
    public static final int ATTR_LONGITUDE = ATTR_FIRST + 2;
    public static final RecordField RECORDATTR_LONGITUDE;
    public static final int ATTR_ALTITUDE = ATTR_FIRST + 3;
    public static final RecordField RECORDATTR_ALTITUDE;
    public static final int ATTR_VALUES = ATTR_FIRST + 4;
    public static final RecordField RECORDATTR_VALUES;
    public static final int ATTR_LAST = ATTR_FIRST + 5;

    static {
        FIELDS.add(RECORDATTR_LATITUDE = new RecordField("latitude",
                "latitude", "", ATTR_LATITUDE, "", "double", "double", 0,
                SEARCHABLE_NO, CHARTABLE_NO));
        RECORDATTR_LATITUDE.setValueGetter(new ValueGetter() {
            public double getValue(BaseRecord record, RecordField field,
                                   VisitInfo visitInfo) {
                return (double) ((LatLonPointRecord) record).latitude;
            }
            public String getStringValue(BaseRecord record,
                                         RecordField field,
                                         VisitInfo visitInfo) {
                return "" + ((LatLonPointRecord) record).latitude;
            }
        });
        FIELDS.add(RECORDATTR_LONGITUDE = new RecordField("longitude",
                "longitude", "", ATTR_LONGITUDE, "", "double", "double", 0,
                SEARCHABLE_NO, CHARTABLE_NO));
        RECORDATTR_LONGITUDE.setValueGetter(new ValueGetter() {
            public double getValue(BaseRecord record, RecordField field,
                                   VisitInfo visitInfo) {
                return (double) ((LatLonPointRecord) record).longitude;
            }
            public String getStringValue(BaseRecord record,
                                         RecordField field,
                                         VisitInfo visitInfo) {
                return "" + ((LatLonPointRecord) record).longitude;
            }
        });
        FIELDS.add(RECORDATTR_ALTITUDE = new RecordField("altitude",
                "altitude", "", ATTR_ALTITUDE, "", "double", "double", 0,
                SEARCHABLE_NO, CHARTABLE_NO));
        RECORDATTR_ALTITUDE.setValueGetter(new ValueGetter() {
            public double getValue(BaseRecord record, RecordField field,
                                   VisitInfo visitInfo) {
                return (double) ((LatLonPointRecord) record).altitude;
            }
            public String getStringValue(BaseRecord record,
                                         RecordField field,
                                         VisitInfo visitInfo) {
                return "" + ((LatLonPointRecord) record).altitude;
            }
        });
        FIELDS.add(RECORDATTR_VALUES = new RecordField("values", "values",
                "", ATTR_VALUES, "", "double[getNumberOfValues()]", "double",
                0, SEARCHABLE_NO, CHARTABLE_NO));

    }


    double latitude;
    double longitude;
    double altitude;
    double[] values = null;

    public LatLonPointRecord(LatLonPointRecord that) {
        super(that);
        this.latitude       = that.latitude;
        this.longitude      = that.longitude;
        this.altitude       = that.altitude;
        this.values         = that.values;

        this.numberOfValues = that.numberOfValues;
    }

    public LatLonPointRecord(RecordFile file) {
        super(file);
    }

    public LatLonPointRecord(RecordFile file, boolean bigEndian) {
        super(file, bigEndian);
    }

    public int getLastAttribute() {
        return ATTR_LAST;
    }

    public boolean equals(Object object) {
        if ( !super.equals(object)) {
            System.err.println("bad super");

            return false;
        }
        if ( !(object instanceof LatLonPointRecord)) {
            return false;
        }
        LatLonPointRecord that = (LatLonPointRecord) object;
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
        if ( !java.util.Arrays.equals(this.values, that.values)) {
            System.err.println("bad values");

            return false;
        }

        return true;
    }

    private int numberOfValues;

    //    public LatLonPointRecord(int numberOfValues) {
    //      super(true);
    //        this.numberOfValues = numberOfValues;
    //        values = new double[getNumberOfValues()];
    //    }

    //    public LatLonPointRecord(boolean bigEndian, int numberOfValues) {
    //        super(bigEndian);
    //        this.numberOfValues = numberOfValues;
    //        values = new double[getNumberOfValues()];
    //    }

    public int getNumberOfValues() {
        return numberOfValues;
    }

    public void setNumberOfValues(int v) {
        numberOfValues = v;
    }

    protected void addFields(List<RecordField> fields) {
        super.addFields(fields);
        fields.addAll(FIELDS);
    }

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

        return super.getValue(attrId);

    }

    public int getRecordSize() {
        return super.getRecordSize() + 24 + 0;
    }

    public ReadStatus read(RecordIO recordIO) throws Exception {
        DataInputStream dis    = recordIO.getDataInputStream();
        ReadStatus      status = super.read(recordIO);
        if (status != ReadStatus.OK) {
            return status;
        }
        latitude  = readDouble(dis);
        longitude = readDouble(dis);
        altitude  = readDouble(dis);
        if ((values == null) || (values.length != getNumberOfValues())) {
            values = new double[getNumberOfValues()];
        }
        readDoubles(dis, values);

        return ReadStatus.OK;
    }

    public void write(RecordIO recordIO) throws IOException {
        DataOutputStream dos = recordIO.getDataOutputStream();
        super.write(recordIO);
        writeDouble(dos, latitude);
        writeDouble(dos, longitude);
        writeDouble(dos, altitude);
        write(dos, values);

    }

    public int doPrintCsv(VisitInfo visitInfo, PrintWriter pw) {
        boolean includeVector = visitInfo.getProperty(PROP_INCLUDEVECTOR,
                                    false);
        int superCnt = super.doPrintCsv(visitInfo, pw);
        int myCnt    = 0;
        if (superCnt > 0) {
            pw.print(',');
        }
        pw.print(latitude);
        myCnt++;
        pw.print(',');
        pw.print(longitude);
        myCnt++;
        pw.print(',');
        pw.print(altitude);
        myCnt++;
        if (includeVector) {
            for (int i = 0; i < this.values.length; i++) {
                pw.print((i == 0)
                         ? '|'
                         : ',');
                pw.print(this.values[i]);
            }
            myCnt++;
        }

        return myCnt + superCnt;

    }

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
        if (includeVector) {
            pw.print(',');
            RECORDATTR_VALUES.printCsvHeader(visitInfo, pw);
            myCnt++;
        }

        return myCnt + superCnt;

    }

    public void print(Appendable buff) throws Exception {
        super.print(buff);
        buff.append(" latitude: " + latitude + " \n");
        buff.append(" longitude: " + longitude + " \n");
        buff.append(" altitude: " + altitude + " \n");

    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double newValue) {
        latitude = newValue;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double newValue) {
        longitude = newValue;
    }

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double newValue) {
        altitude = newValue;
    }

    public double[] getValues() {
        return values;
    }

    public void setValues(double[] newValue) {
        copy(values, newValue);
    }

}
