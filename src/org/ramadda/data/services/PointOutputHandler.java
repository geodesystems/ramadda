/**
   Copyright (c) 2008-2021 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.data.services;


import org.ramadda.data.point.PointRecord;
import org.ramadda.data.record.BaseRecord;
import org.ramadda.data.record.CsvVisitor;
import org.ramadda.data.record.NetcdfVisitor;
import org.ramadda.data.record.GeoRecord;
import org.ramadda.data.record.RecordField;
import org.ramadda.data.record.RecordFile;
import org.ramadda.data.record.RecordVisitor;
import org.ramadda.data.record.RecordVisitorGroup;
import org.ramadda.data.record.VisitInfo;
import org.ramadda.data.record.filter.*;
import org.ramadda.repository.Entry;
import org.ramadda.repository.Link;
import org.ramadda.repository.Repository;
import org.ramadda.repository.Request;
import org.ramadda.repository.Result;
import org.ramadda.repository.ServiceInfo;
import org.ramadda.repository.job.JobInfo;
import org.ramadda.repository.map.MapInfo;
import org.ramadda.repository.metadata.Metadata;
import org.ramadda.repository.metadata.MetadataHandler;
import org.ramadda.repository.output.OutputType;
import org.ramadda.repository.output.CsvOutputHandler;
import org.ramadda.repository.type.TypeHandler;
import org.ramadda.util.ColorTable;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.SelectionRectangle;
import org.ramadda.util.Utils;
import org.ramadda.util.geo.Bounds;
import org.ramadda.util.geo.KmlUtil;
import org.ramadda.util.grid.IdwGrid;
import org.ramadda.util.grid.LatLonGrid;

import org.w3c.dom.Element;

import org.apache.commons.io.input.ReversedLinesFileReader;

import ucar.unidata.ui.ImageUtils;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;

import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;
//import ucar.nc2.ft.point.writer.CFPointObWriter;
//import ucar.nc2.ft.point.writer.PointObVar;


import java.awt.Color;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.geom.Rectangle2D;
import java.awt.image.*;
import java.awt.image.MemoryImageSource;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


/**
 */
@SuppressWarnings("unchecked")
public class PointOutputHandler extends RecordOutputHandler {


    /** The category for the all of the output types */
    public static final String OUTPUT_CATEGORY = "Point Data";


    /** This is used to create a product for a point collection or a point file. */
    public final OutputType OUTPUT_PRODUCT;

    /** output type */
    public final OutputType OUTPUT_VIEW;

    /** output type */
    public final OutputType OUTPUT_METADATA;

    /** output type */
    public final OutputType OUTPUT_POINTCOUNT;

    /** output type */
    public final OutputType OUTPUT_CHART;

    /** output type */
    public final OutputType OUTPUT_FORM;

    public final OutputType OUTPUT_FORM_CSV;    

    /** output type */
    public final OutputType OUTPUT_TIMESERIES_IMAGE;

    /** output type */
    public OutputType OUTPUT_LAS;

    /** output type */
    public final OutputType OUTPUT_GETPOINTINDEX;


    /** output type */
    public final OutputType OUTPUT_GETLATLON;

    /** output type */
    public final OutputType OUTPUT_IMAGE;

    /** _more_ */
    public final OutputType OUTPUT_BOUNDS;

    /** output type */
    public final OutputType OUTPUT_NC;

    /** output type */
    public final OutputType OUTPUT_HILLSHADE;

    /** output type */
    public final OutputType OUTPUT_KMZ;

    /** output type */
    public final OutputType OUTPUT_KML_TRACK;


    /** output type */
    public final OutputType OUTPUT_SUBSET;


    /** output type */
    public final OutputType OUTPUT_KML;

    /** output type */
    public final OutputType OUTPUT_LATLONALTBIN;

    /** output type */
    public final OutputType OUTPUT_LATLONALTCSV;

    /** output type */
    public final OutputType OUTPUT_CSV;

    /**  */
    public final OutputType OUTPUT_IDVCSV;

    /** _more_ */
    public final OutputType OUTPUT_JSON;


    /** output type */
    public final OutputType OUTPUT_ASC;


    /** output type */
    public final OutputType OUTPUT_WAVEFORM_CSV;


    /**
     * constructor. This gets called by the Repository via reflection
     *
     * @param repository the repository
     * @param element the xml from outputhandlers.xml
     * @throws Exception on badness
     */
    public PointOutputHandler(Repository repository, Element element)
	throws Exception {

        super(repository, element);
        String category = getOutputCategory();
        String base     = getDomainBase();

        OUTPUT_PRODUCT = new OutputType("Results", base + ".product",
                                        OutputType.TYPE_OTHER);

        OUTPUT_RESULTS = new OutputType("Results", base + ".results",
                                        OutputType.TYPE_OTHER);

        OUTPUT_VIEW = new OutputType("View Data", base + ".view",
                                     OutputType.TYPE_VIEW, "", ICON_DATA,
                                     category);

        OUTPUT_METADATA = new OutputType("Metadata ", base + ".metadata",
                                         OutputType.TYPE_VIEW, "",
                                         ICON_METADATA, category);


        OUTPUT_GETPOINTINDEX = new OutputType("Point index query",
					      base + ".getpointindex", OutputType.TYPE_OTHER, "",
					      ICON_DATA, category);

        OUTPUT_GETLATLON = new OutputType("Lat/Lon query",
                                          base + ".getlatlon",
                                          OutputType.TYPE_OTHER, "",
                                          ICON_DATA, category);



        OUTPUT_IMAGE = new OutputType("Image", base + ".image",
                                      OutputType.TYPE_OTHER, "png",
                                      ICON_IMAGE, category);

        OUTPUT_BOUNDS = new OutputType("Point Bounds", base + ".bounds",
                                       OutputType.TYPE_OTHER);

        OUTPUT_NC = new OutputType("NetCDF Point", base + ".nc",
                                   OutputType.TYPE_OTHER, "nc", ICON_DATA,
                                   category);

        OUTPUT_HILLSHADE = new OutputType("Hill Shade Image",
                                          base + ".hillshade",
                                          OutputType.TYPE_OTHER, "png",
                                          ICON_IMAGE, category);

        OUTPUT_KMZ = new OutputType("Google Earth", base + ".kmz",
                                    OutputType.TYPE_OTHER, "kmz", ICON_KML,
                                    category);

        OUTPUT_KML_TRACK = new OutputType("Google Earth",
                                          base + ".kml.track",
                                          OutputType.TYPE_OTHER, "kml",
                                          ICON_KML, category);


        OUTPUT_SUBSET = new OutputType("Native format", base + ".subset",
                                       OutputType.TYPE_OTHER, "", ICON_DATA,
                                       category);


        OUTPUT_KML = new OutputType("Google Earth", base + ".kml",
                                    OutputType.TYPE_OTHER, "kml", ICON_KML,
                                    category);


        OUTPUT_LATLONALTBIN = new OutputType("Binary - Lat/Lon/Alt",
                                             base + ".latlonaltbin",
                                             OutputType.TYPE_OTHER, "llab",
                                             ICON_CSV, category);

        OUTPUT_LATLONALTCSV = new OutputType("CSV - Lat/Lon/Alt",
                                             base + ".latlonaltcsv",
                                             OutputType.TYPE_OTHER, "csv",
                                             ICON_CSV, category);

        OUTPUT_CSV = new OutputType("CSV - all fields", base + ".csv",
                                    OutputType.TYPE_OTHER, "csv", ICON_CSV,
                                    category);


        OUTPUT_IDVCSV = new OutputType("CSV for IDV", base + ".idv.csv",
                                       OutputType.TYPE_OTHER, "csv",
                                       ICON_CSV, category);

        OUTPUT_JSON = new OutputType("JSON", base + ".json",
                                     OutputType.TYPE_OTHER, "json", ICON_CSV,
                                     category);

        OUTPUT_ASC = new OutputType("ARC ASCII Grid", base + ".asc",
                                    OutputType.TYPE_OTHER, "asc", ICON_DATA,
                                    category);

        OUTPUT_POINTCOUNT = new OutputType("Point Count", base + ".count",
                                           OutputType.TYPE_OTHER);


        OUTPUT_CHART = new OutputType("Chart ", base + ".chart",
                                      OutputType.TYPE_OTHER
                                      | OutputType.TYPE_IMPORTANT, "",
				      "fa-chart-line", category);

        OUTPUT_FORM_CSV = new OutputType("CSV Download Form", base + ".formcsv",
					 OutputType.TYPE_FILE
					 | OutputType.TYPE_IMPORTANT, "",
					 "fa-solid fa-arrow-down-short-wide fa-darkgray",
					 //                                         ICON_TOOLS, 
					 category);


        OUTPUT_FORM = new OutputType("Subset and Products", base + ".form",
                                     OutputType.TYPE_OTHER
                                     | OutputType.TYPE_IMPORTANT, "",
				     ICON_TOOLS, category);

        OUTPUT_TIMESERIES_IMAGE = new OutputType("",
						 base + ".timeseriesimage", OutputType.TYPE_OTHER, "",
						 ICON_IMAGE, category);


        OUTPUT_WAVEFORM_CSV = new OutputType("Waveform CSV",
                                             base + ".waveformcsv",
                                             OutputType.TYPE_OTHER, "",
                                             ICON_DATA, category);


        setFormHandler(new PointFormHandler(this));
        setRecordJobManager(new PointJobManager(this));
        addDefaultOutputTypes();
    }

    /**
     * _more_
     */
    protected void addDefaultOutputTypes() {
        //Add the output types
        addType(OUTPUT_RESULTS);
        addType(OUTPUT_PRODUCT);
        addType(OUTPUT_CSV);
        addType(OUTPUT_IDVCSV);
        addType(OUTPUT_JSON);
        addType(OUTPUT_NC);
        addType(OUTPUT_VIEW);
        addType(OUTPUT_BOUNDS);
        addType(OUTPUT_METADATA);
        addType(OUTPUT_SUBSET);
        addType(OUTPUT_LATLONALTCSV);
        addType(OUTPUT_LATLONALTBIN);
        addType(OUTPUT_GETLATLON);
        addType(OUTPUT_GETPOINTINDEX);
        addType(OUTPUT_ASC);
        addType(OUTPUT_CHART);
        addType(OUTPUT_FORM_CSV);
        addType(OUTPUT_FORM);
        addType(OUTPUT_WAVEFORM_CSV);
        addType(OUTPUT_IMAGE);
        addType(OUTPUT_HILLSHADE);
        addType(OUTPUT_KMZ);
        addType(OUTPUT_KML_TRACK);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getDomainName() {
        return "Points";
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getOutputCategory() {
        return OUTPUT_CATEGORY;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getDomainBase() {
        return "points";
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     */
    @Override
    public RecordEntry doMakeEntry(Request request, Entry entry) {
        RecordTypeHandler typeHandler =
            (RecordTypeHandler) entry.getTypeHandler();

        return new PointEntry(
			      (PointOutputHandler) typeHandler.getRecordOutputHandler(),
			      request, entry);
    }


    /**
     * Not implemented yet. This will get the point index of a given lat/lon
     *
     * @param request request
     * @param entry the entry
     *
     *
     * @return _more_
     * @throws Exception On badness
     */
    public Result outputEntryGetPointIndex(Request request, Entry entry)
	throws Exception {
        //TODO: find the closest index to the lat/lon in the request                                                              
        int          index = 1;
        StringBuffer sb    = new StringBuffer("<result>");
        sb.append("{\"index\":" + index + "}");
        sb.append("</result>");

        return new Result("", sb, "text/xml");
    }


    /**
     * Get the FormHandler property.
     *
     * @return The FormHandler
     */
    public PointFormHandler getPointFormHandler() {
        return (PointFormHandler) super.getFormHandler();
    }



    /**
     * Gets the approximate point count of the given record files. It figures out
     * the  area  of the of the clipping box intersection with each file.
     *
     * @param request request
     * @param subsetEntries entries
     *
     * @return approximate point count in spatial subset
     *
     * @throws Exception On badness
     */
    public long getApproximatePointCount(
					 Request request, List<? extends RecordEntry> subsetEntries)
	throws Exception {
        long pointCount = 0;
        storeSession(request);
        double north = request.get(ARG_AREA_NORTH, 90.0);
        double south = request.get(ARG_AREA_SOUTH, -90.0);
        double east  = request.get(ARG_AREA_EAST, 180.0);
        double west  = request.get(ARG_AREA_WEST, -180.0);

        Rectangle2D.Double queryRect = new Rectangle2D.Double(west, south,
							      east - west, north - south);
        for (RecordEntry entry : subsetEntries) {
            Rectangle2D.Double entryBounds = entry.getEntry().getBounds(request);
            Rectangle2D intersection =
                entryBounds.createIntersection(queryRect);
            double percent =
                (intersection.getWidth() * intersection.getHeight())
                / (entryBounds.getWidth() * entryBounds.getHeight());
            pointCount += (long) (percent * entry.getNumRecords());
        }

        return pointCount;
    }

    /**
     * Checks for  any spatial bounds URL arguments. If defined then only returns
     * the RecordEntry objects that intersect the bounds
     *
     * @param request The request
     * @param recordEntries The entries to process
     *
     * @return spatially subsetting ReordEntry-s
     *
     * @throws Exception On badness
     */
    @Override
    public List<RecordEntry> doSubsetEntries(
					     Request request, List<? extends RecordEntry> recordEntries)
	throws Exception {

        List<RecordEntry> goodEntries = super.doSubsetEntries(request,
							      recordEntries);

        Date[] dateRange = request.getDateRange(ARG_FROMDATE, ARG_TODATE, "",
						new Date());

        if ((dateRange[0] != null) || (dateRange[1] != null)) {
            List<RecordEntry> timeEntries = new ArrayList<RecordEntry>();
            for (RecordEntry recordEntry : goodEntries) {
                Entry entry = recordEntry.getEntry();

                if (dateRange[0] != null) {
                    if (entry.getEndDate() < dateRange[0].getTime()) {
                        System.err.println("Skipping " + entry
                                           + " because of time range");

                        continue;
                    }
                }

                if (dateRange[1] != null) {
                    if (entry.getStartDate() > dateRange[1].getTime()) {
                        System.err.println("Skipping " + entry
                                           + " because of time range");

                        continue;
                    }
                }
                timeEntries.add(recordEntry);
            }
            goodEntries = timeEntries;
        }


        SelectionRectangle theBbox = request.getSelectionBounds();
        if ( !theBbox.anyDefined()) {
            return goodEntries;
        }
        storeSession(request);
        theBbox.normalizeLongitude();
        SelectionRectangle[] bboxes       = theBbox.splitOnDateLine();

        List<RecordEntry>    spaceEntries = new ArrayList<RecordEntry>();
        for (RecordEntry recordEntry : goodEntries) {
            Entry entry = recordEntry.getEntry();

            if ( !entry.hasAreaDefined(request)) {
                continue;
            }
            for (SelectionRectangle bbox : bboxes) {
                Rectangle2D.Double queryRect = new Rectangle2D.Double(
								      bbox.getWest(-180),
								      bbox.getSouth(-90),
								      bbox.getEast(180)
								      - bbox.getWest(
										     -180), bbox.getNorth(
													  90) - bbox.getSouth(
															      -90));
                Rectangle2D.Double entryRect =
                    new Rectangle2D.Double(entry.getWest(request), entry.getSouth(request),
                                           entry.getEast(request) - entry.getWest(request),
                                           entry.getNorth(request)
                                           - entry.getSouth(request));
                if (entryRect.intersects(queryRect)
		    || entryRect.contains(queryRect)
		    || queryRect.contains(entryRect)) {
                    spaceEntries.add(recordEntry);

                    break;
                }
            }
        }
        goodEntries = spaceEntries;


        return goodEntries;
    }


    /**
     * Finally, this does the real work of extracting data and generating products
     *
     * @param request The request
     * @param entry the entry
     * @param asynch Is this an asynchronous request
     * @param recordEntries List of entries to process
     * @param jobId The job ID
     *
     * @return the result
     * @throws Throwable _more_
     */
    public Result processEntries(Request request, Entry entry,
                                 boolean asynch,
                                 List<? extends RecordEntry> recordEntries,
                                 Object jobId)
	throws Throwable {

        List<PointEntry> pointEntries =
            PointEntry.toPointEntryList(recordEntries);
        //Get the product formats
        HashSet<String> formats = getProductFormats(request);

        //If nothing selected then flake out
        if (formats.size() == 0) {
            return getRepository().makeErrorResult(request,
						   "No product formats were selected");
        }

        //If more than one format is selected and this is a synchronous call then raise an error
        if ((formats.size() > 1) && !asynch) {
            return getRepository().makeErrorResult(
						   request,
						   "Cannot have more than one product format selected when doing an aysnchronous request");
        }


        List<RecordVisitor> visitors = new ArrayList<RecordVisitor>();
        Result              result   = null;
        JobInfo             info     = null;
        if (jobId != null) {
            info = getRecordJobManager().getJobInfo(jobId);
        }
        if (info == null) {
            info = new JobInfo(JOB_TYPE_POINT);
        }
        final JobInfo theJobInfo = info;

        try {
            result = createVisitors(request, entry, asynch, pointEntries,
                                    theJobInfo, formats, visitors);
            if (result != null) {
                return result;
            }

            if ( !jobOK(jobId)) {
                return result;
            }

            //Run through the visitors
            if (visitors.size() > 0) {
                RecordVisitorGroup groupVisitor =
                    new RecordVisitorGroup(visitors) {
			public boolean visitRecord(RecordFile file,
						   VisitInfo visitInfo, BaseRecord record)
                            throws Exception {
			    if ( !super.visitRecord(file, visitInfo, record)) {
				return false;
			    }
			    if (getCount() == 1) {
				theJobInfo.setCurrentStatus("Reading points...");
			    } else if ((getCount() % 100000) == 0) {
				theJobInfo.setCurrentStatus("Read "
							    + (getCount() / 1000) + "K points");
			    }

			    return true;
			}

		    };
                info.setCurrentStatus("Staging request...");

                memoryCheck("POINT: memory before:");

                VisitInfo visitInfo = new VisitInfo(VisitInfo.QUICKSCAN_NO);
                if (request.defined("startdate")) {
                    Date dttm = Utils.parseRelativeDate(new Date(),
							request.getString("startdate", ""), 0);
                    visitInfo.setStartDate(dttm);
                } else if (request.defined("date_fromdate")) {
                    Date dttm = Utils.parseRelativeDate(new Date(),
							request.getString("date_fromdate", ""), 0);
                    visitInfo.setStartDate(dttm);
                }
                if (request.defined("enddate")) {
                    visitInfo.setEndDate(Utils.parseRelativeDate(new Date(),
								 request.getString("enddate", ""), 0));
                } else  if (request.defined("date_todate")) {
                    visitInfo.setEndDate(Utils.parseRelativeDate(new Date(),
								 request.getString("date_todate", ""), 0));
                }
		if(visitInfo.getStartDate() !=null ||  visitInfo.getEndDate()!=null) {
		    request.remove(ARG_RECORD_LAST);
		    request.remove(ARG_MAX);
		}
				   
		int max = -1;
                if (request.defined(ARG_LIMIT)) {
                    max = request.get(ARG_LIMIT, 5000);
		    //If we have a limit and a record.last and then override the record.last with the limit 
		    if (request.defined(ARG_RECORD_LAST)) {
			request.put(ARG_RECORD_LAST,""+max);
		    }
                } else if (request.defined(ARG_MAX)) {
                    max= request.get(ARG_MAX, 5000);
		}
		if(max>=0) visitInfo.setMax(max);
                if (request.defined(ARG_RECORD_LAST)) {
		    int last = request.get(ARG_RECORD_LAST, -1);
		    if(last>0) {
			visitInfo.setLast(last);
			//If there wasn't a max set then set it to something larger than the last count in
			//in case the caching of the count is out of date
			if (max<0) {
			    System.err.println("SETTING MAX:" + (last+100));
			    visitInfo.setMax(last+100);
			}
		    }
                }		

                if (request.defined(ARG_SKIP)) {
                    visitInfo.setStart(request.get(ARG_SKIP, 0));
                } 
		if (request.defined(ARG_STRIDE)) {
                    visitInfo.setSkip(request.get(ARG_STRIDE, 0));
                }


		//		System.err.println("date:" + visitInfo.getStartDate() +" " + visitInfo.getEndDate() +" " + visitInfo);

                getRecordJobManager().visitSequential(request, pointEntries,
						      groupVisitor, visitInfo);

                if ( !jobOK(jobId)) {
                    return result;
                }
                info.addStatusItem("Point reading complete");
                info.setNumPoints(groupVisitor.getCount());
                info.setCurrentStatus("Processing products...");

                for (RecordVisitor visitor : visitors) {
                    if (visitor instanceof GridVisitor) {
                        GridVisitor gridVisitor = (GridVisitor) visitor;
                        gridVisitor.finishedWithAllFiles();
                        info.addStatusItem("Generating gridded products");
                        IdwGrid llg = gridVisitor.getGrid();
                        pointEntries.get(0).setBounds(llg.getBounds());
                        outputEntryGrid(request, entry, llg, formats, jobId);
                        memoryCheck("POINT: memory after grid:");
                    }
                }
                for (RecordVisitor visitor : visitors) {
                    if (visitor instanceof BarnesVisitor) {
                        BarnesVisitor barnesVisitor = (BarnesVisitor) visitor;
                        barnesVisitor.finishedWithAllFiles();
                        IdwGrid llg = barnesVisitor.getGrid();
                        pointEntries.get(0).setBounds(llg.getBounds());
                        info.addStatusItem("Generating gridded products");
                        outputEntryGrid(request, entry,
                                        barnesVisitor.getGrid(), formats,
                                        jobId);
                    }
                }
                info.addStatusItem("Product processing complete");
                visitors     = null;
                groupVisitor = null;
                pointEntries = null;
                memoryCheck("POINT: memory after visit:");
            }
            if (request.responseAsXml()) {
                return new Result(XmlUtil.tag(TAG_RESPONSE,
					      XmlUtil.attr(ATTR_CODE, CODE_OK), "OK"), MIME_XML);
            }

            return getDummyResult();
        } catch (Exception exc) {
	    if(!asynch) throw exc;
	    System.err.println("error:" + exc);
	    exc.printStackTrace();
            Throwable inner = LogUtil.getInnerException(exc);
            try {
                getRecordJobManager().setError(info, inner.toString());
            } catch (Exception noop) {}
            //            getLogManager().logError("processing point request", inner);
            //Special handling for json requests
            if ((formats.size() == 1)
		&& formats.contains(OUTPUT_JSON.getId())) {
                String message = "Error: " + inner.getMessage();
                inner.printStackTrace();
                String       code = "error";
                StringBuffer json = new StringBuffer();
                json.append(JsonUtil.map(Utils.makeListFromValues("error", JsonUtil.quote(message),
							"errorcode", JsonUtil.quote(code))));
                Result errorResult = new Result("", json, JsonUtil.MIMETYPE);

                return errorResult;
            }

            return getRepository().makeErrorResult(request,
						   inner.getMessage());
        }
    }


    /**
     * Main entry point for Point Files. This methods handles things like the point map, the lat/lon web services,
     * the product form and product requests.
     *
     * @param request the request
     * @param outputType The type of output
     * @param entry The entry
     *
     * @return the result
     *
     * @throws Exception on badness
     */
    public Result outputEntry(Request request, OutputType outputType,
                              final Entry entry)
	throws Exception {

        Result parentResult = super.outputEntry(request, outputType, entry);
        if (parentResult != null) {
            return parentResult;
        }

        boolean doingPointCount = request.get(ARG_POINTCOUNT, false)
	    || request.getString(ARG_PRODUCT,
				 "").equals(OUTPUT_POINTCOUNT.getId());


        if (doingPointCount) {
            List<PointEntry> pointEntries = new ArrayList<PointEntry>();
            pointEntries.add((PointEntry) doMakeEntry(request, entry));
            long pointCount = getApproximatePointCount(request, pointEntries);

            return makePointCountResult(request, pointCount);

        }


        if (outputType.equals(OUTPUT_BOUNDS)) {
            return outputEntryBounds(request, entry);
        }

        if (outputType.equals(OUTPUT_GETPOINTINDEX)) {
            return outputEntryGetPointIndex(request, entry);
        }

        if (outputType.equals(OUTPUT_GETLATLON)) {
            return outputEntryGetLatLon(request,
                                        (PointEntry) doMakeEntry(request,
								 entry));
        }

        if (outputType.equals(OUTPUT_VIEW)) {
            return getFormHandler().outputEntryView(request, outputType,
						    doMakeEntry(request, entry));
        }

        if (outputType.equals(OUTPUT_METADATA)) {
            return getFormHandler().outputEntryMetadata(request, outputType,
							doMakeEntry(request, entry));
        }

        if (request.defined(ARG_GETDATA)) {
            if ( !request.defined(ARG_PRODUCT)) {
                return getPointFormHandler().outputEntryForm(
							     request, entry,
							     new StringBuffer(
									      getPageHandler().showDialogError(
													       "No products selected")));
            }
        }

        if (outputType.equals(OUTPUT_CHART)) {
            return outputEntryChart(request, outputType, entry);
        }


        if (outputType.equals(OUTPUT_WAVEFORM_CSV)) {
            return getPointFormHandler().outputEntryWaveformCsv(request,
								outputType, (PointEntry) doMakeEntry(request, entry));
        }

        if (outputType.equals(OUTPUT_FORM_CSV)) {
            return getPointFormHandler().outputEntryFormCsv(request, entry);
        }

        if (outputType.equals(OUTPUT_FORM)) {
            return getPointFormHandler().outputEntryForm(request, entry);
        }
	String product = request.getString(ARG_PRODUCT,"");
	if(product.equals(CsvOutputHandler.WHAT_GENERATED_R) ||
	   product.equals(CsvOutputHandler.WHAT_GENERATED_PYTHON) ||
	   product.equals(CsvOutputHandler.WHAT_GENERATED_MATLAB)) {
	    request.put(ARG_PRODUCT,"points.csv");
	    request.put(CsvOutputHandler.ARG_WHAT,product);
	    request.put(ARG_OUTPUT,CsvOutputHandler.OUTPUT_IDS.toString());
	    return getRepository().getCsvOutputHandler().outputEntry(request,
								     CsvOutputHandler.OUTPUT_IDS,
								     entry);
	}

        boolean          asynchronous = request.get(ARG_ASYNCH, false);
        boolean doingPublish = request.defined(ARG_PUBLISH_ENTRY + "_hidden");
        List<PointEntry> pointEntries = new ArrayList<PointEntry>();
        pointEntries.add((PointEntry) doMakeEntry(request, entry));
        if ( !doingPublish && !asynchronous) {
            //Note - normally the POH here is "this" POH but for Lidar types over in the nlasplugin
            //We want to get the LidarOutputHandler
            PointOutputHandler pointOutputHandler =
                pointEntries.get(0).getPointOutputHandler();
            try {
                Result result = pointOutputHandler.processEntries(request,
								  entry, false, pointEntries, null);
                if (result != null) {
                    return result;
                }
            } catch (Throwable thr) {
                Throwable inner = LogUtil.getInnerException(thr);
                if (inner instanceof Exception) {
                    throw (Exception) inner;
                }

                throw new RuntimeException(inner);
            }
            StringBuffer sb = new StringBuffer();
            if ( !outputType.equals(OUTPUT_FORM)) {
                sb.append(
			  getPageHandler().showDialogError(
							   "Unknown output type:" + outputType));
            }

            return getPointFormHandler().outputEntryForm(request, entry);
        }

        return getRecordJobManager().handleAsynchRequest(request, entry,
							 outputType, pointEntries);
    }


    public String getCsvApiUrl(Request request, Entry entry) {
	String url = request.entryUrl(getRepository().URL_ENTRY_SHOW, entry,
				      ARG_OUTPUT,"points.product",
				      ARG_GETDATA,"getdata",
				      ARG_PRODUCT,"points.csv",
				      ARG_ADD_LATLON,request.getString(ARG_ADD_LATLON,"true"));

	String fmt = request.getString(ARG_DATEFORMAT+"_custom",null);
	if(fmt==null)
	    fmt = request.getString(ARG_DATEFORMAT,null);
	if(fmt!=null) {
	    url = HU.url(url,ARG_DATEFORMAT,fmt);
	}
	return url;
    }

    public void getEntryFormCsv(Request request, Entry entry,   Appendable sb)
	throws Exception {
	getPointFormHandler().getEntryFormCsv(request, entry,sb);
    }


    public Result outputEntryChart(Request request, OutputType outputType,
                                   Entry entry)
	throws Exception {
        StringBuilder sb = new StringBuilder();
        getPageHandler().entrySectionOpen(request, entry, sb, "Point Chart");
        sb.append(getWikiManager().getStandardChartDisplay(request, entry));
        getPageHandler().entrySectionClose(request, entry, sb);
        return new Result("", sb);
    }

    public String getJsonUrl(Request request, Entry entry, Hashtable props,
                             List<String> displayProps)
	throws Exception {

        PointTypeHandler typeHandler =
            (PointTypeHandler) entry.getTypeHandler();


        List<RecordTypeHandler.Macro> macros = typeHandler.getMacros(entry);
        if (macros != null) {
            String all = null;
            for (RecordTypeHandler.Macro macro : macros) {
                if (all != null) {
                    all += ",";
                } else {
                    all = "";
                }
                all += macro.name;
                displayProps.add("request." + macro.name + ".type");
                displayProps.add(JsonUtil.quote(macro.type));
                if (macro.dflt != null) {
                    displayProps.add("request." + macro.name + ".default");
                    displayProps.add(JsonUtil.quote(macro.dflt));
                }
                displayProps.add("request." + macro.name + ".label");
                displayProps.add(JsonUtil.quote(macro.label));
                if (macro.multiple) {
                    displayProps.add("request." + macro.name + ".multiple");
                    displayProps.add("true");
                }
                if (macro.delimiter != null) {
                    displayProps.add("request." + macro.name + ".delimiter");
                    displayProps.add(JsonUtil.quote(macro.delimiter));
                }
                if (macro.template != null) {
                    displayProps.add("request." + macro.name + ".template");
                    displayProps.add(JsonUtil.quote(macro.template));
                }
                if (macro.multitemplate != null) {
                    displayProps.add("request." + macro.name
                                     + ".multitemplate");
                    displayProps.add(JsonUtil.quote(macro.multitemplate));
                }
                if (macro.nonetemplate != null) {
                    displayProps.add("request." + macro.name
                                     + ".nonetemplate");
                    displayProps.add(JsonUtil.quote(macro.nonetemplate));
                }
                if (macro.rows != null) {
                    displayProps.add("request." + macro.name + ".rows");
                    displayProps.add(JsonUtil.quote(macro.rows));
                }
                displayProps.add("request." + macro.name + ".values");
                displayProps.add(JsonUtil.quote(macro.values));
                displayProps.add("request." + macro.name + ".urlarg");
                displayProps.add(JsonUtil.quote("request." + macro.name));
            }
            displayProps.add("requestFields");
            displayProps.add(JsonUtil.quote(all));
        }

        String extra     = "";
        String extraArgs = (props == null)
	    ? null
	    : (String) props.get("extraArgs");
        if (extraArgs != null) {
            for (String tuple :
		     StringUtil.split(extraArgs, ",", true, true)) {
                List<String> toks  = StringUtil.splitUpTo(tuple, ":", 2);
                String       arg   = toks.get(0);
                String       value = ((toks.size() > 1)
                                      ? toks.get(1)
                                      : "");
                extra += "&" + HtmlUtils.arg(arg, value);
            }
        }


        String path = typeHandler.getResourcePath(request, entry);
        if ((path != null) && (path.indexOf("${latitude}") >= 0)) {
            extra += "&latitude=${latitude}&longitude=${longitude}";
        } else if(entry.getTypeHandler().getTypeProperty("data.url.addlaton",false)) {
            extra += "&latitude=${latitude}&longitude=${longitude}";
	}



        if (props != null) {
	    int last = Utils.getProperty(props,"lastRecords",-1);
            if (last >=0) {
                extra += "&"
		    + HU.arg(RecordFormHandler.ARG_RECORD_LAST, last+"");
		//if no max set then use the last value
		if(props.get(ARG_MAX)==null)
		    props.put(ARG_MAX,""+(last+100));
            }	    

	    String max =  Utils.getProperty(props,ARG_MAX,"5000");
	    extra += "&" + HU.arg(RecordFormHandler.ARG_MAX, max);

            String skip = (String) props.get("skip");
            if (skip != null) {
                extra += "&"
		    + HU.arg(RecordFormHandler.ARG_RECORD_SKIP, skip);
            }

            String startDate = (String) props.get("request.startdate");
            if (startDate != null) {
                extra += "&" + HU.arg("startdate", startDate);
            }

            String endDate = (String) props.get("request.enddate");
            if (endDate != null) {
                extra += "&" + HU.arg("enddate", endDate);
            }
        }



        String url = request.entryUrl(getRepository().URL_ENTRY_DATA, entry)
	    + extra;

        return url;
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param pointCount _more_
     *
     * @return _more_
     */
    private Result makePointCountResult(Request request, long pointCount) {
        if (request.responseAsXml()) {
            return getRepository().makeOkResult(request,
						"<pointcount>" + pointCount + "</pointcount>");
        }
        if (request.responseAsText()) {
            return getRepository().makeOkResult(request, "" + pointCount);
        }

        return getRepository().makeOkResult(request,
                                            "Estimated point count:"
                                            + pointCount);
    }




    /**
     * Main entry point for Collections. This shows the form for the collection or the job processing
     * state or dispatches the product request. If doing  the product request it either handles it synchronously
     * or asynchronously. If asynch then it creates a job id and redirects to a web page that shows the job status.
     * If synchronously then whatever single product file is requested is returned on the request.
     *
     * This spatially subsets at the file level to figure out  children  files it should include.
     * If the request is to process data then it then does a rough estimate of the number of points
     * in the request and will return an error if too many.
     *
     * @param request the request
     * @param outputType output type
     * @param group The group
     * @return The result
     *
     * @throws Exception on badness
     */
    @Override
    public Result outputGroup(final Request request,
                              final OutputType outputType, final Entry group,
                              final List<Entry> children)
	throws Exception {

        if (group.getTypeHandler() instanceof PointTypeHandler) {
            return outputEntry(request, outputType, group);
        }



        Result parentResult = super.outputGroup(request, outputType, group,
						children);
        if (parentResult != null) {
            return parentResult;
        }

        if (outputType.equals(OUTPUT_BOUNDS)) {
            return outputEntryBounds(request, group);
        }

	List<Entry> entries = new ArrayList<Entry>();
        for (Entry entry : children) {
            if (entry.getTypeHandler().isType("type_point")) {
                entries.add(entry);
            }
        }
        if (group.getTypeHandler().isType("type_point")) {
            entries.clear();
            entries.add(group);
        }


        boolean doingPointCount = request.get(ARG_POINTCOUNT, false)
	    || request.getString(ARG_PRODUCT,
				 "").equals(OUTPUT_POINTCOUNT.getId());
        //If its a getdata request then check if a product type (e.g., csv) has been selected
        if ( !doingPointCount && request.defined(ARG_GETDATA)) {
            if ( !request.defined(ARG_PRODUCT)) {
                return getPointFormHandler().outputGroupForm(
							     request, group, children,
							     new StringBuffer(
									      getPageHandler().showDialogError(
													       "No products selected")));
            }
        }


        if (outputType.equals(OUTPUT_FORM)) {
            return getPointFormHandler().outputGroupForm(request, group,
							 children, new StringBuffer());
        }

        //        System.err.println("entries:" + children);
        //        System.err.println("record entries:" + makeRecordEntries(request, children, true));
        final List<PointEntry> pointEntries =
            PointEntry.toPointEntryList(doSubsetEntries(request,
							makeRecordEntries(request, entries, true)));

        boolean asynchronous = request.get(ARG_ASYNCH, false);
        if ( !doingPointCount && (pointEntries.size() == 0) && asynchronous) {
            return getRepository().makeErrorResult(request,
						   "No entries found that matched the criteria");
        }

        long pointCount = getApproximatePointCount(request, pointEntries);

        if (doingPointCount) {
            return makePointCountResult(request, pointCount);
        }


        //Check if they've exceeded the threshold
        boolean tooManyPoints = false;
        if (request.getUser().getAnonymous()) {
            if (pointCount > POINT_LIMIT_ANONYMOUS) {
                tooManyPoints = true;
            }
        } else {
            if (pointCount > POINT_LIMIT_USER) {
                tooManyPoints = true;
            }
        }


        if (tooManyPoints) {
            if (request.responseAsXml()) {
                return getRepository().makeErrorResult(request,
						       "Too many points selected:" + pointCount);
            }
            StringBuffer sb = new StringBuffer(
					       getPageHandler().showDialogError(
										"Too many points selected: "
										+ pointCount));

            return getPointFormHandler().outputGroupForm(request, group,
							 children, sb);
        }


        boolean doingPublish = doingPublish(request);

        //If its synchronous
        if ( !doingPublish && !asynchronous) {
            //Note - normally the POH here is "this" POH but for Lidar types over in the nlasplugin
            //We want to get the LidarOutputHandler
            PointOutputHandler pointOutputHandler =
                pointEntries.get(0).getPointOutputHandler();
            try {
                Result result = pointOutputHandler.processEntries(request,
								  group, false, pointEntries, null);
                if (result != null) {
                    return result;
                }
            } catch (Throwable thr) {
                Throwable inner = LogUtil.getInnerException(thr);
                if (inner instanceof Exception) {
                    throw (Exception) inner;
                }

                throw new RuntimeException(inner);
            }
            StringBuffer sb = new StringBuffer();
            if ( !outputType.equals(OUTPUT_FORM)) {
                sb.append(
			  getPageHandler().showDialogError(
							   "Unknown output type:" + outputType));
            }

            return getPointFormHandler().outputGroupForm(request, group,
							 children, sb);
        }

        if ( !request.defined(ARG_PRODUCT)) {
            return getRepository().makeErrorResult(request,
						   "No product formats were selected");
        }

        return getRecordJobManager().handleAsynchRequest(request, group,
							 outputType, pointEntries);


    }

    /**
     *
     * @param id _more_
     * @return _more_
     */
    private String getIdvField(String id,HashSet<String> seen) {
        id = id.trim();
        id = id.replaceAll("\\.", "_");
        if (id.matches("^[0-9]+.*")) {
            id = "var_" + id;
        }

	int cnt = 1;
	String tmp = id;
	while(seen.contains(tmp)) {
	    tmp = id+(cnt++);
	}
	id = tmp;
	seen.add(id);
	//	System.err.println("ID:" + id);
        return id;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param asynch _more_
     * @param pointEntries _more_
     * @param jobInfo _more_
     * @param formats _more_
     * @param visitors _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result createVisitors(Request request, Entry entry,
                                 boolean asynch,
                                 List<? extends PointEntry> pointEntries,
                                 JobInfo jobInfo, HashSet<String> formats,
                                 List<RecordVisitor> visitors)
	throws Exception {


	String returnFileName = entry.getName();
        Result result = null;
        //Make a RecordVisitor for each point product type
        if (formats.contains(OUTPUT_CSV.getId())) {
	    request.setMimeType(IO.MIME_CSV);
	    if(!asynch) request.setReturnFilename(returnFileName+".csv");
            visitors.add(makeCsvVisitor(request, entry, pointEntries, null,
                                        null, jobInfo.getJobId()));
        }
        if (formats.contains(OUTPUT_NC.getId())) {
	    if(!asynch) request.setReturnFilename(returnFileName+".nc");
            visitors.add(makeNetcdfVisitor(request, entry, pointEntries,
                                           jobInfo.getJobId()));
        }



        if (formats.contains(OUTPUT_IDVCSV.getId())) {
	    if(!asynch) request.setReturnFilename(returnFileName+".csv");
            final boolean[] haveLat = { false };

            CsvVisitor.HeaderPrinter headerPrinter =
                new CsvVisitor.HeaderPrinter() {
		    public void call(CsvVisitor visitor, PrintWriter pw,
				     List<RecordField> fields) {
			HashSet<String> seen = new HashSet<String>();
			StringBuilder sb  = new StringBuilder("(index) -> (");
			int           cnt = 0;
			for (RecordField field : fields) {
			    if (field.getName().toLowerCase().equals(
								     "latitude")) {
				haveLat[0] = true;
				break;
			    }
			}

			for (RecordField field : fields) {
			    if (cnt++ > 0) {
				sb.append(",");
			    }
			    String id = getIdvField(field.getName(),seen);
			    if (field.isTypeDate()) {
				sb.append("Time");
			    } else if (field.isTypeString()) {
				sb.append(id + "(Text)");
			    } else {
				sb.append(id);
			    }
			}
			if ( !haveLat[0]) {
			    sb.append(",latitude,longitude");
			}
			sb.append(")\n");
			cnt = 0;
			seen = new HashSet<String>();
			for (RecordField field : fields) {
			    if (cnt++ > 0) {
				sb.append(",");
			    }
			    String id = getIdvField(field.getName(),seen);
			    if (field.isTypeDate()) {
				sb.append(
					  "Time[fmt=\"yyyy-MM-dd'T'HH:mm:ssZ\" ]");
			    } else if (field.isTypeString()) {
				sb.append(id + "(Text)");
			    } else {
				sb.append(id + "[");
				if (Utils.stringDefined(field.getUnit())) {
				    sb.append(" unit=\"" + field.getUnit()
					      + "\" ");
				}
				if (field.isTypeDate()) {
				    sb.append(" fmt=\"yyyy-MM-dd'T'HH:mm:ssZ\" ");
				}
				sb.append("]");
			    }
			}

			if ( !haveLat[0]) {
			    sb.append(",latitude[],longitude[]");
			}
			//                      Time[fmt="yyyy-MM-dd HH:mm:ss"],Latitude[unit="deg"],Longitude[unit="degrees west"],T[unit="celsius"],skip,DIR[unit="deg"],SPD[unit="m/s"]
			sb.append("\n");
			pw.print(sb);
		    }
		};
            CsvVisitor.LineEnder lineEnder = new CsvVisitor.LineEnder() {
		    public void call(CsvVisitor visitor, PrintWriter pw,
				     List<RecordField> fields, BaseRecord record,
				     int cnt) {
			if ( !haveLat[0]) {
			    pw.print(",NaN,NaN");
			}

		    }
		};
            visitors.add(makeCsvVisitor(request, entry, pointEntries,
                                        headerPrinter, lineEnder,
                                        jobInfo.getJobId()));
        }
        if (formats.contains(OUTPUT_JSON.getId())) {
	    if(!asynch) request.setReturnFilename(returnFileName+".json");
            if ( !asynch) {
                String tail = IOUtil.stripExtension(entry.getName());
                request.setReturnFilename(tail + ".json");
                request.getHttpServletResponse().setContentType(
								JsonUtil.MIMETYPE);
                request.setCORSHeaderOnResponse();
            }
            visitors.add(makeJsonVisitor(request, entry, pointEntries,
                                         jobInfo.getJobId()));
        }


        if (request.get(ARG_GRID_BARNES, false)) {
            visitors.add(makeBarnesVisitor(request, pointEntries,
                                           getBounds(request, pointEntries)));
        }
        if (formats.contains(OUTPUT_LATLONALTCSV.getId())) {
	    if(!asynch) request.setReturnFilename(returnFileName+".csv");
            visitors.add(makeLatLonAltCsvVisitor(request, entry,
						 pointEntries, jobInfo.getJobId()));
        }
        if (formats.contains(OUTPUT_LATLONALTBIN.getId())) {
            visitors.add(makeLatLonAltBinVisitor(request, entry,
						 pointEntries, jobInfo.getJobId(), null, true));
        }


        if (formats.contains(OUTPUT_NC.getId())) {
            //            result = outputEntryNc(request, entry,  pointEntries,
            //                                    jobInfo.getJobId());
        }

        //Tracks just do them
        if (formats.contains(OUTPUT_KML_TRACK.getId())) {
	    if(!asynch) request.setReturnFilename(returnFileName+".kml");
            result = outputEntryKmlTrack(request, entry, pointEntries,
                                         jobInfo.getJobId());
        }

        //TODO: Subset we just do directly
        //We need to do a visitor based approach
        if (formats.contains(OUTPUT_SUBSET.getId())) {
            //This is the subset to the original format
            jobInfo.setCurrentStatus("Creating Point file...");
            result = outputEntrySubset(request, entry, pointEntries, jobInfo);
            jobInfo.addStatusItem("Point file created");
            if ( !jobOK(jobInfo.getJobId())) {
                return result;
            }
        }


        //Check if we need to make a grid
        if (anyGriddedFormats(formats)) {
	    System.err.println("Add grid");
            visitors.add(makeGridVisitor(request, pointEntries,
                                         getBounds(request, pointEntries)));
        }

        return null;
    }


    /**
     * _more_
     *
     * @param formats _more_
     *
     * @return _more_
     */
    public boolean anyGriddedFormats(HashSet<String> formats) {
        if (formats.contains(OUTPUT_ASC.getId())
	    || formats.contains(OUTPUT_IMAGE.getId())
	    || formats.contains(OUTPUT_KMZ.getId())
	    || formats.contains(OUTPUT_HILLSHADE.getId())) {
            return true;

        }

        return false;
    }


    /**
     * Make a record visitor that creates a CSV file
     *
     * @param request the request
     * @param mainEntry Either the Point Collection or File Entry
     * @param pointEntries entries to process
     * @param headerPrinter _more_
     * @param lineEnder _more_
     * @param jobId The job ID
     *
     * @return visitor
     *
     * @throws Exception on badness
     */
    public RecordVisitor makeCsvVisitor(
					final Request request, final Entry mainEntry,
					List<? extends PointEntry> pointEntries,
					final CsvVisitor.HeaderPrinter headerPrinter,
					final CsvVisitor.LineEnder lineEnder, final Object jobId)
	throws Exception {

        RecordVisitor visitor = new BridgeRecordVisitor(this, request, jobId,
							mainEntry, ".csv") {
		private CsvVisitor csvVisitor = null;
		int                cnt        = 0;
		public boolean doVisitRecord(RecordFile file,
					     VisitInfo visitInfo,
					     BaseRecord record)
                    throws Exception {
		    if (csvVisitor == null) {
			//Set the georeference flag
			if (request.get(ARG_GEOREFERENCE, false)) {
			    visitInfo.putProperty("georeference", Boolean.TRUE);
			}
			String url = request.getAbsoluteUrl(
							    request.makeUrl(
									    repository.URL_ENTRY_SHOW,
									    ARG_ENTRYID, mainEntry.getId()));
			visitInfo.putProperty(CsvVisitor.PROP_SOURCE, url);
			csvVisitor = new CsvVisitor(getThePrintWriter(),
						    getFields(request, record.getFields()),
						    headerPrinter, lineEnder);
			if(request.get(ARG_ADD_LATLON,false)) {
			    csvVisitor.setExtraHeader("latitude,longitude");
			    csvVisitor.setExtraLine(mainEntry.getLatitude(request)+","+ mainEntry.getLongitude(request));
			}


			if (request.defined(ARG_HEADER)) {
			    csvVisitor.setAltHeader(request.getString(ARG_HEADER, ""));
			}
			if (request.get("fullheader", false)) {
			    csvVisitor.setFullHeader(true);
			}
		    }
		    if ( !jobOK(jobId)) {
			return false;
		    }
		    synchronized (visitInfo) {
			if (visitInfo.getCount() == 0) {
			    visitInfo.putProperty(BaseRecord.PROP_INCLUDEVECTOR,
						  Boolean.valueOf(request.get(ARG_INCLUDEWAVEFORM,
									      false)));
			}
			try {
			    csvVisitor.visitRecord(file, visitInfo, record);
			} catch (Exception exc) {
			    throw exc;
			}
		    }

		    return true;
		}
	    };

        return visitor;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param mainEntry _more_
     * @param pointEntries _more_
     * @param jobId _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public RecordVisitor makeJsonVisitor(
					 final Request request, final Entry mainEntry,
					 List<? extends PointEntry> pointEntries, final Object jobId)
	throws Exception {
        return new JsonVisitor(this, request, jobId, mainEntry, ".json");
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param mainEntry _more_
     * @param pointEntries _more_
     * @param jobId _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public RecordVisitor makeNetcdfVisitor(
					   final Request request, Entry mainEntry,
					   List<? extends PointEntry> pointEntries, final Object jobId)
	throws Exception {
        RecordVisitor visitor = new BridgeRecordVisitor(this, request, jobId, mainEntry, ".nc") {
		private NetcdfVisitor visitor = null;
		public boolean doVisitRecord(RecordFile file,
					     VisitInfo visitInfo,
					     BaseRecord record)
		    throws Exception {
		    if (visitor == null) {
			File tmpFile = getHandler().getStorageManager().getTmpFile("tmp.nc");
			visitor = new NetcdfVisitor(tmpFile,getTheDataOutputStream(),getFields(request, record.getFields()));
		    }
		    if ( !jobOK(jobId)) {
			return false;
		    }
		    synchronized (visitInfo) {
			visitor.visitRecord(file, visitInfo, record);
		    }
		    return true;
		}
		public void close(VisitInfo visitInfo) {
		    if(visitor!=null) visitor.close(visitInfo);
		    else {
			super.close(visitInfo);
		    }
		}
	    };
        return visitor;
	//        return new NetcdfVisitor(this, request, jobId, mainEntry);
    }




    /**
     * _more_
     *
     * @param request The request
     * @param recordEntries _more_
     * @param bounds _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public GridVisitor makeGridVisitor(
				       Request request, List<? extends PointEntry> recordEntries,
				       Rectangle2D.Double bounds)
	throws Exception {
        int imageWidth  = request.get(ARG_WIDTH, DFLT_WIDTH);
        int imageHeight = request.get(ARG_HEIGHT, DFLT_HEIGHT);

        if ((imageWidth > 2500) || (imageHeight > 2500)) {
            throw new IllegalArgumentException("Too large image dimension: "
					       + imageWidth + " X " + imageHeight);
        }
        //        System.err.println("Grid BOUNDS: " + bounds);

        IdwGrid llg = new IdwGrid(imageWidth, imageHeight, bounds.y,
                                  bounds.x, bounds.y + bounds.height,
                                  bounds.x + bounds.width);

        if (request.defined(RecordConstants.ARG_GRID_POWER)) {
            llg.setPower(request.get(RecordConstants.ARG_GRID_POWER, 1.0));
        }
        if (request.defined(RecordConstants.ARG_GRID_MINPOINTS)) {
            llg.setMinPoints(request.get(RecordConstants.ARG_GRID_MINPOINTS,
                                         1));
        }
        //llg.fillValue(Double.NaN);
        //If nothing specified then default to 2 grid cells radius
        if ( !request.defined(ARG_GRID_RADIUS_DEGREES)
	     && !request.defined(ARG_GRID_RADIUS_CELLS)) {
            llg.setRadius(0.0);
            llg.setNumCells(2);
        } else {
            if (request.defined(ARG_GRID_RADIUS_CELLS)) {
                llg.setNumCells(request.get(ARG_GRID_RADIUS_CELLS, 0));
            } else {
                //If the user did not change the degrees radius then get the default radius from the bounds
                if (request.getString(ARG_GRID_RADIUS_DEGREES, "").equals(
									  request.getString(
											    ARG_GRID_RADIUS_DEGREES_ORIG, ""))) {
                    llg.setRadius(
				  getFormHandler().getDefaultRadiusDegrees(
									   request, bounds));
                } else {
                    llg.setRadius(request.get(ARG_GRID_RADIUS_DEGREES, 0.0));
                }
            }


        }
        if (llg.getCellIndexDelta() > 100) {
            System.err.println("POINT: bad grid neighborhood size: "
                               + llg.getCellIndexDelta());
            System.err.println("POINT: llg: " + llg);

            throw new IllegalArgumentException("bad grid neighborhood size: "
					       + llg.getCellIndexDelta());
        }

        GridVisitor visitor = new GridVisitor(this, request, llg);

        return visitor;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param recordEntries _more_
     * @param bounds _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public BarnesVisitor makeBarnesVisitor(
					   Request request, List<? extends PointEntry> recordEntries,
					   Rectangle2D.Double bounds)
	throws Exception {
        int imageWidth  = request.get(ARG_WIDTH, DFLT_WIDTH);
        int imageHeight = request.get(ARG_HEIGHT, DFLT_HEIGHT);

        if ((imageWidth > 2500) || (imageHeight > 2500)) {
            throw new IllegalArgumentException("Too large image dimension: "
					       + imageWidth + " X " + imageHeight);
        }
        //        System.err.println("Grid BOUNDS: " + bounds);

        return new BarnesVisitor(this, request, imageWidth, imageHeight,
                                 bounds);
    }





    /**
     * make the visitor for latlonalt binary formats
     *
     * @param request the request
     * @param mainEntry Either the Point Collection or File Entry
     * @param pointEntries entries to process
     * @param jobId The job ID
     * @param inputDos _more_
     * @param doDouble _more_
     *
     * @return the visitor
     *
     * @throws Exception on badness
     */
    public RecordVisitor makeLatLonAltBinVisitor(Request request,
						 Entry mainEntry, List<? extends PointEntry> pointEntries,
						 final Object jobId, final DataOutputStream inputDos,
						 final boolean doDouble)
	throws Exception {

        final int[]   cnt     = { 0 };

        RecordVisitor visitor = new BridgeRecordVisitor(this, jobId) {
		@Override
		public boolean doVisitRecord(RecordFile file,
					     VisitInfo visitInfo,
					     BaseRecord record) {
		    try {
			if ( !jobOK(jobId)) {
			    return false;
			}
			GeoRecord geoRecord = (GeoRecord) record;
			synchronized (MUTEX) {
			    DataOutputStream dos = inputDos;
			    if (dos == null) {
				dos = getTheDataOutputStream();
			    }
			    //                        if(cnt[0]++<100) {
			    //                            System.err.println("double:" + geoRecord.getLatitude() + " float:" + ((float)geoRecord.getLatitude()));
			    //                        }
			    //FIX
			    if (doDouble) {
				dos.writeDouble(geoRecord.getLatitude());
				dos.writeDouble(geoRecord.getLongitude());
				dos.writeDouble(geoRecord.getAltitude());
			    } else {
				dos.writeFloat((float) geoRecord.getLatitude());
				dos.writeFloat((float) geoRecord.getLongitude());
				dos.writeFloat((float) geoRecord.getAltitude());
			    }
			}

			return true;
		    } catch (Exception exc) {
			throw new RuntimeException(exc);
		    }
		}
		public String toString() {
		    return "LatLonAltBin visitor";
		}
	    };

        return visitor;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param mainEntry _more_
     * @param pointEntries _more_
     * @param jobId _more_
     * @param inputDos _more_
     * @param doDouble _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public RecordVisitor makeLatLonBinVisitor(Request request,
					      Entry mainEntry, List<? extends PointEntry> pointEntries,
					      final Object jobId, final DataOutputStream inputDos,
					      final boolean doDouble)
	throws Exception {

        final int[]   cnt     = { 0 };

        RecordVisitor visitor = new BridgeRecordVisitor(this, jobId) {
		@Override
		public boolean doVisitRecord(RecordFile file,
					     VisitInfo visitInfo,
					     BaseRecord record) {
		    try {
			if ( !jobOK(jobId)) {
			    return false;
			}
			GeoRecord geoRecord = (GeoRecord) record;
			synchronized (MUTEX) {
			    DataOutputStream dos = inputDos;
			    if (dos == null) {
				dos = getTheDataOutputStream();
			    }
			    //                        if(cnt[0]++<100) {
			    //                            System.err.println("double:" + geoRecord.getLatitude() + " float:" + ((float)geoRecord.getLatitude()));
			    //                        }
			    //FIX
			    if (doDouble) {
				dos.writeDouble(geoRecord.getLatitude());
				dos.writeDouble(geoRecord.getLongitude());
			    } else {
				dos.writeFloat((float) geoRecord.getLatitude());
				dos.writeFloat((float) geoRecord.getLongitude());
			    }
			}

			return true;
		    } catch (Exception exc) {
			throw new RuntimeException(exc);
		    }
		}
		public String toString() {
		    return "LatLonAltBin visitor";
		}
	    };

        return visitor;
    }






    /**
     * Make a record visitor that creates a CSV file
     *
     * @param request the request
     * @param mainEntry Either the  Collection or File Entry
     * @param entries entries to process
     * @param jobId The job ID
     *
     * @return visitor
     *
     * @throws Exception on badness
     */
    public RecordVisitor makeLatLonAltCsvVisitor(Request request,
						 Entry mainEntry, List<? extends PointEntry> entries,
						 final Object jobId)
	throws Exception {
        RecordVisitor visitor = new BridgeRecordVisitor(this, request, jobId,
							mainEntry, "latlonalt.csv") {
		public boolean doVisitRecord(RecordFile file,
					     VisitInfo visitInfo,
					     BaseRecord record)
                    throws Exception {
		    if ( !jobOK(jobId)) {
			return false;
		    }
		    StringBuffer buffer      = getBuffer(file);
		    PointRecord  pointRecord = (PointRecord) record;
		    float[]      altitudes   = pointRecord.getAltitudes();
		    if (altitudes != null) {
			for (int i = 0; i < altitudes.length; i++) {
			    buffer.append(pointRecord.getLatitude());
			    buffer.append(',');
			    buffer.append(pointRecord.getLongitude());
			    buffer.append(',');
			    buffer.append(altitudes[i]);
			    buffer.append("\n");
			}
		    } else {
			((PointRecord) record).printLatLonAltCsv(visitInfo,
								 buffer);
		    }
		    if (buffer.length() > 100000) {
			write(buffer);
		    }

		    return true;
		}
		private void write(StringBuffer buffer) {
		    synchronized (MUTEX) {
			try {
			    byte[] bytes = buffer.toString().getBytes();
			    getTheOutputStream().write(bytes, 0, bytes.length);
			    buffer.setLength(0);
			} catch (Exception exc) {
			    throw new RuntimeException(exc);
			}
		    }
		}

		public void finished(RecordFile file, VisitInfo visitInfo)
                    throws Exception {
		    write(getBuffer(file));
		    super.finished(file, visitInfo);
		}
	    };

        return visitor;
    }







    /**
     * _more_
     *
     * @param request the request
     * @param mainEntry Either the Point Collection or File Entry
     * @param llg latlongrid
     * @param formats _more_
     * @param jobId The job ID
     *
     * @return _more_
     *
     * @throws Exception on badness
     */
    public Result outputEntryGrid(Request request, Entry mainEntry,
                                  IdwGrid llg, HashSet<String> formats,
                                  Object jobId)
	throws Exception {

        boolean doKmz          = formats.contains(OUTPUT_KMZ.getId());
        boolean doImage        = formats.contains(OUTPUT_IMAGE.getId());
        boolean doHillshade    = formats.contains(OUTPUT_HILLSHADE.getId());
        boolean doAsc          = formats.contains(OUTPUT_ASC.getId());
        boolean forceHillshade = false;

        if (doKmz && !doImage && !doHillshade) {
            forceHillshade = true;
        }

        Element         root   = null;
        Element         folder = null;
        String          desc   = mainEntry.getDescription();
        ZipOutputStream zos    = null;
        if (doKmz) {
            zos = new ZipOutputStream(getOutputStream(request, jobId,
						      mainEntry, ".kmz"));
            root   = KmlUtil.kml(mainEntry.getName());
            folder = KmlUtil.folder(root, mainEntry.getName(), true);
            if (desc.length() > 0) {
                KmlUtil.description(folder, desc);
            }

            /*            String trackUrl = request.getAbsoluteUrl(
			  request.entryUrl(
			  getRepository().URL_ENTRY_SHOW,
			  mainEntry, new String[] { ARG_OUTPUT,
			  OUTPUT_KML_TRACK.toString() }));
			  Element trackNode = KmlUtil.networkLink(folder, "Track",
			  trackUrl);
			  KmlUtil.open(trackNode, false);
			  KmlUtil.visible(trackNode, false);
            */
        }


        int     imageWidth      = llg.getWidth();
        int     imageHeight     = llg.getHeight();


        boolean anyGridsDefined = false;
        for (int i = 0; i < GRID_ARGS.length; i++) {
            if (request.get(GRID_ARGS[i], false)) {
                anyGridsDefined = true;

                break;
            }
        }

        if ( !anyGridsDefined) {
            request.put(ARG_GRID_IDW, "true");
        }
        for (int i = 0; i < GRID_ARGS.length; i++) {
            String whatGrid = GRID_ARGS[i];
            if ( !request.get(whatGrid, false)) {
                continue;
            }
            double     missingValue    = Double.NaN;
            double[][] grid            = null;
            boolean    isAltitudeValue = true;
            if (whatGrid.equals(ARG_GRID_MIN)) {
                grid = llg.getMinGrid();
            } else if (whatGrid.equals(ARG_GRID_MAX)) {
                grid = llg.getMaxGrid();
            } else if (whatGrid.equals(ARG_GRID_BARNES)) {
                grid = llg.getValueGrid();
            } else if (whatGrid.equals(ARG_GRID_AVERAGE)) {
                grid = llg.getAverageGrid();
            } else if (whatGrid.equals(ARG_GRID_SUM)) {
                grid = llg.getValueGrid();
            } else if (whatGrid.equals(ARG_GRID_COUNT)) {
                isAltitudeValue = false;
                grid            = Misc.toDouble(llg.getCountGrid());
                //                missingValue = 0;
                missingValue = Double.NaN;
            } else if (whatGrid.equals(ARG_GRID_IDW)) {
                grid = llg.getWeightedValueGrid();
            }
            if (grid == null) {
                System.err.println("POINT: No grid found for:" + whatGrid);

                continue;
            }

            String fileSuffix  = "." + whatGrid.replace(ARG_GRID_PREFIX, "");
            String imageSuffix = fileSuffix + ".png";
            String imageLabel  = GRID_LABELS[i];

            if (doAsc) {
                writeAsciiArcGrid(request, jobId, mainEntry, llg, grid,
                                  missingValue, fileSuffix + ".asc");
            }

            double threshold = request.get(ARG_THRESHOLD, Double.NaN);
            if (doImage) {
                File imageFile =
                    getRepository().getStorageManager().getTmpFile("pointimage.png");
                writeImage(request, imageFile, llg, grid, missingValue,
                           threshold);
                InputStream imageInputStream =
                    getStorageManager().getFileInputStream(imageFile);
                OutputStream os = getOutputStream(request, jobId, mainEntry,
						  imageSuffix);
                long bytes = IOUtil.writeTo(imageInputStream, os);
                IOUtil.close(os);
                IOUtil.close(imageInputStream);
                if (doKmz) {
                    String imageFileName = imageSuffix;
                    Element groundOverlay = KmlUtil.groundOverlay(folder,
								  imageLabel, desc,
								  imageFileName,
								  llg.getNorth(),
								  llg.getSouth(),
								  llg.getEast(), llg.getWest());
                    if (request.get(ARG_KML_VISIBLE, true)) {
                        KmlUtil.visible(groundOverlay, true);
                    }
                    imageInputStream =
                        getStorageManager().getFileInputStream(imageFile);
                    zos.putNextEntry(new ZipEntry(imageFileName));
                    IOUtil.writeTo(imageInputStream, zos);
                    IOUtil.close(imageInputStream);
                    zos.closeEntry();
                }
            }

            //Only do hillshade for altitude values
            if (isAltitudeValue && (doHillshade || forceHillshade)) {
                File imageFile =
                    getRepository().getStorageManager().getTmpFile("pointimage.png");
                LatLonGrid hillshadeGrid =
                    org.ramadda.util.grid.Gridder.doHillShade(llg, grid,
							      (float) request.get(ARG_HILLSHADE_AZIMUTH, 315.0f),
							      (float) request.get(ARG_HILLSHADE_ANGLE, 45.0f));
                String destFileName = "hillshade" + imageSuffix;
                if (forceHillshade) {
                    request.putExtraProperty(getOutputFilename(mainEntry,
							       destFileName),  Boolean.FALSE);
                }
                writeImage(request, imageFile, hillshadeGrid,
                           hillshadeGrid.getValueGrid(), missingValue,
                           threshold);
                InputStream imageInputStream =
                    getStorageManager().getFileInputStream(imageFile);
                OutputStream os = getOutputStream(request, jobId, mainEntry,
						  destFileName);
                long bytes = IOUtil.writeTo(imageInputStream, os);
                IOUtil.close(os);
                IOUtil.close(imageInputStream);

                if (doKmz) {
                    String imageFileName = "hillshade" + imageSuffix;
                    Element groundOverlay = KmlUtil.groundOverlay(folder,
								  "Hill shaded  image "
								  + imageLabel, desc,
								  imageFileName,
								  llg.getNorth(),
								  llg.getSouth(),
								  llg.getEast(),
								  llg.getWest());
                    if (request.get(ARG_KML_VISIBLE, true)) {
                        KmlUtil.visible(groundOverlay, true);
                    }
                    imageInputStream =
                        getStorageManager().getFileInputStream(imageFile);
                    zos.putNextEntry(new ZipEntry(imageFileName));
                    IOUtil.writeTo(imageInputStream, zos);
                    IOUtil.close(imageInputStream);
                    zos.closeEntry();
                }
            }
        }


        if (doKmz) {
            request.getHttpServletResponse().setContentType(
							    "application/vnd.google-earth.kmz");
            zos.putNextEntry(new ZipEntry("points.kml"));
            String xml   = XmlUtil.toString(root);
            byte[] bytes = xml.getBytes();
            zos.write(bytes, 0, bytes.length);
            zos.closeEntry();
            IOUtil.close(zos);
        }

        return getDummyResult();
    }




    /**
     * Gets called from the main RAMADDA map view to add extra marking to the map
     *
     * @param request The request
     * @param entry The entry
     * @param map The map
     */
    public void addToMap(Request request, Entry entry, MapInfo map) {
        try {
            //Don't include the tracks if it has  polygon metadata
            List<Metadata> metadataList =
                getMetadataManager().findMetadata(request, entry,
						  MetadataHandler.TYPE_SPATIAL_POLYGON, true);

            if ((metadataList == null) || (metadataList.size() > 0)) {
                return;
            }
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
        getPointFormHandler().addToMap(request, entry, map);
    }



    /**
     * _more_
     *
     * @param request The request
     * @param jobId The job ID
     * @param mainEntry Either the  Collection or File Entry
     * @param llg latlongrid
     * @param grid _more_
     * @param missingValue _more_
     * @param fileSuffix _more_
     *
     * @throws Exception On badness
     */
    public void writeAsciiArcGrid(Request request, Object jobId,
                                  Entry mainEntry, IdwGrid llg,
                                  double[][] grid, double missingValue,
                                  String fileSuffix)
	throws Exception {
        boolean     haveMissingValue = !Double.isNaN(missingValue);
        final int   imageWidth       = llg.getWidth();
        final int   imageHeight      = llg.getHeight();
        PrintWriter pw = getPrintWriter(request, jobId, mainEntry,
                                        fileSuffix);
        pw.println("ncols " + imageWidth);
        pw.println("nrows " + imageHeight);
        pw.println("xllcorner " + llg.getWest());
        pw.println("yllcorner " + llg.getSouth());
        pw.println("cellsize "
                   + (llg.getEast() - llg.getWest()) / imageWidth);
        pw.println("nodata_value " + LatLonGrid.GRID_MISSING);
        System.err.println("POINT: writing ARC ASCII grid " + imageWidth
                           + " X " + imageHeight);
        for (int y = 0; y < imageHeight; y++) {
            for (int x = 0; x < imageWidth; x++) {
                double value = grid[y][x];
                if ((value != value) || (value == LatLonGrid.GRID_MISSING)
		    || (haveMissingValue && (value == missingValue))) {
                    value = LatLonGrid.GRID_MISSING;
                }
                pw.print(value);
                pw.print(" ");
            }
            pw.print("\n");
        }
        System.err.println("POINT: done writing ARC ASCII grid ");
        pw.close();
    }


    /**
     * _more_
     *
     * @param request The request
     * @param imageFile _more_
     * @param llg latlongrid
     * @param grid _more_
     * @param missingValue _more_
     * @param threshold _more_
     *
     * @throws Exception On badness
     */
    public void writeImage(Request request, File imageFile, LatLonGrid llg,
                           double[][] grid, double missingValue,
                           double threshold)
	throws Exception {
        int     imageWidth       = llg.getWidth();
        int     imageHeight      = llg.getHeight();

        boolean haveMissingValue = !Double.isNaN(missingValue);
        Color   defaultColor     = Color.CYAN;
        int[]   pixels           = new int[imageWidth * imageHeight];
        int     index            = 0;
        for (int x = 0; x < imageWidth; x++) {
            for (int y = 0; y < imageHeight; y++) {
                pixels[index++] = ((0xff << 24)
                                   | (defaultColor.getRed() << 16)
                                   | (defaultColor.getRed() << 8)
                                   | defaultColor.getRed());
            }
        }


        double max = Double.MIN_VALUE;
        double min = Double.MAX_VALUE;
        for (int y = 0; y < imageHeight; y++) {
            for (int x = 0; x < imageWidth; x++) {
                double value = grid[y][x];
                if (haveMissingValue && (value == missingValue)) {
                    continue;
                }
                if (Double.isNaN(value)) {
                    continue;
                }
                max = Math.max(max, value);
                min = Math.min(min, value);
            }
        }

        ColorTable colorTable =
            ColorTable.getColorTable(request.getString(ARG_COLORTABLE, ""));
        min = request.get(RecordConstants.ARG_GRID_RANGE_MIN, min);
        max = request.get(RecordConstants.ARG_GRID_RANGE_MAX, max);
        double[] range =
            ColorTable.getRange(request.getString(ARG_COLORTABLE, ""), min,
                                max);
        min   = range[0];
        max   = range[1];
        index = 0;
        double colorRange    = max - min;
        double colorRangeMin = min;
        for (int y = 0; y < imageHeight; y++) {
            for (int x = 0; x < imageWidth; x++) {
                double value = grid[y][x];
                if (( !Double.isNaN(threshold) && (value < threshold))
		    || Double.isNaN(value)
		    || (value == LatLonGrid.GRID_MISSING)
		    || (haveMissingValue && (value == missingValue))) {
                    //Set missing to transparent
                    pixels[index] = (0x00 << 24);
                } else {
                    //TODO: Check range for DBZ exception
                    double percent = (value - colorRangeMin) / colorRange;
                    pixels[index] = colorTable.getPixelValue(percent);
                }
                index++;
            }
        }


        Image newImage = Toolkit.getDefaultToolkit().createImage(
								 new MemoryImageSource(
										       imageWidth, imageHeight, pixels, 0,
										       imageWidth));

        float[] matrix = new float[400];
        for (int i = 0; i < 400; i++) {
            matrix[i] = 1.0f / 400.0f;
        }

        //        BufferedImageOp op = new ConvolveOp( new Kernel(20, 20, matrix), ConvolveOp.EDGE_NO_OP, null );
        /*
	  com.jhlabs.image.ConvolveFilter filter = new com.jhlabs.image.ConvolveFilter();
	  Image filteredImage = filter.filter(ImageUtils.toBufferedImage(newImage), null);

	  ImageUtils.writeImageToFile(filteredImage, imageFile);
        */
        ImageUtils.writeImageToFile(newImage, imageFile);
    }


    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        System.err.println("main");
        float[] matrix = new float[400];
        for (int i = 0; i < 400; i++) {
            matrix[i] = 1.0f / 400.0f;
        }

        //        BufferedImage newImage = ImageUtils.toBufferedImage(ImageUtils.readImage(args[0]));
        //        System.err.println("filtering");
        //        com.jhlabs.image.ConvolveFilter filter = new com.jhlabs.image.ConvolveFilter(new Kernel(20, 20, matrix));
        //        com.jhlabs.image.GaussianFilter filter = new com.jhlabs.image.GaussianFilter(5.0f);

        //        Image filteredImage = filter.filter(newImage, null);
        //        System.err.println("writing");
        //        ImageUtils.writeImageToFile(filteredImage, "filtered_" + args[0]);
    }

    /**
     * _more_
     *
     * @param request the request
     * @param mainEntry Either the  Collection or File Entry
     * @param entries entries to process
     * @param jobId The job ID
     *
     * @return _more_
     *
     * @throws Exception on badness
     */
    public Result outputEntryKmlTrack(Request request, Entry mainEntry,
                                      List<? extends PointEntry> entries,
                                      Object jobId)
	throws Exception {
        Element root = KmlUtil.kml(mainEntry.getName() + " Tracks");
        Element topFolder = KmlUtil.folder(root,
                                           mainEntry.getName() + " Tracks",
                                           false);

        for (PointEntry pointEntry : entries) {
            Entry             entry    = pointEntry.getEntry();
            final int[]       pointCnt = { 0 };
            final float[][][] coords   = {
                new float[3][1000]
            };
            RecordVisitor     visitor  = new BridgeRecordVisitor(this) {
		    public boolean doVisitRecord(RecordFile file,
						 VisitInfo visitInfo,
						 BaseRecord record) {
			PointRecord pointRecord = (PointRecord) record;
			float[][]   kmlCoords   = coords[0];
			if (pointCnt[0] >= kmlCoords[0].length) {
			    kmlCoords = coords[0] = Misc.expand(kmlCoords);
			}
			kmlCoords[0][pointCnt[0]] =
			    (float) pointRecord.getLatitude();
			kmlCoords[1][pointCnt[0]] =
			    (float) pointRecord.getLongitude();
			kmlCoords[2][pointCnt[0]] =
			    (float) pointRecord.getAltitude();
			pointCnt[0]++;

			return true;
		    }
		    public void finished(RecordFile file, VisitInfo visitInfo)
                        throws Exception {
			super.finished(file, visitInfo);
		    }
		};
            long numRecords = pointEntry.getNumRecords();
            int  skip       = (int) (numRecords / 1000);

            try {
                getRecordJobManager().visitSequential(request, pointEntry,
						      visitor,
						      new VisitInfo(VisitInfo.QUICKSCAN_YES, skip));
            } catch (Throwable thr) {
                Throwable inner = LogUtil.getInnerException(thr);
                if (inner instanceof Exception) {
                    throw (Exception) inner;
                }

                throw new RuntimeException(inner);
            }

            coords[0] = Misc.copy(coords[0], pointCnt[0]);
            Element folder = KmlUtil.folder(topFolder, entry.getName(),
                                            false);
            if (entry.getDescription().length() > 0) {
                KmlUtil.description(folder, entry.getDescription());
            }
            KmlUtil.placemark(folder, "Track", "", coords[0], Color.red, 2);
        }

        PrintWriter pw = getPrintWriter(request, jobId, mainEntry, ".kml");
        XmlUtil.toString(root, pw);
        pw.close();

        return getDummyResult();
    }

    /**
     * gets the bounds of the given entries
     *
     * @param request the request
     * @param entries _more_
     *
     * @return the bounds - north, west,south,east
     */
    public Rectangle2D.Double getBounds(Request request,
                                        List<? extends PointEntry> entries) {


        SelectionRectangle theBbox = request.getSelectionBounds();
        theBbox.normalizeLongitude();

        //TODO: handle date line
        SelectionRectangle[] bboxes = theBbox.splitOnDateLine();
        double               north  = 0;
        double               south  = 0;
        double               east   = 0;
        double               west   = 0;
        SelectionRectangle   bbox   = bboxes[0];


        for (int i = 0; i < entries.size(); i++) {
            PointEntry pointEntry = entries.get(i);
            Entry      entry      = pointEntry.getEntry();
            double     tmpnorth   = bbox.getNorth(entry.getNorth(request));
            double     tmpsouth   = bbox.getSouth(entry.getSouth(request));
            double     tmpeast    = bbox.getEast(entry.getEast(request));
            double     tmpwest    = bbox.getWest(entry.getWest(request));
            if (i == 0) {
                north = tmpnorth;
                south = tmpsouth;
                east  = tmpeast;
                west  = tmpwest;
            } else {
                north = Math.max(tmpnorth, north);
                south = Math.min(tmpsouth, south);
                east  = Math.max(tmpeast, east);
                west  = Math.min(tmpwest, west);
            }

            /**
             * north = entry.getNorth();
             * south =  entry.getSouth();
             * east = entry.getEast();
             * west = entry.getWest();
             */

        }





        //TODO: is this right?
        if (Math.abs(north - south) > Math.abs(east - west)) {
            //            east = west + Math.abs(north - south);
        } else {
            //            north = south + Math.abs(east - west);
        }

        //        System.err.println("BOUNDS: lat:" + north +" " + south + " lon:" + west +" " + east);
        return new Rectangle2D.Double(west, north, east - west,
                                      south - north);
    }





    /**
     * This handles the ajax request for the geolocation of an index in the given point file
     *
     * @param request the request
     * @param pointEntry The entry
     *
     * @return result
     *
     * @throws Exception on badness
     */
    public Result outputEntryGetLatLon(Request request, PointEntry pointEntry)
	throws Exception {
        long numRecords = pointEntry.getNumRecords();
        int index = (int) Math.min(numRecords - 1,
                                   request.get(ARG_POINTINDEX, 0));
        //Use the extra short binary file
        PointRecord record =
            (PointRecord) pointEntry.getBinaryPointFile().getRecord(index);
        StringBuffer sb = new StringBuffer("<result>");
        sb.append("{\"latitude\":" + record.getLatitude() + ",\"longitude\":"
                  + record.getLongitude() + "}");
        sb.append("</result>");
        Result result = new Result("", sb, "text/xml");

        return result;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param services _more_
     */
    public void getServiceInfos(Request request, Entry entry,
                                List<ServiceInfo> services) {
        super.getServiceInfos(request, entry, services);
        if ( !isEntryOk(entry)) {
            return;
        }
        String url;
        String dfltBbox = entry.getWest(request) + "," + entry.getSouth(request) + ","
	    + entry.getEast(request) + "," + entry.getNorth(request);

        String lasProduct = null;
        if (OUTPUT_LAS != null) {
            lasProduct = OUTPUT_LAS.toString();
        }
        String pointsIcon = getAbsoluteIconUrl(request, "/icons/chart.png");
        String[][] values = {
            { OUTPUT_JSON.toString(), "Point JSON", ".json", pointsIcon,
              "&max=${numpoints}" },
            { OUTPUT_IDVCSV.toString(), "IDV CSV", ".csv", pointsIcon },
            { OUTPUT_CSV.toString(), "CSV", ".csv", pointsIcon },
            { lasProduct, "LAS 1.2", ".las", pointsIcon },
            { OUTPUT_KMZ.toString(), "Google Earth KMZ", ".kmz",
              getAbsoluteIconUrl(request, "/icons/kml.png") }
        };




        for (String[] tuple : values) {
            String product = tuple[0];
            if (product == null) {
                continue;
            }
            String name      = tuple[1];
            String suffix    = tuple[2];
            String icon      = tuple[3];
            String extraArgs = "";
            if (tuple.length >= 5) {
                extraArgs = tuple[4];
            }
            String serviceFilename = getServiceFilename(entry) + suffix;

            url = HtmlUtils.url(getRepository().URL_ENTRY_SHOW + "/"
                                + serviceFilename, new String[] {
				    ARG_ENTRYID, entry.getId(), ARG_OUTPUT,
				    OUTPUT_PRODUCT.getId(), ARG_PRODUCT, product,
				    //ARG_ASYNCH, "false", 
				    //                ARG_Record_SKIP,
				    //                macro(ARG_RECORD_SKIP), 
				    //                ARG_BBOX,  macro(ARG_BBOX), 
				    //                ARG_DEFAULTBBOX, dfltBbox
				}, false);
            url += extraArgs;
            services.add(new ServiceInfo(product, name,
                                         request.getAbsoluteUrl(url), icon));
        }
    }


    /*
      public Result outputEntryNc(Request request, Entry mainEntry,
      OutputType outputType,
      List<PointEntry> pointEntries,
      Object jobId)
      throws Exception {
      if ( !request.defined(ARG_FILLMISSING)) {
      request.put(ARG_FILLMISSING, "true");
      }

      String    mimeType    = "application/x-netcdf";
      Rectangle2D.Double  bounds      = getBounds(request, pointEntries);


      GridVisitor gridVisitor  =  makeGridVisitor(request,
      pointEntries,
      bounds);
      getPointJobManager().visitConcurrent(request, pointEntries, gridVisitor, new VisitInfo(VisitInfo.QUICKSCAN_NO));
      IdwGrid latLonGrid = gridVisitor.getGrid();


      if(request.get(ARG_FILLMISSING, false)) {
      llg.fillMissing();
      }

      double [][]grid = llg.getValueGrid();

      try {
      float[] xVals = new float[imageWidth];
      float[] yVals = new float[imageHeight];

      String filename = "foo";
      NetcdfFileWriteable ncfile =
      NetcdfFileWriteable.createNew(filename, false);
      List<Dimension> dims           = new ArrayList<Dimension>();
      //            Dimension xDim  = new Dimension(xName, sizeX, true);
      //            ncfile.addDimension(null, xDim);

      Variable v  = new Variable(ncfile, null, null, "elevation");
      v.addAttribute(new Attribute("units", "m"));
      if (projVar != null) {
      v.addAttribute(new Attribute("grid_mapping",
      "geographic"));
      }
      v.setDataType(DataType.FLOAT);
      v.setDimensions(dims);
      ncfile.addVariable(null, v);
      ncfile.addGlobalAttribute(new Attribute("Conventions", "CF-1.X"));
      ncfile.addGlobalAttribute(new Attribute("History",
      "Generated from RAMADDA Point Data"));
      ncfile.create();
      for (Iterator it = keys.iterator(); it.hasNext(); ) {
      Variable v = (Variable) it.next();
      ncfile.write(v.getName(), varData.get(v));
      }
      int   numDims = dims.size();
      int[] sizes   = new int[numDims];
      int   index   = 0;
      for (Dimension dim : dims) {
      sizes[index++] = dim.getLength();
      }

      // write the data
      Array arr = null;
      float[][] samples = ((FlatField) grid).getFloats();
      for (int j = 0; j < rTypes.length; j++) {
      Variable v = ncfile.findVariable(getVarName(rTypes[j]));
      arr = Array.factory(DataType.FLOAT, sizes, samples[j]);
      ncfile.write(v.getName(), arr);
      }
      // write the file
      ncfile.close();

      return getDummyResult();
      }

    */


    /**
     * _more_
     *
     * @param request the request
     * @param dflt _more_
     *
     * @return _more_
     */
    public int getSkip(Request request, int dflt) {
        return super.getSkip(request, dflt, ARG_RECORD_SKIP);
    }

    /**
     * _more_
     *
     * @param request the request
     * @param dflt _more_
     *
     * @return _more_
     */
    public int getSkipZ(Request request, int dflt) {
        String skip = request.getString(ARG_RECORD_SKIPZ, "");
        if (skip.equals("${skipz}")) {
            return dflt;
        }

        return request.get(ARG_RECORD_SKIPZ, dflt);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param recordFile _more_
     * @param filters _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void getFilters(Request request, Entry entry,
                           RecordFile recordFile, List<RecordFilter> filters)
	throws Exception {

        super.getFilters(request, entry, recordFile, filters);
        //      filters.add(new AltitudeFilter(0, Double.NaN));


        Date[] dateRange = request.getDateRange(ARG_FROMDATE, ARG_TODATE, "",
						null);

        if ((dateRange[0] != null) || (dateRange[1] != null)) {
            filters.add(new TimeFilter(dateRange[0], dateRange[1]));
        }


        SelectionRectangle bbox = request.getSelectionBounds();
        if (bbox.anyDefined()) {
            bbox.normalizeLongitude();
            //If the request crosses the dateline then split it into to and make an OR filter
            if (bbox.allDefined() && bbox.crossesDateLine()) {
                SelectionRectangle[] bboxes = bbox.splitOnDateLine();
                RecordFilter leftFilter =
                    new LatLonBoundsFilter(bboxes[0].getNorth(),
                                           bboxes[0].getWest(),
                                           bboxes[0].getSouth(),
                                           bboxes[0].getEast());
                RecordFilter rightFilter =
                    new LatLonBoundsFilter(bboxes[1].getNorth(),
                                           bboxes[1].getWest(),
                                           bboxes[1].getSouth(),
                                           bboxes[1].getEast());
                filters.add(CollectionRecordFilter.or(new RecordFilter[] {
			    leftFilter,
			    rightFilter }));
            } else {
                filters.add(new LatLonBoundsFilter(bbox.getNorth(90),
						   bbox.getWest(-180.0), bbox.getSouth(-90.0),
						   bbox.getEast(180.0)));
            }
        }

        if (request.defined(ARG_PROBABILITY)) {
            filters.add(new RandomizedFilter(request.get(ARG_PROBABILITY,
							 0.5)));
        }

        List<RecordField> searchableFields = null;
        for (Enumeration keys = request.getArgs().keys();
	     keys.hasMoreElements(); ) {
            String key = (String) keys.nextElement();
            if (key.startsWith(ARG_SEARCH_PREFIX) && !key.endsWith("_enumlist")) {
                double v       = request.get(key, 0.0);
                String fieldId = key.substring(ARG_SEARCH_PREFIX.length());
                if (searchableFields == null) {
                    searchableFields = recordFile.getSearchableFields();
                }
                for (RecordField field : searchableFields) {
                    if (field.getName().equals(fieldId)) {
                        filters.add(
				    new NumericRecordFilter(
							    NumericRecordFilter.OP_EQUALS,
							    field.getParamId(), v));

                        break;
                    }
                }
            }
        }

        if (searchableFields != null) {
            for (RecordField field : searchableFields) {
                if (field.isBitField()) {
                    String[] bitFields = field.getBitFields();
                    String urlArgPrefix = ARG_SEARCH_PREFIX + field.getName()
			+ "_" + ARG_BITFIELD + "_";
                    for (int bitIdx = 0; bitIdx < bitFields.length;
			 bitIdx++) {
                        String bitField = bitFields[bitIdx].trim();
                        if (bitField.length() == 0) {
                            continue;
                        }
                        if (request.defined(urlArgPrefix + bitIdx)) {
                            filters.add(new BitmaskRecordFilter(bitIdx,
								request.get(urlArgPrefix + bitIdx,
									    false), field.getParamId()));
                        }
                    }

                    continue;
                }

                if (request.defined(ARG_SEARCH_PREFIX + field.getName()
                                    + "_enumlist")) {
		    List<String> lines = Utils.split(request.getString(ARG_SEARCH_PREFIX + field.getName()   + "_enumlist",""),"\n",true,true);
		    if(lines.size()>0) {
			HashSet enums =Utils.makeHashSet(lines);
			filters.add(new EnumRecordFilter(field.getParamId(),enums));
		    }
		    
                    continue;
		}
		


                if (request.defined(ARG_SEARCH_PREFIX + field.getName()
                                    + "_min")) {
                    double v = request.get(ARG_SEARCH_PREFIX
                                           + field.getName() + "_min", 0.0);
                    filters.add(
				new NumericRecordFilter(
							NumericRecordFilter.OP_GE, field.getParamId(),
							v));
                }
                if (request.defined(ARG_SEARCH_PREFIX + field.getName()
                                    + "_max")) {
                    double v = request.get(ARG_SEARCH_PREFIX
                                           + field.getName() + "_max", 0.0);
                    filters.add(
				new NumericRecordFilter(
							NumericRecordFilter.OP_LE, field.getParamId(),
							v));
                }
            }
        }

    }


    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    @Override
    public boolean isEntryOk(Entry entry) {
        return entry.getTypeHandler().isType("type_point")
	    || entry.getTypeHandler().isType("lidar") ||
	    entry.getTypeHandler().isType("type_document_csv");
    }



    /**
     * This gets called to add links into the entry menus in the HTML views.
     * e.g., Subset form for the Collection, Map, metadata and subset form
     * links for point files
     *
     * @param request the request
     * @param state This holds the group, entry, children, etc.
     * @param links list to add to
     *
     *
     * @throws Exception on badness
     */
    public void getEntryLinks(Request request, State state, List<Link> links)
	throws Exception {

        Entry entry = state.getEntry();
        if (entry == null) {
            return;
        }

        if (entry.getTypeHandler() instanceof PointCollectionTypeHandler) {
            links.add(makeLink(request, state.getEntry(), OUTPUT_FORM));
            return;
        }


        if ( !isEntryOk(entry)) {
            return;
        }


        if (entry.getTypeHandler() instanceof RecordCollectionTypeHandler) {
            links.add(makeLink(request, state.getEntry(), OUTPUT_FORM_CSV));
            links.add(makeLink(request, state.getEntry(), OUTPUT_FORM));
            return;
        }

        //if ( !state.entry.isFile()) {
        //    return;
        //}

        if ( !getRepository().getAccessManager().canAccessFile(request,
							       state.entry)) {
            return;
        }

        links.add(makeLink(request, state.getEntry(), OUTPUT_CHART));
        links.add(makeLink(request, state.getEntry(), OUTPUT_FORM_CSV));
        links.add(makeLink(request, state.getEntry(), OUTPUT_FORM));
        links.add(makeLink(request, state.getEntry(), OUTPUT_VIEW));
        links.add(makeLink(request, state.getEntry(), OUTPUT_METADATA));
    }



    /**
     * make the file object
     *
     * @param request the request
     * @param entry The entry
     * @param numRecords How many records are in the file. May be < 0.
     *
     * @return the file
     *
     * @throws Exception on badness
     */
    public RecordFile createAndInitializeRecordFile(Request request,
						    Entry entry, long numRecords)
	throws Exception {
        RecordFile recordFile = (RecordFile) doMakeRecordFile(request, entry);
        if (recordFile == null) {
            return null;
        }
        recordFile.putProperty("entry", entry);
        if (numRecords < 0) {
            numRecords = recordFile.getNumRecords();
        }



	String format = request.getString(ARG_DATEFORMAT+"_custom",null);
	if(!stringDefined(format)) {
	    format = request.getString(ARG_DATEFORMAT,null);
	}
	if(stringDefined(format)) {
	    recordFile.setOutputDateFormat(Utils.makeDateFormat(format));
	}
        if (request.defined(ARG_RECORD_SKIP)) {
            int skip = getSkip(request, 1000);
            recordFile.setDefaultSkip(skip);
        } else if (request.defined(RecordFormHandler.ARG_NUMPOINTS)) {
            int numPoints = request.get(RecordFormHandler.ARG_NUMPOINTS,
                                        1000);
            if (numPoints > 0) {
                int skip = (int) (numRecords / numPoints);
                recordFile.setDefaultSkip(skip);
            }
        } else if (numRecords < 10000) {
            recordFile.setDefaultSkip(0);
        } else {
            //Default is 10000 points
            //            recordFile.setDefaultSkip((int)(numRecords/10000));
        }

        return recordFile;
    }



    /**
     * _more_
     *
     * @param outputs _more_
     * @param forCollection _more_
     */
    public void getPointFormats(List<HtmlUtils.Selector> outputs,
                                boolean forCollection) {
        outputs.add(getPointFormHandler().getSelect(OUTPUT_SUBSET));
        outputs.add(getPointFormHandler().getSelect(OUTPUT_CSV));
        outputs.add(getPointFormHandler().getSelect(OUTPUT_JSON));
	outputs.add(getPointFormHandler().getSelect(OUTPUT_NC));
        outputs.add(getPointFormHandler().getSelect(OUTPUT_LATLONALTCSV));
    }


    /**
     * return the csv for the given entry
     *
     * @param request the request
     * @param entry the entry
     *
     * @return csv text
     *
     * @throws Exception on badness
     */
    public String getCsv(Request request, Entry entry) throws Exception {
        request = new Request(request.getRepository(), request.getUser());
        request.put(ARG_ASYNCH, "false");
        request.put(ARG_PRODUCT, OUTPUT_CSV.getId());
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        request.setOutputStream(bos);
        Result result = outputEntry(request, OUTPUT_PRODUCT, entry);
        String csv    = new String(bos.toByteArray());
        IOUtil.close(bos);
        return csv;
    }


}
