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

package org.ramadda.plugins.media;



import org.ramadda.data.docs.*;


import org.ramadda.data.point.text.*;

import org.ramadda.data.record.*;
import org.ramadda.data.services.PointOutputHandler;
import org.ramadda.data.services.PointTypeHandler;
import org.ramadda.data.services.RecordTypeHandler;
import org.ramadda.repository.*;
import org.ramadda.repository.database.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.FormInfo;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Json;
import org.ramadda.util.Utils;

import org.ramadda.util.sql.*;
import org.ramadda.util.text.*;
import org.ramadda.util.text.Row;

import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.io.*;

import java.sql.*;

import java.text.SimpleDateFormat;

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
public class DbTableTypeHandler extends PointTypeHandler /*extends TabularTypeHandler*/ {

    /** _more_ */
    private static int IDX = PointTypeHandler.IDX_LAST + 1;  //TabularTypeHandler.IDX_LAST;

    /** _more_ */
    public static final int IDX_DBID = IDX++;

    /** _more_ */
    public static final int IDX_TABLE = IDX++;



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
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Connection getConnection(Entry entry) throws Exception {
        return getConnection(entry.getValue(IDX_DBID, (String) null));
    }

    /**
     *
     *
     * @param dbid _more_
     * @return _more_
     * @throws Exception _more_
     */
    private Connection getConnection(String dbid) throws Exception {
        if (dbid == null) {
            return null;
        }
        Connection connection =
            getDatabaseManager().getExternalConnection("table.db", dbid);

        return connection;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public int getValuesIndex() {
        return PointTypeHandler.IDX_LAST + 1;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param tag _more_
     * @param props _more_
     * @param displayProps _more_
     *
     * @return _more_
     */
    @Override
    public String getUrlForWiki(Request request, Entry entry, String tag,
                                Hashtable props, List<String> displayProps) {
        if (tag.startsWith("display_") || tag.equals("display")) {
            List<String[]> fieldList = getFieldList(entry);
            if (fieldList == null) {
                System.err.println("DbTable: Could not find field list");

                return null;
            }


            String all = null;
            Hashtable recordProps =
                Utils.getProperties(entry.getValue(IDX_PROPERTIES,
                    (String) ""));
            recordProps.putAll(Utils.makeHashtable(displayProps));
            for (String[] tuple : fieldList) {
                String name = tuple[0];
                String type = tuple[1];
                if (recordProps.get("macro." + name + ".type") != null) {
                    type = ((String) recordProps.get("macro." + name
                            + ".type")).trim();
                }
                if ( !Misc.equals(recordProps.get("macro." + name
                        + ".display"), "true")) {
                    continue;
                }
                if (all != null) {
                    all += ",";
                } else {
                    all = "";
                }
                all += name;
                if ( !displayProps.contains("macro." + name + ".label")) {
                    displayProps.add("macro." + name + ".label");
                    displayProps.add(Json.quote(Utils.makeLabel(name)));
                }
                displayProps.add("macro." + name + ".urlarg");
                displayProps.add(Json.quote("search." + name));

                displayProps.add("macro." + name + ".type");
                displayProps.add(Json.quote(type));
                if (type.equals("enumeration")) {
                    String enums = (String) recordProps.get("macro." + name
                                       + ".values");
                    if (enums != null) {
                        displayProps.add("macro." + name + ".values");
                        displayProps.add(Json.quote(enums));
                    }
                }
            }
            displayProps.add("macros");
            displayProps.add(Json.quote(all));
        }

        return super.getUrlForWiki(request, entry, tag, props, displayProps);
    }


    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    private List<String[]> getFieldList(Entry entry) {
        List<String[]> fieldList =
            (List<String[]>) entry.getTransientProperty("db.fields");
        if (fieldList == null) {
            try {
                Connection connection = getConnection(entry);
                if (connection == null) {
                    System.err.println(
                        "DbTableTypeHandler.visit: no connection");

                    return null;
                }
                String table = entry.getValue(IDX_TABLE, (String) null);
                Statement stmt = SqlUtil.select(connection, "*",
                                     Misc.newList(table),
                                     Clause.and(new ArrayList<Clause>()), "",
                                     1, 0);
                SqlUtil.Iterator iter = new SqlUtil.Iterator(stmt);
                DbRecordFile.MyReader reader =
                    new DbRecordFile(null, entry).doMakeReader(connection,
                                     iter);
                new BufferedReader(reader).readLine();
                fieldList = reader.fieldList;
                entry.putTransientProperty("db.fields", fieldList);
                connection.close();
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }
        }

        return fieldList;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public RecordFile doMakeRecordFile(Request request, Entry entry)
            throws Exception {
        return new DbRecordFile(request, entry);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void initializeNewEntry(Request request, Entry entry)
            throws Exception {
        //Don't do this since we don't have the table set yet
    }




    /**
     * Class description
     *
     *
     * @version        $version$, Sat, Dec 8, '18
     * @author         Enter your name here...
     */
    public class DbRecordFile extends CsvFile {

        /** _more_ */
        private Request request;

        /** _more_ */
        private Entry entry;

        /** _more_ */
        private VisitInfo visitInfo;

        /**
         * _more_
         *
         * @param request _more_
         * @param entry _more_
         *
         * @throws IOException _more_
         */
        public DbRecordFile(Request request, Entry entry) throws IOException {
            this.request = request;
            this.entry   = entry;
        }

        /**
         * _more_
         *
         * @param visitInfo _more_
         * @param buffered _more_
         *
         * @return _more_
         *
         * @throws IOException _more_
         */
        public RecordIO doMakeInputIO(VisitInfo visitInfo, boolean buffered)
                throws IOException {
            //Grab this here so we can get the max from it in getInputStream below
            this.visitInfo = visitInfo;
            try {
                return new RecordIO(doMakeBufferedReader(visitInfo));
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }
            //            return super.doMakeInputIO(visitInfo, buffered);
        }

        /**
         * _more_
         *
         * @param visitInfo _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        private BufferedReader doMakeBufferedReader(VisitInfo visitInfo)
                throws Exception {
            TableInfo tableInfo = getTableInfo(entry);
            if (tableInfo == null) {
                System.err.println(
                    "Error: Could not find TableInfo for entry:" + entry);

                return null;
            }
            List columns = new ArrayList();
            for (ColumnInfo col : tableInfo.getColumns()) {
                columns.add(Json.map("name", Json.quote(col.getName()),
                                     "type", Json.quote(col.getTypeName())));
            }
            //              textReader.addTableProperty("columns", Json.list(columns));
            Connection connection = getConnection(entry);
            if (connection == null) {
                System.err.println("DbTableTypeHandler.visit: no connection");

                return null;
            }



            String table = entry.getValue(IDX_TABLE, (String) null);
            //        System.err.println("table:" + table + " idx:" + IDX_TABLE);

            if ( !Utils.stringDefined(table)) {
                System.err.println(
                    "DbTableTypeHadler.visit: no table defined");

                return null;
            }
            String what   = "*";
            int    offset = (visitInfo == null)
                            ? 0
                            : visitInfo.getSkip();

            int    max    = (visitInfo == null)
                            ? 1
                            : visitInfo.getQuickScan()
                              ? 1
                              : visitInfo.getMax();
            if (max <= 0) {
                max = 1;
            }

            List<Clause>   andClauses = new ArrayList<Clause>();
            List<Clause>   orClauses  = new ArrayList<Clause>();
            List<String[]> fieldList  = getFieldList(entry);
            if (fieldList != null) {
                for (String[] tuple : fieldList) {
                    String name = tuple[0];
                    String type = tuple[1];
                    if (type.equals("date")) {
                        String from = request.getString("search." + name
                                          + "_fromdate", null);
                        String to = request.getString("search." + name
                                        + "_todate", null);
                        if (from != null) {
                            andClauses.add(Clause.ge(name, from));
                        }
                        if (to != null) {
                            andClauses.add(Clause.le(name, to));
                        }

                    } else {
                        String s = request.getString("search." + name, null);
                        if (s != null) {
                            if (type.equals("string")) {
                                String sqlString = SqlUtil.wildCardBoth(s);
                                andClauses.add(Clause.like(name, sqlString));
                            } else {
                                andClauses.add(Clause.eq(name, s));
                            }
                        }
                    }
                }
            }
            Statement stmt = SqlUtil.select(connection, what,
                                            Misc.newList(table),
                                            Clause.and(andClauses),
                                            " LIMIT " + max + ((offset > 0)
                    ? " OFFSET " + offset + " "
                    : ""), -1, 0);
            SqlUtil.Iterator iter = new SqlUtil.Iterator(stmt);

            return new BufferedReader(doMakeReader(connection, iter));
        }


        /**
         * _more_
         *
         * @param connection _more_
         * @param iter _more_
         *
         * @return _more_
         */
        public MyReader doMakeReader(Connection connection,
                                     SqlUtil.Iterator iter) {
            return new MyReader(this, connection, iter);
        }


	@Override
	public boolean skip(VisitInfo visitInfo, Record record, int howMany)
            throws Exception {
	    //noop as the DB call does this
	}


        /**
         * Class description
         *
         *
         * @version        $version$, Sat, Feb 15, '20
         * @author         Enter your name here...
         */
        private class MyReader extends Reader {

            /** _more_ */
            private SimpleDateFormat sdf =
                RepositoryUtil.makeDateFormat("yyyyMMdd'T'HHmmss");

            /** _more_ */
            CsvFile csvFile;

            /** _more_ */
            List<String[]> fieldList;

            /** _more_ */
            private ResultSet results;

            /** _more_ */
            SqlUtil.Iterator iter;

            /** _more_ */
            ResultSetMetaData rsmd;

            /** _more_ */
            Connection connection;

            /** _more_ */
            String line;

            /** _more_ */
            int offset = 0;

            /**
             * _more_
             *
             * @param csvFile _more_
             * @param connection _more_
             * @param iter _more_
             */
            public MyReader(CsvFile csvFile, Connection connection,
                            SqlUtil.Iterator iter) {
                this.csvFile    = csvFile;
                this.connection = connection;
                this.iter       = iter;
            }

            /**
             * _more_
             */
            public void close() {
                try {
                    connection.close();
                } catch (Exception exc) {}
            }

            /** _more_ */
            int cnt = 0;

            /**
             * _more_
             *
             * @param cbuf _more_
             * @param off _more_
             * @param len _more_
             *
             * @return _more_
             */
            public int read(char[] cbuf, int off, int len) {
                if (line == null) {
                    line = readLine();
                    cnt++;
                    //              System.out.print("LINE #"+cnt + ":" + line);
                    offset = 0;
                }
                if (line == null) {
                    return -1;
                }
                int cIdx = 0;
                while ((offset < line.length()) && (cIdx < cbuf.length)) {
                    cbuf[cIdx++] = line.charAt(offset++);
                }
                if (offset >= line.length()) {
                    line = null;
                }

                return cIdx;
            }

            /**
             * _more_
             *
             * @return _more_
             */
            public String readLine() {
                try {
                    return readLineInner();
                } catch (Exception exc) {
                    throw new RuntimeException(exc);
                }
            }

            /**
             * _more_
             *
             * @return _more_
             *
             * @throws Exception _more_
             */
            public String readLineInner() throws Exception {
                if (results == null) {
                    results = iter.getNext();
                }
                if (results == null) {
                    if (rsmd == null) {
                        csvFile.putProperty("fields", "dummy");
                    }

                    return null;
                }
                if (rsmd == null) {
                    StringBuilder fields = new StringBuilder("");
                    rsmd = results.getMetaData();
                    int          columnCount = rsmd.getColumnCount();
                    List<Object> names       = new ArrayList<Object>();
                    fieldList = new ArrayList<String[]>();
                    for (int i = 1; i < columnCount + 1; i++) {
                        String extra = "";
                        String name  = rsmd.getColumnName(i);
                        String typeName =
                            rsmd.getColumnTypeName(i).toLowerCase();
                        String type = "string";
                        if (typeName.equals("int")
                                || typeName.equals("double")
                                || typeName.equals("bigint")
                                || typeName.equals("float8")) {
                            type = "double";
                        } else if (typeName.equals("date")) {
                            extra += " format=\"yyyyMMdd'T'HHmmss\" ";
                            type  = "date";
                        }
                        fieldList.add(new String[] { name, type });
                        //                      System.err.println("col:" + name + " type:" + typeName);
                        if (i > 1) {
                            fields.append(",");
                        }
                        fields.append(name);
                        fields.append("[type=" + type + extra + "]");
                        names.add(name);
                    }
                    entry.putTransientProperty("db.fields", this.fieldList);
                    //              System.err.println("DbTable fields:" + fields);
                    csvFile.putProperty("fields", fields.toString());

                    return CsvUtil.columnsToString(names, ",", true);
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
                    } else if (type == java.sql.Types.DATE) {
                        Date date = results.getDate(colIdx);
                        value = sdf.format(date);
                    } else {
                        value = results.getString(colIdx);
                    }
                    values.add(value);
                }

                return CsvUtil.columnsToString(values, ",", true);
            }
        }




        /**
         * _more_
         *
         * @param visitInfo _more_
         *
         * @return _more_
         */
        @Override
        public int getSkipLines(VisitInfo visitInfo) {
            return 1;
        }

        /**
         * _more_
         *
         * @param visitInfo _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        @Override
        public VisitInfo prepareToVisit(VisitInfo visitInfo)
                throws Exception {
            visitInfo      = super.prepareToVisit(visitInfo);
            this.visitInfo = visitInfo;

            return visitInfo;
        }
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
