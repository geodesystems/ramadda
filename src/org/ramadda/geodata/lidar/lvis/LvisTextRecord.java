
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



package org.ramadda.geodata.lidar.lvis;

import org.ramadda.data.record.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import org.ramadda.data.point.Waveform;
import ucar.unidata.util.Misc;





/** This is generated code from generate.tcl. Do not edit it! */
public class LvisTextRecord extends org.ramadda.geodata.lidar.lvis.LvisRecord {
    public static final int ATTR_FIRST = org.ramadda.geodata.lidar.lvis.LvisRecord.ATTR_LAST;
    public static final List<RecordField> FIELDS = new ArrayList<RecordField>();
    public static final int ATTR_LFID =  ATTR_FIRST + 1;
    public static final RecordField RECORDATTR_LFID;
    public static final int ATTR_SHOTNUMBER =  ATTR_FIRST + 2;
    public static final RecordField RECORDATTR_SHOTNUMBER;
    public static final int ATTR_LVISTIME =  ATTR_FIRST + 3;
    public static final RecordField RECORDATTR_LVISTIME;
    public static final int ATTR_LONGITUDE_CENTROID =  ATTR_FIRST + 4;
    public static final RecordField RECORDATTR_LONGITUDE_CENTROID;
    public static final int ATTR_LATITUDE_CENTROID =  ATTR_FIRST + 5;
    public static final RecordField RECORDATTR_LATITUDE_CENTROID;
    public static final int ATTR_ELEVATION_CENTROID =  ATTR_FIRST + 6;
    public static final RecordField RECORDATTR_ELEVATION_CENTROID;
    public static final int ATTR_LONGITUDE_LOW =  ATTR_FIRST + 7;
    public static final RecordField RECORDATTR_LONGITUDE_LOW;
    public static final int ATTR_LATITUDE_LOW =  ATTR_FIRST + 8;
    public static final RecordField RECORDATTR_LATITUDE_LOW;
    public static final int ATTR_ELEVATION_LOW =  ATTR_FIRST + 9;
    public static final RecordField RECORDATTR_ELEVATION_LOW;
    public static final int ATTR_LONGITUDE_HIGH =  ATTR_FIRST + 10;
    public static final RecordField RECORDATTR_LONGITUDE_HIGH;
    public static final int ATTR_LATITUDE_HIGH =  ATTR_FIRST + 11;
    public static final RecordField RECORDATTR_LATITUDE_HIGH;
    public static final int ATTR_ELEVATION_HIGH =  ATTR_FIRST + 12;
    public static final RecordField RECORDATTR_ELEVATION_HIGH;
    public static final int ATTR_LAST = ATTR_FIRST + 13;
    

    static {
    FIELDS.add(RECORDATTR_LFID = new RecordField("lfid", "lfid", "", ATTR_LFID, "", "int", "int", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_LFID.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LvisTextRecord)record).lfid;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LvisTextRecord)record).lfid;
    }
    });
    FIELDS.add(RECORDATTR_SHOTNUMBER = new RecordField("SHOTNUMBER", "SHOTNUMBER", "", ATTR_SHOTNUMBER, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_SHOTNUMBER.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LvisTextRecord)record).SHOTNUMBER;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LvisTextRecord)record).SHOTNUMBER;
    }
    });
    FIELDS.add(RECORDATTR_LVISTIME = new RecordField("lvisTime", "lvisTime", "", ATTR_LVISTIME, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_LVISTIME.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LvisTextRecord)record).lvisTime;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LvisTextRecord)record).lvisTime;
    }
    });
    FIELDS.add(RECORDATTR_LONGITUDE_CENTROID = new RecordField("LONGITUDE_CENTROID", "LONGITUDE_CENTROID", "", ATTR_LONGITUDE_CENTROID, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_LONGITUDE_CENTROID.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LvisTextRecord)record).LONGITUDE_CENTROID;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LvisTextRecord)record).LONGITUDE_CENTROID;
    }
    });
    FIELDS.add(RECORDATTR_LATITUDE_CENTROID = new RecordField("LATITUDE_CENTROID", "LATITUDE_CENTROID", "", ATTR_LATITUDE_CENTROID, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_LATITUDE_CENTROID.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LvisTextRecord)record).LATITUDE_CENTROID;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LvisTextRecord)record).LATITUDE_CENTROID;
    }
    });
    FIELDS.add(RECORDATTR_ELEVATION_CENTROID = new RecordField("ELEVATION_CENTROID", "ELEVATION_CENTROID", "", ATTR_ELEVATION_CENTROID, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_ELEVATION_CENTROID.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LvisTextRecord)record).ELEVATION_CENTROID;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LvisTextRecord)record).ELEVATION_CENTROID;
    }
    });
    FIELDS.add(RECORDATTR_LONGITUDE_LOW = new RecordField("LONGITUDE_LOW", "LONGITUDE_LOW", "", ATTR_LONGITUDE_LOW, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_LONGITUDE_LOW.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LvisTextRecord)record).LONGITUDE_LOW;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LvisTextRecord)record).LONGITUDE_LOW;
    }
    });
    FIELDS.add(RECORDATTR_LATITUDE_LOW = new RecordField("LATITUDE_LOW", "LATITUDE_LOW", "", ATTR_LATITUDE_LOW, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_LATITUDE_LOW.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LvisTextRecord)record).LATITUDE_LOW;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LvisTextRecord)record).LATITUDE_LOW;
    }
    });
    FIELDS.add(RECORDATTR_ELEVATION_LOW = new RecordField("ELEVATION_LOW", "ELEVATION_LOW", "", ATTR_ELEVATION_LOW, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_ELEVATION_LOW.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LvisTextRecord)record).ELEVATION_LOW;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LvisTextRecord)record).ELEVATION_LOW;
    }
    });
    FIELDS.add(RECORDATTR_LONGITUDE_HIGH = new RecordField("LONGITUDE_HIGH", "LONGITUDE_HIGH", "", ATTR_LONGITUDE_HIGH, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_LONGITUDE_HIGH.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LvisTextRecord)record).LONGITUDE_HIGH;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LvisTextRecord)record).LONGITUDE_HIGH;
    }
    });
    FIELDS.add(RECORDATTR_LATITUDE_HIGH = new RecordField("LATITUDE_HIGH", "LATITUDE_HIGH", "", ATTR_LATITUDE_HIGH, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_LATITUDE_HIGH.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LvisTextRecord)record).LATITUDE_HIGH;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LvisTextRecord)record).LATITUDE_HIGH;
    }
    });
    FIELDS.add(RECORDATTR_ELEVATION_HIGH = new RecordField("ELEVATION_HIGH", "ELEVATION_HIGH", "", ATTR_ELEVATION_HIGH, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_ELEVATION_HIGH.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LvisTextRecord)record).ELEVATION_HIGH;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LvisTextRecord)record).ELEVATION_HIGH;
    }
    });
    
    }
    

    int lfid;
    double SHOTNUMBER;
    double LONGITUDE_CENTROID;
    double LATITUDE_CENTROID;
    double ELEVATION_CENTROID;
    double LONGITUDE_LOW;
    double LATITUDE_LOW;
    double ELEVATION_LOW;
    double LONGITUDE_HIGH;
    double LATITUDE_HIGH;
    double ELEVATION_HIGH;
    

    public  LvisTextRecord(LvisTextRecord that)  {
        super(that);
        this.lfid = that.lfid;
        this.SHOTNUMBER = that.SHOTNUMBER;
        this.lvisTime = that.lvisTime;
        this.LONGITUDE_CENTROID = that.LONGITUDE_CENTROID;
        this.LATITUDE_CENTROID = that.LATITUDE_CENTROID;
        this.ELEVATION_CENTROID = that.ELEVATION_CENTROID;
        this.LONGITUDE_LOW = that.LONGITUDE_LOW;
        this.LATITUDE_LOW = that.LATITUDE_LOW;
        this.ELEVATION_LOW = that.ELEVATION_LOW;
        this.LONGITUDE_HIGH = that.LONGITUDE_HIGH;
        this.LATITUDE_HIGH = that.LATITUDE_HIGH;
        this.ELEVATION_HIGH = that.ELEVATION_HIGH;
        
        
    }



    public  LvisTextRecord(RecordFile file)  {
        super(file);
    }



    public  LvisTextRecord(RecordFile file, boolean bigEndian)  {
        super(file, bigEndian);
    }



    public int getLastAttribute()  {
        return ATTR_LAST;
    }



    public  boolean equals(Object object)  {
        if(!super.equals(object)) {System.err.println("bad super"); return false;} if(!(object instanceof LvisTextRecord)) return false;
        LvisTextRecord that = (LvisTextRecord ) object;
        if(this.lfid!= that.lfid) {System.err.println("bad lfid");  return false;}
        if(this.SHOTNUMBER!= that.SHOTNUMBER) {System.err.println("bad SHOTNUMBER");  return false;}
        if(this.lvisTime!= that.lvisTime) {System.err.println("bad lvisTime");  return false;}
        if(this.LONGITUDE_CENTROID!= that.LONGITUDE_CENTROID) {System.err.println("bad LONGITUDE_CENTROID");  return false;}
        if(this.LATITUDE_CENTROID!= that.LATITUDE_CENTROID) {System.err.println("bad LATITUDE_CENTROID");  return false;}
        if(this.ELEVATION_CENTROID!= that.ELEVATION_CENTROID) {System.err.println("bad ELEVATION_CENTROID");  return false;}
        if(this.LONGITUDE_LOW!= that.LONGITUDE_LOW) {System.err.println("bad LONGITUDE_LOW");  return false;}
        if(this.LATITUDE_LOW!= that.LATITUDE_LOW) {System.err.println("bad LATITUDE_LOW");  return false;}
        if(this.ELEVATION_LOW!= that.ELEVATION_LOW) {System.err.println("bad ELEVATION_LOW");  return false;}
        if(this.LONGITUDE_HIGH!= that.LONGITUDE_HIGH) {System.err.println("bad LONGITUDE_HIGH");  return false;}
        if(this.LATITUDE_HIGH!= that.LATITUDE_HIGH) {System.err.println("bad LATITUDE_HIGH");  return false;}
        if(this.ELEVATION_HIGH!= that.ELEVATION_HIGH) {System.err.println("bad ELEVATION_HIGH");  return false;}
        return true;
    }




    public double getLatitude() {
        return LATITUDE_CENTROID;
    }
    public double getLongitude() {
        return org.ramadda.util.GeoUtils.normalizeLongitude(LONGITUDE_CENTROID);
    }
    public double getAltitude() {
        return ELEVATION_CENTROID;
    }


    protected void addFields(List<RecordField> fields)  {
        super.addFields(fields);
        fields.addAll(FIELDS);
    }



    public double getValue(int attrId)  {
        if(attrId == ATTR_LFID) return lfid;
        if(attrId == ATTR_SHOTNUMBER) return SHOTNUMBER;
        if(attrId == ATTR_LVISTIME) return lvisTime;
        if(attrId == ATTR_LONGITUDE_CENTROID) return LONGITUDE_CENTROID;
        if(attrId == ATTR_LATITUDE_CENTROID) return LATITUDE_CENTROID;
        if(attrId == ATTR_ELEVATION_CENTROID) return ELEVATION_CENTROID;
        if(attrId == ATTR_LONGITUDE_LOW) return LONGITUDE_LOW;
        if(attrId == ATTR_LATITUDE_LOW) return LATITUDE_LOW;
        if(attrId == ATTR_ELEVATION_LOW) return ELEVATION_LOW;
        if(attrId == ATTR_LONGITUDE_HIGH) return LONGITUDE_HIGH;
        if(attrId == ATTR_LATITUDE_HIGH) return LATITUDE_HIGH;
        if(attrId == ATTR_ELEVATION_HIGH) return ELEVATION_HIGH;
        return super.getValue(attrId);
        
    }



    public int getRecordSize()  {
        return super.getRecordSize() + 92;
    }



    public BaseRecord.ReadStatus read(RecordIO recordIO) throws Exception  {
        BaseRecord.ReadStatus status = BaseRecord.ReadStatus.OK;
        String line = recordIO.readLine();
        if(line == null) return BaseRecord.ReadStatus.EOF;
        line = line.trim();
        if(line.length()==0) return status;
        String[] toks = line.split(" +");
        int fieldCnt = 0;
        lfid = (int) Double.parseDouble(toks[fieldCnt++]);
        SHOTNUMBER = (double) Double.parseDouble(toks[fieldCnt++]);
        lvisTime = (double) Double.parseDouble(toks[fieldCnt++]);
        LONGITUDE_CENTROID = (double) Double.parseDouble(toks[fieldCnt++]);
        LATITUDE_CENTROID = (double) Double.parseDouble(toks[fieldCnt++]);
        ELEVATION_CENTROID = (double) Double.parseDouble(toks[fieldCnt++]);
        LONGITUDE_LOW = (double) Double.parseDouble(toks[fieldCnt++]);
        LATITUDE_LOW = (double) Double.parseDouble(toks[fieldCnt++]);
        ELEVATION_LOW = (double) Double.parseDouble(toks[fieldCnt++]);
        LONGITUDE_HIGH = (double) Double.parseDouble(toks[fieldCnt++]);
        LATITUDE_HIGH = (double) Double.parseDouble(toks[fieldCnt++]);
        ELEVATION_HIGH = (double) Double.parseDouble(toks[fieldCnt++]);
        
        
        return status;
    }



    public void write(RecordIO recordIO) throws IOException  {
        String delimiter = " ";
        PrintWriter  printWriter= recordIO.getPrintWriter();
        printWriter.print(lfid);
        printWriter.print(delimiter);
        printWriter.print(SHOTNUMBER);
        printWriter.print(delimiter);
        printWriter.print(lvisTime);
        printWriter.print(delimiter);
        printWriter.print(LONGITUDE_CENTROID);
        printWriter.print(delimiter);
        printWriter.print(LATITUDE_CENTROID);
        printWriter.print(delimiter);
        printWriter.print(ELEVATION_CENTROID);
        printWriter.print(delimiter);
        printWriter.print(LONGITUDE_LOW);
        printWriter.print(delimiter);
        printWriter.print(LATITUDE_LOW);
        printWriter.print(delimiter);
        printWriter.print(ELEVATION_LOW);
        printWriter.print(delimiter);
        printWriter.print(LONGITUDE_HIGH);
        printWriter.print(delimiter);
        printWriter.print(LATITUDE_HIGH);
        printWriter.print(delimiter);
        printWriter.print(ELEVATION_HIGH);
        printWriter.print("\n");
        
    }



    public int doPrintCsv(VisitInfo visitInfo,PrintWriter pw)  {
        boolean includeVector = visitInfo.getProperty(PROP_INCLUDEVECTOR, false);
        int superCnt = super.doPrintCsv(visitInfo,pw);
        int myCnt = 0;
        if(superCnt>0) pw.print(',');
        pw.print(getStringValue(RECORDATTR_LFID, lfid));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_SHOTNUMBER, SHOTNUMBER));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_LVISTIME, lvisTime));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_LONGITUDE_CENTROID, LONGITUDE_CENTROID));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_LATITUDE_CENTROID, LATITUDE_CENTROID));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_ELEVATION_CENTROID, ELEVATION_CENTROID));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_LONGITUDE_LOW, LONGITUDE_LOW));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_LATITUDE_LOW, LATITUDE_LOW));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_ELEVATION_LOW, ELEVATION_LOW));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_LONGITUDE_HIGH, LONGITUDE_HIGH));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_LATITUDE_HIGH, LATITUDE_HIGH));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_ELEVATION_HIGH, ELEVATION_HIGH));
        myCnt++;
        return myCnt+superCnt;
        
    }



    public int doPrintCsvHeader(VisitInfo visitInfo,PrintWriter pw)  {
        int superCnt = super.doPrintCsvHeader(visitInfo,pw);
        int myCnt = 0;
        boolean includeVector = visitInfo.getProperty(PROP_INCLUDEVECTOR, false);
        if(superCnt>0) pw.print(',');
        RECORDATTR_LFID.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_SHOTNUMBER.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_LVISTIME.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_LONGITUDE_CENTROID.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_LATITUDE_CENTROID.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_ELEVATION_CENTROID.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_LONGITUDE_LOW.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_LATITUDE_LOW.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_ELEVATION_LOW.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_LONGITUDE_HIGH.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_LATITUDE_HIGH.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_ELEVATION_HIGH.printCsvHeader(visitInfo,pw);
        myCnt++;
        return myCnt+superCnt;
        
    }



    public void print(Appendable buff)  throws Exception  {
        super.print(buff);
        buff.append(" lfid: " + lfid+" \n");
        buff.append(" SHOTNUMBER: " + SHOTNUMBER+" \n");
        buff.append(" lvisTime: " + lvisTime+" \n");
        buff.append(" LONGITUDE_CENTROID: " + LONGITUDE_CENTROID+" \n");
        buff.append(" LATITUDE_CENTROID: " + LATITUDE_CENTROID+" \n");
        buff.append(" ELEVATION_CENTROID: " + ELEVATION_CENTROID+" \n");
        buff.append(" LONGITUDE_LOW: " + LONGITUDE_LOW+" \n");
        buff.append(" LATITUDE_LOW: " + LATITUDE_LOW+" \n");
        buff.append(" ELEVATION_LOW: " + ELEVATION_LOW+" \n");
        buff.append(" LONGITUDE_HIGH: " + LONGITUDE_HIGH+" \n");
        buff.append(" LATITUDE_HIGH: " + LATITUDE_HIGH+" \n");
        buff.append(" ELEVATION_HIGH: " + ELEVATION_HIGH+" \n");
        
    }



    public int getLfid()  {
        return lfid;
    }


    public void setLfid(int newValue)  {
        lfid = newValue;
    }


    public double getSHOTNUMBER()  {
        return SHOTNUMBER;
    }


    public void setSHOTNUMBER(double newValue)  {
        SHOTNUMBER = newValue;
    }


    public double getLONGITUDE_CENTROID()  {
        return LONGITUDE_CENTROID;
    }


    public void setLONGITUDE_CENTROID(double newValue)  {
        LONGITUDE_CENTROID = newValue;
    }


    public double getLATITUDE_CENTROID()  {
        return LATITUDE_CENTROID;
    }


    public void setLATITUDE_CENTROID(double newValue)  {
        LATITUDE_CENTROID = newValue;
    }


    public double getELEVATION_CENTROID()  {
        return ELEVATION_CENTROID;
    }


    public void setELEVATION_CENTROID(double newValue)  {
        ELEVATION_CENTROID = newValue;
    }


    public double getLONGITUDE_LOW()  {
        return LONGITUDE_LOW;
    }


    public void setLONGITUDE_LOW(double newValue)  {
        LONGITUDE_LOW = newValue;
    }


    public double getLATITUDE_LOW()  {
        return LATITUDE_LOW;
    }


    public void setLATITUDE_LOW(double newValue)  {
        LATITUDE_LOW = newValue;
    }


    public double getELEVATION_LOW()  {
        return ELEVATION_LOW;
    }


    public void setELEVATION_LOW(double newValue)  {
        ELEVATION_LOW = newValue;
    }


    public double getLONGITUDE_HIGH()  {
        return LONGITUDE_HIGH;
    }


    public void setLONGITUDE_HIGH(double newValue)  {
        LONGITUDE_HIGH = newValue;
    }


    public double getLATITUDE_HIGH()  {
        return LATITUDE_HIGH;
    }


    public void setLATITUDE_HIGH(double newValue)  {
        LATITUDE_HIGH = newValue;
    }


    public double getELEVATION_HIGH()  {
        return ELEVATION_HIGH;
    }


    public void setELEVATION_HIGH(double newValue)  {
        ELEVATION_HIGH = newValue;
    }



}



