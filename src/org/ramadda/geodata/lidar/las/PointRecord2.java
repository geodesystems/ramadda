
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
public class PointRecord2 extends PointRecord0 {
    public static final int ATTR_FIRST = PointRecord0.ATTR_LAST;
    public static final List<RecordField> FIELDS = new ArrayList<RecordField>();
    public static final int ATTR_RED =  ATTR_FIRST + 1;
    public static final RecordField RECORDATTR_RED;
    public static final int ATTR_GREEN =  ATTR_FIRST + 2;
    public static final RecordField RECORDATTR_GREEN;
    public static final int ATTR_BLUE =  ATTR_FIRST + 3;
    public static final RecordField RECORDATTR_BLUE;
    public static final int ATTR_LAST = ATTR_FIRST + 4;
    

    static {
    FIELDS.add(RECORDATTR_RED = new RecordField("red", "red", "", ATTR_RED, "", "ushort", "short", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_RED.setValueGetter(new ValueGetter() {
    public double getValue(Record record, RecordField field, VisitInfo visitInfo) {
    return (double) ((PointRecord2)record).red;
    }
    public String getStringValue(Record record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((PointRecord2)record).red;
    }
    });
    FIELDS.add(RECORDATTR_GREEN = new RecordField("green", "green", "", ATTR_GREEN, "", "ushort", "short", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_GREEN.setValueGetter(new ValueGetter() {
    public double getValue(Record record, RecordField field, VisitInfo visitInfo) {
    return (double) ((PointRecord2)record).green;
    }
    public String getStringValue(Record record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((PointRecord2)record).green;
    }
    });
    FIELDS.add(RECORDATTR_BLUE = new RecordField("blue", "blue", "", ATTR_BLUE, "", "ushort", "short", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_BLUE.setValueGetter(new ValueGetter() {
    public double getValue(Record record, RecordField field, VisitInfo visitInfo) {
    return (double) ((PointRecord2)record).blue;
    }
    public String getStringValue(Record record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((PointRecord2)record).blue;
    }
    });
    
    }
    

    int red;
    int green;
    int blue;
    

    public  PointRecord2(PointRecord2 that)  {
        super(that);
        this.red = that.red;
        this.green = that.green;
        this.blue = that.blue;
        
        
    }



    public  PointRecord2(RecordFile file)  {
        super(file);
    }



    public  PointRecord2(RecordFile file, boolean bigEndian)  {
        super(file, bigEndian);
    }



    public int getLastAttribute()  {
        return ATTR_LAST;
    }



    public  boolean equals(Object object)  {
        if(!super.equals(object)) {System.err.println("bad super"); return false;} if(!(object instanceof PointRecord2)) return false;
        PointRecord2 that = (PointRecord2 ) object;
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

    protected void addFields(List<RecordField> fields)  {
        super.addFields(fields);
        fields.addAll(FIELDS);
    }



    public double getValue(int attrId)  {
        if(attrId == ATTR_RED) return red;
        if(attrId == ATTR_GREEN) return green;
        if(attrId == ATTR_BLUE) return blue;
        return super.getValue(attrId);
        
    }



    public int getRecordSize()  {
        return super.getRecordSize() + 6;
    }



    public ReadStatus read(RecordIO recordIO) throws Exception  {
        DataInputStream dis = recordIO.getDataInputStream();
        ReadStatus status= super.read(recordIO);
        if(status!=ReadStatus.OK)  return status;
        red =  readUnsignedShort(dis);
        green =  readUnsignedShort(dis);
        blue =  readUnsignedShort(dis);
        
        
        return ReadStatus.OK;
    }



    public void write(RecordIO recordIO) throws IOException  {
        DataOutputStream dos = recordIO.getDataOutputStream();
        super.write(recordIO);
        writeUnsignedShort(dos, red);
        writeUnsignedShort(dos, green);
        writeUnsignedShort(dos, blue);
        
    }



    public int doPrintCsv(VisitInfo visitInfo,PrintWriter pw)  {
        boolean includeVector = visitInfo.getProperty(PROP_INCLUDEVECTOR, false);
        int superCnt = super.doPrintCsv(visitInfo,pw);
        int myCnt = 0;
        if(superCnt>0) pw.print(',');
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
        buff.append(" red: " + red+" \n");
        buff.append(" green: " + green+" \n");
        buff.append(" blue: " + blue+" \n");
        
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



