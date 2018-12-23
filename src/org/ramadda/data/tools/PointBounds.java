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
import org.ramadda.util.grid.LatLonGrid;

import org.w3c.dom.*;

import ucar.unidata.data.gis.KmlUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.xml.XmlUtil;


import java.io.*;

import java.util.Date;
import java.util.List;


/**
 * Class description
 *
 *
 * @version        Enter version here..., Fri, May 21, '10
 * @author         Enter your name here...
 */
public class PointBounds extends RecordTool {

    /** _more_ */
    private static boolean debug = false;

    /** _more_ */
    private int skip = 0;

    /** _more_ */
    private int max = 0;

    /** _more_ */
    private int width = 40;

    /** _more_ */
    private int height = 40;

    /** _more_ */
    private String kmlFile = null;

    /** _more_ */
    private String name = null;

    /** _more_ */
    private boolean doPolygon = false;


    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public PointBounds(String[] args) throws Exception {
        super(null);
        process(args);
    }


    /**
     * _more_
     *
     * @param factoryClass _more_
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public PointBounds(String factoryClass, String[] args) throws Exception {
        super(factoryClass);
        process(args);
    }


    /**
     * _more_
     *
     * @param message _more_
     */
    public void usage(String message) {
        if ((message != null) && (message.length() > 0)) {
            System.err.println("Error:" + message);
        }
        System.err.println(
            "usage: PointBounds\n\t-class <class path of point file reader (see README)>\n\t[-polygon] [-kml <kmlfile>] <one or more data files>");
        System.exit(1);
    }


    /**
     * _more_
     *
     * @param line _more_
     */
    private static void debug(String line) {
        if (debug) {
            System.err.println(line);
        }
    }


    /**
     * _more_
     *
     * @param args _more_
     *
     * @param argArray _more_
     *
     * @throws Exception _more_
     */
    public void process(String[] argArray) throws Exception {

        if (argArray.length == 0) {
            usage("No input files given");
        }

        List<String> args = processArgs(argArray);
        for (int i = 0; i < args.size(); i++) {
            String arg = args.get(i);
            if (arg.equals("-help")) {
                usage("");
            }
            if (arg.equals("-polygon")) {
                doPolygon = true;

                continue;
            }
            if (arg.equals("-kml")) {
                kmlFile = args.get(i + 1);
                i++;

                continue;
            }
            if (arg.equals("-name")) {
                name = args.get(i + 1);
                i++;

                continue;
            }
            if (arg.equals("-width")) {
                i++;
                width = Integer.parseInt(args.get(i));

                continue;
            }
            if (arg.equals("-debug")) {
                debug = true;

                continue;
            }

            if (arg.equals("-skip")) {
                i++;
                skip = Integer.parseInt(args.get(i));

                continue;
            }
            if (arg.equals("-max")) {
                i++;
                max = Integer.parseInt(args.get(i));

                continue;
            }
            if (arg.equals("-height")) {
                i++;
                height = Integer.parseInt(args.get(i));

                continue;
            }




            if ( !new File(arg).exists()) {
                usage("File:" + arg + " does not exist");
            }
        }

        int    total = 0;
        double north = -90,
               west  = 180,
               south = 90,
               east  = -180;

        for (int i = 0; i < args.size(); i++) {
            String arg = args.get(i);
            if (arg.equals("-polygon")) {
                continue;
            }
            if (arg.equals("-kml") || arg.equals("-width")
                    || arg.equals("-skip") || arg.equals("-max")
                    || arg.equals("-height") || arg.equals("-name")) {
                i++;

                continue;
            }

            if (arg.startsWith("-")) {
                continue;
            }
            if (name == null) {
                name = IOUtil.stripExtension(IOUtil.getFileTail(arg));
            }
            PointFile pointFile = (PointFile) doMakeRecordFile(arg);
            PointMetadataHarvester metadata  = new PointMetadataHarvester();
            long                   t1        = System.currentTimeMillis();
            VisitInfo              visitInfo = new VisitInfo();
            if (skip > 0) {
                visitInfo.setSkip(skip);
            }
            if (max > 0) {
                visitInfo.setMax(max);
            }
            pointFile.visit(metadata, visitInfo, null);

            long t2 = System.currentTimeMillis();
            if (debug) {
                System.err.println("time:" + (t2 - t1) + "ms");
            }
            if (metadata.hasTimeRange()) {
                //                System.err.println("time:" + new Date(metadata.getMinTime()) + " -- " + new Date(metadata.getMaxTime()));
            }

            if (doPolygon) {

                LatLonGrid llg = new LatLonGrid(width, height,
                                     metadata.getMaxLatitude(),
                                     metadata.getMinLongitude(),
                                     metadata.getMinLatitude(),
                                     metadata.getMaxLongitude());
                PointMetadataHarvester metadata2 =
                    new PointMetadataHarvester(llg);
                pointFile.visit(metadata2, new VisitInfo(true), null);
                List<double[]> polygon = llg.getBoundingPolygon();
                if (kmlFile != null) {
                    float[][] coords = new float[2][polygon.size()];
                    for (int ptIdx = 0; ptIdx < polygon.size(); ptIdx++) {
                        double[] point = polygon.get(ptIdx);
                        coords[1][ptIdx] = (float) point[1];
                        coords[0][ptIdx] = (float) point[0];
                    }
                    Element root = KmlUtil.kml(name);
                    Element doc  = KmlUtil.document(root, name);
                    KmlUtil.placemark(doc, name, name, coords,
                                      java.awt.Color.red, 1);

                    IOUtil.writeFile(kmlFile, XmlUtil.toString(root));
                }
                int cnt = 0;
                //                System.out.println("name,latitude,longitude");
                for (double[] point : polygon) {
                    System.out.println(point[0] + "," + point[1]);
                    cnt++;
                }
            }

            total += metadata.getCount();
            north = Math.max(north, metadata.getMaxLatitude());
            west  = Math.min(west, metadata.getMinLongitude());
            south = Math.min(south, metadata.getMinLatitude());
            east  = Math.max(east, metadata.getMaxLongitude());
            System.out.println("File:" + arg + " "
                               + metadata.getMaxLatitude() + " "
                               + metadata.getMinLongitude() + " "
                               + metadata.getMinLatitude() + " "
                               + metadata.getMaxLongitude());
        }

        if ( !doPolygon) {
            System.out.println(" " + north + " " + west + " " + south + " "
                               + east);
            //            System.err.println ("total #points:" +  total);
        }


    }

    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        new PointBounds(args);
    }


}
