/**
   Copyright (c) 2008-2025 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.feed;



import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.AtomUtil;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;
import org.ramadda.util.TTLCache;
import org.ramadda.util.Utils;
import org.ramadda.util.WikiUtil;

import org.ramadda.data.point.text.*;
import org.ramadda.data.record.*;

import org.ramadda.data.services.PointTypeHandler;
import org.ramadda.data.services.RecordTypeHandler;

import ucar.unidata.xml.XmlUtil;


import org.w3c.dom.*;


import java.net.URL;
import java.io.*;
import java.text.SimpleDateFormat;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

import java.util.TimeZone;



@SuppressWarnings("unchecked")
public class AlertsTypeHandler extends GenericTypeHandler {

    public AlertsTypeHandler(Repository repository, Element entryNode)
	throws Exception {
        super(repository, entryNode);
    }

    @Override
    public void getWikiTags(List<String[]> tags, Entry entry) {
	super.getWikiTags(tags, entry);
        tags.add(new String[]{"NWS Alerts","nws_alerts"});
    }


    @Override
    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props)
	throws Exception {
	if(!tag.equals("nws_alerts")) { 
	    return super.getWikiInclude(wikiUtil, request, originalEntry,
					entry, tag, props);
	}

	StringBuilder sb = new StringBuilder();
	sb.append(HU.importJS(getRepository().getHtdocsUrl("/feed/alerts.js")));
	sb.append(HU.cssLink(getRepository().getHtdocsUrl("/feed/alerts.css")));
	String id = HU.getUniqueId("alerts");
	sb.append(HU.div("",HU.attrs("id",id,"class","alerts")));
	
	String url = null;
	String tagProperties =  entry.getStringValue(request,"tag_properties","");
	if(stringDefined(tagProperties)) {
	    Hashtable tagProps = HU.parseHtmlProperties(tagProperties);
	    for (Object key : tagProps.keySet()) {
		if(!props.containsKey(key)) {
		    Object v = tagProps.get(key);
		    props.put(key,v);
		}
	    }
	}

	List attrs = new ArrayList();
	for (Object key : props.keySet()) {
	    Object v = props.get(key);
	    if(key.equals("urls") || key.equals("points") ||
	       key.equals("zones") || key.equals("areas")) {
		v = JU.list(Utils.split(v.toString(),",",true,true),true);
	    } else {
		v = JU.quote(v.toString());
	    }
	    Utils.add(attrs,key.toString(),v);
	}
	if(entry.isFile()) {
	    url = entry.getTypeHandler().getEntryResourceUrl(request, entry);
	} else if(entry.getResource().isUrl()) {
	    url = entry.getResourcePath(request);
	}
	boolean hasProp = props.containsKey("urls")||
	    props.containsKey("zones") ||
	    props.containsKey("areas") ||
	    props.containsKey("points");	    

	String headerMessages = entry.getStringValue(request,"header_messages","");
	if(stringDefined(headerMessages)) {
	    headerMessages = headerMessages.replaceAll("\\\\\\s*\n"," ");
	    for(String line:Utils.split(headerMessages,"\n",true,true)) {
		if(line.startsWith("#")) continue;
		List<String> toks = Utils.splitUpTo(line,":",2);
		if(toks.size()==2) {
		    Utils.add(attrs,"headerMessage."+ toks.get(0),JU.quote(toks.get(1)));
		}
	    }
	}


	if(!hasProp) {
	    if(stringDefined(url)) {
		Utils.add(attrs,"urls",JU.quote(url));
	    }
	    String zones = entry.getStringValue(request,"zones","");
	    if(stringDefined(zones)) {
		Utils.add(attrs,"zones",JU.quoteAll(Utils.split(zones,"\n",true,true)));
	    }
	    String areas = entry.getStringValue(request,"areas","");
	    if(stringDefined(areas)) {
		Utils.add(attrs,"areas",JU.quoteAll(Utils.split(areas,"\n",true,true)));
	    }	
	    List points = new ArrayList();
	    if(entry.hasLocationDefined(request)) {
		double[]point = entry.getLocation(request);
		points.add(point[0]+","+point[1]);
	    }
	    if(points.size()>0) {
		Utils.add(attrs,"points",JU.quoteAll(points));
	    }
	}
	HU.script(sb,"new NwsAlerts(" + HU.squote(id)+"," + JU.map(attrs)+");\n");
	return sb.toString();

    }


}
