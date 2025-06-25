/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.data.services;

import org.ramadda.data.record.*;
import org.ramadda.data.record.filter.*;

import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.job.JobInfo;

import org.ramadda.repository.job.JobManager;
import org.ramadda.repository.map.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.TypeHandler;

import org.ramadda.util.geo.Bounds;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JQuery;
import org.ramadda.util.Utils;

import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.*;
import java.util.zip.*;

/**
 */
@SuppressWarnings("unchecked")
public class RecordJobManager extends JobManager implements RecordConstants {

    private RecordOutputHandler recordOutputHandler;

    /**
     * ctor
     *
     *
     * @param recordOutputHandler _more_
     */
    public RecordJobManager(RecordOutputHandler recordOutputHandler) {
        super(recordOutputHandler.getRepository());
        this.recordOutputHandler = recordOutputHandler;
    }

    /**
     * get the url that lists the job status
     *
     * @param request The request
     * @param entry the entry
     * @param jobId The job ID
     * @param output _more_
     *
     * @return url to job status page
     */
    public String getJobUrl(Request request, Entry entry, Object jobId,
                            OutputType output) {
        String actionUrl = request.getAbsoluteUrl(
                               request.entryUrl(
                                   getRepository().URL_ENTRY_SHOW, entry,
                                   new String[] { ARG_OUTPUT,
                output.getId(), JobInfo.ARG_JOB_ID, jobId.toString() }));

        return actionUrl;
    }

    public RecordOutputHandler getRecordOutputHandler() {
        return recordOutputHandler;
    }

    public RecordFormHandler getRecordFormHandler() {
        return recordOutputHandler.getFormHandler();
    }

    /**
     * Apply the visitor to each record file in turn
     *
     * @param request the request
     * @param recordEntries entries to process
     * @param visitor The visitor to apply to the record file
     *
     * @throws Throwable _more_
     */
    public void visitSequential(Request request,
                                List<? extends RecordEntry> recordEntries,
                                RecordVisitor visitor)
            throws Throwable {
        visitSequential(request, recordEntries, visitor, null);
    }

    /**
     * Apply the visitor to each record file in turn
     *
     * @param request the request
     * @param recordEntries entries to process
     * @param visitor The visitor to apply to the record file
     * @param visitInfo visit state
     *
     * @throws Throwable _more_
     */
    public void visitSequential(
            final Request request,
            final List<? extends RecordEntry> recordEntries,
            final RecordVisitor visitor, final VisitInfo visitInfo)
            throws Throwable {
        Callable<Boolean> callable = new Callable<Boolean>() {
            public Boolean call() {
                try {
                    //                    System.err.println("POINT: processing started");
                    long t1 = System.currentTimeMillis();
                    for (RecordEntry recordEntry : recordEntries) {
                        if ( !recordEntry.isEnabled()) {
                            continue;
                        }
                        recordEntry.visit(visitor, visitInfo);
                    }
                    long t2 = System.currentTimeMillis();
                    visitor.close(visitInfo);
                    //                    System.err.println("POINT: processing done time:" + (t2 - t1));

                    return Boolean.TRUE;
                } catch (Exception exc) {
                    throw new RuntimeException(exc);
                }
            }

        };
        invokeAndWait(callable);
    }

    public void visit(
            final Request request,
            final List<? extends RecordEntry> recordEntries,
            final RecordVisitor visitor, final VisitInfo visitInfo)
            throws Throwable {
	long t1 = System.currentTimeMillis();
	for (RecordEntry recordEntry : recordEntries) {
	    if ( !recordEntry.isEnabled()) {
		continue;
	    }
	    recordEntry.visit(visitor, visitInfo);
	}
	long t2 = System.currentTimeMillis();
	visitor.close(visitInfo);
	//                    System.err.println("POINT: processing done time:" + (t2 - t1));
    }

    /**
     * Apply the visitor to to the record file
     *
     * @param request The request
     * @param recordEntry the record entry
     * @param visitor the visitor
     * @param visitInfo visit info
     *
     * @throws Throwable _more_
     */
    public void visitSequential(Request request, RecordEntry recordEntry,
                                RecordVisitor visitor, VisitInfo visitInfo)
            throws Throwable {
        List<RecordEntry> recordEntries = new ArrayList<RecordEntry>();
        recordEntries.add(recordEntry);
        visitSequential(request, recordEntries, visitor, visitInfo);
    }

    /**
     * This applies the visitor to each of the RecordEntries concurrently (well, using the executor service)
     *
     * @param request The request
     * @param recordEntries The entries to process
     * @param visitor the visitor
     * @param visitInfo visit info
     *
     * @throws Throwable _more_
     */
    public void visitConcurrent(Request request,
                                List<? extends RecordEntry> recordEntries,
                                RecordVisitor visitor, VisitInfo visitInfo)
            throws Throwable {
        for (RecordEntry recordEntry : recordEntries) {
            recordEntry.setVisitInfo(visitor, (visitInfo != null)
                    ? new VisitInfo(visitInfo)
                    : null);
        }
        invokeAndWait(makeCallables(recordEntries));
        visitor.close(visitInfo);
    }

    /**
     * utility to make a list of Callable objects for the record entries
     *
     * @param recordEntries The entries to process
     *
     * @return list of callables
     */
    private List<Callable<Boolean>> makeCallables(
            List<? extends RecordEntry> recordEntries) {
        List<Callable<Boolean>> callables =
            new ArrayList<Callable<Boolean>>();
        for (RecordEntry recordEntry : recordEntries) {
            callables.add(recordEntry);
        }

        return callables;
    }

    /**
     * done processing
     *
     * @param request The request
     * @param entry the entry
     * @param recordEntries _more_
     * @param outputType the output type
     * @param jobId The job ID
     *
     * @throws Exception On badness
     */
    public void asynchRequestFinished(
            final Request request, final Entry entry,
            final List<? extends RecordEntry> recordEntries,
            OutputType outputType, Object jobId)
            throws Exception {

        JobInfo jobInfo = getJobInfo(jobId);
        if (jobInfo == null) {
            logError("ERROR: Could not find JobInfo: " + jobId,
                     new IllegalStateException(""));

            return;
        }
        if (jobInfo.isInError()) {
            return;
        }

        final File productDir = getRecordOutputHandler().getProductDir(jobId);
        StringBuffer status   = new StringBuffer();
        boolean doingPublish  =
            getRecordOutputHandler().doingPublish(request);
        if (doingPublish) {
            Entry parent = getEntryManager().findGroup(request,
                               request.getString(ARG_PUBLISH_ENTRY
                                   + "_hidden", ""));
            if (parent == null) {
                throw new IllegalArgumentException("Could not find folder");
            }
            if ( !getAccessManager().canDoNew(request, parent)) {
                throw new AccessException("No access", request);
            }
            File[] files = productDir.listFiles();
            for (File f : files) {
                if (f.getName().startsWith(".")) {
                    continue;
                }
                if (request.getExtraProperty(
                        IOUtil.getFileTail(f.toString())) != null) {
                    continue;
                }
                String filename = f.getName();

                String destFilename = getRepository().getGUID()
                                      + StorageManager.FILE_SEPARATOR
                                      + filename;
                f = getStorageManager().copyToStorage(request, f,
                        destFilename);

                String        name = request.getString(ARG_PUBLISH_NAME, "");
                TypeHandler   typeHandler = null;

                final boolean isPointFile = false;
                //TODO: get the actual type handler
                //                    PointTypeHandler.isPointFile(f.toString());
                if (isPointFile) {
                    typeHandler =
                        recordEntries.get(0).getEntry().getTypeHandler();
                } else if (Resource.isImage(f.getName())) {
                    //Check if the latlonimage entry type is loaded
                    TypeHandler latLonImageTypeHandler =
                        getRepository().getTypeHandler("latlonimage");
                    if (latLonImageTypeHandler != null) {
                        typeHandler = latLonImageTypeHandler;
                    }
                }
                if (name.length() == 0) {
                    name = filename;
                }
                final Bounds bounds = recordEntries.get(0).getBounds();

                //The initializer gets called by the EntryManager to do any initialization
                //of the entry before it gets added to the repository
                EntryInitializer initializer = new EntryInitializer() {
                    public void initEntry(Entry newEntry) {
                        if ( !isPointFile) {
                            if (bounds != null) {
                                newEntry.setBounds(bounds);
                            } else {
                                newEntry.setNorth(request.get(ARG_AREA_NORTH,
                                        entry.getNorth(request)));
                                newEntry.setWest(request.get(ARG_AREA_WEST,
                                        entry.getWest(request)));
                                newEntry.setSouth(request.get(ARG_AREA_SOUTH,
                                        entry.getSouth(request)));
                                newEntry.setEast(request.get(ARG_AREA_EAST,
                                        entry.getEast(request)));
                            }
                        }
                    }
                };

                Entry newEntry = getEntryManager().addFileEntry(request, f,
								parent, null, name, "", request.getUser(),
                                     typeHandler, initializer);

                if (status.length() == 0) {
                    status.append("<b>Published Entries</b><br>");
                }
                status.append(
                    HtmlUtils.href(
                        HtmlUtils.url(
                            getRepository().URL_ENTRY_SHOW.toString(),
                            new String[] { ARG_ENTRYID,
                                           newEntry.getId() }), newEntry
                                           .getName()));

                status.append("<br>");
                getAuthManager().addAuthToken(request);
                getRepository().getAssociationManager().addAssociation(
                    request, newEntry, entry, "generated product",
                    "product generated from");
            }
        }
        if (status.length() > 0) {
            status.append(HtmlUtils.p());
            status.append("\n");
            jobInfo.appendExtraInfo(status.toString());
        }

        final String email = request.getString(ARG_JOB_EMAIL, "");
        if ((email.length() > 0) && getMailManager().isEmailEnabled()) {
            final String actionUrl = jobInfo.getJobStatusUrl();
            final String emailContents =
                "Your RAMADDA point data processing job has completed:\n"
                + actionUrl;
            //Put the mail sending in a thread
            Misc.run(new Runnable() {
                public void run() {
                    try {
                        getRepository().getMailManager().sendEmail(email,
                                "RAMADDA point data processing job",
                                emailContents, false);
                    } catch (Exception exc) {}
                }
            });
        }

        long   productSize = 0;
        File[] files       = productDir.listFiles();
        //TODO: Should zip them here.
        for (File f : files) {
            if (f.getName().startsWith(".")
                    || f.getName().endsWith("_all.zip")) {
                continue;
            }
            productSize = f.length();
        }
        jobInfo.setProductSize(productSize);
        jobHasFinished(jobInfo);
    }

    /**
     * this shows either the html or xml listing of the job status
     *
     * @param request The request
     * @param entry the entry
     *
     * @return the ramadda result
     *
     * @throws Exception On badness
     */
    @Override
    public Result handleJobStatusRequest(Request request, Entry entry)
            throws Exception {

        Result parentResult = super.handleJobStatusRequest(request, entry);
        if (parentResult != null) {
            return parentResult;
        }
        StringBuffer sb    = new StringBuffer();
        String       jobId = request.getString(ARG_JOB_ID, (String) null);
        String productId = request.getString(ARG_POINT_PRODUCT,
                                             (String) null);

        StringBuffer xml     = new StringBuffer();
        JobInfo      jobInfo = getJobInfo(jobId);
        File productDir      = getRecordOutputHandler().getProductDir(jobId);
        if ( !productDir.exists()) {
            return getRepository().makeErrorResult(request,
                    "The results have expired. Please try your query again");
        }

        openHtmlHeader(request, jobInfo, sb);
        sb.append(jobInfo.getDescription().replaceAll("\n", "<br>"));

        if (productId != null) {
            if (productId.equals("zip")) {
                OutputStream os =
                    request.getHttpServletResponse().getOutputStream();
                request.getHttpServletResponse().setContentType(
                    "application/zip");

                ZipOutputStream zos   = new ZipOutputStream(os);

                File[]          files = productDir.listFiles();
                for (File f : files) {
                    if (f.getName().startsWith(".")) {
                        continue;

                    }
                    zos.putNextEntry(new ZipEntry(f.getName()));
                    InputStream fis =
                        getStorageManager().getFileInputStream(f.toString());
                    IOUtil.writeTo(fis, zos);
                    IOUtil.close(fis);
                }
                IOUtil.close(zos);
                Result result = new Result();
                result.setNeedToWrite(false);

                return result;
            }

            return new Result(
                "",
                getStorageManager().getFileInputStream(
                    IOUtil.joinDir(productDir, productId)), "");
        }

        boolean stillRunning = jobInfo.isRunning();
        long    startTime    = jobInfo.getStartDate().getTime();
        long    endTime      = (stillRunning
                                ? new Date().getTime()
                                : jobInfo.getEndDate().getTime());

        if (request.responseAsXml()) {
            //            return handleJobStatusRequestXml(request, entry);
        }

        String jobAttrs;
        if (stillRunning) {
            jobAttrs = XmlUtil.attrs(new String[] {
                JobManager.ATTR_STATUS, STATUS_RUNNING, ATTR_NUMBEROFPOINTS,
                "" + jobInfo.getNumPoints(), ATTR_ELAPSEDTIME,
                "" + ((endTime - startTime) / 1000)
            });
        } else {
            jobAttrs = XmlUtil.attrs(new String[] {
                ATTR_NUMBEROFPOINTS, "" + jobInfo.getNumPoints(), ATTR_STATUS,
                STATUS_DONE, ATTR_ELAPSEDTIME,
                "" + ((endTime - startTime) / 1000),
            });
        }
        xml.append(XmlUtil.openTag(TAG_JOB, jobAttrs));

        if (jobInfo.isInError()) {
            sb.append(
                getPageHandler().showDialogError(
                    "An error occurred while processing the request:<br>"
                    + jobInfo.getError()));
        } else if (stillRunning) {
            sb.append(getRepository().progress("Job is running"));
        } else {
            sb.append(
                getPageHandler().showDialogNote("Processing is complete"));
        }
        sb.append(HtmlUtils.formTable());
        sb.append("<tr><td width=20%></a><td width=80%></td></tr>");
        if ( !jobInfo.isInError() && !stillRunning) {
            sb.append(
                HtmlUtils.formEntry(
                    "",
                    HtmlUtils.div(
                        HtmlUtils.href(
                            jobInfo.getReturnUrl(),
                            "Return to form"), HtmlUtils.cssClass(
                                "ramadda-button"))));
        }
        //set the column width
        if (stillRunning) {
            String cancelUrl =
                request.entryUrl(getRepository().URL_ENTRY_SHOW, entry,
                                 new String[] {
                ARG_OUTPUT, getOutputResults().getId(), ARG_JOB_ID, jobId,
                ARG_CANCEL, "true"
            });
            String cancel =
                HtmlUtils.div(HtmlUtils.href(cancelUrl, msg("Cancel job")),
                              HtmlUtils.cssClass("ramadda-button"));
            sb.append(HtmlUtils.formEntry("", cancel));
        }

        sb.append(HtmlUtils.formEntry("", jobInfo.getExtraInfo().toString()));

        if ( !stillRunning) {
            StringBuffer productSB = new StringBuffer();

            //List the files available
            int    fileCnt = 0;
            File[] files   = productDir.listFiles();
            xml.append(XmlUtil.openTag(TAG_PRODUCTS));
            for (File f : files) {
                if (f.getName().startsWith(".")) {
                    continue;
                }
                if (fileCnt == 0) {
                    productSB.append("<table>");
                }
                fileCnt++;
                String getFileUrl =
                    HtmlUtils.url(getRepository().URL_ENTRY_SHOW + "/"
                                  + f.getName(), new String[] {
                    ARG_ENTRYID, entry.getId(), ARG_OUTPUT,
                    getOutputResults().getId(), ARG_JOB_ID, jobId,
                    ARG_POINT_PRODUCT, f.getName()
                });
                //                xml.append(XmlUtil.openTag(TAG_URL));
                xml.append("<" + TAG_URL + ">");
                XmlUtil.appendCdata(xml, request.getAbsoluteUrl(getFileUrl));
                xml.append(XmlUtil.closeTag(TAG_URL));
                productSB.append("<tr><td>");
                productSB.append(HtmlUtils.href(getFileUrl, f.getName()));
                productSB.append("</td><td align=right>");
                productSB.append(
                    getRecordFormHandler().formatFileSize(f.length()));
                productSB.append("</td></tr>");
            }

            xml.append(XmlUtil.closeTag(TAG_PRODUCTS));
            if (fileCnt > 1) {
                String getFileUrl =
                    HtmlUtils.url(getRepository().URL_ENTRY_SHOW
                                  + "/all.zip", new String[] {
                    ARG_ENTRYID, entry.getId(), ARG_OUTPUT,
                    getOutputResults().getId(), ARG_JOB_ID, jobId,
                    ARG_POINT_PRODUCT, "zip"
                });
                productSB.append("<tr><td>");
                productSB.append(HtmlUtils.href(getFileUrl, "Zip products"));
                productSB.append("</td></tr>");
            }

            if (fileCnt > 0) {
                productSB.append("</table>");
            } else {
                productSB.append(
                    getPageHandler().showDialogNote(
                        msg("No product files found")));
            }

            if (productSB.length() > 0) {
                sb.append(HtmlUtils.formEntryTop(msgLabel("Products"),
                        productSB.toString()));
            }
        }

        sb.append(HtmlUtils.formEntry(msgLabel("Job ID"), jobId));

        sb.append(HtmlUtils.formEntry(msgLabel("Job Name"),
                                      jobInfo.getJobName()));

        if (jobInfo.getJobUrl() != null) {
            sb.append(HtmlUtils.formEntry(msgLabel("Job URL"),
                                          jobInfo.getJobUrl()));
        }
        sb.append(
            HtmlUtils.formEntry(
                msgLabel("Start time"),
                getRecordFormHandler().formatDate(jobInfo.getStartDate())));

        if ( !stillRunning) {
            sb.append(
                HtmlUtils.formEntry(
                    msgLabel("End time"),
                    getRecordFormHandler().formatDate(jobInfo.getEndDate())));
        }

        sb.append(HtmlUtils.formEntry(msgLabel("Run time"),
                                      ((endTime - startTime) / 1000)
                                      + " seconds"));

        if (stillRunning) {
            StringBuffer statusSB = new StringBuffer();
            for (String statusItem : jobInfo.getStatusItems()) {
                statusSB.append(statusItem);
                statusSB.append("<br>");
            }
            String currentStatus = jobInfo.getCurrentStatus();
            if (currentStatus != null) {
                statusSB.append(currentStatus);
            }

            sb.append("<meta http-equiv=\"refresh\" content=\"1\">");
            sb.append(HtmlUtils.formEntry(msgLabel("Status"),
                                          statusSB.toString()));

        }

        if (jobInfo.getNumPoints() != 0) {
            sb.append(
                HtmlUtils.formEntry(
                    msgLabel("Processed"),
                    getRecordFormHandler().formatPointCount(
                        jobInfo.getNumPoints()) + " points"));
        }

        xml.append(XmlUtil.closeTag(TAG_JOB));
        sb.append(HtmlUtils.formTableClose());

        if (request.responseAsXml()) {
            return getRepository().makeOkResult(request, xml.toString());
        }

        closeHtmlHeader(request, jobInfo, sb);

        return new Result("Products", sb);

    }

    public OutputType getOutputResults() {
        return getRecordOutputHandler().OUTPUT_RESULTS;
    }

    public String makeJobUrl(Request dummy) {
        return dummy.getAbsoluteUrl();
    }

    /**
     * This does the product generation asynchronously. It creates a job id, spawns off a thread to actually
     * perform the job and returns a redirect to the job status page.
     *
     * @param request job ingo
     * @param entry the entry
     * @param outputType the output type
     * @param pointEntries list of point entries to process
     *
     * @return the result
     *
     * @throws Exception On badness
     */
    public Result handleAsynchRequest(
            Request request, Entry entry, OutputType outputType,
            List<? extends RecordEntry> pointEntries)
            throws Exception {

        checkNewJobOK();
        try {
            return handleAsynchRequestInner(request, entry, outputType,
                                            pointEntries);
        } catch (Exception exc) {
            logError("Error processing job", exc);

            return getRepository().makeErrorResult(request,
                    "Error processing job: " + exc);
        }
    }

    /**
     * This does the real work of running the processing job
     *
     * @param request the request
     * @param entry the entry (e.g., the collection)
     * @param outputType the output type
     * @param recordEntries list of recordentries to processes
     *
     * @return the ramadda result
     *
     * @throws Exception on badness
     */
    private Result handleAsynchRequestInner(
            final Request request, final Entry entry,
            final OutputType outputType,
            final List<? extends RecordEntry> recordEntries)
            throws Exception {

        String formUrl =
            request.getUrl((HashSet<String>) Utils.makeHashSet(ARG_OUTPUT),
                           null) + "&" + ARG_OUTPUT + "=points.form";
        final JobInfo jobInfo = new JobInfo(request, entry,
                                            getRepository().getGUID(),
                                            "Point Processing");
        jobInfo.setReturnUrl(formUrl);
        jobInfo.setType(JOB_TYPE_POINT);
        jobInfo.setJobStatusUrl(getJobUrl(request, entry, jobInfo.getJobId(),
                                          getOutputResults()));
        jobInfo.setJobUrl(makeJobUrl((Request) request.cloneMe()));

        for (RecordEntry recordEntry : recordEntries) {
            recordEntry.setProcessId(jobInfo.getJobId());
        }
        jobHasStarted(jobInfo);

        Runnable runnable = new Runnable() {
            public void run() {
                try {
                    //Note - normally the POH here is "this" POH but for Lidar types over in the nlasplugin
                    //We want to get the LidarOutputHandler
                    PointOutputHandler pointOutputHandler =
                        (PointOutputHandler) recordEntries.get(
                            0).getOutputHandler();
                    pointOutputHandler.processEntries(request, entry, true,
                            recordEntries, jobInfo.getJobId());
                    if ( !jobOK(jobInfo.getJobId())) {
                        return;
                    }
                    asynchRequestFinished(request, entry, recordEntries,
                                          outputType, jobInfo.getJobId());
                } catch (Throwable exc) {
                    Throwable thr = LogUtil.getInnerException(exc);
                    System.err.println("Error handling async request:" + thr);
                    exc.printStackTrace();
                    if (thr != exc) {
                        thr.printStackTrace();
                    }
                    setError(jobInfo,
                             "Error:" + thr + "\nStack trace outer:<pre>"
                             + LogUtil.getStackTrace(exc)
                             + "\nInner exception:"
                             + LogUtil.getStackTrace(thr) + "</pre>");
                    removeJob(jobInfo);
                    logException("Error processing request", exc);
                    try {
                        IOUtil.writeFile(IOUtil
                            .joinDir(getRecordOutputHandler()
                                .getProductDir(jobInfo
                                    .getJobId()), ".error"), "Error processing equest:"
                                        + thr);
                    } catch (Exception ignore) {}
                }

            }
        };

        Misc.run(runnable);
        //        getExecutor().submit(runnable);
        if (request.responseAsXml()) {
            StringBuffer xml = new StringBuffer();
            String statusUrl =
                request.entryUrl(getRepository().URL_ENTRY_SHOW, entry,
                                 new String[] {
                ARG_OUTPUT, getOutputResults().getId(), ARG_JOB_ID,
                jobInfo.getJobId().toString(), ARG_RESPONSE, RESPONSE_XML,
            });

            String cancelUrl =
                request.entryUrl(getRepository().URL_ENTRY_SHOW, entry,
                                 new String[] {
                ARG_OUTPUT, getOutputResults().getId(), ARG_JOB_ID,
                jobInfo.getJobId().toString(), ARG_RESPONSE, RESPONSE_XML,
                ARG_CANCEL, "true"
            });

            xml.append(XmlUtil.openTag(TAG_URL,
                                       XmlUtil.attrs(new String[] {
                                           JobManager.ATTR_TYPE,
                                           TYPE_STATUS })));
            XmlUtil.appendCdata(xml, request.getAbsoluteUrl(statusUrl));
            xml.append(XmlUtil.closeTag(TAG_URL));

            xml.append(XmlUtil.openTag(TAG_URL,
                                       XmlUtil.attrs(new String[] {
                                           JobManager.ATTR_TYPE,
                                           TYPE_CANCEL })));
            XmlUtil.appendCdata(xml, request.getAbsoluteUrl(cancelUrl));
            xml.append(XmlUtil.closeTag(TAG_URL));

            return getRepository().makeOkResult(request, xml.toString());

        }
        String actionUrl = request.entryUrl(getRepository().URL_ENTRY_SHOW,
                                            entry, new String[] { ARG_OUTPUT,
                getOutputResults().getId(), ARG_JOB_ID,
                jobInfo.getJobId().toString() });

        return new Result(actionUrl);
    }

}
