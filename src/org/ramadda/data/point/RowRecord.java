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

package org.ramadda.data.point;


import org.ramadda.data.record.*;

import org.ramadda.util.Station;
import org.ramadda.util.Utils;
import org.ramadda.util.text.*;

import java.io.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;



/**
 * Class description
 *
 *
 * @version        $version$, Mon, Feb 17, '20
 * @author         Enter your name here...
 */
public abstract class RowRecord extends DataRecord {

    /** _more_ */
    public static final int ATTR_FIRST =
        org.ramadda.data.point.PointRecord.ATTR_LAST;


    /**
     * _more_
     */
    public RowRecord() {}


    /**
     * _more_
     *
     * @param that _more_
     */
    public RowRecord(RowRecord that) {
        super(that);
    }


    /**
     * _more_
     *
     * @param file _more_
     * @param fields _more_
     */
    public RowRecord(RecordFile file, List<RecordField> fields) {
        super(file, fields);
        if (fields != null) {
            initFields(fields);
        }
    }


    /**
     * _more_
     *
     * @param file _more_
     */
    public RowRecord(RecordFile file) {
        super(file);
    }

    /**
     * _more_
     *
     * @param recordIO _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public ReadStatus read(RecordIO recordIO) throws Exception {
        Row row = readNextRow(recordIO);
        if (row == null) {
            return ReadStatus.EOF;
        }
        //      System.err.println("ROW:" + row.size());
        for (int colIdx = 0; colIdx < row.size(); colIdx++) {
            Object      o     = row.get(colIdx);
            RecordField field = fields.get(colIdx);
            //      System.err.println("\tf:" + field.getName() +" field type:" + field.getType() +" v:" + o);
            if (field.isTypeString()) {
                objectValues[colIdx] = o;

                continue;
            }
            if (field.isTypeDate()) {
                objectValues[colIdx] = o;

                continue;
            }
            values[colIdx] = ((Double) o).doubleValue();
        }

        return ReadStatus.OK;
    }

    /**
     * _more_
     *
     * @param recordIO _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public abstract Row readNextRow(RecordIO recordIO) throws Exception;


}
