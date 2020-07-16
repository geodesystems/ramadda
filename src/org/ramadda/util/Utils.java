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

package org.ramadda.util;


import org.apache.commons.lang.text.StrTokenizer;

import org.w3c.dom.*;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;

import java.awt.Toolkit;
import java.awt.image.*;
import java.awt.image.BufferedImage;

import java.io.*;

import java.lang.reflect.Constructor;

import java.net.*;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.ParsePosition;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.imageio.*;




/**
 * A collection of utilities
 *
 * @author Jeff McWhirter
 */

public class Utils extends IO {

    /** _more_ */
    public static final String[] LETTERS = {
        "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N",
        "O", "P", "R", "S", "T", "U", "V", "W", "X", "Y", "Z",
    };

    /** _more_ */
    private static DecimalFormat[] FORMATS = {
        new DecimalFormat("#0"), new DecimalFormat("#0.0"),
        new DecimalFormat("#0.00"), new DecimalFormat("#0.000"),
        new DecimalFormat("#0.0000"), new DecimalFormat("#0.00000"),
    };

    /** _more_ */
    private static DecimalFormat[] COMMA_FORMATS = {
        new DecimalFormat("#,##0"), new DecimalFormat("#,##0.0"),
        new DecimalFormat("#,##0.00"), new DecimalFormat("#,##0.000"),
        new DecimalFormat("#,##0.0000"), new DecimalFormat("#,##0.00000"),
    };

    /** _more_ */
    private static DecimalFormat INT_FORMAT = new DecimalFormat("#,##0");


    /** _more_ */
    public static final TimeZone TIMEZONE_DEFAULT =
        TimeZone.getTimeZone("UTC");


    /** _more_ */
    public static final SimpleDateFormat sdf;

    static {
        sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        sdf.setTimeZone(TIMEZONE_DEFAULT);
    }



    /**
     * _more_
     *
     * @param date _more_
     *
     * @return _more_
     */
    public static String format(Date date) {
        synchronized (sdf) {
            //The sdf produces a time zone that isn't RFC3399 compatible so we just tack on the "Z"
            return sdf.format(date) + "Z";
        }
    }



    /**
     * _more_
     *
     * @param d _more_
     *
     * @return _more_
     */
    public static String format(double d) {
        return getFormat(d).format(d);
    }

    /**
     * _more_
     *
     * @param d _more_
     *
     * @return _more_
     */
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


    /**
     * _more_
     *
     * @param d _more_
     *
     * @return _more_
     */
    public static String formatComma(double d) {
        return getFormatComma(d).format(d);
    }

    /**
     * _more_
     *
     * @param d _more_
     *
     * @return _more_
     */
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


    /**
     * _more_
     *
     * @return _more_
     */
    public static Appendable makeAppendable() {
        return new StringBuilder();
    }


    /**
     * _more_
     *
     * @param l _more_
     * @param c _more_
     *
     * @return _more_
     */
    public static String appendList(String l, String c) {
        if (l == null) {
            l = "";
        } else if (l.length() > 0) {
            l += ",";
        }
        l += c;

        return l;
    }

    /**
     * _more_
     *
     * @param list _more_
     * @param args _more_
     *
     * @return _more_
     */
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

    /**
     * _more_
     *
     * @param map _more_
     * @param args _more_
     *
     * @return _more_
     */
    public static Hashtable put(Hashtable map, Object... args) {
        for (int i = 0; i < args.length; i += 2) {
            map.put(args[i], args[i + 1]);
        }

        return map;
    }

    /**
     * _more_
     *
     * @param sb _more_
     * @param args _more_
     *
     * @return _more_
     */
    public static Appendable append(Appendable sb, Object... args) {
        try {
            for (Object s : args) {
                if (s != null) {
                    sb.append(s.toString());
                } else {
                    sb.append("null");
                }
            }

            return sb;
        } catch (java.io.IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    /**
     * _more_
     *
     * @param args _more_
     *
     * @return _more_
     */
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


    /**
     * _more_
     *
     * @param sb _more_
     * @param args _more_
     */
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


    /**
     * _more_
     *
     * @param source _more_
     * @param rowDelimiter _more_
     * @param columnDelimiter _more_
     * @param skip _more_
     *
     * @return _more_
     */
    public static List<List<String>> tokenize(String source,
            String rowDelimiter, String columnDelimiter, int skip) {
        int                cnt     = 0;
        List<List<String>> results = new ArrayList<List<String>>();
        List<String> lines = StringUtil.split(source, rowDelimiter, true,
                                 true);
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


    /**
     * _more_
     *
     * @param line _more_
     * @param widths _more_
     *
     * @return _more_
     */
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



    /**
     * _more_
     *
     * @param line _more_
     * @param columnDelimiter _more_
     *
     * @return _more_
     */
    public static List<String> tokenizeColumns(String line,
            String columnDelimiter) {
        //      System.err.println("line:" + line);
        //        System.err.println("line:" + line.replaceAll("\t","_TAB_"));
        List<String> toks      = new ArrayList<String>();
        StrTokenizer tokenizer = StrTokenizer.getCSVInstance(line);
        tokenizer.setEmptyTokenAsNull(true);
        //        StrTokenizer tokenizer = new StrTokenizer(line, columnDelimiter);
        if ( !columnDelimiter.equals(",")) {
            tokenizer.setDelimiterChar(columnDelimiter.charAt(0));
        }
        //        tokenizer.setQuoteChar('"');
        while (tokenizer.hasNext()) {
            String tok = tokenizer.nextToken();

            if (tok == null) {
                tok = "";
            }
            //      System.err.println("\ttok:" + tok);
            toks.add(tok);
        }

        return toks;
    }

    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public static String trim(String s) {
        if (s == null) {
            return null;
        }

        return s.trim();
    }

    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public static boolean stringDefined(String s) {
        if ((s == null) || (s.trim().length() == 0)) {
            return false;
        }

        return true;
    }


    /**
     * _more_
     *
     * @param d _more_
     *
     * @return _more_
     */
    public static boolean isReal(double d) {
        if ( !Double.isNaN(d) && (d != Double.POSITIVE_INFINITY)
                && (d != Double.NEGATIVE_INFINITY)) {
            return true;
        }

        return false;
    }



    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public static boolean stringUndefined(String s) {
        return !stringDefined(s);
    }






    /**
     * _more_
     *
     * @param modifiedJulian _more_
     *
     * @return _more_
     */
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

    /** _more_ */
    public static double HALFSECOND = 0.5;

    /**
     * _more_
     *
     * @param ymd _more_
     *
     * @return _more_
     */
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

    /**
     * _more_
     *
     * @param injulian _more_
     * @param src _more_
     *
     * @return _more_
     */
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



    /**
     * _more_
     *
     * @param args _more_
     */
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

    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public static String getArticle(String s) {
        s = s.toLowerCase();
        if (s.startsWith("a") || s.startsWith("e") || s.startsWith("i")
                || s.startsWith("o") || s.startsWith("u")) {
            return "an";
        } else {
            return "a";
        }
    }

    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public static Date extractDate(String s) {
        try {
            String yyyy = "\\d\\d\\d\\d";
            String str = StringUtil.findPattern(s,
                             "(" + yyyy + "-\\d\\d-\\d\\d)");
            if (str != null) {
                //                System.err.println("pattern 1:" + str);
                return DateUtil.parse(str);
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


    /**
     * _more_
     *
     * @param c _more_
     * @param paramTypes _more_
     *
     * @return _more_
     */
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

    /**
     * _more_
     *
     * @param formals _more_
     * @param actuals _more_
     *
     * @return _more_
     */
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





    /**
     * _more_
     *
     * @param s _more_
     * @param regexp _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static String[] findPatterns(String s, String regexp)
            throws Exception {
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

    /**
     * _more_
     *
     * @param source _more_
     * @param datePatterns _more_
     * @param dateFormats _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
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

    /**
     * _more_
     *
     * @param format _more_
     *
     * @return _more_
     */
    public static SimpleDateFormat makeDateFormat(String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        return sdf;
    }


    /**
     * _more_
     *
     * @param date _more_
     *
     * @return _more_
     */
    public static int getYear(Date date) {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(date);

        return cal.get(cal.YEAR);
    }


    /**
     * _more_
     *
     * @param date _more_
     *
     * @return _more_
     */
    public static int getMonth(Date date) {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(date);

        return cal.get(cal.MONTH);
    }

    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public static String removeNonAscii(String s) {
        return removeNonAscii(s, "_");
    }

    /**
     * _more_
     *
     * @param s _more_
     * @param replace _more_
     *
     * @return _more_
     */
    public static String removeNonAscii(String s, String replace) {
        s = s.replaceAll("[^\r\n\\x20-\\x7E]+", replace);

        return s;
    }

    /**
     * _more_
     *
     * @param node _more_
     * @param attrOrTag _more_
     * @param dflt _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static String getAttributeOrTag(Element node, String attrOrTag,
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
        if (attrValue == null) {
            attrValue = dflt;
        }

        return attrValue;
    }






    /**
     * _more_
     *
     * @param node _more_
     * @param attrOrTag _more_
     * @param dflt _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static boolean getAttributeOrTag(Element node, String attrOrTag,
                                            boolean dflt)
            throws Exception {
        String attrValue = getAttributeOrTag(node, attrOrTag, (String) null);
        if (attrValue == null) {
            return dflt;
        }

        return attrValue.equals("true");
    }


    /**
     * _more_
     *
     * @param node _more_
     * @param attrOrTag _more_
     * @param dflt _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static int getAttributeOrTag(Element node, String attrOrTag,
                                        int dflt)
            throws Exception {
        String attrValue = getAttributeOrTag(node, attrOrTag, (String) null);
        if (attrValue == null) {
            return dflt;
        }

        return new Integer(attrValue).intValue();
    }


    /**
     * _more_
     *
     * @param node _more_
     * @param attrOrTag _more_
     * @param dflt _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static double getAttributeOrTag(Element node, String attrOrTag,
                                           double dflt)
            throws Exception {
        String attrValue = getAttributeOrTag(node, attrOrTag, (String) null);
        if (attrValue == null) {
            return dflt;
        }

        return new Double(attrValue).doubleValue();
    }

    /**
     * _more_
     *
     * @param properties _more_
     *
     * @return _more_
     */
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
     * _more_
     *
     * @param args _more_
     *
     * @return _more_
     */
    public static Hashtable makeMap(Object... args) {
        Hashtable map = new Hashtable();
        for (int i = 0; i < args.length; i += 2) {
            map.put(args[i], args[i + 1]);
        }

        return map;
    }



    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public static Hashtable getProperties(String s) {
        Hashtable p = new Hashtable();
        for (String line : StringUtil.split(s, "\n", true, true)) {
            if (line.startsWith("#")) {
                continue;
            }
            List<String> toks = StringUtil.splitUpTo(line, "=", 2);
            if (toks.size() == 2) {
                p.put(toks.get(0), toks.get(1));
            } else if (toks.size() == 2) {
                p.put(toks.get(0), "");
            }
        }

        return p;
    }

    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public static String hexEncode(String s) {
        byte[]       chars = s.getBytes();
        StringBuffer sb    = new StringBuffer();
        for (byte c : chars) {
            sb.append("\\x");
            sb.append(c);
        }

        return sb.toString();

    }

    /**
     * _more_
     *
     * @param path _more_
     *
     * @return _more_
     */
    public static boolean isImage(String path) {
        if (path == null) {
            return false;
        }
        path = path.replaceAll("\\?.*?$", "").toLowerCase();
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")
                || path.endsWith(".gif") || path.endsWith(".png")
                || path.endsWith(".bmp")) {
            return true;
        }
        //wms layer
        if (path.startsWith("http") && (path.indexOf("format=image") >= 0)) {
            return true;
        }

        return false;
    }


    /**
     * _more_
     *
     * @param toks _more_
     *
     * @return _more_
     */
    public static List<Integer> toInt(List<String> toks) {
        List<Integer> ints = new ArrayList<Integer>();
        for (String tok : toks) {
            ints.add(new Integer(tok));
        }

        return ints;
    }

    /**
     * _more_
     *
     * @param files _more_
     * @param ascending _more_
     *
     * @return _more_
     */
    public static File[] sortFilesOnSize(File[] files,
                                         final boolean ascending) {

        ArrayList<IOUtil.FileWrapper> sorted =
            (ArrayList<IOUtil.FileWrapper>) new ArrayList();

        for (int i = 0; i < files.length; i++) {
            sorted.add(new IOUtil.FileWrapper(files[i], ascending));
        }

        Collections.sort(sorted, new FileSizeCompare(ascending));


        for (int i = 0; i < files.length; i++) {
            files[i] = sorted.get(i).getFile();
        }

        return files;
    }


    /**
     * Class description
     *
     *
     * @version        $version$, Sat, Apr 12, '14
     * @author         Enter your name here...
     */
    private static class FileSizeCompare implements Comparator<IOUtil
        .FileWrapper> {

        /** _more_ */
        private boolean ascending;

        /**
         * _more_
         *
         * @param ascending _more_
         */
        public FileSizeCompare(boolean ascending) {
            this.ascending = ascending;
        }

        /**
         * _more_
         *
         * @param o1 _more_
         * @param o2 _more_
         *
         * @return _more_
         */
        public int compare(IOUtil.FileWrapper o1, IOUtil.FileWrapper o2) {
            int result;
            if (o1.length() < o2.length()) {
                result = -1;
            } else if (o1.length() > o2.length()) {
                result = 1;
            } else {
                result = o1.getFile().compareTo(o1.getFile());
            }
            if ( !ascending || (result == 0)) {
                return result;
            }

            return -result;

        }

    }


    /**
     * _more_
     *
     * @param filePatternString _more_
     * @param patternNames _more_
     *
     * @return _more_
     */
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



    /**
     * This method is taken from Unidatas ucar.unidata.util.Misc method.
     * I moved it here to not have it use the parseNumber because that would use
     * a DecimalFormat which was picking up the Locale
     *
     * Decodes a string representation of a latitude or longitude and
     * returns a double version (in degrees).  Acceptible formats are:
     * <pre>
     * +/-  ddd:mm, ddd:mm:, ddd:mm:ss, ddd::ss, ddd.fffff ===>   [+/-] ddd.fffff
     * +/-  ddd, ddd:, ddd::                               ===>   [+/-] ddd
     * +/-  :mm, :mm:, :mm:ss, ::ss, .fffff                ===>   [+/-] .fffff
     * +/-  :, ::                                          ===>       0.0
     * Any of the above with N,S,E,W appended
     * </pre>
     *
     * @param latlon  string representation of lat or lon
     * @return the decoded value in degrees
     */
    public static double decodeLatLon(String latlon) {
        // first check to see if there is a N,S,E,or W on this
        latlon = latlon.trim();
        int    dirIndex    = -1;
        int    southOrWest = 1;
        double value       = Double.NaN;
        if (latlon.indexOf("S") > 0) {
            southOrWest = -1;
            dirIndex    = latlon.indexOf("S");
        } else if (latlon.indexOf("W") > 0) {
            southOrWest = -1;
            dirIndex    = latlon.indexOf("W");
        } else if (latlon.indexOf("N") > 0) {
            dirIndex = latlon.indexOf("N");
        } else if (latlon.endsWith("E")) {  // account for 9E-3, 9E-3E, etc
            dirIndex = latlon.lastIndexOf("E");
        }

        if (dirIndex > 0) {
            latlon = latlon.substring(0, dirIndex).trim();
        }

        // now see if this is a negative value
        if (latlon.indexOf("-") == 0) {
            southOrWest *= -1;
            latlon      = latlon.substring(latlon.indexOf("-") + 1).trim();
        }

        if (latlon.indexOf(":") >= 0) {  //have something like DD:MM:SS, DD::, DD:MM:, etc
            int    firstIdx = latlon.indexOf(":");
            String hours    = latlon.substring(0, firstIdx);
            String minutes  = latlon.substring(firstIdx + 1);
            String seconds  = "";
            if (minutes.indexOf(":") >= 0) {
                firstIdx = minutes.indexOf(":");
                String temp = minutes.substring(0, firstIdx);
                seconds = minutes.substring(firstIdx + 1);
                minutes = temp;
            }
            try {

                value = (hours.equals("") == true)
                        ? 0
                        : Double.parseDouble(hours);
                if ( !minutes.equals("")) {
                    value += Double.parseDouble(minutes) / 60.;
                }
                if ( !seconds.equals("")) {
                    value += Double.parseDouble(seconds) / 3600.;
                }
            } catch (NumberFormatException nfe) {
                value = Double.NaN;
            }
        } else {  //have something like DD.ddd
            try {
                value = parseNumber(latlon);
            } catch (NumberFormatException nfe) {
                value = Double.NaN;
            }
        }

        return value * southOrWest;
    }

    /**
     * _more_
     *
     * @param s _more_
     * @param points _more_
     *
     * @return _more_
     */
    public static List<double[]> parsePointString(String s,
            List<double[]> points) {
        if (s == null) {
            return points;
        }
        for (String pair : StringUtil.split(s, ";", true, true)) {
            List<String> toks = StringUtil.splitUpTo(pair, ",", 2);
            if (toks.size() != 2) {
                continue;
            }
            double lat = Utils.decodeLatLon(toks.get(0));
            double lon = Utils.decodeLatLon(toks.get(1));
            points.add(new double[] { lat, lon });
        }

        return points;
    }

    /**
     * _more_
     *
     * @param list _more_
     * @param index _more_
     *
     * @return _more_
     */
    public static Object safeGet(List list, int index) {
        if ((list == null) || (index >= list.size())) {
            return null;
        }

        return list.get(index);
    }

    /**
     * _more_
     *
     * @param filename _more_
     *
     * @return _more_
     *
     * @throws IOException _more_
     */
    public static boolean isCompressed(String filename) throws IOException {
        filename = filename.toLowerCase();

        return filename.endsWith(".gz") || filename.endsWith(".zip");
    }



    /**
     *
     * @param s _more_
     *
     * @return _more_
     */
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


    /**
     * _more_
     *
     * @param s _more_
     * @param base64 _more_
     *
     * @return _more_
     */
    public static String obfuscate(String s, boolean base64) {
        s = rot13(s);
        if (base64) {
            return encodeBase64(s);
        }

        return s;
    }

    /**
     * _more_
     *
     * @param s _more_
     * @param base64 _more_
     *
     * @return _more_
     */
    public static String unobfuscate(String s, boolean base64) {
        if (base64) {
            return rot13(new String(decodeBase64(s)));
        }

        return rot13(s);
    }


    /** _more_ */
    private static final java.util.Base64.Encoder base64Encoder =
        java.util.Base64.getEncoder();

    /** _more_ */
    private static final java.util.Base64.Decoder base64Decoder =
        java.util.Base64.getDecoder();

    /** _more_ */
    private static final java.util.Base64.Decoder base64MimeDecoder =
        java.util.Base64.getMimeDecoder();


    /**
     * _more_
     *
     * @param b _more_
     *
     * @return _more_
     */
    public static String encodeBase64Bytes(byte[] b) {
        return new String(base64Encoder.encode(b));
    }


    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public static String encodeBase64(String s) {
        try {
            return encodeBase64Bytes(s.getBytes("UTF-8"));
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
            byte[] b = s.getBytes("UTF-8");

            return base64Decoder.decode(b);
        } catch (Exception exc) {
            //In case it was a mime encoded b64
            try {
                return base64MimeDecoder.decode(s.getBytes());
            } catch (Exception exc2) {
                /*
                //Awful hack to not have to deal with why Don't synthid's are barfing
                try {
                return javax.xml.bind.DatatypeConverter.parseBase64Binary(
                s);
                } catch (Exception exc3) {
                */
                throw new RuntimeException("Failed to decode base64 string:"
                                           + s + "  Error:"
                //+ exc3);
                + exc2);
                //}
            }
        }
    }



    /**
     * _more_
     *
     * @param text _more_
     * @param pattern _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static List<String> extractPatterns(String text, String pattern)
            throws Exception {
        List<String> values = new ArrayList<String>();
        Pattern      p      = Pattern.compile(pattern);
        Matcher      m      = p.matcher(text);
        while (m.find()) {
            values.add(m.group(1));
        }

        return values;
    }


    /**
     * _more_
     *
     * @param o _more_
     *
     * @return _more_
     */
    public static String toString(Object o) {
        if (o == null) {
            return "";
        }

        return o.toString();
    }




    /**
     * _more_
     *
     * @param props _more_
     * @param key _more_
     *
     * @return _more_
     */
    public static String getProperty(Hashtable props, String key) {
        return getProperty(props, key, (String) null);
    }


    /**
     * _more_
     *
     * @param props _more_
     * @param key _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public static String getProperty(Hashtable props, String key,
                                     String dflt) {
        String s = (String) props.get(key);
        if (s == null) {
            s = (String) props.get(key.toLowerCase());
        }
        if (s == null) {
            return dflt;
        }

        return s;
    }



    /**
     * _more_
     *
     * @param props _more_
     * @param key _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public static boolean getProperty(Hashtable props, String key,
                                      boolean dflt) {
        String s = Utils.getProperty(props, key, (String) null);
        if (s == null) {
            return dflt;
        }

        return new Boolean(s).booleanValue();
    }

    /**
     * _more_
     *
     * @param props _more_
     * @param key _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public static int getProperty(Hashtable props, String key, int dflt) {
        String s = Utils.getProperty(props, key, (String) null);
        if (s == null) {
            return dflt;
        }

        return new Integer(s).intValue();
    }


    /**
     * _more_
     *
     * @param l _more_
     *
     * @return _more_
     */
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
                    return new Integer(args.get(i)).intValue();
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



    /**
     * _more_
     *
     * @param what _more_
     * @param args _more_
     */
    public static void printTimes(String what, long... args) {
        System.out.print(what + " ");
        for (int i = 1; i < args.length; i++) {
            System.out.print(" " + (args[i] - args[i - 1]));
        }
        System.out.println("  total: " + (args[args.length - 1] - args[0]));
    }

    /**
     * _more_
     *
     * @param f _more_
     *
     * @return _more_
     */
    public static String normalizeTemplateUrl(String f) {
        StringBuilder     s           = new StringBuilder();
        Date              now         = new Date();
        GregorianCalendar g           = new GregorianCalendar();
        Date              currentDate = now;
        TimeZone          tz          = TimeZone.getTimeZone("UTC");
        g.setTime(currentDate);

        //Old macros
        if (f.indexOf("${now") >= 0) {
            f = f.replace("${now.year}", g.get(g.YEAR) + "");
            f = f.replace("${now.month}",
                          StringUtil.padZero(g.get(g.MONTH), 2));
            f = f.replace("${now.day}",
                          StringUtil.padZero(g.get(g.DAY_OF_MONTH), 2));
            f = f.replace("${now.hour}",
                          StringUtil.padZero(g.get(g.HOUR_OF_DAY), 2));
            f = f.replace("${now.minute}",
                          StringUtil.padZero(g.get(g.MINUTE), 2));
        }

        List<String> toks = StringUtil.splitMacros(f);
        for (int i = 0; i < toks.size(); i++) {
            String t = toks.get(i);
            //System.err.println("T:" + t);
            if (i / 2.0 == (int) i / 2) {
                s.append(t);

                //System.err.println("appending");
                continue;
            }
            String d = null;
            if (t.startsWith("z:")) {
                tz = TimeZone.getTimeZone(t.substring(2));

                continue;
            }

            if (t.startsWith("date:")) {
                t = t.substring(5).trim();
                Date newDate = null;
                if (t.equals("now")) {
                    newDate = now;
                } else {
                    newDate = DateUtil.getRelativeDate(now, t);
                }
                if (newDate != null) {
                    currentDate = newDate;
                    g.setTime(currentDate);
                }

                continue;
            }
            if (t.startsWith("format:")) {
                t = t.substring("format:".length()).trim();
                SimpleDateFormat sdf = makeDateFormat(t);
                sdf.setTimeZone(tz);
                s.append(sdf.format(currentDate));

                continue;
            }



            if (t.startsWith("year")) {
                d = g.get(g.YEAR) + "";
            } else if (t.startsWith("month")) {
                d = StringUtil.padZero(g.get(g.MONTH), 2);
            } else if (t.startsWith("day")) {
                d = StringUtil.padZero(g.get(g.DAY_OF_MONTH), 2);
            } else if (t.startsWith("hour")) {
                d = StringUtil.padZero(g.get(g.HOUR_OF_DAY), 2);
            } else if (t.startsWith("minute")) {
                d = StringUtil.padZero(g.get(g.MINUTE), 2);
            } else {
                //Put it back
                d = "${" + t + "}";
                //                System.err.println("Unknown macro:" + t);
                //                continue;
            }
            s.append(d);
        }

        return s.toString();
    }


    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public static String upperCaseFirst(String s) {
        StringBuilder sb = new StringBuilder();
        for (String tok : StringUtil.split(s, " ", true, true)) {
            sb.append(tok.substring(0, 1).toUpperCase()
                      + tok.substring(1).toLowerCase());
            sb.append(" ");
        }

        return sb.toString().trim();
    }


    /**
     * _more_
     *
     * @param label _more_
     *
     * @return _more_
     */
    public static String makeID(String label) {
        label = label.trim().toLowerCase().replaceAll(" ",
                "_").replaceAll("\\.", "_");

        return label;
    }


    /**
     * _more_
     *
     * @param label _more_
     *
     * @return _more_
     */
    public static String makeLabel(String label) {
        label = label.replaceAll("_", " ");
        StringBuilder tmpSB             = new StringBuilder();
        StringBuilder sb                = new StringBuilder();
        boolean       lastCharUpperCase = false;
        for (int i = 0; i < label.length(); i++) {
            char    c           = label.charAt(i);
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

        label = sb.toString();
        label = label.replaceAll("__+", "_");


        for (String tok : StringUtil.split(label, " ", true, true)) {
            tok = tok.substring(0, 1).toUpperCase()
                  + tok.substring(1, tok.length()).toLowerCase();
            tmpSB.append(tok);
            tmpSB.append(" ");
        }
        label = tmpSB.toString().trim();

        return label;

    }

    /**
     * _more_
     *
     * @param s _more_
     * @param dflt _more_
     *
     * @return _more_
     */
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
            int v = new Integer(s).intValue();
            if (isPercent) {
                v = -v;
            }

            return v;
        } catch (Exception exc) {
            System.err.println("Utils.getDimension error:" + s + " " + exc);

            return dflt;
        }
    }

    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
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

    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     *
     */
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

    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public static int hhmmssToSeconds(String s) {
        List<String> toks    = StringUtil.split(s, ":", true, true);
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
     * Class description
     *
     *
     * @version        $version$, Thu, May 19, '16
     * @author         Enter your name here...
     */
    public static class DoubleTupleComparator implements Comparator {

        /** _more_ */
        int index;

        /**
         * _more_
         *
         * @param index _more_
         */
        public DoubleTupleComparator(int index) {
            this.index = index;
        }

        /**
         * _more_
         *
         * @param o1 _more_
         * @param o2 _more_
         *
         * @return _more_
         */
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

        /**
         * _more_
         *
         * @param obj _more_
         *
         * @return _more_
         */
        public boolean equals(Object obj) {
            return obj == this;
        }
    }

    ;



    /**
     * Class description
     *
     *
     * @version        $version$, Fri, May 27, '16
     * @author         Enter your name here...
     */
    public static class FloatTupleComparator implements Comparator {

        /** _more_ */
        int index;

        /**
         * _more_
         *
         * @param index _more_
         */
        public FloatTupleComparator(int index) {
            this.index = index;
        }

        /**
         * _more_
         *
         * @param o1 _more_
         * @param o2 _more_
         *
         * @return _more_
         */
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

        /**
         * _more_
         *
         * @param obj _more_
         *
         * @return _more_
         */
        public boolean equals(Object obj) {
            return obj == this;
        }
    }

    ;



    /**
     * Class description
     *
     *
     * @version        $version$, Thu, May 19, '16
     * @author         Enter your name here...
     */
    public static class IntegerTupleComparator implements Comparator {

        /** _more_ */
        int index;

        /**
         * _more_
         *
         * @param index _more_
         */
        public IntegerTupleComparator(int index) {
            this.index = index;
        }

        /**
         * _more_
         *
         * @param o1 _more_
         * @param o2 _more_
         *
         * @return _more_
         */
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

        /**
         * _more_
         *
         * @param obj _more_
         *
         * @return _more_
         */
        public boolean equals(Object obj) {
            return obj == this;
        }
    }

    ;





    /**
     * _more_
     *
     * @return _more_
     */
    public static String getGuid() {
        return UUID.randomUUID().toString();
    }

    /**
     * _more_
     *
     * @param storedPassword _more_
     * @param givenPassword _more_
     *
     * @return _more_
     */
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

    /**
     * _more_
     *
     * @param c _more_
     * @param sb _more_
     * @param lines _more_
     *
     * @return _more_
     */
    private static StringBuilder append(Object c, StringBuilder sb,
                                        List<StringBuilder> lines) {
        if (sb == null) {
            sb = new StringBuilder();
            lines.add(sb);
        }
        sb.append(c);

        return sb;
    }

    /** _more_ */
    public static final String MULTILINE_END = "_multilineend_";

    /**
     * _more_
     *
     * @param commandString _more_
     *
     * @return _more_
     */
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


    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public static List<String> parseCommandLine(String s) {

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
            throw new IllegalArgumentException("Unclosed quote:" + s);
        }
        if (inBracket) {
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

    /** _more_ */
    public static final String ENTRY_ID_REGEX =
        "[a-f|0-9]{8}-([a-f|0-9]{4}-){3}[a-f|0-9]{12}_";

    /**
     * This will prune out any leading &lt;unique id&gt;_file_&lt;actual file name&gt;
     *
     * @param fileName the filename
     *
     * @return  the pruned filename
     */
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



    /**
     * _more_
     *
     * @param pts _more_
     *
     * @return _more_
     */
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

    /** _more_ */
    private static List<SimpleDateFormat> dateFormats;

    /**
     * _more_
     *
     * @param dttm _more_
     *
     * @return _more_
     */
    public static Date parseDate(String dttm) {
        if (dateFormats == null) {
            String[] formats = {
                "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm:ss z",
                "yyyy-MM-dd'T'HH:mm", "yyyy-MM-dd HH:mm z",
                "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd",
                "yyyyMMddHHmmss", "yyyyMMddHHmm", "yyyyMMddHH", "yyyyMMdd"
            };

            dateFormats = new ArrayList<SimpleDateFormat>();
            for (int i = 0; i < formats.length; i++) {
                SimpleDateFormat dateFormat =
                    new java.text.SimpleDateFormat(formats[i]);
                dateFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
                dateFormats.add(dateFormat);
            }

        }
        int           cnt = 0;
        ParsePosition pp  = new ParsePosition(0);
        for (SimpleDateFormat dateFormat : dateFormats) {
            Date date = dateFormat.parse(dttm, pp);
            if (date != null) {
                return date;
            }
        }

        return null;
    }




    /**
     * _more_
     *
     * @param html _more_
     *
     * @return _more_
     */
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




    /**
     * _more_
     *
     * @param args _more_
     *
     * @return _more_
     */
    public static HashSet makeHashSet(Object... args) {
        HashSet h = new HashSet();
        for (Object arg : args) {
            h.add(arg);
        }

        return h;
    }

    /**
     * _more_
     *
     * @param args _more_
     *
     * @return _more_
     */
    public static HashSet makeHashSet(List args) {
        HashSet h = new HashSet();
        for (Object arg : args) {
            h.add(arg);
        }

        return h;
    }

    /**
     * _more_
     *
     * @param args _more_
     *
     * @return _more_
     */
    public static Hashtable makeHashtable(Object... args) {
        Hashtable h = new Hashtable();
        for (int i = 0; i < args.length; i += 2) {
            h.put(args[i], args[i + 1]);
        }

        return h;
    }


    /**
     * _more_
     *
     * @param list _more_
     *
     * @return _more_
     */
    public static Hashtable makeHashtable(List list) {
        Hashtable h = new Hashtable();
        for (int i = 0; i < list.size(); i += 2) {
            h.put(list.get(i), list.get(i + 1));
        }

        return h;
    }


    /**
     * _more_
     *
     * @param args _more_
     *
     * @return _more_
     */
    public static List makeList(Object... args) {
        List h = new ArrayList();
        for (Object arg : args) {
            h.add(arg);
        }

        return h;
    }

    /** _more_ */
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

    /**
     * _more_
     *
     * @param value _more_
     * @param dflt _more_
     *
     * @return _more_
     */
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



    /**
     * _more_
     *
     * @param x _more_
     *
     * @return _more_
     */
    public static float square(float x) {
        return (float) Math.pow(x, 2);
    }

    /**
     * _more_
     *
     * @param vx _more_
     * @param vy _more_
     * @param wx _more_
     * @param wy _more_
     *
     * @return _more_
     */
    public static float distanceBetweenPoints(float vx, float vy, float wx,
            float wy) {
        return square(vx - wx) + square(vy - wy);
    }

    /**
     * _more_
     *
     * @param px _more_
     * @param py _more_
     * @param vx _more_
     * @param vy _more_
     * @param wx _more_
     * @param wy _more_
     *
     * @return _more_
     */
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

    /**
     * _more_
     *
     * @param px _more_
     * @param py _more_
     * @param vx _more_
     * @param vy _more_
     * @param wx _more_
     * @param wy _more_
     *
     * @return _more_
     */
    public static float perpendicularDistance(float px, float py, float vx,
            float vy, float wx, float wy) {
        return (float) Math.sqrt(distanceToSegmentSquared(px, py, vx, vy, wx,
                wy));
    }

    /**
     * _more_
     *
     * @param list _more_
     * @param s _more_
     * @param e _more_
     * @param index _more_
     *
     * @return _more_
     */
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
            value = value.replace("e", "E");
            synchronized (formatter) {
                return formatter.parse(value).doubleValue();
            }
        } catch (ParseException pe) {
            throw new NumberFormatException(pe.getMessage());
        }
    }

    /**
     * _more_
     *
     * @param v _more_
     * @param min _more_
     * @param max _more_
     *
     * @return _more_
     */
    public static boolean between(double v, double min, double max) {
        return (v >= min) && (v <= max);
    }


    /** _more_ */
    public static final String MISSING = "missing";

    /** NaN string */
    public static final String NaN = "NaN";


    /**
     * _more_
     *
     * @param image _more_
     * @param top _more_
     * @param bottom _more_
     * @param left _more_
     * @param right _more_
     *
     * @return _more_
     */
    public static BufferedImage crop(BufferedImage image, int top, int left,
                                     int bottom, int right) {
        int imageWidth  = image.getWidth(null);
        int imageHeight = image.getHeight(null);
        int w           = imageWidth - right - left;
        int h           = imageHeight - top - bottom;

        //        System.err.println("iw:" + imageWidth +" w:"  + w + " " + left +" " + right);
        //        System.err.println("ih:" + imageHeight +" h:"  + h + " " + top +" " + bottom);
        return image.getSubimage(left, top, w, h);
    }

    /**
     * Class description
     *
     *
     * @version        $version$, Mon, Jan 7, '19
     * @author         Enter your name here...
     */
    public static class ObjectSorter implements Comparable {

        /** _more_ */
        boolean ascending = true;

        /** _more_ */
        String svalue;

        /** _more_ */
        double value;

        /** _more_ */
        Object object;

        /**
         * _more_
         *
         * @param value _more_
         * @param object _more_
         * @param ascending _more_
         */
        public ObjectSorter(Object object, double value, boolean ascending) {
            this.value     = value;
            this.object    = object;
            this.ascending = ascending;
        }

        /**
         * _more_
         *
         * @param object _more_
         * @param value _more_
         * @param ascending _more_
         */
        public ObjectSorter(Object object, int value, boolean ascending) {
            this.value     = value;
            this.object    = object;
            this.ascending = ascending;
        }

        /**
         * _more_
         *
         * @param value _more_
         * @param object _more_
         * @param ascending _more_
         */
        public ObjectSorter(Object object, String value, boolean ascending) {
            this.svalue    = value;
            this.object    = object;
            this.ascending = ascending;
        }

        /**
         * _more_
         *
         * @param o _more_
         *
         * @return _more_
         */
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

        /**
         * _more_
         *
         * @return _more_
         */
        public Object getObject() {
            return object;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public double getValue() {
            return value;
        }
    }


    /**
     * _more_
     *
     * @param s _more_
     * @param start _more_
     * @param end _more_
     *
     * @return _more_
     */
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


    /**
     * _more_
     *
     * @param s _more_
     * @param start _more_
     * @param end _more_
     *
     * @return _more_
     */
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
        s = s.replaceAll("\"", "&quot;");

        //        s = HtmlUtils.urlEncode(s);
        //       s = s.replace("+", " ");
        return s;
    }

    /**
     * _more_
     *
     * @param vs _more_
     *
     * @return _more_
     */
    public static double getAverage(List<Double> vs) {
        double total = 0;
        for (double d : vs) {
            total += d;
        }

        return ((vs.size() > 0)
                ? total / vs.size()
                : 0);
    }

    /**
     * _more_
     *
     * @param text _more_
     *
     * @return _more_
     */
    public static String unquote(String text) {
        if (text.startsWith("\"") && text.endsWith("\"")) {
            text = text.substring(1);
            text = text.substring(0, text.length() - 1);
        }

        return text;
    }

    /**
     * Class description
     *
     *
     * @param <TYPE>
     *
     * @version        $version$, Sun, Oct 20, '19
     * @author         Enter your name here...
     */
    public static class TypedList<TYPE> extends ArrayList {

        /**
         * _more_
         *
         * @param args _more_
         */
        public TypedList(TYPE... args) {
            for (TYPE arg : args) {
                this.add(arg);
            }
        }
    }


    /** _more_ */
    public static int debugCnt = 0;

    /**
     * _more_
     *
     * @param cnt _more_
     * @param s _more_
     */
    public static void debugCount(int cnt, String s) {
        if (debugCnt++ < cnt) {
            System.err.println(s);
        }
    }


    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
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


    /**
     * _more_
     *
     * @param parent _more_
     * @param path _more_
     *
     * @return _more_
     */
    public static List findDescendantsFromPath(Element parent, String path) {
        List results = new ArrayList();
        List tags    = StringUtil.split(path, ".");
        //In case the path starts with the root node
        if (parent.getTagName().equals(tags.get(0))) {
            tags.remove(0);
        }
        findDescendantsFromPath(parent, tags, 0, results, "\t");

        return results;
    }

    /**
     * _more_
     *
     * @param parent _more_
     * @param tags _more_
     * @param tagIdx _more_
     * @param results _more_
     * @param tab _more_
     */
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

    /**
     * _more_
     *
     * @param l _more_
     * @param delim _more_
     * @param inverse _more_
     *
     * @return _more_
     */
    public static String join(List l, String delim, boolean inverse) {
        StringBuilder sb = new StringBuilder();
        if (inverse) {
            for (int i = l.size() - 1; i >= 0; i--) {
                sb.append(l.get(i));
                sb.append(delim);
            }
        } else {
            for (int i = 0; i < l.size(); i++) {
                sb.append(l.get(i));
                sb.append(delim);
            }
        }

        return sb.toString();

    }


    /**
     * _more_
     *
     * @param listToSort _more_
     *
     * @return _more_
     */
    public static List sort(Collection listToSort) {
        Object[] array = listToSort.toArray();
        Arrays.sort(array);

        return Arrays.asList(array);
    }

    /**
     * _more_
     *
     * @param o1 _more_
     * @param o2 _more_
     *
     * @return _more_
     */
    public static boolean equals(Object o1, Object o2) {
        if ((o1 != null) && (o2 != null)) {
            return o1.equals(o2);
        }

        return ((o1 == null) && (o2 == null));
    }

    /**
     * _more_
     *
     * @param s _more_
     * @param baseIdx _more_
     * @param p _more_
     *
     * @return _more_
     */
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

    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
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
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        String s =
            "hello {{there template=\\\"foo bar {{ }} \"   }} I am fine";
        String p   = "}}";
        int    idx = findNext(s, 0, p);
        if (idx < 0) {
            System.err.println("failed");
        } else {
            System.err.println("idx:" + idx + " s:" + s.substring(idx));
        }
        System.exit(0);
    }


}
