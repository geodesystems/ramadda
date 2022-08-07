/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util.geo;


import org.json.*;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;
import org.ramadda.util.Utils;


import org.ramadda.util.geo.Bounds;

import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.net.URL;

import java.util.ArrayList;

import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;


/**
 * A set of utility methods for dealing with geographic things
 *
 * @author  Jeff McWhirter
 */
@SuppressWarnings("unchecked")
public class GeoUtils {

    /**
     *  semimajor axis of Earth WGS 84 (m)
     */
    public static final double WGS84_A = 6378137.0;

    /**
     *  semimajor axis of Earth WGS 84 (m) squared
     */

    public static final double WGS84_A_2 = WGS84_A * WGS84_A;

    /**
     *  semiminor axis of Earth WGS 84 (m)
     */
    public static final double WGS84_B = 6356752.3142451793;

    /**
     *  semiminor axis of Earth WGS 84 (m) squared
     */
    public static final double WGS84_B_2 = WGS84_B * WGS84_B;

    /** _more_ */
    public static final double WGS84_E_2 = (WGS84_A_2 - WGS84_B_2)
                                           / WGS84_A_2;

    /** _more_ */
    public static final double DEG2RAD = Math.PI / 180.0;


    /** _more_ */
    private static long GPS_TIME_OFFSET = 0;

    /** _more_ */
    public static final Calendar GPS_DATE = new GregorianCalendar(1980, 0, 6);

    /** _more_ */
    public static final int MS_PER_DAY = 1000 * 60 * 60 * 24;

    /** _more_ */
    private static String googleKey;

    /** _more_ */
    private static String geocodeioKey;

    /** _more_ */
    private static String hereKey;


    /** _more_ */
    private static File cacheDir;

    /** _more_ */
    private static PrintWriter cacheWriter;

    /** _more_ */
    private static String cacheDelimiter = "_delim_";

    /**  */
    private static boolean haveInitedKeys = false;

    /** _more_ */
    private static Hashtable statesMap;

    /** _more_ */
    private static Hashtable<String, Place> citiesMap;

    /** _more_ */
    private static final String[] citySuffixes = new String[] { "city",
            "town", "cdp", "village" };

    /** _more_ */
    private static final String[] countySuffixes = new String[] {
        "county", "city", "borough", "municipality", "parish", "census area",
        "city and borough"
    };


    /** _more_ */
    private static HashSet noPlaceSet = new HashSet();



    /**
     */
    private static void initKeys() {
        if (haveInitedKeys) {
            return;
        }
        if (googleKey == null) {
            setGoogleKey(System.getenv("GOOGLE_API_KEY"));
        }
        if (geocodeioKey == null) {
            setGeocodeioKey(System.getenv("GEOCODEIO_API_KEY"));
        }
        if (hereKey == null) {
            setHereKey(System.getenv("HERE_API_KEY"));
        }
        haveInitedKeys = true;
    }


    /**
     * _more_
     *
     * @param key _more_
     */
    public static void setGoogleKey(String key) {
        googleKey = key;
    }

    /**
     *
     * @param key _more_
     */
    public static void setHereKey(String key) {
        hereKey = key;
    }

    /**
      * @return _more_
     */
    public static String getHereKey() {
        return hereKey;
    }

    public static String getGoogleKey() {
        return googleKey;
    }    

    /**
     * _more_
     *
     * @param key _more_
     */
    public static void setGeocodeioKey(String key) {
        geocodeioKey = key;
    }

    /**
     * _more_
     *
     * @param file _more_
     */
    public static void setCacheDir(File file) {
        cacheDir = file;
    }

    /**
     * _more_
     *
     * @param date _more_
     *
     * @return _more_
     */
    public static String date2GPS(Calendar date) {
        long elapsedTime = date.getTimeInMillis()
                           - GPS_DATE.getTimeInMillis();
        double elapsedDays = elapsedTime / MS_PER_DAY;
        double fullDays    = Math.floor(elapsedDays);
        /*
          double partialDays = elapsedDays - fullDays;
          double hours = partialDays * 24;
        */

        return Double.toString(fullDays);
    }

    /**
     * _more_
     *
     * @param gpsDays _more_
     *
     * @return _more_
     */
    public static Calendar gps2Date(int gpsDays) {
        Calendar returnDate = (Calendar) GPS_DATE.clone();
        returnDate.add(Calendar.DATE, gpsDays);

        return returnDate;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public static long getGpsTimeOffset() {
        if (GPS_TIME_OFFSET == 0) {
            GregorianCalendar cal =
                new GregorianCalendar(TimeZone.getTimeZone("UTC"));
            cal.set(1980, 1, 6, 0, 0, 0);
            GPS_TIME_OFFSET = cal.getTimeInMillis();
        }

        return GPS_TIME_OFFSET;
    }

    /**
     * _more_
     *
     * @param gpsTime _more_
     *
     * @return _more_
     */
    public static long convertGpsTime(long gpsTime) {
        return getGpsTimeOffset() + gpsTime;
    }

    /**
     * Taken from the C WGS84_xyz_to_geo  in postion.c
     *
     * @param x _more_
     * @param y _more_
     * @param z _more_
     *
     * @return _more_
     */
    public static double[] wgs84XYZToLatLonAlt(double x, double y, double z) {
        return wgs84XYZToLatLonAlt(x, y, z, null);
    }


    /**
     * Taken from the C WGS84_xyz_to_geo  in postion.c
     *
     * @param x _more_
     * @param y _more_
     * @param z _more_
     * @param result _more_
     *
     * @return _more_
     */
    public static double[] wgs84XYZToLatLonAlt(double x, double y, double z,
            double[] result) {
        double lat, lon, alt;
        double cos_lat, sin_lat, last_lat, p, N;
        double r = Math.sqrt(x * x + y * y + z * z);

        if (r != 0) {
            lat = Math.asin(z / r);
        } else {
            lat = 0;
        }
        lon = Math.atan2(y, x);

        /* this accounts for the shape of the WGS 84 ellipsoid and a height h above (or below) it
           note: the initial latitude correction is based on an empirical approximation, which is
           good to several thousandths of a minute in latitude for most elevations; each iteration
           improves the precision by about a factor of 1000, requiring about 5 iterations to converge */

        lat += 3.35842e-3 * Math.sin(2. * lat) + 5.82e-6 * Math.sin(4. * lat);
        p   = Math.sqrt(x * x + y * y);
        int loop = 0;
        do {
            last_lat = lat;
            cos_lat  = Math.cos(lat);
            sin_lat  = Math.sin(lat);
            N = WGS84_A_2
                / Math.sqrt(WGS84_A_2 * cos_lat * cos_lat
                            + WGS84_B_2 * sin_lat * sin_lat);
            if (cos_lat != 0) {
                alt = p / cos_lat - N;
            } else {
                alt = z - N * WGS84_B_2 / WGS84_A_2;
            }
            if (p != 0) {
                lat = Math.atan(z / (p * (1. - WGS84_E_2 * (N / (N + alt)))));
            } else {
                lat = (z < 0.)
                      ? -Math.PI / 2.
                      : Math.PI / 2.;
            }
        } while ((last_lat - lat) != 0 && (loop++ < 10));


        if (result == null) {
            result = new double[3];
        }
        result[0] = Math.toDegrees(lat);
        result[1] = Math.toDegrees(lon);
        result[2] = alt;

        return result;
    }

    /**
     * Normalize the longitude to lie between +/-180
     * @param lon east latitude in degrees
     * @return normalized lon
     */
    static public double normalizeLongitude(double lon) {
        if ((lon < -180.) || (lon > 180.)) {
            return Math.IEEEremainder(lon, 360.0);
        } else {
            return lon;
        }
    }

    /**
     * Normalize the longitude to lie between 0 and 360
     * @param lon east latitude in degrees
     * @return normalized lon
     */
    static public double normalizeLongitude360(double lon) {
        while ((lon < 0.) || (lon > 361.)) {
            lon = 180. + Math.IEEEremainder(lon - 180., 360.0);
        }

        return lon;
    }







    /**
     *
     * @param lat _more_
     * @param lon _more_
     *  @return _more_
     *
     * @throws Exception _more_
     */
    public static Address getAddressFromLatLon(double lat, double lon)
            throws Exception {
        initKeys();
        Address address = null;
        if ((address == null) && (hereKey != null)) {
            address = getAddressFromLatLonHere(lat, lon);
        }
        if ((address == null) && (geocodeioKey != null)) {
            address = getAddressFromLatLonGeocodeio(lat, lon);
        }

        return address;
    }


    /**
     *
     * @param lat _more_
     * @param lon _more_
     *  @return _more_
     *
     * @throws Exception _more_
     */
    private static Address getAddressFromLatLonGeocodeio(double lat,
            double lon)
            throws Exception {
        /*
          https://www.geocod.io/docs/#reverse-geocoding
          {
          "results": [
          {
          "address_components": {
          "number": "508",
          "street": "H",
          "suffix": "St",
          "postdirectional": "NE",
          "formatted_street": "H St NE",
          "city": "Washington",
          "county": "District of Columbia",
          "state": "DC",
          "zip": "20002",
          "country": "US"
          },*/

        String url = HtmlUtils.url("https://api.geocod.io/v1.7/reverse", "q",
                                   lat + "," + lon, "api_key", geocodeioKey);
        //      System.err.println(url);
        String json = IO.readContents(url, GeoUtils.class);
        //      System.err.println(json);
        JSONObject obj = new JSONObject(json);
        if ( !obj.has("results")) {
            System.err.println("No results");

            return null;
        }
        JSONArray  results    = obj.getJSONArray("results");
        JSONObject result     = results.getJSONObject(0);
        JSONObject components = result.getJSONObject("address_components");
        if ( !components.has("number")) {
            System.err.println("No results");

            return null;
        }

        String address = components.getString("number") + " "
                         + components.getString("street") + " "
                         + components.getString("suffix");

        return new Address(address.trim(), components.getString("city"),
                           components.getString("county"),
                           components.getString("state"),
                           components.getString("zip"),
                           components.getString("country"));

    }

    /**
     *
     * @param lat _more_
     * @param lon _more_
     *  @return _more_
     *
     * @throws Exception _more_
     */
    private static Address getAddressFromLatLonHere(double lat, double lon)
            throws Exception {
        /*
          https://developer.here.com/develop/rest-apis
          https://revgeocode.search.hereapi.com/v1/revgeocode?at=52.5228,13.4124
          Authorization: Bearer [your token]
          {
          "items": [
          {
          "title": "5 Rue Daunou, 75002 Paris, France",
          "id": "here:af:streetsection:z42doZW8EyzEiPcuOd5MXB:CggIBCCi-9SPARABGgE1KGQ",
          "resultType": "houseNumber",
          "houseNumberType": "PA",
          "address": {
          "label": "5 Rue Daunou, 75002 Paris, France",
          "countryCode": "FRA",
          "countryName": "France",
          "state": "Île-de-France",
          "county": "Paris",
          "city": "Paris",
          "district": "2e Arrondissement",
          "street": "Rue Daunou",
          "postalCode": "75002",
          "houseNumber": "5"
          },
        */
        String url =
            HtmlUtils.url(
                "https://revgeocode.search.hereapi.com/v1/revgeocode", "at",
                lat + "," + lon, "apiKey", hereKey);
        //      System.err.println(url);
        String json = IO.doGet(new URL(url));
        //      System.err.println(json);
        JSONObject obj = new JSONObject(json);
        if ( !obj.has("items")) {
            System.err.println("No items");

            return null;
        }
        JSONArray items = obj.getJSONArray("items");
        if (items.length() == 0) {
            System.err.println("No items");

            return null;
        }
        JSONObject item = items.getJSONObject(0);
        if ( !item.has("address")) {
            System.err.println("No address");

            return null;
        }
        JSONObject address = item.getJSONObject("address");

        String a = address.getString("houseNumber") + " "
                   + address.getString("street");

        return new Address(a.trim(), address.getString("city"),
                           address.getString("county"),
                           address.getString("state"),
                           address.getString("postalCode"),
                           address.getString("countryName"));

    }




    /** _more_ */
    private static Hashtable<String, Place> addressToLocation = null;

    /** _more_ */
    private static Hashtable<String, String> hoods = null;

    /** _more_ */
    private static PrintWriter hoodsWriter;


    /**
     * _more_
     *
     * @param address _more_
     * @param bounds _more_
     *
     * @return _more_
     */
    public static Place getLocationFromAddress(String address,
            Bounds bounds) {
        try {
            Place place = getLocationFromAddressInner(address, bounds, false);
            //      System.err.println("PLACE:"  + place);
            if (place != null) {
                if (Double.isNaN(place.getLatitude())) {
                    return null;
                }
                if ( !place.within(bounds)) {
                    return null;
                }
            }

            return place;
        } catch (Exception exc) {
            exc.printStackTrace();

            throw new RuntimeException(exc);
        }
    }




    /**
     * _more_
     *
     * @param path _more_
     * @param lat _more_
     * @param lon _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static Feature findFeature(String path, double lat, double lon)
            throws Exception {
        FeatureCollection fc = FeatureCollection.getFeatureCollection(path,
                                   null);
        if (fc == null) {
            String _path = path.toLowerCase();
            if (path.equals("counties")) {
                path  = "/org/ramadda/util/geo/resources/counties.zip";
                _path = path;
            } else if (path.equals("states")) {
                path  = "/org/ramadda/util/geo/resources/states.zip";
                _path = path;
            } else if (path.equals("timezones")) {
                path  = "/org/ramadda/util/geo/resources/timezones.zip";
                _path = path;
            }
            if ( !_path.endsWith(".zip") && !_path.startsWith("/")
                    && !_path.endsWith("json")) {
                path = "/org/ramadda/util/geo/resources/" + path + ".zip";
            }
            fc = FeatureCollection.getFeatureCollection(path,
                    IO.getInputStream(path));
        }

        return fc.find((float) lat, (float) lon);
    }

    /**
     * _more_
     *
     * @param path _more_
     * @param field _more_
     * @param lat _more_
     * @param lon _more_
     * @param dflt _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static Object findFeatureField(String path, String field,
                                          double lat, double lon, Object dflt)
            throws Exception {
        Feature feature = findFeature(path, lat, lon);
        if (feature != null) {
            Hashtable data = feature.getData();
            if (data != null) {
                return data.get(field);
            }
        }

        return dflt;
    }

    /**
     *
     * @param path _more_
     * @param fields _more_
     * @param lat _more_
     * @param lon _more_
     *  @return _more_
     *
     * @throws Exception _more_
     */
    public static List<Object> findFeatureFields(String path,
            List<String> fields, double lat, double lon)
            throws Exception {
        if (fields.size() == 0) {
            String       v  = findFeatureName(path, lat, lon, "");
            List<Object> vs = new ArrayList<Object>();
            vs.add(v);

            return vs;
        }

        Feature feature = findFeature(path, lat, lon);
        if (feature != null) {
            Hashtable data = feature.getData();
            if (data != null) {
                List<Object> vs = new ArrayList<Object>();
                for (String field : fields) {
                    Object o = data.get(field);
                    if (o == null) {
                        o = data.get(field.toUpperCase());
                    }
                    vs.add(o);
                }

                return vs;
            }
        }

        return null;
    }

    /** _more_ */
    private static final String[] NAME_FIELDS = new String[] { "name",
            "state_name", "cntry_name", "tzid" };

    /**
     * _more_
     *
     * @param path _more_
     * @param lat _more_
     * @param lon _more_
     * @param dflt _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static String findFeatureName(String path, double lat, double lon,
                                         String dflt)
            throws Exception {
        Feature feature = findFeature(path, lat, lon);
        if (feature != null) {
            Hashtable data = feature.getData();
            //      System.err.println(data);
            if (data != null) {
                for (String f : NAME_FIELDS) {
                    String name = (String) data.get(f);
                    if (name != null) {
                        return name;
                    }
                }
            }
        }

        return dflt;
    }



    /**
     * _more_
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static Hashtable getStatesMap() throws Exception {
        if (statesMap == null) {
            InputStream inputStream =
                IO.getInputStream("/org/ramadda/util/states.properties",
                                  GeoUtils.class);
            String    s    = IOUtil.readContents(inputStream);
            Hashtable tmp  = Utils.getProperties(s);
            Hashtable tmp2 = new Hashtable();
            IOUtil.close(inputStream);
            for (Enumeration keys = tmp.keys(); keys.hasMoreElements(); ) {
                String key = (String) keys.nextElement();
                tmp2.put(key.toLowerCase(), tmp.get(key));
            }
            tmp.putAll(tmp2);
            statesMap = tmp;
        }

        return statesMap;
    }


    /**
     *
     * @param address _more_
     * @param bounds _more_
     * @param debug _more_
     *  @return _more_
     *
     * @throws Exception _more_
     */
    private static Place getLocationFromAddressInner(String address,
            Bounds bounds, boolean debug)
            throws Exception {

        //      debug = true;
        if ( !Utils.stringDefined(address)) {
            return null;
        }
        address = address.trim();
        String _address = address.toLowerCase();

        GeoResource resource = null;
	GeoResource resource2 = null;



        if (_address.startsWith("from:")) {
            address  = address.substring(5);
            _address = _address.substring(5);
        } else if (_address.startsWith("to:")) {
            address  = address.substring(3);
            _address = _address.substring(3);
        }
        boolean doCountry = false;
        if (_address.startsWith("country:")) {
            address   = address.substring("country:".length()).trim();
            _address  = _address.substring("country:".length()).trim();
            doCountry = true;
        }
        boolean doState = false;
        if (_address.startsWith("state:")) {
            address  = address.substring("state:".length()).trim();
            _address = _address.substring("state:".length()).trim();
            doState  = true;
        }

        boolean doZip = false;
        boolean doZcta = false;
        if (_address.startsWith("zip:")) {
            address  = address.substring("zip:".length()).trim();
            _address = _address.substring("zip:".length()).trim();
            if (address.length() > 5) {
                address = address.substring(0, 5);
            }
            doZip = true;
        }
        if (_address.startsWith("zcta:")) {
            address  = address.substring("zcta:".length()).trim();
            _address = _address.substring("zcta:".length()).trim();
            if (address.length() > 5) {
                address = address.substring(0, 5);
            }
            doZcta = true;
        }
        if (_address.startsWith("congress:")) {
            address  = address.substring("congress:".length()).trim();
            _address = _address.substring("congress:".length()).trim();
            resource = GeoResource.RESOURCE_CONGRESS;
        }


        boolean doCounty = false;
        if (_address.startsWith("county:")) {
            address  = address.substring("county:".length()).trim();
            _address = _address.substring("county:".length()).trim();
            doCounty = true;
        }

        boolean doCity = false;
        if (_address.startsWith("city:")) {
            address  = address.substring("city:".length()).trim();
            _address = _address.substring("city:".length()).trim();
            doCity   = true;
            //For when there is no city, just a state
            if (address.startsWith(",")) {
                doState  = true;
                doCity   = false;
                address  = address.substring(1).trim();
                _address = _address.substring(1).trim();
            }
        }
        if (address.length() == 0) {
            return null;
        }

        if (debug) {
            System.err.println("address:" + address);
        }

        Place       place    = null;

        if (doZip) {
            resource = GeoResource.RESOURCE_ZIPCODES;
            resource2 = GeoResource.RESOURCE_ZCTA;
        } else  if (doZcta) {
            resource = GeoResource.RESOURCE_ZCTA;
            resource2 = GeoResource.RESOURCE_ZIPCODES;
        }

        if (doCity) {
            //abbrev to name
            getStatesMap();
            if (citiesMap == null) {
                citiesMap = new Hashtable<String, Place>();
                List<Place> places = GeoResource.RESOURCE_PLACES.getPlaces();
                for (Place place2 : places) {
                    String abbrev   = place2.getSuffix().toLowerCase();
                    String longName = (String) statesMap.get(abbrev);
                    String cityName = place2.getName().toLowerCase();
                    //              System.out.println("prop:" +cityName + "," + abbrev);
                    citiesMap.put(cityName + "," + abbrev, place2);
                    if (longName != null) {
                        citiesMap.put(cityName + ","
                                      + longName.toLowerCase(), place2);
                        //                      System.out.println(cityName+"," + longName.toLowerCase());
                    }
                    //              System.out.println("prop:" + cityName+"," + abbrev);
                }
            }

            try {
                List<String> toks  = StringUtil.splitUpTo(address, ",", 2);
                String       city  = toks.get(0).toLowerCase().trim();
                String       state = ((toks.size() > 1)
                                      ? toks.get(1)
                                      : null);
                //The resource file has certain cities with state=** for big cities
                if (state == null) {
                    state = "**";
                }
                Place place2 = null;
                if (state != null) {
                    List<String> cityToks = StringUtil.split(city, "-", true,
                                                true);
                    cityToks.add(0, city);
                    state = state.toLowerCase();
                    //              state = state.replace("metropolitan division","");
                    //              state = state.replace("metropolitan division","");
                    state = state.replaceAll("\\.", "");
                    List<String> tmp = StringUtil.split(state, "-", true,
                                           true);
                    //              state = tmp.get(0);
                    tmp.add((String) statesMap.get(state));
                    tmp.add("**");
                    for (String st : tmp) {
                        if (st == null) {
                            continue;
                        }
                        for (String cityTok : cityToks) {
                            place2 = citiesMap.get(cityTok + "," + st);
                            if (place2 != null) {
                                return place2;
                            }
                            for (String suffix : citySuffixes) {
                                //                          System.out.println(city + suffix+"," + st);
                                place2 = citiesMap.get(cityTok + " " + suffix
                                        + "," + st);
                                if (place2 != null) {
                                    return place2;
                                }
                            }
                        }
                    }
                }

                if (state != null) {
                    place = GeoResource.RESOURCE_CITIES.getPlace(city + ","
                            + state);
                    if (place != null) {
                        return place;
                    }
                }
            } catch (Exception exc) {
                exc.printStackTrace();
            }

            //If the city fails then do the county
            doCounty = true;
        }

        if (doCounty) {
            resource = GeoResource.RESOURCE_COUNTIES;
            int index = _address.indexOf(",");
            if (index < 0) {
                return resource.getPlace(_address);
            }
            getStatesMap();
            List<String> toks   = StringUtil.split(_address, ",");
            String       county = toks.get(0).trim();
            String       state  = toks.get(1).trim();
            if (place != null) {
                return place;
            }
            if (debug) {
                System.out.println("trying:" + county + "," + state);
            }
            place = resource.getPlace(county + "," + state);
            if (place != null) {
                return place;
            }
            state = (String) statesMap.get(state);
            if (debug) {
                System.out.println("state after:" + county + "," + state);
            }
            if (state != null) {
                if (debug) {
                    System.out.println("trying:" + county + "," + state);
                }
                place = resource.getPlace(county + "," + state);
                if (place != null) {
                    return place;
                }
                if (debug) {
                    //                    System.out.println("try 2:" + county + "," + state  + " place:" + place);
                }
                for (String suffix : countySuffixes) {
                    if (debug) {
                        //System.out.println("suffix: " + county + " " + suffix + "," + state);
                    }
                    place = resource.getPlace(county + " " + suffix + ","
                            + state);
                    if (place != null) {
                        return place;
                    }
                }
                //                return place;
            }
            doState = true;
        }


        if (doState) {
            resource = GeoResource.RESOURCE_STATES;
            place    = resource.getPlace(address);
            if (place != null) {
                return place;
            }
            doCountry = true;
        }


        if (doCountry) {
            resource = GeoResource.RESOURCE_COUNTRIES;
            place    = resource.getPlace(address);
            if (place != null) {
                return place;
            }
            //            return place;
        }

        if (resource != null) {
            place = resource.getPlace(address);
	    //	    System.err.println("PLACE:" + place);
	    if(place==null && resource2!=null) {
		place = resource2.getPlace(address);		
	    }
            if (place == null) {
                if ( !noPlaceSet.contains(address)) {
                    noPlaceSet.add(address);
		    System.out.println("no place:" + address);
                }
	    }
	    //If we get here and there is no place then for now lets return null
	    //since some resource collection was selected
	    if(true)
		return place;
            if (place != null) {
                return place;
            }
        }


        if (place != null) {
            return place;
        }


        initKeys();
        //geocodeioKey = null;
        //hereKey = null;
        //googleKey = null;       
        if (addressToLocation == null) {
            addressToLocation = new Hashtable<String, Place>();
            if (cacheDir != null) {
                File cacheFile = new File(IOUtil.joinDir(cacheDir,
                                     "addresslocations2.txt"));
                if (cacheFile.exists()) {
                    for (String line :
                            StringUtil.split(IO.readContents(cacheFile),
                                             "\n", true, true)) {
                        List<String> toks = StringUtil.split(line,
                                                cacheDelimiter);
                        if (toks.size() == 4) {
                            addressToLocation.put(toks.get(0),
                                    new Place(toks.get(1),
                                        Double.parseDouble(toks.get(2)),
                                        Double.parseDouble(toks.get(3))));
                        }
                    }
                }
                FileWriter     fw = new FileWriter(cacheFile, true);
                BufferedWriter bw = new BufferedWriter(fw);
                cacheWriter = new PrintWriter(bw);
            }
        }

        if (addressToLocation != null) {
            place = addressToLocation.get(address);
            if (place != null) {
                if (debug) {
                    System.out.println("found in cached address list");
                }

                return place;
            }
        }

        if ((address.length() == 0) || address.equals(",")) {
            return null;
        }
        //        System.err.println("looking for address:" + address);

        String latString      = null;
        String lonString      = null;
        String encodedAddress = StringUtil.replace(address, " ", "%20");
        String name           = address;
        place = null;

        if ((place == null) && (googleKey != null)) {
            String result = null;
            try {
                //https://maps.googleapis.com/maps/api/geocode/json?address=Winnetka&bounds=34.172684,118.604794|34.236144,-118.500938&key=YOUR_API_KEY
                String url =
                    "https://maps.googleapis.com/maps/api/geocode/json?address="
                    + encodedAddress + "&key=" + googleKey;
                if (bounds != null) {
                    url += "&bounds=" + bounds.getSouth() + ","
                           + bounds.getWest() + "|" + bounds.getNorth() + ","
                           + bounds.getEast();
                }
                result = IO.readContents(url, GeoUtils.class);

                name = StringUtil.findPattern(result,
                        "\"formatted_address\"\\s*:\\s*\"([^\"]+)\"");
                if (name == null) {
                    name = address;
                }
                latString = StringUtil.findPattern(result,
                        "\"lat\"\\s*:\\s*([-\\d\\.]+),");
                lonString = StringUtil.findPattern(result,
                        "\"lng\"\\s*:\\s*([-\\d\\.]+)\\s*");
                if ((latString != null) && (lonString != null)) {
                    place = new Place(name, latString, lonString);
                }
                if (place == null) {
                    JSONObject json = new JSONObject(result);
                    System.err.println("google error:"
                                       + json.getString("error_message"));
                }

            } catch (Exception exc) {
                System.err.println("exc:" + exc);
            }
            if (debug) {
                System.err.println("google:" + place);
            }
        }

        if ((place != null) && !place.within(bounds)) {
            place = null;
        }
        if ((place == null) && (geocodeioKey != null)) {
            String url = "https://api.geocod.io/v1.6/geocode?";
            url += HtmlUtils.arg("q", address, true);
            url += "&";
            url += HtmlUtils.arg("api_key", geocodeioKey, true);
            String result = IO.readContents(url, GeoUtils.class);
            //"lat":39.988424,"lng":-105.226083
            latString = StringUtil.findPattern(result,
                    "\"lat\"\\s*:\\s*([-\\d\\.]+),");
            lonString = StringUtil.findPattern(result,
                    "\"lng\"\\s*:\\s*([-\\d\\.]+)\\s*");
            if ((latString != null) && (lonString != null)) {
                place = new Place(name, latString, lonString);
            }

            if (debug) {
                System.err.println("geocodeio:" + place);
            }
        }

        if ((place != null) && !place.within(bounds)) {
            place = null;
        }
        if ((place == null) && (hereKey != null)) {
            String url = HtmlUtils.url(
                             "https://geocode.search.hereapi.com/v1/geocode",
                             "q", encodedAddress, "apiKey", hereKey);
            String result = IO.doGet(new URL(url));
            latString = StringUtil.findPattern(result,
                    "\"lat\"\\s*:\\s*([-\\d\\.]+),");
            lonString = StringUtil.findPattern(result,
                    "\"lng\"\\s*:\\s*([-\\d\\.]+)\\s*");
            if ((latString != null) && (lonString != null)) {
                place = new Place(name, latString, lonString);
            }
            if (debug) {
                System.err.println("here:" + place);
            }
        }


        if ((place != null) && !place.within(bounds)) {
            place = null;
        }
        if (place == null) {
            //fall back to us census
            String url =
                "https://geocoding.geo.census.gov/geocoder/locations/onelineaddress?format=json&benchmark=2020&address="
                + encodedAddress;
            String result = IO.readContents(url, GeoUtils.class);
            latString = StringUtil.findPattern(result,
                    "\"y\"\\s*:\\s*([-\\d\\.]+)");
            lonString = StringUtil.findPattern(result,
                    "\"x\"\\s*:\\s*([-\\d\\.]+)\\s*");
            if ((latString != null) && (lonString != null)) {
                place = new Place(name, latString, lonString);
            }
            if (debug) {
                System.err.println("census:" + place);
            }
        }

        if (place != null) {
            if (addressToLocation != null) {
                addressToLocation.put(address, place);
            }
            if (cacheWriter != null) {
                synchronized (cacheWriter) {
                    cacheWriter.println(address + cacheDelimiter
                                        + place.getName() + cacheDelimiter
                                        + place.getLatitude()
                                        + cacheDelimiter
                                        + place.getLongitude());
                    cacheWriter.flush();
                }
            }

            return place;
        } else {
            if (cacheWriter != null) {
                synchronized (cacheWriter) {
                    //                cacheWriter.println(address + cacheDelimiter + Double.NaN + cacheDelimiter + Double.NaN);
                    //                cacheWriter.flush();
                }
            }
        }

        return null;


    }







    /** _more_ */
    private static String preciselyToken;

    /**
     * _more_
     *
     * @param force _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static String getPreciselyToken(boolean force) throws Exception {
        if (force) {
            preciselyToken = null;
        }
        if (preciselyToken != null) {
            return preciselyToken;
        }
        String key = System.getenv("PRECISELY_API_KEY");
        if (key == null) {
            throw new RuntimeException(
                "No PRECISELY_API_KEY environment variable set");
        }
        String b64 = Utils.encodeBase64(key.trim());
        //      System.err.println("key:" + key);
        //      System.err.println("b64:" + b64);
        String json =
            IO.doPost(new URL("https://api.precisely.com/oauth/token"),
                      "grant_type=client_credetnials", "Authorization",
                      "Basic " + b64, "Content-Type",
                      "application/x-www-form-urlencoded");
        //      System.err.println(json);
        JSONObject obj = new JSONObject(json);
        preciselyToken = obj.optString("access_token", null);

        return preciselyToken;
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private static Hashtable<String, String> getHoods() throws Exception {
        if ((hoods == null) && (cacheDir != null)) {
            File cacheFile = new File(IOUtil.joinDir(cacheDir,
                                 "neighborhoods.txt"));
            if (cacheFile.exists()) {
                hoods = new Hashtable<String, String>();
                //              System.err.println("Reading neighborhoods.txt");
                for (String line :
                        StringUtil.split(IO.readContents(cacheFile), "\n",
                                         true, true)) {
                    List<String> toks = StringUtil.split(line,
                                            cacheDelimiter);
                    hoods.put(toks.get(0), toks.get(1));
                }
            }
            FileWriter     fw = new FileWriter(cacheFile, true);
            BufferedWriter bw = new BufferedWriter(fw);
            hoodsWriter = new PrintWriter(bw);
        }
        if (hoods == null) {
            return null;
        }

        return hoods;
    }

    /**
     * _more_
     *
     * @param lat _more_
     * @param lon _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static String getNeighborhood(double lat, double lon)
            throws Exception {
        String                    key   = lat + "_" + lon;
        Hashtable<String, String> hoods = getHoods();
        if (hoods != null) {
            String hood = hoods.get(key);
            if (hood != null) {
                //              System.err.println("got from cache:" + hood);
                return hood;
            }
        }


        String token = getPreciselyToken(false);
        if (token == null) {
            throw new RuntimeException(
                "Unable to authenticate with precisely");
        }
        URL url =
            new URL(
                "https://api.precisely.com/neighborhoods/v1/place/bylocation?latitude="
                + lat + "&longitude=" + lon);
        String json;
        try {
            json = IO.doGet(url, "Authorization", " Bearer " + token,
                            "Accept", "application/json");
        } catch (Exception exc) {
            if (true) {
                throw exc;
            }
            //Try again to get a new token
            token = getPreciselyToken(true);
            if (token == null) {
                throw new RuntimeException(
                    "Unable to authenticate with precisely");
            }
            json = IO.doGet(url, "Authorization", " Bearer " + token,
                            "Accept", "application/json");
        }
        //      System.err.println(json);
        JSONObject obj = new JSONObject(json);
        if ( !obj.has("location")) {
            System.err.println("No location array found:" + json);

            return null;
        }
        JSONArray a = obj.getJSONArray("location");
        if (a.length() == 0) {
            System.err.println("No location object found:" + json);

            return null;
        }
        obj = a.getJSONObject(0);
        JSONObject place      = obj.getJSONObject("place");
        JSONArray  nameArray  = place.getJSONArray("name");
        JSONObject nameObject = nameArray.getJSONObject(0);
        String     hood       = nameObject.getString("value");
        if (hoodsWriter != null) {
            hoodsWriter.println(key + cacheDelimiter + hood);
            hoodsWriter.flush();
        }

        return hood;
    }


    /**
     * _more_
     *
     * @param pts _more_
     *
     * @return _more_
     */
    public static double[] getBounds(double[] pts) {
        double north = Double.NaN,
               south = Double.NaN,
               east  = Double.NaN,
               west  = Double.NaN;
        for (int i = 0; i < pts.length - 1; i += 2) {
            double lon = pts[i];
            double lat = pts[i + 1];
            north = (i == 0)
                    ? lat
                    : Math.max(north, lat);
            south = (i == 0)
                    ? lat
                    : Math.min(north, lat);
            west  = (i == 0)
                    ? lon
                    : Math.min(west, lon);
            east  = (i == 0)
                    ? lon
                    : Math.max(east, lon);
        }

        return new double[] { north, west, south, east };

    }

    /**
     * _more_
     *
     * @param results _more_
     *
     * @return _more_
     */
    public static Bounds parseGdalInfo(String results) {
        /*
          Upper Left  (  -28493.167, 4255884.544) (117d38'27.05"W, 33d56'37.74"N)
          Lower Left  (  -28493.167, 4224973.143) (117d38'27.05"W, 33d39'53.81"N)
          Upper Right (    2358.212, 4255884.544) (117d18'28.38"W, 33d56'37.74"N)
          Lower Right (    2358.212, 4224973.143) (117d18'28.38"W, 33d39'53.81"N)
        */

        double north = Double.NaN;
        double south = Double.NaN;
        double east  = Double.NaN;
        double west  = Double.NaN;
        for (String line : StringUtil.split(results, "\n", true, true)) {
            double[] latlon;
            if (line.indexOf("Upper Left") >= 0) {
                latlon = getGdalLatLon(line);
                if (latlon != null) {
                    north = ((north != north)
                             ? latlon[1]
                             : Math.max(north, latlon[1]));
                    west  = ((west != west)
                             ? latlon[0]
                             : Math.min(west, latlon[0]));
                }
            } else if (line.indexOf("Lower Right") >= 0) {
                latlon = getGdalLatLon(line);
                if (latlon != null) {
                    south = ((south != south)
                             ? latlon[1]
                             : Math.min(south, latlon[1]));
                    east  = ((east != east)
                             ? latlon[0]
                             : Math.max(east, latlon[0]));
                }
            } else if (line.indexOf("Upper Right") >= 0) {
                latlon = getGdalLatLon(line);
                if (latlon != null) {
                    north = ((north != north)
                             ? latlon[1]
                             : Math.max(north, latlon[1]));
                    east  = ((east != east)
                             ? latlon[0]
                             : Math.max(east, latlon[0]));
                }
            } else if (line.indexOf("Lower Left") >= 0) {
                latlon = getGdalLatLon(line);
                if (latlon != null) {
                    south = ((south != south)
                             ? latlon[1]
                             : Math.min(south, latlon[1]));
                    west  = ((west != west)
                             ? latlon[0]
                             : Math.min(west, latlon[0]));
                }

            } else {}
        }

        Bounds bounds = null;

        if ( !Double.isNaN(north)) {
            bounds = new Bounds(north, west, south, east);
        }

        return bounds;
    }

    /**
     * _more_
     *
     * @param line _more_
     *
     * @return _more_
     */
    private static double[] getGdalLatLon(String line) {
        line = line.trim();
        line = StringUtil.findPattern(line, ".*\\(([^\\)]+)\\.*");
        //        System.err.println("TOK: " + line);
        if (line == null) {
            return null;
        }

        List<String> toks = StringUtil.split(line, ",", true, true);
        if (toks.size() != 2) {
            return null;
        }

        return new double[] { decodeGdalLatLon(toks.get(0)),
                              decodeGdalLatLon(toks.get(1)) };
    }

    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    private static double decodeGdalLatLon(String s) {
        s = s.replace("d", ":");
        s = s.replace("'", ":");
        s = s.replace("\"", "");

        return Misc.decodeLatLon(s);
    }


    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        if (true) {
            System.err.println(
                getAddressFromLatLon(
                    Double.parseDouble(args[0]),
                    Double.parseDouble(args[1])));

            return;
        }


        /*

          if(true) {
          System.err.println(findFeatureField(args[0],"drainage",
          Double.parseDouble(args[1]),
          Double.parseDouble(args[2]),null));
          return;
          }
        */

        setCacheDir(new File("."));

        /*
          if(true) {
          //      System.err.println(getPreciselyToken(true));
          System.err.println(getNeighborhood(39.989094,-105.222402));
          return;
          }*/

        try {
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if (arg.equals("-googlekey")) {
                    setGoogleKey(args[++i]);
                    continue;
                }

                Place place = getLocationFromAddressInner(arg, null, true);

                if (place == null) {
                    System.out.println(arg + ": NA");
                } else {
                    System.out.println(arg + ": PLACE: " + place);
                }
            }
        } catch (Exception exc) {
            System.out.println(exc);
        }
        if (true) {
            return;
        }

        double[][] xyz = {
            { -2307792.824, -4160678.918, 4235698.873 }
        };
        double[]   result;
        for (int i = 0; i < xyz.length; i++) {
            result = wgs84XYZToLatLonAlt(xyz[i][0], xyz[i][1], xyz[i][2]);
            //            result = wgs84XYZToLatLonAlt(xyz[i][1], xyz[i][0], xyz[i][2]);
            System.out.println(result[0] + "/" + result[1] + "/" + result[2]);
        }

    }




}
