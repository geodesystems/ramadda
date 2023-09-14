/**
   Copyright (c) 2008-2023 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.point;

import org.ramadda.data.services.*;
import org.ramadda.repository.Entry;
import org.ramadda.repository.Repository;
import org.ramadda.repository.Request;

import org.ramadda.util.Utils;

import org.w3c.dom.Element;

import java.util.Hashtable;


/**
 * TypeHandler for Aviation Weather Center METARS
 * https://www.aviationweather.gov/adds/dataserver
 *
 *
 */
public class AwcMetarTypeHandler extends NwsStationTypeHandler {

    /** _more_ */
    public static final String URL =
        "https://www.aviationweather.gov/adds/dataserver_current/httpparam?dataSource=metars&requestType=retrieve&format=csv&stationString={station}&hoursBeforeNow={offset}";


    /** _more_ */
    private static int IDX =
        org.ramadda.data.services.RecordTypeHandler.IDX_LAST + 1;


    /** _more_ */
    public static final int IDX_SITE_ID = IDX++;

    /** _more_ */
    public static final int IDX_TIME_OFFSET = IDX++;

    /**
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     *
     * @throws Exception On badnes
     */
    public AwcMetarTypeHandler(Repository repository, Element node)
	throws Exception {
        super(repository, node);
    }


    /**
     *
     * @param request _more_
     * @param entry _more_
     * @param fromImport _more_
     *
     * @throws Exception _more_
     */
    public void initializeNewEntry(Request request, Entry entry,
                                   boolean fromImport)
	throws Exception {
        super.initializeNewEntry(request, entry, fromImport);
        if (fromImport) {
            return;
        }
	initializeNewEntry(request, entry,  (String) entry.getStringValue(IDX_SITE_ID, ""));
    }




    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     * @param forRead _more_
     *
     * @return _more_
     *
     * @throws Exception On badnes
     */
    @Override
    public String getPathForEntry(Request request, Entry entry,
                                  boolean forRead)
	throws Exception {
        if (entry.isFile()) {
            return super.getPathForEntry(request, entry, forRead);
        }
        String siteId = entry.getStringValue(IDX_SITE_ID, "");
        int    offset = (int) entry.getIntValue(IDX_TIME_OFFSET, 24);
        String url = URL.replace("{station}", siteId).replace("{offset}",
							      "" + offset);

        return url;
    }

}
