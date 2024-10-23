/**
Copyright (c) 2008-2023 Geode Systems LLC
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

    /** _more_ */
    public static final String ARG_LETTER = "letter";



    /** _more_ */
    public static String ALL = "all";

    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public LetterTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     */
    public int getDefaultQueryLimit(Request request, Entry entry) {
        if (request.defined(ARG_OUTPUT)) {
            return super.getDefaultQueryLimit(request, entry);
        }

        return 1000;
    }


    public String makeHeader(Request request,Entry group) throws Exception {
	StringBuilder sb = new StringBuilder();
        sb.append("<center>");
        List<String> header    = new ArrayList<String>();
        String       theLetter = request.getString(ARG_LETTER, "");
        String url = request.getUrl(ARG_LETTER);
        for (String letter : getLetters(group)) {
            if (letter.equals(theLetter)) {
                header.add(HU.b(letter));
            } else {
                header.add(HU.href(url + "&" + ARG_LETTER + "="
                                          + letter, letter));
            }
        }
        sb.append(StringUtil.join("&nbsp;|&nbsp;", header));
        sb.append("</center>");
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
