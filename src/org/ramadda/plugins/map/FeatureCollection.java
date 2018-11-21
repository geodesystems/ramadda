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


import org.ramadda.util.Json;
import org.ramadda.util.KmlUtil;

import org.w3c.dom.CDATASection;
import org.w3c.dom.Element;

import ucar.unidata.gis.shapefile.EsriShapefile;
import ucar.unidata.xml.XmlUtil;


import java.awt.Color;

import java.util.ArrayList;
import java.util.HashMap;
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
     * Turn this into KML
     *
     * @return the KML Element
     */
    public Element toKml() {
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
        KmlUtil.open(folder, false);
        if (getDescription().length() > 0) {
            KmlUtil.description(folder, getDescription());
        }
        // Apply one style to the entire document
        String styleName = System.currentTimeMillis() + "_"
                           + (int) (Math.random() * 1000);

        // could try to set these dynamically
        Color   lineColor     = Color.red;
        int     lineWidth     = 1;
        String  polyColorMode = "random";
        Color   polyColor     = Color.darkGray;

        Element style         = KmlUtil.style(folder, styleName);
        if (haveSchema) {
            makeBalloonForDB(style);
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
                KmlUtil.makeText(polystyle, KmlUtil.TAG_COLORMODE,
                                 polyColorMode);
                if (polyColorMode.equals("normal")) {
                    KmlUtil.makeText(polystyle, KmlUtil.TAG_COLOR,
                    //"66" + KmlUtil.toBGRHexString(polyColor).substring(1));
                    "66E6E6E6");
                }
            }
        }

        int points = 0;
        for (Feature feature : features) {
            feature.makeKmlElement(folder, "#" + styleName);
            points+=feature.getNumPoints();
            if(points>ShapefileTypeHandler.MAX_POINTS) break;
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
            XmlUtil.setAttributes(simple, attrs);
            KmlUtil.makeText(simple, KmlUtil.TAG_DISPLAYNAME,
                             "&lt;b&gt;" + entry.getKey() + "&lt;/b&gt;");
        }

        return schema;
    }

    /**
     * Make a balloon style for the db schema
     *
     * @param parent  the parent to add to
     *
     * @return the balloon element
     */
    private Element makeBalloonForDB(Element parent) {
        Element balloon = KmlUtil.makeElement(parent,
                              KmlUtil.TAG_BALLOONSTYLE);
        StringBuilder sb = new StringBuilder();
        sb.append("<table boder=\"0\">\n");
        HashMap<String, String[]> schema =
            (HashMap<String, String[]>) properties.get(PROP_SCHEMA);
        for (String fieldName : schema.keySet()) {
            sb.append("<tr><td><b>");
            sb.append(fieldName);
            sb.append("</b></td><td>$[");
            sb.append(properties.get(PROP_SCHEMANAME));
            sb.append("/");
            sb.append(fieldName);
            sb.append("]</td></tr>\n");
        }
        sb.append("</table>");
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
