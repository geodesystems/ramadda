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

package org.ramadda.data.tools;


import org.ramadda.data.point.*;

import org.ramadda.data.record.*;
import org.ramadda.data.tools.*;
import org.ramadda.util.grid.LatLonGrid;
import org.ramadda.util.grid.ObjectGrid;

import ucar.unidata.util.IOUtil;

import java.io.*;

import java.util.ArrayList;
import java.util.List;


/**
 * This class supports taking a set of input data files (all of the same type) and writing their points into
 * a set of output files based on a spatial tiling
 *
 */
public class PointTiler extends RecordTool {


    /** _more_ */
    //    private JobStatus jobStatus;

    /** _more_ */
    private boolean calcBounds = false;

    /** bounds of the output area */
    private double north = 90,
                   south = -90,
                   west  = -180,
                   east  = 180;

    /** _more_ */
    private int size = 0;

    /** How many grid cells east/west */
    private int width = 12;

    /** How many grid cells north/south */
    private int height = width / 2;

    /** _more_ */
    private int skip = 0;

    /** Where to write output to */
    private String destinationDir = ".";

    /** Input files */
    private List<File> inputFiles = new ArrayList<File>();

    /** output filename prefix */
    private String prefix = "point";

    /** output filename suffix */
    private String suffix = null;

    /** Tracks the output files */
    private List<PointFileInfo> fileInfos = new ArrayList<PointFileInfo>();

    /** _more_ */
    private int pointCnt = 0;

    /**
     * ctor
     *
     * @param args command line arguments
     *
     * @throws Exception On badness
     */
    public PointTiler(String[] args) throws Exception {
        super(null);
        parseArgs(args);
        doTile(inputFiles);
    }


    /**
     * _more_
     *
     * @param north _more_
     * @param west _more_
     * @param south _more_
     * @param east _more_
     * @param nCols _more_
     * @param nRows _more_
     * @param destDir _more_
     *
     * @throws Exception _more_
     */
    public PointTiler(double north, double west, double south, double east,
                      int nCols, int nRows, File destDir)
            throws Exception {
        super(null);
        setBounds(north, west, south, east);
        setSize(nCols, nRows);
        this.destinationDir = destDir.toString();
    }


    /**
     * _more_
     *
     * @return _more_
     */
    private boolean isCancelled() {
        /*
        if (jobStatus != null) {
            return jobStatus.isCancelled();
        }
        */
        return false;
    }

    /**
     * _more_
     *
     * @param message _more_
     *
     */
    private void updateStatus(String message) {
        /*        if (jobStatus != null) {
            jobStatus.updateStatus(message);
            } else {*/
        System.err.println(message);
        //        }
    }



    /**
     * _more_
     *
     * @param nCols _more_
     * @param nRows _more_
     */
    public void setSize(int nCols, int nRows) {
        this.width  = nCols;
        this.height = nRows;
    }

    /**
     * _more_
     *
     * @param north _more_
     * @param west _more_
     * @param south _more_
     * @param east _more_
     */
    public void setBounds(double north, double west, double south,
                          double east) {
        this.north = north;
        this.west  = west;
        this.south = south;
        this.east  = east;
    }

    /**
     * Print out the usage message and exit
     *
     * @param message error message
     */
    public void usage(String message) {
        if ((message != null) && (message.length() > 0)) {
            System.err.println("Error:" + message);
        }

        System.err.println(
            "usage: PointTiler\n\t-class <class path of point file reader (see README)>\n\t-bounds <north> <west> <south> <east>\n\t-calcbounds (use the bounds of the input files)\n\t-size <size of major axis> (used with -calcbounds minor access size is calculated)\n\t-width <number of grid columns> -height <number of grid rows>\n\t-skip <decimation factor>\n\t-destination <destination directory>\n\t-suffix <file suffix - note: if .csv then we write out a csv file>\n\t-prefix <file prefix>\n\t<input files>");

        System.err.println(
            "e.g., -bounds 90 -180 -90 180 -width 12 -height 6 -destination tiles -suffix .dat -prefix TILED <input files>");
        System.exit(1);
    }


    /**
     * Do the actual tiling
     *
     * @param jobStatus _more_
     * @param inputFiles _more_
     *
     * @return List of tiled files
     * @throws Exception On badness
     */
    public List<File> doTile(List<File> inputFiles) throws Exception {

        //        this.jobStatus = jobStatus;

        if (calcBounds) {
            PointMetadataHarvester boundsHarvester =
                new PointMetadataHarvester();
            for (File f : inputFiles) {
                updateStatus("calculating bounds:" + f);
                VisitInfo visitInfo = new VisitInfo();
                if (skip > 0) {
                    visitInfo.setSkip(skip);
                }
                RecordFile recordFile = doMakeRecordFile(f.toString());
                recordFile.visit(boundsHarvester, visitInfo, null);
            }
            north = boundsHarvester.getMaxLatitude();
            west  = boundsHarvester.getMinLongitude();
            south = boundsHarvester.getMinLatitude();
            east  = boundsHarvester.getMaxLongitude();
            updateStatus("using bounds:" + north + " " + west + " " + south
                         + " " + east);
            if (size != 0) {
                double heightDeg = north - south;
                double widthDeg  = east - west;
                if (heightDeg > widthDeg) {
                    double aspect = widthDeg / heightDeg;
                    height = size;
                    width  = Math.max(1, (int) (size * aspect + 0.5));
                } else {
                    double aspect = heightDeg / widthDeg;
                    width  = size;
                    height = Math.max(1, (int) (size * aspect + 0.5));
                }
                System.err.println("width:" + width + " height:" + height
                                   + " deg/col:" + (widthDeg / width)
                                   + " deg/row:" + (heightDeg / height));
            }
        }




        //Make the lat/lon grid of FileInfo objects
        final ObjectGrid<PointFileInfo> outputGrid =
            new ObjectGrid<PointFileInfo>(width, height, north, west, south,
                           east);

        final LatLonGrid countGrid = new LatLonGrid(width, height, north,
                                         west, south, east);
        countGrid.fillValue(1.0);


        //If no suffix specified then use the file suffix of the first file
        if (suffix == null) {
            suffix = IOUtil.getFileExtension(inputFiles.get(0).toString());
        }

        final int[] badcnt = { 0 };
        //Make the visitor
        RecordVisitor visitor = new RecordVisitor() {
            public boolean visitRecord(RecordFile file, VisitInfo visitInfo,
                                       Record record) {
                PointRecord pointRecord = (PointRecord) record;
                if ( !pointRecord.isValidPosition()) {
                    badcnt[0]++;

                    //                    System.err.println("\t bad " +  badcnt[0]);
                    return true;
                }
                double        lat      = pointRecord.getLatitude();
                double        lon      = pointRecord.getLongitude();
                PointFileInfo fileInfo = outputGrid.getValue(lat, lon);
                int           yIndex   = outputGrid.getLatitudeIndex(lat);
                int           xIndex   = outputGrid.getLongitudeIndex(lon);

                countGrid.addValueToIndex(yIndex, xIndex, 1);
                pointCnt++;
                if (pointCnt % 1000000 == 0) {
                    updateStatus("\t" + pointCnt + " points");
                }
                try {
                    if (fileInfo == null) {
                        String destFile = destinationDir + "/" + prefix + "_"
                                          + yIndex + "_" + xIndex + suffix;
                        //                        if(!(yIndex==3 && xIndex==3)) return true;
                        updateStatus("\tnew file:" + destFile);
                        if (suffix.endsWith("csv")) {
                            fileInfo = new CsvFileInfo(destFile,
                                    (PointFile) file);
                        } else {
                            fileInfo = new PointFileInfo(destFile,
                                    (PointFile) file);
                        }
                        fileInfos.add(fileInfo);
                        outputGrid.setValue(lat, lon, fileInfo);
                    }
                    fileInfo.writeRecord(pointRecord, lat, lon);
                } catch (Exception exc) {
                    throw new RuntimeException(exc);
                }

                return true;
            }
        };


        //Process the files
        for (File f : inputFiles) {
            updateStatus("processing:" + f);
            VisitInfo visitInfo = new VisitInfo();
            if (skip > 0) {
                visitInfo.setSkip(skip);
            }

            RecordFile file = doMakeRecordFile(f.toString());
            file.visit(visitor, visitInfo, null);
            if (isCancelled()) {
                break;
            }
        }

        //Finish up
        List<File> tiledFiles = new ArrayList<File>();
        for (PointFileInfo fileInfo : fileInfos) {
            if (fileInfo.getOutputFile().exists()) {
                tiledFiles.add(fileInfo.getOutputFile());
            }
            fileInfo.close();
        }


        int[][] count = countGrid.getCountGrid();
        System.out.println("Counts:");
        System.out.println("<table border=1 cellspacing=0 cellpadding=0>");
        System.out.println("<tr valign=top>");
        System.out.println("<td>Lat/Lon</td>");
        for (int col = 0; col < count[0].length; col++) {
            System.out.println("<td>");
            System.out.println((outputGrid.getWest()
                                + outputGrid.getCellWidth() * col));
            System.out.println("</td>");
        }
        System.out.println("</tr>");

        for (int row = 0; row < count.length; row++) {
            System.out.println("<tr valign=top>");

            System.out.println("<td>");
            System.out.println((outputGrid.getNorth()
                                - outputGrid.getCellHeight() * row));
            System.out.println("</td>");

            for (int col = 0; col < count[row].length; col++) {
                System.out.println("<td>");

                PointFileInfo fileInfo = outputGrid.getValueFromIndex(row,
                                             col);
                if (fileInfo == null) {
                    System.out.println("&nbsp;");
                } else {
                    double[] ranges = fileInfo.getRanges();
                    int      cnt    = count[row][col];
                    System.out.println(fileInfo.getPointCount());
                    /*
                    if(ranges!=null) {
                        System.out.println(fileInfo.getOutputFile().getName());
                        System.out.println("<br>");
                        System.out.println(ranges[0] +"<br>" + ranges[1]);
                        System.out.println("<br>");
                        System.out.println(ranges[3] +"<br>"  +ranges[2]);
                    }
                    */
                }
                System.out.println("</td>");
            }
            System.out.println("");
        }
        System.out.println("</table>");


        return tiledFiles;

    }


    /**
     * parse cmd line arguments
     *
     * @param args cmd line args
     *
     * @param argArray _more_
     *
     * @throws Exception On badness
     */
    private void parseArgs(String[] argArray) throws Exception {
        List<String> argList = processArgs(argArray);
        System.err.println("ARGS:" + argList);
        for (int i = 0; i < argList.size(); i++) {
            String arg = argList.get(i);
            if (arg.equals("-help")) {
                usage("");
            }
            if (arg.equals("-bounds")) {
                if (i + 4 >= argList.size()) {
                    usage("Need " + arg + " argument");
                }
                north = Double.parseDouble(argList.get(++i));
                west  = Double.parseDouble(argList.get(++i));
                south = Double.parseDouble(argList.get(++i));
                east  = Double.parseDouble(argList.get(++i));

                continue;
            } else if (arg.equals("-calcbounds")) {
                calcBounds = true;

                continue;
            } else if (arg.equals("-skip")) {
                if (i + 1 >= argList.size()) {
                    usage("Need " + arg + " argument");
                }
                skip = Integer.parseInt(argList.get(++i));
            } else if (arg.equals("-width")) {
                if (i + 1 >= argList.size()) {
                    usage("Need " + arg + " argument");
                }
                width = Integer.parseInt(argList.get(++i));
            } else if (arg.equals("-size")) {
                if (i + 1 >= argList.size()) {
                    usage("Need " + arg + " argument");
                }
                size = Integer.parseInt(argList.get(++i));
            } else if (arg.equals("-height")) {
                if (i + 1 >= argList.size()) {
                    usage("Need " + arg + " argument");
                }
                height = Integer.parseInt(argList.get(++i));
            } else if (arg.startsWith("-dest")) {
                if (i + 1 >= argList.size()) {
                    usage("Need " + arg + " argument");
                }
                destinationDir = argList.get(++i);
                if ( !new File(destinationDir).exists()) {
                    usage("Destination directory does not exist:"
                          + destinationDir);
                }

            } else if (arg.startsWith("-suffix")) {
                if (i + 1 >= argList.size()) {
                    usage("Need " + arg + " argument");
                }
                suffix = argList.get(++i);
            } else if (arg.startsWith("-prefix")) {
                if (i + 1 >= argList.size()) {
                    usage("Need " + arg + " argument");
                }
                prefix = argList.get(++i);
            } else if (arg.startsWith("-")) {
                usage("Unknown argument:" + arg);
            } else {
                File f = new File(arg);
                if ( !f.exists()) {
                    usage("file does not exist:" + f);
                }
                inputFiles.add(f);
            }
        }

        if (inputFiles.size() == 0) {
            usage("No input files given");
        }
    }




    /**
     * main
     *
     * @param args args
     *
     * @throws Exception On badness
     */
    public static void main(String[] args) throws Exception {
        new PointTiler(args);
        //        test();
    }

    /**
     * _more_
     */
    public static void test() {
        int    width  = 12;
        int    height = 30;
        double north  = 90;
        double south  = -90;
        double west   = -180;
        double east   = 180;
        final ObjectGrid<Double> outputGrid = new ObjectGrid<Double>(width,
                                                  height, north, west, south,
                                                  east);

        final LatLonGrid countGrid = new LatLonGrid(width, height, north,
                                         west, south, east);
        countGrid.fillValue(1.0);
        double lon = 0;
        double lat = 0;

        int    cnt = 0;
        for (lon = -180; lon <= 180; lon += 2) {
            int xIndex = outputGrid.getLongitudeIndex(lon);
            System.err.println("Lon:" + lon + " index:" + xIndex);
            //            if(cnt++>50) break;
        }

        for (lat = -90; lat <= 90; lat += 2) {
            int yIndex = outputGrid.getLatitudeIndex(lat);
            System.err.println("Lat:" + lat + " index:" + yIndex);
        }
    }

}
