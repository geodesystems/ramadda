
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
public class Test extends Record {
    public static final int ATTR_FIRST = Record.ATTR_LAST;
    public static final List<RecordField> FIELDS = new ArrayList<RecordField>();
    public static final int ATTR_FILESOURCEID =  ATTR_FIRST + 1;
    public static final RecordField RECORDATTR_FILESOURCEID;
    public static final int ATTR_NUMBEROFPOINTSBYRETURN =  ATTR_FIRST + 2;
    public static final RecordField RECORDATTR_NUMBEROFPOINTSBYRETURN;
    public static final int ATTR_MINZ =  ATTR_FIRST + 3;
    public static final RecordField RECORDATTR_MINZ;
    public static final int ATTR_LAST = ATTR_FIRST + 4;
    

    static {
    FIELDS.add(RECORDATTR_FILESOURCEID = new RecordField("fileSourceId", "fileSourceId", "", ATTR_FILESOURCEID, "", "ushort", "short", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_FILESOURCEID.setValueGetter(new ValueGetter() {
    public double getValue(Record record, RecordField field, VisitInfo visitInfo) {
    return (double) ((Test)record).fileSourceId;
    }
    public String getStringValue(Record record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((Test)record).fileSourceId;
    }
    });
    FIELDS.add(RECORDATTR_NUMBEROFPOINTSBYRETURN = new RecordField("numberOfPointsByReturn", "numberOfPointsByReturn", "", ATTR_NUMBEROFPOINTSBYRETURN, "", "uint[5]", "int", 5, SEARCHABLE_NO,CHARTABLE_NO));
    FIELDS.add(RECORDATTR_MINZ = new RecordField("minZ", "minZ", "", ATTR_MINZ, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_MINZ.setValueGetter(new ValueGetter() {
    public double getValue(Record record, RecordField field, VisitInfo visitInfo) {
    return (double) ((Test)record).minZ;
    }
    public String getStringValue(Record record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((Test)record).minZ;
    }
    });
    
    }
    

    int fileSourceId = 1;
    long[] numberOfPointsByReturn = new long[5];
    double minZ;
    

    public  Test(Test that)  {
        super(that);
        this.fileSourceId = that.fileSourceId;
        this.numberOfPointsByReturn = that.numberOfPointsByReturn;
        this.minZ = that.minZ;
        
        
    }



    public  Test(RecordFile file)  {
        super(file);
    }



    public  Test(RecordFile file, boolean bigEndian)  {
        super(file, bigEndian);
    }



    public int getLastAttribute()  {
        return ATTR_LAST;
    }



    public  boolean equals(Object object)  {
        if(!super.equals(object)) {System.err.println("bad super"); return false;} if(!(object instanceof Test)) return false;
        Test that = (Test ) object;
        if(this.fileSourceId!= that.fileSourceId) {System.err.println("bad fileSourceId");  return false;}
        if(!java.util.Arrays.equals(this.numberOfPointsByReturn, that.numberOfPointsByReturn)) {System.err.println("bad numberOfPointsByReturn"); return false;}
        if(this.minZ!= that.minZ) {System.err.println("bad minZ");  return false;}
        return true;
    }




    protected void addFields(List<RecordField> fields)  {
        super.addFields(fields);
        fields.addAll(FIELDS);
    }



    public double getValue(int attrId)  {
        if(attrId == ATTR_FILESOURCEID) return fileSourceId;
        if(attrId == ATTR_MINZ) return minZ;
        return super.getValue(attrId);
        
    }



    public int getRecordSize()  {
        return super.getRecordSize() + 30;
    }



    public ReadStatus read(RecordIO recordIO) throws Exception  {
        DataInputStream dis = recordIO.getDataInputStream();
        fileSourceId =  readUnsignedShort(dis);
        readUnsignedInts(dis,numberOfPointsByReturn);
        minZ =  readDouble(dis);
        
        
        return ReadStatus.OK;
    }



    public void write(RecordIO recordIO) throws IOException  {
        DataOutputStream dos = recordIO.getDataOutputStream();
        writeUnsignedShort(dos, fileSourceId);
        writeUnsignedInts(dos, numberOfPointsByReturn);
        writeDouble(dos, minZ);
        
    }



    public int doPrintCsv(VisitInfo visitInfo,PrintWriter pw)  {
        boolean includeVector = visitInfo.getProperty(PROP_INCLUDEVECTOR, false);
        int superCnt = super.doPrintCsv(visitInfo,pw);
        int myCnt = 0;
        if(superCnt>0) pw.print(',');
        pw.print(getStringValue(RECORDATTR_FILESOURCEID, fileSourceId));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_MINZ, minZ));
        myCnt++;
        if(includeVector) {
        for(int i=0;i<this.numberOfPointsByReturn.length;i++) {pw.print(i==0?'|':',');pw.print(this.numberOfPointsByReturn[i]);}
        myCnt++;
        }
        return myCnt+superCnt;
        
    }



    public int doPrintCsvHeader(VisitInfo visitInfo,PrintWriter pw)  {
        int superCnt = super.doPrintCsvHeader(visitInfo,pw);
        int myCnt = 0;
        boolean includeVector = visitInfo.getProperty(PROP_INCLUDEVECTOR, false);
        if(superCnt>0) pw.print(',');
        RECORDATTR_FILESOURCEID.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_MINZ.printCsvHeader(visitInfo,pw);
        myCnt++;
        if(includeVector) {
        pw.print(',');
        RECORDATTR_NUMBEROFPOINTSBYRETURN.printCsvHeader(visitInfo,pw);
        myCnt++;
        }
        return myCnt+superCnt;
        
    }



    public void print(Appendable buff)  throws Exception  {
        super.print(buff);
        buff.append(" fileSourceId: " + fileSourceId+" \n");
        buff.append(" minZ: " + minZ+" \n");
        
    }



    public int getFileSourceId()  {
        return fileSourceId;
    }


    public void setFileSourceId(int newValue)  {
        fileSourceId = newValue;
    }


    public long[] getNumberOfPointsByReturn()  {
        return numberOfPointsByReturn;
    }


    public void setNumberOfPointsByReturn(long[] newValue)  {
        copy(numberOfPointsByReturn, newValue);
    }


    public double getMinZ()  {
        return minZ;
    }


    public void setMinZ(double newValue)  {
        minZ = newValue;
    }



}



