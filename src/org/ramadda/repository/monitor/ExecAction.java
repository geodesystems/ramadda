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
public class ExecAction extends MonitorAction {

    /** _more_ */
    public static final String PROP_EXEC_EXECLINE = "exec.execline";

    /** _more_ */
    private String execLine;


    /**
     * _more_
     */
    public ExecAction() {}

    /**
     * _more_
     *
     * @param id _more_
     */
    public ExecAction(String id) {
        super(id);
    }


    /**
     * _more_
     *
     * @param repository _more_
     *
     * @return _more_
     */
    public boolean enabled(Repository repository) {
        return repository.getProperty(PROP_MONITOR_ENABLE_EXEC, false);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean adminOnly() {
        return true;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getActionName() {
        return "exec";
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getActionLabel() {
        return "Exec Action";
    }

    /**
     * _more_
     *
     *
     * @param entryMonitor _more_
     * @return _more_
     */
    public String getSummary(EntryMonitor entryMonitor) {
        return "Execute external program on server";
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param monitor _more_
     */
    public void applyEditForm(Request request, EntryMonitor monitor) {
        super.applyEditForm(request, monitor);
        this.execLine = request.getString(getArgId(PROP_EXEC_EXECLINE), "");
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
        sb.append(HtmlUtils.colspan("Exec Action", 2));

        sb.append(
            HtmlUtils.formEntry(
                "Execute:",
                HtmlUtils.input(
                    getArgId(PROP_EXEC_EXECLINE), execLine,
                    HtmlUtils.SIZE_60) + HtmlUtils.title(macroTooltip)));
        sb.append(HtmlUtils.formTableClose());
    }


    /**
     * _more_
     *
     *
     * @param monitor _more_
     * @param entry _more_
     */
    protected void entryMatched(EntryMonitor monitor, Entry entry) {
        if ( !monitor.getRepository().getProperty(PROP_MONITOR_ENABLE_EXEC,
                false)) {
            throw new IllegalArgumentException("Exec action not enabled");
        }
        Resource resource = entry.getResource();
        String command =
            monitor.getRepository().getEntryManager().replaceMacros(entry,
                execLine);
        try {
            Process process = Runtime.getRuntime().exec(command);
            int     result  = process.waitFor();
            if (result == 0) {
                monitor.getRepository().getLogManager().logInfo(
                    "ExecMonitor executed:" + command);
            } else {
                try {
                    InputStream is    = process.getErrorStream();
                    byte[]      bytes = IOUtil.readBytes(is);
                    monitor.getRepository().getLogManager().logError(
                        "ExecMonitor failed executing:" + command + "\n"
                        + new String(bytes));
                } catch (Exception noop) {
                    monitor.getRepository().getLogManager().logError(
                        "ExecMonitor failed:" + command);
                }
            }
        } catch (Exception exc) {
            monitor.handleError("Error execing monitor", exc);
        }
    }

    /**
     * Set the ExecLine property.
     *
     * @param value The new value for ExecLine
     */
    public void setExecLine(String value) {
        execLine = value;
    }

    /**
     * Get the ExecLine property.
     *
     * @return The ExecLine
     */
    public String getExecLine() {
        return execLine;
    }



}
