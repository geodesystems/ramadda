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



package org.ramadda.geodata.point.icebridge;

import org.ramadda.util.IO;
import org.ramadda.data.record.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;


import org.ramadda.data.point.PointFile;



/** This is generated code from generate.tcl. Do not edit it! */
public class AtmIceSSNFile extends org.ramadda.data.point.text.TextFile {
public AtmIceSSNFile()  {}
public AtmIceSSNFile(IO.Path path) throws java.io.IOException {super(path);}
public BaseRecord doMakeRecord(VisitInfo visitInfo) {return new AtmIceSSNRecord(this);}
public static void main(String[]args) throws Exception {PointFile.test(args, AtmIceSSNFile.class);
}


//generated record class


public static class AtmIceSSNRecord extends org.ramadda.data.point.text.TextRecord {
    public static final int ATTR_FIRST = org.ramadda.data.point.text.TextRecord.ATTR_LAST;
    public static final List<RecordField> FIELDS = new ArrayList<RecordField>();
    public static final int ATTR_SECONDS =  ATTR_FIRST + 1;
    public static final RecordField RECORDATTR_SECONDS;
    public static final int ATTR_CENTERLATITUDE =  ATTR_FIRST + 2;
    public static final RecordField RECORDATTR_CENTERLATITUDE;
    public static final int ATTR_CENTERLONGITUDE =  ATTR_FIRST + 3;
    public static final RecordField RECORDATTR_CENTERLONGITUDE;
    public static final int ATTR_HEIGHT =  ATTR_FIRST + 4;
    public static final RecordField RECORDATTR_HEIGHT;
    public static final int ATTR_SOUTHTONORTHSLOPE =  ATTR_FIRST + 5;
    public static final RecordField RECORDATTR_SOUTHTONORTHSLOPE;
    public static final int ATTR_WESTTOEASTSLOPE =  ATTR_FIRST + 6;
    public static final RecordField RECORDATTR_WESTTOEASTSLOPE;
    public static final int ATTR_RMSFIT =  ATTR_FIRST + 7;
    public static final RecordField RECORDATTR_RMSFIT;
    public static final int ATTR_NUMBEROFPOINTSUSED =  ATTR_FIRST + 8;
    public static final RecordField RECORDATTR_NUMBEROFPOINTSUSED;
    public static final int ATTR_NUMBEROFPOINTSEDITED =  ATTR_FIRST + 9;
    public static final RecordField RECORDATTR_NUMBEROFPOINTSEDITED;
    public static final int ATTR_DISTANCEFROMTRAJECTORY =  ATTR_FIRST + 10;
    public static final RecordField RECORDATTR_DISTANCEFROMTRAJECTORY;
    public static final int ATTR_TRACKIDENTIFIER =  ATTR_FIRST + 11;
    public static final RecordField RECORDATTR_TRACKIDENTIFIER;
    public static final int ATTR_LAST = ATTR_FIRST + 12;
    

    static {
    FIELDS.add(RECORDATTR_SECONDS = new RecordField("seconds", "seconds", "", ATTR_SECONDS, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_SECONDS.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((AtmIceSSNRecord)record).seconds;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((AtmIceSSNRecord)record).seconds;
    }
    });
    FIELDS.add(RECORDATTR_CENTERLATITUDE = new RecordField("centerLatitude", "centerLatitude", "", ATTR_CENTERLATITUDE, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_CENTERLATITUDE.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((AtmIceSSNRecord)record).centerLatitude;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((AtmIceSSNRecord)record).centerLatitude;
    }
    });
    FIELDS.add(RECORDATTR_CENTERLONGITUDE = new RecordField("centerLongitude", "centerLongitude", "", ATTR_CENTERLONGITUDE, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_CENTERLONGITUDE.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((AtmIceSSNRecord)record).centerLongitude;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((AtmIceSSNRecord)record).centerLongitude;
    }
    });
    FIELDS.add(RECORDATTR_HEIGHT = new RecordField("height", "height", "", ATTR_HEIGHT, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_HEIGHT.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((AtmIceSSNRecord)record).height;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((AtmIceSSNRecord)record).height;
    }
    });
    FIELDS.add(RECORDATTR_SOUTHTONORTHSLOPE = new RecordField("southToNorthSlope", "southToNorthSlope", "", ATTR_SOUTHTONORTHSLOPE, "degrees", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_YES));
    RECORDATTR_SOUTHTONORTHSLOPE.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((AtmIceSSNRecord)record).southToNorthSlope;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((AtmIceSSNRecord)record).southToNorthSlope;
    }
    });
    FIELDS.add(RECORDATTR_WESTTOEASTSLOPE = new RecordField("westToEastSlope", "westToEastSlope", "", ATTR_WESTTOEASTSLOPE, "degrees", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_YES));
    RECORDATTR_WESTTOEASTSLOPE.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((AtmIceSSNRecord)record).westToEastSlope;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((AtmIceSSNRecord)record).westToEastSlope;
    }
    });
    FIELDS.add(RECORDATTR_RMSFIT = new RecordField("rmsFit", "rmsFit", "", ATTR_RMSFIT, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_YES));
    RECORDATTR_RMSFIT.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((AtmIceSSNRecord)record).rmsFit;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((AtmIceSSNRecord)record).rmsFit;
    }
    });
    FIELDS.add(RECORDATTR_NUMBEROFPOINTSUSED = new RecordField("numberOfPointsUsed", "numberOfPointsUsed", "", ATTR_NUMBEROFPOINTSUSED, "", "int", "int", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_NUMBEROFPOINTSUSED.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((AtmIceSSNRecord)record).numberOfPointsUsed;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((AtmIceSSNRecord)record).numberOfPointsUsed;
    }
    });
    FIELDS.add(RECORDATTR_NUMBEROFPOINTSEDITED = new RecordField("numberOfPointsEdited", "numberOfPointsEdited", "", ATTR_NUMBEROFPOINTSEDITED, "", "int", "int", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_NUMBEROFPOINTSEDITED.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((AtmIceSSNRecord)record).numberOfPointsEdited;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((AtmIceSSNRecord)record).numberOfPointsEdited;
    }
    });
    FIELDS.add(RECORDATTR_DISTANCEFROMTRAJECTORY = new RecordField("distanceFromTrajectory", "distanceFromTrajectory", "", ATTR_DISTANCEFROMTRAJECTORY, "m", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_DISTANCEFROMTRAJECTORY.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((AtmIceSSNRecord)record).distanceFromTrajectory;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((AtmIceSSNRecord)record).distanceFromTrajectory;
    }
    });
    FIELDS.add(RECORDATTR_TRACKIDENTIFIER = new RecordField("trackIdentifier", "trackIdentifier", "", ATTR_TRACKIDENTIFIER, "", "int", "int", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_TRACKIDENTIFIER.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((AtmIceSSNRecord)record).trackIdentifier;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((AtmIceSSNRecord)record).trackIdentifier;
    }
    });
    
    }
    

    double seconds;
    double centerLatitude;
    double centerLongitude;
    double height;
    double southToNorthSlope;
    double westToEastSlope;
    double rmsFit;
    int numberOfPointsUsed;
    int numberOfPointsEdited;
    double distanceFromTrajectory;
    int trackIdentifier;
    

    public  AtmIceSSNRecord(AtmIceSSNRecord that)  {
        super(that);
        this.seconds = that.seconds;
        this.centerLatitude = that.centerLatitude;
        this.centerLongitude = that.centerLongitude;
        this.height = that.height;
        this.southToNorthSlope = that.southToNorthSlope;
        this.westToEastSlope = that.westToEastSlope;
        this.rmsFit = that.rmsFit;
        this.numberOfPointsUsed = that.numberOfPointsUsed;
        this.numberOfPointsEdited = that.numberOfPointsEdited;
        this.distanceFromTrajectory = that.distanceFromTrajectory;
        this.trackIdentifier = that.trackIdentifier;
        
        
    }



    public  AtmIceSSNRecord(RecordFile file)  {
        super(file);
    }



    public  AtmIceSSNRecord(RecordFile file, boolean bigEndian)  {
        super(file, bigEndian);
    }



    public int getLastAttribute()  {
        return ATTR_LAST;
    }



    public  boolean equals(Object object)  {
        if(!super.equals(object)) {System.err.println("bad super"); return false;} if(!(object instanceof AtmIceSSNRecord)) return false;
        AtmIceSSNRecord that = (AtmIceSSNRecord ) object;
        if(this.seconds!= that.seconds) {System.err.println("bad seconds");  return false;}
        if(this.centerLatitude!= that.centerLatitude) {System.err.println("bad centerLatitude");  return false;}
        if(this.centerLongitude!= that.centerLongitude) {System.err.println("bad centerLongitude");  return false;}
        if(this.height!= that.height) {System.err.println("bad height");  return false;}
        if(this.southToNorthSlope!= that.southToNorthSlope) {System.err.println("bad southToNorthSlope");  return false;}
        if(this.westToEastSlope!= that.westToEastSlope) {System.err.println("bad westToEastSlope");  return false;}
        if(this.rmsFit!= that.rmsFit) {System.err.println("bad rmsFit");  return false;}
        if(this.numberOfPointsUsed!= that.numberOfPointsUsed) {System.err.println("bad numberOfPointsUsed");  return false;}
        if(this.numberOfPointsEdited!= that.numberOfPointsEdited) {System.err.println("bad numberOfPointsEdited");  return false;}
        if(this.distanceFromTrajectory!= that.distanceFromTrajectory) {System.err.println("bad distanceFromTrajectory");  return false;}
        if(this.trackIdentifier!= that.trackIdentifier) {System.err.println("bad trackIdentifier");  return false;}
        return true;
    }




    //overwrite the getLatitude/getLongitude methods
    public double getLatitude() {
        return centerLatitude;
    }
    public double getLongitude() {
        return org.ramadda.util.geo.GeoUtils.normalizeLongitude(centerLongitude);
    }
    public double getAltitude() {
        return height;
    }


    protected void addFields(List<RecordField> fields)  {
        super.addFields(fields);
        fields.addAll(FIELDS);
    }



    public double getValue(int attrId)  {
        if(attrId == ATTR_SECONDS) return seconds;
        if(attrId == ATTR_CENTERLATITUDE) return centerLatitude;
        if(attrId == ATTR_CENTERLONGITUDE) return centerLongitude;
        if(attrId == ATTR_HEIGHT) return height;
        if(attrId == ATTR_SOUTHTONORTHSLOPE) return southToNorthSlope;
        if(attrId == ATTR_WESTTOEASTSLOPE) return westToEastSlope;
        if(attrId == ATTR_RMSFIT) return rmsFit;
        if(attrId == ATTR_NUMBEROFPOINTSUSED) return numberOfPointsUsed;
        if(attrId == ATTR_NUMBEROFPOINTSEDITED) return numberOfPointsEdited;
        if(attrId == ATTR_DISTANCEFROMTRAJECTORY) return distanceFromTrajectory;
        if(attrId == ATTR_TRACKIDENTIFIER) return trackIdentifier;
        return super.getValue(attrId);
        
    }



    public int getRecordSize()  {
        return super.getRecordSize() + 76;
    }



    public BaseRecord.ReadStatus read(RecordIO recordIO) throws Exception  {
        BaseRecord.ReadStatus status = BaseRecord.ReadStatus.OK;
        String line = recordIO.readLine();
        if(line == null) return BaseRecord.ReadStatus.EOF;
        line = line.trim();
        if(line.length()==0) return status;
        String[] toks = line.split(" +");
        int fieldCnt = 0;
        seconds = (double) Double.parseDouble(toks[fieldCnt++]);
        centerLatitude = (double) Double.parseDouble(toks[fieldCnt++]);
        centerLongitude = (double) Double.parseDouble(toks[fieldCnt++]);
        height = (double) Double.parseDouble(toks[fieldCnt++]);
        southToNorthSlope = (double) Double.parseDouble(toks[fieldCnt++]);
        westToEastSlope = (double) Double.parseDouble(toks[fieldCnt++]);
        rmsFit = (double) Double.parseDouble(toks[fieldCnt++]);
        numberOfPointsUsed = (int) Double.parseDouble(toks[fieldCnt++]);
        numberOfPointsEdited = (int) Double.parseDouble(toks[fieldCnt++]);
        distanceFromTrajectory = (double) Double.parseDouble(toks[fieldCnt++]);
        trackIdentifier = (int) Double.parseDouble(toks[fieldCnt++]);
        
        
        return status;
    }



    public void write(RecordIO recordIO) throws IOException  {
        String delimiter = " ";
        PrintWriter  printWriter= recordIO.getPrintWriter();
        printWriter.print(seconds);
        printWriter.print(delimiter);
        printWriter.print(centerLatitude);
        printWriter.print(delimiter);
        printWriter.print(centerLongitude);
        printWriter.print(delimiter);
        printWriter.print(height);
        printWriter.print(delimiter);
        printWriter.print(southToNorthSlope);
        printWriter.print(delimiter);
        printWriter.print(westToEastSlope);
        printWriter.print(delimiter);
        printWriter.print(rmsFit);
        printWriter.print(delimiter);
        printWriter.print(numberOfPointsUsed);
        printWriter.print(delimiter);
        printWriter.print(numberOfPointsEdited);
        printWriter.print(delimiter);
        printWriter.print(distanceFromTrajectory);
        printWriter.print(delimiter);
        printWriter.print(trackIdentifier);
        printWriter.print("\n");
        
    }



    public int doPrintCsv(VisitInfo visitInfo,PrintWriter pw)  {
        boolean includeVector = visitInfo.getProperty(PROP_INCLUDEVECTOR, false);
        int superCnt = super.doPrintCsv(visitInfo,pw);
        int myCnt = 0;
        if(superCnt>0) pw.print(',');
        pw.print(getStringValue(RECORDATTR_SECONDS, seconds));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_CENTERLATITUDE, centerLatitude));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_CENTERLONGITUDE, centerLongitude));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_HEIGHT, height));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_SOUTHTONORTHSLOPE, southToNorthSlope));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_WESTTOEASTSLOPE, westToEastSlope));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_RMSFIT, rmsFit));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_NUMBEROFPOINTSUSED, numberOfPointsUsed));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_NUMBEROFPOINTSEDITED, numberOfPointsEdited));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_DISTANCEFROMTRAJECTORY, distanceFromTrajectory));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_TRACKIDENTIFIER, trackIdentifier));
        myCnt++;
        return myCnt+superCnt;
        
    }



    public int doPrintCsvHeader(VisitInfo visitInfo,PrintWriter pw)  {
        int superCnt = super.doPrintCsvHeader(visitInfo,pw);
        int myCnt = 0;
        boolean includeVector = visitInfo.getProperty(PROP_INCLUDEVECTOR, false);
        if(superCnt>0) pw.print(',');
        RECORDATTR_SECONDS.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_CENTERLATITUDE.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_CENTERLONGITUDE.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_HEIGHT.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_SOUTHTONORTHSLOPE.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_WESTTOEASTSLOPE.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_RMSFIT.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_NUMBEROFPOINTSUSED.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_NUMBEROFPOINTSEDITED.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_DISTANCEFROMTRAJECTORY.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_TRACKIDENTIFIER.printCsvHeader(visitInfo,pw);
        myCnt++;
        return myCnt+superCnt;
        
    }



    public void print(Appendable buff)  throws Exception  {
        super.print(buff);
        buff.append(" seconds: " + seconds+" \n");
        buff.append(" centerLatitude: " + centerLatitude+" \n");
        buff.append(" centerLongitude: " + centerLongitude+" \n");
        buff.append(" height: " + height+" \n");
        buff.append(" southToNorthSlope: " + southToNorthSlope+" \n");
        buff.append(" westToEastSlope: " + westToEastSlope+" \n");
        buff.append(" rmsFit: " + rmsFit+" \n");
        buff.append(" numberOfPointsUsed: " + numberOfPointsUsed+" \n");
        buff.append(" numberOfPointsEdited: " + numberOfPointsEdited+" \n");
        buff.append(" distanceFromTrajectory: " + distanceFromTrajectory+" \n");
        buff.append(" trackIdentifier: " + trackIdentifier+" \n");
        
    }



    public double getSeconds()  {
        return seconds;
    }


    public void setSeconds(double newValue)  {
        seconds = newValue;
    }


    public double getCenterLatitude()  {
        return centerLatitude;
    }


    public void setCenterLatitude(double newValue)  {
        centerLatitude = newValue;
    }


    public double getCenterLongitude()  {
        return centerLongitude;
    }


    public void setCenterLongitude(double newValue)  {
        centerLongitude = newValue;
    }


    public double getHeight()  {
        return height;
    }


    public void setHeight(double newValue)  {
        height = newValue;
    }


    public double getSouthToNorthSlope()  {
        return southToNorthSlope;
    }


    public void setSouthToNorthSlope(double newValue)  {
        southToNorthSlope = newValue;
    }


    public double getWestToEastSlope()  {
        return westToEastSlope;
    }


    public void setWestToEastSlope(double newValue)  {
        westToEastSlope = newValue;
    }


    public double getRmsFit()  {
        return rmsFit;
    }


    public void setRmsFit(double newValue)  {
        rmsFit = newValue;
    }


    public int getNumberOfPointsUsed()  {
        return numberOfPointsUsed;
    }


    public void setNumberOfPointsUsed(int newValue)  {
        numberOfPointsUsed = newValue;
    }


    public int getNumberOfPointsEdited()  {
        return numberOfPointsEdited;
    }


    public void setNumberOfPointsEdited(int newValue)  {
        numberOfPointsEdited = newValue;
    }


    public double getDistanceFromTrajectory()  {
        return distanceFromTrajectory;
    }


    public void setDistanceFromTrajectory(double newValue)  {
        distanceFromTrajectory = newValue;
    }


    public int getTrackIdentifier()  {
        return trackIdentifier;
    }


    public void setTrackIdentifier(int newValue)  {
        trackIdentifier = newValue;
    }



}

}



