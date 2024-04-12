/**
   Copyright (c) 2008-2023 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.point;

import org.ramadda.data.services.*;
import org.ramadda.repository.Entry;
import org.ramadda.repository.Repository;
import org.ramadda.repository.Request;

import org.ramadda.util.Utils;

import org.w3c.dom.Element;

import java.io.*;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;


/**
 * TypeHandler for Aviation Weather Center METARS
 * https://www.aviationweather.gov/adds/dataserver
 *
 *
 */
public class AwcMetarTypeHandler extends NwsStationTypeHandler {


    public static final String URL = "https://aviationweather.gov/api/data/metar?format=json&ids={station}&hours={offset}";


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


    @Override
    public void initializeNewEntry(Request request, Entry entry,
                                   boolean fromImport)
	throws Exception {
        super.initializeNewEntry(request, entry, fromImport);
        if (fromImport) {
            return;
        }
	String id = (String) entry.getStringValue(IDX_SITE_ID, "");
	initializeNewEntry(request, entry,  id);

	String  bulkFile = request.getUploadedFile(ARG_BULKUPLOAD,true);
	if(!stringDefined(bulkFile) || !new File(bulkFile).exists()) return;

	HashSet<String> seen = new HashSet<String>();
	List<Entry> entries = handleBulkUpload(request, entry.getParentEntry(),bulkFile,"site_id",seen,"^[^-]+$",null);
	for(Entry newEntry: entries) {
	    System.err.println("AwcMetarTypeHandler: bulk entry:" + newEntry.getValue(IDX_SITE_ID));
	    initializeNewEntry(request,newEntry,(String)newEntry.getValue(IDX_SITE_ID));
	}
	getEntryManager().insertEntriesIntoDatabase(request,  entries,true, true);


    }


    @Override
    public String getPathForEntry(Request request, Entry entry,
                                  boolean forRead)
	throws Exception {
        if (entry.isFile()) {
            return super.getPathForEntry(request, entry, forRead);
        }
        String siteId = entry.getStringValue(IDX_SITE_ID, "");
        int    offset = (int) entry.getIntValue(IDX_TIME_OFFSET, 24);
        String url = URL.replace("{station}", siteId).replace("{offset}", "" + offset);
        return url;
    }

}
