/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.model;



import org.ramadda.geodata.cdmdata.CdmDataOutputHandler;
import org.ramadda.repository.Entry;
import org.ramadda.repository.Link;
import org.ramadda.repository.Repository;
import org.ramadda.repository.Request;
import org.ramadda.repository.Result;
import org.ramadda.repository.StorageManager;
import org.ramadda.repository.job.JobManager;
import org.ramadda.repository.map.MapProperties;
import org.ramadda.repository.map.MapInfo;
import org.ramadda.repository.output.OutputHandler;
import org.ramadda.repository.output.OutputType;
import org.ramadda.repository.type.CollectionTypeHandler;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.TempDir;
import org.ramadda.util.geo.GeoUtils;

import org.w3c.dom.Element;

import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;

import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;


import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * An output handler to plot maps with NCL
 */
public class NCLOutputHandler extends OutputHandler {

    /** NCL program path */
    private static final String PROP_NCARG_ROOT = "ncl.ncarg_root";

    /** NCL program path */
    private static final String PROP_CONVERT_PATH = "ncl.convert.path";

    /** NCL program path */
    private static final String PROP_GHOSTSCRIPT_PATH = "ncl.gs.path";

    /** NCL map plot script */
    public static final String SCRIPT_MAPPLOT = "plot.data.comp.ncl";

    /** NCL timeseries plot script */
    public static final String SCRIPT_TSPLOT = "plot.data.timeseries.ncl";

    /** NCL map plot script */
    private static final String SCRIPT_KML = "kml.ncl";

    /** NCL map plot script */
    private static final String SCRIPT_CHANGE_UNITS = "change_units.ncl";

    /** NCL map plot script */
    private static final String SCRIPT_HLURESFILE = ".hluresfile";

    /** scripts */
    private static final String[] SCRIPTS = {
        SCRIPT_MAPPLOT, SCRIPT_TSPLOT, SCRIPT_KML,
        "remove_middle_contours.ncl", SCRIPT_CHANGE_UNITS, SCRIPT_HLURESFILE
    };

    /** NCL prefix string */
    private static final String ARG_NCL_PREFIX = "ncl_";

    /** NCL plot string */
    public static final String ARG_NCL_PLOTTYPE = ARG_NCL_PREFIX + "plottype";

    /** NCL plot string */
    public static final String ARG_NCL_DATATYPE = ARG_NCL_PREFIX + "datatype";

    /** area arg prefix */
    //public static final String ARG_NCL_AREA = ARG_NCL_PREFIX + "area";
    public static final String ARG_NCL_AREA = "area";

    /** area North argument */
    public static final String ARG_NCL_AREA_NORTH = ARG_NCL_AREA + "_north";

    /** area South argument */
    public static final String ARG_NCL_AREA_SOUTH = ARG_NCL_AREA + "_south";

    /** area East argument */
    public static final String ARG_NCL_AREA_EAST = ARG_NCL_AREA + "_east";

    /** area West argument */
    public static final String ARG_NCL_AREA_WEST = ARG_NCL_AREA + "_west";

    /** region id argument */
    public static final String ARG_NCL_AREA_REGIONID = ARG_NCL_AREA
                                                       + "_regionid";


    /** NCL variable argument */
    public static final String ARG_NCL_VARIABLE = ARG_NCL_PREFIX
                                                  + ARG_VARIABLE;

    /** spatial arguments */
    public static final String[] SPATIALARGS = new String[] {
                                                   ARG_NCL_AREA_NORTH,
            ARG_NCL_AREA_WEST, ARG_NCL_AREA_SOUTH, ARG_NCL_AREA_EAST, };

    /** map plot output id */
    public static final OutputType OUTPUT_NCL_MAPPLOT =
        new OutputType("NCL Map Displays", "ncl.mapplot",
                       OutputType.TYPE_OTHER, OutputType.SUFFIX_NONE,
                       "/model/ncl.gif", CdmDataOutputHandler.GROUP_DATA);

    /** the product directory */
    private TempDir productDir;

    /** the resources directory */
    private String resourceDir;

    /** the resources directory */
    private String cmapDir;

    /** the path to NCL program */
    private String ncargRoot;

    /** the path to convert program */
    private String convertPath;

    /** the path to ghostscript program */
    private String gsPath;

    /** spatial arguments */
    public static final String[] NCL_SPATIALARGS = new String[] {
                                                       ARG_NCL_AREA_NORTH,
            ARG_NCL_AREA_WEST, ARG_NCL_AREA_SOUTH, ARG_NCL_AREA_EAST, };

    /** NCL version regex */
    private static final String NCL_VERSION_REGEX =
        "NCAR Service Language Version (\\d+.\\d+.\\d+)";

    /** NCL version pattern */
    public static final Pattern versionPattern =
        Pattern.compile(NCL_VERSION_REGEX);

    private static boolean warned = false;

    /**
     * Construct a new NCLOutputHandler
     *
     * @param repository  the Repository
     *
     * @throws Exception  problem creating handler
     */
    public NCLOutputHandler(Repository repository) throws Exception {
        super(repository, "NCL");
        ncargRoot = getRepository().getScriptPath(PROP_NCARG_ROOT);
        if (ncargRoot == null) {
	    if(!warned) {
		repository.getLogManager().logWarning("To run NCL, set the "
						      + PROP_NCARG_ROOT + " property");
		warned= true;
	    }
        }
        convertPath = getRepository().getScriptPath(PROP_CONVERT_PATH,"convert");
        gsPath = getRepository().getScriptPath(PROP_GHOSTSCRIPT_PATH, "gs");
        resourceDir = IOUtil.joinDir(getStorageManager().getResourceDir(),
                                     "ncl");
        cmapDir = IOUtil.joinDir(resourceDir, "colormaps");
    }

    /**
     * Construct a new NCL output handler
     *
     * @param repository  the Repository
     * @param element     the XML definition
     *
     * @throws Exception  problem creating handler
     */
    public NCLOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        addType(OUTPUT_NCL_MAPPLOT);
        ncargRoot = getRepository().getScriptPath(PROP_NCARG_ROOT);
        convertPath = getRepository().getScriptPath(PROP_CONVERT_PATH, "convert");
        gsPath = getRepository().getScriptPath(PROP_GHOSTSCRIPT_PATH, "gs");
        resourceDir = IOUtil.joinDir(getStorageManager().getResourceDir(),
                                     "ncl");
        cmapDir = IOUtil.joinDir(resourceDir, "colormaps");
        if (ncargRoot != null) {
            // write out the scripts
            StorageManager.makeDir(resourceDir);
            for (int i = 0; i < SCRIPTS.length; i++) {
                String nclScript =
                    getStorageManager().readSystemResource(
                        "/org/ramadda/geodata/model/resources/ncl/"
                        + SCRIPTS[i]);
                if ((nclScript == null) || nclScript.isEmpty()) {
                    getRepository().getLogManager().logWarning(
                        "Unable to find " + SCRIPTS[i]);
                }
                nclScript = nclScript.replaceAll("\\$NCL_RESOURCES",
                        resourceDir);
                nclScript = nclScript.replaceAll("%convert%", convertPath);
                nclScript = nclScript.replaceAll("%gs%", gsPath);
                File outputFile = new File(IOUtil.joinDir(resourceDir,
                                      SCRIPTS[i]));
                InputStream is =
                    new ByteArrayInputStream(nclScript.getBytes());
                OutputStream os =
                    getStorageManager().getUncheckedFileOutputStream(
                        outputFile);
                IOUtil.writeTo(is, os);
            }
            // write out the colormaps
            StorageManager.makeDir(cmapDir);
            String list =
                getRepository().getResource(
                    "/org/ramadda/geodata/model/resources/ncl/colormaps.txt");
            if ((list != null) && !list.isEmpty()) {
                for (String cmap : StringUtil.split(list, "\n", true, true)) {
                    List<String> toks    = StringUtil.split(cmap);
                    String       rgbfile = toks.get(0) + ".rgb";
                    String cmapresource =
                        getRepository().getResource(
                            "/org/ramadda/geodata/model/resources/ncl/colormaps/"
                            + rgbfile, true);
                    if ((cmapresource == null) || cmapresource.isEmpty()) {
                        continue;
                    }
                    File outputFile = new File(IOUtil.joinDir(cmapDir,
                                          rgbfile));
                    InputStream is =
                        new ByteArrayInputStream(cmapresource.getBytes());
                    OutputStream os =
                        getStorageManager().getUncheckedFileOutputStream(
                            outputFile);
                    IOUtil.writeTo(is, os);
                }
            }
        }
    }

    /**
     * Check to see if we have NCL installed
     *
     * @return  true if path to NCL is set
     */
    public boolean isEnabled() {
        return ncargRoot != null;
    }

    /**
     * Get the root directory
     *
     * @return  the NCARG_ROOT directory
     */
    public String getNcargRootDir() {
        return ncargRoot;
    }

    /**
     * This method gets called to determine if the given entry or entries can be displays as las xml
     *
     * @param request  the Request
     * @param state    the State
     * @param links    the list of Links to add to
     *
     * @throws Exception Problem adding links
     */
    public void getEntryLinks(Request request, State state, List<Link> links)
            throws Exception {
        if ( !isEnabled()) {
            return;
        }
        if ((state.entry != null) && state.entry.isFile()
                && state.entry.getResource().getPath().endsWith(".nc")) {
            links.add(makeLink(request, state.entry, OUTPUT_NCL_MAPPLOT));
        }
    }


    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public int getProductDirTTLHours() {
        return 1;
    }



    /**
     * Get the data output handler
     *
     * @return the handler
     *
     * @throws Exception Problem getting that
     */
    public CdmDataOutputHandler getDataOutputHandler() throws Exception {
        return (CdmDataOutputHandler) getRepository().getOutputHandler(
            CdmDataOutputHandler.OUTPUT_OPENDAP.toString());
    }




    /**
     * Create the entry display
     *
     * @param request   the Request
     * @param outputType  the output type
     * @param entry     the entry to output
     *
     * @return the entry or form
     *
     * @throws Exception problem making the form
     */
    public Result outputEntry(Request request, OutputType outputType,
                              Entry entry)
            throws Exception {


        if (request.defined(ARG_SUBMIT)) {
            return outputNCL(request, entry);
        }
        StringBuffer sb = new StringBuffer();
        addForm(request, entry, sb);

        return new Result("NCL Form", sb);
    }

    /**
     * Add the form
     *
     * @param request  the Request
     * @param entry    the Entry
     * @param sb       the HTML
     *
     * @throws Exception problems
     */
    private void addForm(Request request, Entry entry, StringBuffer sb)
            throws Exception {

        String formUrl = request.makeUrl(getRepository().URL_ENTRY_SHOW);
        sb.append(HtmlUtils.form(formUrl));
        /*
sb.append(HtmlUtils.form(formUrl,
                         makeFormSubmitDialog(sb,
                             msg("Plotting Data...."))));
                             */

        sb.append(HtmlUtils.formTable());

        sb.append(HtmlUtils.hidden(ARG_OUTPUT, OUTPUT_NCL_MAPPLOT));
        sb.append(HtmlUtils.hidden(ARG_ENTRYID, entry.getId()));
        String buttons = HtmlUtils.submit("Plot Data", ARG_SUBMIT);
        //sb.append(buttons);
        sb.append(HtmlUtils.h2("Plot Dataset"));
        sb.append(HtmlUtils.hr());
        addToForm(request, entry, sb);
        sb.append(HtmlUtils.p());
        addPublishWidget(
            request, entry, sb,
            msg("Select a folder to publish the generated image file to"));
        sb.append(HtmlUtils.formTableClose());
        sb.append(HtmlUtils.p());
        sb.append(buttons);

    }

    /**
     * Add the necessary components to the form
     *
     * @param request  the Request
     * @param entry    the Entry
     * @param sb       the HTML string
     *
     * @throws Exception  problem making form
     */
    public void addToForm(Request request, Entry entry, StringBuffer sb)
            throws Exception {
        sb.append(HtmlUtils.hidden(ARG_OUTPUT, OUTPUT_NCL_MAPPLOT));
        CdmDataOutputHandler dataOutputHandler = getDataOutputHandler();
        GridDataset dataset =
            dataOutputHandler.getCdmManager().getGridDataset(entry,
                entry.getResource().getPath());
        List<GridDatatype> grids = dataset.getGrids();
        dataOutputHandler.getCdmManager().returnGridDataset(
            entry.getResource().getPath(), dataset);
        GridDatatype var = grids.get(0);
        sb.append(HtmlUtils.hidden(ARG_NCL_VARIABLE, var));
        GridCoordSystem gcs = var.getCoordinateSystem();
        sb.append(HtmlUtils.formEntry(msgLabel("Variable"),
                                      var.getName() + HtmlUtils.space(1)
                                      + ((var.getUnitsString() != null)
                                         ? "(" + var.getUnitsString() + ")"
                                         : "") + HtmlUtils.space(3)
                                         + HtmlUtils.italics(
                                             var.getDescription())));

        if (gcs.getVerticalAxis() != null) {
            CoordinateAxis1D     zAxis  = gcs.getVerticalAxis();
            int                  sizeZ  = (int) zAxis.getSize();
            List<TwoFacedObject> levels =
                new ArrayList<TwoFacedObject>(sizeZ);
            for (int k = 0; k < sizeZ; k++) {
                int level = (int) zAxis.getCoordValue(k);
                levels.add(new TwoFacedObject(String.valueOf(level), level));
            }
            sb.append(
                HtmlUtils.formEntry(
                    msgLabel("Level"),
                    HtmlUtils.select(CdmDataOutputHandler.ARG_LEVEL, levels)
                    + HtmlUtils.space(1) + "(" + zAxis.getUnitsString()
                    + ")"));
        }
        LatLonRect llr = dataset.getBoundingBox();
        if (llr != null) {


            String mapRegionGroup = null;

            MapInfo map = getRepository().getMapManager().createMap(request,
                              null, true, null);

            map.setMapRegions(getPageHandler().getMapRegions(mapRegionGroup));
            map.addBox("", "", "", llr,
                       new MapProperties("blue", false, true));
            String[] points = new String[] { "" + llr.getLatMax(),
                                             "" + llr.getLonMin(),
                                             "" + llr.getLatMin(),
                                             "" + llr.getLonMax(), };

            for (int i = 0; i < points.length; i++) {
                sb.append(HtmlUtils.hidden(NCL_SPATIALARGS[i] + ".original",
                                           points[i]));
            }
            String llb = map.makeSelector(ARG_NCL_AREA, true, points);
            sb.append(HtmlUtils.formEntryTop(msgLabel("Area"), llb));
        }
    }

    /**
     * Output the NCL request
     *
     * @param request  the request
     * @param entry    the entry
     *
     * @return  the output
     *
     * @throws Exception  problem executing the command
     */
    public Result outputNCL(Request request, Entry entry) throws Exception {

        try {
            File input   = entry.getTypeHandler().getFileForEntry(entry);
            File outFile = processRequest(request, input);
            if (doingPublish(request)) {
                if ( !request.defined(ARG_PUBLISH_NAME)) {
                    request.put(ARG_PUBLISH_NAME, outFile.getName());
                }

                return getEntryManager().processEntryPublish(request,
                        outFile, null, entry, "generated from");
            }

            return request.returnFile(
                outFile, getStorageManager().getFileTail(outFile.toString()));
        } catch (IllegalArgumentException iae) {
            return getErrorResult(request, "NCL-Error",
                                  "An error occurred:<br>" + iae);
        }

    }

    /**
     * Process the request
     *
     * @param request  the request
     * @param input    the file to work on
     *
     * @return  the output
     *
     * @throws Exception problem generating output
     */
    public File processRequest(Request request, File input) throws Exception {

        String wksName = getRepository().getGUID();
        String plotType =
            request.getString(CollectionTypeHandler.ARG_REQUEST, "png");
        //String plotType = request.getString(ARG_NCL_PLOTTYPE,"png");
        if (plotType.equals("image")) {
            plotType = "png";
        }
        String suffix = plotType;
        if (plotType.equals("timeseries")) {
            suffix = "png";
        }
        File outFile = new File(IOUtil.joinDir(getProductDir(), wksName)
                                + "." + suffix);
        //String wksName = IOUtil.joinDir(getProductDir(),
        //                                getRepository().getGUID());
        //File outFile = new File(wksName+".png");
        CdmDataOutputHandler dataOutputHandler = getDataOutputHandler();
        GridDataset dataset =
            dataOutputHandler.getCdmManager().createGrid(input.getPath());
        if (dataset == null) {
            throw new Exception("Not a grid");
        }

        StringBuffer commandString = new StringBuffer();
        List<String> commands      = new ArrayList<String>();
        String nclPath = IOUtil.joinDir(ncargRoot, "bin/ncl");
        nclPath = getRepository().getScriptPath("ramadda.ncl.path",nclPath);
        commands.add(nclPath);
        commands.add(
            IOUtil.joinDir(
                IOUtil.joinDir(getStorageManager().getResourceDir(), "ncl"),
                SCRIPT_MAPPLOT));
        Map<String, String> envMap = new HashMap<String, String>();
        addGlobalEnvVars(envMap);
        envMap.put("wks_name", wksName);
        envMap.put("ncfiles", input.toString());
        envMap.put("productdir", getProductDir().toString());
        envMap.put("plot_type", plotType);
        envMap.put("output", "comp");

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
        String varname = request.getString(ARG_NCL_VARIABLE, null);
        if (varname == null) {
            List<GridDatatype> grids = dataset.getGrids();
            GridDatatype       var   = grids.get(0);
            varname = var.getName();
        }
        envMap.put("variable", varname);
        String level = request.getString(CdmDataOutputHandler.ARG_LEVEL,
                                         null);
        if ((level != null) && !level.isEmpty()) {
            envMap.put(CdmDataOutputHandler.ARG_LEVEL, level);
        }
        LatLonRect llb = dataset.getBoundingBox();
        // Normalize longitude bounds to the data
        double origLonMin = llb.getLonMin();
        double lonMin =
            Double.parseDouble(request.getString(ARG_NCL_AREA_WEST,
                String.valueOf(llb.getLonMin())));
        double lonMax =
            Double.parseDouble(request.getString(ARG_NCL_AREA_EAST,
                String.valueOf(llb.getLonMax())));
        if (origLonMin < 0) {  // -180 to 180
            lonMin = GeoUtils.normalizeLongitude(lonMin);
            lonMax = GeoUtils.normalizeLongitude(lonMax);
        } else {               // 0-360
            lonMin = GeoUtils.normalizeLongitude360(lonMin);
            lonMax = GeoUtils.normalizeLongitude360(lonMax);
        }
        // TODO: HACK!
        if (lonMin == lonMax) {  // 360 got set to 0
            lonMax += 360;
        }
        envMap.put("maxLat",
                   request.getString(ARG_NCL_AREA_NORTH,
                                     String.valueOf(llb.getLatMax())));
        envMap.put("minLat",
                   request.getString(ARG_NCL_AREA_SOUTH,
                                     String.valueOf(llb.getLatMin())));
        envMap.put("minLon", String.valueOf(lonMin));
        envMap.put("maxLon", String.valueOf(lonMax));

        boolean haveOriginalBounds = true;
        for (String spatialArg : NCL_SPATIALARGS) {
            if ( !Misc.equals(request.getString(spatialArg, ""),
                              request.getString(spatialArg + ".original",
                                  ""))) {
                haveOriginalBounds = false;

                break;
            }
        }
        envMap.put("addCyclic", Boolean.toString(haveOriginalBounds));
        String mapid = request.getString(ARG_NCL_AREA_REGIONID, "");
        boolean usepolar = mapid.equalsIgnoreCase("NH")
                           || mapid.equalsIgnoreCase("SH")
                           || mapid.equalsIgnoreCase("ANT");
        envMap.put("usepolar", Boolean.toString(usepolar));

        boolean haveAnom = input.toString().indexOf("anom") >= 0;
        envMap.put("anom", Boolean.toString(haveAnom));
        envMap.put("colormap", "rainbow");
        envMap.put("annotation",
                   repository.getProperty(PROP_REPOSITORY_NAME, ""));


        //System.err.println("cmds:" + commands);
        //System.err.println("env:" + envMap);

        //Use new repository method to execute. This gets back [stdout,stderr]
        JobManager.CommandResults results =
            getRepository().getJobManager().executeCommand(commands, envMap,
                getProductDir());
        String errorMsg = results.getStderrMsg();
        String outMsg   = results.getStdoutMsg();
        // Check the version
        if (suffix.equals("png")) {
            Matcher m = versionPattern.matcher(outMsg);
            if (m.find()) {
                String version = m.group(1);
                if (version.compareTo("6.0.0") < 0) {
                    String oldPath = outFile.toString();
                    outFile = new File(oldPath.replace(".png",
                            ".000001.png"));
                }
            }
        }

        if ( !outFile.exists()) {
            if (outMsg.length() > 0) {
                throw new IllegalArgumentException(outMsg);
            }
            if (errorMsg.length() > 0) {
                throw new IllegalArgumentException(errorMsg);
            }
            if ( !outFile.exists()) {
                throw new IllegalArgumentException(
                    "Humm, the NCL image generation failed for some reason");
            }
        }

        return outFile;

    }

    /**
     * Add the global environment vars (e.g. NCARG_ROOT) to the environment map
     * @param envMap  the map
     */
    public void addGlobalEnvVars(Map<String, String> envMap) {
        envMap.put("NCARG_ROOT", ncargRoot);
        envMap.put("NCARG_USRRESFILE",
                   IOUtil.joinDir(resourceDir, SCRIPT_HLURESFILE));
        envMap.put("NCARG_COLORMAPS",
                   cmapDir + ":"
                   + IOUtil.joinDir(ncargRoot, "lib/ncarg/colormaps"));
    }

}
