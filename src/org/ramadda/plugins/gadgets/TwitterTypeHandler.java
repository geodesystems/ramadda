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

package org.ramadda.plugins.gadgets;


import org.ramadda.repository.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;

import org.ramadda.util.Json;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.WikiUtil;

import org.w3c.dom.*;

import ucar.unidata.util.StringUtil;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Date;

import java.util.List;
import org.json.*;


/**
 *
 *
 */
public class TwitterTypeHandler extends GenericTypeHandler {

    private static final String URL = "https://publish.twitter.com/oembed?url=";


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



    @Override
    public void initializeNewEntry(Request request, Entry entry) throws Exception {
        JSONObject obj = Json.readUrl(URL+entry.getResource().getPath());
        String html = obj.optString("html","");
        String name =  StringUtil.findPattern(html,"(Tweets\\s+by\\s+[^<]+)<");
        if(name!=null) {
            entry.setName(name);
            return;
        }
        entry.setName("Tweet: " + obj.optString("author_name",entry.getName()));
    }


    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props)
            throws Exception {
        if ( !tag.equals("tweet")) {
            return super.getWikiInclude(wikiUtil, request, originalEntry,
                                        entry, tag, props);
        }
        JSONObject obj = Json.readUrl(URL+entry.getResource().getPath());
        System.err.println(obj);
        String html = obj.optString("html", "");
        return html;

    }




}
