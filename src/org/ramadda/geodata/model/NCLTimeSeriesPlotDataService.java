/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.model;


import org.ramadda.data.services.NoaaPsdMonthlyClimateIndexTypeHandler;
import org.ramadda.geodata.cdmdata.CdmDataOutputHandler;
import org.ramadda.repository.Constants;
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

import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;

import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;


import java.io.File;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;


/**
 * Map plotting process using NCL
 */
public class NCLTimeSeriesPlotDataService extends NCLDataService {

    /** smoothing argument */
    protected static final String ARG_NCL_NAVE = ARG_NCL_PREFIX + "nave";

    /** averaging periods */
    private static final String[] periods = {
        "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "15", "20"
    };

    /** plot colors */
    private static final String[] lineColors = {
        "Gray", "Black", "Blue", "Pink", "Cyan", "Salmon", "Coral"
    };


    /**
     * Create a new map process
     *
     * @param repository  the repository
     *
     * @throws Exception  badness
     */
    public NCLTimeSeriesPlotDataService(Repository repository)
            throws Exception {
        this(repository, "NCLTimeSeries", "Plot Options");
    }

    /**
     * Create a new map process
     *
     * @param repository  the repository
     * @param id  an id for this process
     * @param label  a label for this process
     *
     * @throws Exception  problem creating process
     */
    public NCLTimeSeriesPlotDataService(Repository repository, String id,
                                        String label)
            throws Exception {
        super(repository, id, label);
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
                "/org/ramadda/geodata/model/htdocs/model/help/plotts.html");
        } catch (Exception excp) {}

        return null;
    }


    /**
     * Init the javascript for the form
     *
     * @param request  the request
     * @param js  the JavaScript form
     * @param formVar the form id
     *
     * @throws Exception problems
     */
    public void initFormJS(Request request, Appendable js, String formVar)
            throws Exception {
        js.append(formVar + ".addService(new NCLModelPlotService());\n");
    }


    /**
     * Add this process to the form
     *
     * @param request  the Request
     * @param input    the process input
     * @param sb       the form
     * @param argPrefix arg prefix
     * @param label     label
     *
     *
     * @throws Exception  problem getting the information for the form
     */
    @Override
    public void addToForm(Request request, ServiceInput input, Appendable sb,
                          String argPrefix, String label)
            throws Exception {

        String type =
            input.getProperty("type",
                              ClimateModelApiHandler
                                  .ARG_ACTION_MULTI_TIMESERIES).toString();
        boolean isTimeseries =
            type.equals(ClimateModelApiHandler.ARG_ACTION_TIMESERIES)
            || type.equals(
                ClimateModelApiHandler.ARG_ACTION_MULTI_TIMESERIES);
        sb.append(HtmlUtils.formTable());
        addImageFormatWidget(request, sb);
        Entry  first = input.getEntries().get(0);

        String units = "";

        CdmDataOutputHandler dataOutputHandler =
            nclOutputHandler.getDataOutputHandler();
        GridDataset dataset =
            dataOutputHandler.getCdmManager().getGridDataset(first,
                first.getResource().getPath());
        if (dataset != null) {
            List<GridDatatype> grids = dataset.getGrids();
            GridDatatype       grid  = grids.get(0);
            units = grid.getUnitsString();
        }

        String space1 = HtmlUtils.space(1);
        String space2 = HtmlUtils.space(1);

        String output = "timeseries";
        if (type.equals(ClimateModelApiHandler.ARG_ACTION_MULTI_TIMESERIES)) {
            output = "multitimeseries";
        }
        sb.append(HtmlUtils.hidden(ARG_NCL_OUTPUT, output));

        boolean onlyHaveOneEntry = (input.getOperands().size() == 1)
                                   && (input.getEntries().size() == 1);
        StringBuilder plotTypes = new StringBuilder();
        if ( !onlyHaveOneEntry) {
            plotTypes.append(
                HtmlUtils.radio(
                    NCLOutputHandler.ARG_NCL_PLOTTYPE, "shaded",
                    RepositoryManager.getShouldButtonBeSelected(
                        request, NCLOutputHandler.ARG_NCL_PLOTTYPE, "shaded",
                        true)));
            plotTypes.append(space1);
            plotTypes.append(Repository.msg("Shaded Area"));
            plotTypes.append(space2);
        }
        plotTypes.append(
            HtmlUtils.radio(
                NCLOutputHandler.ARG_NCL_PLOTTYPE, "lines",
                (RepositoryManager.getShouldButtonBeSelected(
                    request, NCLOutputHandler.ARG_NCL_PLOTTYPE, "lines",
                    onlyHaveOneEntry) || onlyHaveOneEntry)));
        plotTypes.append(space1);
        plotTypes.append(Repository.msg("Lines"));
        if ( !onlyHaveOneEntry) {
            plotTypes.append(space2);
            plotTypes.append(
                HtmlUtils.radio(
                    NCLOutputHandler.ARG_NCL_PLOTTYPE, "both",
                    RepositoryManager.getShouldButtonBeSelected(
                        request, NCLOutputHandler.ARG_NCL_PLOTTYPE, "both",
                        false)));
            plotTypes.append(space1);
            plotTypes.append(Repository.msg("Both"));
        }
        plotTypes.append(HtmlUtils.br());


        //sb.append(HtmlUtils.formEntry(Repository.msgLabel("Plot Type"),
        //                              plotTypes.toString()));

        StringBuilder shadeData = new StringBuilder();
        shadeData.append(Repository.msgLabel("Area"));
        shadeData.append(
            HtmlUtils.radio(
                NCLOutputHandler.ARG_NCL_DATATYPE, "sigma",
                RepositoryManager.getShouldButtonBeSelected(
                    request, NCLOutputHandler.ARG_NCL_DATATYPE, "sigma",
                    true)));
        shadeData.append(space1);
        shadeData.append(Repository.msg("Standard Deviation"));
        shadeData.append(space2);
        shadeData.append(
            HtmlUtils.radio(
                NCLOutputHandler.ARG_NCL_DATATYPE, "maxmin",
                RepositoryManager.getShouldButtonBeSelected(
                    request, NCLOutputHandler.ARG_NCL_DATATYPE, "maxmin",
                    false)));
        shadeData.append(space1);
        shadeData.append(Repository.msg("Max/Min"));
        if ( !onlyHaveOneEntry) {
            plotTypes.append(HtmlUtils.div(shadeData.toString(),
                                           HtmlUtils.cssClass("shade-data")));
        }
        sb.append(HtmlUtils.formEntry(Repository.msgLabel("Plot Type"),
                                      plotTypes.toString()));

        //sb.append(HtmlUtils.formEntry(Repository.msgLabel("Shaded Data"), shadeData.toString()));
        if ( !onlyHaveOneEntry) {
            sb.append(
                HtmlUtils.script(
                    "$('input[name=\"" + NCLOutputHandler.ARG_NCL_PLOTTYPE
                    + "\"]').on('change', function() {"
                    + "$('.shade-data').toggle(!(this.value === 'lines' && this.checked));"
                    + "}).change();"));
        }



        sb.append(
            HtmlUtils.formEntry(
                Repository.msgLabel("Color"),
                HtmlUtils.select(
                    ARG_NCL_LINECOLOR, lineColors,
                    request.getSanitizedString(ARG_NCL_LINECOLOR, "Gray"),
                    Integer.MAX_VALUE)));
        addUnitsWidget(request, units, sb);


        addDataMaskWidget(request, sb);

        // TODO:  For now, don't get value from request.  May not
        // be valid if variable changes.
        StringBuilder yaxisOpts = new StringBuilder();
        yaxisOpts.append(Repository.msg("Min"));
        yaxisOpts.append(HtmlUtils.makeLatLonInput(ARG_NCL_YMIN,
                ARG_NCL_YMIN, ""));
        yaxisOpts.append(HtmlUtils.space(2));
        yaxisOpts.append(Repository.msg("Max"));
        yaxisOpts.append(HtmlUtils.makeLatLonInput(ARG_NCL_YMAX,
                ARG_NCL_YMAX, ""));
        sb.append(HtmlUtils.formEntry(Repository.msgLabel("Y-Axis<br>Range"),
                                      yaxisOpts.toString()));
        sb.append(
            HtmlUtils.formEntry(
                Repository.msgLabel("Running<br>Average"),
                HtmlUtils.select(
                    ARG_NCL_NAVE, periods,
                    request.getSanitizedString(ARG_NCL_NAVE, "5"),
                    Integer.MAX_VALUE) + HtmlUtils.space(1) + "years"));

        sb.append(HtmlUtils.formTableClose());

    }

    /**
     * Process the request
     *
     * @param request  the request
     * @param input    the ServiceInput
     * @param argPrefix arg prefix
     *
     * @return  the output
     *
     * @throws Exception  problems generating the output
     */
    @Override
    public ServiceOutput evaluate(Request request, Object actionID,ServiceInput input,
                                  String argPrefix)
            throws Exception {

        List<Entry>          outputEntries = new ArrayList<Entry>();
        List<Entry>          allEntries    = new ArrayList<Entry>();
        List<ServiceOperand> ops           = input.getOperands();
        StringBuilder        fileList      = new StringBuilder();
        StringBuilder        nameList      = new StringBuilder();
        StringBuilder        modelList     = new StringBuilder();
        StringBuilder        ensList       = new StringBuilder();
        StringBuilder        expList       = new StringBuilder();
        StringBuilder        dateList      = new StringBuilder();
        Entry                inputEntry    = input.getEntries().get(0);

        for (ServiceOperand op : ops) {
            List<Entry> opEntries = op.getEntries();
            for (Entry entry : opEntries) {
                allEntries.add(entry);
            }
        }
        if (ops.size() > 2) {
            // Sort the entries which may be out of order from threading
            allEntries = getEntryUtil().sortEntriesOnName(allEntries, false);
        }

        boolean haveOne = false;
        for (Entry entry : allEntries) {
            if (haveOne) {
                fileList.append(",");
                nameList.append(";");
                //}
                //if (haveGrid) {
                modelList.append(";");
                expList.append(";");
                ensList.append(";");
                dateList.append(";");
            }
            //fileList.append("\"");
            fileList.append(entry.getResource().toString());
            nameList.append(entry.getName());
            //if (isGridEntry(entry)) {
            modelList.append(entry.getValue(request,1));
            expList.append(entry.getValue(request,2));
            ensList.append(entry.getValue(request,3));
            dateList.append(entry.getValue(request,5));
            //}
            //fileList.append("\"");
            haveOne = true;
        }

        String wksName = getRepository().getGUID();
        String plotType =
            request.getString(NCLOutputHandler.ARG_NCL_PLOTTYPE, "lines");
        String shadeType =
            request.getString(NCLOutputHandler.ARG_NCL_DATATYPE, "sigma");
        String lineColor   = request.getString(ARG_NCL_LINECOLOR, "Gray");
        String imageFormat = request.getString(ARG_NCL_IMAGEFORMAT, "gif");
        String suffix      = imageFormat;
        String outputType  = request.getString(ARG_NCL_OUTPUT, "comp");
        String maskType    = request.getString(ARG_NCL_MASKTYPE, "none");
        File outFile = new File(IOUtil.joinDir(input.getProcessDir(),
                           wksName) + "." + suffix);
        File displayFile = new File(IOUtil.joinDir(input.getProcessDir(),
                               wksName) + ".gif");
        CdmDataOutputHandler dataOutputHandler =
            nclOutputHandler.getDataOutputHandler();
        GridDataset dataset =
            dataOutputHandler.getCdmManager().getGridDataset(inputEntry,
                inputEntry.getResource().toString());
        if (dataset == null) {
            throw new Exception("Not a grid");
        }

        StringBuilder commandString = new StringBuilder();
        List<String>  commands      = new ArrayList<String>();
        String        ncargRoot     = nclOutputHandler.getNcargRootDir();
        commands.add(IOUtil.joinDir(ncargRoot, "bin/ncl"));
        commands.add("-Q");
        commands.add(
            IOUtil.joinDir(
                IOUtil.joinDir(
                    nclOutputHandler.getStorageManager().getResourceDir(),
                    "ncl"), NCLOutputHandler.SCRIPT_TSPLOT));
        Map<String, String> envMap = new HashMap<String, String>();
        nclOutputHandler.addGlobalEnvVars(envMap);
        envMap.put("wks_name", wksName);
        envMap.put("ncfiles", fileList.toString());
        envMap.put("titles", nameList.toString());
        envMap.put("models", modelList.toString());
        envMap.put("exps", expList.toString());
        envMap.put("ens", ensList.toString());
        envMap.put("dates", dateList.toString());
        envMap.put("productdir", input.getProcessDir().toString());
        envMap.put("plot_type", plotType);
        envMap.put("shade_data", shadeType);
        envMap.put("image_format", imageFormat);
        envMap.put("output", outputType);
        envMap.put("mask", maskType);
        envMap.put("linecolor", lineColor);

        Hashtable    args     = request.getArgs();
        List<String> varNames = new ArrayList<String>();
        for (Enumeration keys = args.keys(); keys.hasMoreElements(); ) {
            String arg = (String) keys.nextElement();
            if (arg.startsWith(CdmDataOutputHandler.VAR_PREFIX)
                    && request.get(arg, false)) {
                varNames.add(
                    arg.substring(CdmDataOutputHandler.VAR_PREFIX.length()));
            }
        }
        String varname = request.getString(NCLOutputHandler.ARG_NCL_VARIABLE,
                                           null);
        if (varname == null) {
            List<GridDatatype> grids = dataset.getGrids();
            GridDatatype       var   = grids.get(0);
            varname = var.getName();
        }
        envMap.put("variable", varname);
        String level = request.getString(CdmDataOutputHandler.ARG_LEVEL,
                                         null);
        // add in the level units
        if ((level != null) && !level.isEmpty()) {
            envMap.put(CdmDataOutputHandler.ARG_LEVEL, level);
            List<GridDatatype> grids = dataset.getGrids();
            GridDatatype       var   = grids.get(0);
            GridCoordSystem    gcs   = var.getCoordinateSystem();
            CoordinateAxis1D   zAxis = gcs.getVerticalAxis();
            if (zAxis != null) {
                String zUnits = zAxis.getUnitsString();
                if (zUnits != null) {
                    envMap.put("levelunits", zUnits);
                }
            }
        }
        LatLonRect llb = dataset.getBoundingBox();
        // Normalize longitude bounds to the data
        double origLonMin = llb.getLonMin();
        double lonMin = Double.parseDouble(
                            request.getString(
                                NCLOutputHandler.ARG_NCL_AREA_WEST,
                                String.valueOf(llb.getLonMin())));
        double lonMax = Double.parseDouble(
                            request.getString(
                                NCLOutputHandler.ARG_NCL_AREA_EAST,
                                String.valueOf(llb.getLonMax())));
        String minLon = Misc.formatLatOrLon(lonMin, "DD.dH", false, false);
        String maxLon = Misc.formatLatOrLon(lonMax, "DD.dH", false, false);
        /*
        if (origLonMin < 0) {  // -180 to 180
            lonMin = GeoUtils.normalizeLongitude(lonMin);
            lonMax = GeoUtils.normalizeLongitude(lonMax);
        } else {               // 0-360
            lonMin = GeoUtils.normalizeLongitude360(lonMin);
            lonMax = GeoUtils.normalizeLongitude360(lonMax);
        }
        */
        double latMax = Double.parseDouble(
                            request.getString(
                                NCLOutputHandler.ARG_NCL_AREA_NORTH,
                                String.valueOf(llb.getLatMax())));
        String maxLat = Misc.formatLatOrLon(latMax, "DD.dH", true, false);
        double latMin = Double.parseDouble(
                            request.getString(
                                NCLOutputHandler.ARG_NCL_AREA_SOUTH,
                                String.valueOf(llb.getLatMin())));
        String minLat = Misc.formatLatOrLon(latMin, "DD.dH", true, false);

        envMap.put("minLat", String.valueOf(latMin));
        envMap.put("maxLat", String.valueOf(latMax));
        envMap.put("minLon", String.valueOf(lonMin));
        envMap.put("maxLon", String.valueOf(lonMax));
        String region = "Lat: " + minLat + "-" + maxLat + " Lon: " + minLon
                        + "-" + maxLon;
        envMap.put("region", region);


        // yaxis
        double ymin = request.get(ARG_NCL_YMIN, 0.);
        double ymax = request.get(ARG_NCL_YMAX, 0.);
        envMap.put("ymin", String.valueOf(ymin));
        envMap.put("ymax", String.valueOf(ymax));

        envMap.put("nave", request.getString(ARG_NCL_NAVE, "5"));

        boolean haveAnom = fileList.toString().indexOf("anom") >= 0;

        String climstartYear =
            request.getString(
                CDOOutputHandler.ARG_CDO_CLIM_STARTYEAR,
                ClimateModelApiHandler.DEFAULT_CLIMATE_START_YEAR);
        String climendYear =
            request.getString(
                CDOOutputHandler.ARG_CDO_CLIM_ENDYEAR,
                ClimateModelApiHandler.DEFAULT_CLIMATE_END_YEAR);
        envMap.put("climyears",
                   ModelUtil.buildClimateYearsString(climstartYear,
                       climendYear, "-"));

        String anomType = "anom";
        if (fileList.toString().indexOf("pctanom") >= 0) {
            anomType = "pctanom";
        } else if (fileList.toString().indexOf("stdanom") >= 0) {
            anomType = "stdanom";
        }
        envMap.put("anomtype", anomType);

        // gotta be a better way to do this
        boolean shouldConvertUnits = true;
        //!(anomType.equals("stdanom") || anomType.equals("pctanom"));
        String outUnits = request.getString(ARG_NCL_UNITS, null);
        if ((outUnits != null) && !outUnits.isEmpty() && shouldConvertUnits) {
            envMap.put("units", outUnits);
        }

        envMap.put("anom", Boolean.toString(haveAnom));
        envMap.put("annotation",
                   getRepository().getProperty("ramadda.model.sitename", ""));
        String logo = getRepository().getProperty(Constants.PROP_LOGO_IMAGE,
                          "");
        if ( !logo.isEmpty()) {
            if ( !logo.startsWith("http")) {
                if ( !logo.startsWith("/")) {
                    logo = "/" + logo;
                }
                logo = request.getAbsoluteUrl(logo);
            }
            envMap.put("logo", logo);
        }
        dataset.close();

        runCommands(commands, envMap, input.getProcessDir(), outFile);

        String      outType   = "type_image";
        Resource    resource = new Resource(outFile,
                                            Resource.TYPE_LOCAL_FILE);
        TypeHandler myHandler = getRepository().getTypeHandler(outType, true);
        Entry outputEntry     = new Entry(myHandler, true,
                                          outFile.toString());
        outputEntry.setResource(resource);
        nclOutputHandler.getEntryManager().writeEntryXmlFile(request,
                outputEntry);
        outputEntries.add(outputEntry);
        // add a GIF file for the entry.
        if ( !outFile.getPath().equals(displayFile.getPath())) {
            Resource displayresource = new Resource(displayFile,
                                           Resource.TYPE_LOCAL_FILE);
            TypeHandler displayHandler =
                getRepository().getTypeHandler(outType, true);
            Entry displayEntry = new Entry(displayHandler, true,
                                           displayFile.toString());
            displayEntry.setResource(displayresource);
            nclOutputHandler.getEntryManager().writeEntryXmlFile(request,
                    displayEntry);
            //outputEntries.add(displayEntry);
        }
        ServiceOutput dpo = new ServiceOutput(new ServiceOperand("Plot of "
                                + nameList, outputEntries));

        return dpo;

    }

    /**
     * Can we handle this type of ServiceInput?
     *
     * @param dpi  the ServiceInput
     * @return true if we can handle
     */
    public boolean canHandle(ServiceInput dpi) {
        if ( !nclOutputHandler.isEnabled()) {
            return false;
        }

        //if (dpi.getOperands().size() > 2) {
        //    return false;
        //}

        for (ServiceOperand op : dpi.getOperands()) {
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
    private boolean checkForValidEntries(List<Entry> entries) {
        // TODO: change this when we can handle more than one entry (e.g. daily data)
        if (entries.isEmpty()) {
            //if (entries.isEmpty() || (entries.size() > 1)) {
            return false;
        }
        SortedSet<String> uniqueModels =
            Collections.synchronizedSortedSet(new TreeSet<String>());
        SortedSet<String> uniqueMembers =
            Collections.synchronizedSortedSet(new TreeSet<String>());
	Request request = getAdminRequest();
        for (Entry entry : entries) {
            if ( !(entry.getTypeHandler()
                    instanceof ClimateModelFileTypeHandler)) {
                if (entry.getTypeHandler()
                        instanceof NoaaPsdMonthlyClimateIndexTypeHandler) {
                    continue;
                }

                return false;
            }
            uniqueModels.add(entry.getValue(request,1).toString());
            uniqueMembers.add(entry.getValue(request,3).toString());
        }
        // one model, one member
        if ((uniqueModels.size() == 1) && (uniqueMembers.size() == 1)) {
            return true;
        }
        // multi-model multi-ensemble - don't want to think about this
        if ((uniqueModels.size() >= 1) && (uniqueMembers.size() > 1)) {
            return false;
        }
        // single model, multi-ensemble - can't handle yet
        if ((uniqueModels.size() > 1) && (uniqueMembers.size() > 1)) {
            return true;
        }

        return true;
    }

    /**
     * Is this enabled?
     *
     * @return true if it is
     */
    public boolean isEnabled() {
        return nclOutputHandler.isEnabled();
    }

}
