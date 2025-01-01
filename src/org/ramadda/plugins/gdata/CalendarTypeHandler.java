/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.gdata;


import org.ramadda.repository.util.SelectInfo;

import com.google.gdata.client.*;

import com.google.gdata.client.calendar.*;

import com.google.gdata.client.docs.*;
import com.google.gdata.data.MediaContent;
import com.google.gdata.data.TextContent;
import com.google.gdata.data.acl.*;
import com.google.gdata.data.acl.*;
import com.google.gdata.data.calendar.*;
import com.google.gdata.data.docs.*;
import com.google.gdata.data.extensions.*;
import com.google.gdata.data.extensions.*;
import com.google.gdata.util.*;
import com.google.gdata.util.*;
import com.google.gdata.util.*;


import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.CalendarOutputHandler;
import org.ramadda.repository.type.*;

import org.ramadda.util.HtmlUtils;


import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;

import ucar.unidata.util.StringUtil;

import java.io.File;

import java.net.URL;







import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * Class TypeHandler _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class CalendarTypeHandler extends GdataTypeHandler {

    /** _more_ */
    public static final String TYPE_CALENDAR = "calendar";

    /** _more_ */
    public static final String TYPE_EVENT = "event";

    /** _more_ */
    public static final String CALENDAR_ROOT =
        "https://www.google.com/calendar/feeds/";

    /** _more_ */
    private CalendarOutputHandler calendarOutputHandler;

    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public CalendarTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }





    /**
     * _more_
     *
     * @param userId _more_
     * @param password _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected GoogleService doMakeService(String userId, String password)
            throws Exception {
        CalendarService myService =
            new CalendarService("exampleCo-exampleApp-1");
        myService.setUserCredentials(userId, password);

        return myService;
    }


    /**
     * _more_
     *
     * @param entry _more_
     * @param feedUrl _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private CalendarFeed getFeed(Entry entry, URL feedUrl) throws Exception {
        return ((CalendarService) getService(entry)).getFeed(feedUrl,
                CalendarFeed.class);
    }


    /**
     * _more_
     *
     * @param entry _more_
     * @param feedUrl _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private CalendarEventFeed getEventFeed(Entry entry, URL feedUrl)
            throws Exception {
        return ((CalendarService) getService(entry)).getFeed(feedUrl,
                CalendarEventFeed.class);
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
    public List<String> getCalendarIds(Request request, Entry entry)
            throws Exception {
        List<String> ids = entry.getChildIds();
        if (ids != null) {
            return ids;
        }
        ids = new ArrayList<String>();
        for (Entry calendar : getCalendarEntries(request, entry)) {
            ids.add(calendar.getId());
        }
        entry.setChildIds(ids);

        return ids;
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
    public List<Entry> getCalendarEntries(Request request, Entry entry)
            throws Exception {
        List<Entry> entries = new ArrayList<Entry>();
        String      userId  = getUserId(entry);
        if (userId == null) {
            return entries;
        }
        URL feedUrl = new URL(CALENDAR_ROOT + "default/allcalendars/full");
        CalendarFeed resultFeed = getFeed(entry, feedUrl);
        for (int i = 0; i < resultFeed.getEntries().size(); i++) {
            CalendarEntry calendar = resultFeed.getEntries().get(i);
            String entryId = getSynthId(entry, TYPE_CALENDAR,
                                        IOUtil.getFileTail(calendar.getId()));
            String       title    = calendar.getTitle().getPlainText();
            Entry        newEntry = new Entry(entryId, this, true);
            StringBuffer desc     = new StringBuffer();
            addMetadata(newEntry, calendar, desc);
            entries.add(newEntry);
            Resource resource = new Resource();
            Date     now      = new Date();
            newEntry.initEntry(title, desc.toString(), entry,
                               entry.getUser(), resource, "",
                               Entry.DEFAULT_ORDER, now.getTime(),
                               now.getTime(), now.getTime(), now.getTime(),
                               null);
            getEntryManager().cacheSynthEntry(newEntry);
        }

        return entries;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param mainEntry _more_
     * @param entry _more_
     * @param calendarId _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<String> getEventIds(Request request, Entry mainEntry,
                                    Entry entry, String calendarId)
            throws Exception {
        List<String> ids = entry.getChildIds();
        if (ids != null) {
            return ids;
        }
        ids = new ArrayList<String>();
        for (Entry event :
                getEventEntries(request, mainEntry, entry, calendarId)) {
            ids.add(event.getId());
        }
        entry.setChildIds(ids);

        return ids;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param mainEntry _more_
     * @param entry _more_
     * @param calendarId _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Entry> getEventEntries(Request request, Entry mainEntry,
                                       Entry entry, String calendarId)
            throws Exception {
        List<Entry> entries = new ArrayList<Entry>();
        String      userId  = getUserId(mainEntry);
        if (userId == null) {
            return entries;
        }
        URL feedUrl = new URL(CALENDAR_ROOT + calendarId + "/private/full");
        //        System.err.println("Feed:" + feedUrl);
        CalendarEventFeed resultFeed = getEventFeed(mainEntry, feedUrl);
        for (int i = 0; i < resultFeed.getEntries().size(); i++) {
            CalendarEventEntry event = resultFeed.getEntries().get(i);
            String entryId = getSynthId(mainEntry, TYPE_EVENT,
                                        calendarId + ":"
                                        + IOUtil.getFileTail(event.getId()));
            String       title    = event.getTitle().getPlainText();
            Entry        newEntry = new Entry(entryId, this, false);
            StringBuffer desc     = new StringBuffer();
            addMetadata(newEntry, event);
            entries.add(newEntry);
            Date from = new Date();
            Date to   = new Date();
            Date now  = new Date();
            if (event.getTimes().size() > 0) {
                com.google.gdata.data.DateTime startTime =
                    event.getTimes().get(0).getStartTime();
                com.google.gdata.data.DateTime endTime =
                    event.getTimes().get(0).getEndTime();
                from = new Date(startTime.getValue());
                to   = new Date(endTime.getValue());
            }
            if (event.getContent() instanceof TextContent) {
                TextContent content = (TextContent) event.getContent();
                desc.append(content.getContent().getPlainText());
                desc.append(HtmlUtils.p());
            }


            for (EventWho who : event.getParticipants()) {
                getMetadataManager().addMetadata(newEntry,
                        new Metadata(getRepository().getGUID(),
                                     newEntry.getId(), "gdata.participant",
                                     false, who.getValueString(),
                                     who.getEmail(), "", "", ""));

            }

            for (Where where : event.getLocations()) {
                String s = where.getValueString();
                if ((s == null) || (s.length() == 0)) {
                    continue;
                }
                getMetadataManager().addMetadata(newEntry,
                        new Metadata(getRepository().getGUID(),
                                     newEntry.getId(), "gdata.location",
                                     false, s, "", "", "", ""));
            }

            Resource resource = new Resource(event.getHtmlLink().getHref());
            newEntry.initEntry(title, desc.toString(), entry,
                               entry.getUser(), resource, "",
                               Entry.DEFAULT_ORDER, now.getTime(),
                               now.getTime(), from.getTime(), to.getTime(),
                               null);
            getEntryManager().cacheSynthEntry(newEntry);
        }

        return entries;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param mainEntry _more_
     * @param parentEntry _more_
     * @param synthId _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public List<String> getSynthIds(Request request, SelectInfo select, Entry mainEntry,
                                    Entry parentEntry, String synthId)
            throws Exception {
        if (synthId == null) {
            return getCalendarIds(request, mainEntry);
        }
        List<String> ids = parentEntry.getChildIds();
        if (ids != null) {
            return ids;
        }
        ids = new ArrayList<String>();
        List<String> toks       = StringUtil.split(synthId, ":");
        String       type       = toks.get(0);
        String       calendarId = toks.get(1);

        return getEventIds(request, mainEntry, parentEntry, calendarId);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param mainEntry _more_
     * @param id _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry makeSynthEntry(Request request, Entry mainEntry, String id)
            throws Exception {
        List<String> toks = StringUtil.split(id, ":");
        String       type = toks.get(0);
        if (type.equals(TYPE_CALENDAR)) {
            for (Entry entry : getCalendarEntries(request, mainEntry)) {
                if (entry.getId().endsWith(id)) {
                    return entry;
                }
            }

            return null;
        }

        String calendarId = toks.get(1);
        String calendarEntryId = getSynthId(mainEntry, TYPE_CALENDAR,
                                            calendarId);
        Entry calendarEntry = getEntryManager().getEntry(request,
                                  calendarEntryId);
        String eventId = getSynthId(mainEntry, TYPE_EVENT,
                                    calendarId + ":" + toks.get(2));

        //        System.err.println(eventId);
        for (Entry entry :
                getEventEntries(request, mainEntry, calendarEntry,
                                calendarId)) {
            //            System.err.println("\t" + entry.getId());
            if (entry.getId().equals(eventId)) {
                return entry;
            }
        }

        return null;
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
    public String getEntryIconUrl(Request request, Entry entry)
            throws Exception {
        if (entry.getId().indexOf(TYPE_EVENT) >= 0) {
            return getIconUrl("/icons/calendar_view_day.png");
        }

        return super.getIconUrl(request, entry);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param group _more_
     * @param subGroups _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public Result getHtmlDisplay(Request request, Entry group,  Entries children) 
	throws Exception {

        if (request.defined(ARG_OUTPUT)) {
            return null;
        }
        if ( !getEntryManager().isSynthEntry(group.getId())) {
            return null;
        }
        if (group.getId().indexOf(TYPE_CALENDAR) < 0) {
            return null;
        }

        if (calendarOutputHandler == null) {
            calendarOutputHandler =
                (CalendarOutputHandler) getRepository().getOutputHandler(
                    CalendarOutputHandler.OUTPUT_CALENDAR);
        }

        return calendarOutputHandler.outputGroup(request,
						 request.getOutput(), group, children.get());
    }



    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        String userId = "info@ramadda.org";
        CalendarService myService =
            new CalendarService("exampleCo-exampleApp-1");
        myService.setUserCredentials(userId, args[0]);

        // Send the request and print the response
        URL feedUrl = new URL(CALENDAR_ROOT + "default/allcalendars/full");
        CalendarFeed resultFeed = myService.getFeed(feedUrl,
                                      CalendarFeed.class);
        System.out.println("Your calendars:");
        for (int i = 0; i < resultFeed.getEntries().size(); i++) {
            CalendarEntry entry = resultFeed.getEntries().get(i);
            String        id    = IOUtil.getFileTail(entry.getId());
            System.out.println("\t" + entry.getTitle().getPlainText() + " "
                               + id);
            System.out.println("\tEvents:");
            URL eventUrl = new URL(CALENDAR_ROOT + id + "/private/full");
            CalendarEventFeed eventFeed = myService.getFeed(eventUrl,
                                              CalendarEventFeed.class);
            for (int eventIdx = 0; eventIdx < eventFeed.getEntries().size();
                    eventIdx++) {
                CalendarEventEntry calendar =
                    eventFeed.getEntries().get(eventIdx);
                System.err.println("\t\t"
                                   + IOUtil.getFileTail(calendar.getId())
                                   + " "
                                   + calendar.getTitle().getPlainText());
            }
        }

    }




}
