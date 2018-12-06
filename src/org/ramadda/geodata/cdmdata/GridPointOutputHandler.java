/*
* Copyright (c) 2008-2018 Geode Systems LLC
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


import org.jfree.chart.*;
import org.jfree.chart.axis.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.xy.*;
import org.jfree.data.time.*;
import org.jfree.data.xy.*;
import org.jfree.ui.*;

import org.ramadda.data.record.RecordField;

import org.ramadda.repository.Entry;
import org.ramadda.repository.Link;
import org.ramadda.repository.PageHandler;
import org.ramadda.repository.Repository;
import org.ramadda.repository.Request;
import org.ramadda.repository.Result;
import org.ramadda.repository.auth.AccessException;
import org.ramadda.repository.auth.Permission;
import org.ramadda.repository.map.MapBoxProperties;
import org.ramadda.repository.map.MapInfo;
import org.ramadda.repository.output.OutputHandler;
import org.ramadda.repository.output.OutputType;
import org.ramadda.repository.type.TypeHandler;
import org.ramadda.util.HtmlUtils;

import org.ramadda.util.Json;

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
import ucar.nc2.dt.TrajectoryObsDataset;
import ucar.nc2.dt.TrajectoryObsDatatype;
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
import java.util.regex.Pattern;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;


/**
 * A class for handling CDM data output
 */
public class GridPointOutputHandler extends OutputHandler implements CdmConstants {


    /** Grid as point form Output Type */
    public static final OutputType OUTPUT_GRIDASPOINT_FORM =
        new OutputType("Extract Time Series", "data.gridaspoint.form",
                       OutputType.TYPE_OTHER | OutputType.TYPE_IMPORTANT,
                       OutputType.SUFFIX_NONE, "/cdmdata/chart_line.png",
                       GROUP_DATA);

    /** Grid as point Output Type */
    public static final OutputType OUTPUT_GRIDASPOINT =
        new OutputType("data.gridaspoint", OutputType.TYPE_FEEDS);

    /**
     * _more_
     *
     * @return _more_
     */
    public CdmDataOutputHandler getCdmDataOutputHandler() {
        return (CdmDataOutputHandler) getRepository().getOutputHandler(
            CdmDataOutputHandler.class);
    }


    /**
     * Get the CdmManager
     *
     * @return  the CDM data manager
     */
    public CdmManager getCdmManager() {
        return getCdmDataOutputHandler().getCdmManager();
    }




    /**
     * Get the path for the Entry
     *
     *
     * @param request the Request
     * @param entry   the Entry
     *
     * @return   the path
     *
     * @throws Exception problem getting the path
     */
    public String getPath(Request request, Entry entry) throws Exception {
        return getCdmManager().getPath(request, entry);
    }

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

        List<String> varNames = new ArrayList<String>();
        Hashtable    args     = request.getArgs();
        //Look for both variable.<varname>=true  and variable=<varname> url arguments
        for (Enumeration keys = args.keys(); keys.hasMoreElements(); ) {
            String arg = (String) keys.nextElement();
            if (arg.startsWith(VAR_PREFIX) && request.get(arg, false)) {
                varNames.add(arg.substring(VAR_PREFIX.length()));
            }
        }

        List<String> selectedVars = request.get(ARG_VARIABLE,
                                        new ArrayList<String>());
        //Support a comma separated list
        for (String var : selectedVars) {
            varNames.addAll(StringUtil.split(var, ",", true, true));
        }


        //For now add either the 2d or the 3d vars
        if (varNames.size() == 0) {
            List<GridDatatype> grids = sortGrids(gds);
            for (GridDatatype grid : grids) {
                VariableEnhanced var = grid.getVariable();
                if (grid.getZDimension() == null) {
                    varNames.add(var.getShortName());
                }
            }
            if (varNames.size() == 0) {
                for (GridDatatype grid : grids) {
                    VariableEnhanced var = grid.getVariable();
                    if (grid.getZDimension() != null) {
                        varNames.add(var.getShortName());
                    }
                }
            }
        }

        //        System.err.println(varNames);
        LatLonRect llr    = gds.getBoundingBox();
        double     deflat = 0;
        double     deflon = 0;
        if (llr != null) {
            deflat = llr.getLatMin() + llr.getHeight() / 2;
            deflon = llr.getCenterLon();
        }
        LatLonPointImpl llp = null;

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
        if ( !allDates.isEmpty()) {  // have to have some dates
            if (calString == null) {
                calString = allDates.get(0).getCalendar().toString();
            }
            if (request.defined(ARG_FROMDATE)) {
                String fromDateString = request.getString(ARG_FROMDATE,
                                            formatDate(request,
                                                allDates.get(0)));
                dates[0] = CalendarDate.parseISOformat(calString,
                        fromDateString);
            }
            if (request.defined(ARG_TODATE)) {
                String toDateString = request.getString(ARG_TODATE,
                                          formatDate(request,
                                              allDates.get(allDates.size()
                                                  - 1)));
                dates[1] = CalendarDate.parseISOformat(calString,
                        toDateString);
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
            // modelled after thredds.server.ncSubset.controller.PointDataController
            try {
                return processPointRequest(request, entry, gds, varNames,
                                           llp, dates, allDates);
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
                    json.append(Json.map("error", Json.quote(message),
                                         "errorcode", Json.quote(code)));
                    Result result = new Result("", json, Json.MIMETYPE);

                    //                    result.setResponseCode(Result.RESPONSE_INTERNALERROR);
                    return result;
                }

                throw exc;
            }
        }

        return new Result("", sb);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param gds _more_
     * @param varNames _more_
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
                                       List<String> varNames,
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

        SupportedFormat sf   = getSupportedFormat(format);
        NcssParamsBean  pdrb = new NcssParamsBean();
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

        String baseName = IOUtil.stripExtension(entry.getName());
        if (format.equalsIgnoreCase(FORMAT_TIMESERIES_CHART)) {
            request.put(CdmConstants.ARG_FORMAT, FORMAT_JSON);
            request.put(ARG_LATITUDE, "_LATITUDEMACRO_");
            request.put(ARG_LONGITUDE, "_LONGITUDEMACRO_");
            StringBuffer html = new StringBuffer();
            getPageHandler().entrySectionOpen(request, entry, html,
                    "Time Series", true);

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
                List<RecordField> fields = new ArrayList<RecordField>();
                for (int i = 0; i < varNames.size(); i++) {
                    String var = (String) varNames.get(i);
                    RecordField recordField = new RecordField(var, var, var,
                                                  i, "");
                    recordField.setChartable(true);
                    fields.add(recordField);
                }
                RecordField.addJsonHeader(bw, entry.getName(), fields, false,
                                          false, false);

                String  line        = null;
                int     cnt         = 0;
                boolean hasVertical = (pdrb.getVertCoord() != null);

                //                    System.err.println ("has vert:" + hasVertical);
                //                    System.err.println ("vars:" + varNames.size() +" " + varNames);
                while ((line = br.readLine()) != null) {
                    cnt++;
                    List<String> toks = StringUtil.split(line, ",", true,
                                            true);
                    if (cnt == 1) {
                        //                            System.err.println ("line:" + line);
                        //time/lat/lon  maybeZ vars
                        if (toks.size() == 3 + 1 + varNames.size()) {
                            hasVertical = true;
                        }

                        continue;
                    }
                    if (cnt > 2) {
                        bw.append(",");
                    }
                    bw.append("\n");
                    bw.append(Json.mapOpen());
                    //       date            lat   lon   alt     value(s)
                    // 2009-11-10T00:00:00Z,34.6,-101.1,100.0,207.89999389648438
                    CalendarDate date =
                        CalendarDate.parseISOformat(toks.get(0), toks.get(0));
                    double lat = Double.parseDouble(toks.get(1));
                    double lon = Double.parseDouble(toks.get(2));
                    double alt = (hasVertical
                                  ? Double.parseDouble(toks.get(3))
                                  : Double.NaN);
                    Json.addGeolocation(bw, lat, lon, alt);
                    bw.append(",");
                    bw.append(Json.attr(Json.FIELD_DATE, date.getMillis()));
                    bw.append(",");
                    bw.append(Json.mapKey(Json.FIELD_VALUES));
                    int          startIdx = (hasVertical
                                             ? 4
                                             : 3);
                    List<String> values   = new ArrayList();
                    for (int i = startIdx; i < toks.size(); i++) {
                        double v = Double.parseDouble(toks.get(i));
                        values.add(Json.formatNumber(v));
                    }
                    bw.append(Json.list(values));
                    bw.append(Json.mapClose());
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
        if (format.equalsIgnoreCase(FORMAT_TIMESERIES_IMAGE)) {
            result = outputTimeSeriesImage(request, entry, f);
        } else {
            result = new Result(getStorageManager().getFileInputStream(f),
                                pdrb.getAccept());
            //Set return filename sets the Content-Disposition http header so the browser saves the file
            //with the correct name and suffix
            result.setReturnFilename(baseName + "_pointsubset" + suffix);
        }

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

        boolean canAdd =
            getRepository().getAccessManager().canDoAction(request,
                entry.getParentEntry(), Permission.ACTION_NEW);

        String formUrl  = request.makeUrl(getRepository().URL_ENTRY_SHOW);
        String fileName = IOUtil.stripExtension(entry.getName()) + "_point";

        String formId   = HtmlUtils.getUniqueId("form_");

        getPageHandler().entrySectionOpen(request, entry, sb, "Time Series",
                                          true);



        sb.append(HtmlUtils.form(formUrl + "/" + fileName,
                                 HtmlUtils.id(formId)));
        sb.append(HtmlUtils.br());



        sb.append(HtmlUtils.submit("Get Point", ARG_SUBMIT));
        sb.append(HtmlUtils.br());
        sb.append(HtmlUtils.hidden(ARG_OUTPUT, OUTPUT_GRIDASPOINT));
        sb.append(HtmlUtils.hidden(ARG_ENTRYID, entry.getId()));
        sb.append(HtmlUtils.formTable());

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
                          true, null);
        map.addBox("","", "", llr, new MapBoxProperties("blue", false, true));
        String llb = map.makeSelector(ARG_LOCATION, true, new String[] { lat,
                lon });
        sb.append(HtmlUtils.formEntryTop(msgLabel("Location"), llb));

        addTimeWidget(request, dates, sb);

        List<TwoFacedObject> formats = new ArrayList<TwoFacedObject>();
        formats.add(new TwoFacedObject("Interactive Time Series",
                                       FORMAT_TIMESERIES_CHART));

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
                "Comma Separated Values (CSV)",
                SupportedFormat.CSV_STREAM.getFormatName()));
        formats.add(new TwoFacedObject("Time Series Image",
                                       FORMAT_TIMESERIES));
        formats.add(
            new TwoFacedObject(
                "XML", SupportedFormat.XML_STREAM.getFormatName()));


        String format = request.getString(CdmConstants.ARG_FORMAT,
                                          FORMAT_TIMESERIES_CHART);

        sb.append(
            HtmlUtils.formEntry(
                msgLabel("Format"),
                HtmlUtils.select(CdmConstants.ARG_FORMAT, formats, format)));
        addPublishWidget(request, entry, sb,
                         msg("Select a folder to publish the results to"));
        sb.append(HtmlUtils.formTableClose());
        sb.append("<hr>");
        sb.append(msgLabel("Select Variables"));
        sb.append(HtmlUtils.insetDiv(HtmlUtils.table(varSB.toString(),
                HtmlUtils.attrs(HtmlUtils.ATTR_CELLPADDING, "5",
                                HtmlUtils.ATTR_CELLSPACING, "0")), 0, 30, 0,
                                    0));

        sb.append(HtmlUtils.submit("Get Point"));
        //sb.append(submitExtra);
        addUrlShowingForm(sb, formId, "[\".*OpenLayers_Control.*\"]");
        sb.append(HtmlUtils.formClose());

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
            //                            ARG_VARIABLE + "." + var.getShortName(),
            ARG_VARIABLE,
            /*HtmlUtils.VALUE_TRUE,*/
            var.getShortName(), (grids.size() == 1),
                                HtmlUtils.id(cbxId)
                                + call) + HtmlUtils.space(1)
                                        + var.getShortName()
                                        + HtmlUtils.space(1)
                                        + ((var.getUnitsString() != null)
                                           ? "(" + var.getUnitsString() + ")"
                                           : ""), "<i>"
                                           + var.getDescription() + "</i>")));

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
                              new StringBuffer(HtmlUtils.img(redirectUrl,
                                  "Image is being processed...")));
        }
        StringBuffer sb     = new StringBuffer();
        String       path   = getPath(request, entry);

        GridDataset  gds    = getCdmManager().getGridDataset(entry, path);
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
                //formattedDates.add(getPageHandler().formatDate(request, date.toDate()));
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
                                    PageHandler.DEFAULT_TIME_FORMAT);

            return new CalendarDateFormatter(dateFormat).toString(
                (CalendarDate) date);
        } else if (date instanceof Date) {
            return getPageHandler().formatDate(request, (Date) date);
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
     * Output the timeseries image
     *
     * @param request the request
     * @param entry  the entry
     * @param f  the file
     *
     * @return  the image
     *
     * @throws Exception  problem creating image
     */
    private Result outputTimeSeriesImage(Request request, Entry entry, File f)
            throws Exception {

        StringBuffer sb = new StringBuffer();
        //sb.append(getHeader(request, entry));
        sb.append(header(msg("Chart")));

        TimeSeriesCollection dummy  = new TimeSeriesCollection();
        JFreeChart chart = createChart(request, entry, dummy);
        XYPlot               xyPlot = (XYPlot) chart.getPlot();

        Hashtable<String, MyTimeSeries> seriesMap = new Hashtable<String,
                                                        MyTimeSeries>();
        List<MyTimeSeries> allSeries = new ArrayList<MyTimeSeries>();
        int     paramCount = 0;
        int     colorCount = 0;
        boolean axisLeft   = true;
        Hashtable<String, List<ValueAxis>> axisMap = new Hashtable<String,
                                                         List<ValueAxis>>();
        Hashtable<String, double[]> rangeMap = new Hashtable<String,
                                                   double[]>();
        List<String> units      = new ArrayList<String>();
        List<String> paramUnits = new ArrayList<String>();
        List<String> paramNames = new ArrayList<String>();

        long         t1         = System.currentTimeMillis();
        String contents =
            IOUtil.readContents(getStorageManager().getFileInputStream(f));
        List<String> lines      = StringUtil.split(contents, "\n", true,
                                      true);
        String       header     = lines.get(0);
        String[]     headerToks = header.split(",");
        for (int i = 0; i < headerToks.length; i++) {
            paramNames.add(getParamName(headerToks[i]));
            paramUnits.add(getUnitFromName(headerToks[i]));
        }
        boolean hasLevel   = paramNames.get(3).equals("vertCoord");

        boolean readHeader = false;
        for (String line : lines) {
            if ( !readHeader) {
                readHeader = true;

                continue;
            }
            String[] lineTokes = line.split(",");
            Date     date      = DateUtil.parse(lineTokes[0]);
            int      startIdx  = hasLevel
                                 ? 4
                                 : 3;
            for (int i = startIdx; i < lineTokes.length; i++) {
                double value = Double.parseDouble(lineTokes[i]);
                if (value != value) {
                    continue;
                }
                List<ValueAxis> axises     = null;
                double[]        range      = null;
                String          u          = paramUnits.get(i);
                String          paramName  = paramNames.get(i);
                String          formatName = paramName.replaceAll("_", " ");
                String formatUnit = ((u == null) || (u.length() == 0))
                                    ? ""
                                    : "[" + u + "]";
                if (u != null) {
                    axises = axisMap.get(u);
                    range  = rangeMap.get(u);
                    if (axises == null) {
                        axises = new ArrayList<ValueAxis>();
                        range  = new double[] { value, value };
                        rangeMap.put(u, range);
                        axisMap.put(u, axises);
                        units.add(u);
                    }
                    range[0] = Math.min(range[0], value);
                    range[1] = Math.max(range[1], value);
                }
                MyTimeSeries series = seriesMap.get(paramName);
                if (series == null) {
                    paramCount++;
                    TimeSeriesCollection dataset = new TimeSeriesCollection();
                    series = new MyTimeSeries(formatName,
                            FixedMillisecond.class);
                    allSeries.add(series);
                    ValueAxis rangeAxis = new NumberAxis(formatName + " "
                                              + formatUnit);
                    if (axises != null) {
                        axises.add(rangeAxis);
                    }
                    XYItemRenderer renderer =
                        new XYAreaRenderer(XYAreaRenderer.LINES);
                    if (colorCount >= HtmlUtils.COLORS.length) {
                        colorCount = 0;
                    }
                    renderer.setSeriesPaint(0, HtmlUtils.COLORS[colorCount]);
                    colorCount++;
                    xyPlot.setRenderer(paramCount, renderer);
                    xyPlot.setRangeAxis(paramCount, rangeAxis, false);
                    AxisLocation side = (axisLeft
                                         ? AxisLocation.TOP_OR_LEFT
                                         : AxisLocation.BOTTOM_OR_RIGHT);
                    axisLeft = !axisLeft;
                    xyPlot.setRangeAxisLocation(paramCount, side);

                    dataset.setDomainIsPointsInTime(true);
                    dataset.addSeries(series);
                    seriesMap.put(paramNames.get(i), series);
                    xyPlot.setDataset(paramCount, dataset);
                    xyPlot.mapDatasetToRangeAxis(paramCount, paramCount);
                }
                //series.addOrUpdate(new FixedMillisecond(pointData.date),value);
                TimeSeriesDataItem item =
                    new TimeSeriesDataItem(new FixedMillisecond(date), value);
                series.addItem(item);
            }
        }



        for (MyTimeSeries timeSeries : allSeries) {
            timeSeries.finish();
        }

        for (String unit : units) {
            List<ValueAxis> axises = axisMap.get(unit);
            double[]        range  = rangeMap.get(unit);
            for (ValueAxis rangeAxis : axises) {
                rangeAxis.setRange(new org.jfree.data.Range(range[0],
                        range[1]));
            }
        }


        long t2 = System.currentTimeMillis();

        BufferedImage newImage =
            chart.createBufferedImage(request.get(ARG_IMAGE_WIDTH, 1000),
                                      request.get(ARG_IMAGE_HEIGHT, 400));
        long t3 = System.currentTimeMillis();
        //System.err.println("timeseries image time:" + (t2 - t1) + " "
        //                   + (t3 - t2));

        File file = getStorageManager().getTmpFile(request, "point.png");
        ImageUtils.writeImageToFile(newImage, file);
        InputStream is     = getStorageManager().getFileInputStream(file);
        Result      result = new Result("", is, "image/png");

        return result;

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
     * A wrapper for TimeSeries
     *
     * @author RAMADDA Development Team
     */
    private static class MyTimeSeries extends TimeSeries {

        /** the items */
        List<TimeSeriesDataItem> items = new ArrayList<TimeSeriesDataItem>();

        /** seen items */
        HashSet<TimeSeriesDataItem> seen = new HashSet<TimeSeriesDataItem>();

        /**
         * Construct the time series
         *
         * @param name  the name
         * @param c     the class
         */
        public MyTimeSeries(String name, Class c) {
            super(name, c);
        }

        /**
         * Add an item to the timeseries
         *
         * @param item  the item to add
         */
        public void addItem(TimeSeriesDataItem item) {
            if (seen.contains(item)) {
                return;
            }
            seen.add(item);
            items.add(item);
        }

        /**
         * finish this
         */
        public void finish() {
            items = new ArrayList<TimeSeriesDataItem>(Misc.sort(items));

            for (TimeSeriesDataItem item : items) {
                this.data.add(item);
            }
            fireSeriesChanged();
        }


    }


    /**
     * Create the chart
     *
     *
     * @param request  the request
     * @param entry    the entry
     * @param dataset  the dataset
     *
     * @return the chart
     */
    private static JFreeChart createChart(Request request, Entry entry,
                                          XYDataset dataset) {
        LatLonPointImpl llp =
            new LatLonPointImpl(request.getLatOrLonValue(ARG_LOCATION
                + ".latitude", 0), request.getLatOrLonValue(ARG_LOCATION
                               + ".longitude", 0));
        String     title = entry.getName() + " at " + llp.toString();

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
        //entry.getName(),  // title
        title,    // title
        "Date",   // x-axis label
        "",       // y-axis label
        dataset,  // data
        true,     // create legend?
        true,     // generate tooltips?
        false     // generate URLs?
            );

        chart.setBackgroundPaint(Color.white);
        ValueAxis rangeAxis = new NumberAxis("");
        rangeAxis.setVisible(false);
        XYPlot plot = (XYPlot) chart.getPlot();
        if (request.get("gray", false)) {
            plot.setBackgroundPaint(Color.lightGray);
            plot.setDomainGridlinePaint(Color.white);
            plot.setRangeGridlinePaint(Color.white);
        } else {
            plot.setBackgroundPaint(Color.white);
            plot.setDomainGridlinePaint(Color.lightGray);
            plot.setRangeGridlinePaint(Color.lightGray);
        }
        plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
        plot.setDomainCrosshairVisible(true);
        plot.setRangeCrosshairVisible(true);
        plot.setRangeAxis(0, rangeAxis, false);


        XYItemRenderer r    = plot.getRenderer();
        DateAxis       axis = (DateAxis) plot.getDomainAxis();
        //axis.setDateFormatOverride(new SimpleDateFormat("MMM-yyyy"));

        return chart;

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
