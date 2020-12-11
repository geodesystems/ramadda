
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
public class PointRecord1 extends PointRecord0 {
    public static final int ATTR_FIRST = PointRecord0.ATTR_LAST;
    public static final List<RecordField> FIELDS = new ArrayList<RecordField>();
    public static final int ATTR_GPSTIME =  ATTR_FIRST + 1;
    public static final RecordField RECORDATTR_GPSTIME;
    public static final int ATTR_LAST = ATTR_FIRST + 2;
    

    static {
    FIELDS.add(RECORDATTR_GPSTIME = new RecordField("gpsTime", "gpsTime", "", ATTR_GPSTIME, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_GPSTIME.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((PointRecord1)record).gpsTime;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((PointRecord1)record).gpsTime;
    }
    });
    
    }
    

    double gpsTime;
    

    public  PointRecord1(PointRecord1 that)  {
        super(that);
        this.gpsTime = that.gpsTime;
        
        
    }



    public  PointRecord1(RecordFile file)  {
        super(file);
    }



    public  PointRecord1(RecordFile file, boolean bigEndian)  {
        super(file, bigEndian);
    }



    public int getLastAttribute()  {
        return ATTR_LAST;
    }



    public  boolean equals(Object object)  {
        if(!super.equals(object)) {System.err.println("bad super"); return false;} if(!(object instanceof PointRecord1)) return false;
        PointRecord1 that = (PointRecord1 ) object;
        if(this.gpsTime!= that.gpsTime) {System.err.println("bad gpsTime");  return false;}
        return true;
    }




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
        return super.getValue(attrId);
        
    }



    public int getRecordSize()  {
        return super.getRecordSize() + 8;
    }



    public BaseRecord.ReadStatus read(RecordIO recordIO) throws Exception  {
        DataInputStream dis = recordIO.getDataInputStream();
        BaseRecord.ReadStatus status= super.read(recordIO);
        if(status!=BaseRecord.ReadStatus.OK)  return status;
        gpsTime =  readDouble(dis);
        
        
        return BaseRecord.ReadStatus.OK;
    }



    public void write(RecordIO recordIO) throws IOException  {
        DataOutputStream dos = recordIO.getDataOutputStream();
        super.write(recordIO);
        writeDouble(dos, gpsTime);
        
    }



    public int doPrintCsv(VisitInfo visitInfo,PrintWriter pw)  {
        boolean includeVector = visitInfo.getProperty(PROP_INCLUDEVECTOR, false);
        int superCnt = super.doPrintCsv(visitInfo,pw);
        int myCnt = 0;
        if(superCnt>0) pw.print(',');
        pw.print(getStringValue(RECORDATTR_GPSTIME, gpsTime));
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
        return myCnt+superCnt;
        
    }



    public void print(Appendable buff)  throws Exception  {
        super.print(buff);
        buff.append(" gpsTime: " + gpsTime+" \n");
        
    }



    public double getGpsTime()  {
        return gpsTime;
    }


    public void setGpsTime(double newValue)  {
        gpsTime = newValue;
    }



}



