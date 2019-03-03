
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
public class LceRecordV1_2 extends LvisRecord {
    public static final int ATTR_FIRST = LvisRecord.ATTR_LAST;
    public static final List<RecordField> FIELDS = new ArrayList<RecordField>();
    public static final int ATTR_LFID =  ATTR_FIRST + 1;
    public static final RecordField RECORDATTR_LFID;
    public static final int ATTR_SHOTNUMBER =  ATTR_FIRST + 2;
    public static final RecordField RECORDATTR_SHOTNUMBER;
    public static final int ATTR_LVISTIME =  ATTR_FIRST + 3;
    public static final RecordField RECORDATTR_LVISTIME;
    public static final int ATTR_TLON =  ATTR_FIRST + 4;
    public static final RecordField RECORDATTR_TLON;
    public static final int ATTR_TLAT =  ATTR_FIRST + 5;
    public static final RecordField RECORDATTR_TLAT;
    public static final int ATTR_ZT =  ATTR_FIRST + 6;
    public static final RecordField RECORDATTR_ZT;
    public static final int ATTR_LAST = ATTR_FIRST + 7;
    

    static {
    FIELDS.add(RECORDATTR_LFID = new RecordField("lfid", "lfid", "", ATTR_LFID, "", "int", "int", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_LFID.setValueGetter(new ValueGetter() {
    public double getValue(Record record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LceRecordV1_2)record).lfid;
    }
    public String getStringValue(Record record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LceRecordV1_2)record).lfid;
    }
    });
    FIELDS.add(RECORDATTR_SHOTNUMBER = new RecordField("shotnumber", "shotnumber", "", ATTR_SHOTNUMBER, "", "int", "int", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_SHOTNUMBER.setValueGetter(new ValueGetter() {
    public double getValue(Record record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LceRecordV1_2)record).shotnumber;
    }
    public String getStringValue(Record record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LceRecordV1_2)record).shotnumber;
    }
    });
    FIELDS.add(RECORDATTR_LVISTIME = new RecordField("lvisTime", "lvisTime", "", ATTR_LVISTIME, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_LVISTIME.setValueGetter(new ValueGetter() {
    public double getValue(Record record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LceRecordV1_2)record).lvisTime;
    }
    public String getStringValue(Record record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LceRecordV1_2)record).lvisTime;
    }
    });
    FIELDS.add(RECORDATTR_TLON = new RecordField("tlon", "tlon", "", ATTR_TLON, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_TLON.setValueGetter(new ValueGetter() {
    public double getValue(Record record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LceRecordV1_2)record).tlon;
    }
    public String getStringValue(Record record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LceRecordV1_2)record).tlon;
    }
    });
    FIELDS.add(RECORDATTR_TLAT = new RecordField("tlat", "tlat", "", ATTR_TLAT, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_TLAT.setValueGetter(new ValueGetter() {
    public double getValue(Record record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LceRecordV1_2)record).tlat;
    }
    public String getStringValue(Record record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LceRecordV1_2)record).tlat;
    }
    });
    FIELDS.add(RECORDATTR_ZT = new RecordField("zt", "zt", "", ATTR_ZT, "", "float", "float", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_ZT.setValueGetter(new ValueGetter() {
    public double getValue(Record record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LceRecordV1_2)record).zt;
    }
    public String getStringValue(Record record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LceRecordV1_2)record).zt;
    }
    });
    
    }
    

    int lfid;
    int shotnumber;
    double tlon;
    double tlat;
    float zt;
    

    public  LceRecordV1_2(LceRecordV1_2 that)  {
        super(that);
        this.lfid = that.lfid;
        this.shotnumber = that.shotnumber;
        this.lvisTime = that.lvisTime;
        this.tlon = that.tlon;
        this.tlat = that.tlat;
        this.zt = that.zt;
        
        
    }



    public  LceRecordV1_2(RecordFile file)  {
        super(file);
    }



    public  LceRecordV1_2(RecordFile file, boolean bigEndian)  {
        super(file, bigEndian);
    }



    public int getLastAttribute()  {
        return ATTR_LAST;
    }



    public  boolean equals(Object object)  {
        if(!super.equals(object)) {System.err.println("bad super"); return false;} if(!(object instanceof LceRecordV1_2)) return false;
        LceRecordV1_2 that = (LceRecordV1_2 ) object;
        if(this.lfid!= that.lfid) {System.err.println("bad lfid");  return false;}
        if(this.shotnumber!= that.shotnumber) {System.err.println("bad shotnumber");  return false;}
        if(this.lvisTime!= that.lvisTime) {System.err.println("bad lvisTime");  return false;}
        if(this.tlon!= that.tlon) {System.err.println("bad tlon");  return false;}
        if(this.tlat!= that.tlat) {System.err.println("bad tlat");  return false;}
        if(this.zt!= that.zt) {System.err.println("bad zt");  return false;}
        return true;
    }




    protected void addFields(List<RecordField> fields)  {
        super.addFields(fields);
        fields.addAll(FIELDS);
    }



    public double getValue(int attrId)  {
        if(attrId == ATTR_LFID) return lfid;
        if(attrId == ATTR_SHOTNUMBER) return shotnumber;
        if(attrId == ATTR_LVISTIME) return lvisTime;
        if(attrId == ATTR_TLON) return tlon;
        if(attrId == ATTR_TLAT) return tlat;
        if(attrId == ATTR_ZT) return zt;
        return super.getValue(attrId);
        
    }



    public int getRecordSize()  {
        return super.getRecordSize() + 36;
    }



    public ReadStatus read(RecordIO recordIO) throws Exception  {
        DataInputStream dis = recordIO.getDataInputStream();
        ReadStatus status= super.read(recordIO);
        if(status!=ReadStatus.OK)  return status;
        lfid =  readInt(dis);
        shotnumber =  readInt(dis);
        lvisTime =  readDouble(dis);
        tlon =  readDouble(dis);
        setLongitude(org.ramadda.util.GeoUtils.normalizeLongitude(tlon));
        tlat =  readDouble(dis);
        setLatitude(tlat);
        zt =  readFloat(dis);
        setAltitude(zt);
        
        
        return ReadStatus.OK;
    }



    public void write(RecordIO recordIO) throws IOException  {
        DataOutputStream dos = recordIO.getDataOutputStream();
        super.write(recordIO);
        writeInt(dos, lfid);
        writeInt(dos, shotnumber);
        writeDouble(dos, lvisTime);
        writeDouble(dos, tlon);
        writeDouble(dos, tlat);
        writeFloat(dos, zt);
        
    }



    public int doPrintCsv(VisitInfo visitInfo,PrintWriter pw)  {
        boolean includeVector = visitInfo.getProperty(PROP_INCLUDEVECTOR, false);
        int superCnt = super.doPrintCsv(visitInfo,pw);
        int myCnt = 0;
        if(superCnt>0) pw.print(',');
        pw.print(getStringValue(RECORDATTR_LFID, lfid));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_SHOTNUMBER, shotnumber));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_LVISTIME, lvisTime));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_TLON, tlon));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_TLAT, tlat));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_ZT, zt));
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
        RECORDATTR_TLON.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_TLAT.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_ZT.printCsvHeader(visitInfo,pw);
        myCnt++;
        return myCnt+superCnt;
        
    }



    public void print(Appendable buff)  throws Exception  {
        super.print(buff);
        buff.append(" lfid: " + lfid+" \n");
        buff.append(" shotnumber: " + shotnumber+" \n");
        buff.append(" lvisTime: " + lvisTime+" \n");
        buff.append(" tlon: " + tlon+" \n");
        buff.append(" tlat: " + tlat+" \n");
        buff.append(" zt: " + zt+" \n");
        
    }



    public int getLfid()  {
        return lfid;
    }


    public void setLfid(int newValue)  {
        lfid = newValue;
    }


    public int getShotnumber()  {
        return shotnumber;
    }


    public void setShotnumber(int newValue)  {
        shotnumber = newValue;
    }


    public double getTlon()  {
        return tlon;
    }


    public void setTlon(double newValue)  {
        tlon = newValue;
    }


    public double getTlat()  {
        return tlat;
    }


    public void setTlat(double newValue)  {
        tlat = newValue;
    }


    public float getZt()  {
        return zt;
    }


    public void setZt(float newValue)  {
        zt = newValue;
    }



}



