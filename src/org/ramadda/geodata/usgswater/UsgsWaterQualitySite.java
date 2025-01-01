/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.usgswater;


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
public class UsgsWaterQualitySite extends PointTypeHandler {

    /** _more_ */
    public static final String URL =
        "http://www.waterqualitydata.us/Result/search?siteid={siteid}&pCode={parameter}&minresults=10&mimeType=csv&zip=yes&sorted=yes";


    /** _more_ */
    private static int IDX =
        org.ramadda.data.services.RecordTypeHandler.IDX_LAST + 1;


    /** _more_ */
    public static final int IDX_SITE_ID = IDX++;

    /** _more_ */
    public static final int IDX_PARAMETER = IDX++;

    /**
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     *
     * @throws Exception On badnes
     */
    public UsgsWaterQualitySite(Repository repository, Element node)
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
    public String getPathForEntry(Request request, Entry entry, boolean forRead)
            throws Exception {
        String siteId = (String)entry.getValue(IDX_SITE_ID);
        String param  = (String)entry.getValue(IDX_PARAMETER);

        String url = URL.replace("{siteid}", siteId).replace("{parameter}",
                                 param);

        return url;
    }

}
