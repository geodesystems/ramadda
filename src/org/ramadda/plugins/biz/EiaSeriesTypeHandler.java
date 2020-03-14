/*
* Copyright (c) 2008-2019 Geode Systems LLC
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
*     http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.ramadda.plugins.biz;


import org.json.*;

import org.ramadda.data.services.PointTypeHandler;


import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Json;
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
     *
     * @throws Exception _more_
     */
    @Override
    public void initializeNewEntry(Request request, Entry entry, boolean fromImport)
            throws Exception {
        super.initializeNewEntry(request, entry,fromImport);
    }


    /**
     * _more_
     *
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    public void initializeSeries(Entry entry) throws Exception {
        EiaCategoryTypeHandler fcth =
            (EiaCategoryTypeHandler) getRepository().getTypeHandler(
                Eia.TYPE_CATEGORY);
        String seriesId = (String) entry.getValue(IDX_SERIES_ID, null);
        if (seriesId == null) {
            return;
        }

        //TODO: get category ID
        entry.setResource(
            new Resource(
                new URL(
                    "http://www.eia.gov/beta/api/qb.cfm?sdid=" + seriesId)));

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
    public String getPathForEntry(Request request, Entry entry)
            throws Exception {
        String id = entry.getValue(IDX_SERIES_ID, (String) null);
        if (id == null) {
            return null;
        }
        EiaCategoryTypeHandler fcth =
            (EiaCategoryTypeHandler) getRepository().getTypeHandler(
                Eia.TYPE_CATEGORY);
        List<String> args = new ArrayList<String>();
        args.add(Eia.ARG_SERIES_ID);
        args.add(id);
        String url = fcth.makeUrl(Eia.URL_SERIES, args);

        return url;
    }


}
