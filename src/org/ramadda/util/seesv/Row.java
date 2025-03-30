/**
Copyright (c) 2008-2025 Geode Systems LLC
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

@SuppressWarnings("unchecked")
public class Row {

    private static int cnt = 0;

    private int id = (cnt++);

    private int rowCount = -1;

    private List values;

    private Date dateForSort;

    private Row unitRow;

    public Row() {
        values = new ArrayList();
    }

    public Row(Row r) {
        values = new ArrayList(r.getValues());
    }

    public Row(List values) {
        this.values = values;
    }

    public Row(Object[] values) {
        this.values = new ArrayList();
        for (Object o : values) {
            this.values.add(o);
        }
    }

    public int getId() {
        return id;
    }

    /**
       is this row the first row created by the DataProvider
     */
    public boolean isFirstRowInData() {
	return rowCount==0;
    }

    public void setRowCount(int value) {
        rowCount = value;
    }

    public int getRowCount() {
        return rowCount;
    }

    public String toString() {
        return " id:" + id + " values:" + values.toString();
    }

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
        return (index >= 0) && (index < size());
    }

    public void setValues(List value) {
        values = value;
    }

    public List getValues() {
        return values;
    }

    public Object get(int index) {
	if(index<0 || index>=values.size()) {
	    throw new IllegalArgumentException("SeeSV error: bad row index:" + index+" size:" + values.size() +" values:" + this);
	}
        return values.get(index);
    }

    public double getDouble(int index) {
        String s = getString(index);

        return Seesv.parseDouble(s);
    }

    public String getString(int index) {
	return getString(index,"");
    }

    public String getString(int index,String dflt) {	
        if (index >= values.size()) {
            System.err.println("Row error:" + index + " " + values);
        }
        Object o = values.get(index);

        return (o == null)
               ? dflt
               : o.toString();
    }

    public byte[] getBytes(int index) throws UnsupportedEncodingException {
        String s = getString(index);

        return s.getBytes("UTF-8");
    }

    public void set(int index, Object object) {
        while (index >= values.size()) {
            values.add("");
        }
        values.set(index, object);
    }

    public void insert(Object object) {
        values.add(object);
    }

    public void addAll(List values) {
	this.values.addAll(values);
    }

    public void add(Object... args) {
        for (Object object : args) {
            values.add(object);
        }
    }

    public void add(double v) {
	if(v==(int)v) values.add((int)v);
	else values.add(v);
    }

    public void insert(int index, Object object) {
        values.add(index, object);
    }

    public void remove(int index) {
        values.remove(index);
    }

    public int size() {
        return values.size();
    }

    public static class RowCompare implements Comparator<Row> {

	private TextReader ctx;

        private boolean checked = false;

        private List<Integer> indices;

        private boolean ascending;

        private boolean isNumber = false;

	private String how="string";

        public RowCompare(TextReader ctx,List<Integer> indices, boolean asc,String how) {
	    this.ctx = ctx;
            this.indices   = indices;
            this.ascending = asc;
	    this.how = how.trim();
	    if(this.how.equals("")) this.how = "string";
        }

        public RowCompare(TextReader ctx,int idx, boolean asc) {
	    this.ctx= ctx;
            this.indices = new ArrayList<Integer>();
            this.indices.add(idx);
            this.ascending = asc;
        }

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

    public void setUnitRow (Row value) {
	unitRow = value;
    }

    public Row getUnitRow () {
	return unitRow;
    }

}
