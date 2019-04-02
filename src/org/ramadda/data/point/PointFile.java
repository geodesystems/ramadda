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

package org.ramadda.data.point;



import org.ramadda.data.record.*;
import org.ramadda.data.record.filter.*;
import org.ramadda.util.GeoUtils;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Station;
import org.ramadda.util.Utils;

import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.*;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;


import ucar.unidata.util.StringUtil;


import java.awt.geom.*;

import java.io.*;


import java.text.SimpleDateFormat;

import java.util.ArrayList;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;
import java.util.TimeZone;




/**
 */
public abstract class PointFile extends RecordFile implements Cloneable,
        Fields {

    /** _more_ */
    public static final String DFLT_PROPERTIES_FILE = "point.properties";

    /** _more_ */
    public static final String ACTION_TIME = "action.time";

    /** _more_ */
    public static final String ACTION_GRID = "action.grid";

    /** _more_ */
    public static final String ACTION_DECIMATE = "action.decimate";

    /** _more_ */
    public static final String ACTION_TRACKS = "action.tracks";

    /** _more_ */
    public static final String ACTION_WAVEFORM = "action.waveform";

    /** _more_ */
    public static final String ACTION_TRAJECTORY = "action.trajectory";

    /** _more_ */
    public static final String ACTION_BOUNDINGPOLYGON =
        "action.bounding_polygon";

    /** _more_ */
    public static final String ACTION_MAPINCHART = "action.map_in_chart";

    /** _more_ */
    public static final String ACTION_AREAL_COVERAGE =
        "action.areal_coverage";


    /** _more_ */
    private static final org.ramadda.data.point.LatLonPointRecord dummyField1 =
        null;



    /** _more_ */
    public static final String CRS_GEOGRAPHIC = "geographic";

    /** _more_ */
    public static final String CRS_UTM = "utm";

    /** _more_ */
    public static final String CRS_EPSG = "epsg:";

    /** _more_ */
    public static final String CRS_WGS84 = "wgs84";

    /** _more_ */
    public static final String CRS_ECEF = "ecef";

    /** _more_ */
    public static final String PROP_CRS = "crs";

    /** _more_ */
    public static final String PROP_DESCRIPTION = "description";

    /** _more_ */
    public static final String PROP_UTM_ZONE = "utm.zone";

    /** _more_ */
    public static final String PROP_UTM_NORTH = "utm.north";

    /** _more_ */
    public static final int IDX_LAT = 0;

    /** _more_ */
    public static final int IDX_LON = 1;

    /** _more_ */
    public static final int IDX_ALT = 2;


    /** _more_ */
    private String crs = CRS_GEOGRAPHIC;


    /** _more_ */
    boolean isGeographic = true;

    /** _more_ */
    boolean isUtm = false;

    /** _more_ */
    boolean isWgs84 = false;


    /** _more_ */
    private Projection projection;

    /** _more_ */
    private com.jhlabs.map.proj.Projection jhProjection;


    /** _more_ */
    private String description = "";

    /** _more_ */
    private double lat = Double.NaN;

    /** _more_ */
    private double lon = Double.NaN;

    /** _more_ */
    private double elevation = Double.NaN;



    /**
     * _more_
     */
    public PointFile() {}




    /**
     * _more_
     *
     * @param properties _more_
     */
    public PointFile(Hashtable properties) {
        super(properties);
    }


    /**
     * ctor
     *
     *
     * @param filename point data file
     *
     *
     * @throws IOException _more_
     */
    public PointFile(String filename) throws IOException {
        super(filename);
    }


    /**
     * ctor
     *
     *
     * @param filename point data file
     * @param properties _more_
     *
     * @throws IOException _more_
     */
    public PointFile(String filename, Hashtable properties)
            throws IOException {
        super(filename, properties);
    }



    public PointFile(String filename, RecordFileContext context, Hashtable properties) {
        super(filename, context, properties);
    }


    /**
     * _more_
     *
     * @param props _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void initClassProperties(Hashtable<String, String> props)
            throws Exception {
        super.initClassProperties(props);
        props.putAll(
            Utils.getProperties(
                IOUtil.readContents(
                    "/org/ramadda/data/point/PointFile.properties", "")));
    }


    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public String getPropertiesFileName() {
        return DFLT_PROPERTIES_FILE;
    }




    /**
     * _more_
     *
     * @param action _more_
     *
     * @return _more_
     */
    public boolean isCapable(String action) {
        if (action.equals(ACTION_BOUNDINGPOLYGON)) {
            return true;
        }
        if (action.equals(ACTION_WAVEFORM)) {
            return hasWaveform();
        }

        return super.isCapable(action);
    }

    /**
     * _more_
     *
     * @param that _more_
     *
     * @return _more_
     */
    public boolean sameDataType(PointFile that) {
        return getClass().equals(that.getClass());
    }


    /**
     * _more_
     *
     * @param properties _more_
     */
    public void setProperties(Hashtable properties) {
        super.setProperties(properties);
        initProperties();
    }

    /** _more_ */
    static int printCnt = 0;


    /**
     * _more_
     */
    protected void initProperties() {
        //        System.err.println ("PointFile.initProperties:" + getProperties());
        description  = getProperty(PROP_DESCRIPTION, description);
        crs          = getProperty(PROP_CRS, crs);
        isGeographic = crs.equals(CRS_GEOGRAPHIC);
        isUtm        = crs.equals(CRS_UTM);
        isWgs84      = crs.equals(CRS_WGS84) || crs.equals(CRS_ECEF);

        //        crs =  "epsg:32611";
        if (crs.startsWith("epsg:")) {
            jhProjection =
                com.jhlabs.map.proj.ProjectionFactory
                    .getNamedPROJ4CoordinateSystem(crs.substring(5).trim());
        }

        //        String parameters = getProperty( PROP_PARAMETERS,null);
        //        if(parameters==null) {
        //            throw new IllegalArgumentException("No parameters given in file " + propertiesFile);
        //        }
        if (isUtm) {
            String zoneString = getProperty(PROP_UTM_ZONE, null);
            if (zoneString == null) {
                throw new IllegalArgumentException("No " + PROP_UTM_ZONE
                        + " property given");
            }
            boolean isNorth = getProperty(PROP_UTM_NORTH,
                                          "true").trim().equals("true");
            //+proj=utm +zone=11 +south +ellps=WGS72 +units=m +no_defs

            projection = new UtmProjection(Integer.parseInt(zoneString),
                                           isNorth);

        } else {
            //            System.err.println("Unknown crs:" + crs);
        }
    }


    /** _more_ */
    static int cnt = 0;

    /**
     * _more_
     *
     * @param lat _more_
     * @param lon _more_
     * @param elevation _more_
     */
    public void setLocation(double lat, double lon, double elevation) {
        this.lat       = lat;
        this.lon       = lon;
        this.elevation = elevation;
    }



    /**
     * _more_
     *
     * @param pointRecord _more_
     * @param y _more_
     * @param x _more_
     * @param z _more_
     * @param work _more_
     *
     * @return _more_
     */
    public double[] getLatLonAlt(PointRecord pointRecord, double y, double x,
                                 double z, double[] work) {
        if (work == null) {
            work = new double[3];
        }
        work[0] = y;
        work[1] = x;
        work[2] = z;
        if (isGeographic) {
            //Do nothing
        } else if (jhProjection != null) {
            //TODO: keep src and dst around as class members?
            Point2D.Double src = new Point2D.Double(x, y);
            Point2D.Double dst = new Point2D.Double(0, 0);
            dst           = jhProjection.inverseTransform(src, dst);
            work[IDX_LON] = dst.getX();
            work[IDX_LAT] = dst.getY();
            /*
            if(printCnt==0) {
                System.out.println("x,y,lon,lat");
            }
            if(printCnt++<100) {
                System.out.println("" + x +", " +  y +", " + dst.getX() +", " + dst.getY());
            }
            */
        } else if (isUtm) {
            ProjectionPointImpl ppi  = pointRecord.getFromPoint();
            LatLonPointImpl     llpi = pointRecord.getToPoint();
            ppi.setLocation(x / 1000.0, y / 1000.0);
            llpi = (LatLonPointImpl) projection.projToLatLon(ppi, llpi);
            work[IDX_LON] = llpi.getLongitude();
            work[IDX_LAT] = llpi.getLatitude();
        } else if (isWgs84) {
            work = GeoUtils.wgs84XYZToLatLonAlt(x, y, z, work);
            //            if(cnt++<100) {
            //                System.err.println("elev:" + work[IDX_ALT]);
            //            }
        } else {}
        work[IDX_LON] = GeoUtils.normalizeLongitude(work[IDX_LON]);

        return work;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isCRS3D() {
        return isWgs84;
    }




    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isGeographic() {
        return true;
    }

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public void printData() throws Exception {
        final PrintWriter pw = new PrintWriter(
                                   new BufferedOutputStream(
                                       new FileOutputStream("point.out"),
                                       100000));

        final int[]   cnt     = { 0 };
        RecordVisitor visitor = new RecordVisitor() {
            public boolean visitRecord(RecordFile file, VisitInfo visitInfo,
                                       Record record) {
                cnt[0]++;
                ((PointRecord) record).printCsv(visitInfo, pw);

                return true;
            }
        };

        visit(visitor);
        pw.close();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isTrajectory() {
        return false;
    }





    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        String s = "label=\"Height\" type=double missing=99999";
        System.err.println(parseAttributes(s));

        if(true) return;


        String epsg = "32610";
        epsg = "2955";
        epsg = "26711";
        //# NAD83 / UTM zone 11N
        epsg = "26911";
        com.jhlabs.map.proj.Projection jhProjection =
            com.jhlabs.map.proj.ProjectionFactory
                .getNamedPROJ4CoordinateSystem(epsg);
        double         x   = 414639.5382;
        double         y   = 4428236.0648;

        Point2D.Double src = new Point2D.Double(x, y);
        Point2D.Double dst = jhProjection.inverseTransform(src,
                                 new Point2D.Double(0, 0));
        UtmProjection       projection = new UtmProjection(11, true);
        ProjectionPointImpl ppi        = new ProjectionPointImpl(x, y);
        LatLonPointImpl     llpi       = new LatLonPointImpl();
        ppi.setLocation(x / 1000.0, y / 1000.0);
        llpi = (LatLonPointImpl) projection.projToLatLon(ppi, llpi);
        System.err.println("result: " + llpi.getLatitude() + " "
                           + llpi.getLongitude());

        System.err.println("jhproj nad83 result: " + dst.getY() + " "
                           + dst.getX());


        if (true) {
            return;
        }


        /*
        for (int argIdx = 0; argIdx < args.length; argIdx++) {
            String arg = args[argIdx];
            try {
                long        t1  = System.currentTimeMillis();
                final int[] cnt = { 0 };
                PointFile file = new PointFileFactory().doMakePointFile(arg,
                                     getPropertiesForFile(arg));
                final RecordVisitor metadata = new RecordVisitor() {
                    public boolean visitRecord(RecordFile file,
                            VisitInfo visitInfo, Record record) {
                        cnt[0]++;
                        PointRecord pointRecord = (PointRecord) record;
                        if ((pointRecord.getLatitude() < -90)
                                || (pointRecord.getLatitude() > 90)) {
                            System.err.println("Bad lat:"
                                    + pointRecord.getLatitude());
                        }
                        if ((cnt[0] % 100000) == 0) {
                            System.err.println(cnt[0] + " lat:"
                                    + pointRecord.getLatitude() + " "
                                    + pointRecord.getLongitude() + " "
                                    + pointRecord.getAltitude());

                        }
                        return true;
                    }
                };
                file.visit(metadata);
                long t2 = System.currentTimeMillis();
                System.err.println("time:" + (t2 - t1) / 1000.0
                                   + " # record:" + cnt[0]);
            } catch (Exception exc) {
                System.err.println("Error:" + exc);
                exc.printStackTrace();
            }
        }
        */

    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean hasWaveform() {
        String[] waveforms = getWaveformNames();

        return (waveforms != null) && (waveforms.length > 0);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String[] getWaveformNames() {
        return null;
    }

    /**
     * _more_
     *
     * @param pointIndex _more_
     * @param name _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Waveform getWaveform(int pointIndex, String name)
            throws Exception {
        PointRecord record = (PointRecord) getRecord(pointIndex);

        return record.getWaveform(name);
    }


    /**
     * _more_
     *
     * @param file _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void runCheck(String file, StringBuffer sb) throws Exception {
        long                         t1       = System.currentTimeMillis();
        final int[]                  cnt      = { 0 };
        final PointMetadataHarvester metadata = new PointMetadataHarvester() {
            public boolean visitRecord(RecordFile file, VisitInfo visitInfo,
                                       Record record) {
                cnt[0]++;

                return super.visitRecord(file, visitInfo, record);
            }
        };
        Hashtable properties = RecordFile.getPropertiesForFile(file,
                                   PointFile.DFLT_PROPERTIES_FILE);

        if (properties != null) {
            this.setProperties(properties);
        }


        this.visit(metadata);
        long t2 = System.currentTimeMillis();
        sb.append("# records:" + cnt[0]);
        sb.append("\n");
        sb.append("" + metadata);
    }



    /**
     * _more_
     *
     * @param args _more_
     * @param pointFileClass _more_
     */
    public static void test(String[] args, Class pointFileClass) {
        boolean verbose = true;
        for (int argIdx = 0; argIdx < args.length; argIdx++) {
            String arg = args[argIdx];
            if (arg.equals("-noverbose")) {
                verbose = false;

                continue;
            }
            try {
                PointFile pointFile =
                    (PointFile) Misc.findConstructor(pointFileClass,
                        new Class[] {
                            String.class }).newInstance(new Object[] { arg });
                StringBuffer sb = new StringBuffer();
                pointFile.runCheck(arg, sb);
                if (verbose) {
                    System.err.println(sb);
                }
            } catch (Exception exc) {
                System.err.println("Error:" + exc + " file:" + arg);
                exc.printStackTrace();
                if (verbose) {
                    return;
                }
            }
        }
    }

    //Cough, cough

    /** _more_ */
    private static Hashtable<String, Hashtable<String, Station>> stationsMapMap =
        new Hashtable<String, Hashtable<String, Station>>();

    /**
     * _more_
     *
     * @return _more_
     */
    public Hashtable<String, Station> getStationMap() {
        String path = getStationsPath();
        if (path == null) {
            return null;
        }
        Hashtable<String, Station> stations = stationsMapMap.get(path);
        if (stations == null) {
            stations = readStations(path);
            stationsMapMap.put(path, stations);
        }

        return stations;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getStationsPath() {
        String path = getClass().getCanonicalName();
        path = path.replaceAll("\\.", "/");
        path = "/" + path + ".stations.txt";

        return path;
    }


    /**
     * _more_
     *
     * @param id _more_
     *
     * @return _more_
     */
    public Station getStation(String id) {
        Station station = getStationMap().get(id);
        if (station == null) {
            station = getStationMap().get(id.toUpperCase());
        }
        if (station == null) {
            station = getStationMap().get(id.toLowerCase());
        }

        return station;
    }




    /**
     * _more_
     *
     * @param siteId _more_
     *
     * @return _more_
     */
    public Station setLocation(String siteId) {
        Station station = getStation(siteId);

        if (station == null) {
            //            throw new IllegalArgumentException("Unknown station:" + siteId);
            System.out.println("Unknown station:" + siteId);

            return null;
        }

        setLocation(station.getLatitude(), station.getLongitude(),
                    station.getElevation());

        return station;
    }




    /**
     * _more_
     *
     * @param path _more_
     *
     * @return _more_
     */
    public Hashtable<String, Station> readStations(String path) {
        try {
            Hashtable<String, Station> stations = new Hashtable<String,
                                                      Station>();
            for (String line :
                    StringUtil.split(IOUtil.readContents(path, getClass()),
                                     "\n", true, true)) {
                if (line.startsWith("#")) {
                    continue;
                }
                List<String> toks = StringUtil.split(line, ",", true, true);
                Station station = new Station(toks.get(0), toks.get(1),
                                      Utils.decodeLatLon(toks.get(2)),
                                      Utils.decodeLatLon(toks.get(3)),
                                      Double.parseDouble(toks.get(4)));

                /*
                  System.out.println("<entry " +
                  XmlUtil.attr("name", station.getName()) +
                  XmlUtil.attr("type", "project_site") +
                  XmlUtil.attr("latitude", ""+station.getLatitude()) +
                  XmlUtil.attr("longitude", ""+station.getLongitude()) +
                  ">");
                  System.out.println("<short_name>" +"GCNET-" +  toks.get(0) + "</short_name>");
                  System.out.println("<status>active</status>");
                  System.out.println("<network>GCNET</network>");
                  System.out.println("<location>Greenland</location>");
                  System.out.println("</entry>");
                */
                stations.put(toks.get(0), station);
            }

            return stations;
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }



    /**
     * _more_
     *
     * @param year _more_
     * @param julianDay _more_
     *
     * @return _more_
     */
    public Date getDateFromJulianDay(int year, double julianDay) {
        int    day       = (int) julianDay;
        double remainder = julianDay - day;
        int    hour      = (int) remainder;
        remainder = remainder - hour;
        int minute = (int) (remainder * 60);
        remainder = remainder - minute;
        GregorianCalendar gc = new GregorianCalendar();
        gc.set(GregorianCalendar.YEAR, year);
        gc.set(GregorianCalendar.DAY_OF_YEAR, day);
        gc.set(GregorianCalendar.HOUR, hour);
        gc.set(GregorianCalendar.MINUTE, minute);
        gc.set(GregorianCalendar.SECOND, 0);

        return gc.getTime();
    }

    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public double decodeLatLon(String s) {
        return Utils.decodeLatLon(s);
    }

    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public double decodeElevation(String s) {
        if (s.endsWith("m")) {
            s = s.substring(0, s.length() - 1);
        }

        return Double.parseDouble(s);
    }

    /** _more_ */
    public static final String ATTR_TYPE = "type";

    /** _more_ */
    public static final String ATTR_LABEL = "label";

    /** _more_ */
    public static final String ATTR_MISSING = "missing";


    /** _more_ */
    public static final String ATTR_SORTORDER = "sortorder";

    /** _more_ */
    public static final String ATTR_SCALE = "scale";

    /** _more_ */
    public static final String ATTR_OFFSET = "offset";

    /** _more_ */
    public static final String ATTR_OFFSET1 = "offset1";

    /** _more_ */
    public static final String ATTR_OFFSET2 = "offset2";

    /** _more_ */
    public static final String ATTR_VALUE = "value";

    /** _more_ */
    public static final String ATTR_FORMAT = "format";

    /** _more_ */
    public static final String ATTR_UNIT = "unit";

    /** _more_ */
    public static final String ATTR_SEARCHABLE = "searchable";

    /** _more_ */
    public static final String ATTR_CHARTABLE = "chartable";

    /** _more_ */
    public static final String ATTR_PATTERN = "pattern";



    /**
     * _more_
     *
     * @param fieldString _more_
     *
     * @return _more_
     */
    public List<RecordField> doMakeFields(String fieldString) {

        //        System.err.println ("fields:" + fieldString);
        //x[unit="m"],y[unit="m"],z[unit="m"],red[],green[],blue[],amplitude[]
        //        System.err.println ("fields:" + fieldString);
        String defaultMissing     = getProperty(ATTR_MISSING, (String) null);
        String[]          toks    = fieldString.split(",");
        List<RecordField> fields  = new ArrayList<RecordField>();
        int               paramId = 1;
        for (String tok : toks) {
            List<String> pair  = StringUtil.splitUpTo(tok, "[", 2);
            String       name  = pair.get(0).trim();
            String       attrs = ((pair.size() > 1)
                                  ? pair.get(1)
                                  : "").trim();
            if (attrs.startsWith("[")) {
                attrs = attrs.substring(1);
            }
            if (attrs.endsWith("]")) {
                attrs = attrs.substring(0, attrs.length() - 1);
            }
            Hashtable properties = parseAttributes(attrs);
            /*
            if(name.equals("latitude")) {
                System.err.println ("attrs:" + attrs);
                System.err.println ("props:" + properties);
            }
            */
            RecordField field = new RecordField(name, name, "", paramId++,
                                    getProperty(properties, ATTR_UNIT, ""));

            field.setColumnWidth(new Integer(getProperty(field, properties,
                    "width", "0")).intValue());
            field.setIndex(new Integer(getProperty(field, properties,
                    "index", "-1")).intValue());
            field.setScale(Double.parseDouble(getProperty(field, properties,
                    ATTR_SCALE, "1.0")));
            field.setOffset1(Double.parseDouble(getProperty(field,
                    properties, ATTR_OFFSET1, "0.0")));
            field.setOffset2(Double.parseDouble(getProperty(field,
                    properties, ATTR_OFFSET2, "0.0")));
            String offset = getProperty(field, properties, ATTR_OFFSET,
                                        (String) null);

            if (offset != null) {
                field.setOffset2(Double.parseDouble(offset));
            }

            field.setIsDate(getProperty(field, properties,
                                        RecordField.PROP_ISDATE,
                                        "false").equals("true"));
            field.setIsTime(getProperty(field, properties,
                                        RecordField.PROP_ISTIME,
                                        "false").equals("true"));

            field.setIsLatitude(getProperty(field, properties,
                                            RecordField.PROP_ISLATITUDE,
                                            "false").equals("true"));
            field.setSortOrder(Integer.parseInt(getProperty(field,
                    properties, RecordField.PROP_SORTORDER, "0")));

            field.setIsLongitude(getProperty(field, properties,
                                             RecordField.PROP_ISLONGITUDE,
                                             "false").equals("true"));

            field.setIsAltitude(getProperty(field, properties,
                                            RecordField.PROP_ISALTITUDE,
                                            "false").equals("true"));

            field.setIsAltitudeReverse(getProperty(field, properties,
                    RecordField.PROP_ISALTITUDEREVERSE,
                    "false").equals("true"));

            String utcoffset = getProperty(field, properties, PROP_UTCOFFSET,
                                           (String) null);
            if (utcoffset != null) {
                field.setUtcOffset(new Integer(utcoffset).intValue());
            }
            String precision = getProperty(field, properties, PROP_PRECISION,
                                           (String) null);
            if (precision != null) {
                field.setRoundingFactor(Math.pow(10,
                        Integer.parseInt(precision)));
            }

            String missing = getProperty(field, properties, "missing",
                                         defaultMissing);
            if (missing != null) {
                field.setMissingValue(Double.parseDouble(missing));
            }


            String fmt = getProperty(field, properties, "fmt", (String) null);
            if (fmt == null) {
                fmt = getProperty(field, properties, PROP_FORMAT,
                                  (String) null);
            }

            if (fmt != null) {
                String timezone = getProperty(field, properties, "timezone",
                                      "UTC");
                field.setType(field.TYPE_DATE);
                SimpleDateFormat sdf = new SimpleDateFormat();
                sdf.setTimeZone(TimeZone.getTimeZone(timezone));
                sdf.applyPattern(fmt);
                field.setDateFormat(sdf);
            }

            String type = getProperty(field, properties, ATTR_TYPE,
                                      (String) null);
            if (type != null) {
                field.setType(type);
            }
            //Check for a default fixed value
            String value = getProperty(field, properties, ATTR_VALUE,
                                       (String) null);

            if (value == null) {
                String pattern = getProperty(field, properties, ATTR_PATTERN,
                                             (String) null);
                if (pattern != null) {
                    String header = getTextHeader();
                    String patternMatch = StringUtil.findPattern(header,
                                              pattern);
                    if (patternMatch == null) {
                        throw new IllegalArgumentException(
                            "No match.\nPattern:" + pattern + "\nField:"
                            + field + "\nHeader:" + header);
                    }

                    if (name.equalsIgnoreCase(FIELD_LATITUDE)
                            || name.equalsIgnoreCase(FIELD_LONGITUDE)
                            || field.getIsLatitude()
                            || field.getIsLongitude()) {
                        value = "" + decodeLatLon(patternMatch);
                        // I need to think about the implications of elevation reverse
                    } else if (name.equalsIgnoreCase(FIELD_ELEVATION)
                               || field.getIsAltitude()) {
                        value = "" + decodeElevation(patternMatch);
                    } else {
                        value = patternMatch;
                    }
                }
            }

            if (value != null) {
                if (field.isTypeString()) {
                    field.setDefaultStringValue(value);
                } else if (field.isTypeDate()) {
                    field.setDefaultStringValue(value);
                } else {
                    field.setDefaultDoubleValue(Double.parseDouble(value));
                }
            }
            String chartable = getProperty(field, properties, "chartable",
                                           "NA");
            if (chartable.equals("true")) {
                field.setChartable(true);
            } else if (chartable.equals("false")) {
                field.setChartable(false);
            }
            if (getProperty(field, properties, "skip",
                            "false").equals("true")) {
                field.setSkip(true);
            }
            if (getProperty(field, properties, "synthetic",
                            "false").equals("true")) {
                field.setSynthetic(true);
            }
            if (getProperty(field, properties, "searchable",
                            "false").equals("true")) {
                field.setSearchable(true);
            }
            if (getProperty(field, properties, "value",
                            "false").equals("true")) {
                field.setSearchable(true);
            }
            String label = getProperty(field, properties, ATTR_LABEL,
                                       (String) null);
            String desc = getProperty(field, properties, "description",
                                      (String) null);
            if(desc !=null)
                desc = desc.replaceAll("_comma_",",");
            if (label == null) {
                label = desc;
            }
            if (label != null) {
                field.setLabel(label);
            }
            if (desc != null) {
                field.setDescription(desc);
            }
            DataRecord.initField(field);
            fields.add(field);
        }

        return fields;



    }


    /**
     * _more_
     *
     * @param attrs _more_
     *
     * @return _more_
     */
    public static Hashtable parseAttributes(String attrs) {
        if(true)
            return HtmlUtils.parseHtmlProperties(attrs);
        Hashtable ht                    = new Hashtable();
        String    attrName              = "";
        String    attrValue             = "";
        final int STATE_LOOKINGFORNAME  = 0;
        final int STATE_INNAME          = 1;
        final int STATE_LOOKINGFORVALUE = 2;
        final int STATE_INVALUE         = 2;
        int       state                 = STATE_LOOKINGFORNAME;
        attrs = attrs + " ";
        char[]  chars          = attrs.toCharArray();
        boolean gotDblQuote    = false;
        boolean gotSingleQuote = false;
        boolean gotEquals      = false;

        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            switch (state) {

              case STATE_LOOKINGFORNAME : {
                  if ((c == ' ') || (c == '\t')) {
                      break;
                  }
                  attrName  = "" + c;
                  state     = STATE_INNAME;
                  gotEquals = false;

                  break;
              }

              case STATE_INNAME : {
                  //Are we at the end of the name?
                  if ((c == ' ') || (c == '\t') || (c == '=')) {
                      if ( !gotEquals) {
                          gotEquals = (c == '=');
                      }

                      break;
                  }
                  if ((c == '\"') || (c == '\'')) {
                      gotDblQuote    = (c == '\"');
                      gotSingleQuote = (c == '\'');
                      state          = STATE_INVALUE;

                      break;
                  }
                  if (gotEquals) {
                      attrValue += c;
                      state     = STATE_INVALUE;

                      break;
                  }

                  attrName += c;

                  break;
              }

              case STATE_INVALUE : {
                  if ((gotDblQuote && (c == '\"'))
                          || (gotSingleQuote && (c == '\''))
                          || ( !gotDblQuote && !gotSingleQuote
                               && (c == ' '))) {
                      attrValue = attrValue.replace("&#44;", ",");
                      ht.put(attrName.toLowerCase().trim(), attrValue);
                      state     = STATE_LOOKINGFORNAME;
                      attrName  = "";
                      attrValue = "";

                      break;
                  }
                  attrValue += c;

                  break;
              }
            }
        }
        if (attrName.length() > 0) {
            attrValue = attrValue.replace("&#44;", ",");
            ht.put(attrName.toLowerCase().trim(), attrValue.trim());
        }

        return ht;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getTextHeader() {
        return "";
    }



}
