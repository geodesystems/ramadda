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

package org.ramadda.data.point.binary;


import org.ramadda.data.point.*;
import org.ramadda.data.point.*;


import org.ramadda.data.record.*;

import ucar.unidata.util.Misc;

import ucar.unidata.util.StringUtil;

import java.awt.*;
import java.awt.image.*;

import java.io.*;

import java.util.List;

import javax.swing.*;


/**
 * Class description
 *
 *
 * @version        Enter version here..., Fri, May 21, '10
 * @author         Enter your name here...
 */
public class DoubleLatLonBinaryFile extends PointFile {

    /**
     * _more_
     */
    public DoubleLatLonBinaryFile() {}



    /**
     * ctor
     *
     *
     *
     *
     * @param filename _more_
     *
     * @throws IOException _more_
     */
    public DoubleLatLonBinaryFile(String filename) throws IOException {
        super(filename);
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public long getNumRecords() {
        try {
            long numRecords = super.getNumRecords();
            if (numRecords == 0) {
                setNumRecords(new File(getFilename()).length()
                              / doMakeRecord(null).getRecordSize());
            }

            return super.getNumRecords();
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }




    /**
     * _more_
     *
     * @param filename _more_
     *
     * @return _more_
     */
    public boolean canLoad(String filename) {
        filename = filename.toLowerCase();
        for (String pattern : new String[] { ".llab", ".dllab", ".lla",
                                             ".llai" }) {
            if (filename.endsWith(pattern)) {
                return true;
            }
        }

        return false;
    }


    /**
     * _more_
     *
     *
     * @param visitInfo _more_
     * @return _more_
     */
    public Record doMakeRecord(VisitInfo visitInfo) {
        String filename = getFilename().toLowerCase();
        if (filename.endsWith(".llai")) {
            return new DoubleLatLonAltIntensityRecord(this);
        }

        return new DoubleLatLonRecord(this);
    }

    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        int skip = 0;
        /*
          skip this for now

        for (int argIdx = 0; argIdx < args.length; argIdx++) {
            String arg = args[argIdx];
            if (arg.equals("-skip")) {
                argIdx++;
                skip = new Integer(args[argIdx]).intValue();
                continue;
            }
            PointFile pointFile = (PointFile) new LidarFileFactory().doMakeRecordFile(arg);
            String    toFile    = arg;
            int       idx       = toFile.lastIndexOf(".");
            if (idx >= 0) {
                toFile = toFile.substring(0, idx);
            }

            toFile = toFile + ".llab";
            if (toFile.equals(arg)) {
                toFile = toFile.replace(".llab", "_2.llab");
            }
            System.err.println("writing to:" + toFile);
            VisitInfo visitInfo = new VisitInfo(true, skip);
            writeBinaryFile(pointFile, new FileOutputStream(toFile),
                            visitInfo);

        }
        */
    }

    /**
     * _more_
     *
     * @param pointFile file to read from
     * @param fos _more_
     * @param visitInfo _more_
     *
     * @throws Exception On badness
     */
    public static void writeBinaryFile(PointFile pointFile, OutputStream fos,
                                       VisitInfo visitInfo)
            throws Exception {
        final DataOutputStream dos =
            new DataOutputStream(new BufferedOutputStream(fos, 10000));
        RecordVisitor visitor = new RecordVisitor() {
            public boolean visitRecord(RecordFile file, VisitInfo visitInfo,
                                       Record record) {
                try {
                    GeoRecord geoRecord = (GeoRecord) record;
                    dos.writeDouble(geoRecord.getLatitude());
                    dos.writeDouble(geoRecord.getLongitude());

                    return true;
                } catch (Exception exc) {
                    throw new RuntimeException(exc);
                }
            }
        };
        //This doesn't need to be in the thread pool since its called by the PointEntry to make
        //the short form of the file
        pointFile.visit(visitor, visitInfo, null);
        dos.close();
    }






}
