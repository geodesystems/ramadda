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
 */
public class RandomizedFilter implements RecordFilter {

    /** _more_ */
    private double probability = 0.5;

    /**
     *
     * @param probability _more_
     */
    public RandomizedFilter(double probability) {
        this.probability = probability;
    }


    /**
     * _more_
     *
     * @param record _more_
     * @param visitInfo _more_
     *
     * @return _more_
     */
    public boolean isRecordOk(BaseRecord record, VisitInfo visitInfo) {
        return (Math.random() < probability);
    }



}
