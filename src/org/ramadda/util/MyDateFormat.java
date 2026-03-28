/**
Copyright (c) 2008-2026 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;

import java.text.ParseException;
import java.text.ParsePosition;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.TimeZone;

public class MyDateFormat {
    private final String format;
    private final TimeZone timezone;
    private final ZoneId zoneId;
    private final DateTimeFormatter formatter;

    public MyDateFormat(String format) {
        this(format, Utils.TIMEZONE_DEFAULT);
    }

    public MyDateFormat(String format, TimeZone timezone) {
        this.format = format;
        this.timezone = timezone != null ? timezone : Utils.TIMEZONE_DEFAULT;
        this.zoneId = this.timezone.toZoneId();
	//        this.formatter = DateTimeFormatter.ofPattern(format).withZone(zoneId);
        this.formatter = makeFormatter(format,zoneId);
    }

    private static DateTimeFormatter makeFormatter(String pattern, ZoneId zoneId) {
	DateTimeFormatter formatter;
	if ("uuuu".equals(pattern) || "yyyy".equals(pattern)) {
	    formatter = new DateTimeFormatterBuilder()
		.appendValue(ChronoField.YEAR, 1, 10, SignStyle.NORMAL)
		.toFormatter();
	} else if(pattern.equals("SSS") ||
		  pattern.equals("sss") ||
		  pattern.equals("seconds")) {
	    return null;
	    //handled without the SDF
	} else {
	    String javaTimePattern = convertPattern(pattern);
	    formatter = DateTimeFormatter.ofPattern(javaTimePattern);
	}
	
	return formatter.withZone(zoneId);
    }

    private static String convertPattern(String pattern) {
	if (pattern == null) {
	    return null;
	}

	// If no era (G), treat yyyy as proleptic year
	if (pattern.indexOf('G') < 0) {
	    pattern = pattern.replace("yyyy", "uuuu");
	}

	return pattern;
    }



    public MyDateFormat(String format, String timezone) {
        this(format, makeTimeZone(timezone));
    }

    @Override
    public String toString() {
        return "dateformat:" + format;
    }

    private static TimeZone makeTimeZone(String timezone) {
        if (Utils.stringDefined(timezone)) {
            return TimeZone.getTimeZone(timezone);
        }
        return Utils.TIMEZONE_DEFAULT;
    }

    public String getFormat() {
        return format;
    }

    public String format(Date date) {
        if (date == null) {
            return null;
        }

        if (format.equals("SSS")) {
            return String.valueOf(date.getTime());
        }
        if (format.equals("sss")) {
            return String.valueOf(date.getTime() / 1000L);
        }
        if (format.equals("seconds")) {
            return String.valueOf(date.getTime());
        }

        Instant instant = date.toInstant();
        return formatter.format(instant.atZone(zoneId));
    }

    public Date parse(String s) throws ParseException {
        if (s == null) {
            return null;
        }

        try {
            if (format.equals("SSS")) {
                if (s.indexOf("E") >= 0) {
                    long l = (long) (Double.parseDouble(s) * 1000);
                    return new Date(l);
                }
                return new Date(Long.parseLong(s));
            } else if (format.equals("sss")) {
                if (s.indexOf("E") >= 0) {
                    return new Date((long) (Double.parseDouble(s) * 1000));
                }
                return new Date(Long.parseLong(s) * 1000);
            } else if (format.equals("seconds")) {
                if (s.indexOf("E") >= 0) {
                    return new Date((long) Double.parseDouble(s));
                }
                return new Date(Long.parseLong(s));
            }

            TemporalAccessor parsed = formatter.parse(s);
            return Date.from(toInstant(parsed));
        } catch (Exception e) {
            ParseException pe = new ParseException(e.getMessage(), 0);
            pe.initCause(e);
            throw pe;
        }
    }

    private Instant toInstant(TemporalAccessor parsed) {
        if (parsed.isSupported(ChronoField.INSTANT_SECONDS)) {
            return Instant.from(parsed);
        }

        if (parsed.isSupported(ChronoField.OFFSET_SECONDS)) {
            return ZonedDateTime.from(parsed).toInstant();
        }

        boolean hasYear = parsed.isSupported(ChronoField.YEAR);
        boolean hasMonth = parsed.isSupported(ChronoField.MONTH_OF_YEAR);
        boolean hasDay = parsed.isSupported(ChronoField.DAY_OF_MONTH);
        boolean hasHour = parsed.isSupported(ChronoField.HOUR_OF_DAY);
        boolean hasMinute = parsed.isSupported(ChronoField.MINUTE_OF_HOUR);
        boolean hasSecond = parsed.isSupported(ChronoField.SECOND_OF_MINUTE);

        if (hasYear && hasMonth && hasDay && hasHour) {
            LocalDateTime ldt = LocalDateTime.from(parsed);
            return ldt.atZone(zoneId).toInstant();
        }

        if (hasYear && hasMonth && hasDay) {
            LocalDate ld = LocalDate.from(parsed);
            return ld.atStartOfDay(zoneId).toInstant();
        }

        if (hasYear && hasMonth) {
            YearMonth ym = YearMonth.from(parsed);
            return ym.atDay(1).atStartOfDay(zoneId).toInstant();
        }

        if (hasYear) {
            Year y = Year.from(parsed);
            return y.atDay(1).atStartOfDay(zoneId).toInstant();
        }

        throw new DateTimeException("Cannot convert parsed value to an Instant: " + parsed);
    }
}
