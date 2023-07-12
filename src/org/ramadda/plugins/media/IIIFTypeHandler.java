/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.media;


import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.WikiUtil;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.Utils;
import org.ramadda.util.IO;

import org.json.*;

import org.w3c.dom.*;



import java.net.URL;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.Hashtable;
import java.util.ArrayList;
import java.util.List;



/**
 *
 *
 */
public class IIIFTypeHandler extends ExtensibleGroupTypeHandler {


    /** _more_ */
    private static int IDX = 0;

    /** _more_ */
    public static final int IDX_STYLE = IDX++;

    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public IIIFTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }


    /**
     * _more_
     *
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
	String url = getPathForEntry(request, entry,true);
	if(!stringDefined(url)) return;
	IO.Result result = IO.doGetResult(new URL(url));
	if(result.getError()) {
	    String error = result.getResult();
	    getLogManager().logError("IIIF: Error reading url:" + url+" error:" +error);
	    return;
	}
	String json = result.getResult();
	JSONObject root           = new JSONObject(json);
	String label = root.optString("label");
	if(stringDefined(label) && !stringDefined(entry.getName())) entry.setName(label);
	String description = root.optString("description");
	if(stringDefined(description) && !stringDefined(entry.getDescription())) entry.setDescription("+note\n"+description+"\n-note\n");	
	try {
	    String thumbnail   = JU.readValue(root,"thumbnail.@id",null);
	    if(thumbnail!=null) {
		getRepository().getMetadataManager().addThumbnailUrl(request, entry,thumbnail,Utils.getFileTail(thumbnail))
;
		System.err.println(thumbnail);

	    }
	} catch(Exception exc) {
	    getLogManager().logError("IIIF error reading thumbnail:" + url,exc);
	}

	JSONArray topMetadata   = root.optJSONArray("metadata");
	if(topMetadata!=null)  {
	    List<JSONArray> metadataList = new ArrayList<JSONArray>();
	    metadataList.add(topMetadata);
	    IIIFImportHandler.addMetadata(getRepository(), request,entry,metadataList);
	}


    }

    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props)
            throws Exception {
        if ( !tag.equals("iiif")) {
            return super.getWikiInclude(wikiUtil, request, originalEntry,
                                        entry, tag, props);
        }

        StringBuilder sb = new StringBuilder();
	String uid = HU.getUniqueId("iiif_");
	sb.append("<!-- begin IIIF Mirador embed -->\n");
	HU.div(sb,"",HU.attrs("id",uid,"style",HU.css("position","relative",
						      "width",Utils.getProperty(props,"width","1000px"),


						      "height",Utils.getProperty(props,"height","600px"))));
	sb.append("\n");
	sb.append(HU.importJS("https://unpkg.com/mirador@latest/dist/mirador.min.js"));
	sb.append("\n");
	String url = getPathForEntry(request, entry,true);
	StringBuilder js = new StringBuilder();
	List<String> attrs = new ArrayList<String>();
	Utils.add(attrs,"id",JU.quote(uid));
	Utils.add(attrs,"manifests",JU.map(url, JU.map("view",JU.quote("gallery"))));
	Utils.add(attrs,"windows",JU.list(JU.map("loadedManifest",
						 JU.quote(url),
						 "view",JU.quote(Utils.getProperty(props,"view","single")),
						 "canvasIndex","2",
						 "thumbnailNavigationPosition",
						 JU.quote(Utils.getProperty(props,"thumbnailPosition","far-right")))));
	Utils.add(attrs,"workspaceControlPanel",JU.map("enabled",Utils.getProperty(props,"workspaceEnabled","false")));

	js.append("var mirador = Mirador.viewer(" + JU.map(attrs)+");\n");
	HU.script(sb,js.toString());
	sb.append("\n");
        return sb.toString();
    }




}
