/*
* Copyright (c) 2008-2018 Geode Systems LLC
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


import com.drew.imaging.jpeg.*;
import com.drew.lang.*;
import com.drew.metadata.*;
import com.drew.metadata.exif.*;
import com.drew.metadata.iptc.IptcDirectory;


import org.apache.commons.lang.text.StrTokenizer;
import org.apache.commons.net.ftp.*;

import org.w3c.dom.*;


import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.awt.Image;

import java.awt.Toolkit;
import java.awt.image.*;

import java.io.*;

import java.io.*;

import java.lang.reflect.Constructor;

import java.net.*;

import java.text.DecimalFormat;

import java.text.ParsePosition;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Calendar;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
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
import javax.imageio.stream.ImageOutputStream;


/**
 * A collection of utilities
 *
 * @author Jeff McWhirter
 */

public class Utils {

    /** _more_ */
    private static DecimalFormat[] FORMATS = {
        new DecimalFormat("#0"), new DecimalFormat("#0.0"),
        new DecimalFormat("#0.00"), new DecimalFormat("#0.000"),
        new DecimalFormat("#0.0000"), new DecimalFormat("#0.00000"),
    };



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
     * @param sb _more_
     * @param s _more_
     *
     * @return _more_
     */
    public static Appendable append(Appendable sb, String s) {
        try {
            sb.append(s);

            return sb;
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
     * @param columnDelimiter _more_
     *
     * @return _more_
     */
    public static List<String> tokenizeColumns(String line,
            String columnDelimiter) {
        //        System.err.println("line:" + line);
        List<String> toks      = new ArrayList<String>();
        StrTokenizer tokenizer = StrTokenizer.getCSVInstance(line);
        //        StrTokenizer tokenizer = new StrTokenizer(line, columnDelimiter);
        if ( !columnDelimiter.equals(",")) {
            tokenizer.setDelimiterChar(columnDelimiter.charAt(0));
        }
        //        tokenizer.setQuoteChar('"');
        while (tokenizer.hasNext()) {
            String tok = tokenizer.nextToken();
            //            System.err.println("tok:" + tok);
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
    public static boolean stringDefined(String s) {
        if ((s == null) || (s.trim().length() == 0)) {
            return false;
        }

        return true;
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


        for (int i = 0; i < constructors.length; i++) {
            Class[] formals = constructors[i].getParameterTypes();
            for (int j = 0; j < formals.length; j++) {
                System.err.println("param " + j + "  " + formals[j].getName()
                                   + " " + paramTypes[j].getName());
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
            attrValue = XmlUtil.getGrandChildText(node, attrOrTag, dflt);
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
        keyList = (List<String>) Misc.sort(keyList);
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
    public static Hashtable<String, String> makeMap(String... args) {
        Hashtable<String, String> map = new Hashtable<String, String>();
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
     * _more_
     *
     * @param file _more_
     *
     * @return _more_
     */
    public static Image readImage(String file) {

        if (file == null) {
            return null;
        }
        try {
            InputStream is = IOUtil.getInputStream(file, Utils.class);
            if (is != null) {
                byte[] bytes = IOUtil.readBytes(is);
                Image  image = Toolkit.getDefaultToolkit().createImage(bytes);
                //                image = ImageUtils.waitOnImage(image);

                return image;
            }
            System.err.println("Could not read image:" + file);
        } catch (Exception exc) {
            System.err.println(exc + " getting image:  " + file);

            return null;
        }

        return null;
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
                value = Double.parseDouble(latlon);
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
     * _more_
     *
     * @param filename _more_
     * @param buffered _more_
     *
     * @return _more_
     *
     * @throws IOException _more_
     */
    public static InputStream doMakeInputStream(String filename,
            boolean buffered)
            throws IOException {
        int         size = 8000;
        InputStream is   = null;
        if (new File(filename).exists()) {
            is = new FileInputStream(filename);
        } else {
            //Try it as a url
            URL           url        = new URL(filename);
            URLConnection connection = null;
            try {
                connection = url.openConnection();
                is         = connection.getInputStream();
            } catch (Exception exc) {
                String msg = "An error has occurred";
                if ((connection != null)
                        && (connection instanceof HttpURLConnection)) {
                    HttpURLConnection huc = (HttpURLConnection) connection;
                    msg = "Response code: " + huc.getResponseCode() + " ";
                    try {
                        InputStream err = huc.getErrorStream();
                        msg += " Message: "
                               + new String(readBytes(err, 10000));
                    } catch (Exception ignoreIt) {}
                }

                throw new IOException(msg);
            }
        }

        if (filename.toLowerCase().endsWith(".gz")) {
            is = new GZIPInputStream(is);
        }

        if (filename.toLowerCase().endsWith(".zip")) {
            ZipEntry       ze  = null;
            ZipInputStream zin = new ZipInputStream(is);
            //Read into the zip stream to the first entry
            while ((ze = zin.getNextEntry()) != null) {
                if (ze.isDirectory()) {
                    continue;
                }

                break;
                /*                String path = ze.getName();
                                  if(path.toLowerCase().endsWith(".las")) {
                                  break;
                                  }
                */
            }
            is = zin;
        }


        if ( !buffered) {
            //            System.err.println("not buffered");
            //            return is;
            //            size = 8*3;
        }

        if (buffered) {
            size = 1000000;
        }

        //        System.err.println("buffer size:" + size);
        return new BufferedInputStream(is, size);
    }


    /**
     * _more_
     *
     * @param url _more_
     * @param os _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static boolean writeFile(URL url, OutputStream os)
            throws Exception {
        if (url.getProtocol().equals("ftp")) {
            FTPClient ftpClient = null;
            try {
                ftpClient = Utils.makeFTPClient(url);
                if (ftpClient == null) {
                    return false;
                }
                if (ftpClient.retrieveFile(url.getPath(), os)) {
                    return true;
                }

                return false;
            } finally {
                closeConnection(ftpClient);
            }
        } else {
            InputStream is = IOUtil.getInputStream(url.toString(),
                                 Utils.class);
            IOUtil.writeTo(is, os);

            return true;
        }
    }


    /**
     * _more_
     *
     * @param url _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static FTPClient makeFTPClient(URL url) throws Exception {
        FTPClient ftpClient = new FTPClient();
        ftpClient.connect(url.getHost());
        ftpClient.login("anonymous", "");
        int reply = ftpClient.getReplyCode();
        if ( !FTPReply.isPositiveCompletion(reply)) {
            ftpClient.disconnect();
            System.err.println("FTP server refused connection.");

            return null;
        }
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
        ftpClient.enterLocalPassiveMode();

        return ftpClient;
    }


    /**
     * _more_
     *
     * @param ftpClient _more_
     */
    public static void closeConnection(FTPClient ftpClient) {
        try {
            ftpClient.logout();
        } catch (Exception exc) {}
        try {
            ftpClient.disconnect();
        } catch (Exception exc) {}
    }

    /**
     *   public static void mungeCsv() throws Exception {
     *
     *   System.out.print("Street");
     *   for (int i = 0; i < 20; i++) {
     *       System.out.print(",");
     *       System.out.print("" + (2013 - i));
     *   }
     *   System.out.println("");
     *   String c = IOUtil.readContents(args[0], "");
     *   for (String line : StringUtil.split(c, "\n", true, true)) {
     *       //            System.err.println("LINE:" + line);
     *       List cols = new ArrayList();
     *       int  cnt  = 0;
     *       for (String s : Utils.tokenizeColumns(line, ",")) {
     *           cnt++;
     *           if (cnt <= 2) {
     *               continue;
     *           }
     *           if (cnt > 3) {
     *               if (cnt % 2 == 0) {
     *                   continue;
     *               }
     *           }
     *           if (cnt > 43) {
     *               break;
     *           }
     *           s = s.replace(",", "");
     *           cols.add(s.trim());
     *       }
     *       System.out.println(StringUtil.join(",", cols));
     *   }
     *
     *   if (true) {
     *       return;
     *   }
     * }
     *
     * @param s _more_
     *
     * @return _more_
     */


    /**
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
            return encodeBase64(s.getBytes());
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



    /**
     * _more_
     *
     * @param b _more_
     *
     * @return _more_
     */
    public static String encodeBase64(byte[] b) {
        return javax.xml.bind.DatatypeConverter.printBase64Binary(b);
    }


    /**
     *                                                                                                                             * Decode the given base64 String
     *                                                                                                                               * @param s Holds the base64 encoded bytes
     * @return The decoded bytes
     */
    public static byte[] decodeBase64(String s) {
        return javax.xml.bind.DatatypeConverter.parseBase64Binary(s);
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
     * @param dflt _more_
     *
     * @return _more_
     */
    public static double getProperty(Hashtable props, String key,
                                     double dflt) {
        String s = Misc.getProperty(props, key, (String) null);
        if (stringUndefined(s)) {
            return dflt;
        }

        return Double.parseDouble(s);
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
     * @param is _more_
     * @param maxSize _more_
     *
     * @return _more_
     *
     * @throws IOException _more_
     */
    public static byte[] readBytes(InputStream is, int maxSize)
            throws IOException {
        int    totalRead = 0;
        byte[] content   = new byte[100000];
        try {
            while (true) {
                int howMany = is.read(content, totalRead,
                                      content.length - totalRead);
                if (howMany < 0) {
                    break;
                }
                if (howMany == 0) {
                    continue;
                }
                totalRead += howMany;
                if (totalRead >= content.length) {
                    byte[] tmp       = content;
                    int    newLength = ((content.length < 25000000)
                                        ? content.length * 2
                                        : content.length + 5000000);
                    content = new byte[newLength];
                    System.arraycopy(tmp, 0, content, 0, totalRead);
                }
                if ((maxSize >= 0) && (totalRead >= maxSize)) {
                    break;
                }
            }
        } finally {
            IOUtil.close(is);
        }
        byte[] results = new byte[totalRead];
        System.arraycopy(content, 0, results, 0, totalRead);

        return results;
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
     * @param url _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static String readUrl(String url) throws Exception {
        URL           u          = new URL(url);
        URLConnection connection = new URL(url).openConnection();
        connection.setRequestProperty("User-Agent", "ramadda");
        connection.setRequestProperty("Host", u.getHost());
        InputStream is = connection.getInputStream();
        String      s  = IOUtil.readContents(is);
        IOUtil.close(is);

        return s;
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
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void xmain(String args[]) throws Exception {
        BufferedReader br = new BufferedReader(
                                new InputStreamReader(
                                    new FileInputStream("foo")));

        int cnt = 0;
        while (true) {
            cnt++;
            String line1 = br.readLine();
            if (line1 == null) {
                break;
            }
            String line2 = br.readLine();


            while (line2 != null) {
                if (line2.indexOf(",") >= 0) {
                    line1 = line1 + " " + line2;
                    line2 = br.readLine();
                } else {
                    break;
                }
            }
            if (line2 == null) {
                break;
            }

            List<String> toks  = StringUtil.split(line1, " ", true, true);
            double       north = Double.NaN,
                         south = Double.NaN,
                         east  = Double.NaN,
                         west  = Double.NaN;
            for (int i = 0; i < toks.size(); i += 2) {
                String tok = toks.get(i);
                //                System.out.println("TOK:" +tok);
                List<String> toks2 = StringUtil.split(tok, ",", true, true);
                if (toks2.size() != 3) {
                    continue;
                }
                double lon = Double.parseDouble(toks2.get(0));
                double lat = Double.parseDouble(toks2.get(1));
                north = (i == 0)
                        ? lat
                        : Math.max(north, lat);
                south = (i == 0)
                        ? lat
                        : Math.min(north, lat);
                west  = (i == 0)
                        ? lon
                        : Math.min(west, lon);
                east  = (i == 0)
                        ? lon
                        : Math.max(east, lon);
            }
            System.out.println(line2 + "," + north + "," + west + "," + south
                               + "," + east);
        }
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
     * @param url _more_
     * @param body _more_
     * @param args _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static String doPost(URL url, String body, String... args)
            throws Exception {
        return doHttpRequest("POST", url, body, args);
    }

    /**
     * _more_
     *
     * @param action _more_
     * @param url _more_
     * @param body _more_
     * @param args _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static String doHttpRequest(String action, URL url, String body,
                                       String... args)
            throws Exception {
        //        URL url = new URL(request); 
        HttpURLConnection connection =
            (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestMethod(action);
        connection.setRequestProperty("charset", "utf-8");
        for (int i = 0; i < args.length; i += 2) {
            //            System.err.println(args[i]+":" + args[i+1]);
            connection.setRequestProperty(args[i], args[i + 1]);
        }
        if (body != null) {
            connection.setRequestProperty("Content-Length",
                                          Integer.toString(body.length()));

            connection.getOutputStream().write(body.getBytes());
        }
        try {
            return readString(
                new BufferedReader(
                    new InputStreamReader(
                        connection.getInputStream(), "UTF-8")));
        } catch (Exception exc) {
            System.err.println("Utils: error doing http request:" + action
                               + "\nURL:" + url + "\nreturn code:"
                               + connection.getResponseCode() + "\nBody:"
                               + body);
            System.err.println(readError(connection));
            System.err.println(connection.getHeaderFields());

            throw exc;
            //            System.err.println(connection.getContent());
        }
    }


    /**
     * _more_
     *
     * @param conn _more_
     *
     * @return _more_
     */
    public static String readError(HttpURLConnection conn) {
        try {
            return readString(
                new BufferedReader(
                    new InputStreamReader(conn.getErrorStream(), "UTF-8")));

        } catch (Exception exc) {
            return "No error message";
        }
    }

    /**
     * _more_
     *
     * @param input _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static String readString(BufferedReader input) throws Exception {
        StringBuilder sb = new StringBuilder();
        String        line;
        while ((line = input.readLine()) != null) {
            sb.append(line);
            sb.append("\n");
        }

        return sb.toString();
    }




    /**
     * _more_
     *
     * @param url _more_
     * @param args _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static String doGet(URL url, String... args) throws Exception {
        HttpURLConnection connection =
            (HttpURLConnection) url.openConnection();
        //        connection.setDoOutput(true);
        //        connection.setDoInput(true);
        //        connection.setInstanceFollowRedirects(false);
        connection.setRequestMethod("GET");
        //        connection.setRequestProperty("charset", "utf-8");
        for (int i = 0; i < args.length; i += 2) {
            //System.err.println(args[i]+":" + args[i+1]);
            connection.setRequestProperty(args[i], args[i + 1]);
        }
        try {
            BufferedReader in = new BufferedReader(
                                    new InputStreamReader(
                                        connection.getInputStream(),
                                        "UTF-8"));

            StringBuilder sb = new StringBuilder();
            String        line;
            while ((line = in.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }

            return sb.toString();
        } catch (Exception exc) {
            System.err.println("Error:" + connection.getResponseCode());
            System.err.println(connection.getHeaderFields());

            throw exc;
            //            System.err.println(connection.getContent());
        }
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

        return encodeBase64(bytes);
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
     * @param s _more_
     *
     * @return _more_
     */
    public static List<String> parseCommandLine(String s) {
        List<String> args = new ArrayList<String>();
        s = s.trim();
        StringBuilder sb       = new StringBuilder();
        boolean       inQuote  = false;
        boolean       inEscape = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\') {
                if (inEscape) {
                    sb.append(c);
                    inEscape = false;
                } else {
                    inEscape = true;
                }

                continue;
            }
            if (c == '\"') {
                if (inEscape) {
                    sb.append(c);
                    inEscape = false;

                    continue;
                }
                if (inQuote) {
                    inQuote = false;
                    args.add(sb.toString());
                    sb = new StringBuilder();
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
        if (sb.length() > 0) {
            args.add(sb.toString());
        }

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

    private static List<SimpleDateFormat> dateFormats;

    public static Date parseDate(String dttm) {
        if(dateFormats==null) {
            String[] formats = {
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd HH:mm:ss z", "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd HH:mm", "yyyy-MM-dd", "yyyyMMddHHmmss",
                "yyyyMMddHHmm", "yyyyMMddHH", "yyyyMMdd"
            };

            dateFormats = new ArrayList<SimpleDateFormat>();
            for (int i = 0; i < formats.length; i++) {
                SimpleDateFormat dateFormat =
                    new java.text.SimpleDateFormat(formats[i]);
                dateFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
                dateFormats.add(dateFormat);
            }
        
        }
        int cnt = 0;
        ParsePosition pp = new ParsePosition(0);
        for(SimpleDateFormat dateFormat: dateFormats) {
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
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String args[]) throws Exception {
        String url = "https://api.census.gov/data/2015/acs5?get=NAME,B01003_001E,B01001_002E,B01001_026E,B01001A_001E,B01001B_001E,B01001I_001E,B01001D_001E,B25001_001E,B07013_002E,B07013_003E&for=county:*";
        doGet(new URL(url));
        //        parseDate(args[0]);
        //        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        //        sdf.parse("2017-12-28T19:11:57Z");
        //        System.err.println(parseCommandLine(args[0]));
    }






}
