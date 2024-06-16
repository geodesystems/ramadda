/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.gtfs;


import org.ramadda.repository.*;
import org.ramadda.repository.map.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;



import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.Utils;
import org.ramadda.util.WikiUtil;
import org.ramadda.util.geo.GeoUtils;

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
public class GtfsRouteTypeHandler extends ExtensibleGroupTypeHandler {

    /** _more_ */
    public static final String TYPE_ROUTE = "type_gtfs_route";


    /** _more_ */
    private static int IDX = 0;


    /** _more_ */
    public static final int IDX_ID = IDX++;

    /** _more_ */
    public static final int IDX_TYPE = IDX++;

    /** _more_ */
    public static final int IDX_COLOR = IDX++;

    /** _more_ */
    public static final int IDX_TEXT_COLOR = IDX++;

    /** _more_ */
    public static final int IDX_POINTS = IDX++;

    /** _more_ */
    public static final int IDX_STOP_NAMES = IDX++;

    /** _more_ */
    public static final int IDX_AGENCY_ID = IDX++;



    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public GtfsRouteTypeHandler(Repository repository, Element entryNode)
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

        if (tag.equals("gtfs.route.title")) {
            StringBuilder sb = new StringBuilder();
            sb.append(HtmlUtils.cssLink(getRepository().getUrlBase()
                                        + "/gtfs/gtfs.css"));
            HtmlUtils.div(sb, Gtfs.getRouteTitle(request, entry, true),
                          HtmlUtils.cssClass("ramadda-page-title"));

            return sb.toString();
        }

        if (tag.equals("gtfs.route.info")) {
            StringBuilder sb = new StringBuilder();
            sb.append(HtmlUtils.formTable());
            addRouteInfo(request, entry, sb);
            sb.append(HtmlUtils.formTableClose());

            return sb.toString();
        }

        if (tag.equals("gtfs.route.trips")) {
            StringBuilder sb = new StringBuilder();
            List<Entry> trips = getEntryManager().getChildren(request, entry);
            Date          now = Gtfs.addDateLabel(request, entry, sb);
            List<Gtfs.TripInfo> tripInfos = Gtfs.sortTrips(request, trips,
                                                now, false, false, true);
            Gtfs.displayTrips(request, entry, tripInfos, sb, now,
                              (Entry) null, false);

            return sb.toString();
        }


        if (tag.equals("gtfs.route.latest")) {
            boolean open = Misc.getProperty(props, "gtfs.open", true);
            StringBuilder sb = new StringBuilder();
            List<Entry> trips = getEntryManager().getChildren(request, entry);
            Date          now = Gtfs.addDateLabel(request, entry, sb);
            List<Gtfs.TripInfo> tripInfos = Gtfs.sortTrips(request, trips,
                                                now, true, true, true);
            Gtfs.displayTrips(request, entry, tripInfos, sb, now,
                              (Entry) null, false, open);

            return sb.toString();
        }


        return super.getWikiInclude(wikiUtil, request, originalEntry, entry,
                                    tag, props);
    }






    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void addRouteInfo(Request request, Entry entry, Appendable sb)
            throws Exception {
        Entry agency = entry.getAncestor(GtfsAgencyTypeHandler.TYPE_AGENCY);
        if (agency != null) {
            sb.append(
                HtmlUtils.formEntry(
                    msgLabel("Agency"),
                    getPageHandler().getEntryHref(request, agency)));
        }

        List<Metadata> metadataList =
            getMetadataManager().findMetadata(request, entry,
                ContentMetadataHandler.TYPE_ALIAS, false);
        if ((metadataList != null) && (metadataList.size() > 0)) {
            for (Metadata metadata : metadataList) {
                String alias = metadata.getAttr1();
                if (alias.startsWith("http:")) {
                    alias = HtmlUtils.href(alias, alias);
                } else {
                    alias = HtmlUtils.href(getRepository().getUrlBase()
                                           + "/alias/" + alias, alias);
                }
                sb.append(HtmlUtils.formEntry(msgLabel("Link"), alias));
            }
        }

        sb.append(HtmlUtils.formEntry(msgLabel("Route ID"),
                                      entry.getStringValue(request,IDX_ID, "")));
        sb.append(HtmlUtils.formEntry(msgLabel("Route Type"),
                                      getFieldHtml(request, entry,new Hashtable(),
                                          "route_type", true)));
        /*
        sb.append(
            HtmlUtils.formEntry(
                msgLabel("Color"),
                getSwatch(Gtfs.getColor(entry.getStringValue(request,IDX_COLOR, "")))));

        sb.append(
            HtmlUtils.formEntry(
                msgLabel("Text Color"),
                getSwatch(
                    Gtfs.getColor(entry.getStringValue(request,IDX_TEXT_COLOR, "")))));
        */
    }

    /**
     * _more_
     *
     * @param c _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private String getSwatch(String c) throws Exception {
        Appendable sb = new StringBuilder();
        sb.append("<table><tr valign=center><td>");
        HtmlUtils.div(sb, " ",
                      HtmlUtils.cssClass("gtfs-swatch")
                      + HtmlUtils.style("background:" + c));
        sb.append("</td><td>");
        sb.append(c);
        sb.append("</td></tr></table>\n");

        return sb.toString();

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
        return "Route " + entry.getName();
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
	Request request = getRepository().getAdminRequest();
        String color = Gtfs.getColor(entry.getStringValue(request,IDX_COLOR, ""));
        sb.append("'strokeColor':'" + color + "','strokeWidth':4");
    }




    /**
     * _more_
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
        Entry  agency = entry.getAncestor(GtfsAgencyTypeHandler.TYPE_AGENCY);
        String s      = entry.getStringValue(request,IDX_POINTS, "");
        if (Utils.stringDefined(s)) {
            s = Utils.uncompress(s);
            List<double[]> points = new ArrayList<double[]>();
            GeoUtils.parsePointString(s, points);
            map.addLines(entry, entry.getId() + "_polygon", points, null);
        }

        //Find one trip and get its stops
        Date now = new Date();
        List<Entry> children = getEntryManager().getChildren(request, entry);
        List<Gtfs.TripInfo> trips = Gtfs.sortTrips(request, children, now,
                                        false, false, true);

        List<Gtfs.StopTime> stops = null;
        for (Gtfs.TripInfo tripInfo : trips) {
            if (tripInfo.getRunningNow()) {
                Entry tripEntry = tripInfo.getEntry();
                stops = Gtfs.getStopsForTrip(request, tripEntry, null);

                break;
            }
        }

        if ((stops == null) && (trips.size() > 0)) {
            stops = Gtfs.getStopsForTrip(request, trips.get(0).getEntry(),
                                         null);
        }

        for (Entry child : children) {
            if (child.getTypeHandler().isType(
                    GtfsTripTypeHandler.TYPE_TRIP)) {

                break;
            }
        }

        if (stops != null) {
            for (Gtfs.StopTime stopTime : stops) {
                map.addMarker(request, stopTime.entry);
            }
        }
        List<Entry> vehicles = Gtfs.getVehiclesForRoute(request, agency,
                                   entry);
        Gtfs.addToMap(request, vehicles, map);

        return false;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public List<Entry> postProcessEntries(Request request,
                                          List<Entry> entries)
            throws Exception {

        entries = super.postProcessEntries(request, entries);
        List<Gtfs.TripInfo> tripInfos = Gtfs.sortTrips(request, entries,
                                            new Date(), false, false, false);
        entries = new ArrayList<Entry>();
        for (Gtfs.TripInfo tripInfo : tripInfos) {
            entries.add(tripInfo.getEntry());
        }

        return entries;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param items _more_
     * @param attrs _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void addToJson(Request request, Entry entry, List<String> items,
                          List<String> attrs)
            throws Exception {
        super.addToJson(request, entry, items, attrs);
        String color = Gtfs.getColor(entry.getStringValue(request,IDX_COLOR, ""));
        items.add("lineColor");
        items.add(JsonUtil.quote(color));
        String s = entry.getStringValue(request,IDX_POINTS, "");
        if (Utils.stringDefined(s)) {
            s = Utils.uncompress(s);
            List<double[]> points = new ArrayList<double[]>();
            GeoUtils.parsePointString(s, points);
            List<String> pts = new ArrayList<String>();
            for (double[] pt : points) {
                pts.add(Double.toString(pt[0]));
                pts.add(Double.toString(pt[1]));
            }
            items.add("polygon");
            items.add(JsonUtil.list(pts));
        }

        items.add("bubble");
        items.add(JsonUtil.quote(getMapManager().makeInfoBubble(request, entry,null,
                true)));
    }



}
