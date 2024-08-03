/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.model;


import org.ramadda.geodata.cdmdata.CdmDataOutputHandler;
import org.ramadda.repository.Entry;
import org.ramadda.repository.Repository;
import org.ramadda.repository.Request;
import org.ramadda.repository.Resource;
import org.ramadda.repository.type.GranuleTypeHandler;
import org.ramadda.repository.type.TypeHandler;
import org.ramadda.service.ServiceInput;
import org.ramadda.service.ServiceOperand;
import org.ramadda.service.ServiceOutput;
import org.ramadda.util.HtmlUtils;

import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.units.SimpleUnit;

import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import ucar.visad.data.CalendarDateTime;

import visad.util.ThreadManager;


import java.io.File;

import java.util.ArrayList;
import java.util.List;


/**
 * Data service for extracting time series information using CDO
 */
public class CDOTimeSeriesService extends CDODataService {

    /**
     * Create the service
     *
     * @param repository  the repository
     *
     * @throws Exception problems
     */
    public CDOTimeSeriesService(Repository repository) throws Exception {
        super(repository, "CDO_TIMESERIES", "Time Series Statistics");
    }


    /**
     * Initialize the form JavaScript
     *
     * @param request  the request
     * @param js  the javascript
     * @param formVar  the form variable
     *
     * @throws Exception  problemos
     */
    public void initFormJS(Request request, Appendable js, String formVar)
            throws Exception {
        js.append(formVar + ".addService(new CDOTimeSeriesService());\n");
    }

    /**
     * Add the widgets for this service to the HTML form
     *
     * @param request   the request
     * @param input     the input
     * @param sb        the form
     * @param argPrefix the form prefix
     * @param label     a label for the form
     *
     *
     * @throws Exception problems
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
     * @param argPrefix  argument prefix
     *
     * @throws Exception  problem making stuff
     */
    private void makeInputForm(Request request, ServiceInput input,
                               Appendable sb, String argPrefix)
            throws Exception {

        String type =
            input.getProperty(
                "type", ClimateModelApiHandler.ARG_ACTION_COMPARE).toString();
        Entry first = input.getOperands().get(0).getEntries().get(0);

        CdmDataOutputHandler dataOutputHandler =
            getOutputHandler().getDataOutputHandler();
        GridDataset dataset =
            dataOutputHandler.getCdmManager().getGridDataset(first,
                first.getResource().getPath());

        if (dataset != null) {
            getOutputHandler().addVarLevelWidget(request, sb, dataset,
                    CdmDataOutputHandler.ARG_LEVEL);
        }
        GridDatatype grid  = dataset.getGrids().get(0);
        String       units = grid.getUnitsString();
        boolean hasPrecipUnits =
            (SimpleUnit.isCompatible(units, "kg m-2 s-1")
             || SimpleUnit.isCompatible(units, "mm/day"));

        boolean     isAnom    = first.getValue(request,3).toString().equals("anom");
        List<Entry> climos    = findClimatology(request, first);
        boolean     haveClimo = true;
        if ((climos == null) || climos.isEmpty()) {
            haveClimo = false;
        }

        //addStatsWidget(request, sb, input);
        addStatsWidget(request, sb, hasPrecipUnits, isAnom, haveClimo, input,
                       type);

        getOutputHandler().addTimeWidget(request, sb, dataset, true, true);

        addMapWidget(request, sb, dataset);

        if (dataset != null) {
            dataset.close();
        }
    }

    /**
     * Evaluate this service
     *
     * @param request  the request
     * @param input    the service input
     * @param argPrefix  the argument prefix
     *
     * @return  the evaluation
     *
     * @throws Exception problems
     */
    @Override
    public ServiceOutput evaluate(Request request, Object actionID,ServiceInput input,
                                  String argPrefix)
            throws Exception {

        if ( !canHandle(input)) {
            throw new Exception("Illegal data type");
        }
        long millis = System.currentTimeMillis();
        String type =
            input.getProperty(
                "type", ClimateModelApiHandler.ARG_ACTION_COMPARE).toString();
        List<ServiceOperand> outputEntries = evaluateInner(request, input,
                                                 argPrefix,
                                                 "CDOTimeSeriesStatistics",
                                                 type);


        return new ServiceOutput(outputEntries);
    }

    /**
     * @overrride
     *
     * @param entries list of entries
     *
     * @return true if valid
     */
    protected boolean checkForValidEntries(List<Entry> entries) {
        Entry firstEntry = entries.get(0);
        if ( !(firstEntry.getTypeHandler()
                instanceof ClimateModelFileTypeHandler)) {
            return false;
        }

        return true;
    }

    /**
     * Process the monthly request
     *
     * @param request  the request
     * @param dpi      the ServiceInput
     * @param op       the operand
     * @param opNum    the operand number
     * @param type     the request type
     * @param climSample  the climate sample file
     *
     * @return  some output
     *
     * @throws Exception Problem processing the monthly request
     */
    protected ServiceOperand evaluateMonthlyRequest(Request request,
            ServiceInput dpi, ServiceOperand op, int opNum, String type,
            Entry climSample)
            throws Exception {

        long  submillis = System.currentTimeMillis();
        Entry sample    = op.getEntries().get(0);
        if (climSample == null) {
            climSample = sample;
        }

        CdmDataOutputHandler dataOutputHandler =
            getOutputHandler().getDataOutputHandler();
        GridDataset dataset =
            dataOutputHandler.getCdmManager().getGridDataset(sample,
                getPath(request, sample));
        //oneOfThem.getResource().getPath());
        if ((dataset == null) || dataset.getGrids().isEmpty()) {
            throw new Exception("No grids found");
        }
        CalendarDateRange dateRange = dataset.getCalendarDateRange();
        dataOutputHandler.getCdmManager().returnGridDataset(getPath(request,
                sample), dataset);
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
        int    lastDataYear  = lastDataYearMM / 100;
        int    lastDataMonth = lastDataYearMM % 100;
        String varname = ((GridDatatype) dataset.getGrids().get(0)).getName();
        dataset.close();

        String tail =
            getOutputHandler().getStorageManager().getFileTail(sample);
        String id      = getRepository().getGUID();
        String newName = IOUtil.stripExtension(tail) + "_" + id + ".nc";
        newName = cleanName(newName);
        File outFile = new File(IOUtil.joinDir(dpi.getProcessDir(), newName));
        List<String> commands = initCDOService();
        Object[]     values   = sample.getValues(true);
        Object[]     avalues  = new Object[values.length + 1];
        System.arraycopy(values, 0, avalues, 0, values.length);
        
        // tell the world what we are doing
        Object actionId = dpi.getProperty("actionId", null);
        if (actionId != null) {
        	String name = ModelUtil.buildOutputName(request, values, opNum);
            getActionManager().setActionMessage(actionId, "Processing: " + name);
        }

        String  stat = request.getString(CDOOutputHandler.ARG_CDO_STAT);
        Entry   climEntry     = null;
        Entry   sprdEntry     = null;
        boolean isAnom        = sample.getValue(request,3).toString().equals("anom");
        String  climFileToUse = null;
        String climstartYear =
            request.getString(
                CDOOutputHandler.ARG_CDO_CLIM_STARTYEAR,
                ClimateModelApiHandler.DEFAULT_CLIMATE_START_YEAR);
        String climendYear =
            request.getString(
                CDOOutputHandler.ARG_CDO_CLIM_ENDYEAR,
                ClimateModelApiHandler.DEFAULT_CLIMATE_END_YEAR);
        if ( !isAnom
                && (stat.equals(CDOOutputHandler.STAT_ANOM)
                    || stat.equals(CDOOutputHandler.STAT_STDANOM)
                    || stat.equals(CDOOutputHandler.STAT_PCTANOM))) {
            climEntry = getClimatologyEntry(request, dpi, climSample,
                                            climstartYear, climendYear,
                                            CDOOutputHandler.PERIOD_MON);

            if (stat.equals(CDOOutputHandler.STAT_STDANOM)) {
                sprdEntry = getSpreadEntry(request, dpi, climSample,
                                           climstartYear, climendYear);
            }
        }

        // This kind of breaks the agnosticism of the data service, but if we are going to mask the data
        // then we need to remap the data to a 1x1 degree grid.  Actual masking gets done before plotting
        // If you want to keep the original resolution for non-masking, then uncomment
        //String maskType   = request.getString(NCLTimeSeriesPlotDataService.ARG_NCL_MASKTYPE, "none");

        if ( !doMonthsSpanYearEnd(request, sample)) {
            String statyear = "-yearmean";
            if (stat.equals(CDOOutputHandler.STAT_MAX)
               || stat.equals(CDOOutputHandler.STAT_MIN)) {
        	    statyear = "-year"+stat;
            }
            commands.add(statyear);
        }
        // Select order (left to right) - operations go right to left:
        //   - stats
        //   - level
        //   - region
        //   - month range
        //   - year or time range
        getOutputHandler().addStatServices(request, sample, commands);
        getOutputHandler().addAreaSelectServices(request, sample, commands);
        // If we want to use full resolution without mask, then uncomment here
        //if (!maskType.equals("none")) {
        getOutputHandler().addGridRemapServices(request, dpi, commands);
        commands.add("-selname," + varname);
        //}
        String  opStr       = getOpArgString(opNum);
        Request timeRequest = request;
        // Handle the case where the months span the year end (e.g. DJF)
        if (doMonthsSpanYearEnd(timeRequest, sample)) {
            submillis = System.currentTimeMillis();

            // find the start & end month of the request
            int requestStartMonth =
                timeRequest.get(CDOOutputHandler.ARG_CDO_STARTMONTH, 1);
            int requestEndMonth =
                timeRequest.get(CDOOutputHandler.ARG_CDO_ENDMONTH, 1);
            int totalMonths = ((12 - requestStartMonth) + 1)
                              + requestEndMonth;
            Request newRequest = timeRequest.cloneMe();

            // Stats  (timmean)
            /*
            getOutputHandler().addStatServices(newRequest, sample, commands);
            */

            // month average, max or min
            String timesel = "-timselmean";
            if (stat.equals(CDOOutputHandler.STAT_MAX)
               || stat.equals(CDOOutputHandler.STAT_MIN)) {
        	    timesel = "-timsel"+stat;
            }
            commands.add(timesel + "," + totalMonths);

            // date select to cover end of year
            int startYear = timeRequest.get(
                                CDOOutputHandler.ARG_CDO_STARTYEAR + opStr,
                                timeRequest.get(
                                    CDOOutputHandler.ARG_CDO_STARTYEAR,
                                    1979));
            int endYear = timeRequest.get(
                              CDOOutputHandler.ARG_CDO_ENDYEAR + opStr,
                              timeRequest.get(
                                  CDOOutputHandler.ARG_CDO_ENDYEAR, 1979));
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
            commands.add(getPath(request, sample));
            commands.add(outFile.toString());
            runCommands(commands, dpi.getProcessDir(), outFile);
            //System.out.println("subsetting data took: "+(System.currentTimeMillis()-submillis)+" ms");
            submillis = System.currentTimeMillis();

        } else {
            submillis = System.currentTimeMillis();
            getOutputHandler().addDateSelectServices(timeRequest, sample,
                    commands, opNum);
            getOutputHandler().addLevelSelectServices(timeRequest, sample,
                    commands, CdmDataOutputHandler.ARG_LEVEL);

            //System.err.println("cmds:" + commands);

            //commands.add(oneOfThem.getResource().getPath());
            commands.add(getPath(request, sample));
            commands.add(outFile.toString());
            runCommands(commands, dpi.getProcessDir(), outFile);
            //System.out.println("running cdo: "+(System.currentTimeMillis()-submillis)+" ms");
        }

        if (climEntry != null) {
            //String climName = IOUtil.stripExtension(tail) + "_" + id
            //                  + "_clim.nc";
            String climName = null;
            //if (true) {
            Object[] vals = climEntry.getValues();
            climName = vals[4] + "_" + vals[1] + "_" + vals[2] + "_"
                       + vals[3] + ".nc";
            climName = cleanName(climName);
            //} else {
            //    climName = IOUtil.stripExtension(tail) + "_" + id + "_clim.nc";
            //}
            File climFile = new File(IOUtil.joinDir(dpi.getProcessDir(),
                                climName));
            if ( !climFile.exists()) {
                commands = initCDOService();

                commands.add("-timmean");
                // Select order (left to right) - operations go right to left:
                //   - level
                //   - region
                //   - month range
                getOutputHandler().addStatServices(request, climEntry,
                        commands);
                getOutputHandler().addAreaSelectServices(request, climEntry,
                        commands);
                //if (!maskType.equals("none")) {
                getOutputHandler().addGridRemapServices(request, dpi,
                        commands);
                //}
                commands.add("-selname," + varname);
                getOutputHandler().addMonthSelectServices(request, climEntry,
                        commands);
                getOutputHandler().addLevelSelectServices(request, climEntry,
                        commands, CdmDataOutputHandler.ARG_LEVEL);

                commands.add(climEntry.getResource().getPath());
                commands.add(climFile.toString());
                //System.err.println("clim cmds:" + commands);
                runCommands(commands, dpi.getProcessDir(), climFile);
            }

            // now subtract them
            String anomSuffix = "anom";
            if (stat.equals(CDOOutputHandler.STAT_PCTANOM)) {
                anomSuffix = "pctanom";
            }
            String anomName = IOUtil.stripExtension(tail) + "_" + id + "_"
                              + anomSuffix + ".nc";
            anomName = cleanName(anomName);
            File anomFile = new File(IOUtil.joinDir(dpi.getProcessDir(),
                                anomName));
            commands = initCDOService();
            //commands.add("-ymonsub");
            // We use sub instead of ymonsub because there is only one value in each file and
            // CDO sets the time of the merged files to be the last time. 
            //commands.add("-ymonsub");
            if (stat.equals(CDOOutputHandler.STAT_PCTANOM)) {
                commands.add("-setunit,%");
                commands.add("-mulc,100");
                commands.add("-div");
            }
            commands.add("-sub");  // use sub because months might not line up
            commands.add(outFile.toString());
            commands.add(climFile.toString());
            if (stat.equals(CDOOutputHandler.STAT_PCTANOM)) {
                commands.add(climFile.toString());
            }
            commands.add(anomFile.toString());
            runCommands(commands, dpi.getProcessDir(), anomFile);
            outFile = anomFile;
            if ((sprdEntry != null)
                    && stat.equals(CDOOutputHandler.STAT_STDANOM)) {

                Request sprdRequest = request.cloneMe();

                String sprdName = IOUtil.stripExtension(tail) + "_" + id
                                  + "_stdanom.nc";
                sprdName = cleanName(sprdName);
                File sprdFile = new File(IOUtil.joinDir(dpi.getProcessDir(),
                                    sprdName));
                commands = initCDOService();
                commands.add("-setunit, ");
                commands.add("-div");
                commands.add(anomFile.toString());
                // Select order (left to right) - operations go right to left:
                //   - region
                //   - level
                //   - month range
                /*
                getOutputHandler().addStatServices(timeRequest, sprdEntry,
                        commands);
                        */
                commands.add("-timstd");
                getOutputHandler().addAreaSelectServices(timeRequest,
                        sprdEntry, commands);
                getOutputHandler().addGridRemapServices(timeRequest, dpi,
                        commands);
                commands.add("-selname," + varname);
                /*
                getOutputHandler().addMonthSelectServices(timeRequest,
                        sprdEntry, commands);
                */
                getOutputHandler().addLevelSelectServices(timeRequest,
                        sprdEntry, commands, CdmDataOutputHandler.ARG_LEVEL);
                //commands.add(sprdEntry.getResource().getPath());
                commands.add(getPath(request, sprdEntry));
                commands.add(sprdFile.toString());
                //System.err.println("std anom cmds:" + commands);
                runCommands(commands, dpi.getProcessDir(), sprdFile);
                outFile = sprdFile;
            }
        }

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
        outputName.append(values[4]);
        outputName.append(" ");
        outputName.append(stat);
        outputName.append(" ");

        StringBuilder dateSB  = new StringBuilder();
        String        yearNum = (opNum == 0)
                                ? ""
                                : String.valueOf(opNum + 1);

        int startMonth = request.defined(CDOOutputHandler.ARG_CDO_STARTMONTH)
                         ? request.get(CDOOutputHandler.ARG_CDO_STARTMONTH, 1)
                         : 1;
        int endMonth = request.defined(CDOOutputHandler.ARG_CDO_ENDMONTH)
                       ? request.get(CDOOutputHandler.ARG_CDO_ENDMONTH,
                                     startMonth)
                       : startMonth;
        if (startMonth == endMonth) {
            dateSB.append(MONTHS[startMonth - 1]);
        } else {
            dateSB.append(MONTHS[startMonth - 1]);
            dateSB.append("-");
            dateSB.append(MONTHS[endMonth - 1]);
        }
        dateSB.append(" ");
        String startYear = request.defined(CDOOutputHandler.ARG_CDO_STARTYEAR
                                           + yearNum)
                           ? request.getString(
                               CDOOutputHandler.ARG_CDO_STARTYEAR + yearNum)
                           : request.defined(
                               CDOOutputHandler.ARG_CDO_STARTYEAR)
                             ? request.getString(
                                 CDOOutputHandler.ARG_CDO_STARTYEAR, "")
                             : "";
        String endYear = request.defined(CDOOutputHandler.ARG_CDO_ENDYEAR
                                         + yearNum)
                         ? request.getString(CDOOutputHandler.ARG_CDO_ENDYEAR
                                             + yearNum)
                         : request.defined(CDOOutputHandler.ARG_CDO_ENDYEAR)
                           ? request.getString(
                               CDOOutputHandler.ARG_CDO_ENDYEAR, startYear)
                           : startYear;
        if (startYear.equals(endYear)) {
            dateSB.append(startYear);
        } else {
            dateSB.append(startYear);
            dateSB.append("-");
            dateSB.append(endYear);
        }
        avalues[5] = dateSB.toString();
        outputName.append(dateSB);


        //System.out.println("Name: " + outputName.toString());

        Resource resource = new Resource(outFile, Resource.TYPE_LOCAL_FILE);
        TypeHandler myHandler = getRepository().getTypeHandler("cdm_grid",
                                    true);
        //getRepository().getTypeHandler("type_single_point_grid_netcdf", true);
        Entry outputEntry = new Entry(myHandler, true,
                                      cleanName(outputName.toString()));
        outputEntry.setResource(resource);
        outputEntry.setValues(avalues);
        getOutputHandler().getEntryManager().writeEntryXmlFile(request,
                outputEntry);

        ServiceOperand newOp = new ServiceOperand(outputName.toString(),
                                   outputEntry);
        copyServiceOperandProperties(op, newOp);

        return newOp;

    }

    /**
     * Process the daily data request
     *
     * @param request  the request
     * @param dpi      the ServiceInput
     * @param op       the operand
     * @param opNum    the operand number
     * @param type     the type of request
     * @param climSample  a climate sample file
     *
     * @return  some output
     *
     * @throws Exception problem processing the daily data
     */
    protected ServiceOperand evaluateDailyRequest(Request request,
            ServiceInput dpi, ServiceOperand op, int opNum, String type,
            Entry climSample)
            throws Exception {
        throw new Exception("Can't handle daily data yet");
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
                "/org/ramadda/geodata/model/htdocs/model/help/tsstatistics.html");
        } catch (Exception excp) {}

        return null;
    }


}
