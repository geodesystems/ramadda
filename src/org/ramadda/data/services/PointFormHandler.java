/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.data.services;


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
import org.ramadda.util.Utils;
import org.ramadda.util.grid.*;

import ucar.unidata.geoloc.Bearing;
import ucar.unidata.geoloc.LatLonPointImpl;



import ucar.unidata.ui.ImageUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
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
@SuppressWarnings("unchecked")
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
        outputs.add(getSelect(getPointOutputHandler().OUTPUT_HILLSHADE));
        outputs.add(getSelect(getPointOutputHandler().OUTPUT_IMAGE));
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
                                             BaseRecord record) {
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

            try {
                getRecordJobManager().visitSequential(request, recordEntry,
                        visitor,
                        new VisitInfo(VisitInfo.QUICKSCAN_YES, skipFactor));
            } catch (Throwable thr) {
                Throwable inner = LogUtil.getInnerException(thr);
                if (inner instanceof Exception) {
                    throw (Exception) inner;
                }

                throw new RuntimeException(inner);
            }
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
                                  List<Entry> children)
            throws Exception {
        return outputGroupForm(request, group, children,
                               new StringBuffer());
    }


    /**
     * Show the products form
     *
     * @param request the request
     * @param group the group
     * @param msgSB _more_
     *
     * @return the ramadda result
     *
     * @throws Exception on badness
     */
    public Result outputGroupForm(Request request, Entry group,
                                  List<Entry> children,
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
            getOutputHandler().makeRecordEntries(request, children, false);

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

    public Result outputEntryFormCsv(Request request, Entry entry)
            throws Exception {
        return outputEntryFormCsv(request, entry, new StringBuffer());
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
        addToEntryForm(request, entry, sb, recordEntry,false);
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


    public Result outputEntryFormCsv(Request request, Entry entry,
				     Appendable msgSB)
            throws Exception {

        StringBuilder sb = new StringBuilder();
        request.getRepository().getPageHandler().entrySectionOpen(request,
                entry, sb, "CSV Download");
        sb.append(msgSB);

        RecordEntry recordEntry =
            getPointOutputHandler().doMakeEntry(request, entry);
        String formId = HtmlUtils.getUniqueId("form_");
        sb.append(request.formPost(getRepository().URL_ENTRY_SHOW,
                                   HtmlUtils.id(formId)));
        sb.append(HtmlUtils.hidden(ARG_ENTRYID, entry.getId()));
        sb.append(
            HtmlUtils.hidden(
                ARG_OUTPUT, getPointOutputHandler().OUTPUT_PRODUCT.getId()));
	sb.append(HtmlUtils.hidden(ARG_PRODUCT,"points.csv"));
	sb.append(HtmlUtils.hidden(ARG_ASYNCH,"false"));
        addToEntryForm(request, entry, sb, recordEntry,true);
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
                               RecordEntry recordEntry,boolean csvForm)
            throws Exception {

        sb.append(HtmlUtils.submit(msg("Get Data"), ARG_GETDATA));
        sb.append("<p>");
        if (entry.getTypeHandler() instanceof RecordTypeHandler) {
            ((RecordTypeHandler) entry.getTypeHandler()).addToProcessingForm(
                request, entry, sb);
        }


	addSubsetForm(request, entry, sb, false, recordEntry, "",csvForm);
	if(!csvForm) {
	    addGriddingForm(request, entry, sb, recordEntry);
	    addSelectForm(request, entry, sb, false, recordEntry);
	    addSettingsForm(request, entry, sb, recordEntry);
	}

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
        addSelectForm(request, group, sb, true, recordEntries.get(0));
        addGriddingForm(request, group, sb, recordEntries.get(0));
        addSubsetForm(request, group, sb, true, recordEntries.get(0), extra,false);
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
    public void addGriddingForm(Request request, Entry entry, Appendable sb,
                                RecordEntry recordEntry)
            throws Exception {

        //        if (!recordEntry.isCapable(PointFile.ACTION_GRID))  return;
        boolean       showUrl  = request.get(ARG_SHOWURL, false);
        StringBuilder gridding = new StringBuilder();
        gridding.append(HtmlUtils.formTable());

        List params = new ArrayList();
        //TODO: we need a better way to say this is a elevation point cloud
        //        if(pointEntry.isCapable(PointFile.ACTION_ELEVATION)) {
        params.add(new TwoFacedObject(msg(LABEL_ALTITUDE), "_altitude_"));
        params.add(new TwoFacedObject("Point Count", "_pointcount_"));
        //        }
        if (recordEntry != null) {
            for (RecordField field :
                    recordEntry.getRecordFile().getChartableFields()) {
                params.add(new TwoFacedObject(field.getLabel(),
                        "" + field.getParamId()));
            }
        }


        if (params.size() > 1) {
            String selectedParam =
                request.getString(RecordOutputHandler.ARG_PARAMETER,
                                  "_altitude_");
            gridding.append(
                HtmlUtils.formEntry(
                    msgLabel("Parameter to grid"),
                    HtmlUtils.select(
                        RecordOutputHandler.ARG_PARAMETER, params,
                        selectedParam)));
            gridding.append(
                HtmlUtils.formEntry(
                    msgLabel("Divisor"),
                    HtmlUtils.select(
                        RecordOutputHandler.ARG_DIVISOR, params,
                        request.get(
                            RecordOutputHandler.ARG_DIVISOR,
                            new ArrayList<String>()), HtmlUtils.arg(
                                HtmlUtils.ATTR_ROWS, "4") + " multiple ")));
        }



        gridding.append(
            HtmlUtils.formEntry(
                msgLabel("Minimum # of Points"),
                HtmlUtils.input(
                    RecordConstants.ARG_GRID_MINPOINTS,
                    request.getString(
                        RecordConstants.ARG_GRID_MINPOINTS, "1"), 5)));

        /*
        gridding.append(
            HtmlUtils.formEntry(
                msgLabel("Fill missing"),
                HtmlUtils.checkbox(
                    PointOutputHandler.ARG_FILLMISSING, "true",
                    request.get(PointOutputHandler.ARG_FILLMISSING, false))));
        */

        gridding.append(
            HtmlUtils.formEntry(
                msgLabel("Threshold"),
                HtmlUtils.input(
                    RecordConstants.ARG_THRESHOLD,
                    request.getString(RecordConstants.ARG_THRESHOLD, ""),
                    5)));



        String initialDegrees = "" + getDefaultRadiusDegrees(request,
                                    entry.getBounds());

        if (request.defined(ARG_GRID_RADIUS_DEGREES)) {
            initialDegrees = request.getString(ARG_GRID_RADIUS_DEGREES, "");
        }

        String initialCells = request.getString(ARG_GRID_RADIUS_CELLS, "5");
        gridding.append(
            HtmlUtils.formEntry(
                msgLabel("IDW Grid Radius"),
                msgLabel("Grid cells")
                + HtmlUtils.input(ARG_GRID_RADIUS_CELLS, initialCells, 4)
                + HtmlUtils.space(4) + msgLabel("or degrees")
                + HtmlUtils.input(
                    ARG_GRID_RADIUS_DEGREES, initialDegrees, 12)));

        gridding.append(
            HtmlUtils.formEntry(
                msgLabel("IDW Power"),
                HtmlUtils.input(
                    RecordConstants.ARG_GRID_POWER,
                    request.getString(RecordConstants.ARG_GRID_POWER, "1.0"),
                    5) + " "
                       + HtmlUtils.href(
                           "https://gisgeography.com/inverse-distance-weighting-idw-interpolation/",
                           "More Information", "target=_help")));


        gridding.append(
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



        gridding.append(
            HtmlUtils.formEntry(
                msgLabel("Image Dimensions"),
                HtmlUtils.input(
                    ARG_WIDTH, request.getString(ARG_WIDTH, "" + DFLT_WIDTH),
                    5) + " X "
                       + HtmlUtils.input(
                           ARG_HEIGHT,
                           request.getString(ARG_HEIGHT, "" + DFLT_HEIGHT),
                           5)));


        gridding.append(
            HtmlUtils.formEntry(
                msgLabel("Color Table"),
                HtmlUtils.select(
                    ARG_COLORTABLE, ColorTable.getColorTableNames(),
                    request.getString(ARG_COLORTABLE, (String) null))));

        gridding.append(
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
        gridding.append(HtmlUtils.formTableClose());
        sb.append(HtmlUtils.makeShowHideBlock("Gridding",
                gridding.toString(), showUrl));
    }



    /**
     *
     * @param request _more_
     * @param entry _more_
     * @param sb _more_
     * @param recordEntry _more_
     *
     * @throws Exception _more_
     */
    public void addSettingsForm(Request request, Entry entry, Appendable sb,
                                RecordEntry recordEntry)
            throws Exception {

        boolean       showUrl     = request.get(ARG_SHOWURL, false);



        String        paramWidget = null;

        StringBuilder processSB   = new StringBuilder();
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


        User user = request.getUser();
        if (getMailManager().isEmailEnabled()) {
            processSB.append(HtmlUtils.formEntry(msgLabel("Send email to"),
                    HtmlUtils.input(ARG_JOB_EMAIL, user.getEmail(), 40)));
        }
        processSB.append(HtmlUtils.formEntry(msgLabel("Your name"),
                                             HtmlUtils.input(ARG_JOB_USER,
                                                 user.getName(), 40)));

        processSB.append(HtmlUtils.formEntry(msgLabel("Job name"),
                                             HtmlUtils.input(ARG_JOB_NAME,
                                                 "", 40)));

        processSB.append(HtmlUtils.formEntryTop(msgLabel("Description"),
                HtmlUtils.textArea(ARG_JOB_DESCRIPTION, "", 5, 40)));

        processSB.append(HtmlUtils.formTableClose());

        sb.append(HtmlUtils.makeShowHideBlock("Processing",
                processSB.toString(), false));
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
    public void addSubsetForm(Request request, Entry entry, Appendable sb,
                              boolean forGroup, RecordEntry recordEntry,
                              String extraSubset,boolean csvForm)
            throws Exception {

        long         numRecords = forGroup
                                  ? 0
                                  : recordEntry.getNumRecords();

	Entry theEntry  = recordEntry!=null?recordEntry.getEntry():null;

        StringBuffer subsetSB   = new StringBuffer();
        subsetSB.append(HtmlUtils.formTable());
        if (numRecords > 0) {
            subsetSB.append(HtmlUtils.formEntry("# " + msgLabel("Points"),
                    formatPointCount(numRecords)));
        }

        MapInfo map = getRepository().getMapManager().createMap(request,
                          entry, true, null);
        List<Metadata> metadataList = getMetadataManager().getMetadata(request,entry);
        boolean didMetadata = map.addSpatialMetadata(entry, metadataList);
        if ( !didMetadata) {
            map.addBox(entry,
                       new MapBoxProperties(MapInfo.DFLT_BOX_COLOR, false,
                                            true));
        } else {
            map.centerOn(entry);
        }
        SessionManager sm = getRepository().getSessionManager();
	





        if (recordEntry != null) {
            String help = "Probablity a point will be included 0.-1.0";
            String probHelpImg =HU.space(1) +
                HU.span(HU.getIconImage("fas fa-question-circle",HU.attr("title",help)),HU.cssClass("ramadda-hoverable"));
            String prob =
                HtmlUtils.space(3) + msgLabel("Or use probability") + " "
                + HtmlUtils.input(ARG_PROBABILITY,
                                  request.getString(ARG_PROBABILITY, ""),
                                  3) + probHelpImg;





            if (recordEntry.isCapable(PointFile.ACTION_TIME) ||
		theEntry.getTypeHandler().getTypeProperty("subset.date.show",
							  theEntry.getStartDate()!=theEntry.getEndDate())		
		) {
                boolean showTime = true;
                subsetSB.append(
                    formEntry(
                        request, msgLabel("Date Range"),
                        getDateHandler().makeDateInput(
                            request, ARG_FROMDATE, "entryform", null, null,
                            showTime) + HtmlUtils.space(1)
                                      + HtmlUtils.img(getIconUrl(ICON_RANGE))
                                      + HtmlUtils.space(1)
                                      + getDateHandler().makeDateInput(
                                          request, ARG_TODATE, "entryform",
                                          null, null, showTime)));
            }

            subsetSB.append(HtmlUtils.formEntry(msgLabel("Max # Points"),
                    HtmlUtils.input(ARG_MAX, request.getString(ARG_MAX, ""),
                                    4)));

            if (recordEntry.isCapable(PointFile.ACTION_DECIMATE)) {
		List<String> skips = Utils.makeList(new TwoFacedObject("None",""),"1","2","3","4","5",
						    "6","7","8","9","10","15","20","30","40","50","75","100","200","300","400","500","1000");
						   
						   
                subsetSB.append(HtmlUtils.formEntry(msgLabel("Decimate"),
                        msgLabel("Skip every") + " "
						    + HtmlUtils.select(ARG_RECORD_SKIP, skips,
								       request.getString(ARG_RECORD_SKIP,
											 "")) + prob));
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
	    String toggleAllId = HU.getUniqueId("cbx_");
            for (RecordField attr : allFields) {

                //Skip arrays
                if (attr.getArity() > 1) {
                    continue;
                }
                if (paramSB == null) {
                    paramSB = new StringBuffer();
		    //                    paramSB.append(HtmlUtils.formTable());
		    paramSB.append(HU.labeledCheckbox("", HU.VALUE_TRUE,
						      false,HU.attr("id",toggleAllId),
						      "Toggle all"));

		    paramSB.append("<br>");
                }
		String label = attr.getLabel() + " - " +  attr.getName();
                paramSB.append(HtmlUtils.labeledCheckbox(ARG_FIELD_USE,
							 attr.getName(),
							 selected.contains(attr.getName()), HU.cssClass("ramadda-subset-field"),label));
		paramSB.append("<br>");
            }
            if (paramSB != null) {
		if(csvForm)
		    subsetSB.append(
				    HtmlUtils.formEntryTop(msgLabel("Select Fields"),
							   paramSB.toString()));
		else
		    subsetSB.append(
				    HtmlUtils.formEntryTop(
							   msgLabel("Select Fields"),
							   HtmlUtils.makeShowHideBlock(
										       msg(""), paramSB.toString(), false)));

		HU.script(subsetSB,"HtmlUtils.initToggleAll('" + toggleAllId+"','.ramadda-subset-field');");
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
		if(csvForm)
		    subsetSB.append(paramSB);
		else
		    subsetSB.append(
				    HtmlUtils.formEntryTop(
							   msgLabel("Search Fields"),
							   HtmlUtils.makeShowHideBlock(
										       msg(""), paramSB.toString(), false)));


            }
        }
        subsetSB.append(extraSubset);
        subsetSB.append(HtmlUtils.formTableClose());
	if(csvForm)
	    sb.append(subsetSB);
	else
	    sb.append(HtmlUtils.makeShowHideBlock("Subset Data",
						  subsetSB.toString(), false));

    }

    /**
     * add to form
     *
     * @param request request
     * @param entry _more_
     * @param sb buffer
     * @param forGroup for group
     * @param recordEntry the entry
     *
     * @throws Exception On badness
     */
    public void addSelectForm(Request request, Entry entry, Appendable sb,
                              boolean forGroup, RecordEntry recordEntry)
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
        boolean anySelected  = false;
        boolean gridSelected = false;
        for (String arg : GRID_ARGS) {
            if (request.get(arg, false)) {
                anySelected = true;
            }
        }
        for (HtmlUtils.Selector selector : pointFormats) {
            if (selectedFormat.contains(selector.getId())) {
                anySelected = true;
            }

        }

        for (int i = 0; i < GRID_ARGS.length; i++) {
            String helpImg =
                HtmlUtils.img(getRepository().getIconUrl(ICON_HELP),
                              GRID_HELP[i]);
            gridsCol.append(helpImg);
            gridsCol.append(" ");
            boolean on = request.get(GRID_ARGS[i], false);
            if ( !anySelected && GRID_ARGS[i].equals(ARG_GRID_IDW)) {
                on = true;
            }
            if (on) {
                gridSelected = true;
            }
            gridsCol.append(HtmlUtils.labeledCheckbox(GRID_ARGS[i], "true",
                    on, GRID_LABELS[i]));
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

            boolean productSelected = false;
            for (HtmlUtils.Selector selector : formatList) {
                if (selectedFormat.contains(selector.getId())) {
                    productSelected = true;
                }
            }

            for (HtmlUtils.Selector selector : formatList) {
                String label = "";
                if (selector.getIcon() != null) {
                    //Add the  space because fontawesome icons overlap the cbx
                    label = HU.space(3) + HtmlUtils.img(selector.getIcon())
                            + " ";
                }
                label += selector.getLabel();
                boolean on = selectedFormat.contains(selector.getId());
                if (i == 1) {
                    if (gridSelected && !productSelected) {
                        on              = true;
                        productSelected = true;
                    }
                }
                formatCol.append(HtmlUtils.labeledCheckbox(ARG_PRODUCT,
                        selector.getId(), on, label));

                formatCol.append(HtmlUtils.p());
            }
            formats.append(HtmlUtils.col(formatCol.toString()));
        }
        formats.append("</tr></table>");
        productSB.append(HtmlUtils.row(HtmlUtils.colspan(formats.toString(),
                2)));
        productSB.append(HtmlUtils.formTableClose());
        sb.append(
            HtmlUtils.hidden(
                ARG_OUTPUT, getPointOutputHandler().OUTPUT_PRODUCT.getId()));
        sb.append(HtmlUtils.makeShowHideBlock("Select Products",
                productSB.toString(), true));

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




}
