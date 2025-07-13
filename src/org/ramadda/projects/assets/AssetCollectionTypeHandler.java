/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.projects.assets;

import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;
import org.ramadda.repository.output.*;
import org.ramadda.util.WikiUtil;
import org.ramadda.util.NamedBuffer;
import org.ramadda.util.TTLCache;
import org.ramadda.util.IO;
import org.ramadda.util.Utils;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.seesv.Seesv;

import ucar.unidata.util.StringUtil;


import org.w3c.dom.*;
import org.json.*;
import java.net.URL;
import java.io.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.List;


public class AssetCollectionTypeHandler extends ExtensibleGroupTypeHandler  {



    public AssetCollectionTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }


    private void wikify(Request request, Entry entry, StringBuilder sb, String wiki) throws Exception {
	sb.append(getWikiManager().wikifyEntry(request, entry,wiki));
    }

    private String getInfo(Request request, Entry entry) {
	return "asset:" + entry;
    }



    private String getTypeName(Request request,Entry entry) {
	if(entry.getTypeHandler().isType("type_archive_collection")) return "Collection";
	if(entry.getTypeHandler().isType("type_archive_series")) return "Series";
	if(entry.getTypeHandler().isType("type_archive_file")) return "File";
	if(entry.getTypeHandler().isType("type_archive_item")) return "Item";
	return "";
    }

    private void addThumbnails(Request request, StringBuilder sb,Entry entry) throws Exception {
	List<String[]> thumbs = new ArrayList<String[]>();
        getMetadataManager().getFullThumbnailUrls(request, entry, thumbs,false);
	if(thumbs.size()>0) {
	    sb.append("<center>");
	    for(String[] tuple: thumbs) {
		String image=HU.img(tuple[0],"",
				    HU.attrs("width","150px"));
		if(stringDefined(tuple[1])) {
		    image =HU.span(HU.div(image,"")+HU.div(tuple[1],HU.attrs("class","caption")),"");
		}
		sb.append(HU.div(image,HU.style("padding-right:20px;display:table-cell;")));
	    }
	    sb.append("</center>");
	}

    }

    private static String propWiki =
	"{{properties  propertyToggleLimit=100 message=\"\"  metadata.types=\"!archive_internal,!content.alias,!content.attachment,!content.thumbnail,!content.license\" checkTextLength=\"true\" headingClass=\"formgroupheader\" layout=\"linear\"  includeTitle=\"true\"  separator=\"\"  decorate=\"false\" inherited=\"false\"  }}";



    @Override
    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props)
            throws Exception {
        if (tag.equals("assets_report_link")) {
	    String url =
		request.entryUrl(getRepository().URL_ENTRY_SHOW, entry,
				 ARG_OUTPUT,"assets_report");
	    return HU.div(HU.href(url,"Generate Report"),HU.attrs("class","ramadda-button","style","margin-bottom:6px;"));
	}
        if ( !tag.equals("assets_report")) {
            return super.getWikiInclude(wikiUtil, request, originalEntry,
                                        entry, tag, props);
	}
	StringBuilder sb = new StringBuilder();
	//title="{{name}} Finding Aid"
	Entry section = getEntryManager().getAncestor(request,  entry, "type_archive_section");
	String titleWiki = ":title Assets Report  - {{name}}";
	if(section!=null) {
	    wikify(request, section,sb,titleWiki);
	}
	sb.append(HU.cssBlock(".metadata-block {max-height:1000px;}\n.archive-findingaid-header {border-top-left-radius: var(--default-radius);border-top-right-radius: var(--default-radius);padding:4px;padding-left:12px;font-weight:bold;font-size:110%;background:var(--header-background);}\n.archive-findingaid-block {margin-top:1em;margin-left:40px;}\n"));
	wikify(request, entry,sb,titleWiki);
	Date startDate =DateHandler.checkDate(new Date(entry.getStartDate()));
	Date endDate =DateHandler.checkDate(new Date(entry.getEndDate()));	
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	Date now = new Date();
	sb.append(HU.center(sdf.format(now)));
	sb.append("<center>");
	HU.script(sb,"HtmlUtils.initPageSearch('.ramadda-entry',null,'Search in page')");
	sb.append("</center>");
	sb.append("<div class=assets-entry>");
	wikify(request, entry,sb,"----");
	if(stringDefined(entry.getDescription())) {
	    wikify(request, entry,sb,entry.getDescription());
	}
	sb.append("</div>");
	sb.append("<div class=assets-block>\n");
	String contentsWiki = "{{table display=list showBreadcrumbs=false max=5000}}";
	wikify(request, entry,sb,contentsWiki);
	//	makeReport(request, entry, sb);
	sb.append("</div>\n");

	return sb.toString();
    }

    private void makeReport(Request request, Entry entry, StringBuilder buff) throws Exception {
	 List<Entry> entries = getEntryManager().getChildren(request, entry);
	 if(entries.size()==0) return;
	 List<NamedBuffer> contents = new ArrayList<NamedBuffer>();
	 Hashtable<String,NamedBuffer> map = new Hashtable<String,NamedBuffer>();
	 for(Entry child: entries) {
	     if(!child.getTypeHandler().isType("type_assets_base")) {
		 continue;
	     }
	     NamedBuffer sb = map.get(child.getTypeHandler().getType());
	     if(sb == null) {
		 sb = new NamedBuffer(child.getTypeHandler().getLabel());
		 map.put(child.getTypeHandler().getType(),sb);
		 contents.add(sb);
	     }

	     sb.append("<div class=assets-entry>");
	     String label = getTypeName(request, child) +": "+child.getName();
	     label = HU.href(getEntryManager().getEntryUrl(request, child),label);
	     HU.div(sb,label,HU.attrs("class","assets-report-header"));
	     sb.append(getInfo(request, child));
	     wikify(request, child,sb.getBuffer(),propWiki);
	     sb.append("</div>\n");
	 }
	 for(NamedBuffer nb:contents) {
	     buff.append(HU.div(HU.b(nb.getName())));
	     buff.append(nb.getBuffer());
	 }
    }


}
