/**
Copyright (c) 2008-2024 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.projects.kbocc;

import org.ramadda.data.point.text.*;
import org.ramadda.data.record.*;
import org.ramadda.data.services.PointTypeHandler;
import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.WikiUtil;
import org.ramadda.util.IO;
import org.ramadda.util.Utils;
import org.ramadda.util.seesv.Seesv;

import ucar.unidata.util.StringUtil;


import org.w3c.dom.*;
import org.json.*;
import java.net.URL;
import java.io.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.List;


public class KboccTypeHandler extends PointTypeHandler {

    private JSONArray loggers;
    public KboccTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }

    private boolean nameMatch(String n1,String n2) {
	if(n1.equals(n2)) return true;
	String _n1=  n1.toLowerCase().replace(" ","_");
	String _n2=  n2.toLowerCase().replace(" ","_");
	if(_n1.equals(_n2)) return true;
	return false;
    }

    @Override
    public void initializeNewEntry(Request request, Entry entry,NewType newType)
            throws Exception {

	if(!isNew(newType)) {
	    return;
	}
	super.initializeNewEntry(request, entry, newType);

	if(entry.hasLocationDefined(request)) return;

	File file = entry.getFile();
	if(!file.exists()) return;
	if(loggers==null) {
	    loggers = new JSONArray(getStorageManager().readUncheckedSystemResource("/org/ramadda/projects/kbocc/loggers.json"));
	}
	//Add in the year
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
	String dataYear = sdf.format(new Date(entry.getStartDate()));
	boolean isHydro = getType().equals("type_kbocc_hydro");
	String fileName = getStorageManager().getOriginalFilename(file.getName());
	if(fileName.startsWith("kbocc")) fileName = fileName.substring("kbocc_".length());
	String _fileName = fileName.replace("_","-");
	String year = StringUtil.findPattern(fileName,".*_(\\d\\d\\d\\d).*");
	if(isHydro) {
	    entry.setValue("datayear",dataYear);
	    if(year!=null && !year.equals(dataYear)) {
		getSessionManager().addSessionErrorMessage(request,"Warning: "  + entry.getName() +" Year in filename does not match year in data " + year   +" " + dataYear);
	    }
	    String inst = StringUtil.findPattern(fileName,".*\\d\\d\\d\\d_(\\d\\d)\\..*");
	    if(inst!=null) {
		entry.setValue("instrument",inst);
	    }
	}





	String site = StringUtil.findPattern(fileName,"(^.*)_\\d\\d\\d\\d.*");	
	if(site==null) {
	    site = StringUtil.findPattern(fileName,"(^.*)_merged.*");	
	}

	if(site==null) {
	    site="NA";
	}
	Request queryRequest = getRepository().getAdminRequest();
	//Look up any local site entries
	for(Entry siteEntry: getEntryManager().getEntriesWithType(queryRequest, "type_kbocc_site")) {
	    String name = siteEntry.getName();
	    String alias = siteEntry.getStringValue(request,"alias","");
	    if(nameMatch(site,siteEntry.getName()) || nameMatch(site,alias)) {
		entry.setLatitude(siteEntry.getLatitude(request));
		entry.setLongitude(siteEntry.getLongitude(request));		    
		entry.setValue("site",siteEntry.getName());
		return;
	    }
	}

	JSONObject theLogger=null;
	for(int i=0;theLogger==null && i<loggers.length();i++) {
	    JSONObject logger=loggers.getJSONObject(i);
	    String id = logger.getString("site").trim();
	    if(nameMatch(site,id)) {
		theLogger = logger;
	    }
	}

	if(theLogger!=null) {
	    entry.setLatitude(theLogger.getDouble("latitude"));
	    entry.setLongitude(theLogger.getDouble("longitude"));
	    entry.setValue("site",theLogger.getString("site"));
	    //	    entry.setValue("notes",theLogger.getString("notes"));		
	    JSONObject o = theLogger;
	    /*
	    System.out.println(Utils.join(Utils.makeListFromValues(o.getString("id"),o.getString("location"),
								   ""+o.getDouble("latitude"),
								   ""+o.getDouble("longitude"),
								   Seesv.cleanColumnValue(o.getString("notes")),
								   fileName),","));
	    */
	    
	} else {
	    String msg = "Could not find site info for file:" + fileName;
	    getSessionManager().addSessionErrorMessage(request,msg);
	    System.err.println("KBOCC:" + msg);
	}
	
    }

}
