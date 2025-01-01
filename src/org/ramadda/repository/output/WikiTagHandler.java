/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.output;


import org.ramadda.repository.Entry;
import org.ramadda.repository.Request;

import org.ramadda.util.WikiUtil;

import java.util.Hashtable;
import java.util.List;


/**
 * Provides wiki text processing services
 */
@SuppressWarnings("unchecked")
public interface WikiTagHandler {

    /**
     *
     * @param tagHandlers _more_
     */
    public void initTags(Hashtable<String, WikiTagHandler> tagHandlers);

    /**
       This is called by WikiManager in its processWikiTags method
       the tags list should contain a tag (String) and a Json list:
       [
       {"label":"Tag properties"},
       {"p":"tt", "ex":"Entry name"},
       {"p":"link","ex":true}]
     */
    public void addTagDefinition(List<String>  tags);

    /**
     *
     * @param wikiUtil _more_
     * @param request _more_
     * @param originalEntry _more_
     * @param entry _more_
     * @param theTag _more_
     * @param props _more_
     * @param remainder _more_
     *  @return _more_
     *
     * @throws Exception _more_
     */
    public String handleTag(WikiUtil wikiUtil, Request request,
                            Entry originalEntry, Entry entry, String theTag,
                            Hashtable props, String remainder)
     throws Exception;



}
