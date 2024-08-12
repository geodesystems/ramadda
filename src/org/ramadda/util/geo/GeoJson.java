/**
   Copyright (c) 2008-2023 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util.geo;

import org.json.*;

import org.ramadda.util.JsonUtil;
import org.ramadda.util.Utils;
import org.ramadda.util.geo.Bounds;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.StringUtil;


import org.w3c.dom.*;

import java.awt.geom.*;
import java.io.*;


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

    public static final String GEOJSON_MIMETYPE = "application/geo+json";

    public static final String DOWNLOAD_MIMETYPE = "application/forcedownload";


    public static JSONObject read(String f) throws Exception {
        return  new JSONObject(new JSONTokener(new FileInputStream(f)));
    }


    public static void reduce(String file) throws Exception {
        String contents = IOUtil.readContents(file, JsonUtil.class);
	contents = contents.replaceAll("\\.(\\d\\d\\d\\d\\d\\d)[\\d]+",
				       ".$1");
	System.out.println(contents);
    }

    public static JSONObject reduce(JSONObject obj) throws Exception {
        String contents = obj.toString();
	contents = contents.replaceAll("\\.(\\d\\d\\d\\d\\d\\d)[\\d]+",
				       ".$1");
	return new JSONObject(contents);
    }    


    public static void merge(String file1, String file2,String field1,String field2) throws Exception {
	JSONObject j1 =  read(file1);
	JSONObject j2 =  read(file2);
	JSONArray f1=j1.getJSONArray("features");
	JSONArray f2=j2.getJSONArray("features");	
	for(int idx1=0;idx1<f1.length();idx1++) {
	    JSONObject feature1 = f1.getJSONObject(idx1);
	    JSONObject p1 = feature1.getJSONObject("properties");
	    Object v1 = p1.get(field1).toString();
	    //Slow for now
	    JSONObject match = null;
	    for(int idx2=0;idx2<f2.length();idx2++) {
		JSONObject feature2 = f2.getJSONObject(idx2);
		JSONObject p2 = feature2.getJSONObject("properties");
		Object v2 = p2.get(field2).toString();
		if(v1.equals(v2)) {
		    match = p2;
		    break;
		}		    
	    }
	    if(match==null) {
		System.err.println("no match:" + v1);
		continue;
	    }
	    for (String key : match.keySet()) {
		p1.put(key, match.get(key));
	    }
	}
	System.out.println(j1);

    }
    
    public static List<String> getProperties(String file) throws Exception {
	List<String> names = new ArrayList<String>();
	JSONObject obj  =read(file);
        JSONArray             features = readArray(obj, "features");
	if(features.length()==0) return names;
	getProperties(features.getJSONObject(0), names);
	return names;
    }
    
    private  static void getProperties(JSONObject feature, List<String> names) throws Exception {	
	JSONObject props   = feature.getJSONObject("properties");
	String[] nameList = JSONObject.getNames(props);
	if(nameList!=null) {
	    for (String name : nameList) {
		names.add(name);
	    }
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
    public static void toCsv(InputStream json, PrintStream pw,
                                    String colString, boolean addPolygons)
	throws Exception {
        Iterator     iterator = makeIterator(json, colString, addPolygons);
	toCsv(iterator,pw);
    }

    public static void toCsv(Iterator iterator, PrintStream pw)
	throws Exception {	
        List<String> values;
        while ((values = iterator.next()) != null) {
            pw.append(Utils.columnsToString(values, ",", true));
        }
    }


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
        toCsv(is, pw, colString, false);
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
	return makeIterator(new JSONObject(new JSONTokener(json)),colString, addPolygons);
    }

    public static Iterator makeIterator(JSONObject obj, String colString, boolean addPolygons)
	throws Exception {	
        HashSet cols = null;
        if (colString != null) {
            cols = new HashSet();
            for (String tok : StringUtil.split(colString, ",", true, true)) {
                cols.add(tok);
            }
        }

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
    public static void geojsonSubsetByProperty(String file, PrintStream pw, boolean matchAll,
					       List<String>props) throws Exception {
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
	if(crs!=null) {
	    pw.print("\"crs\":");
	    pw.print(crs.toString());
	    pw.println(",");
	}
        pw.print("\"type\":");
        pw.print(quote(type));
        pw.println(",");
        pw.println("\"features\":[");
        JSONArray features = readArray(obj, "features");
        int       cnt      = 0;
	for (int i = 0; i < features.length(); i++) {
	    JSONObject feature   = features.getJSONObject(i);
	    JSONObject jsonProps = feature.getJSONObject("properties");
	    String[]   names     = JSONObject.getNames(jsonProps);
	    boolean allOk = true;
	    boolean anyOk = false;
	    boolean haveProp =false;

	    for(int pidx=0;pidx<props.size() && allOk && !anyOk;pidx+=2) {
		String prop = props.get(pidx);
		String value = props.get(pidx+1);
		boolean   isRegexp = StringUtil.containsRegExp(value);
		if(!Utils.stringDefined(prop)) continue;
		haveProp=true;
		boolean    gotName   = false;
		for (int j = 0; (j < names.length) && !gotName; j++) {
		    String name = names[j];
		    if (name.equalsIgnoreCase(prop)) {
			gotName = true;
			String v = jsonProps.optString(names[j], "");
			boolean matches;
			if (isRegexp) {
			    matches = v.matches(value);
			} else {
			    matches = v.equals(value);
			}
			if(matchAll) {
			    allOk = matches;
			} else {
			    if(matches) anyOk  =true;
			}
		    }
		}
		if ( !gotName) {
		    throw new IllegalArgumentException("Could not find property:"
						       + prop + " properties:" + Arrays.asList(names));
		}
	    }

	    if(matchAll) {
		if(haveProp && !allOk) continue;
	    } else {
		if(haveProp && !anyOk) continue;
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


    private static double dec(double d) {
	return Utils.decimals(d,6);
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
	    
	    if(!GeoUtils.latLonOk(lat,lon)) {
		continue;
	    }
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
	return getBounds(file,null);
    }

    public static Bounds getBounds(String file,List<String> names) throws Exception {
        JSONObject   obj      = read(file);
	return getBounds(obj,names);
    }

    public static Bounds getBounds(JSONObject obj,List<String> names) throws Exception {
        Bounds bounds = null;
        JSONArray    features = readArray(obj, "features");
	
        for (int i = 0; i < features.length(); i++) {
            JSONObject feature = features.getJSONObject(i);
            bounds = getFeatureBounds(feature, bounds, null);
	    if(i==0 && names!=null) {
		getProperties(feature, names);
	    }
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
	if(coords1==null) return bounds;
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
	    if(GeoUtils.latLonOk(lat,lon)) {
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



    public static void split(String f, String prop) throws Exception {
	split(read(f),prop);
    }

    public static void split(JSONObject obj, String prop) throws Exception {
        JSONArray             features = readArray(obj, "features");
	for (int idx1 = 0; idx1 < features.length(); idx1++) {
	    JSONObject feature = features.getJSONObject(idx1);
	    JSONObject properties= feature.getJSONObject("properties");
	    String id = properties.getString(prop);
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

    public static JSONObject reverse(JSONObject obj) throws Exception {
        JSONArray             features = readArray(obj, "features");
	List<Object> objects = new ArrayList<Object>();
	for (int idx1 = 0; idx1 < features.length(); idx1++) {
	    objects.add(0,features.getJSONObject(idx1));
	}
	features.clear();
	features.putAll(objects);
	return obj;
    }



    public static JSONObject stride(JSONObject obj,double step) throws Exception {
        JSONArray             features = readArray(obj, "features");
	List<Object> objects = new ArrayList<Object>();
	double cnt = 0;
	for (int idx1 = 0; idx1 < features.length(); idx1++) {
	    if(step<1) {
		double r = Math.random();
		if(r <= step) {
		    objects.add(features.getJSONObject(idx1));
		}
		continue;
	    }
	    cnt--;
	    if(cnt<=0) {
		cnt=step;
		objects.add(features.getJSONObject(idx1));
	    }
	}
	features.clear();
	features.putAll(objects);
	//	System.err.println("#new features:" + features.length());
	return obj;
    }

    public static JSONObject subset(JSONObject obj,Bounds bounds,boolean intersect) throws Exception {
        JSONArray             features = readArray(obj, "features");
	List<Object> objects = new ArrayList<Object>();
	double cnt = 0;
	for (int idx1 = 0; idx1 < features.length(); idx1++) {
	    boolean debug = false;
	    JSONObject o = features.getJSONObject(idx1);
	    JSONObject            properties = o.optJSONObject("properties");	
	    /*
	    if(properties!=null) {
		double area = properties.getDouble("area");
		if(area==455050000.) {
		    debug=true;
		    System.err.println("props:" + properties);
		}
		}*/


            Bounds b = getFeatureBounds(o, null, null);
	    if(debug) System.err.println("bounds:\n\t" + b+"\n\t" + bounds);
	    if(b==null) {
		continue;
	    }
	    if(intersect) {
		if(bounds.intersects(b)) {
		    objects.add(o);
		    if(debug) System.err.println("intersects - adding to object");
		} else {
		    if(debug) System.err.println("no intersect");
		}

	    } else {
		if(bounds.contains(b)) {
		    objects.add(o);
		}
	    }
	}
	features.clear();
	features.putAll(objects);
	return obj;
    }

    public static JSONObject filter(JSONObject obj,boolean matchAll,
				    List<String>props) throws Exception {
        JSONArray             features = readArray(obj, "features");
	List<Object> objects = new ArrayList<Object>();
	double cnt = 0;
	for (int idx1 = 0; idx1 < features.length(); idx1++) {
	    boolean debug = false;
	    JSONObject feature   = features.getJSONObject(idx1);
	    JSONObject            properties = feature.optJSONObject("properties");	
	    String[]   names     = JSONObject.getNames(properties);
	    boolean allOk = true;
	    boolean anyOk = false;
	    boolean haveProp =false;

	    for(int pidx=0;pidx<props.size() && allOk && !anyOk;pidx+=2) {
		boolean doNot = false;
		String prop = props.get(pidx);
		String value = props.get(pidx+1);
		if(value.startsWith("!")) {
		    doNot = true;
		    value = value.substring(1);
		}
		double dvalue=0;
		String operator = null;
		if(value.startsWith("<")) {
		    operator="lt";
		    dvalue = Double.parseDouble(value.substring(1));
		} else 	if(value.startsWith(">")) {
		    operator="gt";
		    dvalue = Double.parseDouble(value.substring(1));
		} else 	if(value.startsWith("=")) {
		    operator="eq";
		    dvalue = Double.parseDouble(value.substring(1));		    
		}
		boolean   isRegexp = StringUtil.containsRegExp(value);
		if(!Utils.stringDefined(prop)) continue;
		haveProp=true;
		boolean    gotName   = false;
		for (int j = 0; (j < names.length) && !gotName; j++) {
		    String name = names[j];
		    if (name.equalsIgnoreCase(prop)) {
			gotName = true;
			String v = properties.optString(names[j], "");
			boolean matches=false;
			if(operator!=null) {
			    try {
				double d = Double.parseDouble(v);
				if(operator.equals("lt")) matches = d<dvalue;
				else if(operator.equals("gt")) matches = d>dvalue;
				else matches = d==dvalue;				
			    } catch (NumberFormatException nfe) {
				matches=false;
			    }
			} else if (isRegexp) {
			    matches = v.matches(value);
			} else {
			    matches = v.equals(value);
			}
			if(doNot) matches = !matches;
			if(matches) System.err.println(v);
			if(matchAll) {
			    allOk = matches;
			} else {
			    if(matches) anyOk  =true;
			}
		    }
		}
		if ( !gotName) {
		    throw new IllegalArgumentException("Could not find property:"
						       + prop + " properties:" + Arrays.asList(names));
		}
	    }

	    if(matchAll) {
		if(haveProp && !allOk) continue;
	    } else {
		if(haveProp && !anyOk) continue;
	    }
	    objects.add(feature);
	}
	features.clear();
	features.putAll(objects);
	return obj;
    }



    public static JSONObject keep(JSONObject obj,List<String> keepers) throws Exception {
        JSONArray             features = readArray(obj, "features");
	double cnt = 0;
	for (int idx1 = 0; idx1 < features.length(); idx1++) {
	    JSONObject o = features.getJSONObject(idx1);
	    JSONObject            properties = o.optJSONObject("properties");	
	    if(properties==null) continue;
	    String[] nameList = JSONObject.getNames(properties);
	    for(String name: nameList) {
		if(keepers.contains(name) || keepers.contains(name.toLowerCase())) continue;
		properties.remove(name);
	    }
	}
	return obj;
    }



    public static JSONObject first(JSONObject obj,int n) throws Exception {
        JSONArray             features = readArray(obj, "features");
	List<Object> objects = new ArrayList<Object>();
	double cnt = 0;
	for (int idx1 = 0; idx1 < features.length() && idx1<n; idx1++) {
	    objects.add(features.getJSONObject(idx1));
	}
	features.clear();
	features.putAll(objects);
	return obj;
    }
    

    public static double parse(String s) {
	if(s==null || s.trim().length()==0) return Double.NaN;
	return Double.parseDouble(s);
    }


    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
	List<Command> commands = new ArrayList<Command>();
	for(int i=0;i<args.length;i++) {
	    String arg  =args[i];
	    if(arg.equals("-bounds")) {
		System.out.println(getBounds(args[++i]));
		continue;
	    }
	    if(arg.equals("-merge")) {
		merge(args[++i],args[++i],args[++i],args[++i]);
		break;
	    }

	    if(arg.equals("-reverse")) {
		commands.add(new Command() {public JSONObject apply(JSONObject obj) throws Exception {return  reverse(obj);}});
		continue;
	    }
	    if(arg.equals("-reduce")) {
		commands.add(new Command() {public JSONObject apply(JSONObject obj) throws Exception {return  reduce(obj);}});
		continue;
	    }
	    if(arg.equals("-stride")) {
		final double stride = Double.parseDouble(args[++i]);
		commands.add(new Command() {public JSONObject apply(JSONObject obj) throws Exception {return  stride(obj,stride);}});
		continue;
	    }
	    if(arg.equals("-filter")) {
		String p = args[++i];
		p = p.replace("\\!","!");
		final List<String> props = Utils.split(p,",");
		commands.add(new Command() {public JSONObject apply(JSONObject obj) throws Exception {return  filter(obj,true,props);}});
		continue;
	    }	    


	    if(arg.equals("-contained")) {
		final Bounds b = new Bounds(parse(args[++i]),
					    parse(args[++i]),
					    parse(args[++i]),
					    parse(args[++i]));
				    
		commands.add(new Command() {public JSONObject apply(JSONObject obj) throws Exception {return  subset(obj,b,false);}});
		continue;
	    }
	    if(arg.equals("-intersects")) {
		final Bounds b = new Bounds(parse(args[++i]),
					    parse(args[++i]),
					    parse(args[++i]),
					    parse(args[++i]));
				    
		commands.add(new Command() {public JSONObject apply(JSONObject obj) throws Exception {return  subset(obj,b,true);}});
		continue;
	    }	    	    
	    if(arg.equals("-keep")) {
		List<String> keepers = Utils.split(args[++i],",",true,true);
		commands.add(new Command() {public JSONObject apply(JSONObject obj) throws Exception {return  keep(obj,keepers);}});
		continue;
	    }	    	    
	    if(arg.equals("-first")) {
		final int first = Integer.parseInt(args[++i]);
		commands.add(new Command() {public JSONObject apply(JSONObject obj) throws Exception {return  first(obj,first);}});
		continue;
	    }	    
	    if(arg.equals("-split")) {
		final String prop = args[++i];
		commands.add(new Command() {public JSONObject apply(JSONObject obj) throws Exception {split(obj,prop); return obj;}});
		continue;
	    }	    
	    if(arg.equals("-csv")) {
		commands.add(new Command() {public JSONObject apply(JSONObject obj) throws Exception {
		    Iterator     iterator = makeIterator(obj, null,true);
		    toCsv(iterator,System.out);		    
		    return obj;}});
		continue;
	    }
	    if(arg.equals("-print")) {
		commands.add(new Command() {public JSONObject apply(JSONObject obj) throws Exception {System.out.println(obj.toString());return obj;}});
		continue;
	    }	    

	    if(arg.startsWith("-")) {
		System.err.println("Unknown arg:" +arg +" usage commands may be chained together: \n\t-print (Print the GeoJson to stdout)\n\t-bounds (print out the bounds)\n\t-csv (write out the GeoJson as CSV)\n\t-split <property, e.g, GEOID> Split the file to individual features based on property value\n\t-reverse (reverse the feature order)\n\t-first <count> (print out the first count features)\n\t-stride 10 (if stride<0 then it is used to sample) \n\t-intersects north west south east (subset)  \n\t-contained north west south east (subset)\n\t-reduce");
		continue;
	    }


	    JSONObject            obj      = read(arg);
	    for(Command command: commands) {
		obj = command.apply(obj);
	    }
	}
	System.exit(0);
    }


    public interface Command {
	public JSONObject apply(JSONObject obj) throws Exception;
    }

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
            if (featureIdx < 0) {
		featureIdx=0;
                return makeHeader();
            }
            if (featureIdx >= features.length()) {
                return null;
            }
            List<String>      values  = new ArrayList<String>();
            JSONObject        feature = features.getJSONObject(featureIdx++);
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
		Utils.add(values, "" + dec(centroid.getLatitude()),
			  "" + dec(centroid.getLongitude()));
	    } else {
		Utils.add(values,"NaN","NaN");
	    }
	    
            if (addPolygon) {
                StringBuilder poly = new StringBuilder();
                for (List<Point> p2 : pts) {
                    for (Point tuple : p2) {
                        poly.append("" + dec(tuple.getLatitude()));
                        poly.append(";");
                        poly.append("" + dec(tuple.getLongitude()));
                        poly.append(";");
                    }
                }
                values.add(poly.toString());
            }
            return values;
        }


    }




}
