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
    public boolean isRecordOk(Record record, VisitInfo visitInfo) {
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
