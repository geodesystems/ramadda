/**
Copyright (c) 2008-2024 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository;


import org.apache.logging.log4j.Logger;
//import org.apache.logging.log4j.LogManager;
//import org.apache.log4j.Logger;


import org.ramadda.repository.auth.*;
import org.ramadda.repository.type.TypeHandler;
import org.ramadda.util.SortableObject;
import org.ramadda.util.seesv.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;
import org.ramadda.util.Utils;

import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.util.zip.*;
import java.io.*;
import java.io.FileNotFoundException;

import java.sql.Connection;
import java.sql.SQLException;

import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.ArrayList;
import java.util.Date;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Hashtable;
import java.util.List;

import org.apache.commons.lang3.text.StrTokenizer;
import org.apache.commons.text.StringTokenizer;



@SuppressWarnings("unchecked")
public class LogManager extends RepositoryManager {

    public static final String ARG_MATCH="match";

    public final RequestUrl URL_LOG = new RequestUrl(this, "/admin/log");
    public final RequestUrl URL_REPORT = new RequestUrl(this, "/admin/log/report");    

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


    /** the log directory property */
    public static final String PROP_LOGDIR = "ramadda.storage.logdir";


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



    public static final String PROP_USELOG4J = "ramadda.logging.uselog4j";
    private boolean LOGGER_OK = true;
    private final LogManager.LogId REPOSITORY_LOG_ID =
        new LogManager.LogId("org.ramadda.repository.ramadda");
    private final LogManager.LogId REPOSITORY_ACCESS_LOG_ID =
        new LogManager.LogId("org.ramadda.repository.access");
    private final LogManager.LogId REPOSITORY_ACTIVITY_LOG_ID =
        new LogManager.LogId("org.ramadda.repository.entry.activity");
    private final LogManager.LogId REPOSITORY_LICENSE_LOG_ID =
        new LogManager.LogId("org.ramadda.repository.license");    
    private static final LogManager.LogId REPOSITORY_SPECIAL_LOG_ID =
        new LogManager.LogId("org.ramadda.repository.special");
    private static final LogManager.LogId REPOSITORY_MONITOR_LOG_ID =
        new LogManager.LogId("org.ramadda.repository.monitor");
    private static final LogManager.LogId REPOSITORY_REGISTRY_LOG_ID =
        new LogManager.LogId("org.ramadda.repository.registry");        
    private Hashtable<String, MyLogger> loggers = new Hashtable<String,
                                                      MyLogger>();

    /** the log directory */
    private File logDir;

    public static boolean debug = true;
    private PrintWriter testLogWriter;
    private List<LogEntry> log = new ArrayList<LogEntry>();
    private int requestCount = 0;
    private SimpleDateFormat sdf;


    public LogManager(Repository repository) {
        super(repository);
        LOGGER_OK = repository.getProperty(PROP_USELOG4J, true);
        sdf = RepositoryUtil.makeDateFormat(DateHandler.DEFAULT_TIME_FORMAT);
    }


    @Override
    public void initAttributes() {
        super.initAttributes();
        LOGGER_OK = repository.getProperty(PROP_USELOG4J, true);
    }

    public void init() {}

    
    public void initLogs() throws Exception {
        String testLog = getRepository().getProperty("ramadda.log.test",
                             (String) null);
        if (testLog != null) {
            testLogWriter =
                new PrintWriter(new FileOutputStream(new File(testLog)));
        }
    }


    
    public void writeTestLog(Request request) {
        if ((testLogWriter != null) && !request.isPost()
                && !request.getIsRobot() && request.isAnonymous()) {
            testLogWriter.println(
                getRepository().absoluteUrl(request.getUrl()));
            testLogWriter.flush();
        }
    }



    
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
	String id = request.getString(ARG_ENTRYID,null);
	if(id!=null) uri = uri +"?" + ARG_ENTRYID +"="+id;

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
        message = message.replaceAll("\\$", "_dollar_");
	message= HU.strictSanitizeString(message);
        MyLogger logger = getAccessLogger();
        if (logger != null) {
            logger.info(message);
        } else {
            System.err.println("no logger:" + message);
        }
    }



    /**
     * Create if needed and return the logger
     *
     * @return _more_
     */
    public MyLogger getLogger() {
        return getLogger(REPOSITORY_LOG_ID);
    }


    
    public MyLogger getAccessLogger() {
        return getLogger(REPOSITORY_ACCESS_LOG_ID);
    }

    
    public MyLogger getEntryActivityLogger() {
        return getLogger(REPOSITORY_ACTIVITY_LOG_ID);
    }


    
    public MyLogger getSpecialLogger() {
        return getLogger(REPOSITORY_SPECIAL_LOG_ID);
    }

    
    public MyLogger getMonitorLogger() {
        return getLogger(REPOSITORY_MONITOR_LOG_ID);
    }        


    
    public MyLogger getRegistryLogger() {
        return getLogger(REPOSITORY_REGISTRY_LOG_ID);
    }    
    

    
    public MyLogger getLicenseLogger() {
        return getLogger(REPOSITORY_LICENSE_LOG_ID);
    }    
    

    
    public MyLogger getLogger(LogId logId) {
        return getLogger(logId.getId());
    }

    
    public MyLogger getLogger(String logId) {
        if (getRepository().getParentRepository() != null) {
            return getRepository().getParentRepository().getLogManager()
                .getLogger(logId);
        }
        MyLogger logger = loggers.get(logId);
        if (logger != null) {
            return logger;
        }

        //Check if we've already had an error
        if ( !isLoggingEnabled()) {
            String id = logId.replaceAll(".*\\.([^\\.]+)$",
                                         "$1").toLowerCase();
            try {
                String      file = getLogDir() + "/" + id + ".my.log";
                PrintWriter pw   =
                    new PrintWriter(new FileOutputStream(file));
                logger = new MyLogger(pw);
                loggers.put(logId, logger);

                return logger;
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }

        }


        try {
            //            logger = Logger.getLogger(logId.getId());
            long t1 = System.currentTimeMillis();
            Logger _logger =
                org.apache.logging.log4j.LogManager.getLogger(logId);
            if (_logger != null) {
                logger = new MyLogger(_logger);
            }
            long t2 = System.currentTimeMillis();
            if (t2 - t1 > 1000) {
                Utils.printTimes("log initialization time:", t1, t2);
            }
        } catch (Exception exc) {
            LOGGER_OK = false;
            System.err.println("Error getting logger: " + exc);
            exc.printStackTrace();
            return null;
        }
        loggers.put(logId, logger);

        return logger;
    }


    
    public boolean isLoggingEnabled() {
        return LOGGER_OK && (getRepository().getParentRepository() == null);
    }



    
    public void debug(String message) {
        debug(getLogger(), message);
    }

    
    public void debug(MyLogger logger, String message) {
        if (logger != null) {
            logger.debug(message);
        } else {
            System.err.println("RAMADDA DEBUG:" + message);
        }
    }


    
    public List<LogEntry> getLog() {
        synchronized (log) {
            return new ArrayList<LogEntry>(log);
        }
    }

    
    public int getRequestCount() {
        return requestCount;
    }



    
    public void log(Request request, String message) {
        logInfo("user:" + request.getUser() + " -- " + message);
    }


    
    public void logInfoAndPrint(String message) {
        logInfo(message);
        System.err.println(message);
    }


    
    public void logInfo(String message) {
        logInfo(getLogger(), message);
    }

    
    public void logInfo(LogId logId, String message) {
        logInfo(getLogger(logId), message);
    }


    
    public void logInfo(String logId, String message) {
        logInfo(getLogger(logId), message);
    }




    
    public void logInfo(MyLogger logger, String message) {
        if (logger != null) {
            logger.info(message);
        } else {
            System.err.println("RAMADDA INFO:" + message);
        }
    }


    
    public void logActivity(Request request, Entry entry, String activity)
            throws Exception {
        MyLogger logger = getEntryActivityLogger();

        List     cols   = new ArrayList();
        cols.add(request.getIp());
        cols.add(request.getUserAgent("none"));
        cols.add(entry.getId());
        cols.add(entry.getName());
        cols.add(activity);
        cols.add(sdf.format(new Date()));
        String message = Utils.columnsToString(cols, ",", false);
        if (logger != null) {
            logger.info(message);
        } else {
            System.err.println("no entry activity logger:" + message);
        }
    }


    public void logSpecial(String message) {
	try {
	    MyLogger logger = getSpecialLogger();
	    message= encode(message);
	    if (logger != null) {
		logger.info(message);
		System.err.println(message);
	    } else {
		System.err.println("special:" + message);
	    }
	} catch(Exception exc) {
	    System.err.println("LogManager: error in logSpecial:" + exc);
	    exc.printStackTrace();
	}
    }

    public void logMonitorError(String message,Throwable...thr) {
	message= encode(message);
	System.err.println("Error:" + message);
	if(thr.length>0)
	    thr[0].printStackTrace();
	logError(getMonitorLogger(),message, thr.length>0?thr[0]:null);
    }


    public void logMonitor(String message) {
	try {
	    message= encode(message);
	    System.err.println(message);
	    getMonitorLogger().info(message);
	} catch(Exception exc) {
	    System.err.println("LogManager: error in logMonitor:" + exc);
	    exc.printStackTrace();
	}
    }



    public void logRegistry(String message,Throwable thr) {
	logError(getRegistryLogger(),message,thr);
    }

    public void logRegistry(String message) {
	try {
	    message = encode(message);
	    MyLogger logger = getRegistryLogger();
	    if (logger != null) {
		logger.info(message);
	    } else {
		System.err.println("registry:" + message);
	    }
	} catch(Exception exc) {
	    System.err.println("LogManager: error in logRegistry:" + exc);
	    exc.printStackTrace();
	}
    }
    
    public void logLicense(String message) {
	try {
	    MyLogger logger = getLicenseLogger();
	    if (logger != null) {
		logger.info(message);
	    } else {
		System.err.println("license:" + message);
	    }
	} catch(Exception exc) {
	    System.err.println("LogManager: error in logLicense:" + exc);
	    exc.printStackTrace();
	}
    }
    

    
    public void logError(String message) {
        logError(getLogger(), message);
    }

    
    public void logError(MyLogger logger, String message) {
        if (logger != null) {
            logger.error(message);
        }
        System.err.println("RAMADDA ERROR:" + message);
    }


    
    public void logWarning(String message) {
        MyLogger logger = getLogger();
        logWarning(logger, message);

    }

    
    public void logWarning(MyLogger logger, String message) {
        if (logger != null) {
            logger.warn(message);
        } else {
            System.err.println("RAMADDA WARNING:" + message);
        }
    }

    
    public void logError(LogId logId, String message, Throwable exc) {
        logError(getLogger(logId), message, exc);
    }

    
    public void logError(String message, Throwable exc) {
        logError(getLogger(), message, exc);
    }

    
    public void logError(MyLogger log, String message, Throwable exc) {
        message = encode(message);
        Throwable thr = null;
        if (exc != null) {
            thr = LogUtil.getInnerException(exc);
        }

        StringBuffer trace      = new StringBuffer();
        String       stackTrace = ((thr != null)
                                   ? LogUtil.getStackTrace(thr)
                                   : "");
        List<String> lines      = Utils.split(stackTrace, "\n", true, true);
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
	//For now don't print the stack trace as the logging above prints it
	if(true) return;
	if(thr!=null) {
	    thr.printStackTrace();
 	} else if(exc!=null) {
	    exc.printStackTrace();
	}

    }



    
    private String encode(String s) {
        //If we do an entityEncode then the log can only be shown through the web
        s = s.replaceAll("([sS][cC][rR][iI][pP][tT])", "_$1_");
        s = s.replace("<", "&lt;");
        s = s.replace(">", "&gt;");

        return s;
    }


    
    public class LogEntry {

        
        User user;

        
        Date date;

        
        String path;

        
        String ip;

        
        String userAgent;

        
        String url;

        
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


        
        public void setIp(String value) {
            ip = value;
        }

        
        public String getIp() {
            return ip;
        }

        
        public String getUrl() {
            return url;
        }

        
        public void setUserAgent(String value) {
            userAgent = value;
        }

        
        public String getUserAgent() {
            return userAgent;
        }




        
        public void setUser(User value) {
            user = value;
        }

        
        public User getUser() {
            return user;
        }

        
        public void setDate(Date value) {
            date = value;
        }

        
        public Date getDate() {
            return date;
        }

        
        public void setPath(String value) {
            path = value;
        }

        
        public String getPath() {
            return path;
        }
    }

    
    public Result adminLog(Request request) throws Exception {
        StringBuffer sb       = new StringBuffer();
        List<String> header   = new ArrayList();
        File         f        = getLogDir();
        File[]       logFiles = f.listFiles();
        String       log      = request.getString(ARG_LOG, "access");
        File         theFile  = null;
        boolean      didOne   = false;

        sb.append(HtmlUtils.sectionOpen());
        sb.append("Logs are in: " + HtmlUtils.italics(f.toString()));
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
            String label = IO.stripExtension(name);
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
	for (File logFile : logFiles) {
            String name  = logFile.getName();
            if (log.equals(name)) {
                theFile = logFile;
		break;
	    }
	}


        sb.append(HtmlUtils.br());
        sb.append(HtmlUtils.space(10));
        sb.append(StringUtil.join(HtmlUtils.span("&nbsp;|&nbsp;",
                HtmlUtils.cssClass(CSS_CLASS_SEPARATOR)), header));
        sb.append(HtmlUtils.hr());

	if(log.indexOf("entryactivity")>=0) {
	    sb.append(HU.div(HU.href(getRepository().getUrlPath(request, URL_REPORT),"Generate Entry Access Report"),
			     HU.attrs("style","margin-bottom:5px;","class","ramadda-button")));
	    sb.append(HU.br());
	}



        if (log.equals("access")) {
            getAccessLog(request, sb);
        } else {
            getErrorLog(request, sb, theFile);
        }

        sb.append(HtmlUtils.sectionClose());

        return getAdmin().makeResult(request, msg("RAMADDA-Admin-Logs"), sb);
    }

    public Result adminLogReport(Request request) throws Exception {
        StringBuilder sb       = new StringBuilder();
	getPageHandler().sectionOpen(request,sb,"Entry Activity Report",false);
	sb.append(request.formPost(URL_REPORT));
	StringBuilder form = new StringBuilder();

	form.append(HU.submit("Generate Access Report", "report"));
	form.append(HU.formTable());
	form.append(HU.formEntry("Date Range:",
				 HU.b("From: ")+
				 getDateHandler().makeDateInput(request, ARG_FROMDATE,
								"",null,null,false,null) +
				 HU.space(2) +
				 HU.b("To: ")+
				 getDateHandler().makeDateInput(request, ARG_TODATE,
								"",null,null,false,null)));

	List initItems = new ArrayList();
	initItems.add(new TwoFacedObject("None",""));
	HU.formEntry(form,"",HU.labeledCheckbox("csv","true",request.get("csv",false),"As CSV"));

	HU.formEntry(form,"",HU.labeledCheckbox("robots","true",request.get("robots",false),"Exclude Bots"));

	HU.formEntry(form,msgLabel("Aggregate at Type"),
		     getRepository().makeTypeSelect(initItems,
						    request, ARG_TYPE, null,
						    false,request.getString(ARG_TYPE,null),false,null,true));

	form.append(HU.formTableClose());
	form.append(HU.div("Select one or more log files to generate a report"));
	boolean doReport = request.exists("report");
        File         logDir        = getLogDir();
        File[]       logFiles = IOUtil.sortFilesOnAge(logDir.listFiles(),true);
	int cnt =0;
	form.append("<div style='max-height:250px;overflow-y:auto;'>");
	List files = request.get("file",new ArrayList<String>());
	for(File f: logFiles) {
	    if(!f.getName().startsWith("entryactivity")) continue;
	    cnt++;
	    String label  =f.getName();
	    label = label.replaceAll("\\.log.*","");
	    label = label.replaceAll("-\\d\\d$","");		
	    label  =label.replace("entryactivity-","");
	    form.append(HU.labeledCheckbox("file",f.getName(),files.contains(f.getName()),label));
	    form.append(HU.br());
	}
	form.append("</div>");
	if(doReport) {
	    sb.append(HU.makeShowHideBlock("Form",form.toString(),false));
	} else {
	    sb.append(form);
	}
	if(cnt==0) {
	    sb.append(HU.div("No entry activity files are available"));
	}
        sb.append(HU.formClose());

	if(doReport) {
	    StringBuilder csv =processAdminLogReport(request,sb);
	    if(csv!=null) {
		return new Result("", csv,"text/csv");
	    }
	}


	getPageHandler().sectionClose(request,sb);
        return getAdmin().makeResult(request, msg("RAMADDA-Admin-Logs"), sb);
    }

    public StringBuilder processAdminLogReport(final Request request,StringBuilder sb) throws Exception {
	List<IO.Path> files = new ArrayList<IO.Path>();
	for(Object f:request.get("file",new ArrayList<String>())) {
	    String      file = getLogDir() + "/" + f;
	    files.add(new IO.Path(file));
	}
	if(files.size()==0) {
	    sb.append("No files selected");
	    return null;
	}
	String _type = request.getString(ARG_TYPE,null);
	if(!stringDefined(_type)) _type=null;
	boolean asCsv = request.get("csv",false);
	final boolean bots = request.get("robots",false);
	final String type = _type;
	final Date fromDate = request.getDate(ARG_FROMDATE,null);
	final Date toDate = request.getDate(ARG_TODATE,null);	
	final SimpleDateFormat sdf =new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
	final SimpleDateFormat sdf2 =new SimpleDateFormat("yyyy-MM-dd HH:mm");
	final Date[] dateRange = {null,null};
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        sdf2.setTimeZone(TimeZone.getTimeZone("UTC"));
	List<Processor> suffix = new ArrayList<Processor>();
	final LinkedHashMap<String,Integer> counts = new LinkedHashMap<String,Integer>();
	final int[] cnt={0};
	final int[] ecnt={0};	
	final Hashtable<String,Entry> entries = new Hashtable<String,Entry>();
	suffix.add(new Processor() {
		public Row processRow(TextReader ctx, Row row) throws Exception {
		    String ip=row.getString(0);
		    String client=row.getString(1);		    
		    String id=row.getString(2);
		    String name=row.getString(3);
		    String action=row.getString(4);		    
		    String date=row.getString(5);
		    if(!action.equals("view") || id.equals("entryid")) return row;
		    if(bots && request.checkForRobot(client)) {
			System.err.println("bot:" + client);
			return row;
		    }
		    Date dttm = sdf.parse(date);
		    if(fromDate!=null || toDate!=null) {
			long rowTime = dttm.getTime();
			if(fromDate!=null && rowTime < fromDate.getTime()) return row;
			if(toDate!=null && rowTime>toDate.getTime()) return row;
		    }
		    if(dateRange[0]==null) dateRange[0] = dttm;
		    dateRange[1] = dttm;

		    if(type!=null) {
			try {
			    Entry entry = entries.get(id);
			    if(entry==null) {
				entry = getEntryManager().getEntry(request,id);
				if(entry!=null) entries.put(id,entry);
			    }
			    while(entry!=null) {
				if(entry.getTypeHandler().isType(type)) {
				    id = entry.getId();
				    name = entry.getName();
				    break;
				}
				entry=entry.getParentEntry();
			    }
			} catch(Exception exc) {
			    System.err.println("Error:" + exc);
			    exc.printStackTrace();
			}
		    }

		    cnt[0]++;
		    id = id +"name:"+name;
		    Integer c = counts.get(id);
		    if(c==null) {
			counts.put(id,c=new Integer(0));
			ecnt[0]++;
		    }
		    int n = c.intValue()+1;
		    counts.put(id,new Integer(n));
		    return row;
		}
	    });
	String[]args = {"-header",
	    "ip,client,entryid,name,action,date",
	    "-match", "action", "view"};


	long t1 = System.currentTimeMillis();
	Seesv seesv = new Seesv(args,suffix);

	seesv.run(files);
	long t2 = System.currentTimeMillis();

	String header="# requests: " + cnt[0] +"  #entries:" + ecnt[0];
	if(dateRange[0]!=null) {
	    header+=" date range: " + sdf2.format(dateRange[0]) +" - " +sdf2.format(dateRange[1]);
	}
	sb.append(HU.div(header));

	sb.append("<table width=100%>");
	sb.append("<tr><td width=10%><b>Count</b></td><td width=20%><b>Entry type</b></td><td><b>Entry</b></td></tr>");
        List<SortableObject<String>> sort =
            new ArrayList<SortableObject<String>>();	

	for (String key : counts.keySet()) {
	    Integer c = counts.get(key);
	    sort.add(new SortableObject<String>(c,key));
	}

	StringBuilder csv  = new StringBuilder();
	if(asCsv)csv.append("count,type,id,entry\n");
        java.util.Collections.sort(sort,Comparator.reverseOrder());
	long t3 = System.currentTimeMillis();
	Connection connection = getDatabaseManager().getConnection();
        for (SortableObject<String> po : sort) {
	    int  c = po.getPriority();
	    String id = StringUtil.findPattern(po.getValue(),"(.*?)name:");
	    String key = StringUtil.findPattern(po.getValue(),"name:(.*)");
	    String entryType="";
	    if(id!=null) {
		TypeHandler typeHandler= getEntryManager().getEntryTypeHandler(request,connection,id);
		if(typeHandler!=null) entryType=typeHandler.getDescription();
	    }

	    if(asCsv){
		csv.append(c);
		csv.append(",");				
		csv.append(Seesv.cleanColumnValue(entryType));
		csv.append(",");
		csv.append(Seesv.cleanColumnValue(id));
		csv.append(",");						
		csv.append(Seesv.cleanColumnValue(key));
		csv.append("\n");				
	    } else {
		String link = "";
		if(files.size()==1) {
		    link = HU.href(HU.url(URL_LOG.toString(),ARG_MATCH,id,
					  ARG_LOG,files.get(0).getFile().getName()),
				   HU.getIconImage("fas fa-search"),
				   HU.attrs("target","logsearch","title","View log file"));
		}


		key = HU.href(getEntryManager().getEntryUrl(request, id),key,HU.attrs("target","_entry"));
		sb.append("<tr><td align=right width=10%>");
		sb.append(HU.div(""+c,HU.style("margin-right:8px;")));
		sb.append("</td><td>");
		sb.append(entryType);
		sb.append("</td><td>");
		sb.append(link);
		sb.append(" ");
		sb.append(key);
		sb.append("</td></tr>");
	    }
	}
	getDatabaseManager().closeConnection(connection);
	sb.append("</table>");
	long t4 = System.currentTimeMillis();
	//	Utils.printTimes("log",t1,t2,t3,t4);
	if(asCsv) return csv;
	return null;

    }



    
    public File getLogDir() {
        if (logDir != null) {
            return logDir;
        }

        synchronized (PROP_LOGDIR) {
            //Check for race conditions
            if (logDir != null) {
                return logDir;
            }


            File tmpLogDir =
                getStorageManager().getFileFromProperty(PROP_LOGDIR);
            if (getRepository().isReadOnly()) {
                //                System.out.println("RAMADDA: skipping log4j");
                logDir = tmpLogDir;

                return logDir;
            }

            if ( !getLogManager().isLoggingEnabled()) {
                //                System.out.println("RAMADDA: skipping log4j");
                logDir = tmpLogDir;

                return logDir;
            }

            File log4JFile = new File(tmpLogDir + "/" + "log4j.properties");
            //For now always write out the log from the jar
            if (true || !log4JFile.exists()) {
                try {
                    String c =
                        IOUtil.readContents(
                            "/org/ramadda/repository/resources/log4j.properties",
                            getClass());
                    String logDirPath = tmpLogDir.toString();
                    //Replace for windows
                    logDirPath = logDirPath.replace("\\", "/");
                    c          = c.replace("${ramadda.logdir}", logDirPath);
                    c = c.replace("${file.separator}", File.separator);
                    IOUtil.writeFile(log4JFile, c);
                } catch (Exception exc) {
                    System.err.println(
                        "RAMADDA: Error writing log4j properties:" + exc);

                    throw new RuntimeException(exc);
                }
            }
            try {
                java.util.Properties props = System.getProperties();
                props.put("log4j2.configurationFile", log4JFile.toString());
                props.put("LOG4J_FORMAT_MSG_NO_LOOKUPS", "true");
                //                org.apache.log4j.PropertyConfigurator.configure(log4JFile.toString());
            } catch (Exception exc) {
                System.err.println("RAMADDA: Error configuring log4j:" + exc);
                exc.printStackTrace();
            }
            logDir = tmpLogDir;

            return logDir;
        }
    }





    private void getErrorLog(Request request, StringBuffer sb, File logFile)
            throws Exception {
	int    numBytes = request.get(ARG_BYTES, 10000);
	String match=request.getString(ARG_MATCH,"");
	boolean isEntryActivity = logFile.getName().indexOf("entryactivity")>=0;
	String field = request.getString("field",null);
	boolean showSummary = stringDefined(field);
        StringTokenizer tokenizer = StringTokenizer.getCSVInstance();
	LinkedHashMap<String,Integer> counts = new LinkedHashMap<String,Integer>();

	sb.append(request.form(URL_LOG));
	sb.append(HU.hidden(ARG_LOG,request.getString(ARG_LOG,"")));
	sb.append(HU.hidden(ARG_BYTES,""+numBytes));
	sb.append(HU.submit("View Log", "viewlog"));
	sb.append(HU.space(1));
	if(isEntryActivity) {
	    List fields = new ArrayList();
	    fields.add(new TwoFacedObject("---",""));
	    fields.add(new TwoFacedObject("IP Address","ipaddress"));
	    fields.add(new TwoFacedObject("Entry","entry"));	    
	    sb.append(HU.space(1));
	    sb.append(HU.b("Summarize on:"));
	    sb.append(HU.space(1));
	    sb.append(HU.select("field",fields,field));
	    sb.append(HU.space(1));
	}
	sb.append(HU.input(ARG_MATCH,match,HU.attrs("placeholder","match")));
        sb.append(HU.formClose());


        try(InputStream fis1 = getStorageManager().getFileInputStream(logFile)) {
	    InputStream fis = fis1;
	    if (logFile.getName().toLowerCase().endsWith(".gz")) {
		fis = new GZIPInputStream(fis1);
	    }
	    

            String log      = request.getString(ARG_LOG, "error");
            if (numBytes < 0) {
                numBytes = 1000;
            }

            long length = logFile.length();
            long offset = length - numBytes;
            if (numBytes < length) {
                sb.append(
                    HtmlUtils.href(
                        HtmlUtils.url(
                            getAdmin().URL_ADMIN_LOG.toString(), ARG_LOG,
                            log, ARG_MATCH,match,ARG_BYTES,
                            "" + (numBytes + 10000)), "More..."));
            }
            sb.append(HtmlUtils.space(2));
            sb.append(
                HtmlUtils.href(
                    HtmlUtils.url(
                        getAdmin().URL_ADMIN_LOG.toString(), ARG_LOG, log,
			ARG_MATCH,match,
                        ARG_BYTES, "" + (numBytes - 10000)), "Less..."));

	    int maxLines=  -1;
	    if(!showSummary && !stringDefined(match)) {
		match=null;
		if (offset > 0) {
		    fis.skip(offset);
		}
	    } else {
		maxLines = 10000;
	    }



            sb.append(HtmlUtils.br());
            boolean      didOne       = false;
            StringBuffer stackSB      = null;
            boolean      lastOneBlank = false;
	    if(match!=null) match = match.toLowerCase();
	    BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
	    String line=null;
	    int cnt = 0;
            while ((line = reader.readLine()) != null) {
		if(match!=null && line.toLowerCase().indexOf(match)<0) continue;
		line = HU.strictSanitizeString(line);
		//skip the first one since it might be partial
		if (offset>0 &&  !didOne) {
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
		cnt++;
		if(maxLines>0 && cnt> maxLines) break;
		if(showSummary) {
		    tokenizer.reset(line);
		    String tokens[] = tokenizer.getTokenArray();
		    String key = tokens[0];
		    if(field.equals("entry")) key = tokens[2]+"::" + tokens[3];
		    Integer count = counts.get(key);
		    if(count==null) {
			count=new Integer(0);
		    }
		    counts.put(key,count.intValue()+1);
		    continue;
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
	}

	if(showSummary) {
	    List<SortableObject<String>> sort =
		new ArrayList<SortableObject<String>>();	
	    for (String key : counts.keySet()) {
		Integer c = counts.get(key);
		sort.add(new SortableObject<String>(c,key));
	    }
	    java.util.Collections.sort(sort,Comparator.reverseOrder());
	    sb.append("<table>");
	    String fieldLabel = "IP Address";
	    if(field.equals("entry")) fieldLabel = "Entry";
	    sb.append("<tr><td>&nbsp;<b>Count&nbsp;</b></td><td><b>&nbsp;"+fieldLabel+"</b>&nbsp;</td></tr>");

	    for (SortableObject<String> po : sort) {
		int  c = po.getPriority();
		String label = po.getValue();
		if(field.equals("entry")) {
		    List<String>toks = Utils.split(label,"::");
		    label= HU.href(getEntryManager().getEntryUrl(request,toks.get(0)),toks.get(1),HU.attrs("target","_entry"));		    
		}
		sb.append("<tr><td align=right>");
		sb.append(c);
		sb.append("&nbsp;&nbsp;</td><td>");

		sb.append(label);
		sb.append("</td></tr>");
	    }
	    sb.append("</table>");
	}


    }


    
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
		userAgent = HU.strictSanitizeString(userAgent);
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
            String user = (logEntry.getUser() != null)
                          ? logEntry.getUser().getLabel()
                          : "anonymous";
            user = user.replace(" ", "&nbsp;");
            String cols = HtmlUtils.cols(HU.strictSanitizeStrings(user, dttm, path, logEntry.getIp()));
	    cols+=HU.cols(userAgent);
            sb.append(HtmlUtils.row(cols,
                                    HtmlUtils.attr(HtmlUtils.ATTR_VALIGN,
                                        "top") + ( !isBot
                    ? ""
                    : HtmlUtils.attr(HtmlUtils.ATTR_BGCOLOR, "#eeeeee"))));

        }
        sb.append(HtmlUtils.close(HtmlUtils.TAG_TABLE));

    }

    
    public static class LogId {

        
        private String id;

        
        public LogId(String id) {
            this.id = id;
        }

        
        public String getId() {
            return id;
        }

        
        @Override
        public int hashCode() {
            return id.hashCode();
        }

        
        public boolean equals(Object that) {
            return id.equals(that);
        }

    }


    
    private static class MyLogger {

        
        Logger logger;

        
        PrintWriter pw;

        
        MyLogger(Logger logger) {
            this.logger = logger;
        }

        
        MyLogger(PrintWriter pw) {
            this.pw = pw;
        }

        
        MyLogger() {}

        
        public void info(String message) {
            if (logger != null) {
                logger.info(message);
            } else if (pw != null) {
                synchronized (pw) {
                    pw.println(message);
                    pw.flush();
                }
            } else {
                System.err.println(message);
            }
        }

        
        public void debug(String message) {
            if (logger != null) {
                logger.debug(message);
            } else if (pw != null) {
                pw.println(message);
                pw.flush();
            } else {
                System.err.println(message);
            }
        }

        
        public void warn(String message) {
            if (logger != null) {
                logger.warn(message);
            } else if (pw != null) {
                pw.println(message);
                pw.flush();
            } else {
                System.err.println(message);
            }
        }

        
        public void error(String message) {
            if (logger != null) {
                logger.error(message);
            } else if (pw != null) {
                pw.println(message);
                pw.flush();
            } else {
                System.err.println(message);
            }
        }


    }

}
