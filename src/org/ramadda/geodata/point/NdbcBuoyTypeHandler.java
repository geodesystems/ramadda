/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.point;


import org.ramadda.data.point.text.*;
import org.ramadda.data.record.*;
import org.ramadda.data.services.PointTypeHandler;
import org.ramadda.data.services.RecordTypeHandler;
import org.ramadda.repository.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.Utils;

import org.w3c.dom.*;

import java.io.*;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Hashtable;


/**
 */
public class NdbcBuoyTypeHandler extends PointTypeHandler {



    /** _more_ */
    private static int IDX = PointTypeHandler.IDX_LAST + 1;

    /** _more_ */
    private static int IDX_STATION_ID = IDX++;

    /** _more_ */
    private static int IDX_DATA_TYPE = IDX++;

    /** _more_ */
    private static final String URL_TEMPLATE =
        "https://www.ndbc.noaa.gov/data/realtime2/${station_id}.${data_type}";


    /**
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     * @throws Exception _more_
     */
    public NdbcBuoyTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public String getPathForEntry(Request request, Entry entry, boolean forRead)
            throws Exception {
        String url = URL_TEMPLATE;
        url = url.replace("${station_id}",
                          "" + entry.getValue(IDX_STATION_ID));
        url = url.replace("${data_type}", "" + entry.getValue(IDX_DATA_TYPE));
        return url;
    }


}
