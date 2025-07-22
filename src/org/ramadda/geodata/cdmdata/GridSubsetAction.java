/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.cdmdata;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.client.RepositoryClient;


import org.ramadda.repository.monitor.*;
import org.ramadda.repository.type.TypeHandler;
import org.ramadda.util.HtmlUtils;

import org.ramadda.util.Utils;


import ucar.unidata.util.StringUtil;
import ucar.unidata.util.IOUtil;

import java.io.File;

import java.net.URL;

import java.util.ArrayList;
import java.util.List;


/**
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.30 $
 */
public class GridSubsetAction extends MonitorAction {

    /**  */
    public static final String ARG_ARGS = "args";



    /**  */
    private String args = "";


    /**
     * _more_
     */
    public GridSubsetAction() {}

    /**
     * _more_
     *
     * @param id _more_
     */
    public GridSubsetAction(String id) {
        super(id);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getActionName() {
        return "gridsubset";
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getActionLabel() {
        return "Grid Subset Action";
    }

    /**
     * _more_
     *
     *
     * @param entryMonitor _more_
     * @return _more_
     */
    public String getSummary(EntryMonitor entryMonitor) {
        return "Subset Grid Entry";
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param monitor _more_
     */
    public void applyEditForm(Request request, EntryMonitor monitor) {
        super.applyEditForm(request, monitor);
        applyGroupEditForm(request, monitor);
        args = request.getString(getArgId(ARG_ARGS), args).trim();
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
        addGroupToEditForm(monitor, sb);
	addPathTemplateEditForm(request, monitor, sb);
        sb.append(
            HtmlUtils.formEntryTop(
                "Grid Subset Arguments:",
                HtmlUtils.table(new Object[]{
			 HtmlUtils.textArea(getArgId(ARG_ARGS), args, 5, 60),
			 "Arguments from the grid subset form<br>Use allvars=true to select all of the variables"})));

        sb.append(HtmlUtils.formTableClose());
    }

    /**
     *
     * @param repository _more_
      * @return _more_
     *
     * @throws Exception _more_
     */
    public CdmDataOutputHandler getCDO(Repository repository)
            throws Exception {
        return ((CdmDataOutputHandler) repository.getOutputHandler(
            CdmDataOutputHandler.OUTPUT_OPENDAP.toString()));
    }


    /**
     *
     * @param repository _more_
      * @return _more_
     *
     * @throws Exception _more_
     */
    public CdmManager getCdmManager(Repository repository) throws Exception {
        return getCDO(repository).getCdmManager();
    }

    /**
     * _more_
     *
     *
     * @param monitor _more_
     * @param entry _more_
     * @param isNew _more_
     */
    @Override
    public void entryMatched(EntryMonitor monitor, Entry entry,
                             boolean isNew) {
        try {
            CdmDataOutputHandler cdo        = getCDO(monitor.getRepository());
            CdmManager           cdmManager = cdo.getCdmManager();
            if ( !cdmManager.canLoadAsGrid(entry)) {
                monitor.logError(this,
				 "Grid Subset Action:" + " Entry is not a grid:" + entry);
                return;
            }
            Entry group = getGroup(monitor);
            if (group == null) {
                monitor.logError(this,
				 "Grid Subset Action:" + " no parent entry specified");
                return;
            }

            if ( !Utils.stringDefined(args)) {
		args = "allvars=true";
	    }

	    /*
            if ( !Utils.stringDefined(args)) {
                monitor.logError(this,
				 "Grid Subset Action:" + " no args specified");

                return;
            }
	    */

            Request   request = monitor.getRepository().getAdminRequest();
	    for(String line:Utils.split(args,"\n",true,true)) {
		List<String> toks = StringUtil.splitUpTo(line,"=",2);
		String name = toks.get(0);
		String value = toks.size()>1?toks.get(1):"";
                if (name.equals("entryid")) {
                    continue;
                }
		request.put(name, value, false);
            }

            String fileName =
                IOUtil.stripExtension(entry.getFile().getName())
                + "_subset.nc";
            File file =
                monitor.getRepository().getStorageManager().getTmpFile(
                    request, fileName);

            request.putExtraProperty("subsetfile", file);
            request.putExtraProperty("internal", "true");	    
	    try {
		cdo.outputGridSubset(request, entry);
	    } catch(Exception exc) {
		monitor.logError(this,"GridSubsetAction error:" + request,exc);
		return;
	    }

            file = monitor.getRepository().getStorageManager().moveToStorage(
                request, file, fileName);
            String newName = entry.getName() + " Subset";
            TypeHandler gridTypeHandler =
                monitor.getRepository().getTypeHandler("cdm_grid");
            Entry newEntry =
                monitor.getRepository().getEntryManager().addFileEntry(
								       request, file, group, getPathTemplate(),
								       newName, "", request.getUser(),
                    gridTypeHandler, null);
            monitor.logInfo(this,
			    "Grid Subset published:" + newEntry.getName() + " id:"
			    + newEntry.getId());
        } catch (Exception exc) {
            monitor.handleError("Error handling Grid Subset Action", exc);
        }
    }




    /**
     * Set the Args property.
     *
     * @param value The new value for Args
     */
    public void setArgs(String value) {
        args = value;
    }

    /**
     * Get the Args property.
     *
     * @return The Args
     */
    public String getArgs() {
        return args;
    }

    public void setArgJson(String tmp) {
    }

}
