/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util.text;


import org.apache.commons.codec.language.Soundex;


import org.json.*;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.PatternProps;


import org.ramadda.util.IO;
import org.ramadda.util.Json;
import org.ramadda.util.Utils;

import org.ramadda.util.geo.Address;
import org.ramadda.util.geo.Feature;
import org.ramadda.util.geo.GeoUtils;
import org.ramadda.util.geo.Place;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.io.*;
import java.util.Collections;
import java.util.Comparator;

import java.net.URL;

import java.security.MessageDigest;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;

import java.util.regex.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.*;


/**
 * Class description
 *
 *
 * @version        $version$, Fri, Jan 9, '15
 * @author         Jeff McWhirter
 */
@SuppressWarnings("unchecked")
public abstract class Geo extends Processor {


    /**
     *
     */
    public Geo() {}

    /**
     * @param col _more_
     */
    public Geo(String col) {
        super(col);
    }

    /**
     *
     * @param cols _more_
     */
    public Geo(List<String> cols) {
        super(cols);
    }



    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Jan 16, '15
     * @author         Enter your name here...
     */
    public static class Geocoder extends Geo {

        /* */

        /** _more_ */
        private HashSet seen = new HashSet();

        /* */

        /** _more_ */
        private boolean writeForDb = false;

        /* */

        /** _more_ */
        private int badCnt = 0;

        /* */

        /** _more_ */
        private int nameIndex;

        /* */

        /** _more_ */
        private int latIndex;

        /* */

        /** _more_ */
        private int lonIndex;

        /* */

        /** _more_ */
        private Hashtable<String, double[]> map;

        /* */

        /** _more_ */
        private boolean doneHeader = false;

        /* */

        /** _more_ */
        private boolean doAddress = false;

        /* */

        /** _more_ */
        private String prefix;

        /* */

        /** _more_ */
        private String suffix;

        /* */

        /** _more_ */
        private String latLabel = "Latitude";

        /* */

        /** _more_ */
        private String lonLabel = "Longitude";

        /**
         * @param col _more_
         * @param mapFile _more_
         * @param nameIndex _more_
         * @param latIndex _more_
         * @param lonIndex _more_
         * @param writeForDb _more_
         */
        public Geocoder(String col, String mapFile, int nameIndex,
                        int latIndex, int lonIndex, boolean writeForDb) {

            super(col);
            this.nameIndex  = nameIndex;
            this.latIndex   = latIndex;
            this.lonIndex   = lonIndex;
            this.writeForDb = writeForDb;
            try {
                this.map = makeMap(mapFile);
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }
        }



        /**
         * @param cols _more_
         * @param prefix _more_
         * @param suffix _more_
         */
        public Geocoder(List<String> cols, String prefix, String suffix) {

            super(cols);
            this.prefix     = prefix;
            this.suffix     = suffix;
            this.writeForDb = false;
            doAddress       = true;
        }

        /**
         * @param cols _more_
         * @param prefix _more_
         * @param suffix _more_
         * @param forDb _more_
         */
        public Geocoder(List<String> cols, String prefix, String suffix,
                        boolean forDb) {
            super(cols);
            this.prefix     = prefix;
            this.suffix     = suffix;
            this.writeForDb = forDb;
            doAddress       = true;
        }

        /**
         * @param filename _more_
         * @return _more_
         * @throws Exception _more_
         */
        private Hashtable<String, double[]> makeMap(String filename)
                throws Exception {
            Hashtable<String, double[]> map = new Hashtable<String,
                                                  double[]>();
            long t1 = System.currentTimeMillis();
            //            System.err.println("Reading file:" + filename);
            BufferedReader br = new BufferedReader(
                                    new InputStreamReader(
                                        getInputStream(filename)));
            //            System.err.println("Done Reading file:" + filename);
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.length() == 0) {
                    continue;
                }
                if (line.startsWith("#")) {
                    continue;
                }
                List<String> toks = Utils.split(line, ",");
                String       key  = toks.get(nameIndex).trim();
                try {
                    double[] v = new double[2];
                    v[0] = Double.parseDouble(toks.get(latIndex).trim());
                    v[1] = Double.parseDouble(toks.get(lonIndex).trim());
                    //                    map.put(key, GeoUtils.getBounds(v));
                    map.put(key, v);
                } catch (Exception exc) {
                    //                    exc.printStackTrace();
                }
            }
            long t2 = System.currentTimeMillis();

            //            Utils.printTimes("map time:",t1,t2);
            return map;
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            List values = row.getValues();
            if ( !doneHeader) {
                if (writeForDb) {
                    add(ctx, row, "Location");
                } else {
                    add(ctx, row, latLabel, lonLabel);
                }
                doneHeader = true;

                return row;
            }

            List<Integer> indices = getIndices(ctx);
            StringBuilder key     = new StringBuilder();
            if ((prefix != null) && (prefix.length() > 0)) {
                key.append(prefix);
                key.append(" ");
            }
            boolean didOne = false;
            for (int i : indices) {
                Object value = values.get(i);
                if (didOne) {
                    key.append(", ");
                }
                didOne = true;
                key.append(value);
            }

            if ((suffix != null) && (suffix.length() > 0)) {
                key.append(" ");
                key.append(suffix);
            }

            double lat = Double.NaN;
            double lon = Double.NaN;
            if (key != null) {
                Place place = null;


                if (doAddress) {
                    place = GeoUtils.getLocationFromAddress(key.toString());
                } else {
                    String   tok    = key.toString();
                    double[] bounds = map.get(tok);
                    if (bounds == null) {
                        bounds = map.get(tok.replaceAll("-.*$", ""));
                    }
                    if (bounds == null) {
                        List<String> toks = Utils.splitUpTo(tok, ",", 2);
                        if (toks.size() > 0) {
                            bounds = map.get(toks.get(0));
                        }
                    }
                    if (bounds == null) {
                        List<String> toks = Utils.splitUpTo(tok, " ", 2);
                        if (toks.size() > 0) {
                            bounds = map.get(toks.get(0));
                        }
                    }
                    if (bounds == null) {
                        if (key.toString().length() > 0) {
                            badCnt++;
                            if ( !seen.contains(key)) {
                                System.err.println("No bounds:" + key + " "
                                        + badCnt);
                                seen.add(key);
                            }
                        }
                    }
                    if (bounds != null) {
                        place = new Place("", bounds[0], bounds[1]);
                    }
                }
                if (place != null) {
                    lat = place.getLatitude();
                    lon = place.getLongitude();
                }
            }
            if (writeForDb) {
                add(ctx, row, lat + ";" + lon);
            } else {
                add(ctx, row, new Double(lat), new Double(lon));
            }

            return row;
        }

    }




    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Jan 16, '15
     * @author         Enter your name here...
     */
    public static class StateNamer extends Geo {


        /** _more_ */
        private int col = -1;

        /**
         * @param col _more_
         */
        public StateNamer(String col) {
            super(col);
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (col < 0) {
                List<Integer> indices = getIndices(ctx);
                col = indices.get(0);
                row.add("State");

                return row;
            }
            try {
                String id = (String) row.get(col);
                Object o  = GeoUtils.getStatesMap().get(id.toLowerCase());
                row.add((o == null)
                        ? id
                        : o.toString());

                return row;
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }
        }

    }





    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Jan 16, '15
     * @author         Enter your name here...
     */
    public static class Elevation extends Geo {


        /** _more_ */
        private int rowIdx = 0;

        /** _more_ */
        private String lat;

        /** _more_ */
        private String lon;


        /** _more_ */
        private int latColumn = -1;

        /** _more_ */
        private int lonColumn = -1;


        /**
         * @param lat _more_
         * @param lon _more_
         */
        public Elevation(String lat, String lon) {
            super();
            this.lat = lat;
            this.lon = lon;
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowIdx++ == 0) {
                latColumn = getIndex(ctx, lat);
                lonColumn = getIndex(ctx, lon);
                row.add("Elevation");

                return row;
            }
            try {
                double latValue =
                    Double.parseDouble(row.getString(latColumn));
                double lonValue =
                    Double.parseDouble(row.getString(lonColumn));
                String result =
                    IO.readUrl(
                        new URL(
                            "https://nationalmap.gov/epqs/pqs.php?x="
                            + lonValue + "&y=" + latValue
                            + "&units=feet&output=xml"));
                String elev = StringUtil.findPattern(result,
                                  "<Elevation>([^<]+)</Elevation>");
                if (elev != null) {
                    row.add(elev);
                } else {
                    row.add("NaN");
                }

                return row;
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }
        }

    }


    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Nov 4, '21
     * @author         Enter your name here...
     */
    public static class Neighborhood extends Geo {


        /** _more_ */
        private int rowIdx = 0;

        /** _more_ */
        private String lat;

        /** _more_ */
        private String lon;


        /** _more_ */
        private int latColumn = -1;

        /** _more_ */
        private int lonColumn = -1;


        /**
         * @param lat _more_
         * @param lon _more_
         */
        public Neighborhood(String lat, String lon) {
            super();
            this.lat = lat;
            this.lon = lon;
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowIdx++ == 0) {
                latColumn = getIndex(ctx, lat);
                lonColumn = getIndex(ctx, lon);
                row.add("Neighborhood");

                return row;
            }
            try {
                double latValue =
                    Double.parseDouble(row.getString(latColumn));
                double lonValue =
                    Double.parseDouble(row.getString(lonColumn));
                String result = GeoUtils.getNeighborhood(latValue, lonValue);
                if (result == null) {
                    result = "";
                }
                row.add(result);

                return row;
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }
        }

    }


    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Jan 16, '15
     * @author         Enter your name here...
     */
    public static class GeoNamer extends Geo {


        /** _more_ */
        private int rowIdx = 0;

        /** _more_ */
        private String where;

        /** _more_ */
        private String lat;

        /** _more_ */
        private String lon;


        /** _more_ */
        private int latColumn = -1;

        /** _more_ */
        private int lonColumn = -1;


	private List<String> fields;

        /**
         * @param where _more_
         * @param lat _more_
         * @param lon _more_
         */
        public GeoNamer(String where, String what,String lat, String lon) {
            super();
	    this.fields = Utils.split(what,",",true,true);
            this.where = where;
            this.lat   = lat;
            this.lon   = lon;
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowIdx++ == 0) {
                latColumn = getIndex(ctx, lat);
                lonColumn = getIndex(ctx, lon);
		if(fields.size()>0) {
		    for(String f: fields) row.add(f);
		} else {
		    String label = where.equals("counties")
			? "County"
			: where.equals("states")
			? "State"
			: where.equals("timezones")
			? "Timezone"
			: where;
		    row.add(label);
		}
                return row;
            }
            try {
		String slat =row.getString(latColumn).trim();
		String slon =row.getString(lonColumn).trim();		
		if(slat.length()==0 || slon.length()==0) {
		    for(String f: fields) row.add("");
		    return row;
		}
                double latValue =
                    Double.parseDouble(slat);
                double lonValue =
                    Double.parseDouble(slon);
		
		List<Object> vs =  GeoUtils.findFeatureFields(where, fields,latValue,
                                  lonValue);
		if(vs==null) {
		    for(String f: fields) row.add("");
		} else {
		    for(Object o:vs) {
			if(o==null) o="";
			row.add(o.toString().trim());
		    }
		}
		return row;
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }
        }

    }



    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Jan 16, '15
     * @author         Enter your name here...
     */
    public static class GeoContains extends Geo {


	private String file;
	
        /** _more_ */
        private String name;

        /** _more_ */
        private String lat;

        /** _more_ */
        private String lon;


        /** _more_ */
        private int latColumn = -1;

        /** _more_ */
        private int lonColumn = -1;



        /**
         * @param file _more_
         * @param lat _more_
         * @param lon _more_
         */
        public GeoContains(String file, String name,String lat, String lon) {
            super();
            this.file = file;
            this.name = name;	    
            this.lat   = lat;
            this.lon   = lon;
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                latColumn = getIndex(ctx, lat);
                lonColumn = getIndex(ctx, lon);
		row.add(name);
                return row;
            }
            try {
		//		System.err.println(latColumn +" " + lonColumn);		System.err.println(row);
		String slat =row.getString(latColumn).trim();
		String slon =row.getString(lonColumn).trim();		
		if(slat.length()==0 || slon.length()==0) {
		    row.add("false");
		    return row;
		}
                double latValue =
                    Double.parseDouble(slat);
                double lonValue =
                    Double.parseDouble(slon);
		
		if(GeoUtils.findFeature(file, latValue, lonValue)!=null)
		    row.add("true");
		else
		    row.add("false");
		return row;
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }
        }

    }

    
    public static class GetAddress extends Geo {


	private String lat;
	private String lon;	
	
	private int latColumn;
	private int lonColumn;



        /**
         * @param file _more_
         * @param lat _more_
         * @param lon _more_
         */
        public GetAddress(String lat, String lon) {
	    this.lat =lat;
	    this.lon = lon;
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                latColumn = getIndex(ctx, lat);
                lonColumn = getIndex(ctx, lon);
		row.add("address", "city", "county","state", "zip", "country");
                return row;
            }
	    try {
		String slat =row.getString(latColumn).trim();
		String slon =row.getString(lonColumn).trim();		
		if(slat.length()==0 || slon.length()==0) {
		    row.add("","","","","","");
		    return row;
		}
                double latValue =
                    Double.parseDouble(slat);
                double lonValue =
                    Double.parseDouble(slon);
		Address address = GeoUtils.getAddressFromLatLon(latValue,lonValue);
		if(address!=null) {
		    int idx =0;
		    row.add(address.getAddress(),address.getCity(),
			    address.getCounty(),
			    address.getState(), 
			    address.getPostalCode(),
			    address.getCountry());
		} else {
		    row.add("","","","","","");
		}
		return row;
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }
        }

    }




    /**
     * Class description
     *
     *
     * @version        $version$, Sat, Mar 14, '20
     * @author         Enter your name here...
     */
    public static class Populator extends Geo {

        /* */

        /** _more_ */
        private HashSet seen = new HashSet();

        /** _more_ */
        private int badCnt = 0;

        /* */

        /** _more_ */
        private int nameIndex;

        /* */

        /** _more_ */
        private int latIndex;

        /* */

        /** _more_ */
        private int lonIndex;

        /* */

        /** _more_ */
        private Hashtable<String, double[]> map;

        /* */

        /** _more_ */
        private boolean doneHeader = false;

        /** _more_ */
        private String prefix;

        /* */

        /** _more_ */
        private String suffix;

        /**
         * _more_
         * @param cols _more_
         * @param prefix _more_
         * @param suffix _more_
         */
        public Populator(List<String> cols, String prefix, String suffix) {
            super(cols);
            this.prefix = prefix;
            this.suffix = suffix;
        }



        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            List values = row.getValues();
            if ( !doneHeader) {
                add(ctx, row, "Population");
                doneHeader = true;

                //              System.err.println("pop row:" + row);
                return row;
            }

            List<Integer> indices = getIndices(ctx);
            StringBuilder key     = new StringBuilder();
            if ((prefix != null) && (prefix.length() > 0)) {
                key.append(prefix);
                key.append(" ");
            }
            boolean didOne = false;
            for (int i : indices) {
                Object value = values.get(i);
                if (didOne) {
                    key.append(", ");
                }
                didOne = true;
                //A hack for US
                if (value.equals("US")
                        || value.toString().startsWith("United States")) {
                    add(ctx, row, new Integer(327000000));

                    return row;
                }
                key.append(value);
            }

            if ((suffix != null) && (suffix.length() > 0)) {
                key.append(" ");
                key.append(suffix);
            }


            Place place = GeoUtils.getLocationFromAddress(key.toString());
            if (place != null) {
                add(ctx, row, new Integer(place.getPopulation()));
            } else {
                //              System.out.println("NOT:" + key);
                add(ctx, row, new Integer(0));
            }

            //      System.err.println("pop row:" + row);
            return row;
        }

    }



    /**
     * Class description
     *
     *
     * @version        $version$, Sat, Mar 14, '20
     * @author         Enter your name here...
     */
    public static class Regionator extends Geo {

        /** _more_ */
        private boolean doneHeader = false;

        /** _more_ */
        private Properties props;


        /**
         * _more_
         * @param cols _more_
         */
        public Regionator(List<String> cols) {
            super(cols);
            props = new Properties();
            try {
                InputStream inputStream =
                    Utils.getInputStream(
                        "/org/ramadda/util/text/state_regions.properties",
                        getClass());
                props.load(inputStream);
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }
        }


        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if ( !doneHeader) {
                row.add("Region");
                doneHeader = true;

                return row;
            }
            List<Integer> indices = getIndices(ctx);
            StringBuilder keyb    = new StringBuilder();
            boolean       didOne  = false;
            //Really only need one
            for (int i : indices) {
                Object value = row.get(i);
                if (didOne) {
                    keyb.append(" ");
                }
                didOne = true;
                keyb.append(value);
            }
            String key    = keyb.toString();
            String region = (String) props.get(key);
            if (region == null) {
                region = (String) props.get(key.toUpperCase());
            }
            if (region == null) {
                region = (String) props.get(key.toLowerCase());
            }
            if (region == null) {
                region = "NA";
            }
            row.add(region);

            return row;
        }

    }





}
