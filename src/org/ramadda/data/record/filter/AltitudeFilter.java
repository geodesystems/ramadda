/**
Copyright (c) 2008-2026 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.data.record.filter;


import org.ramadda.data.record.*;

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
public class AltitudeFilter implements RecordFilter {

    /** _more_ */
    public double minHeight;

    /** _more_ */
    public double maxHeight;


    /**
     * _more_
     *
     * @param north _more_
     * @param south _more_
     * @param east _more_
     * @param west _more_
     *
     * @param minHeight _more_
     * @param maxHeight _more_
     */
    public AltitudeFilter(double minHeight, double maxHeight) {
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;

    }



    /**
     * _more_
     *
     * @param record _more_
     * @param visitInfo _more_
     *
     * @return _more_
     */
    public boolean isRecordOk(BaseRecord record, VisitInfo visitInfo) {
        GeoRecord geoRecord = (GeoRecord) record;
        double    altitude  = geoRecord.getAltitude();
        if ( !Double.isNaN(minHeight) && (altitude < minHeight)) {
            return false;
        }
        if ( !Double.isNaN(maxHeight) && (altitude > maxHeight)) {
            return false;
        }

        return true;

    }

}
