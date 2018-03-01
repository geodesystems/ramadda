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

package org.ramadda.repository.monitor;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;

import org.ramadda.util.HtmlUtils;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;


import java.io.File;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;


/**
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.30 $
 */
public class CopyAction extends MonitorAction {

    /** _more_ */
    public static final String ARG_SUBGROUP = "subgroup";


    /** _more_ */
    private String parentGroupId;

    /** _more_ */
    private String subGroup = "";

    /** _more_ */
    private Entry group;

    /**
     * _more_
     */
    public CopyAction() {}

    /**
     * _more_
     *
     * @param id _more_
     */
    public CopyAction(String id) {
        super(id);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getActionName() {
        return "copy";
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getActionLabel() {
        return "Copy Action";
    }

    /**
     * _more_
     *
     * @param entryMonitor _more_
     *
     * @return _more_
     */
    private Entry getGroup(EntryMonitor entryMonitor) {
        try {
            if (group == null) {
                group =
                    (Entry) entryMonitor.getRepository().getEntryManager()
                        .findGroup(null, parentGroupId);
            }

            return group;
        } catch (Exception exc) {
            return null;
        }
    }

    /**
     * _more_
     *
     *
     * @param entryMonitor _more_
     * @return _more_
     */
    public String getSummary(EntryMonitor entryMonitor) {
        Entry group = getGroup(entryMonitor);
        if (group == null) {
            return "Copy entry: Error bad folder";
        }

        return "Copy entry to:" + group.getName();
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param monitor _more_
     */
    public void applyEditForm(Request request, EntryMonitor monitor) {
        super.applyEditForm(request, monitor);
        this.parentGroupId = request.getString(getArgId(ARG_GROUP)
                + "_hidden", "");
        this.group    = null;
        this.subGroup = request.getString(getArgId(ARG_SUBGROUP), "").trim();
    }


    /**
     * _more_
     *
     * @param monitor _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void addToEditForm(EntryMonitor monitor, Appendable sb)
            throws Exception {
        sb.append(HtmlUtils.formTable());
        sb.append(HtmlUtils.colspan("Copy Action", 2));
        try {
            Entry  group      = getGroup(monitor);
            String errorLabel = "";
            if ((group != null) && !monitor.okToAddNew(group)) {
                errorLabel = HtmlUtils.span(
                    monitor.getRepository().msg(
                        "You cannot add to the folder"), HtmlUtils.cssClass(
                        HtmlUtils.CLASS_ERRORLABEL));
            }
            String groupName = ((group != null)
                                ? group.getFullName()
                                : "");
            String inputId   = getArgId(ARG_GROUP);
            String select =
                monitor.getRepository().getHtmlOutputHandler().getSelect(
                    null, inputId,
                    HtmlUtils.img(
                        monitor.getRepository().iconUrl(
                            ICON_FOLDER_OPEN)) + HtmlUtils.space(1)
                                + monitor.getRepository().msg(
                                    "Select"), false, "");
            sb.append(HtmlUtils.hidden(inputId + "_hidden", parentGroupId,
                                       HtmlUtils.id(inputId + "_hidden")));
            sb.append(
                HtmlUtils.formEntry(
                    "Folder:",
                    HtmlUtils.disabledInput(
                        inputId, groupName,
                        HtmlUtils.SIZE_60 + HtmlUtils.id(inputId)) + select));
            sb.append(
                HtmlUtils.formEntry(
                    "Sub-Folder Template:",
                    HtmlUtils.input(
                        getArgId(ARG_SUBGROUP), subGroup,
                        HtmlUtils.SIZE_60)));
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
        sb.append(HtmlUtils.formTableClose());
    }


    /**
     * _more_
     *
     *
     * @param monitor _more_
     * @param entry _more_
     * @param isNew _more_
     */
    public void entryMatched(EntryMonitor monitor, Entry entry,
                             boolean isNew) {
        try {
            Entry group = getGroup(monitor);
            if (group == null) {
                return;
            }
        } catch (Exception exc) {
            monitor.handleError("Error handling Copy Action", exc);
        }
    }


    /**
     *  Set the ParentGroupId property.
     *
     *  @param value The new value for ParentGroupId
     */
    public void setParentGroupId(String value) {
        this.parentGroupId = value;
    }

    /**
     *  Get the ParentGroupId property.
     *
     *  @return The ParentGroupId
     */
    public String getParentGroupId() {
        return this.parentGroupId;
    }

    /**
     *  Set the SubGroup property.
     *
     *  @param value The new value for SubGroup
     */
    public void setSubGroup(String value) {
        this.subGroup = value;
    }

    /**
     *  Get the SubGroup property.
     *
     *  @return The SubGroup
     */
    public String getSubGroup() {
        return this.subGroup;
    }



}
