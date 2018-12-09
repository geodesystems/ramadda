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
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;

import org.ramadda.util.HtmlUtils;

import org.w3c.dom.*;

import ucar.unidata.util.StringUtil;

import java.util.ArrayList;
import java.util.Date;

import java.util.List;


/**
 *
 *
 */
public class CountdownTypeHandler extends GenericTypeHandler {


    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public CountdownTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }

    /** _more_ */
    private int countdownCnt = 0;

    /** _more_ */
    private String countdownHtml;

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
        if (countdownHtml == null) {
            countdownHtml = getRepository().getResource(
                "/org/ramadda/plugins/gadgets/countdown.html");
        }
        String orient  = entry.getValue(0, "");
        String howMany = entry.getValue(1, "");
        if (howMany.length() == 0) {
            howMany = "4";
        }
        StringBuffer sb = new StringBuffer(countdownHtml);
        sb.append("<table><tr><td><center>");
        sb.append(getPageHandler().formatDate(request, entry.getStartDate(),
                getEntryUtil().getTimezone(entry)));
        Date   to = new Date(entry.getStartDate());
        String id = "countdownid_" + (countdownCnt++);
        //        sb.append(HtmlUtils.cssBlock(".countdown-clock {font-size: 150%;}\n.countdown-number {color:#A94DEA;\n.countdown-label {color:#000;}\n"));
        String inner = HtmlUtils.div("",
                                     HtmlUtils.id(id)
                                     + HtmlUtils.cssClass("countdown-clock"));
        sb.append("<table><td><td>"
                  + HtmlUtils.div(inner, HtmlUtils.cssClass("countdown"))
                  + "</td></tr></table>");
        sb.append(
            HtmlUtils.script(
                "$(document).ready(function() {countdownStart("
                + HtmlUtils.squote(entry.getName()) + ","
                + HtmlUtils.squote(id) + "," + (to.getTime() / 1000) + ","
                + HtmlUtils.squote(orient) + "," + howMany + ");});\n"));
        sb.append("</center></td></tr></table>");

        return new Result("Countdown", sb);
    }




}
