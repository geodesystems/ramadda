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
import ucar.unidata.util.StringUtil;

import ucar.unidata.xml.XmlUtil;


import java.io.*;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;
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
                double[] loc = getLocationFromAddress(arg, key);
                if (loc == null) {
                    System.out.println(arg + ": NA");
                } else {
                    System.out.println(arg + ": " + loc[0] + "," + loc[1]);
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
    private static Hashtable<String, double[]> addressToLocation = null;




    /**
     * Look up the location of the given address
     *
     * @param address The address
     *
     * @return The location or null if not found
     */
    public static double[] getLocationFromAddress(String address) {
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
    public static double[] getLocationFromAddress(String address,
            String googleKey) {
        try {
            return getLocationFromAddressInner(address, googleKey);

        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    /**
     * _more_
     *
     * @param address _more_
     * @param googleKey _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static double[] getLocationFromAddressInner(String address,
            String googleKey)
            throws Exception {

        if (address == null) {
            return null;
        }
        address = address.trim();
        if (address.length() == 0) {
            return null;
        }
        if(address.toLowerCase().startsWith("from:")) address= address.substring(5);
        else if(address.toLowerCase().startsWith("to:")) address= address.substring(3);
        Place place = Place.getPlace(address);
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

        if (place != null) {
            //            System.err.println("got place:" + address +" " + place.getLatitude()+" " + place.getLongitude());
            return new double[] { place.getLatitude(), place.getLongitude() };
        }

        if (googleKey == null) {
            googleKey = GeoUtils.googleKey;
        }

        if (addressToLocation == null) {
            addressToLocation = new Hashtable<String, double[]>();
            if (cacheDir != null) {
                File cacheFile = new File(IOUtil.joinDir(cacheDir,
                                     "addresslocations.txt"));
                if (cacheFile.exists()) {
                    for (String line :
                            StringUtil.split(IOUtil.readContents(cacheFile),
                                             "\n", true, true)) {
                        List<String> toks = StringUtil.split(line,
                                                cacheDelimiter);
                        if (toks.size() == 3) {
                            addressToLocation.put(toks.get(0),
                                    new double[] { new Double(toks.get(1)),
                                    new Double(toks.get(2)) });
                        }
                    }
                }
                FileWriter     fw = new FileWriter(cacheFile, true);
                BufferedWriter bw = new BufferedWriter(fw);
                cacheWriter = new PrintWriter(bw);
            }
        }



        double[] location = addressToLocation.get(address);
        if (location != null) {
            if (Double.isNaN(location[0])) {
                return null;
            }

            return location;
        }




        String latString      = null;
        String lonString      = null;
        String encodedAddress = StringUtil.replace(address, " ", "%20");



        if (googleKey != null) {
            try {
                String url =
                    "https://maps.googleapis.com/maps/api/geocode/json?address="
                    + encodedAddress + "&key=" + googleKey;
                String result = IOUtil.readContents(url, GeoUtils.class);
                //                System.err.println("url:" + url);
                ///                System.err.println("result:" + result);

                //                    "lng" : -105.226021
                latString = StringUtil.findPattern(result,
                        "\"lat\"\\s*:\\s*([-\\d\\.]+),");
                lonString = StringUtil.findPattern(result,
                        "\"lng\"\\s*:\\s*([-\\d\\.]+)\\s*");
                //                System.err.println(result);
                System.err.println("address:" + address + " loc:" + latString
                                   + " " + lonString);
            } catch (Exception exc) {
                System.err.println("exc:" + exc);
            }
        }
        /*
     try {
         String url = "http://gws2.maps.yahoo.com/findlocation?q="
                      + encodedAddress;
         System.err.println("yahoo:" + url);
         String  result  = IOUtil.readContents(url, GeoUtils.class);
         System.err.println("yahoo:" + result);
         Element root    = XmlUtil.getRoot(result);
         Element latNode = XmlUtil.findDescendant(root, "latitude");
         Element lonNode = XmlUtil.findDescendant(root, "longitude");
         if ((latNode != null) && (lonNode != null)) {
             latString = XmlUtil.getChildText(latNode);
             lonString = XmlUtil.getChildText(lonNode);
         }
     } catch (Exception exc) {
         System.err.println("exc:" + exc);
     }

         */


        if ((latString != null) && (lonString != null)) {
            location = new double[] { Double.parseDouble(latString),
                                      Double.parseDouble(lonString) };
            addressToLocation.put(address, location);
            if (cacheWriter != null) {
                cacheWriter.println(address + cacheDelimiter + location[0]
                                    + cacheDelimiter + location[1]);
                cacheWriter.flush();
            }

            return location;
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



}
