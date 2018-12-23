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

package org.ramadda.repository.auth;


import org.ramadda.repository.*;


/**
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
public class AuthorizationMethod {

    /** _more_ */
    private static final String TYPE_HTTPAUTH = "httpauth";

    /** _more_ */
    private static final String TYPE_HTML = "html";

    /** _more_ */
    private String type = TYPE_HTML;

    /** _more_ */
    public static final AuthorizationMethod AUTH_HTTP =
        new AuthorizationMethod(TYPE_HTTPAUTH);

    /** _more_ */
    public static final AuthorizationMethod AUTH_HTML =
        new AuthorizationMethod(TYPE_HTML);

    /**
     * _more_
     *
     *
     * @param type _more_
     */
    private AuthorizationMethod(String type) {
        this.type = type;
    }


    /**
     * _more_
     *
     * @param method _more_
     *
     * @return _more_
     */
    public static AuthorizationMethod getMethod(String method) {
        if (method.equals(TYPE_HTTPAUTH)) {
            return AUTH_HTTP;
        }

        return AUTH_HTML;
    }

    /**
     * _more_
     *
     * @param o _more_
     *
     * @return _more_
     */
    public boolean equals(Object o) {
        if ( !(o instanceof AuthorizationMethod)) {
            return false;
        }
        AuthorizationMethod that = (AuthorizationMethod) o;

        return this.type.equals(that.type);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return type;
    }

}
