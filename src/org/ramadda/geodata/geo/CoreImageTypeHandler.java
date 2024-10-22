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
import ucar.unidata.util.IOUtil;

import org.ramadda.util.ImageUtils;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import javax.imageio.*;
import java.util.Base64;
import java.awt.geom.Rectangle2D;
import java.awt.image.*;

import org.w3c.dom.*;
import org.json.*;
import java.net.URL;
import java.io.*;
import org.json.*;

import java.util.zip.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.List;


@SuppressWarnings("unchecked")
public class CoreImageTypeHandler extends GenericTypeHandler implements WikiTagHandler {


    public CoreImageTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }


    public Result processEntryAction(Request request, Entry entry)
	throws Exception {
        String action = request.getString("action", "");
	if(!action.equals("applyboxes")) {
	    return super.processEntryAction(request,entry);
	}
	StringBuilder sb = new StringBuilder();
	getPageHandler().entrySectionOpen(request, entry, sb,"Apply Boxes");

	List<Metadata> mtdList=
	    getMetadataManager().findMetadata(request, entry, new String[]{"geo_core_box"}, true);
	if(mtdList.size()==0) {
	    sb.append(getWikiManager().wikifyEntry(request,entry,"+callout-info\nThere are no box properies\n-callout-info\n"));
	    getPageHandler().entrySectionClose(request, entry, sb);
	    return getEntryManager().addEntryHeader(request, entry,
						    new Result("Apply Boxes",sb));
	}
	sb.append(getWikiManager().wikifyEntry(request,entry,"+callout-info\nThis applies the boxes to the image\n-callout-info\n"));
	sb.append(request.uploadForm(getRepository().URL_ENTRY_ACTION,""));
	sb.append(HU.hidden(ARG_ENTRYID, entry.getId()));
	sb.append(HU.hidden(ARG_ACTION, "applyboxes"));
	sb.append(HU.labeledCheckbox("aszip","true",request.get("aszip",false),"Download Zip File"));
	sb.append("<br>");
	
	sb.append(HU.submit("Apply Boxes","apply"));
	sb.append(HU.formClose());

	if(request.exists("apply")) {
	    Result result =applyBoxes(request,entry,sb,mtdList);
	    if(result!=null) return result;
	}	


	getPageHandler().entrySectionClose(request, entry, sb);
	return getEntryManager().addEntryHeader(request, entry,
						new Result("Apply Boxes",sb));

    }

    private Result   applyBoxes(Request request,Entry entry,StringBuilder sb,List<Metadata> metadataList) throws Exception {
	Image image = ImageUtils.readImage(entry.getResource().getPath());
	BufferedImage bi    = ImageUtils.toBufferedImage(image);
	boolean asZip = request.get("aszip",false);
        FileWriter fileWriter     = null;
	OutputStream os = null;
        ZipOutputStream zos = null;

	if(asZip) {
	    request.setReturnFilename(entry.getName()+"_boxes.zip",false);
	    os = request.getHttpServletResponse().getOutputStream();	
	    zos = new ZipOutputStream(os);
	}
	

	

	int cnt=0;
	StringBuilder csv = new StringBuilder();

	csv.append("box,red,green,blue\n");
	
	
	for(Metadata mtd: metadataList) {
	    cnt++;
	    String label =mtd.getAttr(1);
	    int x=Integer.parseInt(mtd.getAttr(2));
	    int y=Integer.parseInt(mtd.getAttr(3));	    
	    int width=Integer.parseInt(mtd.getAttr(4));
	    int height=Integer.parseInt(mtd.getAttr(5));
	    BufferedImage subset =  bi.getSubimage(x,y,width,height);
	    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ImageIO.write(subset, "png", byteArrayOutputStream); 
	    int[]rgb= ImageUtils.averageRGB(subset);

            if(zos!=null)  {
		String file="";
		if(Utils.stringDefined(label)) {
		    file=label+"_";
		}
		file+="box_"+cnt+".png";
		zos.putNextEntry(new ZipEntry(file));
		byte[] imageBytes = byteArrayOutputStream.toByteArray();
		IOUtil.writeTo(new ByteArrayInputStream(imageBytes), zos);
		csv.append(file+","+rgb[0]+"," +rgb[1] +"," + rgb[2]+"\n");
		continue;
	    }
	    

            // Get the byte array from the output stream
            byte[] imageBytes = byteArrayOutputStream.toByteArray();

            // Base64 encode the image byte array
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            // Construct the data URL (using "image/png" as the MIME type)
            String dataUrl = "data:image/png;base64," + base64Image;
	    if(Utils.stringDefined(label)) {
		sb.append(HU.b(label+":"));
	    } else {
		sb.append(HU.b(label+":"));
	    }
	    sb.append("<br>");
	    sb.append("Average: red: " + rgb[0] +" green: " + rgb[1] +" blue: " + rgb[2]);
	    sb.append("<br>");
	    sb.append(HU.image(dataUrl));
	    sb.append("<p>");
	}
	if(zos!=null)  {
	    zos.putNextEntry(new ZipEntry("boxes.csv"));
	    IOUtil.writeTo(new ByteArrayInputStream(csv.toString().getBytes()), zos);
	    
	    zos.close();
	    Result result = new Result();
	    result.setNeedToWrite(false);
	}
	return null;

    }
    
    @Override
    public void initTags(Hashtable<String, WikiTagHandler> tagHandlers) {
	tagHandlers.put("core_visualizer",this);
    }



    public String handleTag(WikiUtil wikiUtil, Request request,
                            Entry originalEntry, Entry entry, String theTag,
                            Hashtable props, String remainder) throws Exception {
	if(!theTag.equals("core_visualizer")) return null;

	StringBuilder sb = new StringBuilder();
	
	String mainId = HU.getUniqueId("id");
	String uid = HU.getUniqueId("id");
	String topId = HU.getUniqueId("topid");
	String leftId = HU.getUniqueId("left");	

	HU.open(sb,"div",HU.attrs("id",mainId,"style","position:relative;","class","cv-main"));
	HU.div(sb,"",HU.attrs("id",topId,"class","cv-top","style","position:relative"));
	HU.div(sb,"",HU.attrs("class","cv-canvas","id",uid));
	HU.close(sb,"div");
	HU.importJS(sb,getRepository().getHtdocsUrl("/geo/konva.min.js"));
	HU.cssLink(sb, getPageHandler().makeHtdocsUrl("/geo/corevisualizer.css"));
	HU.importJS(sb,getRepository().getHtdocsUrl("/geo/corevisualizer.js"));
	String id = "viz_" + uid;
	StringBuilder js = new StringBuilder();
	List<String> args = (List<String>)Utils.makeListFromValues("mainId",JU.quote(mainId),"topId",JU.quote(topId));
	Utils.add(args,"mainEntry",JU.quote(entry.getId()));
	if (getAccessManager().canDoEdit(request, entry)) {
	    Utils.add(args,"canEdit","true");
	}

	String ids = entry.getId();
	String other = Utils.getProperty(props,"otherEntries",null);
	if(other!=null) ids+=","+other;
	Utils.add(args,"collectionIds",JU.quote(ids));
	for(String a:new String[]{"height","canvasHeight","scale","top","autoSize",
				  "showLegend","showAnnotations",
				  "maxColumnWidth",
				  "doRotation",
				  "scaleY",
				  "axisX","legendX","legendTop","legendBottom",
				  "showLabels","showHighlight","showMenuBar","initScale"}) {
	    String v=Utils.getProperty(props,a,null);
	    if(v!=null) {
		if(v.equals("true") || v.equals("false"))
		    Utils.add(args,a,v);
		else
		    Utils.add(args,a,HU.squote(v));
	    }
	}



	List<String> annotations=new ArrayList<String>();
	String annotationsProp  =Utils.getProperty(props,"annotations",null);
	if(annotationsProp!=null)
	    annotations.add(annotationsProp);
	if(annotations.size()>0) {
	    Utils.add(args,"annotations",JU.quote(Utils.join(annotations,",")));
	}



	String legend=Utils.getProperty(props,"legendUrl",null);
	if(legend!=null) {
	    Utils.add(args,"legendUrl",JU.quote(legend));
	}


	/****
	List<Entry> children = getEntryUtil().getEntriesOfType(getWikiManager().getEntries(request, wikiUtil,
											   originalEntry, entry, props),
							       "type_borehole_registeredcoreimage");

	String json = coreApi.makeEntriesJson(request, entry,children);
	js.append("var coreVisualizerData = " + json);
	js.append(";\n");
	*/

	js.append("var container = document.getElementById('"+uid+"');\n");
	js.append("var " +id +"="+ HU.call("new RamaddaCoreVisualizer","null",
					   "container",JU.map(args)));
	js.append("\n");
	HU.script(sb,js.toString());


	return sb.toString();
    }


}
