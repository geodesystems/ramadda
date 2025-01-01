/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.output;


import org.ramadda.repository.*;
import org.ramadda.repository.job.JobManager;
import org.ramadda.repository.type.*;


import org.ramadda.service.OutputDefinition;
import org.ramadda.service.Service;
import org.ramadda.service.ServiceInput;
import org.ramadda.service.ServiceOutput;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;

import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;

import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.io.*;
import java.io.File;


import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;



public class ServiceOutputHandler extends OutputHandler {

    public static final String ARG_ASYNCH = "asynch";
    public static final String ARG_TOXML = "toxml";
    public static final String ARG_NEWDIRECTORY = "newdirectory";
    public static final String ARG_SHOWCOMMAND = "showcommand";
    public static final String ARG_GOTOPRODUCTS = "gotoproducts";
    public static final String ARG_WRITEWORKFLOW = "writeworkflow";
    public static final String ATTR_ICON = "icon";
    public static final String ATTR_LABEL = "label";
    private OutputType outputType;
    private OutputType groupOutputType;
    private Service service;

    
    public ServiceOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        init(element);
    }


    
    public ServiceOutputHandler(Repository repository, Service service)
            throws Exception {
        super(repository, "");
        this.service = service;
    }


    
    private void init(Element element) throws Exception {
        String serviceId = XmlUtil.getAttribute(element, "serviceId",
                               (String) null);
        if (serviceId != null) {
            service = getRepository().getJobManager().getService(serviceId);
            if (service == null) {
                getLogManager().logError(
                    "ServiceOutputHandler: could not find service:"
                    + serviceId);

                return;
            }

        }

        if (service == null) {
            NodeList children = XmlUtil.getElements(element,
                                    Service.TAG_SERVICE);
            Element serviceNode = element;
            if (children.getLength() > 0) {
                serviceNode = (Element) children.item(0);
            }
            service = getRepository().makeService(serviceNode, true);
        }


        if (service == null) {
            getLogManager().logError(
                "ServiceOutputHandler: could not find service:"
                + XmlUtil.toString(element));

            return;
        }

        outputType = new OutputType(
            XmlUtil.getAttribute(element, ATTR_LABEL, service.getLabel()),
            XmlUtil.getAttribute(element, ATTR_ID, service.getId()),
            OutputType.TYPE_SERVICE|OutputType.TYPE_OTHER | OutputType.TYPE_IMPORTANT, "",
            XmlUtil.getAttribute(element, ATTR_ICON, service.getIcon()));
        addType(outputType);
        groupOutputType = new OutputType(XmlUtil
            .getAttribute(element, ATTR_LABEL, service.getLabel()), "group_"
                + XmlUtil
                    .getAttribute(element, ATTR_ID, service
                        .getId()), OutputType.TYPE_OTHER, "", XmlUtil
                            .getAttribute(element, ATTR_ICON, service
                                .getIcon()));
        addType(groupOutputType);


    }

    
    public boolean isEnabled() {
        return (service != null) && service.isEnabled();
    }

    
    public Service getService() {
        return service;
    }

    
    public void getEntryLinks(Request request, State state, List<Link> links)
            throws Exception {


        if ( !isEnabled()) {
            return;
        }

        if (state.group != null) {
            //Not sure what to do here. If its search results then return
            if (state.group.isDummy()) {
                return;
            }
            if (service.isApplicable(state.entries)) {
                links.add(makeLink(request, state.getEntry(),
                                   groupOutputType));
            }
            if (state.entries != null) {
                for (Entry entry : state.entries) {
                    if (service.isApplicable(entry)) {
                        links.add(makeLink(request, state.getEntry(),
                                           groupOutputType));

                        return;
                    }
                }
            }
	    //            return;
        }

        if (state.getEntry() != null) {
            if (service.isApplicable(state.getEntry())) {
                links.add(makeLink(request, state.getEntry(), outputType));
            }
        }
    }







    
    @Override
    public Result outputEntry(final Request request, OutputType outputType,
                              final Entry entry)
            throws Exception {

        List<Entry> entries = new ArrayList<Entry>();
        entries.add(entry);
        return handleRequest(request, entry, entries);
    }




    
    @Override
    public Result outputGroup(Request request, OutputType outputType,
                              Entry group, List<Entry> children)
            throws Exception {
        return handleRequest(request, group, children);
    }



    
    public Result handleRequest(Request request, Entry entry,
                                List<Entry> entries)
            throws Exception {
        if ( !isEnabled()) {
            return null;
        }


        if ( !request.defined(ARG_EXECUTE)) {
            StringBuffer sb = new StringBuffer();
            getPageHandler().entrySectionOpen(request, entry, sb, "");
            makeForm(request, service, entry, entries, outputType, sb);
            getPageHandler().entrySectionClose(request, entry, sb);

            return new Result(outputType.getLabel(), sb);
        }

        return evaluateService(request, getRepository().URL_ENTRY_SHOW,
                               outputType, entry, entries, service, "");
    }



    
    public boolean doExecute(Request request) {
        return request.defined(ARG_EXECUTE)
               || ( !request.isAnonymous()
                    && request.defined(ARG_SHOWCOMMAND));
    }


    
    public Result evaluateService(final Request request,
                                  RequestUrl requestUrl,
                                  OutputType outputType,
                                  final Entry baseEntry,
                                  List<Entry> theEntries,
                                  final Service service, String extraForm)
            throws Exception {

	if((theEntries==null || theEntries.size()==0) && baseEntry!=null) {
	    theEntries = new ArrayList<Entry>();
	    theEntries.add(baseEntry);
	} else if(theEntries!=null && baseEntry!=null && !theEntries.contains(baseEntry)) {
	    theEntries.add(baseEntry);
	}

	final List<Entry> entries = theEntries;
        String actionName = (outputType != null)
                            ? outputType.getLabel()
                            : "Run service";
        File   workDir    = getCurrentProcessingDir(request, false);


        if (request.get(ARG_NEWDIRECTORY, false) || (workDir == null)
                || !workDir.exists()) {
            workDir = getStorageManager().createProcessDir();
            getSessionManager().putSessionProperty(request, PROP_PROCESSDIR,
                    workDir.toString());
        }

        final String processDirUrl =
            getStorageManager().getProcessDirEntryUrl(request, workDir);



        final List<ServiceInput> serviceInputs =
            new ArrayList<ServiceInput>();

        boolean       asynchronous = request.get(ARG_ASYNCH, false);
        boolean       toXml        = request.get(ARG_TOXML, false);
        final boolean forDisplay   = (toXml
                                      ? true
                                      : ( !request.isAnonymous()
                                          && request.get(ARG_SHOWCOMMAND,
                                              false)));
        final boolean doingPublish = doingPublish(request);

        if (service.requiresMultipleEntries()) {
            ServiceInput serviceInput = new ServiceInput(workDir, entries,
                                            true);
            serviceInput.setPublish(doingPublish(request));
            serviceInput.setForDisplay(forDisplay);
            serviceInputs.add(serviceInput);

        } else {
            if (entries != null) {
                for (Entry entry : entries) {
                    if ( !service.isApplicable(entry)) {
                        continue;
                    }
                    ServiceInput serviceInput = new ServiceInput(workDir,
                                                    entry);
                    serviceInput.setPublish(doingPublish(request));
                    serviceInput.setForDisplay(forDisplay);
                    serviceInputs.add(serviceInput);
                }
            }
        }

        if (serviceInputs.size() == 0) {
            ServiceInput serviceInput = new ServiceInput(workDir);
            serviceInput.setPublish(doingPublish(request));
            serviceInput.setForDisplay(forDisplay);
            serviceInputs.add(serviceInput);
        }




        if (asynchronous) {
            ActionManager.Action action = new ActionManager.Action() {
                public void run(Object actionId) throws Exception {
		    request.putExtraProperty("actionid",actionId);
		    getActionManager().setActionMessage(actionId,"Running service:" + service.getLabel());
                    List<ServiceOutput> outputs =
                        new ArrayList<ServiceOutput>();
                    List<Entry> outputEntries = new ArrayList<Entry>();
                    for (ServiceInput serviceInput : serviceInputs) {
                        try {
                            ServiceOutput output = evaluateService(request, actionId, service, serviceInput);
                            if ( !output.isOk()) {
                                getActionManager().setContinueHtml(
                                    actionId,
                                    getPageHandler().showDialogError(
                                        "An error has occurred:<pre>"
                                        + output.getResults() + "</pre>"));

                                return;
                            }
                            outputEntries.addAll(output.getEntries());
                        } catch (Exception exc) {
                            getActionManager().setContinueHtml(
                                actionId,
                                getPageHandler().showDialogError(
                                    "An error has occurred:<pre>" + exc
                                    + "</pre>"));

                            return;
                        }
                    }

		    getActionManager().setActionMessage(actionId,"Service complete");
                    String url = processDirUrl;
                    if (doingPublish && (outputEntries.size() > 0)) {
                        url = request.entryUrl(
                            getRepository().URL_ENTRY_SHOW,
                            outputEntries.get(0));
                    }
                    getActionManager().setContinueHtml(actionId,
                            HU.href(url, msg("Continue")));
                }
            };

            return getActionManager().doAction(request, action, actionName,
                    "");
        }


        StringBuffer        sb            = new StringBuffer();
        List<Entry>         outputEntries = new ArrayList<Entry>();
        List<ServiceOutput> outputs       = new ArrayList<ServiceOutput>();
        for (ServiceInput serviceInput : serviceInputs) {
            ServiceOutput output = evaluateService(request, null,service,   serviceInput);
            outputs.add(output);

            if ( !output.isOk()) {
                getPageHandler().entrySectionOpen(request, baseEntry, sb, "");
                sb.append(
                    getPageHandler().showDialogError(
                        "An error has occurred:<pre>" + output.getResults()
                        + "</pre>"));
                makeForm(request, service, baseEntry, entries, requestUrl,
                         outputType, sb, extraForm);

                getPageHandler().entrySectionClose(request, baseEntry, sb);

                return new Result(actionName, sb);
            }
            outputEntries.addAll(output.getEntries());
            if (serviceInput.getForDisplay()) {
                sb.append(output.getResults());
                sb.append("\n");
            } else if (output.getResultsShownAsText()) {
                service.addOutput(request, serviceInput, output, sb);
            }
        }

        if (toXml) {
            StringBuilder xml = new StringBuilder();
            service.toXml(xml, (serviceInputs.size() > 0)
                               ? serviceInputs.get(0)
                               : null);
            request.setReturnFilename(service.getLabel() + "services.xml");

            return new Result("", xml, "text/xml");
        }


        if (forDisplay) {
            StringBuffer commands = new StringBuffer();
            getPageHandler().entrySectionOpen(request, baseEntry, commands,
                    "");
            commands.append("<div class=service-output>");
            commands.append("<pre>");
            commands.append(sb);
            commands.append("</pre>");
            commands.append("</div>");
            makeForm(request, service, baseEntry, entries, requestUrl,
                     outputType, commands, extraForm);
            getPageHandler().entrySectionClose(request, baseEntry, commands);

            return new Result(actionName, commands);
        }


        writeProcessEntryXml(request, service, workDir, sb.toString());


        if (doingPublish(request) && (outputEntries.size() > 0)) {
            return new Result(
                request.entryUrl(
                    getRepository().URL_ENTRY_SHOW, outputEntries.get(0)));
        }


        //Redirect to the products dir entry 
        if (request.get(ARG_GOTOPRODUCTS, false) && !forDisplay) {
            return new Result(processDirUrl);
        }

        if (outputEntries.size() > 1) {
            List<File> files = new ArrayList<File>();
            for (Entry newEntry : outputEntries) {
                files.add(newEntry.getFile());
            }

            return getRepository().zipFiles(request, "results.zip", files);
        }
        if (outputEntries.size() == 1) {
            File file = outputEntries.get(0).getFile();
            request.setReturnFilename(file.getName());

            return new Result(getStorageManager().getFileInputStream(file),
                              "");
        }


        if ((outputs.size() > 0) && !outputs.get(0).getResultsShownAsText()) {
            sb.append("Error: no output files<br>");
            sb.append("<pre>");
            for (ServiceOutput output : outputs) {
                sb.append(output.getResults());
            }
            sb.append("</pre>");
            sb.append(HU.hr());
        }
        Appendable results = new StringBuilder();
        getPageHandler().entrySectionOpen(request, baseEntry, results, "");
        makeForm(request, service, baseEntry, entries, requestUrl,
                 outputType, results, extraForm);
        if (sb.length() > 0) {
            results.append(
                HU.div(
                    msg("Results"),
                    HU.cssClass("service-results-header")));
            results.append(
                HU.open(
                    HU.TAG_DIV,
                    HU.cssClass("service-results")));
            results.append(sb);
            results.append(HU.close(HU.TAG_DIV));
        }
        getPageHandler().entrySectionClose(request, baseEntry, results);

        return new Result(actionName, results);


    }


    
    private ServiceOutput evaluateService(Request request, Object actionID, Service service,
                                          ServiceInput serviceInput)
            throws Exception {
        ServiceOutput output;

        try {
            output = service.evaluate(request, actionID,serviceInput, null);
            if ( !output.isOk()) {
                return output;
            }
            writeWorkflow(request, serviceInput);
            writeProcessEntryXml(request, service,
                                 serviceInput.getProcessDir(), "");

            return output;
        } catch (Exception exc) {
            Throwable thr = ucar.unidata.util.LogUtil.getInnerException(exc);
            thr.printStackTrace();

            return new ServiceOutput(false, thr.toString());
        }

    }
    
    public void makeForm(Request request, Service service, Entry baseEntry,
                         List<Entry> entries, OutputType outputType,
                         Appendable sb)
            throws Exception {
        makeForm(request, service, baseEntry, entries,
                 getRepository().URL_ENTRY_SHOW, outputType, sb, "");
    }

    public void makeForm(Request request, Service service, Entry baseEntry,
                         List<Entry> entries, RequestUrl url,
                         OutputType outputType, Appendable sb,
                         String extraForm)
            throws Exception {

        sb.append("\n");
        sb.append(HU.comment("Begin service form"));
        String formId = HU.getUniqueId("form_");
        request.uploadFormWithAuthToken(sb, url, HU.id(formId));

        if (baseEntry != null) {
            sb.append(HU.hidden(ARG_ENTRYID, baseEntry.getId()));
        }
        if (outputType != null) {
            sb.append(HU.hidden(ARG_OUTPUT, outputType.getId()));
        }

        if (extraForm != null) {
            sb.append(extraForm);
        }


        boolean                haveAnyOutputs = false;
        List<OutputDefinition> outputs = new ArrayList<OutputDefinition>();

        service.getAllOutputs(outputs);
        for (OutputDefinition output : outputs) {
            if ( !output.getShowResults()) {
                haveAnyOutputs = true;

                break;
            }
        }

        List<String> extraSubmit  = new ArrayList<String>();
        String       extraDirHtml = "";
        File         currentDir   = getCurrentProcessingDir(request, true);
        if (currentDir != null) {
            extraDirHtml = HU.space(2) + "("
                           + HU.href(
                               getStorageManager().getProcessDirEntryUrl(
                                   request, currentDir), msg("View current"),
                                       HU.attrs(
                                           "target", "_view")) + ")";

        }

        extraSubmit.add(HU.labeledCheckbox(ARG_NEWDIRECTORY, "true",
                request.get(ARG_NEWDIRECTORY, true),
                msg("Create new processing folder") + extraDirHtml));
        extraSubmit.add(HU.labeledCheckbox(ARG_GOTOPRODUCTS, "true",
                request.get(ARG_GOTOPRODUCTS, haveAnyOutputs),
                "Go to products page"));
        extraSubmit.add(HU.labeledCheckbox(ARG_WRITEWORKFLOW, "true",
                request.get(ARG_WRITEWORKFLOW, false), "Write workflow"));
        if ( !request.isAnonymous()) {
            extraSubmit.add(HU.labeledCheckbox(ARG_SHOWCOMMAND,
                    "true", request.get(ARG_SHOWCOMMAND, false),
                    "Show command"));
        }
        extraSubmit.add(HU.labeledCheckbox(ARG_TOXML, "true",
                request.get(ARG_TOXML, false), msg("Export full XML")));
        if (haveAnyOutputs) {
	    if(service.getAsynch()) {
		extraSubmit.add(HU.hidden(ARG_ASYNCH, "true"));
	    } else {
		extraSubmit.add(HU.labeledCheckbox(ARG_ASYNCH, "true",
							  request.get(ARG_ASYNCH, false), msg("Asynchronous")));
	    }
        }

	if((entries==null || entries.size()==0) && baseEntry!=null) {
	    entries = new ArrayList<Entry>();
	    entries.add(baseEntry);
	}


        service.addToForm(request, (entries != null && entries.size()>0)
                                   ? new ServiceInput(null, entries, true)
                                   : new ServiceInput(), sb, null, null);

        sb.append(HU.hidden(Service.ARG_SERVICEFORM, "true"));

        StringBuilder buttons = new StringBuilder();

        buttons.append(HU.submit("Execute", ARG_EXECUTE,
                                        makeButtonSubmitDialog(sb,
                                            "Processing request...")));
        StringBuffer etc = new StringBuffer();
        if (haveAnyOutputs) {
	    etc.append(HU.formTable());
            addPublishWidget(request, baseEntry, etc,
                             msg("Select a folder to publish to"), true,
                             false);
	    etc.append(HU.formTableClose());
	    TypeHandler typeHandler = service.getOutputTypeHandler();
	    if(typeHandler!=null) {
		typeHandler.getFileExtras( request, null, etc);
	    }

        }



        etc.append(StringUtil.join("&nbsp; <br> ", extraSubmit));
        etc.append(HU.p());


        addUrlShowingForm(etc, formId, null);
	buttons.append("<br>");
        buttons.append(HU.makeShowHideBlock("Options...",
                HU.insetDiv(etc.toString(), 0, 20, 0, 0), false));

        sb.append(HU.div(buttons.toString(),
                                HU.cssClass("service-form-buttons")));

        sb.append(HU.formClose());
        sb.append("\n");
        sb.append(HU.comment("end service form"));
        sb.append("\n");

    }
    
    private void writeWorkflow(Request request, ServiceInput serviceInput)
            throws Exception {
        if (request.get(ARG_WRITEWORKFLOW, false)) {

            String workflowXml = service.getLinkXml(serviceInput);
            File workflowFile =
                new File(IOUtil.joinDir(serviceInput.getProcessDir(),
                                        "serviceworkflow.xml"));
            IOUtil.writeFile(workflowFile, workflowXml.toString());
            workflowXml =
                "<entry type=\"type_service_file\" name=\"Service workflow\" />";
            IOUtil.writeFile(getEntryManager().getEntryXmlFile(workflowFile),
                             workflowXml.toString());

        }
    }

    private void writeProcessEntryXml(Request request, Service service,
                                      File processDir, String desc)
            throws Exception {

        String pdesc = service.getProcessDescription();
        if (Utils.stringDefined(pdesc)) {
            desc = pdesc.replace("{{description}}", (desc == null)
                    ? ""
                    : desc);
        }
        StringBuffer xml = new StringBuffer();
        if (desc == null) {
            desc = "";
        }
	if(desc.indexOf("{{tree") <0) {
	    desc = desc+"\n{{tree message=\"\"}}";
	}
        desc = "<wiki>\n+section title={{name}}\n" + desc
               + "\n-section\n";
        xml.append(
            XmlUtil.tag(
                "entry",
                XmlUtil.attrs("type", "group", "name", "Processing Results"),
                XmlUtil.tag("description", "", XmlUtil.getCdata(desc))));

        IOUtil.writeFile(new File(IOUtil.joinDir(processDir,
                ".this.ramadda.xml")), xml.toString());
    }


}
