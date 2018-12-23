/*
* Copyright (c) 2008-2019 Geode Systems LLC
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
        String string      = entry.getValue(0, "");
        String width       = entry.getValue(1, "350");
        String height      = entry.getValue(2, "300");
        String orientation = entry.getValue(3, "vertical");

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
