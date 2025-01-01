
/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
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
import org.ramadda.repository.map.MapProperties;
import org.ramadda.repository.map.MapInfo;
import org.ramadda.repository.output.OutputHandler;
import org.ramadda.repository.output.OutputType;
import org.ramadda.repository.type.TypeHandler;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.SelectionRectangle;
import org.ramadda.util.Utils;

import org.ramadda.util.geo.KmlUtil;

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
@SuppressWarnings({ "unchecked", "deprecation" })
public class CdmDataOutputHandler extends CdmOutputHandler implements CdmConstants {

    /** _more_ */
    private static final boolean debug = false;

    /**  */
    public static final boolean debugOpendap = false;

    public static final String ARG_GRIDFIELD="gridField";

    /** set of suffixes */
    private HashSet<String> suffixSet;

    /** hash of patterns */
    private Hashtable<String, List<Pattern>> patterns;

    /** not patterns */
    private Hashtable<String, List<Pattern>> notPatterns;

    /** OPeNDAP Output Type */
    public static final OutputType OUTPUT_OPENDAP =
        new OutputType("OPeNDAP", "data.opendap",
                       OutputType.TYPE_FEEDS | OutputType.TYPE_IMPORTANT|OutputType.TYPE_SERVICE,
                       OutputType.SUFFIX_NONE, ICON_OPENDAP, GROUP_DATA);

    /** CDL Output Type */
    public static final OutputType OUTPUT_CDL =
        new OutputType("File Metadata", "data.cdl",
		       OutputType.TYPE_OTHER|OutputType.TYPE_IMPORTANT|OutputType.TYPE_SERVICE,
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


    /** Radar map Output Type */
    public static final OutputType OUTPUT_RADAR_MAP =
        new OutputType("Show radar on Map", "data.radar.map",
                       OutputType.TYPE_OTHER, OutputType.SUFFIX_NONE,
                       ICON_MAP, GROUP_DATA);

    /** Grid subset form Output Type */
    public static final OutputType OUTPUT_GRIDSUBSET_FORM =
        new OutputType("Subset Grid", "data.gridsubset.form",
                       OutputType.TYPE_OTHER | OutputType.TYPE_IMPORTANT|OutputType.TYPE_SERVICE,
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
	    System.err.println("can't access file");
            return;
        }

        long    t1           = System.currentTimeMillis();
        boolean canLoadAsCdm = getCdmManager().canLoadAsCdm(entry);

        if ( !canLoadAsCdm) {
            return;
        }

	//	System.err.println("cdm.getlinks-can load as grid:" + getCdmManager().canLoadAsGrid(entry) + " " + getCdmManager().canLoadAsCdmGrid(entry));
        if (getCdmManager().canLoadAsCdmGrid(entry) ||getCdmManager().canLoadAsGrid(entry)) {
            addOutputLink(request, entry, links, OUTPUT_GRIDSUBSET_FORM);
            addOutputLink(request, entry, links,
                          GridPointOutputHandler.OUTPUT_GRIDASPOINT_FORM);
            addOutputLink(request, entry, links, OUTPUT_JSON);
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
            if (getRepository().getAccessManager().canDoEdit(request,
                    entry)) {
                sb.append(HU.p());
                List<Entry> entries = (List<Entry>) Misc.newList(entry);
                getEntryManager().addInitialMetadata(request, entries, false,
                        request.get(ARG_SHORT, false));
                getEntryManager().updateEntries(request, entries);
                getPageHandler().entrySectionOpen(request, entry, sb, "");
                sb.append(
                    getPageHandler().showDialogNote("Properties added"));
                sb.append(
                    getRepository().getHtmlOutputHandler().getInformationTabs(
									      request, entry, false,null,true,null));
                getPageHandler().entrySectionClose(request, entry, sb);

            } else {
                sb.append("You cannot add properties");
            }

            return makeLinksResult(request, "NetCDF File Metadata", sb,
                                   new State(entry));
        }


        getPageHandler().entrySectionOpen(request, entry, sb, "");
        sb.append("<center>");
        if (getRepository().getAccessManager().canDoEdit(request, entry)) {
            request.put(ARG_METADATA_ADD, HU.VALUE_TRUE);
            sb.append(
                HU.href(
                    request.getUrl() + "&"
                    + HU.arg(ARG_SHORT, HU.VALUE_TRUE), msg(
                        "Add temporal and spatial properties")));
            sb.append(
                HU.span(
                    "&nbsp;|&nbsp;",
                    HU.cssClass(CSS_CLASS_SEPARATOR)));

            sb.append(HU.href(request.getUrl(),
                                     msg("Add full properties")));
            sb.append(
                HU.span(
                    "&nbsp;|&nbsp;",
                    HU.cssClass(CSS_CLASS_SEPARATOR)));
        }

        String tail =
            IOUtil.stripExtension(getStorageManager().getFileTail(entry));

        sb.append(HU.href(HU.url(getRepository().URL_ENTRY_SHOW
                + "/" + tail + SUFFIX_NCML, new String[] {
            ARG_ENTRYID, entry.getId(), ARG_OUTPUT, OUTPUT_CDL.getId(),
            CdmConstants.ARG_FORMAT, FORMAT_NCML
        }), "NCML"));
        sb.append("</center>");

        sb.append("\n<p>\n");
        GridDataset gds = getCdmManager().getGridDataset(entry, path);
        if (gds != null) {
            List<GridDatatype> grids = sortGrids(gds);
            sb.append(
                "<table class='ramadda-table stripe'><thead><tr><th>Variable</th><th>Unit</th><th>Dimensions</th><th>#Times</th></tr></thead><tbody>");
            List<VariableSimpleIF> variables = gds.getDataVariables();
            for (GridDatatype gdt : grids) {
                Dimension tdim = gdt.getTimeDimension();
                Dimension xdim = gdt.getXDimension();
                Dimension ydim = gdt.getYDimension();
                Dimension zdim = gdt.getZDimension();
                sb.append("<tr>");
                sb.append(HU.td(gdt.getShortName()));
                sb.append(HU.td(gdt.getUnitsString()));
                sb.append("<td>");
                sb.append(xdim.getLength() + "x" + ydim.getLength()
                          + ((zdim != null)
                             ? "x" + zdim.getLength()
                             : ""));
                sb.append("</td>");
		if(tdim!=null)
		    sb.append(HU.td("" + tdim.getLength()));
		else
		    sb.append(HU.td("--"));
                sb.append("</tr>");
            }
            sb.append("</tbody></table>");
            sb.append("\n");
        }

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



    public GeoGrid getGrid(Request request,GridDataset gds) throws Exception {
        String field = request.getString(ARG_GRIDFIELD, (String) null);
        if ((field == null) || (field.length() == 0)) {
            field = gds.getDataVariables().get(0).getShortName();
        }
        GeoGrid     grid = null;
	if(field.startsWith("#")) {
	    int index=Integer.parseInt(field.substring(1).trim())-1;
	    List<GridDatatype> grids= gds.getGrids();
	    if(index<0 || index>=grids.size()) return null;
	    grid = (GeoGrid)grids.get(index);
	} else {
	    grid = (GeoGrid) gds.findGridByName(field);
	}
	if(grid==null) System.err.println("Cannot find grid field:" + field);
	return grid;
    }


    public void getWikiTagAttrs(Request request, Entry entry, String tag,
                                Hashtable props, List<String> displayProps)
            throws Exception {

        List<String> all   = new ArrayList<String>();
        String      path = getPath(request, entry);
        GridDataset gds= getCdmManager().getGridDataset(entry, path);
        GeoGrid     grid = getGrid(request, gds);
        if (grid == null) {
            return;
        }
	String field = grid.getName();
        GridCoordSystem        gcs       = grid.getCoordinateSystem();
        CoordinateAxis         xaxis     = gcs.getXHorizAxis();
        CoordinateAxis         yaxis     = gcs.getYHorizAxis();
        CoordinateAxis1D       compAxis  = null;
        List<CalendarDate>     dates     = getGridDates(gds);

        List<VariableSimpleIF> variables = gds.getDataVariables();
        all.add(ARG_GRIDFIELD);
        String vars = null;
        for (VariableSimpleIF var : variables) {
            if (vars == null) {
                vars = "";
            } else {
                vars += ",";
            }
	    //	    if(getProperty(var.getShortName(), "colortable", null) == null) {
		//		System.out.println(var.getShortName().toLowerCase()+".alias=");
	    //	    }

	    String name = var.getShortName().replaceAll(",","\\\\,");
	    String desc = var.getDescription().replaceAll(",","\\\\,");	    
	    //	    System.err.println("VAR:" + name +" " + desc);
            vars += name + ":" + desc;
        }
        displayProps.add("request.gridField.includeAll");
        displayProps.add("false");
        displayProps.add("request.gridField.label");
        displayProps.add(JsonUtil.quote("Field"));
        displayProps.add("request.gridField.values");
        displayProps.add(JsonUtil.quote(vars));

        if (dates.size() > 0) {
            all.add("gridTime");
            displayProps.add("request.gridTime.includeAll");
            displayProps.add("false");
            displayProps.add("request.gridTime.label");
            displayProps.add(JsonUtil.quote("Time"));
            displayProps.add("request.gridTime.values");
            String v = "-1" + ":" + "All";
            for (int i = 0; i < dates.size(); i++) {
                v += ",";
                v += i + ":" + dates.get(i);
            }
            displayProps.add(JsonUtil.quote(v));
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
                displayProps.add(JsonUtil.quote("Grid Level"));
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
                displayProps.add(JsonUtil.quote(v));
            }
        }
        all.add("gridStride");
        displayProps.add("request.gridStride.includeAll");
        displayProps.add("false");
        displayProps.add("request.gridStride.label");
        displayProps.add(JsonUtil.quote("Stride"));
        displayProps.add("request.gridStride.values");
        displayProps.add(JsonUtil.quote("-1:default,1,2,3,4,5,6,7,8,10"));
        String stride = (String) props.get("gridStride");
        if (stride != null) {
            displayProps.add("request.gridStride.default");
            displayProps.add(JsonUtil.quote(stride));
        }
        String level = (String) props.get("gridLevel");
        if (level != null) {
            displayProps.add("request.gridLevel.default");
            displayProps.add(JsonUtil.quote(level));
        }
        if (field != null) {
            displayProps.add("request.gridField.default");
            displayProps.add(JsonUtil.quote(field));
        }


        //      displayProps.add("request." + column.getName() + ".urlarg");
        //      displayProps.add(JsonUtil.quote(column.getSearchArg()));

        displayProps.add("requestFields");
        displayProps.add(JsonUtil.quote(Utils.join(all, ",")));

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
                HU.attr(HU.ATTR_ONCLICK,
                               HU.call("HU.checkboxClicked",
                                   HU.comma("event",
                                       HU.squote(ARG_VAR),
                                       HU.squote(cbxId))));
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

	    String units = var.getUnitsString() != null? 
		"(" + var.getUnitsString() + ")"
		: "";
	    //	    String label = var.getShortName() + HU.SPACE

	    String desc = Utils.getDefined("",var.getDescription(),var.getShortName())+HU.SPACE+units;
	    String label = HU.span(desc,HU.attrs("title",var.getShortName()));
	    HU.div(sbToUse, HU.labeledCheckbox(ARG_VAR, var.getShortName(),
					       (grids.size() == 1),
					       HU.cssClass("ramadda-grid-variable") +HU.id(cbxId) + call,label),
		   HU.style(HU.css("white-space","nowrap","max-width","100%","overflow-x","hidden")));
        }
	String toggleAllId = HU.getUniqueId("cbx_");
	HU.div(varSB,HU.labeledCheckbox("", HU.VALUE_TRUE,
					false,HU.attr("id",toggleAllId),
					"Toggle all"),"");



        if (varSB2D.length() > 0) {
            if (varSB3D.length() > 0) {
                varSB.append(HU.div(HU.b("2D Grids"),""));
            }
            varSB.append(varSB2D);
        }
        if (varSB3D.length() > 0) {
            if ((varSB2D.length() > 0) || withLevelSelector) {
                String header = " 3D Grids";
                if (withLevelSelector) {
                    if ( !haveOneVerticalCS && !onlyIfAllLevelsEqual) {
                        header += HU.space(3) + "Level:"
                                  + HU.space(1)
                                  + HU.input(ARG_LEVEL, "");
                    } else if (haveOneVerticalCS) {
                        header += HU.space(3) + HU.b("Level:")
                                  + HU.space(1);
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
                        header += HU.select(ARG_LEVEL, selObjs)
                                  + HU.space(2) + "("
                                  + compAxis.getUnitsString() + ")";
                    }
                }
                varSB.append(HU.div(header,""));
            }
            varSB.append(varSB3D);
        }

	varSB.append("\n");
	HU.script(varSB,"HtmlUtils.initToggleAll('" + toggleAllId+"','.ramadda-grid-variable');");
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

        StringBuffer sb      = new StringBuffer();
        getPageHandler().entrySectionOpen(request, entry, sb, "Subset Grid");
	makeGridSubsetForm(request,entry,sb);
        getPageHandler().entrySectionClose(request, entry, sb);
        return makeLinksResult(request, msg("Grid Subset"), sb,
                               new State(entry));

    }

    public void makeGridSubsetForm(Request request, Entry entry,StringBuffer sb)
	throws Exception {

        boolean canAdd = getRepository().getAccessManager().canDoNew(request,
                             entry.getParentEntry());

        String       path    = getPath(request, entry);
        GridDataset dataset      = getCdmManager().getGridDataset(entry, path);
	if(dataset==null) {
	    sb.append(getPageHandler().showDialogWarning("No grids found"));
	    return;
	}




        String       formUrl =
            request.makeUrl(getRepository().URL_ENTRY_SHOW);
        String fileName = IOUtil.stripExtension(entry.getName())
                          + "_subset.nc";

        String formId = HU.getUniqueId("form_");


        sb.append(HU.formPost(formUrl + "/" + fileName,
                                     HU.id(formId)));
        sb.append(HU.br());

        sb.append(HU.submit("Subset Grid", ARG_SUBMIT));
        sb.append(HU.br());
        sb.append(HU.hidden(ARG_OUTPUT, OUTPUT_GRIDSUBSET));
        sb.append(HU.hidden(ARG_ENTRYID, entry.getId()));
        sb.append(HU.div("Select Subset Parameters",HU.cssClass("ramadda-table-header")+HU.style("margin-top:6px;")));
        sb.append(HU.formTable());
	List<String> strides = new ArrayList<String>();
 	for(int i=1;i<=100;i++) strides.add(""+i);
        HU.formEntry(sb,msgLabel("Horizontal Stride"),
		     HU.select(ARG_HSTRIDE,strides,  request.getString(ARG_HSTRIDE,"1")));
        List<CalendarDate> dates = getGridDates(dataset);
        StringBuffer       varSB = getVariableForm(dataset, true, true,
                                       false);
        LatLonRect         llr   = dataset.getBoundingBox();
        if (llr != null) {
            MapInfo map = getRepository().getMapManager().createMap(request,
                              entry, true, null);
            map.addBox("", "", "", llr,
                       new MapProperties("blue", false, true));
            String[] points = new String[] { "" + llr.getLatMax(),
                                             "" + llr.getLonMin(),
                                             "" + llr.getLatMin(),
                                             "" + llr.getLonMax(), };

            String[] selected= new String[] {
		request.getString(ARG_AREA+"_north",""),
		request.getString(ARG_AREA+"_west",""),		
		request.getString(ARG_AREA+"_south",""),
		request.getString(ARG_AREA+"_east","")};


            for (int i = 0; i < points.length; i++) {
                sb.append(HU.hidden(SPATIALARGS[i] + ".original",
                                           points[i]));
            }
	    
            String llb = map.makeSelector(ARG_AREA, true, selected,points,"","");
            sb.append(HU.formEntryTop(msgLabel("Subset Spatially"),
                                             llb));
        }
        getCdmManager().addTimeWidget(request, dates, sb);

	HU.formEntry(sb,"", HU.labeledCheckbox(ARG_ADDLATLON, HU.VALUE_TRUE,
					    request.get(ARG_ADDLATLON, true),
					    "Add Lat/Lon Variables")+HU.SPACE+
		     "(if needed for CF compliance)");

        sb.append(HU.formTableClose());

        /*
        // TODO: check if we can use this.
        // This uses JNI, so not available everywhere
        List formats = Misc.toList(new Object[] {
                           new TwoFacedObject("NetCDF3", NetcdfFileWriter.Version.netcdf3.toString()),
                           new TwoFacedObject("NetCDF4", NetcdfFileWriter.Version.netcdf4.toString()),
                           new TwoFacedObject("NetCDF4 Classic", NetcdfFileWriter.Version.netcdf4_classic.toString())});

        String format = request.getString(CdmConstants.ARG_FORMAT, SupportedFormat.NETCDF3.getFormatName());

        sb.append(HU.formEntry(msgLabel("Format"),
                                      HU.select(CdmConstants.ARG_FORMAT, formats,
                                          format)));
        */


        sb.append(HU.div("Select Variables",HU.cssClass("ramadda-table-header")+HU.style("margin-top:6px;")));
	sb.append(HU.beginInset(0,10,0,0));
        sb.append(varSB);
	sb.append(HU.endInset());

        if (!request.getUser().getAnonymous()) {
	    sb.append(HU.div("Publish",HU.cssClass("ramadda-table-header")+HU.style("margin-top:6px;")));
	    sb.append(HU.formTable());
	    addPublishWidget(request, entry, sb,
			     "Select a folder to publish the results to");
	    sb.append(HU.formTableClose());
	}


	sb.append(HU.vspace("1em"));
        sb.append(HU.submit("Subset Grid"));
	sb.append(HU.vspace("1em"));
        addUrlShowingForm(sb, null, formId,
                          "[\".*OpenLayers_Control.*\",\".*original.*\"]",
                          null, "includeCopyArgs","true");
        sb.append(HU.formClose());



        getCdmManager().returnGridDataset(path, dataset);

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

        //request.remove("formurl");
        //      System.err.println("R:" + request);
        boolean canAdd = getRepository().getAccessManager().canDoNew(request,
                             entry.getParentEntry());

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
	boolean allVars = request.get("allvars",false);
	if(allVars) {
	} else {
	    for (Enumeration keys = args.keys(); keys.hasMoreElements(); ) {
		String arg = (String) keys.nextElement();
		if (arg.startsWith(VAR_PREFIX) && request.get(arg, false)) {
		    varNames.add(arg.substring(VAR_PREFIX.length()));
		}
	    }
	    for (String v :
		     (List<String>) request.get(ARG_VAR,
						new ArrayList<String>())) {
		varNames.addAll(StringUtil.split(v, ",", true, true));
	    }
        }

        GridDataset gds = getCdmManager().getGridDataset(entry, path);
        // initialize the bounds and date range to the defaults
	//Don't set the llr here as it can give a bad spatial domain
	//only set it if the user has specified a bounds
	//        LatLonRect        llr                 = gds.getBoundingBox();
        LatLonRect        llr                 =null;
        CalendarDateRange cdr                 = gds.getCalendarDateRange();
        boolean           anySpatialDifferent = false;
        boolean           haveAllSpatialArgs  = true;
        //if (varNames.size() == 0 || varNames.get(0).equalsIgnoreCase("all")) {
        if (allVars ||((varNames.size() == 1)
		       && varNames.get(0).equalsIgnoreCase("all"))) {
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

        SelectionRectangle bbox = request.getSelectionBounds();
        if (bbox.allDefined()) {
            llr = new LatLonRect(new LatLonPointImpl(bbox.getNorth(),
                    bbox.getWest()), new LatLonPointImpl(bbox.getSouth(),
                        bbox.getEast()));
            if (debug) 
		System.err.println("LLR-1:" + llr);
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
            if (debug) 
		System.err.println("LLR-2:" + llr);
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

	getCdmManager().setDates(request, allDates, dates);
	boolean internal = Misc.equals(request.getExtraProperty("internal"),"true");

        if ((dates[0] != null) && (dates[1] != null)
                && (dates[0].isAfter(dates[1]))) {
            sb.append(getPageHandler().showDialogWarning("From date is after to date"));
	    if(internal) {
		throw new IllegalArgumentException("From date is after to date");
	    }
        } else if (varNames.size() == 0) {
	    if(internal) {
		throw new IllegalArgumentException("No variables specified");
	    }

	    getPageHandler().entrySectionOpen(request, entry, sb, "Subset Grid");
            sb.append(getPageHandler().showDialogWarning("No variables selected"));
	    makeGridSubsetForm(request,entry,sb);
	    getPageHandler().entrySectionClose(request, entry, sb);
	    return makeLinksResult(request, msg("Grid Subset"), sb,
				   new State(entry));	    
        } else {
            boolean doingLocal = false;
            File    f          =
                (File) request.getExtraProperty("subsetfile");
            if (f == null) {
                f = getRepository().getStorageManager().getTmpFile(request,
                        "subset" + ncVersion.getSuffix());
            } else {
                doingLocal = true;
            }

            /**
             * System.err.println(f.getPath());
             * List grids = gds.getGrids();
             * for (int i = 0; i < grids.size(); i++) {
             *   System.err.println(grids.get(i));
             * }
             * GeoGrid grid = (GeoGrid) gds.findGridByName("Pressure_surface");
             * //      System.err.println(grid.getTimes());
             * grid = grid.subset(new Range(0, 0), null, null, 1, 2, 2);
             *
             * GridCoordSystem gcs   = grid.getCoordinateSystem();
             * CoordinateAxis  xaxis = gcs.getXHorizAxis();
             * CoordinateAxis  yaxis = gcs.getYHorizAxis();
             * int[] idx1 = gcs.findXYindexFromLatLon(yaxis.getMinValue(),
             *                xaxis.getMinValue(), null);
             * int[] idx2 = gcs.findXYindexFromLatLon(yaxis.getMaxValue(),
             *                xaxis.getMaxValue(), null);
             * List<LatLonPoint> points = new ArrayList<LatLonPoint>();
             * int               lats   = 0,
             *                 lons   = 0;
             * for (Dimension d : gcs.getDomain()) {
             *   if (d.getShortName().equals("lat")) {
             *       lats = d.getLength();
             *   } else if (d.getShortName().equals("lon")) {
             *       lons = d.getLength();
             *   }
             * }
             * System.err.println("ll:" + lats + " " + lons);
             * for (int lat = 0; lat < lats; lat++) {
             *   for (int lon = 0; lon < lons; lon++) {
             *       points.add(gcs.getLatLon(lon, lat));
             *   }
             * }
             * Array a = grid.readYXData(0, 0);
             * System.err.println("points:" + points.size() + " a:"
             *                  + a.getSize());
             * FileOutputStream fos    = new FileOutputStream(f);
             * PrintWriter      writer = new PrintWriter(fos);
             * writer.println("#fields="
             *              + "Pressure_surface,latitude,longitude");
             * for (int i = 0; i < a.getSize(); i++) {
             *   LatLonPoint llp = points.get(i);
             *   float       v   = a.getFloat(i);
             *   writer.println(v + "," + llp.getLatitude() + ","
             *                  + llp.getLongitude());
             * }
             * writer.close();
             */


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
                                             : CalendarDateRange.of(dates[0], dates[1])),
				    timeStride, includeLatLon, ncFileWriter);

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
                TypeHandler typeHandler =  getRepository().getTypeHandler("cdm_grid");
                Entry newEntry = typeHandler.createEntry(getRepository().getGUID());
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
    public Result outputGridCsv(final Request request, final Entry entry)
            throws Exception {

        final boolean debug = false;
        String        path  = getPath(request, entry);
        final GridDataset  gds   = getCdmManager().getGridDataset(entry,  path);
        GeoGrid     grid = getGrid(request, gds);
        if (grid == null) {
            throw new RuntimeException("Could not find grid field:" + request.getString(ARG_GRIDFIELD,""));
        }
        final String field = grid.getName();
        List<CalendarDate> dates = getGridDates(gds);




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
            if (debug && (zVals != null)) {
                System.err.println("# Z levels:" + zVals.length + " v:"
                                   + java.util.Arrays.toString(zVals));
            }
        }
        Range zRange = null;
        if (request.defined("gridLevel")) {
            String gridLevel = request.getString("gridLevel", (String) null);
            if (gridLevel != null) {
                if (gridLevel.equals("last")) {
                    if (zVals != null) {
                        zRange = new Range(zVals.length - 1,
                                           zVals.length - 1);
                    }
                } else if (gridLevel.equals("all") && (zVals != null)) {
                    zRange = new Range(0, zVals.length - 1);
                } else {
                    int index = Integer.parseInt(gridLevel);
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
                                * yDimension.getLength() * zRange.length()
                                * numTimes / timeStride;
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
        PipedInputStream         in          = new PipedInputStream();
        final Range              finalZRange = zRange;
        final PipedOutputStream  out         = new PipedOutputStream(in);
        final List<CalendarDate> theDates    = dates;
        final GeoGrid            theGrid     = grid;
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
                writer.println("{\"name\":" + JsonUtil.quote(entry.getName())
                               + ",");
                writer.println("\"fields\":");
                List<String> fields = new ArrayList<String>();
                int          index  = 0;
                fields.add(JsonUtil.map(Utils.makeListFromValues("id",
                        JsonUtil.quote(field), "label",
                        JsonUtil.quote(fieldLabel), "index", "" + (index++),
                        "type", JsonUtil.quote("double"), "chartable",
                        "true", "unit", JsonUtil.quote(getUnit(unit)))));
                //todo: check for times
                fields.add(JsonUtil.map(Utils.makeListFromValues("id",
                        JsonUtil.quote("date"), "label",
                        JsonUtil.quote("Date"), "index", "" + (index++),
                        "type", JsonUtil.quote("date"))));
                if (finalZRange.length() > 1) {
                    fields.add(JsonUtil.map(Utils.makeListFromValues("id",
                            JsonUtil.quote("level"), "label",
                            JsonUtil.quote("Level"), "index", "" + (index++),
                            "type", JsonUtil.quote("double"))));
                }
                fields.add(JsonUtil.map(Utils.makeListFromValues("id",
                        JsonUtil.quote("latitude"), "label",
                        JsonUtil.quote("Latitude"), "index", "" + (index++),
                        "type", JsonUtil.quote("double"))));
                fields.add(JsonUtil.map(Utils.makeListFromValues("id",
                        JsonUtil.quote("longitude"), "label",
                        JsonUtil.quote("Longitude"), "index", "" + (index++),
                        "type", JsonUtil.quote("double"))));
                writer.println(JsonUtil.list(fields));
                List<String> displayProps = new ArrayList<String>();
                Hashtable    wikiProps    = new Hashtable();
                for (Enumeration keys = request.keys();
                        keys.hasMoreElements(); ) {
                    String key = (String) keys.nextElement();
                    wikiProps.put(key, request.getString(key, ""));
                }
                wikiProps.put(ARG_GRIDFIELD, field);
                getWikiTagAttrs(request, entry, "display", wikiProps,
                                displayProps);
		getDisplayProperties(request, field,displayProps);


                writer.println(",\"properties\":");
                writer.println(JsonUtil.map(displayProps));
                writer.println(",\"data\":[");
                int                   cnt    = 0;
                DoubleFunction<Float> scaler = getScaler(unit);
                long                  t1     = System.currentTimeMillis();
                for (int tIdx = 0; (tIdx < theDates.size()) && (cnt < max);
		     tIdx += timeStride) {
                    Array a;
                    if (finalZRange.length() == 1) {
                        a = theGrid.readYXData(tIdx, 0);
                    } else {
                        a = theGrid.readVolumeData(tIdx);
                    }
                    if (debug) {
                        System.err.println("\treading time index:" + tIdx
                                           + " size:" + a.getSize());
                    }
                    cnt += writeJson(
                        writer, cnt, max, a,
                        JsonUtil.quote(theDates.get(tIdx).toString()),
                        points, finalZRange, scaler);
                }
                writer.println("]}");
                writer.close();
                System.err.println("time:" + (System.currentTimeMillis() - t1));
                getCdmManager().returnGridDataset(path, gds);
            }
        });

        Result result = new Result(entry.getName() + ".json", in,
                                   "application/json");
        result.setReturnFilename(entry.getName() + ".json");

        return result;

    }


    private void getDisplayProperties(Request request, String field,List<String>displayProps) {
	String colorTable = getProperty(field, "colortable", null);
	if (colorTable != null) {
	    displayProps.add("colorTable");
	    displayProps.add(JsonUtil.quote(colorTable));
	}
	String colors = getProperty(field, "colors", null);
	if (colors != null) {
	    displayProps.add("colors");
	    colors = colors.trim();
	    //Check for array
	    if(colors.startsWith("[")) 
		displayProps.add(colors);
	    else
		displayProps.add(JsonUtil.quote(colors));
	}		
	//		System.err.println(field+" ct:" + colorTable);
	//		System.err.println(field+" colors:" + colors);
	String colorTableMin = getProperty(field, "colortable.min",
                                           null);
	if (colorTableMin != null) {
	    displayProps.add("colorByMin");
	    displayProps.add(JsonUtil.quote(colorTableMin));
	}
	String colorTableMax = getProperty(field, "colortable.max",
                                           null);
	//	System.err.println("F:" + field +" " + colorTable +" range:" + colorTableMin+" " + colorTableMax);
	if (colorTableMax != null) {
	    displayProps.add("colorByMax");
	    displayProps.add(JsonUtil.quote(colorTableMax));
	}
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
        final GridDataset  gds   = getCdmManager().getGridDataset(entry,  path);
        GeoGrid     grid = getGrid(request, gds);
        if (grid == null) {
            throw new RuntimeException("Could not find grid field:" + request.getString(ARG_GRIDFIELD,""));
        }
        final String field = grid.getName();
        List<CalendarDate> dates = getGridDates(gds);
	GridCoordSystem      gcs      = grid.getCoordinateSystem();
	CoordinateAxis1DTime timeAxis = gcs.getTimeAxis1D();
	if (timeAxis != null) {
	    dates = timeAxis.getCalendarDates();
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
            if (debug && (zVals != null)) {
		//                System.err.println("# Z levels:" + zVals.length + " v:" + java.util.Arrays.toString(zVals));
            }
        }
        Range zRange = null;
        if (request.defined("gridLevel")) {
            String gridLevel = request.getString("gridLevel", (String) null);
            if (gridLevel != null) {
                if (gridLevel.equals("last")) {
                    if (zVals != null) {
                        zRange = new Range(zVals.length - 1,
                                           zVals.length - 1);
                    }
                } else if (gridLevel.equals("all") && (zVals != null)) {
                    zRange = new Range(0, zVals.length - 1);
                } else {
                    int index = Integer.parseInt(gridLevel);
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
            grid = grid.subset(tRange, zRange, bounds, 1, gridStride, gridStride);
            Dimension xDimension = grid.getXDimension();
            Dimension yDimension = grid.getYDimension();
            int numPoints = xDimension.getLength() * yDimension.getLength()* (numTimes / timeStride);
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
                int numPoints = xDimension.getLength()  * yDimension.getLength() * zRange.length()
		    * (numTimes / timeStride);
                if (debug) {
                    System.err.println(
                        "\tloop num points:" + numPoints + " per layer:"
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
                grid = grid.subset(null, null, null, 1, gridStrideX, gridStrideY);
                doX = !doX;
            }
        }


	//Get the new GCS after we do the subset above
	gcs      = grid.getCoordinateSystem();
        int                     lats   = (int) gcs.getYHorizAxis().getSize();
        int                     lons   = (int) gcs.getXHorizAxis().getSize();
	if(debug) {
	    LatLonRect llr=gcs.getLatLonBoundingBox();
	    System.err.println("#lats:" + lats +" #lons:"+ lons +" " + llr.getLatMin() +" " +llr.getLatMax());
	}
        final List<LatLonPoint> points = new ArrayList<LatLonPoint>();
        for (int lat = 0; lat < lats; lat++) {
            for (int lon = 0; lon < lons; lon++) {
		LatLonPoint point=gcs.getLatLon(lon, lat);
                points.add(point);
            }
        }

        if (debug) {
            System.err.println("\t# lat/lons:" + points.size() + " #dates:" + dates.size());
        }
        PipedInputStream         in          = new PipedInputStream();
        final Range              finalZRange = zRange;
        final PipedOutputStream  out         = new PipedOutputStream(in);
        final List<CalendarDate> theDates    = dates;
        final GeoGrid            theGrid     = grid;
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
                writer.println("{\"name\":" + JsonUtil.quote(entry.getName())
                               + ",");
                writer.println("\"fields\":");
                List<String> fields = new ArrayList<String>();
                int          index  = 0;
                fields.add(JsonUtil.map(Utils.makeListFromValues("id",
                        JsonUtil.quote(field), "label",
                        JsonUtil.quote(fieldLabel), "index", "" + (index++),
                        "type", JsonUtil.quote("double"), "chartable",
                        "true", "unit", JsonUtil.quote(getUnit(unit)))));
                //todo: check for times
                fields.add(JsonUtil.map(Utils.makeListFromValues("id",
                        JsonUtil.quote("date"), "label",
                        JsonUtil.quote("Date"), "index", "" + (index++),
                        "type", JsonUtil.quote("date"))));
                if (finalZRange.length() > 1) {
                    fields.add(JsonUtil.map(Utils.makeListFromValues("id",
                            JsonUtil.quote("level"), "label",
                            JsonUtil.quote("Level"), "index", "" + (index++),
                            "type", JsonUtil.quote("double"))));
                }
                fields.add(JsonUtil.map(Utils.makeListFromValues("id",
                        JsonUtil.quote("latitude"), "label",
                        JsonUtil.quote("Latitude"), "index", "" + (index++),
                        "type", JsonUtil.quote("double"))));
                fields.add(JsonUtil.map(Utils.makeListFromValues("id",
                        JsonUtil.quote("longitude"), "label",
                        JsonUtil.quote("Longitude"), "index", "" + (index++),
                        "type", JsonUtil.quote("double"))));
                writer.println(JsonUtil.list(fields));
                List<String> displayProps = new ArrayList<String>();
                Hashtable    wikiProps    = new Hashtable();
                for (Enumeration keys = request.keys();
                        keys.hasMoreElements(); ) {
                    String key = (String) keys.nextElement();
                    wikiProps.put(key, request.getString(key, ""));
                }
                wikiProps.put(ARG_GRIDFIELD, field);
                getWikiTagAttrs(request, entry, "display", wikiProps, displayProps);
		getDisplayProperties(request, field,displayProps);
                writer.println(",\"properties\":");
                writer.println(JsonUtil.map(displayProps));
                writer.println(",\"data\":[");
                int                   cnt    = 0;
                DoubleFunction<Float> scaler = getScaler(unit);
                long                  t1     = System.currentTimeMillis();
                for (int tIdx = 0; (tIdx < theDates.size()) && (cnt < max);
                        tIdx += timeStride) {
		    //		    if(tIdx>3) break;
                    Array a;
		    //		    System.err.println("time idx:" + tIdx +" date:" + theDates.get(tIdx));
		    try {
			if (finalZRange.length() == 1) {
			    a = theGrid.readYXData(tIdx, 0);
			} else {
			    a = theGrid.readVolumeData(tIdx);
			}
			if (debug) {
			    System.err.println("read time index:" + tIdx  + " " + theDates.get(tIdx));
			    //+" size:" + a.getSize());
			}
			cnt += writeJson(
					 writer, cnt, max, a,
					 JsonUtil.quote(theDates.get(tIdx).toString()),
					 points, finalZRange, scaler);
		    } catch(Exception exc) {
			System.err.println("Error reading time:" + tIdx + " " + theDates.get(tIdx));
			exc.printStackTrace();
			break;
		    }
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
     * @param zRange _more_
     * @param scaler _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private int writeJson(PrintWriter writer, int cnt, int max, Array a,
                          String dateString, List<LatLonPoint> points,
                          Range zRange, DoubleFunction<Float> scaler)
            throws Exception {
        synchronized (writer) {
            int written           = 0;
            int numPointsPerLevel = (int) (a.getSize() / zRange.length());
	    /*
            System.err.println("a:" + a.getSize() + " per level:"
                               + (numPointsPerLevel) + " #points:"
                               + points.size());
	    */
            for (int z = zRange.first(); z <= zRange.last(); z++) {
                for (int i = 0; (i < numPointsPerLevel) && (cnt < max); i++) {
                    written++;
                    LatLonPoint llp = points.get(i);
                    float v = (float) scaler.apply((double) a.getFloat(i));
                    if (cnt++ > 0) {
                        writer.print(",");
                    }
                    writer.print("[");
                    writer.print((Double.isNaN(v)
                                  ? null
                                  : v));
                    writer.print(",");
                    writer.print(dateString);
                    if (zRange.length() > 1) {
                        writer.print(",");
                        writer.print(z);
                    }
                    writer.print(",");
                    writer.print(Utils.decimals(llp.getLatitude(),6));
                    writer.print(",");
                    writer.print(Utils.decimals(llp.getLongitude(),6));
                    writer.print("]");
                }
            }

            return written;
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
            if (grid == null) {
                continue;
            }
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
                                   + HU.quote(value));
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
            columnNames.add(HU.quote(var.getShortName()));
            String label = var.getDescription();
            //            if(label.trim().length()==0)
            label = var.getShortName();
            columnDefs.add("{key:" + HU.quote(var.getShortName())
                           + "," + "sortable:true," + "label:"
                           + HU.quote(label) + "}");
        }


        if (total > max) {
            sb.append((skip + 1) + "-" + (skip + cnt) + " of " + total + " ");
        } else {
            sb.append((skip + 1) + "-" + (skip + cnt));
        }
        if (total > max) {
            boolean didone = false;
            if (skip > 0) {
                sb.append(HU.space(2));
                sb.append(
                    HU.href(
                        HU.url(request.getRequestPath(), new String[] {
                    ARG_OUTPUT, request.getOutput().toString(), ARG_ENTRYID,
                    entry.getId(), ARG_SKIP, "" + (skip - max), ARG_MAX,
                    "" + max
                }), msg("Previous")));
                didone = true;
            }
            if (total > (skip + cnt)) {
                sb.append(HU.space(2));
                sb.append(
                    HU.href(
                        HU.url(request.getRequestPath(), new String[] {
                    ARG_OUTPUT, request.getOutput().toString(), ARG_ENTRYID,
                    entry.getId(), ARG_SKIP, "" + (skip + max), ARG_MAX,
                    "" + max
                }), msg("Next")));
                didone = true;
            }
            //Just come up with some max number
            if (didone && (total < 2000)) {
                sb.append(HU.space(2));
                sb.append(
                    HU.href(
                        HU.url(request.getRequestPath(), new String[] {
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
        sb.append(HU.form(formUrl + suffix));
        sb.append(HU.submit("Subset Point Data", ARG_SUBMIT));
        sb.append(HU.hidden(ARG_OUTPUT,
                                   request.getString(ARG_OUTPUT, "")));
        sb.append(HU.hidden(ARG_ENTRYID, entry.getId()));
        sb.append(HU.formTable());
        List<TwoFacedObject> formats = new ArrayList<TwoFacedObject>();
        formats.add(new TwoFacedObject("CSV", FORMAT_CSV));
        formats.add(new TwoFacedObject("KML", FORMAT_KML));
        String format = request.getString(CdmConstants.ARG_FORMAT,
                                          FORMAT_CSV);
        sb.append(
            HU.formEntry(
                msgLabel("Format"),
                HU.select(CdmConstants.ARG_FORMAT, formats, format)));

        MapInfo map = getRepository().getMapManager().createMap(request,
                          entry, true, null);
        map.addBox(request,entry, new MapProperties("blue", false, true));
        map.centerOn(request,entry);
        String llb = map.makeSelector(ARG_POINT_BBOX, true, null);
        sb.append(HU.formEntryTop(msgLabel("Location"), llb));
        sb.append(HU.formTableClose());
        sb.append(HU.submit("Subset Point Data", ARG_SUBMIT));
        sb.append(HU.formClose());

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
                         + HU.args(new String[] {
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
                pw.print(HU.quote("Time"));
                pw.print(",");
                pw.print(HU.quote("Latitude"));
                pw.print(",");
                pw.print(HU.quote("Longitude"));
                for (VariableSimpleIF var : (List<VariableSimpleIF>) vars) {
                    pw.print(",");
                    String unit = var.getUnitsString();
                    if (unit != null) {
                        pw.print(HU.quote(var.getShortName() + " ("
                                + unit + ")"));
                    } else {
                        pw.print(HU.quote(var.getShortName()));
                    }
                }
                pw.print("\n");
            }

            pw.print(HU.quote("" + po.getNominalTimeAsCalendarDate()));
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
                        HU.quote(structure.getScalarString(member)));
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
     *
     * @return  the Result
     *
     * @throws Exception  problem outputting group
     */
    @Override
    public Result outputGroup(Request request, OutputType outputType,
                              Entry group, List<Entry> children)
            throws Exception {
        if (getCdmManager().isAggregation(group)) {
            return outputEntry(request, outputType, group);
        }

        //        System.err.println("group:" + group + " " + group.getType());
        return super.outputGroup(request, outputType, group, children);
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

        if ( !getRepository().getAccessManager().canDoFile(request, entry)) {
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
                result.addHttpHeader(HU.HTTP_CONTENT_DESCRIPTION,
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
                HU.mouseClickHref(
                    HU.call(
			    "RamaddaUtils.selectClick",
                        HU.comma(
                            HU.squote(target),
                            HU.squote(entry.getId()),
                            HU.squote(var.getShortName()),
                            HU.squote(type))), var.getDescription()));
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

        if (debugOpendap) {
            System.err.println("OpenDap.outputOpendap location:" + location);
        }
        NetcdfFile ncFile = getCdmManager().createNetcdfFile(location);
        if (debugOpendap) {
            System.err.println("OpenDap.outputOpendap ncfile:" + ncFile);
        }
        opendapCounter.incr();

        //Bridge the ramadda servlet to the opendap servlet
        if (debugOpendap) {
            System.err.println(
                "OpenDap.outputOpendap creating the servlet bridge");
        }
        NcDODSServlet servlet = new NcDODSServlet(request, entry, ncFile) {
            @Override
            public ServletConfig getServletConfig() {
                if (debugOpendap) {
                    System.err.println("NcDODSServlet.getServletConfig");
                }
                ServletConfig config =
                    request.getHttpServlet().getServletConfig();
                if (debugOpendap) {
                    System.err.println(
                        "NcDODSServlet.getServletConfig got config:"
                        + config);
                }

                return config;
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

                if (debugOpendap) {
                    System.err.println("NcDODSServlet.getServletContext");
                }
                ServletContext ctx =
                    request.getHttpServlet().getServletContext();
                if (debugOpendap) {
                    System.err.println(
                        "NcDODSServlet.getServletContext got context:" + ctx);
                }

                return ctx;
            }
            @Override
            public String getServletInfo() {
                if (debugOpendap) {
                    System.err.println("NcDODSServlet.getServletInfo");
                }
                String info = request.getHttpServlet().getServletInfo();
                if (debugOpendap) {
                    System.err.println("NcDODSServlet.getServletInfo info:"
                                       + info);
                }

                return info;
            }
            @Override
            public Enumeration getInitParameterNames() {
                if (debugOpendap) {
                    System.err.println("NcDODSServlet.getInitParameterNames");
                }
                Enumeration names =
                    request.getHttpServlet().getInitParameterNames();
                if (debugOpendap) {
                    System.err.println(
                        "NcDODSServlet.getInitParameterNames names:" + names);
                }

                return names;
            }
        };

        //If we are running as a normal servlet then init the ncdods servlet with the servlet config info
        if ((request.getHttpServlet() != null)
                && (request.getHttpServlet().getServletConfig() != null)) {
            if (debugOpendap) {
                System.err.println("OpenDap calling servlet.init");
            }
            servlet.init(request.getHttpServlet().getServletConfig());
        } else {
            if (debugOpendap) {
                System.err.println("OpenDap calling no servlet.init to call");
            }
        }

        //Do the work
        if (debugOpendap) {
            System.err.println("OpenDap calling servlet.doGet");
        }
        servlet.doGet(request.getHttpServletRequest(),
                      request.getHttpServletResponse());
        if (debugOpendap) {
            System.err.println("OpenDap called servlet.doGet");
        }
        //We have to pass back a result though we set needtowrite to false because the opendap servlet handles the writing
        Result result = new Result("");
        result.setNeedToWrite(false);
        opendapCounter.decr();
        if (debugOpendap) {
            System.err.println("OpenDap returning netcdfile to cache");
        }
        getCdmManager().returnNetcdfFile(location, ncFile);
        if (debugOpendap) {
            System.err.println("OpenDap done");
        }

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
