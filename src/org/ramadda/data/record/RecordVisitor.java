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


import java.io.*;


/**
 * Class description
 *
 *
 * @version        Enter version here..., Fri, May 21, '10
 * @author         Enter your name here...
 */
public abstract class RecordVisitor {

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
