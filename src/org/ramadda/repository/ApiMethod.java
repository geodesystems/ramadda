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


import org.ramadda.repository.auth.*;

import ucar.unidata.util.Counter;

import java.lang.reflect.Method;

import java.util.ArrayList;

import java.util.Hashtable;
import java.util.List;


/**
 */

public class ApiMethod {

    /** _more_ */
    public static final String TAG_API = "api";

    /** _more_ */
    public static final String TAG_GROUP = "group";

    /** _more_ */
    public static final String TAG_PROPERTY = "property";


    /** _more_ */
    public static final String ATTR_REQUEST = "request";

    /** _more_ */
    public static final String ATTR_NEEDS_SSL = "needs_ssl";

    /** _more_ */
    public static final String ATTR_CHECKAUTHMETHOD = "checkauthmethod";

    /** _more_ */
    public static final String ATTR_AUTHMETHOD = "authmethod";

    /** _more_ */
    public static final String ATTR_HANDLER = "handler";

    /** _more_ */
    public static final String ATTR_ACTIONS = "actions";

    /** _more_ */
    public static final String ATTR_TOPLEVEL = "toplevel";

    /** _more_ */
    public static final String ATTR_NAME = "name";

    /** _more_ */
    public static final String ATTR_ICON = "icon";


    /** _more_ */
    public static final String ATTR_ID = "id";

    /** _more_ */
    public static final String ATTR_VALUE = "value";

    /** _more_ */
    public static final String ATTR_METHOD = "method";

    /** _more_ */
    public static final String ATTR_ADMIN = "admin";

    /** _more_ */
    public static final String ATTR_ISUSER = "isuser";

    /** _more_ */
    public static final String ATTR_POST = "post";

    /** _more_ */
    public static final String ATTR_REQUIRESAUTHTOKEN = "requires_auth_token";


    /** _more_ */
    public static final String ATTR_ISHOME = "ishome";

    /** _more_ */
    public static final String ATTR_HANDLESHEAD = "head";

    /** _more_ */
    private String request;

    /** _more_ */
    private String name;

    /** _more_ */
    private String icon;

    /** _more_ */
    private boolean isTopLevel = false;

    /** _more_ */
    private boolean mustBeAdmin = true;

    /** _more_ */
    private boolean mustBeUser = false;

    /** _more_ */
    private boolean mustBePost = false;

    /** _more_ */
    private boolean requiresAuthToken = false;

    /** _more_ */
    private RequestHandler requestHandler;

    /** _more_ */
    private Method method;

    /** _more_ */
    private List actions;

    /** _more_ */
    private Repository repository;

    /** _more_ */
    private RequestUrl url;

    /** _more_ */
    private boolean needsSsl = false;

    /** _more_ */
    private boolean checkAuthMethod = false;

    /** _more_ */
    private String authMethod;

    /** _more_ */
    private boolean handlesHead = false;

    /** _more_ */
    private Counter numberOfCalls = new Counter();


    /**
     * _more_
     *
     *
     *
     * @param repository _more_
     * @param requestHandler _more_
     * @param request _more_
     * @param name _more_
     * @param method _more_
     * @param mustBeAdmin _more_
     * @param mustBeUser _more_
     * @param requiresAuthToken _more_
     * @param needsSsl _more_
     * @param authMethod _more_
     * @param checkAuthMethod _more_
     * @param isTopLevel _more_
     * @param icon _more_
     */
    public ApiMethod(Repository repository, RequestHandler requestHandler,
                     String request, String name, Method method,
                     boolean mustBeAdmin, boolean mustBeUser,
                     boolean requiresAuthToken, boolean needsSsl,
                     String authMethod, boolean checkAuthMethod,
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
        this.method            = method;
        this.isTopLevel        = isTopLevel;
        this.icon              = icon;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isWildcard() {
        return request.endsWith("/*");
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param repository _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
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
            request.ensureAuthToken();
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


    /**
     * _more_
     *
     * @param request _more_
     *
     * @throws Exception _more_
     */
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

    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return request;
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
    public Result invoke(Request request) throws Exception {
        return (Result) getMethod().invoke(requestHandler,
                                           new Object[] { request });
    }


    /**
     * _more_
     *
     * @return _more_
     */
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

    /** _more_ */
    private String wildcardPath1;

    /** _more_ */
    private String wildcardPath2;

    /**
     * _more_
     *
     * @return _more_
     */
    public String getWildcardPath1() {
        if (wildcardPath1 == null) {
            wildcardPath1 = request.substring(0, request.length() - 1);
        }

        return wildcardPath1;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getWildcardPath2() {
        if (wildcardPath2 == null) {
            wildcardPath2 = request.substring(0, request.length() - 2);
        }

        return wildcardPath2;
    }



    /**
     * _more_
     *
     * @return _more_
     */
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

    /**
     * _more_
     *
     * @return _more_
     */
    public int getNumberOfCalls() {
        return numberOfCalls.getCount();
    }

    /**
     * _more_
     */
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
