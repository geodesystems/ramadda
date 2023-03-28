/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.output;


import org.ramadda.repository.Entry;
import org.ramadda.repository.Request;

import org.ramadda.util.WikiUtil;

import java.util.Hashtable;


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
