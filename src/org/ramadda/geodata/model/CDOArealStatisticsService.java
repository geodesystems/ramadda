/*
* Copyright (c) 2008-2019 Geode Systems LLC
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
import org.ramadda.repository.ApiMethod;
import org.ramadda.repository.Association;
import org.ramadda.repository.Entry;
import org.ramadda.repository.Repository;
import org.ramadda.repository.RepositoryManager;
import org.ramadda.repository.Request;
import org.ramadda.repository.RequestHandler;
import org.ramadda.repository.Resource;
import org.ramadda.repository.type.TypeHandler;
import org.ramadda.service.ServiceInput;
import org.ramadda.service.ServiceOperand;
import org.ramadda.service.ServiceOutput;
import org.ramadda.util.HtmlUtils;

import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.time.Calendar;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.units.SimpleUnit;

import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;

import ucar.visad.data.CalendarDateTime;


import java.io.File;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;


/**
 * Service for area statistics using CDO
 */
public class CDOArealStatisticsService extends CDODataService {

    /** the type of request */
    //String type = null;

    /**
     * Ordinal names for years
     */
    public static final String[] ordinalYears = {
        "First", "Second", "Third", "Fourth", "Fifth", "Sixth", "Seventh",
        "Eighth", "Ninth", "Tenth"
    };

    /**
     * Area statistics DataProcess
     *
     * @param repository  the Repository
     *
     * @throws Exception  problems
     */
    public CDOArealStatisticsService(Repository repository) throws Exception {
        super(repository, "CDO_AREA_STATS", "Area Statistics");
    }

    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public String getHelp() {
        try {
            return getStorageManager().readSystemResource(
                "/org/ramadda/geodata/model/htdocs/model/help/areastatistics.html");
        } catch (Exception excp) {}

        return null;
    }

    /**
     * Initialize the form javascript
     *
     * @param request  the request
     * @param js       the javascript
     * @param formVar  the form variable
     *
     * @throws Exception problems
     */
    public void initFormJS(Request request, Appendable js, String formVar)
            throws Exception {
        js.append(formVar
                  + ".addService(new CDOArealStatisticsService());\n");
    }


    /**
     * Add to form
     *
     * @param request  the Request
     * @param input    the ServiceInput
     * @param sb       the form
     * @param argPrefix  the argument prefix
     * @param label      the label
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
     * @param argPrefix  the argument prefix
     *
     * @throws Exception  problem making stuff
     */
    private void makeInputForm(Request request, ServiceInput input,
                               Appendable sb, String argPrefix)
            throws Exception {

        String type =
            input.getProperty(
                "type", ClimateModelApiHandler.ARG_ACTION_COMPARE).toString();

        input = adjustInput(request, input);
        List<NamedTimePeriod> periods = null;
        ApiMethod             api     = request.getApiMethod();
        if (api != null) {
            RequestHandler handler = api.getRequestHandler();
            if ((handler != null)
                    && (handler instanceof ClimateModelApiHandler)) {
                String group = null;
                if (request.defined(ClimateModelApiHandler.ARG_EVENT_GROUP)) {
                    group = request.getString(
                        ClimateModelApiHandler.ARG_EVENT_GROUP, null);
                    periods =
                        ((ClimateModelApiHandler) handler)
                            .getNamedTimePeriods(group);
                }
            }
        }

        Entry first = input.getEntries().get(0);

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

        boolean     isAnom    = first.getValue(3).toString().equals("anom");
        List<Entry> climos    = findClimatology(request, first);
        boolean     haveClimo = true;
        if ((climos == null) || climos.isEmpty()) {
            haveClimo = false;
        }
        addStatsWidget(request, sb, input, hasPrecipUnits, isAnom, haveClimo,
                       type);

        addTimeWidget(request, sb, input, periods);

        /*
        if (type.equals(ClimateModelApiHandler.ARG_ACTION_ENS_COMPARE)) {
            StringBuilder mysb = new StringBuilder();
            mysb.append(HtmlUtils.radio(ARG_TIME_AVERAGE,
                                    "true",
                                    RepositoryManager.getShouldButtonBeSelected(
                                        request,
                                        ARG_TIME_AVERAGE,
                                        "true",
                                        true)));
            mysb.append(HtmlUtils.space(1));
            mysb.append(Repository.msg("True"));
            mysb.append(HtmlUtils.space(2));
            boolean timeSelected =
                RepositoryManager.getShouldButtonBeSelected(request,
                    ARG_TIME_AVERAGE, "false", false);
            String anomRB = HtmlUtils.radio(ARG_TIME_AVERAGE,
                                            "false",
                                            timeSelected);

            mysb.append(anomRB);
            mysb.append(HtmlUtils.space(1));
            mysb.append(Repository.msg("False"));
            mysb.append(HtmlUtils.space(2));
            mysb.append(Repository.msg("(PDF Only)"));
            sb.append(HtmlUtils.formEntry(msgLabel("Composite"),mysb.toString()));
        }
        */

        LatLonRect llr = null;
        if (dataset != null) {
            llr = dataset.getBoundingBox();
        } else {
            llr = new LatLonRect(new LatLonPointImpl(90.0, -180.0),
                                 new LatLonPointImpl(-90.0, 180.0));
        }
        getOutputHandler().addMapWidget(request, sb, llr, false);
    }

    /**
     * Process the request
     *
     * @param request  The request
     * @param input  the  data process input
     * @param argPrefix the argument prefix
     *
     * @return  the processed data
     *
     * @throws Exception  problem processing
     */
    @Override
    public ServiceOutput evaluate(Request request, ServiceInput input,
                                  String argPrefix)
            throws Exception {

        long millis = System.currentTimeMillis();
        String type =
            input.getProperty(
                "type", ClimateModelApiHandler.ARG_ACTION_COMPARE).toString();

        List<ServiceOperand> outputEntries = evaluateInner(request, input,
                                                 argPrefix,
                                                 "CDOAeralStatistics", type);

        //System.err.println("Processing took: "+(System.currentTimeMillis() - millis)+" ms");

        return new ServiceOutput(outputEntries);
    }

    /**
     * Add the ensemble mean to list of output entries
     *
     * @param request the request
     * @param dpi     the service input
     * @param ops     the service operands
     * @param type    the type of request
     *
     * @throws Exception problems
     */
    public void addEnsembleMean(Request request, ServiceInput dpi,
                                List<ServiceOperand> ops, String type)
            throws Exception {
        Entry         oneOfThem = ops.get(0).getEntries().get(0);
        Object[]      values    = oneOfThem.getValues(true);
        StringBuilder fileName  = new StringBuilder();
        fileName.append(oneOfThem.getValue(4));
        if (type.equals(ClimateModelApiHandler.ARG_ACTION_MULTI_COMPARE)) {
            fileName.append("_MultiModel_");
        } else {
            fileName.append("_Ensemble_");
        }
        fileName.append(oneOfThem.getValue(2));
        fileName.append("_mean_");
        String id      = getRepository().getGUID();
        String newName = fileName + id + ".nc";
        /*
        String tail =
            getOutputHandler().getStorageManager().getFileTail(oneOfThem);
        String       id        = getRepository().getGUID();
        String       newName = IOUtil.stripExtension(tail) + "_MMM_" + id + ".nc";
        */
        File outFile = new File(IOUtil.joinDir(dpi.getProcessDir(), newName));
        List<String> commands = initCDOService();
        commands.add("-ensmean");
        for (ServiceOperand op : ops) {
            List<Entry> entries = op.getEntries();
            for (Entry e : entries) {
                //commands.add(e.getResource().getPath());
                commands.add(getPath(request, e));
            }
        }
        commands.add(outFile.toString());
        StringBuilder outputName = new StringBuilder();
        if (type.equals(ClimateModelApiHandler.ARG_ACTION_MULTI_COMPARE)) {
            outputName.append("Multi-Model Ensemble Mean");
        } else {
            outputName.append("Ensemble Mean");
        }
        //System.out.println("ens mean: " + commands);
        runCommands(commands, dpi.getProcessDir(), outFile);
        Resource resource = new Resource(outFile, Resource.TYPE_LOCAL_FILE);
        TypeHandler myHandler = getRepository().getTypeHandler("cdm_grid",
                                    true);
        //TypeHandler myHandler = oneOfThem.getTypeHandler();
        Entry outputEntry = new Entry(myHandler, true, outputName.toString());
        outputEntry.setResource(resource);
        Object[] newValues = values;
        if (type.equals(ClimateModelApiHandler.ARG_ACTION_MULTI_COMPARE)) {
            newValues[1] = "Multi-Model Mean";
        } else {
            newValues[1] = "Mean of Ensemble Members";
        }
        newValues[3] = "mean";
        outputEntry.setValues(newValues);
        // Add in lineage and associations
        outputEntry.addAssociation(new Association(getRepository().getGUID(),
                "generated product", "product generated from",
                oneOfThem.getId(), outputEntry.getId()));
        getOutputHandler().getEntryManager().writeEntryXmlFile(request,
                outputEntry);
        ServiceOperand newOp = new ServiceOperand(outputName.toString(),
                                   outputEntry);
        copyServiceOperandProperties(ops.get(0), newOp);
        ops.add(newOp);
    }

    /**
     * Process the monthly request
     *
     * @param request  the request
     * @param dpi      the ServiceInput
     * @param op       the operand
     * @param opNum    the operand number
     * @param type     the type of request
     * @param climSample _more_
     *
     * @return  some output
     *
     * @throws Exception Problem processing the monthly request
     * protected ServiceOperand evaluateMonthlyRequest(Request request,
     *       ServiceInput dpi, ServiceOperand op, int opNum, String type)
     *       throws Exception {
     *   return evaluateMonthlyRequest(request, dpi, op, opNum, type, null);
     * }
     */

    /**
     * Process the daily data request
     *
     * @param request  the request
     * @param dpi      the ServiceInput
     * @param op       the operand
     * @param opNum    the operand number
     * @param type     the type of request
     * @param climSample sample entry for finding climatology
     *
     * @return  some output
     *
     * @throws Exception problem processing the daily data
     */
    protected ServiceOperand evaluateDailyRequest(Request request,
            ServiceInput dpi, ServiceOperand op, int opNum, String type,
            Entry climSample)
            throws Exception {
        return evaluatePeriodRequest(request, dpi, op, opNum, type,
                                     climSample, CDOOutputHandler.PERIOD_DAY);
    }


    /**
     * Process the monthly request
     *
     * @param request  the request
     * @param dpi      the ServiceInput
     * @param op       the operand
     * @param opNum    the operand number
     * @param type     the type of request
     * @param climSample sample entry for finding climatology
     *
     * @return  some output
     *
     * @throws Exception Problem processing the monthly request
     */
    protected ServiceOperand evaluateMonthlyRequest(Request request,
            ServiceInput dpi, ServiceOperand op, int opNum, String type,
            Entry climSample)
            throws Exception {
        return evaluatePeriodRequest(request, dpi, op, opNum, type,
                                     climSample, CDOOutputHandler.PERIOD_MON);
    }

    /**
     * Process the time period request
     *
     * @param request  the request
     * @param dpi      the ServiceInput
     * @param op       the operand
     * @param opNum    the operand number
     * @param type     the type of request
     * @param climSample sample entry for finding climatology
     * @param period _more_
     *
     * @return  some output
     *
     * @throws Exception Problem processing the monthly request
     */
    protected ServiceOperand evaluatePeriodRequest(Request request,
            ServiceInput dpi, ServiceOperand op, int opNum, String type,
            Entry climSample, String period)
            throws Exception {

        //System.err.println("opNum = "+opNum);
        long         submillis = System.currentTimeMillis();
        List<Entry>  opEntries = op.getEntries();
        Entry        sample    = opEntries.get(0);
        List<String> commands  = null;
        if (op.getEntries().size() > 1) {
            String id = ModelUtil.makeValuesKey(sample.getValues(), true);
            sample = ModelUtil.aggregateEntriesByTime(request, opEntries, id,
                    dpi.getProcessDir());
        }
        if (climSample == null) {
            climSample = sample;
        }
        //sample = oneOfThem;
        CdmDataOutputHandler dataOutputHandler =
            getOutputHandler().getDataOutputHandler();
        GridDataset dataset =
            dataOutputHandler.getCdmManager().getGridDataset(sample,
                getPath(request, sample));
        if ((dataset == null) || dataset.getGrids().isEmpty()) {
            throw new Exception("No grids found");
        }
        String varname = ((GridDatatype) dataset.getGrids().get(0)).getName();

        Object[] values  = sample.getValues(true);
        Object[] avalues = new Object[values.length + 1];
        System.arraycopy(values, 0, avalues, 0, values.length);
        String tail =
            getOutputHandler().getStorageManager().getFileTail(sample);
        String  id        = getRepository().getGUID();
        String  newName   = IOUtil.stripExtension(tail) + "_" + id + ".nc";
        File outFile = new File(IOUtil.joinDir(dpi.getProcessDir(), newName));

        String  stat = request.getString(CDOOutputHandler.ARG_CDO_STAT);
        Entry   climEntry = null;
        Entry   sprdEntry = null;
        boolean isAnom    = values[3].toString().equals("anom");
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
                                            period);

            if (stat.equals(CDOOutputHandler.STAT_STDANOM)) {
                sprdEntry = getSpreadEntry(request, dpi, climSample,
                                           climstartYear, climendYear);
            }
        }

        String  opStr       = (opNum == 0)
                              ? ""
                              : "" + (opNum + 1);

        Request timeRequest = handleNamedTimePeriod(request, opStr);

        makeDataFile(request, dpi, sample, outFile, dataset, varname, opNum);
        if (dataset != null) {
            dataset.close();
        }

        submillis = System.currentTimeMillis();
        if (climEntry != null) {
            String climName = null;
            if (type.equals(ClimateModelApiHandler.ARG_ACTION_ENS_COMPARE)
                    || type.equals(
                        ClimateModelApiHandler.ARG_ACTION_COMPARE)) {
                Object[] vals = climEntry.getValues();
                climName = vals[4] + "_" + vals[1] + "_" + vals[2] + "_"
                           + vals[3] + ".nc";
            } else {
                climName = IOUtil.stripExtension(tail) + "_" + id
                           + "_clim.nc";
            }
            File climFile = new File(IOUtil.joinDir(dpi.getProcessDir(),
                                climName));
            if ( !climFile.exists()) {
                commands = initCDOService();

                // Select order (left to right) - operations go right to left:
                //   - region
                //   - level
                //   - month range
                getOutputHandler().addStatServices(timeRequest, climEntry,
                        commands);
                getOutputHandler().addAreaSelectServices(timeRequest,
                        climEntry, commands);
                commands.add("-remapbil,r360x180");
                commands.add("-selname," + varname);
                getOutputHandler().addMonthSelectServices(timeRequest,
                        climEntry, commands);
                getOutputHandler().addLevelSelectServices(timeRequest,
                        climEntry, commands, CdmDataOutputHandler.ARG_LEVEL);

                //System.err.println("clim select cmds:" + commands);

                //commands.add(climEntry.getResource().getPath());
                commands.add(getPath(request, climEntry));
                commands.add(climFile.toString());
                runCommands(commands, dpi.getProcessDir(), climFile);
            }

            // now subtract them
            String anomSuffix = "anom";
            if (stat.equals(CDOOutputHandler.STAT_PCTANOM)) {
                anomSuffix = "pctanom";
            }
            String anomName = IOUtil.stripExtension(tail) + "_" + id + "_"
                              + anomSuffix + ".nc";
            File anomFile = new File(IOUtil.joinDir(dpi.getProcessDir(),
                                anomName));
            commands = initCDOService();
            // We use sub instead of ymonsub because there is only one value in each file and
            // CDO sets the time of the merged files to be the last time. 
            //commands.add("-ymonsub");
            if (stat.equals(CDOOutputHandler.STAT_PCTANOM)) {
                commands.add("-setunit,%");
                commands.add("-mulc,100");
                commands.add("-div");
            }
            commands.add("-sub");
            commands.add(outFile.toString());
            commands.add(climFile.toString());
            if (stat.equals(CDOOutputHandler.STAT_PCTANOM)) {
                commands.add(climFile.toString());
            }
            commands.add(anomFile.toString());
            //System.err.println("making clim cmds:" + commands);
            runCommands(commands, dpi.getProcessDir(), anomFile);
            outFile = anomFile;
            if ((sprdEntry != null)
                    && stat.equals(CDOOutputHandler.STAT_STDANOM)) {

                Request sprdRequest = request.cloneMe();

                String sprdName = IOUtil.stripExtension(tail) + "_" + id
                                  + "_stdanom.nc";
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
                commands.add("-timstd");
                getOutputHandler().addAreaSelectServices(timeRequest,
                        sprdEntry, commands);
                commands.add("-remapbil,r360x180");
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
        //System.out.println("clim/sprd took: "+(System.currentTimeMillis()-submillis)+" ms");

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
        if (timeRequest.getString(
                CDOOutputHandler.ARG_CDO_MONTHS).equalsIgnoreCase("all")) {
            startMonth = 1;
            endMonth   = 12;
        } else {
            startMonth =
                timeRequest.defined(CDOOutputHandler.ARG_CDO_STARTMONTH)
                ? timeRequest.get(CDOOutputHandler.ARG_CDO_STARTMONTH, 1)
                : 1;
            endMonth = timeRequest.defined(CDOOutputHandler.ARG_CDO_ENDMONTH)
                       ? timeRequest.get(CDOOutputHandler.ARG_CDO_ENDMONTH,
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
        if (timeRequest.defined(CDOOutputHandler.ARG_CDO_YEARS + yearNum)) {
            dateSB.append(
                timeRequest.getString(
                    CDOOutputHandler.ARG_CDO_YEARS + yearNum));
        } else if (timeRequest.defined(CDOOutputHandler.ARG_CDO_YEARS)
                   && !(timeRequest.defined(
                       CDOOutputHandler.ARG_CDO_STARTYEAR
                       + yearNum) || timeRequest.defined(
                           CDOOutputHandler.ARG_CDO_ENDYEAR + yearNum))) {
            dateSB.append(
                timeRequest.getString(CDOOutputHandler.ARG_CDO_YEARS));
        } else {
            String startYear =
                timeRequest.defined(CDOOutputHandler.ARG_CDO_STARTYEAR
                                    + yearNum)
                ? timeRequest.getString(CDOOutputHandler.ARG_CDO_STARTYEAR
                                        + yearNum)
                : timeRequest.defined(CDOOutputHandler.ARG_CDO_STARTYEAR)
                  ? timeRequest.getString(CDOOutputHandler.ARG_CDO_STARTYEAR,
                                          "")
                  : "";
            String endYear =
                timeRequest.defined(CDOOutputHandler.ARG_CDO_ENDYEAR
                                    + yearNum)
                ? timeRequest.getString(CDOOutputHandler.ARG_CDO_ENDYEAR
                                        + yearNum)
                : timeRequest.defined(CDOOutputHandler.ARG_CDO_ENDYEAR)
                  ? timeRequest.getString(CDOOutputHandler.ARG_CDO_ENDYEAR,
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
        //System.err.println("evaluateMonthlyRequest took: "+(System.currentTimeMillis()-millis)+" ms");

        Resource resource = new Resource(outFile, Resource.TYPE_LOCAL_FILE);
        TypeHandler myHandler = getRepository().getTypeHandler("cdm_grid",
                                    true);
        //TypeHandler myHandler = sample.getTypeHandler();
        Entry outputEntry = new Entry(myHandler, true, outputName.toString());
        outputEntry.setResource(resource);
        outputEntry.setValues(avalues);
        // Add in lineage and associations
        outputEntry.addAssociation(new Association(getRepository().getGUID(),
                "generated product", "product generated from",
                sample.getId(), outputEntry.getId()));
        if (climEntry != null) {
            outputEntry.addAssociation(
                new Association(
                    getRepository().getGUID(), "generated product",
                    "product generated from", climEntry.getId(),
                    outputEntry.getId()));
        }
        getOutputHandler().getEntryManager().writeEntryXmlFile(timeRequest,
                outputEntry);

        ServiceOperand newOp = new ServiceOperand(outputName.toString(),
                                   outputEntry);
        copyServiceOperandProperties(op, newOp);

        return newOp;

    }


    /**
     * Make the data file
     *
     * @param request request
     * @param dpi     data input
     * @param sample  sample file
     * @param outFile output file
     * @param dataset gridded dataset
     * @param varname variable name
     * @param opNum   operator number
     *
     * @throws Exception problems
     */
    private void makeDataFile(Request request, ServiceInput dpi,
                              Entry sample, File outFile,
                              GridDataset dataset, String varname, int opNum)
            throws Exception {

        long    millis      = System.currentTimeMillis();
        long    submillis   = System.currentTimeMillis();
        String  opStr       = (opNum == 0)
                              ? ""
                              : "" + (opNum + 1);

        Request timeRequest = handleNamedTimePeriod(request, opStr);
        if ((dataset == null) || dataset.getGrids().isEmpty()) {
            throw new Exception("No grids found");
        }
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
        int          lastDataYear  = lastDataYearMM / 100;
        int          lastDataMonth = lastDataYearMM % 100;
        boolean      collapseTimes = request.get(ARG_TIME_AVERAGE, false);
        List<String> commands      = initCDOService();
        // Select order (left to right) - operations go right to left:
        //   - stats
        //   - region
        //   - variable
        //   - month range
        //   - year or time range
        //   - level   (putting this first speeds things up)
        if (collapseTimes) {
            getOutputHandler().addStatServices(request, sample, commands);
        }
        getOutputHandler().addAreaSelectServices(request, sample, commands);
        commands.add("-remapbil,r360x180");
        //getOutputHandler().addLevelSelectServices(request, sample,
        //        commands, CdmDataOutputHandler.ARG_LEVEL);
        commands.add("-selname," + varname);
        // Handle the case where the months span the year end (e.g. DJF)
        // Break it up into two requests
        int requestStartMonth =
            timeRequest.get(CDOOutputHandler.ARG_CDO_STARTMONTH, 1);
        int requestEndMonth =
            timeRequest.get(CDOOutputHandler.ARG_CDO_ENDMONTH, 1);
        int numMonths = requestEndMonth - requestStartMonth + 1;
        if (doMonthsSpanYearEnd(timeRequest, sample)) {
            numMonths = ((12 - requestStartMonth) + 1) + requestEndMonth;
            submillis = System.currentTimeMillis();
            //System.out.println("months span the year end");
            List<String> tmpFiles = new ArrayList<String>();
            boolean      haveYears;
            // find the start & end month of the request
            haveYears = timeRequest.defined(CDOOutputHandler.ARG_CDO_YEARS
                                            + opStr);
            if (haveYears) {
                List<Integer> years = new ArrayList<Integer>();
                String yearString = timeRequest.getString(
                                        CDOOutputHandler.ARG_CDO_YEARS
                                        + opStr, timeRequest.getString(
                                            CDOOutputHandler.ARG_CDO_YEARS,
                                            null));
                if (yearString != null) {
                    yearString = CDOOutputHandler.verifyYearsList(yearString);
                }
                List<String> yearList = StringUtil.split(yearString, ",",
                                            true, true);
                for (String year : yearList) {
                    int iyear = Integer.parseInt(year);
                    if ((iyear <= firstDataYear) || (iyear > lastDataYear)
                            || ((iyear == lastDataYear)
                                && (requestEndMonth > lastDataMonth))) {
                        continue;
                    }
                    years.add(Integer.parseInt(year));
                }
                for (int i = 0; i < 2; i++) {
                    List<String> savedServices = new ArrayList(commands);
                    Request      newRequest    = timeRequest.cloneMe();
                    newRequest.remove(CDOOutputHandler.ARG_CDO_STARTYEAR);
                    newRequest.remove(CDOOutputHandler.ARG_CDO_ENDYEAR);
                    newRequest.remove(CDOOutputHandler.ARG_CDO_STARTYEAR
                                      + opStr);
                    newRequest.remove(CDOOutputHandler.ARG_CDO_ENDYEAR
                                      + opStr);
                    if (i == 0) {  // last half of previous year
                        String yearsToUse = makeCDOYearsString(years, -1);
                        newRequest.put(CDOOutputHandler.ARG_CDO_ENDMONTH, 12);
                        newRequest.put(CDOOutputHandler.ARG_CDO_YEARS
                                       + opStr, yearsToUse);
                    } else {  // first half of current year
                        String yearsToUse = makeCDOYearsString(years, 0);
                        newRequest.put(CDOOutputHandler.ARG_CDO_STARTMONTH,
                                       1);
                        newRequest.put(CDOOutputHandler.ARG_CDO_YEARS
                                       + opStr, yearsToUse);
                    }
                    File tmpFile = new File(outFile.toString() + "." + i);
                    getOutputHandler().addDateSelectServices(newRequest,
                            sample, savedServices, opNum);
                    getOutputHandler().addLevelSelectServices(newRequest,
                            sample, savedServices,
                            CdmDataOutputHandler.ARG_LEVEL);
                    //System.err.println("years cmds:" + savedServices);
                    //savedServices.add(oneOfThem.getResource().getPath());
                    savedServices.add(getPath(request, sample));
                    savedServices.add(tmpFile.toString());
                    runCommands(savedServices, dpi.getProcessDir(), tmpFile);
                    tmpFiles.add(tmpFile.toString());
                }
                // merge the files together
                commands = initCDOService();
                commands.add("-mergetime");
                commands.add(tmpFiles.get(0).toString());
                commands.add(tmpFiles.get(1).toString());
                File timeFile = new File(outFile.toString() + ".merged");
                commands.add(timeFile.toString());
                //System.err.println("add cmds:" + commands);
                runCommands(commands, dpi.getProcessDir(), timeFile);
                // now create the mean of the times
                commands = initCDOService();
                if (collapseTimes) {
                    getOutputHandler().addStatServices(request, sample,
                            commands);
                }
                // month average
                commands.add("-timselmean," + numMonths);
                commands.add(timeFile.toString());
                commands.add(outFile.toString());
                //System.err.println("add cmds:" + commands);
                runCommands(commands, dpi.getProcessDir(), outFile);

            } else {
                Request newRequest = timeRequest.cloneMe();

                // month average
                commands.add("-timselmean," + numMonths);

                // date select to cover end of year
                int startYear = timeRequest.get(
                                    CDOOutputHandler.ARG_CDO_STARTYEAR
                                    + opStr, timeRequest.get(
                                        CDOOutputHandler.ARG_CDO_STARTYEAR,
                                        1979));
                int endYear = timeRequest.get(
                                  CDOOutputHandler.ARG_CDO_ENDYEAR + opStr,
                                  timeRequest.get(
                                      CDOOutputHandler.ARG_CDO_ENDYEAR,
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
                startyearString.append(StringUtil.padZero(requestStartMonth,
                        2));                            // startmonth
                startyearString.append("-");
                startyearString.append("01");  // startday
                startyearString.append("T00:00:00");  // starttime
                StringBuilder endyearString = new StringBuilder();
                endyearString.append(endYear);  //endyear
                endyearString.append("-");
                endyearString.append(StringUtil.padZero(requestEndMonth, 2));  // endmonth
                endyearString.append("-");
                endyearString
                    .append("" + CDOOutputHandler
                        .DAYS_PER_MONTH[requestEndMonth - 1]);  // endday
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
            }
            //System.out.println("subsetting data took: "+(System.currentTimeMillis()-submillis)+" ms");
            submillis = System.currentTimeMillis();

        } else {
            submillis = System.currentTimeMillis();
            if ( !collapseTimes) {
                commands.add("-timselmean," + numMonths);
            }
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

    }

    /**
     * Handle a named time period request
     *
     * @param request  the request
     * @param opStr the operator
     *
     * @return the answer
     */
    private Request handleNamedTimePeriod(Request request, String opStr) {
        if ( !request.defined(ClimateModelApiHandler.ARG_EVENT)) {
            return request;
        }
        Request newRequest = request.cloneMe();
        String eventString =
            newRequest.getString(ClimateModelApiHandler.ARG_EVENT);
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
     * Check for valid entries
     * @param entries  list of entries
     * @return
     */
    protected boolean checkForValidEntries(List<Entry> entries) {
        // TODO: change this when we can handle more than one entry (e.g. daily data)
        if (entries.isEmpty()) {
            //if (entries.isEmpty() || (entries.size() > 1)) {
            return false;
        }
        SortedSet<String> uniqueModels =
            Collections.synchronizedSortedSet(new TreeSet<String>());
        SortedSet<String> uniqueMembers =
            Collections.synchronizedSortedSet(new TreeSet<String>());
        for (Entry entry : entries) {
            if ( !(isClimateModelType(entry))) {
                return false;
            }
            uniqueModels.add(entry.getValue(1).toString());
            uniqueMembers.add(entry.getValue(3).toString());
        }
        // one model, one member
        if ((uniqueModels.size() == 1) && (uniqueMembers.size() == 1)) {
            return true;
        }
        // single model, multi-ensemble 
        if ((uniqueModels.size() == 1) && (uniqueMembers.size() > 1)) {
            return true;
        }
        // multi-model multi-ensemble - don't want to think about this
        if ((uniqueModels.size() > 1) && (uniqueMembers.size() > 1)) {
            return true;
        }

        return true;
    }

    /**
     * Add the statistics widget
     *
     * @param request  the Request
     * @param sb       the HTML
     * @param si       input parameters
     * @param usePct  use percentage
     * @param isAnom  is this an anomaly file
     * @param haveClimo  do we have a climo
     * @param type     the type of request
     * @throws Exception problems
     */
    public void addStatsWidget(Request request, Appendable sb,
                               ServiceInput si, boolean usePct,
                               boolean isAnom, boolean haveClimo, String type)
            throws Exception {

        if ( !(type.equals(ClimateModelApiHandler
                .ARG_ACTION_MULTI_TIMESERIES) || type
                    .equals(ClimateModelApiHandler.ARG_ACTION_TIMESERIES))) {
            sb.append(
                HtmlUtils.hidden(
                    CDOOutputHandler.ARG_CDO_PERIOD,
                    request.getString(
                        CDOOutputHandler.ARG_CDO_PERIOD,
                        CDOOutputHandler.PERIOD_TIM)));
        }
        super.addStatsWidget(request, sb, usePct, isAnom, haveClimo, si,
                             type);
    }

    /**
     * Add a time widget
     *
     * @param request  the Request
     * @param sb       the HTML page
     * @param input    the input
     * @param periods  the time periods
     *
     * @throws Exception  problem making datasets
     */
    public void addTimeWidget(Request request, Appendable sb,
                              ServiceInput input,
                              List<NamedTimePeriod> periods)
            throws Exception {

        String type =
            input.getProperty(
                "type", ClimateModelApiHandler.ARG_ACTION_COMPARE).toString();
        if ((periods == null) || (periods.isEmpty())) {
            CDOOutputHandler.makeMonthsWidget(request, sb, null);
            makeYearsWidget(request, sb, input, type);
        } else {
            makeEventsWidget(request, sb, periods, type);
        }
    }

    /**
     * Make a widget for named time periods
     *
     * @param request  the request
     * @param sb       the form
     * @param periods  the periods
     * @param type     the type of request
     *
     * @throws Exception problems
     */
    private void makeEventsWidget(Request request, Appendable sb,
                                  List<NamedTimePeriod> periods, String type)
            throws Exception {
        String group =
            request.getString(ClimateModelApiHandler.ARG_EVENT_GROUP, null);
        List<TwoFacedObject> values        = new ArrayList<TwoFacedObject>();
        NamedTimePeriod      selectedEvent = periods.get(0);
        String               event         = null;
        if (request.defined(ClimateModelApiHandler.ARG_EVENT)) {
            event = request.getString(ClimateModelApiHandler.ARG_EVENT);
        }
        for (NamedTimePeriod period : periods) {
            String value = period.getId() + ";" + period.getStartMonth()
                           + ";" + period.getEndMonth() + ";"
                           + period.getYears();
            TwoFacedObject item = new TwoFacedObject(period.getName(), value);
            values.add(item);
        }
        /*
        sb.append(HtmlUtils.hidden(CDOOutputHandler.ARG_CDO_STARTMONTH,
                                   selectedEvent.getStartMonth()));
        sb.append(HtmlUtils.hidden(CDOOutputHandler.ARG_CDO_ENDMONTH,
                                   selectedEvent.getEndMonth()));
        sb.append(HtmlUtils.hidden(CDOOutputHandler.ARG_CDO_YEARS,
                                   selectedEvent.getYears()));
        */
        if ((group == null) || group.equals("all")) {
            group = "Events";
        }
        sb.append(
            HtmlUtils.formEntry(
                Repository.msgLabel(group),
                HtmlUtils.select(
                    ClimateModelApiHandler.ARG_EVENT, values, event)));

    }

    /**
     * Add the year selection widget
     *
     * @param request  the Request
     * @param sb       the StringBuilder to add to
     * @param input    the service input
     * @param type     the type of request
     *
     * @throws Exception problems
     */
    private void makeYearsWidget(Request request, Appendable sb,
                                 ServiceInput input, String type)
            throws Exception {

        /* If we are doing a compare, we make widgets for each operand.  If we are doing
         * a multi compare, we make one widget from the intersection of all grids
         */
        //int numOps = input.getOperands().size();
        List<List<ServiceOperand>> sortedOps =
            ModelUtil.sortOperandsByCollection(request, input.getOperands());
        int numOps = sortedOps.size();
        boolean isCompare =
            type.equals(ClimateModelApiHandler.ARG_ACTION_COMPARE)
            || type.equals(ClimateModelApiHandler.ARG_ACTION_ENS_COMPARE);
        int numWidgets = !isCompare
                         ? 1
                         : numOps;
        int grid       = 0;
        CdmDataOutputHandler dataOutputHandler =
            getOutputHandler().getDataOutputHandler();
        List<List<String>> dataYears  = new ArrayList<List<String>>(numOps);
        int[]              datesIndex = new int[numWidgets];
        int                dateIdx    = 0;
        int                idx        = 0;
        for (List<ServiceOperand> ops : sortedOps) {
            datesIndex[idx] = dateIdx;
            for (ServiceOperand op : ops) {
                List<GridDataset> grids = new ArrayList<GridDataset>();
                for (Entry first : op.getEntries()) {
                    GridDataset dataset =
                        dataOutputHandler.getCdmManager().getGridDataset(
                            first, first.getResource().getPath());
                    if (dataset != null) {
                        grids.add(dataset);
                    }
                }
                List<String> opYears = new ArrayList<String>();
                for (GridDataset dataset : grids) {
                    List<CalendarDate> dates =
                        CdmDataOutputHandler.getGridDates(dataset);
                    if ( !dates.isEmpty() && (grid == 0)) {
                        CalendarDate cd  = dates.get(0);
                        Calendar     cal = cd.getCalendar();
                        if (cal != null) {
                            sb.append(
                                HtmlUtils.hidden(
                                    CdmDataOutputHandler.ARG_CALENDAR,
                                    request.getString(
                                        CdmDataOutputHandler.ARG_CALENDAR,
                                        cal.toString())));
                        }
                    }
                    SortedSet<String> uniqueYears =
                        Collections.synchronizedSortedSet(
                            new TreeSet<String>());
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
                    List<String> years = new ArrayList<String>(uniqueYears);
                    // TODO:  make a better list of years
                    if (years.isEmpty()) {
                        for (int i = 1979; i <= 2012; i++) {
                            years.add(String.valueOf(i));
                        }
                    }
                    opYears.addAll(years);
                    grid++;
                    dataset.close();
                }
                dataYears.add(opYears);
                dateIdx++;
            }
            idx++;
        }
        for (int wnum = 0; wnum < numWidgets; wnum++) {
            List<String> commonYears = new ArrayList<String>();
            if ( !isCompare) {
                for (int opNum = 0; opNum < dataYears.size(); opNum++) {
                    List<String> years = dataYears.get(opNum);
                    if (opNum == 0) {
                        commonYears.addAll(years);
                    } else {
                        commonYears.retainAll(years);
                    }
                }
            } else {
                commonYears.addAll(dataYears.get(datesIndex[wnum]));
            }

            String yearNum = (wnum == 0)
                             ? ""
                             : String.valueOf(wnum + 1);
            String yrLabel = "Start";
            if (numWidgets > 1) {
                if (wnum < ordinalYears.length) {
                    yrLabel = ordinalYears[wnum] + " Dataset:<br>Start";
                } else {
                    yrLabel = "Dataset " + (wnum + 1) + ":<br>Start";
                }
            }
            yrLabel = Repository.msgLabel(yrLabel);
            if ((wnum > 0) && isCompare) {
                commonYears.add(0, "");
            }
            int endIndex = 0;
            //int endIndex = (grid == 0)
            //               ? years.size() - 1
            //               : 0;
            StringBuilder yearsWidget = new StringBuilder();
            yearsWidget.append(yrLabel);
            yearsWidget.append(
                HtmlUtils.select(
                    CDOOutputHandler.ARG_CDO_STARTYEAR + yearNum,
                    commonYears,
                    request.getString(
                        CDOOutputHandler.ARG_CDO_STARTYEAR + yearNum,
                        request.getString(
                            CDOOutputHandler.ARG_CDO_STARTYEAR,
                            commonYears.get(0))), HtmlUtils.title(
                                "Select the starting year")));
            yearsWidget.append(HtmlUtils.space(3));
            yearsWidget.append(Repository.msgLabel("End"));
            yearsWidget.append(
                HtmlUtils.select(
                    CDOOutputHandler.ARG_CDO_ENDYEAR + yearNum, commonYears,
                    request.getString(
                        CDOOutputHandler.ARG_CDO_ENDYEAR + yearNum,
                        request.getString(
                            CDOOutputHandler.ARG_CDO_ENDYEAR,
                            commonYears.get(endIndex))), HtmlUtils.title(
                                "Select the ending year")));
            yearsWidget.append(HtmlUtils.p());
            yearsWidget.append(Repository.msgLabel("or List"));
            yearsWidget.append(
                HtmlUtils.input(
                    CDOOutputHandler.ARG_CDO_YEARS
                    + yearNum, request.getString(
                        CDOOutputHandler.ARG_CDO_YEARS
                        + yearNum, ""), 20, HtmlUtils.title(
                            "Input a set of years separated by commas (e.g. 1980,1983,2012)")));
            yearsWidget.append(HtmlUtils.space(2));
            yearsWidget.append(Repository.msg("(comma separated)"));

            sb.append(HtmlUtils.formEntry(Repository.msgLabel("Years"),
                                          yearsWidget.toString()));
        }

    }

}
