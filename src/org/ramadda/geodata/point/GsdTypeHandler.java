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
import org.ramadda.repository.output.WikiConstants;
import org.ramadda.repository.type.*;
import org.ramadda.util.IO;
import org.ramadda.util.Utils;

import org.w3c.dom.*;

import java.io.*;

import java.text.SimpleDateFormat;


import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;


/**
 */
public class GsdTypeHandler extends PointTypeHandler {


    /** _more_ */
    private SimpleDateFormat dateSDF;

    /** _more_ */
    private static int IDX = PointTypeHandler.IDX_LAST + 1;

    /** _more_ */
    private static int IDX_MODEL = IDX++;


    /**
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     * @throws Exception _more_
     */
    public GsdTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }


    /** _more_ */
    private static final String URL_TEMPLATE =
        "https://rucsoundings.noaa.gov/get_soundings.cgi?data_source={model}&latest=latest&n_hrs=1.0&fcst_len=shortest&airport={lat}%2C{lon}&text=Ascii%20text%20%28GSD%20format%29&hydrometeors=false&start=latest";



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param tag _more_
     * @param props _more_
     * @param topProps _more_
     *
     * @return _more_
     */
    @Override
    public String getUrlForWiki(Request request, Entry entry, String tag,
                                Hashtable props, List<String> topProps) {
        if (tag.equals(WikiConstants.WIKI_TAG_CHART)
                || tag.equals(WikiConstants.WIKI_TAG_DISPLAY)) {
            String url = super.getUrlForWiki(request, entry, tag, props,
                                             topProps);

            return url
                   + "&latitude=${latitude}&longitude=${longitude}";
        }

        return super.getUrlForWiki(request, entry, tag, props, topProps);
    }


    /**
     * _more_
     *
     * @param entry _more_
     * @param requestProperties _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public IO.Path getPathForRecordEntry(Request request,Entry entry,
					 Hashtable requestProperties)
            throws Exception {
        String url   = URL_TEMPLATE;
        String lat   = (String) requestProperties.get("latitude");
        String lon   = (String) requestProperties.get("longitude");
        String model = (String) requestProperties.get("model");
        if ((model == null) || model.equals("{model}")) {
            model = (String) entry.getStringValue(IDX_MODEL, "GFS");
        }
        if (model.length() == 0) {
            model = "GFS";
        }
        url = url.replace("{model}", model);
        url = url.replace("{lat}", (lat != null)
                                   ? lat
                                   : "40");
        url = url.replace("{lon}", (lon != null)
                                   ? lon
                                   : "-105");
        url = super.convertPath(request,entry, url, requestProperties);
	//	System.err.println("GSD Sounding URL:" +url);
        return new IO.Path(url);
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
        return getPathForRecordEntry(request,entry, request.getDefinedProperties()).getPath();
    }

}
