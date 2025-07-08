/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.map;

import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;

import org.ramadda.util.HtmlUtils;

import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import ucar.unidata.xml.XmlUtil;

import java.util.Date;
import java.util.List;

/**
 *
 *
 */
public class GoogleMapsTypeHandler extends GenericTypeHandler {

    public static final int IDX_WIDTH = 0;

    public static final int IDX_HEIGHT = 1;

    public static final int IDX_DISPLAY = 2;

    private int idCnt = 0;

    public GoogleMapsTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }

    public String getDefaultEntryName(String path) {
        String html  = IOUtil.readContents(path, "");
        String title = StringUtil.findPattern(html, "<title>(.*)</title>");
        if (title == null) {
            title = StringUtil.findPattern(html, "<TITLE>(.*)</TITLE>");
        }
        System.err.println("title:" + title);
        if (title != null) {
            title = title.replace("- Google Maps", "");

            return title;
        }

        return "Google Map URL";
    }

    @Override
    public Result getHtmlDisplay(Request request, Entry entry)
            throws Exception {
        String  sdisplay = entry.getStringValue(request,IDX_DISPLAY, "true");
        boolean display  = (sdisplay.length() == 0)
                           ? true
                           : Misc.equals("true", sdisplay);
        if ( !display) {
            return null;
        }
        String width   = entry.getStringValue(request,IDX_WIDTH, "640");
        String height  = entry.getStringValue(request,IDX_HEIGHT, "390");
        String baseUrl = entry.getResource().getPath();
        String url     = baseUrl;
        url = url + "&output=embed";
        url = url.replaceAll("&", "&amp;");
        String html =
            "<iframe width=\"${width}\" height=\"${height}\" frameborder=\"1\" scrolling=\"no\" marginheight=\"0\" marginwidth=\"0\" src=\"${url}\"></iframe>";

        html = html.replace("${width}", width);
        html = html.replace("${height}", height);
        html = html.replace("${url}", url);

        StringBuffer sb = new StringBuffer();

        sb.append(entry.getDescription());
        sb.append(HtmlUtils.p());
        sb.append(html);
        sb.append(HtmlUtils.br());
        sb.append(HtmlUtils.href(baseUrl, msg("Link")));

        return new Result(msg("Google Map"), sb);
    }

    public static void main(String[] args) {
        String pattern = "^http://www.youtube.com/watch\\?v=.*";
        String url =
            "http://www.youtube.com/watch?v=sOU2WXaDEs0&feature=g-vrec";
        System.err.println(url.matches(pattern));
    }

}
