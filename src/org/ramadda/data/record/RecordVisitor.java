/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.data.record;

import java.util.ArrayList;
import java.util.List;
import java.io.*;


/**
 * Class description
 *
 *
 * @version        Enter version here..., Fri, May 21, '10
 * @author         Enter your name here...
 */
public abstract class RecordVisitor {
    public RecordVisitor() {
    }

    /**
     * _more_
     *
     * @param file _more_
     * @param visitInfo _more_
     * @param record _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public abstract boolean visitRecord(RecordFile file, VisitInfo visitInfo,
                                        BaseRecord record)
     throws Exception;

    /**
     * This gets called when the visitor is done visiting the given record file
     *
     * @param file file we just visited
     * @param visitInfo visit info
     *
     * @throws Exception _more_
     */
    public void finished(RecordFile file, VisitInfo visitInfo)
            throws Exception {}

    /**
     * _more_
     *
     * @param visitInfo _more_
     */
    public void close(VisitInfo visitInfo) {}


}
