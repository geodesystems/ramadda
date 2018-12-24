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

package org.ramadda.data.services;


import org.jfree.chart.*;
import org.jfree.chart.axis.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.xy.*;
import org.jfree.data.general.*;
import org.jfree.data.time.*;
import org.jfree.data.xy.*;
import org.jfree.ui.*;


import org.ramadda.data.point.*;
import org.ramadda.data.point.*;

import org.ramadda.data.point.PointFile;


import org.ramadda.data.record.*;



import org.ramadda.data.record.*;
import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.job.*;
import org.ramadda.repository.map.*;
import org.ramadda.repository.metadata.Metadata;
import org.ramadda.repository.output.*;

import org.ramadda.util.ColorTable;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Json;
import org.ramadda.util.Utils;
import org.ramadda.util.grid.*;

import ucar.unidata.geoloc.Bearing;
import ucar.unidata.geoloc.LatLonPointImpl;



import ucar.unidata.ui.ImageUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.*;

import java.io.*;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;


import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;


/**
 *
 * @author         Jeff McWhirter
 */
public class PointFormHandler extends RecordFormHandler {

    /** _more_ */
    public static final String FIELD_ALTITUDE = "dflt_altitude";


    /** _more_ */
    private static IdwGrid dummyField1 = null;

    /** _more_ */
    private static org.ramadda.service.ServiceProvider dummyServiceInfoProvider =
        null;

    /** _more_ */
    private static ObjectGrid dummyField2 = null;

    /** _more_ */
    private static GridUtils dummyField3 = null;

    /** _more_ */
    private static Gridder dummyField4 = null;

    /** _more_ */
    private static GridVisitor dummyField5 = null;

    /** _more_ */
    private RecordFileFactory dummyField6 = null;

    /** _more_ */
    private PointTypeHandler dummyField7 = null;

    /** _more_ */
    private RecordCollectionTypeHandler dummyField8 = null;

    /** _more_ */
    private RecordApiHandler dummyField9 = null;

    /** _more_ */
    private PointJobManager dummyField10 = null;

    /** _more_ */
    private RecordCollectionHarvester dummyField11 = null;

    /** _more_ */
    private PointCollectionTypeHandler dummyField12 = null;

    /** _more_ */
    public static final String LABEL_ALTITUDE = "Altitude";

    /** _more_ */
    public static List<Integer> xindices = new ArrayList<Integer>();

    /** _more_ */
    public static int[] drawCnt = { 0 };

    /** _more_ */
    public static boolean debugChart = false;


    /**
     * ctor
     *
     * @param recordOutputHandler _more_
     */
    public PointFormHandler(PointOutputHandler recordOutputHandler) {
        super(recordOutputHandler);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public PointOutputHandler getPointOutputHandler() {
        return (PointOutputHandler) getOutputHandler();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getSessionPrefix() {
        return "points.";
    }

    /**
     * Adds the grid oriented output formats
     *
     * @param outputs List of html selectors (which hold id, label and icon)
     * @param forCollection Are the grid formats for a point collection
     */
    public void getGridFormats(List<HtmlUtils.Selector> outputs,
                               boolean forCollection) {
        outputs.add(getSelect(getPointOutputHandler().OUTPUT_IMAGE));
        outputs.add(getSelect(getPointOutputHandler().OUTPUT_HILLSHADE));
        outputs.add(getSelect(getPointOutputHandler().OUTPUT_KMZ));
        outputs.add(getSelect(getPointOutputHandler().OUTPUT_ASC));
        //        outputs.add(getSelect(getPointOutputHandler().OUTPUT_NC));
    }



    /**
     * make the map lines for the given ldiar entry
     *
     * @param request the request
     * @param recordEntry The entry
     * @param map the map to add the lines to
     * @param lineCnt how many
     *
     * @throws Exception on badness
     */
    public void makeMapLines(Request request, RecordEntry recordEntry,
                             MapInfo map, int lineCnt)
            throws Exception {
        map.addLines(recordEntry.getEntry(), "",
                     getMapPolyline(request, recordEntry), null);
    }


    /**
     * add the lines to the map
     *
     * @param request the request
     * @param entry the entry
     * @param map the map
     */
    public void addToMap(Request request, Entry entry, MapInfo map) {
        try {
            RecordEntry recordEntry = new RecordEntry(getOutputHandler(),
                                          request, entry);
            List<double[]> polyLine = getMapPolyline(request, recordEntry);
            map.addLines(entry, "", polyLine, null);
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }


    /**
     * create the polyline for the given entry. This will cache it in the RAMADDA entry
     *
     * @param request the request
     * @param recordEntry _more_
     *
     * @return the map lines
     *
     * @throws Exception On badness
     */
    public List<double[]> getMapPolyline(Request request,
                                         RecordEntry recordEntry)
            throws Exception {
        long numRecords = recordEntry.getNumRecords();
        int skipFactor = (int) (numRecords
                                / request.get(ARG_NUMPOINTS, 1000));
        if (skipFactor == 0) {
            skipFactor = 1000;
        }



        String polylineProperty = "mapline" + skipFactor;

        List<double[]> polyLine =
            (List<double[]>) recordEntry.getEntry().getTransientProperty(
                polylineProperty);
        if (polyLine == null) {
            final List<double[]> pts         = new ArrayList<double[]>();
            final Bearing        workBearing = new Bearing();
            RecordVisitor visitor =
                new BridgeRecordVisitor(getOutputHandler()) {
                double[] lastPoint;
                double   maxDistance   = 0;
                double   totalDistance = 0;
                int      cnt           = 0;
                public boolean doVisitRecord(RecordFile file,
                                             VisitInfo visitInfo,
                                             Record record) {
                    PointRecord pointRecord = (PointRecord) record;
                    double[] pt = new double[] { pointRecord.getLatitude(),
                            pointRecord.getLongitude() };
                    //Keep track of the distances we've seen and put a nan to break the line
                    if (lastPoint != null) {
                        //If there is more than a 2 degree difference then put a break;
                        if ((Math.abs(pt[0] - lastPoint[0]) > 2)
                                || (Math.abs(pt[1] - lastPoint[1]) > 2)) {
                            pts.add(new double[] { Double.NaN, Double.NaN });
                        } else {
                            LatLonPointImpl p1 = new LatLonPointImpl(pt[0],
                                                     pt[1]);
                            LatLonPointImpl p2 =
                                new LatLonPointImpl(lastPoint[0],
                                    lastPoint[1]);
                            double distance = Bearing.calculateBearing(p1,
                                                  p2, null).getDistance();
                            //System.err.println(pt[0] +" " + pt[1] + " distance:" + distance +" max:" + maxDistance);
                            if ((maxDistance != 0)
                                    && (distance > maxDistance * 5)) {
                                //                                    System.err.println("BREAK");
                                pts.add(new double[] { Double.NaN,
                                        Double.NaN });
                                distance = 0;
                            }
                            maxDistance = Math.max(maxDistance, distance);
                        }
                    }
                    pts.add(pt);
                    lastPoint = pt;
                    cnt++;
                    if (cnt > 100) {
                        cnt         = 0;
                        maxDistance = 0;
                    }

                    return true;
                }
            };

            getRecordJobManager().visitSequential(request, recordEntry,
                    visitor,
                    new VisitInfo(VisitInfo.QUICKSCAN_YES, skipFactor));
            polyLine = pts;
            recordEntry.getEntry().putTransientProperty(polylineProperty,
                    polyLine);
        }

        return polyLine;
    }

    /**
     * Show the products form
     *
     * @param request the request
     * @param group the entry group
     * @param subGroups sub groups
     * @param entries sub entries
     *
     * @return the ramadda result
     *
     * @throws Exception on badness
     */
    public Result outputGroupForm(Request request, Entry group,
                                  List<Entry> subGroups, List<Entry> entries)
            throws Exception {
        return outputGroupForm(request, group, subGroups, entries,
                               new StringBuffer());
    }


    /**
     * Show the products form
     *
     * @param request the request
     * @param group the group
     * @param subGroups sub groups
     * @param entries sub entries
     * @param msgSB _more_
     *
     * @return the ramadda result
     *
     * @throws Exception on badness
     */
    public Result outputGroupForm(Request request, Entry group,
                                  List<Entry> subGroups, List<Entry> entries,
                                  StringBuffer msgSB)
            throws Exception {
        StringBuilder sb = new StringBuilder();
        request.getRepository().getPageHandler().entrySectionOpen(request,
                group, sb, "Point Processing");
        sb.append(msgSB);
        boolean showUrl = request.get(ARG_SHOWURL, false);

        String  formId  = HtmlUtils.getUniqueId("form_");
        sb.append(request.formPost(getRepository().URL_ENTRY_SHOW,
                                   HtmlUtils.id(formId)));
        sb.append(HtmlUtils.hidden(ARG_ENTRYID, group.getId()));
        sb.append(HtmlUtils.hidden(ARG_RECORDENTRY_CHECK, "true"));

        List<? extends RecordEntry> recordEntries =
            getOutputHandler().makeRecordEntries(request, entries, false);

        StringBuffer entrySB = new StringBuffer();
        entrySB.append("<table width=100%>");
        entrySB.append(
            "<tr><td><b>File</b></td><td><b># Points</b></td></tr>");
        long totalSize = 0;

        for (RecordEntry recordEntry : recordEntries) {
            Entry entry = recordEntry.getEntry();
            entrySB.append("<tr><td>");

            entrySB.append(HtmlUtils.checkbox(ARG_RECORDENTRY, entry.getId(),
                    true));
            entrySB.append(getOutputHandler().getEntryLink(request, entry));
            entrySB.append("</td><td align=right>");
            long numRecords = recordEntry.getNumRecords();
            totalSize += numRecords;
            entrySB.append(formatPointCount(numRecords));
            entrySB.append("</td></tr>");
        }
        if (recordEntries.size() > 1) {
            entrySB.append("<tr><td>" + msgLabel("Total")
                           + "</td><td align=right>"
                           + formatPointCount(totalSize));
            entrySB.append("</td></tr>");
        }
        entrySB.append("</table>");

        if (recordEntries.size() == 0) {
            sb.append(getPageHandler().showDialogNote(msg("No data files")));

            sb.append(HtmlUtils.sectionClose());

            request.getRepository().getPageHandler().entrySectionClose(
                request, group, sb);

            return new Result("", sb);
        }


        String files;
        if (recordEntries.size() == 1) {
            files = "<table width=100%><tr><td width=75%>"
                    + entrySB.toString()
                    + "</td><td width=25%>&nbsp;</td><tr></table>";

        } else {
            files =
                "<table width=100%><tr><td width=75%>"
                + HtmlUtils.div(entrySB.toString(), HtmlUtils.style("max-height:100px;  overflow-y: auto; border: 1px #999999 solid;"))
                + "</td><td width=25%>&nbsp;</td><tr></table>";
        }

        String extra = HtmlUtils.formEntryTop((recordEntries.size() == 1)
                ? ""
                : msgLabel("Files"), files);

        addToGroupForm(request, group, sb, recordEntries, extra);


        sb.append("<p>");
        sb.append(HtmlUtils.submit(msg("Get Data"), ARG_GETDATA));
        sb.append("<p>");
        OutputHandler.addUrlShowingForm(sb, formId,
                                        "[\".*OpenLayers_Control.*\"]");
        sb.append(HtmlUtils.formClose());

        sb.append(HtmlUtils.sectionClose());

        request.getRepository().getPageHandler().entrySectionClose(request,
                group, sb);

        return new Result("", sb);
    }



    /**
     * make the product/subset form
     *
     * @param request the request
     * @param entry The entry
     *
     * @return ramadda result
     *
     * @throws Exception on badness
     */
    public Result outputEntryForm(Request request, Entry entry)
            throws Exception {
        return outputEntryForm(request, entry, new StringBuffer());
    }



    /**
     * make the form
     *
     * @param request the request
     * @param entry The entry
     * @param msgSB _more_
     *
     * @return ramadda result
     *
     * @throws Exception on badness
     */
    public Result outputEntryForm(Request request, Entry entry,
                                  Appendable msgSB)
            throws Exception {


        StringBuilder sb = new StringBuilder();
        request.getRepository().getPageHandler().entrySectionOpen(request,
                entry, sb, "Point Processing");
        sb.append(msgSB);

        RecordEntry recordEntry =
            getPointOutputHandler().doMakeEntry(request, entry);
        String formId = HtmlUtils.getUniqueId("form_");
        sb.append(request.formPost(getRepository().URL_ENTRY_SHOW,
                                   HtmlUtils.id(formId)));
        sb.append(HtmlUtils.hidden(ARG_ENTRYID, entry.getId()));
        addToEntryForm(request, entry, sb, recordEntry);
        sb.append("<p>");
        sb.append(HtmlUtils.submit(msg("Get Data"), ARG_GETDATA));
        sb.append("<p>");
        OutputHandler.addUrlShowingForm(sb, formId,
                                        "[\".*OpenLayers_Control.*\"]");
        sb.append(HtmlUtils.formClose());

        sb.append(HtmlUtils.sectionClose());

        request.getRepository().getPageHandler().entrySectionClose(request,
                entry, sb);

        return new Result("", sb);
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param sb _more_
     * @param recordEntry _more_
     *
     * @throws Exception _more_
     */
    public void addToEntryForm(Request request, Entry entry, Appendable sb,
                               RecordEntry recordEntry)
            throws Exception {

        sb.append(HtmlUtils.submit(msg("Get Data"), ARG_GETDATA));
        sb.append("<p>");
        if (entry.getTypeHandler() instanceof RecordTypeHandler) {
            ((RecordTypeHandler) entry.getTypeHandler()).addToProcessingForm(
                request, entry, sb);
        }

        addSelectForm(request, entry, sb, false, recordEntry, "");
        addSettingsForm(request, entry, sb, recordEntry);

    }


    /**
     * _more_
     *
     * @param request _more_
     * @param group _more_
     * @param sb _more_
     * @param recordEntries _more_
     * @param extra _more_
     *
     * @throws Exception _more_
     */
    public void addToGroupForm(Request request, Entry group, Appendable sb,
                               List<? extends RecordEntry> recordEntries,
                               String extra)
            throws Exception {
        sb.append(HtmlUtils.submit(msg("Get Data"), ARG_GETDATA));
        sb.append("<p>");
        if (group.getTypeHandler() instanceof RecordTypeHandler) {
            ((RecordTypeHandler) group.getTypeHandler()).addToProcessingForm(
                request, group, sb);
        }
        addSelectForm(request, group, sb, true, recordEntries.get(0), extra);
        addSettingsForm(request, group, sb, recordEntries.get(0));
    }




    /**
     * add the Settings
     *
     * @param request request
     * @param entry the entry
     * @param sb buffer to append to
     * @param recordEntry the recordentry
     *
     * @throws Exception On badness
     */
    public void addSettingsForm(Request request, Entry entry, Appendable sb,
                                RecordEntry recordEntry)
            throws Exception {

        boolean      showUrl = request.get(ARG_SHOWURL, false);
        StringBuffer extra   = new StringBuffer();
        extra.append(HtmlUtils.formTable());
        String initialDegrees = "" + getDefaultRadiusDegrees(request,
                                    entry.getBounds());

        if (request.defined(ARG_GRID_RADIUS_DEGREES)) {
            initialDegrees = request.getString(ARG_GRID_RADIUS_DEGREES, "");
        }

        String initialCells = "0";
        if (request.defined(ARG_GRID_RADIUS_CELLS)) {
            initialCells = request.getString(ARG_GRID_RADIUS_CELLS, "");
        }
        extra.append(HtmlUtils.hidden(ARG_GRID_RADIUS_DEGREES_ORIG,
                                      initialDegrees));
        extra.append(
            HtmlUtils.formEntry(
                msgLabel("Grid radius for IDW"),
                msgLabel("Degrees")
                + HtmlUtils.input(
                    ARG_GRID_RADIUS_DEGREES, initialDegrees,
                    12) + HtmlUtils.space(4) + msgLabel("or # of grid cells")
                        + HtmlUtils.input(
                            ARG_GRID_RADIUS_CELLS, initialCells, 4)));


        extra.append(
            HtmlUtils.formEntry(
                msgLabel("Power"),
                HtmlUtils.input(
                    RecordConstants.ARG_GRID_POWER,
                    request.getString(RecordConstants.ARG_GRID_POWER, "1.0"),
                    5) + " "
                       + HtmlUtils.href(
                           "https://gisgeography.com/inverse-distance-weighting-idw-interpolation/",
                           "More Information")));
        extra.append(
            HtmlUtils.formEntry(
                msgLabel("Minimum # of Points"),
                HtmlUtils.input(
                    RecordConstants.ARG_GRID_MINPOINTS,
                    request.getString(
                        RecordConstants.ARG_GRID_MINPOINTS, "1"), 5)));

        /*
        extra.append(
            HtmlUtils.formEntry(
                msgLabel("Fill missing"),
                HtmlUtils.checkbox(
                    PointOutputHandler.ARG_FILLMISSING, "true",
                    request.get(PointOutputHandler.ARG_FILLMISSING, false))));
        */

        extra.append(
            HtmlUtils.formEntry(
                msgLabel("Threshold"),
                HtmlUtils.input(
                    RecordConstants.ARG_THRESHOLD,
                    request.getString(RecordConstants.ARG_THRESHOLD, ""),
                    5)));



        extra.append(
            HtmlUtils.formEntry(
                msgLabel("Hill shading"),
                msgLabel("Azimuth")
                + HtmlUtils.input(
                    ARG_HILLSHADE_AZIMUTH,
                    request.getString(ARG_HILLSHADE_AZIMUTH, "315"),
                    4) + HtmlUtils.space(4) + msgLabel("Angle")
                       + HtmlUtils.input(
                           ARG_HILLSHADE_ANGLE,
                           request.getString(ARG_HILLSHADE_ANGLE, "45"), 4)));
        extra.append(
            HtmlUtils.formEntry(
                msgLabel("Image Dimensions"),
                HtmlUtils.input(
                    ARG_WIDTH, request.getString(ARG_WIDTH, "" + DFLT_WIDTH),
                    5) + " X "
                       + HtmlUtils.input(
                           ARG_HEIGHT,
                           request.getString(ARG_HEIGHT, "" + DFLT_HEIGHT),
                           5)));


        String paramWidget = null;
        List   params      = new ArrayList();
        //TODO: we need a better way to say this is a elevation point cloud
        //        if(pointEntry.isCapable(PointFile.ACTION_ELEVATION)) {
        params.add(new TwoFacedObject(msg(LABEL_ALTITUDE), ""));
        //        }
        if (recordEntry != null) {
            for (RecordField field :
                    recordEntry.getRecordFile().getChartableFields()) {
                params.add(new TwoFacedObject(field.getLabel(),
                        "" + field.getParamId()));
            }
        }

        if (params.size() > 1) {
            extra.append(
                HtmlUtils.formEntry(
                    msgLabel("Parameter for Image and Grid"),
                    HtmlUtils.select(
                        RecordOutputHandler.ARG_PARAMETER, params,
                        request.getString(
                            RecordOutputHandler.ARG_PARAMETER,
                            (String) null))));
            extra.append(
                HtmlUtils.formEntry(
                    msgLabel("Divisor"),
                    HtmlUtils.select(
                        RecordOutputHandler.ARG_DIVISOR, params,
                        request.get(
                            RecordOutputHandler.ARG_DIVISOR,
                            new ArrayList<String>()), HtmlUtils.arg(
                                HtmlUtils.ATTR_ROWS, "4") + " multiple ")));
        }

        extra.append(
            HtmlUtils.formEntry(
                msgLabel("Color Table"),
                HtmlUtils.select(
                    ARG_COLORTABLE, ColorTable.getColorTableNames(),
                    request.getString(ARG_COLORTABLE, (String) null))));

        extra.append(
            HtmlUtils.formEntry(
                msgLabel("Color Range"),
                HtmlUtils.input(
                    RecordConstants.ARG_GRID_RANGE_MIN,
                    request.getString(
                        RecordConstants.ARG_GRID_RANGE_MIN, ""), 5) + " -- "
                            + HtmlUtils.input(
                                RecordConstants.ARG_GRID_RANGE_MAX,
                                request.getString(
                                    RecordConstants.ARG_GRID_RANGE_MAX,
                                    ""), 5)));
        extra.append(HtmlUtils.formTableClose());




        StringBuffer points = new StringBuffer();
        points.append(HtmlUtils.formTable());
        points.append(
            HtmlUtils.formEntry(
                "",
                HtmlUtils.checkbox(ARG_GEOREFERENCE, "true", false) + " "
                + msg("Convert coordinates to lat/lon if needed")));

        points.append(
            HtmlUtils.formEntry(
                "",
                HtmlUtils.checkbox(ARG_INCLUDEWAVEFORM, "true", false) + " "
                + msg("Include waveforms")));


        points.append(HtmlUtils.formTableClose());
        StringBuffer processSB = new StringBuffer();
        processSB.append(HtmlUtils.formTable());
        processSB.append(HtmlUtils.formEntry("",
                                             HtmlUtils.checkbox(ARG_ASYNCH,
                                                 "true", true) + " "
                                                     + msg("Asynchronous")));

        processSB.append(HtmlUtils.formEntry(msgLabel("Alternate header"),
                                             HtmlUtils.input(ARG_HEADER, "")
                                             + " "
                                             + "Enter 'none' for no header"));

        processSB.append(
            HtmlUtils.formEntry(
                "",
                HtmlUtils.checkbox(ARG_RESPONSE, RESPONSE_XML, false)
                + " Return response in XML"));

        processSB.append(
            HtmlUtils.formEntry(
                "",
                HtmlUtils.checkbox(ARG_POINTCOUNT, "true", false)
                + " Just return the estimated point count"));



        getOutputHandler().addPublishWidget(
            request, entry, processSB,
            msg("Select a folder to publish the product to"));

        processSB.append(HtmlUtils.formTableClose());


        StringBuilder advancedSB = new StringBuilder();
        if (recordEntry.isCapable(PointFile.ACTION_GRID)) {
            advancedSB.append(HtmlUtils.makeShowHideBlock(msg("Gridding"),
                    extra.toString(), showUrl));
        }
        advancedSB.append(HtmlUtils.makeShowHideBlock(msg("Points"),
                points.toString(), false));
        advancedSB.append(HtmlUtils.makeShowHideBlock(msg("Processing"),
                processSB.toString(), false));


        StringBuilder jobSB = new StringBuilder();
        jobSB.append(HtmlUtils.formTable());
        User user = request.getUser();
        if (getAdmin().isEmailCapable()) {
            jobSB.append(HtmlUtils.formEntry(msgLabel("Send email to"),
                                             HtmlUtils.input(ARG_JOB_EMAIL,
                                                 user.getEmail(), 40)));
        }
        jobSB.append(HtmlUtils.formEntry(msgLabel("Your name"),
                                         HtmlUtils.input(ARG_JOB_USER,
                                             user.getName(), 40)));

        jobSB.append(HtmlUtils.formEntry(msgLabel("Job name"),
                                         HtmlUtils.input(ARG_JOB_NAME, "",
                                             40)));

        jobSB.append(
            HtmlUtils.formEntryTop(
                msgLabel("Description"),
                HtmlUtils.textArea(ARG_JOB_DESCRIPTION, "", 5, 40)));

        jobSB.append(HtmlUtils.formTableClose());

        formGroup(request, sb, "Advanced Settings", advancedSB.toString());
        formGroup(request, sb, "Job Information",
                  HtmlUtils.makeShowHideBlock("", jobSB.toString(), false));
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     * @param title _more_
     * @param contents _more_
     *
     * @throws Exception _more_
     */
    public static void formGroup(Request request, Appendable sb,
                                 String title, String contents)
            throws Exception {
        sb.append(formHeader(title));
        sb.append(HtmlUtils.insetDiv(contents, 0, 20, 10, 0));
    }


    /**
     * add to form
     *
     * @param request request
     * @param entry _more_
     * @param sb buffer
     * @param forGroup for group
     * @param recordEntry the entry
     * @param extraSubset _more_
     *
     * @throws Exception On badness
     */
    public void addSelectForm(Request request, Entry entry, Appendable sb,
                              boolean forGroup, RecordEntry recordEntry,
                              String extraSubset)
            throws Exception {

        long numRecords = forGroup
                          ? 0
                          : recordEntry.getNumRecords();

        List<HtmlUtils.Selector> pointFormats =
            new ArrayList<HtmlUtils.Selector>();
        List<HtmlUtils.Selector> gridFormats =
            new ArrayList<HtmlUtils.Selector>();

        //Do this so we get the LidarOutputHandler in case of a lidar entry
        ((PointEntry) recordEntry).getPointOutputHandler().getPointFormats(
            pointFormats, forGroup);
        getGridFormats(gridFormats, forGroup);
        List<List<HtmlUtils.Selector>> formatLists =
            new ArrayList<List<HtmlUtils.Selector>>();
        formatLists.add(pointFormats);
        formatLists.add(gridFormats);

        StringBuffer productSB = new StringBuffer();
        productSB.append(HtmlUtils.formTable());
        HashSet<String> selectedFormat = getFormats(request);
        StringBuffer formats =
            new StringBuffer(
                "<table border=0 cellpadding=4 cellspacing=0><tr valign=top>");

        int          cnt = 0;
        StringBuffer formatCol;
        StringBuffer gridsCol = new StringBuffer();
        gridsCol.append(HtmlUtils.b(msg("Select Grids")));
        gridsCol.append(HtmlUtils.p());
        for (int i = 0; i < GRID_ARGS.length; i++) {
            String helpImg =
                HtmlUtils.img(getRepository().getIconUrl(ICON_HELP),
                              GRID_HELP[i]);
            gridsCol.append(helpImg);
            gridsCol.append(HtmlUtils.checkbox(GRID_ARGS[i], "true",
                    request.get(GRID_ARGS[i], false)));
            gridsCol.append(msg(GRID_LABELS[i]));
            gridsCol.append(HtmlUtils.p());
        }



        for (int i = 0; i < formatLists.size(); i++) {
            List<HtmlUtils.Selector> formatList = formatLists.get(i);
            formatCol = new StringBuffer();
            if (i == 0) {
                formatCol.append(HtmlUtils.b(msg("Point Products")));
            } else {
                if ( !recordEntry.isCapable(PointFile.ACTION_GRID)) {
                    continue;
                }
                formats.append(HtmlUtils.col(HtmlUtils.space(5)));
                formats.append(
                    HtmlUtils.col(
                        HtmlUtils.img(
                            getRepository().getFileUrl(
                                "/icons/blank.gif")), HtmlUtils.style(
                                    "border-left:1px #000000 solid")));
                formats.append(HtmlUtils.col(HtmlUtils.space(4)));
                formats.append(HtmlUtils.col(gridsCol.toString()));
                String middle =
                    "<br><br><br>&nbsp;&nbsp;&nbsp;to make&nbsp;&nbsp;&nbsp;<br>"
                    + HtmlUtils.img(
                        getRepository().getFileUrl("/point/rightarrow.jpg"));
                formats.append(
                    HtmlUtils.col(
                        middle,
                        HtmlUtils.attr(HtmlUtils.ATTR_ALIGN, "center")));
                formatCol.append(HtmlUtils.b(msg("Select Products")));
            }
            formatCol.append(HtmlUtils.p());

            for (HtmlUtils.Selector selector : formatList) {
                formatCol.append(HtmlUtils.checkbox(ARG_PRODUCT,
                        selector.getId(),
                        selectedFormat.contains(selector.getId())));
                formatCol.append(" ");
                if (selector.getIcon() != null) {
                    formatCol.append(HtmlUtils.img(selector.getIcon()));
                    formatCol.append(" ");
                }
                formatCol.append(selector.getLabel());
                formatCol.append(HtmlUtils.p());
            }
            formats.append(HtmlUtils.col(formatCol.toString()));
        }
        formats.append("</tr></table>");
        productSB.append(HtmlUtils.row(HtmlUtils.colspan(formats.toString(),
                2)));
        productSB.append(HtmlUtils.formTableClose());

        StringBuffer subsetSB = new StringBuffer();
        subsetSB.append(HtmlUtils.formTable());
        if (numRecords > 0) {
            subsetSB.append(HtmlUtils.formEntry("# " + msgLabel("Points"),
                    formatPointCount(numRecords)));
        }

        MapInfo map = getRepository().getMapManager().createMap(request,
                          true, null);
        List<Metadata> metadataList = getMetadataManager().getMetadata(entry);
        boolean didMetadata = map.addSpatialMetadata(entry, metadataList);
        if ( !didMetadata) {
            map.addBox(entry,
                       new MapBoxProperties(MapInfo.DFLT_BOX_COLOR, false,
                                            true));
        } else {
            map.centerOn(entry);
        }
        SessionManager sm = getRepository().getSessionManager();
        String mapSelector =
            map.makeSelector(ARG_AREA, true,
                             new String[] {
                                 request.getStringOrSession(ARG_AREA_NORTH,
                                     getSessionPrefix(), ""),
                                 request.getStringOrSession(ARG_AREA_WEST,
                                     getSessionPrefix(), ""),
                                 request.getStringOrSession(ARG_AREA_SOUTH,
                                     getSessionPrefix(), ""),
                                 request.getStringOrSession(ARG_AREA_EAST,
                                     getSessionPrefix(), ""), }, "", "");

        subsetSB.append(HtmlUtils.formEntryTop(msgLabel("Region"),
                mapSelector));

        if (recordEntry != null) {
            String help = "Probablity a point will be included 0.-1.0";
            String probHelpImg =
                HtmlUtils.img(getRepository().getIconUrl(ICON_HELP), help);
            String prob =
                HtmlUtils.space(3) + msgLabel("Or use probability") + " "
                + HtmlUtils.input(ARG_PROBABILITY,
                                  request.getString(ARG_PROBABILITY, ""),
                                  4) + probHelpImg;
            if (recordEntry.isCapable(PointFile.ACTION_TIME)) {

                boolean showTime = true;
                subsetSB.append(
                    formEntry(
                        request, msgLabel("Date Range"),
                        getPageHandler().makeDateInput(
                            request, ARG_FROMDATE, "entryform", null, null,
                            showTime) + HtmlUtils.space(1)
                                      + HtmlUtils.img(getIconUrl(ICON_RANGE))
                                      + HtmlUtils.space(1)
                                      + getPageHandler().makeDateInput(
                                          request, ARG_TODATE, "entryform",
                                          null, null, showTime)));
            }

            subsetSB.append(HtmlUtils.formEntry(msgLabel("Max"),
                    HtmlUtils.input(ARG_MAX, request.getString(ARG_MAX, ""),
                                    4)));

            if (recordEntry.isCapable(PointFile.ACTION_DECIMATE)) {
                subsetSB.append(HtmlUtils.formEntry(msgLabel("Decimate"),
                        msgLabel("Skip every") + " "
                        + HtmlUtils.input(ARG_RECORD_SKIP,
                                          request.getString(ARG_RECORD_SKIP,
                                              ""), 4) + prob));
            }

            if (recordEntry.isCapable(PointFile.ACTION_TRACKS)) {
                subsetSB.append(
                    HtmlUtils.formEntry(
                        msgLabel("GLAS Tracks"),
                        HtmlUtils.input(
                            ARG_TRACKS, request.getString(ARG_TRACKS, ""),
                            20) + " "
                                + msg("Comma separated list of track numbers")));
            }
        }


        // Look for searchable fields
        List<RecordField> allFields        = null;
        List<RecordField> searchableFields = null;
        if (recordEntry != null) {
            searchableFields =
                recordEntry.getRecordFile().getSearchableFields();
            allFields = recordEntry.getRecordFile().getFields();
        } else if (forGroup
                   && (entry.getTypeHandler()
                       instanceof RecordCollectionTypeHandler)) {
            //Its a Collection
            RecordEntry childEntry =
                ((RecordCollectionTypeHandler) entry.getTypeHandler())
                    .getChildRecordEntry(entry);
            if (childEntry != null) {
                searchableFields =
                    childEntry.getRecordFile().getSearchableFields();
                allFields = childEntry.getRecordFile().getFields();
            }
        }

        if (allFields != null) {
            StringBuffer paramSB = null;
            List<String> selected = (List<String>) request.get(ARG_FIELD_USE,
                                        new ArrayList<String>());
            for (RecordField attr : allFields) {

                //Skip arrays
                if (attr.getArity() > 1) {
                    continue;
                }
                if (paramSB == null) {
                    paramSB = new StringBuffer();
                    paramSB.append(HtmlUtils.formTable());
                }
                String label = attr.getName();
                if (attr.getDescription().length() > 0) {
                    label = label + " - " + attr.getDescription();
                }
                paramSB.append(
                    HtmlUtils.formEntry(
                        "",
                        HtmlUtils.checkbox(
                            ARG_FIELD_USE, attr.getName(),
                            selected.contains(attr.getName())) + " "
                                + label));
            }
            if (paramSB != null) {
                paramSB.append(HtmlUtils.formTableClose());
                subsetSB.append(
                    HtmlUtils.formEntryTop(
                        msgLabel("Select Fields"),
                        HtmlUtils.makeShowHideBlock(
                            msg(""), paramSB.toString(), false)));

            }
        }


        if (searchableFields != null) {
            StringBuffer paramSB = null;
            for (RecordField field : searchableFields) {
                List<String[]> enums        = field.getEnumeratedValues();
                String         searchSuffix = field.getSearchSuffix();
                if (searchSuffix == null) {
                    searchSuffix = "";
                } else {
                    searchSuffix = "  " + searchSuffix;
                }
                if (field.isBitField()) {
                    if (paramSB == null) {
                        paramSB = new StringBuffer();
                        paramSB.append(HtmlUtils.formTable());
                    }
                    String[]     bitFields = field.getBitFields();
                    StringBuffer widgets   = new StringBuffer();
                    paramSB.append(
                        HtmlUtils.row(
                            HtmlUtils.colspan(
                                formHeader(field.getName()), 2)));
                    List values = new ArrayList();
                    values.add(new TwoFacedObject("--", ""));
                    values.add(new TwoFacedObject("true", "true"));
                    values.add(new TwoFacedObject("false", "false"));
                    String urlArgPrefix = ARG_SEARCH_PREFIX + field.getName()
                                          + "_" + ARG_BITFIELD + "_";
                    for (int bitIdx = 0; bitIdx < bitFields.length;
                            bitIdx++) {
                        String bitField = bitFields[bitIdx].trim();
                        if (bitField.length() == 0) {
                            continue;
                        }
                        String value = request.getString(urlArgPrefix
                                           + bitIdx, "");
                        paramSB.append(HtmlUtils.formEntry(bitField + ":",
                                HtmlUtils.select(urlArgPrefix + bitIdx,
                                    values, value, "")));
                    }
                } else if (enums != null) {
                    List values = new ArrayList();
                    values.add(new TwoFacedObject("--", ""));
                    for (String[] tuple : enums) {
                        values.add(new TwoFacedObject(tuple[1], tuple[0]));
                    }
                    String attrWidget = HtmlUtils.select(ARG_SEARCH_PREFIX
                                            + field.getName(), values, "",
                                                "");
                    if (paramSB == null) {
                        paramSB = new StringBuffer();
                        paramSB.append(HtmlUtils.formTable());
                    }
                    paramSB.append(
                        HtmlUtils.formEntry(
                            msgLabel(field.getLabel()),
                            attrWidget + searchSuffix));
                } else {
                    String attrWidget = HtmlUtils.input(
                                            ARG_SEARCH_PREFIX
                                            + field.getName() + "_min", "",
                                                HtmlUtils.SIZE_8) + " - "
                                                    + HtmlUtils.input(
                                                        ARG_SEARCH_PREFIX
                                                        + field.getName()
                                                        + "_max", "",
                                                            HtmlUtils.SIZE_8);
                    if (paramSB == null) {
                        paramSB = new StringBuffer();
                        paramSB.append(HtmlUtils.formTable());
                    }
                    paramSB.append(
                        HtmlUtils.formEntry(
                            msgLabel(field.getLabel() + " range"),
                            attrWidget + searchSuffix));
                }

            }
            if (paramSB != null) {
                paramSB.append(HtmlUtils.formTableClose());
                subsetSB.append(
                    HtmlUtils.formEntryTop(
                        msgLabel("Search Fields"),
                        HtmlUtils.makeShowHideBlock(
                            msg(""), paramSB.toString(), false)));


            }
        }
        subsetSB.append(extraSubset);
        subsetSB.append(HtmlUtils.formTableClose());

        sb.append(
            HtmlUtils.hidden(
                ARG_OUTPUT, getPointOutputHandler().OUTPUT_PRODUCT.getId()));
        formGroup(request, sb, "Subset Data",
                  HtmlUtils.makeShowHideBlock("", subsetSB.toString(),
                      false));
        formGroup(request, sb, "Select Products",
                  HtmlUtils.makeShowHideBlock("", productSB.toString(),
                      true));

    }




    /**
     * create jfree chart
     *
     * @param request the request
     * @param entry The entry
     * @param dataset the dataset
     * @param backgroundImage background image
     *
     * @return the chart
     */
    public static JFreeChart createTimeseriesChart(Request request,
            Entry entry, XYDataset dataset, Image backgroundImage) {
        JFreeChart chart = ChartFactory.createXYLineChart("",  // chart title
            "",                                                // x axis label
            "Height",                                          // y axis label
            dataset,                                           // data
            PlotOrientation.VERTICAL, true,                    // include legend
            true,                                              // tooltips
            false                                              // urls
                );


        DateAxis timeAxis =
            new DateAxis("Time", request.getRepository().TIMEZONE_UTC);
        NumberAxis valueAxis = new NumberAxis("Data");
        valueAxis.setAutoRangeIncludesZero(true);

        XYPlot plot = new XYPlot(dataset, timeAxis, valueAxis, null);


        chart = new JFreeChart("", JFreeChart.DEFAULT_TITLE_FONT, plot, true);


        chart.setBackgroundPaint(Color.white);
        if (backgroundImage != null) {
            plot.setBackgroundImage(backgroundImage);
            plot.setBackgroundImageAlignment(org.jfree.ui.Align.TOP_LEFT);
        }

        plot.setBackgroundPaint(Color.lightGray);
        plot.setDomainGridlinePaint(Color.white);
        plot.setRangeGridlinePaint(Color.white);

        plot.setInsets(new RectangleInsets(0, 0, 0, 0));
        plot.setAxisOffset(new RectangleInsets(5, 0, 5, 0));

        return chart;
    }



    /**
     * Class description
     *
     *
     * @version        $version$, Mon, Aug 29, '11
     * @author         Enter your name here...
     */
    public static class PlotInfo {

        /** _more_ */
        public List<Double> alts;

        /** _more_ */
        public int minX = Integer.MAX_VALUE;

        /** _more_ */
        public int maxX = 0;

        /** _more_ */
        public int minIndex = Integer.MAX_VALUE;

        /** _more_ */
        public int maxIndex = 0;

        /**
         * _more_
         *
         * @param index _more_
         */
        public void setIndex(int index) {
            minIndex = Math.min(minIndex, index);
            maxIndex = Math.max(maxIndex, index);
        }

        /**
         * _more_
         *
         * @param x _more_
         */
        public void setX(int x) {
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
        }


    }




    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Sep 15, '11
     * @author         Enter your name here...
     */
    public static class MyStandardXYItemRenderer extends StandardXYItemRenderer {

        /** _more_ */
        PlotInfo plotInfo;

        /**
         * _more_
         *
         * @param plotInfo _more_
         */
        public MyStandardXYItemRenderer(PlotInfo plotInfo) {
            this.plotInfo = plotInfo;
        }

        /**
         * _more_
         *
         * @param g2 _more_
         * @param state _more_
         * @param dataArea _more_
         * @param info _more_
         * @param plot _more_
         * @param domainAxis _more_
         * @param rangeAxis _more_
         * @param dataset _more_
         * @param series _more_
         * @param item _more_
         * @param crosshairState _more_
         * @param pass _more_
         */
        public void drawItem(java.awt.Graphics2D g2,
                             XYItemRendererState state,
                             java.awt.geom.Rectangle2D dataArea,
                             PlotRenderingInfo info, XYPlot plot,
                             ValueAxis domainAxis, ValueAxis rangeAxis,
                             XYDataset dataset, int series, int item,
                             CrosshairState crosshairState, int pass) {
            super.drawItem(g2, state, dataArea, info, plot, domainAxis,
                           rangeAxis, dataset, series, item, crosshairState,
                           pass);
            RectangleEdge domainEdge = plot.getDomainAxisEdge();
            int x = (int) domainAxis.valueToJava2D(item, dataArea,
                        domainEdge);
            plotInfo.setX(x);
            drawCnt[0]++;
            if (debugChart && (drawCnt[0] % 10) == 0) {
                int index = xindices.get(item);
                g2.setColor(Color.red);
                g2.drawLine(x, 15, x, 500);
                g2.drawString("" + index, x, 20);
                //                g2.drawString("" + plotInfo.alts.get(item), x, 50);
                //                System.out.println("chart: " + item +" " + index +" " +plotInfo.alts.get(item));
            }
        }
    }

    ;





    /**
     * _more_
     *
     * @param request _more_
     * @param outputType _more_
     * @param pointEntry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputEntryChartOldWay(Request request,
                                         OutputType outputType,
                                         PointEntry pointEntry)
            throws Exception {

        long         numRecords = pointEntry.getNumRecords();
        Entry        entry      = pointEntry.getEntry();
        int numPointsToPlot = request.get(ARG_NUMPOINTS, TIMESERIES_POINTS);

        StringBuffer sb         = new StringBuffer();
        final List<RecordField> fields =
            pointEntry.getRecordFile().getChartableFields();

        final StringBuffer js = new StringBuffer();
        sb.append(HtmlUtils.script("var pointDataDomainBase = \""
                                   + getOutputHandler().getDomainBase()
                                   + "\";\n"));

        StringBuffer mapSB = new StringBuffer();
        boolean showMap = pointEntry.isCapable(PointFile.ACTION_MAPINCHART)
                          && request.get(ARG_MAP_SHOW, true);
        MapInfo map = getRepository().getMapManager().createMap(request, 500,
                          300, false, null);
        if (showMap) {
            makeMapLines(request, pointEntry, map, 0);
            map.centerOn(entry);
            mapSB.append(map.getHtml());
        }

        mapSB.append(HtmlUtils.div("",
                HtmlUtils.style("border-left : 2px red solid; position: absolute;  visibility: hidden; z-index: 1;")
                + HtmlUtils.cssClass("point_timeseries_div")
                + HtmlUtils.id("point_timeseries_div")));

        boolean hasWaveform = pointEntry.isCapable(PointFile.ACTION_WAVEFORM);
        final String waveformName = request.getString(ARG_WAVEFORM_NAME, "");
        String waveformDisplay = request.getString(ARG_WAVEFORM_DISPLAY,
                                     "normal");
        StringBuffer timeSeriesImageUrl = new StringBuffer(( !hasWaveform
                ? request.entryUrl(getRepository().URL_ENTRY_SHOW, entry,
                                   new String[] { ARG_NUMPOINTS,
                "" + numPointsToPlot, ARG_OUTPUT,
                getPointOutputHandler().OUTPUT_TIMESERIES_IMAGE.toString() })
                : request.entryUrl(getRepository().URL_ENTRY_SHOW, entry,
                                   new String[] {
ARG_NUMPOINTS, "" + numPointsToPlot, ARG_OUTPUT, getPointOutputHandler().OUTPUT_TIMESERIES_IMAGE.toString(),
ARG_WAVEFORM_DISPLAY, waveformDisplay, ARG_WAVEFORM_NAME, waveformName
})));


        String formUrl = request.entryUrl(getRepository().URL_ENTRY_SHOW,
                                          pointEntry.getEntry(),
                                          new String[] {});

        StringBuffer formSB = new StringBuffer();
        formSB.append(HtmlUtils.formTable());
        formSB.append(HtmlUtils.form(formUrl));
        formSB.append(
            HtmlUtils.hidden(
                ARG_OUTPUT, getPointOutputHandler().OUTPUT_CHART.toString()));
        formSB.append(HtmlUtils.hidden(ARG_ENTRYID,
                                       pointEntry.getEntry().getId()));


        String mapShow = HtmlUtils.radio(ARG_MAP_SHOW, "true", showMap) + " "
                         + msg("Show") + "  "
                         + HtmlUtils.radio(ARG_MAP_SHOW, "false", !showMap)
                         + " " + msg("Hide");
        formSB.append(HtmlUtils.formEntry(msgLabel("Map"), mapShow));

        if (fields.size() > 0) {
            StringBuffer attrShow = new StringBuffer();
            attrShow.append("<table>");

            List<String[]> names = new ArrayList<String[]>();
            if (pointEntry.isArealCoverage()) {
                names.add(new String[] { FIELD_ALTITUDE, LABEL_ALTITUDE,
                                         "true" });
            }

            for (RecordField attr : fields) {
                names.add(new String[] { attr.getName(), attr.getLabel(),
                                         "false" });
            }
            attrShow.append(HtmlUtils.row(HtmlUtils.cols(new Object[] {
                HtmlUtils.b(msg("Field")),
                HtmlUtils.b(msg("Show")), HtmlUtils.b(msg("Hide")),
                HtmlUtils.b(msg("Color")) })));


            for (String[] tuple : names) {
                String  name        = tuple[0];
                String  arg         = ARG_CHART_SHOW + name;
                String  carg        = ARG_CHART_COLOR + name;
                boolean value = request.get(arg, tuple[2].equals("true"));
                String  color       = request.getString(carg, "");
                String  colorSelect = HtmlUtils.colorSelect(carg, color);


                if (value) {
                    timeSeriesImageUrl.append("&" + arg + "=" + value);
                    timeSeriesImageUrl.append("&" + carg + "="
                            + HtmlUtils.urlEncode(color));
                }

                attrShow.append(HtmlUtils.row(HtmlUtils.cols(new Object[] {
                    tuple[1],
                    HtmlUtils.radio(arg, "true", value),
                    HtmlUtils.radio(arg, "false", !value),
                    colorSelect, })));
            }
            attrShow.append("</table>");
            formSB.append(HtmlUtils.formEntryTop(msgLabel("Chart"),
                    attrShow.toString()));
        }

        if (hasWaveform) {
            String[] waveformNames =
                pointEntry.getPointFile().getWaveformNames();
            List waveformList = new ArrayList();
            waveformList.add(new TwoFacedObject(msg("Normal"), "normal"));
            waveformList.add(new TwoFacedObject(msg("Exaggerrated"),
                    "exaggerrated"));
            waveformList.add(new TwoFacedObject(msg("Hidden"), "none"));

            if (waveformNames.length > 1) {
                String waveFormNamesSelect =
                    HtmlUtils.select(ARG_WAVEFORM_NAME, waveformNames,
                                     request.getString(ARG_WAVEFORM_NAME,
                                         (String) null), Integer.MAX_VALUE);
                formSB.append(HtmlUtils.formEntry(msgLabel("Waveform"),
                        waveFormNamesSelect));
            }

            String waveFormSelect =
                HtmlUtils.select(ARG_WAVEFORM_DISPLAY, waveformList,
                                 request.getString(ARG_WAVEFORM_DISPLAY,
                                     (String) null));

            formSB.append(
                HtmlUtils.formEntry(
                    msgLabel("Waveform Background"), waveFormSelect));
        }
        formSB.append(HtmlUtils.formEntry(msgLabel("Num Points"),
                                          HtmlUtils.input(ARG_NUMPOINTS,
                                              "" + numPointsToPlot,
                                                  HtmlUtils.SIZE_10)));

        formSB.append(
            HtmlUtils.formEntry("", HtmlUtils.submit(msg("Change Display"))));

        formSB.append(HtmlUtils.formClose());
        formSB.append(HtmlUtils.formTableClose());
        PlotInfo plotInfo = new PlotInfo();
        //For now just make the image (though we just throw it out) so
        //we can retrieve the plot spacing info
        BufferedImage newImage = makeTimeseriesImage(request, pointEntry,
                                     plotInfo);


        String mapVarName  = (showMap
                              ? map.getVariableName()
                              : "null");




        String clickParams = HtmlUtils.comma(new String[] {
            "this", "event", mapVarName, HtmlUtils.squote(entry.getId()),
            HtmlUtils.squote("point_timeseries_image"),
            HtmlUtils.squote("point_timeseries_div"), plotInfo.minIndex + "",
            plotInfo.maxIndex + "", plotInfo.minX + "", plotInfo.maxX + "",
            HtmlUtils.squote(waveformName)
        });


        //        System.err.println(clickParams);
        //        String call = HtmlUtils.onMouseClick(HtmlUtils.call("timeSeriesClick",
        //                          clickParams));

        String call = HtmlUtils.call("timeSeriesClick", clickParams);
        //        System.err.println (call);
        String timeseriesImage = HtmlUtils.img(timeSeriesImageUrl.toString(),
                                     "",
                                     HtmlUtils.id("point_timeseries_image")
        /*+ call*/
        );

        String message2 =
            HtmlUtils.italics(msg("Note: positions are approximate"));
        if (hasWaveform) {
            String message = HtmlUtils.italics(msg(showMap
                    ? "Click to show location and waveform"
                    : "Click to show waveform"));

            String waveformUrl =
                request.entryUrl(getRepository().URL_ENTRY_SHOW, entry,
                                 new String[] { ARG_OUTPUT,
                    getPointOutputHandler().OUTPUT_WAVEFORM_IMAGE.toString(),
                    ARG_WAVEFORM_NAME, waveformName });

            String waveformCsvUrl =
                request.entryUrl(getRepository().URL_ENTRY_SHOW, entry,
                                 new String[] { ARG_OUTPUT,
                    getPointOutputHandler().OUTPUT_WAVEFORM_CSV.toString(),
                    ARG_WAVEFORM_NAME, waveformName });


            String waveformImage = HtmlUtils.img(waveformUrl, "",
                                       HtmlUtils.id("point_waveform_image"));

            String waveformCsvHref = HtmlUtils.href(waveformCsvUrl, "csv",
                                         HtmlUtils.id("point_waveform_csv"));
            //            System.err.println(waveformCsvUrl);
            sb.append("<table><tr align=top><td align=center colspan=2>"
                      + mapSB.toString() + "</td></tr><tr align=top><td>"
                      + timeseriesImage + HtmlUtils.br()
                      + HtmlUtils.leftRight(message, message2) + "</td><td>"
                      + waveformImage + HtmlUtils.br() + waveformCsvHref
                      + "<tr><td></td><td align=center><b>" + waveformName
                      + "</b></td></tr>" + "</table>");
        } else {
            String message = showMap
                             ? HtmlUtils.italics(
                                 msg(
                                 "Click to show location")) + HtmlUtils.br()
                             : "";
            sb.append(mapSB);
            sb.append(timeseriesImage);
            sb.append(HtmlUtils.br());
            sb.append(HtmlUtils.leftRight(message, ""));
        }

        sb.append("\n\n");
        sb.append(
            HtmlUtils.script(
                "jQuery(document).ready(function(){\njQuery('#point_timeseries_image').click(function(event){\n"
                + call + "});})\n"));
        sb.append("\n\n");

        String extra = HtmlUtils.makeShowHideBlock("Settings",
                           formSB.toString(), false);
        sb.append(extra);
        sb.append("\n");

        sb.append("\n");





        Result result = new Result("", sb);


        return result;

    }

    /**
     * handle the time series request
     *
     * @param request the request
     * @param outputType type
     * @param pointEntry The entry
     *
     * @return ramadda result
     *
     * @throws Exception on badness
     */
    public Result outputEntryTimeSeriesImage(Request request,
                                             OutputType outputType,
                                             PointEntry pointEntry)
            throws Exception {
        File imageFile =
            getRepository().getStorageManager().getTmpFile(request,
                "timeseries.png");
        PlotInfo plotInfo = new PlotInfo();
        BufferedImage newImage = makeTimeseriesImage(request, pointEntry,
                                     plotInfo);
        ImageUtils.writeImageToFile(newImage, imageFile);
        InputStream is     =
            getStorageManager().getFileInputStream(imageFile);
        Result      result = new Result("", is, "image/png");

        return result;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param pointEntry _more_
     * @param plotInfo _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public BufferedImage makeTimeseriesImage(Request request,
                                             PointEntry pointEntry,
                                             final PlotInfo plotInfo)
            throws Exception {

        Entry entry      = pointEntry.getEntry();
        int   width      = TIMESERIES_WIDTH;
        int   height     = TIMESERIES_HEIGHT;
        long  numRecords = pointEntry.getNumRecords();
        final int numPointsToPlot = request.get(ARG_NUMPOINTS,
                                        TIMESERIES_POINTS);
        final int[]            cnt    = { 0 };
        final List<TimeSeries> series = new ArrayList<TimeSeries>();


        final List<RecordField> tmpFields =
            pointEntry.getRecordFile().getChartableFields();

        final List<RecordField> fields = new ArrayList<RecordField>();

        if (request.get(ARG_CHART_SHOW + FIELD_ALTITUDE, false)) {
            fields.add(new RecordField(FIELD_ALTITUDE, LABEL_ALTITUDE, "",
                                       -1, UNIT_M));
        }

        for (RecordField attr : tmpFields) {
            if (request.get(ARG_CHART_SHOW + attr.getName(), false)) {
                fields.add(attr);
            }
        }

        if ((fields.size() == 0) && (tmpFields.size() > 0)) {
            fields.add(tmpFields.get(0));
            request.put(ARG_CHART_SHOW + tmpFields.get(0).getName(), "true");
        }

        for (RecordField attr : fields) {
            series.add(new TimeSeries(attr.getLabel()));
        }

        RecordVisitor visitor = new BridgeRecordVisitor(getOutputHandler()) {
            public boolean doVisitRecord(RecordFile file,
                                         VisitInfo visitInfo, Record record) {
                PointRecord pointRecord = (PointRecord) record;
                for (int fieldCnt = 0; fieldCnt < fields.size(); fieldCnt++) {
                    RecordField field = fields.get(fieldCnt);
                    double      value;
                    //Check for altitude
                    if (field.getParamId() < 0) {
                        value = pointRecord.getAltitude();
                    } else {
                        value = record.getValue(field.getParamId());
                    }
                    long time = record.getRecordTime();
                    series.get(fieldCnt).add(new FixedMillisecond(time),
                               value);
                }
                plotInfo.setIndex(pointRecord.index);
                cnt[0]++;

                return true;
            }
        };





        long t1   = System.currentTimeMillis();
        int  skip = (int) (numRecords / numPointsToPlot);
        getRecordJobManager().visitSequential(request, pointEntry, visitor,
                new VisitInfo(skip));
        long t2 = System.currentTimeMillis();


        JFreeChart chart = createTimeseriesChart(request, entry,
                               new TimeSeriesCollection(), null);
        long   t3                  = System.currentTimeMillis();
        XYPlot plot                = (XYPlot) chart.getPlot();
        int    lineCnt             = 0;
        int[]  colorCnt            = { 0 };
        int    numberOfAxisLegends = 0;

        Hashtable<String, double[]> valueRanges = new Hashtable<String,
                                                      double[]>();

        for (int extraCnt = 0; extraCnt < series.size(); extraCnt++) {
            TimeSeries  timeSeries = series.get(extraCnt);
            RecordField field      = fields.get(extraCnt);
            String      unit       = field.getUnit();
            if ((unit != null) && (unit.length() == 0)) {
                unit = null;
            }

            if (unit == null) {
                unit = extraCnt + "";
            }
            if (unit == null) {
                continue;
            }
            double   max   = timeSeries.getMaxY();
            double   min   = timeSeries.getMinY();
            double[] range = valueRanges.get(unit);
            if (range == null) {
                range = new double[] { min, max };
                valueRanges.put(unit, range);
            } else {
                range[0] = Math.min(range[0], min);
                range[1] = Math.max(range[1], max);
            }
        }


        Hashtable<String, NumberAxis> seenAxis = new Hashtable<String,
                                                     NumberAxis>();
        for (int extraCnt = 0; extraCnt < series.size(); extraCnt++) {
            TimeSeries  timeSeries = series.get(extraCnt);
            RecordField field      = fields.get(extraCnt);

            String      unit       = field.getUnit();
            if ((unit != null) && (unit.length() == 0)) {
                unit = null;
            }

            TimeSeriesCollection dataset2 = new TimeSeriesCollection();
            dataset2.addSeries(timeSeries);
            NumberAxis axis = new NumberAxis(field.getLabel());
            numberOfAxisLegends++;
            if (unit != null) {
                double[] range = valueRanges.get(unit);
                axis.setRange(range[0], range[1]);
                NumberAxis seenOne = seenAxis.get(unit);
                if (seenOne == null) {
                    seenAxis.put(unit, axis);
                } else {
                    seenOne.setLabel(seenOne.getLabel() + "/"
                                     + field.getLabel());
                    axis.setVisible(false);
                    numberOfAxisLegends--;
                }
            } else {
                axis.setAutoRange(true);
                axis.setAutoRangeIncludesZero(true);
            }

            plot.setRangeAxis(lineCnt, axis);
            plot.setDataset(lineCnt, dataset2);
            plot.mapDatasetToRangeAxis(lineCnt, lineCnt);
            plot.setRangeAxisLocation(lineCnt, AxisLocation.BOTTOM_OR_RIGHT);

            StandardXYItemRenderer renderer =
                new MyStandardXYItemRenderer(plotInfo);
            renderer.setSeriesPaint(0, getColor(request,
                    ARG_CHART_COLOR + field.getName(), colorCnt));
            plot.setRenderer(lineCnt, renderer);
            lineCnt++;
        }

        AxisSpace axisSpace = new AxisSpace();
        axisSpace.setRight(TIMESERIES_AXIS_WIDTHPER * numberOfAxisLegends);
        plot.setFixedRangeAxisSpace(axisSpace);

        long t4 = System.currentTimeMillis();
        BufferedImage newImage = chart.createBufferedImage(width
                                     + (numberOfAxisLegends
                                        * TIMESERIES_AXIS_WIDTHPER), height);

        long t5 = System.currentTimeMillis();

        //        System.err.println("Time series  cnt:" + cnt[0] + " " + (t2 - t1) + " "  + (t3 - t2) + " " + (t4 - t3) + " " + (t5 - t4));
        return newImage;
    }




    /*
      waveform stuff
        final String waveformDisplay =
            request.getString(ARG_WAVEFORM_DISPLAY, "normal");

        if (hasWaveform && !waveformDisplay.equals("none")) {
            XYSeriesCollection waveformDataset = new XYSeriesCollection();
            waveformDataset.addSeries(waveformSeries);
            double[]   range        = valueRanges.get(UNIT_M);
            NumberAxis seenOne      = seenAxis.get(UNIT_M);
            NumberAxis waveformAxis = new NumberAxis("Waveform");
            if (range != null) {
                waveformAxis.setRange(range[0], range[1]);
                waveformAxis.setAutoRange(false);
                seenOne.setLabel(seenOne.getLabel() + "/" + "Waveform");
                waveformAxis.setVisible(false);
            } else {
                waveformAxis.setAutoRange(true);
                waveformAxis.setAutoRangeIncludesZero(false);
                numberOfAxisLegends++;
            }

            plot.setRangeAxisLocation(lineCnt, AxisLocation.BOTTOM_OR_RIGHT);
            plot.setRangeAxis(lineCnt, waveformAxis);
            plot.setDataset(lineCnt, waveformDataset);
            plot.mapDatasetToRangeAxis(lineCnt, lineCnt);

            final boolean normalDisplay = waveformDisplay.equals("normal");
            //final int[]   drawCnt       = { 0 };
            drawCnt = new int[] { 0 };

            plot.setRenderer(lineCnt, new AbstractXYItemRenderer() {
                public void drawItem(java.awt.Graphics2D g2,
                                     XYItemRendererState state,
                                     java.awt.geom.Rectangle2D dataArea,
                                     PlotRenderingInfo info, XYPlot plot,
                                     ValueAxis domainAxis,
                                     ValueAxis rangeAxis, XYDataset dataset,
                                     int series, int item,
                                     CrosshairState crosshairState,
                                     int pass) {
                    int           index          = indices.get(item);
                    Waveform      waveform       = waveforms.get(item);
                    float[]       range          = waveform.getRange();
                    float[]       waveformValues = waveform.getWaveform();
                    RectangleEdge rangeEdge      = plot.getRangeAxisEdge();
                    RectangleEdge domainEdge     = plot.getDomainAxisEdge();
                    int x = (int) domainAxis.valueToJava2D(item, dataArea,
                                domainEdge);
                    plotInfo.setX(x);
                    plotInfo.setIndex(item);
                    int x2 = (int) domainAxis.valueToJava2D(item + 1,
                                 dataArea, domainEdge);
                    double heightDiff = waveform.getAltitudeN()
                                        - waveform.getAltitude0();
                    float waveformThreshold = waveform.getThreshold();
                    g2.setStroke(new BasicStroke(1.0f));
                    int barWidth = (x2 - x) / 2;
                    for (int byteIdx = 0; byteIdx < waveformValues.length;
                            byteIdx++) {
                        int   barWidthToUse = barWidth;
                        float b             = waveformValues[byteIdx];
                        if (b <= waveform.getThreshold()) {
                            barWidthToUse = 1;

                            continue;
                        }
                        float colorPercent = (b - range[0])
                                             / (range[1] - range[0]);
                        colorPercent = (float) Math.min(colorPercent, 1.0f);
                        int colorValue = 255 - (int) (255.0 * colorPercent);
                        g2.setColor(new Color(colorValue, colorValue,
                                colorValue));
                        double percent = byteIdx
                                         / (double) waveformValues.length;
                        double altitude = waveform.getAltitude0()
                                          + (heightDiff * percent);
                        int y;
                        if (normalDisplay) {
                            y = (int) rangeAxis.valueToJava2D(altitude,
                                    dataArea, rangeEdge);
                        } else {
                            y = byteIdx;
                        }
                        //                        if(y>40)
                        System.err.println("draw:" + x + " " + barWidthToUse
                                           + " " + y);
                        g2.drawLine(x - barWidthToUse, y, x + barWidthToUse,
                                    y);
                    }
                }
            }, false);
            lineCnt++;
        }
    */



    /**
     * make the waveform image
     *
     * @param request the request
     * @param outputType output type
     * @param pointEntry The entry
     *
     * @return ramadda result
     *
     * @throws Exception on badness
     */
    public Result outputEntryWaveform(Request request, OutputType outputType,
                                      PointEntry pointEntry)
            throws Exception {
        Entry        entry = pointEntry.getEntry();
        StringBuffer sb    = new StringBuffer();
        StringBuffer js    = new StringBuffer();
        sb.append(js);
        String url =
            request.entryUrl(
                getRepository().URL_ENTRY_SHOW, entry, ARG_OUTPUT,
                getPointOutputHandler().OUTPUT_WAVEFORM_IMAGE.toString());
        sb.append(HtmlUtils.img(url));

        return new Result("", sb);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param outputType _more_
     * @param pointEntry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputEntryWaveformImage(Request request,
                                           OutputType outputType,
                                           PointEntry pointEntry)
            throws Exception {

        int  width      = 300;
        int  height     = TIMESERIES_HEIGHT + 10;
        int  pointIndex = request.get(ARG_POINTINDEX, 0);


        long numRecords = pointEntry.getNumRecords();
        if (pointIndex >= numRecords) {
            pointIndex = (int) (numRecords - 1);
        }
        PointRecord record =
            (PointRecord) pointEntry.getRecordFile().getRecord(pointIndex);
        String   waveformName = request.getString(ARG_WAVEFORM_NAME, "");
        Waveform waveform     = record.getWaveform(waveformName);
        boolean  hasAltitude  = waveform.hasAltitude();
        float[]  values       = waveform.getWaveform();
        double heightDiff = waveform.getAltitudeN() - waveform.getAltitude0();
        XYSeries series       = new XYSeries("");
        //        System.err.println("idx: " + pointIndex + " alt:"
        //                           + waveform.getAltitude0() + " -- "
        //                           + waveform.getAltitudeN());
        //        record.print();
        for (int i = 0; i < values.length; i++) {
            double percent = i / (double) values.length;
            float  b       = values[i];
            if (hasAltitude) {
                double altitude = waveform.getAltitude0()
                                  + (heightDiff * percent);
                //                System.err.println(altitude +" " + b);
                series.add(altitude, b);
            } else {
                series.add(i, b);
            }
        }

        File f = getRepository().getStorageManager().getTmpFile(request,
                     "waveform.png");
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(series);
        JFreeChart    chart = createWaveformChart(request, waveform, dataset);
        BufferedImage newImage = chart.createBufferedImage(width, height);
        ImageUtils.writeImageToFile(newImage, f);
        InputStream is = getStorageManager().getFileInputStream(f);

        return new Result("", is, "image/png");
    }

    /**
     * make the waveform image
     *
     * @param request the request
     * @param outputType output type
     * @param pointEntry The entry
     *
     * @return ramadda result
     *
     * @throws Exception on badness
     */
    public Result outputEntryWaveformCsv(Request request,
                                         OutputType outputType,
                                         PointEntry pointEntry)
            throws Exception {

        int  width      = 300;
        int  height     = TIMESERIES_HEIGHT + 10;
        int  pointIndex = request.get(ARG_POINTINDEX, 0);
        long numRecords = pointEntry.getNumRecords();
        if (pointIndex >= numRecords) {
            pointIndex = (int) (numRecords - 1);
        }
        PointRecord record =
            (PointRecord) pointEntry.getRecordFile().getRecord(pointIndex);
        String       waveformName = request.getString(ARG_WAVEFORM_NAME, "");
        Waveform     waveform     = record.getWaveform(waveformName);
        double heightDiff = waveform.getAltitudeN() - waveform.getAltitude0();
        boolean      hasAltitude  = waveform.hasAltitude();
        float[]      values       = waveform.getWaveform();
        StringBuffer sb           = new StringBuffer();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(values[i]);
        }
        sb.append("\n");
        if (hasAltitude) {
            for (int i = 0; i < values.length; i++) {
                if (i > 0) {
                    sb.append(",");
                }
                double percent = i / (double) values.length;
                double altitude = waveform.getAltitude0()
                                  + (heightDiff * percent);
                sb.append(altitude);
            }
            sb.append("\n");
        }
        Result result = new Result("", sb, "text/csv");
        result.setReturnFilename("waveform" + pointIndex + ".csv");

        return result;
    }


    /**
     * make the jfreechart for the waveform
     *
     * @param request the request
     * @param waveform the waveform
     * @param dataset the dataset
     *
     * @return chart
     */
    private static JFreeChart createWaveformChart(Request request,
            Waveform waveform, XYDataset dataset) {
        JFreeChart chart = ChartFactory.createXYLineChart("",  // chart title
            "",                                                // x axis label
            "",                                                // y axis label
            dataset,                                           // data
            PlotOrientation.HORIZONTAL, false,                 // include legend
            false,                                             // tooltips
            false                                              // urls
                );


        chart.setBackgroundPaint(Color.white);
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.lightGray);
        //        chart.getLegend().setVisible(false);

        plot.setDomainGridlinePaint(Color.white);
        plot.setRangeGridlinePaint(Color.white);

        NumberAxis rangeAxis  = (NumberAxis) plot.getRangeAxis();
        NumberAxis domainAxis = (NumberAxis) plot.getDomainAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        rangeAxis.setAutoRange(false);
        float[] range = waveform.getRange();
        rangeAxis.setRange(new org.jfree.data.Range(range[0], range[1]));

        if (request.defined(ARG_WAVEFORM_AXIS_HIGH)) {
            domainAxis.setRange(
                new org.jfree.data.Range(
                    request.get(ARG_WAVEFORM_AXIS_LOW, 0.0),
                    request.get(ARG_WAVEFORM_AXIS_HIGH, 1000.0)));
        }

        domainAxis.setAutoRange(true);
        domainAxis.setAutoRangeIncludesZero(false);

        //        domainAxis.setVisible(false);
        //        rangeAxis.setFixedDimension(20);
        //        plot.setInsets(new RectangleInsets(0, 0, 0, 0));
        //        plot.setAxisOffset(new RectangleInsets(5, 0, 5, 0));
        return chart;
    }


}
