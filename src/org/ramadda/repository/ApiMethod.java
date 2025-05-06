/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository;

import org.ramadda.repository.auth.*;

import ucar.unidata.util.Counter;

import java.lang.reflect.Method;

import java.util.ArrayList;

import java.util.Hashtable;
import java.util.List;

/**
 */

public class ApiMethod {

    public static final String TAG_API = "api";

    public static final String TAG_GROUP = "group";

    public static final String TAG_PROPERTY = "property";

    public static final String ATTR_REQUEST = "request";

    public static final String ATTR_NEEDS_SSL = "needs_ssl";

    public static final String ATTR_CHECKAUTHMETHOD = "checkauthmethod";

    public static final String ATTR_AUTHMETHOD = "authmethod";

    public static final String ATTR_HANDLER = "handler";

    public static final String ATTR_ACTIONS = "actions";

    public static final String ATTR_TOPLEVEL = "toplevel";

    public static final String ATTR_NAME = "name";

    public static final String ATTR_ICON = "icon";

    public static final String ATTR_ID = "id";

    public static final String ATTR_VALUE = "value";

    public static final String ATTR_METHOD = "method";

    public static final String ATTR_ADMIN = "admin";

    public static final String ATTR_ISUSER = "isuser";

    public static final String ATTR_POST = "post";

    public static final String ATTR_REQUIRESAUTHTOKEN = "requires_auth_token";

    public static final String ATTR_ISHOME = "ishome";

    public static final String ATTR_HANDLESHEAD = "head";

    private String request;

    private String name;

    private String icon;

    private boolean isTopLevel = false;

    private boolean mustBeAdmin = true;

    private boolean mustBeUser = false;

    private boolean mustBePost = false;

    private boolean requiresAuthToken = false;

    private boolean checkIsHuman = false;

    private RequestHandler requestHandler;

    private Method method;

    private List actions;

    private Repository repository;

    private RequestUrl url;

    private boolean needsSsl = false;

    private boolean checkAuthMethod = false;

    private String authMethod;

    private boolean handlesHead = false;

    private Counter numberOfCalls = new Counter();

    public ApiMethod(Repository repository, RequestHandler requestHandler,
                     String request, String name, Method method,
                     boolean mustBeAdmin, boolean mustBeUser,
                     boolean requiresAuthToken, boolean needsSsl,
                     String authMethod, boolean checkAuthMethod,
		     boolean checkIsHuman,
		     
                     boolean isTopLevel, String icon) {
        this.repository        = repository;
        this.requestHandler    = requestHandler;
        this.request           = request;
        this.name              = name;
        this.mustBeAdmin       = mustBeAdmin;
        this.mustBeUser        = mustBeUser;
        this.requiresAuthToken = requiresAuthToken;
        this.needsSsl          = needsSsl;
        this.authMethod        = authMethod;
        this.checkAuthMethod   = checkAuthMethod;
	this.checkIsHuman      = checkIsHuman;
        this.method            = method;
        this.isTopLevel        = isTopLevel;
        this.icon              = icon;
    }

    public boolean isWildcard() {
        return request.endsWith("/*");
    }

    public boolean isRequestOk(Request request, Repository repository)
            throws Exception {
        User user = request.getUser();
        if (mustBeUser) {
            if (user.getAnonymous()) {
                return false;
            }
        }

        if (mustBeAdmin) {
            if (repository.isReadOnly()) {
                return false;
            }
            if ( !user.getAdmin()) {
                return false;
            }
        }
        if (requiresAuthToken) {
            repository.getAuthManager().ensureAuthToken(request);
        }
        if (actions.size() > 0) {
            for (int i = 0; i < actions.size(); i++) {
                if ( !repository.getAccessManager().canDoAction(request,
                        (String) actions.get(i))) {
                    return false;
                }
            }
        }

        return true;
    }

    public void printDebug(Request request) throws Exception {
        System.err.println("Api method requiresAuthToken="
                           + requiresAuthToken + " must be admin="
                           + mustBeAdmin);
        for (int i = 0; i < actions.size(); i++) {
            if ( !repository.getAccessManager().canDoAction(request,
                    (String) actions.get(i))) {
                System.err.println("can't do action:" + actions.get(i));
            }
        }
    }

    public String toString() {
        return request;
    }

    public Result invoke(Request request) throws Exception {
        return (Result) getMethod().invoke(requestHandler,
                                           new Object[] { request });
    }

    public RequestHandler getRequestHandler() {
        return requestHandler;
    }

    /**
     * Set the Request property.
     *
     * @param value The new value for Request
     */
    public void setRequest(String value) {
        request = value;
    }

    /**
     * Get the Request property.
     *
     * @return The Request
     */
    public String getRequest() {
        return request;
    }

    private String wildcardPath1;

    private String wildcardPath2;

    public String getWildcardPath1() {
        if (wildcardPath1 == null) {
            wildcardPath1 = request.substring(0, request.length() - 1);
        }

        return wildcardPath1;
    }

    public String getWildcardPath2() {
        if (wildcardPath2 == null) {
            wildcardPath2 = request.substring(0, request.length() - 2);
        }

        return wildcardPath2;
    }

    public RequestUrl getUrl() {
        if (url == null) {
            url = new RequestUrl(repository, request);
        }

        return url;
    }

    /**
     * Set the Method property.
     *
     * @param value The new value for Method
     */
    public void setMethod(Method value) {
        method = value;
    }

    /**
     * Get the Method property.
     *
     * @return The Method
     */
    public Method getMethod() {
        return method;
    }

    /**
     * Set the Name property.
     *
     * @param value The new value for Name
     */
    public void setName(String value) {
        name = value;
    }

    /**
     * Get the Name property.
     *
     * @return The Name
     */
    public String getName() {
        return name;
    }

    /**
     * Set the IsTopLevel property.
     *
     * @param value The new value for IsTopLevel
     */
    public void setIsTopLevel(boolean value) {
        isTopLevel = value;
    }

    public boolean getCheckIsHuman() {
	return checkIsHuman;
    }


    /**
     * Get the IsTopLevel property.
     *
     * @return The IsTopLevel
     */
    public boolean getIsTopLevel() {
        return isTopLevel;
    }

    /**
     * Set the MustBeAdmin property.
     *
     * @param value The new value for MustBeAdmin
     */
    public void setMustBeAdmin(boolean value) {
        mustBeAdmin = value;
    }

    /**
     * Get the MustBeAdmin property.
     *
     * @return The MustBeAdmin
     */
    public boolean getMustBeAdmin() {
        return mustBeAdmin;
    }

    /**
     * Set the RequiresAuthToken property.
     *
     * @param value The new value for RequiresAuthToken
     */
    public void setRequiresAuthToken(boolean value) {
        requiresAuthToken = value;
    }

    /**
     * Get the RequiresAuthToken property.
     *
     * @return The RequiresAuthToken
     */
    public boolean getRequiresAuthToken() {
        return requiresAuthToken;
    }

    /**
     * Set the Actions property.
     *
     * @param value The new value for Actions
     */
    public void setActions(List value) {
        actions = value;
    }

    /**
     * Get the Actions property.
     *
     * @return The Actions
     */
    public List getActions() {
        return actions;
    }

    /**
     * Set the NeedsSsl property.
     *
     * @param value The new value for NeedsSsl
     */
    public void setNeedsSsl(boolean value) {
        this.needsSsl = value;
    }

    /**
     * Get the NeedsSsl property.
     *
     * @return The NeedsSsl
     */
    public boolean getNeedsSsl() {
        return this.needsSsl;
    }

    /**
     * Set the CheckAuthMethod property.
     *
     * @param value The new value for CheckAuthMethod
     */
    public void setCheckAuthMethod(boolean value) {
        this.checkAuthMethod = value;
    }

    /**
     * Get the CheckAuthMethod property.
     *
     * @return The CheckAuthMethod
     */
    public boolean getCheckAuthMethod() {
        return this.checkAuthMethod;
    }

    /**
     * Set the AuthMethod property.
     *
     * @param value The new value for AuthMethod
     */
    public void setAuthMethod(String value) {
        this.authMethod = value;
    }

    /**
     * Get the AuthMethod property.
     *
     * @return The AuthMethod
     */
    public String getAuthMethod() {
        return this.authMethod;
    }

    public int getNumberOfCalls() {
        return numberOfCalls.getCount();
    }

    public void incrNumberOfCalls() {
        numberOfCalls.incr();
    }

    /**
     *  Set the Icon property.
     *
     *  @param value The new value for Icon
     */
    public void setIcon(String value) {
        icon = value;
    }

    /**
     *  Get the Icon property.
     *
     *  @return The Icon
     */
    public String getIcon() {
        return icon;
    }

}
