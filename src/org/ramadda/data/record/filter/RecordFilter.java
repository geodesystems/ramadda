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
public interface RecordFilter {

    /** _more_ */
    public static final AltitudeFilter dummy1 = null;

    /** _more_ */
    public static final BitmaskRecordFilter dummy2 = null;

    /** _more_ */
    public static final CollectionRecordFilter dummy3 = null;

    /** _more_ */
    public static final LatLonBoundsFilter dummy4 = null;

    /** _more_ */
    public static final NumericRecordFilter dummy5 = null;

    /** _more_ */
    public static final RandomizedFilter dummy6 = null;


    /**
     * _more_
     *
     * @param record _more_
     * @param visitInfo _more_
     *
     * @return _more_
     */
    public boolean isRecordOk(BaseRecord record, VisitInfo visitInfo);

}
