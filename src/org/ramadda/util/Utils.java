/**
   Copyright (c) 2008-2025 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;

import org.apache.commons.lang3.text.StrTokenizer;
import org.apache.commons.text.StringTokenizer;

import org.w3c.dom.*;

import ucar.unidata.util.DateUtil;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;

import java.io.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import java.net.*;

import java.security.*;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * A collection of utilities
 *
 * @author Jeff McWhirter
 */

@SuppressWarnings("unchecked")
public class Utils extends IO {

    public static final String TIME_UNIT_SECOND = "second";
    public static final String TIME_UNIT_MINUTE = "minute";
    public static final String TIME_UNIT_HOUR = "hour";
    public static final String TIME_UNIT_DAY = "day";
    public static final String TIME_UNIT_WEEK = "week";
    public static final String TIME_UNIT_MONTH = "month";
    public static final String TIME_UNIT_YEAR = "year";

    /** _more_ */
    public static final String[] LETTERS = {
        "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N",
        "O", "P", "R", "S", "T", "U", "V", "W", "X", "Y", "Z",
    };

    public static final String[] MONTHS_LONG = {"January","February","March","April","May","June","July","August","September","October","November","December"};

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    public static final String ANSI_RED_BOLD = "\033[1;31m";    // RED
    public static final String ANSI_ORANGE_BOLD = "\033[1;38;5;208m";
    public static final String ANSI_GREEN_BOLD = "\033[1;32m";  // GREEN
    public static final String ANSI_YELLOW_BOLD = "\033[1;33m"; // YELLOW
    public static final String ANSI_BLUE_BOLD = "\033[1;34m";   // BLUE
    public static final String ANSI_PURPLE_BOLD = "\033[1;35m"; // PURPLE
    public static final String ANSI_CYAN_BOLD = "\033[1;36m";   // CYAN
    public static final String ANSI_WHITE_BOLD = "\033[1;37m";  // WHITE
    public static final String ANSI_BLACK_BOLD = "\033[1;30m";
    

    public static final String ANSI_BLACK_BACKGROUND = "\u001B[40m";
    public static final String ANSI_RED_BACKGROUND = "\u001B[41m";
    public static final String ANSI_GREEN_BACKGROUND = "\u001B[42m";
    public static final String ANSI_YELLOW_BACKGROUND = "\u001B[43m";
    public static final String ANSI_BLUE_BACKGROUND = "\u001B[44m";
    public static final String ANSI_PURPLE_BACKGROUND = "\u001B[45m";
    public static final String ANSI_CYAN_BACKGROUND = "\u001B[46m";
    public static final String ANSI_WHITE_BACKGROUND = "\u001B[47m";
    //    public static final String ANSI_LIGHTGRAY_BACKGROUND = "\033[47m";
    public static final String ANSI_LIGHTGRAY_BACKGROUND = "\033[48;5;250m";        


    public static String[] ANSI_COLOR_ARRAY = {
	Utils.ANSI_RED_BOLD,
	Utils.ANSI_ORANGE_BOLD,	    
	Utils.ANSI_YELLOW_BOLD,
	Utils.ANSI_BLUE_BOLD,
	Utils.ANSI_GREEN_BOLD,
	Utils.ANSI_PURPLE_BOLD,
	Utils.ANSI_CYAN_BOLD
    };

    public static String getAnsiColor(int cnt) {
	int index = cnt% ANSI_COLOR_ARRAY.length;
	//	System.out.println("CNT:" + cnt +" INDEX:" + index);
	return ANSI_COLOR_ARRAY[index]; 
    }


    private static DecimalFormat[] FORMATS = {
        new DecimalFormat("#0"), new DecimalFormat("#0.0"),
        new DecimalFormat("#0.00"), new DecimalFormat("#0.000"),
        new DecimalFormat("#0.0000"), new DecimalFormat("#0.00000"),
    };



    //From https://stackoverflow.com/questions/4731055/whitespace-matching-regex-java
    public static final String WHITESPACE_CHARS = ""
	/* dummy empty string for homogeneity */
	+ "\\u0009"  // CHARACTER TABULATION
	+ "\\u000A"  // LINE FEED (LF)
	+ "\\u000B"  // LINE TABULATION
	+ "\\u000C"  // FORM FEED (FF)
	+ "\\u000D"  // CARRIAGE RETURN (CR)
	+ "\\u0020"  // SPACE
	+ "\\u0085"  // NEXT LINE (NEL) 
	+ "\\u00A0"  // NO-BREAK SPACE
	+ "\\u1680"  // OGHAM SPACE MARK
	+ "\\u180E"  // MONGOLIAN VOWEL SEPARATOR
	+ "\\u2000"  // EN QUAD 
	+ "\\u2001"  // EM QUAD 
	+ "\\u2002"  // EN SPACE
	+ "\\u2003"  // EM SPACE
	+ "\\u2004"  // THREE-PER-EM SPACE
	+ "\\u2005"  // FOUR-PER-EM SPACE
	+ "\\u2006"  // SIX-PER-EM SPACE
	+ "\\u2007"  // FIGURE SPACE
	+ "\\u2008"  // PUNCTUATION SPACE
	+ "\\u2009"  // THIN SPACE
	+ "\\u200A"  // HAIR SPACE
	+ "\\u2028"  // LINE SEPARATOR
	+ "\\u2029"  // PARAGRAPH SEPARATOR
	+ "\\u202F"  // NARROW NO-BREAK SPACE
	+ "\\u205F"  // MEDIUM MATHEMATICAL SPACE
	+ "\\u3000"  // IDEOGRAPHIC SPACE
        ;

    public static final String WHITESPACE_CHARCLASS = "[" + WHITESPACE_CHARS
	+ "]";

    private static DecimalFormat INT_COMMA_FORMAT =  new DecimalFormat("#,##0");

    private static DecimalFormat[] COMMA_FORMATS = {
        new DecimalFormat("#,##0"), new DecimalFormat("#,##0.0"),
        new DecimalFormat("#,##0.00"), new DecimalFormat("#,##0.000"),
        new DecimalFormat("#,##0.0000"), new DecimalFormat("#,##0.00000"),
    };

    private static DecimalFormat INT_FORMAT = new DecimalFormat("#,##0");

    public static final TimeZone TIMEZONE_DEFAULT =
        TimeZone.getTimeZone("UTC");

    /** timezone */
    public static final TimeZone TIMEZONE_GMT = TimeZone.getTimeZone("GMT");
    public static final TimeZone TIMEZONE_UTC = TimeZone.getTimeZone("UTC");
    public static final SimpleDateFormat sdf;
    public static final SimpleDateFormat simpleSdf;
    public static final SimpleDateFormat isoSdf;

    static {
        sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        sdf.setTimeZone(TIMEZONE_DEFAULT);
        simpleSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        simpleSdf.setTimeZone(TIMEZONE_DEFAULT);
        isoSdf = new SimpleDateFormat("yyyy-MM-dd\'T\'HH:mm:ssZ");
        isoSdf.setTimeZone(TIMEZONE_DEFAULT);
    }

    public static String ansi(String ansi,Object text)  {
	return ansi + text + ANSI_RESET;
    }


    public static String getMonthName(int month) {
	if(month<0 || month>11) throw new IllegalArgumentException("Bad month:" + month);
	return MONTHS_LONG[month];
    }

    public static String formatFileLength(double bytes) {
        if (bytes < 0) {
            return "";
        }

        if (bytes < 5000) {
            return ((int) bytes) + " bytes";
        }
        if (bytes < 1000000) {
            bytes = ((int) ((bytes * 100) / 1000.0)) / 100.0;

            return ((int) bytes) + " KB";
        }
        bytes = ((int) ((bytes * 100) / 1000000.0)) / 100.0;

        return bytes + " MB";
    }

    public static String format(Date date) {
	//The sdf produces a time zone that isn't RFC3399 compatible so we just tack on the "Z"
	return format(sdf, date)+"Z";
    }

    //Formats in a synchronized block
    public static String format(SimpleDateFormat sdf,Date date) {
        synchronized (sdf) {
            return sdf.format(date);
        }
    }    

    //Formats in a synchronized block
    public static String format(SimpleDateFormat sdf,long date) {
        synchronized (sdf) {
            return sdf.format(date);
        }
    }    

    public static String formatIso(Date date) {
        synchronized (isoSdf) {
            //The sdf produces a time zone that isn't RFC3399 compatible so we just tack on the "Z"
            return isoSdf.format(date);
        }
    }

    public static String generatePassword(int length) throws Exception {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        chars = chars.toLowerCase() + chars + "0123456789";
        Random r        = new Random();
        String password = "";
        for (int i = 0; i < length; i++) {
            char c = chars.charAt(r.nextInt(chars.length()));
            password += c;
        }

        return password;
    }

    public static String format(double d) {
        if (d == (int) d) {
            return "" + (int) d;
        }

        return getFormat(d).format(d);
    }

    public static String getUrlArg(String urlString, String name)
	throws Exception {
        URL    url   = new URL(urlString);
        String query = url.getQuery();
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            String   key  = URLDecoder.decode(pair[0], "UTF-8");
            if (key.equals(name)) {
                return (pair.length > 1)
		    ? URLDecoder.decode(pair[1], "UTF-8")
		    : "";
            }
        }

        return null;
    }

    public static DecimalFormat getFormat(double d) {
        d = Math.abs(d);
        if ((d > 10000) || (d == 0)) {
            return FORMATS[0];
        }
        if (d > 1000) {
            return FORMATS[1];
        }
        if (d > 100) {
            return FORMATS[2];
        }
        if (d > 10) {
            return FORMATS[3];
        }
        if (d > 1) {
            return FORMATS[4];
        }

        return FORMATS[4];
    }

    public static String formatComma(double d) {
        return getFormatComma(d).format(d);
    }

    public static String intFormatComma(int i) {
	return INT_COMMA_FORMAT.format(i);
    }

    public static DecimalFormat getFormatComma(double d) {
        d = Math.abs(d);
        if ((d > 10000) || (d == 0)) {
            return COMMA_FORMATS[0];
        }
        if (d > 1000) {
            return COMMA_FORMATS[1];
        }
        if (d > 100) {
            return COMMA_FORMATS[2];
        }
        if (d > 10) {
            return COMMA_FORMATS[3];
        }
        if (d > 1) {
            return COMMA_FORMATS[4];
        }

        return COMMA_FORMATS[4];
    }

    public static double getUsedMemory() {
        double freeMemory  = (double) Runtime.getRuntime().freeMemory();
        double totalMemory = (double) Runtime.getRuntime().totalMemory();
        double usedMemory  = (totalMemory - freeMemory);
        int    i           = (int) (usedMemory / 1000000 * 10);

        return (double) (i / 10.0);
    }

    public static double decimals(double d, int decimals) {
        if (decimals == 0) {
            return (int) d;
        }
        int i = (int) (d * Math.pow(10, decimals));

        return (double) (i / Math.pow(10, decimals));

    }

    public static Appendable makeAppendable() {
        return new StringBuilder();
    }

    public static String appendList(String l, String c) {
        if (l == null) {
            l = "";
        } else if (l.length() > 0) {
            l += ",";
        }
        l += c;

        return l;
    }

    public static List add(List list, Object... args) {
        if (list == null) {
            list = new ArrayList();
        }
        for (Object s : args) {
            if (s != null) {
                list.add(s);
            }
        }

        return list;
    }

    public static Hashtable put(Hashtable map, Object... args) {
        for (int i = 0; i < args.length; i += 2) {
            map.put(args[i], args[i + 1]);
        }

        return map;
    }

    public static void print(Hashtable props) {
        for (Enumeration keys = props.keys(); keys.hasMoreElements(); ) {
            Object key   =  keys.nextElement();
            Object value = props.get(key);
            System.out.println("KEY:" +key +"=" + value+":");
        }	
    }	

    public static Appendable append(Appendable sb, Object... args) {
        try {
            for (Object s : args) {
                if (s != null) {
                    if (sb == null) {
                        sb = new StringBuilder();
                    }
                    sb.append(s.toString());
                }
            }

            return sb;
        } catch (java.io.IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    public static int indexOf(String s,String ...values) {
	for(String v: values) {
	    int index = s.indexOf(v);
	    if(index>=0) return index;
	}
	return -1;
    }

    public static String concatString(Object... args) {
        try {
            Appendable sb = makeAppendable();
            for (Object s : args) {
                sb.append((s != null)
                          ? s.toString()
                          : (String) null);
            }

            return sb.toString();
        } catch (java.io.IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    public static void concatBuff(Appendable sb, Object... args) {
        try {
            for (Object s : args) {
                sb.append((s != null)
                          ? s.toString()
                          : (String) null);
            }
        } catch (java.io.IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    public static List<List<String>> tokenize(String source,
					      String rowDelimiter, String columnDelimiter, int skip) {
        int                cnt     = 0;
        List<List<String>> results = new ArrayList<List<String>>();
        List<String> lines = Utils.split(source, rowDelimiter, true, true);
        for (String line : lines) {
            line = line.trim();
            if (line.length() == 0) {
                continue;
            }
            cnt++;
            if (cnt <= skip) {
                continue;
            }
            if (line.startsWith("#")) {
                continue;
            }
            results.add(tokenizeColumns(line, columnDelimiter));
        }

        return results;
    }

    public static boolean startsWithIgnoreCase(String s, String prefix) {
        return s.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    public static boolean matchesOrContains(String str, String pattern) {
	if(str==null || pattern==null) return false;
	str  = str.toLowerCase();
	try {
	    if (str.matches(pattern)) 
		return true;
	} catch(Exception ignore) {}
	if(str.indexOf(pattern) >= 0) {
	    return true;
	}
	return false;
    }

    public static List<String> tokenizeColumns(String line,
					       List<Integer> widths) {
        List<String> toks    = new ArrayList<String>();
        int          lastIdx = 0;
        for (int i = 0; i < widths.size(); i++) {
            int width = widths.get(i);
            if (lastIdx + width > line.length()) {
                break;
            }
            String theString = line.substring(lastIdx, lastIdx + width);
            toks.add(theString);
            lastIdx += width;
        }

        return toks;
    }

    public static List<String> tokenizeColumns(String line,
					       String columnDelimiter) {
        StringTokenizer tokenizer = StringTokenizer.getCSVInstance();
        tokenizer.setEmptyTokenAsNull(true);
        if ( !columnDelimiter.equals(",")) {
            tokenizer.setDelimiterString(columnDelimiter);
        }

        return tokenizeColumns(line, tokenizer);
    }

    public static StringTokenizer getTokenizer(String columnDelimiter) {
        StringTokenizer tokenizer = StringTokenizer.getCSVInstance();
        tokenizer.setEmptyTokenAsNull(true);
        if ( !columnDelimiter.equals(",")) {
            tokenizer.setDelimiterString(columnDelimiter);
        }

        return tokenizer;
    }

    public static List<String> tokenizeColumns(String line,
					       StringTokenizer tokenizer) {
        return tokenizeColumns(line, tokenizer, null);
    }

    public static boolean flag = true;

    public static List<String> tokenizeColumns(String line,
					       StringTokenizer tokenizer, List<String> toks) {
        tokenizer.reset(line);
        String tokens[] = tokenizer.getTokenArray();
        if (toks == null) {
            toks = new ArrayList(tokens.length);
        }
        for (String s : tokens) {
            if (s == null) {
                s = "";
            }
            toks.add(s);
        }

        return toks;
    }

    public static String columnsToString(List cols, String delimiter) {
        return columnsToString(cols, delimiter, false);
    }

    public static String columnsToString(List cols, String delimiter,
                                         boolean addNewLine) {
        StringBuilder sb = new StringBuilder();
	columnsToString(sb, cols, delimiter,addNewLine);
	return sb.toString();
    }

    public static void columnsToString(StringBuilder sb, List cols, String delimiter,
				       boolean addNewLine) {

        for (int i = 0; i < cols.size(); i++) {
            Object o = cols.get(i);
            String s = ((o == null)
                        ? ""
                        : o.toString());
            if (i > 0) {
                sb.append(delimiter);
            }

            boolean needToQuote = false;
            if (s.indexOf("\n") >= 0 || s.indexOf("\r") >= 0) {
                needToQuote = true;
            } else if (s.indexOf(delimiter) >= 0) {
                needToQuote = true;
            }

            if (s.indexOf("\"") >= 0) {
                s           = s.replaceAll("\"", "\"\"");
                needToQuote = true;
            }
            if (needToQuote) {
                sb.append('"');
                sb.append(s);
                sb.append('"');
            } else {
                sb.append(s);
            }
        }
        if (addNewLine) {
            sb.append("\n");
        }
    }

    public static List<Double> getDoubles(String s) {
        List<Double> cols = new ArrayList<Double>();
        for (String tok : Utils.split(s, ",", true, true)) {
            if ((tok.indexOf("-") >= 0) && !tok.startsWith("-")) {
                int from = (int)Double.parseDouble(Utils.split(tok, "-", true, true).get(0));

                double step  = 1;
                String right = Utils.split(tok, "-", true, true).get(1);
                if (right.indexOf(":") >= 0) {
                    List<String> tmp = Utils.split(right, ":", true, true);
                    right = tmp.get(0);
                    if (tmp.size() > 1) {
                        step = Double.parseDouble(tmp.get(1));
                    }
                }
                double to = Double.parseDouble(right);
                for (double i = from; i <= to; i += step) {
                    cols.add(i);
                }

                continue;
            }
            cols.add(Double.parseDouble(tok));
        }

        return cols;
    }

    public static List<Integer> getNumbers(String s) {
        List<Integer> cols = new ArrayList<Integer>();
        for (double d : getDoubles(s)) {
            cols.add((int) d);
        }

        return cols;
    }

    public static String trim(String s) {
        if (s == null) {
            return null;
        }

        return s.trim();
    }

    public static String clip(String s, String prefix) {
	if(s==null) return null;
	return s.substring(prefix.length());
    }

    public static boolean isTrue(boolean[]args,boolean...dflt) {
	if(args==null || args.length==0) {
	    return isTrue(dflt,false);
	}
	return args[0];
    }

    public static boolean stringDefined(Object o) {
	if(o==null) return false;
	String s = o.toString();
        if (s.trim().length() == 0) {
            return false;
        }

        return true;
    }

    public static String getDefined(String dflt, String... args) {
        for (String s : args) {
            if (Utils.stringDefined(s)) {
                return s;
            }
        }
        return dflt;
    }

    public static boolean isReal(double d) {
        if ( !Double.isNaN(d) && (d != Double.POSITIVE_INFINITY)
	     && (d != Double.NEGATIVE_INFINITY)) {
            return true;
        }

        return false;
    }

    public static boolean stringUndefined(String s) {
        return !stringDefined(s);
    }

    public static double modifiedJulianToJulian(double modifiedJulian) {
        // MJD = JD - 2400000.5 
        return modifiedJulian + 2400000.5;
    }

    /**
     *  The julian date functions below are from
     *  http://www.rgagnon.com/javadetails/java-0506.html
     */

    /**
     * Returns the Julian day number that begins at noon of
     * this day, Positive year signifies A.D., negative year B.C.
     * Remember that the year after 1 B.C. was 1 A.D.
     *
     * ref :
     *  Numerical Recipes in C, 2nd ed., Cambridge University Press 1992
     */
    // Gregorian Calendar adopted Oct. 15, 1582 (2299161)
    public static int JGREG = 15 + 31 * (10 + 12 * 1582);

    public static double HALFSECOND = 0.5;

    public static double toJulian(int[] ymd) {
        int year       = ymd[0];
        int month      = ymd[1];  // jan=1, feb=2,...
        int day        = ymd[2];
        int julianYear = year;
        if (year < 0) {
            julianYear++;
        }
        int julianMonth = month;
        if (month > 2) {
            julianMonth++;
        } else {
            julianYear--;
            julianMonth += 13;
        }

        double julian = (java.lang.Math.floor(365.25 * julianYear)
                         + java.lang.Math.floor(30.6001 * julianMonth) + day
                         + 1720995.0);
        if (day + 31 * (month + 12 * year) >= JGREG) {
            // change over to Gregorian calendar
            int ja = (int) (0.01 * julianYear);
            julian += 2 - ja + (0.25 * ja);
        }

        return java.lang.Math.floor(julian);
    }

    /**
     * Converts a Julian day to a calendar date
     * ref :
     * Numerical Recipes in C, 2nd ed., Cambridge University Press 1992
     *
     * @param injulian _more_
     *
     * @return _more_
     */
    public static int[] fromJulian(double injulian) {
        return fromJulian(injulian, new int[3]);
    }

    public static int[] fromJulian(double injulian, int[] src) {
        int    jalpha, ja, jb, jc, jd, je, year, month, day;
        double julian = injulian + HALFSECOND / 86400.0;
        ja = (int) julian;
        if (ja >= JGREG) {
            jalpha = (int) (((ja - 1867216) - 0.25) / 36524.25);
            ja     = ja + 1 + jalpha - jalpha / 4;
        }

        jb    = ja + 1524;
        jc    = (int) (6680.0 + ((jb - 2439870) - 122.1) / 365.25);
        jd    = 365 * jc + jc / 4;
        je    = (int) ((jb - jd) / 30.6001);
        day   = jb - jd - (int) (30.6001 * je);
        month = je - 1;
        if (month > 12) {
            month = month - 12;
        }
        year = jc - 4715;
        if (month > 2) {
            year--;
        }
        if (year <= 0) {
            year--;
        }

        src[0] = year;
        src[1] = month;
        src[2] = day;

        return src;
    }

    public static void testJulian(String args[]) {
        // FIRST TEST reference point
        System.out.println("Julian date for May 23, 1968 : "
                           + toJulian(new int[] { 1968,
						  5, 23 }));
        // output : 2440000
        int results[] = fromJulian(toJulian(new int[] { 1968, 5, 23 }));
        System.out.println("... back to calendar : " + results[0] + " "
                           + results[1] + " " + results[2]);

        // SECOND TEST today
        Calendar today = Calendar.getInstance();
        double todayJulian = toJulian(new int[] { today.get(Calendar.YEAR),
						  today.get(Calendar.MONTH) + 1, today.get(Calendar.DATE) });
        System.out.println("Julian date for today : " + todayJulian);
        results = fromJulian(todayJulian);
        System.out.println("... back to calendar : " + results[0] + " "
                           + results[1] + " " + results[2]);

        // THIRD TEST
        double date1 = toJulian(new int[] { 2005, 1, 1 });
        double date2 = toJulian(new int[] { 2005, 1, 31 });
        System.out.println("Between 2005-01-01 and 2005-01-31 : "
                           + (date2 - date1) + " days");

        /*
          expected output :
          Julian date for May 23, 1968 : 2440000.0
          ... back to calendar 1968 5 23
          Julian date for today : 2453487.0
          ... back to calendar 2005 4 26
          Between 2005-01-01 and 2005-01-31 : 30.0 days
        */
    }

    public static String getArticle(String s) {
        s = s.toLowerCase();
        if (s.startsWith("a") || s.startsWith("e") || s.startsWith("i")
	    || s.startsWith("o") || s.startsWith("u")) {
            return "an";
        } else {
            return "a";
        }
    }

    public static Date extractDate(String s) {
        try {
            String yyyy = "\\d\\d\\d\\d";
            String str = StringUtil.findPattern(s,
						"(" + yyyy + "-\\d\\d-\\d\\d)");
            if (str != null) {
                //                System.err.println("pattern 1:" + str);
                return parseDate(str);
            }

            str = StringUtil.findPattern(
					 s, "(" + yyyy + "\\d\\d\\d\\d-\\d\\d\\d\\d\\d\\d)");
            if (str != null) {
                try {
                    //                    System.err.println("pattern 2:" + str);
                    return new SimpleDateFormat("yyyyMMdd-HHmmss").parse(str);
                } catch (Exception ignore) {}
            }

            str = StringUtil.findPattern(s, "(" + yyyy
                                         + "\\d\\d\\d\\d-\\d\\d\\d\\d)");
            if (str != null) {
                try {
                    //                   System.err.println("pattern 3:" + str);
                    return new SimpleDateFormat("yyyyMMdd-HHmm").parse(str);
                } catch (Exception ignore) {}
            }

            str = StringUtil.findPattern(s, "[^\\d]*(" + yyyy
                                         + "\\d\\d\\d\\d)[^\\d]+");
            if (str != null) {
                try {
                    //                    System.err.println("pattern 4:" + str);
                    return new SimpleDateFormat("yyyyMMdd").parse(str);
                } catch (Exception ignore) {}
            }

            return null;
        } catch (Exception exc) {
            System.err.println("Utils.extractDate:" + exc);

            return null;
        }
    }

    public static Date extractDate(String input,String name) throws Exception {
	boolean debug = false;
	if(input==null) return null;
	List<String> lines = Utils.split(input,"\n",true,true);
	if(debug)	System.err.println("extractDate:" + name);
	for(String format:lines) {
	    String pattern = format;
	    if(format.indexOf(":")>=0) {
		List<String> toks = splitUpTo(format,":",2);
		pattern  =toks.get(0).trim();
		format  =toks.get(1).trim();		
		if(debug) System.err.println("pattern:" + pattern + " format:" + format);
	    } else {		
		//swap out any of the date tokens with a decimal regexp
		for (String s : new String[] {
			"y", "m", "M", "d", "H", "m"
		    }) {
		    pattern = pattern.replaceAll(s, "_DIGIT_");
		}
		pattern = pattern.replaceAll("_DIGIT_", "\\\\d");
		pattern = ".*(" + pattern + ").*";
		if(debug) System.err.println("pattern:" + pattern);
	    }
	    try {
		Matcher matcher =
		    Pattern.compile(pattern).matcher(name);
		if (matcher.find()) {
		    String dateString = matcher.group(1);
		    if(debug)
			System.err.println("\tfound:" + dateString);
		    try {
			Date date =  makeDateFormat(format).parse(dateString);
			//			System.err.println("\tDate:" + date);
			if(date!=null) return date;
		    } catch(Exception ignore1) {
			System.err.println("Error extracting date:" + ignore1);
		    }
		} else {
		    if(debug)
			System.err.println("\tNot found");
		}
	    } catch(Exception ignore2) {
		System.err.println("Error extracting date:" + ignore2);
	    }
	}
	return null;
    }

    public static Constructor findConstructor(Class c, Class[] paramTypes) {
        ArrayList<Object> allCtors     = new ArrayList<Object>();
        Constructor[]     constructors = c.getConstructors();
        if (constructors.length == 0) {
            System.err.println(
			       "*** Could not find any constructors for class:"
			       + c.getName());

            return null;
        }
        for (int i = 0; i < constructors.length; i++) {
            if (typesMatch(constructors[i].getParameterTypes(), paramTypes)) {
                allCtors.add(constructors[i]);
            }
        }
        if (allCtors.size() > 1) {
            throw new IllegalArgumentException(
					       "More than one constructors matched for class:"
					       + c.getName());
        }
        if (allCtors.size() == 1) {
            return (Constructor) allCtors.get(0);
        }

        System.err.println("Could not find constructor for:" + c.getName());
        for (int i = 0; i < constructors.length; i++) {
            Class[] formals = constructors[i].getParameterTypes();
            for (int j = 0; j < formals.length; j++) {
                System.err.println("\tparam " + j + "  "
                                   + formals[j].getName() + " "
                                   + paramTypes[j].getName());
            }
        }

        return null;
    }

    public static boolean typesMatch(Class[] formals, Class[] actuals) {
        if (formals.length != actuals.length) {
            return false;
        }
        for (int j = 0; j < formals.length; j++) {
            if (actuals[j] == null) {
                continue;
            }
            if ( !formals[j].isAssignableFrom(actuals[j])) {
                return false;
            }
        }

        return true;
    }

    public static Pattern compilePattern(String pattern) {
	if(!stringDefined(pattern)) return null;
	return Pattern.compile(pattern);
    }

    public static String[] findPatterns(String s, String regexp) {
        Pattern pattern = Pattern.compile(regexp);
        Matcher matcher = pattern.matcher(s);
        if ( !matcher.find()) {
            return null;
        }
        String[] results = new String[matcher.groupCount()];
        for (int i = 0; i < results.length; i++) {
            results[i] = matcher.group(i + 1);
        }

        return results;
    }

    public static List<String[]> findAllPatterns(String s, String regexp) {

        List<String[]> all     = new ArrayList<String[]>();
        Pattern        pattern = Pattern.compile(regexp);
        while (true) {
            Matcher matcher = pattern.matcher(s);
            if ( !matcher.find()) {
                break;
            }
            String[] results = new String[matcher.groupCount()];
            for (int i = 0; i < results.length; i++) {
                results[i] = matcher.group(i + 1);
            }
            all.add(results);
            s = s.substring(matcher.end());
        }

        return all;
    }

    public static Date findDate(String source, String[] datePatterns,
                                String[] dateFormats)
	throws Exception {
        for (int dateFormatIdx = 0; dateFormatIdx < datePatterns.length;
	     dateFormatIdx++) {
            String dttm = StringUtil.findPattern(source,
						 datePatterns[dateFormatIdx]);
            if (dttm != null) {
                dttm = dttm.replaceAll(" _ ", " ");
                dttm = dttm.replaceAll(" / ", "/");

                return makeDateFormat(dateFormats[dateFormatIdx]).parse(dttm);
            }
        }

        return null;
    }

    public static String redact(String s) {
	return "********";
    }

    public static SimpleDateFormat makeDateFormat(String format) {
        return makeDateFormat(format, "UTC");
    }

    public static String convertDateFormat(String format) {
	if(format==null) return null;
	format = format.trim();
	if(format.equals("iso8601") || format.equals("iso")) {
	    format= "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
	} else if(format.equals("isoshort")) {
	    format= "yyyy-MM-dd'T'HH:mm:ss";
	}
	return format;
    }

    public static SimpleDateFormat makeDateFormat(String format,
						  String ...timezone) {
	format = convertDateFormat(format);
        SimpleDateFormat sdf =  new SimpleDateFormat(format);
        if (timezone.length>0) {
            sdf.setTimeZone(TimeZone.getTimeZone(timezone[0]));
        }
        return sdf;
    }

    /**
     *  return the first non-null value in the args
     *
     * @param args _more_
     *  @return _more_
     */
    public static Object getNonNull(Object... args) {
        for (Object a : args) {
            if (a != null) {
                return a;
            }
        }

        return null;
    }

    public static int getYear(Date date) {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(date);
        return cal.get(cal.YEAR);
    }

    public static int getMonth(Date date) {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(date);

        return cal.get(cal.MONTH);
    }

    public static String removeNonAscii(String s) {
        return removeNonAscii(s, "_");
    }

    public static String removeNonAscii(String s, String replace) {
        s = s.replaceAll("[^\r\n\\x20-\\x7E]+", replace);

        return s;
    }

    public static String getAttributeOrTag(Element node, String attrOrTag, String dflt)
	throws Exception {
	return getAttributeOrTag(node, attrOrTag,dflt,false);
    }

    public static String getAttributeOrTag(Element node, String attrOrTag, String dflt, boolean checkParent)
	throws Exception {	
        String attrValue = XmlUtil.getAttribute(node, attrOrTag,
						(String) null);
        if (attrValue == null) {
            Node child = XmlUtil.findChild(node, attrOrTag);
            if (child != null) {
                attrValue = XmlUtil.getChildText(child);
                if (attrValue != null) {
                    if (XmlUtil.getAttribute(child, "encoded", false)) {
                        attrValue = new String(Utils.decodeBase64(attrValue));
                    }
                }
            }
        }
        if (attrValue == null && checkParent) {
	    Node parent = node.getParentNode();
	    if(parent!=null && parent instanceof Element)
		return getAttributeOrTag((Element)parent, attrOrTag,dflt, checkParent);
	}

        if (attrValue == null) {
            attrValue = dflt;
        }

        return attrValue;
    }

    public static String getAttributeOrTagUpTree(Node node, String attrOrTag,
						 String dflt)
	throws Exception {
        String attrValue = XmlUtil.getAttribute(node, attrOrTag,
						(String) null);
        if (attrValue == null) {
            Node child = XmlUtil.findChild(node, attrOrTag);
            if (child != null) {
                attrValue = XmlUtil.getChildText(child);
                if (attrValue != null) {
                    if (XmlUtil.getAttribute(child, "encoded", false)) {
                        attrValue = new String(Utils.decodeBase64(attrValue));
                    }
                }
            }
        }
	if(attrValue==null) {
	    Node parent = node.getParentNode();
	    if(parent!=null) return getAttributeOrTagUpTree(parent,  attrOrTag,
							    dflt);

	}

        if (attrValue == null) {
            attrValue = dflt;
        }

        return attrValue;
    }

    public static boolean getAttributeOrTag(Element node, String attrOrTag,
                                            boolean dflt)
	throws Exception {
        String attrValue = getAttributeOrTag(node, attrOrTag, (String) null);
        if (attrValue == null) {
            return dflt;
        }

        return attrValue.equals("true");
    }

    public static int getAttributeOrTag(Element node, String attrOrTag,
                                        int dflt)
	throws Exception {
        String attrValue = getAttributeOrTag(node, attrOrTag, (String) null);
        if (!stringDefined(attrValue)) {
            return dflt;
        }

        return Integer.parseInt(attrValue);
    }

    public static double getAttributeOrTag(Element node, String attrOrTag,
                                           double dflt)
	throws Exception {
        String attrValue = getAttributeOrTag(node, attrOrTag, (String) null);
        if (!stringDefined(attrValue)) {
            return dflt;
        }

        return Double.parseDouble(attrValue);
    }

    public static String makeProperties(Hashtable properties) {
        StringBuffer sb      = new StringBuffer();
        List<String> keyList = new ArrayList<String>();
        for (Enumeration keys = properties.keys(); keys.hasMoreElements(); ) {
            keyList.add((String) keys.nextElement());
        }
        keyList = (List<String>) Utils.sort(keyList);
        for (String key : keyList) {
            String value = (String) properties.get(key);
            sb.append(key);
            sb.append("=");
            sb.append(value);
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * Convert the properties into a name=value... string
     * @param properties _more_
     * @return _more_
     */
    public static String makeProperties(LinkedHashMap properties) {
        StringBuffer sb      = new StringBuffer();
        List<String> keyList = new ArrayList<String>();
        for (Object key : properties.keySet()) {
            keyList.add(key.toString());
        }
        keyList = (List<String>) Utils.sort(keyList);
        for (String key : keyList) {
            String value = properties.get(key).toString();
            sb.append(key);
            sb.append("=");
            sb.append(value);
            sb.append("\n");
        }

        return sb.toString();
    }

    public static List arrayToList(Object[] values) {
        List v = new ArrayList();
        for (Object o : values) {
            v.add(o);
        }

        return v;
    }

    public static List makeListFromDictionary(Dictionary properties) {
        List l = new ArrayList();
        for (Enumeration keys = properties.keys(); keys.hasMoreElements(); ) {
            Object key = keys.nextElement();
            l.add(key);
            l.add(properties.get(key));
        }

        return l;
    }

    public static List getValues(Hashtable properties) {
        List l = new ArrayList();
        for (Enumeration keys = properties.keys(); keys.hasMoreElements(); ) {
            Object key = keys.nextElement();
            l.add(properties.get(key));
        }

        return l;
    }

    public static List getKeys(Hashtable properties) {
        List l = new ArrayList();
        for (Enumeration keys = properties.keys(); keys.hasMoreElements(); ) {
            Object key = keys.nextElement();
            l.add(key);
        }

        return l;
    }

    public static List getKeys(LinkedHashMap properties) {
        List l = new ArrayList();
        for(Object o: properties.keySet()) {
	    l.add(o);
	}
	return l;
    }

    public static List<String> makeStringList(List l) {
        List<String> s = new ArrayList<String>();
        for (Object o : l) {
            s.add(o.toString());
        }

        return s;
    }

    public static Hashtable makeMap(Object... args) {
        Hashtable map = new Hashtable();
        for (int i = 0; i < args.length; i += 2) {
            if (args[i + 1] != null) {
                map.put(args[i], args[i + 1]);
            }

        }

        return map;
    }

    public static Hashtable makeMap(List args) {
        Hashtable map = new Hashtable();
        for (int i = 0; i < args.size(); i += 2) {
            if (args.get(i + 1) != null) {
                map.put(args.get(i), args.get(i + 1));
            }

        }

        return map;
    }    

    public static Hashtable<String,String> getProperties(String s) {
	return getProperties(s,false);
    }

    public static Hashtable<String,String> getProperties(String s, boolean trimValues) {
        Hashtable<String,String> p = new Hashtable<String,String>();
        for (String line : Utils.split(s, "\n")) {
            if (line.startsWith("#")) {
                continue;
            }
            List<String> toks = Utils.splitUpTo(line, "=", 2);
            if (toks.size() == 0) continue;
	    String key = toks.get(0).trim();
	    key = key.replaceAll("\\r", "");
            if (toks.size() == 2) {
		String value = toks.get(1);
		//Make sure to remove the newline char
		value = value.replaceAll("\\r", "");
		if(trimValues) value = value.trim();
		p.put(key, value);
            } else if (toks.size() == 1) {
                p.put(key, "");
            }
        }
        return p;
    }

    public static String hexEncode(String s) {
        byte[]       chars = s.getBytes();
        StringBuffer sb    = new StringBuffer();
        for (byte c : chars) {
            sb.append("\\x");
            sb.append(c);
        }

        return sb.toString();

    }

    public static String encodeMD(byte[] md) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < md.length; i++) {
            sb.append(Integer.toString((md[i] & 0xff) + 0x100,
                                       16).substring(1));
        }

        return sb.toString();
    }

    public static String makeMD5(String s) throws Exception {
	MessageDigest md5    = MessageDigest.getInstance("MD5");
	md5.update(s.getBytes());
	return Utils.encodeMD(md5.digest());
    }

    /**
     * Parse the integer tokens of the form:1,2-4,8-20:3 
     *
     * @param toks _more_
     *
     * @return _more_
     */
    public static List<Integer> toInt(List<String> toks) {
        List<Integer> ints = new ArrayList<Integer>();
        for (String tok : toks) {
	    int index = tok.indexOf("-");
	    if(index>=0) {
		List<String> range = Utils.splitUpTo(tok,"-",2);
		if(range.size()==2) {
		    int start = Integer.parseInt(range.get(0));
		    int step = 1;
		    int end;
		    int index2 = range.get(1).indexOf(":");
		    if(index2>=0) {
			List<String> toks3 = Utils.splitUpTo(range.get(1),":",2);
			end=Integer.parseInt(toks3.get(0));
			step=Integer.parseInt(toks3.get(1));			
		    } else {
			end = Integer.parseInt(range.get(1));		    
		    }
		    for(int i=start;i<=end;i+=step) {
			ints.add(Integer.valueOf(i));
		    }
		}
		continue;
	    }
            ints.add(Integer.parseInt(tok));
        }

        return ints;
    }

    public static String extractPatternNames(String filePatternString,
                                             List<String> patternNames) {
        List<String> names                 = new ArrayList<String>();
        String       tmp                   = filePatternString;
        StringBuffer pattern               = new StringBuffer();
        boolean      gotAttributeInPattern = false;
        while (true) {
            int openParenIdx = tmp.indexOf("(");
            if (openParenIdx < 0) {
                pattern.append(tmp);

                break;
            }
            int closeParenIdx = tmp.indexOf(")");
            if (closeParenIdx < openParenIdx) {
                pattern.append(tmp);

                break;
            }
            int colonIdx = tmp.indexOf(":");
            if (colonIdx < 0) {
                pattern.append(tmp);

                break;
            }
            if (colonIdx < openParenIdx) {
                pattern.append(tmp.substring(0, colonIdx + 1));
                tmp = tmp.substring(colonIdx + 1);
                continue;
            }
            //      System.err.println("open:" + openParenIdx+" close:" + closeParenIdx +" colon:" + colonIdx+ " tmp:" + tmp);
            if (closeParenIdx < colonIdx) {
                pattern.append(tmp.substring(0, closeParenIdx + 1));
                names.add("");
                tmp = tmp.substring(closeParenIdx + 1);
                continue;
            }
            pattern.append(tmp.substring(0, openParenIdx + 1));
            String name = tmp.substring(openParenIdx + 1, colonIdx);
            names.add(name);
            gotAttributeInPattern = true;
            pattern.append(tmp.substring(colonIdx + 1, closeParenIdx + 1));
            tmp = tmp.substring(closeParenIdx + 1);
        }
        if ( !gotAttributeInPattern) {
            pattern = new StringBuffer(filePatternString);
            names   = new ArrayList<String>();
        }
        patternNames.addAll(names);

        return pattern.toString();
    }

    public static Object safeGet(List list, int index) {
        if ((list == null) || (index >= list.size())) {
            return null;
        }

        return list.get(index);
    }

    public static boolean notEmpty(List list) {
	return list!=null && list.size()>0;
    }

    public static boolean isCompressed(String filename) throws IOException {
        filename = filename.toLowerCase();

        return filename.endsWith(".gz") || filename.endsWith(".zip");
    }

    public static String rot13(String s) {
        StringBuilder sb     = new StringBuilder();
        int           offset = 13;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c >= 'a') && (c <= 'm')) {
                c += offset;
            } else if ((c >= 'A') && (c <= 'M')) {
                c += offset;
            } else if ((c >= 'n') && (c <= 'z')) {
                c -= offset;
            } else if ((c >= 'N') && (c <= 'Z')) {
                c -= offset;
            }
            sb.append(c);
        }

        return sb.toString();
    }

    public static String obfuscate(String s, boolean base64) {
        s = rot13(s);
        if (base64) {
            return encodeBase64(s);
        }

        return s;
    }

    public static String unobfuscate(String s, boolean base64) {
        if (base64) {
            return rot13(new String(decodeBase64(s)));
        }

        return rot13(s);
    }

    private static final Base64.Encoder base64Encoder = Base64.getEncoder();

    private static final Base64.Decoder base64Decoder = Base64.getDecoder();

    private static final Base64.Decoder base64MimeDecoder =   Base64.getMimeDecoder();

    public static String encodeBase64Bytes(byte[] b) {
        return new String(base64Encoder.encode(b));
    }

    public static String encodeBase64(String s) {
	return encodeBase64(s,false);
    }	

    public static String encodeBase64(String s,boolean addPrefix) {	
	try {
	    s =  encodeBase64Bytes(s.getBytes("UTF-8"));
	    if(addPrefix) return "base64:" + s;
	    return s;
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    /**
     *
     * Decode the given base64 String
     *
     * @param s Holds the base64 encoded bytes
     * @return The decoded bytes
     */
    public static byte[] decodeBase64(String s) {
        try {
            return decodeBase64(s.getBytes("UTF-8"));
        } catch (Exception exc) {
            throw new RuntimeException("Failed to decode base64 string:" + s
                                       + "  Error:" + exc);
        }
    }

    public static byte[] decodeBase64(byte[] b) {
        try {
            return base64Decoder.decode(b);
        } catch (Exception exc) {
            //In case it was a mime encoded b64
            try {
                return base64MimeDecoder.decode(b);
            } catch (Exception exc2) {
                /*
                //Awful hack to not have to deal with why Don't synthid's are barfing
                try {
                return javax.xml.bind.DatatypeConverter.parseBase64Binary(
                s);
                } catch (Exception exc3) {
                */
                throw new RuntimeException("Failed to decode base64. error:"
					   //+ exc3);
					   + exc2);
                //}
            }
        }
    }

    public static List<String> extractPatterns(String text, String pattern) {
        List<String> values = new ArrayList<String>();
	if(text==null) return values;
        Pattern      p      = Pattern.compile(pattern);
        Matcher      m      = p.matcher(text);
        while (m.find()) {
            values.add(m.group(1));
        }

        return values;
    }

    public static String toString(Object o) {
        if (o == null) {
            return "";
        }

        return o.toString();
    }

    public static String getProperty(Dictionary props, String key) {
        return getProperty(props, key, (String) null);
    }

    public static String getProperty(Dictionary props, String key,
                                     String dflt) {
	if(props==null) return dflt;
        Object o = props.get(key);
        if (o == null) {
            o = (String) props.get(key.toLowerCase());
        }
        if (o == null) {
            return dflt;
        }

        return o.toString();
    }

    public static boolean getProperty(Dictionary props, String key,
                                      boolean dflt) {
        String s = Utils.getProperty(props, key, (String) null);
        if (s == null) {
            return dflt;
        }

        return Boolean.parseBoolean(s);
    }

    public static int getProperty(Dictionary props, String key, int dflt) {
        String s = Utils.getProperty(props, key, (String) null);
        if ( !stringDefined(s)) {
            return dflt;
        }

        return Integer.parseInt(s);
    }

    public static double getProperty(Dictionary props, String key, double dflt) {
        String s = Utils.getProperty(props, key, (String) null);
        if ( !stringDefined(s)) {
            return dflt;
        }

        return Double.parseDouble(s);
    }    

    public static double getDouble(Object o) {
	if(o==null) return Double.NaN;
	if(o instanceof Double) {
	    Double d = (Double) o;
	    return d.doubleValue();
	}
	String s = o.toString();
	if(s.equals("NA")) return Double.NaN;
	return Double.parseDouble(s);
    }

    public static String[] toStringArray(List l) {
        String[] a = new String[l.size()];
        for (int i = 0; i < l.size(); i++) {
            Object o = l.get(i);
            if (o == null) {
                a[i] = null;
            } else {
                a[i] = o.toString();
            }
        }

        return a;
    }

    public static boolean equalsOne(Object obj, Object... values) {
        if (obj == null) {
            return false;
        }
        for (Object other : values) {
            if ((other != null) && obj.equals(other)) {
                return true;
            }
        }

        return false;
    }

    /**
     * look for ... -arg value ...  in list
     *
     * @param arg _more_
     * @param args _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public static int getArg(String arg, List<String> args, int dflt) {
        for (int i = 0; i < args.size(); i++) {
            if (args.get(i).equals(arg)) {
                i++;
                if (i < args.size()) {
                    return Integer.parseInt(args.get(i));
                }

                throw new IllegalArgumentException("Error: argument " + arg
						   + " needs a value specified");
            }
        }

        return dflt;
    }

    /**
     * look for ... -arg value ...  in list
     *
     * @param arg _more_
     * @param args _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public static String getArg(String arg, List<String> args, String dflt) {
        for (int i = 0; i < args.size(); i++) {
            if (args.get(i).equals(arg)) {
                i++;
                if (i < args.size()) {
                    return args.get(i);
                }

                throw new IllegalArgumentException("Error: argument " + arg
						   + " needs a value specified");
            }
        }

        return dflt;
    }

    public static void print(String prefix, double[]values) {
	System.err.print(prefix);
	for(double d: values)System.err.print(" " + d);
	System.err.println("");
    }

    public static void printTimes(String what, long... args) {
        System.err.print(what + " ");
        for (int i = 1; i < args.length; i++) {
            System.err.print(" " + (args[i] - args[i - 1]));
        }
        if (args.length > 2) {
            System.err.println("  total: "
                               + (args[args.length - 1] - args[0]));
        } else {
            System.err.println("");
        }

    }

    public static void printMemory(String what, double... args) {
        System.out.print(what + " ");
        for (int i = 1; i < args.length; i++) {
            System.out.print(" " + decimals(args[i] - args[i - 1], 1));
        }
        System.out.println("");
    }

    public static String normalizeTemplateUrl(String f) {
	try {
	    return normalizeTemplateUrlInner(f);
	} catch(Exception exc) {
	    throw new RuntimeException(exc);
	}

    }

    public static String normalizeTemplateUrlInner(String f) throws Exception {	
        StringBuilder     s           = new StringBuilder();
        Date              now         = new Date();
        GregorianCalendar g           = new GregorianCalendar();
        Date              currentDate = now;
        TimeZone          tz          = TimeZone.getTimeZone("UTC");
        g.setTime(currentDate);
	SimpleDateFormat sdf = makeDateFormat("yyyy-MM-dd");
	SimpleDateFormat parseSdf = sdf;

	for(Macro macro:splitMacros(f)) {
	    if(macro.isText) {
		s.append(macro.macro);
		continue;
	    }
	    String t = macro.macro;
            if (t.startsWith("z:")) {
                tz = TimeZone.getTimeZone(t.substring(2));
                sdf.setTimeZone(tz);
                continue;
            }
            if (t.startsWith("format:")) {
                t = t.substring("format:".length()).trim();
                sdf = makeDateFormat(t);
                continue;
            }

	    if(t.equals("now.year")) {
		s.append(g.get(g.YEAR) + "");
		continue;
	    }
            if(t.equals("now.month")) {
		int month = g.get(g.MONTH);
		if(macro.getProperty("usebaseone",false)) month+=1;
		s.append(StringUtil.padZero(month, 2));
		continue;
	    }
	    if(t.equals("now.day")) {
		s.append(StringUtil.padZero(g.get(g.DAY_OF_MONTH), 2));
		continue;
	    }
	    if(t.equals("now.hour")) {
		s.append(StringUtil.padZero(g.get(g.HOUR_OF_DAY), 2));
		continue;
	    }
	    if(t.equals("now.minute")) {
		s.append(StringUtil.padZero(g.get(g.MINUTE), 2));
		continue;
	    }

	    if (t.startsWith(TIME_UNIT_YEAR)) {
		s.append(getCal(parseMacroDate(macro, parseSdf,  now, currentDate)).get(g.YEAR) + "");
		continue;
	    }
	    if (t.startsWith(TIME_UNIT_MONTH)) {
		s.append(StringUtil.padZero(getCal(parseMacroDate(macro, parseSdf,  now, currentDate)).get(g.MONTH), 2));
		continue;
            }
	    if (t.startsWith(TIME_UNIT_DAY)) {
		s.append(StringUtil.padZero(getCal(parseMacroDate(macro, parseSdf,  now, currentDate)).get(g.DAY_OF_MONTH), 2));
		continue;
            }
	    if (t.startsWith(TIME_UNIT_HOUR)) {
                s.append(StringUtil.padZero(getCal(parseMacroDate(macro, parseSdf,  now, currentDate)).get(g.HOUR_OF_DAY), 2));
		continue;
            }
	    if (t.startsWith(TIME_UNIT_MINUTE)) {
                s.append(StringUtil.padZero(getCal(parseMacroDate(macro, parseSdf,  now, currentDate)).get(g.MINUTE), 2));
		continue;
	    }

            if (t.equals("setdate")) {
                currentDate = parseMacroDate(macro, parseSdf,  now, currentDate);
		g.setTime(currentDate);
		continue;
	    }

            if (t.equals("date")) {
                Date newDate = parseMacroDate(macro, parseSdf,  now, currentDate);
		String fmt = (String)macro.getProperty("format");
                if (newDate != null) {
		    SimpleDateFormat thisSdf = sdf;	
		    if(fmt!=null) {
			thisSdf = makeDateFormat(fmt);
			if(tz!=null)
			    thisSdf.setTimeZone(tz);
		    }
                    s.append(thisSdf.format(newDate));
                }
                continue;
            }

	    //	    System.err.println("Apply macros: unknown macro:" + t+"\n" +Utils.getStack(10));
	    if(t.indexOf("date")>=0) throw new IllegalArgumentException("Apply macros: unknown macro:" + t);
	    //put it back
	    s.append("${" + t + "}");
        }

        return s.toString();
    }

    private static GregorianCalendar getCal(Date d) {
	GregorianCalendar cal = new GregorianCalendar();
	cal.setTime(d);
	return cal;
    }

    private static Date parseMacroDate(Macro macro,SimpleDateFormat parseSdf, Date now,Date currentDate) throws Exception {
	Date newDate = currentDate;
	String fmt = (String)macro.getProperty("format");
	String parseFormat = (String)macro.getProperty("parseFormat");		
	String date = (String)macro.getProperty("date");
	String offset = (String)macro.getProperty("offset");
	if(date!=null) {
	    if(date.equals("now")) {
		newDate = now;
	    } else {
		SimpleDateFormat psdf = parseSdf;
		if(parseFormat!=null) psdf  = makeDateFormat(parseFormat);
		newDate = psdf.parse(date);
	    }
	}
	if(offset!=null) {
	    newDate = DateUtil.getRelativeDate(newDate, offset);
	}
	return newDate;
    }

    private final static Pattern LTRIM = Pattern.compile("^\\s+");
    public static String ltrim(String s) {
	return LTRIM.matcher(s).replaceAll("");
    }

    /**
       split the string and trim each line
    */
    public static String trimLinesLeft(String s) {
	if(s==null) return null;
	StringBuilder sb  =new StringBuilder();
	for(String line: split(s,"\n",false,false)) {
	    line = ltrim(line);
	    sb.append(line);
	    sb.append("\n");
	}
	return sb.toString();
    }

    public static String upperCaseFirst(String s) {
        StringBuilder sb = new StringBuilder();
        for (String tok : Utils.split(s, " ", true, true)) {
            sb.append(tok.substring(0, 1).toUpperCase()
                      + tok.substring(1).toLowerCase());
            sb.append(" ");
        }

        return sb.toString().trim();
    }

    public static String nameCase(String s) {
        StringBuilder sb = new StringBuilder();
        for (String tok : Utils.split(s, " ", true, true)) {
            List<String> toks2 = Utils.split(tok, "-", true, true);
            for (int i = 0; i < toks2.size(); i++) {
                String tok2 = toks2.get(i);
                if (i > 0) {
                    sb.append("-");
                }
                if (tok2.indexOf(".") >= 0) {
                    sb.append(tok2.toUpperCase());
                } else if (tok2.startsWith("Mc")) {
                    sb.append(tok2);
                } else if (tok2.startsWith("Mac")) {
                    sb.append(tok2);
                } else {
                    sb.append(tok2.substring(0, 1).toUpperCase()
                              + tok2.substring(1).toLowerCase());
                }
            }
            sb.append(" ");
        }

        return sb.toString().trim();
    }

    public static String makeID(String label) {
        return makeID(label, false);
    }

    public static String makeID(String label, boolean forCode) {
        return makeID(label, forCode, "_");
    }

    public static String makeID(String label, boolean forCode,
                                String delimiter) {
        label = stripTags(label);
        label = label.trim().toLowerCase();
	label = Utils.replaceAll(label,delimiter,"thedelimiter");
	label = Utils.replaceAll(label,
				 "!","",
				 ":+", delimiter,
				 "&+", delimiter,
				 "\\s+", delimiter,
				 "\\*",delimiter,
				 "\\.", delimiter,
				 "(\r|\n)+", delimiter,
				 "\\(", delimiter,
				 "\\)", delimiter,
				 "\\?", delimiter,
				 "[\"'`]+", "").trim();
        label = Utils.replaceAll(label,
				 "-", delimiter,
				 ",", delimiter,
				 "/", delimiter,
				 "\\$",delimiter,
				 "%", delimiter,	
				 "__+", delimiter,
				 "[\\{\\}=]+", delimiter,
				 "_$", "");

	label = Utils.replaceAll(label,"thedelimiter",delimiter);
        if (forCode && Pattern.matches("^[0-9]+.*", label)) {
            label = delimiter + label;
        }

	//	System.err.println(label);
        return label;
    }

    private static final String[] ISDATE_PATTERNS = { "\\d\\d\\d\\d-\\d\\d-\\d\\d",
						      "(january|february|march\\s|april|may\\s|june|july|august|septembe|october|november|december).*" };

    public static boolean isDate(String s) {
        for (String p : ISDATE_PATTERNS) {
            if (Pattern.matches(p, s)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isNumber(String s) {
	if(s==null) return false;
        if (s.equals("nan") || s.equals("NaN")) {
            return true;
        }
        try {
            Double.parseDouble(s);
            return true;
        } catch (Exception ignore) {}
        return false;
    }

    public static boolean isInt(double d) {
        return d == (int) d;

    }

    public static void appendAll(Appendable sb, Object... args)
	throws Exception {
        for (Object arg : args) {
            if (arg != null) {
                sb.append(arg.toString());
            }
        }
    }

    public static String makeLabel(String label) {
	if(label==null) return label;
	if(label.length()<=2) return label.toUpperCase();
	String olabel = label;
	label = label.replaceAll("(\\d)(\\.)(\\d)","$1DOTESCAPE$3");
	label = label.replaceAll("\\."," ").replaceAll("_", " ").replaceAll("-"," ");
	label = label.replace("DOTESCAPE",".");
	label = label.replaceAll("\\s\\s+"," ");
        StringBuilder tmpSB             = new StringBuilder();
        StringBuilder sb                = new StringBuilder();
        boolean       lastCharUpperCase = false;
        for (String tok : Utils.split(label, " ", true, true)) {
	    if(tok.length()<4) {
		if(tok.length()==2) tok  = tok.toUpperCase();
		sb.append(tok);
		sb.append(" ");
		continue;
	    }
	    for (int i = 0; i < tok.length(); i++) {
		char    c           = tok.charAt(i);
		boolean isUpperCase = Character.isUpperCase(c);
		if (i > 0) {
		    if (isUpperCase) {
			if ( !lastCharUpperCase) {
			    sb.append(" ");
			}
		    }
		}
		lastCharUpperCase = isUpperCase;
		sb.append(c);
	    }
	    sb.append(" ");
        }

        label = sb.toString().trim();
        for (String tok : Utils.split(label, " ", true, true)) {
            tok = tok.substring(0, 1).toUpperCase()
		+ tok.substring(1, tok.length()).toLowerCase();
            tmpSB.append(tok);
            tmpSB.append(" ");
        }
        label = tmpSB.toString().trim();
        return label;

    }

    public static int getDimension(String s, int dflt) {
        try {
            if (s == null) {
                return dflt;
            }
            s = s.trim();
            boolean isPercent = s.endsWith("%");
            if (isPercent) {
                s = s.substring(0, s.length() - 1);
            }
            int v = Integer.parseInt(s);
            if (isPercent) {
                v = -v;
            }

            return v;
        } catch (Exception exc) {
            System.err.println("Utils.getDimension error:" + s + " " + exc);
	    exc.printStackTrace();
            return dflt;
        }
    }

    public static String compress(String s) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        GZIPOutputStream      gos = new GZIPOutputStream(bos);
        IOUtil.write(gos, s);
        gos.flush();
        IOUtil.close(gos);
        bos.flush();
        IOUtil.close(bos);
        byte[] bytes = bos.toByteArray();

        return encodeBase64Bytes(bytes);
    }

    public static String uncompress(String s) {
        try {
            byte[]               bytes = decodeBase64(s);
            ByteArrayInputStream bos   = new ByteArrayInputStream(bytes);
            GZIPInputStream      gos   = new GZIPInputStream(bos);

            return IOUtil.readInputStream(gos);
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    public static int hhmmssToSeconds(String s) {
        List<String> toks    = Utils.split(s, ":", true, true);
        int          seconds = 0;
        //HH
        if (toks.size() == 1) {
            seconds += Integer.parseInt(toks.get(0)) * 60 * 60;
            //HH:MM
        } else if (toks.size() == 2) {
            seconds += Integer.parseInt(toks.get(0)) * 60 * 60
		+ Integer.parseInt(toks.get(1)) * 60;
            //HH:MM:SS
        } else if (toks.size() >= 3) {
            seconds += Integer.parseInt(toks.get(0)) * 60 * 60
		+ Integer.parseInt(toks.get(1)) * 60
		+ Integer.parseInt(toks.get(2));
        }

        return seconds;
    }

    /**
       compare the strings. Handle for null. If they are equal return dflt
    */
    public static int compare(String s1, String s2, int dflt) {
	if(s1==null && s2==null) return dflt;
	if(s1==null && s2!=null) return 1;
	if(s1!=null && s2==null) return -1;	
	if(s1.equals(s2)) return dflt;
	return s1.compareTo(s2);

    }
    public static int compareIgnoreCase(String s1, String s2, int dflt) {
	if(s1==null && s2==null) return dflt;
	if(s1==null && s2!=null) return 1;
	if(s1!=null && s2==null) return -1;	
	if(s1.equals(s2)) return dflt;
	return s1.compareToIgnoreCase(s2);

    }    

    public static class DoubleTupleComparator implements Comparator {

        int index;

        public DoubleTupleComparator(int index) {
            this.index = index;
        }

        public int compare(Object o1, Object o2) {
            double[] e1 = (double[]) o1;
            double[] e2 = (double[]) o2;
            if (e1[index] == e2[index]) {
                return 0;
            }
            if (e1[index] < e2[index]) {
                return -1;
            }

            return 1;
        }

        public boolean equals(Object obj) {
            return obj == this;
        }
    }

    ;

    public static class FloatTupleComparator implements Comparator {

        int index;

        public FloatTupleComparator(int index) {
            this.index = index;
        }

        public int compare(Object o1, Object o2) {
            float[] e1 = (float[]) o1;
            float[] e2 = (float[]) o2;
            if (e1[index] == e2[index]) {
                return 0;
            }
            if (e1[index] < e2[index]) {
                return -1;
            }

            return 1;
        }

        public boolean equals(Object obj) {
            return obj == this;
        }
    }

    ;

    public static class IntegerTupleComparator implements Comparator {

        int index;

        public IntegerTupleComparator(int index) {
            this.index = index;
        }

        public int compare(Object o1, Object o2) {
            int v1 = ((Integer) ((Object[]) o1)[index]).intValue();
            int v2 = ((Integer) ((Object[]) o2)[index]).intValue();
            if (v1 < v2) {
                return -1;
            }
            if (v1 == v2) {
                return 0;
            }

            return 1;
        }

        public boolean equals(Object obj) {
            return obj == this;
        }
    }

    ;

    public static String getGuid() {
        return UUID.randomUUID().toString();
    }

    public static boolean passwordOK(String storedPassword,
                                     String givenPassword) {
        if (storedPassword.equals(givenPassword)) {
            return true;
        }
        storedPassword = storedPassword.trim();
        givenPassword  = givenPassword.trim();
        if (storedPassword.length() == 0) {
            return false;
        }
        if (storedPassword.equals(givenPassword)) {
            return true;
        }
        if (storedPassword.length() == 0) {
            return false;
        }
        storedPassword = storedPassword.replaceAll("\"", "").trim();
        givenPassword  = givenPassword.replaceAll("\"", "").trim();

        return storedPassword.equals(givenPassword);
    }

    private static StringBuilder append(Object c, StringBuilder sb,
                                        List<StringBuilder> lines) {
        if (sb == null) {
            sb = new StringBuilder();
            lines.add(sb);
        }
        sb.append(c);

        return sb;
    }

    public static Object multiEquals(Object value,Object dflt,Object...pairs) {
	if(value==null) return dflt;
	for(int i=0;i<pairs.length;i+=2) {
	    if(pairs[i].equals(value)) return pairs[i+1];
	}
	return dflt;
    }

    public static final String MULTILINE_END = "_multilineend_";

    public static List<StringBuilder> parseMultiLineCommandLine(
								String commandString) {

        List<StringBuilder> lines      = new ArrayList<StringBuilder>();
        StringBuilder       sb         = null;
        boolean             inEscape   = false;
        boolean             inBracket  = false;
        boolean             inQuote    = false;
        int                 bracketCnt = 0;
        int                 quoteCnt   = 0;
        for (int i = 0; i < commandString.length(); i++) {
            char c = commandString.charAt(i);
            if (c == '\r') {
                continue;
            }
            if (inEscape) {
                sb       = append(c, sb, lines);
                inEscape = false;

                continue;
            }
            if (c == '\\') {
                inEscape = true;

                continue;
            }
            if (c == '{') {
                if (inQuote) {
                    sb = append(c, sb, lines);
                } else {
                    if ( !inBracket) {
                        sb = null;
                    } else {
                        sb = append(c, sb, lines);
                    }
                    inBracket = true;
                    bracketCnt++;
                }

                continue;
            }
            if (c == '}') {
                if (inQuote) {
                    sb = append(c, sb, lines);
                } else {
                    bracketCnt--;
                    if (bracketCnt < 0) {
                        throw new IllegalArgumentException(
							   "Unopened bracket:" + commandString);
                    }
                    if (bracketCnt > 0) {
                        sb = append(c, sb, lines);
                    } else {
                        if (sb == null) {
                            append(' ', sb, lines);
                        }
                        inBracket = false;
                        sb        = null;
                    }
                }

                continue;
            }
            if (c == '\"') {
                if (inBracket) {
                    sb = append(c, sb, lines);
                } else {
                    if ( !inQuote) {
                        inQuote = true;
                    } else {
                        if (sb == null) {
                            append("", sb, lines);
                        }
                        sb      = null;
                        inQuote = false;
                    }
                }

                continue;
            }

            if ((c == '\n') || (c == ' ')) {
                if ( !inQuote && !inBracket) {
                    sb = null;
                    if (c == '\n') {
                        //Add in the new line if we're not in a block
                        append(MULTILINE_END, sb, lines);
                    }
                } else {
                    sb = append(c, sb, lines);
                }

                continue;
            }
            sb = append(c, sb, lines);
        }

        return lines;
    }

    public static Hashtable<String, String> parseKeyValue(String args) {
        Hashtable props = new Hashtable();
        for (String tok : Utils.parseCommandLine(args)) {
            List<String> toks = splitUpTo(tok, "=", 2);
            if (toks.size() == 2) {
                props.put(toks.get(0), toks.get(1));
            } else if (toks.size() == 1) {
                props.put(toks.get(0), "");
            }
        }

        return props;
    }

    public static List<String> parseCommandLine(String s) {
        return parseCommandLine(s, true);
    }

    public static List<String> parseCommandLine(String s,
						boolean throwError) {

        //        System.err.println("command line:" + s);
        List<String> args = new ArrayList<String>();
        s = s.trim();
        StringBuilder sb         = new StringBuilder();
        boolean       inQuote    = false;
        boolean       inBracket  = false;
        boolean       prevEscape = false;
        //        System.out.println("line:" + s);
        //        System.err.println("s:" + s);
        for (int i = 0; i < s.length(); i++) {
            char    c            = s.charAt(i);
            boolean isQuote      = (c == '\"') || (c == '{') || (c == '}');
            boolean openBracket  = (c == '{');
            boolean closeBracket = (c == '}');
            boolean isEscape     = (c == '\\');
            //        s = " -db  {rhl_0000005.id \"test it\" }";
            //            System.out.println("char:" + c + " prev escape:" + prevEscape +" isquote:" + isQuote +" inquote:"+ inQuote);
            if (prevEscape) {
                sb.append(c);
                prevEscape = false;

                continue;
            }

            if (c == '\\') {
                if (prevEscape) {
                    sb.append(c);
                    prevEscape = false;
                } else {
                    prevEscape = true;
                }

                continue;
            }
            if (prevEscape) {
                sb.append(c);
                prevEscape = false;

                continue;
            }
            //.... " {.....} "
            if (openBracket) {
                if (inQuote) {
                    sb.append(c);
                } else {
                    inBracket = true;
                }

                continue;
            }
            if (closeBracket) {
                if (inQuote) {
                    sb.append(c);
                } else {
                    args.add(sb.toString());
                    sb.setLength(0);
                    inBracket = false;
                }

                continue;
            }
            if (inBracket) {
                sb.append(c);

                continue;
            }
            if (isQuote) {
                if (inQuote) {
                    inQuote = false;
                    args.add(sb.toString());
                    sb.setLength(0);
                } else {
                    inQuote = true;
                }

                continue;
            }
            if (inQuote) {
                sb.append(c);

                continue;
            }
            if (c == ' ') {
                if (sb.length() > 0) {
                    args.add(sb.toString());
                    sb = new StringBuilder();
                }

                continue;
            }
            sb.append(c);
        }
        if (inQuote) {
            if ( !throwError) {
                return null;
            }

            throw new IllegalArgumentException("Unclosed quote:" + s);
        }
        if (inBracket) {
            if ( !throwError) {
                return null;
            }

            throw new IllegalArgumentException("Unclosed bracket:" + s);
        }
        if (sb.length() > 0) {
            args.add(sb.toString());
        }
        //        System.err.println("args:" + args);

        return args;

    }

    /** the file separator id */
    public static final String FILE_SEPARATOR = "_file_";

    public static final String ENTRY_ID_REGEX =
        "[a-f|0-9]{8}-([a-f|0-9]{4}-){3}[a-f|0-9]{12}_";

    public static String getFileTail(String fileName) {
        int idx = fileName.indexOf(FILE_SEPARATOR);
        if (idx >= 0) {
            fileName = fileName.substring(idx + FILE_SEPARATOR.length());
        } else {
            /*
              We have this here for files from old versions of RAMADDA where we did
              not add the StorageManager.FILE_SEPARATOR delimiter and it looked something like:
              "62712e31-6123-4474-a96a-5e4edb608fd5_<filename>"
            */
            fileName = fileName.replaceFirst(ENTRY_ID_REGEX, "");
        }

        //Check for Rich's problem
        idx = fileName.lastIndexOf("\\");
        if (idx >= 0) {
            fileName = fileName.substring(idx + 1);
        }
        String tail = IOUtil.getFileTail(fileName);

        return tail;

    }

    public static double[] calculateCentroid(List<double[]> pts) {
        double centroidX  = 0;
        double centroidY  = 0;
        double signedArea = 0.0;
        double x0         = 0.0;
        double y0         = 0.0;
        double x1         = 0.0;
        double y1         = 0.0;
        double a          = 0.0;

        for (int i = 0; i < pts.size(); i++) {
            double[] pt0 = pts.get(i);
            double[] pt1 = pts.get((i == pts.size() - 1)
                                   ? 0
                                   : i + 1);
            x0         = pt0[0];
            y0         = pt0[1];
            x1         = pt1[0];
            y1         = pt1[1];

            a          = x0 * y1 - x1 * y0;
            signedArea = signedArea + a;
            centroidX  = centroidX + (x0 + x1) * a;
            centroidY  = centroidY + (y0 + y1) * a;
        }
        signedArea = signedArea * 0.5;
        centroidX  = centroidX / (6.0 * signedArea);
        centroidY  = centroidY / (6.0 * signedArea);

        return new double[] { centroidX, centroidY };
    }

    public static String prune(String s, String pattern) {
        int index = s.indexOf(pattern);
        if (index > 0) {
            s = s.substring(0, index);
        }

        return s;
    }

    //j--

    private static DateFormat[] DATE_FORMATS = {
	new DateFormat("MM/dd/yyyy hh:mm:ss a"),
        new DateFormat("yyyy-MM-dd'T'HH:mm:ss Z"),
        new DateFormat("yyyyMMdd'T'HHmmss Z"),
        new DateFormat("yyyy/MM/dd HH:mm:ss Z"),
        new DateFormat("yyyy-MM-dd HH:mm:ss Z"),
        new DateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"),
        new DateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"),
        new DateFormat("yyyy-MM-dd HH:mm:ss'Z'"),
        new DateFormat("yyyy-MM-dd HH:mm:ss.SSS'Z'"),
        new DateFormat("EEE MMM dd HH:mm:ss Z yyyy"),
        new DateFormat("yyyy-MM-dd'T'HH:mm:ss"),
        new DateFormat("yyyyMMdd'T'HHmmss"),
        new DateFormat("yyyy-MM-dd'T'HH:mm Z"),
        new DateFormat("yyyyMMdd'T'HHmm Z"),
        new DateFormat("yyyy-MM-dd'T'HH:mm"),
        new DateFormat("yyyyMMdd'T'HHmm"),
        new DateFormat("yyyy/MM/dd HH:mm:ss"),
        new DateFormat("yyyy-MM-dd HH:mm:ss"),
        new DateFormat("yyyy/MM/dd HH:mm Z"),
        new DateFormat("yyyy-MM-dd HH:mm Z"),
        new DateFormat("yyyy/MM/dd HH:mm", true),
        new DateFormat("yyyy-MM-dd HH:mm", true),
        new DateFormat("yyyy-MM-dd", true),
        new DateFormat("yyyy/MM/dd", true),
        new DateFormat("MM/dd/yyyy", true),
	new DateFormat("yyyy-MM", true),
        new DateFormat("yyyy/MM", true),
	new DateFormat("yyyyMMdd", true),
        new DateFormat("yyyyMM", true),
	new DateFormat("yyyy", true)
    };
    //j++

    /** a set of regular expressions that go along with the below DATE_FORMATS */
    public static final String[] FIND_DATE_PATTERNS = {
	"\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2} (AM|PM)", "MM/dd/yyyy hh:mm:ss a",
	"\\d{4}-\\d{2}'T'\\d{2}:\\d{2}:\\d{2}",	"yyyy-MM-dd'T'HH:mm:ss",
	"^\\d{4}-\\d{2}-\\d{2} +\\d{2}:\\d{2}:\\d{2}$", "yyyy-MM-dd HH:mm:ss",
	"^\\d{4}-\\d{2}-\\d{2} +\\d{2}:\\d{2}$", "yyyy-MM-dd HH:mm",
	"^\\d{4}-\\d{2}-\\d{2}$", "yyyy-MM-dd",
	"^\\d{2}/\\d{2}/\\d{4}$", "MM/dd/yyyy",		
	"^\\d{4}/\\d{2}/\\d{2}$","yyyy/MM/dd",
	"^\\d{8}_\\d{4}$","yyyyMMdd_HHmm",
	"^\\d{8}_\\d{2}$","yyyyMMdd_HH",
	"^\\d{6}$", "yyyyMMdd",
	"^\\d{4}-\\d{2}$","yyyy-MM",
	"^\\d{4}$","yyyy"	
    };

    public static final SimpleDateFormat findDateFormat(String s) {
	for(int i=0;i<FIND_DATE_PATTERNS.length;i+=2) {
	    String pattern = FIND_DATE_PATTERNS[i];
	    if(s.matches(pattern)) {
		return makeDateFormat(FIND_DATE_PATTERNS[i+1]);
	    }
	}
	return  null;
    }

    /** The format string that was used for the most recent sdf */
    private static DateFormat lastFormat;

    /** A hack. We keep track of the length of the date string and will only use the lastSdf when the lengths match */
    private static int lengthLastDate = 0;

    public static boolean debugDate = false;

    public static Date parseDate(String dttm) {
        if (dttm == null) {
            return null;
        }
        ParsePosition pp = new ParsePosition(0);
        for (DateFormat dateFormat : getFormatters()) {
            Date date = dateFormat.parse(dttm, pp);
            if (date != null) {
                if (debugDate) {
                    System.err.println("   success:" + " date:" + dttm
                                       + " format:" + dateFormat.format
                                       + " date:" + date);
                }

                return date;
            }
        }

        return null;
    }

    /**
     * this combines a couple of methods from ucar.unidata.util.DateUtil
     *
     * @param baseDate _more_
     * @param s _more_
     * @param roundDays _more_
     *  @return _more_
     */
    public static Date parseRelativeDate(Date baseDate, String s,
                                         int roundDays) {
        Calendar cal = Calendar.getInstance(TIMEZONE_GMT);
        cal.setTimeInMillis(baseDate.getTime());
        Date dttm = null;
        if (s.equals("now")) {
            return cal.getTime();
        } else if (s.equals("today")) {
            dttm = cal.getTime();
        } else if (s.equals("yesterday")) {
            dttm = new Date(cal.getTime().getTime()
                            - DateUtil.daysToMillis(1));
        } else if (s.equals("tomorrow")) {
            dttm = new Date(cal.getTime().getTime()
                            + DateUtil.daysToMillis(1));
        } else if (s.startsWith("last ") || s.startsWith("next ")) {
            List toks = StringUtil.split(s, " ", true, true);
            if (toks.size() != 2) {
                throw new IllegalArgumentException("Bad time format:" + s);
            }
            int    factor = (toks.get(0).equals("last")
                             ? -1
                             : +1);
            String unit   = (String) toks.get(1);
            if (unit.equals(TIME_UNIT_WEEK)) {
                cal.add(Calendar.WEEK_OF_MONTH, factor);
            } else if (unit.equals(TIME_UNIT_MONTH)) {
                cal.add(Calendar.MONTH, factor);
            } else if (unit.equals(TIME_UNIT_YEAR)) {
                cal.add(Calendar.YEAR, factor);
            } else if (unit.equals("century")) {
                cal.add(Calendar.YEAR, factor * 100);
            } else if (unit.equals("millenium")) {
                cal.add(Calendar.YEAR, factor * 1000);
            } else {
                throw new IllegalArgumentException("Bad time format:" + s
						   + " unknown time field:" + unit);
            }
            dttm = cal.getTime();
        }

        if (dttm == null) {
            Pattern pattern =
                Pattern.compile(
				"([\\+\\-0-9]+) +(second|minute|hour|day|week|month|year|decade|century|millenium)s?");
            Matcher matcher = pattern.matcher(s);
            if (matcher.find()) {
                String quantity = matcher.group(1).trim();
                String what     = matcher.group(2).trim();
                long   factor   = 1;
                if (quantity.startsWith("+")) {
                    quantity = quantity.substring(1);
                } else if (quantity.startsWith("-")) {
                    quantity = quantity.substring(1);
                    factor   = -1;
                }
                long delta        = factor * Integer.parseInt(quantity);
                long milliseconds = 0;
                if (what.startsWith(TIME_UNIT_SECOND)) {
                    milliseconds = delta * 1000;
                } else if (what.startsWith(TIME_UNIT_MINUTE)) {
                    milliseconds = 60 * delta * 1000;
                } else if (what.startsWith(TIME_UNIT_HOUR)) {
                    milliseconds = 60 * 60 * delta * 1000;
                } else if (what.startsWith(TIME_UNIT_DAY)) {
                    milliseconds = 24 * 60 * 60 * delta * 1000;
                } else if (what.startsWith(TIME_UNIT_WEEK)) {
                    milliseconds = 7 * 24 * 60 * 60 * delta * 1000;
                } else if (what.startsWith(TIME_UNIT_MONTH)) {
                    milliseconds = 30 * 24 * 60 * 60 * delta * 1000;
                } else if (what.startsWith(TIME_UNIT_YEAR)) {
                    milliseconds = 365 * 24 * 60 * 60 * delta * 1000;
                } else if (what.startsWith("century")) {
                    milliseconds = 100 * 365 * 24 * 60 * 60 * delta * 1000;
                } else if (what.startsWith("millenium")) {
                    milliseconds = 1000 * 365 * 24 * 60 * 60 * delta * 1000;
                }
                dttm = new Date(baseDate.getTime() + milliseconds);
            }
        }
        if (dttm == null) {
            dttm = parseDate(s);
        }

        if (dttm == null) {
            throw new RuntimeException("Unable to parse date:" + s);
        }

        return dttm;
        //      return DateUtil.roundByDay(dttm, roundDays);
    }

    public static String stripTags(String html) {
        StringBuffer stripped = new StringBuffer();
        while (html.length() > 0) {
            int idx = html.indexOf("<");
            if (idx < 0) {
                stripped.append(html.trim());
                break;
            }
            String text = html.substring(0, idx);
            text = text.trim();
            if (text.length() > 0) {
                stripped.append(text);
                stripped.append(" ");
            }
            html = html.substring(idx);
            int idx2 = html.indexOf(">");
            if (idx2 < 0) {
                break;
            }
            html = html.substring(idx2 + 1);
        }

        return StringUtil.replace(stripped.toString(), "&nbsp;", "");
        /*    return stripped.toString();
              stripped = new StringBuffer(replace(stripped.toString(), "&nbsp;",
              ""));
              return stripped.toString();
        */
    }

    public static <T> List<T> unroll(Set<T> set)  {
	List<T> l = new ArrayList<T>();
	Iterator<T> iter = set.iterator();
	while (iter.hasNext()) {
	    l.add(iter.next());
	}
	return l;

    }

    private static HashSet<String> stopWords;

    public static HashSet<String> getStopWords() throws Exception {
        if (stopWords == null) {
            HashSet<String> tmp = new HashSet<String>();
            for (String line :
		     split(IO.readContents("/org/ramadda/util/stopwords.txt",
					   Utils.class), "\n", true, true)) {
                tmp.add(line);
            }
            stopWords = tmp;
        }

        return stopWords;
    }

    public static HashSet makeHashSet(Object... args) {
        HashSet h = new HashSet();
        for (Object arg : args) {
            h.add(arg);
        }

        return h;
    }

    public static HashSet makeHashSet(List args) {
        HashSet h = new HashSet();
        for (Object arg : args) {
            h.add(arg);
        }

        return h;
    }

    public static boolean testAndSet(HashSet set, Object o) {
        if (set.contains(o)) {
            return true;
        }
        set.add(o);

        return false;
    }

    public static Hashtable makeHashtable(Object... args) {
        Hashtable h = new Hashtable();
        for (int i = 0; i < args.length; i += 2) {
            h.put(args[i], args[i + 1]);
        }

        return h;
    }

    public static Hashtable makeHashtable(List list) {
        Hashtable h = new Hashtable();
        for (int i = 0; i < list.size(); i += 2) {
            h.put(list.get(i), list.get(i + 1));
        }

        return h;
    }

    public static boolean isUrl(String s) {
        s = s.toLowerCase();
        return s.startsWith("https:") || s.startsWith("http:");
    }

    public static List makeListFromValues(Object... args) {
        List h = new ArrayList();
        for (Object arg : args) {
            h.add(arg);
        }

        return h;
    }

    public static List makeListFromArray(Object[] a) {
        List h = new ArrayList();
        for (Object arg : a) {
            h.add(arg);
        }

        return h;
    }

    public static final Hashtable<String, Color> COLORNAMES =
        (Hashtable<String,
	 Color>) makeHashtable("lightsalmon",
			       new Color(255, 160, 122), "salmon",
			       new Color(250, 128, 114),
			       "darksalmon",
			       new Color(233, 150, 122),
			       "lightcoral",
			       new Color(240, 128, 128),
			       "indianred", new Color(205, 92, 92),
			       "crimson", new Color(220, 20, 60),
			       "firebrick", new Color(178, 34, 34),
			       "red", new Color(255, 0, 0),
			       "darkred", new Color(139, 0, 0),
			       "coral", new Color(255, 127, 80),
			       "tomato", new Color(255, 99, 71),
			       "orangered", new Color(255, 69, 0),
			       "gold", new Color(255, 215, 0),
			       "orange", new Color(255, 165, 0),
			       "darkorange",
			       new Color(255, 140, 0),
			       "lightyellow",
			       new Color(255, 255, 224),
			       "lemonchiffon",
			       new Color(255, 250, 205),
			       "lightgoldenrodyellow",
			       new Color(250, 250, 210),
			       "papayawhip",
			       new Color(255, 239, 213),
			       "moccasin",
			       new Color(255, 228, 181),
			       "peachpuff",
			       new Color(255, 218, 185),
			       "palegoldenrod",
			       new Color(238, 232, 170), "khaki",
			       new Color(240, 230, 140),
			       "darkkhaki",
			       new Color(189, 183, 107), "yellow",
			       new Color(255, 255, 0), "lawngreen",
			       new Color(124, 252, 0),
			       "chartreuse",
			       new Color(127, 255, 0), "limegreen",
			       new Color(50, 205, 50), "lime",
			       new Color(0, 255, 0), "forestgreen",
			       new Color(34, 139, 34), "green",
			       new Color(0, 128, 0), "darkgreen",
			       new Color(0, 100, 0), "greenyellow",
			       new Color(173, 255, 47),
			       "yellowgreen",
			       new Color(154, 205, 50),
			       "springgreen",
			       new Color(0, 255, 127),
			       "mediumspringgreen",
			       new Color(0, 250, 154),
			       "lightgreen",
			       new Color(144, 238, 144),
			       "palegreen",
			       new Color(152, 251, 152),
			       "darkseagreen",
			       new Color(143, 188, 143),
			       "mediumseagreen",
			       new Color(60, 179, 113), "seagreen",
			       new Color(46, 139, 87), "olive",
			       new Color(128, 128, 0),
			       "darkolivegreen",
			       new Color(85, 107, 47), "olivedrab",
			       new Color(107, 142, 35),
			       "lightcyan",
			       new Color(224, 255, 255), "cyan",
			       new Color(0, 255, 255), "aqua",
			       new Color(0, 255, 255),
			       "aquamarine",
			       new Color(127, 255, 212),
			       "mediumaquamarine",
			       new Color(102, 205, 170),
			       "paleturquoise",
			       new Color(175, 238, 238),
			       "turquoise",
			       new Color(64, 224, 208),
			       "mediumturquoise",
			       new Color(72, 209, 204),
			       "darkturquoise",
			       new Color(0, 206, 209),
			       "lightseagreen",
			       new Color(32, 178, 170),
			       "cadetblue",
			       new Color(95, 158, 160), "darkcyan",
			       new Color(0, 139, 139), "teal",
			       new Color(0, 128, 128),
			       "powderblue",
			       new Color(176, 224, 230),
			       "lightblue",
			       new Color(173, 216, 230),
			       "lightskyblue",
			       new Color(135, 206, 250), "skyblue",
			       new Color(135, 206, 235),
			       "deepskyblue",
			       new Color(0, 191, 255),
			       "lightsteelblue",
			       new Color(176, 196, 222),
			       "dodgerblue",
			       new Color(30, 144, 255),
			       "cornflowerblue",
			       new Color(100, 149, 237),
			       "steelblue",
			       new Color(70, 130, 180),
			       "royalblue",
			       new Color(65, 105, 225), "blue",
			       new Color(0, 0, 255), "mediumblue",
			       new Color(0, 0, 205), "darkblue",
			       new Color(0, 0, 139), "navy",
			       new Color(0, 0, 128),
			       "midnightblue",
			       new Color(25, 25, 112),
			       "mediumslateblue",
			       new Color(123, 104, 238),
			       "slateblue",
			       new Color(106, 90, 205),
			       "darkslateblue",
			       new Color(72, 61, 139), "lavender",
			       new Color(230, 230, 250), "thistle",
			       new Color(216, 191, 216), "plum",
			       new Color(221, 160, 221), "violet",
			       new Color(238, 130, 238), "orchid",
			       new Color(218, 112, 214), "fuchsia",
			       new Color(255, 0, 255), "magenta",
			       new Color(255, 0, 255),
			       "mediumorchid",
			       new Color(186, 85, 211),
			       "mediumpurple",
			       new Color(147, 112, 219),
			       "blueviolet",
			       new Color(138, 43, 226),
			       "darkviolet",
			       new Color(148, 0, 211),
			       "darkorchid",
			       new Color(153, 50, 204),
			       "darkmagenta",
			       new Color(139, 0, 139), "purple",
			       new Color(128, 0, 128), "indigo",
			       new Color(75, 0, 130), "pink",
			       new Color(255, 192, 203),
			       "lightpink",
			       new Color(255, 182, 193), "hotpink",
			       new Color(255, 105, 180),
			       "deeppink", new Color(255, 20, 147),
			       "palevioletred",
			       new Color(219, 112, 147),
			       "mediumvioletred",
			       new Color(199, 21, 133), "white",
			       new Color(255, 255, 255), "snow",
			       new Color(255, 250, 250),
			       "honeydew",
			       new Color(240, 255, 240),
			       "mintcream",
			       new Color(245, 255, 250), "azure",
			       new Color(240, 255, 255),
			       "aliceblue",
			       new Color(240, 248, 255),
			       "ghostwhite",
			       new Color(248, 248, 255),
			       "whitesmoke",
			       new Color(245, 245, 245),
			       "seashell",
			       new Color(255, 245, 238), "beige",
			       new Color(245, 245, 220), "oldlace",
			       new Color(253, 245, 230),
			       "floralwhite",
			       new Color(255, 250, 240), "ivory",
			       new Color(255, 255, 240),
			       "antiquewhite",
			       new Color(250, 235, 215), "linen",
			       new Color(250, 240, 230),
			       "lavenderblush",
			       new Color(255, 240, 245),
			       "mistyrose",
			       new Color(255, 228, 225),
			       "gainsboro",
			       new Color(220, 220, 220),
			       "lightgray",
			       new Color(211, 211, 211), "silver",
			       new Color(192, 192, 192),
			       "darkgray",
			       new Color(169, 169, 169), "gray",
			       new Color(128, 128, 128), "dimgray",
			       new Color(105, 105, 105),
			       "lightslategray",
			       new Color(119, 136, 153),
			       "slategray",
			       new Color(112, 128, 144),
			       "darkslategray",
			       new Color(47, 79, 79), "black",
			       new Color(0, 0, 0), "cornsilk",
			       new Color(255, 248, 220),
			       "blanchedalmond",
			       new Color(255, 235, 205), "bisque",
			       new Color(255, 228, 196),
			       "navajowhite",
			       new Color(255, 222, 173), "wheat",
			       new Color(245, 222, 179),
			       "burlywood",
			       new Color(222, 184, 135), "tan",
			       new Color(210, 180, 140),
			       "rosybrown",
			       new Color(188, 143, 143),
			       "sandybrown",
			       new Color(244, 164, 96),
			       "goldenrod",
			       new Color(218, 165, 32), "peru",
			       new Color(205, 133, 63),
			       "chocolate",
			       new Color(210, 105, 30),
			       "saddlebrown",
			       new Color(139, 69, 19), "sienna",
			       new Color(160, 82, 45), "brown",
			       new Color(165, 42, 42), "maroon",
			       new Color(128, 0, 0));

    /** hex color string without leading # */
    public static final String HEX_COLOR_PATTERN =
        "^?(([a-fA-F0-9]){3}){1,2}$";

    public static Color decodeColor(String value, Color dflt) {
        if (value == null) {
            return dflt;
        }
        value = value.trim();
        if (value.length() == 0) {
            return dflt;
        }
        if (value.equals("null")) {
            return null;
        }
        try {
            String s = value;
            if (Pattern.matches(HEX_COLOR_PATTERN, s) && !s.startsWith("#")) {
                // add # so Integer will decode it properly
                s = "#" + s;
            }

            return new Color(Integer.decode(s).intValue());
        } catch (Exception e) {
            Color c = COLORNAMES.get(value.toLowerCase());
            if (c == null) {
                return dflt;
            }

            return c;
        }
    }

    public static float square(float x) {
        return (float) Math.pow(x, 2);
    }

    public static float distanceBetweenPoints(float vx, float vy, float wx,
					      float wy) {
        return square(vx - wx) + square(vy - wy);
    }

    public static float distanceToSegmentSquared(float px, float py,
						 float vx, float vy, float wx, float wy) {
        float l2 = distanceBetweenPoints(vx, vy, wx, wy);
        if (l2 == 0) {
            return distanceBetweenPoints(px, py, vx, vy);
        }
        float t = ((px - vx) * (wx - vx) + (py - vy) * (wy - vy)) / l2;
        if (t < 0) {
            return distanceBetweenPoints(px, py, vx, vy);
        }
        if (t > 1) {
            return distanceBetweenPoints(px, py, wx, wy);
        }

        return distanceBetweenPoints(px, py, (vx + t * (wx - vx)),
                                     (vy + t * (wy - vy)));
    }

    public static float perpendicularDistance(float px, float py, float vx,
					      float vy, float wx, float wy) {
        return (float) Math.sqrt(distanceToSegmentSquared(px, py, vx, vy, wx,
							  wy));
    }

    public static float getMaxPerpendicularDistance(List<float[]> list,
						    int s, int e, int[] index) {
        // Find the point with the maximum distance
        float dmax = 0;
        if (index != null) {
            index[0] = 0;
        }

        final int start = s;
        final int end   = e - 1;
        for (int i = start + 1; i < end; i++) {
            // Point
            final float px = list.get(i)[0];
            final float py = list.get(i)[1];
            // Start
            final float vx = list.get(start)[0];
            final float vy = list.get(start)[1];
            // End
            final float wx = list.get(end)[0];
            final float wy = list.get(end)[1];

            final float d  = perpendicularDistance(px, py, vx, vy, wx, wy);
            if (d > dmax) {
                if (index != null) {
                    index[0] = i;
                }
                dmax = d;
            }
        }

        return dmax;
    }

    /**
     * originally from https://github.com/phishman3579/java-algorithms-implementation/blob/master/src/com/jwetherell/algorithms/mathematics/RamerDouglasPeucker.java
     * _more_
     *
     * @param list _more_
     * @param s _more_
     * @param e _more_
     * @param epsilon _more_
     * @param resultList _more_
     */
    private static void douglasPeucker(List<float[]> list, int s, int e,
                                       float epsilon,
                                       List<float[]> resultList) {
        final int start = s;
        final int end   = e - 1;
        // Find the point with the maximum distance
        int[] index = { 0 };
        float dmax  = getMaxPerpendicularDistance(list, s, e, index);
        // If max distance is greater than epsilon, recursively simplify
        if (dmax > epsilon) {
            // Recursive call
            douglasPeucker(list, s, index[0], epsilon, resultList);
            douglasPeucker(list, index[0], e, epsilon, resultList);
        } else {
            if ((end - start) > 0) {
                resultList.add(list.get(start));
                resultList.add(list.get(end));
            } else {
                resultList.add(list.get(start));
            }
        }
    }

    /**
     * Given a curve composed of line segments find a similar curve with fewer points.
     *
     * originally from https://github.com/phishman3579/java-algorithms-implementation/blob/master/src/com/jwetherell/algorithms/mathematics/RamerDouglasPeucker.java
     * @param coords _more_
     * @param epsilon Distance dimension
     * @return Similar curve with fewer points
     */
    public static float[][] douglasPeucker(float[][] coords, float epsilon) {
        List<float[]> incoming = new ArrayList<float[]>();
        for (int i = 0; i < coords[0].length; i++) {
            incoming.add(new float[] { coords[0][i], coords[1][i] });
        }
        final List<float[]> result = new ArrayList<float[]>();
        douglasPeucker(incoming, 0, incoming.size(), epsilon, result);
        //        System.err.println("incoming:"+ incoming.size() +" result:"+ result.size());
        float[][] f = new float[result.get(0).length][result.size()];
        for (int i = 0; i < result.size(); i++) {
            float[] coord = result.get(i);
            f[0][i] = coord[0];
            f[1][i] = coord[1];
        }

        return f;
    }

    /** default decimal formatter */
    private static DecimalFormat formatter = new DecimalFormat();

    public static boolean isStandardMissingValue(String s) {
        //I really shouldn't be doing this here
	s  =s.toLowerCase();
        return (s.length() == 0) || s.equals("---") || s.equals("n.v.")
	    || s.equals("null") || s.equals("nan")
	    || s.equals("na") || s.equals("n/a") 
	    || s.equals("ukn") || s.equals("e");
    }

    /**
     *  copy and paste from the IDV Misc.java to have the formatter by synchronized
     *
     * @param value _more_
     *
     * @return _more_
     *
     * @throws NumberFormatException _more_
     */
    public static double parseNumber(String value)
	throws NumberFormatException {
        if (value.equals(MISSING) || value.equals(NaN)) {
            return Double.NaN;
        }
        try {
            // hack to also accept lower case e for exponent
            if (value.indexOf("e") >= 0) {
                value = value.replace("e", "E");
            }
            synchronized (formatter) {
                return formatter.parse(value).doubleValue();
            }
        } catch (ParseException pe) {
            throw new NumberFormatException(pe.getMessage());
        }
    }

    public static String replace(String s, String...args) {
	for(int i=0;i<args.length;i+=2) {
	    s = s.replace(args[i],args[i+1]);
	}
	return s;
    }

    public static String replaceAll(String s, String...args) {
	for(int i=0;i<args.length;i+=2) {
	    s = s.replaceAll(args[i],args[i+1]);
	}
	return s;
    }    

    public static boolean between(double v, double min, double max) {
        return (v >= min) && (v <= max);
    }

    public static final String MISSING = "missing";

    /** NaN string */
    public static final String NaN = "NaN";

    public static class DateFormat {

        SimpleDateFormat sdf;

        String format;

        Pattern pattern;

        String spattern;

        public DateFormat(String format, String pattern) {
            this.format = format;
            sdf         = new SimpleDateFormat(this.format);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            this.pattern = (pattern != null)
		? Pattern.compile(spattern = pattern)
		: null;
        }

        public DateFormat(String format) {
            this(format, null);
        }

        public DateFormat(String format, boolean cvrt) {
            this(format, convert(format));
        }

        private static String convert(String s) {
            s = s.replaceAll("[yMdHms]", "\\\\d");
            return s;
        }

        public synchronized Date parse(String dttm, ParsePosition pp) {
            if (pattern != null) {
                Matcher m = pattern.matcher(dttm);
                if ( !m.matches()) {
                    if (debugDate) {
                        System.err.println("not match:" + dttm + " pattern:"
                                           + spattern);
                    }

                    return null;
                }
                if (debugDate) {
                    System.err.println("match:" + dttm + " pattern:"
                                       + spattern);
                }
            }
            try {
                Date d= sdf.parse(dttm, pp);
		return d;
            } catch (Exception exc) {
                return null;
            }
        }

    }

    public static class ObjectSorter implements Comparable {

        boolean ascending = true;

        String svalue;

        double value;

        Object object;

        public ObjectSorter(Object object, double value, boolean ascending) {
            this.value     = value;
            this.object    = object;
            this.ascending = ascending;
        }

        public ObjectSorter(Object object, int value, boolean ascending) {
            this.value     = value;
            this.object    = object;
            this.ascending = ascending;
        }

        public ObjectSorter(Object object, String value, boolean ascending) {
            this.svalue    = value;
            this.object    = object;
            this.ascending = ascending;
        }

        public int compareTo(Object o) {
            ObjectSorter that = (ObjectSorter) o;
            if (svalue != null) {
                return svalue.compareTo(that.svalue);
            }
            if (value < that.value) {
                return ascending
		    ? -1
		    : 1;
            }
            if (value > that.value) {
                return ascending
		    ? 1
		    : -1;
            }

            return 0;
        }

        public Object getObject() {
            return object;
        }

        public double getValue() {
            return value;
        }
    }

    public static String[] tokenizeChunk(String s, String start, String end) {
        int idx1 = s.indexOf(start);
        if (idx1 < 0) {
            //            System.err.println("no 1");
            return null;
        }
        int idx2 = s.indexOf(end, idx1);
        if (idx2 < 0) {
            //            System.err.println("no 2");
            return null;
        }
        String chunk = s.substring(idx1 + start.length(), idx2);
        //      System.err.println(idx1 +" " + idx2 +" "  + start.length() +" " + chunk);
        s = s.substring(idx2 + start.length());

        return new String[] { chunk, s };
    }

    public static String[] tokenizePattern(String s, String start,
                                           String end) {
        //String s = "<th >Alabama</th><td >4,887,871</td>";
        Pattern p1 = Pattern.compile(start);
        Matcher m1 = p1.matcher(s);
        if ( !m1.find()) {
            //      System.err.println("no match 1");
            return null;
        }
        s = s.substring(m1.end());
        //      System.err.println("S1:" + s);
        Pattern p2 = Pattern.compile(end);
        Matcher m2 = p2.matcher(s);
        if ( !m2.find()) {
            //      System.err.println("no match 2");
            return null;
        }
        String chunk = s.substring(0, m2.start());
        s = s.substring(m2.end());

        //      System.err.println("C:" + chunk +" s:" + s);
        return new String[] { chunk, s };
    }

    public static String toLowerCase(String s) {
	if(s==null) return null;
	return s.toLowerCase();
    }

    /**
     * Encode the input string
     *
     * @param s  the string to encode
     *
     * @return  the encoded String
     */
    public static final String encodeUntrustedText(String s) {
        //        s = s.replaceAll("&","&amp;;");
        //
        //Note: if this is wrong then we can get an XSS attack from the anonymous upload.
        //If we encode possible attack vectors (<,>) as entities then we edit the entry they
        //get turned into the raw character and we're owned.
        s = s.replaceAll("&", "_AMP_");
        s = s.replaceAll("<", "_LT_");
        s = s.replaceAll(">", "_GT_");
	//I don't think we need to encode the quotes
	//        s = s.replaceAll("\"", "&quot;");
        //        s = HtmlUtils.urlEncode(s);
        //       s = s.replace("+", " ");
        return s;
    }

    public static double getAverage(List<Double> vs) {
        double total = 0;
        for (double d : vs) {
            total += d;
        }

        return ((vs.size() > 0)
                ? total / vs.size()
                : 0);
    }

    public static String unquote(String text) {
        if (text.startsWith("\"") && text.endsWith("\"")) {
            text = text.substring(1);
            text = text.substring(0, text.length() - 1);
        }

        return text;
    }

    public static class TypedList<TYPE> extends ArrayList {

        public TypedList(TYPE... args) {
            for (TYPE arg : args) {
                this.add(arg);
            }
        }
    }

    public static int debugCnt = 0;

    public static void debugCount(int cnt, String s) {
        if (debugCnt++ < cnt) {
            System.err.println(s);
        }
    }

    public static String convertPattern(String p) {
        if (p == null) {
            return null;
        }
        p = p.replaceAll("_leftparen_", "\\\\(").replaceAll("_rightparen_",
							    "\\\\)");
        p = p.replaceAll("_leftbracket_",
                         "\\\\[").replaceAll("_rightbracket_", "\\\\]");
        String hr = "<a[^>]*?href *= *\"?([^ <\"]+)";
        p = p.replaceAll("_href_", hr);
        p = p.replaceAll("_hrefandlabel_", hr + "[^>]*>([^<]+)</a>");
        p = p.replaceAll("_dot_", "\\\\.");
        p = p.replaceAll("_dollar_", "\\\\\\$");
        p = p.replaceAll("_dot_", "\\\\.");
        p = p.replaceAll("_star_", "\\\\*");
        p = p.replaceAll("_plus_", "\\\\+");
        p = p.replaceAll("_nl_", "\n");
        p = p.replaceAll("_quote_", "\"");
        p = p.replaceAll("_qt_", "\"");

        return p;
    }

    public static List<Macro> splitMacros(String s) {
	return splitMacros(s,"${","}");
    }

    public static List<Macro> splitMacros(String s, String opener, String closer) {
	List<String> toks  = splitOnMacros(s, opener,closer);
	List<Macro> macros = new ArrayList<Macro>();
	for(int i=0;i<toks.size();i++) {
	    macros.add(new Macro(i / 2.0 == (int) i / 2,toks.get(i)));
	}
	return macros;
    }

    public static List<String> splitOnMacros(String s,String opener, String closer) {
	List<String> tokens = new ArrayList<String>();
	int idx1 = s.indexOf(opener);
	while (idx1 >= 0) {
	    int idx2 = s.indexOf(closer, idx1);
	    if (idx2 < 0) {
		break;
	    }
	    tokens.add(s.substring(0, idx1));
	    tokens.add(s.substring(idx1 + 2, idx2));
	    s = s.substring(idx2 + 1);
	    idx1 = s.indexOf(opener);
	}
	if (s.length() > 0) {
	    tokens.add(s);
	}
	return tokens;
    }

    public static final String CASE_LOWER ="lower";
    public static final String CASE_UPPER ="upper";
    public static final String CASE_CAMEL ="camel";
    public static final String CASE_PROPER ="proper";    
    public static final String CASE_CAPITALIZE ="capitalize";            

    public static String applyCase(String caseType, String s) {
	if (caseType.equals(CASE_LOWER)) {
	    return s.toLowerCase();
	} else if (caseType.equals(CASE_UPPER)) {
	    return s.toUpperCase();
	} else if (caseType.equals(CASE_PROPER)) {
	    return  Utils.nameCase(s);
	} else if (caseType.equals(CASE_CAMEL)) {
	    return  Utils.upperCaseFirst(s);
	} else if (caseType.equals(CASE_CAPITALIZE)) {
	    if (s.length() == 1) {
		return s.toUpperCase();
	    } else if (s.length() > 1) {
		return  s.substring(0, 1).toUpperCase()
		    + s.substring(1).toLowerCase();
	    }
	} else {
	    System.err.println("Unknown case:" + caseType);
	}
	return s;
    }

    public static class Macro {
	private boolean isText;
	private String macro;
	private Hashtable properties;

	public Macro(boolean isText, String macro) {
	    this.isText= isText;
	    this.macro  =macro;
	    if(!isText) {
		List<String> toks = splitUpTo(this.macro," ",2);
		if(toks.size()>1) {
		    this.macro = toks.get(0).trim();
		    String s = toks.get(1).replace("_quote_","\"");
		    this.properties = parseKeyValue(s);
		}
		//		System.err.println("MACRO: is macro:" + this.macro +" props:" + properties );
	    } else {
		//		System.err.println("MACRO: is text:" + macro);
	    }
	}

	public boolean isText() {
	    return isText;
	}

	public String getText() {
	    return macro;
	}

	public String getId() {
	    return macro;
	}	

	public String toString() {
	    return macro;
	}

	public void putProperty(Object key,Object value) {
	    if(properties==null) properties= new Hashtable();
	    properties.put(key,value);
	}

	public Object getProperty(Object key) {
	    if(properties==null) return null;
	    return properties.get(key);
	}

	public String getProperty(String key, String dflt) {
	    if(properties==null) return dflt;
	    String v = (String) properties.get(key);
	    if(v==null) return dflt;
	    return v;
	}

	public boolean getProperty(String key, boolean dflt) {
	    if(properties==null) return dflt;
	    return Utils.getProperty(properties, key,dflt);
	}

	public double getProperty(String key, double dflt) {
	    if(properties==null) return dflt;
	    return Utils.getProperty(properties, key,dflt);
	}

	public int getProperty(String key, int dflt) {
	    if(properties==null) return dflt;
	    return Utils.getProperty(properties, key,dflt);
	}		

	public Hashtable getProperties() {
	    return properties;
	}

    }

    public static String applyMacros(String template,String...args) {
	for(int i=0;i<args.length;i+=2) {
	    template = template.replace("${" + args[i]+"}",args[i+1]);
	}
	return template;
    }

    public static List<List<String>> multiSplit(Object source, String delim1,String delim2, int tupleSize) {
	List<List<String>> result = new ArrayList<List<String>>();
	for(String tuple:split(source.toString(),delim1,true,true)) {
	    List<String>toks = Utils.splitUpTo(tuple,delim2,tupleSize);
	    result.add(toks);
	}
	return result;
    }

    public static String unescapeNL(String s) {
	return s.replaceAll("\\\\\r?\n","");
    }

    public static List<String> split(Object source) {
        return split(source, ",");
    }

    public static String[] split(Object s, String delim, int cnt) {
        return StringUtil.split(s.toString(), delim, cnt);
    }

    /**
       Copied from ucar.unidata.util.StringUtil
       don't trim the results
    */
    public static List<String> splitUpTo(Object o, String delimiter,
					 int cnt) {
	String s = o.toString();
	List<String> toks = new ArrayList<String>();
	int delimLength = delimiter.length();
	for (int i = 0; i < cnt - 1; i++) {
	    int idx = s.indexOf(delimiter);
	    if (idx < 0) {
		break;
	    }
	    toks.add(s.substring(0, idx));
	    s = s.substring(idx + delimLength);
	}
	if (s.length() > 0) {
	    toks.add(s);
	}
	return toks;
    }

    public static List<String> split(Object s, String delim) {
        return split(s, delim, false, false);
    }

    public static List<String> split(Object o, String delim, boolean trim,
                                     boolean skipBlank) {
	return split(o,delim,trim,skipBlank,false);
    }

    public static List<String> split(Object o, String delim, boolean trim,
                                     boolean skipBlank,boolean handleEscape) {	
        //      List<String> test=    StringUtil.split(o, delim, trim, skipBlank);
        List<String> toks = new ArrayList<String>();
        if (o == null) {
            return toks;
        }
        String s = o.toString();
	if(handleEscape) {
	    s = s.replace("\\"+delim,"_ESCAPEDDELIM_");
	}
        String _delim = Pattern.quote(delim);
	//Pass in -1 so it includes trailing blanks
        String[] a = s.split(_delim,-1);
        for (String tok : a) {
            if (trim) {
                tok = tok.trim();
            }
            if (skipBlank && (tok.length() == 0)) {
                continue;
            }
	    if(handleEscape) {
		tok = tok.replace("_ESCAPEDDELIM_",delim);
	    }
            toks.add(tok);
        }

        /*
          if(test.size()!=toks.size()) {
          System.err.println("Error: " + "delim:" + delim +" o:" + o);
          System.err.println("OLD:" + test.size());
          System.err.println("NEW:"+ toks.size() +" " + a.length);
          }
        */
        return toks;
    }

    public static List<String> splitWithQuotes(String s) {
        ArrayList<String> list = new ArrayList();
        if (s == null) {
            return list;
        }
        s = s.replaceAll("\\\\\"", "_quote_");
        while (true) {
            s = s.trim();
            int qidx1 = s.indexOf("\"");
            int qidx2 = s.indexOf("\"", qidx1 + 1);
            int sidx1 = 0;
            int sidx2 = s.indexOf(" ", sidx1 + 1);
            if ((qidx1 < 0) && (sidx2 < 0)) {
                if (s.length() > 0) {
                    list.add(s);
                }

                break;
            }
            if ((qidx1 >= 0) && ((sidx2 == -1) || (qidx1 < sidx2))) {
                if (qidx1 >= qidx2) {
                    //Malformed string. Add the rest of the line and break
                    if (qidx1 == 0) {
                        s = s.substring(qidx1 + 1);
                    } else if (qidx1 > 0) {
                        s = s.substring(0, qidx1);
                    }
                    if (s.length() > 0) {
                        list.add(s);
                    }

                    break;
                }
                if (qidx2 < 0) {
                    //Malformed string. Add the rest of the line and break
                    s = s.substring(1);
                    list.add(s);

                    break;
                }
                String tok = s.substring(qidx1 + 1, qidx2);
                if (tok.length() > 0) {
                    list.add(tok);
                }
                s = s.substring(qidx2 + 1);
                //                System.err.println ("qtok:" + tok);
            } else {
                if (sidx2 < 0) {
                    list.add(s);

                    break;
                }
                String tok = s.substring(sidx1, sidx2);
                if (tok.length() > 0) {
                    list.add(tok);
                }
                s = s.substring(sidx2);
                //                System.err.println ("stok:" + tok);
            }
        }
        List<String> tmp = new ArrayList<String>();
        for (String tmps : list) {
            tmp.add(tmps.replaceAll("_quote_", "\""));
        }

        return tmp;
    }

    public static List findDescendantsFromPath(Element parent, String path) {
        List results = new ArrayList();
        List tags    = Utils.split(path, ".");
        //In case the path starts with the root node
        if (parent.getTagName().equals(tags.get(0))) {
            tags.remove(0);
        }
        findDescendantsFromPath(parent, tags, 0, results, "\t");

        return results;
    }

    /**
     *  return first non-null object
     *
     * @param args _more_
     *
     * @return _more_
     */
    public static Object get(Object... args) {
        for (Object obj : args) {
            if (obj != null) {
                return obj;
            }
        }

        return null;
    }

    public static String getString(Object... args) {
        for (Object obj : args) {
            if (obj != null) {
                return obj.toString();
            }
        }

        return null;
    }

    public static String getDefinedString(Object... args) {
        for (Object obj : args) {
	    if(obj==null) continue;
	    String s = obj.toString();
	    if(stringDefined(s)) return s;
        }

        return null;
    }    

    public static void findDescendantsFromPath(Element parent, List tags,
					       int tagIdx, List results, String tab) {
        String  tag     = (String) tags.get(tagIdx);
        boolean lastTag = (tagIdx == tags.size() - 1);
        //      System.err.println (tab+XmlUtil.getLocalName(parent) + " looking for:" + tag + " idx:" + tagIdx+ " lastTag:" + lastTag);
        NodeList elements = XmlUtil.getElements(parent);
        tab = tab + "\t";
        for (int i = 0; i < elements.getLength(); i++) {
            Element child = (Element) elements.item(i);
            //      System.err.println (tab+">child:" + XmlUtil.getLocalName(child));
            if (tag.equals(XmlUtil.TAG_WILDCARD)
		|| XmlUtil.isTag(child, tag)) {
                if (lastTag) {
                    results.add(child);
                } else {
                    findDescendantsFromPath(child, tags, tagIdx + 1, results,
                                            tab);
                }
            }
        }
    }

    public static boolean containsIgnoreCase(List<String> l, String value) {
        for (String s : l) {
            if (s.equalsIgnoreCase(value)) {
                return true;
            }
        }

        return false;
    }

    /**
       Clip the length of the string to the given length. Add the suffix at the end if clipped
    */
    public static String clip(String s, int length,String suffix) {
	if(s==null) return null;
	if(s.length()>length) {
	    s = s.substring(0,length) +suffix;
	}
	return s;
    }

    public static String makeCacheKey(Object...list) {
	StringBuilder sb = new StringBuilder();
	for(Object o:list) {
	    if(o!=null) {
		String s = o.toString();
		sb.append(s);
	    } else {
		sb.append("null");
	    }
	    sb.append("_"); 
	}
	return sb.toString();
    }

    public static List<String> clip(List<String> list) {
	return clip(list,-1, 8,"...");
    }

    public static List<String> clip(List<String> list, int listLength,
				    int length,String suffix) {
	if(list==null) return null;
	List<String> result=new ArrayList<String>();
	for(int i=0;i<list.size();i++) {
	    if(listLength>=0 && i>=listLength) {
		break;
	    }
	    result.add(clip(list.get(i),length,suffix));
	}
	return result;
    }    

    public static String X(String c) {
	if(c==null) return c;
	if(c.length()>4) c = c.substring(0,3);
	return c;
    }

    public static List<String> Y(List l) {
	List<String> tmp = new ArrayList<String>();
	for(Object o: l) tmp.add(X(o.toString()));
	return tmp;
    }

    public static String join(List l, String delim) {
        return join(l, delim, false);
    }

    public static String join(String delim, String...args) {
	return join(Arrays.asList(args),delim);
    }

    public static String join(List l, String delim, boolean inverse) {
        StringBuilder sb = new StringBuilder();
        if (inverse) {
            for (int i = l.size() - 1; i >= 0; i--) {
		Object s = l.get(i);
		if(s==null) continue;
                if (sb.length() > 0) {
                    sb.append(delim);
                }
                sb.append(s);
            }
        } else {
            for (int i = 0; i < l.size(); i++) {
		Object s = l.get(i);
		if(s==null) continue;
                if (sb.length() > 0) {
                    sb.append(delim);
                }
                sb.append(l.get(i));

            }
        }

        return sb.toString();

    }

    public static String wrap(List l, String prefix, String suffix) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < l.size(); i++) {
            String s = prefix + l.get(i) + suffix;
            s = s.replace("${index}", i + "");
            sb.append(s);
        }

        return sb.toString();
    }

    public static List<List> splitList(List list, int max, int... remainder) {
        List<List> lists = new ArrayList<List>();
        if (list.size() < max) {
            lists.add(list);
        } else {
            int     rem   = (remainder.length > 0)
		? remainder[0]
		: 0;
            boolean debug = false;
            if (debug) {
                System.err.println("*** max:" + max + " rem:" + rem);
            }
            List current = new ArrayList();
            lists.add(current);
            int     added      = 0;
            boolean addedExtra = false;
            for (int i = 0; i < list.size(); i++) {
                Object o = list.get(i);
                if (added >= max) {
                    addedExtra = false;
                    if (debug) {
                        System.err.println("new one");
                    }
                    current = new ArrayList();
                    lists.add(current);
                    added = 0;
                }
                added++;
                if (debug) {
                    System.err.println("\tadding:" + o + "rem:" + rem);
                }
                current.add(o);
                if ( !addedExtra && (rem > 0) && (i < list.size())) {
                    addedExtra = true;
                    rem--;
                    o = list.get(++i);
                    if (debug) {
                        System.err.println("\textra:" + o + "rem:" + rem
                                           + " i:" + (i - 1) + " size:"
                                           + list.size());
                    }
                    current.add(o);
                }
            }
        }

        return lists;
    }

    public static void addAllUnique(List source,List  other) {
	for(Object o:other) {
	    if(!source.contains(o)) source.add(o);
	}
    }

    public static List sort(Collection listToSort) {
        Object[] array = listToSort.toArray();
        Arrays.sort(array);

        return Arrays.asList(array);
    }

    public static boolean equals(Object o1, Object o2) {
        if ((o1 != null) && (o2 != null)) {
            return o1.equals(o2);
        }

        return ((o1 == null) && (o2 == null));
    }

    public static boolean matches(String s, String pattern) {
	if(s==null) return false;
	return s.matches(pattern);
    }

    public static boolean contains(HashSet set, Object o) {
	if(o==null) return false;
	return set.contains(o);
    }

    public static String clipTo(String s, int length, String pad) {
        if (s.length() > length) {
            s = s.substring(0, length) + pad;
        }

        return s;
    }

    public static int findNext(String s, int baseIdx, String p) {
        int     len      = s.length();
        boolean inEscape = false;
        boolean inQuote  = false;
        int     pIdx     = 0;
        char[]  ps       = p.toCharArray();
        for (int idx = baseIdx; idx < len; idx++) {
            char c = s.charAt(idx);
            if (inEscape) {
                inEscape = false;

                continue;
            }
            if (c == '\\') {
                inEscape = true;

                continue;
            }
            if (c == '\"') {
                if (inQuote) {
                    inQuote = false;
                } else {
                    inQuote = true;
                }

                continue;
            }
            if (inQuote) {
                pIdx = 0;

                continue;
            }
            if (ps[pIdx] == c) {
                pIdx++;
                if (pIdx >= ps.length) {
                    return idx - ps.length + 1;
                }

                continue;
            }
        }

        return -1;
    }

    public static String unescape(String s) {
        if (s.indexOf("\\") < 0) {
            return s;
        }
        StringBuilder buff     = new StringBuilder();
        int           len      = s.length();
        boolean       inEscape = false;
        for (int idx = 0; idx < len; idx++) {
            char c = s.charAt(idx);
            if (inEscape) {
                inEscape = false;
                buff.append(c);

                continue;
            }
            if (c == '\\') {
                inEscape = true;

                continue;
            }
            buff.append(c);
        }

        return buff.toString();
    }

    /**
     * Utility to get the elapsed minutes
     *
     * @param date date
     *
     * @return Elapsed minutes
     */
    public static int getElapsedMinutes(Date date) {
        return getElapsedMinutes(new Date(), date);
    }

    /**
     * Utility to the the minutes between the given dates
     *
     * @param now date 1
     * @param date date 2
     *
     * @return elapsed minutes
     */
    public static int getElapsedMinutes(Date now, Date date) {
        if (date == null) {
            return 0;
        }

        return (int) (now.getTime() - date.getTime()) / 1000 / 60;
    }

    public static double millisTo(long ms,String what) {
	if(what.startsWith(TIME_UNIT_SECOND)) return millisToSeconds(ms);
	if(what.startsWith(TIME_UNIT_MINUTE)) return millisToMinutes(ms);	
	if(what.startsWith(TIME_UNIT_HOUR)) return millisToHours(ms);	
	if(what.startsWith(TIME_UNIT_DAY)) return millisToDays(ms);	
	if(what.startsWith(TIME_UNIT_WEEK)) return millisToWeeks(ms);	
	if(what.startsWith(TIME_UNIT_MONTH)) return millisToMonths(ms);	
	if(what.startsWith(TIME_UNIT_YEAR)) return millisToYears(ms);	
	throw new IllegalArgumentException("Unknown time unit to convert milliseconds to:" + what);
    }

    public static long toMillis(double time, String what) {
	if(what.startsWith(TIME_UNIT_SECOND)) return secondsToMillis(time);
	if(what.startsWith(TIME_UNIT_MINUTE)) return minutesToMillis(time);	
	if(what.startsWith(TIME_UNIT_HOUR)) return hoursToMillis(time);	
	if(what.startsWith(TIME_UNIT_DAY)) return daysToMillis(time);	
	if(what.startsWith(TIME_UNIT_WEEK)) return weeksToMillis(time);	
	if(what.startsWith(TIME_UNIT_MONTH)) return monthsToMillis(time);	
	if(what.startsWith(TIME_UNIT_YEAR)) return yearsToMillis(time);	
	throw new IllegalArgumentException("Unknown time unit to convert time to milliseconds to:" + what);
    }

    public static double millisToSeconds(long ms) {
	return ms/1000.0;
    }
    public static double millisToMinutes(long ms) {
	return ms/(1000.0*60);
    }
    public static double millisToHours(long ms) {
	return ms/(1000.0*60*60);
    }
    public static double millisToDays(long ms) {
	return ms/(1000.0*60*60*24);
    }
    public static double millisToWeeks(long ms) {
	return ms/(1000.0*60*60*24*7);
    }
    public static double millisToMonths(long ms) {
	return ms/(1000.0*60*60*24*30);
    }
    public static double millisToYears(long ms) {
	return ms/(1000.0*60*60*24*365);
    }                

    public static long minutesToMillis(double minutes) {
        return (long)(minutes * 60 * 1000);
    }

    public static long hoursToMillis(double hours) {
        return minutesToMillis(hours*60);
    }

    public static long daysToMillis(double days) {
        return hoursToMillis(days*24);
    }

    public static long weeksToMillis(double weeks) {
        return daysToMillis(weeks*7);
    }
    public static long monthsToMillis(double months) {
        return daysToMillis(months*30);
    }    

    public static long yearsToMillis(double years) {
        return daysToMillis(years*365);
    }            

    public static long secondsToMillis(double seconds) {
        return (long)(seconds * 1000);
    }

    public static void sleepUntil(int frequency, boolean debug) {
        Date              now = new Date();
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(now);
        int minute        = cal.get(cal.MINUTE);
        int seconds       = cal.get(cal.SECOND) + minute * 60;
        int secondsToWait = (frequency * 60) - (seconds % (frequency * 60));
        int minutesToWait = frequency - (minute % frequency);
        if (debug) {
            System.err.println("Sleeping for: " + (secondsToWait / 60) + ":"
                               + (secondsToWait % 60));
        }
        Misc.sleepSeconds(minutesToWait * 60);
    }

    public static void pauseEvery(int minutesDelta,Appendable msg)  {
	try {
	    long sleepTime = Misc.getPauseEveryTime(minutesDelta);
	    if(msg!=null) {
		msg.append(""+new Date(new Date().getTime()+sleepTime));
	    }
	    Misc.sleep((long) sleepTime);
	} catch(Exception exc) {
	    throw new RuntimeException(exc);
	}
    }

    public static Date[] getDateRange(String fromDate, String toDate,
                                      Date base)
	throws java.text.ParseException {

        Date fromDttm = Utils.stringDefined(fromDate)
	    ? DateUtil.parseRelative(base, fromDate, -1)
	    : null;
        Date toDttm   = Utils.stringDefined(toDate)
	    ? DateUtil.parseRelative(base, toDate, +1)
	    : null;
        //      System.err.println ("dflt: " + base);
        //      System.err.println ("fromDttm:" + fromDate + " date:" + fromDttm);
        //      System.err.println ("toDttm:" + toDate + " date:" + toDttm);

        if ((Utils.stringDefined(fromDate)) && (fromDttm == null)) {
            if ( !fromDate.startsWith("-")) {
                fromDttm = Utils.parseDate(fromDate);
            }
        }
        if ((Utils.stringDefined(toDate)) && (toDttm == null)) {
            if ( !toDate.startsWith("+")) {
                toDttm = Utils.parseDate(toDate);
            }
        }

        if ((fromDttm == null) && (fromDate != null)
	    && fromDate.startsWith("-")) {
            if (toDttm == null) {
                throw new IllegalArgumentException(
						   "Cannot do relative From Date when To Date is not set");
            }
            fromDttm = DateUtil.getRelativeDate(toDttm, fromDate);
        }

        if ((toDttm == null) && (toDate != null) && toDate.startsWith("+")) {
            if (fromDttm == null) {
                throw new IllegalArgumentException(
						   "Cannot do relative From Date when To Date is not set");
            }
            toDttm = DateUtil.getRelativeDate(fromDttm, toDate);
        }

        return new Date[] { fromDttm, toDttm };
    }

    public static DateFormat[] getFormatters() {
        return DATE_FORMATS;
    }

    private static SimpleDateFormat doySdf;

    public static Date parse(String s) throws java.text.ParseException {
        boolean debug = true;

        //Check for yyyy-DDD
        if ((s.length() == 8) && s.substring(4, 5).equals("-")) {
            try {
                if (doySdf == null) {
                    doySdf = new SimpleDateFormat("yyyy-DDD");
                }
                synchronized (doySdf) {
                    Date date = doySdf.parse(s);

                    //              System.err.println("using doy:" + s +" " + date);
                    return date;
                }
            } catch (java.text.ParseException pe) {}
        }

        if ((lastFormat != null) && (lengthLastDate == s.length())) {
            Date date = lastFormat.parse(s, null);
            if (date != null) {
                if (debug) {
                    System.err.println("Using lastSdf format= "
                                       + lastFormat.format);
                }

                return date;
            }
        }

        for (DateFormat sdf : getFormatters()) {
            if (debug) {
                System.err.println("   trying " + sdf.format);
            }
            Date dttm = sdf.parse(s, null);
            if (dttm == null) {
                continue;
            }
            lastFormat     = sdf;
            lengthLastDate = s.length();
            if (debug) {
                System.err.println("   success:" + " date:" + s + " format:"
                                   + sdf.format);
            }

            return dttm;
        }

        throw new IllegalArgumentException("Could not find date format for:"
                                           + s);
    }

    public static String plural(int count, String label) {
        if (count == 1) {
            return  label;
        }

        return  label + "s";
    }

    public static String formatMinutes(int minutes) {
        if (minutes < 60) {
            return minutes+" " +plural(minutes, TIME_UNIT_MINUTE);
        }
        int hours = minutes / 60;
        int rem   = minutes - (hours * 60);
        if (rem == 0) {
            return hours+" " + plural(hours, TIME_UNIT_HOUR);
        }

        return hours+ " "+ plural(hours, TIME_UNIT_HOUR) + " " + rem+" " + plural(rem, TIME_UNIT_MINUTE);
    }

    public static String getYoutubeID(String url) {
        String id = StringUtil.findPattern(url, "v=([^&]+)&");
        if (id == null) {
            id = StringUtil.findPattern(url, "v=([^&]+)");
        }
        if (id == null) {
            id = StringUtil.findPattern(url, "youtu.be/([^&]+)");
        }

        if (id == null) {
            id = StringUtil.findPattern(url, ".*/watch/([^&/]+)");
        }
        if (id == null) {
            id = StringUtil.findPattern(url, ".*/v/([^&/]+)");
        }

        return id;
    }

    public static String getStack(int howMany) {
        return getStack(howMany, null);
    }

    public static String getStack(int howMany, String not) {
        return getStack(howMany, not, false);
    }

    public static String getStack(int howMany, String not,
                                  boolean stripPackage) {
        List<String> lines = new ArrayList<String>();
        StringBuffer sb    = new StringBuffer();
        int          cnt   = 0;
        for (String line : split(Misc.getStackTrace(), "\n")) {
            if ((not != null) && (line.indexOf(not) >= 0)) {
                continue;
            }
            cnt++;
            //skip the first 3 lines so we don't get this method, etc
            if (cnt <= 5) {
                continue;
            }
            if (stripPackage) {
		//                line = line.replaceAll(".*?\\.([^\\.]+\\.[^\\.]+\\()", "\t$1");
                line = line.replaceAll(".*?\\.([^\\.]+\\.[^\\.]+\\()", "\t$1");		
            }
            lines.add(line);
            if (lines.size() >= howMany) {
                break;
            }
        }

        return join(lines, "\n", false);
    }

    public static double max(double d1, double d2) {
        if (Double.isNaN(d1)) {
            return d2;
        }
        if (Double.isNaN(d2)) {
            return d1;
        }

        return Math.max(d1, d2);
    }

    public static float max(float d1, float d2) {
        if (Float.isNaN(d1)) {
            return d2;
        }
        if (Float.isNaN(d2)) {
            return d1;
        }

        return Math.max(d1, d2);
    }

    public static double min(double d1, double d2) {
        if (Double.isNaN(d1)) {
            return d2;
        }
        if (Double.isNaN(d2)) {
            return d1;
        }

        return Math.min(d1, d2);
    }

    public static float min(float d1, float d2) {
        if (Float.isNaN(d1)) {
            return d2;
        }
        if (Float.isNaN(d2)) {
            return d1;
        }

        return Math.min(d1, d2);
    }

    public static Date min(Date d1, Date d2) {
        if (d1 == null) {
            return d2;
        }
        if (d2 == null) {
            return d1;
        }
        if (d1.getTime() < d2.getTime()) {
            return d1;
        }

        return d2;
    }

    public static Date getStartOfYear(Date date) {
	Calendar startOfYear = Calendar.getInstance();
        startOfYear.setTime(date);
        startOfYear.set(Calendar.MONTH, Calendar.JANUARY);
        startOfYear.set(Calendar.DAY_OF_MONTH, 1);
        startOfYear.set(Calendar.HOUR_OF_DAY, 0);
        startOfYear.set(Calendar.MINUTE, 0);
        startOfYear.set(Calendar.SECOND, 0);
        startOfYear.set(Calendar.MILLISECOND, 0);
	return startOfYear.getTime();
    }

    public static Date max(Date d1, Date d2) {
        if (d1 == null) {
            return d2;
        }
        if (d2 == null) {
            return d1;
        }
        if (d1.getTime() > d2.getTime()) {
            return d1;
        }

        return d2;
    }

    public interface VarArgsConsumer<T> {
        void accept(T ...s);
    }

    public interface UniConsumer<T> {void accept(T t);}
    public interface BiConsumer<S,T> {void accept(S s, T t);}
    public interface TriConsumer<T, U, V> {void accept(T t, U u, V v);}
    public interface UniFunction<R, T> {R call(T t);}
    public interface BiFunction<R, T,U> {R call(T t,U u);}    
    public interface TriFunction<R, T, U, V> {R call(T t, U u, V v);}
    public interface QuadFunction<R, T, U, V, W> {R call(T t, U u, V v, W w);}
    public interface QuadConsumer<T, U, V, W> {void accept(T t, U u, V v, W w);}

    public static void exitTest(int v) {
	System.exit(v);
    }

    public static boolean isImage(String path) {
        if (path == null) {
            return false;
        }
        path = path.replaceAll("\\?.*?$", "").toLowerCase();
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")
	    || path.endsWith(".gif") || path.endsWith(".png")
	    || path.endsWith(".webp") || path.endsWith(".bmp")) {
            return true;
        }
        //wms layer
        if (path.startsWith("http") && (path.indexOf("format=image") >= 0)) {
            return true;
        }

        return false;
    }

    public static String removeInvalidUtf8Bytes(String input,boolean removeZeroByte) {
        byte[] utf8Bytes = input.getBytes(StandardCharsets.UTF_8);
        StringBuilder validUtf8String = new StringBuilder();
        int i = 0;
        while (i < utf8Bytes.length) {
            byte currentByte = utf8Bytes[i];
            int bytesToRead = bytesToReadForUtf8(currentByte);
	    if(currentByte==0x00 && removeZeroByte) {
		i++;
		continue;
	    }
            if (i + bytesToRead > utf8Bytes.length) {
                // If there are not enough bytes left for a complete character, skip them
                i++;
            } else {
                // Append the valid bytes to the output
                for (int j = 0; j < bytesToRead; j++) {
                    validUtf8String.append((char) utf8Bytes[i + j]);
                }
                i += bytesToRead;
            }
        }

        return validUtf8String.toString();
    }

    private static int bytesToReadForUtf8(byte leadingByte) {
        if ((leadingByte & 0b10000000) == 0) {
            return 1; // ASCII character
        } else if ((leadingByte & 0b11100000) == 0b11000000) {
            return 2; // 2-byte character
        } else if ((leadingByte & 0b11110000) == 0b11100000) {
            return 3; // 3-byte character
        } else if ((leadingByte & 0b11111000) == 0b11110000) {
            return 4; // 4-byte character
        }
        return 1; // Default to skip invalid byte
    }

    public static void main(String[] args) throws Exception {

	if(true) {
	    String c = IO.readInputStream(new FileInputStream(args[0]));
	    c= stripTags(c);
	    System.out.println(c);
	    return;
	}

	if(true) {
	    String s = "date: ${setdate date=\"2000-01-04\"} ${date format=\"iso8601\"} ${foo} -1 week: ${date offset=\"-1 week\"}  date:now ${date date=\"20240201\" format=\"yyyyMMdd\" parseFormat=\"yyyyMMdd\"} xx";
	    System.err.println(s);
	    System.err.println(normalizeTemplateUrl(s));
	    return;
	}

	if(true) {
	    String s = "format=\"yyyy-MM-dd HH:mm\" link=true";
	    System.err.println(parseKeyValue(s).get("format"));
	    return;
	}

	if(true) {
	    String fmt = "yyyy-MM-dd'T'HH:mm:ssZ";
	    System.err.println(fmt);
	    SimpleDateFormat sdf = new SimpleDateFormat(fmt);
	    String d = "2021-09-23T13:54:05+0000";
	    String d2 = "2021-09-23T13:54:05";	    
	    System.err.println(sdf.parse(d));
	    return;
	}

	if(true) {
	    String label = " foo (untis)";
	    label = label.replaceAll("\\([^\\)]+\\)", "XXX");
	    System.err.println(label);
	    return;
	}

	if(true) {
	    for(String s: args) {
		System.err.println(makeLabel(s));
	    }
            exitTest(0);
	}

        if (true) {
	    System.err.println("S:" + splitMacros("hello there ${how foo=bar} i am ${fine}"));
            exitTest(0);
	}

        if (true) {
            for (String s : args) {
		System.err.println(s +" " + toInt(split(s,",",true,true)));
            }
            exitTest(0);
        }
        if (true) {
            for (Provider provider : Security.getProviders()) {
                System.out.println(provider.getName());
                for (String key : provider.stringPropertyNames()) {
                    System.out.println("\t" + key + "\t"
                                       + provider.getProperty(key));
                }
            }
        }

        InputStream fis = new FileInputStream(args[0]);
        byte[]      b   = decodeBase64(IOUtil.readBytes(fis));
        IOUtil.writeBytes(new File("out.pdf"), b);
        exitTest(0);

        System.err.println(getStack(10, null, true));
        System.err.println(getStack(10, null, false));
        exitTest(0);

        Date d = new Date();
        System.err.println(formatIso(d));
        exitTest(0);
        String s = "hello there hello";
        System.err.println(s.replace("hello", "xxx"));
        if (true) {
            return;
        }

        for (String dateString : new String[] { "04/01/2021" }) {
            debugDate = true;
            Date date = parseDate(dateString);
            System.err.println("date:" + dateString + " date:" + date);
        }
        exitTest(0);
    }

    public static void printMethods(Object obj) {
        // Get the class of the object
        Class<?> objClass = obj.getClass();

        // Get all methods of the class
        Method[] methods = objClass.getDeclaredMethods();

        // Print each method
        for (Method method : methods) {
            System.out.println(method.getName());
        }
    }

    private static  Map<String, Integer> calendarFields;

    public static int getCalendarField(String fieldName) {
	if(calendarFields==null) {
	    Map<String, Integer> tmp  = new HashMap<String,Integer>();
	    tmp.put("year", Calendar.YEAR);
	    tmp.put("month", Calendar.MONTH);
	    tmp.put("day", Calendar.DAY_OF_MONTH);
	    tmp.put("hour", Calendar.HOUR);
	    tmp.put("hour_of_day", Calendar.HOUR_OF_DAY);
	    tmp.put("minute", Calendar.MINUTE);
	    tmp.put("second", Calendar.SECOND);
	    tmp.put("millisecond", Calendar.MILLISECOND);
	    tmp.put("week_of_year", Calendar.WEEK_OF_YEAR);
	    tmp.put("week_of_month", Calendar.WEEK_OF_MONTH);
	    tmp.put("day_of_year", Calendar.DAY_OF_YEAR);
	    tmp.put("day_of_week", Calendar.DAY_OF_WEEK);
	    tmp.put("day_of_week_in_month", Calendar.DAY_OF_WEEK_IN_MONTH);
	    calendarFields = tmp;
	}
	

        // Lookup the corresponding Calendar field constant
        Integer fieldConstant = calendarFields.get(fieldName.toLowerCase());

        if (fieldConstant == null) {
	    return -1;
	}
	return fieldConstant;
    }





}
