/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.gtfs;


import org.json.*;

import org.ramadda.data.services.*;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.TypeHandler;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.NamedInputStream;
import org.ramadda.util.Utils;
import org.ramadda.util.geo.GeoUtils;
import org.ramadda.util.seesv.*;
import org.ramadda.util.seesv.Seesv;
import org.ramadda.util.seesv.DataProvider;

import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;

import java.awt.geom.Rectangle2D;


import java.io.*;

import java.net.URL;
import java.net.URL;
import java.net.URLConnection;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;



/**
 */
@SuppressWarnings("unchecked")
public class GtfsImportHandler extends ImportHandler {

    /** _more_ */
    private static boolean debug = false;

    /**
     * _more_
     */
    public GtfsImportHandler() {
        super(null);
    }

    /**
     * _more_
     *
     * @param repository _more_
     */
    public GtfsImportHandler(Repository repository) {
        super(repository);
    }

    /**
     * _more_
     *
     * @param importTypes _more_
     * @param formBuffer _more_
     */
    @Override
    public void addImportTypes(List<TwoFacedObject> importTypes,
                               Appendable formBuffer) {
        super.addImportTypes(importTypes, formBuffer);
        importTypes.add(new TwoFacedObject("GTFS Feed", "gtfs"));
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param repository _more_
     * @param file _more_
     * @param parentEntry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public Result handleRequest(Request request, Repository repository,
                                String file, Entry parentEntry)
            throws Exception {

        if ( !request.getString(ARG_IMPORT_TYPE, "").equals("gtfs")) {
            return null;
        }

        Hashtable<String, String> props =
            HtmlUtils.parseHtmlProperties(request.getString("extra", ""));
        List<String> files = new ArrayList<String>();
        String       path  = props.get("directory");
        if ((path != null) && request.getUser().getAdmin()) {
            File[] dirFiles = new File(path).listFiles();
            for (File f : dirFiles) {
                if (f.isFile()) {
                    files.add(f.toString());
                }
            }
        } else {
            files.add(file);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("+section\n");
        sb.append(":title GTFS Import Results\n");
        for (String f : files) {
            System.err.println("GtfsImportHandler processing file:" + f);
            sb.append(HtmlUtils.h3("Imported: " + new File(f).getName()));
            handleRequestInner(request, repository, props, f, parentEntry,
                               sb);
        }
        sb.append("\n-section\n");
        String html = getWikiManager().wikifyEntry(request, parentEntry,
                          sb.toString());

        return getEntryManager().addEntryHeader(request, parentEntry,
                new Result("", new StringBuilder(html)));

    }


    /**
     * _more_
     *
     * @param request _more_
     * @param repository _more_
     * @param props _more_
     * @param file _more_
     * @param parentEntry _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    private void handleRequestInner(Request request, Repository repository,
                                    Hashtable props, String file,
                                    Entry parentEntry, Appendable sb)
            throws Exception {


        List<Entry>    entries = new ArrayList<Entry>();
        List<Entry>    stops   = new ArrayList<Entry>();
        InputStream    is      = IOUtil.getInputStream(file, getClass());
        ZipInputStream zin     = getStorageManager().makeZipInputStream(is);
        ZipEntry       ze      = null;
        File           dir     = getStorageManager().createProcessDir();
        try {
            while ((ze = zin.getNextEntry()) != null) {
                if (ze.isDirectory()) {
                    continue;
                }
                String path = ze.getName();
                String name = IOUtil.getFileTail(path);
                FileOutputStream fos = new FileOutputStream(new File(dir,
                                           name));
                IOUtil.writeTo(zin, fos);
                IOUtil.close(fos);
            }
        } finally {
            IOUtil.close(is);
            IOUtil.close(zin);
        }

        Hashtable<String, Entry> agencyMap = new Hashtable<String, Entry>();
        List<Entry> agencies =
            processAgency(request, props, parentEntry, entries, agencyMap,
                          new FileInputStream(new File(dir, "agency.txt")));

        //        if(true) return;

        Hashtable<String, ServiceInfo> services = processCalendar(request,
                                                      new File(dir,
                                                          "calendar.txt"));

        Hashtable<String, String> stopToTrip = new Hashtable<String,
                                                   String>();
        Hashtable<String, List<String[]>> stopTimes =
            processStopTimes(request,
                             new FileInputStream(new File(dir,
                                 "stop_times.txt")), stopToTrip);

        Hashtable<String, Entry> stopToAgency = new Hashtable<String,
                                                    Entry>();

        if (agencies.size() > 1) {
            List<String> stopIds = new ArrayList<String>();
            Hashtable<String, List<String>> parentToChild =
                new Hashtable<String, List<String>>();
            getParentToChild(request, parentToChild, stopIds,
                             new FileInputStream(new File(dir, "stops.txt")));

            Hashtable<String, String> tripToRoute = new Hashtable<String,
                                                        String>();
            getTripToRoute(request, tripToRoute,
                           new FileInputStream(new File(dir, "trips.txt")));

            Hashtable<String, String> routeToAgency = new Hashtable<String,
                                                          String>();
            getRouteToAgency(request, routeToAgency,
                             new FileInputStream(new File(dir,
                                 "routes.txt")));

            for (String stopId : stopIds) {
                String tripId = stopToTrip.get(stopId);
                if (tripId == null) {
                    List<String> childs = parentToChild.get(stopId);
                    if (childs != null) {
                        for (String child : childs) {
                            tripId = stopToTrip.get(child);
                            if (tripId != null) {
                                break;
                            }
                        }
                    }
                    if (tripId == null) {
                        System.err.println("no trip for stop:" + stopId
                                           + " found from children:" + tripId
                                           + " " + parentToChild.get(stopId));
                    }
                }
                if (tripId == null) {
                    //                throw new IllegalArgumentException("Could not find trip for stop:" + stopId);
                    continue;
                }
                String routeId = tripToRoute.get(tripId);
                if (routeId == null) {
                    if (true) {
                        throw new IllegalArgumentException(
                            "Could not find route for stop:" + stopId
                            + " trip:" + tripId);
                    }

                    continue;
                }
                String agencyId = routeToAgency.get(routeId);
                if (agencyId == null) {
                    if (true) {
                        throw new IllegalArgumentException(
                            "Could not find agencyID for stop:" + stopId
                            + " trip:" + tripId + " route:" + routeId);
                    }

                    continue;
                }
                Entry agency = agencyMap.get(agencyId);
                if (agency == null) {
                    if (true) {
                        throw new IllegalArgumentException(
                            "Could not find agency for stop:" + stopId
                            + " trip:" + tripId + " route:" + routeId);
                    }

                    continue;
                }
                stopToAgency.put(stopId, agency);
            }
        }

        Hashtable<String, List<float[]>> pts = processShapes(request,
                                                   new File(dir,
                                                       "shapes.txt"));

        Hashtable<String, Entry> stopsMap = new Hashtable<String, Entry>();
        if (agencies.size() == 0) {
            sb.append("No agency found for:" + file);

            return;
        }
        processStops(request, stopToAgency, agencies.get(0), entries,
                     stopsMap,
                     new FileInputStream(new File(dir, "stops.txt")));
        List<Entry>              routes   = new ArrayList<Entry>();
        Hashtable<String, Entry> routeMap = new Hashtable<String, Entry>();
        processRoutes(request, props, agencyMap, routes, routeMap,
                      new FileInputStream(new File(dir, "routes.txt")));
        entries.addAll(routes);

        processTrips(request, entries, agencyMap, routeMap, pts, services,
                     stopsMap, stopTimes,
                     new FileInputStream(new File(dir, "trips.txt")));

        for (Entry route : routes) {
            setBounds(route, route.getChildren());
            Object[] values = route.getTypeHandler().getEntryValues(route);
            Object   names  = route.getProperty("stop_names");
            if (names != null) {
                String s = names.toString();
                if (s.length() > 30000) {
                    s = s.substring(0, 29999);
                }
                values[GtfsRouteTypeHandler.IDX_STOP_NAMES] = s;
            }
        }
        if (routes.size() > 0) {
            Entry routesEntry = routes.get(0).getParentEntry();
            setBounds(routesEntry, routesEntry.getChildren());
        }


        for (Entry agencyEntry : agencies) {
            setBounds(agencyEntry, agencyEntry.getChildren());
        }

        sb.append("<ul>\n");
        int cnt = 0;
        for (Entry entry : entries) {
            entry.setUser(request.getUser());
            sb.append("<li> ");
            sb.append(getEntryManager().getEntryLink(request, entry, true,
                    ""));
            sb.append("\n");
            if (cnt++ > 100) {
                sb.append("<p>...<br>");

                break;
            }
        }
        sb.append("</ul>\n");
        getEntryManager().addNewEntries(request, entries);
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param stopToAgency _more_
     * @param dfltAgency _more_
     * @param entries _more_
     * @param stopsMap _more_
     * @param is _more_
     *
     *
     * @throws Exception _more_
     */
    private void processStops(final Request request,
                              final Hashtable<String, Entry> stopToAgency,
                              Entry dfltAgency, final List<Entry> entries,
                              final Hashtable<String, Entry> stopsMap,
                              InputStream is)
            throws Exception {
        final List<Entry> stops      = new ArrayList<Entry>();
        final Date        now        = new Date();
        final User        user       = request.getUser();

        TextReader        textReader = new TextReader();
        textReader.setInput(is);
        textReader.addProcessor(new MyProcessor() {
            boolean googleOk = true;
            @Override
            public org.ramadda.util.seesv.Row processRow(
                    TextReader textReader, org.ramadda.util.seesv.Row row) {
                try {
                    if (checkMap(row)) {
                        return row;
                    }
                    List<String> toks = row.getValues();

                    String       id   = getValue("stop_id", map, toks, "");
                    String       name = getValue("stop_name", map, toks, id);
                    if ( !Utils.stringDefined(name)) {
                        name = "Stop: " + id;
                    }
                    String desc = getValue("stop_desc", map, toks, "");
                    double lat = GeoUtils.decodeLatLon(getValue("stop_lat", map,
                                     toks, "40"));
                    double lon =GeoUtils.decodeLatLon(getValue("stop_lon", map,
                                     toks, "-100"));
                    String url = getValue("stop_url", map, toks, "");
                    TypeHandler typeHandler =
                        getRepository().getTypeHandler("type_gtfs_stop");
                    Entry entry =
                        typeHandler.createEntry(repository.getGUID());
                    Object[] values = typeHandler.getEntryValues(entry);
                    values[GtfsStopTypeHandler.IDX_STOP_ID] = id;
                    values[GtfsStopTypeHandler.IDX_STOP_CODE] =
                        getValue("stop_code", map, toks, "");
                    values[GtfsStopTypeHandler.IDX_ZONE_ID] =
                        getValue("zone_id", map, toks, "");
                    values[GtfsStopTypeHandler.IDX_LOCATION_TYPE] =
                        getValue("location_type", map, toks, "");
                    values[GtfsStopTypeHandler.IDX_TIMEZONE] =
                        getValue("stop_timezone", map, toks, "");
                    values[GtfsStopTypeHandler.IDX_WHEELCHAIR_BOARDING] =
                        getValue("wheelchair_boarding", map, toks, "");

                    Entry agencyEntry = stopToAgency.get(id);
                    if (agencyEntry == null) {
                        if (stopToAgency.size() > 0) {
                            System.err.println(
                                "could not find agency for stop:" + id
                                + " using default");
                        }
                        //                        if(true) throw new IllegalArgumentException("could not find agency for stop:" + id);
                        agencyEntry = dfltAgency;
                    }

                    if (agencyEntry == null) {
                        throw new IllegalArgumentException(
                            "Could not find agency for stop:" + id);
                    }

                    values[GtfsStopTypeHandler.IDX_AGENCY_ID] =
                        agencyEntry.getId();
                    Resource resource = Utils.stringDefined(url)
                                        ? new Resource(new URL(url))
                                        : new Resource();

                    Entry stopsEntry =
                        (Entry) agencyEntry.getProperty("stopsEntry");

                    entry.initEntry(name, desc, stopsEntry, user, resource,
                                    "", Entry.DEFAULT_ORDER, now.getTime(),
                                    now.getTime(), now.getTime(),
                                    now.getTime(), values);
                    entry.setLocation(lat, lon, 0);
                    String agencyId =
                        agencyEntry.getStringValue(
                            GtfsAgencyTypeHandler.IDX_AGENCY_ID, "");
                    Gtfs.addAlias(request, entry,
                                  "gtfs." + agencyId + ".stop." + id);
                    stops.add(entry);
                    stopsMap.put(id, entry);
                } catch (Exception exc) {
                    throw new RuntimeException(exc);
                }

                return row;
            }
        });

        Seesv csvUtil = new Seesv(new ArrayList<String>());
        csvUtil.process(textReader);
        if (stops.size() > 0) {
            setBounds(stops.get(0).getParentEntry(), stops);
        }

        entries.addAll(stops);

    }


    /**
     * _more_
     *
     * @param entry _more_
     * @param children _more_
     *
     * @throws Exception _more_
     */
    private void setBounds(Entry entry, List<Entry> children)
            throws Exception {
        Rectangle2D.Double rect = getEntryUtil().getBounds(getRepository().getAdminRequest(),children);
        if (rect != null) {
            //            System.err.println("set bounds:" + entry.getName() + " " + children.size() + " " + rect.getWidth());
            if ( !Misc.equals(rect, entry.getBounds(getRepository().getAdminRequest()))) {
                entry.setBounds(rect);
            }
        }
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param is _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    static int xcnt = 0;

    /**
     * _more_
     *
     * @param request _more_
     * @param file _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Hashtable<String,
                      List<float[]>> processShapes(final Request request,
                          File file)
            throws Exception {

        final List<String> ids = new ArrayList<String>();
        final Hashtable<String, List<float[]>> pts = new Hashtable<String,
                                                         List<float[]>>();

        if ( !file.exists()) {
            return pts;
        }

        InputStream is         = new FileInputStream(file);
        TextReader  textReader = new TextReader();
        textReader.setInput(is);
        textReader.addProcessor(new MyProcessor() {
            @Override
            public org.ramadda.util.seesv.Row processRow(
                    TextReader textReader, org.ramadda.util.seesv.Row row) {
                try {
                    if (checkMap(row)) {
                        return row;
                    }
                    List<String> toks = row.getValues();
                    String       id   = getValue("shape_id", map, toks, "");
                    float lat =
                        (float) GeoUtils.decodeLatLon(getValue("shape_pt_lat",
                            map, toks, "40"));
                    float lon =
                        (float) GeoUtils.decodeLatLon(getValue("shape_pt_lon",
                            map, toks, "-100"));
                    int seq = Integer.parseInt(getValue("shape_pt_sequence",
                                  map, toks, "0"));

                    List<float[]> list = pts.get(id);
                    if (list == null) {
                        list = new ArrayList<float[]>();
                        pts.put(id, list);
                        ids.add(id);
                    }
                    list.add(new float[] { seq, lat, lon });
                } catch (Exception exc) {
                    throw new RuntimeException(exc);
                }

                return row;
            }
        });
        Seesv csvUtil = new Seesv(new ArrayList<String>());
        csvUtil.process(textReader);

        Hashtable<String, List<float[]>> sorted = new Hashtable<String,
                                                      List<float[]>>();


        Comparator comp = new Utils.FloatTupleComparator(0);

        for (String id : ids) {
            List<float[]> v     = pts.get(id);
            Object[]      array = v.toArray();
            Arrays.sort(array, comp);
            v = (List<float[]>) Misc.toList(array);
            List<float[]> tmp     = new ArrayList<float[]>();
            float         lastLat = Float.NaN;
            float         lastLon = Float.NaN;
            for (float[] tuple : v) {
                float lat = tuple[1];
                float lon = tuple[2];
                lat = ((int) (lat * 100000)) / 100000.0f;
                lon = ((int) (lon * 100000)) / 100000.0f;
                if ((lastLat == lat) && (lastLon == lon)) {
                    continue;
                }
                lastLat = lat;
                lastLon = lon;
                tmp.add(new float[] { lat, lon });
            }
            sorted.put(id, tmp);
        }

        return sorted;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param is _more_
     * @param stopToTrip _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Hashtable<String,
                      List<String[]>> processStopTimes(final Request request,
                          InputStream is,
                          final Hashtable<String, String> stopToTrip)
            throws Exception {
        final Hashtable<String, List<String[]>> stops = new Hashtable<String,
                                                            List<String[]>>();


        TextReader textReader = new TextReader();
        textReader.setInput(is);
        textReader.addProcessor(new MyProcessor() {
            @Override
            public org.ramadda.util.seesv.Row processRow(
                    TextReader textReader, org.ramadda.util.seesv.Row row) {
                try {
                    if (checkMap(row)) {
                        return row;
                    }
                    List<String> toks = row.getValues();
                    //trip_id,arrival_time,departure_time,stop_id,stop_sequence,stop_headsign,pickup_type,drop_off_type,shape_dist_traveled
                    String tripId  = getValue("trip_id", map, toks, "");
                    String arrival = getValue("arrival_time", map, toks, "");
                    String departure = getValue("departure_time", map, toks,
                                           "");
                    String stopId = getValue("stop_id", map, toks, "");

                    if (stopToTrip.get(stopId) == null) {
                        stopToTrip.put(stopId, tripId);
                    }
                    int seq = Integer.parseInt(getValue("stop_sequence", map,
                                  toks, "0"));

                    List<String[]> list = stops.get(tripId);
                    if (list == null) {
                        list = new ArrayList<String[]>();
                        stops.put(tripId, list);
                    }
                    list.add(new String[] { stopId, arrival, departure });
                } catch (Exception exc) {
                    throw new RuntimeException(exc);
                }

                return row;
            }
        });
        Seesv csvUtil = new Seesv(new ArrayList<String>());
        csvUtil.process(textReader);

        return stops;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param file _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Hashtable<String,
                      ServiceInfo> processCalendar(final Request request,
                          File file)
            throws Exception {

        final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        final Hashtable<String, ServiceInfo> cal = new Hashtable<String,
                                                       ServiceInfo>();
        if ( !file.exists()) {
            return cal;
        }
        InputStream is         = new FileInputStream(file);
        TextReader  textReader = new TextReader();
        textReader.setInput(is);
        textReader.addProcessor(new MyProcessor() {
            @Override
            public org.ramadda.util.seesv.Row processRow(
                    TextReader textReader, org.ramadda.util.seesv.Row row) {
                try {
                    if (checkMap(row)) {
                        return row;
                    }
                    //service_id,monday,tuesday,wednesday,thursday,friday,saturday,sunday,start_date,end_date
                    List<String> toks = row.getValues();
                    String       id   = getValue("service_id", map, toks, "");
                    String name = getValue("service_name", map, toks, "");
                    boolean[]    week = {
                        getValue("sunday", map, toks, "0").equals("1"),
                        getValue("monday", map, toks, "0").equals("1"),
                        getValue("tuesday", map, toks, "0").equals("1"),
                        getValue("wednesday", map, toks, "0").equals("1"),
                        getValue("thursday", map, toks, "0").equals("1"),
                        getValue("friday", map, toks, "0").equals("1"),
                        getValue("saturday", map, toks, "0").equals("1"),
                    };


                    String start = getValue("start_date", map, toks, "");
                    String end   = getValue("end_date", map, toks, "");
                    cal.put(id,
                            new ServiceInfo(id, name, week, sdf.parse(start),
                                            sdf.parse(end)));
                } catch (Exception exc) {
                    throw new RuntimeException(exc);
                }

                return row;
            }
        });
        Seesv csvUtil = new Seesv(new ArrayList<String>());
        csvUtil.process(textReader);

        return cal;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param props _more_
     * @param agencyMap _more_
     * @param entries _more_
     * @param routeMap _more_
     * @param is _more_
     *
     *
     * @throws Exception _more_
     */
    private void processRoutes(final Request request,
                               final Hashtable<String, String> props,
                               final Hashtable<String, Entry> agencyMap,
                               final List<Entry> entries,
                               final Hashtable<String, Entry> routeMap,
                               InputStream is)
            throws Exception {

        final Date now        = new Date();
        final User user       = request.getUser();

        TextReader textReader = new TextReader();
        textReader.setInput(is);
        textReader.addProcessor(new MyProcessor() {
            @Override
            public org.ramadda.util.seesv.Row processRow(
                    TextReader textReader, org.ramadda.util.seesv.Row row) {
                try {
                    if (checkMap(row)) {
                        return row;
                    }
                    List<String> toks = row.getValues();
                    //route_id,route_short_name,route_long_name,route_desc,route_type,route_url,route_color,route_text_color
                    TypeHandler typeHandler =
                        getRepository().getTypeHandler("type_gtfs_route");
                    Entry entry =
                        typeHandler.createEntry(repository.getGUID());

                    Object[] values = typeHandler.getEntryValues(entry);
                    String   id = (String) getValue("route_id", map, toks,
                                      "");
                    values[GtfsRouteTypeHandler.IDX_ID] = id;
                    String name;
                    String rawAgencyId = getValue("agency_id", map, toks, "");

                    String shortName = getValue("route_short_name", map,
                                           toks, "");
                    String longName = getValue("route_long_name", map, toks,
                                          shortName);


                    if ( !Utils.stringDefined(shortName)) {
                        shortName = id;
                        name      = longName;
                    } else if (shortName.equals(longName)) {
                        name = shortName;
                    } else {
                        name = shortName + " - " + longName;
                    }

                    String desc = getValue("route_desc", map, toks, "");
                    String url  = getValue("route_url", map, toks, "");
                    values[GtfsRouteTypeHandler.IDX_TYPE] =
                        getValue("route_type", map, toks, "");
                    values[GtfsRouteTypeHandler.IDX_COLOR] = "#"
                            + getValue("route_color", map, toks, "ff0000");
                    values[GtfsRouteTypeHandler.IDX_TEXT_COLOR] = "#"
                            + getValue("route_text_color", map, toks,
                                       "000000");
                    values[GtfsRouteTypeHandler.IDX_STOP_NAMES] = "";




                    Resource resource    = Utils.stringDefined(url)
                                           ? new Resource(new URL(url))
                                           : new Resource();

                    Entry    agencyEntry = agencyMap.get(rawAgencyId);
                    if (agencyEntry == null) {
                        agencyEntry =
                            agencyMap.get(rawAgencyId.toLowerCase());
                    }

                    if (agencyEntry == null) {
                        System.err.println(name + " agency id:" + rawAgencyId
                                           + " map:" + agencyMap);
                    } else {
                        values[GtfsRouteTypeHandler.IDX_AGENCY_ID] =
                            agencyEntry.getId();
                    }

                    Entry routesEntry =
                        (Entry) agencyEntry.getProperty("routesEntry");
                    String agencyId =
                        agencyEntry.getStringValue(
                            GtfsAgencyTypeHandler.IDX_AGENCY_ID, "");

                    entry.initEntry(name, "", routesEntry, user, resource,
                                    "", Entry.DEFAULT_ORDER, now.getTime(),
                                    now.getTime(), now.getTime(),
                                    now.getTime(), values);
                    routesEntry.getChildren().add(entry);
                    entry.putProperty("seen_stops", new HashSet());
                    entry.putProperty("stop_names", new StringBuilder());
                    entry.setChildren(new ArrayList<Entry>());
                    entries.add(entry);
                    entry.putProperty("agencyid", agencyId);

                    //Gtfs.addHostAlias(request, entry, props.get("host"),     "gtfs." + agencyId +"." + id);
                    Gtfs.addAlias(request, entry,
                                  "gtfs." + agencyId + ".route." + id);


                    routeMap.put(id, entry);
                } catch (Exception exc) {
                    throw new RuntimeException(exc);
                }

                return row;
            }
        });

        Seesv csvUtil = new Seesv(new ArrayList<String>());
        csvUtil.process(textReader);



    }


    /**
     * _more_
     *
     * @param request _more_
     * @param props _more_
     * @param parentEntry _more_
     * @param entries _more_
     * @param agencyMap _more_
     * @param is _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private List<Entry> processAgency(final Request request,
                                      final Hashtable<String, String> props,
                                      final Entry parentEntry,
                                      final List<Entry> entries,
                                      final Hashtable<String,
                                          Entry> agencyMap, InputStream is)
            throws Exception {


        final List<Entry> agencies   = new ArrayList<Entry>();
        final Date        now        = new Date();
        final User        user       = request.getUser();
        TextReader        textReader = new TextReader();
        textReader.setInput(is);
        textReader.addProcessor(new MyProcessor() {

            @Override
            public org.ramadda.util.seesv.Row processRow(
                    TextReader textReader, org.ramadda.util.seesv.Row row) {

                try {

                    if (checkMap(row)) {
                        return row;
                    }
                    List<String> toks = row.getValues();

                    //agency_id,agency_name,agency_url,agency_timezone,agency_lang,agency_phone, agency_fare_url
                    //                    debug = true;
                    String rawAgencyId = getValue("agency_id", map, toks, "");
                    String agencyId = Misc.getProperty(props, "agency",
                                          rawAgencyId.toLowerCase());
                    String attr = "agency_name";
                    String name = getValue(attr, map, toks, "DFLTNAME");
                    if (debug) {
                        System.err.println("name:" + name + " raw:"
                                           + rawAgencyId);
                    }
                    debug = false;



                    if (rawAgencyId.length() == 0) {
                        rawAgencyId = name.toLowerCase().replaceAll(" ", "_");
                    }
                    if (agencyId.length() == 0) {
                        agencyId = rawAgencyId;
                    }
                    //If its just a number then
                    System.err.println("AgencyId:" + agencyId);
                    if (agencyId.matches("\\d+")) {
                        agencyId = name.toLowerCase().replaceAll(" ", "_");
                        System.err.println("new AgencyId:" + agencyId);
                    }

                    String desc = getValue("agency_desc", map, toks, "");
                    String timezone = getValue("agency_timezone", map, toks,
                                          "");
                    String lang = getValue("agency_lang", map, toks, "");
                    String phone = getValue("agency_phone", map, toks, "");
                    String url = getValue("agency_url", map, toks, "");
                    String fare = getValue("agency_fare_url", map, toks, "");
                    TypeHandler typeHandler =
                        getRepository().getTypeHandler("type_gtfs_agency");
                    Entry entry =
                        typeHandler.createEntry(repository.getGUID());
                    Object[] values = typeHandler.getEntryValues(entry);
                    values[GtfsAgencyTypeHandler.IDX_AGENCY_ID] = agencyId;
                    values[GtfsAgencyTypeHandler.IDX_FARE_URL]  = fare;
                    values[GtfsAgencyTypeHandler.IDX_PHONE]     = phone;
                    values[GtfsAgencyTypeHandler.IDX_TIMEZONE]  = timezone;
                    values[GtfsAgencyTypeHandler.IDX_LANGUAGE]  = lang;

                    if (Utils.stringDefined(timezone)) {
                        getMetadataManager().addMetadata(request,
                            entry,
                            new Metadata(
                                request.getRepository().getGUID(),
                                entry.getId(),
                                ContentMetadataHandler.TYPE_TIMEZONE, true,
                                timezone, null, null, null, null));
                    }
                    agencies.add(entry);
                    //Gtfs.addHostAlias(request, entry,(String) props.get("host"), agencyId);
                    Gtfs.addAlias(request, entry, "gtfs." + agencyId);

                    Resource resource = Utils.stringDefined(url)
                                        ? new Resource(new URL(url))
                                        : new Resource();

                    entry.initEntry(name, "", parentEntry, user, resource,
                                    "", Entry.DEFAULT_ORDER, now.getTime(),
                                    now.getTime(), now.getTime(),
                                    now.getTime(), values);
                    entry.setChildren(new ArrayList<Entry>());
                    entries.add(entry);
                    agencyMap.put(rawAgencyId, entry);
                    agencyMap.put(agencyId, entry);
                    if (agencyMap.get("") == null) {
                        agencyMap.put("", entry);
                    }

                    Entry routesEntry = getRepository().getTypeHandler(
                                            "type_gtfs_routes").createEntry(
                                            repository.getGUID());


                    entry.getChildren().add(routesEntry);
                    routesEntry.initEntry("Routes", "", entry, user,
                                          new Resource(), "",
                                          Entry.DEFAULT_ORDER, now.getTime(),
                                          now.getTime(), now.getTime(),
                                          now.getTime(), null);
                    entries.add(routesEntry);
                    routesEntry.setChildren(new ArrayList<Entry>());
                    entry.putProperty("routesEntry", routesEntry);
                    Entry stopsEntry = getRepository().getTypeHandler(
                                           "type_gtfs_stops").createEntry(
                                           repository.getGUID());

                    entry.getChildren().add(stopsEntry);
                    String stopsDesc = "";
                    //                    getRepository().getResource("/org/ramadda/plugins/gtfs/stops.txt");
                    stopsEntry.initEntry("Stops", stopsDesc, entry, user,
                                         new Resource(), "",
                                         Entry.DEFAULT_ORDER, now.getTime(),
                                         now.getTime(), now.getTime(),
                                         now.getTime(), null);
                    entries.add(stopsEntry);
                    entry.putProperty("stopsEntry", stopsEntry);


                } catch (Exception exc) {
                    throw new RuntimeException(exc);
                }

                return row;

            }

        });

        Seesv csvUtil = new Seesv(new ArrayList<String>());
        csvUtil.process(textReader);

        return agencies;
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param parentToChild _more_
     * @param stopIds _more_
     * @param is _more_
     *
     * @throws Exception _more_
     */
    private void getParentToChild(
            final Request request,
            final Hashtable<String, List<String>> parentToChild,
            final List<String> stopIds, InputStream is)
            throws Exception {
        TextReader textReader = new TextReader();
        textReader.setInput(is);
        textReader.addProcessor(new MyProcessor() {
            public org.ramadda.util.seesv.Row processRow(
                    TextReader textReader, org.ramadda.util.seesv.Row row) {
                try {
                    if (checkMap(row)) {
                        return row;
                    }
                    //route_id,service_id,trip_id,trip_headsign,direction_id,block_id,shape_id,wheelchair_accessible,bikes_allowed

                    List<String> toks = row.getValues();
                    String parentId = getValue("parent_station", map, toks,
                                          "");
                    String stopId = getValue("stop_id", map, toks, "");
                    stopIds.add(stopId);
                    if (parentId.length() > 0) {
                        List<String> childs = parentToChild.get(parentId);
                        if (childs == null) {
                            childs = new ArrayList<String>();
                            parentToChild.put(parentId, childs);
                        }
                        childs.add(stopId);
                    }
                } catch (Exception exc) {
                    throw new RuntimeException(exc);
                }

                return row;
            }
        });
        Seesv csvUtil = new Seesv(new ArrayList<String>());
        csvUtil.process(textReader);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param tripToRoute _more_
     * @param is _more_
     *
     * @throws Exception _more_
     */
    private void getTripToRoute(final Request request,
                                final Hashtable<String, String> tripToRoute,
                                InputStream is)
            throws Exception {
        TextReader textReader = new TextReader();
        textReader.setInput(is);
        textReader.addProcessor(new MyProcessor() {
            public org.ramadda.util.seesv.Row processRow(
                    TextReader textReader, org.ramadda.util.seesv.Row row) {
                try {
                    if (checkMap(row)) {
                        return row;
                    }
                    //route_id,service_id,trip_id,trip_headsign,direction_id,block_id,shape_id,wheelchair_accessible,bikes_allowed

                    List<String> toks    = row.getValues();
                    String       routeId = getValue("route_id", map, toks,
                                               "");
                    String       tripId  = getValue("trip_id", map, toks, "");
                    if (tripToRoute.get(tripId) == null) {
                        tripToRoute.put(tripId, routeId);
                    }
                } catch (Exception exc) {
                    throw new RuntimeException(exc);
                }

                return row;
            }
        });
        Seesv csvUtil = new Seesv(new ArrayList<String>());
        csvUtil.process(textReader);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param idMap _more_
     * @param is _more_
     *
     * @throws Exception _more_
     */
    private void getRouteToAgency(final Request request,
                                  final Hashtable<String, String> idMap,
                                  InputStream is)
            throws Exception {
        TextReader textReader = new TextReader();
        textReader.setInput(is);
        textReader.addProcessor(new MyProcessor() {
            public org.ramadda.util.seesv.Row processRow(
                    TextReader textReader, org.ramadda.util.seesv.Row row) {
                try {
                    if (checkMap(row)) {
                        return row;
                    }
                    List<String> toks = row.getValues();
                    //route_id,agency_id,route_short_name,route_long_name,route_desc,route_type,route_url,route_color,route_text_color

                    String routeId  = getValue("route_id", map, toks, "");
                    String agencyId = getValue("agency_id", map, toks, "");
                    if (idMap.get(routeId) == null) {
                        idMap.put(routeId, agencyId);
                    }
                } catch (Exception exc) {
                    throw new RuntimeException(exc);
                }

                return row;
            }
        });
        Seesv csvUtil = new Seesv(new ArrayList<String>());
        csvUtil.process(textReader);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entries _more_
     * @param agencyMap _more_
     * @param routeMap _more_
     * @param pts _more_
     * @param services _more_
     * @param stopsMap _more_
     * @param stopTimes _more_
     * @param is _more_
     *
     * @throws Exception _more_
     */
    private void processTrips(final Request request,
                              final List<Entry> entries,
                              final Hashtable<String, Entry> agencyMap,
                              final Hashtable<String, Entry> routeMap,
                              final Hashtable<String, List<float[]>> pts,
                              final Hashtable<String, ServiceInfo> services,
                              final Hashtable<String, Entry> stopsMap,
                              final Hashtable<String,
                                  List<String[]>> stopTimes, InputStream is)
            throws Exception {

        final HashSet errors     = new HashSet();
        final Date    now        = new Date();
        final User    user       = request.getUser();
        TextReader    textReader = new TextReader();
        textReader.setInput(is);
        textReader.addProcessor(new MyProcessor() {

            int maxStops = 0;
            @Override
            public org.ramadda.util.seesv.Row processRow(
                    TextReader textReader, org.ramadda.util.seesv.Row row) {

                try {
                    if (checkMap(row)) {
                        return row;
                    }
                    List<String> toks = row.getValues();
                    //route_id,service_id,trip_id,trip_headsign,direction_id,block_id,shape_id
                    String routeId   = getValue("route_id", map, toks, "");
                    String serviceId = getValue("service_id", map, toks, "");

                    String serviceName = getValue("service_name", map, toks,
                                             "");
                    String tripId   = getValue("trip_id", map, toks, "");
                    String headsign = getValue("trip_headsign", map, toks,
                                          "");
                    String blockId = getValue("block_id", map, toks, "");
                    String shapeId = getValue("shape_id", map, toks, "");
                    TypeHandler typeHandler =
                        getRepository().getTypeHandler("type_gtfs_trip");
                    Entry entry =
                        typeHandler.createEntry(repository.getGUID());
                    Object[] values = typeHandler.getEntryValues(entry);

                    addPolygon(entry, shapeId, pts,
                               GtfsTripTypeHandler.IDX_POINTS);

                    Entry route = routeMap.get(routeId);


                    if (route.getProperty("added_a_shape") == null) {
                        addPolygon(route, shapeId, pts,
                                   GtfsRouteTypeHandler.IDX_POINTS);
                        route.putProperty("added_a_shape", "");
                    }


                    ServiceInfo serviceInfo = services.get(serviceId);
                    if (serviceInfo == null) {

                        if ( !errors.contains(serviceId)) {
                            errors.add(serviceId);
                            System.err.println("Could not find service:"
                                    + serviceId);
                        }
                        serviceInfo = new ServiceInfo(serviceId, "",
                                new boolean[] {
                            true, true, true, true, true, true, true
                        }, null, null);
                    }
                    List<String[]> stops = stopTimes.get(tripId);
                    values[GtfsTripTypeHandler.IDX_TRIP_ID] = tripId;
                    values[GtfsTripTypeHandler.IDX_HEADSIGN] =
                        getValue("trip_headsign", map, toks, "");
                    values[GtfsTripTypeHandler.IDX_DIRECTION] =
                        getValue("direction_id", map, toks, "0");
                    values[GtfsTripTypeHandler.IDX_BLOCK_ID] =
                        getValue("block_id", map, toks, "");
                    values[GtfsTripTypeHandler.IDX_WHEELCHAIR_ACCESSIBLE] =
                        getValue("wheelchair_accessible", map, toks, "0");
                    values[GtfsTripTypeHandler.IDX_BIKES_ALLOWED] =
                        getValue("bikes_allowed", map, toks, "0");
                    values[GtfsTripTypeHandler.IDX_SERVICE_ID] = serviceId;
                    values[GtfsTripTypeHandler.IDX_SERVICE_NAME] =
                        serviceInfo.name;
                    values[GtfsTripTypeHandler.IDX_WEEK] =
                        getRepository().encodeObject(serviceInfo.week);


                    if (stops == null) {
                        stops = new ArrayList<String[]>();
                    }
                    StringBuilder stopIds     = new StringBuilder();
                    String agencyId = (String) route.getProperty("agencyid");
                    Entry         agencyEntry = agencyMap.get(agencyId);

                    if (agencyEntry != null) {
                        values[GtfsTripTypeHandler.IDX_AGENCY_ID] =
                            agencyEntry.getId();
                    }

                    Entry firstStop = null;
                    Entry lastStop  = null;
                    for (String[] tuple : stops) {
                        stopIds.append("[");
                        stopIds.append(agencyId);
                        stopIds.append("-");
                        String stopId = tuple[0];
                        stopIds.append(stopId);
                        stopIds.append("]");

                        Entry stop = stopsMap.get(stopId);
                        if (firstStop == null) {
                            firstStop = stop;
                        }
                        lastStop = stop;
                        if (stop != null) {
                            String routes =
                                (String) stop.getStringValue(
                                    GtfsStopTypeHandler.IDX_ROUTES, "");
                            if (routes.indexOf(route.getId()) < 0) {
                                routes += route.getId();
                                routes += ",";
                                stop.setValue(GtfsStopTypeHandler.IDX_ROUTES,
                                        routes);
                            }
                            HashSet seen =
                                (HashSet) route.getProperty("seen_stops");
                            if ( !seen.contains(stop.getId())) {
                                seen.add(stop.getId());
                                ((StringBuilder) route.getProperty(
                                    "stop_names")).append(stop.getName());
                            }
                        }

                    }

                    String s1 = getRepository().encodeObject(stops);
                    String s2 = Utils.compress(s1);

                    if (stops.size() > maxStops) {
                        maxStops = stops.size();
                        /*
                        String msg = "trip: # stops:" + stops.size()
                                     + " compressed: " + s2.length()
                                     + " stop id size:" + stopIds.length();
                        System.err.println(msg);
                        */
                    }

                    String s = stopIds.toString();
                    if ((s2.length() > 30000) || (s.length() > 30000)) {
                        System.err.println("err:" + stops.size() + " "
                                           + s2.length() + " " + s.length()
                                           + " " + s.substring(0, 200));

                    }
                    values[GtfsTripTypeHandler.IDX_STOPS]    = s2;
                    values[GtfsTripTypeHandler.IDX_STOP_IDS] = s;

                    if (firstStop != null) {
                        values[GtfsTripTypeHandler.IDX_FIRST_STOP] =
                            firstStop.getId();
                    }
                    if (lastStop != null) {
                        values[GtfsTripTypeHandler.IDX_LAST_STOP] =
                            lastStop.getId();
                    }

                    String startTime = null;
                    String endTime   = null;
                    for (String[] tuple : stops) {
                        String stopId    = tuple[0];
                        String arrival   = tuple[1];
                        String departure = tuple[2];
                        if (startTime == null) {
                            startTime = arrival;
                        }
                        endTime = arrival;
                    }

                    if (startTime == null) {
                        startTime = "";
                    }
                    if (endTime == null) {
                        endTime = "";
                    }
                    values[GtfsTripTypeHandler.IDX_STARTTIME] = startTime;
                    values[GtfsTripTypeHandler.IDX_ENDTIME]   = endTime;

                    StringBuilder desc = new StringBuilder();


                    StringBuilder name = new StringBuilder();
                    if (Utils.stringDefined(routeId)) {
                        name.append("Rt. " + Gtfs.getRouteId(route));
                    } else {
                        //                        name.append(route.getName());
                    }
                    //                    name.append(": ");
                    if (Utils.stringDefined(headsign)) {
                        name.append(" to ");
                        name.append(headsign);
                    }

                    String weekLabel = serviceInfo.getLabel();
                    if (weekLabel.length() > 0) {
                        name.append(" - ");
                        name.append(weekLabel);
                    }
                    name.append(" - ");
                    name.append(startTime.replaceAll(":[^:]+$", ""));
                    name.append(" - ");
                    name.append(endTime.replaceAll(":[^:]+$", ""));
                    Resource resource = new Resource();
                    entry.initEntry(name.toString(), desc.toString(), route,
                                    user, resource, "", Entry.DEFAULT_ORDER,
                                    now.getTime(), now.getTime(),
                                    (serviceInfo.start != null)
                                    ? serviceInfo.start.getTime()
                                    : now.getTime(), (serviceInfo.end != null)
                            ? serviceInfo.end.getTime()
                            : now.getTime(), values);
                    route.getChildren().add(entry);

                    entries.add(entry);
                } catch (Exception exc) {
                    throw new RuntimeException(exc);
                }

                return row;

            }


        });

        Seesv csvUtil = new Seesv(new ArrayList<String>());
        csvUtil.process(textReader);

    }


    /** _more_ */
    private static int maxPts = 0;

    /**
     * _more_
     *
     * @param entry _more_
     * @param shapeId _more_
     * @param pts _more_
     * @param index _more_
     *
     * @throws Exception _more_
     */
    private void addPolygon(Entry entry, String shapeId,
                            Hashtable<String, List<float[]>> pts, int index)
            throws Exception {
        List<float[]> poly = pts.get(shapeId);
        if (poly == null) {
            System.err.println("Could not find shape:" + shapeId
                               + " for entry:" + entry.getName());

            return;
        }
        int skipEvery = 0;
        while (true) {
            StringBuilder all     = new StringBuilder();
            int           idx     = 0;
            float         north   = 0,
                          south   = 0,
                          west    = 0,
                          east    = 0;
            float         lastLat = Float.NaN;
            float         lastLon = Float.NaN;
            int           numPts  = 0;
            int           skipped = 0;
            for (float[] point : poly) {
                float lat = point[0];
                float lon = point[1];
                if ((lat == lastLat) && (lon == lastLon)) {
                    skipped++;

                    continue;
                }
                numPts++;
                if ((skipEvery > 0) && (numPts % skipEvery) == 0) {
                    skipped++;

                    continue;
                }

                lastLat = lat;
                lastLon = lon;
                all.append(lat);
                all.append(",");
                all.append(lon);
                all.append(";");

                north = (idx == 0)
                        ? lat
                        : Math.max(north, lat);
                south = (idx == 0)
                        ? lat
                        : Math.min(south, lat);
                west  = (idx == 0)
                        ? lon
                        : Math.min(west, lon);
                east  = (idx == 0)
                        ? lon
                        : Math.max(east, lon);

                idx++;
            }

            String compressed = Utils.compress(all.toString());
            if (numPts > maxPts) {
                maxPts = numPts;
                /*
                  System.out.println("Points:" + numPts + " skipped:" + skipped
                  + " text:" + all.length() + " compressed:"
                  + compressed.length() + " as array:"
                  + compressed2.length());
                */
            }
            if (compressed.length() > 35000) {
                /*
                System.out.println("Points:" + numPts + " skipevery:" + skipEvery+" skipped:" + skipped
                                   + " text length:" + all.length() + " compressed:"
                                   + compressed.length());
                */
                if (skipEvery == 0) {
                    skipEvery = 10;
                } else {
                    skipEvery--;
                }
                if (skipEvery < 0) {
                    throw new IllegalArgumentException(
                        "Too many points in polygon:" + poly.size());
                }

                continue;
            }
            Object[] values = entry.getTypeHandler().getEntryValues(entry);
            values[index] = compressed;
            if (idx > 0) {
                entry.setNorth(north);
                entry.setWest(west);
                entry.setSouth(south);
                entry.setEast(east);
            }

            break;
        }
    }


    /**
     * Class description
     *
     *
     * @version        $version$, Mon, May 16, '16
     * @author         Enter your name here...
     */
    private static class MyProcessor extends Processor {

        /** _more_ */
        Hashtable<String, Integer> map;

        /** _more_ */
        List<String> header = new ArrayList<String>();

        /**
         * _more_
         *
         * @param row _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        public boolean checkMap(Row row) throws Exception {
            if (map == null) {
                List<String> toks = row.getValues();
                map = new Hashtable<String, Integer>();
                for (int i = 0; i < toks.size(); i++) {
                    String tok = toks.get(i).trim();
                    //I've seen some odd chars in the header
                    tok = Utils.removeNonAscii(tok, "");
                    if (debug) {
                        System.err.println("tok:" + tok);
                    }
                    header.add(tok);
                    map.put(tok, Integer.valueOf(i));
                }

                return true;
            }

            return false;
        }
    }




    /**
     * _more_
     *
     * @param id _more_
     * @param map _more_
     * @param toks _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    private String getValue(String id, Hashtable<String, Integer> map,
                            List<String> toks, String dflt) {
        Integer idx = map.get(id);
        if (idx == null) {
            if (debug) {
                System.err.println("getValue: no idx:" + id + ": " + map);
            }

            return dflt;
        }
        String s = toks.get(idx);
        if (s == null) {
            if (debug) {
                System.err.println("getValue: null tok");
            }

            return dflt;
        }
        if (s.length() == 0) {
            if (debug) {
                System.err.println("0 length");
            }

            return dflt;
        }
        if (debug) {
            System.err.println("s:" + s);
        }

        return s;
    }

    /**
     * Class description
     *
     *
     * @version        $version$, Sun, May 22, '16
     * @author         Enter your name here...
     */
    public static class ServiceInfo {

        /** _more_ */
        String id;

        /** _more_ */
        String name;

        /** _more_ */
        boolean[] week;

        /** _more_ */
        Date start;

        /** _more_ */
        Date end;

        /**
         * _more_
         *
         * @param id _more_
         * @param name _more_
         * @param week _more_
         * @param start _more_
         * @param end _more_
         */
        public ServiceInfo(String id, String name, boolean[] week,
                           Date start, Date end) {
            this.id    = id;
            this.name  = name;
            this.week  = week;
            this.start = start;
            this.end   = end;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public String getLabel() {
            if (Utils.stringDefined(name)) {
                return name;
            }

            return Gtfs.getWeekString(this.week);
        }


    }






}
