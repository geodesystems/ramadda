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

package org.ramadda.plugins.map;


import org.ramadda.repository.Entry;
import org.ramadda.repository.Repository;
import org.ramadda.repository.Request;
import org.ramadda.repository.map.MapInfo;

import org.ramadda.repository.output.WikiConstants;
import org.ramadda.repository.type.GenericTypeHandler;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;

import org.ramadda.data.services.PointTypeHandler;
import org.ramadda.data.point.text.*;
import org.ramadda.data.record.*;




import org.w3c.dom.Element;

import ucar.unidata.gis.GisPart;

import ucar.unidata.gis.shapefile.DbaseData;
import ucar.unidata.gis.shapefile.DbaseFile;
import ucar.unidata.gis.shapefile.EsriShapefile;
import ucar.unidata.gis.shapefile.ProjFile;
import org.ramadda.util.TTLCache;


import java.awt.geom.Rectangle2D;
import java.io.*;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;


/**
 */
public class ShapefileTypeHandler extends PointTypeHandler implements WikiConstants {

	public static final String PROP_FIELDS = "fields";

	public static final String PROP_FIELDS_WITHSHAPES = "fieldsWithShapes";
	public static final String PROP_FIELDS_WITHOUTSHAPES = "fieldsWithoutShapes";	



    /** _more_ */
    public static int MAX_POINTS = 200000;


    /** _more_ */
    private static final int IDX_LON = 0;

    /** _more_ */
    private static final int IDX_LAT = 1;



    /**
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     * @throws Exception _more_
     */
    public ShapefileTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);

    }

    public EsriShapefile getShapefile(Entry entry) throws Exception {
	return getOutputHandler().getShapefile(entry);
    }



    public boolean shouldProcessResource(Request request, Entry entry) {
        return false;
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
    public void initializeEntryFromForm(Request request, Entry entry,
                                        Entry parent, boolean newEntry)
            throws Exception {
	super.initializeEntryFromForm(request,  entry,parent, newEntry);


        if ( !newEntry) {
            return;
        }
        if ( !entry.isFile()) {
            System.err.println("Shapefile not a file");
            return;
        }
        EsriShapefile shapefile = null;
        try {
            shapefile = getShapefile(entry);
        } catch (Exception exc) {
            System.err.println("Error opening shapefile:" + exc);
            return;
        }

	ShapefileRecordFile recordFile =  new ShapefileRecordFile(request,entry,entry.getResource().getPath(),shapefile);
	String props = recordFile.getEntryFieldsProperties();
	getEntryValues(entry)[IDX_PROPERTIES] = props;

        Rectangle2D bounds   = shapefile.getBoundingBox();
        double[][]  lonlat   = new double[][] {
            { bounds.getX() }, { bounds.getY() + bounds.getHeight() }
        };
        ProjFile    projFile = shapefile.getProjFile();
        if (projFile != null) {
            lonlat = projFile.convertToLonLat(lonlat);
        }
        entry.setNorth(lonlat[IDX_LAT][0]);
        entry.setWest(lonlat[IDX_LON][0]);
        lonlat[IDX_LAT][0] = bounds.getY();
        lonlat[IDX_LON][0] = bounds.getX() + bounds.getWidth();
        if (projFile != null) {
            lonlat = projFile.convertToLonLat(lonlat);
        }
        entry.setSouth(lonlat[IDX_LAT][0]);
        entry.setEast(lonlat[IDX_LON][0]);
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param firstCall _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void initializeEntryFromHarvester(Request request, Entry entry,
                                             boolean firstCall)
            throws Exception {
        super.initializeEntryFromHarvester(request, entry, firstCall);
        if (firstCall) {
            initializeEntryFromForm(request, entry, null, true);
        }
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * 
     * @throws Exception _more_
     */
    @Override
    public void metadataChanged(Request request, Entry entry)
            throws Exception {
        super.metadataChanged(request, entry);
        getEntryManager().updateEntry(request, entry);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param tabTitles _more_
     * @param tabContents _more_
     */
    public void addToInformationTabs(Request request, Entry entry,
                                     List<String> tabTitles,
                                     List<String> tabContents) {
        super.addToInformationTabs(request, entry, tabTitles, tabContents);
        try {
	    Hashtable props = getRecordProperties(entry);
	    String fields = (String) props.get(PROP_FIELDS);
	    if(fields==null) return;
            StringBuilder sb = new StringBuilder("<h2>Fields</h2><ul>");
            for (String field: fields.split(",")) {
                sb.append("<li>");
                sb.append(field);
		//                sb.append(" (");             sb.append(")\n");
            }
            sb.append("</ul>");
            tabTitles.add("Shapefile Fields");
            tabContents.add(sb.toString());
        } catch (Exception exc) {
            tabTitles.add("Shapefile Error");
            tabContents.add("Error opening shapefile:" + exc);
        }
    }


    /**
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
        if ( !entry.isFile()) {
            return true;
        }
        int numPoints = entry.getValue(IDX_RECORD_COUNT, -1);

        if (numPoints < 0) {
            long t1 = System.currentTimeMillis();
            //TODO: stream through the shapes
            EsriShapefile shapefile = null;
            try {
                shapefile = getShapefile(entry);
            } catch (Exception exc) {
                map.setHeaderMessage("Error opening shapefile:" + exc);

                return true;
            }
            List<EsriShapefile.EsriFeature> features =
                (List<EsriShapefile.EsriFeature>) shapefile.getFeatures();
            numPoints = 0;
            int numFeatures = 0;
            for (EsriShapefile.EsriFeature feature : features) {
                numPoints += feature.getNumPoints();
                numFeatures++;
            }
            long t2 = System.currentTimeMillis();
            getEntryValues(entry)[IDX_RECORD_COUNT] = new Integer(numPoints);
            getEntryManager().updateEntry(request, entry);
        }

        String kmlUrl =
            request.entryUrl(getRepository().URL_ENTRY_SHOW, entry,
                             ARG_OUTPUT,
                             ShapefileOutputHandler.OUTPUT_KML.toString(),
                             "formap", "true");
        String fields = request.getString(ATTR_SELECTFIELDS,
                                          map.getSelectFields());
        if (fields != null) {
            kmlUrl += "&mapsubset=true&"
                      + HtmlUtils.arg("selectFields", fields, false);
        }
        String bounds = map.getSelectBounds();
        if (bounds != null) {
            kmlUrl += "&selectBounds=" + bounds;
        }
        map.addKmlUrl(entry.getName(), kmlUrl, true,
                      ShapefileOutputHandler.makeMapStyle(request, entry));

        /*  For testing
        map.addGeoJsonUrl(
            entry.getName(),
            request.entryUrl(
                getRepository().URL_ENTRY_SHOW, entry, ARG_OUTPUT,
                ShapefileOutputHandler.OUTPUT_GEOJSON.toString()), true,null);
        */
        return false;
    }


    @Override
    public String getUrlForWiki(Request request, Entry entry, String tag,
                                Hashtable props, List<String> topProps) {
        if (tag.equals(WikiConstants.WIKI_TAG_CHART)
                || tag.equals(WikiConstants.WIKI_TAG_DISPLAY)
                || tag.startsWith("display_")) {
            try {
		String url = super.getUrlForWiki(request, entry, tag, props, topProps);
		url+="&addShapes=" +  Utils.getProperty(props,"addShapes",false);
		return url;
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
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public RecordFile doMakeRecordFile(Request request, Entry entry)
            throws Exception {
        return new ShapefileRecordFile(request,entry,entry.getResource().getPath(),null);
    }


    private ShapefileOutputHandler getOutputHandler() throws Exception {
	return (ShapefileOutputHandler) getRepository().getOutputHandler(ShapefileOutputHandler.class);
    }

    
    /**
     * Class description
     *
     *
     * @version        $version$, Sat, Dec 8, '18
     * @author         Enter your name here...
     */
    public class  ShapefileRecordFile extends CsvFile {

	Request  request;
	Entry entry;
	EsriShapefile shapefile;
	Hashtable props;
	String 	    fieldsWithoutShapes;
	String 	    fieldsWithShapes;
	String 	    fields;	

        /**
         * _more_
         *
         * @param filename _more_
         *
         * @throws IOException _more_
         */
	public  ShapefileRecordFile(Request  request, Entry entry, String filename, EsriShapefile shapefile) throws Exception {
            super(filename);
	    //Get the properties from the entry
	    props = getRecordProperties(entry);
	    this.request = request;
	    this.entry = entry;
	    this.shapefile  = shapefile;
	    fieldsWithShapes = (String) props.get(PROP_FIELDS_WITHSHAPES);
	    fieldsWithoutShapes = (String) props.get(PROP_FIELDS_WITHOUTSHAPES);
	    fields = (String) props.get(PROP_FIELDS);	    	    
	    if(fieldsWithShapes==null) {
		getEntryFieldsProperties();
	    }
        }



        /**
         * _more_
         *
         * @param buffered _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
	 */
        @Override
        public InputStream doMakeInputStream(boolean buffered)
                throws Exception {
	    if(shapefile ==null) {
		shapefile = getShapefile(entry);
	    }
	    //	    new RuntimeException("").printStackTrace();
	    StringBuilder sb = getOutputHandler().getCsvBuffer(request,  entry, shapefile, false,request.get("addShapes",false));
	    ByteArrayInputStream bais = new ByteArrayInputStream(sb.toString().getBytes());
	    return bais;
        }

        /**
         * _more_
         *
         * @param visitInfo _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        public VisitInfo prepareToVisit(VisitInfo visitInfo)
                throws Exception {
	    putProperty(PROP_SKIPLINES,"1");
            super.prepareToVisit(visitInfo);
	    boolean addShapes = request.get("addShapes",false);
	    putProperty(PROP_FIELDS, makeFields(addShapes));
            return visitInfo;
        }

	private String makeFields(boolean addShapes) throws Exception {
	    List<String[]> fileFields = getFields(request, entry);
	    List<String> fields = new ArrayList<String>();
	    this.fields = null;
	    for(String[] tuple: fileFields) {
		if(this.fields!=null)
		    this.fields+=",";
		else
		    this.fields = "";
		this.fields+=tuple[0];
		fields.add(makeField(tuple[0], attrType(tuple[1])));
	    }
	    if(addShapes) {
		fields.add(makeField("shapeType", attrType("string")));
		fields.add(makeField("shape", attrType("string")));
	    }
	    return  makeFields(fields);
	}	    



	@Override
	public List<RecordField> doMakeFields(boolean failureOk)  {
	    try {
	    if(fieldsWithoutShapes==null) {
		getEntryFieldsProperties();
	    }		
	    boolean addShapes = request.get("addShapes",false);
	    if(addShapes) {
		putProperty(PROP_FIELDS, fieldsWithShapes);
	    } else {
		putProperty(PROP_FIELDS, fieldsWithoutShapes);
	    }
	    return super.doMakeFields(failureOk);
	    } catch(Exception exc) {
		throw new RuntimeException(exc);
	    }
	}


	public String getEntryFieldsProperties() throws Exception {
	    fieldsWithShapes =  makeFields(true);
	    fieldsWithoutShapes =  makeFields(false);	    
	    String result =  "#fields for data access. do not change\n" + PROP_FIELDS+"=" + fields+"\n"+PROP_FIELDS_WITHSHAPES + "="+fieldsWithShapes+"\n" + PROP_FIELDS_WITHOUTSHAPES+"=" + fieldsWithoutShapes+"\n";	    
	    return result;
	}

	public List<String[]> getFields(Request request, Entry entry) throws Exception {
	    List<String[]> fields =new ArrayList<String[]>();
	    if(shapefile == null) {
		shapefile =   getShapefile(entry);
	    }
	    DbaseFile     dbfile   = shapefile.getDbFile();
	    if (dbfile == null) {
		return fields;
	    }	    
	    List<DbaseDataWrapper> fieldDatum = getOutputHandler().getDatum(request, entry, dbfile);
	    for (DbaseDataWrapper dbd : fieldDatum) {
		String type = "string";
		if (dbd.getType() == DbaseData.TYPE_NUMERIC) {
		    type = "double";
		}
		fields.add(new String[]{dbd.getName(),type});
	    }
	    return fields;
	}



    }


}
