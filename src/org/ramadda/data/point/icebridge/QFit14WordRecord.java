
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



package org.ramadda.data.point.icebridge;

import org.ramadda.data.record.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;





/** This is generated code from generate.tcl. Do not edit it! */
public class QFit14WordRecord extends org.ramadda.data.point.icebridge.QfitRecord {
    public static final int ATTR_FIRST = org.ramadda.data.point.icebridge.QfitRecord.ATTR_LAST;
    public static final List<RecordField> FIELDS = new ArrayList<RecordField>();
    public static final int ATTR_RELATIVETIME =  ATTR_FIRST + 1;
    public static final RecordField RECORDATTR_RELATIVETIME;
    public static final int ATTR_LASERLATITUDE =  ATTR_FIRST + 2;
    public static final RecordField RECORDATTR_LASERLATITUDE;
    public static final int ATTR_LASERLONGITUDE =  ATTR_FIRST + 3;
    public static final RecordField RECORDATTR_LASERLONGITUDE;
    public static final int ATTR_ELEVATION =  ATTR_FIRST + 4;
    public static final RecordField RECORDATTR_ELEVATION;
    public static final int ATTR_STARTSIGNALSTRENGTH =  ATTR_FIRST + 5;
    public static final RecordField RECORDATTR_STARTSIGNALSTRENGTH;
    public static final int ATTR_REFLECTEDSIGNALSTRENGTH =  ATTR_FIRST + 6;
    public static final RecordField RECORDATTR_REFLECTEDSIGNALSTRENGTH;
    public static final int ATTR_AZIMUTH =  ATTR_FIRST + 7;
    public static final RecordField RECORDATTR_AZIMUTH;
    public static final int ATTR_PITCH =  ATTR_FIRST + 8;
    public static final RecordField RECORDATTR_PITCH;
    public static final int ATTR_ROLL =  ATTR_FIRST + 9;
    public static final RecordField RECORDATTR_ROLL;
    public static final int ATTR_PASSIVESIGNAL =  ATTR_FIRST + 10;
    public static final RecordField RECORDATTR_PASSIVESIGNAL;
    public static final int ATTR_PASSIVELATITUDE =  ATTR_FIRST + 11;
    public static final RecordField RECORDATTR_PASSIVELATITUDE;
    public static final int ATTR_PASSIVELONGITUDE =  ATTR_FIRST + 12;
    public static final RecordField RECORDATTR_PASSIVELONGITUDE;
    public static final int ATTR_PASSIVEELEVATION =  ATTR_FIRST + 13;
    public static final RecordField RECORDATTR_PASSIVEELEVATION;
    public static final int ATTR_GPSTIME =  ATTR_FIRST + 14;
    public static final RecordField RECORDATTR_GPSTIME;
    public static final int ATTR_LAST = ATTR_FIRST + 15;
    

    static {
    FIELDS.add(RECORDATTR_RELATIVETIME = new RecordField("relativeTime", "relativeTime", "", ATTR_RELATIVETIME, "", "int", "int", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_RELATIVETIME.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((QFit14WordRecord)record).relativeTime;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((QFit14WordRecord)record).relativeTime;
    }
    });
    FIELDS.add(RECORDATTR_LASERLATITUDE = new RecordField("laserLatitude", "laserLatitude", "", ATTR_LASERLATITUDE, "", "int", "int", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_LASERLATITUDE.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((QFit14WordRecord)record).laserLatitude;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((QFit14WordRecord)record).laserLatitude;
    }
    });
    FIELDS.add(RECORDATTR_LASERLONGITUDE = new RecordField("laserLongitude", "laserLongitude", "", ATTR_LASERLONGITUDE, "", "int", "int", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_LASERLONGITUDE.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((QFit14WordRecord)record).laserLongitude;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((QFit14WordRecord)record).laserLongitude;
    }
    });
    FIELDS.add(RECORDATTR_ELEVATION = new RecordField("elevation", "elevation", "", ATTR_ELEVATION, "mm", "int", "int", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_ELEVATION.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((QFit14WordRecord)record).elevation;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((QFit14WordRecord)record).elevation;
    }
    });
    FIELDS.add(RECORDATTR_STARTSIGNALSTRENGTH = new RecordField("startSignalStrength", "startSignalStrength", "", ATTR_STARTSIGNALSTRENGTH, "", "int", "int", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_STARTSIGNALSTRENGTH.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((QFit14WordRecord)record).startSignalStrength;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((QFit14WordRecord)record).startSignalStrength;
    }
    });
    FIELDS.add(RECORDATTR_REFLECTEDSIGNALSTRENGTH = new RecordField("reflectedSignalStrength", "reflectedSignalStrength", "", ATTR_REFLECTEDSIGNALSTRENGTH, "", "int", "int", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_REFLECTEDSIGNALSTRENGTH.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((QFit14WordRecord)record).reflectedSignalStrength;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((QFit14WordRecord)record).reflectedSignalStrength;
    }
    });
    FIELDS.add(RECORDATTR_AZIMUTH = new RecordField("azimuth", "azimuth", "", ATTR_AZIMUTH, "millidegree", "int", "int", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_AZIMUTH.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((QFit14WordRecord)record).azimuth;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((QFit14WordRecord)record).azimuth;
    }
    });
    FIELDS.add(RECORDATTR_PITCH = new RecordField("pitch", "pitch", "", ATTR_PITCH, "millidegree", "int", "int", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_PITCH.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((QFit14WordRecord)record).pitch;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((QFit14WordRecord)record).pitch;
    }
    });
    FIELDS.add(RECORDATTR_ROLL = new RecordField("roll", "roll", "", ATTR_ROLL, "millidegree", "int", "int", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_ROLL.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((QFit14WordRecord)record).roll;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((QFit14WordRecord)record).roll;
    }
    });
    FIELDS.add(RECORDATTR_PASSIVESIGNAL = new RecordField("passiveSignal", "passiveSignal", "", ATTR_PASSIVESIGNAL, "", "int", "int", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_PASSIVESIGNAL.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((QFit14WordRecord)record).passiveSignal;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((QFit14WordRecord)record).passiveSignal;
    }
    });
    FIELDS.add(RECORDATTR_PASSIVELATITUDE = new RecordField("passiveLatitude", "passiveLatitude", "", ATTR_PASSIVELATITUDE, "", "int", "int", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_PASSIVELATITUDE.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((QFit14WordRecord)record).passiveLatitude;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((QFit14WordRecord)record).passiveLatitude;
    }
    });
    FIELDS.add(RECORDATTR_PASSIVELONGITUDE = new RecordField("passiveLongitude", "passiveLongitude", "", ATTR_PASSIVELONGITUDE, "", "int", "int", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_PASSIVELONGITUDE.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((QFit14WordRecord)record).passiveLongitude;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((QFit14WordRecord)record).passiveLongitude;
    }
    });
    FIELDS.add(RECORDATTR_PASSIVEELEVATION = new RecordField("passiveElevation", "passiveElevation", "", ATTR_PASSIVEELEVATION, "", "int", "int", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_PASSIVEELEVATION.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((QFit14WordRecord)record).passiveElevation;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((QFit14WordRecord)record).passiveElevation;
    }
    });
    FIELDS.add(RECORDATTR_GPSTIME = new RecordField("gpsTime", "gpsTime", "", ATTR_GPSTIME, "", "int", "int", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_GPSTIME.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((QFit14WordRecord)record).gpsTime;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((QFit14WordRecord)record).gpsTime;
    }
    });
    
    }
    

    int startSignalStrength;
    int reflectedSignalStrength;
    int azimuth;
    int pitch;
    int roll;
    int passiveSignal;
    int passiveLatitude;
    int passiveLongitude;
    int passiveElevation;
    int gpsTime;
    

    public  QFit14WordRecord(QFit14WordRecord that)  {
        super(that);
        this.relativeTime = that.relativeTime;
        this.laserLatitude = that.laserLatitude;
        this.laserLongitude = that.laserLongitude;
        this.elevation = that.elevation;
        this.startSignalStrength = that.startSignalStrength;
        this.reflectedSignalStrength = that.reflectedSignalStrength;
        this.azimuth = that.azimuth;
        this.pitch = that.pitch;
        this.roll = that.roll;
        this.passiveSignal = that.passiveSignal;
        this.passiveLatitude = that.passiveLatitude;
        this.passiveLongitude = that.passiveLongitude;
        this.passiveElevation = that.passiveElevation;
        this.gpsTime = that.gpsTime;
        
        
    }



    public  QFit14WordRecord(RecordFile file)  {
        super(file);
    }



    public  QFit14WordRecord(RecordFile file, boolean bigEndian)  {
        super(file, bigEndian);
    }



    public int getLastAttribute()  {
        return ATTR_LAST;
    }



    public  boolean equals(Object object)  {
        if(!super.equals(object)) {System.err.println("bad super"); return false;} if(!(object instanceof QFit14WordRecord)) return false;
        QFit14WordRecord that = (QFit14WordRecord ) object;
        if(this.relativeTime!= that.relativeTime) {System.err.println("bad relativeTime");  return false;}
        if(this.laserLatitude!= that.laserLatitude) {System.err.println("bad laserLatitude");  return false;}
        if(this.laserLongitude!= that.laserLongitude) {System.err.println("bad laserLongitude");  return false;}
        if(this.elevation!= that.elevation) {System.err.println("bad elevation");  return false;}
        if(this.startSignalStrength!= that.startSignalStrength) {System.err.println("bad startSignalStrength");  return false;}
        if(this.reflectedSignalStrength!= that.reflectedSignalStrength) {System.err.println("bad reflectedSignalStrength");  return false;}
        if(this.azimuth!= that.azimuth) {System.err.println("bad azimuth");  return false;}
        if(this.pitch!= that.pitch) {System.err.println("bad pitch");  return false;}
        if(this.roll!= that.roll) {System.err.println("bad roll");  return false;}
        if(this.passiveSignal!= that.passiveSignal) {System.err.println("bad passiveSignal");  return false;}
        if(this.passiveLatitude!= that.passiveLatitude) {System.err.println("bad passiveLatitude");  return false;}
        if(this.passiveLongitude!= that.passiveLongitude) {System.err.println("bad passiveLongitude");  return false;}
        if(this.passiveElevation!= that.passiveElevation) {System.err.println("bad passiveElevation");  return false;}
        if(this.gpsTime!= that.gpsTime) {System.err.println("bad gpsTime");  return false;}
        return true;
    }




    protected void addFields(List<RecordField> fields)  {
        super.addFields(fields);
        fields.addAll(FIELDS);
    }



    public double getValue(int attrId)  {
        if(attrId == ATTR_RELATIVETIME) return relativeTime;
        if(attrId == ATTR_LASERLATITUDE) return laserLatitude;
        if(attrId == ATTR_LASERLONGITUDE) return laserLongitude;
        if(attrId == ATTR_ELEVATION) return elevation;
        if(attrId == ATTR_STARTSIGNALSTRENGTH) return startSignalStrength;
        if(attrId == ATTR_REFLECTEDSIGNALSTRENGTH) return reflectedSignalStrength;
        if(attrId == ATTR_AZIMUTH) return azimuth;
        if(attrId == ATTR_PITCH) return pitch;
        if(attrId == ATTR_ROLL) return roll;
        if(attrId == ATTR_PASSIVESIGNAL) return passiveSignal;
        if(attrId == ATTR_PASSIVELATITUDE) return passiveLatitude;
        if(attrId == ATTR_PASSIVELONGITUDE) return passiveLongitude;
        if(attrId == ATTR_PASSIVEELEVATION) return passiveElevation;
        if(attrId == ATTR_GPSTIME) return gpsTime;
        return super.getValue(attrId);
        
    }



    public int getRecordSize()  {
        return super.getRecordSize() + 56;
    }



    public BaseRecord.ReadStatus read(RecordIO recordIO) throws Exception  {
        DataInputStream dis = recordIO.getDataInputStream();
        BaseRecord.ReadStatus status= super.read(recordIO);
        if(status!=BaseRecord.ReadStatus.OK)  return status;
        relativeTime =  readInt(dis);
        laserLatitude =  readInt(dis);
        laserLongitude =  readInt(dis);
        elevation =  readInt(dis);
        startSignalStrength =  readInt(dis);
        reflectedSignalStrength =  readInt(dis);
        azimuth =  readInt(dis);
        pitch =  readInt(dis);
        roll =  readInt(dis);
        passiveSignal =  readInt(dis);
        passiveLatitude =  readInt(dis);
        passiveLongitude =  readInt(dis);
        passiveElevation =  readInt(dis);
        gpsTime =  readInt(dis);
        
        
        return BaseRecord.ReadStatus.OK;
    }



    public void write(RecordIO recordIO) throws IOException  {
        DataOutputStream dos = recordIO.getDataOutputStream();
        super.write(recordIO);
        writeInt(dos, relativeTime);
        writeInt(dos, laserLatitude);
        writeInt(dos, laserLongitude);
        writeInt(dos, elevation);
        writeInt(dos, startSignalStrength);
        writeInt(dos, reflectedSignalStrength);
        writeInt(dos, azimuth);
        writeInt(dos, pitch);
        writeInt(dos, roll);
        writeInt(dos, passiveSignal);
        writeInt(dos, passiveLatitude);
        writeInt(dos, passiveLongitude);
        writeInt(dos, passiveElevation);
        writeInt(dos, gpsTime);
        
    }



    public int doPrintCsv(VisitInfo visitInfo,PrintWriter pw)  {
        boolean includeVector = visitInfo.getProperty(PROP_INCLUDEVECTOR, false);
        int superCnt = super.doPrintCsv(visitInfo,pw);
        int myCnt = 0;
        if(superCnt>0) pw.print(',');
        pw.print(getStringValue(RECORDATTR_RELATIVETIME, relativeTime));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_LASERLATITUDE, laserLatitude));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_LASERLONGITUDE, laserLongitude));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_ELEVATION, elevation));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_STARTSIGNALSTRENGTH, startSignalStrength));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_REFLECTEDSIGNALSTRENGTH, reflectedSignalStrength));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_AZIMUTH, azimuth));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_PITCH, pitch));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_ROLL, roll));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_PASSIVESIGNAL, passiveSignal));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_PASSIVELATITUDE, passiveLatitude));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_PASSIVELONGITUDE, passiveLongitude));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_PASSIVEELEVATION, passiveElevation));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_GPSTIME, gpsTime));
        myCnt++;
        return myCnt+superCnt;
        
    }



    public int doPrintCsvHeader(VisitInfo visitInfo,PrintWriter pw)  {
        int superCnt = super.doPrintCsvHeader(visitInfo,pw);
        int myCnt = 0;
        boolean includeVector = visitInfo.getProperty(PROP_INCLUDEVECTOR, false);
        if(superCnt>0) pw.print(',');
        RECORDATTR_RELATIVETIME.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_LASERLATITUDE.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_LASERLONGITUDE.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_ELEVATION.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_STARTSIGNALSTRENGTH.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_REFLECTEDSIGNALSTRENGTH.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_AZIMUTH.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_PITCH.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_ROLL.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_PASSIVESIGNAL.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_PASSIVELATITUDE.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_PASSIVELONGITUDE.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_PASSIVEELEVATION.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_GPSTIME.printCsvHeader(visitInfo,pw);
        myCnt++;
        return myCnt+superCnt;
        
    }



    public void print(Appendable buff)  throws Exception  {
        super.print(buff);
        buff.append(" relativeTime: " + relativeTime+" \n");
        buff.append(" laserLatitude: " + laserLatitude+" \n");
        buff.append(" laserLongitude: " + laserLongitude+" \n");
        buff.append(" elevation: " + elevation+" \n");
        buff.append(" startSignalStrength: " + startSignalStrength+" \n");
        buff.append(" reflectedSignalStrength: " + reflectedSignalStrength+" \n");
        buff.append(" azimuth: " + azimuth+" \n");
        buff.append(" pitch: " + pitch+" \n");
        buff.append(" roll: " + roll+" \n");
        buff.append(" passiveSignal: " + passiveSignal+" \n");
        buff.append(" passiveLatitude: " + passiveLatitude+" \n");
        buff.append(" passiveLongitude: " + passiveLongitude+" \n");
        buff.append(" passiveElevation: " + passiveElevation+" \n");
        buff.append(" gpsTime: " + gpsTime+" \n");
        
    }



    public int getStartSignalStrength()  {
        return startSignalStrength;
    }


    public void setStartSignalStrength(int newValue)  {
        startSignalStrength = newValue;
    }


    public int getReflectedSignalStrength()  {
        return reflectedSignalStrength;
    }


    public void setReflectedSignalStrength(int newValue)  {
        reflectedSignalStrength = newValue;
    }


    public int getAzimuth()  {
        return azimuth;
    }


    public void setAzimuth(int newValue)  {
        azimuth = newValue;
    }


    public int getPitch()  {
        return pitch;
    }


    public void setPitch(int newValue)  {
        pitch = newValue;
    }


    public int getRoll()  {
        return roll;
    }


    public void setRoll(int newValue)  {
        roll = newValue;
    }


    public int getPassiveSignal()  {
        return passiveSignal;
    }


    public void setPassiveSignal(int newValue)  {
        passiveSignal = newValue;
    }


    public int getPassiveLatitude()  {
        return passiveLatitude;
    }


    public void setPassiveLatitude(int newValue)  {
        passiveLatitude = newValue;
    }


    public int getPassiveLongitude()  {
        return passiveLongitude;
    }


    public void setPassiveLongitude(int newValue)  {
        passiveLongitude = newValue;
    }


    public int getPassiveElevation()  {
        return passiveElevation;
    }


    public void setPassiveElevation(int newValue)  {
        passiveElevation = newValue;
    }


    public int getGpsTime()  {
        return gpsTime;
    }


    public void setGpsTime(int newValue)  {
        gpsTime = newValue;
    }



}



