/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.media;


import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;


import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;
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
    public void initializeNewEntry(Request request, Entry entry,NewType newType)
            throws Exception {
        super.initializeNewEntry(request, entry, newType);
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
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public Result getHtmlDisplay(Request request, Entry entry, Entries children)
	throws Exception {
        Column c = getColumn("embed_type");
        String style = entry.getStringValue(request,IDX_STYLE, c.getDflt());
        if (style.equals("none")) {
            return null;
        }


	StringBuffer sb = new StringBuffer();
	//	sb.append("<br>");
	getPageHandler().entrySectionOpen(request,  entry,sb, "");
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

	    HU.div(sb, HU.tag(
			      HU.TAG_IFRAME,
			      HU.attr(HU.ATTR_SRC, url)
			      + HU.attr(HU.ATTR_WIDTH, "100%")
			      + HU.attr(HU.ATTR_HEIGHT, "800px"), "Need frames"),
		   HU.style("margin:10px;margin-top:0px;border:1px solid #ccc;padding:5px;"));
	    getPageHandler().entrySectionClose(request, entry, sb);
	    return  new Result("Embedded HTML Page", sb);
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
	    sb.append(content);
	    getPageHandler().entrySectionClose(request,  entry, sb);
            return getEntryManager().addHeaderToAncillaryPage(request,
							      new Result(BLANK, sb));
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
        if ( !((entry.getValue(request,IDX_STYLE) + "").equals("partial"))) {
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
