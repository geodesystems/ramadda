/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util.geo;


import org.json.*;

import org.ramadda.util.JsonUtil;
import org.ramadda.util.Utils;


import org.ramadda.util.geo.Bounds;

import org.ramadda.util.text.Seesv;

import org.w3c.dom.*;


import ucar.unidata.util.IOUtil;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;

import java.awt.geom.*;

import java.io.*;

import java.text.StringCharacterIterator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.List;


/**
 * JSON Utility class
 */
@SuppressWarnings("unchecked")
public class GeoJson extends JsonUtil {

    /**  */
    public static final String GEOJSON_MIMETYPE = "application/geo+json";


    /**
     * _more_
     *
     * @param file _more_
     * @param pw _more_
     * @param colString _more_
     *
     * @throws Exception _more_
     */
    public static void geojsonFileToCsv(String file, PrintStream pw, String colString)
            throws Exception {
        String contents = IOUtil.readContents(file, JsonUtil.class);
        //        InputStream    is   = IOUtil.getInputStream(file, JsonUtil.class);
        InputStream is = new ByteArrayInputStream(contents.getBytes());
        geojsonToCsv(is, pw, colString, false);
    }



    /**
     *
     * @param json _more_
     * @param pw _more_
     * @param colString _more_
     * @param addPolygons _more_
     *
     * @throws Exception _more_
     */
    public static void geojsonToCsv(InputStream json, PrintStream pw,
                                    String colString, boolean addPolygons)
            throws Exception {
        Iterator     iterator = makeIterator(json, colString, addPolygons);
        List<String> values;
        while ((values = iterator.next()) != null) {
            pw.append(Seesv.columnsToString(values, ",", true));
        }


    }

    /**
     *
     * @param json _more_
     * @param colString _more_
     * @param addPolygons _more_
     *  @return _more_
     *
     * @throws Exception _more_
     */
    public static Iterator makeIterator(InputStream json, String colString,
                                        boolean addPolygons)
            throws Exception {
        HashSet cols = null;
        if (colString != null) {
            cols = new HashSet();
            for (String tok : StringUtil.split(colString, ",", true, true)) {
                cols.add(tok);
            }
        }

        JSONObject            obj      =
            new JSONObject(new JSONTokener(json));
        JSONArray             features = readArray(obj, "features");
        LinkedHashSet<String> names    = new LinkedHashSet<String>();
        for (int i = 0; i < features.length(); i++) {
            JSONObject feature = features.getJSONObject(i);
            JSONObject props   = feature.getJSONObject("properties");
	    String[] nameList = JSONObject.getNames(props);
	    if(nameList!=null) {
		for (String name : nameList) {
		    if ( !names.contains(name)) {
			names.add(name);
		    }
		}
	    }
        }

        List<String> nameList = new ArrayList<String>();
        for (String name : names) {
            if ((cols != null) && !cols.contains(name)) {
                continue;
            }
            nameList.add(name);
        }

        return new Iterator(features, nameList, addPolygons);

    }


    /**
     * Class description
     *
     *
     * @version        $version$, Tue, Sep 6, '22
     * @author         Enter your name here...
     */
    public static class Iterator {

        /**  */
        JSONObject obj;

        /**  */
        int featureIdx = -1;

        /**  */
        JSONArray features;

        /**  */
        List<String> names;

        /**  */
        boolean addPolygon;


        /**
         *
         *
         * @param features _more_
         * @param names _more_
         * @param addPolygon _more_
         */
        public Iterator(JSONArray features, List<String> names,
                        boolean addPolygon) {
            this.features   = features;
            this.names      = names;
            this.addPolygon = addPolygon;
        }

        /**
         *  @return _more_
         */
        private List<String> makeHeader() {
            List<String> header = new ArrayList<String>();
            for (String name : names) {
                header.add(name.toLowerCase());
            }
            header.add("latitude");
            header.add("longitude");
            if (addPolygon) {
                header.add("polygon");
            }
            return header;
        }

        /**
         *  @return _more_
         *
         * @throws Exception _more_
         */
        public List<String> next() throws Exception {
            if (featureIdx++ < 0) {
                return makeHeader();
            }
            if (featureIdx >= features.length()) {
                return null;
            }
            List<String>      values  = new ArrayList<String>();
            JSONObject        feature = features.getJSONObject(featureIdx);
            JSONObject        props   = feature.getJSONObject("properties");
            List<List<Point>> pts     = null;
            if (addPolygon) {
                pts = new ArrayList<List<Point>>();
            }
            Bounds    bounds   = getFeatureBounds(feature, null, pts);
	    for (String name : names) {
                String value = props.optString(name, "");
                value = value.replaceAll("\n", " ");
                if (value.indexOf(",") >= 0) {
                    value = "\"" + value + "\"";
                }
                values.add(value);
            }
	    if(bounds!=null) {
		//		JSONArray geom     = readArray(feature, "geometry.coordinates");
		//		String    type     = readValue(feature, "geometry.type", "NULL");

		Point     centroid = bounds.getCenter();
		Utils.add(values, "" + centroid.getLatitude(),
			  "" + centroid.getLongitude());
	    } else {
		Utils.add(values,"NaN","NaN");
	    }
	    
            if (addPolygon) {
                StringBuilder poly = new StringBuilder();
                for (List<Point> p2 : pts) {
                    for (Point tuple : p2) {
                        poly.append("" + tuple.getLatitude());
                        poly.append(";");
                        poly.append("" + tuple.getLongitude());
                        poly.append(";");
                    }
                }
                values.add(poly.toString());
            }
            return values;
        }


    }



    /**
     * @param file _more_
     *
     *  @return _more_
     * @throws Exception _more_
     */
    public static List<Feature> getFeatures(String file) throws Exception {
        String        json         = IOUtil.readContents(file, GeoJson.class);
        List<Feature> features     = new ArrayList<Feature>();
        JSONObject    obj          = new JSONObject(json);
        JSONArray     jsonFeatures = readArray(obj, "features");
        //        List<String> names    = null;
        String[] names = null;
        for (int i = 0; i < jsonFeatures.length(); i++) {
            //            if((i%100)==0) System.err.println("cnt:" + i);
            JSONObject        jsonFeature = jsonFeatures.getJSONObject(i);
            JSONObject jsonProps = jsonFeature.getJSONObject("properties");
            List<List<Point>> points      = new ArrayList<List<Point>>();
            Bounds bounds = getFeatureBounds(jsonFeature, null, points);
            JSONArray jsonGeom = readArray(jsonFeature,
                                           "geometry.coordinates");
            String type = readValue(jsonFeature, "geometry.type", "NULL");
            Hashtable<String, Object> props = new Hashtable<String, Object>();
            if (names == null) {
                names = JSONObject.getNames(jsonProps);
            }

            for (String name : names) {
                String value = jsonProps.optString(name, "");
                if (value.indexOf(",") >= 0) {
                    value = "\"" + value + "\"";
                }
                props.put(name, value);
            }

            List<float[][]> parts = new ArrayList<float[][]>();
            for (List<Point> p2 : points) {
                float[][] pts = new float[2][p2.size()];
                parts.add(pts);
                for (int ptIdx = 0; ptIdx < p2.size(); ptIdx++) {
                    Point tuple = p2.get(ptIdx);
                    pts[1][ptIdx] = (float) tuple.getLatitude();
                    pts[0][ptIdx] = (float) tuple.getLongitude();
                }
            }
            Geometry geom    = new Geometry("Polygon", parts);
            Feature  feature = new Feature("", geom, props, null);
            features.add(feature);
        }

        return features;

    }




    /**
     *
     * @param file _more_
     * @param pw _more_
     *
     * @throws Exception _more_
     */
    public static void geojsonPolygon(String file, PrintStream pw)
            throws Exception {
        InputStream    is   = IOUtil.getInputStream(file, JsonUtil.class);
        BufferedReader br   = new BufferedReader(new InputStreamReader(is));
        StringBuilder  json = new StringBuilder();
        String         input;
        while ((input = br.readLine()) != null) {
            json.append(input);
            json.append("\n");
        }
        JSONObject obj      = new JSONObject(json.toString());
        JSONArray  features = readArray(obj, "features");
        String[]   names    = null;
        pw.println("#name,latitude,longitude,points");
        for (int i = 0; i < features.length(); i++) {
            //            if((i%100)==0) System.err.println("cnt:" + i);
            JSONObject feature = features.getJSONObject(i);
            JSONObject props   = feature.getJSONObject("properties");
            //      for(String name: JSONObject.getNames(props)) {pw.println(name);}

            String name = props.optString("name");
            if ( !Utils.stringDefined(name)) {
                name = props.optString("NAME");
            }
            Bounds bounds   = getFeatureBounds(feature, null, null);
            Point  centroid = bounds.getCenter();
            pw.print(name + ",");
            //            pw.print(centroid[1] + "," + centroid[0]+",");
            String    polygon = getFeaturePolygon(feature);
            JSONArray geom    = readArray(feature, "geometry.coordinates");
            String    type    = readValue(feature, "geometry.type", "NULL");
            //            pw.print(polygon);
            pw.println("");
        }
    }



    /**
     * _more_
     *
     * @param file _more_
     * @param pw _more_
     * @param prop _more_
     * @param value _more_
     *
     * @throws Exception _more_
     */
    public static void geojsonSubsetByProperty(String file, PrintStream pw,
            String prop, String value)
            throws Exception {
        //      System.err.println("prop:" + prop);
        //      System.err.println("value:" + value);   
        InputStream    is   = IOUtil.getInputStream(file, JsonUtil.class);
        BufferedReader br   = new BufferedReader(new InputStreamReader(is));

        StringBuilder  json = new StringBuilder();
        String         input;
        while ((input = br.readLine()) != null) {
            json.append(input);
            json.append("\n");
        }
        JSONObject obj  = new JSONObject(json.toString());
        JSONObject crs  = readObject(obj, "crs");
        String     type = obj.optString("type", "");
        pw.println("{");
        pw.print("\"crs\":");
        pw.print(crs.toString());
        pw.println(",");
        pw.print("\"type\":");
        pw.print(quote(type));
        pw.println(",");
        pw.println("\"features\":[");
        JSONArray features = readArray(obj, "features");
        int       cnt      = 0;

        boolean   isRegexp = StringUtil.containsRegExp(value);
        for (int i = 0; i < features.length(); i++) {
            JSONObject feature   = features.getJSONObject(i);
            JSONObject jsonProps = feature.getJSONObject("properties");
            String[]   names     = JSONObject.getNames(jsonProps);
            boolean    haveIt    = false;
            boolean    gotName   = false;
            for (int j = 0; (j < names.length) && !haveIt; j++) {
                String name = names[j];
                if (name.equalsIgnoreCase(prop)) {
                    gotName = true;
                    String v = jsonProps.optString(names[j], "");
                    if (isRegexp) {
                        haveIt = v.matches(value);
                    } else {
                        haveIt = v.equals(value);
                    }
                }
            }

            if ( !gotName) {
                throw new IllegalArgumentException("Could not find property:"
                        + prop + " properties:" + Arrays.asList(names));
            }
            if ( !haveIt) {
                continue;
            }
            if (cnt > 0) {
                pw.println(",");
            }
            cnt++;
            feature.put("id", cnt);
            pw.print(feature.toString());
        }
        pw.println("");
        pw.println("]}");
    }



    /**
     * _more_
     *
     * @param points _more_
     * @param bounds _more_
     * @param pts _more_
     *
     * @return _more_
     */
    private static Bounds getBounds(JSONArray points, Bounds bounds,
                                    List<Point> pts) {
        for (int j = 0; j < points.length(); j++) {
            JSONArray tuple = points.getJSONArray(j);
            double    lon   = tuple.getDouble(0);
            double    lat   = tuple.getDouble(1);
            if (pts != null) {
                pts.add(new Point(lat, lon));
            }

            if (bounds == null) {
                bounds = new Bounds(lat, lon, lat, lon);
            } else {
                bounds.expand(lat, lon);
            }
        }

        return bounds;
    }

    /**
     *
     * @param points _more_
     *
     * @return _more_
     */
    private static String getLatLonString(JSONArray points) {
        StringBuilder sb = new StringBuilder();
        for (int j = 0; j < points.length(); j++) {
            JSONArray tuple = points.getJSONArray(j);
            System.err.println("X:" + tuple.optString(0));
            System.err.println("Y:" + tuple.optString(1));
            double lon = tuple.getDouble(0);
            double lat = tuple.getDouble(1);
            if (j > 0) {
                sb.append(",");
            }
            sb.append(lat);
            sb.append(",");
            sb.append(lon);
        }

        return sb.toString();
    }


    /**
     * _more_
     *
     * @param file _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static Bounds getBounds(String file) throws Exception {
        Bounds bounds = null;
        BufferedReader br = new BufferedReader(
                                new InputStreamReader(
                                    IOUtil.getInputStream(
                                        file, JsonUtil.class)));
        StringBuilder json = new StringBuilder();
        String        input;
        while ((input = br.readLine()) != null) {
            json.append(input);
            json.append("\n");
        }
        JSONObject   obj      = new JSONObject(json.toString());
        JSONArray    features = readArray(obj, "features");
        List<String> names    = null;
        for (int i = 0; i < features.length(); i++) {
            //            if((i%100)==0) System.err.println("cnt:" + i);
            JSONObject feature = features.getJSONObject(i);
            bounds = getFeatureBounds(feature, bounds, null);
        }

        return bounds;
    }

    /**
     * _more_
     *
     * @param feature _more_
     * @param bounds _more_
     * @param pts _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static Bounds getFeatureBounds(JSONObject feature, Bounds bounds,
                                          List<List<Point>> pts)
            throws Exception {
        JSONArray coords1;
        //Catch and ignore null coordinates
        try {
            coords1 = readArray(feature, "geometry.coordinates");
        } catch (Exception ignore) {
            return bounds;
        }
        String type = readValue(feature, "geometry.type", "NULL");
	if(type==null) return bounds;
        if (type.equals("Polygon") || type.equals("MultiLineString")) {
            for (int idx1 = 0; idx1 < coords1.length(); idx1++) {
                JSONArray   coords2 = coords1.getJSONArray(idx1);
                List<Point> p2      = new ArrayList<Point>();
                bounds = getBounds(coords2, bounds, p2);
                if (pts != null) {
                    pts.add(p2);
                }
            }
        } else if (type.equals("MultiPolygon")) {
            for (int idx1 = 0; idx1 < coords1.length(); idx1++) {
                JSONArray coords2 = coords1.getJSONArray(idx1);
                for (int idx2 = 0; idx2 < coords2.length(); idx2++) {
                    JSONArray   coords3 = coords2.getJSONArray(idx2);
                    List<Point> p2      = new ArrayList<Point>();
                    bounds = getBounds(coords3, bounds, p2);
                    if (pts != null) {
                        pts.add(p2);
                    }
                }
            }
        } else if (type.equals("LineString")) {
            List<Point> p2 = new ArrayList<Point>();
            bounds = getBounds(coords1, bounds, p2);
            if (pts != null) {
                pts.add(p2);
            }
        } else {
            double      lon = coords1.getDouble(0);
            double      lat = coords1.getDouble(1);
            List<Point> p2  = new ArrayList<Point>();
            p2.add(new Point(lat, lon));
            if (pts != null) {
                pts.add(p2);
            }
            if (bounds == null) {
                bounds = new Bounds(lat, lon, lat, lon);
            } else {
                bounds.expand(lat, lon);
            }
        }

        return bounds;
    }

    /**
     *
     * @param feature _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static String getFeaturePolygon(JSONObject feature)
            throws Exception {
        StringBuilder sb      = new StringBuilder();
        JSONArray     coords1 = readArray(feature, "geometry.coordinates");
        String        type    = readValue(feature, "geometry.type", "NULL");
        if (type.equals("Polygon") || type.equals("MultiLineString")) {
            for (int idx1 = 0; idx1 < coords1.length(); idx1++) {
                if (sb.length() > 0) {
                    sb.append(";");
                }
                //              System.out.println("type:" + type +" " + coords1);
                sb.append(getLatLonString(coords1));
            }
        } else if (type.equals("MultiPolygon")) {
            for (int idx1 = 0; idx1 < coords1.length(); idx1++) {
                JSONArray coords2 = coords1.getJSONArray(idx1);
                for (int idx2 = 0; idx2 < coords2.length(); idx2++) {
                    JSONArray coords3 = coords2.getJSONArray(idx2);
                    if (sb.length() > 0) {
                        sb.append(";");
                    }
                    sb.append(getLatLonString(coords3));
                }
            }
        } else if (type.equals("LineString")) {
            if (sb.length() > 0) {
                sb.append(";");
            }
            sb.append(getLatLonString(coords1));
        } else {
            if (sb.length() > 0) {
                sb.append(";");
            }
            //TODO
            //      sb.append(lat);
            //      sb.append(",");
            //      sb.append(lon);
        }

        return sb.toString();
    }



    public static void split(String f) throws Exception {
        JSONObject            obj      =
            new JSONObject(new JSONTokener(new FileInputStream(f)));
        JSONArray             features = readArray(obj, "features");
	for (int idx1 = 0; idx1 < features.length(); idx1++) {
	    JSONObject feature = features.getJSONObject(idx1);
	    JSONObject properties= feature.getJSONObject("properties");
	    String id = properties.getString("GEO_ID");
	    System.err.println(id);
	    id = id.replaceAll(".*US","");
	    StringBuilder sb = new StringBuilder("{\n\"type\": \"FeatureCollection\",\"features\": [");
	    sb.append(feature.toString());
	    sb.append("\n");
	    sb.append("]}\n");	    
	    FileOutputStream fos = new FileOutputStream(id+".geojson");
	    fos.write(sb.toString().getBytes());
	    fos.close();
	    //	    if(true) return;
	}

    }

    public static void reverse(String f) throws Exception {
        JSONObject            obj      =
            new JSONObject(new JSONTokener(new FileInputStream(f)));
        JSONArray             features = readArray(obj, "features");
	List<Object> objects = new ArrayList<Object>();
	for (int idx1 = 0; idx1 < features.length(); idx1++) {
	    objects.add(0,features.getJSONObject(idx1));
	}
	features.clear();
	features.putAll(objects);
	System.out.println(obj.toString());
    }


    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {

	reverse(args[0]);
	if(true) return;

        split(args[0]);
	if(true) return;

        geojsonFileToCsv(args[0], System.out, (args.length > 1)
                ? args[1]
                : null);
        if (true) {
            return;
        }

        getFeatures(args[0]);
        //      System.err.println(getFeatures(args[0]));
        Utils.exitTest(0);


        geojsonSubsetByProperty(args[0], System.out, args[1], args[2]);
        if (true) {
            return;
        }


        /*
          geojsonPolygon(args[0], System.out);
          if (true) {
          return;
          }



          geojsonSubsetByProperty(args[0], System.out, args[1], args[2]);
          if (true) {
          return;
          }



          String  file = args[0];
          boolean html = true;
          if (file.equals("-plain")) {
          html = false;
          file = args[1];
          }
          String s = format(file, html);
          if (s != null) {
          System.out.println(s);
          }
          if (true) {
          return;
          }

          System.err.println(getBounds(args[0]));
          if (true) {
          return;
          }


        */


        //        convertCameras(args);
    }





}
