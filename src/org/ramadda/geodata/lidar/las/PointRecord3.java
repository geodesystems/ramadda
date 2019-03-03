
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



package org.ramadda.geodata.lidar.las;

import org.ramadda.data.record.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import org.ramadda.geodata.lidar.*;





/** This is generated code from generate.tcl. Do not edit it! */
public class PointRecord3 extends PointRecord0 {
    public static final int ATTR_FIRST = PointRecord0.ATTR_LAST;
    public static final List<RecordField> FIELDS = new ArrayList<RecordField>();
    public static final int ATTR_GPSTIME =  ATTR_FIRST + 1;
    public static final RecordField RECORDATTR_GPSTIME;
    public static final int ATTR_RED =  ATTR_FIRST + 2;
    public static final RecordField RECORDATTR_RED;
    public static final int ATTR_GREEN =  ATTR_FIRST + 3;
    public static final RecordField RECORDATTR_GREEN;
    public static final int ATTR_BLUE =  ATTR_FIRST + 4;
    public static final RecordField RECORDATTR_BLUE;
    public static final int ATTR_LAST = ATTR_FIRST + 5;
    

    static {
    FIELDS.add(RECORDATTR_GPSTIME = new RecordField("gpsTime", "gpsTime", "", ATTR_GPSTIME, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_GPSTIME.setValueGetter(new ValueGetter() {
    public double getValue(Record record, RecordField field, VisitInfo visitInfo) {
    return (double) ((PointRecord3)record).gpsTime;
    }
    public String getStringValue(Record record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((PointRecord3)record).gpsTime;
    }
    });
    FIELDS.add(RECORDATTR_RED = new RecordField("red", "red", "", ATTR_RED, "", "ushort", "short", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_RED.setValueGetter(new ValueGetter() {
    public double getValue(Record record, RecordField field, VisitInfo visitInfo) {
    return (double) ((PointRecord3)record).red;
    }
    public String getStringValue(Record record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((PointRecord3)record).red;
    }
    });
    FIELDS.add(RECORDATTR_GREEN = new RecordField("green", "green", "", ATTR_GREEN, "", "ushort", "short", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_GREEN.setValueGetter(new ValueGetter() {
    public double getValue(Record record, RecordField field, VisitInfo visitInfo) {
    return (double) ((PointRecord3)record).green;
    }
    public String getStringValue(Record record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((PointRecord3)record).green;
    }
    });
    FIELDS.add(RECORDATTR_BLUE = new RecordField("blue", "blue", "", ATTR_BLUE, "", "ushort", "short", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_BLUE.setValueGetter(new ValueGetter() {
    public double getValue(Record record, RecordField field, VisitInfo visitInfo) {
    return (double) ((PointRecord3)record).blue;
    }
    public String getStringValue(Record record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((PointRecord3)record).blue;
    }
    });
    
    }
    

    double gpsTime;
    int red;
    int green;
    int blue;
    

    public  PointRecord3(PointRecord3 that)  {
        super(that);
        this.gpsTime = that.gpsTime;
        this.red = that.red;
        this.green = that.green;
        this.blue = that.blue;
        
        
    }



    public  PointRecord3(RecordFile file)  {
        super(file);
    }



    public  PointRecord3(RecordFile file, boolean bigEndian)  {
        super(file, bigEndian);
    }



    public int getLastAttribute()  {
        return ATTR_LAST;
    }



    public  boolean equals(Object object)  {
        if(!super.equals(object)) {System.err.println("bad super"); return false;} if(!(object instanceof PointRecord3)) return false;
        PointRecord3 that = (PointRecord3 ) object;
        if(this.gpsTime!= that.gpsTime) {System.err.println("bad gpsTime");  return false;}
        if(this.red!= that.red) {System.err.println("bad red");  return false;}
        if(this.green!= that.green) {System.err.println("bad green");  return false;}
        if(this.blue!= that.blue) {System.err.println("bad blue");  return false;}
        return true;
    }




    public short[] getRgb() {
        return new short[]{(short)red,(short)green,(short)blue};
    }
    public void setRgb(short[]rgb) {
        this.red = rgb[0];        
        this.green = rgb[1];        
        this.blue = rgb[2];        
    }

//    public void setLidarTime(double time) {
//        this.gpsTime = time;
//    }
    public long getLidarTime() {
        return ((LasFile)getRecordFile()).convertTime(this,gpsTime);
    }
    public boolean hasLidarTime() {
        return true;
    }

    protected void addFields(List<RecordField> fields)  {
        super.addFields(fields);
        fields.addAll(FIELDS);
    }



    public double getValue(int attrId)  {
        if(attrId == ATTR_GPSTIME) return gpsTime;
        if(attrId == ATTR_RED) return red;
        if(attrId == ATTR_GREEN) return green;
        if(attrId == ATTR_BLUE) return blue;
        return super.getValue(attrId);
        
    }



    public int getRecordSize()  {
        return super.getRecordSize() + 14;
    }



    public ReadStatus read(RecordIO recordIO) throws Exception  {
        DataInputStream dis = recordIO.getDataInputStream();
        ReadStatus status= super.read(recordIO);
        if(status!=ReadStatus.OK)  return status;
        gpsTime =  readDouble(dis);
        red =  readUnsignedShort(dis);
        green =  readUnsignedShort(dis);
        blue =  readUnsignedShort(dis);
        
        
        return ReadStatus.OK;
    }



    public void write(RecordIO recordIO) throws IOException  {
        DataOutputStream dos = recordIO.getDataOutputStream();
        super.write(recordIO);
        writeDouble(dos, gpsTime);
        writeUnsignedShort(dos, red);
        writeUnsignedShort(dos, green);
        writeUnsignedShort(dos, blue);
        
    }



    public int doPrintCsv(VisitInfo visitInfo,PrintWriter pw)  {
        boolean includeVector = visitInfo.getProperty(PROP_INCLUDEVECTOR, false);
        int superCnt = super.doPrintCsv(visitInfo,pw);
        int myCnt = 0;
        if(superCnt>0) pw.print(',');
        pw.print(getStringValue(RECORDATTR_GPSTIME, gpsTime));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_RED, red));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_GREEN, green));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_BLUE, blue));
        myCnt++;
        return myCnt+superCnt;
        
    }



    public int doPrintCsvHeader(VisitInfo visitInfo,PrintWriter pw)  {
        int superCnt = super.doPrintCsvHeader(visitInfo,pw);
        int myCnt = 0;
        boolean includeVector = visitInfo.getProperty(PROP_INCLUDEVECTOR, false);
        if(superCnt>0) pw.print(',');
        RECORDATTR_GPSTIME.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_RED.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_GREEN.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_BLUE.printCsvHeader(visitInfo,pw);
        myCnt++;
        return myCnt+superCnt;
        
    }



    public void print(Appendable buff)  throws Exception  {
        super.print(buff);
        buff.append(" gpsTime: " + gpsTime+" \n");
        buff.append(" red: " + red+" \n");
        buff.append(" green: " + green+" \n");
        buff.append(" blue: " + blue+" \n");
        
    }



    public double getGpsTime()  {
        return gpsTime;
    }


    public void setGpsTime(double newValue)  {
        gpsTime = newValue;
    }


    public int getRed()  {
        return red;
    }


    public void setRed(int newValue)  {
        red = newValue;
    }


    public int getGreen()  {
        return green;
    }


    public void setGreen(int newValue)  {
        green = newValue;
    }


    public int getBlue()  {
        return blue;
    }


    public void setBlue(int newValue)  {
        blue = newValue;
    }



}



