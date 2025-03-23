/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
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
