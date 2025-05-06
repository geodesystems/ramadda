/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository;

import org.ramadda.repository.admin.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.database.*;
import org.ramadda.repository.harvester.*;
import org.ramadda.repository.map.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.monitor.*;
import org.ramadda.repository.type.*;
import org.ramadda.repository.util.*;
import org.ramadda.repository.util.ServerInfo;

import org.ramadda.util.HtmlTemplate;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.PropertyProvider;

import org.ramadda.util.TempDir;

import org.ramadda.util.Utils;

import org.ramadda.util.sql.Clause;
import org.ramadda.util.sql.SqlUtil;

import org.w3c.dom.*;

import ucar.unidata.util.Cache;
import ucar.unidata.util.CacheManager;

import ucar.unidata.util.Counter;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlEncoder;

import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.io.File;
import java.io.InputStream;

import java.lang.reflect.*;

import java.net.*;

import java.sql.ResultSet;
import java.sql.Statement;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

import java.util.regex.*;
import java.util.zip.*;

/**
 * The main class.
 *
 */
@SuppressWarnings("unchecked")
public class ApiManager extends RepositoryManager {

    ApiMethod homeApi;

    private Hashtable<String, RequestHandler> apiHandlers =
        new Hashtable<String, RequestHandler>();

    Hashtable<String, ApiMethod> requestMap = new Hashtable();

    ArrayList<ApiMethod> apiMethods = new ArrayList();

    ArrayList<ApiMethod> wildCardApiMethods = new ArrayList();

    ArrayList<ApiMethod> topLevelMethods = new ArrayList();

    /**
     * ctor
     *
     * @param repository _more_
     */
    public ApiManager(Repository repository) {
        super(repository);
    }

    protected void addRequest(Element node, Hashtable props,
                              Hashtable handlers, String defaultHandler)
            throws Exception {

        String request    = XmlUtil.getAttribute(node,
                                ApiMethod.ATTR_REQUEST);

        String methodName = XmlUtil.getAttribute(node, ApiMethod.ATTR_METHOD);
        boolean needsSsl = XmlUtil.getAttributeFromTree(node,
                               ApiMethod.ATTR_NEEDS_SSL, false);
        boolean checkAuthMethod = XmlUtil.getAttributeFromTree(node,
                                      ApiMethod.ATTR_CHECKAUTHMETHOD, false);
        boolean checkIsHuman = XmlUtil.getAttributeFromTree(node,
                                      "checkishuman", false);	

        String authMethod = XmlUtil.getAttributeFromTree(node,
                                ApiMethod.ATTR_AUTHMETHOD, "");

        boolean admin = XmlUtil.getAttributeFromTree(node,
                            ApiMethod.ATTR_ADMIN,
                            Misc.getProperty(props, ApiMethod.ATTR_ADMIN,
                                             true));

        boolean mustBeUser = XmlUtil.getAttributeFromTree(node,
                                 ApiMethod.ATTR_ISUSER,
                                 Misc.getProperty(props,
                                     ApiMethod.ATTR_ISUSER, false));

        boolean requiresAuthToken = XmlUtil.getAttributeFromTree(node,
                                        ApiMethod.ATTR_REQUIRESAUTHTOKEN,
                                        Misc.getProperty(props,
                                            ApiMethod.ATTR_REQUIRESAUTHTOKEN,
                                            false));

        String handlerName = XmlUtil.getAttributeFromTree(node,
                                 ApiMethod.ATTR_HANDLER,
                                 Misc.getProperty(props,
                                     ApiMethod.ATTR_HANDLER, defaultHandler));

        String handlerId = XmlUtil.getAttributeFromTree(node,
                               ApiMethod.ATTR_ID, handlerName);
        RequestHandler handler = (RequestHandler) handlers.get(handlerId);

        if (handler == null) {
            handler = getRepository();
            if (handlerName.equals("usermanager")) {
                handler = getRepository().getUserManager();
            } else if (handlerName.equals("monitormanager")) {
                handler = getRepository().getMonitorManager();
            } else if (handlerName.equals("admin")) {
                handler = getRepository().getAdmin();
            } else if (handlerName.equals("logmanager")) {
                handler = getRepository().getLogManager();
            } else if (handlerName.equals("harvestermanager")) {
                handler = getRepository().getHarvesterManager();
            } else if (handlerName.equals("localrepositorymanager")) {
                handler = getRepository().getLocalRepositoryManager();
            } else if (handlerName.equals("actionmanager")) {
                handler = getRepository().getActionManager();
            } else if (handlerName.equals("graphmanager")) {
                handler =
                    getRepository()
                        .getOutputHandler(org.ramadda.repository.output
                            .GraphOutputHandler.OUTPUT_GRAPH);
            } else if (handlerName.equals("accessmanager")) {
                handler = getRepository().getAccessManager();
            } else if (handlerName.equals("llmmanager")) {
                handler = getRepository().getLLMManager();		
            } else if (handlerName.equals("searchmanager")) {
                handler = getRepository().getSearchManager();
            } else if (handlerName.equals("datehandler")) {
                handler = getRepository().getDateHandler();		
            } else if (handlerName.equals("entrymanager")) {
                handler = getRepository().getEntryManager();
            } else if (handlerName.equals("exteditor")) {
                handler = getRepository().getExtEditor();
            } else if (handlerName.equals("mapmanager")) {
                handler = getRepository().getMapManager();
            } else if (handlerName.equals("wikimanager")) {
                handler = getRepository().getWikiManager();
            } else if (handlerName.equals("jobmanager")) {
                handler = getRepository().getJobManager();
            } else if (handlerName.equals("associationmanager")) {
                handler = getRepository().getAssociationManager();
            } else if (handlerName.equals("commentmanager")) {
                handler = getRepository().getCommentManager();
            } else if (handlerName.equals("metadatamanager")) {
                handler = getRepository().getMetadataManager();
            } else if (handlerName.equals("registrymanager")) {
                handler = getRepository().getRegistryManager();
            } else if (handlerName.equals("repository")) {
                handler = getRepository();
            } else {
                Class       handlerClass = Misc.findClass(handlerName);
                Constructor ctor         = null;
                Object[]    params       = null;

                ctor = Misc.findConstructor(handlerClass,
                                            new Class[] { Repository.class,
                        Element.class, Hashtable.class });
                params = new Object[] { getRepository(), node, props };

                if (ctor == null) {
                    ctor = Misc.findConstructor(handlerClass,
                            new Class[] { Repository.class,
                                          Element.class });
                    params = new Object[] { getRepository(), node };
                }

                if (ctor == null) {
                    ctor = Misc.findConstructor(handlerClass,
                            new Class[] { Repository.class });
                    params = new Object[] { getRepository() };
                }

                if (ctor == null) {
                    throw new IllegalStateException("Could not find ctor:"
                            + handlerClass.getName());
                }
                handler = (RequestHandler) ctor.newInstance(params);
            }
            if (handler == null) {
                getLogManager().logInfo("Could not find handler for:"
                                        + handlerName + ":");

                return;
            }
            handlers.put(handlerId, handler);
        }

        String    url       = request;
        ApiMethod oldMethod = requestMap.get(url);
        if (oldMethod != null) {
            requestMap.remove(url);
        }

        Class[] paramTypes = new Class[] { Request.class };
        Method method = Misc.findMethod(handler.getClass(), methodName,
                                        paramTypes);
        if (method == null) {
            throw new IllegalArgumentException("Unknown request method:"
                    + methodName + " in class:"
                    + handler.getClass().getName());
        }

        String icon = XmlUtil.getAttribute(node, ApiMethod.ATTR_ICON,
                                           (String) null);
        ApiMethod apiMethod =
            new ApiMethod(getRepository(), handler, request,
                          XmlUtil.getAttribute(node, ApiMethod.ATTR_NAME,
                              request), method, admin, mustBeUser,
                                        requiresAuthToken, needsSsl,
                                        authMethod, checkAuthMethod,
			  checkIsHuman,
			  XmlUtil.getAttribute(node,
                                            ApiMethod.ATTR_TOPLEVEL,
                                            false), icon);
        List actions = Utils.split(XmlUtil.getAttribute(node,
                           ApiMethod.ATTR_ACTIONS, BLANK), ",", true, true);
        if ( !Permission.isValidActions(actions)) {
            throw new IllegalArgumentException("Bad actions:" + actions
                    + " for api method:" + apiMethod.getName());
        }
        apiMethod.setActions(actions);
        if (XmlUtil.getAttribute(node, ApiMethod.ATTR_ISHOME, false)) {
            homeApi = apiMethod;
        }
        requestMap.put(url, apiMethod);
        if (oldMethod != null) {
            int index = apiMethods.indexOf(oldMethod);
            apiMethods.remove(index);
            apiMethods.add(index, apiMethod);
            if (apiMethod.isWildcard()) {
                index = wildCardApiMethods.indexOf(oldMethod);
                wildCardApiMethods.remove(index);
                wildCardApiMethods.add(index, apiMethod);
            }
        } else {
            apiMethods.add(apiMethod);
            if (apiMethod.isWildcard()) {
                wildCardApiMethods.add(apiMethod);
            }
        }
        if (apiMethod.getIsTopLevel()
                && !topLevelMethods.contains(apiMethod)) {
            topLevelMethods.add(apiMethod);
        }
    }

    public void loadApi() throws Exception {
        for (String file : getPluginManager().getApiDefFiles()) {
            file = getStorageManager().localizePath(file);
            if (getPluginManager().haveSeen(file)) {
                continue;
            }
            Element   apiRoot = XmlUtil.getRoot(file, getClass());
            Hashtable props   = new Hashtable();
            try {
                processApiNode(apiRoot, apiHandlers, props, "repository");
            } catch (Exception exc) {
                getLogManager().logError("Error processing API node:" + file
                                         + "\n", exc);
            }
        }
    }

    public ArrayList<ApiMethod> getTopLevelMethods() {
        return topLevelMethods;
    }

    public ApiMethod getHomeApi() {
        return homeApi;
    }

    public RequestHandler getApiHandler(String id) {
        return apiHandlers.get(id);
    }

    public List<ApiMethod> getApiMethods() {
        return apiMethods;
    }

    private void processApiNode(Element apiRoot, Hashtable handlers,
                                Hashtable props, String defaultHandler)
            throws Exception {
        if (apiRoot == null) {
            return;
        }
        NodeList children = XmlUtil.getElements(apiRoot);
        for (int i = 0; i < children.getLength(); i++) {
            Element node = (Element) children.item(i);
            String  tag  = node.getTagName();
            if (tag.equals(ApiMethod.TAG_PROPERTY)) {
                props.put(XmlUtil.getAttribute(node, ApiMethod.ATTR_NAME),
                          XmlUtil.getAttribute(node, ApiMethod.ATTR_VALUE));
            } else if (tag.equals(ApiMethod.TAG_API)) {
                addRequest(node, props, handlers, defaultHandler);
            } else if (tag.equals(ApiMethod.TAG_GROUP)) {
                processApiNode(node, handlers, props,
                               XmlUtil.getAttribute(node,
                                   ApiMethod.ATTR_HANDLER, defaultHandler));
            } else {
                throw new IllegalArgumentException("Unknown api.xml tag:"
                        + tag);
            }
        }
    }

    protected ApiMethod findApiMethod(Request request) throws Exception {
        String incoming = request.getRequestPath().trim();
        if (incoming.endsWith("/")) {
            incoming = incoming.substring(0, incoming.length() - 1);
        }
        String urlBase = getRepository().getUrlBase();
        if (incoming.equals("/") || incoming.equals("")) {
            incoming = urlBase;
        }
        while (incoming.startsWith("//")) {
            incoming = incoming.substring(1);
        }
        if ( !incoming.startsWith(urlBase)) {
            //check for top-level apis
            ApiMethod apiMethod = (ApiMethod) requestMap.get(incoming);
	    if(apiMethod==null)
		apiMethod = findWildcard(incoming);
            return apiMethod;
        }
        incoming = incoming.substring(urlBase.length());
        if (incoming.length() == 0) {
            return homeApi;
        }

        ApiMethod apiMethod = (ApiMethod) requestMap.get(incoming);
        if (apiMethod == null) {
	    apiMethod = findWildcard(incoming);
        }
        if ((apiMethod == null) && incoming.equals(urlBase)) {
            apiMethod = homeApi;
        }

        return apiMethod;
    }

    private ApiMethod findWildcard(String incoming) {
	for (ApiMethod tmp : wildCardApiMethods) {
	    if (incoming.startsWith(tmp.getWildcardPath1())) {
		return tmp;
	    }
	    if (incoming.equals(tmp.getWildcardPath2())) {
		return tmp;
            }
	}
	return null;
    }	

    public ApiMethod getApiMethod(String path) {
        return requestMap.get(path);
    }

}
