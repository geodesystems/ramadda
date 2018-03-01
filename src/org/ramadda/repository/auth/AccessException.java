/*
* Copyright (c) 2008-2018 Geode Systems LLC
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
 * Class AccessException _more_
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
public class AccessException extends RuntimeException {

    /** _more_ */
    Request request;


    /**
     * _more_
     *
     * @param message _more_
     * @param request _more_
     */
    public AccessException(String message, Request request) {
        super(message);
        this.request = request;
    }

    /**
     * Set the Request property.
     *
     * @param value The new value for Request
     */
    public void setRequest(Request value) {
        this.request = value;
    }

    /**
     * Get the Request property.
     *
     * @return The Request
     */
    public Request getRequest() {
        return this.request;
    }




}
