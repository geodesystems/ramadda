/**
   Copyright (c) 2008-2026 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.projects.assets;
import org.ramadda.repository.*;
import org.ramadda.repository.type.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.WikiUtil;
import org.ramadda.util.NamedBuffer;
import org.ramadda.util.Utils;
import org.w3c.dom.*;
import org.json.*;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.Date;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

@SuppressWarnings("unchecked")
public class AssetBaseTypeHandler extends ExtensibleGroupTypeHandler   {
    public static final String TYPE_BASE = "type_assets_base";
    public static final String TYPE_THING = "type_assets_thing";
    public static final String TYPE_VEHICLE = "type_assets_vehicle";        
    public static final String TYPE_BUILDING = "type_assets_building";
    public static final String TYPE_EQUIPMENT = "type_assets_equipment";
    public static final String TYPE_IT = "type_assets_it";        
    public static final String TYPE_FIXTURE = "type_assets_fixture";
    public static final String TYPE_INTANGIBLE = "type_assets_intangible";
    public static final String TYPE_LICENSE = "type_assets_license";
    public static final String TYPE_DEPARTMENT = "type_assets_department";
    public static final String TYPE_VENDOR = "type_assets_vendor";
    public static final String TYPE_PERSONNEL = "type_assets_personnel";        
    public static final String TYPE_LOCATION = "type_assets_location";

    public static final String ARG_REPORT="report";
    public static final String ARG_ALLCOLUMNS = "allcolumns";
    public static final String ARG_MIN_COST="mincost";
    public static final String ARG_MAX_COST="maxcost";    
    public static final String ARG_DOWNLOAD = "download";

    public static final String ACTION_SEARCH="assets_search";
    public static final String ACTION_REPORT="assets_report";
    public static final String ACTION_NEW="assets_new";
    public static final String ACTION_SCAN="assets_scan";
    public static final String ACTION_DATA_COSTS="assets_data_costs";        

    public static final String REPORT_TABLE = "assets_report_table";
    public static final String REPORT_COUNTS = "assets_report_counts";
    public static final String REPORT_MAINTENANCE = "assets_report_maintenance";
    public static final String REPORT_WARRANTY = "assets_report_warranty";            
    public static final String REPORT_COSTS = "assets_report_costs";
    public static final String REPORT_DOWNLOAD = "assets_report_download";    

    public static final String TAG_HEADER= "assets_header";
    public static final String TAG_SUMMARY= "assets_summary";
    public static final String TAG_REPORT_LINK= "assets_report_link";

    public static final String COL_LOCATION_ENTRY="location_entry";
    public static final String COL_VENDOR="vendor";
    public static final String COL_DEPARTMENT="department";
    public static final String COL_ASSIGNED_TO="assigned_to";

    public static final String WIKI_SCAN=
	"+section title={{name}}\n:heading Scan for Asset\n+center\n{{assets_barcode doScan=true}}\n-center\n-section\n";

    public static final String  WIKI_NEW=
	"+section title={{name}}\n:heading Create New Asset\n+center\n{{new_entry   template=\"${type}\" fromEntry=true    message=\"\"  }}\n:vspace 1em\n{{assets_barcode #type=type_assets_vehicle}}\n-section\n";

    public static final String WIKI_SEARCH =
	"+section title={{name}}\n:heading Asset Search\n{{display_entrylist     showEntryType=true orderByTypes=\"name,acquisition_cost,relevant,date,createdate,changedate\"  \nshowAncestor=false ancestor=this  typesLabel=\"Asset Type\"  typesToggleClose=false displayTypes=\"list,images,map,display\" showName=true  \ntoggleClose=true  \nentryTypes=\"super:type_assets_base,super:type_assets_thing\" \nexcludeTypes=\"type_assets_thing,type_assets_physical\"\n}} \n-section";


    private String assetHeaderWiki;
    private String assetSummaryWiki;    
    private String summaryReportWiki;

    public AssetBaseTypeHandler(Repository repository, Element node)
	throws Exception {
        super(repository, node);
	assetHeaderWiki =  getStorageManager().readUncheckedSystemResource("/org/ramadda/projects/assets/assetheader.txt");
	assetSummaryWiki = getStorageManager().readUncheckedSystemResource("/org/ramadda/projects/assets/assetsummary.txt");
	summaryReportWiki =getStorageManager().readUncheckedSystemResource("/org/ramadda/projects/assets/summaryreport.txt");
    }

    @Override
    public void getWikiTags(List<WikiTag> tags, Entry entry) {
	super.getWikiTags(tags, entry);
	tags.add(new WikiTag(TAG_HEADER,"Asset Group Header"));
	tags.add(new WikiTag(TAG_SUMMARY,"Asset Summary"));	
	tags.add(new WikiTag(REPORT_TABLE+" #types=\"type_assets_it,type_assets_equipment,...\" showHeader=true","Assets Report Table"));
	tags.add(new WikiTag(REPORT_COUNTS,"Assets Report Counts"));
        tags.add(new WikiTag(REPORT_MAINTENANCE,"Assets Report Maintenance"));
        tags.add(new WikiTag(REPORT_WARRANTY+" showWarranties=true showLicenses=true showDetails=true ","Assets Report Warranty"));
        tags.add(new WikiTag(REPORT_COSTS+" #types=\"type_assets_it,type_assets_equipment,...\" #showSummary=false #showTable=false #summaryTitle=\"\"  #tableTitle=\"\" ","Assets Report Costs"));				
        tags.add(new WikiTag("raw:dataUrl=\"" + getRepository().getUrlPath("/entry/action?action=assets_data_costs&entryid=${entryid}&output=\""),
			     "Assets Costs URL"));
    }

    private String getSearchUrl(Request request, Entry entry, Hashtable props) throws Exception {
	if(entry.isType(TYPE_DEPARTMENT))
	    return  getWikiManager().makeEntryLinkSearchUrl(request, entry,TYPE_BASE,COL_DEPARTMENT);
	if(entry.isType(TYPE_PERSONNEL)) 
	    return  getWikiManager().makeEntryLinkSearchUrl(request, entry,TYPE_BASE,COL_ASSIGNED_TO);
	if(entry.isType(TYPE_VENDOR))
	    return  getWikiManager().makeEntryLinkSearchUrl(request, entry,TYPE_BASE,COL_VENDOR);
	if(entry.isType(TYPE_LOCATION))
	    return  getWikiManager().makeEntryLinkSearchUrl(request, entry,TYPE_BASE,COL_LOCATION_ENTRY);
	String types = Utils.getProperty(props,"types","super:" + TYPE_BASE+"%2C"+ TYPE_LICENSE);
	return  "/search/do?forsearch=true&type=" + types +"&orderby=name&ascending=true&ancestor=" + entry.getId()+"&max=10000";
    }

    private List<Entry> getEntries(Request request, Entry entry, Hashtable props) throws Exception {
	List<Entry> entries = new ArrayList<Entry>();
	if(entry.isType(TYPE_DEPARTMENT)) {
	    String searchUrl = getWikiManager().makeEntryLinkSearchUrl(request, entry,TYPE_BASE,COL_DEPARTMENT);
	    return getSearchManager().processSearchUrl(request, entries,searchUrl);
	}
	if(entry.isType(TYPE_PERSONNEL)) {
	    String searchUrl = getWikiManager().makeEntryLinkSearchUrl(request, entry,TYPE_BASE,COL_ASSIGNED_TO);
	    return getSearchManager().processSearchUrl(request, entries,searchUrl);
	}	
	if(entry.isType(TYPE_VENDOR)) {
	    String searchUrl = getWikiManager().makeEntryLinkSearchUrl(request, entry,TYPE_BASE,COL_VENDOR);
	    return getSearchManager().processSearchUrl(request, entries,searchUrl);
	}
	if(entry.isType(TYPE_LOCATION)) {
	    String searchUrl = getWikiManager().makeEntryLinkSearchUrl(request, entry,TYPE_BASE,COL_LOCATION_ENTRY);
	    return getSearchManager().processSearchUrl(request, entries,searchUrl);
	}		
	return getSearchManager().processSearchUrl(request, entries,getSearchUrl(request, entry,props));
    }

    private void wikify(Request request, Entry entry, StringBuilder sb, String wiki) throws Exception {
	sb.append(getWikiManager().wikifyEntry(request, entry,wiki));
    }

    public Result processEntryAction(Request request, Entry entry)
	throws Exception {
        String action = request.getString("action", "");
	if(action.equals(ACTION_DATA_COSTS)) {
	    StringBuilder csv = new StringBuilder();
	    makeCsvCosts(request,  entry,  csv,new Hashtable());
	    request.setReturnFilename(Utils.makeID(entry.getName())+"_costs.csv",false);
	    return new Result("", csv, MIME_CSV);
	}
	if(action.equals(ACTION_SEARCH)) return handleActionSearch(request, entry);
	if(action.equals(ACTION_REPORT)) return handleActionReport(request, entry);
	if(action.equals(ACTION_NEW)) return handleActionNew(request, entry);
	if(action.equals(ACTION_SCAN)) return handleActionScan(request, entry);	
	return super.processEntryAction(request,entry);
    }

    public Result handleActionSearch(Request request, Entry entry) throws Exception {
	StringBuilder sb = new StringBuilder();
	wikify(request, entry,sb,WIKI_SEARCH);
        return getEntryManager().addEntryHeader(request, entry, new Result("Asset Search - " + entry.getName(),sb));
    }

    public Result handleActionNew(Request request, Entry entry) throws Exception {
	StringBuilder sb = new StringBuilder();
	wikify(request, entry,sb,WIKI_NEW);
	return getEntryManager().addEntryHeader(request, entry, new Result("New Assets - " + entry.getName(),sb));
    }

    public Result handleActionScan(Request request, Entry entry) throws Exception {
	StringBuilder sb = new StringBuilder();
	wikify(request, entry,sb,WIKI_SCAN);
	return getEntryManager().addEntryHeader(request, entry, new Result("Scan Asset - " + entry.getName(),sb));
    }

    public Result handleActionReport(Request request, Entry entry) throws Exception {
	StringBuilder sb = new StringBuilder();
	String report = request.getString("report",REPORT_TABLE);
	getPageHandler().entrySectionOpen(request, entry, sb, "");
	List<HtmlUtils.Href> headerItems = new ArrayList<HtmlUtils.Href>();
	for(String[]tuple:new String[][]{{REPORT_TABLE,"Table"},{REPORT_COUNTS,"Counts"},
					 {REPORT_COSTS,"Costs"}, {REPORT_WARRANTY,"Warranty"},
					 {REPORT_MAINTENANCE,"Maintenance"},{REPORT_DOWNLOAD,"Download Data"}}) {
	    headerItems.add(new HtmlUtils.Href(HU.url(getEntryActionUrl(request,  entry,ACTION_REPORT), ARG_REPORT,tuple[0]),
					       tuple[1],
					       report.equals(tuple[0])? "ramadda-linksheader-on": "ramadda-linksheader-off"));
	}
	sb.append(HU.center(HU.makeHeader1(headerItems)));
	if(report.equals(REPORT_TABLE))    makeReportTable(request, entry,sb, new Hashtable());
	else if(report.equals(REPORT_MAINTENANCE))   makeReportMaintenance(request, entry,sb, new Hashtable());
	else if(report.equals(REPORT_WARRANTY))   makeReportWarranty(request, entry,sb, new Hashtable());		
	else if(report.equals(REPORT_COUNTS))   makeReportCounts(request, entry,sb,new Hashtable());
	else if(report.equals(REPORT_COSTS))    makeReportCosts(request, entry,sb,new Hashtable());		
	else if(report.equals(REPORT_DOWNLOAD)) {
	    Result result  =   makeReportDownload(request, entry,sb, new Hashtable());
	    if(result!=null) return result;
	} else {
	    sb.append("Unknown report:" + report);
	}
	getPageHandler().entrySectionClose(request, entry, sb);
        return getEntryManager().addEntryHeader(request, entry, new Result("Assets Report - " + entry.getName(),sb));
    }

    @Override
    public String getWikiInclude(WikiUtil wikiUtil, Request request,  Entry originalEntry,
				 Entry entry,  String tag, Hashtable props)
	throws Exception {
	if(tag.equals(TAG_HEADER)) return getWikiManager().wikifyEntry(request, entry,assetHeaderWiki);
	if(tag.equals(TAG_SUMMARY)) return getWikiManager().wikifyEntry(request, entry,assetSummaryWiki);
        if (tag.equals(TAG_REPORT_LINK)) 
	    return HU.div(HU.href(getEntryActionUrl(request,  entry,ACTION_REPORT),"Reports"),
			  HU.attrs("class","ramadda-button","style","margin-bottom:6px;"));
	StringBuilder sb = new StringBuilder();
        if (tag.equals(REPORT_TABLE)) {
	    makeReportTable(request, entry,sb,props);
	} else  if (tag.equals(REPORT_COUNTS)) {
	    makeReportCounts(request, entry,sb,props);
	} else if (tag.equals(REPORT_COSTS)) {
	    makeReportCosts(request, entry,sb,props);
	} else  if (tag.equals(REPORT_WARRANTY)) {
	    makeReportWarranty(request, entry,sb,props);
	} else    if (tag.equals(REPORT_MAINTENANCE)) {
	    makeReportMaintenance(request, entry,sb,props);
	} else {	
	    return super.getWikiInclude(wikiUtil, request, originalEntry,entry, tag, props);
	}
	return sb.toString();
    }

    private Result makeReportDownload(Request request, Entry entry, StringBuilder sb,Hashtable props) throws Exception {
	if(request.defined(ARG_DOWNLOAD)) {
	    String searchUrl = getSearchUrl(request,entry,null);
	    String xlsUrl = request.entryUrl(getRepository().URL_ENTRY_SHOW, entry,
					     ARG_OUTPUT, CsvOutputHandler.OUTPUT_XLSX.toString(),
					     ARG_SHOWCHANGEDATE,"false",
					     ARG_SHOWRESOURCE,"false",
					     ARG_SEARCH_URL,searchUrl);
	    String title = request.getString(ARG_TITLE,null);
	    List<String> types =request.get(ARG_TYPES, new ArrayList<String>());
	    if(types.size()>0) {
		String typeSelect = Utils.join(types,",");
		if(stringDefined(typeSelect)) {
		    xlsUrl = HU.url(xlsUrl,ARG_TYPES,typeSelect);
		}
	    }
	    
	    if(!request.get(ARG_SEPARATETYPES,false)) {
		xlsUrl = HU.url(xlsUrl,ARG_SEPARATETYPES,"false",ARG_TAGS,"_none_",ARG_SHOWTYPE,"true");
		if(stringDefined(title)) title = title.replace("${sheet}","All assets");
	    } else {
		if(!request.get(ARG_ALLCOLUMNS,false)) {
		    xlsUrl = HU.url(xlsUrl,ARG_TAGS,"reportable");
		}	    
	    }
	    if(stringDefined(title)) xlsUrl = HU.url(xlsUrl, ARG_TITLE,title);
	    int op = 0;
	    String minCost = request.getString(ARG_MIN_COST,"");
	    if(stringDefined(minCost)) {
		op++;
		minCost = minCost.replace(",","");
		xlsUrl= HU.url(xlsUrl, ARG_OPERATOR+op,">=",
			       ARG_OPERATOR_COLUMN+op,"acquisition_cost",
			       ARG_OPERATOR_VALUE+op,minCost);
	    }
	    String maxCost = request.getString(ARG_MAX_COST,"");
	    if(stringDefined(maxCost)) {
		op++;
		maxCost = maxCost.replace(",","");
		xlsUrl= HU.url(xlsUrl, ARG_OPERATOR+op,"<=",
			       ARG_OPERATOR_COLUMN+op,"acquisition_cost",
			       ARG_OPERATOR_VALUE+op,maxCost);
	    }
	    return new Result(xlsUrl);
	}
	String url =  request.makeUrl(getRepository().URL_ENTRY_ACTION);
	sb.append(HU.formPost(url));
	sb.append(HU.hidden(ARG_ACTION,ACTION_REPORT));
	sb.append(HU.hidden(ARG_ENTRYID,entry.getId()));
	sb.append(HU.hidden(ARG_REPORT,REPORT_DOWNLOAD));
	sb.append(HU.formTable());
	HU.formEntry(sb,msgLabel("Title"),
		     HU.input(ARG_TITLE,request.getString(ARG_TITLE,"Asset report for ${sheet}"),HU.SIZE_60));
	
	List options = new ArrayList();
	options.add(new HtmlUtils.Selector("All Asset Types",""));
	options.add(new HtmlUtils.Selector("Vehicles",TYPE_VEHICLE));	
	options.add(new HtmlUtils.Selector("Equipment",TYPE_EQUIPMENT));	
	options.add(new HtmlUtils.Selector("Buildings",TYPE_BUILDING));	
	options.add(new HtmlUtils.Selector("IT",TYPE_IT));
	options.add(new HtmlUtils.Selector("Furniture and Fixtures",TYPE_FIXTURE));
	options.add(new HtmlUtils.Selector("Intangible",TYPE_INTANGIBLE));
	options.add(new HtmlUtils.Selector("Licenses",TYPE_LICENSE));		
	HU.formEntry(sb,"",HU.select(ARG_TYPES,options,"","multiple rows=4"));
	HU.formEntry(sb,"Cost Range:",
		     HU.input(ARG_MIN_COST,"",HU.attrs("size","10","placeholder","Min value")) +" - "+
		     HU.input(ARG_MAX_COST,"",HU.attrs("size","10","placeholder","Max value")));
	HU.formEntry(sb,"",HU.labeledCheckbox(ARG_ALLCOLUMNS,"true",true,"All Fields"));
	HU.formEntry(sb,"",HU.labeledCheckbox(ARG_SEPARATETYPES,"true",true,"Separate asset types"));	
	HU.formEntry(sb,"",HU.submit("Download",ARG_DOWNLOAD));
	Utils.append(sb,HU.formTableClose(),HU.formClose());
	return null;
    }

    private void makeReportTable(Request request, Entry entry, StringBuilder sb,Hashtable props) throws Exception {
	boolean showHeader   = Utils.getProperty(props,"showHeader",true);
	String guid = HU.getUniqueId("assets");
	String searchUrl =  getSearchUrl(request,entry,null);	
	String prefix = "+center_nl_:pagesearch parentSelector=\\\"ramadda-entry-table\\\" label=\\\"Search in page\\\" selector=\\\"." +guid+"\\\" _nl_-center";
	String contentsWiki = "{{table message=\":p_nl_+callout-info_nl_No assets available_nl_-callout\" dateLabel=\"Purchase Date\" showHeader=" + showHeader+" " +
	    " contentsHeading=\"" + prefix+"\" " +
	    " entries=\"searchurl:" +
	    searchUrl+"\" display=list showBreadcrumbs=false entryRowClass=\"" + guid+"\"}}";
	wikify(request, entry,sb,contentsWiki);
    }

    private String cleanColumnValue(String v) {
	if(v==null) return v;
	return  v.replace("\"","_qt_").replace("\n","_nl_").replace(",","_comma_");
    }

    private String inlineData(StringBuilder buff,String id,String header,LinkedHashMap<String,Integer> map,String wiki) {
	String guid = HU.getUniqueId(id);
	buff.append(HU.open("div",HU.attrs("style","display:none","id",guid)));
	Utils.append(buff,header,"\n");
	for (String key : map.keySet()) {
	    Utils.append(buff,cleanColumnValue(key),",",map.get(key),"\n");
	}
	buff.append("</div>\n");
	return wiki.replace("${" + id+"}",guid);
    }

    private String strip(Hashtable props,String wiki, String... keys) {
	for(int i=0;i<keys.length;i+=2) {
	    if(!Utils.getProperty(props,keys[i],true)) {
		wiki = wiki.replaceAll("(?s)<" + keys[i+1]+">.*</" + keys[i+1]+">","");
	    }
	}
	return wiki;
    }

    private String inlineData(StringBuilder buff,String id,String csv,String wiki) {
	String guid = HU.getUniqueId(id);
	buff.append(HU.open("div",HU.attrs("style","display:none","id",guid)));
	buff.append(csv);
	buff.append("</div>\n");
	return wiki.replace("${" + id+"}",guid);
    }

    private void addCount(LinkedHashMap<String,Integer> map,String label) {
	if(!stringDefined(label)) label="NA";
	Integer typeCnt = map.get(label);
	if(typeCnt==null)  map.put(label,typeCnt=new Integer(0));
	map.put(label,new Integer(typeCnt+1));
    }

    private void makeReportMaintenance(Request request, Entry entry, StringBuilder buff,Hashtable props ) throws Exception {
	List<Entry> entries = getEntries(request, entry,props);
	String guid = HU.getUniqueId("assets");
	HU.open(buff,"div",HU.attrs("id",guid,"class","dashboard-component","style","min-width:400px;"));
	if(entries.size()==0) {
	    buff.append(getPageHandler().showDialogNote("No assets available"));
	    HU.close(buff,"div");
	    return;
	}
	boolean didOne = false;
	for(Entry child:entries) {
	    //TODO - sort the metadata on date
            List<Metadata> metadataList =
		getMetadataManager().findMetadata(request, child, "asset_maintenance", true);
            if (metadataList == null || metadataList.size() == 0) continue;
	    if(!didOne) {
		buff.append("<br><center>");
		HU.script(buff,"HtmlUtils.initPageSearch('#" +guid+" .ramadda-entry',null,'Search in page')");
		buff.append("</center>");
	    }
	    buff.append("<div class=ramadda-entry>");
	    //	    if(didOne) buff.append("<div class=ramadda-hr></div>\n");
	    didOne=true;
	    HU.div(buff,getEntryManager().getEntryLink(request,child,true,""),HU.style("margin-top:1em;"));
	    buff.append("<div style='margin-top:4px;xmargin-left:20px;' class='ramadda-dashboard-component'>");
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
	if(!didOne) {
	    buff.append(getPageHandler().showDialogNote("No maintenance records available"));
	}
	HU.close(buff,"div");
    }

    private void makeReportWarranty(Request request, Entry entry, StringBuilder buff,Hashtable props ) throws Exception {
	List<Entry> entries = getEntries(request, entry,props);
	if(entries.size()==0) {
	    buff.append(getPageHandler().showDialogNote("No assets available"));
	    return;
	}
	boolean showLicenses = Utils.getProperty(props,"showLicenses",true);
	boolean showWarranties = Utils.getProperty(props,"showWarranties",true);
	boolean showDetails = Utils.getProperty(props,"showDetails",true);		
	boolean didOne = false;
	Date now = new Date();
	Date near = new Date(now.getTime()+Utils.daysToMillis(30));
	List<String>  warrantyExpired=new ArrayList<String>();
	List<String>  warrantyClose=new ArrayList<String>();	
	List<String>  warrantyOk=new ArrayList<String>();
	List<String>  licenseExpired=new ArrayList<String>();
	List<String>  licenseClose=new ArrayList<String>();	
	List<String>  licenseOk=new ArrayList<String>();	
	List<String>  licenseNone=new ArrayList<String>();	
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	for(Entry asset: entries) {
	    Date date=null;
	    boolean isLicense=asset.getTypeHandler().isType("type_assets_license");
	    if(isLicense && !showLicenses) continue;
	    if(!isLicense && !showWarranties) continue;	    

	    if(isLicense) {
		date = (Date) asset.getValue(request, "expiration_date");
	    } else {
		date = (Date) asset.getValue(request, "warranty_expiration");
	    }

	    if(date==null && !isLicense) continue;
	    String link = getEntryManager().getEntryLink(request,asset,true,"");
	    boolean expired = false;
	    boolean close = false;
	    boolean none=false;
	    if(date==null) {
		none = true;
	    }  else {
		expired = date.getTime()< now.getTime();
		if(!expired)
		    close  = date.getTime()<near.getTime();
	    }
	    didOne = true;
	    List<String> list=null;
	    if(isLicense) {
		if(expired) list = licenseExpired;
		else if (close) list = licenseClose;
		else if (none) list = licenseNone;		
		else list = licenseOk;
	    } else {
		if(expired) list = warrantyExpired;
		else if(close) list = warrantyClose;		
		else list = warrantyOk;
	    }
	    list.add(HU.row(HU.td(link)+  HU.td(date==null?"NA":sdf.format(date),"align=right")+HU.td(expired?"Yes":(close?"1 month":(none?"NA":"No"))),
			    HU.attrs("class",expired?"assets-expired":(close?"assets-close":(none?"assets-none":"assets-ok")))));
	}

	buff.append(HU.importCss(".assets-expired {background:#fddcdc !important;}\n.assets-close {background: #fff3cd !important}\n.assets-ok {background:#d4edda !important;}\n"));
	buff.append("<div class=dashboard-component>");
	buff.append("<div  class='row wiki-row'  >");
	String cw = "6";
	String tableHeight="300px";
	String bullet = "&nbsp;&bull;&nbsp;";
	int warrantyTotal = warrantyExpired.size() +  warrantyClose.size()+warrantyOk.size();
	int licenseTotal = licenseExpired.size() +  licenseClose.size() + licenseOk.size() + licenseNone.size();	

	if(licenseTotal==0)  cw = "12";
	else if(warrantyTotal==0)  cw = "12";	
	if(warrantyTotal>0) {
	    buff.append("<div  class='col-md-" + cw+" ramadda-col wiki-col'>");
	    buff.append("<h3>Warranties</h3>");
	    if(warrantyTotal==1) 
		HU.div(buff,bullet  +" 1 asset with a warranty date","");
	    else
		HU.div(buff,bullet+ (warrantyTotal) +" assets with warranty dates","");	    

	    if(warrantyExpired.size()>0)
		HU.div(buff,bullet+ warrantyExpired.size() +
		       (warrantyExpired.size()==1?" warranty past due":" warranties past due"),HU.attrs("class","assets-expired"));
	    if(warrantyClose.size()>0)
		HU.div(buff,bullet+ warrantyClose.size() +
		       (warrantyClose.size()==1?" warranty near due (1 month)":" warranties near due (1 month)"),
		       HU.attrs("class","assets-close"));	    
	    if(warrantyOk.size()>0)
		HU.div(buff,bullet+ warrantyOk.size() +(warrantyOk.size()==1?" warranty current":" warranties current"),
		       HU.attrs("class","assets-ok"));    
	    
	    if(showDetails) {
		buff.append("<br>");
		HU.open(buff,"table",
			"table-height", tableHeight,"table-searching","true",
			"table-ordering","true",
			"table-ordering-init","none",
			"class","ramadda-table");
		HU.tag(buff,"thead","",HU.tag("tr","","<th>Asset</th><th>Warranty Expiration</th><th>Expired</th>"));
		HU.open(buff,"tbody","");
		buff.append(Utils.join(warrantyExpired,""));
		buff.append(Utils.join(warrantyClose,""));		
		buff.append(Utils.join(warrantyOk,""));		
		HU.close(buff,"tbody","table");
	    }
	    HU.close(buff,"div");
	}


	if(licenseTotal>0) {
	    buff.append("<div  class='col-md-" + cw+" ramadda-col wiki-col'>");
	    buff.append("<h3>Licenses</h3>");
	    HU.open(buff,"div",HU.clazz("ramadda-dashboard-component"));
	    HU.div(buff,bullet+ (licenseTotal) +" " + Utils.plural(licenseTotal,"license"),"");
	    if(licenseExpired.size()>0)
		HU.div(buff,bullet+ licenseExpired.size() +" " + Utils.plural(licenseExpired.size(),"license")+" expired",HU.attrs("class","assets-expired"));
	    if(licenseClose.size()>0)
		HU.div(buff,bullet+ licenseClose.size() +" " + Utils.plural(licenseClose.size(),"license")+" near due (1 month)",
		       HU.attrs("class","assets-close"));	    
	    if(licenseOk.size()>0)
		HU.div(buff, bullet+ licenseOk.size() +" " + Utils.plural(licenseOk.size(),"license")+" current",
		       HU.attrs("class","assets-ok"));
	    if(licenseNone.size()>0)
		HU.div(buff, bullet+ licenseNone.size() +" " +Utils.plural(licenseNone.size(),"license")+" with no expiration date",
		       HU.attrs("class","assets-none"));

	    HU.close(buff,"div");
	    if(showDetails) {	    
		buff.append("<br>");
		HU.open(buff,"table",
			"table-height", tableHeight,"table-searching","true",
			"table-ordering","true",
			"table-ordering-init","none",
			"class","ramadda-table");

		HU.tag(buff,"thead","",HU.tag("tr","","<th>License</th><th>Expiration Date</th><th>Expired?</th>"));
		HU.open(buff,"tbody","");
		buff.append(Utils.join(licenseExpired,""));
		buff.append(Utils.join(licenseClose,""));		
		buff.append(Utils.join(licenseOk,""));
		buff.append(Utils.join(licenseNone,""));				
		HU.close(buff,"tbody","table");
	    }
	    HU.close(buff,"div");
	}
	HU.close(buff,"div","div");


	if(!didOne) {
	    buff.append(getPageHandler().showDialogNote("No assets or licenses have a warranty expiration date"));
	    return;
	}
    }    

    private void makeReportCounts(Request request, Entry entry, StringBuilder buff,Hashtable props ) throws Exception {
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
	int statusCnt = 0;
	double min=request.get(ARG_MIN_COST,-1);
	double max=request.get(ARG_MAX_COST,-1);
	String url =  request.makeUrl(getRepository().URL_ENTRY_ACTION);
	buff.append(HU.formPost(url));
	Utils.append(buff,HU.hidden(ARG_ACTION,ACTION_REPORT),HU.hidden(ARG_REPORT,REPORT_COUNTS),HU.hidden(ARG_ENTRYID,entry.getId()));
	buff.append(HU.formTable());
 	HU.formEntry(buff,"Cost Range:",
		     HU.input(ARG_MIN_COST,request.getString(ARG_MIN_COST,""),HU.attrs("size","10","placeholder","Min value")) +" - "+
		     HU.input(ARG_MAX_COST,request.getString(ARG_MAX_COST,""),HU.attrs("size","10","placeholder","Max value")) +
		     HU.SPACE2 +HU.submit("Apply",""));
	buff.append(HU.formTableClose());
	buff.append(HU.formClose());
	int cnt = 0;
	for(Entry child: entries) {
	    if(!child.getTypeHandler().isType("type_assets_base")) {
		continue;
	    }
	    if(min>=0) {
		double cost = child.getDoubleValue(request, "acquisition_cost",Double.NaN);
		if(Double.isNaN(cost) || cost<min) continue;
	    }
	    if(max>=0) {
		double cost = child.getDoubleValue(request, "acquisition_cost",Double.NaN);
		if(Double.isNaN(cost) || cost>max) continue;
	    }

	    cnt++;
	    addCount(types,child.getTypeHandler().getLabel());
	    Entry departmentEntry = getDepartment(request,child);
	    addCount(department,departmentEntry==null?"NA":departmentEntry.getName());
	    Entry locationEntry = getLocation(request,child);
	    addCount(location,locationEntry==null?"NA":locationEntry.getName());
	    String assignedTo = child.getEnumValue(request,COL_ASSIGNED_TO,"");
	    Entry personnel = getEntryManager().getEntry(request, assignedTo,true);
	    addCount(assignedto,personnel!=null?personnel.getName():"NA");
	    if(child.getTypeHandler().isType("type_assets_physical")) {
		statusCnt++;
		addCount(status,child.getEnumValue(request,"status",""));	     	     	     
		addCount(condition,child.getEnumValue(request,"condition",""));
	    }
	}
	HU.div(buff,cnt+" assets");
	String wiki =	summaryReportWiki;
	if(statusCnt==0) {
	    wiki = wiki.replaceAll("(?s)<status>.*</status>","");
	}
	wiki = inlineData(buff,"typesdata","type,count",types,wiki);
	wiki = inlineData(buff,"departmentdata","department,count",department,wiki);
	wiki = inlineData(buff,"locationdata","location,count",location,wiki);
	wiki = inlineData(buff,"assignedtodata","assigned to,count",assignedto,wiki);
	wiki = inlineData(buff,"statusdata","status,count",status,wiki);
	wiki = inlineData(buff,"conditiondata","condition,count",condition,wiki);	 	 	 	 	 
	buff.append(getWikiManager().wikifyEntry(request, entry, wiki));
    }

    private Entry getEntry(Request request, String id) throws Exception {
	if(!stringDefined(id)) return null;
	return getEntryManager().getEntry(request, id,true);
    }

    private Entry getVendor(Request request, Entry entry) throws Exception {
	return getEntry(request,entry.getStringValue(request, COL_VENDOR,null));
    }

    private Entry getLocation(Request request, Entry entry) throws Exception {
	return getEntry(request,entry.getStringValue(request, COL_LOCATION_ENTRY,null));
    }
    
    private Entry getDepartment(Request request, Entry entry) throws Exception {
	return getEntry(request,entry.getStringValue(request, COL_DEPARTMENT,null));
    }    

    private void makeReportCosts(Request request, Entry entry, StringBuilder buff,Hashtable props ) throws Exception {
	StringBuilder csv = new StringBuilder();
	if(!makeCsvCosts(request,  entry,  csv,props )) {
	    buff.append(getPageHandler().showDialogNote("No assets available"));
	    return;
	}
	String wiki =getStorageManager().readUncheckedSystemResource("/org/ramadda/projects/assets/costreport.txt");
	String title = Utils.getProperty(props,"summaryTitle","Summary");
	if(stringDefined(title)) title = ":heading " + title;
	wiki = wiki.replace("${summary_title}",title).replace("${table_title}",Utils.getProperty(props,"tableTitle","Table"));	
	wiki = strip(props,wiki,"showSummary","costs_summary","showTable","costs_table");
	wiki = inlineData(buff,"costdata",csv.toString(),wiki);
	buff.append(getWikiManager().wikifyEntry(request, entry, wiki));
    }
    
    private boolean makeCsvCosts(Request request, Entry entry, StringBuilder csv,Hashtable props ) throws Exception {
	csv.append("Name[type=string],Cost[type=double],Asset Type[type=enumeration],Department[type=enumeration],Vendor[type=enumeration],Url,Icon\n");
	List<Entry> entries = getEntries(request, entry,props);
	int cnt =0;
	for(Entry child: entries) {
	    if(!child.getTypeHandler().isType("type_assets_base")) {
		continue;
	    }
	    Double cost=(Double) child.getValue(request,"acquisition_cost");
	    if(cost==null || Double.isNaN(cost)) continue;
	    String        entryIcon = getPageHandler().getIconUrl(request, child);
	    Entry vendorEntry = getVendor(request, child);
	    Entry departmentEntry = getDepartment(request,child);
	    csv.append(cleanColumnValue(child.getName()));
	    csv.append(","); csv.append(cost);  csv.append(",");
	    csv.append(cleanColumnValue(child.getTypeHandler().getLabel()));
	    csv.append(",");
	    csv.append(cleanColumnValue(departmentEntry==null?"NA":departmentEntry.getName()));
	    csv.append(",");	    
	    csv.append(cleanColumnValue(vendorEntry==null?"NA":vendorEntry.getName()));
	    csv.append(",");	    
	    csv.append(cleanColumnValue(getEntryManager().getEntryURL(request,child)));
	    csv.append(",");
	    csv.append(cleanColumnValue(entryIcon));
	    csv.append("\n");
	    cnt++;
	}
	return cnt!=0;
    }
}
