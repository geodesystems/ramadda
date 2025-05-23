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
public class GtfsStopTypeHandler extends GenericTypeHandler {

    public static int IDX = 0;

    public static final int IDX_STOP_ID = IDX++;

    public static final int IDX_STOP_CODE = IDX++;

    public static final int IDX_ZONE_ID = IDX++;

    public static final int IDX_LOCATION_TYPE = IDX++;

    public static final int IDX_TIMEZONE = IDX++;

    public static final int IDX_WHEELCHAIR_BOARDING = IDX++;

    public static final int IDX_ROUTES = IDX++;

    public static final int IDX_AGENCY_ID = IDX++;

    public GtfsStopTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }

    @Override
    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props)
            throws Exception {

        if (tag.equals("gtfs.uber.estimate")) {
            //            return getUberInclude(wikiUtil, request, originalEntry, entry,  props);
        }

        if ( !tag.equals("gtfs.stop.schedule")) {
            return super.getWikiInclude(wikiUtil, request, originalEntry,
                                        entry, tag, props);
        }

        List<Entry>   trips = Gtfs.getTripsForStop(request, entry);
        StringBuilder sb    = new StringBuilder();
        sb.append(HtmlUtils.cssLink(getRepository().getUrlBase()
                                    + "/gtfs/gtfs.css"));

        boolean doAll = request.get(Gtfs.ARG_ALLDATES, false);
        if (doAll) {
            request.put(Gtfs.ARG_DATE, "today");
        }

        doAll = true;
        Date now = Gtfs.addDateLabel(request, entry, sb);
        List<Gtfs.TripInfo> tripInfos = Gtfs.sortTrips(request, trips, now,
                                            !doAll, false, true);
        if ( !doAll && (tripInfos.size() == 0)) {
            //            sb.append(HtmlUtils.note("No trips today"));
            doAll = true;
            tripInfos = Gtfs.sortTrips(request, trips, now, !doAll, false,
                                       true);
        }

        if (tripInfos.size() > 0) {
            Gtfs.displayTrips(request, entry, tripInfos, sb, now, entry,
                              true);
        }

        return sb.toString();
    }

    public boolean addToMap(Request request, Entry entry, MapInfo map)
            throws Exception {
        super.addToMap(request, entry, map);

        Entry agency = entry.getAncestor(GtfsAgencyTypeHandler.TYPE_AGENCY);
        List<Entry> vehicles = Gtfs.getVehiclesForStop(request, agency,
                                   entry);
        Gtfs.addToMap(request, vehicles, map);

	return true;
    }

}
