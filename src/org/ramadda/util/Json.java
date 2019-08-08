/*
* Copyright (c) 2008-2019 Geode Systems LLC
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

package org.ramadda.util;


import org.json.*;

import org.w3c.dom.*;


import ucar.unidata.util.IOUtil;


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
public class Json {

    /** JSON MIME type */
    public static final String MIMETYPE = "application/json";

    /** the null string identifier */
    public static final String NULL = "null";

    /** default quote value */
    public static final boolean DFLT_QUOTE = false;

    /** _more_ */
    public static final String FIELD_NAME = "name";

    /** _more_ */
    public static final String FIELD_FIELDS = "fields";

    /** _more_ */
    public static final String FIELD_DATA = "data";

    /** _more_ */
    public static final String FIELD_VALUES = "values";

    /** _more_ */
    public static final String FIELD_LATITUDE = "latitude";

    /** _more_ */
    public static final String FIELD_LONGITUDE = "longitude";

    /** _more_ */
    public static final String FIELD_ELEVATION = "elevation";

    /** _more_ */
    public static final String FIELD_DATE = "date";





    /**
     * _more_
     *
     * @param pw _more_
     * @param lat _more_
     * @param lon _more_
     * @param elevation _more_
     *
     * @throws Exception _more_
     */
    public static void addGeolocation(Appendable pw, double lat, double lon,
                                      double elevation)
            throws Exception {

        pw.append(attr(FIELD_LATITUDE, lat));
        pw.append(",\n");
        pw.append(attr(FIELD_LONGITUDE, lon));
        pw.append(",\n");
        pw.append(attr(FIELD_ELEVATION, elevation));
    }

    /**
     * Create a JSON map
     *
     * @param values  key/value pairs { key1,value1,key2,value2 }
     *
     * @return  the map object { key1:value1, key2:value2 }
     */
    public static String mapAndQuote(String... values) {
        return map(values, true);
    }


    /**
     * Create a JSON map
     *
     * @param values  key/value pairs { key1,value1,key2,value2 }
     *
     * @return  the map object { key1:value1, key2:value2 }
     */
    public static String map(String... values) {
        return map(values, DFLT_QUOTE);
    }

    /**
     * Create a JSON map
     *
     * @param values  key/value pairs [ key1,value1,key2,value2 ]
     *
     * @return  the map object { key1:value1, key2:value2 }
     */
    public static String map(List<String> values) {
        return map(values, DFLT_QUOTE);
    }

    /**
     * _more_
     *
     * @param values _more_
     *
     * @return _more_
     */
    public static String mapAndQuote(List<String> values) {
        return map(values, true);
    }

    /**
     * Create a JSON map
     *
     * @param values  key/value pairs { key1,value1,key2,value2 }
     * @param quoteValue  true to quote the values
     *
     * @return  the map object { key1:value1, key2:value2 }
     */
    public static String map(String[] values, boolean quoteValue) {
        return map((List<String>) Misc.toList(values), quoteValue);
    }

    /**
     * Create a JSON map
     *
     * @param values  key/value pairs [ key1,value1,key2,value2 ]
     * @param quoteValue  true to quote the values
     *
     * @return  the map object { key1:value1, key2:value2 }
     */
    public static String map(List<String> values, boolean quoteValue) {
        StringBuffer row = new StringBuffer();
        map(row, values, quoteValue);

        return row.toString();
    }

    /**
     * _more_
     *
     * @param row _more_
     * @param values _more_
     * @param quoteValue _more_
     */
    public static void map(Appendable row, List<String> values,
                           boolean quoteValue) {
        try {
            row.append(mapOpen());
            int cnt = 0;
            for (int i = 0; i < values.size(); i += 2) {
                String name  = values.get(i);
                String value = values.get(i + 1);
                if (value == null) {
                    continue;
                }
                if (cnt > 0) {
                    row.append(",\n");
                }
                cnt++;
                row.append(attr(name, value, quoteValue));
            }
            row.append(mapClose());
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }


    /**
     * _more_
     *
     * @param key _more_
     *
     * @return _more_
     */
    public static String mapKey(String key) {
        return "\"" + key + "\":";
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public static String mapOpen() {
        return "{";
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public static String mapClose() {
        return "}";
    }

    /**
     * Create a JSON list from the array of strings
     *
     * @param values  list of values { value1,value2,value3,value4 }
     *
     * @return  the values as a JSON array [ value1,value2,value3,value4 ]
     */
    public static String list(String... values) {
        return list(Misc.toList(values));
    }

    /**
     * Create a JSON list from the array of strings
     *
     * @param values  list of values [ value1,value2,value3,value4 ]
     *
     * @return  the values as a JSON array [ value1,value2,value3,value4 ]
     */
    public static String list(List values) {
        return list(values, DFLT_QUOTE);
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public static String listOpen() {
        return "[";
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public static String listClose() {
        return "]";
    }



    /**
     * Create a JSON list from the array of strings
     *
     * @param values  list of values [ value1,value2,value3,value4 ]
     * @param quoteValue  true to quote the values
     *
     * @return  the values as a JSON array [ value1,value2,value3,value4 ]
     */
    public static String list(List values, boolean quoteValue) {
        StringBuffer row = new StringBuffer();
        list(row, values, quoteValue);

        return row.toString();
    }

    /**
     * _more_
     *
     * @param row _more_
     * @param values _more_
     * @param quoteValue _more_
     */
    public static void list(Appendable row, List values, boolean quoteValue) {
        try {
            row.append(listOpen());
            for (int i = 0; i < values.size(); i++) {
                if (i > 0) {
                    row.append(",\n");
                }
                if (quoteValue) {
                    row.append(quote(values.get(i).toString()));
                } else {
                    row.append(values.get(i).toString());
                }
            }
            row.append(listClose());
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }



    /**
     * Create a list of JSON object from a list of TwoFacedObjects
     *
     * @param values  the values
     *
     * @return  the list [ {id:id1,label:label1},{id:id2,label:label2} ]
     */
    public static String tfoList(List<TwoFacedObject> values) {
        return tfoList(values, "id", "label");
    }

    /**
     * Create a list of JSON object from a list of TwoFacedObjects
     *
     * @param values  the values
     * @param idKey   the key for the TwoFacedObject ID
     * @param labelKey   the key for the TwoFacedObject label
     *
     * @return  the list [ {id:id1,label:label1},{id:id2,label:label2} ]
     */
    public static String tfoList(List<TwoFacedObject> values, String idKey,
                                 String labelKey) {
        List<String> arrayVals = new ArrayList<String>();
        for (TwoFacedObject tfo : values) {
            List<String> mapValues = new ArrayList<String>();
            String       id        = TwoFacedObject.getIdString(tfo);
            String       label     = tfo.toString();
            mapValues.add(idKey);
            mapValues.add((id == null)
                          ? label
                          : id);
            mapValues.add(labelKey);
            mapValues.add(label);
            arrayVals.add(map(mapValues, true));
        }

        return list(arrayVals);
    }

    /**
     * Get a string
     *
     * @param s  the string
     * @param quote  true to quote
     *
     * @return the string
     */
    public static String getString(String s, boolean quote) {
        if (s == null) {
            return NULL;
        }
        if (quote) {
            return quote(s);
        }

        return s;
    }



    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     *
     * @return _more_
     */
    public static String attr(String name, double value) {
        return attr(name, formatNumber(value), false);
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     *
     * @return _more_
     */
    public static String attr(String name, long value) {
        return attr(name, "" + value, false);
    }


    /**
     * Create a JSON object attribute
     *
     * @param name  the attribute name
     * @param value  the attribute value
     *
     * @return  the attribute as name:value
     */
    public static String attr(String name, String value) {
        return attr(name, value, DFLT_QUOTE);
    }

    /**
     * Create a JSON object attribute
     *
     * @param name  the attribute name
     * @param value  the attribute value
     * @param quoteValue true to quote the name and value
     *
     * @return  the attribute as name:value
     */
    public static String attr(String name, String value, boolean quoteValue) {
        return mapKey(name) + getString(value, quoteValue);
    }

    /**
     * quote the attribute value and add it to the list
     *
     * @param items the list of items
     * @param name  the attribute name
     * @param value the attribute value
     */
    public static void quoteAttr(List<String> items, String name,
                                 String value) {
        items.add(name);
        items.add(getString(value, true));
    }

    /**
     * Make an attribute and add it to the list
     *
     * @param items  the list of name/value pairs
     * @param name   the attribute name
     * @param value  the attribute value
     */
    public static void attr(List<String> items, String name, String value) {
        items.add(name);
        items.add(getString(value, false));
    }


    /**
     * _more_
     *
     * @param d _more_
     *
     * @return _more_
     */
    public static String formatNumber(double d) {
        if (Double.isNaN(d)) {
            return "null";
        }

        if ((d == Double.NEGATIVE_INFINITY)
                || (d == Double.POSITIVE_INFINITY)) {
            return "null";

        }

        return "" + d;
    }


    /**
     * Quote a string
     *
     * @param s the string
     *
     * @return  the quoted string
     */
    public static String quote(String s) {
        if (s == null) {
            return NULL;
        }
        StringBuilder sb = new StringBuilder();
        quote(sb, s);

        return sb.toString();
    }

    /**
     * _more_
     *
     * @param sb _more_
     * @param s _more_
     */
    public static void quote(Appendable sb, String s) {
        try {
            if (s == null) {
                sb.append(NULL);

                return;
            }
            s = cleanString(s);
            s = s.replaceAll("\n", "\\n");
            s = s.replaceAll("\r", "\\r");
            s = s.replaceAll("\"", "\\\\\"");
            if (s.equals("true") || s.equals("false")) {
                sb.append(s);

                return;
            }
            //This can mess up and match on what should be a string value, e.g.
            //00000000000
            //Not sure what to do here
            //            if(s.matches("^[0-9]+\\.?[0-9]*$")) return s;

            sb.append("\"");
            sb.append(s);
            sb.append("\"");
        } catch (Exception exc) {
            throw new IllegalArgumentException("Could not quote string:" + s);
        }
    }


    /**
     * Clean a string of illegal JSON characters
     *
     * @param aText  the string
     *
     * @return  the cleaned string
     */
    public static String cleanString(String aText) {
        if ( !Utils.stringDefined(aText)) {
            return "";
        }
        final StringBuilder     result      = new StringBuilder();
        StringCharacterIterator iterator = new StringCharacterIterator(aText);
        char                    character   = iterator.current();
        char                    char_slash  = '\\';
        char                    char_dquote = '"';

        while (character != StringCharacterIterator.DONE) {
            if (character == char_dquote) {
                //For now don't escape double quotes
                result.append(character);
                //                result.append(char_slash);
                //                result.append(char_dquote);
            } else if (character == char_slash) {
                result.append(char_slash);
                result.append(char_slash);
            } else if (character == '\b') {
                result.append("\\b");
            } else if (character == '\f') {
                result.append("\\f");
            } else if (character == '\n') {
                result.append("\\n");
            } else if (character == '\r') {
                result.append("\\r");
            } else if (character == '\t') {
                result.append("\\t");
            } else {
                //the char is not a special one
                //add it to the result as is
                result.append(character);
            }
            character = iterator.next();
        }

        String s = result.toString();

        //Make into all ascii ??
        s = s.replaceAll("[^\n\\x20-\\x7E]+", " ");

        return s;
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
    public static void geojsonToCsv(String file, PrintStream pw, String colString)
            throws Exception {
        InputStream is = IOUtil.getInputStream(file, Json.class);
        BufferedReader br = new BufferedReader(
                                new InputStreamReader(
                                                      is));

        HashSet cols = null;
        if (colString != null) {
            cols = new HashSet();
            for (String tok : StringUtil.split(colString, ",", true, true)) {
                cols.add(tok);
            }

        }

        StringBuilder json = new StringBuilder();
        String        input;
        while ((input = br.readLine()) != null) {
            json.append(input);
            json.append("\n");
        }
        JSONObject obj      = new JSONObject(json.toString());
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
                    pw.print(name);
                    pw.print(",");
                }
                //                pw.println("latitude,longitude");
                pw.println("location");
            }

            Bounds    bounds   = getFeatureBounds(feature, null);
            JSONArray geom     = readArray(feature, "geometry.coordinates");
            String    type     = readValue(feature, "geometry.type", "NULL");
            double[]  centroid = bounds.getCenter();
            for (String name : names) {
                String value = props.optString(name, "");
                if (value.indexOf(",") >= 0) {
                    value = "\"" + value + "\"";
                }
                pw.print(value);
                pw.print(",");
            }
            //            pw.println(centroid[1] + "," + centroid[0]);
            pw.println(centroid[1] + ";" + centroid[0]);
        }
    }


    /**
     * _more_
     *
     * @param points _more_
     * @param bounds _more_
     *
     * @return _more_
     */
    private static Bounds getBounds(JSONArray points, Bounds bounds) {
        for (int j = 0; j < points.length(); j++) {
            JSONArray tuple = points.getJSONArray(j);
            double    lon   = tuple.getDouble(0);
            double    lat   = tuple.getDouble(1);
            if (bounds == null) {
                bounds = new Bounds(lat, lon, lat, lon);
            } else {
                bounds.expand(lat, lon);
            }
        }

        return bounds;
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
            bounds = getFeatureBounds(feature, bounds);
        }

        return bounds;
    }

    /**
     * _more_
     *
     * @param feature _more_
     * @param bounds _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static Bounds getFeatureBounds(JSONObject feature, Bounds bounds)
            throws Exception {
        JSONArray coords1 = readArray(feature, "geometry.coordinates");
        String    type    = readValue(feature, "geometry.type", "NULL");
        if (type.equals("Polygon") || type.equals("MultiLineString")) {
            for (int idx1 = 0; idx1 < coords1.length(); idx1++) {
                JSONArray coords2 = coords1.getJSONArray(idx1);
                bounds = getBounds(coords2, bounds);
            }
        } else if (type.equals("MultiPolygon")) {
            for (int idx1 = 0; idx1 < coords1.length(); idx1++) {
                JSONArray coords2 = coords1.getJSONArray(idx1);
                for (int idx2 = 0; idx2 < coords2.length(); idx2++) {
                    JSONArray coords3 = coords2.getJSONArray(idx2);
                    bounds = getBounds(coords3, bounds);
                }
            }
        } else if (type.equals("LineString")) {
            bounds = getBounds(coords1, bounds);
        } else {
            double lon = coords1.getDouble(0);
            double lat = coords1.getDouble(1);
            if (bounds == null) {
                bounds = new Bounds(lat, lon, lat, lon);
            } else {
                bounds.expand(lat, lon);
            }
        }

        return bounds;
    }




    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void convertCameras(String[] args) throws Exception {
        BufferedReader br =
            new BufferedReader(new InputStreamReader(System.in));
        StringBuilder json = new StringBuilder();
        String        input;
        while ((input = br.readLine()) != null) {
            json.append(input);
            json.append("\n");
        }
        JSONObject obj     = new JSONObject(json.toString());
        JSONArray  cameras = readArray(obj, "CameraDetails.Camera");
        System.out.println("<entries>");
        for (int i = 0; i < cameras.length(); i++) {
            JSONObject camera = cameras.getJSONObject(i);

            String     tourId = readValue(camera, "CameraTourId", null);
            if (tourId != null) {
                continue;
            }
            double lat = Double.parseDouble(readValue(camera,
                             "Location.Latitude", "0.0"));
            double lon = Double.parseDouble(readValue(camera,
                             "Location.Longitude", "0.0"));
            JSONArray views = readArray(camera, "CameraView");
            for (int j = 0; j < views.length(); j++) {
                JSONObject view = views.getJSONObject(j);
                StringBuilder desc = new StringBuilder(readValue(view,
                                         "ViewDescription", ""));
                String dttm = readValue(view, "LastUpdatedDate", "");
                String url = "http://www.cotrip.org/"
                             + readValue(view, "ImageLocation", "");
                desc.append("<br>");
                String cameraName = readValue(view, "CameraName", "NA");
                String roadName   = readValue(view, "RoadName", "NA");
                String s1         = cameraName.toLowerCase();
                String s2         = roadName.toLowerCase();
                s1 = s1.replaceAll("\\.", "");
                s2 = s2.replaceAll("\\.", "");
                s1 = s1.replaceAll(" ", "-");
                s2 = s2.replaceAll(" ", "-");
                if (s1.indexOf(s2) < 0) {
                    cameraName = roadName + " - " + cameraName;
                }
                //                System.out.println (cameraName);
                desc.append("Mile marker:  "
                            + readValue(view, "MileMarker", "NA") + "<br>");
                String name    = "CDOT Camera - " + cameraName;
                String inner   = "";
                String dirName = readValue(view, "Direction", "");
                String dir     = null;
                if (dirName.equals("North")) {
                    dir = "0";
                } else if (dirName.equals("Northeast")) {
                    dir = "45";
                } else if (dirName.equals("East")) {
                    dir = "90";
                } else if (dirName.equals("Southeast")) {
                    dir = "135";
                } else if (dirName.equals("South")) {
                    dir = "180";
                } else if (dirName.equals("Southwest")) {
                    dir = "225";
                } else if (dirName.equals("West")) {
                    dir = "270";
                } else if (dirName.equals("Northwest")) {
                    dir = "315";
                }
                if (dir != null) {
                    desc.append("Camera Direction:  " + dirName + "<br>");
                    inner +=
                        "<metadata inherited=\"false\" type=\"camera.direction\"><attr encoded=\"false\" index=\"1\">"
                        + dir + "</attr></metadata>\n";
                }
                inner += HtmlUtils.tag("description", "",
                                       "<![CDATA[" + desc + "]]>");
                System.out.println(XmlUtil.tag("entry",
                        XmlUtil.attrs(new String[] {
                    "type", "type_image_webcam", "url", url, "latitude",
                    lat + "", "longitude", "" + lon, "name", name
                }), inner));

            }
        }
        System.out.println("</entries>");
    }

    /**
     * _more_
     *
     * @param node _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static String xmlToJson(Element node) throws Exception {
        StringBuilder json = new StringBuilder();
        xmlToJson(node, json);

        return json.toString();
    }

    /*

     */

    /**
     * _more_
     *
     * @param node _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    private static void xmlToJson(Element node, Appendable sb)
            throws Exception {
        List<String> attrs = new ArrayList<String>();
        attrs.add("xml_tag");
        attrs.add(quote(node.getTagName()));
        NamedNodeMap nnm = node.getAttributes();
        if (nnm != null) {
            for (int i = 0; i < nnm.getLength(); i++) {
                Attr attr = (Attr) nnm.item(i);
                attrs.add(attr.getNodeName());
                attrs.add(quote(attr.getNodeValue()));
            }
        }
        String text = XmlUtil.getChildText(node);
        if (Utils.stringDefined(text)) {
            text = text.replaceAll("\"", "\\\"");
            attrs.add("xml_text");
            attrs.add(quote(text));
        }

        List<String> childJson = new ArrayList<String>();



        NodeList     children  = node.getChildNodes();



        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if ( !(child instanceof Element)) {
                continue;
            }
            String        tag = ((Element) child).getTagName();

            StringBuilder csb = new StringBuilder();
            xmlToJson((Element) child, csb);
            childJson.add(csb.toString());
        }

        if (childJson.size() > 0) {
            attrs.add("children");
            attrs.add(list(childJson));
        }

        sb.append(map(attrs));

    }


    /**
     * _more_
     *
     * @param obj _more_
     * @param path _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public static String readValue(JSONObject obj, String path, String dflt) {
        List<String> toks = StringUtil.split(path, ".", true, true);
        while (toks.size() > 1) {
            String tok = toks.get(0);
            toks.remove(0);
            if ( !obj.has(tok)) {
                return dflt;
            }
            try {
                obj = obj.getJSONObject(tok);
            } catch (org.json.JSONException ignore) {
                //There is the case where the named field is a string and not an object
                return null;
            }
            if (obj == null) {
                return dflt;
            }
        }

        String key = toks.get(0);
        if ( !obj.has(key)) {
            return dflt;
        }
        Object s = obj.get(key);
        if (s == null) {
            return dflt;
        }

        return s.toString();
    }


    /**
     * _more_
     *
     * @param obj _more_
     * @param path _more_
     *
     * @return _more_
     */
    public static JSONObject readObject(JSONObject obj, String path) {
        List<String> toks = StringUtil.split(path, ".", true, true);
        while (toks.size() > 0) {
            String tok = toks.get(0);
            toks.remove(0);
            obj = obj.getJSONObject(tok);
            if (obj == null) {
                return null;
            }
        }

        return obj;
    }


    /**
     * _more_
     *
     * @param obj _more_
     * @param path _more_
     *
     * @return _more_
     */
    public static JSONArray readArray(JSONObject obj, String path) {
        List<String> toks    = StringUtil.split(path, ".", true, true);
        String       lastTok = toks.get(toks.size() - 1);
        toks.remove(toks.size() - 1);
        while (toks.size() > 0) {
            String tok = toks.get(0);
            toks.remove(0);
            obj = obj.getJSONObject(tok);
            if (obj == null) {
                return null;
            }
        }

        if ( !obj.has(lastTok)) {
            return null;
        }

        return obj.getJSONArray(lastTok);
    }

    /**
     * _more_
     *
     * @param url _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static JSONObject readUrl(String url) throws Exception {
        String json = Utils.readUrl(url);
        if (json == null) {
            return null;
        }

        return new JSONObject(new JSONTokener(json));
    }



    /**
     * _more_
     *
     * @param file _more_
     * @param forHtml _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static String format(String file, boolean forHtml)
            throws Exception {
        String     json = IOUtil.readContents(file, Json.class);
        JSONObject obj  = new JSONObject(json.toString());
        //        String     s    = forHtml?obj.toString().replaceAll("\n"," "):obj.toString(3);
        String s = forHtml
                   ? obj.toString(1)
                   : obj.toString(3);
        if (forHtml) {
            s = s.replaceAll("\t", "  ").replaceAll("<",
                             "&lt;").replaceAll(">", "&gt;");

            s = s.replaceAll(
                "\\{",
                "<span class=ramadda-json-openbracket>{</span><span class='ramadda-json-block'>");
            s = s.replaceAll("( *)\\}( *)([^,])", "</span>$1$2}$3");
            s = s.replaceAll("( *)\\}( *),", "</span>$1}$2,");
            s = s.replaceAll("}}", "}</span>}");

            s = s.replaceAll(
                "\\[",
                "<span class=ramadda-json-openbracket>[</span><span class='ramadda-json-block'>");
            s = s.replaceAll("( *)\\]( *)([^,])", "</span>$1$2]$3");
            s = s.replaceAll("( *)\\]( *),", "</span>$1]$2,");

            s = s.replace(">\n", ">");
            s = s.replaceAll("(?s)\n( *)</span", "</span");
        }

        return s;
    }


    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
	/*
        geojsonToCsv(args[0], System.out, (args.length > 1)
                                   ? args[1]
                                   : null);
        if(true) return;
	*/


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


        geojsonToCsv(args[0], System.out, (args.length > 1)
                                   ? args[1]
                                   : null);
        //        convertCameras(args);
    }



}
