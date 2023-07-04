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
public class McordsIrmcr3File extends org.ramadda.data.point.text.TextFile {
public McordsIrmcr3File()  {}
public McordsIrmcr3File(IO.Path path) throws java.io.IOException {super(path);}
public BaseRecord doMakeRecord(VisitInfo visitInfo) {return new McordsIrmcr3Record(this);}
public static void main(String[]args) throws Exception {PointFile.test(args, McordsIrmcr3File.class);
}


@Override
public int getSkipLines(VisitInfo visitInfo) {
return  1;
}



//generated record class


public static class McordsIrmcr3Record extends org.ramadda.data.point.PointRecord {
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
    public static final int ATTR_SURFACE =  ATTR_FIRST + 7;
    public static final RecordField RECORDATTR_SURFACE;
    public static final int ATTR_BOTTOM =  ATTR_FIRST + 8;
    public static final RecordField RECORDATTR_BOTTOM;
    public static final int ATTR_QUALITY =  ATTR_FIRST + 9;
    public static final RecordField RECORDATTR_QUALITY;
    public static final int ATTR_A_SURFACE =  ATTR_FIRST + 10;
    public static final RecordField RECORDATTR_A_SURFACE;
    public static final int ATTR_A_BOTTOM =  ATTR_FIRST + 11;
    public static final RecordField RECORDATTR_A_BOTTOM;
    public static final int ATTR_LAST = ATTR_FIRST + 12;
    

    static {
    FIELDS.add(RECORDATTR_LATITUDE = new RecordField("latitude", "latitude", "", ATTR_LATITUDE, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_LATITUDE.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((McordsIrmcr3Record)record).latitude;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((McordsIrmcr3Record)record).latitude;
    }
    });
    FIELDS.add(RECORDATTR_LONGITUDE = new RecordField("longitude", "longitude", "", ATTR_LONGITUDE, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_LONGITUDE.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((McordsIrmcr3Record)record).longitude;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((McordsIrmcr3Record)record).longitude;
    }
    });
    FIELDS.add(RECORDATTR_TIME = new RecordField("time", "time", "", ATTR_TIME, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_TIME.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((McordsIrmcr3Record)record).time;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((McordsIrmcr3Record)record).time;
    }
    });
    FIELDS.add(RECORDATTR_THICKNESS = new RecordField("thickness", "thickness", "", ATTR_THICKNESS, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_YES));
    RECORDATTR_THICKNESS.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((McordsIrmcr3Record)record).thickness;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((McordsIrmcr3Record)record).thickness;
    }
    });
    FIELDS.add(RECORDATTR_ALTITUDE = new RecordField("altitude", "altitude", "", ATTR_ALTITUDE, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_YES));
    RECORDATTR_ALTITUDE.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((McordsIrmcr3Record)record).altitude;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((McordsIrmcr3Record)record).altitude;
    }
    });
    FIELDS.add(RECORDATTR_FRAME = new RecordField("frame", "frame", "", ATTR_FRAME, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_YES));
    RECORDATTR_FRAME.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((McordsIrmcr3Record)record).frame;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((McordsIrmcr3Record)record).frame;
    }
    });
    FIELDS.add(RECORDATTR_SURFACE = new RecordField("surface", "surface", "", ATTR_SURFACE, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_YES));
    RECORDATTR_SURFACE.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((McordsIrmcr3Record)record).surface;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((McordsIrmcr3Record)record).surface;
    }
    });
    FIELDS.add(RECORDATTR_BOTTOM = new RecordField("bottom", "bottom", "", ATTR_BOTTOM, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_YES));
    RECORDATTR_BOTTOM.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((McordsIrmcr3Record)record).bottom;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((McordsIrmcr3Record)record).bottom;
    }
    });
    FIELDS.add(RECORDATTR_QUALITY = new RecordField("quality", "quality", "", ATTR_QUALITY, "", "int", "int", 0, SEARCHABLE_NO,CHARTABLE_YES));
    RECORDATTR_QUALITY.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((McordsIrmcr3Record)record).quality;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((McordsIrmcr3Record)record).quality;
    }
    });
    FIELDS.add(RECORDATTR_A_SURFACE = new RecordField("a_surface", "a_surface", "", ATTR_A_SURFACE, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_YES));
    RECORDATTR_A_SURFACE.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((McordsIrmcr3Record)record).a_surface;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((McordsIrmcr3Record)record).a_surface;
    }
    });
    FIELDS.add(RECORDATTR_A_BOTTOM = new RecordField("a_bottom", "a_bottom", "", ATTR_A_BOTTOM, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_YES));
    RECORDATTR_A_BOTTOM.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((McordsIrmcr3Record)record).a_bottom;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((McordsIrmcr3Record)record).a_bottom;
    }
    });
    
    }
    

    double time;
    double thickness;
    double frame;
    double surface;
    double bottom;
    int quality;
    double a_surface;
    double a_bottom;
    

    public  McordsIrmcr3Record(McordsIrmcr3Record that)  {
        super(that);
        this.latitude = that.latitude;
        this.longitude = that.longitude;
        this.time = that.time;
        this.thickness = that.thickness;
        this.altitude = that.altitude;
        this.frame = that.frame;
        this.surface = that.surface;
        this.bottom = that.bottom;
        this.quality = that.quality;
        this.a_surface = that.a_surface;
        this.a_bottom = that.a_bottom;
        
        
    }



    public  McordsIrmcr3Record(RecordFile file)  {
        super(file);
    }



    public  McordsIrmcr3Record(RecordFile file, boolean bigEndian)  {
        super(file, bigEndian);
    }



    public int getLastAttribute()  {
        return ATTR_LAST;
    }



    public  boolean equals(Object object)  {
        if(!super.equals(object)) {System.err.println("bad super"); return false;} if(!(object instanceof McordsIrmcr3Record)) return false;
        McordsIrmcr3Record that = (McordsIrmcr3Record ) object;
        if(this.latitude!= that.latitude) {System.err.println("bad latitude");  return false;}
        if(this.longitude!= that.longitude) {System.err.println("bad longitude");  return false;}
        if(this.time!= that.time) {System.err.println("bad time");  return false;}
        if(this.thickness!= that.thickness) {System.err.println("bad thickness");  return false;}
        if(this.altitude!= that.altitude) {System.err.println("bad altitude");  return false;}
        if(this.frame!= that.frame) {System.err.println("bad frame");  return false;}
        if(this.surface!= that.surface) {System.err.println("bad surface");  return false;}
        if(this.bottom!= that.bottom) {System.err.println("bad bottom");  return false;}
        if(this.quality!= that.quality) {System.err.println("bad quality");  return false;}
        if(this.a_surface!= that.a_surface) {System.err.println("bad a_surface");  return false;}
        if(this.a_bottom!= that.a_bottom) {System.err.println("bad a_bottom");  return false;}
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
        if(attrId == ATTR_SURFACE) return surface;
        if(attrId == ATTR_BOTTOM) return bottom;
        if(attrId == ATTR_QUALITY) return quality;
        if(attrId == ATTR_A_SURFACE) return a_surface;
        if(attrId == ATTR_A_BOTTOM) return a_bottom;
        return super.getValue(attrId);
        
    }



    public int getRecordSize()  {
        return super.getRecordSize() + 84;
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
        altitude = (double) Double.parseDouble(toks[fieldCnt++]);
        frame = (double) Double.parseDouble(toks[fieldCnt++]);
        surface = (double) Double.parseDouble(toks[fieldCnt++]);
        bottom = (double) Double.parseDouble(toks[fieldCnt++]);
        quality = (int) Double.parseDouble(toks[fieldCnt++]);
        a_surface = (double) Double.parseDouble(toks[fieldCnt++]);
        a_bottom = (double) Double.parseDouble(toks[fieldCnt++]);
        
        
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
        printWriter.print(surface);
        printWriter.print(delimiter);
        printWriter.print(bottom);
        printWriter.print(delimiter);
        printWriter.print(quality);
        printWriter.print(delimiter);
        printWriter.print(a_surface);
        printWriter.print(delimiter);
        printWriter.print(a_bottom);
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
        pw.print(getStringValue(RECORDATTR_SURFACE, surface));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_BOTTOM, bottom));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_QUALITY, quality));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_A_SURFACE, a_surface));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_A_BOTTOM, a_bottom));
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
        RECORDATTR_SURFACE.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_BOTTOM.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_QUALITY.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_A_SURFACE.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_A_BOTTOM.printCsvHeader(visitInfo,pw);
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
        buff.append(" surface: " + surface+" \n");
        buff.append(" bottom: " + bottom+" \n");
        buff.append(" quality: " + quality+" \n");
        buff.append(" a_surface: " + a_surface+" \n");
        buff.append(" a_bottom: " + a_bottom+" \n");
        
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


    public double getFrame()  {
        return frame;
    }


    public void setFrame(double newValue)  {
        frame = newValue;
    }


    public double getSurface()  {
        return surface;
    }


    public void setSurface(double newValue)  {
        surface = newValue;
    }


    public double getBottom()  {
        return bottom;
    }


    public void setBottom(double newValue)  {
        bottom = newValue;
    }


    public int getQuality()  {
        return quality;
    }


    public void setQuality(int newValue)  {
        quality = newValue;
    }


    public double getA_surface()  {
        return a_surface;
    }


    public void setA_surface(double newValue)  {
        a_surface = newValue;
    }


    public double getA_bottom()  {
        return a_bottom;
    }


    public void setA_bottom(double newValue)  {
        a_bottom = newValue;
    }



}

}



