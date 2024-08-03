/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.model;


import org.ramadda.data.services.NoaaPsdMonthlyClimateIndexTypeHandler;
import org.ramadda.data.services.PointOutputHandler;
import org.ramadda.geodata.cdmdata.CdmDataOutputHandler;
import org.ramadda.repository.Association;
import org.ramadda.repository.Entry;
import org.ramadda.repository.Repository;
import org.ramadda.repository.RepositoryManager;
import org.ramadda.repository.Request;
import org.ramadda.repository.Resource;
import org.ramadda.repository.type.TypeHandler;
import org.ramadda.service.ServiceInput;
import org.ramadda.service.ServiceOperand;
import org.ramadda.service.ServiceOutput;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;

//import ucar.ma2.Range.Iterator;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.time.Calendar;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;

import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;

import ucar.visad.data.CalendarDateTime;

import visad.DateTime;

import visad.util.ThreadManager;


import java.io.File;
import java.io.OutputStream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeSet;



/**
 * Class to handle extraction of time series for correlation/regression
 */
public class CDOTimeSeriesComparison extends CDODataService {

    /**
     * Area statistics service
     *
     * @param repository  the Repository
     *
     * @throws Exception  problems
     */
    public CDOTimeSeriesComparison(Repository repository) throws Exception {
        super(repository, "CDO_TIMESERIES_COMPARISON",
              "Time Series Comparison");
    }

    /**
     * Add to form
     *
     * @param request  the Request
     * @param input    the ServiceInput
     * @param sb       the form
     * @param argPrefix argument prefixes
     * @param label     the label
     *
     * @throws Exception  problem adding to the form
     */
    @Override
    public void addToForm(Request request, ServiceInput input, Appendable sb,
                          String argPrefix, String label)
            throws Exception {
        sb.append(HtmlUtils.formTable());
        makeInputForm(request, input, sb, argPrefix);
        sb.append(HtmlUtils.formTableClose());
    }

    /**
     * Make the input form
     *
     * @param request  the Request
     * @param input    the ServiceInput
     * @param sb       the StringBuilder
     * @param argPrefix the prefix
     *
     * @throws Exception  problem making stuff
     */
    private void makeInputForm(Request request, ServiceInput input,
                               Appendable sb, String argPrefix)
            throws Exception {

        input = adjustInput(request, input);
        List<Entry> entries = input.getEntries();
        GridDataset dataset = null;
        Entry       tsEntry = null;
        for (Entry e : entries) {
            if (isClimateModelType(e)) {
                if (dataset == null) {
                    CdmDataOutputHandler dataOutputHandler =
                        getOutputHandler().getDataOutputHandler();
                    dataset =
                        dataOutputHandler.getCdmManager().getGridDataset(e,
                            getPath(request, e));

                    if (dataset != null) {
                        getOutputHandler().addVarLevelWidget(request, sb,
                                dataset, CdmDataOutputHandler.ARG_LEVEL);
                    }
                }
            } else if (e.getTypeHandler()
                       instanceof NoaaPsdMonthlyClimateIndexTypeHandler) {
                tsEntry = e;
            }
        }
        if (dataset == null) {
            throw new Exception("No grids found");
        }

        if (tsEntry != null) {
            sb.append(HtmlUtils.formEntry(msgLabel("Time Series"),
                                          tsEntry.getDescription()));
        }

        //addStatsWidget(request, sb);
        addTimeWidget(request, sb, input);

        addMapWidget(request, sb, dataset);

        if (dataset != null) {
            dataset.close();
        }
    }

    /**
     * Add a time widget
     *
     * @param request  the Request
     * @param sb       the HTML page
     * @param input    the input
     *
     * @throws Exception  problem making datasets
     */
    public void addTimeWidget(Request request, Appendable sb,
                              ServiceInput input)
            throws Exception {

        CDOOutputHandler.makeMonthsWidget(request, sb, null);
        makeYearsWidget(request, sb, input);
        makeTimeSeriesMonthsWidget(request, sb, null);
        //TODO: add in a lag widget
        /*
        sb.append(HtmlUtils
                .formEntry(Repository.msgLabel("Lag/Lead"),
                        HtmlUtils.input(CDOOutputHandler
                .ARG_CDO_STARTMONTH_LAG, request
                    .getString(CDOOutputHandler.ARG_CDO_STARTMONTH_LAG, ""), 6, HtmlUtils
                            .title("Positive = lead, negative = lag")) + HtmlUtils
                                .space(2) + Repository.msg("(months)")));
        */

    }

    /**
     * Add the time series month selection widget
     *
     * @param request  the Request
     * @param sb       the StringBuilder to add to
     * @param dates    the list of dates (just in case)
     *
     * @throws Exception problems appending
     */
    public static void makeTimeSeriesMonthsWidget(Request request,
            Appendable sb, List<CalendarDate> dates)
            throws Exception {

        StringBuilder leadlagOpts = new StringBuilder();
        leadlagOpts.append(msg("Time Series:"));
        leadlagOpts.append(HtmlUtils.space(1));
        leadlagOpts.append(
            HtmlUtils.radio(
                CDOOutputHandler.ARG_CDO_LEADLAG, "none",
                RepositoryManager.getShouldButtonBeSelected(
                    request, CDOOutputHandler.ARG_CDO_LEADLAG, "none",
                    true)));
        leadlagOpts.append(HtmlUtils.space(1));
        leadlagOpts.append(msg("None"));
        leadlagOpts.append(HtmlUtils.space(2));
        leadlagOpts.append(
            HtmlUtils.radio(
                CDOOutputHandler.ARG_CDO_LEADLAG, "lead",
                RepositoryManager.getShouldButtonBeSelected(
                    request, CDOOutputHandler.ARG_CDO_LEADLAG, "lead",
                    false)));
        leadlagOpts.append(HtmlUtils.space(1));
        leadlagOpts.append(msg("Lead"));
        leadlagOpts.append(HtmlUtils.space(2));
        leadlagOpts.append(
            HtmlUtils.radio(
                CDOOutputHandler.ARG_CDO_LEADLAG, "lag",
                RepositoryManager.getShouldButtonBeSelected(
                    request, CDOOutputHandler.ARG_CDO_LEADLAG, "lag",
                    false)));
        leadlagOpts.append(HtmlUtils.space(1));
        leadlagOpts.append(msg("Lag"));
        leadlagOpts.append(HtmlUtils.br());
        leadlagOpts.append(msgLabel("Months of Time Series Lead/Lag"));
        leadlagOpts.append(HtmlUtils.br());
        List<TwoFacedObject> TSMONTHS =
            new ArrayList<TwoFacedObject>(CDOOutputHandler.MONTHS);
        String opStr = getOpArgString(1);
        TSMONTHS.add(0, new TwoFacedObject("", ""));
        leadlagOpts
            .append(msgLabel("Start")
                + HtmlUtils
                    .select(CDOOutputHandler.ARG_CDO_STARTMONTH
                        + opStr, TSMONTHS, request
                            .getSanitizedString(CDOOutputHandler
                                .ARG_CDO_STARTMONTH + opStr, null), HtmlUtils
                                    .title("Select the starting timeseries month")) + HtmlUtils
                                        .space(2) + msgLabel("End")
                                            + HtmlUtils
                                                .select(CDOOutputHandler
                                                    .ARG_CDO_ENDMONTH + opStr, TSMONTHS, request
                                                        .getSanitizedString(CDOOutputHandler
                                                            .ARG_CDO_ENDMONTH + opStr, null), HtmlUtils
                                                                .title("Select the ending timeseries month")));
        sb.append(HtmlUtils.formEntry(msgLabel("Lead/Lag"),
                                      leadlagOpts.toString()));
    }

    /**
     * Make the years widget
     * @param request the request
     * @param sb      the form
     * @param input   the data
     * @throws Exception problems
     */
    private void makeYearsWidget(Request request, Appendable sb,
                                 ServiceInput input)
            throws Exception {

        List<List<String>> gridDates = new ArrayList<List<String>>();
        String             calString = null;
        Entry              tsEntry   = null;
        for (ServiceOperand op : input.getOperands()) {
            List<Entry> entries = op.getEntries();
            SortedSet<String> uniqueYears =
                Collections.synchronizedSortedSet(new TreeSet<String>());
            for (Entry e : entries) {
                if (isClimateModelType(e)) {
                    CdmDataOutputHandler dataOutputHandler =
                        getOutputHandler().getDataOutputHandler();
                    GridDataset dataset =
                        dataOutputHandler.getCdmManager().getGridDataset(e,
                            getPath(request, e));
                    if (dataset != null) {
                        List<CalendarDate> dates =
                            CdmDataOutputHandler.getGridDates(dataset);
                        dataOutputHandler.getCdmManager().returnGridDataset(
                            getPath(request, e), dataset);
                        if ((dates != null) && !dates.isEmpty()) {
                            if (calString == null) {
                                CalendarDate cd  = dates.get(0);
                                Calendar     cal = cd.getCalendar();
                                if (cal != null) {
                                    calString = cal.toString();
                                    sb.append(
                                        HtmlUtils.hidden(
                                            CdmDataOutputHandler.ARG_CALENDAR,
                                            request.getSanitizedString(
                                                CdmDataOutputHandler.ARG_CALENDAR,
                                                    calString)));
                                }
                            }
                            for (CalendarDate d : dates) {
                                try {  // shouldn't get an exception
                                    String year =
                                        new CalendarDateTime(d)
                                            .formattedString("yyyy",
                                                CalendarDateTime
                                                    .DEFAULT_TIMEZONE);
                                    uniqueYears.add(year);
                                } catch (Exception excp) {}
                            }
                        }
                    }
                } else if (e.getTypeHandler()
                           instanceof NoaaPsdMonthlyClimateIndexTypeHandler) {
                    // For now, we can only handle one tsEntry
                    tsEntry = e;

                    break;
                }
            }
            if ( !uniqueYears.isEmpty()) {
                gridDates.add(new ArrayList<String>(uniqueYears));
            }
        }
        List<String> commonYears = new ArrayList<String>();
        int          grid        = 0;
        int          numGroups   = gridDates.size();
        for (List<String> years : gridDates) {
            // TODO:  make a better list of years
            if (years.isEmpty()) {
                for (int i = 1979; i <= 2012; i++) {
                    years.add(String.valueOf(i));
                }
            }
            if (grid == 0) {
                commonYears.addAll(years);
            } else {
                commonYears.retainAll(years);
            }
            if (grid < numGroups - 1) {
                grid++;

                continue;
            } else {
                years = commonYears;
            }
            grid++;
        }
        // Get the years from the time series
        List<String> tsYears = getTimeSeriesYears(request, tsEntry);
        // merge them into the common years
        commonYears.retainAll(tsYears);

        // Make the widget
        String yearNum  = "";
        String yrLabel  = Repository.msgLabel("Start");
        int    endIndex = commonYears.size() - 1;
        sb.append(
            HtmlUtils.formEntry(
                Repository.msgLabel("Years"),
                yrLabel
                + HtmlUtils.select(
                    CDOOutputHandler.ARG_CDO_STARTYEAR + yearNum,
                    commonYears,
                    request.getSanitizedString(
                        CDOOutputHandler.ARG_CDO_STARTYEAR + yearNum,
                        request.getSanitizedString(
                            CDOOutputHandler.ARG_CDO_STARTYEAR,
                            commonYears.get(0))), HtmlUtils.title(
                                "Select the starting year")) + HtmlUtils.space(
                                    3) + Repository.msgLabel("End")
                                       + HtmlUtils.select(
                                           CDOOutputHandler.ARG_CDO_ENDYEAR
                                           + yearNum, commonYears,
                                               request.getSanitizedString(
                                                   CDOOutputHandler.ARG_CDO_ENDYEAR
                                                       + yearNum, request.getSanitizedString(
                                                           CDOOutputHandler.ARG_CDO_ENDYEAR,
                                                               commonYears.get(
                                                                   endIndex))), HtmlUtils.title(
                                                                       "Select the ending year"))));


    }

    /**
     * Get the list of common years between the grids and time series
     * @param request  the request
     * @param input  the input data
     * @return the starting and ending dates where the data overlap
     * @throws Exception
     */
    private Date[] getCommonDateRange(Request request, ServiceInput input)
            throws Exception {
        List<GridDataset> grids   = new ArrayList<GridDataset>();
        Entry             tsEntry = null;
        List<Entry>       entries = input.getEntries();
        for (Entry e : entries) {
            if (isClimateModelType(e)) {
                CdmDataOutputHandler dataOutputHandler =
                    getOutputHandler().getDataOutputHandler();
                GridDataset dataset =
                    dataOutputHandler.getCdmManager().getGridDataset(e,
                        getPath(request, e));
                if (dataset != null) {
                    grids.add(dataset);
                }
                dataOutputHandler.getCdmManager().returnGridDataset(
                    getPath(request, e), dataset);
            } else if (e.getTypeHandler()
                       instanceof NoaaPsdMonthlyClimateIndexTypeHandler) {
                tsEntry = e;
            }
        }
        int    grid            = 0;
        int    numGrids        = grids.size();
        Date[] commonDateRange = new Date[2];
        for (GridDataset dataset : grids) {
            List<CalendarDate> dates =
                CdmDataOutputHandler.getGridDates(dataset);
            Date first = dates.get(0).toDate();
            Date last  = dates.get(dates.size() - 1).toDate();
            if (commonDateRange[0] == null) {
                commonDateRange[0] = first;
            } else if (first.compareTo(commonDateRange[0]) > 0) {
                commonDateRange[0] = first;
            }
            if (commonDateRange[1] == null) {
                commonDateRange[1] = dates.get(dates.size() - 1).toDate();
            } else if (last.compareTo(commonDateRange[0]) < 0) {
                commonDateRange[1] = last;
            }
        }
        // Get the years from the time series
        Date[] tsRange = getTimeSeriesDateRange(getTimeSeriesData(request,
                             tsEntry));
        if (tsRange[0].compareTo(commonDateRange[0]) > 0) {
            commonDateRange[0] = tsRange[0];
        }
        if (tsRange[1].compareTo(commonDateRange[1]) < 0) {
            commonDateRange[1] = tsRange[1];
        }
        //Misc.printArray("daterange", commonDateRange);

        return commonDateRange;

    }

    /**
     * Get the list of years from the time series
     *
     * @param r the request
     * @param tsEntry the time series entry
     * @return list of the years of the time series
     *
     * @throws Exception problems
     */
    private List<String> getTimeSeriesYears(Request r, Entry tsEntry)
            throws Exception {
        TimeSeriesData tsd     = getTimeSeriesData(r, tsEntry);
        List<String>   tsYears = new ArrayList<String>();
        for (TimeSeriesRecord tsr : tsd.getRecords()) {
            if (Double.isNaN(tsr.getValue())) {
                continue;
            }
            String year = new DateTime(tsr.getDate().getTime()
                                       / 1000).formattedString("yyyy",
                                           DateTime.DEFAULT_TIMEZONE);
            if ( !tsYears.contains(year)) {
                tsYears.add(year);
            }
        }

        /*
        String start = new DateTime(tsEntry.getStartDate()/1000).formattedString("yyyy",
                                DateTime.DEFAULT_TIMEZONE);
        String end = new DateTime(tsEntry.getEndDate()/1000).formattedString("yyyy",
                                DateTime.DEFAULT_TIMEZONE);
        int startYear = Integer.parseInt(start);
        int endYear = Integer.parseInt(end);
        for (int i = 0; i < (endYear-startYear+1); i++) {
            tsYears.add(String.valueOf(startYear+i));
        }
        */
        return tsYears;
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

        List<ServiceOperand> ops = input.getOperands();
        // TODO: maybe call input.getEntries() and check on that
        // need one grid and one timeseries
        if (ops.size() < 2) {
            return false;
        }
        for (ServiceOperand op : ops) {
            if (checkForValidEntries(op.getEntries())) {
                continue;
            } else {
                return false;
            }
        }

        return true;
    }

    /**
     * Check for valid entries
     * @param entries  list of entries
     * @return
     */
    protected boolean checkForValidEntries(List<Entry> entries) {
        // TODO: change this when we can handle more than one entry (e.g. daily data)
        if (entries.isEmpty()) {
            return false;
        }
        SortedSet<String> uniqueModels =
            Collections.synchronizedSortedSet(new TreeSet<String>());
        SortedSet<String> uniqueMembers =
            Collections.synchronizedSortedSet(new TreeSet<String>());
        boolean isTS = false;
	Request request = getAdminRequest();
        for (Entry entry : entries) {
            TypeHandler th = entry.getTypeHandler();
            if ( !(isClimateModelType(entry)
                    || (th instanceof NoaaPsdMonthlyClimateIndexTypeHandler))) {
                return false;
            }
            if (isClimateModelType(entry)) {
                uniqueModels.add(entry.getValue(request,1).toString());
                uniqueMembers.add(entry.getValue(request,3).toString());
            } else if (th instanceof NoaaPsdMonthlyClimateIndexTypeHandler) {
                isTS = true;
            }
        }
        if ( !isTS) {
            // one model, one member
            if ((uniqueModels.size() == 1) && (uniqueMembers.size() == 1)) {
                return true;
            }

            /*  We don't handle these now.
            // multi-model multi-ensemble - don't want to think about this
            if ((uniqueModels.size() >= 1) && (uniqueMembers.size() > 1)) {
                return false;
            }
            */
            // single model, multi-ensemble
            if ((uniqueModels.size() == 1) && (uniqueMembers.size() >= 1)) {
                return true;
            }

            return false;
        }

        return true;
    }

    /**
     * Process the request
     *
     * @param request  The request
     * @param input  the  data process input
     * @param argPrefix  the prefix for arguments
     *
     * @return  the processed data
     *
     * @throws Exception  problem processing
     */
    @Override
    public ServiceOutput evaluate(Request request, Object actionID, ServiceInput input,
                                  String argPrefix)
            throws Exception {

        if ( !canHandle(input)) {
            throw new Exception("Illegal data type");
        }

        final List<ServiceOperand> outputOperands =
            new ArrayList<ServiceOperand>();
        ServiceInput localInput  = adjustInput(request, input);
        List<Entry>  gridEntries = new ArrayList<Entry>();
        List<Entry>  tsEntries   = new ArrayList<Entry>();
        for (Entry oneOfThem : localInput.getEntries()) {
            if (isClimateModelType(oneOfThem)) {
                gridEntries.add(oneOfThem);
            } else if (oneOfThem.getTypeHandler()
                       instanceof NoaaPsdMonthlyClimateIndexTypeHandler) {
                tsEntries.add(oneOfThem);
            }
        }
        Request tsrequest = adjustRequestDates(request, localInput, 1);
        for (Entry tsEntry : tsEntries) {
            outputOperands.add(processTimeSeriesData(tsrequest, localInput,
                    tsEntry));
        }

        final Request myRequest = adjustRequestDates(request, localInput, 0);
        final ServiceInput myInput = localInput;
        int                opNum   = 0;
        int numProcs = Runtime.getRuntime().availableProcessors();
        //System.out.println("num Ops = " + input.getOperands().size() + ", num processors = " + numProcs);
        int numThreads = Math.min(localInput.getOperands().size(), numProcs);
        boolean useThreads = (numThreads > 2) && true;
        //System.err.println("Using threads: " + useThreads);
        ThreadManager threadManager =
            new ThreadManager("CDOTimeSeriesComparison.evaluate");
        gridEntries = getEntryUtil().sortEntriesOnName(gridEntries, false);
        for (final Entry gridEntry : gridEntries) {
            //            outputOperands.add(processModelData(request, input, gridEntry, 0));
            //        }
            if ( !useThreads) {
                outputOperands.add(processModelData(request, localInput,
                        gridEntry, 0));
            } else {
                final int myOp = opNum;
                //System.out.println("making thread " + opNum);
                threadManager.addRunnable(new ThreadManager.MyRunnable() {
                    public void run() throws Exception {
                        try {
                            ServiceOperand so = processModelData(myRequest,
                                                    myInput, gridEntry, 0);
                            if (so != null) {
                                synchronized (outputOperands) {
                                    outputOperands.add(so);
                                }
                            }
                        } catch (Exception ve) {
                            ve.printStackTrace();
                        }
                    }
                });
            }
        }

        if (localInput.getOperands().size() <= 2) {
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

        return new ServiceOutput(outputOperands);
    }

    /**
     * Process the monthly request
     *
     * @param request  the request
     * @param dpi      the ServiceInput
     * @param op       the operand
     * @param opNum    the operand number
     * @param type     the type of request
     * @param climSample  sample for the climate
     *
     * @return  some output
     *
     * @throws Exception Problem processing the monthly request
     */
    protected ServiceOperand evaluateMonthlyRequest(Request request,
            ServiceInput dpi, ServiceOperand op, int opNum, String type,
            Entry climSample)
            throws Exception {
        //TODO:  implemented since it's abstract in CDODataService.
        //       Eventually, this needs to be reconciled with processModelData
        //       But this is not that day.
        return null;
    }

    /**
     * Process the daily data request
     *
     * @param request  the request
     * @param dpi      the ServiceInput
     * @param op       the ServiceOperand
     * @param opNum    the operand number
     * @param type     the type of request
     * @param climSample  a sample for the climatology
     *
     * @return  some output
     *
     * @throws Exception problem processing the daily data
     */
    protected ServiceOperand evaluateDailyRequest(Request request,
            ServiceInput dpi, ServiceOperand op, int opNum, String type,
            Entry climSample)
            throws Exception {
        return null;
    }

    /**
     *     Process the request
     *
     *     @param request  the request
     *     @param dpi      the ServiceInput
     *     @param sample  a sample entry
     *     @param opNum    the operand number
     *
     *     @return  some output
     *
     *     @throws Exception Problem processing the monthly request
     */
    private ServiceOperand processModelData(Request request,
                                            ServiceInput dpi, Entry sample,
                                            int opNum)
            throws Exception {

        long millis    = System.currentTimeMillis();
        long submillis = System.currentTimeMillis();
        CdmDataOutputHandler dataOutputHandler =
            getOutputHandler().getDataOutputHandler();
        GridDataset dataset =
            dataOutputHandler.getCdmManager().getGridDataset(sample,
                getPath(request, sample));
        CalendarDateRange dateRange = dataset.getCalendarDateRange();
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
        if ((dataset == null) || dataset.getGrids().isEmpty()) {
            throw new Exception("No grids found");
        }
        String varname = ((GridDatatype) dataset.getGrids().get(0)).getName();
        dataset.close();
        submillis = System.currentTimeMillis();

        Object[] values  = sample.getValues(true);
        Object[] avalues = new Object[values.length + 1];
        System.arraycopy(values, 0, avalues, 0, values.length);
        String tail =
            getOutputHandler().getStorageManager().getFileTail(sample);
        String       id       = getRepository().getGUID();
        String       newName  = IOUtil.stripExtension(tail) + "_" + id
                                + ".nc";
        File outFile = new File(IOUtil.joinDir(dpi.getProcessDir(), newName));
        List<String> commands = initCDOService();

        // Select order (left to right) - operations go right to left:
        //   - stats
        //   - region
        //   - variable
        //   - month range
        //   - year or time range
        //   - level   (putting this first speeds things up)
        boolean spanYears = doMonthsSpanYearEnd(request, sample);
        addStatServices(request, sample, commands);
        getOutputHandler().addAreaSelectServices(request, sample, commands);
        getOutputHandler().addGridRemapServices(request, dpi, commands);

        //getOutputHandler().addLevelSelectServices(request, oneOfThem,
        //        commands, CdmDataOutputHandler.ARG_LEVEL);
        commands.add("-selname," + varname);
        // find the start & end month of the request
        int requestStartMonth =
            request.get(CDOOutputHandler.ARG_CDO_STARTMONTH, 1);
        int requestEndMonth = request.get(CDOOutputHandler.ARG_CDO_ENDMONTH,
                                          1);
        // Handle the case where the months span the year end (e.g. DJF)
        // Break it up into two requests
        if (spanYears) {
            //System.out.println("months span the year end");
            String  opStr      = getOpArgString(opNum);

            Request newRequest = request.cloneMe();

            // date select to cover end of year
            int startYear =
                request.get(CDOOutputHandler.ARG_CDO_STARTYEAR + opStr,
                            request.get(CDOOutputHandler.ARG_CDO_STARTYEAR,
                                        1979));
            int endYear =
                request.get(CDOOutputHandler.ARG_CDO_ENDYEAR + opStr,
                            request.get(CDOOutputHandler.ARG_CDO_ENDYEAR,
                                        1979));
            // can't go back before the beginning of data or past the last data
            if (startYear <= firstDataYear) {
                startYear = firstDataYear + 1;
            }
            if (endYear > lastDataYear) {
                endYear = lastDataYear;
            }
            if ((endYear == lastDataYear)
                    && (requestEndMonth > lastDataMonth)) {
                endYear = lastDataYear - 1;
            }
            newRequest.remove(CDOOutputHandler.ARG_CDO_YEARS);
            newRequest.remove(CDOOutputHandler.ARG_CDO_YEARS + opStr);
            newRequest.remove(CDOOutputHandler.ARG_CDO_STARTYEAR);
            newRequest.remove(CDOOutputHandler.ARG_CDO_STARTYEAR + opStr);
            newRequest.remove(CDOOutputHandler.ARG_CDO_ENDYEAR);
            newRequest.remove(CDOOutputHandler.ARG_CDO_ENDYEAR + opStr);
            StringBuilder startyearString = new StringBuilder();
            startyearString.append(startYear - 1);  // startyear
            startyearString.append("-");
            startyearString.append(StringUtil.padZero(requestStartMonth, 2));  // startmonth
            startyearString.append("-");
            startyearString.append("01");  // startday
            startyearString.append("T00:00:00");  // starttime
            StringBuilder endyearString = new StringBuilder();
            endyearString.append(endYear);  //endyear
            endyearString.append("-");
            endyearString.append(StringUtil.padZero(requestEndMonth, 2));  // endmonth
            endyearString.append("-");
            endyearString.append(
                "" + CDOOutputHandler.DAYS_PER_MONTH[requestEndMonth - 1]);  // endday
            endyearString.append("T23:59:59");  // endtime

            // update the request to use the date
            newRequest.put(CDOOutputHandler.ARG_CDO_FROMDATE + opStr,
                           startyearString.toString());
            newRequest.put(CDOOutputHandler.ARG_CDO_TODATE + opStr,
                           endyearString.toString());

            getOutputHandler().addDateSelectServices(newRequest, sample,
                    commands, opNum);
            getOutputHandler().addLevelSelectServices(newRequest, sample,
                    commands, CdmDataOutputHandler.ARG_LEVEL);
            commands.add(getPath(newRequest, sample));
            commands.add(outFile.toString());
            runCommands(commands, dpi.getProcessDir(), outFile);

        } else {
            getOutputHandler().addDateSelectServices(request, sample,
                    commands, opNum);
            getOutputHandler().addLevelSelectServices(request, sample,
                    commands, CdmDataOutputHandler.ARG_LEVEL);

            //System.err.println("cmds:" + commands);

            commands.add(getPath(request, sample));
            commands.add(outFile.toString());
            runCommands(commands, dpi.getProcessDir(), outFile);
        }
        submillis = System.currentTimeMillis();
        //System.err.println("cdo commands took: "+(System.currentTimeMillis()-submillis)+" ms");


        StringBuilder outputName = new StringBuilder();
        // values = collection,model,experiment,ens,var
        // model
        outputName.append(values[1].toString().toUpperCase());
        outputName.append(" ");
        // experiment
        outputName.append(values[2]);
        outputName.append(" ");
        // ens
        String ens = values[3].toString();
        if (ens.equals("mean") || ens.equals("sprd") || ens.equals("clim")) {
            outputName.append("ens");
        }
        outputName.append(ens);
        outputName.append(" ");
        // var
        /*
        outputName.append(values[4]);
        outputName.append(" ");
        outputName.append(stat);
        outputName.append(" ");
        */

        StringBuilder dateSB  = new StringBuilder();
        String        yearNum = (opNum == 0)
                                ? ""
                                : String.valueOf(opNum + 1);
        int           startMonth, endMonth;
        if (request.getString(
                CDOOutputHandler.ARG_CDO_MONTHS).equalsIgnoreCase("all")) {
            startMonth = 1;
            endMonth   = 12;
        } else {
            startMonth = request.defined(CDOOutputHandler.ARG_CDO_STARTMONTH)
                         ? request.get(CDOOutputHandler.ARG_CDO_STARTMONTH, 1)
                         : 1;
            endMonth = request.defined(CDOOutputHandler.ARG_CDO_ENDMONTH)
                       ? request.get(CDOOutputHandler.ARG_CDO_ENDMONTH,
                                     startMonth)
                       : startMonth;
        }
        if (startMonth == endMonth) {
            dateSB.append(MONTHS[startMonth - 1]);
        } else {
            dateSB.append(MONTHS[startMonth - 1]);
            dateSB.append("-");
            dateSB.append(MONTHS[endMonth - 1]);
        }
        dateSB.append(" ");
        if (request.defined(CDOOutputHandler.ARG_CDO_YEARS + yearNum)) {
            dateSB.append(request.getString(CDOOutputHandler.ARG_CDO_YEARS
                                            + yearNum));
        } else if (request.defined(CDOOutputHandler.ARG_CDO_YEARS)
                   && !(request.defined(
                       CDOOutputHandler.ARG_CDO_STARTYEAR
                       + yearNum) || request.defined(
                           CDOOutputHandler.ARG_CDO_ENDYEAR + yearNum))) {
            dateSB.append(request.getString(CDOOutputHandler.ARG_CDO_YEARS));
        } else {
            String startYear =
                request.defined(CDOOutputHandler.ARG_CDO_STARTYEAR + yearNum)
                ? request.getString(CDOOutputHandler.ARG_CDO_STARTYEAR
                                    + yearNum)
                : request.defined(CDOOutputHandler.ARG_CDO_STARTYEAR)
                  ? request.getString(CDOOutputHandler.ARG_CDO_STARTYEAR, "")
                  : "";
            String endYear = request.defined(CDOOutputHandler.ARG_CDO_ENDYEAR
                                             + yearNum)
                             ? request.getString(
                                 CDOOutputHandler.ARG_CDO_ENDYEAR + yearNum)
                             : request.defined(
                                 CDOOutputHandler.ARG_CDO_ENDYEAR)
                               ? request.getString(
                                   CDOOutputHandler.ARG_CDO_ENDYEAR,
                                   startYear)
                               : startYear;
            if (startYear.equals(endYear)) {
                dateSB.append(startYear);
            } else {
                dateSB.append(startYear);
                dateSB.append("-");
                dateSB.append(endYear);
            }
        }
        avalues[5] = dateSB.toString();
        outputName.append(dateSB);
        //System.out.println("Name: " + outputName.toString());
        //System.err.println("processModelData took: "+(System.currentTimeMillis()-millis)+" ms");

        Resource resource = new Resource(outFile, Resource.TYPE_LOCAL_FILE);
        TypeHandler myHandler = getRepository().getTypeHandler("cdm_grid",
                                    true);
        Entry outputEntry = new Entry(myHandler, true, outputName.toString());
        outputEntry.setResource(resource);
        outputEntry.setValues(avalues);
        // Add in lineage and associations
        outputEntry.addAssociation(new Association(getRepository().getGUID(),
                "generated product", "product generated from",
                sample.getId(), outputEntry.getId()));
        getOutputHandler().getEntryManager().writeEntryXmlFile(request,
                outputEntry);

        ServiceOperand newOp = new ServiceOperand(outputName.toString(),
                                   outputEntry);
        newOp.putProperty(ClimateModelApiHandler.ARG_COLLECTION,
                          ClimateModelApiHandler.ARG_COLLECTION1);

        return newOp;

    }

    /**
     * Get the requested/possible date range for the given request
     * @param  request   the request
     * @param  input     the ServiceInput
     * @param  opNum     the type of range (0 = model data, 1 = ts data)
     *
     * @return starting/ending date
     *
     * @throws Exception Problems retrieving dates
     */
    private Date[] getDateRange(Request request, ServiceInput input,
                                int opNum)
            throws Exception {

        String gridOpStr     = getOpArgString(0);
        String tsOpStr       = getOpArgString(1);
        Date[] possibleDates = getCommonDateRange(request, input);

        // find the start & end month of the request
        int modelStartMonth = request.get(CDOOutputHandler.ARG_CDO_STARTMONTH
                                          + gridOpStr, 1);
        int modelEndMonth = request.get(CDOOutputHandler.ARG_CDO_ENDMONTH
                                        + gridOpStr, modelStartMonth);

        int tsStartMonth =
            request.get(CDOOutputHandler.ARG_CDO_STARTMONTH + tsOpStr,
                        request.get(CDOOutputHandler.ARG_CDO_STARTMONTH
                                    + gridOpStr, modelStartMonth));
        int tsEndMonth =
            request.get(CDOOutputHandler.ARG_CDO_ENDMONTH + tsOpStr,
                        request.get(CDOOutputHandler.ARG_CDO_ENDMONTH
                                    + gridOpStr, modelEndMonth));

        String leadlag = request.getString(CDOOutputHandler.ARG_CDO_LEADLAG,
                                           "none");

        if (leadlag.equals("none")
                && ((modelStartMonth != tsStartMonth)
                    || (modelEndMonth != tsEndMonth))) {
            leadlag = "lead";
        }
        int firstDataYearMM = Integer.parseInt(
                                  new DateTime(
                                      possibleDates[0]).formattedString(
                                      "yyyyMM",
                                      CalendarDateTime.DEFAULT_TIMEZONE));
        int firstDataYear  = firstDataYearMM / 100;
        int firstDataMonth = firstDataYearMM % 100;
        int lastDataYearMM = Integer.parseInt(
                                 new DateTime(
                                     possibleDates[1]).formattedString(
                                     "yyyyMM",
                                     CalendarDateTime.DEFAULT_TIMEZONE));
        int lastDataYear  = lastDataYearMM / 100;
        int lastDataMonth = lastDataYearMM % 100;

        int requestStartYear =
            request.get(CDOOutputHandler.ARG_CDO_STARTYEAR, firstDataYear);
        int requestEndYear = request.get(CDOOutputHandler.ARG_CDO_ENDYEAR,
                                         lastDataYear);

        int     modelStartYear = requestStartYear;
        int     modelEndYear   = requestEndYear;
        int     tsStartYear    = requestStartYear;
        int     tsEndYear      = requestEndYear;

        boolean modelSpanYear  = doMonthsSpanYearEnd(request, null, 0);
        boolean tsSpanYear     = doMonthsSpanYearEnd(request, null, 1);

        if (modelSpanYear) {
            // can't go back before the beginning of data or past the last data
            if (modelStartYear <= firstDataYear) {
                modelStartYear = firstDataYear + 1;
            }
        }
        if (modelEndYear > lastDataYear) {
            modelEndYear = lastDataYear;
        }
        if ((modelEndYear == lastDataYear)
                && (modelEndMonth > lastDataMonth)) {
            modelEndYear = lastDataYear - 1;
        }

        if (tsSpanYear) {
            // can't go back before the beginning of data or past the last data
            if (tsStartYear <= firstDataYear) {
                tsStartYear = firstDataYear + 1;
            }
        }
        if (tsEndYear > lastDataYear) {
            tsEndYear = lastDataYear;
        }
        if ((tsEndYear == lastDataYear) && (tsEndMonth > lastDataMonth)) {
            tsEndYear = lastDataYear - 1;
        }

        /*
        System.out.println("Before adjustment "+opNum);
        System.out.println("Model years: " + modelStartYear + "/"
                           + modelEndYear);
        System.out.println("TS years: " + tsStartYear + "/" + tsEndYear);
        */

        /* TODO:  There's got to be a simpler way to figure all this out.  Most was done by trial and error */
        int leadlagYears = 1;
        if (leadlag.equals("none")) {
            if ((tsStartMonth == modelStartMonth)
                    && (tsEndMonth == modelEndMonth)) {
                tsStartYear = modelStartYear;
                tsEndYear   = modelEndYear;
                // do nothing
            } else if (tsSpanYear || (tsStartMonth > modelEndMonth)) {
                modelStartYear = tsStartYear;
                tsEndYear      = modelEndYear;
            }
        } else if (leadlag.equals("lead")) {
            if (((tsStartMonth == modelStartMonth)
                    && (tsEndMonth == modelEndMonth)) && (tsEndYear
                       == modelEndYear)) {
                tsEndYear      = modelEndYear - leadlagYears;
                modelStartYear = tsStartYear + leadlagYears;
            } else if (modelSpanYear) {                     // e.g., DJF
                if (tsSpanYear) {                           // e.g., DJF
                    if (modelEndYear == tsEndYear) {
                        tsEndYear      = modelEndYear - leadlagYears;
                        modelStartYear = tsStartYear + leadlagYears;
                    } else if (tsEndYear >= modelEndYear) {
                        tsEndYear      = modelEndYear - leadlagYears;
                        modelStartYear = tsStartYear + leadlagYears;
                    }
                } else {
                    if (tsStartMonth >= modelStartMonth) {  // D predicts DJF
                        tsEndYear      = modelEndYear - (leadlagYears + 1);
                        modelStartYear = tsStartYear + (leadlagYears + 1);
                    } else {
                        tsEndYear      = modelEndYear - leadlagYears;
                        modelStartYear = tsStartYear + leadlagYears;
                    }
                }
            } else if (tsSpanYear) {                        // tsSpan, but model not
                if (tsEndYear >= modelEndYear) {
                    tsStartYear = modelStartYear;
                    tsEndYear   = modelEndYear;
                }
            } else {                                        // neither span
                if (tsEndMonth >= modelEndMonth) {
                    tsEndYear      = modelEndYear - leadlagYears;
                    modelStartYear = tsStartYear + leadlagYears;
                }
            }
        } else if (leadlag.equals("lag")) {
            if (((tsStartMonth == modelStartMonth)
                    && (tsEndMonth == modelEndMonth)) && (tsEndYear
                       == modelEndYear)) {
                modelEndYear = tsEndYear - leadlagYears;
                tsStartYear  = modelStartYear + leadlagYears;
            } else if (modelSpanYear) {                     // e.g., DJF
                if (tsSpanYear) {                           // e.g., DJF
                    if (modelEndYear >= tsEndYear) {
                        modelEndYear = tsEndYear - leadlagYears;
                        tsStartYear  = modelStartYear + leadlagYears;
                    } else if (tsEndYear >= modelEndYear) {
                        modelEndYear = tsEndYear - leadlagYears;
                        tsStartYear  = modelStartYear + leadlagYears;
                    }
                } else {
                    if (tsStartMonth >= modelStartMonth) {  // DJF predicts D
                        modelEndYear = tsEndYear - (leadlagYears + 1);
                        tsStartYear  = modelStartYear + (leadlagYears + 1);
                    } else {
                        modelEndYear = tsEndYear - leadlagYears;
                        tsStartYear  = modelStartYear + leadlagYears;
                    }
                }
            } else if (tsSpanYear) {                        // tsSpan, but model not
                if (tsEndYear == modelEndYear) {
                    modelEndYear = tsEndYear - leadlagYears;
                    tsStartYear  = modelStartYear + leadlagYears;
                }
            } else {                                        // neither span
                if (tsEndMonth >= modelEndMonth) {
                    modelEndYear = tsEndYear - leadlagYears;
                    tsStartYear  = modelStartYear + leadlagYears;
                }
            }
        }

        // Now do a sanity check
        if (tsStartYear < firstDataYear) {
            tsStartYear++;
        }
        if (modelStartYear < firstDataYear) {
            modelStartYear++;
        }
        if (tsEndYear > lastDataYear) {
            tsEndYear--;
        }
        if (modelEndYear > lastDataYear) {
            modelEndYear--;
        }
        if ((tsEndYear == lastDataYear) && (tsEndMonth > lastDataMonth)) {
            tsEndYear--;
        }
        if ((modelEndYear == lastDataYear)
                && (modelEndMonth > lastDataMonth)) {
            modelEndYear--;
        }
        /*
        System.out.println("After adjustment "+opNum);
        System.out.println("Model years: " + modelStartYear + "/"
                           + modelEndYear);
        System.out.println("TS years: " + tsStartYear + "/" + tsEndYear);
        */
        int    numModelYears = (modelEndYear - modelStartYear) + 1;
        int    numTSYears    = (tsEndYear - tsStartYear) + 1;
        String tsYears       = tsStartYear + "/" + tsEndYear;
        String modelYears    = modelStartYear + "/" + modelEndYear;
        if (numModelYears != numTSYears) {
            System.err.println("Differing number of years, model: "
                               + modelYears + " (" + numModelYears
                               + ") vs. ts: " + tsYears + " (" + numTSYears
                               + ")");
        }

        Date[] newDates = new Date[2];
        if (opNum == 0) {
            newDates[0] =
                Utils.parseDate(modelStartYear + "-"
                                + StringUtil.padZero(modelStartMonth, 2)
                                + "-01T00:00:00Z");
            newDates[1] = Utils.parseDate(modelEndYear + "-"
                                          + StringUtil.padZero(modelEndMonth,
                                              2) + "-28T00:00:00Z");
        } else {
            newDates[0] = Utils.parseDate(tsStartYear + "-"
                                          + StringUtil.padZero(tsStartMonth,
                                              2) + "-01T00:00:00Z");
            newDates[1] = Utils.parseDate(tsEndYear + "-"
                                          + StringUtil.padZero(tsEndMonth, 2)
                                          + "-28T00:00:00Z");
        }

        return newDates;

    }

    /**
     * Add the statistics services
     *
     * @param request    the request
     * @param oneOfThem  a sample entry
     * @param commands   the commands to execute
     *
     * @throws Exception problems doing this
     */
    private void addStatServices(Request request, Entry oneOfThem,
                                 List<String> commands)
            throws Exception {
        // find the start & end month of the request
        int requestStartMonth =
            request.get(CDOOutputHandler.ARG_CDO_STARTMONTH, 1);
        int requestEndMonth = request.get(CDOOutputHandler.ARG_CDO_ENDMONTH,
                                          1);
        int numMonths = 0;
        if (doMonthsSpanYearEnd(request, oneOfThem)) {
            numMonths = ((12 - requestStartMonth) + 1) + requestEndMonth;
        } else {
            numMonths = requestEndMonth - requestStartMonth + 1;
        }
        String timeSelect = "-" + CDOOutputHandler.PERIOD_TIM + "selmean,"
                            + numMonths;
        commands.add(timeSelect);
    }

    /**
     * Get the time series data
     *
     * @param request the request
     * @param tsEntry the entry pointing to the data
     *
     * @return the data
     *
     * @throws Exception problems
     */
    private TimeSeriesData getTimeSeriesData(Request request, Entry tsEntry)
            throws Exception {
        //String url = "http://localhost/repository/entry/show?entryid=79e642ee-dffe-4848-8aae-241e614c0c95&getdata=Get%20Data&output=points.product&product=points.csv
        /*
        StringBuilder url = new StringBuilder();
        url.append(getRepository().getHttpProtocol());
        url.append("://");
        //url.append(request.getServerName());
        url.append("localhost");
        url.append(":");
        url.append(request.getServerPort());
        url.append(getRepository().getUrlBase());
        url.append("/entry/show?entryid=");
        url.append(tsEntry.getId());
        url.append(
            "&getdata=Get Data&output=points.product&product=points.csv");
        System.err.println(url.toString());
        String         contents = IOUtil.readContents(url.toString());
        */
        String contents = getPointOutputHandler().getCsv(request, tsEntry);
        List<String>   lines   = StringUtil.split(contents, "\n", true, true);
        TimeSeriesData data    = new TimeSeriesData(tsEntry.getName());
        int            linenum = 0;
        for (String line : lines) {
            linenum++;
            if (line.startsWith("#") || line.isEmpty() || (linenum == 1)) {
                continue;
            }
            String[] toks = StringUtil.split(line, ",", 2);
            //Date             d     = Utils.parseDate(toks[0]);
            Date             d     = Utils.parseDate(toks[0]);
            double           value = Misc.parseNumber(toks[1]);
            TimeSeriesRecord r     = new TimeSeriesRecord(d, value);
            data.addRecord(r);
        }

        return data;
    }

    /**
     * Get the time series date range
     *
     * @param data  the data
     *
     * @return  the min/max date range for non-missing data
     */
    private Date[] getTimeSeriesDateRange(TimeSeriesData data) {
        Date minDate = new Date(Long.MAX_VALUE);
        Date maxDate = new Date(-Long.MAX_VALUE);
        for (TimeSeriesRecord r : data.getRecords()) {
            Date rd = r.getDate();
            if (Double.isNaN(r.getValue())) {
                continue;
            }
            if (rd.compareTo(minDate) < 0) {
                minDate = rd;
            }
            if (rd.compareTo(maxDate) > 0) {
                maxDate = rd;
            }
        }

        return new Date[] { minDate, maxDate };
    }

    /**
     * Process the time series request
     *
     * @param request  the request
     * @param input    the input
     * @param tsEntry  the time series entry
     *
     * @return a new operand
     *
     * @throws Exception  problems
     */
    private ServiceOperand processTimeSeriesData(Request request,
            ServiceInput input, Entry tsEntry)
            throws Exception {

        String         opStr = getOpArgString(1);
        TimeSeriesData tsd   = getTimeSeriesData(request, tsEntry);
        //Date[] range = getTimeSeriesDateRange(tsd);
        Date[] range = getCommonDateRange(request, input);
        //Date[] dateRange = getDateRange(request, input, 1);
        int firstDataYear =
            Integer.parseInt(new DateTime(range[0]).formattedString("yyyy",
                                          CalendarDateTime.DEFAULT_TIMEZONE));
        // find the start & end month of the request
        int requestStartMonth =
            request.get(CDOOutputHandler.ARG_CDO_STARTMONTH + opStr,
                        request.get(CDOOutputHandler.ARG_CDO_STARTMONTH, 1));
        int requestEndMonth =
            request.get(CDOOutputHandler.ARG_CDO_ENDMONTH + opStr,
                        request.get(CDOOutputHandler.ARG_CDO_ENDMONTH, 1));

        int startYear =
            request.get(CDOOutputHandler.ARG_CDO_STARTYEAR + opStr,
                        request.get(CDOOutputHandler.ARG_CDO_STARTYEAR,
                                    firstDataYear));
        int endYear =
            request.get(CDOOutputHandler.ARG_CDO_ENDYEAR + opStr,
                        request.get(CDOOutputHandler.ARG_CDO_ENDYEAR,
                                    startYear));

        /*
        int modelRequestStartMonth =
            request.get(CDOOutputHandler.ARG_CDO_STARTMONTH, 1);
        int modelRequestEndMonth =
            request.get(CDOOutputHandler.ARG_CDO_ENDMONTH,
                        modelRequestStartMonth);

        String leadlag = request.getString(CDOOutputHandler.ARG_CDO_LEADLAG,
                                           "none");
        if ((requestStartMonth == modelRequestStartMonth)
                && (requestEndMonth == modelRequestEndMonth)) {
            leadlag = "none";
        }

        int firstDataYearMM =
            Integer.parseInt(new DateTime(range[0]).formattedString("yyyyMM",
                                                                    CalendarDateTime
                                                                        .DEFAULT_TIMEZONE));
        int firstDataYear  = firstDataYearMM / 100;
        int firstDataMonth = firstDataYearMM % 100;
        int lastDataYearMM =
            Integer.parseInt(new DateTime(range[1]).formattedString("yyyyMM",
                                                                    CalendarDateTime
                                                                        .DEFAULT_TIMEZONE));
        int           lastDataYear  = lastDataYearMM / 100;
        int           lastDataMonth = lastDataYearMM % 100;
            */
        boolean spanYears = doMonthsSpanYearEnd(request, tsEntry, 1);
        /*
        int startYear =
            Integer.parseInt(new DateTime(dateRange[0]).formattedString("yyyy",
                                                                    CalendarDateTime
                                                                        .DEFAULT_TIMEZONE));
        int endYear =
            Integer.parseInt(new DateTime(dateRange[1]).formattedString("yyyy",
                                                                    CalendarDateTime
                                                                        .DEFAULT_TIMEZONE));
                                                                        */
        List<Integer> years = new ArrayList<Integer>();
        int           numMonths;
        if (spanYears) {
            /*
            startYear = request.get(CDOOutputHandler.ARG_CDO_STARTYEAR, 1979);
            endYear = request.get(CDOOutputHandler.ARG_CDO_ENDYEAR,
                                  startYear);
            // can't go back before the beginning of data or past the last data
            if (startYear <= firstDataYear) {
                startYear = firstDataYear + 1;
            }
            if (endYear > lastDataYear) {
                endYear = lastDataYear;
            }
            if ((endYear == lastDataYear)
                    && (requestEndMonth > lastDataMonth)) {
                endYear = lastDataYear - 1;
            }
            if (modelspanYears && leadlag.equals("lead")) {
                endYear = endYear - 1;
            }
            years = makeYears(startYear, endYear);
            */

            //}
            numMonths = requestEndMonth + (12 - requestStartMonth + 1);
        } else {
            numMonths = requestEndMonth - requestStartMonth + 1;
            /*
            startYear = request.get(CDOOutputHandler.ARG_CDO_STARTYEAR, 1979);
            endYear = request.get(CDOOutputHandler.ARG_CDO_ENDYEAR,
                                  startYear);
            if ((endYear == lastDataYear)
                    && (requestEndMonth > lastDataMonth)) {
                endYear -= 1;
            }
            if (startYear < firstDataYear) {
                startYear = firstDataYear;
            }
            if (modelspanYears
                    && request.getString(CDOOutputHandler.ARG_CDO_LEADLAG,
                                         "lead").equals("lead")) {
                endYear = endYear - 1;
            }
            years = makeYears(startYear, endYear);
            */

        }
        years = makeYears(startYear, endYear);
        int                    numYears = years.size();
        List<TimeSeriesRecord> subset   = new ArrayList<TimeSeriesRecord>();
        GregorianCalendar cal =
            new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        GregorianCalendar cal2 =
            new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        cal.set(GregorianCalendar.DAY_OF_MONTH, 1);
        cal.set(GregorianCalendar.HOUR_OF_DAY, 0);
        cal.set(GregorianCalendar.MINUTE, 0);
        cal.set(GregorianCalendar.SECOND, 0);
        cal.set(GregorianCalendar.MILLISECOND, 0);
        int endyear   = endYear;
        int startyear = startYear;
        // adjustRequestDates gives the ending year of the span, not the starting
        if (spanYears) {
            startyear--;
        }
        cal.set(GregorianCalendar.YEAR, endyear);
        cal.set(GregorianCalendar.MONTH, requestEndMonth - 1);
        Date endDate = cal.getTime();
        cal.set(GregorianCalendar.YEAR, startyear);
        cal.set(GregorianCalendar.MONTH, requestStartMonth - 1);
        Date startDate = cal.getTime();
        //System.out.println("start: "+startDate);
        //System.out.println("end: "+endDate);
        int mcntr = 0;
        int ycntr = 0;
        int year  = startyear;
        //int maxyears = numYears - 1;
        int maxyears = (endyear - startyear);
        for (TimeSeriesRecord tsr : tsd.getRecords()) {
            Date d = tsr.getDate();
            if ((d.compareTo(startDate) < 0) || (d.compareTo(endDate) > 0)) {
                continue;
            }
            cal2.setTime(d);
            if (cal2.get(GregorianCalendar.YEAR)
                    < cal.get(GregorianCalendar.YEAR)) {
                continue;
            }
            if (cal2.get(GregorianCalendar.MONTH)
                    < cal.get(GregorianCalendar.MONTH)) {
                continue;
            }
            //System.err.println(tsr);
            subset.add(tsr);
            if (mcntr < numMonths - 1) {
                cal.add(GregorianCalendar.MONTH, 1);
                mcntr++;
            } else if (ycntr < maxyears) {
                year++;
                cal.set(GregorianCalendar.YEAR, year);
                cal.set(GregorianCalendar.MONTH, requestStartMonth - 1);
                mcntr = 0;
            }
        }
        StringBuilder name = new StringBuilder();
        name.append(tsEntry.getName());
        name.append(": ");
        name.append(MONTHS[requestStartMonth - 1]);
        if (requestStartMonth != requestEndMonth) {
            name.append("-");
            name.append(MONTHS[requestEndMonth - 1]);
        }
        name.append(" ");
        name.append(startYear);
        if (startYear != endYear) {
            name.append("-");
            name.append(endYear);
        }

        TimeSeriesData output           = new TimeSeriesData(name.toString());
        Iterator<TimeSeriesRecord> iter = subset.iterator();
        while (iter.hasNext()) {
            double value = 0;
            Date   d     = null;
            for (int i = 0; i < numMonths; i++) {
                TimeSeriesRecord r = iter.next();
                value += r.getValue();
                d     = r.getDate();
            }
            value = value / numMonths;
            TimeSeriesRecord avg = new TimeSeriesRecord(d, value);
            output.addRecord(avg);
        }
        String csv = output.toString();
        //System.out.println(csv);
        String id      = getRepository().getGUID();
        String newName = tsEntry.getName() + "_" + id + ".csv";
        File outFile = new File(IOUtil.joinDir(input.getProcessDir(),
                           newName));
        OutputStream os =
            getStorageManager().getUncheckedFileOutputStream(outFile);
        os.write(csv.getBytes());
        os.flush();
        os.close();
        Resource resource = new Resource(outFile, Resource.TYPE_LOCAL_FILE);
        TypeHandler myHandler = getRepository().getTypeHandler("point_text",
                                    true);
        Entry outputEntry = new Entry(myHandler, true, name.toString());
        outputEntry.setResource(resource);
        // Add in lineage and associations
        outputEntry.addAssociation(new Association(getRepository().getGUID(),
                "generated product", "product generated from",
                tsEntry.getId(), outputEntry.getId()));
        getOutputHandler().getEntryManager().writeEntryXmlFile(request,
                outputEntry);

        ServiceOperand newOp = new ServiceOperand(name.toString(),
                                   outputEntry);
        newOp.putProperty(ClimateModelApiHandler.ARG_COLLECTION,
                          ClimateModelApiHandler.ARG_COLLECTION2);

        return newOp;

    }

    /**
     * Make a list of years from the start and end
     *
     * @param start  first year
     * @param end    last year
     *
     * @return list of years between start and end
     */
    private List<Integer> makeYears(int start, int end) {
        List<Integer> years = new ArrayList<Integer>();
        for (int i = start; i <= end; i++) {
            years.add(i);
        }

        return years;
    }

    /**
     * Get the point output handler
     *
     * @return the point output handler
     */
    public PointOutputHandler getPointOutputHandler() {
        return (PointOutputHandler) getRepository().getOutputHandler(
            PointOutputHandler.class);
    }

    /**
     * Adjust the request dates to fall within the bounds of the data
     *
     * @param request the request
     * @param input   the input files
     * @param opNum   operand number
     *
     * @return a new request
     *
     * @throws Exception problems
     */
    private Request adjustRequestDates(Request request, ServiceInput input,
                                       int opNum)
            throws Exception {

        Request timeRequest = request.cloneMe();
        String  opStr       = getOpArgString(opNum);
        //Date[] range = getCommonDateRange(request, input);
        Date[] fixedRange = getDateRange(request, input, opNum);
        // find the start & end month of the request
        /*
        int requestStartMonth =
            request.get(CDOOutputHandler.ARG_CDO_STARTMONTH + opStr,
                        request.get(CDOOutputHandler.ARG_CDO_STARTMONTH,
                                    1));
        int requestEndMonth =
            request.get(CDOOutputHandler.ARG_CDO_ENDMONTH + opStr,
                        request.get(CDOOutputHandler.ARG_CDO_ENDMONTH,
                                    1));

        int firstDataYearMM =
            Integer.parseInt(new DateTime(fixedRange[0]).formattedString("yyyyMM",
                                                                    CalendarDateTime
                                                                        .DEFAULT_TIMEZONE));
        int firstDataYear  = firstDataYearMM / 100;
        int firstDataMonth = firstDataYearMM % 100;
        int lastDataYearMM =
            Integer.parseInt(new DateTime(fixedRange[1]).formattedString("yyyyMM",
                                                                    CalendarDateTime
                                                                        .DEFAULT_TIMEZONE));
        int           lastDataYear  = lastDataYearMM / 100;
        int           lastDataMonth = lastDataYearMM % 100;
                                    */
        int startYear = Integer.parseInt(
                            new DateTime(fixedRange[0]).formattedString(
                                "yyyy", CalendarDateTime.DEFAULT_TIMEZONE));
        int endYear = Integer.parseInt(
                          new DateTime(fixedRange[1]).formattedString(
                              "yyyy", CalendarDateTime.DEFAULT_TIMEZONE));
        /*
        boolean       spanYears = doMonthsSpanYearEnd(request, null, opNum);
        List<Integer> years         = new ArrayList<Integer>();
        if (spanYears) {
            boolean haveYears =
                request.defined(CDOOutputHandler.ARG_CDO_YEARS);
            if (haveYears) {
                String yearString =
                    request.getString(CDOOutputHandler.ARG_CDO_YEARS, null);
                if (yearString != null) {
                    yearString = CDOOutputHandler.verifyYearsList(yearString);
                }
                //System.err.println("years: "+yearString);
                List<String> yearList = StringUtil.split(yearString, ",",
                                            true, true);
                for (String year : yearList) {
                    int iyear = Integer.parseInt(year);
                    if ((iyear <= firstDataYear)
                            || (iyear > lastDataYear)
                            || ((iyear == lastDataYear)
                                && (requestEndMonth > lastDataMonth))) {
                        continue;
                    }
                    years.add(iyear);
                }
                request.put(CDOOutputHandler.ARG_CDO_YEARS,
                            yearsListToString(years));
                //System.err.println("new years: "+yearsListToString(years));
            } else {
                startYear = request.get(CDOOutputHandler.ARG_CDO_STARTYEAR,
                                        1979);
                endYear = request.get(CDOOutputHandler.ARG_CDO_ENDYEAR,
                                      startYear);
                //System.err.println("start: "+startYear+", end: "+endYear);
                // can't go back before the beginning of data or past the last data
                if (startYear <= firstDataYear) {
                    startYear = firstDataYear + 1;
                }
                if (endYear > lastDataYear) {
                    endYear = lastDataYear;
                }
                if ((endYear == lastDataYear)
                        && (requestEndMonth > lastDataMonth)) {
                    endYear = lastDataYear - 1;
                }
                request.put(CDOOutputHandler.ARG_CDO_STARTYEAR, startYear);
                request.put(CDOOutputHandler.ARG_CDO_ENDYEAR, endYear);
                //System.err.println("new start: "+startYear+", new end: "+endYear);
            }
        } else {
            startYear = request.get(CDOOutputHandler.ARG_CDO_STARTYEAR, 1979);
            endYear = request.get(CDOOutputHandler.ARG_CDO_ENDYEAR,
                                  startYear);
            //System.err.println("start: "+startYear+", end: "+endYear);
            if ((endYear == lastDataYear)
                    && (requestEndMonth > lastDataMonth)) {
                endYear -= 1;
            }
            if (startYear < firstDataYear) {
                startYear = firstDataYear;
            }
            request.put(CDOOutputHandler.ARG_CDO_STARTYEAR, startYear);
            request.put(CDOOutputHandler.ARG_CDO_ENDYEAR, endYear);
            //System.err.println("new start: "+startYear+", new end: "+endYear);
        }
        */
        timeRequest.put(CDOOutputHandler.ARG_CDO_STARTYEAR, startYear);
        timeRequest.put(CDOOutputHandler.ARG_CDO_ENDYEAR, endYear);

        return timeRequest;

    }

    /**
     * Get the help for this widget
     *
     * @return the help
     */
    @Override
    public String getHelp() {
        try {
            return getStorageManager().readSystemResource(
                "/org/ramadda/geodata/model/htdocs/model/help/correlation-stats.html");
        } catch (Exception excp) {}

        return null;
    }

    /**
     * Create a comma separated list from the years
     * @param l  list of years
     * @return  comma separated list
     */
    private String yearsListToString(List<Integer> l) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < l.size(); i++) {
            if ((i > 0) && (i < l.size() - 1)) {
                sb.append(",");
            }
            sb.append(l.get(i).toString());
        }

        return sb.toString();
    }

    /**
     * Class to hold time series data
     */
    class TimeSeriesData {

        /** The records */
        List<TimeSeriesRecord> records = new ArrayList<TimeSeriesRecord>();

        /** the name */
        String name = null;

        /** the missing value */
        double missingValue = Double.NaN;

        /**
         * Create a time series data with the given name
         *
         * @param name  the name
         */
        TimeSeriesData(String name) {
            this.name = name;
        }

        /**
         * Get the list of data records
         *
         * @return  the records
         */
        public List<TimeSeriesRecord> getRecords() {
            return records;
        }

        /**
         * Get the name
         *
         * @return the name
         */
        public String getName() {
            return name;
        }

        /**
         * Add a record to this time series
         *
         * @param record the record
         */
        public void addRecord(TimeSeriesRecord record) {
            records.add(record);
        }

        /**
         * Get a string representation of the data
         *
         * @return  the string representation of the data
         */
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("# ");
            sb.append(name);
            sb.append("\n");
            sb.append("# Generated on: ");
            try {
                sb.append(new DateTime());
            } catch (Exception e) {
                sb.append(new Date());
            }
            sb.append("\n");
            for (TimeSeriesRecord r : records) {
                try {
                    sb.append(new DateTime(r.getDate()));
                } catch (Exception e) {
                    sb.append(r.getDate());
                }
                sb.append(",");
                sb.append(Misc.format(r.getValue()));
                sb.append("\n");
            }

            return sb.toString();
        }
    }

    /**
     * A holder for a time series point
     */
    class TimeSeriesRecord {

        /** the date */
        Date date;

        /** the value */
        double value;

        /**
         * Create a new record
         *
         * @param d record date
         * @param v record value
         */
        TimeSeriesRecord(Date d, double v) {
            date  = d;
            value = v;
        }

        /**
         * Get the date
         *
         * @return the date
         */
        public Date getDate() {
            return date;
        }

        /**
         * Get the value
         *
         * @return the value
         */
        public double getValue() {
            return value;
        }

        /**
         * Get a string representation of this
         *
         * @return a string representation of this
         */
        public String toString() {
            return date.toString() + ": " + Misc.format(value);
        }

    }

}
