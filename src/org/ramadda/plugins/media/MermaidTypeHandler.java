/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.media;


import org.json.*;

import org.ramadda.data.docs.*;

import org.ramadda.data.point.text.*;
import org.ramadda.data.record.*;

import org.ramadda.data.services.PointTypeHandler;
import org.ramadda.data.services.RecordTypeHandler;

import org.ramadda.repository.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;

import org.ramadda.util.JsonUtil;
import org.ramadda.util.Utils;
import org.ramadda.util.WikiUtil;
import org.ramadda.util.seesv.Seesv;

import org.w3c.dom.Element;

import java.io.*;

import java.net.URL;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;


/**
 */
@SuppressWarnings("unchecked")
public class MermaidTypeHandler extends ConvertibleTypeHandler {

    public MermaidTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }


    @Override
    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props)
            throws Exception {
        if ( !tag.equals("mermaid")) {
	    return super.getWikiInclude(wikiUtil, request, originalEntry, entry, tag, props);
        }

        StringBuilder sb = new StringBuilder();
	if (request.getExtraProperty("addedmermaid") == null) {
	    request.putExtraProperty("addedmermaid", "true");
	    sb.append("<script type='module'>\nimport mermaid from 'https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.esm.min.mjs';\nmermaid.initialize({ startOnLoad: true });\n</script>\n");
	}
        //          String width  = entry.getValue(request,IDX_WIDTH, "320");
        //          String height = entry.getValue(request,IDX_HEIGHT, "256");

	String text;

	if(entry.getResource().isFile()) {
	    text = getStorageManager().readSystemResource(entry.getFile());
	} else {
	     text  = entry.getStringValue(request, "mermaid_text","");
	}
	
	sb.append(HU.pre(text,HU.cssClass("mermaid preplain")));
        return sb.toString();
    }




}
