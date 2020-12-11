
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
public class LgwRecordV1_4 extends LvisRecord {
    public static final int ATTR_FIRST = LvisRecord.ATTR_LAST;
    public static final List<RecordField> FIELDS = new ArrayList<RecordField>();
    public static final int ATTR_LFID =  ATTR_FIRST + 1;
    public static final RecordField RECORDATTR_LFID;
    public static final int ATTR_SHOTNUMBER =  ATTR_FIRST + 2;
    public static final RecordField RECORDATTR_SHOTNUMBER;
    public static final int ATTR_AZIMUTH =  ATTR_FIRST + 3;
    public static final RecordField RECORDATTR_AZIMUTH;
    public static final int ATTR_INCIDENTANGLE =  ATTR_FIRST + 4;
    public static final RecordField RECORDATTR_INCIDENTANGLE;
    public static final int ATTR_RANGE =  ATTR_FIRST + 5;
    public static final RecordField RECORDATTR_RANGE;
    public static final int ATTR_LVISTIME =  ATTR_FIRST + 6;
    public static final RecordField RECORDATTR_LVISTIME;
    public static final int ATTR_LON0 =  ATTR_FIRST + 7;
    public static final RecordField RECORDATTR_LON0;
    public static final int ATTR_LAT0 =  ATTR_FIRST + 8;
    public static final RecordField RECORDATTR_LAT0;
    public static final int ATTR_Z0 =  ATTR_FIRST + 9;
    public static final RecordField RECORDATTR_Z0;
    public static final int ATTR_LON527 =  ATTR_FIRST + 10;
    public static final RecordField RECORDATTR_LON527;
    public static final int ATTR_LAT527 =  ATTR_FIRST + 11;
    public static final RecordField RECORDATTR_LAT527;
    public static final int ATTR_Z527 =  ATTR_FIRST + 12;
    public static final RecordField RECORDATTR_Z527;
    public static final int ATTR_SIGMEAN =  ATTR_FIRST + 13;
    public static final RecordField RECORDATTR_SIGMEAN;
    public static final int ATTR_TXWAVE =  ATTR_FIRST + 14;
    public static final RecordField RECORDATTR_TXWAVE;
    public static final int ATTR_RXWAVE =  ATTR_FIRST + 15;
    public static final RecordField RECORDATTR_RXWAVE;
    public static final int ATTR_LAST = ATTR_FIRST + 16;
    

    static {
    FIELDS.add(RECORDATTR_LFID = new RecordField("lfid", "lfid", "", ATTR_LFID, "", "int", "int", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_LFID.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LgwRecordV1_4)record).lfid;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LgwRecordV1_4)record).lfid;
    }
    });
    FIELDS.add(RECORDATTR_SHOTNUMBER = new RecordField("shotnumber", "shotnumber", "", ATTR_SHOTNUMBER, "", "int", "int", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_SHOTNUMBER.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LgwRecordV1_4)record).shotnumber;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LgwRecordV1_4)record).shotnumber;
    }
    });
    FIELDS.add(RECORDATTR_AZIMUTH = new RecordField("azimuth", "azimuth", "", ATTR_AZIMUTH, "", "float", "float", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_AZIMUTH.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LgwRecordV1_4)record).azimuth;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LgwRecordV1_4)record).azimuth;
    }
    });
    FIELDS.add(RECORDATTR_INCIDENTANGLE = new RecordField("incidentangle", "incidentangle", "", ATTR_INCIDENTANGLE, "", "float", "float", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_INCIDENTANGLE.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LgwRecordV1_4)record).incidentangle;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LgwRecordV1_4)record).incidentangle;
    }
    });
    FIELDS.add(RECORDATTR_RANGE = new RecordField("range", "range", "", ATTR_RANGE, "", "float", "float", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_RANGE.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LgwRecordV1_4)record).range;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LgwRecordV1_4)record).range;
    }
    });
    FIELDS.add(RECORDATTR_LVISTIME = new RecordField("lvisTime", "lvisTime", "", ATTR_LVISTIME, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_LVISTIME.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LgwRecordV1_4)record).lvisTime;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LgwRecordV1_4)record).lvisTime;
    }
    });
    FIELDS.add(RECORDATTR_LON0 = new RecordField("lon0", "Lon. waveform top", "Longitude of the highest sample of the waveform ", ATTR_LON0, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_LON0.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LgwRecordV1_4)record).lon0;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LgwRecordV1_4)record).lon0;
    }
    });
    FIELDS.add(RECORDATTR_LAT0 = new RecordField("lat0", "Lat. waveform top", "Latitude of the highest sample of the waveform ", ATTR_LAT0, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_LAT0.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LgwRecordV1_4)record).lat0;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LgwRecordV1_4)record).lat0;
    }
    });
    FIELDS.add(RECORDATTR_Z0 = new RecordField("z0", "Waveform top", "Elevation of the highest sample of the waveform", ATTR_Z0, "m", "float", "float", 0, SEARCHABLE_YES,CHARTABLE_YES));
    RECORDATTR_Z0.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LgwRecordV1_4)record).z0;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LgwRecordV1_4)record).z0;
    }
    });
    FIELDS.add(RECORDATTR_LON527 = new RecordField("lon527", "Lon. waveform bottom", "Longitude of the lowest sample of the waveform", ATTR_LON527, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_LON527.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LgwRecordV1_4)record).lon527;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LgwRecordV1_4)record).lon527;
    }
    });
    FIELDS.add(RECORDATTR_LAT527 = new RecordField("lat527", "Lat. waveform bottom", "Latitude of the lowest sample of the waveform", ATTR_LAT527, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_LAT527.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LgwRecordV1_4)record).lat527;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LgwRecordV1_4)record).lat527;
    }
    });
    FIELDS.add(RECORDATTR_Z527 = new RecordField("z527", "Waveform bottom", "Elevation of the lowest sample of the waveform ", ATTR_Z527, "m", "float", "float", 0, SEARCHABLE_YES,CHARTABLE_YES));
    RECORDATTR_Z527.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LgwRecordV1_4)record).z527;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LgwRecordV1_4)record).z527;
    }
    });
    FIELDS.add(RECORDATTR_SIGMEAN = new RecordField("sigmean", "Signal Mean", "", ATTR_SIGMEAN, "", "float", "float", 0, SEARCHABLE_YES,CHARTABLE_YES));
    RECORDATTR_SIGMEAN.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LgwRecordV1_4)record).sigmean;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LgwRecordV1_4)record).sigmean;
    }
    });
    FIELDS.add(RECORDATTR_TXWAVE = new RecordField("txwave", "Transmit waveform", "", ATTR_TXWAVE, "", "ubyte[240]", "byte", 240, SEARCHABLE_NO,CHARTABLE_NO));
    FIELDS.add(RECORDATTR_RXWAVE = new RecordField("rxwave", "Return waveform", "", ATTR_RXWAVE, "", "ubyte[1056]", "byte", 1056, SEARCHABLE_NO,CHARTABLE_NO));
    
    }
    

    int lfid;
    int shotnumber;
    float azimuth;
    float incidentangle;
    float range;
    double lon0;
    double lat0;
    float z0;
    double lon527;
    double lat527;
    float z527;
    float sigmean;
    short[] txwave = new short[240];
    short[] rxwave = new short[1056];
    

    public  LgwRecordV1_4(LgwRecordV1_4 that)  {
        super(that);
        this.lfid = that.lfid;
        this.shotnumber = that.shotnumber;
        this.azimuth = that.azimuth;
        this.incidentangle = that.incidentangle;
        this.range = that.range;
        this.lvisTime = that.lvisTime;
        this.lon0 = that.lon0;
        this.lat0 = that.lat0;
        this.z0 = that.z0;
        this.lon527 = that.lon527;
        this.lat527 = that.lat527;
        this.z527 = that.z527;
        this.sigmean = that.sigmean;
        this.txwave = that.txwave;
        this.rxwave = that.rxwave;
        
        
    }



    public  LgwRecordV1_4(RecordFile file)  {
        super(file);
    }



    public  LgwRecordV1_4(RecordFile file, boolean bigEndian)  {
        super(file, bigEndian);
    }



    public int getLastAttribute()  {
        return ATTR_LAST;
    }



    public  boolean equals(Object object)  {
        if(!super.equals(object)) {System.err.println("bad super"); return false;} if(!(object instanceof LgwRecordV1_4)) return false;
        LgwRecordV1_4 that = (LgwRecordV1_4 ) object;
        if(this.lfid!= that.lfid) {System.err.println("bad lfid");  return false;}
        if(this.shotnumber!= that.shotnumber) {System.err.println("bad shotnumber");  return false;}
        if(this.azimuth!= that.azimuth) {System.err.println("bad azimuth");  return false;}
        if(this.incidentangle!= that.incidentangle) {System.err.println("bad incidentangle");  return false;}
        if(this.range!= that.range) {System.err.println("bad range");  return false;}
        if(this.lvisTime!= that.lvisTime) {System.err.println("bad lvisTime");  return false;}
        if(this.lon0!= that.lon0) {System.err.println("bad lon0");  return false;}
        if(this.lat0!= that.lat0) {System.err.println("bad lat0");  return false;}
        if(this.z0!= that.z0) {System.err.println("bad z0");  return false;}
        if(this.lon527!= that.lon527) {System.err.println("bad lon527");  return false;}
        if(this.lat527!= that.lat527) {System.err.println("bad lat527");  return false;}
        if(this.z527!= that.z527) {System.err.println("bad z527");  return false;}
        if(this.sigmean!= that.sigmean) {System.err.println("bad sigmean");  return false;}
        if(!java.util.Arrays.equals(this.txwave, that.txwave)) {System.err.println("bad txwave"); return false;}
        if(!java.util.Arrays.equals(this.rxwave, that.rxwave)) {System.err.println("bad rxwave"); return false;}
        return true;
    }




    public Waveform getWaveform(String name) {
        if(name == null|| name.length()==0 || name.equals(WAVEFORM_RETURN)) {
            return new Waveform(toFloat(rxwave), new float[]{0,255},sigmean, z0,z527,lat0,lon0,lat527,lon527);
        }
        return new Waveform(toFloat(txwave), new float[]{0,255},0);
    }

    protected void addFields(List<RecordField> fields)  {
        super.addFields(fields);
        fields.addAll(FIELDS);
    }



    public double getValue(int attrId)  {
        if(attrId == ATTR_LFID) return lfid;
        if(attrId == ATTR_SHOTNUMBER) return shotnumber;
        if(attrId == ATTR_AZIMUTH) return azimuth;
        if(attrId == ATTR_INCIDENTANGLE) return incidentangle;
        if(attrId == ATTR_RANGE) return range;
        if(attrId == ATTR_LVISTIME) return lvisTime;
        if(attrId == ATTR_LON0) return lon0;
        if(attrId == ATTR_LAT0) return lat0;
        if(attrId == ATTR_Z0) return z0;
        if(attrId == ATTR_LON527) return lon527;
        if(attrId == ATTR_LAT527) return lat527;
        if(attrId == ATTR_Z527) return z527;
        if(attrId == ATTR_SIGMEAN) return sigmean;
        return super.getValue(attrId);
        
    }



    public int getRecordSize()  {
        return super.getRecordSize() + 1368;
    }



    public BaseRecord.ReadStatus read(RecordIO recordIO) throws Exception  {
        DataInputStream dis = recordIO.getDataInputStream();
        BaseRecord.ReadStatus status= super.read(recordIO);
        if(status!=BaseRecord.ReadStatus.OK)  return status;
        if(!getQuickScan()) {
        lfid =  readInt(dis);
        shotnumber =  readInt(dis);
        azimuth =  readFloat(dis);
        incidentangle =  readFloat(dis);
        range =  readFloat(dis);
        } else {
        recordIO.getDataInputStream().skipBytes(20);
        }
        lvisTime =  readDouble(dis);
        lon0 =  readDouble(dis);
        setLongitude(org.ramadda.util.GeoUtils.normalizeLongitude(lon0));
        lat0 =  readDouble(dis);
        setLatitude(lat0);
        z0 =  readFloat(dis);
        lon527 =  readDouble(dis);
        lat527 =  readDouble(dis);
        z527 =  readFloat(dis);
        setAltitude(z527);
        sigmean =  readFloat(dis);
        if(!getQuickScan()) {
        readUnsignedBytes(dis,txwave);
        readUnsignedBytes(dis,rxwave);
        } else {
        recordIO.getDataInputStream().skipBytes(1296);
        }
        
        
        return BaseRecord.ReadStatus.OK;
    }



    public void write(RecordIO recordIO) throws IOException  {
        DataOutputStream dos = recordIO.getDataOutputStream();
        super.write(recordIO);
        writeInt(dos, lfid);
        writeInt(dos, shotnumber);
        writeFloat(dos, azimuth);
        writeFloat(dos, incidentangle);
        writeFloat(dos, range);
        writeDouble(dos, lvisTime);
        writeDouble(dos, lon0);
        writeDouble(dos, lat0);
        writeFloat(dos, z0);
        writeDouble(dos, lon527);
        writeDouble(dos, lat527);
        writeFloat(dos, z527);
        writeFloat(dos, sigmean);
        writeUnsignedBytes(dos, txwave);
        writeUnsignedBytes(dos, rxwave);
        
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
        pw.print(getStringValue(RECORDATTR_AZIMUTH, azimuth));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_INCIDENTANGLE, incidentangle));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_RANGE, range));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_LVISTIME, lvisTime));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_LON0, lon0));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_LAT0, lat0));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_Z0, z0));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_LON527, lon527));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_LAT527, lat527));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_Z527, z527));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_SIGMEAN, sigmean));
        myCnt++;
        if(includeVector) {
        for(int i=0;i<this.txwave.length;i++) {pw.print(i==0?'|':',');pw.print(this.txwave[i]);}
        myCnt++;
        }
        if(includeVector) {
        for(int i=0;i<this.rxwave.length;i++) {pw.print(i==0?'|':',');pw.print(this.rxwave[i]);}
        myCnt++;
        }
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
        RECORDATTR_AZIMUTH.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_INCIDENTANGLE.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_RANGE.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_LVISTIME.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_LON0.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_LAT0.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_Z0.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_LON527.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_LAT527.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_Z527.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_SIGMEAN.printCsvHeader(visitInfo,pw);
        myCnt++;
        if(includeVector) {
        pw.print(',');
        RECORDATTR_TXWAVE.printCsvHeader(visitInfo,pw);
        myCnt++;
        }
        if(includeVector) {
        pw.print(',');
        RECORDATTR_RXWAVE.printCsvHeader(visitInfo,pw);
        myCnt++;
        }
        return myCnt+superCnt;
        
    }



    public void print(Appendable buff)  throws Exception  {
        super.print(buff);
        buff.append(" lfid: " + lfid+" \n");
        buff.append(" shotnumber: " + shotnumber+" \n");
        buff.append(" azimuth: " + azimuth+" \n");
        buff.append(" incidentangle: " + incidentangle+" \n");
        buff.append(" range: " + range+" \n");
        buff.append(" lvisTime: " + lvisTime+" \n");
        buff.append(" lon0: " + lon0+" \n");
        buff.append(" lat0: " + lat0+" \n");
        buff.append(" z0: " + z0+" \n");
        buff.append(" lon527: " + lon527+" \n");
        buff.append(" lat527: " + lat527+" \n");
        buff.append(" z527: " + z527+" \n");
        buff.append(" sigmean: " + sigmean+" \n");
        
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


    public float getAzimuth()  {
        return azimuth;
    }


    public void setAzimuth(float newValue)  {
        azimuth = newValue;
    }


    public float getIncidentangle()  {
        return incidentangle;
    }


    public void setIncidentangle(float newValue)  {
        incidentangle = newValue;
    }


    public float getRange()  {
        return range;
    }


    public void setRange(float newValue)  {
        range = newValue;
    }


    public double getLon0()  {
        return lon0;
    }


    public void setLon0(double newValue)  {
        lon0 = newValue;
    }


    public double getLat0()  {
        return lat0;
    }


    public void setLat0(double newValue)  {
        lat0 = newValue;
    }


    public float getZ0()  {
        return z0;
    }


    public void setZ0(float newValue)  {
        z0 = newValue;
    }


    public double getLon527()  {
        return lon527;
    }


    public void setLon527(double newValue)  {
        lon527 = newValue;
    }


    public double getLat527()  {
        return lat527;
    }


    public void setLat527(double newValue)  {
        lat527 = newValue;
    }


    public float getZ527()  {
        return z527;
    }


    public void setZ527(float newValue)  {
        z527 = newValue;
    }


    public float getSigmean()  {
        return sigmean;
    }


    public void setSigmean(float newValue)  {
        sigmean = newValue;
    }


    public short[] getTxwave()  {
        return txwave;
    }


    public void setTxwave(short[] newValue)  {
        copy(txwave, newValue);
    }


    public short[] getRxwave()  {
        return rxwave;
    }


    public void setRxwave(short[] newValue)  {
        copy(rxwave, newValue);
    }



}



