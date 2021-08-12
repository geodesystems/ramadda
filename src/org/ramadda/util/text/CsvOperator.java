/**
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


import org.ramadda.util.GeoUtils;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;

import org.ramadda.util.Utils;

import ucar.unidata.util.StringUtil;

import java.io.*;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

import java.util.regex.*;



/**
 * Class description
 *
 *
 * @version        $version$, Fri, Jan 9, '15
 * @author         Jeff McWhirter
 */
public class CsvOperator {

    public static final HtmlUtils HU = null;

    /** _more_ */
    public static int OP_LT = 0;

    /** _more_ */
    public static int OP_LE = 1;

    /** _more_ */
    public static int OP_GT = 2;

    /** _more_ */
    public static int OP_GE = 3;

    /** _more_ */
    public static int OP_EQUALS = 4;

    /** _more_ */
    public static int OP_NOTEQUALS = 5;

    /** _more_ */
    public static int OP_DEFINED = 6;

    /** _more_ */
    public static int OP_MATCH = 7;

    /** _more_ */
    protected int rowCnt = 0;

    /** _more_ */
    public static final int UNDEFINED_INDEX = -1;

    /** _more_ */
    public static final int INDEX_ALL = -9999;

    /** _more_ */
    private int index = UNDEFINED_INDEX;

    /** _more_ */
    protected List<String> sindices;

    /** _more_ */
    List<Integer> indices;

    /** _more_ */
    HashSet<Integer> indexMap;

    /** _more_ */
    HashSet<Integer> colsSeen = new HashSet<Integer>();

    /** _more_ */
    private List header;

    /** _more_ */
    private Hashtable<String, Integer> columnMap;

    /** _more_ */
    private List<String> columnNames;


    /** _more_ */
    private String scol;

    /**
     * _more_
     */
    public CsvOperator() {}

    /**
     * _more_
     *
     * @param col _more_
     */
    public CsvOperator(String col) {
        sindices = new ArrayList<String>();
	if(Utils.stringDefined(col)) {
	    sindices.add(col);
	}
    }

    /**
     * _more_
     *
     * @param cols _more_
     */
    public CsvOperator(List<String> cols) {
        this.sindices = cols;
    }


    public String makeID(Object obj) {
	String colId = Utils.makeLabel(obj.toString());
	colId = colId.toLowerCase().replaceAll(" ",
					       "_").replaceAll("[^a-z0-9]", "_");
	colId = colId.replaceAll("_+_", "_");
	colId = colId.replaceAll("_$", "");
	return colId;
    }
	

    public void setIndices(List<Integer> i) {
	indices=i;
    }


    public boolean hasColumns() {
	return sindices!=null && sindices.size()>0;
    }

    /** _more_ */
    private Hashtable<String, Integer> debugCounts = new Hashtable<String,
                                                         Integer>();

    /**
     * _more_
     *
     * @param msg _more_
     */
    public void debug(String msg) {
        debug(msg, null);
    }

    /**
     * _more_
     *
     * @param msg _more_
     * @param extra _more_
     */
    public void debug(String msg, Object extra) {
        if (true) {
            return;
        }
        Integer cnt = debugCounts.get(msg);
        if (cnt == null) {
            cnt = new Integer(0);
        } else {
            if (cnt > 5) {
                return;
            }
            cnt = new Integer(cnt + 1);
        }
        debugCounts.put(msg, cnt);
        String c = getClass().getName();
        c = c.substring(c.indexOf("$") + 1);
        System.err.println(c + ":" + msg + ((extra != null)
                                            ? " " + extra
                                            : ""));
    }


    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public double parse(String s) {
        s = s.trim().replaceAll(",", "");
        if (s.equals("")) {
            return 0;
        }

        return Double.parseDouble(s);
    }



    /**
     * _more_
     *
     * @param reader _more_
     * @param row _more_
     * @param values _more_
     */
    public void add(TextReader reader, Row row, Object... values) {
        if (reader.getPositionStart()) {
            for (int i = values.length - 1; i >= 0; i--) {
                row.insert(0, values[i]);
            }
        } else {
            for (Object value : values) {
                row.add(value);
            }
        }
    }



    /**
     * _more_
     *
     * @param ctx _more_
     * @param row _more_
     *
     * @throws Exception _more_
     */
    public void processFirstRow(TextReader ctx, Row row) throws Exception {}



    /**
     * _more_
     *
     * @return _more_
     */
    public String getDescription() {
        String className = getClass().getName();

        return className.replace("org.ramadda.util.text.",
                                 "").replaceAll("^[^\\$]+\\$", "");
    }


    /**
     * _more_
     *
     * @param row _more_
     */
    public void setHeaderIfNeeded(Row row) {
        if (header == null) {
            setHeader(row.getValues());
        }
    }


    /**
     * _more_
     *
     * @param header _more_
     */
    public void setHeader(List header) {
	List tmp = new ArrayList();
	tmp.addAll(header);
        this.header = tmp;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean hasHeader() {
        return header != null;
    }


    public void fatal(String msg) throws RuntimeException {
	throw new RuntimeException(msg +" function: " + getClass().getSimpleName());
    }

    public void fatal(String msg,Exception exc) throws RuntimeException {
	throw new RuntimeException( msg +" function: " + getClass().getSimpleName(),exc);
    }


    /**
     * _more_
     *
     * @param ctx _more_
     *
     * @return _more_
     */
    public int getIndex(TextReader ctx) {
        if (index != UNDEFINED_INDEX) {
            return index;
        }
	List<Integer>  indices = getIndices(ctx);
	if(indices.size()==0) fatal("No indices specified");
	index = indices.get(0);

        return index;
    }


    public boolean getBoolean(String v) {
	if(v==null) return false;
	if(v.equals("true")) return true;
	if(v.equals("false") || !Utils.stringDefined(v)) return false;
	try {
	    double d = Double.parseDouble(v);
	    if(d==0) return false;
	    return true;
	} catch(Exception exc) {
	}
	return false;
    }



    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public static int getOperator(String s) {
        s = s.trim();
        if (s.equals("<")) {
            return OP_LT;
        }
        if (s.equals("<=")) {
            return OP_LE;
        }
        if (s.equals(">")) {
            return OP_GT;
        }
        if (s.equals(">=")) {
            return OP_GE;
        }
        if (s.equals("=")) {
            return OP_EQUALS;
        }
        if (s.equals("!=")) {
            return OP_NOTEQUALS;
        }
        if (s.equals("~")) {
            return OP_MATCH;
        }

        throw new RuntimeException("unknown operator:" + s);
    }




    /** _more_ */
    public static final String[] FILE_PREFIXES = { "/org/ramadda/repository/resources/geo" };

    /**
     * _more_
     *
     * @param filename _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static InputStream getInputStream(String filename)
            throws Exception {
        try {
            return new FileInputStream(filename);
        } catch (Exception exc) {
            try {
                return IO.getInputStream(filename, CsvOperator.class);
            } catch (Exception exc2) {
                for (String prefix : FILE_PREFIXES) {
                    try {
                        return IO.getInputStream(prefix + "/" + filename,
                                CsvOperator.class);
                    } catch (Exception exc3) {}
                }
            }
        }

        throw new RuntimeException("Could not open file:" + filename);
    }


    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public int xgetColumnIndex(String s) {
        List<Integer> indices = new ArrayList<Integer>();
        getColumnIndex(null, indices, s,null);
        return indices.get(0);
    }

    private boolean isLastIndex(String tok) {
	return tok.startsWith("_last") && tok.endsWith("_");
    }

    private int getLastIndex(String tok, int last) {
	if (tok.equals("_last_")) {
	    return last;
	}
	return -1;
    }

    public Integer getColumnIndex(TextReader ctx, String tok) {
	checkColumns();
	Integer iv = columnMap.get(tok);
	if(iv==null) {
	    tok = ctx.getFieldAlias(tok);
	    if(tok!=null) {
		iv = columnMap.get(tok);
	    }
	}
	return iv;
    }


    private void checkColumns() {
	if (columnNames == null) {
	    columnNames = new ArrayList<String>();
	    columnMap = new Hashtable<String, Integer>();
	    if (header == null) {
		debug("no names or header");
		return;
	    }

	    for (int i = 0; i < header.size(); i++) {
		String colName = (String) header.get(i);
		String colId = Utils.makeID(colName,false);
		columnNames.add(colName);
		colName = colName.trim();
		columnMap.put(colName, i);
		columnMap.put(colId, i);
		columnMap.put(colName.toLowerCase(), i);
		columnMap.put(i+"", i);
	    }
	}
    }


    /**
     * _more_
     *
     *
     *
     * @param ctx _more_
     * @param indices _more_
     * @param s _more_
     *
     */
    public void getColumnIndex(TextReader ctx, List<Integer> indices,
                               String s,HashSet seen) {
	if(seen==null) seen = new HashSet();
        List<String> toks  = Utils.splitUpTo(s, "-", 2);
        int          start = -1;
        int          end   = -1;
	checkColumns();
	if(s.startsWith("regex:")) {
	    s = s.substring("regex:".length());
	    //	    System.err.println("pattern:" + s+":");
	    Pattern      p      = Pattern.compile(s);
	    for(String name: columnNames) {
		Matcher      m      = p.matcher(name);
		if(m.matches()) {
		    //System.err.println("\tmatch:" + name);
		    indices.add(columnMap.get(name));

		} else {
		    //		    System.err.println("\tno match:" + name);
		}
	    }
	    return;
	}

        s = s.toLowerCase().trim();

        try {
            if (toks.size() == 1) {
		//not now		if(Utils.testAndSet(seen,s)) return;
                start = end = Integer.parseInt(s);
            } else {
                start = Integer.parseInt(toks.get(0));
                end   = Integer.parseInt(toks.get(1));
            }
        } catch (NumberFormatException exc) {
            if (toks.size() == 1) {
                String tok = toks.get(0);
                if (isLastIndex(tok)) {
		    indices.add(getLastIndex(tok,header.size()-1));
		    return;
		}

                if (tok.equals("*")) {
                    for (int i = 0; i < header.size(); i++) {
                        if ( !colsSeen.contains(i)) {
                            colsSeen.add(i);
                            indices.add(i);
                        }
                    }

                    return;
                }
                if (StringUtil.containsRegExp(tok)) {
                    for (int i = 0; i < header.size(); i++) {
                        if ( !colsSeen.contains(i)) {
                            String colName = (String) header.get(i);
                            if (colName.matches(tok)) {
                                colsSeen.add(i);
                                indices.add(i);
                            }
                        }
                    }

                    return;
                }
                Integer iv = getColumnIndex(ctx, tok);
                if (iv != null) {
                    start = end = iv;
                } else {
                    //Not sure whether we should throw an error
		    for(String colName:columnNames) {
			//			System.err.println(colName);
		    }			
		    for (Enumeration keys = columnMap.keys(); keys.hasMoreElements(); ) {
			String key =(String) keys.nextElement(); 
			if(key.length()==0) continue;
			if(key.matches("^[0-9]+$")) continue;
			System.err.println("key:" + key);
		    }
		    //		    System.err.println(columnMap);
                    fatal("Could not find index:" + tok);
                }
            } else {
		String tok1 = toks.get(0);
		String tok2 = toks.get(1);
                if (isLastIndex(tok1)) {
		    start = getLastIndex(tok1,header.size()-1);
		} else {
		    Integer iv = getColumnIndex(ctx, tok1);
		    if(iv!=null) start=iv;
		}
                if (isLastIndex(tok2)) {
		    end = getLastIndex(tok2,header.size()-1);
		} else {
		    Integer iv = getColumnIndex(ctx, tok2);
		    if(iv!=null) end=iv;
		}
		if(start==-1 || end==-1) {
		    for (Enumeration keys = columnMap.keys(); keys.hasMoreElements(); ) {
			String key =(String) keys.nextElement(); 
			System.err.println("key:" + key);
		    }
		    fatal("Could not find indices:" + toks);
		}
		/*
		  Integer iv2 = getColumnIndex(ctx, tok2);
                if ((iv1 != null) && (iv2 != null)) {
                    start = iv1;
                    end   = iv2;
                }
		*/
            }
        }
        if (start >= 0) {
            for (int i = start; i <= end; i++) {
		//not now		if(Utils.testAndSet(seen,i)) continue;
                colsSeen.add(i);
                indices.add(i);
            }
        }

        /*
        for (int i = 0; i < columnNames.size(); i++) {
            String v = columnNames.get(i);
            if (v.startsWith(s)) {
                columnMap.put(v, i);
                indices.add(i);
                return;
            }
            }*/
    }


    /**
     * _more_
     *
     * @param ctx _more_
     *
     * @return _more_
     */
    public List<Integer> getIndices(TextReader ctx) {
        if (indices == null) {
            indices = getIndices(ctx, sindices);
        }

        return indices;
    }

    /**
     * _more_
     *
     *
     * @param ctx _more_
     * @param cols _more_
     *
     * @return _more_
     */
    public List<Integer> getIndices(TextReader ctx, List<String> cols) {
        debug("getIndices:" + cols);
        if (cols == null) {
            return null;
        }
        List<Integer> indices = new ArrayList<Integer>();
	HashSet seen = new HashSet();
        for (String s : cols) {
            getColumnIndex(ctx, indices, s, seen);
        }
        return indices;
    }



    /**
     * _more_
     *
     * @param ctx _more_
     * @param idx _more_
     *
     * @return _more_
     */
    public int getIndex(TextReader ctx, String idx) {
        List<Integer> indices = new ArrayList<Integer>();
        getColumnIndex(ctx, indices, idx,new HashSet());
        if (indices.size() == 0) {
            throw new IllegalArgumentException("Could not find column index:"
                    + idx);

        }

        return indices.get(0);
    }



    public Row removeColumns(List<Integer> indices, Row row)  {
	if (indices.size() == 0) {
	    debug("processRow- no indices");
	    return row;
	}
	List<String> result = new ArrayList<String>();
	for (int i = 0; i < row.size(); i++) {
	    if ( !indices.contains(i)) {
		result.add(row.getString(i));
	    }
	}
	return new Row(result);
    }




    /**
     * _more_
     *
     * @param ctx _more_
     * @param row _more_
     *
     * @return _more_
     */
    public Row filterValues(TextReader ctx, Row row) {
        row.setValues(filterValues(ctx, row.getValues()));

        return row;
    }

    /**
     * _more_
     *
     * @param ctx _more_
     * @param values _more_
     *
     * @return _more_
     */
    public List filterValues(TextReader ctx, List values) {
        List             newValues = new ArrayList();
        HashSet<Integer> indexMap  = getIndexMap(ctx);
        for (int i = 0; i < values.size(); i++) {
            if ( !indexMap.contains(i)) {
                newValues.add(values.get(i));
            }
        }

        return newValues;
    }


    /**
     * _more_
     *
     * @param ctx _more_
     *
     * @return _more_
     */
    public HashSet<Integer> getIndexMap(TextReader ctx) {
        if (indexMap == null) {
            List<Integer> indices = getIndices(ctx);
            indexMap = new HashSet();
            for (Integer i : indices) {
                indexMap.add(i);
            }
        }

        return indexMap;
    }


    /**
     * _more_
     *
     * @param rows _more_
     * @param indices _more_
     * @param keys _more_
     *
     * @return _more_
     */
    public Hashtable<Object, List<Row>> groupRows(List<Row> rows,
            List<Integer> indices, List keys) {
        Hashtable<Object, List<Row>> rowMap = new Hashtable<Object,
                                                  List<Row>>();
        for (Row row : rows) {
            List          values = row.getValues();
            StringBuilder key    = new StringBuilder();
            for (int idx : indices) {
                key.append(values.get(idx).toString());
                key.append("-");
            }
            String    k     = key.toString();
            List<Row> group = rowMap.get(k);
            if (group == null) {
                if (keys != null) {
                    keys.add(k);
                }
                group = new ArrayList<Row>();
                rowMap.put(k, group);
            }
            group.add(row);
        }

        return rowMap;
    }



}
