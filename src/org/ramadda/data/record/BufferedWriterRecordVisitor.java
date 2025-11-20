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
public abstract class BufferedWriterRecordVisitor {

    /** _more_ */
    protected StringBuffer buffer = new StringBuffer();

    /** _more_ */
    protected PrintWriter pw;

    /**
     * _more_
     *
     * @param pw _more_
     */
    public BufferedWriterRecordVisitor(PrintWriter pw) {}


    /**
     * _more_
     *
     * @param file _more_
     * @param visitInfo _more_
     */
    public void finished(RecordFile file, VisitInfo visitInfo) {
        if (buffer.length() > 0) {
            synchronized (pw) {
                pw.append(buffer);
            }
        }
    }



}
