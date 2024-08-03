/*
* Copyright (c) 2008-2023 Geode Systems LLC
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

package org.ramadda.geodata.model;


import org.ramadda.repository.ActionManager;
import org.ramadda.repository.Entry;
import org.ramadda.repository.Repository;
import org.ramadda.repository.RepositoryManager;
import org.ramadda.repository.Request;
import org.ramadda.repository.RequestHandler;
import org.ramadda.repository.Result;
import org.ramadda.repository.database.Tables;
import org.ramadda.repository.job.JobManager;
import org.ramadda.repository.metadata.Metadata;
import org.ramadda.repository.type.CollectionTypeHandler;
import org.ramadda.repository.type.Column;
import org.ramadda.service.Service;
import org.ramadda.service.ServiceInput;
import org.ramadda.service.ServiceOperand;
import org.ramadda.service.ServiceOutput;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JQuery;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.Utils;
import org.ramadda.util.sql.Clause;

import org.w3c.dom.Element;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;


import java.io.File;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;


/**
 * An API for doing climate model comparisons
 *
 */
@SuppressWarnings("unchecked")
public class ClimateModelApiHandler extends RepositoryManager implements RequestHandler {

    /** search action */
    public static final String ARG_ACTION_SEARCH = "action.search";

    /** compare action */
    public static final String ARG_ACTION_COMPARE = "action.compare";

    /** multi model compare action */
    public static final String ARG_ACTION_MULTI_COMPARE =
        "action.multicompare";

    /** ensemble compare action */
    public static final String ARG_ACTION_ENS_COMPARE = "action.enscompare";

    /** timeseries action */
    public static final String ARG_ACTION_TIMESERIES = "action.timeseries";

    /** timeseries action */
    public static final String ARG_ACTION_MULTI_TIMESERIES =
        "action.multitimeseries";

    /** correlation action */
    public static final String ARG_ACTION_CORRELATION = "action.correlation";

    /** test action */
    public static final String ARG_ACTION_TEST = "action.test";

    /** fixed collection id */
    public static final String ARG_COLLECTION = "collection";

    /** collection 1 id */
    public static final String ARG_COLLECTION1 = "collection1";

    /** collection 2 id */
    public static final String ARG_COLLECTION2 = "collection2";

    /** collection frequency */
    public static final String ARG_FREQUENCY = "frequency";


    /** shortcut to JQuery class */
    private static final JQuery JQ = null;

    /** the event argument */
    public static final String ARG_EVENT = "event";

    /** the event group argument */
    public static final String ARG_EVENT_GROUP = "event_group";

    /** the collection type */
    private String collectionType;

    /** named time periods */
    private List<NamedTimePeriod> namedTimePeriods;

    /** List of formulas */
    private List<Formula> formulas;

    /** Default starting year for climatology */
    public static final String DEFAULT_CLIMATE_START_YEAR = "1981";

    /** Default ending year for climatology */
    public static final String DEFAULT_CLIMATE_END_YEAR = "2010";

    /** model field index */
    public static final int MODEL_FIELD_INDEX = 0;

    /** experiment field index */
    public static final int EXP_FIELD_INDEX = 1;

    /** ensemble field index */
    public static final int ENS_FIELD_INDEX = 2;

    /** var field index */
    public static final int VAR_FIELD_INDEX = 3;

    /** formula arg */
    public static final String ARG_FORMULA = "formula";

    /**
     * Constructor
     *
     * @param repository the repository
     * @param node xml from api.xml
     * @param props properties
     *
     * @throws Exception on badness
     */

    public ClimateModelApiHandler(Repository repository, Element node,
                                  Hashtable props)
            throws Exception {
        super(repository);
        collectionType = Misc.getProperty(props, "collectiontype",
                                          "climate_collection");
    }


    /**
     * Get the data processes for this request
     *
     * @param request  the Request
     * @param action lights, camera, ACTION!
     *
     * @return  the list of data processes
     *
     * @throws Exception problem generating list
     */
    private List<Service> getServices(Request request, String action)
            throws Exception {
        //return getTypeHandler().getServicesToRun(request);
        List<Service> processes = new ArrayList<Service>();
        if (action.equals(ARG_ACTION_COMPARE)
                || action.equals(ARG_ACTION_MULTI_COMPARE)
                || action.equals(ARG_ACTION_ENS_COMPARE)) {
            Service process = new CDOArealStatisticsService(repository);
            if (process.isEnabled()) {
                processes.add(process);
            }
            process = new NCLModelPlotDataService(repository);
            if (process.isEnabled()) {
                processes.add(process);
            }
        } else if (action.equals(ARG_ACTION_TIMESERIES)
                   || action.equals(ARG_ACTION_MULTI_TIMESERIES)) {
            Service process = new CDOTimeSeriesService(repository);
            if (process.isEnabled()) {
                processes.add(process);
            }
            process = new NCLTimeSeriesPlotDataService(repository);
            if (process.isEnabled()) {
                processes.add(process);
            }
        } else if (action.equals(ARG_ACTION_CORRELATION)) {
            Service process = new CDOTimeSeriesComparison(repository);
            if (process.isEnabled()) {
                processes.add(process);
            }
            process = new NCLModelPlotDataService(repository);
            if (process.isEnabled()) {
                processes.add(process);
            }
        } else if (action.equals(ARG_ACTION_TEST)) {
            Service process = new TestDataService(repository);
            if (process.isEnabled()) {
                processes.add(process);
            }
        }

        return processes;
    }

    /**
     * Do the compare asynchronously
     *
     * @param request  the Request
     * @param dpi   the input
     * @param type  the request type
     *
     * @return  a Result
     *
     * @throws Exception  problems processing the input
     */
    public Result doCompareAsync(final Request request,
                                 final ServiceInput dpi, final String type)
            throws Exception {
        if (request.get("returnimage", false)) {
            return doCompare(request, dpi, type);
        } else {
            ActionManager.Action action = new ActionManager.Action() {
                public void run(Object actionId) throws Exception {
                    //System.err.println(actionId);
                    try {
                        //The testit is set in compare.js
                        boolean test = request.get("testit", false);
                        if (test) {
                            //For testing the compare.js
                            for (int i = 0; i < 5; i++) {
                                getActionManager().setActionMessage(actionId,
                                        "Waiting: " + i);
                                Misc.sleepSeconds(1);
                            }
                            getActionManager().setContinueHtml(actionId,
                                    "Some continue html here");

                            return;
                        }

                        Result result = doCompare(request, dpi, type,
                                            actionId);
                        String url = result.getRedirectUrl();
                        if (url != null) {
                            getActionManager().setContinueHtml(actionId,
                                    HU.href(url,
                                            msg("Continue")));
                        } else {
                            String content = result.getStringContent();
                            getActionManager().setContinueHtml(actionId,
                                    content);
                        }
                    } catch (Exception exc) {
                        getActionManager().handleError(actionId, exc);
                        logError("", exc);
                    }
                }
            };

            return getActionManager().doJsonAction(request, action,
                    "Generating Plot", "", null);
        }
    }

    /**
     * Do the compare
     *
     * @param request  the Request
     * @param dpi   the input
     * @param type  the request type
     *
     * @return  a Result
     *
     * @throws Exception  problems processing the input
     */
    public Result doCompare(Request request, ServiceInput dpi, String type)
            throws Exception {
        return doCompare(request, dpi, type, null);
    }

    
    /**
     * Get the system commands for listing  directory
     */
    private List<String> getDirListCommands() {
        List<String> commands = new ArrayList<String>();
        String os = System.getProperty("os.name");
        if (os.toLowerCase().indexOf("windows") >= 0) {
        	commands.add("dir");
        } else {
            commands.add("/bin/ls");
            commands.add("-l");
        }
        return commands;
    }

    /**
     * Do the compare
     *
     * @param request  the Request
     * @param dpi   the input
     * @param type  the request type
     * @param actionId _more_
     *
     * @return  a Result
     *
     * @throws Exception  problems processing the input
     */
    public Result doCompare(Request request, ServiceInput dpi, String type,
                            Object actionId)
            throws Exception {

        //This finds the selected processes
        List<Service> processesToRun = getServices(request, type);

        //This is the dir under <home>/process
        File processDir = null;
        processDir = dpi.getProcessDir();
        if (processDir == null) {
            processDir = getStorageManager().createProcessDir();
        }

        List<String> commands = getDirListCommands();
        commands.add(processDir.toString());
        Map<String, String> envMap = new HashMap<String, String>();
        List<ServiceOutput> outputs   = new ArrayList<ServiceOutput>();
        ServiceInput        nextInput = dpi;
        for (Service process : processesToRun) {
            long milli = System.currentTimeMillis();
            //System.err.println("MODEL: applying process: "
            //                   + process.getLabel());
            if (actionId != null) {
                getActionManager().setActionMessage(actionId,
                        "Processing: " + process.getLabel());
                nextInput.putProperty("actionId", actionId);
            }
            ServiceOutput output = process.evaluate(request, null,nextInput, null);
            //System.err.println("MODEL: " + process.getLabel() + " took "+(System.currentTimeMillis()-milli)+" ms");
            outputs.add(output);

            //make a new input for the next process
            nextInput = dpi.makeInput(output);
            /*  Debug if files are not found.
            JobManager.CommandResults results =
                getRepository().getJobManager().executeCommand(commands, envMap,
                    processDir, 60);
            String errorMsg = results.getStderrMsg();
            String outMsg   = results.getStdoutMsg();
            //System.err.println(outMsg);
            //System.out.println(StringUtil.toString(processDir.listFiles()));
             */

            //Are we done? This should probably be a check to see if the output has a Result
            //if (output.hasOutput()) {
            //    break;
            //}
        }
        

        boolean    anyKMZ    = false;
        List<File> files     = new ArrayList<File>();
        File       lastFile  = null;
        Entry      lastEntry = null;
        for (ServiceOutput dpo : outputs) {
            for (Entry granule : dpo.getEntries()) {
                if (granule.isFile()) {
                    lastFile = granule.getFile();
                    if (IOUtil.hasSuffix(lastFile.toString(), "kmz")) {
                        anyKMZ = true;
                    }
                    files.add(lastFile);
                }
                lastEntry = granule;
            }
        }

        String template =
            getStorageManager().readSystemResource(
                "/org/ramadda/geodata/model/resources/plot_template.xml");
        template = template.replace("${name}", "Model comparison output");
        StringBuilder dpiDesc = new StringBuilder();
        if (dpi.getOperands().size() > 1) {
            dpiDesc.append("Comparison of ");
        } else {
            dpiDesc.append("Analysis of ");
        }
        int cntr = 0;
        for (ServiceOperand dpo : dpi.getOperands()) {
            if (cntr > 0) {
                dpiDesc.append(" and ");
            }
            dpiDesc.append(dpo.getDescription());
            cntr++;
        }
        template = template.replace("${description}", dpiDesc.toString());
        if (anyKMZ) {
            template = template.replace(
                "${output}",
                "{{earth width=\"500\" name=\"*.kmz\" listentries=\"true\" listwidth=\"450\"}}");
        } else {
            template = template.replace(
                "${output}",
                "{{gallery prefix=\"'''Images'''\" message=\"\" width=\"500\"}}");
        }
        IOUtil.writeFile(new File(IOUtil.joinDir(processDir,
                ".this.ramadda.xml")), template);


        //If no processing was done then return the raw files
        if (processesToRun.size() == 0) {
            ClimateCollectionTypeHandler typeHandler = getTypeHandler();

            return typeHandler.zipFiles(request, "results.zip", files);
        }

        //Now we get the process entry id
        String processId = processDir.getName();
        String processEntryId =
            getStorageManager().getEncodedProcessDirEntryId(processId);

        String entryUrl = getStorageManager().getProcessDirEntryUrl(request,
                              processId);

        if (request.get("returnimage", false)) {
            String suffix = IOUtil.getFileExtension(lastFile.toString());
            request.setReturnFilename(processId + suffix);

            return new Result(
                "",
                getStorageManager().getFileInputStream(lastFile.toString()),
                getRepository().getMimeTypeFromSuffix(suffix));
        } else if (request.get("returnjson", false)) {
            StringBuilder json = new StringBuilder();
            Entry processDirEntry =
                new Entry(processEntryId,
                          getEntryManager().getProcessFileTypeHandler());
            getRepository().getJsonOutputHandler().makeJson(request,
                    Misc.newList(processDirEntry), json);

            return new Result("", json, "application/json");

        } else {
            return new Result(entryUrl);
        }

    }

    /**
     * Make the time series asynchronously
     *
     * @param request  the Request
     * @param dpi   the input
     * @param type  the request type
     *
     * @return  a Result
     *
     * @throws Exception  problems processing the input
     */
    public Result makeTimeSeriesAsync(final Request request,
                                      final ServiceInput dpi,
                                      final String type)
            throws Exception {
        if (request.get("returnimage", false)) {
            return makeTimeSeries(request, dpi, type, null);
        } else {

            ActionManager.Action action = new ActionManager.Action() {
                public void run(Object actionId) throws Exception {
                    //System.err.println(actionId);
                    try {

                        Result result = makeTimeSeries(request, dpi, type,
                                            actionId);
                        String url = result.getRedirectUrl();
                        if (url != null) {
                            getActionManager().setContinueHtml(actionId,
                                    HU.href(url,
                                            msg("Continue")));
                        } else {
                            String content = result.getStringContent();
                            getActionManager().setContinueHtml(actionId,
                                    content);
                        }
                    } catch (Exception exc) {
                        getActionManager().handleError(actionId, exc);
                        logError("", exc);
                    }
                }
            };

            return getActionManager().doJsonAction(request, action,
                    "Generating Plot", "", null);
        }
    }


    /**
     * Make the time series
     *
     * @param request  the Request
     * @param dpi   the input
     *
     * @return  a Result
     *
     * @throws Exception  problems processing the input
     */
    public Result makeTimeSeries(Request request, ServiceInput dpi)
            throws Exception {
        return makeTimeSeries(request, dpi, ARG_ACTION_MULTI_TIMESERIES,
                              null);
    }

    /**
     * Make the time series
     *
     * @param request  the Request
     * @param dpi   the input
     * @param type  the request type
     * @param actionId _more_
     *
     * @return  a Result
     *
     * @throws Exception  problems processing the input
     */
    public Result makeTimeSeries(Request request, ServiceInput dpi,
                                 String type, Object actionId)
            throws Exception {

        //This finds the selected processes
        List<Service> processesToRun = getServices(request, type);

        //This is the dir under <home>/process
        File processDir = null;
        processDir = dpi.getProcessDir();
        if (processDir == null) {
            processDir = getStorageManager().createProcessDir();
        }

        List<ServiceOutput> outputs   = new ArrayList<ServiceOutput>();
        ServiceInput        nextInput = dpi;
        for (Service process : processesToRun) {
            //System.err.println("MODEL: applying process: "
            //                   + process.getLabel());
            if (actionId != null) {
                getActionManager().setActionMessage(actionId,
                        "Processing: " + process.getLabel());
                nextInput.putProperty("actionId", actionId);
            }
            ServiceOutput output = process.evaluate(request, null, nextInput, null);
            outputs.add(output);

            //make a new input for the next process
            nextInput = dpi.makeInput(output);

            //Are we done? This should probably be a check to see if the output has a Result
            //if (output.hasOutput()) {
            //    break;
            //}
        }

        boolean    anyKMZ    = false;
        List<File> files     = new ArrayList<File>();
        File       lastFile  = null;
        Entry      lastEntry = null;
        for (ServiceOutput dpo : outputs) {
            for (Entry granule : dpo.getEntries()) {
                if (granule.isFile()) {
                    lastFile = granule.getFile();
                    if (IOUtil.hasSuffix(lastFile.toString(), "kmz")) {
                        anyKMZ = true;
                    }
                    files.add(lastFile);
                }
                lastEntry = granule;
            }
        }

        String template =
            getStorageManager().readSystemResource(
                "/org/ramadda/geodata/model/resources/ts_template.xml");
        template = template.replace("${name}",
                                    "Climate model comparison output");
        StringBuilder dpiDesc = new StringBuilder();
        if (dpi.getOperands().size() > 1) {
            dpiDesc.append("Comparison of ");
        } else {
            dpiDesc.append("Analysis of ");
        }
        int cntr = 0;
        for (ServiceOperand dpo : dpi.getOperands()) {
            if (cntr > 0) {
                dpiDesc.append(" and ");
            }
            dpiDesc.append(dpo.getDescription());
            cntr++;
        }
        template = template.replace("${description}", dpiDesc.toString());
        if (anyKMZ) {
            template = template.replace(
                "${output}",
                "{{earth width=\"500\" name=\"*.kmz\" listentries=\"true\" listwidth=\"450\"}}");
        } else {
            template = template.replace(
                "${output}",
                "{{gallery prefix=\"'''Images'''\" message=\"\" width=\"500\"}}");
        }
        IOUtil.writeFile(new File(IOUtil.joinDir(processDir,
                ".this.ramadda.xml")), template);



        //If no processing was done then return the raw files
        if (processesToRun.size() == 0) {
            ClimateCollectionTypeHandler typeHandler = getTypeHandler();

            return typeHandler.zipFiles(request, "results.zip", files);
        }

        //Now we get the process entry id
        String processId = processDir.getName();
        String processEntryId =
            getStorageManager().getEncodedProcessDirEntryId(processId);

        String entryUrl = getStorageManager().getProcessDirEntryUrl(request,
                              processId);
        if (request.get("returnimage", false)) {
            request.setReturnFilename(processId + ".png");

            return new Result(
                "",
                getStorageManager().getFileInputStream(lastFile.toString()),
                "image/png");
        } else if (request.get("returnjson", false)) {
            StringBuilder json = new StringBuilder();
            Entry processDirEntry =
                new Entry(processEntryId,
                          getEntryManager().getProcessFileTypeHandler());
            getRepository().getJsonOutputHandler().makeJson(request,
                    Misc.newList(processDirEntry), json);

            return new Result("", json, "application/json");

        } else {
            return new Result(entryUrl);
        }

    }

    /**
     * handle the plot comparison request
     *
     * @param request request
     *
     * @return result
     *
     * @throws Exception on badness
     */
    public Result processCompareRequest(Request request) throws Exception {
        return handleRequest(request, ARG_ACTION_COMPARE);
    }

    /**
     * handle the multiple param comparison request
     *
     * @param request request
     *
     * @return result
     *
     * @throws Exception on badness
     */
    public Result processMultiCompareRequest(Request request)
            throws Exception {
        return handleRequest(request, ARG_ACTION_MULTI_COMPARE);
    }

    /**
     * handle the multiple param comparison request
     *
     * @param request request
     *
     * @return result
     *
     * @throws Exception on badness
     */
    public Result processEnsCompareRequest(Request request) throws Exception {
        return handleRequest(request, ARG_ACTION_ENS_COMPARE);
    }

    /**
     * handle the time series request
     *
     * @param request request
     *
     * @return result
     *
     * @throws Exception on badness
     */
    public Result processTimeSeriesRequest(Request request) throws Exception {
        return handleRequest(request, ARG_ACTION_TIMESERIES);
    }

    /**
     * handle the mult-time series request
     *
     * @param request request
     *
     * @return result
     *
     * @throws Exception on badness
     */
    public Result processMultiTimeSeriesRequest(Request request)
            throws Exception {
        return handleRequest(request, ARG_ACTION_MULTI_TIMESERIES);
    }

    /**
     * handle the correlation
     *
     * @param request request
     *
     * @return result
     *
     * @throws Exception on badness
     */
    public Result processCorrelationRequest(Request request)
            throws Exception {
        return handleRequest(request, ARG_ACTION_CORRELATION);
    }

    /**
     * handle the correlation
     *
     * @param request request
     *
     * @return result
     *
     * @throws Exception on badness
     */
    public Result processTestRequest(Request request) throws Exception {
        return handleRequest(request, ARG_ACTION_TEST);
    }

    /**
     * handle the request
     *
     * @param request request
     * @param type  the request type
     *
     * @return result
     *
     * @throws Exception on badness
     */
    public Result handleRequest(Request request, String type)
            throws Exception {

        boolean debug = false;
        if (debug) {
            System.err.println("handleRequest:" + request);
        }
        if (getServices(request, type).isEmpty()) {
            throw new RuntimeException(
                "Data processes for model comparison are not configured.");
        }
        String[] collectionArgs = null;
        int      numCollections = 1;
        if (type.equals(ARG_ACTION_COMPARE)
                || type.equals(ARG_ACTION_ENS_COMPARE)
                || type.equals(ARG_ACTION_TIMESERIES)) {
            numCollections = 2;
            collectionArgs = new String[] { ARG_COLLECTION1,
                                            ARG_COLLECTION2 };
        } else {
            collectionArgs = new String[] { ARG_COLLECTION1 };
        }

        String fixedCollectionId = request.getSanitizedString(ARG_COLLECTION,
                                       (String) null);
        Entry fixedCollection = null;

        if (fixedCollectionId != null) {
            //            System.err.println ("Have fixed collection:" + fixedCollectionId);
            for (String collection : collectionArgs) {
                request.put(collection, fixedCollectionId);
            }
        }


        String json = request.getString("json", (String) null);
        if (json != null) {
            return processJsonRequest(request, type);
        }
        boolean returnjson = request.get("returnjson", false);

        Hashtable<String, StringBuilder> extra = new Hashtable<String,
                                                     StringBuilder>();
        List<ServiceOperand> operands = new ArrayList<ServiceOperand>();

        File processDir               =
            getStorageManager().createProcessDir();

        //If we are searching or comparing then find the selected entries
        if (request.exists(ARG_ACTION_SEARCH) || request.exists(type)) {
            int collectionCnt = 0;
            for (String collection : collectionArgs) {

                collectionCnt++;
                StringBuilder tmp = new StringBuilder();
                extra.put(collection, tmp);
                String selectArg = getCollectionSelectArg(collection);
                Entry collectionEntry = getEntryManager().getEntry(request,
                                            request.getString(selectArg,
                                                    ""));
                //                System.err.println("collectionEntry:" + collectionEntry+" select: " +
                //                                    request.getString(selectArg,""));

                if (collectionEntry == null) {
                    tmp.append("No collection");

                    continue;
                }
                List<Entry> entries = findModelEntries(request, collection,
                                          collectionEntry, collectionCnt);
                //System.err.println("Found "+entries.size()+" entries");
                if (entries.isEmpty()) {
                    if (operands.isEmpty()) {
                        if (returnjson) {
                            StringBuilder data = new StringBuilder();
                            data.append(
                                JsonUtil.mapAndQuote(Utils.makeListFromValues("Error",
                                        "You need to select all fields")));

                            return new Result("", data, JsonUtil.MIMETYPE);
                        } else {
                            tmp.append(
                                getPageHandler().showDialogError(
                                    "You need to select all fields"));
                        }
                    }

                    continue;
                }
                List<List<Entry>> sordidEntries =
                    ModelUtil.groupEntriesByColumn(request, entries);
                if (type.equals(ARG_ACTION_MULTI_COMPARE)
                        || type.equals(ARG_ACTION_MULTI_TIMESERIES)
                        || type.equals(ARG_ACTION_ENS_COMPARE)
                        || type.equals(ARG_ACTION_CORRELATION)
                        || request.defined(ARG_FORMULA)) {
                    for (List<Entry> e : sordidEntries) {
                        ServiceOperand op =
                            new ServiceOperand(e.get(0).getName(), e);
                        //System.out.println(collection);
                        op.putProperty(ARG_COLLECTION, collection);
                        operands.add(op);
                    }
                } else {
                    List<Entry> allEntries = new ArrayList();
                    /* Should only be one, but just in case, get the first */
                    Entry e = sordidEntries.get(0).get(0);
                    ServiceOperand op = new ServiceOperand(e.getName(),
                                            sordidEntries.get(0));
                    /* Should only be one, unless a formula like vectors
                    StringBuilder opName     = new StringBuilder();
                    for (List<Entry> eList : sordidEntries) {
                        for (Entry e : eList) {
                            opName.append(e.getName());
                            opName.append(";");
                            allEntries.add(e);
                        }
                    }
                    ServiceOperand op = new ServiceOperand(opName.toString(),
                                            allEntries);
                    */
                    op.putProperty(ARG_COLLECTION, collection);
                    operands.add(op);
                }

            }
            if (type.equals(ARG_ACTION_CORRELATION)) {
                Entry tsEntry = getTimeSeriesEntry(request);
                if (tsEntry != null) {
                    ServiceOperand op = new ServiceOperand(tsEntry.getName(),
                                            tsEntry);
                    op.putProperty(ARG_COLLECTION, ARG_COLLECTION2);
                    operands.add(op);
                } else {
                    if (returnjson) {
                        StringBuilder data = new StringBuilder();
                        data.append(
                            JsonUtil.mapAndQuote(Utils.makeListFromValues("Error",
                                    "You need to select a time series")));

                        return new Result("", data, JsonUtil.MIMETYPE);
                    }  /*else {
                         tmp.append(
                             getPageHandler().showDialogError(
                                 "You need to select all fields"));
                     } */
                }

            }
        }


        //Check to see if we at least 1 operand 
        boolean hasOperands = false;
        if (operands.size() >= 1) {
            hasOperands = (operands.get(0).getEntries().size() > 0)
                          || (operands.get(1).getEntries().size() > 0);
        }

        if ((fixedCollectionId != null) && (fixedCollection == null)) {
            fixedCollection = getEntryManager().getEntry(request,
                    fixedCollectionId);
            //            System.err.println("got it:" + fixedCollection);
        }



        StringBuilder sb  = new StringBuilder();
        ServiceInput  dpi = new ServiceInput(processDir, operands);
        dpi.putProperty("type", type);

        if (request.exists(type)) {
            if (hasOperands) {
                try {
                    if (type.equals(ARG_ACTION_COMPARE)
                            || type.equals(ARG_ACTION_TEST)
                            || type.equals(ARG_ACTION_ENS_COMPARE)
                            || type.equals(ARG_ACTION_MULTI_COMPARE)
                            || type.equals(ARG_ACTION_CORRELATION)) {
                        //return doCompare(request, dpi, type);
                        return doCompareAsync(request, dpi, type);
                    } else if (type.equals(ARG_ACTION_MULTI_TIMESERIES)
                               || type.equals(ARG_ACTION_TIMESERIES)) {
                        return makeTimeSeriesAsync(request, dpi, type);
                    }
                } catch (Exception exc) {
                    if (returnjson) {
                        StringBuilder data = new StringBuilder();
                        data.append(
                            JsonUtil.mapAndQuote(Utils.makeListFromValues("error",
                                    exc.getMessage())));

                        return new Result("", data, JsonUtil.MIMETYPE);
                    } else {
                        sb.append(
                            getPageHandler().showDialogError(
                                "An error occurred:<br>" + exc.getMessage()));
                    }
                }
            } else {
                if (returnjson) {
                    StringBuilder data = new StringBuilder();
                    data.append(JsonUtil.map(Utils.makeListFromValues("error",
                            "No fields selected.")));

                    return new Result("", data, JsonUtil.MIMETYPE);
                } else {
                    sb.append(
                        getPageHandler().showDialogWarning(
                            "No fields selected"));
                }
            }
        }

        // Build the input form

        List<Entry> collections = getCollections(request);
        if (collections.size() == 0) {
            return new Result(
                "Model Comparison",
                new StringBuilder(
                    getPageHandler().showDialogWarning(
                        msg("No collections found"))));
        }

        String formId = "selectform" + HtmlUtils.blockCnt++;
        sb.append(HtmlUtils.comment("collection form"));
        sb.append(HtmlUtils.importJS(getHtdocsUrl("/model/compare.js")));
        sb.append(HtmlUtils.cssLink(getRepository().getUrlBase()
                                    + "/model/model.css"));

        String formAttrs = HtmlUtils.attrs(ATTR_ID, formId);
        sb.append(HtmlUtils.formPost(getApiUrlPath(request, type),
                                     formAttrs));
        sb.append(HtmlUtils.hidden(ARG_MAX,
                                   request.getSanitizedString(ARG_MAX,
                                           "9999")));
        getPageHandler().addDisplayImports(request, sb);

        List<TwoFacedObject> tfos = new ArrayList<TwoFacedObject>();
        tfos.add(new TwoFacedObject("Select Model Collection", ""));
        for (Entry collection : collections) {
            tfos.add(new TwoFacedObject(collection.getLabel(),
                                        collection.getId()));
        }

        List<Service> processes = getServices(request, type);


        String        formType  = "compare";
        String helpFile =
            "/org/ramadda/geodata/model/htdocs/model/compare.html";
        String fieldsHelpFile = formType + "-fields.html";


        if (type.equals(ARG_ACTION_MULTI_COMPARE)) {
            formType       = "multicompare";
            fieldsHelpFile = formType + "-fields.html";
        } else if (type.equals(ARG_ACTION_ENS_COMPARE)) {
            formType       = "enscompare";
            fieldsHelpFile = formType + "-fields.html";
        } else if (type.equals(ARG_ACTION_CORRELATION)) {
            formType       = "correlation";
            fieldsHelpFile = formType + "-fields.html";
            helpFile =
                "/org/ramadda/geodata/model/htdocs/model/correlation.html";
        } else if (type.equals(ARG_ACTION_TIMESERIES)) {
            formType       = "timeseries";
            fieldsHelpFile = "compare-fields.html";
            helpFile =
                "/org/ramadda/geodata/model/htdocs/model/timeseries.html";
        } else if (type.equals(ARG_ACTION_MULTI_TIMESERIES)) {
            formType       = "multitimeseries";
            fieldsHelpFile = "enscompare-fields.html";
            helpFile =
                "/org/ramadda/geodata/model/htdocs/model/timeseries.html";
        } else if (type.equals(ARG_ACTION_TEST)) {
            formType       = "test";
            fieldsHelpFile = formType + "-fields.html";
        }


        StringBuilder js =
            new StringBuilder("\n//collection form initialization\n");
        String jsArgs = getFrequencyArgs(request);
        if (request.defined(ARG_TEMPLATE)) {
            jsArgs = jsArgs + "&" + ARG_TEMPLATE + "="
                     + request.getSanitizedString(ARG_TEMPLATE, "");
        }
        js.append("var " + formId + " = new "
                  + HtmlUtils.call("CollectionForm",
                                   HtmlUtils.squote(formId),
                                   HtmlUtils.squote(formType),
                                   HtmlUtils.squote(jsArgs)));




        for (Service process : processes) {
            process.initFormJS(request, js, formId);
        }

        if (request.defined(ARG_EVENT_GROUP)) {
            String egroup = request.getSanitizedString(ARG_EVENT_GROUP, "");
            if ( !egroup.isEmpty()) {
                sb.append(HtmlUtils.hidden(ARG_EVENT_GROUP, egroup));
            }
        }


        String frequency = "monthly";
        if (request.defined(ARG_FREQUENCY)) {
            String fstring = request.getSanitizedString(ARG_FREQUENCY, "");
            if ( !fstring.isEmpty()) {
                frequency = fstring.toLowerCase();
            }
        } else if (fixedCollection != null) {
            String fstring = ModelUtil.getCollectionFrequency(request,
                                 fixedCollection).toLowerCase();
            frequency = (fstring.indexOf("daily") > 0)
                        ? "daily"
                        : "monthly";
        }

        String title = "Model Comparison";
        String desc  = "";
        if (type.equals(ARG_ACTION_COMPARE) || type.equals(ARG_ACTION_TEST)) {
            if (request.defined(ARG_EVENT_GROUP)) {
                String groupName =
                    request.getSanitizedString(ARG_EVENT_GROUP, "");
                //groupName = groupName.replaceAll("[+_]"," ");
                title += " for " + groupName;
                desc = "Plot " + frequency
                       + " maps from different model datasets as well as differences between datasets for "
                       + groupName;
            } else {
                title = "Model Comparison";
                desc = "Plot " + frequency
                       + " maps from different model datasets as well as differences between datasets.";
            }

        } else if (type.equals(ARG_ACTION_TIMESERIES)) {
            title = "Model Time Series";
            desc = "Plot " + frequency
                   + " time series from different model datasets.";
        } else if (type.equals(ARG_ACTION_MULTI_TIMESERIES)) {
            title = "Model Ensemble Time Series";
            desc = "Plot " + frequency
                   + " time series from different model ensemble members.";
        } else if (type.equals(ARG_ACTION_ENS_COMPARE)) {
            title = "Model Ensemble Plot";
            desc  = "Compare " + frequency + " model ensemble members.";
        } else if (type.equals(ARG_ACTION_CORRELATION)) {
            title = "Model Correlation";
            desc = "Plot correlations between model output and climate index time series";
        } else {
            title = "Model Comparison";
            desc = "Plot " + frequency
                   + " maps from different model datasets.";
        }


        if (fixedCollection != null) {
            String entryUrl =
                request.entryUrl(getRepository().URL_ENTRY_SHOW,
                                 fixedCollection);

            title += " for "
                     + HtmlUtils.href(entryUrl, fixedCollection.getName());

            sb.append(HtmlUtils.hidden(ARG_COLLECTION,
                                       fixedCollection.getId()));
        }

        sb.append("\n+section\n+title\n");
        sb.append(title);
        sb.append("\n-title\n");
        sb.append("+note\n");
        sb.append(desc);
        sb.append("\n-note\n");

        if (request.defined(ARG_FREQUENCY)) {
            sb.append(HtmlUtils.hidden(ARG_FREQUENCY,
                                       request.getSanitizedString(
                                           ARG_FREQUENCY,
                                           "")));
        }
        if (request.defined(ARG_TEMPLATE)) {
            sb.append(HtmlUtils.hidden(ARG_TEMPLATE,
                                       request.getSanitizedString(
                                           ARG_TEMPLATE,
                                           "")));
        }

        String plotButton  = "&nbsp;";
        String selectClass = "multi_select_widget";
        if (type.equals(ARG_ACTION_MULTI_COMPARE)
        //|| type.equals(ARG_ACTION_ENS_COMPARE)
        || type.equals(ARG_ACTION_MULTI_TIMESERIES)) {
            selectClass = "select_widget";
        }
        if (hasOperands) {
            if (type.equals(ARG_ACTION_COMPARE)
                    || type.equals(ARG_ACTION_TEST)
                    || type.equals(ARG_ACTION_CORRELATION)) {
                plotButton = HtmlUtils.submit("Make Plot", type,
                        HtmlUtils.id(formId + "_submit"));
                /*
                        HtmlUtils.id(formId + "_submit")
                        + makeButtonSubmitDialog(sb,
                                msg("Making Plot, Please Wait") + "..."));
                */
            } else if (type.equals(ARG_ACTION_MULTI_COMPARE)
                       || type.equals(ARG_ACTION_ENS_COMPARE)
                       || type.equals(ARG_ACTION_CORRELATION)) {
                plotButton = HtmlUtils.submit("Make Plot", type,
                        HtmlUtils.id(formId + "_submit"));
                /*
                        HtmlUtils.id(formId + "_submit")
                        + makeButtonSubmitDialog(sb,
                                msg("Making Plot, Please Wait") + "..."));
                */
            } else {
                plotButton = HtmlUtils.submit("Make Time Series", type,
                        HtmlUtils.id(formId + "_submit"));
                /*
        HtmlUtils.id(formId + "_submit")
        + makeButtonSubmitDialog(sb,
                msg("Making Time Series, Please Wait")
                + "..."));
                */
            }
        }

        String plotButtonDiv =
            HtmlUtils.div(plotButton, HtmlUtils.cssClass("model-header"));

        sb.append("<div class=\"row\">\n");
        sb.append(HtmlUtils.open("div", "class=\"col-md-5\"") + "\n");

        // Field selection
        String fieldsHelp = getHelp(fieldsHelpFile);
        sb.append(HtmlUtils.open("div", HtmlUtils.cssClass("titled_border")));
        String s = "Select Data To Plot";
        //        s= "Field(s)  ";
        //sb.append(HtmlUtils.div(msg(s), HtmlUtils.id("title")));
        sb.append(HtmlUtils.div(msg(s) + HtmlUtils.space(2) + fieldsHelp,
                                HtmlUtils.id("title")));
        //        sb.append(HtmlUtils.leftRight("",fieldsHelp.toString()));
        sb.append(HtmlUtils.open("div", HtmlUtils.id("content")));
        int           collectionNumber = 0;

        StringBuilder ack              = new StringBuilder();
        HashSet       seenAck          = new HashSet();

        List<String>  datasets = new ArrayList<String>(collectionArgs.length);
        List<String> datasetTitles =
            new ArrayList<String>(collectionArgs.length);
        for (String collection : collectionArgs) {

            StringBuilder dsb = new StringBuilder();

            //dsb.append(HtmlUtils.formTable());
            if (collectionNumber == 0) {
                if (type.equals(ARG_ACTION_CORRELATION)) {
                    datasetTitles.add("Model Dataset ");
                } else {
                    datasetTitles.add("Dataset 1 ");
                }
            } else {
                datasetTitles.add("Dataset 2 (Optional)");
            }
            collectionNumber++;
            String       arg       = getCollectionSelectArg(collection);
            String       id        = getCollectionSelectId(formId,
                                         collection);



            List<String> selectors = new ArrayList<String>();
            if (fixedCollection != null) {
                dsb.append("\n");
                dsb.append(HtmlUtils.hidden(arg,
                                            fixedCollection.getId(),
                                            HtmlUtils.id(id)));
            } else {
                String collectionWidget = HtmlUtils.select(arg, tfos,
                                              request.getSanitizedString(arg,
                                                      ""), HtmlUtils.cssClass(
                                                          selectClass) + HtmlUtils.id(
                                                          id));
                String select = "<label class=\"selector\" for=\"" + id
                                + "\">" + msgLabel("Collection") + "</label>"
                                + collectionWidget;
                selectors.add(select);
            }


            //Entry        entry   = collections.get(0);
            List<Column> columns = null;
            Entry collectionEntry = getEntryManager().getEntry(request,
                                        request.getSanitizedString(arg,
                                                ""));
            if (collectionEntry != null) {
                columns =
                    ((CollectionTypeHandler) (collectionEntry.getTypeHandler())).getGranuleColumns();
                //Add the ack
                //This metadadata type is define in repository/resources/metadata/metadata.xml
                //bit a different one could be used
                List<Metadata> metadataList =
                    getMetadataManager().findMetadata(request,
                        collectionEntry, "content.acknowledgement", true);
                if (metadataList != null) {
                    for (Metadata metadata : metadataList) {
                        //Check for duplicates
                        if (seenAck.contains(metadata.getId())) {
                            continue;
                        }
                        seenAck.add(metadata.getId());
                        if (ack.length() == 0) {
                            //some verbiage
                            ack.append("\n");
                            HtmlUtils.open(ack, "div",
                                           HtmlUtils.attrs("style",
                                                   "margin:10px;"));
                            ack.append("<h3>Citation</h3>\n");
                            ack.append(
                                "<br>When using this data please cite the below blah blah blah...\n");
                            HtmlUtils.open(ack, "div",
                                           HtmlUtils.attrs("style",
                                                   "margin:10px;"));
                        }
                        //Style the div to your liking
                        HtmlUtils.div(ack, metadata.getAttr1(),
                                      HtmlUtils.attrs("class",
                                              "someclass",
                                              "style",
                                              ""));
                    }
                }



            } else {
                ClimateCollectionTypeHandler typeHandler = getTypeHandler();
                columns = typeHandler.getGranuleColumns();
            }
            int numColumns = columns.size();

            for (int fieldIdx = 0; fieldIdx < numColumns; fieldIdx++) {
                Column column = columns.get(fieldIdx);
                //String key = "values::" + entry.getId()+"::" +column.getName();
                List values = new ArrayList();
                values.add(new TwoFacedObject("--", ""));
                arg = getFieldSelectArg(collection, fieldIdx);
                //String selectedValue = request.getString(arg, "");
                List selectedValues = sanitizeRequestValues(request.get(arg,
                                                                        new ArrayList()));
                if ((selectedValues.size() > 0)
                        && !selectedValues.get(0).toString().isEmpty()) {
                    values.addAll(selectedValues);
                }
                if (debug) {
                    System.err.println("\tcolumn:" + column + " arg:" + arg
                                       + " values:" + values);
                }
                // don't show variable selector for subsequent collections when we have a fixed
                // collection
                if (request.defined(ARG_COLLECTION)
                        && (collectionNumber > 1)
                        && column.getName().equals("variable")
                        && (fieldIdx == (numColumns - 1))) {
                    //dsb.append(HtmlUtils.formEntry(msg(""),
                    String selector = "<span class=\"" + selectClass
                                      + "\">&nbsp;</span>";
                    selectors.add("<label class=\"selector\">" + msg("")
                                  + "</label>" + selector);
                    //                } else if (type.equals(ARG_ACTION_MULTI_TIMESERIES) && column.getName().equals("ensemble")) {
                    //                    String selector =
                    //                        "<span class=\""+selectClass+"\">&nbsp;</span>";
                    //                    selectors.add("<label class=\"selector\">" + msg("Ensemble Members: All")
                    //                                  + "</label>" + selector);
                } else {

                    String extraSelect = "";
                    String selectBox   = "";
                    String multiboxCss =    HU.css("padding",
                               "5px",
                               "max-height",
                               "100px",
                               "overflow-y",
                               "auto",
                               "border",
                               "1px solid #ccc");
                    /*
                    if (type.equals(ARG_ACTION_MULTI_COMPARE)
                            && column.getName().equals("model")) {
                        String multiple = "true";
                        extraSelect =
                            HtmlUtils.attr(HtmlUtils.ATTR_MULTIPLE, multiple)
                            + HtmlUtils.attr("size", "4")
                            + HtmlUtils.attr(
                                "title",
                                "Select one or more models");
                        if (values.size() == 1) {
                            values = new ArrayList();
                        }
                        selectBox = HtmlUtils.checkboxSelect(arg, values,
                                selectedValues, multiboxCss,  HtmlUtils.attr("id",
                                       getFieldSelectId(formId,
                                               collection,
                                               fieldIdx)) + extraSelect);
                    } else if (((type.equals(
                    		*/
                    		
                    if (((type.equals(
                    ARG_ACTION_ENS_COMPARE) || type.equals(
                            ARG_ACTION_TEST) || type.equals(
                            ARG_ACTION_CORRELATION) || type.equals(
                            ARG_ACTION_MULTI_TIMESERIES))
                               && column.getName().equals("ensemble")) ||
                    (type.equals(ARG_ACTION_MULTI_COMPARE)
                            && column.getName().equals("model"))) {
                        //String multiple = (frequency.equals("monthly")) ? "true" : "false";
                        String hint = "Select one or more ensemble members";
                    	if (type.equals(ARG_ACTION_MULTI_COMPARE)) {
                    		hint = "Select one or more models";
                    	}
                        String multiple = "true";
                        extraSelect =
                            HtmlUtils.attr(HtmlUtils.ATTR_MULTIPLE, multiple)
                            + HtmlUtils.attr("size", "4")
                            + HtmlUtils.attr(
                                "title", hint);
                        /*
                        selectBox = HtmlUtils.select(arg, values,
                                selectedValues,
                                HtmlUtils.cssClass(selectClass)
                                + HtmlUtils.attr("id",
                                        getFieldSelectId(formId,
                                                collection,
                                                fieldIdx)) + extraSelect);
                                                           */
                        //jeffmc:If there is only the "all" value then don't include any
                        if (values.size() == 1) {
                            values = new ArrayList();
                        }
                        selectBox = HtmlUtils.checkboxSelect(arg, values,
                                selectedValues, multiboxCss, HtmlUtils.attr("id",
                                       getFieldSelectId(formId,
                                               collection,
                                               fieldIdx)) + extraSelect);

                    } else {
                        selectBox = HtmlUtils.select(arg, values,
                                selectedValues,
                                HtmlUtils.cssClass(selectClass)
                                + HtmlUtils.attr("id",
                                        getFieldSelectId(formId,
                                                collection,
                                                fieldIdx)) + extraSelect);
                    }
                    String select = "<label class=\"selector\" for=\""
                                    + getFieldSelectId(formId, collection,
                                        fieldIdx) + "\">"
                                            + msgLabel(column.getLabel())
                                            + "</label>" + selectBox;

                    selectors.add(select);

                }
            }
            addSelectorTable(dsb, selectors);

            datasets.add(dsb.toString());
        }
        // Add a separate dataset if correlation
        if (type.equals(ARG_ACTION_CORRELATION)) {
            datasetTitles.add("Time Series");
            datasets.add(makeTimeSeriesSelectors(request, formId));
        }
        // table of two datasets
        sb.append("<div class=\"row\">");
        if (datasets.size() > 1) {
            sb.append(
                "<div class=\"col-md-6 dataset-selector bottom-spacer\" >");
        } else {
            sb.append("<div class=\"col-md-12 bottom-spacer\">");
        }
        if (type.equals(ARG_ACTION_COMPARE)
                || type.equals(ARG_ACTION_TIMESERIES)
                || type.equals(ARG_ACTION_ENS_COMPARE)
                || type.equals(ARG_ACTION_TEST)
                || type.equals(ARG_ACTION_CORRELATION)) {
            sb.append(HtmlUtils.div(msg(datasetTitles.get(0)),
                                    HtmlUtils.cssClass(
                                        "model-dataset_title")));
        }
        sb.append(HtmlUtils.div(datasets.get(0)));
        sb.append(HtmlUtils.close(HtmlUtils.TAG_DIV));  // left or only column
        if (datasets.size() > 1) {
            sb.append("<div class=\"col-md-6 text-vertical\">");
            sb.append(HtmlUtils.div(msg(datasetTitles.get(1)),
                                    HtmlUtils.cssClass(
                                        "model-dataset_title")));
            sb.append(HtmlUtils.div(datasets.get(1)));
            sb.append(HtmlUtils.close(HtmlUtils.TAG_DIV));  // right column
        }
        sb.append("</div>");  // dataset row

        String searchButton = null;
        if ( !hasOperands) {
            searchButton = HtmlUtils.submit("Select Data", ARG_ACTION_SEARCH,
                                            HtmlUtils.id(formId + "_submit")
                                            + makeButtonSubmitDialog(sb,
                                                    msg("Searching for data")
                                                    + "..."));
        } else {
            searchButton = HtmlUtils.submit("Update Data Selection",
                                            ARG_ACTION_SEARCH,
                                            HtmlUtils.id(formId + "_submit")
                                            + makeButtonSubmitDialog(sb,
                                                    msg("Searching for new data")
                                                    + "..."));
        }
        sb.append("<div class=\"row\">");
        sb.append("  <div class=\"col-md-12 text-center\">");
        sb.append(searchButton);
        sb.append(HtmlUtils.close(HtmlUtils.TAG_DIV));  // button column
        sb.append(HtmlUtils.close(HtmlUtils.TAG_DIV));  // button row
        sb.append(HtmlUtils.close(HtmlUtils.TAG_DIV));  // data select titled_border_content
        sb.append(HtmlUtils.close(HtmlUtils.TAG_DIV)
                  + "<!-- titled_border -->");  // data select titled_border

        if (hasOperands) {  // add in the process boxes


            //Variable has been selected.  See if it's a formula
            if (type.equals(ARG_ACTION_COMPARE)) {
                String varArg = getFieldSelectArg(ARG_COLLECTION1,
                                    VAR_FIELD_INDEX);
                String variable = request.getString(varArg, "");
                if (isFormula(variable)) {
                    if (request.defined(ARG_FORMULA)) {
                        request.remove(ARG_FORMULA);
                    }
                    request.put(ARG_FORMULA, variable);
                }
            }

            List<String> processTabs   = new ArrayList<String>();
            List<String> processHelp   = new ArrayList<String>();
            List<String> processTitles = new ArrayList<String>();

            boolean      first         = true;

            for (Service process : processes) {
                StringBuilder tmpSB = new StringBuilder();
                if (processes.size() > 1) {
                    /*
                tmpSB.append(
                    HtmlUtils.radio(
                        ClimateCollectionTypeHandler.ARG_DATA_PROCESS_ID,
                        process.getId(), first));
                tmpSB.append(HtmlUtils.space(1));
                tmpSB.append(msg("Select"));
                tmpSB.append(HtmlUtils.br());
                */
                } else {
                    tmpSB.append(
                        HtmlUtils.hidden(
                            ClimateCollectionTypeHandler.ARG_DATA_PROCESS_ID,
                            process.getId()));
                }
                if (process.canHandle(dpi)) {
                    process.addToForm(request, dpi, tmpSB, null, null);
                    processTabs.add(tmpSB.toString());
                    processHelp.add(process.getHelp());
                    processTitles.add(process.getLabel());
                    first = false;
                }
            }


            for (int i = 0; i < processTitles.size(); i++) {
                sb.append(HtmlUtils.open("div",
                                         HtmlUtils.cssClass(
                                             "titled_border")));
                String titleString = msg(processTitles.get(i));
                String help        = processHelp.get(i);
                if (help != null) {
                    StringBuilder hsb = new StringBuilder();
                    hsb.append(HtmlUtils.space(1));
                    HtmlUtils.tooltip(hsb, getIconUrl("/icons/help.png"),
                                      help);
                    titleString += hsb.toString();
                }
                sb.append(HtmlUtils.div(titleString, HtmlUtils.id("title")));
                sb.append("<div id=\"content\">\n");
                /*
                if(help!=null) {
                    sb.append("<div class=\"service-help-link\">\n");
                    HtmlUtils.tooltip(sb,getIconUrl("/icons/help.png"), help);
                    sb.append("</div>\n");
                }
                */
                sb.append(processTabs.get(i));
                sb.append("</div>\n");  // content
                sb.append("</div> <!-- titled_border -->\n");
            }

        }

        sb.append("<div class=\"row\">");
        sb.append("  <div class=\"col-md-12 text-center\">");
        sb.append(plotButton);
        sb.append(HtmlUtils.close(HtmlUtils.TAG_DIV));  // button column
        sb.append(HtmlUtils.close(HtmlUtils.TAG_DIV));  // button row

        sb.append(HtmlUtils.close(HtmlUtils.TAG_DIV)
                  + " <!-- col-md-5 -->\n");  // col-md-5 - left column

        // Right column - output
        sb.append(HtmlUtils.open(HtmlUtils.TAG_DIV,
                                 HtmlUtils.cssClass("col-md-7")));
        sb.append(plotButtonDiv);
        sb.append("\n<div id=\"" + formId
                  + "_status\" class=\"padded\"></div>\n");
        sb.append("<div id=\"" + formId
                  + "_output\" class=\"padded\"></div>\n");
        sb.append("<div id=\"" + formId + "_url\"></div>\n");
        sb.append(HtmlUtils.close(HtmlUtils.TAG_DIV)
                  + " <!-- col-md-7 -->\n");  // col-md-7

        sb.append(HtmlUtils.close(HtmlUtils.TAG_DIV));  // input/output row  col-md-5, col-md-7
        sb.append("\n</div>  <!--  ramadda section div -->\n");

        sb.append("\n");
        sb.append(HtmlUtils.script(js.toString()));
        sb.append("\n");

        sb.append(HtmlUtils.formClose());



        //Tack on the acknowldgement
        if (ack.length() >= 0) {
            ack.append(HtmlUtils.close("div"));
            ack.append(HtmlUtils.close("div"));
        }

        sb.append(ack);

        sb.append("\n-section\n");

        StringBuffer sb2 = new StringBuffer();
        sb2.append(getWikiManager().wikify(request, sb.toString()));

        return new Result("Model Comparison", sb2);

    }

    /**
     * Get the time series entry
     *
     * @param request the request
     *
     * @return  the entry
     *
     * @throws Exception problems
     */
    private Entry getTimeSeriesEntry(Request request) throws Exception {
        String arg     = getFieldSelectArg(ARG_COLLECTION2, 0);
        String entryId = request.getSanitizedString(arg, "");
        if (entryId.isEmpty()) {
            return null;
        }
        Entry e = getEntryManager().getEntry(request, entryId);

        return e;
    }


    /**
     * Make the time series selectors
     *
     * @param request  the request
     * @param formId   the form
     *
     * @return  the html
     *
     * @throws Exception problems
     */
    private String makeTimeSeriesSelectors(Request request, String formId)
            throws Exception {
        List<Entry> entries = findTimeSeriesEntries(request);
        if ((entries == null) || entries.isEmpty()) {
            return "No Time Series Found";
        }
        List values = new ArrayList();
        values.add(new TwoFacedObject("--", ""));
        for (Entry e : entries) {
            values.add(new TwoFacedObject(e.getDescription(), e.getId()));
        }
        TwoFacedObject.sort(values);
        StringBuilder dsb           = new StringBuilder();
        String        arg           = getFieldSelectArg(ARG_COLLECTION2, 0);
        List<String>  selectors     = new ArrayList<String>();
        String        extraSelect   = "";
        String        selectedValue = request.getSanitizedString(arg, "");
        String selectBox =
            HtmlUtils.select(arg, values, selectedValue,
                             HtmlUtils.cssClass("multi_select_widget")
                             + HtmlUtils.attr("id",
                                     getFieldSelectId(formId,
                                             ARG_COLLECTION2,
                                             0)) + extraSelect);
        String select = "<label class=\"selector\" for=\""
                        + getFieldSelectId(formId, ARG_COLLECTION2, 0)
                        + "\">" + msgLabel("Climate Index") + "</label>"
                        + selectBox;

        selectors.add(select);
        addSelectorTable(dsb, selectors);

        return dsb.toString();
    }

    /**
     * Add a selector table
     *
     * @param dsb   the html
     * @param selectors the selectors
     */
    private void addSelectorTable(StringBuilder dsb, List<String> selectors) {
        dsb.append(
            "<table cellspacing=\"3px\" cellpadding=\"2px\" align=\"center\">\n");
        for (int i = 0; i < selectors.size(); i++) {
            dsb.append("<tr valign=\"top\">\n");
            dsb.append("<td>");
            dsb.append(selectors.get(i));
            dsb.append("</td>");
        }
        dsb.append("</tr></table>\n");
    }

    /**
     * Find the time series entries
     *
     * @param request  the request
     *
     * @return  the entires
     *
     * @throws Exception problems
     */
    private List<Entry> findTimeSeriesEntries(Request request)
            throws Exception {
        Request tmpRequest = new Request(getRepository(), request.getUser());

        tmpRequest.put(ARG_TYPE, "type_psd_monthly_climate_index");

        return getEntryManager().getEntriesFromDb(tmpRequest);
    }


    /**
     * Get the frequency arguments from the request
     * @param request the Request
     * @return empty string if not defined
     */
    private String getFrequencyArgs(Request request) {
        // TODO Auto-generated method stub
        StringBuilder args = new StringBuilder();
        if (request.defined(ARG_FREQUENCY)) {
            args.append(ARG_FREQUENCY);
            args.append("=");
            args.append(request.getSanitizedString(ARG_FREQUENCY, ""));
        }

        return args.toString();
    }


    /**
     * Find the entries
     *
     * @param request   the Request
     * @param collection  the collection
     * @param entry  the entry
     * @param collectionCnt  the collection count
     *
     * @return the list of entries (may be empty)
     *
     * @throws Exception  problem with search
     */
    private List<Entry> findModelEntries(Request request, String collection,
                                         Entry entry, int collectionCnt)
            throws Exception {

        CollectionTypeHandler typeHandler =
            (CollectionTypeHandler) entry.getTypeHandler();
        List<Clause>    clauses       = new ArrayList<Clause>();
        List<Column>    columns       = typeHandler.getGranuleColumns();
        HashSet<String> seenTable     = new HashSet<String>();
        boolean         haveAllFields = true;
        int             numColumns    = columns.size();
        for (int fieldIdx = 0; fieldIdx < numColumns; fieldIdx++) {
            Column column      = columns.get(fieldIdx);
            String dbTableName = column.getTableName();
            if ( !seenTable.contains(dbTableName)) {
                clauses.add(
                    Clause.eq(typeHandler.getCollectionIdColumn(column),
                              entry.getId()));
                clauses.add(Clause.join(Tables.ENTRIES.COL_ID,
                                        dbTableName + ".id"));
                seenTable.add(dbTableName);
            }
            String arg = "";
            if (request.defined(ARG_COLLECTION)
                    && collection.equals(ARG_COLLECTION2)
                    && column.getName().equals("variable")
                    && (fieldIdx == (numColumns - 1))) {
                arg = getFieldSelectArg(ARG_COLLECTION1, fieldIdx);
                //            } else if (request.exists(ARG_ACTION_MULTI_TIMESERIES) &&
                //                    column.getName().equals("ensemble")) {
                //                continue;
            } else {
                arg = getFieldSelectArg(collection, fieldIdx);
            }
            List v = request.get(arg, new ArrayList());
            if (v.isEmpty()
                    || ((v.size() == 1) && v.get(0).toString().isEmpty())) {
                haveAllFields = false;

                break;
            }
            if (v.size() > 0) {
                List<Clause> ors = new ArrayList<Clause>();
                for (Object o : v) {
                    String s = o.toString();
                    if (s.length() > 0) {
                        if (column.getName().equals("variable")
                                && isFormula(s)) {
                            request.put(ARG_FORMULA, s);
                            List<String> esses =
                                Formula.getFormulaOperands(s);
                            for (String ess : esses) {
                                ors.add(Clause.eq(column.getName(), ess));
                            }
                        } else {
                            ors.add(Clause.eq(column.getName(), s));
                        }
                    }
                }
                if ( !ors.isEmpty()) {
                    clauses.add(Clause.or(ors));
                }
            }
        }
        //System.out.println(clauses);
        //System.out.println(haveAllFields);
        //If we have a fixed collection then don't do the search if no fields were selected
        //if ( !haveAllFields && request.defined(ARG_COLLECTION)) {
        if ( !haveAllFields) {
            return new ArrayList<Entry>();
        }


        List<Entry> entries = getEntryManager().getEntriesFromDb(request,
                                  clauses,
                                  typeHandler.getGranuleTypeHandler());

        entries = getEntryUtil().sortEntriesOnName(entries, false);

        return entries;
    }

    /**
     * _more_
     *
     * @param list _more_
     *
     * @return _more_
     */
    private List sanitizeRequestValues(List list) {
        List v = new ArrayList();
        for (Object item : list) {
            String s = HtmlUtils.sanitizeString(
                           Request.cleanupInput(item.toString()));
            v.add(s);
        }

        return v;
    }


    /**
     * Get the field select argument
     *
     * @param collection  the collection
     * @param fieldIdx  the field index
     *
     * @return  the argument
     */
    public static String getFieldSelectArg(String collection, int fieldIdx) {
        return collection + "_field" + fieldIdx;

    }

    /**
     * Get the collection select argument
     *
     * @param collection  the collection
     *
     * @return  the collection
     */
    public static String getCollectionSelectArg(String collection) {
        return collection;
    }


    /**
     * Get the field select id
     *
     * @param formId   the form id
     * @param collection  the collection
     * @param fieldIdx  the field index
     *
     * @return  the field id
     */
    private String getFieldSelectId(String formId, String collection,
                                    int fieldIdx) {
        return getCollectionSelectId(formId, collection) + "_field"
               + fieldIdx;
    }

    /**
     * Get the collection select id
     *
     * @param formId   the form id
     * @param collection  the collection
     *
     * @return  the collection select id
     */
    private String getCollectionSelectId(String formId, String collection) {
        return formId + "_" + collection;
    }



    /**
     * Process the JSON request
     *
     * @param request  the request
     * @param type  the type of request
     *
     * @return  the JSON string
     *
     * @throws Exception  problem with processing
     */
    private Result processJsonRequest(Request request, String type)
            throws Exception {

        boolean debug = false;
        if (debug) {
            System.err.println("processJsonRequest");
        }

        //        System.err.println("Request:" + request);
        Entry entry = getEntryManager().getEntry(request,
                          request.getString("thecollection",
                                            ""));
        //TODO: this happens for the correlation collection - what should we do?
        if (entry == null) {
            return new Result("", new StringBuilder(), JsonUtil.MIMETYPE);
        }
        //        System.err.println("Entry:" + entry);
        CollectionTypeHandler typeHandler =
            (CollectionTypeHandler) entry.getTypeHandler();
        List<Clause> clauses = new ArrayList<Clause>();
        List<Column> columns = typeHandler.getGranuleColumns();
        List<Clause> ors     = new ArrayList<Clause>();
        for (int fieldIdx = 0; fieldIdx < columns.size(); fieldIdx++) {
            String arg = "field" + fieldIdx;
            //            String column = columns.get(fieldIdx).getName();
            String column = columns.get(fieldIdx).getFullName();
            List   v      = request.get(arg, new ArrayList());
            if ((v.size() == 1) && v.get(0).toString().isEmpty()) {
                continue;
            }
            if (v.size() > 0) {
                List<Clause> myOrs = new ArrayList<Clause>();
                for (Object o : v) {
                    String s = o.toString();
                    if (s.length() > 0) {
                        myOrs.add(Clause.eq(column, s));
                    }
                }
                if ( !myOrs.isEmpty()) {
                    if (myOrs.size() == 1) {
                        clauses.add(myOrs.get(0));
                    } else {
                        ors.addAll(myOrs);
                    }
                }
            }
        }
        if (debug) {
            System.err.println("\tclauses: " + clauses);
            System.err.println("\tors: " + ors);
        }

        int columnIdx = request.get("field", 1);
        if (columnIdx >= columns.size()) {
            return new Result("", new StringBuilder(), JsonUtil.MIMETYPE);
        }
        Column       myColumn  = columns.get(columnIdx);
        List<String> uniqueOrs = new ArrayList<String>();
        int          numOrs    = ors.size();
        int          orNum     = 0;
        List<String> values    = new ArrayList<String>();
        if (ors.size() > 0) {
            for (Clause or : ors) {
                List<Clause> orClauses = new ArrayList<Clause>(clauses);
                orClauses.add(or);
                values = new ArrayList<String>(
                    ((CollectionTypeHandler) entry.getTypeHandler()).getUniqueColumnValues(
                        entry,
                        myColumn,
                        orClauses,
                        false));
                if (orNum == 0) {
                    uniqueOrs.addAll(values);
                } else {
                    uniqueOrs.retainAll(values);
                }
                if (orNum == numOrs - 1) {
                    values = uniqueOrs;
                    if (debug) {
                        System.err.println("final or values:" + values);
                    }
                }
                orNum++;
            }
        } else {
            //System.err.println("no or: "+ clauses);
            values = new ArrayList<String>(
                ((CollectionTypeHandler) entry.getTypeHandler()).getUniqueColumnValues(
                    entry,
                    myColumn,
                    clauses,
                    false));
            if (debug) {
                System.err.println("values:" + values);
            }
        }

        StringBuilder sb = new StringBuilder();
        boolean showBlank =
            !(((type.equals(ARG_ACTION_ENS_COMPARE)
                || type.equals(ARG_ACTION_TEST)
                || type.equals(ARG_ACTION_MULTI_TIMESERIES)
                || type.equals(
                    ARG_ACTION_CORRELATION)) && myColumn.getName().equals(
                        "ensemble"))
              || (type.equals(ARG_ACTION_MULTI_COMPARE)
                  && myColumn.getName().equals("model")));
        if (myColumn.isEnumeration()) {
            List<TwoFacedObject> tfos = typeHandler.getValueList(entry,
                                            values, myColumn);
            /*
            if (myColumn.getName().equals("variable")
                    && type.equals(ARG_ACTION_COMPARE)) {
                addVariableFormulas(tfos);
            }
            */
            if (showBlank) {
                tfos.add(0, new TwoFacedObject(""));
            }
            String json = JsonUtil.tfoList(tfos);
            sb.append(json);
        } else {
            if (showBlank) {
                values.add(0, "");
            }
            String json = JsonUtil.list(values, true);
            sb.append(json);
        }
        //System.out.println("json:" + sb);

        return new Result("", sb, JsonUtil.MIMETYPE);

    }

    /**
     * Add any formulas to the variable list
     * @param tfos  list of variables
     *
     * @throws Exception problem reading formulas
     */
    private void addVariableFormulas(List<TwoFacedObject> tfos)
            throws Exception {
        List<Formula>        formulas = getFormulas();
        List<TwoFacedObject> vars     = new ArrayList<TwoFacedObject>();
        for (Formula form : formulas) {
            vars.add(new TwoFacedObject(form.getName(),
                                        form.getExpression()));
        }
        for (TwoFacedObject var : vars) {
            String       formula  = var.getId().toString();
            List<String> operands = Formula.getFormulaOperands(formula);
            boolean      haveOps  = false;
            for (Object op : operands) {
                haveOps = TwoFacedObject.contains(tfos, op.toString());
            }
            if (haveOps) {
                //tfos.add(new TwoFacedObject(var.getLabel(), StringUtil.join("," , operands)));
                tfos.add(var);
            }
        }
    }

    /**
     * Is this a formula?
     *
     * @param exp problem reading formulas
     *
     * @return true or false
     *
     * @throws Exception can't read formulas
     */
    private boolean isFormula(String exp) throws Exception {
        List<Formula> formulas = getFormulas();
        for (Formula form : formulas) {
            if (form.getExpression().equals(exp)) {
                return true;
            }
        }

        return false;
    }

    /**
     *  return the main entry point URL
     *
     *
     * @param request  the request
     * @param type the type of request
     * @return  the main entry point
     */
    private String getApiUrlPath(Request request, String type) {
        //Use the collection type in the path. This is defined in the api.xml file
        StringBuilder base = new StringBuilder(getRepository().getUrlBase());
        if (type.equals(ARG_ACTION_COMPARE)) {
            base.append("/model/compare");
        } else if (type.equals(ARG_ACTION_MULTI_COMPARE)) {
            base.append("/model/multicompare");
        } else if (type.equals(ARG_ACTION_ENS_COMPARE)) {
            base.append("/model/enscompare");
        } else if (type.equals(ARG_ACTION_CORRELATION)) {
            base.append("/model/correlation");
        } else if (type.equals(ARG_ACTION_TIMESERIES)) {
            base.append("/model/timeseries");
        } else if (type.equals(ARG_ACTION_MULTI_TIMESERIES)) {
            base.append("/model/multitimeseries");
        } else if (type.equals(ARG_ACTION_TEST)) {
            base.append("/model/test");
        }
        if (request.defined(ARG_FREQUENCY)) {
            base.append("?");
            base.append(getFrequencyArgs(request));
        }

        return base.toString();
    }

    /**
     * Get the type handler
     *
     * @return  the climate collection type handler
     *
     * @throws Exception  problem finding one
     */
    private ClimateCollectionTypeHandler getTypeHandler() throws Exception {
        return (ClimateCollectionTypeHandler) getRepository().getTypeHandler(
            collectionType);
    }



    /**
     * Get the list of collections
     *
     * @param request  the request
     *
     * @return  the list of collections
     *
     * @throws Exception  problem generating list
     */
    private List<Entry> getCollections(Request request) throws Exception {
        Request tmpRequest = new Request(getRepository(), request.getUser());

        tmpRequest.put(ARG_TYPE, collectionType);
        List<Clause> fclause = new ArrayList<Clause>();
        if (request.defined(ARG_FREQUENCY)) {
            tmpRequest.put(
                Column.ARG_SEARCH_PREFIX + collectionType + "."
                + ARG_FREQUENCY, request.getSanitizedString(ARG_FREQUENCY,
                        ""));
        }

        return getEntryManager().getEntriesFromDb(tmpRequest);
    }

    /**
     * Get the named time periods
     *
     * @return  the list of named time periods (may be empty)
     *
     * @throws Exception problems
     */
    public List<NamedTimePeriod> getNamedTimePeriods() throws Exception {
        return getNamedTimePeriods(null);
    }

    /**
     * Get the named time periods in the given group
     *
     * @param group  group name
     *
     * @return  the list of periods
     *
     * @throws Exception problems
     */
    public List<NamedTimePeriod> getNamedTimePeriods(String group)
            throws Exception {
        if (namedTimePeriods == null) {
            loadNamedTimePeriods();
        }
        if (group == null) {
            return namedTimePeriods;
        }
        List<NamedTimePeriod> events = new ArrayList<NamedTimePeriod>();
        for (NamedTimePeriod ntp : namedTimePeriods) {
            if (ntp.isGroup(group)) {
                events.add(ntp);
            }
        }

        return events;
    }

    /**
     * Load the NamedTimePeriods
     *
     * @throws Exception problem reading files
     */
    protected void loadNamedTimePeriods() throws Exception {
        if (namedTimePeriods != null) {
            return;
        }
        namedTimePeriods = new ArrayList<NamedTimePeriod>();
        List<String> timePeriodFiles = new ArrayList<String>();
        List<String> allFiles        = getPluginManager().getAllFiles();
        for (String f : allFiles) {
            if (f.endsWith("events.txt")) {
                timePeriodFiles.add(f);
            }
        }

        /*  Maybe we can add this later
        String dir = getStorageManager().getSystemResourcePath() + "/geo";
        List<String> listing = getRepository().getListing(dir, getClass());
        for (String f : listing) {
            if (f.endsWith("events.csv")) {
                timePeriodFiles.add(f);
            }
        }
        */

        for (String path : timePeriodFiles) {
            String contents =
                getStorageManager().readUncheckedSystemResource(path,
                    (String) null);
            if (contents == null) {
                getLogManager().logInfoAndPrint("RAMADDA: could not read:"
                        + path);

                continue;
            }
            //Name;ID;startMonth;endMonth;years
            //Group
            List<String> lines = StringUtil.split(contents, "\n", true, true);
            // get rid of comment
            if (lines.get(0).startsWith("#")) {
                lines.remove(0);
            }
            String group = lines.get(0);
            lines.remove(0);
            for (String line : lines) {
                // skip comments
                if (line.startsWith("#")) {
                    continue;
                }
                List<String> toks = StringUtil.split(line, ";");
                if (toks.size() != 5) {
                    throw new IllegalArgumentException(
                        "Bad named time period line:" + line + "\nFile:"
                        + path);
                }


                namedTimePeriods.add(new NamedTimePeriod(toks.get(0),
                        toks.get(1),
                        group,
                        Integer.parseInt(toks.get(2)),
                        Integer.parseInt(toks.get(3)),
                        toks.get(4)));
            }

        }
    }

    /**
     * Get the formulas
     *
     * @return  the formulas
     *
     * @throws Exception problems
     */
    public List<Formula> getFormulas() throws Exception {
        if (formulas == null) {
            loadFormulas();
        }

        return formulas;
    }

    /**
     * Load the formulas
     *
     * @throws Exception problem reading files
     */
    protected void loadFormulas() throws Exception {
        if (formulas != null) {
            return;
        }
        formulas = new ArrayList<Formula>();
        List<String> formulaFiles = new ArrayList<String>();
        List<String> allFiles     = getPluginManager().getAllFiles();
        for (String f : allFiles) {
            if (f.endsWith("formulas.txt")) {
                formulaFiles.add(f);
            }
        }

        for (String path : formulaFiles) {
            String contents =
                getStorageManager().readUncheckedSystemResource(path,
                    (String) null);
            if (contents == null) {
                getLogManager().logInfoAndPrint("RAMADDA: could not read:"
                        + path);

                continue;
            }
            //Name;exp
            List<String> lines = StringUtil.split(contents, "\n", true, true);
            // get rid of comment
            if (lines.get(0).startsWith("#")) {
                lines.remove(0);
            }
            for (String line : lines) {
                // skip comments
                if (line.startsWith("#")) {
                    continue;
                }
                List<String> toks = StringUtil.split(line, ";");
                if (toks.size() != 2) {
                    throw new IllegalArgumentException("Bad formula line:"
                            + line + "\nFile:" + path);
                }


                formulas.add(new Formula(toks.get(0), toks.get(1)));
            }

        }
    }


    /**
     * Add a help file
     *
     * @param sb  the string buffer
     * @param helpFile  the help file name
     */
    public void addHelp(Appendable sb, String helpFile) {
        if ( !helpFile.startsWith("/")) {
            helpFile = "/org/ramadda/geodata/model/htdocs/model/help/"
                       + helpFile;
        }
        try {
            String helpText =
                getStorageManager().readSystemResource(helpFile);
            HtmlUtils.tooltip(sb, getIconUrl("/icons/help.png"), helpText);
        } catch (Exception excp) {}
    }

    /**
     * Get the help file
     *
     * @param helpFile the help file
     *
     * @return the contents
     */
    public String getHelp(String helpFile) {
        Appendable sb = new StringBuffer();
        addHelp(sb, helpFile);

        return sb.toString();
    }


    /**
     * Get the formula name from the formula expression
     *
     * @param exp the expression
     *
     * @return the name or null
     *
     * @throws Exception problem reading formulas
     */
    public String getFormulaName(String exp) throws Exception {
        List<Formula> forms = getFormulas();
        for (Formula form : forms) {
            if (form.getExpression().equals(exp)) {
                return form.getName();
            }
        }

        return null;
    }


}
