/*
* Copyright (c) 2008-2018 Geode Systems LLC
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

    /** _more_ */
    public static final int IDX_WIDTH = 0;

    /** _more_ */
    public static final int IDX_HEIGHT = 1;

    /** _more_ */
    public static final int IDX_DISPLAY = 2;

    /** _more_ */
    private int idCnt = 0;

    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public GoogleMapsTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }


    /**
     * _more_
     *
     * @param path _more_
     *
     * @return _more_
     */
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
    public Result getHtmlDisplay(Request request, Entry entry)
            throws Exception {
        String  sdisplay = entry.getValue(IDX_DISPLAY, "true");
        boolean display  = (sdisplay.length() == 0)
                           ? true
                           : Misc.equals("true", sdisplay);
        if ( !display) {
            return null;
        }
        String width   = entry.getValue(IDX_WIDTH, "640");
        String height  = entry.getValue(IDX_HEIGHT, "390");
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


    /**
     * _more_
     *
     * @param args _more_
     */
    public static void main(String[] args) {
        String pattern = "^http://www.youtube.com/watch\\?v=.*";
        String url =
            "http://www.youtube.com/watch?v=sOU2WXaDEs0&feature=g-vrec";
        System.err.println(url.matches(pattern));
    }

}
