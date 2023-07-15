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
public class IIIFCollectionTypeHandler extends TypeHandler {


    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public IIIFCollectionTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository,entryNode);
    }



    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props)
            throws Exception {
        if ( !tag.equals("iiif_collection")) {
            return super.getWikiInclude(wikiUtil, request, originalEntry,
                                        entry, tag, props);
        }

	List<Entry> children = getEntryManager().getChildren(request, entry);
	if(children.size()==0) return "No IIIF Documents";
	return IIIFTypeHandler.getIIIFDisplay(request, children.get(0), props,children);
    }

    public static String getIIIFDisplay(Request request, Entry entry, Hashtable props,List<Entry> catalog)
            throws Exception {
        StringBuilder sb = new StringBuilder();
	String uid = HU.getUniqueId("iiif_");
	sb.append("<!-- begin IIIF Mirador embed -->\n");
	sb.append(HU.importCss(".MuiTypography-h4,.MuiTypography-body1, .mirador48, .mirador18, .MuiSvgIcon-root {font-size: var(--font-size)}"));
	HU.div(sb,"",HU.attrs("id",uid,"style",HU.css("position","relative",
						      "width",Utils.getProperty(props,"width","1000px"),


						      "height",Utils.getProperty(props,"height","600px"))));
	sb.append("\n");
	sb.append(HU.importJS("https://unpkg.com/mirador@latest/dist/mirador.min.js"));
	sb.append("\n");
	String url = entry.getTypeHandler().getPathForEntry(request, entry,true);
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
	//Hide the workspace for single IIIF entry
	if(catalog==null) {
	    Utils.add(attrs,"workspaceControlPanel",JU.map("enabled",Utils.getProperty(props,"workspaceEnabled","false")));
	}
	if(catalog!=null && catalog.size()>0) {
	    List<String> l = new ArrayList<String>();
	    for(Entry e:catalog) {
		if(e.getTypeHandler().isType("type_iiif_document")) {
		    l.add(JU.map("manifestId",JU.quote(e.getTypeHandler().getPathForEntry(request, e,true))));
		}
	    }
	    if(l.size()>0) {
		Utils.add(attrs,"catalog",JU.list(l));
	    }
	}
	js.append("var mirador = Mirador.viewer(" + JU.map(attrs)+");\n");
	HU.script(sb,js.toString());
	sb.append("\n");
        return sb.toString();
    }





}
