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

import org.w3c.dom.*;

import org.ramadda.util.WikiUtil;

import ucar.unidata.util.StringUtil;


import java.net.URL;

import java.text.SimpleDateFormat;

import java.util.Random;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;


/**
 */
public class MissingPersonTypeHandler extends ExtensibleGroupTypeHandler {

    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public MissingPersonTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }

    @Override
    public void initializeNewEntry(Request request, Entry entry,
                                   boolean fromImport)
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
	Random rand = new Random();
        int random = rand.nextInt(900) + 100;
	caseNumber=("MP-" + random)+"-"+(cnt);
	entry.setValue("case_number",caseNumber);
	getMetadataManager().addMetadataAlias(request,entry,caseNumber);
    }



    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props)
            throws Exception {
        if ( !tag.equals("missing_header")) {
	    return super.getWikiInclude(wikiUtil, request, originalEntry,
                                        entry, tag, props);
	}

	StringBuilder sb = new StringBuilder();
	sb.append(HU.importCss(".missing-header {}\n.missing-header a {text-decoration:none;}\n.missing-header1 {font-size:120%;font-weight:bold;}\n.missing-block {border:var(--basic-border); margin:10px; padding:10px;display:inline-block;}\n.missing-sub-header {font-weight:bold;}\n"));
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
	SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy");
	Date birthDate = DateHandler.checkDate(new Date(entry.getStartDate()));
	Date missingDate = DateHandler.checkDate((Date) entry.getValue("date_missing"));
	Date foundDate = DateHandler.checkDate((Date) entry.getValue("date_found"));	

	sb.append("<div style='border-top:var(--basic-border);'>");
	if(missingDate!=null) {
	    sb.append("<div class=missing-block>");
	    HU.div(sb,"Date of last contact",HU.cssClass("missing-sub-header"));
	    sb.append(sdf.format(missingDate));
	    sb.append("</div>");
	    if(birthDate!=null) {
		int years = DateHandler.getYearsBetween(birthDate,missingDate);
		sb.append("<div class=missing-block>");
		HU.div(sb,"Age when missing",HU.cssClass("missing-sub-header"));
		sb.append(years+" Years");
		sb.append("</div>");
		if(status.equals("missing")) {
		    years = DateHandler.getYearsBetween(birthDate,new Date());
		    sb.append("<div class=missing-block>");
		    HU.div(sb,"Current age",HU.cssClass("missing-sub-header"));
		    sb.append(years+" Years");
		    sb.append("</div>");
		} else if (foundDate!=null) {
		    String label1 = null;
		    String label2 = null;
		    if(status.equals("died")) {
			label1= "Date of death";
			label2 = "Age of death";
		    } else  if(status.equals("found")) {
			label1 ="Date when found";
			label2 ="Age when found";			    
		    }
			

		    if(label1!=null) {
			sb.append("<div class=missing-block>");
			HU.div(sb,label1,HU.cssClass("missing-sub-header"));
			sb.append(sdf.format(foundDate));
			sb.append("</div>");

			years = DateHandler.getYearsBetween(birthDate,foundDate);
			sb.append("<div class=missing-block>");
			HU.div(sb,label2,HU.cssClass("missing-sub-header"));
			sb.append(years+" Years");
			sb.append("</div>");
		    }
		}
	    }

	}
	sb.append("</div>");
	sb.append("</div>");

	return sb.toString();
    }




}
