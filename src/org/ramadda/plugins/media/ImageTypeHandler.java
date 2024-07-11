/**
Copyright (c) 2008-2024 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.media;


import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.Utils;
import org.ramadda.util.WikiUtil;

import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;

import java.util.Hashtable;
import org.ramadda.util.ImageUtils;
import java.awt.Image;

import java.io.*;
import java.util.ArrayList;
import java.util.List;


public class ImageTypeHandler extends GenericTypeHandler {

    public static final String ARG_IMAGE_RESIZE="imageresize";
    public static final String ARG_STRIP_METADATA="stripmetadata";    
    public static final String ARG_IMAGE_WIDTH="imagewidth";
    public static int IDX = 0;
    public static final int IDX_PROXY = IDX++;
    public static final int IDX_FILENAME = IDX++;
    public static final int IDX_LAST = IDX_FILENAME;

    public ImageTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }

    @Override
    public void getFileExtras(Request request, Entry entry, StringBuilder sb)
            throws Exception {
	sb.append(HU.b("Image:"));
	sb.append("<div style='margin-left:30px;'>");
        sb.append(HU.labeledCheckbox(ARG_IMAGE_RESIZE, "true", false,"Resize image"));
	sb.append(HU.space(2));
	sb.append("Width:");
	sb.append(HU.space(1));
	sb.append(HU.input(ARG_IMAGE_WIDTH,"600",HU.SIZE_5));
	sb.append("<br>");
        sb.append(HU.labeledCheckbox(ARG_STRIP_METADATA, "true", false,"Strip metadata"));
	sb.append("<br>");
	if(getRepository().getSearchManager().isImageIndexingEnabled()) {
	    sb.append(HU.labeledCheckbox(ARG_INDEX_IMAGE, "true", false,"Extract text from image"));
	}
	sb.append("</div>");



        super.getFileExtras(request, entry,sb);

    }


    @Override
    public void initializeNewEntry(Request request, Entry entry,NewType newType)
            throws Exception {
        super.initializeNewEntry(request, entry, newType);
	if(!isNew(newType)) return;
	if(!request.get(ARG_IMAGE_RESIZE,false) &&
	   !request.get(ARG_STRIP_METADATA,false)) return;
	if(!entry.getResource().isStoredFile()) return;

	String theFile = entry.getResource().getPath();
	Image image = ImageUtils.readImage(theFile);
	int width = request.get(ARG_IMAGE_WIDTH,600);
	if(request.get(ARG_IMAGE_RESIZE,false)) {
	    if (image.getWidth(null) > width) {
		image = ImageUtils.resize(image, width, -1);
		ImageUtils.waitOnImage(image);
	    }
	}
	ImageUtils.writeImageToFile(image, theFile);
	File f = new File(theFile);
	entry.getResource().setFileSize(f.length());
    }
    

    @Override
    public String getPathForEntry(Request request, Entry entry, boolean forRead)
            throws Exception {
        Resource resource = entry.getResource();
        String   path     = Utils.normalizeTemplateUrl(resource.getPath());
        boolean  useProxy = entry.getBooleanValue(request,0, false);
        if (useProxy) {
            String filename = entry.getStringValue(request,1, (String) null);
            String tail     = IOUtil.getFileTail(path);
            if (Utils.stringDefined(filename)) {
                tail = filename;
            }
            path = getRepository().getUrlBase() + "/proxy/" + tail
                   + "?entryid=" + entry.getId();
        }

        return path;
    }


    @Override
    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props)
            throws Exception {

        if (tag.equals("360image")) {
            StringBuilder sb = new StringBuilder();
            sb.append("\n+skip\n");
	    if(request.getExtraProperty("aframejs")==null) {
		request.putExtraProperty("aframejs", "true");
		sb.append(HU.importJS(getHtdocsUrl("/lib/aframe/aframe-master.js")));
		sb.append(HU.importJS(getHtdocsUrl("/media/ramadda_aframe.js")));
	    }
            String imgUrl =
                entry.getTypeHandler().getEntryResourceUrl(request, entry);
            String width  = Utils.getProperty(props, "width", "600px");
            String height = Utils.getProperty(props, "height", "200px");
	    String sceneId = HU.getUniqueId("scene");
	    String loadingId = HU.getUniqueId("loading");
	    String skyId = HU.getUniqueId("sky");	    	    
	    String cameraId = HU.getUniqueId("camera");
            sb.append("\n");
	    String css = "a-scene {height: " + height
                                          + ";width:" + width + ";}\n" +

		".aframe-progress {z-index: 999;position: absolute; width: 100%;text-align: center;top: 50%;transform: translateY(-50%);color: #000000;font-size: 175%;font-family: Arial, sans-serif;}\n";

            sb.append(HU.importCss(css));
	    List<String> args = new ArrayList<String>();

	    sb.append("<div style='position:relative;'>");
	    sb.append(HU.div("Loading...",HU.attrs("class","aframe-progress","id",loadingId)));
            sb.append("\n<a-scene embedded id='"  +sceneId+"'>\n");
	    Utils.add(args,"loadingId",JU.quote(loadingId));

            List<Metadata> metadataList =
                getMetadataManager().findMetadata(request, entry, "3d_label",
                    true);
            if ((metadataList != null) && (metadataList.size() > 0)) {
                for (Metadata metadata : metadataList) {
                    sb.append("<a-text ");
		    sb.append(HU.attrs("value", metadata.getAttr1(),
				       "width", metadata.getAttr2(),
				       "position" ,metadata.getAttr3(),
				       "rotation" ,metadata.getAttr4(),
				       "look-at","#" + cameraId));
		    sb.append("></a-text>\n");
                }
            }
	    String initX="0";
	    String initY = "0";
            String zoom = entry.getStringValue(request,"zoom","");
	    if(stringDefined(zoom)) {
		Utils.add(args,"zoom",zoom);
	    }

            String rotx = entry.getStringValue(request,"rotationx","");
	    if(stringDefined(rotx)) {
		Utils.add(args,"rotateX",rotx);
		initX=rotx;
	    }
            String roty = entry.getStringValue(request,"rotationy","");
	    if(stringDefined(roty)) {
		Utils.add(args,"rotateY",roty);
		initY=roty;
	    }
	    Utils.add(args,"skyId",JU.quote(skyId));
	    sb.append("<a-sky ");
	    sb.append(HU.attrs("src",imgUrl,"id",skyId));
	    sb.append(" ></a-sky>\n ");
	    sb.append("<a-entity camera id=\"" +cameraId +"\"  mouse-drag-rotate   position=\"0 1.6 0\" rotation='" + initX +" " + initY +"  " + " 0' ></a-entity>\n");
            sb.append("</a-scene>\n ");
	    sb.append("</div>");

            sb.append("\n-skip\n");
	    sb.append(HU.script(HU.call("RamaddaAframe.init",HU.quote(sceneId),HU.quote(cameraId),JU.map(args))));
            return sb.toString();
        }

        return super.getWikiInclude(wikiUtil, request, originalEntry, entry,
                                    tag, props);

    }
}
