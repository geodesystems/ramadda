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


import org.ramadda.util.sql.Clause;


import org.w3c.dom.*;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;



import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;



/**
 *
 *
 */
@SuppressWarnings("unchecked")
public class DictionaryTypeHandler extends ExtensibleGroupTypeHandler {

    private  static  int IDX=0;
    public static final int IDX_LANGUAGE = IDX++;
    public static final int IDX_TARGET_LANGUAGE = IDX++;    

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
    public DictionaryTypeHandler(Repository repository, Element entryNode)
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
	String target = (String) entry.getValue(DictionaryTypeHandler.IDX_TARGET_LANGUAGE);
	String label = stringDefined(target)?column.getEnumLabel(target):"";
	if(!stringDefined(label)) label = target;
	return label;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param group _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public Result getHtmlDisplay(Request request, Entry group,  Entries children)
            throws Exception {
        if ( !isDefaultHtmlOutput(request)) {
            return null;
        }


        StringBuffer sb = new StringBuffer();
        getPageHandler().entrySectionOpen(request, group, sb, null);
	String headerLabel = (String)group.getValue(IDX_LANGUAGE);
	String to  =getTargetLabel(group);
	if(stringDefined(to))  headerLabel += HU.space(1) +"-&gt;"+ HU.space(1) + to;
	sb.append(HU.center(headerLabel));

        sb.append(getWikiManager().wikifyEntry(request, group,
                group.getDescription()));
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
        sb.append("<center>");
        List<String> header    = new ArrayList<String>();
        String       theLetter = request.getString(ARG_LETTER, "");
        String[]     ltrs      = {
            "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M",
            "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z",
            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "-",ALL
        };
        String url = request.getUrl(ARG_LETTER);
        for (String letter : ltrs) {
            if (letter.equals(theLetter)) {
                header.add(HU.b(letter));
            } else {
                header.add(HU.href(url + "&" + ARG_LETTER + "="
                                          + letter, letter));
            }
        }
        sb.append(StringUtil.join("&nbsp;|&nbsp;", header));
        sb.append("</center>");

	List<Entry> entries = children.get();
        sb.append(
            "<style type=\"text/css\">.dictionary_word {margin:0px;margin-bottom:5px;}\n");
        sb.append(
            ".dictionary_words {margin:0px;margin-bottom:5px;}\n</style>");
        sb.append(getWikiManager().wikifyEntry(request, group,
					       ":vspace 0.5em\n+center\n{{display_simplesearch  ancestor=this }}\n-center\n")); 

        if ((entries.size() == 0) && request.defined(ARG_LETTER)) {
            sb.append(
                getPageHandler().showDialogNote(
                    msg("No dictionary words found")));
        }

        for (Entry entry : entries) {
            String name   = entry.getName();
	    if(entry.getTypeHandler().isType("type_dictionary_word")) {
		name += HU.space(1) +"-&gt;" + HU.space(1) +entry.getValue(DictionaryWordTypeHandler.IDX_OTHER_WORD);
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
            String href = getEntryManager().getAjaxLink(request, entry, name).toString();
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

        getPageHandler().entrySectionClose(request, group, sb);

        return new Result(msg("Dictionary"), sb);
    }




}
