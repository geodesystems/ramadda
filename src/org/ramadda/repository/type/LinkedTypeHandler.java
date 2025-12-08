/**
   Copyright (c) 2008-2026 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.type;

import org.ramadda.repository.*;
import org.ramadda.repository.type.*;


import org.ramadda.util.Utils;
import org.ramadda.util.WikiUtil;

import org.w3c.dom.*;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

public  class LinkedTypeHandler extends ExtensibleGroupTypeHandler {
    public LinkedTypeHandler(Repository repository, Element entryNode)
	throws Exception {
        super(repository, entryNode);
    }

    public void initializeEntryFromForm(Request request, Entry entry,
                                        Entry parent, boolean newEntry)
	throws Exception {
	super.initializeEntryFromForm(request, entry,
				      parent,  newEntry);
	Entry linkedEntry = getLinkedEntry(request, entry,false);
	if(linkedEntry!=null) {
	    long date;
	    date = linkedEntry.getStartDate();
	    if(!getDateHandler().isNullDate(date)) 
		entry.setStartDate(date);
	    date = linkedEntry.getEndDate();
	    if(!getDateHandler().isNullDate(date)) 
		entry.setEndDate(date);	    

	    if(linkedEntry.isGeoreferenced(request))  {
		entry.setNorth(linkedEntry.getNorth(request));
		entry.setWest(linkedEntry.getWest(request));
		entry.setSouth(linkedEntry.getSouth(request));	    	    
		entry.setEast(linkedEntry.getEast(request));
	    }
	}
    }

    @Override
    public Entry getLinkedEntry(Request request, Entry entry) {
	return getLinkedEntry(request, entry,true);
    }

    private Entry getLinkedEntry(Request request, Entry entry,boolean passThis) {
	try {
	    String entryId = entry.getStringValue(request, "linked_entry",null);
	    if(!stringDefined(entryId)) {
		if(passThis) 
		    return entry;
		return null;
	    }
	    Entry linkedEntry =  getEntryManager().getEntry(request, entryId);
	    if(linkedEntry==null) {
		if(passThis) 
		    return entry;
		return null;
	    }
	    return linkedEntry;
	} catch(Exception exc) {
	    throw new RuntimeException(exc);
	}
    }


    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props)
	throws Exception {
        if ( !tag.equals("linked_entry")) {
            return super.getWikiInclude(wikiUtil, request, originalEntry,
                                        entry, tag, props);
        }
	String entryId = entry.getStringValue(request, "linked_entry",null);
	String wikiText;
	if(!stringDefined(entryId)) {
	    wikiText = "+section title={{name}}\n{{description wikify=true}}\nNo linked entry\n-section";
	} else {
	    wikiText = "+section title={{name}}\n";
	    if(stringDefined(entry.getDescription())) {
		wikiText+="{{description wikify=true}}\n";
	    }
	    if(entry.getBooleanValue(request,"show_breadcrumbs",false)) {
		wikiText+="+center\n{{breadcrumbs addThis=true fixed=true count=4 entry="+ entryId+"}}\n-center\n";
	    }
	    wikiText+="{{import showTitle=false entry=\"" + entryId +"\"}}\n-section";
	}
	return getWikiManager().wikifyEntry(request, entry, wikiText);
    }

}
