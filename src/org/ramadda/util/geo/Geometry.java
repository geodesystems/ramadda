/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util.geo;


import org.ramadda.util.JsonUtil;
import org.ramadda.util.Utils;


import org.ramadda.util.geo.GeoUtils;

import java.awt.geom.*;

import java.util.ArrayList;
import java.util.List;


/**
 * Class to hold a Geometry which has a type and a list of coordinates.  If the
 * coordinates list has more than one element, the this is a Multi* geometry.  Coordinates are
 * in (x,y(,z)) order.
 */
public class Geometry {

    /** Geometry type */
    public final static String TYPE_GEOMETRY = "geometry";

    /** GeometryCollection type */
    public final static String TYPE_GEOMETRY_COLLECTION =
        "GeometryCollection";

    /** Point type */
    public final static String TYPE_POINT = "Point";

    /** MultiPoint type */
    public final static String TYPE_MULTIPOINT = "MultiPoint";

    /** LineString type */
    public final static String TYPE_LINESTRING = "LineString";

    /** MultiLineString type */
    public final static String TYPE_MULTILINESTRING = "MultiLineString";

    /** Polygon type */
    public final static String TYPE_POLYGON = "Polygon";

    /** MultiPolygon type */
    public final static String TYPE_MULTIPOLYGON = "MultiPolygon";

    /** the type */
    private String geometryType;

    /** the coordinates (x,y(,z) order) */
    private List<float[][]> coords;

    /** _more_ */
    private List<Path2D.Double> paths;

    /**  */
    private Bounds bounds;


    /**
     * Create a Geometry
     * @param geometryType the type
     * @param coords the coordinates
     */
    public Geometry(String geometryType, List<float[][]> coords) {
        this.geometryType = geometryType;
        this.coords       = coords;
    }

    /**
     * _more_
     *
     * @param lat _more_
     * @param lon _more_
     *
     * @return _more_
     */
    public boolean contains(float lat, float lon) {
        if ( !getBounds().contains(lat, lon)) {
            return false;
        }
        for (Path2D.Double path : getPaths()) {
            if (path.contains(lon, lat)) {
                return true;
            }
        }

        return false;
    }


    /**
      * @return _more_
     */
    public Bounds getBounds() {
        if (bounds == null) {
            bounds = new Bounds();
            for (float[][] coord : coords) {
                if (coord == null) {
                    continue;
                }
                int numPoints = coord[0].length;
                for (int i = 0; i < numPoints; i++) {
                    float lon = coord[0][i];
                    float lat = coord[1][i];
                    bounds.expand(lat, lon);
                }
            }
        }

        return bounds;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List<Path2D.Double> getPaths() {
        if (paths == null) {
            List<Path2D.Double> tmp = new ArrayList<Path2D.Double>();
            for (float[][] coord : coords) {
                if (coord == null) {
                    continue;
                }
                Path2D.Double path = new Path2D.Double();
                tmp.add(path);
                int numPoints = coord[0].length;
                for (int i = 0; i < numPoints; i++) {
                    float lon = coord[0][i];
                    float lat = coord[1][i];
                    if (i == 0) {
                        path.moveTo(lon, lat);
                    } else {
                        path.lineTo(lon, lat);
                    }
                }
            }
            paths = tmp;
        }

        return paths;
    }


    /**
     * Set the type of this Geometry
     * @param tag the type
     */
    public void setGeometryType(String tag) {
        this.geometryType = tag;
    }

    /**
     * Get the type of this Geometry
     * @return the type
     */
    public String getGeometryType() {
        return geometryType;
    }

    /**
     * Set the coordinates of this Geometry
     * @param coords the coordinates (x,y(,z) order)
     */
    public void setCoordinates(List<float[][]> coords) {
        this.coords = coords;
    }

    /**
     * Get the coordinates of this Geometry
     * @return the coordinates (x,y(,z)) order
     */
    public List<float[][]> getCoordinates() {
        return coords;
    }


    /**
     * _more_
     *
     *
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void toGeoJson(Appendable sb) throws Exception {
        sb.append(JsonUtil.mapOpen());
        JsonUtil.attr(sb, "type", JsonUtil.quote(geometryType));
        sb.append(",\n");
        sb.append(JsonUtil.mapKey("coordinates"));
        sb.append(getCoordsString());
        sb.append(JsonUtil.mapClose());
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public float[] getCenter() {
        return getCenter(false);
    }

    /**
     * _more_
     *
     * @param debug _more_
     *
     * @return _more_
     */
    public float[] getCenter(boolean debug) {


        float minLat = Float.NaN;
        float minLon = Float.NaN;
        float maxLat = Float.NaN;
        float maxLon = Float.NaN;

        for (float[][] coord : coords) {
            if (coord == null) {
                continue;
            }
            int numPoints = coord[0].length;
            for (int i = 0; i < numPoints; i++) {
                float lon = coord[0][i];
                float lat = coord[1][i];
                minLat = Utils.min(minLat, lat);
                maxLat = Utils.max(maxLat, lat);
                minLon = Utils.min(minLon, lon);
                maxLon = Utils.max(maxLon, lon);
            }
        }
        float cLat = minLat + (maxLat - minLat) / 2.0f;
        float cLon = minLon + (maxLon - minLon) / 2.0f;

        return new float[] { cLat, cLon };
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getCoordsString() {
        List<String> allCoords = new ArrayList<String>();
        for (float[][] coord : coords) {
            int          numPoints = coord[0].length;
            List<String> points    = new ArrayList<String>();
            if (coord != null) {
                int numvals = coord.length;
                for (int i = 0; i < numPoints; i++) {
                    StringBuilder coordStr = new StringBuilder("[");
                    for (int j = 0; j < numvals; j++) {
                        //coordStr.append(Misc.format(coord[j][i]));
                        coordStr.append(coord[j][i]);
                        if (j < numvals - 1) {
                            coordStr.append(",");
                        }
                    }
                    coordStr.append("]");
                    points.add(coordStr.toString());
                }
                if ((numPoints == 1) || geometryType.equals(TYPE_POINT)) {
                    allCoords.add(points.get(0));
                } else {
                    int           num = 0;
                    StringBuilder buf = new StringBuilder();
                    // basic building of LineString, Polygon
                    buf.append("[");
                    for (String point : points) {
                        buf.append(point);
                        if (num < points.size() - 1) {
                            buf.append(",");
                        }
                        num++;
                    }
                    buf.append("]");
                    String coordStr = buf.toString();
                    if (geometryType.equals(TYPE_POLYGON)
                            || geometryType.equals(TYPE_MULTIPOLYGON)) {
                        coordStr = JsonUtil.list(coordStr);
                    }
                    allCoords.add(coordStr);
                }
            }
        }
        StringBuilder sb  = new StringBuilder();
        int           idx = 0;
        for (String coordStr : allCoords) {
            sb.append(coordStr);
            if ((allCoords.size() > 1) && (idx < allCoords.size() - 1)) {
                sb.append(",");
            }
            idx++;
        }
        if (geometryType.equals(TYPE_MULTILINESTRING)
                || geometryType.equals(TYPE_MULTIPOINT)
                || geometryType.equals(TYPE_MULTIPOLYGON)) {
            StringBuilder buf = new StringBuilder();
            buf.append(JsonUtil.listOpen());
            buf.append(sb.toString());
            buf.append(JsonUtil.listClose());

            return buf.toString();
        }

        return sb.toString();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        try {
            StringBuilder sb = new StringBuilder();
            toGeoJson(sb);

            return sb.toString();
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    /**
     * _more_
     *
     * @param args _more_
     */
    public static void main(String[] args) {
        int numcoords = (args.length == 0)
                        ? 2
                        : Integer.parseInt(args[0]);
        numcoords = Math.min(numcoords, 3);
        int             numpoints = (args.length > 1)
                                    ? Integer.parseInt(args[1])
                                    : 10;
        int             numGeoms  = (args.length > 2)
                                    ? Integer.parseInt(args[2])
                                    : 1;
        int             typeType  = (args.length > 3)
                                    ? Integer.parseInt(args[3])
                                    : 0;
        List<float[][]> allCoords = new ArrayList<float[][]>();
        float[][]       coords    = new float[numcoords][numpoints];
        float           latin     = 40.5f;
        float           lonin     = -105;
        float           altin     = 10;
        for (int j = 0; j < numGeoms; j++) {
            float offset = j * 5.f;
            for (int i = 0; i < numpoints; i++) {
                lonin        -= (float) Math.random() * offset;
                latin        += (float) Math.random() * offset;
                coords[0][i] = lonin;
                coords[1][i] = latin;
                if (numcoords > 2) {
                    altin        += (float) Math.random() * 10f;
                    coords[2][i] = altin;
                }
            }
            allCoords.add(coords);
        }
        String type = Geometry.TYPE_POINT;
        if (numpoints == 1) {
            if (allCoords.size() > 1) {
                type = Geometry.TYPE_MULTIPOINT;
            }
        } else {
            if (typeType == 0) {
                type = Geometry.TYPE_LINESTRING;
                if (allCoords.size() > 1) {
                    type = Geometry.TYPE_MULTILINESTRING;
                }
            } else {
                type = Geometry.TYPE_POLYGON;
                if (allCoords.size() > 1) {
                    type = Geometry.TYPE_MULTIPOLYGON;
                }
            }
        }
        Geometry geom = new Geometry(type, allCoords);
        System.out.println(geom);
    }
}
