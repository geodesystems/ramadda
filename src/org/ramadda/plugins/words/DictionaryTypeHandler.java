/**
Copyright (c) 2008-2025 Geode Systems LLC
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

import org.ramadda.util.WikiUtil;
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
public class DictionaryTypeHandler extends LetterTypeHandler {



    private  static  int IDX=0;
    public static final int IDX_LANGUAGE = IDX++;
    public static final int IDX_TARGET_LANGUAGE = IDX++;    




    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public DictionaryTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param searchCriteria _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public List<Clause> assembleWhereClause(Request request,
                                            Appendable searchCriteria)
            throws Exception {
        List<Clause> where = super.assembleWhereClause(request,
						       searchCriteria);
        if ( !isDefaultHtmlOutput(request)) {
            return where;
        }

        /* if (request.defined(ARG_OUTPUT)) {
            return where;
        }
        */

        if ( !request.defined(ARG_LETTER)) {
            return where;
        }
        String letter = request.getString(ARG_LETTER, "A");
        if (letter.equals(ALL)) {
            return where;
        }
        where.add(
            Clause.or(
                Clause.like(
                    Tables.ENTRIES.COL_NAME,
                    letter.toLowerCase() + "%"), Clause.like(
                        Tables.ENTRIES.COL_NAME,
                        letter.toUpperCase() + "%")));


        return where;
    }


    public String getTargetLabel(Entry entry)  {
	Column column = findColumn("target_language");
	String target = (String) entry.getValue(getRepository().getAdminRequest(),
						DictionaryTypeHandler.IDX_TARGET_LANGUAGE);
	String label = stringDefined(target)?column.getEnumLabel(target):"";
	if(!stringDefined(label)) label = target;
	return label;
    }


    public String getDictionary(Request request, Entry group,WikiUtil wikiUtil,Hashtable props) throws Exception {
        StringBuffer sb = new StringBuffer();
	String headerLabel = (String)group.getValue(request,IDX_LANGUAGE);
	String to  =getTargetLabel(group);
	if(stringDefined(to))  headerLabel += HU.space(1) +"-&gt;"+ HU.space(1) + to;
	sb.append(HU.center(headerLabel));
        boolean canAdd = getAccessManager().canDoNew(request, group);
	if(canAdd) {
	    sb.append(HU.center(HU.href(
					HU.url(getRepository().getUrl("/entry/form"),
					       ARG_GROUP,      group.getId(),
					       ARG_TYPE, "type_dictionary_word"),
					msg("Add Word"),HU.clazz("ramadda-button"))));
	}

        List<String> letters = new ArrayList<String>();
        Hashtable<String, StringBuffer> letterToBuffer =
            new Hashtable<String, StringBuffer>();

        sb.append(HU.hr());
	sb.append(makeHeader(request,group));


	List<Entry> entries = new ArrayList<Entry>();
	getEntryManager().getChildrenEntries(request, getRepository().getHtmlOutputHandler(), group, entries);
        sb.append(
            "<style type=\"text/css\">.dictionary_word {margin:0px;margin-bottom:5px;}\n");
        sb.append(
            ".dictionary_words {margin:0px;margin-bottom:5px;}\n</style>");
        sb.append(getWikiManager().wikifyEntry(request, group,
					       ":vspace 0.5em\n+center\n+hbox\n{{display_simplesearch  inputSize=200 width=200 ancestor=this }}\n-hbox\n"
					       +
					       "+hbox\n&nbsp;&nbsp;<a href='{{root}}/search/type/type_dictionary_word?ancestor={{this}}' title='Go to search form'><i class='fas fa-magnifying-glass-arrow-right'></i></a>\n-hbox\n-center\n")); 

        if ((entries.size() == 0) && request.defined(ARG_LETTER)) {
            sb.append(
                getPageHandler().showDialogNote(
                    msg("No dictionary words found")));
        }

        for (Entry child : entries) {
            String name   = child.getName();
	    if(child.getTypeHandler().isType("type_dictionary_word")) {
		name += HU.space(1) +"-&gt;" + HU.space(1) +child.getValue(request,DictionaryWordTypeHandler.IDX_OTHER_WORD);
	    }
            String letter = "-";
            if (name.length() > 0) {
                letter = name.substring(0, 1).toUpperCase();
            }
            StringBuffer letterBuffer = letterToBuffer.get(letter);
            if (letterBuffer == null) {
                letterToBuffer.put(letter, letterBuffer = new StringBuffer());
                letters.add(letter);
                letterBuffer.append("<ul class=\"dictionary_words\">");
            }
            String href = getEntryManager().getAjaxLink(request, child, name).toString();
            letterBuffer.append(
                HU.li(href, HU.cssClass("dictionary_word")));
        }

        letters = (List<String>) Misc.sort(letters);

        for (String letter : letters) {
            StringBuffer letterBuffer = letterToBuffer.get(letter);
            letterBuffer.append("</ul>");
            sb.append("<a name=\"letter_" + letter + "\"></a>");
            sb.append(HU.h2(letter));
            sb.append(letterBuffer);
        }

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
        if ( !tag.equals("dictionary")) {
            return super.getWikiInclude(wikiUtil, request, originalEntry,
                                        entry, tag, props);
        }
	return getDictionary(request, entry,wikiUtil,props);

    }



}
