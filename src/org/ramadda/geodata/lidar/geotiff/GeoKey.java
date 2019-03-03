
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
public class GeoKey extends Record {
    public static final int ATTR_FIRST = Record.ATTR_LAST;
    public static final List<RecordField> FIELDS = new ArrayList<RecordField>();
    public static final int ATTR_KEYID =  ATTR_FIRST + 1;
    public static final RecordField RECORDATTR_KEYID;
    public static final int ATTR_TIFFTAGLOCATION =  ATTR_FIRST + 2;
    public static final RecordField RECORDATTR_TIFFTAGLOCATION;
    public static final int ATTR_COUNT =  ATTR_FIRST + 3;
    public static final RecordField RECORDATTR_COUNT;
    public static final int ATTR_VALUEOFFSET =  ATTR_FIRST + 4;
    public static final RecordField RECORDATTR_VALUEOFFSET;
    public static final int ATTR_LAST = ATTR_FIRST + 5;
    

    static {
    FIELDS.add(RECORDATTR_KEYID = new RecordField("keyId", "keyId", "", ATTR_KEYID, "", "ushort", "short", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_KEYID.setValueGetter(new ValueGetter() {
    public double getValue(Record record, RecordField field, VisitInfo visitInfo) {
    return (double) ((GeoKey)record).keyId;
    }
    public String getStringValue(Record record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((GeoKey)record).keyId;
    }
    });
    FIELDS.add(RECORDATTR_TIFFTAGLOCATION = new RecordField("tiffTagLocation", "tiffTagLocation", "", ATTR_TIFFTAGLOCATION, "", "ushort", "short", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_TIFFTAGLOCATION.setValueGetter(new ValueGetter() {
    public double getValue(Record record, RecordField field, VisitInfo visitInfo) {
    return (double) ((GeoKey)record).tiffTagLocation;
    }
    public String getStringValue(Record record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((GeoKey)record).tiffTagLocation;
    }
    });
    FIELDS.add(RECORDATTR_COUNT = new RecordField("Count", "Count", "", ATTR_COUNT, "", "ushort", "short", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_COUNT.setValueGetter(new ValueGetter() {
    public double getValue(Record record, RecordField field, VisitInfo visitInfo) {
    return (double) ((GeoKey)record).Count;
    }
    public String getStringValue(Record record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((GeoKey)record).Count;
    }
    });
    FIELDS.add(RECORDATTR_VALUEOFFSET = new RecordField("valueOffset", "valueOffset", "", ATTR_VALUEOFFSET, "", "ushort", "short", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_VALUEOFFSET.setValueGetter(new ValueGetter() {
    public double getValue(Record record, RecordField field, VisitInfo visitInfo) {
    return (double) ((GeoKey)record).valueOffset;
    }
    public String getStringValue(Record record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((GeoKey)record).valueOffset;
    }
    });
    
    }
    

    int keyId;
    int tiffTagLocation;
    int Count;
    int valueOffset;
    

    public  GeoKey(GeoKey that)  {
        super(that);
        this.keyId = that.keyId;
        this.tiffTagLocation = that.tiffTagLocation;
        this.Count = that.Count;
        this.valueOffset = that.valueOffset;
        
        
    }



    public  GeoKey(RecordFile file)  {
        super(file);
    }



    public  GeoKey(RecordFile file, boolean bigEndian)  {
        super(file, bigEndian);
    }



    public int getLastAttribute()  {
        return ATTR_LAST;
    }



    public  boolean equals(Object object)  {
        if(!super.equals(object)) {System.err.println("bad super"); return false;} if(!(object instanceof GeoKey)) return false;
        GeoKey that = (GeoKey ) object;
        if(this.keyId!= that.keyId) {System.err.println("bad keyId");  return false;}
        if(this.tiffTagLocation!= that.tiffTagLocation) {System.err.println("bad tiffTagLocation");  return false;}
        if(this.Count!= that.Count) {System.err.println("bad Count");  return false;}
        if(this.valueOffset!= that.valueOffset) {System.err.println("bad valueOffset");  return false;}
        return true;
    }





    private byte[]geoKeyAsciiParams;
    private double[]doubles;

    private String stringValue;
    public GeoKey(RecordFile file, boolean endian,byte[]geoKeyAsciiParams,double[]doubles) {
        super(file, endian);
        this.geoKeyAsciiParams = geoKeyAsciiParams;
        this.doubles = doubles;
    }



    public double getValue() {
        if(tiffTagLocation==0) {
            return (double) getValueOffset();
        }
        return doubles[getValueOffset()];
        
    }


    public String getStringValue() {
        if(stringValue == null) {
            if(tiffTagLocation==34737) {
                stringValue = new String(geoKeyAsciiParams, getValueOffset(), getCount());
            } else  if(tiffTagLocation==34736) {
                stringValue = ""+getValue();
            } else {
                stringValue = ""+getValueOffset();
            }
        }
        return stringValue;
    }



    protected void addFields(List<RecordField> fields)  {
        super.addFields(fields);
        fields.addAll(FIELDS);
    }



    public double getValue(int attrId)  {
        if(attrId == ATTR_KEYID) return keyId;
        if(attrId == ATTR_TIFFTAGLOCATION) return tiffTagLocation;
        if(attrId == ATTR_COUNT) return Count;
        if(attrId == ATTR_VALUEOFFSET) return valueOffset;
        return super.getValue(attrId);
        
    }



    public int getRecordSize()  {
        return super.getRecordSize() + 8;
    }



    public ReadStatus read(RecordIO recordIO) throws Exception  {
        DataInputStream dis = recordIO.getDataInputStream();
        keyId =  readUnsignedShort(dis);
        tiffTagLocation =  readUnsignedShort(dis);
        Count =  readUnsignedShort(dis);
        valueOffset =  readUnsignedShort(dis);
        
        
        return ReadStatus.OK;
    }



    public void write(RecordIO recordIO) throws IOException  {
        DataOutputStream dos = recordIO.getDataOutputStream();
        writeUnsignedShort(dos, keyId);
        writeUnsignedShort(dos, tiffTagLocation);
        writeUnsignedShort(dos, Count);
        writeUnsignedShort(dos, valueOffset);
        
    }



    public int doPrintCsv(VisitInfo visitInfo,PrintWriter pw)  {
        boolean includeVector = visitInfo.getProperty(PROP_INCLUDEVECTOR, false);
        int superCnt = super.doPrintCsv(visitInfo,pw);
        int myCnt = 0;
        if(superCnt>0) pw.print(',');
        pw.print(getStringValue(RECORDATTR_KEYID, keyId));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_TIFFTAGLOCATION, tiffTagLocation));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_COUNT, Count));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_VALUEOFFSET, valueOffset));
        myCnt++;
        return myCnt+superCnt;
        
    }



    public int doPrintCsvHeader(VisitInfo visitInfo,PrintWriter pw)  {
        int superCnt = super.doPrintCsvHeader(visitInfo,pw);
        int myCnt = 0;
        boolean includeVector = visitInfo.getProperty(PROP_INCLUDEVECTOR, false);
        if(superCnt>0) pw.print(',');
        RECORDATTR_KEYID.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_TIFFTAGLOCATION.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_COUNT.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_VALUEOFFSET.printCsvHeader(visitInfo,pw);
        myCnt++;
        return myCnt+superCnt;
        
    }



    public void print(Appendable buff)  throws Exception  {
        super.print(buff);
        buff.append(" keyId: " + keyId+" \n");
        buff.append(" tiffTagLocation: " + tiffTagLocation+" \n");
        buff.append(" Count: " + Count+" \n");
        buff.append(" valueOffset: " + valueOffset+" \n");
        
    }



    public int getKeyId()  {
        return keyId;
    }


    public void setKeyId(int newValue)  {
        keyId = newValue;
    }


    public int getTiffTagLocation()  {
        return tiffTagLocation;
    }


    public void setTiffTagLocation(int newValue)  {
        tiffTagLocation = newValue;
    }


    public int getCount()  {
        return Count;
    }


    public void setCount(int newValue)  {
        Count = newValue;
    }


    public int getValueOffset()  {
        return valueOffset;
    }


    public void setValueOffset(int newValue)  {
        valueOffset = newValue;
    }



}



