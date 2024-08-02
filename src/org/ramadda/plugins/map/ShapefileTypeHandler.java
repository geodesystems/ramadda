/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.map;


import org.ramadda.data.point.text.*;
import org.ramadda.data.record.*;

import org.ramadda.data.services.PointTypeHandler;


import org.ramadda.repository.Entry;
import org.ramadda.repository.Repository;
import org.ramadda.repository.Request;
import org.ramadda.repository.map.MapInfo;

import org.ramadda.repository.output.WikiConstants;
import org.ramadda.repository.type.GenericTypeHandler;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;
import org.ramadda.util.Utils;

import org.ramadda.util.geo.*;




import org.w3c.dom.Element;

import ucar.unidata.gis.GisPart;
import ucar.unidata.gis.shapefile.DbaseData;
import ucar.unidata.gis.shapefile.DbaseFile;
import ucar.unidata.gis.shapefile.EsriShapefile;
import ucar.unidata.gis.shapefile.ProjFile;

import ucar.unidata.util.IOUtil;


import java.awt.geom.Rectangle2D;

import java.io.*;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;


/**
 */
@SuppressWarnings("unchecked")
public class ShapefileTypeHandler extends PointTypeHandler implements WikiConstants {

    /** _more_ */
    public static final String PROP_ADD_SHAPES = "addShapes";

    /** _more_ */
    public static final String PROP_ADD_POINTS = "addPoints";


    /** _more_ */
    public static final String PROP_FIELDS = "fields";

    /** _more_ */
    public static final String PROP_FIELDS_NOGEO = "fieldsNoGeo";

    /** _more_ */
    public static final String PROP_FIELDS_WITHSHAPES = "fieldsWithShapes";

    /** _more_ */
    public static final String PROP_FIELDS_WITHPOINTS = "fieldsWithPoints";




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

    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public EsriShapefile getShapefile(Entry entry) throws Exception {
        return getOutputHandler().getShapefile(entry);
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     */
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
        super.initializeEntryFromForm(request, entry, parent, newEntry);


        if ( !newEntry) {
            return;
        }
        if ( !entry.isFile()) {
	    //            System.err.println("Shapefile not a file");
	    //            return;
        }
        EsriShapefile shapefile = null;
        try {
            shapefile = getShapefile(entry);
        } catch (Exception exc) {
            System.err.println("Error opening shapefile:" + exc);

            return;
        }

        ShapefileRecordFile recordFile =
            new ShapefileRecordFile(getRepository().getTmpRequest(), entry,
                                    getRecordProperties(entry),
                                    new IO.Path(entry.getResourcePath(request)), shapefile);
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
            Hashtable props  = getRecordProperties(entry);
            String    fields = (String) props.get(PROP_FIELDS);
            if (fields == null) {
                return;
            }
            StringBuilder sb = new StringBuilder("<h2>Fields</h2><ul>");
            for (String field : fields.split(",")) {
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
	    //            return true;
        }
        int numPoints = entry.getIntValue(request,IDX_RECORD_COUNT, -1);

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
            getEntryValues(entry)[IDX_RECORD_COUNT] = Integer.valueOf(numPoints);
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
	List<String> styles = new ArrayList<String>();
	ShapefileOutputHandler.makeMapStyle(request, entry,styles);
        map.addKmlUrl(entry.getName(), kmlUrl, true,JU.map(styles));
                      

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
    public boolean addToMapSelector(Request request, Entry entry, Entry forEntry, MapInfo map)
            throws Exception {
        if (entry != null) {
	    String url =
		request.entryUrl(getRepository()
				 .URL_ENTRY_SHOW, entry, ARG_OUTPUT,
				 ShapefileOutputHandler.OUTPUT_GEOJSON
				 .toString(), "formap", "true");
	    List<String> styles = new ArrayList<String>();
	    ShapefileOutputHandler.makeMapStyle(request, entry,styles);
	    map.addGeoJsonUrl(entry.getName(), url, true,JU.map(styles));

	}
        return super.addToMapSelector(request, entry, forEntry, map);
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param tag _more_
     * @param props _more_
     * @param topProps _more_
     *
     * @return _more_
     */
    @Override
    public String getUrlForWiki(Request request, Entry entry, String tag,
                                Hashtable props, List<String> topProps) {
        if (tag.equals(WikiConstants.WIKI_TAG_CHART)
                || tag.equals(WikiConstants.WIKI_TAG_DISPLAY)
                || tag.startsWith("display_")) {
            try {
                String url = super.getUrlForWiki(request, entry, tag, props,
                                 topProps);
                if (Utils.getProperty(props, PROP_ADD_SHAPES, false)) {
                    url += "&" + PROP_ADD_SHAPES + "=true";
                } else if (Utils.getProperty(props, PROP_ADD_POINTS, false)) {
                    url += "&" + PROP_ADD_POINTS + "=true";
                }

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
    public String getCacheFileName(Request request, Entry entry)
            throws Exception {
        String file = super.getCacheFileName(request, entry);
        if (request.get(PROP_ADD_SHAPES, false)) {
            file = "shapes_true_" + file;
        } else if (request.get(PROP_ADD_POINTS, false)) {
            file = "points_true_" + file;
        }

        return file;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param properties _more_
     * @param requestProperties _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override

    public RecordFile doMakeRecordFile(Request request, Entry entry,
                                       Hashtable properties,
                                       Hashtable requestProperties)
            throws Exception {
        return new ShapefileRecordFile(request, entry, properties,
                                       new IO.Path(entry.getResourcePath(request)), null);
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private ShapefileOutputHandler getOutputHandler() throws Exception {
        return (ShapefileOutputHandler) getRepository().getOutputHandler(
            ShapefileOutputHandler.class);
    }


    /**
     * Class description
     *
     *
     * @version        $version$, Sat, Dec 8, '18
     * @author         Enter your name here...
     */
    public class ShapefileRecordFile extends CsvFile {

        /** _more_ */
        Request request;

        /** _more_ */
        Entry entry;

        /** _more_ */
        EsriShapefile shapefile;

        /** _more_ */
        Hashtable props;



        /** _more_ */
        String fields;


        /** _more_ */
        String fieldsNoGeo;


        /** _more_ */
        String fieldsWithShapes;

        /** _more_ */
        String fieldsWithPoints;


        /** _more_ */
        boolean addShapes;

        /** _more_ */
        boolean addPoints;

        /**
         * _more_
         *
         *
         * @param request _more_
         * @param entry _more_
         * @param entryProperties _more_
         * @param filename _more_
         * @param shapefile _more_
         * @throws Exception _more_
         */
        public ShapefileRecordFile(Request request, Entry entry,
                                   Hashtable entryProperties,
                                   IO.Path path, EsriShapefile shapefile)
                throws Exception {
            super(path);
            props          = entryProperties;
            this.addShapes = request.get(PROP_ADD_SHAPES, false);
            this.addPoints = request.get(PROP_ADD_POINTS, false);
            this.request   = request;
            this.entry     = entry;
            this.shapefile = shapefile;
            fieldsNoGeo    = (String) props.get(PROP_FIELDS_NOGEO);
            if (fieldsNoGeo != null) {
                fieldsWithShapes =
                    fieldsNoGeo
                    + ",shapeType[ type=string],shape[type=string]";
                fieldsWithPoints =
                    fieldsNoGeo
                    + ",latitude[type=double],longitude[type=double]";
            }
            if (fieldsWithShapes == null) {
                getEntryFieldsProperties();
                fieldsWithShapes =
                    fieldsNoGeo
                    + ",shapeType[ type=string],shape[type=string]";
                fieldsWithPoints =
                    fieldsNoGeo
                    + ",latitude[type=double],longitude[type=double]";

            }
        }



        /**
         * _more_
         *
         * @return _more_
         */
        @Override
        public boolean shouldCreateCsvFile() {
            return true;
        }

        /**
         * _more_
         *
         * @param file _more_
         * @param fos _more_
         * @param buffered _more_
         * @param commands _more_
         *
         * @throws Exception _more_
         */
        @Override
        protected void doCreateCsvFile(File file, OutputStream fos,
                                       boolean buffered,
                                       List<String> commands)
                throws Exception {
            if (shapefile == null) {
                shapefile = getShapefile(entry);
            }
            getOutputHandler().getCsvBuffer(request, fos, entry, shapefile,
                                            false, addPoints, addShapes);
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
        @Override
        public VisitInfo prepareToVisit(VisitInfo visitInfo)
                throws Exception {
            putProperty(PROP_SKIPLINES, "1");
            super.prepareToVisit(visitInfo);
            if (addShapes && (fieldsWithShapes != null)) {
                putProperty(PROP_FIELDS, fieldsWithShapes);
            } else if (addPoints && (fieldsWithPoints != null)) {
                putProperty(PROP_FIELDS, fieldsWithPoints);
            } else if (fieldsNoGeo != null) {
                putProperty(PROP_FIELDS, fieldsNoGeo);
            } else {
                putProperty(PROP_FIELDS, makeFields(addPoints, addShapes));
            }

            return visitInfo;
        }

        /**
         * _more_
         *
         *
         * @param addPoints _more_
         * @param addShapes _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        private String makeFields(boolean addPoints, boolean addShapes)
                throws Exception {
            List<String[]> fileFields = getFields(request, entry);
            List<String>   fields     = new ArrayList<String>();
            this.fields = null;
            for (String[] tuple : fileFields) {
                if (this.fields != null) {
                    this.fields += ",";
                } else {
                    this.fields = "";
                }
                this.fields += tuple[0];
                fields.add(makeField(tuple[0], attrType(tuple[1])));
            }
            if (addShapes) {
                fields.add(makeField("shapeType", attrType("string")));
                fields.add(makeField("shape", attrType("string")));
            } else if (addPoints) {
                fields.add(makeField("latitude", attrType("double")));
                fields.add(makeField("longitude", attrType("double")));
            }

            return makeFields(fields);
        }



        /**
         * _more_
         *
         * @param failureOk _more_
         *
         * @return _more_
         */
        @Override
        public List<RecordField> doMakeFields(boolean failureOk) {
            try {
                if (fieldsNoGeo == null) {
                    getEntryFieldsProperties();
                }
                if (addShapes) {
                    putProperty(PROP_FIELDS, fieldsWithShapes);
                } else if (addPoints) {
                    putProperty(PROP_FIELDS, fieldsWithPoints);
                } else {
                    putProperty(PROP_FIELDS, fieldsNoGeo);
                }

                return super.doMakeFields(failureOk);
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }
        }


        /**
         * _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        public String getEntryFieldsProperties() throws Exception {
            fieldsNoGeo = makeFields(false, false);
            String result = "#fields for data access. do not change\n"
                            + PROP_FIELDS_NOGEO + "=" + fieldsNoGeo + "\n";

            return result;
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
        public List<String[]> getFields(Request request, Entry entry)
                throws Exception {
            List<String[]> fields = new ArrayList<String[]>();
            if (shapefile == null) {
                shapefile = getShapefile(entry);
            }
            DbaseFile dbfile = shapefile.getDbFile();
            if (dbfile == null) {
                return fields;
            }
            List<DbaseDataWrapper> fieldDatum =
                getOutputHandler().getDatum(request, entry, dbfile);
            for (DbaseDataWrapper dbd : fieldDatum) {
                String type = "string";
                if (dbd.getType() == DbaseData.TYPE_NUMERIC) {
                    type = "double";
                }
                fields.add(new String[] { dbd.getName(), type });
            }

            return fields;
        }



    }


}
