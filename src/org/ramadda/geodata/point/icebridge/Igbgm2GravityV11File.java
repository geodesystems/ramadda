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
public class Igbgm2GravityV11File extends org.ramadda.data.point.text.TextFile {
public Igbgm2GravityV11File()  {}
public Igbgm2GravityV11File(IO.Path path) throws java.io.IOException {super(path);}
public BaseRecord doMakeRecord(VisitInfo visitInfo) {return new Igbgm2GravityV11Record(this);}
public static void main(String[]args) throws Exception {PointFile.test(args, Igbgm2GravityV11File.class);
}


//generated record class


public static class Igbgm2GravityV11Record extends org.ramadda.data.point.PointRecord {
    public static final int ATTR_FIRST = org.ramadda.data.point.PointRecord.ATTR_LAST;
    public static final List<RecordField> FIELDS = new ArrayList<RecordField>();
    public static final int ATTR_YEAR =  ATTR_FIRST + 1;
    public static final RecordField RECORDATTR_YEAR;
    public static final int ATTR_DAYOFYEAR =  ATTR_FIRST + 2;
    public static final RecordField RECORDATTR_DAYOFYEAR;
    public static final int ATTR_SECONDOFDAY =  ATTR_FIRST + 3;
    public static final RecordField RECORDATTR_SECONDOFDAY;
    public static final int ATTR_LONGITUDE =  ATTR_FIRST + 4;
    public static final RecordField RECORDATTR_LONGITUDE;
    public static final int ATTR_LATITUDE =  ATTR_FIRST + 5;
    public static final RecordField RECORDATTR_LATITUDE;
    public static final int ATTR_AIRCRAFTHEIGHT =  ATTR_FIRST + 6;
    public static final RecordField RECORDATTR_AIRCRAFTHEIGHT;
    public static final int ATTR_FREEAIRGRAVITYDISTURBANCE =  ATTR_FIRST + 7;
    public static final RecordField RECORDATTR_FREEAIRGRAVITYDISTURBANCE;
    public static final int ATTR_LAST = ATTR_FIRST + 8;
    

    static {
    FIELDS.add(RECORDATTR_YEAR = new RecordField("year", "year", "", ATTR_YEAR, "", "int", "int", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_YEAR.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((Igbgm2GravityV11Record)record).year;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((Igbgm2GravityV11Record)record).year;
    }
    });
    FIELDS.add(RECORDATTR_DAYOFYEAR = new RecordField("dayOfYear", "dayOfYear", "", ATTR_DAYOFYEAR, "", "int", "int", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_DAYOFYEAR.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((Igbgm2GravityV11Record)record).dayOfYear;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((Igbgm2GravityV11Record)record).dayOfYear;
    }
    });
    FIELDS.add(RECORDATTR_SECONDOFDAY = new RecordField("secondOfDay", "secondOfDay", "", ATTR_SECONDOFDAY, "", "int", "int", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_SECONDOFDAY.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((Igbgm2GravityV11Record)record).secondOfDay;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((Igbgm2GravityV11Record)record).secondOfDay;
    }
    });
    FIELDS.add(RECORDATTR_LONGITUDE = new RecordField("longitude", "longitude", "", ATTR_LONGITUDE, "degrees", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_LONGITUDE.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((Igbgm2GravityV11Record)record).longitude;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((Igbgm2GravityV11Record)record).longitude;
    }
    });
    FIELDS.add(RECORDATTR_LATITUDE = new RecordField("latitude", "latitude", "", ATTR_LATITUDE, "degrees", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_LATITUDE.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((Igbgm2GravityV11Record)record).latitude;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((Igbgm2GravityV11Record)record).latitude;
    }
    });
    FIELDS.add(RECORDATTR_AIRCRAFTHEIGHT = new RecordField("aircraftHeight", "aircraftHeight", "", ATTR_AIRCRAFTHEIGHT, "m", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_YES));
    RECORDATTR_AIRCRAFTHEIGHT.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((Igbgm2GravityV11Record)record).aircraftHeight;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((Igbgm2GravityV11Record)record).aircraftHeight;
    }
    });
    FIELDS.add(RECORDATTR_FREEAIRGRAVITYDISTURBANCE = new RecordField("freeAirGravityDisturbance", "freeAirGravityDisturbance", "", ATTR_FREEAIRGRAVITYDISTURBANCE, "mGal", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_YES));
    RECORDATTR_FREEAIRGRAVITYDISTURBANCE.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((Igbgm2GravityV11Record)record).freeAirGravityDisturbance;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((Igbgm2GravityV11Record)record).freeAirGravityDisturbance;
    }
    });
    
    }
    

    int year;
    int dayOfYear;
    int secondOfDay;
    double longitude;
    double latitude;
    double aircraftHeight;
    double freeAirGravityDisturbance;
    

    public  Igbgm2GravityV11Record(Igbgm2GravityV11Record that)  {
        super(that);
        this.year = that.year;
        this.dayOfYear = that.dayOfYear;
        this.secondOfDay = that.secondOfDay;
        this.longitude = that.longitude;
        this.latitude = that.latitude;
        this.aircraftHeight = that.aircraftHeight;
        this.freeAirGravityDisturbance = that.freeAirGravityDisturbance;
        
        
    }



    public  Igbgm2GravityV11Record(RecordFile file)  {
        super(file);
    }



    public  Igbgm2GravityV11Record(RecordFile file, boolean bigEndian)  {
        super(file, bigEndian);
    }



    public int getLastAttribute()  {
        return ATTR_LAST;
    }



    public  boolean equals(Object object)  {
        if(!super.equals(object)) {System.err.println("bad super"); return false;} if(!(object instanceof Igbgm2GravityV11Record)) return false;
        Igbgm2GravityV11Record that = (Igbgm2GravityV11Record ) object;
        if(this.year!= that.year) {System.err.println("bad year");  return false;}
        if(this.dayOfYear!= that.dayOfYear) {System.err.println("bad dayOfYear");  return false;}
        if(this.secondOfDay!= that.secondOfDay) {System.err.println("bad secondOfDay");  return false;}
        if(this.longitude!= that.longitude) {System.err.println("bad longitude");  return false;}
        if(this.latitude!= that.latitude) {System.err.println("bad latitude");  return false;}
        if(this.aircraftHeight!= that.aircraftHeight) {System.err.println("bad aircraftHeight");  return false;}
        if(this.freeAirGravityDisturbance!= that.freeAirGravityDisturbance) {System.err.println("bad freeAirGravityDisturbance");  return false;}
        return true;
    }




    protected void addFields(List<RecordField> fields)  {
        super.addFields(fields);
        fields.addAll(FIELDS);
    }



    public double getValue(int attrId)  {
        if(attrId == ATTR_YEAR) return year;
        if(attrId == ATTR_DAYOFYEAR) return dayOfYear;
        if(attrId == ATTR_SECONDOFDAY) return secondOfDay;
        if(attrId == ATTR_LONGITUDE) return longitude;
        if(attrId == ATTR_LATITUDE) return latitude;
        if(attrId == ATTR_AIRCRAFTHEIGHT) return aircraftHeight;
        if(attrId == ATTR_FREEAIRGRAVITYDISTURBANCE) return freeAirGravityDisturbance;
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
        year = (int) Double.parseDouble(toks[fieldCnt++]);
        dayOfYear = (int) Double.parseDouble(toks[fieldCnt++]);
        secondOfDay = (int) Double.parseDouble(toks[fieldCnt++]);
        longitude = (double) Double.parseDouble(toks[fieldCnt++]);
        latitude = (double) Double.parseDouble(toks[fieldCnt++]);
        aircraftHeight = (double) Double.parseDouble(toks[fieldCnt++]);
        freeAirGravityDisturbance = (double) Double.parseDouble(toks[fieldCnt++]);
        
        
        return status;
    }



    public void write(RecordIO recordIO) throws IOException  {
        String delimiter = " ";
        PrintWriter  printWriter= recordIO.getPrintWriter();
        printWriter.print(year);
        printWriter.print(delimiter);
        printWriter.print(dayOfYear);
        printWriter.print(delimiter);
        printWriter.print(secondOfDay);
        printWriter.print(delimiter);
        printWriter.print(longitude);
        printWriter.print(delimiter);
        printWriter.print(latitude);
        printWriter.print(delimiter);
        printWriter.print(aircraftHeight);
        printWriter.print(delimiter);
        printWriter.print(freeAirGravityDisturbance);
        printWriter.print("\n");
        
    }



    public int doPrintCsv(VisitInfo visitInfo,PrintWriter pw)  {
        boolean includeVector = visitInfo.getProperty(PROP_INCLUDEVECTOR, false);
        int superCnt = super.doPrintCsv(visitInfo,pw);
        int myCnt = 0;
        if(superCnt>0) pw.print(',');
        pw.print(getStringValue(RECORDATTR_YEAR, year));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_DAYOFYEAR, dayOfYear));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_SECONDOFDAY, secondOfDay));
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
        pw.print(getStringValue(RECORDATTR_FREEAIRGRAVITYDISTURBANCE, freeAirGravityDisturbance));
        myCnt++;
        return myCnt+superCnt;
        
    }



    public int doPrintCsvHeader(VisitInfo visitInfo,PrintWriter pw)  {
        int superCnt = super.doPrintCsvHeader(visitInfo,pw);
        int myCnt = 0;
        boolean includeVector = visitInfo.getProperty(PROP_INCLUDEVECTOR, false);
        if(superCnt>0) pw.print(',');
        RECORDATTR_YEAR.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_DAYOFYEAR.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_SECONDOFDAY.printCsvHeader(visitInfo,pw);
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
        RECORDATTR_FREEAIRGRAVITYDISTURBANCE.printCsvHeader(visitInfo,pw);
        myCnt++;
        return myCnt+superCnt;
        
    }



    public void print(Appendable buff)  throws Exception  {
        super.print(buff);
        buff.append(" year: " + year+" \n");
        buff.append(" dayOfYear: " + dayOfYear+" \n");
        buff.append(" secondOfDay: " + secondOfDay+" \n");
        buff.append(" longitude: " + longitude+" \n");
        buff.append(" latitude: " + latitude+" \n");
        buff.append(" aircraftHeight: " + aircraftHeight+" \n");
        buff.append(" freeAirGravityDisturbance: " + freeAirGravityDisturbance+" \n");
        
    }



    public int getYear()  {
        return year;
    }


    public void setYear(int newValue)  {
        year = newValue;
    }


    public int getDayOfYear()  {
        return dayOfYear;
    }


    public void setDayOfYear(int newValue)  {
        dayOfYear = newValue;
    }


    public int getSecondOfDay()  {
        return secondOfDay;
    }


    public void setSecondOfDay(int newValue)  {
        secondOfDay = newValue;
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


    public double getFreeAirGravityDisturbance()  {
        return freeAirGravityDisturbance;
    }


    public void setFreeAirGravityDisturbance(double newValue)  {
        freeAirGravityDisturbance = newValue;
    }



}

}



