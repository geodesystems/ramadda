/**
   Copyright (c) 2008-2025 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
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

import java.util.regex.Pattern;
import java.text.StringCharacterIterator;

import java.util.regex.Pattern;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


/**
 */
@SuppressWarnings("unchecked")
public class JsonUtil {

    /** JSON MIME type */
    public static final String MIMETYPE = "application/json";

    /**  */
    public static final String GEOJSON_MIMETYPE = "application/geo+json";


    /**  */
    public static final String MAP_OPEN = "{";

    /**  */
    public static final String MAP_CLOSE = "}";

    /**  */
    public static final String LIST_OPEN = "[";

    /**  */
    public static final String LIST_CLOSE = "]";


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




    /*
      This method create a set of rows of date that are the result of joining the field values in a set of
      arrays (defined by arrayPaths) matching on the fields defined by keyList. If an array does not have a corresponding
      object for a particular key value then missing is used to fill in the value
      @param root the json root object
      @param arrayPaths the object paths to each array
      @param keyList List of fields to key on
      @param pattern If defined then replace the key value with the pattern/replace strings
      @param missing Missing value
    */
    public static List<List<String>> joinArrays(JSONObject root, List<String> arrayPaths,
						List<String> keyList,
						String pattern,
						String replace, String missing) {
	boolean hasPattern = Utils.stringDefined(pattern);
	List<List<String>> results = new ArrayList<List<String>>();
	List<Hashtable<String,JSONObject>> maps =
	    new ArrayList<Hashtable<String,JSONObject>>();
	Set<String> keys = new LinkedHashSet<String>();
	LinkedHashSet<String> fieldsMap = new LinkedHashSet<String>();
	List<List<String>> fieldsList = new ArrayList<List<String>>();
	for(String arrayPath: arrayPaths) {
	    JSONArray array =  readArray(root, arrayPath);
	    if(array==null) {
		continue;
	    }
	    Hashtable<String,JSONObject> map = new Hashtable<String,JSONObject>();
	    maps.add(map);
	    List<String> myFields = new ArrayList<String>();
	    fieldsList.add(myFields);
	    boolean addedFields = false;
	    for (int i = 0; i < array.length(); i++) {
		JSONObject item = array.getJSONObject(i);
		String keyValue = null;
		if(keyList.size()==0) {
		    keyValue = item.optString(keyList.get(0),null);
		} else {
		    StringBuilder sb = new StringBuilder();
		    for(String key: keyList) {
			if(item.has(key)) {
			    if(sb.length()>0) sb.append("_");
			    sb.append(item.optString(key,""));
			}
		    }
		    keyValue = sb.toString();
		}
		if(keyValue==null) {
		    continue;
		}
		if(hasPattern) {
		    keyValue = keyValue.replaceAll(pattern,replace);
		}		    
		keys.add(keyValue);
		map.put(keyValue,item);
		if(!addedFields) {
		    addedFields = true;
		    for(String field:Utils.unroll(item.keySet())) {
			if(!fieldsMap.contains(field)) {
			    myFields.add(field);
			    fieldsMap.add(field);
			}
		    }
		}
	    }
	}

	List<String> header = new ArrayList<String>();
	results.add(header);
	for(List<String> fields:fieldsList) {
	    header.addAll(fields);
	}

	for(String keyValue: Utils.unroll(keys)) {
	    List<String> row = new ArrayList<String>();
	    results.add(row);
	    for(int i=0;i<maps.size();i++) {
		Hashtable<String,JSONObject> map = maps.get(i);
		List<String> fields = fieldsList.get(i);
		JSONObject obj = map.get(keyValue);
		//Find the first row that has the key value
		if(obj==null) {
		    for(int j=0;j<maps.size() && obj==null;j++) {
			obj = maps.get(j).get(keyValue);			
		    }
		}		    

		if(obj==null) {
		    for(String field: fields) {
			row.add(missing);
		    }
		    continue;
		}
		for(String field: fields) {
		    String s = obj.optString(field,missing);
		    row.add(s);
		}
	    }
	}
	return results;
    }


    /**
     *   This quotes every other list value for showing in a map
     *
     * @param values _more_
     * @return _more_
     */
    public static List quoteList(List values) {
        List quoted = new ArrayList();
        for (int i = 0; i < values.size(); i += 2) {
            quoted.add(values.get(i));
            String value = values.get(i + 1).toString();
            quoted.add(quote(value));
        }

        return quoted;
    }

    public static List quoteTypeList(List values) {
        List quoted = new ArrayList();
        for (int i = 0; i < values.size(); i += 2) {
            quoted.add(values.get(i));
            String value = values.get(i + 1).toString();
            quoted.add(quoteType(value));
        }

        return quoted;
    }


    public static List quoteAll(List values) {
        List quoted = new ArrayList();
        for (int i = 0; i < values.size(); i ++) {
            String value = values.get(i).toString();
            quoted.add(quote(value));
        }
        return quoted;
    }    


    /**
     * _more_
     *
     * @param values _more_
     *
     * @return _more_
     */
    public static String mapAndQuote(List values) {
        return map(quoteList(values));
    }

    /**
     * Create a JSON map
     *
     * @param values  key/value pairs [ key1,value1,key2,value2 ]
     * @param quoteValue  true to quote the values
     *
     * @return  the map object { key1:value1, key2:value2 }
     */
    public static String map(List values) {
        StringBuffer row = new StringBuffer();
        map(row, values);
        return row.toString();
    }

    public static String map(Object... values) {
	return map(Utils.arrayToList(values));
    }


    /**
     * _more_
     *
     * @param row _more_
     * @param values _more_
     *
     * @return _more_
     */
    public static Appendable map(Appendable row, List values) {
        try {
            if (row == null) {
                row = new StringBuilder();
            }
            row.append(mapOpen());
            int cnt = 0;
            for (int i = 0; i < values.size(); i += 2) {
                String name  = values.get(i).toString();
                String value = values.get(i + 1).toString();
                if (value == null) {
                    continue;
                }
                if (cnt > 0) {
                    row.append(",\n");
                }
                cnt++;
                row.append(attr(name, value));
            }
            row.append(mapClose());
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

        return row;
    }

    /**
     *
     * @param values _more_
     *
     * @return _more_
     */
    public static String mapAndGuessType(List<String> values) {
        StringBuilder row = new StringBuilder();
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
            row.append(attrGuessType(name, value));
        }
        row.append(mapClose());

        return row.toString();
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
        return MAP_OPEN;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public static String mapClose() {
        return MAP_CLOSE;
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
        return LIST_OPEN;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public static String listClose() {
        return LIST_CLOSE;
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
     *
     * @param values _more_
     * @return _more_
     */
    public static List<String> quote(List values) {
        List result = new ArrayList<String>();
        for (Object o : values) {
            result.add(quote(o.toString()));
        }

        return result;
    }


    /**
     * _more_
     *
     * @param row _more_
     * @param values _more_
     * @param quoteValue _more_
     *
     * @return _more_
     */
    public static Appendable list(Appendable row, List values,
                                  boolean quoteValue) {
        try {
            if (row == null) {
                row = new StringBuilder();
            }
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
            row.append("\n");
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

        return row;
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
            arrayVals.add(mapAndQuote(mapValues));
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
     *
     * @param sb _more_
     * @param name _more_
     * @param value _more_
     *
     * @throws Exception _more_
     */
    public static void attr(Appendable sb, String name, String value) {
	try {
	    sb.append(mapKey(name));
	    sb.append(getString(value, false));
	} catch(Exception exc) {
	    throw new RuntimeException(exc);
	}
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


    public static String quoteType(Object v) {
	String s = v.toString().trim();
        if (s.equals("true") || s.equals("false")) {
	    return s;
        }
        if (Pattern.matches("^[+-]?([0-9]*[.])?[0-9]+$", s)) {
	    return s;
	}
	return quote(s);
    }

    /**
     *
     * @param name _more_
     * @param v _more_
     *
     * @return _more_
     */
    public static String attrGuessType(String name, String v) {
	return attr(name,quoteType(v));
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
     *
     * @param d _more_
     *
     * @return _more_
     */
    public static boolean isNullNumber(double d) {
        if (Double.isNaN(d)) {
            return true;
        }

        if ((d == Double.NEGATIVE_INFINITY)
	    || (d == Double.POSITIVE_INFINITY)) {
            return true;
        }

        return false;
    }


    /**
     * _more_
     *
     * @param d _more_
     *
     * @return _more_
     */
    public static String formatNumber(double d) {
        if (isNullNumber(d)) {
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
    public static String quote(Object s) {
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
    public static void quote(Appendable sb, Object o) {
        try {
            if (o == null) {
                sb.append(NULL);
                return;
            }
	    String s = o.toString();
            //      s = cleanString(s);
            //            s = s.replaceAll("\n", "\\n");
            //            s = s.replaceAll("\r", "\\r");
            //            s = s.replaceAll("\"", "\\\\\"");
            if (s.equals("true") || s.equals("false")) {
                sb.append(s);

                return;
            }
            sb.append(JSONWriter.valueToString(s));
            //      sb.append("\"");        sb.append(s);           sb.append("\"");
        } catch (Exception exc) {
            throw new IllegalArgumentException("Could not quote string:" + o);
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


    /*
      return the string denoted by path. Catch and ignore any errors
    */
    public static String readValue(String json, String path, String dflt) {
	try {
	    return readValue(new JSONObject(json),path, dflt);
	} catch(Throwable thr) {
	    return dflt;
	}
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
                obj = readObjectFromTok(obj,tok);
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
            if ( !obj.has(tok)) {
                return null;
            }
            obj = obj.getJSONObject(tok);
            if (obj == null) {
                return null;
            }
        }

        return obj;
    }

    public static JSONObject readObjectFromTok(JSONObject obj, String tok)  {
	if(StringUtil.containsRegExp(tok)) {
	    for(Iterator<String> keys=obj.keys();keys.hasNext();) {
		String key = keys.next();
		if(key.matches(tok)) {
		    return obj.getJSONObject(key);
		}
	    }
	} else {
	    return  obj.getJSONObject(tok);
	}
	return null;
    }

    public static List<String> getKeys(JSONObject obj)  {
	List<String> list = new ArrayList<String>();
	for(Iterator<String> keys=obj.keys();keys.hasNext();) {
	    list.add(keys.next());
	}
	return list;
    }


    public static JSONArray readArrayFromTok(JSONObject obj, String tok)  {
	if(StringUtil.containsRegExp(tok)) {
	    for(Iterator<String> keys=obj.keys();keys.hasNext();) {
		String key = keys.next();
		if(key.matches(tok)) {
		    return obj.getJSONArray(key);
		}
	    }
	} else {
	    return  obj.optJSONArray(tok);
	}
	return null;
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
            int idx1 = tok.indexOf("[");
            //is an array
            if (idx1 >= 0) {
                int       idx2      = tok.indexOf("]");
                String    arrayName = tok.substring(0, idx1);
                int aidx = Integer.parseInt(tok.substring(idx1 + 1, idx2));
                JSONArray a         = obj.getJSONArray(arrayName);
                obj = a.getJSONObject(aidx);
                continue;
            }
	    obj = readObjectFromTok(obj,tok);
            if (obj == null) {
                return null;
            }
        }
        return readArrayFromTok(obj,lastTok);
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
     *
     * @param json _more_
     * @param forHtml _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static String format(String json, boolean forHtml)
	throws Exception {
	String s;
	try {
	    JSONObject obj = new JSONObject(json.toString());
	    s = forHtml
		? obj.toString(1)
		: obj.toString(3);
	} catch(Exception exc) {
	    JSONArray obj = new JSONArray(json.toString());
	    s = forHtml
		? obj.toString(1)
		: obj.toString(3);
	}
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
	/* Dont' have this in the release as it pulls too many things in
        JSONObject obj     = new JSONObject(IO.readInputStream(new FileInputStream(args[0])));
	List<List<String>> results =
	    joinArrays(obj, Utils.split(args[1],",",true,true),
		       Utils.split(args[2],",",true,true),null,null,
		       "missing");
	for(int i=0;i<results.size();i++) {
	    System.out.print(Utils.columnsToString(results.get(i),",",true));
	}
	System.exit(0);
	**/
    }


    /**
     *
     * @param obj _more_
     * @param primitiveOnly _more_
     * @param arrayKeys _more_
     *
     * @return _more_
     */
    public static Hashtable getHashtable(Object obj, boolean primitiveOnly,
                                         List<String> arrayKeys) {
        if (obj instanceof JSONObject) {
            return getHashtableFromObject((JSONObject) obj, primitiveOnly,
                                          arrayKeys);
        } else if (obj instanceof JSONArray) {
            return getHashtableFromArray((JSONArray) obj, primitiveOnly,
                                         arrayKeys);
        }

        throw new IllegalArgumentException("Unknown object type");
    }


    /**
     *
     * @param obj _more_
     * @param primitiveOnly _more_
     * @param arrayKeys _more_
     *
     * @return _more_
     */
    public static Hashtable getHashtableFromObject(JSONObject obj,
						   boolean primitiveOnly, List<String> arrayKeys) {
        Hashtable hashtable = new Hashtable();
        JSONArray names     = obj.names();
        for (int i = 0; i < names.length(); i++) {
            String name  = (String) names.get(i);
            Object value = obj.get(name);
            if (primitiveOnly
		&& ((value instanceof JSONObject)
		    || (value instanceof JSONArray))) {
                continue;
            }
            hashtable.put(name, value);
        }

        return hashtable;
    }


    /**
     *
     * @param obj _more_
     * @param primitiveOnly _more_
     * @param arrayKeys _more_
     *
     * @return _more_
     */
    public static Hashtable getHashtableFromArray(JSONArray obj,
						  boolean primitiveOnly, List<String> arrayKeys) {
        Hashtable hashtable = new Hashtable();
        for (int i = 0; i < obj.length(); i++) {
            Object value = obj.get(i);
            if (primitiveOnly
		&& ((value instanceof JSONObject)
		    || (value instanceof JSONArray))) {
                continue;
            }
            String index = "Index " + i;
            if (arrayKeys != null) {
                arrayKeys.add(index);
            }
            hashtable.put(index, value);
        }

        return hashtable;
    }

    /**
       Make a List from the array. For now this expects strings in the array
    */
    public static List<String> getList(JSONArray array) {
	List<String> result  = new ArrayList<String>();
	for(int i=0;i<array.length();i++) {
	    result.add(array.getString(i));
	}
	return result;
    }

    public static String optString(JSONObject obj,String key) {
	if(obj==null) return null;
	return obj.optString(key,null);
    }


    public static List<String> getStrings(JSONArray array) {
	List<String> s = new ArrayList<String>();
	if(array!=null) {
	    for (int i = 0; i < array.length(); i++) {
		s.add(array.getString(i));
	    }
	}
	return s;
    }


    public static String getString(JSONArray array,int idx) {
	if(array==null) return null;
	Object obj = array.get(idx);
	return obj.toString();
    }


}
