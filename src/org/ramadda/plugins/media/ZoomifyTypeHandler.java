/**
   Copyright (c) 2008-2023 Geode Systems LLC
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
    public void initializeNewEntry(Request request, Entry entry,NewType newType)
	throws Exception {
        super.initializeNewEntry(request, entry, newType);
        if ( !entry.isFile()) {
            return;
        }
        String slicer = getRepository().getScriptPath("ramadda.image.slicer");
        if (slicer == null) {
            return;
        }
	getRepository().addScriptPath("sh");
        File entryDir  = getStorageManager().getEntryDir(entry.getId(), true);
        File imagesDir = new File(entryDir, "images");
        imagesDir.mkdir();
        List<String> commands = new ArrayList<String>();
        Utils.add(commands, "sh", slicer, "-i",
                  entry.getResource().getPath(), "-o", imagesDir.toString());
        ProcessBuilder pb = getRepository().makeProcessBuilder(commands);
        pb.redirectErrorStream(true);
	getLogManager().logSpecial("Zoomify: creating image tiles for:" + entry.getName());
        Process     process = pb.start();
        InputStream is      = process.getInputStream();
	getLogManager().logSpecial("Zoomify: done creating image tiles for:" + entry.getName());
	byte[] bytes = IO.readBytes(is,100000);
        String      result  = new String(bytes);
        if (result.indexOf("unable to open image")<0 && result.trim().length() > 0) {
            throw new IllegalArgumentException("Error running image slicer:"
					       + result);
        }
    }


 

    
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
	String id = getWikiManager().makeZoomifyLayout(request, entry,sb,props);
	List<String> jsonProps =  getWikiManager().getZoomifyProperties(request, entry,props);	
        Utils.add(jsonProps, "id", JsonUtil.quote(id));
	HU.script(sb, "new RamaddaZoomableImage(" + HU.comma(JsonUtil.map(jsonProps),HU.quote(id))+");\n");
        return sb.toString();
    }


    @Override
    public void getWikiTags(List<String[]> tags, Entry entry) {
	tags.add(new String[]{"zoomify_collection","zoomify_collection"});
    }

    @Override
    public void initTags(Hashtable<String, WikiTagHandler> tagHandlers) {
	tagHandlers.put("zoomify_collection",this);
    }

    @Override
    public void addTagDefinition(List<String>  tags) {
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
	getWikiManager().initZoomifyImports(request,sb);
	String id = getWikiManager().makeZoomifyLayout(request, children.get(0),sb,props);
	List<String> jsonProps =  getWikiManager().getZoomifyProperties(request, children.get(0),props);	
        Utils.add(jsonProps, "id", JsonUtil.quote(id));
	HU.script(sb, "new RamaddaZoomify(" + HU.comma(JsonUtil.map(jsonProps),HU.quote(id))+");\n");
        return sb.toString();
    }


}
