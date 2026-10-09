/**
Copyright (c) 2008-2026 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.words;


import org.ramadda.repository.*;
import org.ramadda.repository.database.Tables;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;

import org.ramadda.util.sql.SqlUtil;


import org.ramadda.util.sql.Clause;
import java.sql.ResultSet;
import java.sql.Statement;


import org.w3c.dom.*;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;



import java.util.ArrayList;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.List;

import org.ramadda.util.TTLCache;



/**
 *
 *
 */
@SuppressWarnings("unchecked")
public class LetterTypeHandler extends ExtensibleGroupTypeHandler {

    //5 minute cache
    private TTLCache<String, List<String>> letterCache =
	new TTLCache<String,List<String>>(5*60*1000);


    public static final String ARG_LETTER = "letter";
    public static final String ARG_SOURCE="source";
    public static String ALL = "all";


    public LetterTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }


    public int getDefaultQueryLimit(Request request, Entry entry) {
        if (request.defined(ARG_OUTPUT)) {
            return super.getDefaultQueryLimit(request, entry);
        }

        return 1000;
    }


    public String makeHeader(Request request,Entry group,List<String> sources) throws Exception {
	String delimiter = "&nbsp;|&nbsp;";
	StringBuilder sb = new StringBuilder();
        List<String> header    = new ArrayList<String>();
        String url = request.getUrl(ARG_LETTER,ARG_SOURCE);
        String       theSource = request.getString(ARG_SOURCE, null);
        String       theLetter = request.getString(ARG_LETTER, null);
	boolean haveSources = sources!=null && sources.size()>1;
	if(haveSources) {
	    StringBuilder sourceSB = new StringBuilder();
	    String argSource = request.getString(ARG_SOURCE,"");
	    //	    sourceSB.append(HU.bold("Sources:&nbsp;" ));
	    int cnt=0;
	    for(String source: sources) {
		if(cnt>0) {
		    sourceSB.append(delimiter);
		}
		if(source.equals(argSource)) {
		    sourceSB.append(HU.b(source));
		} else {
		    String theUrl = HU.url(url,ARG_SOURCE,source);
		    if(theLetter!=null) {
			theUrl = HU.url(theUrl,ARG_LETTER, theLetter);
		    }
		    sourceSB.append(HU.href(theUrl, source));
		}
		cnt++;
	    }
	    String theUrl = HU.url(url);
	    if(theLetter!=null) {
		theUrl = HU.url(theUrl,ARG_LETTER, theLetter);
	    }
	    sourceSB.append(delimiter);
	    sourceSB.append(HU.href(theUrl, ALL));
	    sb.append(HU.makeShowHideBlock("By Source",sourceSB.toString(),theSource!=null));
	}


	//        sb.append("<center>");

        for (String letter : getLetters(group)) {
            if (Misc.equals(letter,theLetter)) {
                header.add(HU.b(letter));
            } else {
		String theUrl = HU.url(url,ARG_LETTER, letter);
		if(theSource!=null) {
		    theUrl = HU.url(theUrl,ARG_SOURCE, theSource);
		}
                header.add(HU.href(theUrl,letter));
            }
        }
	
	String letterHeader = StringUtil.join("&nbsp;|&nbsp;", header);
	if(haveSources) {
	    sb.append(HU.makeShowHideBlock("By Letter",letterHeader,true));
	} else {
	    sb.append(letterHeader);
	}

	//        sb.append("</center>");
	return sb.toString();
    }


    @Override
    public void childrenChanged(Entry entry,boolean isNew) {
	super.childrenChanged(entry,isNew);
	letterCache =
	    new TTLCache<String,List<String>>(5*60*1000);
    }


    public List<String> getLetters(Entry entry) throws Exception {
	Statement stmt = getDatabaseManager().select(Tables.ENTRIES.COL_NAME,
						     Misc.newList(Tables.ENTRIES.NAME),
						     Clause.eq(Tables.ENTRIES.COL_NODOT_PARENT_GROUP_ID, entry.getId()), "", -1);
	List<String> letters  = letterCache.get(entry.getId());
	if(letters!=null) {
	    return letters;
	}
	letters = new ArrayList<String>();
        try {
            SqlUtil.Iterator iter = getDatabaseManager().getIterator(stmt);
            ResultSet        results;
	    HashSet seen = new HashSet();
            while ((results = iter.getNext()) != null) {
		String name = results.getString(1);
		if(name.length()>0){
		    String ltr = name.substring(0,1).toUpperCase();
		    if(!seen.contains(ltr)) {
			seen.add(ltr);
			letters.add(ltr);
		    }
		}
	    }
        } finally {
            getRepository().getDatabaseManager().closeAndReleaseConnection(
                stmt);
        }	    
	letters = new ArrayList<String>((List<String>) Utils.sort(letters));
	letters.add(ALL);
	letterCache.put(entry.getId(),letters);
	return letters;
    }



}
