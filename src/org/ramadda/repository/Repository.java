/**
   Copyright (c) 2008-2023 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/



package org.ramadda.repository;


import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;

import org.ramadda.util.HttpFormEntry;

import org.json.*;



import org.ramadda.repository.admin.Admin;
import org.ramadda.repository.admin.AdminHandler;
import org.ramadda.repository.admin.MailManager;
import org.ramadda.repository.auth.AccessException;
import org.ramadda.repository.auth.AccessManager;
import org.ramadda.repository.auth.AuthorizationMethod;
import org.ramadda.repository.auth.SessionManager;
import org.ramadda.repository.auth.User;
import org.ramadda.repository.auth.UserManager;
import org.ramadda.repository.database.DatabaseManager;
import org.ramadda.repository.database.Tables;
import org.ramadda.repository.ftp.FtpManager;
import org.ramadda.repository.harvester.HarvesterManager;
import org.ramadda.repository.job.JobManager;
import org.ramadda.repository.map.MapManager;
import org.ramadda.repository.metadata.ContentMetadataHandler;
import org.ramadda.repository.metadata.Metadata;
import org.ramadda.repository.metadata.MetadataManager;
import org.ramadda.repository.monitor.MonitorManager;
import org.ramadda.repository.output.CalendarOutputHandler;
import org.ramadda.repository.output.HtmlOutputHandler;
import org.ramadda.repository.output.ImageOutputHandler;
import org.ramadda.repository.output.JsonOutputHandler;
import org.ramadda.repository.output.OutputHandler;
import org.ramadda.repository.output.OutputType;
import org.ramadda.repository.output.WikiManager;
import org.ramadda.repository.output.XmlOutputHandler;
import org.ramadda.repository.output.ZipOutputHandler;
import org.ramadda.repository.search.SearchManager;

import java.awt.Image;

import java.awt.Toolkit;
import java.awt.image.*;
import java.awt.image.BufferedImage;
import javax.imageio.*;
import org.ramadda.repository.server.RepositoryServlet;

import org.ramadda.repository.type.Column;
import org.ramadda.repository.type.GroupTypeHandler;
import org.ramadda.repository.type.ProcessFileTypeHandler;
import org.ramadda.repository.type.TypeHandler;
import org.ramadda.repository.util.ServerInfo;

import org.ramadda.service.Service;
import org.ramadda.util.CategoryBuffer;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;
import org.ramadda.util.S3File;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.MyTrace;
import org.ramadda.util.TTLCache;
import org.ramadda.util.text.Seesv;


import org.ramadda.util.PropertyProvider;
import org.ramadda.util.StreamEater;
import org.ramadda.util.TTLObject;
import org.ramadda.util.Utils;
import org.ramadda.util.geo.GeoUtils;
import org.ramadda.util.sql.Clause;
import org.ramadda.util.sql.SqlUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.w3c.dom.NodeList;


import ucar.unidata.util.CacheManager;
import ucar.unidata.util.Counter;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlEncoder;
import ucar.unidata.xml.XmlUtil;

import java.awt.Toolkit;

import java.io.*;
import java.nio.charset.Charset;
import java.lang.reflect.Constructor;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;

import java.security.cert.X509Certificate;

import java.sql.ResultSet;
import java.sql.Statement;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import java.util.TimeZone;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import java.util.zip.*;

import javax.net.ssl.*;




/**
 * The main RAMADDA class.
 *
 */
@SuppressWarnings("unchecked")
public class Repository extends RepositoryBase implements RequestHandler,
							  PropertyProvider {


    private static boolean  debugGpt = false;

    public static final boolean debugInit = false;

    /** dummy field 2 */
    private static final org.ramadda.util.ObjectPool dummyField2ToForceCompile =
        null;



    /** dummy field 3 */
    private static final org.ramadda.util.EntryGroup dummyField3ToForceCompile =
        null;

    /** dummy field 4 */
    private static final org.ramadda.util.geo.GeoUtils dummyField4ToForceCompile =
        null;

    /** dummy field 5 */
    private static final org.ramadda.data.services.PointOutputHandler dummyField5ToForceCompile =
        null;


    /** dummy field 6 */
    private static final org.ramadda.repository.job.JobManager dummyField6ToForceCompile =
        null;

    /** _more_ */
    private static final org.ramadda.data.docs.TabularOutputHandler dummyField7 =
        null;


    /** _more_ */
    private static final org.ramadda.util.text.Seesv dummyField7ToForceCompile =
        null;

    /** Cache resoruces property */
    public static final String PROP_CACHE_RESOURCES = "ramadda.cacheresources";
    public static final String PROP_CACHE_HTDOCS = "ramadda.cachehtdocs";    

    /** Entry edit URLs */
    protected List<RequestUrl> entryEditUrls;

    /** Group edit URLs */
    protected List<RequestUrl> groupEditUrls;

    /** list of initialized URLs */
    List<RequestUrl> initializedUrls = new ArrayList<RequestUrl>();

    /** page cache limit */
    private static final int PAGE_CACHE_LIMIT = 100;


    /** Deleter output type */
    public static final OutputType OUTPUT_DELETER =
        new OutputType("Delete Entry", "repository.delete",
                       OutputType.TYPE_ACTION | OutputType.TYPE_EDIT, "",
                       ICON_DELETE);


    /** Change type output type */
    public static final OutputType OUTPUT_TYPECHANGE =
        new OutputType("Change Type", "repository.typechange",
                       OutputType.TYPE_ACTION | OutputType.TYPE_EDIT, "",
                       "/icons/shape_rotate_clockwise.png");

    /** Change type output type */
    public static final OutputType OUTPUT_MAKESNAPSHOT =
        new OutputType("Make Snapshot", "repository.makesnapshot",
                       OutputType.TYPE_OTHER | OutputType.TYPE_FILE, "",
                       "fas fa-save");

    /** Publish OutputType */
    public static final OutputType OUTPUT_PUBLISH =
        new OutputType("Make Public", "repository.makepublic",
                       OutputType.TYPE_ACTION | OutputType.TYPE_EDIT, "",
                       ICON_PUBLISH);


    /** Full Metadata OutputType */
    public static final OutputType OUTPUT_METADATA_FULL =
        new OutputType("Add full metadata", "repository.metadata.full",
                       OutputType.TYPE_ACTION | OutputType.TYPE_EDIT, "",
                       ICON_METADATA_ADD);

    /** short metadata OutputType */
    public static final OutputType OUTPUT_METADATA_SHORT =
        new OutputType("Add short metadata", "repository.metadata.short",
                       OutputType.TYPE_ACTION | OutputType.TYPE_EDIT, "",
                       ICON_METADATA_ADD);


    /** Copy OutputType */
    public static final OutputType OUTPUT_COPY =
        new OutputType("Move/Copy/Link", "repository.copymovelink",
                       OutputType.TYPE_ACTION | OutputType.TYPE_EDIT, "",
                       ICON_MOVE);

    /** File Listing OutputType */
    public static final OutputType OUTPUT_FILELISTING =
        new OutputType("File Listing", "repository.filelisting",
                       OutputType.TYPE_OTHER, "",
                       ICON_FILELISTING);


    /** the stand alone server */
    private Object standAloneServer;


    /** The UserManager */
    private UserManager userManager;

    /** The MonitorManager */
    private MonitorManager monitorManager;

    /** The SessionManager */
    private SessionManager sessionManager;

    /** _more_ */
    private JobManager jobManager;

    /** The WikiManager */
    private WikiManager wikiManager;

    /** The LogManager */
    private LogManager logManager;

    /** The EntryManager */
    private EntryManager entryManager;

    /** _more_ */
    private ExtEditor extEditor;

    /** _more_ */
    private EntryUtil entryUtil;

    /** The CommentManager */
    private CommentManager commentManager;

    /** The PageHandler */
    private PageHandler pageHandler;

    /** The DateHandler */
    private DateHandler dateHandler;

    /** The AssociationManager */
    private AssociationManager associationManager;

    /** The SearchManager */
    private SearchManager searchManager;

    /** The MapManager */
    private MapManager mapManager;

    /** The HarvesterManager */
    private HarvesterManager harvesterManager;

    /** The ActionManager */
    private ActionManager actionManager;

    /** The AccessManager */
    private AccessManager accessManager;

    /** The Meeta-dataManager */
    private MetadataManager metadataManager;

    /** The RegistryManager */
    private RegistryManager registryManager;

    /** The MailManager */
    private MailManager mailManager;

    /** The LocalRepositoryManager */
    private LocalRepositoryManager localRepositoryManager;

    /** The StorageManager */
    private StorageManager storageManager;

    /** The API Manager */
    private ApiManager apiManager;

    /** The PluginManager */
    private PluginManager pluginManager;

    /** The DatabaseManager */
    private DatabaseManager databaseManager;

    /** The FTP Manager */
    private FtpManager ftpManager;

    /** A global FtpManager */
    private static FtpManager globalFtpManager;

    /** The Admin */
    private Admin admin;

    /** The list of RepositoryMangers */
    private List<RepositoryManager> repositoryManagers =
        new ArrayList<RepositoryManager>();

    /** The Parent Repository */
    private Repository parentRepository;

    /** The list of children */
    private List<Repository> childRepositories = new ArrayList<Repository>();

    /** The cookie expiration date */
    private String cookieExpirationDate;

    /** _more_ */
    private Properties mimeTypes;

    /** _more_ */
    private Properties localProperties = new Properties();

    /** _more_ */
    private Properties pluginProperties = new Properties();

    /** _more_ */
    private Properties cmdLineProperties = new Properties();

    /** _more_ */
    private Properties coreProperties = new Properties();


    /** _more_ */
    private Map<String, String> systemEnv;

    /** _more_ */
    private TTLObject<Properties> dbProperties;

    private Properties dbPropertiesDummy = new Properties();    

    /** _more_ */
    private long baseTime = System.currentTimeMillis();

    /** _more_ */
    ucar.unidata.util.SocketConnection dummyConnection;

    /** _more_ */
    private List<String> sqlLoadFiles = new ArrayList<String>();

    /** _more_ */
    private List<EntryChecker> entryMonitors = new ArrayList<EntryChecker>();

    private List<String[]>httpHeaders = new ArrayList<String[]>();

    /** _more_ */
    private String dumpFile;

    /** _more_ */
    private String startupScript;

    /** _more_ */
    private Date startTime = new Date();


    /** _more_ */
    private HashSet blacklist;

    /** _more_ */
    private Hashtable<String, TypeHandler> typeHandlersMap =
        new Hashtable<String, TypeHandler>();

    /** _more_ */
    private List<TypeHandler> allTypeHandlers = new ArrayList<TypeHandler>();

    /** _more_ */
    private List<OutputHandler> outputHandlers =
        new ArrayList<OutputHandler>();


    /** _more_ */
    private Hashtable<String, OutputHandler> outputHandlerMap =
        new Hashtable<String, OutputHandler>();


    /** _more_ */
    private Hashtable<String, OutputType> outputTypeMap =
        new Hashtable<String, OutputType>();

    /** _more_ */
    private Hashtable resources = new Hashtable();


    /** _more_ */
    private Hashtable namesHolder = new Hashtable();


    /** _more_ */
    private List<User> cmdLineUsers = new ArrayList();


    /** _more_ */
    String[] args;


    /** _more_ */
    public static boolean debug = false;


    /** _more_ */
    public static boolean debugSession = false;


    /** _more_ */
    private GroupTypeHandler groupTypeHandler;

    private TypeHandler fileTypeHandler;    



    /** _more_ */
    private List categoryList = null;

    /** _more_ */
    private List<String> htdocRoots = new ArrayList<String>();

    /** _more_ */
    private int htdocsCacheSize = 0;

    private boolean cacheHtdocs = true;
    
    /** _more_ */
    private static final int HTDOCS_CACHE_LIMIT = 10_000_000;

    /** _more_ */
    private Hashtable<String, byte[]> htdocsCache = new Hashtable<String,
	byte[]>();


    /** _more_ */
    private Hashtable<String, String> htdocsPathCache = new Hashtable<String,
	String>();



    /** _more_ */
    private List<File> localFilePaths = new ArrayList<File>();


    /** _more_ */
    private HttpClient httpClient;

    /** _more_ */
    private boolean repositoryInitialized = false;

    /** _more_ */
    private boolean isActive = true;


    /** _more_ */
    private boolean readOnly = false;

    /** _more_ */
    private boolean doCache = true;


    /** _more_ */
    private boolean adminOnly = false;

    /** _more_ */
    private boolean requireLogin = false;

    /** _more_ */
    private boolean allSsl = false;

    /** _more_ */
    private boolean sslIgnore = false;

    /** _more_ */
    private boolean cacheResources = false;

    /** _more_ */
    private String repositoryName = "Repository";

    /** _more_ */
    private String repositoryDescription = "";

    private boolean logActivityToFile = false;
    private boolean logActivityToDatabase = false;    

    /** _more_ */
    private boolean downloadOk = true;

    /** _more_ */
    private boolean minifiedOk = true;

    private boolean acceptRobots = true;    

    private boolean commentsEnabled  =false;


    private boolean useFixedHostName = false;

    private boolean corsOk = true;

    private boolean streamOutput = false;


    /** _more_ */
    private boolean cdnOk = false;

    /** _more_ */
    private boolean enableHostnameMapping = true;

    /** _more_ */
    private String language = "";

    /** _more_ */
    private String languageDefault = "";


    /** _more_ */
    private int overridePort = -1;

    /** _more_ */
    private boolean ignoreSSL = false;

    public static final int GPT_TOKEN_LIMIT = 3000;


    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public Repository() throws Exception {}

    /**
     * _more_
     *
     * @param parentRepository _more_
     * @param args _more_
     * @param port _more_
     *
     * @throws Exception _more_
     */
    public Repository(Repository parentRepository, String[] args, int port)
	throws Exception {
        super(port);
        this.parentRepository = parentRepository;
        init(args, port);
    }


    /**
     * _more_
     *
     * @param args _more_
     * @param port _more_
     *
     * @throws Exception _more_
     */
    public Repository(String[] args, int port) throws Exception {
        this(null, args, port);
    }



    /**
     * _more_
     *
     * @param args _more_
     * @param port _more_
     *
     * @throws Exception _more_
     */
    public void init(String[] args, int port) throws Exception {
        //NOTE: Only do this for now so we can have snotel data
        trustAllCertificates();

	//In later java versions this disables serialization
	//ObjectInputFilter.Config.setSerialFilter(info -> ObjectInputFilter.Status.REJECTED);
	//this disables jndi
	javax.naming.spi.NamingManager.setInitialContextFactoryBuilder(env -> { throw new javax.naming.NamingException("JNDI disabled"); });

        setPort(port);
        LogUtil.setTestMode(true);
        //This takes a second or two so do it in a thread
        Misc.run(new Runnable() {
		public void run() {
		    try {
			//                      System.err.println("calling getLocalHost");
			java.net.InetAddress localMachine =
			    java.net.InetAddress.getLocalHost();
			//                      System.err.println("after getLocalHost:" +localMachine.getHostName() +" " +
			//                                         localMachine.getHostAddress());                         
			setHostname(localMachine.getHostName());
			setIpAddress(localMachine.getHostAddress());
		    } catch (Exception exc) {
			System.err.println(
					   "Got exception accessing local hostname");
			//            exc.printStackTrace();
			setHostname("unknown");
			setIpAddress("unknown");
		    }
		}
	    });

        this.args     = args;

        entryEditUrls = RequestUrl.toList(new RequestUrl[] {
		URL_ENTRY_FORM, URL_ENTRY_EXTEDIT,
		URL_ENTRY_ACTIVITY,
		getMetadataManager().URL_METADATA_FORM,
		getMetadataManager().URL_METADATA_ADDFORM,
		URL_ACCESS_FORM  //,
		//        URL_ENTRY_DELETE
		//        URL_ENTRY_SHOW
	    });

        groupEditUrls = RequestUrl.toList(new RequestUrl[] {
		URL_ENTRY_NEW, URL_ENTRY_FORM, URL_ENTRY_EXTEDIT,
		URL_ENTRY_ACTIVITY,
		getMetadataManager().URL_METADATA_FORM,
		getMetadataManager().URL_METADATA_ADDFORM,
		URL_ACCESS_FORM  //,
		//        URL_ENTRY_DELETE
		//        URL_ENTRY_SHOW
	    });



    }



    /**
     * _more_
     *
     * @return _more_
     */
    public List<RepositoryManager> getRepositoryManagers() {
        return repositoryManagers;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getHostname() {
        String hostname = getProperty(PROP_HOSTNAME, (String) null);
        if (Utils.stringDefined(hostname)) {
            if (hostname.equals("ipaddress")) {
                return getIpAddress();
            }

            return hostname;
        }

        return super.getHostname();
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean useFixedHostnameForAbsoluteUrls() {
        return useFixedHostName;
    }

    /**
     *  Are cross origin requests OK
     *  Right now we only use this for JSON
     *
     * @return _more_
     */
    public boolean isCORSOk() {
	return corsOk;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public int getPort() {
        if (overridePort > 0) {
            return overridePort;
        }
        String port = getProperty(PROP_PORT, (String) null);
        if (Utils.stringDefined(port)) {
            return Integer.decode(port.trim()).intValue();
        }

        return super.getPort();
    }



    /**
     * _more_
     *
     * @param value _more_
     */
    public void setPort(int value) {
        super.setPort(value);
        overridePort = value;
    }


    /**
     * _more_
     *
     *
     * @param request The request
     * @return _more_
     */
    public boolean isSSLEnabled(Request request) {
        //Defer to the parent
        if (parentRepository != null) {
            return parentRepository.isSSLEnabled(request);
        }
        if (ignoreSSL) {
            return false;
        }
        if (sslIgnore) {
            return false;
        }
        return getHttpsPort() >= 0;
    }


    /**
     *  Set the standAlone server property.
     *
     *  @param value The new value for standAlone server
     */
    public void setStandAloneServer(Object value) {
        standAloneServer = value;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public Object getStandAloneServer() {
        return standAloneServer;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isChild() {
        return parentRepository != null;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isMaster() {
        //For now always enable this
        return true;
        //        return getProperty(LocalRepositoryManager.PROP_MASTER_ENABLED,false);
    }



    /**
     * _more_
     *
     * @param parent _more_
     */
    private void setParentRepository(Repository parent) {
        this.parentRepository = parent;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public Repository getParentRepository() {
        return parentRepository;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List<Repository> getChildRepositories() {
        return new ArrayList<Repository>(childRepositories);
    }

    //

    /**
     * _more_
     *
     * @param childRepository _more_
     *
     * @throws Exception _more_
     */
    public void removeChildRepository(Repository childRepository)
	throws Exception {
        childRepositories.remove(childRepository);
    }


    /**
     * _more_
     *
     * @param childRepository _more_
     *
     * @throws Exception _more_
     */
    public void addChildRepository(Repository childRepository)
	throws Exception {
        childRepositories.add(childRepository);
        childRepository.setParentRepository(this);

        int sslPort = getHttpsPort();
        if (sslPort > 0) {
            childRepository.setHttpsPort(sslPort);
        }
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isReadOnly() {
        return readOnly;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean doCache() {
        //Don't cache even when we're in readonly mode
        //        if (readOnly) {
        //            return false;
        //        }
        return doCache;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getActive() {
        return isActive;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getIsInitialized() {
        return repositoryInitialized;
    }

    /**
     * _more_
     *
     * @param v _more_
     */
    public void setActive(boolean v) {
        isActive = v;
    }


    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public void close() throws Exception {
        shutdown();
    }


    //    private boolean shutdownEnabled = false;

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getShutdownEnabled() {
        return standAloneServer != null;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isRunningStandAlone() {
        return standAloneServer != null;
    }

    /**
     * _more_
     */
    public void shutdown() {
        try {
            if ( !isActive) {
                return;
            }
            println("RAMADDA: shutting down");
            isActive = false;
            //Call this one first so it recurses if needed
            if (localRepositoryManager != null) {
                try {
                    localRepositoryManager.shutdown();
                } catch (Throwable thr) {
                    System.err.println(
				       "RAMADDA: Error shutting down local repository manager: "
				       + thr);
                }
                synchronized (repositoryManagers) {
                    repositoryManagers.remove(localRepositoryManager);
                }
            }

            List<RepositoryManager> later =
                new ArrayList<RepositoryManager>();

            synchronized (repositoryManagers) {
                for (RepositoryManager repositoryManager :
			 repositoryManagers) {
                    try {
                        if ((repositoryManager == pluginManager)
			    || (repositoryManager == entryManager)
			    || (repositoryManager == databaseManager)
			    || (repositoryManager == metadataManager)) {
                            later.add(repositoryManager);

                            continue;
                        }
                        repositoryManager.shutdown();
                    } catch (Throwable thr) {
                        System.err.println(
					   "RAMADDA: Error shutting down:"
					   + repositoryManager.getClass().getName() + " "
					   + thr);
                        thr.printStackTrace();
                    }
                }
            }

            for (RepositoryManager repositoryManager : later) {
                try {
                    repositoryManager.shutdown();
                } catch (Throwable thr) {
                    System.err.println(
				       "RAMADDA: Error shutting down plugin manager:"
				       + repositoryManager.getClass().getName());
                    thr.printStackTrace();
                }
            }

            repositoryManagers     = null;
            userManager            = null;
            monitorManager         = null;
            sessionManager         = null;
            wikiManager            = null;
            jobManager             = null;
            logManager             = null;
            entryManager           = null;
            commentManager         = null;
            associationManager     = null;
            searchManager          = null;
            mapManager             = null;
            harvesterManager       = null;
            actionManager          = null;
            accessManager          = null;
            metadataManager        = null;
            registryManager        = null;
            mailManager            = null;
            localRepositoryManager = null;
            storageManager         = null;
            apiManager             = null;
            pluginManager          = null;
            databaseManager        = null;
            ftpManager             = null;
            admin                  = null;
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }


    /**
     * _more_
     *
     * @param port _more_
     */
    public void setHttpsPort(int port) {
        super.setHttpsPort(port);
        reinitializeRequestUrls();
    }


    /**
     * _more_
     *
     *
     * @param properties _more_
     * @throws Exception _more_
     */
    public void init(Properties properties) throws Exception {
	if(Repository.debugInit)   System.err.println("Repository.init");
        //        MyTrace.startTrace();
        //This stops jython from processing jars and printing out its annoying message
        System.setProperty("python.cachedir.skip", "true");


        CacheManager.setDoCache(false);
	//	IO.debugStderr();

	if(Repository.debugInit)   System.err.println("Repository calling initProperties");
        initProperties(properties);
	if(Repository.debugInit)   System.err.println("Repository done initProperties");

	//Call this now since further initialize might trigger calls
	getLogManager().initAttributes();



        //Clear the tmp dir as it gets set by the plugin manager and any tmp dir set in a properties file will be ignored
        getStorageManager().clearTmpDir();

        initServer();

        RepositoryServlet.debugRequests =
            getProperty("ramadda.debug.requests", false);
        RepositoryServlet.debugMultiPart =
            getProperty("ramadda.debug.multipart", false);


        repositoryInitialized = true;
        //Call this here to load initial properties
        initAttributes();


        File blacklistFile = new File(getStorageManager().getRepositoryDir()
                                      + "/ipblacklist.txt");
        if (blacklistFile.exists()) {
            try {
                blacklist = new HashSet();
                for (String ip :
			 Utils.split(IOUtil.readContents(blacklistFile), "\n",
				     true, true)) {
		    //                    getLogManager().logInfoAndPrint("RAMADDA: Add blacklist ip:" + ip);
                    blacklist.add(ip);
                }
            } catch (Exception exc) {
                System.err.println("Error reading blacklist file:" + exc);
            }
        }


        //This depends on the html templates which depends on the 
        getMetadataManager().loadMetadataHandlers(getPluginManager());

        clearAllCaches();
        StringBuilder statusMsg =
            new StringBuilder("RAMADDA: repository started at:" + new Date());
        statusMsg.append("\n");
        statusMsg.append("RAMADDA: home dir: "
                         + getStorageManager().getRepositoryDir());

        statusMsg.append("  version: "
                         + RepositoryUtil.getVersion());
        statusMsg.append("  build date: "
                         + getProperty(PROP_BUILD_DATE, "N/A"));
        statusMsg.append("  java version: "
                         + getProperty(PROP_JAVA_VERSION, "N/A"));
	statusMsg.append("\n");
        statusMsg.append("RAMADDA: running on port:" + getPort() + " "
                         + (isSSLEnabled(null)
                            ? "SSL port:" + getHttpsPort()
                            : " SSL not enabled"));
        getLogManager().logInfoAndPrint(statusMsg.toString());

        if (getProperty("ramadda.beep", false)) {
            Toolkit.getDefaultToolkit().beep();
            Misc.sleep(200);
            Toolkit.getDefaultToolkit().beep();
        }

        String script = (startupScript != null)
	    ? startupScript
	    : getProperty("ramadda.startupscript");
        if (script != null) {
            try {
		List<String> commands  =Utils.split(script," ", true,true);
		ProcessBuilder pb = new ProcessBuilder(commands);
		Process     process = pb.start();
            } catch (Exception exc) {
                System.err.println("Error running startup script:" + script
                                   + "\n" + exc);
            }
        }
        Repository theRepository = this;
        //Add a listener for the kill signal so we can shutdown gracefully
        Runtime.getRuntime().addShutdownHook(new Thread() {
		public void run() {
		    System.err.println("RAMADDA shutting down");
		    theRepository.shutdown();
		}
	    });

        /*
	  try {
	  System.err.println("sending test email");
	  getMailManager().sendEmail("jeff.mcwhirter@gmail.com","test it","contents", true);
	  } catch(Exception exc) {
	  System.err.println("Error:" + exc);
	  exc.printStackTrace();
	  }
        */

        if (getProperty("ramadda.debug.stderr", false)) {
            IO.debugStderr();
        }
    }

    /**
     * Load the properties from the path
     *
     * @param props  current properties
     * @param path   path to more.
     *
     * @throws Exception  problem reading properties
     */
    public void loadProperties(Properties props, String path)
	throws Exception {
        InputStream inputStream = IOUtil.getInputStream(path, getClass());
        if (inputStream == null) {
            System.err.println("RAMADDA:  null properties: " + path);
            return;
        }
        Properties tmp = new Properties();
	tmp.load(new InputStreamReader(inputStream, Charset.forName("UTF-8")));
        //        System.out.println ("RAMADDA:  loading " + path+" " +  tmp.get("ramadda.wiki.macros"));
        //        props.load(inputStream);
        props.putAll(tmp);
        IOUtil.close(inputStream);
    }

    /**
     * _more_
     *
     *
     * @param contextProperties _more_
     * @throws Exception _more_
     */
    public void initProperties(Properties contextProperties)
	throws Exception {
        MyTrace.msg("RAMADDA: initializing properties");
        /*
          order in which we load properties files
          system
          context (e.g., from tomcat web-inf)
          cmd line args (both -Dname=value and .properties files)
          (We load the above so we can define an alternate repository dir)
          local repository directory
          (Now load in the cmd line again because they have precedence over anything else);
          cmd line
	*/

        coreProperties  = new Properties();
        localProperties = new Properties();
        loadProperties(
		       coreProperties,
		       "/org/ramadda/repository/resources/repository.properties");

        /*
	  for (Enumeration keys = coreProperties.keys(); keys.hasMoreElements(); ) {
	  String key   = (String) keys.nextElement();
	  String value = (String) coreProperties.get(key);
	  System.out.println("CORE:" +key +"=" + value);
	  }
        */



        try {
            loadProperties(
			   coreProperties,
			   "/org/ramadda/repository/resources/build.properties");
            loadProperties(
			   coreProperties,
			   "/org/ramadda/repository/resources/version.properties");	    
        } catch (Exception exc) {}


	RepositoryUtil.setVersion((String)coreProperties.get(PROP_VERSION_MAJOR),
				  (String)coreProperties.get(PROP_VERSION_MINOR),
				  (String)coreProperties.get(PROP_VERSION_PATCH));				  




        for (int i = 0; i < args.length; i++) {
            String arg = args[i];


            if (getPluginManager().checkFile(arg)) {
                continue;
            }
            if (arg.endsWith(".properties")) {
                loadProperties(cmdLineProperties, arg);
            } else if (arg.equals("-dump")) {
                dumpFile = args[i + 1];
                i++;
            } else if (arg.equals("-startup")) {
                startupScript = args[i + 1];
                i++;
            } else if (arg.equals("-load")) {
                sqlLoadFiles.add(args[i + 1]);
                i++;
            } else if (arg.equals("-admin")) {
                User user = new User(args[i + 1], true);
                user.setPassword(args[i + 2]);
                cmdLineUsers.add(user);
                i += 2;
            } else if (arg.equals("-port")) {
                //skip
                i++;
            } else if (arg.equals("-home") || arg.equals("-ramadda_home")) {
                String homeDir = args[++i];
                cmdLineProperties.put(PROP_REPOSITORY_HOME, homeDir);
            } else if (arg.startsWith("-D")) {
                String       s    = arg.substring(2);
                List<String> toks = Utils.split(s, "=", true, true);
                if (toks.size() == 0) {
                    throw new IllegalArgumentException("Bad argument:" + arg);
                } else if (toks.size() == 1) {
                    cmdLineProperties.put(toks.get(0), "");
                } else {
                    cmdLineProperties.put(toks.get(0), toks.get(1));
                }
            } else if (arg.equals("-help")) {
                //For command line use
                System.err.println(USAGE_MESSAGE);
                System.exit(0);
            } else {
                usage("Unknown argument: " + arg);
            }
        }




        //Load the context and the command line properties now 
        //so the storage manager can get to them
        if (contextProperties != null) {
            localProperties.putAll(contextProperties);
        }


        MyTrace.msg("init-2");

        //Now look around the tomcat environment                                          
        String catalinaBase = null;
        for (String arg : new String[] { "CATALINA_BASE", "catalina.base",
                                         "CATALINA_HOME", "catalina.home" }) {
            catalinaBase = getProperty(arg);
            if (catalinaBase != null) {
                break;
            }
        }
        if (catalinaBase != null) {
            File catalinaConfFile = new File(catalinaBase
                                             + "/conf/repository.properties");
            if (catalinaConfFile.exists()) {
                loadProperties(localProperties, catalinaConfFile.toString());
            }
        }

        //check for glassfish, e.g.:
        //$GLASSFISH_HOME/glassfish/config/repository.properties
        String glassfish = getProperty("GLASSFISH_HOME");
        if (glassfish != null) {
            File confFile =
                new File(glassfish
                         + "/glassfish/config/repository.properties");
            if (confFile.exists()) {
                println("RAMADDA: loading:" + confFile);
                loadProperties(localProperties, confFile.toString());
            }
        }


        //Call the storage manager so it can figure out the home dir
        getStorageManager();


        MyTrace.call1("plugin-init");
        //initialize the plugin manager with the properties
	if(Repository.debugInit)   System.err.println("Repository calling PluginManager.init");
        getPluginManager().init(pluginProperties);
	if(Repository.debugInit)   System.err.println("Repository done PluginManager.init");
        MyTrace.call2("plugin-init");

        //create the log dir
        getLogManager().getLogDir();

        MyTrace.msg("init-3");
        try {
            //Now load in the local properties file
            //First load in the repository.properties file
            String localPropertyFile =
                IOUtil.joinDir(getStorageManager().getRepositoryDir(),
                               "repository.properties");

            if (new File(localPropertyFile).exists()) {
                //                System.err.println("loading local properties:" + localPropertyFile);
                loadProperties(localProperties, localPropertyFile);
            } else {
                //                System.err.println("local properties does not exist:" + localPropertyFile);
            }

            File[] localFiles =
                getStorageManager().getRepositoryDir().listFiles();
            for (File f : localFiles) {

                if ( !f.toString().endsWith(".properties")) {
                    continue;
                }
                if (f.getName().startsWith(".")) {
                    continue;
                }
                if (f.getName().equals("repository.properties")) {
                    continue;
                }
                loadProperties(localProperties, f.toString());
            }

            File[] vfiles = getStorageManager().getVolatileDir().listFiles();
            if (vfiles != null) {
                for (File f : vfiles) {
                    if (f == null) {
                        continue;
                    }
                    if ( !f.toString().endsWith(".properties")) {
                        continue;
                    }
                    loadProperties(localProperties, f.toString());
                    System.err.println(
				       "RAMADDA: loaded and deleted volatile file:" + f);
                    f.delete();
                }
            }

        } catch (Exception exc) {
            System.out.println("error loading files:" + exc);
            exc.printStackTrace();
        }

        debug    = getProperty(PROP_DEBUG, false);
        readOnly = getProperty(PROP_READ_ONLY, false);
        doCache  = getProperty(PROP_DOCACHE, true);

        int sslPort = -1;
        String ssls = getPropertyValue(PROP_SSL_PORT,
				       (String) null, false);
        if ((ssls != null) && (ssls.trim().length() > 0)) {
            sslPort = Integer.parseInt(ssls.trim());
        }
	if(sslPort>0) setHttpsPort(sslPort);


        if (readOnly) {
            println("RAMADDA: running in readonly mode");
        }
        if ( !doCache) {
            println("RAMADDA: running with no in-memory cache");
        }
        setUrlBase(getLocalProperty(PROP_HTML_URLBASE, "/repository"));
        if (getUrlBase() == null) {
            setUrlBase(BLANK);
        }
        setIsMinified(getProperty(PROP_MINIFIED, true));

        String derbyHome = getLocalProperty(PROP_DB_DERBY_HOME,
                                            (String) null);
        if (derbyHome != null) {
            derbyHome = getStorageManager().localizePath(derbyHome);
            File dir = new File(derbyHome);
            StorageManager.makeDirRecursive(dir);
            System.setProperty("derby.system.home", derbyHome);
        }



        mimeTypes = new Properties();
        for (String path : getResourcePaths(PROP_HTML_MIMEPROPERTIES)) {
            try {
                loadProperties(mimeTypes, path);
            } catch (Exception exc) {
                //noop
            }
        }

        for (String s :
		 Utils.split(getProperty("ramadda.html.htdocroots", BLANK),
			     ";", true, true)) {
            htdocRoots.add(getStorageManager().localizePath(s));
        }
        initProxy();
        if ( !debugSession) {
            debugSession = getProperty("ramadda.debug.session", false);
        }


    }


    /**
     * _more_
     */
    private void initProxy() {
        //First try the local ramadda properties
        //The default value is the system property 
        String proxyHost = getProperty(PROP_PROXY_HOST,
                                       getProperty("http.proxyHost",
						   (String) null));
        String proxyPort = getProperty(PROP_PROXY_PORT,
                                       getProperty("http.proxyPort", "8080"));
        final String proxyUser = getProperty(PROP_PROXY_USER, (String) null);
        final String proxyPass = getProperty(PROP_PROXY_PASSWORD,
                                             (String) null);
        HttpClientBuilder builder = HttpClients.custom();
        //        httpClient = new HttpClient();
        if (proxyHost != null) {
            getLogManager().logInfoAndPrint("Setting proxy server to:"
                                            + proxyHost + ":" + proxyPort);
            System.setProperty("http.proxyHost", proxyHost);
            System.setProperty("http.proxyPort", proxyPort);
            System.setProperty("ftp.proxyHost", proxyHost);
            System.setProperty("ftp.proxyPort", proxyPort);
            builder.setProxy(new HttpHost(proxyHost,
                                          Integer.parseInt(proxyPort)));

            // Just if proxy has authentication credentials
            if (proxyUser != null) {

                /**
                 *  !!!!TODO: This was from the old library. need to implement it
                 * getLogManager().logInfoAndPrint("Setting proxy user to:" + proxyUser);
                 * //                httpClient.getParams().setAuthenticationPreemptive(true);
                 * Credentials defaultcreds =
                 *   new UsernamePasswordCredentials(proxyUser, proxyPass);
                 * httpClient.getState().setProxyCredentials(
                 *                                         new AuthScope(
                 *                                                       proxyHost, Integer.parseInt(proxyPort),
                 *                                                       AuthScope.ANY_REALM), defaultcreds);
                 * Authenticator.setDefault(new Authenticator() {
                 *       public PasswordAuthentication getPasswordAuthentication() {
                 *           return new PasswordAuthentication(proxyUser,
                 *                                             proxyPass.toCharArray());
                 *       }
                 *   });
                 */
            }

            httpClient = builder.build();


        }

    }





    /**
     * _more_
     *
     * @return _more_
     */
    public HttpClient getHttpClient() {
        return httpClient;
    }



    /**
     * _more_
     *
     * @throws Exception _more_
     */
    protected void initServer() throws Exception {
	if(Repository.debugInit)   System.err.println("Repository.initServer");
        getDatabaseManager().init();
	if(Repository.debugInit)   System.err.println("Repository.init-1");
        initDefaultTypeHandlers();
	if(Repository.debugInit)   System.err.println("Repository.init-2");
        boolean loadedRdb = false;
        boolean doDrop    = getProperty("db.load.drop", true);
        sqlLoadFiles.addAll(Utils.split(getProperty("db.load.files", ""),
                                        ";", true, true));
        for (String sqlFile : (List<String>) sqlLoadFiles) {
            if (sqlFile.endsWith(".rdb")) {
                getDatabaseManager().loadRdbFile(sqlFile, doDrop);
                loadedRdb = true;
            }
        }

	if(Repository.debugInit)   System.err.println("Repository.init-3");
        if ( !loadedRdb) {
            initSchema();
        }
	if(Repository.debugInit)   System.err.println("Repository.init-4");
        readDatabaseProperties();
        checkVersion();
        MyTrace.call1("Repository.loadResources");
	if(Repository.debugInit)   System.err.println("Repository.init-5");
        loadResources();
	if(Repository.debugInit)   System.err.println("Repository.init-6");
        MyTrace.call2("Repository.loadResources");
        getRegistryManager().checkApi();

        //Load in any other sql files from the command line
        for (String sqlFile : (List<String>) sqlLoadFiles) {
            if ( !sqlFile.endsWith(".rdb")) {
                String sql =
                    getStorageManager().readUncheckedSystemResource(sqlFile);
                getDatabaseManager().loadSql(sql, false, true);
                readDatabaseProperties();
            }
        }

	if(Repository.debugInit)   System.err.println("Repository.init-7");
        getUserManager().initUsers(cmdLineUsers);


	if(Repository.debugInit)   System.err.println("Repository:initializing top entry");
        //This finds or creates the top-level group
        getEntryManager().initTopEntry();

        setLocalFilePaths();

        if (dumpFile != null) {
            FileOutputStream fos = new FileOutputStream(dumpFile);
            getDatabaseManager().makeDatabaseCopy(fos, true, null);
            IOUtil.close(fos);
        }

        HtmlUtils.setBlockHideShowImage(getIconUrl(ICON_MINUS),
                                        getIconUrl(ICON_PLUS));
        HtmlUtils.setInlineHideShowImage(getIconUrl(ICON_MINUS),
                                         getIconUrl(ICON_PLUS));

        getLogManager().logInfo("RAMADDA started");


	if(Repository.debugInit)   System.err.println("Repository:calling StorageManager.doFinalInitialization");
        getStorageManager().doFinalInitialization();
	if(Repository.debugInit)   System.err.println("Repository:done StorageManager.doFinalInitialization");


        if (getAdmin().getInstallationComplete()) {
            getRegistryManager().doFinalInitialization();
        }
        getAdmin().doFinalInitialization();
	if(Repository.debugInit)   System.err.println("Repository:done Admin.doFinalInitialization");
        if (loadedRdb) {
            getDatabaseManager().finishRdbLoad();
        }
        getHarvesterManager().initHarvesters();
        getLogManager().initLogs();

        //Do this in a thread because (on macs) it hangs sometimes)
	//	Misc.run(this, "getFtpManager");

        //Initialize the local repositories in a thread
        Misc.run(getLocalRepositoryManager(), "initializeLocalRepositories");

        /**
         * getAdmin().checkRegistration();
         */

        if (getParentRepository() == null) {
            GeoUtils.setGoogleKey(getProperty("google.key", (String) null));
            GeoUtils.setGeocodeioKey(getProperty("geocodeio.key", (String) null));
            GeoUtils.setHereKey(getProperty("here.key", (String) null));	    
            IO.setCacheDir(getStorageManager().getRepositoryDir());
            Seesv.setTmpCacheDir(getStorageManager().getCacheDir().getDir());
        }


	//Call this so it gets instantiated
	getMonitorManager();
	//	testLocal();
	if(Repository.debugInit)   System.err.println("Repository.initServer:done");
    }


    private void testLocal() throws Exception {
	Request request = getAdminRequest();
	Entry rootEntry = getEntryManager().getRootEntry();
	System.err.println("***** TESTING ******");
	System.err.println("\tshould be Top:" +  getEntryManager().findEntryFromName(request, null, "/Top"));
	System.err.println("\tshould be Top:" +  getEntryManager().findEntryFromName(request, null, "Top"));
	System.err.println("\tshould be Top:" +  getEntryManager().findEntryFromName(request, null, ""));	
	System.err.println("\tshould be test:" +  getEntryManager().findEntryFromName(request, null, "/Top/RAMADDA/test"));
	System.err.println("\tshould be test:" +  getEntryManager().findEntryFromName(request, null, "Top/RAMADDA/test"));
	System.err.println("\tshould be test:" +  getEntryManager().findEntryFromName(request, rootEntry, "RAMADDA/test"));
	System.err.println("\tshould be test:" +  getEntryManager().findEntryFromName(request, rootEntry, "/RAMADDA/test"));
	System.err.println("\tshould be null:" +   getEntryManager().findEntryFromName(request, rootEntry, "/Top/RAMADDA/test"));
    }
    


    /**
     * _more_
     *
     * @throws Exception _more_
     */
    private void loadResources() throws Exception {
	if(Repository.debugInit)   System.err.println("Repository.loadResources-1");
        getPluginManager().loadPlugins();
	if(Repository.debugInit)   System.err.println("Repository.loadResources-2");
        getPageHandler().clearCache();
	if(Repository.debugInit)   System.err.println("Repository.loadResources-3");
        loadPluginResources();
	if(Repository.debugInit)   System.err.println("Repository.loadResources-4");
        getPluginManager().loadPluginsFinish();
	if(Repository.debugInit)   System.err.println("Repository.loadResources-5");
        initDefaultOutputHandlers();
	if(Repository.debugInit)   System.err.println("Repository.loadResources-6");	
    }


    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public void loadPluginResources() throws Exception {
        getPluginManager().loadPropertyFiles();

	//Do the licenses before we do the types
	getMetadataManager().loadLicenses();
	long t1 = System.currentTimeMillis();
        loadTypeHandlers();
	long t2 = System.currentTimeMillis();
	//System.err.println("loadTypeHandlers:" + (t2-t1));
        loadOutputHandlers();
        getApiManager().loadApi();
        getPageHandler().loadResources();
        loadSql();
        loadAdminHandlers();
    }

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public void loadTypeHandlers() throws Exception {
        List<String> badFiles = new ArrayList<String>();
        String       theFile  = null;
        try {
            for (String file : getPluginManager().getTypeDefFiles()) {
                try {
		    long t1 = System.currentTimeMillis();
                    file    = getStorageManager().localizePath(file);
                    theFile = file;
                    if (getPluginManager().haveSeen("types:" + file, false)) {
                        continue;
                    }
                    Element root = XmlUtil.getRoot(file, getClass());
                    if (root == null) {
                        continue;
                    }
                    loadTypeHandlers(root, false,file.indexOf("geodata/model")>=0);
                    getPluginManager().markSeen("types:" + file);
		    long t2 = System.currentTimeMillis();
		    //		    System.err.println("\t" + file +" time:" + (t2-t1));
                } catch (java.lang.NoClassDefFoundError ncdfe) {
                    throw new RuntimeException(ncdfe);
                } catch (Exception exc) {
                    System.err.println("CATCH:" + exc);
                    badFiles.add(file);
                }
            }

            for (String file : badFiles) {
                System.err.println("bad file:" + file);
                theFile = file;
                try {
                    Element root = XmlUtil.getRoot(file, getClass());
                    if (root == null) {
                        continue;
                    }
                    loadTypeHandlers(root, false,false);
                    getPluginManager().markSeen("types:" + file);
                } catch (Exception exc) {
                    System.err.println("RAMADDA: Error loading type handler:"
                                       + " file=" + file);
                    exc.printStackTrace();

                    throw exc;
                }
            }
        } catch (Exception exc) {
            System.err.println("Error loading typehandler from file:"
                               + theFile);

            throw exc;
        }


    }

    /**
     * _more_
     *
     * @param root _more_
     * @param overwrite _more_
     *
     *
     * @return _more_
     * @throws Exception _more_
     */
    public List<TypeHandler> loadTypeHandlers(Element root, boolean overwrite, boolean debug)
	throws Exception {
        List children = XmlUtil.findChildren(root, TypeHandler.TAG_TYPE);
        List<TypeHandler> typeHandlers = new ArrayList<TypeHandler>();
        if ((children.size() == 0)
	    && root.getTagName().equals(TypeHandler.TAG_TYPE)) {
            typeHandlers.add(loadTypeHandler(root, overwrite));
        } else {
            for (int i = 0; i < children.size(); i++) {
                Element entryNode = (Element) children.get(i);
                typeHandlers.add(loadTypeHandler(entryNode, overwrite));
		//		if(debug)   System.err.println((t2-t1) +" " + XmlUtil.toString(entryNode));
            }
        }

        return typeHandlers;
    }


    /**
     * _more_
     *
     * @param entryNode _more_
     * @param overwrite _more_
     *
     *
     * @return _more_
     * @throws Exception _more_
     */
    private TypeHandler loadTypeHandler(Element entryNode, boolean overwrite)
	throws Exception {
        String classPath = XmlUtil.getAttribute(entryNode,
						TypeHandler.ATTR_HANDLER, (String) null);

        if (classPath == null) {
            String superType = XmlUtil.getAttribute(entryNode,
						    TypeHandler.ATTR_SUPER, (String) null);
            if (superType != null) {
                TypeHandler parent = getTypeHandler(superType);
                if (parent == null) {
                    throw new IllegalArgumentException(
						       "Cannot find parent type:" + superType);
                }
                classPath = parent.getClass().getName();
                //                        System.err.println ("Using parent class:" +  classPath +" " + XmlUtil.toString(entryNode));
            } else {
                classPath = "org.ramadda.repository.type.GenericTypeHandler";
            }
        }


        Class handlerClass = Misc.findClass(classPath);
        Constructor ctor = Misc.findConstructor(handlerClass,
						new Class[] { Repository.class,
						    Element.class });
        try {
            TypeHandler typeHandler =
                (TypeHandler) ctor.newInstance(new Object[] { this,
							      entryNode });
            addTypeHandler(typeHandler.getType(), typeHandler, overwrite);
            return typeHandler;
        } catch (Exception exc) {
            System.err.println("Error creating type handler:"
                               + XmlUtil.toString(entryNode).replaceAll("\\s\\s+"," "));
            exc.printStackTrace();
            throw exc;
        }


    }


    /**
     * _more_
     *
     * @throws Exception _more_
     */
    private void loadOutputHandlers() throws Exception {
        for (String commandFile : getPluginManager().getAllFiles()) {
            if ( !commandFile.endsWith("services.xml")) {
                continue;
            }
            if (getPluginManager().haveSeen("services:" + commandFile)) {
                continue;
            }
            Element  root  = XmlUtil.getRoot(commandFile, getClass());

            NodeList nodes = XmlUtil.getElements(root, Service.TAG_SERVICE);

            for (int i = 0; i < nodes.getLength(); i++) {
                Element node = (Element) nodes.item(i);
                makeService(node, true);
            }
        }


        for (String file : getPluginManager().getOutputDefFiles()) {
            file = getStorageManager().localizePath(file);
            if (getPluginManager().haveSeen("outputhandler:" + file)) {
                continue;
            }
            Element root = XmlUtil.getRoot(file, getClass());
            if (root == null) {
                continue;
            }
            List children = XmlUtil.findChildren(root, TAG_OUTPUTHANDLER);
            for (int i = 0; i < children.size(); i++) {
                Element node = (Element) children.get(i);
                boolean required = XmlUtil.getAttribute(node, ARG_REQUIRED,
							true);
                try {
                    Class c =
                        Misc.findClass(XmlUtil.getAttributeFromTree(node,
								    ATTR_CLASS));

                    Constructor ctor = Misc.findConstructor(c,
							    new Class[] { Repository.class,
								Element.class });
                    OutputHandler outputHandler =
                        (OutputHandler) ctor.newInstance(new Object[] { this,
									node });
                    if ( !addOutputHandler(outputHandler)) {
                        System.err.println("file:" + file);
                    }

                } catch (Exception exc) {
                    System.err.println("\terror:" + exc);
                    exc.printStackTrace();
                    if ( !required) {
                        getLogManager().logWarning(
						   "Couldn't load optional output handler:"
						   + XmlUtil.toString(node));
                        getLogManager().logWarning(exc.toString());
                    } else {
                        getLogManager().logError(
						 "Error loading output handler file:" + file, exc);

                        throw exc;
                    }
                }
            }
        }
    }

    /**
     * _more_
     *
     * @param node _more_
     * @param addToGlobals _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Service makeService(Element node, boolean addToGlobals)
	throws Exception {
        Constructor ctor =
            Misc.findConstructor(
				 Misc.findClass(
						XmlUtil.getAttributeFromTree(
									     node, "handler",
									     "org.ramadda.service.Service")), new Class[] {
				     Repository.class,
				     Element.class });
        Service command = (Service) ctor.newInstance(new Object[] { this,
								    node });
        //               Service command =  new Service(this, node);
        if (addToGlobals) {
            getJobManager().addService(command);
        }

        return command;
    }


    /**
     * _more_
     *
     * @throws Exception _more_
     */
    private void loadSql() throws Exception {
        for (String sqlFile : getPluginManager().getSqlFiles()) {
            if (getPluginManager().haveSeen(sqlFile)) {
                continue;
            }
            String sql =
                getStorageManager().readUncheckedSystemResource(sqlFile);
            sql = getDatabaseManager().convertSql(sql);
            getDatabaseManager().loadSql(sql, true, false);
        }
    }

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public void loadAdminHandlers() throws Exception {
        for (Class adminHandlerClass :
		 getPluginManager().getAdminHandlerClasses()) {
            if (getPluginManager().haveSeen(adminHandlerClass)) {
                continue;
            }
            MyTrace.call1("handler:" + adminHandlerClass.getName());
            Constructor ctor = Misc.findConstructor(adminHandlerClass,
						    new Class[] { Repository.class });
            if (ctor != null) {
                getAdmin().addAdminHandler(
					   ((AdminHandler) ctor.newInstance(
									    new Object[] { Repository.this })));
            } else {
                getAdmin().addAdminHandler(
					   (AdminHandler) adminHandlerClass.getDeclaredConstructor().newInstance());
            }
            MyTrace.call2("handler:" + adminHandlerClass.getName());
        }
        //        getAdmin().addAdminHandler(new LdapAdminHandler());
    }







    /**
     * _more_
     *
     * @return _more_
     */
    public Repository getRepository() {
        return this;
    }

    /**
     * _more_
     *
     * @param repositoryManager _more_
     */
    public void addRepositoryManager(RepositoryManager repositoryManager) {
        synchronized (repositoryManagers) {
            //            System.err.println("adding repo manager:" + repositoryManager.getClass().getName());
            //Only call this if we've added one after the repo has been initialized
            if (getIsInitialized()) {
                repositoryManager.initAttributes();
            }
            repositoryManagers.add(repositoryManager);
        }
    }

    /**
     * _more_
     *
     * @param c _more_
     *
     * @return _more_
     */
    public RepositoryManager getRepositoryManager(Class c) {
        synchronized (repositoryManagers) {
            for (RepositoryManager manager : repositoryManagers) {
                if (manager.getClass().equals(c)) {
                    return manager;
                }
            }
        }

        return null;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    protected SessionManager doMakeSessionManager() {
        return new SessionManager(this);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected PageHandler doMakePageHandler() {
        return new PageHandler(this);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected DateHandler doMakeDateHandler() {
        return new DateHandler(this);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected WikiManager doMakeWikiManager() {
        return new WikiManager(this);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected EntryManager doMakeEntryManager() {
        return new EntryManager(this);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    protected CommentManager doMakeCommentManager() {
        return new CommentManager(this);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected AssociationManager doMakeAssociationManager() {
        return new AssociationManager(this);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected HarvesterManager doMakeHarvesterManager() {
        return new HarvesterManager(this);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    protected ActionManager doMakeActionManager() {
        return new ActionManager(this);
    }





    /**
     * _more_
     *
     * @return _more_
     */
    protected StorageManager doMakeStorageManager() {
        return new StorageManager(this);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    protected PluginManager doMakePluginManager() {
        return new PluginManager(this);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    protected DatabaseManager doMakeDatabaseManager() {
        return new DatabaseManager(this);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected FtpManager doMakeFtpManager() {
        return new FtpManager(this);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    protected Admin doMakeAdmin() {
        return new Admin(this);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected UserManager doMakeUserManager() {
        return new UserManager(this);
    }


    //TODO: use this to synchronize the getters below

    /** _more_ */
    private Object getMutex = new Object();

    /**
     * _more_
     *
     * @return _more_
     */
    public UserManager getUserManager() {
        if (userManager == null) {
            userManager = doMakeUserManager();
        }

        return userManager;
    }




    /**
     * _more_
     *
     * @return _more_
     */
    protected MonitorManager doMakeMonitorManager() {
        return new MonitorManager(this);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public MonitorManager getMonitorManager() {
        if (monitorManager == null) {
            monitorManager = doMakeMonitorManager();
        }

        return monitorManager;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public SessionManager getSessionManager() {
        if (sessionManager == null) {
            sessionManager = doMakeSessionManager();
            sessionManager.init();
        }

        return sessionManager;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected ApiManager doMakeApiManager() {
        return new ApiManager(this);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public ApiManager getApiManager() {
        if (apiManager == null) {
            apiManager = doMakeApiManager();
        }

        return apiManager;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public PageHandler getPageHandler() {
        if (pageHandler == null) {
            pageHandler = doMakePageHandler();
        }

        return pageHandler;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public DateHandler getDateHandler() {

        if (dateHandler == null) {
            dateHandler = doMakeDateHandler();
        }

        return dateHandler;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public WikiManager getWikiManager() {
        if (wikiManager == null) {
            wikiManager = doMakeWikiManager();
        }

        return wikiManager;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public LogManager getLogManager() {
        if (logManager == null) {
            logManager = doMakeLogManager();
            logManager.init();
        }

        return logManager;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected LogManager doMakeLogManager() {
        return new LogManager(this);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public JobManager getJobManager() {
        if (jobManager == null) {
            jobManager = doMakeJobManager();
        }

        return jobManager;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected JobManager doMakeJobManager() {
        return new JobManager(this);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public EntryManager getEntryManager() {
        if (entryManager == null) {
            entryManager = doMakeEntryManager();
        }

        return entryManager;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public ExtEditor getExtEditor() {
        if (extEditor == null) {
            extEditor = new ExtEditor(this);
        }

        return extEditor;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public synchronized EntryUtil getEntryUtil() {
        if (entryUtil == null) {
            entryUtil = EntryUtil.newEntryUtil(getRepository());
        }

        return entryUtil;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public CommentManager getCommentManager() {
        if (commentManager == null) {
            commentManager = doMakeCommentManager();
        }

        return commentManager;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public AssociationManager getAssociationManager() {
        if (associationManager == null) {
            associationManager = doMakeAssociationManager();
        }

        return associationManager;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public HarvesterManager getHarvesterManager() {
        if (harvesterManager == null) {
            harvesterManager = doMakeHarvesterManager();
        }

        return harvesterManager;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public ActionManager getActionManager() {
        if (actionManager == null) {
            actionManager = doMakeActionManager();
        }

        return actionManager;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    protected AccessManager doMakeAccessManager() {
        return new AccessManager(this);
    }




    /**
     * _more_
     *
     * @return _more_
     */
    public AccessManager getAccessManager() {
        if (accessManager == null) {
            accessManager = doMakeAccessManager();
        }

        return accessManager;
    }




    /**
     * _more_
     *
     * @return _more_
     */
    protected SearchManager doMakeSearchManager() {
        return new SearchManager(this);
    }




    /**
     * _more_
     *
     * @return _more_
     */
    public SearchManager getSearchManager() {
        if (searchManager == null) {
            searchManager = doMakeSearchManager();
        }

        return searchManager;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    protected MapManager doMakeMapManager() {
        return new MapManager(this);
    }




    /**
     * _more_
     *
     * @return _more_
     */
    public MapManager getMapManager() {
        if (mapManager == null) {
            mapManager = doMakeMapManager();
        }

        return mapManager;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    protected MetadataManager doMakeMetadataManager() {
        return new MetadataManager(this);
    }





    /**
     * _more_
     *
     * @return _more_
     */
    public MetadataManager getMetadataManager() {
        if (metadataManager == null) {
            metadataManager = doMakeMetadataManager();
        }

        return metadataManager;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    protected RegistryManager doMakeRegistryManager() {
        return new RegistryManager(this);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected MailManager doMakeMailManager() {
        return new MailManager(this);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    protected LocalRepositoryManager doMakeLocalRepositoryManager() {
        return new LocalRepositoryManager(this);
    }




    /**
     * _more_
     *
     * @return _more_
     */
    public RegistryManager getRegistryManager() {
        if (registryManager == null) {
            registryManager = doMakeRegistryManager();
        }

        return registryManager;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public MailManager getMailManager() {
        if (mailManager == null) {
            mailManager = doMakeMailManager();
        }

        return mailManager;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public LocalRepositoryManager getLocalRepositoryManager() {
        if (localRepositoryManager == null) {
            localRepositoryManager = doMakeLocalRepositoryManager();
        }

        return localRepositoryManager;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public StorageManager getStorageManager() {
        if (storageManager == null) {
            storageManager = doMakeStorageManager();
            storageManager.init();
        }

        return storageManager;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public PluginManager getPluginManager() {
        if (pluginManager == null) {
            pluginManager = doMakePluginManager();
        }

        return pluginManager;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public DatabaseManager getDatabaseManager() {
        if (databaseManager == null) {
            databaseManager = doMakeDatabaseManager();
        }

        return databaseManager;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public FtpManager getFtpManager() {
        if (ftpManager == null) {
            //Only the top-level ramaddas gets the ftpmanager
            if (globalFtpManager != null) {
                return null;
            }
            ftpManager       = doMakeFtpManager();
            globalFtpManager = ftpManager;
        }

        return ftpManager;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public Admin getAdmin() {
        if (admin == null) {
            admin = doMakeAdmin();
        }

        return admin;
    }


    /** _more_ */
    public static final double VERSION = 1.0;

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    private void updateToVersion1_0() throws Exception {}

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public void checkVersion() throws Exception {
	/*
	  double version = getDbProperty(PROP_VERSION, 0.0);
	  if (version == VERSION) {
	  return;
	  }
	  updateToVersion1_0();
	*/
        //        writeGlobal(PROP_VERSION,""+VERSION);
    }


    /**
     * _more_
     *
     * @param path _more_
     * @param c _more_
     *
     * @return _more_
     */
    public List<String> getListing(String path, Class c) {
        List<String> listing = new ArrayList<String>();
        File         f       = new File(path);
        //        getLogManager().logInfoAndPrint("RAMADDA: getListing:" + path);
        if (f.exists()) {
            File[] files = f.listFiles();
            for (int i = 0; i < files.length; i++) {
                listing.add(files[i].toString());
            }
        } else {
            //try it as a java resource
            String contents = null;
            contents = IOUtil.readContents(path + "/files.txt", c,
                                           (String) null);
            if (contents == null) {
                contents = IOUtil.readContents(path, c, (String) null);
                //                getLogManager().logInfoAndPrint("RAMADDA: resourceList (2):" + (contents == null?"NULL":contents.replaceAll("\n"," ")));
            } else {
                //                getLogManager().logInfoAndPrint("RAMADDA: resourceList (1):" + (contents == null?"NULL":contents.replaceAll("\n"," ")));
            }

            if (contents != null) {
                List<String> lines = Utils.split(contents, "\n", true, true);
                for (String file : lines) {
                    listing.add(path + "/" + file);
                }
            }
        }

        return listing;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public Date getStartTime() {
        return startTime;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List<ImportHandler> getImportHandlers() {
        return getPluginManager().getImportHandlers();
    }




    /** _more_ */
    private static final String USAGE_MESSAGE =
        "\nUsage: repository\n\t-admin <admin name> <admin password>\n\t-port <http port>\n\t-Dname=value (e.g., -Dramadda_home=/path/to/home/dir)";

    /**
     * _more_
     *
     * @param message _more_
     */
    protected void usage(String message) {
        throw new IllegalArgumentException(message + "\n" + USAGE_MESSAGE);
    }




    /**
     * _more_
     *
     * @param propertyName _more_
     *
     * @return _more_
     */
    public List<String> getResourcePaths(String propertyName) {
        List<String> tmp = Utils.split(getProperty(propertyName, BLANK), ";",
                                       true, true);
        List<String> paths = new ArrayList<String>();
        for (String path : tmp) {
            path = getStorageManager().localizePath(path);
            paths.add(path);
        }

        return paths;
    }




    /**
     * _more_
     *
     * @return _more_
     */
    public Properties readDatabaseProperties() {
	//	System.err.println("readDatabaseProperties");
	//	System.err.println(Utils.getStack(10));
        try {
            Statement statement =
                getDatabaseManager().select(Tables.GLOBALS.COLUMNS,
                                            Tables.GLOBALS.NAME,
                                            new Clause[] {});
            Properties tmp     = new Properties();
            ResultSet  results = statement.getResultSet();
            while (results.next()) {
                String name  = results.getString(1);
                String value = results.getString(2);
                if (name.equals(PROP_PROPERTIES)) {
                    tmp.load(new ByteArrayInputStream(value.getBytes()));
                }
                //Always store the value even if its PROP_PROPERTIES
                tmp.put(name, value);
            }
            getDatabaseManager().closeAndReleaseConnection(statement);

            if (dbProperties == null) {
                dbProperties = new TTLObject<Properties>(tmp, 5 * 60 * 1000,"Repository DB Properties");
            } else {
		dbProperties.put(tmp);
	    }
            return tmp;
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }



    /**
     * _more_
     */
    public void clearAllCaches() {
        synchronized (outputHandlers) {
            for (OutputHandler outputHandler :
		     new ArrayList<OutputHandler>(outputHandlers)) {
                outputHandler.clearCache();
            }
        }
        synchronized (allTypeHandlers) {
            for (TypeHandler typeHandler :
		     new ArrayList<TypeHandler>(allTypeHandlers)) {
                typeHandler.clearCache();
            }
        }
        synchronized (repositoryManagers) {
            for (RepositoryManager manager :
		     new ArrayList<RepositoryManager>(repositoryManagers)) {
                manager.clearCache();
            }
        }
        resources = new Hashtable();
        try {
            readDatabaseProperties();
        } catch (Exception exc) {
            getLogManager().logError("Error reading globals", exc);
        }

	TTLCache.clearCaches();

    }


    /**
     * _more_
     */
    public void clearCache() {
        getEntryManager().clearCache();
        getAccessManager().clearCache();
        categoryList = null;
    }


    /**
     * _more_
     *
     * @param requestUrl _more_
     */
    public void initRequestUrl(RequestUrl requestUrl) {
        try {
            synchronized (initializedUrls) {
                if ( !initializedUrls.contains(requestUrl)) {
                    initializedUrls.add(requestUrl);
                }
            }
            Request request = new Request(this, null,
                                          getUrlBase()
                                          + requestUrl.getPath());
            super.initRequestUrl(requestUrl);
            ApiMethod apiMethod = getApiManager().findApiMethod(request);
            if (apiMethod == null) {
                getLogManager().logError("Could not find api for: "
                                         + requestUrl.getPath());

                return;
            }
            if (isSSLEnabled(null) && apiMethod.getNeedsSsl()) {
                requestUrl.setNeedsSsl(true);
            }
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    /**
     * _more_
     */
    protected void reinitializeRequestUrls() {
        synchronized (initializedUrls) {
            for (RequestUrl requestUrl : initializedUrls) {
                initRequestUrl(requestUrl);
            }
        }
    }



    /**
     * _more_
     *
     * @param url _more_
     *
     * @return _more_
     */
    @Override
    public String getHttpsUrl(String url) {
        return getHttpsUrl(null, url);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param url _more_
     *
     * @return _more_
     */
    public String getHttpsUrl(Request request, String url) {
        String hostname = (request != null)
	    ? request.getServerName()
	    : getHostname();
        int    port     = getHttpsPort();
        if (port < 0) {
            return getHttpProtocol() + "://" + hostname + ":" + getPort()
		+ url;
            //            return url;
            //            throw new IllegalStateException("Do not have ssl port defined");
        }
        if (port == 0) {
            return "https://" + hostname + url;
        } else {
            return "https://" + hostname + ":" + port + url;
        }
    }


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param requestUrl _more_
     *
     * @return _more_
     */
    //    @Override
    public String getUrlPath(Request request, RequestUrl requestUrl) {
        if (requestUrl.getNeedsSsl()) {
            return httpsUrl(request, getUrlBase() + requestUrl.getPath());
        }

        return getUrlBase() + requestUrl.getPath();
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param url _more_
     *
     * @return _more_
     */
    public String httpsUrl(Request request, String url) {
        String hostname = (request != null)
	    ? request.getServerName()
	    : getHostname();
        int    port     = getHttpsPort();
        if (port < 0) {
            return getHttpProtocol() + "://" + hostname + ":"
		+ request.getServerPort() + url;
        }
        if (port == 0) {
            return "https://" + hostname + url;
        } else {
            return "https://" + hostname + ":" + port + url;
        }
    }


    /**
     * _more_
     *
     * @param type _more_
     */
    public void addOutputType(OutputType type) {
        outputTypeMap.put(type.getId(), type);
    }

    /**
     * _more_
     *
     * @param id _more_
     *
     * @return _more_
     */
    public OutputType findOutputType(String id) {
        if ((id == null) || (id.length() == 0)) {
            return OutputHandler.OUTPUT_HTML;
        }

        return outputTypeMap.get(id);
    }






    /**
     * _more_
     *
     * @throws Exception _more_
     */
    protected void initDefaultOutputHandlers() throws Exception {

        OutputHandler outputHandler = new OutputHandler(getRepository(),
							"Entry Deleter") {

		public boolean canHandleOutput(OutputType output) {
		    return output.equals(OUTPUT_DELETER)
			|| output.equals(OUTPUT_METADATA_SHORT)
			|| output.equals(OUTPUT_PUBLISH)
			|| output.equals(OUTPUT_METADATA_FULL);
		}
		public void getEntryLinks(Request request, State state,
					  List<Link> links)
                    throws Exception {
		    if ( !state.isDummyGroup()) {
			return;
		    }

		    for (Entry entry : state.getAllEntries()) {
			if (getAccessManager().canDoEdit(request, entry)) {
			    if (getEntryManager().isAnonymousUpload(entry)) {
				links.add(makeLink(request, state.getEntry(),
						   OUTPUT_PUBLISH));

				break;
			    }
			}
		    }
		    boolean deleteOk = false;
		    for (Entry entry : state.getAllEntries()) {
			if (getEntryManager().okToDelete(request, entry)) {
			    deleteOk = true;
			    break;
			}
		    }
		    if (deleteOk) {
			links.add(makeLink(request, state.getEntry(), OUTPUT_DELETER));
		    }
		}


		@Override
		public Result outputGroup(Request request, OutputType outputType,
					  Entry group, List<Entry> children)
                    throws Exception {

		    OutputType output = request.getOutput();
		    if (output.equals(OUTPUT_PUBLISH)) {
			return getEntryManager().publishEntries(request, children);
		    }


		    if (output.equals(OUTPUT_METADATA_SHORT)) {
			return getEntryManager().addInitialMetadataToEntries(
									     request, children, true);
		    }


		    if (output.equals(OUTPUT_METADATA_FULL)) {
			return getEntryManager().addInitialMetadataToEntries(
									     request, children, false);
		    }
		    request.remove(ARG_DELETE_CONFIRM);

		    return getEntryManager().processEntryListDelete(request,
								    children);
		}
	    };
        outputHandler.addType(OUTPUT_DELETER);
        outputHandler.addType(OUTPUT_METADATA_SHORT);
        outputHandler.addType(OUTPUT_METADATA_FULL);
        addOutputHandler(outputHandler);

        OutputHandler copyHandler = new OutputHandler(getRepository(),
						      "Entry Copier") {
		public boolean canHandleOutput(OutputType output) {
		    return output.equals(OUTPUT_COPY);
		}
		public void getEntryLinks(Request request, State state,
					  List<Link> links)
                    throws Exception {
		    if ((request.getUser() == null)
                        || request.getUser().getAnonymous()) {
			return;
		    }
		    if ( !state.isDummyGroup()) {
			return;
		    }
		    links.add(makeLink(request, state.getEntry(), OUTPUT_COPY));
		}

		public Result outputEntry(Request request, OutputType outputType,
					  Entry entry)
                    throws Exception {
		    if (request.getUser().getAnonymous()) {
			return new Result("", "");
		    }

		    return new Result(request.makeUrl(URL_ENTRY_COPY, ARG_FROM,
						      entry.getId()));
		}

		public String toString() {
		    return "Copy handler";
		}

		@Override
		public Result outputGroup(Request request, OutputType outputType,
					  Entry group, List<Entry> children)
                    throws Exception {
		    if (request.getUser().getAnonymous()) {
			return new Result("", "");
		    }
		    if ( !group.isDummy()) {
			return outputEntry(request, outputType, group);
		    }
		    StringBuilder idBuffer = new StringBuilder();
		    for (Entry entry : children) {
			idBuffer.append(",");
			idBuffer.append(entry.getId());
		    }
		    request.put(ARG_FROM, idBuffer);

		    return getEntryManager().processEntryCopy(request);

		    //                return new Result(request.makeUrl(URL_ENTRY_COPY, ARG_FROM,
		    //                        idBuffer.toString()));
		}
	    };
        copyHandler.addType(OUTPUT_COPY);
        addOutputHandler(copyHandler);


        OutputHandler snapshotHandler = new OutputHandler(getRepository(),
							  "Entry Snapshotter") {
		public boolean canHandleOutput(OutputType output) {
		    return output.equals(OUTPUT_MAKESNAPSHOT);
		}
		public void getEntryLinks(Request request, State state,
					  List<Link> links)
                    throws Exception {
		    if ( !request.isAdmin()) {
			return;
		    }
		    links.add(makeLink(request, state.getEntry(),
				       OUTPUT_MAKESNAPSHOT));
		}

		public String toString() {
		    return "Snapshot handler";
		}

		public Result outputEntry(Request request, OutputType outputType,
					  Entry entry)
                    throws Exception {
		    return outputSnapshot(request, entry);
		}


		@Override
		public Result outputGroup(Request request, OutputType outputType,
					  Entry group, List<Entry> children)
                    throws Exception {
		    return outputSnapshot(request, group);
		}

		public Result outputSnapshot(Request request, Entry entry)
                    throws Exception {
		    //Access is checked by the entrymanager
		    return getEntryManager().processMakeSnapshot(request, entry);
		}

	    };
        snapshotHandler.addType(OUTPUT_MAKESNAPSHOT);
        addOutputHandler(snapshotHandler);



        OutputHandler typeChangeHandler = new OutputHandler(getRepository(),
							    "Entry Type Changer") {
		public boolean canHandleOutput(OutputType output) {
		    return output.equals(OUTPUT_TYPECHANGE);
		}
		public void getEntryLinks(Request request, State state,
					  List<Link> links)
                    throws Exception {
		    if ((request.getUser() == null)
                        || request.getUser().getAnonymous()) {
			return;
		    }
		    if ( !state.isDummyGroup()) {
			return;
		    }
		    links.add(makeLink(request, state.getEntry(),
				       OUTPUT_TYPECHANGE));
		}

		public Result outputEntry(Request request, OutputType outputType,
					  Entry entry)
                    throws Exception {
		    if (request.getUser().getAnonymous()) {
			return new Result("", "");
		    }

		    return new Result(request.makeUrl(URL_ENTRY_TYPECHANGE,
						      ARG_FROM, entry.getId()));
		}

		public String toString() {
		    return "Type Change handler";
		}

		@Override
		public Result outputGroup(Request request, OutputType outputType,
					  Entry group, List<Entry> children)
                    throws Exception {
		    if (request.getUser().getAnonymous()) {
			return new Result("", "");
		    }
		    if ( !group.isDummy()) {
			return outputEntry(request, outputType, group);
		    }
		    StringBuilder idBuffer = new StringBuilder();
		    for (Entry entry : children) {
			idBuffer.append(",");
			idBuffer.append(entry.getId());
		    }
		    request.put(ARG_FROM, idBuffer);

		    return getExtEditor().processEntryTypeChange(request);
		    //                return new Result(request.makeUrl(URL_ENTRY_COPY, ARG_FROM,
		    //                        idBuffer.toString()));
		}
	    };
        typeChangeHandler.addType(OUTPUT_TYPECHANGE);
        addOutputHandler(typeChangeHandler);



        OutputHandler fileListingHandler = new OutputHandler(getRepository(),
							     "File Listing") {
		public boolean canHandleOutput(OutputType output) {
		    return output.equals(OUTPUT_FILELISTING);
		}
		public void getEntryLinks(Request request, State state,
					  List<Link> links)
                    throws Exception {
		    if (fileListingOK(request)) {
			links.add(makeLink(request, state.getEntry(),
					   OUTPUT_FILELISTING));
		    }
		}

		private boolean fileListingOK(Request request) {
		    return request.getUser().getAdmin()
			|| ( !request.getUser().getAnonymous()
			     && getProperty(PROP_ENABLE_FILE_LISTING, false));
		}

		public Result outputEntry(Request request, OutputType outputType,
					  Entry entry)
                    throws Exception {
		    return outputFileListing(request, entry,
					     (List<Entry>) Misc.newList(entry));
		}

		@Override
		public Result outputGroup(Request request, OutputType outputType,
					  Entry group, List<Entry> children)
                    throws Exception {
		    return outputFileListing(request, group, children);

		}
		public Result outputFileListing(Request request, Entry entry,
						List<Entry> entries)
                    throws Exception {

		    if ( !fileListingOK(request)) {
			throw new AccessException("File listing not enabled",
						  request);
		    }
		    StringBuilder sb = new StringBuilder();
		    getPageHandler().entrySectionOpen(request, entry, sb,
						      "File Listing");
		    boolean       didOne   = false;
		    StringBuilder forAdmin = new StringBuilder();
		    for (Entry child : entries) {
			Resource resource = child.getResource();
			if (resource == null) {
			    continue;
			}
			if ( !resource.isFile()) {
			    continue;
			}
			forAdmin.append(resource.getTheFile().toString());
			forAdmin.append(HtmlUtils.br());

			sb.append(
				  child.getTypeHandler().getEntryResourceHref(
									      request, child));
			sb.append(
				  formatFileLength(
						   child.getResource().getFileSize(), true));
			sb.append(HtmlUtils.br());
			didOne = true;
		    }
		    if (request.getUser().getAdmin()) {
			sb.append("<hr>");
			sb.append(getPageHandler().msgHeader("File Paths"));
			sb.append(forAdmin);
		    }
		    if ( !didOne) {
			sb.append(
				  getPageHandler().showDialogNote(
								  "No files available"));
		    }

		    getPageHandler().entrySectionClose(request, entry, sb);

		    return makeLinksResult(request, msg("File Listing"), sb,
					   new State(entry));
		}

		public String toString() {
		    return "File listing handler";
		}

	    };
        fileListingHandler.addType(OUTPUT_FILELISTING);
        addOutputHandler(fileListingHandler);

        getUserManager().initOutputHandlers();

    }







    /**
     * _more_
     *
     * @param entryMonitor _more_
     */
    public void addEntryChecker(EntryChecker entryMonitor) {
        entryMonitors.add(entryMonitor);
    }


    public Result processXss(Request request) throws Exception {
	StringBuilder sb = new StringBuilder();
	sb.append("<div style='margin-left:20px;'>\n");
	int cnt = 1;
	for(String line: Utils.split(IOUtil.readContents("/org/ramadda/repository/resources//xss-payload-list.txt",getClass()),"\n",true,true)) {
	    if(line.equals("quit")) break;
	    if(line.startsWith("#")) continue;
	    line  =Request.cleanXSS(line);
	    //	    String pattern = StringUtil.findPattern(line,"\\s+(on[^\\s]+)");
	    //	    if(pattern!=null) System.out.println(pattern);
	    cnt++;
	    line = line.replace("_CNT_","" + (cnt));
	    System.out.println("#" + cnt+line);
	    sb.append(line);
	    sb.append("<br>\n");
	}
	sb.append("</div>");

	return new Result("",sb);
    }


    /**
     * _more_
     *
     * @param request The request
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result handleRequest(Request request) throws Exception {
	//Add in any default http headers
	for(String[]pair: httpHeaders) {
	    request.setHeader(pair[0],pair[1]);
	}	    


        if ( !getActive()) {
            Result result =
                new Result(msg("Error"),
                           new StringBuilder("Repository not active"));
            result.setResponseCode(Result.RESPONSE_NOTFOUND);
            return result;
        }

        long t1 = System.currentTimeMillis();
        propcnt = 0;
	//	propdebug = true;
        Result result = handleRequestInner(request);





        long   t2     = System.currentTimeMillis();
	String path       = request.getRequestPath();
	//	if(path.indexOf("/entry/show")>=0) System.err.println((t2-t1) +" " +path +" " + propcnt);
	propdebug = false;
        return result;
    }

    /** _more_ */
    public static boolean propdebug = false;

    /** _more_ */
    public int propcnt = 0;

    /**
     * _more_
     *
     * @return _more_
     */
    private boolean acceptRobots() {
        return acceptRobots;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getCommentsEnabled() {
	return commentsEnabled;
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     */
    public Result getNoRobotsResult(Request request) {
        Result result = new Result("", new StringBuilder("no bots"));
        result.setResponseCode(Result.RESPONSE_UNAUTHORIZED);
        return result;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param zipFileName _more_
     * @param files _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result zipFiles(Request request, String zipFileName,
                           List<File> files)
	throws Exception {
        request.setReturnFilename(zipFileName);
        Result result = new Result();
        result.setNeedToWrite(false);
        OutputStream os = request.getHttpServletResponse().getOutputStream();
        request.getHttpServletResponse().setContentType("application/zip");
        ZipOutputStream zos = new ZipOutputStream(os);
        for (File f : files) {
            zos.putNextEntry(new ZipEntry(f.getName()));
            InputStream fis = getStorageManager().getFileInputStream(f);
            IOUtil.writeTo(fis, zos);
            zos.closeEntry();
            IOUtil.close(fis);
        }
        IOUtil.close(zos);

        return result;
    }



    /**
     * _more_
     *
     * @param request The request
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Result handleRequestInner(Request request) throws Exception {

        if (debugSession) {
            debugSession(request,
                         "RAMADDA.handleRequest:" + request.getRequestPath());
        }
        if (request.getIsRobot()) {
            if ( !acceptRobots()) {
                return getNoRobotsResult(request);
            }
            //Sleep a second to slow the google bot down
            if (request.getUserAgent().indexOf("www.majestic12.co.uk") >= 0) {
                System.err.println("Sleeping for the bad bot:" + request
                                   + " " + request.getUserAgent());
                Misc.sleepSeconds(60);
            } else {
                //Slow other bots down
                Misc.sleepSeconds(1);
            }
        }

        if (blacklist != null) {
            String ip = request.getIpRaw();
            if ((ip != null) && blacklist.contains(ip)) {
                return makeBlockedResult(request);
            }
        }

        //A hack  - should put this in a user-agent blacklist file sometime
        String userAgent = request.getUserAgent();
        if (userAgent != null) {
            if ((userAgent.indexOf("OpenVAS") >= 0)
		|| (userAgent.indexOf("GBN") >= 0)) {
                return makeBlockedResult(request);
            }
        }

        String requestPath = request.getRequestPath();
        //Check for scanners
        if (requestPath.endsWith(".php")) {
            return makeBlockedResult(request);
        }


        boolean debugMemory = false;
	/*
	  String theUrl = request.toString();
	  debugMemory =         !theUrl.matches(".*(images|icons|htdocs|/metadata/view).*")
	  && !theUrl.matches(".*(\\.js|\\.png|\\.gif|favicon.ico)$");
	*/

        if (debugMemory) {
            Runtime.getRuntime().gc();
        }
        double mem1 = Utils.getUsedMemory();
        if (debug) {
            getLogManager().debug("user:" + request.getUser() + " -- "
                                  + request.toString());
        }

        //        logInfo("request:" + request);
        Result  result         = null;
        try {
            try {
                getSessionManager().checkSession(request);
            } catch (Throwable sessionExc) {
                request.setUser(getUserManager().getAnonymousUser());
                result = makeErrorResult(request, sessionExc);
                result.setResponseCode(Result.RESPONSE_UNAUTHORIZED);
            }
            if (result == null) {
                result = getResult(request);
            }
        } catch (Throwable exc) {  //getResult error
            //In case the session checking didn't set the user
            if (request.getUser() == null) {
                request.setUser(getUserManager().getAnonymousUser());
            }

            Throwable     inner     = LogUtil.getInnerException(exc);
            boolean       badAccess = inner instanceof AccessException;
            StringBuilder sb        = new StringBuilder();
	    boolean responseAsData = request.responseAsData();

            if ( !badAccess || responseAsData) {
                inner.printStackTrace();
                sb.append(makeErrorResponse(request, inner.getMessage()));
            } else {               //bad access
                AccessException     ae         = (AccessException) inner;
                AuthorizationMethod authMethod =
                    AuthorizationMethod.AUTH_HTML;
                if (request.getApiMethod() != null) {
                    ApiMethod apiMethod = request.getApiMethod();
                    if (apiMethod.getCheckAuthMethod()) {
                        request.setCheckingAuthMethod(true);
                        Result authResult =
                            (Result) apiMethod.invoke(request);
                        authMethod = authResult.getAuthorizationMethod();
                    } else {
                        authMethod = AuthorizationMethod.getMethod(
								   apiMethod.getAuthMethod());
                    }
                }
                //              System.err.println ("auth:" + authMethod);
                if (authMethod.equals(AuthorizationMethod.AUTH_HTML)) {
                    sb.append(
			      getPageHandler().showDialogError(inner.getMessage()));
                    sb.append(getUserManager().makeLoginForm(request,
							     HtmlUtils.hiddenBase64(ARG_REDIRECT, request.getUrl())));

                } else {  //auth isnt html
                    sb.append(inner.getMessage());
                    //If the authmethod is basic http auth then, if ssl is enabled, we 
                    //want to have the authentication go over ssl. Else we do it clear text
                    if ( !request.getSecure() && isSSLEnabled(null)) {
                        /*
			  If ssl then we are a little tricky here. We redirect the request to the generic ssl based SSLREDIRCT url
			  passing the actual request as an argument. The processSslRedirect method checks for authentication. If
			  not authenticated then it throws an access exception which triggers a auth request back to the client
			  If authenticated then it redirects the client back to the original non ssl request
                        */
			String redirectUrl =
			    Utils.encodeBase64(request.getUrl());
                        String url =
                            HtmlUtils.url(URL_SSLREDIRECT.toString(),
                                          ARG_REDIRECT, redirectUrl);
                        result = new Result(url);
                    } else {
                        result = new Result("Error", sb);
			result.addHttpHeader(HtmlUtils.HTTP_WWW_AUTHENTICATE, "Basic realm=\"ramadda\"");
                        result.setResponseCode(Result.RESPONSE_UNAUTHORIZED);
                    }

                    return result;
                }
            }


            if (!request.responseAsJson() && (request.getUser() != null) && request.getUser().getAdmin()) {
                sb.append(
			  HtmlUtils.pre(
					HtmlUtils.entityEncode(
							       LogUtil.getStackTrace(inner))));
            }

            result = new Result(msg("Error"), sb);

            if (badAccess) {
                result.setResponseCode(Result.RESPONSE_UNAUTHORIZED);
                //                result.addHttpHeader(HtmlUtils.HTTP_WWW_AUTHENTICATE,"Basic realm=\"repository\"");
            } else {
                result.setResponseCode(Result.RESPONSE_INTERNALERROR);
                getLogManager().logError("Error handling request:" + request
                                         + " ip:" + request.getIp(), inner);
            }

            if (responseAsData) {
                result.setShouldDecorate(false);
            }
	}

        if (debugMemory) {
            Runtime.getRuntime().gc();
            double mem2 = Utils.getUsedMemory();
            System.err.println("memory:" + mem2 + " delta:"
                               + Utils.decimals(mem2 - mem1, 1) + " url:"
                               + request);
        }

        getLogManager().logRequest(request, (result == null)
				   ? Result.RESPONSE_INTERNALERROR
				   : result.getResponseCode());

        boolean okToAddCookie = false;


        if (result != null) {
	    if ((result.getInputStream() == null) && result.isHtml()
		&& result.getShouldDecorate()
		&& result.getNeedToWrite()) {
		getPageHandler().decorateResult(request, result);
	    }
	    if(result.getOkToAddCookies()) {
		okToAddCookie = result.getResponseCode() == Result.RESPONSE_OK;
	    }
	    if (result.getRedirectUrl() != null) {
		okToAddCookie = true;
	    } 
	}


        if (request.getSessionHasBeenHandled()) {
            okToAddCookie = false;
        }

        if (okToAddCookie && (result != null)
	    && (request.getSessionIdWasSet()
		|| (request.getSessionId() == null))) {
	    handleRequestCookie(request, result);
        }

        return result;

    }


    public void handleRequestCookie(Request request, Result result) {
	//	if(result.getCookieWasAdded()) return;
	if(request.getCookieWasAdded()) return;	
	if (request.getSessionId() == null) {
	    request.setSessionId(getSessionManager().createSessionId());
	}
	String sessionId = request.getSessionId();
	if (cookieExpirationDate == null) {
	    //expire the cookie in 4 years year
	    //Assume this ramadda doesn't run continuously for more than 4 years
	    Date future = new Date(new Date().getTime()
				   + DateUtil.daysToMillis(365 * 4));
	    SimpleDateFormat sdf =
		new SimpleDateFormat("EEE, dd-MMM-yyyy");
	    cookieExpirationDate = sdf.format(future);
	}
	//            System.err.println (getUrlBase() +" setting cookie:" + sessionId);
	if (debugSession) {
	    //                debugSession(request,
	    //                             "Cookie:"+ getSessionManager().getSessionCookieName()+ "=" + sessionId + " path=" + getUrlBase());
	}
	String path;

	if (getShutdownEnabled() && (getParentRepository() == null)) {
	    path = "/";
	} else {
	    path = getUrlBase();
	}

	request.addCookie(getSessionManager().getSessionCookieName(),
			  sessionId + "; path=" + path + "; expires="
			  + cookieExpirationDate + " 23:59:59 GMT"
			  + (isSSLEnabled(request)
			     ? "; secure"
			     : "") + "; HttpOnly;SameSite=Strict");

    }


    /**
     * _more_
     *
     * @param request _more_
     * @param exc _more_
     * @param message _more_
     *
     * @return _more_
     */
    public String handleError(Request request, Throwable exc,
                              String message) {
        getLogManager().logError("Error:" + exc.getMessage(), exc);
        Throwable     inner = LogUtil.getInnerException(exc);
        StringBuilder sb    = new StringBuilder();
        sb.append(getPageHandler().showDialogError(message + "<br>"
						   + inner.getMessage()));
        if ((request.getUser() != null) && request.getUser().getAdmin()) {
            String stack = HtmlUtils.pre(
					 HtmlUtils.entityEncode(
								LogUtil.getStackTrace(inner)));
            sb.append(HtmlUtils.makeShowHideBlock("Stack", stack, false));
        }

        return sb.toString();
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     */
    public Result makeBlockedResult(Request request) {
        getLogManager().logRequest(request, Result.RESPONSE_BLOCKED);
        Misc.sleepSeconds(20);
        Result r = new Result("", new StringBuilder());
        r.setResponseCode(Result.RESPONSE_NOTFOUND);

        return r;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param exc _more_
     *
     * @return _more_
     */
    public Result makeErrorResult(Request request, Throwable exc) {
        Throwable inner = LogUtil.getInnerException(exc);
        String    msg;
        if (inner instanceof RepositoryUtil.MissingEntryException) {
            msg = translate(request, msgLabel("Entry not found"))
		+ inner.getMessage();
        } else {
            msg = "Error:" + inner.getMessage();
        }

        return makeErrorResult(request, msg);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param msg _more_
     *
     * @return _more_
     */
    public Result makeErrorResult(Request request, String msg) {
	return makeErrorResult(request, msg, true);
    }
    public Result makeErrorResult(Request request, String msg,boolean decorate) {	
        StringBuilder sb = new StringBuilder(decorate?makeErrorResponse(request, msg):msg);
        Result        result = null;
        if (request.responseAsJson()) {
            result = new Result("", sb, JsonUtil.MIMETYPE);
            result.setShouldDecorate(false);
        } else if (request.responseAsXml()) {
            result = new Result("", sb, MIME_XML);
            result.setShouldDecorate(false);
        } else if (request.responseAsText()) {
            result = new Result("", sb, MIME_TEXT);
            result.setShouldDecorate(false);
        } else {
            result = new Result(msg("Error"), sb);
        }
	result.setResponseCode(Result.RESPONSE_INTERNALERROR);
        return result;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param msg _more_
     *
     * @return _more_
     */
    public Result makeOkResult(Request request, String msg) {
        StringBuilder sb = new StringBuilder(makeOkResponse(request, msg));

        return makeResponseResult(request, sb);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     *
     * @return _more_
     */
    public Result makeResponseResult(Request request, StringBuilder sb) {
        Result result = null;
        if (request.responseAsJson()) {
            result = new Result("", sb, JsonUtil.MIMETYPE);
            result.setShouldDecorate(false);
        } else if (request.responseAsXml()) {
            result = new Result("", sb, MIME_XML);
            result.setShouldDecorate(false);
        } else if (request.responseAsText()) {
            result = new Result("", sb, MIME_TEXT);
            result.setShouldDecorate(false);
        } else {
            result = new Result(msg("Error"), sb);
        }

        return result;

    }

    /**
     * _more_
     *
     * @param request _more_
     * @param msg _more_
     *
     * @return _more_
     */
    public String makeErrorResponse(Request request, String msg) {
        msg = translate(request, msg);
        if (request.responseAsJson()) {
            return JsonUtil.mapAndQuote(Utils.makeList("error", msg));
        } else if (request.responseAsXml()) {
            return XmlUtil.tag(TAG_RESPONSE,
                               XmlUtil.attr(ATTR_CODE, CODE_ERROR), msg);
        } else if (request.responseAsText()) {
            return msg;
        } else {
            msg = HtmlUtils.sanitizeString(msg);
            StringBuilder sb = new StringBuilder();
            sb.append(
		      getPageHandler().showDialogError(
						       getPageHandler().translate(
										  request, "An error has occurred") + ":"
						       + HtmlUtils.p() + msg));

            return sb.toString();
        }
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param msg _more_
     *
     * @return _more_
     */
    public String makeOkResponse(Request request, String msg) {
        msg = translate(request, msg);
        if (request.responseAsJson()) {
            return JsonUtil.mapAndQuote(Utils.makeList("ok", msg));
        } else if (request.responseAsXml()) {
            return XmlUtil.tag(TAG_RESPONSE,
                               XmlUtil.attr(ATTR_CODE, CODE_OK), msg);
        } else if (request.responseAsText()) {
            return msg;
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(
		      getPageHandler().showDialogNote(
						      getPageHandler().translate(
										 request, "An error has occurred") + ":"
						      + HtmlUtils.p() + msg));

            return sb.toString();
        }
    }




    /**
     * _more_
     *
     * @return _more_
     */
    public String debugPrefix() {
        return getUrlBase() + ": ";
    }

    /**
     * _more_
     *
     *
     * @param request _more_
     * @param msg _more_
     */
    public void debugSession(Request request, String msg) {
        if (debugSession) {
            /*
	      if ((request != null)
	      && !request.getRequestPath().equals(
	      "/repository/entry/show")) {
	      return;
	      }*/
            //            System.err.println(debugPrefix() + msg);
            System.err.println(msg);
        }
    }

    int dodsCnt=0;

    /**
     * _more_
     *
     * @param request The request
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Result getResult(Request request) throws Exception {
        ApiMethod apiMethod = getApiManager().findApiMethod(request);
        if (apiMethod == null) {
	    long t1 = System.currentTimeMillis();
	    Result result =  getHtdocsFile(request);
	    if(result==null) {
		return make404(request);
	    }
	    //Don't do this for now
	    //if(result!=null) result.setOkToAddCookies(false);
	    long t2 = System.currentTimeMillis();
	    //	    String path       = request.getRequestPath();
	    //	    System.err.println("time: " + (t2-t1) +"ms  " +path);
	    return result;
        }


        Result sslRedirect = checkForSslRedirect(request, apiMethod);
        if (sslRedirect != null) {
            debugSession(request, "redirecting to ssl:" + request.getUrl());
            return sslRedirect;
        }


        request.setApiMethod(apiMethod);
        apiMethod.incrNumberOfCalls();

        if ( !getAdmin().getInstallationComplete()) {
	    //Check for the favicon.ico request
	    String path       = request.getRequestPath();
	    if(path.equals("/favicon.ico")) return  processFavIcon(request);
            return getAdmin().doInstall(request);
        }

        if ( !getUserManager().isRequestOk(request)) {
            throw new AccessException(
				      msg("You do not have permission to access this page"),
				      request);
        }

        if ( !apiMethod.isRequestOk(request, this)) {
            throw new AccessException(msg("Incorrect access"), request);
        }
        Result result = null;
        try {
	    /*
	      String path = request.toString();
	      boolean isOpendap = 	   path.indexOf("entry.das")>=0;
	      long t1 = System.currentTimeMillis();
	      if(path.indexOf("entry.das")>=0) {
	      javax.servlet.http.HttpServletRequest r = request.getHttpServletRequest();
	      dodsCnt++;
	      String u = path;
	      String q = r.getQueryString();
	      if(q!=null) u= u +"?"+HU.urlDecode(q);
	      System.err.println("#"+dodsCnt +" url:" + u);
	      }
	    */
	    result = (Result) apiMethod.invoke(request);
	    /*
	      if(isOpendap) {
	      long t2 = System.currentTimeMillis();
	      Utils.printTimes("Repository.opendap",t1,t2);
	      }
	    */
        } catch (Exception exc) {
            Throwable inner = LogUtil.getInnerException(exc);
            if (inner instanceof RepositoryUtil.MissingEntryException) {
                result = makeErrorResult(request, inner);
                result.setResponseCode(Result.RESPONSE_NOTFOUND);
            } else {
                throw exc;
            }
        }

        getLogManager().writeTestLog(request);


        return result;
    }



    /**
     *  Convert the sessionId into a authorization token that is used to verify form
     *  submissions, etc.
     *
     * @param sessionId _more_
     *
     * @return _more_
     */
    public String getAuthToken(String sessionId) {
        if (sessionId == null) {
            return "";
        }

        return RepositoryUtil.hashString(sessionId);
    }

    /**
     * _more_
     *
     * @param request The request
     * @param sb _more_
     */
    public void addAuthToken(Request request, Appendable sb) {
        try {
            String sessionId = request.getSessionId();
            if (sessionId != null) {
                String authToken = getAuthToken(sessionId);
                sb.append(HtmlUtils.hidden(ARG_AUTHTOKEN, authToken));
            }
        } catch (java.io.IOException ioe) {
            throw new RuntimeException(ioe);
        }

    }

    /**
     * _more_
     *
     * @param request _more_
     */
    public void addAuthToken(Request request) {
        String sessionId = request.getSessionId();
        if (sessionId != null) {
            String authToken = getAuthToken(sessionId);
            request.put(ARG_AUTHTOKEN, authToken);
        }
    }



    /**
     * _more_
     *
     * @param request The request
     * @param apiMethod _more_
     *
     * @return _more_
     */
    private Result checkForSslRedirect(Request request, ApiMethod apiMethod) {
        boolean debug      = false;

        boolean sslEnabled = isSSLEnabled(request);

        //check for the sub-repositories
        if (apiMethod.getRequest().startsWith("/repos/")) {
            return null;
        }
        if (debug) {
            System.err.println("checkForSslRedirect allSsl:" + allSsl
                               + " request secure:" + request.getSecure());
        }
        if (sslEnabled) {
            if (allSsl && !request.getSecure()) {
                if (debug) {
                    System.err.println("\tredirecting 1");
                }

                return new Result(httpsUrl(request, request.getUrl()));
            }
        }

        if (sslEnabled) {
            if ( !request.get(ARG_NOREDIRECT, false)) {
                if (apiMethod.getNeedsSsl() && !request.getSecure()) {
                    //redirect them to the https request
                    if (debug) {
                        System.err.println("\tredirecting 2");
                    }

                    return new Result(httpsUrl(request, request.getUrl()));

                } else if ( !allSsl && !apiMethod.getNeedsSsl()
                            && request.getSecure()) {
                    /*
                    //we used to redirect the https: request to entry points that don't require https
                    //back to the http request
                    String url = request.getUrl();
                    String redirectUrl;
                    int    port = getPort();
                    if (port == 80) {
		    redirectUrl = getHttpProtocol() + "://"
		    + request.getServerName() + url;
                    } else {
		    redirectUrl = getHttpProtocol() + "://"
		    + request.getServerName() + ":" + port
		    + url;
                    }
                    return new Result(redirectUrl);
                    */
                    return null;
                }
            }
        }

        return null;
    }



    /**
     * _more_
     *
     * @param path _more_
     * @param bytes _more_
     * @param force _more_
     */
    private void putHtdocsCache(String path, byte[] bytes, boolean force) {
	if (!cacheHtdocs) return;
        synchronized (htdocsCache) {
            if ( !force && (htdocsCacheSize > HTDOCS_CACHE_LIMIT)) {
		htdocsCache =  new Hashtable<String,  byte[]>();
		htdocsCacheSize =0;
		System.err.println("clearing htdocs cache");
            }
	    htdocsCacheSize += bytes.length;
	    htdocsCache.put(path, bytes);
        }
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param path _more_
     * @param inputStream _more_
     * @param mimeType _more_
     * @param cacheOk _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result makeResult(Request request, String path,
                             InputStream inputStream, String mimeType,
                             boolean cacheOk)
	throws Exception {
        return makeResult(request, path, inputStream, mimeType, cacheOk,
                          false);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param path _more_
     * @param inputStream _more_
     * @param mimeType _more_
     * @param cacheOk _more_
     * @param gzipIt _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result makeResult(Request request, String path,
                             InputStream inputStream, String mimeType,
                             boolean cacheOk, boolean gzipIt)
	throws Exception {
        String tail = IOUtil.getFileTail(path);
        //        boolean acceptGzip = request.canAcceptGzip();
        //        acceptGzip =  false;
        //        if(acceptGzip) {
        //            OutputStream outputStream = new ByteArrayOutputStream();
        //            inputStream = new GZIPInputStream(inputStream);
        //            new GZIPOutputStream(inputStream);
        //        }
        Result result = new Result(tail, inputStream, mimeType);
        //        if(acceptGzip) {
        //            result.addHttpHeader("Content-Encoding","gzip");
        //        }
        if (tail.length() > 0) {
            result.setReturnFilename(tail);
        }
        result.setCacheOk(cacheOk);

        return result;
    }


    /**
     * _more_
     *
     * @param request The request
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Result getHtdocsFile(Request request) throws Exception {
	boolean debug = false;
        request.setCORSHeaderOnResponse();
        String path       = request.getRequestPath();
	if(path.equals("/favicon.ico")) return  processFavIcon(request);
	if(debug)
	    System.err.println("\n* path 1:" + path);
	//Right off the bat exclude any path with .. as, if it is a non-hacker request from a browser then
	//there should never be a relative path element
        path       = path.replaceAll("//+", "/").replaceAll("/+$","");
	//System.err.println(path);
	if(path.indexOf("..")>=0) {
	    if(debug) System.err.println("\t404:" + path);
	    return make404(request);
	}
	if(path.endsWith("/")) {
	    if(debug) System.err.println("\t404:" + path);
	    return make404(request);
	}
	//This should never happen
	if(path.equals("")) {
	    if(debug) System.err.println("\t404:" + path);
	    return make404(request);
	}

	if(debug)
	    System.err.println("\tpath clean:" + path);
        String urlBase    = getUrlBase();
	if (path.startsWith(urlBase)) {
            int length = urlBase.length();
            path = path.substring(length);
        }


        if (path.startsWith("/htdocs_v")) {
            path = path.substring(9);
            int index = path.indexOf("/");
            if (index >= 0) {
                path = path.substring(index);
            }
        }

        String mimeType =
            getMimeTypeFromSuffix(IOUtil.getFileExtension(path));
        if (path.endsWith("asm.data")) {
            mimeType = "application/octet-stream";
        }

        boolean decorate = true;
        if (path.startsWith("/raw")) {
            path     = path.substring("/raw".length());
            decorate = false;
        }

        byte[] bytes = htdocsCache.get(path);
        if (bytes != null) {
            InputStream inputStream = new ByteArrayInputStream(bytes);
            return makeResult(request, path, inputStream, mimeType, true);
        }
        String cachePath = htdocsPathCache.get(path);
        //Go through all of the htdoc roots
        for (String root : htdocRoots) {
            String fullPath = null;
            if (cachePath != null) {
                fullPath  = cachePath;
                cachePath = null;
            } else {
                fullPath = root + path;
            }
	    if(debug)
		System.err.println("\ttrying path:" + fullPath);
            try {
                InputStream inputStream =
                    getStorageManager().getInputStream(fullPath);
		if(debug)
		    System.err.println("\tgot it:" + fullPath);
                htdocsPathCache.put(path, fullPath);
                if (path.endsWith(".js") || path.endsWith(".css")) {
                    String js = IOUtil.readInputStream(inputStream);
		    bytes = js.getBytes();
		    putHtdocsCache(path, bytes, false);
                    inputStream = new ByteArrayInputStream(bytes);
                } else if (path.endsWith(".png") || path.endsWith(".gif")
                           || path.endsWith(".jpg")
                           || path.endsWith(".jpeg")) {
                    bytes = IOUtil.readBytes(inputStream);
                    putHtdocsCache(path, bytes, false);
                    inputStream = new ByteArrayInputStream(bytes);
                } else if (path.endsWith(".html")) {
                    //If its just sitting on the server then don't decorate
                    if (new File(fullPath).exists()) {
                        decorate = false;
                    }
		    return processHtmlPage(request, inputStream, decorate);
                }
                return makeResult(request, path, inputStream, mimeType, true);
            } catch (IOException fnfe) {
                //The first time through there are lots of filenotfound exeptions but then they get cached and we're good
            }
        }

        String pluginPath = getPluginManager().getHtdocsMap().get(path);
        if (pluginPath != null) {
	    if(debug)
		System.err.println("\tpluginPath:" + pluginPath);
            //We can go directly here instead of thru the storagemanager which checks the white list
            InputStream inputStream = IOUtil.getInputStream(pluginPath,
							    getClass());

            if (pluginPath.endsWith(".js") || pluginPath.endsWith(".css")
		|| pluginPath.endsWith(".json")) {
                String js = IOUtil.readInputStream(inputStream);
                js = js.replace("${urlroot}", urlBase);
                js = js.replace("${htdocs}", getPageHandler().makeHtdocsUrl("")).replace("${root}",
											 urlBase);
                js    = js.replace("${hostname}", request.getServerName());
                bytes = js.getBytes();
                putHtdocsCache(path, bytes, path.endsWith(RESOURCE_ALLCSS));
                inputStream = new ByteArrayInputStream(bytes);
            } else if (path.endsWith(".png") || path.endsWith(".gif")
                       || path.endsWith(".jpg") || path.endsWith(".jpeg")) {
                bytes = IOUtil.readBytes(inputStream);
                putHtdocsCache(path, bytes, false);
                inputStream = new ByteArrayInputStream(bytes);
            } else if (path.endsWith(".html")) {
		return processHtmlPage(request, inputStream, true);
            }

            return makeResult(request, path, inputStream, mimeType, true);
        }


        String  alias                = null;
        boolean tryingOnePathAsAlias = false;
        if (path.startsWith("/alias/")) {
            alias = path.substring("/alias/".length());
        } else if (path.startsWith("/aka/")) {
            alias = path.substring("/aka/".length());
        } else if (path.startsWith("/a/")) {
            alias = path.substring("/a/".length());
        } else {
	    if (path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }
            if (path.length() > 0) {
                String tmp = path.substring(1);
                if (tmp.indexOf("/") < 0) {
                    alias                = tmp;
                    tryingOnePathAsAlias = true;
                }
            }
        }


        if (Utils.stringDefined(alias)) {
            if (alias.endsWith("/")) {
                alias = alias.substring(0, alias.length() - 1);
            }
            if (Utils.stringDefined(alias)) {
                String       childPath = null;
                List<String> toks      = Utils.splitUpTo(alias, "/", 2);
                if (toks.size() > 0) {
		    alias = toks.get(0);
		    List<Entry> entries =  getEntryManager().getEntriesFromAlias(request,
										 alias);
		    if(entries.size()>1) {
			StringBuilder sb = new StringBuilder();
			getPageHandler().sectionOpen(request, sb, "Entries", false);
			sb.append(getPageHandler().showDialogNote("Multiple entries have the alias: " +alias));
			sb.append("<ul>");
			for (Entry entry : entries) {
			    sb.append("<li> ");
			    sb.append(getPageHandler().getBreadCrumbs(request,entry));
			    sb.append(HU.br());
			}
			sb.append("</ul>");
			getPageHandler().sectionClose(request, sb);
			return new Result("", sb);
		    }
                    Entry entry =(entries.size()>0?entries.get(0):null);
                    if ((toks.size() == 2) && (entry != null)) {
			//we have a child entry path under the alias
			System.err.println("Alias:"+alias +" rest:" + toks.get(1));
                        entry = getEntryManager().findEntryFromName(request, entry, toks.get(1));
                    }
                    if (entry == null) {
                        if ( !tryingOnePathAsAlias) {
                            Result result =
                                getRepository().makeErrorResult(request,
								"Could not find aliased entry:"
								+ HtmlUtils.sanitizeString(alias));
                            result.setResponseCode(Result.RESPONSE_NOTFOUND);

                            return result;
                        } else {
			    return make404(request);
                        }
                    }
                    request.put(ARG_ENTRYID, entry.getId());
                }
            }


            //For now, don't redirect
            return getEntryManager().processEntryShow(request);
            //            return new Result(request.makeUrl(URL_ENTRY_SHOW, ARG_ENTRYID,
            //                                          entry.getId()));
        }

	if(debug)
	    System.err.println("\t404");
	return make404(request);
    }


    private Result processHtmlPage(Request request, InputStream inputStream, boolean decorate) throws Exception {
	String html = IOUtil.readInputStream(inputStream);
	html = getPageHandler().applyBaseMacros(html);
	html = html.replace("${version}",
			    getProperty(PROP_BUILD_VERSION,
					"1.0"));
	html = html.replace("${hostname}",
			    request.getServerName());
	
	String title = null;
	if (decorate) {
	    title = StringUtil.findPattern(html, "(?s).*<title>(.+?)</title>");
	    html = (String)Utils.getNonNull(StringUtil.findPattern(html, "(?s).*<body[^>]*>(.+)</body>.*"), html);
	}

	html = getWikiManager().wikify(request, html);
	Result result = new Result(title!=null?title:BLANK,
				   new StringBuilder(html));
	if (decorate) {
	    return getEntryManager().addHeaderToAncillaryPage(
							      request, result);
	}
	result.setShouldDecorate(false);
	return result;
    }


    public  Result make404(Request request) throws Exception {
        String userAgent = request.getHeaderArg(HtmlUtils.HTTP_USER_AGENT);
        if (userAgent == null) {
            userAgent = "Unknown";
        }
        getLogManager().log(request,
                            "Unknown request:" + request.getUrl()
                            + " user-agent:" + userAgent + " ip:"
                            + request.getIp());

        Result result = makeErrorResult(request, "Unknown request " + request.getRequestPath());
        result.setResponseCode(Result.RESPONSE_NOTFOUND);

        return result;


    }


    /**
     * _more_
     */
    private void initRepositoryAttributes() {
        adminOnly             = getProperty(PROP_ACCESS_ADMINONLY, false);
        requireLogin          = getProperty(PROP_ACCESS_REQUIRELOGIN, false);
	alwaysHttps           =  getProperty(PROP_ALWAYS_HTTPS, false);
        allSsl                = getProperty(PROP_ACCESS_ALLSSL, false);
        sslIgnore             = getProperty(PROP_SSL_IGNORE, false);
        cacheResources        = getProperty(PROP_CACHE_RESOURCES, false);
	cacheHtdocs           = getProperty(PROP_CACHE_HTDOCS, true);
	logActivityToFile     = getProperty("ramadda.logging.logactivityfile", false);
	logActivityToDatabase = getProperty("ramadda.logging.logactivitydatabase", false);	
        repositoryName = getProperty(PROP_REPOSITORY_NAME, repositoryName);
        repositoryDescription = getProperty(PROP_REPOSITORY_DESCRIPTION, "");
        language              = getProperty(PROP_LANGUAGE, "");
        languageDefault       = getProperty(PROP_LANGUAGE_DEFAULT, "default");
        downloadOk            = getProperty(PROP_DOWNLOAD_OK, true);
        minifiedOk            = getProperty(PROP_MINIFIED, true);
        acceptRobots          = !getProperty(PROP_ACCESS_NOBOTS, false);
        commentsEnabled       =  getProperty("ramadda.enable_comments", false);
	useFixedHostName      =  getProperty(PROP_USE_FIXED_HOSTNAME, false);
        corsOk                = getProperty(PROP_CORS_OK, false);
	streamOutput          =  getProperty("ramadda.streamoutput",false);
        enableHostnameMapping = getProperty(PROP_ENABLE_HOSTNAME_MAPPING,   false);
        cdnOk                 = getProperty(PROP_CDNOK, true);

	//Create the default http headers
	List<String[]>tmpHttpHeaders = new ArrayList<String[]>();
	int i=1;
	while(true) {
	    String prop = getProperty("ramadda.httpheader" + (i++),null);
	    if(prop==null) break;
	    List<String> toks = Utils.splitUpTo(prop,":",2);
	    if(toks.size()==2) {
		tmpHttpHeaders.add(new String[]{toks.get(0).trim(),toks.get(1).trim()});
	    } else {
		System.err.println("Bad httpheader property:" + prop);
	    }
	}
	httpHeaders = tmpHttpHeaders;
    }

    /**
     * _more_
     */
    public void initAttributes() {
        initRepositoryAttributes();
        synchronized (repositoryManagers) {
            for (RepositoryManager manager : repositoryManagers) {
                manager.initAttributes();
            }
        }
        synchronized (outputHandlers) {
            for (OutputHandler outputHandler : outputHandlers) {
                outputHandler.initAttributes();
            }
        }
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getAdminOnly() {
        return adminOnly;
    }

    public boolean getLogActivity() {
	return getLogActivityToFile() || getLogActivityToDatabase();
    }

    public boolean getLogActivityToFile() {
	return logActivityToFile;
    }

    public boolean getLogActivityToDatabase() {
	return logActivityToDatabase;
    }    

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getRequireLogin() {
        return requireLogin;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getEnableHostnameMapping() {
        return enableHostnameMapping;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getDownloadOk() {
        return downloadOk;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getMinifiedOk() {
        return minifiedOk;
    }

    /**
     * _more_
     *
     * @param f _more_
     *
     * @return _more_
     */
    @Override
    public String getIconUrl(String f) {
        if (f == null) {
            return null;
        }

        if (getCdnOk()) {
            if (f.startsWith("/icons")) {
                return getPageHandler().getCdnPath(f);
            }
        }


        if (HU.isFontAwesome(f)) {
            return f;
        }

        return getUrlBase() + f;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getCdnOk() {
        return cdnOk;
    }


    
    /**
       Get the StreamOutput property.
       
       @return The StreamOutput
    **/
    public boolean getStreamOutput () {
	return streamOutput;
    }




    /**
     * _more_
     *
     * @return _more_
     */
    public String getLanguage() {
        return language;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getLanguageDefault() {
        return languageDefault;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getRepositoryName() {
        return repositoryName;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getCacheResources() {
        return cacheResources;
    }






    /**
     * Get properties from the local repository or cmd line properties - not from the plugins
     *
     * @param name _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public String getLocalProperty(String name, String dflt) {
        Object value = localProperties.get(name);
        if (value == null) {
            value = cmdLineProperties.get(name);
        }

        if (value == null) {
            value = coreProperties.get(name);
        }

        if (value == null) {
            return dflt;
        }

        return value.toString();
    }


    /**
     * _more_
     *
     * @param name _more_
     *
     * @return _more_
     */
    public String getProperty(String name) {
        return getPropertyValue(name, true);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public Properties getPluginProperties() {
        return pluginProperties;
    }


    /**
     * _more_
     *
     * @param name _more_
     * @param checkDb _more_
     *
     * @return _more_
     */
    public String getPropertyValue(String name, boolean checkDb) {
        return getPropertyValue(name, checkDb, false);
    }



    /**
     * get the property value
     *
     * @param name property name
     * @param checkDb Check the db properties
     * @param needsToBeNonEmpty If true then only return true if the string is length>0
     *
     * @return property value
     */
    private String getPropertyValue(String name, boolean checkDb,
                                    boolean needsToBeNonEmpty) {
	//	propdebug = name.equals(PROP_READ_ONLY);

	//	System.err.println("getPropertyValue:" + name);
	propcnt++;

	if(propdebug) {
	    System.err.println("getPropertyValue:" + name);
	    //	    propcnt++;
	}
        if (systemEnv == null) {
            systemEnv = System.getenv();
        }
        String prop     = null;
        String override = "override." + name;

        //Check if there is an override 
        prop = (String) cmdLineProperties.get(override);
        if (checkProperty(prop, needsToBeNonEmpty)) {
            if (propdebug) {
		System.err.println("\t override from command line:" + prop);
            }

            return prop;
        }

        prop = (String) localProperties.get(override);
        if (checkProperty(prop, needsToBeNonEmpty)) {
            if (propdebug) {
		System.err.println("\t override from local:" + prop);
            }

            return prop;
        }

        prop = (String) pluginProperties.get(override);
        if (checkProperty(prop, needsToBeNonEmpty)) {
            if (propdebug) {
		System.err.println("\t override from plugin:" + prop);
            }

            return prop;
        }

        prop = (String) coreProperties.get(override);
        if (checkProperty(prop, needsToBeNonEmpty)) {
            if (propdebug) {
		System.err.println("\t override from core:" + prop);
            }

            return prop;
        }

        boolean debug = false;
        //Order:  command line, database, local (e.g., ramadda home .properties files), plugins, core
        prop = (String) cmdLineProperties.get(name);
        if (checkProperty(prop, needsToBeNonEmpty)) {
            if (propdebug) {
		System.err.println("\t from command line:" + prop);
            }

            return prop;
        }

        if (checkDb) {
            prop = (String) getDbProperties().get(name);
            if (checkProperty(prop, needsToBeNonEmpty)) {
                if (propdebug) {
                    System.err.println("\t from db:" + prop);
                }

                return prop;
            }
        }

        prop = (String) localProperties.get(name);
        if (checkProperty(prop, needsToBeNonEmpty)) {
            if (propdebug) {
                System.err.println("\t from local:" + prop);
            }

            return prop;
        }

        prop = (String) pluginProperties.get(name);
        if (checkProperty(prop, needsToBeNonEmpty)) {
            if (propdebug) {
                System.err.println("\t from plugin:" + prop);
            }

            return prop;
        }


        //xxxxx
        prop = (String) coreProperties.get(name);
        if (checkProperty(prop, needsToBeNonEmpty)) {
            if (propdebug) {
                System.err.println("\t from core:" + prop);
            }

            return prop;
        }

        prop = System.getProperty(name);
        if (checkProperty(prop, needsToBeNonEmpty)) {
            if (propdebug) {
                System.err.println("\t from system:" + prop);
            }

            return prop;
        }


        prop = systemEnv.get(name);
        if (checkProperty(prop, needsToBeNonEmpty)) {
            if (propdebug) {
                System.err.println("\t from system:" + prop);
            }

            return prop;
        }
        if (propdebug) {
            System.err.println("\t from null:");
        }

        return null;

    }

    /**
     * Check if the property has been set
     *
     * @param prop The property value
     * @param needsToBeNonEmpty If true then only return true if the string is length>0
     *
     * @return Is property set
     */
    private boolean checkProperty(String prop, boolean needsToBeNonEmpty) {
        if (prop == null) {
            return false;
        }
        if (needsToBeNonEmpty && (prop.trim().length() == 0)) {
            return false;
        }

        return true;
    }




    /**
     * _more_
     *
     * @param props _more_
     * @param label _more_
     *
     * @return _more_
     */
    private String getPropertiesListing(Properties props, String label) {
        StringBuilder sb = null;
        for (Enumeration keys = props.keys(); keys.hasMoreElements(); ) {
            String key   = (String) keys.nextElement();
            String value = (String) props.get(key);
            if (sb == null) {
                sb = new StringBuilder("<h2>" + label + "</h2>");
            }
            sb.append(key + "=" + value + "<br>");
        }

        if (sb == null) {
            return "";
        }

        return sb.toString();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getPropertiesListing() {
        StringBuilder sb = new StringBuilder();
        sb.append(getPropertiesListing(getDbProperties(), "Database"));
        sb.append(getPropertiesListing(localProperties, "Local"));
        //      sb.append(getPropertiesListing(pluginProperties,"Plugin"));
        //      sb.append(getPropertiesListing(coreProperties,"Core"));
        return sb.toString();
    }


    /**
     *  this gets the property from this repository if defined. Else it will get the
     *  property from the parent repository if defined. Else this returns the dflt
     *
     * @param name _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public String getPropertyFromTree(String name, String dflt) {
        String value = getProperty(name, (String) null);
        if (Utils.stringDefined(value)) {
            return value;
        }
        if (getParentRepository() != null) {
            return getParentRepository().getPropertyFromTree(name, dflt);
        }

        return dflt;
    }


    /**
     * _more_
     *
     * @param name _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public String getProperty(String name, String dflt) {
        return getPropertyValue(name, dflt, true);
    }


    /**
     * _more_
     *
     * @param name _more_
     * @param dflt _more_
     * @param checkDb _more_
     *
     * @return _more_
     */
    public String getPropertyValue(String name, String dflt,
                                   boolean checkDb) {
        String prop = getPropertyValue(name, checkDb);
        if (prop != null) {
            return prop;
        }

        return dflt;
    }




    /**
     * _more_
     *
     * @param name _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public boolean getProperty(String name, boolean dflt) {
        String prop = getProperty(name);
        if (Utils.stringDefined(prop)) {
            boolean value =  Boolean.parseBoolean(prop.trim());
	    return value;
        }

        return dflt;
    }


    /**
     * _more_
     *
     * @param name _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public int getProperty(String name, int dflt) {
        String prop = getPropertyValue(name, true, true);
        if (prop != null) {
            return Integer.parseInt(prop.trim());
        }

        return dflt;
    }


    /**
     * _more_
     *
     * @param name _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public long getProperty(String name, long dflt) {
        String prop = getPropertyValue(name, true, true);
        if (prop != null) {
            return Long.parseLong(prop.trim());
        }

        return dflt;
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public double getProperty(String name, double dflt) {
        String prop = getPropertyValue(name, true, true);
        if (prop != null) {
            return Double.parseDouble(prop.trim());
        }

        return dflt;
    }



    /**
     * _more_
     *
     * @param name _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public boolean getDbProperty(String name, boolean dflt) {
        return Misc.getProperty(getDbProperties(), name, dflt);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    private Properties getDbProperties() {
        try {
	    if (dbProperties == null) {
		//We do this here to keep from an infinite loop at start up
		//		System.err.println("dbProperties-dummy");
		return dbPropertiesDummy;
	    }
	    Properties props = dbProperties.get();
	    //If its null that means the dbProperties TTLCache got cleared so we want to read them again
	    if (props == null) {
		props = readDatabaseProperties();
	    }
	    return props;
        } catch (Exception exc) {
            getLogManager().logError("Error reading globals", exc);
	    return null;
        }
    }


    /**
     * _more_
     *
     * @param name _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public double getDbProperty(String name, double dflt) {
        return Misc.getProperty(getDbProperties(), name, dflt);
    }






    /**
     * _more_
     *
     * @throws Exception _more_
     *
     */
    protected void initSchema() throws Exception {
        //Force a connection
        getDatabaseManager().init();
        String sql = getStorageManager().readUncheckedSystemResource(
								     getProperty(DatabaseManager.PROP_DB_SCRIPT));
        sql = getDatabaseManager().convertSql(sql);

        //        System.err.println("RAMADDA: loading schema");
        //        SqlUtil.showLoadingSql = true;
        getDatabaseManager().loadSql(sql, true, false);
        //        SqlUtil.showLoadingSql = false;
        //        System.err.println("RAMADDA: done loading schema");

        loadSql();

	
        getDatabaseManager().initComplete();
        readDatabaseProperties();
        getDatabaseManager().applyUpdates();

    }

    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     *
     * @throws Exception _more_
     */
    public void writeGlobal(String name, boolean value) throws Exception {
        writeGlobal(name, BLANK + value);
    }

    /**
     * _more_
     *
     * @param request The request
     * @param propName _more_
     *
     * @throws Exception _more_
     */
    public void writeGlobal(Request request, String propName)
	throws Exception {
        writeGlobal(request, propName, false);
    }



    /**
     * _more_
     *
     * @param request The request
     * @param propName _more_
     * @param deleteIfNull _more_
     *
     * @throws Exception _more_
     */
    public void writeGlobal(Request request, String propName,
                            boolean deleteIfNull)
	throws Exception {
        String value = request.getString(propName, getProperty(propName, ""));
        if (deleteIfNull && (value.trim().length() == 0)) {
            getDatabaseManager().delete(Tables.GLOBALS.NAME,
                                        Clause.eq(Tables.GLOBALS.COL_NAME,
						  propName));
            getDbProperties().remove(propName);
        } else {
            writeGlobal(propName, value);
        }
    }


    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     *
     * @throws Exception _more_
     */
    public void writeGlobal(String name, String value) throws Exception {
        getDatabaseManager().delete(Tables.GLOBALS.NAME,
                                    Clause.eq(Tables.GLOBALS.COL_NAME, name));
        getDatabaseManager().executeInsert(Tables.GLOBALS.INSERT,
                                           new Object[] { name,
							  value });

        if (name.equals(PROP_PROPERTIES)) {
            getDbProperties().load(
				   new ByteArrayInputStream(value.getBytes()));
            getPageHandler().clearCache();
        }
        getDbProperties().put(name, value);
    }



    /**
     *  _more_
     *
     *  @param request The request
     * @param state _more_
     *
     *  @return _more_
     *
     *  @throws Exception _more_
     */
    public List<Link> getOutputLinks(Request request,
                                     OutputHandler.State state)
	throws Exception {
        boolean    isRobot = request.getIsRobot();
        List<Link> links   = new ArrayList<Link>();

        for (OutputHandler outputHandler : outputHandlers) {
            if (isRobot && !outputHandler.allowRobots()) {
                continue;
            }
            //            String c = outputHandler.getClass().getName().toLowerCase();
            long t1 = System.currentTimeMillis();
            outputHandler.getEntryLinks(request, state, links);
            long t2 = System.currentTimeMillis();
            //            if(t2-t1>10) 
            //                System.err.println("handler:" + outputHandler.getName() +" "+ outputHandler+ " time:" +(t2-t1));
        }

        List<Link> okLinks = new ArrayList<Link>();


        for (Link link : links) {
            OutputType outputType = link.getOutputType();
            if (isOutputTypeOK(outputType)) {
                okLinks.add(link);
            }
        }

        return okLinks;
    }


    /**
     * _more_
     *
     * @param request The request
     * @param state _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Link> getLinksForHeader(Request request,
                                        OutputHandler.State state)
	throws Exception {
        List<Link> links   = getOutputLinks(request, state);

        List<Link> okLinks = new ArrayList<Link>();

        for (Link link : links) {
            if (link.isType(OutputType.TYPE_VIEW)) {
                okLinks.add(link);
            }
        }

        return okLinks;
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<OutputType> getOutputTypes() throws Exception {
        List<OutputType> allTypes = new ArrayList<OutputType>();
        for (OutputHandler outputHandler : outputHandlers) {
            allTypes.addAll(outputHandler.getTypes());
        }

        return allTypes;
    }


    /**
     * _more_
     *
     * @param outputType _more_
     *
     * @return _more_
     */
    public boolean isOutputTypeOK(OutputType outputType) {
        if (outputType == null) {
            return true;
        }

        return outputType.getOkToUse();
    }

    /**
     * _more_
     *
     * @param outputType _more_
     */
    public void setOutputTypeOK(OutputType outputType) {
        try {
            if (outputType == null) {
                return;
            }

            if (outputType.getId() == null) {
                outputType.setOkToUse(true);

                return;
            }
            String prop = getProperty(outputType.getId() + ".ok");
            if ((prop == null) || prop.equals("true")) {
                outputType.setOkToUse(true);

                return;
            }
            outputType.setOkToUse(false);
        } catch (Exception exc) {
            throw new IllegalArgumentException(exc);
        }
    }


    /**
     * _more_
     *
     * @param outputType _more_
     * @param ok _more_
     *
     * @throws Exception _more_
     */
    public void setOutputTypeOK(OutputType outputType, boolean ok)
	throws Exception {
        outputType.setOkToUse(ok);
        String prop = outputType.getId() + ".ok";
        writeGlobal(prop, "" + ok);
    }


    /**
     * _more_
     *
     * @param outputHandler _more_
     *
     * @return _more_
     */
    public boolean addOutputHandler(OutputHandler outputHandler) {
        synchronized (outputHandlers) {
            outputHandlers.add(outputHandler);
        }

        return addOutputHandlerTypes(outputHandler);
    }

    /**
     * _more_
     *
     * @param outputHandler _more_
     *
     * @return _more_
     */
    public boolean addOutputHandlerTypes(OutputHandler outputHandler) {
        boolean ok   = true;
        HashSet seen = new HashSet();
        for (OutputType type : outputHandler.getTypes()) {
            String id = type.getId();
            if (seen.contains(id)) {
                System.err.println("duplicate type:" + id);
                ok = false;
            }
            seen.add(id);
            if (outputHandlerMap.get(id) != null) {
                System.err.println("Already have output: " + id + " " + type);
                ok = false;
            } else {
                outputHandlerMap.put(id, outputHandler);
            }
        }

        return ok;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public List<OutputHandler> getOutputHandlers() {
        return new ArrayList<OutputHandler>(outputHandlers);
    }

    /**
     * _more_
     *
     * @param handlerClass _more_
     *
     * @return _more_
     */
    public OutputHandler getOutputHandler(Class handlerClass) {
        for (OutputHandler handler : outputHandlers) {
            if (handler.getClass().equals(handlerClass)) {
                return handler;
            }
        }

        return null;
    }


    /**
     * _more_
     *
     * @param outputType _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public OutputHandler getOutputHandler(OutputType outputType)
	throws Exception {
        if ( !isOutputTypeOK(outputType)) {
            return null;
        }

        return getOutputHandler(outputType.getId());
    }


    /**
     * _more_
     *
     * @param request The request
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public OutputHandler getOutputHandler(Request request) throws Exception {

        //Do this for now as it forces the setting of the output
        //from the Accept http arg
        request.isOutputDefined();

        OutputHandler handler = getOutputHandler(request.getOutput());
        if (handler != null) {
            return handler;
        }

        throw new IllegalArgumentException(
					   msgLabel("Could not find output handler for")
					   + request.getOutput());
    }


    /**
     * _more_
     *
     * @param type _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public OutputHandler getOutputHandler(String type) throws Exception {
        if ((type == null) || (type.length() == 0)) {
            type = OutputHandler.OUTPUT_HTML.getId();
        }

        return outputHandlerMap.get(type);
    }




    /**
     * _more_
     *
     * @return _more_
     *
     */
    public HtmlOutputHandler getHtmlOutputHandler() {
        try {
            return (HtmlOutputHandler) getOutputHandler(
							OutputHandler.OUTPUT_HTML);
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }


    /** _more_ */
    private JsonOutputHandler jsonOutputHandler;

    /**
     * _more_
     *
     * @return _more_
     */
    public JsonOutputHandler getJsonOutputHandler() {
        if (jsonOutputHandler == null) {
            jsonOutputHandler = (JsonOutputHandler) getOutputHandler(
								     org.ramadda.repository.output.JsonOutputHandler.class);
        }

        return jsonOutputHandler;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public CalendarOutputHandler getCalendarOutputHandler() {
        try {
            return (CalendarOutputHandler) getOutputHandler(
							    CalendarOutputHandler.OUTPUT_CALENDAR);
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
    public ZipOutputHandler getZipOutputHandler() throws Exception {
        return (ZipOutputHandler) getOutputHandler(
						   ZipOutputHandler.OUTPUT_ZIP);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public TypeHandler getGroupTypeHandler() {
        return groupTypeHandler;
    }

    public TypeHandler getFileTypeHandler() {
        return fileTypeHandler;
    }    



    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public XmlOutputHandler getXmlOutputHandler() throws Exception {
        return (XmlOutputHandler) getOutputHandler(
						   XmlOutputHandler.OUTPUT_XML);
    }




    /**
     * _more_
     *
     * @throws Exception _more_
     */
    private void initDefaultTypeHandlers() throws Exception {
        addTypeHandler(TypeHandler.TYPE_ANY,
                       new TypeHandler(this, TypeHandler.TYPE_ANY,
                                       "Any file type"));
        addTypeHandler(TypeHandler.TYPE_GROUP,
                       groupTypeHandler = new GroupTypeHandler(this));
        groupTypeHandler.putWikiText("simple","{{tabletree showType=false  showSize=false   showCreateDate=false}}");
        groupTypeHandler.setCategory("Documents");
        groupTypeHandler.putProperty("form.resource.show", "false");
        groupTypeHandler.putProperty("icon", ICON_FOLDER);
        groupTypeHandler.setHelp("A group of entries");
        TypeHandler typeHandler;
        addTypeHandler(TypeHandler.TYPE_FILE,
                       fileTypeHandler =typeHandler = new TypeHandler(this, "file", "File",
								      "Documents"));
        typeHandler.setHelp(
			    "The entry type is figured out by the file extension");
        typeHandler.putProperty("icon", ICON_FILE);

        addTypeHandler(ProcessFileTypeHandler.TYPE_PROCESS,
                       processFileTypeHandler = new ProcessFileTypeHandler(this));

    }

    private  ProcessFileTypeHandler processFileTypeHandler;

    public ProcessFileTypeHandler getProcessFileTypeHandler() {
	return processFileTypeHandler;
    }


    /**
     * _more_
     *
     * @param typeName _more_
     * @param typeHandler _more_
     */
    public void addTypeHandler(String typeName, TypeHandler typeHandler) {
        addTypeHandler(typeName, typeHandler, false);
    }



    /**
     * _more_
     *
     * @param typeName _more_
     * @param typeHandler _more_
     * @param overwrite _more_
     */
    public void addTypeHandler(String typeName, TypeHandler typeHandler,
                               boolean overwrite) {
        if (typeHandlersMap.containsKey(typeName)) {
            if ( !overwrite) {
                return;
            }
            TypeHandler oldTypeHandler = typeHandlersMap.get(typeName);
            typeHandlersMap.remove(typeName);
            allTypeHandlers.remove(oldTypeHandler);
        }

        if ( !typeHandlersMap.contains(typeName)) {
            typeHandlersMap.put(typeName, typeHandler);
            allTypeHandlers.add(typeHandler);
        }
    }


    /**
     * _more_
     *
     * @param typeHandler _more_
     */
    public void removeTypeHandler(TypeHandler typeHandler) {
        typeHandlersMap.remove(typeHandler.getType());
        allTypeHandlers.remove(typeHandler);
    }

    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<TypeHandler> getTypeHandlers() throws Exception {
        return new ArrayList<TypeHandler>(allTypeHandlers);
    }


    /**
     * _more_
     *
     * @param anyOk _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<TypeHandler> getTypeHandlersForDisplay(boolean anyOk)
	throws Exception {
        List<TypeHandler> typeHandlers = getTypeHandlers();
        Comparator        comp         = new Comparator() {
		public int compare(Object o1, Object o2) {
		    TypeHandler t1 = (TypeHandler) o1;
		    TypeHandler t2 = (TypeHandler) o2;
		    if (t1.getPriority() == t2.getPriority()) {
			return t1.getLabel().compareTo(t2.getLabel());
		    }

		    return t1.getPriority() - t2.getPriority();
		}
	    };
        Object[] array = typeHandlers.toArray();
        Arrays.sort(array, comp);
        List<TypeHandler> tmp = new ArrayList<TypeHandler>();
        for (TypeHandler typeHandler :
		 (List<TypeHandler>) Misc.toList(array)) {
            if ( !typeHandler.getIncludeInSearch()
		 && !typeHandler.getForUser()) {
                continue;
            }
            if (typeHandler.isAnyHandler()) {
                if ( !anyOk) {
                    continue;
                }
            }
            tmp.add(typeHandler);
        }

        return tmp;
    }


    /**
     * _more_
     *
     * @param request The request
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public TypeHandler getTypeHandler(Request request) throws Exception {
        if (request != null) {
            String type = request.getString(ARG_TYPE,
                                            TypeHandler.TYPE_ANY).trim();

            return getTypeHandler(type, true);
        } else {
            return getTypeHandler(TypeHandler.TYPE_FILE);
        }
    }

    /**
     * _more_
     *
     * @param type _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public TypeHandler getTypeHandler(String type) throws Exception {
        return getTypeHandler(type, false);
    }

    /**
     * _more_
     *
     * @param type _more_
     * @param useDefaultIfNotFound _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public TypeHandler getTypeHandler(String type,
                                      boolean useDefaultIfNotFound)
	throws Exception {
        if (type.trim().length() == 0) {
            type = TypeHandler.TYPE_FILE;
        }
        TypeHandler typeHandler = (TypeHandler) typeHandlersMap.get(type);
        if (typeHandler == null) {
            if ( !useDefaultIfNotFound) {
                return null;
            }
            try {
                Class c = Misc.findClass("org.ramadda.repository." + type);
                Constructor ctor = Misc.findConstructor(c,
							new Class[] { Repository.class,
							    String.class });
                typeHandler = (TypeHandler) ctor.newInstance(new Object[] {
			this,
			type });
            } catch (Throwable cnfe) {}
        }

        if (typeHandler == null) {
            return getTypeHandler(TypeHandler.TYPE_ANY);
        }

        return typeHandler;
    }



    /**
     * _more_
     *
     * @param request The request
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processPing(Request request) throws Exception {
        if (request.getString(ARG_RESPONSE, "").equals(RESPONSE_XML)) {
            Document resultDoc = XmlUtil.makeDocument();
            Element resultRoot = XmlUtil.create(resultDoc, TAG_RESPONSE,
						null, new String[] { ATTR_CODE,
								     "ok" });
            String xml = XmlUtil.toString(resultRoot);

            return new Result(xml, MIME_XML);
        }
        StringBuilder sb = new StringBuilder("OK");

        return new Result("", sb);
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
    public Result processClearState(Request request) throws Exception {
        StringBuilder sb         = new StringBuilder("");
        String        passPhrase = getProperty(PROP_PASSPHRASE, "").trim();
        if ((passPhrase.length() > 0) && request.defined(PROP_PASSPHRASE)) {
            if (request.getString(PROP_PASSPHRASE,
                                  "").trim().equals(passPhrase)) {
                clearAllCaches();
                readDatabaseProperties();
                //Tell the other repositoryManagers that the settings changed
                synchronized (repositoryManagers) {
                    for (RepositoryManager repositoryManager :
			     getRepository().getRepositoryManagers()) {
                        repositoryManager.adminSettingsChanged();
                    }
                }
                sb.append("OK, state is cleared");

                return new Result("", sb);
            }
            sb.append("Bad pass phrase");
            sb.append(HtmlUtils.p());
        }
        sb.append(HtmlUtils.p());
        sb.append(
		  "This form allows you to clear any caches and have RAMADDA reload properties");
        sb.append(HtmlUtils.br());
        if (passPhrase.length() == 0) {
            sb.append(
		      "The pass phrase needs to be set as a property on your server - <i>ramadda.passphrase</i>");
            sb.append(HtmlUtils.br());
        }
        sb.append(
		  "Note: The pass phrase is not meant to be secure, it is just used so anonymous users can't be clearing your repository state");
        sb.append(HtmlUtils.hr());
        sb.append(HtmlUtils.formTable());
        sb.append(request.formPost(URL_CLEARSTATE));
        sb.append(HtmlUtils.formEntry(msgLabel("Pass Phrase"),
                                      HtmlUtils.input(PROP_PASSPHRASE)));
        sb.append(
		  HtmlUtils.formEntry(
				      "", HtmlUtils.submit("Clear Repository State")));
        sb.append(HtmlUtils.formTableClose());
        sb.append(HtmlUtils.formClose());

        return new Result("", sb);
    }


    public boolean isGptEnabled() {
	return Utils.stringDefined(getProperty("gpt.api.key"));
    }




    public Result processGpt(Request request)  throws Exception {
	if(request.isAnonymous()) {
	    String json = JsonUtil.map(Utils.makeList("error", JsonUtil.quote("You must be logged in to use the rewrite service")));
	    return new Result("", new StringBuilder(json), "text/json");
	}

	String text =request.getString("text","");
	String promptPrefix = request.getString("promptprefix",
						"Rewrite the following text:");
	String promptSuffix = request.getString("promptsuffix",
						"");
	//	text = callGpt("Rewrite the following text:","",new StringBuilder(text),1000,false);		    
	text = callGpt(promptPrefix,promptSuffix,new StringBuilder(text),1000,false,null);		    
	String json = JsonUtil.map(Utils.makeList("result", JsonUtil.quote(text)));
	return new Result("", new StringBuilder(json), "text/json");
	
    }

    private Result makeJsonErrorResult(String error) {
	String json = JsonUtil.map(Utils.makeList("error", JsonUtil.quote(error)));
	return new Result("", new StringBuilder(json), "text/json");
    }

    public Result processTranscribe(Request request)  throws Exception {
	try {
	    return processTranscribeInner(request);
	} catch(Exception exc) {
	    getLogManager().logError("Error calling transcribe",exc);
	    return makeJsonErrorResult("An error has occurred:" + exc);
	}
    }

    private Result processTranscribeInner(Request request)  throws Exception {	
	if(request.isAnonymous()) {
	    return makeJsonErrorResult("You must be logged in to use the rewrite service");
	}
	String gptKey = getRepository().getProperty("gpt.api.key");
	if(gptKey==null) {
	    if(debugGpt) System.err.println("\tno gptKey");
	    return makeJsonErrorResult("GPT is not enabled");
	}

	File file = new File(request.getUploadedFile("audio-file"));
	String[]args = new String[]{"Authorization","Bearer " +gptKey};
	System.err.println("key:" + args[0]+":");
	String mime = request.getString("mimetype","audio/webm");

	String fileName = "audio" + mime.replaceAll(".*/",".");
	//	fileName = "test.mp4";
	//	file = new File("/Users/jeffmc/test.mp4");
	//	mime = "audio/mp4";

	List postArgs   =Utils.add(new ArrayList(),
				   "model","whisper-1","file", new IO.FileWrapper(file,fileName,mime));
	System.err.println(postArgs);
	URL url =  new URL("https://api.openai.com/v1/audio/transcriptions");
	IO.Result result =  IO.doMultipartPost(url, args,postArgs);
	System.err.println("transcribe:" + result);
	String results = result.getResult();
	JSONObject json = new JSONObject(results);
	if(json.has("text")) {
	    String text = json.getString("text");
	    if(request.get("addfile",false)) {
		getEntryManager().processEntryAddFile(request, file,fileName,text);
	    }

	    StringBuilder sb = new StringBuilder(JsonUtil.map(Utils.makeList("results", JsonUtil.quote(text))));
	    return new Result("", sb, "text/json");
	}
	if(json.has("error")) {
	    JSONObject error = json.getJSONObject("error");
	    return makeJsonErrorResult(error.getString("message"));
	}
	return makeJsonErrorResult("An error occurred:" + results);
    }


    public String callGpt(String prompt1,String prompt2,StringBuilder corpus,int tokens,boolean tokenize,int[]initTokenLimit)
	throws Exception {
	if(debugGpt) System.err.println("callGpt");
	String text = corpus.toString();
	String gptKey = getRepository().getProperty("gpt.api.key");
	if(gptKey==null) {
	    if(debugGpt) System.err.println("\tno gptKey");
	    return null;
	}

	int tokenLimit = initTokenLimit!=null?initTokenLimit[0]:GPT_TOKEN_LIMIT;
	while(tokenLimit>=500) {
	    if(initTokenLimit!=null) initTokenLimit[0] = tokenLimit;
	    StringBuilder gptCorpus = new StringBuilder(prompt1);
	    gptCorpus.append("\n\n");
	    if(!tokenize) {
		gptCorpus.append(text);
	    } else {
		//	    debugGpt = true;
		if(debugGpt) System.err.println("Repository.callGpt tokenizing " + text.length());
		text = Utils.removeNonAscii(text," ").replaceAll("[,-\\.\n]+"," ").replaceAll("  +"," ");
		if(text.trim().length()==0) {
		    if(debugGpt) System.err.println("\tRepository.callGpt no text");
		    return null;
		}
		List<String> toks = Utils.split(text," ",true,true);
		//best guess-
		int extraCnt = 0;
		int i =0;
		for(i=0;i<toks.size() && i+extraCnt<tokenLimit;i++) {
		    String tok = toks.get(i);
		    if(tok.length()>5) {
			extraCnt+=(int)(tok.length()/5);
		    }
		    gptCorpus.append(tok);
		    gptCorpus.append(" ");
		}
		if(debugGpt) System.err.println("\tRepository.callGpt done tokenizing: i=" + i +" extraCnt=" + extraCnt);
	    }
	    gptCorpus.append("\n\n");
	    gptCorpus.append(prompt2);
	    if(debugGpt) System.err.println("Repository.callGpt return max tokens:" + tokens);
	    String gptText =  gptCorpus.toString();
	    List<String> args = Utils.makeList("temperature", "0",
					       "max_tokens" ,""+ tokens,
					       "top_p", "1.0");
	    boolean useTurbo = true;
	    if(!useTurbo) {
		Utils.add(args,"model",JsonUtil.quote("text-davinci-003"),"prompt",  JsonUtil.quote(gptText));
	    } else {
		Utils.add(args,"model",JsonUtil.quote("gpt-3.5-turbo"));
		Utils.add(args,"messages",JsonUtil.list(JsonUtil.map(
								     "role",JsonUtil.quote("user"),
								     "content",JsonUtil.quote(gptText))));
	    }

	    String body = JsonUtil.map(args);
	    String url = useTurbo?"https://api.openai.com/v1/chat/completions":"https://api.openai.com/v1/completions";
	    IO.Result  result = IO.getHttpResult(IO.HTTP_METHOD_POST
						 , new URL(url), body,
						 "Content-Type","application/json",
						 "Authorization","Bearer " +gptKey);
	    if(result.getError()) {
		try {
		    JSONObject json = new JSONObject(result.getResult());
		    JSONObject error = json.optJSONObject("error");
		    if(error == null || !error.optString("code","").equals("context_length_exceeded")) {
			System.err.println("NO CODE:" + result.getResult());
			return null;
			//			throw new RuntimeException("Unable to process GPT request:" + result.getResult());
		    }
		    tokenLimit-=1000;
		    if(debugGpt)
			System.err.println("Repostory.callGpt: too many tokens. Trying again with limit:" + tokenLimit);
		    continue;
		} catch(Exception exc) {
		    System.err.println("EXC:" + exc);
		    exc.printStackTrace();

		    throw new RuntimeException("Unable to process GPT request:" + result.getResult());
		}
	    }

	    JSONObject json = new JSONObject(result.getResult());
	    if(debugGpt)	    System.err.println("gpt json:" + json);		
	    if(json.has("choices")) {
		JSONArray choices = json.getJSONArray("choices");
		if(choices.length()>0) {
		    JSONObject choice= choices.getJSONObject(0);
		    if(choice.has("text")) {
			return choice.getString("text");
		    } else if(choice.has("message")) {
			JSONObject message= choice.getJSONObject("message");
			return message.optString("content",null);
		    }
		    System.err.println("No results from GPT:" + result);
		}
	    }
	}
	return null;
    }



    public Result processTiffToPng(Request request) throws Exception {
	String file = request.getString("url","");
	//	String file = "/Users/jeffmc/test.tif";
	System.err.println("TIFF Proxy:" + file);
	try {
	    final BufferedImage tif = ImageIO.read(getStorageManager().getInputStream(file));
	    final PipedInputStream      in   = new PipedInputStream();
	    final PipedOutputStream     out  = new PipedOutputStream(in);
	    Result theResult = new Result(in,"image/png");
	    request.setReturnFilename(IOUtil.stripExtension(IO.getFileTail(file))+".png");
	    Misc.run(new Runnable() {
		    public void run()  {
			try {
			    ImageIO.write(tif, "png", out);
			    out.close();
			} catch(Exception exc) {
			    IOUtil.close(in);
			    IOUtil.close(out);
			    System.err.println("Error:" + exc);
			    exc.printStackTrace();
			}
		    }
		});
	    return theResult;
	} catch(Exception exc) {
	    System.err.println("Error:" + exc);
	    throw new RuntimeException(exc);
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
    public Result processProxy(Request request) throws Exception {
        String url = request.getString(ARG_URL, (String) null);
        if (url != null) {
            if ( !url.startsWith("http:") && !url.startsWith("https:")) {
                throw new IllegalArgumentException("Bad URL:" + url);
            }
            //Check the whitelist
            boolean ok = false;
            for (String pattern :
		     Utils.split(getProperty(PROP_PROXY_WHITELIST, ""), ",",
				 true, true)) {
                //            System.err.println("pattern:" + pattern);
                if (url.matches(pattern)) {
                    ok = true;
                    break;
                }
            }
            if ( !ok) {
		if(url.startsWith("https://ramadda.org")) ok = true;
	    }

            if ( !ok) {
                throw new IllegalArgumentException("URL not in whitelist:"
						   + url);
            }
        }
        if ((url == null) && request.defined(ARG_ENTRYID)) {
            Entry entry = getEntryManager().getEntry(request);
            if (entry == null) {
                throw new IllegalArgumentException("No Entry found:");
            }
            if ( !entry.getResource().isUrl()) {
                throw new IllegalArgumentException("Entry not a URL");
            }
            url = entry.getResource().getPath();
            if (url.startsWith("//")) {
                if (request.getAbsoluteUrl().startsWith("https:")) {
                    url = "https:" + url;
                } else {
                    url = "http:" + url;
                }
            }
        }


        if (url == null) {
            throw new IllegalArgumentException("No URL");
        }

        URLConnection connection = new URL(url).openConnection();
	//Pass through some of the headers
	for(String hdr:new String[]{"Content-disposition","Content-Type"}) {
	    String v = connection.getHeaderField(hdr);
	    if(v!=null)
		request.setHeader(hdr,v);
	}

        InputStream   is         = connection.getInputStream();
        if (request.get("xmltojson", false)) {
            String contents = IOUtil.readInputStream(is);
            contents = contents.trim();
            IOUtil.close(is);
            contents = JsonUtil.xmlToJson(XmlUtil.getRoot(contents));
            //            System.out.println(contents);

            return new Result(new ByteArrayInputStream(contents.getBytes()),
                              "application/json");
        }


        if (request.get("trim", false)) {
            String contents = IOUtil.readInputStream(is);
            contents = contents.trim();
            IOUtil.close(is);

            return new Result(new ByteArrayInputStream(contents.getBytes()),
                              "application/json");
        }


        return request.returnStream(is);
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
    public Result processBlank(Request request) throws Exception {
        Result result = new Result("", new StringBuilder());
        result.setShouldDecorate(false);

        return result;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getRepositoryDescription() {
        return repositoryDescription;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getRepositoryEmail() {
        return getProperty(PROP_ADMIN_EMAIL, "");
    }



    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public ServerInfo getServerInfo() throws Exception {
        int    sslPort = getHttpsPort();
        String url     = getTmpRequest().getAbsoluteUrl("");

        return new ServerInfo(url, getHostname(), getPort(), sslPort,
                              getUrlBase(), getRepositoryName(),
                              getRepositoryDescription(),
                              getRepositoryEmail(),
                              getRegistryManager().isEnabledAsServer(),
                              false);
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
    public Result processRobotsTxt(Request request) throws Exception {
        StringBuilder sb = new StringBuilder("User-agent: *\n");
        if ( !acceptRobots() && request.getIsRobot()) {
            sb.append("Disallow: /\n");
        } else {
            sb.append("Allow: /\n");
        }
        Result result = new Result("", sb);
        result.setShouldDecorate(false);

        return result;
    }

    private byte[] favIcon;
    public Result processFavIcon(Request request) throws Exception {
	if(favIcon==null) {
	    InputStream is =getStorageManager().getInputStream("/org/ramadda/repository/htdocs/favicon.ico");
	    favIcon = IOUtil.readBytes(is);
	    is.close();
	}
	InputStream inputStream = new ByteArrayInputStream(favIcon);
        Result result = new Result("favicon.ico", inputStream,  "image/x-icon");
        result.setCacheOk(true);
	return result;
    }

    private String baseJs;
    public String getBaseJs(Request request) throws Exception {
	if(baseJs==null) {
	    InputStream is =getStorageManager().getInputStream("/org/ramadda/repository/htdocs/base.js");
	    baseJs = new String(IOUtil.readBytes(is));
	    is.close();
	}
	String js = baseJs;
	String extra = "";
        String base    = getUrlBase();
	js = js.replace(
			"${ramadda.htdocs}",
			base + "/"
			+ RepositoryUtil.getHtdocsVersion()).replace(
								     "${ramadda.root}", base);
	js  = js.replace("${ramadda.cdn}", getPageHandler().getCdnPath(""));
	js = js.replace("${ramadda.search.tree}",
			getSearchManager().isLuceneEnabled()+ "");
	js = js.replace("${ramadda.urlroot}", base);
	js = js.replace(
			"${ramadda.baseentry}",
			getEntryManager().getRootEntry().getId());
	js = js.replace("${hostname}",
			request.getServerName());
	js = js.replace("${ramadda.user}",
			request.getUser().getId());

	js = js.replace("${ramadda.base.extra}",extra);
	return js;
    }


    public Result processBaseJs(Request request) throws Exception {
	String js = getBaseJs(request);
	Result result =  new Result("base.js",js.getBytes(),"application/x-javascript");
        result.setCacheOk(false);
	return result;
    }
    

    public Result processTestAction(Request request) throws Exception {
	//Make the action
	ActionManager.Action action = new ActionManager.Action() {
		public void run(Object actionId) throws Exception {
		    //In a real scenario you can pass around the actionId and check if cancel was called, 
		    //update the actionMessage and call setContinueHtml when you are done
		    
		    //Run for 10 seconds
		    for(int i=0;i<10;i++) {
			//check if cancel was called
			if(!getActionManager().getActionOk(actionId)) {
			    //cancel was called
			    System.err.println("Test action: cancel was called");
			    return;
			}
			//post a status message
			getActionManager().setActionMessage(actionId,"test step: " + i);
			Misc.sleepSeconds(1);
		    }
		    //We are done
		    getActionManager().setContinueHtml(actionId,"done message");
		}
	    };
	//This runs the action and makes the json return
	return getActionManager().doJsonAction(request, action,  "", "",null);
    }


    /**
     * _more_
     *
     * @param request The request
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processInfo(Request request) throws Exception {
        //        getDatabaseManager().printIt();
        if (request.getString(ARG_RESPONSE, "").equals(RESPONSE_XML)) {
            Document doc  = XmlUtil.makeDocument();
            Element  info = getServerInfo().toXml(this, doc);
            info.setAttribute(ATTR_CODE, CODE_OK);
            String xml = XmlUtil.toString(info);

            //            System.err.println("returning xml:" + xml);
            return new Result(xml, MIME_XML);
        }
        StringBuilder sb = new StringBuilder("");
        getPageHandler().sectionOpen(request, sb,"Repository Information",false);
        sb.append(HtmlUtils.formTable());
        sb.append(
		  HtmlUtils.formEntry(
				      msgLabel("RAMADDA Version"),
				      RepositoryUtil.getVersion()));
        sb.append(
		  HtmlUtils.formEntry(
				      msgLabel("Build Date"),
				      getRepository().getProperty(PROP_BUILD_DATE, "N/A")));
        String version =
            Runtime.class.getPackage().getImplementationVersion();
        sb.append(HtmlUtils.formEntry(msgLabel("Java Version"), version));
        getAdmin().addInfo(request, sb);
        if (request.exists("class")) {
            Class c = Class.forName(request.getString("class", ""));
            URL classesRootDir =
                c.getProtectionDomain().getCodeSource().getLocation();
            sb.append(HtmlUtils.formEntry(msgLabel("Class location - "
						   + request.getString("class",
								       "")), classesRootDir.toString()));

        }


        sb.append(HtmlUtils.formEntry(msgLabel("Port"), "" + getPort()));
        sb.append(HtmlUtils.formEntry(msgLabel("Https Port"),
                                      "" + getHttpsPort()));

        sb.append(HtmlUtils.formTableClose());
        getPageHandler().sectionClose(request, sb);
        return new Result("", sb);
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
    public Result processHttpTest(Request request) throws Exception {
        StringBuilder sb = new StringBuilder("");
        getPageHandler().sectionOpen(request, sb,"Http Test",false);
        sb.append(
		  "Below is the http header that was received. On reload of this page there should be a 'ramadda_repository_session' cookie.");
        sb.append("<ul>");
        Hashtable args = request.getHttpHeaderArgs();
        for (Enumeration keys = args.keys(); keys.hasMoreElements(); ) {
            String key   = (String) keys.nextElement();
            String value = (String) args.get(key);
            sb.append("<li>" + key + "=" + value + "<br>");
        }
        sb.append("</ul>");
        getPageHandler().sectionClose(request, sb);
        Result result = new Result("", sb);

        return result;
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
    public Result processLicense(Request request) throws Exception {
        StringBuilder sb = new StringBuilder("");
        getPageHandler().sectionOpen(request, sb,"RAMADDA License",false);	
        String license =
            getStorageManager().readSystemResource(
						   "/org/ramadda/repository/resources/ramadda_license.txt");

        license = license.replace("(C)", "&copy;");
        license = license.replace("(c)", "&copy;");
        sb.append("<pre>");
        sb.append(license);
        sb.append("</pre>");
        getPageHandler().sectionClose(request, sb);
        return new Result("", sb);
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
    public Result processColorTables(Request request) throws Exception {
        StringBuilder sb = new StringBuilder();
        getPageHandler().sectionOpen(request, sb,"Color Tables",false);	
        sb.append(HtmlUtils.div("", "id='colortables'"));
	sb.append(HtmlUtils.importJS(getHtdocsUrl("/colortables.js")));
	//sb.append(HtmlUtils.importJS(getHtdocsUrl("/esdlcolortables.js")));	
        sb.append(HtmlUtils.script("Utils.displayAllColorTables('colortables');"));
        getPageHandler().sectionClose(request, sb);
        return new Result("", sb);
    }



    /**
     * _more_
     *
     * @param request The request
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processDocs(Request request) throws Exception {
        StringBuilder  sb      = new StringBuilder();
        List<String[]> docUrls = getPluginManager().getDocUrls();
        getPageHandler().sectionOpen(request, sb,"Available documentation",false);
        if (docUrls.size() == 0) {
            sb.append(
		      getPageHandler().showDialogNote(
						      msg("No documentation available")));
        }
        CategoryBuffer cats = new CategoryBuffer();
        for (String cat : new String[] { "Basics", "Integration",
                                         "Other Documentation",
                                         "Advanced" }) {
            cats.get(cat);
        }
        for (String[] url : docUrls) {
            StringBuilder tmpSB = new StringBuilder();
            tmpSB.append("<li>");
            String fullUrl = getUrlBase() + url[0];
            tmpSB.append(HtmlUtils.href(fullUrl, url[1]));
            tmpSB.append("<br>&nbsp;");
            cats.get((url[2] == null)
                     ? "Other Documentation"
                     : url[2]).append(tmpSB.toString());
        }
        for (String cat : cats.getCategories()) {
            StringBuilder cb = cats.get(cat);
            if (cb.length() > 0) {
                sb.append(HtmlUtils.h3(cat));
                sb.append("<ul>");
                sb.append(cb);
                sb.append("</ul>");
            }
        }

        getPageHandler().sectionClose(request, sb);
        return new Result("Documentation", sb);
    }




    /**
     * _more_
     *
     * @param request The request
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processMessage(Request request) throws Exception {
        StringBuilder sb = new StringBuilder();
        request.appendMessage(sb);

        return new Result(BLANK, sb);
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
    public Result processGetTmpFile(Request request) throws Exception {
        String file     = request.getString("file", "");
        File   f        = getStorageManager().getTmpFilePath(request, file);
        String fileName = f.getName();
        int    idx      = fileName.indexOf("_file_");
        if (idx >= 0) {
            fileName = fileName.substring(idx + 6);
        }
        request.setReturnFilename(fileName);

        InputStream inputStream = getStorageManager().getFileInputStream(f);

        Result      result      = new Result(BLANK, inputStream, "");

        return result;
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
    public Result processTest(Request request) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("HttpServletRequest.getServerName: ");
        sb.append(request.getHttpServletRequest().getServerName());
        sb.append("<br>");


        sb.append("HttpServletRequest.getServerPort: ");
        sb.append(request.getHttpServletRequest().getServerPort());
        sb.append("<br>");


        return new Result(BLANK, sb);
    }

    /**
     * _more_
     *
     * @param request The request
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processDummy(Request request) throws Exception {
        return new Result(BLANK, new StringBuilder(BLANK));
    }

    public Result processDummyInstall(Request request) throws Exception {
	StringBuilder sb = new StringBuilder();
        getPageHandler().sectionOpen(request, sb, "Install", false);
	sb.append(getPageHandler().showDialogNote("Install is finished"));
        getPageHandler().sectionClose(request, sb);
        return new Result("Install", sb);
    }    



    /**
     * _more_
     *
     * @param request The request
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processSslRedirect(Request request) throws Exception {
        if (request.getCheckingAuthMethod()) {
            return new Result(AuthorizationMethod.AUTH_HTTP);
        }

        if (request.isAnonymous()) {
            throw new AccessException("Cannot access data", request);
        }
        String url = request.getBase64String(ARG_REDIRECT, "");
        return new Result(url);
    }



    /**
     * _more_
     *
     *
     * @param request The request
     * @param what _more_
     * @param includeExtra _more_
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List getListLinks(Request request, String what,
                             boolean includeExtra)
	throws Exception {
        List                 links       = new ArrayList();
        TypeHandler          typeHandler = getTypeHandler(request);
        List<TwoFacedObject> typeList    = typeHandler.getListTypes(false);
        String               extra1      = " class=subnavnolink ";
        String               extra2      = " class=subnavlink ";
        if ( !includeExtra) {
            extra1 = BLANK;
            extra2 = BLANK;
        }
        if (typeList.size() > 0) {
            for (TwoFacedObject tfo : typeList) {
                if (what.equals(tfo.getId())) {
                    links.add(HtmlUtils.span(tfo.toString(), extra1));
                } else {
                    links.add(HtmlUtils.href(request.makeUrl(URL_LIST_SHOW,
							     ARG_WHAT, (String) tfo.getId(), ARG_TYPE,
							     (String) typeHandler.getType()), tfo.toString(),
					     extra2));
                }
            }
        }
        String typeAttr = BLANK;
        if ( !typeHandler.getType().equals(TypeHandler.TYPE_ANY)) {
            typeAttr = "&type=" + typeHandler.getType();
        }


        String[] whats = { WHAT_TYPE, WHAT_TAG, WHAT_ASSOCIATION };
        String[] names = { "Types", "Tags", "Associations" };
        for (int i = 0; i < whats.length; i++) {
            if (what.equals(whats[i])) {
                links.add(HtmlUtils.span(names[i], extra1));
            } else {
                links.add(HtmlUtils.href(request.makeUrl(URL_LIST_SHOW,
							 ARG_WHAT, whats[i]) + typeAttr, names[i], extra2));
            }
        }

        return links;
    }






    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Request getTmpRequest()  {
	try {
	    return getTmpRequest(UserManager.USER_ANONYMOUS);
	}catch(Exception exc) {
	    throw new RuntimeException(exc);
	}
    }

    /**
     * _more_
     *
     * @param userId _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Request getTmpRequest(String userId) throws Exception {
        User user = getUserManager().findUser(userId);

        if (user == null) {
            //            throw new IllegalArgumentException("Could not find user:" + userId);
        }
        Request request = new Request(getRepository(), "", new Hashtable());
        request.setUser(user);
        request.setSessionId(getGUID());

        return request;
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Request getAdminRequest() throws Exception {
        User    user    = getUserManager().getAdminUser();
        Request request = new Request(getRepository(), "", new Hashtable());
        request.setUser(user);
        request.setSessionId(getGUID());

        return request;
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
    public Request getTmpRequest(Entry entry) throws Exception {
        Request request = getTmpRequest();
        request.setPageStyle(getPageHandler().doMakePageStyle(request,
							      entry));

        return request;
    }



    /**
     * _more_
     *
     * @param user _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Request getRequest(User user) throws Exception {
        Request request = new Request(getRepository(), "", new Hashtable());
        request.setUser(user);

        return request;
    }



    /**
     * _more_
     *
     * @param msg _more_
     *
     * @return _more_
     */
    public static String msg(String msg) {
        return PageHandler.msg(msg);
    }

    /**
     * _more_
     *
     * @param msg _more_
     *
     * @return _more_
     */
    public static String msgLabel(String msg) {
        return PageHandler.msgLabel(msg);
    }

    /**
     * _more_
     *
     * @param h _more_
     *
     * @return _more_
     */
    public static String msgHeader(String h) {
        return PageHandler.msgHeader(h);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param s _more_
     *
     * @return _more_
     */
    public String translate(Request request, String s) {
        return getPageHandler().translate(request, s);
    }





    /**
     * _more_
     *
     * @param id _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getResource(String id) throws Exception {
        return getResource(id, false);
    }


    /**
     * _more_
     *
     * @param id _more_
     * @param ignoreErrors _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getResource(String id, boolean ignoreErrors)
	throws Exception {
        String resource = (String) resources.get(id);
        if (resource != null) {
            return resource;
        }
        String fromProperties = getProperty(id);
        if (fromProperties != null) {
            List<String> paths = getResourcePaths(id);
            for (String path : paths) {
                try {
                    resource = getStorageManager().readSystemResource(path);
                } catch (Exception exc) {
                    //noop
                }
                if (resource != null) {
                    break;
                }
            }
        } else {
            try {
                resource = getStorageManager().readSystemResource(
								  getStorageManager().localizePath(id));
            } catch (Exception exc) {
                if ( !ignoreErrors) {
                    throw exc;
                }
            }
        }
        if (getCacheResources() && (resource != null)) {
            resources.put(id, resource);
        }

        return resource;
    }





    /**
     * _more_
     *
     * @param request The request
     * @param includeAny _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String makeTypeSelect(Request request, boolean includeAny)
	throws Exception {
        return makeTypeSelect(request, includeAny, "", false, null);
    }

    /**
     * _more_
     *
     * @param request The request
     * @param includeAny _more_
     * @param selected _more_
     * @param checkAddOk _more_
     * @param exclude _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String makeTypeSelect(Request request, boolean includeAny,
                                 String selected, boolean checkAddOk,
                                 HashSet<String> exclude)
	throws Exception {
        return makeTypeSelect(new ArrayList(), request, ARG_TYPE,"",includeAny, selected,
                              checkAddOk, exclude,false);
    }

    /**
     * _more_
     *
     * @param items _more_
     * @param request _more_
     * @param includeAny _more_
     * @param selected _more_
     * @param checkAddOk _more_
     * @param exclude _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String makeTypeSelect(List items, Request request,
				 String arg, String attrs,
                                 boolean includeAny, String selected,
                                 boolean checkAddOk, HashSet<String> exclude,boolean groupOnly)
	throws Exception {
	if(items==null) items = new ArrayList();

        for (TypeHandler typeHandler : getTypeHandlers()) {
	    if(groupOnly && !typeHandler.isGroup()) continue;

            if (typeHandler.isAnyHandler() && !includeAny) {
                continue;
            }
            if (exclude != null) {
                if (exclude.contains(typeHandler.getType())) {
                    continue;
                }
            }
            if ( !typeHandler.getForUser()) {
                continue;
            }

            if (checkAddOk && !typeHandler.canBeCreatedBy(request)) {
                continue;
            }
            //            System.err.println("type: " + typeHandler.getType() + " label:" + typeHandler.getLabel());
            items.add(new TwoFacedObject(typeHandler.getLabel(),
                                         typeHandler.getType()));
        }

        return HtmlUtils.select(arg, items, selected,attrs);
    }



    /**
     * _more_
     *
     * @param request The request
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<TypeHandler> getTypeHandlers(Request request)
	throws Exception {
        TypeHandler       typeHandler = getTypeHandler(request);
        List<TypeHandler> tmp         = new ArrayList<TypeHandler>();
        if ( !typeHandler.isAnyHandler()) {
            tmp.add(typeHandler);

            return tmp;
        }

        //For now don't do the db query to find the type handlers
        return getTypeHandlers();
    }

    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List getDefaultCategorys() throws Exception {
        if (categoryList != null) {
            return categoryList;
        }
        Statement stmt = getDatabaseManager().select(
						     SqlUtil.distinct(Tables.ENTRIES.COL_DATATYPE),
						     Tables.ENTRIES.NAME, new Clause[] {});
        String[] types =
            SqlUtil.readString(getDatabaseManager().getIterator(stmt), 1);
        List      tmp  = new ArrayList();
        Hashtable seen = new Hashtable();
        for (TypeHandler typeHandler : getTypeHandlers()) {
            if (typeHandler.hasDefaultCategory()
		&& (seen.get(typeHandler.getDefaultCategory()) == null)) {
                tmp.add(typeHandler.getDefaultCategory());
                seen.put(typeHandler.getDefaultCategory(), "");
            }
        }

        for (int i = 0; i < types.length; i++) {
            if ((types[i] != null) && (types[i].length() > 0)
		&& (seen.get(types[i]) == null)) {
                tmp.add(types[i]);
            }
        }

        tmp.add(0, "");

        return categoryList = tmp;
    }



    public String getMimeType(Request request, Entry entry) {
	String mimetype = entry.getTypeHandler().getTypeProperty("mimetype",null);
	if(mimetype!=null) return mimetype;
	return getMimeTypeFromSuffix(IOUtil.getFileExtension(entry.getResource().getPath()));
    }

    /**
     * _more_
     *
     * @param suffix _more_
     *
     * @return _more_
     */
    public String getMimeTypeFromSuffix(String suffix) {
        String type = (String) mimeTypes.get(suffix);
        if (type == null) {
            if (suffix.startsWith(".")) {
                suffix = suffix.substring(1);
            }
            type = (String) mimeTypes.get(suffix);
        }
        if (type == null) {
            type = "unknown";
        }

        return type;
    }





    /**
     * _more_
     */
    public void setLocalFilePaths() {
        localFilePaths = (List<File>) Misc.toList(
						  IOUtil.toFiles(
								 (List<String>) Utils.split(
											    getProperty(PROP_LOCALFILEPATHS, ""), "\n", true, true)));
        //Add the ramadda dir as well
        localFilePaths.add(0, getStorageManager().getRepositoryDir());
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List<File> getLocalFilePaths() {
        return localFilePaths;
    }


    /**
     * _more_
     *
     * @param fieldValue _more_
     * @param namesFile _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getFieldDescription(String fieldValue, String namesFile)
	throws Exception {
        return getFieldDescription(fieldValue, namesFile, null);
    }



    /**
     * _more_
     *
     * @param namesFile _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Properties getFieldProperties(String namesFile) throws Exception {
        if (namesFile == null) {
            return null;
        }
        Properties names = (Properties) namesHolder.get(namesFile);
        if (names == null) {
            try {
                names = new Properties();
                loadProperties(names, namesFile);
                namesHolder.put(namesFile, names);
            } catch (Exception exc) {
                getLogManager().logError("err:" + exc, exc);

                throw exc;
            }
        }

        return names;
    }


    /**
     * _more_
     *
     * @param fieldValue _more_
     * @param namesFile _more_
     * @param dflt _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getFieldDescription(String fieldValue, String namesFile,
                                      String dflt)
	throws Exception {
        if (namesFile == null) {
            return dflt;
        }
        String s = (String) getFieldProperties(namesFile).get(fieldValue);
        if (s == null) {
            return dflt;
        }

        return s;
    }


    /**
     * _more_
     *
     * @param entries _more_
     */
    public void checkNewEntries(final Request request, final List<Entry> entries) {
	final boolean debug = false;
	if(debug) System.err.println("checkNewEntries:" + entries);
        Misc.run(new Runnable() {
		public void run() {
		    for (EntryChecker entryMonitor : getEntryCheckers()) {
			if(debug) System.err.println("\tentryMonitor:" + entryMonitor);
			entryMonitor.entriesCreated(request, entries);
		    }
		}
	    });
    }

    /**
     *
     * @param entries _more_
     */
    public void checkMovedEntries(final List<Entry> entries) {
        Misc.run(new Runnable() {
		public void run() {
		    for (EntryChecker entryMonitor : getEntryCheckers()) {
			entryMonitor.entriesMoved(entries);
		    }
		}
	    });
    }

    /**
     * _more_
     *
     * @param ids _more_
     */
    public void checkDeletedEntries(final Request request, final List<String> ids) {
        Misc.run(new Runnable() {
		public void run() {
		    for (EntryChecker entryMonitor : getEntryCheckers()) {
			entryMonitor.entriesDeleted(ids);
		    }
		}
	    });
    }


    /**
     * _more_
     *
     * @param entries _more_
     */
    public void checkModifiedEntries(final Request request, final List<Entry> entries) {
        Misc.run(new Runnable() {
		public void run() {
		    for (EntryChecker entryMonitor : getEntryCheckers()) {
			entryMonitor.entriesModified(request, entries);
		    }
		}
	    });
    }


    /**
     *
     * @return _more_
     */
    public List<EntryChecker> getEntryCheckers() {
        return entryMonitors;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public static XmlEncoder getEncoder() {
        XmlEncoder xmlEncoder = new XmlEncoder();
        xmlEncoder.addClassPatternReplacement("ucar.unidata.repository",
					      "org.ramadda.repository");
        xmlEncoder.addClassPatternReplacement(
					      "ucar.unidata.repository.data.Catalog",
					      "org.ramadda.geodata.thredds.Catalog");

        return xmlEncoder;
    }

    /**
     * _more_
     *
     * @param object _more_
     *
     * @return _more_
     */
    public static String encodeObject(Object object) {
        return getEncoder().toXml(object, false);
    }

    /**
     * Decode the XML into an Object
     *
     * @param xml  the XML spec
     *
     * @return the Object defined by the XML
     *
     * @throws Exception  problem decoding
     */
    public static Object decodeObject(String xml) throws Exception {
        return getEncoder().toObject(xml);
    }

    /**
     * Get the python libraries
     *
     * @return  the libraries
     */
    public List<String> getPythonLibs() {
        return getPluginManager().getPythonLibs();
    }

    /**
     * Print the message
     *
     * @param msg the message
     */
    public static void println(String msg) {
        System.err.println(msg);
    }

    /**
     * Get the system message
     *
     * @return  the message
     */
    public String getSystemMessage() {
	return getProperty(PROP_SYSTEM_MESSAGE, "");
    }

    /**
     * Get the system message for the request
     *
     * @param request  the request
     *
     * @return  the message
     */
    public String getSystemMessage(Request request) {
        return getSystemMessage();
    }

    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean isStreetviewEnabled() throws Exception {
        ImageOutputHandler imageOutputHandler =
            (ImageOutputHandler) getOutputHandler(
						  ImageOutputHandler.OUTPUT_PLAYER);

        return imageOutputHandler.isStreetviewEnabled();
    }

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    private void trustAllCertificates() throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[] {
	    new X509TrustManager() {
		public java.security.cert.X509Certificate[] getAcceptedIssuers() {
		    return new X509Certificate[0];
		}
		public void checkClientTrusted(
					       java.security.cert.X509Certificate[] certs,
					       String authType) {}
		public void checkServerTrusted(
					       java.security.cert.X509Certificate[] certs,
					       String authType) {}
	    } };

        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(
							  sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(
							  new HostnameVerifier() {
							      public boolean verify(String string, SSLSession ssls) {
								  return true;
							      }
							  });
        } catch (Exception e) {}

    }



}


