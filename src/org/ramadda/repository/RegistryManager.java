/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository;


import org.ramadda.repository.database.*;


import org.ramadda.repository.util.ServerInfo;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;
import org.ramadda.util.Utils;

import org.ramadda.util.sql.Clause;

import org.ramadda.util.sql.SqlUtil;


import org.w3c.dom.*;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;


import java.io.*;

import java.lang.reflect.*;

import java.net.*;


import java.sql.ResultSet;
import java.sql.Statement;

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

import java.util.regex.*;
import java.util.zip.*;




/**
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
@SuppressWarnings("unchecked")
public class RegistryManager extends RepositoryManager {

    private static final XmlUtil XU=null;

    /** _more_ */
    public final RequestUrl URL_REGISTRY_ADD = new RequestUrl(this,
                                                   "/registry/add");

    /** _more_ */
    public final RequestUrl URL_REGISTRY_LIST = new RequestUrl(this,
                                                    "/registry/list");

    /** _more_ */
    public final RequestUrl URL_REGISTRY_INFO = new RequestUrl(this,
                                                    "/registry/info");

    /** _more_ */
    public RequestUrl URL_REGISTRY_REMOTESERVERS =
        new RequestUrl(this, "/admin/remoteservers", "Remote Servers");


    /** _more_ */
    private Object REMOTE_MUTEX = new Object();

    /**  */
    public static final String ARG_CHANGE = "registry.change";

    /** _more_ */
    public static final String ARG_REGISTRY_RELOAD = "registry.reload";

    /** _more_ */
    public static final String ARG_REGISTRY_SERVER = "registry.server";

    /** _more_ */
    public static final String ARG_REGISTRY_SELECTED = "registry.selected";

    /** _more_ */
    public static final String ARG_REGISTRY_ENABLED = "registry.enabled";

    public static final String ARG_REGISTRY_LIVE = "registry.live";

    public static final String ARG_REGISTRY_ISREGISTRY = "registry.isregistry";        

    /** _more_ */
    public static final String ARG_REGISTRY_LABEL = "registry.label";

    /** _more_ */
    public static final String ARG_REGISTRY_CLIENT = "registry.client";

    /** _more_ */
    public static final String ARG_REGISTRY_ADD = "registry.add";

    /** _more_ */
    public static final String ARG_REGISTRY_URL = "registry.url";

    /** _more_ */
    public static final String ARG_REGISTRY_SEARCHROOT = "registry.searchroot";    

    public static final String ARG_REGISTRY_SLUG="slug";





    /** _more_ */
    private List<ServerInfo> remoteServers;

    /** _more_ */
    private List<ServerInfo> enabledRemoteServers;


    /** _more_ */
    private Hashtable<String, ServerInfo> remoteServerMap;


    /**
     * _more_
     *
     *
     * @param repository _more_
     *
     */
    public RegistryManager(Repository repository) {
        super(repository);
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
    public Result processAdminRemoteServers(Request request)
            throws Exception {
	
	if ( !request.isAdmin()) {
	    throw new IllegalArgumentException("No access");
	}

        StringBuilder sb = new StringBuilder();
        if (request.exists(ARG_REGISTRY_ADD)) {
            if (request.defined(ARG_REGISTRY_URL)) {
                String url = request.getString(ARG_REGISTRY_URL, "");
                URL fullUrl =
                    new URL(
                        HU.url(
                            url + getRepository().URL_INFO.getPath(),
                            new String[] { ARG_RESPONSE,
                                           RESPONSE_XML }));

                try {
                    String contents = getStorageManager().readSystemResource(
                                          fullUrl.toString());
                    Element root = XU.getRoot(contents);
                    if ( !responseOk(root)) {
                        sb.append(
                            getPageHandler().showDialogError(
                                "Failed to read information from:"
                                + fullUrl));
                    } else {
                        ServerInfo serverInfo = new ServerInfo(url,root);
			serverInfo.setIsRegistry(false);
                        serverInfo.setEnabled(true);
                        addRemoteServer(serverInfo, false);
                        return new Result(request.makeUrl(URL_REGISTRY_REMOTESERVERS));
                    }
                } catch (Exception exc) {
                    sb.append(
                        getPageHandler().showDialogError(
                            "Failed to read information from:" + fullUrl));
                }
            }

            sb.append(request.formPost(URL_REGISTRY_REMOTESERVERS, ""));
            sb.append(HU.formTable());
            sb.append(HU.p());

            sb.append(
                HU.formEntry(
                    msgLabel("URL"),
                    HU.input(
                        ARG_REGISTRY_URL,
                        request.getString(ARG_REGISTRY_URL, ""),
                        HU.SIZE_60)));
            sb.append(HU.formTableClose());
            sb.append(HU.submit("Add New Server", ARG_REGISTRY_ADD));
            sb.append(HU.submit(LABEL_CANCEL, ARG_CANCEL));
            sb.append(HU.formClose());

            return getAdmin().makeResult(request, "RAMADDA-Admin-Remote Servers", sb);

        } else if (request.exists(ARG_REGISTRY_RELOAD)) {
            clearRemoteServers();
            for (String server : getServersToRegisterWith()) {
                fetchRemoteServers(server);
            }
            checkApi();
        } else if (request.exists(ARG_CHANGE)) {
            for (ServerInfo serverInfo : getRemoteServers()) {
                String argBase    = serverInfo.getArgBase();
                String enabledId     = ARG_REGISTRY_ENABLED + argBase;
                String liveId     = ARG_REGISTRY_LIVE + argBase;
                String registryId     = ARG_REGISTRY_ISREGISTRY + argBase;				
                String labelFldId = ARG_REGISTRY_LABEL + argBase;
                String urlFldId   = ARG_REGISTRY_URL + argBase;
                String rootFldId   = ARG_REGISTRY_SEARCHROOT + argBase;
                String slugFldId   = ARG_REGISTRY_SLUG + argBase;				
                String label = request.getString(labelFldId,
                                   serverInfo.getLabel());
                String url = request.getString(urlFldId, serverInfo.getUrl());
                String root = request.getString(rootFldId, serverInfo.getSearchRoot());		
                String slug = request.getString(slugFldId, serverInfo.getUrl());		
		if(root==null) root="";
                boolean isEnabled = request.get(enabledId, false);
                boolean isLive = request.get(liveId, false);
                boolean isRegistry = request.get(registryId, false);				
                getDatabaseManager().update(Tables.REMOTESERVERS.NAME,
                                            Tables.REMOTESERVERS.COL_URL,
                                            serverInfo.getId(),
                                            new String[] {
                                                Tables.REMOTESERVERS.COL_URL,
						Tables.REMOTESERVERS.COL_TITLE,
						Tables.REMOTESERVERS.COL_ENABLED,
						Tables.REMOTESERVERS.COL_ISREGISTRY,						
						Tables.REMOTESERVERS.COL_LIVE,						
						Tables.REMOTESERVERS.COL_SEARCHROOT,
						Tables.REMOTESERVERS.COL_SLUG}, new Object[] {
                            url,
                            label, Boolean.valueOf(isEnabled),Boolean.valueOf(isRegistry),Boolean.valueOf(isLive),root,slug });
            }
            clearRemoteServers();
            checkApi();
        } else if (request.exists(ARG_DELETE)) {
            for (ServerInfo serverInfo : getRemoteServers()) {
                String argBase = serverInfo.getArgBase();
                String selectedId  = ARG_REGISTRY_SELECTED + argBase;
                if ( !request.get(selectedId, false)) {
                    continue;
                }
                getDatabaseManager().delete(
                    Tables.REMOTESERVERS.NAME,
                    Clause.eq(
                        Tables.REMOTESERVERS.COL_URL, serverInfo.getId()));
            }
            clearRemoteServers();
            checkApi();
        }

	getPageHandler().sectionOpen(request, sb,"Remote Servers",false);
        sb.append(request.formPost(URL_REGISTRY_REMOTESERVERS, ""));
        sb.append(
            HU.buttons(
                HU.submit("Change", ARG_CHANGE),
                HU.submit("Delete Selected", ARG_DELETE),
                HU.submit("Add New Server", ARG_REGISTRY_ADD)));
        sb.append(HU.open(HU.TAG_UL));
        List<ServerInfo> remoteServers = getRemoteServers();
        sb.append(HU.br());

        sb.append(HU.open(HU.TAG_TABLE));
        int idCnt = 0;
	String space = HU.space(2);				 
	sb.append(HU.row(HU.cols(HU.b(msg("Select")) + space,
				 HU.b(msg("Enabled")) + space,
				 HU.b(msg("From")+"<br>"+msg("Registry?")) + space,
				 HU.b(msg("Live")) + space,
				 HU.b(msg("Repository"))+ space,
				 HU.b(msg("Slug"))+ space,
				 HU.b(msg("URL"))+ space,
				 HU.b(msg("Search Root")))));

        for (ServerInfo serverInfo : remoteServers) {
            sb.append(HU.hidden(ARG_REGISTRY_SERVER,serverInfo.getId()));
            String argBase    = serverInfo.getArgBase();
            String selectedId = ARG_REGISTRY_SELECTED + argBase;
            String enabledId  = ARG_REGISTRY_ENABLED + argBase;
	    String registryId     = ARG_REGISTRY_ISREGISTRY + argBase;				
            String liveId     = ARG_REGISTRY_LIVE + argBase;	    
            String labelFldId = ARG_REGISTRY_LABEL + argBase;
            String urlFldId   = ARG_REGISTRY_URL + argBase;
            String rootFldId   = ARG_REGISTRY_SEARCHROOT + argBase;
            String slugFldId   = ARG_REGISTRY_SLUG+ argBase;	    	    

            String call1 =
                HU.attr(
                    HU.ATTR_ONCLICK,
                    HU.call(
                        "HU.checkboxClicked",
                        HU.comma(
                            "event", HU.squote(ARG_REGISTRY_SELECTED),
                            HU.squote(selectedId))));

            String call2 =
                HU.attr(
                    HU.ATTR_ONCLICK,
                    HU.call(
                        "HU.checkboxClicked",
                        HU.comma(
                            "event", HU.squote(ARG_REGISTRY_ENABLED),
                            HU.squote(enabledId))));

            String selectedCbx = HU.checkbox(selectedId, "true", false,
                                             HU.id(selectedId) + call1);

            String enabledCbx = HU.checkbox(enabledId, "true",
                                             serverInfo.getEnabled(),
                                             HU.id(enabledId));
            String liveCbx = HU.checkbox(liveId, "true",
                                             serverInfo.getLive(),
                                             HU.id(liveId));
            String registryCbx = HU.checkbox(registryId, "true",
                                             serverInfo.getIsRegistry(),
                                             HU.id(registryId));	    	    
            sb.append(HU.row(HU.cols(selectedCbx, enabledCbx, registryCbx,liveCbx,
				     HU.insetDiv(HU.input(labelFldId, serverInfo.getLabel(), HU.SIZE_25),5, 10, 5,10), 
				     HU.insetDiv(HU.input(slugFldId,serverInfo.getSlug()!=null?serverInfo.getSlug():"",
							  HU.attr("title","Short ID") + HU.SIZE_10),5,10,5,10),
				     HU.insetDiv(HU.input(urlFldId,serverInfo.getUrl(), HU.SIZE_30),5,10, 5, 10),
				     HU.insetDiv(HU.input(rootFldId,serverInfo.getSearchRoot(),HU.attr("title","Entry ID to search under") + HU.SIZE_20),5, 10, 5, 10))));


        }

        sb.append(HU.close(HU.TAG_TABLE));
        sb.append(HU.close(HU.TAG_UL));
        sb.append(HU.formClose());
	getPageHandler().sectionClose(request, sb);
        return getAdmin().makeResult(request, msg("RAMADDA-Admin-Remote Servers"), sb);

    }

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public void doFinalInitialization() throws Exception {
        if (isEnabledAsServer()) {
            Misc.run(new Runnable() {
                public void run() {
                    while (true) {
                        //Every one hour clean up the server collection
                        cleanupServers();
                        Misc.sleep(DateUtil.hoursToMillis(1));
                    }
                }
            });
        }



        Misc.run(new Runnable() {public void run() {runRegistrationLoop();}});

    }


    /**
     * _more_
     */
    private void runRegistrationLoop() {
	Misc.sleep(5000);
	while (true) {
	    //Every one hour clean up the server collection
	    try {
		registerWithServers();
	    } catch(Exception exc) {
		logError("RegistryManager.runRegistrationLoop:", exc);
	    }
	    Misc.sleep(DateUtil.hoursToMillis(1));
	}
    }






    /**
     * _more_
     */
    private void cleanupServers() {
        if ( !isEnabledAsServer()) {
            return;
        }
        try {
            List<ServerInfo> servers = getRemoteServers();
            for (ServerInfo serverInfo : servers) {
		checkServer(serverInfo, true);
            }
        } catch (Exception exc) {
            logError("RegistryManager.cleanUpServers:", exc);
        }


    }




    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public void checkApi() throws Exception {
        ApiMethod apiMethod =
            getRepository().getApiManager().getApiMethod("/registry/list");
        if (apiMethod != null) {
            apiMethod.setIsTopLevel(isEnabledAsServer() || getEnabledRemoteServers().size() > 0);
        }

    }


    /**
     * _more_
     *
     * @param request _more_
     * @param csb _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void addAdminSettings(Request request, StringBuffer csb)
            throws Exception {
        String helpLink =
            HU.href(getRepository().getUrlBase()
                           + "/userguide/remoteservers.html", msg("Help"),
                               HU.attr(HU.ATTR_TARGET,
                                   "_help"));
        csb.append(HU.row(
			  HU.colspan(msgHeader("Server Registry"), 2)));

	HU.formEntry(csb,  "",
			    HU.labeledCheckbox(
					       PROP_REGISTRY_ENABLED, "true", 
					       formPropValue(request,PROP_REGISTRY_ENABLED,false),
					       msg("Enable this server to be a registry for other servers")));

        csb.append(HU.formEntry("",
				msgLabel("Servers this server registers with")
				+ HU.space(2) + helpLink));
        csb.append(
            HU.formEntry(
                "",
                HU.textArea(
                    PROP_REGISTRY_SERVERS,
		    formPropValue(request,PROP_REGISTRY_SERVERS,""),
                    5, 60)));

    }



    /**
     * _more_
     *
     * @param request _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void applyAdminSettings(Request request) throws Exception {
        List<String> oldList = getServersToRegisterWith();
        List<String> newList =
            Utils.split(request.getUnsafeString(PROP_REGISTRY_SERVERS, ""),
                        "\n", true, true);


        getRepository().writeGlobal(PROP_REGISTRY_SERVERS,
                                    StringUtil.join("\n", newList));
        getRepository().writeGlobal(PROP_REGISTRY_ENABLED,
                                    request.get(PROP_REGISTRY_ENABLED, false)
                                    + "");
        checkApi();
        if ( !newList.equals(oldList)) {
            for (String url : oldList) {
                newList.remove(url);
                Misc.run(this, "registerWithServer", url);
            }
            for (String url : newList) {
                Misc.run(this, "registerWithServer", url);
            }
        }
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isEnabledAsServer() {
        return getRepository().getProperty(PROP_REGISTRY_ENABLED, false);
    }




    /**
     * _more_
     */
    private void clearRemoteServers() {
        enabledRemoteServers = null;
        remoteServers        = null;
        remoteServerMap      = null;
        getSearchManager().clearSearchProviders();
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
    public ServerInfo findRemoteServer(String id) throws Exception {
        if (id.equals(ServerInfo.ID_THIS)) {
            ServerInfo serverInfo = getRepository().getServerInfo();
            serverInfo.setEnabled(true);

            return serverInfo;
        }

        return getRemoteServerMap().get(id);
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<ServerInfo> getEnabledRemoteServers() throws Exception {
        List<ServerInfo> selected = enabledRemoteServers;
        if (selected != null) {
            synchronized (selected) {
                return new ArrayList<ServerInfo>(selected);
            }
        }
        selected = new ArrayList<ServerInfo>();
        for (ServerInfo serverInfo : getRemoteServers()) {
            if (serverInfo.getEnabled()) {
                selected.add(serverInfo);
            }
        }
        enabledRemoteServers = selected;

        return selected;
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Hashtable<String, ServerInfo> getRemoteServerMap()
            throws Exception {
        Hashtable<String, ServerInfo> map = remoteServerMap;
        while (map == null) {
            getRemoteServers();
            map = remoteServerMap;
        }

        return map;
    }



    /**
     * _more_
     *
     * @param results _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private ServerInfo makeRemoteServer(ResultSet results) throws Exception {
	int idx=1;
        String  id         = results.getString(idx++);
        URL     url        = new URL(id);
        String  title      = results.getString(idx++);
        String  desc       = results.getString(idx++);
        String  email      = results.getString(idx++);
        boolean isRegistry = results.getInt(idx++) != 0;
	boolean isEnabled = results.getInt(idx++) != 0;
	boolean isLive = results.getInt(idx++) != 0;	
        String  root      = results.getString(idx++);
        String  slug      = results.getString(idx++);	

        ServerInfo serverInfo = new ServerInfo(id, url.getHost(),
					       url.getPort(), -1, url.getPath(), title,
					       desc, email, isRegistry, isEnabled,isLive,root,slug);

        return serverInfo;
    }

    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<ServerInfo> getRemoteServers() throws Exception {
        List<ServerInfo> servers = remoteServers;
        if (servers != null) {
            return servers;
        }
        servers = new ArrayList<ServerInfo>();
        Hashtable<String, ServerInfo> map = new Hashtable<String,
                                                ServerInfo>();

        Statement stmt =
            getDatabaseManager().select(Tables.REMOTESERVERS.COLUMNS,
                                        Tables.REMOTESERVERS.NAME,
                                        (Clause) null,
                                        " order by "
                                        + Tables.REMOTESERVERS.COL_URL);
        SqlUtil.Iterator iter = getDatabaseManager().getIterator(stmt);
        ResultSet        results;
        HashSet          seen = new HashSet();
        while ((results = iter.getNext()) != null) {
            ServerInfo serverInfo = makeRemoteServer(results);
            if (seen.contains(serverInfo.getId())) {
                continue;
            }
            seen.add(serverInfo.getId());
            map.put(serverInfo.getId(), serverInfo);
            servers.add(serverInfo);
        }
        clearRemoteServers();
        remoteServerMap = map;
        remoteServers   = servers;

        return servers;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public List<String> getServersToRegisterWith() {
        List<String> urls =
            Utils.split(getRepository().getProperty(PROP_REGISTRY_SERVERS,
                ""), "\n", true, true);

        return urls;
    }


    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public void registerWithServers() throws Exception {
        List<String> urls = getServersToRegisterWith();
        for (String url : urls) {
            registerWithServer(url);
        }
    }


    /**
     * _more_
     *
     * @param url _more_
     *
     * @throws Exception _more_
     */
    public void registerWithServer(String url) throws Exception {
        ServerInfo serverInfo = getRepository().getServerInfo();
        url = url + URL_REGISTRY_ADD.getPath();
        URL theUrl = new URL(HU.url(url, ARG_REGISTRY_CLIENT, serverInfo.getUrl()));
        try {
            String  contents = getStorageManager().readSystemResource(theUrl);
            Element root     = XU.getRoot(contents);
            if ( !responseOk(root)) {
		System.err.println(contents);
                logInfo("RegistryManager.registerWithServer: Failed to register with:" + theUrl);
		return;
            } 
	    logInfo("RegistryManager.registerWithServer: Registered with:"+ theUrl);

        } catch (Exception exc) {
            logError("RegistryManager.registerWithServer: Error registering with:" + theUrl, exc);
        }
    }


    /**
     * _more_
     *
     * @param root _more_
     *
     * @return _more_
     */
    public boolean responseOk(Element root) {
        return XU.getAttribute(root, ATTR_CODE).equals(CODE_OK);
    }


    private Result makeErrorResult(String msg) throws Exception {
	return new Result(XU.tag(TAG_RESPONSE, XU.attr(ATTR_CODE, CODE_ERROR),msg), MIME_XML);	
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
    public Result processRegistryAdd(Request request) throws Exception {
        StringBuffer sb = new StringBuffer();
        if ( !isEnabledAsServer()) {
            logInfo("RegistryManager.processRegistryAdd: Was asked to register a server when this server is not configured as a registry server. URL = "
                + request);
            return makeErrorResult("Not enabled as a registry");
        }

        String     baseUrl    = request.getString(ARG_REGISTRY_CLIENT, "");
        ServerInfo serverInfo = new ServerInfo(new URL(baseUrl), "", "");
        logInfo("RegistryManager.processRegistryAdd: calling checkServer url="+ baseUrl);
        if (checkServer(serverInfo, true)) {
	    //            return new Result(XU.tag(TAG_RESPONSE, XU.attr(ATTR_CODE, CODE_OK), "OK"), MIME_XML);
	    return returnRegistryXml(request);
        }
	return returnRegistryXml(request);
	//        return makeErrorResult("failed");
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
    public Result processRegistryInfo(Request request) throws Exception {
        final String requestingServer =
            request.getString(ARG_REGISTRY_SERVER, "");
        URL          requestingServerUrl = new URL(requestingServer);
        List<String> servers             = getServersToRegisterWith();
        boolean      ok                  = false;
        StringBuffer msg                 = new StringBuffer();

        int          requestingPort      = requestingServerUrl.getPort();
        if (requestingPort == -1) {
            requestingPort = 80;
        }
        msg.append("requesting server:" + requestingServerUrl.getHost() + ":"
                   + requestingPort + "\n");


        for (String myServer : servers) {
            URL myServerUrl  = new URL(myServer);
            int myServerPort = myServerUrl.getPort();
            if (myServerPort == -1) {
                myServerPort = 80;
            }
            msg.append("    my server:" + myServerUrl.getHost() + ":"
                       + myServerPort + "\n");
            if (myServerUrl.getHost().toLowerCase()
                    .equals(requestingServerUrl.getHost()
                        .toLowerCase()) && (myServerPort == requestingPort)) {
                ok = true;

                break;
            }
        }

        if ( !ok) {
            logInfo(
                "RegistryManger.processRegistryInfo: Was asked to register with a server:"
                + requestingServer + " that is not in our list:" + servers);
	    makeErrorResult("Not registering with you. Output: " + msg);
        }
        Misc.run(new Runnable() {
            public void run() {
                try {
                    fetchRemoteServers(requestingServer);
                } catch (Exception exc) {
                    logError(
                        "RegistryManager.processRegistryInfo: Loading servers from:"
                        + requestingServer, exc);
                }
            }
        });

        return getRepository().processInfo(request);
    }


    /**
     * _more_
     *
     * @param serverUrl _more_
     *
     * @throws Exception _more_
     */
    private void fetchRemoteServers(String serverUrl) throws Exception {
        serverUrl = serverUrl + URL_REGISTRY_LIST.getPath();
        serverUrl = HU.url(serverUrl, ARG_RESPONSE, RESPONSE_XML);
        String contents =
            getStorageManager().readSystemResource(new URL(serverUrl));
        Element root = XU.getRoot(contents);

        if ( !responseOk(root)) {
            logInfo("RegistryManager.fetchRemoteServers: Bad response from " + serverUrl);
            return;
        }
        List<ServerInfo> servers  = new ArrayList<ServerInfo>();
        ServerInfo       me       = getRepository().getServerInfo();
        NodeList         children = XU.getElements(root);
        for (int i = 0; i < children.getLength(); i++) {
            Element node = (Element) children.item(i);
            servers.add(new ServerInfo(node));
        }


        Hashtable<String, ServerInfo> map = getRemoteServerMap();
        for (ServerInfo serverInfo : servers) {
            if (serverInfo.equals(me)) {
                continue;
            } else {}
            ServerInfo oldServer = map.get(serverInfo.getId());
	    //If we already have it then don't add it
            if (oldServer != null) {
		continue;
            }
            addRemoteServer(serverInfo, false);
        }
    }


    /**
     * _more_
     *
     * @param serverInfo _more_
     * @param deleteOldServer _more_
     *
     * @throws Exception _more_
     */
    private void addRemoteServer(ServerInfo serverInfo,
                                 boolean deleteOldServer)
            throws Exception {
        if (deleteOldServer) {
            getDatabaseManager().delete(
                Tables.REMOTESERVERS.NAME,
                Clause.eq(Tables.REMOTESERVERS.COL_URL, serverInfo.getUrl()));
        }

	if(getDatabaseManager().tableContains(Tables.REMOTESERVERS.NAME,
					      Tables.REMOTESERVERS.COL_URL,
					      serverInfo.getUrl())) {
	    return;
	}

        getDatabaseManager().executeInsert(Tables.REMOTESERVERS.INSERT,
                                           new Object[] {
            serverInfo.getUrl(), serverInfo.getTitle(),
            serverInfo.getDescription(), serverInfo.getEmail(),
            Boolean.valueOf(serverInfo.getIsRegistry()),
            Boolean.valueOf(serverInfo.getEnabled()),
            Boolean.valueOf(serverInfo.getLive()),	    
            serverInfo.getSearchRoot(),
	    serverInfo.getSlug()	    
        });
        clearRemoteServers();
    }

    private boolean checkServer(ServerInfo serverInfo,  boolean deleteOnFailure)
	throws Exception {
        String serverUrl =
            HU.url(
                serverInfo.getUrl()
                + URL_REGISTRY_INFO.getPath(), new String[] {
                    ARG_RESPONSE,
                    RESPONSE_XML, ARG_REGISTRY_SERVER,
                    getRepository().getServerInfo().getUrl() });

        if (serverUrl.indexOf("/localhost:") >= 0) {
	    return false;
	}
        if (serverUrl.indexOf("pws.scqx.gov.cn") >= 0) {
            return false;
        }
        try {
            String contents =  IO.readUrl(new URL(serverUrl));
            Element root = XU.getRoot(contents);
            if (responseOk(root)) {
                ServerInfo clientServer = new ServerInfo(root);
                if (clientServer.equals(serverInfo)) {
                    logInfo("RegistryManager.checkServer: adding server " + serverUrl);
                    return true;
                } else {
                    logInfo("RegistryManager.checkServer: not equals:"
                            + serverInfo.getId() + " "
                            + clientServer.getId());

                }
            } else {
                logInfo("RegistryManager.checkServer: response not ok from:"
                        + serverInfo + " with url: " + serverUrl
                        + "response:\n" + contents);
            }
        } catch (Exception exc) {
            logError(
                "RegistryManager.checkServer: Could not fetch server xml from:"
                + serverInfo + " with url:" + serverUrl, exc);
        }
        if (deleteOnFailure) {
            logInfo("RegistryManager.checkServer: Marking server not live:"  + serverInfo.getUrl());
        }
        return false;
    }


    private Result returnRegistryXml(Request request) throws Exception {
	List<ServerInfo> servers = new ArrayList<ServerInfo>(remoteServers);
	//Add myself to the list
	servers.add(0, getRepository().getServerInfo());
	Document resultDoc = XU.makeDocument();
	Element resultRoot = XU.create(resultDoc, TAG_RESPONSE,
				       null, new String[] { ATTR_CODE, CODE_OK });
	
	for (ServerInfo serverInfo : servers) {
	    resultRoot.appendChild(serverInfo.toXml(getRepository(),
						    resultDoc));
	}
	return new Result(XU.toString(resultRoot, false), MIME_XML);
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
    public Result processRegistryList(Request request) throws Exception {
        boolean responseAsXml = request.getString(ARG_RESPONSE,
                                    "").equals(RESPONSE_XML);
        List<ServerInfo>    remoteServers     = getEnabledRemoteServers();
        if (responseAsXml) {
	    return returnRegistryXml(request);
        }

        StringBuilder sb = new StringBuilder();
	makeRegistryList(request,sb);
        Result result = new Result(msg("Registry List"), sb);
        return result;
    }

    private void makeRegistryList(Request request,StringBuilder sb) throws Exception {
        HashSet<ServerInfo> seen              = new HashSet<ServerInfo>();
	getPageHandler().sectionOpen(request, sb,"Repository Registry List",false);
	boolean          evenRow = false;
	boolean          didone  = false;
	List<ServerInfo> servers =  remoteServers;
	if(servers==null) servers=new ArrayList<ServerInfo>();
	for (ServerInfo serverInfo : servers) {
	    if (seen.contains(serverInfo)) {
		continue;
	    }
	    if ( !didone) {
		sb.append(HU.p());
		sb.append(msgHeader("Remote Servers"));
		sb.append("<table cellspacing=\"0\" cellpadding=\"4\">");
		sb.append(
			  HU.row(
				 HU.headerCols(
					       new String[] { msg("Repository"),
							      msg("URL"), msg("Is Registry?") })));
	    }
	    didone = true;
	    seen.add(serverInfo);
	    sb.append(HU.row(HU.cols(new String[] {
			    serverInfo.getLabel(),
			    HU.href(serverInfo.getUrl(), serverInfo.getUrl()),
			    (serverInfo.getIsRegistry()
			     ? msg("Yes")
			     : msg("No")) }), HU.cssClass(evenRow
							  ? "listrow1"
							  : "listrow2")));
	    String desc = serverInfo.getDescription();
	    if (stringDefined(desc)) {
		desc = HU.makeShowHideBlock(msg("Description"),
					    desc, false);
		sb.append(HU.row(HU.colspan(desc, 3),
				 HU.cssClass(evenRow
					     ? "listrow1"
                            : "listrow2")));
	    }
	    evenRow = !evenRow;
	}
	if (didone) {
	    sb.append("</table>");
	}
	if (isEnabledAsServer()) {
	    if (!didone) {
		sb.append("This RAMADDA can act as a registry server but no servers are currently registered");
	    }
        }
	getPageHandler().sectionClose(request, sb);

    }

}
