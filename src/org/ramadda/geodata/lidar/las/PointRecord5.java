
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
public class PointRecord5 extends PointRecord3 {
    public static final int ATTR_FIRST = PointRecord3.ATTR_LAST;
    public static final List<RecordField> FIELDS = new ArrayList<RecordField>();
    public static final int ATTR_WAVEPACKETDESCRIPTORINDEX =  ATTR_FIRST + 1;
    public static final RecordField RECORDATTR_WAVEPACKETDESCRIPTORINDEX;
    public static final int ATTR_BYTEOFFSETTOWAVEFORMDATA =  ATTR_FIRST + 2;
    public static final RecordField RECORDATTR_BYTEOFFSETTOWAVEFORMDATA;
    public static final int ATTR_WAVEFORMPACKETSIZEINBYTES =  ATTR_FIRST + 3;
    public static final RecordField RECORDATTR_WAVEFORMPACKETSIZEINBYTES;
    public static final int ATTR_RETURNPOINTWAVEFORMLOCATION =  ATTR_FIRST + 4;
    public static final RecordField RECORDATTR_RETURNPOINTWAVEFORMLOCATION;
    public static final int ATTR_XT =  ATTR_FIRST + 5;
    public static final RecordField RECORDATTR_XT;
    public static final int ATTR_YT =  ATTR_FIRST + 6;
    public static final RecordField RECORDATTR_YT;
    public static final int ATTR_ZT =  ATTR_FIRST + 7;
    public static final RecordField RECORDATTR_ZT;
    public static final int ATTR_LAST = ATTR_FIRST + 8;
    

    static {
    FIELDS.add(RECORDATTR_WAVEPACKETDESCRIPTORINDEX = new RecordField("wavePacketDescriptorIndex", "wavePacketDescriptorIndex", "", ATTR_WAVEPACKETDESCRIPTORINDEX, "", "byte", "byte", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_WAVEPACKETDESCRIPTORINDEX.setValueGetter(new ValueGetter() {
    public double getValue(Record record, RecordField field, VisitInfo visitInfo) {
    return (double) ((PointRecord5)record).wavePacketDescriptorIndex;
    }
    public String getStringValue(Record record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((PointRecord5)record).wavePacketDescriptorIndex;
    }
    });
    FIELDS.add(RECORDATTR_BYTEOFFSETTOWAVEFORMDATA = new RecordField("byteOffsetToWaveformData", "byteOffsetToWaveformData", "", ATTR_BYTEOFFSETTOWAVEFORMDATA, "", "long", "long", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_BYTEOFFSETTOWAVEFORMDATA.setValueGetter(new ValueGetter() {
    public double getValue(Record record, RecordField field, VisitInfo visitInfo) {
    return (double) ((PointRecord5)record).byteOffsetToWaveformData;
    }
    public String getStringValue(Record record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((PointRecord5)record).byteOffsetToWaveformData;
    }
    });
    FIELDS.add(RECORDATTR_WAVEFORMPACKETSIZEINBYTES = new RecordField("waveformPacketSizeInBytes", "waveformPacketSizeInBytes", "", ATTR_WAVEFORMPACKETSIZEINBYTES, "", "uint", "int", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_WAVEFORMPACKETSIZEINBYTES.setValueGetter(new ValueGetter() {
    public double getValue(Record record, RecordField field, VisitInfo visitInfo) {
    return (double) ((PointRecord5)record).waveformPacketSizeInBytes;
    }
    public String getStringValue(Record record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((PointRecord5)record).waveformPacketSizeInBytes;
    }
    });
    FIELDS.add(RECORDATTR_RETURNPOINTWAVEFORMLOCATION = new RecordField("returnPointWaveformLocation", "returnPointWaveformLocation", "", ATTR_RETURNPOINTWAVEFORMLOCATION, "", "float", "float", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_RETURNPOINTWAVEFORMLOCATION.setValueGetter(new ValueGetter() {
    public double getValue(Record record, RecordField field, VisitInfo visitInfo) {
    return (double) ((PointRecord5)record).returnPointWaveformLocation;
    }
    public String getStringValue(Record record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((PointRecord5)record).returnPointWaveformLocation;
    }
    });
    FIELDS.add(RECORDATTR_XT = new RecordField("Xt", "Xt", "", ATTR_XT, "", "float", "float", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_XT.setValueGetter(new ValueGetter() {
    public double getValue(Record record, RecordField field, VisitInfo visitInfo) {
    return (double) ((PointRecord5)record).Xt;
    }
    public String getStringValue(Record record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((PointRecord5)record).Xt;
    }
    });
    FIELDS.add(RECORDATTR_YT = new RecordField("Yt", "Yt", "", ATTR_YT, "", "float", "float", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_YT.setValueGetter(new ValueGetter() {
    public double getValue(Record record, RecordField field, VisitInfo visitInfo) {
    return (double) ((PointRecord5)record).Yt;
    }
    public String getStringValue(Record record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((PointRecord5)record).Yt;
    }
    });
    FIELDS.add(RECORDATTR_ZT = new RecordField("Zt", "Zt", "", ATTR_ZT, "", "float", "float", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_ZT.setValueGetter(new ValueGetter() {
    public double getValue(Record record, RecordField field, VisitInfo visitInfo) {
    return (double) ((PointRecord5)record).Zt;
    }
    public String getStringValue(Record record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((PointRecord5)record).Zt;
    }
    });
    
    }
    

    byte wavePacketDescriptorIndex;
    long byteOffsetToWaveformData;
    long waveformPacketSizeInBytes;
    float returnPointWaveformLocation;
    float Xt;
    float Yt;
    float Zt;
    

    public  PointRecord5(PointRecord5 that)  {
        super(that);
        this.wavePacketDescriptorIndex = that.wavePacketDescriptorIndex;
        this.byteOffsetToWaveformData = that.byteOffsetToWaveformData;
        this.waveformPacketSizeInBytes = that.waveformPacketSizeInBytes;
        this.returnPointWaveformLocation = that.returnPointWaveformLocation;
        this.Xt = that.Xt;
        this.Yt = that.Yt;
        this.Zt = that.Zt;
        
        
    }



    public  PointRecord5(RecordFile file)  {
        super(file);
    }



    public  PointRecord5(RecordFile file, boolean bigEndian)  {
        super(file, bigEndian);
    }



    public int getLastAttribute()  {
        return ATTR_LAST;
    }



    public  boolean equals(Object object)  {
        if(!super.equals(object)) {System.err.println("bad super"); return false;} if(!(object instanceof PointRecord5)) return false;
        PointRecord5 that = (PointRecord5 ) object;
        if(this.wavePacketDescriptorIndex!= that.wavePacketDescriptorIndex) {System.err.println("bad wavePacketDescriptorIndex");  return false;}
        if(this.byteOffsetToWaveformData!= that.byteOffsetToWaveformData) {System.err.println("bad byteOffsetToWaveformData");  return false;}
        if(this.waveformPacketSizeInBytes!= that.waveformPacketSizeInBytes) {System.err.println("bad waveformPacketSizeInBytes");  return false;}
        if(this.returnPointWaveformLocation!= that.returnPointWaveformLocation) {System.err.println("bad returnPointWaveformLocation");  return false;}
        if(this.Xt!= that.Xt) {System.err.println("bad Xt");  return false;}
        if(this.Yt!= that.Yt) {System.err.println("bad Yt");  return false;}
        if(this.Zt!= that.Zt) {System.err.println("bad Zt");  return false;}
        return true;
    }




    protected void addFields(List<RecordField> fields)  {
        super.addFields(fields);
        fields.addAll(FIELDS);
    }



    public double getValue(int attrId)  {
        if(attrId == ATTR_WAVEPACKETDESCRIPTORINDEX) return wavePacketDescriptorIndex;
        if(attrId == ATTR_BYTEOFFSETTOWAVEFORMDATA) return byteOffsetToWaveformData;
        if(attrId == ATTR_WAVEFORMPACKETSIZEINBYTES) return waveformPacketSizeInBytes;
        if(attrId == ATTR_RETURNPOINTWAVEFORMLOCATION) return returnPointWaveformLocation;
        if(attrId == ATTR_XT) return Xt;
        if(attrId == ATTR_YT) return Yt;
        if(attrId == ATTR_ZT) return Zt;
        return super.getValue(attrId);
        
    }



    public int getRecordSize()  {
        return super.getRecordSize() + 29;
    }



    public ReadStatus read(RecordIO recordIO) throws Exception  {
        DataInputStream dis = recordIO.getDataInputStream();
        ReadStatus status= super.read(recordIO);
        if(status!=ReadStatus.OK)  return status;
        wavePacketDescriptorIndex =  readByte(dis);
        byteOffsetToWaveformData =  readLong(dis);
        waveformPacketSizeInBytes =  readUnsignedInt(dis);
        returnPointWaveformLocation =  readFloat(dis);
        Xt =  readFloat(dis);
        Yt =  readFloat(dis);
        Zt =  readFloat(dis);
        
        
        return ReadStatus.OK;
    }



    public void write(RecordIO recordIO) throws IOException  {
        DataOutputStream dos = recordIO.getDataOutputStream();
        super.write(recordIO);
        writeByte(dos, wavePacketDescriptorIndex);
        writeLong(dos, byteOffsetToWaveformData);
        writeUnsignedInt(dos, waveformPacketSizeInBytes);
        writeFloat(dos, returnPointWaveformLocation);
        writeFloat(dos, Xt);
        writeFloat(dos, Yt);
        writeFloat(dos, Zt);
        
    }



    public int doPrintCsv(VisitInfo visitInfo,PrintWriter pw)  {
        boolean includeVector = visitInfo.getProperty(PROP_INCLUDEVECTOR, false);
        int superCnt = super.doPrintCsv(visitInfo,pw);
        int myCnt = 0;
        if(superCnt>0) pw.print(',');
        pw.print(getStringValue(RECORDATTR_WAVEPACKETDESCRIPTORINDEX, wavePacketDescriptorIndex));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_BYTEOFFSETTOWAVEFORMDATA, byteOffsetToWaveformData));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_WAVEFORMPACKETSIZEINBYTES, waveformPacketSizeInBytes));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_RETURNPOINTWAVEFORMLOCATION, returnPointWaveformLocation));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_XT, Xt));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_YT, Yt));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_ZT, Zt));
        myCnt++;
        return myCnt+superCnt;
        
    }



    public int doPrintCsvHeader(VisitInfo visitInfo,PrintWriter pw)  {
        int superCnt = super.doPrintCsvHeader(visitInfo,pw);
        int myCnt = 0;
        boolean includeVector = visitInfo.getProperty(PROP_INCLUDEVECTOR, false);
        if(superCnt>0) pw.print(',');
        RECORDATTR_WAVEPACKETDESCRIPTORINDEX.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_BYTEOFFSETTOWAVEFORMDATA.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_WAVEFORMPACKETSIZEINBYTES.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_RETURNPOINTWAVEFORMLOCATION.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_XT.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_YT.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_ZT.printCsvHeader(visitInfo,pw);
        myCnt++;
        return myCnt+superCnt;
        
    }



    public void print(Appendable buff)  throws Exception  {
        super.print(buff);
        buff.append(" wavePacketDescriptorIndex: " + wavePacketDescriptorIndex+" \n");
        buff.append(" byteOffsetToWaveformData: " + byteOffsetToWaveformData+" \n");
        buff.append(" waveformPacketSizeInBytes: " + waveformPacketSizeInBytes+" \n");
        buff.append(" returnPointWaveformLocation: " + returnPointWaveformLocation+" \n");
        buff.append(" Xt: " + Xt+" \n");
        buff.append(" Yt: " + Yt+" \n");
        buff.append(" Zt: " + Zt+" \n");
        
    }



    public byte getWavePacketDescriptorIndex()  {
        return wavePacketDescriptorIndex;
    }


    public void setWavePacketDescriptorIndex(byte newValue)  {
        wavePacketDescriptorIndex = newValue;
    }


    public long getByteOffsetToWaveformData()  {
        return byteOffsetToWaveformData;
    }


    public void setByteOffsetToWaveformData(long newValue)  {
        byteOffsetToWaveformData = newValue;
    }


    public long getWaveformPacketSizeInBytes()  {
        return waveformPacketSizeInBytes;
    }


    public void setWaveformPacketSizeInBytes(long newValue)  {
        waveformPacketSizeInBytes = newValue;
    }


    public float getReturnPointWaveformLocation()  {
        return returnPointWaveformLocation;
    }


    public void setReturnPointWaveformLocation(float newValue)  {
        returnPointWaveformLocation = newValue;
    }


    public float getXt()  {
        return Xt;
    }


    public void setXt(float newValue)  {
        Xt = newValue;
    }


    public float getYt()  {
        return Yt;
    }


    public void setYt(float newValue)  {
        Yt = newValue;
    }


    public float getZt()  {
        return Zt;
    }


    public void setZt(float newValue)  {
        Zt = newValue;
    }



}



