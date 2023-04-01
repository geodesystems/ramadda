/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.data.record;


import org.ramadda.util.Utils;
import org.ramadda.data.record.*;

import java.io.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;




/** This is generated code from generate.tcl. Do not edit it! */
public class PointDataRecord extends BaseRecord {

    /** _more_ */
    public static final int ATTR_FIRST = BaseRecord.ATTR_LAST;

    /** _more_ */
    public static final List<RecordField> FIELDS =
        new ArrayList<RecordField>();

    /** _more_ */
    public static final int ATTR_LATITUDE = ATTR_FIRST + 1;

    /** _more_ */
    public static final RecordField RECORDATTR_LATITUDE;

    /** _more_ */
    public static final int ATTR_LONGITUDE = ATTR_FIRST + 2;

    /** _more_ */
    public static final RecordField RECORDATTR_LONGITUDE;

    /** _more_ */
    public static final int ATTR_ALTITUDE = ATTR_FIRST + 3;

    /** _more_ */
    public static final RecordField RECORDATTR_ALTITUDE;

    /** _more_ */
    public static final int ATTR_TIME = ATTR_FIRST + 4;

    /** _more_ */
    public static final RecordField RECORDATTR_TIME;

    /** _more_ */
    public static final int ATTR_DVALS = ATTR_FIRST + 5;

    /** _more_ */
    public static final RecordField RECORDATTR_DVALS;

    /** _more_ */
    public static final int ATTR_SVALS = ATTR_FIRST + 6;

    /** _more_ */
    public static final RecordField RECORDATTR_SVALS;

    /** _more_ */
    public static final int ATTR_LAST = ATTR_FIRST + 7;


    static {
        FIELDS.add(RECORDATTR_LATITUDE = new RecordField("Latitude",
                "Latitude", "", ATTR_LATITUDE, "", "double", "double", 0,
                SEARCHABLE_NO, CHARTABLE_NO));
        RECORDATTR_LATITUDE.setValueGetter(new ValueGetter() {
            public double getValue(BaseRecord record, RecordField field,
                                   VisitInfo visitInfo) {
                return (double) ((PointDataRecord) record).latitude;
            }
            public String getStringValue(BaseRecord record,
                                         RecordField field,
                                         VisitInfo visitInfo) {
                return "" + ((PointDataRecord) record).latitude;
            }
        });
        FIELDS.add(RECORDATTR_LONGITUDE = new RecordField("Longitude",
                "Longitude", "", ATTR_LONGITUDE, "", "double", "double", 0,
                SEARCHABLE_NO, CHARTABLE_NO));
        RECORDATTR_LONGITUDE.setValueGetter(new ValueGetter() {
            public double getValue(BaseRecord record, RecordField field,
                                   VisitInfo visitInfo) {
                return (double) ((PointDataRecord) record).longitude;
            }
            public String getStringValue(BaseRecord record,
                                         RecordField field,
                                         VisitInfo visitInfo) {
                return "" + ((PointDataRecord) record).longitude;
            }
        });
        FIELDS.add(RECORDATTR_ALTITUDE = new RecordField("Altitude",
                "Altitude", "", ATTR_ALTITUDE, "", "double", "double", 0,
                SEARCHABLE_NO, CHARTABLE_NO));
        RECORDATTR_ALTITUDE.setValueGetter(new ValueGetter() {
            public double getValue(BaseRecord record, RecordField field,
                                   VisitInfo visitInfo) {
                return (double) ((PointDataRecord) record).altitude;
            }
            public String getStringValue(BaseRecord record,
                                         RecordField field,
                                         VisitInfo visitInfo) {
                return "" + ((PointDataRecord) record).altitude;
            }
        });
        FIELDS.add(RECORDATTR_TIME = new RecordField("Time", "Time", "",
                ATTR_TIME, "", "long", "long", 0, SEARCHABLE_NO,
                CHARTABLE_NO));
        RECORDATTR_TIME.setValueGetter(new ValueGetter() {
            public double getValue(BaseRecord record, RecordField field,
                                   VisitInfo visitInfo) {
                return (double) ((PointDataRecord) record).Time;
            }
            public String getStringValue(BaseRecord record,
                                         RecordField field,
                                         VisitInfo visitInfo) {
                return "" + ((PointDataRecord) record).Time;
            }
        });
        FIELDS.add(RECORDATTR_DVALS = new RecordField("Dvals", "Dvals", "",
                ATTR_DVALS, "", "double[getDvalsSize()]", "double", 0,
                SEARCHABLE_NO, CHARTABLE_NO));
        FIELDS.add(RECORDATTR_SVALS = new RecordField("Svals", "Svals", "",
                ATTR_SVALS, "", "String[getSvalsSize()]", "String", 0,
                SEARCHABLE_NO, CHARTABLE_NO));

    }

    /** _more_ */
    double latitude;

    /** _more_ */
    double longitude;

    /** _more_ */
    double altitude;

    /** _more_ */
    long Time;

    /** _more_ */
    double[] doubleValues = null;

    /** _more_ */
    String[] stringValues = null;

    /** _more_ */
    public int dvalsSize;

    /** _more_ */
    public int svalsSize;



    /**
     * _more_
     *
     * @param that _more_
     */
    public PointDataRecord(PointDataRecord that) {
        super(that);
        this.latitude  = that.latitude;
        this.longitude = that.longitude;
        this.altitude  = that.altitude;
        this.Time      = that.Time;
        this.doubleValues     = that.doubleValues;
        this.stringValues     = that.stringValues;
    }



    /**
     * _more_
     *
     * @param file _more_
     */
    public PointDataRecord(RecordFile file) {
        super(file);
    }



    /**
     * _more_
     *
     * @param file _more_
     * @param bigEndian _more_
     */
    public PointDataRecord(RecordFile file, boolean bigEndian) {
        super(file, bigEndian);
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public int getLastAttribute() {
        return ATTR_LAST;
    }



    /**
     * _more_
     *
     * @param object _more_
     *
     * @return _more_
     */
    public boolean equals(Object object) {
        if ( !super.equals(object)) {
            System.err.println("bad super");

            return false;
        }
        if ( !(object instanceof PointDataRecord)) {
            return false;
        }
        PointDataRecord that = (PointDataRecord) object;
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
        if (this.Time != that.Time) {
            System.err.println("bad Time");

            return false;
        }
        if ( !java.util.Arrays.equals(this.doubleValues, that.doubleValues)) {
            System.err.println("bad doubleValues");

            return false;
        }
        if ( !java.util.Arrays.equals(this.stringValues, that.stringValues)) {
            System.err.println("bad stringValues");

            return false;
        }

        return true;
    }






    /**
     * _more_
     *
     * @return _more_
     */
    public int getDoubleValuesSize() {
        return dvalsSize;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getStringValuesSize() {
        return svalsSize;
    }

    /**
     * _more_
     *
     * @param fields _more_
     */
    protected void addFields(List<RecordField> fields) {
        super.addFields(fields);
        fields.addAll(FIELDS);
    }



    /**
     * _more_
     *
     * @param attrId _more_
     *
     * @return _more_
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
        if (attrId == ATTR_TIME) {
            return Time;
        }

        return super.getValue(attrId);

    }


    /**
     * _more_
     *
     * @return _more_
     */
    public int getRecordSize() {
        return super.getRecordSize() + 32 + 0 + 0;
    }

    private int readCnt = 0;

    /**
     * _more_
     *
     * @param recordIO _more_
     *
     * @return _more_
     *
     * @throws IOException _more_
     */
    public ReadStatus read(RecordIO recordIO) throws IOException {
	readCnt++;
        DataInputStream dis = recordIO.getDataInputStream();
        latitude  = readDouble(dis);
        longitude = readDouble(dis);
        altitude  = readDouble(dis);
        Time      = readLong(dis);
        if ((doubleValues == null) || (doubleValues.length != getDoubleValuesSize())) {
            doubleValues = new double[getDoubleValuesSize()];
        }
        if ((stringValues == null) || (stringValues.length != getStringValuesSize())) {
            stringValues = new String[getStringValuesSize()];
        }
	readDoubles(dis, doubleValues);
        readStrings(dis, stringValues);
        return ReadStatus.OK;
    }



    /**
     * _more_
     *
     * @param recordIO _more_
     *
     * @throws IOException _more_
     */
    public void write(RecordIO recordIO) throws IOException {
        DataOutputStream dos = recordIO.getDataOutputStream();
        writeDouble(dos, latitude);
        writeDouble(dos, longitude);
        writeDouble(dos, altitude);
        writeLong(dos, Time);
        write(dos, doubleValues);
        write(dos, stringValues);
    }



    /**
     * _more_
     *
     * @param visitInfo _more_
     * @param pw _more_
     *
     * @return _more_
     */
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
        pw.print(',');
        pw.print(Time);
        myCnt++;
        if (includeVector) {
            for (int i = 0; i < this.doubleValues.length; i++) {
                pw.print((i == 0)
                         ? '|'
                         : ',');
                pw.print(this.doubleValues[i]);
            }
            myCnt++;
        }
        if (includeVector) {
            for (int i = 0; i < this.stringValues.length; i++) {
                pw.print((i == 0)
                         ? '|'
                         : ',');
                pw.print(this.stringValues[i]);
            }
            myCnt++;
        }

        return myCnt + superCnt;

    }



    /**
     * _more_
     *
     * @param visitInfo _more_
     * @param pw _more_
     *
     * @return _more_
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
        RECORDATTR_TIME.printCsvHeader(visitInfo, pw);
        myCnt++;
        if (includeVector) {
            pw.print(',');
            RECORDATTR_DVALS.printCsvHeader(visitInfo, pw);
            myCnt++;
        }
        if (includeVector) {
            pw.print(',');
            RECORDATTR_SVALS.printCsvHeader(visitInfo, pw);
            myCnt++;
        }

        return myCnt + superCnt;

    }



    /**
     * _more_
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
        buff.append(" Time: " + Time + " \n");

    }



    /**
     * _more_
     *
     * @return _more_
     */
    public double getLatitude() {
        return latitude;
    }


    /**
     * _more_
     *
     * @param newValue _more_
     */
    public void setLatitude(double newValue) {
        latitude = newValue;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public double getLongitude() {
        return longitude;
    }


    /**
     * _more_
     *
     * @param newValue _more_
     */
    public void setLongitude(double newValue) {
        longitude = newValue;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public double getAltitude() {
        return altitude;
    }


    /**
     * _more_
     *
     * @param newValue _more_
     */
    public void setAltitude(double newValue) {
        altitude = newValue;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public long getTime() {
        return Time;
    }


    /**
     * _more_
     *
     * @param newValue _more_
     */
    public void setTime(long newValue) {
        Time = newValue;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public double[] getDoubleValues() {
        return doubleValues;
    }


    /**
     * _more_
     *
     * @param newValues _more_
     */
    public void setDoubleValues(double[] newValues) {
        if (doubleValues == null) {
            doubleValues = new double[newValues.length];
        } 
	//	Utils.print("PointDataRecord.setValues: from:",newValues); 
	copy(doubleValues, newValues);
	//	Utils.print("PointDataRecord.setValues: to:",doubleValues); 
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String[] getStringValues() {
        return stringValues;
    }


    /**
     * _more_
     *
     * @param newValue _more_
     */
    public void setStringValues(String[] newValue) {
        if (stringValues == null) {
            stringValues = new String[newValue.length];
        } 
	copy(stringValues, newValue);
    }



}
