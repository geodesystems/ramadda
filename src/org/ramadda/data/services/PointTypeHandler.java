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


import org.ramadda.data.point.PointFile;
import org.ramadda.data.point.PointMetadataHarvester;
import org.ramadda.data.record.RecordField;
import org.ramadda.data.record.RecordFile;
import org.ramadda.data.record.RecordFileFactory;
import org.ramadda.data.record.RecordVisitorGroup;

import org.ramadda.data.record.VisitInfo;
import org.ramadda.data.services.PointEntry;

import org.ramadda.data.services.RecordEntry;

import org.ramadda.repository.*;
import org.ramadda.repository.map.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.FileInfo;
import org.ramadda.util.FormInfo;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Json;
import org.ramadda.util.Utils;
import org.ramadda.util.grid.LatLonGrid;

import org.w3c.dom.*;

import ucar.unidata.ui.ImageUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.awt.image.*;


import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;


import java.io.File;
import java.io.FileOutputStream;


import java.text.SimpleDateFormat;


import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;


/**
 *
 *
 * @author Jeff McWhirter
 * @version $Revision: 1.3 $
 */
public class PointTypeHandler extends RecordTypeHandler {

    /** _more_ */
    public static final int IDX_LAST = RecordTypeHandler.IDX_LAST;

    /** _more_ */
    public static final String ARG_PROPERTIES_FILE = "properties.file";

    /**
     * _more_
     *
     * @param repository _more_
     * @param type _more_
     * @param description _more_
     */
    public PointTypeHandler(Repository repository, String type,
                            String description) {
        super(repository, type, description);
    }




    /**
     * _more_
     *
     * @param repository ramadda
     * @param node _more_
     * @throws Exception On badness
     */
    public PointTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }




    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public RecordOutputHandler doMakeRecordOutputHandler() throws Exception {
        RecordOutputHandler poh =
            (RecordOutputHandler) getRepository().getOutputHandler(
                PointOutputHandler.class);
        if (poh == null) {
            poh = new PointOutputHandler(getRepository(), null);
        }

        return poh;
    }






    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     *
     * @throws Exception On badness
     */
    @Override
    public void initializeNewEntry(Request request, Entry entry)
            throws Exception {

        if (entry.getXmlNode() != null) {
            return;
        }

        if (anySuperTypesOfThisType()) {
            super.initializeNewEntry(request, entry);
            return;
        }

        log("initialize new entry:" + entry.getResource());
        File file = entry.getFile();
        if ((file != null) && !file.exists()) {
            //Maybe this is a URL?
            //            return;
        } else {
            //This finds any properties files next to the file
            initializeRecordEntry(entry, file, false);
        }


        PointOutputHandler outputHandler =
            (PointOutputHandler) getRecordOutputHandler();
        RecordVisitorGroup visitorGroup = new RecordVisitorGroup();
        PointEntry pointEntry = (PointEntry) outputHandler.doMakeEntry(
                                    getRepository().getTmpRequest(), entry);
        RecordFile pointFile = pointEntry.getRecordFile();
        if (pointFile == null) {
            System.err.println("PointTypeHandler.init: point file is null");

            return;
        }
        List<PointEntry> pointEntries = new ArrayList<PointEntry>();
        pointEntries.add(pointEntry);
        PointMetadataHarvester metadataHarvester =
            ((PointTypeHandler) entry.getTypeHandler())
                .doMakeMetadataHarvester(pointEntry);
        //        System.err.println (getClass().getName()+"  - scanning file:" + metadataHarvester.getClass().getName());
        visitorGroup.addVisitor(metadataHarvester);
        final File quickScanFile = pointEntry.getQuickScanFile();


        DataOutputStream dos = new DataOutputStream(
                                   new BufferedOutputStream(
                                       new FileOutputStream(quickScanFile)));
        boolean quickscanDouble =
            PointEntry.isDoubleBinaryFile(quickScanFile);
        //Make the latlon binary file when we ingest the  datafile
        visitorGroup.addVisitor(outputHandler.makeLatLonBinVisitor(request,
                entry, pointEntries, null, dos, quickscanDouble));
        log("initialize new entry: visting file");
        pointFile.visit(visitorGroup, new VisitInfo(VisitInfo.QUICKSCAN_NO),
                        null);
        dos.close();
        log("initialize new entry: count=" + metadataHarvester.getCount());
        ((PointTypeHandler) entry.getTypeHandler()).handleHarvestedMetadata(
            pointEntry, metadataHarvester);
        log("initialize new entry: done");


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
        PointOutputHandler poh =
            (PointOutputHandler) getRecordOutputHandler();
        PointEntry recordEntry = (PointEntry) poh.doMakeEntry(request, entry);
        RecordFile recordFile  = recordEntry.getRecordFile();
        if (recordFile == null) {
            return;
        }
        List<RecordField> fields =
            recordEntry.getRecordFile().getFields(true);

        if (fields == null) {
            return;
        }

	StringBuilder all = new StringBuilder();

        for (RecordField field : fields) {
	    if(all.length()>0) all.append(",");
	    all.append(field.getName());
	}
	sb.append("&nbsp;");
	sb.append(
		  HtmlUtils.mouseClickHref(
					   HtmlUtils.call(
							  "selectClick",
							  HtmlUtils.comma(
									  HtmlUtils.squote(target),
									  HtmlUtils.squote(entry.getId()),
									  HtmlUtils.squote(all.toString()),
									  HtmlUtils.squote(type))), "All Fields"));
	sb.append("<br>");

        for (RecordField field : fields) {
            sb.append("&nbsp;");
            sb.append(
                HtmlUtils.mouseClickHref(
                    HtmlUtils.call(
                        "selectClick",
                        HtmlUtils.comma(
                            HtmlUtils.squote(target),
                            HtmlUtils.squote(entry.getId()),
                            HtmlUtils.squote(field.getName()),
                            HtmlUtils.squote(type))), field.getLabel()));
            sb.append("<br>");
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
    public String getWikiEditorSidebar(Request request, Entry entry)
            throws Exception {
        //        PointOutputHandler outputHandler =
        //            (PointOutputHandler) getRecordOutputHandler();
        //TODO
        return "";

    }

    /**
     * Gets called by the slack plugin. create a time series image
     *
     *
     * @param cmdRequest _more_
     * @param entry _more_
     * @param harvester _more_
     * @param args _more_
     * @param sb _more_
     * @param files _more_
     *
     *
     * @return _more_
     * @throws Exception on badness
     */
    @Override
    public boolean processCommandView(org.ramadda.repository.harvester
            .CommandHarvester.CommandRequest cmdRequest, Entry entry,
                org.ramadda.repository.harvester.CommandHarvester harvester,
                final List<String> args, final Appendable sb,
                List<FileInfo> files)
            throws Exception {
        Request            request = cmdRequest.getRequest();
        PointOutputHandler poh =
            (PointOutputHandler) getRecordOutputHandler();
        PointEntry pointEntry = (PointEntry) poh.doMakeEntry(request, entry);
        File       processDir = getStorageManager().createProcessDir();
        File imageFile = new File(IOUtil.joinDir(processDir,
                             entry.getName() + "_timeseries.png"));

        PointFormHandler.PlotInfo plotInfo = new PointFormHandler.PlotInfo();
        //        System.err.println ("calling makeTimeSeriesImage");
        long t1 = System.currentTimeMillis();
        BufferedImage newImage =
            poh.getPointFormHandler().makeTimeseriesImage(request,
                pointEntry, plotInfo);
        //        System.err.println ("Done makeTimeSeriesImage");
        long t2 = System.currentTimeMillis();
        //        System.err.println("File:  " + imageFile);
        ImageUtils.writeImageToFile(newImage, imageFile);
        long t3 = System.currentTimeMillis();
        //        System.err.println ("Done writeImageToFile:" + (t3-t2));


        FileInfo fileInfo = new FileInfo(imageFile);
        if ( !isWikiText(entry.getDescription())) {
            fileInfo.setDescription(entry.getDescription());
        }
        fileInfo.setTitle("Time series - " + entry.getName());
        files.add(fileInfo);



        //Now we get the process entry id
        String processId = processDir.getName();
        String processEntryId =
            getStorageManager().getEncodedProcessDirEntryId(processId);

        String entryUrl =
            HtmlUtils.url(
                request.getAbsoluteUrl(getRepository().URL_ENTRY_SHOW),
                ARG_ENTRYID,
        // Use this if you want to return the process directory
        //        processEntryId);
        getStorageManager().getEncodedProcessDirEntryId(processId + "/"
            + imageFile.getName()));

        //        System.err.println("URL:" + entryUrl);
        return true;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param parent _more_
     * @param newEntry _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void initializeEntryFromForm(Request request, Entry entry,
                                        Entry parent, boolean newEntry)
            throws Exception {

        if (anySuperTypesOfThisType()) {
            super.initializeEntryFromForm(request, entry, parent, newEntry);

            return;
        }

        //Check for an uploaded properties file and set the ARG_PROPERTIES 
        String propertyFileName =
            request.getUploadedFile(ARG_PROPERTIES_FILE);
        if (propertyFileName != null) {
            String contents =
                getStorageManager().readSystemResource(propertyFileName);
            request.put(getColumns().get(1).getEditArg(), contents);
        }

        //        System.err.println ("Values after:" +         entry.getTypeHandler().getEntryValues(entry)[1]);

        super.initializeEntryFromForm(request, entry, parent, newEntry);

    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param fromImport _more_
     */
    @Override
    public void doFinalEntryInitialization(Request request, Entry entry,
                                           boolean fromImport) {
        try {
            super.doFinalEntryInitialization(request, entry, fromImport);
            if ( !anySuperTypesOfThisType()) {
                getEntryManager().setBoundsFromChildren(request,
                        entry.getParentEntry());
                getEntryManager().setTimeFromChildren(request,
                        entry.getParentEntry(), null);
            }
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    /**
     * _more_
     *
     * @param pointEntry _more_
     *
     * @return _more_
     */
    public PointMetadataHarvester doMakeMetadataHarvester(
            RecordEntry pointEntry) {
        return new PointMetadataHarvester();
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param column _more_
     * @param formBuffer _more_
     * @param entry _more_
     * @param values _more_
     * @param state _more_
     * @param formInfo _more_
     * @param baseTypeHandler _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void addColumnToEntryForm(Request request, Column column,
                                     Appendable formBuffer, Entry entry,
                                     Object[] values, Hashtable state,
                                     FormInfo formInfo,
                                     TypeHandler baseTypeHandler)
            throws Exception {
        super.addColumnToEntryForm(request, column, formBuffer, entry,
                                   values, state, formInfo, baseTypeHandler);


        if ((entry == null) && column.getName().equals("properties")) {
            if (baseTypeHandler.okToShowInForm(entry, "properties")) {
                formBuffer.append(
                    HtmlUtils.formEntry(
                        msgLabel("Or upload properties"),
                        HtmlUtils.fileInput(
                            ARG_PROPERTIES_FILE, HtmlUtils.SIZE_70)));
            }
        }

    }




    /**
     * _more_
     *
     * @param path _more_
     * @param filename _more_
     *
     * @return _more_
     */
    @Override
    public boolean canHandleResource(String path, String filename) {
        try {
            if (filename.endsWith(".csv") || filename.endsWith(".txt")
                    || filename.endsWith(".xyz")
                    || filename.endsWith(".tsv")) {
                //Look to see if there is also a properties file
                Hashtable props = RecordFile.getPropertiesForFile(path,
                                      PointFile.DFLT_PROPERTIES_FILE);
                if (props.size() == 0) {
                    return false;
                }
            }

            return super.canHandleResource(path, filename);
        } catch (Exception exc) {
            //If the loading flaked out then just keep going
            //            logException("Harvesting file:" + f, exc);
            return false;
        }
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param tag _more_
     * @param props _more_
     *
     * @return _more_
     */
    @Override
    public String getUrlForWiki(Request request, Entry entry, String tag,
                                Hashtable props) {
        if (tag.equals(WikiConstants.WIKI_TAG_CHART)
                || tag.equals(WikiConstants.WIKI_TAG_DISPLAY)) {
            try {
                if (props.get("max") == null) {
                    props.put("max",
                              "" + getDefaultMax(request, entry, tag, props));
                }

                return ((PointOutputHandler) getRecordOutputHandler())
                    .getJsonUrl(request, entry, props);
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }
        }

        return super.getUrlForWiki(request, entry, tag, props);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param tag _more_
     * @param props _more_
     *
     * @return _more_
     */
    public int getDefaultMax(Request request, Entry entry, String tag,
                             Hashtable props) {
        try {
            String fromProps;
            fromProps = (String) props.get("maxPoints");
            if (fromProps != null) {
                return Integer.parseInt(fromProps);
            }
            Hashtable recordProps = getRecordProperties(entry);
            if (recordProps != null) {
                fromProps = (String) recordProps.get("maxPoints");
                if (fromProps != null) {
                    return Integer.parseInt(fromProps);
                }
            }

            return getTypeProperty("point.default.max", 5000);
        } catch (Exception exc) {
            throw new IllegalArgumentException(exc);

        }
    }

    /**
     * _more_
     *
     * @param recordEntry _more_
     * @param metadata _more_
     *
     * @throws Exception _more_
     */
    protected void handleHarvestedMetadata(RecordEntry recordEntry,
                                           PointMetadataHarvester metadata)
            throws Exception {

        PointEntry pointEntry = (PointEntry) recordEntry;
        Entry      entry      = pointEntry.getEntry();

        //We need to do the polygon thing here so we have the geo bounds to make the grid

        /**
         * lets not harvest the bounding polygon as this doesn't make sense for most point data
         * if (pointEntry.isCapable(PointFile.ACTION_BOUNDINGPOLYGON)) {
         *   if ( !entry.hasMetadataOfType(
         *           MetadataHandler.TYPE_SPATIAL_POLYGON)) {
         *       LatLonGrid llg = new LatLonGrid(80, 40,
         *                            metadata.getMaxLatitude(),
         *                            metadata.getMinLongitude(),
         *                            metadata.getMinLatitude(),
         *                            metadata.getMaxLongitude());
         *
         *       PointMetadataHarvester metadata2 =
         *           new PointMetadataHarvester(llg);
         *       //                System.err.println("PointTypeHandler: visiting binary file");
         *       pointEntry.getBinaryPointFile().visit(metadata2,
         *                                             new VisitInfo(VisitInfo.QUICKSCAN_NO), null);
         *       List<double[]> polygon = llg.getBoundingPolygon();
         *       StringBuilder[] sb = new StringBuilder[] {
         *                                new StringBuilder(),
         *                                new StringBuilder(),
         *                                new StringBuilder(),
         *                                new StringBuilder() };
         *       int idx = 0;
         *       for (double[] point : polygon) {
         *           String toAdd = point[0] + "," + point[1] + ";";
         *           if ((sb[idx].length() + toAdd.length())
         *                   >= (Metadata.MAX_LENGTH - 100)) {
         *               idx++;
         *               if (idx >= sb.length) {
         *                   break;
         *               }
         *           }
         *           sb[idx].append(toAdd);
         *       }
         *       //                System.err.println ("sb length:" + sb[idx].length() +" " +Metadata.MAX_LENGTH);
         *
         *       Metadata polygonMetadata =
         *           new Metadata(getRepository().getGUID(), entry.getId(),
         *                        MetadataHandler.TYPE_SPATIAL_POLYGON,
         *                        DFLT_INHERITED, sb[0].toString(),
         *                        sb[1].toString(), sb[2].toString(),
         *                        sb[3].toString(), Metadata.DFLT_EXTRA);
         *       getMetadataManager().addMetadata(entry, polygonMetadata,
         *               false);
         *   }
         * }
         */

        String descriptionFromFile =
            pointEntry.getRecordFile().getDescriptionFromFile();
        if (Utils.stringDefined(descriptionFromFile)
                && !Utils.stringDefined(entry.getDescription())) {
            entry.setDescription(descriptionFromFile);
        }


        //All point types should have at least:
        //pointCount, properties
        Object[] values = entry.getTypeHandler().getEntryValues(entry);
        values[0] = new Integer(metadata.getCount());



        //If the file has metadata then it better match up with the values that are defined in types.xml
        Object[] fileMetadata = pointEntry.getRecordFile().getFileMetadata();
        if (fileMetadata != null) {
            if (fileMetadata.length != values.length - 2) {
                throw new IllegalArgumentException("Bad file metadata count:"
                        + fileMetadata.length + " was expecting:"
                        + (values.length - 2));
            }
            for (int i = 0; i < fileMetadata.length; i++) {
                values[i + 2] = fileMetadata[i];
            }
        }

        Properties properties = metadata.getProperties();
        if (properties != null) {
            String contents = makePropertiesString(properties);
            //Append the properties file contents
            if (values[1] != null) {
                values[1] = "\n" + contents;
            } else {
                values[1] = contents;
            }
        }



        //        xxxxx
        entry.setValues(values);
        if ( !Double.isNaN(metadata.getMaxLatitude())) {
            entry.setNorth(metadata.getMaxLatitude());
            entry.setSouth(metadata.getMinLatitude());
            entry.setEast(metadata.getMaxLongitude());
            entry.setWest(metadata.getMinLongitude());
            if ( !Double.isNaN(metadata.getMinElevation())) {
                entry.setAltitudeBottom(metadata.getMinElevation());
            }
            if ( !Double.isNaN(metadata.getMaxElevation())) {
                entry.setAltitudeTop(metadata.getMaxElevation());
            }
        }

        if (metadata.hasTimeRange()) {
            entry.setStartDate(metadata.getMinTime());
            entry.setEndDate(metadata.getMaxTime());
            //            System.err.println("has time:" + new Date(entry.getStartDate()) +"  --  " + new Date(entry.getEndDate()));
        } else {
            //            System.err.println("no time in metadata");
        }

	String header = pointEntry.getRecordFile().getTextHeader();
	if(header!=null && header.length()>0 && getTypeProperty("point.initialize", true)) {
	    String patterns = (String) getTypeProperty("record.patterns",
						       (String) null);
	    if (patterns != null) {
		//TODO: Don't read the full contents, rather read the header
		List<String> toks = StringUtil.split(patterns, ",");
		String time = null;
		for (String tok : toks) {
		    List<String> toks2 = StringUtil.splitUpTo(tok, ":", 2);
		    if (toks2.size() != 2) {
			continue;
		    }
		    String field   = toks2.get(0).trim();
		    String pattern = toks2.get(1);
		    String value   = StringUtil.findPattern(header, pattern);
		    //		    System.err.println(field +" p:" + pattern +" v:" +value);
		    if (value != null) {
			if (field.equals("latitude")) {
			    entry.setLatitude(Utils.decodeLatLon(value));
			} else if (field.equals("longitude")) {
			    entry.setLongitude(Utils.decodeLatLon(value));
			} else if (field.equals("elevation")) {
			    entry.setAltitude(Double.parseDouble(value));
			} else if (field.equals("time")) {
			    time = value;
			} else if (field.equals("date")) {
			    String format =
				getTypeProperty("record.pattern.date.format",
						"yyyyMMdd'T'HHmmss Z");
			    SimpleDateFormat sdf =
				RepositoryUtil.makeDateFormat(format, null);
			    if(time!=null) value += " " + time;
			    Date date = sdf.parse(value);
			    entry.setStartAndEndDate(date.getTime());
			} else {
			    List<Column> columns = getColumns();
			    if (columns != null) {
				for (Column c : columns) {
				    if (c.getName().equals(field)) {
					Object[] v = getEntryValues(entry);
					c.setValue(entry, v, value);
				    }
				}
			    }
			}			    
                    }
                }

            }
	}
    }



    /**
     * _more_
     *
     * @param entry _more_
     * @param name _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public String getProperty(Entry entry, String name, String dflt) {
        try {
            if (name.equals("chart.wiki.map")) {
                Hashtable props = getRecordProperties(entry);
                if (props != null) {
                    String prop = (String) props.get(name);
                    if (prop != null) {
                        return prop;
                    }
                }
            }

            return super.getProperty(entry, name, dflt);
        } catch (Exception exc) {
            return dflt;
        }
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param services _more_
     *
     */
    @Override
    public void getServiceInfos(Request request, Entry entry,
                                List<ServiceInfo> services) {
        super.getServiceInfos(request, entry, services);
        String url;
        String dfltBbox = entry.getWest() + "," + entry.getSouth() + ","
                          + entry.getEast() + "," + entry.getNorth();


        RecordOutputHandler outputHandler = getRecordOutputHandler();
        //TODO: let the output handler add services

        /**
         * String[][] values = {
         *   { outputHandler.OUTPUT_LATLONALTCSV.toString(),
         *     "Lat/Lon/Alt CSV", ".csv", outputHandler.ICON_POINTS },
         *   { outputHandler.OUTPUT_LAS.toString(), "LAS 1.2", ".las",
         *     outputHandler.ICON_POINTS },
         *   //            {outputHandler.OUTPUT_ASC.toString(),
         *   //             "ARC Ascii Grid",
         *   //             ".asc",null},
         *   { outputHandler.OUTPUT_KMZ.toString(), ".kmz",
         *     "Google Earth KMZ", getIconUrl(request, ICON_KML) }
         * };
         *
         *
         *
         *
         * for (String[] tuple : values) {
         *   String product = tuple[0];
         *   String name    = tuple[1];
         *   String suffix  = tuple[2];
         *   String icon    = tuple[3];
         *   url = HtmlUtils.url(getRepository().URL_ENTRY_SHOW + "/"
         *                       + entry.getName() + suffix, new String[] {
         *       ARG_ENTRYID, entry.getId(), ARG_OUTPUT,
         *       outputHandler.OUTPUT_PRODUCT.getId(), ARG_PRODUCT, product,
         *       //ARG_ASYNCH, "false",
         *       //                PointOutputHandler.ARG_POINT_SKIP,
         *       //                macro(PointOutputHandler.ARG_POINT_SKIP),
         *       //                ARG_BBOX,  macro(ARG_BBOX),
         *       //                ARG_DEFAULTBBOX, dfltBbox
         *   }, false);
         *   services.add(new ServiceInfo(product, name,
         *                            request.getAbsoluteUrl(url), icon));
         * }
         *
         */
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param map _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public boolean addToMap(Request request, Entry entry, MapInfo map)
            throws Exception {
        try {
            PointOutputHandler outputHandler =
                (PointOutputHandler) getRecordOutputHandler();
            outputHandler.addToMap(request, entry, map);

            return true;
        } catch (Exception exc) {
            throw new RuntimeException(exc);
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
    @Override
    public String getMapInfoBubble(Request request, Entry entry)
            throws Exception {

        //        String fromParent = super.getMapInfoBubble(request,  entry);
        //        if(fromParent!=null) return fromParent;
        try {
            String chartType = getTypeProperty("map.chart.type", "linechart");
	    String chartArgs = getTypeProperty("map.chart.args", "");
            if ( !Utils.stringDefined(chartType)
                    || chartType.equals("none")) {
                return super.getMapInfoBubble(request, entry);
            }
            String chartField = getTypeProperty("map.chart.field", "");
            String minSizeX   = getTypeProperty("map.chart.minSizeX", "600");
            String minSizeY   = getTypeProperty("map.chart.minSizeY", "300");
            String fields = getTypeProperty("map.chart.fields",
                                            (String) null);
            StringBuilder sb   = new StringBuilder();
            String        name = getEntryDisplayName(entry);
            sb.append(
                HtmlUtils.href(
                    HtmlUtils.url(
                        request.makeUrl(getRepository().URL_ENTRY_SHOW),
                        ARG_ENTRYID, entry.getId()), name));

            String id = HtmlUtils.getUniqueId("divid_");
            sb.append(HtmlUtils.div("", HtmlUtils.id(id)));

            return Json.mapAndQuote("entryId", entry.getId(), "chartType",
                                    chartType, "chartArgs",chartArgs, "fields", chartField, "divId",
                                    id, "title", "", "text", sb.toString(),
                                    "minSizeX", minSizeX, "minSizeY",
                                    minSizeY, "vAxisMinValue", "0",
                                    "showTitle", "false", ((fields == null)
                    ? "dummy"
                    : "fields"), ((fields == null)
                                  ? ""
                                  : fields));
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }



}
