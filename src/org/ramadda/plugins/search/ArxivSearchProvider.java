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

package org.ramadda.plugins.search;


import org.json.*;

import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.search.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;

import org.w3c.dom.*;


import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.net.URL;
import java.net.URLConnection;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 *
 */
public class ArxivSearchProvider extends SearchProvider {

    /** _more_ */
    private static final String ID = "arxiv";

    /** _more_ */
    private static final String URL =
        "https://export.arxiv.org/api/query?search_query=all:";


    /**
     * _more_
     *
     * @param repository _more_
     */
    public ArxivSearchProvider(Repository repository) {
        super(repository, ID, "Arxiv Search");
    }



    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public String getSiteUrl() {
        return "http://www.arxiv.org";
    }


    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public String getSearchProviderIconUrl() {
        return "${root}/search/arxiv.png";
    }


    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public String getCategory() {
        return CATEGORY_SCIENCE;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param searchInfo _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public List<Entry> getEntries(Request request, SearchInfo searchInfo)
            throws Exception {

        List<Entry> entries = new ArrayList<Entry>();
        String url = URL
                     + HtmlUtils.urlEncode(request.getString(ARG_TEXT, ""));
        System.err.println(getName() + " search url:" + url);
        URLConnection connection = new URL(url).openConnection();
        connection.setRequestProperty("User-Agent", "ramadda");
        InputStream is = connection.getInputStream();
        String  xml  = IOUtil.readContents(is);
        Element root = XmlUtil.getRoot(xml);
        org.ramadda.plugins.feed.FeedTypeHandler fth =
            (org.ramadda.plugins.feed
                .FeedTypeHandler) getRepository().getTypeHandler("feed");
        Entry parent = getSynthTopLevelEntry();
        if (fth != null) {
            fth.processAtom(request, parent, entries, root);
        }

        for (Entry entry : entries) {
            entry.setIcon("/search/arxiv.png");
        }

        return entries;
    }



}
