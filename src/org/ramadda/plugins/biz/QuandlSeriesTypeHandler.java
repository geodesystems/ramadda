/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.biz;


import org.ramadda.data.services.PointTypeHandler;

import org.ramadda.repository.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;

import org.w3c.dom.*;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.StringUtil;

import java.net.URL;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;


/**
 */
public class QuandlSeriesTypeHandler extends PointTypeHandler {


    //NOTE: This starts at 2 because the point type has a number of points field

    /** _more_ */
    public static final int IDX_SOURCE_CODE = 2;

    /** _more_ */
    public static final int IDX_SERIES_CODE = 3;



    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public QuandlSeriesTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
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
     * @throws Exception _more_
     */
    @Override
    public String getPathForEntry(Request request, Entry entry, boolean forRead)
            throws Exception {
        String sourceCode = entry.getStringValue(request,IDX_SOURCE_CODE, (String) null);
        if (sourceCode == null) {
            return null;
        }
        String seriesCode = entry.getStringValue(request,IDX_SERIES_CODE, (String) null);
        if (seriesCode == null) {
            return null;
        }

        String url = "https://www.quandl.com/api/v1/datasets/" + sourceCode
                     + "/" + seriesCode + ".csv";
        url = HtmlUtils.url(url, "auth_token",
                            getRepository().getProperty("quandl.api.key",
                                ""));

        //        System.err.println("quandl:" + url);
        return url;
    }


}
