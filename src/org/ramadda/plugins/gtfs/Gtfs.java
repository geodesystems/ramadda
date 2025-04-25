/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.gtfs;

import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;

import org.json.*;

import org.ramadda.repository.*;
import org.ramadda.repository.map.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;

import org.ramadda.util.BufferMapList;
import org.ramadda.util.HtmlUtils;

import org.ramadda.util.TTLCache;
import org.ramadda.util.Utils;
import org.ramadda.util.WikiUtil;

import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.net.*;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;

import java.util.Hashtable;
import java.util.List;
import java.util.TimeZone;

/**
 *
 *
 */
@SuppressWarnings("unchecked")
public class Gtfs implements Constants {

    public static final String ARG_DATE = "date";

    public static final String ARG_ALLDATES = "alldates";

    public static String[] DAYS_FULL = {
        "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday",
        "Saturday"
    };

    public static String[] DAYS_MEDIUM = {
        "Sun", "Mon", "Tues", "Wed", "Thurs", "Fri", "Sat"
    };

    public static String[] DAYS_SHORT = {
        "Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"
    };

    public static String formatTime(String s) {
        List<String> toks = StringUtil.split(s, ":", true, true);
        if (toks.size() == 0) {
            return "NA";
        }
        String hh = toks.get(0);
        String suffix;
        int    hour = Integer.parseInt(hh.replace("^0", ""));
        if (hour > 12) {
            hour   -= 12;
            suffix = "&nbsp;pm";
        } else {
            suffix = "&nbsp;am";
        }
        hh = "&nbsp;" + hour;
        if (toks.size() == 1) {
            return hh + suffix;
        }

        return hh + ":" + toks.get(1) + suffix;

    }

    public static String formatDateRange(Request request, Entry trip)
            throws Exception {
        return request.getRepository().getDateHandler()
            .formatYYYYMMDD(new Date(trip.getStartDate())) + " - "
                + request.getRepository().getDateHandler()
                    .formatYYYYMMDD(new Date(trip.getEndDate()));
    }

    public static String getTimeRange(Request request,Entry entry) {
        return formatStartTime(request,entry) + " - " + formatEndTime(request,entry);
    }

    public static String formatStartTime(Request request,Entry entry) {
        return formatTime(entry.getStringValue(request,GtfsTripTypeHandler.IDX_STARTTIME,
                                         ""));
    }

    public static String formatEndTime(Request request,Entry entry) {
        return formatTime(entry.getStringValue(request,GtfsTripTypeHandler.IDX_ENDTIME,
                                         ""));
    }

    public static String getWeekString(boolean[] week) {
        if (week == null) {
            return "";
        }
        boolean anyWeekdays = week[1] || week[2] || week[3] || week[4]
                              || week[5];
        boolean weekdays = week[1] && week[2] && week[3] && week[4]
                           && week[5];
        boolean weekend    = week[0] && week[6];
        boolean anyWeekend = week[0] || week[6];
        if ( !anyWeekend && weekdays) {
            return "Weekday";
        } else if ( !anyWeekdays && weekend) {
            return "Weekend";
        } else if (weekdays && weekend) {
            return "Weekly";
        } else if (week[0] && !anyWeekdays) {
            return "Sunday";
        } else if (week[6] && !anyWeekdays) {
            return "Saturday";
        }
        StringBuilder sked     = new StringBuilder();
        String        longName = null;
        int           cnt      = 0;
        boolean       seenOne  = false;
        int           firstOn  = -1;
        int           lastOn   = -1;
        boolean       seenOff  = false;
        for (int i = 0; i < week.length; i++) {
            if (week[i]) {
                if (firstOn == -1) {
                    firstOn = i;
                }
                lastOn = i;
            }
        }

        if (firstOn >= 0) {
            boolean contiguous = true;
            for (int i = firstOn + 1; i < lastOn; i++) {
                if ( !week[i]) {
                    contiguous = false;

                    break;
                }
            }
            if (contiguous) {
                if (firstOn == lastOn) {
                    return Gtfs.DAYS_FULL[firstOn];
                }

                return Gtfs.DAYS_FULL[firstOn] + " - "
                       + Gtfs.DAYS_FULL[lastOn];
            }
        }

        for (int i = 0; i < week.length; i++) {
            if (week[i]) {
                if (sked.length() > 0) {
                    sked.append("-");
                }
                longName = Gtfs.DAYS_SHORT[i];
                cnt++;
                sked.append(Gtfs.DAYS_SHORT[i]);
            }
        }
        if (cnt == 1) {
            return longName;
        }

        return sked.toString();
    }

    public static String getRouteId(Request request,Entry entry) {
        return entry.getStringValue(request,GtfsRouteTypeHandler.IDX_ID, "");
    }

    public static Date addDateLabel(Request request, Entry entry,
                                    Appendable sb)
            throws Exception {
        Entry agency = entry.getAncestor(GtfsAgencyTypeHandler.TYPE_AGENCY);
        SimpleDateFormat sdf    = new SimpleDateFormat("EEEE, MMMM d HH:mm");
        SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd");
        if (agency != null) {
            String tz = agency.getStringValue(request,3, "");
            if (Utils.stringDefined(tz)) {
                TimeZone timeZone = TimeZone.getTimeZone(tz);
                sdf.setTimeZone(timeZone);
                parser.setTimeZone(timeZone);
            }
        }
        Date   now       = new Date();
        String dateLabel = "Today";
        if (request.exists(ARG_DATE)) {
            String dttm = request.getString(ARG_DATE, "today");
            if (dttm.equals("today")) {
                dateLabel = "Today";
            } else if (dttm.equals("tomorrow")) {}
            else {
                now = parser.parse(dttm);
            }
            dateLabel = "Day";
        }
        if (sb != null) {
            sb.append(
                HtmlUtils.div(
                    dateLabel + ": " + sdf.format(now),
                    HtmlUtils.cssClass("ramadda-page-heading")));
        }

        return now;
    }

    public static void displayTrips(final Request request, Entry entry,
                                    List<TripInfo> trips, Appendable sb,
                                    Date now, Entry stopEntry,
                                    boolean showRoute, boolean... flags)
            throws Exception {

        String stopId = ((stopEntry != null)
                         ? stopEntry.getStringValue(request,
                             GtfsStopTypeHandler.IDX_STOP_ID, null)
                         : null);
        Hashtable<String, Entry> stopMap = new Hashtable<String, Entry>();

        StringBuilder            nextSB  = new StringBuilder();

        boolean                  open    = (flags.length > 0)
                                           ? flags[0]
                                           : true;

        final PageHandler pageHandler =
            request.getRepository().getPageHandler();

        final List<String> titles   = new ArrayList<String>();
        final List<String> contents = new ArrayList<String>();

        Hashtable<String, TripBlob> blobMap = new Hashtable<String,
                                                  TripBlob>();
        List<String>   blobs        = new ArrayList<String>();

        boolean        sawDir0      = false;
        boolean        sawDir1      = false;
        boolean        anyActive    = false;

        List<TripInfo> currentTrips = new ArrayList<TripInfo>();

        for (TripInfo tripInfo : trips) {
            if (tripInfo.getRunningNow()) {
                currentTrips.add(tripInfo);
            }
            Entry tripEntry = tripInfo.getEntry();

            Entry route     = showRoute
                              ? tripEntry.getAncestor("type_gtfs_route")
                              : null;
            String dir =
                tripEntry.getStringValue(request,GtfsTripTypeHandler.IDX_DIRECTION, "0");

            boolean inService = true;
            if ((now.getTime() < tripEntry.getStartDate())
                    || (now.getTime() > tripEntry.getEndDate())) {
                inService = false;
            }

            boolean active = inService && tripInfo.getScheduleOk();
            if (active) {
                anyActive = true;
            }
            String key =
                tripEntry.getStringValue(request,GtfsTripTypeHandler.IDX_SERVICE_ID, "")
                + "-" + dir;
            if (route != null) {
                key += "-" + route.getId();
            }

            String serviceName =
                tripEntry.getStringValue(request,GtfsTripTypeHandler.IDX_SERVICE_NAME, "");
            if ( !Utils.stringDefined(serviceName)) {
                serviceName = tripInfo.getWeekString();
            }

            String headsign =
                tripEntry.getStringValue(request,GtfsTripTypeHandler.IDX_HEADSIGN, "");

            if (Utils.stringDefined(headsign)) {
                serviceName += " " + headsign;
            } else {
                if (dir.equals("0")) {
                    sawDir0     = true;
                    serviceName += "{outbound}";
                } else {
                    sawDir1     = true;
                    serviceName += "{inbound}";
                }
            }
            if ( !active) {
                serviceName = "Off Schedule - " + serviceName;
            } else if ( !inService) {
                serviceName = "Out of Service - " + serviceName;
            } else {
                serviceName = "Schedule - " + serviceName;
            }
            String longServiceName = serviceName;
            if (route != null) {
                longServiceName += " - "
                                   + getRouteTitle(request, route, false);
            }
            TripBlob blob = blobMap.get(key);
            if (blob == null) {
                blob = new TripBlob(tripInfo, serviceName, longServiceName,
                                    inService);
                blobMap.put(key, blob);
                blobs.add(key);
                if ( !showRoute) {
                    String dttm = formatDateRange(request, tripEntry);
                    if ( !inService) {
                        blob.header.append(
                            HtmlUtils.note(
                                HtmlUtils.b(
                                    "Out of service. Only valid between: "
                                    + dttm)));
                    } else {
                        blob.header.append(
                            HtmlUtils.center(
                                HtmlUtils.b("Service valid: " + dttm)));
                    }
                }
            }

            Appendable buff = tripInfo.getInPast()
                              ? blob.buff1
                              : blob.buff2;
            HtmlUtils.open(buff, "tr",
                           HtmlUtils.attr("valign", "top")
                           + HtmlUtils.cssClass(inService
                    ? tripInfo.getCssClass()
                    : "gtfs-trip-list-oos"));

            String tripLabel1 = HtmlUtils.div(tripInfo.getStartTimeLabel(),
                                    HtmlUtils.cssClass("gtfs-timerange"));
            String tripLabel2 = HtmlUtils.div(tripInfo.getEndTimeLabel(),
                                    HtmlUtils.cssClass("gtfs-timerange"));
            HtmlUtils.td(buff,
                         pageHandler.getEntryHref(request, tripEntry,
                             tripLabel1));
            HtmlUtils.td(buff,
                         pageHandler.getEntryHref(request, tripEntry,
                             tripLabel2));

            if (showRoute && (route != null)) {
                HtmlUtils.td(buff, getRouteTitle(request, route, true), "");
            } else {
                HtmlUtils.td(buff, "", "");
            }
            if (inService && active && (blob.nextTrip == null)
                    && !tripInfo.getInPast()) {
                blob.nextTrip = tripInfo;
            }
            HtmlUtils.td(buff, "", "");
            buff.append("</tr>\n");
        }

        if ( !anyActive) {
            //            sb.append(pageHandler.showDialogWarning("No active trips today"));
        }

        List<String> firstTitles    = new ArrayList<String>();
        List<String> firstContents  = new ArrayList<String>();
        List<String> secondTitles   = new ArrayList<String>();
        List<String> secondContents = new ArrayList<String>();

        List<String> nextTrips      = new ArrayList<String>();

        for (int i = 0; i < blobs.size(); i++) {
            TripBlob blob  = blobMap.get(blobs.get(i));
            String   title = blob.longTitle;
            if ( !sawDir0 || !sawDir1) {
                title = title.replace("{outbound}", "").replace("{inbound}",
                                      "");
                blob.title = blob.title.replace("{outbound}",
                        "").replace("{inbound}", "");
            } else {
                title = title.replace("{outbound}",
                                      " - Outbound").replace("{inbound}",
                                          " - Inbound");
                blob.title = blob.title.replace("{outbound}",
                        " - Outbound").replace("{inbound}", " - Inbound");
            }

            String startLabel = "Start";
            String endLabel   = "End";
            if (blob.tripInfo.getFirstStop() != null) {
                startLabel = blob.tripInfo.getFirstStop().getName();
            }
            if (blob.tripInfo.getLastStop() != null) {
                endLabel = blob.tripInfo.getLastStop().getName();
            }

            if (blob.nextTrip != null) {
                TripInfo tripInfo  = blob.nextTrip;
                Entry    tripEntry = blob.nextTrip.entry;
                String tripLabel = tripInfo.getTimeLabel() + ": "
                                   + blob.title;
                StringBuilder nextTripSB =
                    new StringBuilder(HtmlUtils.open("div",
                        HtmlUtils.cssClass("gtfs-trip-next")));
                Entry route = tripEntry.getAncestor("type_gtfs_route");
                if (showRoute) {
                    if (route != null) {
                        nextTripSB.append(HtmlUtils.td(getRouteTitle(request,
                                route, true), ""));
                        nextTripSB.append(HtmlUtils.br());
                    }
                }

                nextTripSB.append(pageHandler.getEntryHref(request,
                        tripEntry, tripLabel));

                List<StopTime> stops = getStopsForTrip(request, tripEntry,
                                           stopMap);
                if (stops.size() > 0) {
                    startLabel = stops.get(0).entry.getName();
                    endLabel   = stops.get(stops.size() - 1).entry.getName();

                    StringBuilder tmp =
                        new StringBuilder("<table class=gtfs-stops-list>");
                    for (StopTime stopTime : stops) {
                        String label = formatTime(stopTime.startTime);
                        tmp.append("<tr>");
                        tmp.append(HtmlUtils.td(label,
                                HtmlUtils.cssClass("gtfs-timerange")));
                        String stopHref = pageHandler.getEntryHref(request,
                                              stopTime.entry);
                        tmp.append(HtmlUtils.td(stopHref, ""));

                        if ((stopEntry != null)
                                && stopTime.entry.getId().equals(
                                    stopEntry.getId())) {
                            String routeText = "";
                            if (route != null) {
                                routeText = pageHandler.getEntryHref(request,
                                        tripEntry,
                                        getRouteTitle(request, route, false));
                            }
                            nextSB.append(HtmlUtils.formTable());
                            nextSB.append(
                                HtmlUtils.row(
                                    HtmlUtils.cols(routeText, label, "")));
                            nextSB.append(HtmlUtils.formTableClose());
                        }

                    }
                    tmp.append("</table>");
                    String stopsHtml =
                        HtmlUtils
                            .makeShowHideBlock("Stops", tmp
                                .toString(), false, HtmlUtils
                                .cssClass("entry-toggleblock-label"), "", request
                                .getRepository()
                                .getIconUrl("ramadda.icon.togglearrowdown"), request
                                .getRepository()
                                .getIconUrl("ramadda.icon.togglearrowright"));

                    nextTripSB.append(HtmlUtils.div(stopsHtml,
                            HtmlUtils.cssClass("gtfs-stops")));
                }
                nextTripSB.append(HtmlUtils.close("div"));
                nextTrips.add(nextTripSB.toString());
            }

            Appendable buff = new StringBuilder();
            buff.append(blob.header);
            HtmlUtils.open(buff, "table", "class",
                           "gtfs-table gtfs-trips-list");
            HtmlUtils.open(buff, "tr", HtmlUtils.cssClass("gtfs-header"));
            HtmlUtils.col(
                buff,
                HtmlUtils.tag(
                    "div", "style=\"width:100%;overflow-x:auto\"",
                    startLabel));
            HtmlUtils.col(
                buff,
                HtmlUtils.tag(
                    "div", "style=\"width:100%;overflow-x:auto\"", endLabel));
            HtmlUtils.col(buff, "&nbsp;");
            HtmlUtils.col(buff, "&nbsp;");
            HtmlUtils.close(buff, "tr");
            if ((blob.buff1.length() > 0) && (blob.buff2.length() > 0)) {
                buff.append(blob.buff1);
                buff.append(blob.buff2);
            } else {
                buff.append(blob.buff1);
                buff.append(blob.buff2);
            }
            HtmlUtils.close(buff, "table");
            buff.append("<p>");
            if (blob.tripInfo.getScheduleOk() && blob.inService) {
                firstTitles.add(title);
                firstContents.add(buff.toString());
            } else {
                secondTitles.add(title);
                secondContents.add(buff.toString());
            }
        }

        firstTitles.addAll(secondTitles);
        firstContents.addAll(secondContents);

        if (stopEntry != null) {
            if (nextSB.length() == 0) {
                nextSB.append(
                    HtmlUtils.note(
                        HtmlUtils.b("No scheduled transit service today")));
            } else {
                sb.append(HtmlUtils.b("Next Trip"));
                sb.append("\n");
            }
            StringBuilder rides = new StringBuilder(HtmlUtils.formTable());
            rides.append("\n");
            boolean didone = getUberInclude(request, stopEntry, rides);
            didone |= getLyftInclude(request, entry, rides);
            if (didone) {
                rides.append("\n");
                rides.append(HtmlUtils.formTableClose());
                rides.append("\n");
                List<String> tmptitles = new ArrayList<String>();
                List<String> tmptabs   = new ArrayList<String>();
                tmptitles.add("Car Service");
                tmptabs.add(rides.toString());
                HtmlUtils.makeAccordion(nextSB, tmptitles, tmptabs, true,
                                        "ramadda-accordion", null);
            }
        }

        if (nextSB.length() > 0) {
            sb.append("\n<nowiki>\n");
            sb.append(nextSB);
            sb.append("\n</nowiki>\n");
        }

        if ((stopEntry == null) && (currentTrips.size() > 0)) {
            for (TripInfo tripInfo : currentTrips) {
                StringBuffer tmp = new StringBuffer();
                String headsign = (String) tripInfo.entry.getStringValue(request,
                                      GtfsTripTypeHandler.IDX_HEADSIGN,
                                      (String) null);
                String label = (Utils.stringDefined(headsign)
                                ? "To " + headsign + " - "
                                : "") + Gtfs.getTimeRange(request,tripInfo.entry);
                HtmlUtils.div(
                    tmp,
                    entry.getTypeHandler().getPageHandler().getEntryHref(
                        request, tripInfo.entry,
                        HtmlUtils.img(
                            request.getRepository().getIconUrl(
                                "/icons/link.png"))), "");
                tmp.append(
                    tripInfo.entry.getTypeHandler().getWikiInclude(
                        new WikiUtil(), request, tripInfo.entry,
                        tripInfo.entry, "gtfs.trip.list", new Hashtable()));
                firstTitles.add(0, "Now: " + label);
                firstContents.add(0, tmp.toString());
            }
        }

        sb.append(HtmlUtils.hr());
        /*
        if (nextTrips.size() > 0) {
            sb.append(StringUtil.join("<br>", nextTrips));
            sb.append(HtmlUtils.br());
        }
        */

        if (open && (firstTitles.size() == 1)) {
            sb.append(firstTitles.get(0));
            sb.append(firstContents.get(0));
        } else if (firstTitles.size() > 0) {
            if (currentTrips.size() > 0) {
                open = true;
            }
            HtmlUtils.makeAccordion(sb, firstTitles, firstContents,
                                    !open || showRoute, "ramadda-accordion",
                                    null);
        }

    }

    public static boolean getUberInclude(Request request, Entry entry,
                                         Appendable sb)
            throws Exception {
        try {
            String uberKey =
                request.getRepository().getProperty("ramadda.uber.token",
                    null);

            if (uberKey == null) {
                //                System.err.println("No UBER token");
                return false;

            }
            String url =
                HtmlUtils.url("https://api.uber.com/v1/estimates/time",
                              "start_latitude", entry.getNorth(request) + "",
                              "start_longitude", entry.getWest(request) + "");
            HttpURLConnection huc =
                (HttpURLConnection) new URL(url).openConnection();
            huc.addRequestProperty("Authorization", "Token " + uberKey);
            String json = new String(IOUtil.readBytes(huc.getInputStream()));
            JSONObject obj = new JSONObject(new JSONTokener(json));
            if ( !obj.has("times")) {
                System.err.println("UBER: no times:" + json);

                return false;
            }

            JSONArray times = obj.getJSONArray("times");
            String pickupTemplate =
                "https://m.uber.com/ul?&action=setPickup&pickup[latitude]={lat}&pickup[longitude]={lon}&product_id={product}";
            if (times.length() == 0) {
                return false;
            }
            for (int i = 0; i < times.length(); i++) {
                JSONObject time    = times.getJSONObject(i);
                String     name    = time.getString("display_name");
                int        seconds = time.getInt("estimate");
                double     minutes = ((int) (seconds / 60.0 * 10)) / 10.0;
                String pickup = pickupTemplate.replace("{lat}",
                                    entry.getLatitude(request)
                                    + "").replace("{lon}",
                                        entry.getLongitude(request)
                                        + "").replace("{product}",
                                            time.getString("product_id"));
                if (i == 0) {
                    String cols =
                        HtmlUtils.col(HtmlUtils.b("UBER"))
                        + HtmlUtils.col(HtmlUtils.b("Minutes Away"),
                                        "colspan=2");
                    sb.append(HtmlUtils.row(cols));
                    sb.append("\n");
                }

                sb.append(HtmlUtils.row(HtmlUtils.cols(name, "" + minutes,
                        HtmlUtils.href(pickup, "Request"))));
                sb.append("\n");
            }
        } catch (Exception exc) {
            System.err.println("Uber include error:" + exc);

            return false;
        }

        return true;
    }

    private static String lyftToken;

    public static String getLyftToken(Request request, boolean force)
            throws Exception {
        HttpURLConnection huc = null;
        try {
            if ((lyftToken != null) && !force) {
                return lyftToken;
            }
            String lyftId =
                request.getRepository().getProperty("ramadda.lyft.clientid",
                    null);

            String lyftSecret =
                request.getRepository().getProperty("ramadda.lyft.secret",
                    null);

            if ((lyftId == null) || (lyftSecret == null)) {
                //                System.err.println("No lyft secret");

                return null;
            }
            String auth = lyftId + ":" + lyftSecret;
            auth = Utils.encodeBase64(auth);
            String url = "https://api.lyft.com/oauth/token";
            huc = (HttpURLConnection) new URL(url).openConnection();

            String data =
                "{\"grant_type\": \"client_credentials\", \"scope\": \"public\"}";
            byte[] postData       = data.getBytes();
            int    postDataLength = postData.length;
            huc.setDoOutput(true);
            huc.setInstanceFollowRedirects(false);
            huc.setRequestMethod("POST");
            huc.setRequestProperty("Content-Type", "application/json");
            huc.setRequestProperty("Content-Length",
                                   Integer.toString(postDataLength));
            huc.addRequestProperty("Authorization", "Basic " + auth);
            huc.setUseCaches(false);
            DataOutputStream wr = new DataOutputStream(huc.getOutputStream());
            wr.write(postData);
            IOUtil.close(wr);
            String json = new String(IOUtil.readBytes(huc.getInputStream()));
            JSONObject obj = new JSONObject(new JSONTokener(json));
            if ( !obj.has("access_token")) {
                System.err.println("No access token from Lyft:" + json);

                return null;
            }

            return lyftToken = obj.getString("access_token");
        } catch (Exception exc) {
            System.err.println("Error doing Lyft authentication:" + exc);
            if (huc != null) {
                System.err.println("Error:" + huc.getResponseMessage());
            }
        }

        return null;
    }

    public static boolean getLyftInclude(Request request, Entry entry,
                                         Appendable sb)
            throws Exception {
        try {
            String token = getLyftToken(request, false);
            if (token == null) {
                return false;
            }
            //We try a couple times since the first time might have an expired token
            if ( !getLyftIncludeInner(request, entry, sb, token)) {
                return getLyftIncludeInner(request, entry, sb,
                                           getLyftToken(request, false));
            }
        } catch (Exception exc) {
            System.err.println("Lyft include error:" + exc);

            return false;
        }

        return false;
    }

    private static boolean getLyftIncludeInner(Request request, Entry entry,
            Appendable sb, String token)
            throws Exception {
        if (token == null) {
            return false;
        }
        String json = null;
        try {
            String url = HtmlUtils.url("https://api.lyft.com/v1/eta", "lat",
                                       entry.getNorth(request) + "", "lng",
                                       entry.getWest(request) + "");
            HttpURLConnection huc =
                (HttpURLConnection) new URL(url).openConnection();
            huc.addRequestProperty("Authorization", "Bearer " + token);
            json = new String(IOUtil.readBytes(huc.getInputStream()));
        } catch (Exception exc) {
            System.err.println("Lyft error:" + exc);

            return false;
        }

        JSONObject obj = new JSONObject(new JSONTokener(json));
        if ( !obj.has("eta_estimates")) {
            System.err.println("Lyft: No etas:" + json);

            return false;
        }

        JSONArray times = obj.getJSONArray("eta_estimates");
        String pickupTemplate =
            "lyft://ridetype?id={product}&pickup[latitude]={lat}&pickup[longitude]={lon}";
        try {
            for (int i = 0; i < times.length(); i++) {
                JSONObject time = times.getJSONObject(i);
                String     name = time.getString("display_name");
                if (time.isNull("eta_seconds")) {
                    continue;
                }
                int     seconds = time.getInt("eta_seconds");
                double  minutes = ((int) (seconds / 60.0 * 10)) / 10.0;
                boolean isValid = time.getBoolean("is_valid_estimate");
                String pickup = pickupTemplate.replace("{lat}",
                                    entry.getLatitude(request)
                                    + "").replace("{lon}",
                                        entry.getLongitude(request)
                                        + "").replace("{product}",
                                            time.getString("ride_type"));

                if (i == 0) {
                    String cols =
                        HtmlUtils.col(HtmlUtils.b("LYFT"))
                        + HtmlUtils.col(HtmlUtils.b("Minutes Away"),
                                        "colspan=2");
                    sb.append(HtmlUtils.row(cols));
                    sb.append("\n");
                }

                sb.append(HtmlUtils.row(HtmlUtils.cols(name,
                        "" + minutes + ( !isValid
                                         ? " (approx)"
                                         : ""), HtmlUtils.href(pickup,
                                         "Request"))));
                sb.append("\n");
            }
        } catch (Exception exc) {
            System.err.println("Error processing Lyft json:" + exc + "\n"
                               + json);

            throw exc;
        }

        return true;
    }

    /**
     * Class description
     *
     *
     * @version        $version$, Fri, May 27, '16
     * @author         Enter your name here...
     */
    public static class TripBlob {

        TripInfo tripInfo;

        String title;

        String longTitle;

        StringBuilder header;

        StringBuilder buff1;

        StringBuilder buff2;

        boolean inService;

        TripInfo nextTrip;

        public TripBlob(TripInfo tripInfo, String title, String longTitle,
                        boolean inService) {
            this.tripInfo  = tripInfo;
            this.title     = title;
            this.longTitle = longTitle;
            this.inService = inService;
            this.header    = new StringBuilder();
            this.buff1     = new StringBuilder();
            this.buff2     = new StringBuilder();
        }

    }

    public static List<TripInfo> sortTrips(Request request,
                                           List<Entry> trips, Date now,
                                           boolean onlyToday,
                                           boolean onlyFuture,
                                           boolean filterNonTrips)
            throws Exception {

        List<TripInfo> tripInfos = new ArrayList<TripInfo>();
        List<TripInfo> extra     = new ArrayList<TripInfo>();

        if (trips.size() == 0) {
            return new ArrayList<TripInfo>();
        }

        List<Object[]> tuples = new ArrayList<Object[]>();
        for (Entry tripEntry : trips) {
            if (tripEntry.getTypeHandler().isType(
                    GtfsTripTypeHandler.TYPE_TRIP)) {
                String s1 = (String) tripEntry.getValue(request,
                                GtfsTripTypeHandler.IDX_STARTTIME);
                String s2 = (String) tripEntry.getValue(request,
                                GtfsTripTypeHandler.IDX_ENDTIME);
                int seconds1 = Utils.hhmmssToSeconds(s1);
                int seconds2 = Utils.hhmmssToSeconds(s2);
                tuples.add(new Object[] { seconds1, seconds2, tripEntry });
            } else {
                if ( !filterNonTrips) {
                    extra.add(new TripInfo(request, tripEntry));
                }
            }
        }

        Entry agency =
            trips.get(0).getAncestor(GtfsAgencyTypeHandler.TYPE_AGENCY);
        int dow        = -1;
        int nowSeconds = -1;
        if (agency != null) {
            String tz = agency.getStringValue(request,3, "");
            if (Utils.stringDefined(tz)) {
                TimeZone         timeZone = TimeZone.getTimeZone(tz);
                SimpleDateFormat dsdf     = new SimpleDateFormat("u");
                dsdf.setTimeZone(timeZone);
                //'u' gives monday as 1 to sunday 7
                dow = Integer.parseInt(dsdf.format(now));
                //change sunday to 0
                if (dow > 6) {
                    dow = 0;
                }
                GregorianCalendar calendar = new GregorianCalendar(timeZone);
                calendar.setTime(now);
                nowSeconds = calendar.get(calendar.MINUTE) * 60
                             + calendar.get(calendar.HOUR_OF_DAY) * 3600
                             + calendar.get(calendar.SECOND);
            }
        }

        Comparator comp  = new Utils.IntegerTupleComparator(0);
        Object[]   array = tuples.toArray();
        Arrays.sort(array, comp);
        boolean anyActive = false;
        for (Object tuple : array) {
            Entry child = (Entry) ((Object[]) tuple)[2];
            int start = Utils.hhmmssToSeconds(
                            child.getStringValue(request,
                                GtfsTripTypeHandler.IDX_STARTTIME, ""));
            int stop = Utils.hhmmssToSeconds(
                           child.getStringValue(request,
                               GtfsTripTypeHandler.IDX_ENDTIME, ""));
            Entry firstStop = null;
            Entry lastStop  = null;
            if (Utils.stringDefined(
                    child.getStringValue(request,
                        GtfsTripTypeHandler.IDX_FIRST_STOP, (String) null))) {
                firstStop =
                    request.getRepository().getEntryManager().getEntry(
                        request,
                        child.getStringValue(request,
                            GtfsTripTypeHandler.IDX_FIRST_STOP, ""));
            }
            if (Utils.stringDefined(
                    child.getStringValue(request,
                        GtfsTripTypeHandler.IDX_LAST_STOP, (String) null))) {
                lastStop = request.getRepository().getEntryManager().getEntry(
                    request,
                    child.getStringValue(request,GtfsTripTypeHandler.IDX_LAST_STOP, ""));
            }

            boolean[] week = (boolean[]) request.getRepository().decodeObject(
                                 child.getStringValue(request,
                                     GtfsTripTypeHandler.IDX_WEEK, ""));

            boolean scheduleOk = true;
            if ((week != null) && (dow >= 0)) {
                if ( !week[dow]) {
                    scheduleOk = false;
                }
            }
            boolean runningNow = false;
            boolean inThePast  = true;
            if (nowSeconds >= 0) {
                if (scheduleOk && (start <= nowSeconds)
                        && (stop >= nowSeconds)) {
                    runningNow = true;
                }
                if (stop < nowSeconds) {
                    inThePast = true;
                } else {
                    inThePast = false;
                }
            }

            if (onlyToday && !scheduleOk) {
                continue;
            }
            if (onlyFuture && inThePast) {
                continue;
            }

            tripInfos.add(new TripInfo(child, week, scheduleOk, runningNow,
                                       inThePast, now, firstStop, lastStop));
        }

        tripInfos.addAll(extra);

        return tripInfos;
    }

    /**
     * Class description
     *
     *
     * @version        $version$, Fri, May 20, '16
     * @author         Enter your name here...
     */
    public static class TripInfo {

	private Request request;

        private Entry entry;

        private boolean[] week;

        private boolean inService;

        private boolean scheduleOk;

        private boolean inPast;

        private boolean runningNow;

        private String timeLabel;

        private String startTimeLabel = "";

        private String endTimeLabel = "";

        private Entry firstStop;

        private Entry lastStop;

        public TripInfo(Request request,Entry entry) {
	    this.request = request;
            this.entry     = entry;
            this.timeLabel = "";
        }

        public TripInfo(Entry entry, boolean[] week, boolean scheduleOk,
                        boolean runningNow, boolean inPast, Date now,
                        Entry firstStop, Entry lastStop) {
            this.entry          = entry;
            this.timeLabel      = getTimeRange(request,entry);
            this.startTimeLabel = Gtfs.formatStartTime(request,entry);
            this.endTimeLabel   = Gtfs.formatEndTime(request,entry);
            this.week           = week;
            this.scheduleOk     = scheduleOk;
            this.runningNow     = runningNow;
            this.inPast         = inPast;
            if ((now.getTime() < entry.getStartDate())
                    || (now.getTime() > entry.getEndDate())) {
                this.inService = false;
            } else {
                this.inService = true;
            }
            this.firstStop = firstStop;
            this.lastStop  = lastStop;
        }

        /**
         *  Get the Entry property.
         *
         *  @return The Entry
         */
        public Entry getEntry() {
            return entry;
        }

        public Entry getFirstStop() {
            return firstStop;
        }

        public Entry getLastStop() {
            return lastStop;
        }

        /**
         *  Get the SchedulOek property.
         *
         *  @return The ScheduleOk
         */
        public boolean getScheduleOk() {
            return scheduleOk;
        }

        /**
         *  Get the InPast property.
         *
         *  @return The InPast
         */
        public boolean getInPast() {
            return inPast;
        }

        /**
         *  Get the InPast property.
         *
         *  @return The InPast
         */
        public boolean getRunningNow() {
            return runningNow;
        }

        public boolean[] getWeek() {
            return week;
        }

        public String getTimeLabel() {
            return timeLabel;
        }

        public String getStartTimeLabel() {
            return startTimeLabel;
        }

        public String getEndTimeLabel() {
            return endTimeLabel;
        }

        public String getCssClass() {
            if (runningNow) {
                return "gtfs-trip-list-now";
            }

            return (scheduleOk
                    ? (inPast
                       ? "gtfs-trip-list-past"
                       : "gtfs-trip-list-future")
                    : "gtfs-trip-list-inactive");
        }

        public String getWeekString() {
            return Gtfs.getWeekString(week);
        }

    }

    public static String getRouteTitle(Request request, Entry entry,
                                       boolean doLink)
            throws Exception {
        return getRouteTitle(request, entry, doLink, true);
    }

    public static String getRouteTitle(Request request, Entry entry,
                                       boolean doLink, boolean full)
            throws Exception {
        String bg = getColor(entry.getStringValue(request,GtfsRouteTypeHandler.IDX_COLOR,
                                            "#ccc"));
        String fg =
            getColor(entry.getStringValue(request,GtfsRouteTypeHandler.IDX_TEXT_COLOR,
                                    "#000"));
        String routeId = entry.getStringValue(request,GtfsRouteTypeHandler.IDX_ID, "");
        String routeTag =
            HtmlUtils.span(routeId,
                           HtmlUtils.cssClass("gtfs-route-badge")
                           + HtmlUtils.style("background:" + bg + ";"
                                             + "color:" + fg));
        String label = routeTag;
        if (full) {
            label = "Route " + routeTag;
            if ( !routeId.equals(entry.getLabel())) {
                label = Utils.concatString(label, "&nbsp;", entry.getLabel());
            }
        }
        if ( !doLink) {
            return label;
        }
        String link = HtmlUtils.href(
                          request.entryUrl(
                              request.getRepository().URL_ENTRY_SHOW,
                              entry), label);

        return link;
    }

    public static String getColor(String c) {
        if (c.startsWith("#")) {
            return c;
        }

        return "#" + c;
    }

    public static void addHostAlias(Request request, Entry entry,
                                    String host, String prefix)
            throws Exception {
        if (host != null) {
            addAlias(request, entry, "http://" + prefix + "." + host);
        }
    }

    public static void addAlias(Request request, Entry entry, String alias)
            throws Exception {
        alias = alias.replaceAll(" ", "_");
        request.getRepository().getMetadataManager().addMetadata(request,entry,
                new Metadata(request.getRepository().getGUID(),
                             entry.getId(),
                             request.getRepository().getMetadataManager().findType(ContentMetadataHandler.TYPE_ALIAS), false,
                             alias.toLowerCase(), null, null, null, null));
    }

    private static Entry getStopsEntry(Request request, Entry entry)
            throws Exception {
        Entry agency = entry.getAncestor(GtfsAgencyTypeHandler.TYPE_AGENCY);
        if (agency == null) {
            return null;
        }
        Entry stopsEntry = (Entry) agency.getProperty("stopsEntry");
        if (stopsEntry == null) {
            stopsEntry =
                request.getRepository().getEntryManager().findEntryWithName(
                    request, agency, "Stops");
        }
        if (stopsEntry == null) {
            return null;
        }
        agency.putProperty("stopsEntry", stopsEntry);

        return stopsEntry;
    }

    public static List<StopTime> getStopsForTrip(Request request,
            Entry entry, Hashtable<String, Entry> stopMap)
            throws Exception {

        List<StopTime> stops =
            (List<StopTime>) entry.getProperty("gtfs-stops");
        if (stops != null) {
            return stops;
        }
        Entry stopsEntry = getStopsEntry(request, entry);
        if (stopsEntry == null) {
            return null;
        }
        stops = new ArrayList<StopTime>();
        Request searchRequest = new Request(request.getRepository(),
                                            request.getUser());
        searchRequest.put(GtfsRouteTypeHandler.ARG_GROUP, stopsEntry.getId());
        String s = entry.getStringValue(request,GtfsTripTypeHandler.IDX_STOPS, "");
        List<String[]> tuples =
            (List<String[]>) request.getRepository().decodeObject(
                Utils.uncompress(s));

        for (String[] tuple : tuples) {
            String stopId    = tuple[0];
            String arrival   = tuple[1];
            String departure = tuple[2];

            Entry  stopEntry = (stopMap != null)
                               ? stopMap.get(stopId)
                               : null;

            if (stopEntry == null) {
                searchRequest.put(GtfsRouteTypeHandler.ARG_TYPE,
                                  "type_gtfs_stop");
                searchRequest.put("search.type_gtfs_stop.stop_id",
                                  "=" + stopId);
                List<Entry> entries =
                    request.getRepository().getEntryManager().getEntriesFromDb(searchRequest);
                stopEntry = (entries.size() > 0)
                            ? entries.get(0)
                            : null;

            }
            if (stopEntry != null) {
                if (stopMap != null) {
                    stopMap.put(stopId, stopEntry);
                }
                stops.add(new StopTime(stopEntry, arrival, departure));
            }
        }
        entry.putProperty("gtfs-stops", stops);

        return stops;
    }

    public static Entry getStop(Request request, Entry entry, String stopId)
            throws Exception {
        if (stopId == null) {
            return null;
        }
        Entry agency = entry.getAncestor(GtfsAgencyTypeHandler.TYPE_AGENCY);
        Request searchRequest = new Request(request.getRepository(),
                                            request.getUser());
        searchRequest.put(GtfsRouteTypeHandler.ARG_TYPE, "type_gtfs_stop");
        searchRequest.put("search.type_gtfs_stop.stop_id", "=" + stopId);
        searchRequest.put("search.type_gtfs_stop.agency_id",
                          "=" + agency.getId());
        List<Entry> entries =
            request.getRepository().getEntryManager().getEntriesFromDb(searchRequest);

        return (entries.size() > 0)
               ? entries.get(0)
               : null;
    }

    public static Entry getRoute(Request request, Entry entry, String routeId)
            throws Exception {
        if (routeId == null) {
            return null;
        }
        Entry agency = entry.getAncestor(GtfsAgencyTypeHandler.TYPE_AGENCY);
        Request searchRequest = new Request(request.getRepository(),
                                            request.getUser());
        searchRequest.put(GtfsRouteTypeHandler.ARG_TYPE, "type_gtfs_route");
        searchRequest.put("search.type_gtfs_route.route_id", "=" + routeId);
        searchRequest.put("search.type_gtfs_route.agency_id",
                          "=" + agency.getId());
        List<Entry> entries =
            request.getRepository().getEntryManager().getEntriesFromDb(searchRequest);

        return (entries.size() > 0)
               ? entries.get(0)
               : null;
    }

    public static Entry getTrip(Request request, Entry entry, String tripId)
            throws Exception {
        if (tripId == null) {
            return null;
        }
        Entry agency = entry.getAncestor(GtfsAgencyTypeHandler.TYPE_AGENCY);
        Request searchRequest = new Request(request.getRepository(),
                                            request.getUser());
        searchRequest.put(GtfsRouteTypeHandler.ARG_TYPE, "type_gtfs_trip");
        searchRequest.put("search.type_gtfs_trip.trip_id", "=" + tripId);
        searchRequest.put("search.type_gtfs_trip.agency_id",
                          "=" + agency.getId());
        List<Entry> entries =
            request.getRepository().getEntryManager().getEntriesFromDb(searchRequest);

        return (entries.size() > 0)
               ? entries.get(0)
               : null;
    }

    /**
     * Class description
     *
     *
     * @version        $version$, Thu, May 19, '16
     * @author         Enter your name here...
     */
    public static class StopTime {

        public Entry entry;

        public String startTime;

        public String endTime;

        public StopTime(Entry entry, String startTime, String endTime) {
            this.entry     = entry;
            this.startTime = startTime;
            this.endTime   = endTime;
        }
    }

    public static void main(String[] args) {
        String s = "hh:mm:ss";
        System.err.println(s.replaceAll(":[^:]+$", ""));
    }

    private static TTLCache<String, List<Entry>> vehicleCache =
        new TTLCache<String, List<Entry>>(1000 * 60);

    public static List<Entry> getVehiclesForRoute(Request request,
            Entry agencyEntry, Entry routeEntry)
            throws Exception {
        List<Entry> vehicles      = getVehicles(request, agencyEntry);
        List<Entry> routeVehicles = new ArrayList<Entry>();
        String routeId =
            (String) routeEntry.getStringValue(request,GtfsRouteTypeHandler.IDX_ID,
                                         (String) null);
        for (Entry vehicle : vehicles) {
            if (routeId.equals(
                    vehicle.getValue(request,GtfsVehicleTypeHandler.IDX_ROUTE_ID))) {
                routeVehicles.add(vehicle);
            }
        }

        //        System.err.println(" route:" + routeId +" vehicles:" + routeVehicles);
        return routeVehicles;
    }

    public static List<Entry> getVehiclesForStop(Request request,
            Entry agencyEntry, Entry stopEntry)
            throws Exception {
        List<Entry> vehicles     = getVehicles(request, agencyEntry);
        List<Entry> stopVehicles = new ArrayList<Entry>();
        String stopId =
            (String) stopEntry.getStringValue(request,GtfsStopTypeHandler.IDX_STOP_ID,
                                        (String) null);
        for (Entry vehicle : vehicles) {
            if (stopId.equals(
                    vehicle.getValue(request,GtfsVehicleTypeHandler.IDX_STOP_ID))) {
                stopVehicles.add(vehicle);
            }
        }
        //        System.err.println("stop:" + stopId + " vehicles:" + stopVehicles);

        return stopVehicles;
    }

    public static List<Entry> getVehiclesForTrip(Request request,
            Entry agencyEntry, Entry tripEntry)
            throws Exception {
        List<Entry> vehicles     = getVehicles(request, agencyEntry);
        List<Entry> tripVehicles = new ArrayList<Entry>();
        String tripId =
            (String) tripEntry.getStringValue(request,GtfsTripTypeHandler.IDX_TRIP_ID,
                                        (String) null);
        for (Entry vehicle : vehicles) {
            if (tripId.equals(
                    vehicle.getValue(request,GtfsVehicleTypeHandler.IDX_TRIP_ID))) {
                tripVehicles.add(vehicle);
            }
        }

        //        System.err.println("Trip:" + tripId +" vehicles:" + tripVehicles);
        return tripVehicles;
    }

    public static List<Entry> getVehicles(Request request, Entry agencyEntry)
            throws Exception {

        URLConnection urlConnection = null;
	if(agencyEntry==null) return new ArrayList<Entry>();
        String agencyId = (String) agencyEntry.getStringValue(request,
                              GtfsAgencyTypeHandler.IDX_AGENCY_ID, null);
        String      cacheKey = agencyEntry.getId();
        List<Entry> vehicles = vehicleCache.get(cacheKey);
        if (vehicles != null) {
            return vehicles;
        }
        vehicles = new ArrayList<Entry>();
        String rtUrl =
            (String) agencyEntry.getStringValue(request,GtfsAgencyTypeHandler.IDX_RT_URL,
                                          null);
        Repository repository = agencyEntry.getTypeHandler().getRepository();
        if ( !Utils.stringDefined(rtUrl)) {
            rtUrl = repository.getProperty(agencyId + ".rt.url",
                                           (String) null);
        }

        if ( !Utils.stringDefined(rtUrl)) {
            return vehicles;
        }

        String rtId =
            (String) agencyEntry.getStringValue(request,GtfsAgencyTypeHandler.IDX_RT_ID,
                                          null);
        if ( !Utils.stringDefined(rtId)) {
            rtId = repository.getProperty(agencyId + ".rt.id", (String) null);
        }

        String rtPassword = (String) agencyEntry.getStringValue(request,
                                GtfsAgencyTypeHandler.IDX_RT_PASSWORD, null);
        if ( !Utils.stringDefined(rtPassword)) {
            rtPassword = repository.getProperty(agencyId + ".rt.password",
                    (String) null);
        }

        try {
            URL url = new URL(rtUrl);
            urlConnection = url.openConnection();
            if (Utils.stringDefined(rtId)
                    && Utils.stringDefined(rtPassword)) {
                String authString    = rtId.trim() + ":" + rtPassword.trim();
                String authStringEnc = Utils.encodeBase64(authString);
                urlConnection.setRequestProperty("Authorization",
                        "Basic " + authStringEnc);
            }

            FeedMessage feed =
                FeedMessage.parseFrom(urlConnection.getInputStream());
            Date dttm = new Date();
            TypeHandler vehicleTypeHandler =
                repository.getTypeHandler("type_gtfs_vehicle");
            for (FeedEntity entity : feed.getEntityList()) {
                if (entity.hasTripUpdate()) {
                    //System.out.println(entity.getTripUpdate());
                } else if (entity.hasVehicle()) {
                    com.google.transit.realtime.GtfsRealtime.VehiclePosition vehicle =
                        entity.getVehicle();
                    com.google.transit.realtime.GtfsRealtime.TripDescriptor trip =
                        vehicle.getTrip();
                    com.google.transit.realtime.GtfsRealtime.VehicleDescriptor desc =
                        vehicle.getVehicle();
                    com.google.transit.realtime.GtfsRealtime.Position pos =
                        vehicle.getPosition();

                    Entry newEntry = new Entry(Repository.ID_PREFIX_SYNTH
                                         + agencyEntry.getId()
                                         + TypeHandler.ID_DELIMITER
                                         + desc.getId(), vehicleTypeHandler);

                    Date vdttm = dttm;
                    if (vehicle.hasTimestamp()) {
                        vdttm = new Date(vehicle.getTimestamp() * 1000);
                    }
                    Object[] values =
                        vehicleTypeHandler.makeEntryValues(null);
                    values[GtfsVehicleTypeHandler.IDX_VEHICLE_ID] =
                        desc.getId();
                    values[GtfsVehicleTypeHandler.IDX_ROUTE_ID] =
                        trip.getRouteId();
                    values[GtfsVehicleTypeHandler.IDX_TRIP_ID] =
                        trip.getTripId();
                    values[GtfsVehicleTypeHandler.IDX_STATUS] =
                        vehicle.getCurrentStatus();
                    if (vehicle.hasStopId()) {
                        //                        Entry stop = getStop(request, agencyEntry, vehicle.getStopId());
                        //                        if(stop!=null) {
                        //                            values[GtfsVehicleTypeHandler.IDX_STOP_ID] = stop.getName();
                        //                        } else {
                        values[GtfsVehicleTypeHandler.IDX_STOP_ID] =
                            vehicle.getStopId();
                        //                        }
                    }
                    String name = "Vehicle:";
                    if (desc.hasLabel()) {
                        name += desc.getLabel();
                    } else {
                        name += desc.getId();
                    }
                    newEntry.initEntry(
                        name, "", agencyEntry,
                        repository.getUserManager().getLocalFileUser(),
                        new Resource(), "", Entry.DEFAULT_ORDER,
                        vdttm.getTime(), vdttm.getTime(), vdttm.getTime(),
                        vdttm.getTime(), values);
                    repository.getEntryManager().cacheSynthEntry(newEntry);
                    vehicles.add(newEntry);
                    newEntry.setLatitude(pos.getLatitude());
                    newEntry.setLongitude(pos.getLongitude());
                    if (pos.hasBearing()) {
                        Metadata dirMetadata =
                            new Metadata(repository.getGUID(),
                                         newEntry.getId(),
                                         request.getRepository().getMetadataManager().findType("camera.direction"), false,
                                         "" + pos.getBearing(),
                                         Metadata.DFLT_ATTR,
                                         Metadata.DFLT_ATTR,
                                         Metadata.DFLT_ATTR,
                                         Metadata.DFLT_EXTRA);
                        request.getRepository().getMetadataManager()
                            .addMetadata(request,newEntry, dirMetadata);
                    }
                }
            }
        } catch (Exception exc) {
            System.err.println("Error:" + exc);
            ((HttpURLConnection) urlConnection).getResponseCode();
            InputStream stream =
                ((HttpURLConnection) urlConnection).getErrorStream();
            if (stream == null) {
                stream = urlConnection.getInputStream();
            }
            //            System.err.println("error:"+ IOUtil.readContents(stream));
        }
        vehicleCache.put(cacheKey, vehicles);

        return vehicles;

    }

    public static List<Entry> getTripsForStop(Request request, Entry entry)
            throws Exception {
        Entry agency = entry.getAncestor(GtfsAgencyTypeHandler.TYPE_AGENCY);

	if(agency==null) throw new IllegalStateException("Gtfs.getTripsForStop: no agency found");

        List<Entry> trips =
            (List<Entry>) agency.getTransientProperty(entry.getId()
                + ".trips");
        if (trips != null) {
            return trips;
        }

        Request searchRequest = new Request(request.getRepository(),
                                            request.getUser());
        searchRequest.put(ARG_TYPE, GtfsTripTypeHandler.TYPE_TRIP);
        StringBuilder q = new StringBuilder();
        q.append("[");
        if (agency != null) {
            q.append(agency.getStringValue(request,GtfsAgencyTypeHandler.IDX_AGENCY_ID,
                                     ""));
        }
        q.append("-");
        q.append(entry.getStringValue(request,GtfsStopTypeHandler.IDX_STOP_ID, ""));
        q.append("]");
        searchRequest.put("search.type_gtfs_trip.stop_ids", q);
        trips = request.getRepository().getEntryManager().getEntriesFromDb(searchRequest);
        agency.putTransientProperty(entry.getId() + ".trips", trips);

        return trips;
    }

    public static List<Entry> getRoutesForStop(Request request, Entry entry)
            throws Exception {

        Entry agency = entry.getAncestor(GtfsAgencyTypeHandler.TYPE_AGENCY);

        List<Entry> routes =
            (List<Entry>) agency.getTransientProperty(entry.getId()
                + ".routes");
        if (routes != null) {
            return routes;
        }
        routes = new ArrayList<Entry>();

        for (String id :
                StringUtil.split(
                    entry.getStringValue(request,GtfsStopTypeHandler.IDX_ROUTES, ""), ",",
                    true, true)) {
            routes.add(
                request.getRepository().getEntryManager().getEntry(
                    request, id));
        }
        /*
        HashSet seen = new HashSet();
        for(Entry trip: getTripsForStop(request, entry)) {
            Entry route = trip.getParentEntry();
            if(!seen.contains(route.getId())) {
                seen.add(route.getId());
                routes.add(route);
            }
        }
        */
        agency.putTransientProperty(entry.getId() + ".routes", routes);

        return routes;

    }

    public static void addToMap(Request request, List<Entry> vehicles,
                                MapInfo map)
            throws Exception {
        request.getRepository().getMapManager().addToMap(request, map,
                vehicles,
                Utils.makeMap(MapManager.PROP_DETAILED, "true",
                              MapManager.PROP_SCREENBIGRECTS, "true"));
        for (Entry vehicle : vehicles) {
            map.addMarker(request, vehicle);
        }
    }

}
