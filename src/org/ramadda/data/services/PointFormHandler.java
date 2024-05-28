/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.data.services;


import org.ramadda.repository.type.TypeHandler;

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

        String  formId  = HU.getUniqueId("form_");
        sb.append(request.formPost(getRepository().URL_ENTRY_SHOW,
                                   HU.id(formId)));
        sb.append(HU.hidden(ARG_ENTRYID, group.getId()));
        sb.append(HU.hidden(ARG_RECORDENTRY_CHECK, "true"));

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

            entrySB.append(HU.checkbox(ARG_RECORDENTRY, entry.getId(),
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

            sb.append(HU.sectionClose());

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
                + HU.div(entrySB.toString(), HU.style("max-height:100px;  overflow-y: auto; border: 1px #999999 solid;"))
                + "</td><td width=25%>&nbsp;</td><tr></table>";
        }

        String extra = HU.formEntryTop((recordEntries.size() == 1)
                ? ""
                : msgLabel("Files"), files);

        addToGroupForm(request, group, sb, recordEntries, extra);


        sb.append("<p>");
        sb.append(HU.submit("Get Data", ARG_GETDATA));
        sb.append("<p>");
        OutputHandler.addUrlShowingForm(sb, formId,
                                        "[\".*OpenLayers_Control.*\"]");
        sb.append(HU.formClose());

        sb.append(HU.sectionClose());

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
        String formId = HU.getUniqueId("form_");
        sb.append(request.formPost(getRepository().URL_ENTRY_SHOW,
                                   HU.id(formId)));
        sb.append(HU.hidden(ARG_ENTRYID, entry.getId()));
        addToEntryForm(request, entry, sb, recordEntry,false);
        sb.append("<p>");
        sb.append(HU.submit("Get Data", ARG_GETDATA));
        sb.append("<p>");
        OutputHandler.addUrlShowingForm(sb, formId,
                                        "[\".*OpenLayers_Control.*\"]");
        sb.append(HU.formClose());

        sb.append(HU.sectionClose());

        request.getRepository().getPageHandler().entrySectionClose(request,
                entry, sb);

        return new Result("", sb);
    }


    public Result outputEntryFormCsv(Request request, Entry entry,
				     Appendable msgSB)
            throws Exception {

        StringBuilder sb = new StringBuilder();
        request.getRepository().getPageHandler().entrySectionOpen(request,
                entry, sb, "Data Download");
	if(msgSB!=null)
	    sb.append(msgSB);
	getEntryFormCsv(request,entry,sb);
        request.getRepository().getPageHandler().entrySectionClose(request,
                entry, sb);


        return new Result("", sb);
    }

    public void getEntryFormCsv(Request request, Entry entry,   Appendable sb)
	throws Exception {
        RecordEntry recordEntry =
            getPointOutputHandler().doMakeEntry(request, entry);
        String formId = HU.getUniqueId("form_");
        sb.append(request.formPost(getRepository().URL_ENTRY_SHOW,
                                   HU.id(formId)));
        sb.append(HU.hidden(ARG_ENTRYID, entry.getId()));
	
        sb.append(
            HU.hidden(
                ARG_OUTPUT, getPointOutputHandler().OUTPUT_PRODUCT.getId()));
        addToEntryForm(request, entry, sb, recordEntry,true);
        sb.append("<p>");
        sb.append(HU.submit("Get Data", ARG_GETDATA));
        sb.append("<p>");
        OutputHandler.addUrlShowingForm(sb, formId,
                                        "[\".*OpenLayers_Control.*\"]");
        sb.append(HU.formClose());
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

        sb.append(HU.submit("Get Data", ARG_GETDATA));
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
        sb.append(HU.submit("Get Data", ARG_GETDATA));
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
        gridding.append(HU.formTable());

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
                HU.formEntry(
                    msgLabel("Parameter to grid"),
                    HU.select(
                        RecordOutputHandler.ARG_PARAMETER, params,
                        selectedParam)));
            gridding.append(
                HU.formEntry(
                    msgLabel("Divisor"),
                    HU.select(
                        RecordOutputHandler.ARG_DIVISOR, params,
                        request.get(
                            RecordOutputHandler.ARG_DIVISOR,
                            new ArrayList<String>()), HU.arg(
                                HU.ATTR_ROWS, "4") + " multiple ")));
        }



        gridding.append(
            HU.formEntry(
                msgLabel("Minimum # of Points"),
                HU.input(
                    RecordConstants.ARG_GRID_MINPOINTS,
                    request.getString(
                        RecordConstants.ARG_GRID_MINPOINTS, "1"), 5)));

        /*
        gridding.append(
            HU.formEntry(
                msgLabel("Fill missing"),
                HU.checkbox(
                    PointOutputHandler.ARG_FILLMISSING, "true",
                    request.get(PointOutputHandler.ARG_FILLMISSING, false))));
        */

        gridding.append(
            HU.formEntry(
                msgLabel("Threshold"),
                HU.input(
                    RecordConstants.ARG_THRESHOLD,
                    request.getString(RecordConstants.ARG_THRESHOLD, ""),
                    5)));



        String initialDegrees = "" + getDefaultRadiusDegrees(request,
                                    entry.getBounds(request));

        if (request.defined(ARG_GRID_RADIUS_DEGREES)) {
            initialDegrees = request.getString(ARG_GRID_RADIUS_DEGREES, "");
        }

        String initialCells = request.getString(ARG_GRID_RADIUS_CELLS, "5");
        gridding.append(
            HU.formEntry(
                msgLabel("IDW Grid Radius"),
                msgLabel("Grid cells")
                + HU.input(ARG_GRID_RADIUS_CELLS, initialCells, 4)
                + HU.space(4) + msgLabel("or degrees")
                + HU.input(
                    ARG_GRID_RADIUS_DEGREES, initialDegrees, 12)));

        gridding.append(
            HU.formEntry(
                msgLabel("IDW Power"),
                HU.input(
                    RecordConstants.ARG_GRID_POWER,
                    request.getString(RecordConstants.ARG_GRID_POWER, "1.0"),
                    5) + " "
                       + HU.href(
                           "https://gisgeography.com/inverse-distance-weighting-idw-interpolation/",
                           "More Information", "target=_help")));


        gridding.append(
            HU.formEntry(
                msgLabel("Hill shading"),
                msgLabel("Azimuth")
                + HU.input(
                    ARG_HILLSHADE_AZIMUTH,
                    request.getString(ARG_HILLSHADE_AZIMUTH, "315"),
                    4) + HU.space(4) + msgLabel("Angle")
                       + HU.input(
                           ARG_HILLSHADE_ANGLE,
                           request.getString(ARG_HILLSHADE_ANGLE, "45"), 4)));



        gridding.append(
            HU.formEntry(
                msgLabel("Image Dimensions"),
                HU.input(
                    ARG_WIDTH, request.getString(ARG_WIDTH, "" + DFLT_WIDTH),
                    5) + " X "
                       + HU.input(
                           ARG_HEIGHT,
                           request.getString(ARG_HEIGHT, "" + DFLT_HEIGHT),
                           5)));


        gridding.append(
            HU.formEntry(
                msgLabel("Color Table"),
                HU.select(
                    ARG_COLORTABLE, ColorTable.getColorTableNames(),
                    request.getString(ARG_COLORTABLE, (String) null))));

        gridding.append(
            HU.formEntry(
                msgLabel("Color Range"),
                HU.input(
                    RecordConstants.ARG_GRID_RANGE_MIN,
                    request.getString(
                        RecordConstants.ARG_GRID_RANGE_MIN, ""), 5) + " -- "
                            + HU.input(
                                RecordConstants.ARG_GRID_RANGE_MAX,
                                request.getString(
                                    RecordConstants.ARG_GRID_RANGE_MAX,
                                    ""), 5)));
        gridding.append(HU.formTableClose());
        sb.append(HU.makeShowHideBlock("Gridding",
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
        processSB.append(HU.formTable());
        processSB.append(HU.formEntry("",
                                             HU.checkbox(ARG_ASYNCH,
                                                 "true", true) + " "
                                                     + msg("Asynchronous")));

        processSB.append(HU.formEntry(msgLabel("Alternate header"),
                                             HU.input(ARG_HEADER, "")
                                             + " "
                                             + "Enter 'none' for no header"));

        processSB.append(
            HU.formEntry(
                "",
                HU.checkbox(ARG_RESPONSE, RESPONSE_XML, false)
                + " Return response in XML"));

        processSB.append(
            HU.formEntry(
                "",
                HU.checkbox(ARG_POINTCOUNT, "true", false)
                + " Just return the estimated point count"));



        getOutputHandler().addPublishWidget(
            request, entry, processSB,
            msg("Select a folder to publish the product to"));


        User user = request.getUser();
        if (getMailManager().isEmailEnabled()) {
            processSB.append(HU.formEntry(msgLabel("Send email to"),
                    HU.input(ARG_JOB_EMAIL, user.getEmail(), 40)));
        }
        processSB.append(HU.formEntry(msgLabel("Your name"),
                                             HU.input(ARG_JOB_USER,
                                                 user.getName(), 40)));

        processSB.append(HU.formEntry(msgLabel("Job name"),
                                             HU.input(ARG_JOB_NAME,
                                                 "", 40)));

        processSB.append(HU.formEntryTop(msgLabel("Description"),
					 HU.textArea(ARG_JOB_DESCRIPTION, "", 5, 40)));					 

        processSB.append(HU.formTableClose());

        sb.append(HU.makeShowHideBlock("Processing",
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
        sb.append(HU.insetDiv(contents, 0, 20, 10, 0));
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
        subsetSB.append(HU.formTable());
        if (numRecords > 0) {
            subsetSB.append(HU.formEntry("# " + msgLabel("Points"),
                    formatPointCount(numRecords)));
        }

	if(csvForm) {
	    List products = new ArrayList();
	    products.add(new TwoFacedObject("CSV", "points.csv"));
	    products.add(new TwoFacedObject("JSON", "points.json"));
	    //	    products.add(new TwoFacedObject("NetCDF", "points.nc"));
	    HU.formEntry(subsetSB,msgLabel("Format"),
			 HU.select(ARG_PRODUCT, products,
				   request.getString(ARG_PRODUCT,"")));

	    List sdfs = new ArrayList();
	    sdfs.add(new TwoFacedObject("Default", ""));
	    sdfs.add(new TwoFacedObject("ISO8601", "iso8601"));
	    sdfs.add("yyyy-MM-dd HH:mm:ss");
	    sdfs.add("yyyy-MM-dd");
	    sdfs.add("yyyyMMdd");
	    sdfs.add("yyyy/MM/dd");	    	    
	    subsetSB.append(HU.formEntry(
					  msgLabel("Date Format"),
					  HU.select(
							   ARG_DATEFORMAT, sdfs,
							   request.getString(ARG_DATEFORMAT,"")) +HU.space(1) +
					  HU.b("Or custom:") + HU.space(1) +HU.input(ARG_DATEFORMAT+"_custom",
										     request.getString(ARG_DATEFORMAT+"_custom",""),
										     HU.attrs("placeholder","yyyyMMdd HH:mm:ss.SSSZ"))));
	    

	}


        MapInfo map = getRepository().getMapManager().createMap(request,
                          entry, true, null);
        List<Metadata> metadataList = getMetadataManager().getMetadata(request,entry);
        boolean didMetadata = map.addSpatialMetadata(entry, metadataList);
        if ( !didMetadata) {
            map.addBox(request,entry,
                       new MapProperties(MapInfo.DFLT_BOX_COLOR, false,
                                            true));
        } else {
            map.centerOn(request,entry);
        }
	getMapManager().initMapSelector(request, entry.getTypeHandler(),entry.getParentEntry(), entry, map);
	String mapSelector = map.makeSelector(ARG_AREA, true, null,   "", "");
	HU.formEntry(subsetSB,msgLabel("Subset"),  mapSelector);
        if (recordEntry != null) {
            String help = "Probablity a point will be included 0 - 1.0";
            String probHelpImg =HU.space(1) +
                HU.span(HU.getIconImage("fas fa-question-circle",HU.attr("title",help)),HU.cssClass("ramadda-hoverable"));
            String prob =
                HU.space(3) + msgLabel("Or use probability") + " "
                + HU.input(ARG_PROBABILITY,
                                  request.getString(ARG_PROBABILITY, ""),
                                  5,HU.attr("placeholder","e.g., 0.5")) + probHelpImg;





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
                            showTime) + HU.space(1)
                                      + HU.img(getIconUrl(ICON_RANGE))
                                      + HU.space(1)
                                      + getDateHandler().makeDateInput(
                                          request, ARG_TODATE, "entryform",
                                          null, null, showTime)));
            }

            subsetSB.append(HU.formEntry(msgLabel("Max # Rows"),
                    HU.input(ARG_MAX, request.getString(ARG_MAX, ""),
                                    4)));

            if (recordEntry.isCapable(PointFile.ACTION_DECIMATE)) {
		List<String> skips = Utils.makeListFromValues(new TwoFacedObject("None",""),"1","2","3","4","5",
						    "6","7","8","9","10","15","20","30","40","50","75","100","200","300","400","500","1000");
						   
						   
                subsetSB.append(HU.formEntry(msgLabel("Decimate"),
                        msgLabel("Skip every") + " "
						    + HU.select(ARG_RECORD_SKIP, skips,
								       request.getString(ARG_RECORD_SKIP,
											 "")) + prob));
            }

            if (recordEntry.isCapable(PointFile.ACTION_TRACKS)) {
                subsetSB.append(
                    HU.formEntry(
                        msgLabel("GLAS Tracks"),
                        HU.input(
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
		    //                    paramSB.append(HU.formTable());
		    paramSB.append(HU.labeledCheckbox("", HU.VALUE_TRUE,
						      false,HU.attr("id",toggleAllId),
						      "Toggle all"));

		    paramSB.append("<br>");
                }
		String label = attr.getLabel() + " - " +  attr.getName();
                paramSB.append(HU.labeledCheckbox(ARG_FIELD_USE,
							 attr.getName(),
							 selected.contains(attr.getName()), HU.cssClass("ramadda-subset-field"),label));
		paramSB.append("<br>");
            }
            if (paramSB != null) {
		if(csvForm)
		    subsetSB.append(
				    HU.formEntryTop(msgLabel("Select Fields"),
							   paramSB.toString()));
		else
		    subsetSB.append(
				    HU.formEntryTop(
							   msgLabel("Select Fields"),
							   HU.makeShowHideBlock(
										       msg(""), paramSB.toString(), false)));

		HU.script(subsetSB,"HU.initToggleAll('" + toggleAllId+"','.ramadda-subset-field');");
            }
        }


	RecordTypeHandler typeHandler = (RecordTypeHandler) (theEntry!=null?theEntry.getTypeHandler():null);
	Hashtable props = typeHandler!=null?typeHandler.getRecordProperties(entry):null;

        if (searchableFields != null) {
            StringBuffer paramSB = null;
            for (RecordField field : searchableFields) {
                List<String[]> enums        = field.getEnumeratedValues();
                String         searchSuffix = field.getSearchSuffix();
		if(!Utils.stringDefined(searchSuffix) && props!=null) {
		    searchSuffix = Utils.getProperty(props,"record.searchsuffix." + field.getName(),null);
		}

                if (searchSuffix == null) {
                    searchSuffix = "";
                } else {
                    searchSuffix = "  " + searchSuffix;
                }
                if (field.isBitField()) {
                    if (paramSB == null) {
                        paramSB = new StringBuffer();
                        paramSB.append(HU.formTable());
                    }
                    String[]     bitFields = field.getBitFields();
                    StringBuffer widgets   = new StringBuffer();
                    paramSB.append(
                        HU.row(
                            HU.colspan(
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
                        paramSB.append(HU.formEntry(bitField + ":",
                                HU.select(urlArgPrefix + bitIdx,
                                    values, value, "")));
                    }
                } else if (enums != null) {
                    List values = new ArrayList();
                    values.add(new TwoFacedObject("--", ""));
                    for (String[] tuple : enums) {
                        values.add(new TwoFacedObject(tuple[1], tuple[0]));
                    }
                    String attrWidget = HU.select(ARG_SEARCH_PREFIX
                                            + field.getName(), values, "",
                                                "");
                    if (paramSB == null) {
                        paramSB = new StringBuffer();
                        paramSB.append(HU.formTable());
                    }
                    paramSB.append(
                        HU.formEntry(
                            msgLabel(field.getLabel()),
                            attrWidget + searchSuffix));
                } else if(field.isTypeEnumeration()) {
                    if (paramSB == null) {
                        paramSB = new StringBuffer();
                        paramSB.append(HU.formTable());
                    }
                    String attrWidget = HU.textArea(ARG_SEARCH_PREFIX + field.getName()+"_enumlist", "", 5, 40);
                    paramSB.append(
                        HU.formEntry(
                            msgLabel(field.getLabel()),
                            attrWidget + searchSuffix));

                } else {
                    String attrWidget = HU.input(
                                            ARG_SEARCH_PREFIX
                                            + field.getName() + "_min", "",
                                                HU.SIZE_8) + " - "
                                                    + HU.input(
                                                        ARG_SEARCH_PREFIX
                                                        + field.getName()
                                                        + "_max", "",
                                                            HU.SIZE_8);
                    if (paramSB == null) {
                        paramSB = new StringBuffer();
                        paramSB.append(HU.formTable());
                    }
                    paramSB.append(
                        HU.formEntry(
                            msgLabel(field.getLabel() + " range"),
                            attrWidget + searchSuffix));
                }

            }
            if (paramSB != null) {
                paramSB.append(HU.formTableClose());
		subsetSB.append(HU.formEntryTop(msgLabel("Search Values"),
						HU.makeShowHideBlock(msg(""), paramSB.toString(), false)));


            }
        }
        subsetSB.append(extraSubset);
        subsetSB.append(HU.formTableClose());
	if(csvForm)
	    sb.append(subsetSB);
	else
	    sb.append(HU.makeShowHideBlock("Subset Data",
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
        productSB.append(HU.formTable());
        HashSet<String> selectedFormat = getFormats(request);
        StringBuffer formats =
            new StringBuffer(
                "<table border=0 cellpadding=4 cellspacing=0><tr valign=top>");

        int          cnt = 0;
        StringBuffer formatCol;
        StringBuffer gridsCol = new StringBuffer();
        gridsCol.append(HU.b(msg("Select Grids")));
        gridsCol.append(HU.p());
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
                HU.img(getRepository().getIconUrl(ICON_HELP),
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
            gridsCol.append(HU.labeledCheckbox(GRID_ARGS[i], "true",
                    on, GRID_LABELS[i]));
            gridsCol.append(HU.p());
        }



        for (int i = 0; i < formatLists.size(); i++) {
            List<HtmlUtils.Selector> formatList = formatLists.get(i);
            formatCol = new StringBuffer();
            if (i == 0) {
                formatCol.append(HU.b(msg("Point Products")));
            } else {
                if ( !recordEntry.isCapable(PointFile.ACTION_GRID)) {
                    continue;
                }
                formats.append(HU.col(HU.space(5)));
                formats.append(
                    HU.col(
                        HU.img(
                            getRepository().getFileUrl(
                                "/icons/blank.gif")), HU.style(
                                    "border-left:1px #000000 solid")));
                formats.append(HU.col(HU.space(4)));
                formats.append(HU.col(gridsCol.toString()));
                String middle =
                    "<br><br><br>&nbsp;&nbsp;&nbsp;to make&nbsp;&nbsp;&nbsp;<br>"
                    + HU.img(
                        getRepository().getFileUrl("/point/rightarrow.jpg"));
                formats.append(
                    HU.col(
                        middle,
                        HU.attr(HU.ATTR_ALIGN, "center")));
                formatCol.append(HU.b(msg("Select Products")));
            }
            formatCol.append(HU.p());

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
                    label = HU.space(3) + HU.img(selector.getIcon())
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
                formatCol.append(HU.labeledCheckbox(ARG_PRODUCT,
                        selector.getId(), on, label));

                formatCol.append(HU.p());
            }
            formats.append(HU.col(formatCol.toString()));
        }
        formats.append("</tr></table>");
        productSB.append(HU.row(HU.colspan(formats.toString(),
                2)));
        productSB.append(HU.formTableClose());
        sb.append(
            HU.hidden(
                ARG_OUTPUT, getPointOutputHandler().OUTPUT_PRODUCT.getId()));
        sb.append(HU.makeShowHideBlock("Select Products",
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
