/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.faq;


import org.ramadda.repository.Entry;
import org.ramadda.repository.Repository;
import org.ramadda.repository.Request;
import org.ramadda.repository.Result;
import org.ramadda.repository.type.ExtensibleGroupTypeHandler;
import org.ramadda.util.HtmlUtils;

import org.w3c.dom.Element;


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;


/**
 *
 *
 */
@SuppressWarnings("unchecked")
public class FaqTypeHandler extends ExtensibleGroupTypeHandler {


    /** the FAQ type id */
    public static String TYPE_FAQ = "faq";

    /**
     * Create a new FaqTypeHandler
     *
     * @param repository  the Repository
     * @param entryNode   the Entry node XML
     *
     * @throws Exception  problems creating handler
     */
    public FaqTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }


    /**
     * Get the HTML display for this type
     *
     * @param request  the Request
     * @param group    the group
     *
     * @return  the Result
     *
     * @throws Exception  problem getting the HTML
     */
    @Override
    public Result getHtmlDisplay(Request request, Entry group,   Entries children)
            throws Exception {
        if ( !isDefaultHtmlOutput(request)) {
            return null;
        }

        StringBuffer sb = new StringBuffer();
        getPageHandler().entrySectionOpen(request, group, sb, null);
        boolean canAdd = getAccessManager().canDoNew(request, group);

        if (canAdd) {
            sb.append(
                HU.href(
                    HU.url(request
                        .entryUrlWithArg(
                            getRepository().URL_ENTRY_FORM, group,
                            ARG_GROUP), ARG_TYPE,
                                        FaqEntryTypeHandler.TYPE_FAQENTRY), HU
                                            .img(getRepository()
                                                .getIconUrl(ICON_NEW), msg(
                                                    "New FAQ Question"))));
            sb.append(HU.SPACE);
        }



        sb.append(getWikiManager().wikifyEntry(request, group,
                group.getDescription()));
        Hashtable<String, StringBuffer> catQuestionMap =
            new Hashtable<String, StringBuffer>();
        Hashtable<String, StringBuffer> catAnswerMap = new Hashtable<String,
                                                           StringBuffer>();
        List cats = new ArrayList();
        sb.append(
            "<style type=\"text/css\">.faq_question {margin:0px;margin-bottom:5px;}\n.faq_question a {text-decoration:none;}\n</style>");
        for (Entry entry : children.get()) {
            String cat = "General";
            if (entry.getType().equals(FaqEntryTypeHandler.TYPE_FAQENTRY)) {
                cat = (String) entry.getStringValue(request,0, cat);
                if (cat == null) {
                    cat = "General";
                }
            }
            StringBuffer catQuestionSB = catQuestionMap.get(cat);
            StringBuffer catAnswerSB   = catAnswerMap.get(cat);
            if (catQuestionSB == null) {
                catQuestionSB = new StringBuffer();
                catQuestionSB.append("<ol>");
                catQuestionMap.put(cat, catQuestionSB);
                catAnswerSB = new StringBuffer();
                catAnswerSB.append("<ol>");
                catAnswerMap.put(cat, catAnswerSB);
                cats.add(cat);
            }
            catQuestionSB.append("<li class=\"faq_question\">");
            boolean includeLink = group.getBooleanValue(request,0, true);
            if (canAdd || includeLink) {
                String link =
                    HU.href(getEntryManager().getEntryURL(request, entry),
                            HU.img(getRepository().getIconUrl(ICON_ENTRY),
                                   msg("View entry details")));
                //            catQuestionSB.append(" ");
                catQuestionSB.append(link);
                catQuestionSB.append(" ");
            }
            catQuestionSB.append(
                HU.mouseClickHref(
                    "HtmlUtils.scrollToAnchor('" + entry.getId() + "')",
                    entry.getName(), ""));

            catAnswerSB.append("<a name=" + entry.getId() + "></a>");
            catAnswerSB.append("<li class=\"faq_question\">");
            catAnswerSB.append(" ");
            catAnswerSB.append(HU.b(entry.getName()));
            catAnswerSB.append(HU.br());
            String desc = entry.getDescription();
            desc = desc.replaceAll("\r\n\r\n", "\n<p>\n");
            catAnswerSB.append(desc);
            catAnswerSB.append(HU.p());
        }



        //sort the categories and put the blank one at the end
	cats = ucar.unidata.util.Misc.sort(cats);
        if (cats.contains("")) {
            cats.remove("");
            cats.add("");
        }
	for(int i=0;i<cats.size();i++) {
	    String cat = (String) cats.get(i);
            StringBuffer catQuestionSB = catQuestionMap.get(cat);
            catQuestionSB.append("</ol>");
            if (cats.size() > 1) {
                sb.append(HU.h2(cat));
            }
            sb.append(catQuestionSB.toString());
        }


	for(int i=0;i<cats.size();i++) {
	    String cat = (String) cats.get(i);
            StringBuffer catAnswerSB = catAnswerMap.get(cat);
            sb.append("<hr>");
            catAnswerSB.append("</ol>");
            if (cats.size() > 1) {
                sb.append(HU.h2(cat));
            }

            sb.append(catAnswerSB);
        }

        getPageHandler().entrySectionClose(request, group, sb);

        return new Result(msg("FAQ"), sb);

    }




}
