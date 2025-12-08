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
 * Class description
 *
 *
 * @version        Enter version here..., Fri, May 21, '10
 * @author         Enter your name here...
 */
public class NumericRecordFilter implements RecordFilter {

    /** _more_ */
    public static final int OP_LT = 0;

    /** _more_ */
    public static final int OP_LE = 1;

    /** _more_ */
    public static final int OP_GT = 2;

    /** _more_ */
    public static final int OP_GE = 3;

    /** _more_ */
    public static final int OP_EQUALS = 4;

    /** _more_ */
    public static final int ATTR_FIRST = 0;

    /** _more_ */
    public static final int ATTR_LAST = 0;


    /** _more_ */
    private double value;

    /** _more_ */
    private int operator;

    /** _more_ */
    private int attrId;

    /**
     * _more_
     *
     * @param operator _more_
     * @param attrId _more_
     * @param value _more_
     */
    public NumericRecordFilter(int operator, int attrId, double value) {
        this.operator = operator;
        this.value    = value;
        this.attrId   = attrId;
    }


    /**
     * _more_
     *
     * @param operator _more_
     * @param v1 _more_
     *
     * @return _more_
     */
    static int cnt = 0;

    /**
     * _more_
     *
     * @param operator _more_
     * @param v1 _more_
     *
     * @return _more_
     */
    public boolean evaluate(int operator, double v1) {
        if (operator == OP_EQUALS) {
            return v1 == value;
        }
        if (operator == OP_LT) {
            return v1 < value;
        }
        if (operator == OP_LE) {
            return v1 <= value;
        }
        if (operator == OP_GT) {
            return v1 > value;
        }
        if (operator == OP_GE) {
            return v1 >= value;
        }

        throw new IllegalArgumentException("Unknown operator:" + operator);
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
        return evaluate(operator, record.getValue(attrId));
    }



    /**
     * _more_
     *
     * @param name _more_
     *
     * @return _more_
     */
    public static int getOperator(String name) {
        if (name.equalsIgnoreCase("eq")) {
            return OP_EQUALS;
        }
        if (name.equalsIgnoreCase("lt")) {
            return OP_LT;
        }
        if (name.equalsIgnoreCase("le")) {
            return OP_LE;
        }
        if (name.equalsIgnoreCase("gt")) {
            return OP_GT;
        }
        if (name.equalsIgnoreCase("ge")) {
            return OP_GE;
        }

        throw new IllegalArgumentException("Unknown operator:" + name);
    }

    /**
     * _more_
     *
     * @param operator _more_
     *
     * @return _more_
     */
    public String getOperatorName(int operator) {
        if (operator == OP_EQUALS) {
            return "eq";
        }
        if (operator == OP_LT) {
            return "lt";
        }
        if (operator == OP_LE) {
            return "le";
        }
        if (operator == OP_GT) {
            return "gt";
        }
        if (operator == OP_GE) {
            return "ge";
        }

        throw new IllegalArgumentException("Unknown operator:" + operator);
    }






}
