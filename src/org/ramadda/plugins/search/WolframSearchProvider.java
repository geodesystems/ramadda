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


import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * Proxy that searches wolfram
 *
 */
@SuppressWarnings("unchecked")
public class WolframSearchProvider extends SearchProvider {


    /** _more_ */
    public static final String TAG_QUERYRESULT = "queryresult";

    /** _more_ */
    public static final String TAG_POD = "pod";

    /** _more_ */
    public static final String TAG_SUBPOD = "subpod";

    /** _more_ */
    public static final String TAG_PLAINTEXT = "plaintext";

    /** _more_ */
    public static final String TAG_IMG = "img";

    /** _more_ */
    public static final String TAG_STATES = "states";

    /** _more_ */
    public static final String TAG_STATE = "state";

    /** _more_ */
    public static final String TAG_INFOS = "infos";

    /** _more_ */
    public static final String TAG_INFO = "info";

    /** _more_ */
    public static final String TAG_LINK = "link";

    /** _more_ */
    public static final String TAG_STATELIST = "statelist";

    /** _more_ */
    public static final String TAG_ASSUMPTIONS = "assumptions";

    /** _more_ */
    public static final String TAG_ASSUMPTION = "assumption";

    /** _more_ */
    public static final String TAG_VALUE = "value";

    /** _more_ */
    public static final String TAG_SOURCES = "sources";

    /** _more_ */
    public static final String TAG_SOURCE = "source";

    /** _more_ */
    public static final String ATTR_DATATYPES = "datatypes";

    /** _more_ */
    public static final String ATTR_ERROR = "error";

    /** _more_ */
    public static final String ATTR_HOST = "host";

    /** _more_ */
    public static final String ATTR_ID = "id";

    /** _more_ */
    public static final String ATTR_NUMPODS = "numpods";

    /** _more_ */
    public static final String ATTR_PARSETIMEDOUT = "parsetimedout";

    /** _more_ */
    public static final String ATTR_PARSETIMING = "parsetiming";

    /** _more_ */
    public static final String ATTR_RECALCULATE = "recalculate";

    /** _more_ */
    public static final String ATTR_RELATED = "related";

    /** _more_ */
    public static final String ATTR_SERVER = "server";

    /** _more_ */
    public static final String ATTR_SUCCESS = "success";

    /** _more_ */
    public static final String ATTR_TIMEDOUT = "timedout";

    /** _more_ */
    public static final String ATTR_TIMEDOUTPODS = "timedoutpods";

    /** _more_ */
    public static final String ATTR_TIMING = "timing";

    /** _more_ */
    public static final String ATTR_VERSION = "version";

    /** _more_ */
    public static final String ATTR_NUMSUBPODS = "numsubpods";

    /** _more_ */
    public static final String ATTR_POSITION = "position";

    /** _more_ */
    public static final String ATTR_SCANNER = "scanner";

    /** _more_ */
    public static final String ATTR_TITLE = "title";

    /** _more_ */
    public static final String ATTR_ALT = "alt";

    /** _more_ */
    public static final String ATTR_HEIGHT = "height";

    /** _more_ */
    public static final String ATTR_SRC = "src";

    /** _more_ */
    public static final String ATTR_WIDTH = "width";

    /** _more_ */
    public static final String ATTR_PRIMARY = "primary";

    /** _more_ */
    public static final String ATTR_COUNT = "count";

    /** _more_ */
    public static final String ATTR_INPUT = "input";

    /** _more_ */
    public static final String ATTR_NAME = "name";

    /** _more_ */
    public static final String ATTR_TEXT = "text";

    /** _more_ */
    public static final String ATTR_URL = "url";

    /** _more_ */
    public static final String ATTR_DELIMITERS = "delimiters";

    /** _more_ */
    public static final String ATTR_VALUE = "value";

    /** _more_ */
    public static final String ATTR_TEMPLATE = "template";

    /** _more_ */
    public static final String ATTR_TYPE = "type";

    /** _more_ */
    public static final String ATTR_WORD = "word";

    /** _more_ */
    public static final String ATTR_DESC = "desc";



    /** _more_ */
    private static final String ID = "wolfram";

    /** _more_ */
    private static final String URL = "http://api.wolframalpha.com/v2/query";

    /** _more_ */
    public static final String ARG_INPUT = "input";

    /** _more_ */
    public static final String ARG_APPID = "appid";



    /**
     * _more_
     *
     * @param repository _more_
     */
    public WolframSearchProvider(Repository repository) {
        super(repository, ID, "Wolfram/Alpha Search");
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
        return "http://www.wolframalpha.com/";
    }


    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public String getSearchProviderIconUrl() {
        return "${root}/search/wolfram.png";
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

        String      searchText = request.getString(ARG_TEXT, "");
        List<Entry> entries    = new ArrayList<Entry>();
        String searchUrl = HtmlUtils.url(URL, ARG_APPID, getApiKey(),
                                         ARG_INPUT, searchText);
        List<String> args = request.get("slack.args",
                                        new ArrayList<String>());
        System.err.println(getName() + " search url:" + searchUrl);
        InputStream is  = getInputStream(searchUrl);
        String      xml = IOUtil.readContents(is);
        //        System.out.println(xml);
        IOUtil.close(is);
        Entry       parent       = getSynthTopLevelEntry();
        Element     root         = XmlUtil.getRoot(xml);
        NodeList    pods         = XmlUtil.getElements(root, TAG_POD);
        TypeHandler typeHandler  = getLinkTypeHandler();

        boolean     includeText  = args.contains("-text");
        boolean     excludeMatch = args.contains("-exclude");


        for (int childIdx = 0; childIdx < pods.getLength(); childIdx++) {
            Element pod = (Element) pods.item(childIdx);

            String  id  = XmlUtil.getAttribute(pod, "id", "");
            Entry newEntry = new Entry(Repository.ID_PREFIX_SYNTH + getId()
                                       + TypeHandler.ID_DELIMITER
                                       + id, typeHandler);
            newEntry.setIcon("/search/wolfram.png");
            entries.add(newEntry);

            String        name    = XmlUtil.getAttribute(pod, ATTR_TITLE, "");
            StringBuilder desc    = new StringBuilder();

            NodeList      subPods = XmlUtil.getElements(pod, TAG_SUBPOD);
            for (int subPodIdx = 0; subPodIdx < subPods.getLength();
                    subPodIdx++) {
                Element subPod = (Element) subPods.item(subPodIdx);
                String plainText = XmlUtil.getGrandChildText(subPod,
                                       TAG_PLAINTEXT);
                if (Utils.stringDefined(plainText)) {
                    if (includeText) {
                        desc.append(HtmlUtils.pre(plainText));
                    }
                }
                Element img = XmlUtil.findChild(subPod, TAG_IMG);
                if (img != null) {
                    String imgUrl = XmlUtil.getAttribute(img, ATTR_SRC, "");
                    Metadata metadata =
                        new Metadata(getRepository().getGUID(),
                                     newEntry.getId(),
                                     getMetadataManager().findType(ContentMetadataHandler.TYPE_THUMBNAIL),
                                     false, imgUrl, "image", null, null,
                                     null);
                    getMetadataManager().addMetadata(request,newEntry, metadata);
                    desc.append(XmlUtil.toString(img, false));
                    desc.append(HtmlUtils.br());
                }
            }

            String itemUrl = null;
            for (Element link :
                    (List<Element>) XmlUtil.findDescendants(pod, TAG_LINK)) {
                String url  = XmlUtil.getAttribute(link, ATTR_URL, "");
                String text = XmlUtil.getAttribute(link, ATTR_TEXT, url);
                if (itemUrl != null) {
                    itemUrl = url;
                }
                desc.append(HtmlUtils.br());
                desc.append(HtmlUtils.href(url, text));
                desc.append(HtmlUtils.br());
                desc.append("\n");
            }


            if (excludeMatch) {
                String s = searchText.toLowerCase();
                String d = desc.toString().toLowerCase();
                String n = name.toLowerCase();
                //TODO - make this better
                if (d.indexOf(s) >= 0) {
                    continue;
                }
                if (n.indexOf(s) >= 0) {
                    continue;
                }
            }

            Date     dttm     = new Date();
            Date     fromDate = dttm,
                     toDate   = dttm;

            Resource resource = new Resource("");
            newEntry.initEntry(name, desc.toString(), parent,
                               getUserManager().getLocalFileUser(), resource,
                               "", Entry.DEFAULT_ORDER, dttm.getTime(),
                               dttm.getTime(), fromDate.getTime(),
                               toDate.getTime(), null);
            getEntryManager().cacheSynthEntry(newEntry);
        }

        return entries;
    }



}
