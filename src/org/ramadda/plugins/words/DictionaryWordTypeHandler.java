/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.words;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.WikiUtil;

import org.w3c.dom.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;


/**
 *
 *
 */
public class DictionaryWordTypeHandler extends GenericTypeHandler {
    private static int IDX=0;
    public static final int IDX_OTHER_WORD  =IDX++;
    public static final int IDX_PART_OF_SPEECH = IDX++;
    public static final int IDX_DIALECT  =IDX++;
    public static final int IDX_SOURCE = IDX++;    


    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public DictionaryWordTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }

    private String getFromLabel(Entry parentEntry) {
	if(parentEntry.getTypeHandler().isType("type_dictionary")) {
	    String from  = (String) parentEntry.getValue(getRepository().getAdminRequest(),DictionaryTypeHandler.IDX_LANGUAGE);
	    return from +" " + msg("Word");
	}
	return null;
    }

    private String getToLabel(Entry parentEntry) {
	if(parentEntry.getTypeHandler().isType("type_dictionary")) {
	    DictionaryTypeHandler dth  = (DictionaryTypeHandler) parentEntry.getTypeHandler();
	    String target = dth.getTargetLabel(parentEntry);
	    return target +" " + msg("Word");
	}
	return null;
    }

    @Override
    public String getFormLabel(Entry parentEntry, Entry entry, String arg, String dflt) {
	if(arg.equals(FIELD_NAME)) {
	    String from = getFromLabel(parentEntry);
	    if(from!=null) return from;
	}

	if(arg.equals("other_word")) {
	    String to= getToLabel(parentEntry);
	    if(to!=null) return to;
	}


        return super.getFormLabel(parentEntry,entry,arg,dflt);
    }


    @Override
    public String getInlineHtml(Request request, Entry entry)
            throws Exception {
	String wiki  = getWikiManager().wikifyEntry(request, entry, entry.getDescription());
	return getInfo(request, entry,false,true)+wiki;
    }

    public String getInfo(Request request, Entry entry, boolean includeWord, boolean includeDetails)
            throws Exception {	

	StringBuilder sb = new StringBuilder();
	sb.append(HU.formTable());
	if(includeWord) {
	    String to= getToLabel(entry.getParentEntry());
	    if(to==null)  to = "Word";
	    HU.formEntry(sb,to+":", (String) entry.getValue(request,IDX_OTHER_WORD));
	}
	String s;
	//Add the fields to the table and include the search under the parent
	addColumnToTable(request, entry,findColumn("part_of_speech"),sb,ARG_ANCESTOR,entry.getParentEntry().getId());
	addColumnToTable(request, entry,findColumn("dialect"),sb,ARG_ANCESTOR,entry.getParentEntry().getId());
	addColumnToTable(request, entry,findColumn("source"),sb,ARG_ANCESTOR,entry.getParentEntry().getId());		
	if(includeDetails) {
	    addUserSearchLink(request, entry, sb);
	}
	sb.append(HU.formTableClose());
	return sb.toString();
    }


    /**
     *
     * @param wikiUtil _more_
     * @param request _more_
     * @param originalEntry _more_
     * @param entry _more_
     * @param tag _more_
     * @param props _more_
      * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props)
            throws Exception {
        if ( !tag.equals("dictionary.word")) {
            return super.getWikiInclude(wikiUtil, request, originalEntry,
                                        entry, tag, props);
        }
	return getInfo(request, entry,true,true);

    }


}
