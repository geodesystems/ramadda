/**
   Copyright (c) 2008-2025 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/


package org.ramadda.repository;


import org.ramadda.util.HttpFormEntry;


import org.ramadda.repository.admin.Admin;
import org.ramadda.repository.admin.AdminHandler;
import org.ramadda.repository.admin.MailManager;
import org.ramadda.repository.auth.AccessException;
import org.ramadda.repository.auth.AccessManager;
import org.ramadda.repository.auth.AuthorizationMethod;
import org.ramadda.repository.auth.SessionManager;
import org.ramadda.repository.auth.User;
import org.ramadda.repository.auth.UserManager;
import org.ramadda.repository.auth.AuthManager;
import org.ramadda.repository.database.DatabaseManager;
import org.ramadda.repository.database.Tables;
import org.ramadda.repository.ftp.FtpManager;
import org.ramadda.repository.harvester.HarvesterManager;
import org.ramadda.repository.job.JobManager;
import org.ramadda.repository.map.MapManager;
import org.ramadda.repository.metadata.AdminMetadataHandler;
import org.ramadda.repository.metadata.ContentMetadataHandler;
import org.ramadda.repository.metadata.Metadata;
import org.ramadda.repository.metadata.MetadataManager;
import org.ramadda.repository.monitor.MonitorManager;
import org.ramadda.repository.output.CalendarOutputHandler;
import org.ramadda.repository.output.CsvOutputHandler;
import org.ramadda.repository.output.HtmlOutputHandler;
import org.ramadda.repository.output.ImageOutputHandler;
import org.ramadda.repository.output.JsonOutputHandler;
import org.ramadda.repository.output.OutputHandler;
import org.ramadda.repository.output.OutputType;
import org.ramadda.repository.output.WikiManager;
import org.ramadda.repository.output.XmlOutputHandler;
import org.ramadda.repository.output.ZipOutputHandler;
import org.ramadda.repository.search.SearchManager;

import org.ramadda.data.services.PointOutputHandler;

import org.ramadda.repository.server.RepositoryServlet;
import org.ramadda.repository.type.Column;
import org.ramadda.repository.type.DataTypes;
import org.ramadda.repository.type.GroupTypeHandler;
import org.ramadda.repository.type.ProcessFileTypeHandler;
import org.ramadda.repository.type.GenericTypeHandler;
import org.ramadda.repository.type.TypeHandler;

import org.ramadda.repository.util.SelectInfo;
import org.ramadda.repository.util.ServerInfo;

import org.ramadda.service.Service;
import org.ramadda.util.CategoryBuffer;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.XmlUtils;
import org.ramadda.util.NamedBuffer;
import org.ramadda.util.IO;
import org.ramadda.util.S3File;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.MyTrace;
import org.ramadda.util.TTLCache;
import org.ramadda.util.seesv.Seesv;


import org.ramadda.util.PropertyProvider;
import org.ramadda.util.StreamEater;
import org.ramadda.util.TTLObject;
import org.ramadda.util.Utils;
import org.ramadda.util.geo.GeoUtils;
import java.sql.Connection;
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


import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;


import org.json.*;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.*;
import java.awt.image.BufferedImage;
import javax.imageio.*;

import java.io.*;
import java.util.zip.*;
import java.nio.charset.Charset;
import java.lang.reflect.Constructor;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.HttpURLConnection;
import java.net.URLConnection;

import java.security.cert.X509Certificate;
import javax.net.ssl.*;

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

/**
 * The main RAMADDA class.
 */
@SuppressWarnings("unchecked")
public class Repository extends RepositoryBase implements RequestHandler,
							  PropertyProvider {

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
    private static final PointOutputHandler dummyField5ToForceCompile =
        null;


    /** dummy field 6 */
    private static final org.ramadda.repository.job.JobManager dummyField6ToForceCompile =
        null;

    private static final org.ramadda.data.docs.TabularOutputHandler dummyField7 =
        null;

    private static final org.ramadda.util.seesv.Seesv dummyField7ToForceCompile =
        null;

    /** Cache resources property */
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
                       OutputType.TYPE_OTHER, "",

                       "fas fa-save");

    public static final OutputType OUTPUT_EXTEDIT =
        new OutputType("Extended Edit", "repository.extedit",
                       OutputType.TYPE_NONE, "",
                       ICON_EDIT);    

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

    /** Entry Listing OutputType */
    public static final OutputType OUTPUT_ENTRYLISTING =
        new OutputType("Entry Listing", "repository.entrylisting",
                       OutputType.TYPE_OTHER, "",
                       ICON_FILELISTING);

    public static final OutputType OUTPUT_CREATETYPE =
        new OutputType("Create Entry Type", "repository.createtype",
                       OutputType.TYPE_OTHER, "",
                       "/icons/newtype.png");    


    /** the stand alone server */
    private Object standAloneServer;


    /** The UserManager */
    private UserManager userManager;

    private AuthManager authManager;    

    /** The MonitorManager */
    private MonitorManager monitorManager;

    /** The SessionManager */
    private SessionManager sessionManager;

    private JobManager jobManager;

    /** The WikiManager */
    private WikiManager wikiManager;

    /** The LogManager */
    private LogManager logManager;

    /** The EntryManager */
    private EntryManager entryManager;

    private ExtEditor extEditor;

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

    /** The LLMManager */
    private LLMManager llmManager;    

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

    private Properties mimeTypes;

    private String installPassword;


    private Properties localProperties = new Properties();

    private Properties pluginProperties = new Properties();

    private Properties cmdLineProperties = new Properties();

    private Properties coreProperties = new Properties();

    private Map<String, String> systemEnv;

    private TTLObject<Properties> dbProperties;

    private Properties dbPropertiesDummy = new Properties();    

    private long baseTime = System.currentTimeMillis();

    ucar.unidata.util.SocketConnection dummyConnection;

    private List<String> sqlLoadFiles = new ArrayList<String>();

    private List<EntryChecker> entryMonitors = new ArrayList<EntryChecker>();

    private List<String[]>httpHeaders = new ArrayList<String[]>();

    private String dumpFile;

    private String startupScript;

    private Date startTime = new Date();

    private HashSet blacklist;

    private List<String> blacklistList;    

    private Hashtable<String, TypeHandler> typeHandlersMap =
        new Hashtable<String, TypeHandler>();

    private List<TypeHandler> allTypeHandlers = new ArrayList<TypeHandler>();

    private List<OutputHandler> outputHandlers =
        new ArrayList<OutputHandler>();

    private Hashtable<String, OutputHandler> outputHandlerMap =
        new Hashtable<String, OutputHandler>();

    private Hashtable<String, OutputType> outputTypeMap =
        new Hashtable<String, OutputType>();

    private Hashtable resources = new Hashtable();

    private Hashtable namesHolder = new Hashtable();

    private List<User> cmdLineUsers = new ArrayList();

    String[] args;

    public static boolean debug = false;

    public static boolean debugSession = false;

    private GroupTypeHandler groupTypeHandler;

    private TypeHandler fileTypeHandler;    

    private List categoryList = null;

    private List<String> htdocRoots = new ArrayList<String>();

    private int htdocsCacheSize = 0;

    private boolean cacheHtdocs = true;

    private static final int HTDOCS_CACHE_LIMIT = 10_000_000;

    private Hashtable<String, byte[]> htdocsCache = new Hashtable<String,
	byte[]>();

    private Hashtable<String, String> htdocsPathCache = new Hashtable<String,
	String>();

    private List<File> localFilePaths = new ArrayList<File>();

    private HttpClient httpClient;


    private boolean repositoryInitialized = false;

    private boolean isActive = true;

    private boolean readOnly = false;

    private boolean generateEntryDocs = false;
    
    private PrintWriter entryDocsWriter;

    private List<String> dropTables = new ArrayList<String>();
    
    private boolean doCache = true;

    private boolean adminOnly = false;

    private boolean requireLogin = false;

    private boolean allSsl = false;

    private boolean sslIgnore = false;

    private boolean cacheResources = false;

    private String repositoryName = "Repository";

    private String repositoryDescription = "";

    private boolean logActivityToFile = true;

    private boolean logActivityToDatabase = false;    

    private boolean downloadOk = true;

    private boolean minifiedOk = true;

    private boolean acceptRobots = true;

    private boolean acceptGoogleBot = true;        

    private boolean commentsEnabled  =false;

    private boolean useFixedHostName = false;

    private boolean corsOk = true;

    private boolean streamOutput = false;

    private boolean cdnOk = false;

    private boolean enableHostnameMapping = true;

    private String language = "";

    private String languageDefault = "";

    private boolean runningStandAlone = false;

    private int overridePort = -1;

    private boolean ignoreSSL = false;

    private int defaultMaxEntries=1000;

    private HashSet<String> scriptPaths = new HashSet<String>();    

    public Repository() throws Exception {}

    public Repository(Repository parentRepository, String[] args, int port)
	throws Exception {
        super(port);
        this.parentRepository = parentRepository;
        init(args, port);
    }

    public Repository(String[] args, int port) throws Exception {
        this(null, args, port);
    }

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

    public synchronized String getInstallPassword() throws Exception {
        if ( !Utils.stringDefined(installPassword)) {
            installPassword =
                getRepository().getProperty(PROP_INSTALL_PASSWORD,
                                            (String) null);
	    if ( !Utils.stringDefined(installPassword)) {
                installPassword = Utils.generatePassword(6);
	    }
        }
	//Generate an install password
	File install = new File(
				IOUtil.joinDir(
					       getStorageManager().getRepositoryDir(),
					       "install.properties"));
	if ( !install.exists()) {
	    System.err.println("RAMADDA: install password created in: " + install);
	    System.err.println("RAMADDA: install password: "
			       + installPassword);
	    StringBuilder sb = new StringBuilder();
	    sb.append("#This is a generated password used in the install process\n");
	    sb.append(PROP_INSTALL_PASSWORD + "=" + installPassword + "\n\n");
	    sb.append("#If you have a certificate for https access installed uncomment and set these property values\n");
	    sb.append("#ramadda.ssl.password=<the password>\n");
	    sb.append("#ramadda.ssl.keypassword=<the keystore password>\n");
	    sb.append("#ramadda.ssl.port=443\n");
	    sb.append("\n");	    
	    sb.append("#You can override the location of the keystore. The default is: <this directory>/keystore.jks\n");
	    sb.append("#ramadda.ssl.keystore=alternative path to keystore\n");
	    sb.append("\n");	    
	    sb.append("#If you want to disable ssl set this to true\n");
	    sb.append("#ramadda.ssl.ignore=true\n");
	    sb.append("\n\n");	    
	    sb.append("#If you need to reset the admin password then uncomment the below\n");
	    sb.append("#ramadda.admin=admin:some_password\n");
	    sb.append("\n\n");	    

	    try (FileOutputStream fos = new FileOutputStream(install)) {
		IOUtil.write(fos, sb.toString());
            }
   

        }

        return installPassword;
    }


    private void writeConfigFiles() throws Exception {
	File file;
	file = new File(IOUtil.joinDir(getStorageManager().getRepositoryDir(), "repository.properties"));
	if ( !file.exists()) {
	    String properties = getStorageManager().readUncheckedSystemResource("/org/ramadda/repository/resources/init.repository.properties");
	    try (FileOutputStream fos = new FileOutputStream(file)) {
		IOUtil.write(fos, properties);
	    }
	}
	file = new File(IOUtil.joinDir(getStorageManager().getRepositoryDir(), "config.properties"));
	if ( !file.exists()) {
	    String properties = getStorageManager().readUncheckedSystemResource("/org/ramadda/repository/resources/config.properties");
	    try (FileOutputStream fos = new FileOutputStream(file)) {
		IOUtil.write(fos, properties);
	    }
	}	

    }

    public List<RepositoryManager> getRepositoryManagers() {
        return repositoryManagers;
    }

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

    public void setPort(int value) {
        super.setPort(value);
        overridePort = value;
    }

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

    public Object getStandAloneServer() {
        return standAloneServer;
    }

    public void setRunningStandalone(boolean v) {
	runningStandAlone = v;
    }

    public boolean isChild() {
        return parentRepository != null;
    }

    public boolean isMaster() {
        //For now always enable this
        return true;
        //        return getProperty(LocalRepositoryManager.PROP_MASTER_ENABLED,false);
    }

    private void setParentRepository(Repository parent) {
        this.parentRepository = parent;
    }

    public Repository getParentRepository() {
        return parentRepository;
    }

    public List<Repository> getChildRepositories() {
        return new ArrayList<Repository>(childRepositories);
    }

    public void removeChildRepository(Repository childRepository)
	throws Exception {
        childRepositories.remove(childRepository);
    }

    public void addChildRepository(Repository childRepository)
	throws Exception {
        childRepositories.add(childRepository);
        childRepository.setParentRepository(this);

        int sslPort = getHttpsPort();
        if (sslPort > 0) {
            childRepository.setHttpsPort(sslPort);
        }
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public boolean doCache() {
        //Don't cache even when we're in readonly mode
        //        if (readOnly) {
        //            return false;
        //        }
        return doCache;
    }

    public boolean getActive() {
        return isActive;
    }

    public boolean getIsInitialized() {
        return repositoryInitialized;
    }

    public void setActive(boolean v) {
        isActive = v;
    }

    public void close() throws Exception {
        shutdown();
    }

    public boolean getShutdownEnabled() {
        return standAloneServer != null;
    }

    public boolean isRunningStandAlone() {
        return standAloneServer != null;
    }

    public void shutdown() {
        try {
            if ( !isActive) {
                return;
            }
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
            authManager            = null;	    
            monitorManager         = null;
            sessionManager         = null;
            wikiManager            = null;
            jobManager             = null;
            logManager             = null;
            entryManager           = null;
            commentManager         = null;
            associationManager     = null;
            searchManager          = null;
            llmManager          = null;	    
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

    public void setHttpsPort(int port) {
        super.setHttpsPort(port);
        reinitializeRequestUrls();
    }

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
                blacklistList = new ArrayList<String>();		
                for (String ip :
			 Utils.split(IOUtil.readContents(blacklistFile), "\n",
				     true, true)) {
		    if(ip.endsWith("*")) {
			blacklistList.add(ip.replace("*",""));
		    } else {			 
			blacklist.add(ip);
		    }
                }
            } catch (Exception exc) {
                System.err.println("Error reading blacklist file:" + exc);
            }
        }


        //This depends on the html templates which depends on the 
        getMetadataManager().loadMetadataHandlers(getPluginManager());

        clearAllCaches();
        StringBuilder statusMsg =
            new StringBuilder("RAMADDA: started at:" + new Date());
        statusMsg.append("\n");
        statusMsg.append("RAMADDA: home dir: "
                         + getStorageManager().getRepositoryDir());

        statusMsg.append("  version: "
                         + RepositoryUtil.getVersion());
        statusMsg.append("  build date: "
                         + getProperty(PROP_BUILD_DATE, "N/A"));
        statusMsg.append("  java version: "
                         + getProperty(PROP_JAVA_VERSION, "N/A"));
	statusMsg.append(getDatabaseManager().getStatusMessage());
	statusMsg.append("\n");
        statusMsg.append("RAMADDA: running on port:" + getPort() + " "
                         + (isSSLEnabled(null)
                            ? "SSL port:" + getHttpsPort()
                            : " SSL not enabled"));



	if(!getAdmin().getInstallationComplete()) {
	    statusMsg.append("\n");
	    statusMsg.append("RAMADDA: to complete the installation go to: http://localhost:" + getPort());
	    statusMsg.append("\n");
	}



        getLogManager().logInfoAndPrint(statusMsg.toString());

        if (getProperty("ramadda.beep", false)) {
            Toolkit.getDefaultToolkit().beep();
            Misc.sleep(200);
            Toolkit.getDefaultToolkit().beep();
        }




        String script = (startupScript != null)
	    ? startupScript
	    : getScriptPath("ramadda.startupscript");
        if (Utils.stringDefined(script)) {
            try {
		addScriptPath(script);
		makeProcessBuilder(Utils.split(script," ", true,true)).start();
            } catch (Exception exc) {
                System.err.println("Error running startup script:" + script+ "\n" + exc);
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

        if (getProperty("ramadda.debug.stderr", false)) {
            IO.debugStderr();
        }

	for(String path: Utils.split(getProperty("ramadda.programs",""),",",true,true)) {
	    addScriptPath(path);
	}

	//	getMetadataManager().applySchemaChanges();



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
        IO.close(inputStream);
    }

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
	    if(arg.equals("-entrydocs")) {
		generateEntryDocs = true;
		continue;
	    }

            if (getPluginManager().checkFile(arg)) {
                continue;
            }
            if (arg.endsWith(".properties")) {
                loadProperties(cmdLineProperties, arg);
	    } else if(arg.equals("-dropdbtable")) {
		//Only for the db plugin tables
		String table =   args[i + 1];
                i++;
		dropTables.add(table);
            } else if (arg.equals("-dump")) {
                dumpFile = args[i + 1];
                i++;
            } else if (arg.equals("-installpassword")) {
                installPassword = args[i + 1];
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
	writeConfigFiles();

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

   

    public int getDefaultMaxEntries() {
	return defaultMaxEntries;
    }

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

    public HttpClient getHttpClient() {
        return httpClient;
    }

    protected void initServer() throws Exception {
	if(Repository.debugInit)   System.err.println("Repository.initServer");
        getDatabaseManager().init();

	for(String table: dropTables) {
	    table = "db_" +table;
	    System.err.println("RAMADDA: drop table:" + table);
	    String sql = "drop table " + table;
	    getDatabaseManager().executeAndClose(sql);
	}
	


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
            IO.close(fos);
        }

        HU.setBlockHideShowImage(getIconUrl(ICON_MINUS),
				 getIconUrl(ICON_PLUS));
        HU.setInlineHideShowImage(getIconUrl(ICON_MINUS),
				  getIconUrl(ICON_PLUS));

        getLogManager().logInfo("RAMADDA started");


        MyTrace.call2("Repository.loadResources");
	getRegistryManager().checkApi();



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


        if (getParentRepository() == null) {
            GeoUtils.setGoogleKey(getProperty("google.key", (String) null));
            GeoUtils.setGeocodeioKey(getProperty("geocodeio.key", (String) null));
            GeoUtils.setHereKey(getProperty("here.key", (String) null));	    
            IO.setCacheDir(getStorageManager().getRepositoryDir());
            Seesv.setTmpCacheDir(getStorageManager().getCacheDir().getDir());
        }


	//create some of them to prevent the slight possiblitity of race conditions and to start them up if needed
	getAuthManager();
	getUserManager();
	getSessionManager();	
	getMonitorManager();



	if(Repository.debugInit)   System.err.println("Repository.initServer:done");

	if(generateEntryDocs) {
	    entryDocsWriter = new PrintWriter(new FileOutputStream("entrydocs.html"));
	    entryDocsWriter.println("[ht::head {Entry Types}]\n<%nochildlist%>\n:navleft\n+center\n:pagesearch selector=.entrydoc\n-center\n");
	    entryDocsWriter.flush();
	    PrintWriter pw = entryDocsWriter;
	    Request request = getAdminRequest();
	    for(EntryManager.SuperType superType:getEntryManager().getCats(false)) {
		boolean didSuper = false;
		for(EntryManager.Types types: superType.getList()) {
		    boolean didSub = false;
		    for(TypeHandler typeHandler: types.getList()) {
			if(!typeHandler.canCreate(request))
			    continue;
			if(!didSuper) {
			    didSuper = true;
			    pw.println("<div class=type-group-container><div class='type-group-header'>" + superType.getName()+"</div><div class=type-group>");
			}
			if(!didSub) {
			    didSub=true;
			    pw.println("<div style='font-size:140%;text-decoration:underline;'>" + types.getName()+"</div><div style='margin-left:40px;'>");
			}
			writeEntryDocs(typeHandler);
		    }
		    if(didSub) {
			pw.println("</div>");
		    }
		}
		if(didSuper) {
		    pw.println("</div></div>");
		}
	    }
	    entryDocsWriter.println("[ht::foot]");
	    entryDocsWriter.flush();
	    entryDocsWriter.close();
	}


	
	if(!getProperty("ramadda.entrytype.fix",false)) {
	    getLogManager().logSpecial("Adding entry types to tables");
	    try {
		fixEntryTypes();
	    } catch(Exception exc) {
		getLogManager().logError("Error fixing entry types",exc);
	    }
	    writeGlobal("ramadda.entrytype.fix","true");
	}


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

    public void loadTypeHandlers() throws Exception {
        List<String> badFiles = new ArrayList<String>();
        String       theFile  = null;
        try {
            for (String file : getPluginManager().getTypeDefFiles()) {
                try {
		    long t1 = System.currentTimeMillis();
		    theFile = file;
		    if(!loadTypeHandlers(file)) continue;
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
                    Element root = XU.getRoot(file, getClass());
                    if (root == null) {
                        continue;
                    }
                    loadTypeHandlers(root, file, false,false);
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

	/*
	  run through the TypeHandlers to check if any need initialization
	  this occurs when we load a type handler which has a super type that has not
	  been loaded yet. This brute forces (thus the 10 iterations of the loop)
	*/
	for(int tries=0;tries<10;tries++) {
	    for(TypeHandler typeHandler: allTypeHandlers) {
		if(typeHandler.getNeedsToInitialize()) {
		    typeHandler.initTypeHandler();
		}
	    }
	}

	List<TypeHandler> goodOnes = new ArrayList<TypeHandler>();

	for(TypeHandler typeHandler: allTypeHandlers) {
	    if(typeHandler.getNeedsToInitialize()) {
		getLogManager().logError("TypeHandler could not be initialized:" + typeHandler);
	    } else {
		goodOnes.add(typeHandler);
	    }
	}
	allTypeHandlers = goodOnes;
    }

    /**
       This adds the typehandler type to the extra database tables
     */
    private void fixEntryTypes() throws Exception {
	int totalCnt = 0;
	Request request = getAdminRequest();
	for(TypeHandler typeHandler: allTypeHandlers) {
	    //	    System.out.println("type:" + typeHandler  +" " + typeHandler.haveDatabaseTable());
	    if(!typeHandler.haveDatabaseTable()) {
		continue;
	    }

	    String table = typeHandler.getTableName();
	    if (!Utils.stringDefined(table)) {
		continue;
	    }
	    int typeCnt = 0;
	    //	    if(table.toLowerCase().indexOf("taxonomy")<0) continue;
	    //	    System.err.println("Table:" + table);
	    Connection connection = getDatabaseManager().getConnection();
	    try {
		String what = SqlUtil.comma(table+"."+GenericTypeHandler.COL_ID,table+"."+GenericTypeHandler.COL_ENTRY_TYPE);
		Statement statement = SqlUtil.select(connection, what, Misc.newList(table), null,"",-1,0);

		ResultSet  results = statement.getResultSet();
		//		System.out.println(typeHandler.getType() +" " + table);
		while (results.next()) {
		    String id  = results.getString(1);
		    String type = results.getString(2);
		    //		    if(Utils.stringDefined(type)) continue;
		    try {
			Entry entry = getEntryManager().getEntry(request,id);
			if(entry==null) continue;
			totalCnt++;
			typeCnt++;
			getDatabaseManager().update(table,GenericTypeHandler.COL_ID,id,
						    new String[]{GenericTypeHandler.COL_ENTRY_TYPE},
						    new Object[] {entry.getTypeHandler().getType()});
		    } catch(Exception ignore1) {
			getLogManager().logError("Error fixing entry types:" + typeHandler,ignore1);
			return;
		    }
		}
		statement.close();
	    } catch(Exception exc) {
		getLogManager().logSpecial("Error processing entry type fix:" + typeHandler+" error:" +exc);
	    }
	    getDatabaseManager().closeConnection(connection);
	    if(typeCnt>0) {
		getLogManager().logSpecial("entry type update: " + typeHandler+" #"  + typeCnt);
	    }
	}
	if(totalCnt>0) {
	    getLogManager().logSpecial("entry type update: total changed="  + totalCnt);
	}
    }    

    private boolean loadTypeHandlers(String file) throws Exception     {
	file    = getStorageManager().localizePath(file);
	if (getPluginManager().haveSeen("types:" + file, false)) {
	    return false;
	}
	Element root = XU.getRoot(file, getClass());
	if (root == null) {
	    return false;
	    
	}
	loadTypeHandlers(root, file,false,false);
	getPluginManager().markSeen("types:" + file);
	return true;
	
    }
    

    public List<TypeHandler> loadTypeHandlers(Element root, String file, boolean overwrite, boolean debug)
	throws Exception {
        List children = XmlUtils.findChildren(root, TypeHandler.TAG_TYPE,"import");
        List<TypeHandler> typeHandlers = new ArrayList<TypeHandler>();
        if ((children.size() == 0)
	    && root.getTagName().equals(TypeHandler.TAG_TYPE)) {
            typeHandlers.add(loadTypeHandler(root, overwrite));
        } else {
            for (int i = 0; i < children.size(); i++) {
                Element node = (Element) children.get(i);
		if (XmlUtil.isTag(node,"import")) {
		    String resource = XU.getAttribute(node,"resource");
		    if(!resource.startsWith("/")) {
			resource  = new File(file).getParentFile().toString()+"/"+ resource;
		    }
		    loadTypeHandlers(resource);
		} else {
		    typeHandlers.add(loadTypeHandler(node, overwrite));
		}
            }
        }

        return typeHandlers;
    }

    public TypeHandler loadTypeHandler(Element entryNode, boolean overwrite)
	throws Exception {
        String classPath = XU.getAttribute(entryNode,
						TypeHandler.ATTR_HANDLER, (String) null);

        if (classPath == null) {
            String superType = XU.getAttribute(entryNode,
						    TypeHandler.ATTR_SUPER, (String) null);
            if (superType != null) {
                TypeHandler parent = getTypeHandler(superType);
                if (parent == null) {
                    throw new IllegalArgumentException(
						       "Cannot find parent type:" + superType);
                }
                classPath = parent.getClass().getName();
                //                        System.err.println ("Using parent class:" +  classPath +" " + XU.toString(entryNode));
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
                               + XU.toString(entryNode).replaceAll("\\s\\s+"," "));
            exc.printStackTrace();
            throw exc;
        }


    }

    private void loadOutputHandlers() throws Exception {
        for (String commandFile : getPluginManager().getAllFiles()) {
            if ( !commandFile.endsWith("services.xml")) {
                continue;
            }
            if (getPluginManager().haveSeen("services:" + commandFile)) {
                continue;
            }
            Element  root  = XU.getRoot(commandFile, getClass());

            NodeList nodes = XU.getElements(root, Service.TAG_SERVICE);

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
            Element root = XU.getRoot(file, getClass());
            if (root == null) {
                continue;
            }
            List children = XU.findChildren(root, TAG_OUTPUTHANDLER);
            for (int i = 0; i < children.size(); i++) {
                Element node = (Element) children.get(i);
                boolean required = XU.getAttribute(node, ARG_REQUIRED,
							true);
                try {
                    Class c =
                        Misc.findClass(XU.getAttributeFromTree(node,
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
						   + XU.toString(node));
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

    public Service makeService(Element node, boolean addToGlobals)
	throws Exception {
        Constructor ctor =
            Misc.findConstructor(
				 Misc.findClass(
						XU.getAttributeFromTree(
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


    public Repository getRepository() {
        return this;
    }


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

    protected SessionManager doMakeSessionManager() {
        return new SessionManager(this);
    }

    protected PageHandler doMakePageHandler() {
        return new PageHandler(this);
    }

    protected DateHandler doMakeDateHandler() {
        return new DateHandler(this);
    }

    protected WikiManager doMakeWikiManager() {
        return new WikiManager(this);
    }

    protected EntryManager doMakeEntryManager() {
        return new EntryManager(this);
    }

    protected CommentManager doMakeCommentManager() {
        return new CommentManager(this);
    }

    protected AssociationManager doMakeAssociationManager() {
        return new AssociationManager(this);
    }

    protected HarvesterManager doMakeHarvesterManager() {
        return new HarvesterManager(this);
    }

    protected ActionManager doMakeActionManager() {
        return new ActionManager(this);
    }

    protected StorageManager doMakeStorageManager() {
        return new StorageManager(this);
    }

    protected PluginManager doMakePluginManager() {
        return new PluginManager(this);
    }

    protected DatabaseManager doMakeDatabaseManager() {
        return new DatabaseManager(this);
    }

    protected FtpManager doMakeFtpManager() {
        return new FtpManager(this);
    }

    protected Admin doMakeAdmin() {
        return new Admin(this);
    }

    protected UserManager doMakeUserManager() {
        return new UserManager(this);
    }

    protected AuthManager doMakeAuthManager() {
        return new AuthManager(this);
    }    

    private Object getMutex = new Object();

    public UserManager getUserManager() {
        if (userManager == null) {
            userManager = doMakeUserManager();
        }

        return userManager;
    }

    public AuthManager getAuthManager() {
        if (authManager == null) {
            authManager = doMakeAuthManager();
        }
        return authManager;
    }    

    protected MonitorManager doMakeMonitorManager() {
        return new MonitorManager(this);
    }

    public MonitorManager getMonitorManager() {
        if (monitorManager == null) {
            monitorManager = doMakeMonitorManager();
        }

        return monitorManager;
    }

    public SessionManager getSessionManager() {
        if (sessionManager == null) {
            sessionManager = doMakeSessionManager();
            sessionManager.init();
        }

        return sessionManager;
    }

    protected ApiManager doMakeApiManager() {
        return new ApiManager(this);
    }

    public ApiManager getApiManager() {
        if (apiManager == null) {
            apiManager = doMakeApiManager();
        }

        return apiManager;
    }

    public PageHandler getPageHandler() {
        if (pageHandler == null) {
            pageHandler = doMakePageHandler();
        }

        return pageHandler;
    }

    public DateHandler getDateHandler() {

        if (dateHandler == null) {
            dateHandler = doMakeDateHandler();
        }

        return dateHandler;
    }

    public WikiManager getWikiManager() {
        if (wikiManager == null) {
            wikiManager = doMakeWikiManager();
        }

        return wikiManager;
    }
    public LogManager getLogManager() {
        if (logManager == null) {
            logManager = doMakeLogManager();
            logManager.init();
        }

        return logManager;
    }

    
    protected LogManager doMakeLogManager() {
        return new LogManager(this);
    }


    
    public JobManager getJobManager() {
        if (jobManager == null) {
            jobManager = doMakeJobManager();
        }

        return jobManager;
    }

    
    protected JobManager doMakeJobManager() {
        return new JobManager(this);
    }

    
    public EntryManager getEntryManager() {
        if (entryManager == null) {
            entryManager = doMakeEntryManager();
        }

        return entryManager;
    }

    
    public ExtEditor getExtEditor() {
        if (extEditor == null) {
            extEditor = new ExtEditor(this);
        }

        return extEditor;
    }

    
    public synchronized EntryUtil getEntryUtil() {
        if (entryUtil == null) {
            entryUtil = EntryUtil.newEntryUtil(getRepository());
        }

        return entryUtil;
    }



    
    public CommentManager getCommentManager() {
        if (commentManager == null) {
            commentManager = doMakeCommentManager();
        }

        return commentManager;
    }


    
    public AssociationManager getAssociationManager() {
        if (associationManager == null) {
            associationManager = doMakeAssociationManager();
        }

        return associationManager;
    }

    
    public HarvesterManager getHarvesterManager() {
        if (harvesterManager == null) {
            harvesterManager = doMakeHarvesterManager();
        }

        return harvesterManager;
    }


    
    public ActionManager getActionManager() {
        if (actionManager == null) {
            actionManager = doMakeActionManager();
        }

        return actionManager;
    }


    
    protected AccessManager doMakeAccessManager() {
        return new AccessManager(this);
    }




    
    public AccessManager getAccessManager() {
        if (accessManager == null) {
            accessManager = doMakeAccessManager();
        }

        return accessManager;
    }




    
    protected SearchManager doMakeSearchManager() {
        return new SearchManager(this);
    }

    protected LLMManager doMakeLLMManager() {
        return new LLMManager(this);
    }    




    
    public SearchManager getSearchManager() {
        if (searchManager == null) {
            searchManager = doMakeSearchManager();
        }
        return searchManager;
    }


    public LLMManager getLLMManager() {
        if (llmManager == null) {
            llmManager = doMakeLLMManager();
        }
        return llmManager;
    }    



    
    protected MapManager doMakeMapManager() {
        return new MapManager(this);
    }




    
    public MapManager getMapManager() {
        if (mapManager == null) {
            mapManager = doMakeMapManager();
        }

        return mapManager;
    }



    
    protected MetadataManager doMakeMetadataManager() {
        return new MetadataManager(this);
    }

    
    public MetadataManager getMetadataManager() {
        if (metadataManager == null) {
            metadataManager = doMakeMetadataManager();
        }

        return metadataManager;
    }



    
    protected RegistryManager doMakeRegistryManager() {
        return new RegistryManager(this);
    }

    
    protected MailManager doMakeMailManager() {
        return new MailManager(this);
    }


    
    protected LocalRepositoryManager doMakeLocalRepositoryManager() {
        return new LocalRepositoryManager(this);
    }




    
    public RegistryManager getRegistryManager() {
        if (registryManager == null) {
            registryManager = doMakeRegistryManager();
        }

        return registryManager;
    }

    
    public MailManager getMailManager() {
        if (mailManager == null) {
            mailManager = doMakeMailManager();
        }

        return mailManager;
    }

    
    public LocalRepositoryManager getLocalRepositoryManager() {
        if (localRepositoryManager == null) {
            localRepositoryManager = doMakeLocalRepositoryManager();
        }

        return localRepositoryManager;
    }



    
    public StorageManager getStorageManager() {
        if (storageManager == null) {
            storageManager = doMakeStorageManager();
            storageManager.init();
        }

        return storageManager;
    }

    
    public PluginManager getPluginManager() {
        if (pluginManager == null) {
            pluginManager = doMakePluginManager();
        }

        return pluginManager;
    }


    
    public DatabaseManager getDatabaseManager() {
        if (databaseManager == null) {
            databaseManager = doMakeDatabaseManager();
        }

        return databaseManager;
    }


    
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


    
    public Admin getAdmin() {
        if (admin == null) {
            admin = doMakeAdmin();
        }

        return admin;
    }


    public static final double VERSION = 1.0;

    
    private void updateToVersion1_0() throws Exception {}

    
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
            contents = IOUtil.readContents(path + "/files.txt", c,  (String) null);
            if (contents == null) {
                contents = IOUtil.readContents(path, c, (String) null);
                //                getLogManager().logInfoAndPrint("RAMADDA: resourceList (2):" + (contents == null?"NULL":contents.replaceAll("\n"," ")));
            } else {
                //                getLogManager().logInfoAndPrint("RAMADDA: resourceList (1):" + (contents == null?"NULL":contents.replaceAll("\n"," ")));
            }

            if (contents != null) {
                List<String> lines = Utils.split(contents, "\n", true, true);
                for (String file : lines) {
		    if(file.startsWith("#")) continue;
                    listing.add(path + "/" + file);
                }
            }
        }

        return listing;
    }



    
    public Date getStartTime() {
        return startTime;
    }

    
    public List<ImportHandler> getImportHandlers() {
        return getPluginManager().getImportHandlers();
    }




    
    private static final String USAGE_MESSAGE =
        "\nUsage: repository\n\t-installpassword <password>\n\n-home /path/to/ramadda/home\n\t-admin <admin name> <admin password>\n\t-port <http port>\n\t-Dname=value";

    
    protected void usage(String message) {
        throw new IllegalArgumentException(message + "\n" + USAGE_MESSAGE);
    }




    
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



    private static final Object CLEAR_CACHE_MUTEX =new Object();
    
    public void clearAllCaches() {
	synchronized(CLEAR_CACHE_MUTEX) {
	    List<RepositoryManager> managers = new ArrayList<RepositoryManager>();
	    managers.addAll(outputHandlers);
	    managers.addAll(allTypeHandlers);
	    managers.addAll(repositoryManagers);
	    for (RepositoryManager manager :managers) {
		manager.clearCache();
	    }
	    resources = new Hashtable();
	    try {
		readDatabaseProperties();
	    } catch (Exception exc) {
		getLogManager().logError("Error reading globals", exc);
	    }
	    TTLCache.clearCaches();
	}

    }


    
    public void clearCache() {
        getEntryManager().clearCache();
        getAccessManager().clearCache();
        categoryList = null;
    }


    
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

    
    protected void reinitializeRequestUrls() {
        synchronized (initializedUrls) {
            for (RequestUrl requestUrl : initializedUrls) {
                initRequestUrl(requestUrl);
            }
        }
    }



    
    @Override
    public String getHttpsUrl(String url) {
        return getHttpsUrl(null, url);
    }


    
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


    
    //    @Override
    public String getUrlPath(Request request, RequestUrl requestUrl) {
        if (requestUrl.getNeedsSsl()) {
            return httpsUrl(request, getUrlBase() + requestUrl.getPath());
        }

        return getUrlBase() + requestUrl.getPath();
    }

    public String getUrlPath(String path) {
        return getUrlBase() + path;
    }


    
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


    
    public void addOutputType(OutputType type) {
        outputTypeMap.put(type.getId(), type);
    }

    
    public OutputType findOutputType(String id) {
        if ((id == null) || (id.length() == 0)) {
            return OutputHandler.OUTPUT_HTML;
        }

        return outputTypeMap.get(id);
    }






    
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

        OutputHandler exteditHandler = new OutputHandler(getRepository(),
						      "Extended Edit") {
		public boolean canHandleOutput(OutputType output) {
		    return output.equals(OUTPUT_EXTEDIT);
		}
		public String toString() {
		    return "Extended Edit";
		}

		@Override
		public Result outputGroup(Request request, OutputType outputType,
					  Entry group, List<Entry> children)
                    throws Exception {
		    if (request.getUser().getAnonymous()) {
			return new Result("", "");
		    }
		    if ( !group.isDummy()) {
			return new Result("", "");
		    }
		    return getExtEditor().processEntryExtEdit(request,group, children);
		}
	    };
        exteditHandler.addType(OUTPUT_EXTEDIT);
        addOutputHandler(exteditHandler);


        OutputHandler snapshotHandler = new OutputHandler(getRepository(), "Entry Snapshotter") {
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



        OutputHandler entryListingHandler = new OutputHandler(getRepository(), "Entry Listing") {
		public boolean canHandleOutput(OutputType output) {
		    return output.equals(OUTPUT_ENTRYLISTING);
		}
		public void getEntryLinks(Request request, State state,
					  List<Link> links)
                    throws Exception {
		    if (entryListingOK(request)) {
			links.add(makeLink(request, state.getEntry(), OUTPUT_ENTRYLISTING));
		    }
		}


		public Result outputEntry(Request request, OutputType outputType,
					  Entry entry)
                    throws Exception {
		    return outputEntryListing(request, entry,
					     (List<Entry>) Misc.newList(entry));
		}

		@Override
		public Result outputGroup(Request request, OutputType outputType,
					  Entry group, List<Entry> children)
                    throws Exception {
		    return outputEntryListing(request, group, children);

		}

		public String toString() {
		    return "Entry listing handler";
		}

	    };
        entryListingHandler.addType(OUTPUT_ENTRYLISTING);
        addOutputHandler(entryListingHandler,true);


        OutputHandler createTypeHandler = new OutputHandler(getRepository(),
							     "Create Entry Type") {
		public boolean canHandleOutput(OutputType output) {
		    return output.equals(OUTPUT_CREATETYPE);
		}
		public void getEntryLinks(Request request, State state,
					  List<Link> links)
                    throws Exception {
		    if (getExtEditor().createTypeOK(request)) {
			links.add(makeLink(request, state.getEntry(),
					   OUTPUT_CREATETYPE));
		    }
		}


		public Result outputEntry(Request request, OutputType outputType,
					  Entry entry)
                    throws Exception {
		    return getExtEditor().outputCreateType(request, entry);
		}

		@Override
		public Result outputGroup(Request request, OutputType outputType,
					  Entry group, List<Entry> children)
                    throws Exception {
		    return getExtEditor().outputCreateType(request, group);
		}
		public String toString() {
		    return "Entry listing handler";
		}

	    };
        createTypeHandler.addType(OUTPUT_CREATETYPE);
        addOutputHandler(createTypeHandler);

        getUserManager().initOutputHandlers();

    }

    private boolean entryListingOK(Request request) {
	return request.getUser().getAdmin()
	    || ( !request.getUser().getAnonymous()
		 && getProperty(PROP_ENABLE_FILE_LISTING, false));
    }


    public Result outputEntryListing(Request request, Entry entry,  List<Entry> entries)
	throws Exception {
	if ( !entryListingOK(request)) {
	    throw new AccessException("Entry listing not enabled",
				      request);
	}
	SelectInfo select= new SelectInfo(request);
	select.setSyntheticOk(false);

	Request anon = getAnonymousRequest();
	StringBuilder sb = new StringBuilder();
	getPageHandler().entrySectionOpen(request, entry, sb, "Entry Listing");
	StringBuilder sb2 = new StringBuilder();
	boolean recurse = request.get("recurse",false);
	if(!recurse) {
	    String url = request.entryUrl(
					  getRepository().URL_ENTRY_SHOW, entry,
					  ARG_OUTPUT, OUTPUT_ENTRYLISTING.toString());
	    sb.append(HU.center(HU.href(HU.url(url,"recurse","true"),"Recurse Entry Listing")));
	}
	StringBuilder forAdmin = new StringBuilder();
	int []entryCnt={0};
	int []fileCnt={0};	


	long size = outputEntryListingInner(request, anon, select,entry,  entries, sb2,forAdmin,recurse,entryCnt,fileCnt,"");

	if(sb2.length()==0) {
	    sb.append(getPageHandler().showDialogNote("No entries available"));
	} else {
	    HU.addPageSearch(sb,".entry-listing",null,"Search");
	    sb.append("#Entries: " + entryCnt[0]);
	    sb.append("&nbsp;#Files: " + fileCnt[0]);	    
	    sb.append("&nbsp;Total size: " + getPageHandler().formatFileLength(size));

	    sb.append(HU.space(3));
	    sb.append("Anonymous user - ");
	    sb.append("&nbsp;<i title='No access' style='color:red' class='fa-solid fa-ban'></i>: private"); 
	    sb.append("&nbsp;<i title='Can access' style='color:green' class='fa-solid fa-circle'></i>: public");
	    sb.append("<br>");
	    sb.append(sb2);
	    /**
	    if (request.getUser().getAdmin()) {
		sb.append("<hr>");
		sb.append(getPageHandler().msgHeader("File Paths"));
		sb.append(forAdmin);
	    }
	    **/
	}
	getPageHandler().entrySectionClose(request, entry, sb);
	return getHtmlOutputHandler().makeLinksResult(request, msg("Entry Listing"), sb,
						      new OutputHandler.State(entry));
    }


    private long outputEntryListingInner(Request request, Request anon,SelectInfo select,Entry entry,
					 List<Entry> entries,StringBuilder sb, 
					 StringBuilder forAdmin,boolean recurse,
					 int []entryCnt,int []fileCnt,String indent) throws Exception {
	long size =  0;
	if(entryCnt[0]>5000) {
	    sb.append("<div>...</div>");
	    return size;
	}

	for (Entry child : entries) {
	    entryCnt[0]++;
	    Resource resource = child.getResource();
	    if (resource != null && resource.isFile()) {
		fileCnt[0]++;
		forAdmin.append(resource.getTheFile().toString());
		forAdmin.append(HU.br());
	    }

	    long size2=0;
	    StringBuilder sb2 = new StringBuilder();
	    if(recurse) {
		String indent2= indent+"&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";
		List<Entry> children= getEntryManager().getChildren(request, child,select);
		if(children.size()>0) {
		    size2+=outputEntryListingInner(request,anon,select,child,children, sb2,forAdmin,recurse,entryCnt,fileCnt,indent2);
		    size+=size2;
		}
	    }
	    
	    sb.append("<div class=entry-listing>");
	    sb.append(indent);

	    sb.append(getPageHandler().getEntryIconImage(request, child));
	    if(!getAccessManager().canDoView(anon,child)) {
		sb.append("&nbsp;<i title='private' style='color:red' class='fa-solid fa-ban'></i>");
	    } else {
		sb.append("&nbsp;<i title='public' style='color:green' class='fa-solid fa-circle'></i>");
	    }
	    sb.append("&nbsp;");
	    sb.append(getEntryManager().getEntryLink(request, child,false,""));
	    long fileSize =0;
	    if (resource != null && resource.isFile()) {
		fileSize = child.getResource().getFileSize();
		size+=fileSize;
		sb.append("&nbsp;");
		sb.append(getPageHandler().formatFileLength(fileSize));
	    }
	    if(size2!=0) {
		sb.append("&nbsp;total size: ");
		sb.append(getPageHandler().formatFileLength(size2+fileSize));
	    }


	    sb.append("</div>");
	    if(sb2.length()>0) {
		sb.append(sb2);
	    }

	}
	return size;
    }




    
    public void addEntryChecker(EntryChecker entryMonitor) {
        entryMonitors.add(entryMonitor);
    }


    public Result processXss(Request request) throws Exception {
	StringBuilder sb = new StringBuilder();
	sb.append("<div style='margin-left:20px;'>\n");
	int cnt = 1;
	for(String line: Utils.split(IOUtil.readContents("/org/ramadda/repository/resources/xss-payload-list.txt",getClass()),"\n",true,true)) {
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

        Result result = handleRequestInner(request);
        return result;
    }

    
    public static boolean propdebug = false;

    
    public int propcnt = 0;

    
    private boolean acceptRobots() {
        return acceptRobots;
    }
    
    private boolean acceptGoogleBot() {
        return  acceptGoogleBot;
    }

    
    public boolean getCommentsEnabled() {
	return commentsEnabled;
    }


    
    public Result getNoRobotsResult(Request request) throws Exception {
	if(request.getRequestPath().indexOf("robots.txt")>=0) {
	    return processRobotsTxt(request);
	}
	
        Result result = new Result("", new StringBuilder("no bots"));
        result.setResponseCode(Result.RESPONSE_UNAUTHORIZED);
        return result;
    }


    
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
            IO.close(fis);
        }
        IO.close(zos);

        return result;
    }



    
    private Result handleRequestInner(Request request) throws Exception {

        if (debugSession) {
            debugSession(request,
                         "RAMADDA.handleRequest:" + request.getRequestPath());
        }
        if (request.getIsRobot()) {
            if (!acceptRobots()) {
		//		System.err.println("robot not ok:" + request.getUserAgent());
		return getNoRobotsResult(request);
	    }
	    //	    System.err.println("robot ok:" + request.getUserAgent());
	}
	if(request.getIsGoogleBot()) {
	    if(!acceptGoogleBot()) {
		//		System.err.println("googlebot not ok:" + request.getUserAgent());
		return getNoRobotsResult(request);
	    }
	    //	    System.err.println("googlebot ok:" + request.getUserAgent());
            //Sleep a bit to slow the  bot down
	    //	    Misc.sleepSeconds(1);
	}
	

        if (blacklist != null) {
            String ip = request.getIpRaw();
	    if(ip!=null) {
		if (blacklist.contains(ip)) {
		    return makeBlockedResult(request,true);
		}
		if (blacklistList.size()>0) {
		    for(String prefix: blacklistList) {
			if(ip.startsWith(prefix)) {
			    return makeBlockedResult(request,true);
			}
		    }
		} 
	    }
        }

        //A hack  - should put this in a user-agent blacklist file sometime
        String userAgent = request.getUserAgent();
        if (userAgent != null) {
            if ((userAgent.indexOf("OpenVAS") >= 0)
		|| (userAgent.indexOf("GBN") >= 0)) {
                return makeBlockedResult(request,true);
            }
        }

        String requestPath = request.getRequestPath();
        //Check for scanners
        if (requestPath.endsWith(".php")) {
            return makeBlockedResult(request,true);
        }


        boolean debugMemory = false;
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
		sessionExc.printStackTrace();
		return result;
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
							     HU.hiddenBase64(ARG_REDIRECT, request.getUrl())));

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
                            HU.url(URL_SSLREDIRECT.toString(),
				   ARG_REDIRECT, redirectUrl);
                        result = new Result(url);
                    } else {
                        result = new Result("Error", sb);
			result.addHttpHeader(HU.HTTP_WWW_AUTHENTICATE, "Basic realm=\"ramadda\"");
                        result.setResponseCode(Result.RESPONSE_UNAUTHORIZED);
                    }

                    return result;
                }
            }

            if (!request.responseAsJson() && (request.getUser() != null) && request.getUser().getAdmin()) {
                sb.append(HU.pre(HU.strictSanitizeString(LogUtil.getStackTrace(inner))));
            }

            result = new Result(msg("Error"), sb);

            if (badAccess) {
                result.setResponseCode(Result.RESPONSE_UNAUTHORIZED);
                //                result.addHttpHeader(HU.HTTP_WWW_AUTHENTICATE,"Basic realm=\"repository\"");
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

        if (okToAddCookie && result != null
	    && (request.getSessionIdWasSet()
		|| (request.getSessionId() == null))) {
	    handleRequestCookie(request, result);
        }
        return result;
    }



    public void handleRequestCookie(Request request, Result result) {
	if(request.getCookieWasAdded()) return;	
	if (request.getSessionId() == null) {
	    if(!getSessionManager().addAnonymousCookie(request)) return;
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


    
    public String handleError(Request request, Throwable exc,
                              String message) {
        getLogManager().logError("Error:" + exc.getMessage(), exc);
        Throwable     inner = LogUtil.getInnerException(exc);
        StringBuilder sb    = new StringBuilder();
        sb.append(getPageHandler().showDialogError(message + "<br>"
						   + inner.getMessage()));
        if ((request.getUser() != null) && request.getUser().getAdmin()) {
            String stack = HU.pre(
				  HU.entityEncode(
						  LogUtil.getStackTrace(inner)));
            sb.append(HU.makeShowHideBlock("Stack", stack, false));
        }

        return sb.toString();
    }


    
    public Result makeBlockedResult(Request request, boolean sleep) {
        getLogManager().logRequest(request, Result.RESPONSE_BLOCKED);
	if(sleep)
	    Misc.sleepSeconds(10);
        Result r = new Result("", new StringBuilder("This IP address is blocked"));
        r.setResponseCode(Result.RESPONSE_NOTFOUND);
        return r;
    }


    
    public Result makeErrorResult(Request request, Throwable exc) {
        Throwable inner = LogUtil.getInnerException(exc);
        String    msg;
        if (inner instanceof RepositoryUtil.MissingEntryException) {
            msg = msgLabel("Entry not found")+ inner.getMessage();
        } else {
            msg = "Error:" + inner.getMessage();
        }
        return makeErrorResult(request, msg);
    }

    
    public Result makeErrorResult(Request request, String msg) {
	return makeErrorResult(request, msg, true);
    }


    public Result makeErrorResult(Request request, String msg,boolean decorate) {
	return makeErrorResult(request, msg, decorate,true);
    }


    public Result makeErrorResult(Request request, String msg,boolean decorate, boolean sanitize,boolean...asText) {	
	StringBuilder sb = new StringBuilder(decorate?makeErrorResponse(request, msg,sanitize):sanitize?HU.strictSanitizeString(msg):msg);
        Result        result = null;
        if (request.responseAsJson()) {
            result = new Result("", sb, JsonUtil.MIMETYPE);
            result.setShouldDecorate(false);
        } else if (request.responseAsXml()) {
            result = new Result("", sb, MIME_XML);
            result.setShouldDecorate(false);
        } else if (Utils.isTrue(asText,false) || request.responseAsText()) {
            result = new Result("", sb, MIME_TEXT);
            result.setShouldDecorate(false);
        } else {
	    StringBuilder tmp = new StringBuilder();
	    getPageHandler().sectionOpen(request, tmp,"Error",false);
	    tmp.append(sb);
	    getPageHandler().sectionClose(request, tmp);
            result = new Result(msg("Error"), tmp);
        }
	result.setResponseCode(Result.RESPONSE_INTERNALERROR);
        return result;
    }


    
    public Result makeOkResult(Request request, String msg) {
        StringBuilder sb = new StringBuilder(makeOkResponse(request, msg));

        return makeResponseResult(request, sb);
    }

    
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

    
    public String makeErrorResponse(Request request, String msg,boolean ...sanitize) {
	if(sanitize.length==0 || sanitize[0])
	    msg = HU.strictSanitizeString(msg);
        if (request.responseAsJson()) {
            return JsonUtil.mapAndQuote(Utils.makeListFromValues("error", msg));
        } else if (request.responseAsXml()) {
            return XU.tag(TAG_RESPONSE,
                               XU.attr(ATTR_CODE, CODE_ERROR), msg);
        } else if (request.responseAsText()) {
            return msg;
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(getPageHandler().showDialogError("An error has occurred") 
		      + HU.br() + msg);

            return sb.toString();
        }
    }


    
    public String makeOkResponse(Request request, String msg) {
        if (request.responseAsJson()) {
            return JsonUtil.mapAndQuote(Utils.makeListFromValues("ok", msg));
        } else if (request.responseAsXml()) {
            return XU.tag(TAG_RESPONSE,
                               XU.attr(ATTR_CODE, CODE_OK), msg);
        } else if (request.responseAsText()) {
            return msg;
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(getPageHandler().showDialogNote("An error has occurred") + ":" + HU.p() + msg);
            return sb.toString();
        }
    }


    public Result processGetLanguage(Request request) throws Exception {
	StringBuilder sb = getPageHandler().getLanguage(request.getString("language",""));
	if(sb==null) sb  = new StringBuilder();
	return new Result("", sb, MIME_TEXT);
    }


    
    public String debugPrefix() {
        return getUrlBase() + ": ";
    }

    
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

    private long callCnt=0;

    
    protected Result getResult(Request request) throws Exception {
        ApiMethod apiMethod = getApiManager().findApiMethod(request);
	//	System.err.println(request.getAbsoluteUrl(request.toString()));
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

	callCnt++;
	/*
	if((callCnt%10)==0) {
	    System.err.println("cnt:" +callCnt);
	}
	*/
        request.setApiMethod(apiMethod);
        apiMethod.incrNumberOfCalls();

        if ( !getAdmin().getInstallationComplete()) {
	    //Check for the favicon.ico request
	    String path       = request.getRequestPath();
	    if(path.equals("/favicon.ico")) return  processFavIcon(request);
            return getAdmin().doInstall(request);
        }

        if ( !getUserManager().isRequestOk(request)) {
            throw new AccessException(msg("You do not have permission to access this page"),request);
        }

        if ( !apiMethod.isRequestOk(request, this)) {
            throw new AccessException(msg("Incorrect access"), request);
        }
        Result result = null;
        try {
	    result = (Result) apiMethod.invoke(request);
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








    
    private Result checkForSslRedirect(Request request, ApiMethod apiMethod) {
        boolean debug      = false;
        boolean sslEnabled = isSSLEnabled(request);
        //check for the sub-repositories
        if (apiMethod.getRequest().startsWith("/repos/")) {
            return null;
        }
        if (debug) {
            System.err.println("checkForSslRedirect allSsl:" + allSsl
                               + " request secure:" + request.getSecure() +
			       " port:" + request.getServerPort());
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

    
    public Result makeResult(Request request, String path,
                             InputStream inputStream, String mimeType,
                             boolean cacheOk)
	throws Exception {
        return makeResult(request, path, inputStream, mimeType, cacheOk,
                          false);
    }

    
    public Result makeResult(Request request, String path,
                             InputStream inputStream, String mimeType,
                             boolean cacheOk, boolean gzipIt)
	throws Exception {
        String tail = IO.getFileTail(path);
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

        String mimeType =  getMimeTypeFromSuffix(path);
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
                        entry = getEntryManager().findEntryFromName(request, entry, toks.get(1));
                    }
                    if (entry == null) {
                        if ( !tryingOnePathAsAlias) {



			    StringBuilder sb =new StringBuilder(HU.center(getPageHandler().showDialogError("Could not find aliased entry:"+ HU.sanitizeString(alias))));

			    if(request.isAnonymous()) {
				String redirectUrl =   Utils.encodeBase64(request.getUrl());
				request.put(ARG_REDIRECT, redirectUrl);
				sb.append(getUserManager().makeLoginForm(request));
			    }
                            Result result = makeErrorResult(request,sb.toString(),false,false);
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


    public String applyPropertyMacros(String s) {
	StringBuilder sb = new StringBuilder();
	for(Utils.Macro macro:Utils.splitMacros(s)) {
	    if(macro.isText()) {
		sb.append(macro.getText());
	    } else {
		String id = macro.getId();
		String v = getProperty("property." + id,"missing:property." + id);
		sb.append(v);
	    }
	}
	return sb.toString();
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
        String userAgent = request.getHeaderArg(HU.HTTP_USER_AGENT);
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


    
    private void initRepositoryAttributes() {
        adminOnly             = getProperty(PROP_ACCESS_ADMINONLY, false);
	defaultMaxEntries     = getProperty("ramadda.defaultmaxentries",defaultMaxEntries);
        requireLogin          = getProperty(PROP_ACCESS_REQUIRELOGIN, false);
	alwaysHttps           = getProperty(PROP_ALWAYS_HTTPS, false);
        allSsl                = getProperty(PROP_ACCESS_ALLSSL, false);
        sslIgnore             = getProperty(PROP_SSL_IGNORE, false);
        cacheResources        = getProperty(PROP_CACHE_RESOURCES, false);
	cacheHtdocs           = getProperty(PROP_CACHE_HTDOCS, true);
	logActivityToFile     = getProperty("ramadda.logging.logactivityfile", true);
	logActivityToDatabase = getProperty("ramadda.logging.logactivitydatabase", false);	
        repositoryName = getProperty(PROP_REPOSITORY_NAME, repositoryName);
        repositoryDescription = getProperty(PROP_REPOSITORY_DESCRIPTION, "");
        language              = getProperty(PROP_LANGUAGE, "");
        languageDefault       = getProperty(PROP_LANGUAGE_DEFAULT, "en");
        downloadOk            = getProperty(PROP_DOWNLOAD_OK, true);
        minifiedOk            = getProperty(PROP_MINIFIED, true);
        acceptRobots          = !getProperty(PROP_ACCESS_NOBOTS, false);
        acceptGoogleBot       = !getProperty(PROP_ACCESS_NOGOOGLEBOT, false);	
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

    
    public boolean getRequireLogin() {
        return requireLogin;
    }

    
    public boolean getEnableHostnameMapping() {
        return enableHostnameMapping;
    }

    
    public boolean getDownloadOk() {
        return downloadOk;
    }

    
    public boolean getMinifiedOk() {
        return minifiedOk;
    }

    
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

    
    public boolean getCdnOk() {
        return cdnOk;
    }


    
    
    public boolean getStreamOutput () {
	return streamOutput;
    }




    
    public String getLanguage() {
        return language;
    }

    
    public String getLanguageDefault() {
        return languageDefault;
    }


    
    public String getRepositoryName() {
        return repositoryName;
    }


    
    public boolean getCacheResources() {
        return cacheResources;
    }



    
    public String getScriptPath(String name,String...dflts) {
	String path =  getLocalProperty(name, dflts!=null && dflts.length>0?dflts[0]:null);
	addScriptPath(path);
    	return path;
    }

    public void addScriptPath(String path) {
	if(Utils.stringDefined(path)) {
	    if(!scriptPaths.contains(path)) {
		scriptPaths.add(path);
		getLogManager().logInfo("RAMADDA: adding script path: "+path);
	    }
	}
    }

    public boolean isScriptOk(String path) {
	boolean ok= scriptPaths.contains(path);
	//	System.err.println("script ok:" +path +" " + ok);
	return ok;
    }

    public ProcessBuilder makeProcessBuilder(List<String> commands) {
	String command = commands.get(0);
	if(!isScriptOk(command)) {
	    throw new IllegalArgumentException("Error executing commands: given command is not in allowable list of commands:" + command);
	}
	return new ProcessBuilder(commands);
    }


    
    public String[] runCommands(List<String>commands) throws Exception {
        ProcessBuilder pb      = makeProcessBuilder(commands);
        Process        process = pb.start();
        InputStream    is      = process.getInputStream();
        InputStream    es      = process.getErrorStream();	
	String results =  IO.readInputStream(is);
        String error = IO.readInputStream(es);
	return new String[]{error,results};
    }


    public String getScriptPathFromTree(String name, String dflt) {
        String value = getScriptPath(name, (String) null);
        if (Utils.stringDefined(value)) {
            return value;
        }
        if (getParentRepository() != null) {
            return getParentRepository().getScriptPath(name, dflt);
        }

        return dflt;
    }




    
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


    public boolean getLocalProperty(String name, boolean dflt) {
	String v = getLocalProperty(name,null);
	if(v==null) return dflt;
	return v.trim().equals("true");
    }


    
    public String getProperty(String name) {
        return getPropertyValue(name, true);
    }


    
    public Properties getPluginProperties() {
        return pluginProperties;
    }


    
    public String getPropertyValue(String name, boolean checkDb) {
        return getPropertyValue(name, checkDb, false);
    }



    
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

    
    private boolean checkProperty(String prop, boolean needsToBeNonEmpty) {
        if (prop == null) {
            return false;
        }
        if (needsToBeNonEmpty && (prop.trim().length() == 0)) {
            return false;
        }

        return true;
    }




    
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

    
    public String getPropertiesListing() {
        StringBuilder sb = new StringBuilder();
        sb.append(getPropertiesListing(getDbProperties(), "Database"));
        sb.append(getPropertiesListing(localProperties, "Local"));
        //      sb.append(getPropertiesListing(pluginProperties,"Plugin"));
        //      sb.append(getPropertiesListing(coreProperties,"Core"));
        return sb.toString();
    }


    
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


    
    public String getProperty(String name, String dflt) {
        return getPropertyValue(name, dflt, true);
    }


    
    public String getPropertyValue(String name, String dflt,
                                   boolean checkDb) {
        String prop = getPropertyValue(name, checkDb);
        if (prop != null) {
            return prop;
        }

        return dflt;
    }




    
    public boolean getProperty(String name, boolean dflt) {
        String prop = getProperty(name);
        if (Utils.stringDefined(prop)) {
            boolean value =  Boolean.parseBoolean(prop.trim());
	    return value;
        }

        return dflt;
    }


    
    public int getProperty(String name, int dflt) {
        String prop = getPropertyValue(name, true, true);
        if (prop != null) {
            return Integer.parseInt(prop.trim());
        }

        return dflt;
    }


    
    public long getProperty(String name, long dflt) {
        String prop = getPropertyValue(name, true, true);
        if (prop != null) {
            return Long.parseLong(prop.trim());
        }

        return dflt;
    }

    
    public double getProperty(String name, double dflt) {
        String prop = getPropertyValue(name, true, true);
        if (prop != null) {
            return Double.parseDouble(prop.trim());
        }

        return dflt;
    }



    
    public boolean getGenerateEntryDocs() {
	return generateEntryDocs;
   }

    public boolean getDbProperty(String name, boolean dflt) {
        return Misc.getProperty(getDbProperties(), name, dflt);
    }


    
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
	    return dbPropertiesDummy;
        }
    }


    
    public double getDbProperty(String name, double dflt) {
        return Misc.getProperty(getDbProperties(), name, dflt);
    }

    public String getDbProperty(String name, String dflt) {
        return Misc.getProperty(getDbProperties(), name, dflt);
    }    






    
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

    
    public void writeGlobal(String name, boolean value) throws Exception {
        writeGlobal(name, BLANK + value);
    }

    
    public void writeGlobal(Request request, String propName)
	throws Exception {
        writeGlobal(request, propName, false);
    }



    
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


    
    public List<OutputType> getOutputTypes() throws Exception {
        List<OutputType> allTypes = new ArrayList<OutputType>();
        for (OutputHandler outputHandler : outputHandlers) {
            allTypes.addAll(outputHandler.getTypes());
        }

        return allTypes;
    }


    
    public boolean isOutputTypeOK(OutputType outputType) {
        if (outputType == null) {
            return true;
        }

        return outputType.getOkToUse();
    }

    
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


    
    public void setOutputTypeOK(OutputType outputType, boolean ok)
	throws Exception {
        outputType.setOkToUse(ok);
        String prop = outputType.getId() + ".ok";
        writeGlobal(prop, "" + ok);
    }


    
    public boolean addOutputHandler(OutputHandler outputHandler,boolean...first) {
        synchronized (outputHandlers) {
	    if(first.length>0 && first[0])
		outputHandlers.add(0,outputHandler);
	    else
		outputHandlers.add(outputHandler);
        }

        return addOutputHandlerTypes(outputHandler);
    }

    
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


    
    public List<OutputHandler> getOutputHandlers() {
        return new ArrayList<OutputHandler>(outputHandlers);
    }

    
    public OutputHandler getOutputHandler(Class handlerClass) {
        for (OutputHandler handler : outputHandlers) {
            if (handler.getClass().equals(handlerClass)) {
                return handler;
            }
        }

        return null;
    }


    
    public OutputHandler getOutputHandler(OutputType outputType)
	throws Exception {
        if ( !isOutputTypeOK(outputType)) {
            return null;
        }

        return getOutputHandler(outputType.getId());
    }


    
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


    
    public OutputHandler getOutputHandler(String type) throws Exception {
        if ((type == null) || (type.length() == 0)) {
            type = OutputHandler.OUTPUT_HTML.getId();
        }

        return outputHandlerMap.get(type);
    }




    
    public HtmlOutputHandler getHtmlOutputHandler() {
        try {
            return (HtmlOutputHandler) getOutputHandler(
							OutputHandler.OUTPUT_HTML);
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }


    
    private JsonOutputHandler jsonOutputHandler;

    private CsvOutputHandler csvOutputHandler;
    private PointOutputHandler pointOutputHandler;    

    
    public CsvOutputHandler getCsvOutputHandler() {
        if (csvOutputHandler == null) {
            csvOutputHandler = (CsvOutputHandler) getOutputHandler(
								     org.ramadda.repository.output.CsvOutputHandler.class);
        }

        return csvOutputHandler;
    }

    
    public PointOutputHandler getPointOutputHandler() {
        if (pointOutputHandler == null) {
            pointOutputHandler = (PointOutputHandler) getOutputHandler(PointOutputHandler.class);
        }

        return pointOutputHandler;
    }


    public JsonOutputHandler getJsonOutputHandler() {
        if (jsonOutputHandler == null) {
            jsonOutputHandler = (JsonOutputHandler) getOutputHandler(
								     org.ramadda.repository.output.JsonOutputHandler.class);
        }

        return jsonOutputHandler;
    }
    


    
    public CalendarOutputHandler getCalendarOutputHandler() {
        try {
            return (CalendarOutputHandler) getOutputHandler(
							    CalendarOutputHandler.OUTPUT_CALENDAR);
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }


    
    public ZipOutputHandler getZipOutputHandler() throws Exception {
        return (ZipOutputHandler) getOutputHandler(
						   ZipOutputHandler.OUTPUT_ZIP);
    }


    
    public TypeHandler getGroupTypeHandler() {
        return groupTypeHandler;
    }

    public TypeHandler getFileTypeHandler() {
        return fileTypeHandler;
    }    



    
    public XmlOutputHandler getXmlOutputHandler() throws Exception {
        return (XmlOutputHandler) getOutputHandler(
						   XmlOutputHandler.OUTPUT_XML);
    }




    private TypeHandler anyTypeHandler;

    
    private void initDefaultTypeHandlers() throws Exception {
	
        addTypeHandler(TypeHandler.TYPE_ANY,
                       anyTypeHandler = new TypeHandler(this, TypeHandler.TYPE_ANY,
							"Any file type"));
        addTypeHandler(TypeHandler.TYPE_GROUP,
                       groupTypeHandler = new GroupTypeHandler(this));
        groupTypeHandler.putWikiText("simple","{{tabletree showType=false  showSize=false   showCreateDate=false}}");
        groupTypeHandler.setCategory("Documents");
        groupTypeHandler.putProperty("form.resource.show", "false");
        groupTypeHandler.putProperty("form.ark.show", "true");	
        groupTypeHandler.putProperty("icon", ICON_FOLDER);
        groupTypeHandler.setHelp("A group of entries");
	groupTypeHandler.setPriority(0);
        TypeHandler typeHandler;
        addTypeHandler(TypeHandler.TYPE_FILE,
                       fileTypeHandler =typeHandler = new TypeHandler(this, "file", "File",
								      "Documents"));
	fileTypeHandler.setPriority(0);
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


    
    public void addTypeHandler(String typeName, TypeHandler typeHandler) {
        addTypeHandler(typeName, typeHandler, false);
    }






    private void writeEntryDocs(TypeHandler th) {
	try {
	    if(!th.getForUser()) return;
	    PrintWriter  pw = entryDocsWriter;
	    pw.println("<div class=entrydoc>");
	    pw.println(":lheading " + th.getLabel());
	    pw.println("<div class=ramadda-hidden>");
	    pw.println("To create an entry of type " + th.getLabel() +" go to the desired folder and select Find a Type... from the entry popup menu. Look for the " + th.getLabel() +" entry type");
	    pw.println("</div>");
	    String help = th.getHelp();
	    if(help!=null) {
		pw.println(help.replace("\\n","\n"));
	    }
	    String fhn = th.getTypeProperty("form.header.new",(String) null);
	    Request request = getAdminRequest();
	    if(fhn!=null) {
		pw.println("When creating a new entry:<br>");
		pw.println(getWikiManager().wikify(request,fhn.replace("\\n","\n")));
	    }
	    pw.println("</div>");
	    pw.flush();
	} catch(Exception exc) {
	    exc.printStackTrace();
	}
    }


    private HashSet notTypes;
    private List<String> notTypesList;

    public synchronized void addTypeHandler(String typeName, TypeHandler typeHandler,
                               boolean overwrite) {

	if(notTypes==null) {
	    notTypes=new HashSet();
	    notTypesList = new ArrayList<String>();
	    for(String stype: Utils.split(getRepository().getProperty("ramadda.entry.nottypes",""),",",true,true)) {
		notTypes.add(stype);
		if (StringUtil.containsRegExp(stype)) {
		    notTypesList.add(stype);
		}
	    }
	}
	
	boolean ok = true;
	if(Utils.contains(notTypes,typeHandler.getType()) ||
	   Utils.contains(notTypes,typeHandler.getDescription()) ||
	   Utils.contains(notTypes,"category:" +typeHandler.getCategory()) ||
	   Utils.contains(notTypes,"category:" +typeHandler.getSuperCategory())) {
	    ok =false;
	} else {
	    for(String pattern: notTypesList) {
		if(pattern.startsWith("category:")) {
		    pattern = pattern.substring("category:".length());
		    if(Utils.matches(typeHandler.getCategory(),pattern) ||
		       Utils.matches(typeHandler.getSuperCategory(),pattern)) {
			ok =false;
		    }			
		} else {
		    if(Utils.matches(typeHandler.getType(),pattern) ||
		       Utils.matches(typeHandler.getDescription(),pattern)) {
			ok =false;
		    }
		}
	    }
	}

	if(!ok) {
	    typeHandler.setForUser(false);
	    typeHandler.setCanCreate(false);	    
	}	    

        if (typeHandlersMap.containsKey(typeName)) {
            if ( !overwrite) {
                return;
            }
            TypeHandler oldTypeHandler = typeHandlersMap.get(typeName);
	    oldTypeHandler.setFlushedFromCache(true);
            typeHandlersMap.remove(typeName);
	    
            allTypeHandlers.remove(oldTypeHandler);
        }

        if ( !typeHandlersMap.contains(typeName)) {
            typeHandlersMap.put(typeName, typeHandler);
            allTypeHandlers.add(typeHandler);
        }
    }


    
    public void removeTypeHandler(TypeHandler typeHandler) {
        typeHandlersMap.remove(typeHandler.getType());
        allTypeHandlers.remove(typeHandler);
    }

    
    public TypeHandler getTypeHandler(TypeHandler typeHandler)  {
	try {
	    if(typeHandler.getFlushedFromCache()) {
		typeHandler = getTypeHandler(typeHandler.getType());
	    }
	    return typeHandler;
	} catch(Exception exc) {
	    throw new IllegalArgumentException(exc);
	}
    }

    public List<TypeHandler> getTypeHandlers() throws Exception {
        return new ArrayList<TypeHandler>(allTypeHandlers);
    }


    
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


    public TypeHandler getTypeHandler(Request request) throws Exception {
	return getTypeHandler(request, true);
    }


    public TypeHandler getTypeHandler(Request request,boolean useDefaultIfNotFound) throws Exception {
        if (request != null) {
	    //Handle comma separated list of types
            List<String> types = Utils.split(request.getString(ARG_TYPE,TypeHandler.TYPE_ANY),",",true,true);
            return getTypeHandler(types.size()==0?TypeHandler.TYPE_ANY:types.get(0), useDefaultIfNotFound);
        } else {
            return getTypeHandler(TypeHandler.TYPE_FILE);
        }
    }

    public TypeHandler getTypeHandler(String type) throws Exception {
        return getTypeHandler(type, false);
    }

    
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



    
    public Result processPing(Request request) throws Exception {
        if (request.getString(ARG_RESPONSE, "").equals(RESPONSE_XML)) {
            Document resultDoc = XU.makeDocument();
            Element resultRoot = XU.create(resultDoc, TAG_RESPONSE,
						null, new String[] { ATTR_CODE,
								     "ok" });
            String xml = XU.toString(resultRoot);

            return new Result(xml, MIME_XML);
        }
        StringBuilder sb = new StringBuilder("OK");

        return new Result("", sb);
    }


    
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
            sb.append(HU.p());
        }
        sb.append(HU.p());
        sb.append(
		  "This form allows you to clear any caches and have RAMADDA reload properties");
        sb.append(HU.br());
        if (passPhrase.length() == 0) {
            sb.append(
		      "The pass phrase needs to be set as a property on your server - <i>ramadda.passphrase</i>");
            sb.append(HU.br());
        }
        sb.append(
		  "Note: The pass phrase is not meant to be secure, it is just used so anonymous users can't be clearing your repository state");
        sb.append(HU.hr());
        sb.append(HU.formTable());
        sb.append(request.formPost(URL_CLEARSTATE));
        sb.append(HU.formEntry(msgLabel("Pass Phrase"),
			       HU.input(PROP_PASSPHRASE)));
        sb.append(
		  HU.formEntry(
			       "", HU.submit("Clear Repository State")));
        sb.append(HU.formTableClose());
        sb.append(HU.formClose());

        return new Result("", sb);
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
	    request.setReturnFilename(IO.stripExtension(IO.getFileTail(file))+".png");
	    Misc.run(new Runnable() {
		    public void run()  {
			try {
			    ImageIO.write(tif, "png", out);
			    out.close();
			} catch(Exception exc) {
			    IO.close(in);
			    IO.close(out);
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


    
    public Result processProxy(Request request) throws Exception {
	try {
	    return processProxyInner(request);
	} catch(Exception exc) {
	    String msg = "Error handling proxy request:" + exc.getMessage();
	    getLogManager().logError(msg, exc);
	    //            String json =  JsonUtil.mapAndQuote(Utils.makeListFromValues("error", msg));
	    return  makeErrorResult(request,  msg,false,true,true);
	}
    }

    private synchronized Result processProxyInner(Request request) throws Exception {
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


        URLConnection connection=null;
	try {
	    connection = new URL(url).openConnection();
	    //Pass through some of the headers
	    for(String hdr:new String[]{"Content-disposition","Content-Type"}) {
		String v = connection.getHeaderField(hdr);
		if(v!=null)
		    request.setHeader(hdr,v);
	    }
	} catch (Throwable exc) {
	    HttpURLConnection huc = (HttpURLConnection)connection;
	    if(huc!=null) {
		System.err.println("\tError reading URL:" + url + "\ncode:"  + huc.getResponseCode());
		String error = IO.readError(huc);
		System.err.println("\tError:" + error);
	    }
	    throw exc;
	}

        InputStream   is         = connection.getInputStream();
        if (request.get("xmltojson", false)) {
            String contents = IOUtil.readInputStream(is);
            contents = contents.trim();
            IO.close(is);
            contents = JsonUtil.xmlToJson(XU.getRoot(contents));
            //            System.out.println(contents);

            return new Result(new ByteArrayInputStream(contents.getBytes()),
                              "application/json");
        }


        if (request.get("trim", false)) {
            String contents = IOUtil.readInputStream(is);
            contents = contents.trim();
            IO.close(is);

            return new Result(new ByteArrayInputStream(contents.getBytes()),
                              "application/json");
        }

	try {
	    return request.returnStream(is);
	} catch (Throwable exc) {
	    HttpURLConnection huc = (HttpURLConnection)connection;
	    if(huc!=null) {
		System.err.println("\tError reading URL:" + url + "\ncode:"  + huc.getResponseCode());
	    }
	    throw exc;
	}
    }



    
    public Result processBlank(Request request) throws Exception {
        Result result = new Result("", new StringBuilder());
        result.setShouldDecorate(false);

        return result;
    }

    
    public String getRepositoryDescription() {
        return repositoryDescription;
    }


    
    public String getRepositoryEmail() {
        return getProperty(PROP_ADMIN_EMAIL, "");
    }



    
    public ServerInfo getServerInfo() throws Exception {
        int    sslPort = getHttpsPort();
        String url     = getTmpRequest().getAbsoluteUrl("");
	url = url+getUrlBase();
        return new ServerInfo(url, getHostname(), getPort(), sslPort,
                              getUrlBase(), getRepositoryName(),
                              getRepositoryDescription(),
                              getRepositoryEmail(),
                              getRegistryManager().isEnabledAsServer(),
			      true,
                              false,"",getRepositorySlug());
    }


    public String getRepositorySlug() {
	return getProperty(PROP_REPOSITORY_SLUG,"");
    }

    
    public Result processRobotsTxt(Request request) throws Exception {
        StringBuilder sb = new StringBuilder("");
	sb.append("User-agent: Twitterbot\nAllow: /\n\nUser-agent: facebookexternalhit\nAllow: /\n\n");
	if(acceptGoogleBot()) {
	    sb.append("User-agent: Googlebot\nAllow: /\n\n");
	    sb.append("User-agent: Googlebot-image\nAllow: /\n\n");
	    sb.append("User-agent: Googlebot-Mobile\nAllow: /\n\n");	    	    
	} else {
	    sb.append("User-agent: Googlebot\nDisallow: /\n\n");
	    sb.append("User-agent: Googlebot-image\nDisallow: /\n\n");	    
	    sb.append("User-agent: Googlebot-Mobile\nDisallow: /\n\n");	    	    
	}

	
	if(acceptRobots()) {
	    sb.append("User-agent: *\nAllow: /\n\n");
	} else {
	    sb.append("User-agent: *\nDisallow: /\n\n");
	}
	
	sb.append("Sitemap: "+ request.getAbsoluteUrl(getUrlBase()+"/sitemap.xml")+"\n");
	

        Result result = new Result("", sb,MIME_TEXT);
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
	js = js.replace("${ramadda.ismobile}",""+request.isMobile());
	js  = js.replace("${ramadda.cdn}", getPageHandler().getCdnPath(""));
	js = js.replace("${ramadda.search.tree}","true");
	js = js.replace("${ramadda.urlroot}", base);
	js = js.replace(
			"${ramadda.baseentry}",
			getEntryManager().getRootEntry().getId());
	js = js.replace("${hostname}",
			request.getServerName());
	js = js.replace("${ramadda.user}",
			request.getUser().getId());
	String language  = request.getLanguage();	
	//quote and remove any non-ascii to prevent XSS
	if(Utils.stringDefined(language)) 
	    language = HU.quote(language.toLowerCase().replaceAll("[^a-z]+","_"));
	else 
	    language="null";
	js = js.replace("${ramadda.user.language}",language);

	js = js.replace("${ramadda.languages}",
			getPageHandler().getLanguagesJson());
	js = js.replace("${ramadda.languages.enabled}",
			""+getProperty("ramadda.languages.enabled",false));

	js = js.replace("${ramadda.base.extra}",extra);
	return js;
    }


    public Result processBaseJs(Request request) throws Exception {
	String js = getBaseJs(request);
	Result result =  new Result("base.js",js.getBytes(),"application/x-javascript");
        result.setCacheOk(false);
	return result;
    }
    

    public Result processFlush(Request request) throws Exception {
	if(true) return new Result("",new StringBuilder("not implemented"));
	clearAllCaches();
	return new Result("",new StringBuilder("cleared"));
    }

    public Result processPrintDb(Request request) throws Exception {
	if(true) return new Result("",new StringBuilder("not implemented"));
	if(request.defined("debug"))
	    DatabaseManager.debugConnections = request.get("debug",true);
	getDatabaseManager().printIt();
	return new Result("",new StringBuilder());
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


    public Result processSecurity(Request request) throws Exception {
	String contact = getProperty("ramadada.security.contact","mailto:info@ramadda.org");
	StringBuilder sb = new StringBuilder();
	sb.append("Contact: " + contact+"\n");
	sb.append("Expires: 2030-12-01T19:00:00.000Z\n");
	return new Result(sb.toString(), MIME_TEXT);
    }

    
    public Result processInfo(Request request) throws Exception {
        //        getDatabaseManager().printIt();
        if (request.getString(ARG_RESPONSE, "").equals(RESPONSE_XML)) {
            Document doc  = XU.makeDocument();
            Element  info = getServerInfo().toXml(this, doc);
            info.setAttribute(ATTR_CODE, CODE_OK);
            String xml = XU.toString(info);

            //            System.err.println("returning xml:" + xml);
            return new Result(xml, MIME_XML);
        }
        StringBuilder sb = new StringBuilder("");
        getPageHandler().sectionOpen(request, sb,"Server Information",false);
	sb.append(HU.center(HU.href("https://geodesystems.com",HU.img(getHtdocsUrl("/images/poweredby.png"),"Powered by RAMADDA",HU.attr("width","200px")))));
	sb.append(getPageHandler().showDialogNote("<div style='xwidth:1000px;'>This is a RAMADDA server, developed by <a style='text-decoration: underline;' href='https://geodesystems.com'>Geode Systems</a>.<br> Further information is available at  <a style='text-decoration: underline;' href=https://ramadda.org/repository/a/ramadda_information>ramadda.org</a>.</div>"));


	String info = getProperty("ramadda.information",null);
	if(info!=null)
	    sb.append(info);

        sb.append(HU.formTable());
	String name = getRepositoryName();
	if(Utils.stringDefined(name))
	    HU.formEntry(sb, msgLabel("Name"),name);
	String slug= getRepositorySlug();
	if(Utils.stringDefined(slug))
	    HU.formEntry(sb, msgLabel("Slug"),slug);
	String desc = getRepositoryDescription();
	if(Utils.stringDefined(desc))
	    HU.formEntry(sb, msgLabel("Description"),desc);


	String contact = getProperty("ramadda.contact",null);
	if(Utils.stringDefined(contact))
	    HU.formEntry(sb, msgLabel("Contact"),contact);	

	HU.formEntry(sb, msgLabel("Start Time"),
		     getDateHandler().formatDate(startTime));
	HU.formEntry(sb, msgLabel("RAMADDA Version"),
		     RepositoryUtil.getVersion());
	HU.formEntry(sb, msgLabel("Build Date"),
		     getRepository().getProperty(PROP_BUILD_DATE, "N/A"));
        String version =
            Runtime.class.getPackage().getImplementationVersion();
	version = System.getProperty("java.version");
        HU.formEntry(sb,msgLabel("Java Version"), (String)Utils.getNonNull(version,"NA"));
        getAdmin().addInfo(request, sb);
        if (request.exists("class")) {
            Class c = Class.forName(request.getString("class", ""));
            URL classesRootDir =
                c.getProtectionDomain().getCodeSource().getLocation();
            HU.formEntry(sb, msgLabel("Class location - "
				      + request.getString("class",
							  "")), classesRootDir.toString());

        }

	
        HU.formEntry(sb, msgLabel("Stand alone"), "" + runningStandAlone);
        HU.formEntry(sb,msgLabel("Port"), "" + getPort());
        HU.formEntry(sb, msgLabel("Https Port"), "" + getHttpsPort());


        sb.append(HU.formTableClose());
        getPageHandler().sectionClose(request, sb);
        return new Result("", sb);
    }

    
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




    
    public Result processColorTables(Request request) throws Exception {
        StringBuilder sb = new StringBuilder();
        getPageHandler().sectionOpen(request, sb,"Color Tables",false);	
        sb.append(HU.div("", "id='colortables'"));
	sb.append(HU.importJS(getHtdocsUrl("/colortables.js")));
	//sb.append(HU.importJS(getHtdocsUrl("/esdlcolortables.js")));	
        sb.append(HU.script("Utils.displayAllColorTables('colortables');"));
        getPageHandler().sectionClose(request, sb);
        return new Result("", sb);
    }



    
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
            tmpSB.append(HU.href(fullUrl, url[1]));
            tmpSB.append("<br>&nbsp;");
            cats.get((url[2] == null)
                     ? "Other Documentation"
                     : url[2]).append(tmpSB.toString());
        }
        for (String cat : cats.getCategories()) {
            StringBuilder cb = cats.get(cat);
            if (cb.length() > 0) {
                sb.append(HU.h3(cat));
                sb.append("<ul>");
                sb.append(cb);
                sb.append("</ul>");
            }
        }

        getPageHandler().sectionClose(request, sb);
        return new Result("Documentation", sb);
    }




    
    public Result processMessage(Request request) throws Exception {
        StringBuilder sb = new StringBuilder();
        request.appendMessage(sb);

        return new Result(BLANK, sb);
    }


    
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

    
    public Result processDummy(Request request) throws Exception {
        return new Result(BLANK, new StringBuilder(BLANK));
    }

    public Result processLogLicense(Request request) throws Exception {
	String entryId = request.getString(ARG_ENTRYID,"").trim();
	Entry entry = getEntryManager().getEntry(request,entryId);
	String name = request.getStrictSanitizedString(ARG_NAME,"").trim();
	String email = request.getStrictSanitizedString("email","").trim();
	String id = request.getStrictSanitizedString("licenseid","").trim();	
	getLogManager().logLicense(request.getIp()+","+entryId+","+(entry!=null?entry.getName():"NULL")+","+id+","+name+","+email);
	return new Result(BLANK, new StringBuilder(BLANK));
    }



    public Result processDummyInstall(Request request) throws Exception {
	StringBuilder sb = new StringBuilder();
        getPageHandler().sectionOpen(request, sb, "Install", false);
	sb.append(getPageHandler().showDialogNote("Install is finished"));
        getPageHandler().sectionClose(request, sb);
        return new Result("Install", sb);
    }    



    
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
                    links.add(HU.span(tfo.toString(), extra1));
                } else {
                    links.add(HU.href(request.makeUrl(URL_LIST_SHOW,
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
                links.add(HU.span(names[i], extra1));
            } else {
                links.add(HU.href(request.makeUrl(URL_LIST_SHOW,
						  ARG_WHAT, whats[i]) + typeAttr, names[i], extra2));
            }
        }

        return links;
    }






    
    public Request getTmpRequest()  {
	try {
	    return getTmpRequest(UserManager.USER_ANONYMOUS);
	}catch(Exception exc) {
	    throw new RuntimeException(exc);
	}
    }

    
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


    
    public Request getAdminRequest()  {
	try {
	    User    user    = getUserManager().getAdminUser();
	    Request request = new Request(getRepository(), "", new Hashtable());
	    request.setUser(user);
	    request.setSessionId(getGUID());
	    return request;
	}catch(Exception exc) {
	    throw new RuntimeException(exc);
	}
    }

    public Request getAnonymousRequest()  {
	try {
	    User    user    = getUserManager().getAnonymousUser();
	    Request request = new Request(getRepository(), "", new Hashtable());
	    request.setUser(user);
	    request.setSessionId(getGUID());
	    return request;
	}catch(Exception exc) {
	    throw new RuntimeException(exc);
	}
    }
    public Request getDefaultUserRequest()  {
	try {
	    User    user    = getUserManager().getDefaultUser();
	    Request request = new Request(getRepository(), "", new Hashtable());
	    request.setUser(user);
	    request.setSessionId(getGUID());
	    return request;
	}catch(Exception exc) {
	    throw new RuntimeException(exc);
	}
    }




    
    public Request getTmpRequest(Entry entry) throws Exception {
        Request request = getTmpRequest();
        request.setPageStyle(getPageHandler().doMakePageStyle(request,
							      entry));

        return request;
    }



    
    public Request getRequest(User user) throws Exception {
        Request request = new Request(getRepository(), "", new Hashtable());
        request.setUser(user);

        return request;
    }



    
    public static String msg(String msg) {
        return PageHandler.msg(msg);
    }

    
    public static String msgLabel(String msg) {
        return PageHandler.msgLabel(msg);
    }

    
    public static String msgHeader(String h) {
        return PageHandler.msgHeader(h);
    }


    
    public String getResource(String id) throws Exception {
        return getResource(id, false);
    }


    
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



    public String makeTypeSelect(Request request, boolean includeAny)
	throws Exception {
        return makeTypeSelect(request, includeAny, "", false, null);
    }

    
    public String makeTypeSelect(Request request, boolean includeAny,
                                 String selected, boolean checkAddOk,
                                 HashSet<String> exclude)
	throws Exception {
        return makeTypeSelect(new ArrayList(), request, ARG_TYPE,"",includeAny, selected,
                              checkAddOk, exclude,false);
    }

    
    public String makeTypeSelect(List initItems, Request request,
				 String arg, String attrs,
                                 boolean includeAny, String selected,
                                 boolean checkAddOk, HashSet<String> exclude,boolean groupOnly)
	throws Exception {
	List items =  new ArrayList();
        for (TypeHandler typeHandler : getTypeHandlers()) {
	    if(groupOnly && !typeHandler.isGroup()) continue;


            if (typeHandler.equals(groupTypeHandler)) {
		continue;
	    }

            if (typeHandler.isAnyHandler()) {
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

	TwoFacedObject.sort(items);
	//Add these in in the front of the list
	items.add(0,new TwoFacedObject(groupTypeHandler.getLabel(),
				       groupTypeHandler.getType()));

	if(includeAny && anyTypeHandler!=null) {
            items.add(0,new TwoFacedObject(anyTypeHandler.getLabel(),
					   anyTypeHandler.getType()));
	}	    

	if(initItems!=null)  {
	    List tmp = new ArrayList();
	    tmp.addAll(initItems);
	    tmp.addAll(items);
	    items = tmp;
	}
        return HU.select(arg, items, selected,attrs);
    }



    
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
	return getMimeTypeFromSuffix(entry.getResource().getPath());
    }

    
    public String getMimeTypeFromSuffix(String path) {
	String suffix = IO.getFileExtension(path);
	//A hack because of the mime type properties
	if(!suffix.startsWith(".")) suffix = "." + suffix;
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





    
    public void setLocalFilePaths() {
        localFilePaths = (List<File>) Misc.toList(
						  IOUtil.toFiles(
								 (List<String>) Utils.split(
											    getProperty(PROP_LOCALFILEPATHS, ""), "\n", true, true)));
        //Add the ramadda dir as well
        localFilePaths.add(0, getStorageManager().getRepositoryDir());
    }

    
    public List<File> getLocalFilePaths() {
        return localFilePaths;
    }


    
    public String getFieldDescription(String fieldValue, String namesFile)
	throws Exception {
        return getFieldDescription(fieldValue, namesFile, null);
    }



    
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


    
    public void checkNewEntries(final Request request, final List<Entry> entries) {
	final boolean debug = false;
	if(debug) System.err.println("checkNewEntries:" + entries);
        Misc.run(new Runnable() {
		public void run() {
		    for (EntryChecker entryMonitor : getEntryCheckers()) {
			if(debug) System.err.println("\tentryMonitor:" + entryMonitor);
			entryMonitor.entriesCreated(request, entries);
			if(debug) System.err.println("\tDone");
		    }
		}
	    });
    }

    
    public void checkMovedEntries(final List<Entry> entries) {
	Misc.run(new Runnable() {
		public void run() {
		    for (EntryChecker entryMonitor : getEntryCheckers()) {
			entryMonitor.entriesMoved(entries);
		    }
		}
	    });
    }

    
    public void checkDeletedEntries(final Request request, final List<String> ids) {
        Misc.run(new Runnable() {
		public void run() {
		    for (EntryChecker entryMonitor : getEntryCheckers()) {
			entryMonitor.entriesDeleted(ids);
		    }
		}
	    });
    }


    
    public void checkModifiedEntries(final Request request, final List<Entry> entries) {
        Misc.run(new Runnable() {
		public void run() {
		    Request theRequest = request;
		    if(theRequest==null) theRequest=getAdminRequest();

		    for (EntryChecker entryMonitor : getEntryCheckers()) {
			entryMonitor.entriesModified(theRequest, entries);
		    }
		}
	    });
    }


    
    public List<EntryChecker> getEntryCheckers() {
        return entryMonitors;
    }



    
    public static XmlEncoder getEncoder() {
        XmlEncoder xmlEncoder = new XmlEncoder();
        xmlEncoder.addClassPatternReplacement("ucar.unidata.repository",
					      "org.ramadda.repository");
        xmlEncoder.addClassPatternReplacement(
					      "ucar.unidata.repository.data.Catalog",
					      "org.ramadda.geodata.thredds.Catalog");

        return xmlEncoder;
    }

    
    public static String encodeObject(Object object) {
        return getEncoder().toXml(object, false);
    }

    
    public static Object decodeObject(String xml) throws Exception {
        return getEncoder().toObject(xml);
    }

    
    public List<String> getPythonLibs() {
        return getPluginManager().getPythonLibs();
    }

    
    public static void println(String msg) {
        System.err.println(msg);
    }

    
    public String getSystemMessage() {
	return getProperty(PROP_SYSTEM_MESSAGE, "");
    }

    
    public String getSystemMessage(Request request) {
        return getSystemMessage();
    }

    
    public boolean isStreetviewEnabled() throws Exception {
        ImageOutputHandler imageOutputHandler =
            (ImageOutputHandler) getOutputHandler(
						  ImageOutputHandler.OUTPUT_PLAYER);

        return imageOutputHandler.isStreetviewEnabled();
    }

    
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


