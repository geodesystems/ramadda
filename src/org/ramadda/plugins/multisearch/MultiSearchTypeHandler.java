/*
* Copyright (c) 2008-2019 Geode Systems LLC
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
*     http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.ramadda.plugins.multisearch;


import org.ramadda.repository.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;

import org.ramadda.util.HtmlUtils;

import org.w3c.dom.*;

import ucar.unidata.util.StringUtil;

import java.util.ArrayList;

import java.util.List;


/**
 *
 *
 */
public class MultiSearchTypeHandler extends GenericTypeHandler {

    /** _more_ */
    public static String ARG_QUERY = "query";

    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public MultiSearchTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param group _more_
     * @param subGroups _more_
     * @param entries _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result getHtmlDisplay(Request request, Entry entry)
            throws Exception {
        StringBuffer sb      = new StringBuffer();
        getPageHandler().entrySectionOpen(request, entry, sb,null);

        String       formUrl = getEntryManager().getEntryURL(request, entry);
        String       query   = request.getString(ARG_QUERY, "");
        sb.append(HtmlUtils.form(formUrl, ""));
        sb.append(HtmlUtils.hidden(ARG_ENTRYID, entry.getId()));
        sb.append(msg("Search across multiple search engines"));
        sb.append(HtmlUtils.br());
        sb.append(HtmlUtils.input(ARG_QUERY, query, 40));
        sb.append(HtmlUtils.submit("Search"));
        sb.append(HtmlUtils.formClose());
        if (request.defined(ARG_QUERY)) {
            List<String> tabTitles = new ArrayList<String>();
            List<String> tabs      = new ArrayList<String>();
            String[]     urls      = {
                "Google",
                "https://www.google.com/search?hl=en&ie=ISO-8859-1&q=${query}&btnG=Search",
                "Bing",
                "https://www.bing.com/search?q=${query}&go=&form=QBLH&qs=n&sk=&sc=8-5",
                "Yahoo",
                "https://search.yahoo.com/search?p=${query}&ei=UTF-8&fr=moz35",
                "DuckDuckGo", "https://duckduckgo.com/?q=${query}",
            };
            for (int i = 0; i < urls.length; i += 2) {
                String title = urls[i];
                String url   = urls[i + 1];
                tabTitles.add(title);
                url = url.replace("${query}", query);
                StringBuffer tmp = new StringBuffer();
                tmp.append(HtmlUtils.href(url, "Go to " + title));
                tmp.append(
                    HtmlUtils.tag(
                        HtmlUtils.TAG_IFRAME,
                        HtmlUtils.attr(HtmlUtils.ATTR_SRC, url)
                        + HtmlUtils.attr(HtmlUtils.ATTR_WIDTH, "100%")
                        + HtmlUtils.attr(
                            HtmlUtils.ATTR_HEIGHT, "300"), "Need frames"));
                tabs.add(tmp.toString());
            }
            sb.append(OutputHandler.makeTabs(tabTitles, tabs, true));
        }

        getPageHandler().entrySectionClose(request, entry, sb);
        return new Result("MultiSearch", sb);
    }




}
