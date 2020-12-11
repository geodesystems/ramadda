
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
public class LgeRecordV1_2 extends LvisRecord {
    public static final int ATTR_FIRST = LvisRecord.ATTR_LAST;
    public static final List<RecordField> FIELDS = new ArrayList<RecordField>();
    public static final int ATTR_LFID =  ATTR_FIRST + 1;
    public static final RecordField RECORDATTR_LFID;
    public static final int ATTR_SHOTNUMBER =  ATTR_FIRST + 2;
    public static final RecordField RECORDATTR_SHOTNUMBER;
    public static final int ATTR_LVISTIME =  ATTR_FIRST + 3;
    public static final RecordField RECORDATTR_LVISTIME;
    public static final int ATTR_GLON =  ATTR_FIRST + 4;
    public static final RecordField RECORDATTR_GLON;
    public static final int ATTR_GLAT =  ATTR_FIRST + 5;
    public static final RecordField RECORDATTR_GLAT;
    public static final int ATTR_ZG =  ATTR_FIRST + 6;
    public static final RecordField RECORDATTR_ZG;
    public static final int ATTR_RH25 =  ATTR_FIRST + 7;
    public static final RecordField RECORDATTR_RH25;
    public static final int ATTR_RH50 =  ATTR_FIRST + 8;
    public static final RecordField RECORDATTR_RH50;
    public static final int ATTR_RH75 =  ATTR_FIRST + 9;
    public static final RecordField RECORDATTR_RH75;
    public static final int ATTR_RH100 =  ATTR_FIRST + 10;
    public static final RecordField RECORDATTR_RH100;
    public static final int ATTR_LAST = ATTR_FIRST + 11;
    

    static {
    FIELDS.add(RECORDATTR_LFID = new RecordField("lfid", "lfid", "", ATTR_LFID, "", "int", "int", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_LFID.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LgeRecordV1_2)record).lfid;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LgeRecordV1_2)record).lfid;
    }
    });
    FIELDS.add(RECORDATTR_SHOTNUMBER = new RecordField("shotnumber", "shotnumber", "", ATTR_SHOTNUMBER, "", "int", "int", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_SHOTNUMBER.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LgeRecordV1_2)record).shotnumber;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LgeRecordV1_2)record).shotnumber;
    }
    });
    FIELDS.add(RECORDATTR_LVISTIME = new RecordField("lvisTime", "lvisTime", "", ATTR_LVISTIME, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_LVISTIME.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LgeRecordV1_2)record).lvisTime;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LgeRecordV1_2)record).lvisTime;
    }
    });
    FIELDS.add(RECORDATTR_GLON = new RecordField("glon", "glon", "", ATTR_GLON, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_GLON.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LgeRecordV1_2)record).glon;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LgeRecordV1_2)record).glon;
    }
    });
    FIELDS.add(RECORDATTR_GLAT = new RecordField("glat", "glat", "", ATTR_GLAT, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_GLAT.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LgeRecordV1_2)record).glat;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LgeRecordV1_2)record).glat;
    }
    });
    FIELDS.add(RECORDATTR_ZG = new RecordField("zg", "zg", "", ATTR_ZG, "", "float", "float", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_ZG.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LgeRecordV1_2)record).zg;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LgeRecordV1_2)record).zg;
    }
    });
    FIELDS.add(RECORDATTR_RH25 = new RecordField("rh25", "rh25", "", ATTR_RH25, "", "float", "float", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_RH25.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LgeRecordV1_2)record).rh25;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LgeRecordV1_2)record).rh25;
    }
    });
    FIELDS.add(RECORDATTR_RH50 = new RecordField("rh50", "rh50", "", ATTR_RH50, "", "float", "float", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_RH50.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LgeRecordV1_2)record).rh50;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LgeRecordV1_2)record).rh50;
    }
    });
    FIELDS.add(RECORDATTR_RH75 = new RecordField("rh75", "rh75", "", ATTR_RH75, "", "float", "float", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_RH75.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LgeRecordV1_2)record).rh75;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LgeRecordV1_2)record).rh75;
    }
    });
    FIELDS.add(RECORDATTR_RH100 = new RecordField("rh100", "rh100", "", ATTR_RH100, "", "float", "float", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_RH100.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LgeRecordV1_2)record).rh100;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LgeRecordV1_2)record).rh100;
    }
    });
    
    }
    

    int lfid;
    int shotnumber;
    double glon;
    double glat;
    float zg;
    float rh25;
    float rh50;
    float rh75;
    float rh100;
    

    public  LgeRecordV1_2(LgeRecordV1_2 that)  {
        super(that);
        this.lfid = that.lfid;
        this.shotnumber = that.shotnumber;
        this.lvisTime = that.lvisTime;
        this.glon = that.glon;
        this.glat = that.glat;
        this.zg = that.zg;
        this.rh25 = that.rh25;
        this.rh50 = that.rh50;
        this.rh75 = that.rh75;
        this.rh100 = that.rh100;
        
        
    }



    public  LgeRecordV1_2(RecordFile file)  {
        super(file);
    }



    public  LgeRecordV1_2(RecordFile file, boolean bigEndian)  {
        super(file, bigEndian);
    }



    public int getLastAttribute()  {
        return ATTR_LAST;
    }



    public  boolean equals(Object object)  {
        if(!super.equals(object)) {System.err.println("bad super"); return false;} if(!(object instanceof LgeRecordV1_2)) return false;
        LgeRecordV1_2 that = (LgeRecordV1_2 ) object;
        if(this.lfid!= that.lfid) {System.err.println("bad lfid");  return false;}
        if(this.shotnumber!= that.shotnumber) {System.err.println("bad shotnumber");  return false;}
        if(this.lvisTime!= that.lvisTime) {System.err.println("bad lvisTime");  return false;}
        if(this.glon!= that.glon) {System.err.println("bad glon");  return false;}
        if(this.glat!= that.glat) {System.err.println("bad glat");  return false;}
        if(this.zg!= that.zg) {System.err.println("bad zg");  return false;}
        if(this.rh25!= that.rh25) {System.err.println("bad rh25");  return false;}
        if(this.rh50!= that.rh50) {System.err.println("bad rh50");  return false;}
        if(this.rh75!= that.rh75) {System.err.println("bad rh75");  return false;}
        if(this.rh100!= that.rh100) {System.err.println("bad rh100");  return false;}
        return true;
    }




    public float[] getAltitudes() {
        return new float[]{zg,zg+rh25,zg+rh50,zg+rh75,zg+rh100};
    }

    protected void addFields(List<RecordField> fields)  {
        super.addFields(fields);
        fields.addAll(FIELDS);
    }



    public double getValue(int attrId)  {
        if(attrId == ATTR_LFID) return lfid;
        if(attrId == ATTR_SHOTNUMBER) return shotnumber;
        if(attrId == ATTR_LVISTIME) return lvisTime;
        if(attrId == ATTR_GLON) return glon;
        if(attrId == ATTR_GLAT) return glat;
        if(attrId == ATTR_ZG) return zg;
        if(attrId == ATTR_RH25) return rh25;
        if(attrId == ATTR_RH50) return rh50;
        if(attrId == ATTR_RH75) return rh75;
        if(attrId == ATTR_RH100) return rh100;
        return super.getValue(attrId);
        
    }



    public int getRecordSize()  {
        return super.getRecordSize() + 52;
    }



    public BaseRecord.ReadStatus read(RecordIO recordIO) throws Exception  {
        DataInputStream dis = recordIO.getDataInputStream();
        BaseRecord.ReadStatus status= super.read(recordIO);
        if(status!=BaseRecord.ReadStatus.OK)  return status;
        if(!getQuickScan()) {
        lfid =  readInt(dis);
        shotnumber =  readInt(dis);
        lvisTime =  readDouble(dis);
        } else {
        recordIO.getDataInputStream().skipBytes(16);
        }
        glon =  readDouble(dis);
        setLongitude(org.ramadda.util.GeoUtils.normalizeLongitude(glon));
        glat =  readDouble(dis);
        setLatitude(glat);
        zg =  readFloat(dis);
        setAltitude(zg);
        if(!getQuickScan()) {
        rh25 =  readFloat(dis);
        rh50 =  readFloat(dis);
        rh75 =  readFloat(dis);
        rh100 =  readFloat(dis);
        } else {
        recordIO.getDataInputStream().skipBytes(16);
        }
        
        
        return BaseRecord.ReadStatus.OK;
    }



    public void write(RecordIO recordIO) throws IOException  {
        DataOutputStream dos = recordIO.getDataOutputStream();
        super.write(recordIO);
        writeInt(dos, lfid);
        writeInt(dos, shotnumber);
        writeDouble(dos, lvisTime);
        writeDouble(dos, glon);
        writeDouble(dos, glat);
        writeFloat(dos, zg);
        writeFloat(dos, rh25);
        writeFloat(dos, rh50);
        writeFloat(dos, rh75);
        writeFloat(dos, rh100);
        
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
        pw.print(getStringValue(RECORDATTR_GLON, glon));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_GLAT, glat));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_ZG, zg));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_RH25, rh25));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_RH50, rh50));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_RH75, rh75));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_RH100, rh100));
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
        RECORDATTR_GLON.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_GLAT.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_ZG.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_RH25.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_RH50.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_RH75.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_RH100.printCsvHeader(visitInfo,pw);
        myCnt++;
        return myCnt+superCnt;
        
    }



    public void print(Appendable buff)  throws Exception  {
        super.print(buff);
        buff.append(" lfid: " + lfid+" \n");
        buff.append(" shotnumber: " + shotnumber+" \n");
        buff.append(" lvisTime: " + lvisTime+" \n");
        buff.append(" glon: " + glon+" \n");
        buff.append(" glat: " + glat+" \n");
        buff.append(" zg: " + zg+" \n");
        buff.append(" rh25: " + rh25+" \n");
        buff.append(" rh50: " + rh50+" \n");
        buff.append(" rh75: " + rh75+" \n");
        buff.append(" rh100: " + rh100+" \n");
        
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


    public double getGlon()  {
        return glon;
    }


    public void setGlon(double newValue)  {
        glon = newValue;
    }


    public double getGlat()  {
        return glat;
    }


    public void setGlat(double newValue)  {
        glat = newValue;
    }


    public float getZg()  {
        return zg;
    }


    public void setZg(float newValue)  {
        zg = newValue;
    }


    public float getRh25()  {
        return rh25;
    }


    public void setRh25(float newValue)  {
        rh25 = newValue;
    }


    public float getRh50()  {
        return rh50;
    }


    public void setRh50(float newValue)  {
        rh50 = newValue;
    }


    public float getRh75()  {
        return rh75;
    }


    public void setRh75(float newValue)  {
        rh75 = newValue;
    }


    public float getRh100()  {
        return rh100;
    }


    public void setRh100(float newValue)  {
        rh100 = newValue;
    }



}



