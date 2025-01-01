/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
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
        return "https://www.arxiv.org";
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
    public List<Entry> getEntries(Request request, org.ramadda.repository.util.SelectInfo searchInfo)
            throws Exception {

        List<Entry> entries = new ArrayList<Entry>();
        String url = URL
                     + HtmlUtils.urlEncode(request.getString(ARG_TEXT, ""));
	//        System.err.println(getName() + " search url:" + url);
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
