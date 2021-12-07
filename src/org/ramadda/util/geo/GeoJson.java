/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util.geo;


import org.ramadda.util.geo.Bounds;
import org.ramadda.util.Json;
import org.ramadda.util.Utils;

import org.json.*;

import org.w3c.dom.*;


import ucar.unidata.util.IOUtil;

import java.awt.geom.*;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.text.StringCharacterIterator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;


/**
 * JSON Utility class
 */
@SuppressWarnings("unchecked")
public class GeoJson extends Json {

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
    public static void geojsonFileToCsv(String file, PrintStream pw,
                                        String colString)
            throws Exception {
        InputStream    is   = IOUtil.getInputStream(file, Json.class);
        BufferedReader br   = new BufferedReader(new InputStreamReader(is));

        StringBuilder  json = new StringBuilder();
        String         input;
        while ((input = br.readLine()) != null) {
            json.append(input);
            json.append("\n");
        }

        geojsonToCsv(json.toString(), pw, colString, false);
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
    public static void geojsonToCsv(String json, Appendable pw,
                                    String colString, boolean addPolygons)
            throws Exception {
        HashSet cols = null;
        if (colString != null) {
            cols = new HashSet();
            for (String tok : StringUtil.split(colString, ",", true, true)) {
                cols.add(tok);
            }
        }

        JSONObject obj      = new JSONObject(json);
        JSONArray  features = readArray(obj, "features");
        //        List<String> names    = null;
        String[] names = null;
        for (int i = 0; i < features.length(); i++) {
            //            if((i%100)==0) System.err.println("cnt:" + i);
            JSONObject feature = features.getJSONObject(i);
            JSONObject props   = feature.getJSONObject("properties");
            if (names == null) {
                names = JSONObject.getNames(props);
                for (String name : names) {
                    if ((cols != null) && !cols.contains(name)) {
                        continue;
                    }
                    pw.append(name.toLowerCase());
                    pw.append(",");
                }
                pw.append("longitude,latitude");
                if (addPolygons) {
                    pw.append(",polygon");
                }
                pw.append("\n");
                //              pw.append("location\n");
            }

            List<List<Point>> pts = null;
            if (addPolygons) {
                pts = new ArrayList<List<Point>>();
            }
            Bounds    bounds   = getFeatureBounds(feature, null, pts);
            JSONArray geom     = readArray(feature, "geometry.coordinates");
            String    type     = readValue(feature, "geometry.type", "NULL");
            Point  centroid = bounds.getCenter();
            for (String name : names) {
                String value = props.optString(name, "");
                if (value.indexOf(",") >= 0) {
                    value = "\"" + value + "\"";
                }
                pw.append(value);
                pw.append(",");
            }
            pw.append(centroid.getLatitude() + "," + centroid.getLongitude());
            if (addPolygons) {
                pw.append(",");
                for (List<Point>  p2 : pts) {
		    for (Point tuple : p2) {
			pw.append("" + tuple.getLatitude());
			pw.append(";");
			pw.append("" + tuple.getLongitude());
			pw.append(";");
		    }
                }
            }
            pw.append("\n");
        }
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
    public static List<Feature> getFeatures(String file)
            throws Exception {
	String json = IOUtil.readContents(file, GeoJson.class);
	List<Feature> features = new ArrayList<Feature>();
        JSONObject obj      = new JSONObject(json);
        JSONArray  jsonFeatures = readArray(obj, "features");
        //        List<String> names    = null;
        String[] names = null;
        for (int i = 0; i < jsonFeatures.length(); i++) {
            //            if((i%100)==0) System.err.println("cnt:" + i);
            JSONObject jsonFeature = jsonFeatures.getJSONObject(i);
            JSONObject jsonProps   = jsonFeature.getJSONObject("properties");
            List<List<Point>> points =  new ArrayList<List<Point>>();
            Bounds    bounds   = getFeatureBounds(jsonFeature, null, points);
            JSONArray jsonGeom     = readArray(jsonFeature, "geometry.coordinates");
            String    type     = readValue(jsonFeature, "geometry.type", "NULL");
	    Hashtable<String, Object> props= new Hashtable<String,Object>();
	    if(names==null) {
                names = JSONObject.getNames(jsonProps);
	    }

            for (String name : names) {
                String value = jsonProps.optString(name, "");
                if (value.indexOf(",") >= 0) {
                    value = "\"" + value + "\"";
                }
		props.put(name,value);
            }

	    List<float[][]>parts = new ArrayList<float[][]>();
	    for(List<Point> p2: points) {
		float[][] pts = new float[2][p2.size()];
		parts.add(pts);
		for (int ptIdx = 0; ptIdx < p2.size(); ptIdx++) {
		    Point tuple = p2.get(ptIdx);
		    pts[1][ptIdx] = (float) tuple.getLatitude();
		    pts[0][ptIdx] = (float) tuple.getLongitude();
		}
            }
	    Geometry geom = new Geometry("Polygon",parts);
	    Feature feature =new Feature("",geom,props,null);
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
        InputStream    is   = IOUtil.getInputStream(file, Json.class);
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
            Bounds   bounds   = getFeatureBounds(feature, null, null);
            Point centroid = bounds.getCenter();
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
	//	System.err.println("prop:" + prop);
	//	System.err.println("value:" + value);	
        InputStream    is   = IOUtil.getInputStream(file, Json.class);
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

	boolean isRegexp = StringUtil.containsRegExp(value);
        for (int i = 0; i < features.length(); i++) {
            JSONObject feature = features.getJSONObject(i);
            JSONObject jsonProps   = feature.getJSONObject("properties");
            String[]   names   = JSONObject.getNames(jsonProps);
            boolean    haveIt  = false;
            for (int j = 0; (j < names.length) && !haveIt; j++) {
		String name = names[j];
		if(name.equalsIgnoreCase(prop)) {
		    String v = jsonProps.optString(names[j], "");
		    if(isRegexp) {
			haveIt =  v.matches(value);
		    } else {
			haveIt =  v.equals(value);
		    }
		}
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
                pts.add(new Point(lat, lon ));
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
                                    IOUtil.getInputStream(file, Json.class)));
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
        JSONArray coords1 = readArray(feature, "geometry.coordinates");
        String    type    = readValue(feature, "geometry.type", "NULL");
        if (type.equals("Polygon") || type.equals("MultiLineString")) {
            for (int idx1 = 0; idx1 < coords1.length(); idx1++) {
                JSONArray coords2 = coords1.getJSONArray(idx1);
		List<Point> p2 = new ArrayList<Point>();
                bounds = getBounds(coords2, bounds, p2);
		pts.add(p2);
            }
        } else if (type.equals("MultiPolygon")) {
            for (int idx1 = 0; idx1 < coords1.length(); idx1++) {
                JSONArray coords2 = coords1.getJSONArray(idx1);
                for (int idx2 = 0; idx2 < coords2.length(); idx2++) {
                    JSONArray coords3 = coords2.getJSONArray(idx2);
		    List<Point> p2 = new ArrayList<Point>();
                    bounds = getBounds(coords3, bounds, p2);
		    pts.add(p2);
                }
            }
        } else if (type.equals("LineString")) {
	    List<Point> p2 = new ArrayList<Point>();
            bounds = getBounds(coords1, bounds, p2);
	    pts.add(p2);
        } else {
            double lon = coords1.getDouble(0);
            double lat = coords1.getDouble(1);
	    List<Point> p2 = new ArrayList<Point>();
	    p2.add(new Point(lat,lon));
	    pts.add(p2);
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





    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
	getFeatures(args[0]);
	//	System.err.println(getFeatures(args[0]));
	System.exit(0);


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

          geojsonFileToCsv(args[0], System.out, (args.length > 1)
          ? args[1]
          : null);
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
        geojsonFileToCsv(args[0], System.out, (args.length > 1)
                ? args[1]
                : null);
        //        convertCameras(args);
    }





}
