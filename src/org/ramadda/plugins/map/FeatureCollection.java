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


import org.ramadda.repository.metadata.Metadata;
import org.ramadda.util.ColorTable;

import org.ramadda.util.Json;
import org.ramadda.util.KmlUtil;
import org.ramadda.util.Utils;

import org.w3c.dom.CDATASection;
import org.w3c.dom.Element;

import ucar.unidata.gis.shapefile.DbaseData;
import ucar.unidata.gis.shapefile.DbaseFile;
import ucar.unidata.gis.shapefile.EsriShapefile;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;


import java.awt.Color;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * Class to hold a Collection of Features.  Modeled off the GEOJson spec.
 */
public class FeatureCollection {

    /** FeatureCollection type */
    public final static String TYPE_FEATURE_COLLECTION = "FeatureCollection";

    /** Feature type */
    public final static String TYPE_FEATURE = "Feature";

    /** Collection name */
    private String name = null;

    /** Collection description */
    private String description = null;

    /** List of Features */
    private List<Feature> features = null;

    /** properties */
    private HashMap<String, Object> properties = new HashMap();

    /** Schema property */
    public static final String PROP_SCHEMA = "Schema";

    /** Schema name property */
    public static final String PROP_SCHEMANAME = "SchemaName";

    /** Schema data property */
    public static final String PROP_SCHEMADATA = "SchemaData";

    /** Schema Id property */
    public static final String PROP_SCHEMAID = "SchemaId";

    /** _more_ */
    public static final String PROP_STYLEID = "StyleId";

    /** _more_ */
    public static final String PROP_BALLOON_TEMPLATE = "BalloonTemplate";


    /**
     * Create a FeatureCollection
     */
    public FeatureCollection() {
        this("", null);
    }

    /**
     * Create a FeatureCollection
     *
     * @param name  the name
     * @param description the description
     */
    public FeatureCollection(String name, String description) {
        this(name, description, new HashMap<String, Object>());
        this.name        = name;
        this.description = description;
    }

    /**
     * Create a FeatureCollection
     *
     * @param name   the name
     * @param description  the description
     * @param properties  the properties
     */
    public FeatureCollection(String name, String description,
                             HashMap<String, Object> properties) {
        this.name        = name;
        this.description = description;
        this.properties  = properties;
    }

    /**
     * Set the name
     *
     * @param name the name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the name
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Set the description
     *
     * @param description the description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Get the description
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set the features
     *
     * @param features the features
     */
    public void setFeatures(List<Feature> features) {
        this.features = features;
    }

    /**
     * Get the features
     *
     * @return the features
     */
    public List<Feature> getFeatures() {
        return features;
    }

    /**
     * Set the properties
     *
     * @param props  the properties
     */
    public void setProperties(HashMap props) {
        this.properties = props;
    }

    /**
     * Get the properties
     *
     * @return the properties
     */
    public HashMap getProperties() {
        return properties;
    }

    /**
     * _more_
     *
     * @param color _more_
     * @param colorMap _more_
     * @param folder _more_
     * @param styleCnt _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private String makeFillStyle(Color color,
                                 Hashtable<Color, String> colorMap,
                                 Element folder, int[] styleCnt)
            throws Exception {
        String styleUrl = colorMap.get(color);
        if (styleUrl == null) {
            styleUrl = "#colorStyle" + (styleCnt[0]++);
            colorMap.put(color, styleUrl);
            //            System.err.println("making style:" + styleUrl);
            //make style
            Element style = KmlUtil.style(folder, styleUrl);
            Element polystyle = KmlUtil.makeElement(style,
                                    KmlUtil.TAG_POLYSTYLE);
            KmlUtil.makeText(polystyle, KmlUtil.TAG_COLOR,
                             "66"
                             + KmlUtil.toBGRHexString(color).substring(1));
            KmlUtil.makeText(polystyle, KmlUtil.TAG_COLORMODE, "normal");
        }

        return styleUrl;
    }

    /**
     * Turn this into KML
     *
     * @return the KML Element
     *
     * @throws Exception _more_
     */
    public Element toKml() throws Exception {

        Element root       = KmlUtil.kml(getName());
        Element doc        = KmlUtil.document(root, getName(), true);
        boolean haveSchema = false;
        if ( !properties.isEmpty()
                && (properties.get(PROP_SCHEMANAME) != null)
                && (properties.get(PROP_SCHEMAID) != null)
                && (properties.get(PROP_SCHEMA) != null)) {
            makeKmlSchema(doc);
            haveSchema = true;
        }
        Element folder = KmlUtil.folder(doc, getName(), true);
        // Apply one style to the entire document
        String styleName = (String) properties.get(PROP_STYLEID);
        if (styleName == null) {
            styleName = "" + (int) (Math.random() * 1000);
        }

        String        colorByField = null;
        Metadata      colorBy      = (Metadata) properties.get("colorby");
        DbaseFile     dbfile       = (DbaseFile) properties.get("dbfile");
        EsriShapefile shapefile = (EsriShapefile) properties.get("shapefile");
        //Correspond to the index
        List<Color>  colors    = null;
        List<String> styleUrls = null;
        int[]        styleCnt  = { 0 };
        if ((colorBy != null) && (dbfile != null)) {
            Hashtable<Color, String> colorMap = new Hashtable<Color,
                                                    String>();
            colorByField = colorBy.getAttr1();
            String[] fieldNames = dbfile.getFieldNames();
            colorByField = colorByField.trim();
            DbaseData dbaseField = null;
            for (int j = 0; j < fieldNames.length; j++) {
                if (fieldNames[j].equalsIgnoreCase(colorByField)) {
                    dbaseField = dbfile.getField(j);

                    break;
                }
            }
            double     min = Double.NaN;
            double     max = Double.NaN;
            ColorTable ct  = null;
            if (Utils.stringDefined(colorBy.getAttr2())) {
                ct = ColorTable.getColorTable(colorBy.getAttr2().trim());
            }
            if (Utils.stringDefined(colorBy.getAttr3())) {
                min = Double.parseDouble(colorBy.getAttr3());
            }
            if (Utils.stringDefined(colorBy.getAttr4())) {
                max = Double.parseDouble(colorBy.getAttr4());
            }


            List features = shapefile.getFeatures();
            styleUrls = new ArrayList<String>();

            Hashtable<String, Color> valueMap = new Hashtable<String,
                                                    Color>();
            if (ct != null) {
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
                        double value = dbaseField.getDouble(i);
                        if (needMin) {
                            min = Math.min(min, value);
                        }
                        if (needMax) {
                            max = Math.max(max, value);
                        }
                    }
                }
            } else {
                String enums = colorBy.getAttr(5);
                for (String line :
                        StringUtil.split(enums, "\n", true, true)) {
                    List<String> toks = StringUtil.splitUpTo(line, ":", 2);
                    if (toks.size() > 1) {
                        Color c = Utils.decodeColor(toks.get(1).trim(),
                                      (Color) null);
                        if (c != null) {
                            valueMap.put(toks.get(0), c);
                        }
                    }
                }
            }


            for (int i = 0; i < features.size(); i++) {
                Color color = null;
                if (ct != null) {
                    double value = dbaseField.getDouble(i);
                    color = ct.getColor(min, max, value);
                } else {
                    String value = "" + dbaseField.getData(i);
                    color = valueMap.get(value);
                }
                if (color != null) {
                    String styleUrl = makeFillStyle(color, colorMap, folder,
                                          styleCnt);
                    styleUrls.add(styleUrl);
                } else {
                    styleUrls.add(styleName);
                }
            }

        }

        String balloonTemplate =
            (String) properties.get(PROP_BALLOON_TEMPLATE);
        KmlUtil.open(folder, false);
        if (getDescription().length() > 0) {
            KmlUtil.description(folder, getDescription());
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
            makeBalloonForDB(style, balloonTemplate);
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
        int cnt    = 0;
        for (Feature feature : features) {
            String styleUrl = (styleUrls == null)
                              ? styleName
                              : styleUrls.get(cnt);
            feature.makeKmlElement(folder, "#" + styleUrl);
            cnt++;
            points += feature.getNumPoints();
            if (points > ShapefileTypeHandler.MAX_POINTS) {
                break;
            }
        }

        return root;

    }


    /**
     * Get the KML tag corresponding the the EsriShapefileFeature
     *
     * @param featureType feature type
     * @return the corresponding KML tag or null
     */
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



    /**
     * Get the KML tag corresponding the the EsriFeature
     * @param feature the feature
     * @return the corresponding KML tag or null
     */
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

    /**
     * Make the KML schema element
     *
     * @param parent  the one to add to
     *
     * @return  the element
     */
    public Element makeKmlSchema(Element parent) {
        Element schema = KmlUtil.makeElement(parent, KmlUtil.TAG_SCHEMA);
        schema.setAttribute(KmlUtil.ATTR_NAME,
                            (String) properties.get(PROP_SCHEMANAME));
        schema.setAttribute(KmlUtil.ATTR_ID,
                            (String) properties.get(PROP_SCHEMAID));
        HashMap<String, String[]> data =
            (HashMap<String, String[]>) properties.get(PROP_SCHEMA);
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

    /**
     * Make a balloon style for the db schema
     *
     * @param parent  the parent to add to
     * @param template _more_
     *
     * @return the balloon element
     */
    private Element makeBalloonForDB(Element parent, String template) {
        Element balloon = KmlUtil.makeElement(parent,
                              KmlUtil.TAG_BALLOONSTYLE);
        StringBuilder sb = new StringBuilder();
        if (template != null) {
            sb.append(template);
        } else {
            sb.append("<table cellpadding=\"5\" border=\"0\">\n");
            HashMap<String, String[]> schema =
                (HashMap<String, String[]>) properties.get(PROP_SCHEMA);
            for (String fieldName : schema.keySet()) {
                //            String label = props.get
                sb.append("<tr><td align=right><b>");
                sb.append(Utils.makeLabel(fieldName));
                sb.append(":</b>&nbsp;&nbsp;</td><td>$[");
                sb.append(properties.get(PROP_SCHEMANAME));
                sb.append("/");
                sb.append(fieldName.toLowerCase());
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

    /**
     * _more_
     *
     * @return _more_
     */
    public String toGeoJson() {
        List<String> map = new ArrayList<String>();
        map.add("type");
        map.add(Json.quote(TYPE_FEATURE_COLLECTION));
        map.add("features");
        List<String> flist = new ArrayList<String>();
        for (Feature feature : features) {
            flist.add(feature.toGeoJson());
        }
        map.add(Json.list(flist, false));

        return Json.map(map, false);
    }

}
