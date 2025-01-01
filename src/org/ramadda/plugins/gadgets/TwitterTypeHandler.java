/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.gadgets;


import org.json.*;


import org.ramadda.repository.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;

import org.ramadda.util.JsonUtil;
import org.ramadda.util.WikiUtil;

import org.w3c.dom.*;

import ucar.unidata.util.StringUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;

import java.util.List;


/**
 *
 *
 */
public class TwitterTypeHandler extends GenericTypeHandler {

    /**  */
    private static final String URL =
        "https://publish.twitter.com/oembed?url=";


    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public TwitterTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }



    /**
     *
     * @param request _more_
     * @param entry _more_
     * @param fromImport _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void initializeNewEntry(Request request, Entry entry,NewType newType)
            throws Exception {
        JSONObject obj  = JsonUtil.readUrl(URL + entry.getResource().getPath());
        String     html = obj.optString("html", "");
        String name = StringUtil.findPattern(html,
                                             "(Tweets\\s+by\\s+[^<]+)<");
        if (name != null) {
            entry.setName(name);

            return;
        }
        entry.setName("Tweet: "
                      + obj.optString("author_name", entry.getName()));
    }


    /**
     *
     * @param wikiUtil _more_
     * @param request _more_
     * @param originalEntry _more_
     * @param entry _more_
     * @param tag _more_
     * @param props _more_
     *  @return _more_
     *
     * @throws Exception _more_
     */
    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props)
            throws Exception {
        if ( !tag.equals("tweet")) {
            return super.getWikiInclude(wikiUtil, request, originalEntry,
                                        entry, tag, props);
        }
        StringBuilder sb = new StringBuilder();
        sb.append(
            HtmlUtils.importCss(
                ".timeline-Tweet-text {font-size:16pt !important;}"));
        JSONObject obj = JsonUtil.readUrl(URL + entry.getResource().getPath());
        System.err.println(obj);
        sb.append(obj.optString("html", ""));

        return sb.toString();
    }




}
