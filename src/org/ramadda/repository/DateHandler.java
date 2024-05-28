/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository;


import org.ramadda.repository.output.CalendarOutputHandler;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;

import ucar.unidata.util.DateUtil;

import java.text.SimpleDateFormat;

import java.util.ArrayList;


import java.time.LocalDate;
import java.time.Period;

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

    public static final long NULL_DATE=-9999999L;
    public static final String NULL_DATE_LABEL="NA";


    /** _more_ */
    public static final String DEFAULT_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss z";

    /** _more_ */
    public static final String DEFAULT_TIME_SHORTFORMAT = "yyyy/MM/dd";

    /** _more_ */
    public static final String DEFAULT_TIME_THISYEARFORMAT =
        "yyyy/MM/dd HH:mm z";


    /** _more_ */
    private static TimeZone displayTimeZone;


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
    private static Hashtable<String, SimpleDateFormat> dateFormats =
        new Hashtable<String, SimpleDateFormat>();


    /** _more_ */
    protected static List<SimpleDateFormat> parseFormats;


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
        //      Repository.propdebug = true;
        //      System.err.println("getting timezone property");
        String tz = getRepository().getProperty(PROP_TIMEZONE, "UTC");
        //      Repository.propdebug = false;
        //      System.err.println("timezone:" + tz);
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

        HU.open(sb, HU.TAG_TABLE, HU.ATTR_BORDER, "1", HU.ATTR_CELLSPACING,
                "0", HU.ATTR_CELLPADDING, "0");
        String[] dayNames = {
            "Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"
        };

        String prevUrl =
            HU.SPACE
            + HU.href(url + "&" + CalendarOutputHandler.getUrlArgs(prev),
                      HU.getIconImage("fas fa-caret-left"));
        String nextUrl =
            HU.href(url + "&" + CalendarOutputHandler.getUrlArgs(next),
                    HU.getIconImage("fas fa-caret-right")) + HU.SPACE;
        HU.open(sb, HU.TAG_TR, HU.ATTR_VALIGN, HU.VALUE_TOP);
        HU.open(sb, HU.TAG_TD, HU.ATTR_COLSPAN, "7", HU.ATTR_ALIGN,
                HU.VALUE_CENTER, HU.ATTR_CLASS, "calnavmonthheader");

        HU.open(sb, HU.TAG_TABLE, "class", "calnavtable",
                HU.ATTR_CELLSPACING, "0", HU.ATTR_CELLPADDING, "0",
                HU.ATTR_WIDTH, "100%");
        HU.open(sb, HU.TAG_TR);
        HU.col(sb, prevUrl,
               HU.attrs("nowrap", "true", HU.ATTR_WIDTH, "1", HU.ATTR_CLASS,
                        "calnavmonthheader"));
        int month = cal.get(cal.MONTH);
        String monthHref = HU.href(url + "&month=" + month + "&year="
                                   + theYear, DateUtil.MONTH_NAMES[month]
                                       + HU.SPACE + theYear);
        HU.col(sb, monthHref, HU.attr(HU.ATTR_CLASS, "calnavmonthheader"));

        HU.col(sb, nextUrl,
               HU.attrs("nowrap", "true", HU.ATTR_WIDTH, "1", HU.ATTR_CLASS,
                        "calnavmonthheader"));
        HU.close(sb, HU.TAG_TABLE);
        HU.close(sb, HU.TAG_TR);
        HU.open(sb, HU.TAG_TR);
        for (int colIdx = 0; colIdx < 7; colIdx++) {
            HU.col(sb, dayNames[colIdx],
                   HU.attrs(HU.ATTR_WIDTH, "14%", HU.ATTR_CLASS,
                            "calnavdayheader"));
        }
        HU.close(sb, HU.TAG_TR);
        int startDow = cal.get(cal.DAY_OF_WEEK);
        while (startDow > 1) {
            cal.add(cal.DAY_OF_MONTH, -1);
            startDow--;
        }
        for (int rowIdx = 0; rowIdx < 6; rowIdx++) {
            HU.open(sb, HU.TAG_TR, HU.ATTR_VALIGN, HU.VALUE_TOP);
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
                        content = HU.href(
                            url + "&"
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
                    content =
                        HU.href(url + "&"
                                + CalendarOutputHandler.getUrlArgs(cal), ""
                                    + thisDay);
                }

                sb.append(HU.col(content, HU.cssClass(dayClass)));
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
        sb.append(HU.close(HU.TAG_TABLE));

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
	date = checkDate(date);
        String           dateString = ((date == null)
                                       ? dateArg
                                       : doFormat(date, dateSdf));
        SimpleDateFormat timeFormat = ((timezone == null)
                                       ? timeSdf
                                       : getSDF("HH:mm:ss z", timezone));
        String           timeString = ((date == null)
                                       ? timeArg
                                       : doFormat(date, timeFormat));

        String           inputId    = HU.getUniqueId("dateinput");
        String           minDate    = null;
        String           maxDate    = null;
        if ((dates != null) && !dates.isEmpty()) {
            minDate = doFormat((Date) dates.get(0), dateSdf);
            maxDate = doFormat((Date) dates.get(dates.size() - 1), dateSdf);
        }

        StringBuilder jsBuf =
            new StringBuilder("<script>jQuery(function() {$( ");
        HU.squote(jsBuf, "#" + inputId);
        jsBuf.append(
            " ).datepicker(HtmlUtils.makeClearDatePickerArgs({ dateFormat: 'yy-mm-dd',changeMonth: true, changeYear: true,constrainInput:false, yearRange: '1900:2100' ");
        if ((minDate != null) && (maxDate != null)) {
            jsBuf.append(", minDate: '" + minDate + "', maxDate: '" + maxDate
                         + "'");
        }
        jsBuf.append(" }),);});</script>");
        String extra = "";
        if (includeTime) {
            extra = " T:"
                    + HU.input(name + ".time", timeString,
                               HU.attr("placeholder", "hh:mm:ss TZ")
                               + HU.sizeAttr(10)
                               + HU.attr(HU.ATTR_TITLE, timeHelp));
        }

        return jsBuf.toString() + "\n"
               + HU.input(name, dateString,
                          HU.attr("placeholder", "yyyy-MM-dd") + HU.SIZE_10
                          + HU.id(inputId) + HU.title(dateHelp)) + extra;
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
        return getRepository().getProperty(PROP_DATE_FORMAT,
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
    public SimpleDateFormat getDateFormat(Request request, Entry entry, String format) {
        try {
            if (format == null) {
		format=entry.getTypeHandler().getTypeProperty("date.format",null);
	    }

            if (format == null) {
                format = getDefaultDisplayDateFormat();
            }
            String tz = getEntryUtil().getTimezone(request,entry);
            return getSDF(format, tz);
        } catch (Exception exc) {
            throw new IllegalArgumentException(exc);
        }

    }


    public static int getYearsBetween(Date d1,Date d2) {
	LocalDate localDate1 = d1.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        LocalDate localDate2 = d2.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();

        // Calculate the period between two dates
        Period period = Period.between(localDate1, localDate2);

        // Get the difference in years
        int years = period.getYears();
	return years;
    }

    public static long getTime(Date date) {
	if(date==null) return NULL_DATE;
	return date.getTime();
    }

    //If date.getTime()==NULL_DATE then return null
    public static Date checkDate(Date date) {
	if(isNullDate(date)) return null;
	return date;
    }

    public static boolean isNullDate(Date date) {
	if(date!=null && date.getTime()==NULL_DATE) return true;
	return date==null;
    }

    public static boolean isNullDate(long date) {
	if(date==NULL_DATE) return true;
	return false;
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
    public String formatDate(Request request, Entry entry, Date date, String format) {
        return doFormat(date, getDateFormat(request,entry, format));
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
	if(ms==NULL_DATE) return NULL_DATE_LABEL;
        return formatDate(request, entry, new Date(ms));
    }

    public String formatDateWithMacro(Request request, Entry entry, long d,Utils.Macro macro) {
        return formatDate(request,entry, new Date(d), macro.getProperty("format",null));
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
        return formatDate(request,entry, d, null);
    }


    /**
     * _more_
     *
     * @param format _more_
     *
     * @return _more_
     */
    public SimpleDateFormat getSDF(String format) {
        return getSDF(format, null);
    }


    public SimpleDateFormat getSDF(String format, String timezone) {
	return getSDF(format, timezone, true);
    }


    /**
     * _more_
     *
     * @param format _more_
     * @param timezone _more_
     *
     * @return _more_
     */
    public  static  SimpleDateFormat getSDF(String format, String timezone,boolean shared) {
        SimpleDateFormat sdf = null;
        String key=null;
	if(shared) {
	    if (timezone != null) {
		key = format + "-" + timezone;
	    } else {
		key = format;
	    }
	    sdf = dateFormats.get(key);
	}

        if (sdf == null) {
            sdf = new SimpleDateFormat();
            sdf.applyPattern(format);
            if (Utils.stringDefined(timezone)
                    && !timezone.equals("default")) {
                sdf.setTimeZone(TimeZone.getTimeZone(timezone));
            } else {
                sdf.setTimeZone(displayTimeZone);
            }
	    if(shared) {
		dateFormats.put(key, sdf);
	    }
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
        return doFormat(date, yyyymmddSdf);
    }

    /**
     * _more_
     *
     * @param date _more_
     * @param sdf _more_
     *
     * @return _more_
     */
    private String doFormat(Date date, SimpleDateFormat sdf) {
        synchronized (sdf) {
            return sdf.format(date);
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

        return doFormat(d, dateFormat);
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
                               getEntryUtil().getTimezone(request, entry));
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
	d = checkDate(d);
        if (d == null) {
            return NULL_DATE_LABEL;
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
            result = doFormat(d, sdf);
        }


        return HU.span(result,
                       HU.cssClass(CSS_CLASS_DATETIME)
                       + HU.attr(HU.ATTR_TITLE, fullDate + extraAlt));
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
            List<SimpleDateFormat> tmp = new ArrayList<SimpleDateFormat>();
            tmp.add(RepositoryUtil.makeDateFormat("yyyy-MM-dd HH:mm:ss z"));
            tmp.add(RepositoryUtil.makeDateFormat("yyyy-MM-dd HH:mm:ss"));
            tmp.add(RepositoryUtil.makeDateFormat("yyyy-MM-dd HH:mm"));
            tmp.add(RepositoryUtil.makeDateFormat("yyyy-MM-dd"));
            tmp.add(RepositoryUtil.makeDateFormat("yyyy-MM"));
            tmp.add(RepositoryUtil.makeDateFormat("yyyy"));	    
            parseFormats = tmp;
	    
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
