/**
Copyright (c) 2008-2026 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.data.record;


import java.io.*;


/**
 * Class description
 *
 *
 * @version        Enter version here..., Fri, May 21, '10
 * @author         Enter your name here...
 */
public class RecordCountVisitor extends RecordVisitor {

    /** _more_ */
    private int cnt = 0;

    /**
     * _more_
     */
    public RecordCountVisitor() {}

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
                               BaseRecord record) {
        cnt++;

        return true;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getCount() {
        return cnt;
    }

}
