/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.output;

import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.Utils;

import org.ramadda.util.sql.SqlUtil;

import org.w3c.dom.*;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.io.File;

import java.net.*;

import java.text.SimpleDateFormat;

import java.util.ArrayList;

import java.util.TimeZone;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import java.util.regex.*;

import java.util.zip.*;

/**
 *
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
@SuppressWarnings("unchecked")
public class CalendarOutputHandler extends OutputHandler {

    /** _more_ */
    public static final String TAG_DATA = "data";

    /** _more_ */
    public static final String TAG_EVENT = "event";

    /** _more_ */
    public static final String ATTR_IMAGE = "image";

    /** _more_ */
    public static final String ATTR_LINK = "link";

    /** _more_ */
    public static final String ATTR_START = "start";

    /** _more_ */
    public static final String ATTR_TITLE = "title";

    /** _more_ */
    public static final String ATTR_END = "end";

    /** _more_ */
    public static final String ATTR_EARLIESTEND = "earliestEnd";

    /** _more_ */
    public static final String ATTR_ISDURATION = "isDuration";

    /** _more_ */
    public static final String ATTR_LATESTSTART = "latestStart";

    /** _more_ */
    public static final String ATTR_ICON = "icon";

    /** _more_ */
    public static final String ATTR_COLOR = "color";

    /** _more_ */
    public static final OutputType OUTPUT_DATE_GRID =
        new OutputType("Date Grid", "calendar.grid", OutputType.TYPE_VIEW,
                       "", ICON_DATEGRID);

    /** _more_ */
    public static final OutputType OUTPUT_CALENDAR =
        new OutputType("Calendar", "calendar.calendar",
                       OutputType.TYPE_VIEW | OutputType.TYPE_FORSEARCH, "",
                       ICON_CALENDAR);

    /** _more_ */
    public static final OutputType OUTPUT_TIMELINE =
        new OutputType("Timeline", "default.timeline", OutputType.TYPE_VIEW,
                       "", ICON_TIMELINE);

    /**
     * _more_
     *
     * @param repository _more_
     * @param element _more_
     * @throws Exception _more_
     */
    public CalendarOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        addType(OUTPUT_CALENDAR);
        addType(OUTPUT_TIMELINE);
        addType(OUTPUT_DATE_GRID);
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
        if (state.entry != null) {
            return;
        }
        if (state.getEntry() != null) {
            links.add(makeLink(request, state.getEntry(), OUTPUT_CALENDAR));
            if (state.getAllEntries().size() > 1) {
                links.add(makeLink(request, state.getEntry(),
                                   OUTPUT_TIMELINE));
                //                links.add(makeLink(request, state.getEntry(),
                //                                   OUTPUT_DATE_GRID));
            }
        }
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param group _more_
     * @param children _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result handleIfTimelineXml(Request request, Entry group,
                                      List<Entry> children)
            throws Exception {
        if (request.get("timelinexml", false)) {
            return outputTimelineXml(request, group, children);
        }

        return null;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param outputType _more_
     * @param group _more_
     * @param children _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public Result outputGroup(Request request, OutputType outputType,
                              Entry group, List<Entry> children)
            throws Exception {

        Result timelineResult = handleIfTimelineXml(request, group, children);
        if (timelineResult != null) {
            return timelineResult;
        }

        StringBuffer sb = new StringBuffer();
        showNext(request, children, sb);
        Result result;
        if (outputType.equals(OUTPUT_DATE_GRID)) {
            result = outputDateGrid(request, group, children, sb);
        } else if (outputType.equals(OUTPUT_TIMELINE)) {
            getPageHandler().entrySectionOpen(request, group, sb, "Timeline");
            makeTimeline(request, group, children, sb, "height: 300px;",
                         new Hashtable());
            getPageHandler().entrySectionClose(request, group, sb);
            result = makeLinksResult(request,
                                     msg("Timeline") + " - "
                                     + group.getName(), sb,
                                         new State(group, children));

            return result;
        } else {
            String prefix = request.getPrefixHtml();
            if (Utils.stringDefined(prefix)) {
                sb.append(prefix);
            } else {
                getPageHandler().entrySectionOpen(request, group, sb, "");
            }
            result = outputCalendar(request, group, children, sb);
            if (Utils.stringDefined(prefix)) {
                getPageHandler().entrySectionClose(request, group, sb);
            }
        }

        addLinks(request, result, new State(group, children));

        return result;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param group _more_
     * @param allEntries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputTimelineXml(Request request, Entry group,
                                    List<Entry> allEntries)
            throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d yyyy HH:mm:ss Z");
        StringBuffer     sb  = new StringBuffer();
        sb.append(XmlUtil.openTag(TAG_DATA));

        for (Entry entry : allEntries) {
            String icon = getPageHandler().getIconUrl(request, entry);
            StringBuffer attrs = new StringBuffer(XmlUtil.attrs(ATTR_TITLE,
                                     " " + entry.getName(), ATTR_ICON, icon));

            List<String> urls = new ArrayList<String>();
            getMetadataManager().getThumbnailUrls(request, entry, urls);
            if (urls.size() > 0) {
                attrs.append(XmlUtil.attrs(ATTR_IMAGE, urls.get(0)));
            }
            String entryUrl =
                request.entryUrl(getRepository().URL_ENTRY_SHOW, entry);
            attrs.append(XmlUtil.attrs(ATTR_LINK, entryUrl));
            attrs.append(XmlUtil.attrs("eventID", entry.getId()));

            attrs.append(
                XmlUtil.attrs(
                    ATTR_START, sdf.format(new Date(entry.getStartDate()))));
            if (entry.getStartDate() != entry.getEndDate()) {
                attrs.append(
                    XmlUtil.attrs(
                        ATTR_END, sdf.format(new Date(entry.getEndDate()))));
            }
            sb.append(XmlUtil.openTag(TAG_EVENT, attrs.toString()));
            if (entry.getDescription().length() > 0) {
                String infoHtml = getMapManager().cleanupInfo(request,
                                      getMapManager().makeInfoBubble(request,
								     entry, null,false));
                infoHtml = HtmlUtils.div(
                    infoHtml,
                    HtmlUtils.style(
                        "max-height: 350px;      overflow-y:auto;"));
                sb.append(XmlUtil.getCdata(infoHtml));
            }
            sb.append(XmlUtil.closeTag(TAG_EVENT));
            sb.append("\n");
        }

        sb.append(XmlUtil.closeTag(TAG_DATA));

        //        System.err.println(sb);
        return new Result("", sb, "text/xml");
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param mainEntry _more_
     * @param entries _more_
     * @param sb _more_
     * @param style _more_
     * @param props _more_
     *
     *
     * @throws Exception _more_
     */
    public void makeTimeline(Request request, Entry mainEntry,
                             List<Entry> entries, Appendable sb,
                             String style, Hashtable props)
            throws Exception {

        SimpleDateFormat sdf = new SimpleDateFormat("MMM d yyyy HH:mm:ss Z");
        long             minDate = 0;
        long             maxDate = 0;
        for (Entry entry : (List<Entry>) entries) {
            if ((minDate == 0) || (entry.getStartDate() < minDate)) {
                minDate = entry.getStartDate();
            }
            if ((maxDate == 0) || (entry.getEndDate() > maxDate)) {
                maxDate = entry.getEndDate();
            }
        }
        long diffDays = (maxDate - minDate) / 1000 / 3600 / 24;
        //      System.err.println("HOURS:" + diffDays +" " + new Date(minDate) + " " + new Date(maxDate));

        String interval = "Timeline.DateTime.MONTH";

        if (diffDays < 3) {
            interval = "Timeline.DateTime.HOUR";
        } else if (diffDays < 21) {
            interval = "Timeline.DateTime.DAY";
        } else if (diffDays < 30) {
            interval = "Timeline.DateTime.WEEK";
        } else if (diffDays < 150) {
            interval = "Timeline.DateTime.MONTH";
        } else if (diffDays < 10 * 365) {
            interval = "Timeline.DateTime.YEAR";
        } else {
            interval = "Timeline.DateTime.DECADE";
        }

        String timelineTemplate =
            getRepository().getResource(
                "/org/ramadda/repository/resources/web/timeline.html");

        String url = request.getUrl();
        if (mainEntry != null) {
            url = HtmlUtils.url(getRepository().URL_ENTRY_SHOW.toString(),
                                ARG_ENTRYID, mainEntry.getId(), ARG_OUTPUT,
                                OUTPUT_TIMELINE.toString());
        }
        url = url + "&timelinexml=true";
        timelineTemplate = getRepository().getPageHandler().applyBaseMacros(
            timelineTemplate);
        timelineTemplate = timelineTemplate.replace("${timelineurl}", url);
        String mapVar = (String) props.get("mapVar");
        if (mapVar == null) {
            mapVar = "null";
        }
        StringBuilder json =
            new StringBuilder(
                "timelineJson = {'dateTimeFormat': 'iso8601','events' : [");
        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        for (int i = 0; i < entries.size(); i++) {
            Entry        entry  = entries.get(i);
            List<String> jprops = new ArrayList<String>();
            jprops.add("id");
            jprops.add(entry.getId());
            jprops.add("start");
            jprops.add(sdf2.format(new Date(entry.getStartDate())));
            if (entry.getStartDate() != entry.getEndDate()) {
                jprops.add("end");
                jprops.add(sdf2.format(new Date(entry.getEndDate())));
            }
            jprops.add("title");
            jprops.add(entry.getName());
            jprops.add("description");
            jprops.add("");
            jprops.add("icon");
            String icon = getPageHandler().getIconUrl(request, entry);
	    jprops.add(icon);
            jprops.add("link");
            jprops.add(request.entryUrl(getRepository().URL_ENTRY_SHOW,
                                        entry));
            if (i > 0) {
                json.append(",");
            }

            json.append(JsonUtil.mapAndQuote(jprops));

        }
        json.append("]};");

        timelineTemplate = timelineTemplate.replace("${timelinejson}",
                json.toString());

        timelineTemplate = timelineTemplate.replace("${mapvar}", mapVar);
        timelineTemplate = timelineTemplate.replace("${basedate}",
                sdf.format(new Date(minDate)));
        timelineTemplate = timelineTemplate.replace("${intervalUnit}",
                interval);
        timelineTemplate = timelineTemplate.replace("${style}", style);

        String extra = "";
        if (Misc.equals(props.get("shareSelected"), "true")) {
            extra = "  timelineShareSelected = true;\n";
        }
        timelineTemplate = timelineTemplate.replace("${extra}", extra);
        sb.append(timelineTemplate);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param group _more_
     * @param entries _more_
     * @param sb _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Result outputDateGrid(Request request, Entry group,
                                  List<Entry> entries, Appendable sb)
            throws Exception {
        getPageHandler().entrySectionOpen(request, group, sb, "Date Grid");
	makeDateTable(request,entries, sb, true,true);
        getPageHandler().entrySectionClose(request, group, sb);
        return new Result(msg("Date Grid"), sb);
    }

    public void makeDateTable(Request request, 
			      List<Entry> entries, Appendable sb, boolean byType,boolean showTime)
	throws Exception {
        List<String>     types    = new ArrayList<String>();
        List             days     = new ArrayList();
        HashSet         seenDay  = new HashSet();
        HashSet          seen = new HashSet(); 
        Hashtable        contents = new Hashtable();

        SimpleDateFormat sdf      = new SimpleDateFormat();
        sdf.setTimeZone(RepositoryUtil.TIMEZONE_DEFAULT);
        sdf.applyPattern("d");
        SimpleDateFormat timeSdf = new SimpleDateFormat();
        timeSdf.setTimeZone(RepositoryUtil.TIMEZONE_DEFAULT);
        timeSdf.applyPattern("HH:mm");
        SimpleDateFormat monthSdf = new SimpleDateFormat();
        monthSdf.setTimeZone(RepositoryUtil.TIMEZONE_DEFAULT);
        monthSdf.applyPattern("MM");
        StringBuffer header = new StringBuffer();
        header.append(HtmlUtils.cols(HtmlUtils.bold("%date%")));
	SimpleDateFormat headerSdf = new SimpleDateFormat("MMM yyyy");
        for (Entry entry : entries) {
            String type = entry.getTypeHandler().getType();
	    if(!byType) type = "Entry";
            String day  = sdf.format(new Date(entry.getStartDate()));
            if (!seen.contains(type)) {
                seen.add(type);
		if(byType) {
		    types.add(entry.getTypeHandler().getType());
		    header.append("<td>" + HtmlUtils.bold(entry.getTypeHandler().getLabel()) + "</td>");
		} else {
		    types.add(type);
		    header.append("<td>" + HtmlUtils.bold(type) + "</td>");
		}
            }
            if (!seenDay.contains(day)) {
                days.add(new Date(entry.getStartDate()));
                seenDay.add(day);
            }
            String       time =
                timeSdf.format(new Date(entry.getStartDate()));
            String       key   = type + "_" + day;
            StringBuffer colSB = (StringBuffer) contents.get(key);
            if (colSB == null) {
                colSB = new StringBuffer();
                contents.put(key, colSB);
            }
            colSB.append(getEntryManager().getAjaxLink(request, entry, showTime?time:entry.getName()));
        }

        sb.append(
            "<table class=\"datetable\" border=\"0\" cellspacing=\"0\" cellpadding=\"3\" width=\"100%\">");
        days = Misc.sort(days);
        String currentMonth = "";
	int rowCnt = 0;
        for (int dayIdx = 0; dayIdx < days.size(); dayIdx++) {
            Date   date  = (Date) days.get(dayIdx);
            String month = monthSdf.format(date);
            //Put the header in every month
            if ( !currentMonth.equals(month)) {
                currentMonth = month;
		String headerDttm = headerSdf.format(date);

                sb.append("<tr class=\"datetableheader\">" + header.toString().replace("%date%",headerDttm) + "</tr>");
		rowCnt=0;
            }
	    boolean even = (rowCnt % 2) == 0;
	    rowCnt++;
            String day = sdf.format(date);
            sb.append("<tr valign=\"top\" class=" +(even?"ramadda-row-even":"ramadda-row-odd")+">");
            sb.append("<td width='200'>&nbsp;&nbsp;" + day + "</td>");
	    for(String type: types) {
                String       key         = type + "_" + day;
                StringBuffer cb          = (StringBuffer) contents.get(key);
                if (cb == null) {
                    sb.append("<td>&nbsp;</td>");
                } else {
                    sb.append(
                        "<td><div style=\"max-height: 150px; overflow-y: auto;\">"
                        + cb + "</div></td>");
                }
            }
            sb.append("</tr>");
        }
        sb.append("</table>");

    }

    /**
     * _more_
     *
     * @param day _more_
     *
     * @return _more_
     */
    public static GregorianCalendar getCalendar(int[] day) {
        return getCalendar(day[IDX_DAY], day[IDX_MONTH], day[IDX_YEAR]);
    }

    /**
     * _more_
     *
     * @param day _more_
     * @param month _more_
     * @param year _more_
     *
     * @return _more_
     */
    public static GregorianCalendar getCalendar(int day, int month,
            int year) {
        GregorianCalendar cal =
            new GregorianCalendar(RepositoryUtil.TIMEZONE_DEFAULT);
        cal.set(cal.DAY_OF_MONTH, day);
        cal.set(cal.MONTH, month);
        cal.set(cal.YEAR, year);

        return cal;
    }

    /**
     * _more_
     *
     * @param cal _more_
     *
     * @return _more_
     */
    public static int[] getDayMonthYear(GregorianCalendar cal) {
        return new int[] { cal.get(cal.DAY_OF_MONTH), cal.get(cal.MONTH),
                           cal.get(cal.YEAR) };
    }

    /**
     * _more_
     *
     * @param cal _more_
     * @param what _more_
     * @param delta _more_
     *
     * @return _more_
     */
    private GregorianCalendar add(GregorianCalendar cal, int what,
                                  int delta) {
        cal.add(what, delta);

        return cal;
    }

    /** _more_ */
    private static final int IDX_DAY = 0;

    /** _more_ */
    private static final int IDX_MONTH = 1;

    /** _more_ */
    private static final int IDX_YEAR = 2;

    /**
     * _more_
     *
     * @param cal _more_
     *
     * @return _more_
     */
    public static String getUrlArgs(GregorianCalendar cal) {
        return getUrlArgs(getDayMonthYear(cal));
    }

    /**
     * _more_
     *
     * @param dayMonthYear _more_
     *
     * @return _more_
     */
    public static String getUrlArgs(int[] dayMonthYear) {
        return ARG_YEAR + "=" + dayMonthYear[IDX_YEAR] + "&" + ARG_MONTH
               + "=" + dayMonthYear[IDX_MONTH] + "&" + ARG_DAY + "="
               + dayMonthYear[IDX_DAY];
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param group _more_
     * @param entries _more_
     * @param sb _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputCalendar(Request request, Entry group,
                                 List<Entry> entries, Appendable sb)
            throws Exception {

        if (entries.size() == 0) {
            sb.append(
                getPageHandler().showDialogNote(msg(LABEL_NO_ENTRIES_FOUND)));
        }
        outputCalendar(request, makeCalendarEntries(request, entries), sb,
                       request.defined(ARG_DAY));
        getPageHandler().entrySectionClose(request, group, sb);

        return new Result(msg("Calendar"), sb);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<CalendarEntry> makeCalendarEntries(Request request,
            List<Entry> entries)
            throws Exception {
	entries = getEntryUtil().sortEntriesOnDate(entries, false);
        List<CalendarEntry> calEntries = new ArrayList<CalendarEntry>();
	SimpleDateFormat sdf = new SimpleDateFormat("HH:mm z");
	String tz = entries.size()>0?getEntryUtil().getTimezone(request,entries.get(0)):null;
	if(tz!=null) {
	    sdf.setTimeZone(TimeZone.getTimeZone(tz));
	}
        for (Entry entry : entries) {
            Date   entryDate = new Date(entry.getStartDate());
            String label     = entry.getLabel();
            if (label.length() > 20) {
                label = label.substring(0, 19) + "...";
            }
	    label = sdf.format(new Date(entry.getStartDate())) +" - " + label;
            String url =
                HU.nobr(getEntryManager().getAjaxLink(request, entry,
						      label, null, false, false).toString());
	    url = getEntryManager().getPopupLink(request, entry, label);

            calEntries.add(new CalendarEntry(entryDate, url, entry));
        }

        return calEntries;
    }

    /**
     * Class description
     *
     *
     * @version        Enter version here..., Mon, May 3, '10
     * @author         Enter your name here...
     */
    public static class CalendarEntry {

        /** _more_ */
        Date date;

        /** _more_ */
        String label;

        /** _more_ */
        Object dayObject;

        /**
         * _more_
         *
         * @param date _more_
         * @param label _more_
         * @param dayObject _more_
         */
        public CalendarEntry(Date date, String label, Object dayObject) {
            this.date      = date;
            this.label     = label;
            this.dayObject = dayObject;
        }
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entries _more_
     * @param sb _more_
     * @param doDay _more_
     *
     * @throws Exception _more_
     */
    public void outputCalendar(Request request, List<CalendarEntry> entries,
                               Appendable sb, boolean doDay)
            throws Exception {

        boolean hadDateArgs = request.defined(ARG_YEAR)
                              || request.defined(ARG_MONTH)
                              || request.defined(ARG_DAY);

        int[] today = getDayMonthYear(
                          new GregorianCalendar(
                              RepositoryUtil.TIMEZONE_DEFAULT));

        int[] selected = new int[] { request.get(ARG_DAY, today[IDX_DAY]),
                                     request.get(ARG_MONTH, today[IDX_MONTH]),
                                     request.get(ARG_YEAR, today[IDX_YEAR]) };

        int[] prev = (doDay
                      ? getDayMonthYear(add(getCalendar(selected),
                                            Calendar.DAY_OF_MONTH, -1))
                      : getDayMonthYear(add(getCalendar(selected),
                                            Calendar.MONTH, -1)));
        int[] next = (doDay
                      ? getDayMonthYear(add(getCalendar(selected),
                                            Calendar.DAY_OF_MONTH, 1))
                      : getDayMonthYear(add(getCalendar(selected),
                                            Calendar.MONTH, 1)));

        int[] prevprev = (doDay
                          ? getDayMonthYear(add(getCalendar(selected),
                              Calendar.MONTH, -1))
                          : getDayMonthYear(add(getCalendar(selected),
                              Calendar.YEAR, -1)));
        int[] nextnext = (doDay
                          ? getDayMonthYear(add(getCalendar(selected),
                              Calendar.MONTH, 1))
                          : getDayMonthYear(add(getCalendar(selected),
                              Calendar.YEAR, 1)));

        int[]                   someDate = null;

        List                    dayItems = null;
        Hashtable               dates    = new Hashtable();
        Hashtable<String, List> map      = new Hashtable<String, List>();
        GregorianCalendar mapCal =
            new GregorianCalendar(RepositoryUtil.TIMEZONE_DEFAULT);
        boolean didone = false;
        for (int tries = 0; tries < 2; tries++) {
            dayItems = new ArrayList();
            for (CalendarEntry entry : entries) {
                Date entryDate = entry.date;
                mapCal.setTime(entryDate);
                int[] entryDay = getDayMonthYear(mapCal);

                //                System.err.println("entry:" + entryDate + " -- "  + entryDay[IDX_YEAR] +" " + selected[IDX_YEAR] +" " + entryDay[IDX_MONTH] + " " + selected[IDX_MONTH]);

                String key = entryDay[IDX_YEAR] + "/" + entryDay[IDX_MONTH]
                             + "/" + entryDay[IDX_DAY];
                if (tries == 0) {
                    dates.put(key, key);
                }
                if (someDate == null) {
                    someDate = entryDay;
                }
                if (doDay) {
                    if ((entryDay[IDX_DAY] != selected[IDX_DAY])
                            || (entryDay[IDX_MONTH] != selected[IDX_MONTH])
                            || (entryDay[IDX_YEAR] != selected[IDX_YEAR])) {
                        continue;
                    }
                } else {
                    if ( !((entryDay[IDX_YEAR] == selected[IDX_YEAR])
                            && (entryDay[IDX_MONTH]
                                == selected[IDX_MONTH]))) {
                        continue;
                    }
                }
                List dayList = map.get(key);
                if (dayList == null) {
                    map.put(key, dayList = new ArrayList());
                }
                if (doDay) {
                    dayItems.add(entry.dayObject);
                } else {
                    dayList.add(entry.label);
                }
                didone = true;
            }

            if (didone || hadDateArgs) {
                break;
            }
            if (someDate != null) {
                selected = someDate;
                prev     = (doDay
                            ? getDayMonthYear(add(getCalendar(selected),
                            Calendar.DAY_OF_MONTH, -1))
                            : getDayMonthYear(add(getCalendar(selected),
                            Calendar.MONTH, -1)));
                next = (doDay
                        ? getDayMonthYear(add(getCalendar(selected),
                        Calendar.DAY_OF_MONTH, 1))
                        : getDayMonthYear(add(getCalendar(selected),
                        Calendar.MONTH, 1)));
                prevprev = (doDay
                            ? getDayMonthYear(add(getCalendar(selected),
                            Calendar.MONTH, -1))
                            : getDayMonthYear(add(getCalendar(selected),
                            Calendar.YEAR, -1)));
                nextnext = (doDay
                            ? getDayMonthYear(add(getCalendar(selected),
                            Calendar.MONTH, 1))
                            : getDayMonthYear(add(getCalendar(selected),
                            Calendar.YEAR, 1)));
            }
        }

        String[] navIconolds = { "/icons/prevprev.gif", "/icons/prev.gif",
                                 "/icons/today.gif", "/icons/next.gif",
                                 "/icons/nextnext.gif" };

        String[] navIcons = { "fas fa-backward", "fas fa-step-backward",
                              "fas fa-stop", "fas fa-step-forward",
                              "fas fa-forward" };

        String[]          navLabels;
        SimpleDateFormat  headerSdf;
        GregorianCalendar cal;
        List<String>      navUrls = new ArrayList<String>();

        if (doDay) {
            headerSdf = new SimpleDateFormat("MMMMM, dd yyyy");
            navLabels = new String[] { "Previous Month", "Previous Day",
                                       "Today", "Next Day", "Next Month" };
            cal = getCalendar(selected);
        } else {
            headerSdf = new SimpleDateFormat("MMMMM yyyy");
            navLabels = new String[] { "Last Year", "Last Month",
                                       "Current Month", "Next Month",
                                       "Next Year" };
            cal = getCalendar(1, selected[IDX_MONTH], selected[IDX_YEAR]);
        }

        request.put(ARG_YEAR, "" + (prevprev[IDX_YEAR]));
        request.put(ARG_MONTH, "" + (prevprev[IDX_MONTH]));
        if (doDay) {
            request.put(ARG_DAY, "" + (prevprev[IDX_DAY]));
        }
        navUrls.add(request.getUrl());

        request.put(ARG_YEAR, "" + (prev[IDX_YEAR]));
        request.put(ARG_MONTH, "" + (prev[IDX_MONTH]));
        if (doDay) {
            request.put(ARG_DAY, "" + (prev[IDX_DAY]));
        }
        navUrls.add(request.getUrl());

        request.put(ARG_YEAR, "" + (today[IDX_YEAR]));
        request.put(ARG_MONTH, "" + (today[IDX_MONTH]));
        if (doDay) {
            request.put(ARG_DAY, "" + (today[IDX_DAY]));
        }
        navUrls.add(request.getUrl());

        request.put(ARG_YEAR, "" + next[IDX_YEAR]);
        request.put(ARG_MONTH, "" + next[IDX_MONTH]);
        if (doDay) {
            request.put(ARG_DAY, "" + (next[IDX_DAY]));
        }
        navUrls.add(request.getUrl());

        request.put(ARG_YEAR, "" + (nextnext[IDX_YEAR]));
        request.put(ARG_MONTH, "" + (nextnext[IDX_MONTH]));
        if (doDay) {
            request.put(ARG_DAY, "" + (nextnext[IDX_DAY]));
        }
        navUrls.add(request.getUrl());
        request.remove(ARG_DAY,ARG_MONTH,ARG_YEAR);
        List navList = new ArrayList();

        for (int i = 0; i < navLabels.length; i++) {
            navList.add(HtmlUtils.href(navUrls.get(i),
                                       HtmlUtils.img(getIconUrl(navIcons[i]),
                                           navLabels[i], " border=\"0\"")));
        }

        if (doDay) {
            StringBuffer tmp  = new StringBuffer();
            String       link = "";
            if (dayItems.size() > 0) {
                if (dayItems.get(0) instanceof Entry) {
		    Hashtable props = Utils.makeMap("columns","name,time","showTime","true","showForm","false");

                    link = getWikiManager().makeTableTree(request, null,
							  props, dayItems);
                } else {
                    link = StringUtil.join(" ", dayItems);
                }
            }
            request.remove(ARG_MONTH);
            request.remove(ARG_YEAR);
            request.remove(ARG_DAY);
            sb.append(HtmlUtils.p());
            sb.append(
                "<table  width=100% border=0 cellpadding=10><tr valign=top><td width=200>");
            getDateHandler().createMonthNav(sb, cal.getTime(),
                                            request.getUrl(), dates);
            sb.append("</td><td>");
            if (request.isMobile()) {
                sb.append("</td></tr><tr valign=top><td>");
            }
            request.put(ARG_MONTH, "" + selected[IDX_MONTH]);
            request.put(ARG_YEAR, "" + selected[IDX_YEAR]);
            String monthUrl = request.getUrl();
            request.put(ARG_DAY, selected[IDX_DAY]);
            //            sb.append(HtmlUtils.b(StringUtil.join(HtmlUtils.space(1),
            //                    navList)));
            //            sb.append(
            //                HtmlUtils.href(
            //                    monthUrl, HtmlUtils.b(headerSdf.format(cal.getTime()))));
            sb.append(HtmlUtils.b(headerSdf.format(cal.getTime())));
            if (dayItems.size() == 0) {
                sb.append("<p>No Entries");
            } else {
                sb.append(HtmlUtils.br());
                sb.append(link);
                sb.append(tmp);
            }
            sb.append("</table>");
        } else {
            sb.append(
                HtmlUtils.center(
                    HtmlUtils.b(
                        StringUtil.join(HtmlUtils.space(1), navList))));
            sb.append(
                HtmlUtils.center(
                    HtmlUtils.b(headerSdf.format(cal.getTime()))));
            sb.append(
                "<table class=\"calendartable\" border=\"1\" cellspacing=\"0\" cellpadding=\"0\" width=\"100%\">");
            String[] dayNames = {
                "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"
            };
            sb.append("<tr>");
            for (int colIdx = 0; colIdx < 7; colIdx++) {
                sb.append("<td width=\"14%\" class=\"calheader\">"
                          + msg(dayNames[colIdx]) + "</td>");
            }
            sb.append("</tr>");
            int startDow = cal.get(cal.DAY_OF_WEEK);
            while (startDow > 1) {
                cal.add(cal.DAY_OF_MONTH, -1);
                startDow--;
            }
            for (int rowIdx = 0; rowIdx < 6; rowIdx++) {
                sb.append("<tr valign=top>");
                for (int colIdx = 0; colIdx < 7; colIdx++) {
                    String content   = HtmlUtils.space(1);
                    String bg        = "";
                    int    thisDay   = cal.get(cal.DAY_OF_MONTH);
                    int    thisMonth = cal.get(cal.MONTH);
                    int    thisYear  = cal.get(cal.YEAR);
                    String key = thisYear + "/" + thisMonth + "/" + thisDay;
                    List   inner     = map.get(key);
                    request.put(ARG_MONTH, "" + thisMonth);
                    request.put(ARG_YEAR, "" + thisYear);
                    request.put(ARG_DAY, "" + thisDay);
                    String dayUrl = request.getUrl();
                    if (thisMonth != selected[IDX_MONTH]) {
                        bg = " style=\"background-color:lightgray;\"";
                    } else if ((today[IDX_DAY] == thisDay)
                               && (today[IDX_MONTH] == thisMonth)
                               && (today[IDX_YEAR] == thisYear)) {
                        bg = " style=\"background-color:lightblue;\"";
                    }
                    String dayContents = "&nbsp;";
                    if (inner != null) {
                        dayContents = HtmlUtils.div(StringUtil.join("",
                                inner), HtmlUtils.cssClass("calcontents"));
                    }
                    content =
                        "<table border=0 cellspacing=\"0\" cellpadding=\"2\" width=100%><tr valign=top><td>"
                        + dayContents
                        + "</td><td align=right class=calday width=5>"
                        + HtmlUtils.href(dayUrl, "" + thisDay)
                        + "<br>&nbsp;</td></tr></table>";
                    content =
                        HtmlUtils.div(content,
                                      HtmlUtils.cssClass("cal-daywrapper"));
                    content = HtmlUtils.div(
                        HtmlUtils.href(dayUrl, "" + thisDay),
                        HtmlUtils.cssClass("cal-day-link")) + HtmlUtils.div(
                            dayContents, HtmlUtils.cssClass("cal-day"));

                    sb.append("<td width=15% class=\"calentry\" " + bg + " >"
                              + content + "</td>");
                    cal.add(cal.DAY_OF_MONTH, 1);
                }
                if ((cal.get(cal.YEAR) >= selected[IDX_YEAR])
                        && (cal.get(cal.MONTH) > selected[IDX_MONTH])) {
                    break;
                }
            }

            sb.append("</table>");
        }

        request.remove(ARG_DAY);
        request.remove(ARG_MONTH);
        request.remove(ARG_YEAR);

        //        return new Result(msg("Calendar"), sb);
    }

}
