/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;

public abstract class ActionContext {
    private String actionID;
    private String status;

    public ActionContext() {}

    public ActionContext(String actionID) {
        this.actionID = actionID;
    }

    public void setStatus(String message) {
        this.status = message;
    }

    public String getStatus() {
        return status;
    }

    public String toString() {
        return getStatus();
    }

}
