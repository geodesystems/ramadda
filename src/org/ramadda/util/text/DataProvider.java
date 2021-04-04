/*
* Copyright (c) 2008-2021 Geode Systems LLC
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


import org.apache.commons.lang3.text.StrTokenizer;

import org.json.*;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;
import org.ramadda.util.KmlUtil;
import org.ramadda.util.Json;
import org.ramadda.util.NamedInputStream;
import org.ramadda.util.Utils;

import org.ramadda.util.sql.Clause;
import org.ramadda.util.sql.SqlUtil;

import org.w3c.dom.*;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.sql.*;

import java.util.ArrayList;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import java.util.regex.*;


/**
 *
 * @author Jeff McWhirter
 */

public abstract class DataProvider {

    /** _more_ */
    private CsvUtil csvUtil;

    /**
     * _more_
     *
     * @param csvUtil _more_
     */
    public DataProvider() {}


    /**
     * _more_
     *
     *
     * @param csvUtil _more_
     * @param info _more_
     * @param stream _more_
     *
     * @throws Exception _more_
     */
    public void initialize(CsvUtil csvUtil, TextReader info)
	throws Exception {}


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public abstract Row readRow() throws Exception;


    /**
     * _more_
     */
    public void finish() {}


    /**
     * Class description
     *
     *
     * @version        $version$, Tue, Feb 16, '21
     * @author         Enter your name here...
     */
    public abstract static class BulkDataProvider extends DataProvider {

        /** _more_ */
        private List<Row> rows = new ArrayList<Row>();

        /** _more_ */
        private int index = 0;

        /**
         * _more_
         *
         */
        public BulkDataProvider() {
            super();
        }

        /**
         * _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        public Row readRow() throws Exception {
            if (index >= rows.size()) {
                return null;
            }

            return rows.get(index++);
        }

        /**
         * _more_
         *
         * @param row _more_
         *
         * @return _more_
         */
        protected boolean addRow(Row row) {
            //      System.err.println("row:" + row);
            rows.add(row);

            return true;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        protected List<Row> getRows() {
            return rows;
        }

        /**
         * _more_
         *
         * @param csvUtil _more_
         * @param textReader _more_
         * @param stream _more_
         *
         * @throws Exception _more_
         */
        public void initialize(CsvUtil csvUtil, TextReader textReader)
                throws Exception {
	    String s = textReader.convertContents(textReader.readContents());
	    tokenize(textReader, s);
        }

        /**
         * _more_
         *
         * @param info _more_
         * @param s _more_
         *
         * @throws Exception _more_
         */
        public abstract void tokenize(TextReader info, String s)
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

        /** _more_ */
        private int skip;

        /** _more_ */
        private String pattern;

        /** _more_ */
        private Hashtable<String, String> props;

        /**
         * _more_
         *
         * @param sSkip _more_
         * @param htmlPattern _more_
         * @param props _more_
         */
        public HtmlDataProvider(String sSkip, String htmlPattern,
                                Hashtable<String, String> props) {
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
         * _more_
         *
         * @param info _more_
         * @param s _more_
         *
         * @throws Exception _more_
         */
        public void tokenize(TextReader info, String s) throws Exception {

            int count = Utils.getProperty(props, "count", 1);
            if ((pattern == null) || (pattern.trim().length() == 0)) {
                pattern = props.get("pattern");
            }
            String skipAttr = props.get("skipAttr");
            boolean removeEntity = Utils.equals(props.get("removeEntity"),
                                       "true");
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
                    Row     row         = new Row();
                    boolean checkHeader = true;
                    while (true) {
                        toks = Utils.tokenizePattern(tr, "(<td|<th)",
                                "(</td|</th)");
                        if (toks == null) {
                            break;
                        }
                        String td = toks[0].trim();
                        tr = toks[1];
                        String colspan = StringUtil.findPattern(td,
                                             "colspan *= *\"?([0-9]+)\"?");
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
                        td = Utils.stripTags(td);
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
     * @version        $version$, Tue, Feb 16, '21
     * @author         Enter your name here...
     */
    public static class HtmlPatternDataProvider extends BulkDataProvider {

        /** _more_ */
        private String cols;

        /** _more_ */
        private String start;

        /** _more_ */
        private String end;

        /** _more_ */
        private String pattern;



        /**
         * _more_
         *
         * @param cols _more_
         * @param start _more_
         * @param end _more_
         * @param pattern _more_
         */
        public HtmlPatternDataProvider(String cols, String start, String end,
                                       String pattern) {
            super();
            this.cols    = cols;
            this.start   = start;
            this.end     = end;
            this.pattern = pattern;
        }



        /**
         * _more_
         *
         * @param info _more_
         * @param s _more_
         *
         * @throws Exception _more_
         */
        public void tokenize(TextReader info, String s) throws Exception {
            int numLines = info.getMaxRows();
            pattern = Utils.convertPattern(pattern);
            if (cols.length() > 0) {
                addRow(new Row(Utils.split(cols, ",")));
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
                long    t1 = System.currentTimeMillis();
                Matcher m  = p.matcher(s);
                if ( !m.find()) {
                    break;
                }
                Row row = new Row();
                addRow(row);
                for (int i = 1; i <= m.groupCount(); i++) {
                    String tok = m.group(i);
                    if (tok == null) {
                        tok = "";
                    }
                    row.add(tok.trim());
                }
                String s2 = s.substring(m.end());
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

        /** _more_ */
        private String arrayPath;

        /** _more_ */
        private String objectPath;



        /**
         * _more_
         *
         * @param arrayPath _more_
         * @param objectPath _more_
         */
        public JsonDataProvider(String arrayPath, String objectPath) {
            super();
            this.arrayPath  = arrayPath;
            this.objectPath = objectPath;
        }


        /**
         * _more_
         *
         * @param info _more_
         * @param s _more_
         *
         * @throws Exception _more_
         */
        public void tokenize(TextReader info, String s) throws Exception {
	    boolean debug = false;

            int        xcnt  = 0;
            JSONArray  array = null;
            JSONObject root  = null;
	    if(debug) System.err.println("JSON:" + s);
            try {
                root = new JSONObject(s);
                if (arrayPath != null) {
                    array = Json.readArray(root, arrayPath);
                }
		if(debug) 
		    System.err.println("array path:" + arrayPath+" a:" + array);
            } catch (Exception exc) {
		if(debug) 
		    System.err.println("error trying to read JSONObject:" + exc);
            }

            if (array == null) {
		try {
		    array = new JSONArray(s);
		} catch(Exception exc) {
		    System.err.println("Error reading array");
		    if(s.length()>1000) s = s.substring(0,999);
		    System.err.println("JSON:" + s);
		    throw new IllegalArgumentException("Could not read JSON data");
		}		    
            }

            List<String> objectPathList = null;
            if (Utils.stringDefined(objectPath)) {
                objectPathList = Utils.split(objectPath, ",", true,
                        true);
            }
            List<String> names = null;
            for (int arrayIdx = 0; arrayIdx < array.length(); arrayIdx++) {
                Hashtable       primary   = new Hashtable();
                List<Hashtable> secondary = new ArrayList<Hashtable>();
		List<String> arrayKeys = new ArrayList<String>();
                if (objectPathList != null) {
                    JSONObject jrow = array.getJSONObject(arrayIdx);
                    for (String tok : objectPathList) {
                        if (tok.equals("*")) {
                            primary.putAll(Json.getHashtable(jrow, true,arrayKeys));
                        } else if (tok.endsWith("[]")) {
                            JSONArray a = Json.readArray(jrow,
                                              tok.substring(0,
                                                  tok.length() - 2));
                            for (int j = 0; j < a.length(); j++) {
                                secondary.add(
                                    Json.getHashtable(
						      a.getJSONObject(j), true,arrayKeys));
                            }
                        } else {
                            try {
                                Object o = Json.readObject(jrow, tok);
                                if (o != null) {
                                    primary.putAll(Json.getHashtable(o,
								     false,arrayKeys));
                                }
                            } catch (Exception exc) {
                                Object o = Json.readArray(jrow, tok);
                                if (o != null) {
                                    primary.putAll(Json.getHashtable(o,
								     true,arrayKeys));
                                }
                            }
                        }
                    }
                } else {
                    try {
                        primary.putAll(
                            Json.getHashtable(
                                array.getJSONArray(arrayIdx), true,arrayKeys));
                    } catch (Exception exc) {
                        primary.putAll(
                            Json.getHashtable(
                                array.getJSONObject(arrayIdx), true,arrayKeys));
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
                    Row row = new Row();
                    addRow(row);
		    if(arrayKeys.size()>0) {
			names.addAll(arrayKeys);
		    } else {
			for (Enumeration keys = secondary.get(0).keys();
			     keys.hasMoreElements(); ) {
			    names.add((String) keys.nextElement());
			}
		    }
		    //		    names = (List<String>) Utils.sort(names);
                    for (String name : names) {
                        row.add(name);
                    }
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
                    Row row = new Row();
                    addRow(row);
                    for (String name : names) {
                        Object value = h.get(name);
                        if (value == null) {
                            value = "NULL";
                        }
                        row.add(value);
                    }
                }
            }

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

        /** _more_          */
        private String db;

        /** _more_          */
        private String table;

        private String columns;
        private String where;	

        /** _more_          */
        private Hashtable<String, String> props;

        /** _more_          */
        private Connection connection;

        /** _more_          */
        private Statement statement;

        /** _more_          */
        private SqlUtil.Iterator iter;

        /** _more_          */
        private int columnCount;

        /** _more_          */
        private Row headerRow;

        /** _more_          */
        private int rowCnt = 0;

        /** _more_          */
        private int maxRows;

        /**
         * _more_
         *
         * @param arrayPath _more_
         * @param objectPath _more_
         *
         * @param db _more_
         * @param table _more_
         * @param props _more_
         */
        public SqlDataProvider(String db, String table, String columns, String where,
                               Hashtable<String, String> props) {
            this.db    = db.trim();
            this.table = SqlUtil.sanitize(table.trim());
            this.columns = SqlUtil.sanitize(columns.trim());	    
            this.where = where.trim();
            this.props = props;
        }

        /**
         * _more_
         *
         * @param csvUtil _more_
         * @param ctx _more_
         * @param stream _more_
         *
         * @throws Exception _more_
         */
        public void initialize(CsvUtil csvUtil, TextReader ctx)
                throws Exception {
            String jdbcUrl = csvUtil.getProperty("csv_" + db + "_url");
            if (jdbcUrl == null) {
                String dbs = csvUtil.getProperty("csv_dbs");
                throw new IllegalArgumentException("Unknown database:" + db
                        + " " + ((dbs == null)
                                 ? ""
                                 : "Available databases:" + dbs));
            }

            String user     = csvUtil.getProperty("csv_" + db + "_user");
            String password = csvUtil.getProperty("csv_" + db + "_password");
            this.connection = SqlUtil.getConnection(jdbcUrl, user, password);

            //Check tables whitelist
            String tables   = csvUtil.getProperty("csv_" + db + "_tables",
                                  "");
            List   okTables = Arrays.asList(tables.split(","));
            if ((okTables.size() == 1) && okTables.get(0).equals("*")) {
                okTables = SqlUtil.getTableNames(this.connection);
            }

	    List<String> tableList = Utils.split(table,",",true,true);
	    for(String t: tableList) {
		if ( !Utils.containsIgnoreCase(okTables, t)) {
		    throw new IllegalArgumentException("Unknown table:" + t
						       + "\nAvailable tables:\n"
						       + Utils.wrap(okTables, "\t", "\n"));
		}
	    }

            if (Misc.equals(props.get("help"), "true")) {
                throw new CsvUtil.MessageException("table:" + table
                        + "\ncolumns:\n"
                        + Utils.wrap(SqlUtil.getColumnNames(connection,
                            table, true), "\t", "\n"));
            }


            List<Clause> clauses = new ArrayList<Clause>();


	    String join  = (String) props.get("join");
	    if(join!=null) {
		List<String> toks = Utils.split(join,",");
		if(toks.size()!=2) {
		    throw new IllegalArgumentException("Bad join:" + join);
		}
		clauses.add(Clause.join(toks.get(0),toks.get(1)));
	    }
            String what = columns;
            if (what == null || what.length()==0) {
                what = "*";
            }
	    if (where != null && where.length()>0) {
                for (String tok : where.split(";")) {
                    String[] toks = tok.split(",");
                    if (toks.length != 3) {
                        throw new IllegalArgumentException("Bad where value:"
                                + tok);
                    }
                    String expr  = toks[1].toLowerCase();
                    String value = toks[2];
                    if (expr.equals("like")
                            && !(value.startsWith("%")
                                 || value.endsWith("%"))) {
                        value = "%" + value + "%";
                    }
                    clauses.add(new Clause(toks[0], expr, value));
                }
            }
            Clause clause = (clauses.size() > 0)
                            ? Clause.and(clauses)
                            : null;
            this.statement = SqlUtil.select(connection, what,
                                            Utils.makeList(table), clause,
                                            "");
            this.iter = SqlUtil.getIterator(this.statement);

            List              values  = new ArrayList();
            ResultSet         results = this.statement.getResultSet();
            ResultSetMetaData rsmd    = results.getMetaData();
            this.columnCount = rsmd.getColumnCount();
	    //	    System.err.println("header cnt:" + this.columnCount);
            for (int i = 0; i < this.columnCount; i++) {
                values.add(rsmd.getColumnName(i+1).toLowerCase());
            }
            headerRow = new Row(values);
            maxRows   = ctx.getMaxRows();
        }


        /**
         * _more_
         *
         * @param info _more_
         * @param s _more_
         *
         *
         * @return _more_
         * @throws Exception _more_
         */
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
	    //	    System.err.println("row:" + rowCnt);
            for (int i = 0; i < this.columnCount; i++) {
		String v = results.getString(i + 1);
		if(v==null) v = "";
		//		System.err.println("\tcol:" + i +" v:" +v);
                values.add(v);
		//                values.add(results.getString(i + 1) +" name:" + rsmd.getColumnName(i+1));
            }

            return new Row(values);
        }

        /**
         * _more_
         */
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

        /** _more_ */
        private String arrayPath;

        /** _more_ */
        private String objectPath;

        /**
         * _more_
         *
         * @param arrayPath _more_
         */
        public XmlDataProvider(String arrayPath) {
            super();
            this.arrayPath = arrayPath;
        }


        /**
         * _more_
         *
         * @param info _more_
         * @param s _more_
         *
         * @throws Exception _more_
         */
        public void tokenize(TextReader info, String s) throws Exception {

            Element root   = XmlUtil.getRoot(s);
            Row     header = new Row();
            addRow(header);
            List<Element> nodes =
                (List<Element>) Utils.findDescendantsFromPath(root,
                    arrayPath);
            int cnt    = 0;
            int colCnt = 0;
            Hashtable<String, Integer> colMap = new Hashtable<String,
                                                    Integer>();
            System.err.println("NODES:" + nodes.size());
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
                Row      row      = new Row();
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


    public static class KmlDataProvider extends BulkDataProvider {


        /**
         * _more_
         *
         * @param arrayPath _more_
         */
        public KmlDataProvider() {
            super();
        }


	@Override
        public void initialize(CsvUtil csvUtil, TextReader textReader)
                throws Exception {
	    List<String> files = csvUtil.getInputFiles();
	    if(files.size()==0) return;
	    String path = files.get(0);
	    Element root = KmlUtil.readKml(path,textReader.getInputStream());
	    read(root);
        }


        public void tokenize(TextReader info, String s)
	    throws Exception {
	}


        /**
         * _more_
         *
         * @param info _more_
         * @param s _more_
         *
         * @throws Exception _more_
         */
        private void read(Element root) throws Exception {
            List<Element> placemarks = KmlUtil.findPlacemarks(root);
	    List<Element> lookAts = (List<Element>) XmlUtil.findDescendants(root, KmlUtil.TAG_LOOKAT);
	    if(lookAts.size()>placemarks.size()) {
		readLookAts(lookAts);
	    } else  if(placemarks.size()>0) {
		readPlacemarks(placemarks);
	    } else {
		List<Element> nodes = (List<Element>) XmlUtil.findDescendants(root, KmlUtil.TAG_GROUNDOVERLAY);
		if(nodes.size()>0) {
		    readGroundOverlays(nodes);
		}
	    }
	}
		
	private  void readLookAts(List<Element> lookAts) throws Exception  {
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
  </LookAt>	    */
            Row     header = new Row();
	    header.add("Latitude","Longitude","Elevation","Heading","Tilt","Range");
            addRow(header);
            for (Element lookAt : lookAts) {
		Row row = new Row();
		addRow(row);
		row.add(XmlUtil.getGrandChildText(lookAt,"latitude","NaN"));
		row.add(XmlUtil.getGrandChildText(lookAt,"longitude","NaN"));		
		row.add(XmlUtil.getGrandChildText(lookAt,"altitude","NaN"));
		row.add(XmlUtil.getGrandChildText(lookAt,"heading","NaN"));
		row.add(XmlUtil.getGrandChildText(lookAt,"tilt","NaN"));
            }
	}


	private  void readGroundOverlays(List<Element> nodes) throws Exception  {
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
            Row     header = new Row();
	    header.add("Name","Image","Latitude","Longitude","North","South","East","West");
            addRow(header);
            for (Element node : nodes) {
		Row row = new Row();
		addRow(row);
		row.add(XmlUtil.getGrandChildText(node,"name",""));
		Element icon = XmlUtil.findChild(node,"Icon");
		if(icon!=null) row.add(XmlUtil.getGrandChildText(icon,"href",""));
		else row.add("");
		Element llbox = XmlUtil.findChild(node,"LatLonBox");
		if(llbox==null) {
		    row.add("NaN","NaN","NaN","NaN","NaN","NaN");
		    continue;
		}
		double north = Double.parseDouble(XmlUtil.getGrandChildText(llbox,"north","NaN"));
		double south = Double.parseDouble(XmlUtil.getGrandChildText(llbox,"south","NaN"));		
		double east = Double.parseDouble(XmlUtil.getGrandChildText(llbox,"east","NaN"));
		double west = Double.parseDouble(XmlUtil.getGrandChildText(llbox,"west","NaN"));
		double lat = south+(north-south)/2;
		double lon = west+(east-west)/2;		
		row.add(""+lat);
		row.add(""+lon);
		row.add(""+north);
		row.add(""+south);
		row.add(""+east);
		row.add(""+west);
            }
	}



	private void readPlacemarks(List<Element> placemarks) throws Exception {
            Row     header = new Row();
	    header.add("Name","Description","Latitude","Longitude","Elevation");
            addRow(header);
            for (Element placemark : placemarks) {
		Row row = new Row();
		addRow(row);
		row.add(XmlUtil.getGrandChildText(placemark,"name",""));
		row.add(XmlUtil.getGrandChildText(placemark,"description",""));		
		List<Element> coords = (List<Element>) XmlUtil.findDescendants(placemark, "coordinates");
		if(coords.size()==0) {
		    row.add("NaN","NaN","NaN");
		} else {
		    String coordsText = XmlUtil.getChildText(coords.get(0));
		    double[][] pts = KmlUtil.parseCoordinates(coordsText, 1);
		    row.add(pts[0][0]+"",pts[1][0]+"",pts[2][0]+"");
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

        /** _more_ */
        private List<String> header;

        /** _more_ */
        private String pattern;

        /** _more_ */
        private String objectPath;

        /**
         * _more_
         *
         * @param header _more_
         * @param pattern _more_
         */
        public PatternDataProvider(List<String> header, String pattern) {
            super();
            this.header  = header;
            this.pattern = Utils.convertPattern(pattern);
        }




        /**
         * _more_
         *
         * @param info _more_
         * @param s _more_
         *
         * @throws Exception _more_
         */
        public void tokenize(TextReader info, String s) throws Exception {
            addRow(new Row(header));
            if (pattern.length() == 0) {
                return;
            }
            Pattern p = Pattern.compile(pattern);
            while (true) {
                Matcher m = p.matcher(s);
                if ( !m.find()) {
                    break;
                }
                Row row = new Row();
                addRow(row);
                for (int i = 1; i <= m.groupCount(); i++) {
                    row.add(m.group(i));
                }
                s = s.substring(m.end());
                //      System.err.println(s.length());
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

        /** _more_ */
        String header;

        /** _more_ */
        String chunkPattern;

        /** _more_ */
        String tokenPattern;


        /**
         * _more_
         *
         * @param header _more_
         * @param chunkPattern _more_
         * @param tokenPattern _more_
         */
        public Pattern2DataProvider(String header, String chunkPattern,
                                    String tokenPattern) {
            super();
            this.header       = header;
            this.chunkPattern = Utils.convertPattern(chunkPattern);
            this.tokenPattern = Utils.convertPattern(tokenPattern);
        }


        /**
         * _more_
         *
         * @param info _more_
         * @param s _more_
         *
         * @throws Exception _more_
         */
        public void tokenize(TextReader info, String s) throws Exception {
            Row headerRow = new Row();
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
                s = s.substring(m1.end());
                //              System.err.println("REMAINDER:" + s);
                for (int i1 = 1; i1 <= m1.groupCount(); i1++) {
                    String chunk = m1.group(i1).trim();
                    //              System.err.println("CHUNK[" + i1 +"]=" + chunk+"\n**********");
                    //              System.err.println("CHUNK:");
                    //              System.exit(0);
                    int cnt = 1;
                    while (true) {
                        Matcher m2 = p2.matcher(chunk);
                        if ( !m2.find()) {
                            break;
                        }
                        Row row = null;
                        if (cnt < getRows().size()) {
                            row = getRows().get(cnt);
                        } else {
                            row = new Row();
                            addRow(row);
                        }
                        cnt++;
                        for (int i2 = 1; i2 <= m2.groupCount(); i2++) {
                            String tok = m2.group(i2).trim();
                            //                      System.err.println("\ttok:" + tok);
                            row.add(tok);
                        }
                        chunk = chunk.substring(m2.end());
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
    public static class Pattern3DataProvider extends BulkDataProvider {

        /** _more_ */
        String header;


        /** _more_ */
        String tokenPattern;


        /**
         * _more_
         *
         * @param header _more_
         * @param chunkPattern _more_
         * @param tokenPattern _more_
         */
        public Pattern3DataProvider(String header, String tokenPattern) {
            super();
            this.header       = header;
            this.tokenPattern = Utils.convertPattern(tokenPattern);
        }


        /**
         * _more_
         *
         * @param info _more_
         * @param s _more_
         *
         * @throws Exception _more_
         */
        public void tokenize(TextReader info, String s) throws Exception {
            Row headerRow = new Row();
            addRow(headerRow);
            for (String tok : Utils.split(header, ",")) {
                headerRow.add(tok);
            }
            if (info.getDebug()) {
                info.printDebug("-text3",
                                "Parsing text input\n\tpattern:"
                                + tokenPattern);
            }
            Pattern p1 = Pattern.compile(tokenPattern);
            while (true) {
                Matcher m1 = p1.matcher(s);
                if ( !m1.find()) {
                    info.printDebug("-text3", "no match");

                    //              System.err.println("no match");
                    break;
                }
                //              System.err.println("match");
                s = s.substring(m1.end());
                if (info.getDebug()) {
                    info.printDebug("-text3",
                                    "match group count:" + m1.groupCount());
                }
                //              System.err.println("REMAINDER:" + s);
                Row row = new Row();
                addRow(row);
                for (int i = 1; i <= m1.groupCount(); i++) {
                    String tok = m1.group(i).trim();
                    row.add(tok);
                }
            }
            if (info.getDebug()) {
                info.printDebug("\tdone parsing input #rows:"
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

        /** _more_ */
        String header;

        /** _more_ */
        String chunkPattern;

        /** _more_ */
        String tokenPattern;


        /**
         * _more_
         *
         * @param header _more_
         * @param chunkPattern _more_
         * @param tokenPattern _more_
         */
        public TextDataProvider(String header, String chunkPattern,
                                String tokenPattern) {
            super();
            this.header       = header;
            this.chunkPattern = Utils.convertPattern(chunkPattern);
            this.tokenPattern = Utils.convertPattern(tokenPattern);
        }


        /**
         * _more_
         *
         * @param info _more_
         * @param s _more_
         *
         * @throws Exception _more_
         */
        public void tokenize(TextReader info, String s) throws Exception {
            Row headerRow = new Row();
            addRow(headerRow);
            for (String tok : Utils.split(header, ",")) {
                headerRow.add(tok);
            }
            try {
                Pattern p1 = Pattern.compile(chunkPattern);
                Pattern p2 = Pattern.compile(tokenPattern);
                while (true) {
                    Matcher m1 = p1.matcher(s);
                    if ( !m1.find()) {
                        break;
                    }
                    s = s.substring(m1.end());
                    if (m1.groupCount() > 2) {
                        throw new IllegalArgumentException(
                            "There should only be one sub-pattern in the chunk");
                    }
                    String  chunk = m1.group(1).trim();
                    Matcher m2    = p2.matcher(chunk);
                    if ( !m2.find()) {
                        break;
                    }
                    Row row = new Row();
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

        /** _more_ */
        int cnt = 0;

        /** _more_ */
        int rawLines = 0;

        /** _more_ */
        TextReader ctx;


        /** _more_ */
        int rowCnt = 0;

        /** _more_          */
        boolean deHeader = false;

	private StrTokenizer   tokenizer;



        /**
         * _more_
         *
         * @param ctx _more_
         * @param rawLines _more_
         */
        public CsvDataProvider(TextReader ctx, int rawLines) {
            this.ctx = ctx;
            if (this.ctx != null) {
                this.deHeader = Misc.equals("true",
                                            ctx.getProperty("deheader"));
            }
            this.rawLines = rawLines;
        }



        /**
         * _more_
         *
         * @param rawLines _more_
         */
        public CsvDataProvider(int rawLines) {
            this(null, rawLines);
        }


        /**
         * _more_
         *
         * @param ctx _more_
         */
        public CsvDataProvider(TextReader ctx) {
            this(ctx, 0);
        }


        /**
         * _more_
         *
         *
         * @param csvUtil _more_
         * @param ctx _more_
         * @param stream _more_
         *
         * @throws Exception _more_
         */
        public void initialize(CsvUtil csvUtil, TextReader ctx)
                throws Exception {
            this.ctx = ctx;
        }

        /**
         * _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        public Row readRow() throws Exception {
            while (true) {
                String line = ctx.readLine();
		if (line == null) {
		   return null;
                }

		//		line = line.replaceAll("\\u000d", " ");

                if (rawLines > 0) {
                    ctx.getWriter().println(line);
                    rawLines--;
                    continue;
                }
                if (ctx.getVerbose()) {
                    if (((++cnt) % 1000) == 0) {
                        System.err.println("processed:" + cnt);
                    }
                }
                if (deHeader) {
                    if (line.startsWith("#fields=")) {
                        line     = line.substring("#fields=".length());
                        line     = line.replaceAll("\\[.*?\\]", "");
                        deHeader = false;
                    }
                }


                if ( !ctx.lineOk(ctx, line)) {
                    continue;
                }


                rowCnt++;
                if (rowCnt <= ctx.getSkip()) {
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
                    //                System.err.println("CsvUtil.delimiter is null new one is:" + delimiter);
                    ctx.setDelimiter(delimiter);
                }
                if (line.length() == 0) {
                    continue;
                }
                Row row = null;
                if (widths != null) {
                    row = new Row(Utils.tokenizeColumns(line, widths));
                } else if (ctx.getSplitOnSpaces()) {
                    row = new Row(Utils.split(line, " ", true, true));
                } else {
		    if(tokenizer==null) {
			tokenizer = StrTokenizer.getCSVInstance();
			tokenizer.setEmptyTokenAsNull(true);
			if ( !ctx.getDelimiter().equals(",")) {
			    tokenizer.setDelimiterChar(ctx.getDelimiter().charAt(0));
			}
		    }
		    List<String> toks = Utils.tokenizeColumns(line,tokenizer);
		    row = new Row(toks);
		}

                return row;
            }
        }

    }

}
