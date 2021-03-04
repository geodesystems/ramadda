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

package org.ramadda.geodata.cdmdata;


import opendap.dap.DAP2Exception;

import opendap.servlet.GuardedDataset;
import opendap.servlet.ReqState;

import org.ramadda.repository.DateHandler;



import org.ramadda.repository.Entry;
import org.ramadda.repository.Link;
import org.ramadda.repository.PageHandler;
import org.ramadda.repository.Repository;
import org.ramadda.repository.Request;
import org.ramadda.repository.Result;
import org.ramadda.repository.ServiceInfo;
import org.ramadda.repository.auth.AccessException;
import org.ramadda.repository.auth.AuthorizationMethod;
import org.ramadda.repository.auth.Permission;
import org.ramadda.repository.map.MapBoxProperties;
import org.ramadda.repository.map.MapInfo;
import org.ramadda.repository.output.OutputHandler;
import org.ramadda.repository.output.OutputType;
import org.ramadda.repository.type.TypeHandler;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Json;

import org.ramadda.util.KmlUtil;
import org.ramadda.util.SelectionRectangle;
import org.ramadda.util.Utils;

import org.w3c.dom.Element;

import thredds.server.ncss.format.SupportedFormat;
import thredds.server.opendap.GuardedDatasetImpl;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.Range;
import ucar.ma2.StructureData;
import ucar.ma2.StructureMembers;

import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.NetcdfFileWriter.Version;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableEnhanced;
import ucar.nc2.dt.GridCoordSystem;

import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.TrajectoryObsDataset;
import ucar.nc2.dt.TrajectoryObsDatatype;
import ucar.nc2.dt.grid.CFGridWriter2;
import ucar.nc2.dt.grid.GeoGrid;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.ft.FeatureCollection;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.NestedPointFeatureCollection;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.PointFeatureCollection;
import ucar.nc2.ft.PointFeatureIterator;
import ucar.nc2.jni.netcdf.Nc4Iosp;
import ucar.nc2.ncml.NcMLWriter;
import ucar.nc2.time.Calendar;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.nc2.time.CalendarDateRange;

import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.util.Counter;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.util.WrapperException;
import ucar.unidata.xml.XmlUtil;


import java.awt.Color;
import java.awt.image.BufferedImage;

import java.io.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;



import java.util.function.DoubleFunction;
import java.util.regex.Pattern;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;


/**
 * A class for handling CDM data output
 */
public class CdmDataOutputHandler extends CdmOutputHandler implements CdmConstants {

    /** _more_ */
    private static final boolean debug = false;


    /** set of suffixes */
    private HashSet<String> suffixSet;

    /** hash of patterns */
    private Hashtable<String, List<Pattern>> patterns;

    /** not patterns */
    private Hashtable<String, List<Pattern>> notPatterns;

    /** OPeNDAP Output Type */
    public static final OutputType OUTPUT_OPENDAP =
        new OutputType("OPeNDAP", "data.opendap",
                       OutputType.TYPE_FEEDS | OutputType.TYPE_IMPORTANT,
                       OutputType.SUFFIX_NONE, ICON_OPENDAP, GROUP_DATA);

    /** CDL Output Type */
    public static final OutputType OUTPUT_CDL =
        new OutputType("File Metadata", "data.cdl", OutputType.TYPE_OTHER,
                       OutputType.SUFFIX_NONE,
                       "/cdmdata/page_white_text.png", GROUP_DATA);


    /** _more_ */
    public static final OutputType OUTPUT_JSON = new OutputType("JSON",
                                                     "data.json",
                                                     OutputType.TYPE_OTHER,
                                                     OutputType.SUFFIX_NONE,
                                                     ICON_CSV, GROUP_DATA);

    /** WCS Output Type */
    public static final OutputType OUTPUT_WCS = new OutputType("WCS",
                                                    "data.wcs",
                                                    OutputType.TYPE_FEEDS);

    /** Point map Output Type */
    public static final OutputType OUTPUT_POINT_MAP =
        new OutputType("Plot Points on a Map", "data.point.map",
                       OutputType.TYPE_OTHER, OutputType.SUFFIX_NONE,
                       ICON_MAP, GROUP_DATA);

    /** CSV Output Type */
    public static final OutputType OUTPUT_POINT_SUBSET =
        new OutputType("CSV, KML Output", "data.point.subset",
                       OutputType.TYPE_OTHER, OutputType.SUFFIX_NONE,
                       ICON_CSV, GROUP_DATA);


    /** Trajectory map Output Type */
    public static final OutputType OUTPUT_TRAJECTORY_MAP =
        new OutputType("Show track on Map", "data.trajectory.map",
                       OutputType.TYPE_OTHER, OutputType.SUFFIX_NONE,
                       ICON_MAP, GROUP_DATA);

    /** Radar map Output Type */
    public static final OutputType OUTPUT_RADAR_MAP =
        new OutputType("Show radar on Map", "data.radar.map",
                       OutputType.TYPE_OTHER, OutputType.SUFFIX_NONE,
                       ICON_MAP, GROUP_DATA);

    /** Grid subset form Output Type */
    public static final OutputType OUTPUT_GRIDSUBSET_FORM =
        new OutputType("Subset Grid", "data.gridsubset.form",
                       OutputType.TYPE_OTHER | OutputType.TYPE_IMPORTANT,
                       OutputType.SUFFIX_NONE, "/cdmdata/subsetgrid.png",
                       GROUP_DATA);

    /** Grid subset Output Type */
    public static final OutputType OUTPUT_GRIDSUBSET =
        new OutputType("data.gridsubset", OutputType.TYPE_FEEDS);


    /** opendap counter */
    Counter opendapCounter = new Counter();


    /** the CDM manager */
    private static CdmManager cdmManager;

    /**
     * Get the CdmManager
     *
     * @return  the CDM data manager
     */
    public CdmManager getCdmManager() {
        if (cdmManager == null) {
            try {
                getRepository().addRepositoryManager(cdmManager =
                    new CdmManager(getRepository()));
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }
        }

        return cdmManager;
    }



    /**
     * Create a new CdmDataOutputHandler
     *
     * @param repository  the repository
     * @param name        the name of this handler
     *
     * @throws Exception problem creating class
     */
    public CdmDataOutputHandler(Repository repository, String name)
            throws Exception {
        super(repository, name);
    }

    /**
     *     Create a CdmDataOutputHandler
     *
     *     @param repository  the repository
     *     @param element     the element
     *     @throws Exception On badness
     */
    public CdmDataOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        getCdmManager();
        addType(OUTPUT_OPENDAP);
        addType(OUTPUT_CDL);
        addType(OUTPUT_JSON);
        addType(OUTPUT_WCS);
        addType(OUTPUT_TRAJECTORY_MAP);
        addType(OUTPUT_POINT_MAP);
        addType(OUTPUT_POINT_SUBSET);
        addType(OUTPUT_GRIDSUBSET);
        addType(OUTPUT_GRIDSUBSET_FORM);
    }


    /**
     * Get the system stats
     *
     * @param sb  the stats
     */
    public void getSystemStats(StringBuffer sb) {
        super.getSystemStats(sb);
        getCdmManager().getSystemStats(sb);
    }


    /**
     * clear the cache
     */
    public void clearCache() {
        super.clearCache();
        getCdmManager().clearCache();
    }


    /**
     * Add to an entry
     *
     * @param request the request
     * @param entry  the entry
     * @param node   the node
     *
     * @throws Exception  on badness
     */
    public void addToEntryNode(Request request, Entry entry, Element node)
            throws Exception {
        super.addToEntryNode(request, entry, node);
        if ( !getCdmManager().canLoadAsCdm(entry)) {
            return;
        }
        if ( !getRepository().getAccessManager().canAccessFile(request,
                entry)) {
            return;
        }
        String  url         = getAbsoluteOpendapUrl(request, entry);
        Element serviceNode = XmlUtil.create(TAG_SERVICE, node);
        XmlUtil.setAttributes(serviceNode, new String[] { ATTR_TYPE,
                SERVICE_OPENDAP, ATTR_URL, url });

    }


    /**
     * Get the Entry links
     *
     * @param request  the request
     * @param state    the state
     * @param links    the links
     *
     * @throws Exception on badness
     */
    public void getEntryLinks(Request request, State state, List<Link> links)
            throws Exception {

        Entry entry = state.entry;

        if ((state.group != null)
                && getCdmManager().isAggregation(state.group)) {
            entry = state.group;
        }


        if (entry == null) {
            return;
        }

        if ( !getRepository().getAccessManager().canAccessFile(request,
                entry)) {
            return;
        }

        long    t1           = System.currentTimeMillis();
        boolean canLoadAsCdm = getCdmManager().canLoadAsCdm(entry);

        if ( !canLoadAsCdm) {
            long t2 = System.currentTimeMillis();
            if ((t2 - t1) > 1) {
                //                System.err.println("CdmDataOutputHandler (cdm) getEntryLinks  "
                //                                   + entry.getName() + " time:" + (t2 - t1));
            }

            return;
        }

        //        System.err.println("cdm.getlinks-can load as grid:" + getCdmManager().canLoadAsGrid(entry) + " " + getCdmManager().canLoadAsCdmGrid(entry));
        if (getCdmManager().canLoadAsGrid(entry)
                || getCdmManager().canLoadAsCdmGrid(entry)) {
            addOutputLink(request, entry, links, OUTPUT_GRIDSUBSET_FORM);
            addOutputLink(request, entry, links,
                          GridPointOutputHandler.OUTPUT_GRIDASPOINT_FORM);
            addOutputLink(request, entry, links, OUTPUT_JSON);
        } else if (getCdmManager().canLoadAsTrajectory(entry)) {
            addOutputLink(request, entry, links, OUTPUT_TRAJECTORY_MAP);
        } else if (getCdmManager().canLoadAsPoint(entry)) {
            addOutputLink(request, entry, links, OUTPUT_POINT_MAP);
            addOutputLink(request, entry, links, OUTPUT_POINT_SUBSET);
        } else {
            //            System.err.println ("Don't know what this is:"+ entry);
        }

        Object oldOutput = request.getOutput();
        request.put(ARG_OUTPUT, OUTPUT_OPENDAP);
        String opendapUrl = getOpendapUrl(entry);
        links.add(new Link(opendapUrl, ICON_OPENDAP, "OPeNDAP",
                           OUTPUT_OPENDAP));
        request.put(ARG_OUTPUT, oldOutput);


        Link cdlLink = makeLink(request, entry, OUTPUT_CDL);
        //        cdlLink.setLinkType(OutputType.TYPE_ACTION);
        links.add(cdlLink);
        long t2 = System.currentTimeMillis();
        if ((t2 - t1) > 1) {
            //            System.err.println("CdmDataOutputHandler  getEntryLinks  "
            //                               + entry.getName() + " time:" + (t2 - t1));
        }
    }




    /**
     * Get the OPeNDAP URL
     *
     * @param entry the Entry
     *
     * @return  the URL as a string
     */
    public String getOpendapUrl(Entry entry) {
        return getOpendapHandler().getOpendapUrl(entry);
    }


    /**
     * Get the absolute OPeNDAP URL
     *
     *
     * @param request  the request
     * @param entry    the entry
     *
     * @return  the URL as a String
     */
    public String getAbsoluteOpendapUrl(Request request, Entry entry) {
        return getOpendapHandler().getAbsoluteOpendapUrl(request, entry);
    }


    /**
     * Check if we can load the Entry
     *
     * @param entry  the Entry
     *
     * @return true if we can load it
     */
    private boolean canLoadEntry(Entry entry) {
        String url = entry.getResource().getPath();
        if (url == null) {
            return false;
        }
        if (url.endsWith("~")) {
            return false;
        }
        if (url.endsWith("#")) {
            return false;
        }
        if (entry.isGroup()) {
            return false;
        }

        if (entry.getResource().isRemoteFile()) {
            return true;
        }

        if (entry.getResource().isFileType()) {
            return entry.getFile().exists();
        }
        if ( !entry.getResource().isUrl()) {
            return false;
        }
        if (url.indexOf("dods") >= 0) {
            return true;
        }

        return true;
    }




    /**
     * Output the CDL for the Entry
     *
     * @param request   the Request
     * @param entry     the Entry
     *
     * @return the CDL Result
     *
     * @throws Exception problems
     */
    public Result outputCdl(final Request request, Entry entry)
            throws Exception {

        String path     = getPath(request, entry);
        String dodspath = getAbsoluteOpendapUrl(request, entry);
        if (request.getString(CdmConstants.ARG_FORMAT,
                              "").equals(FORMAT_NCML)) {

            /**
             *  This gets hung up calling back into the repository
             *  so for now don't do it and just use the file
             * path = getAbsoluteOpendapUrl(request, entry);
             */

            NetcdfFile ncFile = NetcdfDataset.openFile(path, null);
            NcMLWriter writer = new NcMLWriter();
            String     xml    = writer.writeXML(ncFile);
            xml = xml.replace("file:" + path, dodspath).replace(path,
                              dodspath);
            Result result = new Result("", new StringBuffer(xml), "text/xml");
            ncFile.close();

            return result;
        }


        StringBuffer sb = new StringBuffer();
        if (request.get(ARG_METADATA_ADD, false)) {
            if (getRepository().getAccessManager().canDoAction(request,
                    entry, Permission.ACTION_EDIT)) {
                sb.append(HtmlUtils.p());
                List<Entry> entries = (List<Entry>) Misc.newList(entry);
                getEntryManager().addInitialMetadata(request, entries, false,
                        request.get(ARG_SHORT, false));
                getEntryManager().updateEntries(request, entries);
                sb.append(
                    getPageHandler().showDialogNote("Properties added"));
                sb.append(
                    getRepository().getHtmlOutputHandler().getInformationTabs(
                        request, entry, false));

            } else {
                sb.append("You cannot add properties");
            }

            return makeLinksResult(request, "NetCDF File Metadata", sb,
                                   new State(entry));
        }


        getPageHandler().entrySectionOpen(request, entry, sb, "");
        sb.append("<center>");
        if (getRepository().getAccessManager().canDoAction(request, entry,
                Permission.ACTION_EDIT)) {
            request.put(ARG_METADATA_ADD, HtmlUtils.VALUE_TRUE);
            sb.append(
                HtmlUtils.href(
                    request.getUrl() + "&"
                    + HtmlUtils.arg(ARG_SHORT, HtmlUtils.VALUE_TRUE), msg(
                        "Add temporal and spatial properties")));
            sb.append(
                HtmlUtils.span(
                    "&nbsp;|&nbsp;",
                    HtmlUtils.cssClass(CSS_CLASS_SEPARATOR)));

            sb.append(HtmlUtils.href(request.getUrl(),
                                     msg("Add full properties")));
            sb.append(
                HtmlUtils.span(
                    "&nbsp;|&nbsp;",
                    HtmlUtils.cssClass(CSS_CLASS_SEPARATOR)));
        }

        String tail =
            IOUtil.stripExtension(getStorageManager().getFileTail(entry));

        sb.append(HtmlUtils.href(HtmlUtils.url(getRepository().URL_ENTRY_SHOW
                + "/" + tail + SUFFIX_NCML, new String[] {
            ARG_ENTRYID, entry.getId(), ARG_OUTPUT, OUTPUT_CDL.getId(),
            CdmConstants.ARG_FORMAT, FORMAT_NCML
        }), "NCML"));
        sb.append("</center>");

        sb.append("\n<p>\n");
        sb.append(
            "<table class='ramadda-table stripe'><thead><tr><th>Variable</th><th>Unit</th><th>Dimensions</th><th>#Times</th></tr></thead><tbody>");
        GridDataset gds = getCdmManager().getGridDataset(entry, path);
        List<GridDatatype>     grids     = sortGrids(gds);
        List<VariableSimpleIF> variables = gds.getDataVariables();
        for (GridDatatype gdt : grids) {
            Dimension tdim = gdt.getTimeDimension();
            Dimension xdim = gdt.getXDimension();
            Dimension ydim = gdt.getYDimension();
            Dimension zdim = gdt.getZDimension();
            sb.append("<tr>");
            sb.append(HtmlUtils.td(gdt.getShortName()));
            sb.append(HtmlUtils.td(gdt.getUnitsString()));
            sb.append("<td>");
            sb.append(xdim.getLength() + "x" + ydim.getLength()
                      + ((zdim != null)
                         ? "x" + zdim.getLength()
                         : ""));
            sb.append("</td>");
            sb.append(HtmlUtils.td("" + tdim.getLength()));
            sb.append("</tr>");
        }
        sb.append("</tbody></table>");
        sb.append("\n");


        sb.append("<p><center><h2>CDL</h2></center>");
        NetcdfDataset dataset = getCdmManager().createNetcdfDataset(path);

        if (dataset == null) {
            sb.append("Could not open dataset");
        } else {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            PrintWriter           pw  = new PrintWriter(bos);
            ucar.nc2.NCdumpW.print(dataset, "", pw, null);
            String cdl = bos.toString();
            cdl = cdl.replace("file:" + path, dodspath).replace(path,
                              dodspath);
            sb.append("<pre>" + cdl + "</pre>");
            getCdmManager().returnNetcdfDataset(path, dataset);
        }
        getPageHandler().entrySectionClose(request, entry, sb);

        return makeLinksResult(request, "NetCDF File Metadata", sb,
                               new State(entry));

    }




    /**
     * Output the Entry as a WCS result
     *
     * @param request  the Request
     * @param entry    the Entry
     *
     * @return  the Result
     */
    public Result outputWcs(Request request, Entry entry) {
        return new Result("", new StringBuffer("TBD"));
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param tag _more_
     * @param props _more_
     * @param displayProps _more_
     *
     * @throws Exception _more_
     */
    public void getWikiTagAttrs(Request request, Entry entry, String tag,
                                Hashtable props, List<String> displayProps)
            throws Exception {

        List<String> all   = new ArrayList<String>();
        String       field = (String) props.get("gridField");
        if (field == null) {
            return;
        }
        String      path = getPath(request, entry);
        GridDataset gds  = getCdmManager().getGridDataset(entry, path);
        GeoGrid     grid = (GeoGrid) gds.findGridByName(field);
        if (grid == null) {
            System.err.println("Cannot find grid field:" + grid);

            return;
        }
        GridCoordSystem        gcs       = grid.getCoordinateSystem();
        CoordinateAxis         xaxis     = gcs.getXHorizAxis();
        CoordinateAxis         yaxis     = gcs.getYHorizAxis();
        CoordinateAxis1D       compAxis  = null;
        List<CalendarDate>     dates     = getGridDates(gds);

        List<VariableSimpleIF> variables = gds.getDataVariables();
        all.add("gridField");
        String vars = null;
        for (VariableSimpleIF var : variables) {
            if (vars == null) {
                vars = "";
            } else {
                vars += ",";
            }
            vars += var.getShortName() + ":" + var.getDescription();
        }
        displayProps.add("request.gridField.includeAll");
        displayProps.add("false");
        displayProps.add("request.gridField.label");
        displayProps.add(Json.quote("Field"));
        displayProps.add("request.gridField.values");
        displayProps.add(Json.quote(vars));

        if (dates.size() > 0) {
            all.add("gridTime");
            displayProps.add("request.gridTime.includeAll");
            displayProps.add("false");
            displayProps.add("request.gridTime.label");
            displayProps.add(Json.quote("Time"));
            displayProps.add("request.gridTime.values");
            String v = "-1" + ":" + "All";
            for (int i = 0; i < dates.size(); i++) {
		v += ",";
                v += i + ":" + dates.get(i);
            }
            displayProps.add(Json.quote(v));
        }

        if (grid.getZDimension() != null) {
            CoordinateAxis1D zAxis =
                grid.getCoordinateSystem().getVerticalAxis();
            if (zAxis != null) {
                String unit = zAxis.getUnitsString();
                if (unit == null) {
                    unit = "";
                } else {
                    unit = " [" + unit + "]";
                }
                double[] zVals = zAxis.getCoordValues();
                all.add("gridLevel");
                displayProps.add("request.gridLevel.label");
                displayProps.add(Json.quote("Grid Level"));
                displayProps.add("request.gridLevel.includeAll");
                displayProps.add("false");
                displayProps.add("request.gridLevel.values");
                String v = null;
                for (int i = 0; i < zVals.length; i++) {
                    if (v == null) {
                        v = "";
                    } else {
                        v += ",";
                    }
                    v += i + ":" + zVals[i] + unit;
                }
                v += ",last" + ":" + zVals[zVals.length - 1] + unit;
                displayProps.add(Json.quote(v));
            }
        }
        all.add("gridStride");
        displayProps.add("request.gridStride.includeAll");
        displayProps.add("false");
        displayProps.add("request.gridStride.label");
        displayProps.add(Json.quote("Stride"));
        displayProps.add("request.gridStride.values");
        displayProps.add(Json.quote("-1:default,1,2,3,4,5,6,7,8,10"));
        String stride = (String) props.get("gridStride");
        if (stride != null) {
            displayProps.add("request.gridStride.default");
            displayProps.add(Json.quote(stride));
        }
        String level = (String) props.get("gridLevel");
        if (level != null) {
            displayProps.add("request.gridLevel.default");
            displayProps.add(Json.quote(level));
        }
        if (field != null) {
            displayProps.add("request.gridField.default");
            displayProps.add(Json.quote(field));
        }


        //      displayProps.add("request." + column.getName() + ".urlarg");
        //      displayProps.add(Json.quote(column.getSearchArg()));

        displayProps.add("requestFields");
        displayProps.add(Json.quote(Misc.join(",", all)));

        getCdmManager().returnGridDataset(path, gds);

    }




    /**
     * Get the grid dates
     *
     * @param dataset  the dataset
     *
     * @return  the dates or null
     */
    public static List<CalendarDate> getGridDates(GridDataset dataset) {
        List<CalendarDate> gridDates = new ArrayList<CalendarDate>();
        if (dataset == null) {
            return gridDates;
        }
        List<GridDatatype>    grids    = dataset.getGrids();
        HashSet<CalendarDate> dateHash = new HashSet<CalendarDate>();
        List<CoordinateAxis1DTime> timeAxes =
            new ArrayList<CoordinateAxis1DTime>();

        for (GridDatatype grid : grids) {
            GridCoordSystem      gcs      = grid.getCoordinateSystem();
            CoordinateAxis1DTime timeAxis = gcs.getTimeAxis1D();
            if ((timeAxis != null) && !timeAxes.contains(timeAxis)) {
                timeAxes.add(timeAxis);

                List<CalendarDate> timeDates = timeAxis.getCalendarDates();
                for (CalendarDate timeDate : timeDates) {
                    dateHash.add(timeDate);
                }
            }
        }
        if ( !dateHash.isEmpty()) {
            gridDates = Arrays.asList(
                dateHash.toArray(new CalendarDate[dateHash.size()]));
            Collections.sort(gridDates);
        }

        return gridDates;
    }

    /**
     * Get the variable selector form
     *
     * @param dataset  the dataset
     * @param withLevelSelector  if true, include a level selector widget
     *
     * @return  the form
     */
    protected StringBuffer getVariableForm(GridDataset dataset,
                                           boolean withLevelSelector) {
        return getVariableForm(dataset, withLevelSelector, false, true);
    }

    /**
     * Get the variable selector form
     *
     * @param dataset  the dataset
     * @param withLevelSelector  if true, include a level selector widget
     * @param onlyIfAllLevelsEqual  only display the level selector
     *                              there is only one type of level
     * @param useLevelValue true to use the level value, otherwise the index
     *
     * @return  the form
     */
    protected StringBuffer getVariableForm(GridDataset dataset,
                                           boolean withLevelSelector,
                                           boolean onlyIfAllLevelsEqual,
                                           boolean useLevelValue) {
        int                varCnt            = 0;
        StringBuffer       varSB             = new StringBuffer();
        StringBuffer       varSB2D           = new StringBuffer();
        StringBuffer       varSB3D           = new StringBuffer();
        List<GridDatatype> grids             = sortGrids(dataset);
        boolean            haveOneVerticalCS = true;
        CoordinateAxis1D   compAxis          = null;




        for (GridDatatype grid : grids) {
            String cbxId = "varcbx_" + (varCnt++);
            String call =
                HtmlUtils.attr(HtmlUtils.ATTR_ONCLICK,
                               HtmlUtils.call("checkboxClicked",
                                   HtmlUtils.comma("event",
                                       HtmlUtils.squote(ARG_VARIABLE),
                                       HtmlUtils.squote(cbxId))));
            VariableEnhanced var     = grid.getVariable();
            StringBuffer     sbToUse = null;
            if (grid.getZDimension() == null) {
                sbToUse = varSB2D;
            } else {
                sbToUse = varSB3D;
                CoordinateAxis1D myZAxis =
                    grid.getCoordinateSystem().getVerticalAxis();
                if (myZAxis != null) {
                    if (compAxis == null) {
                        compAxis = myZAxis;
                    } else if (haveOneVerticalCS) {
                        haveOneVerticalCS = compAxis.equals(myZAxis);
                    }
                }
            }

            sbToUse.append(HtmlUtils.row(HtmlUtils.cols(HtmlUtils.checkbox(
            //ARG_VARIABLE + "." + var.getShortName(),
            ARG_VARIABLE, var.getShortName() /*HtmlUtils.VALUE_TRUE*/,
                          (grids.size() == 1),
                          HtmlUtils.id(cbxId) + call) + HtmlUtils.space(1)
                              + var.getShortName() + HtmlUtils.space(1)
                              + ((var.getUnitsString() != null)
                                 ? "(" + var.getUnitsString() + ")"
                                 : ""), "<i>" + var.getDescription()
                                        + "</i>")));

        }
        if (varSB2D.length() > 0) {
            if (varSB3D.length() > 0) {
                varSB.append(
                    HtmlUtils.row(
                        HtmlUtils.headerCols(new Object[] { "2D Grids" })));
            }
            varSB.append(varSB2D);
        }
        if (varSB3D.length() > 0) {
            if ((varSB2D.length() > 0) || withLevelSelector) {
                String header = " 3D Grids";
                if (withLevelSelector) {
                    if ( !haveOneVerticalCS && !onlyIfAllLevelsEqual) {
                        header += HtmlUtils.space(3) + "Level:"
                                  + HtmlUtils.space(1)
                                  + HtmlUtils.input(ARG_LEVEL, "");
                    } else if (haveOneVerticalCS) {
                        header += HtmlUtils.space(3) + "Level:"
                                  + HtmlUtils.space(1);
                        double[] zVals = compAxis.getCoordValues();
                        List<TwoFacedObject> selObjs =
                            new ArrayList<TwoFacedObject>(zVals.length);
                        selObjs.add(new TwoFacedObject("All", -1));
                        for (int i = 0; i < zVals.length; i++) {
                            if (useLevelValue) {
                                selObjs.add(
                                    new TwoFacedObject(
                                        String.valueOf(zVals[i]), zVals[i]));
                            } else {
                                selObjs.add(
                                    new TwoFacedObject(
                                        String.valueOf(zVals[i]), i));
                            }
                        }
                        header += HtmlUtils.select(ARG_LEVEL, selObjs)
                                  + HtmlUtils.space(2) + "("
                                  + compAxis.getUnitsString() + ")";
                    }
                }
                varSB.append(
                    HtmlUtils.row(
                        HtmlUtils.headerCols(new Object[] { header })));
            }
            varSB.append(varSB3D);
        }

        return varSB;
    }



    /**
     * Handle a grid subset request
     *
     * @param request the request
     * @param entry   the entry
     *
     * @return  a Result
     *
     * @throws Exception  problem handling the request
     */
    public Result outputGridSubsetForm(Request request, Entry entry)
            throws Exception {

        boolean canAdd =
            getRepository().getAccessManager().canDoAction(request,
                entry.getParentEntry(), Permission.ACTION_NEW);


        String       path    = getPath(request, entry);
        StringBuffer sb      = new StringBuffer();
        String       formUrl =
            request.makeUrl(getRepository().URL_ENTRY_SHOW);
        String fileName = IOUtil.stripExtension(entry.getName())
                          + "_subset.nc";

        String formId = HtmlUtils.getUniqueId("form_");

        getPageHandler().entrySectionOpen(request, entry, sb, "Subset Grid");

        sb.append(HtmlUtils.formPost(formUrl + "/" + fileName,
                                     HtmlUtils.id(formId)));
        sb.append(HtmlUtils.br());

        sb.append(HtmlUtils.submit("Subset", ARG_SUBMIT));
        sb.append(HtmlUtils.br());
        sb.append(HtmlUtils.hidden(ARG_OUTPUT, OUTPUT_GRIDSUBSET));
        sb.append(HtmlUtils.hidden(ARG_ENTRYID, entry.getId()));
        sb.append(HtmlUtils.formTable());



        sb.append(HtmlUtils.formEntry(msgLabel("Horizontal Stride"),
                                      HtmlUtils.input(ARG_HSTRIDE,
                                          request.getString(ARG_HSTRIDE,
                                              "1"), HtmlUtils.SIZE_3)));

        GridDataset dataset      = getCdmManager().getGridDataset(entry,
                                       path);
        List<CalendarDate> dates = getGridDates(dataset);
        StringBuffer       varSB = getVariableForm(dataset, true, true,
                                       false);
        LatLonRect         llr   = dataset.getBoundingBox();
        if (llr != null) {
            MapInfo map = getRepository().getMapManager().createMap(request,
                              entry, true, null);
            map.addBox("", "", "", llr,
                       new MapBoxProperties("blue", false, true));
            String[] points = new String[] { "" + llr.getLatMax(),
                                             "" + llr.getLonMin(),
                                             "" + llr.getLatMin(),
                                             "" + llr.getLonMax(), };

            for (int i = 0; i < points.length; i++) {
                sb.append(HtmlUtils.hidden(SPATIALARGS[i] + ".original",
                                           points[i]));
            }
            String llb = map.makeSelector(ARG_AREA, true, points);
            sb.append(HtmlUtils.formEntryTop(msgLabel("Subset Spatially"),
                                             llb));
        }
        addTimeWidget(request, dates, sb);

        sb.append(
            HtmlUtils.formEntry(
                msgLabel("Add Lat/Lon Variables"),
                HtmlUtils.checkbox(
                    ARG_ADDLATLON, HtmlUtils.VALUE_TRUE,
                    request.get(
                        ARG_ADDLATLON,
                        true)) + " (if needed for CF compliance)"));

        /*
        // TODO: check if we can use this.
        // This uses JNI, so not available everywhere
        List formats = Misc.toList(new Object[] {
                           new TwoFacedObject("NetCDF3", NetcdfFileWriter.Version.netcdf3.toString()),
                           new TwoFacedObject("NetCDF4", NetcdfFileWriter.Version.netcdf4.toString()),
                           new TwoFacedObject("NetCDF4 Classic", NetcdfFileWriter.Version.netcdf4_classic.toString())});

        String format = request.getString(CdmConstants.ARG_FORMAT, SupportedFormat.NETCDF3.getFormatName());

        sb.append(HtmlUtils.formEntry(msgLabel("Format"),
                                      HtmlUtils.select(CdmConstants.ARG_FORMAT, formats,
                                          format)));
        */

        addPublishWidget(request, entry, sb,
                         msg("Select a folder to publish the results to"));
        sb.append(HtmlUtils.formTableClose());
        sb.append("<hr>");
        sb.append(msgLabel("Select Variables"));
        sb.append("<ul>");
        sb.append("<table>");
        sb.append(varSB);
        sb.append("</table>");
        sb.append("</ul>");
        sb.append(HtmlUtils.br());
        sb.append(HtmlUtils.submit(msg("Subset")));
        addUrlShowingForm(sb, formId,
                          "[\".*OpenLayers_Control.*\",\".*original.*\"]");
        sb.append(HtmlUtils.formClose());

        getPageHandler().entrySectionClose(request, entry, sb);

        getCdmManager().returnGridDataset(path, dataset);

        return makeLinksResult(request, msg("Grid Subset"), sb,
                               new State(entry));
    }



    /**
     * Output the grid subset
     *
     * @param request the request
     * @param entry the entry to subset
     *
     * @return  the Result
     *
     * @throws Exception some problem
     */
    public Result outputGridSubset(Request request, Entry entry)
            throws Exception {

        boolean canAdd =
            getRepository().getAccessManager().canDoAction(request,
                entry.getParentEntry(), Permission.ACTION_NEW);

        String       path = getPath(request, entry);
        StringBuffer sb   = new StringBuffer();

        String format =
            request.getString(CdmConstants.ARG_FORMAT,
                              NetcdfFileWriter.Version.netcdf3.toString());


        // There's gotta be a better way to do this
        NetcdfFileWriter.Version ncVersion = NetcdfFileWriter.Version.netcdf3;
        for (NetcdfFileWriter.Version ver :
                NetcdfFileWriter.Version.values()) {
            if (format.equals(ver.toString())) {
                ncVersion = ver;
            }
        }
        List<String> varNames = new ArrayList<String>();
        Hashtable    args     = request.getArgs();
        for (Enumeration keys = args.keys(); keys.hasMoreElements(); ) {
            String arg = (String) keys.nextElement();
            if (arg.startsWith(VAR_PREFIX) && request.get(arg, false)) {
                varNames.add(arg.substring(VAR_PREFIX.length()));
            }
        }


        for (String v :
                (List<String>) request.get(ARG_VARIABLE,
                                           new ArrayList<String>())) {
            varNames.addAll(StringUtil.split(v, ",", true, true));
        }

        //        System.err.println("vars:" + varNames);


        //            System.err.println(varNames);
        GridDataset gds = getCdmManager().getGridDataset(entry, path);
        // initialize the bounds and date range to the defaults
        LatLonRect        llr                 = gds.getBoundingBox();
        CalendarDateRange cdr                 = gds.getCalendarDateRange();
        boolean           anySpatialDifferent = false;
        boolean           haveAllSpatialArgs  = true;
        //if (varNames.size() == 0 || varNames.get(0).equalsIgnoreCase("all")) {
        if ((varNames.size() == 1)
                && varNames.get(0).equalsIgnoreCase("all")) {
            List<VariableSimpleIF> variables = gds.getDataVariables();
            varNames = new ArrayList<String>();
            for (VariableSimpleIF var : variables) {
                varNames.add(var.getShortName());
            }
        }

        for (String spatialArg : SPATIALARGS) {
            if ( !Misc.equals(request.getString(spatialArg, ""),
                              request.getString(spatialArg + ".original",
                                  ""))) {
                anySpatialDifferent = true;

                break;
            }
        }
        for (String spatialArg : SPATIALARGS) {
            if ( !request.defined(spatialArg)) {
                haveAllSpatialArgs = false;

                break;
            }
        }

        SelectionRectangle bbox = TypeHandler.getSelectionBounds(request);

        if (bbox.allDefined()) {
            llr = new LatLonRect(new LatLonPointImpl(bbox.getNorth(),
                    bbox.getWest()), new LatLonPointImpl(bbox.getSouth(),
                        bbox.getEast()));
        } else if (haveAllSpatialArgs && anySpatialDifferent) {
            llr = new LatLonRect(
                new LatLonPointImpl(
                    request.getLatOrLonValue(ARG_AREA_NORTH, 90.0), request
                        .getLatOrLonValue(
                            ARG_AREA_WEST, -180.0)), new LatLonPointImpl(
                                request.getLatOrLonValue(
                                    ARG_AREA_SOUTH, 0.0), request
                                        .getLatOrLonValue(
                                            ARG_AREA_EAST, 180.0)));
            //                System.err.println("llr:" + llr);
        }
        int   hStride = request.get(ARG_HSTRIDE, request.get("stride", 1));
        Range zRange  = null;
        if (request.defined(ARG_LEVEL)) {
            int index = request.get(ARG_LEVEL, -1);
            if (index >= 0) {
                zRange = new Range(index, index);
            }
        }
        boolean            includeLatLon = request.get(ARG_ADDLATLON, false);
        int                timeStride    = 1;
        List<CalendarDate> allDates      = getGridDates(gds);
        CalendarDate[]     dates         = new CalendarDate[2];
        if (cdr != null) {
            dates[0] = cdr.getStart();
            dates[1] = cdr.getEnd();
        }
        Calendar cal       = null;
        String   calString = request.getString(ARG_CALENDAR, null);
        if ( !allDates.isEmpty()) {  // have some dates
            if (calString == null) {
                calString = allDates.get(0).getCalendar().toString();
            }
            // have to check if defined, because no selection is ""
            if (request.defined(ARG_FROMDATE)) {
                String fromDateString = request.getString(ARG_FROMDATE,
                                            formatDate(request,
                                                allDates.get(0)));
                dates[0] = CalendarDate.parseISOformat(calString,
                        fromDateString);
            } else {
                dates[0] = allDates.get(0);
            }
            if (request.defined(ARG_TODATE)) {
                String toDateString = request.getString(ARG_TODATE,
                                          formatDate(request,
                                              allDates.get(allDates.size()
                                                  - 1)));
                dates[1] = CalendarDate.parseISOformat(calString,
                        toDateString);
            } else {
                dates[1] = allDates.get(allDates.size() - 1);
            }
        }
        //have to have both dates
        if ((dates[0] != null) && (dates[1] == null)) {
            dates[0] = null;
        }
        if ((dates[1] != null) && (dates[0] == null)) {
            dates[1] = null;
        }
        if ((dates[0] != null) && (dates[1] != null)
                && (dates[0].isAfter(dates[1]))) {
            sb.append(
                getPageHandler().showDialogWarning(
                    "From date is after to date"));
        } else if (varNames.size() == 0) {
            sb.append(
                getPageHandler().showDialogWarning("No variables selected"));
        } else {
            File f = getRepository().getStorageManager().getTmpFile(request,
                         "subset" + ncVersion.getSuffix());
	    /**
            System.err.println(f.getPath());
            List grids = gds.getGrids();
            for (int i = 0; i < grids.size(); i++) {
                System.err.println(grids.get(i));
            }
            GeoGrid grid = (GeoGrid) gds.findGridByName("Pressure_surface");
            //      System.err.println(grid.getTimes());
            grid = grid.subset(new Range(0, 0), null, null, 1, 2, 2);

            GridCoordSystem gcs   = grid.getCoordinateSystem();
            CoordinateAxis  xaxis = gcs.getXHorizAxis();
            CoordinateAxis  yaxis = gcs.getYHorizAxis();
            int[] idx1 = gcs.findXYindexFromLatLon(yaxis.getMinValue(),
                             xaxis.getMinValue(), null);
            int[] idx2 = gcs.findXYindexFromLatLon(yaxis.getMaxValue(),
                             xaxis.getMaxValue(), null);
            List<LatLonPoint> points = new ArrayList<LatLonPoint>();
            int               lats   = 0,
                              lons   = 0;
            for (Dimension d : gcs.getDomain()) {
                if (d.getShortName().equals("lat")) {
                    lats = d.getLength();
                } else if (d.getShortName().equals("lon")) {
                    lons = d.getLength();
                }
            }
            System.err.println("ll:" + lats + " " + lons);
            for (int lat = 0; lat < lats; lat++) {
                for (int lon = 0; lon < lons; lon++) {
                    points.add(gcs.getLatLon(lon, lat));
                }
            }
            Array a = grid.readYXData(0, 0);
            System.err.println("points:" + points.size() + " a:"
                               + a.getSize());
            FileOutputStream fos    = new FileOutputStream(f);
            PrintWriter      writer = new PrintWriter(fos);
            writer.println("#fields="
                           + "Pressure_surface,latitude,longitude");
            for (int i = 0; i < a.getSize(); i++) {
                LatLonPoint llp = points.get(i);
                float       v   = a.getFloat(i);
                writer.println(v + "," + llp.getLatitude() + ","
                               + llp.getLongitude());
            }
            writer.close();
	    **/


            NetcdfFileWriter ncFileWriter = null;
            try {
                ncFileWriter = NetcdfFileWriter.createNew(ncVersion,
                        f.getPath());
            } catch (IOException e) {
                e.printStackTrace();
            }


            if (debug) {
                System.err.println("CdmData.subset: " + " vars:" + varNames
                                   + " llr:" + llr);
            }
            // ucar.nc2.dt.GridDataset gds,
            // List<String> gridList,
            // LatLonRect llbb,
            // ProjectionRect projRect,
            // int horizStride,
            // Range zRange,
            // CalendarDateRange dateRange,
            // int stride_time,
            // boolean addLatLon,
            // NetcdfFileWriter writer



            CFGridWriter2.writeFile(gds, varNames, llr, null, hStride,
                                    zRange, ((dates[0] == null)
                                             ? null
                                             : CalendarDateRange.of(dates[0],
                                             dates[1])), timeStride,
                                                 includeLatLon, ncFileWriter);

            /*
            writer.makeFile(f.toString(), gds, varNames, llr, hStride,
                            zStride,
                            ((dates[0] == null)
                                      ? null
                                      : CalendarDateRange.of(dates[0],
                                      dates[1])),
                                      timeStride, includeLatLon,
                                          ncVersion);
                                          */

            getCdmManager().returnGridDataset(path, gds);




            if (doingPublish(request)) {
                TypeHandler typeHandler =
                    getRepository().getTypeHandler(TypeHandler.TYPE_FILE);
                Entry newEntry =
                    typeHandler.createEntry(getRepository().getGUID());

                return getEntryManager().processEntryPublish(request, f,
                        newEntry, entry, "subset of");
            }

            Result result =
                new Result(entry.getName() + ncVersion.getSuffix(),
                           getStorageManager().getFileInputStream(f),
                           "application/x-netcdf");
            result.setReturnFilename(entry.getName() + "_subset"
                                     + ncVersion.getSuffix());

            return result;
        }

        return makeLinksResult(request, msg("Grid Subset"), sb,
                               new State(entry));


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
    public Result processGridJsonRequest(Request request) throws Exception {
        String prefix = getRepository().getUrlBase() + "/grid/json";
        Entry  entry  = getCdmManager().findEntryFromPath(request, prefix);
        request.setCORSHeaderOnResponse();

        return outputGridJson(request, entry);
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
    public Result outputGridJson(final Request request, final Entry entry)
            throws Exception {

        final boolean debug = false;
        String        path  = getPath(request, entry);
        if (debug) {
            System.err.println("outputGridJson path:" + path);
        }
        String gridField = request.getString("gridField", (String) null);
        final GridDataset  gds   = getCdmManager().getGridDataset(entry,
                                       path);
        List<CalendarDate> dates = getGridDates(gds);
        if ((gridField == null) || (gridField.length() == 0)) {
            gridField = gds.getDataVariables().get(0).getShortName();
        }
        final String field = gridField;
        GeoGrid      grid  = (GeoGrid) gds.findGridByName(field);
        if (grid == null) {
            throw new RuntimeException("Could not find grid field:" + field);
        }
        final String fieldLabel = grid.getDescription();
        int          timeIndex  = -1;
        Range        tRange     = null;
        if (request.defined("gridTime")) {
            timeIndex = request.get("gridTime", -1);
            if (timeIndex >= 0) {
                tRange = new Range(timeIndex, timeIndex);
                CalendarDate date = dates.get(timeIndex);
                dates = new ArrayList<CalendarDate>();
                dates.add(date);
            }
        }



        CoordinateAxis1D zAxis = grid.getCoordinateSystem().getVerticalAxis();
        double[]         zVals = null;
        if (zAxis != null) {
            zVals = zAxis.getCoordValues();
        }
        Range zRange = null;
        if (request.defined("gridLevel")) {
            String gridLevel = request.getString("gridLevel", (String) null);
            if (gridLevel != null) {
                if (gridLevel.equals("last") && (zVals != null)) {
                    zRange = new Range(zVals.length - 1, zVals.length - 1);
                } else {
                    int index = new Integer(gridLevel).intValue();
                    if (index >= 0) {
                        zRange = new Range(index, index);
                    }
                }
            }
        }

        if (zRange == null) {
            zRange = new Range(1, 1);
        }
        final int  max        = request.get("max", 200000);
        final int  timeStride = request.get("timeStride", 1);
        int        gridStride = request.get("gridStride", -1);



        LatLonRect bounds     = null;
        String     gridBounds = request.getString("gridBounds", null);
        if (gridBounds != null) {
            List<String> toks = StringUtil.split(gridBounds, ",");
            if (toks.size() == 4) {
                bounds = new LatLonRect(
                    new LatLonPointImpl(
                        Double.parseDouble(toks.get(0)), Double.parseDouble(
                            toks.get(1))), new LatLonPointImpl(
                                Double.parseDouble(
                                    toks.get(2)), Double.parseDouble(
                                    toks.get(3))));
            }
        }


        Dimension timeDimension = grid.getTimeDimension();
        int       numTimes      = timeDimension.getLength();
        String    unit          = grid.getUnitsString();
        if (debug) {
            System.err.println("field:" + field + " unit:" + unit
                               + " timeStride:" + timeStride + " gridStride:"
                               + gridStride + " max:" + max + " bounds:"
                               + bounds + " numTimes:" + numTimes
                               + " zRange:" + zRange + " tRange:" + tRange);
        }


        //If a stride was not specified then keep subsetting until we're under the max # points
        if (gridStride > 0) {
            grid = grid.subset(tRange, zRange, bounds, 1, gridStride,
                               gridStride);
            Dimension xDimension = grid.getXDimension();
            Dimension yDimension = grid.getYDimension();
            int numPoints = xDimension.getLength() * yDimension.getLength()
                            * numTimes / timeStride;
            if (debug) {
                System.err.println(
                    "\tnum points:" + numPoints + " per layer:"
                    + (xDimension.getLength() * yDimension.getLength())
                    + " grid stride:" + gridStride);
            }
        } else {
            grid       = grid.subset(tRange, zRange, bounds, 1, 1, 1);
            gridStride = 1;
            int     gridStrideX = 1;
            int     gridStrideY = 1;
            boolean doX         = true;
            while (true) {
                Dimension xDimension = grid.getXDimension();
                Dimension yDimension = grid.getYDimension();
                int numPoints = xDimension.getLength()
                                * yDimension.getLength() * numTimes
                                / timeStride;
                if (debug) {
                    System.err.println(
                        "\tnum points:" + numPoints + " per layer:"
                        + (xDimension.getLength() * yDimension.getLength())
                        + " grid stride:" + gridStrideX);
                }
                if (numPoints < max) {
                    break;
                }
                if (doX) {
                    gridStrideX++;
                } else {
                    gridStrideY++;
                }
                grid = grid.subset(null, null, null, 1, gridStrideX,
                                   gridStrideY);
                doX = !doX;
            }
        }



        GridCoordSystem         gcs    = grid.getCoordinateSystem();
        int                     lats   = (int) gcs.getYHorizAxis().getSize();
        int                     lons   = (int) gcs.getXHorizAxis().getSize();
        final List<LatLonPoint> points = new ArrayList<LatLonPoint>();
        for (int lat = 0; lat < lats; lat++) {
            for (int lon = 0; lon < lons; lon++) {
                points.add(gcs.getLatLon(lon, lat));
            }
        }

        if (debug) {
            System.err.println("\t# lat/lons:" + points.size() + " #dates:"
                               + dates.size());
        }
        PipedInputStream         in       = new PipedInputStream();
        final PipedOutputStream  out      = new PipedOutputStream(in);
        final List<CalendarDate> theDates = dates;
        final GeoGrid            theGrid  = grid;
        Misc.run(new Runnable() {
            public void run() {
                PrintWriter writer = new PrintWriter(out);
                try {

                    runInner(writer);
                } catch (Exception exc) {
                    writer.println("Error:" + exc);
                    System.err.println("Error:" + exc);
                    exc.printStackTrace();
                }
            }
            public void runInner(PrintWriter writer) throws Exception {
                writer.println("{\"name\":" + Json.quote(entry.getName())
                               + ",");
                writer.println("\"fields\":");
                List<String> fields = new ArrayList<String>();
                int          index  = 0;
                fields.add(Json.map("id", Json.quote(field), "label",
                                    Json.quote(fieldLabel), "index",
                                    "" + (index++), "type",
                                    Json.quote("double"), "chartable",
                                    "true", "unit",
                                    Json.quote(getUnit(unit))));
                //todo: check for times
                fields.add(Json.map("id", Json.quote("date"), "label",
                                    Json.quote("Date"), "index",
                                    "" + (index++), "type",
                                    Json.quote("date")));

                fields.add(Json.map("id", Json.quote("latitude"), "label",
                                    Json.quote("Latitude"), "index",
                                    "" + (index++), "type",
                                    Json.quote("double")));
                fields.add(Json.map("id", Json.quote("longitude"), "label",
                                    Json.quote("Longitude"), "index",
                                    "" + (index++), "type",
                                    Json.quote("double")));
                writer.println(Json.list(fields));
                List<String> displayProps = new ArrayList<String>();
                Hashtable    wikiProps    = new Hashtable();
                for (Enumeration keys = request.keys();
                        keys.hasMoreElements(); ) {
                    String key = (String) keys.nextElement();
                    wikiProps.put(key, request.getString(key, ""));
                }
                wikiProps.put("gridField", field);
                getWikiTagAttrs(request, entry, "display", wikiProps,
                                displayProps);
                String colorTable = getProperty(field, "colortable", null);
                if (colorTable != null) {
                    displayProps.add("colorTable");
                    displayProps.add(Json.quote(colorTable));
                }
                String colorTableMin = getProperty(field, "colortable.min",
                                           null);
                if (colorTableMin != null) {
                    displayProps.add("colorByMin");
                    displayProps.add(Json.quote(colorTableMin));
                }
                String colorTableMax = getProperty(field, "colortable.max",
                                           null);
                if (colorTableMax != null) {
                    displayProps.add("colorByMax");
                    displayProps.add(Json.quote(colorTableMax));
                }

                writer.println(",\"properties\":");
                writer.println(Json.map(displayProps));
                writer.println(",\"data\":[");
                int                   cnt    = 0;
                DoubleFunction<Float> scaler = getScaler(unit);
                long                  t1     = System.currentTimeMillis();
                for (int tIdx = 0; (tIdx < theDates.size()) && (cnt < max);
                        tIdx += timeStride) {
                    Array a = theGrid.readYXData(tIdx, 0);
                    if (debug) {
			System.err.println("\treading time index:" + tIdx + " size:" + a.getSize());
                    }
                    cnt += writeJson(writer, cnt, max, a, Json.quote(theDates.get(tIdx).toString()), points,
                                     scaler);
                }
                writer.println("]}");
                writer.close();
                System.err.println("time:"
                                   + (System.currentTimeMillis() - t1));
                getCdmManager().returnGridDataset(path, gds);
            }
        });

        Result result = new Result(entry.getName() + ".json", in,
                                   "application/json");
        result.setReturnFilename(entry.getName() + ".json");

        return result;

    }


    /**
     * _more_
     *
     * @param writer _more_
     * @param cnt _more_
     * @param max _more_
     * @param a _more_
     * @param dateString _more_
     * @param points _more_
     * @param scaler _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private int writeJson(PrintWriter writer, int cnt, int max, Array a,
                          String dateString, List<LatLonPoint> points,
                          DoubleFunction<Float> scaler)
            throws Exception {
        synchronized (writer) {
            int written = 0;
            for (int i = 0; (i < a.getSize()) && (cnt < max); i++) {
                written++;
                LatLonPoint llp = points.get(i);
                float       v   =
                    (float) scaler.apply((double) a.getFloat(i));
                if (cnt++ > 0) {
                    writer.print(",");
                }
                writer.print("[");
                writer.print((Double.isNaN(v)
                              ? null
                              : v));
                writer.print(",");
                writer.print(dateString);
                writer.print(",");
                writer.print(llp.getLatitude());
                writer.print(",");
                writer.print(llp.getLongitude());
                writer.print("]");
            }

            return written;
        }
    }


    /**
     * Make a time widget for grid subsetting
     *
     * @param request  the Request
     * @param dates    the list of dates
     * @param sb       the HTML to add to
     */
    private void addTimeWidget(Request request, List<CalendarDate> dates,
                               StringBuffer sb) {
        long millis = System.currentTimeMillis();
        if ((dates != null) && (dates.size() > 0)) {
            CalendarDate cd  = dates.get(0);
            Calendar     cal = cd.getCalendar();
            if (cal != null) {
                sb.append(HtmlUtils.hidden(ARG_CALENDAR, cal.toString()));
            }
            List formattedDates = new ArrayList();
            formattedDates.add(new TwoFacedObject("---", ""));
            for (CalendarDate date : dates) {
                //formattedDates.add(getDateHandler().formatDate(request, date.toDate()));
                formattedDates.add(formatDate(request, date));
            }
            String fromDate = request.getUnsafeString(ARG_FROMDATE, "");
            String toDate   = request.getUnsafeString(ARG_TODATE, "");
            sb.append(
                HtmlUtils.formEntry(
                    msgLabel("Time Range"),
                    HtmlUtils.select(ARG_FROMDATE, formattedDates, fromDate)
                    + HtmlUtils.img(getIconUrl(ICON_ARROW))
                    + HtmlUtils.select(ARG_TODATE, formattedDates, toDate)));
        }
        //System.err.println("Times took "
        //                   + (System.currentTimeMillis() - millis) + " ms");
    }

    /**
     * Format a date
     *
     * @param request  the request
     * @param date     the date object (CalendarDate or Date)
     *
     * @return the formatted date
     */
    public String formatDate(Request request, Object date) {
        if (date == null) {
            return BLANK;
        }
        if (date instanceof CalendarDate) {
            String dateFormat = getRepository().getProperty(PROP_DATE_FORMAT,
                                    DateHandler.DEFAULT_TIME_FORMAT);

            return new CalendarDateFormatter(dateFormat).toString(
                (CalendarDate) date);
        } else if (date instanceof Date) {
            return getDateHandler().formatDate(request, (Date) date);
        } else {
            return date.toString();
        }
    }

    /**
     * Sort the grids
     *
     * @param dataset  the grid dataset
     *
     * @return  the grids
     */
    public List<GridDatatype> sortGrids(GridDataset dataset) {
        List tuples = new ArrayList();
        for (GridDatatype grid : dataset.getGrids()) {
            VariableEnhanced var = grid.getVariable();
            tuples.add(new Object[] { var.getShortName().toLowerCase(),
                                      grid });
        }
        tuples = Misc.sortTuples(tuples, true);
        List<GridDatatype> result = new ArrayList<GridDatatype>();
        for (Object[] tuple : (List<Object[]>) tuples) {
            result.add((GridDatatype) tuple[1]);
        }

        return result;
    }


    /**
     * Get the PointFeatureIterator
     *
     * @param input  the dataset
     *
     * @return  the iterator
     *
     * @throws Exception  problem getting the iterator
     */
    public static PointFeatureIterator getPointIterator(
            FeatureDatasetPoint input)
            throws Exception {
        List<FeatureCollection> collectionList =
            input.getPointFeatureCollectionList();
        if (collectionList.size() > 1) {
            throw new IllegalArgumentException(
                "Can't handle point data with multiple collections");
        }
        FeatureCollection      fc         = collectionList.get(0);
        PointFeatureCollection collection = null;
        if (fc instanceof PointFeatureCollection) {
            collection = (PointFeatureCollection) fc;
        } else if (fc instanceof NestedPointFeatureCollection) {
            NestedPointFeatureCollection npfc =
                (NestedPointFeatureCollection) fc;
            collection = npfc.flatten(null, (CalendarDateRange) null);
        } else {
            throw new IllegalArgumentException(
                "Can't handle collection of type " + fc.getClass().getName());
        }

        return collection.getPointFeatureIterator(16384);
    }




    /**
     * Output a point map
     *
     * @param request  the request
     * @param entry  the entry
     *
     * @return  the Result
     *
     * @throws Exception  on badness
     */
    public Result outputPointMap(Request request, Entry entry)
            throws Exception {


        MapInfo map = getRepository().getMapManager().createMap(request,
                          entry, false, null);
        String              path = getPath(request, entry);
        FeatureDatasetPoint pod = getCdmManager().getPointDataset(entry,
                                      path);

        StringBuffer         sb             = new StringBuffer();
        List                 vars           = pod.getDataVariables();
        int                  skip           = request.get(ARG_SKIP, 0);
        int                  max            = request.get(ARG_MAX, 200);

        int                  cnt            = 0;
        int                  total          = 0;
        String               icon = getIconUrl("/icons/pointdata.gif");

        PointFeatureIterator dataIterator   = getPointIterator(pod);

        List                 columnDataList = new ArrayList();
        while (dataIterator.hasNext()) {
            PointFeature po = (PointFeature) dataIterator.next();
            //                ucar.unidata.geoloc.EarthLocation el = po.getLocation();
            ucar.unidata.geoloc.EarthLocation el = po.getLocation();
            if (el == null) {
                continue;
            }
            double lat = el.getLatitude();
            double lon = el.getLongitude();
            if ((lat != lat) || (lon != lon)) {
                continue;
            }
            if ((lat < -90) || (lat > 90) || (lon < -180) || (lon > 180)) {
                continue;
            }
            total++;
            if (total <= skip) {
                continue;
            }
            if (total > (max + skip)) {
                continue;
            }
            cnt++;
            List          columnData = new ArrayList();
            StructureData structure  = po.getData();
            StringBuffer  info       = new StringBuffer("");
            info.append("<b>Date:</b> " + po.getNominalTimeAsCalendarDate()
                        + "<br>");
            for (VariableSimpleIF var : (List<VariableSimpleIF>) vars) {
                //{name:\"Ashley\",breed:\"German Shepherd\",age:12}
                StructureMembers.Member member =
                    structure.findMember(var.getShortName());
                if ((var.getDataType() == DataType.STRING)
                        || (var.getDataType() == DataType.CHAR)) {
                    String value = structure.getScalarString(member);
                    columnData.add(var.getShortName() + ":"
                                   + HtmlUtils.quote(value));
                    info.append("<b>" + var.getShortName() + ": </b>" + value
                                + "</br>");

                } else {
                    float value = structure.convertScalarFloat(member);
                    info.append("<b>" + var.getShortName() + ": </b>" + value
                                + "</br>");

                    columnData.add(var.getShortName() + ":" + value);
                }
            }
            columnDataList.add("{" + StringUtil.join(",", columnData)
                               + "}\n");
            map.addMarker("",
                          new LatLonPointImpl(el.getLatitude(),
                              el.getLongitude()), icon, "", info.toString());
        }



        List columnDefs  = new ArrayList();
        List columnNames = new ArrayList();
        for (VariableSimpleIF var : (List<VariableSimpleIF>) vars) {
            columnNames.add(HtmlUtils.quote(var.getShortName()));
            String label = var.getDescription();
            //            if(label.trim().length()==0)
            label = var.getShortName();
            columnDefs.add("{key:" + HtmlUtils.quote(var.getShortName())
                           + "," + "sortable:true," + "label:"
                           + HtmlUtils.quote(label) + "}");
        }


        if (total > max) {
            sb.append((skip + 1) + "-" + (skip + cnt) + " of " + total + " ");
        } else {
            sb.append((skip + 1) + "-" + (skip + cnt));
        }
        if (total > max) {
            boolean didone = false;
            if (skip > 0) {
                sb.append(HtmlUtils.space(2));
                sb.append(
                    HtmlUtils.href(
                        HtmlUtils.url(request.getRequestPath(), new String[] {
                    ARG_OUTPUT, request.getOutput().toString(), ARG_ENTRYID,
                    entry.getId(), ARG_SKIP, "" + (skip - max), ARG_MAX,
                    "" + max
                }), msg("Previous")));
                didone = true;
            }
            if (total > (skip + cnt)) {
                sb.append(HtmlUtils.space(2));
                sb.append(
                    HtmlUtils.href(
                        HtmlUtils.url(request.getRequestPath(), new String[] {
                    ARG_OUTPUT, request.getOutput().toString(), ARG_ENTRYID,
                    entry.getId(), ARG_SKIP, "" + (skip + max), ARG_MAX,
                    "" + max
                }), msg("Next")));
                didone = true;
            }
            //Just come up with some max number
            if (didone && (total < 2000)) {
                sb.append(HtmlUtils.space(2));
                sb.append(
                    HtmlUtils.href(
                        HtmlUtils.url(request.getRequestPath(), new String[] {
                    ARG_OUTPUT, request.getOutput().toString(), ARG_ENTRYID,
                    entry.getId(), ARG_SKIP, "" + 0, ARG_MAX, "" + total
                }), msg("All")));

            }
        }
        map.center();
        sb.append(map.getHtml());
        getCdmManager().returnPointDataset(path, pod);

        return new Result(msg("Point Data Map"), sb);
    }


    /** Fixed var name for lat */
    public static final String VAR_LATITUDE = "Latitude";

    /** Fixed var name for lon */
    public static final String VAR_LONGITUDE = "Longitude";

    /** Fixed var name for alt */
    public static final String VAR_ALTITUDE = "Altitude";

    /** Fixed var name for time */
    public static final String VAR_TIME = "Time";



    /**
     * Get the 1D values for an array as floats.
     *
     * @param arr   Array of values
     * @return  float representation
     */
    public static float[] toFloatArray(Array arr) {
        Object dst       = arr.get1DJavaArray(float.class);
        Class  fromClass = dst.getClass().getComponentType();
        if (fromClass.equals(float.class)) {
            //It should always be a float
            return (float[]) dst;
        } else {
            float[] values = new float[(int) arr.getSize()];
            if (fromClass.equals(byte.class)) {
                byte[] fromArray = (byte[]) dst;
                for (int i = 0; i < fromArray.length; ++i) {
                    values[i] = fromArray[i];
                }
            } else if (fromClass.equals(short.class)) {
                short[] fromArray = (short[]) dst;
                for (int i = 0; i < fromArray.length; ++i) {
                    values[i] = fromArray[i];
                }
            } else if (fromClass.equals(int.class)) {
                int[] fromArray = (int[]) dst;
                for (int i = 0; i < fromArray.length; ++i) {
                    values[i] = fromArray[i];
                }
            } else if (fromClass.equals(double.class)) {
                double[] fromArray = (double[]) dst;
                for (int i = 0; i < fromArray.length; ++i) {
                    values[i] = (float) fromArray[i];
                }
            } else {
                throw new IllegalArgumentException("Unknown array type:"
                        + fromClass.getName());
            }

            return values;
        }

    }


    /**
     * Output a trajectory map
     *
     * @param request  the request
     * @param entry    the entry
     *
     * @return  the result
     *
     * @throws Exception  on badness
     */
    public Result outputTrajectoryMap(Request request, Entry entry)
            throws Exception {
        String               path = getPath(request, entry);
        TrajectoryObsDataset tod  =
            getCdmManager().getTrajectoryDataset(path);
        StringBuffer         sb   = new StringBuffer();

        MapInfo map = getRepository().getMapManager().createMap(request,
                          entry, "800", "600", false, null);
        List trajectories = tod.getTrajectories();
        //TODO: Use new openlayers map
        for (int i = 0; i < trajectories.size(); i++) {
            List allVariables = tod.getDataVariables();
            TrajectoryObsDatatype todt =
                (TrajectoryObsDatatype) trajectories.get(i);
            float[] lats    = toFloatArray(todt.getLatitude(null));
            float[] lons    = toFloatArray(todt.getLongitude(null));
            float   lastLat = 0,
                    lastLon = 0;
            int     stride  = lats.length / 500;
            for (int ptIdx = 0; ptIdx < lats.length; ptIdx += stride) {
                float lat = lats[ptIdx];
                float lon = lons[ptIdx];
                if (ptIdx > 0) {
                    if (ptIdx + stride >= lats.length) {
                        map.addMarker("", lat, lon, null, "",
                                      "End time:" + todt.getEndDate());
                    }
                    //#FF0000
                    map.addLine(entry, "", lastLat, lastLon, lat, lon, null);
                } else {
                    map.addMarker("", lat, lon, null, "",
                                  "Start time:" + todt.getEndDate());
                }
                lastLat = lat;
                lastLon = lon;
            }
            StructureData    structure = todt.getData(0);
            VariableSimpleIF theVar    = null;
            for (int varIdx = 0; varIdx < allVariables.size(); varIdx++) {
                VariableSimpleIF var =
                    (VariableSimpleIF) allVariables.get(varIdx);
                if (var.getRank() != 0) {
                    continue;
                }
                theVar = var;

                break;
            }
            if (theVar == null) {
                continue;
            }
        }

        map.centerOn(entry);
        sb.append(map.getHtml());
        getCdmManager().returnTrajectoryDataset(path, tod);

        return new Result(msg("Trajectory Map"), sb);


    }






    /**
     * Make the Point Subset form
     *
     * @param request   the Request
     * @param entry     the Entry
     * @param suffix    the type as a suffix
     *
     * @return the Result
     *
     * @throws Exception _more_
     */
    private Result makePointSubsetForm(Request request, Entry entry,
                                       String suffix)
            throws Exception {
        StringBuffer sb      = new StringBuffer();
        String       formUrl =
            request.makeUrl(getRepository().URL_ENTRY_SHOW);
        sb.append(HtmlUtils.form(formUrl + suffix));
        sb.append(HtmlUtils.submit("Subset Point Data", ARG_SUBMIT));
        sb.append(HtmlUtils.hidden(ARG_OUTPUT,
                                   request.getString(ARG_OUTPUT, "")));
        sb.append(HtmlUtils.hidden(ARG_ENTRYID, entry.getId()));
        sb.append(HtmlUtils.formTable());
        List<TwoFacedObject> formats = new ArrayList<TwoFacedObject>();
        formats.add(new TwoFacedObject("CSV", FORMAT_CSV));
        formats.add(new TwoFacedObject("KML", FORMAT_KML));
        String format = request.getString(CdmConstants.ARG_FORMAT,
                                          FORMAT_CSV);
        sb.append(
            HtmlUtils.formEntry(
                msgLabel("Format"),
                HtmlUtils.select(CdmConstants.ARG_FORMAT, formats, format)));

        MapInfo map = getRepository().getMapManager().createMap(request,
                          entry, true, null);
        map.addBox(entry, new MapBoxProperties("blue", false, true));
        map.centerOn(entry);
        String llb = map.makeSelector(ARG_POINT_BBOX, true, null);
        sb.append(HtmlUtils.formEntryTop(msgLabel("Location"), llb));


        sb.append(HtmlUtils.formTableClose());
        sb.append(HtmlUtils.submit("Subset Point Data", ARG_SUBMIT));
        sb.append(HtmlUtils.formClose());

        return new Result("", sb);
    }


    /**
     * Get the services for the request
     *
     * @param request  the Request
     * @param entry    the Entry
     * @param services  the list of services
     */
    @Override
    public void getServiceInfos(Request request, Entry entry,
                                List<ServiceInfo> services) {
        super.getServiceInfos(request, entry, services);
        if ( !getCdmManager().canLoadAsCdm(entry)) {
            return;
        }
        //Add the GridPoint service
        if (getCdmManager().canLoadAsCdmGrid(entry)) {
            String url = getRepository().getUrlBase() + "/grid/json?"
                         + HtmlUtils.args(new String[] {
                ARG_ENTRYID, entry.getId(), ARG_LOCATION_LATITUDE,
                "${latitude}", ARG_LOCATION_LONGITUDE, "${longitude}"
            }, false);

            services.add(
                new ServiceInfo(
                    "grid.point.json",
                    "Point time series - " + entry.getName(),
                    request.getAbsoluteUrl(url),
                    request.getAbsoluteUrl(getIconUrl("/icons/chart.png"))));
        }

        String url = getAbsoluteOpendapUrl(request, entry);
        services.add(
            new ServiceInfo(
                "opendap", "OPeNDAP Link", url,
                request.getAbsoluteUrl(getIconUrl(ICON_OPENDAP))));
    }


    /**
     * Output a point subset
     *
     * @param request  the Request
     * @param entry    the Entry
     *
     * @return  the Result
     *
     * @throws Exception problem making subset
     */
    public Result outputPointSubset(Request request, Entry entry)
            throws Exception {
        if ( !request.defined(CdmConstants.ARG_FORMAT)) {
            return makePointSubsetForm(request, entry, "");
        }
        String format = request.getString(CdmConstants.ARG_FORMAT,
                                          FORMAT_CSV);

        if (format.equals(FORMAT_CSV)) {
            request.getHttpServletResponse().setContentType("text/csv");
            request.setReturnFilename(IOUtil.stripExtension(entry.getName())
                                      + SUFFIX_CSV);
        } else {
            request.getHttpServletResponse().setContentType(
                "application/vnd.google-earth.kml+xml");
            request.setReturnFilename(IOUtil.stripExtension(entry.getName())
                                      + ".kml");
        }
        //        System.out.println("name: " + request);


        OutputStream os = request.getHttpServletResponse().getOutputStream();
        PrintWriter  pw = new PrintWriter(os);

        if (format.equals(FORMAT_CSV)) {
            outputPointCsv(request, entry, pw);
        } else {
            outputPointKml(request, entry, pw);
        }

        pw.close();
        Result result = new Result();
        result.setNeedToWrite(false);

        return result;
    }


    /**
     * Output the point data as CSV
     *
     * @param request  the Request
     * @param entry    the Entry
     * @param pw       the PrintWriter
     *
     *
     * @throws Exception  problem getting data
     */
    private void outputPointCsv(Request request, Entry entry, PrintWriter pw)
            throws Exception {
        String path = getPath(request, entry);
        FeatureDatasetPoint pod = getCdmManager().getPointDataset(entry,
                                      path);;
        List                 vars         = pod.getDataVariables();
        PointFeatureIterator dataIterator = getPointIterator(pod);
        int                  cnt          = 0;
        while (dataIterator.hasNext()) {
            PointFeature po = (PointFeature) dataIterator.next();
            ucar.unidata.geoloc.EarthLocation el = po.getLocation();
            if (el == null) {
                continue;
            }
            cnt++;

            double        lat       = el.getLatitude();
            double        lon       = el.getLongitude();
            StructureData structure = po.getData();

            if (cnt == 1) {
                pw.print(HtmlUtils.quote("Time"));
                pw.print(",");
                pw.print(HtmlUtils.quote("Latitude"));
                pw.print(",");
                pw.print(HtmlUtils.quote("Longitude"));
                for (VariableSimpleIF var : (List<VariableSimpleIF>) vars) {
                    pw.print(",");
                    String unit = var.getUnitsString();
                    if (unit != null) {
                        pw.print(HtmlUtils.quote(var.getShortName() + " ("
                                + unit + ")"));
                    } else {
                        pw.print(HtmlUtils.quote(var.getShortName()));
                    }
                }
                pw.print("\n");
            }

            pw.print(HtmlUtils.quote("" + po.getNominalTimeAsCalendarDate()));
            pw.print(",");
            pw.print(el.getLatitude());
            pw.print(",");
            pw.print(el.getLongitude());

            for (VariableSimpleIF var : (List<VariableSimpleIF>) vars) {
                StructureMembers.Member member =
                    structure.findMember(var.getShortName());
                pw.print(",");
                if ((var.getDataType() == DataType.STRING)
                        || (var.getDataType() == DataType.CHAR)) {
                    pw.print(
                        HtmlUtils.quote(structure.getScalarString(member)));
                } else {
                    pw.print(structure.convertScalarFloat(member));
                }
            }
            pw.print("\n");
        }
        getCdmManager().returnPointDataset(path, pod);

    }


    /**
     * Output the points as KML
     *
     * @param request  the Request
     * @param entry    the Entry
     * @param pw       the PrintWriter
     *
     *
     * @throws Exception problem generating KML
     */
    private void outputPointKml(Request request, Entry entry, PrintWriter pw)
            throws Exception {
        String              path = getPath(request, entry);
        FeatureDatasetPoint pod = getCdmManager().getPointDataset(entry,
                                      path);
        List                 vars         = pod.getDataVariables();
        PointFeatureIterator dataIterator = getPointIterator(pod);

        Element              root         = KmlUtil.kml(entry.getName());
        Element              docNode = KmlUtil.document(root,
                                           entry.getName());

        while (dataIterator.hasNext()) {
            PointFeature po = (PointFeature) dataIterator.next();
            ucar.unidata.geoloc.EarthLocation el = po.getLocation();
            if (el == null) {
                continue;
            }
            double lat = el.getLatitude();
            double lon = el.getLongitude();
            double alt = 0;
            if ((lat != lat) || (lon != lon)) {
                continue;
            }

            StructureData structure = po.getData();
            StringBuffer  info      = new StringBuffer("");
            info.append("<b>Date:</b> " + po.getNominalTimeAsCalendarDate()
                        + "<br>");
            for (VariableSimpleIF var : (List<VariableSimpleIF>) vars) {
                StructureMembers.Member member =
                    structure.findMember(var.getShortName());
                if ((var.getDataType() == DataType.STRING)
                        || (var.getDataType() == DataType.CHAR)) {
                    info.append("<b>" + var.getShortName() + ": </b>"
                                + structure.getScalarString(member) + "<br>");
                } else {
                    info.append("<b>" + var.getShortName() + ": </b>"
                                + structure.convertScalarFloat(member)
                                + "<br>");

                }
            }
            KmlUtil.placemark(docNode,
                              "" + po.getNominalTimeAsCalendarDate(),
                              info.toString(), lat, lon, alt, null);
        }
        pw.print(XmlUtil.toString(root));
        getCdmManager().returnPointDataset(path, pod);
    }


    /**
     * Get the Authorization method
     *
     * @param request  the Request
     *
     * @return  the autorization method
     */
    public AuthorizationMethod getAuthorizationMethod(Request request) {
        OutputType output = request.getOutput();
        if (output.equals(OUTPUT_WCS) || output.equals(OUTPUT_OPENDAP)) {
            return AuthorizationMethod.AUTH_HTTP;
        }

        return super.getAuthorizationMethod(request);
    }




    /**
     * Output a group of entries
     *
     * @param request     the Request
     * @param outputType  the output type
     * @param group       the group
     * @param subGroups   the subgroups
     * @param entries     the List of Entrys
     *
     * @return  the Result
     *
     * @throws Exception  problem outputting group
     */
    public Result outputGroup(Request request, OutputType outputType,
                              Entry group, List<Entry> subGroups,
                              List<Entry> entries)
            throws Exception {
        if (getCdmManager().isAggregation(group)) {
            return outputEntry(request, outputType, group);
        }

        //        System.err.println("group:" + group + " " + group.getType());
        return super.outputGroup(request, outputType, group, subGroups,
                                 entries);
    }


    /**
     * Serve up the entry
     *
     * @param request     the Request
     * @param outputType  the output type
     * @param entry       the Entry
     *
     * @return the Result
     *
     * @throws Exception On badness
     */
    public Result outputEntry(Request request, OutputType outputType,
                              Entry entry)
            throws Exception {

        if ( !getRepository().getAccessManager().canDoAction(request, entry,
                Permission.ACTION_FILE)) {
            throw new AccessException("Cannot access data", request);
        }

        if ( !getRepository().getAccessManager().canAccessFile(request,
                entry)) {
            throw new AccessException("Cannot access data", request);
        }

        if (outputType.equals(OUTPUT_CDL)) {
            return outputCdl(request, entry);
        }
        if (outputType.equals(OUTPUT_WCS)) {
            return outputWcs(request, entry);
        }
        if (outputType.equals(OUTPUT_GRIDSUBSET_FORM)) {
            return outputGridSubsetForm(request, entry);
        }
        if (outputType.equals(OUTPUT_JSON)) {
            return outputGridJson(request, entry);
        }

        if (outputType.equals(OUTPUT_GRIDSUBSET)) {
            return outputGridSubset(request, entry);
        }

        if (outputType.equals(OUTPUT_TRAJECTORY_MAP)) {
            return outputTrajectoryMap(request, entry);
        }

        if (outputType.equals(OUTPUT_POINT_MAP)) {
            return outputPointMap(request, entry);
        }

        if (outputType.equals(OUTPUT_POINT_SUBSET)) {
            return outputPointSubset(request, entry);
        }
        if (outputType.equals(OUTPUT_OPENDAP)) {
            //If its a head request then just return the content description
            if (request.isHeadRequest()) {
                Result result = new Result("", new StringBuffer());
                result.addHttpHeader(HtmlUtils.HTTP_CONTENT_DESCRIPTION,
                                     "dods-dds");

                return result;
            }
            Result result = outputOpendap(request, entry);

            return result;
        }

        throw new IllegalArgumentException("Unknown output type:"
                                           + outputType);
    }

    /**
     * Get the OPeNDAP handler
     *
     * @return  the handler
     */
    public OpendapApiHandler getOpendapHandler() {
        return (OpendapApiHandler) getRepository().getApiManager()
            .getApiHandler(OpendapApiHandler.API_ID);
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param sb _more_
     * @param type _more_
     * @param target _more_
     *
     * @throws Exception _more_
     */
    public void addToSelectMenu(Request request, Entry entry,
                                StringBuilder sb, String type, String target)
            throws Exception {

        String                 path      = getPath(request, entry);
        GridDataset gds = getCdmManager().getGridDataset(entry, path);
        List<VariableSimpleIF> variables = gds.getDataVariables();
        for (VariableSimpleIF var : variables) {
            sb.append("&nbsp;");
            sb.append(
                HtmlUtils.mouseClickHref(
                    HtmlUtils.call(
                        "selectClick",
                        HtmlUtils.comma(
                            HtmlUtils.squote(target),
                            HtmlUtils.squote(entry.getId()),
                            HtmlUtils.squote(var.getShortName()),
                            HtmlUtils.squote(type))), var.getDescription()));
            sb.append("<br>");
        }
    }






    /**
     * Output OPeNDAP
     *
     * @param request   the Request
     * @param entry     the Entry
     *
     * @return the Result
     *
     * @throws Exception  problems
     */
    public Result outputOpendap(final Request request, final Entry entry)
            throws Exception {

        request.remove(ARG_ENTRYID);
        request.remove(ARG_OUTPUT);

        //Get the file location for the entry
        String location = getPath(request, entry);

        //Get the ncFile from the pool

        NetcdfFile ncFile = getCdmManager().createNetcdfFile(location);
        opendapCounter.incr();

        //Bridge the ramadda servlet to the opendap servlet
        NcDODSServlet servlet = new NcDODSServlet(request, entry, ncFile) {
            @Override
            public ServletConfig getServletConfig() {
                return request.getHttpServlet().getServletConfig();
            }
            @Override
            public ServletContext getServletContext() {
                /*
                System.err.println(
                    "getServletContext:"
                    + request.getHttpServlet().getServletContext());
                System.err
                    .println("getServletContext:"
                             + request.getHttpServlet().getServletContext()
                                 .getRealPath("/"));
                */

                return request.getHttpServlet().getServletContext();
            }
            @Override
            public String getServletInfo() {
                return request.getHttpServlet().getServletInfo();
            }
            @Override
            public Enumeration getInitParameterNames() {
                return request.getHttpServlet().getInitParameterNames();
            }
        };

        //If we are running as a normal servlet then init the ncdods servlet with the servlet config info
        if ((request.getHttpServlet() != null)
                && (request.getHttpServlet().getServletConfig() != null)) {
            servlet.init(request.getHttpServlet().getServletConfig());
        }

        //Do the work
        servlet.doGet(request.getHttpServletRequest(),
                      request.getHttpServletResponse());
        //We have to pass back a result though we set needtowrite to false because the opendap servlet handles the writing
        Result result = new Result("");
        result.setNeedToWrite(false);
        opendapCounter.decr();
        getCdmManager().returnNetcdfFile(location, ncFile);

        return result;
    }


    /**
     * NcDODSServlet to wrap the OPeNDAP servelet
     *
     */
    public class NcDODSServlet extends opendap.servlet.AbstractServlet {


        /** repository request */
        Request repositoryRequest;

        /** the NetcdfFile object */
        NetcdfFile ncFile;

        /** the Entry */
        Entry entry;

        /**
         * Construct a new NcDODSServlet
         *
         * @param request the Request
         * @param entry   the Entry
         * @param ncFile  the NetcdfFile object
         */
        public NcDODSServlet(Request request, Entry entry,
                             NetcdfFile ncFile) {
            this.repositoryRequest = request;
            this.entry             = entry;
            this.ncFile            = ncFile;
        }

        /**
         * Make the dataset
         *
         * @param preq preq
         *
         * @return The dataset
         *
         * @throws DAP2Exception On badness
         * @throws IOException On badness
         */
        protected GuardedDataset getDataset(ReqState preq)
                throws DAP2Exception, IOException /*, ParseException*/ {
            HttpServletRequest request = preq.getRequest();
            String             reqPath = entry.getName();

            try {
                GuardedDatasetImpl guardedDataset =
                    new GuardedDatasetImpl(reqPath, ncFile, true);

                return guardedDataset;
            } catch (Exception exc) {
                throw new WrapperException(exc);
            }
        }

        /**
         * Get the server version
         *
         * @return  the version
         */
        public String getServerVersion() {
            return "opendap/3.7";
        }

    }





}
