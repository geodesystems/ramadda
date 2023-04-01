/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.data.tools;


import org.ramadda.data.point.*;


import org.ramadda.data.record.*;
import org.ramadda.data.record.filter.*;
import org.ramadda.data.services.*;
import org.ramadda.data.tools.*;

import ucar.unidata.util.IOUtil;
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
public class Point2Netcdf extends RecordTool {

    /**
     * _more_
     *
     * @param factoryClass _more_
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public Point2Netcdf(String factoryClass, String[] args) throws Exception {

        super(factoryClass);
        double             north     = 0,
                           south     = 0,
                           west      = 0,
                           east      = 0;

        List<String>       inFiles   = new ArrayList<String>();
        String             outFile   = null;
        String             prefix    = null;
        final boolean[]    doHeader  = { false };
        final boolean[]    latLonAlt = { false };
        final boolean[]    lonLatAlt = { false };
        List<RecordFilter> filters   = new ArrayList<RecordFilter>();
        VisitInfo          visitInfo = new VisitInfo();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("-out")) {
                if (i == args.length - 1) {
                    usage("Need " + arg + " argument");
                }
                outFile = args[++i];

                continue;
            }
            if (arg.equals("-class")) {
                if (i == args.length - 1) {
                    usage("Need " + arg + " argument");
                }
                setRecordFileClass(args[++i]);

                continue;
            }
            if (arg.equals("-bounds")) {
                if (i + 4 >= args.length) {
                    usage("Need " + arg + " argument");
                }
                filters.add(
                    new LatLonBoundsFilter(
                        Double.parseDouble(args[++i]),
                        Double.parseDouble(args[++i]),
                        Double.parseDouble(args[++i]),
                        Double.parseDouble(args[++i])));

                continue;
            }
            if (arg.equals("-skip")) {
                if (i == args.length - 1) {
                    usage("Need " + arg + " argument");
                }
                visitInfo.setSkip(Integer.parseInt(args[++i]));

                continue;
            }
            if (arg.equals("-start")) {
                if (i == args.length - 1) {
                    usage("Need " + arg + " argument");
                }
                visitInfo.setStart(Integer.parseInt(args[++i]));

                continue;
            }
            if (arg.equals("-max")) {
                if (i == args.length - 1) {
                    usage("Need " + arg + " argument");
                }
                visitInfo.setMax(Integer.parseInt(args[++i]));

                continue;
            }
            if (arg.equals("-prefix")) {
                if (i == args.length - 1) {
                    usage("Need " + arg + " argument");
                }
                prefix = args[++i];

                continue;
            }
            if (arg.equals("-randomized")) {
                if (i == args.length - 1) {
                    usage("Need " + arg + " argument");
                }
                filters.add(
                    new RandomizedFilter(Double.parseDouble(args[++i])));

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
            inFiles.add(arg);
        }

        if (inFiles.size() == 0) {
            usage("Need to specify an input file");
        }
        for (String inFile : inFiles) {
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

            File tmpFile = new File(IOUtil.stripExtension(inFile)
                                    + ".nc.tmp");
            File destFile = new File(IOUtil.stripExtension(inFile) + ".nc");
            System.err.println("writing:" + destFile);
            RecordVisitor visitor = new NetcdfVisitor(tmpFile,  destFile, null);
            RecordFilter  filter  = null;
            if (filters.size() == 1) {
                filter = filters.get(0);
            } else if (filters.size() > 1) {
                filter = new CollectionRecordFilter(filters);
            }
            file.visit(visitor, visitInfo, filter);
            visitor.close(visitInfo);
            outputWriter.close();
            tmpFile.delete();
        }

    }

    /**
     * _more_
     *
     * @param message _more_
     */
    public void usage(String message) {
        System.err.println("Error:" + message);
        System.err.println(
            "usage: point2csv\n\t-latlonalt (output lat/lon/alt)\n\t-lonlatalt (output lon/lat/alt)\n\t-header (include the header)\n\t-prefix [arbitrary text to include in the output, e.g., \"X,Y,Z\"]\n\t-randomized [probablity between 0.0 - 1.0]\n\t-skip [integer skip factor, e.g. -skip 1 skips every other point]\n\t-bounds <north> <west> <south> <east>\n\t-out <output file>\n\t<input_file>");
        org.ramadda.util.Utils.exitTest(1);
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
        new Point2Netcdf(null, args);
    }

}
