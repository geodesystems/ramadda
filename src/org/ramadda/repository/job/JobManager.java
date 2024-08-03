/**
   Copyright (c) 2008-2024 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.job;


import org.ramadda.data.record.*;
import org.ramadda.data.record.filter.*;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;

import org.ramadda.repository.job.*;
import org.ramadda.repository.map.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.TypeHandler;


import org.ramadda.service.Service;
import org.ramadda.service.ServiceTypeHandler;
import org.ramadda.util.CategoryBuffer;


import org.ramadda.util.HtmlUtils;
import org.ramadda.util.ProcessRunner;
import org.ramadda.util.StreamEater;
import org.ramadda.util.TTLCache;
import org.ramadda.util.Utils;

import org.ramadda.util.sql.Clause;
import org.ramadda.util.sql.SqlUtil;


import org.w3c.dom.*;

import ucar.unidata.util.Misc;
import ucar.unidata.util.Trace;
import ucar.unidata.xml.XmlUtil;



import java.io.*;

import java.sql.ResultSet;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;

import java.util.Hashtable;
import java.util.List;




import java.util.Map;
import java.util.concurrent.*;

import java.util.zip.*;




public class JobManager extends RepositoryManager {
    public final RequestUrl URL_SERVICES_LIST = new RequestUrl(this,  "/services/list");
    public final RequestUrl URL_SERVICES_VIEW = new RequestUrl(this,   "/services/view");
    protected boolean debug = false;
    private int jobLimit;
    private long myTime = System.currentTimeMillis();
    private boolean running = true;
    public static final String TAG_JOB = "job";
    public static final String TAG_URL = "url";
    public static final String TAG_PRODUCTS = "products";
    public static final String ATTR_STATUS = "status";
    public static final String ATTR_NUMBEROFPOINTS = "numberofpoints";
    public static final String ATTR_ELAPSEDTIME = "elapsedtime";
    public static final String ATTR_TYPE = "type";
    public static final String TYPE_STATUS = "status";
    public static final String TYPE_CANCEL = "cancel";
    public static final String STATUS_RUNNING = "running";
    public static final String STATUS_DONE = "done";
    public static final String STATUS_CANCELLED = "cancelled";

    /** Property name for the max number of threads to use */
    public static final String PROP_NUMTHREADS = "job.numberofthreads";

    /** The singleton thread pool */
    private ExecutorService executor;

    private Hashtable<String, Service> serviceMap = new Hashtable<String,Service>();
    private List<Service> services = new ArrayList<Service>();
    private Object MUTEX = new Object();
    protected int totalJobs = 0;
    protected int currentJobs = 0;
    private long callCnt=0;
    private TTLCache<Object, JobInfo> jobCache = new TTLCache<Object,
	JobInfo>(60 * 24 * 60
		 * 1000);

    private Hashtable<Object, JobInfo> runningJobs = new Hashtable<Object,
	JobInfo>();

    private int numThreads;

    public JobManager(Repository repository) {
        super(repository);
	jobLimit = repository.getProperty("ramadda.job.limit",100);
	numThreads = Math.max(1, getRepository().getProperty(PROP_NUMTHREADS,  6));
    }

    public JobManager(Repository repository,int numThreads) {
        super(repository);
	this.numThreads = numThreads;
	jobLimit = repository.getProperty("ramadda.job.limit",100);
    }    


    public void setDebug(boolean v) {
	debug =v;
    }

    
    public int getNumberOfJobs() {
        return currentJobs;
    }

    
    public void shutdown() {
        running = false;
        if (executor != null) {
            System.err.println("RAMADDA: Shutting down the executor");
            executor.shutdownNow();
            executor    = null;
            currentJobs = 0;
        }
    }


    
    public List<Service> getServices() {
        return services;
    }

    
    public Service getService(String id) throws Exception {
        Service service = serviceMap.get(id);
        if (service != null) {
            return service;
        }
        if ( !getAdmin().getInstallationComplete()) {
            return null;
        }
        Request request      = getRepository().getTmpRequest();
        Entry   serviceEntry = getEntryManager().getEntry(request, id);
        if ((serviceEntry != null)
	    && (serviceEntry.getTypeHandler()
		instanceof ServiceTypeHandler)) {
            ServiceTypeHandler serviceType =
                (ServiceTypeHandler) serviceEntry.getTypeHandler();

            return serviceType.getService(request, serviceEntry);
        }

        return null;

    }


    
    public Service addService(Service service) {
        Service existingService = serviceMap.get(service.getId());
        if (existingService != null) {
            return existingService;
        }
        //        System.err.println ("JobManager.addService:"+ service.getId());
        serviceMap.put(service.getId(), service);
        services.add(service);

        return service;
    }


    
    public ExecutorService getExecutor() {
        if (executor == null) {
            synchronized (MUTEX) {
                if (executor != null) {
                    return executor;
                }
                int numThreads =getNumThreads();
                //Runtime.getRuntime().availableProcessors() / 2));

                System.err.println(
				   "RAMADDA JobManager: #threads: " + numThreads
				   + " available cores:"
				   + Runtime.getRuntime().availableProcessors());
                executor = Executors.newFixedThreadPool(numThreads);
            }
        }

        return executor;
    }


    public int getNumThreads() {
	return 	numThreads;
    }



    
    public JobInfo doMakeJobInfo(Object jobId) throws Exception {
        Statement stmt =
            getDatabaseManager().select(JobInfo.DB_COL_JOB_INFO_BLOB,
                                        JobInfo.DB_TABLE,
                                        Clause.eq(JobInfo.DB_COL_ID, jobId));

        String[] values =
            SqlUtil.readString(getDatabaseManager().getIterator(stmt), 1);
        if ((values == null) || (values.length == 0)) {
            return null;
        }

        return makeJobInfo(values[0]);
    }


    
    private JobInfo makeJobInfo(String blob) throws Exception {
        blob = blob.replaceAll("org.unavco.projects.nlas.ramadda.JobInfo",
                               "org.ramadda.repository.job.JobInfo");
        JobInfo jobInfo = (JobInfo) getRepository().decodeObject(blob);
        if (jobInfo != null) {
            jobInfo.setTheEntry(
				getEntryManager().getEntry(
							   getRepository().getTmpRequest(), jobInfo.getEntryId()));
        }

        return jobInfo;
    }


    
    public List<JobInfo> readJobs(String type) throws Exception {
        return readJobs(Clause.eq(JobInfo.DB_COL_TYPE, type));
    }


    
    public List<JobInfo> readJobs(Clause clause) throws Exception {
        List<JobInfo> jobInfos = new ArrayList<JobInfo>();
        Statement stmt =
            getDatabaseManager().select(JobInfo.DB_COL_JOB_INFO_BLOB,
                                        JobInfo.DB_TABLE, clause);
        SqlUtil.Iterator iter = getDatabaseManager().getIterator(stmt);
        ResultSet        results;
        while ((results = iter.getNext()) != null) {
            jobInfos.add(makeJobInfo(results.getString(1)));
        }

        return jobInfos;

    }


    /**
     * save the job info to the database. This either writes the job to the db if its not there or overwrites the row if it is there
     *
     * @param jobInfo job info to write
     */
    public void writeJobInfo(JobInfo jobInfo) {
        writeJobInfo(jobInfo, false);
    }

    /**
     * write job info to db
     *
     * @param jobInfo job info
     * @param newOne is this a new job
     */
    public void writeJobInfo(JobInfo jobInfo, boolean newOne) {
        try {
            jobCache.put(jobInfo.getJobId(), jobInfo);
            String blob =
                getRepository().getRepository().encodeObject(jobInfo);
            if (newOne) {
                String insert = SqlUtil.makeInsert(JobInfo.DB_TABLE,
						   JobInfo.DB_COLUMNS);
                getDatabaseManager().executeInsert(insert, new Object[] {
			jobInfo.getJobId(), jobInfo.getEntryId(), new Date(),
			jobInfo.getUser(), jobInfo.getType(), blob
		    });
            } else {
                getDatabaseManager().update(
					    JobInfo.DB_TABLE, JobInfo.DB_COL_ID,
					    jobInfo.getJobId().toString(),
					    new String[] { JobInfo.DB_COL_JOB_INFO_BLOB },
					    new Object[] { blob });
            }
        } catch (Exception exc) {
            throw new RuntimeException(exc);

        }
    }


    /**
     * find the jobinfo for the given job. This looks in the runningJobs and if its not there looks at the database
     *
     * @param jobId The job ID
     *
     * @return job id
     */
    public JobInfo getJobInfo(Object jobId) {
        try {
            JobInfo jobInfo = jobCache.get(jobId);
            if (jobInfo == null) {
                jobInfo = doMakeJobInfo(jobId);
                jobCache.put(jobInfo.getJobId(), jobInfo);

                return jobInfo;
            }

            return jobInfo;
        } catch (Exception exc) {
            logError("RAMADDA: Could not read processing job: " + jobId, exc);
        }

        return null;
    }



    
    public void setError(JobInfo jobInfo, String error) {
        jobInfo.setError(error);
        writeJobInfo(jobInfo);
    }


    /**
     * Is the job still running
     *
     * @param jobId The job ID
     *
     * @return is the job OK
     */
    public boolean jobOK(Object jobId) {
        if (jobId == null) {
            return true;
        }

        return runningJobs.get(jobId) != null;
    }

    /**
     * _more_
     *
     * @param jobInfo _more_
     */
    public void jobHasStarted(JobInfo jobInfo) {
        runningJobs.put(jobInfo.getJobId(), jobInfo);
        writeJobInfo(jobInfo, true);
    }


    /**
     * _more_
     *
     * @param jobInfo _more_
     */
    public void jobHasFinished(JobInfo jobInfo) {
        removeJob(jobInfo);
        jobInfo.setStatus(jobInfo.STATUS_DONE);
        jobInfo.setEndDate(new Date());
        writeJobInfo(jobInfo);
    }


    /**
     * _more_
     *
     * @param jobInfo _more_
     */
    public void jobWasCancelled(JobInfo jobInfo) {
        removeJob(jobInfo);
        jobInfo.setStatus(jobInfo.STATUS_CANCELLED);
        writeJobInfo(jobInfo);
    }

    /**
     * _more_
     *
     * @param jobInfo _more_
     */
    public void removeJob(JobInfo jobInfo) {
        runningJobs.remove(jobInfo.getJobId());
    }


    /**
     * utility to execute the list of callable objects
     *
     *
     * @param request _more_
     * @param callable callable object
     *
     * @throws Exception On badness
     *
     * @throws Throwable _more_
     */
    public void invokeAndWait(Callable<Boolean> callable)
	throws Throwable {
        List<Callable<Boolean>> callables =
            new ArrayList<Callable<Boolean>>();
        callables.add(callable);
        invokeAndWait(callables);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return "# threads:" + getNumThreads() +" current jobs:" + currentJobs + " completed jobs:" + totalJobs;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    private boolean canAcceptJob() {
        return currentJobs <= jobLimit;
    }

    /**
     * If more than jobLimit # of jobs is running then this pauses at most 10 seconds
     * Then it throws an error if still can't run the job
     */
    public void checkNewJobOK() {
	int tries = 1000;
	while(!canAcceptJob() && tries-->0) {
	    Misc.sleep(10);
	}

	if(tries<=0) {
            throw new IllegalStateException("JobManager: Too many outstanding processing jobs");
        }
    }


    /**
     * execute the list of callables in the executor thread pool
     *
     *
     * @param request _more_
     * @param callables callables to execute
     *
     * @throws Exception On badness
     *
     * @throws Throwable _more_
     */
    public void invokeAndWait(List<Callable<Boolean>> callables)
	throws Throwable {
        checkNewJobOK();
        try {
            synchronized (MUTEX) {
		callCnt++;
                currentJobs++;
            }
	    
	    //If only 1 job then run in this thread
	    /*	    if(false && callables.size()==1) {
		    callables.get(0).call();
		    } else {*/
		if(debug) System.err.println("JobManager: calling " + this);
		List<Future<Boolean>> results =	getExecutor().invokeAll(callables);
		for (Future future : results) {
		    try {
			future.get();
		    } catch (ExecutionException ex) {
			throw ex.getCause();
		    }
		}
		if(debug) System.err.println("JobManager: done calling");
		/*}*/
	} finally {
	    synchronized (MUTEX) {
                currentJobs--;
                totalJobs++;
            }
	    //	    if((callCnt%100)==0)System.err.println("JobManager:" + (callCnt++) +" " + currentJobs);
        }
    }


    /**
     *
     * @param callables _more_
     *
     * @throws Throwable _more_
     */
    public void invokeAllAndWait(List<Callable<Boolean>> callables)
	throws Throwable {
        ExecutorService executor =
            Executors.newFixedThreadPool(callables.size());
        List<Future<Boolean>> results = executor.invokeAll(callables);
        for (Future future : results) {
            try {
                future.get();
            } catch (ExecutionException ex) {
                throw ex.getCause();
            }
        }
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
    public Result handleJobStatusRequest(Request request, Entry entry)
	throws Exception {
        String jobId = request.getString(JobInfo.ARG_JOB_ID, (String) null);
        StringBuffer sb      = new StringBuffer();
        StringBuffer xml     = new StringBuffer();
        JobInfo      jobInfo = getJobInfo(jobId);
        if (jobInfo == null) {
            return makeRequestErrorResult(request, null,
                                          "No job found with id = " + jobId);
        }

        if (jobInfo.isCancelled()) {
            if (request.responseAsXml()) {
                xml.append(XmlUtil.tag(TAG_JOB,
                                       XmlUtil.attrs(new String[] {
					       JobManager.ATTR_STATUS,
					       STATUS_CANCELLED })));

                return makeRequestOKResult(request, jobInfo, xml.toString());
            }

            return makeRequestErrorResult(request, jobInfo,
                                          "The job has been cancelled.");
        }

        if (jobInfo.isInError() && request.responseAsXml()) {
            return makeRequestErrorResult(request, jobInfo,
                                          "An error has occurred:"
                                          + jobInfo.getError());
        }


        if (request.get(ARG_CANCEL, false)) {
            runningJobs.remove(jobId);
            jobInfo.setStatus(jobInfo.STATUS_CANCELLED);
            writeJobInfo(jobInfo);

            return makeRequestOKResult(request, jobInfo,
                                       "The job has been cancelled.");
        }

        return null;
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processServicesList(Request request) throws Exception {
        StringBuffer sb = new StringBuffer("\n");
        sb.append(HtmlUtils.p());
        sb.append(header(msg("Services")));
        sb.append(HtmlUtils.p());
        sb.append("\n");
        sb.append(HtmlUtils.open(HtmlUtils.TAG_DIV,
                                 HtmlUtils.cssClass("service-list")));
        CategoryBuffer cb      = new CategoryBuffer();
        String         urlBase = getRepository().getUrlBase();
        for (Service service : getServices()) {
            if ( !service.isEnabled()) {
                continue;
            }

            String img = "";
            if (service.getIcon() != null) {
                img = HtmlUtils.img(getIconUrl(service.getIcon()),"",HU.attr(HU.ATTR_WIDTH,ICON_WIDTH));
            } else {
                img = HtmlUtils.img(getIconUrl("/icons/cog.png"),"",HU.attr(HU.ATTR_WIDTH,ICON_WIDTH));
            }
            StringBuffer serviceSB = new StringBuffer();

            serviceSB.append(
			     HtmlUtils.open(
					    HtmlUtils.TAG_DIV,
					    HtmlUtils.cssClass("service-list-service")));
            serviceSB.append(img);
            serviceSB.append(" ");
            serviceSB.append(
			     HtmlUtils.href(
					    HtmlUtils.url(
							  urlBase + "/services/view", ARG_SERVICEID,
							  service.getId()), service.getLabel()));

            /*
	      String xmlUrl = HtmlUtils.href(HtmlUtils.url(
	      urlBase +"/services/view",ARG_SERVICEID, service.getId(),ARG_OUTPUT,"xml"),HtmlUtils.img(getIconUrl(ICON_XML)));

	      serviceSB.append(xmlUrl);
            */
            if (Utils.stringDefined(service.getDescription())) {
                serviceSB.append(HtmlUtils.div(service.getDescription(),
					       HtmlUtils.cssClass("service-list-description")));
            }
            serviceSB.append(HtmlUtils.close(HtmlUtils.TAG_DIV));
            serviceSB.append("\n");
            cb.append(service.getCategory(), serviceSB.toString());
        }


        for (String category : cb.getCategories()) {
            sb.append(
		      HtmlUtils.div(
				    category, HtmlUtils.cssClass("service-list-header")));
            sb.append(
		      HtmlUtils.open(
				     HtmlUtils.TAG_DIV,
				     HtmlUtils.cssClass("service-list-category")));
            sb.append(cb.get(category).toString());
            sb.append(HtmlUtils.close(HtmlUtils.TAG_DIV));
        }

        sb.append(HtmlUtils.close(HtmlUtils.TAG_DIV));

        return new Result(msg("Services"), sb);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param service _more_
     *
     * @return _more_
     */
    public String getServiceUrl(Request request, Service service) {
        return HtmlUtils.url(getRepository().getUrlBase() + "/services/view",
                             ARG_SERVICEID, service.getId());
    }

    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processServicesView(Request request) throws Exception {
        StringBuilder sb = new StringBuilder();
        Service service  = getService(request.getString(ARG_SERVICEID, ""));
        if (service == null) {
            sb.append(getPageHandler().showDialogError("No service found:"
						       + request.getString(ARG_SERVICEID, "")));

            return new Result(msg("Services"), sb);
        }

        /*
	  if(request.getString(ARG_OUTPUT,"").equals("xml")) {
	  service.toXml(sb);
	  request.setReturnFilename(service.getLabel()+"services.xml");
	  return new Result("",sb,"text/xml");
	  }*/


        if ( !service.isEnabled()) {
            sb.append(
		      getPageHandler().showDialogError("Service not enabled"));

            return new Result(msg("Services"), sb);
        }

        ServiceOutputHandler soh = new ServiceOutputHandler(getRepository(),
							    service);
        String extra = HtmlUtils.hidden(ARG_SERVICEID, service.getId());
        if ( !soh.doExecute(request)) {
            getPageHandler().sectionOpen(request, sb, "Services", false);
            soh.makeForm(request, service, null, null, URL_SERVICES_VIEW,
                         null, sb, extra);
            getPageHandler().sectionClose(request, sb);

            return new Result("", sb);
        }

        return soh.evaluateService(request, URL_SERVICES_VIEW, null, null,
                                   null, service, extra);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param jobInfo _more_
     * @param message _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result makeRequestErrorResult(Request request, JobInfo jobInfo,
                                         String message)
	throws Exception {
        if (request.responseAsXml()) {
            //TODO            return makeRequestErrorResult(request, null, message);
        }
        StringBuffer sb = new StringBuffer();
        openHtmlHeader(request, jobInfo, sb);
        sb.append(getPageHandler().showDialogNote(message));
        if (jobInfo.getReturnUrl() != null) {
            sb.append("<p>");
            sb.append(HtmlUtils.div(HtmlUtils.href(jobInfo.getReturnUrl(),
						   "Return to form"), HtmlUtils.cssClass("ramadda-button")));
        }
        closeHtmlHeader(request, jobInfo, sb);

        return new Result("", sb);
    }

    /**
     * This creates the appropriate response for a Point data API request.
     * If its the  API this creates the response  xml. If its the browser
     * then this creates a web page
     *
     * @param request http request
     * @param jobInfo _more_
     * @param message error message
     *
     * @return xml or html result
     *
     * @throws Exception _more_
     */
    public Result makeRequestOKResult(Request request, JobInfo jobInfo,
                                      String message)
	throws Exception {
        if (request.responseAsXml()) {
            return new Result(XmlUtil.tag(TAG_RESPONSE,
                                          XmlUtil.attr(ATTR_CODE, CODE_OK),
                                          message), MIME_XML);

        }
        if (request.responseAsText()) {
            return new Result(message, "text");
        }

        StringBuilder sb = new StringBuilder();
        openHtmlHeader(request, jobInfo, sb);
        sb.append(getPageHandler().showDialogNote(message));
        sb.append("<p>");
        if (jobInfo.getReturnUrl() != null) {
            sb.append(HtmlUtils.div(HtmlUtils.href(jobInfo.getReturnUrl(),
						   "Return to form"), HtmlUtils.cssClass("ramadda-button")));
        }
        closeHtmlHeader(request, jobInfo, sb);

        return new Result("", sb);
    }







    /**
     * _more_
     *
     * @param request _more_
     * @param jobInfo _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void openHtmlHeader(Request request, JobInfo jobInfo,
                               Appendable sb)
	throws Exception {
        if ((jobInfo != null) && (jobInfo.getEntry() != null)) {
            getPageHandler().entrySectionOpen(request, jobInfo.getEntry(),
					      sb, jobInfo.getJobLabel());
        }
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param jobInfo _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void closeHtmlHeader(Request request, JobInfo jobInfo,
                                Appendable sb)
	throws Exception {
        if ((jobInfo != null) && (jobInfo.getEntry() != null)) {
            getPageHandler().entrySectionClose(request, jobInfo.getEntry(),
					       sb);
        }
    }



    /**
     * Excecute a command
     *
     * @param commands     command parameters
     * @param dir   the working directory
     *
     * @return the input and output streams
     *
     * @throws Exception  problem with execution
     */
    public CommandResults executeCommand(List<String> commands, File dir)
	throws Exception {
        return executeCommand(commands, null, dir);
    }

    /**
     * Excecute a command
     *
     * @param commands     command parameters
     * @param envVars      enviroment variables
     * @param workingDir   the working directory
     *
     * @return the input and output streams
     *
     * @throws Exception  problem with execution
     */
    public CommandResults executeCommand(List<String> commands,
                                         Map<String, String> envVars,
                                         File workingDir)
	throws Exception {
        return executeCommand(commands, envVars, workingDir,
                              -1 /* don't timeout*/);
    }

    /**
     * Excecute a command
     *
     * @param commands     command parameters
     * @param envVars      enviroment variables
     * @param workingDir   the working directory
     * @param timeOutInSeconds   number of seconds to allow process to finish
     *                           before killing it. <= 0 to not time out.
     *
     * @return the input and output streams
     *
     * @throws Exception  problem with execution
     */
    public CommandResults executeCommand(List<String> commands,
                                         Map<String, String> envVars,
                                         File workingDir,
                                         int timeOutInSeconds)
	throws Exception {

        return executeCommand(commands, envVars, workingDir,
                              timeOutInSeconds, null, null);
    }

    /**
     * _more_
     *
     * @param commands _more_
     * @param envVars _more_
     * @param workingDir _more_
     * @param timeOutInSeconds _more_
     * @param stdOutPrintWriter _more_
     * @param stdErrPrintWriter _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public CommandResults executeCommand(List<String> commands,
                                         Map<String, String> envVars,
                                         File workingDir,
                                         int timeOutInSeconds,
                                         PrintWriter stdOutPrintWriter,
                                         PrintWriter stdErrPrintWriter,Object...actionID)
	throws Exception {
        //        Trace.startTrace();
        //        Trace.call1("JobManager.executeCommand",  "timeout:" + timeOutInSeconds + " Commands:" + commands);
        StringWriter outBuf   = new StringWriter();
        StringWriter errorBuf = new StringWriter();
        if (stdOutPrintWriter == null) {
            stdOutPrintWriter = new PrintWriter(outBuf);
        }
        if (stdErrPrintWriter == null) {
            stdErrPrintWriter = new PrintWriter(errorBuf);
        }

        ProcessBuilder pb = getRepository().makeProcessBuilder(commands);
        if (envVars != null) {
            Map<String, String> env = pb.environment();
            //env.clear();
            env.putAll(envVars);
        }
        pb.directory(workingDir);

	final boolean[] running = {true};
	final ProcessRunner []runner={null};
	if(actionID.length>0 && actionID[0]!=null) {
	    final Object _actionID = actionID[0];
	    Misc.run(new Runnable() {
		    public void run() {
			while(running[0]) {
			    Misc.sleep(200);
			    if(!running[0]) {
				System.err.println("done");
				break;
			    }
			    if(!getActionManager().getActionOk(_actionID)) {
				try {
				    runner[0].kill();
				} catch(Throwable thr) {
				    break;
				}				    
			    }
			}
			
		    }
		});
	}

	try {
	    runner[0] = new ProcessRunner(pb, timeOutInSeconds,
					  stdOutPrintWriter, stdErrPrintWriter);

	    int exitCode = runner[0].runProcess();
	    if (runner[0].getProcessTimedOut()) {
		throw new InterruptedException("Process timed out");
	    }
	    return new CommandResults(outBuf.toString(), errorBuf.toString(),
				      exitCode);
	} finally {
	    running[0] = false;
	}
    }


    /**
     * _more_
     *
     * @param command _more_
     * @param workingDir _more_
     * @param timeOutInSeconds _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public CommandResults executeCommand(String command, File workingDir,
                                         int timeOutInSeconds)
	throws Exception {
        StringWriter outBuf   = new StringWriter();
        StringWriter errorBuf = new StringWriter();

        List<String> commands = new ArrayList<String>();
        commands.add(command);
        ProcessBuilder pb = getRepository().makeProcessBuilder(commands);
        pb.directory(workingDir);

        ProcessRunner runner = new ProcessRunner(pb, timeOutInSeconds,
						 new PrintWriter(outBuf),
						 new PrintWriter(errorBuf));
        int exitCode = runner.runProcess();
        if (runner.getProcessTimedOut()) {
            throw new InterruptedException("Process timed out");
        }

        return new CommandResults(outBuf.toString(), errorBuf.toString(),
                                  exitCode);

    }


    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Sep 11, '14
     * @author         Enter your name here...
     */
    public static class CommandResults {

        /** _more_ */
        private String stdoutMsg;

        /** _more_ */
        private String stderrMsg;

        /** _more_ */
        private int exitCode;

        /**
         * _more_
         *
         * @param stdoutMsg _more_
         * @param stderrMsg _more_
         * @param exitCode _more_
         */
        public CommandResults(String stdoutMsg, String stderrMsg,
                              int exitCode) {
            this.stdoutMsg = stdoutMsg;
            this.stderrMsg = stderrMsg;
            this.exitCode  = exitCode;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public String getStderrMsg() {
            return stderrMsg;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public String getStdoutMsg() {
            return stdoutMsg;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public int getExitCode() {
            return exitCode;
        }

    }

}
