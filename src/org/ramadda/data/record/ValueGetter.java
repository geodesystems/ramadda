/**
Copyright (c) 2008-2026 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.data.record;


import java.io.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;


/**
 * Holds information about the record's parameters
 *
 *
 * @author Jeff McWhirter
 */
public interface ValueGetter {

    /**
     * _more_
     *
     * @param record _more_
     * @param field _more_
     * @param visitInfo _more_
     *
     * @return _more_
     */
    public double getValue(BaseRecord record, RecordField field,
                           VisitInfo visitInfo);

    /**
     * _more_
     *
     * @param record _more_
     * @param field _more_
     * @param visitInfo _more_
     *
     * @return _more_
     */
    public String getStringValue(BaseRecord record, RecordField field,
                                 VisitInfo visitInfo);
}
