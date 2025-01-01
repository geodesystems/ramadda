/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.biz;


import org.ramadda.data.services.PointTypeHandler;
import org.ramadda.repository.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.Utils;
import org.ramadda.util.WikiUtil;

import org.w3c.dom.*;

import java.util.Hashtable;


/**
 * Reads yahoo stock ticker time series CSV
 */
public class StockSeriesTypeHandler extends PointTypeHandler {

    /** _more_ */
    public static final int IDX_SYMBOL = IDX_PROPERTIES + 1;

    /** _more_ */
    public static final int IDX_DATATYPE = IDX_SYMBOL + 1;

    /** _more_ */
    public static final int IDX_INTERVAL = IDX_DATATYPE + 1;


    /** _more_ */
    public static final String URL =
        "https://www.alphavantage.co/query?function=${function}&symbol=${symbol}&apikey=${apikey}&datatype=csv";

    /**
     * ctor
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception on badness
     */
    public StockSeriesTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }


    /**
     * _more_
     *
     * @param wikiUtil _more_
     * @param request _more_
     * @param originalEntry _more_
     * @param entry _more_
     * @param tag _more_
     * @param props _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props)
            throws Exception {
        if (tag.equals("stockurl")) {
            return getPathForEntry(request, entry,false);
        }

        return super.getWikiInclude(wikiUtil, request, originalEntry, entry,
                                    tag, props);
    }

    /** _more_ */
    public static final String PROPERTIES =
        "skiplines=1\nfields=Timestamp[type=date format=yyyy-MM-dd HH:mm:ss],Open[chartable=true],High[chartable=true],Low[chartable=true],Close[chartable=true],Volume[chartable=true]";

    /** _more_ */
    public static final String PROPERTIES_ADJUSTED =
        "skiplines=1\nfields=Timestamp[type=date format=yyyy-MM-dd HH:mm:ss],Open[chartable=true],High[chartable=true],Low[chartable=true],Close[chartable=true],Adjusted_Close[chartable=true]Volume[chartable=true],Dividend_Amount[chartable=true],Split_Coefficient[chartable=true]";

    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public String getRecordPropertiesFromType(Request request,Entry entry) throws Exception {
        String datatype = entry.getStringValue(request,IDX_DATATYPE, "");
        if (datatype.equals("TIME_SERIES_INTRADAY")
                || datatype.equals("TIME_SERIES_DAILY")
                || datatype.equals("TIME_SERIES_WEEKLY")
                || datatype.equals("TIME_SERIES_MONTHLY")) {
            return PROPERTIES;
        }

        return PROPERTIES_ADJUSTED;
    }

    /**
     * Return the URL to the CSV file
     *
     *
     * @param request _more_
     * @param entry the entry
     *
     * @return URL
     *
     * @throws Exception on badness
     */
    @Override
    public String getPathForEntry(Request request, Entry entry, boolean forRead)
            throws Exception {
        String apikey = getRepository().getProperty("alphavantage.api",
                            (String) null);
        if ( !Utils.stringDefined(apikey)) {
            return null;
        }

        String symbol = entry.getStringValue(request,IDX_SYMBOL, (String) null);
        if ( !Utils.stringDefined(symbol)) {
            return null;
        }

        String url      = URL;
        String datatype = entry.getStringValue(request,IDX_DATATYPE, "");
        if (datatype.equals("TIME_SERIES_INTRADAY")) {
            url += "&interval=" + entry.getStringValue(request,IDX_INTERVAL, "");
        }
        url = url.replace("${apikey}", apikey).replace("${symbol}",
                          symbol).replace("${function}", datatype);

        return url;
    }



}
