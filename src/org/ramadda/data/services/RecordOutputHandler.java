/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.data.services;


import org.ramadda.data.record.RecordField;
import org.ramadda.data.record.RecordFile;
import org.ramadda.data.record.RecordIO;
import org.ramadda.data.record.VisitInfo;
import org.ramadda.data.record.filter.CollectionRecordFilter;
import org.ramadda.data.record.filter.RecordFilter;
import org.ramadda.repository.Entry;
import org.ramadda.repository.Link;
import org.ramadda.repository.Repository;
import org.ramadda.repository.Request;
import org.ramadda.repository.Result;
import org.ramadda.repository.ServiceInfo;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.auth.AccessException;
import org.ramadda.repository.job.JobInfo;
import org.ramadda.repository.output.OutputHandler;
import org.ramadda.repository.output.OutputType;
import org.ramadda.util.Utils;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.TempDir;

import org.w3c.dom.Element;

import ucar.unidata.util.IOUtil;
import ucar.unidata.xml.XmlUtil;


import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;



/**
 */
@SuppressWarnings("unchecked")
public class RecordOutputHandler extends OutputHandler implements RecordConstants {

    /** output type */
    public OutputType OUTPUT_RESULTS;

    /** _more_ */
    public static final String ARG_SKIP = "skip";

    public static final String ARG_STRIDE = "stride";
    public static final String ARG_LIMIT = "limit";

    /** _more_ */
    public static final String ARG_PARAMETER = "parameter";

    /** _more_ */
    public static final String ARG_DIVISOR = "divisor";

    /** _more_ */
    public static final String ARG_FIELD_USE = "field_use";

    /** _more_ */
    public static final String SESSION_PREFIX = "record.";

    /** _more_ */
    public static final String PROP_TTL = "record.files.ttl";

    /** Max number of points an anonymous user is allowed to access */
    public static final long POINT_LIMIT_ANONYMOUS = 200000000;

    /** Max number of points a non-anonymous user is allowed to access */
    public static final long POINT_LIMIT_USER = POINT_LIMIT_ANONYMOUS * 5;

    /** Where products get put */
    private TempDir productDir;


    /** This is a static so we can correctly handle mulitple output handlers (e.g., lidar) */
    private static RecordJobManager jobManager;


    /** _more_ */
    private RecordFormHandler formHandler;


    /**
     * constructor. This gets called by the Repository via reflection
     * This class is specified in outputhandlers.xml
     *
     *
     * @param repository the repository
     * @param element the xml from outputhandlers.xml
     * @throws Exception on badness
     */
    public RecordOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        getProductDir();
    }


    /**
     * _more_
     *
     * @param jobManager _more_
     */
    protected void setRecordJobManager(RecordJobManager jobManager) {
        if (this.jobManager == null) {
            this.jobManager = jobManager;
        }
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     */
    public RecordEntry doMakeEntry(Request request, Entry entry) {
        return new RecordEntry(this, request, entry);
    }


    /**
     * This makes  a list of RecordEntry which is a wrapper around Entry.
     *
     * @param request the request
     * @param entries The entries to process
     * @param checkIfSelected If true then check if the request has a ARG_RECORDENTRY is for the given Entry
     *
     * @return List of RecordEntry
     */
    public List<RecordEntry> makeRecordEntries(Request request,
            List<Entry> entries, boolean checkIfSelected) {
        List<RecordEntry> recordEntries = new ArrayList<RecordEntry>();
        boolean           hasAnyArgs = request.exists(ARG_RECORDENTRY_CHECK);
        List<String> ids = request.get(ARG_RECORDENTRY,
                                       new ArrayList<String>());

        for (Entry entry : entries) {
            if ( !isEntryOk(entry)) {
                continue;
            }
            if (checkIfSelected && hasAnyArgs
                    && !ids.contains(entry.getId())) {
                continue;
            }
            if (entry.getTypeHandler() instanceof RecordTypeHandler) {
                recordEntries.add(doMakeEntry(request, entry));
            } else {
                //                System.err.println("not type:" + entry);
            }
        }

        return recordEntries;
    }


    /**
     * Is the given entry a point type or a point collection type
     *
     * @param entry The entry
     *
     * @return Is the entry some point type
     */

    public boolean isEntryOk(Entry entry) {
        System.err.println("ROH.canhandle");

        return false;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param recordEntries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<RecordEntry> doSubsetEntries(
            Request request, List<? extends RecordEntry> recordEntries)
            throws Exception {

        List<RecordEntry> result = new ArrayList<RecordEntry>();
        for (RecordEntry recordEntry : recordEntries) {
            RecordTypeHandler recordType =
                (RecordTypeHandler) recordEntry.getEntry().getTypeHandler();
            if (recordType.includedInRequest(request, recordEntry)) {
                result.add(recordEntry);
            }
        }

        return result;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param recordFile _more_
     * @param filters _more_
     *
     * @throws Exception _more_
     */
    public void getFilters(Request request, Entry entry,
                           RecordFile recordFile, List<RecordFilter> filters)
            throws Exception {
        RecordTypeHandler typeHandler =
            (RecordTypeHandler) entry.getTypeHandler();
        typeHandler.getFilters(request, entry, recordFile, filters);
    }

    /**
     */
    public void shutdown() {
        //        super.shutdown();
        if (jobManager != null) {
            jobManager.shutdown();
        }

    }







    /**
     * _more_
     *
     * @return _more_
     */
    public String getProductDirName() {
        return getDomainBase() + "_products";
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getDomainBase() {
        return "record";
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getProductDirTTLHours() {
        return getRepository().getProperty(PROP_TTL, 7) * 24;
    }


    /**
     * Get the job manager
     *
     * @return the job manager
     */
    public RecordJobManager getRecordJobManager() {
        return jobManager;
    }

    /**
     * Set the FormHandler property.
     *
     * @param value The new value for FormHandler
     */
    public void setFormHandler(RecordFormHandler value) {
        formHandler = value;
    }

    /**
     * Get the FormHandler property.
     *
     * @return The FormHandler
     */
    public RecordFormHandler getFormHandler() {
        if (formHandler == null) {
            formHandler = new RecordFormHandler(this);
        }

        return formHandler;
    }




    /**
     * Wrapper around JobManager.jobOK
     *
     * @param jobId processing job id
     *
     * @return is job running and ok
     */
    public boolean jobOK(Object jobId) {
        if ((jobId == null) || (jobManager == null)) {
            return true;
        }

        return jobManager.jobOK(jobId);
    }




    /**
     * This gets called to add links into the entry menus in the HTML views.
     *
     * @param request the request
     * @param state This holds the group, entry, children, etc.
     * @param links list to add to
     *
     *
     * @throws Exception on badness
     */
    public void getEntryLinks(Request request, State state, List<Link> links)
            throws Exception {
        /*

        Entry entry = state.getEntry();
        if (entry == null) {
            return;
        }
        if ( !isEntryOk(entry)) {
            return;
        }

        if (entry.getTypeHandler() instanceof PointCollectionTypeHandler) {
            links.add(makeLink(request, state.getEntry(), OUTPUT_FORM));
            return;
        }

        if ( !state.entry.isFile()) {
            return;
        }


        if ( !getRepository().getAccessManager().canAccessFile(request,
                state.entry)) {
            return;
        }


        links.add(makeLink(request, state.getEntry(), OUTPUT_CHART));
        links.add(makeLink(request, state.getEntry(), OUTPUT_FORM));
        links.add(makeLink(request, state.getEntry(), OUTPUT_VIEW));
        links.add(makeLink(request, state.getEntry(), OUTPUT_METADATA));

        //Don't add these for now but these are the direct URLS to the data products
        if (false) {
            String path = "/"
                          + IOUtil.stripExtension(state.getEntry().getName());
            links.add(makeLink(request, state.getEntry(), OUTPUT_KMZ,
                               path + ".kmz"));

            links.add(makeLink(request, state.getEntry(), OUTPUT_IMAGE,
                               path + ".png"));
            links.add(makeLink(request, state.getEntry(), OUTPUT_HILLSHADE,
                               path + ".png"));


            links.add(makeLink(request, state.getEntry(), OUTPUT_CSV,
                               path + ".csv"));

            //Lets not have the 3d points  for now
            // if (hasWaveform(state.entry)) {
            if (false) {
                links.add(makeLink(request, state.getEntry(),
                                   OUTPUT_LATLONALT3DCSV, path + ".csv"));
            } else {
                links.add(makeLink(request, state.getEntry(),
                                   OUTPUT_LATLONALTCSV, path + ".csv"));

                links.add(makeLink(request, state.getEntry(),
                                   OUTPUT_LATLONALTBIN, path + ".llab"));
            }
            links.add(makeLink(request, state.getEntry(), OUTPUT_LAS,
                               path + ".las"));
            links.add(makeLink(request, state.getEntry(), OUTPUT_ASC,
                               path + ".asc"));
            //            links.add(makeLink(request, state.getEntry(), OUTPUT_NC,
            //                               path + ".nc"));
        }
        //TODO: What to do with waveforms?
        //            if(hasWaveform(state.entry)) {
        //                links.add(makeLink(request, state.getEntry(), OUTPUT_WAVEFORM));
        */
    }





    /**
     *
     * @param request the request
     * @param outputType The type of output
     * @param entry The entry
     *
     * @return the result
     *
     * @throws Exception on badness
     */
    public Result outputEntry(Request request, OutputType outputType,
                              final Entry entry)
            throws Exception {

        if (jobManager == null) {
            return null;
        }

        //Route any of the processing job requests to the JobManager
        if (request.defined(JobInfo.ARG_JOB_ID)) {
            return jobManager.handleJobStatusRequest(request, entry);
        }

        //Check for access to the file
        if ( !getRepository().getAccessManager().canAccessFile(request,
                entry)) {
            StringBuilder json = new StringBuilder();
	    JsonUtil.map(json,Utils.makeListFromValues(
					     "error", JsonUtil.quote("Unauthorized access to file")));
            Result result = new Result("", json, JsonUtil.MIMETYPE);
            result.setResponseCode(Result.RESPONSE_UNAUTHORIZED);

            return result;
            //            throw new AccessException("Cannot access data", request);
        }

        return null;

    }


    /**
     * @param request the request
     * @param outputType output type
     * @param group The group
     *
     * @return The result
     *
     * @throws Exception on badness
     */
    @Override
    public Result outputGroup(final Request request,
                              final OutputType outputType, final Entry group,
                              final List<Entry> children)
            throws Exception {


        if (request.defined(JobInfo.ARG_JOB_ID)) {
            return jobManager.handleJobStatusRequest(request, group);
        }

        return null;
    }



    /** _more_ */
    private int callCnt = 0;



    /**
     * _more_
     *
     * @param msg _more_
     */
    public void memoryCheck(String msg) {
        //        Runtime.getRuntime().gc();
        //        getLogManager().logInfoAndPrint(msg + ((int)(Misc.usedMemory()/1000000.0))+"MB");
    }


    /**
     * _more_
     *
     * @param request _more_
     */
    public void storeSession(Request request) {
        request.putSessionIfDefined(ARG_AREA_NORTH, SESSION_PREFIX);
        request.putSessionIfDefined(ARG_AREA_WEST, SESSION_PREFIX);
        request.putSessionIfDefined(ARG_AREA_SOUTH, SESSION_PREFIX);
        request.putSessionIfDefined(ARG_AREA_EAST, SESSION_PREFIX);
    }


    /**
     * This gets the selected product formats.
     *
     * @param request The request
     *
     * @return set of formats
     */
    public HashSet<String> getProductFormats(Request request) {
        HashSet<String> formats = new HashSet<String>();
        for (String format :
                (List<String>) request.get(ARG_PRODUCT,
                                           new ArrayList<String>())) {
            formats.add(format);
        }

        return formats;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param fields _more_
     *
     * @return _more_
     */
    public List<RecordField> getFields(Request request,
                                       List<RecordField> fields) {
        HashSet<String> selectedFields = new HashSet<String>();
        selectedFields.addAll((List<String>) request.get(ARG_FIELD_USE,
                new ArrayList<String>()));
        List<RecordField> fieldsToUse = new ArrayList<RecordField>();
        for (RecordField field : fields) {
            if (selectedFields.size() > 0) {
                if (selectedFields.contains(field.getName())) {
                    fieldsToUse.add(field);
                }
            } else {
                fieldsToUse.add(field);
            }
        }
        if (fieldsToUse.size() == 0) {
            //TODO: Do we default to all fields if none are selected
            return fields;
        } else {
            return fieldsToUse;
        }
    }



    /**
     * Create a record filter from the url args. This can make a spatial bounds filter,
     * a probabilistic filter, and a value range filter.
     *
     * @param request the request
     * @param entry The entry
     * @param recordFile _more_
     *
     * @return The record filter.
     *
     * @throws Exception _more_
     */
    public RecordFilter getFilter(Request request, Entry entry,
                                  RecordFile recordFile)
            throws Exception {
        List<RecordFilter> filters = new ArrayList<RecordFilter>();
        getFilters(request, entry, recordFile, filters);
        if (filters.size() == 0) {
            return null;
        }
        if (filters.size() == 1) {
            return filters.get(0);
        }

        return new CollectionRecordFilter(filters);
    }




    /**
     * Make if needed and return the directory to store products to for the given job id
     *
     * @param jobId The job ID
     *
     * @return product dir
     *
     * @throws Exception On badness
     */
    public File getProductDir(Object jobId) throws Exception {
        File theProductDir = new File(IOUtil.joinDir(getProductDir(),
                                 jobId.toString()));
        IOUtil.makeDir(theProductDir);

        return theProductDir;
    }


    /**
     * _more_
     *
     * @param entry _more_
     * @param ext _more_
     *
     * @return _more_
     */
    public String getOutputFilename(Entry entry, String ext) {
        return IOUtil.stripExtension(entry.getName()) + ext;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputEntryBounds(Request request, Entry entry)
            throws Exception {
        return new Result(entry.getNorth() + "," + entry.getWest() + ","
                          + entry.getSouth() + "," + entry.getEast(), "text");
    }




    /**
     * _more_
     *
     * @param request The request
     * @param jobId The job ID
     * @param entry _more_
     * @param ext _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public OutputStream getOutputStream(Request request, Object jobId,
                                        Entry entry, String ext)
            throws Exception {
        if (entry == null) {
            System.err.println("BAD ENTRY");
        }
        String fileName = getOutputFilename(entry, ext);
        if (jobId == null) {
            return request.getOutputStream();
        }
        File file = new File(IOUtil.joinDir(getProductDir(jobId), fileName));
        return new BufferedOutputStream(
            getStorageManager().getUncheckedFileOutputStream(file), 100000);
    }


    /**
     * _more_
     *
     * @param request The request
     * @param jobId The job ID
     * @param entry _more_
     * @param ext _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    static boolean buffered = true;

    /**
     *
     * @param request _more_
     * @param jobId _more_
     * @param entry _more_
     * @param ext _more_
      * @return _more_
     *
     * @throws Exception _more_
     */
    public PrintWriter getPrintWriter(Request request, Object jobId,
                                      Entry entry, String ext)
            throws Exception {
        buffered = !buffered;
	/*        if(buffered) {
            System.err.println("Using buffered");
            return new PrintWriter(new BufferedWriter(new OutputStreamWriter(getOutputStream(request, jobId, entry, ext))));
	    }
        System.err.println("Using unbuffered");
	*/
        return new PrintWriter(getOutputStream(request, jobId, entry, ext));
    }

    /**
     * _more_
     *
     * @param request The request
     * @param jobId The job ID
     * @param entry _more_
     * @param ext _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public DataOutputStream getDataOutputStream(Request request,
            Object jobId, Entry entry, String ext)
            throws Exception {
        return new DataOutputStream(getOutputStream(request, jobId, entry,
                ext));
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public Result getDummyResult() {
        Result result = new Result();
        result.setMimeType("dummy");
        result.setNeedToWrite(false);

        return result;
    }


    /**
     * _more_
     *
     * @param request the request
     * @param dflt _more_
     * @param arg _more_
     *
     * @return _more_
     */
    public int getSkip(Request request, int dflt, String arg) {
        String skip = request.getString(arg, "");
        if (skip.equals("${skip}")) {
            return dflt;
        }

        return request.get(arg, dflt);
    }

    /**
     * This subsets the point file. The return format is the same format as the input file
     *
     * @param request the request
     * @param mainEntry Either the Point Collection or File Entry
     * @param recordEntries _more_
     * @param jobInfo processing job
     *
     * @return result
     *
     * @throws Exception on badness
     */
    public Result outputEntrySubset(
            Request request, Entry mainEntry,
            List<? extends RecordEntry> recordEntries, JobInfo jobInfo)
            throws Exception {
        RecordEntry recordEntry = recordEntries.get(0);
        String ext = IOUtil.getFileExtension(
					     recordEntry.getRecordFile().getPath().getPath());
        VisitInfo visitInfo = recordEntry.getRecordFile().doMakeVisitInfo();
        OutputStream outputStream = getOutputStream(request,
                                        jobInfo.getJobId(), mainEntry, ext);
        RecordIO recordOutput = new RecordIO(outputStream);
        recordEntry.getRecordFile().write(recordOutput, visitInfo,
                                          recordEntry.getFilter(), true);
        for (int i = 1; i < recordEntries.size(); i++) {
            recordEntry = recordEntries.get(i);
            recordEntry.getRecordFile().write(recordOutput, visitInfo,
                    recordEntry.getFilter(), false);
        }
        recordOutput.close();
        jobInfo.setNumPoints(visitInfo.getCount());

        return getDummyResult();
    }

    /**
     *
     *
     * @param request _more_
     * @param entry The entry
     *
     * @return A new PointFile.
     *
     * @throws Exception on badness
     */
    public RecordFile doMakeRecordFile(Request request, Entry entry)
            throws Exception {
        RecordTypeHandler type = (RecordTypeHandler) entry.getTypeHandler();

        RecordFile file = (RecordFile) type.doMakeRecordFile(request, entry);
        if (file == null) {
            return null;
        }
        file.putProperty("entry", entry);
	List<Metadata> metadataList =
	    getMetadataManager().findMetadata(getRepository().getTmpRequest(), entry,
					      new String[]{"pointdata_units"}, true);
	if ((metadataList != null) && (metadataList.size() > 0)) {
	    StringBuilder sb = new StringBuilder();
	    for(Metadata mtd: metadataList) {
		sb.append(mtd.getAttr1());
		sb.append("\n");
	    }
	    List<String[]> units = new ArrayList<String[]>();
	    for(String line: Utils.split(sb.toString(),"\n",true,true)) {
		List<String> toks = Utils.splitUpTo(line,"=",2);
		if(toks.size()==2) units.add(new String[]{toks.get(0),toks.get(1)});
	    }
	    file.setUnitPatterns(units);
	}

        return file;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param services _more_
     */
    public void getServiceInfos(Request request, Entry entry,
                                List<ServiceInfo> services) {}


    /**
     * _more_
     *
     * @param request _more_
     * @param icon _more_
     *
     * @return _more_
     */
    public String getAbsoluteIconUrl(Request request, String icon) {
        return request.getAbsoluteUrl(getRepository().getIconUrl(icon));
    }



}
