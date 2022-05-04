/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
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

@SuppressWarnings("unchecked")
public class Row {

    /** _more_ */
    private static int cnt = 0;

    /** _more_ */
    private int id = (cnt++);


    /** _more_ */
    private List values;


    /**
     * _more_
     */
    public Row() {
        values = new ArrayList();
    }

    /**
     * _more_
     *
     * @param r _more_
     */
    public Row(Row r) {
        values = new ArrayList(r.getValues());
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
     * @return _more_
     */
    public int getId() {
        return id;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return " id:" + id + " v:" + values.toString();
    }

    /**
     * _more_
     *
     * @param r _more_
     *
     * @return _more_
     */
    public String print(Row r) {
        StringBuilder sb = new StringBuilder("***** row:" + values.size()
                                             + "\n");
        for (int i = 0; i < values.size(); i++) {
            sb.append("\t" + ((r != null)
                              ? r.get(i)
                              : "") + "[" + i + "]=" + values.get(i) + "\n");
        }

        return sb.toString();
    }

    public boolean indexOk(int index) {
	return index>=0 && index<size();
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

    public double getDouble(int index) {
	String s = getString(index);
	return Double.parseDouble(s);
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
     *
     * @param index _more_
     *  @return _more_
     *
     * @throws UnsupportedEncodingException _more_
     */
    public byte[] getBytes(int index) throws UnsupportedEncodingException {
        String s = getString(index);

        return s.getBytes("UTF-8");
    }

    /**
     * _more_
     *
     * @param index _more_
     * @param object _more_
     */
    public void set(int index, Object object) {
        while (index >= values.size()) {
            values.add("");
        }
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
     *
     * @param values _more_
     */
    public void addAll(List values) {
        for (Object value : values) {
            add(value);
        }
    }

    /**
     * _more_
     *
     * @param args _more_
     */
    public void add(Object... args) {
        for (Object object : args) {
            values.add(object);
        }
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
            int result;
            if ((idx < 0) || (idx >= r1.size())) {
                return 1;
            }
            if ((idx < 0) || (idx >= r2.size())) {
                return 0;
            }
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
