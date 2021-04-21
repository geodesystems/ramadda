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

package org.ramadda.geodata.point.icebridge;


import org.ramadda.data.record.*;

import java.io.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;





/** This is generated code from generate.tcl. Do not edit it! */
public class QfitRecord extends org.ramadda.data.point.PointRecord {

    /** _more_ */
    long baseDate = 0;

    /** _more_ */
    int relativeTime;

    /** _more_ */
    int laserLatitude;

    /** _more_ */
    int laserLongitude;

    /** _more_ */
    int elevation;


    /**
     * _more_
     *
     * @param that _more_
     */
    public QfitRecord(QfitRecord that) {
        super(that);
        this.relativeTime   = that.relativeTime;
        this.laserLatitude  = that.laserLatitude;
        this.laserLongitude = that.laserLongitude;
        this.elevation      = that.elevation;
    }

    /**
     * _more_
     *
     * @param file _more_
     */
    public QfitRecord(RecordFile file) {
        super(file);
    }

    /**
     * _more_
     *
     * @param file _more_
     * @param bigEndian _more_
     */
    public QfitRecord(RecordFile file, boolean bigEndian) {
        super(file, bigEndian);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public long getRecordTime() {
        if (baseDate == 0L) {
            return super.getRecordTime();
        }

        return baseDate + relativeTime;
    }

    /**
     * _more_
     *
     * @param l _more_
     */
    public void setBaseDate(long l) {
        baseDate = l;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public double getLatitude() {
        return laserLatitude / 1000000.0;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public double getLongitude() {
        return org.ramadda.util.GeoUtils.normalizeLongitude(laserLongitude
                / 1000000.0);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public double getAltitude() {
        return elevation / 1000.0;
    }



}
