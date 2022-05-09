/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util.text;

import org.apache.commons.codec.language.Soundex;

import org.json.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.PatternProps;
import org.ramadda.util.Utils;


import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.io.*;

import java.net.URL;
import java.security.MessageDigest;
import java.security.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;


import java.util.regex.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

import javax.script.*;


/**
 * Class description
 *
 *
 * @version        $version$, Fri, Jan 9, '15
 * @author         Jeff McWhirter
 */
@SuppressWarnings("unchecked")
public abstract class Converter extends Processor {


    /**
     *
     */
    public Converter() {}

    /**
     * @param col _more_
     */
    public Converter(String col) {
        super(col);
    }

    /**
     *
     * @param cols _more_
     */
    public Converter(List<String> cols) {
        super(cols);
    }



    /**
     * Class description
     *
     * @version        $version$, Fri, Jan 16, '15
     * @author         Enter your name here...
     */
    public static class ConverterGroup extends Converter {


        /**
         */
        public ConverterGroup() {}

    }


    /**
     * Class description
     *
     * @version        $version$, Fri, Jan 9, '15
     * @author         Jeff McWhirter
     */
    public static class ColumnSelector extends Converter {

        /**
         *
         * @param ctx _more_
         * @param cols _more_
         */
        public ColumnSelector(TextReader ctx, List<String> cols) {
            super(cols);
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            List<Integer> indices = getIndices(ctx);
            boolean       debug   = ctx.getDebug() && (rowCnt++ == 0);
            if (indices.size() == 0) {
                if (debug) {
                    ctx.printDebug("-columns", "No indices");
                }

                //                return row;
            }
            List<String> result = new ArrayList<String>();
            for (Integer idx : indices) {
                if (idx < row.size()) {
                    String s = row.getString(idx);
                    result.add(s);
                } else {
                    if (debug) {
                        ctx.printDebug("-columns", "Missing index:" + idx);
                    }
                }
            }

            return new Row(result);
        }
    }


    /**
     * Class description
     *
     *
     * @version        $version$, Wed, Apr 6, '22
     * @author         Enter your name here...
     */
    public static class Faker extends Converter {

        /**  */
        private String what;

        /**  */
        com.github.javafaker.Faker faker = new com.github.javafaker.Faker();

        /**  */
        private double v1 = Double.NaN;

        /**  */
        private double v2 = Double.NaN;

        /**  */
        private double v3 = Double.NaN;

        /**
         *
         * @param ctx _more_
         * @param what _more_
         * @param cols _more_
         */
        public Faker(TextReader ctx, String what, List<String> cols) {
            super(cols);
            List<String> toks = Utils.split(what, ":", true, true);
            this.what = toks.get(0);
            if (toks.size() > 1) {
                v1 = Double.parseDouble(toks.get(1));
            }
            if (toks.size() > 2) {
                v2 = Double.parseDouble(toks.get(2));
            }
            if (toks.size() > 3) {
                v3 = Double.parseDouble(toks.get(3));
            }
        }

        /**
         *  @return _more_
         */
        private boolean hasV1() {
            return !Double.isNaN(v1);
        }

        /**
         *  @return _more_
         */
        private boolean hasV2() {
            return !Double.isNaN(v2);
        }

        /**
         *  @return _more_
         */
        private boolean hasV3() {
            return !Double.isNaN(v3);
        }


        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            List<Integer> indices = getIndices(ctx);
            if (rowCnt++ == 0) {
                if (indices.size() == 0) {
                    row.add(what);
                }

                return row;
            }

            if (indices.size() == 0) {
                row.add(getFakerValue());
            } else {
                for (Integer idx : getIndices(ctx)) {
                    row.set(idx, getFakerValue());
                }
            }

            return row;
        }

        /**
         *  @return _more_
         */
        private String getFakerValue() {

            if (what.equals("boolean")) {
                return "" + faker.bool().bool();
            }
            if (what.equals("firstname")) {
                return faker.name().firstName();
            }
            if (what.equals("fullname")) {
                return faker.name().fullName();
            }
            if (what.equals("lastname")) {
                return faker.name().lastName();
            }
            if (what.equals("name")) {
                return faker.name().name();
            }
            if (what.equals("namewithmiddle")) {
                return faker.name().nameWithMiddle();
            }
            if (what.equals("prefix")) {
                return faker.name().prefix();
            }
            if (what.equals("suffix")) {
                return faker.name().suffix();
            }
            if (what.equals("title")) {
                return faker.name().title();
            }
            if (what.equals("username")) {
                return faker.name().username();
            }

            if (what.equals("address")) {
                return faker.address().streetAddress();
            }
            if (what.equals("city")) {
                return faker.address().city();
            }
            if (what.equals("country")) {
                return faker.address().country();
            }
            if (what.equals("state")) {
                return faker.address().state();
            }
            if (what.equals("stateabbr")) {
                return faker.address().stateAbbr();
            }
            if (what.equals("streetname")) {
                return faker.address().streetName();
            }
            if (what.equals("timezone")) {
                return faker.address().timeZone();
            }
            if (what.equals("zipcode")) {
                return faker.address().zipCode();
            }
            if (what.equals("latitude")) {
                return faker.address().latitude();
            }
            if (what.equals("longitude")) {
                return faker.address().longitude();
            }
            if (what.equals("countrycode")) {
                return faker.address().countryCode();
            }

            if (what.equals("asin")) {
                return faker.code().asin();
            }

            if (what.equals("ean13")) {
                return faker.code().ean13();
            }

            if (what.equals("ean8")) {
                return faker.code().ean8();
            }

            if (what.equals("gtin13")) {
                return faker.code().gtin13();
            }
            if (what.equals("gtin8")) {
                return faker.code().gtin8();
            }

            if (what.equals("imei")) {
                return faker.code().imei();
            }

            if (what.equals("isbn10")) {
                return faker.code().isbn10();
            }

            if (what.equals("isbn13")) {
                return faker.code().isbn13();
            }

            if (what.equals("isbngroup")) {
                return faker.code().isbnGroup();
            }

            if (what.equals("isbngs1")) {
                return faker.code().isbnGs1();
            }

            if (what.equals("isbnregistrant")) {
                return faker.code().isbnRegistrant();
            }

            if (what.equals("color")) {
                return faker.commerce().color();
            }

            if (what.equals("department")) {
                return faker.commerce().department();
            }

            if (what.equals("material")) {
                return faker.commerce().material();
            }

            if (what.equals("price")) {
                return faker.commerce().price();
            }

            if (what.equals("productname")) {
                return faker.commerce().productName();
            }

            if (what.equals("promotioncode")) {
                return faker.commerce().promotionCode();
            }

            if (what.equals("demonym")) {
                return faker.demographic().demonym();
            }

            if (what.equals("educationalattainment")) {
                return faker.demographic().educationalAttainment();
            }

            if (what.equals("maritalstatus")) {
                return faker.demographic().maritalStatus();
            }

            if (what.equals("race")) {
                return faker.demographic().race();
            }

            if (what.equals("sex")) {
                return faker.demographic().sex();
            }

            if (what.equals("bic")) {
                return faker.finance().bic();
            }

            if (what.equals("creditcard")) {
                return faker.finance().creditCard();
            }

            if (what.equals("iban")) {
                return faker.finance().iban();
            }

            if (what.equals("ssn")) {
                return faker.idNumber().ssnValid();
            }

            if (what.equals("digit")) {
                if (hasV1()) {
                    return "" + faker.number().digits((int) v1);
                }

                return "" + faker.number().digit();
            }

            if (what.equals("digits")) {
                return "" + faker.number().digits((int) v1);
            }

            if (what.equals("numberbetween")) {
                return "" + faker.number().numberBetween((long) v1,
                        (long) v2);
            }

            if (what.equals("randomdigit")) {
                return "" + faker.number().randomDigit();
            }

            if (what.equals("randomdigitnotzero")) {
                return "" + faker.number().randomDigitNotZero();
            }

            if (what.equals("randomdouble")) {
                return "" + faker.number().randomDouble((int) v1, (long) v2,
                        (long) v3);
            }

            if (what.equals("randomnumber")) {
                return "" + faker.number().randomNumber();
            }

            if (what.equals("cellphone")) {
                return faker.phoneNumber().cellPhone();
            }

            if (what.equals("phonenumber")) {
                return faker.phoneNumber().phoneNumber();
            }

            if (what.equals("diseasename")) {
                return faker.medical().diseaseName();
            }

            if (what.equals("hospitalname")) {
                return faker.medical().hospitalName();
            }

            if (what.equals("medicinename")) {
                return faker.medical().medicineName();
            }

            if (what.equals("symptoms")) {
                return faker.medical().symptoms();
            }

            throw new IllegalArgumentException(
                "Unknown anonymization:" + what + " needs to be one of:"
                + "firstname|fullname|lastname|name|namewithmiddle|prefix|suffix|title|username|address|city|country|state|stateabbr|streetname|timezone|zipcode|latitude|longitude|countrycode|boolean|asin|ean13|ean8|gtin13|gtin8|imei|isbn10|isbn13|isbngroup|isbngs1|isbnregistrant|color|department|material|price|productname|promotioncode|demonym|educationalattainment|maritalstatus|race|sex|bic|creditcard|iban|ssn|digit|digits:number_of_digits|numberbetween:first:last|randomdigit|randomdigitnotzero|randomDouble:maxNumberOfDecimals:min:max|randomnumber|cellphone|phonenumber|diseasename|hospitalname|medicinename|symptoms");
        }
    }



    /**
     * Class description
     *
     * @version        $version$, Thu, Nov 4, '21
     * @author         Enter your name here...
     */
    public static class ColumnFirst extends Converter {

        /**  */
        private HashSet<Integer> firstSeen;


        /**
         *
         * @param ctx _more_
         * @param cols _more_
         */
        public ColumnFirst(TextReader ctx, List<String> cols) {
            super(cols);
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            List<Integer> indices = getIndices(ctx);
            if (indices.size() == 0) {
                return row;
            }
            List<String> result = new ArrayList<String>();
            if (firstSeen == null) {
                firstSeen = new HashSet<Integer>();
                for (Integer idx : indices) {
                    firstSeen.add(idx);
                }
            }
            for (Integer idx : indices) {
                String s = row.getString(idx);
                result.add(s);
            }
            for (int i = 0; i < row.size(); i++) {
                if ( !firstSeen.contains(i)) {
                    result.add(row.getString(i));
                }
            }

            return new Row(result);
        }
    }


    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Nov 4, '21
     * @author         Enter your name here...
     */
    public static class ColumnsBefore extends Converter {

        /**  */
        private HashSet<Integer> set;

        /**  */
        private String col;

        /**  */
        private int colIdx = -1;

        /**
         *
         * @param ctx _more_
         * @param col _more_
         * @param cols _more_
         */
        public ColumnsBefore(TextReader ctx, String col, List<String> cols) {
            super(cols);
            this.col = col;
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (colIdx == -1) {
                colIdx = getColumnIndex(ctx, col);
            }
            List<Integer> indices = getIndices(ctx);
            if (set == null) {
                set = new HashSet<Integer>();
                for (int i : indices) {
                    set.add(i);
                }
            }
            if (indices.size() == 0) {
                return row;
            }

            List values  = row.getValues();
            List newList = new ArrayList();
            for (int i = 0; i < colIdx; i++) {
                if ( !set.contains(i)) {
                    newList.add(values.get(i));
                }
            }
            for (int i : indices) {
                newList.add(values.get(i));
            }
            for (int i = colIdx; i < values.size(); i++) {
                if (set.contains(i)) {
                    continue;
                }
                newList.add(values.get(i));
            }

            return new Row(newList);
        }
    }


    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Nov 4, '21
     * @author         Enter your name here...
     */
    public static class ColumnsAfter extends Converter {

        /**  */
        private HashSet<Integer> set;

        /**  */
        private String col;

        /**  */
        private int colIdx = -1;

        /**
         *
         * @param ctx _more_
         * @param col _more_
         * @param cols _more_
         */
        public ColumnsAfter(TextReader ctx, String col, List<String> cols) {
            super(cols);
            this.col = col;
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (colIdx == -1) {
                Integer i = getColumnIndex(ctx, col);
                if (i == null) {
                    fatal(ctx, "Could not find index:" + col);
                }
                colIdx = i;
            }

            List<Integer> indices = getIndices(ctx);
            if (set == null) {
                set = new HashSet<Integer>();
                for (int i : indices) {
                    set.add(i);
                }
            }
            if (indices.size() == 0) {
                return row;
            }

            List values  = row.getValues();
            List newList = new ArrayList();
            for (int i = colIdx + 1; i < values.size(); i++) {
                if ( !set.contains(i)) {
                    newList.add(values.get(i));
                }
            }
            newList.add(0, values.get(colIdx));
            for (int i = indices.size() - 1; i >= 0; i--) {
                newList.add(1, values.get(indices.get(i)));
            }
            for (int i = colIdx - 1; i >= 0; i--) {
                if (set.contains(i)) {
                    continue;
                }
                newList.add(0, values.get(i));
            }

            return new Row(newList);
        }
    }


    /**
     * Class description
     *
     * @version        $version$, Mon, Oct 14, '19
     * @author         Enter your name here...
     */
    public static class ColumnNotSelector extends Converter {

        /**
         *
         * @param ctx _more_
         * @param cols _more_
         */
        public ColumnNotSelector(TextReader ctx, List<String> cols) {
            super(cols);
        }


        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            //      System.err.println("row:" + row);
            List<Integer> indices = getIndices(ctx);

            return removeColumns(indices, row);
        }

    }




    /* */

    /** _more_ */
    private static Hashtable<String, String> imageMap = new Hashtable<String,
                                                            String>();

    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Jan 9, '15
     * @author         Jeff McWhirter
     */
    public static class ImageSearch extends Converter {

        /* */

        /** _more_ */
        private String suffix;

        /** _more_ */
        private String imageColumn;

        /** _more_ */
        private int imageColumnIndex = -1;

        /**
         *
         * @param ctx _more_
         * @param cols _more_
         * @param suffix _more_
         */
        public ImageSearch(TextReader ctx, List<String> cols, String suffix) {
            super(cols);
            this.suffix = suffix;
        }

        /**
         *
         * @param ctx _more_
         * @param cols _more_
         * @param suffix _more_
         * @param imageColumn _more_
         */
        public ImageSearch(TextReader ctx, List<String> cols, String suffix,
                           String imageColumn) {
            super(cols);
            this.suffix      = suffix;
            this.imageColumn = imageColumn;
        }


        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if ((imageColumn != null) && (imageColumnIndex == -1)) {
                imageColumnIndex = getIndex(ctx, imageColumn);
            }
            if (rowCnt++ == 0) {
                if (imageColumnIndex == -1) {
                    add(ctx, row, "image");
                }

                return row;
            }


            List<Integer> indices = getIndices(ctx);
            String        s       = "";
            for (Integer idx : indices) {
                s += row.getString(idx) + " ";
            }
            s += suffix;
            //hack, hack
            String script = "/Users/jeffmc/bin/imagesearch.sh";
            for (int attempt = 0; attempt < 3; attempt++) {
                if (imageColumnIndex >= 0) {
                    String existing = (String) row.get(imageColumnIndex);
                    if (Utils.stringDefined(existing)) {
                        return row;
                    }
                }
                String result = "";
                try {
                    s = s.replace(" ", "%s");
                    String image = imageMap.get(s);
                    if (image == null) {
                        Process p = Runtime.getRuntime().exec(new String[] {
                                        "sh",
                                        script, s });
                        result =
                            IO.readInputStream(p.getInputStream()).trim();
                        JSONObject obj    = new JSONObject(result);
                        JSONArray  values = obj.getJSONArray("value");
                        if (values.length() == 0) {
                            System.err.println(s + " failed. sleeping");
                            if (attempt == 0) {
                                System.err.println("response:" + result);
                            }
                            Misc.sleepSeconds(1 + attempt);

                            continue;
                        }
                        JSONObject value = values.getJSONObject(0);
                        image = value.optString("thumbnailUrl", "");
                        System.err.println("found image:" + s + " image:"
                                           + image);
                        ctx.printDebug("-image",
                                       "value:" + s + " found:" + image);
                        imageMap.put(s, image);
                    } else {
                        ctx.printDebug("-image",
                                       "value:" + s + " in cache:" + image);
                    }
                    if (imageColumnIndex >= 0) {
                        row.set(imageColumnIndex, image);
                    } else {
                        add(ctx, row, image);
                    }

                    return row;
                } catch (Exception exc) {
                    System.err.println("JSON:" + result);

                    throw new RuntimeException(exc);
                }
            }
            if (imageColumnIndex == -1) {
                add(ctx, row, "");
            }

            return row;
        }

    }

    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Nov 4, '21
     * @author         Enter your name here...
     */
    public static class Embed extends Converter {

        /** _more_ */
        private String imageColumn;

        /** _more_ */
        private int imageColumnIndex = -1;

        /**
         *
         * @param ctx _more_
         * @param col _more_
         */
        public Embed(TextReader ctx, String col) {
            super();
            imageColumn = col;
        }




        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                imageColumnIndex = getIndex(ctx, imageColumn);
                row.add("Image Data");

                return row;
            }

            String url = row.getString(imageColumnIndex);
            if ( !Utils.stringDefined(url)) {
                row.add("");

                return row;
            }
            url = url.trim();
            if (url.toLowerCase().startsWith("file")) {
                fatal(ctx, "Bad url:" + url);
            }
            String type = IOUtil.getFileExtension(url);
            if ( !Utils.stringDefined(type)) {
                type = "png";
            }
            try {
                URL         _url  = new URL(url);
                InputStream is    = IO.getInputStream(_url);
                byte[]      bytes = IOUtil.readBytes(is);
                String b = "data:image/" + type + ";base64, "
                           + Utils.encodeBase64Bytes(bytes);
                row.add(b);
            } catch (Exception exc) {
                System.err.println("Error reading url:" + url);
                exc.printStackTrace();
                row.add("");
                //              fatal(ctx, "Reading url:" + url, exc);
            }

            return row;
        }

    }


    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Nov 4, '21
     * @author         Enter your name here...
     */
    public static class Fetch extends Converter {

        /**  */
        private Row headerRow;

        /** _more_ */
        private String name;

        /** _more_ */
        private String urlTemplate;

        /**
         *
         * @param ctx _more_
         * @param name _more_
         * @param urlTemplate _more_
         */
        public Fetch(TextReader ctx, String name, String urlTemplate) {
            super();
            this.name        = name;
            this.urlTemplate = urlTemplate;
        }


        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                headerRow = row;
                row.add(name);

                return row;
            }
            if (rowCnt > 5) {
                //              row.add("FILLER");
                //              return row;
            }

            String url = urlTemplate;
            //Offset by -1 because we added the name to the end
            for (int i = 0; i < headerRow.size() - 1; i++) {
                String id = Utils.makeID(headerRow.getString(i));
                url = url.replace("${" + id + "}", row.getString(i));
            }

            System.err.println("URL:" + url);
            if (url.toLowerCase().startsWith("file")) {
                fatal(ctx, "Bad url:" + url);
            }
            try {
                URL         _url     = new URL(url);
                InputStream is       = IO.getInputStream(_url);
                String      contents = IO.readInputStream(is);
                is.close();
                row.add(contents);
            } catch (Exception exc) {
                fatal(ctx, "Reading url:" + url, exc);
            }

            return row;
        }

    }


    /**
     * Class description
     * @version        $version$, Fri, Jan 9, '15
     * @author         Jeff McWhirter
     */
    public static class WikiDescSearch extends Converter {

        /* */

        /** _more_ */
        private String suffix;

        /**
         * @param cols _more_
         * @param suffix _more_
         */
        public WikiDescSearch(List<String> cols, String suffix) {
            super(cols);
            this.suffix = suffix;
        }


        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                add(ctx, row, "description");

                return row;
            }

            try {
                List<Integer> indices = getIndices(ctx);
                String        s       = "";
                for (Integer idx : indices) {
                    s += row.getString(idx) + " ";
                }
                s += suffix;
                s = s.replaceAll(" ", "%20");
                String baseUrl = "https://en.wikipedia.org/w/api.php";
                String url =
                    baseUrl
                    + "?action=query&srlimit=1&list=search&format=json&srsearch="
                    + s;
                String     result = IO.readUrl(url);
                JSONObject obj    = new JSONObject(result);
                JSONArray  values = JsonUtil.readArray(obj, "query.search");
                if (values.length() == 0) {
                    System.err.println("No results for query:" + s);
                    add(ctx, row, "");

                    return row;
                }
                JSONObject value   = values.getJSONObject(0);
                String     snippet = value.optString("snippet", "");
                String     title   = value.optString("title", "");
                title = title.replaceAll(" ", "%20");
                url = baseUrl + "?action=parse&prop=text&page=" + title
                      + "&format=json";
                result = IO.readUrl(url);
                obj    = new JSONObject(result);
                obj    = JsonUtil.readObject(obj, "parse.text");
                String contents = obj.optString("*", "");
                String p = StringUtil.findPattern(contents,
                               "(?s)/table>(.*?)<div id=\"toc\"");
                if (p != null) {
                    String p2 = StringUtil.findPattern(contents,
                                    "(?s).*?<p>(.*?)</p>");
                    if (p2 != null) {
                        p = p2;
                    } else {
                        p = snippet;
                    }
                }
                if (p == null) {
                    p = snippet;
                }
                add(ctx, row, p);

                return row;
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }
        }

    }


    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Jan 11, '19
     * @author         Enter your name here...
     */
    public static class ColumnWidth extends Converter {

        /* */

        /** _more_ */
        int size;

        /**
         *
         *
         * @param cols _more_
         * @param size _more_
         */
        public ColumnWidth(List<String> cols, int size) {
            super(cols);
            this.size = size;
        }



        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            List<Integer> indices = getIndices(ctx);
            for (int i : indices) {
                if ((i < 0) || (i >= row.size())) {
                    continue;
                }
                String s = row.get(i).toString();
                if (s.length() > size) {
                    s = s.substring(0, size - 1);
                }
                row.set(i, s);
            }

            return row;
        }

    }


    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Dec 27, '18
     * @author         Enter your name here...
     */
    public static class ColumnFormatter extends Converter {

        /* */

        /** _more_ */
        private DecimalFormat format;

        /**
         *
         * @param cols _more_
         * @param fmt _more_
         */
        public ColumnFormatter(List<String> cols, String fmt) {
            super(cols);
            format = new DecimalFormat(fmt);
        }



        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            List<Integer> indices = getIndices(ctx);
            for (int i : indices) {
                if ((i < 0) || (i >= row.size())) {
                    continue;
                }
                try {
                    double value = Double.parseDouble(row.get(i).toString());
                    row.set(i, format.format(value));
                } catch (NumberFormatException nfe) {}
            }

            return row;
        }
    }

    /**
     * Class description
     *
     *
     * @version        $version$, Wed, Dec 2, '15
     * @author         Enter your name here...
     */
    public static class Padder extends Converter {

        /* */

        /** _more_ */
        private int count;

        /* */

        /** _more_ */
        private String pad;

        /**
         * @param count _more_
         * @param s _more_
         */
        public Padder(int count, String s) {
            this.count = count;
            this.pad   = s;
        }


        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            while (row.getValues().size() < count) {
                row.getValues().add(pad);
            }
            while (row.getValues().size() > count) {
                row.getValues().remove(row.getValues().size() - 1);
            }

            return row;
        }

    }


    /**
     * Class description
     *
     *
     * @version        $version$, Wed, Feb 20, '19
     * @author         Enter your name here...
     */
    public static class Prefixer extends Converter {



        /* */

        /** _more_ */
        private String pad;

        /**
         * @param cols _more_
         * @param s _more_
         */
        public Prefixer(List<String> cols, String s) {
            super(cols);
            this.pad = s;
        }


        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                if ( !ctx.getAllData()) {
                    return row;
                }
            }
            List<Integer> indices = getIndices(ctx);
            for (int i : indices) {
                row.set(i, pad + row.get(i));
            }

            return row;
        }
    }


    /**
     * Class description
     *
     *
     * @version        $version$, Wed, Feb 20, '19
     * @author         Enter your name here...
     */
    public static class Suffixer extends Converter {

        /* */

        /** _more_ */
        private String pad;

        /**
         * @param cols _more_
         * @param s _more_
         */
        public Suffixer(List<String> cols, String s) {
            super(cols);
            this.pad = s;
        }


        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                if ( !ctx.getAllData()) {
                    return row;
                }
            }
            List<Integer> indices = getIndices(ctx);
            for (int i : indices) {
                row.set(i, row.get(i) + pad);
            }

            return row;
        }
    }



    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Jan 24, '19
     * @author         Enter your name here...
     */
    public static class PrintHeader extends Converter {

        /* */

        /** _more_ */
        private boolean asPoint = false;

        /**
         */
        public PrintHeader() {}

        /**
         * @param asPoint _more_
         */
        public PrintHeader(boolean asPoint) {
            this.asPoint = asPoint;
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            PrintWriter writer    = ctx.getWriter();
            String      delimiter = ctx.getDelimiter();
            if (delimiter == null) {
                delimiter = ",";
            }
            if (asPoint) {
                writer.println("skiplines=1");
                writer.print("fields=");
            }

            for (int i = 0; i < row.size(); i++) {
                String col = ((String) row.get(i).toString()).trim();
            }


            for (int i = 0; i < row.size(); i++) {
                String col = ((String) row.get(i).toString()).trim();
                col = col.replaceAll("\n", " ");
                if (asPoint) {
                    if (i > 0) {
                        writer.print(", ");
                    }
                    String label =
                        Utils.makeLabel(col.replaceAll("\\([^\\)]+\\)", ""));
                    String unit = StringUtil.findPattern(col,
                                      ".*?\\(([^\\)]+)\\).*");
                    StringBuffer attrs = new StringBuffer();
                    attrs.append("label=\"" + label + "\" ");
                    if (unit != null) {
                        attrs.append("unit=\"" + unit + "\" ");

                    }
                    String id = label.replaceAll(
                                    "\\([^\\)]+\\)", "").replaceAll(
                                    "-", "_").trim().toLowerCase().replaceAll(
                                    " ", "_").replaceAll(":", "_");
                    id = id.replaceAll("/+", "_");
                    id = id.replaceAll("\\.", "_");
                    id = id.replaceAll("_+_", "_");
                    id = id.replaceAll("_+$", "");
                    id = id.replaceAll("^_+", "");
                    if (id.indexOf("date") >= 0) {
                        attrs.append("type=\"date\" format=\"\" ");
                    }
                    writer.print(id + "[" + attrs + "] ");
                } else {
                    writer.println("#" + i + " " + col);
                }
            }
            if (asPoint) {
                writer.println("");
            }
            writer.flush();
            writer.close();

            ctx.stopRunning();

            return null;
        }

    }


    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Nov 4, '21
     * @author         Enter your name here...
     */
    public static class HeaderNames extends Converter {

        /**
         */
        public HeaderNames() {}

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ > 0) {
                return row;
            }
            for (int i = 0; i < row.size(); i++) {
                String s = row.getString(i);
                s = s.replaceAll("([A-Z])", "xdelimiter$1");
                s = Utils.makeID(s);
                s = s.replace("_", "xdelimiter");
                List<String> toks = Utils.split(s, "xdelimiter", true, true);
                String       tmp  = "";
                for (String tok : toks) {
                    if (tmp.length() > 0) {
                        tmp += " ";
                    }
                    tok = Utils.upperCaseFirst(tok);
                    tmp += tok;
                }
                tmp = tmp.replaceAll("[^\\p{ASCII}]", " ");
                row.set(i, tmp);
            }

            return row;
        }

    }


    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Nov 4, '21
     * @author         Enter your name here...
     */
    public static class HeaderIds extends Converter {

        /**
         */
        public HeaderIds() {}

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ > 0) {
                return row;
            }
            for (int i = 0; i < row.size(); i++) {
                String s = row.getString(i);
                s = Utils.makeID(s);
                row.set(i, s);
            }

            return row;
        }

    }



    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Nov 4, '21
     * @author         Enter your name here...
     */
    public static class Ids extends Converter {

        /**
         */
        public Ids() {}

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ > 0) {
                return row;
            }
            for (int i = 0; i < row.size(); i++) {
                String s = row.getString(i);
                s = Utils.makeID(s);
                row.set(i, s);
            }

            return row;
        }

    }



    /**
     * Class description
     *
     *
     * @version        $version$, Wed, Dec 2, '15
     * @author         Enter your name here...
     */
    public static class HeaderMaker extends Converter {

        /** _more_ */
        private int count = 0;

        /* */

        /** _more_ */
        PatternProps props;

        /* */

        /** _more_ */
        String defaultType = "string";

        /**  */
        String defaultTypeFromProperties;

        /* */

        /** _more_ */
        boolean defaultChartable = true;

        /** _more_ */
        boolean makeLabel = true;

        /** _more_ */
        boolean toStdOut = false;

        /** _more_ */
        Row firstRow;

        /**
         * @param props _more_
         */
        public HeaderMaker(Hashtable<String, String> props) {
            this.props = new PatternProps(props);
            defaultType = CsvUtil.getDbProp(props, "default", "type",
                                            defaultType);
            defaultTypeFromProperties = CsvUtil.getDbProp(props, "default",
                    "type", null);
            defaultChartable = CsvUtil.getDbProp(props, "default",
                    "chartable", true);
            makeLabel = CsvUtil.getDbProp(props, null, "makeLabel", true);
            toStdOut  = CsvUtil.getDbProp(props, null, "stdout", false);
        }


        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            boolean debug = Misc.equals(props.get("debug"), "true");
            //      debug = true;
            rowCnt++;
            if (rowCnt > 2) {
                if (debug) {
                    System.err.println("addHeader data row:" + row);
                }

                return row;
            }
            if (firstRow == null) {
                firstRow = row;
                if (debug) {
                    System.err.println("got first row:" + row);
                }

                return null;
            }
            boolean justFields  = Misc.equals(props.get("justFields"),
                                      "true");
            PrintWriter  writer = ctx.getWriter();
            StringBuffer sb     = new StringBuffer();
            if (toStdOut) {
                sb.append("fields=");
            } else {
                sb.append("#fields=");
            }
            List values = new ArrayList<String>();
            String dfltFormat = CsvUtil.getDbProp(props, "default", "format",
                                    null);
            String dfltUnit = CsvUtil.getDbProp(props, "default", "unit",
                                  null);
            for (int i = 0; i < firstRow.getValues().size(); i++) {
                String   col = (String) firstRow.getValues().get(i);
                String[] toks;
                String   desc  = null;
                String   label = null;

                try {
                    toks = Utils.findPatterns(col, "<desc>(.*)</desc>");
                    if ((toks != null) && (toks.length == 1)) {
                        desc = toks[0];
                        desc = desc.replaceAll(",", "_comma_");
                        desc = desc.replaceAll("\"", "").replaceAll("\n",
                                " ");
                        col = col.replaceAll("<desc>.*</desc>", "");
                    }
                    toks = Utils.findPatterns(col, "<label>(.*)</label>");
                    if ((toks != null) && (toks.length == 1)) {
                        label = toks[0];
                        col   = col.replaceAll("<label>.*</label>", "");
                    }
                } catch (Exception exc) {
                    throw new IllegalArgumentException(exc);
                }


                Object osample = row.getValues().get(i);
                if (osample == null) {
                    continue;
                }
                String sample  = (String) osample.toString();
                String _sample = sample.toLowerCase();
                col = col.replaceAll("\u00B5", "u").replaceAll("\u00B3",
                                     "^3").replaceAll("\n", " ");
                String id = col.replaceAll("\\([^\\)]+\\)", "").replaceAll(
                                "\\?", "").replaceAll("\\$", "").replaceAll(
                                ",", "_").replaceAll(
                                "-", "_").trim().toLowerCase().replaceAll(
                                " ", "_").replaceAll(":", "_");

                id = id.replaceAll("<", "_").replaceAll(">", "_");
                id = id.replaceAll("\\+", "_").replaceAll(
                    "\"", "_").replaceAll("%", "_").replaceAll(
                    "\'", "_").replaceAll("/+", "_").replaceAll(
                    "\\.", "_").replaceAll("_+_", "_").replaceAll(
                    "_+$", "").replaceAll("^_+", "").replaceAll("\\^", "_");

                id = CsvUtil.getDbProp(props, id, i, "id", id);


                StringBuffer attrs = new StringBuffer();
                String group = CsvUtil.getDbProp(props, id, i, "group",
                                   (String) null);
                if (group != null) {
                    attrs.append(" group=\"" + group + "\" ");
                }

                if (label == null) {
                    label = CsvUtil.getDbProp(props, id, i, "label",
                            (String) null);
                }
                if (makeLabel && (label == null)) {
                    label = Utils.makeLabel(col.replaceAll("\\([^\\)]+\\)",
                            ""));
                }
                String unit = StringUtil.findPattern(col,
                                  ".*?\\(([^\\)]+)\\).*");
                if (label != null) {
                    label = label.replaceAll(",", "%2C").replaceAll("<br>",
                                             " ").replaceAll("<p>", " ");
                    label = label.replaceAll("  +", " ").replace("_space_",
                                             " ");
                    attrs.append("label=\"" + label + "\" ");
                }
                if (desc != null) {
                    attrs.append(" description=\"" + desc + "\" ");
                }
                if (unit == null) {
                    unit = CsvUtil.getDbProp(props, id, i, "unit", dfltUnit);
                }
                if (unit != null) {
                    attrs.append("unit=\"" + unit + "\" ");

                }
                String format = dfltFormat;
                String type   = (defaultTypeFromProperties != null)
                                ? defaultTypeFromProperties
                                : defaultType;
                //              System.err.println("id:"  + id  + " default:" + type);
                boolean isGeo = false;

                boolean chartable = CsvUtil.getDbProp(props, id, "chartable",
                                        defaultChartable);
                if (id.equals("date")) {
                    type = "date";
                } else if (id.equals("year")) {
                    type   = "date";
                    format = "yyyy";
                } else if (id.startsWith("date_")) {
                    type   = "date";
                    format = "yyyy-MM-dd";
                } else if (id.equals("url") || id.endsWith("_url")
                           || id.equals("website")) {
                    if (id.indexOf("photo") >= 0) {
                        type = "image";
                    } else {
                        type = "url";
                    }
                } else if (id.equals("state") || id.startsWith("state_")
                           || id.equals("country") || id.equals("category")
                           || id.equals("party") || id.equals("class")) {
                    type = "enumeration";
                } else if (id.equals("city") || id.endsWith("_city")) {
                    type = "enumeration";
                } else if (id.equals("gender") || id.equals("ethnicity")
                           || id.equals("religion")) {
                    type = "enumeration";
                } else if (id.equals("latitude") || id.equals("longitude")) {
                    type      = "double";
                    isGeo     = true;
                    chartable = false;
                } else if (defaultTypeFromProperties == null) {
                    try {
                        if (_sample.equals("true")
                                || _sample.equals("false")) {
                            type = "enumeration";
                        } else if (_sample.equals("nan")
                                   || _sample.equals("na")) {
                            type = "double";
                        } else if (sample.startsWith("http")) {
                            if (Utils.isImage(sample)) {
                                type = "image";
                            } else {
                                type = "url";
                            }
                        } else if (sample.matches("^(\\+|-)?\\d+$")) {
                            type = "integer";
                            //                      System.err.println("\tinteger:" + sample);
                        } else if (sample.matches(
                                "^[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?$")) {
                            //                      System.err.println("\tdouble:" + sample);
                            type = "double";
                        } else if (sample.matches(
                                "\\d\\d\\d\\d-\\d\\d-\\d\\d")) {
                            type   = "date";
                            format = "yyyy-MM-dd";
                        } else {}
                    } catch (Exception exc) {}
                }

                //              System.err.println("\tfinal type:" + type);

                type = CsvUtil.getDbProp(props, id, i, "type", type);
                if (Misc.equals(type, "enum")) {
                    type = "enumeration";
                }
                format = CsvUtil.getDbProp(props, id, i, "format", format);
                if (format != null) {
                    format = format.replaceAll("_space_", " ");
                }

                attrs.append(" type=\"" + type + "\"");
                String enumeratedValues = CsvUtil.getDbProp(props, id, i,
                                              "enumeratedValues", null);
                if (enumeratedValues != null) {
                    attrs.append(" enumeratedValues=\"" + enumeratedValues
                                 + "\"");
                }
                if (Misc.equals(type, "date")) {
                    if (format == null) {
                        format = "yyyy-MM-dd";
                    }
                    attrs.append(" format=\"" + format + "\" ");
                }
                if (type.equals("double") || type.equals("integer")) {
                    if (chartable) {
                        attrs.append(" chartable=\"" + "true" + "\" ");
                    } else {
                        attrs.append(" chartable=\"" + "false" + "\" ");
                    }
                }
                if (debug) {
                    writer.println(StringUtil.padLeft(id, 20) + " " + attrs
                                   + "  sample:" + sample);
                }
                String field;

                if (justFields) {
                    field = id;
                } else {
                    field = id + "[" + attrs + "] ";
                }
                if (i == 0) {
                    if (toStdOut) {
                        field = "fields=" + field;
                    } else {
                        field = "#fields=" + field;
                    }
                }
                values.add(field);
            }

            if (debug) {
                System.err.println("header values:" + values);
            }
            Processor nextProcessor = getNextProcessor();
            firstRow.setValues(values);
            if (nextProcessor != null) {
                try {
                    if (debug) {
                        System.err.println(
                            "addheader: telling nextProcessor to handle row:"
                            + firstRow);
                    }
                    nextProcessor.handleRow(ctx, firstRow);
                } catch (Exception exc) {
                    throw new RuntimeException(exc);
                }
            }
            firstRow = null;

            if (debug) {
                System.err.println("addheader: returning row:" + row);
            }


            return row;

        }
    }



    /**
     * Class description
     *
     *
     * @version        $version$, Wed, Jan 17, '18
     * @author         Enter your name here...
     */
    public static class ColumnPercenter extends Converter {



        /**
         * @param cols _more_
         */
        public ColumnPercenter(List<String> cols) {
            super(cols);
        }


        /**
         *
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            List          values  = row.getValues();
            double        total   = 0;
            int           cnt     = 0;
            List<Integer> indices = getIndices(ctx);
            for (Integer idx : indices) {
                String s = values.get(idx).toString().trim();
                double v = (s.length() == 0)
                           ? 0
                           : Double.parseDouble(s);
                total += v;
                cnt++;
            }
            for (Integer idx : indices) {
                String s = values.get(idx).toString().trim();
                double v = (s.length() == 0)
                           ? 0
                           : Double.parseDouble(s);
                if (total != 0) {
                    values.set(idx, v / total);
                } else {
                    values.set(idx, Double.NaN);
                }
            }

            return row;
        }

    }



    /**
     * Class description
     *
     * @version        $version$, Tue, Nov 19, '19
     * @author         Enter your name here...
     */
    public static class ColumnIncrease extends Converter {


        /* */

        /** _more_ */
        int step;

        /* */

        /** _more_ */
        List<Double> values = new ArrayList<Double>();

        /**
         * @param col _more_
         * @param step _more_
         */
        public ColumnIncrease(String col, int step) {
            super(col);
            this.step = step;
        }


        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            int col = getIndex(ctx);
            if (rowCnt++ == 0) {
                add(ctx, row, row.get(col) + " increase");

                return row;
            }
            double v = Double.parseDouble(row.get(col).toString());
            if (values.size() < step) {
                add(ctx, row, new Double(Double.NaN));
            } else {
                double pastValue = values.get(0);
                values.remove(0);
                double increase = 0;
                // 20 30
                if (pastValue == 0) {
                    add(ctx, row, new Double(Double.NaN));
                } else {
                    double diff = v - pastValue;
                    increase = diff / pastValue;
                    //              System.out.println("x:" + v +" " + pastValue +"  diff:" + diff +" i:" + increase);
                    add(ctx, row, new Double(increase));
                }
            }
            values.add(v);

            return row;
        }

    }


    /**
     * Class description
     *
     *
     * @version        $version$, Tue, Nov 19, '19
     * @author         Enter your name here...
     */
    public static class Ranges extends Converter {

        /** _more_ */
        String name;

        /**  */
        double start;

        /**  */
        double size;

        /**
         *
         * @param ctx _more_
         * @param col _more_
         * @param name _more_
         * @param start _more_
         * @param size _more_
         */
        public Ranges(TextReader ctx, String col, String name, double start,
                      double size) {
            super(col);
            this.name  = name;
            this.start = start;
            this.size  = size;
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            int col = getIndex(ctx);
            if (rowCnt++ == 0) {
                add(ctx, row, name);

                return row;
            }
            double v = Double.parseDouble(row.get(col).toString());
            if (Double.isNaN(v)) {
                add(ctx, row, "");
            } else if (v < start) {
                add(ctx, row, "<" + start);
            } else {
                double offset  = v - start;
                int    buckets = (int) (offset / size);
                double s       = start + buckets * size;
                double e       = s + size;
                String ss      = "" + (((int) s) == s
                                       ? (int) s
                                       : s);
                String se      = "" + (((int) e) == e
                                       ? (int) e
                                       : e);
                add(ctx, row, Utils.format(s) + "-" + Utils.format(e));
            }

            return row;
        }

    }


    /**
     * Class description
     *
     * @version        $version$, Tue, Nov 19, '19
     * @author         Enter your name here...
     */
    public static class And extends Converter {

        /**  */
        private String name;

        /**
         * @param name _more_
         * @param cols _more_
         */
        public And(String name, List<String> cols) {
            super(cols);
            this.name = name;
        }


        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                row.add(name);

                return row;
            }
            List<Integer> indices = getIndices(ctx);
            boolean       r       = true;
            for (int index : indices) {
                if ((index >= 0) && (index < row.size())) {
                    if ( !getBoolean(row.getString(index))) {
                        r = false;

                        break;
                    }
                }
            }
            row.add("" + r);

            return row;
        }

    }



    /**
     * Class description
     *
     *
     * @version        $version$, Tue, Nov 19, '19
     * @author         Enter your name here...
     */
    public static class Or extends Converter {

        /**  */
        private String name;

        /**
         * @param name _more_
         * @param cols _more_
         */
        public Or(String name, List<String> cols) {
            super(cols);
            this.name = name;
        }


        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                row.add(name);

                return row;
            }
            List<Integer> indices = getIndices(ctx);
            boolean       r       = false;
            for (int index : indices) {
                if ((index >= 0) && (index < row.size())) {
                    String v = row.getString(index);
                    if (v == null) {
                        v = "false";
                    }
                    if (getBoolean(row.getString(index))) {
                        r = true;

                        break;
                    }
                }
            }
            row.add("" + r);

            return row;
        }

    }



    /**
     * Class description
     *
     *
     * @version        $version$, Tue, Nov 19, '19
     * @author         Enter your name here...
     */
    public static class Not extends Converter {

        /**  */
        private String name;

        /**
         * @param name _more_
         * @param col _more_
         */
        public Not(String name, String col) {
            super(col);
            this.name = name;
        }


        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                row.add(name);

                return row;
            }
            boolean b = getBoolean(row.getString(getIndex(ctx)));
            row.add("" + !b);

            return row;
        }

    }



    /**
     * Class description
     *
     *
     * @version        $version$, Tue, Nov 19, '19
     * @author         Enter your name here...
     */
    public static class ColumnAverage extends Converter {

        /* */

        /** _more_ */
        public static final int MA = 0;

        /* */

        /** _more_ */
        int what;

        /* */

        /** _more_ */
        int period;

        /* */

        /** _more_ */
        List<List<Double>> values = new ArrayList<List<Double>>();

        /* */

        /** _more_ */
        String label;

        /**
         * @param what _more_
         * @param cols _more_
         * @param period _more_
         * @param label _more_
         */
        public ColumnAverage(int what, List<String> cols, int period,
                             String label) {
            super(cols);
            this.what   = what;
            this.period = period;
            this.label  = label;
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            List<Integer> indices = getIndices(ctx);
            if (rowCnt++ == 0) {
                for (int i = 0; i < indices.size(); i++) {
                    values.add(new ArrayList<Double>());
                    add(ctx, row, row.get(indices.get(i)) + " " + label);
                }

                return row;
            }

            for (int i = 0; i < indices.size(); i++) {
                int          index = indices.get(i);
                List<Double> nums  = values.get(i);
                double       v =
                    Double.parseDouble(row.get(index).toString());
                nums.add(v);
                if (nums.size() > period) {
                    nums.remove(0);
                }
                double total = 0;
                int    cnt   = 0;
                for (int j = 0; j < nums.size(); j++) {
                    if ( !Double.isNaN(nums.get(j))) {
                        cnt++;
                        total += nums.get(j);
                    }
                }
                double average = (cnt == 0)
                                 ? Double.NaN
                                 : total / cnt;
                add(ctx, row, average);
            }

            return row;
        }

    }











    /**
     * Class description
     *
     *
     * @version        $version$, Wed, Jan 17, '18
     * @author         Enter your name here...
     */
    public static class ColumnFunc extends Converter {

        /** _more_ */
        String code;

        /** _more_ */
        List<String> names;

        /** _more_ */
        Row headerRow;

        /** _more_ */
        org.mozilla.javascript.Context cx =
            org.mozilla.javascript.Context.enter();

        /** _more_ */
        org.mozilla.javascript.Scriptable scope =
            cx.initSafeStandardObjects();

        /** _more_ */
        org.mozilla.javascript.Script script;

        /**
         *
         * @param ctx _more_
         * @param js _more_
         * @param names _more_
         * @param code _more_
         */
        public ColumnFunc(TextReader ctx, String js, String names,
                          String code) {
            this.names = Utils.split(names, ",", true, true);
            if (code.indexOf("return ") >= 0) {
                code = "function _tmp() {" + code + "}\n_tmp()";
            }
            this.code = code;
            try {
                cx    = org.mozilla.javascript.Context.enter();
                scope = cx.initSafeStandardObjects();
                eval("var globalCache = {};");
                if (js.trim().length() > 0) {
                    eval(js);
                }
                String testScript =
                    "print(java.lang.System.getProperty(\"java.home\"));"
                    + "print(\"Create file variable\");"
                    + "var File = Java.type(\"java.io.File\");";
                //                eval(testScript);
                script = cx.compileString(code, "code", 0, null);
            } catch (Exception exc) {
                fatal(ctx, "Error evaluation function:" + code, exc);
                //                throw new RuntimeException(exc);
            }
        }

        /**
         * _more_
         * @param s _more_
         * @return _more_
         * @throws Exception _more_
         */
        private Object eval(String s) throws Exception {
            if (s == null) {
                return script.exec(cx, scope);
            }

            return cx.evaluateString(scope, s, "<cmd>", 1, null);
        }

        /**
         * _more_
         * @param name _more_
         * @param value _more_
         * @throws Exception _more_
         */
        private void put(String name, Object value) throws Exception {
            scope.put(name, scope, value);
        }


        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            rowCnt++;
            debug("processRow");
            if (rowCnt == 1) {
                headerRow = new Row(row);
                for (String s : names) {
                    row.add(s);
                }

                return row;
            }
            List<String> vars = null;
            try {
                List hdr = headerRow.getValues();
                if (ctx.getDebug()) {
                    ctx.printDebug("function:" + code);
                    vars = new ArrayList<String>();
                }
                for (int i = 0; i < hdr.size(); i++) {
                    if (i >= row.size()) {
                        continue;
                    }
                    Object o   = row.get(i);
                    String var = hdr.get(i).toString();
                    var = Utils.makeID(var.toLowerCase());
                    if (ctx.getDebug()) {
                        ctx.printDebug("\tvar:" + var);
                    }
                    put("_" + var, o);
                    put("_" + var + "_idx", i);
                    put("_col" + i, o);
                    if (vars != null) {
                        vars.add("_" + var);
                    }
                }
                put("_header", hdr);
                put("_values", row.getValues());
                put("_rowidx", rowCnt - 1);
                Object o = eval(null);
                if (names.size() == 0) {
                    if (o == null) {
                        return row;
		    }
                    String s = o.toString();
                    if (s.equals("false")) {
                        return null;
                    }

                    return row;
                }
                if (o == null) {
                    return null;
                }

                row.add(org.mozilla.javascript.Context.toString(o));

                //              System.err.println("func row:" + row);
                return row;
            } catch (Exception exc) {
                fatal(ctx,
                      "Error evaluating function:" + code + "\n\theader:"
                      + headerRow + ((vars != null)
                                     ? "\n\tvars:" + Utils.join(vars, ",")
                                     : ""), exc);

                //                throw new RuntimeException(exc);
                return row;
            }
        }

    }



    /**
     * Class description
     *
     *
     * @version        $version$, Wed, Jan 17, '18
     * @author         Enter your name here...
     */
    public static class ColumnOperator extends Converter {

        /**
         */
        public ColumnOperator() {}

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            List   values    = row.getValues();
            List   newValues = new ArrayList();
            double total     = 0;
            int    cnt       = 0;
            for (int i = 0; i < values.size(); i++) {
                String s = values.get(i).toString();
                if (s.trim().length() == 0) {
                    continue;
                }
                if (s.matches("(-|\\+)?[\\d\\.]+")) {
                    double d = Double.parseDouble(s);
                    total += d;
                } else {}
            }


            for (int i = 0; i < values.size(); i++) {
                String s = values.get(i).toString();
                if (s.trim().length() == 0) {
                    continue;
                }
                if (s.matches("(-|\\+)?[\\d\\.]+")) {}
                else {
                    newValues.add(s);
                }
            }
            newValues.add(new Double(total));
            row.setValues(newValues);

            return row;
        }

    }





    /**
     * Class description
     *
     *
     * @version        $version$, Wed, Apr 6, '22
     * @author         Enter your name here...
     */
    public static class Changer extends Converter {

        /** _more_ */
        boolean isRegex;

        /**  */
        List<String[]> patterns = new ArrayList<String[]>();

        /**
         *
         * @param ctx _more_
         * @param cols _more_
         * @param pattern _more_
         * @param value _more_
         */
        public Changer(TextReader ctx, List<String> cols, String pattern,
                       String value) {
            super(cols);
            if (pattern.startsWith("file:")) {
                String file = pattern.substring("file:".length());
                if ( !IO.okToReadFrom(file)) {
                    fatal(ctx, "Cannot read file:" + file);
                }
                try {
                    init(file);
                } catch (Exception exc) {
                    fatal(ctx, "Reading file:" + file, exc);
                }
                this.isRegex = true;
            } else {
                this.isRegex = StringUtil.containsRegExp(pattern);
                patterns.add(new String[] { Utils.convertPattern(pattern),
                                            value });
            }
        }

        /**
         * @param file _more_
         * @throws Exception _more_
         */
        private void init(String file) throws Exception {
            String       contents = IO.readContents(file);
            List<String> lines    = Utils.split(contents, "\n");
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if ((line.length() == 0) || line.startsWith("#")) {
                    continue;
                }
                List<String> toks;
                if (line.indexOf("::") >= 0) {
                    toks = Utils.splitUpTo(line, "::", 2);
                } else {
                    toks = Utils.splitUpTo(line, ",", 2);
                }
                String pattern = toks.get(0);
                patterns.add(new String[] { Utils.convertPattern(pattern),
                                            (toks.size() > 1)
                                            ? toks.get(1)
                                            : "" });
                //              System.out.println(Utils.convertPattern(pattern));
            }

            Collections.sort(patterns, new Comparator() {
                public int compare(Object o1, Object o2) {
                    String[] t1 = (String[]) o1;
                    String[] t2 = (String[]) o2;

                    return t2[0].length() - t1[0].length();
                }
            });
            //      for(String[]t:patterns)System.err.println(t[0]);
        }

        /**
         *
         * @param ctx _more_
         * @param row _more_
         * @param s _more_
         *  @return _more_
         */
        String change(TextReader ctx, Row row, String s) {
            String os = s;
            //      System.out.println("change:" + s);
            for (String[] tuple : patterns) {
                String pattern = tuple[0];
                String value   = tuple[1];
                if (pattern.startsWith("one:")) {
                    pattern = pattern.substring(4);
                    s       = s.replaceFirst(pattern, value);
                } else {
                    s = s.replaceAll(pattern, value);
                }
                //              System.out.println("\tchange:" + s);
                //              if(!os.equals(s)) break;
            }

            //      if(os.equals(s))System.out.println("\tno changes:" + os +" s:" + s);
            //      else System.out.println("\tchanged:" + os +" s:" + s);          
            return s;
        }


    }




    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Jan 16, '15
     * @author         Enter your name here...
     */
    public static class ColumnChanger extends Changer {


        /**
         *
         * @param ctx _more_
         * @param cols _more_
         * @param pattern _more_
         * @param value _more_
         */
        public ColumnChanger(TextReader ctx, List<String> cols,
                             String pattern, String value) {
            super(ctx, cols, pattern, value);
        }


        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            //Don't process the first row
            if (rowCnt++ == 0) {
                if ( !ctx.getAllData()) {
                    return row;
                }
            }
            List<Integer> indices = getIndices(ctx);
            for (Integer idx : indices) {
                int index = idx.intValue();
                if ((index >= 0) && (index < row.size())) {
                    String s = row.getString(index).trim();
                    s = change(ctx, row, s);
                    row.set(index, s);
                }
            }

            return row;
        }

    }


    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Jan 16, '15
     * @author         Enter your name here...
     */
    public static class ColumnReplacer extends Converter {

        /**  */
        String with;


        /**
         *
         * @param ctx _more_
         * @param cols _more_
         * @param with _more_
         */
        public ColumnReplacer(TextReader ctx, List<String> cols,
                              String with) {
            super(cols);
            this.with = with;
        }


        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            //Don't process the first row
            if (rowCnt++ == 0) {
                if ( !ctx.getAllData()) {
                    return row;
                }
            }
            List<Integer> indices = getIndices(ctx);
            for (Integer idx : indices) {
                int index = idx.intValue();
                if ((index >= 0) && (index < row.size())) {
                    String s = row.getString(index).trim();
                    s = with.replace("{value}", s);
                    row.set(index, s);
                }
            }

            return row;
        }

    }




    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Jan 16, '15
     * @author         Enter your name here...
     */
    public static class Ascii extends Converter {

        /** _more_ */
        private String value;


        /**
         * @param cols _more_
         * @param value _more_
         */
        public Ascii(List<String> cols, String value) {
            super(cols);
            this.value = value;
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            List<Integer> indices = getIndices(ctx);
            for (Integer idx : indices) {
                int index = idx.intValue();
                if ((index >= 0) && (index < row.size())) {
                    String s = row.getString(index).trim();
                    s = s.replaceAll("[^\\x00-\\x7F]", value);
                    row.set(index, s);
                }
            }

            return row;
        }

    }


    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Feb 12, '21
     * @author         Enter your name here...
     */
    public static class Cropper extends Converter {

        /** _more_ */
        private List<String> patterns;

        /**
         * @param cols _more_
         * @param patterns _more_
         */
        public Cropper(List<String> cols, List<String> patterns) {
            super(cols);
            this.patterns = new ArrayList<String>();
            for (String s : patterns) {
                this.patterns.add(s.replaceAll("_comma_", ","));
            }
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            //Don't process the first row
            if (rowCnt++ == 0) {
                if ( !ctx.getAllData()) {
                    return row;
                }
            }
            List<Integer> indices = getIndices(ctx);
            for (Integer idx : indices) {
                if ((idx >= 0) && (idx < row.size())) {
                    String s = row.getString(idx).trim();
                    for (String pattern : patterns) {
                        s = Utils.prune(s, pattern);
                    }
                    row.set(idx, s);
                } else {
                    //              System.out.println("bad idx:" + idx +" " + row.size());
                }
            }

            return row;
        }

    }





    /**
     * Class description
     *
     *
     * @version        $version$, Sat, Mar 14, '20
     * @author         Enter your name here...
     */
    public static class ColumnEndsWith extends Converter {

        /** _more_ */
        private String value;

        /**
         * @param cols _more_
         * @param value _more_
         */
        public ColumnEndsWith(List<String> cols, String value) {
            super(cols);
            this.value = value;
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            //Don't process the first row
            if (rowCnt++ == 0) {
                if ( !ctx.getAllData()) {
                    return row;
                }
            }
            List<Integer> indices = getIndices(ctx);
            for (Integer idx : indices) {
                int index = idx.intValue();
                if ((index >= 0) && (index < row.size())) {
                    String s = row.getString(index).trim();
                    if ( !s.endsWith(value)) {
                        s = s + value;
                    }
                    row.set(index, s);
                }
            }

            return row;
        }

    }






    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Sep 5, '19
     * @author         Enter your name here...
     */
    public static class ColumnExtracter extends Converter {

        /* */

        /** _more_ */
        private int col = -1;

        /**  */
        private String scol;

        /* */

        /** _more_ */
        private String pattern;

        /* */

        /** _more_ */
        private String replace;

        /* */

        /** _more_ */
        private String name;

        /**
         * @param col _more_
         * @param pattern _more_
         * @param replace _more_
         * @param name _more_
         */
        public ColumnExtracter(String col, String pattern, String replace,
                               String name) {
            this.scol    = col;
            this.pattern = pattern;
            this.replace = replace;
            this.name    = name;
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            //Don't process the first row
            if (rowCnt++ == 0) {
                this.col = getColumnIndex(ctx, scol);
                add(ctx, row, name);

                return row;
            }
            String value    = row.getString(col);
            String newValue = StringUtil.findPattern(value, pattern);
            //      System.err.println("value:" + value);
            //      System.err.println("pattern:" + pattern);
            //      System.err.println("NV:" + newValue);
            if (newValue == null) {
                newValue = "";
            }
            add(ctx, row, newValue);
            if ( !replace.equals("none")) {
                value = value.replaceAll(pattern, replace);
            }
            row.set(col, value);

            return row;
        }

    }

    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Nov 4, '21
     * @author         Enter your name here...
     */
    public static class UrlArg extends Converter {

        /** _more_ */
        private String name;

        /**  */
        private int index = -1;

        /**
         * @param col _more_
         * @param name _more_
         */
        public UrlArg(String col, String name) {
            super(col);
            this.name = name;
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            //Don't process the first row
            if (rowCnt++ == 0) {
                row.add(name);

                return row;
            }
            if (index == -1) {
                index = getIndex(ctx);
            }
            try {
                String arg = Utils.getUrlArg(row.getString(index), name);
                if (arg == null) {
                    arg = "";
                }
                row.add(arg);
            } catch (Exception exc) {
                row.add("");
            }

            return row;
        }

    }


    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Nov 4, '21
     * @author         Enter your name here...
     */
    public static class UrlEncode extends Converter {



        /**
         * @param name _more_
         */
        public UrlEncode(List<String> cols) {
            super(cols);
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
		for(int idx:getIndices(ctx)) {
		    row.add(row.getString(idx)+" encoded");
		}
                return row;
            }
	    for(int idx:getIndices(ctx)) {
		try {
		    row.add(java.net.URLEncoder.encode(row.getString(idx), "UTF-8"));
		} catch(Exception exc) {
		    throw new RuntimeException(exc);
		}
	    }
            return row;
        }
    }


    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Nov 4, '21
     * @author         Enter your name here...
     */
    public static class UrlDecode extends Converter {



        /**
         * @param name _more_
         */
        public UrlDecode(List<String> cols) {
            super(cols);
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
		for(int idx:getIndices(ctx)) {
		    row.add(row.getString(idx)+" decoded");
		}
                return row;
            }
	    for(int idx:getIndices(ctx)) {
		try {
		    row.add(java.net.URLDecoder.decode(row.getString(idx), "UTF-8"));
		} catch(Exception exc) {
		    throw new RuntimeException(exc);
		}
	    }
            return row;
        }
    }
    


    /**
     * Class description
     *
     *
     * @version        $version$, Wed, Mar 4, '20
     * @author         Enter your name here...
     */
    public static class Truncater extends Converter {


        /** _more_ */
        private int col;

        /** _more_ */
        private int length;

        /** _more_ */
        private String suffix;


        /**
         * @param col _more_
         * @param length _more_
         * @param suffix _more_
         */
        public Truncater(int col, int length, String suffix) {
            this.col    = col;
            this.length = length;
            this.suffix = suffix;
        }


        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            String value = row.get(col).toString();
            if (value.length() > length) {
                value = value.substring(0, length - 1) + suffix;
            }
            row.set(col, value);

            return row;
        }

    }





    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Mar 22, '19
     * @author         Enter your name here...
     */
    public static class RowChanger extends Changer {

        /** _more_ */
        private HashSet<Integer> rows;

        /**
         *
         * @param ctx _more_
         * @param rowList _more_
         * @param cols _more_
         * @param pattern _more_
         * @param value _more_
         */
        public RowChanger(TextReader ctx, List<Integer> rowList,
                          List<String> cols, String pattern, String value) {

            super(ctx, cols, pattern, value);
            rows = new HashSet<Integer>();
            for (int row : rowList) {
                rows.add(row);
            }
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            //Don't process the first row
            if ( !rows.contains(rowCnt++)) {
                if ( !ctx.getAllData()) {
                    return row;
                }
            }
            List<Integer> indices = getIndices(ctx);
            for (Integer idx : indices) {
                int    index = idx.intValue();
                String s     = row.getString(index);
                s = change(ctx, row, s);
                row.set(index, s);
            }

            return row;
        }

    }




    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Mar 22, '19
     * @author         Enter your name here...
     */
    public static class Shifter extends Converter {

        /** _more_ */
        private HashSet<Integer> rows;

        /** _more_ */
        private int column;

        /** _more_ */
        private int count;

        /**
         * @param rowList _more_
         * @param column _more_
         * @param count _more_
         */
        public Shifter(List<Integer> rowList, int column, int count) {
            rows = new HashSet<Integer>();
            for (int row : rowList) {
                rows.add(row);
            }
            this.column = column;
            this.count  = count;
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if ( !rows.contains(rowCnt++)) {
                return row;
            }
            //a,b,c,d,e
            //a,b,c,d,e,X
            //a,b,c,d,e,X

            List values    = row.getValues();
            List newValues = new ArrayList();
            for (int i = 0; i < values.size(); i++) {
                newValues.add(values.get(i));
            }
            for (int i = 0; i < count; i++) {
                newValues.add(column, "");
            }

            return new Row(newValues);
        }

    }



    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Mar 22, '19
     * @author         Enter your name here...
     */
    public static class RowMerger extends Converter {

        /* */

        /** _more_ */
        HashSet<Integer> rows = new HashSet<Integer>();

        /* */

        /** _more_ */
        private String delimiter;

        /* */

        /** _more_ */
        private String close;

        /* */

        /** _more_ */
        private Row firstRow;

        /**
         * @param rows _more_
         * @param delimiter _more_
         * @param close _more_
         */
        public RowMerger(List<Integer> rows, String delimiter, String close) {
            this.delimiter = delimiter;
            this.close     = close;
            for (int i = 0; i < rows.size(); i++) {
                this.rows.add(rows.get(i));
            }
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rows.size() == 0) {
                return row;
            }
            int rowNumber = rowCnt++;
            if ( !rows.contains(rowNumber)) {
                return row;
            }
            rows.remove(rowNumber);
            if (firstRow == null) {
                firstRow = row;

                return null;
            }
            for (int i = 0; i < row.size(); i++) {
                String s  = (i < row.size())
                            ? row.getString(i)
                            : "";
                String ss = (i < firstRow.size())
                            ? firstRow.getString(i)
                            : "";
                ss = ss + delimiter + s;
                firstRow.set(i, ss);
            }
            if (rows.size() == 0) {
                for (int i = 0; i < firstRow.size(); i++) {
                    firstRow.set(i, firstRow.getString(i) + close);
                }
                row      = firstRow;
                firstRow = null;

                return row;
            }

            return null;
        }

    }



    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Jan 16, '15
     * @author         Enter your name here...
     */
    public static class ColumnDebugger extends Converter {

        /* */

        /** _more_ */
        private String pattern;


        /**
         * @param cols _more_
         * @param pattern _more_
         */
        public ColumnDebugger(List<String> cols, String pattern) {
            super(cols);
            this.pattern = pattern;
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            //Don't process the first row
            if (rowCnt++ == 0) {
                if ( !ctx.getAllData()) {
                    return row;
                }
            }
            List<Integer> indices = getIndices(ctx);
            for (Integer idx : indices) {
                int index = idx.intValue();
                if ((index >= 0) && (index < row.size())) {
                    String s = row.getString(index);
                    if ((pattern.length() > 0) && (s.indexOf(pattern) < 0)) {
                        continue;
                    }
                    System.err.println("column: " + idx + "=" + s);
                }
            }

            return row;
        }

    }




    /**
     * Class description
     *
     *
     * @version        $version$, Sat, Feb 10, '18
     * @author         Enter your name here...
     */
    public static class ColumnMapper extends Converter {

        /* */

        /** _more_ */
        private String name;

        /* */

        /** _more_ */
        private Hashtable<String, String> map = new Hashtable();


        /**
         * @param cols _more_
         * @param name _more_
         * @param toks _more_
         */
        public ColumnMapper(List<String> cols, String name,
                            List<String> toks) {
            super(cols);
            this.name = name;
            this.map  = map;
            for (int i = 0; i < toks.size(); i += 2) {
                map.put(toks.get(i), toks.get(i + 1));
            }
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                if (name.length() > 0) {
                    row.getValues().add(name);
                }

                return row;
            }
            List<Integer> indices = getIndices(ctx);
            String        na      = map.get("_na_");
            if (na == null) {
                na = "";
            }
            for (Integer idx : indices) {
                int index = idx.intValue();
                if ((index >= 0) && (index < row.size())) {
                    String s        = row.getString(index);
                    String newValue = map.get(s);
                    if (name.length() > 0) {
                        if (newValue != null) {
                            row.getValues().add(newValue);
                        } else {
                            row.getValues().add(na);
                        }
                    } else {
                        if (newValue != null) {
                            row.set(index, newValue);
                        }
                    }
                }
            }

            return row;
        }

    }





    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Jan 16, '15
     * @author         Enter your name here...
     */
    public static class ColumnSplitter extends Converter {


        /* */

        /** _more_ */
        private String delimiter;

        /* */

        /** _more_ */
        private List<String> names;

        /**
         * @param col _more_
         * @param delimiter _more_
         * @param names _more_
         */
        public ColumnSplitter(String col, String delimiter,
                              List<String> names) {
            super(col);
            this.delimiter = delimiter;
            this.names     = names;
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            int index = getIndex(ctx);
            int cnt   = 0;
            if (rowCnt++ == 0) {
                for (String name : names) {
                    row.insert(index + 1 + (cnt++), name);
                }

                return row;
            }
            if ((index < 0) || (index >= row.size())) {
                return row;
            }
            //            row.remove(index);
            int          colOffset = 0;
            List<String> toks      = Utils.split(row.get(index), delimiter);
            while (toks.size() < names.size()) {
                toks.add("");
            }
            for (String tok : toks) {
                row.insert(index + 1 + (colOffset++), tok);
            }


            return row;
        }

    }



    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Jan 16, '15
     * @author         Enter your name here...
     */
    public static class ColumnConcatter extends Converter {

        /* */

        /** _more_ */
        private String delimiter;

        /* */

        /** _more_ */
        private String name;

        /* */

        /** _more_ */
        private boolean inPlace;

        /**
         * @param indices _more_
         * @param delimiter _more_
         * @param name _more_
         * @param inPlace _more_
         */
        public ColumnConcatter(List<String> indices, String delimiter,
                               String name, boolean inPlace) {
            super(indices);
            this.delimiter = delimiter;
            this.name      = name;
            this.inPlace   = inPlace;
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            List<Integer> indices = getIndices(ctx);
            if (rowCnt++ == 0) {
                if (name.length() > 0) {
                    if (inPlace) {
                        row = filterValues(ctx, row);
                        row.insert(indices.get(0), name);
                    } else {
                        row.getValues().add(name);
                    }

                    return row;
                }

                return null;
            }

            StringBuilder sb  = new StringBuilder();
            int           cnt = 0;
            for (Integer idx : indices) {
                int i = idx.intValue();
                if (cnt++ > 0) {
                    sb.append(delimiter);
                }
                if (i >= row.size()) {
                    sb.append("");
                } else {
                    sb.append(row.getString(i));
                }
            }

            //            if(rowCnt<5)
            //                System.err.println("combine:" + sb);
            if (inPlace) {
                row = filterValues(ctx, row);
                row.insert(indices.get(0), sb.toString());
            } else {
                row.getValues().add(sb.toString());
            }



            return row;
        }

    }


    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Jan 16, '15
     * @author         Enter your name here...
     */
    public static class ColumnMerger extends Converter {


        /** _more_ */
        private String name;

        /**  */
        private List<String> what;

        /** _more_ */
        private boolean inPlace;

        /**
         * @param indices _more_
         * @param name _more_
         * @param what _more_
         */
        public ColumnMerger(List<String> indices, String name, String what) {
            super(indices);
            this.what = Utils.split(what, ",", true, true);
            this.name = name;
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            List<Integer> indices = getIndices(ctx);
            if (rowCnt++ == 0) {
                for (String what : this.what) {
                    row.getValues().add(name + " " + what);
                }

                return row;
            }


            double min   = Double.POSITIVE_INFINITY;
            double max   = Double.NEGATIVE_INFINITY;

            double total = 0;
            int    count = 0;
            for (Integer idx : indices) {
                int    i = idx.intValue();
                double v = Double.parseDouble(row.getString(i));
                if (Double.isNaN(v)) {
                    continue;
                }
                total += v;
                count++;
                min = Math.min(min, v);
                max = Math.max(max, v);
            }

            for (String op : what) {
                if (op.equals(OPERAND_SUM)) {
                    row.add(total);
                } else if (op.equals(OPERAND_MIN)) {
                    row.add((min == Double.POSITIVE_INFINITY)
                            ? "NaN"
                            : Double.toString(min));
                } else if (op.equals(OPERAND_MAX)) {
                    row.add((max == Double.NEGATIVE_INFINITY)
                            ? "NaN"
                            : Double.toString(max));
                } else if (op.equals(OPERAND_AVERAGE)) {
                    if (count == 0) {
                        row.add("NaN");
                    } else {
                        row.add(total / count);
                    }
                } else {
                    fatal(ctx, "Unknown histogram operator:" + op);
                }
            }

            return row;
        }

    }






    /* */

    /** _more_ */
    private static Properties genderProperties;

    /**
     * Class description
     *
     *
     * @version        $version$, Mon, Jul 29, '19
     * @author         Enter your name here...
     */
    public static class Genderizer extends Converter {

        /* */

        /** _more_ */
        private boolean doneHeader = false;


        /**
         * @param col _more_
         */
        public Genderizer(String col) {
            super(col);
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            int  column = getIndex(ctx);
            List values = row.getValues();
            if ( !doneHeader) {
                values.add("Gender");
                doneHeader = true;

                return row;
            }
            String value = values.get(column).toString().toLowerCase().trim();
            int    index = value.indexOf(",");
            if (index >= 0) {
                value = value.substring(index + 1).trim();
            }
            index = value.indexOf(" ");
            if (index >= 0) {
                value = value.substring(0, index).trim();
            }
            if (genderProperties == null) {
                genderProperties = new Properties();
                try {
                    InputStream inputStream =
                        Utils.getInputStream(
                            "/org/ramadda/util/text/gender.properties",
                            getClass());
                    genderProperties.load(inputStream);
                } catch (Exception exc) {
                    throw new RuntimeException(exc);
                }
            }
            String gender = (String) genderProperties.get(value);
            if (gender == null) {
                gender = "na";
            } else if ( !(gender.equals("m") || gender.equals("f"))) {
                gender = "na";
            }
            values.add(gender);

            return row;
        }

    }


    /**
     * Class description
     *
     *
     * @version        $version$, Wed, Jan 17, '18
     * @author         Enter your name here...
     */
    public static class Denormalizer extends Converter {

        /* */

        /** _more_ */
        private Hashtable map = new Hashtable();

        /* */

        /** _more_ */
        int destCol;

        /* */

        /** _more_ */
        String newColName;


        /* */

        /** _more_ */
        String mode;

        /* */

        /** _more_ */
        boolean doDelete;

        /**
         * @param mapFile _more_
         * @param col1 _more_
         * @param col2 _more_
         * @param col _more_
         * @param newName _more_
         * @param mode _more_
         */
        public Denormalizer(String mapFile, int col1, int col2, int col,
                            String newName, String mode) {
            try {
                makeMap(mapFile, col1, col2);
                this.destCol    = col;
                this.newColName = newName;
                this.mode       = mode;
                this.doDelete   = mode.endsWith("delete");
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }
        }


        /**
         * @param filename _more_
         * @param col1 _more_
         * @param col2 _more_
         * @throws Exception _more_
         */
        private void makeMap(String filename, int col1, int col2)
                throws Exception {
            BufferedReader br = new BufferedReader(
                                    new InputStreamReader(
                                        getInputStream(filename)));

            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.length() == 0) {
                    continue;
                }
                if (line.startsWith("#")) {
                    continue;
                }
                List<String> toks = Utils.tokenizeColumns(line, ",");
                map.put(toks.get(col1), toks.get(col2).replaceAll("\"", ""));
            }
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            List   values   = row.getValues();
            String newValue = null;
            if (rowCnt++ == 0) {
                if (newColName.length() > 1) {
                    newValue = newColName;
                }
            } else {
                Object key = values.get(destCol);
                newValue = (String) map.get(key);
                if (doDelete && (newValue == null)) {
                    return null;
                }
            }

            if ((newValue == null) && mode.startsWith("add")) {
                newValue = "";
            }

            if (newValue != null) {
                //                if(newValue.indexOf(",")>=0) newValue = "\"" + newValue+"\"";
                if (mode.startsWith("replace")) {
                    values.set(destCol, newValue);
                } else {
                    values.add(destCol + 1, newValue);
                }
            }

            return row;
        }

    }




    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Jan 16, '15
     * @author         Enter your name here...
     */
    public static class ColumnDeleter extends Converter {



        /**
         * @param indices _more_
         */
        public ColumnDeleter(List<String> indices) {
            super(indices);
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            row = filterValues(ctx, row);

            return row;
        }
    }



    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Dec 27, '18
     * @author         Enter your name here...
     */
    public static class ColumnScaler extends Converter {

        /* */

        /** _more_ */
        private double delta1;

        /* */

        /** _more_ */
        private double delta2;

        /* */

        /** _more_ */
        private double scale;


        /**
         * @param cols _more_
         * @param delta1 _more_
         * @param scale _more_
         * @param delta2 _more_
         */
        public ColumnScaler(List<String> cols, double delta1, double scale,
                            double delta2) {
            super(cols);
            this.delta1 = delta1;
            this.delta2 = delta2;
            this.scale  = scale;
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            List<Integer> indices = getIndices(ctx);
            for (int index : indices) {
                if ((index < 0) || (index >= row.size())) {
                    continue;
                }
                try {
                    double value =
                        Double.parseDouble(row.get(index).toString());
                    row.set(index,
                            new Double((value + delta1) * scale
                                       + delta2).toString());
                } catch (NumberFormatException nfe) {}
            }

            return row;
        }

    }


    /**
     * Class description
     *
     *
     * @version        $version$, Mon, Jul 29, '19
     * @author         Enter your name here...
     */
    public static class Decimals extends Converter {

        /* */

        /** _more_ */
        private int tens;

        /**  */
        private int decimals;

        /**
         * @param cols _more_
         * @param decimals _more_
         */
        public Decimals(List<String> cols, int decimals) {
            super(cols);
            this.decimals = decimals;
            this.tens     = (int) Math.pow(10, decimals);
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            List<Integer> indices = getIndices(ctx);
            for (int i = 0; i < indices.size(); i++) {
                try {
                    int index = indices.get(i);
                    if ((index < 0) || (index >= row.size())) {
                        continue;
                    }
                    double value =
                        Double.parseDouble(row.get(index).toString());
                    if (decimals == 0) {
                        row.set(index, new Integer((int) value));
                    } else {
                        value = (double) Math.round(value * tens) / tens;
                        row.set(index, new Double(value));
                    }
                } catch (NumberFormatException nfe) {}
            }

            return row;
        }

    }

    /**
     * Class description
     *
     *
     * @version        $version$, Mon, Jul 29, '19
     * @author         Enter your name here...
     */
    public static class Fuzzer extends Converter {

        /**  */
        private int places;

        /**  */
        private int numRandomDigits;

        /**  */
        private int tens;

        /**
         * @param cols _more_
         * @param places _more_
         * @param numRandomDigits _more_
         */
        public Fuzzer(List<String> cols, int places, int numRandomDigits) {
            super(cols);
            this.places          = places;
            this.numRandomDigits = numRandomDigits;
            this.tens            = (int) Math.pow(10, numRandomDigits);
        }


        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            List<Integer> indices = getIndices(ctx);
            for (int i = 0; i < indices.size(); i++) {
                try {
                    int index = indices.get(i);
                    if ((index < 0) || (index >= row.size())) {
                        continue;
                    }
                    double value =
                        Double.parseDouble(row.get(index).toString());
                    if (Double.isNaN(value)) {
                        continue;
                    }
                    int    digits  = (int) (Math.random() * tens);
                    String sdigits = "" + digits;
                    if (sdigits.length() < numRandomDigits) {
                        sdigits = StringUtil.padRight(sdigits,
                                numRandomDigits, "0");
                        digits = Integer.parseInt(sdigits);
                    }

                    if (places <= 0) {
                        String       svalue = "" + value;
                        List<String> toks   = Utils.splitUpTo(svalue, ".", 2);
                        String       v      = toks.get(0);
                        String       d      = ((toks.size() > 1)
                                ? toks.get(1)
                                : "");
                        d = StringUtil.padRight(d, -places, "0");
                        if (d.length() > -places) {
                            d = d.substring(0, -places);
                        } else {
                            d = StringUtil.padRight("", -places, "0");
                        }
                        String newValue = v + "." + d + digits;
                        row.set(index, newValue);
                        //                      System.err.println("value:" + value+" left:" +v +" right:" + d+ " result:" +newValue);
                    } else {
                        int    ivalue = (int) value;
                        double d      = Math.pow(10, places);
                        ivalue = (int) (d * ((int) (ivalue / d)));

                        if (ivalue == 0) {
                            row.set(index, ivalue + "." + digits);
                        } else {
                            row.set(index, "" + (ivalue + digits));
                        }
                        //                      System.err.println("v:" + value+" I:" +ivalue +" result:" + row.get(index));
                    }
                } catch (NumberFormatException nfe) {}
            }

            return row;
        }

    }



    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Nov 4, '21
     * @author         Enter your name here...
     */
    public static class Ceil extends Converter {

        /** _more_ */
        private double value;

        /**
         * @param cols _more_
         * @param value _more_
         */
        public Ceil(List<String> cols, double value) {
            super(cols);
            this.value = value;
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                return row;
            }
            List<Integer> indices = getIndices(ctx);
            for (int index : indices) {
                try {
                    if ((index < 0) || (index >= row.size())) {
                        continue;
                    }
                    double value =
                        Double.parseDouble(row.get(index).toString());
                    value = (double) Math.min(value, this.value);
                    row.set(index, new Double(value));
                } catch (NumberFormatException nfe) {}
            }

            return row;
        }
    }


    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Nov 4, '21
     * @author         Enter your name here...
     */
    public static class Floor extends Converter {

        /** _more_ */
        private double value;

        /**
         * @param cols _more_
         * @param value _more_
         */
        public Floor(List<String> cols, double value) {
            super(cols);
            this.value = value;
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                return row;
            }
            List<Integer> indices = getIndices(ctx);
            for (int index : indices) {
                try {
                    if ((index < 0) || (index >= row.size())) {
                        continue;
                    }
                    double value =
                        Double.parseDouble(row.get(index).toString());
                    value = (double) Math.max(value, this.value);
                    row.set(index, new Double(value));
                } catch (NumberFormatException nfe) {}
            }

            return row;
        }
    }


    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Nov 29, '18
     * @author         Enter your name here...
     */
    public static class ColumnCopier extends Converter {

        /* */

        /** _more_ */
        private String name;


        /**
         *
         * @param col _more_
         * @param name _more_
         */
        public ColumnCopier(String col, String name) {
            super(col);
            this.name = name;
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            int index = getIndex(ctx);
            if ((index < 0) || (index >= row.size())) {
                return row;
            }
            if (rowCnt++ == 0) {
                row.insert(index + 1, name);
            } else {
                row.insert(index + 1, row.getValues().get(index));
            }

            return row;
        }

    }




    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Jan 16, '15
     * @author         Enter your name here...
     */
    public static class ColumnNewer extends Converter {

        /* */

        /** _more_ */
        private String delimiter;

        /**  */
        private String name;

        /**
         * @param indices _more_
         * @param delimiter _more_
         * @param name _more_
         */
        public ColumnNewer(List<String> indices, String delimiter,
                           String name) {
            super(indices);
            this.delimiter = delimiter;
            this.name      = name;
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                row.add(name);

                return row;
            }
            List<Integer> indices = getIndices(ctx);
            StringBuilder sb      = new StringBuilder();
            int           cnt     = 0;
            for (Integer idx : indices) {
                int index = idx.intValue();
                if ((index < 0) || (index >= row.size())) {
                    continue;
                }
                String s = (String) row.getValues().get(index);
                if (cnt++ > 0) {
                    sb.append(delimiter);
                }
                sb.append(s);
            }
            row.getValues().add(sb.toString());

            return row;
        }
    }


    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Jan 16, '15
     * @author         Enter your name here...
     */
    public static class RowConcat extends Converter {

        /** _more_ */
        private int num;
	private List<Row> rows = new ArrayList<Row>();

        /**
         */
        public RowConcat(int num) {
	    this.num = num;
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
		Row newRow = new Row();
		for(int i=0;i<num;i++) {
		    for(Object o: row.getValues()) {
			newRow.add(o);
		    }
		}
		return newRow;
            }
	    rows.add(row);
	    if(rows.size()>=num) {
		Row newRow = makeRow();
		rows = new ArrayList<Row>();
		return newRow;
	    }
            return null;
	}

        @Override
        public void finish(TextReader ctx) throws Exception {
	    if(rows.size()>0) {
		Row newRow = makeRow();
		Processor nextProcessor = getNextProcessor();
		if (nextProcessor != null) {
		    nextProcessor.handleRow(ctx,newRow);
		}
	    }
            super.finish(ctx);
	}


	private Row makeRow() {
	    Row newRow = new Row();
	    for(Row row: rows) {
		for(Object o: row.getValues())
		    newRow.add(o);
	    }
	    return newRow;
	}

    }
    


    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Jan 16, '15
     * @author         Enter your name here...
     */
    public static class ColumnMathOperator extends Converter {

        /* */

        /** _more_ */
        private String name;

        /* */

        /** _more_ */
        private String op;


        /**  */
        String foo;

        /**
         * @param indices _more_
         * @param name _more_
         * @param op _more_
         */

        public ColumnMathOperator(List<String> indices, String name,
                                  String op) {
            super(indices);
            foo       = "" + indices;
            this.name = name;
            this.op   = op;
        }


        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            List<Integer> indices = getIndices(ctx);
            if (rowCnt++ == 0) {
                row.getValues().add(name);

                return row;
            }
            double value = 0;
            int    cnt   = 0;
            double total = 0;
            //      System.err.println("op:" + op +" " + foo +" indices:" + indices);
            for (Integer idx : indices) {
                int index = idx.intValue();
                if ((index < 0) || (index >= row.size())) {
                    //              System.err.println("\tSkipping:" + index);
                    continue;
                }
                String s = row.getValues().get(index).toString();
                double v = parse(s);
                //              System.err.println("\tindex::" + index +" value:" + value);
                if ( !Double.isNaN(v)) {
                    total += v;
                }
                if (op.equals("+")) {
                    value += v;
                } else {
                    if (cnt == 0) {
                        value = v;
                    } else {
                        if (op.equals("-")) {
                            value = value - v;
                        } else if (op.equals("*")) {
                            value = value * v;
                        } else if (op.equals("%")) {
                            value = value % v;			    
                        } else if (op.equals("/")) {
                            value = value / v;
                        }
                    }
                }
                cnt++;
            }
            if (op.equals(OPERAND_AVERAGE)) {
                if (cnt == 0) {
                    value = Double.NaN;
                } else {
                    value = total / cnt;
                }
            }
            //      System.err.println("\tfinal value:" + value);
            row.getValues().add(value + "");
            return row;
        }


    }



    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Jan 16, '15
     * @author         Enter your name here...
     */
    public static class CompareNumber extends Converter {


        /** _more_ */
        private String op;

	private String scol1;
	private String scol2;
	private int col1;
	private int col2;	


        /**
         * @param indices _more_
         * @param name _more_
         * @param op _more_
         */
        public CompareNumber(String col1, String col2, String op) {
	    this.scol1=  col1;
	    this.scol2=  col2;	    
            this.op   = op;
        }


        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
		col1 = getIndex(ctx, scol1);
		col2 = getIndex(ctx, scol2);		
		row.add(row.getString(col1) +" " + op +" " + row.getString(col2));
                return row;
            }
	    if(col1>= row.size()) return row;
	    if(col2>= row.size()) return row;	    
            double value1 = row.getDouble(col1);
            double value2 = row.getDouble(col2);	    
	    boolean value = true;
	    if(op.equals("<")) value  = value1 < value2;
	    else if(op.equals("<=")) value  = value1 <= value2;
	    else if(op.equals("=")) value  = value1 == value2;
	    else if(op.equals("!=")) value  = value1 != value2;	    
	    else if(op.equals(">")) value  = value1 > value2;
	    else if(op.equals(">=")) value  = value1 >= value2;    	    
	    else fatal(ctx,"Unknown operator:" + op);
            row.getValues().add(value + "");
            return row;
        }

    }
    



    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Jan 16, '15
     * @author         Enter your name here...
     */
    public static class Delta extends Converter {

        /** _more_ */
        List<String> keys;


        /** _more_ */
        private Hashtable<String, Row> prevRows = new Hashtable<String,
                                                      Row>();

        /** _more_ */
        List<Integer> indices;

        /** _more_ */
        List<Integer> keyindices;



        /**
         * @param keys _more_
         * @param indices _more_
         */
        public Delta(List<String> keys, List<String> indices) {
            super(indices);
            this.keys = keys;
        }


        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (indices == null) {
                indices    = getIndices(ctx);
                keyindices = getIndices(ctx, keys);
            }
            if (rowCnt++ == 0) {
                for (Integer idx : indices) {
                    int index = idx.intValue();
                    row.getValues().add("Difference " + row.get(index));
                }

                return row;
            }
            String key = "";
            for (Integer idx : keyindices) {
                int index = idx.intValue();
                if ((index < 0) || (index >= row.size())) {
                    continue;
                }
                key += row.get(index).toString() + "_";
            }
            Row prevRow = prevRows.get(key);
            if (prevRow == null) {
                prevRow = row;
                for (int i : indices) {
                    prevRow.add("0");
                }
                prevRows.put(key, prevRow);

                return row;
            }
            prevRows.put(key, row);
            for (Integer idx : indices) {
                int index = idx.intValue();
                if ((index < 0) || (index >= row.size())) {
                    continue;
                }
                double v1 = parse(prevRow.get(index).toString());
                double v2 = parse(row.get(index).toString());
                row.add((v2 - v1) + "");
            }

            return row;
        }

    }

    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Jan 16, '15
     * @author         Enter your name here...
     */
    public static class Mercator extends Converter {


        /**
         * @param indices _more_
         */
        public Mercator(List<String> indices) {
            super(indices);

        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                row.getValues().add("Latitude");
                row.getValues().add("Longitude");

                return row;
            }
            List<Integer> indices = getIndices(ctx);
            double x =
                new Double(row.getValues().get(indices.get(0)).toString());
            double y =
                new Double(row.getValues().get(indices.get(1)).toString());
            double rMajor = 6378137;  //Equatorial Radius, WGS84
            double shift  = Math.PI * rMajor;
            double lon    = x / shift * 180.0;
            double lat    = y / shift * 180.0;
            lat = 180 / Math.PI
                  * (2 * Math.atan(Math.exp(lat * Math.PI / 180.0))
                     - Math.PI / 2.0);

            row.getValues().add(lat);
            row.getValues().add(lon);

            return row;
        }

    }




    /**
     * Class description
     *
     *
     * @version        $version$, Wed, Feb 20, '19
     * @author         Enter your name here...
     */
    public static class ColumnRounder extends Converter {



        /**
         * @param indices _more_
         */
        public ColumnRounder(List<String> indices) {
            super(indices);
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                if ( !ctx.getAllData()) {
                    return row;
                }
            }
            List<Integer> indices = getIndices(ctx);
            for (Integer idx : indices) {
                int index = idx.intValue();
                if ((index < 0) || (index >= row.size())) {
                    continue;
                }
                String s = row.getString(index);
                double v = (s.length() == 0)
                           ? 0
                           : Double.parseDouble(s.replaceAll(",", ""));
                row.set(index, "" + ((int) Math.round(v)));
            }

            return row;
        }

    }

    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Nov 4, '21
     * @author         Enter your name here...
     */
    public static class Bytes extends Converter {

        /**  */
        private String unit;

        /**
         * @param unit _more_
         * @param indices _more_
         */
        public Bytes(String unit, List<String> indices) {
            super(indices);
            this.unit = unit;
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            List<Integer> indices = getIndices(ctx);
            if (rowCnt++ == 0) {
                for (int i : indices) {
                    row.add("Size " + row.get(i));
                }

                return row;
            }
            for (Integer idx : indices) {
                int    index = idx.intValue();
                String s     = (String) row.getValues().get(index);
                double v     = 0;

                s = s.replaceAll(" ", "").toLowerCase();
                String u = StringUtil.findPattern(s, "([^0-9\\.\\-]+)");
                if (u == null) {
                    v = Double.parseDouble(s);
                } else {
                    v = Double.parseDouble(s.replaceAll(u, ""));
                    v = v * getMultiplier(u);
                }
                row.add(v);

            }

            return row;
        }

        /**
         * @param u _more_
         *  @return _more_
         */
        private double getMultiplier(String u) {
            if (unit.equals("binary")) {
                return getMultiplier(u, 1024);
            }

            return getMultiplier(u, 1000);
        }

        /**
         * @param u _more_
         * @param base _more_
         *  @return _more_
         */
        private double getMultiplier(String u, double base) {
            if (u.equals("kb")) {
                return base;
            }
            if (u.equals("mb")) {
                return base * base;
            }
            if (u.equals("gb")) {
                return base * base * base;
            }
            if (u.equals("tb")) {
                return base * base * base * base;
            }
            if (u.equals("pb")) {
                return base * base * base * base * base;
            }

            return 1;
        }


    }


    /**
     * Class description
     *
     *
     * @version        $version$, Wed, Feb 20, '19
     * @author         Enter your name here...
     */
    public static class ColumnAbs extends Converter {

        /**
         * @param indices _more_
         */
        public ColumnAbs(List<String> indices) {
            super(indices);
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                return row;
            }
            List<Integer> indices = getIndices(ctx);
            for (Integer idx : indices) {
                int index = idx.intValue();
                if ((index < 0) || (index >= row.size())) {
                    continue;
                }
                String s = (String) row.getValues().get(index);
                double v = (s.length() == 0)
                           ? 0
                           : Double.parseDouble(s.replaceAll(",", ""));
                row.set(index, "" + Math.abs(v));
            }

            return row;
        }

    }

    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Nov 4, '21
     * @author         Enter your name here...
     */
    public static class ColumnRand extends Converter {

        /**  */
        String name;

        /**  */
        double min;

        /**  */
        double max;

        /**
         *
         * @param name _more_
         * @param min _more_
         * @param max _more_
         */
        public ColumnRand(String name, double min, double max) {
            this.name = name;
            this.min  = min;
            this.max  = max;
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                row.add(name);

                return row;
            }
            double r = Math.random();
            r = r * (max - min) + min;
            row.add("" + r);

            return row;
        }

    }





    /**
     * Class description
     *
     *
     * @version        $version$, Wed, Dec 2, '15
     * @author         Enter your name here...
     */
    public static class Case extends Converter {

        /* */

        /** _more_ */
        String action;

        /**
         * @param indices _more_
         * @param action _more_
         */
        public Case(List<String> indices, String action) {
            super(indices);
            this.action = action;
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                return row;
            }
            List<Integer> indices = getIndices(ctx);
            for (Integer idx : indices) {
                int index = idx.intValue();
                if ((index < 0) || (index >= row.size())) {
                    continue;
                }
                String s  = (String) row.getValues().get(index);
                String os = s;
                if (s == null) {
                    return row;
                }
                if (action.equals("lower")) {
                    s = s.toLowerCase();
                } else if (action.equals("upper")) {
                    s = s.toUpperCase();
                } else if (action.equals("proper")) {
                    s = Utils.nameCase(s);
                } else if (action.equals("camel")) {
                    s = Utils.upperCaseFirst(s);
                } else if (action.equals("capitalize")) {
                    if (s.length() == 1) {
                        s = s.toUpperCase();
                    } else if (s.length() > 1) {
                        s = s.substring(0, 1).toUpperCase()
                            + s.substring(1).toLowerCase();
                    }
                } else {
                    throw new IllegalArgumentException(
                        "Unknown case:" + action
                        + ". Needs to be one of lower, upper, camel, capitalize");
                }
                //              s = s.replaceAll("  +"," ");
                //              os = os.replaceAll("  +"," ");
                //              if(!s.trim().equals(os.trim())) System.out.println(os +":" + s+":");
                row.getValues().set(index, s);
            }

            return row;
        }

    }

    /**
     * Class description
     *
     *
     * @version        $version$, Wed, Dec 2, '15
     * @author         Enter your name here...
     */
    public static class NumColumns extends Converter {

        /**  */
        int number;

        /**
         * @param number _more_
         */
        public NumColumns(int number) {
            this.number = number;
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            List values = row.getValues();
            while (values.size() < number) {
                values.add("");
            }
            while (values.size() > number) {
                values.remove(values.size() - 1);
            }

            return row;
        }

    }




    /**
     * Class description
     *
     *
     * @version        $version$, Wed, Dec 2, '15
     * @author         Enter your name here...
     */
    public static class Trim extends Converter {

        /**
         * @param indices _more_
         */
        public Trim(List<String> indices) {
            super(indices);
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            List<Integer> indices = getIndices(ctx);
            for (Integer idx : indices) {
                int index = idx.intValue();
                if ((index < 0) || (index >= row.size())) {
                    continue;
                }
                Object o = row.getValues().get(index);
                if (o == null) {
                    continue;
                }
                String s = o.toString().trim();
                row.set(index, s);
            }

            return row;
        }

    }

    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Nov 4, '21
     * @author         Enter your name here...
     */
    public static class PadLeftRight extends Converter {

        /**  */
        private int length;

        /**  */
        private String c;

        /**  */
        private boolean left;

        /**
         * @param left _more_
         * @param indices _more_
         * @param c _more_
         * @param length _more_
         */
        public PadLeftRight(boolean left, List<String> indices, String c,
                            int length) {
            super(indices);
            this.left   = left;
            this.c      = c;
            this.length = length;
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                return row;
            }
            List<Integer> indices = getIndices(ctx);
            for (Integer idx : indices) {
                int index = idx.intValue();
                if ((index < 0) || (index >= row.size())) {
                    continue;
                }
                String s = (String) row.getValues().get(index);
                if (s == null) {
                    continue;
                }
                while (s.length() < length) {
                    if (left) {
                        s = c + s;
                    } else {
                        s = s + c;
                    }
                }
                row.set(index, s);
            }

            return row;
        }

    }



    /**
     * Class description
     *
     *
     * @version        $version$, Wed, Dec 2, '15
     * @author         Enter your name here...
     */
    public static class TrimQuotes extends Converter {

        /**
         * @param indices _more_
         */
        public TrimQuotes(List<String> indices) {
            super(indices);
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            List<Integer> indices = getIndices(ctx);
            System.err.println("process:" + row);
            for (Integer idx : indices) {
                int index = idx.intValue();
                if ((index < 0) || (index >= row.size())) {
                    continue;
                }
                String s = (String) row.getValues().get(index);
                if (s == null) {
                    continue;
                }
                if (s.startsWith("\"")) {
                    s = s.substring(1);
                }
                if (s.endsWith("\"")) {
                    s = s.substring(0, s.length() - 2);
                }
                row.set(index, s);
            }

            return row;
        }

    }




    /**
     * Class description
     *
     *
     * @version        $version$, Wed, Mar 24, '21
     * @author         Enter your name here...
     */
    public static class StripTags extends Converter {

        /**
         * @param indices _more_
         */
        public StripTags(List<String> indices) {
            super(indices);
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                return row;
            }
            List<Integer> indices = getIndices(ctx);
            for (Integer idx : indices) {
                int index = idx.intValue();
                if ((index < 0) || (index >= row.size())) {
                    continue;
                }
                String s = (String) row.getValues().get(index);
                s = Utils.stripTags(s);
                row.set(index, s);
            }

            return row;
        }

    }


    /**
     * Class description
     *
     *
     * @version        $version$, Wed, Mar 24, '21
     * @author         Enter your name here...
     */
    public static class Decoder extends Converter {

        /**
         * @param indices _more_
         */
        public Decoder(List<String> indices) {
            super(indices);
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                return row;
            }
            List<Integer> indices = getIndices(ctx);
            for (Integer idx : indices) {
                int index = idx.intValue();
                if ((index < 0) || (index >= row.size())) {
                    continue;
                }
                String s = (String) row.getValues().get(index);
                row.set(index, HtmlUtils.entityDecode(s));
            }

            return row;
        }

    }


    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Feb 12, '21
     * @author         Enter your name here...
     */
    public static class MD extends Converter {

        /** _more_ */
        private MessageDigest md;

        /**
         *
         * @param ctx _more_
         * @param indices _more_
         * @param type _more_
         */
        public MD(TextReader ctx, List<String> indices, String type) {
            super(indices);
            try {
                type = type.trim();
                if (type.length() == 0) {
                    type = "MD5";
                }
                md = MessageDigest.getInstance(type);
            } catch (Exception exc) {
                fatal(ctx, "Creating message digest:" + type, exc);
            }
        }


        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                row.add("MD");

                return row;
            }
            List<Integer> indices = getIndices(ctx);
            for (Integer idx : indices) {
                int index = idx.intValue();
                if ((index < 0) || (index >= row.size())) {
                    continue;
                }
                String s = (String) row.getValues().get(index);
                if (s == null) {
                    s = "";
                }
                md.update(s.getBytes());
            }
            try {
                row.add(Utils.encodeMD(md.digest()));
            } catch (Exception exc) {
                fatal(ctx, "Error making message digest", exc);
            }

            return row;
        }

    }


    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Feb 12, '21
     * @author         Enter your name here...
     */
    public static class Subst extends Converter {
	private List<String>header;
	private List<String>_header;
	private String name;
	private String template;

        /**
         */
        public Subst(String name, String template) {
	    this.name = name;
	    this.template = template;
        }


        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
		header = new ArrayList<String>();
		_header = new ArrayList<String>();
		for(int i=0;i<row.size();i++) {
		    header.add(row.getString(i));
		    _header.add(row.getString(i).toLowerCase());
		}
                row.add(name);
                return row;
            }
	    String v = template;
	    for(int i=0;i<row.size();i++) {	    
		v = Utils.replaceAll(v,"${" + header.get(i) +"}", row.getString(i));
		v = Utils.replaceAll(v,"${" + _header.get(i) +"}", row.getString(i));
		v = Utils.replaceAll(v,"${" + i +"}", row.getString(i));
	    }
	    row.add(v);
            return row;
        }
    }




    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Feb 12, '21
     * @author         Enter your name here...
     */
    public static class SoundexMaker extends Converter {

        /**  */
        private Soundex soundex;

        /**
         * @param indices _more_
         */
        public SoundexMaker(List<String> indices) {
            super(indices);
            soundex = new Soundex();
        }


        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                row.add("Soundex");

                return row;
            }
            List<Integer> indices = getIndices(ctx);
            for (Integer idx : indices) {
                int index = idx.intValue();
                if ((index < 0) || (index >= row.size())) {
                    continue;
                }
                String s = (String) row.getValues().get(index);
                if (s == null) {
                    s = "";
                }
                row.add(soundex.soundex(s));
            }

            return row;
        }

    }




    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Feb 12, '21
     * @author         Enter your name here...
     */
    public static class Even extends Converter {


        /**
         * @param indices _more_
         */
        public Even(List<String> indices) {
            super(indices);
        }


        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                row.add("Even");

                return row;
            }
            String        v       = "";
            List<Integer> indices = getIndices(ctx);
            for (Integer idx : indices) {
                int index = idx.intValue();
                if ((index < 0) || (index >= row.size())) {
                    continue;
                }
                String s = (String) row.getValues().get(index);
                if (s == null) {
                    s = "";
                }
                v += s;
            }

            String  snum = StringUtil.findPattern(v, "^(\\d+)");
            boolean even = true;
            if (snum != null) {
                int num = (int) Double.parseDouble(snum);
                even = ((num % 2) == 0);
            }
            row.add(even
                    ? "even"
                    : "odd");

            return row;
        }

    }

    /**
     * Class description
     *
     *
     * @version        $version$, Wed, Dec 2, '15
     * @author         Enter your name here...
     */
    public static class ColumnInserter extends Converter {

        /**  */
        private String name;

        /** _more_ */
        private List<String> values;

        /**
         * @param col _more_
         * @param name _more_
         * @param value _more_
         */
        public ColumnInserter(String col, String name, String value) {
            super(col);
            this.values = Utils.split(value, ",", false, false);
            this.name   = name;
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            int    col = hasColumns()
                         ? getIndex(ctx)
                         : -1;
            String v   = "";
            if (rowCnt == 0) {
                v = name;
            } else if (rowCnt < values.size()) {
                v = values.get(rowCnt);
            } else {
                v = values.get(values.size() - 1);
            }
            rowCnt++;
            if ((col < 0) || (col > row.getValues().size())) {
                row.getValues().add(v);
            } else {
                row.getValues().add(col, v);
            }

            return row;
        }
    }

    /**
     * Class description
     *
     *
     * @version        $version$, Sat, Apr 9, '22
     * @author         Enter your name here...
     */
    public static class ColumnAdder extends Converter {

        /**  */
        private List<String> names;

        /** _more_ */
        private List<String> values;

        /**
         * @param names _more_
         * @param values _more_
         */
        public ColumnAdder(String names, String values) {
            this.names  = Utils.split(names, ",", false, false);
            this.values = Utils.split(values, ",", false, false);
            while (this.values.size() < this.names.size()) {
                this.values.add("");
            }
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                row.addAll(names);
            } else {
                row.addAll(values);
            }

            return row;
        }
    }





    /**
     * Class description
     *
     *
     * @version        $version$, Wed, Dec 2, '15
     * @author         Enter your name here...
     */
    public static class Generator extends Converter {

        /** _more_ */
        private String label;

        /** _more_ */
        private double start;

        /** _more_ */
        private double step;

        /** _more_ */
        private double value;

        /**
         * @param label _more_
         * @param start _more_
         * @param step _more_
         */
        public Generator(String label, double start, double step) {
            this.label = label;
            this.start = start;
            this.step  = step;
            value      = start;
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                add(ctx, row, label);

                return row;
            }
            if (value == (int) value) {
                add(ctx, row, "" + ((int) value));
            } else {
                add(ctx, row, "" + value);
            }
            value += step;

            return row;
        }
    }




    /**
     * Class description
     *
     *
     * @version        $version$, Mon, Jul 29, '19
     * @author         Enter your name here...
     */
    public static class ColumnMacro extends Converter {

        /* */

        /** _more_ */
        private Pattern pattern;

        /* */

        /** _more_ */
        private String template;

        /* */

        /** _more_ */
        private String label;

        /* */

        /** _more_ */
        private String value;

        /**
         * @param pattern _more_
         * @param template _more_
         * @param label _more_
         */
        public ColumnMacro(String pattern, String template, String label) {
            this.pattern  = Pattern.compile(pattern, Pattern.MULTILINE);
            this.template = template;
            this.label    = label;

        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if ((rowCnt++ == 0) && !label.equals("none")) {
                row.getValues().add(label);
                return row;
            }
            if (value == null) {
                for (String hline : ctx.getHeaderLines()) {
                    Matcher matcher = pattern.matcher(hline);
                    if (matcher.find()) {
                        String v = template;
                        for (int i = 0; i < matcher.groupCount(); i++) {
                            v = v.replace("{" + (i + 1) + "}",
                                          matcher.group(i + 1));
                        }
                        value = v;

                        break;
                    }
                }
            }
            if (value == null) {
                value = "";
            }
            row.getValues().add(value);

            return row;
        }
    }



    /**
     * Class description
     *
     *
     * @version        $version$, Wed, Dec 2, '15
     * @author         Enter your name here...
     */
    public static class CopyIf extends Converter {

        /**  */
        String pattern;

        /** _more_ */
        String scol1;

        /**  */
        String scol2;

        /**  */
        int col1 = -1;

        /**  */
        int col2 = -1;

        /**
         * _more_
         * @param cols _more_
         * @param pattern _more_
         * @param col1 _more_
         * @param col2 _more_
         */
        public CopyIf(List<String> cols, String pattern, String col1,
                      String col2) {
            super(cols);
            this.pattern = pattern;
            scol1        = col1;
            scol2        = col2;
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                col1 = getIndex(ctx, scol1);
                col2 = getIndex(ctx, scol2);

                return row;
            }

            //      System.err.println("Row:");
            for (int i : getIndices(ctx)) {
                String v = row.getString(i);
                //              System.err.println("\ti:" + i +" v:" + v +" p:" + pattern);
                if ( !v.matches(pattern)) {
                    //              System.err.println("\tno match:" +row);
                    return row;
                }
            }

            row.set(col2, row.getString(col1));

            return row;
        }
    }

    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Nov 4, '21
     * @author         Enter your name here...
     */
    public static class CopyColumns extends Converter {

        /**  */
        List<String> toCols;

        /**  */
        List<Integer> toIndices;

        /**
         * _more_
         * @param cols _more_
         * @param cols2 _more_
         */
        public CopyColumns(List<String> cols, List<String> cols2) {
            super(cols);
            if (cols.size() != cols2.size()) {
                throw new IllegalArgumentException(
                    "Mismatched columns in -copycolumns:" + cols + " "
                    + cols2);
            }
            this.toCols = cols2;
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                toIndices = getIndices(ctx, toCols);

                return row;
            }

            List<Integer> from = getIndices(ctx);
            //      System.err.println("copy");
            for (int i = 0; i < from.size(); i++) {
                String v = row.getString(from.get(i));
                //              System.err.println("\tsetting:" + toIndices.get(i) +" to:" + v);
                row.set(toIndices.get(i), v);
            }

            return row;
        }
    }




    /**
     * Class description
     *
     *
     * @version        $version$, Wed, Dec 2, '15
     * @author         Enter your name here...
     */
    public static class ColumnNudger extends Converter {

        /* */

        /** _more_ */
        private int col;

        /* */

        /** _more_ */
        private int rowIdx;

        /* */

        /** _more_ */
        private String value;


        /**
         * @param row _more_
         * @param col _more_
         * @param value _more_
         */
        public ColumnNudger(int row, int col, String value) {
            this.rowIdx = row;
            this.col    = col;
            this.value  = value;
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if ((rowIdx < 0) || (rowIdx == rowCnt)) {
                if (col < 0) {
                    row.getValues().add(value);
                } else {
                    row.getValues().add(col, value);
                }
            }
            rowCnt++;

            return row;

        }

    }


    /**
     * Class description
     *
     *
     * @version        $version$, Sun, Jan 21, '18
     * @author         Enter your name here...
     */
    public static class ColumnUnNudger extends Converter {

        /* */

        /** _more_ */
        private List<Integer> cols;

        /* */

        /** _more_ */
        private int rowIdx;



        /**
         * @param row _more_
         * @param cols _more_
         */
        public ColumnUnNudger(int row, List<String> cols) {
            this.cols   = Utils.toInt(cols);
            this.rowIdx = row;
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if ((rowIdx < 0) || (rowIdx == rowCnt)) {
                List newValues = new ArrayList();
                List values    = row.getValues();
                for (int i = 0; i < values.size(); i++) {
                    if ( !cols.contains(i)) {
                        newValues.add(values.get(i));
                    }
                }
                row.setValues(newValues);
            }
            rowCnt++;

            return row;

        }

    }



    /**
     * Class description
     *
     *
     * @version        $version$, Wed, Dec 2, '15
     * @author         Enter your name here...
     */
    public static class ColumnSetter extends Converter {

        /* */

        /** _more_ */
        private List<Integer> cols;

        /* */

        /** _more_ */
        private List<Integer> rows;

        /* */

        /** _more_ */
        private String value;


        /**
         * @param cols _more_
         * @param rows _more_
         * @param value _more_
         */
        public ColumnSetter(List<String> cols, List<String> rows,
                            String value) {
            super(cols);
            this.rows  = Utils.toInt(rows);
            this.value = value;
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (cols == null) {
                cols = getIndices(ctx);
            }

            boolean gotRow = false;
            for (int rowIdx : rows) {
                if (rowCnt == rowIdx) {
                    gotRow = true;

                    break;
                }
            }
            rowCnt++;
            if ( !gotRow) {
                return row;
            }

            List values = row.getValues();
            for (int col : cols) {
                if (col < values.size()) {
                    values.set(col, value);
                }
            }

            return row;
        }
    }


    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Nov 4, '21
     * @author         Enter your name here...
     */
    public static class FillDown extends Converter {

        /**  */
        private Hashtable<Integer, String> lastValue = new Hashtable<Integer,
                                                           String>();


        /**
         * @param cols _more_
         */
        public FillDown(List<String> cols) {
            super(cols);
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                return row;
            }
            List<Integer> indices = getIndices(ctx);
            for (int col : indices) {
                String v = row.getString(col);
                v = v.trim();
                if (v.length() == 0) {
                    v = lastValue.get(col);
                    if (v != null) {
                        row.set(col, v);
                    }
                } else {
                    lastValue.put(col, v);
                }
            }

            return row;
        }
    }

    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Nov 4, '21
     * @author         Enter your name here...
     */
    public static class Unfill extends Converter {

        /**  */
        private Hashtable<Integer, String> lastValue = new Hashtable<Integer,
                                                           String>();


        /**
         * @param cols _more_
         */
        public Unfill(List<String> cols) {
            super(cols);
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                return row;
            }
            List<Integer> indices = getIndices(ctx);
            for (int col : indices) {
                String v = row.getString(col);
                v = v.trim();
		String lastv = lastValue.get(col);
		if(lastv!=null && v.equals(lastv)) {
		    row.set(col,"");
		} else {
		    lastValue.put(col,v);
		}
            }

            return row;
        }
    }



    /**
     * Class description
     *
     *
     * @version        $version$, Wed, Mar 24, '21
     * @author         Enter your name here...
     */
    public static class MakeIds extends Converter {


        /**
         */
        public MakeIds() {}

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                Row newRow = new Row();
                for (int i = 0; i < row.size(); i++) {
                    newRow.add(Utils.makeID(row.getString(i)));
                }

                return newRow;
            }

            return row;
        }
    }


    /**
     * Class description
     *
     *
     * @version        $version$, Mon, Jan 20, '20
     * @author         Enter your name here...
     */
    public static class PriorPrefixer extends Converter {

        /** _more_ */
        private int col;

        /** _more_ */
        private String pattern;

        /** _more_ */
        private String delim;

        /** _more_ */
        private String prefix;

        /**
         * @param col _more_
         * @param pattern _more_
         * @param delim _more_
         */
        public PriorPrefixer(int col, String pattern, String delim) {
            this.col     = col;
            this.pattern = pattern;
            this.delim   = delim;
        }

        /**
         * _more_
         */
        public void reset() {
            super.reset();
            prefix = null;
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            String val = row.get(col).toString();
            if ((prefix != null)
                    && (val.matches(pattern)
                        || (val.indexOf(pattern) >= 0))) {
                row.set(col, prefix + delim + val);
            } else {
                prefix = val;
            }

            return row;
        }

    }


    /**
     * Class description
     *
     *
     * @version        $version$, Mon, Oct 14, '19
     * @author         Enter your name here...
     */
    public static class Letter extends Converter {

        /* */

        /** _more_ */
        int cnt = 0;

        /**
         *
         * @param ctx _more_
         */
        public Letter(TextReader ctx) {}

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            cnt++;
            if (cnt == 1) {
                add(ctx, row, "label");

                return row;

            }
            String letter = getLabel(cnt - 2);
            add(ctx, row, letter);

            return row;
        }

        /**
         * @param n _more_
         * @return _more_
         */
        private String getLabel(int n) {
            int d = (int) (n / 25.0);
            int r = n % 25;
            if (d != 0) {
                return getLabel(d) + Utils.LETTERS[r];
            } else {
                return Utils.LETTERS[r];
            }
        }
    }


    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Feb 12, '21
     * @author         Enter your name here...
     */
    public static class Number extends Converter {

        /* */

        /** _more_ */
        int cnt = 0;

        /**
         *
         * @param ctx _more_
         */
        public Number(TextReader ctx) {}

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            cnt++;
            if (cnt == 1) {
                row.insert(0, "number");

                return row;
            }
            row.insert(0, "" + (cnt - 1));

            return row;
        }


        /**
         * @param n _more_
         * @return _more_
         */
        private String getLabel(int n) {
            int d = (int) (n / 25.0);
            int r = n % 25;
            if (d != 0) {
                return getLabel(d) + Utils.LETTERS[r];
            } else {
                return Utils.LETTERS[r];
            }
        }
    }



    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Feb 12, '21
     * @author         Enter your name here...
     */
    public static class UUID extends Converter {


        /**
         *
         * @param ctx _more_
         */
        public UUID(TextReader ctx) {}

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                row.add("UUID");
            } else {
                row.add(Utils.getGuid());
            }

            return row;
        }
    }


    /**
     * Class description
     *
     *
     * @version        $version$, Wed, Apr 6, '22
     * @author         Enter your name here...
     */
    public static class B64Encode extends Converter {


        /**
         *
         * @param ctx _more_
         * @param cols _more_
         */
        public B64Encode(TextReader ctx, List<String> cols) {
            super(cols);
        }

        /**
         *
         * @param ctx _more_
         * @param row _more_
         *  @return _more_
         */
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                for (int i : getIndices(ctx)) {
                    row.add(row.getString(i) + " base64");
                }
            } else {
                for (int i : getIndices(ctx)) {
                    row.add(new String(Utils.encodeBase64(row.getString(i))));
                }
            }

            return row;
        }


    }


    /**
     * Class description
     *
     *
     * @version        $version$, Wed, Apr 6, '22
     * @author         Enter your name here...
     */
    public static class B64Decode extends Converter {

        /**
         *
         * @param ctx _more_
         * @param cols _more_
         */
        public B64Decode(TextReader ctx, List<String> cols) {
            super(cols);
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                for (int i : getIndices(ctx)) {
                    row.add(row.getString(i) + " value");
                }
            } else {
                for (int i : getIndices(ctx)) {
                    row.add(
                        new String(
                            Utils.decodeBase64(row.getString(i).getBytes())));
                }
            }

            return row;
        }
    }


    /**
     * Class description
     *
     *
     * @version        $version$, Wed, Apr 6, '22
     * @author         Enter your name here...
     */
    public static class Rot13 extends Converter {

        /**
         *
         * @param ctx _more_
         * @param cols _more_
         */
        public Rot13(TextReader ctx, List<String> cols) {
            super(cols);
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                for (int i : getIndices(ctx)) {
                    row.add(row.getString(i) + " rot13");
                }
            } else {
                for (int i : getIndices(ctx)) {
                    row.add(new String(Utils.rot13(row.getString(i))));
                }
            }

            return row;
        }
    }



    /**
     * Class description
     *
     *
     * @version        $version$, Wed, Apr 6, '22
     * @author         Enter your name here...
     */
    public static class Crypt extends Converter {

        /**  */
        protected Cipher cipher;

        /**
         *
         *
         * @param encrypt _more_
         * @param ctx _more_
         * @param cols _more_
         * @param cipherSpec _more_
         *
         * @throws Exception _more_
         */
        public Crypt(boolean encrypt, TextReader ctx, List<String> cols,
                     String password)
                throws Exception {
            super(cols);
	    MessageDigest digester = MessageDigest.getInstance("SHA-256");
	    digester.update(password.getBytes("UTF-8"));
	    byte[] key = digester.digest();
	    SecretKeySpec spec = new SecretKeySpec(key, "AES");
	    cipher = Cipher.getInstance("AES");
	    cipher.init(encrypt
			? Cipher.ENCRYPT_MODE
			: Cipher.DECRYPT_MODE, spec);
	}
    }

    /**
     * Class description
     *
     *
     * @version        $version$, Wed, Apr 6, '22
     * @author         Enter your name here...
     */
    public static class EncryptDecrypt extends Crypt {
	boolean mode;

        /**
         *
         * @param ctx _more_
         * @param cols _more_
         * @param cipherSpec _more_
         * @param key _more_
         *
         * @throws Exception _more_
         */
        public EncryptDecrypt(TextReader ctx, boolean encrypt, List<String> cols, 
                       String key)
                throws Exception {
            super(encrypt, ctx, cols, key);
	    this.mode = encrypt;
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                for (int i : getIndices(ctx)) {
                    row.add(row.getString(i) + (mode?" encrypted":" encrypted"));
                }
		return row;
            }
	    try {
		for (int i : getIndices(ctx)) {
		    if(mode) {
			byte[]bytes = cipher.doFinal(row.getBytes(i));
			row.add(Utils.encodeBase64Bytes(bytes));
		    } else {
			byte[]bytes = Utils.decodeBase64(row.getBytes(i));
			row.add(new String(cipher.doFinal(bytes)));
		    }
		}
	    } catch (Exception exc) {
		fatal(ctx, "Error encrypting", exc);
            }

            return row;
        }
    }








    /**
     * Class description
     *
     *
     * @version        $version$, Wed, Dec 2, '15
     * @author         Enter your name here...
     */
    public static class ColumnPatternSetter extends Converter {

        /* */

        /** _more_ */
        private int patternCol = -1;

        /** _more_ */
        private String spatternCol;

        /** _more_ */
        private String pattern;

        /* */

        /** _more_ */
        private int writeCol = -1;

        /** _more_ */
        private String swriteCol;

        /** _more_ */
        private String what;



        /**
         * @param col1 _more_
         * @param pattern _more_
         * @param col2 _more_
         * @param what _more_
         */
        public ColumnPatternSetter(String col1, String pattern, String col2,
                                   String what) {
            this.spatternCol = col1;
            this.pattern     = pattern;
            this.swriteCol   = col2;
            this.what        = what;
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                patternCol = getIndex(ctx, spatternCol);
                writeCol   = getIndex(ctx, swriteCol);

                return row;
            }
            if (patternCol == -1) {
                row.set(writeCol, what);
            } else {
                String v = row.get(patternCol).toString();
                if (v.matches(pattern) || (v.indexOf(pattern) >= 0)) {
                    row.set(writeCol, what);
                }
            }

            return row;

        }

    }

    /**
     *
     *
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        List l = new ArrayList();
        l.add("a");
        l.add("b");
        l.add("c");
        l.add(0, "x");
        System.err.println(l);
        if (true) {
            return;
        }



        String s = "x";
        try {
            org.mozilla.javascript.Context cx =
                org.mozilla.javascript.Context.enter();
            org.mozilla.javascript.Scriptable scope =
                cx.initSafeStandardObjects();
            scope.put("x", scope, "33");
            Object result = cx.evaluateString(scope, s, "<cmd>", 1, null);
            System.err.println(
                org.mozilla.javascript.Context.toString(result));
        } finally {
            org.mozilla.javascript.Context.exit();
        }
    }

}
