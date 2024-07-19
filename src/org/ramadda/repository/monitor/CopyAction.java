/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
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
    @Override
    public void applyEditForm(Request request, EntryMonitor monitor) {
        super.applyEditForm(request, monitor);
	applyGroupEditForm(request, monitor);
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
    public void addToEditForm(Request request,EntryMonitor monitor, Appendable sb)
            throws Exception {
        sb.append(HtmlUtils.formTable());
        sb.append(HtmlUtils.colspan("Copy Action", 2));
        try {
	    addGroupToEditForm(monitor, sb);
	    addPathTemplateEditForm(request, monitor, sb);
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
	    List<Entry> entries = new ArrayList<Entry>();
	    entries.add(entry);
	    monitor.getRepository().getEntryManager().processEntryCopyAsynch(monitor.getRequest(),  group, getPathTemplate(), entries, null, null);
        } catch (Exception exc) {
            monitor.handleError("Error handling Copy Action", exc);
        }
    }


}
