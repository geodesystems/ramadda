/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.cdmdata;




import org.ramadda.data.record.RecordField;
import org.ramadda.repository.DateHandler;

import org.ramadda.repository.Entry;
import org.ramadda.repository.Link;
import org.ramadda.repository.PageHandler;
import org.ramadda.repository.Repository;
import org.ramadda.repository.Request;
import org.ramadda.repository.Result;
import org.ramadda.repository.auth.AccessException;
import org.ramadda.repository.map.MapProperties;
import org.ramadda.repository.map.MapInfo;
import org.ramadda.repository.output.OutputHandler;
import org.ramadda.repository.output.OutputType;
import org.ramadda.repository.type.TypeHandler;
import org.ramadda.util.HtmlUtils;

import org.ramadda.util.JsonUtil;
import org.ramadda.util.Utils;

import org.w3c.dom.Element;


import thredds.server.ncss.format.SupportedFormat;
//import thredds.server.ncss.params.PointDataRequestParamsBean;
import thredds.server.ncss.params.NcssParamsBean;
import thredds.server.ncss.util.NcssRequestUtils;
import thredds.server.ncss.view.gridaspoint.PointDataStream;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.Range;
import ucar.ma2.StructureData;
import ucar.ma2.StructureMembers;

import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableEnhanced;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridAsPointDataset;
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
import ucar.nc2.util.DiskCache2;


import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.ui.ImageUtils;
import ucar.unidata.util.Counter;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;


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


import java.util.Properties;
import java.util.regex.Pattern;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;


/**
 * A class for handling CDM data output
 */
@SuppressWarnings("unchecked")
public class GridPointOutputHandler extends CdmOutputHandler implements CdmConstants {


    /** Grid as point form Output Type */
    public static final OutputType OUTPUT_GRIDASPOINT_FORM =
        new OutputType("Extract Time Series", "data.gridaspoint.form",
                       OutputType.TYPE_OTHER | OutputType.TYPE_IMPORTANT|OutputType.TYPE_SERVICE,
                       OutputType.SUFFIX_NONE, "/cdmdata/timeseries.png",
                       GROUP_DATA);

    /** Grid as point Output Type */
    public static final OutputType OUTPUT_GRIDASPOINT =
        new OutputType("data.gridaspoint", OutputType.TYPE_FEEDS);

    /**
     * Create a new GridPointOutputHandler
     *
     * @param repository  the repository
     * @param name        the name of this handler
     *
     * @throws Exception problem creating class
     */
    public GridPointOutputHandler(Repository repository, String name)
            throws Exception {
        super(repository, name);
    }

    /**
     *     Create a GridPointOutputHandler
     *
     *     @param repository  the repository
     *     @param element     the element
     *     @throws Exception On badness
     */
    public GridPointOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        addType(OUTPUT_GRIDASPOINT);
        addType(OUTPUT_GRIDASPOINT_FORM);
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

        if (outputType.equals(OUTPUT_GRIDASPOINT)
                || outputType.equals(OUTPUT_GRIDASPOINT_FORM)) {
            return outputGridAsPoint(request, entry);
        }

        throw new IllegalArgumentException("Unknown output type:"
                                           + outputType);
    }



    /**
     * Process a grid as point request
     *
     * @param request  the request
     * @param entry    the entry
     * @param gds      the corresponding grid dataset
     * @param sb       the StringBuffer
     *
     * @return a Result
     *
     * @throws Exception problem doing what was asked
     */
    public Result outputGridAsPointProcess(Request request, Entry entry,
                                           GridDataset gds, StringBuffer sb)
            throws Exception {

        List<String>           varNames = new ArrayList<String>();
        List<VariableEnhanced> vars     = new ArrayList<VariableEnhanced>();
        Hashtable              args     = request.getArgs();
        //Look for both variable.<varname>=true  and variable=<varname> url arguments
        for (Enumeration keys = args.keys(); keys.hasMoreElements(); ) {
            String arg = (String) keys.nextElement();
            if (arg.startsWith(VAR_PREFIX) && request.get(arg, false)) {
                varNames.add(arg.substring(VAR_PREFIX.length()));
            }
        }

        varNames.addAll((List<String>) request.get(ARG_VAR,
                new ArrayList<String>()));
        HashSet selectedVarsMap = null;
        if (varNames.size() > 0) {
            selectedVarsMap = Utils.makeHashSet(varNames);
        }


        //For now add either the 2d or the 3d vars
        List<GridDatatype> grids = sortGrids(gds);
        for (GridDatatype grid : grids) {
            VariableEnhanced var = grid.getVariable();
            if ((selectedVarsMap != null)
                    && !selectedVarsMap.contains(var.getShortName())) {
                continue;
            }
            if (grid.getZDimension() == null) {
                vars.add(var);
            }
        }
        if (vars.size() == 0) {
            for (GridDatatype grid : grids) {
                VariableEnhanced var = grid.getVariable();
                if ((selectedVarsMap != null)
                        && !selectedVarsMap.contains(var.getShortName())) {
                    continue;
                }
                if (grid.getZDimension() != null) {
                    vars.add(var);
                }
            }
        }

        LatLonRect llr    = gds.getBoundingBox();
        double     deflat = 0;
        double     deflon = 0;
        if (llr != null) {
            deflat = llr.getLatMin() + llr.getHeight() / 2;
            deflon = llr.getCenterLon();
        }
        LatLonPointImpl llp = null;

        if (request.defined("default_latitude")) {
            deflat = request.get("default_latitude", 0.0);
        }
        if (request.defined("default_longitude")) {
            deflon = request.get("default_longitude", 0.0);
        }

        if (request.defined(TypeHandler.REQUESTARG_LATITUDE)) {
            llp = new LatLonPointImpl(request
                .getLatOrLonValue(TypeHandler.REQUESTARG_LATITUDE,
                                  deflat), request
                                      .getLatOrLonValue(TypeHandler
                                          .REQUESTARG_LONGITUDE, deflon));
            //            System.err.println("latlon point:" + llp);
        }
        if (llp == null) {
            llp = new LatLonPointImpl(deflat, deflon);
            //            System.err.println("using default values:" + llp);
        }



        int                timeStride = 1;
        List<CalendarDate> allDates   =
            CdmDataOutputHandler.getGridDates(gds);
        CalendarDate[]     dates      = new CalendarDate[2];
        Calendar           cal        = null;
        String             calString  = request.getString(ARG_CALENDAR, null);
	getCdmManager().setDates(request, allDates, dates);

        if ((dates[0] != null) && (dates[1] != null)
                && (dates[0].isAfter(dates[1]))) {
            sb.append(
                getPageHandler().showDialogWarning(
                    "From date is after to date"));
        } else if (vars.size() == 0) {
            sb.append(
                getPageHandler().showDialogWarning("No variables selected"));
        } else {
            // modelled after thredds.server.ncSubset.controller.PointDataController
            try {
                return processPointRequest(request, entry, gds, vars, llp,
                                           dates, allDates);
            } catch (Exception exc) {
                if (request.getString(
                        CdmConstants.ARG_FORMAT,
                        SupportedFormat.NETCDF3.toString()).equals(
                            FORMAT_JSON)) {
                    String message = "Error extracting data:" + exc;
                    String code    = "error";
                    if ((exc instanceof java.lang.ArrayIndexOutOfBoundsException)
                            && (Misc.getStackTrace(exc).indexOf(
                                "ucar.nc2.dataset.CoordinateAxis1D.getCoordValue") >= 0)) {
                        message =
                            "Spatial coordinates are outside the domain of the data";
                        code = "spatial";
                    }
                    //                    exc.printStackTrace();
                    StringBuffer json = new StringBuffer();
                    json.append(JsonUtil.map(Utils.makeListFromValues("error",
                            JsonUtil.quote(message), "errorcode",
                            JsonUtil.quote(code))));
                    Result result = new Result("", json, JsonUtil.MIMETYPE);

                    //                    result.setResponseCode(Result.RESPONSE_INTERNALERROR);
                    return result;
                }

                throw exc;
            }
        }

        return new Result("", sb, JsonUtil.MIMETYPE);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param gds _more_
     * @param vars _more_
     * @param llp _more_
     * @param dates _more_
     * @param allDates _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Result processPointRequest(Request request, Entry entry,
                                       GridDataset gds,
                                       List<VariableEnhanced> vars,
                                       LatLonPointImpl llp,
                                       CalendarDate[] dates,
                                       List<CalendarDate> allDates)
            throws Exception {

        double levelVal = request.get(ARG_LEVEL, Double.NaN);

        String format = request.getString(CdmConstants.ARG_FORMAT,
                                          SupportedFormat.NETCDF3.toString());
        boolean doingJson = format.equals(FORMAT_JSON);

        if (doingJson) {
            format = FORMAT_CSV;
            request.setCORSHeaderOnResponse();
        }

        SupportedFormat sf       = getSupportedFormat(format);
        NcssParamsBean  pdrb     = new NcssParamsBean();
        List<String>    varNames = new ArrayList<String>();
        for (VariableEnhanced var : vars) {
            varNames.add(var.getShortName());
        }
        GridAsPointDataset gapds =
            NcssRequestUtils.buildGridAsPointDataset(gds, varNames);
        pdrb.setVar(varNames);
        // accept uses the response type
        pdrb.setAccept((format.equalsIgnoreCase(FORMAT_TIMESERIES_CHART)
                        || format.equalsIgnoreCase(FORMAT_TIMESERIES_IMAGE))
                       ? SupportedFormat.CSV_STREAM.getResponseContentType()
                       : sf.getResponseContentType());
        //pdrb.setPoint(true);
        pdrb.setLatitude(llp.getLatitude());
        pdrb.setLongitude(llp.getLongitude());
        if (dates[0] != null) {
            pdrb.setTime_start(dates[0].toString());
            if (dates[1] != null) {
                pdrb.setTime_end(dates[1].toString());
            } else {
                pdrb.setTime(pdrb.getTime_start());
            }
        } else {  // dates weren't specified
            dates[0] = allDates.get(0);
            dates[1] = allDates.get(allDates.size() - 1);
            pdrb.setTemporal("all");
        }
        if (levelVal == levelVal) {
            pdrb.setVertCoord(levelVal);
        }
        Map<String, List<String>> groupVars = groupVarsByVertLevels(gds,
                                                  pdrb);

        String suffix = SUFFIX_NC;
        if (sf.equals(SupportedFormat.NETCDF4)) {
            suffix = SUFFIX_NC4;
        } else if (pdrb.getAccept()
                .equals(SupportedFormat.CSV_STREAM
                    .getResponseContentType()) || format
                        .equals(FORMAT_TIMESERIES_CHART_DATA) || format
                        .equals(FORMAT_TIMESERIES_IMAGE)) {
            suffix = SUFFIX_CSV;
        } else if (pdrb.getAccept().equals(
                SupportedFormat.XML_STREAM.getResponseContentType())) {
            suffix = SUFFIX_XML;
        }
        if (doingJson) {
            suffix = "json";
        }

        String baseName = IOUtil.stripExtension(entry.getName());
        if (format.equalsIgnoreCase(FORMAT_TIMESERIES_CHART)) {
            request.put(CdmConstants.ARG_FORMAT, FORMAT_JSON);
            request.put(ARG_LATITUDE, "_LATITUDEMACRO_");
            request.put(ARG_LONGITUDE, "_LONGITUDEMACRO_");
            StringBuffer html = new StringBuffer();
            getPageHandler().entrySectionOpen(request, entry, html,
                    "Time Series");

            html.append(getWikiManager().getStandardChartDisplay(request,
                    entry));
            getPageHandler().entrySectionClose(request, entry, html);

            return new Result("Point as Grid Time Series", html);
        }

        File tmpFile = getStorageManager().getTmpFile(request,
                           "pointsubset" + suffix);

        OutputStream outStream =
            getStorageManager().getUncheckedFileOutputStream(tmpFile);
        DiskCache2      dc = getCdmManager().getDiskCache2();
        PointDataStream pds = PointDataStream.factory(sf, outStream, dc);
        List<CalendarDate> wantedDates = NcssRequestUtils.wantedDates(gapds,
                                             CalendarDateRange.of(dates[0],
                                                 dates[1]), 0);
        boolean allWritten = false;
        allWritten = pds.stream(gds, llp, wantedDates, groupVars,
                                pdrb.getVertCoord());

        File f = null;
        if (allWritten) {
            outStream.close();
            f = tmpFile;
            if (doingJson) {
                File jsonFile =
                    getRepository().getStorageManager().getTmpFile(request,
                        "subset.json");
                BufferedReader br = new BufferedReader(
                                        new InputStreamReader(
                                            new FileInputStream(f)));
                BufferedWriter bw = new BufferedWriter(
                                        new OutputStreamWriter(
                                            new FileOutputStream(jsonFile)));
                List<RecordField> fields      = new ArrayList<RecordField>();

                String            line        = null;
                int               cnt         = 0;
                boolean           hasVertical = (pdrb.getVertCoord() != null);

                //                    System.err.println ("has vert:" + hasVertical);
                //                    System.err.println ("vars:" + varNames.size() +" " + varNames);
                boolean didOne = false;
                while ((line = br.readLine()) != null) {
                    cnt++;
                    //              System.out.println("line:" + line);
                    List<String> toks = StringUtil.split(line, ",", true,
                                            true);
                    if (cnt == 1) {
                        //                            System.err.println ("line:" + line);
                        //time/lat/lon  maybeZ vars
                        if (toks.size() == 3 + 1 + vars.size()) {
                            hasVertical = true;
                        }
                        continue;
                    }
                    if ( !didOne) {
                        didOne = true;
                        for (int i = 0; i < vars.size(); i++) {
                            VariableEnhanced var = vars.get(i);
                            RecordField recordField =
                                new RecordField(getAlias(var.getShortName()),
                                    Utils.makeLabel(getAlias(var
                                        .getShortName())), var
                                            .getShortName(), i,
                                                var.getUnitsString());
                            recordField.setChartable(true);
                            fields.add(recordField);

                        }
                        if (hasVertical) {
                            RecordField recordField =
                                new RecordField("level", "Level", "level",
                                    vars.size(), "");
                            recordField.setChartable(true);
                            fields.add(recordField);
                        }
                        /*

                        fields.add(
                                   new RecordField(
                                                   "latitude",
                                                   "Latitude",
                                                   "latitude"));
                        fields.add(
                                   new RecordField(
                                                   "longitude",
                                                   "Longitude",
                                                   "longitude"));
                        */
                        RecordField.addJsonHeader(bw, entry.getName(),
                                fields, false, false, false);
                    }
                    if (cnt > 2) {
                        bw.append(",");
                    }
                    bw.append("\n");
                    bw.append(JsonUtil.mapOpen());
                    //       date            lat   lon   alt     value(s)
                    // 2009-11-10T00:00:00Z,34.6,-101.1,100.0,207.89999389648438
                    CalendarDate date =
                        CalendarDate.parseISOformat(toks.get(0), toks.get(0));
                    double lat = Double.parseDouble(toks.get(1));
                    double lon = Double.parseDouble(toks.get(2));
                    double alt = (hasVertical
                                  ? Double.parseDouble(toks.get(3))
                                  : Double.NaN);
                    JsonUtil.addGeolocation(bw, lat, lon, alt);
                    bw.append(",");
                    bw.append(JsonUtil.attr(JsonUtil.FIELD_DATE,
                                            date.getMillis()));
                    bw.append(",");
                    bw.append(JsonUtil.mapKey(JsonUtil.FIELD_VALUES));
                    int          startIdx = (hasVertical
                                             ? 4
                                             : 3);
                    List<String> values   = new ArrayList();
                    for (int i = startIdx; i < toks.size(); i++) {
                        double v = Double.parseDouble(toks.get(i));
                        values.add(JsonUtil.formatNumber(v));
                    }
                    if (hasVertical) {
                        values.add(JsonUtil.formatNumber(alt));
                    }
                    bw.append(JsonUtil.list(values));
                    bw.append(JsonUtil.mapClose());
                }
                RecordField.addJsonFooter(bw);
                bw.close();
                f = jsonFile;
            }

        } else {
            //Something went wrong...
            System.err.println("something went wrong");
        }

        if (doingPublish(request)) {
            return getEntryManager().processEntryPublish(request, f,
                    (Entry) entry.clone(), entry, "point series of");
        }
        Result result = null;
        result = new Result(getStorageManager().getFileInputStream(f),
                            doingJson
                            ? JsonUtil.MIMETYPE
                            : pdrb.getAccept());
        //Set return filename sets the Content-Disposition http header so the browser saves the file
        //with the correct name and suffix
	baseName = baseName.replaceAll(" +","_");
        result.setReturnFilename(baseName + "_pointsubset" + suffix);

        return result;
    }




    /**
     * Get the SupportedFormat from the name
     * @param name
     * @return the corresponding format
     */
    private SupportedFormat getSupportedFormat(String name) {

        if (name.equalsIgnoreCase(FORMAT_TIMESERIES_CHART)
                || name.equalsIgnoreCase(FORMAT_TIMESERIES_IMAGE)) {
            return SupportedFormat.CSV_STREAM;
        }
        for (SupportedFormat sf : SupportedFormat.values()) {
            // check for the name
            if (name.equalsIgnoreCase(sf.getFormatName())) {
                return sf;
            }
            // check for aliases
            List<String> aliases = sf.getAliases();
            if (aliases.contains(name)) {
                return sf;
            }
        }

        // default to netCDF 3
        return SupportedFormat.NETCDF3;
    }

    /**
     * Group the variables by level.  Copied from  thredds.server.ncSubset.controller.PointDataController
     * @param gds   GridDataSet
     * @param params list of parameter names
     * @return map by levels
     *
     * @throws Exception _more_
     */
    private Map<String, List<String>> groupVarsByVertLevels(GridDataset gds,
            NcssParamsBean params)
            throws Exception {
        String       no_vert_levels = "no_vert_level";
        List<String> vars           = params.getVar();
        Map<String, List<String>> varsGroupsByLevels = new HashMap<String,
                                                           List<String>>();

        for (String var : vars) {
            GridDatatype grid = gds.findGridDatatype(var);

            //Variables should have been checked before...  
            if (grid == null) {
                throw new IllegalArgumentException("Variable: " + var
                        + " is not contained in the requested dataset");
            }


            CoordinateAxis1D axis =
                grid.getCoordinateSystem().getVerticalAxis();

            String axisKey = null;
            if (axis == null) {
                axisKey = no_vert_levels;
            } else {
                axisKey = axis.getShortName();
            }

            if (varsGroupsByLevels.containsKey(axisKey)) {
                varsGroupsByLevels.get(axisKey).add(var);
            } else {
                List<String> varListForVerlLevel = new ArrayList<String>();
                varListForVerlLevel.add(var);
                varsGroupsByLevels.put(axisKey, varListForVerlLevel);
            }
        }

        return varsGroupsByLevels;
    }


    /**
     * Output the grid as a point form
     *
     * @param request   the request
     * @param entry     the entry
     * @param dataset   the corresponding dataset
     * @param sb        the string buffer
     *
     * @return the result
     *
     * @throws Exception problem creating form
     */
    public Result outputGridAsPointForm(Request request, Entry entry,
                                        GridDataset dataset, StringBuffer sb)
            throws Exception {

        boolean canAdd = getRepository().getAccessManager().canDoNew(request,
                             entry.getParentEntry());

        String formUrl  = request.makeUrl(getRepository().URL_ENTRY_SHOW);
        String fileName = IOUtil.stripExtension(entry.getName()) + "_point";

        String formId   = HU.getUniqueId("form_");

        getPageHandler().entrySectionOpen(request, entry, sb, "Extract Time Series");



        sb.append(HU.form(formUrl + "/" + fileName,
                                 HU.id(formId)));
        sb.append(HU.br());



        sb.append(HU.submit("Get Data", ARG_SUBMIT));
        sb.append(HU.br());
        sb.append(HU.hidden(ARG_OUTPUT, OUTPUT_GRIDASPOINT));
        sb.append(HU.hidden(ARG_ENTRYID, entry.getId()));
        sb.append(HU.formTable());

        List<CalendarDate> dates = getGridDates(dataset);

        StringBuffer       varSB = getVariableForm(dataset, true, false,
                                       true);

        LatLonRect         llr   = dataset.getBoundingBox();
        String             lat   = "";
        String             lon   = "";
        if (llr != null) {
            lat = Misc.format(llr.getLatMin() + llr.getHeight() / 2);
            lon = Misc.format(llr.getCenterLon());
        }
        MapInfo map = getRepository().getMapManager().createMap(request,
                          entry, true, null);
        map.addBox("", "", "", llr,
                   new MapProperties("blue", false, true));
        String llb = map.makeSelector(ARG_LOCATION, true, new String[] { lat,
                lon });
        sb.append(HU.formEntryTop(msgLabel("Location"), llb));

        getCdmManager().addTimeWidget(request, dates, sb);

        List<TwoFacedObject> formats = new ArrayList<TwoFacedObject>();
        formats.add(
            new TwoFacedObject(
                "Comma Separated Values (CSV)",
                SupportedFormat.CSV_STREAM.getFormatName()));

        formats.add(new TwoFacedObject("JSON", FORMAT_JSON));
        formats.add(
            new TwoFacedObject(
                "NetCDF", SupportedFormat.NETCDF3.getFormatName()));
        /* comment out until file sizes are smaller
        //Check if netcdf4 is available
        try {
            if (Nc4Iosp.isClibraryPresent()) {
                formats.add(new TwoFacedObject("NetCDF4",
                        SupportedFormat.NETCDF4.getFormatName()));
            }
        } catch (UnsatisfiedLinkError e) {}
        */
        formats.add(
            new TwoFacedObject(
                "XML", SupportedFormat.XML_STREAM.getFormatName()));


        String format = request.getString(CdmConstants.ARG_FORMAT,
                                          FORMAT_TIMESERIES_CHART);

        sb.append(
            HU.formEntry(
                msgLabel("Format"),
                HU.select(CdmConstants.ARG_FORMAT, formats, format)));
        sb.append(HU.formTableClose());

        sb.append(HU.div("Select Variables",HU.cssClass("ramadda-table-header")+HU.style("margin-top:6px;padding-top:2px;")));
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
        sb.append(HU.submit("Get Data"));
	sb.append(HU.vspace("1em"));
        //sb.append(submitExtra);
        addUrlShowingForm(sb, formId, "[\".*OpenLayers_Control.*\"]");
        sb.append(HU.formClose());

        getPageHandler().entrySectionClose(request, entry, sb);

        return makeLinksResult(request, msg("Grid At Point"), sb,
                               new State(entry));
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
	String toggleAllId = HU.getUniqueId("cbx_");
	HU.div(varSB,HU.labeledCheckbox("", HU.VALUE_TRUE,
					false,HU.attr("id",toggleAllId),
					"Toggle all"),"");

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
                        header += HU.space(3) + "Level:"
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
                varSB.append(HU.div(HU.b(header),""));
            }
            varSB.append(varSB3D);
        }

	varSB.append("\n");
	HU.script(varSB,"HU.initToggleAll('" + toggleAllId+"','.ramadda-grid-variable');");
        return varSB;
    }

    /**
     * Handle a grid as point request
     *
     * @param request  the request
     * @param entry    the entry
     *
     * @return  the result
     *
     * @throws Exception problems
     */
    public Result outputGridAsPoint(Request request, Entry entry)
            throws Exception {
        String format =
            request.getString(CdmConstants.ARG_FORMAT,
                              SupportedFormat.NETCDF3.getFormatName());
        String baseName = IOUtil.stripExtension(entry.getName());
        if (format.equalsIgnoreCase(FORMAT_TIMESERIES)) {
            request.put(CdmConstants.ARG_FORMAT, FORMAT_TIMESERIES_IMAGE);
            String redirectUrl = request.getRequestPath() + "/" + baseName
                                 + ".png" + "?" + request.getUrlArgs();

            return new Result("Point As Grid Time Series Image",
                              new StringBuffer(HU.img(redirectUrl,
                                  "Image is being processed...")));
        }
        StringBuffer sb     = new StringBuffer();
        String       path   = getPath(request, entry);

        GridDataset  gds    = getCdmManager().getGridDataset(entry, path);
	if(gds==null) {
            getPageHandler().entrySectionOpen(request, entry, sb,
					      "Extract Time Series");
	    sb.append(getPageHandler().showDialogWarning("No grids found"));
            getPageHandler().entrySectionClose(request, entry, sb);
            return new Result("Extract Time Series",sb);

	    //	    throw new IllegalStateException("Unable to open the file as a grid");
	}
        OutputType   output = request.getOutput();
        try {
            if (output.equals(OUTPUT_GRIDASPOINT)) {
                Result result = outputGridAsPointProcess(request, entry, gds,
                                    sb);
                if (result != null) {
                    return result;
                }
            }

            return outputGridAsPointForm(request, entry, gds, sb);
        } finally {
            getCdmManager().returnGridDataset(path, gds);
        }
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
        List               tuples = new ArrayList();
        List<GridDatatype> result = new ArrayList<GridDatatype>();
        List<GridDatatype> grids  = dataset.getGrids();
        if (grids == null) {
            return result;
        }
        for (GridDatatype grid : grids) {
            if (grid == null) {
                continue;
            }
            VariableEnhanced var = grid.getVariable();
            tuples.add(new Object[] { var.getShortName().toLowerCase(),
                                      grid });
        }
        tuples = Misc.sortTuples(tuples, true);
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
     * get the parameter name from the raw name
     *
     * @param rawname the raw name
     *
     * @return  the parameter name
     */
    public String getParamName(String rawname) {
        String name  = rawname;
        int    index = rawname.indexOf("[unit=");
        if (index >= 0) {
            name = rawname.substring(0, index);
        }

        return name;
    }

    /**
     * Get the parameter unit from the raw name
     *
     * @param rawname  the raw name
     *
     * @return  the unit or null
     */
    private String getUnitFromName(String rawname) {
        String unit  = null;
        int    index = rawname.indexOf("[unit=");
        if (index >= 0) {
            unit = rawname.substring(index + 6, rawname.indexOf("]"));
            unit = unit.replaceAll("\"", "");
        }

        return unit;
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
    public Result processJsonRequest(Request request) throws Exception {
        String prefix = getRepository().getUrlBase() + "/grid/json";
        Entry  entry  = getCdmManager().findEntryFromPath(request, prefix);
        request.setCORSHeaderOnResponse();
        request.put(CdmConstants.ARG_FORMAT, FORMAT_JSON);
        request.put(ARG_OUTPUT, OUTPUT_GRIDASPOINT.getId());
        return outputGridAsPoint(request, entry);
    }

}
