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

package org.ramadda.data.tools;


import org.ramadda.data.point.*;


import org.ramadda.data.record.*;
import org.ramadda.util.grid.LatLonGrid;
import org.ramadda.util.grid.ObjectGrid;

import ucar.unidata.util.IOUtil;

import java.io.*;

import java.util.ArrayList;
import java.util.List;


/**
 * Class description
 *
 *
 * @version        $version$, Wed, Feb 15, '12
 * @author         Enter your name here...
 */
public class CsvFileInfo extends PointFileInfo {

    /** _more_ */
    boolean wroteHeader = false;

    /** _more_ */
    VisitInfo visitInfo;

    /**
     * _more_
     *
     * @param outputFile _more_
     * @param pointFile _more_
     *
     * @throws Exception _more_
     */
    public CsvFileInfo(String outputFile, PointFile pointFile)
            throws Exception {
        super(outputFile, pointFile);
        visitInfo = new VisitInfo();
    }

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public void writeHeader() throws Exception {}

    /**
     * _more_
     *
     * @param record _more_
     *
     * @throws Exception _more_
     */
    public void writeRecord(Record record) throws Exception {
        if ( !wroteHeader) {
            record.printCsvHeader(visitInfo, recordOutput.getPrintWriter());
            wroteHeader = true;
        }
        record.printCsv(visitInfo, recordOutput.getPrintWriter());
    }
}
