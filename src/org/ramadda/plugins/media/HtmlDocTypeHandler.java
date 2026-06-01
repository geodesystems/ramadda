/**
Copyright (c) 2008-2026 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.media;

import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;
import org.ramadda.util.IO;
import org.ramadda.util.WikiUtil;
import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;

import ucar.unidata.util.StringUtil;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.List;
import java.util.Hashtable;


public class HtmlDocTypeHandler extends ExtensibleGroupTypeHandler {

    private static int IDX = 0;

    public static final int IDX_STYLE = IDX++;

    public HtmlDocTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }

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

    @Override
    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props)
	throws Exception {
        if ( !tag.equals("embedhtml")) {
            return super.getWikiInclude(wikiUtil, request, originalEntry,
                                        entry, tag, props);
        }
        Column c = getColumn("embed_type");
        String style = entry.getStringValue(request,IDX_STYLE, c.getDflt());
        if (style.equals("none")) {
            return "";
        }

	StringBuffer sb = new StringBuffer();
        if (style.equals("frame")) {
            String url = null;
            if (entry.getResource().isUrl()) {
                url = entry.getResource().getPath();
            } else if (entry.isFile()) {
                url = entry.getTypeHandler().getEntryResourceUrl(request,
                        entry);
            } else {
                return "";
            }
	    HU.div(sb, HU.tag(
			      HU.TAG_IFRAME,
			      HU.attr(HU.ATTR_SRC, url)
			      + HU.attr(HU.ATTR_WIDTH, "100%")
			      + HU.attr(HU.ATTR_HEIGHT, "800px"), "Need frames"),
		   HU.style("margin:0px;margin-top:0px;border:var(--basic-border);padding:5px;"));
	    return sb.toString();

        }

        if (entry.getResource().isUrl()) {
            return "";
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
	    return sb.toString();

        }

        return "";
    }



    /*
    @Override
    public Result getHtmlDisplay(Request request, Entry entry, Entries children)
	throws Exception {

    }
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
