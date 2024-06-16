/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.gadgets;


import org.ramadda.repository.*;
import org.ramadda.repository.type.*;


import org.ramadda.util.HtmlUtils;


import org.w3c.dom.*;

import ucar.unidata.util.StringUtil;

import java.util.Date;
import java.util.List;


/**
 *
 *
 */
public class TwitterSearchTypeHandler extends GenericTypeHandler {



    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public TwitterSearchTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
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
    public Result getHtmlDisplay(Request request, Entry entry)
            throws Exception {

        if ( !isDefaultHtmlOutput(request)) {
            return null;
        }

        StringBuffer sb = new StringBuffer();

        sb.append(
            "Sorry, the embedded Twitter search service is no longer available");
        if (true) {
            return new Result(msg("Twitter Search"), sb);
        }

        String template = getRepository().getResource(
                              "/org/ramadda/plugins/gadgets/template.html");
        String string      = entry.getStringValue(request,0, "");
        String width       = entry.getStringValue(request,1, "350");
        String height      = entry.getStringValue(request,2, "300");
        String orientation = entry.getStringValue(request,3, "vertical");

        String html        = template;

        if (orientation.equals("horizontal")) {
            sb.append("<table><tr valign=top>");
        }
        for (String tok : StringUtil.split(string, "\n", true, true)) {
            html = html.replace("${string}", tok);
            html = html.replace("${title}", entry.getName());
            html = html.replace("${caption}", entry.getDescription());
            html = html.replace("${width}", width);
            html = html.replace("${height}", height);
            if (orientation.equals("horizontal")) {
                sb.append("<td>");
            }

            sb.append(HtmlUtils.href("http://twitter.com/#search?q="
                                     + HtmlUtils.urlEncode(tok), tok));
            sb.append(HtmlUtils.br());
            sb.append(html);
            if (orientation.equals("horizontal")) {
                sb.append("</td>");
            } else {
                sb.append(HtmlUtils.p());
            }
        }
        if (orientation.equals("horizontal")) {
            sb.append("</table>");
        }

        return new Result(msg("Twitter Search"), sb);
    }




}
