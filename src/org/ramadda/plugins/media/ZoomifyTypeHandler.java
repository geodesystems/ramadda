/**
   Copyright (c) 2008-2022 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.media;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;


import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JsonUtil;


import org.ramadda.util.IO;
import org.ramadda.util.Utils;
import org.ramadda.util.WikiUtil;

import org.w3c.dom.*;


import java.io.*;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;


/**
 *
 *
 */
public class ZoomifyTypeHandler extends GenericTypeHandler implements WikiTagHandler {

    private static final String OSD_PATH = "/lib/openseadragon-bin-3.0.0";
    private static final String ANN_PATH = "/lib/annotorius";

    /**  */
    private static int IDX = 0;

    /**  */
    private static final int IDX_IMAGE_WIDTH = IDX++;

    /**  */
    private static final int IDX_IMAGE_HEIGHT = IDX++;

    /**  */
    private static final int IDX_TILES_URL = IDX++;

    /**  */
    private static final int IDX_STYLE = IDX++;
    private static final int IDX_ANNOTATIONS = IDX++;    

    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public ZoomifyTypeHandler(Repository repository, Element entryNode)
	throws Exception {
        super(repository, entryNode);
    }



    /**
     *
     * @param request _more_
     * @param entry _more_
     * @param fromImport _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void initializeNewEntry(Request request, Entry entry,
                                   boolean fromImport)
	throws Exception {
        super.initializeNewEntry(request, entry, fromImport);
        if ( !entry.isFile()) {
            return;
        }
        String slicer = getRepository().getProperty("ramadda.image.slicer");
        if (slicer == null) {
            return;
        }
        File entryDir  = getStorageManager().getEntryDir(entry.getId(), true);
        File imagesDir = new File(entryDir, "images");
        imagesDir.mkdir();
        List<String> commands = new ArrayList<String>();
        Utils.add(commands, "sh", slicer, "-i",
                  entry.getResource().getPath(), "-o", imagesDir.toString());
	System.err.println(commands);
        ProcessBuilder pb = new ProcessBuilder(commands);
        pb.redirectErrorStream(true);
        Process     process = pb.start();
        InputStream is      = process.getInputStream();
	byte[] bytes = IO.readBytes(is,100000);
        String      result  = new String(bytes);
        if (result.indexOf("unable to open image")<0 && result.trim().length() > 0) {
            throw new IllegalArgumentException("Error running image slicer:"
					       + result);
        }
    }

    private String htdocs(String path) {
	return getPageHandler().makeHtdocsUrl(path);
    }



    private void initImports(Request request, StringBuilder sb) throws Exception {
        if (request.getExtraProperty("seadragon_added") == null) {
            HU.importJS(sb,htdocs(OSD_PATH+"/openseadragon.min.js"));
            HU.importJS(sb,htdocs(OSD_PATH+"/openseadragon-bookmark-url.js"));
	    HU.cssLink(sb, htdocs(ANN_PATH+"/annotorious.min.css"));
	    HU.importJS(sb,htdocs(ANN_PATH+"/openseadragon-annotorious.min.js"));
	    HU.importJS(sb,htdocs(ANN_PATH+"/annotorious-toolbar.min.js"));
	    HU.cssLink(sb,htdocs("/media/annotation.css"));
            HU.importJS(sb,htdocs("/media/annotation.js"));	    	    
            request.putExtraProperty("seadragon_added", "true");
        }
    }	


    private List<String> getProperties(Request request, Entry entry,Hashtable props) throws Exception {
	List<String> jsonProps = new ArrayList<String>();
        List<String> tiles     = new ArrayList<String>();
        Utils.add(jsonProps,  "showNavigator",
                  "true", "maxZoomLevel", "18", "prefixUrl",
                  JsonUtil.quote(getRepository().getUrlBase()
                                 + OSD_PATH+"/images/"));
        Utils.add(jsonProps, "showRotationControl", "true",
                  "gestureSettingsTouch",
                  JsonUtil.map(Utils.add(null, "pinchRotate", "true")));

        //If its a file then we did the tiling ourselves
        if (entry.isFile()) {
            Utils.add(jsonProps, "tileSources",
                      JsonUtil.quote(getRepository().getUrlBase()
                                     + "/entryfile/" + entry.getId()
                                     + "/images.dzi"));
        } else if (Utils.stringDefined("" + entry.getValue(IDX_TILES_URL))) {
	    String        width  = Utils.getProperty(props, "width", "800px");
	    String        height = Utils.getProperty(props, "height", "600px");
            if (entry.getValue(IDX_IMAGE_WIDTH, 0) != 0) {
                width = "" + entry.getValue(IDX_IMAGE_WIDTH, 0);
            }
            if (entry.getValue(IDX_IMAGE_HEIGHT, 0) != 0) {
                height = "" + entry.getValue(IDX_IMAGE_HEIGHT, 0);
            }
            Utils.add(tiles, "type", JsonUtil.quote("zoomifytileservice"),
                      "tilesUrl", JsonUtil.quote(entry.getValue(2)));
            Utils.add(tiles, "width", width, "height", height);
            Utils.add(jsonProps, "tileSources", JsonUtil.map(tiles));
        } else {
            throw new IllegalArgumentException(
					       "No image tile source defined");
        }
        String        doBookmark  = Utils.getProperty(props, "doBookmark", "false");
	Utils.add(jsonProps, "doBookmark", JsonUtil.quoteType(doBookmark));

        String annotations = (String) entry.getValue(IDX_ANNOTATIONS);
	if(!Utils.stringDefined(annotations)) {
	    annotations = "[]";
	}
	Utils.add(jsonProps, "annotations", annotations);
	Utils.add(jsonProps,"canEdit",""+ getAccessManager().canDoEdit(request, entry));
	String authToken = request.getAuthToken();	
	Utils.add(jsonProps,"authToken",HU.quote(authToken));
	Utils.add(jsonProps,"entryId",HU.quote(entry.getId()));
	Utils.add(jsonProps,"name",HU.quote(entry.getName()));	
        return  jsonProps;
    }

    private String makeLayout(Request request, Entry entry,StringBuilder sb,Hashtable props) throws Exception {
	initImports(request,sb);
        String        width  = Utils.getProperty(props, "width", "800px");
        String        height = Utils.getProperty(props, "height", "600px");
        String mainStyle = HU.css("width", HU.makeDim(width, null), "height",
				  HU.makeDim(height, null),
				  "padding","2px");
        String style = HU.css("width", HU.makeDim(width, null),
			      //			      "border", "1px solid #aaa", 
			      "color", "#333",
                              "background-color", "#fff");

        String s = (String) entry.getValue(IDX_STYLE);
        if (Utils.stringDefined(s)) {
            style += s;
        }
	style += Utils.getProperty(props, "style","");
        style = style.replaceAll("\n", " ");
	HU.open(sb,"center");
	HU.open(sb,"div",HU.attrs("style",HU.css("text-align","left","display","inline-block","width",width)));
        String id = HU.getUniqueId("zoomify_div");
	String main = HU.div("",HU.attrs("style",mainStyle,"id", id));
	String top = HU.div("", HU.attrs("id", id+"_top"));
	String bar = HU.div("", HU.attrs("id", id+"_annotations"));
        sb.append(HU.div(top +
			 HU.div(bar+main,HU.attrs("class","ramadda-annotation-wrapper","style", style)),""));
        sb.append("\n</div>\n");
	HU.close(sb,"center");
	return id;
    }
    
    /**
     * _more_
     *
     * @param wikiUtil _more_
     * @param request _more_
     * @param originalEntry _more_
     * @param entry _more_
     * @param tag _more_
     * @param props _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props)
	throws Exception {

        if ( !tag.equals("zoomify") && !tag.equals("zoomable")) {
            return super.getWikiInclude(wikiUtil, request, originalEntry,
                                        entry, tag, props);
        }
        StringBuilder sb     = new StringBuilder();
	String id = makeLayout(request, entry,sb,props);
	List<String> jsonProps =  getProperties(request, entry,props);	
        Utils.add(jsonProps, "id", JsonUtil.quote(id));
	HU.script(sb, "new RamaddaZoomableImage(" + HU.comma(JsonUtil.map(jsonProps),HU.quote(id))+");\n");
        return sb.toString();
    }


    public void getWikiTags(List<String[]> tags, Entry entry) {
	tags.add(new String[]{"zoomify_collection","zoomify_collection"});
    }

    public void initTags(Hashtable<String, WikiTagHandler> tagHandlers) {
	tagHandlers.put("zoomify_collection",this);
    }

    public String handleTag(WikiUtil wikiUtil, Request request,
                            Entry originalEntry, Entry entry, String theTag,
                            Hashtable props, String remainder) throws Exception {
	List<Entry> children = getEntryUtil().getEntriesOfType(getWikiManager().getEntries(request, wikiUtil,
											   originalEntry, entry, props),
							       getType());

	if(children.size()==0) {
	    return "No zoomable images";
	}
        StringBuilder sb     = new StringBuilder();
	String id = makeLayout(request, children.get(0),sb,props);
	List<String> jsonProps =  getProperties(request, children.get(0),props);	
        Utils.add(jsonProps, "id", JsonUtil.quote(id));
	HU.script(sb, "new RamaddaZoomify(" + HU.comma(JsonUtil.map(jsonProps),HU.quote(id))+");\n");
        return sb.toString();
    }


}
