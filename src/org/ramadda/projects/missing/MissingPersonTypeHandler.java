/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.projects.missing;

import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;
import org.ramadda.util.WikiUtil;
import ucar.unidata.util.StringUtil;
import org.w3c.dom.*;
import java.text.SimpleDateFormat;
import java.util.Random;
import java.util.Date;
import java.util.Hashtable;
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

	StringBuilder sb = new StringBuilder();
	if(request.getExtraProperty("missingcss")==null) {
	    request.putExtraProperty("missingcss","true");
	    linkCSS(request, sb, getRepository().getHtdocsUrl("/missing/missing.css"));
	}
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
	String sex=(String)entry.getValue("biological_sex");
	if(sex==null) sex = "unknown";
	sb.append(sex.equals("m")?"Male":sex.equals("f")?"Female":sex);
	sb.append(", ");
	sb.append(entry.getValue("race_ethnicity"));
	sb.append(HU.close("a"));
	sb.append("</div>");
	int years;
	SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");
	Date birthDate = DateHandler.checkDate(new Date(entry.getStartDate()));
	Date missingDate = DateHandler.checkDate((Date) entry.getValue("date_missing"));
	Date foundDate = DateHandler.checkDate((Date) entry.getValue("date_found"));	
	String clazz="missing-block missing-status-"+ status; 
	sb.append("<div style='margin-top:5px;border-top:var(--basic-border);'>");
	if(Utils.getProperty(props,"includeImage",false)) {
	    List<String> urls =    getMetadataManager().getThumbnailUrls(request, entry, null);
	    if(urls!=null && urls.size()>0) {
		HU.div(sb,HU.image(urls.get(0),HU.attrs("width","100px")),HU.attrs("class","missing-block"));
	    }
	}

	makeBlock(sb,clazz,"Status",Utils.applyCase(Utils.CASE_PROPER,status));
	if(missingDate!=null) {
	    String label = sdf.format(missingDate);
	    if(birthDate!=null) {
		years = DateHandler.getYearsBetween(birthDate,missingDate);
		label+="<br>" +years+" years old";
	    //		makeBlock(sb,clazz,"Age when missing", 
	    }
	makeBlock(sb,clazz,"Date last seen", label);
	    if(birthDate!=null) {
		//		int years = DateHandler.getYearsBetween(birthDate,missingDate);
		//		makeBlock(sb,clazz,"Age when missing", years+" years");
		if(status.equals("missing")) {
		    years = DateHandler.getYearsBetween(birthDate,new Date());
		    makeBlock(sb,clazz,"Current age",years+" years old");
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
			makeBlock(sb,clazz,label1,
				  sdf.format(foundDate)+"<br>"+years+" years old");

			//			makeBlock(sb,clazz,label2,years+" years old");
		    }
		}
	    }
	}
	sb.append("</div></div>");
	return sb.toString();
    }
}
