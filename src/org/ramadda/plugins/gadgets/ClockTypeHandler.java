/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
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
import java.util.TimeZone;


/**
 *
 *
 */
public class ClockTypeHandler extends GenericTypeHandler {


    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public ClockTypeHandler(Repository repository, Element entryNode)
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
        //        String orient = entry.getStringValue(request,0,"");
        String timezone       = entry.getStringValue(request,0, "");
        int    timezoneOffset = 0;
        if (timezone.length() > 0) {
            TimeZone t = TimeZone.getTimeZone(timezone);
            timezoneOffset = t.getOffset(new Date().getTime()) / 1000 / 3600;
        }
        StringBuffer sb    = new StringBuffer();
        String       title = entry.getName();
        sb.append(HtmlUtils.importCss(".gadgets-clock {max-height:200px;}"));
        sb.append("<div class=gadgets-clock>");
        sb.append(
            "<script src=\"//www.gmodules.com/ig/ifr?url=https://www.gstatic.com/ig/modules/datetime_v3/datetime_v3.xml&amp;up_color=grey&amp;up_dateFormat=wmd&amp;up_firstDay=0&amp;up_clocks=%5B%5D&amp;up_mainClock=&amp;up_mainClockDSTOffset=&amp;up_24hourClock=true&amp;up_showWorldClocks=true&amp;up_useServerTime=false&amp;synd=open&amp;w=320&amp;h=2000&amp;" + HtmlUtils.arg("title", title, true) + "&amp;up_mainClockTimeZoneOffset=" + timezoneOffset + "&amp;lang=en&amp;country=ALL&amp;border=http%3A%2F%2Fwww.gmodules.com%2Fig%2Fimages%2F&amp;output=js\"></script>");
        sb.append("</div>");


        return new Result("Clock", sb);
    }




}
