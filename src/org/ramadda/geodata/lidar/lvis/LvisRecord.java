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

package org.ramadda.geodata.lidar.lvis;


import org.ramadda.data.record.*;


import org.ramadda.geodata.lidar.*;
import org.ramadda.util.Utils;

import ucar.unidata.util.StringUtil;

import java.io.*;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.TimeZone;



/**
 * Class description
 *
 *
 * @version        Enter version here..., Fri, May 21, '10
 * @author         Enter your name here...
 */
public abstract class LvisRecord extends LidarRecord {


    /** _more_ */
    public static final String WAVEFORM_TRANSMIT = "Transmit waveform";

    /** _more_ */
    public static final String WAVEFORM_RETURN = "Return waveform";


    /** _more_ */
    double lvisTime;

    /**
     * _more_
     *
     * @param recordFile _more_
     */
    public LvisRecord(RecordFile recordFile) {
        super(recordFile);
    }

    /**
     * _more_
     *
     * @param that _more_
     */
    public LvisRecord(LvisRecord that) {
        super(that);
        this.lvisTime = that.lvisTime;
    }

    /**
     * _more_
     *
     *
     * @param recordFile _more_
     * @param bigEndian _more_
     */
    public LvisRecord(RecordFile recordFile, boolean bigEndian) {
        super(recordFile, bigEndian);
    }

    /** _more_          */
    private int[] julianWorkArray = { 0, 0, 0 };

    /** _more_          */
    private boolean timeOK = true;

    /** _more_          */
    private SimpleDateFormat sdf;


    /** _more_          */
    private int lastYear  = -1,
                lastMonth = -1,
                lastDay   = -1;

    /** _more_          */
    private long lastDate;

    /** _more_          */
    private int lastModifiedJulian = -1;

    /**
     * _more_
     *
     * @param recordIO _more_
     *
     * @throws IOException _more_
     */
    @Override
    public void initializeAfterRead(RecordIO recordIO) throws IOException {
        super.initializeAfterRead(recordIO);
        //lfid - 1655277009
        //Where the first 2 numbers (16 in this case) give us the instrument version
        //the next 5 numbers give us Modified Julian Date (55277 or mar 22 2010 in this example)
        //and last 3 numbers give a file number/id.
        int modifiedJulian = getLfid() / 1000;
        modifiedJulian = modifiedJulian % 100000;

        if (lastModifiedJulian != modifiedJulian) {
            Utils.fromJulian(Utils.modifiedJulianToJulian(modifiedJulian),
                             julianWorkArray);
            lastModifiedJulian = modifiedJulian;
        }

        try {
            if ((lastYear != julianWorkArray[0])
                    || (lastMonth != julianWorkArray[1])
                    || (lastDay != julianWorkArray[2])) {
                //This is expensive so cache the results
                //                System.err.println("getting base date:" + julianWorkArray[0] +" -- " + julianWorkArray[1] + " -- " + julianWorkArray[2]);
                if (sdf == null) {
                    sdf = new SimpleDateFormat("yyyy-MM-dd");
                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                }
                lastDate = sdf.parse(julianWorkArray[0] + "-"
                                     + julianWorkArray[1] + "-"
                                     + julianWorkArray[2]).getTime();
                lastYear  = julianWorkArray[0];
                lastMonth = julianWorkArray[1];
                lastDay   = julianWorkArray[2];
            }
            setRecordTime(lastDate + ((long) (getLvisTime() * 1000)));
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }

    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getLfid() {
        return 0;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public double getLvisTime() {
        return lvisTime;
    }


    /**
     * _more_
     *
     * @param newValue _more_
     */
    public void setLvisTime(double newValue) {
        lvisTime = newValue;
    }

}
