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

package org.ramadda.repository;


import org.ramadda.repository.auth.Permission;
import org.ramadda.repository.auth.User;

import org.ramadda.repository.map.MapInfo;
import org.ramadda.repository.map.MapLayer;
import org.ramadda.repository.metadata.ContentMetadataHandler;
import org.ramadda.repository.metadata.Metadata;
import org.ramadda.repository.output.CalendarOutputHandler;
import org.ramadda.repository.output.HtmlOutputHandler;
import org.ramadda.repository.output.OutputHandler;
import org.ramadda.repository.output.OutputType;
import org.ramadda.repository.output.PageStyle;
import org.ramadda.repository.type.TypeHandler;
import org.ramadda.util.CategoryBuffer;
import org.ramadda.util.HtmlTemplate;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JQuery;
import org.ramadda.util.MapRegion;

import org.ramadda.util.Utils;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;


import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;

import java.net.URL;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;


/**
 * The main class.
 *
 */
public class DateHandler extends RepositoryManager {


    /** _more_ */
    public static final String DEFAULT_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss z";

    /** _more_ */
    public static final String DEFAULT_TIME_SHORTFORMAT = "yyyy/MM/dd";

    /** _more_ */
    public static final String DEFAULT_TIME_THISYEARFORMAT =
        "yyyy/MM/dd HH:mm z";


    /** _more_ */
    private TimeZone displayTimeZone;


    /** _more_ */
    private SimpleDateFormat displaySdf;

    /** _more_ */
    private SimpleDateFormat yyyymmddSdf;


    /** _more_ */
    private SimpleDateFormat dateSdf =
        RepositoryUtil.makeDateFormat("yyyy-MM-dd");

    /** _more_ */
    private SimpleDateFormat timeSdf =
        RepositoryUtil.makeDateFormat("HH:mm:ss z");

    /** _more_ */
    private String shortDateFormat;

    /** _more_ */
    private Hashtable<String, SimpleDateFormat> dateFormats =
        new Hashtable<String, SimpleDateFormat>();


    /** _more_ */
    protected List<SimpleDateFormat> parseFormats;



    /**
     * _more_
     *
     * @param repository _more_
     */
    public DateHandler(Repository repository) {
        super(repository);
    }



    /**
     * _more_
     */
    @Override
    public void initAttributes() {
        super.initAttributes();
        dateFormats = new Hashtable<String, SimpleDateFormat>();
        shortDateFormat = getRepository().getProperty(PROP_DATE_SHORTFORMAT,
                DEFAULT_TIME_SHORTFORMAT);
        String tz = getRepository().getProperty(PROP_TIMEZONE, "UTC");
        displayTimeZone = TimeZone.getTimeZone(tz);
        displaySdf =
            RepositoryUtil.makeDateFormat(getDefaultDisplayDateFormat(),
                                          displayTimeZone);
        yyyymmddSdf = RepositoryUtil.makeDateFormat("yyyy-MM-dd");

        TimeZone.setDefault(RepositoryUtil.TIMEZONE_DEFAULT);
    }




    /**
     * _more_
     *
     * @param sb _more_
     * @param date _more_
     * @param url _more_
     * @param dayLinks _more_
     *
     * @throws Exception _more_
     */
    public void createMonthNav(Appendable sb, Date date, String url,
                               Hashtable dayLinks)
            throws Exception {

        GregorianCalendar cal =
            new GregorianCalendar(RepositoryUtil.TIMEZONE_DEFAULT);
        cal.setTime(date);
        int[] theDate  = CalendarOutputHandler.getDayMonthYear(cal);
        int   theDay   = cal.get(cal.DAY_OF_MONTH);
        int   theMonth = cal.get(cal.MONTH);
        int   theYear  = cal.get(cal.YEAR);
        while (cal.get(cal.DAY_OF_MONTH) > 1) {
            cal.add(cal.DAY_OF_MONTH, -1);
        }
        GregorianCalendar prev =
            new GregorianCalendar(RepositoryUtil.TIMEZONE_DEFAULT);
        prev.setTime(date);
        prev.add(cal.MONTH, -1);
        GregorianCalendar next =
            new GregorianCalendar(RepositoryUtil.TIMEZONE_DEFAULT);
        next.setTime(date);
        next.add(cal.MONTH, 1);

        HtmlUtils.open(sb, HtmlUtils.TAG_TABLE, HtmlUtils.ATTR_BORDER, "1",
                       HtmlUtils.ATTR_CELLSPACING, "0",
                       HtmlUtils.ATTR_CELLPADDING, "0");
        String[] dayNames = {
            "Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"
        };
        String prevUrl = HtmlUtils.space(1)
                         + HtmlUtils.href(
                             url + "&"
                             + CalendarOutputHandler.getUrlArgs(
                                 prev), "&lt;");
        String nextUrl =
            HtmlUtils.href(
                url + "&" + CalendarOutputHandler.getUrlArgs(next),
                HtmlUtils.ENTITY_GT) + HtmlUtils.space(1);
        HtmlUtils.open(sb, HtmlUtils.TAG_TR, HtmlUtils.ATTR_VALIGN,
                       HtmlUtils.VALUE_TOP);
        HtmlUtils.open(sb, HtmlUtils.TAG_TD, HtmlUtils.ATTR_COLSPAN, "7",
                       HtmlUtils.ATTR_ALIGN, HtmlUtils.VALUE_CENTER,
                       HtmlUtils.ATTR_CLASS, "calnavmonthheader");

        HtmlUtils.open(sb, HtmlUtils.TAG_TABLE, "class", "calnavtable",
                       HtmlUtils.ATTR_CELLSPACING, "0",
                       HtmlUtils.ATTR_CELLPADDING, "0", HtmlUtils.ATTR_WIDTH,
                       "100%");
        HtmlUtils.open(sb, HtmlUtils.TAG_TR);
        HtmlUtils.col(sb, prevUrl,
                      HtmlUtils.attrs(HtmlUtils.ATTR_WIDTH, "1",
                                      HtmlUtils.ATTR_CLASS,
                                      "calnavmonthheader"));
        HtmlUtils.col(
            sb, DateUtil.MONTH_NAMES[cal.get(cal.MONTH)] + HtmlUtils.space(1)
            + theYear, HtmlUtils.attr(
                HtmlUtils.ATTR_CLASS, "calnavmonthheader"));

        HtmlUtils.col(sb, nextUrl,
                      HtmlUtils.attrs(HtmlUtils.ATTR_WIDTH, "1",
                                      HtmlUtils.ATTR_CLASS,
                                      "calnavmonthheader"));
        HtmlUtils.close(sb, HtmlUtils.TAG_TABLE);
        HtmlUtils.close(sb, HtmlUtils.TAG_TR);
        HtmlUtils.open(sb, HtmlUtils.TAG_TR);
        for (int colIdx = 0; colIdx < 7; colIdx++) {
            HtmlUtils.col(sb, dayNames[colIdx],
                          HtmlUtils.attrs(HtmlUtils.ATTR_WIDTH, "14%",
                                          HtmlUtils.ATTR_CLASS,
                                          "calnavdayheader"));
        }
        HtmlUtils.close(sb, HtmlUtils.TAG_TR);
        int startDow = cal.get(cal.DAY_OF_WEEK);
        while (startDow > 1) {
            cal.add(cal.DAY_OF_MONTH, -1);
            startDow--;
        }
        for (int rowIdx = 0; rowIdx < 6; rowIdx++) {
            HtmlUtils.open(sb, HtmlUtils.TAG_TR, HtmlUtils.ATTR_VALIGN,
                           HtmlUtils.VALUE_TOP);
            for (int colIdx = 0; colIdx < 7; colIdx++) {
                int     thisDay    = cal.get(cal.DAY_OF_MONTH);
                int     thisMonth  = cal.get(cal.MONTH);
                int     thisYear   = cal.get(cal.YEAR);
                boolean currentDay = false;
                String  dayClass   = "calnavday";
                if (thisMonth != theMonth) {
                    dayClass = "calnavoffday";
                } else if ((theMonth == thisMonth) && (theYear == thisYear)
                           && (theDay == thisDay)) {
                    dayClass   = "calnavtheday";
                    currentDay = true;
                }
                String content;
                if (dayLinks != null) {
                    String key = thisYear + "/" + thisMonth + "/" + thisDay;
                    if (dayLinks.get(key) != null) {
                        content = HtmlUtils.href(url + "&"
                                + CalendarOutputHandler.getUrlArgs(cal), ""
                                    + thisDay);
                        if ( !currentDay) {
                            dayClass = "calnavoffday";
                        }
                    } else {
                        content  = "" + thisDay;
                        dayClass = "calnavday";
                    }
                } else {
                    content = HtmlUtils.href(
                        url + "&" + CalendarOutputHandler.getUrlArgs(cal),
                        "" + thisDay);
                }

                sb.append(HtmlUtils.col(content,
                                        HtmlUtils.cssClass(dayClass)));
                sb.append("\n");
                cal.add(cal.DAY_OF_MONTH, 1);
            }
            if (cal.get(cal.MONTH) > theMonth) {
                break;
            }
            if (cal.get(cal.YEAR) > theYear) {
                break;
            }
        }
        sb.append(HtmlUtils.close(HtmlUtils.TAG_TABLE));

    }



    /**
     * _more_
     *
     * @param request The request
     * @param name _more_
     * @param formName _more_
     * @param date _more_
     *
     * @return _more_
     */
    public String makeDateInput(Request request, String name,
                                String formName, Date date) {
        return makeDateInput(request, name, formName, date, null);
    }

    /**
     * _more_
     *
     * @param request The request
     * @param name _more_
     * @param formName _more_
     * @param date _more_
     * @param timezone _more_
     *
     * @return _more_
     */
    public String makeDateInput(Request request, String name,
                                String formName, Date date, String timezone) {
        return makeDateInput(request, name, formName, date, timezone, true);
    }

    /**
     * Make the HTML for a date input widget
     *
     * @param request The request
     * @param name    the name
     * @param formName  the form name
     * @param date      the default date
     * @param timezone  the timezone
     * @param includeTime  true to include a time box
     *
     * @return  the widget html
     */
    public String makeDateInput(Request request, String name,
                                String formName, Date date, String timezone,
                                boolean includeTime) {
        return makeDateInput(request, name, formName, date, timezone,
                             includeTime, null);
    }

    /**
     * Make the HTML for a date input widget
     *
     * @param request The request
     * @param name    the name
     * @param formName  the form name
     * @param date      the default date
     * @param timezone  the timezone
     * @param includeTime  true to include a time box
     * @param dates _more_
     *
     * @return  the widget html
     */
    public String makeDateInput(Request request, String name,
                                String formName, Date date, String timezone,
                                boolean includeTime, List dates) {
        String dateHelp = "e.g., yyyy-mm-dd,  now, -1 week, +3 days, etc.";
        String           timeHelp   = "hh:mm:ss Z, e.g. 20:15:00 MST";

        String           dateArg    = request.getString(name, "");
        String           timeArg    = request.getString(name + ".time", "");
        String           dateString = ((date == null)
                                       ? dateArg
                                       : dateSdf.format(date));
        SimpleDateFormat timeFormat = ((timezone == null)
                                       ? timeSdf
                                       : getSDF("HH:mm:ss z", timezone));
        String           timeString = ((date == null)
                                       ? timeArg
                                       : timeFormat.format(date));

        String           inputId    = HtmlUtils.getUniqueId("dateinput");
        String           minDate    = null;
        String           maxDate    = null;
        if ((dates != null) && !dates.isEmpty()) {
            minDate = dateSdf.format((Date) dates.get(0));
            maxDate = dateSdf.format((Date) dates.get(dates.size() - 1));
        }

        StringBuilder jsBuf =
            new StringBuilder("<script>jQuery(function() {$( ");
        HtmlUtils.squote(jsBuf, "#" + inputId);
        jsBuf.append(
            " ).datepicker({ dateFormat: 'yy-mm-dd',changeMonth: true, changeYear: true,constrainInput:false, yearRange: '1900:2100' ");
        if ((minDate != null) && (maxDate != null)) {
            jsBuf.append(", minDate: '" + minDate + "', maxDate: '" + maxDate
                         + "'");
        }
        jsBuf.append(" });});</script>");
        String extra = "";
        if (includeTime) {
            extra = " T:"
                    + HtmlUtils.input(name + ".time", timeString,
                                      HtmlUtils.sizeAttr(6)
                                      + HtmlUtils.attr(HtmlUtils.ATTR_TITLE,
                                          timeHelp));
        }

        return jsBuf.toString() + "\n"
               + HtmlUtils.input(name, dateString,
                                 HtmlUtils.SIZE_10 + HtmlUtils.id(inputId)
                                 + HtmlUtils.title(dateHelp)) + extra;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getShortDateFormat() {
        return shortDateFormat;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public TimeZone getDisplayTimeZone() {
        return displayTimeZone;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public String getDefaultDisplayDateFormat() {
        return getRepository().getProperty(PROP_DATEFORMAT,
                                           DEFAULT_TIME_FORMAT);
    }


    /**
     * _more_
     *
     * @param entry _more_
     * @param format _more_
     *
     * @return _more_
     *
     */
    public SimpleDateFormat getDateFormat(Entry entry, String format) {
        try {
            if (format == null) {
                format = getDefaultDisplayDateFormat();
            }
            String tz = getEntryUtil().getTimezone(entry);

            return getSDF(format, tz);
        } catch (Exception exc) {
            throw new IllegalArgumentException(exc);
        }

    }


    /**
     * _more_
     *
     * @param entry _more_
     * @param date _more_
     * @param format _more_
     *
     * @return _more_
     *
     */
    public String formatDate(Entry entry, Date date, String format) {
        return getDateFormat(entry, format).format(date);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param ms _more_
     *
     * @return _more_
     */
    public String formatDate(Request request, Entry entry, long ms) {
        return formatDate(request, entry, new Date(ms));
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param d _more_
     *
     * @return _more_
     */
    public String formatDate(Request request, Entry entry, Date d) {
        return formatDate(entry, d, null);
    }


    /**
     * _more_
     *
     * @param format _more_
     *
     * @return _more_
     */
    private SimpleDateFormat getSDF(String format) {
        return getSDF(format, null);
    }


    /**
     * _more_
     *
     * @param format _more_
     * @param timezone _more_
     *
     * @return _more_
     */
    private SimpleDateFormat getSDF(String format, String timezone) {
        String key;
        if (timezone != null) {
            key = format + "-" + timezone;
        } else {
            key = format;
        }
        SimpleDateFormat sdf = dateFormats.get(key);
        if (sdf == null) {
            sdf = new SimpleDateFormat();
            sdf.applyPattern(format);
            if (Utils.stringDefined(timezone)
                    && !timezone.equals("default")) {
                sdf.setTimeZone(TimeZone.getTimeZone(timezone));
            } else {
                sdf.setTimeZone(displayTimeZone);
            }
            dateFormats.put(key, sdf);
        }

        return sdf;
    }




    /**
     * _more_
     *
     * @param d _more_
     *
     * @return _more_
     */
    public String formatDate(Date d) {
        return formatDate(d, null);
    }

    /**
     * _more_
     *
     * @param date _more_
     *
     * @return _more_
     */
    public String formatYYYYMMDD(Date date) {
        synchronized (yyyymmddSdf) {
            return yyyymmddSdf.format(date);
        }
    }



    /**
     * _more_
     *
     * @param d _more_
     * @param timezone _more_
     *
     * @return _more_
     */
    public String formatDate(Date d, String timezone) {
        if (d == null) {
            return BLANK;
        }

        if (displaySdf == null) {
            displaySdf = getSDF(getDefaultDisplayDateFormat());
        }

        SimpleDateFormat dateFormat = ((timezone == null)
                                       ? displaySdf
                                       : getSDF(
                                           getDefaultDisplayDateFormat(),
                                           timezone));

        synchronized (dateFormat) {
            return dateFormat.format(d);
        }
    }


    /**
     * _more_
     *
     * @param request The request
     * @param ms _more_
     *
     * @return _more_
     */
    public String formatDate(Request request, long ms) {
        return formatDate(new Date(ms));
    }


    /**
     * _more_
     *
     * @param request The request
     * @param ms _more_
     * @param timezone _more_
     *
     * @return _more_
     */
    public String formatDate(Request request, long ms, String timezone) {
        return formatDate(new Date(ms), timezone);
    }

    /**
     * _more_
     *
     * @param request The request
     * @param d _more_
     *
     * @return _more_
     */
    public String formatDate(Request request, Date d) {
        return formatDate(d);
    }


    /**
     * _more_
     *
     * @param request The request
     * @param d _more_
     * @param timezone _more_
     *
     * @return _more_
     */
    public String formatDate(Request request, Date d, String timezone) {
        return formatDate(d, timezone);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param ms _more_
     *
     * @return _more_
     */
    public String formatDateShort(Request request, Entry entry, long ms) {
        return formatDateShort(request, entry, new Date(ms));
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param date _more_
     *
     * @return _more_
     */
    public String formatDateShort(Request request, Entry entry, Date date) {
        return formatDateShort(request, date,
                               getEntryUtil().getTimezone(entry));
    }

    /**
     * _more_
     *
     * @param request The request
     * @param d _more_
     * @param timezone _more_
     *
     * @return _more_
     */
    public String formatDateShort(Request request, Date d, String timezone) {
        return formatDateShort(request, d, timezone, "");
    }

    /**
     * _more_
     *
     * @param request The request
     * @param d _more_
     * @param timezone _more_
     * @param extraAlt _more_
     *
     * @return _more_
     */
    public String formatDateShort(Request request, Date d, String timezone,
                                  String extraAlt) {
        if (d == null) {
            return BLANK;
        }


        SimpleDateFormat sdf      = getSDF(getShortDateFormat(), timezone);

        Date             now      = new Date();
        long             diff     = now.getTime() - d.getTime();
        double           minutes  = DateUtil.millisToMinutes(diff);
        String           fullDate = formatDate(d, timezone);
        String           result;
        if ((minutes > 0) && (minutes < 65) && (minutes > 55)) {
            result = "about an hour ago";
        } else if ((diff > 0) && (diff < DateUtil.minutesToMillis(1))) {
            result = (int) (diff / (1000)) + " seconds ago";
        } else if ((diff > 0) && (diff < DateUtil.hoursToMillis(1))) {
            int value = (int) DateUtil.millisToMinutes(diff);
            result = value + " minute" + ((value > 1)
                                          ? "s"
                                          : "") + " ago";
        } else if ((diff > 0) && (diff < DateUtil.hoursToMillis(24))) {
            int value = (int) (diff / (1000 * 60 * 60));
            result = value + " hour" + ((value > 1)
                                        ? "s"
                                        : "") + " ago";
        } else if ((diff > 0) && (diff < DateUtil.daysToMillis(6))) {
            int value = (int) (diff / (1000 * 60 * 60 * 24));
            result = value + " day" + ((value > 1)
                                       ? "s"
                                       : "") + " ago";
        } else {
            result = sdf.format(d);
        }

        return HtmlUtils.span(result,
                              HtmlUtils.cssClass(CSS_CLASS_DATETIME)
                              + HtmlUtils.attr(HtmlUtils.ATTR_TITLE,
                                  fullDate + extraAlt));
    }





    /**
     * _more_
     *
     * @param dttm _more_
     *
     * @return _more_
     *
     * @throws java.text.ParseException _more_
     */
    public Date parseDate(String dttm) throws java.text.ParseException {
        if ( !Utils.stringDefined(dttm)) {
            return null;
        }
        if (parseFormats == null) {
            parseFormats = new ArrayList<SimpleDateFormat>();
            parseFormats.add(
                RepositoryUtil.makeDateFormat("yyyy-MM-dd HH:mm:ss z"));
            parseFormats.add(
                RepositoryUtil.makeDateFormat("yyyy-MM-dd HH:mm:ss"));
            parseFormats.add(
                RepositoryUtil.makeDateFormat("yyyy-MM-dd HH:mm"));
            parseFormats.add(RepositoryUtil.makeDateFormat("yyyy-MM-dd"));
        }


        for (SimpleDateFormat fmt : parseFormats) {
            try {
                synchronized (fmt) {
                    return fmt.parse(dttm);
                }
            } catch (Exception noop) {}
        }

        throw new IllegalArgumentException("Unable to parse date:" + dttm);
    }


}
