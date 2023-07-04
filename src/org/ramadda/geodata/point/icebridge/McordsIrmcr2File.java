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
public class McordsIrmcr2File extends org.ramadda.data.point.text.TextFile {
public McordsIrmcr2File()  {}
public McordsIrmcr2File(IO.Path path) throws java.io.IOException {super(path);}
public BaseRecord doMakeRecord(VisitInfo visitInfo) {return new McordsIrmcr2Record(this);}
public static void main(String[]args) throws Exception {PointFile.test(args, McordsIrmcr2File.class);
}


@Override
public int getSkipLines(VisitInfo visitInfo) {
return  1;
}


@Override
public boolean isCapable(String action) {
if(action.equals(ACTION_MAPINCHART)) return true;
return super.isCapable(action);
}


//generated record class


public static class McordsIrmcr2Record extends org.ramadda.data.point.PointRecord {
    public static final int ATTR_FIRST = org.ramadda.data.point.PointRecord.ATTR_LAST;
    public static final List<RecordField> FIELDS = new ArrayList<RecordField>();
    public static final int ATTR_LATITUDE =  ATTR_FIRST + 1;
    public static final RecordField RECORDATTR_LATITUDE;
    public static final int ATTR_LONGITUDE =  ATTR_FIRST + 2;
    public static final RecordField RECORDATTR_LONGITUDE;
    public static final int ATTR_TIME =  ATTR_FIRST + 3;
    public static final RecordField RECORDATTR_TIME;
    public static final int ATTR_THICKNESS =  ATTR_FIRST + 4;
    public static final RecordField RECORDATTR_THICKNESS;
    public static final int ATTR_ALTITUDE =  ATTR_FIRST + 5;
    public static final RecordField RECORDATTR_ALTITUDE;
    public static final int ATTR_FRAME =  ATTR_FIRST + 6;
    public static final RecordField RECORDATTR_FRAME;
    public static final int ATTR_BOTTOM =  ATTR_FIRST + 7;
    public static final RecordField RECORDATTR_BOTTOM;
    public static final int ATTR_SURFACE =  ATTR_FIRST + 8;
    public static final RecordField RECORDATTR_SURFACE;
    public static final int ATTR_QUALITY =  ATTR_FIRST + 9;
    public static final RecordField RECORDATTR_QUALITY;
    public static final int ATTR_LAST = ATTR_FIRST + 10;
    

    static {
    FIELDS.add(RECORDATTR_LATITUDE = new RecordField("latitude", "latitude", "", ATTR_LATITUDE, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_LATITUDE.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((McordsIrmcr2Record)record).latitude;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((McordsIrmcr2Record)record).latitude;
    }
    });
    FIELDS.add(RECORDATTR_LONGITUDE = new RecordField("longitude", "longitude", "", ATTR_LONGITUDE, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_LONGITUDE.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((McordsIrmcr2Record)record).longitude;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((McordsIrmcr2Record)record).longitude;
    }
    });
    FIELDS.add(RECORDATTR_TIME = new RecordField("time", "time", "", ATTR_TIME, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_TIME.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((McordsIrmcr2Record)record).time;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((McordsIrmcr2Record)record).time;
    }
    });
    FIELDS.add(RECORDATTR_THICKNESS = new RecordField("thickness", "thickness", "", ATTR_THICKNESS, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_YES));
    RECORDATTR_THICKNESS.setMissingValue(-9999.0);
    RECORDATTR_THICKNESS.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((McordsIrmcr2Record)record).thickness;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((McordsIrmcr2Record)record).thickness;
    }
    });
    FIELDS.add(RECORDATTR_ALTITUDE = new RecordField("altitude", "altitude", "", ATTR_ALTITUDE, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_YES));
    RECORDATTR_ALTITUDE.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((McordsIrmcr2Record)record).altitude;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((McordsIrmcr2Record)record).altitude;
    }
    });
    FIELDS.add(RECORDATTR_FRAME = new RecordField("frame", "frame", "", ATTR_FRAME, "", "int", "int", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_FRAME.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((McordsIrmcr2Record)record).frame;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((McordsIrmcr2Record)record).frame;
    }
    });
    FIELDS.add(RECORDATTR_BOTTOM = new RecordField("bottom", "bottom", "", ATTR_BOTTOM, "m", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_YES));
    RECORDATTR_BOTTOM.setMissingValue(-9999.0);
    RECORDATTR_BOTTOM.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((McordsIrmcr2Record)record).bottom;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((McordsIrmcr2Record)record).bottom;
    }
    });
    FIELDS.add(RECORDATTR_SURFACE = new RecordField("surface", "surface", "", ATTR_SURFACE, "m", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_YES));
    RECORDATTR_SURFACE.setMissingValue(-9999.0);
    RECORDATTR_SURFACE.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((McordsIrmcr2Record)record).surface;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((McordsIrmcr2Record)record).surface;
    }
    });
    FIELDS.add(RECORDATTR_QUALITY = new RecordField("quality", "quality", "", ATTR_QUALITY, "", "int", "int", 0, SEARCHABLE_NO,CHARTABLE_YES));
    RECORDATTR_QUALITY.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((McordsIrmcr2Record)record).quality;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((McordsIrmcr2Record)record).quality;
    }
    });
    
    }
    

    double time;
    double thickness;
    int frame;
    double bottom;
    double surface;
    int quality;
    

    public  McordsIrmcr2Record(McordsIrmcr2Record that)  {
        super(that);
        this.latitude = that.latitude;
        this.longitude = that.longitude;
        this.time = that.time;
        this.thickness = that.thickness;
        this.altitude = that.altitude;
        this.frame = that.frame;
        this.bottom = that.bottom;
        this.surface = that.surface;
        this.quality = that.quality;
        
        
    }



    public  McordsIrmcr2Record(RecordFile file)  {
        super(file);
    }



    public  McordsIrmcr2Record(RecordFile file, boolean bigEndian)  {
        super(file, bigEndian);
    }



    public int getLastAttribute()  {
        return ATTR_LAST;
    }



    public  boolean equals(Object object)  {
        if(!super.equals(object)) {System.err.println("bad super"); return false;} if(!(object instanceof McordsIrmcr2Record)) return false;
        McordsIrmcr2Record that = (McordsIrmcr2Record ) object;
        if(this.latitude!= that.latitude) {System.err.println("bad latitude");  return false;}
        if(this.longitude!= that.longitude) {System.err.println("bad longitude");  return false;}
        if(this.time!= that.time) {System.err.println("bad time");  return false;}
        if(this.thickness!= that.thickness) {System.err.println("bad thickness");  return false;}
        if(this.altitude!= that.altitude) {System.err.println("bad altitude");  return false;}
        if(this.frame!= that.frame) {System.err.println("bad frame");  return false;}
        if(this.bottom!= that.bottom) {System.err.println("bad bottom");  return false;}
        if(this.surface!= that.surface) {System.err.println("bad surface");  return false;}
        if(this.quality!= that.quality) {System.err.println("bad quality");  return false;}
        return true;
    }




    protected void addFields(List<RecordField> fields)  {
        super.addFields(fields);
        fields.addAll(FIELDS);
    }



    public double getValue(int attrId)  {
        if(attrId == ATTR_LATITUDE) return latitude;
        if(attrId == ATTR_LONGITUDE) return longitude;
        if(attrId == ATTR_TIME) return time;
        if(attrId == ATTR_THICKNESS) return thickness;
        if(attrId == ATTR_ALTITUDE) return altitude;
        if(attrId == ATTR_FRAME) return frame;
        if(attrId == ATTR_BOTTOM) return bottom;
        if(attrId == ATTR_SURFACE) return surface;
        if(attrId == ATTR_QUALITY) return quality;
        return super.getValue(attrId);
        
    }



    public int getRecordSize()  {
        return super.getRecordSize() + 64;
    }



    public BaseRecord.ReadStatus read(RecordIO recordIO) throws Exception  {
        BaseRecord.ReadStatus status = BaseRecord.ReadStatus.OK;
        String line = recordIO.readLine();
        if(line == null) return BaseRecord.ReadStatus.EOF;
        line = line.trim();
        if(line.length()==0) return status;
        String[] toks = line.split(",");
        int fieldCnt = 0;
        latitude = (double) Double.parseDouble(toks[fieldCnt++]);
        longitude = (double) Double.parseDouble(toks[fieldCnt++]);
        time = (double) Double.parseDouble(toks[fieldCnt++]);
        thickness = (double) Double.parseDouble(toks[fieldCnt++]);
        if(isMissingValue(RECORDATTR_THICKNESS, thickness)) thickness = Double.NaN;
        altitude = (double) Double.parseDouble(toks[fieldCnt++]);
        frame = (int) Double.parseDouble(toks[fieldCnt++]);
        bottom = (double) Double.parseDouble(toks[fieldCnt++]);
        if(isMissingValue(RECORDATTR_BOTTOM, bottom)) bottom = Double.NaN;
        surface = (double) Double.parseDouble(toks[fieldCnt++]);
        if(isMissingValue(RECORDATTR_SURFACE, surface)) surface = Double.NaN;
        quality = (int) Double.parseDouble(toks[fieldCnt++]);
        
        
        return status;
    }



    public void write(RecordIO recordIO) throws IOException  {
        String delimiter = ",";
        PrintWriter  printWriter= recordIO.getPrintWriter();
        printWriter.print(latitude);
        printWriter.print(delimiter);
        printWriter.print(longitude);
        printWriter.print(delimiter);
        printWriter.print(time);
        printWriter.print(delimiter);
        printWriter.print(thickness);
        printWriter.print(delimiter);
        printWriter.print(altitude);
        printWriter.print(delimiter);
        printWriter.print(frame);
        printWriter.print(delimiter);
        printWriter.print(bottom);
        printWriter.print(delimiter);
        printWriter.print(surface);
        printWriter.print(delimiter);
        printWriter.print(quality);
        printWriter.print("\n");
        
    }



    public int doPrintCsv(VisitInfo visitInfo,PrintWriter pw)  {
        boolean includeVector = visitInfo.getProperty(PROP_INCLUDEVECTOR, false);
        int superCnt = super.doPrintCsv(visitInfo,pw);
        int myCnt = 0;
        if(superCnt>0) pw.print(',');
        pw.print(getStringValue(RECORDATTR_LATITUDE, latitude));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_LONGITUDE, longitude));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_TIME, time));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_THICKNESS, thickness));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_ALTITUDE, altitude));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_FRAME, frame));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_BOTTOM, bottom));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_SURFACE, surface));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_QUALITY, quality));
        myCnt++;
        return myCnt+superCnt;
        
    }



    public int doPrintCsvHeader(VisitInfo visitInfo,PrintWriter pw)  {
        int superCnt = super.doPrintCsvHeader(visitInfo,pw);
        int myCnt = 0;
        boolean includeVector = visitInfo.getProperty(PROP_INCLUDEVECTOR, false);
        if(superCnt>0) pw.print(',');
        RECORDATTR_LATITUDE.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_LONGITUDE.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_TIME.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_THICKNESS.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_ALTITUDE.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_FRAME.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_BOTTOM.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_SURFACE.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_QUALITY.printCsvHeader(visitInfo,pw);
        myCnt++;
        return myCnt+superCnt;
        
    }



    public void print(Appendable buff)  throws Exception  {
        super.print(buff);
        buff.append(" latitude: " + latitude+" \n");
        buff.append(" longitude: " + longitude+" \n");
        buff.append(" time: " + time+" \n");
        buff.append(" thickness: " + thickness+" \n");
        buff.append(" altitude: " + altitude+" \n");
        buff.append(" frame: " + frame+" \n");
        buff.append(" bottom: " + bottom+" \n");
        buff.append(" surface: " + surface+" \n");
        buff.append(" quality: " + quality+" \n");
        
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


    public int getFrame()  {
        return frame;
    }


    public void setFrame(int newValue)  {
        frame = newValue;
    }


    public double getBottom()  {
        return bottom;
    }


    public void setBottom(double newValue)  {
        bottom = newValue;
    }


    public double getSurface()  {
        return surface;
    }


    public void setSurface(double newValue)  {
        surface = newValue;
    }


    public int getQuality()  {
        return quality;
    }


    public void setQuality(int newValue)  {
        quality = newValue;
    }



}

}



