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

    public static final int IDX_AGENCY_ID = 0;

    public static final int IDX_FARE_URL = 1;

    public static final int IDX_PHONE = 2;

    public static final int IDX_TIMEZONE = 3;

    public static final int IDX_LANGUAGE = 4;

    public static final int IDX_RT_URL = 5;

    public static final int IDX_RT_ID = 6;

    public static final int IDX_RT_PASSWORD = 7;

    public static final String TYPE_AGENCY = "type_gtfs_agency";

    public GtfsAgencyTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }

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
