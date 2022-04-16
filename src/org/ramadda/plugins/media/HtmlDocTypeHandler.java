/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.media;


import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;


import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;

import ucar.unidata.util.StringUtil;


import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.List;



/**
 *
 *
 */
public class HtmlDocTypeHandler extends ExtensibleGroupTypeHandler {


    /** _more_ */
    private static int IDX = 0;

    /** _more_ */
    public static final int IDX_STYLE = IDX++;

    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public HtmlDocTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     * @param fromImport _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void initializeNewEntry(Request request, Entry entry,
                                   boolean fromImport)
            throws Exception {
        super.initializeNewEntry(request, entry, fromImport);
        File file = entry.getFile();
        if ( !file.exists()) {
            return;
        }
        try {
            InputStream fis  = getStorageManager().getFileInputStream(file);
            String      html = IOUtil.readInputStream(fis);
            String title = StringUtil.findPattern(html,
                               "<title>(.*)</title>");
            if (title != null) {
                entry.setName(title.trim());
            }
            IOUtil.close(fis);
        } catch (Exception exc) {
            System.err.println("oops:" + exc);
        }
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param subGroups _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public Result getHtmlDisplay(Request request, Entry entry,
                                 List<Entry> subGroups, List<Entry> entries)
            throws Exception {
        String style = entry.getValue(IDX_STYLE, "none");
        if (style.equals("none")) {
            return null;
        }
        if (style.equals("frame")) {
            String url = null;
            if (entry.getResource().isUrl()) {
                url = entry.getResource().getPath();
            } else if (entry.isFile()) {
                url = entry.getTypeHandler().getEntryResourceUrl(request,
                        entry);
            } else {
                return null;
            }
            StringBuffer sb = new StringBuffer();
            sb.append(
                HtmlUtils.tag(
                    HtmlUtils.TAG_IFRAME,
                    HtmlUtils.attr(HtmlUtils.ATTR_SRC, url)
                    + HtmlUtils.attr(HtmlUtils.ATTR_WIDTH, "100%")
                    + HtmlUtils.attr(
                        HtmlUtils.ATTR_HEIGHT, "800px"), "Need frames"));

            return new Result("", sb);
        }

        if (entry.getResource().isUrl()) {
            return null;
        }

        if (style.equals("embed") || style.equals("full")) {
            String content = getContent(request, entry);
            if (content == null) {
                content = "No HTML file";
            }
            String head = StringUtil.findPattern(content,
                              "(?s)<head>(.*?)</head>");
            //      System.err.println("head:" + head);
            if (head != null) {
                content = content.replaceAll("(?s)<head>(.*?)</head>", "");
                head    = head.replaceAll("(?s)<title>(.*?)</title>", "");
                request.appendHead(head);
            }
            String body = StringUtil.findPattern(content,
                              "(?s)<body>(.*?)</body>");
            if (body != null) {
                content = body;
            }
            String title = HU.href(getEntryManager().getEntryUrl(request,
                               entry), entry.getName());
            content = content.replaceAll(
                "(?s)<div *class *= *\"ramadda-page-title\"[^>]*>(.*?)</div>",
                "<div class=\"ramadda-page-title\">" + title + "</div>");
            if (true) {
                return new Result("", new StringBuilder(content));
            }

            return getEntryManager().addHeaderToAncillaryPage(request,
                    new Result(BLANK, new StringBuilder(content)));
        }

        return null;

    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param wikiTemplate _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public String getInnerWikiContent(Request request, Entry entry,
                                      String wikiTemplate)
            throws Exception {
        if ( !((entry.getValue(IDX_STYLE) + "").equals("partial"))) {
            return null;
        }

        return getContent(request, entry);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private String getContent(Request request, Entry entry) throws Exception {
        File file = entry.getFile();
        if ( !file.exists()) {
            return null;
        }
        InputStream fis  = getStorageManager().getFileInputStream(file);
        String      html = IOUtil.readInputStream(fis);
        IOUtil.close(fis);
        html = html.replace("${urlroot}",
                            getRepository().getUrlBase()).replace("${root}",
                                getRepository().getUrlBase());

        return html;
    }

}
