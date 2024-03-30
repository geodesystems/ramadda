/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util.seesv;


import org.ramadda.util.Utils;

import ucar.unidata.util.StringUtil;

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


    /**  */
    private int rowCount = -1;

    /** _more_ */
    private List values;

    private Date dateForSort;

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
       is this row the first row created by the DataProvider
     */
    public boolean isFirstRowInData() {
	return rowCount==0;
    }

    /**
     * Set the RowCount property.
     *
     * @param value The new value for RowCount
     */
    public void setRowCount(int value) {
        rowCount = value;
    }

    /**
     * Get the RowCount property.
     *
     * @return The RowCount
     */
    public int getRowCount() {
        return rowCount;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return " id:" + id + " values:" + values.toString();
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

    /**
     *
     * @param index _more_
      * @return _more_
     */
    public boolean indexOk(int index) {
        return (index >= 0) && (index < size());
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
	if(index<0 || index>=values.size()) {
	    throw new IllegalArgumentException("SeeSV error: bad row index:" + index+" size:" + values.size() +" values:" + this);
	}
        return values.get(index);
    }

    /**
     *
     * @param index _more_
      * @return _more_
     */
    public double getDouble(int index) {
        String s = getString(index);

        return Seesv.parseDouble(s);
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

    public void add(double v) {
	if(v==(int)v) values.add((int)v);
	else values.add(v);
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

	private TextReader ctx;

        /** _more_ */
        private boolean checked = false;

        /** _more_ */
        private List<Integer> indices;

        /** _more_ */
        private boolean ascending;

        /** _more_ */
        private boolean isNumber = false;

	private String how="string";

        /**
         *
         * @param indices _more_
         * @param asc _more_
         */
        public RowCompare(TextReader ctx,List<Integer> indices, boolean asc,String how) {
	    this.ctx = ctx;
            this.indices   = indices;
            this.ascending = asc;
	    this.how = how.trim();
	    if(this.how.equals("")) this.how = "string";
        }


        /**
         * _more_
         *
         *
         * @param idx _more_
         * @param asc _more_
         */
        public RowCompare(TextReader ctx,int idx, boolean asc) {
	    this.ctx= ctx;
            this.indices = new ArrayList<Integer>();
            this.indices.add(idx);
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
            for (int idx : indices) {
		int dir = 0;
                if ((idx < 0) || (idx >= r1.size())) {
		    dir = 1;
                } else if ((idx < 0) || (idx >= r2.size())) {
		    dir = -1;
                } else {
		    Object o1 = r1.get(idx);
		    Object o2 = r2.get(idx);
		    String s1 = o1.toString();
		    String s2 = o2.toString();
		    if(how.equals("string")) {
			dir= s1.compareTo(s2);
		    } else if(how.equals("length")) {
			dir= s1.length()-s2.length();
		    } else if(how.equals("date")) {
			Date d1 = r1.dateForSort;
			Date d2 = r2.dateForSort;
			if(d1==null) {
			    try {
				r1.dateForSort = d1 = ctx.parseDate(s1);
			    } catch(Exception exc) {
				System.err.println("bad date(1): " + s1);
			    }
			}
			if(d2==null) {
			    try {
				r2.dateForSort = d2 = ctx.parseDate(s2);
			    } catch(Exception exc) {
				System.err.println("bad date(2): " + s2);
			    }
			}
			if(d1==null && d2==null) dir = 0;
			else if(d1==null) dir=-1;
			else if(d2==null) dir=1;
			else dir = d1.compareTo(d2);
		    } else if(how.equals("extract")) {
			String[] p1 = Utils.findPatterns(s1,"[^\\d]*([\\d\\.]+)([^\\d]*)");
			String[] p2 = Utils.findPatterns(s2,"[^\\d]*([\\d\\.]+)([^\\d]*)");		    
			if(p1==null && p2==null) dir = 0;
			else if(p1==null) dir=-1;
			else if(p2==null) dir=1;
			else {
			    dir = (int)(Seesv.parseDouble(p1[0])-Seesv.parseDouble(p2[0]));
			    if(dir==0) {
				dir= p1[0].compareTo(p2[1]);
			    }
			}
		    } else {
			if ( !checked) {
			    try {
				checked = true;
				double d = Seesv.parseDouble(s1);
				isNumber = true;
			    } catch (Exception e) {}
			}
			if (isNumber) {
			    double d1 = Seesv.parseDouble(s1);
			    double d2 = Seesv.parseDouble(s2);
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
		    }
		}
		if (dir == 0) {
		    continue;
		}
                return ascending
                       ? dir
                       : -dir;
            }

            return 0;

        }

    }




}
