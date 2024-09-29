/**
Copyright (c) 2008-2024 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
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
import org.ramadda.util.ColorTable;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.TTLCache;
import org.ramadda.util.IO;
import org.ramadda.util.Utils;
import org.ramadda.util.geo.*;
import org.ramadda.util.geo.KmlUtil;
import org.ramadda.util.seesv.Seesv;

import org.w3c.dom.CDATASection;
import org.w3c.dom.Element;

import ucar.unidata.gis.GisPart;
import ucar.unidata.gis.shapefile.DbaseData;
import ucar.unidata.gis.shapefile.DbaseFile;
import ucar.unidata.gis.shapefile.EsriShapefile;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.awt.Color;
import java.awt.geom.Rectangle2D;


import java.io.*;

import java.text.DecimalFormat;


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import java.util.Map;
import java.util.Properties;

import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;


/**
 * Class to handle the output of shapefiles
 */
@SuppressWarnings("unchecked")
public class ShapefileOutputHandler extends OutputHandler implements WikiConstants {

    public static boolean debug = false;

    public static final OutputType OUTPUT_KML =
        new OutputType("Convert Shapefile to KML", "shapefile.kml",
                       OutputType.TYPE_FILE, "", ICON_KML);

    public static final OutputType OUTPUT_GEOJSON =
        new OutputType("Convert Shapefile to GeoJSON", "geojson.geojson",
                       OutputType.TYPE_FILE, "", ICON_GEOJSON);

    public static final OutputType OUTPUT_CSV =
        new OutputType("Convert Shapefile to CSV", "shapefile.csv",
                       OutputType.TYPE_FILE, "", ICON_CSV);
    public static final OutputType OUTPUT_FIELDS_LIST =
        new OutputType("Map Fields", "shapefile.fields_list",
                       OutputType.TYPE_VIEW, "", ICON_TABLE);
    public static final OutputType OUTPUT_FIELDS_TABLE =
        new OutputType("Map Table", "shapefile.fields_table",
                       OutputType.TYPE_VIEW, "", ICON_TABLE);

    public static final DecimalFormat decimalFormat =
        new DecimalFormat("#,##0.#");

    
    public static final DecimalFormat intFormat = new DecimalFormat("#,###");

    
    public static final DecimalFormat plainFormat =
        new DecimalFormat("####.#");


    
    private TTLCache<String, ShapefileWrapper> cache;



    
    public ShapefileOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        addType(OUTPUT_KML);
        addType(OUTPUT_GEOJSON);
        addType(OUTPUT_CSV);
        addType(OUTPUT_FIELDS_TABLE);
        addType(OUTPUT_FIELDS_LIST);
        //Create the cache with a 1 minute TTL
        cache = new TTLCache<String, ShapefileWrapper>(60 * 1000,
                             "Shapefile Cache") {
            @Override
            public void cacheRemove(ShapefileWrapper wrapper) {
                IOUtil.close(wrapper.inputStream);
            }
        };
    }


    
    public EsriShapefile getShapefile(Entry entry) throws Exception {
        synchronized (cache) {
            ShapefileWrapper wrapper   = cache.get(entry.getId());
            EsriShapefile    shapefile = (wrapper != null)
                                         ? wrapper.shapefile
                                         : null;
            if ((shapefile != null) && debug) {
                System.err.println(
                    "ShapefileOutputHandler.getShapefile: in cache");
                System.err.println("stack:" + Utils.getStack(5));
            }

            if (shapefile == null) {
                if (debug) {
                    System.err.println(
                        "ShapefileOutputHandler.getShapefile: new EsriShapefile");
                    System.err.println("stack:" + Utils.getStack(5));
                }
                String      path        = entry.getResourcePath(getRepository().getTmpRequest());
                InputStream inputStream = IO.getInputStream(path);
                shapefile = new EsriShapefile(inputStream, null, 0.0f);
                cache.put(entry.getId(),
                          new ShapefileWrapper(shapefile, inputStream));
            }

            return shapefile;
        }
    }

    
    private static class ShapefileWrapper {

        
        EsriShapefile shapefile;

        
        InputStream inputStream;

        
        public ShapefileWrapper(EsriShapefile shapefile,
                                InputStream inputStream) {
            this.shapefile   = shapefile;
            this.inputStream = inputStream;
        }
    }



    
    public void getEntryLinks(Request request, State state, List<Link> links)
            throws Exception {
        if ((state.entry != null)
                && state.entry.getTypeHandler().isType("geo_shapefile")) {
            links.add(makeLink(request, state.entry, OUTPUT_FIELDS_TABLE));
            links.add(makeLink(request, state.entry, OUTPUT_FIELDS_LIST));
            links.add(makeLink(request, state.entry, OUTPUT_KML));
            links.add(makeLink(request, state.entry, OUTPUT_GEOJSON));
            links.add(makeLink(request, state.entry, OUTPUT_CSV));
        }
    }


    
    public Result outputEntry(Request request, OutputType outputType,
                              Entry entry)
            throws Exception {
        if (outputType.equals(OUTPUT_KML)) {
            return outputKml(request, entry);
        } else if (outputType.equals(OUTPUT_GEOJSON)) {
            return outputGeoJson(request, entry);
        } else if (outputType.equals(OUTPUT_CSV)) {
            return outputCsv(request, entry);
        } else if (outputType.equals(OUTPUT_FIELDS_LIST)) {
            return outputFields(request, entry, false, OUTPUT_FIELDS_LIST);
        } else if (outputType.equals(OUTPUT_FIELDS_TABLE)) {
            return outputFields(request, entry, true, OUTPUT_FIELDS_TABLE);
        }

        return null;
    }

    
    public Hashtable getExtraProperties(Request request, Entry entry)
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

            Hashtable props = new Hashtable();
            props.putAll(properties);

            return props;
        }

        return null;
    }

    
    public List<DbaseDataWrapper> getDatum(Request request, Entry entry,
                                           DbaseFile dbfile)
            throws Exception {
        Hashtable  properties       = getExtraProperties(request, entry);
        Properties pluginProperties = getRepository().getPluginProperties();

        return FeatureCollection.getDatum(dbfile, properties,
                                          pluginProperties);
    }


    
    private FeatureCollection makeFeatureCollection(Request request,
            Entry entry, EsriShapefile shapefile)
            throws Exception {

        if (shapefile == null) {
            shapefile = getShapefile(entry);
        }
        DbaseFile              dbfile = shapefile.getDbFile();
        String                 schemaName;
        String                 schemaId;
        List<DbaseDataWrapper> fieldDatum = null;
        DbaseDataWrapper       nameField  = null;

        Hashtable collectionProps         = new Hashtable<String, Object>();
        Metadata               colorBy    = null;
        List<Metadata>         metadataList;
        metadataList = getMetadataManager().findMetadata(request, entry,
                "shapefile_color", true);
        if ((metadataList != null) && (metadataList.size() > 0)) {
            colorBy = metadataList.get(0);
            collectionProps.put("colorby", colorBy);
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
            collectionProps.putAll(getRepository().getPluginProperties());
            Hashtable extraProperties = getExtraProperties(request, entry);
            fieldDatum = FeatureCollection.getDatum(dbfile, extraProperties,
                    (Hashtable<String, Object>) collectionProps);
            nameField = FeatureCollection.getNameField(fieldDatum);
            collectionProps.put(FeatureCollection.PROP_SCHEMANAME,
                                schemaName);
            collectionProps.put(FeatureCollection.PROP_SCHEMAID, schemaId);
            collectionProps.put(FeatureCollection.PROP_STYLEID, schemaId);
            if (balloonTemplate != null) {
                collectionProps.put(FeatureCollection.PROP_BALLOON_TEMPLATE,
                                    balloonTemplate);
            }
        }

        Hashtable properties = getExtraProperties(request, entry);

        return FeatureCollection.makeFeatureCollection(entry.getName(),
                entry.getDescription(), shapefile, properties,
                collectionProps);
    }


    
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

    
    private static class MapRequest {
	String filename;
	String returnFile;
	File cacheFile;
        Rectangle2D.Double bounds;
	MapRequest(String filename, String returnFile,File cacheFile,       Rectangle2D.Double bounds) {
	    this.filename = filename;
	    this.returnFile = returnFile;
	    this.cacheFile = cacheFile;
	    this.bounds = bounds;
	}
    }


    private MapRequest makeMapRequest(Request request, Entry entry, String suffix) {
        String fieldsArg = request.getString(ATTR_SELECTFIELDS,
                                             (String) null);
        String boundsArg = request.getString(ATTR_SELECTBOUNDS,
                                             (String) null);
        boolean forMap = request.get("formap", false);
        String returnFile =entry.getChangeDate() +"_"+
            IOUtil.stripExtension(getStorageManager().getFileTail(entry))
            + ".kml";
        String filename = forMap + "_" + returnFile;
	filename+="_"+entry.getChangeDate();
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

	return new MapRequest(filename, returnFile,file,bounds);
    }

    private Result outputKml(Request request, Entry entry) throws Exception {
        String fieldsArg = request.getString(ATTR_SELECTFIELDS, (String) null);
        boolean forMap = request.get("formap", false);
	MapRequest mapRequest=makeMapRequest(request, entry,".kml");
        if (mapRequest.cacheFile.exists()) {
            Result result = new Result(new FileInputStream(mapRequest.cacheFile),
                                       KmlOutputHandler.MIME_KML);
            result.setReturnFilename(mapRequest.returnFile);
            return result;
        }

        FeatureCollection fc = makeFeatureCollection(request, entry, null);
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

        Element      root = toKml(fc, forMap, mapRequest.bounds, fieldValues);
        long         t2   = System.currentTimeMillis();
        StringBuffer sb   = new StringBuffer(XmlUtil.XML_HEADER);
	FileOutputStream fos = new FileOutputStream(mapRequest.cacheFile);
	OutputStreamWriter osw = new OutputStreamWriter(fos);
        XmlUtil.toString(root, osw);
	osw.flush();
	osw.close();
	fos.close();
        long t3 = System.currentTimeMillis();
        Result result = new Result(new FileInputStream(mapRequest.cacheFile), KmlOutputHandler.MIME_KML);
        result.setReturnFilename(mapRequest.returnFile);
        return result;
    }

    
    private Result outputGeoJson(Request request, Entry entry)
            throws Exception {
	MapRequest mapRequest=makeMapRequest(request, entry,".geojson");
        if (mapRequest.cacheFile.exists()) {
            Result result = new Result(new FileInputStream(mapRequest.cacheFile),
                                       GeoJson.GEOJSON_MIMETYPE);
            result.setReturnFilename(mapRequest.returnFile);
            return result;
        }

	FileOutputStream fos = new FileOutputStream(mapRequest.cacheFile);
	OutputStreamWriter osw = new OutputStreamWriter(fos);

        FeatureCollection fc = makeFeatureCollection(request, entry, null);
        fc.toGeoJson(osw);
	osw.flush();
	osw.close();
	fos.close();

        Result result = new Result(new FileInputStream(mapRequest.cacheFile), GeoJson.GEOJSON_MIMETYPE);
        result.setReturnFilename(mapRequest.returnFile);
        return result;
    }

    
    public static String format(int v) {
        return intFormat.format(v);
    }


    
    public static String format(double v) {
        if (v == (int) v) {
            return intFormat.format(v);
        }

        return decimalFormat.format(v);
    }

    
    private Result outputCsv(Request request, Entry entry) throws Exception {
        return getCsvResult(request, entry);
    }

    
    public Result getCsvResult(Request request, Entry entry)
            throws Exception {
        String filename =
            IOUtil.stripExtension(getStorageManager().getFileTail(entry))
            + ".csv";
        Result result = request.getOutputStreamResult(filename,
						      Result.TYPE_CSV);
        getCsvBuffer(request, request.getOutputStream(), entry, null, false,
                     false, false);

        return result;
    }


    
    public void getCsvBuffer(Request request, OutputStream os, Entry entry,
                             EsriShapefile shapefile, boolean addHeader,
                             boolean addPoints, boolean addFeatures)
            throws Exception {

        PrintWriter sb = new PrintWriter(os);
        FeatureCollection fc = makeFeatureCollection(request, entry,
                                   shapefile);
        List<DbaseDataWrapper> datum    = fc.getDatum();
        List<Feature>          features = (List<Feature>) fc.getFeatures();
        if (datum == null) {
            if (addHeader) {
                sb.println("#fields=");
            }
            sb.println("#No fields");

            return;
        }
        if (addHeader) {
            sb.print("#fields=");
        }
        int colCnt = 0;
        for (DbaseDataWrapper dbd : datum) {
            String type = "string";
            if (dbd.getType() == DbaseData.TYPE_NUMERIC) {
                type = "double";
            }
            if (colCnt++ > 0) {
                sb.print(",");
            }
            sb.print(dbd.getName());
            if (addHeader) {
                sb.print("[type=" + type + "]");
            }
        }
        if (addPoints) {
            sb.print(",latitude");
            if (addHeader) {
                sb.print("[type=double]");
            }
            sb.print(",longitude");
            if (addHeader) {
                sb.print("[type=double]");
            }
        }

        if (addFeatures) {
            sb.print(",shapeType");
            if (addHeader) {
                sb.print("[type=string]");
            }
            sb.print(",shape");
            if (addHeader) {
                sb.print("[type=string]");
            }
        }




        sb.print("\n");
        int     cnt   = 0;
        boolean debug = false;
        for (int i = 0; i < features.size(); i++) {
            Feature feature = features.get(i);
            cnt++;
            colCnt = 0;
            String line = "";
            for (DbaseDataWrapper dbd : datum) {
                String value;
                if (dbd.getType() == DbaseData.TYPE_NUMERIC) {
                    value = plainFormat.format(dbd.getDouble(i));
                } else {
                    value = "" + dbd.getData(i);
                    value = value.trim();
                }
                if (colCnt++ > 0) {
                    sb.print(",");
                }
                line += "," + value;
                sb.print(Seesv.cleanColumnValue(value));
            }
            if (debug) {
                System.out.println("line:" + line);
            }
            if (debug) {
                System.out.println(
                    "pts:" + feature.getId() + " coords:"
                    + feature.getGeometry().getCoordsString().replaceAll(
                        "\n", " "));
            }
            if (addPoints) {
                float[] center = feature.getGeometry().getCenter(debug);
                sb.print(",");
                sb.print(center[0]);
                sb.print(",");
                sb.print(center[1]);
                line += "," + center[0] + "," + center[1];
            }


            if (addFeatures) {
                sb.print(",");
                sb.print(feature.getGeometry().getGeometryType());
                sb.print(",");
                String shape = feature.getGeometry().getCoordsString();
                shape = shape.replaceAll("\n", "").replaceAll(" ", "");
                sb.print("\"");
                sb.print(shape);
                sb.print("\"");
            }
            sb.print("\n");
        }
        sb.flush();

    }







    
    private Result outputFields(Request request, Entry entry, boolean table,
                                OutputType output)
            throws Exception {

        EsriShapefile shapefile = getShapefile(entry);
        DbaseFile     dbfile    = shapefile.getDbFile();
        StringBuilder sb        = new StringBuilder();
        getPageHandler().entrySectionOpen(request, entry, sb, table
                ? "Shapefile Table"
                : "Shapefile Fields");
        if (dbfile == null) {
            sb.append("No fields");
            getPageHandler().entrySectionClose(request, entry, sb);

            return new Result("", sb);
        }
        HtmlUtils.open(sb, "div", HtmlUtils.cssClass("ramadda-links"));
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
            sb.append(
                "<table class='stripe hover ramadda-table' table-height=400 >");
            sb.append("<thead>");
            sb.append("<tr valign=top>");
            sb.append(HtmlUtils.th(HtmlUtils.b("Field&nbsp;#"),
                                   HtmlUtils.style("padding:5px;")));
            for (DbaseDataWrapper dbd : fieldDatum) {
                String label = HtmlUtils.mouseClickHref("shapefileSearch('"
                                   + dbd.getName() + "')", dbd.getLabel());
                sb.append(HtmlUtils
                    .th(HtmlUtils
                        .div(label, HtmlUtils
                            .attr("title", "id:" + dbd.getName()) + HtmlUtils
                            .style("font-weight:bold;padding:5px;text-align:center;")), HtmlUtils
                                .attr("align", "center")));
            }
            sb.append("</tr>");
            sb.append("</thead>");
            sb.append("</thead>");
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
                          + HtmlUtils.arg("mapsubset", "true") + "&"
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
            sb.append("</tbody>");
            sb.append("</table>");
        } else {
            sb.append("</ul>");
        }
        HtmlUtils.close(sb, "div");
        HtmlUtils.close(sb, "div");

        return new Result("", sb);

    }


    
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


    
    public static void makeMapStyle(Request request, Entry entry,List<String> values)
            throws Exception {
        List<Metadata> metadataList =
            request.getRepository().getMetadataManager().findMetadata(
                request, entry, "map_style", true);
        if ((metadataList != null) && (metadataList.size() > 0)) {
            Metadata kmlDisplay = metadataList.get(0);
            if (Utils.stringDefined(kmlDisplay.getAttr1())) {
                values.add("strokeColor");
                values.add(JU.quote(kmlDisplay.getAttr1()));
            }
            if (Utils.stringDefined(kmlDisplay.getAttr2())) {
                values.add("fillColor");
                values.add(JU.quote(kmlDisplay.getAttr2()));
            }
            if (Utils.stringDefined(kmlDisplay.getAttr3())) {
                values.add("select_strokeColor");
                values.add(JU.quote(kmlDisplay.getAttr3()));
            }
            if (Utils.stringDefined(kmlDisplay.getAttr4())) {
                values.add("select_fillColor");
                values.add(JU.quote(kmlDisplay.getAttr4()));
            }
            if (Utils.stringDefined(kmlDisplay.getAttr(5))) {
                values.add("strokeWidth");
                values.add(kmlDisplay.getAttr(5));
            }
            if (Utils.stringDefined(kmlDisplay.getAttr(6))) {
                values.add("pointRadius");
                values.add(kmlDisplay.getAttr(6));
            }
        }
    }




    
    public Element toKml(FeatureCollection fc, boolean decimate,
                         Rectangle2D.Double bounds, List<String> fieldValues)
            throws Exception {

        Hashtable              properties = fc.getProperties();
        List<Feature>          features   = fc.getFeatures();
        List<DbaseDataWrapper> fieldDatum = fc.getDatum();



        Element                root       = KmlUtil.kml(getName());
        Element                doc = KmlUtil.document(root, getName(), true);
        boolean                haveSchema = false;
        if ( !properties.isEmpty()
                && (properties.get(FeatureCollection.PROP_SCHEMANAME)
                    != null) && (properties
                        .get(FeatureCollection
                            .PROP_SCHEMAID) != null) && (properties
                                .get(FeatureCollection
                                    .PROP_SCHEMA) != null)) {
            makeKmlSchema(properties, doc);
            haveSchema = true;
        }
        Element folder = KmlUtil.folder(doc, getName(), true);
        // Apply one style to the entire document
        String styleName =
            (String) properties.get(FeatureCollection.PROP_STYLEID);
        if (styleName == null) {
            styleName = "" + (int) (Math.random() * 1000);
        }



        Metadata      colorBy   = (Metadata) properties.get("colorby");
        DbaseFile     dbfile    = (DbaseFile) properties.get("dbfile");
        EsriShapefile shapefile = (EsriShapefile) properties.get("shapefile");
        //Correspond to the index
        List<Color>  colors    = null;
        List<String> styleUrls = null;
        int[]        styleCnt  = { 0 };
        String balloonTemplate =
            (String) properties.get(FeatureCollection.PROP_BALLOON_TEMPLATE);

        if ((colorBy != null) && (dbfile != null)) {
            Hashtable<Color, String> colorMap = new Hashtable<Color,
                                                    String>();

            String           colorByFieldAttr = colorBy.getAttr1().trim();
            DbaseDataWrapper colorByField     = null;
            for (int j = 0; j < fieldDatum.size(); j++) {
                if (fieldDatum.get(j).getName().equalsIgnoreCase(
                        colorByFieldAttr)) {
                    colorByField = fieldDatum.get(j);

                    break;
                }
            }
            double     min = Double.NaN;
            double     max = Double.NaN;
            ColorTable ct  = null;
            if (Utils.stringDefined(colorBy.getAttr2())) {
                ct = ColorTable.getColorTable(colorBy.getAttr2().trim());
            }

            String lineColorAttr = colorBy.getAttr3().trim();
            Color  lineColor     = ((lineColorAttr.length() == 0)
                                    ? null
                                    : lineColorAttr.equals("none")
                                      ? null
                                      : Utils.decodeColor(lineColorAttr,
                                          null));

            if (Utils.stringDefined(colorBy.getAttr(4))) {
                min = Double.parseDouble(colorBy.getAttr(4));
            }
            if (Utils.stringDefined(colorBy.getAttr(5))) {
                max = Double.parseDouble(colorBy.getAttr(5));
            }

            //            List features = shapefile.getFeatures();
            styleUrls = new ArrayList<String>();


            Hashtable<String, Color> valueMap = new Hashtable<String,
                                                    Color>();
            if ((ct != null) && (colorByField != null)) {
                if (colorByField.isNumeric()) {
                    boolean needMin = Double.isNaN(min);
                    boolean needMax = Double.isNaN(max);
                    if (needMin || needMax) {
                        if (needMin) {
                            min = Double.MAX_VALUE;
                        }
                        if (needMax) {
                            max = Double.MIN_VALUE;
                        }

                        for (int i = 0; i < features.size(); i++) {
                            double value = colorByField.getDouble(i);
                            if (needMin) {
                                min = Math.min(min, value);
                            }
                            if (needMax) {
                                max = Math.max(max, value);
                            }
                        }
                    }
                }
            } else {
                String enums = colorBy.getAttr(6);
                for (String line :
                        StringUtil.split(enums, "\n", true, true)) {
                    List<String> toks = StringUtil.splitUpTo(line, ":", 2);
                    if (toks.size() > 1) {
                        Color c = Utils.decodeColor(toks.get(1).trim(),
                                      (Color) null);
                        if (c != null) {
                            valueMap.put(toks.get(0), c);
                        } else {
                            System.err.println("Bad color:" + toks.get(1));
                        }
                    }
                }
            }
            if (colorByField != null) {
                int cnt = 0;
                Hashtable<String, Integer> indexMap = new Hashtable<String,
                                                          Integer>();
                for (int i = 0; i < features.size(); i++) {
                    Color color = null;
                    if (ct != null) {
                        if (colorByField.isNumeric()) {
                            double value = colorByField.getDouble(i);
                            color = ct.getColor(min, max, value);
                        } else {
                            String  value = "" + colorByField.getData(i);
                            Integer index = indexMap.get(value);
                            if (index == null) {
                                index = Integer.valueOf(cnt++);
                                indexMap.put(value, index);
                            }
                            color = ct.getColorByIndex(index);
                        }
                    } else {
                        String value = "" + colorByField.getData(i);
                        color = valueMap.get(value);
                    }
                    if (color != null) {
                        String styleUrl = makeFillStyle(fc, color, colorMap,
                                              lineColor,
                                              !lineColorAttr.equals("none"),
                                              folder, styleCnt, styleName,
                                              balloonTemplate);
                        styleUrls.add(styleUrl);
                    } else {
                        styleUrls.add(styleName);
                    }
                }
            }
        }

        KmlUtil.open(folder, false);
        if (fc.getDescription().length() > 0) {
            KmlUtil.description(folder, fc.getDescription());
        }

        // could try to set these dynamically
        Color lineColor =
            Utils.decodeColor((String) properties.get("lineColor"),
                              Color.gray);
        int    lineWidth     = 1;
        String polyColorMode = "random";
        Color polyColor =
            Utils.decodeColor((String) properties.get("fillColor"),
                              Color.lightGray);

        Element style = KmlUtil.style(folder, styleName);
        if (haveSchema) {
            makeBalloonForDB(fc, style, balloonTemplate);
        }
        String featureType = features.get(0).getGeometry().getGeometryType();

        if (featureType.equals(Geometry.TYPE_POLYGON)
                || featureType.equals(Geometry.TYPE_MULTIPOLYGON)
                || featureType.equals(Geometry.TYPE_LINESTRING)
                || featureType.equals(Geometry.TYPE_MULTILINESTRING)) {
            Element linestyle = KmlUtil.makeElement(style,
                                    KmlUtil.TAG_LINESTYLE);
            if (lineColor != null) {
                KmlUtil.makeText(
                    linestyle, KmlUtil.TAG_COLOR,
                    "ff" + KmlUtil.toBGRHexString(lineColor).substring(1));
                KmlUtil.makeText(linestyle, KmlUtil.TAG_COLORMODE, "normal");
            }
            if (lineWidth > 0) {
                KmlUtil.makeText(linestyle, KmlUtil.TAG_WIDTH,
                                 "" + lineWidth);
            }
            if (featureType.equals(Geometry.TYPE_POLYGON)
                    || featureType.equals(Geometry.TYPE_MULTIPOLYGON)) {
                Element polystyle = KmlUtil.makeElement(style,
                                        KmlUtil.TAG_POLYSTYLE);
                if (polyColor != null) {
                    KmlUtil.makeText(
                        polystyle, KmlUtil.TAG_COLOR,
                        "66"
                        + KmlUtil.toBGRHexString(polyColor).substring(1));
                    KmlUtil.makeText(polystyle, KmlUtil.TAG_COLORMODE,
                                     "normal");
                } else {
                    KmlUtil.makeText(polystyle, KmlUtil.TAG_COLORMODE,
                                     polyColorMode);
                }
                if (polyColorMode.equals("normal")) {
                    //                    KmlUtil.makeText(polystyle, KmlUtil.TAG_COLOR,
                    //"66" + KmlUtil.toBGRHexString(polyColor).substring(1));
                    //                    "66E6E6E6");
                }
            }
        }
        int points = 0;
        if (decimate) {
            float epsilon = 0.005f;
            while (true) {
                points = 0;
                if (epsilon > 360.0f) {
                    break;
                }
                for (Feature feature : features) {
                    points += feature.getNumPoints();
                }
                if (points < ShapefileTypeHandler.MAX_POINTS) {
                    //                    System.err.println("points:" + points);
                    break;
                }
                //                System.err.println("points:" + points +" epsilon:" + epsilon);
                for (Feature feature2 : features) {
                    feature2.applyEpsilon(epsilon);
                }
                epsilon = epsilon * 2.0f;
            }
        }
        int cnt = 0;
        for (Feature feature : features) {
            String  styleUrl = (styleUrls == null)
                               ? styleName
                               : styleUrls.get(cnt);
            boolean ok       = true;
            if ((fieldValues != null) && (fieldValues.size() > 0)
                    && (dbfile != null)) {
                for (int i = 0; i < fieldValues.size(); i += 3) {
                    String fieldName  = fieldValues.get(i);
                    String operator   = fieldValues.get(i + 1);
                    String fieldValue = fieldValues.get(i + 2);
                    for (DbaseDataWrapper wrapper : fieldDatum) {
                        if (wrapper.getName().trim().equalsIgnoreCase(
                                fieldName)) {
                            Object obj = wrapper.getData(cnt);
                            if ((obj instanceof Double)
                                    || (obj instanceof Integer)) {
                                double v = Double.parseDouble(fieldValue);
                                double opValue;
                                if (obj instanceof Double) {
                                    opValue = ((Double) obj).doubleValue();
                                } else {
                                    opValue = ((Integer) obj).intValue();
                                }
                                if (operator.equals("<")) {
                                    ok = opValue < v;
                                } else if (operator.equals("<=")) {
                                    ok = opValue <= v;
                                } else if (operator.equals(">")) {
                                    ok = opValue > v;
                                } else if (operator.equals(">=")) {
                                    ok = opValue >= v;
                                } else if (operator.equals("=")) {
                                    ok = opValue == v;
                                }
                            } else {
                                String value = obj.toString().trim();
                                ok = value.equals(fieldValue);
                            }
                            if ( !ok) {
                                break;
                            }
                        }
                    }
                    if ( !ok) {
                        break;
                    }
                }
            }
            if (ok) {
                feature.makeKmlElement(folder, "#" + styleUrl, bounds);
            }
            cnt++;
        }

        return root;

    }


    
    private String makeFillStyle(FeatureCollection fc, Color color,
                                 Hashtable<Color, String> colorMap,
                                 Color lineColor, boolean doLine,
                                 Element folder, int[] styleCnt,
                                 String balloonSchema, String balloonTemplate)
            throws Exception {
        String styleUrl = colorMap.get(color);
        if (styleUrl == null) {
            styleUrl = "colorStyle" + (styleCnt[0]++);
            colorMap.put(color, styleUrl);
            //            System.err.println("making style:" + styleUrl);
            //make style
            Element style = KmlUtil.style(folder, styleUrl);
            if (Utils.stringDefined(balloonTemplate)) {
                balloonTemplate = balloonTemplate.replaceAll("\\["
                        + balloonSchema + "/", "[" + styleUrl + "/");
                makeBalloonForDB(fc, style, balloonTemplate);
            }
            Element polystyle = KmlUtil.makeElement(style,
                                    KmlUtil.TAG_POLYSTYLE);
            Element linestyle = KmlUtil.makeElement(style,
                                    KmlUtil.TAG_LINESTYLE);
            KmlUtil.makeText(polystyle, KmlUtil.TAG_COLOR,
                             "66"
                             + KmlUtil.toBGRHexString(color).substring(1));
            KmlUtil.makeText(polystyle, KmlUtil.TAG_COLORMODE, "normal");

            if ( !doLine) {
                KmlUtil.makeText(polystyle, "outline", "0");
            } else {
                KmlUtil.makeText(linestyle, KmlUtil.TAG_WIDTH, "1");
                if (lineColor != null) {
                    KmlUtil.makeText(
                        linestyle, KmlUtil.TAG_COLOR,
                        "ff"
                        + KmlUtil.toBGRHexString(lineColor).substring(1));
                }
            }
        }

        return styleUrl;
    }



    
    public static String getKmlTag(int featureType) {
        String tag = null;
        switch (featureType) {

          case EsriShapefile.POINT :      // 1
          case EsriShapefile.POINTZ :     // 11
              tag = KmlUtil.TAG_POINT;

              break;

          case EsriShapefile.POLYLINE :   // 3
          case EsriShapefile.POLYLINEZ :  // 13
              tag = KmlUtil.TAG_LINESTRING;

              break;

          case EsriShapefile.POLYGON :    // 5
          case EsriShapefile.POLYGONZ :   // 15
              tag = KmlUtil.TAG_POLYGON;

              break;

          default :
              tag = null;

              break;
        }

        return tag;
    }



    
    public static String getKmlTag(EsriShapefile.EsriFeature feature) {
        String tag = null;
        if ((feature instanceof EsriShapefile.EsriPoint)
                || (feature instanceof EsriShapefile.EsriPointZ)) {
            tag = KmlUtil.TAG_POINT;
        } else if ((feature instanceof EsriShapefile.EsriPolyline)
                   || (feature instanceof EsriShapefile.EsriPolylineZ)) {
            tag = KmlUtil.TAG_LINESTRING;
        } else if ((feature instanceof EsriShapefile.EsriPolygon)
                   || (feature instanceof EsriShapefile.EsriPolygonZ)) {
            tag = KmlUtil.TAG_POLYGON;
        }

        return tag;
    }

    
    public Element makeKmlSchema(Hashtable properties, Element parent) {
        Element schema = KmlUtil.makeElement(parent, KmlUtil.TAG_SCHEMA);
        schema.setAttribute(
            KmlUtil.ATTR_NAME,
            (String) properties.get(FeatureCollection.PROP_SCHEMANAME));
        schema.setAttribute(
            KmlUtil.ATTR_ID,
            (String) properties.get(FeatureCollection.PROP_SCHEMAID));
        Hashtable<String, String[]> data =
            (Hashtable<String,
                       String[]>) properties.get(
                           FeatureCollection.PROP_SCHEMA);
        Iterator<Map.Entry<String, String[]>> entries =
            data.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<String, String[]> entry = entries.next();
            Element simple = KmlUtil.makeElement(schema,
                                 KmlUtil.TAG_SIMPLEFIELD);
            String[] attrs = entry.getValue();
            for (int i = 0; i < attrs.length; i += 2) {
                if (attrs[i].equals("name")) {
                    attrs[i + 1] = attrs[i + 1].toLowerCase();
                }
            }
            XmlUtil.setAttributes(simple, attrs);
            KmlUtil.makeText(simple, KmlUtil.TAG_DISPLAYNAME,
                             "&lt;b&gt;" + Utils.makeLabel(entry.getKey())
                             + "&lt;/b&gt;");
        }

        return schema;
    }

    
    private Element makeBalloonForDB(FeatureCollection fc, Element parent,
                                     String template) {
        List<DbaseDataWrapper> fieldDatum = fc.getDatum();
        Element balloon = KmlUtil.makeElement(parent,
                              KmlUtil.TAG_BALLOONSTYLE);
        StringBuilder sb = new StringBuilder();
        if (template != null) {
            sb.append(template);
        } else {
            sb.append("<table cellpadding=\"5\" border=\"0\">\n");
            //            Hashtable<String, String[]> schema =   (Hashtable<String, String[]>) properties.get(PROP_SCHEMA);
            Hashtable properties = fc.getProperties();
            for (DbaseDataWrapper dbd : fieldDatum) {
                //            String label = props.get
                sb.append("<tr><td align=right><b>");
                sb.append(dbd.getLabel());
                sb.append(":</b>&nbsp;&nbsp;</td><td>$[");
                sb.append(properties.get(FeatureCollection.PROP_SCHEMANAME));
                sb.append("/");
                sb.append(dbd.getName());
                sb.append("]</td></tr>\n");
            }
            sb.append("</table>");
        }
        Element node = KmlUtil.makeElement(balloon, KmlUtil.TAG_TEXT);
        CDATASection cdata =
            parent.getOwnerDocument().createCDATASection(sb.toString());
        node.appendChild(cdata);

        return balloon;
    }

}
