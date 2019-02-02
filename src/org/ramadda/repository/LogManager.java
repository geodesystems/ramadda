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

package org.ramadda.repository;


import org.apache.log4j.Logger;



import org.ramadda.repository.auth.*;

import org.ramadda.util.HtmlUtils;

import ucar.unidata.util.IOUtil;

import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;





import ucar.unidata.util.StringUtil;

import java.io.*;

import java.io.FileNotFoundException;

import java.sql.SQLException;


import java.text.SimpleDateFormat;


import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;




/**
 *
 *
 *  @author RAMADDA Development Team
 *  @version $Revision: 1.3 $
 */
public class LogManager extends RepositoryManager {

    /** apache style log macro */
    public static final String LOG_MACRO_IP = "%h";

    /** apache style log macro */
    public static final String LOG_MACRO_REQUEST = "%r";

    /** apache style log macro */
    public static final String LOG_MACRO_USERAGENT = "%{User-agent}i";

    /** apache style log macro */
    public static final String LOG_MACRO_REFERER = "%{Referer}i";

    /** apache style log macro */
    public static final String LOG_MACRO_USER = "%u";

    /** apache style log macro */
    public static final String LOG_MACRO_TIME = "%t";

    /** apache style log macro */
    public static final String LOG_MACRO_RESPONSE = "%>s";

    /** _more_ */
    public static final String LOG_MACRO_SIZE = "%b";

    /** apache style log macro */
    public static final String LOG_MACRO_METHOD = "%m";

    /** apache style log macro */
    public static final String LOG_MACRO_PATH = "%U";

    /** apache style log macro */
    public static final String LOG_MACRO_PROTOCOL = "%H";

    /** quote */
    public static final String QUOTE = "\"";


    /** _more_ */
    public static final String LOG_TEMPLATE = LOG_MACRO_IP + " " + "["
                                              + LOG_MACRO_TIME + "] " + QUOTE
                                              + LOG_MACRO_REQUEST + QUOTE
                                              + " " + QUOTE
                                              + LOG_MACRO_REFERER + QUOTE
                                              + " " + QUOTE
                                              + LOG_MACRO_USERAGENT + QUOTE
                                              + " " + LOG_MACRO_RESPONSE
                                              + " " + LOG_MACRO_SIZE;


    /** _more_ */
    public static final String PROP_USELOG4J = "ramadda.logging.uselog4j";

    /** _more_ */
    private boolean LOGGER_OK = true;

    /** _more_ */
    private final LogManager.LogId REPOSITORY_LOG_ID =
        new LogManager.LogId("org.ramadda.repository.log");

    /** _more_ */
    private final LogManager.LogId REPOSITORY_ACCESS_LOG_ID =
        new LogManager.LogId("org.ramadda.repository.access");

    /** _more_ */
    private Hashtable<String, Logger> loggers = new Hashtable<String,
                                                    Logger>();

    /** _more_ */
    public static boolean debug = true;

    /** _more_ */
    private PrintWriter testLogWriter;

    /** _more_ */
    private List<LogEntry> log = new ArrayList<LogEntry>();

    /** _more_ */
    private int requestCount = 0;

    /** _more_ */
    private SimpleDateFormat sdf;

    /**
     * _more_
     *
     * @param repository _more_
     */
    public LogManager(Repository repository) {
        super(repository);
        LOGGER_OK = repository.getProperty(PROP_USELOG4J, true);
        sdf = RepositoryUtil.makeDateFormat(DateHandler.DEFAULT_TIME_FORMAT);
    }


    /**
     * _more_
     */
    public void init() {}

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public void initLogs() throws Exception {
        String testLog = getRepository().getProperty("ramadda.log.test",
                             (String) null);
        if (testLog != null) {
            testLogWriter =
                new PrintWriter(new FileOutputStream(new File(testLog)));
        }
    }


    /**
     * _more_
     *
     * @param request _more_
     */
    public void writeTestLog(Request request) {
        if ((testLogWriter != null) && !request.isPost()
                && !request.getIsRobot() && request.isAnonymous()) {
            testLogWriter.println(
                getRepository().absoluteUrl(request.getUrl()));
            testLogWriter.flush();
        }
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param response _more_
     */
    public void logRequest(Request request, int response) {
        int count = 0;
        requestCount++;
        //Keep the size of the log at 200
        synchronized (log) {
            while (log.size() > 200) {
                log.remove(0);
            }
            log.add(new LogEntry(request));
        }


        String ip        = request.getIp();
        String uri       = request.getRequestPath();
        String method    = request.getHttpServletRequest().getMethod();
        String userAgent = request.getUserAgent("none");
        String time      = sdf.format(new Date());
        String requestPath = method + " " + uri + " "
                             + request.getHttpServletRequest().getProtocol();
        String referer = request.getHttpServletRequest().getHeader("referer");
        if (referer == null) {
            referer = "-";
        }
        String message = LOG_TEMPLATE;

        message = message.replace(LOG_MACRO_IP, ip);
        message = message.replace(LOG_MACRO_TIME, time);
        message = message.replace(LOG_MACRO_METHOD, method);
        message = message.replace(LOG_MACRO_PATH, uri);
        message = message.replace(LOG_MACRO_RESPONSE, "" + response);
        message =
            message.replace(LOG_MACRO_PROTOCOL,
                            request.getHttpServletRequest().getProtocol());
        message = message.replace(LOG_MACRO_REQUEST, requestPath);
        message = message.replace(LOG_MACRO_USERAGENT, userAgent);
        message = message.replace(LOG_MACRO_REFERER, referer);
        message = message.replace(LOG_MACRO_USER, "-");
        message = message.replace(LOG_MACRO_SIZE, "" + count);
        getAccessLogger().info(message);
    }



    /**
     * Create if needed and return the logger
     *
     * @return _more_
     */
    public Logger getLogger() {
        return getLogger(REPOSITORY_LOG_ID);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public Logger getAccessLogger() {
        return getLogger(REPOSITORY_ACCESS_LOG_ID);
    }


    /**
     * _more_
     *
     * @param logId _more_
     *
     * @return _more_
     */
    public Logger getLogger(LogId logId) {
        if (getRepository().getParentRepository() != null) {
            return getRepository().getParentRepository().getLogManager()
                .getLogger();
        }
        //Check if we've already had an error
        if ( !isLoggingEnabled()) {
            return null;
        }

        Logger logger = loggers.get(logId.getId());
        if (logger != null) {
            return logger;
        }
        try {
            logger = Logger.getLogger(logId.getId());
        } catch (Exception exc) {
            LOGGER_OK = false;
            System.err.println("Error getting logger: " + exc);
            exc.printStackTrace();

            return null;
        }
        loggers.put(logId.getId(), logger);

        return logger;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isLoggingEnabled() {
        return LOGGER_OK && (getRepository().getParentRepository() == null);
    }



    /**
     * _more_
     *
     * @param message _more_
     */
    public void debug(String message) {
        debug(getLogger(), message);
    }

    /**
     * _more_
     *
     * @param logger _more_
     * @param message _more_
     */
    public void debug(Logger logger, String message) {
        if (logger != null) {
            logger.debug(message);
        } else {
            System.err.println("RAMADDA DEBUG:" + message);
        }
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public List<LogEntry> getLog() {
        synchronized (log) {
            return new ArrayList<LogEntry>(log);
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getRequestCount() {
        return requestCount;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param message _more_
     */
    public void log(Request request, String message) {
        logInfo("user:" + request.getUser() + " -- " + message);
    }


    /**
     * _more_
     *
     * @param message _more_
     */
    public void logInfoAndPrint(String message) {
        logInfo(message);
        System.err.println(message);
    }


    /**
     * _more_
     *
     * @param message _more_
     */
    public void logInfo(String message) {
        logInfo(getLogger(), message);
    }

    /**
     * _more_
     *
     * @param logId _more_
     * @param message _more_
     */
    public void logInfo(LogId logId, String message) {
        logInfo(getLogger(logId), message);
    }


    /**
     * _more_
     *
     * @param logger _more_
     * @param message _more_
     */
    public void logInfo(Logger logger, String message) {
        if (logger != null) {
            logger.info(message);
        } else {
            System.err.println("RAMADDA INFO:" + message);
        }
    }


    /**
     * _more_
     *
     * @param message _more_
     */
    public void logError(String message) {
        logError(getLogger(), message);
    }

    /**
     * _more_
     *
     * @param logger _more_
     * @param message _more_
     */
    public void logError(Logger logger, String message) {
        if (logger != null) {
            logger.error(message);
        }
        System.err.println("RAMADDA ERROR:" + message);
    }


    /**
     * _more_
     *
     * @param message _more_
     */
    public void logWarning(String message) {
        logWarning(getLogger(), message);
    }

    /**
     * _more_
     *
     * @param logger _more_
     * @param message _more_
     */
    public void logWarning(Logger logger, String message) {
        if (logger != null) {
            logger.warn(message);
        } else {
            System.err.println("RAMADDA WARNING:" + message);
        }
    }

    /**
     * _more_
     *
     * @param logId _more_
     * @param message _more_
     * @param exc _more_
     */
    public void logError(LogId logId, String message, Throwable exc) {
        logError(getLogger(logId), message, exc);
    }

    /**
     * _more_
     *
     * @param message _more_
     * @param exc _more_
     */
    public void logError(String message, Throwable exc) {
        logError(getLogger(), message, exc);
    }

    /**
     * _more_
     *
     * @param log _more_
     * @param message _more_
     * @param exc _more_
     */
    public void logError(Logger log, String message, Throwable exc) {
        message = encode(message);
        Throwable thr = null;
        if (exc != null) {
            thr = LogUtil.getInnerException(exc);
        }

        StringBuffer trace      = new StringBuffer();
        String       stackTrace = ((thr != null)
                                   ? LogUtil.getStackTrace(thr)
                                   : "");
        List<String> lines = StringUtil.split(stackTrace, "\n", true, true);
        for (int i = 0; (i < lines.size()) && (i < 20); i++) {
            trace.append(lines.get(i));
            trace.append("\n");
        }
        if (log == null) {
            System.err.println("RAMADDA ERROR:" + message + " " + thr);
            System.err.println(trace);
        } else if (thr != null) {
            if ((thr instanceof RepositoryUtil.MissingEntryException)
                    || (thr instanceof AccessException)) {
                log.error(message + " " + thr);
            } else {
                log.error(message + "\n<stack>\n" + thr + "\n" + stackTrace
                          + "\n</stack>");
                System.err.println("RAMADDA ERROR:" + message);
                System.err.println(trace);
                if (thr instanceof SQLException) {
                    SQLException sqlException = (SQLException) thr;
                    while ((sqlException = sqlException.getNextException())
                            != null) {
                        log.error("getNextException:" + "\n<stack>\n"
                                  + sqlException + "\n"
                                  + LogUtil.getStackTrace(sqlException)
                                  + "\n</stack>");
                        System.err.println(
                            "getNextException:" + sqlException + "\n"
                            + LogUtil.getStackTrace(sqlException));
                    }
                }

            }
        } else {
            System.err.println("RAMADDA ERROR:" + message);
            log.error(message);
        }

    }



    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    private String encode(String s) {
        //If we do an entityEncode then the log can only be shown through the web
        s = s.replaceAll("([sS][cC][rR][iI][pP][tT])", "_$1_");
        s = s.replace("<", "&lt;");
        s = s.replace(">", "&gt;");

        return s;
    }


    /**
     * Class LogEntry _more_
     *
     *
     * @author RAMADDA Development Team
     */
    public class LogEntry {

        /** _more_ */
        User user;

        /** _more_ */
        Date date;

        /** _more_ */
        String path;

        /** _more_ */
        String ip;

        /** _more_ */
        String userAgent;

        /** _more_ */
        String url;

        /**
         * _more_
         *
         * @param request _more_
         */
        public LogEntry(Request request) {
            this.user = request.getUser();
            this.path = request.getRequestPath();

            String entryPrefix = getRepository().URL_ENTRY_SHOW.toString();
            if (this.path.startsWith(entryPrefix)) {
                url       = request.getUrl();
                this.path = this.path.substring(entryPrefix.length());
                if (path.trim().length() == 0) {
                    path = "/entry/show";
                }

            }

            this.date      = new Date();
            this.ip        = request.getIp();
            this.userAgent = request.getUserAgent();
        }


        /**
         *  Set the Ip property.
         *
         *  @param value The new value for Ip
         */
        public void setIp(String value) {
            ip = value;
        }

        /**
         *  Get the Ip property.
         *
         *  @return The Ip
         */
        public String getIp() {
            return ip;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public String getUrl() {
            return url;
        }

        /**
         *  Set the UserAgent property.
         *
         *  @param value The new value for UserAgent
         */
        public void setUserAgent(String value) {
            userAgent = value;
        }

        /**
         *  Get the UserAgent property.
         *
         *  @return The UserAgent
         */
        public String getUserAgent() {
            return userAgent;
        }




        /**
         *  Set the User property.
         *
         *  @param value The new value for User
         */
        public void setUser(User value) {
            user = value;
        }

        /**
         *  Get the User property.
         *
         *  @return The User
         */
        public User getUser() {
            return user;
        }

        /**
         *  Set the Date property.
         *
         *  @param value The new value for Date
         */
        public void setDate(Date value) {
            date = value;
        }

        /**
         *  Get the Date property.
         *
         *  @return The Date
         */
        public Date getDate() {
            return date;
        }

        /**
         *  Set the Path property.
         *
         *  @param value The new value for Path
         */
        public void setPath(String value) {
            path = value;
        }

        /**
         *  Get the Path property.
         *
         *  @return The Path
         */
        public String getPath() {
            return path;
        }
    }

    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result adminLog(Request request) throws Exception {
        StringBuffer sb       = new StringBuffer();
        List<String> header   = new ArrayList();
        File         f        = getStorageManager().getLogDir();
        File[]       logFiles = f.listFiles();
        String       log      = request.getString(ARG_LOG, "access");
        File         theFile  = null;
        boolean      didOne   = false;

        sb.append(HtmlUtils.sectionOpen());
        sb.append("Logs are in: " + HtmlUtils.italics(f.toString()));
        sb.append(HtmlUtils.p());

        if (log.equals("access")) {
            header.add(HtmlUtils.bold("Recent Access"));
        } else {
            header.add(
                HtmlUtils.href(
                    HtmlUtils.url(
                        request.makeUrl(getAdmin().URL_ADMIN_LOG), ARG_LOG,
                        "access"), "Recent Access"));
        }

        for (File logFile : logFiles) {
            if ( !logFile.toString().endsWith(".log")) {
                continue;
            }
            if (logFile.length() == 0) {
                continue;
            }
            String name  = logFile.getName();
            String label = IOUtil.stripExtension(name);
            label = StringUtil.camelCase(label);
            if (log.equals(name)) {
                header.add(HtmlUtils.bold(label));
                theFile = logFile;
            } else {
                header.add(
                    HtmlUtils.href(
                        HtmlUtils.url(
                            request.makeUrl(getAdmin().URL_ADMIN_LOG),
                            ARG_LOG, name), label));
            }
        }


        sb.append(HtmlUtils.br());
        sb.append(HtmlUtils.space(10));
        sb.append(StringUtil.join(HtmlUtils.span("&nbsp;|&nbsp;",
                HtmlUtils.cssClass(CSS_CLASS_SEPARATOR)), header));
        sb.append(HtmlUtils.hr());

        if (log.equals("access")) {
            getAccessLog(request, sb);
        } else {
            getErrorLog(request, sb, theFile);
        }

        sb.append(HtmlUtils.sectionClose());

        return getAdmin().makeResult(request, msg("Logs"), sb);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     * @param logFile _more_
     *
     * @throws Exception _more_
     */
    private void getErrorLog(Request request, StringBuffer sb, File logFile)
            throws Exception {
        InputStream fis = getStorageManager().getFileInputStream(logFile);
        try {
            String log      = request.getString(ARG_LOG, "error");
            int    numBytes = request.get(ARG_BYTES, 10000);
            if (numBytes < 0) {
                numBytes = 100;
            }
            long length = logFile.length();
            long offset = length - numBytes;
            if (numBytes < length) {
                sb.append(
                    HtmlUtils.href(
                        HtmlUtils.url(
                            getAdmin().URL_ADMIN_LOG.toString(), ARG_LOG,
                            log, ARG_BYTES,
                            "" + (numBytes + 2000)), "More..."));
            }
            sb.append(HtmlUtils.space(2));
            sb.append(
                HtmlUtils.href(
                    HtmlUtils.url(
                        getAdmin().URL_ADMIN_LOG.toString(), ARG_LOG, log,
                        ARG_BYTES, "" + (numBytes - 2000)), "Less..."));

            sb.append(HtmlUtils.br());
            if (offset > 0) {
                fis.skip(offset);
            } else {
                numBytes = (int) length;
            }
            byte[] bytes = new byte[numBytes];
            fis.read(bytes);
            String       logString    = new String(bytes);
            boolean      didOne       = false;
            StringBuffer stackSB      = null;
            boolean      lastOneBlank = false;
            for (String line :
                    StringUtil.split(logString, "\n", false, false)) {
                if ( !didOne) {
                    didOne = true;

                    continue;
                }
                line = line.trim();
                if (line.length() == 0) {
                    if (lastOneBlank) {
                        continue;
                    }
                    lastOneBlank = true;
                } else {
                    lastOneBlank = false;
                }
                if (line.startsWith("</stack>") && (stackSB != null)) {
                    sb.append(
                        HtmlUtils.insetLeft(
                            HtmlUtils.makeShowHideBlock(
                                "Stack trace",
                                HtmlUtils.div(
                                    stackSB.toString(),
                                    HtmlUtils.cssClass(
                                        CSS_CLASS_STACK)), false), 10));
                    sb.append("<br>");
                    stackSB = null;
                } else if (stackSB != null) {
                    line = HtmlUtils.entityEncode(line);
                    line = line.replaceAll("\t", "&nbsp;");
                    stackSB.append(line);
                    stackSB.append("<br>");
                } else if (line.startsWith("<stack>")) {
                    stackSB = new StringBuffer();
                } else {
                    line = HtmlUtils.entityEncode(line);
                    line = line.replaceAll("\t", "&nbsp;");
                    sb.append(line);
                    sb.append("<br>");
                    sb.append("\n");
                }
            }
            if (stackSB != null) {
                sb.append(
                    HtmlUtils.makeShowHideBlock(
                        "Stack trace",
                        HtmlUtils.div(
                            stackSB.toString(),
                            HtmlUtils.cssClass(CSS_CLASS_STACK)), false));
            }

            //        sb.append("</pre>");
        } finally {
            IOUtil.close(fis);
        }
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    private void getAccessLog(Request request, StringBuffer sb)
            throws Exception {

        sb.append(HtmlUtils.open(HtmlUtils.TAG_TABLE));
        sb.append(HtmlUtils.row(HtmlUtils.cols(HtmlUtils.b(msg("User")),
                HtmlUtils.b(msg("Date")), HtmlUtils.b(msg("Path")),
                HtmlUtils.b(msg("IP")), HtmlUtils.b(msg("User agent")))));
        List<LogManager.LogEntry> log = getLogManager().getLog();
        for (int i = log.size() - 1; i >= 0; i--) {
            LogManager.LogEntry logEntry = log.get(i);
            //Encode the path just in case the user does a XSS attack
            String path = logEntry.getPath();
            if (path.length() > 50) {
                path = path.substring(0, 49) + "...";
            }
            path = HtmlUtils.entityEncode(path);
            if (logEntry.getUrl() != null) {
                path = HtmlUtils.href(logEntry.getUrl(), path);
            }
            String userAgent = logEntry.getUserAgent();
            if (userAgent == null) {
                userAgent = "";
            }
            boolean isBot = true;
            if (userAgent.indexOf("Googlebot") >= 0) {
                userAgent = "Googlebot";
            } else if (userAgent.indexOf("Slurp") >= 0) {
                userAgent = "Yahoobot";
            } else if (userAgent.indexOf("msnbot") >= 0) {
                userAgent = "Msnbot";
            } else {
                isBot = false;
                String full = userAgent;
                int    idx  = userAgent.indexOf("(");
                if (idx > 0) {
                    userAgent = userAgent.substring(0, idx);
                    userAgent = HtmlUtils.makeShowHideBlock(
                        HtmlUtils.entityEncode(userAgent), full, false);
                }



            }

            String dttm = getDateHandler().formatDate(logEntry.getDate());
            dttm = dttm.replace(" ", "&nbsp;");
            String user = logEntry.getUser().getLabel();
            user = user.replace(" ", "&nbsp;");
            String cols = HtmlUtils.cols(user, dttm, path, logEntry.getIp(),
                                         userAgent);
            sb.append(HtmlUtils.row(cols,
                                    HtmlUtils.attr(HtmlUtils.ATTR_VALIGN,
                                        "top") + ( !isBot
                    ? ""
                    : HtmlUtils.attr(HtmlUtils.ATTR_BGCOLOR, "#eeeeee"))));

        }
        sb.append(HtmlUtils.close(HtmlUtils.TAG_TABLE));

    }

    /**
     * Class description
     *
     *
     * @version        $version$, Mon, Aug 20, '12
     * @author         Enter your name here...
     */
    public static class LogId {

        /** _more_ */
        private String id;

        /**
         * _more_
         *
         * @param id _more_
         */
        public LogId(String id) {
            this.id = id;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public String getId() {
            return id;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        @Override
        public int hashCode() {
            return id.hashCode();
        }

        /**
         * _more_
         *
         * @param that _more_
         *
         * @return _more_
         */
        public boolean equals(Object that) {
            return id.equals(that);
        }

    }



}
