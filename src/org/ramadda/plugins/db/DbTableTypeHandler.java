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

package org.ramadda.plugins.db;


import org.ramadda.data.docs.*;
import org.ramadda.repository.*;
import org.ramadda.repository.database.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.FormInfo;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Json;
import org.ramadda.util.Utils;

import org.ramadda.util.sql.*;
import org.ramadda.util.text.Row;
import org.ramadda.util.text.TextReader;

import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

import java.sql.*;

import java.util.ArrayList;

import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.*;



/**
 *
 *
 */
public class DbTableTypeHandler extends TabularTypeHandler {

    /** _more_ */
    private static int IDX = TabularTypeHandler.IDX_LAST;

    /** _more_ */
    public static final int IDX_DBID = IDX++;

    /** _more_ */
    public static final int IDX_TABLE = IDX++;

    /** _more_ */
    public static final int IDX_PROPERTIES = IDX++;


    /** _more_ */
    private Hashtable<String, List<TableInfo>> dbToTables =
        new Hashtable<String, List<TableInfo>>();

    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public DbTableTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public boolean adminOnly() {
        return true;
    }


    /**
     *  column: Label, searchable
     *
     * @param request _more_
     * @param entry _more_
     * @param myxls _more_
     * @param visitInfo _more_
     * @param visitor _more_
     *
     * @param dbid _more_
     *
     *
     * @return _more_
     * @throws Exception _more_
     */


    private Connection getConnection(String dbid) throws Exception {
        Connection connection =
            getDatabaseManager().getExternalConnection("table.db", dbid);

        return connection;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param myxls _more_
     * @param visitInfo _more_
     * @param textReader _more_
     * @param visitor _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void visit(Request request, Entry entry, InputStream myxls,
                      TextReader textReader, TabularVisitor visitor)
            throws Exception {


        TableInfo tableInfo = getTableInfo(entry);
        if (tableInfo == null) {
            System.err.println("Error: Could not find TableInfo for entry:"
                               + entry);

            return;
        }
        List columns = new ArrayList();
        for (ColumnInfo col : tableInfo.getColumns()) {
            columns.add(Json.map("name", Json.quote(col.getName()), "type",
                                 Json.quote(col.getTypeName())));
        }
        textReader.addTableProperty("columns", Json.list(columns));


        String dbid = entry.getValue(IDX_DBID, (String) null);
        if ( !Utils.stringDefined(dbid)) {
            System.err.println("DbTableTypeHandler.visit: no dbid defined");

            return;
        }

        Connection connection = getConnection(dbid);
        if (connection == null) {
            System.err.println("DbTableTypeHandler.visit: no connection");

            return;
        }

        String table = entry.getValue(IDX_TABLE, (String) null);
        //        System.err.println("table:" + table + " idx:" + IDX_TABLE);

        if ( !Utils.stringDefined(table)) {
            System.err.println("DbTableTypeHadler.visit: no table defined");

            return;
        }
        String what = "*";
        /*
        List<String> cols = StringUtil.split(entry.getValue(IDX_COLUMNS, ""),
                                             "\n", true, true);

        if (cols.size() > 0) {
            what = StringUtil.join(",", cols);
            }*/

        int          max        = (textReader.getMaxRows() > 0)
                                  ? textReader.getMaxRows()
                                  : TabularOutputHandler.MAX_ROWS;

        List<Clause> andClauses = new ArrayList<Clause>();
        List<Clause> orClauses  = new ArrayList<Clause>();
        //        String       s          = textReader.getSearchText();
        String s   = null;
        String opp = "(<>|=|<|>|<=|>=)";

        if (tableInfo != null) {
            Pattern pattern = Pattern.compile("(.*?)(<|<=|>|>=|=|!=)(.*?)");
            for (String arg : textReader.getSearchExpressions()) {
                Matcher matcher = pattern.matcher(arg);
                if ( !matcher.find()) {
                    continue;
                }
                String col   = matcher.group(1).toLowerCase().trim();
                String expr  = matcher.group(2).trim();
                Object value = matcher.group(3).trim();
                try {
                    value = new Double(value.toString());
                } catch (Exception exc) {}
                System.err.println("Match:" + col + ":" + expr + ":" + value);
                for (ColumnInfo colInfo : tableInfo.getColumns()) {
                    if (colInfo.getName().equals(col)) {
                        andClauses.add(new Clause(colInfo.getName(), expr,
                                value));

                        break;
                    }
                }
            }
        }


        if (Utils.stringDefined(s)) {
            if (tableInfo != null) {
                for (ColumnInfo col : tableInfo.getColumns()) {
                    //Don't loop forever
                    int cnt = 5;
                    while (cnt-- > 0) {
                        if (s.length() == 0) {
                            break;
                        }
                        String pattern = col.getName().toLowerCase() + "\\s*"
                                         + opp + "\\s*([^\\s]+)";
                        String[] toks = Utils.findPatterns(s, pattern);
                        if (toks == null) {
                            pattern = col.getName().toLowerCase() + "\\s*"
                                      + opp + "\\s*\"(.*)\"";
                            toks = Utils.findPatterns(s, pattern);
                        }
                        if (toks == null) {
                            break;
                        }
                        if (toks != null) {
                            String op = toks[0].trim();
                            String v  = toks[1];
                            s = s.replaceFirst(pattern, "").trim();
                            v = v.trim();
                            Clause clause = new Clause(col.getName(), op, v);
                            andClauses.add(clause);
                        }
                    }
                }


                if (Utils.stringDefined(s)) {
                    String sqlString = SqlUtil.wildCardBoth(s);
                    for (ColumnInfo col : tableInfo.getColumns()) {
                        if (col.getType() == col.TYPE_VARCHAR) {
                            orClauses.add(Clause.like(col.getName(),
                                    sqlString));
                        }
                    }
                }
            }
        }


        if (tableInfo != null) {
            for (ColumnInfo col : tableInfo.getColumns()) {
                String v =
                    request.getString("table." + col.getName().toLowerCase(),
                                      (String) null);
                if ( !Utils.stringDefined(v)) {
                    continue;
                }
                List<String> toks    = StringUtil.split(v, " ", true, true);
                String       lastOp  = null;
                String       pattern = "^" + opp + "(.+)";
                for (int i = 0; i < toks.size(); i++) {
                    String   tok   = toks.get(i);
                    String[] toks2 = Utils.findPatterns(tok, pattern);
                    if (toks2 == null) {
                        String isOp = StringUtil.findPattern(tok, opp);
                        if (isOp != null) {
                            lastOp = tok;

                            continue;
                        }
                        if (lastOp == null) {
                            lastOp = Clause.EXPR_EQUALS;
                        }
                        andClauses.add(new Clause(col.getName(), lastOp,
                                tok));
                        lastOp = null;

                        continue;
                    }
                    Clause clause = new Clause(col.getName(),
                                        toks2[0].trim(), toks2[1].trim());
                    andClauses.add(clause);

                }
            }
        }



        if (orClauses.size() > 0) {
            andClauses.add(Clause.or(orClauses));
        }
        //        SqlUtil.debug = true;
        Statement stmt = SqlUtil.select(connection, what,
                                        Misc.newList(table),
                                        Clause.and(andClauses), "", max, 0);

        //        SqlUtil.debug = false;
        SqlUtil.Iterator   iter = new SqlUtil.Iterator(stmt);
        ResultSet          results;
        ResultSetMetaData  rsmd = null;

        List<List<Object>> rows = new ArrayList<List<Object>>();
        while ((results = iter.getNext()) != null) {
            if (rsmd == null) {
                rsmd = results.getMetaData();
                int          columnCount = rsmd.getColumnCount();
                List<Object> names       = new ArrayList<Object>();
                for (int i = 1; i < columnCount + 1; i++) {
                    String name     = rsmd.getColumnName(i);
                    String typeName = rsmd.getColumnTypeName(i);
                    //                    names.add(name + " - " + typeName);
                    names.add(name);
                }
                rows.add(names);
            }
            List<Object> values = new ArrayList<Object>();
            for (int col = 0; col < rsmd.getColumnCount(); col++) {
                int    colIdx = col + 1;
                int    type   = rsmd.getColumnType(colIdx);
                Object value  = null;
                //TODO: Handle more dates
                if (type == java.sql.Types.DOUBLE) {
                    value = new Double(results.getDouble(colIdx));
                } else if (type == java.sql.Types.FLOAT) {
                    value = new Float(results.getFloat(colIdx));
                } else if (type == java.sql.Types.INTEGER) {
                    value = new Integer(results.getInt(colIdx));
                } else {
                    value = results.getString(colIdx);
                }
                values.add(value);
            }
            Row row = new Row(values);
            if ((andClauses.size() == 0)
                    && !textReader.getFilter().rowOk(textReader, row)) {
                //                System.err.println ("skipping row:" + row);
                continue;
            }
            //            System.err.println ("adding row:" + row);
            rows.add(values);
        }
        visitor.visit(textReader, table, rows);
        connection.close();
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     */
    @Override
    public boolean okToShowTable(Request request, Entry entry) {
        if ( !Utils.stringDefined(entry.getValue(IDX_DBID, (String) null))) {
            return false;
        }
        if ( !Utils.stringDefined(entry.getValue(IDX_TABLE, (String) null))) {
            return false;
        }

        return super.okToShowTable(request, entry);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param column _more_
     * @param formBuffer _more_
     * @param values _more_
     * @param state _more_
     * @param formInfo _more_
     * @param baseTypeHandler _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void addColumnToEntryForm(Request request, Entry entry,
                                     Column column, Appendable formBuffer,
                                     Object[] values, Hashtable state,
                                     FormInfo formInfo,
                                     TypeHandler baseTypeHandler)
            throws Exception {

        if (column.getName().equals("db_id")) {
            List<String> dbs = StringUtil.split(
                                   getRepository().getProperty(
                                       "table.db.databases", ""), ",", true,
                                           true);
            if (dbs.size() > 0) {
                String dbid = ((entry == null)
                               ? null
                               : entry.getValue(IDX_DBID, (String) null));
                formBuffer.append(
                    formEntry(
                        request, column.getLabel() + ":",
                        HtmlUtils.select(column.getEditArg(), dbs, dbid)));

                return;
            }
        }

        if (column.getName().equals("table_name")) {
            List<String> tables = getTableNames(entry);
            if ((tables != null) && (tables.size() > 0)) {
                tables.add(0, "");
                String name = (entry == null)
                              ? null
                              : entry.getValue(IDX_TABLE, (String) null);
                formBuffer.append(
                    formEntry(
                        request, column.getLabel() + ":",
                        HtmlUtils.select(column.getEditArg(), tables, name)));

                return;
            }
        }
        super.addColumnToEntryForm(request, entry, column, formBuffer,
                                   values, state, formInfo, baseTypeHandler);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param column _more_
     * @param widget _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String xxxgetFormWidget(Request request, Entry entry,
                                   Column column, String widget)
            throws Exception {
        return super.getFormWidget(request, entry, column, widget);
    }



    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private List<String> getTableNames(Entry entry) throws Exception {
        if (entry == null) {
            return null;
        }
        List<TableInfo> tableInfos = getTableInfos(entry);
        if (tableInfos == null) {
            return null;
        }

        return TableInfo.getTableNames(tableInfos);
    }

    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public TableInfo getTableInfo(Entry entry) throws Exception {
        List<TableInfo> tableInfos = getTableInfos(entry);
        if (tableInfos == null) {
            return null;
        }
        String table = entry.getValue(IDX_TABLE, (String) null);
        if ( !Utils.stringDefined(table)) {
            return null;
        }
        table = table.toLowerCase();
        for (TableInfo tableInfo : tableInfos) {
            if (tableInfo.getName().toLowerCase().equals(table)) {
                return tableInfo;
            }
        }

        return null;
    }



    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private List<TableInfo> getTableInfos(Entry entry) throws Exception {
        String dbid = entry.getValue(IDX_DBID, (String) null);
        if ( !Utils.stringDefined(dbid)) {
            return null;
        }

        List<TableInfo> tableInfos = dbToTables.get(dbid);
        if (tableInfos == null) {
            Connection connection = null;
            try {
                connection = getConnection(dbid);
                if (connection == null) {
                    System.err.println(
                        "DbTableTypeHadler.visit: no connection");

                    return null;
                }
                tableInfos = getDatabaseManager().getTableInfos(connection,
                        true);
            } finally {
                if (connection != null) {
                    connection.close();
                }
            }
            dbToTables.put(dbid, tableInfos);
        }

        return tableInfos;
    }

    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Jan 16, '15
     * @author         Enter your name here...
     */
    public static class TableProperty {

        /** _more_ */
        private String type;

        /** _more_ */
        private String otherTable;

        /** _more_ */
        private String name;

        /** _more_ */
        private String label;

        /** _more_ */
        private boolean canSearch;

    }

}
