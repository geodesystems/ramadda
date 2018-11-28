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
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.JsonOutputHandler;
import org.ramadda.repository.output.KmlOutputHandler;
import org.ramadda.repository.output.OutputHandler;
import org.ramadda.repository.output.OutputType;
import org.ramadda.repository.output.WikiConstants;
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

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.awt.geom.Rectangle2D;

import java.io.File;
import java.io.FileInputStream;

import java.text.DecimalFormat;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;


/**
 * Class to handle the output of shapefiles
 */
public class ShapefileOutputHandler extends OutputHandler implements WikiConstants {


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


    /** _more_ */
    public static final DecimalFormat decimalFormat =
        new DecimalFormat("#,##0.#");

    /** _more_ */
    public static final DecimalFormat intFormat = new DecimalFormat("#,###");


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
            return outputFields(request, entry, false, OUTPUT_FIELDS_LIST);
        } else if (outputType.equals(OUTPUT_FIELDS_TABLE)) {
            return outputFields(request, entry, true, OUTPUT_FIELDS_TABLE);
        }

        return null;
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
    public Properties getExtraProperties(Request request, Entry entry)
            throws Exception {
        List<Metadata> metadataList =
            getMetadataManager().findMetadata(request, entry,
                "shapefile_properties", true);
        if ((metadataList != null) && (metadataList.size() > 0)) {
            Properties properties = new Properties();
            for (Metadata metadata : metadataList) {
                MetadataType    type = getMetadataManager().getType(metadata);
                MetadataElement element = type.getChildren().get(0);
                File            f = type.getFile(entry, metadata, element);
                getRepository().loadProperties(properties, f.toString());
            }

            return properties;
        }

        return null;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param dbfile _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<DbaseDataWrapper> getDatum(Request request, Entry entry,
                                           DbaseFile dbfile)
            throws Exception {
        Properties             properties = getExtraProperties(request,
                                                entry);
        List<DbaseDataWrapper> fieldDatum = null;
        if (dbfile != null) {
            List<String> extraFields = null;
            String       extraKey    = (properties != null)
                                       ? (String) properties.get("map.key")
                                       : null;
            if (properties != null) {
                String fields = (String) properties.get("map.fields");
                if (fields != null) {
                    extraFields = StringUtil.split(fields, ",");
                }
            }
            DbaseDataWrapper keyWrapper = null;
            String[]         fieldNames = dbfile.getFieldNames();
            fieldDatum = new ArrayList<DbaseDataWrapper>();
            Hashtable<String, DbaseDataWrapper> wrapperMap =
                new Hashtable<String, DbaseDataWrapper>();
            for (int j = 0; j < fieldNames.length; j++) {
                DbaseDataWrapper dbd =
                    new DbaseDataWrapper(fieldNames[j].toLowerCase(),
                                         dbfile.getField(j), properties);
                wrapperMap.put(dbd.getName(), dbd);
                if (properties != null) {
                    if (Misc.equals((String) properties.get("map." + dbd.getName()
                            + ".drop"), "true")) {
                        continue;
                    }
                }
                fieldDatum.add(dbd);
            }
            if (extraFields != null) {
                if (extraKey != null) {
                    keyWrapper = wrapperMap.get(extraKey);
                }
                for (String extraField : extraFields) {
                    DbaseDataWrapper dbd = new DbaseDataWrapper(extraField,
                                               keyWrapper, properties);
                    String combine = (String) properties.get("map." + extraField
                                         + ".combine");
                    if (combine != null) {
                        List<DbaseDataWrapper> combineList = new ArrayList();
                        for (String id :
                                StringUtil.split(combine, ",", true, true)) {
                            DbaseDataWrapper other = wrapperMap.get(id);
                            if (other != null) {
                                combineList.add(other);
                            }
                        }
                        dbd.setCombine(combineList);
                    }
                    fieldDatum.add(dbd);
                    wrapperMap.put(dbd.getName(), dbd);
                }
            }
        }

        return fieldDatum;
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
        DbaseFile              dbfile    = shapefile.getDbFile();
        DbaseDataWrapper       nameField = null;
        String                 schemaName;
        String                 schemaId;
        List<DbaseDataWrapper> fieldDatum = null;
        HashMap                props      = new HashMap<String, Object>();
        props.put("dbfile", dbfile);
        props.put("shapefile", shapefile);
        Metadata       colorBy = null;
        List<Metadata> metadataList;
        metadataList = getMetadataManager().findMetadata(request, entry,
                "shapefile_color", true);
        if ((metadataList != null) && (metadataList.size() > 0)) {
            colorBy = metadataList.get(0);
            props.put("colorby", colorBy);
        }

        String balloonTemplate = null;

        metadataList = getMetadataManager().findMetadata(request, entry,
                "shapefile_display", true);
        if ((metadataList != null) && (metadataList.size() > 0)) {
            Metadata kmlDisplay = metadataList.get(0);
            schemaName      = schemaId = kmlDisplay.getAttr1();
            balloonTemplate = kmlDisplay.getAttr2();
            if ( !Utils.stringDefined(balloonTemplate)) {
                balloonTemplate = null;
            }
        } else {
            schemaId = schemaName = entry.getId();
        }


        if (dbfile != null) {
            fieldDatum = getDatum(request, entry, dbfile);
            int firstChar = -1;
            for (int i = 0; i < fieldDatum.size(); i++) {
                DbaseDataWrapper dbd = fieldDatum.get(i);
                if (dbd.getType() == DbaseData.TYPE_CHAR) {
                    if (firstChar < 0) {
                        firstChar = i;
                    }
                    if (dbd.getName().toLowerCase().indexOf("name") >= 0) {
                        if (nameField == null) {
                            nameField = dbd;
                        }
                    }
                }
            }
            if ((nameField == null) && (firstChar >= 0)) {
                nameField = fieldDatum.get(firstChar);
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
            for (DbaseDataWrapper dbd : fieldDatum) {
                String[] attrs = new String[4];
                String   dtype = null;
                switch (dbd.getType()) {

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
                attrs[3] = dbd.getName();
                schema.put(dbd.getName(), attrs);
            }
            props.put(FeatureCollection.PROP_SCHEMA, schema);
        }
        FeatureCollection fc = new FeatureCollection(entry.getName(),
                                   entry.getDescription(),
                                   (HashMap<String, Object>) props,
                                   fieldDatum);

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
                    name = nameField.getString(i).trim();
                }
            }
            HashMap<String, Object> fprops = new HashMap<String, Object>();
            if (dbfile != null) {
                fprops.put(FeatureCollection.PROP_SCHEMANAME, schemaName);
                fprops.put(FeatureCollection.PROP_SCHEMAID, schemaId);
                HashMap<String, Object> schemaData =
                    new HashMap<String, Object>(fieldDatum.size());
                for (int j = 0; j < fieldDatum.size(); j++) {
                    // since shapefile parser makes no distinction between ints & doubles,
                    // this hack will fix that.
                    Object data = fieldDatum.get(j).getData(i);
                    if (data instanceof Double) {
                        double d = ((Double) data).doubleValue();
                        if ((int) d == d) {
                            data = new Integer((int) d);
                        }
                    }
                    schemaData.put(fieldDatum.get(j).getName(), data);
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
        String fieldsArg = request.getString(ATTR_SELECTFIELDS,
                                             (String) null);
        String boundsArg = request.getString(ATTR_SELECTBOUNDS,
                                             (String) null);
        boolean forMap = request.get("formap", false);
        String returnFile =
            IOUtil.stripExtension(getStorageManager().getFileTail(entry))
            + ".kml";
        String filename = forMap + "_" + returnFile;
        if (boundsArg != null) {
            filename = boundsArg.replaceAll(",", "_") + filename;
        }
        if (fieldsArg != null) {
            filename =
                fieldsArg.replaceAll(",", "_").replaceAll(":",
                                     "_").replaceAll("<",
                                         "_lt_").replaceAll(">",
                                             "_gt_").replaceAll("=",
                                                 "_eq_").replaceAll("\\.",
                                                     "_dot_") + filename;
        }
        File file = getEntryManager().getCacheFile(entry, filename);


        if (file.exists()) {
            Result result = new Result(new FileInputStream(file),
                                       KmlOutputHandler.MIME_KML);
            result.setReturnFilename(returnFile);

            return result;
        }


        Rectangle2D.Double bounds = null;
        if (boundsArg != null) {
            List<String> toks = StringUtil.split(boundsArg, ",");
            if (toks.size() == 4) {
                double north = Double.parseDouble(toks.get(0));
                double west  = Double.parseDouble(toks.get(1));
                double south = Double.parseDouble(toks.get(2));
                double east  = Double.parseDouble(toks.get(3));
                bounds = new Rectangle2D.Double(west, south, east - west,
                        north - south);
            }
        }

        FeatureCollection fc          = makeFeatureCollection(request, entry);
        long              t1          = System.currentTimeMillis();
        List<String>      fieldValues = null;
        if (fieldsArg != null) {
            //selectFields=statefp:=:13,....
            fieldValues = new ArrayList<String>();
            List<String> toks = StringUtil.split(fieldsArg, ",", true, true);
            for (String tok : toks) {
                List<String> expr = StringUtil.splitUpTo(tok, ":", 3);
                if (expr.size() >= 2) {
                    fieldValues.add(expr.get(0));
                    if (expr.size() == 2) {
                        fieldValues.add("=");
                        fieldValues.add(expr.get(1));
                    } else {
                        fieldValues.add(expr.get(1));
                        fieldValues.add(expr.get(2));
                    }
                }
            }
        }

        Element      root = fc.toKml(forMap, bounds, fieldValues);
        long         t2   = System.currentTimeMillis();
        StringBuffer sb   = new StringBuffer(XmlUtil.XML_HEADER);
        String       xml  = XmlUtil.toString(root, false);
        sb.append(xml);
        IOUtil.writeFile(file, xml);
        long t3 = System.currentTimeMillis();
        //        Utils.printTimes("OutputKml time:", t1,t2,t3);
        Result result = new Result("", sb, KmlOutputHandler.MIME_KML);
        result.setReturnFilename(returnFile);

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
     * _more_
     *
     * @param v _more_
     *
     * @return _more_
     */
    public static String format(int v) {
        return intFormat.format(v);
    }


    /**
     * _more_
     *
     * @param v _more_
     *
     * @return _more_
     */
    public static String format(double v) {
        if (v == (int) v) {
            return intFormat.format(v);
        }

        return decimalFormat.format(v);
    }

    /**
     * Output the shapefile entry as GeoJSON
     *
     * @param request  the request
     * @param entry  the entry
     * @param table _more_
     * @param output _more_
     *
     * @return the GeoJSON
     *
     * @throws Exception _more_
     */
    private Result outputFields(Request request, Entry entry, boolean table,
                                OutputType output)
            throws Exception {

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
        Hashtable              props = getRepository().getPluginProperties();

        List<DbaseDataWrapper> fieldDatum = getDatum(request, entry, dbfile);

        String searchFieldId = request.getString("searchfield",
                                   (String) null);
        String searchText = request.getString("searchtext", (String) null);
        String searchtext = null;
        //        System.err.println("search:" + searchFieldId +"=" + searchText);
        String baseUrl = request.entryUrl(getRepository().URL_ENTRY_SHOW,
                                          entry) + "&output=" + output;
        String           stepUrl     = baseUrl;
        DbaseDataWrapper searchField = null;
        if ((searchFieldId != null) && (searchText != null)) {
            stepUrl += "&" + HtmlUtils.arg("searchfield", searchFieldId)
                       + "&" + HtmlUtils.arg("searchtext", searchText);
            searchtext = searchText.toLowerCase();
            for (DbaseDataWrapper dbd : fieldDatum) {
                if (dbd.getName().equals(searchFieldId)) {
                    searchField = dbd;

                    break;
                }
            }
        }
        if (searchField != null) {
            sb.append("Searching for " + searchField.getLabel()
                      + " contains " + searchText);
            sb.append("<br>");
        }

        int  start    = request.get("start", 0);
        int  i        = start;
        List features = shapefile.getFeatures();
        int  max      = request.get("max", 100);
        HtmlUtils.open(sb, "div", HtmlUtils.cssClass("black_href"));
        sb.append(features.size() + " Features&nbsp;&nbsp;");
        if (start > 0) {
            int prevStart = Math.max(0, start - max);
            HtmlUtils.href(
                sb, stepUrl + "&"
                + HtmlUtils.args(
                    "start", "" + prevStart, "max", ""
                    + max), "Previous", HtmlUtils.style("color:#000000;"));
            sb.append("&nbsp;&nbsp;");
        }
        if (i + max < features.size()) {
            int nextStart = start + max;
            HtmlUtils.href(
                sb, stepUrl + "&"
                + HtmlUtils.args(
                    "start", "" + (nextStart), "max", ""
                    + max), "Next", HtmlUtils.style("color:#000000;"));
        }


        String searchUrl = baseUrl + "&"
                           + HtmlUtils.args("start", "" + start, "max",
                                            "" + max);
        sb.append(
            HtmlUtils.script(
                "\nfunction shapefileSearch(field) {\nvar shapefileSearchUrl='"
                + searchUrl
                + "';\nvar text=prompt('Search for');\nif(text) window.location.href=shapefileSearchUrl+'&' + HtmlUtil.urlArg('searchfield',field)+'&' +HtmlUtil.urlArg('searchtext',text);}\n"));
        //        System.err.println(sb);

        if (table) {
            sb.append("<table border=1>");
            sb.append("<tr>");
            sb.append(HtmlUtils.td(HtmlUtils.b("Field&nbsp;#"),
                                   HtmlUtils.style("padding:5px;")));
            for (DbaseDataWrapper dbd : fieldDatum) {
                String label = HtmlUtils.mouseClickHref("shapefileSearch('"
                                   + dbd.getName() + "')", dbd.getLabel());
                sb.append(HtmlUtils
                    .td(HtmlUtils
                        .div(label, HtmlUtils
                            .attr("title", "id:" + dbd.getName()) + HtmlUtils
                            .style("font-weight:bold;padding:5px;text-align:center;")), HtmlUtils
                                .attr("align", "center")));
            }
            sb.append("</tr>");
        } else {
            sb.append("<ul>\n");
        }

        int    cnt       = 0;
        String searchKey = (searchField != null)
                           ? searchField.getLowerCaseName()
                           : null;
        for (; i < features.size(); i++) {
            if (searchField != null) {
                String value = searchField.getString(i).toLowerCase();
                String fromProps = (String) props.get("map." + searchKey
                                       + "." + value);
                if (fromProps != null) {
                    value = fromProps + " (" + value + ")";
                    value = value.toLowerCase();
                }
                if ((value.indexOf(searchText) >= 0)
                        || (value.indexOf(searchtext) >= 0)) {
                    //OK
                } else {
                    continue;
                }
            }

            cnt++;
            if (cnt > max) {
                break;
            }

            if (table) {
                sb.append("<tr>");
                sb.append(HtmlUtils.td("" + (i + 1),
                                       HtmlUtils.style("padding:5px;")));
            } else {
                sb.append("<li> ");
                sb.append("Field&nbsp;#" + (i + 1));
                sb.append("<ul>\n");
            }
            //            sb.append(HtmlUtils.script("function fieldSelect(

            String displayUrl =
                request.entryUrl(getRepository().URL_ENTRY_SHOW, entry);

            for (DbaseDataWrapper dbd : fieldDatum) {
                String value;
                String extra = "";
                String url   = null;
                String key   = dbd.getLowerCaseName();
                if (dbd.getType() == DbaseData.TYPE_NUMERIC) {
                    value = format(dbd.getDouble(i));
                    extra = " align=right ";
                } else {
                    value = "" + dbd.getData(i);
                    url = displayUrl + "&"
                          + HtmlUtils.arg(ATTR_SELECTFIELDS,
                                          key + ":=:" + value);
                }

                String fromProps = (String) props.get("map." + key + "."
                                       + value);
                if (fromProps != null) {
                    value = fromProps + " (" + value + ")";
                }

                String rawValue = value;
                if (url != null) {
                    value = HtmlUtils.href(url, value,
                                           HtmlUtils.attr("title",
                                               "Show in map"));
                }
                if (table) {
                    sb.append(HtmlUtils.td(HtmlUtils.div(value,
                            HtmlUtils.style("padding:5px;")), extra));
                } else {
                    sb.append("<li>");
                    String label = HtmlUtils.href(searchUrl + "&"
                                       + HtmlUtils.args("searchfield",
                                           dbd.getName(), "searchtext",
                                           rawValue), dbd.getLabel(),
                                               HtmlUtils.attr("title",
                                                   "Search for value"));
                    sb.append(
                        HtmlUtils.span(
                            label + ": ",
                            HtmlUtils.attr("title", "id:" + dbd.getName())
                            + HtmlUtils.style("font-weight:bold;")));
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
        HtmlUtils.close(sb, "div");

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


    public static String makeMapStyle(Request request, Entry entry) throws Exception {
        List<Metadata> metadataList = request.getRepository().getMetadataManager().findMetadata(request, entry, "map_style", true);
        List<String> values = new ArrayList<String>();
        if ((metadataList != null) && (metadataList.size() > 0)) {
            Metadata kmlDisplay = metadataList.get(0);
            if (Utils.stringDefined(kmlDisplay.getAttr1())) {
                values.add("strokeColor");
                values.add(kmlDisplay.getAttr1());
            }
            if (Utils.stringDefined(kmlDisplay.getAttr2())) {
                values.add("fillColor");
                values.add(kmlDisplay.getAttr2());
            }
            if (Utils.stringDefined(kmlDisplay.getAttr3())) {
                values.add("select_strokeColor");
                values.add(kmlDisplay.getAttr3());
            }
            if (Utils.stringDefined(kmlDisplay.getAttr4())) {
                values.add("select_fillColor");
                values.add(kmlDisplay.getAttr4());
            }
            if (Utils.stringDefined(kmlDisplay.getAttr(5))) {
                values.add("strokeWidth");
                values.add(kmlDisplay.getAttr(5));
            }
        }
        return Json.mapAndQuote(values);
    }



}
