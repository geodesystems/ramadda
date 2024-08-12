/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.data.services;


import org.ramadda.data.point.PointFile;
import org.ramadda.data.point.PointMetadataHarvester;
import org.ramadda.data.record.RecordField;
import org.ramadda.data.record.RecordFile;
import org.ramadda.data.record.RecordFileFactory;
import org.ramadda.data.record.RecordVisitorGroup;
import org.ramadda.util.WikiUtil;
import org.ramadda.data.docs.*;

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
import org.ramadda.util.JsonUtil;
import org.ramadda.util.Utils;
import org.ramadda.util.geo.GeoUtils;
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
import java.util.HashSet;
import java.util.List;
import java.util.Properties;


/**
 *
 *
 * @author Jeff McWhirter
 * @version $Revision: 1.3 $
 */
@SuppressWarnings("unchecked")
public class PointTypeHandler extends RecordTypeHandler {

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
     * @param fromImport _more_
     *
     * @throws Exception On badness
     */
    @Override
    public void initializeNewEntry(Request request, Entry entry,NewType newType)
            throws Exception {

	//Check for some of the files that act like CSV but aren't
	/*
	String resource = entry.getResource().getPath();
	if(Utils.stringDefined(resource)) {
	    resource = resource.toLowerCase();
	    if(resource.endsWith(".pdf")) {
		return;
	    }
	}
	*/

        if (entry.getXmlNode() != null) {
            return;
        }

        if ( !shouldProcessResource(request, entry)) {
            return;
        }


        if (anySuperTypesOfThisType()) {
            super.initializeNewEntry(request, entry, newType);
            return;
        }



	if(!isNew(newType)) {
            return;
        }
	initializeColumns(request, entry, newType);
	addInitialMetadata(request, entry,false);

    }


    @Override
    public void addInitialMetadata(Request request, Entry entry,boolean force) throws Exception {
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
	metadataHarvester.setForce(force);
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
        ((PointTypeHandler) entry.getTypeHandler()).handleHarvestedMetadata(request,
									    pointEntry,
									    metadataHarvester);
        log("initialize new entry: done");


    }


    public String getResourcePath(Request request, Entry entry) {
	return entry.getResource().getPath();

    }


    /**
     *
     * @param request _more_
     * @param entry _more_
     * @param tag _more_
     * @param props _more_
     * @param topProps _more_
     *  @return _more_
     */
    @Override
    public String getUrlForWiki(Request request, Entry entry, String tag,
                                Hashtable props, List<String> topProps) {
        if (tag.equals(WikiConstants.WIKI_TAG_CHART)
                || tag.equals(WikiConstants.WIKI_TAG_DISPLAY)
                || tag.startsWith("display_")) {
            try {
                if (props != null) {
                    if (props.get("max") == null && props.get("lastRecords")==null) {
                        props.put("max",
                                  "" + getDefaultMax(request, entry, tag,
                                      props));
                    }
                }

                return ((PointOutputHandler) getRecordOutputHandler())
                    .getJsonUrl(request, entry, props, topProps);
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }
        }

        return super.getUrlForWiki(request, entry, tag, props, topProps);
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
	if(!type.equals("fieldname")) return;
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
            if (all.length() > 0) {
                all.append(",");
            }
            all.append(field.getName());
        }
        sb.append("&nbsp;");
        sb.append(HtmlUtils.mouseClickHref(HtmlUtils.call("RamaddaUtils.selectClick",
                HtmlUtils.comma(HtmlUtils.squote(target),
                                HtmlUtils.squote(entry.getId()),
                                HtmlUtils.squote(all.toString()),
                                HtmlUtils.squote(type))), "All Fields"));
        sb.append("<br>");

        for (RecordField field : fields) {
            sb.append("&nbsp;");
            sb.append(
                HtmlUtils.mouseClickHref(
                    HtmlUtils.call(
				   "RamaddaUtils.selectClick", HtmlUtils.comma(
                            HtmlUtils.squote(target), HtmlUtils.squote(
                                entry.getId()), HtmlUtils.squote(
                                field.getName()), HtmlUtils.squote(
                                type))), field.getLabel() + " ("
                                         + field.getName() + ")"));
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
     * @Override
     * public boolean processCommandView(org.ramadda.repository.harvester
     *       .CommandHarvester.CommandRequest cmdRequest, Entry entry,
     *           org.ramadda.repository.harvester.CommandHarvester harvester,
     *           final List<String> args, final Appendable sb,
     *           List<FileInfo> files)
     *       throws Exception {
     *   Request            request = cmdRequest.getRequest();
     *   PointOutputHandler poh =
     *       (PointOutputHandler) getRecordOutputHandler();
     *   PointEntry pointEntry = (PointEntry) poh.doMakeEntry(request, entry);
     *   File       processDir = getStorageManager().createProcessDir();
     *   File imageFile = new File(IOUtil.joinDir(processDir,
     *                        entry.getName() + "_timeseries.png"));
     *
     *   PointFormHandler.PlotInfo plotInfo = new PointFormHandler.PlotInfo();
     *   //        System.err.println ("calling makeTimeSeriesImage");
     *   long t1 = System.currentTimeMillis();
     *   BufferedImage newImage =
     *       poh.getPointFormHandler().makeTimeseriesImage(request,
     *           pointEntry, plotInfo);
     *   //        System.err.println ("Done makeTimeSeriesImage");
     *   long t2 = System.currentTimeMillis();
     *   //        System.err.println("File:  " + imageFile);
     *   ImageUtils.writeImageToFile(newImage, imageFile);
     *   long t3 = System.currentTimeMillis();
     *   //        System.err.println ("Done writeImageToFile:" + (t3-t2));
     *
     *
     *   FileInfo fileInfo = new FileInfo(imageFile);
     *   if ( !isWikiText(entry.getDescription())) {
     *       fileInfo.setDescription(entry.getDescription());
     *   }
     *   fileInfo.setTitle("Time series - " + entry.getName());
     *   files.add(fileInfo);
     *
     *
     *
     *   //Now we get the process entry id
     *   String processId = processDir.getName();
     *   String processEntryId =
     *       getStorageManager().getEncodedProcessDirEntryId(processId);
     *
     *   String entryUrl =
     *       HtmlUtils.url(
     *           request.getAbsoluteUrl(getRepository().URL_ENTRY_SHOW),
     *           ARG_ENTRYID,
     *   // Use this if you want to return the process directory
     *   //        processEntryId);
     *   getStorageManager().getEncodedProcessDirEntryId(processId + "/"
     *       + imageFile.getName()));
     *
     *   //        System.err.println("URL:" + entryUrl);
     *   return true;
     * }
     */


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
                getRepository().getExtEditor().setBoundsFromChildren(request,
                        entry.getParentEntry());
                getRepository().getExtEditor().setTimeFromChildren(request,
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
                                     Appendable formBuffer, Entry parentEntry,Entry entry,
                                     Object[] values, Hashtable state,
                                     FormInfo formInfo,
                                     TypeHandler baseTypeHandler)
            throws Exception {
        super.addColumnToEntryForm(request, column, formBuffer, parentEntry,entry,
                                   values, state, formInfo, baseTypeHandler);


        if ((entry == null) && column.getName().equals("properties")) {
            if (column.getEditable()) {
                if (baseTypeHandler.okToShowInForm(entry, "properties")) {
                    formBuffer.append(
                        HtmlUtils.formEntry(
                            msgLabel("Or upload properties"),
                            HtmlUtils.fileInput(
                                ARG_PROPERTIES_FILE, HtmlUtils.SIZE_70)));
                }
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
            boolean canParent = super.canHandleResource(path, filename);
            if (canParent) {
                return true;
            }
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

            return false;
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
    protected void handleHarvestedMetadata(Request request, RecordEntry recordEntry,
                                           PointMetadataHarvester metadata)
            throws Exception {


        PointEntry pointEntry = (PointEntry) recordEntry;
        Entry      entry      = pointEntry.getEntry();
        if (!metadata.getForce() && !shouldProcessResource(null, entry)) {
	    return;
        }

	boolean debug =getRepository().getProperty("debug.pointdata.new",entry.getTypeHandler().getTypeProperty("debug.pointdata.new",false));




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
         *                        getMetadataManager().findType(MetadataHandler.TYPE_SPATIAL_POLYGON),
         *                        DFLT_INHERITED, sb[0].toString(),
         *                        sb[1].toString(), sb[2].toString(),
         *                        sb[3].toString(), Metadata.DFLT_EXTRA);
         *       getMetadataManager().addMetadata(request,entry, polygonMetadata,
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
        values[0] = Integer.valueOf(metadata.getCount());

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

        List<RecordField> fields = metadata.getFields();
        if (fields != null && entry.getTypeHandler().getTypeProperty("addinitialmetadata",true)) {
            for (RecordField field : fields) {
                String unit = field.getUnit();
                Metadata fieldMetadata =
                    new Metadata(getRepository().getGUID(), entry.getId(),
                                 getMetadataManager().findType("thredds.variable"),
				 DFLT_INHERITED,
                                 field.getName(), field.getLabel(),
                                 (unit != null)
                                 ? unit
                                 : "", "", Metadata.DFLT_EXTRA);
                getMetadataManager().addMetadata(getRepository().getAdminRequest(),entry, fieldMetadata, false);
            }
        }


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


	List<Column> columns = getColumns();
	if (columns != null) {
	    for (Column c : columns) {
		String fieldId = c.getProperty("record_field",null);
		if(fieldId!=null) {
		    HashSet<String> samples = metadata.getSamples(fieldId);
		    if(samples!=null) {
			if(samples.size()==1) {
			    String[] svalues = samples.toArray(new String[samples.size()]);
			    Object[] v = getEntryValues(entry);
			    c.setValue(entry, v, svalues[0]);
			}
		    }
		}
	    }

	    //Check if the name was the default
	    if(entry.getTransientProperty("noname")!=null) {
		for (Column c : columns) {
		    String isDefaultName = c.getProperty("is_default_entry_name",null);
		    if(isDefaultName!=null && isDefaultName.equals("true")) {
			String name = (String) entry.getValue(request,c.getOffset());
			if(Utils.stringDefined(name)) {
			    entry.setName(name);
			}
		    }
		}
	    }
	}


        String header = pointEntry.getRecordFile().getTextHeader();
	if(debug)
	    System.err.println("HEADER:" + header);
        if ((header != null) && (header.length() > 0)
                && getTypeProperty("point.initialize", true)) {

            String patterns = (String) getTypeProperty("record.patterns",
                                  (String) null);

            if (patterns != null) {
                List<String> toks = StringUtil.split(patterns, ",");
		Hashtable state = new Hashtable();
                for (String tok : toks) {
                    List<String> toks2 = StringUtil.splitUpTo(tok, ":", 2);
                    if (toks2.size() != 2) {
                        continue;
                    }
                    String field   = toks2.get(0).trim();
                    String pattern = toks2.get(1).replace("_comma_",",").replace("_nl_","\n").replace("\\n","\n");
                    String value   = StringUtil.findPattern(header, pattern);
		    if(debug)
			System.err.println("\t" +field +" p:" + pattern.trim() +" v:" +value);
                    if (Utils.stringDefined(value)) {
			handleHeaderPatternValue(request, entry,state,field,value);
		    }
		}

            }
        }
   }


    public void handleHeaderPatternValue(Request request, Entry entry,Hashtable state, String field, String value) throws Exception {
	if (field.equals("latitude")) {
	    entry.setLatitude(decode(value));
	} else if (field.equals("longitude")) {
	    entry.setLongitude(decode(value));
	} else if (field.equals("elevation") || field.equals("altitude")) {
	    entry.setAltitude(Double.parseDouble(value));
	} else if (field.equals("time")) {
	    state.put("time",value);
	} else if (field.equals("date")) {
	    String format =
		getTypeProperty("record.pattern.date.format",
				"yyyyMMdd'T'HHmmss Z");
	    SimpleDateFormat sdf =
		RepositoryUtil.makeDateFormat(format, null);
	    String time = (String) state.get("time");
	    if (time != null) {
		value += " " + time;
	    }
	    //A hack
	    if(!value.trim().equals("none") && !value.trim().equals("")) {
		Date date = null;
		try {
		    date = sdf.parse(value);
		} catch(Exception exc){
		    date=Utils.parseDate(value);
		}
		//                      System.err.println("date:" + date);
		if(date!=null)
		    entry.setStartAndEndDate(date.getTime());
	    }
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

    

    private double decode(String lls) {
	lls = lls.trim();
        lls = lls.replace(" ", ":");
        lls = lls.replace(":S", "S");
        lls = lls.replace(":N", "N");
        lls = lls.replace(":E", "E");
        lls = lls.replace(":W", "W");
        return GeoUtils.decodeLatLon(lls);
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
        String dfltBbox = entry.getWest(request) + "," + entry.getSouth(request) + ","
                          + entry.getEast(request) + "," + entry.getNorth(request);


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



    @Override
    public String getSimpleDisplay(Request request, Hashtable props,
                                   Entry entry)
	throws Exception {
	String wikiText = getWikiText(request, entry,"simple");
	if(wikiText!=null) {
	    return null;
	}
	String chartType = getTypeProperty("map.chart.type", "linechart");
	String chartArgs = getTypeProperty("map.chart.args", "");
	if ( !Utils.stringDefined(chartType)
	     || chartType.equals("none")) {
	    return super.getSimpleDisplay(request,props,entry);
	}
	String chartField = getTypeProperty("map.chart.field", "");
	String minSizeX   = getTypeProperty("map.chart.minSizeX", "600");
	String minSizeY   = getTypeProperty("map.chart.minSizeY", "200");
	String fields = getTypeProperty("map.chart.fields",
					chartField);
	StringBuilder sb   = new StringBuilder();

	sb.append("{{display_" +chartType +" fields=\"" + fields+"\" width=" + minSizeX +" height=" + minSizeY +" " + chartArgs+"}}\n");
	String wiki= getWikiManager().wikifyEntry(request,entry,sb.toString());
	return wiki;
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

	String fromMetadata =  getBubbleTemplate(request,  entry);
	if(Utils.stringDefined(fromMetadata)) {
	    return fromMetadata;
	}

        //        String fromParent = super.getMapInfoBubble(request,  entry);
        //        if(fromParent!=null) return fromParent;
	String popup = getTypeProperty("map.popup",null);
	if(popup!=null) {
	    return  getWikiManager().wikifyEntry(request,entry,popup);
	}


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
            return JsonUtil.mapAndQuote(Utils.makeListFromValues("entryId", entry.getId(), "chartType",
                                    chartType, "chartArgs", chartArgs,
                                    "fields", chartField, "divId", id,
                                    "title", "", "text", sb.toString(),
                                    "minSizeX", minSizeX, "minSizeY",
                                    minSizeY, "vAxisMinValue", "0",
                                    "showTitle", "false", ((fields == null)
                    ? "dummy"
                    : "fields"), ((fields == null)
                                  ? ""
                                  : fields)));
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    public List<String> preprocessCsvCommands(Request request, List<String> args1) throws Exception {
	List<String> args = new ArrayList<String>();
	for (int j = 0; j < args1.size(); j++) {
	    String arg = args1.get(j);
	    String fileEntryId = null;
	    if (arg.startsWith("entry:")) {
		fileEntryId = arg.substring("entry:".length());
	    } else {
		if(arg.indexOf("entry:")>=0) {
		    //			    fileEntryId = StringUtil.findPattern(arg,"entry:[^\\s\"']+[\\s\"']");
		    fileEntryId = StringUtil.findPattern(arg,".*entry:([^\\s\"']+).*");
		    System.err.println("FOUND:" + fileEntryId +" arg:"+ arg);
		}			    
	    }
	    if(fileEntryId!=null) {
		Entry fileEntry =
		    getEntryManager().getEntry(request,fileEntryId);
		if (fileEntry == null) {
		    throw new IllegalArgumentException("Could not find " + arg);
		}
		File file = getStorageManager().getEntryFile(fileEntry);
		if (!file.exists()) {
		    throw new IllegalArgumentException("Entry not a file  " + arg);
		}
		arg = arg.replace("entry:" + fileEntryId,file.toString());
	    }
	    args.add(arg);
	}

	return args;
    }



    @Override
    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props)
            throws Exception {
        if ( !tag.equals("convertform") && !tag.equals("seesv")) {
            return super.getWikiInclude(wikiUtil, request, originalEntry,
                                        entry, tag, props);
        }
        ConvertibleOutputHandler coh =
            (ConvertibleOutputHandler) (ConvertibleOutputHandler) getRepository()
                .getOutputHandler(ConvertibleOutputHandler.class);
        StringBuilder sb = new StringBuilder();
        coh.makeConvertForm(request, entry, sb,props);

        return sb.toString();
    }



}
