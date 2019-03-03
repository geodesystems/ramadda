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


import org.ramadda.data.point.PointFile;
import org.ramadda.data.point.PointRecord;


import org.ramadda.data.record.*;
import org.ramadda.data.record.filter.*;

import org.ramadda.geodata.lidar.*;

import ucar.unidata.util.IOUtil;

import java.io.*;

import java.util.ArrayList;

import java.util.List;


/**
 * Convert one or more  LidarFiles to Las
 *
 *
 */
public class LasConverter {

    /** scale factor */
    private static final double SCALE_LATLON = 0.00001;

    /** scale factor */
    private static final double SCALE_ALTITUDE = 0.00001;

    /** The source lidar files */
    private List<PointFile> sourceFiles = new ArrayList<PointFile>();

    /** The destination LAS file */
    private LasFile lasFile;

    /** point record */
    private PointRecord0 lasPointRecord;

    /**
     * ctor
     *
     * @param sourceFile A single source file
     * @throws Exception On badness
     */
    public LasConverter(PointFile sourceFile) throws Exception {
        this.sourceFiles.add(sourceFile);
        lasFile = new LasFile();
    }


    /**
     * ctor with multiple source files
     *
     * @param sourceFiles source lidar files
     *
     * @throws Exception On badness
     */
    public LasConverter(List<? extends PointFile> sourceFiles)
            throws Exception {
        this.sourceFiles.addAll(sourceFiles);
        lasFile = new LasFile();
    }


    /**
     * Do the conversion. The tmpFile is where we can write the las body
     *
     *
     * @param tmpFile tmp file. After the convert we do a tmpFile.delete
     * @param output where we write to
     * @param filter filter to apply to  records (e.g., bounding box filter)
     *
     * @return _more_
     * @throws Exception On badness
     */
    public int doConvert(File tmpFile, final RecordIO output,
                         RecordFilter filter)
            throws Exception {

        final RecordIO tmpIO = new RecordIO(
                                   new BufferedOutputStream(
                                       new FileOutputStream(tmpFile),
                                       100000));

        final int[]       rgbIndices = { -1, -1, -1 };
        List<RecordField> fields     = sourceFiles.get(0).getFields();
        int               tmpIndex   = -1;
        for (int i = 0; i < fields.size(); i++) {
            RecordField field = fields.get(i);
            String      name  = field.getName().toLowerCase();
            if (name.equals("red")) {
                rgbIndices[0] = i;
            } else if (name.equals("green")) {
                rgbIndices[1] = i;
            } else if (name.equals("blue")) {
                rgbIndices[2] = i;
            } else if (name.equals("intensity")) {
                tmpIndex = i;
            }
        }

        final short[] theRgb       = { 0, 0, 0 };
        final int     intensityIdx = tmpIndex;
        final boolean haveRGB = ((rgbIndices[0] != -1)
                                 && (rgbIndices[1] != -1)
                                 && (rgbIndices[2] != -1));



        final int[]    cnt     = { 0 };
        final double[] xRange  = { 0, 0 };
        final double[] yRange  = { 0, 0 };
        final double[] zRange  = { 0, 0 };
        final Object   mutex   = new Object();
        RecordVisitor  visitor = new RecordVisitor() {

            public boolean visitRecord(RecordFile file, VisitInfo visitInfo,
                                       Record record) {
                try {
                    PointRecord pointRecord = (PointRecord) record;
                    long        time        = pointRecord.getRecordTime();
                    boolean     hasTime     = pointRecord.hasRecordTime();
                    short[]     rgb         = null;
                    if (haveRGB) {
                        rgb    = theRgb;
                        rgb[0] = (short) record.getValue(rgbIndices[0]);
                        rgb[1] = (short) record.getValue(rgbIndices[1]);
                        rgb[2] = (short) record.getValue(rgbIndices[2]);

                    }


                    if (cnt[0] == 0) {
                        //Set the point format
                        int pointFormat = LasFile.POINTFORMAT_0;
                        if (rgb != null) {
                            if (hasTime) {
                                pointFormat = LasFile.POINTFORMAT_3;
                            } else {
                                pointFormat = LasFile.POINTFORMAT_2;
                            }
                        } else if (hasTime) {
                            pointFormat = LasFile.POINTFORMAT_1;
                        }

                        lasFile.setPointRecordFormat(pointFormat);
                        lasPointRecord = lasFile.doMakePointRecord();
                    }


                    //If there is a valid time in the from record then set it
                    if (hasTime) {
                        //Some point records might not have time and just won't have this implemented
                        lasPointRecord.setRecordTime(time);
                    }

                    if (rgb != null) {
                        if (lasPointRecord instanceof PointRecord2) {
                            ((PointRecord2) lasPointRecord).setRgb(rgb);
                        } else if (lasPointRecord instanceof PointRecord3) {
                            ((PointRecord3) lasPointRecord).setRgb(rgb);
                        }
                    }

                    if (intensityIdx >= 0) {
                        lasPointRecord.setIntensity(
                            (int) record.getValue(intensityIdx));
                    }



                    double lon = pointRecord.getLongitude();
                    double lat = pointRecord.getLatitude();
                    double alt = pointRecord.getAltitude();
                    if (cnt[0] == 0) {
                        xRange[0] = xRange[1] = lon;
                        yRange[0] = yRange[1] = lat;
                        zRange[0] = zRange[1] = alt;
                    } else {
                        xRange[0] = Math.min(xRange[0], lon);
                        xRange[1] = Math.max(xRange[1], lon);
                        yRange[0] = Math.min(yRange[0], lat);
                        yRange[1] = Math.max(yRange[1], lat);
                        zRange[0] = Math.min(zRange[0], alt);
                        zRange[1] = Math.max(zRange[1], alt);
                    }
                    int x = (int) (lon / SCALE_LATLON);
                    int y = (int) (lat / SCALE_LATLON);
                    int z = (int) (alt / SCALE_ALTITUDE);
                    lasPointRecord.setX(x);
                    lasPointRecord.setY(y);
                    lasPointRecord.setZ(z);


                    lasPointRecord.setReturnNumber((byte) 1);
                    lasPointRecord.setNumberOfReturns((byte) 1);
                    lasPointRecord.write(tmpIO);
                } catch (Exception exc) {
                    throw new RuntimeException(exc);
                }
                cnt[0]++;

                return true;
            }
        };

        //TODO: put these in threads to run sequentially
        for (RecordFile sourceFile : sourceFiles) {
            sourceFile.visit(visitor, filter);
        }

        tmpIO.close();

        //Create and initialize the header

        LasHeader_V12 lasHeader = lasFile.createHeader((short) 5432,
                                      "NLAS Conversion",
                                      lasFile.getPointRecordFormat(), cnt[0],
                                      1);


        lasHeader.setXScaleFactor(SCALE_LATLON);
        lasHeader.setYScaleFactor(SCALE_LATLON);
        lasHeader.setZScaleFactor(SCALE_ALTITUDE);

        lasHeader.setMinX(xRange[0]);
        lasHeader.setMaxX(xRange[1]);
        lasHeader.setMinY(yRange[0]);
        lasHeader.setMaxY(yRange[1]);
        lasHeader.setMinZ(zRange[0]);
        lasHeader.setMaxZ(zRange[1]);

        //Now actually write the header
        lasHeader.write(output);
        lasFile.writeVariableLengthRecords(output);

        //Now we write the tmp file
        InputStream tmpInputStream =
            new BufferedInputStream(new FileInputStream(tmpFile));
        IOUtil.writeTo(tmpInputStream, output.getOutputStream());


        //cleanup
        IOUtil.close(tmpInputStream);
        tmpFile.delete();

        output.close();

        return cnt[0];
        //        System.err.println("Wrote " + cnt[0] + " records");

    }


}
