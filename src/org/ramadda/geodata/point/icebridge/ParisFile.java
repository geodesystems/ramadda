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
public class ParisFile extends org.ramadda.data.point.text.TextFile {
public ParisFile()  {}
public ParisFile(IO.Path path) throws java.io.IOException {super(path);}
public BaseRecord doMakeRecord(VisitInfo visitInfo) {return new ParisRecord(this);}
public static void main(String[]args) throws Exception {PointFile.test(args, ParisFile.class);
}


//generated record class


public static class ParisRecord extends org.ramadda.data.point.PointRecord {
    public static final int ATTR_FIRST = org.ramadda.data.point.PointRecord.ATTR_LAST;
    public static final List<RecordField> FIELDS = new ArrayList<RecordField>();
    public static final int ATTR_LAT =  ATTR_FIRST + 1;
    public static final RecordField RECORDATTR_LAT;
    public static final int ATTR_LON =  ATTR_FIRST + 2;
    public static final RecordField RECORDATTR_LON;
    public static final int ATTR_TIME =  ATTR_FIRST + 3;
    public static final RecordField RECORDATTR_TIME;
    public static final int ATTR_THICKNESS =  ATTR_FIRST + 4;
    public static final RecordField RECORDATTR_THICKNESS;
    public static final int ATTR_AIRCRAFTALTITUDE =  ATTR_FIRST + 5;
    public static final RecordField RECORDATTR_AIRCRAFTALTITUDE;
    public static final int ATTR_CONFIDENCE =  ATTR_FIRST + 6;
    public static final RecordField RECORDATTR_CONFIDENCE;
    public static final int ATTR_LAST = ATTR_FIRST + 7;
    

    static {
    FIELDS.add(RECORDATTR_LAT = new RecordField("lat", "lat", "", ATTR_LAT, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_LAT.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((ParisRecord)record).lat;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((ParisRecord)record).lat;
    }
    });
    FIELDS.add(RECORDATTR_LON = new RecordField("lon", "lon", "", ATTR_LON, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_LON.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((ParisRecord)record).lon;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((ParisRecord)record).lon;
    }
    });
    FIELDS.add(RECORDATTR_TIME = new RecordField("time", "time", "", ATTR_TIME, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_TIME.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((ParisRecord)record).time;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((ParisRecord)record).time;
    }
    });
    FIELDS.add(RECORDATTR_THICKNESS = new RecordField("thickness", "thickness", "", ATTR_THICKNESS, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_YES));
    RECORDATTR_THICKNESS.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((ParisRecord)record).thickness;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((ParisRecord)record).thickness;
    }
    });
    FIELDS.add(RECORDATTR_AIRCRAFTALTITUDE = new RecordField("aircraftAltitude", "aircraftAltitude", "", ATTR_AIRCRAFTALTITUDE, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_AIRCRAFTALTITUDE.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((ParisRecord)record).aircraftAltitude;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((ParisRecord)record).aircraftAltitude;
    }
    });
    FIELDS.add(RECORDATTR_CONFIDENCE = new RecordField("confidence", "confidence", "", ATTR_CONFIDENCE, "", "int", "int", 0, SEARCHABLE_YES,CHARTABLE_YES));
    RECORDATTR_CONFIDENCE.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((ParisRecord)record).confidence;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((ParisRecord)record).confidence;
    }
    });
    
    }
    

    double lat;
    double lon;
    double time;
    double thickness;
    double aircraftAltitude;
    int confidence;
    

    public  ParisRecord(ParisRecord that)  {
        super(that);
        this.lat = that.lat;
        this.lon = that.lon;
        this.time = that.time;
        this.thickness = that.thickness;
        this.aircraftAltitude = that.aircraftAltitude;
        this.confidence = that.confidence;
        
        
    }



    public  ParisRecord(RecordFile file)  {
        super(file);
    }



    public  ParisRecord(RecordFile file, boolean bigEndian)  {
        super(file, bigEndian);
    }



    public int getLastAttribute()  {
        return ATTR_LAST;
    }



    public  boolean equals(Object object)  {
        if(!super.equals(object)) {System.err.println("bad super"); return false;} if(!(object instanceof ParisRecord)) return false;
        ParisRecord that = (ParisRecord ) object;
        if(this.lat!= that.lat) {System.err.println("bad lat");  return false;}
        if(this.lon!= that.lon) {System.err.println("bad lon");  return false;}
        if(this.time!= that.time) {System.err.println("bad time");  return false;}
        if(this.thickness!= that.thickness) {System.err.println("bad thickness");  return false;}
        if(this.aircraftAltitude!= that.aircraftAltitude) {System.err.println("bad aircraftAltitude");  return false;}
        if(this.confidence!= that.confidence) {System.err.println("bad confidence");  return false;}
        return true;
    }




    public double getLatitude() {
        return lat;
    }
    public double getLongitude() {
        return lon;
    }
    public double getAltitude() {
        return aircraftAltitude;
    }


    protected void addFields(List<RecordField> fields)  {
        super.addFields(fields);
        fields.addAll(FIELDS);
    }



    public double getValue(int attrId)  {
        if(attrId == ATTR_LAT) return lat;
        if(attrId == ATTR_LON) return lon;
        if(attrId == ATTR_TIME) return time;
        if(attrId == ATTR_THICKNESS) return thickness;
        if(attrId == ATTR_AIRCRAFTALTITUDE) return aircraftAltitude;
        if(attrId == ATTR_CONFIDENCE) return confidence;
        return super.getValue(attrId);
        
    }



    public int getRecordSize()  {
        return super.getRecordSize() + 44;
    }



    public BaseRecord.ReadStatus read(RecordIO recordIO) throws Exception  {
        BaseRecord.ReadStatus status = BaseRecord.ReadStatus.OK;
        String line = recordIO.readLine();
        if(line == null) return BaseRecord.ReadStatus.EOF;
        line = line.trim();
        if(line.length()==0) return status;
        String[] toks = line.split(" +");
        int fieldCnt = 0;
        lat = (double) Double.parseDouble(toks[fieldCnt++]);
        lon = (double) Double.parseDouble(toks[fieldCnt++]);
        time = (double) Double.parseDouble(toks[fieldCnt++]);
        thickness = (double) Double.parseDouble(toks[fieldCnt++]);
        aircraftAltitude = (double) Double.parseDouble(toks[fieldCnt++]);
        confidence = (int) Double.parseDouble(toks[fieldCnt++]);
        
        
        return status;
    }



    public void write(RecordIO recordIO) throws IOException  {
        String delimiter = " ";
        PrintWriter  printWriter= recordIO.getPrintWriter();
        printWriter.print(lat);
        printWriter.print(delimiter);
        printWriter.print(lon);
        printWriter.print(delimiter);
        printWriter.print(time);
        printWriter.print(delimiter);
        printWriter.print(thickness);
        printWriter.print(delimiter);
        printWriter.print(aircraftAltitude);
        printWriter.print(delimiter);
        printWriter.print(confidence);
        printWriter.print("\n");
        
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
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_TIME, time));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_THICKNESS, thickness));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_AIRCRAFTALTITUDE, aircraftAltitude));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_CONFIDENCE, confidence));
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
        pw.print(',');
        RECORDATTR_TIME.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_THICKNESS.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_AIRCRAFTALTITUDE.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_CONFIDENCE.printCsvHeader(visitInfo,pw);
        myCnt++;
        return myCnt+superCnt;
        
    }



    public void print(Appendable buff)  throws Exception  {
        super.print(buff);
        buff.append(" lat: " + lat+" \n");
        buff.append(" lon: " + lon+" \n");
        buff.append(" time: " + time+" \n");
        buff.append(" thickness: " + thickness+" \n");
        buff.append(" aircraftAltitude: " + aircraftAltitude+" \n");
        buff.append(" confidence: " + confidence+" \n");
        
    }



    public double getLat()  {
        return lat;
    }


    public void setLat(double newValue)  {
        lat = newValue;
    }


    public double getLon()  {
        return lon;
    }


    public void setLon(double newValue)  {
        lon = newValue;
    }


    public double getTime()  {
        return time;
    }


    public void setTime(double newValue)  {
        time = newValue;
    }


    public double getThickness()  {
        return thickness;
    }


    public void setThickness(double newValue)  {
        thickness = newValue;
    }


    public double getAircraftAltitude()  {
        return aircraftAltitude;
    }


    public void setAircraftAltitude(double newValue)  {
        aircraftAltitude = newValue;
    }


    public int getConfidence()  {
        return confidence;
    }


    public void setConfidence(int newValue)  {
        confidence = newValue;
    }



}

}



