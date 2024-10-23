/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.geo;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.search.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JsonUtil;

import org.ramadda.util.Utils;
import org.ramadda.util.geo.GeoResource;
import org.ramadda.util.geo.Place;

import org.w3c.dom.*;

import ucar.unidata.ui.HttpFormEntry;

import ucar.unidata.util.IOUtil;


import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.io.*;


import java.net.*;

import java.util.ArrayList;

import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Provides a top-level API
 *
 */
public class CoreApiHandler extends RepositoryManager implements RequestHandler {

    public CoreApiHandler(Repository repository) throws Exception {
        super(repository);
    }


    public String makeEntriesJson(Request request, Entry entry,List<Entry> children)  throws Exception {
	List<String> collection = new ArrayList<String>();
	Utils.add(collection,"name",JU.quote(entry.getName()),"entryId",JU.quote(entry.getId()));
	List<String> legends=new ArrayList<String>();
	for(Metadata mtd: getMetadataManager().findMetadata(request, entry, new String[]{"geo_core_legend"}, true)) {
	    String[]tuple=  getMetadataManager().getFileUrl(request, entry, mtd);
	    if(tuple==null) continue;
	    List<String>obj = new ArrayList<String>();
	    String url = tuple[1];
	    Utils.add(obj,"url",JU.quote(url),"top",JU.quote(mtd.getAttr2()),"bottom",
		      JU.quote(mtd.getAttr3()));

	    legends.add(JU.map(obj));
	}
	if(legends.size()>0) {
	    Utils.add(collection,"legends",JU.list(legends));
	}
	List<String> annotations=new ArrayList<String>();
	for(Metadata mtd: getMetadataManager().findMetadata(request, entry, new String[]{"geo_core_annotation"}, true)) {
	    String label = mtd.getAttr1();
	    String depth = mtd.getAttr2();
	    String style = mtd.getAttr3();	    
	    String desc =  mtd.getAttr4();	    
	    if(Utils.stringDefined(depth)) {
		annotations.add(depth+";"+ label+";"+Utils.encodeBase64(style,true)+";"+Utils.encodeBase64(desc,true));
	    }
	}

	if(annotations.size()>0) {
	    Utils.add(collection,"annotations",JU.quote(Utils.join(annotations,",")));
	}

	List<String> entries = new ArrayList<String>();
	for(Entry child: children) {
	    String info =getMapManager().encodeText(getMapManager().makeInfoBubble(request, child));
	    String url = getEntryManager().getEntryResourceUrl(request, child);
	    List<String> attrs = new ArrayList<String>();
	    Utils.add(attrs,"url",JU.quote(url),"label",JU.quote(child.getName()),
		      "entryId",JU.quote(child.getId()),
		      "topDepth",JU.quote(child.getStringValue(request,"top_depth","")),
		      "bottomDepth",JU.quote(child.getStringValue(request,"bottom_depth","")),
		      "text",JU.quote(info));
	    List<String>boxes = null;
	    List<CoreApiHandler.Box> _boxes = getBoxes(request, child);

	    for(Box box:_boxes) {
		if(boxes==null)boxes=new ArrayList<String>();
		boxes.add(JU.map("label",JU.quote(box.label),
				 "x",JU.quote(box.x),
				 "y",JU.quote(box.y),
				 "width",JU.quote(box.width),
				 "height",JU.quote(box.height),
				 "top",JU.quote(box.top),
				 "bottom",JU.quote(box.height)));
	    };

	    if(boxes!=null) Utils.add(attrs,"boxes",JU.list(boxes));

	    entries.add(JU.map(attrs));

	}

	Utils.add(collection,"data",JU.list(entries));
	return JU.map(collection);
    }

    public List<Box> getBoxes(Request request, Entry entry) throws Exception  {
	List<Metadata> mtdList=
	    getMetadataManager().findMetadata(request, entry, new String[]{"geo_core_box"}, true);
	List<CoreApiHandler.Box> boxes = new ArrayList<CoreApiHandler.Box>();
	for(Metadata mtd: mtdList) {
	    boxes.add(new CoreApiHandler.Box(mtd));
	}	
	return boxes;
    }

    public Result processEntriesApi(Request request) throws Exception {
	StringBuilder sb = new StringBuilder();
	Entry entry = getEntryManager().getEntry(request,request.getString(ARG_ENTRYID,""));
	if(entry==null) {
	    return new Result("", new StringBuilder(JsonUtil.map("error",JU.quote("No entry found"))), JU.MIMETYPE);
	}
	List<Entry> children = getEntryManager().getChildren(request, entry);
	sb.append(makeEntriesJson(request, entry,children));
	return new Result("", sb, JU.MIMETYPE);
    }

    public static class Box {
	public String label;
	public double  x;
	public double  y;
	public double  width;
	public double  height;
	public double  top;
	public double  bottom;

	Box(Metadata m) {
	    this(m.getAttr(1),
		 p(m.getAttr(2)),		 
		 p(m.getAttr(3)),
		 p(m.getAttr(4)),
		 p(m.getAttr(5)),
		 p(m.getAttr(6)),
		 p(m.getAttr(7)));		 
	}

	Box(String label,
	    double x,
	    double y,
	    double width,
	    double height,
	    double top,
	    double bottom) {
	    this.label = label;
	    this.x = x;
	    this.y = y;
	    this.width = width;
	    this.height = height;
	    this.top = top;
	    this.bottom = bottom;
	}

	private static double p(String s) {
	    if(!Utils.stringDefined(s)) return Double.NaN;
	    try{
		return Double.parseDouble(s);
	    } catch(Exception e) {
		return Double.NaN;
	    }
	}
    }
	


}
