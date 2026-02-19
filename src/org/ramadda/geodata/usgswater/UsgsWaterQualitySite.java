/**
Copyright (c) 2008-2026 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.usgswater;

import org.ramadda.data.services.*;
import org.ramadda.repository.Entry;
import org.ramadda.repository.Repository;
import org.ramadda.repository.Request;

import org.w3c.dom.Element;

public class UsgsWaterQualitySite extends PointTypeHandler {

    public static final String URL =
        "http://www.waterqualitydata.us/Result/search?siteid={siteid}&pCode={parameter}&minresults=10&mimeType=csv&zip=yes&sorted=yes";

    public static final String COL_SITE_ID = "site_id";
    public static final String COL_PARAMETER = "parameter";

    public UsgsWaterQualitySite(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }

    @Override
    public String getPathForEntry(Request request, Entry entry, boolean forRead)
            throws Exception {

        String siteId = (String)entry.getStringValue(request,COL_SITE_ID,"");
        String param  = (String)entry.getStringValue(request,COL_PARAMETER,"");

        String url = URL.replace("{siteid}", siteId).replace("{parameter}",
                                 param);

        return url;
    }

}
