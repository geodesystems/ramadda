/**
   Copyright (c) 2008-2024 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.point;

import org.ramadda.data.point.text.*;
import org.ramadda.data.services.PointTypeHandler;
import org.ramadda.repository.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.IO;
import org.ramadda.util.Utils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import org.w3c.dom.*;

import java.net.URL;
import java.io.*;
import java.util.HashSet;
import java.util.List;

/**
This class supports accessing data in real-time from the
USGS National Ground-Water Monitoring Network
https://cida.usgs.gov/ngwmn/index.jsp
*/
public class NgwmTypeHandler extends PointTypeHandler {
    public NgwmTypeHandler(Repository repository, Element node)
	throws Exception {
        super(repository, node);
    }


    /**
       check for bulk upload of site ids
       initialize the entry with metadata extracted from the USGS HTML page for the site (uggh)
     */
    @Override
    public void initializeNewEntry(Request request, Entry entry,NewType newType)
	throws Exception {
	super.initializeNewEntry(request, entry, newType);
	if(!isNew(newType)) return;
	initializeNewEntryInner(request, entry);
	String  bulkFile = request.getUploadedFile(ARG_BULKUPLOAD,true);
	if(!stringDefined(bulkFile) || !new File(bulkFile).exists()) return;
	HashSet<String> seen = new HashSet<String>();
	List<Entry> entries = handleBulkUpload(request, entry.getParentEntry(),bulkFile,"site",seen,null,null);
	for(Entry newEntry: entries) {
	    initializeNewEntryInner(request,newEntry);
	}
	getEntryManager().insertEntriesIntoDatabase(request,  entries,true, true);	
    }


    private void initializeNewEntryInner(Request request, Entry entry)
	throws Exception {
	String id = (String)  entry.getValue(request,"site");
	if(!stringDefined(id)) return;

	String url = getUrl("https://cida.usgs.gov/ngwmn/provider/${agency}/site/${site}/",id);
	try {
	    //	    System.err.println(url);
	    String html = IO.readUrl(new URL(url));
	    String title = StringUtil.findPattern(html,"(?s)(?i)<title>([^<]+)</title>");
	    String name = null;
	    if(title!=null) {
		name = title.replaceAll("\n"," ");
		name = name.trim();
		name = name.replaceAll("National\\s*Ground\\s*Water\\s*Monitoring\\s*Network","").trim();
		name = name.replaceAll("-$","").trim();
	    }
	    String lat = find(html,"Latitude");
	    if(lat!=null) {
		entry.setLatitude(Misc.decodeLatLon(lat));
	    }
	    String lon = find(html,"Longitude");
	    if(lon!=null) {
		entry.setLongitude(Misc.decodeLatLon(lon));
	    }
	    String[]args ={"Agency","agency","Site Type","site_type",
			   "Well Depth","well_depth","Local Aquifer Name","local_aquifer",
			   "National Aquifer Name","national_aquifer",
			   "Aquifer Type","aquifer_type",				
			   "Water-Level Well Type","well_type"};
	    for(int i=0;i<args.length;i+=2) {
		String v = find(html,args[i]);
		if(v!=null) {
		    if(args[i+1].equals("well_depth")) {
			v = v.replaceAll("[^0-9\\.]+","").trim();
			try {
			    double d = Double.parseDouble(v);
			} catch(NumberFormatException nfe) {
			    continue;
			}
		    }
		    v = v.trim();
		    if(v.equals("-")) v="";
		    entry.setValue(args[i+1],v.trim());
		}
	    }
	    String info = find(html,"Additional info");
	    if(info!=null) {
		info = StringUtil.findPattern(info,"href=\"(.*)\"");
		if(info!=null) {
		    info=info.trim();
		    if(!info.startsWith("http")) info = "https://" + info;
		    entry.setValue("link",info.trim());
		}
	    }
	    if(name!=null && !stringDefined(entry.getName())) entry.setName(name);
	} catch(Exception exc) {
	    getLogManager().logError("Error reading site metadata for id:" + id +" url:" + url,exc);
	    getSessionManager().addSessionMessage(request,"Error reading site metadata for station:" +  id +"<br>Url:" + url +
						  "<br>Error:" +exc.getMessage());

	}
    }

    @Override
    public String getPathForEntry(Request request, Entry entry, boolean forRead)
	throws Exception {
	String id = (String)  entry.getValue(request,"site");
	return  getUrl("https://cida.usgs.gov/ngwmn_cache/direct/flatXML/waterlevel/${agency}/${site}",id);
    }

    private String find(String html,String key) {
	String p = "(?s)(?i)<th.*?>" + key+"</th>\\s*<td>(.*?)</td";
	String v = StringUtil.findPattern(html,p);
	if(v!=null) v = v.trim();
	return v;
    }

    /**
       split the id into network:site and apply those values to the url template
     */
    private String getUrl(String url,String id) {
	List<String> toks = Utils.split(id,":",true,true);
	if(toks.size()!=2)
	    throw new IllegalArgumentException("Incorrect Site ID:" + id +". It must be of the form agency:Site #, e.g. UNLCSD:241551");
	return url.replace("${agency}",toks.get(0)).replace("${site}",toks.get(1));
	
    }
}
