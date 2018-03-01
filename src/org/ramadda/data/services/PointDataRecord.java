/*
* Copyright (c) 2008-2018 Geode Systems LLC
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
*     http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.ramadda.data.services;


import org.ramadda.data.record.*;
import org.ramadda.util.AtomUtil;

import java.io.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;




/** This is generated code from generate.tcl. Do not edit it! */
public class PointDataRecord extends Record {

    /** _more_ */
    public static final int ATTR_FIRST = Record.ATTR_LAST;

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
            public double getValue(Record record, RecordField field,
                                   VisitInfo visitInfo) {
                return (double) ((PointDataRecord) record).Latitude;
            }
            public String getStringValue(Record record, RecordField field,
                                         VisitInfo visitInfo) {
                return "" + ((PointDataRecord) record).Latitude;
            }
        });
        FIELDS.add(RECORDATTR_LONGITUDE = new RecordField("Longitude",
                "Longitude", "", ATTR_LONGITUDE, "", "double", "double", 0,
                SEARCHABLE_NO, CHARTABLE_NO));
        RECORDATTR_LONGITUDE.setValueGetter(new ValueGetter() {
            public double getValue(Record record, RecordField field,
                                   VisitInfo visitInfo) {
                return (double) ((PointDataRecord) record).Longitude;
            }
            public String getStringValue(Record record, RecordField field,
                                         VisitInfo visitInfo) {
                return "" + ((PointDataRecord) record).Longitude;
            }
        });
        FIELDS.add(RECORDATTR_ALTITUDE = new RecordField("Altitude",
                "Altitude", "", ATTR_ALTITUDE, "", "double", "double", 0,
                SEARCHABLE_NO, CHARTABLE_NO));
        RECORDATTR_ALTITUDE.setValueGetter(new ValueGetter() {
            public double getValue(Record record, RecordField field,
                                   VisitInfo visitInfo) {
                return (double) ((PointDataRecord) record).Altitude;
            }
            public String getStringValue(Record record, RecordField field,
                                         VisitInfo visitInfo) {
                return "" + ((PointDataRecord) record).Altitude;
            }
        });
        FIELDS.add(RECORDATTR_TIME = new RecordField("Time", "Time", "",
                ATTR_TIME, "", "long", "long", 0, SEARCHABLE_NO,
                CHARTABLE_NO));
        RECORDATTR_TIME.setValueGetter(new ValueGetter() {
            public double getValue(Record record, RecordField field,
                                   VisitInfo visitInfo) {
                return (double) ((PointDataRecord) record).Time;
            }
            public String getStringValue(Record record, RecordField field,
                                         VisitInfo visitInfo) {
                //                return AtomUtil.format(((PointDataRecord) record).Time);
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
    double Latitude;

    /** _more_ */
    double Longitude;

    /** _more_ */
    double Altitude;

    /** _more_ */
    long Time;

    /** _more_ */
    double[] Dvals = null;

    /** _more_ */
    String[] Svals = null;


    /**
     * _more_
     *
     * @param that _more_
     */
    public PointDataRecord(PointDataRecord that) {
        super(that);
        this.Latitude  = that.Latitude;
        this.Longitude = that.Longitude;
        this.Altitude  = that.Altitude;
        this.Time      = that.Time;
        this.Dvals     = that.Dvals;
        this.Svals     = that.Svals;


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
        if (this.Latitude != that.Latitude) {
            System.err.println("bad Latitude");

            return false;
        }
        if (this.Longitude != that.Longitude) {
            System.err.println("bad Longitude");

            return false;
        }
        if (this.Altitude != that.Altitude) {
            System.err.println("bad Altitude");

            return false;
        }
        if (this.Time != that.Time) {
            System.err.println("bad Time");

            return false;
        }
        if ( !java.util.Arrays.equals(this.Dvals, that.Dvals)) {
            System.err.println("bad Dvals");

            return false;
        }
        if ( !java.util.Arrays.equals(this.Svals, that.Svals)) {
            System.err.println("bad Svals");

            return false;
        }

        return true;
    }




    /** _more_ */
    public int dvalsSize;

    /** _more_ */
    public int svalsSize;

    /**
     * _more_
     *
     * @return _more_
     */
    public int getDvalsSize() {
        return dvalsSize;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getSvalsSize() {
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
            return Latitude;
        }
        if (attrId == ATTR_LONGITUDE) {
            return Longitude;
        }
        if (attrId == ATTR_ALTITUDE) {
            return Altitude;
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
        DataInputStream dis = recordIO.getDataInputStream();
        Latitude  = readDouble(dis);
        Longitude = readDouble(dis);
        Altitude  = readDouble(dis);
        Time      = readLong(dis);
        if ((Dvals == null) || (Dvals.length != getDvalsSize())) {
            Dvals = new double[getDvalsSize()];
        }
        readDoubles(dis, Dvals);
        if ((Svals == null) || (Svals.length != getSvalsSize())) {
            Svals = new String[getSvalsSize()];
        }
        readStrings(dis, Svals);


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
        writeDouble(dos, Latitude);
        writeDouble(dos, Longitude);
        writeDouble(dos, Altitude);
        writeLong(dos, Time);
        write(dos, Dvals);
        write(dos, Svals);

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
        pw.print(Latitude);
        myCnt++;
        pw.print(',');
        pw.print(Longitude);
        myCnt++;
        pw.print(',');
        pw.print(Altitude);
        myCnt++;
        pw.print(',');
        pw.print(Time);
        myCnt++;
        if (includeVector) {
            for (int i = 0; i < this.Dvals.length; i++) {
                pw.print((i == 0)
                         ? '|'
                         : ',');
                pw.print(this.Dvals[i]);
            }
            myCnt++;
        }
        if (includeVector) {
            for (int i = 0; i < this.Svals.length; i++) {
                pw.print((i == 0)
                         ? '|'
                         : ',');
                pw.print(this.Svals[i]);
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
        buff.append(" Latitude: " + Latitude + " \n");
        buff.append(" Longitude: " + Longitude + " \n");
        buff.append(" Altitude: " + Altitude + " \n");
        buff.append(" Time: " + Time + " \n");

    }



    /**
     * _more_
     *
     * @return _more_
     */
    public double getLatitude() {
        return Latitude;
    }


    /**
     * _more_
     *
     * @param newValue _more_
     */
    public void setLatitude(double newValue) {
        Latitude = newValue;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public double getLongitude() {
        return Longitude;
    }


    /**
     * _more_
     *
     * @param newValue _more_
     */
    public void setLongitude(double newValue) {
        Longitude = newValue;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public double getAltitude() {
        return Altitude;
    }


    /**
     * _more_
     *
     * @param newValue _more_
     */
    public void setAltitude(double newValue) {
        Altitude = newValue;
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
    public double[] getDvals() {
        return Dvals;
    }


    /**
     * _more_
     *
     * @param newValue _more_
     */
    public void setDvals(double[] newValue) {
        if (Dvals == null) {
            Dvals = newValue;
        } else {
            copy(Dvals, newValue);
        }
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String[] getSvals() {
        return Svals;
    }


    /**
     * _more_
     *
     * @param newValue _more_
     */
    public void setSvals(String[] newValue) {
        if (Svals == null) {
            Svals = newValue;
        } else {
            copy(Svals, newValue);
        }
    }



}
