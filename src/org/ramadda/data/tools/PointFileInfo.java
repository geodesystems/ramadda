/*
* Copyright (c) 2008-2018 Geode Systems LLC
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
import org.ramadda.util.grid.LatLonGrid;
import org.ramadda.util.grid.ObjectGrid;

import ucar.unidata.util.IOUtil;

import java.io.*;

import java.util.ArrayList;
import java.util.List;



/**
 *
 */
public class PointFileInfo {

    /** output filename */
    private File outputFile;

    /** The point file */
    private PointFile pointFile;

    /** what we write to */
    RecordIO recordOutput;

    /** track number of points */
    private int pointCnt = 0;

    /** _more_ */
    private double[] ranges;


    /**
     * ctor
     *
     * @param outputFile output file
     * @param pointFile point file
     *
     * @throws Exception On badness
     */
    public PointFileInfo(String outputFile, PointFile pointFile)
            throws Exception {
        this.outputFile = new File(outputFile);
        this.recordOutput = new RecordIO(
            new BufferedOutputStream(
                new FileOutputStream(outputFile), 100000));
        this.pointFile = pointFile;
        writeHeader();
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public File getOutputFile() {
        return outputFile;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public double[] getRanges() {
        return ranges;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getPointCount() {
        return pointCnt;
    }

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public void writeHeader() throws Exception {
        pointFile.writeHeader(recordOutput);
    }

    /**
     * Write the record
     *
     * @param record record to write
     * @param lat _more_
     * @param lon _more_
     *
     * @throws Exception On badness
     */
    public void writeRecord(PointRecord record, double lat, double lon)
            throws Exception {
        if (ranges == null) {
            ranges = new double[] { lat, lat, lon, lon };
        }
        ranges[0] = Math.max(ranges[0], lat);
        ranges[1] = Math.min(ranges[1], lat);
        ranges[2] = Math.max(ranges[2], lon);
        ranges[3] = Math.min(ranges[3], lon);
        record.recontextualize(pointFile);
        record.write(recordOutput);
        pointCnt++;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public double getMaxLatitude() {
        return ranges[0];
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public double getMinLatitude() {
        return ranges[1];
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public double getMaxLongitude() {
        return ranges[2];
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public double getMinLongitude() {
        return ranges[3];
    }


    /**
     * finish up
     */
    public void close() {
        if (recordOutput != null) {
            recordOutput.close();
        }
    }
}
