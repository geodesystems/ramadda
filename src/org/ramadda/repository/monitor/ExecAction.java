/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.monitor;

import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;

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

    public static final String PROP_EXEC_EXECLINE = "exec.execline";

    private String execLine;

    public ExecAction() {}

    public ExecAction(String id) {
        super(id);
    }

    public boolean enabled(Repository repository) {
        return repository.getProperty(PROP_MONITOR_ENABLE_EXEC, false);
    }

    public boolean adminOnly() {
        return true;
    }

    public String getActionName() {
        return "exec";
    }

    public String getActionLabel() {
        return "Exec Action";
    }

    public String getSummary(EntryMonitor entryMonitor) {
        return "Execute external program on server";
    }

    public void applyEditForm(Request request, EntryMonitor monitor) {
        super.applyEditForm(request, monitor);
        this.execLine = request.getString(getArgId(PROP_EXEC_EXECLINE), "");
    }

    @Override
    public void addToEditForm(Request request,EntryMonitor monitor, Appendable sb)
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
	    //Assume it  is space delimited
	    List<String> commands = Utils.split(command," ",true,true);
	    ProcessBuilder pb = new ProcessBuilder(commands);
	    Process     process = pb.start();
	    //            Process process = Runtime.getRuntime().exec(command);
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
