/*
* Copyright (c) 2008-2025 Geode Systems LLC
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


import org.ramadda.geodata.cdmdata.CdmDataOutputHandler;
import org.ramadda.geodata.cdmdata.OpendapApiHandler;
import org.ramadda.repository.Association;
import org.ramadda.repository.Entry;
import org.ramadda.repository.Repository;
import org.ramadda.repository.RepositoryManager;
import org.ramadda.repository.Request;
import org.ramadda.repository.Resource;
import org.ramadda.repository.database.Tables;
import org.ramadda.repository.job.JobManager;
import org.ramadda.repository.type.CollectionTypeHandler;
import org.ramadda.repository.type.Column;
import org.ramadda.repository.type.GranuleTypeHandler;
import org.ramadda.repository.type.TypeHandler;
import org.ramadda.service.Service;
import org.ramadda.service.ServiceInput;
import org.ramadda.service.ServiceOperand;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;
import org.ramadda.util.sql.Clause;

import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;

import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;

import ucar.visad.data.CalendarDateTime;

import visad.util.ThreadManager;


import java.io.File;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;


/**
 * Base class for CDO data services
 */
public abstract class CDODataService extends Service {

    /** months */
    protected static final String[] MONTHS = {
        "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct",
        "Nov", "Dec"
    };


    /** localhost name */
    public static final String CDO_SERVICE_LOCALHOST_NAME =
        "service.cdo.localhostname";

    /** localhost port */
    public static final String CDO_SERVICE_LOCALHOST_PORT =
        "service.cdo.localhostport";

    /** localhost protocol */
    public static final String CDO_SERVICE_LOCALHOST_PROTOCOL =
        "service.cdo.localhostprotocol";

    /** time average */
    public static final String ARG_TIME_AVERAGE = "time_average";


    /** the type handler associated with this */
    private CDOOutputHandler outputHandler;

    /**
     * Create a CDO Data Service
     *
     * @param repository  the repository
     * @param id   an id for this service
     * @param label  a label for this service
     *
     * @throws Exception problem creating service
     */
    public CDODataService(Repository repository, String id, String label)
            throws Exception {
        super(repository, id, label);
        outputHandler = new CDOOutputHandler(repository);
    }




    /**
     * Is this enabled?
     *
     * @return true if it is
     */
    public boolean isEnabled() {
        return outputHandler.isEnabled();
    }

    /**
     * Find the associated climatology for the input
     *
     * @param request  the Request
     * @param granule  the entry
     *
     * @return the climatology entry or null
     *
     * @throws Exception  problems
     */
    protected List<Entry> findClimatology(Request request, Entry granule)
            throws Exception {
        return findStatisticEntry(request, granule, "clim");
    }

    /**
     * Find the associated stat (e.g., clim or sprd) for the input
     *
     * @param request  the Request
     * @param granule  the entry
     * @param stat     the stat type (sprd, clim, etc)
     *
     * @return the stat entry or null
     *
     * @throws Exception  problems
     */
    protected List<Entry> findStatisticEntry(Request request, Entry granule,
                                             String stat)
            throws Exception {
        TypeHandler gh = granule.getTypeHandler();
        if ( !(gh instanceof ClimateModelFileTypeHandler)) {
            return null;
        }
        Entry collection = GranuleTypeHandler.getCollectionEntry(request,
                               granule);
        CollectionTypeHandler ctypeHandler =
            (CollectionTypeHandler) collection.getTypeHandler();
        List<Clause>    clauses   = new ArrayList<Clause>();
        List<Column>    columns   = ctypeHandler.getGranuleColumns();
        HashSet<String> seenTable = new HashSet<String>();
        Object[]        values    = granule.getValues();
        for (int colIdx = 0; colIdx < columns.size(); colIdx++) {
            Column column = columns.get(colIdx);
            // first column is the collection ID
            int    valIdx      = colIdx + 1;
            String dbTableName = column.getTableName();
            if ( !seenTable.contains(dbTableName)) {
                clauses.add(
                    Clause.eq(ctypeHandler.getCollectionIdColumn(column),
                              collection.getId()));
                clauses.add(Clause.join(Tables.ENTRIES.COL_ID,
                                        dbTableName + ".id"));
                seenTable.add(dbTableName);
            }
            String v = values[valIdx].toString();
            if (column.getName().equals("ensemble")) {
                clauses.add(Clause.eq(column.getName(), stat));
            } else {
                if (v.length() > 0) {
                    clauses.add(Clause.eq(column.getName(), v));
                }
            }

        }

        return outputHandler.getEntryManager().getEntriesFromDb(request, clauses,
                ctypeHandler.getGranuleTypeHandler());

    }


    /**
     * Make a climatology for the given entry
     * @param request the request
     * @param entry the entry
     * @param dpi  the service input
     * @param tail the tail for the new enty file
     *
     * @return A new entry for the climatology or null
     *
     * @throws Exception  problems creating climatology
     */
    protected Entry makeClimatology(Request request, Entry entry,
                                    ServiceInput dpi, String tail)
            throws Exception {
        return makeClimatology(
            request, entry, dpi, tail,
            ClimateModelApiHandler.DEFAULT_CLIMATE_START_YEAR,
            ClimateModelApiHandler.DEFAULT_CLIMATE_END_YEAR);
    }

    /**
     * Make a climatology for the given entry
     * @param request the request
     * @param entry the entry
     * @param dpi  the service input
     * @param tail the tail for the new enty file
     * @param start start year
     * @param end   end year
     *
     * @return A new entry for the climatology or null
     *
     * @throws Exception  problems creating climatology
     */
    protected Entry makeClimatology(Request request, Entry entry,
                                    ServiceInput dpi, String tail,
                                    String start, String end)
            throws Exception {
        return makeStatistic(request, entry, dpi, tail, "clim", start, end);
    }

    /**
     * Make the standard deviation of the anomaly
     *
     * @param request the request
     * @param mean the file for the statistic to work on
     * @param dpi the input
     * @param tail the file tail
     * @param stat  the statistic to create (mean, sprd, climo)
     * @param startYear start year
     * @param endYear   end year
     *
     * @return the anomaly standard deviation
     *
     * @throws Exception problems
     */
    protected Entry makeStatistic(Request request, Entry mean,
                                  ServiceInput dpi, String tail, String stat,
                                  String startYear, String endYear)
            throws Exception {

        Object[] values = mean.getValues();
        String   model  = values[1].toString();
        String statName = IOUtil.stripExtension(tail) + "_" + model + "_"
                          + stat + "_" + startYear + "-" + endYear + ".nc";
        statName = cleanName(statName);
        File statFile = new File(IOUtil.joinDir(dpi.getProcessDir(),
                                                statName));
        boolean isMonthly = ModelUtil.getFrequency(request, mean).equals(
                                CDOOutputHandler.FREQUENCY_MONTHLY);
        if ( !statFile.exists()) {  // make the file
            List<String> commands = initCDOService();
            boolean      spanYear = doMonthsSpanYearEnd(request, mean);
            if ( !spanYear) {
                String statCmd = isMonthly
                                 ? "-ymonmean"
                                 : "-ydaymean";
                if (stat.equals("sprd") || stat.equals("smegma")) {
                    statCmd = "-yearmean";
                }
                commands.add(statCmd);
            } else {
                int startMonth =
                    request.get(CDOOutputHandler.ARG_CDO_STARTMONTH, 1);
                int endMonth = request.get(CDOOutputHandler.ARG_CDO_ENDMONTH,
                                           startMonth);
                int totalMonths = ((12 - startMonth) + 1) + endMonth;
                if (isMonthly) {
                    commands.add("-timselmean," + totalMonths);
                    /*  when we create a climatology, should we condense it down to one value?
                    if (!(stat.equals("sprd") || stat.equals("smegma"))) {
                        commands.add("-ymonmean");
                    }
                    */
                } else {
                    commands.add("-ydaymean");
                }
            }
            if ( !spanYear) {
                String selyears =
                    ModelUtil.buildClimateYearsString(startYear, endYear,
                        "/");
                commands.add(CDOOutputHandler.OP_SELYEAR + "," + selyears);
            } else {
                // Start in the previous year if possible since DJF in year starts in December year-1
                String seldate = makeDateSelectString(request, mean,
                                     Integer.parseInt(startYear),
                                     Integer.parseInt(endYear));
                commands.add(CDOOutputHandler.OP_SELDATE + "," + seldate);
            }
            getOutputHandler().addMonthSelectServices(request, mean,
                    commands);
            getOutputHandler().addLevelSelectServices(request, mean,
                    commands, CdmDataOutputHandler.ARG_LEVEL);
            commands.add(getPath(request, mean));
            commands.add(statFile.toString());
            //System.err.println("stat command: "+commands);
            runCommands(commands, dpi.getProcessDir(), statFile, 90);  // add more time if daily
        }
        Object[] newValues = new Object[values.length];
        System.arraycopy(values, 0, newValues, 0, values.length);
        newValues[3] = stat;
        Resource resource = new Resource(statFile, Resource.TYPE_LOCAL_FILE);
        TypeHandler myHandler = getRepository().getTypeHandler("file", true);
        Entry statEntry = new Entry(myHandler, true, statFile.toString());
        statEntry.setResource(resource);
        statEntry.setValues(newValues);
        statEntry.addAssociation(new Association(getRepository().getGUID(),
                "generated product",
                "product generated from",
                mean.getId(),
                statEntry.getId()));

        return statEntry;

    }



    /**
     * Make a date select string for request spanning the end of the year
     *
     * @param request  the request
     * @param sample   sample file for dates
     * @param start    start year
     * @param end      end year
     *
     * @return  the date select string
     *
     * @throws Exception problems reading data
     */
    protected String makeDateSelectString(Request request, Entry sample,
                                          int start, int end)
            throws Exception {
        int requestStartMonth =
            request.get(CDOOutputHandler.ARG_CDO_STARTMONTH, 1);
        int requestEndMonth = request.get(CDOOutputHandler.ARG_CDO_ENDMONTH,
                                          1);
        int requestStartDay = request.get(CDOOutputHandler.ARG_CDO_STARTDAY,
                                          1);
        int requestEndDay =
            request.get(CDOOutputHandler.ARG_CDO_ENDDAY,
                        CDOOutputHandler.DAYS_PER_MONTH[requestEndMonth - 1]);  // endday
        CdmDataOutputHandler dataOutputHandler =
            getOutputHandler().getDataOutputHandler();
        GridDataset dataset =
            dataOutputHandler.getCdmManager().getGridDataset(sample,
                getPath(request,
                        sample));
        CalendarDateRange dateRange = dataset.getCalendarDateRange();
        dataOutputHandler.getCdmManager().returnGridDataset(getPath(request,
                sample), dataset);
        //dataset.close();
        int firstDataYearMM = Integer.parseInt(
                                  new CalendarDateTime(
                                      dateRange.getStart()).formattedString(
                                      "yyyyMM",
                                      CalendarDateTime.DEFAULT_TIMEZONE));
        int firstDataYear  = firstDataYearMM / 100;
        int firstDataMonth = firstDataYearMM % 100;
        int lastDataYearMM = Integer.parseInt(
                                 new CalendarDateTime(
                                     dateRange.getEnd()).formattedString(
                                     "yyyyMM",
                                     CalendarDateTime.DEFAULT_TIMEZONE));
        int lastDataYear  = lastDataYearMM / 100;
        int lastDataMonth = lastDataYearMM % 100;
        // can't go back before the beginning of data or past the last data
        if (start <= firstDataYear) {
            start = firstDataYear + 1;
        }
        if (end > lastDataYear) {
            end = lastDataYear;
        }
        if ((end == lastDataYear) && (requestEndMonth > lastDataMonth)) {
            end = lastDataYear - 1;
        }
        StringBuilder yearString = new StringBuilder();
        yearString.append(start - 1);  // startyear
        yearString.append("-");
        yearString.append(StringUtil.padZero(requestStartMonth, 2));  // startmonth
        yearString.append("-");
        yearString.append(StringUtil.padZero(requestStartDay, 2));  // startmonth
        yearString.append("T00:00:00");  // starttime
        yearString.append(",");
        yearString.append(end);                                   //endyear
        yearString.append("-");
        yearString.append(StringUtil.padZero(requestEndMonth, 2));  // endmonth
        yearString.append("-");
        yearString.append(StringUtil.padZero(requestEndDay, 2));  // endmonth
        yearString.append("T23:59:59");                           // endtime

        return yearString.toString();
    }

    /**
     * Get the output handler
     *
     * @return the output handler
     */
    protected CDOOutputHandler getOutputHandler() {
        return outputHandler;
    }

    /**
     * Initialize the CDO command list
     *
     * @return  the initial list of CDO commands
     */
    protected List<String> initCDOService() {
        List<String> newServices = new ArrayList<String>();
        newServices.add(getOutputHandler().getCDOPath());
        newServices.add("-L");
        newServices.add("-s");
        newServices.add("-O");

        return newServices;
    }

    /**
     * Run the process
     *
     * @param commands  the list of commands to run
     * @param processDir  the processing directory
     * @param outFile     the outfile
     *
     * @throws Exception problem running commands
     */
    protected void runCommands(List<String> commands, File processDir,
                               File outFile)
            throws Exception {
        runCommands(commands, processDir, outFile, 60);
    }

    /**
     * Run the process
     *
     * @param commands  the list of commands to run
     * @param processDir  the processing directory
     * @param outFile     the outfile
     * @param timeoutSecs _more_
     *
     * @throws Exception problem running commands
     */
    protected void runCommands(List<String> commands, File processDir,
                               File outFile, int timeoutSecs)
            throws Exception {

        //System.out.println(commands);
        // Have to add this for our stupid system
        Map<String, String> envMap = new HashMap<String, String>();
        envMap.put("HDF5_USE_FILE_LOCKING", "FALSE");
        //envMap.put("HDF5_DISABLE_VERSION_CHECK", "2");


        long millis = System.currentTimeMillis();
        JobManager.CommandResults results =
            getRepository().getJobManager().executeCommand(commands, envMap,
                processDir, 300);
        //processDir, -1);
        //System.out.println("processing took: " + (System.currentTimeMillis()-millis));
        String errorMsg = results.getStderrMsg();
        String outMsg   = results.getStdoutMsg();
        if ( !outFile.exists()) {
            if (outMsg.length() > 0) {
                throw new IllegalArgumentException(outMsg);
            }
            if (errorMsg.length() > 0) {
                throw new IllegalArgumentException(errorMsg);
            }
            if ( !outFile.exists()) {
                throw new IllegalArgumentException("Error processing data.");
            }
        }
    }

    /**
     * Add the statitics widget  - use instead of CDOOutputHandler
     *
     * @param request  the Request
     * @param sb       the HTML
     *
     * @throws Exception problem adding the statistics widget
     */
    public void addStatsWidget(Request request, Appendable sb)
            throws Exception {
        addStatsWidget(request, sb, null);

    }

    /**
     * Add the statistics widget  - use instead of CDOOutputHandler to only show avg/anom
     *
     * @param request  the Request
     * @param sb       the HTML
     * @param si       the input
     *
     * @throws Exception problem adding the statistics widget
     */
    public void addStatsWidget(Request request, Appendable sb,
                               ServiceInput si)
            throws Exception {
        StringBuilder mysb = new StringBuilder();
        mysb.append(HtmlUtils.radio(CDOOutputHandler.ARG_CDO_STAT,
                                    CDOOutputHandler.STAT_MEAN,
                                    RepositoryManager.getShouldButtonBeSelected(
                                        request,
                                        CDOOutputHandler.ARG_CDO_STAT,
                                        CDOOutputHandler.STAT_MEAN,
                                        true)));
        mysb.append(HtmlUtils.space(1));
        mysb.append(Repository.msg("Average"));
        mysb.append(HtmlUtils.space(2));
        boolean anomSelected =
            RepositoryManager.getShouldButtonBeSelected(request,
                CDOOutputHandler.ARG_CDO_STAT, CDOOutputHandler.STAT_ANOM,
                false);
        String anomRB = HtmlUtils.radio(CDOOutputHandler.ARG_CDO_STAT,
                                        CDOOutputHandler.STAT_ANOM,
                                        anomSelected);

        mysb.append(anomRB);
        mysb.append(HtmlUtils.space(1));
        mysb.append(Repository.msg("Anomaly"));
        StringBuilder climyearsSB = new StringBuilder();
        String type =
            si.getProperty(
                "type", ClimateModelApiHandler.ARG_ACTION_COMPARE).toString();
        mysb.append(HtmlUtils.br());
        if (type.equals(ClimateModelApiHandler.ARG_ACTION_ENS_COMPARE)
                || (type.equals(ClimateModelApiHandler.ARG_ACTION_COMPARE)
                    && (si.getOperands().size() == 2))) {
            climyearsSB.append(Repository.msgLabel("Relative to"));
            climyearsSB.append(
                HtmlUtils.radio(CDOOutputHandler.ARG_CLIMATE_DATASET_NUMBER,
                                "1",
                                RepositoryManager.getShouldButtonBeSelected(
                                    request,
                                    CDOOutputHandler
                                        .ARG_CLIMATE_DATASET_NUMBER,
                                    "1",
                                    false)));
            climyearsSB.append(HtmlUtils.space(1));
            climyearsSB.append(Repository.msg("Dataset 1"));
            climyearsSB.append(HtmlUtils.space(2));
            climyearsSB.append(
                HtmlUtils.radio(CDOOutputHandler.ARG_CLIMATE_DATASET_NUMBER,
                                "2",
                                RepositoryManager.getShouldButtonBeSelected(
                                    request,
                                    CDOOutputHandler
                                        .ARG_CLIMATE_DATASET_NUMBER,
                                    "2",
                                    false)));
            climyearsSB.append(HtmlUtils.space(1));
            climyearsSB.append(Repository.msg("Dataset 2"));
            climyearsSB.append(HtmlUtils.space(2));
            climyearsSB.append(
                HtmlUtils.radio(CDOOutputHandler.ARG_CLIMATE_DATASET_NUMBER,
                                "0",
                                RepositoryManager.getShouldButtonBeSelected(
                                    request,
                                    CDOOutputHandler
                                        .ARG_CLIMATE_DATASET_NUMBER,
                                    "0",
                                    true)));
            climyearsSB.append(HtmlUtils.space(1));
            climyearsSB.append(Repository.msg("Own Dataset"));
            climyearsSB.append(HtmlUtils.br());
        }
        climyearsSB.append(Repository.msgLabel("Reference Period"));
        climyearsSB.append(HtmlUtils.space(1));
        addClimYearsWidget(request, climyearsSB, si);

        mysb.append(HtmlUtils.div(climyearsSB.toString(),
                                  HtmlUtils.cssClass("ref-years")));
        mysb.append(
            HtmlUtils.script(
                "$('input[name=\"" + CDOOutputHandler.ARG_CDO_STAT
                + "\"]').on('change', function() {\n"
                + "  $('.ref-years').toggle(this.value === \""
                + CDOOutputHandler.STAT_ANOM + "\" && this.checked);\n"
                + "}).change();"));
        sb.append(HtmlUtils.formEntry(Repository.msgLabel("Statistic"),
                                      mysb.toString()));
    }

    /**
     * Add a widget for selecting the climatology years
     * @param request   the request
     * @param sb        the form to add to
     * @param si        the ServiceInput with all the grids
     * @throws Exception  problems
     */
    protected void addClimYearsWidget(Request request, Appendable sb,
                                      ServiceInput si)
            throws Exception {

        List<List<ServiceOperand>> sortedOps =
            ModelUtil.sortOperandsByCollection(request, si.getOperands());
        int                numOps    = sortedOps.size();
        List<List<String>> dataYears = new ArrayList<List<String>>(numOps);
        for (List<ServiceOperand> ops : sortedOps) {
            List<String> years = new ArrayList<String>();
            Entry        first = ops.get(0).getEntries().get(0);

            //TODO: Get the intersection of all the grid times
            CdmDataOutputHandler dataOutputHandler =
                getOutputHandler().getDataOutputHandler();
            GridDataset dataset =
                dataOutputHandler.getCdmManager().getGridDataset(first,
                    first.getResource().getPath());

            if (dataset != null) {
                List<CalendarDate> dates =
                    CdmDataOutputHandler.getGridDates(dataset);
                SortedSet<String> uniqueYears =
                    Collections.synchronizedSortedSet(new TreeSet<String>());
                if ((dates != null) && !dates.isEmpty()) {
                    for (CalendarDate d : dates) {
                        try {  // shouldn't get an exception
                            String year =
                                new CalendarDateTime(d).formattedString(
                                    "yyyy",
                                    CalendarDateTime.DEFAULT_TIMEZONE);
                            uniqueYears.add(year);
                        } catch (Exception e) {}
                    }
                }
                if ( !uniqueYears.isEmpty()) {
                    years.addAll(uniqueYears);
                }
                dataOutputHandler.getCdmManager().returnGridDataset(
                    first.getResource().getPath(), dataset);
                //dataset.close();
            }
            // TODO:  make a better list of years
            if (years.isEmpty()) {
                for (int i =
                        Integer.parseInt(ClimateModelApiHandler
                            .DEFAULT_CLIMATE_START_YEAR);
                        i <= Integer.parseInt(
                            ClimateModelApiHandler.DEFAULT_CLIMATE_END_YEAR);
                        i++) {
                    years.add(String.valueOf(i));
                }
            }
            dataYears.add(years);
        }
        List<String> commonYears = new ArrayList<String>();
        for (int opNum = 0; opNum < sortedOps.size(); opNum++) {
            List<String> years = dataYears.get(opNum);
            if (opNum == 0) {
                commonYears.addAll(years);
            } else {
                commonYears.retainAll(years);
            }
        }

        StringBuilder yearsWidget = new StringBuilder();
        yearsWidget.append(
            HtmlUtils.select(CDOOutputHandler.ARG_CDO_CLIM_STARTYEAR,
                             commonYears,
                             ClimateModelApiHandler
                                 .DEFAULT_CLIMATE_START_YEAR,
                             HtmlUtils.title(
                                 "Select the starting reference year")));
        yearsWidget.append(Repository.msg(" to "));
        yearsWidget.append(
            HtmlUtils.select(CDOOutputHandler.ARG_CDO_CLIM_ENDYEAR,
                             commonYears,
                             ClimateModelApiHandler.DEFAULT_CLIMATE_END_YEAR,
                             HtmlUtils.title(
                                 "Select the ending reference year")));
        sb.append(yearsWidget.toString());

    }

    /**
     * Add the statitics widget  - use instead of CDOOutputHandler
     *
     * @param request  the Request
     * @param sb       the HTML
     * @param addPct   true add a percent normal option
     * @param isAnom   is this an anomaly file
     * @param haveClimo true if we have a climatology
     * @throws Exception problem creating widget
     */
    public void addStatsWidget(Request request, Appendable sb,
                               boolean addPct, boolean isAnom,
                               boolean haveClimo)
            throws Exception {
        addStatsWidget(request, sb, addPct, isAnom, haveClimo, null,
                       ClimateModelApiHandler.ARG_ACTION_COMPARE);
    }

    /**
     * Add the statitics widget  - use instead of CDOOutputHandler
     *
     * @param request  the Request
     * @param sb       the HTML
     * @param addPct   true add a percent normal option
     * @param isAnom   is this an anomaly file
     * @param haveClimo true if we have a climatology
     * @param si      the input
     * @param type    the request type
     * @throws Exception problem creating widget
     */
    public void addStatsWidget(Request request, Appendable sb,
                               boolean addPct, boolean isAnom,
                               boolean haveClimo, ServiceInput si,
                               String type)
            throws Exception {

        boolean isMonthly = ModelUtil.getFrequency(
                                request, si.getEntries().get(0)).equals(
                                CDOOutputHandler.FREQUENCY_MONTHLY);
        if ( !isAnom) {
            List<TwoFacedObject> stats = new ArrayList<TwoFacedObject>();
            if ( !isMonthly
                    && type.equals(
                        ClimateModelApiHandler.ARG_ACTION_ENS_COMPARE)) {
                // don't show widget if doing a pdf
                return;
                /*
                stats.add(new TwoFacedObject("None",
                                         CDOOutputHandler.STAT_NONE));
                                         */
            } else {
                stats.add(new TwoFacedObject("Average",
                                             CDOOutputHandler.STAT_MEAN));
            }
            if ( !isMonthly && addPct) {  // for now, don't allow this
                stats.add(new TwoFacedObject("Accumulation",
                                             CDOOutputHandler.STAT_SUM));
            }
            //if ( !isMonthly) {  // try this out
                stats.add(new TwoFacedObject("Maximum",
                                             CDOOutputHandler.STAT_MAX));
                stats.add(new TwoFacedObject("Minimum",
                                             CDOOutputHandler.STAT_MIN));
            //}
            if (haveClimo) {
                stats.add(new TwoFacedObject("Anomaly",
                                             CDOOutputHandler.STAT_ANOM));
                if (isMonthly) {  // for now, don't allow this
                    stats.add(new TwoFacedObject("Standardized Anomaly",
                            CDOOutputHandler.STAT_STDANOM));
                }
                if (addPct) {
                    stats.add(new TwoFacedObject("Percent of Normal",
                            CDOOutputHandler.STAT_PCTANOM));
                }
                if ( !type.equals(
                        ClimateModelApiHandler.ARG_ACTION_MULTI_TIMESERIES)
                        && isMonthly) {
                    stats.add(new TwoFacedObject("Standard Deviation",
                            CDOOutputHandler.STAT_STD));
                }
            }

            StringBuilder statForm = new StringBuilder();
            statForm.append(HtmlUtils.select(CDOOutputHandler.ARG_CDO_STAT,
                                             stats,
                                             request.getString(
                                             CDOOutputHandler.ARG_CDO_STAT,
                                             CDOOutputHandler.STAT_MEAN)));

            if ((si != null)
                    && (type.equals(
                    ClimateModelApiHandler
                        .ARG_ACTION_ENS_COMPARE) || type.equals(
                            ClimateModelApiHandler
                                .ARG_ACTION_MULTI_TIMESERIES) || (type.equals(
                                    ClimateModelApiHandler
                                        .ARG_ACTION_COMPARE) && (si.getOperands()
                                            .size() <= 2) && isMonthly))) {
                StringBuilder climyearsSB = new StringBuilder();
                statForm.append(HtmlUtils.br());
                if (type.equals(ClimateModelApiHandler.ARG_ACTION_ENS_COMPARE)
                        || (type.equals(ClimateModelApiHandler
                            .ARG_ACTION_COMPARE) && (si.getOperands().size()
                                == 2))) {
                    climyearsSB.append(Repository.msgLabel("Relative to"));
                    climyearsSB.append(
                        HtmlUtils.radio(
                            CDOOutputHandler.ARG_CLIMATE_DATASET_NUMBER,
                            "1",
                            RepositoryManager.getShouldButtonBeSelected(
                                request,
                                CDOOutputHandler.ARG_CLIMATE_DATASET_NUMBER,
                                "1",
                                false)));
                    climyearsSB.append(HtmlUtils.space(1));
                    climyearsSB.append(Repository.msg("Dataset 1"));
                    climyearsSB.append(HtmlUtils.space(2));
                    climyearsSB.append(
                        HtmlUtils.radio(
                            CDOOutputHandler.ARG_CLIMATE_DATASET_NUMBER,
                            "2",
                            RepositoryManager.getShouldButtonBeSelected(
                                request,
                                CDOOutputHandler.ARG_CLIMATE_DATASET_NUMBER,
                                "2",
                                false)));
                    climyearsSB.append(HtmlUtils.space(1));
                    climyearsSB.append(Repository.msg("Dataset 2"));
                    climyearsSB.append(HtmlUtils.space(2));
                    climyearsSB.append(
                        HtmlUtils.radio(
                            CDOOutputHandler.ARG_CLIMATE_DATASET_NUMBER,
                            "0",
                            RepositoryManager.getShouldButtonBeSelected(
                                request,
                                CDOOutputHandler.ARG_CLIMATE_DATASET_NUMBER,
                                "0",
                                true)));
                    climyearsSB.append(HtmlUtils.space(1));
                    climyearsSB.append(Repository.msg("Own Dataset"));
                    climyearsSB.append(HtmlUtils.br());
                }
                climyearsSB.append(Repository.msgLabel("Reference Period"));
                climyearsSB.append(HtmlUtils.space(1));
                addClimYearsWidget(request, climyearsSB, si);
                statForm.append(HtmlUtils.div(climyearsSB.toString(),
                        HtmlUtils.cssClass("ref-years")));
                statForm.append(HtmlUtils.script("$('select[name=\""
                        + CDOOutputHandler.ARG_CDO_STAT
                        + "\"]').on('change', function() {\n"
                        + "$('.ref-years').toggle(!($(this).val() == \""
                        + CDOOutputHandler.STAT_MEAN
                        + "\" || $(this).val() == \""
                        + CDOOutputHandler.STAT_STD
                        + "\" || $(this).val() == \""
                        + CDOOutputHandler.STAT_MAX
                        + "\" || $(this).val() == \""
                        + CDOOutputHandler.STAT_MIN
                        + "\" || $(this).val() == \""
                        + CDOOutputHandler.STAT_NONE + "\" ));\n"
                        + "}).change();\n"));
            }
            sb.append(HtmlUtils.formEntry(Repository.msgLabel("Statistic"),
                                          statForm.toString()));
        } else {
            sb.append(HtmlUtils.hidden(CDOOutputHandler.ARG_CDO_STAT,
                                       CDOOutputHandler.STAT_ANOM));
        }

    }

    /**
     * Can we handle this input
     *
     * @param input  the input
     *
     * @return true if we can, otherwise false
     */
    public boolean canHandle(ServiceInput input) {
        if ( !getOutputHandler().isEnabled()) {
            return false;
        }

        for (ServiceOperand op : input.getOperands()) {
            if (checkForValidEntries(op.getEntries())) {
                continue;
            } else {
                return false;
            }
        }

        return true;
    }

    /**
     * Check for valid entries.  Subclasses need override as necessary
     * @param entries  list of entries
     * @return
     */
    protected abstract boolean checkForValidEntries(List<Entry> entries);

    /**
     * Make a years string for CDO for the list of years
     *
     * @param years  list of years
     * @param offset offset
     *
     * @return a string representation of the years
     */
    protected String makeCDOYearsString(List<Integer> years, int offset) {
        StringBuilder buf = new StringBuilder();
        for (int year = 0; year < years.size(); year++) {
            buf.append(years.get(year) + offset);
            if (year < years.size() - 1) {
                buf.append(",");
            }
        }

        return buf.toString();
    }

    /**
     * Do the months span the year end (e.g. DJF)
     *
     * @param request    the request
     * @param oneOfThem  a sample file
     *
     * @return  true if they do
     *
     * @throws Exception  problem reading the data
     */
    protected static boolean doMonthsSpanYearEnd(Request request,
            Entry oneOfThem)
            throws Exception {
        return doMonthsSpanYearEnd(request, oneOfThem, 0);
    }

    /**
     * Do the months span the year end (e.g. DJF)
     *
     * @param request    the request
     * @param oneOfThem  a sample file
     * @param opNum      operand number
     *
     * @return  true if they do
     *
     * @throws Exception  problem reading the data
     */
    protected static boolean doMonthsSpanYearEnd(Request request,
            Entry oneOfThem, int opNum)
            throws Exception {
        String opStr = getOpArgString(opNum);
        if (request.defined(CDOOutputHandler.ARG_CDO_MONTHS + opStr)
                && request.getString(CDOOutputHandler.ARG_CDO_MONTHS
                                     + opStr).equalsIgnoreCase("all")) {
            return false;
        }
        // Can't handle years requests yet.
        //if (request.defined(CDOOutputHandler.ARG_CDO_YEARS)
        //        || request.defined(CDOOutputHandler.ARG_CDO_YEARS + "1")) {
        //    return false;
        //}
        if (request.defined(CDOOutputHandler.ARG_CDO_STARTMONTH)
                || request.defined(CDOOutputHandler.ARG_CDO_ENDMONTH)
                || request.defined(CDOOutputHandler.ARG_CDO_STARTMONTH
                                   + opStr)
                || request.defined(CDOOutputHandler.ARG_CDO_ENDMONTH
                                   + opStr)) {
            int startMonth =
                request.get(CDOOutputHandler.ARG_CDO_STARTMONTH + opStr,
                            request.get(CDOOutputHandler.ARG_CDO_STARTMONTH,
                                        1));
            int endMonth =
                request.get(CDOOutputHandler.ARG_CDO_ENDMONTH + opStr,
                            request.get(CDOOutputHandler.ARG_CDO_ENDMONTH,
                                        startMonth));
            // if they requested all months, no need to do a select on month
            if ((startMonth == 1) && (endMonth == 12)) {
                return false;
            }
            if (endMonth > startMonth) {
                return false;
            } else if (startMonth > endMonth) {
                return true;
            }
        }

        return false;
    }




    /**
     * Get the path of the entry
     * @param r request
     * @param e entry
     * @return either the file name or the opendap url if ncml
     */
    protected String getPath(Request r, Entry e) {
        String path = e.getResource().getPath();
        if (IOUtil.hasSuffix(path, "ncml")) {
            // Handle ncml files through OPeNDAP so programs like CDO can use it
            OpendapApiHandler oah =
                (OpendapApiHandler) getRepository().getApiManager()
                .getApiHandler(OpendapApiHandler.API_ID);
            if (oah != null) {
                String odapUrl = oah.getOpendapUrl(e);
                //path = r.getAbsoluteUrl(odapUrl);
                path = getLocalhostUrl(r, odapUrl);
            }
        }

        //System.out.println(path);

        return path;
    }

    /**
     * Get the localhost URL
     *
     * @param r  the request
     * @param url the URL tail
     *
     * @return  the complete URL pointing to localhost
     */
    public String getLocalhostUrl(Request r, String url) {
        int    rport     = r.getServerPort();
        String rprotocol = "http";
        int port = repository.getProperty(CDO_SERVICE_LOCALHOST_PORT, rport);
        /*
        HttpServletRequest httpServletRequest = r.getHttpServletRequest();
        if (httpServletRequest != null) {
            rprotocol = StringUtil.split(httpServletRequest.getScheme(), "/",
                                        true, true).get(0);
        }
        */
        String protocol =
            repository.getProperty(CDO_SERVICE_LOCALHOST_PROTOCOL, rprotocol);
        String localhost = repository.getProperty(CDO_SERVICE_LOCALHOST_NAME,
                               "localhost");
        //        System.err.println("Request.getAbsoluteUrl:" + protocol +" port:" + port);
        if (port == 80) {
            return protocol + "://" + localhost + url;
        } else {
            return protocol + "://" + localhost + ":" + port + url;
        }
    }

    /**
     * Adjust the input to handle operands with multiple files
     *
     * @param request  the request
     * @param input the old input
     * @return a new input or the old
     *
     * @throws Exception problem adjusting the input
     */
    protected ServiceInput adjustInput(Request request, ServiceInput input)
            throws Exception {
        return adjustInput(request, input, true);
    }

    /**
     * Adjust the input to handle operands with multiple files
     *
     * @param request  the request
     * @param input the old input
     * @param adjustDaily adjust the daily
     * @return a new input or the old
     *
     * @throws Exception problem adjusting the input
     */
    protected ServiceInput adjustInput(Request request, ServiceInput input,
                                       boolean adjustDaily)
            throws Exception {
        ServiceInput newInput = new ServiceInput(input.getProcessDir());
        List<ServiceOperand> newOps =
            new ArrayList<ServiceOperand>(input.getOperands().size());
        int opNum = 0;
        for (ServiceOperand so : input.getOperands()) {
            int collectionNum     = ModelUtil.getOperandCollectionNumber(so);
            List<Entry> opEntries = so.getEntries();
            Entry       oneOfThem = opEntries.get(0);
            if (oneOfThem.getTypeHandler()
                    instanceof ClimateModelFileTypeHandler) {
                if ((opEntries.size() == 1)
                        || request.defined(
                            ClimateModelApiHandler.ARG_FORMULA)) {
                    newOps.add(so);
                } else {
                    // Aggregate by time via ncml
                    String id =
                        ModelUtil.makeValuesKey(oneOfThem.getValues(), true,
                            "_");
                    id += "_" + collectionNum;
                    List<Entry> aggEntries = opEntries;
                    // reduce the daily files to just the years requested
                    if (ModelUtil.getFrequency(request,
                            oneOfThem).equals(
                                CDOOutputHandler.FREQUENCY_DAILY)) {
                        if (adjustDaily) {
                            aggEntries = extractDailyEntries(request,
                                    opEntries, collectionNum);
                            id += "_reduced_" + opNum;
                        }
                        List<Entry> newEntries = new ArrayList<Entry>();
                        for (Entry e : aggEntries) {
                            newEntries.add(e);
                        }
                        ServiceOperand newOp =
                            new ServiceOperand(so.getDescription(),
                                newEntries);
                        copyServiceOperandProperties(so, newOp);
                        newOps.add(newOp);
                    } else {
                        Entry agg = ModelUtil.aggregateEntriesByTime(request,
                                        aggEntries, id,
                                        input.getProcessDir());
                        List<Entry> newEntries = new ArrayList<Entry>();
                        newEntries.add(agg);
                        ServiceOperand newOp =
                            new ServiceOperand(so.getDescription(),
                                newEntries);
                        copyServiceOperandProperties(so, newOp);
                        newOps.add(newOp);
                    }
                }
            } else {
                newOps.add(so);
            }
            opNum++;
        }
        newInput.setOperands(newOps);
        ModelUtil.copyServiceInputProperties(input, newInput, "type",
                                             "actionId");
        /*
        // make sure to set the type of service if not null
        if (input.getProperty("type") != null) {
            newInput.putProperty("type", input.getProperty("type"));
        }
        if (input.getProperty("actionId") != null) {
            newInput.putProperty("actionId", input.getProperty("actionId"));
        }
        */

        return newInput;
    }


    /**
     * Get the operand argument as a string
     *
     * @param opNum  the operand number
     *
     * @return the appropriate string for the operand number
     */
    protected static String getOpArgString(int opNum) {
        String opStr = (opNum == 0)
                       ? ""
                       : "" + (opNum + 1);

        return opStr;
    }

    /**
     * Extract only those daily entries that are needed for the request
     * @param request  the request
     * @param opEntries the operand entries
     * @param opNum  which op number
     * @return
     *
     * @throws Exception problems, problems, problems
     */
    private List<Entry> extractDailyEntries(Request request,
                                            List<Entry> opEntries, int opNum)
            throws Exception {

        // for makeInputForm
        if ( !(request.defined(ClimateModelApiHandler.ARG_ACTION_COMPARE)
                || request.defined(
                    ClimateModelApiHandler.ARG_ACTION_ENS_COMPARE)
                || request.defined(
                    ClimateModelApiHandler.ARG_ACTION_MULTI_COMPARE)
                || request.defined(
                    ClimateModelApiHandler.ARG_ACTION_CORRELATION)
                || request.defined(
                    ClimateModelApiHandler.ARG_ACTION_MULTI_TIMESERIES)
                || request.defined(
                    ClimateModelApiHandler.ARG_ACTION_TIMESERIES))) {
            return opEntries;
        }
        List<Integer> years       = new ArrayList<Integer>();
        String        opStr       = getOpArgString(opNum);
        Request       timeRequest = handleNamedTimePeriod(request, opStr);
        boolean       spanYear    = doMonthsSpanYearEnd(timeRequest, null);
        boolean       haveYears   = false;
        // only compare has 2 different years, others only use the single years string
        // this convoluted code handles the case where the user doesn't enter anything for the second
        // dataset and is using years for dataset 1
        if (request.defined(ClimateModelApiHandler.ARG_ACTION_COMPARE)
                || request.defined(
                    ClimateModelApiHandler.ARG_ACTION_ENS_COMPARE)) {
            if (opStr.isEmpty()) {
                haveYears =
                    timeRequest.defined(CDOOutputHandler.ARG_CDO_YEARS);
            } else {
                haveYears =
                    timeRequest.defined(CDOOutputHandler.ARG_CDO_YEARS
                                        + opStr)
                    || ( !timeRequest.defined(
                        CDOOutputHandler.ARG_CDO_STARTYEAR
                        + opStr) && (timeRequest.getString(
                            CDOOutputHandler.ARG_CDO_YEARS + opStr,
                            timeRequest.getString(
                                CDOOutputHandler.ARG_CDO_YEARS,
                                null)) != null));
            }
            /*
            haveYears = timeRequest.defined(CDOOutputHandler.ARG_CDO_YEARS
                                            + opStr);
            */
        } else {
            haveYears = timeRequest.defined(CDOOutputHandler.ARG_CDO_YEARS);
            // TODO: should we do this then? 
            //opStr = "";
        }
        if (haveYears) {
            String yearString = timeRequest.getString(
                                    CDOOutputHandler.ARG_CDO_YEARS + opStr,
                                    timeRequest.getString(
                                        CDOOutputHandler.ARG_CDO_YEARS,
                                        null));
            if (yearString != null) {
                yearString = CDOOutputHandler.verifyYearsList(yearString);
            }
            List<String> yearList = StringUtil.split(yearString, ",", true,
                                        true);
            for (String year : yearList) {
                int iyear = Integer.parseInt(year);
                if (spanYear) {
                    years.add(iyear - 1);
                }
                years.add(Integer.parseInt(year));
            }
        } else {
            int startYear = timeRequest.get(
                                CDOOutputHandler.ARG_CDO_STARTYEAR + opStr,
                                timeRequest.get(
                                    CDOOutputHandler.ARG_CDO_STARTYEAR,
                                    1979));
            int endYear = timeRequest.get(
                              CDOOutputHandler.ARG_CDO_ENDYEAR + opStr,
                              timeRequest.get(
                                  CDOOutputHandler.ARG_CDO_ENDYEAR,
                                  1979));
            if (spanYear) {
                years.add(startYear - 1);
            }
            for (int yr = startYear; yr <= endYear; yr++) {
                years.add(yr);
            }
        }

        List<Entry> newEntries = new ArrayList<Entry>();
        for (Entry yearEntry : opEntries) {
            for (Integer year : years) {
                Date start = Utils.parseDate(year + "-01-01T00:00:00");
                Date end   = Utils.parseDate(year + "-12-31T23:59:59");
                if ((yearEntry.getStartDate() >= start.getTime())
                        && (yearEntry.getEndDate() <= end.getTime())) {
                    newEntries.add(yearEntry);
                }
            }
        }
        if (newEntries.isEmpty()) {
            newEntries = opEntries;
        }

        return newEntries;

    }




    /**
     * Copy over the service operand properties to the new ServiceOperand
     *
     * @param oldOp  old operand
     * @param newOp  new operand
     */
    protected void copyServiceOperandProperties(ServiceOperand oldOp,
            ServiceOperand newOp) {
        String prop = oldOp.getProperty(
                          ClimateModelApiHandler.ARG_COLLECTION).toString();
        //System.err.println(prop);
        if (prop != null) {
            newOp.putProperty(ClimateModelApiHandler.ARG_COLLECTION, prop);
            for (Entry e : newOp.getEntries()) {
                e.putTransientProperty(ClimateModelApiHandler.ARG_COLLECTION,
                                       prop);
            }
        }
    }

    /**
     * Check to see if this is a model grid
     * @param e the entry
     * @return return true if climate model type
     */
    protected boolean isClimateModelType(Entry e) {
        TypeHandler type = e.getTypeHandler();

        return (type instanceof ClimateModelFileTypeHandler);
    }


    /**
     * Common code for cdo evaluations
     *
     * @param request   the request
     * @param input     the input
     * @param argPrefix argument prefix
     * @param name      name of the service
     * @param type      type of request
     *
     * @return list of output operands
     *
     * @throws Exception  problems
     */
    protected List<ServiceOperand> evaluateInner(Request request,
            ServiceInput input, String argPrefix, String name, String type)
            throws Exception {

        boolean isFormula =
            request.defined(ClimateModelApiHandler.ARG_FORMULA);
        // The first time we adjust without reducing daily entries so we have something
        // for the climatology if necessary
        ServiceInput climInput = adjustInput(request, input, false);
        boolean      needAnom  = CDOOutputHandler.requestIsAnom(request);

        if ( !canHandle(climInput)) {
            throw new Exception("Illegal data type");
        }

        final String myType =
            input.getProperty(
                "type", ClimateModelApiHandler.ARG_ACTION_COMPARE).toString();



        List<List<ServiceOperand>> sortedOps = null;
        if (isFormula) {
            sortedOps = new ArrayList<List<ServiceOperand>>();
            sortedOps.add(climInput.getOperands());
        } else {
            sortedOps = ModelUtil.sortOperandsByCollection(request,
                    climInput.getOperands());
        }
        Entry freqSample = sortedOps.get(0).get(0).getEntries().get(0);
        boolean isMonthly = ModelUtil.getFrequency(
                                request, freqSample).equals(
                                CDOOutputHandler.FREQUENCY_MONTHLY);
        Entry[] climSamples = new Entry[sortedOps.size()];
        if (needAnom
                && (type.equals(
                    ClimateModelApiHandler
                        .ARG_ACTION_ENS_COMPARE) || type.equals(
                            ClimateModelApiHandler.ARG_ACTION_COMPARE))) {
            Entry climSample = null;
            int climDatasetNumber =
                request.get(CDOOutputHandler.ARG_CLIMATE_DATASET_NUMBER, 0);
            //sortedOps = sortOpsByModelExperiment(request,
            //        myInput.getOperands());
            //sortedOps = sortOperandsByCollection(request,
            //        myInput.getOperands());
            if ((sortedOps.size() > 1)
                    && (climDatasetNumber > 0)
                    && isMonthly) {
                String climKey = ModelUtil.getModelExperimentString(request,
                                     climDatasetNumber);
                for (List<ServiceOperand> myOp : sortedOps) {
                    Entry    firstOne = myOp.get(0).getEntries().get(0);
                    Object[] values   = firstOne.getValues();
                    String myKey = values[1].toString() + " "
                                   + values[2].toString();
                    if (myKey.equals(climKey)) {
                        climSample = firstOne;

                        break;
                    }
                }
                for (int i = 0; i < sortedOps.size(); i++) {
                    climSamples[i] = climSample;
                }
            } else if ( !isMonthly) {
                for (int i = 0; i < sortedOps.size(); i++) {
                    climSamples[i] =
                        sortedOps.get(i).get(0).getEntries().get(0);
                    i++;
                }
            }
        }
        // If we have daily data, we now adjust to get only the years we need
        if ( !isMonthly) {
            climInput = adjustInput(request, input, true);
            sortedOps = ModelUtil.sortOperandsByCollection(request,
                    climInput.getOperands());
        }

        // make some things final for the threading
        final ServiceInput myInput = climInput;
        final List<ServiceOperand> outputEntries =
            new ArrayList<ServiceOperand>();
        final Request myRequest = request;

        // Check on using multiple threads
        int  opNum    = 0;
        int  numProcs = Runtime.getRuntime().availableProcessors();
        long millis   = System.currentTimeMillis();
        //System.out.println("num Ops = " + myInput.getOperands().size() + ", num processors = " + numProcs);
        int numThreads = Math.min(climInput.getOperands().size(), numProcs);
        boolean useThreads = (numThreads > 2) && true;
        //System.err.println("Using threads: " + useThreads);
        ThreadManager threadManager = new ThreadManager(name + ".evaluate");
        // If we need an anomaly, we run the first evaluation not in a thread so that the climatology
        // can get created first and there is no interference with other threads
        if (needAnom
                && type.equals(
                    ClimateModelApiHandler.ARG_ACTION_MULTI_COMPARE)) {
            useThreads = false;
        }
        for (List<ServiceOperand> ops : sortedOps) {
            int threadNum = 0;
            for (final ServiceOperand op : ops) {
                Entry oneOfThem = op.getEntries().get(0);
                if ( !useThreads || ((threadNum == 0) && needAnom)) {
                    if (isMonthly) {
                        outputEntries.add(evaluateMonthlyRequest(request,
                                myInput,
                                op,
                                opNum,
                                myType,
                                climSamples[opNum]));
                    } else {
                        outputEntries.add(evaluateDailyRequest(request,
                                myInput,
                                op,
                                opNum,
                                myType,
                                climSamples[opNum]));
                    }
                } else {
                    final int     myOp         = opNum;
                    final boolean myIsMonthly  = isMonthly;
                    final Entry   myClimSample = climSamples[opNum];
                    //System.out.println("making thread " + opNum);
                    threadManager.addRunnable(new ThreadManager.MyRunnable() {
                                public void run() throws Exception {
                                    try {
                                        ServiceOperand so;
                                        if (myIsMonthly) {
                                            so =
                                            evaluateMonthlyRequest(myRequest,
                                                myInput, op, myOp, myType,
                                                    myClimSample);
                                        } else {
                                            so =
                                            evaluateDailyRequest(myRequest,
                                                myInput, op, myOp, myType,
                                                    myClimSample);
                                        }
                                        if (so != null) {
                                            synchronized (outputEntries) {
                                                outputEntries.add(so);
                                            }
                                        }
                                    } catch (Exception ve) {
                                        ve.printStackTrace();
                                    }
                                }
                            });
                }
                //if (myInput.getOperands().size() <= 2) {
                //    opNum++;
                //}
                threadNum++;
            }
            opNum++;
        }
        if (useThreads) {
            try {
                //System.out.println("Running in " + numThreads + " threads");
                threadManager.runInParallel(numThreads);
            } catch (Exception ve) {
                ve.printStackTrace();
            }
        }
        List<ServiceOperand> output =
            new ArrayList<ServiceOperand>(outputEntries.size());
        output.addAll(outputEntries);

        return output;

    }

    /**
     * Sort the operands by model and experiment
     *
     * @param request  the request
     * @param operands all the operands from the ServiceInput
     *
     * @return sorted list
     *
     * @throws Exception problems sorting
     */
    private List<List<ServiceOperand>> sortOpsByModelExperiment(
            Request request, List<ServiceOperand> operands)
            throws Exception {
        List<List<ServiceOperand>> sortedList =
            new ArrayList<List<ServiceOperand>>();
        Map<String, List<ServiceOperand>> opMap = new HashMap<String,
                                                      List<ServiceOperand>>();
        for (ServiceOperand op : operands) {
            Entry                sample = op.getEntries().get(0);
            Object[]             values = sample.getValues();
            String key = values[1].toString() + " " + values[2].toString();
            List<ServiceOperand> myList = opMap.get(key);
            if (myList == null) {
                myList = new ArrayList<ServiceOperand>();
            }
            myList.add(op);
            opMap.put(key, myList);
        }

        // check to see if the collections are the same
        int numcollections = 2;
        if (ModelUtil.getModelExperimentString(request,
                1).equals(ModelUtil.getModelExperimentString(request, 2))) {
            numcollections = 1;
        }
        for (int i = 0; i < numcollections; i++) {
            List<ServiceOperand> ops =
                opMap.get(ModelUtil.getModelExperimentString(request,
                                                             i + 1));
            if (ops != null) {
                sortedList.add(ops);
            }
        }

        return sortedList;
    }



    /**
     * Process the monthly request
     *
     * @param request  the request
     * @param dpi      the ServiceInput
     * @param op       the operand
     * @param opNum    the operand number
     * @param type     the type of request
     * @param climSample sample for finding climate entry
     *
     * @return  some output
     *
     * @throws Exception Problem processing the monthly request
     */
    protected abstract ServiceOperand evaluateMonthlyRequest(Request request,
            ServiceInput dpi, ServiceOperand op, int opNum, String type,
            Entry climSample)
     throws Exception;

    /**
     * Process the daily data request
     *
     * @param request  the request
     * @param dpi      the ServiceInput
     * @param op       the ServiceOperand
     * @param opNum    the operand number
     * @param type     request type
     * @param climSample sample for making climatology
     *
     * @return  some output
     *
     * @throws Exception problem processing the daily data
     */
    protected abstract ServiceOperand evaluateDailyRequest(Request request,
            ServiceInput dpi, ServiceOperand op, int opNum, String type,
            Entry climSample)
     throws Exception;

    /**
     * Get the climatology entry
     *
     * @param request  the request
     * @param dpi      the input
     * @param oneOfThem  a sample from the input
     * @param climstartYear  starting year for climatology
     * @param climendYear    ending year for climatology
     * @param climType       type of climatology
     *
     * @return  the entry or null
     *
     * @throws Exception problems
     */
    protected Entry getClimatologyEntry(Request request, ServiceInput dpi,
                                        Entry oneOfThem,
                                        String climstartYear,
                                        String climendYear, String climType)
            throws Exception {
        Entry  climEntry     = null;
        String climFileToUse = null;
        if ( !(climstartYear.equals(
                ClimateModelApiHandler.DEFAULT_CLIMATE_START_YEAR)
                && climendYear.equals(
                    ClimateModelApiHandler.DEFAULT_CLIMATE_END_YEAR))) {
            if (climFileToUse == null) {
                Entry meanEntry = null;
                List<Entry> mean = findStatisticEntry(request, oneOfThem,
                                       "mean");
                if ((mean == null) || mean.isEmpty()) {
                    System.out.println("Couldn't find mean, using entry");
                    meanEntry = oneOfThem;
                } else {
                    if (mean.size() == 1) {
                        meanEntry = mean.get(0);
                    } else {
                        String id =
                            ModelUtil.makeValuesKey(oneOfThem.getValues(),
                                true);
                        meanEntry = ModelUtil.aggregateEntriesByTime(request,
                                mean, id, dpi.getProcessDir());
                    }
                }
                //meanEntry = oneOfThem;
                //Object[] mvals = meanEntry.getValues();
                //String climTail = mvals[4] + "_" + mvals[1] + "_" + mvals[2];
                String mtail =
                    getOutputHandler().getStorageManager().getFileTail(
                        meanEntry);
                climEntry = makeClimatology(request, meanEntry, dpi, mtail,
                                            climstartYear, climendYear);
                if (climEntry != null) {
                    climFileToUse =
                        getOutputHandler().getStorageManager().getFileTail(
                            climEntry);
                }
            }
        } else {
            List<Entry> climo = findClimatology(request, oneOfThem);
            if ((climo == null) || climo.isEmpty()) {
                if (climFileToUse == null) {
                    Entry meanEntry = null;
                    List<Entry> mean = findStatisticEntry(request, oneOfThem,
                                           "mean");
                    if ((mean == null) || mean.isEmpty()) {
                        System.out.println("Couldn't find mean, using entry");
                        meanEntry = oneOfThem;
                    } else {
                        if (mean.size() == 1) {
                            meanEntry = mean.get(0);
                        } else {
                            String id = ModelUtil.makeValuesKey(
                                            oneOfThem.getValues(), true);
                            meanEntry =
                                ModelUtil.aggregateEntriesByTime(request,
                                    mean, id, dpi.getProcessDir());
                        }
                    }
                    //Object[] mvals = meanEntry.getValues();
                    //String climTail = mvals[4] + "_" + mvals[1] + "_" + mvals[2];
                    String mtail =
                        getOutputHandler().getStorageManager().getFileTail(
                            meanEntry);
                    climEntry = makeClimatology(request, meanEntry, dpi,
                            mtail);
                    //throw new Exception("Unable to find climatology for "
                    //                    + oneOfThem.getName());
                    if (climEntry != null) {
                        climFileToUse =
                            getOutputHandler().getStorageManager()
                            .getFileTail(climEntry);
                    }
                }
                System.err.println(
                    "Couldn't find one - making climo for entry "
                    + oneOfThem);
                String tail =
                    getOutputHandler().getStorageManager().getFileTail(
                        oneOfThem);
                climEntry = makeClimatology(request, oneOfThem, dpi, tail);
                //throw new Exception("Unable to find climatology for "
                //                    + oneOfThem.getName());
            } else if (climo.size() > 1) {
                System.err.println("found too many");
            } else {
                climEntry = climo.get(0);
                //System.err.println("found climo: " + climEntry);
            }
        }

        return climEntry;

    }

    /**
     * Get the spread (smegma) entry for the climatology
     *
     * @param request  the request
     * @param dpi      the input
     * @param sample   a sample grid
     * @param climstartYear  starting year for climatology
     * @param climendYear    ending year for climatology
     *
     * @return  an entry or null
     *
     * @throws Exception  problems
     */
    protected Entry getSpreadEntry(Request request, ServiceInput dpi,
                                   Entry sample, String climstartYear,
                                   String climendYear)
            throws Exception {
        return getSpreadEntry(request, dpi, sample, climstartYear,
                              climendYear, "ens01");
    }

    /**
     * Get the spread (smegma) entry for the climatology
     *
     * @param request  the request
     * @param dpi      the input
     * @param sample   a sample grid
     * @param climstartYear  starting year for climatology
     * @param climendYear    ending year for climatology
     * @param ensName   the ensemble name
     *
     * @return  an entry or null
     *
     * @throws Exception  problems
     */
    protected Entry getSpreadEntry(Request request, ServiceInput dpi,
                                   Entry sample, String climstartYear,
                                   String climendYear, String ensName)
            throws Exception {
        Entry sprdEntry = null;
        //System.err.println("Creating spread");
        //String statName = "mean";
        //String statName = "ens01";  // per Marty hoerling - just use the spread of one member
        String statName = ensName;
        // Find the mean
        List<Entry> mean = findStatisticEntry(request, sample, statName);
        if ((mean == null) || mean.isEmpty()) {
            //System.err.println("Couldn't find " + statName);
            // TODO: Should we just exit if no mean?
            sprdEntry = sample;
        } else if (mean.size() > 1) {
            System.err.println("found too many");
        } else {
            sprdEntry = mean.get(0);
            //System.err.println("found mean: " + sprdEntry);
        }
        //sprdEntry = sample;
        // Now make the spread from the mean
        String stail =
            getOutputHandler().getStorageManager().getFileTail(sprdEntry);
        sprdEntry = makeStatistic(request, sprdEntry, dpi, stail, "smegma",
                                  climstartYear, climendYear);

        return sprdEntry;
    }




    /**
     * Handle a named time period request
     *
     * @param request  the request
     * @param opStr the operand
     *
     * @return the answer
     */
    protected Request handleNamedTimePeriod(Request request, String opStr) {
        if ( !request.defined(ClimateModelApiHandler.ARG_EVENT)) {
            return request;
        }
        Request newRequest = request.cloneMe();
        String eventString =
            newRequest.getSanitizedString(ClimateModelApiHandler.ARG_EVENT,
                                          "");
        if (eventString == null) {
            return request;
        }
        List<String> toks = StringUtil.split(eventString, ";");
        if (toks.size() != 4) {
            System.err.println("Bad named time period: " + eventString);

            return request;
        }
        newRequest.remove(ClimateModelApiHandler.ARG_EVENT);
        newRequest.put(CDOOutputHandler.ARG_CDO_STARTMONTH, toks.get(1));
        newRequest.put(CDOOutputHandler.ARG_CDO_ENDMONTH, toks.get(2));
        String years = toks.get(3);
        if (years.indexOf("/") > 0) {
            List<String> ytoks = StringUtil.split(years, "/");
            newRequest.put(CDOOutputHandler.ARG_CDO_STARTYEAR + opStr,
                           ytoks.get(0));
            newRequest.put(CDOOutputHandler.ARG_CDO_ENDYEAR + opStr,
                           ytoks.get(1));
        } else {
            newRequest.put(CDOOutputHandler.ARG_CDO_YEARS + opStr,
                           toks.get(3));
        }

        return newRequest;
    }

    /**
     *     _more_
     *
     *     @param name _more_
     *
     *     @return _more_
     */
    public static String cleanName(String name) {
        name = name.replaceAll("\\s+", "_").replaceAll(",", "_");
        name = name.replaceAll("__+", "_");

        return name;
    }

    /**
     * Calculated the latlon rectangle for this request
     *
     * @param request _more_
     * @param sb _more_
     * @param dataset _more_
     *
     * @throws Exception _more_
     */
    protected void addMapWidget(Request request, Appendable sb,
                                GridDataset dataset)
            throws Exception {
        // if a custom map region was used before, set llr to that.
        LatLonRect llr = null;
        if (request.defined("mapregion")
                && request.getSanitizedString("mapregion",
                        null).equals("CUSTOM")) {
            double maxlon = request.get(CDOOutputHandler.ARG_AREA_EAST,
                                        Double.NaN);
            double minlon = request.get(CDOOutputHandler.ARG_AREA_WEST,
                                        Double.NaN);
            double maxlat = request.get(CDOOutputHandler.ARG_AREA_NORTH,
                                        Double.NaN);
            double minlat = request.get(CDOOutputHandler.ARG_AREA_SOUTH,
                                        Double.NaN);
            if ( !(Double.isNaN(maxlat)
                    || Double.isNaN(minlat)
                    || Double.isNaN(maxlon)
                    || Double.isNaN(minlon))) {
                llr = new LatLonRect(new LatLonPointImpl(maxlat,
                        minlon), new LatLonPointImpl(minlat, maxlon));
            }
        } else {
            if (dataset != null) {
                llr = dataset.getBoundingBox();
            } else {
                llr = new LatLonRect(new LatLonPointImpl(90.0,
                        -180.0), new LatLonPointImpl(-90.0, 180.0));
            }
        }
        getOutputHandler().addMapWidget(request, sb, llr, false);
    }

}
