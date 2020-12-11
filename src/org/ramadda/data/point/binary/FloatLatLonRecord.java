
/*
 * Copyright 2013 ramadda.org
 * http://ramadda.org
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */



package org.ramadda.data.point.binary;

import org.ramadda.data.record.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;





/** This is generated code from generate.tcl. Do not edit it! */
public class FloatLatLonRecord extends org.ramadda.data.point.PointRecord {
    public static final int ATTR_FIRST = org.ramadda.data.point.PointRecord.ATTR_LAST;
    public static final List<RecordField> FIELDS = new ArrayList<RecordField>();
    public static final int ATTR_LAT =  ATTR_FIRST + 1;
    public static final RecordField RECORDATTR_LAT;
    public static final int ATTR_LON =  ATTR_FIRST + 2;
    public static final RecordField RECORDATTR_LON;
    public static final int ATTR_LAST = ATTR_FIRST + 3;
    

    static {
    FIELDS.add(RECORDATTR_LAT = new RecordField("lat", "lat", "", ATTR_LAT, "", "float", "float", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_LAT.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((FloatLatLonRecord)record).lat;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((FloatLatLonRecord)record).lat;
    }
    });
    FIELDS.add(RECORDATTR_LON = new RecordField("lon", "lon", "", ATTR_LON, "", "float", "float", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_LON.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((FloatLatLonRecord)record).lon;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((FloatLatLonRecord)record).lon;
    }
    });
    
    }
    

    float lat;
    float lon;
    

    public  FloatLatLonRecord(FloatLatLonRecord that)  {
        super(that);
        this.lat = that.lat;
        this.lon = that.lon;
        
        
    }



    public  FloatLatLonRecord(RecordFile file)  {
        super(file);
    }



    public  FloatLatLonRecord(RecordFile file, boolean bigEndian)  {
        super(file, bigEndian);
    }



    public int getLastAttribute()  {
        return ATTR_LAST;
    }



    public  boolean equals(Object object)  {
        if(!super.equals(object)) {System.err.println("bad super"); return false;} if(!(object instanceof FloatLatLonRecord)) return false;
        FloatLatLonRecord that = (FloatLatLonRecord ) object;
        if(this.lat!= that.lat) {System.err.println("bad lat");  return false;}
        if(this.lon!= that.lon) {System.err.println("bad lon");  return false;}
        return true;
    }




    public double getLatitude() {
        return (double) lat;
    }
    public double getLongitude() {
        return (double) lon;
    }

    protected void addFields(List<RecordField> fields)  {
        super.addFields(fields);
        fields.addAll(FIELDS);
    }



    public double getValue(int attrId)  {
        if(attrId == ATTR_LAT) return lat;
        if(attrId == ATTR_LON) return lon;
        return super.getValue(attrId);
        
    }



    public int getRecordSize()  {
        return super.getRecordSize() + 8;
    }



    public ReadStatus read(RecordIO recordIO) throws Exception  {
        DataInputStream dis = recordIO.getDataInputStream();
        ReadStatus status= super.read(recordIO);
        if(status!=ReadStatus.OK)  return status;
        lat =  readFloat(dis);
        lon =  readFloat(dis);
        
        
        return ReadStatus.OK;
    }



    public void write(RecordIO recordIO) throws IOException  {
        DataOutputStream dos = recordIO.getDataOutputStream();
        super.write(recordIO);
        writeFloat(dos, lat);
        writeFloat(dos, lon);
        
    }



    public int doPrintCsv(VisitInfo visitInfo,PrintWriter pw)  {
        boolean includeVector = visitInfo.getProperty(PROP_INCLUDEVECTOR, false);
        int superCnt = super.doPrintCsv(visitInfo,pw);
        int myCnt = 0;
        if(superCnt>0) pw.print(',');
        pw.print(getStringValue(RECORDATTR_LAT, lat));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_LON, lon));
        myCnt++;
        return myCnt+superCnt;
        
    }



    public int doPrintCsvHeader(VisitInfo visitInfo,PrintWriter pw)  {
        int superCnt = super.doPrintCsvHeader(visitInfo,pw);
        int myCnt = 0;
        boolean includeVector = visitInfo.getProperty(PROP_INCLUDEVECTOR, false);
        if(superCnt>0) pw.print(',');
        RECORDATTR_LAT.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_LON.printCsvHeader(visitInfo,pw);
        myCnt++;
        return myCnt+superCnt;
        
    }



    public void print(Appendable buff)  throws Exception  {
        super.print(buff);
        buff.append(" lat: " + lat+" \n");
        buff.append(" lon: " + lon+" \n");
        
    }



    public float getLat()  {
        return lat;
    }


    public void setLat(float newValue)  {
        lat = newValue;
    }


    public float getLon()  {
        return lon;
    }


    public void setLon(float newValue)  {
        lon = newValue;
    }



}



