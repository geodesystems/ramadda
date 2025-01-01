/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.gtfs;




import org.ramadda.repository.*;
import org.ramadda.repository.map.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;
import org.ramadda.util.WikiUtil;



import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;


import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;


import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.net.*;

import java.util.ArrayList;

import java.util.Date;
import java.util.Hashtable;
import java.util.List;


/**
 *
 *
 */
public class GtfsVehicleTypeHandler extends GenericTypeHandler {

    /** _more_ */
    public static int IDX = 0;

    /** _more_ */
    public static final int IDX_VEHICLE_ID = IDX++;

    /** _more_ */
    public static final int IDX_ROUTE_ID = IDX++;

    /** _more_ */
    public static final int IDX_TRIP_ID = IDX++;

    /** _more_ */
    public static final int IDX_STATUS = IDX++;

    /** _more_ */
    public static final int IDX_STOP_ID = IDX++;

    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public GtfsVehicleTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
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
        sb.append("strokeWidth:2");
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

        //        map.addMarker(request, entry);
        return false;
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

        if (tag.equals("gtfs.stop.link")) {
            String stopId = (String) entry.getValue(request,IDX_STOP_ID);
            Entry  stop   = Gtfs.getStop(request, entry, stopId);
            if (stop == null) {
                if (stopId == null) {
                    return "No stop defined";
                }

                return stopId;
            }

            return getPageHandler().getEntryHref(request, stop);
        }

        if (tag.equals("gtfs.trip.link")) {
            String tripId = (String) entry.getValue(request,IDX_TRIP_ID);
            //            System.err.println("calling gettrip trip id:" + tripId);
            Entry trip = Gtfs.getTrip(request, entry, tripId);
            if (trip == null) {
                if (tripId == null) {
                    return "No trip defined";
                }

                return tripId;
            }

            return getPageHandler().getEntryHref(request, trip);
        }

        if (tag.equals("gtfs.route.link")) {
            String routeId = (String) entry.getValue(request,IDX_ROUTE_ID);
            Entry  route   = Gtfs.getRoute(request, entry, routeId);
            if (route == null) {
                if (routeId == null) {
                    return "No route defined";
                }

                return routeId;
            }

            return Gtfs.getRouteTitle(request, route, true);
        }


        return null;
    }


}
