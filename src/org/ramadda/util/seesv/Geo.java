/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util.seesv;

import org.json.*;
import org.ramadda.util.IO;
import org.ramadda.util.Utils;
import org.ramadda.util.geo.Address;
import org.ramadda.util.geo.Feature;
import org.ramadda.util.geo.GeoUtils;
import org.ramadda.util.geo.Place;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.io.*;
import java.net.URL;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

@SuppressWarnings("unchecked")
public abstract class Geo extends Processor {

    public Geo() {}

    public Geo(String col) {
        super(col);
    }

    public Geo(List<String> cols) {
        super(cols);
    }

    public static class Geocoder extends Geo {
        private HashSet seen = new HashSet();
        private boolean writeForDb = false;
        private int badCnt = 0;
        private int nameIndex;
        private int latIndex;
        private int lonIndex;
        private Hashtable<String, double[]> map;
        private boolean doneHeader = false;
        private boolean doAddress = false;
        private String prefix;
        private String suffix;
        private String latLabel = "Latitude";
        private String lonLabel = "Longitude";
        private boolean ifNeeded;
        private String sLatColumn;
        private String sLonColumn;

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

        public Geocoder(List<String> cols, String prefix, String suffix) {
            super(cols);
            this.prefix     = prefix;
            this.suffix     = suffix;
            this.writeForDb = false;
            doAddress       = true;
        }

        public Geocoder(List<String> cols, String prefix, String suffix,
                        String lat, String lon) {
            this(cols, prefix, suffix);
            ifNeeded   = true;
            sLatColumn = lat;
            sLonColumn = lon;
        }

        public Geocoder(List<String> cols, String prefix, String suffix,
                        boolean forDb) {
            super(cols);
            this.prefix     = prefix;
            this.suffix     = suffix;
            this.writeForDb = forDb;
            doAddress       = true;
        }

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

        @Override
        public Row processRow(TextReader ctx, Row row) {

            List values = row.getValues();
            if ( !doneHeader) {
                if (ifNeeded) {
                    latIndex = getIndex(ctx, sLatColumn);
                    lonIndex = getIndex(ctx, sLonColumn);
                } else if (writeForDb) {
                    add(ctx, row, "Location");
                } else {
                    add(ctx, row, latLabel, lonLabel);
                }
                doneHeader = true;

                return row;
            }

            if (ifNeeded) {
                String slat = row.getString(latIndex);
                String slon = row.getString(lonIndex);
                if (Utils.stringDefined(slat) && Utils.stringDefined(slon)) {
                    try {
                        if ( !Double.isNaN(Double.parseDouble(slat))
                                && !Double.isNaN(Double.parseDouble(slat))) {
                            //                      System.err.println("not needed:"+ slat +" " + slon);
                            return row;
                        }
                    } catch (Exception ignore) {}
                }
                //              System.err.println("needed:"+ slat +" " + slon);
            }

            List<Integer> indices = getIndices(ctx);
            StringBuilder key     = new StringBuilder();
            if ((prefix != null) && (prefix.length() > 0)) {
                key.append(prefix);
                key.append(" ");
            }
            boolean didOne = false;
            for (int i : indices) {
		if(!row.indexOk(i)) continue;
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
            if (didOne && key != null) {
                Place place = null;
                if (doAddress) {
                    place = GeoUtils.getLocationFromAddress(key.toString(),
                            ctx.getBounds());
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
            if (ifNeeded) {
                row.set(latIndex, lat);
                row.set(lonIndex, lon);
            } else if (writeForDb) {
                add(ctx, row, lat + ";" + lon);
            } else {
                add(ctx, row,  Double.valueOf(lat), Double.valueOf(lon));
            }

            return row;

        }

    }

    public static class StateNamer extends Geo {

        private int col = -1;

        public StateNamer(String col) {
            super(col);
        }

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

    public static class Elevation extends Geo {

        private int rowIdx = 0;

        private String lat;

        private String lon;

        private int latColumn = -1;

        private int lonColumn = -1;

        public Elevation(String lat, String lon) {
            super();
            this.lat = lat;
            this.lon = lon;
        }

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
		String url = "https://nationalmap.gov/epqs/pqs.php?x="
		    + lonValue + "&y=" + latValue
		    + "&units=feet&output=xml";
		String result =IO.readUrl(new URL(url));
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

    public static class LatLonToUtm extends Geo {

        private String lat;

        private String lon;

        private int latColumn = -1;

        private int lonColumn = -1;

        public LatLonToUtm(String lat, String lon) {
            super();
            this.lat = lat;
            this.lon = lon;
        }
	private Row addUndefined(Row row) {
	    row.add("");
	    row.add("NaN");		    
	    row.add("NaN");
	    return row;
	}

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                latColumn = getIndex(ctx, lat);
                lonColumn = getIndex(ctx, lon);
                row.add("UTM Zone");
                row.add("UTM Easting");		
                row.add("UTM Northing");
                return row;
            }
            try {
		if(!row.indexOk(latColumn) || !row.indexOk(lonColumn))
		    return addUndefined(row);
                double latValue =
                    Double.parseDouble(row.getString(latColumn));
                double lonValue =
                    Double.parseDouble(row.getString(lonColumn));
		if(!GeoUtils.latLonOk(latValue,lonValue)) {
		    return addUndefined(row);
		}
		GeoUtils.UTMInfo info = GeoUtils.LatLonToUTM(latValue, lonValue);
		if(info==null) {
		    return addUndefined(row);

		}
		row.add(info.getZone());
		row.add(""+info.getEasting());
		row.add(""+info.getNorthing());		
                return row;
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }
        }

    }

    public static class UtmToLatLon extends Geo {

        private String zone;
        private String easting;
        private String northing;

        private int zoneColumn = -1;
        private int eastingColumn = -1;
        private int northingColumn = -1;

        public UtmToLatLon(String zone, String easting, String northing) {
            super();
	    this.zone = zone;
            this.easting = easting;
            this.northing = northing;
        }
	private Row addUndefined(Row row) {
	    row.add("NaN");		    
	    row.add("NaN");
	    return row;
	}

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                zoneColumn = getIndex(ctx, zone);
                northingColumn = getIndex(ctx, northing);
                eastingColumn = getIndex(ctx, easting);
                row.add("Latitude");
                row.add("Longitude");		
                return row;
            }
            try {
		if(!row.indexOk(zoneColumn) ||
		   !row.indexOk(eastingColumn) || !row.indexOk(northingColumn))
		    return addUndefined(row);
		String zone = row.getString(zoneColumn);
                double eastingValue =  Seesv.parseDouble(row.getString(eastingColumn));
                double northingValue = Seesv.parseDouble(row.getString(northingColumn));
		double[]latlon =  GeoUtils.UTMToLatLon(zone,eastingValue,northingValue);
		if(latlon==null) {
		    return addUndefined(row);

		}
		row.add(""+latlon[0]);
		row.add(""+latlon[1]);		
                return row;
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }
        }

    }





    public static class Neighborhood extends Geo {

        private int rowIdx = 0;

        private String lat;

        private String lon;

        private int latColumn = -1;

        private int lonColumn = -1;

	private String dflt;

        public Neighborhood(String lat, String lon, String dflt) {
            super();
            this.lat = lat;
            this.lon = lon;
	    this.dflt = dflt;
        }

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
                    Seesv.parseDouble(row.getString(latColumn));
                double lonValue =
                    Seesv.parseDouble(row.getString(lonColumn));
                String result = GeoUtils.getNeighborhood(latValue, lonValue);
                if (result == null) {
                    result = dflt;
                }
                row.add(result);

                return row;
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }
        }

    }

    public static class GeoNamer extends Geo {

        private int rowIdx = 0;

        private String where;

        private String lat;

        private String lon;

        private int latColumn = -1;

        private int lonColumn = -1;

        private List<String> fields;

        public GeoNamer(String where, String what, String lat, String lon) {
            super();
            this.fields = Utils.split(what, ",", true, true);
            this.where  = where;
            this.lat    = lat;
            this.lon    = lon;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowIdx++ == 0) {
                latColumn = getIndex(ctx, lat);
                lonColumn = getIndex(ctx, lon);
                if (fields.size() > 0) {
                    for (String f : fields) {
                        row.add(f);
                    }
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
                String slat = row.getString(latColumn).trim();
                String slon = row.getString(lonColumn).trim();
                if ((slat.length() == 0) || (slon.length() == 0)) {
                    for (String f : fields) {
                        row.add("");
                    }

                    return row;
                }
                double latValue = Double.parseDouble(slat);
                double lonValue = Double.parseDouble(slon);

                List<Object> vs = GeoUtils.findFeatureFields(where, fields,
                                      latValue, lonValue);
                if (vs == null) {
                    for (String f : fields) {
                        row.add("");
                    }
                } else {
                    for (Object o : vs) {
                        if (o == null) {
                            o = "";
                        }
                        row.add(o.toString().trim());
                    }
                }

                return row;
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }
        }

    }

    public static class GeoContains extends Geo {

        private String file;

        private String name;

        private String lat;

        private String lon;

        private int latColumn = -1;

        private int lonColumn = -1;

        public GeoContains(String file, String name, String lat, String lon) {
            super();
            this.file = file;
            this.name = name;
            this.lat  = lat;
            this.lon  = lon;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                latColumn = getIndex(ctx, lat);
                lonColumn = getIndex(ctx, lon);
                row.add(name);

                return row;
            }
            try {
                //              System.err.println(latColumn +" " + lonColumn);         System.err.println(row);
                String slat = row.getString(latColumn).trim();
                String slon = row.getString(lonColumn).trim();
                if ((slat.length() == 0) || (slon.length() == 0)) {
                    row.add("false");

                    return row;
                }
                double latValue = Double.parseDouble(slat);
                double lonValue = Double.parseDouble(slon);

                if (GeoUtils.findFeature(file, latValue, lonValue) != null) {
                    row.add("true");
                } else {
                    row.add("false");
                }

                return row;
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }
        }

    }

    public static class DecodeLatLon extends Geo {

        public DecodeLatLon(List<String> cols) {
            super(cols);
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                return row;
            }
            for (int idx : getIndices(ctx)) {
                String slat = row.getString(idx).trim();
                row.set(idx, "" + Misc.decodeLatLon(slat));
            }

            return row;
        }
    }

    public static class GetAddress extends Geo {

        private String lat;

        private String lon;

        private int latColumn;

        private int lonColumn;

        public GetAddress(String lat, String lon) {
            this.lat = lat;
            this.lon = lon;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                latColumn = getIndex(ctx, lat);
                lonColumn = getIndex(ctx, lon);
                row.add("address", "city", "county", "state", "zip",
                        "country");

                return row;
            }
            try {
                String slat = row.getString(latColumn).trim();
                String slon = row.getString(lonColumn).trim();
                if ((slat.length() == 0) || (slon.length() == 0)) {
                    row.add("", "", "", "", "", "");

                    return row;
                }
                double latValue = Double.parseDouble(slat);
                double lonValue = Double.parseDouble(slon);
                Address address = GeoUtils.getAddress(latValue,
                                      lonValue);
                if (address != null) {
                    int idx = 0;
                    row.add(address.getAddress(), address.getCity(),
                            address.getCounty(), address.getState(),
                            address.getPostalCode(), address.getCountry());
                } else {
                    row.add("", "", "", "", "", "");
                }

                return row;
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }
        }

    }

    public static class Populator extends Geo {

        /* */

        private HashSet seen = new HashSet();

        private int badCnt = 0;

        /* */

        private int nameIndex;

        /* */

        private int latIndex;

        /* */

        private int lonIndex;

        /* */

        private Hashtable<String, double[]> map;

        /* */

        private boolean doneHeader = false;

        private String prefix;

        /* */

        private String suffix;

        public Populator(List<String> cols, String prefix, String suffix) {
            super(cols);
            this.prefix = prefix;
            this.suffix = suffix;
        }

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
                    add(ctx, row,  Integer.valueOf(327000000));

                    return row;
                }
                key.append(value);
            }

            if ((suffix != null) && (suffix.length() > 0)) {
                key.append(" ");
                key.append(suffix);
            }

            Place place = GeoUtils.getLocationFromAddress(key.toString(),
                              null);
            if (place != null) {
                add(ctx, row, Integer.valueOf(place.getPopulation()));
            } else {
                //              System.out.println("NOT:" + key);
                add(ctx, row, Integer.valueOf(0));
            }

            //      System.err.println("pop row:" + row);
            return row;
        }

    }

    public static class InBounds extends Filter {

	String latCol;
	String lonCol;

	int latIdx;
	int lonIdx;
	double north;
	double west;
	double south;
	double east;
        public InBounds(String slat,String slon,
			double north,double west,double south, double east){
            super();
	    this.latCol=slat;
	    this.lonCol=slon;
	    if(!Utils.stringDefined(slat)) this.latCol = "latitude";
	    if(!Utils.stringDefined(slon)) this.lonCol = "longitude";	       
	    this.north=north;
	    this.west = west;
	    this.south = south;
	    this.east = east;
        }

        @Override
        public boolean rowOk(TextReader ctx, Row row) {
            if (cnt++ == 0) {
		latIdx = getIndex(ctx,latCol);
		lonIdx = getIndex(ctx,lonCol);		    
                return true;
            }
	    double lat= Seesv.parseDouble(row.getString(latIdx));
	    double lon= Seesv.parseDouble(row.getString(lonIdx));	    
	    if(Double.isNaN(lat) || Double.isNaN(lon)) return false;
	    if(!Double.isNaN(north) && lat>north) return false;
	    if(!Double.isNaN(south) && lat<south) return false;
	    if(!Double.isNaN(west) && lon<west) return false;	    	        
	    if(!Double.isNaN(east) && lon>east) return false;	    	    
            return true;
        }

    }

    public static class Regionator extends Geo {

        private boolean doneHeader = false;

        private Properties props;

        public Regionator(List<String> cols) {
            super(cols);
            props = new Properties();
            try {
                InputStream inputStream =
                    Utils.getInputStream(
                        "/org/ramadda/util/seesvstate_regions.properties",
                        getClass());
                props.load(inputStream);
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }
        }

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
