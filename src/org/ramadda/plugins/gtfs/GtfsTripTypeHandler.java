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

package org.ramadda.plugins.gtfs;


import org.ramadda.repository.*;
import org.ramadda.repository.map.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;

import org.ramadda.util.JQuery;
import org.ramadda.util.Utils;
import org.ramadda.util.WikiUtil;


import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;


import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;


import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;
import java.util.TimeZone;


/**
 *
 *
 */
public class GtfsTripTypeHandler extends GenericTypeHandler {

    /** _more_ */
    public static final String TYPE_TRIP = "type_gtfs_trip";

    /** _more_ */
    private static int IDX = 0;

    /** _more_ */
    public static final int IDX_TRIP_ID = IDX++;

    /** _more_ */
    public static final int IDX_DIRECTION = IDX++;

    /** _more_ */
    public static final int IDX_HEADSIGN = IDX++;

    /** _more_ */
    public static final int IDX_STARTTIME = IDX++;

    /** _more_ */
    public static final int IDX_ENDTIME = IDX++;

    /** _more_ */
    public static final int IDX_SERVICE_ID = IDX++;

    /** _more_ */
    public static final int IDX_SERVICE_NAME = IDX++;

    /** _more_ */
    public static final int IDX_WEEK = IDX++;


    /** _more_ */
    public static final int IDX_STOPS = IDX++;

    /** _more_ */
    public static final int IDX_STOP_IDS = IDX++;

    /** _more_ */
    public static final int IDX_BLOCK_ID = IDX++;

    /** _more_ */
    public static final int IDX_WHEELCHAIR_ACCESSIBLE = IDX++;

    /** _more_ */
    public static final int IDX_BIKES_ALLOWED = IDX++;

    /** _more_ */
    public static final int IDX_POINTS = IDX++;

    /** _more_ */
    public static final int IDX_AGENCY_ID = IDX++;

    /** _more_ */
    public static final int IDX_FIRST_STOP = IDX++;

    /** _more_ */
    public static final int IDX_LAST_STOP = IDX++;



    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public GtfsTripTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }

    /**
     * _more_
     *
     * @param wikiUtil _more_
     * @param request _more_
     * @param originalEntry _more_
     * @param entry _more_
     * @param tag _more_
     * @param props _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props)
            throws Exception {

        if (tag.equals("gtfs.trip.title")) {
            StringBuilder sb = new StringBuilder();
            sb.append(HtmlUtils.cssLink(getRepository().getUrlBase()
                                        + "/gtfs/gtfs.css"));
            Entry route = entry.getAncestor("type_gtfs_route");
            if (route != null) {
                HtmlUtils.div(sb, Gtfs.getRouteTitle(request, route, true),
                              HtmlUtils.cssClass("ramadda-page-title"));
            }


            String headsign =
                (String) entry.getValue(GtfsTripTypeHandler.IDX_HEADSIGN,
                                        (String) null);
            String sked = Gtfs.getWeekString(
                              (boolean[]) getRepository().decodeObject(
                                  entry.getValue(IDX_WEEK, "")));
            if ( !Utils.stringDefined(sked)) {
                sked = "No scheduled days";
            }

            String label = (Utils.stringDefined(headsign)
                            ? "To " + headsign + " - "
                            : "") + sked + " - " + Gtfs.getTimeRange(entry);
            HtmlUtils.div(
                sb, getPageHandler().getEntryHref(request, entry, label),
                HtmlUtils.cssClass("ramadda-page-heading"));

            return sb.toString();
        }

        if (tag.equals("gtfs.trip.info")) {
            StringBuilder sb = new StringBuilder();
            sb.append(HtmlUtils.formTable());
            Entry route = entry.getAncestor("type_gtfs_route");
            if (route != null) {
                ((GtfsRouteTypeHandler) route.getTypeHandler()).addRouteInfo(
                    request, route, sb);
            }
            sb.append(HtmlUtils.formEntry(msgLabel("Direction"),
                                          getFieldHtml(request, entry,
                                              "direction")));
            sb.append(HtmlUtils.formEntry(msgLabel("Wheelchair accessible"),
                                          getFieldHtml(request, entry,
                                              "wheelchair_accessible")));
            sb.append(HtmlUtils.formEntry(msgLabel("Bikes allowed"),
                                          getFieldHtml(request, entry,
                                              "bikes_allowed")));
            sb.append(HtmlUtils.formTableClose());

            return sb.toString();
        }

        if (tag.equals("gtfs.trip.schedule")) {
            StringBuilder sb     = new StringBuilder();
            String        suffix = "";
            if (entry.hasDate()) {
                Date now = new Date();
                String dttm = getDateHandler().formatYYYYMMDD(
                                  new Date(entry.getStartDate())) + " - "
                                      + getDateHandler().formatYYYYMMDD(
                                          new Date(entry.getEndDate()));
                if ((now.getTime() < entry.getStartDate())
                        || (now.getTime() > entry.getEndDate())) {
                    sb.append(
                        HtmlUtils.note(
                            HtmlUtils.b(
                                "Out of service. Only valid between: "
                                + dttm)));
                } else {
                    suffix = " (" + msg("effective") + " " + dttm + ")";
                }
            }

            sb.append("\n:heading Schedule" + suffix + "\n");
            showSchedule(request, sb, entry);

            return sb.toString();
        }

        if (tag.equals("gtfs.trip.list")) {
            Entry agency =
                entry.getAncestor(GtfsAgencyTypeHandler.TYPE_AGENCY);
            List<Entry> vehicles = Gtfs.getVehiclesForTrip(request, agency,
                                       entry);
            Hashtable<String, Entry> vehicleMap = new Hashtable<String,
                                                      Entry>();
            for (Entry vehicle : vehicles) {
                vehicleMap.put(
                    (String) vehicle.getValue(
                        GtfsVehicleTypeHandler.IDX_STOP_ID, ""), vehicle);
            }

            StringBuilder tmp = new StringBuilder();
            List<Gtfs.StopTime> stops = Gtfs.getStopsForTrip(request, entry,
                                            null);
            StringBuilder sb = new StringBuilder();
            sb.append(HtmlUtils.cssLink(getRepository().getUrlBase()
                                        + "/gtfs/gtfs.css"));

            if ((stops != null) && (stops.size() > 0)) {
                sb.append(
                    "<div style=\"max-height:400px; overflow-y:auto; border-bottom: 1px #cccccc solid; border-top: 1px #cccccc solid;\" >");
                sb.append(
                    "<table id=\"gtfs-stops-list-" + entry.getId()
                    + "\" class=\"gtfs-table gtfs-stops-list\" width=\"100%\">\n");
                sb.append("<tr>");
                sb.append(HtmlUtils.col("Stop",
                                        HtmlUtils.cssClass("gtfs-header")));
                sb.append(HtmlUtils.col("Arrival",
                                        HtmlUtils.cssClass("gtfs-header")));
                sb.append(HtmlUtils.col("Departure",
                                        HtmlUtils.cssClass("gtfs-header")));
                sb.append(HtmlUtils.col("&nbsp;",
                                        HtmlUtils.cssClass("gtfs-header")));
                sb.append("</tr>");
                Entry myRoute = entry.getParentEntry();
                for (Gtfs.StopTime stopTime : stops) {
                    String stopId = (String) stopTime.entry.getValue(
                                        GtfsStopTypeHandler.IDX_STOP_ID, "");
                    Entry vehicle = vehicleMap.get(stopId);
                    String lbl =
                        HtmlUtils.href(
                            request.entryUrl(
                                getRepository().URL_ENTRY_SHOW,
                                stopTime.entry), stopTime.entry.getLabel());
                    if (vehicle != null) {
                        lbl = getPageHandler()
                            .getEntryHref(request, vehicle, HtmlUtils
                                .img(vehicle.getTypeHandler()
                                    .getTypeIconUrl()) + " "
                                        + vehicle.getName() + " "
                                        + vehicle
                                            .getValue(GtfsVehicleTypeHandler
                                                .IDX_STATUS, "")) + "<br>"
                                                    + lbl;
                    }
                    String arrival   = Gtfs.formatTime(stopTime.startTime);
                    String departure = Gtfs.formatTime(stopTime.endTime);
                    List<Entry> routes = Gtfs.getRoutesForStop(request,
                                             stopTime.entry);
                    //                    StringBuilder routesSB = new StringBuilder("<div style=\"max-width:100%;overflow-x:auto;\"><table cellspacing=0 cellpadding=0><tr style=\"border-bottom:0px;\">");
                    StringBuilder routesSB = new StringBuilder("");
                    for (Entry route : routes) {
                        if (route.getId().equals(myRoute.getId())) {
                            continue;
                        }
                        //                        routesSB.append("<td style=\"padding:0px;\" >");
                        routesSB.append(Gtfs.getRouteTitle(request, route,
                                true, false));
                        //                        routesSB.append("</td>");
                    }
                    //                    routesSB.append("</tr></table></div>");
                    sb.append(HtmlUtils
                        .row(HtmlUtils
                            .cols(lbl, arrival, departure, routesSB
                                .toString()), " valign=top "
                                    + HtmlUtils
                                        .attr("data-mapid", stopTime.entry
                                            .getId()) + HtmlUtils
                                                .attr("data-latitude", ""
                                                    + stopTime.entry
                                                        .getLatitude()) + HtmlUtils
                                                            .attr("data-longitude", ""
                                                                + stopTime
                                                                    .entry.getLongitude())));
                }
                sb.append("</table>");
                sb.append("</div>");

                String js = "highlightMarkers('#gtfs-stops-list-"
                            + entry.getId()
                            + "  tr', gtfsTripMap, '#ffffcc', 'white', '"
                            + entry.getId() + "');";
                sb.append(HtmlUtils.script(JQuery.ready(js)));
            }

            return sb.toString();
        }

        return super.getWikiInclude(wikiUtil, request, originalEntry, entry,
                                    tag, props);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     * @param entry _more_
     *
     * @throws Exception On badness
     */
    public void showSchedule(Request request, Appendable sb, Entry entry)
            throws Exception {
        boolean[] week =
            (boolean[]) getRepository().decodeObject(entry.getValue(IDX_WEEK,
                ""));
        sb.append(
            "<table class=\"gtfs-table gtfs-schedule\" width=\"100%\">\n");
        sb.append("<tr>");
        for (int i = 0; i < 7; i++) {
            sb.append(HtmlUtils.td(Gtfs.DAYS_MEDIUM[i],
                                   HtmlUtils.cssClass("gtfs-header")));
        }
        sb.append("</tr>\n");
        sb.append("<tr>");
        for (int i = 0; i < 7; i++) {
            boolean on = ((week != null)
                          ? week[i]
                          : false);
            sb.append(HtmlUtils.td(on
                                   ? "ON"
                                   : "&nbsp;", HtmlUtils.cssClass(on
                    ? "gtfs-day gtfs-day-on"
                    : "gtfs-day gtfs-day-off")));
        }
        sb.append("</tr>\n");
        sb.append("</table>\n");
    }

    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    @Override
    public String getEntryName(Entry entry) {
        return entry.getName();
    }

    /**
     * _more_
     *
     * @param entry _more_
     * @param mapInfo _more_
     * @param sb _more_
     */
    @Override
    public void initMapAttrs(Entry entry, MapInfo mapInfo, StringBuilder sb) {
        super.initMapAttrs(entry, mapInfo, sb);
        Entry route = entry.getAncestor("type_gtfs_route");
        String color =
            getColor(route.getValue(GtfsRouteTypeHandler.IDX_COLOR,
                                    "#ff0000"));
        sb.append("'strokeColor':'" + color + "','strokeWidth':4");
    }

    /**
     * _more_
     *
     * @param c _more_
     *
     * @return _more_
     */
    public String getColor(String c) {
        if (c.startsWith("#")) {
            return c;
        }

        return "#" + c;
    }

    /**
     *
     * @param request _more_
     * @param entry _more_
     * @param map _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public boolean addToMap(Request request, Entry entry, MapInfo map)
            throws Exception {
        super.addToMap(request, entry, map);
        //Note: This is a cut-and-paste from GtfsRouteTypeHandler
        String s = entry.getValue(IDX_POINTS, "");
        if (Utils.stringDefined(s)) {
            s = Utils.uncompress(s);
            List<double[]> points = new ArrayList<double[]>();
            Utils.parsePointString(s, points);
            map.addLines(entry, entry.getId() + "_polygon", points, null);
        }
        List<Gtfs.StopTime> stops = Gtfs.getStopsForTrip(request, entry,
                                        null);
        if (stops != null) {
            for (Gtfs.StopTime stopTime : stops) {
                map.addMarker(request, stopTime.entry);
            }
        }

        Entry       route    = entry.getAncestor("type_gtfs_route");
        Entry agency = entry.getAncestor(GtfsAgencyTypeHandler.TYPE_AGENCY);
        List<Entry> vehicles = Gtfs.getVehiclesForTrip(request, agency,
                                   entry);
        Gtfs.addToMap(request, vehicles, map);

        return false;
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param trips _more_
     * @param onlyToday _more_
     * @param onlyFuture _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */

}
