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

package org.ramadda.plugins.chat;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.OutputHandler;
import org.ramadda.repository.type.*;


import org.ramadda.util.HtmlUtils;
import org.ramadda.util.WikiUtil;
import org.w3c.dom.*;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;


/**
 *
 *
 */
public class ChatTypeHandler extends ExtensibleGroupTypeHandler {

    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public ChatTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }


    /**
     * _more_
     *
     *
     * @param wikiUtil _more_
     * @param request _more_
     * @param originalEntry _more_
     * @param tag _more_
     * @param props _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props)
            throws Exception {
        if ( !tag.equals("chat")) {
            return super.getWikiInclude(wikiUtil, request, originalEntry,
                                        entry, tag, props);
        }
        StringBuilder sb   = new StringBuilder();
        sb.append(HtmlUtils.cssLink(getPageHandler().makeHtdocsUrl("/chat/chat.css")));
        HtmlUtils.importJS(sb,
                           "https://www.gstatic.com/charts/loader.js");
        HtmlUtils.importJS(sb, getPageHandler().makeHtdocsUrl("/chat/chat.js"));
        String id = HtmlUtils.getUniqueId("chat_");
        HtmlUtils.div(sb,
                      "",
                      HtmlUtils.id(id)
                      + HtmlUtils.cssClass("ramadda-chat"));
        boolean canEdit = getAccessManager().canEditEntry(request,entry);
        sb.append(HtmlUtils.script("\nnew RamaddaChat('" + entry.getId()+"','"+ id +"'," + canEdit+");\n"));
        return sb.toString();
    }




}
