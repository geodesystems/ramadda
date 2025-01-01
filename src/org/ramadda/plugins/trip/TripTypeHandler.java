/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.trip;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.WikiUtil;


import org.ramadda.util.sql.Clause;


import org.ramadda.util.sql.SqlUtil;
import org.ramadda.util.sql.SqlUtil;


import org.w3c.dom.*;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HttpServer;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.sql.PreparedStatement;

import java.sql.ResultSet;
import java.sql.Statement;


import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;


/**
 *
 *
 */
public class TripTypeHandler extends ExtensibleGroupTypeHandler {

    /** _more_ */
    private CalendarOutputHandler calendarOutputHandler;

    /** _more_ */
    public static final String TYPE_HOTEL = "trip_hotel";

    /** _more_ */
    public static final String TYPE_FLIGHT = "trip_flight";

    /** _more_ */
    public static final String TYPE_CAR = "trip_car";

    /** _more_ */
    public static final String TYPE_TRAIN = "trip_train";

    /** _more_ */
    public static final String TYPE_EVENT = "trip_event";


    /** _more_ */
    private static String[] types = { TYPE_HOTEL, TYPE_CAR, TYPE_FLIGHT,
                                      TYPE_TRAIN, TYPE_EVENT, };

    /** _more_ */
    private static String[] names = { "New Lodging", "New Car Rental",
                                      "New Flight", "New Train",
                                      "New Event" };

    /** _more_ */
    private static String[] icons = { "/trip/hotel.png", "/trip/car.gif",
                                      "/trip/plane.png", "/trip/train.gif",
                                      "/trip/event.png" };


    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public TripTypeHandler(Repository repository, Element entryNode)
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
        if (tag.equals("newheader")) {
            boolean canAdd = getAccessManager().canDoNew(request, entry);

            StringBuilder sb = new StringBuilder();
            if (canAdd) {
                sb.append("<b>New:</b> ");
                for (int i = 0; i < types.length; i++) {
                    if (i > 0) {
                        sb.append("&nbsp;|&nbsp;");
                    }
                    sb.append(
                        HtmlUtils.href(
                            HtmlUtils.url(
                                request.entryUrlWithArg(
                                    getRepository().URL_ENTRY_FORM, entry,
                                    ARG_GROUP), ARG_TYPE,
                                        types[i]), HtmlUtils.img(
                                            getRepository().getIconUrl(
                                                icons[i]), msg(names[i]))));
                }

                sb.append("<p>");
            }

            return sb.toString();
        }

        return null;
    }


}
