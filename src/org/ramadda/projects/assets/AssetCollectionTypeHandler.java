/**
   Copyright (c) 2008-2025 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.projects.assets;

import org.ramadda.repository.*;
import org.ramadda.repository.search.SearchManager;
import org.ramadda.repository.type.*;
import org.ramadda.repository.metadata.*;
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


public class AssetCollectionTypeHandler extends ExtensibleGroupTypeHandler   {
    public static final String ARG_REPORT="report";
    public static final String ACTION_SEARCH="assets_search";
    public static final String ACTION_REPORT="assets_report";
    public static final String ACTION_NEW="assets_new";
    public static final String ACTION_SCAN="assets_scan";    
    public static final String REPORT_TABLE = "assets_report_table";
    public static final String REPORT_SUMMARY = "assets_report_summary";
    public static final String REPORT_MAINTENANCE = "assets_report_maintenance";
    public static final String REPORT_WARRANTY = "assets_report_warranty";            
    public static final String REPORT_COSTS = "assets_report_costs";


    public AssetCollectionTypeHandler(Repository repository, Element node)
	throws Exception {
        super(repository, node);
    }

    @Override
    public void getWikiTags(List<String[]> tags, Entry entry) {
	super.getWikiTags(tags, entry);
        tags.add(new String[]{"Assets Report Table",REPORT_TABLE+" #types=\"\" showHeader=true"});
        tags.add(new String[]{"Assets Report Summary",REPORT_SUMMARY +" "});
        tags.add(new String[]{"Assets Report Maintenance",REPORT_MAINTENANCE+" "});
        tags.add(new String[]{"Assets Report Warranty",REPORT_WARRANTY+" "});
        tags.add(new String[]{"Assets Report Costs",REPORT_COSTS+" "});				
    }


    private String getSearchUrl(Request request, Entry entry, Hashtable props) throws Exception {
	List<Entry> entries = new ArrayList<Entry>();
	String types = Utils.getProperty(props,"types","super:type_assets_base%2Ctype_assets_license");
	String searchUrl = "/search/do?forsearch=true&type=" + types +"&orderby=name&ascending=true&ancestor=" + entry.getId()+"&max=10000";
	return searchUrl;
    }


    private List<Entry> getEntries(Request request, Entry entry, Hashtable props) throws Exception {
	List<Entry> entries = new ArrayList<Entry>();
	String types = Utils.getProperty(props,"types","super:type_assets_base%2Ctype_assets_license");
	getSearchManager().processSearchUrl(request, entries,getSearchUrl(request, entry,props));
	return entries;

	//	return  getEntryManager().getChildren(request, entry);
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



    public Result processEntryAction(Request request, Entry entry)
	throws Exception {
        String action = request.getString("action", "");
	if(action.equals(ACTION_SEARCH)) 
	    return handleActionSearch(request, entry);
	if(action.equals(ACTION_REPORT)) 
	    return handleActionReport(request, entry);
	if(action.equals(ACTION_NEW))
	    return handleActionNew(request, entry);
	if(action.equals(ACTION_SCAN))
	    return handleActionScan(request, entry);	
	return super.processEntryAction(request,entry);
    }





    public Result handleActionSearch(Request request, Entry entry) throws Exception {
	StringBuilder sb = new StringBuilder();
	getPageHandler().entrySectionOpen(request, entry, sb, "Asset Search");
	String wiki = "{{display_entrylist     showEntryType=true orderByTypes=\"name,acquisition_cost,relevant,date,createdate,changedate\"  \nshowAncestor=false ancestor=this  typesLabel=\"Asset Type\"  typesToggleClose=false displayTypes=\"list,images,map,display\" showName=true  \ntoggleClose=true  \nentryTypes=\"super:type_assets_base,super:type_assets_thing\" \nexcludeTypes=\"type_assets_thing,type_assets_physical\"\n}} \n";

	wikify(request, entry,sb,wiki);
	getPageHandler().entrySectionClose(request, entry, sb);
	Result result = new Result("Asset Search - " + entry.getName(),sb);
        return getEntryManager().addEntryHeader(request, entry, result);
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

    public Result handleActionScan(Request request, Entry entry) throws Exception {
	StringBuilder sb = new StringBuilder();
	getPageHandler().entrySectionOpen(request, entry, sb, "Scan for Asset");
	String wiki = "+center\n{{assets_barcode doScan=true}}\n";
	wikify(request, entry,sb,wiki);
	getPageHandler().entrySectionClose(request, entry, sb);
	Result result = new Result("Scan Asset - " + entry.getName(),sb);
        return getEntryManager().addEntryHeader(request, entry, result);
    }


    public Result handleActionReport(Request request, Entry entry) throws Exception {
	StringBuilder sb = new StringBuilder();
	String report = request.getString("report",REPORT_TABLE);
	getPageHandler().entrySectionOpen(request, entry, sb, "");

	String searchUrl = getSearchUrl(request,entry,null);
	String xlsUrl = request.entryUrl(getRepository().URL_ENTRY_SHOW, entry,
					 ARG_OUTPUT, CsvOutputHandler.OUTPUT_XLSX.toString(),
					 ARG_SEARCH_URL,searchUrl);

	List<HtmlUtils.Href> headerItems = new ArrayList<HtmlUtils.Href>();
	for(String[]tuple:new String[][]{{REPORT_TABLE,"Table"},
					 {REPORT_SUMMARY,"Summary"},
					 {REPORT_COSTS,"Costs"},
					 {REPORT_WARRANTY,"Warranty"},
					 {REPORT_MAINTENANCE,"Maintenance"},					 
	    }) {
	    headerItems.add(new HtmlUtils.Href(HU.url(getEntryActionUrl(request,  entry,ACTION_REPORT),
						      ARG_REPORT,tuple[0]),
					       tuple[1],
					       report.equals(tuple[0])?
					       "ramadda-linksheader-on":
					       "ramadda-linksheader-off"));
	}

	headerItems.add(new HtmlUtils.Href(xlsUrl,"Download Data"));
	sb.append(HU.center(HU.makeHeader1(headerItems)));


	if(report.equals(REPORT_TABLE))
	    makeReportTable(request, entry,sb, new Hashtable());
	else if(report.equals(REPORT_MAINTENANCE))
	    makeReportMaintenance(request, entry,sb, new Hashtable());
	else if(report.equals(REPORT_WARRANTY))
	    makeReportWarranty(request, entry,sb, new Hashtable());		
	else if(report.equals(REPORT_SUMMARY))
	    makeReportSummary(request, entry,sb,new Hashtable());
	else if(report.equals(REPORT_COSTS))
	    makeReportCosts(request, entry,sb,new Hashtable());		
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
        if (tag.equals(REPORT_TABLE)) {
	    StringBuilder sb = new StringBuilder();
	    makeReportTable(request, entry,sb,props);
	    return sb.toString();
	}
        if (tag.equals(REPORT_SUMMARY)) {
	    StringBuilder sb = new StringBuilder();
	    makeReportSummary(request, entry,sb,props);
	    return sb.toString();
	}
        if (tag.equals(REPORT_COSTS)) {
	    StringBuilder sb = new StringBuilder();
	    makeReportCosts(request, entry,sb,props);
	    return sb.toString();
	}	
        if (tag.equals(REPORT_WARRANTY)) {
	    StringBuilder sb = new StringBuilder();
	    makeReportWarranty(request, entry,sb,props);
	    return sb.toString();
	}		

        if (tag.equals(REPORT_MAINTENANCE)) {
	    StringBuilder sb = new StringBuilder();
	    makeReportMaintenance(request, entry,sb,props);
	    return sb.toString();
	}	


	return super.getWikiInclude(wikiUtil, request, originalEntry,
				    entry, tag, props);

    }


    private void makeReportTable(Request request, Entry entry, StringBuilder sb,Hashtable props) throws Exception {
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	Date now = new Date();
	String types = Utils.getProperty(props,"types","super:type_assets_base,type_assets_license");
	types = HU.urlEncode(types);
	boolean showHeader   = Utils.getProperty(props,"showHeader",true);
	String guid = HU.getUniqueId("assets");
	String contentsWiki = "{{table 	showHeader=" + showHeader+" entries=\"searchurl:/repository/search/do?forsearch=true&type=" + types+"&orderby=name&ascending=true&ancestor=" + entry.getId()+"&max=5000&skip=0\" display=list showBreadcrumbs=false entryRowClass=\"" + guid+"\"}}";


	sb.append("<center>");
	HU.script(sb,"HtmlUtils.initPageSearch('." + guid+"','.ramadda-entry-table','Search in page')");
	sb.append("</center>");
	sb.append("\n");
	//	HU.open(sb,"div",HU.attrs("id",guid,"class","assets-block"));
	sb.append("\n");
	wikify(request, entry,sb,contentsWiki);
	//	HU.close(sb,"div");
	sb.append("\n");
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

    private void inlineData(StringBuilder buff,String id,String csv) {
	buff.append(HU.comment("inline data for "));
	buff.append(HU.open("div",HU.attrs("style","display:none","id",id)));
	buff.append(csv);
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






    private void makeReportMaintenance(Request request, Entry entry, StringBuilder buff,Hashtable props ) throws Exception {
	List<Entry> entries = getEntries(request, entry,props);
	if(entries.size()==0) {
	    buff.append(getPageHandler().showDialogNote("No assets available"));
	    return;
	}
	boolean didOne = false;
	String guid = HU.getUniqueId("assets");
	HU.open(buff,"div",HU.attrs("id",guid));
	for(Entry child:entries) {
	    //TODO - sort the metadata on date
            List<Metadata> metadataList =
                getMetadataManager().findMetadata(request, child,
						  "asset_maintenance", true);
            if (metadataList == null || metadataList.size() == 0) continue;
	    if(!didOne) {
		buff.append("<br><center>");
		HU.script(buff,"HtmlUtils.initPageSearch('#" +guid+" .ramadda-entry',null,'Search in page')");
		buff.append("</center>");
	    }
	    buff.append("<div class=ramadda-entry>");
	    if(didOne)
		buff.append("<div class=ramadda-hr></div>\n");
	    didOne=true;
	    buff.append(getEntryManager().getEntryLink(request,child,true,""));
	    buff.append("<div style='margin-left:20px;'>");
	    buff.append(HU.formTable());
	    buff.append("<tr><td>&nbsp;<b>Date</b>&nbsp;</td><td>&nbsp;<b>Maintainer</b>&nbsp;</td><td>&nbsp;<b>Note</b>&nbsp;</td></tr>");	    
	    for(Metadata mtd:metadataList) {
		HU.row(buff,HU.td(mtd.getAttr1())+
		       HU.td(mtd.getAttr2())+
		       HU.td(mtd.getAttr3()));		       
	    }
	    buff.append(HU.formTableClose());
	    HU.close(buff,"div","div");
	}
	buff.append("</div>");
	if(!didOne) {
	    buff.append(getPageHandler().showDialogNote("No maintenance records available"));
	}
    }

    private void makeReportWarranty(Request request, Entry entry, StringBuilder buff,Hashtable props ) throws Exception {
	List<Entry> entries = getEntries(request, entry,props);
	if(entries.size()==0) {
	    buff.append(getPageHandler().showDialogNote("No assets available"));
	    return;
	}
	boolean didOne = false;
	Date now = new Date();
	StringBuilder pastDueSB = new StringBuilder();
	StringBuilder postDueSB = new StringBuilder();
	StringBuilder licensePastDueSB = new StringBuilder();
	StringBuilder licensePostDueSB = new StringBuilder();		
	int warrantyPastDueCnt =0;
	int warrantyPostDueCnt =0;
	int licensePastDueCnt =0;
	int licensePostDueCnt =0;		
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	for(Entry asset: entries) {
	    Date date=null;
	    boolean isLicense=asset.getTypeHandler().isType("type_assets_license");

	    if(isLicense) {
		date = (Date) asset.getValue(request, "expiration_date");
	    } else {
		date = (Date) asset.getValue(request, "warranty_expiration");
	    }
	    if(date==null) continue;
	    didOne = true;
	    String link = getEntryManager().getEntryLink(request,asset,true,"");
	    boolean expired = date.getTime()< now.getTime();
	    if(expired) {
		if(isLicense) licensePastDueCnt++;
		else warrantyPastDueCnt++;
	    } else {
		if(isLicense) licensePostDueCnt++;
		else warrantyPostDueCnt++;
	    }


	    StringBuilder tmp = isLicense?(expired?licensePastDueSB:licensePostDueSB):
		(expired?pastDueSB:postDueSB);
	    HU.row(tmp,HU.td(link)+  HU.td(sdf.format(date),"align=right")+HU.td(expired?"Yes":"No"),
		   HU.attrs("class",expired?"assets-expired":""));
	}

	buff.append(HU.importCss(".assets-expired {background:#f8d7da;}\n"));
	buff.append("<div  class='row wiki-row'  >");
	String cw = "6";
	if(licensePastDueSB.length()==0 && licensePostDueSB.length()==0)  cw = "12";
	else if(pastDueSB.length()==0 && postDueSB.length()==0)  cw = "12";	
	if(pastDueSB.length()>0 || postDueSB.length()>0) {
	    buff.append("<div  class='col-md-" + cw+" ramadda-col wiki-col'>");
	    buff.append(HU.formTable());
	    buff.append("<h3>Warranties</h3>");
	    if(warrantyPastDueCnt+warrantyPostDueCnt==1)
		buff.append((warrantyPastDueCnt+warrantyPostDueCnt) +" asset with a warranty<br>");
	    else
		buff.append((warrantyPastDueCnt+warrantyPostDueCnt) +" assets with warranties<br>");	    
	    if(warrantyPastDueCnt>0)
		buff.append(warrantyPastDueCnt +(warrantyPastDueCnt==1?" warrany past due<br>":" warranties past due<br>"));
	    if(warrantyPostDueCnt>0)
		buff.append(warrantyPostDueCnt +(warrantyPostDueCnt==1?" warranty current<br>":" warranties current<br>"));	    
	    
	    buff.append("<tr><td>&nbsp;<b>Asset</b>&nbsp;</td><td>&nbsp;<b>Warranty Expiration Date</b>&nbsp;</td><td>&nbsp;<b>Expired</b>&nbsp;</tr>");
	    if(pastDueSB.length()>0) 
		buff.append(pastDueSB);
	    if(postDueSB.length()>0) 
		buff.append(postDueSB);	    
	    buff.append(HU.formTableClose());
	    buff.append("</div>");
	}


	if(licensePastDueSB.length()>0 || licensePostDueSB.length()>0) {
	    buff.append("<div  class='col-md-" + cw+" ramadda-col wiki-col'>");
	    buff.append(HU.formTable());
	    buff.append("<h3>Licenses</h3>");
	    buff.append((licensePastDueCnt+licensePostDueCnt) +" " + Utils.plural(licensePastDueCnt+licensePostDueCnt,"license") +" with expiration dates<br>");
	    if(licensePastDueCnt>0)
		buff.append(licensePastDueCnt +Utils.plural(licensePastDueCnt,"license")+" expired<br>");
	    if(licensePostDueCnt>0)
		buff.append(licensePostDueCnt +Utils.plural(licensePostDueCnt,"license")+" current<br>");	    
	    buff.append("<tr><td>&nbsp;<b>License</b>&nbsp;</td><td>&nbsp;<b>License Expiration Date</b>&nbsp;</td><td>&nbsp;<b>Expired</b>&nbsp;</tr>");
	    buff.append(licensePastDueSB);
	    buff.append(licensePostDueSB);	    
	    buff.append(HU.formTableClose());
	    buff.append("</div>");
	}
	buff.append("</div>");


	if(!didOne) {
	    buff.append(getPageHandler().showDialogNote("No assets or licenses have a warranty expiration date"));
	    return;
	}

    }    



    private void makeReportSummary(Request request, Entry entry, StringBuilder buff,Hashtable props ) throws Exception {
	List<Entry> entries = getEntries(request, entry,props);
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



    private void makeReportCosts(Request request, Entry entry, StringBuilder buff,Hashtable props ) throws Exception {
	List<Entry> entries = getEntries(request, entry,props);
	if(entries.size()==0) {
	    buff.append(getPageHandler().showDialogNote("No assets available"));
	    return;
	}


	StringBuilder csv = new StringBuilder();
	csv.append("Name,Cost,Asset Type,Url,Icon\n");
	List<NamedBuffer> contents = new ArrayList<NamedBuffer>();
	LinkedHashMap<String,Double> costs = new LinkedHashMap<String,Double>();
	int cnt =0;
	for(Entry child: entries) {
	    if(!child.getTypeHandler().isType("type_assets_base")) {
		continue;
	    }
	    Double cost=(Double) child.getValue(request,"acquisition_cost");
	    if(cost==null || Double.isNaN(cost) || cost==0) continue;

	    String url = getEntryManager().getEntryURL(request,child);
	    String        entryIcon = getPageHandler().getIconUrl(request, child);
	    csv.append(Seesv.cleanColumnValue(child.getName()));
	    csv.append(",");	    
	    csv.append(cost);
	    csv.append(",");
	    csv.append(Seesv.cleanColumnValue(child.getTypeHandler().getLabel()));
	    csv.append(",");
	    csv.append(Seesv.cleanColumnValue(url));
	    csv.append(",");
	    csv.append(Seesv.cleanColumnValue(entryIcon));
	    csv.append("\n");
	    cnt++;
	}

	//	System.out.println(csv);
	if(cnt==0) {
	    buff.append(getPageHandler().showDialogNote("No assets available"));
	    return;
	}
	inlineData(buff,"costdata",csv.toString());
	String wiki =getStorageManager().readUncheckedSystemResource("/org/ramadda/projects/assets/costreport.txt");
	buff.append(getWikiManager().wikifyEntry(request, entry, wiki));
    }
    

}
