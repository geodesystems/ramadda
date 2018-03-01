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
public class BitmaskRecordFilter implements RecordFilter {

    /** _more_ */
    private int mask;

    /** _more_ */
    private boolean value;

    /** _more_ */
    private int attrId;

    /**
     * _more_
     *
     * @param operator _more_
     *
     * @param bitNumber _more_
     * @param attrId _more_
     * @param value _more_
     */
    public BitmaskRecordFilter(int bitNumber, boolean value, int attrId) {
        mask        = 1 << bitNumber;
        this.value  = value;
        this.attrId = attrId;
        System.err.println("BitmaskRecordFilter: bitNumber:" + bitNumber
                           + " " + value + " attrId:" + attrId + " mask= "
                           + mask);
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
        int     v        = (int) record.getValue(attrId);
        boolean bitSet   = (v & mask) != 0;
        boolean recordOk = false;
        if (bitSet) {
            recordOk = value;
        } else {
            recordOk = !value;
        }

        //        if(pcnt++<50) {
        //            System.err.println ("BitmaskRecordFilter.isRecordOk: bit set: " + bitSet +" " + value +" value=" + v +" record:" + record.getValue(attrId-1) +"  OK:" + recordOk);
        //        }
        return recordOk;
    }

    /** _more_ */
    int pcnt = 0;

}
