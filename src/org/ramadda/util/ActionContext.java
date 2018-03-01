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

package org.ramadda.util;


/**
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.30 $
 */
public abstract class ActionContext {

    /** _more_ */
    private String actionID;

    /** _more_ */
    private String status;

    /**
     * _more_
     */
    public ActionContext() {}


    /**
     * _more_
     *
     * @param actionID _more_
     */
    public ActionContext(String actionID) {
        this.actionID = actionID;
    }

    /**
     * _more_
     *
     * @param message _more_
     */
    public void setStatus(String message) {
        this.status = message;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getStatus() {
        return status;
    }


    /**
     *
     * @return _more_
     */
    public String toString() {
        return getStatus();
    }


}
