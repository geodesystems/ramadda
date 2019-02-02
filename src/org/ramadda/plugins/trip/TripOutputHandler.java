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

package org.ramadda.plugins.trip;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.output.*;
import org.ramadda.util.HtmlUtils;

import org.ramadda.util.sql.SqlUtil;


import org.w3c.dom.*;


import ucar.unidata.util.Misc;

import java.io.*;




import java.io.File;


import java.net.*;


import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;



import java.util.regex.*;

import java.util.zip.*;


/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class TripOutputHandler extends OutputHandler {






    /** _more_ */
    public static final OutputType OUTPUT_ITINERARY =
        new OutputType("Trip Itinerary", "trip_itinerary",
                       OutputType.TYPE_VIEW, "", "/trip/itinerary.png");


    /**
     * _more_
     *
     *
     * @param repository _more_
     * @param element _more_
     * @throws Exception _more_
     */
    public TripOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        addType(OUTPUT_ITINERARY);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param state _more_
     * @param links _more_
     *
     *
     * @throws Exception _more_
     */
    public void getEntryLinks(Request request, State state, List<Link> links)
            throws Exception {
        if (state.group == null) {
            return;
        }
        if (state.group.getTypeHandler().getType().equals("trip_trip")) {
            links.add(makeLink(request, state.getEntry(), OUTPUT_ITINERARY));
        }
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param outputType _more_
     * @param group _more_
     * @param subGroups _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputGroup(Request request, OutputType outputType,
                              Entry group, List<Entry> subGroups,
                              List<Entry> entries)
            throws Exception {

        TimeZone         utcTimeZone = TimeZone.getTimeZone("UTC");
        TimeZone timeZone = TimeZone.getTimeZone(group.getValue(0, "UTC"));
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE MMMMM d, yyyy");
        sdf.setTimeZone(timeZone);
        SimpleDateFormat timeSdf = new SimpleDateFormat("HH:mm Z");
        timeSdf.setTimeZone(timeZone);
        SimpleDateFormat utcTimeSdf = new SimpleDateFormat("HH:mm Z");
        utcTimeSdf.setTimeZone(utcTimeZone);
        boolean         differentTZ = !timeZone.equals(utcTimeZone);

        boolean         forPrint    = request.get("forprint", false);
        TripTypeHandler handler     =
            (TripTypeHandler) group.getTypeHandler();
        StringBuffer    sb          = new StringBuffer();
        if (forPrint) {
            request.setHtmlTemplateId("empty");
        } else {
            request.put("forprint", "true");
            sb.append(
                HtmlUtils.href(
                    request.getUrl(),
                    HtmlUtils.img(
                        getRepository().getIconUrl("/icons/printer.png"))));
            sb.append(" ");
            sb.append(handler.getWikiInclude(null, request, group, group,
                                             "newheader", null));
        }
        subGroups.addAll(entries);

        List<Entry> eventEntries = new ArrayList<Entry>();
        for (Entry entry : subGroups) {
            if (entry.getTypeHandler() instanceof TripItemHandler) {
                eventEntries.add(entry);
            }
        }
        subGroups = eventEntries;


        sb.append(HtmlUtils.cssLink(getRepository().getUrlBase()
                                    + "/trip/trip.css"));
        subGroups = getEntryUtil().sortEntriesOnDate(subGroups, false);
        String currentDate = "";
        for (Entry entry : subGroups) {
            String entryDate = sdf.format(new Date(entry.getStartDate()));
            if ( !Misc.equals(currentDate, entryDate)) {
                if (currentDate.length() > 0) {
                    sb.append("</div>");
                }
                currentDate = entryDate;
                sb.append(
                    HtmlUtils.div(
                        entryDate, HtmlUtils.cssClass("itinerary-header")));
                sb.append("<div class=itinerary-day>");

            }
            String url = getEntryManager().getAjaxLink(request, entry,
                             entry.getLabel(), null, true, null,
                             false).toString();

            sb.append(url);
            StringBuffer desc = new StringBuffer(entry.getDescription());
            String       type = entry.getTypeHandler().getType();
            desc.append(HtmlUtils.formTable());
            String entryTimezone = entry.getValue(0, null);
            if (type.equals(TripTypeHandler.TYPE_FLIGHT)
                    || type.equals(TripTypeHandler.TYPE_TRAIN)) {
                SimpleDateFormat sdfToUse = timeSdf;
                boolean          addUtc   = differentTZ;
                if (entryTimezone != null) {
                    sdfToUse = new SimpleDateFormat("HH:mm Z");
                    sdfToUse.setTimeZone(TimeZone.getTimeZone(entryTimezone));
                    addUtc = true;
                }
                String timeHtml =
                    sdfToUse.format(new Date(entry.getStartDate()));
                if (addUtc) {
                    timeHtml =
                        timeHtml + " ("
                        + utcTimeSdf.format(new Date(entry.getStartDate()))
                        + ")";
                }
                desc.append(HtmlUtils.formEntry(msgLabel("Time"), timeHtml));
            }

            if (type.equals(TripTypeHandler.TYPE_HOTEL)) {
                String address      = entry.getValue(1, "");
                String phone        = entry.getValue(2, null);
                String email        = entry.getValue(3, null);
                String confirmation = entry.getValue(4, null);

                String mapUrl =
                    "http://maps.google.com/maps?f=q&source=s_q&hl=en&geocode=&q="
                    + address.replaceAll("\n", " ");
                address = address.replaceAll("\n", "<br>");
                desc.append(HtmlUtils.formEntryTop(msgLabel("Address"),
                        HtmlUtils.italics(address) + " "
                        + HtmlUtils.href(mapUrl, "(map)")));
                if (phone != null) {
                    desc.append(HtmlUtils.formEntry(msgLabel("Phone"),
                            phone));
                }
                if (email != null) {
                    desc.append(HtmlUtils.formEntry(msgLabel("Email"),
                            email));
                }
                if (confirmation != null) {
                    desc.append(HtmlUtils.formEntry(msgLabel("Confirmation"),
                            confirmation));
                }
            }


            if (type.equals(TripTypeHandler.TYPE_EVENT)) {
                String address = entry.getValue(1, "");
                String phone   = entry.getValue(2, null);
                String email   = entry.getValue(3, null);
                String mapUrl =
                    "http://maps.google.com/maps?f=q&source=s_q&hl=en&geocode=&q="
                    + address.replaceAll("\n", " ");
                address = address.replaceAll("\n", "<br>");
                desc.append(HtmlUtils.formEntryTop(msgLabel("Address"),
                        HtmlUtils.italics(address) + " "
                        + HtmlUtils.href(mapUrl, "(map)")));
                if (phone != null) {
                    desc.append(HtmlUtils.formEntry(msgLabel("Phone"),
                            phone));
                }
                if (email != null) {
                    desc.append(HtmlUtils.formEntry(msgLabel("Email"),
                            email));
                }
            }

            desc.append(HtmlUtils.formTableClose());

            if (desc.length() > 0) {
                sb.append(HtmlUtils.div(desc.toString(), ""));
            }
        }
        if (subGroups.size() > 0) {
            sb.append("</div>");
        }

        return new Result("", sb);

    }



}
