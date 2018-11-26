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
import org.ramadda.util.Utils;

import org.w3c.dom.Element;


import java.awt.geom.Rectangle2D;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * A holder for Feature information
 */
public class Feature {

    /** the feature id */
    private String id;

    /** the geometry for this feature */
    private Geometry geometry;

    /** _more_ */
    private List<float[][]> origCoords;

    /** the list of properties */

    private HashMap<String, Object> featureProperties;

    /** _more_ */
    private HashMap allProperties;

    /** _more_ */
    private int numPoints = -1;

    /** _more_ */
    private int maxPoints = -1;

    /** _more_ */
    private int stride = 0;


    /**
     * Create a Feature
     * @param id the id (name)
     * @param geometry the Geometry
     */
    public Feature(String id, Geometry geometry) {
        this(id, geometry, new HashMap<String, Object>(), new HashMap());
    }


    /**
     * Create a Feature
     * @param id the id (name)
     * @param geometry the geometry
     * @param properties  properties for this feature
     * @param featureProperties _more_
     * @param allProperties _more_
     */
    public Feature(String id, Geometry geometry,
                   HashMap<String, Object> featureProperties,
                   HashMap allProperties) {
        this.id                = id;
        this.geometry          = geometry;
        this.featureProperties = featureProperties;
        this.allProperties     = allProperties;
    }


    /**
     * _more_
     *
     * @param stride _more_
     */
    public void setStride(int stride) {
        this.stride = stride;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getNumPoints() {
        if (numPoints < 0) {
            numPoints = 0;
            for (float[][] coord : geometry.getCoordinates()) {
                numPoints += coord[0].length;
            }
            if (maxPoints < 0) {
                maxPoints = numPoints;
            }
        }

        return numPoints;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getMaxPoints() {
        if (maxPoints < 0) {
            getNumPoints();
        }

        return maxPoints;
    }

    /**
     * Set the id of this Feature
     * @param id the id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Get the id of this Feature
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * Set the geometry of this
     * @param geometry the geometry
     */
    public void setGeometry(Geometry geometry) {
        this.geometry = geometry;
    }

    /**
     * Get the geometry of this Feature
     * @return the geometry
     */
    public Geometry getGeometry() {
        return geometry;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public float getMaxDistance() {
        float dmax = -1;
        if (origCoords == null) {
            origCoords = geometry.getCoordinates();
        }
        List<float[][]> newCoords = new ArrayList<float[][]>();
        for (float[][] coords : origCoords) {
            List<float[]> list = new ArrayList<float[]>();
            for (int i = 0; i < coords[0].length; i++) {
                list.add(new float[] { coords[0][i], coords[1][i] });
            }
            dmax = (float) Math.max(dmax,
                                    Utils.getMaxPerpendicularDistance(list,
                                        0, list.size(), null));
        }

        return dmax;
    }



    /**
     * _more_
     *
     * @param epsilon _more_
     */
    public void applyEpsilon(float epsilon) {
        numPoints = -1;
        if (origCoords == null) {
            origCoords = geometry.getCoordinates();
        }
        List<float[][]> newCoords = new ArrayList<float[][]>();
        for (float[][] coords : origCoords) {
            float[][] coords2 = Utils.douglasPeucker(coords, epsilon);
            //            System.err.println("coords:" + coords[0].length +" new length:" + coords2[0].length);
            //            if(true) throw new IllegalArgumentException("");
            newCoords.add(coords2);
        }
        geometry.setCoordinates(newCoords);
    }

    /**
     * Make the Feature element
     * @param parent  the parent to add to
     * @param styleUrl  the style URL
     * @param bounds _more_
     * @return the element
     */
    public Element makeKmlElement(Element parent, String styleUrl,
                                  Rectangle2D.Double bounds) {
        Element placemark = KmlUtil.placemark(parent, getId(), "");
        Element geom      = placemark;
        if (geometry.getCoordinates().size() > 1) {
            geom = KmlUtil.makeElement(placemark, KmlUtil.TAG_MULTIGEOMETRY);
        }
        String geometryType = geometry.getGeometryType();
        if (geometryType.equals(Geometry.TYPE_POINT)) {
            KmlUtil.visible(placemark, true);
        } else {
            if (styleUrl != null) {
                KmlUtil.styleurl(placemark, styleUrl);
            }
        }
        String type = geometry.getGeometryType();
        for (float[][] coords : geometry.getCoordinates()) {
            if (bounds != null) {
                boolean ok = false;
                for (int i = 1; i < coords[0].length; i++) {
                    float x1 = coords[0][i - 1];
                    float y1 = coords[1][i - 1];
                    float x2 = coords[0][i];
                    float y2 = coords[1][i];
                    if (bounds.intersectsLine((double) x1, (double) y1,
                            (double) x2, (double) y2)) {
                        ok = true;

                        break;
                    }
                }
                if ( !ok) {
                    continue;
                }
            }
            if (type.equals(Geometry.TYPE_POINT) || (coords[0].length == 1)) {
                Element point = KmlUtil.makeElement(geom, KmlUtil.TAG_POINT);
                KmlUtil.makeText(point, KmlUtil.TAG_COORDINATES,
                                 coords[0][0] + "," + coords[1][0] + ",0 ");
            } else {
                if (type.equals(Geometry.TYPE_LINESTRING)
                        || type.equals(Geometry.TYPE_MULTILINESTRING)) {
                    KmlUtil.linestring(geom, false, false, coords, true,
                                       stride);
                } else if (type.equals(Geometry.TYPE_POLYGON)
                           || type.equals(Geometry.TYPE_MULTIPOLYGON)) {
                    KmlUtil.polygon(geom, coords, true, stride);
                }
            }
        }
        if ( !featureProperties.isEmpty()
                && (featureProperties.get(FeatureCollection.PROP_SCHEMANAME)
                    != null) && (featureProperties
                        .get(FeatureCollection
                            .PROP_SCHEMAID) != null) && (featureProperties
                                .get(FeatureCollection
                                    .PROP_SCHEMADATA) != null)) {
            String schemaId = featureProperties.get(
                                  FeatureCollection.PROP_SCHEMAID).toString();
            HashMap<String, Object> data =
                (HashMap<String,
                         Object>) featureProperties.get(
                             FeatureCollection.PROP_SCHEMADATA);
            Element xdata = KmlUtil.makeElement(placemark,
                                KmlUtil.TAG_EXTENDEDDATA);
            Element schemaData = KmlUtil.makeElement(xdata,
                                     KmlUtil.TAG_SCHEMADATA);
            schemaData.setAttribute(KmlUtil.ATTR_SCHEMAURL, "#" + schemaId);
            Iterator<Map.Entry<String, Object>> entries =
                data.entrySet().iterator();
            while (entries.hasNext()) {
                Map.Entry<String, Object> entry = entries.next();
                String                    key   =
                    entry.getKey().toLowerCase();
                Object                    obj   = entry.getValue();
                String                    value;
                if (obj instanceof Double) {
                    value = ShapefileOutputHandler.format(
                        ((Double) obj).doubleValue());
                } else if (obj instanceof Integer) {
                    value = ShapefileOutputHandler.format(
                        ((Integer) obj).intValue());
                } else {
                    value = obj.toString().trim();
                }
                //System.err.println(key+":" + obj.getClass().getName()+":" + value);
                String fromProps = (String) allProperties.get("kml." + key
                                       + "." + value);
                if (fromProps != null) {
                    value = fromProps + " (" + value + ")";
                }

                Element simple = KmlUtil.makeText(schemaData,
                                     KmlUtil.TAG_SIMPLEDATA, value);
                simple.setAttribute(KmlUtil.ATTR_NAME, key);
            }
        }

        return placemark;

    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String toGeoJson() {
        List<String> map = new ArrayList<String>();
        map.add("type");
        map.add(Json.quote(FeatureCollection.TYPE_FEATURE));
        if ((getId() != null) && !getId().isEmpty()) {
            map.add("id");
            map.add(Json.quote(getId()));
        }
        if ((featureProperties != null) && !featureProperties.isEmpty()) {
            HashMap<String, Object> data =
                (HashMap<String,
                         Object>) featureProperties.get(
                             FeatureCollection.PROP_SCHEMADATA);
            if (data != null) {
                List<String> schemadata = new ArrayList<String>();
                Iterator<Map.Entry<String, Object>> entries =
                    data.entrySet().iterator();
                while (entries.hasNext()) {
                    Map.Entry<String, Object> entry = entries.next();
                    schemadata.add(entry.getKey());
                    Object value = entry.getValue();
                    if (value instanceof Number) {
                        schemadata.add(value.toString());
                    } else {
                        schemadata.add(Json.quote(value.toString().trim()));
                    }
                }
                if (schemadata != null) {
                    map.add("properties");
                    map.add(Json.map(schemadata, false));
                }
            }
        }
        map.add(Geometry.TYPE_GEOMETRY);
        map.add(geometry.toGeoJson());

        return Json.map(map, false);
    }
}
