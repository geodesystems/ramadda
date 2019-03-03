
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



package org.ramadda.geodata.lidar.geotiff;

import org.ramadda.data.record.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;





/** This is generated code from generate.tcl. Do not edit it! */
public class GeoKeyHeader extends Record {
    public static final int ATTR_FIRST = Record.ATTR_LAST;
    public static final List<RecordField> FIELDS = new ArrayList<RecordField>();
    public static final int ATTR_KEYDIRECTORYVERSION =  ATTR_FIRST + 1;
    public static final RecordField RECORDATTR_KEYDIRECTORYVERSION;
    public static final int ATTR_KEYREVISION =  ATTR_FIRST + 2;
    public static final RecordField RECORDATTR_KEYREVISION;
    public static final int ATTR_MINORREVISION =  ATTR_FIRST + 3;
    public static final RecordField RECORDATTR_MINORREVISION;
    public static final int ATTR_NUMBEROFKEYS =  ATTR_FIRST + 4;
    public static final RecordField RECORDATTR_NUMBEROFKEYS;
    public static final int ATTR_LAST = ATTR_FIRST + 5;
    

    static {
    FIELDS.add(RECORDATTR_KEYDIRECTORYVERSION = new RecordField("keyDirectoryVersion", "keyDirectoryVersion", "", ATTR_KEYDIRECTORYVERSION, "", "ushort", "short", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_KEYDIRECTORYVERSION.setValueGetter(new ValueGetter() {
    public double getValue(Record record, RecordField field, VisitInfo visitInfo) {
    return (double) ((GeoKeyHeader)record).keyDirectoryVersion;
    }
    public String getStringValue(Record record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((GeoKeyHeader)record).keyDirectoryVersion;
    }
    });
    FIELDS.add(RECORDATTR_KEYREVISION = new RecordField("keyRevision", "keyRevision", "", ATTR_KEYREVISION, "", "ushort", "short", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_KEYREVISION.setValueGetter(new ValueGetter() {
    public double getValue(Record record, RecordField field, VisitInfo visitInfo) {
    return (double) ((GeoKeyHeader)record).keyRevision;
    }
    public String getStringValue(Record record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((GeoKeyHeader)record).keyRevision;
    }
    });
    FIELDS.add(RECORDATTR_MINORREVISION = new RecordField("minorRevision", "minorRevision", "", ATTR_MINORREVISION, "", "ushort", "short", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_MINORREVISION.setValueGetter(new ValueGetter() {
    public double getValue(Record record, RecordField field, VisitInfo visitInfo) {
    return (double) ((GeoKeyHeader)record).minorRevision;
    }
    public String getStringValue(Record record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((GeoKeyHeader)record).minorRevision;
    }
    });
    FIELDS.add(RECORDATTR_NUMBEROFKEYS = new RecordField("numberOfKeys", "numberOfKeys", "", ATTR_NUMBEROFKEYS, "", "ushort", "short", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_NUMBEROFKEYS.setValueGetter(new ValueGetter() {
    public double getValue(Record record, RecordField field, VisitInfo visitInfo) {
    return (double) ((GeoKeyHeader)record).numberOfKeys;
    }
    public String getStringValue(Record record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((GeoKeyHeader)record).numberOfKeys;
    }
    });
    
    }
    

    int keyDirectoryVersion;
    int keyRevision;
    int minorRevision;
    int numberOfKeys;
    

    public  GeoKeyHeader(GeoKeyHeader that)  {
        super(that);
        this.keyDirectoryVersion = that.keyDirectoryVersion;
        this.keyRevision = that.keyRevision;
        this.minorRevision = that.minorRevision;
        this.numberOfKeys = that.numberOfKeys;
        
        
    }



    public  GeoKeyHeader(RecordFile file)  {
        super(file);
    }



    public  GeoKeyHeader(RecordFile file, boolean bigEndian)  {
        super(file, bigEndian);
    }



    public int getLastAttribute()  {
        return ATTR_LAST;
    }



    public  boolean equals(Object object)  {
        if(!super.equals(object)) {System.err.println("bad super"); return false;} if(!(object instanceof GeoKeyHeader)) return false;
        GeoKeyHeader that = (GeoKeyHeader ) object;
        if(this.keyDirectoryVersion!= that.keyDirectoryVersion) {System.err.println("bad keyDirectoryVersion");  return false;}
        if(this.keyRevision!= that.keyRevision) {System.err.println("bad keyRevision");  return false;}
        if(this.minorRevision!= that.minorRevision) {System.err.println("bad minorRevision");  return false;}
        if(this.numberOfKeys!= that.numberOfKeys) {System.err.println("bad numberOfKeys");  return false;}
        return true;
    }




    protected void addFields(List<RecordField> fields)  {
        super.addFields(fields);
        fields.addAll(FIELDS);
    }



    public double getValue(int attrId)  {
        if(attrId == ATTR_KEYDIRECTORYVERSION) return keyDirectoryVersion;
        if(attrId == ATTR_KEYREVISION) return keyRevision;
        if(attrId == ATTR_MINORREVISION) return minorRevision;
        if(attrId == ATTR_NUMBEROFKEYS) return numberOfKeys;
        return super.getValue(attrId);
        
    }



    public int getRecordSize()  {
        return super.getRecordSize() + 8;
    }



    public ReadStatus read(RecordIO recordIO) throws Exception  {
        DataInputStream dis = recordIO.getDataInputStream();
        keyDirectoryVersion =  readUnsignedShort(dis);
        keyRevision =  readUnsignedShort(dis);
        minorRevision =  readUnsignedShort(dis);
        numberOfKeys =  readUnsignedShort(dis);
        
        
        return ReadStatus.OK;
    }



    public void write(RecordIO recordIO) throws IOException  {
        DataOutputStream dos = recordIO.getDataOutputStream();
        writeUnsignedShort(dos, keyDirectoryVersion);
        writeUnsignedShort(dos, keyRevision);
        writeUnsignedShort(dos, minorRevision);
        writeUnsignedShort(dos, numberOfKeys);
        
    }



    public int doPrintCsv(VisitInfo visitInfo,PrintWriter pw)  {
        boolean includeVector = visitInfo.getProperty(PROP_INCLUDEVECTOR, false);
        int superCnt = super.doPrintCsv(visitInfo,pw);
        int myCnt = 0;
        if(superCnt>0) pw.print(',');
        pw.print(getStringValue(RECORDATTR_KEYDIRECTORYVERSION, keyDirectoryVersion));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_KEYREVISION, keyRevision));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_MINORREVISION, minorRevision));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_NUMBEROFKEYS, numberOfKeys));
        myCnt++;
        return myCnt+superCnt;
        
    }



    public int doPrintCsvHeader(VisitInfo visitInfo,PrintWriter pw)  {
        int superCnt = super.doPrintCsvHeader(visitInfo,pw);
        int myCnt = 0;
        boolean includeVector = visitInfo.getProperty(PROP_INCLUDEVECTOR, false);
        if(superCnt>0) pw.print(',');
        RECORDATTR_KEYDIRECTORYVERSION.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_KEYREVISION.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_MINORREVISION.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_NUMBEROFKEYS.printCsvHeader(visitInfo,pw);
        myCnt++;
        return myCnt+superCnt;
        
    }



    public void print(Appendable buff)  throws Exception  {
        super.print(buff);
        buff.append(" keyDirectoryVersion: " + keyDirectoryVersion+" \n");
        buff.append(" keyRevision: " + keyRevision+" \n");
        buff.append(" minorRevision: " + minorRevision+" \n");
        buff.append(" numberOfKeys: " + numberOfKeys+" \n");
        
    }



    public int getKeyDirectoryVersion()  {
        return keyDirectoryVersion;
    }


    public void setKeyDirectoryVersion(int newValue)  {
        keyDirectoryVersion = newValue;
    }


    public int getKeyRevision()  {
        return keyRevision;
    }


    public void setKeyRevision(int newValue)  {
        keyRevision = newValue;
    }


    public int getMinorRevision()  {
        return minorRevision;
    }


    public void setMinorRevision(int newValue)  {
        minorRevision = newValue;
    }


    public int getNumberOfKeys()  {
        return numberOfKeys;
    }


    public void setNumberOfKeys(int newValue)  {
        numberOfKeys = newValue;
    }



}



