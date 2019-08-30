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


import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import ucar.unidata.xml.XmlUtil;


import java.io.*;

import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;


/**
 * A set of utility methods for dealing with geographic things
 * Note: this was originally in the  the NLAS SF SVN repository AND the
 * Unavco GSAC SVN repository.
 *
 * @author  Jeff McWhirter
 */
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
    private static File cacheDir;

    /** _more_ */
    private static PrintWriter cacheWriter;

    /** _more_ */
    private static String cacheDelimiter = "_delim_";

    /**
     * _more_
     *
     * @param key _more_
     */
    public static void setGoogleKey(String key) {
        googleKey = key;
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
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        String key = null;
        try {
            for (String arg : args) {
                if (key == null) {
                    key = arg;

                    continue;
                }
                Place place = getLocationFromAddress(arg, key);
                if (place == null) {
                    System.out.println(arg + ": NA");
                } else {
                    System.out.println(arg + ": " + place);
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

    /** _more_ */
    private static Hashtable<String, Place> addressToLocation = null;




    /**
     * Look up the location of the given address
     *
     * @param address The address
     *
     * @return The location or null if not found
     */
    public static Place getLocationFromAddress(String address) {
        return getLocationFromAddress(address, null);
    }

    /**
     * _more_
     *
     * @param address _more_
     * @param googleKey _more_
     *
     * @return _more_
     */
    public static Place getLocationFromAddress(String address,
            String googleKey) {
        return getLocationFromAddress(address, googleKey, null);
    }

    /**
     * _more_
     *
     * @param address _more_
     * @param googleKey _more_
     * @param bounds _more_
     *
     * @return _more_
     */
    public static Place getLocationFromAddress(String address,
            String googleKey, Bounds bounds) {
        try {
            return getLocationFromAddressInner(address, googleKey, bounds);
        } catch (Exception exc) {
            exc.printStackTrace();

            throw new RuntimeException(exc);
        }
    }

    /** _more_ */
    private static Hashtable statesMap;

    /** _more_ */
    private static Hashtable<String, Place> citiesMap;

    /** _more_ */
    private static final String[] citySuffixes = new String[] { " city",
            " town", " cdp", " village" };


    /**
     * _more_
     *
     * @param address _more_
     * @param googleKey _more_
     * @param bounds _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */


    public static Hashtable getStatesMap() throws Exception {
        if (statesMap == null) {
            InputStream inputStream =
                IOUtil.getInputStream("/org/ramadda/util/states.properties",
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
     * _more_
     *
     * @param address _more_
     * @param googleKey _more_
     * @param bounds _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static Place getLocationFromAddressInner(String address,
            String googleKey, Bounds bounds)
            throws Exception {

        if (address == null) {
            return null;
        }
        address = address.trim();
        if (address.length() == 0) {
            return null;
        }
        if (address.toLowerCase().startsWith("from:")) {
            address = address.substring(5);
        } else if (address.toLowerCase().startsWith("to:")) {
            address = address.substring(3);
        }
        boolean doCountry = false;
        if (address.toLowerCase().startsWith("country:")) {
            address   = address.substring("country:".length()).trim();
            doCountry = true;
        }
        boolean doState = false;
        if (address.toLowerCase().startsWith("state:")) {
            address = address.substring("state:".length()).trim();
            doState = true;
        }

        boolean doZip = false;
        if (address.toLowerCase().startsWith("zip:")) {
            address = address.substring("zip:".length()).trim();
            if (address.length() > 5) {
                address = address.substring(0, 5);
            }
            doZip = true;
        }
        String  _address = address.toLowerCase();
        boolean doCounty = false;
        if (address.toLowerCase().startsWith("county:")) {
            address  = address.substring("county:".length()).trim();
            _address = _address.substring("county:".length()).trim();
            doCounty = true;
        }

        boolean doCity = false;
        if (_address.startsWith("city:")) {
            address  = address.substring("city:".length()).trim();
            _address = _address.substring("city:".length()).trim();
            doCity   = true;
        }
        //        System.err.println ("address:" + address +" " + doZip);

        if (address.length() == 0) {
            return null;
        }


        Place.Resource resource = null;
        Place          place    = null;
        if (doCountry) {
            resource = Place.getResource("countries");
        }

        if (doState) {
            resource = Place.getResource("states");
        }

        if (doZip) {
            resource = Place.getResource("zipcodes");
        }



        if (resource != null) {
            place = resource.getPlace(address);
            if (place == null) {
                System.err.println("no place:" + address);
            }

            return place;
        }

        if (doCity) {
            //abbrev to name
            getStatesMap();
            if (citiesMap == null) {
                citiesMap = new Hashtable<String, Place>();
                List<Place> places = Place.getPlaces("places");
                for (Place place2 : places) {
                    String abbrev   = place2.getLowerCaseSuffix();
                    String longName = (String) statesMap.get(abbrev);
                    String cityName = place2.getLowerCaseName();
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
                                place2 = citiesMap.get(cityTok + suffix + ","
                                        + st);
                                if (place2 != null) {
                                    return place2;
                                }
                            }
                        }
                    }
                }

                if (state != null) {
                    resource = Place.getResource("cities");
                    place    = resource.getPlace(city + "," + state);
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
            //      if(_address.indexOf("arundel")<0) return null;
            resource = Place.getResource("counties");
            int index = _address.indexOf(",");
            if (index >= 0) {
                getStatesMap();
                List<String> toks   = StringUtil.splitUpTo(_address, ",", 2);
                String       county = toks.get(0);
                String       state  = toks.get(1);

                //              System.out.println("address:" + _address);
                place = resource.getPlace(county + "," + state);
                //              resource.debug();
                //              System.out.println("try:" +county+"," + state +": place:" + place);
                if (place == null) {
                    //              System.out.println("state before:" +county+":" + state+":");
                    state = (String) statesMap.get(state);
                    //              System.out.println("state after:" +county+"," + state);
                    if (state != null) {
                        place = resource.getPlace(county + "," + state);
                        //                      System.out.println("try 2:" +county+"," + state +" place:" + place);
                    }
                }
                if (place == null) {
                    //              System.err.println("No place:" + address);
                    //              System.exit(0);
                }

                return place;
            }
        }



        /*
        place = Place.getPlace(address);
        if (place == null) {
            int index = address.indexOf("-");
            if (index > 0) {
                String tmp = address.substring(0, index);
                place = Place.getPlace(tmp);
            }
        }
        if ((place == null) && (address.length() > 5)) {
            String tmp = address.substring(0, 5);
            place = Place.getPlace(tmp);
        }
        */

        if (place != null) {
            //            System.err.println("got place:" + address +" " + place.getLatitude()+" " + place.getLongitude());
            return place;
        }

        if (googleKey == null) {
            googleKey = GeoUtils.googleKey;
        }

        if (addressToLocation == null) {
            addressToLocation = new Hashtable<String, Place>();
            if (cacheDir != null) {
                File cacheFile = new File(IOUtil.joinDir(cacheDir,
                                     "addresslocations2.txt"));
                if (cacheFile.exists()) {
                    for (String line :
                            StringUtil.split(IOUtil.readContents(cacheFile),
                                             "\n", true, true)) {
                        List<String> toks = StringUtil.split(line,
                                                cacheDelimiter);
                        if (toks.size() == 4) {
                            addressToLocation.put(toks.get(0),
                                    new Place(toks.get(1),
                                        new Double(toks.get(2)),
                                        new Double(toks.get(3))));
                        }
                    }
                }
                FileWriter     fw = new FileWriter(cacheFile, true);
                BufferedWriter bw = new BufferedWriter(fw);
                cacheWriter = new PrintWriter(bw);
            }
        }


        place = addressToLocation.get(address);
        if (place != null) {
            if ((bounds == null)
                    || bounds.contains(place.getLatitude(),
                                       place.getLongitude())) {
                if (Double.isNaN(place.getLatitude())) {
                    return null;
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
        String name           = null;


        if (googleKey != null) {
            try {
                //                https://maps.googleapis.com/maps/api/geocode/json?address=Winnetka&bounds=34.172684,118.604794|34.236144,-118.500938&key=YOUR_API_KEY
                String url =
                    "https://maps.googleapis.com/maps/api/geocode/json?address="
                    + encodedAddress + "&key=" + googleKey;
                if (bounds != null) {
                    url += "&bounds=" + bounds.getSouth() + ","
                           + bounds.getWest() + "|" + bounds.getNorth() + ","
                           + bounds.getEast();
                }
                String result = IOUtil.readContents(url, GeoUtils.class);
                //                System.err.println("result:" + result);

                name = StringUtil.findPattern(result,
                        "\"formatted_address\"\\s*:\\s*\"([^\"]+)\"");
                latString = StringUtil.findPattern(result,
                        "\"lat\"\\s*:\\s*([-\\d\\.]+),");
                lonString = StringUtil.findPattern(result,
                        "\"lng\"\\s*:\\s*([-\\d\\.]+)\\s*");
            } catch (Exception exc) {
                System.err.println("exc:" + exc);
            }
        }
        if ((latString != null) && (lonString != null)) {
            place = new Place((name == null)
                              ? address
                              : name, new Double(latString),
                                      new Double(lonString));
            addressToLocation.put(address, place);
            if (cacheWriter != null) {
                cacheWriter.println(address + cacheDelimiter
                                    + place.getName() + cacheDelimiter
                                    + place.getLatitude() + cacheDelimiter
                                    + place.getLongitude());
                cacheWriter.flush();
            }

            return place;
        } else {
            if (cacheWriter != null) {
                cacheWriter.println(address + cacheDelimiter + Double.NaN
                                    + cacheDelimiter + Double.NaN);
                cacheWriter.flush();
            }

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


}
