/**
   Copyright (c) 2008-2023 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util.geo;


import org.json.*;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.Utils;


import org.ramadda.util.geo.Bounds;

import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ucar.unidata.xml.XmlUtil;

import java.awt.Polygon;

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

    public static final String PREFIX_ANY = "any:";
    public static final String PREFIX_STATE = "state:";
    public static final String PREFIX_COUNTRY = "country:";
    public static final String PREFIX_COUNTY = "county:";
    public static final String PREFIX_CITY = "city:";
    public static final String PREFIX_ZIP = "zip:";
    public static final String PREFIX_ZCTA="zcta:";
    public static final String PREFIX_CONGRESS = "congress:";
    public static final String PREFIX_TRACT = "tract:";

    public static final Pattern latLonPattern1 = Pattern.compile("(\\d+)°(\\d+)'([\\d\\.]+)\"");

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
    private static PrintWriter cacheWriter;

    /** _more_ */
    private static String cacheDelimiter = "_delim_";

    /**  */
    private static boolean haveInitedKeys = false;

    /** _more_ */
    private static Hashtable<String,String> statesMap;

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
     * _more_
     *
     * @param lat _more_
     * @param lon _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static Address getAddress(double lat, double lon)
	throws Exception {
        String                    key   = lat + "_" + lon;
        Hashtable<String, Address> addresses= getAddresses();
        if (addresses != null) {
            Address address = addresses.get(key);
            if (address != null) {
                return address;
            }
        }
	Address address = getAddressInner(lat,lon);
        if (address!=null && addressesWriter != null) {
	    synchronized(addressesWriter) {
		addressesWriter.println(key + cacheDelimiter + address.encode());
		addressesWriter.flush();
	    }
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
    private static Address getAddressInner(double lat, double lon)
	throws Exception {
        initKeys();
        Address address = null;
        if ((address == null) && (googleKey != null)) {
            address = getAddressFromGoogle(lat, lon);
        }
        if ((address == null) && (hereKey != null)) {
            address = getAddressFromLatLonHere(lat, lon);
	    System.err.println("HERE ADDRESS:" + address);
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
	//	System.err.println(json);
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

        String a = "";
	if(address.has("houseNumber"))
	    a = address.getString("houseNumber") + " ";
	if(address.has("street"))
	    a = a + address.getString("street");

        return new Address(a.trim(),
			   address.getString("city"),
                           address.getString("county"),
                           address.getString("state"),
                           address.getString("postalCode"),
                           address.getString("countryName"));

    }




    private static Object ADDRESS_MUTEX = new Object();

    /** _more_ */
    private static Hashtable<String, Place> addressToLocation = null;

    /** _more_ */
    private static Hashtable<String, String> hoods = null;

    /** _more_ */
    private static PrintWriter hoodsWriter;


    private static Hashtable<String, Address> addresses = null;    

    /** _more_ */
    private static PrintWriter addressesWriter;


    private static Hashtable<String, Place> getGeocodeCache() throws Exception {
        if (addressToLocation == null) {
	    synchronized(ADDRESS_MUTEX) {
		if (addressToLocation == null) {
		    Hashtable<String, Place> tmp =  new Hashtable<String, Place>();
		    File cacheDir =  IO.getCacheDir();
		    if (cacheDir != null) {
			File cacheFile = new File(IOUtil.joinDir(cacheDir,
								 "addresslocations2.txt"));
			if (cacheFile.exists()) {
			    try(BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(cacheFile)))) {
				while(true) {
				    String line  = reader.readLine();
				    if(line==null) break;
				    line = line.trim();
				    if(line.length()==0) continue;
				    List<String> toks = StringUtil.split(line,
									 cacheDelimiter);
				    if (toks.size() == 4) {
					tmp.put(toks.get(0),
						new Place(toks.get(1),
							  Double.parseDouble(toks.get(2)),
							  Double.parseDouble(toks.get(3))));
				    }
				}
			    }
			}
			FileWriter     fw = new FileWriter(cacheFile, true);
			BufferedWriter bw = new BufferedWriter(fw);
			cacheWriter = new PrintWriter(bw);
		    }
		    addressToLocation = tmp;
		}
	    }
	}
	return addressToLocation;
    }

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
    public static Hashtable<String,String> getStatesMap() throws Exception {
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
            statesMap = (Hashtable<String,String>) tmp;
        }

        return statesMap;
    }

    private static String cleanAddress(String s) {
	return s.toLowerCase().replaceAll("[-_]+"," ").replaceAll("\\s\\s+"," ");
    }

    public static class TiledObject {
	public double latitude;
	public double longitude;
	public Object object;
	public TiledObject(Object _object,double _latitude, double _longitude) {
	    this.object = _object;
	    this.latitude = _latitude;
	    this.longitude = _longitude;	    
	}
	public Object getObject() {
	    return object;
	}


    }


    public static class Tile {
	private List<TiledObject> objects = new ArrayList<TiledObject>();
	public Tile() {
	}

	public void add(TiledObject obj) {
	    objects.add(obj);
	}

	public List<TiledObject> getObjects() {
	    return objects;
	}
    }    

    public static List<Tile> tile(List<TiledObject> objects, int dim) {
	List<Tile> tiles = new ArrayList<Tile>();
	Bounds bounds = new Bounds();
	for(TiledObject obj: objects) {
	    bounds.expand(obj.latitude,obj.longitude);
	}
	Hashtable<String,Tile> tilesMap = new Hashtable<String,Tile>();
	double latitudeStep = (bounds.getNorth()-bounds.getSouth())/dim;
	double longitudeStep = (bounds.getEast()-bounds.getWest())/dim;	
	for(TiledObject obj: objects) {
	    //40-50 
	    int yIndex = latitudeStep==0? 0:(int)((obj.latitude-bounds.getSouth())/latitudeStep);
	    int xIndex = longitudeStep==0? 0:(int)((obj.longitude-bounds.getWest())/longitudeStep);	    
	    String key = yIndex+"_"+xIndex;
	    Tile tile = tilesMap.get(key);
	    if(tile==null) {
		tilesMap.put(key, tile = new Tile());
		tiles.add(tile);
	    }
	    tile.add(obj);
	}
	return tiles;
    }

    private static class Locale {
        boolean doCountry = false;
        boolean doState = false;
        boolean doCounty = false;
        boolean doTract = false;	
        boolean doCity = false;
	boolean doAny  = false;
	GeoResource resource;
	GeoResource resource2;
	String  address;
	String  _address;	
	String  cleanAddress;

	Locale(String a) {
	    address= a.trim().replaceAll("\\s\\s+"," ");
	    if (address.startsWith(PREFIX_ANY)) {
		doCountry = doState =  doCounty = doCity = true;
		address   = address.substring(PREFIX_ANY.length()).trim();
		doAny = true;
	    }

	    if (address.startsWith("from:")) {
		address  = address.substring(5);
	    } else if (address.startsWith("to:")) {
		address  = address.substring(3);
	    }

	    if (address.startsWith(PREFIX_COUNTRY)) {
		address   = address.substring(PREFIX_COUNTRY.length()).trim();
		doCountry = true;
	    }

	    if (address.startsWith(PREFIX_STATE)) {
		address  = address.substring(PREFIX_STATE.length()).trim();
		doState  = true;
	    }

	    if (address.startsWith(PREFIX_ZIP)) {
		address  = address.substring(PREFIX_ZIP.length()).trim();
		if (address.length() > 5) {
		    address = address.substring(0, 5);
		}
		resource = GeoResource.RESOURCE_ZIPCODES;
		resource2 = GeoResource.RESOURCE_ZCTA;
	    }
	    if (address.startsWith(PREFIX_ZCTA)) {
		address  = address.substring(PREFIX_ZCTA.length()).trim();
		if (address.length() > 5) {
		    address = address.substring(0, 5);
		}
		resource = GeoResource.RESOURCE_ZCTA;
		resource2 = GeoResource.RESOURCE_ZIPCODES;
	    }

	    if (address.startsWith(PREFIX_CONGRESS)) {
		address  = address.substring(PREFIX_CONGRESS.length()).trim();
		resource = GeoResource.RESOURCE_CONGRESS;
	    }

	    if (address.startsWith(PREFIX_TRACT)) {
		doTract = true;
		address  = address.substring(PREFIX_TRACT.length()).trim();
		resource = GeoResource.RESOURCE_TRACTS;
	    }	    

	    if (address.startsWith(PREFIX_COUNTY)) {
		address  = address.substring(PREFIX_COUNTY.length()).trim();
		doCounty = true;
	    }

	    if (address.startsWith(PREFIX_CITY)) {
		address  = address.substring(PREFIX_CITY.length()).trim();
		doCity   = true;
		//For when there is no city, just a state
		if (address.startsWith(",")) {
		    doState  = true;
		    doCity   = false;
		    address  = address.substring(1).trim();
		}
	    }
	    _address=address.toLowerCase();
	    cleanAddress=cleanAddress(_address);
	}

	public Place match() {
	    Place place = null;
	    if(doTract) {
		place = resource.getPlace(address);
		//A hack, try prepending a "0"
		if(place==null)
		    place = resource.getPlace("0"+address);
		return place;
	    }



	    if(resource!=null)
		place=match(resource);
	    if(place==null && resource2!=null)
		place=match(resource2);
	    if(doAny) {
		for(GeoResource r: GeoResource.RESOURCES) {
		    place = match(r);
		    if(place!=null) return place;
		}
	    }
	    return place;
	}

	public Place match(GeoResource resource) {
	    Place place = resource.getPlace(address);
	    if(place==null)
		place = resource.getPlace(_address);
	    if(place==null)
		place = resource.getPlace(cleanAddress);
	    return place;
	}
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

	//	debug = true;
        if ( !Utils.stringDefined(address) || address.equals(",")) {
            return null;
        }
	if(debug)
	    System.err.println("address:" + address);

        Place       place    = null;
        GeoResource resource = null;
	GeoResource resource2 = null;
	Locale locale = new Locale(address);
	place = locale.match();
	if(place!=null) return place;
	if (locale.doTract) return null;

        if (locale.doCity) {
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
                List<String> toks  = StringUtil.splitUpTo(locale.address, ",", 2);
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
                    state = state.replaceAll("\\.", "");
                    List<String> tmp = StringUtil.split(state, "-", true,
							true);
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
            locale.doCounty = true;
        }


	if (locale.doCounty) {
            resource = GeoResource.RESOURCE_COUNTIES;
            int index = locale.cleanAddress.indexOf(",");
            if (index < 0) {
                place =  locale.match(resource);
		if(place!=null) return place;
            }
            getStatesMap();
            List<String> toks   = StringUtil.split(locale.cleanAddress, ",");
            String       county = toks.get(0).trim();
            String       state  = toks.size()<2?null:toks.get(1).trim();
            if (debug) {
                System.out.println("\ttrying:" + county + "," + state);
            }
	    if(state!=null) {
		place = resource.getPlace(county + "," + state);
		if (place != null) {
		    return place;
		}
		state = (String) statesMap.get(state);
		if (debug) {
		    System.out.println("\tstate after:" + county + "," + state);
		}
		if (state != null) {
		    if (debug) {
			System.out.println("\ttrying:" + county + "," + state);
		    }
		    place = resource.getPlace(county + "," + state);
		    if (place != null) {
			return place;
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
		}
            }
            locale.doState = true;
        }


        if (locale.doState) {
	    resource = GeoResource.RESOURCE_STATES;
            place    = locale.match(resource);
            if (place != null) {
                return place;
            }
            locale.doCountry = true;
        }


        if (locale.doCountry) {
            resource = GeoResource.RESOURCE_COUNTRIES;
            place    = locale.match(resource);
            if (place != null) {
                return place;
            }
	}

        if (resource != null) {
            place = locale.match(resource);
	    //	    System.err.println("PLACE:" + place);
	    if(place==null && resource2!=null) {
		place = locale.match(resource2);		
	    }
            if (place == null) {
                if ( !noPlaceSet.contains(locale.address)) {
                    noPlaceSet.add(locale.address);
		    //		    System.out.println("no place:" + address);
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

	if(locale.doAny) {
	    return null;
	}

        initKeys();
	place = getGeocodeCache().get(address);
	if (place != null) {
	    if (debug) {
		System.err.println("\tfound in cached address list:" + place);
	    }
	    return place;
        }
        //        System.err.println("looking for address:" + address);
        String latString      = null;
        String lonString      = null;
        String encodedAddress = StringUtil.replace(address, " ", "%20");
        String name           = address;
        if (place == null && googleKey != null) {
            try {
		String url =HtmlUtils.url("https://maps.googleapis.com/maps/api/geocode/json",
					  "address", address, "key" ,googleKey);
                if (bounds != null) {
                    url += "&bounds=" + bounds.getSouth() + ","
			+ bounds.getWest() + "|" + bounds.getNorth() + ","
			+ bounds.getEast();
                }
                String result = IO.readContents(url, GeoUtils.class);
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
		    String status = json.optString("status","");
		    if(!status.equals("ZERO_RESULTS")) {
			String message = json.optString("error_message",null);
			if(message!=null)  message = result;
			System.err.println("google error:"+ message);
		    }
                }
            } catch (Exception exc) {
                System.err.println("Error calling google:" + exc);
            }
	    if(debug)
		System.err.println("\tgoogle:" + place);
        }

        if ((place != null) && !place.within(bounds)) {
            place = null;
        }
        if ((place == null) && (geocodeioKey != null)) {
            String url = HtmlUtils.url("https://api.geocod.io/v1.6/geocode","q", address, "api_key", geocodeioKey);
	    try {
		String result = IO.readContents(url, GeoUtils.class);
		//"lat":39.988424,"lng":-105.226083
		latString = StringUtil.findPattern(result,
						   "\"lat\"\\s*:\\s*([-\\d\\.]+),");
		lonString = StringUtil.findPattern(result,
						   "\"lng\"\\s*:\\s*([-\\d\\.]+)\\s*");
		if ((latString != null) && (lonString != null)) {
		    place = new Place(name, latString, lonString);
		}
	    } catch(Exception exc) {
		System.err.println("Geocode Error:" + url +" error:" + exc);
		//throw new IllegalArgumentException("Error geocoding address:" + address+" error:" + exc);
	    }
	    if (debug) {
                System.err.println("\tgeocodeio:" + place);
            }
        }

        if ((place != null) && !place.within(bounds)) {
            place = null;
        }
        if ((place == null) && (hereKey != null)) {
            String url = HtmlUtils.url(
				       "https://geocode.search.hereapi.com/v1/geocode",
				       "q", address, "apiKey", hereKey);
            String result = IO.doGet(new URL(url));
            latString = StringUtil.findPattern(result,
					       "\"lat\"\\s*:\\s*([-\\d\\.]+),");
            lonString = StringUtil.findPattern(result,
					       "\"lng\"\\s*:\\s*([-\\d\\.]+)\\s*");
            if ((latString != null) && (lonString != null)) {
                place = new Place(name, latString, lonString);
            }
            if (debug) {
                System.err.println("\there:" + place);
            }
        }


        if ((place != null) && !place.within(bounds)) {
            place = null;
        }
        if (place == null) {
            //fall back to us census
            String url =HtmlUtils.url("https://geocoding.geo.census.gov/geocoder/locations/onelineaddress",
				      "format","json","benchmark","2020","address", address);
            String result = IO.readContents(url, GeoUtils.class);
            latString = StringUtil.findPattern(result,
					       "\"y\"\\s*:\\s*([-\\d\\.]+)");
            lonString = StringUtil.findPattern(result,
					       "\"x\"\\s*:\\s*([-\\d\\.]+)\\s*");
            if ((latString != null) && (lonString != null)) {
                place = new Place(name, latString, lonString);
            }
            if (debug) {
                System.err.println("\tcensus:" + place);
            }
        }

        if (place != null) {
	    getGeocodeCache().put(address, place);
            if (cacheWriter != null) {
		synchronized(ADDRESS_MUTEX) {
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
		synchronized(ADDRESS_MUTEX) {
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
	System.err.println("key:" + key);
	System.err.println("b64:" + b64);
        String json =
            IO.doPost(new URL("https://api.precisely.com/oauth/token"),
                      "grant_type=client_credentials", "Authorization",
                      "Basic " + b64, "Content-Type",
                      "application/x-www-form-urlencoded");
	//	System.err.println(json);
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
	File cacheDir =  IO.getCacheDir();
        if ((hoods == null) && (cacheDir != null)) {
            File cacheFile = new File(IOUtil.joinDir(cacheDir,
						     "neighborhoods.txt"));
            if (cacheFile.exists()) {
                hoods = new Hashtable<String, String>();
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

    private static Hashtable<String,Address> getAddresses() throws Exception {
	File cacheDir =  IO.getCacheDir();
        if ((addresses == null) && (cacheDir != null)) {
            File cacheFile = new File(IOUtil.joinDir(cacheDir,
						     "addresses.txt"));
            if (cacheFile.exists()) {
		addresses = new Hashtable<String, Address>();
                for (String line :
			 StringUtil.split(IO.readContents(cacheFile), "\n",
					  true, true)) {
                    List<String> toks = StringUtil.split(line,
							 cacheDelimiter);
		    String key = toks.get(0);
		    Address address = new Address();
		    address.decode(toks.get(1));
                    addresses.put(key,address);
                }
            }
            FileWriter     fw = new FileWriter(cacheFile, true);
            BufferedWriter bw = new BufferedWriter(fw);
            addressesWriter = new PrintWriter(bw);
        }
        return addresses;
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
		//		System.err.println("got from cache:" + hood);
                return hood;
            }
        }
	String hood = getNeighborhoodInner(lat,lon);
        if (hood!=null && hoodsWriter != null) {
	    synchronized(hoodsWriter) {
		hoodsWriter.println(key + cacheDelimiter + hood);
		hoodsWriter.flush();
	    }
        }

        return hood;
    }

    private static String getNeighborhoodGoogle(double lat, double lon)
	throws Exception {
	String url=HtmlUtils.url("https://maps.googleapis.com/maps/api/geocode/json",
				 "result_type","neighborhood",
				 "key",googleKey,
				 "latlng",lat+","+lon);
        String json = IO.doGet(new URL(url));
	//	System.err.println(json);
	/*
	  "results" : [
	  {
	  "address_components" : [
	  {
	  "long_name" : "Keewayden",
	  "short_name" : "Keewayden",
	  "types" : [ "neighborhood", "political" ]
	  },
	*/
	JSONObject obj = new JSONObject(json);
	JSONArray results = obj.optJSONArray("results");
	if(results==null) return null;
	if(results.length()==0) return null;
	for(int i=0;i<results.length();i++) {
	    obj = results.getJSONObject(i);
	    JSONArray comps = obj.optJSONArray("address_components");
	    if(comps==null) continue;
	    for(int j=0;i<comps.length();j++) {
		obj = comps.getJSONObject(j);
		JSONArray types = obj.optJSONArray("types");
		if(types==null) continue;
		if(!JsonUtil.getList(types).contains("neighborhood")) continue;
		return obj.getString("long_name");
	    }
	    
	}
	//	System.err.println(json);
	return null;
    }




    private static String getNeighborhoodInner(double lat, double lon)
	throws Exception {
	initKeys();
	if(googleKey!=null) {
	    //	    return getNeighborhoodGoogle(lat,lon);
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
	//	System.err.println(json);
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
        return  nameObject.getString("value");
    }
    


    private static boolean checkGoogleComponent(JSONObject obj, String type) {
	JSONArray types = obj.optJSONArray("types");
	if(types==null) return false;
	return JsonUtil.getList(types).contains(type);
    }

    static String lastJson;

    private static Address getAddressFromGoogle(double lat, double lon)
	throws Exception {
	String url=HtmlUtils.url("https://maps.googleapis.com/maps/api/geocode/json",
				 //				 "result_type","neighborhood",
				 "key",googleKey,
				 "latlng",lat+","+lon);
        String json = IO.doGet(new URL(url));
	lastJson = json;
	/*
	  "results" : [
	  {
	  "address_components" : [
	  {
	  "long_name" : "Keewayden",
	  "short_name" : "Keewayden",
	  "types" : [ "neighborhood", "political" ]
	  },
	*/
	JSONObject obj = new JSONObject(json);
	JSONArray results = obj.optJSONArray("results");
	if(results==null) return null;
	if(results.length()==0) return null;
	Address address = new Address();
	String number = null;
	String street = null;	
	for(int i=0;i<results.length();i++) {
	    obj = results.getJSONObject(i);
	    JSONArray comps = obj.optJSONArray("address_components");
	    if(comps==null) continue;
	    for(int j=0;j<comps.length();j++) {
		obj = comps.getJSONObject(j);
		if(checkGoogleComponent(obj,"street_number")) {
		    number = obj.getString("long_name");
		    if(street!=null) address.setAddress(number+" " + street);
		} else if(checkGoogleComponent(obj,"route")) {
		    street = obj.getString("long_name");
		    if(number!=null) address.setAddress(number+" " + street);
		} else if(checkGoogleComponent(obj,"locality")) {
		    address.setCity(obj.getString("long_name"));
		} else if(checkGoogleComponent(obj,"administrative_area_level_2")) {
		    address.setCounty(obj.getString("long_name"));
		} else if(checkGoogleComponent(obj,"administrative_area_level_1")) {
		    address.setState(obj.getString("long_name"));
		} else if(checkGoogleComponent(obj,"country")) {
		    address.setCountry(obj.getString("long_name"));
		} else if(checkGoogleComponent(obj,"postal_code")) {
		    address.setPostalCode(obj.getString("long_name"));		    
		}		
		if(address.isComplete()) return address;
	    }
	    if(address.isPartialComplete()) return address;
	}
	return null;
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

    public static boolean latLonOk(Object o) {
        if (o == null) {
            return false;
        }
        Double d = (Double) o;

        return latLonOk(d.doubleValue());
    }

    

    //Hackish as  this is really checking for a -9999 sortof null value
    public static boolean latLonOk(double v) {
        return ((v == v) && (v>=-180 && v<=360));
    }


    public static boolean latLonOk(double lat, double lon) {
	return lat>=-90 && lat<=90 &&
	    lon>=-180 && lon<=360;
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

    public static double getHMS(String hours, String minutes, String seconds) {
	double value=
	    (hours.equals(""))  ? 0 : Double.parseDouble(hours);
	if ( !minutes.equals("")) {
	    value += Double.parseDouble(minutes) / 60.;
	}
	if ( !seconds.equals("")) {
	    value += Double.parseDouble(seconds) / 3600.;
	}
	//	System.err.println(hours +" " +minutes +" " + seconds  +" value=" + value);
	return value;
    }

    /**
     * This method is taken from Unidatas ucar.unidata.util.Misc method.
     * I moved it here to not have it use the parseNumber because that would use
     * a DecimalFormat which was picking up the Locale
     *
     * Decodes a string representation of a latitude or longitude and
     * returns a double version (in degrees).  Acceptible formats are:
     * <pre>
     * +/-  ddd:mm, ddd:mm:, ddd:mm:ss, ddd::ss, ddd.fffff ===>   [+/-] ddd.fffff
     * +/-  ddd, ddd:, ddd::                               ===>   [+/-] ddd
     * +/-  :mm, :mm:, :mm:ss, ::ss, .fffff                ===>   [+/-] .fffff
     * +/-  :, ::                                          ===>       0.0
     * Any of the above with N,S,E,W appended
     * </pre>
     *
     * @param latlon  string representation of lat or lon
     * @return the decoded value in degrees
     */
    public static double decodeLatLon(String latlon) {
        // first check to see if there is a N,S,E,or W on this
	latlon = latlon.trim();
        int    dirIndex    = -1;
        int    southOrWest = 1;
        double value       = Double.NaN;
        if (latlon.indexOf("S") > 0) {
            southOrWest = -1;
            dirIndex    = latlon.indexOf("S");
        } else if (latlon.indexOf("W") > 0) {
            southOrWest = -1;
            dirIndex    = latlon.indexOf("W");
        } else if (latlon.indexOf("N") > 0) {
            dirIndex = latlon.indexOf("N");
        } else if (latlon.endsWith("E")) {  // account for 9E-3, 9E-3E, etc
            dirIndex = latlon.lastIndexOf("E");
        }

        if (dirIndex > 0) {
            latlon = latlon.substring(0, dirIndex).trim();
        }

        // now see if this is a negative value
        if (latlon.indexOf("-") == 0) {
            southOrWest *= -1;
            latlon      = latlon.substring(latlon.indexOf("-") + 1).trim();
        }

        if (latlon.indexOf(":") >= 0) {  //have something like DD:MM:SS, DD::, DD:MM:, etc
            int    firstIdx = latlon.indexOf(":");
            String hours    = latlon.substring(0, firstIdx);
            String minutes  = latlon.substring(firstIdx + 1);
            String seconds  = "";
            if (minutes.indexOf(":") >= 0) {
                firstIdx = minutes.indexOf(":");
                String temp = minutes.substring(0, firstIdx);
                seconds = minutes.substring(firstIdx + 1);
                minutes = temp;
            }
            try {
		value = getHMS(hours,minutes,seconds);
            } catch (NumberFormatException nfe) {
                value = Double.NaN;
            }
        } else {  //have something like DD.ddd
	    Matcher m = latLonPattern1.matcher(latlon);
	    if (m.find()) {
		value = getHMS(m.group(1),m.group(2),m.group(3));
	    }

	    if(Double.isNaN(value)) {
		try {
		    value = Double.parseDouble(latlon);
		} catch (NumberFormatException nfe) {
		    value = Double.NaN;
		}
	    }
        }

        return value * southOrWest;
    }


    /**
     * _more_
     *
     * @param s _more_
     * @param points _more_
     *
     * @return _more_
     */
    public static List<double[]> parsePointString(String s,
						  List<double[]> points) {
        if (s == null) {
            return points;
        }
        for (String pair : Utils.split(s, ";", true, true)) {
            List<String> toks = Utils.splitUpTo(pair, ",", 2);
            if (toks.size() != 2) {
                continue;
            }
            double lat = GeoUtils.decodeLatLon(toks.get(0));
            double lon = GeoUtils.decodeLatLon(toks.get(1));
            points.add(new double[] { lat, lon });
        }

        return points;
    }


    private static int POLYGON_SCALE = 1000;
    
    /*
      Array of <latitude,longitude,latitude,longitude>
      this applies the POLYGON_SCALE value to scale the points up to integer
     */
    public static Polygon makePolygon(double[]d) {
	int[]x=new int[d.length/2];
	int[]y=new int[d.length/2];	
	for(int i=0;i<d.length;i+=2) {
	    y[i/2] = (int)(d[i]*POLYGON_SCALE);
	    x[i/2] = (int)(d[i+1]*POLYGON_SCALE);	    

	}
	return new Polygon(x,y,x.length);
    }
    /*
      List of <latitude,longitude,latitude,longitude>
      this applies the POLYGON_SCALE value to scale the points up to integer
     */
    public static Polygon makePolygon(List<Double>d) {
	int[]x=new int[d.size()/2];
	int[]y=new int[d.size()/2];	
	for(int i=0;i<d.size();i+=2) {
	    y[i/2] = (int)(d.get(i)*POLYGON_SCALE);
	    x[i/2] = (int)(d.get(i+1)*POLYGON_SCALE);	    

	}
	return new Polygon(x,y,x.length);
    }
    
    public static boolean polygonContains(Polygon polygon, double lat,double lon) {
	return polygon.contains(POLYGON_SCALE*lon,POLYGON_SCALE*lat);

    }
	



    public static class UTMInfo {
	String zone;
	String epsg;
	double easting;
	double northing;
	public UTMInfo(String zone, String epsg, double easting, double northing) {
	    this.zone = zone;
	    this.epsg = epsg;
	    this.easting = easting;
	    this.northing = northing;
	}

	@Override
	public String toString() {
	    return "UTM EPSG: " + epsg +" Zone: " + zone +
		" Easting: " + easting  +" Northing:" + northing;
	}

	public String getZone() {
	    return zone;
	}
	public String getEpsg() {
	    return epsg;
	}
	public double getEasting() {
	    return easting;
	}
	public double getNorthing() {
	    return northing;
	}
    }

    public static UTMInfo  LatLonToUTM(double latitude, double longitude) {
        // Determine UTM zone
        int zone = (int)Math.floor((longitude + 180) / 6) + 1;
        boolean isNorthernHemisphere = latitude >= 0;

        String utmCode = "EPSG:" + (isNorthernHemisphere ? 326 : 327) + zone;

        org.locationtech.proj4j.CRSFactory crsFactory = new org.locationtech.proj4j.CRSFactory();
        org.locationtech.proj4j.CoordinateTransformFactory ctFactory = new org.locationtech.proj4j.CoordinateTransformFactory();

        org.locationtech.proj4j.CoordinateReferenceSystem crsLatLon = crsFactory.createFromName("EPSG:4326"); // WGS84
        org.locationtech.proj4j.CoordinateReferenceSystem crsUTM = crsFactory.createFromName(utmCode); // UTM Zone

        org.locationtech.proj4j.CoordinateTransform transform = ctFactory.createTransform(crsLatLon, crsUTM);

        org.locationtech.proj4j.ProjCoordinate srcCoord = new org.locationtech.proj4j.ProjCoordinate(longitude, latitude);
        org.locationtech.proj4j.ProjCoordinate dstCoord = new org.locationtech.proj4j.ProjCoordinate();

        transform.transform(srcCoord, dstCoord);

	return new UTMInfo(zone+(isNorthernHemisphere?"N":"S"),
			   utmCode,dstCoord.x,dstCoord.y);
    }


    public static void main(String[] args) throws Exception {
	System.err.println("lat:" + 40 +" lon:" + -107 +" " +LatLonToUTM(40,-107));
	System.err.println("lat:" + -40 +" lon:" + -107 +" " +LatLonToUTM(-40,-107));	
	if(true) return;
	int scale = 1000;
	List<Double> d = Utils.getDoubles("44.992342,-110.65619,44.430205,-88.507752,32.875383,-87.980408,29.874414,-98.351502,33.537222,-110.65619,43.989213,-112.06244");
	
	Polygon poly = makePolygon(d);
	System.err.println(polygonContains(poly,40,-104));
	
	if(true) return;
	for(String s: args) {
	    System.err.println(decodeLatLon(s));
	}

	if(true) return;



	initKeys();
	System.err.println(getAddress(39.9905392833907,-105.22957815436592));
	//	System.err.println(getNeighborhood(39.9905392833907,-105.22957815436592));



        /*

          if(true) {
          System.err.println(findFeatureField(args[0],"drainage",
          Double.parseDouble(args[1]),
          Double.parseDouble(args[2]),null));
          return;
          }
        */

        IO.setCacheDir(new File("."));

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
	    exc.printStackTrace();
        }
        if (true) {
	    Utils.exitTest(0);
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
