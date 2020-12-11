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

package org.ramadda.data.record;


import org.ramadda.data.record.*;

import java.io.*;



/**
 * Class description
 *
 *
 * @version        Enter version here..., Fri, May 21, '10
 * @author         Enter your name here...
 */
public abstract class GeoRecord extends BaseRecord {


    /**
     * _more_
     */
    public GeoRecord() {}


    /**
     * _more_
     *
     * @param recordFile _more_
     */
    public GeoRecord(RecordFile recordFile) {
        super(recordFile);
    }

    /**
     * _more_
     *
     * @param that _more_
     */
    public GeoRecord(GeoRecord that) {
        super(that);
    }

    /**
     * _more_
     *
     *
     * @param recordFile _more_
     * @param bigEndian _more_
     */
    public GeoRecord(RecordFile recordFile, boolean bigEndian) {
        super(recordFile, bigEndian);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public abstract double getLatitude();

    /**
     * _more_
     *
     * @return _more_
     */
    public abstract double getLongitude();

    /**
     * _more_
     *
     * @return _more_
     */
    public abstract double getAltitude();

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isValidPosition() {
        if ((getLatitude() <= 90) && (getLatitude() >= -90)
                && (getLongitude() >= -180) && (getLongitude() <= 360)) {
            return true;
        }

        return false;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isValidAltitude() {
        return getAltitude() == getAltitude();
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean needsValidPosition() {
        return true;
    }



}
