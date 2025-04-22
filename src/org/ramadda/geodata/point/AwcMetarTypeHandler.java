/**
   Copyright (c) 2008-2025 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.point;

import org.ramadda.data.services.*;
import org.ramadda.repository.Entry;
import org.ramadda.repository.Repository;
import org.ramadda.repository.Request;
import java.sql.ResultSet;
import java.sql.Statement;
import ucar.unidata.util.Misc;

import org.ramadda.util.Utils;

import org.w3c.dom.Element;

import java.io.*;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import org.json.*;

/**
 * TypeHandler for Aviation Weather Center METARS
 * https://www.aviationweather.gov/adds/dataserver
 */
public class AwcMetarTypeHandler extends NwsStationTypeHandler {

    public static final String URL = "https://aviationweather.gov/api/data/metar?format=json&ids={station}&hours={offset}";

    private static int IDX =
        org.ramadda.data.services.RecordTypeHandler.IDX_LAST + 1;

    public static final int IDX_SITE_ID = IDX++;
    public static final int IDX_STATE = IDX++;
    public static final int IDX_TIME_OFFSET = IDX++;

    public AwcMetarTypeHandler(Repository repository, Element node)
	throws Exception {
        super(repository, node);
	//Not now
	//	Misc.runInABit(5000,new Runnable() {public void run() {doCleanup();}});
    }

    private void doCleanup() {
	/*
	  Table:TYPE_AWC_METAR
	  ID (VARCHAR 200)
	  SITE_ID (VARCHAR 200)
	  TIME_OFFSET (INTEGER 10)
	  STATE (VARCHAR 200)
	*/

	try {
	    Request request = getRepository().getAdminRequest();
	    String table = "TYPE_AWC_METAR";
            Statement statement =
                getDatabaseManager().select("ID,SITE_ID",
					    table,
                                            new org.ramadda.util.sql.Clause[] {});
            ResultSet  results = statement.getResultSet();

            while (results.next()) {
                String id  = results.getString(1);
                String site  = results.getString(2);		
		JSONObject station = getStation(site);
		if(station==null) {
		    continue;
		}
		getDatabaseManager().update(table, "ID",id, new String[]{"STATE"},
					    new Object[]{station.optString("st","")});
		System.err.println("awc:" + id +" state:" +station.optString("st",""));
		//Force the reindex
		Entry entry = getEntryManager().getEntry(request,id);
		if(entry==null) {
		    System.err.println("usgs: could not find entry:" + id);
		    continue;
		}
		List<Entry> entries = new ArrayList<Entry>();
		entries.add(entry);
		getSearchManager().entriesModified(request,  entries);
            }
            getDatabaseManager().closeAndReleaseConnection(statement);
	}catch(Exception exc) {
	    exc.printStackTrace();
	}

    }

    @Override
    public void initializeNewEntry(Request request, Entry entry,NewType newType)
	throws Exception {
        super.initializeNewEntry(request, entry, newType);
	if(!isNew(newType)) return;
	String id = (String) entry.getStringValue(request,IDX_SITE_ID, "");
	initializeStation(request, entry,  id);

	String  bulkFile = request.getUploadedFile(ARG_BULKUPLOAD,true);
	if(!stringDefined(bulkFile) || !new File(bulkFile).exists()) return;

	HashSet<String> seen = new HashSet<String>();
	List<Entry> entries = handleBulkUpload(request, entry.getParentEntry(),bulkFile,"site_id",seen,"^[^-]+$",null);
	for(Entry newEntry: entries) {
	    initializeStation(request,newEntry,(String)newEntry.getValue(request,IDX_SITE_ID));
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
        String siteId = entry.getStringValue(request,IDX_SITE_ID, "");
        int    offset = (int) entry.getIntValue(request,IDX_TIME_OFFSET, 24);
        String url = URL.replace("{station}", siteId).replace("{offset}", "" + offset);
        return url;
    }

}
