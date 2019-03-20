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

package org.ramadda.repository.database;


import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.log4j.Logger;


import org.ramadda.repository.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Log4jPrintWriter;
import org.ramadda.util.Utils;

import org.ramadda.util.sql.Clause;
import org.ramadda.util.sql.SqlUtil;


import org.w3c.dom.*;

import ucar.unidata.util.Counter;
import ucar.unidata.util.DateUtil;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;


import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlEncoder;
import ucar.unidata.xml.XmlUtil;


import java.io.*;

import java.io.File;

import java.lang.reflect.*;



import java.net.*;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;

import javax.sql.DataSource;


/**
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
public class DatabaseManager extends RepositoryManager implements SqlUtil
    .ConnectionManager {

    //Try for something that won't ever be in a string

    /** _more_ */
    public static final String COMPRESS_PREFIX = "___gzip:";

    /** _more_ */
    public static final String PROP_DB_DRIVER = "ramadda.db.${db}.driver";

    /** _more_ */
    public static final String PROP_DB_PASSWORD = "ramadda.db.${db}.password";

    /** _more_ */
    public static final String PROP_DB_SCRIPT = "ramadda.db.script";

    /** _more_ */
    public static final String PROP_DB_URL = "ramadda.db.${db}.url";

    /** _more_ */
    public static final String PROP_DB_USER = "ramadda.db.${db}.user";


    /** _more_ */
    private long myTime = System.currentTimeMillis();

    /** _more_ */
    private final LogManager.LogId LOGID =
        new LogManager.LogId(
            "org.ramadda.repository.database.DatabaseManager");

    /** _more_ */
    //NOTE: When we had a non-zero timeout we got a memory leak
    private static final int TIMEOUT = 0;

    /** _more_ */
    private Counter numberOfSelects = new Counter();

    /** _more_ */
    private static final int DUMPTAG_TABLE = 1;

    /** _more_ */
    private static final int DUMPTAG_ROW = 2;

    /** _more_ */
    private static final int DUMPTAG_END = 3;


    /** _more_ */
    private String db;

    /** _more_ */
    private static final String DB_MYSQL = "mysql";

    /** _more_ */
    private static final String DB_H2 = "h2";

    /** _more_ */
    private static final String DB_DERBY = "derby";

    /** _more_ */
    private static final String DB_POSTGRES = "postgres";

    /** _more_ */
    private static final String DB_ORACLE = "oracle";

    /** _more_ */
    private String connectionURL;

    /** _more_ */
    private BasicDataSource dataSource;

    /** _more_ */
    private Hashtable<String, BasicDataSource> externalDataSources =
        new Hashtable<String, BasicDataSource>();


    /** Keeps track of active connections */
    //    private Hashtable<Connection, ConnectionInfo> connectionMap =
    //        new Hashtable<Connection, ConnectionInfo>();

    private final Object CONNECTION_MUTEX = new Object();

    /** _more_ */
    private List<ConnectionInfo> connectionInfos =
        new ArrayList<ConnectionInfo>();



    /** _more_ */
    private List<String> scourMessages = new ArrayList<String>();

    /** _more_ */
    private int totalScours = 0;

    /** _more_ */
    private boolean runningCheckConnections = false;

    /** _more_ */
    private boolean haveInitialized = false;


    /**
     * _more_
     *
     * @param repository _more_
     */
    public DatabaseManager(Repository repository) {
        super(repository);
        db = (String) getRepository().getProperty(PROP_DB);
        if (db == null) {
            throw new IllegalStateException("Must have a " + PROP_DB
                                            + " property defined");
        }
        db = db.trim();
    }





    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public void init() throws Exception {
        if (haveInitialized) {
            return;
        }
        haveInitialized = true;
        System.setProperty("jdbc.drivers",
        "org.apache.derby.jdbc.EmbeddedDriver:com.mysql.jdbc.Driver:org.postgresql.Driver:org.Oracle.Driver");
        //"org.apache.derby.jdbc.EmbeddedDriver:com.mysql.cj.jdbc.Driver:org.postgresql.Driver:org.Oracle.Driver");


        SqlUtil.setConnectionManager(this);

        dataSource = doMakeDataSource();
        Statement statement = getConnection().createStatement();
        if (db.equals(DB_MYSQL)) {
            statement.execute("set time_zone = '+0:00'");
        }
        closeAndReleaseConnection(statement);
        //        Misc.run(this, "checkConnections", null);
    }


    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public void reInitialize() throws Exception {
        if (dataSource != null) {
            BasicDataSource bds = (BasicDataSource) dataSource;
            try {
                bds.close();
            } catch (Exception exc) {
                logError("Closing data source", exc);
            }
            dataSource = doMakeDataSource();
        }
    }




    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public void initComplete() throws Exception {
        //If nothing  in dummy table then add an entry
        BasicDataSource bds   = (BasicDataSource) dataSource;
        int             count = getCount(Tables.DUMMY.NAME, null);
        if (count == 0) {
            executeInsert(Tables.DUMMY.INSERT, new Object[] { "dummyentry" });
        }
        bds.setValidationQuery("select * from dummy");

        /*
        System.err.println("min evict:" +bds.getMinEvictableIdleTimeMillis()/1000);
        System.err.println("test on borrow:"+bds.getTestOnBorrow());
        System.err.println("test while idle:"+bds.getTestWhileIdle());
        System.err.println("time between runs:"+bds.getTimeBetweenEvictionRunsMillis()/1000);
        */
    }




    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private BasicDataSource doMakeDataSource() throws Exception {
        try {
            scourMessages = new ArrayList<String>();
            totalScours   = 0;

            String userName = (String) getRepository().getProperty(
                                  PROP_DB_USER.replace("${db}", db));
            String password = (String) getRepository().getProperty(
                                  PROP_DB_PASSWORD.replace("${db}", db));


            connectionURL = getStorageManager().localizePath(
                (String) getRepository().getProperty(
                    PROP_DB_URL.replace("${db}", db)));


            //ramadda.db.derby.url=jdbc:derby:${storagedir}/derby/${db.name};create=true;
            connectionURL = connectionURL.replace("%repositorydir%",
                    getStorageManager().getRepositoryDir().toString());
            connectionURL = connectionURL.replace("%db.name%",
                    getRepository().getProperty("db.name", "repository"));

            connectionURL = connectionURL.trim();
            System.err.println("RAMADDA: JDBC url:" + connectionURL
                               + ((userName != null)
                                  ? " user name:" + userName
                                  : ""));

            String encryptPassword =
                getStorageManager().getEncryptionPassword();
            if ((encryptPassword != null) && isDatabaseDerby()) {
                System.err.println("encrypting");
                connectionURL += "dataEncryption=true;bootPassword="
                                 + encryptPassword + ";";
            }
            BasicDataSource ds = makeDataSource(connectionURL, userName,
                                     password);

            return ds;
        } catch (Exception exc) {
            System.err.println(
                "RAMADDA: error initializing database connection:" + exc);
            exc.printStackTrace();

            throw exc;
        }
    }



    /**
     * _more_
     *
     * @param jdbc _more_
     *
     * @return _more_
     */
    private String getDbType(String jdbc) {
        if (jdbc.indexOf("mysql") >= 0) {
            return DB_MYSQL;
        }
        if (jdbc.indexOf("postgres") >= 0) {
            return DB_POSTGRES;
        }
        if (jdbc.indexOf("oracle") >= 0) {
            return DB_ORACLE;
        }
        if (jdbc.indexOf("h2") >= 0) {
            return DB_H2;
        }
        if (jdbc.indexOf("derby") >= 0) {
            return DB_DERBY;
        }

        throw new IllegalArgumentException(
            "Could not determine database type:" + jdbc);
    }

    /**
     * _more_
     *
     * @param connectionUrl _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private String loadDriver(String connectionUrl) throws Exception {
        String dbType = getDbType(connectionUrl);
        String driverClassPropertyName = PROP_DB_DRIVER.replace("${db}",
                                             dbType);
        String driverClassName =
            (String) getRepository().getProperty(driverClassPropertyName);

        Misc.findClass(driverClassName);

        return driverClassName;
    }


    /**
     * _more_
     *
     * @param connectionUrl _more_
     * @param userName _more_
     * @param password _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public BasicDataSource makeDataSource(String connectionUrl,
                                          String userName, String password)
            throws Exception {
        String          driverClassName = loadDriver(connectionUrl);
        BasicDataSource ds              = new BasicDataSource();


        //ds.setMaxActive(getRepository().getProperty(PROP_DB_POOL_MAXACTIVE, 100));
        //ds.setMaxIdle(getRepository().getProperty(PROP_DB_POOL_MAXIDLE,100));
        ds.setMaxTotal(getRepository().getProperty(PROP_DB_POOL_MAXACTIVE,
                100));
        //30 second time out
        ds.setMaxWaitMillis(1000 * 30);
        //60 seconds
        ds.setRemoveAbandonedTimeout(60);
        //ds.setRemoveAbandoned(true);
        ds.setRemoveAbandonedOnBorrow(true);
        ds.setRemoveAbandonedOnMaintenance(true);

        //        System.err.println("DatabaseManager.makeDataSource: url="  + connectionUrl);
        //        System.err.println("JDBC driver class:" + driverClassName + " db type:" + dbType);

        ds.setDriverClassName(driverClassName);
        ds.setUsername(userName);
        ds.setPassword(password);
        ds.setUrl(connectionURL);



        /*
        Logger logger = getLogManager().getLogger(LOGID);
        if (logger != null) {
            ds.setLogWriter(new Log4jPrintWriter(logger));
        }
        */

        return ds;
    }




    /**
     * _more_
     *
     *
     * @param prefix _more_
     * @param id _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Connection getExternalConnection(String prefix, String id)
            throws Exception {
        String full = prefix + "." + id;
        String connectionUrl = getRepository().getProperty(full + ".url",
                                   getRepository().getProperty(prefix
                                       + ".url", (String) null));
        String user = getRepository().getProperty(full + ".user",
                          getRepository().getProperty(prefix + ".user",
                              (String) null));
        String password = getRepository().getProperty(full + ".password",
                              getRepository().getProperty(prefix
                                  + ".password", (String) null));

        if (connectionUrl == null) {
            System.err.println("No connection url property for:" + full);

            return null;
        }


        //Load the jdbc driver class
        String     driverClassName = loadDriver(connectionUrl);

        Properties connectionProps = new Properties();
        if (user != null) {
            connectionProps.put("user", user);
        }
        if (password != null) {
            connectionProps.put("password", password);
        }


        Connection conn = DriverManager.getConnection(connectionUrl,
                              connectionProps);
        if (conn == null) {
            System.err.println("Got null connection for url:"
                               + connectionUrl);

            return null;
        }

        //TODO: do connection pooling of connections sometime
        if (true) {
            return conn;
        }

        BasicDataSource ds = getExternalDataSource(prefix);
        if (ds == null) {
            return null;
        }

        return ds.getConnection();
    }

    /**
     * _more_
     *
     * @param dbId _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public BasicDataSource getExternalDataSource(String dbId)
            throws Exception {

        String connectionUrl = getRepository().getProperty("ramadda.db."
                                   + dbId + ".url", (String) null);
        String user = getRepository().getProperty("ramadda.db." + dbId
                          + ".user", (String) null);
        String password = getRepository().getProperty("ramadda.db." + dbId
                              + ".password", (String) null);

        if (connectionUrl == null) {
            return null;
        }

        BasicDataSource ds = externalDataSources.get(connectionUrl);
        if (ds != null) {
            return ds;
        }

        ds = makeDataSource(connectionUrl, user, password);
        externalDataSources.put(connectionUrl, ds);

        return ds;
    }




    /**
     * _more_
     *
     * @return _more_
     */
    private List<ConnectionInfo> getConnectionInfos() {
        synchronized (connectionInfos) {
            return new ArrayList<ConnectionInfo>(connectionInfos);
        }

        /*
        Hashtable<Connection, ConnectionInfo> tmp = new Hashtable<Connection,
                                                        ConnectionInfo>();
        synchronized (connectionMap) {
            tmp.putAll(connectionMap);
        }
        List<ConnectionInfo> infos = new ArrayList<ConnectionInfo>();
        for (Enumeration keys = tmp.keys(); keys.hasMoreElements(); ) {
            Connection     connection = (Connection) keys.nextElement();
            ConnectionInfo info       = tmp.get(connection);
            infos.add(info);
        }
        return infos;
        */
    }




    /**
     * _more_
     */
    public void checkConnections() {
        if (runningCheckConnections) {
            return;
        }
        runningCheckConnections = true;
        while (true) {
            try {
                Misc.sleep(5000);
                long now = System.currentTimeMillis();
                //Scour after 5 minutes
                int seconds =
                    getRepository().getProperty(PROP_DB_POOL_TIMEUNTILCLOSED,
                        300);
                for (ConnectionInfo info : getConnectionInfos()) {
                    //If a connection has been out for more than seconds then close it
                    if (now - info.time > seconds * 1000) {
                        getLogManager().logError("SCOURED @" + new Date()
                                + " info.date: " + new Date(info.time)
                                + " info.id: " + info.myCnt + "<stack>"
                                + "  msg:" + info.msg + "<br>  Where:"
                                + info.where + "</stack>");

                        synchronized (scourMessages) {
                            while (scourMessages.size() > 100) {
                                scourMessages.remove(0);
                            }
                            totalScours++;
                            scourMessages.add("SCOURED @" + new Date()
                                    + " info.date: " + new Date(info.time)
                                    + " info.id: " + info.myCnt + "<br>"
                                    + "  msg:" + info.msg + "  Where:"
                                    + info.where);
                        }
                        closeConnection(info.connection);
                    }
                }
            } catch (Exception exc) {
                getLogManager().logError(
                    "CONNECTION: Error checking connection", exc);
            }
        }
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param dbSB _more_
     *
     * @throws Exception _more_
     */
    public void addStatistics(Request request, StringBuffer dbSB)
            throws Exception {

        BasicDataSource bds    = (BasicDataSource) dataSource;

        StringBuffer    poolSB = new StringBuffer();
        poolSB.append("&nbsp;&nbsp;#active:" + bds.getNumActive()
                      + "<br>&nbsp;&nbsp;#idle:" + bds.getNumIdle()
        //                      + "<br>&nbsp;&nbsp;max active: " + bds.getMaxActive()
        //                      + "<br>&nbsp;&nbsp;max idle:" + bds.getMaxIdle());
        + "<br>&nbsp;&nbsp;max active: " + bds.getMaxTotal());

        poolSB.append("<br># of open selects:" + numberOfSelects.getCount());
        poolSB.append("<br>");

        long                 time            = System.currentTimeMillis();
        StringBuffer         openConnections = new StringBuffer();
        List<ConnectionInfo> infos           = getConnectionInfos();
        for (ConnectionInfo info : infos) {
            openConnections.append(HtmlUtils.makeShowHideBlock("Open for:"
                    + ((time - info.time) / 1000)
                    + " seconds", HtmlUtils.pre(info.msg + "\nStack:"
                        + info.where), false));
        }
        if (infos.size() > 0) {
            poolSB.append(HtmlUtils.br());
            poolSB.append(msgLabel("Open connections"));
            poolSB.append(openConnections);
        }


        StringBuffer msgb = new StringBuffer();
        synchronized (scourMessages) {
            if (totalScours > 0) {
                msgb.append("Total scours:" + totalScours + HtmlUtils.p());
            }
            for (String msg : scourMessages) {
                msgb.append("<pre>" + msg + "</pre>");
                msgb.append("<hr>");
            }
            if (scourMessages.size() > 0) {
                poolSB.append(
                    HtmlUtils.insetLeft(
                        HtmlUtils.makeShowHideBlock(
                            msg("Scoured Connections"), msgb.toString(),
                            false), 20));
            }
        }


        dbSB.append(
            HtmlUtils.insetLeft(
                HtmlUtils.makeShowHideBlock(
                    msg("Connection Pool"), poolSB.toString(), false), 20));

        /**
         * Don't show the entry break down as it can be kind of slow
         * dbSB.append(HtmlUtils.br());
         * dbSB.append("<table>\n");
         * String[] names = { msg("Users"), msg("Associations"),
         *                  msg("Metadata Items") };
         * String[] tables = { Tables.USERS.NAME, Tables.ASSOCIATIONS.NAME,
         *                   Tables.METADATA.NAME };
         * for (int i = 0; i < tables.length; i++) {
         *   dbSB.append(HtmlUtils.row(HtmlUtils.cols(""
         *           + getDatabaseManager().getCount(tables[i].toLowerCase(),
         *               new Clause()), names[i])));
         * }
         *
         *
         * dbSB.append(
         *   HtmlUtils.row(
         *       HtmlUtils.colspan(HtmlUtils.bold(msgLabel("Types")), 2)));
         * int total = 0;
         * dbSB.append(HtmlUtils.row(HtmlUtils.cols(""
         *       + getDatabaseManager().getCount(Tables.ENTRIES.NAME,
         *           new Clause()), msg("Total entries"))));
         * for (TypeHandler typeHandler : getRepository().getTypeHandlers()) {
         *   if (typeHandler.isType(TypeHandler.TYPE_ANY)) {
         *       continue;
         *   }
         *   int cnt = getCount(Tables.ENTRIES.NAME,
         *                      Clause.eq(Tables.ENTRIES.COL_TYPE,
         *                                typeHandler.getType()));
         *
         *   String url =
         *       HtmlUtils.href(
         *           request.makeUrl(
         *               getRepository().getSearchManager().URL_SEARCH_FORM,
         *               ARG_TYPE,
         *               typeHandler.getType()), typeHandler.getLabel());
         *   dbSB.append(HtmlUtils.row(HtmlUtils.cols("" + cnt, url)));
         * }
         *
         *
         * dbSB.append("</table>\n");
         */
    }




    /**
     * Class ConnectionWrapper _more_
     *
     *
     * @author RAMADDA Development Team
     * @version $Revision: 1.3 $
     */
    private static class ConnectionInfo {

        /** _more_ */
        static int cnt = 0;

        /** _more_ */
        int myCnt = cnt++;


        /** _more_ */
        Connection connection;

        /** _more_ */
        long time;

        /** _more_ */
        String where;

        /** _more_ */
        String msg;

        /**
         * _more_
         *
         * @param connection _more_
         *
         * @param msg _more_
         */
        ConnectionInfo(Connection connection, String msg) {
            this.connection = connection;
            this.time       = System.currentTimeMillis();
            this.msg        = msg;
            where           = Misc.getStackTrace();
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public String toString() {
            //            return "info:" + connection + " ";
            //            return "info:" + msg +"\n" + where + " ";
            return where;
        }


    }


    /**
     * _more_
     *
     *
     * @param query _more_
     * @return _more_
     *
     * @throws Exception _more_
     */



    /**
     * _more_
     *
     * @param query _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public PreparedStatement getPreparedStatement(String query)
            throws Exception {
        return getConnection().prepareStatement(query);
    }


    /**
     * _more_
     *
     * @param table _more_
     * @param colId _more_
     * @param id _more_
     * @param names _more_
     * @param values _more_
     *
     * @throws Exception _more_
     */
    public void update(String table, String colId, String id, String[] names,
                       Object[] values)
            throws Exception {
        PreparedStatement statement =
            getPreparedStatement(SqlUtil.makeUpdate(table, colId, names));
        for (int i = 0; i < values.length; i++) {
            Object value = values[i];
            if (value == null) {
                statement.setNull(i + 1, java.sql.Types.VARCHAR);
            } else if (value instanceof Date) {
                setDate(statement, i + 1, (Date) value);
            } else if (value instanceof Boolean) {
                boolean b = ((Boolean) value).booleanValue();
                statement.setInt(i + 1, (b
                                         ? 1
                                         : 0));
            } else {
                statement.setObject(i + 1, value);
            }
        }
        statement.setString(values.length + 1, id);
        statement.execute();
        closeAndReleaseConnection(statement);
    }

    /**
     * _more_
     *
     * @param table _more_
     * @param clause _more_
     * @param names _more_
     * @param values _more_
     *
     * @throws Exception _more_
     */
    public void update(String table, Clause clause, String[] names,
                       Object[] values)
            throws Exception {
        Connection connection = getConnection();
        try {
            SqlUtil.update(connection, table, clause, names, values);
        } finally {
            closeConnection(connection);
        }
    }




    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Statement createStatement() throws Exception {
        return getConnection().createStatement();
    }


    /**
     * _more_
     *
     * @param table _more_
     * @param clause _more_
     *
     * @throws Exception _more_
     */
    public void delete(String table, Clause clause) throws Exception {
        Connection connection = getConnection();
        try {
            SqlUtil.delete(connection, table, clause);
        } finally {
            closeConnection(connection);
        }
    }

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void shutdown() throws Exception {
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
        }
        //only shut down if this is the top-level ramadda
        if (isDatabaseDerby()
                && (getRepository().getParentRepository() == null)) {
            try {
                DriverManager.getConnection("jdbc:derby:;shutdown=true");
            } catch (Exception ignoreThis) {}
        }
        super.shutdown();
    }

    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean hasConnection() throws Exception {
        Connection connection = getConnection();
        boolean    connected  = connection != null;
        closeConnection(connection);

        return connected;
    }



    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Connection getConnection() throws Exception {
        return getConnection("");
    }

    /**
     * _more_
     *
     * @param msg _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Connection getConnection(String msg) throws Exception {
        Connection connection;
        synchronized (CONNECTION_MUTEX) {
            BasicDataSource tmpDataSource = dataSource;
            if (tmpDataSource == null) {
                throw new IllegalStateException(
                    "DatabaseManager: dataSource is null");
            }
            openCnt--;
            /*
            System.err.println("");
            */
            connection = tmpDataSource.getConnection();
        }
        synchronized (connectionInfos) {
            //            connectionInfos.add(new ConnectionInfo(connection, msg));
            //            connectionMap.put(connection,
            //                              new ConnectionInfo(connection, msg));
        }

        return connection;
    }

    /**
     * _more_
     */
    public void printIt() {
        System.err.println("active:" + dataSource.getNumActive());
        System.err.println("idle:" + dataSource.getNumIdle());
        //        System.err.println ("Open cnt:" + openCnt);
        for (ConnectionInfo info : getConnectionInfos()) {
            System.out.println("*******************");
            System.out.println(info);
        }
    }


    /**
     * _more_
     *
     * @param connection _more_
     */
    public void closeConnection(Connection connection) {
        try {
            synchronized (connectionInfos) {
                boolean gotOne = false;
                for (ConnectionInfo info : connectionInfos) {
                    //                    if(info.connection == connection) {
                    if ((info.connection == connection)
                            || info.connection.equals(connection)) {
                        connectionInfos.remove(info);
                        gotOne = true;

                        break;
                    }
                }
                if ( !gotOne) {
                    //                    System.err.println("     failed to find connection infos.size:" + connectionInfos.size());
                    //                    System.err.println("     connection:" + connection);
                    //                    System.err.println("     infos:" + connectionInfos);
                }
                //                connectionMap.remove(connection);
            }
            try {
                connection.setAutoCommit(true);
            } catch (Throwable ignore) {}

            try {
                synchronized (CONNECTION_MUTEX) {
                    openCnt--;
                    //                    System.err.println("close:" + (openCnt)); 
                    connection.close();
                }
            } catch (Exception ignore) {}

        } catch (Exception exc) {
            getLogManager().logError("Closing connections", exc);
        }
    }

    /** _more_ */
    public static int openCnt = 0;


    /**
     * _more_
     *
     * @param stmt _more_
     */
    public void initSelectStatement(Statement stmt) {}

    /**
     * _more_
     *
     * @param statement _more_
     */
    public void closeStatement(Statement statement) {
        if (statement == null) {
            return;
        }
        try {
            statement.close();
        } catch (Exception ignore) {}
    }



    /**
     * _more_
     *
     * @param statement _more_
     *
     * @throws SQLException _more_
     */
    public void closeAndReleaseConnection(Statement statement)
            throws SQLException {
        if (statement == null) {
            return;
        }
        Connection connection = null;
        try {
            connection = statement.getConnection();
            statement.close();
        } catch (Throwable ignore) {}

        if (connection != null) {
            //            System.err.println( "CONNECTION: Closing connection");
            closeConnection(connection);
        } else {
            //            Misc.printStack("statement with no connection");
            //                new IllegalArgumentException());
            //            getLogManager().logError(
            //                "CONNECTION: Tried to close a statement with no connection",
            //                new IllegalArgumentException());
        }
    }


    /**
     * _more_
     *
     * @param table _more_
     * @param clause _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public int getCount(String table, Clause clause) throws Exception {
        Statement statement = select("count(*)", table, clause);
        ResultSet results   = statement.getResultSet();
        int       result;
        if ( !results.next()) {
            result = 0;
        } else {
            result = results.getInt(1);
        }
        closeAndReleaseConnection(statement);

        return result;
    }




    /**
     * _more_
     *
     * @param sb _more_
     */
    public void addInfo(StringBuffer sb) {
        sb.append(HtmlUtils.formEntry("Database:", db));
        sb.append(HtmlUtils.formEntry("JDBC URL:", connectionURL));
    }



    /**
     * _more_
     *
     * @param os _more_
     * @param all _more_
     *
     * @throws Exception _more_
     */
    public void makeDatabaseCopyxxx(OutputStream os, boolean all)
            throws Exception {

        Connection connection = getConnection();
        try {
            DatabaseMetaData dbmd     = connection.getMetaData();
            ResultSet        catalogs = dbmd.getCatalogs();
            ResultSet tables = dbmd.getTables(null, null, null,
                                   new String[] { "TABLE" });

            ResultSetMetaData rsmd = tables.getMetaData();
            for (int col = 1; col <= rsmd.getColumnCount(); col++) {
                System.err.println(rsmd.getColumnName(col));
            }
            int totalRowCnt = 0;
            while (tables.next()) {
                //                String tableName = tables.getString("Tables.NAME.NAME");
                //                String tableType = tables.getString("Tables.TYPE.NAME");
                String tableName = tables.getString("TABLE_NAME");
                String tableType = tables.getString("TABLE_TYPE");
                if ((tableType == null) || Misc.equals(tableType, "INDEX")
                        || tableType.startsWith("SYSTEM")) {
                    continue;
                }


                String tn = tableName.toLowerCase();
                if ( !all) {
                    if (tn.equals(Tables.GLOBALS.NAME)
                            || tn.equals(Tables.USERS.NAME)
                            || tn.equals(Tables.PERMISSIONS.NAME)
                            || tn.equals(Tables.HARVESTERS.NAME)
                            || tn.equals(Tables.USERROLES.NAME)) {
                        continue;
                    }
                }


                ResultSet cols = dbmd.getColumns(null, null, tableName, null);

                int       colCnt   = 0;

                String    colNames = null;
                List      types    = new ArrayList();
                while (cols.next()) {
                    String colName = cols.getString("COLUMN_NAME");
                    if (colNames == null) {
                        colNames = " (";
                    } else {
                        colNames += ",";
                    }
                    colNames += colName;
                    int type = cols.getInt("DATA_TYPE");
                    types.add(type);
                    colCnt++;
                }
                colNames += ") ";

                Statement statement = execute("select * from " + tableName,
                                          10000000, 0);
                SqlUtil.Iterator iter = getIterator(statement);
                ResultSet        results;
                int              rowCnt    = 0;
                List             valueList = new ArrayList();
                boolean          didDelete = false;
                while ((results = iter.getNext()) != null) {
                    if ( !didDelete) {
                        didDelete = true;
                        IOUtil.write(os,
                                     "delete from  "
                                     + tableName.toLowerCase() + ";\n");
                    }
                    totalRowCnt++;
                    rowCnt++;
                    StringBuffer value = new StringBuffer("(");
                    for (int i = 1; i <= colCnt; i++) {
                        int type = ((Integer) types.get(i - 1)).intValue();
                        if (i > 1) {
                            value.append(",");
                        }
                        if (type == java.sql.Types.TIMESTAMP) {
                            Timestamp ts = results.getTimestamp(i);
                            //                            sb.append(SqlUtil.format(new Date(ts.getTime())));
                            if (ts == null) {
                                value.append("null");
                            } else {
                                value.append(HtmlUtils.squote(ts.toString()));
                            }

                        } else if (type == java.sql.Types.VARCHAR) {
                            String s = results.getString(i);
                            if (s != null) {
                                //If the target isn't mysql:
                                //s = s.replace("'", "''");
                                //If the target is mysql:
                                s = s.replace("'", "\\'");
                                s = s.replace("\r", "\\r");
                                s = s.replace("\n", "\\n");
                                value.append("'" + s + "'");
                            } else {
                                value.append("null");
                            }
                        } else {
                            String s = results.getString(i);
                            value.append(s);
                        }
                    }
                    value.append(")");
                    valueList.add(value.toString());
                    if (valueList.size() > 50) {
                        IOUtil.write(os,
                                     "insert into " + tableName.toLowerCase()
                                     + colNames + " values ");
                        IOUtil.write(os, StringUtil.join(",", valueList));
                        IOUtil.write(os, ";\n");
                        valueList = new ArrayList();
                    }
                }
                if (valueList.size() > 0) {
                    if ( !didDelete) {
                        didDelete = true;
                        IOUtil.write(os,
                                     "delete from  "
                                     + tableName.toLowerCase() + ";\n");
                    }
                    IOUtil.write(os,
                                 "insert into " + tableName.toLowerCase()
                                 + colNames + " values ");
                    IOUtil.write(os, StringUtil.join(",", valueList));
                    IOUtil.write(os, ";\n");
                }
            }
        } finally {
            closeConnection(connection);
        }


    }


    /**
     * _more_
     *
     * @param dos _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private String readString(DataInputStream dos) throws Exception {
        int length = dos.readInt();
        if (length < 0) {
            return null;
        }
        byte[] bytes = new byte[length];
        dos.read(bytes);

        return new String(bytes);
    }


    /**
     * _more_
     *
     * @param dos _more_
     * @param i _more_
     *
     * @throws Exception _more_
     */
    private void writeInteger(DataOutputStream dos, Integer i)
            throws Exception {
        if (i == null) {
            //            dos.writeInt(Integer.NaN);
            dos.writeInt(-999999);
        } else {
            dos.writeInt(i.intValue());
        }
    }

    /**
     * _more_
     *
     * @param dos _more_
     * @param i _more_
     *
     * @throws Exception _more_
     */
    private void writeLong(DataOutputStream dos, long i) throws Exception {
        dos.writeLong(i);
    }

    /**
     * _more_
     *
     * @param dos _more_
     * @param i _more_
     *
     * @throws Exception _more_
     */
    private void writeDouble(DataOutputStream dos, Double i)
            throws Exception {
        if (i == null) {
            dos.writeDouble(Double.NaN);
        } else {
            dos.writeDouble(i.doubleValue());
        }
    }

    /**
     * _more_
     *
     * @param dos _more_
     * @param s _more_
     *
     * @throws Exception _more_
     */
    private void writeString(DataOutputStream dos, String s)
            throws Exception {
        if (s == null) {
            dos.writeInt(-1);
        } else {
            dos.writeInt(s.length());
            dos.writeBytes(s);
        }
    }


    /**
     * _more_
     *
     * @param file _more_
     * @param doDrop _more_
     *
     * @throws Exception _more_
     */
    public void loadRdbFile(String file, boolean doDrop) throws Exception {

        DataInputStream dis = new DataInputStream(new FileInputStream(file));
        XmlEncoder      encoder  = new XmlEncoder();
        String          tableXml = readString(dis);
        List<TableInfo> tableInfos =
            (List<TableInfo>) encoder.toObject(tableXml);
        System.err.println("# table infos:" + tableInfos.size());
        Hashtable<String, TableInfo> tables = new Hashtable<String,
                                                  TableInfo>();
        StringBuffer sql  = new StringBuffer();
        StringBuffer drop = new StringBuffer();
        for (TableInfo tableInfo : tableInfos) {
            tables.put(tableInfo.getName(), tableInfo);
            drop.append("drop table " + tableInfo.getName() + ";\n");
            sql.append("CREATE TABLE " + tableInfo.getName() + "  (\n");
            for (int i = 0; i < tableInfo.getColumns().size(); i++) {
                ColumnInfo column = tableInfo.getColumns().get(i);
                if (i > 0) {
                    sql.append(",\n");
                }
                sql.append(column.getName());
                sql.append(" ");
                int type = column.getType();

                if (type == ColumnInfo.TYPE_TIMESTAMP) {
                    sql.append("ramadda.datetime");
                } else if (type == ColumnInfo.TYPE_VARCHAR) {
                    sql.append("varchar(" + column.getSize() + ")");
                } else if (type == ColumnInfo.TYPE_INTEGER) {
                    sql.append("int");
                } else if (type == ColumnInfo.TYPE_DOUBLE) {
                    sql.append("ramadda.double");
                } else if (type == ColumnInfo.TYPE_BIGINT) {
                    sql.append("ramadda.bigint");
                } else if (type == ColumnInfo.TYPE_SMALLINT) {
                    sql.append("int");
                } else if (type == ColumnInfo.TYPE_CLOB) {
                    sql.append(convertType("clob", column.getSize()));
                } else if (type == ColumnInfo.TYPE_BLOB) {
                    sql.append(convertType("blob", column.getSize()));
                } else if (type == ColumnInfo.TYPE_UNKNOWN) {
                    //                    sql.append(convertType("blob", column.getSize()));
                } else {
                    throw new IllegalStateException("Unknown column type:"
                            + type);
                }
            }
            sql.append(");\n");
            for (IndexInfo indexInfo : tableInfo.getIndices()) {
                sql.append("CREATE INDEX " + indexInfo.getName() + " ON "
                           + tableInfo.getName() + " ("
                           + indexInfo.getColumnName() + ");\n");
            }
        }

        //        System.err.println(drop);
        //        System.err.println(sql);

        //TODO: 
        if (doDrop) {
            loadSql(drop.toString(), true, false);
        }
        loadSql(convertSql(sql.toString()), false, true);

        TableInfo  tableInfo  = null;
        int        rows       = 0;
        Connection connection = getConnection();
        try {
            while (true) {
                int what = dis.readInt();
                if (what == DUMPTAG_TABLE) {
                    String tableName = readString(dis);
                    tableInfo = tables.get(tableName);
                    if (tableInfo == null) {
                        throw new IllegalArgumentException("No table:"
                                + tableName);
                    }
                    if (tableInfo.statement == null) {
                        String insert =
                            SqlUtil.makeInsert(tableInfo.getName(),
                                tableInfo.getColumnNames());
                        tableInfo.statement =
                            connection.prepareStatement(insert);
                    }
                    System.err.println("importing table:"
                                       + tableInfo.getName());

                    continue;
                }
                if (what == DUMPTAG_END) {
                    break;
                }
                if (what != DUMPTAG_ROW) {
                    throw new IllegalArgumentException("Unkown tag:" + what);
                }

                rows++;
                if ((rows % 1000) == 0) {
                    System.err.println("rows:" + rows);
                }



                Object[] values = new Object[tableInfo.getColumns().size()];
                int      colCnt = 0;
                for (ColumnInfo columnInfo : tableInfo.getColumns()) {
                    int type = columnInfo.getType();
                    if (type == ColumnInfo.TYPE_TIMESTAMP) {
                        long dttm = dis.readLong();
                        values[colCnt++] = new Date(dttm);
                    } else if (type == ColumnInfo.TYPE_VARCHAR) {
                        String s = readString(dis);
                        if ((s != null) && (s.length() > 5000)) {
                            //A hack for old dbs
                            if (tableInfo.getName().equals("metadata")) {
                                s = s.substring(0, 4999);
                                System.err.println("clipping: "
                                        + tableInfo.getName() + "."
                                        + columnInfo.getName());
                            }

                        }
                        values[colCnt++] = s;
                    } else if (type == ColumnInfo.TYPE_INTEGER) {
                        values[colCnt++] = new Integer(dis.readInt());
                    } else if (type == ColumnInfo.TYPE_DOUBLE) {
                        values[colCnt++] = new Double(dis.readDouble());
                    } else if (type == ColumnInfo.TYPE_CLOB) {
                        values[colCnt++] = readString(dis);
                    } else if (type == ColumnInfo.TYPE_BLOB) {
                        values[colCnt++] = readString(dis);
                    } else if (type == ColumnInfo.TYPE_BIGINT) {
                        long v = dis.readLong();
                        values[colCnt++] = new Long(v);
                    } else if (type == ColumnInfo.TYPE_SMALLINT) {
                        short v = dis.readShort();
                        values[colCnt++] = new Short(v);
                    } else if (type == ColumnInfo.TYPE_UNKNOWN) {}
                    else {
                        throw new IllegalArgumentException(
                            "Unknown type for table" + tableInfo.getName()
                            + " " + type);
                    }
                }
                setValues(tableInfo.statement, values);
                tableInfo.statement.addBatch();
                tableInfo.batchCnt++;
                if (tableInfo.batchCnt > 1000) {
                    tableInfo.batchCnt = 0;
                    tableInfo.statement.executeBatch();

                }
            }

            //Now finish up the batch
            for (TableInfo ti : tableInfos) {
                if (ti.batchCnt > 0) {
                    ti.batchCnt = 0;
                    ti.statement.executeBatch();
                }
            }
        } finally {
            IOUtil.close(dis);
            closeConnection(connection);
        }

        System.err.println("imported " + rows + " rows");


    }


    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public void finishRdbLoad() throws Exception {
        for (TypeHandler typeHandler : getRepository().getTypeHandlers()) {
            typeHandler.initAfterDatabaseImport();
        }
    }


    /**
     * _more_
     *
     * @param connection _more_
     * @param all _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<TableInfo> getTableInfos(Connection connection, boolean all)
            throws Exception {

        DatabaseMetaData dbmd = connection.getMetaData();
        ResultSet tables = dbmd.getTables(null, null, null,
                                          new String[] { "TABLE" });


        ResultSetMetaData rsmd = tables.getMetaData();
        for (int col = 1; col <= rsmd.getColumnCount(); col++) {
            //                System.err.println (rsmd.getColumnName(col));
        }
        List<TableInfo> tableInfos = new ArrayList<TableInfo>();
        HashSet<String> seen       = new HashSet<String>();



        while (tables.next()) {
            String tableName = tables.getString("TABLE_NAME");
            String tn        = tableName.toLowerCase();

            if (tn.equals("participant")) {
                //a hack due to some old bad derby db I have
                continue;
            }


            //Just in case
            if (seen.contains(tn)) {
                System.err.println("Warning: duplicate table:" + tableName);

                continue;
            }
            seen.add(tn);


            boolean ok = true;
            for (TypeHandler typeHandler :
                    getRepository().getTypeHandlers()) {
                if ( !typeHandler.shouldExportTable(tn)) {
                    ok = false;

                    break;
                }
            }

            if ( !ok) {
                continue;
            }
            String tableType = tables.getString("TABLE_TYPE");

            if ((tableType == null) || tableType.startsWith("SYSTEM")
                    || Misc.equals(tableType, "INDEX")) {
                continue;
            }

            ResultSet indices = dbmd.getIndexInfo(null, null, tableName,
                                    false, false);
            List<IndexInfo> indexList = new ArrayList<IndexInfo>();
            while (indices.next()) {
                indexList.add(
                    new IndexInfo(
                        indices.getString("INDEX_NAME"),
                        indices.getString("COLUMN_NAME")));

            }



            ResultSet cols = dbmd.getColumns(null, null, tableName, null);
            rsmd = cols.getMetaData();
            List<ColumnInfo> columns = new ArrayList<ColumnInfo>();
            while (cols.next()) {
                String colName  = cols.getString("COLUMN_NAME");
                int    type     = cols.getInt("DATA_TYPE");
                String typeName = cols.getString("TYPE_NAME");
                int    size     = cols.getInt("COLUMN_SIZE");
                if (type == -1) {
                    if (typeName.toLowerCase().equals("mediumtext")) {
                        type = java.sql.Types.CLOB;
                        //Just come up with some size

                        if (size <= 0) {
                            size = 36000;
                        }
                    } else if (typeName.toLowerCase().equals("longtext")) {
                        type = java.sql.Types.CLOB;
                        //Just come up with some size
                        if (size <= 0) {
                            size = 36000;
                        }
                    }
                }
                if (typeName.equalsIgnoreCase("text")) {
                    if (size <= 0) {
                        size = 36000;
                    }
                }

                columns.add(new ColumnInfo(colName, typeName, type, size));
                if (tn.indexOf("wiki") >= 0) {
                    System.err.println("COLS:" + columns);
                }
            }

            tableInfos.add(new TableInfo(tn, indexList, columns));
        }

        return tableInfos;

    }



    /**
     * _more_
     *
     * @param os _more_
     * @param all _more_
     * @param actionId _more_
     *
     * @throws Exception _more_
     */
    public void makeDatabaseCopy(OutputStream os, boolean all,
                                 Object actionId)
            throws Exception {

        XmlEncoder       encoder    = new XmlEncoder();
        DataOutputStream dos        = new DataOutputStream(os);
        Connection       connection = getConnection();
        try {
            HashSet<String> skip = new HashSet<String>();
            skip.add(Tables.SESSIONS.NAME);

            List<TableInfo> tableInfos = getTableInfos(connection, false);
            String          xml        = encoder.toXml(tableInfos, false);
            writeString(dos, xml);


            int rowCnt = 0;
            System.err.println("Exporting database");
            for (TableInfo tableInfo : tableInfos) {
                if (tableInfo.getName().equalsIgnoreCase("base")) {
                    continue;
                }
                if (tableInfo.getName().equalsIgnoreCase("agggregation")) {
                    continue;
                }
                if (tableInfo.getName().equalsIgnoreCase("entry")) {
                    continue;
                }
                System.err.println("Exporting table: " + tableInfo.getName());
                List<ColumnInfo> columns   = tableInfo.getColumns();
                List             valueList = new ArrayList();
                Statement statement = execute("select * from "
                                          + tableInfo.getName(), 10000000, 0);
                SqlUtil.Iterator iter = getIterator(statement);
                ResultSet        results;
                dos.writeInt(DUMPTAG_TABLE);
                writeString(dos, tableInfo.getName());
                if (skip.contains(tableInfo.getName().toLowerCase())) {
                    continue;
                }
                while ((results = iter.getNext()) != null) {
                    dos.writeInt(DUMPTAG_ROW);
                    rowCnt++;
                    if ((rowCnt % 1000) == 0) {
                        if (actionId != null) {
                            getActionManager().setActionMessage(actionId,
                                    "Written " + rowCnt + " database rows");
                        }
                        System.err.println("rows:" + rowCnt);
                    }
                    for (int i = 1; i <= columns.size(); i++) {
                        ColumnInfo colInfo = columns.get(i - 1);
                        int        type    = colInfo.getType();
                        if (type == ColumnInfo.TYPE_TIMESTAMP) {
                            Timestamp ts = results.getTimestamp(i);
                            if (ts == null) {
                                dos.writeLong((long) -1);
                            } else {
                                dos.writeLong(ts.getTime());
                            }
                        } else if (type == ColumnInfo.TYPE_VARCHAR) {
                            writeString(dos, results.getString(i));
                        } else if (type == ColumnInfo.TYPE_TIME) {
                            //TODO: What is the format of a type time?
                            //                            writeString(dos, results.getString(i));
                        } else if (type == ColumnInfo.TYPE_INTEGER) {
                            writeInteger(dos, (Integer) results.getObject(i));
                        } else if (type == ColumnInfo.TYPE_DOUBLE) {
                            writeDouble(dos, (Double) results.getObject(i));
                        } else if (type == ColumnInfo.TYPE_CLOB) {
                            writeString(dos, results.getString(i));
                        } else if (type == ColumnInfo.TYPE_BLOB) {
                            writeString(dos, results.getString(i));
                        } else if (type == ColumnInfo.TYPE_BIGINT) {
                            writeLong(dos, results.getLong(i));
                        } else if (type == ColumnInfo.TYPE_SMALLINT) {
                            dos.writeShort(results.getShort(i));
                        } else if (type == ColumnInfo.TYPE_TINYINT) {
                            //TODO:
                            //dos.write(results.getChar(i));
                        } else {
                            Object object = results.getObject(i);

                            throw new IllegalArgumentException(
                                "Unknown type:" + type + "  c:"
                                + object.getClass().getName());
                        }
                    }
                }
            }
            System.err.println("Wrote " + rowCnt + " rows");
        } finally {
            closeConnection(connection);
        }
        //Write the end tag
        dos.writeInt(DUMPTAG_END);
        IOUtil.close(dos);

    }


    /**
     * _more_
     *
     * @param statement _more_
     *
     * @return _more_
     */
    public SqlUtil.Iterator getIterator(Statement statement) {
        return new Iterator(this, statement);
    }





    /**
     * _more_
     *
     * @param sql _more_
     *
     * @throws Exception _more_
     */
    public void executeAndClose(String sql) throws Exception {
        executeAndClose(sql, 10000000, 0);
    }





    /**
     * _more_
     *
     * @param sql _more_
     * @param max _more_
     * @param timeout _more_
     *
     * @throws Exception _more_
     */
    public void executeAndClose(String sql, int max, int timeout)
            throws Exception {
        Connection connection = getConnection();
        try {
            Statement statement = execute(connection, sql, max, timeout);
            closeStatement(statement);
        } finally {
            closeConnection(connection);
        }
    }


    /**
     * _more_
     *
     * @param sql _more_
     * @param max _more_
     * @param timeout _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Statement execute(String sql, int max, int timeout)
            throws Exception {
        return execute(getConnection(), sql, max, timeout);
    }


    /**
     * _more_
     *
     * @param connection _more_
     * @param sql _more_
     * @param max _more_
     * @param timeout _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Statement execute(Connection connection, String sql, int max,
                             int timeout)
            throws Exception {
        Statement statement = connection.createStatement();
        if (timeout > 0) {
            statement.setQueryTimeout(timeout);
        }

        if (max > 0) {
            statement.setMaxRows(max);
        }

        long t1 = System.currentTimeMillis();
        try {
            statement.execute(sql);
        } catch (Exception exc) {
            //            logError("Error executing sql:" + sql, exc);
            throw exc;
        }
        long t2 = System.currentTimeMillis();
        if (getRepository().debug || (t2 - t1 > 300)) {
            logInfo("query took:" + (t2 - t1) + " " + sql);
        }
        if (t2 - t1 > 2000) {
            //            Misc.printStack("query:" + sql);
        }

        return statement;
    }





    /**
     * _more_
     *
     * @param oldTable _more_
     * @param newTable _more_
     * @param connection _more_
     *
     * @throws Exception _more_
     */
    public void copyTable(String oldTable, String newTable,
                          Connection connection)
            throws Exception {
        String copySql = "INSERT INTO  " + newTable + " SELECT * from "
                         + oldTable;
        execute(connection, copySql, -1, -1);
    }



    /**
     * _more_
     *
     * @param statement _more_
     * @param col _more_
     * @param date _more_
     *
     * @throws Exception _more_
     */
    public void setTimestamp(PreparedStatement statement, int col, Date date)
            throws Exception {
        if (date == null) {
            statement.setTimestamp(col, null);
        } else {
            statement.setTimestamp(col,
                                   new java.sql.Timestamp(date.getTime()),
                                   Repository.calendar);
        }
    }


    /**
     * _more_
     *
     * @param results _more_
     * @param col _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Date getTimestamp(ResultSet results, int col) throws Exception {
        return getTimestamp(results, col, true);
    }


    /**
     * _more_
     *
     * @param results _more_
     * @param col _more_
     * @param makeDflt If true then return a new Date if there are no results found
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Date getTimestamp(ResultSet results, int col, boolean makeDflt)
            throws Exception {
        Date date = results.getTimestamp(col, Repository.calendar);
        if (date != null) {
            return date;
        }
        if (makeDflt) {
            return new Date();
        }

        return null;
    }


    /**
     * _more_
     *
     * @param results _more_
     * @param col _more_
     * @param makeDflt _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Date getTimestamp(ResultSet results, String col, boolean makeDflt)
            throws Exception {
        Date date = results.getTimestamp(col, Repository.calendar);
        if (date != null) {
            return date;
        }
        if (makeDflt) {
            return new Date();
        }

        return null;
    }



    /**
     * _more_
     *
     * @param statement _more_
     * @param col _more_
     * @param time _more_
     *
     * @throws Exception _more_
     */
    public void setDate(PreparedStatement statement, int col, long time)
            throws Exception {
        setDate(statement, col, new Date(time));
    }


    /**
     * _more_
     *
     * @param statement _more_
     * @param col _more_
     * @param date _more_
     *
     * @throws Exception _more_
     */
    public void setDate(PreparedStatement statement, int col, Date date)
            throws Exception {
        //        if (!db.equals(DB_MYSQL)) {
        if (true || !db.equals(DB_MYSQL)) {
            setTimestamp(statement, col, date);
        } else {
            if (date == null) {
                statement.setTime(col, null);
            } else {
                statement.setTime(col, new java.sql.Time(date.getTime()),
                                  Repository.calendar);
            }
        }
    }


    /**
     * _more_
     *
     * @param results _more_
     * @param col _more_
     * @param dflt _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Date getDate(ResultSet results, int col, Date dflt)
            throws Exception {
        Date date = getDate(results, col, false);
        if (date == null) {
            return dflt;
        }

        return date;
    }


    /**
     * _more_
     *
     * @param results _more_
     * @param col _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Date getDate(ResultSet results, int col) throws Exception {
        return getDate(results, col, true);
    }

    /**
     * _more_
     *
     * @param results _more_
     * @param col _more_
     * @param makeDflt _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Date getDate(ResultSet results, int col, boolean makeDflt)
            throws Exception {
        //        if (!db.equals(DB_MYSQL)) {
        if (true || !db.equals(DB_MYSQL)) {
            return getTimestamp(results, col, makeDflt);
        }
        Date date = results.getTime(col, Repository.calendar);
        if (date != null) {
            return date;
        }
        if (makeDflt) {
            return new Date();
        }

        return null;
    }



    /**
     * _more_
     *
     * @param statement _more_
     * @param values _more_
     *
     * @throws Exception _more_
     */
    public void setValues(PreparedStatement statement, Object[] values)
            throws Exception {
        setValues(statement, values, 1);
    }

    /**
     * _more_
     *
     * @param statement _more_
     * @param values _more_
     * @param startIdx _more_
     *
     * @throws Exception _more_
     */
    public void setValues(PreparedStatement statement, Object[] values,
                          int startIdx)
            throws Exception {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == null) {
                statement.setNull(i + startIdx, java.sql.Types.VARCHAR);
            } else if (values[i] instanceof Date) {
                setDate(statement, i + startIdx, (Date) values[i]);
            } else if (values[i] instanceof Boolean) {
                boolean b = ((Boolean) values[i]).booleanValue();
                statement.setInt(i + startIdx, (b
                        ? 1
                        : 0));
            } else if (values[i] instanceof Double) {
                double d = ((Double) values[i]).doubleValue();
                //Special check for nans on derby
                if (d == Double.POSITIVE_INFINITY) {
                    d = Double.NaN;
                } else if (d == Double.NEGATIVE_INFINITY) {
                    d = Double.NaN;
                }
                if (d != d) {
                    if (isDatabaseDerby()) {
                        d = -99999999.999;
                    }
                    //
                }
                try {
                    statement.setDouble(i + startIdx, d);
                } catch (Exception exc) {
                    System.err.println("d:" + d);

                    throw exc;
                }
            } else {
                statement.setObject(i + startIdx, values[i]);
            }
        }
    }


    /**
     * _more_
     *
     * @param insert _more_
     * @param values _more_
     *
     * @throws Exception _more_
     */
    public void executeInsert(String insert, Object[] values)
            throws Exception {
        List<Object[]> valueList = new ArrayList<Object[]>();
        valueList.add(values);
        executeInsert(insert, valueList);
    }



    /**
     * _more_
     *
     * @param insert _more_
     * @param valueList _more_
     *
     * @throws Exception _more_
     */
    public void executeInsert(String insert, List<Object[]> valueList)
            throws Exception {
        PreparedStatement pstatement = getPreparedStatement(insert);
        for (Object[] values : valueList) {
            setValues(pstatement, values);
            try {
                pstatement.executeUpdate();
            } catch (Exception exc) {
                logError("Error:" + insert, exc);
            }
        }
        closeAndReleaseConnection(pstatement);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean supportsRegexp() {
        return db.equals(DB_MYSQL) || db.equals(DB_POSTGRES);
    }

    /**
     * _more_
     *
     * @param column _more_
     * @param pattern _more_
     * @param doNot _more_
     *
     * @return _more_
     */
    public Clause makeRegexpClause(String column, String pattern,
                                   final boolean doNot) {
        if (isDatabaseMysql()) {
            return new Clause(column, "regexp", pattern) {
                public StringBuffer addClause(StringBuffer sb) {
                    if (doNot) {
                        sb.append(SqlUtil.group(getColumn()
                                + "  NOT REGEXP  ?"));
                    } else {
                        sb.append(SqlUtil.group(getColumn()
                                + "   REGEXP  ?"));
                    }

                    return sb;
                }
            };
        } else if (isDatabasePostgres()) {
            return new Clause(column, "regexp", pattern) {
                public StringBuffer addClause(StringBuffer sb) {
                    if (doNot) {
                        sb.append(SqlUtil.group(getColumn()
                                + "  NOT SIMILAR TO  ?"));
                    } else {
                        sb.append(SqlUtil.group(getColumn()
                                + "   SIMILAR TO  ?"));
                    }

                    return sb;
                }
            };
        } else {
            throw new IllegalStateException("regexp not supported in " + db);
        }
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isDatabaseDerby() {
        return (db.equals(DB_DERBY));
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isDatabaseMysql() {
        return (db.equals(DB_MYSQL));
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isDatabaseH2() {
        return (db.equals(DB_H2));
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isDatabasePostgres() {
        return (db.equals(DB_POSTGRES));
    }

    /**
     * _more_
     *
     * @param sql _more_
     *
     * @return _more_
     */
    public String convertSql(String sql) {
        if (db.equals(DB_MYSQL)) {
            sql = sql.replace("ramadda.double", "double");
            sql = sql.replace("ramadda.datetime", "datetime");
            sql = sql.replace("ramadda.clob", "text");
            sql = sql.replace("ramadda.bigclob", "text");
            sql = sql.replace("ramadda.bigint", "bigint");
            //sql = sql.replace("ramadda.datetime", "timestamp");
        } else if (db.equals(DB_DERBY)) {
            sql = sql.replace("ramadda.double", "double");
            sql = sql.replace("ramadda.datetime", "timestamp");
            sql = sql.replace("ramadda.clob", "clob(64000)");
            sql = sql.replace("ramadda.bigclob", "clob(256000)");
            sql = sql.replace("ramadda.bigint", "bigint");
        } else if (db.equals(DB_POSTGRES)) {
            sql = sql.replace("ramadda.double", "float8");
            sql = sql.replace("ramadda.datetime", "timestamp");
            sql = sql.replace("ramadda.clob", "text");
            sql = sql.replace("ramadda.bigclob", "text");
            sql = sql.replace("ramadda.bigint", "bigint");
        } else if (db.equals(DB_ORACLE)) {
            sql = sql.replace("ramadda.double", "number");
            //            sql = sql.replace("ramadda.datetime", "date");
            sql = sql.replace("ramadda.datetime", "timestamp");
            sql = sql.replace("ramadda.clob", "clob");
            sql = sql.replace("ramadda.bigclob", "clob");
            sql = sql.replace("ramadda.bigint", "bigint");
        } else if (db.equals(DB_H2)) {
            sql = sql.replace("ramadda.double", "float8");
            sql = sql.replace("ramadda.datetime", "timestamp");
            sql = sql.replace("ramadda.clob", "text");
            sql = sql.replace("ramadda.bigclob", "text");
            sql = sql.replace("ramadda.bigint", "bigint");
        }

        return sql;
    }


    /**
     * _more_
     *
     * @param col _more_
     *
     * @return _more_
     */
    public String getExtractYear(String col) {
        if (db.equals(DB_POSTGRES)) {
            return " extract (year from " + col + ") ";
        } else {
            return "year(" + col + ")";
        }
    }



    /**
     * _more_
     *
     * @param sql _more_
     * @param ignoreErrors _more_
     * @param printStatus _more_
     *
     * @throws Exception _more_
     */
    public void loadSql(String sql, boolean ignoreErrors, boolean printStatus)
            throws Exception {
        Connection connection = getConnection();
        try {
            //            connection.setAutoCommit(false);
            loadSql(connection, sql, ignoreErrors, printStatus);
            //            connection.commit();
            //            connection.setAutoCommit(true);
        } finally {
            closeConnection(connection);
        }
    }



    /**
     * _more_
     *
     * @param connection _more_
     * @param sql _more_
     * @param ignoreErrors _more_
     * @param printStatus _more_
     *
     * @throws Exception _more_
     */
    public void loadSql(Connection connection, String sql,
                        boolean ignoreErrors, boolean printStatus)
            throws Exception {
        Statement statement = connection.createStatement();
        try {
            List<SqlUtil.SqlError> errors = new ArrayList<SqlUtil.SqlError>();
            SqlUtil.loadSql(sql, statement, ignoreErrors, printStatus,
                            errors);
            int existsCnt = 0;
            for (SqlUtil.SqlError error : errors) {
                String errorString =
                    error.getException().toString().toLowerCase();
                if ((errorString.indexOf("already exists") < 0)
                        && (errorString.indexOf("duplicate") < 0)) {
                    System.err.println(
                        "RAMADDA: Error in DatabaseManager.loadSql: "
                        + error.getException() + "\nsql:" + error.getSql());
                } else {
                    //                    System.err.println("EXISTS: "+error.getSql());
                    existsCnt++;
                }
            }
            if (existsCnt > 0) {
                //                System.err.println("DatabaseManager.loadSql: Some tables and indices already exist");
            }
        } finally {
            closeStatement(statement);
        }
    }


    /**
     * _more_
     *
     * @param value _more_
     *
     * @return _more_
     */
    public String escapeString(String value) {
        if (db.equals(DB_MYSQL)) {
            value = value.replace("'", "\\'");
        } else {
            value = value.replace("'", "''");
        }

        return value;
    }


    /**
     * _more_
     *
     * @param type _more_
     *
     * @return _more_
     */
    public String convertType(String type) {
        return convertType(type, -1);
    }

    /**
     * _more_
     *
     * @param type _more_
     * @param size _more_
     *
     * @return _more_
     */
    public String convertType(String type, int size) {
        if (type.equals("clob")) {
            if (db.equals(DB_DERBY)) {
                return "clob(" + size + ") ";
            }
            if (db.equals(DB_MYSQL)) {
                return "mediumtext";
            }
            if (db.equals(DB_POSTGRES)) {
                return "text";
            }
        }
        if (type.equals("double")) {
            if (db.equals(DB_POSTGRES)) {
                return "float8";
            }
        } else if (type.equals("float8")) {
            if (db.equals(DB_MYSQL) || db.equals(DB_DERBY)) {
                return "double";
            }
        }

        return type;
    }


    /**
     * _more_
     *
     * @param skip _more_
     * @param max _more_
     *
     * @return _more_
     */
    public String getLimitString(int skip, int max) {
        if (skip < 0) {
            skip = 0;
        }
        if (max < 0) {
            max = DB_MAX_ROWS;
        }
        if (db.equals(DB_MYSQL)) {
            return " LIMIT " + max + " OFFSET " + skip + " ";
        } else if (db.equals(DB_DERBY)) {
            return " OFFSET " + skip + " ROWS ";
        } else if (db.equals(DB_POSTGRES)) {
            return " LIMIT " + max + " OFFSET " + skip + " ";

        } else if (db.equals(DB_H2)) {
            return " LIMIT " + max + " OFFSET " + skip + " ";
        }

        return "";
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean canDoSelectOffset() {
        if (db.equals(DB_MYSQL)) {
            return true;
        } else if (db.equals(DB_DERBY)) {
            return true;
        } else if (db.equals(DB_POSTGRES)) {
            return true;
        } else if (db.equals(DB_H2)) {
            return true;
        }

        return false;
    }


    /**
     * _more_
     *
     * @param what _more_
     * @param table _more_
     * @param clause _more_
     * @param extra _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Statement select(String what, String table, Clause clause,
                            String extra)
            throws Exception {
        return select(what, Misc.newList(table), clause, extra, -1);
    }



    /**
     * Class SelectInfo _more_
     *
     *
     * @author RAMADDA Development Team
     */
    private static class SelectInfo {

        /** _more_ */
        long time;

        /** _more_ */
        String what;

        /** _more_ */
        List tables;

        /** _more_ */
        Clause clause;

        /** _more_ */
        String extra;

        /** _more_ */
        int max;

        /**
         * _more_
         *
         * @param what _more_
         * @param tables _more_
         * @param clause _more_
         * @param extra _more_
         * @param max _more_
         */
        public SelectInfo(String what, List tables, Clause clause,
                          String extra, int max) {
            time        = System.currentTimeMillis();
            this.what   = what;
            this.tables = tables;
            this.clause = clause;
            this.extra  = extra;
            this.max    = max;
        }

    }



    /**
     * _more_
     *
     * @param what _more_
     * @param tables _more_
     * @param clause _more_
     * @param extra _more_
     * @param max _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Statement select(final String what, final List tables,
                            final Clause clause, String extra, final int max)
            throws Exception {
        if (extra != null) {
            extra = escapeString(extra);
        }
        SelectInfo selectInfo = new SelectInfo(what, tables, clause, extra,
                                    max);
        final boolean[] done = { false };
        String msg = "Select what:" + what + "\ntables:" + tables
                     + "\nclause:" + clause + "\nextra:" + extra + "\nmax:"
                     + max;
        /*
        Misc.run(new Runnable() {
                public void run() {
                    //Wait 20 seconds
                    Misc.sleep(1000*10);
                    if(!done[0]) {
                        System.err.println("Select is taking too long\nwhat:" + what + "\ntables:" +
                                           tables +
                                           "\nclause:" + clause +
                                           "\nextra:" + extra+
                                           "max:" + max);
                        Misc.printStack("select",20);
                    }
                }
            });
        */

        Connection connection = getConnection(msg);
        try {
            numberOfSelects.incr();
            //            System.err.println ("clause:" + clause +" tables:" + tables);
            Statement statement = SqlUtil.select(connection, what, tables,
                                      clause, extra, max, TIMEOUT);

            done[0] = true;

            return statement;
        } catch (Exception exc) {
            logError("Error doing select \nwhat:" + what + "\ntables:"
                     + tables + "\nclause:" + clause + "\nextra:" + extra
                     + "max:" + max, exc);
            closeConnection(connection);

            throw exc;
        } finally {
            numberOfSelects.decr();
        }

    }


    /**
     * _more_
     *
     * @param what _more_
     * @param table _more_
     * @param clause _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Statement select(String what, String table, Clause clause)
            throws Exception {
        return select(what, Misc.newList(table), ((clause == null)
                ? null
                : new Clause[] { clause }));
    }


    /**
     * _more_
     *
     * @param what _more_
     * @param table _more_
     * @param clauses _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Statement select(String what, String table, Clause[] clauses)
            throws Exception {
        return select(what, Misc.newList(table), clauses);
    }


    /**
     * _more_
     *
     * @param what _more_
     * @param table _more_
     * @param clauses _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Statement select(String what, String table, List<Clause> clauses)
            throws Exception {
        return select(what, Misc.newList(table), Clause.toArray(clauses));
    }

    /**
     * _more_
     *
     * @param what _more_
     * @param tables _more_
     * @param clauses _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Statement select(String what, List tables, Clause[] clauses)
            throws Exception {
        return select(what, tables, Clause.and(clauses), null, -1);
    }




    /**
     * _more_
     *
     * @param id _more_
     * @param tableName _more_
     * @param column _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean tableContains(String id, String tableName, String column)
            throws Exception {
        return tableContains(Clause.eq(column, id), tableName, column);
    }


    /**
     * _more_
     *
     * @param clause _more_
     * @param tableName _more_
     * @param column _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean tableContains(Clause clause, String tableName,
                                 String column)
            throws Exception {
        Statement statement = select(column, tableName, clause);
        ResultSet results   = statement.getResultSet();
        boolean   result    = results.next();
        closeAndReleaseConnection(statement);

        return result;
    }



    /**
     * Class Iterator _more_
     *
     *
     * @author RAMADDA Development Team
     */
    public static class Iterator extends SqlUtil.Iterator {

        /** _more_ */
        Statement statement;

        /** _more_ */
        DatabaseManager databaseManager;

        /**
         * _more_
         *
         * @param databaseManager _more_
         * @param statement _more_
         */
        public Iterator(DatabaseManager databaseManager,
                        Statement statement) {
            super(statement);
            this.statement       = statement;
            this.databaseManager = databaseManager;
        }

        /**
         * _more_
         *
         * @param statement _more_
         *
         * @throws SQLException _more_
         */
        @Override
        protected void close(Statement statement) throws SQLException {
            databaseManager.closeAndReleaseConnection(statement);
        }

    }




    /**
     * This writes out the full database table definition to a file called Tables.java
     *
     *
     * @param packageName Tables class package name
     * @throws Exception On badness
     */
    public void writeTables(String packageName) throws Exception {
        writeTables(packageName, new String[] { "TABLE", "VIEW" });
    }

    /**
     * _more_
     *
     * @param packageName _more_
     * @param what _more_
     *
     * @throws Exception On badness
     */
    public void writeTables(String packageName, String[] what)
            throws Exception {
        FileOutputStream fos = new FileOutputStream("Tables.java");
        PrintWriter      pw  = new PrintWriter(fos);
        writeTables(pw, packageName, what);
        pw.close();
        fos.close();
    }

    /**
     * Actually write the tables
     *
     * @param pw What to write to
     * @param packageName Tables.java package name
     * @param what _more_
     *
     * @throws Exception on badness
     */

    private void writeTables(PrintWriter pw, String packageName,
                             String[] what)
            throws Exception {

        String sp1 = "    ";
        String sp2 = sp1 + sp1;
        String sp3 = sp1 + sp1 + sp1;

        pw.append(
            "/**Generated by running: java org.unavco.projects.gsac.repository.UnavcoGsacDatabaseManager**/\n\n");
        pw.append("package " + packageName + ";\n\n");
        pw.append("import org.ramadda.util.sql.SqlUtil;\n\n");
        pw.append("//J-\n");
        pw.append("public abstract class Tables {\n");
        pw.append(sp1 + "public abstract String getName();\n");
        pw.append(sp1 + "public abstract String getColumns();\n");
        Connection       connection = getConnection();
        DatabaseMetaData dbmd       = connection.getMetaData();
        ResultSet        catalogs   = dbmd.getCatalogs();
        ResultSet        tables     = dbmd.getTables(null, null, null, what);


        HashSet          seenTables = new HashSet();
        while (tables.next()) {
            String tableName = tables.getString("TABLE_NAME");
            //            System.err.println ("NAME:" + tableName);
            String TABLENAME = tableName.toUpperCase();
            if (seenTables.contains(TABLENAME)) {
                continue;
            }
            seenTables.add(TABLENAME);
            String tableType = tables.getString("TABLE_TYPE");
            if (Misc.equals(tableType, "INDEX")) {
                continue;
            }
            if (tableName.indexOf("$") >= 0) {
                continue;
            }

            if (tableType == null) {
                continue;
            }

            if ((tableType != null) && tableType.startsWith("SYSTEM")) {
                continue;
            }

            ResultSet columns  = dbmd.getColumns(null, null, tableName, null);


            List      colNames = new ArrayList();
            pw.append("\n\n");
            pw.append(sp1 + "public static class " + TABLENAME
                      + " extends Tables {\n");

            pw.append(sp2 + "public static final String NAME = \""
                      + tableName.toLowerCase() + "\";\n");
            pw.append("\n");
            pw.append(sp2 + "public String getName() {return NAME;}\n");
            pw.append(sp2 + "public String getColumns() {return COLUMNS;}\n");
            System.out.println("processing table:" + TABLENAME);

            String  tableVar = null;
            List    colVars  = new ArrayList();
            HashSet seen     = new HashSet();
            while (columns.next()) {
                String colName =
                    columns.getString("COLUMN_NAME").toLowerCase();
                String colSize = columns.getString("COLUMN_SIZE");
                String COLNAME = colName.toUpperCase();
                if (seen.contains(COLNAME)) {
                    continue;
                }
                seen.add(COLNAME);
                COLNAME = COLNAME.replace("#", "");
                colNames.add("COL_" + COLNAME);
                pw.append(sp2 + "public static final String COL_" + COLNAME
                          + " =  NAME + \"." + colName + "\";\n");

                pw.append(sp2 + "public static final String COL_NODOT_"
                          + COLNAME + " =   \"" + colName + "\";\n");
                /*
                pw.append(sp2 + "public static final String ORA_" + COLNAME
                          + " =  \"" + colName + "\";\n");
                */
            }

            pw.append("\n");
            pw.append(
                sp2
                + "public static final String[] ARRAY = new String[] {\n");
            pw.append(sp3 + StringUtil.join(",", colNames));
            pw.append("\n");
            pw.append(sp2 + "};\n");
            pw.append(
                sp2
                + "public static final String COLUMNS = SqlUtil.comma(ARRAY);\n");
            pw.append(
                sp2
                + "public static final String NODOT_COLUMNS = SqlUtil.commaNoDot(ARRAY);\n");

            pw.append(sp2 + "public static final String INSERT ="
                      + "SqlUtil.makeInsert(NAME, NODOT_COLUMNS,"
                      + "SqlUtil.getQuestionMarks(ARRAY.length));\n");

            pw.append(sp1 + "public static final " + TABLENAME
                      + " table  = new  " + TABLENAME + "();\n");
            pw.append(sp1 + "}\n\n");

        }


        pw.append("\n\n}\n");

    }

    /**
     * _more_
     *
     * @param column _more_
     * @param value _more_
     * @param not _more_
     *
     * @return _more_
     */
    public Clause makeLikeTextClause(String column, String value,
                                     boolean not) {
        Clause clause = Clause.like(column, value.toUpperCase(), not);
        clause.setColumnModifier("UPPER(", ")");

        return clause;
    }


    /**
     * _more_
     *
     * @param tableName _more_
     * @param column _more_
     * @param clause _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<String> selectDistinct(String tableName, String column,
                                       Clause clause)
            throws Exception {
        Statement stmt   = select(SqlUtil.distinct(column), tableName,
                                  clause);
        String[]  values = SqlUtil.readString(getIterator(stmt), 1);

        return (List<String>) Misc.toList(values);
    }



    /**
     * _more_
     *
     * @param table _more_
     * @param column _more_
     * @param type _more_
     *
     * @return _more_
     */
    public String getAlterTableSql(String table, String column, String type) {
        String sql;
        if (isDatabaseDerby()) {
            sql = "alter table " + table + "  alter column " + column
                  + "  set data type " + type + ";";
        } else if (isDatabasePostgres()) {
            sql = "alter table " + table + " alter column " + column
                  + " type " + type + ";";
        } else if (isDatabaseMysql()) {
            //              ALTER TABLE t1 MODIFY col1 BIGINT;
            sql = "alter table " + table + " modify " + column + " " + type
                  + ";";
        } else {
            //h2
            //            ALTER TABLE TEST ALTER COLUMN NAME CLOB;
            sql = "alter table " + table + " alter column " + column + " "
                  + type + ";";

        }

        return sql;
    }


    /**
     * _more_
     *
     * @param statement _more_
     * @param col _more_
     * @param value _more_
     * @param missing _more_
     *
     * @throws Exception _more_
     */
    public void setDouble(PreparedStatement statement, int col, double value,
                          double missing)
            throws Exception {
        if (Double.isNaN(value) || (value == Double.NEGATIVE_INFINITY)
                || (value == Double.POSITIVE_INFINITY)) {
            value = missing;
        }
        statement.setDouble(col, value);
    }



    /**
     * _more_
     *
     * @param stmt _more_
     * @param handler _more_
     *
     * @throws Exception _more_
     */
    public void iterate(Statement stmt, SqlUtil.ResultsHandler handler)
            throws Exception {
        SqlUtil.Iterator iter = getIterator(stmt);
        ResultSet        results;
        while ((results = iter.getNext()) != null) {
            if ( !handler.handleResults(results)) {
                closeAndReleaseConnection(stmt);

                return;
            }
        }
    }


    /**
     * _more_
     *
     * @param column _more_
     *
     * @return _more_
     */
    public String makeOrderBy(String column) {
        return makeOrderBy(column, true);
    }

    /**
     * _more_
     *
     * @param column _more_
     * @param ascending _more_
     *
     * @return _more_
     */
    public String makeOrderBy(String column, boolean ascending) {
        return " order by " + column + (ascending
                                        ? " asc "
                                        : " desc ");
    }

    /**
     * _more_
     *
     * @param results _more_
     * @param col _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getString(ResultSet results, int col) throws Exception {
        String s = results.getString(col);
        if ((s != null) && s.startsWith(COMPRESS_PREFIX)) {
            s = Utils.uncompress(s.substring(COMPRESS_PREFIX.length()));
        }

        return s;
    }

    /**
     * _more_
     *
     * @param statement _more_
     * @param idx _more_
     * @param name _more_
     * @param value _more_
     * @param maxSize _more_
     *
     * @throws Exception _more_
     */
    public void setString(PreparedStatement statement, int idx, String name,
                          String value, int... maxSize)
            throws Exception {
        if ((value != null) && (maxSize.length > 0)
                && (value.length() > maxSize[0])) {
            int l = value.length();
            value = COMPRESS_PREFIX + Utils.compress(value);
            if (value.length() > maxSize[0]) {
                getRepository().getEntryManager().checkColumnSize(name,
                        value, maxSize[0]);
            }
        }
        statement.setString(idx, checkString(name, value, (maxSize.length > 0)
                ? maxSize[0]
                : 0));
    }


    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     * @param maxSize _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String checkString(String name, String value, int maxSize)
            throws Exception {
        if ((value != null) && (maxSize > 0) && (value.length() > maxSize)) {
            int l = value.length();
            value = COMPRESS_PREFIX + Utils.compress(value);
            if (value.length() > maxSize) {
                getRepository().getEntryManager().checkColumnSize(name,
                        value, maxSize);
            }
        }

        return value;
    }



}
