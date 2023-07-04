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
public class FloatLatLonRecord extends org.ramadda.data.point.PointRecord {

    /**  */
    public static final int ATTR_FIRST =
        org.ramadda.data.point.PointRecord.ATTR_LAST;

    /**  */
    public static final List<RecordField> FIELDS =
        new ArrayList<RecordField>();

    /**  */
    public static final int ATTR_LAT = ATTR_FIRST + 1;

    /**  */
    public static final RecordField RECORDATTR_LAT;

    /**  */
    public static final int ATTR_LON = ATTR_FIRST + 2;

    /**  */
    public static final RecordField RECORDATTR_LON;

    /**  */
    public static final int ATTR_LAST = ATTR_FIRST + 3;


    static {
        FIELDS.add(RECORDATTR_LAT = new RecordField("lat", "lat", "",
                ATTR_LAT, "", "float", "float", 0, SEARCHABLE_NO,
                CHARTABLE_NO));
        RECORDATTR_LAT.setValueGetter(new ValueGetter() {
            public double getValue(BaseRecord record, RecordField field,
                                   VisitInfo visitInfo) {
                return (double) ((FloatLatLonRecord) record).lat;
            }
            public String getStringValue(BaseRecord record,
                                         RecordField field,
                                         VisitInfo visitInfo) {
                return "" + ((FloatLatLonRecord) record).lat;
            }
        });
        FIELDS.add(RECORDATTR_LON = new RecordField("lon", "lon", "",
                ATTR_LON, "", "float", "float", 0, SEARCHABLE_NO,
                CHARTABLE_NO));
        RECORDATTR_LON.setValueGetter(new ValueGetter() {
            public double getValue(BaseRecord record, RecordField field,
                                   VisitInfo visitInfo) {
                return (double) ((FloatLatLonRecord) record).lon;
            }
            public String getStringValue(BaseRecord record,
                                         RecordField field,
                                         VisitInfo visitInfo) {
                return "" + ((FloatLatLonRecord) record).lon;
            }
        });

    }


    /**  */
    float lat;

    /**  */
    float lon;


    /**
     *
     *
     * @param that _more_
     */
    public FloatLatLonRecord(FloatLatLonRecord that) {
        super(that);
        this.lat = that.lat;
        this.lon = that.lon;


    }



    /**
     *
     *
     * @param file _more_
     */
    public FloatLatLonRecord(RecordFile file) {
        super(file);
    }



    /**
     *
     *
     * @param file _more_
     * @param bigEndian _more_
     */
    public FloatLatLonRecord(RecordFile file, boolean bigEndian) {
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
        if ( !(object instanceof FloatLatLonRecord)) {
            return false;
        }
        FloatLatLonRecord that = (FloatLatLonRecord) object;
        if (this.lat != that.lat) {
            System.err.println("bad lat");

            return false;
        }
        if (this.lon != that.lon) {
            System.err.println("bad lon");

            return false;
        }

        return true;
    }




    /**
     *  @return _more_
     */
    public double getLatitude() {
        return (double) lat;
    }

    /**
     *  @return _more_
     */
    public double getLongitude() {
        return (double) lon;
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
        if (attrId == ATTR_LAT) {
            return lat;
        }
        if (attrId == ATTR_LON) {
            return lon;
        }

        return super.getValue(attrId);

    }



    /**
     *  @return _more_
     */
    public int getRecordSize() {
        return super.getRecordSize() + 8;
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
        lat = readFloat(dis);
        lon = readFloat(dis);


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
        writeFloat(dos, lat);
        writeFloat(dos, lon);

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
        pw.print(getStringValue(RECORDATTR_LAT, lat));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_LON, lon));
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
        RECORDATTR_LAT.printCsvHeader(visitInfo, pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_LON.printCsvHeader(visitInfo, pw);
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
        buff.append(" lat: " + lat + " \n");
        buff.append(" lon: " + lon + " \n");

    }



    /**
     *  @return _more_
     */
    public float getLat() {
        return lat;
    }


    /**
     *
     * @param newValue _more_
     */
    public void setLat(float newValue) {
        lat = newValue;
    }


    /**
     *  @return _more_
     */
    public float getLon() {
        return lon;
    }


    /**
     *
     * @param newValue _more_
     */
    public void setLon(float newValue) {
        lon = newValue;
    }



}
