/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.model;


import org.ramadda.data.services.NoaaPsdMonthlyClimateIndexTypeHandler;
import org.ramadda.geodata.cdmdata.CdmDataOutputHandler;
import org.ramadda.geodata.cdmdata.GridTypeHandler;
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
import org.ramadda.util.geo.GeoUtils;

import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;

import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;


import java.io.File;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;


/**
 * Map plotting process using NCL
 */
@SuppressWarnings("unchecked")
public class NCLModelPlotDataService extends NCLDataService {

    /** list of colormaps */
    private List colormaps = null;

    /** argument for plotting means on PDFs */
    private final static String ARG_INCLUDE_STAT = ARG_NCL_PREFIX
                                                   + "plot_stat";

    /** argument for plotting lat/lon lines on maps */
    private final static String ARG_LATLONLINES = ARG_NCL_PREFIX
                                                  + "latlonlines";

    /** argument for plotting ensemble mean on maps */
    private final static String ARG_ENSAVG = ARG_NCL_PREFIX + "ensavg";

    /** argument for plotting value ticks (rug) on PDFs */
    private final static String ARG_SHOW_VALUE_TICKS = ARG_NCL_PREFIX
                                                       + "show_rug";

    /** path to ncl */
    private String nclPath;
    
    /**
     * Create a new map process
     *
     * @param repository  the repository
     *
     * @throws Exception  badness
     */
    public NCLModelPlotDataService(Repository repository) throws Exception {
        this(repository, "NCLPlot", "Plot Options");
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
    public NCLModelPlotDataService(Repository repository, String id,
                                   String label)
            throws Exception {
        super(repository, id, label);
        nclPath = IOUtil.joinDir(nclOutputHandler.getNcargRootDir(), "bin/ncl");
        nclPath = getRepository().getScriptPath("ramadda.ncl.path",nclPath);
    }

    /**
     * get the help for this service
     *
     * @return the help
     */
    @Override
    public String getHelp() {
        try {
            return getStorageManager().readSystemResource(
                "/org/ramadda/geodata/model/htdocs/model/help/plot.html");
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
            input.getProperty(
                "type", ClimateModelApiHandler.ARG_ACTION_COMPARE).toString();
        boolean handleMultiple =
            type.equals(ClimateModelApiHandler.ARG_ACTION_MULTI_COMPARE)
            || type.equals(ClimateModelApiHandler.ARG_ACTION_ENS_COMPARE)
            || type.equals(ClimateModelApiHandler.ARG_ACTION_CORRELATION);
        boolean isCorrelation =
            type.equals(ClimateModelApiHandler.ARG_ACTION_CORRELATION);
        //List<Entry> entries = input.getEntries();
        Entry first = getFirstGridEntry(input);
        /*
        for (Entry entry : entries) {
            TypeHandler mytype = entry.getTypeHandler();
            if (mytype instanceof ClimateModelFileTypeHandler ||
                mytype instanceof GridTypeHandler) {
                first = entry;
                break;
            }
        }
        */

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
            dataset.close();
        }

        //System.out.println("Units: "+units);
        //        String space1 = HtmlUtils.space(1);
        //        String space2 = HtmlUtils.space(2);

        sb.append(HtmlUtils.formTable());
        addImageFormatWidget(request, sb);

        if ((input.getOperands().size() > 1) && !handleMultiple
                && !isCorrelation) {
            StringBuilder buttons = new StringBuilder();
            buttons.append(
                HtmlUtils.radio(
                    ARG_NCL_OUTPUT, "diff",
                    RepositoryManager.getShouldButtonBeSelected(
                        request, ARG_NCL_OUTPUT, "diff", true)));
            buttons.append(space1);
            buttons.append(Repository.msg("Difference"));
            buttons.append(space2);
            buttons.append(
                HtmlUtils.radio(
                    ARG_NCL_OUTPUT, "comp",
                    RepositoryManager.getShouldButtonBeSelected(
                        request, ARG_NCL_OUTPUT, "comp", false)));
            buttons.append(space1);
            buttons.append(Repository.msg("Separate Plots"));

            sb.append(HtmlUtils.formEntry(Repository.msgLabel("Plot As"),
                                          buttons.toString()));
        } else if (isCorrelation) {
            StringBuilder buttons = new StringBuilder();
            buttons.append(
                HtmlUtils.radio(
                    ARG_NCL_OUTPUT, "correlation",
                    RepositoryManager.getShouldButtonBeSelected(
                        request, ARG_NCL_OUTPUT, "correlation", true)));
            buttons.append(space1);
            buttons.append(Repository.msg("Correlation"));
            buttons.append(space2);
            buttons.append(
                HtmlUtils.radio(
                    ARG_NCL_OUTPUT, "regression",
                    RepositoryManager.getShouldButtonBeSelected(
                        request, ARG_NCL_OUTPUT, "regression", false)));
            buttons.append(space1);
            buttons.append(Repository.msg("Regression"));

            sb.append(HtmlUtils.formEntry(Repository.msgLabel("Plot As"),
                                          buttons.toString()));
        } else {
            String output = "comp";
            if (type.equals(
                    ClimateModelApiHandler.ARG_ACTION_MULTI_COMPARE)) {
                output = "multicomp";
            } else if (type.equals(
                    ClimateModelApiHandler.ARG_ACTION_ENS_COMPARE)) {
                output = "enscomp";
            }
            sb.append(HtmlUtils.hidden(ARG_NCL_OUTPUT, output));
        }
        if ( !isCorrelation) {
            if (type.equals(ClimateModelApiHandler.ARG_ACTION_ENS_COMPARE)) {

                if ( !ModelUtil.getFrequency(request, first).equals(
                        CDOOutputHandler.FREQUENCY_DAILY)) {
                    StringBuilder plotTypes = new StringBuilder();
                    plotTypes.append(
                        HtmlUtils.radio(
                            NCLOutputHandler.ARG_NCL_PLOTTYPE, "pdf",
                            RepositoryManager.getShouldButtonBeSelected(
                                request, NCLOutputHandler.ARG_NCL_PLOTTYPE,
                                "pdf", true)));
                    plotTypes.append(space1);
                    plotTypes.append(Repository.msg("Histogram"));
                    plotTypes.append(space2);
                    plotTypes.append(
                        HtmlUtils.radio(
                            NCLOutputHandler.ARG_NCL_PLOTTYPE, "image",
                            RepositoryManager.getShouldButtonBeSelected(
                                request, NCLOutputHandler.ARG_NCL_PLOTTYPE,
                                "image", false)));
                    plotTypes.append(space1);
                    plotTypes.append(Repository.msg("Maps"));

                    StringBuilder yearsSB = new StringBuilder();
                    yearsSB.append(Repository.msgLabel("Plot"));
                    yearsSB.append(HtmlUtils.space(1));
                    yearsSB.append(
                        HtmlUtils.radio(
                            ARG_TIME_AVERAGE, "true",
                            RepositoryManager.getShouldButtonBeSelected(
                                request, ARG_TIME_AVERAGE, "true", true)));
                    yearsSB.append(HtmlUtils.space(1));
                    yearsSB.append(Repository.msg("Average"));
                    yearsSB.append(HtmlUtils.space(2));
                    boolean timeSelected =
                        RepositoryManager.getShouldButtonBeSelected(request,
                            ARG_TIME_AVERAGE, "false", false);
                    String anomRB = HtmlUtils.radio(ARG_TIME_AVERAGE,
                                        "false", timeSelected);

                    yearsSB.append(anomRB);
                    yearsSB.append(HtmlUtils.space(1));
                    yearsSB.append(Repository.msg("Individual Years"));
                    plotTypes.append(HtmlUtils.div(yearsSB.toString(),
                            HtmlUtils.id("comp-years")));

                    sb.append(
                        HtmlUtils.formEntry(
                            Repository.msgLabel("Plot Type"),
                            plotTypes.toString()));
                } else {
                    sb.append(
                        HtmlUtils.hidden(
                            NCLOutputHandler.ARG_NCL_PLOTTYPE, "pdf"));
                    sb.append(HtmlUtils.hidden(ARG_TIME_AVERAGE, "false"));
                }
            } else {
                sb.append(HtmlUtils.hidden(NCLOutputHandler.ARG_NCL_PLOTTYPE,
                                           "image"));
            }

            addUnitsWidget(request, units, sb);
        }


        addDataMaskWidget(request, sb);
        sb.append(HtmlUtils.formTableClose());

        if ( !(type.equals(ClimateModelApiHandler.ARG_ACTION_ENS_COMPARE)
                && ModelUtil.getFrequency(request, first).equals(
                    CDOOutputHandler.FREQUENCY_DAILY))) {
            StringBuilder plotOpts = new StringBuilder();
            plotOpts.append(HtmlUtils.formTable());
            plotOpts.append(
                HtmlUtils.formEntry(
                    Repository.msgLabel("Overlays"),
                    HtmlUtils.labeledCheckbox(
                        ARG_LATLONLINES, "true",
                        request.get(ARG_LATLONLINES, false),
                        "Lat/Lon Lines")));
            if (handleMultiple) {
                String ensavgLabel = "Ensemble Mean";
                if (type.equals(
                        ClimateModelApiHandler.ARG_ACTION_MULTI_COMPARE)) {
                    ensavgLabel = "Multi-Model Mean";
                } else if (type.equals(
                        ClimateModelApiHandler.ARG_ACTION_CORRELATION)) {
                    ensavgLabel = "Average";
                }
                plotOpts.append(
                    HtmlUtils.formEntry(
                        Repository.msgLabel("Other"),
                        HtmlUtils.labeledCheckbox(
                            ARG_ENSAVG, "true",
                            request.get(ARG_ENSAVG, false),
                            "Create " + ensavgLabel + " Plot")));
            }

            // TODO:  For now, don't get value from request.  May not
            // be valid if variable changes.
            // Contour options
            StringBuilder contourOpts = new StringBuilder();
            contourOpts.append(HtmlUtils.labeledCheckbox(ARG_NCL_CFILL,
                    "true", request.get(ARG_NCL_CFILL, true), "Color-fill"));
            contourOpts.append(HtmlUtils.space(3));
            contourOpts.append(HtmlUtils.labeledCheckbox(ARG_NCL_CLINES,
                    "true", request.get(ARG_NCL_CLINES, false), "Lines"));
            contourOpts.append(HtmlUtils.space(3));
            contourOpts.append(HtmlUtils.labeledCheckbox(ARG_NCL_CLABELS,
                    "true", request.get(ARG_NCL_CLABELS, false), "Labels"));
            // Contour interval
            //StringBuilder contourSB = new StringBuilder();
            //contourOpts.append("</p>");
            contourOpts.append("</br>");
            contourOpts.append(
                HtmlUtils.bold(
                    Repository.msg("Override Contour Interval Defaults:")));
            contourOpts.append("<br>");
            contourOpts.append(Repository.msg("Interval: "));
            contourOpts.append(HtmlUtils.makeLatLonInput(ARG_NCL_CINT,
                    ARG_NCL_CINT, ""));
            //request.getString(ARG_NCL_CINT, "")));
            contourOpts.append("<br/>");
            contourOpts.append(Repository.msg("Range: Low"));
            contourOpts.append(HtmlUtils.makeLatLonInput(ARG_NCL_CMIN,
                    ARG_NCL_CMIN, ""));
            //request.getString(ARG_NCL_CMIN, "")));
            contourOpts.append(Repository.msg("High"));
            contourOpts.append(HtmlUtils.makeLatLonInput(ARG_NCL_CMAX,
                    ARG_NCL_CMAX, ""));
            //request.getString(ARG_NCL_CMAX, "")));
            /*
            sb.append(
                HtmlUtils.formEntry(
                    "<div style=\"width:9em\">"
                    + Repository.msgLabel("Override Contour Defaults")
                    + "</div>", contour.toString()));
                    */
            plotOpts.append(
                HtmlUtils.formEntry(
                    Repository.msgLabel("Contours"), contourOpts.toString()));
            /* for now, don't give the option of turning on/off the shading
            plotOpts.append(HtmlUtils.formEntry(Repository.msgLabel("Shading"),
                    HtmlUtils.labeledCheckbox(ARG_NCL_SHADEMASK, "true",
                    request.get(ARG_NCL_SHADEMASK, true),
                    Repository.msg("Shade Masked Areas"))));
            */
            // colormaps
            List          cmaps    = getColorMaps();
            StringBuilder cmapOpts = new StringBuilder();
            cmapOpts.append(
                HtmlUtils.select(
                    ARG_NCL_COLORMAP, cmaps,
                    request.getSanitizedString(ARG_NCL_COLORMAP, "default"),
                    HtmlUtils.cssClass("ramadda-pulldown-with-icons")));
            cmapOpts.append("<br/>");
            cmapOpts.append(HtmlUtils.labeledCheckbox(ARG_NCL_REVERSE_CMAP,
                    "true", request.get(ARG_NCL_REVERSE_CMAP, false),
                    Repository.msg("Reverse Colormap")));
            plotOpts.append(
                HtmlUtils.formEntry(
                    Repository.msgLabel("Colormap"), cmapOpts.toString()));

            plotOpts.append(HtmlUtils.formTableClose());

            sb.append(HtmlUtils.div(plotOpts.toString(),
                                    HtmlUtils.id("plotopts")));
        }


        if (type.equals(ClimateModelApiHandler.ARG_ACTION_ENS_COMPARE)) {
            // options for charts (e.g. PDF)
            StringBuilder chartOpts = new StringBuilder();
            chartOpts.append(HtmlUtils.formTable());
            StringBuilder xaxisOpts = new StringBuilder();
            xaxisOpts.append(Repository.msgLabel("Min"));
            xaxisOpts.append(HtmlUtils.makeLatLonInput(ARG_NCL_XMIN,
                    ARG_NCL_XMIN, ""));
            xaxisOpts.append(HtmlUtils.space(2));
            xaxisOpts.append(Repository.msgLabel("Max"));
            xaxisOpts.append(HtmlUtils.makeLatLonInput(ARG_NCL_XMAX,
                    ARG_NCL_XMAX, ""));
            chartOpts.append(
                HtmlUtils.formEntry(
                    Repository.msgLabel("X-Axis Range"),
                    xaxisOpts.toString()));

            StringBuilder yaxisOpts = new StringBuilder();
            yaxisOpts.append(Repository.msgLabel("Min"));
            yaxisOpts.append(HtmlUtils.makeLatLonInput(ARG_NCL_YMIN,
                    ARG_NCL_YMIN, ""));
            yaxisOpts.append(HtmlUtils.space(2));
            yaxisOpts.append(Repository.msgLabel("Max"));
            yaxisOpts.append(HtmlUtils.makeLatLonInput(ARG_NCL_YMAX,
                    ARG_NCL_YMAX, ""));
            chartOpts.append(
                HtmlUtils.formEntry(
                    Repository.msgLabel("Y-Axis Range"),
                    yaxisOpts.toString()));

            StringBuilder statOpts = new StringBuilder();
            statOpts.append(
                HtmlUtils.radio(
                    ARG_INCLUDE_STAT, "none",
                    RepositoryManager.getShouldButtonBeSelected(
                        request, ARG_INCLUDE_STAT, "none", true)));
            statOpts.append(space1);
            statOpts.append(Repository.msg("None"));
            statOpts.append(space2);
            statOpts.append(
                HtmlUtils.radio(
                    ARG_INCLUDE_STAT, "mean",
                    RepositoryManager.getShouldButtonBeSelected(
                        request, ARG_INCLUDE_STAT, "mean", false)));
            statOpts.append(space1);
            statOpts.append(Repository.msg("Mean"));
            statOpts.append(space2);
            statOpts.append(
                HtmlUtils.radio(
                    ARG_INCLUDE_STAT, "median",
                    RepositoryManager.getShouldButtonBeSelected(
                        request, ARG_INCLUDE_STAT, "median", false)));
            statOpts.append(space1);
            statOpts.append(Repository.msg("Median"));
            chartOpts.append(
                HtmlUtils.formEntry(
                    Repository.msgLabel("Show Statistic"),
                    statOpts.toString()));

            chartOpts.append(
                HtmlUtils.formEntry(
                    Repository.msgLabel("Other"),
                    HtmlUtils.labeledCheckbox(
                        ARG_SHOW_VALUE_TICKS, "true",
                        request.get(ARG_SHOW_VALUE_TICKS, true),
                        "Show Value Ticks")));

            chartOpts.append(HtmlUtils.formTableClose());

            sb.append(HtmlUtils.div(chartOpts.toString(),
                                    HtmlUtils.id("chartopts")));

            if ( !(type.equals(ClimateModelApiHandler.ARG_ACTION_ENS_COMPARE)
                    && ModelUtil.getFrequency(request,
                        first).equals(CDOOutputHandler.FREQUENCY_DAILY))) {
                sb.append(
                    HtmlUtils.script(
                        "$('input[name=\""
                        + NCLOutputHandler.ARG_NCL_PLOTTYPE
                        + "\"]').on('change',\n function() {\n"
                        + "  if (this.value != 'pdf' && this.checked) {\n"
                        + "    $('#plotopts').toggle(true);\n"
                        + "    $('#chartopts').toggle(false);\n"
                        + "    $('#comp-years').toggle(false);\n"
                        + "  } else if (this.value === 'pdf' && this.checked) {\n"
                        + "    $('#chartopts').toggle(true);\n"
                        + "    $('#plotopts').toggle(false);\n"
                        + "    $('#comp-years').toggle(true);\n" + "  }\n"
                        + "}).change();\n"));
            }
        }


    }

    /**
     * Get the list of color maps
     *
     * @return  list
     *
     * @throws Exception problems
     */
    public List getColorMaps() throws Exception {
        if (colormaps == null) {
            colormaps = new ArrayList<TwoFacedObject>();
            colormaps.add(new TwoFacedObject("Default", "default"));
            String list =
                getRepository().getResource(
                    "/org/ramadda/geodata/model/resources/ncl/colormaps.txt");
            List<String> cmaps = StringUtil.split(list, "\n", true, true);
            for (String cmap : cmaps) {
                if (cmap.startsWith("#")) {
                    continue;
                }
                List<String> toks = StringUtil.split(cmap);
                colormaps.add(new HtmlUtils.Selector(toks.get(1),
                        toks.get(0),
                        getRepository().getUrlBase() + "/model/images/"
                        + toks.get(2), 3, 130, false));
            }
        }

        return colormaps;
    }

    /**
     * Get the first grid entry
     *
     * @param input the operands
     *
     * @return  the first grid
     */
    private Entry getFirstGridEntry(ServiceInput input) {
        List<Entry> entries = input.getEntries();
        Entry       first   = null;
        for (Entry entry : entries) {
            if (isGridEntry(entry)) {
                first = entry;

                break;
            }
        }

        return first;
    }

    /**
     * Is this a grid entry
     *
     * @param entry  the entry
     *
     * @return  true if it's a grid
     */
    private boolean isGridEntry(Entry entry) {
        TypeHandler mytype = entry.getTypeHandler();

        return (mytype instanceof ClimateModelFileTypeHandler)
               || (mytype instanceof GridTypeHandler);
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
    public ServiceOutput evaluate(Request request, Object actionID, ServiceInput input,
                                  String argPrefix)
            throws Exception {

        List<Entry>          outputEntries = new ArrayList<Entry>();
        List<Entry>          gridEntries   = new ArrayList<Entry>();
        List<Entry>          allEntries    = new ArrayList<Entry>();
        List<ServiceOperand> ops           = input.getOperands();
        StringBuilder        nameList      = new StringBuilder();
        Entry                inputEntry    = getFirstGridEntry(input);
        for (ServiceOperand op : ops) {
            List<Entry> opEntries = op.getEntries();
            for (Entry entry : opEntries) {
                if (isGridEntry(entry)) {
                    gridEntries.add(entry);
                } else {
                    allEntries.add(entry);
                }
            }
        }
        if (ops.size() > 2) {
            // Sort the entries which may be out of order from threading
            allEntries  = getEntryUtil().sortEntriesOnName(allEntries, false);
            gridEntries = getEntryUtil().sortEntriesOnName(gridEntries,
                    false);
        }
        allEntries.addAll(gridEntries);
        String type =
            input.getProperty(
                "type", ClimateModelApiHandler.ARG_ACTION_COMPARE).toString();
        List<List<Entry>> entryCollection = new ArrayList<List<Entry>>();
        if (type.equals(ClimateModelApiHandler.ARG_ACTION_ENS_COMPARE)) {
            entryCollection = sortEntriesByCollection(request, allEntries);
            String plotType =
                request.getString(NCLOutputHandler.ARG_NCL_PLOTTYPE, "pdf");
            if (plotType.equals("pdf")) {
                return evaluateForPdf(request, input, argPrefix, inputEntry);
            }
        } else {
            entryCollection.add(allEntries);
        }

        for (List<Entry> entryList : entryCollection) {


            StringBuilder fileList  = new StringBuilder();
            StringBuilder modelList = new StringBuilder();
            StringBuilder ensList   = new StringBuilder();
            StringBuilder expList   = new StringBuilder();
            StringBuilder dateList  = new StringBuilder();
            boolean       haveOne   = false;
            boolean       haveGrid  = false;
            for (Entry entry : entryList) {
                if (haveOne) {
                    fileList.append(",");
                    nameList.append(";");
                }
                if (haveGrid) {
                    modelList.append(";");
                    expList.append(";");
                    ensList.append(";");
                    dateList.append(";");
                }
                String ftail =
                    IOUtil.getFileTail(entry.getResource().toString());
                fileList.append(ftail);

                nameList.append(entry.getName());
                if (isGridEntry(entry)) {
                    modelList.append(entry.getValue(request,1));
                    expList.append(entry.getValue(request,2));
                    ensList.append(entry.getValue(request,3));
                    dateList.append(entry.getValue(request,5));
                    haveGrid = true;
                }
                //fileList.append("\"");
                haveOne = true;
            }

            String wksName = getRepository().getGUID();
            String plotType =
                request.getString(NCLOutputHandler.ARG_NCL_PLOTTYPE, "image");
            if (plotType.equals("png")) {
                plotType = "image";
            }
            String imageFormat = request.getString(ARG_NCL_IMAGEFORMAT,
                                     "gif");
            String suffix = plotType;
            if (plotType.equals("timeseries") || plotType.equals("pdf")
                    || plotType.equals("image")) {
                suffix = imageFormat;
            }

            // Hack
            if (imageFormat.equals("kmz")) {
                plotType    = "kmz";
                imageFormat = "png";
            }

            String  outputType = request.getString(ARG_NCL_OUTPUT, "comp");
            String  maskType   = request.getString(ARG_NCL_MASKTYPE, "none");
            Boolean shadeMask  = request.get(ARG_NCL_SHADEMASK, true);
            File outFile = new File(IOUtil.joinDir(input.getProcessDir(),
                               wksName) + "." + suffix);
            // The plotting routine will also generate a gif file for display
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
            commands.add(nclPath);
            commands
                .add(IOUtil
                    .joinDir(IOUtil
                        .joinDir(nclOutputHandler.getStorageManager()
                            .getResourceDir(), "ncl"), NCLOutputHandler
                                .SCRIPT_MAPPLOT));
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
            envMap.put("image_format", imageFormat);
            envMap.put("output", outputType);
            envMap.put("mask", maskType);
            envMap.put("shademask", Boolean.toString(shadeMask));
            envMap.put("rpath", repository.getScriptPath("r.rscript.path", ""));

            Hashtable    args     = request.getArgs();
            List<String> varNames = new ArrayList<String>();
            for (Enumeration keys = args.keys(); keys.hasMoreElements(); ) {
                String arg = (String) keys.nextElement();
                if (arg.startsWith(CdmDataOutputHandler.VAR_PREFIX)
                        && request.get(arg, false)) {
                    varNames.add(
                        arg.substring(
                            CdmDataOutputHandler.VAR_PREFIX.length()));
                }
            }
            String varname =
                request.getString(NCLOutputHandler.ARG_NCL_VARIABLE, null);
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
            dataset.close();
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
            if (origLonMin < 0) {  // -180 to 180
                lonMin = GeoUtils.normalizeLongitude(lonMin);
                lonMax = GeoUtils.normalizeLongitude(lonMax);
            } else {               // 0-360
                lonMin = GeoUtils.normalizeLongitude360(lonMin);
                lonMax = GeoUtils.normalizeLongitude360(lonMax);
            }
            envMap.put("maxLat",
                       request.getString(NCLOutputHandler.ARG_NCL_AREA_NORTH,
                                         String.valueOf(llb.getLatMax())));
            envMap.put("minLat",
                       request.getString(NCLOutputHandler.ARG_NCL_AREA_SOUTH,
                                         String.valueOf(llb.getLatMin())));
            envMap.put("minLon", String.valueOf(lonMin));
            envMap.put("maxLon", String.valueOf(lonMax));

            boolean haveOriginalBounds = true;
            for (String spatialArg : NCLOutputHandler.NCL_SPATIALARGS) {
                if ( !Misc.equals(request.getString(spatialArg, ""),
                                  request.getString(spatialArg + ".original",
                                      ""))) {
                    haveOriginalBounds = false;

                    break;
                }
            }
            envMap.put("addCyclic", Boolean.toString(haveOriginalBounds));
            // contours
            double   cint  = request.get(ARG_NCL_CINT, 0.);
            double   cmin  = request.get(ARG_NCL_CMIN, 0.);
            double   cmax  = request.get(ARG_NCL_CMAX, 0.);
            double[] cvals = verifyContourInfo(cint, cmin, cmax);
            if (cint != 0.) {
                envMap.put("cint", String.valueOf(cvals[0]));
                envMap.put("cmin", String.valueOf(cvals[1]));
                envMap.put("cmax", String.valueOf(cvals[2]));
            }

            String mapid =
                request.getString(NCLOutputHandler.ARG_NCL_AREA_REGIONID,
                                  "").toLowerCase().trim();
            boolean usepolar = mapid.startsWith("nh")
                               || mapid.startsWith("sh")
                               || mapid.startsWith("arctic")
                               || mapid.startsWith("ant");
            if (mapid.isEmpty()) {  // CUSTOM map
                usepolar = false;
            }
            envMap.put("usepolar", Boolean.toString(usepolar));
            if (usepolar) {
                String center = "0";
                if ((mapid.startsWith("nh") || mapid.startsWith("sh"))
                        && (mapid.length() > 2)) {
                    center = mapid.substring(2);
                } else if (mapid.startsWith("ant") && (mapid.length() > 3)) {
                    center = mapid.substring(3);
                }
                //System.out.println("Map: "+mapid+", center: "+center);
                envMap.put("meridian", center);
            }
            // xaxis
            double xmin = request.get(ARG_NCL_XMIN, 0.);
            double xmax = request.get(ARG_NCL_XMAX, 0.);
            envMap.put("xmin", String.valueOf(xmin));
            envMap.put("xmax", String.valueOf(xmax));

            // yaxis
            double ymin = request.get(ARG_NCL_YMIN, 0.);
            double ymax = request.get(ARG_NCL_YMAX, 0.);
            envMap.put("ymin", String.valueOf(ymin));
            envMap.put("ymax", String.valueOf(ymax));


            boolean haveAnom = (fileList.toString().indexOf("anom") >= 0);

            String  anomType = "anom";
            if (fileList.toString().indexOf("pctanom") >= 0) {
                anomType = "pctanom";
            } else if (fileList.toString().indexOf("stdanom") >= 0) {
                anomType = "stdanom";
            } else if (fileList.toString().indexOf("timstd") >= 0) {
                anomType = "timstd";
            }
            envMap.put("anomtype", anomType);

            String stat = "mean";
            String reqstat = request.getString(
                    CDOOutputHandler.ARG_CDO_STAT,
                    CDOOutputHandler.STAT_MEAN);
            if (reqstat.equals(CDOOutputHandler.STAT_SUM)) {
                stat = "sum";
            } else if (reqstat.equals(CDOOutputHandler.STAT_MAX)) {
                stat = "max";
            } else if (reqstat.equals(CDOOutputHandler.STAT_MIN)) {
                stat = "min";
            }
            envMap.put("stat", stat);


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

            if (type.equals(ClimateModelApiHandler.ARG_ACTION_COMPARE)
                    || type.equals(
                        ClimateModelApiHandler.ARG_ACTION_ENS_COMPARE)) {
                int climNumber =
                    request.get(CDOOutputHandler.ARG_CLIMATE_DATASET_NUMBER,
                                0);
                if (climNumber > 0) {
                    String modelexp =
                        ModelUtil.getModelExperimentString(request,
                            climNumber);
                    envMap.put("climdataId", modelexp);
                }
            }

            // gotta be a better way to do this
            boolean shouldConvertUnits = true;
            //!(anomType.equals("stdanom") || anomType.equals("pctanom"));
            String outUnits = request.getString(ARG_NCL_UNITS, null);
            //System.out.println("Out units = "+outUnits);
            if ((outUnits != null) && !outUnits.isEmpty()
                    && shouldConvertUnits) {
                envMap.put("units", outUnits);
            }

            boolean isCorrelation = outputType.equals("correlation")
                                    || outputType.equals("regression");
            String colormap = request.getString(ARG_NCL_COLORMAP, "default");
            if (colormap.equals("default")) {
                colormap = "rainbow";
                if (outputType.equals("diff") || haveAnom || isCorrelation) {
                    colormap = "testcmap";
                }
            }
            envMap.put("colormap", colormap);
            envMap.put("reverse_cmap",
                       Boolean.toString(request.get(ARG_NCL_REVERSE_CMAP,
                           false)));
            envMap.put("clines",
                       Boolean.toString(request.get(ARG_NCL_CLINES, true)));
            envMap.put("clabels",
                       Boolean.toString(request.get(ARG_NCL_CLABELS, false)));
            envMap.put("cfill",
                       Boolean.toString(request.get(ARG_NCL_CFILL, false)));
            envMap.put("latlonlines",
                       Boolean.toString(request.get(ARG_LATLONLINES, false)));
            envMap.put("ensavg",
                       Boolean.toString(request.get(ARG_ENSAVG, false)));
            envMap.put("anom", Boolean.toString(haveAnom || isCorrelation));
            envMap.put(
                "annotation",
                getRepository().getProperty(
                    "ramadda.model.sitename",
                    getRepository().getProperty(
                        Constants.PROP_REPOSITORY_NAME, "")));
            String logo =
                getRepository().getProperty(Constants.PROP_LOGO_IMAGE, "");
            if ( !logo.isEmpty()) {
                if ( !logo.startsWith("http")) {
                    if ( !logo.startsWith("/")) {
                        logo = "/" + logo;
                    }
                    logo = request.getAbsoluteUrl(logo);
                }
                envMap.put("logo", logo);
            }


            runCommands(commands, envMap, input.getProcessDir(), outFile);

            String outType = "type_image";
            if (plotType.equals("kmz")) {
                outType = "geo_kml";
            }
            Resource resource = new Resource(outFile,
                                             Resource.TYPE_LOCAL_FILE);
            TypeHandler myHandler = getRepository().getTypeHandler(outType,
                                        true);
            Entry outputEntry = new Entry(myHandler, true,
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
            if (dataset != null) {
                dataset.close();
            }
        }
        ServiceOutput dpo = new ServiceOutput(new ServiceOperand("Plot of "
                                + nameList, outputEntries));

        return dpo;

    }

    /**
     * Evaluate for making a PDF
     *
     * @param request the request
     * @param input   the input
     * @param argPrefix  the argument prefix
     * @param inputEntry  input Entry
     *
     * @return the output
     *
     * @throws Exception  problems generating PDF
     */
    private ServiceOutput evaluateForPdf(Request request, ServiceInput input,
                                         String argPrefix, Entry inputEntry)
            throws Exception {

        List<List<ServiceOperand>> sortedOps =
            ModelUtil.sortOperandsByCollection(request, input.getOperands());
        List<Entry>   outputEntries = new ArrayList<Entry>();
        StringBuilder nameList      = new StringBuilder();
        StringBuilder fileList      = new StringBuilder();
        StringBuilder modelList     = new StringBuilder();
        StringBuilder ensList       = new StringBuilder();
        StringBuilder expList       = new StringBuilder();
        StringBuilder dateList      = new StringBuilder();

        int           listNum       = 0;
        boolean       haveGrid      = false;
        for (List<ServiceOperand> ops : sortedOps) {

            Entry first = ops.get(0).getEntries().get(0);
            if (haveGrid) {
                modelList.append(";");
                expList.append(";");
                ensList.append(";");
                dateList.append(";");
            }
            modelList.append(first.getValue(request,1));
            expList.append(first.getValue(request,2));
            ensList.append(first.getValue(request,3));
            dateList.append(first.getValue(request,5));
            haveGrid = true;

            boolean     haveOne   = false;

            List<Entry> entryList = new ArrayList<Entry>();
            for (ServiceOperand op : ops) {
                entryList.add(op.getEntries().get(0));
            }

            for (Entry entry : entryList) {
                if (haveOne) {
                    fileList.append(",");
                    nameList.append(";");
                }
                String ftail =
                    IOUtil.getFileTail(entry.getResource().toString());
                fileList.append(ftail);

                nameList.append(entry.getName());
                haveOne = true;
            }
            if ((sortedOps.size() > 1) && (listNum < sortedOps.size() - 1)) {
                fileList.append("|");
                nameList.append("|");
            }
            listNum++;
        }

        String  wksName     = getRepository().getGUID();
        String  plotType    = "pdf";
        boolean tavg        = request.get(ARG_TIME_AVERAGE, true);
        String  pdfstat     = request.getString(ARG_INCLUDE_STAT, "none");
        boolean showRug     = request.get(ARG_SHOW_VALUE_TICKS, true);
        String  imageFormat = request.getString(ARG_NCL_IMAGEFORMAT, "gif");
        String  suffix      = imageFormat;
        String  outputType  = request.getString(ARG_NCL_OUTPUT, "enscomp");
        String  maskType    = request.getString(ARG_NCL_MASKTYPE, "none");
        Boolean shadeMask   = request.get(ARG_NCL_SHADEMASK, true);
        File outFile = new File(IOUtil.joinDir(input.getProcessDir(),
                           wksName) + "." + suffix);
        // The plotting routine will also generate a gif file for display
        File displayFile = new File(IOUtil.joinDir(input.getProcessDir(),
                               wksName) + ".gif");
        CdmDataOutputHandler dataOutputHandler =
            nclOutputHandler.getDataOutputHandler();
        GridDataset dataset = dataOutputHandler.getCdmManager().createGrid(
                                  inputEntry.getResource().toString());
        if (dataset == null) {
            throw new Exception("Not a grid");
        }

        List<String> commands  = new ArrayList<String>();
        commands.add(nclPath);
        commands.add(
            IOUtil.joinDir(
                IOUtil.joinDir(
                    nclOutputHandler.getStorageManager().getResourceDir(),
                    "ncl"), NCLOutputHandler.SCRIPT_MAPPLOT));
        Map<String, String> envMap = new HashMap<String, String>();
        nclOutputHandler.addGlobalEnvVars(envMap);
        envMap.put("wks_name", wksName);
        envMap.put("ncfiles", fileList.toString());
        //envMap.put("titles", nameList.toString());
        envMap.put("models", modelList.toString());
        envMap.put("exps", expList.toString());
        envMap.put("ens", ensList.toString());
        envMap.put("dates", dateList.toString());
        envMap.put("productdir", input.getProcessDir().toString());
        envMap.put("plot_type", plotType);
        envMap.put("image_format", imageFormat);
        envMap.put("output", outputType);
        envMap.put("mask", maskType);
        envMap.put("shademask", Boolean.toString(shadeMask));
        String rpath = repository.getScriptPath("r.rscript.path");
        if (rpath != null) {
            envMap.put("rpath", rpath);
        }
        envMap.put("time_average", Boolean.toString(tavg));
        envMap.put("pdfstat", pdfstat);
        envMap.put("showrug", Boolean.toString(showRug));

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
        if (origLonMin < 0) {  // -180 to 180
            lonMin = GeoUtils.normalizeLongitude(lonMin);
            lonMax = GeoUtils.normalizeLongitude(lonMax);
        } else {               // 0-360
            lonMin = GeoUtils.normalizeLongitude360(lonMin);
            lonMax = GeoUtils.normalizeLongitude360(lonMax);
        }
        envMap.put("maxLat",
                   request.getString(NCLOutputHandler.ARG_NCL_AREA_NORTH,
                                     String.valueOf(llb.getLatMax())));
        envMap.put("minLat",
                   request.getString(NCLOutputHandler.ARG_NCL_AREA_SOUTH,
                                     String.valueOf(llb.getLatMin())));
        envMap.put("minLon", String.valueOf(lonMin));
        envMap.put("maxLon", String.valueOf(lonMax));

        // xaxis
        double xmin = request.get(ARG_NCL_XMIN, 0.);
        double xmax = request.get(ARG_NCL_XMAX, 0.);
        envMap.put("xmin", String.valueOf(xmin));
        envMap.put("xmax", String.valueOf(xmax));

        // yaxis
        double ymin = request.get(ARG_NCL_YMIN, 0.);
        double ymax = request.get(ARG_NCL_YMAX, 0.);
        envMap.put("ymin", String.valueOf(ymin));
        envMap.put("ymax", String.valueOf(ymax));


        boolean haveAnom = (fileList.toString().indexOf("anom") >= 0);

        String  anomType = "anom";
        if (fileList.toString().indexOf("pctanom") >= 0) {
            anomType = "pctanom";
        } else if (fileList.toString().indexOf("stdanom") >= 0) {
            anomType = "stdanom";
        } else if (fileList.toString().indexOf("timstd") >= 0) {
            anomType = "timstd";
        }
        envMap.put("anomtype", anomType);

        String stat = "mean";
        String reqstat = request.getString(
                CDOOutputHandler.ARG_CDO_STAT,
                CDOOutputHandler.STAT_MEAN);
        if (reqstat.equals(CDOOutputHandler.STAT_SUM)) {
            stat = "sum";
        } else if (reqstat.equals(CDOOutputHandler.STAT_MAX)) {
            stat = "max";
        } else if (reqstat.equals(CDOOutputHandler.STAT_MIN)) {
            stat = "min";
        }
        envMap.put("stat", stat);
        
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

        int climNumber =
            request.get(CDOOutputHandler.ARG_CLIMATE_DATASET_NUMBER, 0);
        if (climNumber > 0) {
            String modelexp = ModelUtil.getModelExperimentString(request,
                                  climNumber);
            envMap.put("climdataId", modelexp);
        }

        // gotta be a better way to do this
        boolean shouldConvertUnits = true;
        //!(anomType.equals("stdanom") || anomType.equals("pctanom"));
        String outUnits = request.getString(ARG_NCL_UNITS, null);
        //System.out.println("Out units = "+outUnits);
        if ((outUnits != null) && !outUnits.isEmpty() && shouldConvertUnits) {
            envMap.put("units", outUnits);
        }

        envMap.put("anom", Boolean.toString(haveAnom));
        envMap.put(
            "annotation",
            getRepository().getProperty(
                "ramadda.model.sitename",
                getRepository().getProperty(
                    Constants.PROP_REPOSITORY_NAME, "")));
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


        runCommands(commands, envMap, input.getProcessDir(), outFile);

        String outType = "type_image";
        if (plotType.equals("kmz")) {
            outType = "geo_kml";
        }
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
            outputEntries.add(displayEntry);
        }
        ServiceOutput dpo = new ServiceOutput(new ServiceOperand("Plot of "
                                + nameList, outputEntries));
        if (dataset != null) {
            dataset.close();
        }

        return dpo;

    }

    /**
     * Sort the entries by model and experiment
     *
     * @param request  the request
     * @param entries  the list of all entries
     *
     * @return the entries sorted by model and experiment
     *
     * @throws Exception problems
     */
    private List<List<Entry>> sortEntriesByModelExperiment(Request request,
            List<Entry> entries)
            throws Exception {
        List<List<Entry>>        sortedList = new ArrayList<List<Entry>>();
        Map<String, List<Entry>> opMap = new HashMap<String, List<Entry>>();
        for (Entry sample : entries) {
            Object[]    values = sample.getValues();
            String key = values[1].toString() + " " + values[2].toString();
            List<Entry> myList = opMap.get(key);
            if (myList == null) {
                myList = new ArrayList<Entry>();
            }
            myList.add(sample);
            opMap.put(key, myList);
        }
        // check to see if the collections are the same
        int numcollections = 2;
        if (ModelUtil.getModelExperimentString(request,
                1).equals(ModelUtil.getModelExperimentString(request, 2))) {
            numcollections = 1;
        }
        for (int i = 0; i < numcollections; i++) {
            List<Entry> ops =
                opMap.get(ModelUtil.getModelExperimentString(request, i + 1));
            if (ops != null) {
                sortedList.add(ops);
            }
        }

        return sortedList;
    }

    /**
     * Sort the entries by model and experiment
     *
     * @param request  the request
     * @param entries  the list of all entries
     *
     * @return the entries sorted by model and experiment
     *
     * @throws Exception problems
     */
    private List<List<Entry>> sortEntriesByCollection(Request request,
            List<Entry> entries)
            throws Exception {
        List<List<Entry>>        sortedList = new ArrayList<List<Entry>>();
        Map<String, List<Entry>> opMap = new HashMap<String, List<Entry>>();
        for (Entry sample : entries) {
            String key = null;
            Object cprop = sample.getTransientProperty(
                               ClimateModelApiHandler.ARG_COLLECTION);
            if (cprop == null) {
                System.err.println("No collection property");

                continue;
            } else {
                key = cprop.toString();
            }
            List<Entry> myList = opMap.get(key);
            if (myList == null) {
                myList = new ArrayList<Entry>();
            }
            myList.add(sample);
            opMap.put(key, myList);
        }
        // check to see if the collections are the same
        int numcollections = 2;
        /*
        if (ModelUtil.getModelExperimentString(request,
                1).equals(ModelUtil.getModelExperimentString(request, 2))) {
            numcollections = 1;
        }
        */
        for (int i = 0; i < numcollections; i++) {
            String      collection = (i == 0)
                                     ? ClimateModelApiHandler.ARG_COLLECTION1
                                     : ClimateModelApiHandler.ARG_COLLECTION2;
            List<Entry> ops        = opMap.get(collection);
            if (ops != null) {
                sortedList.add(ops);
            }
        }

        return sortedList;
    }

    /**
     * Verify the contour values
     *
     * @param cint  the contour interval
     * @param cmin  the minimum value
     * @param cmax  the maximum value
     *
     * @return the adjusted values
     */
    private double[] verifyContourInfo(double cint, double cmin,
                                       double cmax) {

        if (cint == 0) {
            cint = 0;
            cmin = 0;
            cmax = 0;
        } else if (cint < 0) {
            System.err.println(
                "contour interval must be greater than zero - using default values");
            cint = 0;
            cmin = 0;
            cmax = 0;
        } else if (cmin >= cmax) {
            System.err.println(
                "min must be less than max - using default values");
            cint = 0;
            cmin = 0;
            cmax = 0;
        } else {
            double diff = (cmax - cmin) * 1.0;
            double var  = diff / cint;
            if (var > 300.) {
                System.err.println(
                    "too many contour lines - using default values");
                cint = 0;
                cmin = 0;
                cmax = 0;
            }
        }

        return new double[] { cint, cmin, cmax };

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
        // single model, multi-ensemble - can't handle yet
        if ((uniqueModels.size() == 1) && (uniqueMembers.size() > 1)) {
            return true;
        }
        // multi-model multi-ensemble - don't want to think about this
        if ((uniqueModels.size() > 1) && (uniqueMembers.size() > 1)) {
            return true;
        }

        return true;
    }

}
