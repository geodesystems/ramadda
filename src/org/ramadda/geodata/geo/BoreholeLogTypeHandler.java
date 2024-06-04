/**
Copyright (c) 2008-2024 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.geo;

import org.ramadda.data.point.text.*;
import org.ramadda.data.record.*;
import org.ramadda.data.services.PointTypeHandler;
import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.WikiUtil;
import org.ramadda.util.IO;
import org.ramadda.util.Utils;
import ucar.unidata.util.StringUtil;

import org.w3c.dom.*;
import org.json.*;
import java.net.URL;
import java.io.*;
import org.json.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.List;


public class BoreholeLogTypeHandler extends PointTypeHandler {
    private JSONArray holes;
    public BoreholeLogTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }


    @Override
    public void initializeNewEntry(Request request, Entry entry,NewType newType)
            throws Exception {
	super.initializeNewEntry(request, entry,newType);
	if(!isNew(newType)) return;
	String holeId = (String) entry.getValue("hole");
	if(!stringDefined(holeId)) return;
	if(holes==null) {
	    holes = new JSONArray(getStorageManager().readUncheckedSystemResource("/org/ramadda/geodata/geo/resources/holes.json"));
	}
	holeId=holeId.trim();
	for(int i=0;i<holes.length();i++) {
	    JSONObject hole=holes.getJSONObject(i);
	    String id = hole.getString("id").trim();
	    if(id.equals(holeId)) {
		entry.setLatitude(hole.getDouble("latitude"));
		entry.setLongitude(hole.getDouble("longitude"));		
	    }
	}

    }

    @Override
    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props)
	throws Exception {
        if (tag.equals("borehole_profiles")) {
	    StringBuilder sb = new StringBuilder();
	    String fields=(String) entry.getValue("fields_to_show");
	    if(!stringDefined(fields)) fields="#2";
	    List<String> ids = Utils.split(fields,",",true,true);
	    sb.append("+row\n");
	    String template = "{{display_profile showLegend=false marginRight=0 {extra} max=10000 showMenu=true yAxisReverse=true marginTop=25  profileMode=lines indexField=\".*depth.*\"  fields=\"{field}\"}}\n";
	    for(int i=0;i<ids.size();i++) {
		String id = ids.get(i);
		String extra = i>0?" marginLeft=0 yAxisTitle=\"\" ":"";
		if(i % 2 != 0)
		    extra += " lineColor1=\"#FF7F0E\" ";
		sb.append("+col-"+ Math.round((12/ids.size()))+"\n");
		sb.append(template.replace("{field}",id).replace("{extra}",extra));
		sb.append("-col-"+ Math.round((12/ids.size()))+"\n");
	    }
	    sb.append("-row\n");
	    return getWikiManager().wikifyEntry(request, entry,sb.toString());
	}

	return super.getWikiInclude(wikiUtil,request, originalEntry, entry,tag,props);

    }
}
