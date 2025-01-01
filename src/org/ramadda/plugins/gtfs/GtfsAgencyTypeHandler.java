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
import org.ramadda.util.WikiUtil;


import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;


import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.util.Date;
import java.util.Hashtable;
import java.util.List;


/**
 *
 *
 */
public class GtfsAgencyTypeHandler extends ExtensibleGroupTypeHandler {



    /** _more_ */
    public static final int IDX_AGENCY_ID = 0;

    /** _more_ */
    public static final int IDX_FARE_URL = 1;

    /** _more_ */
    public static final int IDX_PHONE = 2;

    /** _more_ */
    public static final int IDX_TIMEZONE = 3;

    /** _more_ */
    public static final int IDX_LANGUAGE = 4;


    /** _more_ */
    public static final int IDX_RT_URL = 5;

    /** _more_ */
    public static final int IDX_RT_ID = 6;

    /** _more_ */
    public static final int IDX_RT_PASSWORD = 7;

    /** _more_ */
    public static final String TYPE_AGENCY = "type_gtfs_agency";

    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public GtfsAgencyTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }

    /**
     * _more_
     *
     * @param wikiUtil _more_
     * @param request _more_
     * @param originalEntry _more_
     * @param entry _more_
     * @param tag _more_
     * @param props _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props)
            throws Exception {

        if ( !tag.equals("gtfs.schedule")) {
            return super.getWikiInclude(wikiUtil, request, originalEntry,
                                        entry, tag, props);
        }

        return "The schedule goes here";
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param map _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public boolean addToMap(Request request, Entry entry, MapInfo map)
            throws Exception {
        //Don't call super as this adds the folders, etc
        //super.addToMap(request, entry, map);
        List<Entry> vehicles = Gtfs.getVehicles(request, entry);
        System.err.println("all vehicles: " + vehicles);
        Gtfs.addToMap(request, vehicles, map);

        return false;
    }

}
