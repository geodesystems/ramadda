/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.cdmdata;


import org.json.*;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.client.RepositoryClient;


import org.ramadda.repository.monitor.*;
import org.ramadda.repository.type.TypeHandler;
import org.ramadda.util.HtmlUtils;

import org.ramadda.util.JsonUtil;
import org.ramadda.util.Utils;

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
    public static final String ARG_ARGJSON = "argjson";



    /**  */
    private String argJson = "";


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
        argJson = request.getString(getArgId(ARG_ARGJSON), argJson).trim();
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
        addGroupToEditForm(monitor, sb);


        sb.append(
            HtmlUtils.formEntryTop(
                "Grid Subset JSON:",
                HtmlUtils.textArea(getArgId(ARG_ARGJSON), argJson, 5, 60)
                + " " + "Arguments from the grid subset form"));

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
    public void entryMatched(EntryMonitor monitor, Entry entry,
                             boolean isNew) {
        try {
            CdmDataOutputHandler cdo        = getCDO(monitor.getRepository());
            CdmManager           cdmManager = cdo.getCdmManager();
            if ( !cdmManager.canLoadAsGrid(entry)) {
                monitor.getRepository().getLogManager().logError(
                    "Grid Subset Action:" + " Entry is not a grid:" + entry);

                return;
            }
            Entry group = getGroup(monitor);
            if (group == null) {
                monitor.getRepository().getLogManager().logError(
                    "Grid Subset Action:" + " no parent entry specified");

                return;
            }

            if ( !Utils.stringDefined(argJson)) {
                monitor.getRepository().getLogManager().logError(
                    "Grid Subset Action:" + " no arg json specified");

                return;
            }

            Request   request = monitor.getRepository().getAdminRequest();
            JSONArray args    = new JSONArray(argJson);

            for (int i = 0; i < args.length(); i++) {
                JSONObject arg    = args.getJSONObject(i);
                JSONArray  values = arg.getJSONArray("values");
                String     name   = arg.getString("arg");
                if (name.equals("entryid")) {
                    continue;
                }
                for (int j = 0; j < values.length(); j++) {
                    String value = values.getString(j);
                    request.put(name, value, false);
                }
            }

            String fileName =
                IOUtil.stripExtension(entry.getFile().getName())
                + "_subset.nc";
            File file =
                monitor.getRepository().getStorageManager().getTmpFile(
                    request, fileName);


            request.putExtraProperty("subsetfile", file);
            cdo.outputGridSubset(request, entry);
            file = monitor.getRepository().getStorageManager().moveToStorage(
                request, file, fileName);
            String newName = entry.getName() + " Subset";
            TypeHandler gridTypeHandler =
                monitor.getRepository().getTypeHandler("cdm_grid");
            Entry newEntry =
                monitor.getRepository().getEntryManager().addFileEntry(
                    request, file, group, newName, "", request.getUser(),
                    gridTypeHandler, null);
            monitor.getRepository().getLogManager().logInfoAndPrint(
                "Grid Subset published:" + newEntry.getName() + " id:"
                + newEntry.getId());
        } catch (Exception exc) {
            monitor.handleError("Error handling Grid Subset Action", exc);
        }
    }




    /**
     * Set the ArgJson property.
     *
     * @param value The new value for ArgJson
     */
    public void setArgJson(String value) {
        argJson = value;
    }

    /**
     * Get the ArgJson property.
     *
     * @return The ArgJson
     */
    public String getArgJson() {
        return argJson;
    }


}
