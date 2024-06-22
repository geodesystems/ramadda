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


public class KboccHydroTypeHandler extends PointTypeHandler {

    private JSONArray loggers;
    public KboccHydroTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
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
	entry.setValue("datayear",sdf.format(new Date(entry.getStartDate())));

	String fileName = getStorageManager().getOriginalFilename(file.getName());
	String _fileName = fileName.replace("_","-");
	String yearSite = StringUtil.findPattern(fileName,"(\\d\\d-\\d\\d)");
	if(yearSite==null)
	    yearSite = StringUtil.findPattern(fileName,"(\\d\\d_\\d\\d)");
	if(yearSite==null)
	    yearSite = StringUtil.findPattern(fileName,"(\\d\\d_\\d)");
	if(yearSite==null)
	    yearSite = StringUtil.findPattern(fileName,"(\\d\\d-\\d)");	
	if(yearSite==null) yearSite="";
	yearSite=yearSite.replace("_","-");
	List<String> toks = Utils.splitUpTo(yearSite,"-",2);
	String site = toks.size()==2?toks.get(1):"";
	JSONObject theLogger=null;
	//First look for year-site
	for(int i=0;theLogger==null && i<loggers.length();i++) {
	    JSONObject logger=loggers.getJSONObject(i);
	    String id = logger.getString("id").trim();
	    if(fileName.equals(id) || _fileName.equals(id) ||fileName.startsWith(id) || id.equals(yearSite)) {
		theLogger = logger;
	    }
	}


	//next look for site
	site = site.trim();
	for(int i=0;theLogger==null && i<loggers.length();i++) {
	    JSONObject logger=loggers.getJSONObject(i);
	    if(logger.has("site")) {
		if(site.equals(logger.getString("site").trim())) {
		    theLogger = logger;
		}
	    }
	}

	if(theLogger!=null) {
	    entry.setLatitude(theLogger.getDouble("latitude"));
	    entry.setLongitude(theLogger.getDouble("longitude"));
	    entry.setValue("location",theLogger.getString("location"));
	    entry.setValue("notes",theLogger.getString("notes"));		
	    JSONObject o = theLogger;
	    System.out.println(Utils.join(Utils.makeListFromValues(o.getString("id"),o.getString("location"),
								   ""+o.getDouble("latitude"),
								   ""+o.getDouble("longitude"),
								   Seesv.cleanColumnValue(o.getString("notes")),
								   fileName),","));
	    
	} else {
	    String msg = "Could not find site info for file:" + fileName;
	    getSessionManager().addSessionErrorMessage(request,msg);
	}
	
    }

}
