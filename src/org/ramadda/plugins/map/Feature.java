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

import org.w3c.dom.Element;


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

    /** the list of properties */
    private HashMap<String, Object> properties;

    /**
     * Create a Feature
     * @param id the id (name)
     * @param geometry the Geometry
     */
    public Feature(String id, Geometry geometry) {
        this(id, geometry, new HashMap<String, Object>());
    }

    /**
     * Create a Feature
     * @param id the id (name)
     * @param geometry the geometry
     * @param properties  properties for this feature
     */
    public Feature(String id, Geometry geometry,
                   HashMap<String, Object> properties) {
        this.id         = id;
        this.geometry   = geometry;
        this.properties = properties;
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
     * Make the Feature element
     * @param parent  the parent to add to
     * @param styleUrl  the style URL
     * @return the element
     */
    public Element makeKmlElement(Element parent, String styleUrl) {
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
            if (type.equals(Geometry.TYPE_POINT) || (coords[0].length == 1)) {
                Element point = KmlUtil.makeElement(geom, KmlUtil.TAG_POINT);
                KmlUtil.makeText(point, KmlUtil.TAG_COORDINATES,
                                 coords[0][0] + "," + coords[1][0] + ",0 ");
            } else {
                if (type.equals(Geometry.TYPE_LINESTRING)
                        || type.equals(Geometry.TYPE_MULTILINESTRING)) {
                    KmlUtil.linestring(geom, false, false, coords, true);
                } else if (type.equals(Geometry.TYPE_POLYGON)
                           || type.equals(Geometry.TYPE_MULTIPOLYGON)) {
                    KmlUtil.polygon(geom, coords, true);
                }
            }
        }
        if ( !properties.isEmpty()
                && (properties.get(FeatureCollection.PROP_SCHEMANAME)
                    != null) && (properties
                        .get(FeatureCollection
                            .PROP_SCHEMAID) != null) && (properties
                                .get(FeatureCollection
                                    .PROP_SCHEMADATA) != null)) {
            String schemaId =
                properties.get(FeatureCollection.PROP_SCHEMAID).toString();
            HashMap<String, Object> data =
                (HashMap<String,
                         Object>) properties.get(
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
                Element simple = KmlUtil.makeText(schemaData,
                                     KmlUtil.TAG_SIMPLEDATA,
                                     entry.getValue().toString().trim());
                simple.setAttribute(KmlUtil.ATTR_NAME, entry.getKey());
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
        if ((properties != null) && !properties.isEmpty()) {
            HashMap<String, Object> data =
                (HashMap<String,
                         Object>) properties.get(
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
