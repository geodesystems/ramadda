/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.server;

import org.ramadda.repository.Constants;
import org.ramadda.repository.Repository;

import org.ramadda.util.Utils;


import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.security.Constraint;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Hashtable;
import java.util.Properties;

public class JettyServer implements Constants {
    private String[] args;
    private int port;
    private int sslPort = -1;
    private Server server;
    private RepositoryServlet baseServlet;
    private ServletContextHandler context;
    private Repository baseRepository;
    private Hashtable<RepositoryServlet, ServletHolder> servletToHolder =
        new Hashtable<RepositoryServlet, ServletHolder>();

    public JettyServer(String[] args) throws Throwable {
        this.args = args;
        boolean hadPort = false;
        port = 8080;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-port")) {
		i++;
		if(Utils.stringDefined(args[i])) {
		    hadPort = true;
		    port    = Integer.parseInt(args[i]);
		}
            } else if (args[i].equals("-sslport")) {
		i++;
		if(Utils.stringDefined(args[i])) {
		    sslPort =  Integer.parseInt(args[i]);
		}
	    }
        }

        context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
	//block TRACE calls
	//from https://stackoverflow.com/questions/67503838/disable-trace-for-embedded-jetty-server
	ConstraintSecurityHandler constraintSecurityHandler = new ConstraintSecurityHandler();
	context.setSecurityHandler(constraintSecurityHandler);
        Constraint constraintDisableTrace = new Constraint();
        constraintDisableTrace.setAuthenticate(true);
        ConstraintMapping mappingDisableTrace = new ConstraintMapping();
        mappingDisableTrace.setPathSpec("/");
        mappingDisableTrace.setMethod("TRACE");
        mappingDisableTrace.setConstraint(constraintDisableTrace);
        constraintSecurityHandler.addConstraintMapping(mappingDisableTrace);

        Constraint constraintEnabledEverythingButTrace = new Constraint();
        ConstraintMapping mappingEnableEverythingButTrace = new ConstraintMapping();
        mappingEnableEverythingButTrace.setPathSpec("/");
        mappingEnableEverythingButTrace.setMethodOmissions(new String[]{"TRACE"});
        mappingEnableEverythingButTrace.setConstraint(constraintEnabledEverythingButTrace);
        constraintSecurityHandler.addConstraintMapping(mappingEnableEverythingButTrace);

        GzipHandler gzipHandler = new GzipHandler();
        gzipHandler.addIncludedMethods("GET", "POST");
        context.setGzipHandler(gzipHandler);
        baseServlet    = addServlet();
        baseRepository = baseServlet.getRepository();

        if ( !hadPort) {
            port = baseRepository.getProperty("ramadda.port", port);
        }
        baseRepository.setPort(port);
	if(sslPort<0)
	    sslPort = baseRepository.getHttpsPort();
	QueuedThreadPool threadPool = new QueuedThreadPool();
	int threads = baseRepository.getProperty("ramadda.server.threadcount",-1);
	if(threads>0) {
	    System.err.println("RAMADDA: #http threads:" + threads);
	    threadPool.setMaxThreads(threads);
	}
        server  = new Server(threadPool);
        HttpConfiguration httpConfig = new HttpConfiguration();
	ServerConnector http = new ServerConnector(server, new HttpConnectionFactory(httpConfig));
	http.setPort(port);
	server.addConnector(http);
        server.setHandler(context);

        context.addServlet(new ServletHolder(baseServlet), "/");
        try {
            initSsl(server, baseServlet.getRepository());
        } catch (Throwable exc) {
            baseServlet.getRepository().getLogManager().logError(
								 "SSL: error opening ssl connection", exc);
        }
        server.start();
        server.join();
    }

    public int getPort() {
        return port;
    }

    public RepositoryServlet addServlet() throws Exception {
        Properties properties  = new Properties();
        String[]   cmdLineArgs = args;

        return addServlet(new RepositoryServlet(this, cmdLineArgs, port,sslPort,
						properties));
    }

    public RepositoryServlet addServlet(RepositoryServlet servlet)
	throws Exception {
        Repository    repository = servlet.getRepository();
        String        path       = repository.getUrlBase();
        ServletHolder holder     = new ServletHolder(servlet);
        context.addServlet(holder, path + "/*");
        servletToHolder.put(servlet, holder);
        repository.setStandAloneServer(this);

        return servlet;
    }

    public void removeServlet(RepositoryServlet servlet) throws Exception {
        ServletHolder holder = servletToHolder.get(servlet);
        //TODO: Remove the servlet from the server
    }

    protected void initSsl(Server server, Repository repository)
	throws Throwable {

        File keystore =
            new File(repository.getPropertyValue(PROP_SSL_KEYSTORE,
						 repository.getStorageManager().getRepositoryDir()
						 + "/keystore.jks", false));

        if ( !keystore.exists()) {
            keystore =
                new File(repository.getPropertyValue(PROP_SSL_KEYSTORE,
						     repository.getStorageManager().getRepositoryDir()
						     + "/keystore", false));
        }

        if ( !keystore.exists()) {
            return;
        }

        if (repository.getProperty(PROP_SSL_IGNORE, false)) {
            repository.getLogManager().logInfo("SSL: ssl.ignore is set.");

            return;
        }
        //        repository.getLogManager().logInfo("SSL: using keystore: " + keystore);

        String password = repository.getPropertyValue(PROP_SSL_PASSWORD,
						      (String) null, false);
        String keyPassword = repository.getPropertyValue(PROP_SSL_PASSWORD,
							 password, false);
        if (password == null) {
            repository.getLogManager().logInfo(
					       "SSL: no password and keypassword property defined");

            /*
	      repository.getLogManager().logInfoAndPrint(
	      "SSL: define the properties:\n\t" + PROP_SSL_PASSWORD
	      + "=<the ssl password>\n" + "\t" + PROP_SSL_KEYPASSWORD
	      + "=<the key password>\n"
	      + "in some .properties file (e.g., \"ssl.properties\") in the RAMADDA directory:"
	      + repository.getStorageManager().getRepositoryDir()
	      + "\nor as a System property on the java command line:"
	      + "-D" + PROP_SSL_PASSWORD + "=<the ssl password>  " + "-D"
	      + PROP_SSL_KEYPASSWORD + "=<the key password>");
            */
            return;
        }

        if (sslPort < 0) {
            repository.getLogManager().logInfoAndPrint(
						       "SSL: no ssl port defined. not creating ssl connection");
            repository.getLogManager().logInfoAndPrint(
						       "SSL: define the property:\n\t" + PROP_SSL_PORT
						       + "=<the ssl port>\n"
						       + "in some .properties file (e.g., \"ssl.properties\") in the RAMADDA directory:"
						       + repository.getStorageManager().getRepositoryDir()
						       + "\nor as a System property on the java command line:"
						       + "-D" + PROP_SSL_PORT + "=<the ssl port>");

            return;
        }

        repository.getLogManager().logInfo(
					   "SSL: creating ssl connection on port:" + sslPort);
        HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setSecureScheme("https");
        httpConfig.setSecurePort(sslPort);
        httpConfig.setOutputBufferSize(32768);

        //        System.err.println("key:" + keystore +" pass:" + password+ " " + keyPassword);
        SslContextFactory sslContextFactory = new SslContextFactory.Server();
        sslContextFactory.setKeyStorePath(keystore.toString());
        sslContextFactory.setKeyStorePassword(password);
        sslContextFactory.setKeyManagerPassword(keyPassword);

        String certAlias = repository.getPropertyValue(PROP_SSL_CERTALIAS,
						       (String) null, false);
        if (certAlias != null) {
            sslContextFactory.setCertAlias(certAlias);
        }

	sslContextFactory.setIncludeProtocols("TLSv1.2", "TLSv1.3");
        sslContextFactory.setExcludeProtocols("SSLv3");
	sslContextFactory.setUseCipherSuitesOrder(true);
	// Prefer server ciphers over client preference
        sslContextFactory.setRenegotiationAllowed(false);
	File ciphers = new File(baseRepository.getStorageManager().localizePath("%repositorydir%/ciphers.txt"));
	if(ciphers.exists()) {
	    List<String> list = new ArrayList<String>();
	    for (String cipher :  Utils.split(IOUtil.readContents(ciphers), "\n",true, true)) {
		if(cipher.startsWith("#")) continue;
		System.err.println("Adding:" + cipher);
		list.add(cipher);
	    }
	    //	    System.err.println("LIST:" + list);
	    sslContextFactory.setIncludeCipherSuites(Utils.toStringArray(list));
	} else {
	    sslContextFactory.setIncludeCipherSuites(
						     // Preferred AEAD ciphers (TLS 1.2)
						     "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
						     "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
						     "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
						     "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
						     // Safe fallback CBC ciphers for Java 8 compatibility
						     "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256",
						     "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384"
						     );
	}

	/* old suites
        sslContextFactory.setIncludeCipherSuites(
						 "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
						 "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
						 "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
						 "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
						 "TLS_AES_128_GCM_SHA256",
						 "TLS_AES_256_GCM_SHA384",
						 "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
						 "TLS_DHE_DSS_WITH_AES_128_GCM_SHA256",
						 "TLS_DHE_DSS_WITH_AES_256_GCM_SHA384",
						 "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
						 "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256",
						 "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
						 "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
						 "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
						 "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384",
						 "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
						 "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
						 "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
						 "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256",
						 "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
						 "TLS_DHE_DSS_WITH_AES_128_CBC_SHA256",
						 "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256",
						 "TLS_DHE_DSS_WITH_AES_256_CBC_SHA",
						 "TLS_DHE_RSA_WITH_AES_256_CBC_SHA",
						 "TLS_RSA_WITH_AES_128_GCM_SHA256",
						 "TLS_RSA_WITH_AES_256_GCM_SHA384",
						 "TLS_RSA_WITH_AES_128_CBC_SHA256",
						 "TLS_RSA_WITH_AES_256_CBC_SHA256",
						 "TLS_RSA_WITH_AES_128_CBC_SHA", "TLS_RSA_WITH_AES_256_CBC_SHA",
						 "TLS_SRP_SHA_DSS_WITH_AES_256_CBC_SHA",
						 "TLS_SRP_SHA_RSA_WITH_AES_256_CBC_SHA",
						 "TLS_SRP_SHA_WITH_AES_256_CBC_SHA",
						 "TLS_DHE_DSS_WITH_AES_256_CBC_SHA256",
						 "TLS_SRP_SHA_DSS_WITH_AES_128_CBC_SHA",
						 "TLS_SRP_SHA_RSA_WITH_AES_128_CBC_SHA",
						 "TLS_SRP_SHA_WITH_AES_128_CBC_SHA",
						 "TLS_DHE_DSS_WITH_AES_128_CBC_SHA",
						 "TLS_DHE_RSA_WITH_CAMELLIA_256_CBC_SHA",
						 "TLS_DHE_DSS_WITH_CAMELLIA_256_CBC_SHA",
						 "TLS_RSA_WITH_CAMELLIA_256_CBC_SHA",
						 "TLS_DHE_RSA_WITH_CAMELLIA_128_CBC_SHA",
						 "TLS_DHE_DSS_WITH_CAMELLIA_128_CBC_SHA",
						 "TLS_RSA_WITH_CAMELLIA_128_CBC_SHA");
	*/


        sslContextFactory.setExcludeCipherSuites(
						 "^.*_(MD5|RC4|DES|3DES)_.*$",
						 "^TLS_RSA_.*$",
						 "SSL_RSA_WITH_3DES_EDE_CBC_SHA", "SSL_DHE_RSA_WITH_DES_CBC_SHA",
						 "SSL_DHE_DSS_WITH_DES_CBC_SHA", "EXP-RC4-MD5",
						 "EDH-RSA-DES-CBC-SHA", "EXP-EDH-RSA-DESCBC-SHA", "DES-CBC-SHA",
						 "EXP-DES-CBC-SHA", "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
						 "TLS_DHE_RSA_WITH_AES_256_CBC_SHA");



        HttpConfiguration httpsConfig = new HttpConfiguration(httpConfig);
        httpsConfig.addCustomizer(new SecureRequestCustomizer());

        ServerConnector httpsConnector =
            new ServerConnector(
				server,
				new SslConnectionFactory(
							 sslContextFactory,
							 HttpVersion.HTTP_1_1
							 .asString()), new HttpConnectionFactory(httpsConfig));
        httpsConnector.setPort(sslPort);
        httpsConnector.setIdleTimeout(500000);
        server.addConnector(httpsConnector);
    }

    public static void main(String[] args) throws Throwable {
        try {
            JettyServer mds = new JettyServer(args);
        } catch (Exception exc) {
            LogUtil.printExceptionNoGui(null, "Error in main",  LogUtil.getInnerException(exc));
            System.exit(1);
        }
    }

}
