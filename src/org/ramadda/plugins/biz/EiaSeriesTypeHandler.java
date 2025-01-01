/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.biz;


import org.json.*;

import org.ramadda.data.services.PointTypeHandler;


import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.TTLCache;
import org.ramadda.util.Utils;

import org.w3c.dom.*;
import org.w3c.dom.*;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.net.URL;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;


/**
 */
public class EiaSeriesTypeHandler extends PointTypeHandler {


    //NOTE: This starts at 2 because the point type has a number of points field

    /** _more_ */
    public static final int IDX_SERIES_ID = 2;

    /** _more_ */
    public static final int IDX_FREQUENCY = 3;

    /** _more_ */
    public static final int IDX_UNITS = 4;

    /** _more_ */
    public static final int IDX_SEASONAL_ADJUSTMENT = 5;


    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public EiaSeriesTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param fromImport _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void initializeNewEntry(Request request, Entry entry,NewType newType)
            throws Exception {
        super.initializeNewEntry(request, entry, newType);
    }


    /**
     * _more_
     *
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    public void initializeSeries(Request request,Entry entry) throws Exception {
        EiaCategoryTypeHandler fcth =
            (EiaCategoryTypeHandler) getRepository().getTypeHandler(
                Eia.TYPE_CATEGORY);
        String seriesId = (String) entry.getStringValue(request,IDX_SERIES_ID, null);
        if (seriesId == null) {
            return;
        }

        //TODO: get category ID
        entry.setResource(
            new Resource(
                new URL(
                    "https://www.eia.gov/beta/api/qb.cfm?sdid=" + seriesId)));

        //Don't do this for now since it takes too long with lots of series

        /*
        List<String> args = new ArrayList<String>();
        args.add(Eia.ARG_SERIES_ID);
        args.add(seriesId);
        args.add(Eia.ARG_NUM);
        args.add("1");
        Element root = fcth.call(Eia.URL_SERIES, args);
        Object[] values = getEntryValues(entry);
        Element  node   = XmlUtil.findChild(root, Eia.TAG_SERIES);
        if(node == null) return;
        Element  row   = XmlUtil.findChild(node, Eia.TAG_ROW);
        entry.setName(XmlUtil.getAttribute(node, Eia.ATTR_NAME, entry.getName()));
        entry.setDescription(XmlUtil.getAttribute(node, Eia.ATTR_DESCRIPTION, ""));
        */
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
        String id = entry.getStringValue(request,IDX_SERIES_ID, (String) null);
        if (id == null) {
            return null;
        }
        EiaCategoryTypeHandler fcth =
            (EiaCategoryTypeHandler) getRepository().getTypeHandler(
                Eia.TYPE_CATEGORY);
        List<String> args = new ArrayList<String>();
	String url = Eia.URL_SERIES+"/" + id;
	if(!forRead) return url;
	url = fcth.makeUrl(url, args,"json");
        return url;
    }


}
