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

package org.ramadda.plugins.map;


import org.ramadda.repository.Entry;
import org.ramadda.repository.Link;
import org.ramadda.repository.Repository;
import org.ramadda.repository.Request;
import org.ramadda.repository.Result;
import org.ramadda.repository.metadata.Metadata;
import org.ramadda.repository.output.JsonOutputHandler;
import org.ramadda.repository.output.KmlOutputHandler;
import org.ramadda.repository.output.OutputHandler;
import org.ramadda.repository.output.OutputType;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Json;
import org.ramadda.util.KmlUtil;
import org.ramadda.util.Utils;

import org.w3c.dom.Element;

import ucar.unidata.gis.GisPart;
import ucar.unidata.gis.shapefile.DbaseData;
import ucar.unidata.gis.shapefile.DbaseFile;
import ucar.unidata.gis.shapefile.EsriShapefile;
import ucar.unidata.util.IOUtil;
import ucar.unidata.xml.XmlUtil;

import java.text.DecimalFormat;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * Class to handle the output of shapefiles
 */
public class ShapefileOutputHandler extends OutputHandler {


    /** Map output type */
    public static final OutputType OUTPUT_KML =
        new OutputType("Convert Shapefile to KML", "shapefile.kml",
                       OutputType.TYPE_FILE, "", ICON_KML);



    /** Map output type */
    public static final OutputType OUTPUT_GEOJSON =
        new OutputType("Convert Shapefile to GeoJSON", "geojson.geojson",
                       OutputType.TYPE_FILE, "", ICON_GEOJSON);


    /** _more_ */
    public static final OutputType OUTPUT_FIELDS_LIST =
        new OutputType("Shapefile Fields", "shapefile.fields_list",
                       OutputType.TYPE_VIEW, "", ICON_TABLE);

    /** _more_ */
    public static final OutputType OUTPUT_FIELDS_TABLE =
        new OutputType("Shapefile Table", "shapefile.fields_table",
                       OutputType.TYPE_VIEW, "", ICON_TABLE);


    /**
     * Create a ShapefileOutputHandler
     *
     * @param repository  the repository
     * @param element     the Element
     * @throws Exception  problem generating handler
     */
    public ShapefileOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        addType(OUTPUT_KML);
        addType(OUTPUT_GEOJSON);
        addType(OUTPUT_FIELDS_LIST);
        addType(OUTPUT_FIELDS_TABLE);
    }



    /**
     * Get the entry links
     *
     * @param request  the Request
     * @param state    the repository State
     * @param links    the links
     *
     * @throws Exception  problem creating links
     */
    public void getEntryLinks(Request request, State state, List<Link> links)
            throws Exception {
        if ((state.entry != null)
                && state.entry.getTypeHandler().isType("geo_shapefile")) {
            links.add(makeLink(request, state.entry, OUTPUT_KML));
            links.add(makeLink(request, state.entry, OUTPUT_GEOJSON));
            links.add(makeLink(request, state.entry, OUTPUT_FIELDS_LIST));
            links.add(makeLink(request, state.entry, OUTPUT_FIELDS_TABLE));
        }
    }


    /**
     * Output the entry
     *
     * @param request      the Request
     * @param outputType   the type of output
     * @param entry        the Entry to output
     *
     * @return  the Result
     *
     * @throws Exception  problem outputting entry
     */
    public Result outputEntry(Request request, OutputType outputType,
                              Entry entry)
            throws Exception {
        if (outputType.equals(OUTPUT_KML)) {
            return outputKml(request, entry);
        } else if (outputType.equals(OUTPUT_GEOJSON)) {
            return outputGeoJson(request, entry);
        } else if (outputType.equals(OUTPUT_FIELDS_LIST)) {
            return outputFields(request, entry, false);
        } else if (outputType.equals(OUTPUT_FIELDS_TABLE)) {
            return outputFields(request, entry, true);
        }

        return null;
    }

    /**
     * Make a FeatureCollection from the entry
     *
     * @param request  the Request
     * @param entry    the Entry
     *
     * @return a FeatureCollection
     *
     * @throws Exception  problems
     */
    private FeatureCollection makeFeatureCollection(Request request,
            Entry entry)
            throws Exception {

        EsriShapefile shapefile =
            new EsriShapefile(entry.getFile().toString());
        DbaseFile   dbfile     = shapefile.getDbFile();
        String      nameField  = null;
        DbaseData[] dbd        = null;
        String[]    fieldNames = null;

        String      schemaName;
        String      schemaId;

        HashMap     props = new HashMap<String, Object>();
        props.put("dbfile", dbfile);
        props.put("shapefile", shapefile);
        Metadata colorBy = null;
        List<Metadata> metadataList =
            getMetadataManager().findMetadata(request, entry,
                "shapefile_color", true);
        if ((metadataList != null) && (metadataList.size() > 0)) {
            colorBy = metadataList.get(0);
            props.put("colorby", colorBy);
        }

        metadataList = getMetadataManager().findMetadata(request, entry,
                "shapefile_display", true);

        String balloonTemplate = null;
        if ((metadataList != null) && (metadataList.size() > 0)) {
            Metadata kmlDisplay = metadataList.get(0);
            schemaName      = schemaId = kmlDisplay.getAttr1();
            balloonTemplate = kmlDisplay.getAttr2();
            if ( !Utils.stringDefined(balloonTemplate)) {
                balloonTemplate = null;
            }
            if (Utils.stringDefined(kmlDisplay.getAttr3())) {
                props.put("lineColor", kmlDisplay.getAttr3());
            }
            if (Utils.stringDefined(kmlDisplay.getAttr4())) {
                props.put("fillColor", kmlDisplay.getAttr4());
            }
        } else {
            schemaId = schemaName = entry.getId();
            /*IOUtil.getFileTail(
                                            IOUtil.stripExtension(
                                                                  entry.getResource().getPath()));
            schemaId = "S_" + schemaName + System.currentTimeMillis()
                + "_" + (int) (Math.random() * 1000);
            */
        }
        if (dbfile != null) {
            fieldNames = dbfile.getFieldNames();
            int nfld = fieldNames.length;
            // look for a field with "NAME in it
            dbd = new DbaseData[nfld];
            int firstChar = -1;
            for (int field = 0; field < nfld; field++) {
                dbd[field] = dbfile.getField(field);
                if (dbd[field].getType() == DbaseData.TYPE_CHAR) {
                    if (firstChar < 0) {
                        firstChar = field;
                    }
                    if (fieldNames[field].indexOf("NAME") >= 0) {
                        if (nameField == null) {
                            nameField = fieldNames[field];
                        }
                    }
                }
            }
            if ((nameField == null) && (firstChar >= 0)) {
                nameField = fieldNames[firstChar];
            }
        }



        props.putAll(getRepository().getPluginProperties());

        if (dbfile != null) {
            props.put(FeatureCollection.PROP_SCHEMANAME, schemaName);
            props.put(FeatureCollection.PROP_SCHEMAID, schemaId);
            props.put(FeatureCollection.PROP_STYLEID, schemaId);
            if (balloonTemplate != null) {
                props.put(FeatureCollection.PROP_BALLOON_TEMPLATE,
                          balloonTemplate);
            }
            HashMap<String, String[]> schema = new HashMap<String,
                                                   String[]>();
            for (int i = 0; i < dbd.length; i++) {
                DbaseData data  = dbd[i];
                String[]  attrs = new String[4];
                String    dtype = null;
                switch (data.getType()) {

                  case DbaseData.TYPE_BOOLEAN :
                      dtype = "bool";

                      break;

                  case DbaseData.TYPE_CHAR :
                      dtype = "string";

                      break;

                  case DbaseData.TYPE_NUMERIC :
                      dtype = "double";

                      break;
                }
                attrs[0] = KmlUtil.ATTR_TYPE;
                attrs[1] = dtype;
                attrs[2] = KmlUtil.ATTR_NAME;
                attrs[3] = fieldNames[i];
                schema.put(fieldNames[i], attrs);
            }
            props.put(FeatureCollection.PROP_SCHEMA, schema);
        }
        FeatureCollection fc = new FeatureCollection(entry.getName(),
                                   entry.getDescription(),
                                   (HashMap<String, Object>) props);

        List          features   = shapefile.getFeatures();
        List<Feature> fcfeatures = new ArrayList<Feature>(features.size());
        for (int i = 0; i < features.size(); i++) {
            EsriShapefile.EsriFeature gf =
                (EsriShapefile.EsriFeature) features.get(i);
            String type = getGeometryType(gf, gf.getNumParts());
            if (type == null) {
                System.err.println("Can't handle feature type "
                                   + gf.getClass().toString());

                continue;
            }
            List<float[][]> parts =
                new ArrayList<float[][]>(gf.getNumParts());
            java.util.Iterator pi = gf.getGisParts();
            while (pi.hasNext()) {
                GisPart  gp = (GisPart) pi.next();
                double[] xx = gp.getX();
                double[] yy = gp.getY();
                // TODO:  Why are we down casting to floats?
                // pts is in x,y order
                float[][] pts = new float[2][xx.length];
                for (int ptIdx = 0; ptIdx < xx.length; ptIdx++) {
                    pts[0][ptIdx] = (float) xx[ptIdx];
                    pts[1][ptIdx] = (float) yy[ptIdx];
                }
                parts.add(pts);
            }
            Geometry geom = new Geometry(type, parts);
            String   name = "";
            if (dbfile != null) {
                if (nameField != null) {
                    name = dbfile.getField(nameField).getString(i).trim();
                }
            }
            HashMap<String, Object> fprops = new HashMap<String, Object>();
            if (dbfile != null) {
                fprops.put(FeatureCollection.PROP_SCHEMANAME, schemaName);
                fprops.put(FeatureCollection.PROP_SCHEMAID, schemaId);
                HashMap<String, Object> schemaData =
                    new HashMap<String, Object>(fieldNames.length);
                for (int j = 0; j < fieldNames.length; j++) {
                    // since shapefile parser makes no distinction between ints & doubles,
                    // this hack will fix that.
                    Object data = dbd[j].getData(i);
                    if (data instanceof Double) {
                        double d = ((Double) data).doubleValue();
                        if ((int) d == d) {
                            data = new Integer((int) d);
                        }
                    }
                    schemaData.put(fieldNames[j], data);
                }
                fprops.put(FeatureCollection.PROP_SCHEMADATA, schemaData);
            }
            Feature feature = new Feature(name, geom, fprops, props);
            fcfeatures.add(feature);
        }
        fc.setFeatures(fcfeatures);
        //System.out.println(fc.toGeoJson());

        return fc;

    }

    /**
     * _more_
     *
     * @param type _more_
     *
     * @return _more_
     */
    public static String getTypeName(int type) {
        switch (type) {

          case DbaseData.TYPE_BOOLEAN :
              return "bool";

          case DbaseData.TYPE_CHAR :
              return "string";

          case DbaseData.TYPE_NUMERIC :
              return "double";
        }

        return "unknown:" + type;
    }

    /**
     * Output the shapefile entry as KML
     *
     * @param request  the request
     * @param entry  the entry
     *
     * @return the KML
     *
     * @throws Exception _more_
     */
    private Result outputKml(Request request, Entry entry) throws Exception {
        FeatureCollection fc   = makeFeatureCollection(request, entry);
        Element           root = fc.toKml();
        StringBuffer      sb   = new StringBuffer(XmlUtil.XML_HEADER);
        sb.append(XmlUtil.toString(root));
        Result result = new Result("", sb, KmlOutputHandler.MIME_KML);
        result.setReturnFilename(
            IOUtil.stripExtension(getStorageManager().getFileTail(entry))
            + ".kml");

        return result;
    }

    /**
     * Output the shapefile entry as GeoJSON
     *
     * @param request  the request
     * @param entry  the entry
     *
     * @return the GeoJSON
     *
     * @throws Exception _more_
     */
    private Result outputGeoJson(Request request, Entry entry)
            throws Exception {
        FeatureCollection fc     = makeFeatureCollection(request, entry);
        StringBuffer      sb     = new StringBuffer(fc.toGeoJson());
        Result            result = new Result("", sb, Json.MIMETYPE);
        result.setReturnFilename(
            IOUtil.stripExtension(getStorageManager().getFileTail(entry))
            + ".geojson");

        return result;
    }


    /**
     * Output the shapefile entry as GeoJSON
     *
     * @param request  the request
     * @param entry  the entry
     * @param table _more_
     *
     * @return the GeoJSON
     *
     * @throws Exception _more_
     */
    private Result outputFields(Request request, Entry entry, boolean table)
            throws Exception {
        DecimalFormat format = new DecimalFormat("####0.00");
        EsriShapefile shapefile =
            new EsriShapefile(entry.getFile().toString());
        DbaseFile     dbfile = shapefile.getDbFile();
        StringBuilder sb     = new StringBuilder();
        getPageHandler().entrySectionOpen(request, entry, sb, table
                ? "Shapefile Table"
                : "Shapefile Fields");
        if (dbfile == null) {
            sb.append("No fields");
            getPageHandler().entrySectionClose(request, entry, sb);

            return new Result("", sb);
        }

        String[] fieldNames = dbfile.getFieldNames();
        if (table) {
            sb.append("<table border=1>");
            sb.append("<tr>");
            sb.append(HtmlUtils.td(HtmlUtils.b("Field #"),
                                   HtmlUtils.style("padding:5px;")));
            for (int j = 0; j < fieldNames.length; j++) {
                sb.append(HtmlUtils.td(HtmlUtils.b(fieldNames[j]),
                                       HtmlUtils.style("padding:5px;")));
            }
            sb.append("</tr>");
        } else {
            sb.append("<ul>\n");
        }


        List features = shapefile.getFeatures();
        for (int i = 0; i < features.size(); i++) {
            if (i > 500) {
                if (table) {
                    sb.append("<tr>");
                    sb.append(HtmlUtils.td("...",
                                           HtmlUtils.style("padding:5px;")));
                    sb.append("</tr>");
                } else {
                    sb.append("<li>...");
                }

                break;
            }

            if (table) {
                sb.append("<tr>");
                sb.append(HtmlUtils.td("" + (i + 1),
                                       HtmlUtils.style("padding:5px;")));
            } else {
                sb.append("<li> ");
                sb.append("Field #" + (i + 1));
                sb.append("<ul>\n");
            }
            for (int j = 0; j < fieldNames.length; j++) {
                DbaseData field = dbfile.getField(j);
                String    value;
                if (field.getType() == field.TYPE_NUMERIC) {
                    value = format.format(field.getDouble(i));
                } else {
                    value = "" + field.getData(i);
                }

                if (table) {
                    sb.append(HtmlUtils.td(value,
                                           HtmlUtils.style("padding:5px;")));
                } else {
                    sb.append("<li><b>");
                    sb.append(fieldNames[j]);
                    sb.append("</b>: ");
                    sb.append(value);
                }
            }
            if (table) {
                sb.append("</tr>");
            } else {
                sb.append("</ul>\n");
            }
        }
        if (table) {
            sb.append("</table>");
        } else {
            sb.append("</ul>");
        }

        return new Result("", sb);
    }


    /**
     * Get the Geometry type corresponding the the EsriFeature
     * @param feature the feature
     * @param numParts _more_
     * @return the corresponding KML tag or null
     */
    public static String getGeometryType(EsriShapefile.EsriFeature feature,
                                         int numParts) {
        String tag = null;
        if (numParts == 0) {
            return tag;
        }
        if ((feature instanceof EsriShapefile.EsriPoint)
                || (feature instanceof EsriShapefile.EsriPointZ)) {
            if (numParts == 1) {
                tag = Geometry.TYPE_POINT;
            } else {
                tag = Geometry.TYPE_MULTIPOINT;
            }
        } else if ((feature instanceof EsriShapefile.EsriPolyline)
                   || (feature instanceof EsriShapefile.EsriPolylineZ)) {
            if (numParts == 1) {
                tag = Geometry.TYPE_LINESTRING;
            } else {
                tag = Geometry.TYPE_MULTILINESTRING;
            }
        } else if ((feature instanceof EsriShapefile.EsriPolygon)
                   || (feature instanceof EsriShapefile.EsriPolygonZ)) {
            if (numParts == 1) {
                tag = Geometry.TYPE_POLYGON;
            } else {
                tag = Geometry.TYPE_MULTIPOLYGON;
            }
        }

        return tag;
    }


    /**
     * Get the Geometry type corresponding the the EsriShapefileFeature
     *
     * @param featureType  the feature type
     * @return the corresponding KML tag or null
     */
    public static String getGeometryType(int featureType) {
        String tag = null;
        switch (featureType) {

          case EsriShapefile.POINT :      // 1
          case EsriShapefile.POINTZ :     // 11
              tag = Geometry.TYPE_POINT;

              break;

          case EsriShapefile.POLYLINE :   // 3
          case EsriShapefile.POLYLINEZ :  // 13
              tag = Geometry.TYPE_LINESTRING;

              break;

          case EsriShapefile.POLYGON :    // 5
          case EsriShapefile.POLYGONZ :   // 15
              tag = Geometry.TYPE_POLYGON;

              break;

          default :
              tag = null;

              break;
        }

        return tag;
    }
}
