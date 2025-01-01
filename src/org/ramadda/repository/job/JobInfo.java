/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.job;


import org.ramadda.repository.*;


import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;


/**
 * Holds information about processing jobs
 *
 */
@SuppressWarnings("unchecked")
public class JobInfo implements Constants {

    /** _more_ */
    public static final String ARG_REQUEST_CLIENT = "request.client";

    /** _more_ */
    public static final String ARG_REQUEST_DOMAIN = "request.domain";

    /** _more_ */
    public static final String ARG_REQUEST_EMAIL = "request.email";

    /** _more_ */
    public static final String ARG_REQUEST_USER = "request.user";

    /** _more_ */
    public static final String ARG_REQUEST_IP = "request.ip";


    /** url argument */
    public static final String ARG_JOB_EMAIL = "job.email";

    /** url argument */
    public static final String ARG_JOB_ID = "job.id";

    /** url argument */
    public static final String ARG_JOB_NAME = "job.name";

    /** url argument */
    public static final String ARG_JOB_DESCRIPTION = "job.description";

    /** url argument */
    public static final String ARG_JOB_USER = "job.user";



    /** db column for jobs table */
    public static final String DB_TABLE = "jobinfos";

    /** db column for jobs table */
    public static final String DB_COL_ID = "id";

    /** db column for jobs table */
    public static final String DB_COL_ENTRY_ID = "entry_id";

    /** db column for jobs table */
    public static final String DB_COL_DATE = "date";

    /** db column for jobs table */
    public static final String DB_COL_USER_ID = "user_id";

    /** _more_ */
    public static final String DB_COL_TYPE = "type";

    /** db column for jobs table */
    public static final String DB_COL_JOB_INFO_BLOB = "job_info_blob";

    /** db column for jobs table */
    public static final String[] DB_COLUMNS = {
        DB_COL_ID, DB_COL_ENTRY_ID, DB_COL_DATE, DB_COL_USER_ID, DB_COL_TYPE,
        DB_COL_JOB_INFO_BLOB
    };


    /** job status */
    public static final String STATUS_RUNNING = "running";

    /** job status */
    public static final String STATUS_DONE = "done";

    /** job status */
    public static final String STATUS_CANCELLED = "cancelled";

    /** job status */
    public static final String STATUS_INERROR = "inerror";

    /** job status */
    public static final String STATUS_UNKNOWN = "unknown";


    /** unique job id. This is the jobs.id db column */
    private Object jobId;

    /** job status */
    private String status = STATUS_RUNNING;

    /** _more_ */
    private String type = "processing";

    /** The processing can add various status messages */
    private List<String> statusItems = new ArrayList<String>();

    /** What is currently running */
    private String currentStatus;

    /** how many points did the job process */
    private int numPoints = 0;

    /** start time of job */
    private Date startDate = new Date();

    /** end time of job */
    private Date endDate = new Date();

    /** user name */
    private String user;

    /** user email */
    private String email;

    /** user email */
    private String logEmail;

    /** job name the user gave */
    private String jobName;

    /** _more_ */
    private String jobUrl;

    /** _more_ */
    private String returnUrl;

    /** _more_ */
    private String jobStatusUrl;

    /** job description the user gave */
    private String description;

    /** holds the error message */
    private String error;

    /** ip address of the job requestor */
    private String ipAddress;

    /** the job url arguments */
    private Hashtable urlArguments;

    /** entry id */
    private String entryId;

    /** file size of the products */
    private long productSize;

    /** _more_ */
    private StringBuffer extraInfo = new StringBuffer();

    /** _more_ */
    private Entry entry;

    /** _more_ */
    private String jobLabel;

    /**
     * ctor
     */
    public JobInfo() {}

    /**
     * _more_
     *
     * @param type _more_
     */
    public JobInfo(String type) {
        this.type = type;
    }

    /**
     * ctor
     *
     * @param request the request
     * @param entry _more_
     * @param jobId unique id of the job
     * @param jobLabel _more_
     */
    public JobInfo(Request request, Entry entry, Object jobId,
                   String jobLabel) {
        this.entry        = entry;
        this.jobLabel     = jobLabel;
        this.entryId      = ((entry != null)
                             ? entry.getId()
                             : null);
        this.jobId        = jobId;
        this.urlArguments = new Hashtable(request.getArgs());
        this.ipAddress = request.getEncodedString(ARG_REQUEST_IP,
                request.getIp());
        this.user        = request.getUser().getId();
        this.email       = request.getEncodedString(ARG_JOB_EMAIL, "");
        this.logEmail    = request.getEncodedString(ARG_REQUEST_EMAIL, "");
        this.jobName     = request.getEncodedString(ARG_JOB_NAME, "");
        this.description = request.getEncodedString(ARG_JOB_DESCRIPTION, "");

    }


    /**
     *  Get the Entry property.
     *  This needs to be a different name than the getEntry getter because we encode this object as xml
     *
     * @param entry _more_
     */
    public void setTheEntry(Entry entry) {
        this.entry = entry;
    }

    /**
     *  Get the Entry property.
     *
     *  @return The Entry
     */
    public Entry getEntry() {
        return entry;
    }


    /**
     *  Set the JobLabel property.
     *
     *  @param value The new value for JobLabel
     */
    public void setJobLabel(String value) {
        jobLabel = value;
    }

    /**
     *  Get the JobLabel property.
     *
     *  @return The JobLabel
     */
    public String getJobLabel() {
        return jobLabel;
    }




    /**
     * is job cancelled
     *
     * @return is cancelled
     */
    public boolean isCancelled() {
        return status.equals(STATUS_CANCELLED);
    }


    /**
     * Is job running
     *
     * @return job running
     */
    public boolean isRunning() {
        return status.equals(STATUS_RUNNING);
    }

    /**
     * Is job complete
     *
     * @return job complete
     */
    public boolean isDone() {
        return status.equals(STATUS_DONE);
    }

    /**
     * Was there an error
     *
     * @return in error
     */
    public boolean isInError() {
        return status.equals(STATUS_INERROR);
    }


    /**
     * Set the JobId property.
     *
     * @param value The new value for JobId
     */
    public void setJobId(Object value) {
        jobId = value;
    }

    /**
     * Get the JobId property.
     *
     * @return The JobId
     */
    public Object getJobId() {
        return jobId;
    }


    /**
     * Set the User property.
     *
     * @param value The new value for User
     */
    public void setUser(String value) {
        user = value;
    }

    /**
     * Get the User property.
     *
     * @return The User
     */
    public String getUser() {
        return user;
    }

    /**
     *  Set the RequestArgs property.
     *
     *  @param value The new value for RequestArgs
     * noop -  save for old jobs
     */
    public void setRequestArgs(Hashtable value) {}



    /**
     * Set the JobName property.
     *
     * @param value The new value for JobName
     */
    public void setJobName(String value) {
        jobName = value;
    }

    /**
     * Get the JobName property.
     *
     * @return The JobName
     */
    public String getJobName() {
        return jobName;
    }

    /**
     * Set the JobUrl property.
     *
     * @param value The new value for JobUrl
     */
    public void setJobUrl(String value) {
        jobUrl = value;
    }

    /**
     * Get the JobUrl property.
     *
     * @return The JobUrl
     */
    public String getJobUrl() {
        return jobUrl;
    }

    /**
     * Set the Description property.
     *
     * @param value The new value for Description
     */
    public void setDescription(String value) {
        description = value;
    }

    /**
     * Get the Description property.
     *
     * @return The Description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set the Error property.
     *
     * @param value The new value for Error
     */
    public void setError(String value) {
        status = STATUS_INERROR;
        error  = value;
    }

    /**
     * Get the Error property.
     *
     * @return The Error
     */
    public String getError() {
        return error;
    }



    /**
     *  Set the CurrentStatus property.
     *
     *  @param value The new value for CurrentStatus
     */
    public void setCurrentStatus(String value) {
        currentStatus = value;
    }

    /**
     *  Get the CurrentStatus property.
     *
     *  @return The CurrentStatus
     */
    public String getCurrentStatus() {
        return currentStatus;
    }




    /**
     *  Set the Status property.
     *
     *  @param value The new value for Status
     */
    public void setStatus(String value) {
        status = value;
    }

    /**
     *  Get the Status property.
     *
     *  @return The Status
     */
    public String getStatus() {
        return status;
    }


    /**
     * set date
     *
     * @param d the date
     */
    public void setStartDate(Date d) {
        startDate = d;
    }

    /**
     * get start date
     *
     * @return start date
     */
    public Date getStartDate() {
        return startDate;
    }


    /**
     * Set the EndDate property.
     *
     * @param value The new value for EndDate
     */
    public void setEndDate(Date value) {
        endDate = value;
    }

    /**
     * Get the EndDate property.
     *
     * @return The EndDate
     */
    public Date getEndDate() {
        return endDate;
    }




    /**
     * Add the status message to the list of messages
     *
     * @param item status message
     */
    public void addStatusItem(String item) {
        statusItems.add(item);
        currentStatus = null;
    }

    /**
     * get status messages
     *
     * @return status messages
     */
    public List<String> getStatusItems() {
        return statusItems;
    }


    /**
     *  Set the IpAddress property.
     *
     *  @param value The new value for IpAddress
     */
    public void setIpAddress(String value) {
        ipAddress = value;
    }

    /**
     *  Get the IpAddress property.
     *
     *  @return The IpAddress
     */
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     *  Set the UrlArguments property.
     *
     *  @param value The new value for UrlArguments
     */
    public void setUrlArguments(Hashtable value) {
        urlArguments = value;
    }

    /**
     *  Get the UrlArguments property.
     *
     *  @return The UrlArguments
     */
    public Hashtable getUrlArguments() {
        return urlArguments;
    }

    /**
     *  Set the EntryId property.
     *
     *  @param value The new value for EntryId
     */
    public void setEntryId(String value) {
        entryId = value;
    }

    /**
     *  Get the EntryId property.
     *
     *  @return The EntryId
     */
    public String getEntryId() {
        return entryId;
    }

    /**
     * Set the Email property.
     *
     * @param value The new value for Email
     */
    public void setEmail(String value) {
        email = value;
    }

    /**
     * Get the Email property.
     *
     * @return The Email
     */
    public String getEmail() {
        return email;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getEmailForDisplay() {
        if ((email == null) || (email.length() == 0)) {
            if (logEmail != null) {
                return logEmail;
            }

            return "";
        }

        return email;
    }


    /**
     * Set the ProductSize property.
     *
     * @param value The new value for ProductSize
     */
    public void setProductSize(long value) {
        productSize = value;
    }

    /**
     * Get the ProductSize property.
     *
     * @return The ProductSize
     */
    public long getProductSize() {
        return productSize;
    }

    /**
     * get number of points
     *
     * @return number of points
     */
    public int getNumPoints() {
        return numPoints;
    }

    /**
     * set number of points
     *
     * @param p number of points
     */
    public void setNumPoints(int p) {
        numPoints = p;
    }

    /**
     *  Set the JobStatusUrl property.
     *
     *  @param value The new value for JobStatusUrl
     */
    public void setJobStatusUrl(String value) {
        jobStatusUrl = value;
    }

    /**
     *  Get the JobStatusUrl property.
     *
     *  @return The JobStatusUrl
     */
    public String getJobStatusUrl() {
        return jobStatusUrl;
    }

    /**
     *  Set the Type property.
     *
     *  @param value The new value for Type
     */
    public void setType(String value) {
        type = value;
    }

    /**
     *  Get the Type property.
     *
     *  @return The Type
     */
    public String getType() {
        return type;
    }


    /**
     * _more_
     *
     * @param s _more_
     */
    public void appendExtraInfo(String s) {
        extraInfo.append(s);
    }

    /**
     *  Set the ExtraInfo property.
     *
     *  @param value The new value for ExtraInfo
     */
    public void setExtraInfo(String value) {
        if (value != null) {
            extraInfo = new StringBuffer(value);
        } else {
            extraInfo = new StringBuffer();
        }
    }

    /**
     *  Get the ExtraInfo property.
     *
     *  @return The ExtraInfo
     */
    public String getExtraInfo() {
        return extraInfo.toString();
    }


    /**
     *  Set the ReturnUrl property.
     *
     *  @param value The new value for ReturnUrl
     */
    public void setReturnUrl(String value) {
        returnUrl = value;
    }

    /**
     *  Get the ReturnUrl property.
     *
     *  @return The ReturnUrl
     */
    public String getReturnUrl() {
        return returnUrl;
    }



}
