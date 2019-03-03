
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
public class LgwRecordV1_2 extends LvisRecord {
    public static final int ATTR_FIRST = LvisRecord.ATTR_LAST;
    public static final List<RecordField> FIELDS = new ArrayList<RecordField>();
    public static final int ATTR_LFID =  ATTR_FIRST + 1;
    public static final RecordField RECORDATTR_LFID;
    public static final int ATTR_SHOTNUMBER =  ATTR_FIRST + 2;
    public static final RecordField RECORDATTR_SHOTNUMBER;
    public static final int ATTR_LVISTIME =  ATTR_FIRST + 3;
    public static final RecordField RECORDATTR_LVISTIME;
    public static final int ATTR_LON0 =  ATTR_FIRST + 4;
    public static final RecordField RECORDATTR_LON0;
    public static final int ATTR_LAT0 =  ATTR_FIRST + 5;
    public static final RecordField RECORDATTR_LAT0;
    public static final int ATTR_Z0 =  ATTR_FIRST + 6;
    public static final RecordField RECORDATTR_Z0;
    public static final int ATTR_LON431 =  ATTR_FIRST + 7;
    public static final RecordField RECORDATTR_LON431;
    public static final int ATTR_LAT431 =  ATTR_FIRST + 8;
    public static final RecordField RECORDATTR_LAT431;
    public static final int ATTR_Z431 =  ATTR_FIRST + 9;
    public static final RecordField RECORDATTR_Z431;
    public static final int ATTR_SIGMEAN =  ATTR_FIRST + 10;
    public static final RecordField RECORDATTR_SIGMEAN;
    public static final int ATTR_RETURNWAVEFORM =  ATTR_FIRST + 11;
    public static final RecordField RECORDATTR_RETURNWAVEFORM;
    public static final int ATTR_LAST = ATTR_FIRST + 12;
    

    static {
    FIELDS.add(RECORDATTR_LFID = new RecordField("lfid", "lfid", "", ATTR_LFID, "", "int", "int", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_LFID.setValueGetter(new ValueGetter() {
    public double getValue(Record record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LgwRecordV1_2)record).lfid;
    }
    public String getStringValue(Record record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LgwRecordV1_2)record).lfid;
    }
    });
    FIELDS.add(RECORDATTR_SHOTNUMBER = new RecordField("shotnumber", "shotnumber", "", ATTR_SHOTNUMBER, "", "int", "int", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_SHOTNUMBER.setValueGetter(new ValueGetter() {
    public double getValue(Record record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LgwRecordV1_2)record).shotnumber;
    }
    public String getStringValue(Record record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LgwRecordV1_2)record).shotnumber;
    }
    });
    FIELDS.add(RECORDATTR_LVISTIME = new RecordField("lvisTime", "lvisTime", "", ATTR_LVISTIME, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_LVISTIME.setValueGetter(new ValueGetter() {
    public double getValue(Record record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LgwRecordV1_2)record).lvisTime;
    }
    public String getStringValue(Record record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LgwRecordV1_2)record).lvisTime;
    }
    });
    FIELDS.add(RECORDATTR_LON0 = new RecordField("lon0", "Lon. waveform top", "Longitude of the highest sample of the waveform ", ATTR_LON0, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_LON0.setValueGetter(new ValueGetter() {
    public double getValue(Record record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LgwRecordV1_2)record).lon0;
    }
    public String getStringValue(Record record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LgwRecordV1_2)record).lon0;
    }
    });
    FIELDS.add(RECORDATTR_LAT0 = new RecordField("lat0", "Lat. waveform top", "Latitude of the highest sample of the waveform ", ATTR_LAT0, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_LAT0.setValueGetter(new ValueGetter() {
    public double getValue(Record record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LgwRecordV1_2)record).lat0;
    }
    public String getStringValue(Record record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LgwRecordV1_2)record).lat0;
    }
    });
    FIELDS.add(RECORDATTR_Z0 = new RecordField("z0", "Waveform top", "Elevation of the highest sample of the waveform", ATTR_Z0, "m", "float", "float", 0, SEARCHABLE_YES,CHARTABLE_YES));
    RECORDATTR_Z0.setValueGetter(new ValueGetter() {
    public double getValue(Record record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LgwRecordV1_2)record).z0;
    }
    public String getStringValue(Record record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LgwRecordV1_2)record).z0;
    }
    });
    FIELDS.add(RECORDATTR_LON431 = new RecordField("lon431", "Lon. waveform bottom", "Longitude of the lowest sample of the waveform ", ATTR_LON431, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_LON431.setValueGetter(new ValueGetter() {
    public double getValue(Record record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LgwRecordV1_2)record).lon431;
    }
    public String getStringValue(Record record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LgwRecordV1_2)record).lon431;
    }
    });
    FIELDS.add(RECORDATTR_LAT431 = new RecordField("lat431", "Lat. waveform bottom", "Latitude of the lowest sample of the waveform ", ATTR_LAT431, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_LAT431.setValueGetter(new ValueGetter() {
    public double getValue(Record record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LgwRecordV1_2)record).lat431;
    }
    public String getStringValue(Record record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LgwRecordV1_2)record).lat431;
    }
    });
    FIELDS.add(RECORDATTR_Z431 = new RecordField("z431", "Waveform bottom", "Elevation of the lowest sample of the waveform ", ATTR_Z431, "m", "float", "float", 0, SEARCHABLE_YES,CHARTABLE_YES));
    RECORDATTR_Z431.setValueGetter(new ValueGetter() {
    public double getValue(Record record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LgwRecordV1_2)record).z431;
    }
    public String getStringValue(Record record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LgwRecordV1_2)record).z431;
    }
    });
    FIELDS.add(RECORDATTR_SIGMEAN = new RecordField("sigmean", "Signal Mean", "", ATTR_SIGMEAN, "", "float", "float", 0, SEARCHABLE_YES,CHARTABLE_YES));
    RECORDATTR_SIGMEAN.setValueGetter(new ValueGetter() {
    public double getValue(Record record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LgwRecordV1_2)record).sigmean;
    }
    public String getStringValue(Record record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LgwRecordV1_2)record).sigmean;
    }
    });
    FIELDS.add(RECORDATTR_RETURNWAVEFORM = new RecordField("returnWaveform", "Return waveform", "", ATTR_RETURNWAVEFORM, "", "ubyte[432]", "byte", 432, SEARCHABLE_NO,CHARTABLE_NO));
    
    }
    

    int lfid;
    int shotnumber;
    double lon0;
    double lat0;
    float z0;
    double lon431;
    double lat431;
    float z431;
    float sigmean;
    short[] returnWaveform = new short[432];
    

    public  LgwRecordV1_2(LgwRecordV1_2 that)  {
        super(that);
        this.lfid = that.lfid;
        this.shotnumber = that.shotnumber;
        this.lvisTime = that.lvisTime;
        this.lon0 = that.lon0;
        this.lat0 = that.lat0;
        this.z0 = that.z0;
        this.lon431 = that.lon431;
        this.lat431 = that.lat431;
        this.z431 = that.z431;
        this.sigmean = that.sigmean;
        this.returnWaveform = that.returnWaveform;
        
        
    }



    public  LgwRecordV1_2(RecordFile file)  {
        super(file);
    }



    public  LgwRecordV1_2(RecordFile file, boolean bigEndian)  {
        super(file, bigEndian);
    }



    public int getLastAttribute()  {
        return ATTR_LAST;
    }



    public  boolean equals(Object object)  {
        if(!super.equals(object)) {System.err.println("bad super"); return false;} if(!(object instanceof LgwRecordV1_2)) return false;
        LgwRecordV1_2 that = (LgwRecordV1_2 ) object;
        if(this.lfid!= that.lfid) {System.err.println("bad lfid");  return false;}
        if(this.shotnumber!= that.shotnumber) {System.err.println("bad shotnumber");  return false;}
        if(this.lvisTime!= that.lvisTime) {System.err.println("bad lvisTime");  return false;}
        if(this.lon0!= that.lon0) {System.err.println("bad lon0");  return false;}
        if(this.lat0!= that.lat0) {System.err.println("bad lat0");  return false;}
        if(this.z0!= that.z0) {System.err.println("bad z0");  return false;}
        if(this.lon431!= that.lon431) {System.err.println("bad lon431");  return false;}
        if(this.lat431!= that.lat431) {System.err.println("bad lat431");  return false;}
        if(this.z431!= that.z431) {System.err.println("bad z431");  return false;}
        if(this.sigmean!= that.sigmean) {System.err.println("bad sigmean");  return false;}
        if(!java.util.Arrays.equals(this.returnWaveform, that.returnWaveform)) {System.err.println("bad returnWaveform"); return false;}
        return true;
    }




    public Waveform getWaveform(String name) {
        return new Waveform(toFloat(returnWaveform), new float[]{0,255},sigmean, z0,z431,lat0,lon0,lat431,lon431);
    }

    protected void addFields(List<RecordField> fields)  {
        super.addFields(fields);
        fields.addAll(FIELDS);
    }



    public double getValue(int attrId)  {
        if(attrId == ATTR_LFID) return lfid;
        if(attrId == ATTR_SHOTNUMBER) return shotnumber;
        if(attrId == ATTR_LVISTIME) return lvisTime;
        if(attrId == ATTR_LON0) return lon0;
        if(attrId == ATTR_LAT0) return lat0;
        if(attrId == ATTR_Z0) return z0;
        if(attrId == ATTR_LON431) return lon431;
        if(attrId == ATTR_LAT431) return lat431;
        if(attrId == ATTR_Z431) return z431;
        if(attrId == ATTR_SIGMEAN) return sigmean;
        return super.getValue(attrId);
        
    }



    public int getRecordSize()  {
        return super.getRecordSize() + 492;
    }



    public ReadStatus read(RecordIO recordIO) throws Exception  {
        DataInputStream dis = recordIO.getDataInputStream();
        ReadStatus status= super.read(recordIO);
        if(status!=ReadStatus.OK)  return status;
        if(!getQuickScan()) {
        lfid =  readInt(dis);
        shotnumber =  readInt(dis);
        } else {
        recordIO.getDataInputStream().skipBytes(8);
        }
        lvisTime =  readDouble(dis);
        lon0 =  readDouble(dis);
        setLongitude(org.ramadda.util.GeoUtils.normalizeLongitude(lon0));
        lat0 =  readDouble(dis);
        setLatitude(lat0);
        z0 =  readFloat(dis);
        if(!getQuickScan()) {
        lon431 =  readDouble(dis);
        lat431 =  readDouble(dis);
        z431 =  readFloat(dis);
        setAltitude(z431);
        sigmean =  readFloat(dis);
        readUnsignedBytes(dis,returnWaveform);
        } else {
        recordIO.getDataInputStream().skipBytes(456);
        }
        
        
        return ReadStatus.OK;
    }



    public void write(RecordIO recordIO) throws IOException  {
        DataOutputStream dos = recordIO.getDataOutputStream();
        super.write(recordIO);
        writeInt(dos, lfid);
        writeInt(dos, shotnumber);
        writeDouble(dos, lvisTime);
        writeDouble(dos, lon0);
        writeDouble(dos, lat0);
        writeFloat(dos, z0);
        writeDouble(dos, lon431);
        writeDouble(dos, lat431);
        writeFloat(dos, z431);
        writeFloat(dos, sigmean);
        writeUnsignedBytes(dos, returnWaveform);
        
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
        pw.print(getStringValue(RECORDATTR_LON0, lon0));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_LAT0, lat0));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_Z0, z0));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_LON431, lon431));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_LAT431, lat431));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_Z431, z431));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_SIGMEAN, sigmean));
        myCnt++;
        if(includeVector) {
        for(int i=0;i<this.returnWaveform.length;i++) {pw.print(i==0?'|':',');pw.print(this.returnWaveform[i]);}
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
        RECORDATTR_LON431.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_LAT431.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_Z431.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_SIGMEAN.printCsvHeader(visitInfo,pw);
        myCnt++;
        if(includeVector) {
        pw.print(',');
        RECORDATTR_RETURNWAVEFORM.printCsvHeader(visitInfo,pw);
        myCnt++;
        }
        return myCnt+superCnt;
        
    }



    public void print(Appendable buff)  throws Exception  {
        super.print(buff);
        buff.append(" lfid: " + lfid+" \n");
        buff.append(" shotnumber: " + shotnumber+" \n");
        buff.append(" lvisTime: " + lvisTime+" \n");
        buff.append(" lon0: " + lon0+" \n");
        buff.append(" lat0: " + lat0+" \n");
        buff.append(" z0: " + z0+" \n");
        buff.append(" lon431: " + lon431+" \n");
        buff.append(" lat431: " + lat431+" \n");
        buff.append(" z431: " + z431+" \n");
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


    public double getLon431()  {
        return lon431;
    }


    public void setLon431(double newValue)  {
        lon431 = newValue;
    }


    public double getLat431()  {
        return lat431;
    }


    public void setLat431(double newValue)  {
        lat431 = newValue;
    }


    public float getZ431()  {
        return z431;
    }


    public void setZ431(float newValue)  {
        z431 = newValue;
    }


    public float getSigmean()  {
        return sigmean;
    }


    public void setSigmean(float newValue)  {
        sigmean = newValue;
    }


    public short[] getReturnWaveform()  {
        return returnWaveform;
    }


    public void setReturnWaveform(short[] newValue)  {
        copy(returnWaveform, newValue);
    }



}



