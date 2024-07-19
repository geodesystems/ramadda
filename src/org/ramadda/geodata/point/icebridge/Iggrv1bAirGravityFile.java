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
public class Iggrv1bAirGravityFile extends org.ramadda.data.point.text.TextFile {
public Iggrv1bAirGravityFile()  {}
public Iggrv1bAirGravityFile(IO.Path path) throws java.io.IOException {super(path);}
public BaseRecord doMakeRecord(VisitInfo visitInfo) {return new Iggrv1bAirGravityRecord(this);}
public static void main(String[]args) throws Exception {PointFile.test(args, Iggrv1bAirGravityFile.class);
}


@Override
public int getSkipLines(VisitInfo visitInfo) {
return  7;
}



//generated record class


public static class Iggrv1bAirGravityRecord extends org.ramadda.data.point.PointRecord {
    public static final int ATTR_FIRST = org.ramadda.data.point.PointRecord.ATTR_LAST;
    public static final List<RecordField> FIELDS = new ArrayList<RecordField>();
    public static final int ATTR_LATITUDE =  ATTR_FIRST + 1;
    public static final RecordField RECORDATTR_LATITUDE;
    public static final int ATTR_LONGITUDE =  ATTR_FIRST + 2;
    public static final RecordField RECORDATTR_LONGITUDE;
    public static final int ATTR_DATE =  ATTR_FIRST + 3;
    public static final RecordField RECORDATTR_DATE;
    public static final int ATTR_DAY =  ATTR_FIRST + 4;
    public static final RecordField RECORDATTR_DAY;
    public static final int ATTR_FLIGHT =  ATTR_FIRST + 5;
    public static final RecordField RECORDATTR_FLIGHT;
    public static final int ATTR_TIME =  ATTR_FIRST + 6;
    public static final RecordField RECORDATTR_TIME;
    public static final int ATTR_FIDUCIAL =  ATTR_FIRST + 7;
    public static final RecordField RECORDATTR_FIDUCIAL;
    public static final int ATTR_UPSX =  ATTR_FIRST + 8;
    public static final RecordField RECORDATTR_UPSX;
    public static final int ATTR_UPSY =  ATTR_FIRST + 9;
    public static final RecordField RECORDATTR_UPSY;
    public static final int ATTR_WGSHGT =  ATTR_FIRST + 10;
    public static final RecordField RECORDATTR_WGSHGT;
    public static final int ATTR_FX =  ATTR_FIRST + 11;
    public static final RecordField RECORDATTR_FX;
    public static final int ATTR_FY =  ATTR_FIRST + 12;
    public static final RecordField RECORDATTR_FY;
    public static final int ATTR_FZ =  ATTR_FIRST + 13;
    public static final RecordField RECORDATTR_FZ;
    public static final int ATTR_EOTGRAV =  ATTR_FIRST + 14;
    public static final RecordField RECORDATTR_EOTGRAV;
    public static final int ATTR_FACOR =  ATTR_FIRST + 15;
    public static final RecordField RECORDATTR_FACOR;
    public static final int ATTR_INTCOR =  ATTR_FIRST + 16;
    public static final RecordField RECORDATTR_INTCOR;
    public static final int ATTR_FAG070 =  ATTR_FIRST + 17;
    public static final RecordField RECORDATTR_FAG070;
    public static final int ATTR_FAG100 =  ATTR_FIRST + 18;
    public static final RecordField RECORDATTR_FAG100;
    public static final int ATTR_FAG140 =  ATTR_FIRST + 19;
    public static final RecordField RECORDATTR_FAG140;
    public static final int ATTR_FLTENVIRO =  ATTR_FIRST + 20;
    public static final RecordField RECORDATTR_FLTENVIRO;
    public static final int ATTR_LAST = ATTR_FIRST + 21;
    

    static {
    FIELDS.add(RECORDATTR_LATITUDE = new RecordField("latitude", "latitude", "", ATTR_LATITUDE, "degrees", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_LATITUDE.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((Iggrv1bAirGravityRecord)record).latitude;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((Iggrv1bAirGravityRecord)record).latitude;
    }
    });
    FIELDS.add(RECORDATTR_LONGITUDE = new RecordField("longitude", "longitude", "", ATTR_LONGITUDE, "degrees", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_LONGITUDE.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((Iggrv1bAirGravityRecord)record).longitude;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((Iggrv1bAirGravityRecord)record).longitude;
    }
    });
    FIELDS.add(RECORDATTR_DATE = new RecordField("date", "date", "", ATTR_DATE, "", "int", "int", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_DATE.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((Iggrv1bAirGravityRecord)record).date;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((Iggrv1bAirGravityRecord)record).date;
    }
    });
    FIELDS.add(RECORDATTR_DAY = new RecordField("day", "day", "", ATTR_DAY, "", "int", "int", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_DAY.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((Iggrv1bAirGravityRecord)record).day;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((Iggrv1bAirGravityRecord)record).day;
    }
    });
    FIELDS.add(RECORDATTR_FLIGHT = new RecordField("flight", "flight", "", ATTR_FLIGHT, "", "int", "int", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_FLIGHT.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((Iggrv1bAirGravityRecord)record).flight;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((Iggrv1bAirGravityRecord)record).flight;
    }
    });
    FIELDS.add(RECORDATTR_TIME = new RecordField("time", "time", "", ATTR_TIME, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_TIME.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((Iggrv1bAirGravityRecord)record).time;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((Iggrv1bAirGravityRecord)record).time;
    }
    });
    FIELDS.add(RECORDATTR_FIDUCIAL = new RecordField("fiducial", "fiducial", "", ATTR_FIDUCIAL, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_FIDUCIAL.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((Iggrv1bAirGravityRecord)record).fiducial;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((Iggrv1bAirGravityRecord)record).fiducial;
    }
    });
    FIELDS.add(RECORDATTR_UPSX = new RecordField("upsx", "upsx", "", ATTR_UPSX, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_UPSX.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((Iggrv1bAirGravityRecord)record).upsx;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((Iggrv1bAirGravityRecord)record).upsx;
    }
    });
    FIELDS.add(RECORDATTR_UPSY = new RecordField("upsy", "upsy", "", ATTR_UPSY, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_UPSY.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((Iggrv1bAirGravityRecord)record).upsy;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((Iggrv1bAirGravityRecord)record).upsy;
    }
    });
    FIELDS.add(RECORDATTR_WGSHGT = new RecordField("wgshgt", "wgshgt", "", ATTR_WGSHGT, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_WGSHGT.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((Iggrv1bAirGravityRecord)record).wgshgt;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((Iggrv1bAirGravityRecord)record).wgshgt;
    }
    });
    FIELDS.add(RECORDATTR_FX = new RecordField("fx", "fx", "", ATTR_FX, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_FX.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((Iggrv1bAirGravityRecord)record).fx;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((Iggrv1bAirGravityRecord)record).fx;
    }
    });
    FIELDS.add(RECORDATTR_FY = new RecordField("fy", "fy", "", ATTR_FY, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_FY.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((Iggrv1bAirGravityRecord)record).fy;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((Iggrv1bAirGravityRecord)record).fy;
    }
    });
    FIELDS.add(RECORDATTR_FZ = new RecordField("fz", "fz", "", ATTR_FZ, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_FZ.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((Iggrv1bAirGravityRecord)record).fz;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((Iggrv1bAirGravityRecord)record).fz;
    }
    });
    FIELDS.add(RECORDATTR_EOTGRAV = new RecordField("eotgrav", "eotgrav", "", ATTR_EOTGRAV, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_EOTGRAV.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((Iggrv1bAirGravityRecord)record).eotgrav;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((Iggrv1bAirGravityRecord)record).eotgrav;
    }
    });
    FIELDS.add(RECORDATTR_FACOR = new RecordField("facor", "facor", "", ATTR_FACOR, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_FACOR.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((Iggrv1bAirGravityRecord)record).facor;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((Iggrv1bAirGravityRecord)record).facor;
    }
    });
    FIELDS.add(RECORDATTR_INTCOR = new RecordField("intcor", "intcor", "", ATTR_INTCOR, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_INTCOR.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((Iggrv1bAirGravityRecord)record).intcor;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((Iggrv1bAirGravityRecord)record).intcor;
    }
    });
    FIELDS.add(RECORDATTR_FAG070 = new RecordField("fag070", "fag070", "", ATTR_FAG070, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_FAG070.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((Iggrv1bAirGravityRecord)record).fag070;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((Iggrv1bAirGravityRecord)record).fag070;
    }
    });
    FIELDS.add(RECORDATTR_FAG100 = new RecordField("fag100", "fag100", "", ATTR_FAG100, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_FAG100.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((Iggrv1bAirGravityRecord)record).fag100;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((Iggrv1bAirGravityRecord)record).fag100;
    }
    });
    FIELDS.add(RECORDATTR_FAG140 = new RecordField("fag140", "fag140", "", ATTR_FAG140, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_FAG140.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((Iggrv1bAirGravityRecord)record).fag140;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((Iggrv1bAirGravityRecord)record).fag140;
    }
    });
    FIELDS.add(RECORDATTR_FLTENVIRO = new RecordField("fltenviro", "fltenviro", "", ATTR_FLTENVIRO, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_FLTENVIRO.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((Iggrv1bAirGravityRecord)record).fltenviro;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((Iggrv1bAirGravityRecord)record).fltenviro;
    }
    });
    
    }
    

    double latitude;
    double longitude;
    int date;
    int day;
    int flight;
    double time;
    double fiducial;
    double upsx;
    double upsy;
    double wgshgt;
    double fx;
    double fy;
    double fz;
    double eotgrav;
    double facor;
    double intcor;
    double fag070;
    double fag100;
    double fag140;
    double fltenviro;
    

    public  Iggrv1bAirGravityRecord(Iggrv1bAirGravityRecord that)  {
        super(that);
        this.latitude = that.latitude;
        this.longitude = that.longitude;
        this.date = that.date;
        this.day = that.day;
        this.flight = that.flight;
        this.time = that.time;
        this.fiducial = that.fiducial;
        this.upsx = that.upsx;
        this.upsy = that.upsy;
        this.wgshgt = that.wgshgt;
        this.fx = that.fx;
        this.fy = that.fy;
        this.fz = that.fz;
        this.eotgrav = that.eotgrav;
        this.facor = that.facor;
        this.intcor = that.intcor;
        this.fag070 = that.fag070;
        this.fag100 = that.fag100;
        this.fag140 = that.fag140;
        this.fltenviro = that.fltenviro;
        
        
    }



    public  Iggrv1bAirGravityRecord(RecordFile file)  {
        super(file);
    }



    public  Iggrv1bAirGravityRecord(RecordFile file, boolean bigEndian)  {
        super(file, bigEndian);
    }



    public int getLastAttribute()  {
        return ATTR_LAST;
    }



    public  boolean equals(Object object)  {
        if(!super.equals(object)) {System.err.println("bad super"); return false;} if(!(object instanceof Iggrv1bAirGravityRecord)) return false;
        Iggrv1bAirGravityRecord that = (Iggrv1bAirGravityRecord ) object;
        if(this.latitude!= that.latitude) {System.err.println("bad latitude");  return false;}
        if(this.longitude!= that.longitude) {System.err.println("bad longitude");  return false;}
        if(this.date!= that.date) {System.err.println("bad date");  return false;}
        if(this.day!= that.day) {System.err.println("bad day");  return false;}
        if(this.flight!= that.flight) {System.err.println("bad flight");  return false;}
        if(this.time!= that.time) {System.err.println("bad time");  return false;}
        if(this.fiducial!= that.fiducial) {System.err.println("bad fiducial");  return false;}
        if(this.upsx!= that.upsx) {System.err.println("bad upsx");  return false;}
        if(this.upsy!= that.upsy) {System.err.println("bad upsy");  return false;}
        if(this.wgshgt!= that.wgshgt) {System.err.println("bad wgshgt");  return false;}
        if(this.fx!= that.fx) {System.err.println("bad fx");  return false;}
        if(this.fy!= that.fy) {System.err.println("bad fy");  return false;}
        if(this.fz!= that.fz) {System.err.println("bad fz");  return false;}
        if(this.eotgrav!= that.eotgrav) {System.err.println("bad eotgrav");  return false;}
        if(this.facor!= that.facor) {System.err.println("bad facor");  return false;}
        if(this.intcor!= that.intcor) {System.err.println("bad intcor");  return false;}
        if(this.fag070!= that.fag070) {System.err.println("bad fag070");  return false;}
        if(this.fag100!= that.fag100) {System.err.println("bad fag100");  return false;}
        if(this.fag140!= that.fag140) {System.err.println("bad fag140");  return false;}
        if(this.fltenviro!= that.fltenviro) {System.err.println("bad fltenviro");  return false;}
        return true;
    }




    protected void addFields(List<RecordField> fields)  {
        super.addFields(fields);
        fields.addAll(FIELDS);
    }



    public double getValue(int attrId)  {
        if(attrId == ATTR_LATITUDE) return latitude;
        if(attrId == ATTR_LONGITUDE) return longitude;
        if(attrId == ATTR_DATE) return date;
        if(attrId == ATTR_DAY) return day;
        if(attrId == ATTR_FLIGHT) return flight;
        if(attrId == ATTR_TIME) return time;
        if(attrId == ATTR_FIDUCIAL) return fiducial;
        if(attrId == ATTR_UPSX) return upsx;
        if(attrId == ATTR_UPSY) return upsy;
        if(attrId == ATTR_WGSHGT) return wgshgt;
        if(attrId == ATTR_FX) return fx;
        if(attrId == ATTR_FY) return fy;
        if(attrId == ATTR_FZ) return fz;
        if(attrId == ATTR_EOTGRAV) return eotgrav;
        if(attrId == ATTR_FACOR) return facor;
        if(attrId == ATTR_INTCOR) return intcor;
        if(attrId == ATTR_FAG070) return fag070;
        if(attrId == ATTR_FAG100) return fag100;
        if(attrId == ATTR_FAG140) return fag140;
        if(attrId == ATTR_FLTENVIRO) return fltenviro;
        return super.getValue(attrId);
        
    }



    public int getRecordSize()  {
        return super.getRecordSize() + 148;
    }



    public BaseRecord.ReadStatus read(RecordIO recordIO) throws Exception  {
        BaseRecord.ReadStatus status = BaseRecord.ReadStatus.OK;
        String line = recordIO.readLine();
        if(line == null) return BaseRecord.ReadStatus.EOF;
        line = line.trim();
        if(line.length()==0) return status;
        String[] toks = line.split(" +");
        int fieldCnt = 0;
        latitude = (double) Double.parseDouble(toks[fieldCnt++]);
        longitude = (double) Double.parseDouble(toks[fieldCnt++]);
        date = (int) Double.parseDouble(toks[fieldCnt++]);
        day = (int) Double.parseDouble(toks[fieldCnt++]);
        flight = (int) Double.parseDouble(toks[fieldCnt++]);
        time = (double) Double.parseDouble(toks[fieldCnt++]);
        fiducial = (double) Double.parseDouble(toks[fieldCnt++]);
        upsx = (double) Double.parseDouble(toks[fieldCnt++]);
        upsy = (double) Double.parseDouble(toks[fieldCnt++]);
        wgshgt = (double) Double.parseDouble(toks[fieldCnt++]);
        fx = (double) Double.parseDouble(toks[fieldCnt++]);
        fy = (double) Double.parseDouble(toks[fieldCnt++]);
        fz = (double) Double.parseDouble(toks[fieldCnt++]);
        eotgrav = (double) Double.parseDouble(toks[fieldCnt++]);
        facor = (double) Double.parseDouble(toks[fieldCnt++]);
        intcor = (double) Double.parseDouble(toks[fieldCnt++]);
        fag070 = (double) Double.parseDouble(toks[fieldCnt++]);
        fag100 = (double) Double.parseDouble(toks[fieldCnt++]);
        fag140 = (double) Double.parseDouble(toks[fieldCnt++]);
        fltenviro = (double) Double.parseDouble(toks[fieldCnt++]);
        
        
        return status;
    }



    public void write(RecordIO recordIO) throws IOException  {
        String delimiter = " ";
        PrintWriter  printWriter= recordIO.getPrintWriter();
        printWriter.print(latitude);
        printWriter.print(delimiter);
        printWriter.print(longitude);
        printWriter.print(delimiter);
        printWriter.print(date);
        printWriter.print(delimiter);
        printWriter.print(day);
        printWriter.print(delimiter);
        printWriter.print(flight);
        printWriter.print(delimiter);
        printWriter.print(time);
        printWriter.print(delimiter);
        printWriter.print(fiducial);
        printWriter.print(delimiter);
        printWriter.print(upsx);
        printWriter.print(delimiter);
        printWriter.print(upsy);
        printWriter.print(delimiter);
        printWriter.print(wgshgt);
        printWriter.print(delimiter);
        printWriter.print(fx);
        printWriter.print(delimiter);
        printWriter.print(fy);
        printWriter.print(delimiter);
        printWriter.print(fz);
        printWriter.print(delimiter);
        printWriter.print(eotgrav);
        printWriter.print(delimiter);
        printWriter.print(facor);
        printWriter.print(delimiter);
        printWriter.print(intcor);
        printWriter.print(delimiter);
        printWriter.print(fag070);
        printWriter.print(delimiter);
        printWriter.print(fag100);
        printWriter.print(delimiter);
        printWriter.print(fag140);
        printWriter.print(delimiter);
        printWriter.print(fltenviro);
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
        pw.print(getStringValue(RECORDATTR_DATE, date));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_DAY, day));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_FLIGHT, flight));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_TIME, time));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_FIDUCIAL, fiducial));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_UPSX, upsx));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_UPSY, upsy));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_WGSHGT, wgshgt));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_FX, fx));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_FY, fy));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_FZ, fz));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_EOTGRAV, eotgrav));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_FACOR, facor));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_INTCOR, intcor));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_FAG070, fag070));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_FAG100, fag100));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_FAG140, fag140));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_FLTENVIRO, fltenviro));
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
        RECORDATTR_DATE.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_DAY.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_FLIGHT.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_TIME.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_FIDUCIAL.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_UPSX.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_UPSY.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_WGSHGT.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_FX.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_FY.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_FZ.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_EOTGRAV.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_FACOR.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_INTCOR.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_FAG070.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_FAG100.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_FAG140.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_FLTENVIRO.printCsvHeader(visitInfo,pw);
        myCnt++;
        return myCnt+superCnt;
        
    }



    public void print(Appendable buff)  throws Exception  {
        super.print(buff);
        buff.append(" latitude: " + latitude+" \n");
        buff.append(" longitude: " + longitude+" \n");
        buff.append(" date: " + date+" \n");
        buff.append(" day: " + day+" \n");
        buff.append(" flight: " + flight+" \n");
        buff.append(" time: " + time+" \n");
        buff.append(" fiducial: " + fiducial+" \n");
        buff.append(" upsx: " + upsx+" \n");
        buff.append(" upsy: " + upsy+" \n");
        buff.append(" wgshgt: " + wgshgt+" \n");
        buff.append(" fx: " + fx+" \n");
        buff.append(" fy: " + fy+" \n");
        buff.append(" fz: " + fz+" \n");
        buff.append(" eotgrav: " + eotgrav+" \n");
        buff.append(" facor: " + facor+" \n");
        buff.append(" intcor: " + intcor+" \n");
        buff.append(" fag070: " + fag070+" \n");
        buff.append(" fag100: " + fag100+" \n");
        buff.append(" fag140: " + fag140+" \n");
        buff.append(" fltenviro: " + fltenviro+" \n");
        
    }



    public double getLatitude()  {
        return latitude;
    }


    public void setLatitude(double newValue)  {
        latitude = newValue;
    }


    public double getLongitude()  {
        return longitude;
    }


    public void setLongitude(double newValue)  {
        longitude = newValue;
    }


    public int getIntDate()  {
        return date;
    }


    public void setIntDate(int newValue)  {
        date = newValue;
    }


    public int getDay()  {
        return day;
    }


    public void setDay(int newValue)  {
        day = newValue;
    }


    public int getFlight()  {
        return flight;
    }


    public void setFlight(int newValue)  {
        flight = newValue;
    }


    public double getTime()  {
        return time;
    }


    public void setTime(double newValue)  {
        time = newValue;
    }


    public double getFiducial()  {
        return fiducial;
    }


    public void setFiducial(double newValue)  {
        fiducial = newValue;
    }


    public double getUpsx()  {
        return upsx;
    }


    public void setUpsx(double newValue)  {
        upsx = newValue;
    }


    public double getUpsy()  {
        return upsy;
    }


    public void setUpsy(double newValue)  {
        upsy = newValue;
    }


    public double getWgshgt()  {
        return wgshgt;
    }


    public void setWgshgt(double newValue)  {
        wgshgt = newValue;
    }


    public double getFx()  {
        return fx;
    }


    public void setFx(double newValue)  {
        fx = newValue;
    }


    public double getFy()  {
        return fy;
    }


    public void setFy(double newValue)  {
        fy = newValue;
    }


    public double getFz()  {
        return fz;
    }


    public void setFz(double newValue)  {
        fz = newValue;
    }


    public double getEotgrav()  {
        return eotgrav;
    }


    public void setEotgrav(double newValue)  {
        eotgrav = newValue;
    }


    public double getFacor()  {
        return facor;
    }


    public void setFacor(double newValue)  {
        facor = newValue;
    }


    public double getIntcor()  {
        return intcor;
    }


    public void setIntcor(double newValue)  {
        intcor = newValue;
    }


    public double getFag070()  {
        return fag070;
    }


    public void setFag070(double newValue)  {
        fag070 = newValue;
    }


    public double getFag100()  {
        return fag100;
    }


    public void setFag100(double newValue)  {
        fag100 = newValue;
    }


    public double getFag140()  {
        return fag140;
    }


    public void setFag140(double newValue)  {
        fag140 = newValue;
    }


    public double getFltenviro()  {
        return fltenviro;
    }


    public void setFltenviro(double newValue)  {
        fltenviro = newValue;
    }



}

}



