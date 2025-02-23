/**
Copyright (c) 2008-2025 Geode Systems LLC
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
import org.ramadda.util.IO;

import org.json.*;
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
		      "doRotation",child.getStringValue(request,"do_rotation","false"),
		      "text",JU.quote(info));
	    List<String>boxes = null;
	    List<Box> _boxes = getBoxes(request, child);

	    for(Box box:_boxes) {
		if(boxes==null)boxes=new ArrayList<String>();
		if(box.poly!=null) {
		    boxes.add(JU.map("label",JU.quote(box.label),
				     "polygon",JU.list(box.poly),
				     "top",JU.quote(box.top),
				     "bottom",JU.quote(box.bottom)));
		} else {
		    boxes.add(JU.map("label",JU.quote(box.label),
				     "x",JU.quote(box.x),
				     "y",JU.quote(box.y),
				     "width",JU.quote(box.width),
				     "height",JU.quote(box.height),
				     "top",JU.quote(box.top),
				     "bottom",JU.quote(box.bottom)));
		}
	    };

	    if(boxes!=null) Utils.add(attrs,"boxes",JU.list(boxes));

	    entries.add(JU.map(attrs));
	}

	Utils.add(collection,"data",JU.list(entries));
	return JU.map(collection);
    }

    public Entry findCoreboxEntry(Request request, Entry entry) throws Exception {
	for(Entry child:  getEntryManager().getChildren(request, entry)) {
	    if(child.isFile() && child.getFile().getName().endsWith("corebox.json")) {
		return child;
	    }
	}
	return null;
    }




    public List<Box> getBoxes(Request request, Entry entry) throws Exception  {
	List<Metadata> mtdList=
	    getMetadataManager().findMetadata(request, entry, new String[]{"geo_core_box"}, true);
	List<Box> boxes = new ArrayList<Box>();
	for(Metadata mtd: mtdList) {
	    boxes.add(new CoreApiHandler.Box(mtd));
	}	
	

	//This is from the zoomify drawing
	String annotations  = entry.getStringValue(request,"annotations_json",null);
	//	System.out.println(annotations);
	if(stringDefined(annotations)) {
	    try {
		//		System.out.println(annotations);

		JSONArray a = new JSONArray(annotations);
		for(int i=0;i<a.length();i++) {
		    JSONObject ann = a.getJSONObject(i);
		    JSONArray bodyArray = ann.optJSONArray("body");
		    String top=null;
		    String bottom=null;		    
		    String label = "";
		    if(bodyArray!=null) {
			for(int bidx=0;bidx<bodyArray.length();bidx++) {
			    JSONObject body = bodyArray.getJSONObject(bidx);
			    String purpose= body.optString("purpose","");
			    if(purpose.equals("commenting")) {
				label = body.optString("value","");
				continue;
			    } else if(purpose.equals("tagging")) {
				String v = body.optString("value",null);
				if(v==null) continue;
				if(v.trim().startsWith("top:")) top = v.substring(4).trim();
				else if(v.trim().startsWith("bottom:")) bottom= v.substring(7).trim();
			    }
			}
		    }
		    String value = JU.readValue(ann,"target.selector.value",null);
		    if(value==null) continue;
		    //xywh=pixel:428.0892028808594,54.38945770263672,373.4395446777344,74.16742706298828"
		    int idx = value.indexOf("pixel:");
		    if(idx<0) {
			String points = StringUtil.findPattern(value,"points=\"(.*)\"");
			if(points==null)  continue;
			List<String> toks = Utils.split(points," ",true,true);
			List<Double> poly = new ArrayList<Double>();
			for(String pair: toks) {
			    List<String>tuple = Utils.split(pair,",",true,true);
			    if(tuple.size()!=2) continue;
			    poly.add(new Double(tuple.get(0)));
			    poly.add(new Double(tuple.get(1)));			    
			}
			boxes.add(new Box(label,
					  poly,
					  top==null?Double.NaN:Double.parseDouble(top),
					  bottom==null?Double.NaN:Double.parseDouble(bottom)));
			continue;
		    }
		    value = value.substring(idx+"pixel:".length());
		    List<String> toks = Utils.split(value);
		    if(toks.size()!=4) continue;
		    boxes.add(new Box(label,
				      Double.parseDouble(toks.get(0)),
				      Double.parseDouble(toks.get(1)),
				      Double.parseDouble(toks.get(2)),
				      Double.parseDouble(toks.get(3)),
				      top==null?Double.NaN:Double.parseDouble(top),
				      bottom==null?Double.NaN:Double.parseDouble(bottom)));
		}
	    } catch(Exception exc) {
		System.err.println(exc);
		exc.printStackTrace();
	    }
	}
	/**
	   these don't have the correct box dimensions
	Entry corebox = findCoreboxEntry(request,entry);
	if(corebox!=null) {
	    makeBoxesFromJson(request,entry, corebox,boxes);
	}
	*/
	//	for(Box box: boxes)	    System.err.println("box:" + box);
	return boxes;
    }

    public void makeBoxesFromJson(Request request, Entry entry, Entry corebox, List<Box> boxes) throws Exception {
	JSONObject obj     = new JSONObject(IO.readInputStream(new FileInputStream(corebox.getFile())));
	JSONObject dims= obj.optJSONObject("Dimensions");	    
	if(dims==null) return;
	double mainWidth = dims.getDouble("width");
	double mainHeight = dims.getDouble("height");
	double mainLength = dims.getDouble("length");	
	

	JSONArray comps = obj.optJSONArray("Compartments");
	if(comps==null) return;
	for(int i=0;i<comps.length();i++) {
	    JSONObject comp = comps.getJSONObject(i);
	    JSONObject position= comp.optJSONObject("Position");
	    JSONObject dimensions= comp.optJSONObject("Dimensions");	    
	    if(position==null || dimensions==null) continue;
	    JSONObject origin= position.getJSONObject("origin");
	    double length=dimensions.getDouble("length");
	    double height=dimensions.getDouble("height");	    
	    double width=dimensions.getDouble("width");
	    width=length;
	    double x = origin.getDouble("x");
	    double y = mainHeight-origin.getDouble("y");	    
	    double z = origin.getDouble("z");
	    double[] d = getRegionsDepths(comp);
	    if(d!=null) {
		boxes.add(new Box("",x,y,width,height,d[0],d[1]));
	    } else {
		boxes.add(new Box("",x,y,width,height));
	    }
	}
    }


    public Result processEntriesApi(Request request) throws Exception {
	StringBuilder sb = new StringBuilder();
	Entry entry = getEntryManager().getEntry(request,request.getString(ARG_ENTRYID,""));
	if(entry==null) {
	    return new Result("", new StringBuilder(JsonUtil.map("error",JU.quote("No entry found"))), JU.MIMETYPE);
	}
	List<Entry> children;
	if(entry.getTypeHandler().isType("type_borehole_coreimage")) {
	    children = new ArrayList<Entry>();
	    children.add(entry);
	} else {
	    children = getEntryManager().getChildren(request, entry);
	}
	sb.append(makeEntriesJson(request, entry,children));
	return new Result("", sb, JU.MIMETYPE);
    }

    public void processCorebox(Request request, Entry entry, Entry corebox) throws Exception {
	double min= Double.NaN;
	double max= Double.NaN;	
	JSONObject obj     = new JSONObject(IO.readInputStream(new FileInputStream(corebox.getFile())));
	JSONArray comps = obj.optJSONArray("Compartments");
	if(comps==null) {
	    System.err.println("no compartments");

	    return;
	}
	for(int i=0;i<comps.length();i++) {
	    JSONObject comp = comps.getJSONObject(i);
	    double[]d = getRegionsDepths(comp);
	    if(d==null || Double.isNaN(d[0])) continue;
	    if(Double.isNaN(min) || d[0]<min) min=d[0];
	    if(Double.isNaN(max) || d[1]>max) max=d[1];		
	}

	//	System.err.println("min/max:" + min +" " + max); 
	if(!Double.isNaN(min)){
	    entry.setValue("top_depth",new Double(min));
	    entry.setValue("bottom_depth",new Double(max));	    
	    getEntryManager().updateEntry(request, entry);
	}
    }

    private double[]getRegionsDepths(JSONObject comp) {
	JSONArray regions= comp.optJSONArray("Regions");
	if(regions==null) return null;
	double min= Double.NaN;
	double max= Double.NaN;	
	for(int j=0;j<regions.length();j++) {
	    JSONObject region = regions.getJSONObject(j);
	    JSONObject depth = region.optJSONObject("depth");
	    if(depth==null) continue;
	    double top = depth.getDouble("start");
	    double bottom = top+depth.getDouble("length");		
	    if(Double.isNaN(min) || top<min) min=top;
	    if(Double.isNaN(max) || bottom>max) max=bottom;		
	}
	//The json is in millimeters. the boxes are in meters
	return new double[]{min/1000,max/1000};
    }

    public static class Box {
	public String label;
	public double  x;
	public double  y;
	public double  width;
	public double  height;
	public double  top;
	public double  bottom;
	public List<Double> poly;

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

	Box(String label,
	    double x,
	    double y,
	    double width,
	    double height) {
	    this(label,x,y,width,height,Double.NaN,Double.NaN);
	}


	Box(String label,
	    List<Double>poly,
	    double top,
	    double bottom) {
	    this.label = label;
	    this.poly=poly;
	    this.top = top;
	    this.bottom = bottom;
	}



	public String toString() {
	    return "box:" + label +" x/y:" + x +" " + y +" dim:" + width +"x" + height +" depth:" + top +" " + bottom;
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
