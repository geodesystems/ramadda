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
import org.ramadda.util.geo.GeoUtils;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JsonUtil;

import org.ramadda.util.Utils;
import org.ramadda.util.geo.GeoResource;
import org.ramadda.util.geo.Place;
import org.ramadda.util.IO;

import org.json.*;
import org.w3c.dom.*;


import ucar.unidata.ui.HttpFormEntry;
import org.ramadda.util.ImageUtils;
import ucar.unidata.xml.XmlUtil;
import ucar.unidata.util.IOUtil;

import org.w3c.dom.*;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.io.*;
import java.awt.Image;
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

    public static final GeoUtils GU = null;

    
    public CoreApiHandler(Repository repository) throws Exception {
        super(repository);
    }


    public Element getRoot(Entry entry) throws Exception {
	return XU.getRoot(entry.getFile().toString(),CoreApiHandler.class);
    }

    public String makeEntriesJson(Request request, Entry entry,List<Entry> children)  throws Exception {
	List<String> collection = new ArrayList<String>();
	Utils.add(collection,"name",JU.quote(entry.getName()),"entryId",JU.quote(entry.getId()));
	List<String> legends=new ArrayList<String>();
	for(Metadata mtd: getMetadataManager().findMetadata(request, entry, new String[]{"geo_core_legend"}, true)) {
	    String[]tuple=  getMetadataManager().getFileUrl(request, entry, mtd);
	    //	    if(tuple==null) continue;
	    List<String>obj = new ArrayList<String>();
	    String url = tuple==null?"":tuple[1];
	    Utils.add(obj,
		      "url",JU.quote(url),
		      "top",     JU.quote(mtd.getAttr2()),
		      "bottom",   JU.quote(mtd.getAttr3()),
		      "width",   JU.quote(mtd.getAttr4()));

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
	    if(!child.getTypeHandler().isType("type_borehole_coreimage")) continue;
	    String info =getMapManager().encodeText(getMapManager().makeInfoBubble(request, child));
	    String url = getEntryManager().getEntryResourceUrl(request, child);
	    List<String> attrs = new ArrayList<String>();
	    double top  = child.getDoubleValue(request,"top_depth",Double.NaN);
	    double bottom  = child.getDoubleValue(request,"bottom_depth",Double.NaN);	    
	    if(Double.isNaN(top) || Double.isNaN(bottom)) continue;
	    Utils.add(attrs,"url",JU.quote(url),"label",JU.quote(child.getName()),
		      "entryId",JU.quote(child.getId()),
		      "topDepth",JU.quote(Double.toString(top)),
		      "bottomDepth",JU.quote(Double.toString(bottom)),
		      "doRotation",child.getStringValue(request,"do_rotation","false"),
		      "text",JU.quote(info));
	    List<String>boxes = null;
	    List<Box> _boxes = getBoxes(request, child);

	    for(Box box:_boxes) {
		if(boxes==null)boxes=new ArrayList<String>();
		String stroke = box.stroke==null?"null":JU.quote(box.stroke);
		String strokeWidth = box.strokeWidth==null?"null":JU.quote(box.strokeWidth);		
		String fill = box.fill==null?"null":JU.quote(box.fill);		
		if(box.poly!=null) {
		    boxes.add(JU.map("label",JU.quote(box.label),
				     "polygon",JU.list(box.poly),
				     "top",JU.quote(box.top),
				     "bottom",JU.quote(box.bottom),
				     "fill",fill,"stroke",stroke,"strokeWidth",strokeWidth));
		} else {
		    boxes.add(JU.map("label",JU.quote(box.label),
				     "marker",box.marker,
				     "fill",fill,"stroke",stroke,"strokeWidth",strokeWidth,
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
	    if(isPrediktera(child)) return child;
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
		System.out.println(annotations);

		JSONArray a = new JSONArray(annotations);
		for(int i=0;i<a.length();i++) {
		    JSONObject ann = a.getJSONObject(i);
		    JSONArray bodyArray = ann.optJSONArray("body");
		    String top=null;
		    String bottom=null;		    
		    String label = "";
		    String stroke=null;
		    String strokeWidth=null;
		    String fill=null;		    		    

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
				v = v.trim();
				if(v.startsWith("c:")) stroke = v.substring("c:".length()).trim();
				else if(v.startsWith("color:")) stroke = v.substring("color:".length()).trim();
				else if(v.startsWith("fill:")) fill= v.substring("fill:".length()).trim();
				else if(v.startsWith("w:")) strokeWidth = v.substring("w:".length()).trim();
				else if(v.startsWith("width:")) strokeWidth = v.substring("width:".length()).trim();				
				else if(v.startsWith("top:")) top = v.substring(4).trim();
				else if(v.startsWith("bottom:")) bottom= v.substring(7).trim();
			    }
			}
		    }
		    String value = JU.readValue(ann,"target.selector.value",null);
		    if(value==null) continue;
		    //xywh=pixel:428.0892028808594,54.38945770263672,373.4395446777344,74.16742706298828"
		    int idx = value.indexOf("pixel:");
		    Box  box=null;
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
			boxes.add(box=new Box(label,
					  poly,
					  top==null?Double.NaN:Double.parseDouble(top),
					  bottom==null?Double.NaN:Double.parseDouble(bottom)));
			continue;
		    }
		    value = value.substring(idx+"pixel:".length());
		    List<String> toks = Utils.split(value);
		    if(toks.size()!=4) continue;
		    boxes.add(box=new Box(label,
				      Double.parseDouble(toks.get(0)),
				      Double.parseDouble(toks.get(1)),
				      Double.parseDouble(toks.get(2)),
				      Double.parseDouble(toks.get(3)),
				      top==null?Double.NaN:Double.parseDouble(top),
				      bottom==null?Double.NaN:Double.parseDouble(bottom)));
		    if(box!=null) {
			if(stroke!=null) box.stroke = stroke;
			box.fill =fill;
			box.strokeWidth = strokeWidth;
		    }
		}
	    } catch(Exception exc) {
		System.err.println(exc);
		exc.printStackTrace();
	    }
	}

	Entry child  = findCoreboxEntry(request,entry);
	if(child!=null && isPrediktera(child)) {
	    Image image = ImageUtils.readImage(entry.getResource().getPath());
	    if (image.getWidth(null) <0) {
                ImageUtils.waitOnImage(image);
	    }
	    double imageWidth = image.getWidth(null);
	    double imageHeight = image.getHeight(null);
	    if(imageHeight<0 || imageWidth<0) {
		getLogManager().logSpecial("CoreApiHandler: image width/height bad:" + entry);
	    }
	    Element root = getRoot(child);
	    List depths = XU.findChildren(root,"depth");
	    double resolution  = entry.getDoubleValue(request,"resolution",Double.NaN);
	    double lastDepth = entry.getDoubleValue(request,"top_depth",Double.NaN);
	    int lastPixel = 0;
	    for (int depthIdx = 0; depthIdx < depths.size(); depthIdx++) {
		Element depthNode = (Element) depths.get(depthIdx);
		boolean vertical = XU.getAttribute(depthNode,"direction","Vertical").toLowerCase().equals("vertical");
		double depthWidth = XU.getAttribute(depthNode,"width",-1.0);
		double depthWidth2 = depthWidth/2.0;
		List lines = XU.findChildren(depthNode,"line");
		for (int lineIdx = 0; lineIdx < lines.size(); lineIdx++) {
		    //		    if(lineIdx>=1) break;
		    Element lineNode = (Element) lines.get(lineIdx);
		    double linePosition = XU.getAttribute(lineNode,"position",Double.NaN);	
		    if(Double.isNaN(linePosition)) continue;
		    List areas = XU.findChildren(lineNode,"area");
		    double lineDepth=Double.NaN;
		    double lineDepthPixel= Double.NaN;
		    for (int areaIdx = 0; areaIdx < areas.size(); areaIdx++) {
			Element areaNode = (Element) areas.get(areaIdx);
			int aposition = XU.getAttribute(areaNode,"position",-1);
			if(aposition>=0) {
			    double d1=convertPrediktera(resolution, XU.getAttribute(areaNode,"depth",Double.NaN));
			    String label = "";
			    if(!Double.isNaN(d1)) {
				label  =Utils.decimals(d1,2)+"";
			    }
			    lastDepth=d1;
			    lastPixel = aposition;
			    double x=aposition;
			    double y = linePosition-depthWidth2;
			    if(vertical) {
				x = imageWidth-linePosition+depthWidth2;
				y = aposition;
			    }
			    System.err.println("marker:" + GU.metersToFeet(d1));
			    lineDepth=d1;
			    lineDepthPixel=x;
			    Box box = new Box(label,x,y,5,10,d1,d1);
			    box.fill="rgba(0,255,255,0.9)";
			    box.stroke="rgba(0,0,0,0)";
			    box.marker = true;
			    boxes.add(box);
			} else {
			    int start = XU.getAttribute(areaNode,"start",-1);
			    int end = XU.getAttribute(areaNode,"end",-1);			
			    if(start<0 || end<0) continue;
			    double d1=lastDepth+GU.mmToMeters((start-lastPixel)*resolution);
			    double d2=d1+GU.mmToMeters((end-start)*resolution);
			    /*			    System.err.println("start:" + start+ " end:" + end +
					       " last depth:" + Utils.decimals(GU.metersToFeet(GU.mmToMeters(lastDepth)),2) +
					       " depth:" + Utils.decimals(GU.metersToFeet(GU.mmToMeters(d1)),2) +" - " +
					       Utils.decimals(GU.metersToFeet(GU.mmToMeters(d2)),2));
			    */
			    lastPixel=end;
			    lastDepth=d2;
			    
			    //			    d1 =d2 = Double.NaN;
			    //			    d1=GU.mmToMeters(d1);
			    //			    d2 = GU.mmToMeters(d2);
			    System.err.println("box:" + GU.metersToFeet(d1) +" " +
					       GU.metersToFeet(d2));
			    //			    System.err.println(start+ " " + end +" " + GU.metersToFeet(d1) +" " +
			    //					       GU.metersToFeet(d2));
			    String type = XU.getAttribute(areaNode,"type","");
			    double x=start;
			    double y =linePosition-depthWidth2;
			    double width = end-start;
			    double height = depthWidth;
			    if(!Double.isNaN(lineDepth)) {
			    }
			    if(vertical) {
				x = imageWidth - linePosition - depthWidth2;
				y = start;
				height = end - start;
				width = depthWidth;
			    }
			    Box box = new Box("",x,y,width,height,d1,d2);
			    if(type.equals("Ruble")) box.fill="rgba(0,255,255,0.3)";
			    else if(type.equals("Core")) box.fill="rgba(255,255,0,0.3)";			    
			    boxes.add(box);
			}
		    }
		}
	    }
	}

	/**
	   these don't have the correct box dimensions
	Entry corebox = findCoreboxEntry(request,entry);
	if(corebox!=null) {
	    makeBoxesFromJson(request,entry, corebox,boxes);
	}
	*/
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


    public boolean isPrediktera(Entry entry) {
	return entry.getTypeHandler().isType("type_geo_corebox_prediktera_xml");
    }

    public void processCorebox(Request request, Entry entry, Entry corebox) throws Exception {
	if(corebox.isFile() && corebox.getFile().getName().endsWith("corebox.json")) {
	    processCoreboxJson(request, entry,corebox);
	}
	if(isPrediktera(corebox)) {
	    processCoreboxPrediktera(request, entry,corebox);
	}	
    }
	
    private double convertPrediktera(double resolution, double v) {
	//mm/pixel * pixel = mm
	return  GU.mmToMeters(resolution*v);
    }

    public void processCoreboxPrediktera(Request request, Entry entry, Entry corebox) throws Exception {
	boolean changed = false;
	double min= Double.NaN;
	double max= Double.NaN;	
	double resolution = Double.NaN;
	Element root = getRoot(corebox);
	Element settings =  XU.findChild(root,"settings");
	if(settings !=null) {
	    Element logging =  XU.findChild(settings,"logging");
	    if(logging!=null) {
		try {
		    min = Double.parseDouble(XU.getGrandChildText(logging,"startDepth"));
		    max = Double.parseDouble(XU.getGrandChildText(logging,"endDepth"));
		} catch(Exception exc) {
		    getLogManager().logError("Extracting depth from:" + entry,exc);
		}
	    }
	}

	Element metadata =  XU.findChild(root,"metadata");	
	if(metadata!=null) {
	    List data = XU.findChildren(metadata,"data");
	    for (int dataIdx = 0; dataIdx < data.size(); dataIdx++) {
		Element dataNode  = (Element)data.get(dataIdx);
		String name = XU.getAttribute(dataNode,"name");
		String value = XU.getChildText(dataNode);
		if(Utils.stringDefined(name) && Utils.stringDefined(value)) {
		    if(name.equals("Resolution")) {
			resolution = Double.parseDouble(value);
			entry.setValue("resolution",new Double(resolution));
		    }
		    getMetadataManager().addMetadata(request,entry, "property",true,name,value);
		    changed = true;
		}
	    }
	}

	if(!Double.isNaN(min) && !Double.isNaN(resolution)){
	    min = convertPrediktera(resolution,min);
	    max = convertPrediktera(resolution,max);
	    entry.setValue("top_depth",new Double(min));
	    entry.setValue("bottom_depth",new Double(max));	    
	    System.err.println("top:" + min  +" bottom:" + max);
	    changed = true;
	}


	if(changed) {
	    entry.setMetadataChanged(true);
	    getEntryManager().updateEntry(request, entry);
	}

    }




    public void processCoreboxJson(Request request, Entry entry, Entry corebox) throws Exception {


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
	return new double[]{GU.mmToMeters(min),GU.mmToMeters(max)};
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
	public String stroke;
	public String strokeWidth;	
	public String fill;	
	public boolean marker=false;

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
