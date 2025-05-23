/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.point;

import org.ramadda.data.point.text.*;
import org.ramadda.data.record.*;
import org.ramadda.data.services.PointTypeHandler;
import org.ramadda.data.services.RecordTypeHandler;
import org.ramadda.repository.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.IO;
import org.ramadda.util.Utils;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import org.w3c.dom.*;

import java.net.URL;
import java.io.*;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;

public class NdbcBuoyTypeHandler extends PointTypeHandler {
    private static int IDX = PointTypeHandler.IDX_LAST + 1;
    private static int IDX_STATION_ID = IDX++;
    private static int IDX_DATA_TYPE = IDX++;
    private static final String URL_TEMPLATE =
        "https://www.ndbc.noaa.gov/data/realtime2/${station_id}.${data_type}";

    public NdbcBuoyTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }

    @Override
    public void initializeNewEntry(Request request, Entry entry,NewType newType)
            throws Exception {
	super.initializeNewEntry(request, entry, newType);
	if(!isNew(newType)) return;
	String type = (String)  entry.getValue(request,IDX_DATA_TYPE);	
	initializeNewEntryInner(request, entry,type);
	String  bulkFile = request.getUploadedFile(ARG_BULKUPLOAD,true);
	if(!stringDefined(bulkFile) || !new File(bulkFile).exists()) return;
	HashSet<String> seen = new HashSet<String>();
	List<Entry> entries = handleBulkUpload(request, entry.getParentEntry(),bulkFile,"station",seen,null,null);
	for(Entry newEntry: entries) {
	    initializeNewEntryInner(request,newEntry,type);
	}
	getEntryManager().insertEntriesIntoDatabase(request,  entries,true, true);	
    }

    private void initializeNewEntryInner(Request request, Entry entry,String type)
            throws Exception {

	String id = (String)  entry.getValue(request,IDX_STATION_ID);
	if(!stringDefined(id)) return;
	entry.setValue(IDX_DATA_TYPE,type);	
	String url = "https://www.ndbc.noaa.gov/station_page.php?station=" + id.toLowerCase();
	try {
	    String html = IO.readUrl(new URL(url));
	    String name =  StringUtil.findPattern(html,"(?i).*currentstnname *= *'([^']+)'");
	    String mtd = StringUtil.findPattern(html,"(?s).*<div id=\"stn_metadata\">(.*?)</p>.*");
	    if(mtd!=null) {
		mtd = mtd.replace("<p>","");
		StringBuilder sb = new StringBuilder("<div>");
		for(String line: Utils.split(mtd,"\n",true,true)) {
		    line = line.replace("<b>","").replace("</b>","");
		    line = line.replace("<p>","").replace("</p>","").replace("<br>","").trim();		    
		    if(line.length()==0) continue;
		    if(((line.indexOf(" N ")>=0) || (line.indexOf(" S ")>=0)) &&
		       ((line.indexOf(" E ")>=0) || (line.indexOf(" W ")>=0))) continue;
		    sb.append(line);
		    sb.append("<br>\n");
		}
		sb.append("</div>");
		entry.setDescription(entry.getDescription()+sb.toString());
		String lat = StringUtil.findPattern(mtd,"<b>([0-9\\.]+ (N|S)) ");
		if(lat!=null) {
		    entry.setLatitude(Misc.decodeLatLon(lat));
		}
		String lon = StringUtil.findPattern(mtd," ([0-9\\.]+ (W|E)) ");
		if(lon!=null) {
		    entry.setLongitude(Misc.decodeLatLon(lon));
		}		
	    }
	    if(name!=null && !stringDefined(entry.getName())) entry.setName(name);
	} catch(Exception exc) {
	    getLogManager().logError("Error reading buoy metadata for id:" + id +" url:" + url,exc);
	    getSessionManager().addSessionMessage(request,"Error reading buoy metadata for station:" +  id +"<br>Url:" + url +
						       "<br>Error:" +exc.getMessage());

	}
    }

    @Override
    public String getPathForEntry(Request request, Entry entry, boolean forRead)
            throws Exception {
        String url = URL_TEMPLATE;
        url = url.replace("${station_id}",
                          "" + entry.getValue(request,IDX_STATION_ID));
        url = url.replace("${data_type}", "" + entry.getValue(request,IDX_DATA_TYPE));
        return url;
    }

    public static void main(String[]args) {
  	String mtd = "<p><b>Owned and maintained by National Data Buoy Center</b><br>\n		<b>3-meter foam buoy</b><br>\n		<b>SCOOP payload</b><br>\n		<b>31.759 N 74.936 W (31&#176;45'33\" N 74&#176;56'10\" W)</b><br>\n		<br>";
	//	String name =  StringUtil.findPattern(s,"(?i).*currentstnname *= *'+([^'])'.*");
	System.err.println(StringUtil.findPattern(mtd,"<b>([0-9\\.]+ (N|S))"));

    }

}
