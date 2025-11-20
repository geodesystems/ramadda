/**
Copyright (c) 2008-2026 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
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
