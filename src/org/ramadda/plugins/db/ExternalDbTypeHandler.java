/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.db;


import org.ramadda.data.docs.*;

import org.ramadda.data.point.RowRecord;
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
import org.ramadda.util.JsonUtil;
import org.ramadda.util.Utils;
import org.ramadda.util.sql.*;
import org.ramadda.util.seesv.*;
import org.ramadda.util.seesv.Row;

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
@SuppressWarnings("unchecked")
public class ExternalDbTypeHandler extends PointTypeHandler {

    /** _more_ */
    private static int IDX = PointTypeHandler.IDX_LAST + 1;

    /** _more_ */
    public static final int IDX_DBID = IDX++;

    /** _more_ */
    public static final int IDX_TABLE = IDX++;

    /** _more_ */
    public static final int IDX_TABLE2 = IDX++;

    /** _more_ */
    public static final int IDX_JOIN1 = IDX++;

    /** _more_ */
    public static final int IDX_JOIN2 = IDX++;


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
    public ExternalDbTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }


    /**
     *
     * @param entry _more_
     * @param name _more_
     *
     * @return _more_
     */
    private List<String> getEnums(Entry entry, String name) {
        try {
            List<String> enums =
                (List<String>) entry.getTransientProperty("db.enums." + name);
            if (enums == null) {
                try (Connection connection = getConnection(entry)) {
                    String table = entry.getStringValue(IDX_TABLE, (String) null);
                    Statement stmt = SqlUtil.select(connection,
                                         SqlUtil.distinct(name),
                                         Utils.makeListFromValues(table), null, null,
                                         1000, 0);

                    String[] values =
                        SqlUtil.readString(
                            getRepository().getDatabaseManager().getIterator(
                                stmt), 1);

                    stmt.close();
                    enums = (List<String>) Misc.toList(values);
                }
                entry.putTransientProperty("db.enums." + name, enums);
            }

            return enums;
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
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
     * @param request _more_
     * @param entry _more_
     * @param fromImport _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void initializeNewEntry(Request request, Entry entry,
                                   boolean fromImport)
            throws Exception {
        //Don't do this since we don't have the table set yet
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
        return getConnection(entry.getStringValue(IDX_DBID, (String) null));
    }


    /**
     * _more_
     *
     * @param connection _more_
     *
     * @throws Exception _more_
     */
    private void closeConnection(Connection connection) throws Exception {
        connection.close();
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
                Utils.getProperties(entry.getStringValue(IDX_PROPERTIES,
                    (String) ""));
            recordProps.putAll(Utils.makeHashtable(displayProps));
            boolean displayAll =
                Misc.equals(recordProps.get("request.*.display"), "true");

            for (String[] tuple : fieldList) {
                String name = tuple[0];
                String type = tuple[1];
                if (recordProps.get("request." + name + ".type") != null) {
                    type = ((String) recordProps.get("request." + name
                            + ".type")).trim();
                }
                if ( !displayAll
                        && !Misc.equals(recordProps.get("request." + name
                            + ".display"), "true")) {
                    continue;
                }
                if (all != null) {
                    all += ",";
                } else {
                    all = "";
                }
                all += name;
                if ( !displayProps.contains("request." + name + ".label")) {
                    displayProps.add("request." + name + ".label");
                    displayProps.add(JsonUtil.quote(Utils.makeLabel(name)));
                }
                displayProps.add("request." + name + ".urlarg");
                displayProps.add(JsonUtil.quote("search." + name));

                displayProps.add("request." + name + ".type");
                displayProps.add(JsonUtil.quote(type));
                if (type.equals("enumeration")) {
                    String enums = (String) recordProps.get("request." + name
                                       + ".values");
                    if (enums == null) {
                        List<String> tmp = getEnums(entry, name);
                        enums = Utils.join(tmp, ",");
                    }
                    if (enums != null) {
                        displayProps.add("request." + name + ".values");
                        displayProps.add(JsonUtil.quote(enums));
                    }
                }
            }
            displayProps.add("requestFields");
            displayProps.add(JsonUtil.quote(all));
        }

        return super.getUrlForWiki(request, entry, tag, props, displayProps);
    }


    /**
     *
     * @param entry _more_
     *
     * @return _more_
     */
    private String getWhat(Entry entry) {
        Hashtable recordProps =
            Utils.getProperties(entry.getStringValue(IDX_PROPERTIES, (String) ""));

        return Utils.getProperty(recordProps, "what", "*");
    }

    /**
     *
     * @param entry _more_
     *
     * @return _more_
     */
    private List getTableList(Entry entry) {
        String table = entry.getStringValue(IDX_TABLE, (String) null);
        if ( !Utils.stringDefined(table)) {
            System.err.println("DbTableTypeHadler.visit: no table defined");

            return null;
        }
        List tableList = Utils.makeListFromValues(table);
        Hashtable recordProps =
            Utils.getProperties(entry.getStringValue(IDX_PROPERTIES, (String) ""));
        String join = Utils.getProperty(recordProps, "jointables",
                                        (String) null);
        if (join != null) {
            tableList.addAll(Utils.split(join, ","));
        }

        return tableList;
    }


    /**
     *
     * @param entry _more_
     *
     * @return _more_
     */
    private List<Clause> getInitialClauses(Entry entry) {
        List<Clause> andClauses = new ArrayList<Clause>();

        Hashtable recordProps =
            Utils.getProperties(entry.getStringValue(IDX_PROPERTIES, (String) ""));
        String joinFields = Utils.getProperty(recordProps, "joinfields",
                                (String) null);
        //"salaries.emp_no","employees.emp_no";
        if (joinFields != null) {
            for (String tuple : Utils.split(joinFields, ",")) {
                List<String> pair = Utils.split(tuple, ":");
                if (pair.size() == 2) {
                    andClauses.add(Clause.join(pair.get(0), pair.get(1)));
                }
            }
        }

        return andClauses;
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
            (List<String[]>) entry.getTransientProperty("db.fieldlist");
        if (fieldList == null) {
            try {
                Connection connection = getConnection(entry);
                if (connection == null) {
                    System.err.println(
                        "DbTableTypeHandler.visit: no connection");

                    return null;
                }

                List tableList = getTableList(entry);
                if (tableList == null) {
                    return fieldList;
                }
                List<Clause> clauses = getInitialClauses(entry);
                Statement stmt = SqlUtil.select(connection, getWhat(entry),
                                     tableList, Clause.and(clauses), "", 1,
                                     0);
                SqlUtil.Iterator iter = new SqlUtil.Iterator(stmt);
                DbRecordFile.MyRecord record =
                    new DbRecordFile(null, entry).doMakeRecord(connection,
                                     iter);
                fieldList = record.fieldList;
                entry.putTransientProperty("db.fieldlist", fieldList);
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
     * @param properties _more_
     * @param equestProperties _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public RecordFile doMakeRecordFile(Request request, Entry entry,
                                       Hashtable properties,
                                       Hashtable equestProperties)
            throws Exception {
        return new DbRecordFile(request, entry);
    }


    /**
     * Class description
     *
     * @version        $version$, Sat, Dec 8, '18
     * @author         Enter your name here...
     */
    public class DbRecordFile extends RecordFile {

        /** _more_ */
        private Request request;

        /** _more_ */
        private Entry entry;

        /** _more_ */
        List<RecordField> fields;


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
         *
         * @return _more_
         */
        @Override
        public BaseRecord doMakeRecord(VisitInfo visitInfo) {
            try {
                Connection connection = getConnection(entry);

                return doMakeRecord(connection,
                                    makeIterator(connection, visitInfo));
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }
        }

        /**
         * _more_
         *
         * @param connection _more_
         * @param iter _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        public MyRecord doMakeRecord(Connection connection,
                                     SqlUtil.Iterator iter)
                throws Exception {
            return new MyRecord(this, connection, iter);

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
            try {
                return new RecordIO((BufferedReader) null);
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }
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
        public VisitInfo prepareToVisit(VisitInfo visitInfo)
                throws Exception {
            return super.prepareToVisit(visitInfo);
        }

        /**
         * _more_
         *
         * @param failureOk _more_
         *
         * @return _more_
         */
        public List<RecordField> doMakeFields(boolean failureOk) {
            if (fields == null) {
                getFieldList(entry);
                fields = (List<RecordField>) entry.getTransientProperty(
                    "db.fields");
            }

            return fields;
        }


        /**
         * _more_
         *
         * @param connection _more_
         * @param visitInfo _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        private SqlUtil.Iterator makeIterator(Connection connection,
                VisitInfo visitInfo)
                throws Exception {

            if (connection == null) {
                System.err.println("DbTableTypeHandler.visit: no connection");

                return null;
            }

            Hashtable recordProps =
                Utils.getProperties(entry.getStringValue(IDX_PROPERTIES,
                    (String) ""));

            List tableList = getTableList(entry);
            if (tableList == null) {
                return null;
            }

            String what   = getWhat(entry);

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

            List<Clause>   andClauses = getInitialClauses(entry);
            List<String[]> fieldList  = getFieldList(entry);
            if (fieldList != null) {
                for (String[] tuple : fieldList) {
                    String name = tuple[0];
                    String type = tuple[1];
                    type = Utils.getProperty(recordProps,
                                             "request." + name + ".type",
                                             type);
                    String tablePrefix = Utils.getProperty(recordProps,
                                             name + ".tableprefix",
                                             (String) null);
                    String fieldName = name;
                    if (tablePrefix != null) {
                        fieldName = tablePrefix + "." + fieldName;
                    }
                    if (type.equals("date")) {
                        String from = request.getString("search." + name
                                          + "_fromdate", null);
                        String to = request.getString("search." + name
                                        + "_todate", null);
                        if (from != null) {
                            andClauses.add(Clause.ge(fieldName, from));
                        }
                        if (to != null) {
                            andClauses.add(Clause.le(fieldName, to));
                        }

                    } else {
                        String s = request.getString("search." + name, null);
                        if (s != null) {
                            if (type.equals("string")) {
                                String sqlString = SqlUtil.wildCardBoth(s);
                                andClauses.add(Clause.like(fieldName,
                                        sqlString));
                            } else {
                                andClauses.add(Clause.eq(fieldName, s));
                            }
                        }
                    }
                }
            }

            String orderBy  = null;
            String extraSql = "";
            if (request.defined(ARG_GROUPBY)) {
                String groupBy =
                    SqlUtil.sanitize(request.getString(ARG_GROUPBY));
                String aggColumn =
                    SqlUtil.sanitize(request.getString(ARG_AGG));
                extraSql += SqlUtil.groupBy(groupBy);
                String aggType = request.getEnum(ARG_AGG_TYPE, "count",
                                     "sum", "min", "max", "avg");
                if ((aggColumn == null) || aggColumn.equals("")) {
                    aggColumn = groupBy;
                }
                String agg;
                if (aggType.equals("min")) {
                    agg = SqlUtil.min(aggColumn);
                } else if (aggType.equals("max")) {
                    agg = SqlUtil.max(aggColumn);
                } else if (aggType.equals("sum")) {
                    agg = SqlUtil.sum(aggColumn);
                } else if (aggType.equals("avg")) {
                    agg = SqlUtil.avg(aggColumn);
                } else {
                    agg = SqlUtil.count(aggColumn);
                }
                what    = groupBy + "," + agg;
                orderBy = agg;
            }

            if (orderBy == null) {
                orderBy = SqlUtil.sanitize(request.getString("orderBy"));
            }
            if (Utils.stringDefined(orderBy)) {
                extraSql += SqlUtil.orderBy(orderBy,
                                            request.getString("descending",
                                                "true").equals("true"));
            }
            extraSql += SqlUtil.limit(max, offset);
            //            SqlUtil.debug = true;
            Statement stmt = SqlUtil.select(connection, what, tableList,
                                            Clause.and(andClauses), extraSql,
                                            -1, 0);
            //            SqlUtil.debug = false;

            return new SqlUtil.Iterator(stmt);

        }


        /**
         * _more_
         *
         * @param visitInfo _more_
         * @param record _more_
         * @param howMany _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        @Override
        public boolean skip(VisitInfo visitInfo, BaseRecord record,
                            int howMany)
                throws Exception {
            //noop as the DB call does this
            return true;
        }


        /**
         * Class description
         *
         *
         * @version        $version$, Mon, Feb 17, '20
         * @author         Enter your name here...
         */
        private class MyRecord extends RowRecord {

            /** _more_ */
            private SimpleDateFormat sdf =
                RepositoryUtil.makeDateFormat("yyyyMMdd'T'HHmmss");

            /** _more_ */
            Connection connection;

            /** _more_ */
            List<String[]> fieldList;


            /** _more_ */
            SqlUtil.Iterator iter;

            /** _more_ */
            ResultSetMetaData rsmd;

            /** _more_ */
            ResultSet results;

            /** _more_ */
            DbRecordFile recordFile;

            /**
             * _more_
             *
             * @param file _more_
             * @param connection _more_
             * @param iter _more_
             *
             * @throws Exception _more_
             */
            MyRecord(DbRecordFile file, Connection connection,
                     SqlUtil.Iterator iter)
                    throws Exception {
                super(file, null);
                recordFile      = file;
                this.connection = connection;
                this.iter       = iter;
                makeFields();
            }

            /**
             * _more_
             *
             * @throws Exception _more_
             */
            private void makeFields() throws Exception {
                Hashtable recordProps =
                    Utils.getProperties(entry.getStringValue(IDX_PROPERTIES,
                        (String) ""));
                List<RecordField> fields = recordFile.fields =
                                               new ArrayList<RecordField>();
                results = iter.getNext();
                if (results == null) {
                    fields.add(new RecordField("dummy", "dummy", "",
                            "string", 1, ""));
                    closeConnection(connection);
                    initFields(fields);

                    return;
                }
                rsmd = results.getMetaData();
                int columnCount = rsmd.getColumnCount();
                fieldList = new ArrayList<String[]>();
                for (int i = 1; i < columnCount + 1; i++) {
                    String extra = "";
                    String name  = rsmd.getColumnName(i);
                    name = name.replaceAll("(.*)[\\(]",
                                           "$1_of_").replaceAll("\\)", "");
                    String label = rsmd.getColumnLabel(i);
                    label = label.replaceAll("(.*)[\\(]",
                                             "$1_of_").replaceAll("\\)", "");

                    label = Utils.makeLabel(label);
                    int    dbType = rsmd.getColumnType(i);
                    String type   = "string";
                    if ((dbType == java.sql.Types.DOUBLE)
                            || (dbType == java.sql.Types.NUMERIC)
                            || (dbType == java.sql.Types.REAL)
                            || (dbType == java.sql.Types.DECIMAL)) {
                        type = "double";
                    } else if (dbType == java.sql.Types.FLOAT) {
                        type = "double";
                    } else if ((dbType == java.sql.Types.INTEGER)
                               || (dbType == java.sql.Types.BIGINT)
                               || (dbType == java.sql.Types.SMALLINT)
                               || (dbType == java.sql.Types.TINYINT)) {
                        type = "integer";
                    } else if (dbType == java.sql.Types.DATE) {
                        type = "date";
                    } else {
                        type = "string";
                    }
                    fieldList.add(new String[] { name, type });
                    RecordField field = new RecordField(name, label, "",
                                            type, fields.size() + 1, "");
                    //                    System.err.println("F:" + field);
                    if (type.equals("date")) {
                        field.setDateFormat(sdf);
                    }
                    fields.add(field);
                }
                if (entry.getTransientProperty("db.fieldlist") == null) {
                    entry.putTransientProperty("db.fieldlist", fieldList);
                }
                //              System.err.println("DbTable fields:" + fields);
                initFields(fields);
                entry.putTransientProperty("db.fields", fields);
            }

            /**
             * _more_
             *
             * @param recordIO _more_
             *
             * @return _more_
             *
             * @throws Exception _more_
             */
            public Row readNextRow(RecordIO recordIO) throws Exception {
                if (results == null) {
                    results = iter.getNext();
                }
                if (results == null) {
                    closeConnection(connection);

                    return null;
                }
                List<Object> values = new ArrayList<Object>();
                for (int col = 0; col < rsmd.getColumnCount(); col++) {
                    int    colIdx = col + 1;
                    int    type   = rsmd.getColumnType(colIdx);
                    Object value  = null;
                    //TODO: Handle more dates
                    if ((type == java.sql.Types.DOUBLE)
                            || (type == java.sql.Types.NUMERIC)
                            || (type == java.sql.Types.REAL)
                            || (type == java.sql.Types.DECIMAL)) {
                        value = Double.valueOf(results.getDouble(colIdx));
                    } else if (type == java.sql.Types.FLOAT) {
                        //                        value = new Float(results.getFloat(colIdx));
                        value = Double.valueOf(results.getFloat(colIdx));
                    } else if ((type == java.sql.Types.INTEGER)
                               || (type == java.sql.Types.BIGINT)
                               || (type == java.sql.Types.SMALLINT)
                               || (type == java.sql.Types.TINYINT)) {
                        //                        value = Integer.valueOf(results.getInt(colIdx));
                        value = Double.valueOf(results.getInt(colIdx));
                    } else if (type == java.sql.Types.DATE) {
                        value = results.getDate(colIdx);
                    } else {
                        value = results.getString(colIdx);
                    }
                    values.add(value);
                }
                results = null;

                return new Row(values);
            }
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
    public void addColumnToEntryForm(Request request, Entry parentEntry,
				     Entry entry,
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
                               : entry.getStringValue(IDX_DBID, (String) null));
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
                              : entry.getStringValue(IDX_TABLE, (String) null);
                formBuffer.append(
                    formEntry(
                        request, column.getLabel() + ":",
                        HtmlUtils.select(column.getEditArg(), tables, name)));

                return;
            }
        }
        super.addColumnToEntryForm(request, parentEntry, entry, column, formBuffer,
                                   values, state, formInfo, baseTypeHandler);
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
        String table = entry.getStringValue(IDX_TABLE, (String) null);

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
        String dbid = entry.getStringValue(IDX_DBID, (String) null);
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
                    closeConnection(connection);
                }
            }
            dbToTables.put(dbid, tableInfos);
        }

        return tableInfos;
    }

}
