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

package org.ramadda.plugins.gadgets;


import org.ramadda.repository.*;
import org.ramadda.repository.output.OutputHandler;
import org.ramadda.repository.type.*;

import org.ramadda.util.HtmlUtils;

import org.w3c.dom.*;

import ucar.unidata.util.StringUtil;

import java.util.ArrayList;
import java.util.List;


/**
 *
 *
 */
public class FramesTypeHandler extends GenericTypeHandler {


    /** _more_ */
    public static final String LAYOUT_TABLE = "table";

    /** _more_ */
    public static final String LAYOUT_TABS = "tabs";

    /** _more_ */
    public static final String LAYOUT_ACCORDIAN = "accordian";

    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public FramesTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param group _more_
     * @param subGroups _more_
     * @param entries _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result getHtmlDisplay(Request request, Entry entry)
            throws Exception {
        StringBuffer sb       = new StringBuffer();
        String       urls     = entry.getValue(0, "");
        String       height   = entry.getValue(1, "300");
        String       layout   = entry.getValue(2, LAYOUT_TABLE);
        int          cols     = Integer.parseInt(entry.getValue(3, "1"));

        List<String> labels   = new ArrayList<String>();
        List<String> contents = new ArrayList<String>();


        for (String line : StringUtil.split(urls, "\n", true, true)) {
            if (line.startsWith("#")) {
                continue;
            }
            List<String> toks = StringUtil.splitUpTo(line, "|", 2);
            String       url;
            String       label;
            if (toks.size() == 1) {
                url   = line;
                label = line;
            } else {
                url   = toks.get(0);
                label = toks.get(1);
            }

            StringBuffer frameSB = new StringBuffer();
            frameSB.append("<div class=frames-block>");
            frameSB.append("<div class=frames-link>");
            frameSB.append(HtmlUtils.href(url, label));
            frameSB.append("</div>");
            frameSB.append("<div class=frames-frame>");
            frameSB.append(
                HtmlUtils.tag(
                    HtmlUtils.TAG_IFRAME,
                    HtmlUtils.attr(HtmlUtils.ATTR_SRC, url)
                    + HtmlUtils.attr(HtmlUtils.ATTR_WIDTH, "100%")
                    + HtmlUtils.attr(
                        HtmlUtils.ATTR_HEIGHT, height), "Need frames"));
            frameSB.append("</div>");
            frameSB.append("</div>");
            labels.add(label);
            contents.add(frameSB.toString());
        }

        sb.append(
            HtmlUtils.importCss(
                ".frames-contents {margin:10px;}\n.frames-link {padding-top:10px; padding-bottom:2px; font-size: 150%;}\n.frames-link a {color: black;}\n"));

        sb.append("<div class=frames-contents>");
        if (layout.equals(LAYOUT_TABLE)) {
            sb.append(
                "<table width=100% cellspacing=10 cellpadding=10><tr valign=top>");
            int colCnt = 0;
            for (int frameIdx = 0; frameIdx < contents.size(); frameIdx++) {
                colCnt++;
                sb.append("<td>");
                sb.append(contents.get(frameIdx));
                sb.append("</td>");
                if (colCnt >= cols) {
                    sb.append("</tr><tr valign=top>");
                    colCnt = 0;
                }

            }
            sb.append("</tr></table>");
        } else if (layout.equals(LAYOUT_TABS)) {
            sb.append(OutputHandler.makeTabs(labels, contents, false));
        } else if (layout.equals(LAYOUT_ACCORDIAN)) {
            HtmlUtils.makeAccordian(sb, labels, contents);
        } else {
            sb.append("Unknown layout:" + layout);
        }


        sb.append("</div>");

        return new Result(msg("Frames"), sb);
    }




}
