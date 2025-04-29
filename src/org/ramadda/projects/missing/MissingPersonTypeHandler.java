/**
   Copyright (c) 2008-2025 Geode Systems LLC
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
    public void initializeNewEntry(Request request, Entry entry,NewType newType)
	throws Exception {
        super.initializeNewEntry(request, entry, newType);
	if(!isNew(newType)) return;
	String caseNumber = (String) entry.getValue(request,"case_number",null);
	if(stringDefined(caseNumber)) return;
	int cnt = getEntryUtil().getEntryCount(this);
	int base = 10;
	cnt=cnt=base+1;
        int random = new Random().nextInt(900) + 100;
	caseNumber=("MP-" + random)+"-"+(cnt);
	entry.setValue("case_number",caseNumber);
	getMetadataManager().addMetadataAlias(request,entry,caseNumber);
    }


    @Override
    public String getNameSort(Entry entry) {
	Request  request=getAdminRequest();
	return 	entry.getValue(request,"last_name") +"-" + entry.getValue(request,"first_name") +"-" + entry.getValue(request,"middle_name");
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
        if (tag.equals("missing_flyer")) {
	    StringBuilder sb = new StringBuilder();
	    makeFlyer(request, entry,sb);
	    return sb.toString();
	}

        if ( !tag.equals("missing_header")) {
	    return super.getWikiInclude(wikiUtil, request, originalEntry, entry, tag, props);
	}

	boolean forSearch = request.get("forsearch",Utils.getProperty(props,"forSearch",false));
	String style = Utils.getProperty(props,"style","");
	StringBuilder sb = new StringBuilder();
	linkCSS(request, sb, getRepository().getHtdocsUrl("/missing/missing.css"));
	int years;
	SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");
	Date missingDate = DateHandler.checkDate(new Date(entry.getStartDate()));
	Date birthDate = DateHandler.checkDate((Date) entry.getValue(request,"date_of_birth"));
	Date foundDate = DateHandler.checkDate((Date) entry.getValue(request,"date_found"));	
	String status=(String)entry.getValue(request,"status","");	
	String clazz="missing-block missing-status-"+ status; 
	String image="";
	if(Utils.getProperty(props,"includeImage",false)) {
	    List<String> urls =    getMetadataManager().getThumbnailUrls(request, entry, null);
	    if(urls!=null && urls.size()>0) {
		image=HU.image(urls.get(0),HU.attrs("width","100px","loading","lazy"));
	    }
	}

	if(forSearch) {
	    getHeaderLine(request, entry, sb,forSearch);
	    sb.append("\n");
	    HU.open(sb,"table",HU.cssClass("formtable"));
	    HU.formEntry(sb,msgLabel("Weight"),
			 colValue(request,entry,"weight")+
			 HU.space(1)+
			 HU.b("Height: ")+colValue(request,entry,"height"));



	    sb.append("\n");
	    HU.formEntry(sb,msgLabel("Missing From"),
			 colValue(request,entry,"missing_city")+
			 " "+
			 colValue(request,entry,"missing_state"));
	    if(missingDate!=null) {
		HU.formEntry(sb,msgLabel("Date Missing"),
			     getFieldHtml(request,  entry,  null,"startdate",false));
	    }
	    sb.append("</table>\n");	    


	    
	    StringBuilder sb2=new StringBuilder();
	    HU.open(sb2,"div",HU.attrs("style",style,"class","search-component"));
	    sb2.append(HU.div(HU.leftRight(sb.toString(),image),HU.clazz("missing-header")));
	    HU.close(sb2,"div");
	    return sb2.toString();
	}

	HU.open(sb,"div",HU.attrs("class","missing-header search-component","style",style));
	getHeaderLine(request, entry, sb,forSearch);
	StringBuilder blocks = new StringBuilder();
	blocks.append("<div style='margin-top:5px;border-top:var(--basic-border);'>");
	if(image.length()>0)
	    HU.div(blocks,image,HU.attrs("class","missing-block"));
	makeBlock(blocks,clazz,"Status",Utils.applyCase(Utils.CASE_PROPER,status));
	if(missingDate!=null) {
	    String label = sdf.format(missingDate);
	    if(birthDate!=null) {
		years = DateHandler.getYearsBetween(birthDate,missingDate);
		label+="<br>" +years+" years old";
	    }
	    makeBlock(blocks,clazz,"Date last seen", label);
	    if(birthDate!=null) {
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
		    }
		}
	    }
	}
	blocks.append("</div>");
	if(forSearch) {
	    String line =  HU.leftRight(sb.toString(),image);
	    line+="\n</div>";
	    return line;
	} else {
	    blocks.append("</div>");
	    sb.append(blocks);
	}
	return sb.toString();
    }

    private void getHeaderLine(Request request, Entry entry, StringBuilder sb,boolean forSearch) throws Exception {
	String url = request.entryUrl(getRepository().URL_ENTRY_SHOW, entry);
	String nickname=(String)entry.getValue(request,"nickname");
	String status=(String)entry.getValue(request,"status","");	
	sb.append("<div class=missing-header1>\n");
	sb.append(HU.href(url));
	//	sb.append("Missing Person/#" + entry.getValue(request,"case_number","")+"<br>");
	//	sb.append(entry.getName().trim());
	String caseNumber=(String)entry.getValue(request,"case_number","");
	String name = entry.getName().trim();
	if(stringDefined(nickname)) {
	    nickname = nickname.trim();
	    if(nickname.startsWith("\"")) 
		name+=" - " + nickname;
	    else
		name+=" - \"" + nickname +"\"";
	}
	sb.append("#"+ caseNumber+": "); 
	sb.append(name);
	sb.append("<br>");
	if(forSearch) {
	    findColumn("status").formatValue(request, entry, sb, Column.OUTPUT_HTML, entry.getValues(),
					     false);
	    sb.append(", ");
	}
	Column sexColumn = findColumn("biological_sex");
        sexColumn.formatValue(request, entry, sb, Column.OUTPUT_HTML, entry.getValues(),
			      false);
	sb.append(", ");
	Column ethnicityColumn = findColumn("race_ethnicity");
        ethnicityColumn.formatValue(request, entry, sb, Column.OUTPUT_HTML, entry.getValues(),false);

	sb.append(HU.close("a"));
	HU.close(sb,"div");
    }

    public Result processEntryAction(Request request, Entry entry)
	throws Exception {
        String action = request.getString("action", "");
	if(!action.equals("flyer")) {
	    return super.processEntryAction(request, entry);
	}
	request.put("template","empty");
	StringBuilder sb = new StringBuilder();
	makeFlyer(request, entry, sb);
	return new Result("",sb);
    }
    
    private String colValue(Request request, Entry entry, String column) throws Exception {
	return findColumn(column).formatValue(request, entry, entry.getValues());
    }

    public void makeFlyer(Request request, final Entry entry,StringBuilder sb)
	throws Exception {
	final Object[] values = entry.getValues();
	final Utils.UniFunction<String,String> colValue = new Utils.UniFunction<String,String>() {
		public String call(String column) {
		    try {
			return findColumn(column).formatValue(request, entry, values);
		    } catch(Exception exc) {
			throw new RuntimeException(exc);
		    }

		}
	    };
	Utils.TriConsumer<StringBuilder,String,String>
	    fmt = new Utils.TriConsumer<StringBuilder,String,String>() {
		    public void accept(StringBuilder buff,String label,String column)  {
			try {
			    HU.formEntry(buff,msgLabel(label),colValue.call(column));
			} catch(Exception exc) {
			    throw new RuntimeException(exc);
			}
		    }
		};

	linkCSS(request, sb, getRepository().getHtdocsUrl("/missing/missing.css"));
	String agencyImage=getMetadataManager().getMetadataUrl(request, entry,"missing_agency_image");
	if(agencyImage==null) agencyImage="";
	else agencyImage=HU.image(agencyImage,HU.attrs("width","150px"));
	sb.append(HU.comment("Begin flyer"));
	HU.open(sb,"div",HU.cssClass("missing-flyer"));
	HU.open(sb,"div",HU.cssClass("issing-flyer-header"));
	sb.append("<table width=100%><tr valign=bottom>\n");
	sb.append(HU.td(agencyImage,HU.attrs("width","15%")));
	sb.append("<td width=60% align=center>\n");
	HU.div(sb,"MISSING",HU.cssClass(" missing-flyer-title "));
	HU.div(sb,"IF YOU HAVE ANY INFORMATION ABOUT","");
	HU.div(sb,entry.getName(),HU.cssClass("missing-flyer-name"));
	HU.close(sb,"td");
	String qr = getWikiManager().wikifyEntry(request, entry,"{{qrcode width=100}}");
	sb.append(HU.td(qr+HU.span("More Information",HU.style("font-size:12pt;")),HU.attrs("align","center","width","15%")));
	HU.close(sb,"tr","table");
        List<Metadata> metadataList =
            getMetadataManager().findMetadata(request, entry, "missing_agency",  true);

	if(metadataList!=null && metadataList.size()>0) {
	    Metadata contact = metadataList.get(0);
	    String phone=contact.getAttr(8);
	    String email =contact.getAttr(9);
	    sb.append("Contact: ");
	    sb.append(contact.getAttr2());
	    if(stringDefined(phone) ||stringDefined(email))  
		sb.append("&nbsp;@&nbsp;");

	    if(stringDefined(phone)) 
		sb.append(PhoneUtils.formatPhone(phone));

	    if(stringDefined(email)) {
		sb.append(HU.space(1));
		sb.append("<a href=mailto:"+ email+">" + email +"</a>");
	    }
	}
	sb.append(HU.comment("Close flyer header"));
	HU.close(sb,"div");

	StringBuilder info = new StringBuilder();
	SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy");
	HU.open(info,"table",HU.cssClass("formtable"));
	HU.formEntry(info,msgLabel("Missing From"),
		     colValue.call("missing_city") +" "+
		     colValue.call("missing_state"));

	Date missingDate = DateHandler.checkDate(new Date(entry.getStartDate()));
	if(missingDate!=null) {
	    HU.formEntry(info,msgLabel("Date Missing"), sdf.format(missingDate));
	}	    


	SimpleDateFormat yob = new SimpleDateFormat("yyyy");
	Date birthDate = DateHandler.checkDate(	(Date) entry.getValue(request,"date_missing"));
	if(birthDate!=null)
	    HU.formEntry(info,msgLabel("Year of Birth"), yob.format(birthDate));	    
	
	if(birthDate!=null && missingDate!=null)
	    HU.formEntry(info,msgLabel("Age went Missing"),
			 ""+DateHandler.getYearsBetween(birthDate,missingDate));

	fmt.accept(info,"Sex", "biological_sex");
	fmt.accept(info,"Height","height");
	fmt.accept(info,"Weight","weight");
	fmt.accept(info,"Eye Color","left_eye_color");
	fmt.accept(info,"Hair Color", "hair_color");
	fmt.accept(info,"Race", "race_ethnicity");
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

	String clothing =  colValue.call("clothing_and_accessories");
	if(stringDefined(clothing)) 
	    sb.append(HU.b(msgLabel("Clothing"))+HU.div(clothing,""));

	String features =  colValue.call("distinctive_physical_features");
	if(stringDefined(features)) 
	    sb.append(HU.b(msgLabel("Features"))+HU.div(features,""));
	sb.append(HU.b("Circumstances:"));
	sb.append(HU.div(getWikiManager().wikifyEntry(request, entry,entry.getDescription()),""));
	HU.close(sb,"div");
	HU.div(sb,sdf.format(new Date()),HU.cssClass("missing-flyer-footer"));
	HU.div(sb,"",HU.cssClass("missing-page-break"));
	String photos = getWikiManager().wikifyEntry(request,entry,
						     "{{gallery decorate=false entry=\"child:entry:this;type:media_photoalbum\" message=\"\" columns=2}}");
	if(stringDefined(photos) && photos.indexOf("img")>=0)  {
	    HU.open(sb,"div",HU.cssClass("missing-flyer"));
	    HU.open(sb,"div",HU.clazz("missing-flyer-header missing-flyer-photos"));
	    sb.append("Photos");
	    sb.append(photos);
	    HU.close(sb,"div","div");
	}

    }

}
