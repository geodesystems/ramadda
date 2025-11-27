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

    @Override
    public Entry getLinkedEntry(Request request, Entry entry) {
	try {
	    String entryId = entry.getStringValue(request, "linked_entry",null);
	    if(!stringDefined(entryId)) return entry;
	    Entry linkedEntry =  getEntryManager().getEntry(request, entryId);
	    if(linkedEntry==null) return entry;
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
