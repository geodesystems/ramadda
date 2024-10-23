/**
Copyright (c) 2008-2024 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.geo;

import org.ramadda.repository.output.WikiTagHandler;
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


@SuppressWarnings("unchecked")
public class BoreholeTypeHandler extends PointTypeHandler {


    private JSONArray holes;

    public BoreholeTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);

    }


    @Override
    public void initializeNewEntry(Request request, Entry entry,NewType newType)
            throws Exception {
	super.initializeNewEntry(request, entry,newType);
	if(!isNew(newType)) return;

	if(isType("type_borehole_xrf")) {
	    Double d=(Double)entry.getValue(request, "xrf_voltage");
	    if(d!=null) {	    
		double voltage = d;
		if(voltage==15) {
		    entry.setValue("fields_to_show","k_pct,si_pct,fe_pct,s_pct");
		} else if (voltage==40) {
		    entry.setValue("fields_to_show","fe_pct,sr_pct,zr_pct,ba_pct");
		} else {
		    entry.setValue("fields_to_show","k_pct,si_pct,fe_pct,s_pct");
		}
	    }
	}


	String holeId = (String) entry.getValue(request,"hole");
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
    public void handleHeaderPatternValue(Request request, Entry entry,Hashtable state, String field, String value) throws Exception {
	super.handleHeaderPatternValue(request, entry, state,  field, value);
    }

    @Override
    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props)
	throws Exception {
        if (tag.equals("borehole_profiles")) {
	    StringBuilder sb = new StringBuilder();
	    String fields= Utils.getProperty(props,"fields","");
	    String indexField= Utils.getProperty(props,"indexField",".*depth.*");
						 
	    if(!stringDefined(fields)) {
		fields = (String) entry.getValue(request,"fields_to_show");
	    }

	    if(!stringDefined(fields)) {
		fields = getTypeProperty("borehole.fields","");
	    }

	    if(!stringDefined(fields)) {
		//Hacky way to get the number of fields since the metadata can be edited/changed
		List<Metadata> metadata= getMetadataManager().findMetadata(request, entry, "thredds.variable",false);
		if(metadata!=null) {
		    fields="";
		    HashSet<String> seen = new HashSet<String>();
		    for(int i=1;i<metadata.size();i++) {
			Metadata mtd =metadata.get(i);
			if(Utils.equals(Utils.toLowerCase(mtd.getAttr1()),"depth")) continue;
			if(seen.contains(mtd.getAttr1())) continue;
			seen.add(mtd.getAttr1());
			if(fields.length()>0) fields+=",";
			fields+="#"+ (i+1);
		    }
		}		    
	    }
	    if(!stringDefined(fields)) fields="#2";
	    List<String> ids = Utils.split(fields,",",true,true);
	    sb.append("+row tight=true\n");
	    String template = "{{display_profile loadingMessage=\"\" width=100% height=500px displayInnerStyle=\"border-right:1px solid #000;\" showLegend=false marginRight=0 {extra} max=10000 showMenu=true yAxisReverse=true marginTop=0  profileMode=lines indexField=\"${indexField}\"  fields=\"{field}\"}}\n";
	    template = template.replace("${indexField}",indexField);
	    String height=Utils.getProperty(props,"height",null);
	    for(int i=0;i<ids.size();i++) {
		String id = ids.get(i);
		String extra = i>0?" marginLeft=0 yAxisTitle=\"\" ":"";
		if(i % 2 != 0)
		    extra += " lineColor1=\"#FF7F0E\" ";
		if(height!=null)
		    extra+= " height=\"" + height+"\" ";
		sb.append("+col-"+ Math.round((12/ids.size()))+"\n");
		sb.append(template.replace("{field}",id).replace("{extra}",extra));
		sb.append("-col-"+ Math.round((12/ids.size()))+"\n");
	    }
	    sb.append("-row\n");
	    //	    System.err.println(sb);
	    return getWikiManager().wikifyEntry(request, entry,sb.toString());
	}

	return super.getWikiInclude(wikiUtil,request, originalEntry, entry,tag,props);

    }



}
