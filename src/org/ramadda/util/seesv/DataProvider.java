/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util.seesv;

import org.apache.commons.lang3.text.StrTokenizer;
import org.apache.commons.text.StringTokenizer;
import org.json.*;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.NamedInputStream;
import org.ramadda.util.Utils;

import org.ramadda.util.geo.GeoJson;
import org.ramadda.util.geo.KmlUtil;
import org.ramadda.util.sql.Clause;
import org.ramadda.util.sql.SqlUtil;

import org.w3c.dom.*;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.net.URL;

import java.sql.*;


import java.util.ArrayList;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.List;

import java.util.regex.*;

/**
 *
 * @author Jeff McWhirter
 */

@SuppressWarnings("unchecked")
public abstract class DataProvider extends SeesvOperator {

    public static final JsonUtil JU = null;
    boolean debugInput = false;

    TextReader ctx;

    private Seesv seesv;

    public DataProvider() {}

    public DataProvider(TextReader ctx) {this.ctx=ctx;}    

    public void initialize(Seesv seesv, TextReader ctx) throws Exception {
	this.ctx = ctx;
	if (ctx != null) {
	    this.debugInput = ctx.getDebugInput();
	}
        rowCnt = 0;
    }

    /**  */
    private int rowCnt = 0;

    /**
      * @return _more_
     */
    public Row makeRow() {
        Row row = new Row();
        row.setRowCount(rowCnt++);
        return row;
    }

    public void incrRowCnt() {
	rowCnt++;
    }

    /**
     *
     * @param values _more_
      * @return _more_
     */
    public Row makeRow(List values) {
        Row row = new Row(values);
        row.setRowCount(rowCnt++);

        return row;

    }

    public abstract Row readRow() throws Exception;

    public void finish() {}

    /**
     * Class description
     *
     *
     * @version        $version$, Tue, Feb 16, '21
     * @author         Enter your name here...
     */
    public abstract static class BulkDataProvider extends DataProvider {

        private List<Row> rows = new ArrayList<Row>();

        private int index = 0;

        public BulkDataProvider() {
            super();
        }

        public Row readRow() throws Exception {
            if (index >= rows.size()) {
                return null;
            }

            return rows.get(index++);
        }

	int debugCnt=0;
        protected boolean addRow(Row row) {
            rows.add(row);
	    //	    if(debugInput && debugCnt++<10)System.err.println("row:" + row);
            return true;
        }

        protected List<Row> getRows() {
            return rows;
        }

        public void initialize(Seesv seesv, TextReader textReader)
                throws Exception {
            super.initialize(seesv, textReader);
            String s = textReader.convertContents(textReader.readContents());
            tokenize(textReader, s);
        }

        public abstract void tokenize(TextReader ctx, String s)
         throws Exception;

    }

    /**
     * Class description
     *
     *
     * @version        $version$, Tue, Feb 16, '21
     * @author         Enter your name here...
     */
    public static class HtmlDataProvider extends BulkDataProvider {

        private int skip;

        private String pattern;

        private Dictionary<String, String> props;

        public HtmlDataProvider(String sSkip, String htmlPattern,
                                Dictionary<String, String> props) {
            super();
            this.pattern = htmlPattern;
            this.props   = props;
            if ((sSkip != null) && (sSkip.trim().length() > 0)) {
                skip = Integer.parseInt(sSkip.trim());
            } else {
                skip = 0;
            }

        }

        /**
         * Class description
         *
         *
         * @version        $version$, Thu, Nov 4, '21
         * @author         Enter your name here...
         */
        private static class ColInfo {

            /**  */
            int index;

            /**  */
            boolean extractUrls;

            /**  */
            boolean stripTags;

            /**
             *
             *
             * @param index _more_
             * @param extractUrls _more_
             * @param stripTags _more_
             */
            ColInfo(int index, boolean extractUrls, boolean stripTags) {
                this.index       = index;
                this.extractUrls = extractUrls;
                this.stripTags   = stripTags;
            }
        }

        public void tokenize(TextReader ctx, String s) throws Exception {

            int count = Utils.getProperty(props, "numTables", 1);
            if ((pattern == null) || (pattern.trim().length() == 0)) {
                pattern = props.get("pattern");
            }
            String skipAttr = props.get("skipAttr");
            boolean removeEntity = Utils.equals(props.get("removeEntity"),
                                       "true");
            boolean extractUrls = Utils.getProperty(props, "extractUrls",
                                      false);
            boolean stripTags = Utils.getProperty(props, "stripTags", true);
            List<ColInfo> cols = new ArrayList<ColInfo>();
            Hashtable<Integer, Object> extractUrlsColumns =
                new Hashtable<Integer, Object>();

            for (int i = 0; i < 50; i++) {
                String prefix = "column" + (i + 1) + ".";
                cols.add(new ColInfo(i, Utils.getProperty(props,
                        prefix + "extractUrls",
                        extractUrls), Utils.getProperty(props,
                            prefix + "stripTags", stripTags)));
            }

            String removePattern =
                Utils.convertPattern(props.get("removePattern"));
            String removePattern2 =
                Utils.convertPattern(props.get("removePattern2"));
            Pattern attrPattern = null;
            if (skipAttr != null) {
                skipAttr    = skipAttr.replaceAll("_quote_", "\"");
                attrPattern = Pattern.compile(skipAttr, Pattern.MULTILINE);
            }
            boolean debug = false;
            //        System.err.println("HTML:" + file);
            //        System.out.println("TABLE:" + s);

            String[] toks;
            if (Utils.stringDefined(pattern)) {
                int idx = s.indexOf(pattern);
                if (idx < 0) {
                    return;
                }
                s = s.substring(idx + pattern.length());
            }

	    s = s.replace("<TABLE","<table").replace("/TABLE>","/table").replace("<TR","<tr").replace("/TR>","/tr>");
            while (true) {
                toks = Utils.tokenizeChunk(s, "<table", "</table");
                if (toks == null) {
                    break;
                }
                String table = toks[0];
                s = toks[1];
                if (skip > 0) {
                    skip--;

                    continue;
                }
                if (debug) {
                    System.out.println("table");
                }
                while (true) {
                    toks = Utils.tokenizeChunk(table, "<tr", "</tr");
                    if (toks == null) {
                        break;
                    }
                    String tr = toks[0];
                    table = toks[1];
                    if (debug) {
                        System.out.println("\trow: " + tr);
                    }
                    Row     row         = makeRow();
                    boolean checkHeader = true;
                    int     colCnt      = 0;
                    while (true) {
                        ColInfo info = (colCnt < cols.size())
                                       ? cols.get(colCnt)
                                       : cols.get(cols.size() - 1);
                        colCnt++;
                        toks = Utils.tokenizePattern(tr, "(?i)(<td|<th)",
                                "(?i)(</td|</th)");
                        if (toks == null) {
                            break;
                        }
                        String td = toks[0].trim();
                        tr = toks[1];
                        String colspan = StringUtil.findPattern(td,
                                             "(?i)colspan *= *\"?([0-9]+)\"?");
                        int inserts = 1;
                        if (colspan != null) {
                            inserts = Integer.parseInt(colspan);
                        }

                        int idx = td.indexOf(">");
                        if ((attrPattern != null) && (idx >= 0)) {
                            String attrs = td.substring(0, idx).toLowerCase();
                            if (attrPattern.matcher(attrs).find()) {
                                System.out.println("skipping:"
                                        + td.replaceAll("\n", " "));

                                continue;
                            }
                            //                        System.out.println("not skipping:" +td );
                        }
                        //              System.err.println("td:" + td);
                        td = td.substring(idx + 1);
                        //              System.err.println("after TD:" + td);
                        if (info.extractUrls) {
                            String url = StringUtil.findPattern(td,
                                             "(?i)href *= *\"([^\"]+)\"");
                            if (url == null) {
                                url = StringUtil.findPattern(td,
                                        "(?i)href *= *\'([^\"]+)\'");
                            }
                            if (url != null) {
                                td = url;
                            }
                        }
                        if (info.stripTags) {
                            td = Utils.stripTags(td);
                        }

                        //              System.err.println("after Strip:" + td);
                        td = td.replaceAll("\n", " ").replaceAll("  +", "");
                        td = HtmlUtils.unescapeHtml3(td);
                        //              System.err.println(td+"  stripped:" + td);
                        if (removeEntity) {
                            td = td.replaceAll("&[^;]+;", "");
                        } else {
                            td = HtmlUtils.unescapeHtml3(td);
                        }
                        if (removePattern != null) {
                            td = td.replaceAll(removePattern, "");
                        }
                        if (removePattern2 != null) {
                            td = td.replaceAll(removePattern2, "");
                        }
                        td = td.replaceAll("&nbsp;", " ");
                        td = td.replaceAll("&quot;", "\"");
                        td = td.replaceAll("&lt;", "<");
                        td = td.replaceAll("&gt;", ">");
                        td = td.replaceAll("\n", " ");
                        while (inserts-- > 0) {
                            row.insert(td.trim());
                        }
                        if (debug) {
                            System.out.println("\t\ttd:" + td);
                        }
                    }
                    checkHeader = false;
                    if (row.size() > 0) {
                        addRow(row);
                    }
                }
                if (--count <= 0) {
                    break;
                }
            }

        }

    }

    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Nov 4, '21
     * @author         Enter your name here...
     */
    public static class Harvester extends BulkDataProvider {

        private String pattern;

        public Harvester(String pattern) {
            this.pattern = pattern;
        }

        public void tokenize(TextReader ctx, String s) throws Exception {
            List<HtmlUtils.Link> links;
	    if(new File(ctx.getInputFile()).exists()) {
		URL url = new URL("https://ramadda.org");
		links = HtmlUtils.extractLinks(url,
					       s,  pattern);
	    } else {
		URL url = new URL(ctx.getInputFile());

		links = HtmlUtils.extractLinks(url, s,  pattern);
	    }
            Row row = makeRow();
            row.add("Label");
            row.add("URL");
            addRow(row);
            for (HtmlUtils.Link link : links) {
                row = makeRow();
                row.add(link.getLabel());
                row.add(link.getUrl().toString());
                addRow(row);
            }
        }

    }

    /**
     * Class description
     *
     *
     * @version        $version$, Tue, Feb 16, '21
     * @author         Enter your name here...
     */
    public static class HtmlPatternDataProvider extends BulkDataProvider {

        private String cols;

        private String start;

        private String end;

        private String pattern;

        public HtmlPatternDataProvider(String cols, String start, String end,
                                       String pattern) {
            super();
            this.cols    = cols;
            this.start   = start;
            this.end     = end;
            this.pattern = pattern;
        }

        public void tokenize(TextReader ctx, String s) throws Exception {
            int numLines = ctx.getMaxRows();
            pattern = Utils.convertPattern(pattern);
            if (cols.length() > 0) {
                addRow(makeRow(Utils.split(cols, ",")));
            }

            //      System.err.println(start);
            if (start.length() != 0) {
                int index = s.indexOf(start);
                if (index >= 0) {
                    s = s.substring(index);
                } else {
                    throw new IllegalArgumentException(
                        "Missing start pattern:" + start);
                }
            }
            //      System.out.println("***** START:"+s);
            if (end.length() != 0) {
                int index = s.indexOf(end);
                if (index >= 0) {
                    s = s.substring(0, index - 1);
                } else {
                    throw new IllegalArgumentException("Missing end pattern:"
                            + end);
                }
            }
            //      System.out.println("***** FINAL:"+s);
            Pattern p   = Pattern.compile(pattern);
            int     cnt = 0;
            while (true) {
                long    t1      = System.currentTimeMillis();
                Matcher matcher = p.matcher(s);
                if ( !matcher.find()) {
                    break;
                }
                Row row = makeRow();
                addRow(row);
                for (int i = 1; i <= matcher.groupCount(); i++) {
                    String tok = matcher.group(i);
                    if (tok == null) {
                        tok = "";
                    }
                    row.add(tok.trim());
                }
                String s2 = s.substring(matcher.end());
                if (s.length() == s2.length()) {
                    break;
                }
                s = s2;
                if ((numLines >= 0) && (getRows().size() >= numLines)) {
                    break;
                }
                long t2 = System.currentTimeMillis();
            }
        }
    }

    /**
     * Class description
     *
     *
     * @version        $version$, Tue, Feb 16, '21
     * @author         Enter your name here...
     */
    public static class JsonDataProvider extends BulkDataProvider {

        private String arrayPath;

        private String objectPath;

        public JsonDataProvider(String arrayPath, String objectPath) {
            super();
            this.arrayPath  = arrayPath;
            this.objectPath = objectPath;
        }

        public void tokenize(TextReader ctx, String s) throws Exception {

	    Row firstRow = ctx.getFirstRow();
	    boolean addingTuple = false;
            boolean    debug = false;
	    debug = debugInput;
	    if(debug) 	System.err.println("Json.tokenize: array path:" + arrayPath +" object path:" + objectPath);
            int        xcnt  = 0;
            JSONArray  array = null;
            JSONObject root  = null;
            if (debug) {
                System.err.println("JSON:" + Utils.clip(s,200,"..."));
            }
            try {
                root = new JSONObject(s);
		if(Misc.equals(arrayPath,"addkey")) {
		    arrayPath="";
		    array = new JSONArray();
		    Iterator<String> keys = root.keys();
		    while (keys.hasNext()) {
			Object o = keys.next();
			String key = o.toString();
			JSONObject obj = root.getJSONObject(key);
			obj.put("key",key);
			array.put(obj);
		    }
		}

                if (array==null && arrayPath != null) {
                    array = JsonUtil.readArray(root, arrayPath);
                }
                if (debug) {
		    //                    System.err.println("array path:" + arrayPath + " a:"   + Utils.clip(array.toString(),200,"..."));
                }
            } catch (Exception exc) {
                if (debug) {
                    System.err.println("error trying to read JSONObject:"
                                       + exc);
                }
            }

            if ((array == null) && (root != null)) {
                array = new JSONArray();
                Iterator<String> keys = root.keys();
                while (keys.hasNext()) {
                    Object o = keys.next();
                    array.put(root.opt(o.toString()));
                }
            }

            if (array == null) {
                try {
                    array = new JSONArray(s);
                } catch (Exception exc) {
                    System.err.println("Error reading array");
                    if (s.length() > 1000) {
                        s = s.substring(0, 999);
                    }
                    System.err.println("JSON:" + s);
                    String msg = "Could not read JSON data";
                    if ( !Utils.stringDefined(arrayPath)) {
                        msg += " No array path specified";
                    }
                    msg += "\nError:" + exc;

                    throw new IllegalArgumentException(msg);
                }
            }

            List<String> objectPathList = null;
            if (Utils.stringDefined(objectPath)) {
                objectPathList = Utils.split(objectPath, ",", true, true);
		if(debug) System.err.println("object path list:" + objectPathList);
            }
            List<String> names = null;
            for (int arrayIdx = 0; arrayIdx < array.length(); arrayIdx++) {
                Hashtable       primary   = new Hashtable();
                List<Hashtable> secondary = new ArrayList<Hashtable>();
                List<String>    arrayKeys = new ArrayList<String>();
		//		System.err.println("ROW:"+ arrayIdx);
                if (objectPathList != null) {
                    JSONObject jrow = array.getJSONObject(arrayIdx);
		    if(debug && arrayIdx<5) System.err.println("ROW:" + jrow);
                    for (String tok : objectPathList) {
			//			System.err.println("\ttok:" + tok);
                        if (tok.equals("*")) {
                            primary.putAll(JU.getHashtable(jrow, true, arrayKeys));
                        } else if (tok.endsWith("[]")) {
                            JSONArray a = JsonUtil.readArray(jrow,
                                              tok.substring(0,
                                                  tok.length() - 2));
                            for (int j = 0; j < a.length(); j++) {
                                secondary.add(
                                    JsonUtil.getHashtable(
                                        a.getJSONObject(j), true, arrayKeys));
                            }
                        } else {
                            try {
                                Object o = JsonUtil.readObject(jrow, tok);
                                if (o != null) {
                                    primary.putAll(JsonUtil.getHashtable(o,
                                            false, arrayKeys));
                                }
                            } catch (Exception exc) {
				try {
				    Object o = JsonUtil.readArray(jrow, tok);
				    if (o != null) {
					primary.putAll(JsonUtil.getHashtable(o,
									     true, arrayKeys));
				    }
				} catch (Exception exc2) {
				    try {
					primary.put(tok,jrow.getString(tok));
				    } catch(Exception exc3) {
					try {
					    primary.put(tok,jrow.getDouble(tok));
					} catch(Exception exc4) {
					    primary.put(tok,jrow.getBoolean(tok));
					}
				    }
				}
                            }
                        }
                    }
                } else {
                    try {
			JSONArray tuple = array.getJSONArray(arrayIdx);
			//We have a fixed header
			if(firstRow!=null) {
			    addingTuple=true;
			    Row row = makeRow();
			    addRow(row);
                            for (int tupleIdx = 0;
                                    tupleIdx < tuple.length(); tupleIdx++) {
				row.add(JU.getString(tuple,tupleIdx));
			    }
			    continue;
			}
                        primary.putAll(JU.getHashtable(tuple, true,  arrayKeys));
			if(arrayIdx<3) System.err.println("PRIMARY1:" + primary);
			//			if(debug && arrayIdx<5) System.err.println("PRIMARY1:" + primary);
                    } catch (Exception exc) {
                        try {
                            primary.putAll(
					   JU.getHashtable(
                                    array.getJSONObject(arrayIdx), true,
                                    arrayKeys));
			    if(debug && arrayIdx<2) System.err.println("PRIMARY2:" + primary);
                        } catch (Exception exc2) {
                            //Maybe it is an array of strings
                            for (int arrayIdx2 = 0;
                                    arrayIdx2 < array.length(); arrayIdx2++) {
                                String v = array.optString(arrayIdx2);
                                if (v == null) {
                                    continue;
                                }
                                Row row = makeRow();
                                row.add(v);
                                addRow(row);
                            }
                            return;
                        }
                    }
		}

                if (secondary.size() == 0) {
                    secondary.add(primary);
                } else {
                    for (Hashtable h : secondary) {
                        h.putAll(primary);
                    }
		}

                if (names == null) {
                    names = new ArrayList<String>();
                    Row row = makeRow();
                    addRow(row);
		    for (Enumeration keys = secondary.get(0).keys();
			 keys.hasMoreElements(); ) {
			String key = (String) keys.nextElement();
			if(!names.contains(key)) 
			    names.add(key);
		    }
                    if (arrayKeys.size() > 0) {
			for(String key: arrayKeys) {
			    if(!names.contains(key)) 
				names.addAll(arrayKeys);
			}
			if(debug) System.err.println("names 1:" + names);
                    } 
		    //else {
		    if(debug) System.err.println("names 2:" + names);
		    //                    }
                    //              names = (List<String>) Utils.sort(names);
                    for (String name : names) {
                        row.add(name);
                    }
		    //		    System.err.println("names:" + names);
		}
                /*
                  JSONArray fields = root.optJSONArray("fields");
                  if(fields!=null) {
                  for(int k=0;k<fields.length();k++)
                  row.add(fields.getString(k));
                  } else  {
                  for (int k = 0; k < jarray.length(); k++) {
                  row.add("Index " + k);
                  }
                ***/

                for (Hashtable h : secondary) {
                    Row row = makeRow();
                    addRow(row);
                    for (String name : names) {
                        Object value = h.get(name);
                        if (value == null) {
                            value = "NULL";
                        }
			//			System.err.println("\tNAME:" + name +" value:" + value);
                        row.add(value);
                    }
                }
            }

	    if (debug) {
		List<Row> rows = getRows();
		for(int i=0;i<rows.size()&& i<5;i++) {
		    System.err.println("ROW:" + rows.get(i));
		}
	    }
	}

    }

    public static class JsonJoinDataProvider extends BulkDataProvider {

        private String arrayPaths;

        private String key;

	private String pattern;

	private String replace;

	private String missing; 

        public JsonJoinDataProvider(String arrayPaths, String key, String pattern,
				    String replace,String missing) {
            super();
            this.arrayPaths  = arrayPaths;
	    this.key = key;
	    this.pattern = pattern;
	    this.replace =replace;
	    this.missing = missing;
        }

        public void tokenize(TextReader ctx, String s) throws Exception {

            boolean    debug = false;
	    debug = debugInput;
            JSONObject root  = new JSONObject(s);
	    List<List<String>> results = JsonUtil.joinArrays(root,
							      Utils.split(arrayPaths,",",true,true),
							      Utils.split(key,",",true,true),
							      pattern,replace,missing);
	    for(List<String> values: results) {
		Row row = new Row(values);
		addRow(row);
	    }
	}
    }

    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Nov 4, '21
     * @author         Enter your name here...
     */
    public static class GeoJsonDataProvider extends DataProvider {

        /**  */
        boolean addPolygon;

        /**  */
        GeoJson.Iterator iterator;

        public GeoJsonDataProvider(boolean addPolygon) {
            super();
            this.addPolygon = addPolygon;
        }

        /**
         *
         * @param seesv _more_
         * @param ctx _more_
         *
         * @throws Exception _more_
         */
        public void initialize(Seesv seesv, TextReader ctx)
                throws Exception {
            super.initialize(seesv, ctx);
            iterator = GeoJson.makeIterator(ctx.getInputStream(), null,
                                            addPolygon);
        }

        /**
          * @return _more_
         *
         * @throws Exception _more_
         */
        public Row readRow() throws Exception {
            List<String> values = iterator.next();
            if (values == null) {
                return null;
            }

            return new Row(values);
        }

    }

    /**
     * Class description
     *
     *
     * @version        $version$, Sun, Feb 21, '21
     * @author         Enter your name here...
     */
    public static class SqlDataProvider extends DataProvider {

        private String db;

        private String table;

        /**  */
        private String columns;

        /**  */
        private String where;

        private Dictionary<String, String> props;

        private Connection connection;

        private Statement statement;

        private SqlUtil.Iterator iter;

        private int columnCount;

        private Row headerRow;

        private int rowCnt = 0;

        private int maxRows;

        public SqlDataProvider(String db, String table, String columns,
                               String where,
                               Dictionary<String, String> props) {
            this.db      = db.trim();
            this.table   = SqlUtil.sanitize(table.trim());
            this.columns = SqlUtil.sanitize(columns.trim());
            this.where   = where.trim();
            this.props   = props;
        }

        public void initialize(Seesv seesv, TextReader ctx)
                throws Exception {
            super.initialize(seesv, ctx);
            this.connection = seesv.getDbConnection(ctx, this, props, db,
                    table);
            List<Clause> clauses = new ArrayList<Clause>();
            String       join    = (String) props.get("join");
            if (join != null) {
                List<String> toks = Utils.split(join, ",");
                if (toks.size() != 2) {
                    throw new IllegalArgumentException("Bad join:" + join);
                }
                clauses.add(Clause.join(toks.get(0), toks.get(1)));
            }
            String what = columns;
            if ((what == null) || (what.length() == 0)) {
                what = "*";
            }
            if (Utils.stringDefined(where)) {
                for (String tok : where.split(";")) {
                    List<String> toks = StringUtil.splitUpTo(tok, ":", 3);
                    if (toks.size() != 3) {
                        throw new IllegalArgumentException("Bad where value:"
                                + tok);
                    }
                    String col   = toks.get(0);
                    String expr  = toks.get(1).toLowerCase().trim();
                    String value = toks.get(2);
                    if (expr.equals("like")
                            && !(value.startsWith("%")
                                 || value.endsWith("%"))) {
                        value = "%" + value + "%";
                    }
                    clauses.add(new Clause(col, expr, value));
                }
            }
            System.err.println(clauses);
            Clause clause = (clauses.size() > 0)
                            ? Clause.and(clauses)
                            : null;
            this.statement = SqlUtil.select(connection, what,
                                            Utils.makeListFromValues(table), clause,
                                            "");
            this.iter = SqlUtil.getIterator(this.statement);

            List              values  = new ArrayList();
            ResultSet         results = this.statement.getResultSet();
            ResultSetMetaData rsmd    = results.getMetaData();
            this.columnCount = rsmd.getColumnCount();
            //      System.err.println("header cnt:" + this.columnCount);
            for (int i = 0; i < this.columnCount; i++) {
                values.add(rsmd.getColumnName(i + 1).toLowerCase());
            }
            headerRow = makeRow(values);
            maxRows   = ctx.getMaxRows();
        }

        public Row readRow() throws Exception {
            rowCnt++;
            if (rowCnt == 1) {
                return headerRow;
            }
            if ((maxRows >= 0) && (rowCnt > maxRows)) {
                return null;
            }
            ResultSet results = this.iter.getNext();
            if (results == null) {
                return null;
            }
            //            ResultSetMetaData rsmd    = results.getMetaData();
            List values = new ArrayList();
            //      System.err.println("row:" + rowCnt);
            for (int i = 0; i < this.columnCount; i++) {
                String v = results.getString(i + 1);
                if (v == null) {
                    v = "";
                }
                //              System.err.println("\tcol:" + i +" v:" +v);
                values.add(v);
                //                values.add(results.getString(i + 1) +" name:" + rsmd.getColumnName(i+1));
            }

            return makeRow(values);
        }

        @Override
        public void finish() {
            super.finish();
            SqlUtil.closeAndReleaseConnection(this.statement);
            this.statement = null;
        }

    }

    /**
     * Class description
     *
     *
     * @version        $version$, Tue, Feb 16, '21
     * @author         Enter your name here...
     */
    public static class XmlDataProvider extends BulkDataProvider {

        private String arrayPath;

        private String objectPath;

        public XmlDataProvider(String arrayPath) {
            super();
            this.arrayPath = arrayPath;
        }

        public void tokenize(TextReader ctx, String s) throws Exception {

            Element root   = XmlUtil.getRoot(s);
            Row     header = makeRow();
            addRow(header);
            List<Element> nodes =
                (List<Element>) Utils.findDescendantsFromPath(root,
                    arrayPath);
            int cnt    = 0;
            int colCnt = 0;
            Hashtable<String, Integer> colMap = new Hashtable<String,
                                                    Integer>();
            //            System.err.println("NODES:" + nodes.size());
            for (Element parent : nodes) {
                NodeList children = XmlUtil.getElements(parent);

                if (children.getLength() == 0) {
                    //use attrs
                    NamedNodeMap nnm = parent.getAttributes();
                    if (nnm == null) {
                        continue;
                    }
                    for (int i = 0; i < nnm.getLength(); i++) {
                        Attr    attr = (Attr) nnm.item(i);
                        String  name = attr.getNodeName();
                        Integer idx  = colMap.get(name);
                        if (idx == null) {
                            colMap.put(name, colCnt++);
                            header.add(name);
                        }
                    }

                    continue;
                }
                for (int i = 0; i < children.getLength(); i++) {
                    Element  child     = (Element) children.item(i);
                    NodeList gchildren = XmlUtil.getElements(child);
                    //go one level down
                    String tag = child.getTagName();
                    if (gchildren.getLength() > 0) {
                        for (int gi = 0; gi < gchildren.getLength(); gi++) {
                            Element gchild = (Element) gchildren.item(gi);
                            String  gtag   = tag + "." + gchild.getTagName();
                            Integer idx    = colMap.get(gtag);
                            if (idx == null) {
                                colMap.put(gtag, colCnt++);
                                header.add(gchild.getTagName());
                            }
                        }
                    } else {
                        Integer idx = colMap.get(tag);
                        if (idx == null) {
                            colMap.put(child.getTagName(), colCnt++);
                            header.add(child.getTagName());
                        }
                    }
                }
            }

            for (Element parent : nodes) {
                NodeList children = XmlUtil.getElements(parent);
                Row      row      = makeRow();
                addRow(row);
                for (int i = 0; i < colCnt; i++) {
                    row.add("");
                }
                if (children.getLength() == 0) {
                    NamedNodeMap nnm = parent.getAttributes();
                    if (nnm == null) {
                        continue;
                    }
                    for (int i = 0; i < nnm.getLength(); i++) {
                        Attr    attr  = (Attr) nnm.item(i);
                        String  name  = attr.getNodeName();
                        String  value = attr.getNodeValue();
                        Integer idx   = colMap.get(name);
                        row.set(idx, value);
                    }
                    continue;
                }

                for (int i = 0; i < children.getLength(); i++) {
                    Element  child     = (Element) children.item(i);
                    NodeList gchildren = XmlUtil.getElements(child);
                    //go one level down
                    String tag = child.getTagName();
                    if (gchildren.getLength() > 0) {
                        for (int gi = 0; gi < gchildren.getLength(); gi++) {
                            Element gchild = (Element) gchildren.item(gi);
                            String  gtag   = tag + "." + gchild.getTagName();
                            Integer idx    = colMap.get(gtag);
                            String  text   = XmlUtil.getChildText(gchild);
                            row.set(idx, text);
                        }
                    } else {
                        Integer idx  = colMap.get(child.getTagName());
                        String  text = XmlUtil.getChildText(child);
                        row.set(idx, text);
                    }
                }
                cnt++;
            }

        }

    }

    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Nov 4, '21
     * @author         Enter your name here...
     */
    public static class KmlDataProvider extends BulkDataProvider {

        public KmlDataProvider() {
            super();
        }

        /**
         *
         * @param seesv _more_
         * @param textReader _more_
         *
         * @throws Exception _more_
         */
        @Override
        public void initialize(Seesv seesv, TextReader textReader)
                throws Exception {
            super.initialize(seesv, textReader);
            List<IO.Path> files = seesv.getInputFiles();
            if (files.size() == 0) {
                return;
            }
            IO.Path  path = files.get(0);
            Element root = KmlUtil.readKml(path.getPath(), textReader.getInputStream());
            read(root);
        }

        /**
         *
         * @param ctx _more_
         * @param s _more_
         *
         * @throws Exception _more_
         */
        public void tokenize(TextReader ctx, String s) throws Exception {}

        private void read(Element root) throws Exception {
            List<Element> placemarks = KmlUtil.findPlacemarks(root);
            List<Element> lookAts =
                (List<Element>) XmlUtil.findDescendants(root,
                    KmlUtil.TAG_LOOKAT);
            if (lookAts.size() > placemarks.size()) {
                readLookAts(lookAts);
            } else if (placemarks.size() > 0) {
                readPlacemarks(placemarks);
            } else {
                List<Element> nodes =
                    (List<Element>) XmlUtil.findDescendants(root,
                        KmlUtil.TAG_GROUNDOVERLAY);
                if (nodes.size() > 0) {
                    readGroundOverlays(nodes);
                }
            }
        }

        /**
         *
         * @param lookAts _more_
         *
         * @throws Exception _more_
         */
        private void readLookAts(List<Element> lookAts) throws Exception {
            /*
              <LookAt>
              <longitude>-125.3755150766427</longitude>
              <latitude>45.14857104780791</latitude>
              <altitude>0</altitude>
              <heading>11.0763894632395</heading>
              <tilt>82.24426129889574</tilt>
              <range>7143.488623627887</range>
              <altitudeMode>relativeToGround</altitudeMode>
              <gx:altitudeMode>relativeToSeaFloor</gx:altitudeMode>
              </LookAt>         */
            Row header = makeRow();
            header.add("Latitude", "Longitude", "Elevation", "Heading",
                       "Tilt", "Range");
            addRow(header);
            for (Element lookAt : lookAts) {
                Row row = makeRow();
                addRow(row);
                row.add(XmlUtil.getGrandChildText(lookAt, "latitude", "NaN"));
                row.add(XmlUtil.getGrandChildText(lookAt, "longitude",
                        "NaN"));
                row.add(XmlUtil.getGrandChildText(lookAt, "altitude", "NaN"));
                row.add(XmlUtil.getGrandChildText(lookAt, "heading", "NaN"));
                row.add(XmlUtil.getGrandChildText(lookAt, "tilt", "NaN"));
            }
        }

        /**
         *
         * @param nodes _more_
         *
         * @throws Exception _more_
         */
        private void readGroundOverlays(List<Element> nodes)
                throws Exception {
            /*
            <GroundOverlay>
            <name>colorado</name>
            <Icon>
            <href>colorado.png</href>
            </Icon><LatLonBox>
            <north>41.406131744384766</north>
            <south>36.645328521728516</south>
            <east>-102.614990234375</east>
            <west>-108.80926513671875</west>
            </LatLonBox>
            </GroundOverlay>

             */
            Row header = makeRow();
            header.add("Name", "Image", "Latitude", "Longitude", "North",
                       "South", "East", "West");
            addRow(header);
            for (Element node : nodes) {
                Row row = makeRow();
                addRow(row);
                row.add(XmlUtil.getGrandChildText(node, "name", ""));
                Element icon = XmlUtil.findChild(node, "Icon");
                if (icon != null) {
                    row.add(XmlUtil.getGrandChildText(icon, "href", ""));
                } else {
                    row.add("");
                }
                Element llbox = XmlUtil.findChild(node, "LatLonBox");
                if (llbox == null) {
                    row.add("NaN", "NaN", "NaN", "NaN", "NaN", "NaN");
                    continue;
                }
                double north =
                    Double.parseDouble(XmlUtil.getGrandChildText(llbox,
                        "north", "NaN"));
                double south =
                    Double.parseDouble(XmlUtil.getGrandChildText(llbox,
                        "south", "NaN"));
                double east =
                    Double.parseDouble(XmlUtil.getGrandChildText(llbox,
                        "east", "NaN"));
                double west =
                    Double.parseDouble(XmlUtil.getGrandChildText(llbox,
                        "west", "NaN"));
                double lat = south + (north - south) / 2;
                double lon = west + (east - west) / 2;
                row.add("" + lat);
                row.add("" + lon);
                row.add("" + north);
                row.add("" + south);
                row.add("" + east);
                row.add("" + west);
            }
        }

        /**
         *
         * @param placemarks _more_
         *
         * @throws Exception _more_
         */
        private void readPlacemarks(List<Element> placemarks)
                throws Exception {
            Row header = makeRow();
            header.add("Name", "Description", "Latitude", "Longitude",
                       "Elevation");
            addRow(header);
            for (Element placemark : placemarks) {
                Row row = makeRow();
                addRow(row);
                row.add(XmlUtil.getGrandChildText(placemark, "name", ""));
                row.add(XmlUtil.getGrandChildText(placemark, "description",
                        ""));
                List<Element> coords =
                    (List<Element>) XmlUtil.findDescendants(placemark,
                        "coordinates");
                if (coords.size() == 0) {
                    row.add("NaN", "NaN", "NaN");
                } else {
                    String coordsText = XmlUtil.getChildText(coords.get(0));
                    double[][] pts = KmlUtil.parseCoordinates(coordsText, 1);
                    row.add(pts[0][0] + "", pts[1][0] + "", pts[2][0] + "");
                }
            }
        }
    }

    /**
     * Class description
     *
     *
     * @version        $version$, Tue, Feb 16, '21
     * @author         Enter your name here...
     */
    public static class PatternDataProvider extends BulkDataProvider {

        private List<String> header;

        private String pattern;

        private String objectPath;

        public PatternDataProvider(List<String> header, String pattern) {
            super();
            this.header  = header;
            this.pattern = Utils.convertPattern(pattern);
        }

        public void tokenize(TextReader ctx, String s) throws Exception {
            addRow(makeRow(header));
            if (pattern.length() == 0) {
                return;
            }
            Pattern p = Pattern.compile(pattern);
            while (true) {
                Matcher matcher = p.matcher(s);
                if ( !matcher.find()) {
                    break;
                }
                Row row = makeRow();
                addRow(row);
                for (int i = 1; i <= matcher.groupCount(); i++) {
                    row.add(matcher.group(i));
                }
                String s2 = s.substring(matcher.end());
                if (s.length() == s2.length()) {
                    break;
                }
                s = s2;
            }
        }
    }

    /**
     * Class description
     *
     *
     * @version        $version$, Tue, Feb 16, '21
     * @author         Enter your name here...
     */
    public static class Pattern2DataProvider extends BulkDataProvider {

        String header;

        String chunkPattern;

        String tokenPattern;

        public Pattern2DataProvider(String header, String chunkPattern,
                                    String tokenPattern) {
            super();
            this.header       = header;
            this.chunkPattern = Utils.convertPattern(chunkPattern);
            this.tokenPattern = Utils.convertPattern(tokenPattern);
        }

        public void tokenize(TextReader ctx, String s) throws Exception {
            Row headerRow = makeRow();
            addRow(headerRow);
            for (String tok : Utils.split(header, ",")) {
                headerRow.add(tok);
            }
            Pattern p1 = Pattern.compile(chunkPattern);
            Pattern p2 = Pattern.compile(tokenPattern);
            while (true) {
                Matcher m1 = p1.matcher(s);
                if ( !m1.find()) {
                    //              System.err.println(" no chunk match");
                    break;
                }
                String s2 = s.substring(m1.end());
                if (s.length() == s2.length()) {
                    break;
                }
                s = s2;
                //              System.err.println("REMAINDER:" + s);
                for (int i1 = 1; i1 <= m1.groupCount(); i1++) {
                    String chunk = m1.group(i1).trim();
                    int    cnt   = 1;
                    while (true) {
                        Matcher m2 = p2.matcher(chunk);
                        if ( !m2.find()) {
                            break;
                        }
                        Row row = null;
                        if (cnt < getRows().size()) {
                            row = getRows().get(cnt);
                        } else {
                            row = makeRow();
                            addRow(row);
                        }
                        cnt++;
                        for (int i2 = 1; i2 <= m2.groupCount(); i2++) {
                            String tok = m2.group(i2).trim();
                            //                      System.err.println("\ttok:" + tok);
                            row.add(tok);
                        }
                        String c2 = chunk.substring(m2.end());
                        if (chunk.length() == c2.length()) {
                            break;
                        }
                        chunk = c2;
                    }
                }
            }
        }
    }

    /**
     * Class description
     *
     *
     * @version        $version$, Tue, Feb 16, '21
     * @author         Enter your name here...
     */

    public static class PatternExtractDataProvider extends BulkDataProvider {

        String header;

        String tokenPattern;

        public PatternExtractDataProvider(String header,
                                          String tokenPattern) {
            super();
            this.header       = header;
            this.tokenPattern = Utils.convertPattern(tokenPattern);
        }

        public void tokenize(TextReader ctx, String s) throws Exception {
            Row headerRow = makeRow();
            addRow(headerRow);
            for (String tok : Utils.split(header, ",")) {
                headerRow.add(tok);
            }
            if (ctx.getDebug()) {
                ctx.printDebug("-text3",
                               "Parsing text input\n\tpattern:"
                               + tokenPattern);
            }
            Pattern p1 = Pattern.compile(tokenPattern);
            //      System.err.println("pattern:" + tokenPattern);
            int remLength = -1;
            while (true) {
                Matcher m1 = p1.matcher(s);
                if ( !m1.find()) {
                    ctx.printDebug("-text3", "no match");

                    break;
                }
                //              System.err.println("match");
                String s2 = s.substring(m1.end());
                if (s.length() == s2.length()) {
                    break;
                }
                s = s2;
                if (ctx.getDebug()) {
                    ctx.printDebug("-text3",
                                   "match group count:" + m1.groupCount());
                }
                //              System.err.println("REMAINDER:" + s.length());
                if ((remLength > 0) && (remLength == s.length())) {
                    throw new IllegalArgumentException("Bad pattern:"
                            + tokenPattern + " groupCount:" + m1.groupCount()
                            + " " + ((m1.groupCount() > 1)
                                     ? ("group:" + m1.group(1))
                                     : "") + " remainder is the same as before");
                }
                remLength = s.length();
                Row row = makeRow();
                addRow(row);
                for (int i = 1; i <= m1.groupCount(); i++) {
                    String tok = m1.group(i).trim();
                    row.add(tok);
                }
            }
            if (ctx.getDebug()) {
                ctx.printDebug("\tdone parsing input #rows:"
                               + getRows().size());
            }
        }

    }

    /**
     * Class description
     *
     *
     * @version        $version$, Tue, Feb 16, '21
     * @author         Enter your name here...
     */
    public static class TextDataProvider extends BulkDataProvider {

        String header;

        String chunkPattern;

        String tokenPattern;

        public TextDataProvider(String header, String chunkPattern,
                                String tokenPattern) {
            super();
            this.header       = header;
            this.chunkPattern = Utils.convertPattern(chunkPattern);
            this.tokenPattern = Utils.convertPattern(tokenPattern);
        }

        public void tokenize(TextReader ctx, String s) throws Exception {
            Row headerRow = makeRow();
            addRow(headerRow);
            for (String tok : Utils.split(header, ",")) {
                headerRow.add(tok);
            }
            try {
                //              System.err.println("pattern:" + chunkPattern);
                Pattern p1 = Utils.stringDefined(chunkPattern)?Pattern.compile(chunkPattern, Pattern.DOTALL):null;
                Pattern p2 = Pattern.compile(tokenPattern, Pattern.DOTALL);
		System.err.println("pattern:" + tokenPattern);
                while (true) {
		    String chunk;
                    if(p1!=null) {
			Matcher m1 = p1.matcher(s);
			if ( !m1.find()) {
                        //                      System.err.println("no match");
			    break;
			}
			String s2 = s.substring(m1.end());
			if (s.length() == s2.length()) {
			    break;
			}
			s = s2;
			if (m1.groupCount() > 2) {
			    throw new IllegalArgumentException(
							       "There should only be one sub-pattern in the chunk");
			}
			chunk = m1.group(1).trim();
		    } else {
			chunk=s;
		    }
                    Matcher m2 = p2.matcher(chunk);
                    if ( !m2.find()) {
			System.err.println("No match");
                        break;
                    }
		    if(p1==null) {
			s = s.substring(m2.end());
			System.err.println("rest:" + Utils.clip(s.replace("\n"," "),400,""));

		    }
                    Row row = makeRow();
                    addRow(row);
                    for (int i2 = 1; i2 <= m2.groupCount(); i2++) {
                        String tok = m2.group(i2);
                        if (tok == null) {
                            tok = "";
                        }
                        //                      System.err.println("\ttok:" + tok);
                        row.add(tok);
                    }
                }
            } catch (Exception exc) {
                System.err.println("error: pattern:\"" + chunkPattern
                                   + "\"  " + exc);
                exc.printStackTrace();

                throw exc;
            }
        }
    }

    /**
     * Class description
     *
     *
     * @version        $version$, Tue, Feb 16, '21
     * @author         Enter your name here...
     */
    public static class CsvDataProvider extends DataProvider {

        int cnt = 0;

        int rawLines = 0;

        int rowCnt = 0;

        boolean deHeader = false;

        /**  */
        private StringTokenizer tokenizer;

	private boolean doingSpaces = false;

	private boolean guessDelimiter = false;	
	private List<String> delimiters;

        public CsvDataProvider(TextReader ctx, int rawLines) {
	    super(ctx);
            if (ctx != null) {
                this.deHeader = Misc.equals("true",
                                            ctx.getProperty("deheader"));
            }
            this.rawLines = rawLines;
        }

        /**
         *
         * @param ctx _more_
         *  @return _more_
         */
        private StringTokenizer getTokenizer(TextReader ctx) {
            if (tokenizer == null) {
                tokenizer = StringTokenizer.getCSVInstance();
                tokenizer.setEmptyTokenAsNull(true);
		delimiters=ctx.getDelimiters();
                if ( !ctx.getDelimiter().equals(",")) {
		    guessDelimiter = ctx.getDelimiterGuess();
		    String delim = ctx.getDelimiter();
		    if(delim.equals("_spaces_")) {
			delim = " ";
			doingSpaces = true;
		    }
                    tokenizer.setDelimiterString(delim);
                }
            }

            return tokenizer;
        }

        public void initialize(Seesv seesv, TextReader ctx)
                throws Exception {
            super.initialize(seesv, ctx);
        }

	private int lineCnt=0;

        public Row readRow() throws Exception {
            while (true) {
                String line = ctx.readLine();
		if(debugInput) {
		    if(lineCnt++<5)
			System.err.println("LINE:" + line);
		}
                if (line == null) {
                    return null;
                }
		line =  ctx.convertContents(line);

                if (rawLines > 0) {
                    ctx.getWriter().println(line);
                    rawLines--;
                    continue;
                }
                if (ctx.getVerbose()) {
                    if (((cnt) % 10000) == 0) {
                        System.err.println("lines: " + cnt + " LINE:" + line);
                    }
                }
                if (deHeader) {
                    if (line.startsWith("#fields=")) {
                        line     = line.substring("#fields=".length());
                        line     = line.replaceAll("\\[.*?\\]", "");
                        deHeader = false;
                    }
                }

                if ( !ctx.lineOk(line)) {
		    //		    System.err.println("NOT OK LINE:" + line);
                    continue;
                }
		//		System.err.println("LINE:" + line);

                rowCnt++;
                if (rowCnt <= ctx.getSkipLines()) {
                    ctx.addHeaderLine(line);
                    continue;
                }
                List<Integer> widths = ctx.getWidths();
                if ((widths == null) && (ctx.getDelimiter() == null)) {
                    String delimiter = ",";
                    //Check for the bad separator
                    int i1 = line.indexOf(",");
                    int i2 = line.indexOf("|");
                    if ((i2 >= 0) && ((i1 < 0) || (i2 < i1))) {
                        delimiter = "|";
                    }
                    //              System.err.println("Seesv.delimiter is null new one is:" + delimiter);
                    ctx.setDelimiter(delimiter);
                }
                if (line.length() == 0) {
                    //For not don't do this as a zero length line might be valid
                    //                    continue;
                }
                if (widths != null) {
                    return makeRow(Utils.tokenizeColumns(line, widths));
                } else if (ctx.getSplitOnSpaces()) {
                    return makeRow(Utils.split(line, " ", true, true));
                } else {
		    StringTokenizer tokenizer = getTokenizer(ctx);
		    if(doingSpaces) {
			line = line.replaceAll("  *"," ").trim();
		    } else if(delimiters!=null) {
			int minIndex=-1;
			String minDelimiter=",";
			for(String delimiter: delimiters) {
			    int index;
			    if(delimiter.equals("spaces"))
				index = line.indexOf(" ");
			    else
				index = line.indexOf(delimiter);
			    if(index<0) continue;
			    if(minIndex<0 || index<minIndex) {
				minIndex = index;
				minDelimiter=delimiter;
			    }
			}
			if(minDelimiter.equals("spaces")) {
			    line = line.replaceAll("  *"," ").trim();
			    minDelimiter=" ";
			}
			tokenizer.setDelimiterString(minDelimiter);
		    } else if(guessDelimiter) {
			int tabIndex = line.indexOf("\t");
			int commaIndex = line.indexOf(",");
			String delim=",";
			if(tabIndex>=0 && (commaIndex<0 || tabIndex<commaIndex)) {
			    delim="\t";
			}
			tokenizer.setDelimiterString(delim);
			guessDelimiter = false;
			ctx.setDelimiterGuess(false);
		    }

                    return makeRow(Utils.tokenizeColumns(line,tokenizer));
                }
            }
        }

    }

    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Nov 4, '21
     * @author         Enter your name here...
     */
    public static class Lines extends DataProvider {

        /**  */
        boolean didFirst = false;

        public Lines() {}

        public Row readRow() throws Exception {
            Row row = makeRow();
            if ( !didFirst) {
                didFirst = true;
                row.add("line");
                return row;
            }
	    String line=null;
	    while(true) {
		line = ctx.readLine();
		if (line == null) {
		    return null;
		}
		if (ctx.lineOk(line)) {
		    break;
		}
	    }
            row.add(line);
            return row;
        }

    }

    /**
     * Class description
     *
     *
     * @version        $version$, Wed, Apr 13, '22
     * @author         Enter your name here...
     */
    public static class Synthetic extends DataProvider {

        /**  */
        TextReader ctx;

        /**  */
        List<String> header;

        /**  */
        List<Value> values;

        /**  */
        int rows = 10;

        /**  */
        int count = 0;

        public Synthetic(String header, String values, int rows) {
            this.header = Utils.split(header, ",", true, true);
	    this.values = new ArrayList<Value>();
	    for(String  v: Utils.split(values, ",", true, true)) {
		this.values.add(new Value(v));
	    }
            this.rows   = rows;
	    while(this.values.size()<this.header.size()) this.values.add(new Value(""));
        }

	private static class Value {
	    String value;
	    double from = Double.NaN;
	    double to = Double.NaN;	
	    boolean random = false;
	    boolean sine=false;
	    double amplitude = 10.0;     // Amplitude of sine wave
	    double period = 50.0;        // Number of steps for one full wave (in x units)
	    double noiseLevel = 1.0;


	    public Value(String s) {
		if(s.startsWith("range:")) {
		    s = s.substring("range:".length());
		    List<String> toks = Utils.split(s,";",true,true);
		    if(toks.size()!=2) throw new IllegalArgumentException("-synthetic range error:" + s+" value must be from;to");
		    from  = Double.parseDouble(toks.get(0));
		    to  = Double.parseDouble(toks.get(1));		    
		} else if(s.startsWith("sine:")) {
		    sine = true;
		    s = s.substring("sine:".length());
		    List<String> toks = Utils.split(s,";",true,true);
		    if(toks.size()>0 && toks.size()!=3) {
			throw new IllegalArgumentException("-synthetic sine error:" + s+" value must be amplitude;period;noise e.g. 10;50;1");
		    }
		    if(toks.size()==3) {
			amplitude = Double.parseDouble(toks.get(0));
			period  = Double.parseDouble(toks.get(1));
			noiseLevel  = Double.parseDouble(toks.get(2));			
		    }

		} else if(s.startsWith("rand:")) {
		    random = true;
		    s = s.substring("rand:".length());
		    List<String> toks = Utils.split(s,";",true,true);
		    if(toks.size()==2) {
			from  = Double.parseDouble(toks.get(0));
			to  = Double.parseDouble(toks.get(1));
		    }
		} else {
		    value = s;
		}
	    }
	    public String getValue(int step,int max) {
		if(value!=null) return value;
		if(random) {
		    if(!Double.isNaN(from)) {
			return Double.toString(ThreadLocalRandom.current().nextDouble(from,to));
		    }
		    return Double.toString(ThreadLocalRandom.current().nextDouble());
		}
		if(sine) {
		    double angle = 2 * Math.PI * step / period;
		    double sineValue = amplitude * Math.sin(angle);
		    double noise = ThreadLocalRandom.current().nextDouble(-noiseLevel, noiseLevel);
		    double noisyValue = sineValue + noise;
		    return Double.toString(noisyValue);
		}
		double percent = step/(double)max;
		return Double.toString(from+(to-from)*percent);
	    }
	}

        public void initialize(Seesv seesv, TextReader ctx)
                throws Exception {
            super.initialize(seesv, ctx);
            this.ctx = ctx;
        }

        public Row readRow() throws Exception {
            if (count > rows) {
                return null;
            }
            count++;
	    if (count == 1) {
		Row row = makeRow();
                for (String name : header) {
                    row.add(name);
                }
		return row;
            } else {
		Row row = new Row();
		for(Value v: values) {
		    row.add(v.getValue(count-2,rows));
		}
		return row;
            }
        }

    }

    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Nov 4, '21
     * @author         Enter your name here...
     */
    public static class Pdf extends DataProvider {

        /**  */
        StringTokenizer tokenizer;

        /**  */
        boolean didFirst = false;

        /**  */
        BufferedReader stdInput;

        /**  */
        TextReader ctx;

        /**  */
        String tabula;

        /**  */
        private int rowCnt = 0;

        public Pdf(Seesv seesv) {

            tokenizer = StringTokenizer.getCSVInstance();
            tokenizer.setEmptyTokenAsNull(true);
            tabula = seesv.getProperty("RAMADDA_TABULA");
            if (tabula == null) {
                tabula = seesv.getProperty("ramadda_tabula");
            }
        }

        public void initialize(Seesv seesv, TextReader ctx)
                throws Exception {
            super.initialize(seesv, ctx);
            this.ctx = ctx;
            Runtime rt = Runtime.getRuntime();
            if (tabula == null) {
                throw new IllegalArgumentException(
                    "No ramadda_tabula environment variable set");
            }
            String[] commands = { tabula, ctx.getInputFile() };
            Process  proc     = rt.exec(commands);
            stdInput = new BufferedReader(
                new InputStreamReader(proc.getInputStream()));

            BufferedReader stdError = new BufferedReader(
                                          new InputStreamReader(
                                              proc.getErrorStream()));

            /*
              while ((s = stdError.readLine()) != null) {
              System.out.println(s);
              }
            */
        }

        public Row readRow() throws Exception {
            String line = null;

            while (true) {
                line = stdInput.readLine();
                if (line == null) {
                    return null;
                }
                rowCnt++;
                if (rowCnt <= ctx.getSkipLines()) {
                    ctx.addHeaderLine(line);
                    continue;
                }

                break;
            }
            List<String> toks = Utils.tokenizeColumns(line, tokenizer);
            return makeRow(toks);
        }

    }

}
