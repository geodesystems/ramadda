/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.gadgets;


import org.ramadda.repository.*;
import org.ramadda.repository.type.*;


import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.Utils;


import org.w3c.dom.*;

import ucar.unidata.util.StringUtil;

import java.util.Date;
import java.util.List;


/**
 *
 *
 */
public class StockTickerTypeHandler extends GenericTypeHandler {



    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public StockTickerTypeHandler(Repository repository, Element entryNode)
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
        StringBuffer sb = new StringBuffer();
        sb.append(entry.getDescription());
        sb.append(HtmlUtils.p());
        StringBuffer js       = new StringBuffer();
        String       symbols  = entry.getStringValue(0, "");
        String       width    = entry.getStringValue(1, "400");
        String       height   = entry.getStringValue(2, "400");
        String       interval = entry.getStringValue(3, "60");
        if ( !Utils.stringDefined(width)) {
            width = "400";
        }
        if ( !Utils.stringDefined(height)) {
            width = "400";
        }
        if ( !Utils.stringDefined(interval)) {
            interval = "60";
        }

        sb.append(
            HtmlUtils.importJS(
                "https://d33t3vvu2t2yu5.cloudfront.net/tv.js"));

        for (String line : StringUtil.split(symbols, "\n", true, true)) {
            js.append("new TradingView.widget(");
            js.append(JsonUtil.mapAndQuote(Utils.makeListFromValues("symbol", line, "width", width,
                                       "height", height, "interval",
                                       interval, "timezone", "exchange",
                                       "theme", "White", "style", "2",
                                       "toolbar_bg", "#f1f3f6",
                                       "hide_top_toolbar", "true",
                                       "allow_symbol_change", "true",
                                       "hideideas", "true",
                                       "show_popup_button", "false",
                                       "popup_width", "1000", "popup_height",
							  "650")));
            js.append(");\n");
        }
        sb.append(HtmlUtils.script(js.toString()));

        return new Result(msg("Stock ticker"), sb);
    }




}
