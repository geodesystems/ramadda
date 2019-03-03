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
public abstract class LasPointRecord extends LidarRecord {


    /** _more_          */
    double scaledAndOffsetX;

    /** _more_          */
    double scaledAndOffsetY;

    /** _more_          */
    double scaledAndOffsetZ;




    /**
     * _more_
     *
     * @param that _more_
     */
    public LasPointRecord(LasPointRecord that) {
        super(that);


    }



    /**
     * _more_
     *
     * @param file _more_
     */
    public LasPointRecord(RecordFile file) {
        super(file);
    }



    /**
     * _more_
     *
     * @param file _more_
     * @param bigEndian _more_
     */
    public LasPointRecord(RecordFile file, boolean bigEndian) {
        super(file, bigEndian);
    }



    /**
     * Set the ScaledAndOffsetX property.
     *
     * @param value The new value for ScaledAndOffsetX
     */
    public void setScaledAndOffsetX(double value) {
        scaledAndOffsetX = value;
    }

    /**
     * Get the ScaledAndOffsetX property.
     *
     * @return The ScaledAndOffsetX
     */
    public double getScaledAndOffsetX() {
        return scaledAndOffsetX;
    }

    /**
     * Set the ScaledAndOffsetY property.
     *
     * @param value The new value for ScaledAndOffsetY
     */
    public void setScaledAndOffsetY(double value) {
        scaledAndOffsetY = value;
    }

    /**
     * Get the ScaledAndOffsetY property.
     *
     * @return The ScaledAndOffsetY
     */
    public double getScaledAndOffsetY() {
        return scaledAndOffsetY;
    }

    /**
     * Set the ScaledAndOffsetZ property.
     *
     * @param value The new value for ScaledAndOffsetZ
     */
    public void setScaledAndOffsetZ(double value) {
        scaledAndOffsetZ = value;
    }

    /**
     * Get the ScaledAndOffsetZ property.
     *
     * @return The ScaledAndOffsetZ
     */
    public double getScaledAndOffsetZ() {
        return scaledAndOffsetZ;
    }




}
