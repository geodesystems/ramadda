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
