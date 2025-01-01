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
import org.ramadda.util.seesv.Seesv;
import org.ramadda.util.IO;

import org.ramadda.util.WikiUtil;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.IOUtil;
import org.ramadda.util.sql.Clause;
import org.w3c.dom.*;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;



import java.io.*;
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
public class InventoryTypeHandler extends ExtensibleGroupTypeHandler {

    public InventoryTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }


    @Override
    public Result getHtmlDisplay(Request request, Entry group,
                                 Entries children)
	throws Exception {
	return getHtmlDisplay(request,group);
    }

    private List<String> getBarcodes(Request request, Entry entry)          throws Exception {
	String barcodes = entry.getStringValue(request,"barcodes","");
	return Utils.split(barcodes,"\n",true,true);
    }


    @Override
    public Result getHtmlDisplay(Request request, Entry entry)
            throws Exception {
	StringBuilder sb = new StringBuilder();
	boolean canEdit = getAccessManager().canDoEdit(request, entry);
	getPageHandler().entrySectionOpen(request, entry, sb, "");
	sb.append(getWikiManager().wikifyEntry(request, entry, entry.getDescription()));
	String file = request.getUploadedFile(ARG_FILE);
	List<String> barcodeList = getBarcodes(request,entry);
	if(canEdit && file!=null) {
	    String contents = IOUtil.readContents(new FileInputStream(file));
	    List<String> lines = Utils.split(contents,"\n",true,true);
	    lines.remove(0);
	    HashSet seen = Utils.makeHashSet(barcodeList);
	    int count=0;
	    for(String barcode: lines) {
		if(seen.contains(barcode)) continue;
		barcodeList.add(barcode);
		seen.add(barcode);
		count++;
	    }
	    sb.append(getPageHandler().showDialogNote(count +" barcodes added"));
	    entry.setValue("barcodes",Utils.join(barcodeList,"\n"));
	    getEntryManager().updateEntry(request, entry);
	}


        String formUrl   = request.makeUrl(getRepository().URL_ENTRY_SHOW);


	if(canEdit) {
	    sb.append(HU.uploadForm(formUrl,""));
	    sb.append(HU.formTable());
	    sb.append(HU.hidden(ARG_ENTRYID, entry.getId()));
	    sb.append(
		      HU.formEntry(
				   msgLabel("Bar Code File"),
				   HU.fileInput(ARG_FILE, HU.SIZE_60) +HU.space(2) +
				   HU.submit("Add Bar Codes", ARG_FILE)));
	    sb.append(HU.formTableClose());
	    sb.append(HU.formClose());
	}

        sb.append(HU.uploadForm(formUrl,""));
        sb.append(HU.hidden(ARG_ENTRYID, entry.getId()));
        sb.append(HU.submit("Run Inventory", "check"));
	sb.append(HU.space(2));
	sb.append(HU.labeledCheckbox("download","true",false,"Download CSV"));
        sb.append(HU.formClose());

	
	if(request.exists("check")) {
	    boolean download = request.get("download",false);
	    List<Entry> entries = getEntryManager().getEntriesWithType(request, "type_archive_book");
	    HashSet seen = Utils.makeHashSet(barcodeList);
	    List<String> hit = new ArrayList<String>();
	    List<String> miss = new ArrayList<String>();	    
	    StringBuilder nomatch = new StringBuilder();
	    List<String> matched = new ArrayList<String>();
	    StringBuilder csv = new StringBuilder();
	    csv.append("barcode,entry,url\n");
	    for(Entry book: entries) {
		String barcode = book.getStringValue(request, "barcode","");
		String link = getEntryManager().getEntryLink(request, book,barcode +" -- " + book.getName(),true,"");
		if(seen.contains(barcode)) {
		    matched.add(barcode);
		    hit.add(HU.div(link));
		} else {
		    if(download) {
			csv.append(barcode);
			csv.append(",");
			csv.append(Seesv.cleanColumnValue(book.getName()));
			csv.append(",");
			csv.append(request.getAbsoluteUrl(getEntryManager().getEntryURL(request, book)));
			csv.append("\n");
		    }
		    miss.add(HU.div(link));
		}
	    }
	    if(download) {
		request.setReturnFilename("inventory.csv",false);
		return new Result("",csv,"text/csv");
	    }
	    seen = Utils.makeHashSet(matched);
	    for(String barcode: barcodeList) {
		if(seen.contains(barcode)) continue;
		nomatch.append(HU.div(barcode));
	    }

	    sb.append("<hr>");
	    sb.append("# Missing Books: "+ miss.size() +HU.space(2) +
		      "# Found Books: "+ hit.size());
	    List<String>titles = new ArrayList<String>();
	    List<String>contents = new ArrayList<String>();	    


	    if(miss.size()>0) {
		titles.add("Missing Books");
		contents.add(Utils.join(miss,""));
	    }
	    if(hit.size()>0) {
		titles.add("Found Books");
		contents.add(Utils.join(hit,""));
	    }
	    if(nomatch.length()>0) {
		titles.add("Missing Barcodes");
		contents.add(nomatch.toString());
	    }
	    HU.makeTabs(sb, titles, contents);
	} else {
	    if(barcodeList.size()==0) {
		sb.append("No bar codes");
	    } else {
		sb.append("<hr>");
		sb.append(HU.b("Bar Codes - #" +barcodeList.size()));
		sb.append("<pre style='width:300px;max-height:600px;overflow-y:auto;'>");
		sb.append(Utils.join(barcodeList,"\n"));
		sb.append("</pre>");
	    }
	}



	getPageHandler().entrySectionClose(request, entry, sb);
	return new Result(entry.getName() +" - Inventory",sb);
    }

}
