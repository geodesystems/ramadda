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

package org.ramadda.util.text;


import org.ramadda.util.Utils;

import java.io.*;

import java.text.DateFormat;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import java.util.ArrayList;


import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

import java.util.regex.*;


/**
 *
 * @author Jeff McWhirter
 */

public class Row {

    /** _more_ */
    static int x = 0;

    /** _more_ */
    private List values;

    /** _more_ */
    private Object skipTo;

    /**
     * _more_
     */
    public Row() {
        values = new ArrayList() {
            public boolean xadd(Object o) {
                boolean v = super.add(o);
                return v;
            }
        };
    }

    /**
     * _more_
     *
     * @param values _more_
     */
    public Row(List values) {
        this.values = values;
    }

    /**
     * _more_
     *
     * @param values _more_
     */
    public Row(Object[] values) {
        this.values = new ArrayList();
        for (Object o : values) {
            this.values.add(o);
        }
    }


    /**
     * _more_
     *
     * @param line _more_
     * @param delimiter _more_
     */
    public Row(String line, String delimiter) {
        this(Utils.tokenizeColumns(line, delimiter));
    }


    /**
     * Set the SkipTo property.
     *
     * @param value The new value for SkipTo
     */
    public void setSkipTo(Object value) {
        skipTo = value;
    }

    /**
     * Get the SkipTo property.
     *
     * @return The SkipTo
     */
    public Object getSkipTo() {
        return skipTo;
    }





    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return " id:" + " " + values.toString();
    }

    /**
     * Set the Values property.
     *
     * @param value The new value for Values
     */
    public void setValues(List value) {
        values = value;
    }

    /**
     * Get the Values property.
     *
     * @return The Values
     */
    public List getValues() {
        return values;
    }

    /**
     * _more_
     *
     * @param index _more_
     *
     * @return _more_
     */
    public Object get(int index) {
        return values.get(index);
    }

    /**
     * _more_
     *
     * @param index _more_
     *
     * @return _more_
     */
    public String getString(int index) {
        if (index >= values.size()) {
            System.err.println("Row error:" + index + " " + values);
        }
        Object o = values.get(index);

        return (o == null)
               ? ""
               : o.toString();
    }

    /**
     * _more_
     *
     * @param index _more_
     * @param object _more_
     */
    public void set(int index, Object object) {
        values.set(index, object);
    }

    /**
     * _more_
     *
     * @param object _more_
     */
    public void insert(Object object) {
        values.add(object);
    }

    /**
     * _more_
     *
     * @param object _more_
     */
    public void add(Object object) {
        values.add(object);
    }

    /**
     * _more_
     *
     * @param index _more_
     * @param object _more_
     */
    public void insert(int index, Object object) {
        values.add(index, object);
    }

    /**
     * _more_
     *
     * @param index _more_
     */
    public void remove(int index) {
        values.remove(index);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public int size() {
        return values.size();
    }



    /**
     * Class description
     *
     *
     * @version        $version$, Wed, Nov 25, '15
     * @author         Enter your name here...
     */
    public static class RowCompare implements Comparator<Row> {

        /** _more_ */
        private boolean checked = false;

        /** _more_ */
        private int idx;

        /** _more_ */
        private boolean ascending;

        /** _more_ */
        private boolean isNumber = false;

        /**
         * _more_
         *
         *
         * @param idx _more_
         * @param asc _more_
         */
        public RowCompare(int idx, boolean asc) {
            this.idx       = idx;
            this.ascending = asc;
        }

        /**
         * _more_
         *
         *
         * @param r1 _more_
         * @param r2 _more_
         *
         * @return _more_
         */
        public int compare(Row r1, Row r2) {
            int    result;
            Object o1 = r1.get(idx);
            Object o2 = r2.get(idx);
            String s1 = o1.toString();
            String s2 = o2.toString();
            if ( !checked) {
                try {
                    checked = true;
                    double d = Double.parseDouble(s1);
                    isNumber = true;
                } catch (Exception e) {}
            }

            int dir = 0;
            if (isNumber) {
                double d1 = Double.parseDouble(s1);
                double d2 = Double.parseDouble(s2);
                if (d1 < d2) {
                    dir = -1;
                } else if (d1 > d2) {
                    dir = 1;
                } else {
                    dir = 0;
                }
            } else {
                dir = s1.compareTo(s2);
            }
            if (dir == 0) {
                return 0;
            }

            return ascending
                   ? dir
                   : -dir;
        }

    }




}
