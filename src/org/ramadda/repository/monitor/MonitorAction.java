// Copyright (c) 2008-2021 Geode Systems LLC
// SPDX-License-Identifier: Apache-2.0

package org.ramadda.repository.monitor;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;


/**
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.30 $
 */
public abstract class MonitorAction implements Constants, Cloneable {

    /** _more_ */
    public static final String macroTooltip =
        "macros: ${entryid} ${resourcepath} ${resourcename} ${fileextension} ${from_day}  ${from_month} ${from_year} ${from_monthname}  <br>"
        + "${to_day}  ${to_month} ${to_year} ${to_monthname}";



    /** _more_ */
    private String id;


    /**
     * _more_
     */
    public MonitorAction() {}


    /**
     * _more_
     *
     * @param id _more_
     */
    public MonitorAction(String id) {
        this.id = id;
    }

    /**
     * _more_
     *
     * @return _more_
     *
     * @throws CloneNotSupportedException _more_
     */
    public MonitorAction cloneMe() throws CloneNotSupportedException {
        return (MonitorAction) super.clone();
    }

    /**
     * _more_
     *
     * @param repository _more_
     *
     * @return _more_
     */
    public boolean enabled(Repository repository) {
        return true;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean adminOnly() {
        return false;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public abstract String getActionLabel();


    /**
     * _more_
     *
     * @return _more_
     */
    public abstract String getActionName();


    /**
     * _more_
     *
     *
     * @param entryMonitor _more_
     * @return _more_
     */
    public String getSummary(EntryMonitor entryMonitor) {
        return getActionName();
    }

    /**
     * _more_
     *
     * @param prefix _more_
     *
     * @return _more_
     */
    protected String getArgId(String prefix) {
        return prefix + "_" + id;
    }

    /**
     * _more_
     *
     * @param monitor _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void addToEditForm(EntryMonitor monitor, Appendable sb)
            throws Exception {}

    /**
     * _more_
     *
     * @param request _more_
     * @param monitor _more_
     */
    public void applyEditForm(Request request, EntryMonitor monitor) {}


    /**
     * _more_
     *
     *
     * @param monitor _more_
     * @param entry _more_
     * @param isNew _more_
     */
    public void entryMatched(EntryMonitor monitor, Entry entry,
                             boolean isNew) {}



    /**
     *  Set the Id property.
     *
     *  @param value The new value for Id
     */
    public void setId(String value) {
        id = value;
    }

    /**
     *  Get the Id property.
     *
     *  @return The Id
     */
    public String getId() {
        return id;
    }




}
