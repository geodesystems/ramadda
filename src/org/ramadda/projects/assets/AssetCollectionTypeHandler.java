/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.projects.assets;

import org.ramadda.repository.*;
import org.ramadda.repository.search.SearchManager;
import org.ramadda.repository.type.*;
import org.ramadda.repository.output.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.WikiUtil;
import org.ramadda.util.NamedBuffer;
import org.ramadda.util.Utils;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.seesv.Seesv;

import org.w3c.dom.*;
import org.json.*;

import java.util.LinkedHashMap;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.Hashtable;
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
	return entry.getTypeHandler().getLabel();
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


    public static final String ARG_REPORT="report";
    public static final String ACTION_REPORT="assets_report";
    public static final String ACTION_NEW="assets_new";    
    public static final String REPORT_TABLE = "assets_report_table";
    public static final String REPORT_SUMMARY = "assets_report_summary";    

    public Result processEntryAction(Request request, Entry entry)
            throws Exception {
        String action = request.getString("action", "");
	if(action.equals(ACTION_REPORT)) 
	    return handleActionReport(request, entry);
	if(action.equals(ACTION_NEW))
	    return handleActionNew(request, entry);
	return super.processEntryAction(request,entry);
    }







    public Result handleActionNew(Request request, Entry entry) throws Exception {
	StringBuilder sb = new StringBuilder();
	getPageHandler().entrySectionOpen(request, entry, sb, "Create New Asset");
	String wiki = "+center\n{{new_entry   fromEntry=true    message=\"\"  }}\n:vspace 1em\n{{assets_barcode #type=type_assets_vehicle}}\n";

	wikify(request, entry,sb,wiki);
	getPageHandler().entrySectionClose(request, entry, sb);
	Result result = new Result("New Assets - " + entry.getName(),sb);
        return getEntryManager().addEntryHeader(request, entry, result);
    }

    public Result handleActionReport(Request request, Entry entry) throws Exception {
	StringBuilder sb = new StringBuilder();
	String report = request.getString("report",REPORT_TABLE);
	getPageHandler().entrySectionOpen(request, entry, sb, "");

	String searchUrl = "/search/do?forsearch=true&type=super:type_assets_base%2Ctype_assets_license&orderby=name&ascending=true&ancestor=" + entry.getId()+"&max=10000";

	String xlsUrl = request.entryUrl(getRepository().URL_ENTRY_SHOW, entry,
					 ARG_OUTPUT, CsvOutputHandler.OUTPUT_XLSX.toString(),
					 ARG_SEARCH_URL,searchUrl);

	List<HtmlUtils.Href> headerItems = new ArrayList<HtmlUtils.Href>();
	headerItems.add(new HtmlUtils.Href(HU.url(getEntryActionUrl(request,  entry,ACTION_REPORT),
						  ARG_REPORT,REPORT_TABLE),
					   "Table",
					   report.equals(REPORT_TABLE)?
					   "ramadda-linksheader-on":
					   "ramadda-linksheader-off"));
	headerItems.add(new HtmlUtils.Href(HU.url(getEntryActionUrl(request,  entry,ACTION_REPORT),
						  ARG_REPORT,REPORT_SUMMARY),
					   "Summary",
					   report.equals(REPORT_SUMMARY)?
					   "ramadda-linksheader-on":
					   "ramadda-linksheader-off"));	

	headerItems.add(new HtmlUtils.Href(xlsUrl,"Download XLSX"));
	sb.append(HU.center(HU.makeHeader1(headerItems)));


	if(report.equals(REPORT_TABLE))
	    makeTableReport(request, entry,sb);
	else if(report.equals(REPORT_SUMMARY))
	    makeSummaryReport(request, entry,sb);	
	else
	    sb.append("Unknown report:" + report);

	getPageHandler().entrySectionClose(request, entry, sb);
	Result result = new Result("Assets Report - " + entry.getName(),sb);
        return getEntryManager().addEntryHeader(request, entry, result);

    }


    @Override
    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props)
            throws Exception {
        if (tag.equals("assets_report_link")) {
	    String url =getEntryActionUrl(request,  entry,ACTION_REPORT);
	    return HU.div(HU.href(url,"Reports"),HU.attrs("class","ramadda-button","style","margin-bottom:6px;"));
	}
	return super.getWikiInclude(wikiUtil, request, originalEntry,
				    entry, tag, props);

    }

    private void makeTableReport(Request request, Entry entry, StringBuilder sb) throws Exception {
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	Date now = new Date();
	sb.append("<center>");
	HU.script(sb,"HtmlUtils.initPageSearch('.ramadda-entry','.ramadda-entry-table','Search in page')");
	sb.append("</center>");
	sb.append("<div class=assets-entry>");
	wikify(request, entry,sb,"----");
	if(stringDefined(entry.getDescription())) {
	    wikify(request, entry,sb,entry.getDescription());
	}
	sb.append("</div>");
	sb.append("<div class=assets-block>\n");
	String contentsWiki = "{{table 	showHeader=true entries=\"searchurl:/repository/search/do?forsearch=true&type=super:type_assets_base%2Ctype_assets_license&orderby=name&ascending=true&ancestor=" + entry.getId()+"&max=5000&skip=0\" display=list showBreadcrumbs=false xmax=5000}}";
	wikify(request, entry,sb,contentsWiki);
    }



    private void inlineData(StringBuilder buff,String id,String header,LinkedHashMap<String,Integer> map) {
	buff.append(HU.comment("inline data for " + id));
	buff.append(HU.open("div",HU.attrs("style","display:none","id",id)));
	buff.append(header);
	buff.append("\n");
	for (String key : map.keySet()) {
	    Integer value = map.get(key);
	    buff.append(Seesv.cleanColumnValue(key));
	    buff.append(",");
	    buff.append(value);
	    buff.append("\n");
	}
	HU.close(buff,"div");
	buff.append("\n");
    }


    private void addSummary(LinkedHashMap<String,Integer> map,String label) {
	if(!stringDefined(label)) label="NA";
	Integer typeCnt = map.get(label);
	if(typeCnt==null) {
	    map.put(label,typeCnt=new Integer(0));
	}		 
	map.put(label,new Integer(typeCnt+1));
    }



    private void makeSummaryReport(Request request, Entry entry, StringBuilder buff ) throws Exception {
	 List<Entry> entries = getEntryManager().getChildren(request, entry);
	 if(entries.size()==0) {
	     buff.append(getPageHandler().showDialogNote("No assets available"));
	     return;
	 }

	 List<NamedBuffer> contents = new ArrayList<NamedBuffer>();
	 LinkedHashMap<String,Integer> types = new LinkedHashMap<String,Integer>();
	 LinkedHashMap<String,Integer> department = new LinkedHashMap<String,Integer>();
	 LinkedHashMap<String,Integer> location = new LinkedHashMap<String,Integer>();
	 LinkedHashMap<String,Integer> assignedto = new LinkedHashMap<String,Integer>();
	 LinkedHashMap<String,Integer> status = new LinkedHashMap<String,Integer>();
	 LinkedHashMap<String,Integer> condition = new LinkedHashMap<String,Integer>();	 	 	 	 	 
	 for(Entry child: entries) {
	     if(!child.getTypeHandler().isType("type_assets_base")) {
		 continue;
	     }
	     addSummary(types,child.getTypeHandler().getLabel());
	     addSummary(department,child.getEnumValue(request,"department",""));
	     addSummary(location,child.getEnumValue(request,"location",""));
	     addSummary(assignedto,child.getEnumValue(request,"assigned_to",""));
	     if(child.getTypeHandler().isType("type_assets_physical")) {
		 addSummary(status,child.getEnumValue(request,"status",""));	     	     	     
		 addSummary(condition,child.getEnumValue(request,"condition",""));
	     }
	 }

	 inlineData(buff,"typesdata","type,count",types);
	 inlineData(buff,"departmentdata","department,count",department);
	 inlineData(buff,"locationdata","location,count",location);
	 inlineData(buff,"assignedtodata","assigned to,count",assignedto);
	 inlineData(buff,"statusdata","status,count",status);
	 inlineData(buff,"conditiondata","condition,count",condition);	 	 	 	 	 
	 String wiki =getStorageManager().readUncheckedSystemResource("/org/ramadda/projects/assets/summaryreport.txt");
	 buff.append(getWikiManager().wikifyEntry(request, entry, wiki));

    }


}
