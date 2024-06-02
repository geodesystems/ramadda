/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.archive;


import org.ramadda.repository.*;
import org.ramadda.repository.database.Tables;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;

import org.ramadda.util.WikiUtil;

import org.w3c.dom.*;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;



import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.List;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;



@SuppressWarnings("unchecked")
public class ArchiveTypeHandler extends ExtensibleGroupTypeHandler {

    public ArchiveTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }

    private void wikify(Request request, Entry entry, StringBuilder sb, String wiki) throws Exception {
	sb.append(getWikiManager().wikifyEntry(request, entry,wiki));
    }

    private String getTypeName(Request request,Entry entry) {
	if(entry.getTypeHandler().isType("type_archive_collection")) return "Collection";
	if(entry.getTypeHandler().isType("type_archive_series")) return "Series";
	if(entry.getTypeHandler().isType("type_archive_file")) return "File";
	if(entry.getTypeHandler().isType("type_archive_item")) return "Item";
	return "";
    }

    private String getArchiveNumber(Request request,Entry entry) {
	String num;
	String name = getTypeName(request, entry);
	num = (String) entry.getValue("collection_number");
	if(stringDefined(num)) {
	    return HU.b(name+" Number: ")+ num;
	}
	num = (String) entry.getValue("series_number");
	if(stringDefined(num)) {
	    return HU.b(name+" Number: ")+ num;
	}	
	num = (String) entry.getValue("file_number");
	if(stringDefined(num)) {
	    return HU.b(name+" Number: ")+ num;
	}
	num = (String) entry.getValue("item_number");
	if(stringDefined(num)) {
	    return HU.b(name+" Number: ")+ num;
	}
	return null;
    }	

    private String getInfo(Request request, Entry entry) {
	String info="";
	String num = getArchiveNumber(request,entry);
	if(num!=null) info+=num;
	String location = (String) entry.getValue("location");
	if(stringDefined(location)) info+=HU.space(2) +HU.b("Location: ") +location;
	Object size= entry.getValue("size");
	if(size!=null) {
	    double s = (Double)size;
	    if(!Double.isNaN(s) && s!=0) {
		DecimalFormat df = new DecimalFormat("#0.0");
		info+=HU.space(2) +HU.b("Size: ") +df.format(s) +" linear feet";
	    }
	}
	return info;
    }
    private static String propWiki = "{{properties  propertyToggleLimit=100 message=\"\"  metadata.types=\"!archive_internal,!content.alias,!content.thumbnail,!content.license\" checkTextLength=\"false\" headingClass=\"formgroupheader\" layout=\"linear\"  includeTitle=\"true\"  separator=\"\"  decorate=\"false\" inherited=\"false\"  }}";

    @Override
    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props)
            throws Exception {
        if (tag.equals("archive_finding_aid_link")) {
	    String url =
		request.entryUrl(getRepository().URL_ENTRY_SHOW, entry,
				 ARG_OUTPUT,"archive_finding_aid");
	    return HU.div(HU.href(url,"Finding Aid"),HU.cssClass("ramadda-button"));
	}
        if ( !tag.equals("archive_finding_aid")) {
            return super.getWikiInclude(wikiUtil, request, originalEntry,
                                        entry, tag, props);
	}
	StringBuilder sb = new StringBuilder();
	//title="{{name}} Finding Aid"
	Entry section = getEntryManager().getAncestor(request,  entry, "type_archive_section");
	String titleWiki = ":title {{name}}";
	if(section!=null) {
	    wikify(request, section,sb,titleWiki);
	}
	sb.append(HU.cssBlock(".metadata-block {max-height:1000px;}\n.archive-findingaid-header {padding:4px;padding-left:12px;font-weight:bold;font-size:110%;background:var(--header-background);}\n.archive-findingaid-block {margin-top:1em;margin-left:40px;}\n"));
	sb.append(HU.center(HU.italics(HU.div("Finding Aid",HU.cssClass("ramadda-page-title")))));
	String name = getTypeName(request, entry);
	if(name!=null) titleWiki=":title " + name +": " + entry.getName()+"";
	wikify(request, entry,sb,titleWiki);
	Date startDate =DateHandler.checkDate(new Date(entry.getStartDate()));
	Date endDate =DateHandler.checkDate(new Date(entry.getEndDate()));	
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
	if(startDate!=null  && endDate!=null) {
	    sb.append(HU.center("\\[ca. " + sdf.format(startDate) +" - " +sdf.format(endDate)+"]"));
	} else 	if(startDate!=null) {
	    sb.append(HU.center("\\[ca. " + sdf.format(startDate)+"]"));
	} else 	if(endDate!=null) {
	    sb.append(HU.center("\\[ca. " + sdf.format(endDate)+"]"));		      
	}
	String info = getInfo(request, entry);
	sb.append(HU.center(info));	
	wikify(request, entry,sb,"----");
	if(stringDefined(entry.getDescription())) {
	    wikify(request, entry,sb,entry.getDescription());
	}
	wikify(request, entry,sb,propWiki);

	wikify(request, entry,sb,"----");

	sb.append("<div class=archive-findingaid-block>\n");
	makeFindingAid(request, entry, sb);
	sb.append("</div>\n");



	return sb.toString();
    }


    private void makeFindingAid(Request request, Entry entry, StringBuilder sb) throws Exception {
	 List<Entry> entries = getEntryManager().getChildren(request, entry);
	 if(entries.size()==0) return;
	 for(Entry child: entries) {
	     if(!child.getTypeHandler().isType("type_archive_root")) continue;
	     HU.div(sb,getTypeName(request, child) +": "+child.getName(),HU.attrs("class","archive-findingaid-header"));
	     sb.append(getInfo(request, child));
	     wikify(request, child,sb,propWiki);
	     sb.append("<div class=archive-findingaid-block>\n");
	     makeFindingAid(request, child, sb);
	     sb.append("</div>\n");


	 }
    }
}
