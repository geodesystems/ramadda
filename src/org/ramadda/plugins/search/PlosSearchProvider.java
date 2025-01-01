/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.search;


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


import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * Proxy that searches wolfram
 *
 */
public class PlosSearchProvider extends SearchProvider {



    /** _more_ */
    public static final String TAG_RESPONSE = "response";

    /** _more_ */
    public static final String TAG_RESULT = "result";

    /** _more_ */
    public static final String TAG_DOC = "doc";

    /** _more_ */
    public static final String TAG_STR = "str";

    /** _more_ */
    public static final String TAG_DATE = "date";

    /** _more_ */
    public static final String TAG_ARR = "arr";

    /** _more_ */
    public static final String TAG_FLOAT = "float";

    /** _more_ */
    public static final String ATTR_MAXSCORE = "maxScore";

    /** _more_ */
    public static final String ATTR_NAME = "name";

    /** _more_ */
    public static final String ATTR_NUMFOUND = "numFound";

    /** _more_ */
    public static final String ATTR_START = "start";


    /** _more_ */
    private static final String ID = "plos";

    /** _more_ */
    private static final String URL = "https://api.plos.org/search";

    /** _more_ */
    public static final String ARG_Q = "q";

    /** _more_ */
    public static final String ARG_API_KEY = "api_key";



    /**
     * _more_
     *
     * @param repository _more_
     */
    public PlosSearchProvider(Repository repository) {
        super(repository, ID, "PLoS Search");
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isEnabled() {
        return getApiKey() != null;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public String getSiteUrl() {
        return "https://www.plos.org/";
    }


    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public String getSearchProviderIconUrl() {
        return "${root}/search/plos.png";
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

        //    https://api.plos.org/search?q=title:%22Ten%20Simple%20Rules%22&api_key=2ifY1fYfC9xz33odffyX
        String      searchText = request.getString(ARG_TEXT, "");
        List<Entry> entries    = new ArrayList<Entry>();
        String      max        = request.getString("max", "100");
        String searchUrl = HtmlUtils.url(URL, "rows", max, "wt", "xml",
                                         ARG_API_KEY, getApiKey(), ARG_Q,
                                         searchText);
        if (request.get("skip", 0) > 0) {
            searchUrl += "&start=" + request.get("skip", 0);
        }

        System.err.println(getName() + " search url:" + searchUrl);
        InputStream is  = getInputStream(searchUrl);
        String      xml = IOUtil.readContents(is);
        //        System.out.println(xml);
        IOUtil.close(is);
        Entry       parent      = getSynthTopLevelEntry();
        Element     root        = XmlUtil.getRoot(xml);
        Element     result      = XmlUtil.getElement(root, TAG_RESULT);
        NodeList    docs        = XmlUtil.getElements(result, TAG_DOC);
        TypeHandler typeHandler = getLinkTypeHandler();


        String[]    sites       = {
            ".*pone.*", "plosone", ".*pbio.*", "plosbiology", ".*ppat.*",
            "plospathogens", ".*pgen.*", "plosgenetics", ".*pmed.*",
            "plosmedicine", ".*pcbi.*", "ploscompbiol", ".*pntd.*", "plosntds"
        };
        /*
          https://journals.plos.org/plosbiology/article?id=10.1371/journal.pbio.1002252
         */
        /*<response>
  <result
     maxScore="14.289115"
     name="response"
     numFound="57"
     start="0">
    <doc>
      <str name="id">10.1371/journal.pcbi.0020110</str>
      <str name="journal">PLoS Computational Biology</str>
      <str name="eissn">1553-7358</str>
      <date name="publication_date">2006-09-29T00:00:00Z</date>
      <str name="article_type">Editorial</str>
      <arr name="author_display">
        <str>Philip E Bourne</str>
        <str>Alon Korngreen</str>
      </arr>
      <arr name="abstract">
        <str/>
      </arr>
      <str name="title_display">Ten Simple Rules for Reviewers</str>
      <float name="score">14.289115</float>
    </doc>
        */

        for (int childIdx = 0; childIdx < docs.getLength(); childIdx++) {
            Element       doc      = (Element) docs.item(childIdx);
            NodeList      nodeList = XmlUtil.getElements(doc);
            String        id       = getAttr(nodeList, "id", "");
            String        name     = getAttr(nodeList, "title_display", id);
            StringBuilder desc     = new StringBuilder();
            String        url      = null;
            for (int i = 0; i < sites.length; i += 2) {
                if (id.matches(sites[i])) {
                    url = "https://journals.plos.org/" + sites[i + 1]
                          + "/article?id=" + id;
                }
            }

            Element abs = XmlUtil.findElement(nodeList, ATTR_NAME,
                              "abstract");
            if (abs != null) {
                String str = XmlUtil.getGrandChildText(abs, TAG_STR);
                str = str.replaceAll("\n", "<br>");
                desc.append(str);
                desc.append("<br>");
            }

            desc.append(HtmlUtils.p());

            String journal = getAttr(nodeList, "journal", null);
            if (journal != null) {
                desc.append("<b>Journal:</b> " + journal + "<br>");
            }

            String article_type = getAttr(nodeList, "article_type", null);
            if (article_type != null) {
                desc.append("<b>Article Type:</b> " + article_type + "<br>");
            }

            String eissn = getAttr(nodeList, "eissn", null);
            if (eissn != null) {
                desc.append("<b>Eissn:</b> " + eissn + "<br>");
            }

            Entry newEntry = new Entry(Repository.ID_PREFIX_SYNTH + getId()
                                       + TypeHandler.ID_DELIMITER
                                       + id, typeHandler);
            newEntry.setIcon("/search/plos.png");
            entries.add(newEntry);


            Element authors = XmlUtil.findElement(nodeList, ATTR_NAME,
                                  "author_display");

            if (authors != null) {
                NodeList strs = XmlUtil.getElements(authors, TAG_STR);
                for (int i = 0; i < strs.getLength(); i++) {
                    String author =
                        XmlUtil.getChildText((Element) strs.item(i));
                    Metadata metadata =
                        new Metadata(getRepository().getGUID(),
                                     newEntry.getId(), getMetadataManager().findType("metadata_author"),
                                     false, author, null, null, null, null);
                    getMetadataManager().addMetadata(request,newEntry, metadata);
                }
            }


            Date   dttm     = new Date();
            Date   fromDate = dttm,
                   toDate   = dttm;

            String date     = getAttr(nodeList, "publication_date", null);
            if (date != null) {
                dttm = fromDate = toDate = Utils.parseDate(date);
            }
            Resource resource = (url != null)
                                ? new Resource(new URL(url))
                                : new Resource("");
            newEntry.initEntry(name, makeSnippet(desc.toString()), parent,
                               getUserManager().getLocalFileUser(), resource,
                               "", Entry.DEFAULT_ORDER, dttm.getTime(),
                               dttm.getTime(), fromDate.getTime(),
                               toDate.getTime(), null);
            getEntryManager().cacheSynthEntry(newEntry);
        }

        return entries;
    }

    /**
     * _more_
     *
     * @param nodeList _more_
     * @param id _more_
     * @param dflt _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private String getAttr(NodeList nodeList, String id, String dflt)
            throws Exception {
        Element e = XmlUtil.findElement(nodeList, ATTR_NAME, id);
        if (e == null) {
            return dflt;
        }

        return XmlUtil.getChildText(e);
    }


}
