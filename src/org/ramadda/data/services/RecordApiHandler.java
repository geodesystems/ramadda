/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.data.services;

import org.ramadda.data.record.*;
import org.ramadda.data.record.filter.*;
import org.ramadda.data.services.*;

import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;

import org.ramadda.repository.job.*;
import org.ramadda.repository.map.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.search.*;
import org.ramadda.repository.type.TypeHandler;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.sql.Clause;

import org.ramadda.util.sql.SqlUtil;

import org.w3c.dom.*;

import ucar.unidata.ui.ImageUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TemporaryDir;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.*;

import java.io.*;

import java.io.File;

import java.sql.ResultSet;
import java.sql.Statement;

import java.text.DecimalFormat;

import java.text.DecimalFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.*;

import java.util.zip.*;

/**
 * This implements a number of the top-level record api services.
 *
 * This derives from RAMADDA's SpecialSearch class to provide
 * the top-level Browse record search form and map
 *
 * @author Jeff McWhirter
 */

public abstract class RecordApiHandler extends SpecialSearch implements RequestHandler,
        RecordConstants {

    /**
     * ctor
     *
     * @param repository the main ramadda repository
     * @param node _more_
     * @param props extra properties
     *
     * @throws Exception On badness
     */
    public RecordApiHandler(Repository repository, Element node,
                            Hashtable props)
            throws Exception {
        super(repository, node, props);
    }

    public abstract RecordOutputHandler getRecordOutputHandler();

    /**
     * Utility to make the HTML header
     *
     * @param request The  HTTP request
     * @param sb buffer
     *
     * @throws Exception On badness
     */
    public void makeHeader(Request request, StringBuffer sb)
            throws Exception {}

    /**
     * Utility to make a Result object
     *
     * @param request The  HTTP request
     * @param sb buffer
     *
     * @return The result
     *
     * @throws Exception On badness
     */
    public Result makeResult(Request request, StringBuffer sb)
            throws Exception {
        Result result = new Result("", sb);

        return getRepository().getEntryManager().addEntryHeader(request,
                getRepository().getEntryManager().getRootEntry(), result);
    }

    /**
     * Handle the metrics API call
     *
     * @param request The  HTTP request
     *
     * @return The result
     *
     * @throws Exception On badness
     */
    public Result processMetricsRequest(Request request) throws Exception {
        RecordOutputHandler loh = getRecordOutputHandler();
        StringBuffer        sb  = new StringBuffer();
        makeHeader(request, sb);
        boolean isAdmin = request.getUser().getAdmin();
        if ( !isAdmin) {
            //Allow anyone to view metrics
            //            sb.append(getPageHandler().showDialogError("You must be a site administrator to see metrics"));
            //            return makeResult(request, sb);
        }

        Hashtable<String, long[]> info = new Hashtable<String, long[]>();
        List<Entry>               entries        = new ArrayList<Entry>();
        long                      totalNumPoints = 0;
        long                      totalSize      = 0;
        long                      totalJobs      = 0;
        for (JobInfo jobInfo :
                getRecordOutputHandler().getRecordJobManager().readJobs(
                    JOB_TYPE_POINT)) {
            int numberOfPoints = jobInfo.getNumPoints();
            totalNumPoints += numberOfPoints;
            long productSize = jobInfo.getProductSize();
            totalSize += productSize;
            String entryId = jobInfo.getEntryId();
            long[] values  = info.get(entryId);
            if (values == null) {
                Entry entry = getEntryManager().getEntry(request, entryId);
                if (entry == null) {
                    System.err.println("missing entry from metrics:"
                                       + entryId);

                    continue;
                }
                entries.add(entry);
                values = new long[] { 0, 0, 0 };
                info.put(entryId, values);
            }
            values[0]++;
            totalJobs++;
            values[1] += numberOfPoints;
            values[2] += productSize;
        }

        sb.append(HtmlUtils.p());
        sb.append(
            "<table cellpadding=0 cellspacing=0  class=\"result-table\"><tr class=\"result-header\"><td>Collection</td><td># Jobs</td><td># Points</td><td>Product size</td></tr>");
        String size = "";
        long   mb;
        for (Entry entry : entries) {
            long[] values = info.get(entry.getId());
            sb.append("<tr>");
            sb.append(HtmlUtils.col(getEntryManager().getEntryLink(request,
                    entry, "")));
            sb.append(HtmlUtils.col("" + values[0], " align=right "));
            sb.append(
                HtmlUtils.col(
                    loh.getFormHandler().formatPointCount(values[1]),
                    " align=right "));
            mb = (values[2] / 1000000);
            if (mb > 0) {
                size = mb + "MB";
            } else {
                size = (values[2] / 1000) + "KB";
            }
            sb.append(HtmlUtils.colRight(size));
            sb.append("</tr>");
        }
        sb.append("<tr>");
        sb.append(HtmlUtils.col("&nbsp"));
        sb.append(HtmlUtils.colRight("" + totalJobs));
        sb.append(
            HtmlUtils.colRight(
                loh.getFormHandler().formatPointCount(totalNumPoints)));
        mb = (totalSize / 1000000);
        if (mb > 0) {
            size = mb + "MB";
        } else {
            size = (totalSize / 1000) + "KB";
        }
        sb.append(HtmlUtils.colRight(size));
        sb.append("</tr>");

        sb.append("</table>");

        return makeResult(request, sb);
    }

    /**
     * Handles the show all metrics API request
     *
     * @param request The  HTTP request
     *
     * @return The result
     *
     * @throws Exception On badness
     */
    public Result processMetricsAllJobs(Request request) throws Exception {
        request.put(ARG_ALL, "true");

        return processMetricsJobs(request);
    }

    /**
     * Handles the usage metrics csv request
     *
     * @param request The  HTTP request
     *
     * @return The result
     *
     * @throws Exception On badness
     */
    public Result processMetricsCsvJobs(Request request) throws Exception {
        request.put("csv", "true");

        return processMetricsAllJobs(request);
    }

    /**
     * Process usage metrics request
     *
     * @param request The  HTTP request
     *
     * @return The result
     *
     * @throws Exception On badness
     */
    public Result processMetricsJobs(Request request) throws Exception {

        StringBuffer sb  = new StringBuffer();
        boolean      csv = request.get("csv", false);
        if ( !csv) {
            makeHeader(request, sb);
            if (request.getUser().getAnonymous()) {
                sb.append(
                    getPageHandler().showDialogError(
                        "You must be logged in to see your jobs"));

                return makeResult(request, sb);
            }
        }

        Clause  clause;
        boolean isAdmin = request.getUser().getAdmin();
        if (isAdmin && request.get(ARG_ALL, false)) {
            clause = null;
        } else {
            clause = Clause.eq(JobInfo.DB_COL_USER_ID,
                               request.getUser().getId());
        }
        Statement stmt =
            getDatabaseManager().select(JobInfo.DB_COL_JOB_INFO_BLOB,
                                        JobInfo.DB_TABLE, clause,
                                        " ORDER BY " + JobInfo.DB_COL_DATE
                                        + " DESC ");

        String[] values =
            SqlUtil.readString(getDatabaseManager().getIterator(stmt), 1);
        if (values.length == 0) {
            if ( !csv) {
                sb.append(
                    getPageHandler().showDialogNote("You have no jobs"));
            }

            return makeResult(request, sb);
        }

        if ( !csv) {
            sb.append(HtmlUtils.p());
            sb.append(
                "<table cellpadding=0 cellspacing=0  class=\"result-table\">");
            sb.append(
                "<tr class=\"result-header\"><td align=center>&nbsp;</td><td align=center><b>Job Name</b></td><td align=center><b>Date</b></td><td align=center><b>Collection</b></td><td align=center><b>Contact</b></td><td align=center><b># Points</b></td><td align=center><b>IP</b></td></tr>");

        }
        RecordOutputHandler loh = getRecordOutputHandler();
        for (String blob : values) {
            //Handle old nlas format
            blob = blob.replaceAll(
                "org.unavco.projects.nlas.ramadda.JobInfo",
                "org.ramadda.repository.job.JobInfo");
            JobInfo jobInfo = (JobInfo) getRepository().decodeObject(blob);

            Entry entry = getEntryManager().getEntry(request,
                              jobInfo.getEntryId());
            if (entry == null) {
                continue;
            }
            if ( !csv) {
                String jobUrl = loh.getRecordJobManager().getJobUrl(request,
                                    entry, jobInfo.getJobId(),
                                    loh.OUTPUT_RESULTS);
                sb.append("<tr>");
                sb.append(HtmlUtils.col(HtmlUtils.href(jobUrl,
                        msg("Details"))));
                sb.append(HtmlUtils.col(jobInfo.getJobName() + "&nbsp;"));
                sb.append(
                    HtmlUtils.col(
                        loh.getFormHandler().formatDate(
                            jobInfo.getStartDate())));
                sb.append(
                    HtmlUtils.col(
                        getEntryManager().getEntryLink(request, entry, "")));
                sb.append(HtmlUtils.col(jobInfo.getEmailForDisplay()
                                        + "&nbsp;"));
                sb.append(HtmlUtils.colRight("" + jobInfo.getNumPoints()));
                sb.append(HtmlUtils.col(jobInfo.getIpAddress()));
                sb.append("</tr>");
            } else {
                sb.append(
                    loh.getFormHandler().formatDate(jobInfo.getStartDate()));
                sb.append(",");
                sb.append(entry.getName().replaceAll(",", "_"));
                sb.append(",");
                sb.append(jobInfo.getEmailForDisplay());
                sb.append(",");
                sb.append(jobInfo.getNumPoints());
                sb.append(",");
                sb.append(jobInfo.getIpAddress());
                sb.append("\n");
            }
        }
        if ( !csv) {
            sb.append("</table>");

            return makeResult(request, sb);
        }

        return new Result("", sb,
                          getRepository().getMimeTypeFromSuffix(".csv"));

    }

}
