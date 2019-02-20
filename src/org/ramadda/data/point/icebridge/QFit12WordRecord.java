/*
* Copyright (c) 2008-2019 Geode Systems LLC
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

package org.ramadda.data.point.icebridge;


import org.ramadda.data.record.*;

import java.io.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;





/** This is generated code from generate.tcl. Do not edit it! */
public class QFit12WordRecord extends org.ramadda.data.point.icebridge
    .QfitRecord {

    /** _more_          */
    public static final int ATTR_FIRST =
        org.ramadda.data.point.icebridge.QfitRecord.ATTR_LAST;

    /** _more_          */
    public static final List<RecordField> FIELDS =
        new ArrayList<RecordField>();

    /** _more_          */
    public static final int ATTR_RELATIVETIME = ATTR_FIRST + 1;

    /** _more_          */
    public static final RecordField RECORDATTR_RELATIVETIME;

    /** _more_          */
    public static final int ATTR_LASERLATITUDE = ATTR_FIRST + 2;

    /** _more_          */
    public static final RecordField RECORDATTR_LASERLATITUDE;

    /** _more_          */
    public static final int ATTR_LASERLONGITUDE = ATTR_FIRST + 3;

    /** _more_          */
    public static final RecordField RECORDATTR_LASERLONGITUDE;

    /** _more_          */
    public static final int ATTR_ELEVATION = ATTR_FIRST + 4;

    /** _more_          */
    public static final RecordField RECORDATTR_ELEVATION;

    /** _more_          */
    public static final int ATTR_STARTSIGNALSTRENGTH = ATTR_FIRST + 5;

    /** _more_          */
    public static final RecordField RECORDATTR_STARTSIGNALSTRENGTH;

    /** _more_          */
    public static final int ATTR_REFLECTEDSIGNALSTRENGTH = ATTR_FIRST + 6;

    /** _more_          */
    public static final RecordField RECORDATTR_REFLECTEDSIGNALSTRENGTH;

    /** _more_          */
    public static final int ATTR_AZIMUTH = ATTR_FIRST + 7;

    /** _more_          */
    public static final RecordField RECORDATTR_AZIMUTH;

    /** _more_          */
    public static final int ATTR_PITCH = ATTR_FIRST + 8;

    /** _more_          */
    public static final RecordField RECORDATTR_PITCH;

    /** _more_          */
    public static final int ATTR_ROLL = ATTR_FIRST + 9;

    /** _more_          */
    public static final RecordField RECORDATTR_ROLL;

    /** _more_          */
    public static final int ATTR_GPSPDOP = ATTR_FIRST + 10;

    /** _more_          */
    public static final RecordField RECORDATTR_GPSPDOP;

    /** _more_          */
    public static final int ATTR_PULSEWIDTH = ATTR_FIRST + 11;

    /** _more_          */
    public static final RecordField RECORDATTR_PULSEWIDTH;

    /** _more_          */
    public static final int ATTR_GPSTIME = ATTR_FIRST + 12;

    /** _more_          */
    public static final RecordField RECORDATTR_GPSTIME;

    /** _more_          */
    public static final int ATTR_LAST = ATTR_FIRST + 13;


    static {
        FIELDS.add(RECORDATTR_RELATIVETIME = new RecordField("relativeTime",
                "relativeTime", "", ATTR_RELATIVETIME, "", "int", "int", 0,
                SEARCHABLE_NO, CHARTABLE_NO));
        RECORDATTR_RELATIVETIME.setValueGetter(new ValueGetter() {
            public double getValue(Record record, RecordField field,
                                   VisitInfo visitInfo) {
                return (double) ((QFit12WordRecord) record).relativeTime;
            }
            public String getStringValue(Record record, RecordField field,
                                         VisitInfo visitInfo) {
                return "" + ((QFit12WordRecord) record).relativeTime;
            }
        });
        FIELDS.add(RECORDATTR_LASERLATITUDE =
            new RecordField("laserLatitude", "laserLatitude", "",
                            ATTR_LASERLATITUDE, "", "int", "int", 0,
                            SEARCHABLE_NO, CHARTABLE_NO));
        RECORDATTR_LASERLATITUDE.setValueGetter(new ValueGetter() {
            public double getValue(Record record, RecordField field,
                                   VisitInfo visitInfo) {
                return (double) ((QFit12WordRecord) record).laserLatitude;
            }
            public String getStringValue(Record record, RecordField field,
                                         VisitInfo visitInfo) {
                return "" + ((QFit12WordRecord) record).laserLatitude;
            }
        });
        FIELDS.add(RECORDATTR_LASERLONGITUDE =
            new RecordField("laserLongitude", "laserLongitude", "",
                            ATTR_LASERLONGITUDE, "", "int", "int", 0,
                            SEARCHABLE_NO, CHARTABLE_NO));
        RECORDATTR_LASERLONGITUDE.setValueGetter(new ValueGetter() {
            public double getValue(Record record, RecordField field,
                                   VisitInfo visitInfo) {
                return (double) ((QFit12WordRecord) record).laserLongitude;
            }
            public String getStringValue(Record record, RecordField field,
                                         VisitInfo visitInfo) {
                return "" + ((QFit12WordRecord) record).laserLongitude;
            }
        });
        FIELDS.add(RECORDATTR_ELEVATION = new RecordField("elevation",
                "elevation", "", ATTR_ELEVATION, "mm", "int", "int", 0,
                SEARCHABLE_NO, CHARTABLE_NO));
        RECORDATTR_ELEVATION.setValueGetter(new ValueGetter() {
            public double getValue(Record record, RecordField field,
                                   VisitInfo visitInfo) {
                return (double) ((QFit12WordRecord) record).elevation;
            }
            public String getStringValue(Record record, RecordField field,
                                         VisitInfo visitInfo) {
                return "" + ((QFit12WordRecord) record).elevation;
            }
        });
        FIELDS.add(RECORDATTR_STARTSIGNALSTRENGTH =
            new RecordField("startSignalStrength", "startSignalStrength", "",
                            ATTR_STARTSIGNALSTRENGTH, "", "int", "int", 0,
                            SEARCHABLE_NO, CHARTABLE_NO));
        RECORDATTR_STARTSIGNALSTRENGTH.setValueGetter(new ValueGetter() {
            public double getValue(Record record, RecordField field,
                                   VisitInfo visitInfo) {
                return (double) ((QFit12WordRecord) record)
                    .startSignalStrength;
            }
            public String getStringValue(Record record, RecordField field,
                                         VisitInfo visitInfo) {
                return "" + ((QFit12WordRecord) record).startSignalStrength;
            }
        });
        FIELDS.add(RECORDATTR_REFLECTEDSIGNALSTRENGTH =
            new RecordField("reflectedSignalStrength",
                            "reflectedSignalStrength", "",
                            ATTR_REFLECTEDSIGNALSTRENGTH, "", "int", "int",
                            0, SEARCHABLE_NO, CHARTABLE_NO));
        RECORDATTR_REFLECTEDSIGNALSTRENGTH.setValueGetter(new ValueGetter() {
            public double getValue(Record record, RecordField field,
                                   VisitInfo visitInfo) {
                return (double) ((QFit12WordRecord) record)
                    .reflectedSignalStrength;
            }
            public String getStringValue(Record record, RecordField field,
                                         VisitInfo visitInfo) {
                return "" + ((QFit12WordRecord) record)
                    .reflectedSignalStrength;
            }
        });
        FIELDS.add(RECORDATTR_AZIMUTH = new RecordField("azimuth", "azimuth",
                "", ATTR_AZIMUTH, "millidegree", "int", "int", 0,
                SEARCHABLE_NO, CHARTABLE_NO));
        RECORDATTR_AZIMUTH.setValueGetter(new ValueGetter() {
            public double getValue(Record record, RecordField field,
                                   VisitInfo visitInfo) {
                return (double) ((QFit12WordRecord) record).azimuth;
            }
            public String getStringValue(Record record, RecordField field,
                                         VisitInfo visitInfo) {
                return "" + ((QFit12WordRecord) record).azimuth;
            }
        });
        FIELDS.add(RECORDATTR_PITCH = new RecordField("pitch", "pitch", "",
                ATTR_PITCH, "millidegree", "int", "int", 0, SEARCHABLE_NO,
                CHARTABLE_NO));
        RECORDATTR_PITCH.setValueGetter(new ValueGetter() {
            public double getValue(Record record, RecordField field,
                                   VisitInfo visitInfo) {
                return (double) ((QFit12WordRecord) record).pitch;
            }
            public String getStringValue(Record record, RecordField field,
                                         VisitInfo visitInfo) {
                return "" + ((QFit12WordRecord) record).pitch;
            }
        });
        FIELDS.add(RECORDATTR_ROLL = new RecordField("roll", "roll", "",
                ATTR_ROLL, "millidegree", "int", "int", 0, SEARCHABLE_NO,
                CHARTABLE_NO));
        RECORDATTR_ROLL.setValueGetter(new ValueGetter() {
            public double getValue(Record record, RecordField field,
                                   VisitInfo visitInfo) {
                return (double) ((QFit12WordRecord) record).roll;
            }
            public String getStringValue(Record record, RecordField field,
                                         VisitInfo visitInfo) {
                return "" + ((QFit12WordRecord) record).roll;
            }
        });
        FIELDS.add(RECORDATTR_GPSPDOP = new RecordField("gpsPdop", "gpsPdop",
                "", ATTR_GPSPDOP, "", "int", "int", 0, SEARCHABLE_NO,
                CHARTABLE_NO));
        RECORDATTR_GPSPDOP.setValueGetter(new ValueGetter() {
            public double getValue(Record record, RecordField field,
                                   VisitInfo visitInfo) {
                return (double) ((QFit12WordRecord) record).gpsPdop;
            }
            public String getStringValue(Record record, RecordField field,
                                         VisitInfo visitInfo) {
                return "" + ((QFit12WordRecord) record).gpsPdop;
            }
        });
        FIELDS.add(RECORDATTR_PULSEWIDTH = new RecordField("pulseWidth",
                "pulseWidth", "", ATTR_PULSEWIDTH, "", "int", "int", 0,
                SEARCHABLE_NO, CHARTABLE_NO));
        RECORDATTR_PULSEWIDTH.setValueGetter(new ValueGetter() {
            public double getValue(Record record, RecordField field,
                                   VisitInfo visitInfo) {
                return (double) ((QFit12WordRecord) record).pulseWidth;
            }
            public String getStringValue(Record record, RecordField field,
                                         VisitInfo visitInfo) {
                return "" + ((QFit12WordRecord) record).pulseWidth;
            }
        });
        FIELDS.add(RECORDATTR_GPSTIME = new RecordField("gpsTime", "gpsTime",
                "", ATTR_GPSTIME, "", "int", "int", 0, SEARCHABLE_NO,
                CHARTABLE_NO));
        RECORDATTR_GPSTIME.setValueGetter(new ValueGetter() {
            public double getValue(Record record, RecordField field,
                                   VisitInfo visitInfo) {
                return (double) ((QFit12WordRecord) record).gpsTime;
            }
            public String getStringValue(Record record, RecordField field,
                                         VisitInfo visitInfo) {
                return "" + ((QFit12WordRecord) record).gpsTime;
            }
        });

    }


    /** _more_          */
    int startSignalStrength;

    /** _more_          */
    int reflectedSignalStrength;

    /** _more_          */
    int azimuth;

    /** _more_          */
    int pitch;

    /** _more_          */
    int roll;

    /** _more_          */
    int gpsPdop;

    /** _more_          */
    int pulseWidth;

    /** _more_          */
    int gpsTime;


    /**
     * _more_
     *
     * @param that _more_
     */
    public QFit12WordRecord(QFit12WordRecord that) {
        super(that);
        this.relativeTime            = that.relativeTime;
        this.laserLatitude           = that.laserLatitude;
        this.laserLongitude          = that.laserLongitude;
        this.elevation               = that.elevation;
        this.startSignalStrength     = that.startSignalStrength;
        this.reflectedSignalStrength = that.reflectedSignalStrength;
        this.azimuth                 = that.azimuth;
        this.pitch                   = that.pitch;
        this.roll                    = that.roll;
        this.gpsPdop                 = that.gpsPdop;
        this.pulseWidth              = that.pulseWidth;
        this.gpsTime                 = that.gpsTime;


    }



    /**
     * _more_
     *
     * @param file _more_
     */
    public QFit12WordRecord(RecordFile file) {
        super(file);
    }



    /**
     * _more_
     *
     * @param file _more_
     * @param bigEndian _more_
     */
    public QFit12WordRecord(RecordFile file, boolean bigEndian) {
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
        if ( !(object instanceof QFit12WordRecord)) {
            return false;
        }
        QFit12WordRecord that = (QFit12WordRecord) object;
        if (this.relativeTime != that.relativeTime) {
            System.err.println("bad relativeTime");

            return false;
        }
        if (this.laserLatitude != that.laserLatitude) {
            System.err.println("bad laserLatitude");

            return false;
        }
        if (this.laserLongitude != that.laserLongitude) {
            System.err.println("bad laserLongitude");

            return false;
        }
        if (this.elevation != that.elevation) {
            System.err.println("bad elevation");

            return false;
        }
        if (this.startSignalStrength != that.startSignalStrength) {
            System.err.println("bad startSignalStrength");

            return false;
        }
        if (this.reflectedSignalStrength != that.reflectedSignalStrength) {
            System.err.println("bad reflectedSignalStrength");

            return false;
        }
        if (this.azimuth != that.azimuth) {
            System.err.println("bad azimuth");

            return false;
        }
        if (this.pitch != that.pitch) {
            System.err.println("bad pitch");

            return false;
        }
        if (this.roll != that.roll) {
            System.err.println("bad roll");

            return false;
        }
        if (this.gpsPdop != that.gpsPdop) {
            System.err.println("bad gpsPdop");

            return false;
        }
        if (this.pulseWidth != that.pulseWidth) {
            System.err.println("bad pulseWidth");

            return false;
        }
        if (this.gpsTime != that.gpsTime) {
            System.err.println("bad gpsTime");

            return false;
        }

        return true;
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
        if (attrId == ATTR_RELATIVETIME) {
            return relativeTime;
        }
        if (attrId == ATTR_LASERLATITUDE) {
            return laserLatitude;
        }
        if (attrId == ATTR_LASERLONGITUDE) {
            return laserLongitude;
        }
        if (attrId == ATTR_ELEVATION) {
            return elevation;
        }
        if (attrId == ATTR_STARTSIGNALSTRENGTH) {
            return startSignalStrength;
        }
        if (attrId == ATTR_REFLECTEDSIGNALSTRENGTH) {
            return reflectedSignalStrength;
        }
        if (attrId == ATTR_AZIMUTH) {
            return azimuth;
        }
        if (attrId == ATTR_PITCH) {
            return pitch;
        }
        if (attrId == ATTR_ROLL) {
            return roll;
        }
        if (attrId == ATTR_GPSPDOP) {
            return gpsPdop;
        }
        if (attrId == ATTR_PULSEWIDTH) {
            return pulseWidth;
        }
        if (attrId == ATTR_GPSTIME) {
            return gpsTime;
        }

        return super.getValue(attrId);

    }



    /**
     * _more_
     *
     * @return _more_
     */
    public int getRecordSize() {
        return super.getRecordSize() + 48;
    }



    /**
     * _more_
     *
     * @param recordIO _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public ReadStatus read(RecordIO recordIO) throws Exception {
        DataInputStream dis    = recordIO.getDataInputStream();
        ReadStatus      status = super.read(recordIO);
        if (status != ReadStatus.OK) {
            return status;
        }
        relativeTime            = readInt(dis);
        laserLatitude           = readInt(dis);
        laserLongitude          = readInt(dis);
        elevation               = readInt(dis);
        startSignalStrength     = readInt(dis);
        reflectedSignalStrength = readInt(dis);
        azimuth                 = readInt(dis);
        pitch                   = readInt(dis);
        roll                    = readInt(dis);
        gpsPdop                 = readInt(dis);
        pulseWidth              = readInt(dis);
        gpsTime                 = readInt(dis);


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
        super.write(recordIO);
        writeInt(dos, relativeTime);
        writeInt(dos, laserLatitude);
        writeInt(dos, laserLongitude);
        writeInt(dos, elevation);
        writeInt(dos, startSignalStrength);
        writeInt(dos, reflectedSignalStrength);
        writeInt(dos, azimuth);
        writeInt(dos, pitch);
        writeInt(dos, roll);
        writeInt(dos, gpsPdop);
        writeInt(dos, pulseWidth);
        writeInt(dos, gpsTime);

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
        pw.print(getStringValue(RECORDATTR_RELATIVETIME, relativeTime));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_LASERLATITUDE, laserLatitude));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_LASERLONGITUDE, laserLongitude));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_ELEVATION, elevation));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_STARTSIGNALSTRENGTH,
                                startSignalStrength));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_REFLECTEDSIGNALSTRENGTH,
                                reflectedSignalStrength));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_AZIMUTH, azimuth));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_PITCH, pitch));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_ROLL, roll));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_GPSPDOP, gpsPdop));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_PULSEWIDTH, pulseWidth));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_GPSTIME, gpsTime));
        myCnt++;

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
        RECORDATTR_RELATIVETIME.printCsvHeader(visitInfo, pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_LASERLATITUDE.printCsvHeader(visitInfo, pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_LASERLONGITUDE.printCsvHeader(visitInfo, pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_ELEVATION.printCsvHeader(visitInfo, pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_STARTSIGNALSTRENGTH.printCsvHeader(visitInfo, pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_REFLECTEDSIGNALSTRENGTH.printCsvHeader(visitInfo, pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_AZIMUTH.printCsvHeader(visitInfo, pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_PITCH.printCsvHeader(visitInfo, pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_ROLL.printCsvHeader(visitInfo, pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_GPSPDOP.printCsvHeader(visitInfo, pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_PULSEWIDTH.printCsvHeader(visitInfo, pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_GPSTIME.printCsvHeader(visitInfo, pw);
        myCnt++;

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
        buff.append(" relativeTime: " + relativeTime + " \n");
        buff.append(" laserLatitude: " + laserLatitude + " \n");
        buff.append(" laserLongitude: " + laserLongitude + " \n");
        buff.append(" elevation: " + elevation + " \n");
        buff.append(" startSignalStrength: " + startSignalStrength + " \n");
        buff.append(" reflectedSignalStrength: " + reflectedSignalStrength
                    + " \n");
        buff.append(" azimuth: " + azimuth + " \n");
        buff.append(" pitch: " + pitch + " \n");
        buff.append(" roll: " + roll + " \n");
        buff.append(" gpsPdop: " + gpsPdop + " \n");
        buff.append(" pulseWidth: " + pulseWidth + " \n");
        buff.append(" gpsTime: " + gpsTime + " \n");

    }



    /**
     * _more_
     *
     * @return _more_
     */
    public int getStartSignalStrength() {
        return startSignalStrength;
    }


    /**
     * _more_
     *
     * @param newValue _more_
     */
    public void setStartSignalStrength(int newValue) {
        startSignalStrength = newValue;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public int getReflectedSignalStrength() {
        return reflectedSignalStrength;
    }


    /**
     * _more_
     *
     * @param newValue _more_
     */
    public void setReflectedSignalStrength(int newValue) {
        reflectedSignalStrength = newValue;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public int getAzimuth() {
        return azimuth;
    }


    /**
     * _more_
     *
     * @param newValue _more_
     */
    public void setAzimuth(int newValue) {
        azimuth = newValue;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public int getPitch() {
        return pitch;
    }


    /**
     * _more_
     *
     * @param newValue _more_
     */
    public void setPitch(int newValue) {
        pitch = newValue;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public int getRoll() {
        return roll;
    }


    /**
     * _more_
     *
     * @param newValue _more_
     */
    public void setRoll(int newValue) {
        roll = newValue;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public int getGpsPdop() {
        return gpsPdop;
    }


    /**
     * _more_
     *
     * @param newValue _more_
     */
    public void setGpsPdop(int newValue) {
        gpsPdop = newValue;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public int getPulseWidth() {
        return pulseWidth;
    }


    /**
     * _more_
     *
     * @param newValue _more_
     */
    public void setPulseWidth(int newValue) {
        pulseWidth = newValue;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public int getGpsTime() {
        return gpsTime;
    }


    /**
     * _more_
     *
     * @param newValue _more_
     */
    public void setGpsTime(int newValue) {
        gpsTime = newValue;
    }



}
