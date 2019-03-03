
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
public class PointRecord0 extends BasePointRecord {
    public static final int ATTR_FIRST = BasePointRecord.ATTR_LAST;
    public static final List<RecordField> FIELDS = new ArrayList<RecordField>();
    public static final int ATTR_X =  ATTR_FIRST + 1;
    public static final RecordField RECORDATTR_X;
    public static final int ATTR_Y =  ATTR_FIRST + 2;
    public static final RecordField RECORDATTR_Y;
    public static final int ATTR_Z =  ATTR_FIRST + 3;
    public static final RecordField RECORDATTR_Z;
    public static final int ATTR_INTENSITY =  ATTR_FIRST + 4;
    public static final RecordField RECORDATTR_INTENSITY;
    public static final int ATTR_RETURNANDFLAGS =  ATTR_FIRST + 5;
    public static final RecordField RECORDATTR_RETURNANDFLAGS;
    public static final int ATTR_CLASSIFICATIONBITFIELD =  ATTR_FIRST + 6;
    public static final RecordField RECORDATTR_CLASSIFICATIONBITFIELD;
    public static final int ATTR_CLASSIFICATION =  ATTR_FIRST + 7;
    public static final RecordField RECORDATTR_CLASSIFICATION;
    public static final int ATTR_SYNTHETIC =  ATTR_FIRST + 8;
    public static final RecordField RECORDATTR_SYNTHETIC;
    public static final int ATTR_SCANANGLERANK =  ATTR_FIRST + 9;
    public static final RecordField RECORDATTR_SCANANGLERANK;
    public static final int ATTR_USERDATA =  ATTR_FIRST + 10;
    public static final RecordField RECORDATTR_USERDATA;
    public static final int ATTR_POINTSOURCEID =  ATTR_FIRST + 11;
    public static final RecordField RECORDATTR_POINTSOURCEID;
    public static final int ATTR_LAST = ATTR_FIRST + 12;
    

    static {
    FIELDS.add(RECORDATTR_X = new RecordField("X", "X", "", ATTR_X, "", "int", "int", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_X.setValueGetter(new ValueGetter() {
    public double getValue(Record record, RecordField field, VisitInfo visitInfo) {
    LasPointRecord lasRecord = ((LasPointRecord)record);if(visitInfo.getProperty("georeference",false)) return lasRecord.getLongitude();return lasRecord.getScaledAndOffsetX();
    }
    public String getStringValue(Record record, RecordField field, VisitInfo visitInfo) {
    return ""+ getValue(record, field, visitInfo);
    }
    });
    FIELDS.add(RECORDATTR_Y = new RecordField("Y", "Y", "", ATTR_Y, "", "int", "int", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_Y.setValueGetter(new ValueGetter() {
    public double getValue(Record record, RecordField field, VisitInfo visitInfo) {
    LasPointRecord lasRecord = ((LasPointRecord)record);if(visitInfo.getProperty("georeference",false)) return lasRecord.getLatitude();return lasRecord.getScaledAndOffsetY();
    }
    public String getStringValue(Record record, RecordField field, VisitInfo visitInfo) {
    return ""+ getValue(record, field, visitInfo);
    }
    });
    FIELDS.add(RECORDATTR_Z = new RecordField("Z", "Z", "", ATTR_Z, "", "int", "int", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_Z.setValueGetter(new ValueGetter() {
    public double getValue(Record record, RecordField field, VisitInfo visitInfo) {
    LasPointRecord lasRecord = ((LasPointRecord)record);if(visitInfo.getProperty("georeference",false)) return lasRecord.getAltitude();return lasRecord.getScaledAndOffsetZ();
    }
    public String getStringValue(Record record, RecordField field, VisitInfo visitInfo) {
    return ""+ getValue(record, field, visitInfo);
    }
    });
    FIELDS.add(RECORDATTR_INTENSITY = new RecordField("Intensity", "Intensity", "", ATTR_INTENSITY, "", "ushort", "short", 0, SEARCHABLE_YES,CHARTABLE_YES));
    RECORDATTR_INTENSITY.setValueGetter(new ValueGetter() {
    public double getValue(Record record, RecordField field, VisitInfo visitInfo) {
    return (double) ((PointRecord0)record).Intensity;
    }
    public String getStringValue(Record record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((PointRecord0)record).Intensity;
    }
    });
    RECORDATTR_INTENSITY.setProperty(RecordField.PROP_SEARCH_SUFFIX,"Pulse return magnitude");
    FIELDS.add(RECORDATTR_RETURNANDFLAGS = new RecordField("returnAndFlags", "returnAndFlags", "", ATTR_RETURNANDFLAGS, "", "byte", "byte", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_RETURNANDFLAGS.setValueGetter(new ValueGetter() {
    public double getValue(Record record, RecordField field, VisitInfo visitInfo) {
    return (double) ((PointRecord0)record).returnAndFlags;
    }
    public String getStringValue(Record record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((PointRecord0)record).returnAndFlags;
    }
    });
    FIELDS.add(RECORDATTR_CLASSIFICATIONBITFIELD = new RecordField("classificationBitField", "classificationBitField", "", ATTR_CLASSIFICATIONBITFIELD, "", "byte", "byte", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_CLASSIFICATIONBITFIELD.setValueGetter(new ValueGetter() {
    public double getValue(Record record, RecordField field, VisitInfo visitInfo) {
    return (double) ((PointRecord0)record).classificationBitField;
    }
    public String getStringValue(Record record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((PointRecord0)record).classificationBitField;
    }
    });
    FIELDS.add(RECORDATTR_CLASSIFICATION = new RecordField("Classification", "Classification", "", ATTR_CLASSIFICATION, "", "int", "int", 0, SEARCHABLE_YES,CHARTABLE_NO));
    RECORDATTR_CLASSIFICATION.setSynthetic(true);
    RECORDATTR_CLASSIFICATION.setValueGetter(new ValueGetter() {
    public double getValue(Record record, RecordField field, VisitInfo visitInfo) {
    return (double) ((PointRecord0)record).getClassification();
    }
    public String getStringValue(Record record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((PointRecord0)record).getClassification();
    }
    });
    List<String[]> Classification_enums = new ArrayList<String[]>();
    Classification_enums.add(new String[]{"0","Created, never classified"});
    Classification_enums.add(new String[]{"1","Unclassified"});
    Classification_enums.add(new String[]{"2","Ground"});
    Classification_enums.add(new String[]{"3","Low Vegetation"});
    Classification_enums.add(new String[]{"4","Medium Vegetation"});
    Classification_enums.add(new String[]{"5","High Vegetation"});
    Classification_enums.add(new String[]{"6","Building"});
    Classification_enums.add(new String[]{"7","Low Point (noise)"});
    Classification_enums.add(new String[]{"8","Model Key-point (mass point)"});
    Classification_enums.add(new String[]{"9","Water"});
    Classification_enums.add(new String[]{"10","10"});
    Classification_enums.add(new String[]{"11","11"});
    Classification_enums.add(new String[]{"12","Overlap Points"});
    Classification_enums.add(new String[]{"13","13"});
    Classification_enums.add(new String[]{"14","14"});
    Classification_enums.add(new String[]{"15","15"});
    Classification_enums.add(new String[]{"16","16"});
    RECORDATTR_CLASSIFICATION.setEnumeratedValues(Classification_enums);
    FIELDS.add(RECORDATTR_SYNTHETIC = new RecordField("Synthetic", "Synthetic", "", ATTR_SYNTHETIC, "", "int", "int", 0, SEARCHABLE_YES,CHARTABLE_NO));
    RECORDATTR_SYNTHETIC.setSynthetic(true);
    RECORDATTR_SYNTHETIC.setValueGetter(new ValueGetter() {
    public double getValue(Record record, RecordField field, VisitInfo visitInfo) {
    return (double) ((PointRecord0)record).getSynthetic();
    }
    public String getStringValue(Record record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((PointRecord0)record).getSynthetic();
    }
    });
    List<String[]> Synthetic_enums = new ArrayList<String[]>();
    Synthetic_enums.add(new String[]{"0","Not Synthetic"});
    Synthetic_enums.add(new String[]{"1","Synthetic"});
    RECORDATTR_SYNTHETIC.setEnumeratedValues(Synthetic_enums);
    FIELDS.add(RECORDATTR_SCANANGLERANK = new RecordField("scanAngleRank", "scanAngleRank", "", ATTR_SCANANGLERANK, "", "byte", "byte", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_SCANANGLERANK.setValueGetter(new ValueGetter() {
    public double getValue(Record record, RecordField field, VisitInfo visitInfo) {
    return (double) ((PointRecord0)record).scanAngleRank;
    }
    public String getStringValue(Record record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((PointRecord0)record).scanAngleRank;
    }
    });
    FIELDS.add(RECORDATTR_USERDATA = new RecordField("userData", "userData", "", ATTR_USERDATA, "", "byte", "byte", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_USERDATA.setValueGetter(new ValueGetter() {
    public double getValue(Record record, RecordField field, VisitInfo visitInfo) {
    return (double) ((PointRecord0)record).userData;
    }
    public String getStringValue(Record record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((PointRecord0)record).userData;
    }
    });
    FIELDS.add(RECORDATTR_POINTSOURCEID = new RecordField("pointSourceId", "pointSourceId", "", ATTR_POINTSOURCEID, "", "ushort", "short", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_POINTSOURCEID.setValueGetter(new ValueGetter() {
    public double getValue(Record record, RecordField field, VisitInfo visitInfo) {
    return (double) ((PointRecord0)record).pointSourceId;
    }
    public String getStringValue(Record record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((PointRecord0)record).pointSourceId;
    }
    });
    
    }
    

    int X;
    int Y;
    int Z;
    int Intensity;
    byte returnAndFlags;
    byte classificationBitField;
    byte scanAngleRank;
    byte userData;
    int pointSourceId;
    

    public  PointRecord0(PointRecord0 that)  {
        super(that);
        this.X = that.X;
        this.Y = that.Y;
        this.Z = that.Z;
        this.Intensity = that.Intensity;
        this.returnAndFlags = that.returnAndFlags;
        this.classificationBitField = that.classificationBitField;
        this.scanAngleRank = that.scanAngleRank;
        this.userData = that.userData;
        this.pointSourceId = that.pointSourceId;
        
        
    }



    public  PointRecord0(RecordFile file)  {
        super(file);
    }



    public  PointRecord0(RecordFile file, boolean bigEndian)  {
        super(file, bigEndian);
    }



    public int getLastAttribute()  {
        return ATTR_LAST;
    }



    public  boolean equals(Object object)  {
        if(!super.equals(object)) {System.err.println("bad super"); return false;} if(!(object instanceof PointRecord0)) return false;
        PointRecord0 that = (PointRecord0 ) object;
        if(this.X!= that.X) {System.err.println("bad X");  return false;}
        if(this.Y!= that.Y) {System.err.println("bad Y");  return false;}
        if(this.Z!= that.Z) {System.err.println("bad Z");  return false;}
        if(this.Intensity!= that.Intensity) {System.err.println("bad Intensity");  return false;}
        if(this.returnAndFlags!= that.returnAndFlags) {System.err.println("bad returnAndFlags");  return false;}
        if(this.classificationBitField!= that.classificationBitField) {System.err.println("bad classificationBitField");  return false;}
        if(this.scanAngleRank!= that.scanAngleRank) {System.err.println("bad scanAngleRank");  return false;}
        if(this.userData!= that.userData) {System.err.println("bad userData");  return false;}
        if(this.pointSourceId!= that.pointSourceId) {System.err.println("bad pointSourceId");  return false;}
        return true;
    }




    int cnt = 0;
    public void recontextualize(LidarFile lidarFile) {
        super.recontextualize(lidarFile);
        LasFile newLasFile = (LasFile) lidarFile;
        newLasFile.importRecord(this);
    }
    public int getClassification() {
        int c =(int) (classificationBitField & 0x001F);
        return c;
    }

    public int getSynthetic() {
        if((classificationBitField & (1<<5))==0) return 0;
        return 1;
    }

    public void setReturnNumber(byte b) {
        int masked = mask(returnAndFlags,0,2,true);
        returnAndFlags = (byte)(masked & mask(b,3,7,true));
    }
    public void setNumberOfReturns(byte b) {
        int masked = mask(returnAndFlags,3,5,true);
        returnAndFlags = (byte)(masked | mask(b,3,7,false));
    }

    protected void addFields(List<RecordField> fields)  {
        super.addFields(fields);
        fields.addAll(FIELDS);
    }



    public double getValue(int attrId)  {
        if(attrId == ATTR_X) return X;
        if(attrId == ATTR_Y) return Y;
        if(attrId == ATTR_Z) return Z;
        if(attrId == ATTR_INTENSITY) return Intensity;
        if(attrId == ATTR_RETURNANDFLAGS) return returnAndFlags;
        if(attrId == ATTR_CLASSIFICATIONBITFIELD) return classificationBitField;
        if(attrId == ATTR_CLASSIFICATION) return getClassification();
        if(attrId == ATTR_SYNTHETIC) return getSynthetic();
        if(attrId == ATTR_SCANANGLERANK) return scanAngleRank;
        if(attrId == ATTR_USERDATA) return userData;
        if(attrId == ATTR_POINTSOURCEID) return pointSourceId;
        return super.getValue(attrId);
        
    }



    public int getRecordSize()  {
        return super.getRecordSize() + 20;
    }



    public ReadStatus read(RecordIO recordIO) throws Exception  {
        DataInputStream dis = recordIO.getDataInputStream();
        ReadStatus status= super.read(recordIO);
        if(status!=ReadStatus.OK)  return status;
        X =  readInt(dis);
        Y =  readInt(dis);
        Z =  readInt(dis);
        Intensity =  readUnsignedShort(dis);
        returnAndFlags =  readByte(dis);
        classificationBitField =  readByte(dis);
        scanAngleRank =  readByte(dis);
        userData =  readByte(dis);
        pointSourceId =  readUnsignedShort(dis);
        
        ((LasFile)getRecordFile()).initRecord(this);
        
        
        return ReadStatus.OK;
    }



    public void write(RecordIO recordIO) throws IOException  {
        DataOutputStream dos = recordIO.getDataOutputStream();
        super.write(recordIO);
        writeInt(dos, X);
        writeInt(dos, Y);
        writeInt(dos, Z);
        writeUnsignedShort(dos, Intensity);
        writeByte(dos, returnAndFlags);
        writeByte(dos, classificationBitField);
        writeByte(dos, scanAngleRank);
        writeByte(dos, userData);
        writeUnsignedShort(dos, pointSourceId);
        
    }



    public int doPrintCsv(VisitInfo visitInfo,PrintWriter pw)  {
        boolean includeVector = visitInfo.getProperty(PROP_INCLUDEVECTOR, false);
        int superCnt = super.doPrintCsv(visitInfo,pw);
        int myCnt = 0;
        if(superCnt>0) pw.print(',');
        pw.print(getLongitude());;
        myCnt++;
        pw.print(',');
        pw.print(getLatitude());;
        myCnt++;
        pw.print(',');
        pw.print(getAltitude());;
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_INTENSITY, Intensity));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_RETURNANDFLAGS, returnAndFlags));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_CLASSIFICATIONBITFIELD, classificationBitField));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_SCANANGLERANK, scanAngleRank));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_USERDATA, userData));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_POINTSOURCEID, pointSourceId));
        myCnt++;
        return myCnt+superCnt;
        
    }



    public int doPrintCsvHeader(VisitInfo visitInfo,PrintWriter pw)  {
        int superCnt = super.doPrintCsvHeader(visitInfo,pw);
        int myCnt = 0;
        boolean includeVector = visitInfo.getProperty(PROP_INCLUDEVECTOR, false);
        if(superCnt>0) pw.print(',');
        RECORDATTR_X.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_Y.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_Z.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_INTENSITY.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_RETURNANDFLAGS.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_CLASSIFICATIONBITFIELD.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_SCANANGLERANK.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_USERDATA.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_POINTSOURCEID.printCsvHeader(visitInfo,pw);
        myCnt++;
        return myCnt+superCnt;
        
    }



    public void print(Appendable buff)  throws Exception  {
        super.print(buff);
        buff.append(" X: " + X+" \n");
        buff.append(" Y: " + Y+" \n");
        buff.append(" Z: " + Z+" \n");
        buff.append(" Intensity: " + Intensity+" \n");
        buff.append(" returnAndFlags: " + returnAndFlags+" \n");
        buff.append(" classificationBitField: " + classificationBitField+" \n");
        buff.append(" scanAngleRank: " + scanAngleRank+" \n");
        buff.append(" userData: " + userData+" \n");
        buff.append(" pointSourceId: " + pointSourceId+" \n");
        
    }



    public int getX()  {
        return X;
    }


    public void setX(int newValue)  {
        X = newValue;
    }


    public int getY()  {
        return Y;
    }


    public void setY(int newValue)  {
        Y = newValue;
    }


    public int getZ()  {
        return Z;
    }


    public void setZ(int newValue)  {
        Z = newValue;
    }


    public int getIntensity()  {
        return Intensity;
    }


    public void setIntensity(int newValue)  {
        Intensity = newValue;
    }


    public byte getReturnAndFlags()  {
        return returnAndFlags;
    }


    public void setReturnAndFlags(byte newValue)  {
        returnAndFlags = newValue;
    }


    public byte getClassificationBitField()  {
        return classificationBitField;
    }


    public void setClassificationBitField(byte newValue)  {
        classificationBitField = newValue;
    }


    public byte getScanAngleRank()  {
        return scanAngleRank;
    }


    public void setScanAngleRank(byte newValue)  {
        scanAngleRank = newValue;
    }


    public byte getUserData()  {
        return userData;
    }


    public void setUserData(byte newValue)  {
        userData = newValue;
    }


    public int getPointSourceId()  {
        return pointSourceId;
    }


    public void setPointSourceId(int newValue)  {
        pointSourceId = newValue;
    }



}



