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



package org.ramadda.geodata.point.icebridge;

import org.ramadda.util.IO;
import org.ramadda.data.record.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;


import org.ramadda.data.point.PointFile;



/** This is generated code from generate.tcl. Do not edit it! */
public class Igbgm2GravityV09File extends org.ramadda.data.point.text.TextFile {
public Igbgm2GravityV09File()  {}
public Igbgm2GravityV09File(IO.Path path) throws java.io.IOException {super(path);}
public BaseRecord doMakeRecord(VisitInfo visitInfo) {return new Igbgm2GravityV09Record(this);}
public static void main(String[]args) throws Exception {PointFile.test(args, Igbgm2GravityV09File.class);
}


//generated record class


public static class Igbgm2GravityV09Record extends org.ramadda.data.point.PointRecord {
    public static final int ATTR_FIRST = org.ramadda.data.point.PointRecord.ATTR_LAST;
    public static final List<RecordField> FIELDS = new ArrayList<RecordField>();
    public static final int ATTR_TIME =  ATTR_FIRST + 1;
    public static final RecordField RECORDATTR_TIME;
    public static final int ATTR_LONGITUDE =  ATTR_FIRST + 2;
    public static final RecordField RECORDATTR_LONGITUDE;
    public static final int ATTR_LATITUDE =  ATTR_FIRST + 3;
    public static final RecordField RECORDATTR_LATITUDE;
    public static final int ATTR_AIRCRAFTHEIGHT =  ATTR_FIRST + 4;
    public static final RecordField RECORDATTR_AIRCRAFTHEIGHT;
    public static final int ATTR_FREEAIRANOMALIES =  ATTR_FIRST + 5;
    public static final RecordField RECORDATTR_FREEAIRANOMALIES;
    public static final int ATTR_LAST = ATTR_FIRST + 6;
    

    static {
    FIELDS.add(RECORDATTR_TIME = new RecordField("time", "time", "", ATTR_TIME, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_TIME.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((Igbgm2GravityV09Record)record).time;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((Igbgm2GravityV09Record)record).time;
    }
    });
    FIELDS.add(RECORDATTR_LONGITUDE = new RecordField("longitude", "longitude", "", ATTR_LONGITUDE, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_LONGITUDE.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((Igbgm2GravityV09Record)record).longitude;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((Igbgm2GravityV09Record)record).longitude;
    }
    });
    FIELDS.add(RECORDATTR_LATITUDE = new RecordField("latitude", "latitude", "", ATTR_LATITUDE, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_LATITUDE.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((Igbgm2GravityV09Record)record).latitude;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((Igbgm2GravityV09Record)record).latitude;
    }
    });
    FIELDS.add(RECORDATTR_AIRCRAFTHEIGHT = new RecordField("aircraftHeight", "aircraftHeight", "", ATTR_AIRCRAFTHEIGHT, "m", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_YES));
    RECORDATTR_AIRCRAFTHEIGHT.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((Igbgm2GravityV09Record)record).aircraftHeight;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((Igbgm2GravityV09Record)record).aircraftHeight;
    }
    });
    FIELDS.add(RECORDATTR_FREEAIRANOMALIES = new RecordField("freeAirAnomalies", "freeAirAnomalies", "", ATTR_FREEAIRANOMALIES, "mGal", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_YES));
    RECORDATTR_FREEAIRANOMALIES.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((Igbgm2GravityV09Record)record).freeAirAnomalies;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((Igbgm2GravityV09Record)record).freeAirAnomalies;
    }
    });
    
    }
    

    double time;
    double longitude;
    double latitude;
    double aircraftHeight;
    double freeAirAnomalies;
    

    public  Igbgm2GravityV09Record(Igbgm2GravityV09Record that)  {
        super(that);
        this.time = that.time;
        this.longitude = that.longitude;
        this.latitude = that.latitude;
        this.aircraftHeight = that.aircraftHeight;
        this.freeAirAnomalies = that.freeAirAnomalies;
        
        
    }



    public  Igbgm2GravityV09Record(RecordFile file)  {
        super(file);
    }



    public  Igbgm2GravityV09Record(RecordFile file, boolean bigEndian)  {
        super(file, bigEndian);
    }



    public int getLastAttribute()  {
        return ATTR_LAST;
    }



    public  boolean equals(Object object)  {
        if(!super.equals(object)) {System.err.println("bad super"); return false;} if(!(object instanceof Igbgm2GravityV09Record)) return false;
        Igbgm2GravityV09Record that = (Igbgm2GravityV09Record ) object;
        if(this.time!= that.time) {System.err.println("bad time");  return false;}
        if(this.longitude!= that.longitude) {System.err.println("bad longitude");  return false;}
        if(this.latitude!= that.latitude) {System.err.println("bad latitude");  return false;}
        if(this.aircraftHeight!= that.aircraftHeight) {System.err.println("bad aircraftHeight");  return false;}
        if(this.freeAirAnomalies!= that.freeAirAnomalies) {System.err.println("bad freeAirAnomalies");  return false;}
        return true;
    }




    protected void addFields(List<RecordField> fields)  {
        super.addFields(fields);
        fields.addAll(FIELDS);
    }



    public double getValue(int attrId)  {
        if(attrId == ATTR_TIME) return time;
        if(attrId == ATTR_LONGITUDE) return longitude;
        if(attrId == ATTR_LATITUDE) return latitude;
        if(attrId == ATTR_AIRCRAFTHEIGHT) return aircraftHeight;
        if(attrId == ATTR_FREEAIRANOMALIES) return freeAirAnomalies;
        return super.getValue(attrId);
        
    }



    public int getRecordSize()  {
        return super.getRecordSize() + 40;
    }



    public BaseRecord.ReadStatus read(RecordIO recordIO) throws Exception  {
        BaseRecord.ReadStatus status = BaseRecord.ReadStatus.OK;
        String line = recordIO.readLine();
        if(line == null) return BaseRecord.ReadStatus.EOF;
        line = line.trim();
        if(line.length()==0) return status;
        String[] toks = line.split(" +");
        int fieldCnt = 0;
        time = (double) Double.parseDouble(toks[fieldCnt++]);
        longitude = (double) Double.parseDouble(toks[fieldCnt++]);
        latitude = (double) Double.parseDouble(toks[fieldCnt++]);
        aircraftHeight = (double) Double.parseDouble(toks[fieldCnt++]);
        freeAirAnomalies = (double) Double.parseDouble(toks[fieldCnt++]);
        
        
        return status;
    }



    public void write(RecordIO recordIO) throws IOException  {
        String delimiter = " ";
        PrintWriter  printWriter= recordIO.getPrintWriter();
        printWriter.print(time);
        printWriter.print(delimiter);
        printWriter.print(longitude);
        printWriter.print(delimiter);
        printWriter.print(latitude);
        printWriter.print(delimiter);
        printWriter.print(aircraftHeight);
        printWriter.print(delimiter);
        printWriter.print(freeAirAnomalies);
        printWriter.print("\n");
        
    }



    public int doPrintCsv(VisitInfo visitInfo,PrintWriter pw)  {
        boolean includeVector = visitInfo.getProperty(PROP_INCLUDEVECTOR, false);
        int superCnt = super.doPrintCsv(visitInfo,pw);
        int myCnt = 0;
        if(superCnt>0) pw.print(',');
        pw.print(getStringValue(RECORDATTR_TIME, time));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_LONGITUDE, longitude));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_LATITUDE, latitude));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_AIRCRAFTHEIGHT, aircraftHeight));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_FREEAIRANOMALIES, freeAirAnomalies));
        myCnt++;
        return myCnt+superCnt;
        
    }



    public int doPrintCsvHeader(VisitInfo visitInfo,PrintWriter pw)  {
        int superCnt = super.doPrintCsvHeader(visitInfo,pw);
        int myCnt = 0;
        boolean includeVector = visitInfo.getProperty(PROP_INCLUDEVECTOR, false);
        if(superCnt>0) pw.print(',');
        RECORDATTR_TIME.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_LONGITUDE.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_LATITUDE.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_AIRCRAFTHEIGHT.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_FREEAIRANOMALIES.printCsvHeader(visitInfo,pw);
        myCnt++;
        return myCnt+superCnt;
        
    }



    public void print(Appendable buff)  throws Exception  {
        super.print(buff);
        buff.append(" time: " + time+" \n");
        buff.append(" longitude: " + longitude+" \n");
        buff.append(" latitude: " + latitude+" \n");
        buff.append(" aircraftHeight: " + aircraftHeight+" \n");
        buff.append(" freeAirAnomalies: " + freeAirAnomalies+" \n");
        
    }



    public double getTime()  {
        return time;
    }


    public void setTime(double newValue)  {
        time = newValue;
    }


    public double getLongitude()  {
        return longitude;
    }


    public void setLongitude(double newValue)  {
        longitude = newValue;
    }


    public double getLatitude()  {
        return latitude;
    }


    public void setLatitude(double newValue)  {
        latitude = newValue;
    }


    public double getAircraftHeight()  {
        return aircraftHeight;
    }


    public void setAircraftHeight(double newValue)  {
        aircraftHeight = newValue;
    }


    public double getFreeAirAnomalies()  {
        return freeAirAnomalies;
    }


    public void setFreeAirAnomalies(double newValue)  {
        freeAirAnomalies = newValue;
    }



}

}



