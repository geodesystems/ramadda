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

import java.io.*;


/**
 * Class description
 *
 *
 * @version        Enter version here..., Fri, May 21, '10
 * @author         Enter your name here...
 */
public class LvisMetadataHarvester extends RecordVisitor {

    /** _more_ */
    private double minLatitude = Double.NaN;

    /** _more_ */
    private double maxLatitude = Double.NaN;

    /** _more_ */
    private double minLongitude = Double.NaN;

    /** _more_ */
    private double maxLongitude = Double.NaN;

    /**
     * _more_
     */
    public LvisMetadataHarvester() {}

    /**
     * _more_
     *
     * @param file _more_
     * @param visitInfo _more_
     * @param record _more_
     *
     * @return _more_
     */
    public boolean visitRecord(RecordFile file, VisitInfo visitInfo,
                               Record record) {
        LidarRecord lvisRecord = (LidarRecord) record;
        if ((minLatitude != minLatitude)
                || (minLatitude > lvisRecord.getLatitude())) {
            minLatitude = lvisRecord.getLatitude();
        }
        if ((maxLatitude != maxLatitude)
                || (maxLatitude < lvisRecord.getLatitude())) {
            maxLatitude = lvisRecord.getLatitude();
        }

        if ((minLongitude != minLongitude)
                || (minLongitude > lvisRecord.getLongitude())) {
            minLongitude = lvisRecord.getLongitude();
        }
        if ((maxLongitude != maxLongitude)
                || (maxLongitude < lvisRecord.getLongitude())) {
            maxLongitude = lvisRecord.getLongitude();
        }

        return true;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return "latitude:" + minLatitude + " - " + maxLatitude
               + "  longitude:" + minLongitude + " - " + maxLongitude;
    }




    /**
     * Get the MinLatitude property.
     *
     * @return The MinLatitude
     */
    public double getMinLatitude() {
        return this.minLatitude;
    }



    /**
     * Get the MaxLatitude property.
     *
     * @return The MaxLatitude
     */
    public double getMaxLatitude() {
        return this.maxLatitude;
    }


    /**
     * Get the MinLongitude property.
     *
     * @return The MinLongitude
     */
    public double getMinLongitude() {
        return this.minLongitude;
    }


    /**
     * Get the MaxLongitude property.
     *
     * @return The MaxLongitude
     */
    public double getMaxLongitude() {
        return this.maxLongitude;
    }




}
