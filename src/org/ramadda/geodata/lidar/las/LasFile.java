/*
* Copyright (c) 2008-2019 Geode Systems LLC
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
*     http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.ramadda.geodata.lidar.las;



import org.ramadda.util.IO;
import org.ramadda.data.record.*;
import org.ramadda.geodata.lidar.*;


import org.ramadda.geodata.lidar.geotiff.*;
import org.ramadda.util.geo.GeoUtils;



import ucar.unidata.gis.epsg.CoordinateOperationMethod;
import ucar.unidata.gis.epsg.CoordinateOperationParameter;
import ucar.unidata.gis.epsg.Pcs;
import ucar.unidata.gis.geotiff.GeneratedKeys;

import ucar.unidata.gis.geotiff.GeoKeys;

import java.io.*;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;


/**
 * Class description
 *
 *
 * @version        Enter version here..., Fri, May 21, '10
 * @author         Enter your name here...
 */
public class LasFile extends LidarFile {

    /** default flag for bigendian records */
    private static final boolean BIGENDIAN = false;

    /** user defined crs type */
    private static final int USER_DEFINED = 32767;

    /** projection */
    private static final String PROJ_TRANSVERSEMERCATOR =
        "transverse mercator";



    /** point record format enum */
    public static int POINTFORMAT_0 = 0;

    /** point record format enum */
    public static int POINTFORMAT_1 = 1;

    /** point record format enum */
    public static int POINTFORMAT_2 = 2;

    /** point record format enum */
    public static int POINTFORMAT_3 = 3;

    /** point record format enum */
    public static int POINTFORMAT_4 = 4;

    /** point record format enum */
    public static int POINTFORMAT_5 = 5;

    /** Use this when writing out las */
    public static final String SOFTWARE = "RAMADDA";

    /** geotiff stuff */
    public static final int VAR_GEOKEY_DIRECTORYTAG = 34735;

    /** geotiff stuff */
    public static final int VAR_GEOKEY_DOUBLEPARAMSTAG = 34736;

    /** geotiff stuff */
    public static final int VAR_GEOKEY_ASCIIPARAMSTAG = 34737;

    /** this files version */
    private double version = 1.2;

    /** the point format */
    private int pointFormat = -1;

    /** the header */
    private LasHeader_V12 header;

    /** Keep around the variable length records */
    List<LasVariableLengthRecord> variableLengthRecords =
        new ArrayList<LasVariableLengthRecord>();


    /** geotiff stuff */
    private GeoKeyDirectory geoKeyDirectory;

    /** geotiff stuff */
    private boolean isGeographic = false;

    /** coordinate transform */
    private PointConverter pointConverter;

    /** work array */
    private double[] work = { 0, 0, 0 };

    /**
     * ctor
     */
    public LasFile() {}



    /**
     * ctor
     *
     *
     * @param filename las file to open
     * @throws IOException On badness
     */
    public LasFile(IO.Path path) throws IOException {
        super(path);
    }


    /**
     * get the header  record
     *
     * @return header
     */
    public LasHeader_V12 getHeader() {
        return header;
    }

    /**
     * _more_
     *
     * @param action _more_
     *
     * @return _more_
     */
    public boolean isCapable(String action) {
        if (action.equals(ACTION_TIME)) {
            return ((pointFormat == POINTFORMAT_1)
                    || (pointFormat == POINTFORMAT_4));
        }

        return super.isCapable(action);
    }


    /**
     * _more_
     */
    private void initHeader() {
        if (getFilename() == null) {
            return;
        }
        if (header == null) {
            try {
                RecordIO recordIO = doMakeInputIO(new VisitInfo(),false);
                recordIO = readHeader(recordIO);
                recordIO.close();
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }
        }
    }


    /**
     * _more_
     *
     * @param buff _more_
     *
     * @throws Exception _more_
     */
    public void getInfo(Appendable buff) throws Exception {
        super.getInfo(buff);
        initHeader();
        buff.append("Z Negative:" + zNegative + " Z Positive:" + zPositive);
        buff.append("\n");
        buff.append(" xScaleFactor: " + header.xScaleFactor + " \n");
        buff.append(" yScaleFactor: " + header.yScaleFactor + " \n");
        buff.append(" zScaleFactor: " + header.zScaleFactor + " \n");
        buff.append(" xOffset: " + header.xOffset + " \n");
        buff.append(" yOffset: " + header.yOffset + " \n");
        buff.append(" zOffset: " + header.zOffset + " \n");
        buff.append(" maxX: " + header.maxX + " \n");
        buff.append(" minX: " + header.minX + " \n");
        buff.append(" maxY: " + header.maxY + " \n");
        buff.append(" minY: " + header.minY + " \n");
        buff.append(" maxZ: " + header.maxZ + " \n");
        buff.append(" minZ: " + header.minZ + " \n");


        //      header.print(buff);
    }

    /** _more_ */
    static int cnt = 0;

    /**
     * _more_
     *
     * @param record _more_
     * @param time _more_
     *
     * @return _more_
     */
    public long convertTime(BasePointRecord record, double time) {
        int c = (int) (header.getGlobalEncoding() & 0x01);
        if (cnt == 0) {
            int encoding = header.getGlobalEncoding();
            //            System.err.println("encoding:" +encoding);
        }
        if (cnt++ < 10) {
            //            System.err.println ("c=" + c +" time=" + time);
        }
        //TODO:
        if (c == 0) {
            return 0;
        } else {
            time = time + Math.pow(10, 9);

            return GeoUtils.convertGpsTime((long) time);
        }
    }

    /**
     * set the format
     *
     * @param pointFormat point record format
     */
    public void setPointRecordFormat(int pointFormat) {
        this.pointFormat = pointFormat;
    }

    /**
     * get the point record format
     *
     * @return point record format
     */
    public int getPointRecordFormat() {
        return this.pointFormat;
    }

    /**
     * Is the given file a las file. Checks if it ends with ".las"
     *
     * @param file file
     *
     * @return is las file
     */
    public boolean canLoad(String file) {
        file = file.toLowerCase();

        return file.endsWith(".las") || file.endsWith(".las.zip")
               || file.endsWith("_las.zip");
    }

    /**
     * add a record to the list of records
     *
     * @param record record
     */
    public void addVariableLengthRecord(LasVariableLengthRecord record) {
        variableLengthRecords.add(record);
    }



    /**
     * get the index'th record
     *
     * @param index index
     *
     * @return record
     *
     * @throws Exception On badness
     */
    public LidarRecord getRecord(int index) throws Exception {
        RecordIO recordIO = doMakeInputIO(new VisitInfo(),false);
        recordIO = readHeader(recordIO);
        PointRecord0 record = doMakePointRecord();
        skip(new VisitInfo(recordIO), record, index);
        record.read(recordIO);

        return record;
    }


    /**
     * create the header. Either 1.2 or 1.3
     *
     * @return header
     */
    public LasHeader_V12 createHeader() {
        if (version == 1.2) {
            return new LasHeader_V12(this, BIGENDIAN);
        }
        if (version == 1.3) {
            return new LasHeader_V13(this, BIGENDIAN);
        }

        throw new IllegalArgumentException("Unknown version:" + version);
    }

    /**
     * Create a new record
     *
     * @param fileSourceId file id
     * @param systemId system id
     * @param pointFormat the format
     * @param numberOfPoints number of points
     * @param numberOfReturnsPerPoint returns per point
     *
     * @return new record
     */
    public LasHeader_V12 createHeader(short fileSourceId, String systemId,
                                      int pointFormat, int numberOfPoints,
                                      int numberOfReturnsPerPoint) {
        //        System.err.println ("LasFile.createHeader:" + pointFormat);

        this.pointFormat = pointFormat;
        LasHeader_V12 lasHeader = createHeader();
        lasHeader.setFileSourceId(fileSourceId);
        lasHeader.setSystemIdentifier(systemId);
        lasHeader.setGeneratingSoftware(SOFTWARE);
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTime(new Date());
        lasHeader.setFileCreationDayOfYear(cal.get(Calendar.DAY_OF_YEAR));
        lasHeader.setFileCreationYear(cal.get(Calendar.YEAR));
        lasHeader.setHeaderSize(lasHeader.getRecordSize());
        lasHeader.setPointDataFormatId((byte) pointFormat);
        lasHeader.setNumberOfPointRecords(numberOfPoints);
        long   tmp               = numberOfPoints / numberOfReturnsPerPoint;

        long[] numPointsByReturn = lasHeader.getNumberOfPointsByReturn();
        for (int i = 0;
                (i < numPointsByReturn.length)
                && (i < (numberOfReturnsPerPoint - 1));
                i++) {
            numPointsByReturn[i] = tmp;
        }

        lasHeader.setNumberOfPointsByReturn(numPointsByReturn);

        lasHeader.setNumberOfVariableLengthRecords(
            variableLengthRecords.size());
        int vlrSize = 0;
        for (LasVariableLengthRecord record : variableLengthRecords) {
            vlrSize += record.getRecordSize();
            vlrSize += record.getRecordLengthAfterHeader();
        }
        lasHeader.setOffsetToPointData(lasHeader.getRecordSize() + vlrSize);
        PointRecord0 pointRecord = doMakePointRecord();
        lasHeader.setPointDataRecordLength(pointRecord.getRecordSize());
        //        System.err.println("point record length:" +lasHeader.getPointDataRecordLength());

        return lasHeader;
    }

    /**
     * how many records
     *
     * @return num records
     */
    public long getNumRecords() {
        try {
            long numRecords = super.getNumRecords();
            if (numRecords == 0) {
                initHeader();
                setNumRecords(numRecords = header.getNumberOfPointRecords());
            }

            return numRecords;
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }


    /**
     * write out the variable length records
     *
     * @param recordIO io to write to
     *
     *
     * @return _more_
     * @throws Exception On badness
     */
    public int writeVariableLengthRecords(RecordIO recordIO)
            throws Exception {
        //      System.err.println("Writing vlrs:" + variableLengthRecords.size());
        int bytes = 0;
        for (LasVariableLengthRecord record : variableLengthRecords) {
            bytes += record.getRecordSize() + record.buffer.length;
            record.write(recordIO);
        }

        return bytes;
    }

    /**
     * write the header
     *
     * @param recordIO io to write to
     *
     * @throws Exception On badness
     */
    public void writeHeader(RecordIO recordIO) throws Exception {
        int headerBytesWritten = 0;
        headerBytesWritten = header.getRecordSize();
        header.write(recordIO);
        headerBytesWritten += writeVariableLengthRecords(recordIO);
        //See if we have to pad out the header
        int offset = (int) header.getOffsetToPointData();
        while (headerBytesWritten < offset) {
            byte b = 0;
            recordIO.getDataOutputStream().write(b);
            headerBytesWritten++;
        }
    }

    /** _more_ */
    static boolean printed = false;

    /**
     * _more_
     *
     * @param record _more_
     */
    public void importRecord(PointRecord0 record) {
        LasFile       oldLasFile = (LasFile) record.getRecordFile();
        LasHeader_V12 oldHeader  = oldLasFile.header;
        LasHeader_V12 newHeader  = this.header;

        double        origX      = record.getX();
        double        origY      = record.getY();
        double        origZ      = record.getZ();

        double        origLat    = record.getLatitude();
        double        origLon    = record.getLongitude();
        double        origAlt    = record.getAltitude();

        boolean       changed    = false;
        if ((newHeader.getXOffset() != oldHeader.getXOffset())
                || (newHeader.getXScaleFactor()
                    != oldHeader.getXScaleFactor())) {

            if ( !printed) {
                /*                System.err.println("header : " + oldHeader.getXOffset() + " "
                                   + oldHeader.getXScaleFactor() + " "
                                   + +newHeader.getXOffset() + " "
                                   + newHeader.getXScaleFactor());*/
                printed = true;
            }

            double coord = record.getX();
            coord =
                ((oldHeader.getXOffset()
                  + oldHeader.getXScaleFactor() * coord) - newHeader
                      .getXOffset()) / newHeader.getXScaleFactor();
            record.setX(roundTo(coord));
            changed = true;
        }


        if ((newHeader.getYOffset() != oldHeader.getYOffset())
                || (newHeader.getYScaleFactor()
                    != oldHeader.getYScaleFactor())) {
            double coord = record.getY();
            coord =
                ((oldHeader.getYOffset()
                  + oldHeader.getYScaleFactor() * coord) - newHeader
                      .getYOffset()) / newHeader.getYScaleFactor();
            record.setY(roundTo(coord));
            changed = true;
        }

        if ((newHeader.getZOffset() != oldHeader.getZOffset())
                || (newHeader.getZScaleFactor()
                    != oldHeader.getZScaleFactor())) {
            double coord = record.getZ();
            coord =
                ((oldHeader.getZOffset()
                  + oldHeader.getZScaleFactor() * coord) - newHeader
                      .getZOffset()) / newHeader.getZScaleFactor();
            record.setZ(roundTo(coord));
            changed = true;
        }


        /*
        if (changed && (xcnt++ < 100)) {
            initRecord(record);

            double origUtmX = oldHeader.getXOffset() + oldHeader.getXScaleFactor() * origX;
            double newUtmX = newHeader.getXOffset() + newHeader.getXScaleFactor() * record.getX();
            double origUtmY = oldHeader.getYOffset() + oldHeader.getYScaleFactor() * origY;
            double newUtmY = newHeader.getYOffset() + newHeader.getYScaleFactor() * record.getY();

            System.out.println("orig utm x/y: " + origUtmX + "/"  + origUtmY +" new: " + newUtmX+"/" + newUtmY);
            System.out.println("orig lon/lat: " + origLon + "/" + origLat + " new: " + record.getLongitude() +"/" + record.getLatitude());
        }
        */

    }

    /**
     * _more_
     *
     * @param d _more_
     *
     * @return _more_
     */
    private int roundTo(double d) {
        return (int) (d + 0.5);
    }


    /** _more_ */
    static int xcnt = 0;



    /**
     * read the header
     *
     * @param recordIO io to read from
     *
     * @return Possibly a new io
     *
     *
     * @throws Exception _more_
     */
    @Override
    public RecordIO readHeader(RecordIO recordIO) throws Exception {
        header = new LasHeader_V12(this, BIGENDIAN);
        header.read(recordIO);
        this.pointFormat = header.getPointDataFormatId();


        if (header.getVersionMinor() == 3) {
            version = 1.3;
            //            System.err.println("version 1.3");
            header = new LasHeader_V13(this, BIGENDIAN);
            recordIO.reset(doMakeInputIO(new VisitInfo(),false));
            header.read(recordIO);
        } else if (header.getVersionMinor() != 3) {
            //            throw new IllegalArgumentException("Unknown version:" +header.getVersionMajor()+"." +
            //                                               header.getVersionMinor() +" in: " + getFilename());

        }


        //        System.err.println("offset:" + header.getXOffset()+" scale:" + header.getXScaleFactor());

        pointFormat = header.getPointDataFormatId();
        int bytes = header.getRecordSize();
        /*
        System.err.println("x/y/z: " +header.getMinX() +" " + header.getMaxX() +" " +
                           header.getMinY() +" " + header.getMaxY() +" " +
                           header.getMinZ() +" " + header.getMaxZ());
        */
        recordIO.reset(doMakeInputIO(new VisitInfo(),false));
        recordIO.getDataInputStream().skip(header.getHeaderSize());
        byte[] geoKeyDirectoryBytes = null;
        byte[] geoKeyDoubleParams   = null;
        byte[] geoKeyAsciiParams    = null;

        variableLengthRecords = new ArrayList<LasVariableLengthRecord>();
        for (int varRecordIdx = 0;
                varRecordIdx < header.getNumberOfVariableLengthRecords();
                varRecordIdx++) {
            LasVariableLengthRecord varRecord =
                new LasVariableLengthRecord(this, false);
            variableLengthRecords.add(varRecord);
            varRecord.read(recordIO);
            byte[] varBytes =
                new byte[varRecord.getRecordLengthAfterHeader()];
            recordIO.getDataInputStream().read(varBytes);
            varRecord.setBuffer(varBytes);
            if (varRecord.getUserIdAsString().trim().equals(
                    "LASF_Projection")) {
                varRecord.setBuffer(varBytes);
                //                recordIO.getDataInputStream().skipBytes(varRecord.getRecordLengthAfterHeader());
                if (varRecord.getRecordId() == VAR_GEOKEY_DIRECTORYTAG) {
                    geoKeyDirectoryBytes = varBytes;
                } else if (varRecord.getRecordId()
                           == VAR_GEOKEY_DOUBLEPARAMSTAG) {
                    geoKeyDoubleParams = varBytes;
                } else if (varRecord.getRecordId()
                           == VAR_GEOKEY_ASCIIPARAMSTAG) {
                    geoKeyAsciiParams = varBytes;
                } else {
                    //                    System.err.println ("unknown VLR:");
                    //                    varRecord.print();
                }
                //                System.err.println ("bytes:" + new String(varBytes)+":");
            } else {
                //System.err.println ("\tvar record:" + varRecord.getRecord_id());
                //                recordIO.getDataInputStream().skipBytes(varRecord.getRecordLengthAfterHeader());
            }
            bytes += varRecord.getRecordSize();
            bytes += varRecord.getRecordLengthAfterHeader();
        }

        if (geoKeyDirectoryBytes != null) {
            geoKeyDirectory = new GeoKeyDirectory(geoKeyDirectoryBytes,
                    geoKeyAsciiParams, geoKeyDoubleParams);
            initProjection(geoKeyDirectory);
        } else {
            //            System.err.println("    No geokey found");
        }

        //Reset to beginning and then skip to the start of the point data
        recordIO.reset(doMakeInputIO(new VisitInfo(),false));
        recordIO.getDataInputStream().skipBytes(
            (int) header.getOffsetToPointData());

        /*
        System.err.println("file: " +getFilename() + " version:" + version +" point format:" + pointFormat +
                           "  #vlr: " + header.getNumberOfVariableLengthRecords() +
                           "  #points: " + header.getNumberOfPointRecords()+"  header size:" + header.getHeaderSize() +" header record:" + header.getRecordSize() +" offset:" + header.getOffsetToPointData() +" total bytes:" + bytes);
        */
        return recordIO;
    }



    /**
     * initialize the projection
     *
     * @param dir geotiff key
     */
    private void initProjection(GeoKeyDirectory dir) {
        //        dir.printKeys();
        int modelType = dir.getModelType();
        if (modelType == GeoKeys.ModelType.Projected) {
            //            System.err.println("    model type: projected");
            isGeographic = true;
            initProjectionProjected(dir);
        } else if (modelType == GeoKeys.ModelType.Geographic) {
            //            System.err.println("    model type: geographic");
            isGeographic = true;
            initProjectionGeographic(dir);
        } else if (modelType == GeoKeys.ModelType.UserDefined) {
            //            System.err.println("    model type: user defined");
        } else if (modelType == GeoKeys.ModelType.Geocentric) {
            //            System.err.println("    model type: geocentric");
            pointConverter = new PointConverter() {
                public double[] xyzToLatLonAlt(double x, double y, double z,
                        double[] result) {
                    return GeoUtils.wgs84XYZToLatLonAlt(x, y, z, result);
                }
            };
        } else if (modelType == GeoKeys.ModelType.Undefined) {
            //            System.err.println("    model type: undefined");
        } else {
            //            System.err.println("Unknown model type:" + modelType);
        }
    }

    /**
     * geotiff crs stuff
     *
     * @param dir geotiff crs stuff
     */
    private void initProjectionGeographic(GeoKeyDirectory dir) {
        String projCode =
            dir.getGeoKey(GeneratedKeys.Geokey.GeographicTypeGeoKey,
                          "unnamed");
        if ( !projCode.startsWith("EPSG") && !projCode.startsWith("epsg")) {
            projCode = "EPSG:" + projCode;
        }
        String unit =
            dir.getGeoKey(GeneratedKeys.Geokey.GeogAngularUnitsGeoKey,
                          (String) null);
        //        System.err.println ("    code:" + projCode +" unit:" + unit);
    }



    /**
     * geotiff crs stuff
     *
     * @param dir geotiff crs stuff
     */
    private void initProjectionProjected(GeoKeyDirectory dir) {

        int projectedCSType = dir.getProjectedCSType();
        if (projectedCSType == dir.NOVALUE) {
            //            System.err.println("No CS type given in keys " + GeoKeys.Geokey.ProjectedCSTypeGeoKey);
            return;
        }
        Pcs pcs = Pcs.findCoordRefSysCode(projectedCSType);
        if (pcs == null) {
            if (projectedCSType == USER_DEFINED) {
                //                System.err.println("user defined");
            } else {
                //                System.err.println("Unable to find projected CS type = "
                //                                   + projectedCSType);
                return;
                //                return null;
            }
        }
        //        initParamCodeMap(pcs);
        //        System.err.println ("pcs:" + pcs);

        CoordinateOperationMethod com = pcs.findCoordinateOperationMethod();
        if (com == null) {
            //            System.err.println(
            //                "Unable to find coordinate operation method = "
            //                + pcs.getCoordOpMethodCode());
            return;
            //            return null;
        }
        //        System.err.println("    com:" + com.getCoordOpMethodName());
        if (com.getCoordOpMethodName().toLowerCase().equals(
                PROJ_TRANSVERSEMERCATOR)) {
            //            return createFromTransverseMercator(pcs);
        }


    }


    /**
     * crs stuff
     *
     * @return is geographic crs
     */
    public boolean isGeographic() {
        return isGeographic;
    }




    /**
     * prepare to visit
     *
     * @param visitInfo visit state
     *
     * @return possibly new visit state
     *
     *
     * @throws Exception _more_
     */
    @Override
    public VisitInfo prepareToVisit(VisitInfo visitInfo) throws Exception {
        visitInfo.setRecordIO(readHeader(visitInfo.getRecordIO()));

        return visitInfo;
    }



    /**
     * read the file
     *
     *
     * @throws Exception _more_
     */
    public void read() throws Exception {
        RecordIO recordIO = doMakeInputIO(new VisitInfo(),false);
        recordIO = readHeader(recordIO);
        long pointIdx = 0;
        try {
            PointRecord0 pointRecord = doMakePointRecord();
            if (pointRecord.getRecordSize()
                    != header.getPointDataRecordLength()) {
                System.err.println("Point record size error:"
                                   + pointRecord.getRecordSize() + " "
                                   + header.getPointDataRecordLength());
            }
            long numPts = header.getNumberOfPointRecords();
            for (pointIdx = 0; pointIdx < numPts; pointIdx++) {
                pointRecord.read(recordIO);
                //Sanity check

                if ((pointRecord.getScanAngleRank() > 180)
                        || (pointRecord.getScanAngleRank() < -180)) {
                    System.err.println("bad angle:"
                                       + pointRecord.getScanAngleRank() + " "
                                       + pointIdx);

                    break;
                }
            }
        } catch (Exception exc) {
            System.err.println("Error:" + exc + " read:" + pointIdx);
            exc.printStackTrace();
        }
        //        pointRecord.print();
        recordIO.close();
    }


    /** _more_ */
    static int zcnt = 0;

    /** _more_ */
    int zNegative = 0;

    /** _more_ */
    int zPositive = 0;

    /**
     * init the record
     *
     * @param record record
     */
    public void initRecord(PointRecord0 record) {
        if (header == null) {
            throw new IllegalStateException(
                "The LAS header has not been initialized");
        }
        double x         = record.getX();
        double y         = record.getY();
        double z         = record.getZ();
        double originalZ = z;

        x = header.getXOffset() + header.getXScaleFactor() * x;
        y = header.getYOffset() + header.getYScaleFactor() * y;
        z = header.getZOffset() + header.getZScaleFactor() * z;

        if (z < 0) {
            zNegative++;
            //      if(zcnt++<10) {
            //          System.out.println("read z:" + originalZ + "  scaled z:" + z);
            //          }
        } else {
            zPositive++;
        }


        record.setScaledAndOffsetX(x);
        record.setScaledAndOffsetY(y);
        record.setScaledAndOffsetZ(z);
        record.setLocation(x, y, z);

        //If its not wgs84 then the record.setLocation will just set the altitude to z
        if ( !isCRS3D()) {
            //      record.setAltitude(z);          
        }
    }


    /**
     * factory  method
     *
     * @return new record
     */
    public PointRecord0 doMakePointRecord() {
        initHeader();
        //        System.err.println("LasFile.doMakePointRecord:  format=" + pointFormat);
        if (pointFormat == POINTFORMAT_0) {
            return new PointRecord0(this, false);
        }
        if (pointFormat == POINTFORMAT_1) {
            return new PointRecord1(this, false);
        }
        if (pointFormat == POINTFORMAT_2) {
            return new PointRecord2(this, false);
        }
        if (pointFormat == POINTFORMAT_3) {
            return new PointRecord3(this, false);
        }
        if (pointFormat == POINTFORMAT_4) {
            return new PointRecord4(this, false);
        }
        if (pointFormat == POINTFORMAT_5) {
            return new PointRecord5(this, false);
        }

        throw new IllegalStateException("Unknown point format:"
                                        + pointFormat);

    }

    /**
     * factory method
     *
     *
     * @param visitInfo read state
     * @return new record
     */
    public BaseRecord doMakeRecord(VisitInfo visitInfo) {
        return doMakePointRecord();
    }



    /**
     * The intent is that we can encapsulate the coordinate transform to lat/lon/alt space
     * with one of these
     *
     *
     */
    public static interface PointConverter {

        /**
         * method to convert the lidar files x/y/z to lat/lon/alt
         *
         * @param x x
         * @param y y
         * @param z z
         * @param result use this if non null. Save a new object
         *
         * @return lon/lat/alt
         */
        public double[] xyzToLatLonAlt(double x, double y, double z,
                                       double[] result);
    }




    /**
     * main
     *
     * @param args args
     *
     * @throws Exception On badness
     */
    public static void main(String[] args) throws Exception {

        //        Record h1 = new LasHeader_V12((RecordFile)null);
        //        Record h2 = new LasHeader_V12((RecordFile)null);
        //        System.err.println("header v12:" + h1.runRandomTest(h2));

        for (String arg : args) {
            try {
                long    t1      = System.currentTimeMillis();
                LasFile lasFile = new LasFile(new IO.Path(arg));
                System.err.println("reading:" + arg);
                lasFile.read();
                StringBuffer buff = new StringBuffer();
                lasFile.getHeader().print(buff);
                System.err.println(buff);
                long t2 = System.currentTimeMillis();
                //                System.err.println("time:" + (t2 - t1) / 1000.0);
            } catch (Exception exc) {
                System.err.println("Error:" + exc);
                exc.printStackTrace();
            }
        }
    }




}
