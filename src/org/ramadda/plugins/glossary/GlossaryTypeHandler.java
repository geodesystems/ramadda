/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.glossary;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.database.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;
import org.ramadda.repository.util.SelectInfo;
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
public class GlossaryTypeHandler extends ExtensibleGroupTypeHandler {


    /** _more_ */
    public static final String ARG_LETTER = "letter";

    /** _more_ */
    public static String TYPE_GLOSSARY = "glossary";

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
    public GlossaryTypeHandler(Repository repository, Element entryNode)
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
    public Result getHtmlDisplay(Request request, Entry group,  List<Entry> children)
            throws Exception {
        if ( !isDefaultHtmlOutput(request)) {
            return null;
        }


        StringBuffer sb = new StringBuffer();
        getPageHandler().entrySectionOpen(request, group, sb, null);

        sb.append(getWikiManager().wikifyEntry(request, group,
                group.getDescription()));
        sb.append(HtmlUtils.p());

        boolean canAdd = getAccessManager().canDoNew(request, group);

        /*
        if (canAdd) {
            String label =
                HtmlUtils.img(getRepository().getIconUrl(ICON_NEW),
                              msg("New Glossary Question")) + " "
                                  + msg("Create new glossary entry");
            sb.append(HtmlUtils
                .href(HtmlUtils
                    .url(request
                         .entryUrlWithArg(getRepository().URL_ENTRY_FORM, group, ARG_GROUP), ARG_TYPE,
                                      GlossaryEntryTypeHandler
                                          .TYPE_GLOSSARYENTRY), label));
        }
        */

        List<String> letters = new ArrayList<String>();
        Hashtable<String, StringBuffer> letterToBuffer =
            new Hashtable<String, StringBuffer>();

        sb.append(HtmlUtils.p());
        sb.append("<center>");
        List<String> header    = new ArrayList<String>();
        String       theLetter = request.getString(ARG_LETTER, "");
        String[]     ltrs      = {
            "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M",
            "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z",
            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", ALL
        };
        String url = request.getUrl(ARG_LETTER);
        for (String letter : ltrs) {
            if (letter.equals(theLetter)) {
                header.add(HtmlUtils.b(letter));
            } else {
                header.add(HtmlUtils.href(url + "&" + ARG_LETTER + "="
                                          + letter, letter));
            }
        }
        sb.append(StringUtil.join("&nbsp;|&nbsp;", header));
        sb.append("</center>");

        if ((children.size() == 0) && request.defined(ARG_LETTER)) {
            sb.append(
                getPageHandler().showDialogNote(
                    msg("No glossary entries found")));
        }
        sb.append(
            "<style type=\"text/css\">.glossary_entry {margin:0px;margin-bottom:5px;}\n");
        sb.append(
            ".glossary_entries {margin:0px;margin-bottom:5px;}\n</style>");
        for (Entry entry : children) {
            String name   = entry.getName();
            String letter = "-";
            if (name.length() > 0) {
                letter = name.substring(0, 1).toUpperCase();
            }
            StringBuffer letterBuffer = letterToBuffer.get(letter);
            if (letterBuffer == null) {
                letterToBuffer.put(letter, letterBuffer = new StringBuffer());
                letters.add(letter);
                letterBuffer.append("<ul class=\"glossary_entries\">");
            }
            String href = getEntryManager().getAjaxLink(request, entry, name).toString();
            letterBuffer.append(
                HtmlUtils.li(href, HtmlUtils.cssClass("glossary_entry")));
        }

        letters = (List<String>) Misc.sort(letters);

        for (String letter : letters) {
            StringBuffer letterBuffer = letterToBuffer.get(letter);
            letterBuffer.append("</ul>");
            sb.append("<a name=\"letter_" + letter + "\"></a>");
            sb.append(HtmlUtils.h2(letter));
            sb.append(letterBuffer);
        }

        getPageHandler().entrySectionClose(request, group, sb);

        return new Result(msg("Glossary"), sb);
    }




}
