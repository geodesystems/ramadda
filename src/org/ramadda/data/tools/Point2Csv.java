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
import org.ramadda.data.record.filter.*;
import org.ramadda.data.tools.*;

import ucar.unidata.util.Misc;

import java.io.*;

import java.util.ArrayList;

import java.util.List;


/**
 * Class description
 *
 *
 * @version        Enter version here..., Fri, May 21, '10
 * @author         Enter your name here...
 */
public class Point2Csv extends RecordTool {

    /**
     * _more_
     *
     * @param factoryClass _more_
     * @param args _more_
     * @param argArray _more_
     *
     * @throws Exception _more_
     */
    public Point2Csv(String factoryClass, String[] argArray)
            throws Exception {

        super(factoryClass);
        double             north     = 0,
                           south     = 0,
                           west      = 0,
                           east      = 0;

        String             inFile    = null;
        String             outFile   = null;
        String             prefix    = null;
        final boolean[]    doHeader  = { false };
        final boolean[]    latLonAlt = { false };
        final boolean[]    lonLatAlt = { false };
        List<RecordFilter> filters   = new ArrayList<RecordFilter>();
        VisitInfo          visitInfo = new VisitInfo();

        List<String>       args      = processArgs(argArray);
        int                numArgs   = args.size();
        for (int i = 0; i < args.size(); i++) {
            String arg = args.get(i);
            if (arg.equals("-out")) {
                if (i == numArgs - 1) {
                    usage("Need " + arg + " argument");
                }
                outFile = args.get(++i);

                continue;
            }
            if (arg.equals("-class")) {
                if (i == numArgs - 1) {
                    usage("Need " + arg + " argument");
                }
                setRecordFileClass(args.get(++i));

                continue;
            }
            if (arg.equals("-bounds")) {
                if (i + 4 >= numArgs) {
                    usage("Need " + arg + " argument");
                }
                filters.add(
                    new LatLonBoundsFilter(
                        Double.parseDouble(args.get(++i)),
                        Double.parseDouble(args.get(++i)),
                        Double.parseDouble(args.get(++i)),
                        Double.parseDouble(args.get(++i))));

                continue;
            }
            if (arg.equals("-skip")) {
                if (i == numArgs - 1) {
                    usage("Need " + arg + " argument");
                }
                visitInfo.setSkip(Integer.parseInt(args.get(++i)));

                continue;
            }
            if (arg.equals("-start")) {
                if (i == numArgs - 1) {
                    usage("Need " + arg + " argument");
                }
                visitInfo.setStart(Integer.parseInt(args.get(++i)));

                continue;
            }
            if (arg.equals("-max")) {
                if (i == numArgs - 1) {
                    usage("Need " + arg + " argument");
                }
                visitInfo.setMax(Integer.parseInt(args.get(++i)));

                continue;
            }
            if (arg.equals("-prefix")) {
                if (i == numArgs - 1) {
                    usage("Need " + arg + " argument");
                }
                prefix = args.get(++i);

                continue;
            }
            if (arg.equals("-randomized")) {
                if (i == numArgs - 1) {
                    usage("Need " + arg + " argument");
                }
                filters.add(
                    new RandomizedFilter(Double.parseDouble(args.get(++i))));

                continue;
            }

            if (arg.equals("-header")) {
                doHeader[0] = true;

                continue;
            }
            if (arg.equals("-latlonalt")) {
                latLonAlt[0] = true;

                continue;
            }
            if (arg.equals("-lonlatalt")) {
                lonLatAlt[0] = true;

                continue;
            }

            if (arg.startsWith("-")) {
                usage("Unknown argument:" + arg);
            }
            inFile = arg;
        }

        if (inFile == null) {
            usage("Need to specify an input file");
        }
        OutputStream os;
        if (outFile != null) {
            os = new FileOutputStream(outFile);
        } else {
            os = System.out;
        }
        final PrintWriter outputWriter = new PrintWriter(os);
        if (prefix != null) {
            outputWriter.println(prefix);
        }

        if (getRecordFileClass() == null) {
            if (inFile.endsWith(".txt")) {
                setRecordFileClass("org.ramadda.data.point.text.CsvFile");
            }
        }


        RecordFile file = doMakeRecordFile(inFile);
        /*
        if (suffix.endsWith("csv")) {
            fileInfo = new CsvFileInfo(destFile,
                                       (PointFile) file);
        } else {
            fileInfo = new PointFileInfo(destFile,
                                         (PointFile) file);
        }
        //Make the visitor
        RecordVisitor visitor = new RecordVisitor() {
                public boolean visitRecord(RecordFile file, VisitInfo visitInfo,
                                       Record record) {
                                       PointRecord   pointRecord = (PointRecord) record;
                double        lat         = pointRecord.getLatitude();
                double        lon         = pointRecord.getLongitude();
                pointCnt++;
                if (pointCnt % 1000000 == 0) {
                    updateStatus("\t" + pointCnt + " points");
                }
                try {
                    fileInfo.writeRecord(pointRecord, lat, lon);
                } catch (Exception exc) {
                    throw new RuntimeException(exc);
                }
                return true;
            }
        };
        fileInfo.close();
        */
        final int[]         cnt      = { 0 };
        final boolean       fullData = !latLonAlt[0] && !lonLatAlt[0];

        final RecordVisitor visitor  = new RecordVisitor() {
            public boolean visitRecord(RecordFile file, VisitInfo visitInfo,
                                       Record record) {
                PointRecord pointRecord = (PointRecord) record;
                if ((pointRecord.getLatitude() < -90)
                        || (pointRecord.getLatitude() > 90)) {
                    outputWriter.println("*** Bad latitude:"
                                         + pointRecord.getLatitude());
                    //pointRecord.print();
                }
                pointRecord.convertXYZToLatLonAlt();

                //                    pointRecord.print();
                //                    if(cnt[0]++>4)return false;
                //                    if(true) return true;

                if ((cnt[0] == 0) && doHeader[0]) {
                    if (fullData) {
                        pointRecord.printCsvHeader(visitInfo, outputWriter);
                    } else {
                        if (latLonAlt[0]) {
                            outputWriter.println(
                                "LATITUDE,LONGITUDE,ALTITUDE");
                        } else {
                            outputWriter.println(
                                "LONGITUDE,LATITUDE,ALTITUDE");
                        }
                    }
                }
                cnt[0]++;
                if ((cnt[0] % 100000) == 0) {
                    System.err.println("#records: " + cnt[0]);
                } else if ((cnt[0] % 10000) == 0) {
                    //                        System.err.print(".");
                }

                if (fullData) {
                    pointRecord.printCsv(visitInfo, outputWriter);
                } else {
                    if (latLonAlt[0]) {
                        pointRecord.printLatLonAltCsv(visitInfo,
                                outputWriter);
                    } else {
                        pointRecord.printLonLatAltCsv(visitInfo,
                                outputWriter);
                    }
                }

                return true;
            }
        };

        RecordFilter filter = null;
        if (filters.size() == 1) {
            filter = filters.get(0);
        } else if (filters.size() > 1) {
            filter = new CollectionRecordFilter(filters);
        }
        file.visit(visitor, visitInfo, filter);
        outputWriter.close();

    }

    /**
     * _more_
     *
     * @param message _more_
     */
    public void usage(String message) {
        System.err.println("Error:" + message);
        System.err.println(
            "usage: point2csv\n\t-class <class path of point file reader (see README)>\n\t-latlonalt (output lat/lon/alt)\n\t-lonlatalt (output lon/lat/alt)\n\t-header (include the header)\n\t-prefix [arbitrary text to include in the output, e.g., \"X,Y,Z\"]\n\t-randomized [probablity between 0.0 - 1.0]\n\t-skip [integer skip factor, e.g. -skip 1 skips every other point]\n\t-bounds <north> <west> <south> <east>\n\t-out <output file>\n\t<input_file>");
        System.exit(1);
    }


    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        //TODO: get the record factory class from the args
        new Point2Csv(null, args);
    }

}
