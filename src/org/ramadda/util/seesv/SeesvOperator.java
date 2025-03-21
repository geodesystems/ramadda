/**
   Copyright (c) 2008-2023 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util.seesv;


import org.apache.commons.codec.language.Soundex;
import org.apache.commons.text.similarity.FuzzyScore;

import org.apache.commons.text.similarity.JaroWinklerDistance;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;

import org.ramadda.util.Utils;


import org.ramadda.util.geo.GeoUtils;

import ucar.unidata.util.LogUtil;

import ucar.unidata.util.StringUtil;

import java.io.*;

import java.text.DateFormat;
import java.text.DecimalFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Hashtable;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.regex.*;


/**
 * Class description
 *
 *
 * @version        $version$, Fri, Jan 9, '15
 * @author         Jeff McWhirter
 */
@SuppressWarnings("unchecked")
public class SeesvOperator {

    /**  */
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

    /**  */
    public static final String OPERAND_PERCENT = "percent";

    /**  */
    public static final String OPERAND_COUNT = "count";

    /**  */
    public static final String OPERAND_SUM = "sum";

    /**  */
    public static final String OPERAND_MIN = "min";

    /**  */
    public static final String OPERAND_MAX = "max";

    /**  */
    public static final String OPERAND_AVERAGE = "average";

    public static final String OPERAND_AVG = "avg";    


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
    private LinkedHashMap<String, Integer> columnMap;

    /** _more_ */
    private List<String> columnNames;


    /** _more_ */
    private String scol;

    /**  */
    Seesv seesv;



    /**
     * _more_
     */
    public SeesvOperator() {}

    /**
     
     *
     * @param seesv _more_
     */
    public SeesvOperator(Seesv seesv) {
        this.seesv = seesv;
    }


    /**
     * _more_
     *
     * @param col _more_
     */
    public SeesvOperator(String col) {
        sindices = new ArrayList<String>();
        if (Utils.stringDefined(col)) {
            sindices.add(col);
        }
    }

    /**
     * _more_
     *
     * @param cols _more_
     */
    public SeesvOperator(List<String> cols) {
        this.sindices = cols;
    }


    public String replaceMacros(String v, Row header, Row row) {
	for(int j=0;j<row.size();j++) {
	    if(j<header.size()) {
		v = v.replace("${" + Utils.makeID(header.getString(j))+"}", row.getString(j));
		v = v.replace("${" + header.getString(j)+"}", row.getString(j));		
	    }
	    v = v.replace("${" + j+"}", row.getString(j));
	    v = v.replace("${column_index}", ""+j);
	}
	return v;
    }

    /**
     *
     * @param name _more_
     * @return _more_
     */
    public String getProperty(String name) {
        if (seesv != null) {
            return seesv.getProperty(name);
        }

        return null;
    }



    /**
     *
     * @param obj _more_
     *
     * @return _more_
     */
    public String makeID(Object obj) {
        String colId = Utils.makeLabel(obj.toString());
        colId = colId.toLowerCase().replaceAll(" ",
					       "_").replaceAll("[^a-z0-9]", "_");
        colId = colId.replaceAll("_+_", "_");
        colId = colId.replaceAll("_$", "");

        return colId;
    }



    /**  */
    private JaroWinklerDistance jaro;

    /**  */
    private Soundex soundex;

    /**  */
    private FuzzyScore fuzzy;

    /**
     *
     * @param s1 _more_
     * @param s2 _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public int similarScore(String s1, String s2) throws Exception {
        int levenshteinScore = me.xdrop.fuzzywuzzy.FuzzySearch.ratio(s1, s2);
        if (true) {
            return levenshteinScore;
        }
        if (soundex == null) {
            soundex = new Soundex();
            jaro    = new JaroWinklerDistance();
            //      fuzzy = new FuzzyScore(java.util.Locale.getDefault());
        }
        //      int fuzzyScore = fuzzy.fuzzyScore(s1,s2).intValue();
        //      int soundexScore = 25*soundex.difference(s1,s2);
        int jaroScore = (int) (100 * jaro.apply(s1, s2));
        int max       = Math.max(levenshteinScore, jaroScore);

        //      if(max>90)
        //          System.err.println(levenshteinScore +" " + jaroScore);
        return max;
    }


    /**
     *
     * @param i _more_
     */
    public void setIndices(List<Integer> i) {
        indices = i;
    }


    /**
     *
     * @return _more_
     */
    public boolean hasColumns() {
        return (sindices != null) && (sindices.size() > 0);
    }

    public void checkUniqueRow(Row row) {
	HashSet seen = new HashSet();
	for(int i=0;i<row.size();i++) {
	    Object o = row.get(i);
	    String s = makeID(o);
	    if(seen.contains(s))
		throw new RuntimeException("Non unique header value:" + o);
	    seen.add(s);
	}
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

    public boolean debug = false;

    /**
     * _more_
     *
     * @param msg _more_
     * @param extra _more_
     */
    public void debug(String msg, Object extra) {
        if (!debug) {
            return;
        }
        Integer cnt = debugCounts.get(msg);
        if (cnt == null) {
            cnt = Integer.valueOf(0);
        } else {
            if (cnt > 5) {
                return;
            }
            cnt = Integer.valueOf(cnt + 1);
        }
        debugCounts.put(msg, cnt);
        String c = getClass().getName();
        c = c.substring(c.indexOf("$") + 1);
        System.err.println(c + ":" + msg + ((extra != null)
                                            ? " " + extra
                                            : ""));
    }


    /**
     * Parse the string as a double
     *
     * @param s the string
     *
     * @return the double
     */
    public double parse(String s) {
	return parse(null, s);
    }

    /**
     * Parse the string as a double
     * Use the row (if non-null) to add context to the error message
     *
     * @param s The string to parse
     *
     * @return double value
     */
    public double parse(Row row, String s) {
        s = s.trim().replaceAll(",", "");
        if (s.equals("")) {
            return 0;
        }

	try {
	    return Double.parseDouble(s);
	} catch(NumberFormatException nfe) {
	    if(row!=null) 
		throw new SeesvException(this, "Error parsing value:" + s +" row:" + row);
	    else 
		throw new SeesvException(this, "Error parsing value:" + s);
	}
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

        return className.replace("org.ramadda.util.seesv.",
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


    /**
     *
     *
     * @param ctx _more_
     * @param msg _more_
     *
     * @throws RuntimeException _more_
     */
    public void fatal(TextReader ctx, String msg) throws RuntimeException {
        throw new SeesvException(this,msg);
    }

    /**
     *
     *
     * @param ctx _more_
     * @param msg _more_
     * @param exc _more_
     *
     * @throws RuntimeException _more_
     */
    public void fatal(TextReader ctx, String msg, Exception exc)
	throws RuntimeException {
        Throwable inner = LogUtil.getInnerException(exc);

        throw new RuntimeException(msg + " function: "
                                   + getClass().getSimpleName(), inner);
    }


    /**
     * _more_
     *
     * @param ctx _more_
     *
     * @return _more_
     */
    public int getIndex(TextReader ctx,boolean...missingOk) {	
        if (index != UNDEFINED_INDEX) {
            return index;
        }
        List<Integer> indices = getIndices(ctx);
        if (indices.size() == 0) {
	    if(missingOk.length==0 || !missingOk[0]) {
		fatal(ctx, "No indices specified");
	    }
        }
	if(indices.size()==0) {
	    return UNDEFINED_INDEX;
	}	    
        index = indices.get(0);

        return index;
    }


    /**
     *
     * @param v _more_
     *
     * @return _more_
     */
    public boolean getBoolean(String v) {
        if (v == null) {
            return false;
        }
        if (v.equals("true")) {
            return true;
        }
        if (v.equals("false") || !Utils.stringDefined(v)) {
            return false;
        }
        try {
            double d = Double.parseDouble(v);
            if (d == 0) {
                return false;
            }

            return true;
        } catch (Exception exc) {}

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


    public boolean  checkOperator(int op, double v1,double v2) {
	if (op == OP_LT) {
	    return v1<v2;
	}
	if (op == OP_LE) {
	    return v1<=v2;
	}
	if (op == OP_GT) {
	    return v1>v2;
	} 
	if (op == OP_GE) {
	    return v1>=v2;
	}
	if (op == OP_EQUALS) {
	    return v1==v2;
	}
	if (op == OP_NOTEQUALS) {
	    return v1!=v2;
	} 
	if (op == OP_DEFINED) {
	    return !Double.isNaN(v2);
	}
	return true;
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
                return IO.getInputStream(filename, SeesvOperator.class, true);
            } catch (Exception exc2) {
                for (String prefix : FILE_PREFIXES) {
                    try {
                        return IO.getInputStream(prefix + "/" + filename,
						 SeesvOperator.class,true);
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
        getColumnIndex(null, indices, s, null);

        return indices.get(0);
    }

    /**
     *
     * @param tok _more_
     *
     * @return _more_
     */
    private boolean isLastIndex(String tok) {
        return tok.startsWith("_last") && tok.endsWith("_");
    }

    /**
     *
     * @param tok _more_
     * @param last _more_
     *
     * @return _more_
     */
    private int getLastIndex(String tok, int last) {
        if (tok.equals("_last_")) {
            return last;
        }

        return -1;
    }

    public int getColumnIndex(TextReader ctx, String tok,boolean...optional) {
        checkColumns();
	if(debug)
	    debug("\tgetColumnIndex:" + tok);
        Integer iv = columnMap.get(tok);
        //      System.err.println("\t\tiv-a:" + iv);
	if(tok.startsWith(">")) {
	    int index = Integer.parseInt(tok.substring(1))+1;
	    return columnNames.size()-index;


	}
        if (iv == null) {
            String tmp = ctx.getFieldAlias(tok);
            if (tmp != null) {
                iv = columnMap.get(tmp);
            }
        }
        if (iv == null) {
            String colId = Utils.makeID(tok, false);
            iv = columnMap.get(colId);
        }

	if(iv==null) {
	    if(optional.length>0 && optional[0]) return -1;
	    if (!Utils.equals("true",ctx.getProperty("goeasy")+"")) {
		throw new SeesvException(this, "Could not find column:"
					 + tok + "\npossible columns: " + Utils.getKeys(columnMap));
	    }
	}

        return (iv != null)
	    ? iv
	    : -1;
    }


    /**
     */
    private void checkColumns() {
        if (columnNames == null) {
            columnNames = new ArrayList<String>();
            columnMap   = new LinkedHashMap<String, Integer>();
            if (header == null) {
                debug("no names or header");

                return;
            }

            for (int i = 0; i < header.size(); i++) {
                String colName =  Utils.getNonNull(header.get(i),"").toString();
                String colId   = Utils.makeID(colName, false);
                columnNames.add(colName);
                colName = colName.trim();
                columnMap.put(colName, i);
                columnMap.put(colId, i);
                columnMap.put(colName.toLowerCase(), i);
                columnMap.put(i + "", i);
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
     * @param seen _more_
     *
     */
    public void getColumnIndex(TextReader ctx, List<Integer> indices,
                               String s, HashSet seen) {

        if (seen == null) {
            seen = new HashSet();
        }
        checkColumns();
	boolean optional = s.startsWith("?");
	if(optional) s = s.substring(1);
	boolean isRegexp = s.startsWith("regex:");
	if(isRegexp) {
            s = s.substring("regex:".length());
	} else {
	    if(!s.equals("*"))
		isRegexp = StringUtil.containsRegExp(s);
	}

        if (isRegexp) {
            //      System.err.println("pattern:" + s+":");
            Pattern p = Pattern.compile(s);
            for (int i = 0; i < header.size(); i++) {
                String  name    = columnNames.get(i);
                String  id      = makeID(header.get(i));
                boolean matches = p.matcher(name).matches();
                if ( !matches) {
                    matches = p.matcher(id).matches();
                }
                if (matches) {
                    //              System.err.println("\tmatch:" + name);
                    indices.add(columnMap.get(name));
                } else {
                    //              System.err.println("\tno match:" + name +" " + id);
                }
            }

            //      System.err.println("Indices:" + indices);
            return;
        }

        //      System.err.println("getColumnIndex:" + s);
        List<String> toks = Utils.splitUpTo(s, "-", 2);
        //      System.err.println("\ttoks:" + toks);
        int start = -1;
        int end   = -1;
        int step  = 1;

        s = s.toLowerCase().trim();

        try {
            if (toks.size() == 0) {
                //No columns
                return;
            } else if (toks.size() == 1) {
                //not now               if(Utils.testAndSet(seen,s)) return;
                start = end = Integer.parseInt(s);

            } else {
                start = Integer.parseInt(toks.get(0));
                String second = toks.get(1);
                int    index  = second.indexOf(":");
                if (index >= 0) {
                    step   = Integer.parseInt(second.substring(index + 1));
                    second = second.substring(0, index);
                }
                //              System.err.println("step:" + step +" second:" + second);
                end = Integer.parseInt(second);
            }
        } catch (NumberFormatException exc) {
            if (toks.size() == 1) {
                String tok = toks.get(0);
                if (isLastIndex(tok)) {
                    indices.add(getLastIndex(tok, header.size() - 1));
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


		//check for regexp
                if (tok.indexOf("*")>=0 || tok.indexOf("+")>=0) {
                    for (int i = 0; i < header.size(); i++) {
                        if ( !colsSeen.contains(i)) {
			    String v = header.get(i).toString();
			    String _v = v.toLowerCase();
			    if(v.matches(tok) || _v.matches(tok)) {
				colsSeen.add(i);
				indices.add(i);
			    }
                        }
                    }
		    return;
		}

                Integer iv = getColumnIndex(ctx, tok,optional);
                if (iv != null) {
                    start = end = iv;
                } else {
                    //Not sure whether we should throw an error
                    for (String colName : columnNames) {
                        //                      System.err.println(colName);
                    }
                    //              System.out.println("TOK:" + tok);
                    StringBuilder msg = new StringBuilder("");
                    for (Object key : Utils.getKeys(columnMap)) {
                        String skey = (String) key;
                        //                      System.err.println("KEY:" + skey);
                        if (skey.length() == 0) {
                            continue;
                        }
                        if (skey.matches("^[0-9]+$")) {
                            continue;
                        }
                        if (msg.length() > 0) {
                            msg.append(",");
                        } else {
                            msg.append("\nColumns: ");
                        }
                        msg.append(skey);
                    }
                    //              System.err.println(columnMap);
                    fatal(ctx, "Could not find index:" + tok + msg + "\n");
                }
            } else {
                String tok1 = toks.get(0);
                String tok2 = toks.get(1);
                if (isLastIndex(tok1)) {
                    start = getLastIndex(tok1, header.size() - 1);
                } else {
                    Integer iv = getColumnIndex(ctx, tok1);
                    if (iv != null) {
                        start = iv;
                    }
                }
                if (isLastIndex(tok2)) {
                    end = getLastIndex(tok2, header.size() - 1);
                } else {
                    Integer iv = getColumnIndex(ctx, tok2);
                    if (iv != null) {
                        end = iv;
                    }
                }
                if ((start == -1) || (end == -1)) {
		    for(String key: columnMap.keySet()) {
                        System.err.println("key:" + key);
                    }
                    fatal(ctx, "Could not find indices:" + toks);
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
        if (step <= 0) {
            throw new IllegalArgumentException("Step can't be <=0:" + step);
        }
        if (start >= 0) {
            for (int i = start; i <= end; i += step) {
                //not now               if(Utils.testAndSet(seen,i)) continue;
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
	if(debug)
	    debug("getIndices:" + cols);
        if (cols == null) {
            return null;
        }
        List<Integer> indices = new ArrayList<Integer>();
        HashSet       seen    = new HashSet();
        for (String s : cols) {
            getColumnIndex(ctx, indices, s, seen);
        }
	if(debug)
	    debug("getIndices:" + indices);

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
        getColumnIndex(ctx, indices, idx, new HashSet());
        if (indices.size() == 0) {
            return -1;
            //            throw new IllegalArgumentException("Could not find column index:" + idx);

        }

        return indices.get(0);
    }



    /**
     *
     * @param indices _more_
     * @param row _more_
     *
     * @return _more_
     */
    public Row removeColumns(List<Integer> indices, Row row) {
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



    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Nov 4, '21
     * @author         Enter your name here...    
     */
    public static class Tuple {

	/**  */
	int count = 0;

	/**  */
	double min = 0;

	/**  */
	double max = 0;

	/**  */
	double sum = 0;

	int nanCount =0;

	public double getValue(String op) {
	    if(op.equals(OPERAND_COUNT)) return count;
	    if(op.equals(OPERAND_SUM)) return sum;
	    if(op.equals(OPERAND_MIN)) return min;
	    if(op.equals(OPERAND_MAX)) return max;	    	    
	    if(op.equals(OPERAND_AVERAGE)||op.equals(OPERAND_AVG)) {
		if(count==0) return Double.NaN;
		return sum/count;
	    }
	    throw new IllegalArgumentException("Unknown operator:" + op);
	}

	public void add(double v) {
	    if(Double.isNaN(v)) {
		nanCount++;
		return;
	    }
	    if(count==0) {
		min =v;
		max=v;
	    } else {
		min = Math.min(min,v);
		max = Math.max(max,v);
	    }
	    count++;
	    sum+=v;
	}



	/**
	 *
	 * @return _more_
	 */
	public String toString() {
	    return "cnt:" + count + " min:" + min + " max:" + max
		+ " sum:" + sum;
	}
    }



}
