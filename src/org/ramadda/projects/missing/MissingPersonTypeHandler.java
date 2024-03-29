/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.projects.missing;

import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.PhoneUtils;
import org.ramadda.util.Utils;
import org.ramadda.util.WikiUtil;
import ucar.unidata.util.StringUtil;
import org.w3c.dom.*;
import java.text.SimpleDateFormat;
import java.util.Random;
import java.util.Date;
import java.util.Hashtable;
import java.util.ArrayList;
import java.util.List;

public class MissingPersonTypeHandler extends ExtensibleGroupTypeHandler {
    public MissingPersonTypeHandler(Repository repository, Element entryNode) throws Exception {
        super(repository, entryNode);
    }

    @Override
    public void initializeNewEntry(Request request, Entry entry,boolean fromImport)
	throws Exception {
        super.initializeNewEntry(request, entry, fromImport);
        if (fromImport) {
            return;
        }
	String caseNumber = (String) entry.getValue("case_number",null);
	if(stringDefined(caseNumber)) return;
	int cnt = getEntryUtil().getEntryCount(this);
	int base = 10;
	cnt=cnt=base+1;
        int random = new Random().nextInt(900) + 100;
	caseNumber=("MP-" + random)+"-"+(cnt);
	entry.setValue("case_number",caseNumber);
	getMetadataManager().addMetadataAlias(request,entry,caseNumber);
    }


    private void makeBlock(Appendable sb, String clazz,String header,String contents) throws Exception {
	HU.open(sb,"div",HU.cssClass(clazz));
	HU.div(sb,header,HU.cssClass("missing-sub-header"));
	sb.append(contents);
	sb.append("</div>");
    }

    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props)
            throws Exception {
        if ( !tag.equals("missing_header")) {
	    return super.getWikiInclude(wikiUtil, request, originalEntry, entry, tag, props);
	}

	boolean forSearch = request.get("forsearch",false);
	StringBuilder sb = new StringBuilder();
	linkCSS(request, sb, getRepository().getHtdocsUrl("/missing/missing.css"));
	String url = request.entryUrl(getRepository().URL_ENTRY_SHOW, entry);
	String nickname=(String)entry.getValue("nickname");
	String status=(String)entry.getValue("status","");	
	sb.append("<div class=missing-header>");
	sb.append("<div class=missing-header1>");
	sb.append(HU.href(url));
	sb.append("Missing Person/#" + entry.getValue("case_number","")+"<br>");
	sb.append(entry.getName().trim());
	if(stringDefined(nickname)) sb.append(" - \"" + nickname.trim()+"\"");
	sb.append(", ");
	if(forSearch) {
	    findColumn("status").formatValue(request, entry, sb, Column.OUTPUT_HTML, entry.getValues(),
					     false);
	    sb.append(", ");
	}
	//	String sex=(String)entry.getValue("biological_sex");
	Column sexColumn = findColumn("biological_sex");
        sexColumn.formatValue(request, entry, sb, Column.OUTPUT_HTML, entry.getValues(),
                           false);
	//	if(sex==null) sex = "unknown";
	//	sb.append(sex.equals("m")?"Male":sex.equals("f")?"Female":sex);
	sb.append(", ");
	Column ethnicityColumn = findColumn("race_ethnicity");
        ethnicityColumn.formatValue(request, entry, sb, Column.OUTPUT_HTML, entry.getValues(),
                           false);

	//	sb.append(entry.getValue("race_ethnicity"));
	sb.append(HU.close("a"));
	sb.append("</div>");
	int years;
	SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");
	Date birthDate = DateHandler.checkDate(new Date(entry.getStartDate()));
	Date missingDate = DateHandler.checkDate((Date) entry.getValue("date_missing"));
	Date foundDate = DateHandler.checkDate((Date) entry.getValue("date_found"));	
	String clazz="missing-block missing-status-"+ status; 
	StringBuilder blocks = new StringBuilder();
	blocks.append("<div style='margin-top:5px;border-top:var(--basic-border);'>");
	if(Utils.getProperty(props,"includeImage",false)) {
	    List<String> urls =    getMetadataManager().getThumbnailUrls(request, entry, null);
	    if(urls!=null && urls.size()>0) {
		HU.div(blocks,HU.image(urls.get(0),HU.attrs("width","100px")),HU.attrs("class","missing-block"));
	    }
	}

	makeBlock(blocks,clazz,"Status",Utils.applyCase(Utils.CASE_PROPER,status));
	if(missingDate!=null) {
	    String label = sdf.format(missingDate);
	    if(birthDate!=null) {
		years = DateHandler.getYearsBetween(birthDate,missingDate);
		label+="<br>" +years+" years old";
	    //		makeBlock(blocks,clazz,"Age when missing", 
	    }
	    makeBlock(blocks,clazz,"Date last seen", label);
	    if(birthDate!=null) {
		//		int years = DateHandler.getYearsBetween(birthDate,missingDate);
		//		makeBlock(sb,clazz,"Age when missing", years+" years");
		if(status.equals("missing")) {
		    years = DateHandler.getYearsBetween(birthDate,new Date());
		    makeBlock(blocks,clazz,"Current age",years+" years old");
		} else if(foundDate!=null) {
		    String label1 = null;
		    String label2 = null;
		    if(status.equals("died")) {
			label1= "Date of death";
			label2 = "Age of death";
		    } else  if(status.equals("found")) {
			label1 ="Date found";
			label2 ="Age found";			    
		    }
			
		    if(label1!=null) {
			years = DateHandler.getYearsBetween(birthDate,foundDate);
			makeBlock(blocks,clazz,label1,
				  sdf.format(foundDate)+"<br>"+years+" years old");

			//			makeBlock(blocks,clazz,label2,years+" years old");
		    }
		}
	    }
	}
	blocks.append("</div></div>");
	if(forSearch) {
	    sb.append(HU.makeShowHideBlock("Details",blocks.toString(),false));
	} else {
	    sb.append(blocks);
	}




	return sb.toString();
    }

    public Result processEntryAction(Request request, Entry entry)
            throws Exception {
        String action = request.getString("action", "");
	if(!action.equals("flyer")) {
	    return super.processEntryAction(request, entry);
	}
	request.put("template","empty");
	Object[] values = entry.getValues();
	StringBuilder sb = new StringBuilder();
	linkCSS(request, sb, getRepository().getHtdocsUrl("/missing/missing.css"));
	String agencyImage=getMetadataManager().getMetadataUrl(request, entry,"missing_agency_image");
	if(agencyImage==null) agencyImage="";
	else agencyImage=HU.image(agencyImage,HU.attrs("width","150px"));
	sb.append("<div class=missing-flyer>");
	sb.append("<div class=missing-flyer-header>");
	sb.append("<table width=100%><tr valign=bottom>");
	sb.append(HU.td(agencyImage,HU.attrs("width","15%")));
	String title = HU.div("MISSING",HU.cssClass(" missing-flyer-title "));
	sb.append("<td width=60% align=center>");
	sb.append(title);
	sb.append("<div>IF YOU HAVE ANY INFORMATION ABOUT</div>");
	sb.append(HU.div(entry.getName(),HU.cssClass("missing-flyer-name")));
	sb.append("</td>");
	String qr = getWikiManager().wikifyEntry(request, entry,"{{qrcode width=100}}");
	sb.append(HU.td(qr+HU.span("More Information",HU.style("font-size:12pt;")),HU.attrs("align","center","width","15%")));
	sb.append("</tr></table>");


        List<Metadata> metadataList =
            getMetadataManager().findMetadata(request, entry, "missing_agency",  true);

	if(metadataList!=null && metadataList.size()>0) {
	    Metadata contact = metadataList.get(0);
	    sb.append("Contact: ");
	    sb.append(contact.getAttr2());
	    sb.append("&nbsp;@&nbsp;");
	    String phone=contact.getAttr(8);
	    if(stringDefined(phone)) 
		sb.append(PhoneUtils.formatPhone(phone));
	    String email =contact.getAttr(9);
	    if(stringDefined(email)) {
		sb.append(HU.space(1));
		sb.append("<a href=mailto:"+ email+">" + email +"</a>");
	    }
	}
	sb.append("</div>");

	StringBuilder info = new StringBuilder();
	info.append("<table class=formtable>");

	HU.formEntry(info,msgLabel("Missing From"),
		     findColumn("missing_city").formatValue(request, entry, values) +" " +
		     findColumn("missing_state").formatValue(request, entry, values));

	Date missingDate = DateHandler.checkDate((Date) entry.getValue("date_missing"));
	SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy");
	if(missingDate!=null) {
	    HU.formEntry(info,msgLabel("Date Missing"), sdf.format(missingDate));
	}	    


	SimpleDateFormat yob = new SimpleDateFormat("yyyy");
	Date birthDate = DateHandler.checkDate(new Date(entry.getStartDate()));
	if(birthDate!=null)
	    HU.formEntry(info,msgLabel("Year of Birth"), yob.format(birthDate));	    
	
	if(birthDate!=null && missingDate!=null)
	    HU.formEntry(info,msgLabel("Age went Missing"),
			 ""+DateHandler.getYearsBetween(birthDate,missingDate));

	HU.formEntry(info,msgLabel("Sex"),   findColumn("biological_sex").formatValue(request, entry, values));   
	HU.formEntry(info,msgLabel("Height"),   findColumn("height").formatValue(request, entry, values));   
	HU.formEntry(info,msgLabel("Weight"),   findColumn("weight").formatValue(request, entry, values));
	HU.formEntry(info,msgLabel("Eye Color"), findColumn("left_eye_color").formatValue(request, entry, values));
	HU.formEntry(info,msgLabel("Hair Color"), findColumn("hair_color").formatValue(request, entry, values));
	HU.formEntry(info,msgLabel("Race"), findColumn("race_ethnicity").formatValue(request, entry, values));	
	String features =  findColumn("distinctive_physical_features").formatValue(request, entry, values);
	if(stringDefined(features)) 
	    HU.formEntry(info,msgLabel("Features"), features);


	String clothing =  findColumn("clothing_and_accessories").formatValue(request, entry, values);
	if(stringDefined(clothing)) 
	    HU.formEntry(info,msgLabel("Clothing"), clothing);
	
		     
		     


	info.append("</table>");	

	String image="";
	List<String> urls = new ArrayList<String>();
	getMetadataManager().getThumbnailUrls(request, entry, urls);
	if(urls.size()>0) {
	    image=HU.image(urls.get(0),HU.attrs("class","missing-flyer-image"));
	    image+=HU.div(entry.getName(),HU.cssClass("missing-flyer-name"));
	}

	sb.append("<table width=100%><tr valign=top><td width=70%>");
	sb.append(info);
	sb.append("</td><td width=30% align=center>");
	sb.append(image);

	sb.append("</tr></table>");

	HU.open(sb,"div",HU.cssClass("missing-flyer-circumstances"));
	sb.append(HU.b("CIRCUMSTANCES:"));
	HU.div(sb,getWikiManager().wikifyEntry(request, entry,entry.getDescription()),
	       HU.cssClass("missing-flyer-circumstances-details"));

	sb.append(HU.close("div"));

	sb.append("</div>");
	Date now =new Date();
	HU.div(sb,sdf.format(now),HU.cssClass("missing-flyer-footer"));

	sb.append(HU.div("",HU.cssClass("missing-page-break")));

	String photos = getWikiManager().wikifyEntry(request,entry,
						     "{{gallery decorate=false entry=\"child:entry:this;type:media_photoalbum\" message=\"\" columns=2}}");
	if(stringDefined(photos) && photos.indexOf("image")>=0)  {
	    sb.append("<div class=missing-flyer>");
	    sb.append("<div class='missing-flyer-header missing-flyer-photos'>");
	    sb.append("Photos");
	    sb.append(photos);
	    sb.append("</div>");
	    sb.append("</div>");
	}


	return new Result("",sb);
    }




}
