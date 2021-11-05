/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.point;


import org.ramadda.data.services.*;
import org.ramadda.repository.Entry;
import org.ramadda.repository.Repository;
import org.ramadda.repository.Request;

import org.w3c.dom.Element;


/**
 * TypeHandler for Aviation Weather Center METARS
 * https://www.aviationweather.gov/adds/dataserver
 *
 *
 */
public class AwcMetarTypeHandler extends PointTypeHandler {

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
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception On badnes
     */
    @Override
    public String getPathForEntry(Request request, Entry entry)
            throws Exception {
        if (entry.isFile()) {
            return super.getPathForEntry(request, entry);
        }
        String siteId = entry.getValue(IDX_SITE_ID, "");
        int    offset = (int) entry.getValue(IDX_TIME_OFFSET, 24);
        String url = URL.replace("{station}", siteId).replace("{offset}",
                                 "" + offset);

        return url;
    }

}
