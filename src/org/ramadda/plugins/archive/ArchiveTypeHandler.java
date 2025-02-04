/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.archive;



import org.ramadda.repository.*;
import org.ramadda.repository.database.DatabaseManager;
import org.ramadda.repository.database.Tables;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;

import org.ramadda.util.WikiUtil;
import ucar.unidata.util.StringUtil;
import org.ramadda.util.sql.Clause;
import org.w3c.dom.*;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;



import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.List;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;


@SuppressWarnings("unchecked")
public class ArchiveTypeHandler extends ExtensibleGroupTypeHandler {

    public ArchiveTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }

    @Override
    public void initializeNewEntry(Request request, Entry entry,NewType newType)
            throws Exception {
	if(!isNew(newType)) return;
	String field = getNumberField(request, entry);
	String num = (String) entry.getValue(request,field);
	if(stringDefined(num)) return;
	Entry parent = entry.getParentEntry();
	List<String> values= new ArrayList<String>();
	HashSet<String> seen = new HashSet<String>();
	for(Entry child: getEntryManager().getChildren(request,parent)) {
	    if(child.getTypeHandler().equals(entry.getTypeHandler())) {
		String value = (String) child.getValue(request,field);
		if(value==null) continue;
		if(!seen.contains(value)) {
		    seen.add(value);
		    values.add(value);
		}
	    }
	}
	//	List<String> values = getDatabaseManager().selectDistinct(entry.getTypeHandler().getTableName(), field,clause);

	Collections.sort(values);
	//Look for the 001,002, etc
	String last=null;
	//Find the largest 00N
	for(String v: values) {
	    v = v.trim();
	    if(v.matches("^\\d\\d\\d$")) {
		last =v;
	    }
	}
	if(last==null) last= "000";
	if(last!=null) {
	    last=last.replace("^0+","");
	    int i = last.length()==0?0:Integer.parseInt(last);
	    i++;
	    last = StringUtil.padLeft(""+i,3,"0");
	    entry.setValue(field,last);
	}


    }


    private void wikify(Request request, Entry entry, StringBuilder sb, String wiki) throws Exception {
	sb.append(getWikiManager().wikifyEntry(request, entry,wiki));
    }


    private String getNumberField(Request request,Entry entry) {
	if(entry.getTypeHandler().isType("type_archive_collection")) return "collection_number";
	if(entry.getTypeHandler().isType("type_archive_series")) return "series_number";
	if(entry.getTypeHandler().isType("type_archive_file")) return "file_number";
	if(entry.getTypeHandler().isType("type_archive_item")) return "item_number";
	return "";
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
	num = (String) entry.getValue(request,getNumberField(request, entry));
	if(stringDefined(num)) {
	    return HU.b(name+" Number: ")+ num;
	}
	return null;
    }	

    private String getInfo(Request request, Entry entry) {
	String info="";
	String num = getArchiveNumber(request,entry);
	if(num!=null) info+=num;
	String location = (String) entry.getValue(request,"location");
	if(stringDefined(location)) info+=HU.space(2) +HU.b("Location: ") +location;
	Object size= entry.getValue(request,"size");
	if(size!=null) {
	    double s = (Double)size;
	    if(!Double.isNaN(s) && s!=0) {
		DecimalFormat df = new DecimalFormat("#0.0");
		info+=HU.space(2) +HU.b("Size: ") +df.format(s) +" linear feet";
	    }
	}
	return info;
    }
    private static String propWiki = "{{properties  propertyToggleLimit=100 message=\"\"  metadata.types=\"!archive_internal,!content.alias,!content.attachment,!content.thumbnail,!content.license\" checkTextLength=\"true\" headingClass=\"formgroupheader\" layout=\"linear\"  includeTitle=\"true\"  separator=\"\"  decorate=\"false\" inherited=\"false\"  }}";

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


    @Override
    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props)
            throws Exception {
        if (tag.equals("archive_finding_aid_link")) {
	    String url =
		request.entryUrl(getRepository().URL_ENTRY_SHOW, entry,
				 ARG_OUTPUT,"archive_finding_aid");
	    return HU.div(HU.href(url,"Finding Aid"),HU.attrs("class","ramadda-button","style","margin-bottom:6px;"));
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
	sb.append(HU.cssBlock(".metadata-block {max-height:1000px;}\n.archive-findingaid-header {border-top-left-radius: var(--default-radius);border-top-right-radius: var(--default-radius);padding:4px;padding-left:12px;font-weight:bold;font-size:110%;background:var(--header-background);}\n.archive-findingaid-block {margin-top:1em;margin-left:40px;}\n"));
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
	sb.append("<center>");
	HU.script(sb,"HtmlUtils.initPageSearch('.archive-findingaid-entry',null,'Search in page')");
	sb.append("</center>");

	sb.append("<div class=archive-findingaid-entry>");
	String info = getInfo(request, entry);
	sb.append(HU.center(info));	
	wikify(request, entry,sb,"----");
	if(stringDefined(entry.getDescription())) {
	    wikify(request, entry,sb,entry.getDescription());
	}
	addThumbnails(request, sb, entry);



	wikify(request, entry,sb,propWiki);
	wikify(request, entry,sb,"----");

	sb.append("</div>");
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
	     sb.append("<div class=archive-findingaid-entry>");
	     String label = getTypeName(request, child) +": "+child.getName();
	     label = HU.href(getEntryManager().getEntryUrl(request, child),label);
	     HU.div(sb,label,HU.attrs("class","archive-findingaid-header"));
	     sb.append(getInfo(request, child));
	     wikify(request, child,sb,propWiki);
	     sb.append("</div>\n");
	     sb.append("<div class=archive-findingaid-block>\n");
	     makeFindingAid(request, child, sb);
	     sb.append("</div>\n");
	 }
    }
}
