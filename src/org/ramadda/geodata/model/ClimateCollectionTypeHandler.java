/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.model;


import org.ramadda.geodata.thredds.CatalogOutputHandler;
import org.ramadda.repository.Entry;
import org.ramadda.repository.Repository;
import org.ramadda.repository.Request;
import org.ramadda.repository.Result;
import org.ramadda.repository.database.Tables;
import org.ramadda.repository.type.CollectionTypeHandler;


import org.ramadda.service.Service;
import org.ramadda.service.ServiceInput;
import org.ramadda.service.ServiceOperand;
import org.ramadda.service.ServiceOutput;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.WikiUtil;
import org.ramadda.util.sql.Clause;

import org.w3c.dom.Element;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;


import java.io.File;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.TimeZone;


/**
 * Class to handle climate model collections
 */
public class ClimateCollectionTypeHandler extends CollectionTypeHandler {


    /** data process id */
    public static final String ARG_DATA_PROCESS_ID = "data_process_id";

    /** list of data processes */
    private List<Service> processes = new ArrayList<Service>();

    /** NCL output handler */
    private NCLOutputHandler nclOutputHandler;

    /** catalog output handler */
    private CatalogOutputHandler catalogOutputHandler;

    /** image request id */
    public static final String REQUEST_IMAGE = "image";

    /** GoogleEarth kmz request id */
    public static final String REQUEST_KMZ = "kmz";

    /** Timeseries request id */
    public static final String REQUEST_TIMESERIES = "timeseries";

    /** thredds catalog */
    public static final String REQUEST_THREDDSCATALOG = "threddscatalog";

    /**
     * Create a ClimateCollectionTypeHandler
     *
     * @param repository the Repository
     * @param entryNode  the entry Element
     *
     * @throws Exception Problem creating handler
     */
    public ClimateCollectionTypeHandler(Repository repository,
                                        Element entryNode)
            throws Exception {
        super(repository, entryNode);
        processes.add(new CDOArealStatisticsService(repository));
        nclOutputHandler = new NCLOutputHandler(repository);
    }


    /**
     * Get the services
     *
     * @return the list of Services
     */
    public List<Service> getServices() {
        return processes;
    }

    /**
     * Get the HTML display for this type
     *
     * @param request  the Request
     * @param entry    the entry
     *
     * @return  the Result
     *
     * @throws Exception  problem getting the HTML
     */
    @Override
    public Result getHtmlDisplay(Request request, Entry entry,   Entries children)
            throws Exception {
        //Always call this to initialize things
        getGranuleTypeHandler();

        //Check if the user clicked on tree view, etc.
        if ( !isDefaultHtmlOutput(request)) {
            return null;
        }
        Result result = processRequest(request, entry);
        if (result != null) {
            return result;
        }
        if (isWikiText(entry.getDescription())) {
            return null;
        }
        //String wiki = "+section title={{name}}\n{{description wikify=true}}\n{{collection.form}}\n-section+section label=\"Browse\"\n{{tree message=\"\"}}\n\n";
        String wiki =
            "+section title={{name}}\n{{description wikify=true}}\n{{collection.form}}\n-section";
        StringBuilder sb =
            new StringBuilder(getWikiManager().wikifyEntry(request, entry,
                wiki));

        return new Result(msg(getLabel()), sb);
    }

    /**
     * Get the download form
     *
     * @param request the request
     * @param entry the collection entry
     * @param sb  the HTML
     *
     * @throws Exception something went wrong
     */
    private void getDownloadForm(Request request, Entry entry, Appendable sb)
            throws Exception {
        StringBuilder js     = new StringBuilder();
        String        formId = openForm(request, entry, sb, js);
        addSelectorWidgets(request, entry, sb, js, formId);
        addProcessWidgets(request, entry, sb, js, formId);
        sb.append(HtmlUtils.hidden(ARG_MAX,
                                   request.getString(ARG_MAX, "9999")));
        sb.append(HtmlUtils.formClose());
        sb.append(HtmlUtils.script(js.toString()));
    }

    /**
     * Get the wiki include
     *
     * @param wikiUtil  WikiUtil instance
     * @param request   the request
     * @param originalEntry the original entry
     * @param entry the entry for wiki
     * @param tag the wiki tag
     * @param props the properties
     *
     * @return the wiki include text
     *
     * @throws Exception worries
     */
    @Override
    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props)
            throws Exception {

        if (tag.equals("collection.form")) {
            StringBuilder sb = new StringBuilder();
            getDownloadForm(request, entry, sb);

            return getWikiManager().wikifyEntry(request, entry,
                    sb.toString());
        }

        return null;
    }

    /**
     * Add the selector widgets
     *
     * @param request the Request
     * @param entry   the Entry
     * @param sb      the HTML buffer
     * @param js      the JavaScript buffer
     * @param formId  the form identifier
     *
     * @throws Exception  problem creating the form
     */
    protected void addSelectorWidgets(Request request, Entry entry,
                                      Appendable sb, Appendable js,
                                      String formId)
            throws Exception {

        StringBuilder selectorSB = new StringBuilder();
        selectorSB.append(HtmlUtils.formTable());
        addSelectorsToForm(request, entry, selectorSB, formId, js);
        String searchButton = JQ.button("Search", formId + "_search", js,
                                        HtmlUtils.call(formId + ".search",
                                            "event"));
        String downloadButton = JQ.button("Download Data",
                                          formId + "_do_download", js,
                                          HtmlUtils.call(formId
                                              + ".download", "event"));
        String bdownloadButton = JQ.button("Get Download Script",
                                           formId + "_do_bulkdownload", js,
                                           HtmlUtils.call(formId
                                               + ".bulkdownload", "event"));
        String catalogButton = JQ.button("Get THREDDS Catalog",
                                         formId + "_do_threddscatalog", js,
                                         HtmlUtils.call(formId
                                             + ".threddscatalog", "event"));

        selectorSB.append(HtmlUtils.formTableClose());
        sb.append(
            "<table width=100% border=0 cellspacing=0 cellpadding=0><tr valign=top>");
        sb.append("<td width=\"30%\" align=\"center\">");
        String freq = getFrequency(request, entry).trim();

        WikiUtil.heading(sb, "Select " + freq + " Data");
        sb.append(HtmlUtils.div(selectorSB.toString(),
                                HtmlUtils.cssClass("entryselect")));
        StringBuilder buttonBuf = new StringBuilder();
        buttonBuf.append(searchButton);
        buttonBuf.append(HtmlUtils.div(HtmlUtils.buttons(downloadButton,
                bdownloadButton), "style=\"margin-top: 1em;\""));
        sb.append(HtmlUtils.div(buttonBuf.toString()));
        sb.append("</td><td width=\"70%\">");
        sb.append(HtmlUtils.div("",
                                HtmlUtils.cssClass("entryoutput")
                                + HtmlUtils.id(formId + "_output_list")));
        sb.append("</td></tr>");
        sb.append("</table>");
    }

    /**
     * @see org.ramadda.repository.type.CollectionTypeHandler#addSelectorsToForm(org.ramadda.repository.Request, org.ramadda.repository.Entry, java.lang.StringBuilder, java.lang.String, java.lang.StringBuilder)
     *
     * @param request the request
     * @param entry   the entry
     * @param sb      the form
     * @param formId  the form id
     * @param js      the JavaScript
     *
     * @throws Exception something went wrong
     */
    @Override
    public void addSelectorsToForm(Request request, Entry entry,
                                   Appendable sb, String formId,
                                   Appendable js)
            throws Exception {
        super.addSelectorsToForm(request, entry, sb, formId, js);
        if (getFrequency(request, entry).equalsIgnoreCase("Daily")) {
            String widget = msgLabel("Start") + HtmlUtils.space(1)
                            + HtmlUtils.input(ARG_FROMDATE, null, 6)
                            + HtmlUtils.space(3) + msgLabel("End")
                            + HtmlUtils.space(1)
                            + HtmlUtils.input(ARG_TODATE, null, 6);
            sb.append(HtmlUtils.formEntry(msgLabel("Years"), widget));
        }
    }


    /**
     * @see org.ramadda.repository.type.CollectionTypeHandler#addClauses(org.ramadda.repository.Request, org.ramadda.repository.Entry, java.util.List)
     *
     * @param request  the request
     * @param group    the groups
     * @param clauses  the clauses
     *
     * @throws Exception problemos
     */
    @Override
    public void addClauses(Request request, Entry group, List<Clause> clauses)
            throws Exception {
        super.addClauses(request, group, clauses);
        if (request.defined(ARG_FROMDATE) || request.defined(ARG_TODATE)) {
            String           start = request.getString(ARG_FROMDATE, null);
            String           end         = request.getString(ARG_TODATE,
                                               null);
            List<Clause>     dateClauses = new ArrayList<Clause>();
            SimpleDateFormat yfmt = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            yfmt.setTimeZone(TimeZone.getTimeZone("GMT"));
            if (start != null) {
                Date startDate = yfmt.parse(start + "-01-01 00:00");
                dateClauses.add(Clause.ge(Tables.ENTRIES.COL_FROMDATE,
                                          startDate));
            }
            if (end != null) {
                Date endDate = yfmt.parse(end + "-12-31 23:59");
                dateClauses.add(Clause.le(Tables.ENTRIES.COL_TODATE,
                                          endDate));
            }
            Clause dateClause = null;
            if (dateClauses.size() > 1) {
                dateClause = Clause.and(dateClauses);
            } else {
                dateClause = dateClauses.get(0);
            }
            clauses.add(dateClause);
        }
        //System.out.println(clauses);
    }


    /**
     * Get the frequency for the climate collection
     * @param request  the request
     * @param entry  the climate collection entry
     * @return the frequency
     */
    public String getFrequency(Request request, Entry entry) {
        if ( !(entry.getTypeHandler()
                instanceof ClimateCollectionTypeHandler)) {
            throw new IllegalArgumentException("Entry: " + entry.getName()
                    + " is not a ClimateCollection");
        }
        String f = entry.getValue(request,0).toString();

        return f;
    }

    /**
     * Add the processing widgets
     *
     * @param request the Request
     * @param entry   the Entry
     * @param sb      the HTML buffer
     * @param js      the JavaScript buffer
     * @param formId  the form identifier
     *
     * @throws Exception Problem creating widgets
     */
    protected void addProcessWidgets(Request request, Entry entry,
                                     Appendable sb, Appendable js,
                                     String formId)
            throws Exception {
        // for now, don't add in the process widgets - just do a search/download.
        if (true) {
            return;
        }
        String processButtons =
        //JQ.button("Download Data", formId+"_do_download",js, HtmlUtils.call(formId +".download","event"));
        /*
JQ.button(
    "Download Data", formId + "_do_download", js,
    HtmlUtils.call(formId + ".download", "event")) + " "
        +
        */
        JQ.button(
            "Plot Map", formId + "_do_image", js,
            HtmlUtils.call(formId + ".makeImage", "event")) + " "
                + JQ.button(
                    "Google Earth", formId + "_do_kmz", js,
                    HtmlUtils.call(formId + ".makeKMZ", "event")) + " "
                        + JQ.button(
                            "Time Series", formId + "_do_timeseries", js,
                            HtmlUtils.call(
                                formId + ".makeTimeSeries", "event"));
        List<String>  processTabs   = new ArrayList<String>();
        List<String>  processTitles = new ArrayList<String>();

        StringBuilder settingsSB    = new StringBuilder();
        settingsSB.append(HtmlUtils.radio(ARG_DATA_PROCESS_ID, "none", true));
        settingsSB.append(HtmlUtils.space(1));
        settingsSB.append(msg("No Processing"));
        settingsSB.append(HtmlUtils.br());

        processTitles.add(msg("Settings"));
        processTabs.add(HtmlUtils.div(settingsSB.toString(),
                                      HtmlUtils.style("min-height:200px;")));
        for (Service process : processes) {
            //TODO: add radio buttons
            StringBuilder tmpSB = new StringBuilder();
            tmpSB.append(HtmlUtils.radio(ARG_DATA_PROCESS_ID,
                                         process.getId(), false));
            tmpSB.append(HtmlUtils.space(1));
            tmpSB.append(msg("Select"));
            tmpSB.append(HtmlUtils.br());
            ServiceOperand op = new ServiceOperand(entry);
            process.addToForm(request, new ServiceInput(op), tmpSB, null,
                              null);
            processTabs.add(
                HtmlUtils.div(
                    tmpSB.toString(), HtmlUtils.style("min-height:200px;")));
            processTitles.add(process.getLabel());
        }

        sb.append(
            "<table width=100% border=0 cellspacing=0 cellpadding=0><tr valign=top>");
        sb.append("<td width=30%>");
        sb.append(header(msg("Process Selected Data")));
        HtmlUtils.makeAccordion(sb, processTitles, processTabs);
        sb.append(processButtons);
        sb.append("</td><td>");
        sb.append(HtmlUtils.div("",
                                HtmlUtils.cssClass("entryoutput")
                                + HtmlUtils.id(formId + "_output_image")));
        sb.append("</td></tr></table>");
    }

    /**
     * Process the request
     *
     * @param request  the Request
     * @param entry    the Entry
     *
     * @return the Result
     *
     * @throws Exception problems arose
     */
    public Result processRequest(Request request, Entry entry)
            throws Exception {
        Result result = super.processRequest(request, entry);
        if (result != null) {
            return result;
        }
        String what = request.getString(ARG_REQUEST, (String) null);
        if (what == null) {
            return null;
        }
        if (what.equals(REQUEST_IMAGE) || what.equals(REQUEST_KMZ)
                || what.equals(REQUEST_TIMESERIES)
                || what.equals(REQUEST_THREDDSCATALOG)) {
            return processDataRequest(request, entry, what);
        }

        return null;
    }


    /**
     * Get the services to run
     *
     * @param request the request
     *
     * @return  the services
     *
     * @throws Exception  something went wrong
     */
    public List<Service> getServicesToRun(Request request) throws Exception {
        List<Service> processesToRun = new ArrayList<Service>();
        String selectedProcess = request.getString(ARG_DATA_PROCESS_ID,
                                     (String) null);
        if (selectedProcess != null) {
            for (Service process : processes) {
                if (process.getId().equals(selectedProcess)) {
                    processesToRun.add(process);
                }
            }
        }

        return processesToRun;
    }


    /**
     * Process the data request
     *
     * @param request  the Request
     * @param entry    the Entry
     * @param type     the type of request
     *
     * @return  the Result of the Request
     *
     * @throws Exception  on badness
     */
    public Result processDataRequest(Request request, Entry entry,
                                     String type)
            throws Exception {
        //        File   imageFile =     getStorageManager().getTmpFile(request, "test.png");
        //        BufferedImage image = new BufferedImage(600,400,BufferedImage.TYPE_INT_RGB);
        //        Graphics2D g = (Graphics2D) image.getGraphics();

        //Get the entries
        List<Entry> entries = processSearch(request, entry, true);

        List<File>  files   = new ArrayList<File>();
        //Process each one in turn
        boolean       didProcess     = false;
        List<Service> processesToRun = getServicesToRun(request);
        File          processDir     = getStorageManager().createProcessDir();
        for (Service process : processesToRun) {
            System.err.println("MODEL: applying process: "
                               + process.getLabel());
            ServiceOperand op  = new ServiceOperand(entries);
            ServiceInput   dpi = new ServiceInput(processDir, op);
            didProcess = true;
            //TODO:
            ServiceOutput output = process.evaluate(request, null,  dpi, null);
            if (output.hasOutput()) {
                for (ServiceOperand oper : output.getOperands()) {
                    for (Entry outEntry : oper.getEntries()) {
                        if (outEntry.getResource().isFile()) {
                            files.add(outEntry.getResource().getTheFile());
                        }
                    }
                }
            }
        }

        String processId = processDir.getName();
        String processEntryId =
            getStorageManager().getEncodedProcessDirEntryId(processId);

        if (false) {
            String entryUrl =
                HtmlUtils.url(
                    request.getAbsoluteUrl(getRepository().URL_ENTRY_SHOW),
                    ARG_ENTRYID, processEntryId);

            return new Result(entryUrl);
        }


        if ( !didProcess) {
            for (Entry granule : entries) {
                if (granule.isFile()) {
                    files.add(granule.getFile());
                }
            }
        }

        if (type.equals(REQUEST_DOWNLOAD)) {
            return zipFiles(request,
                            IOUtil.stripExtension(entry.getName()) + ".zip",
                            files);
        }

        if (type.equals(REQUEST_BULKDOWNLOAD)) {
            return processBulkDownloadRequest(request, entry);
        }

        if (type.equals(REQUEST_THREDDSCATALOG)) {
            return processThreddsCatalogRequest(request, entry);
        }

        //Make the image
        File imageFile = nclOutputHandler.processRequest(request,
                             files.get(0));

        //And return the result
	return new Result("",
                          getStorageManager().getFileInputStream(imageFile),
                          getRepository().getMimeTypeFromSuffix(imageFile.toString()));
    }


    /**
     *  Overwrite the base class and route it through processImageRequest
     *
     * @param request process a download request
     * @param entry   the Entry
     *
     * @return the Result
     *
     * @throws Exception problem downloading data
     */
    @Override
    public Result processDownloadRequest(Request request, Entry entry)
            throws Exception {
        return processDataRequest(request, entry, REQUEST_DOWNLOAD);
    }

    /**
     * Process the THREDDS catalog request
     *
     * @param request  the request
     * @param entry    the entry
     *
     * @return the script
     *
     * @throws Exception problems
     */
    public Result processThreddsCatalogRequest(Request request, Entry entry)
            throws Exception {
        request.setReturnFilename(entry.getName() + "_catalog.xml");
        request.put(ARG_LATESTOPENDAP, false);
        StringBuilder        sb  = new StringBuilder();
        CatalogOutputHandler coh = getCatalogOutputHandler();

        return coh.outputGroup(request, CatalogOutputHandler.OUTPUT_CATALOG,
                               entry,
			       processSearch(request, entry, true));

    }

    /**
     * Get the catalog output handler
     *
     * @return the handler
     */
    public CatalogOutputHandler getCatalogOutputHandler() {
        if (catalogOutputHandler == null) {
            catalogOutputHandler =
                (CatalogOutputHandler) getRepository().getOutputHandler(
                    org.ramadda.geodata.thredds.CatalogOutputHandler.class);
        }

        return catalogOutputHandler;
    }

}
