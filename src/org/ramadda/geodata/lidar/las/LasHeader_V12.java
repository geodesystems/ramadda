
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
public class LasHeader_V12 extends LasHeader {
    public static final int ATTR_FIRST = LasHeader.ATTR_LAST;
    public static final List<RecordField> FIELDS = new ArrayList<RecordField>();
    public static final int ATTR_FILESIGNATURE =  ATTR_FIRST + 1;
    public static final RecordField RECORDATTR_FILESIGNATURE;
    public static final int ATTR_FILESOURCEID =  ATTR_FIRST + 2;
    public static final RecordField RECORDATTR_FILESOURCEID;
    public static final int ATTR_GLOBALENCODING =  ATTR_FIRST + 3;
    public static final RecordField RECORDATTR_GLOBALENCODING;
    public static final int ATTR_PROJECTIDGUIDDATA1 =  ATTR_FIRST + 4;
    public static final RecordField RECORDATTR_PROJECTIDGUIDDATA1;
    public static final int ATTR_PROJECTIDGUIDDATA2 =  ATTR_FIRST + 5;
    public static final RecordField RECORDATTR_PROJECTIDGUIDDATA2;
    public static final int ATTR_PROJECTIDGUIDDATA3 =  ATTR_FIRST + 6;
    public static final RecordField RECORDATTR_PROJECTIDGUIDDATA3;
    public static final int ATTR_PROJECTIDGUIDDATA4 =  ATTR_FIRST + 7;
    public static final RecordField RECORDATTR_PROJECTIDGUIDDATA4;
    public static final int ATTR_VERSIONMAJOR =  ATTR_FIRST + 8;
    public static final RecordField RECORDATTR_VERSIONMAJOR;
    public static final int ATTR_VERSIONMINOR =  ATTR_FIRST + 9;
    public static final RecordField RECORDATTR_VERSIONMINOR;
    public static final int ATTR_SYSTEMIDENTIFIER =  ATTR_FIRST + 10;
    public static final RecordField RECORDATTR_SYSTEMIDENTIFIER;
    public static final int ATTR_GENERATINGSOFTWARE =  ATTR_FIRST + 11;
    public static final RecordField RECORDATTR_GENERATINGSOFTWARE;
    public static final int ATTR_FILECREATIONDAYOFYEAR =  ATTR_FIRST + 12;
    public static final RecordField RECORDATTR_FILECREATIONDAYOFYEAR;
    public static final int ATTR_FILECREATIONYEAR =  ATTR_FIRST + 13;
    public static final RecordField RECORDATTR_FILECREATIONYEAR;
    public static final int ATTR_HEADERSIZE =  ATTR_FIRST + 14;
    public static final RecordField RECORDATTR_HEADERSIZE;
    public static final int ATTR_OFFSETTOPOINTDATA =  ATTR_FIRST + 15;
    public static final RecordField RECORDATTR_OFFSETTOPOINTDATA;
    public static final int ATTR_NUMBEROFVARIABLELENGTHRECORDS =  ATTR_FIRST + 16;
    public static final RecordField RECORDATTR_NUMBEROFVARIABLELENGTHRECORDS;
    public static final int ATTR_POINTDATAFORMATID =  ATTR_FIRST + 17;
    public static final RecordField RECORDATTR_POINTDATAFORMATID;
    public static final int ATTR_POINTDATARECORDLENGTH =  ATTR_FIRST + 18;
    public static final RecordField RECORDATTR_POINTDATARECORDLENGTH;
    public static final int ATTR_NUMBEROFPOINTRECORDS =  ATTR_FIRST + 19;
    public static final RecordField RECORDATTR_NUMBEROFPOINTRECORDS;
    public static final int ATTR_NUMBEROFPOINTSBYRETURN =  ATTR_FIRST + 20;
    public static final RecordField RECORDATTR_NUMBEROFPOINTSBYRETURN;
    public static final int ATTR_XSCALEFACTOR =  ATTR_FIRST + 21;
    public static final RecordField RECORDATTR_XSCALEFACTOR;
    public static final int ATTR_YSCALEFACTOR =  ATTR_FIRST + 22;
    public static final RecordField RECORDATTR_YSCALEFACTOR;
    public static final int ATTR_ZSCALEFACTOR =  ATTR_FIRST + 23;
    public static final RecordField RECORDATTR_ZSCALEFACTOR;
    public static final int ATTR_XOFFSET =  ATTR_FIRST + 24;
    public static final RecordField RECORDATTR_XOFFSET;
    public static final int ATTR_YOFFSET =  ATTR_FIRST + 25;
    public static final RecordField RECORDATTR_YOFFSET;
    public static final int ATTR_ZOFFSET =  ATTR_FIRST + 26;
    public static final RecordField RECORDATTR_ZOFFSET;
    public static final int ATTR_MAXX =  ATTR_FIRST + 27;
    public static final RecordField RECORDATTR_MAXX;
    public static final int ATTR_MINX =  ATTR_FIRST + 28;
    public static final RecordField RECORDATTR_MINX;
    public static final int ATTR_MAXY =  ATTR_FIRST + 29;
    public static final RecordField RECORDATTR_MAXY;
    public static final int ATTR_MINY =  ATTR_FIRST + 30;
    public static final RecordField RECORDATTR_MINY;
    public static final int ATTR_MAXZ =  ATTR_FIRST + 31;
    public static final RecordField RECORDATTR_MAXZ;
    public static final int ATTR_MINZ =  ATTR_FIRST + 32;
    public static final RecordField RECORDATTR_MINZ;
    public static final int ATTR_LAST = ATTR_FIRST + 33;
    

    static {
    FIELDS.add(RECORDATTR_FILESIGNATURE = new RecordField("fileSignature", "fileSignature", "", ATTR_FILESIGNATURE, "", "string[4]", "byte", 4, SEARCHABLE_NO,CHARTABLE_NO));
    FIELDS.add(RECORDATTR_FILESOURCEID = new RecordField("fileSourceId", "fileSourceId", "", ATTR_FILESOURCEID, "", "ushort", "short", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_FILESOURCEID.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LasHeader_V12)record).fileSourceId;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LasHeader_V12)record).fileSourceId;
    }
    });
    FIELDS.add(RECORDATTR_GLOBALENCODING = new RecordField("globalEncoding", "globalEncoding", "", ATTR_GLOBALENCODING, "", "ushort", "short", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_GLOBALENCODING.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LasHeader_V12)record).globalEncoding;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LasHeader_V12)record).globalEncoding;
    }
    });
    FIELDS.add(RECORDATTR_PROJECTIDGUIDDATA1 = new RecordField("projectIdGuidData1", "projectIdGuidData1", "", ATTR_PROJECTIDGUIDDATA1, "", "uint", "int", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_PROJECTIDGUIDDATA1.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LasHeader_V12)record).projectIdGuidData1;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LasHeader_V12)record).projectIdGuidData1;
    }
    });
    FIELDS.add(RECORDATTR_PROJECTIDGUIDDATA2 = new RecordField("projectIdGuidData2", "projectIdGuidData2", "", ATTR_PROJECTIDGUIDDATA2, "", "ushort", "short", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_PROJECTIDGUIDDATA2.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LasHeader_V12)record).projectIdGuidData2;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LasHeader_V12)record).projectIdGuidData2;
    }
    });
    FIELDS.add(RECORDATTR_PROJECTIDGUIDDATA3 = new RecordField("projectIdGuidData3", "projectIdGuidData3", "", ATTR_PROJECTIDGUIDDATA3, "", "ushort", "short", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_PROJECTIDGUIDDATA3.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LasHeader_V12)record).projectIdGuidData3;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LasHeader_V12)record).projectIdGuidData3;
    }
    });
    FIELDS.add(RECORDATTR_PROJECTIDGUIDDATA4 = new RecordField("projectIdGuidData4", "projectIdGuidData4", "", ATTR_PROJECTIDGUIDDATA4, "", "string[8]", "byte", 8, SEARCHABLE_NO,CHARTABLE_NO));
    FIELDS.add(RECORDATTR_VERSIONMAJOR = new RecordField("versionMajor", "versionMajor", "", ATTR_VERSIONMAJOR, "", "byte", "byte", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_VERSIONMAJOR.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LasHeader_V12)record).versionMajor;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LasHeader_V12)record).versionMajor;
    }
    });
    FIELDS.add(RECORDATTR_VERSIONMINOR = new RecordField("versionMinor", "versionMinor", "", ATTR_VERSIONMINOR, "", "byte", "byte", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_VERSIONMINOR.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LasHeader_V12)record).versionMinor;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LasHeader_V12)record).versionMinor;
    }
    });
    FIELDS.add(RECORDATTR_SYSTEMIDENTIFIER = new RecordField("systemIdentifier", "systemIdentifier", "", ATTR_SYSTEMIDENTIFIER, "", "string[32]", "byte", 32, SEARCHABLE_NO,CHARTABLE_NO));
    FIELDS.add(RECORDATTR_GENERATINGSOFTWARE = new RecordField("generatingSoftware", "generatingSoftware", "", ATTR_GENERATINGSOFTWARE, "", "string[32]", "byte", 32, SEARCHABLE_NO,CHARTABLE_NO));
    FIELDS.add(RECORDATTR_FILECREATIONDAYOFYEAR = new RecordField("fileCreationDayOfYear", "fileCreationDayOfYear", "", ATTR_FILECREATIONDAYOFYEAR, "", "ushort", "short", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_FILECREATIONDAYOFYEAR.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LasHeader_V12)record).fileCreationDayOfYear;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LasHeader_V12)record).fileCreationDayOfYear;
    }
    });
    FIELDS.add(RECORDATTR_FILECREATIONYEAR = new RecordField("fileCreationYear", "fileCreationYear", "", ATTR_FILECREATIONYEAR, "", "ushort", "short", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_FILECREATIONYEAR.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LasHeader_V12)record).fileCreationYear;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LasHeader_V12)record).fileCreationYear;
    }
    });
    FIELDS.add(RECORDATTR_HEADERSIZE = new RecordField("headerSize", "headerSize", "", ATTR_HEADERSIZE, "", "ushort", "short", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_HEADERSIZE.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LasHeader_V12)record).headerSize;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LasHeader_V12)record).headerSize;
    }
    });
    FIELDS.add(RECORDATTR_OFFSETTOPOINTDATA = new RecordField("offsetToPointData", "offsetToPointData", "", ATTR_OFFSETTOPOINTDATA, "", "uint", "int", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_OFFSETTOPOINTDATA.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LasHeader_V12)record).offsetToPointData;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LasHeader_V12)record).offsetToPointData;
    }
    });
    FIELDS.add(RECORDATTR_NUMBEROFVARIABLELENGTHRECORDS = new RecordField("numberOfVariableLengthRecords", "numberOfVariableLengthRecords", "", ATTR_NUMBEROFVARIABLELENGTHRECORDS, "", "uint", "int", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_NUMBEROFVARIABLELENGTHRECORDS.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LasHeader_V12)record).numberOfVariableLengthRecords;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LasHeader_V12)record).numberOfVariableLengthRecords;
    }
    });
    FIELDS.add(RECORDATTR_POINTDATAFORMATID = new RecordField("pointDataFormatId", "pointDataFormatId", "", ATTR_POINTDATAFORMATID, "", "byte", "byte", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_POINTDATAFORMATID.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LasHeader_V12)record).pointDataFormatId;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LasHeader_V12)record).pointDataFormatId;
    }
    });
    FIELDS.add(RECORDATTR_POINTDATARECORDLENGTH = new RecordField("pointDataRecordLength", "pointDataRecordLength", "", ATTR_POINTDATARECORDLENGTH, "", "ushort", "short", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_POINTDATARECORDLENGTH.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LasHeader_V12)record).pointDataRecordLength;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LasHeader_V12)record).pointDataRecordLength;
    }
    });
    FIELDS.add(RECORDATTR_NUMBEROFPOINTRECORDS = new RecordField("numberOfPointRecords", "numberOfPointRecords", "", ATTR_NUMBEROFPOINTRECORDS, "", "uint", "int", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_NUMBEROFPOINTRECORDS.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LasHeader_V12)record).numberOfPointRecords;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LasHeader_V12)record).numberOfPointRecords;
    }
    });
    FIELDS.add(RECORDATTR_NUMBEROFPOINTSBYRETURN = new RecordField("numberOfPointsByReturn", "numberOfPointsByReturn", "", ATTR_NUMBEROFPOINTSBYRETURN, "", "uint[5]", "int", 5, SEARCHABLE_NO,CHARTABLE_NO));
    FIELDS.add(RECORDATTR_XSCALEFACTOR = new RecordField("xScaleFactor", "xScaleFactor", "", ATTR_XSCALEFACTOR, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_XSCALEFACTOR.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LasHeader_V12)record).xScaleFactor;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LasHeader_V12)record).xScaleFactor;
    }
    });
    FIELDS.add(RECORDATTR_YSCALEFACTOR = new RecordField("yScaleFactor", "yScaleFactor", "", ATTR_YSCALEFACTOR, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_YSCALEFACTOR.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LasHeader_V12)record).yScaleFactor;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LasHeader_V12)record).yScaleFactor;
    }
    });
    FIELDS.add(RECORDATTR_ZSCALEFACTOR = new RecordField("zScaleFactor", "zScaleFactor", "", ATTR_ZSCALEFACTOR, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_ZSCALEFACTOR.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LasHeader_V12)record).zScaleFactor;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LasHeader_V12)record).zScaleFactor;
    }
    });
    FIELDS.add(RECORDATTR_XOFFSET = new RecordField("xOffset", "xOffset", "", ATTR_XOFFSET, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_XOFFSET.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LasHeader_V12)record).xOffset;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LasHeader_V12)record).xOffset;
    }
    });
    FIELDS.add(RECORDATTR_YOFFSET = new RecordField("yOffset", "yOffset", "", ATTR_YOFFSET, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_YOFFSET.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LasHeader_V12)record).yOffset;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LasHeader_V12)record).yOffset;
    }
    });
    FIELDS.add(RECORDATTR_ZOFFSET = new RecordField("zOffset", "zOffset", "", ATTR_ZOFFSET, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_ZOFFSET.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LasHeader_V12)record).zOffset;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LasHeader_V12)record).zOffset;
    }
    });
    FIELDS.add(RECORDATTR_MAXX = new RecordField("maxX", "maxX", "", ATTR_MAXX, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_MAXX.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LasHeader_V12)record).maxX;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LasHeader_V12)record).maxX;
    }
    });
    FIELDS.add(RECORDATTR_MINX = new RecordField("minX", "minX", "", ATTR_MINX, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_MINX.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LasHeader_V12)record).minX;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LasHeader_V12)record).minX;
    }
    });
    FIELDS.add(RECORDATTR_MAXY = new RecordField("maxY", "maxY", "", ATTR_MAXY, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_MAXY.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LasHeader_V12)record).maxY;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LasHeader_V12)record).maxY;
    }
    });
    FIELDS.add(RECORDATTR_MINY = new RecordField("minY", "minY", "", ATTR_MINY, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_MINY.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LasHeader_V12)record).minY;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LasHeader_V12)record).minY;
    }
    });
    FIELDS.add(RECORDATTR_MAXZ = new RecordField("maxZ", "maxZ", "", ATTR_MAXZ, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_MAXZ.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LasHeader_V12)record).maxZ;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LasHeader_V12)record).maxZ;
    }
    });
    FIELDS.add(RECORDATTR_MINZ = new RecordField("minZ", "minZ", "", ATTR_MINZ, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_MINZ.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LasHeader_V12)record).minZ;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LasHeader_V12)record).minZ;
    }
    });
    
    }
    

    byte[] fileSignature = createByteArray("LASF", 4);
    String fileSignatureAsString;
    int fileSourceId = 1;
    int globalEncoding;
    long projectIdGuidData1;
    int projectIdGuidData2;
    int projectIdGuidData3;
    byte[] projectIdGuidData4 = new byte[8];
    String projectIdGuidData4AsString;
    byte versionMajor = 1;
    byte versionMinor = 2;
    byte[] systemIdentifier = new byte[32];
    String systemIdentifierAsString;
    byte[] generatingSoftware = new byte[32];
    String generatingSoftwareAsString;
    int fileCreationDayOfYear;
    int fileCreationYear;
    int headerSize;
    long offsetToPointData;
    long numberOfVariableLengthRecords;
    byte pointDataFormatId;
    int pointDataRecordLength;
    long numberOfPointRecords;
    long[] numberOfPointsByReturn = new long[5];
    double xScaleFactor = 1.0;
    double yScaleFactor = 1.0;
    double zScaleFactor = 1.0;
    double xOffset = 0;
    double yOffset = 0;
    double zOffset = 0;
    double maxX;
    double minX;
    double maxY;
    double minY;
    double maxZ;
    double minZ;
    

    public  LasHeader_V12(LasHeader_V12 that)  {
        super(that);
        this.fileSignature = that.fileSignature;
        this.fileSourceId = that.fileSourceId;
        this.globalEncoding = that.globalEncoding;
        this.projectIdGuidData1 = that.projectIdGuidData1;
        this.projectIdGuidData2 = that.projectIdGuidData2;
        this.projectIdGuidData3 = that.projectIdGuidData3;
        this.projectIdGuidData4 = that.projectIdGuidData4;
        this.versionMajor = that.versionMajor;
        this.versionMinor = that.versionMinor;
        this.systemIdentifier = that.systemIdentifier;
        this.generatingSoftware = that.generatingSoftware;
        this.fileCreationDayOfYear = that.fileCreationDayOfYear;
        this.fileCreationYear = that.fileCreationYear;
        this.headerSize = that.headerSize;
        this.offsetToPointData = that.offsetToPointData;
        this.numberOfVariableLengthRecords = that.numberOfVariableLengthRecords;
        this.pointDataFormatId = that.pointDataFormatId;
        this.pointDataRecordLength = that.pointDataRecordLength;
        this.numberOfPointRecords = that.numberOfPointRecords;
        this.numberOfPointsByReturn = that.numberOfPointsByReturn;
        this.xScaleFactor = that.xScaleFactor;
        this.yScaleFactor = that.yScaleFactor;
        this.zScaleFactor = that.zScaleFactor;
        this.xOffset = that.xOffset;
        this.yOffset = that.yOffset;
        this.zOffset = that.zOffset;
        this.maxX = that.maxX;
        this.minX = that.minX;
        this.maxY = that.maxY;
        this.minY = that.minY;
        this.maxZ = that.maxZ;
        this.minZ = that.minZ;
        
        
    }



    public  LasHeader_V12(RecordFile file)  {
        super(file);
    }



    public  LasHeader_V12(RecordFile file, boolean bigEndian)  {
        super(file, bigEndian);
    }



    public int getLastAttribute()  {
        return ATTR_LAST;
    }



    public  boolean equals(Object object)  {
        if(!super.equals(object)) {System.err.println("bad super"); return false;} if(!(object instanceof LasHeader_V12)) return false;
        LasHeader_V12 that = (LasHeader_V12 ) object;
        if(!java.util.Arrays.equals(this.fileSignature, that.fileSignature)) {System.err.println("bad fileSignature"); return false;}
        if(this.fileSourceId!= that.fileSourceId) {System.err.println("bad fileSourceId");  return false;}
        if(this.globalEncoding!= that.globalEncoding) {System.err.println("bad globalEncoding");  return false;}
        if(this.projectIdGuidData1!= that.projectIdGuidData1) {System.err.println("bad projectIdGuidData1");  return false;}
        if(this.projectIdGuidData2!= that.projectIdGuidData2) {System.err.println("bad projectIdGuidData2");  return false;}
        if(this.projectIdGuidData3!= that.projectIdGuidData3) {System.err.println("bad projectIdGuidData3");  return false;}
        if(!java.util.Arrays.equals(this.projectIdGuidData4, that.projectIdGuidData4)) {System.err.println("bad projectIdGuidData4"); return false;}
        if(this.versionMajor!= that.versionMajor) {System.err.println("bad versionMajor");  return false;}
        if(this.versionMinor!= that.versionMinor) {System.err.println("bad versionMinor");  return false;}
        if(!java.util.Arrays.equals(this.systemIdentifier, that.systemIdentifier)) {System.err.println("bad systemIdentifier"); return false;}
        if(!java.util.Arrays.equals(this.generatingSoftware, that.generatingSoftware)) {System.err.println("bad generatingSoftware"); return false;}
        if(this.fileCreationDayOfYear!= that.fileCreationDayOfYear) {System.err.println("bad fileCreationDayOfYear");  return false;}
        if(this.fileCreationYear!= that.fileCreationYear) {System.err.println("bad fileCreationYear");  return false;}
        if(this.headerSize!= that.headerSize) {System.err.println("bad headerSize");  return false;}
        if(this.offsetToPointData!= that.offsetToPointData) {System.err.println("bad offsetToPointData");  return false;}
        if(this.numberOfVariableLengthRecords!= that.numberOfVariableLengthRecords) {System.err.println("bad numberOfVariableLengthRecords");  return false;}
        if(this.pointDataFormatId!= that.pointDataFormatId) {System.err.println("bad pointDataFormatId");  return false;}
        if(this.pointDataRecordLength!= that.pointDataRecordLength) {System.err.println("bad pointDataRecordLength");  return false;}
        if(this.numberOfPointRecords!= that.numberOfPointRecords) {System.err.println("bad numberOfPointRecords");  return false;}
        if(!java.util.Arrays.equals(this.numberOfPointsByReturn, that.numberOfPointsByReturn)) {System.err.println("bad numberOfPointsByReturn"); return false;}
        if(this.xScaleFactor!= that.xScaleFactor) {System.err.println("bad xScaleFactor");  return false;}
        if(this.yScaleFactor!= that.yScaleFactor) {System.err.println("bad yScaleFactor");  return false;}
        if(this.zScaleFactor!= that.zScaleFactor) {System.err.println("bad zScaleFactor");  return false;}
        if(this.xOffset!= that.xOffset) {System.err.println("bad xOffset");  return false;}
        if(this.yOffset!= that.yOffset) {System.err.println("bad yOffset");  return false;}
        if(this.zOffset!= that.zOffset) {System.err.println("bad zOffset");  return false;}
        if(this.maxX!= that.maxX) {System.err.println("bad maxX");  return false;}
        if(this.minX!= that.minX) {System.err.println("bad minX");  return false;}
        if(this.maxY!= that.maxY) {System.err.println("bad maxY");  return false;}
        if(this.minY!= that.minY) {System.err.println("bad minY");  return false;}
        if(this.maxZ!= that.maxZ) {System.err.println("bad maxZ");  return false;}
        if(this.minZ!= that.minZ) {System.err.println("bad minZ");  return false;}
        return true;
    }



    public boolean getGPSTimeType()  {
        return isBitSet(globalEncoding,0);
    }


    public void setGPSTimeType(boolean flag)  {
        globalEncoding = setBit(globalEncoding,0,flag);
    }


    public boolean getWaveFormDataPacketsInternal()  {
        return isBitSet(globalEncoding,1);
    }


    public void setWaveFormDataPacketsInternal(boolean flag)  {
        globalEncoding = setBit(globalEncoding,1,flag);
    }


    public boolean getWaveFormDataPacketsExternal()  {
        return isBitSet(globalEncoding,2);
    }


    public void setWaveFormDataPacketsExternal(boolean flag)  {
        globalEncoding = setBit(globalEncoding,2,flag);
    }


    public boolean getSyntheticReturnNumbers()  {
        return isBitSet(globalEncoding,3);
    }


    public void setSyntheticReturnNumbers(boolean flag)  {
        globalEncoding = setBit(globalEncoding,3,flag);
    }



    protected void addFields(List<RecordField> fields)  {
        super.addFields(fields);
        fields.addAll(FIELDS);
    }



    public double getValue(int attrId)  {
        if(attrId == ATTR_FILESOURCEID) return fileSourceId;
        if(attrId == ATTR_GLOBALENCODING) return globalEncoding;
        if(attrId == ATTR_PROJECTIDGUIDDATA1) return projectIdGuidData1;
        if(attrId == ATTR_PROJECTIDGUIDDATA2) return projectIdGuidData2;
        if(attrId == ATTR_PROJECTIDGUIDDATA3) return projectIdGuidData3;
        if(attrId == ATTR_VERSIONMAJOR) return versionMajor;
        if(attrId == ATTR_VERSIONMINOR) return versionMinor;
        if(attrId == ATTR_FILECREATIONDAYOFYEAR) return fileCreationDayOfYear;
        if(attrId == ATTR_FILECREATIONYEAR) return fileCreationYear;
        if(attrId == ATTR_HEADERSIZE) return headerSize;
        if(attrId == ATTR_OFFSETTOPOINTDATA) return offsetToPointData;
        if(attrId == ATTR_NUMBEROFVARIABLELENGTHRECORDS) return numberOfVariableLengthRecords;
        if(attrId == ATTR_POINTDATAFORMATID) return pointDataFormatId;
        if(attrId == ATTR_POINTDATARECORDLENGTH) return pointDataRecordLength;
        if(attrId == ATTR_NUMBEROFPOINTRECORDS) return numberOfPointRecords;
        if(attrId == ATTR_XSCALEFACTOR) return xScaleFactor;
        if(attrId == ATTR_YSCALEFACTOR) return yScaleFactor;
        if(attrId == ATTR_ZSCALEFACTOR) return zScaleFactor;
        if(attrId == ATTR_XOFFSET) return xOffset;
        if(attrId == ATTR_YOFFSET) return yOffset;
        if(attrId == ATTR_ZOFFSET) return zOffset;
        if(attrId == ATTR_MAXX) return maxX;
        if(attrId == ATTR_MINX) return minX;
        if(attrId == ATTR_MAXY) return maxY;
        if(attrId == ATTR_MINY) return minY;
        if(attrId == ATTR_MAXZ) return maxZ;
        if(attrId == ATTR_MINZ) return minZ;
        return super.getValue(attrId);
        
    }



    public int getRecordSize()  {
        return super.getRecordSize() + 227;
    }



    public BaseRecord.ReadStatus read(RecordIO recordIO) throws Exception  {
        DataInputStream dis = recordIO.getDataInputStream();
        BaseRecord.ReadStatus status= super.read(recordIO);
        if(status!=BaseRecord.ReadStatus.OK)  return status;
        readBytes(dis,fileSignature);
        fileSourceId =  readUnsignedShort(dis);
        globalEncoding =  readUnsignedShort(dis);
        projectIdGuidData1 =  readUnsignedInt(dis);
        projectIdGuidData2 =  readUnsignedShort(dis);
        projectIdGuidData3 =  readUnsignedShort(dis);
        readBytes(dis,projectIdGuidData4);
        versionMajor =  readByte(dis);
        versionMinor =  readByte(dis);
        readBytes(dis,systemIdentifier);
        readBytes(dis,generatingSoftware);
        fileCreationDayOfYear =  readUnsignedShort(dis);
        fileCreationYear =  readUnsignedShort(dis);
        headerSize =  readUnsignedShort(dis);
        offsetToPointData =  readUnsignedInt(dis);
        numberOfVariableLengthRecords =  readUnsignedInt(dis);
        pointDataFormatId =  readByte(dis);
        pointDataRecordLength =  readUnsignedShort(dis);
        numberOfPointRecords =  readUnsignedInt(dis);
        readUnsignedInts(dis,numberOfPointsByReturn);
        xScaleFactor =  readDouble(dis);
        yScaleFactor =  readDouble(dis);
        zScaleFactor =  readDouble(dis);
        xOffset =  readDouble(dis);
        yOffset =  readDouble(dis);
        zOffset =  readDouble(dis);
        maxX =  readDouble(dis);
        minX =  readDouble(dis);
        maxY =  readDouble(dis);
        minY =  readDouble(dis);
        maxZ =  readDouble(dis);
        minZ =  readDouble(dis);
        
        
        return BaseRecord.ReadStatus.OK;
    }



    public void write(RecordIO recordIO) throws IOException  {
        DataOutputStream dos = recordIO.getDataOutputStream();
        super.write(recordIO);
        write(dos, fileSignature);
        writeUnsignedShort(dos, fileSourceId);
        writeUnsignedShort(dos, globalEncoding);
        writeUnsignedInt(dos, projectIdGuidData1);
        writeUnsignedShort(dos, projectIdGuidData2);
        writeUnsignedShort(dos, projectIdGuidData3);
        write(dos, projectIdGuidData4);
        writeByte(dos, versionMajor);
        writeByte(dos, versionMinor);
        write(dos, systemIdentifier);
        write(dos, generatingSoftware);
        writeUnsignedShort(dos, fileCreationDayOfYear);
        writeUnsignedShort(dos, fileCreationYear);
        writeUnsignedShort(dos, headerSize);
        writeUnsignedInt(dos, offsetToPointData);
        writeUnsignedInt(dos, numberOfVariableLengthRecords);
        writeByte(dos, pointDataFormatId);
        writeUnsignedShort(dos, pointDataRecordLength);
        writeUnsignedInt(dos, numberOfPointRecords);
        writeUnsignedInts(dos, numberOfPointsByReturn);
        writeDouble(dos, xScaleFactor);
        writeDouble(dos, yScaleFactor);
        writeDouble(dos, zScaleFactor);
        writeDouble(dos, xOffset);
        writeDouble(dos, yOffset);
        writeDouble(dos, zOffset);
        writeDouble(dos, maxX);
        writeDouble(dos, minX);
        writeDouble(dos, maxY);
        writeDouble(dos, minY);
        writeDouble(dos, maxZ);
        writeDouble(dos, minZ);
        
    }



    public int doPrintCsv(VisitInfo visitInfo,PrintWriter pw)  {
        boolean includeVector = visitInfo.getProperty(PROP_INCLUDEVECTOR, false);
        int superCnt = super.doPrintCsv(visitInfo,pw);
        int myCnt = 0;
        if(superCnt>0) pw.print(',');
        pw.print(getStringValue(RECORDATTR_FILESOURCEID, fileSourceId));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_GLOBALENCODING, globalEncoding));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_PROJECTIDGUIDDATA1, projectIdGuidData1));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_PROJECTIDGUIDDATA2, projectIdGuidData2));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_PROJECTIDGUIDDATA3, projectIdGuidData3));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_VERSIONMAJOR, versionMajor));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_VERSIONMINOR, versionMinor));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_FILECREATIONDAYOFYEAR, fileCreationDayOfYear));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_FILECREATIONYEAR, fileCreationYear));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_HEADERSIZE, headerSize));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_OFFSETTOPOINTDATA, offsetToPointData));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_NUMBEROFVARIABLELENGTHRECORDS, numberOfVariableLengthRecords));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_POINTDATAFORMATID, pointDataFormatId));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_POINTDATARECORDLENGTH, pointDataRecordLength));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_NUMBEROFPOINTRECORDS, numberOfPointRecords));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_XSCALEFACTOR, xScaleFactor));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_YSCALEFACTOR, yScaleFactor));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_ZSCALEFACTOR, zScaleFactor));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_XOFFSET, xOffset));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_YOFFSET, yOffset));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_ZOFFSET, zOffset));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_MAXX, maxX));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_MINX, minX));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_MAXY, maxY));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_MINY, minY));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_MAXZ, maxZ));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_MINZ, minZ));
        myCnt++;
        if(includeVector) {
        for(int i=0;i<this.fileSignature.length;i++) {pw.print(i==0?'|':',');pw.print(this.fileSignature[i]);}
        myCnt++;
        }
        if(includeVector) {
        for(int i=0;i<this.projectIdGuidData4.length;i++) {pw.print(i==0?'|':',');pw.print(this.projectIdGuidData4[i]);}
        myCnt++;
        }
        if(includeVector) {
        for(int i=0;i<this.systemIdentifier.length;i++) {pw.print(i==0?'|':',');pw.print(this.systemIdentifier[i]);}
        myCnt++;
        }
        if(includeVector) {
        for(int i=0;i<this.generatingSoftware.length;i++) {pw.print(i==0?'|':',');pw.print(this.generatingSoftware[i]);}
        myCnt++;
        }
        if(includeVector) {
        for(int i=0;i<this.numberOfPointsByReturn.length;i++) {pw.print(i==0?'|':',');pw.print(this.numberOfPointsByReturn[i]);}
        myCnt++;
        }
        return myCnt+superCnt;
        
    }



    public int doPrintCsvHeader(VisitInfo visitInfo,PrintWriter pw)  {
        int superCnt = super.doPrintCsvHeader(visitInfo,pw);
        int myCnt = 0;
        boolean includeVector = visitInfo.getProperty(PROP_INCLUDEVECTOR, false);
        if(superCnt>0) pw.print(',');
        RECORDATTR_FILESOURCEID.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_GLOBALENCODING.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_PROJECTIDGUIDDATA1.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_PROJECTIDGUIDDATA2.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_PROJECTIDGUIDDATA3.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_VERSIONMAJOR.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_VERSIONMINOR.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_FILECREATIONDAYOFYEAR.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_FILECREATIONYEAR.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_HEADERSIZE.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_OFFSETTOPOINTDATA.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_NUMBEROFVARIABLELENGTHRECORDS.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_POINTDATAFORMATID.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_POINTDATARECORDLENGTH.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_NUMBEROFPOINTRECORDS.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_XSCALEFACTOR.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_YSCALEFACTOR.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_ZSCALEFACTOR.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_XOFFSET.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_YOFFSET.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_ZOFFSET.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_MAXX.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_MINX.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_MAXY.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_MINY.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_MAXZ.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_MINZ.printCsvHeader(visitInfo,pw);
        myCnt++;
        if(includeVector) {
        pw.print(',');
        RECORDATTR_FILESIGNATURE.printCsvHeader(visitInfo,pw);
        myCnt++;
        }
        if(includeVector) {
        pw.print(',');
        RECORDATTR_PROJECTIDGUIDDATA4.printCsvHeader(visitInfo,pw);
        myCnt++;
        }
        if(includeVector) {
        pw.print(',');
        RECORDATTR_SYSTEMIDENTIFIER.printCsvHeader(visitInfo,pw);
        myCnt++;
        }
        if(includeVector) {
        pw.print(',');
        RECORDATTR_GENERATINGSOFTWARE.printCsvHeader(visitInfo,pw);
        myCnt++;
        }
        if(includeVector) {
        pw.print(',');
        RECORDATTR_NUMBEROFPOINTSBYRETURN.printCsvHeader(visitInfo,pw);
        myCnt++;
        }
        return myCnt+superCnt;
        
    }



    public void print(Appendable buff)  throws Exception  {
        super.print(buff);
        buff.append(" fileSignature: " + getFileSignatureAsString()+" \n");
        buff.append(" fileSourceId: " + fileSourceId+" \n");
        buff.append(" globalEncoding: " + globalEncoding+" \n");
        buff.append(" projectIdGuidData1: " + projectIdGuidData1+" \n");
        buff.append(" projectIdGuidData2: " + projectIdGuidData2+" \n");
        buff.append(" projectIdGuidData3: " + projectIdGuidData3+" \n");
        buff.append(" projectIdGuidData4: " + getProjectIdGuidData4AsString()+" \n");
        buff.append(" versionMajor: " + versionMajor+" \n");
        buff.append(" versionMinor: " + versionMinor+" \n");
        buff.append(" systemIdentifier: " + getSystemIdentifierAsString()+" \n");
        buff.append(" generatingSoftware: " + getGeneratingSoftwareAsString()+" \n");
        buff.append(" fileCreationDayOfYear: " + fileCreationDayOfYear+" \n");
        buff.append(" fileCreationYear: " + fileCreationYear+" \n");
        buff.append(" headerSize: " + headerSize+" \n");
        buff.append(" offsetToPointData: " + offsetToPointData+" \n");
        buff.append(" numberOfVariableLengthRecords: " + numberOfVariableLengthRecords+" \n");
        buff.append(" pointDataFormatId: " + pointDataFormatId+" \n");
        buff.append(" pointDataRecordLength: " + pointDataRecordLength+" \n");
        buff.append(" numberOfPointRecords: " + numberOfPointRecords+" \n");
        buff.append(" xScaleFactor: " + xScaleFactor+" \n");
        buff.append(" yScaleFactor: " + yScaleFactor+" \n");
        buff.append(" zScaleFactor: " + zScaleFactor+" \n");
        buff.append(" xOffset: " + xOffset+" \n");
        buff.append(" yOffset: " + yOffset+" \n");
        buff.append(" zOffset: " + zOffset+" \n");
        buff.append(" maxX: " + maxX+" \n");
        buff.append(" minX: " + minX+" \n");
        buff.append(" maxY: " + maxY+" \n");
        buff.append(" minY: " + minY+" \n");
        buff.append(" maxZ: " + maxZ+" \n");
        buff.append(" minZ: " + minZ+" \n");
        
    }



    public byte[] getFileSignature()  {
        return fileSignature;
    }


    public String getFileSignatureAsString()  {
        if(fileSignatureAsString==null) fileSignatureAsString =new String(fileSignature);
        return fileSignatureAsString;
    }


    public void setFileSignature(byte[] newValue)  {
        copy(fileSignature, newValue);
    }


    public void setFileSignature(String  newValue)  {
        copy(fileSignature, newValue.getBytes());
    }


    public int getFileSourceId()  {
        return fileSourceId;
    }


    public void setFileSourceId(int newValue)  {
        fileSourceId = newValue;
    }


    public int getGlobalEncoding()  {
        return globalEncoding;
    }


    public void setGlobalEncoding(int newValue)  {
        globalEncoding = newValue;
    }


    public long getProjectIdGuidData1()  {
        return projectIdGuidData1;
    }


    public void setProjectIdGuidData1(long newValue)  {
        projectIdGuidData1 = newValue;
    }


    public int getProjectIdGuidData2()  {
        return projectIdGuidData2;
    }


    public void setProjectIdGuidData2(int newValue)  {
        projectIdGuidData2 = newValue;
    }


    public int getProjectIdGuidData3()  {
        return projectIdGuidData3;
    }


    public void setProjectIdGuidData3(int newValue)  {
        projectIdGuidData3 = newValue;
    }


    public byte[] getProjectIdGuidData4()  {
        return projectIdGuidData4;
    }


    public String getProjectIdGuidData4AsString()  {
        if(projectIdGuidData4AsString==null) projectIdGuidData4AsString =new String(projectIdGuidData4);
        return projectIdGuidData4AsString;
    }


    public void setProjectIdGuidData4(byte[] newValue)  {
        copy(projectIdGuidData4, newValue);
    }


    public void setProjectIdGuidData4(String  newValue)  {
        copy(projectIdGuidData4, newValue.getBytes());
    }


    public byte getVersionMajor()  {
        return versionMajor;
    }


    public void setVersionMajor(byte newValue)  {
        versionMajor = newValue;
    }


    public byte getVersionMinor()  {
        return versionMinor;
    }


    public void setVersionMinor(byte newValue)  {
        versionMinor = newValue;
    }


    public byte[] getSystemIdentifier()  {
        return systemIdentifier;
    }


    public String getSystemIdentifierAsString()  {
        if(systemIdentifierAsString==null) systemIdentifierAsString =new String(systemIdentifier);
        return systemIdentifierAsString;
    }


    public void setSystemIdentifier(byte[] newValue)  {
        copy(systemIdentifier, newValue);
    }


    public void setSystemIdentifier(String  newValue)  {
        copy(systemIdentifier, newValue.getBytes());
    }


    public byte[] getGeneratingSoftware()  {
        return generatingSoftware;
    }


    public String getGeneratingSoftwareAsString()  {
        if(generatingSoftwareAsString==null) generatingSoftwareAsString =new String(generatingSoftware);
        return generatingSoftwareAsString;
    }


    public void setGeneratingSoftware(byte[] newValue)  {
        copy(generatingSoftware, newValue);
    }


    public void setGeneratingSoftware(String  newValue)  {
        copy(generatingSoftware, newValue.getBytes());
    }


    public int getFileCreationDayOfYear()  {
        return fileCreationDayOfYear;
    }


    public void setFileCreationDayOfYear(int newValue)  {
        fileCreationDayOfYear = newValue;
    }


    public int getFileCreationYear()  {
        return fileCreationYear;
    }


    public void setFileCreationYear(int newValue)  {
        fileCreationYear = newValue;
    }


    public int getHeaderSize()  {
        return headerSize;
    }


    public void setHeaderSize(int newValue)  {
        headerSize = newValue;
    }


    public long getOffsetToPointData()  {
        return offsetToPointData;
    }


    public void setOffsetToPointData(long newValue)  {
        offsetToPointData = newValue;
    }


    public long getNumberOfVariableLengthRecords()  {
        return numberOfVariableLengthRecords;
    }


    public void setNumberOfVariableLengthRecords(long newValue)  {
        numberOfVariableLengthRecords = newValue;
    }


    public byte getPointDataFormatId()  {
        return pointDataFormatId;
    }


    public void setPointDataFormatId(byte newValue)  {
        pointDataFormatId = newValue;
    }


    public int getPointDataRecordLength()  {
        return pointDataRecordLength;
    }


    public void setPointDataRecordLength(int newValue)  {
        pointDataRecordLength = newValue;
    }


    public long getNumberOfPointRecords()  {
        return numberOfPointRecords;
    }


    public void setNumberOfPointRecords(long newValue)  {
        numberOfPointRecords = newValue;
    }


    public long[] getNumberOfPointsByReturn()  {
        return numberOfPointsByReturn;
    }


    public void setNumberOfPointsByReturn(long[] newValue)  {
        copy(numberOfPointsByReturn, newValue);
    }


    public double getXScaleFactor()  {
        return xScaleFactor;
    }


    public void setXScaleFactor(double newValue)  {
        xScaleFactor = newValue;
    }


    public double getYScaleFactor()  {
        return yScaleFactor;
    }


    public void setYScaleFactor(double newValue)  {
        yScaleFactor = newValue;
    }


    public double getZScaleFactor()  {
        return zScaleFactor;
    }


    public void setZScaleFactor(double newValue)  {
        zScaleFactor = newValue;
    }


    public double getXOffset()  {
        return xOffset;
    }


    public void setXOffset(double newValue)  {
        xOffset = newValue;
    }


    public double getYOffset()  {
        return yOffset;
    }


    public void setYOffset(double newValue)  {
        yOffset = newValue;
    }


    public double getZOffset()  {
        return zOffset;
    }


    public void setZOffset(double newValue)  {
        zOffset = newValue;
    }


    public double getMaxX()  {
        return maxX;
    }


    public void setMaxX(double newValue)  {
        maxX = newValue;
    }


    public double getMinX()  {
        return minX;
    }


    public void setMinX(double newValue)  {
        minX = newValue;
    }


    public double getMaxY()  {
        return maxY;
    }


    public void setMaxY(double newValue)  {
        maxY = newValue;
    }


    public double getMinY()  {
        return minY;
    }


    public void setMinY(double newValue)  {
        minY = newValue;
    }


    public double getMaxZ()  {
        return maxZ;
    }


    public void setMaxZ(double newValue)  {
        maxZ = newValue;
    }


    public double getMinZ()  {
        return minZ;
    }


    public void setMinZ(double newValue)  {
        minZ = newValue;
    }



}



