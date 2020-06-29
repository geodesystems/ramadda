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

package org.ramadda.util.text;


import org.json.*;

import org.ramadda.util.GeoUtils;


import org.ramadda.util.IO;
import org.ramadda.util.Json;
import org.ramadda.util.Place;
import org.ramadda.util.Utils;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.io.*;

import java.net.URL;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;

import java.util.regex.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.*;


/**
 * Class description
 *
 *
 * @version        $version$, Fri, Jan 9, '15
 * @author         Jeff McWhirter
 */
public abstract class Converter extends Processor {


    /**
     *
     */
    public Converter() {}

    /**
     *
     *
     *
     * @param col _more_
     */
    public Converter(String col) {
        super(col);
    }

    /**
     *
     *
     *
     *
     * @param cols _more_
     */
    public Converter(List<String> cols) {
        super(cols);
    }



    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Jan 16, '15
     * @author         Enter your name here...
     */
    public static class ConverterGroup extends Converter {


        /**
         *
         */
        public ConverterGroup() {}

    }


    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Jan 9, '15
     * @author         Jeff McWhirter
     */
    public static class ColumnSelector extends Converter {


        /**
         *
         *
         *
         *
         * @param cols _more_
         */
        public ColumnSelector(List<String> cols) {
            super(cols);
        }



        /**
         *
         * @param info _more_
         * @param row _more_
         * @param line _more_
         *
         * @return _more_
         */
        @Override
        public Row processRow(TextReader info, Row row, String line) {
            List<Integer> indices = getIndices(info);
            if (indices.size() == 0) {
                return row;
            }
            List<String> result = new ArrayList<String>();
            for (Integer idx : indices) {
                if (idx < row.size()) {
                    String s = row.getString(idx);
                    result.add(s);
                }
            }

            return new Row(result);
        }
    }



    /**
     * Class description
     *
     *
     * @version        $version$, Mon, Oct 14, '19
     * @author         Enter your name here...
     */
    public static class ColumnNotSelector extends Converter {


        /**
         *
         * @param cols _more_
         */
        public ColumnNotSelector(List<String> cols) {
            super(cols);
        }


        /**
         *
         * @param info _more_
         * @param row _more_
         * @param line _more_
         *
         * @return _more_
         */
        @Override
        public Row processRow(TextReader info, Row row, String line) {

            List<Integer> indices = getIndices(info);
            if (indices.size() == 0) {
                return row;
            }
            List<String> result = new ArrayList<String>();
            for (int i = 0; i < row.size(); i++) {
                if ( !indices.contains(i)) {
                    result.add(row.getString(i));
                }
            }

            return new Row(result);
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

        /**
         *
         *
         *
         *
         * @param cols _more_
         * @param suffix _more_
         */
        public ImageSearch(List<String> cols, String suffix) {
            super(cols);
            this.suffix = suffix;
        }


        /**
         *
         *
         *
         *
         *
         * @param info _more_
         * @param row _more_
         * @param line _more_
         *
         * @return _more_
         */
        @Override
        public Row processRow(TextReader info, Row row, String line) {
            if (rowCnt++ == 0) {
                add(info, row, "image");

                return row;
            }

            List<Integer> indices = getIndices(info);
            String        s       = "";
            for (Integer idx : indices) {
                s += row.getString(idx) + " ";
            }
            s += suffix;
            //hack, hack
            String script = "/Users/jeffmc/bin/imagesearch.sh";
            for (int attempt = 0; attempt < 3; attempt++) {
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
                        imageMap.put(s, image);
                    }
                    add(info, row, image);

                    return row;
                } catch (Exception exc) {
                    System.err.println("JSON:" + result);

                    throw new RuntimeException(exc);
                }
            }
            add(info, row, "");

            return row;
        }

    }


    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Jan 9, '15
     * @author         Jeff McWhirter
     */
    public static class DescSearch extends Converter {

        /* */

        /** _more_ */
        private String suffix;

        /**
         *
         *
         *
         *
         * @param cols _more_
         * @param suffix _more_
         */
        public DescSearch(List<String> cols, String suffix) {
            super(cols);
            this.suffix = suffix;
        }


        /**
         * @param info _more_
         * @param row _more_
         * @param line _more_
         *
         * @return _more_
         */
        @Override
        public Row processRow(TextReader info, Row row, String line) {
            if (rowCnt++ == 0) {
                add(info, row, "description");

                return row;
            }

            try {
                List<Integer> indices = getIndices(info);
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
                JSONArray  values = Json.readArray(obj, "query.search");
                if (values.length() == 0) {
                    System.err.println("No results for query:" + s);
                    add(info, row, "");

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
                obj    = Json.readObject(obj, "parse.text");
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
                add(info, row, p);

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
         *
         *
         *
         *
         *
         * @param info _more_
         * @param row _more_
         * @param line _more_
         *
         * @return _more_
         */
        @Override
        public Row processRow(TextReader info, Row row, String line) {
            List<Integer> indices = getIndices(info);
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
         *
         *
         *
         * @param cols _more_
         * @param fmt _more_
         */
        public ColumnFormatter(List<String> cols, String fmt) {
            super(cols);
            format = new DecimalFormat(fmt);
        }



        /**
         *
         *
         *
         *
         *
         * @param info _more_
         * @param row _more_
         * @param line _more_
         *
         * @return _more_
         */
        @Override
        public Row processRow(TextReader info, Row row, String line) {
            List<Integer> indices = getIndices(info);
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
         *
         *
         *
         *
         * @param count _more_
         * @param s _more_
         */
        public Padder(int count, String s) {
            this.count = count;
            this.pad   = s;
        }


        /**
         *
         *
         *
         *
         *
         * @param info _more_
         * @param row _more_
         * @param line _more_
         *
         * @return _more_
         */
        @Override
        public Row processRow(TextReader info, Row row, String line) {
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
         *
         *
         *
         *
         *
         *
         * @param cols _more_
         * @param s _more_
         */
        public Prefixer(List<String> cols, String s) {
            super(cols);
            this.pad = s;
        }


        /**
         *
         *
         *
         *
         *
         * @param info _more_
         * @param row _more_
         * @param line _more_
         *
         * @return _more_
         */
        @Override
        public Row processRow(TextReader info, Row row, String line) {
            if (rowCnt++ == 0) {
                if ( !info.getAllData()) {
                    return row;
                }
            }
            List<Integer> indices = getIndices(info);
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
         *
         *
         *
         *
         *
         *
         * @param cols _more_
         * @param s _more_
         */
        public Suffixer(List<String> cols, String s) {
            super(cols);
            this.pad = s;
        }


        /**
         *
         *
         *
         *
         *
         * @param info _more_
         * @param row _more_
         * @param line _more_
         *
         * @return _more_
         */
        @Override
        public Row processRow(TextReader info, Row row, String line) {
            if (rowCnt++ == 0) {
                if ( !info.getAllData()) {
                    return row;
                }
            }
            List<Integer> indices = getIndices(info);
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
         *
         *
         */
        public PrintHeader() {}

        /**
         *
         *
         *
         * @param asPoint _more_
         */
        public PrintHeader(boolean asPoint) {
            this.asPoint = asPoint;
        }


        /**
         *
         *
         *
         *
         *
         * @param info _more_
         * @param row _more_
         * @param line _more_
         *
         * @return _more_
         */
        @Override
        public Row processRow(TextReader info, Row row, String line) {
            PrintWriter writer    = info.getWriter();
            String      delimiter = info.getDelimiter();
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

            info.stopRunning();

            return null;
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
        Hashtable<String, String> props;

        /* */

        /** _more_ */
        String defaultType = "string";

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
         *
         * @param props _more_
         */
        public HeaderMaker(Hashtable<String, String> props) {
            this.props = props;
            defaultType = CsvUtil.getDbProp(props, "default", "type",
                                            defaultType);
            defaultChartable = CsvUtil.getDbProp(props, "default",
                    "chartable", true);
            makeLabel = CsvUtil.getDbProp(props, null, "makeLabel", true);
            toStdOut  = CsvUtil.getDbProp(props, null, "stdout", false);
        }


        /**
         *
         * @param info _more_
         * @param row _more_
         * @param line _more_
         *
         * @return _more_
         */
        @Override
        public Row processRow(TextReader info, Row row, String line) {

            rowCnt++;
            if (rowCnt > 2) {
                //              System.err.println("hdr rest:" + row);
                return row;
            }
            if (firstRow == null) {
                firstRow = row;

                return null;
            }
            boolean justFields  = Misc.equals(props.get("justFields"),
                                      "true");
            boolean      debug  = Misc.equals(props.get("debug"), "true");
            PrintWriter  writer = info.getWriter();
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
                StringBuffer attrs = new StringBuffer();
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
                String  format = dfltFormat;
                String  type   = defaultType;
                boolean isGeo  = false;

                boolean chartable = CsvUtil.getDbProp(props, id, "chartable",
                                        defaultChartable);
                if (id.equals("date")) {
                    type = "date";
                } else if (id.equals("year")) {
                    type   = "date";
                    format = "yyyy";
                } else if (id.equals("url") || id.endsWith("_url")) {
                    type = "url";

                } else if (id.equals("state") || id.equals("country")
                           || id.equals("category")) {
                    type = "enumeration";

                } else if (id.equals("latitude") || id.equals("longitude")) {
                    type      = "double";
                    isGeo     = true;
                    chartable = false;
                } else {
                    try {
                        if (_sample.equals("true")
                                || _sample.equals("false")) {
                            type = "enumeration";
                        } else if (_sample.equals("nan")
                                   || _sample.equals("na")) {
                            type = "double";
                        } else if (sample.matches("^(\\+|-)?\\d+$")) {
                            type = "integer";
                        } else if (sample.matches(
                                "^[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?$")) {
                            type = "double";
                        } else {}
                    } catch (Exception exc) {}
                }


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


                if ((format != null) && Misc.equals(type, "date")) {
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

            if (toStdOut) {
                System.out.println(StringUtil.join(",", values));
                info.stopRunning();

                return firstRow;
            }
            //      System.err.println("hdr first:" + firstRow);
            //      System.err.println("hdr row:" + row);
            firstRow.setValues(values);
            row.setSkipTo(this);
            info.setExtraRow(row);
            Row tmp = firstRow;
            firstRow = null;
            if (debug) {
                writer.println("");
                info.stopRunning();
            }

            return tmp;

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
         *
         *
         *
         *
         *
         * @param cols _more_
         */
        public ColumnPercenter(List<String> cols) {
            super(cols);
        }


        /**
         *
         *
         *
         *
         *
         * @param info _more_
         * @param row _more_
         * @param line _more_
         *
         * @return _more_
         */
        @Override
        public Row processRow(TextReader info, Row row, String line) {
            List          values  = row.getValues();
            double        total   = 0;
            int           cnt     = 0;
            List<Integer> indices = getIndices(info);
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
     *
     * @version        $version$, Tue, Nov 19, '19
     * @author         Enter your name here...
     */
    public static class ColumnIncrease extends Converter {


        /* */

        /** _more_ */
        int col;

        /* */

        /** _more_ */
        int step;

        /* */

        /** _more_ */
        List<Double> values = new ArrayList<Double>();

        /**
         *
         *
         *
         *
         *
         * @param col _more_
         * @param step _more_
         */
        public ColumnIncrease(int col, int step) {
            this.col  = col;
            this.step = step;
        }


        /**
         *
         *
         *
         *
         *
         * @param info _more_
         * @param row _more_
         * @param line _more_
         *
         * @return _more_
         */
        @Override
        public Row processRow(TextReader info, Row row, String line) {
            if (rowCnt++ == 0) {
                add(info, row, row.get(col) + " increase");

                return row;
            }
            double v = Double.parseDouble(row.get(col).toString());
            if (values.size() < step) {
                add(info, row, new Double(Double.NaN));
            } else {
                double pastValue = values.get(0);
                values.remove(0);
                double increase = 0;
                // 20 30
                if (pastValue == 0) {
                    add(info, row, new Double(Double.NaN));
                } else {
                    double diff = v - pastValue;
                    increase = diff / pastValue;
                    //              System.out.println("x:" + v +" " + pastValue +"  diff:" + diff +" i:" + increase);
                    add(info, row, new Double(increase));
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
         *
         *
         *
         *
         *
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
         *
         *
         *
         *
         *
         * @param info _more_
         * @param row _more_
         * @param line _more_
         *
         * @return _more_
         */
        @Override
        public Row processRow(TextReader info, Row row, String line) {
            List<Integer> indices = getIndices(info);
            if (rowCnt++ == 0) {
                for (int i = 0; i < indices.size(); i++) {
                    values.add(new ArrayList<Double>());
                    add(info, row, row.get(indices.get(i)) + " " + label);
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
                add(info, row, average);
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
        ScriptEngine engine;

        /** _more_ */
        List<String> names;

        /** _more_ */
        Row headerRow;

        /**
         *
         *
         *
         * @param js _more_
         * @param names _more_
         * @param code _more_
         */
        public ColumnFunc(String js, String names, String code) {
            this.names = StringUtil.split(names, ",", true, true);
            if (code.indexOf("return ") >= 0) {
                code = "function _tmp() {" + code + "}\n_tmp()";
            }
            this.code = code;
            try {
                jdk.nashorn.api.scripting.ClassFilter cf =
                    new jdk.nashorn.api.scripting.ClassFilter() {
                    public boolean exposeToScripts(String s) {
                        return false;
                    }
                };
                engine =
                    new jdk.nashorn.api.scripting.NashornScriptEngineFactory()
                        .getScriptEngine(new String[] { "--no-java" }, null,
                                         cf);
                engine.eval("var globalCache = {};");
                if (js.trim().length() > 0) {
                    engine.eval(js);
                }

                String testScript =
                    "print(java.lang.System.getProperty(\"java.home\"));"
                    + "print(\"Create file variable\");"
                    + "var File = Java.type(\"java.io.File\");";
                //              engine.eval(testScript);
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }

        }

        /**
         *
         *
         *
         *
         * @param info _more_
         * @param row _more_
         * @param line _more_
         *
         * @return _more_
         */
        @Override
        public Row processRow(TextReader info, Row row, String line) {
            //      System.err.println("func row:" + row);
            rowCnt++;
            if (rowCnt == 1) {
                headerRow = new Row(row);
                for (String s : names) {
                    row.add(s);
                }

                return row;
            }
            try {
                List hdr = headerRow.getValues();
                for (int i = 0; i < hdr.size(); i++) {
                    if (i >= row.size()) {
                        continue;
                    }
                    Object o   = row.get(i);
                    String var = hdr.get(i).toString();
                    var = Utils.makeID(var.toLowerCase());
                    engine.put("_" + var, o);
                    engine.put("_" + var + "_idx", i);
                    engine.put("_col" + i, o);
                }
                engine.put("_header", hdr);
                engine.put("_values", row.getValues());
                engine.put("_rowidx", rowCnt - 1);

                // evaluate JavaScript code
                Object o = engine.eval(code);
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
                if (o instanceof jdk.nashorn.api.scripting
                        .ScriptObjectMirror) {
                    jdk.nashorn.api.scripting.ScriptObjectMirror som =
                        (jdk.nashorn.api.scripting.ScriptObjectMirror) o;
                    if (som.isArray()) {
                        Collection<Object> values = som.values();
                        for (Object v : values) {
                            row.add(v);
                        }
                    } else {
                        row.add(o);
                    }
                } else {
                    row.add(o);
                }

                //              System.err.println("func row:" + row);
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
     * @version        $version$, Wed, Jan 17, '18
     * @author         Enter your name here...
     */
    public static class ColumnOperator extends Converter {

        /**
         *
         */
        public ColumnOperator() {}

        /**
         *
         *
         *
         *
         * @param info _more_
         * @param row _more_
         * @param line _more_
         *
         * @return _more_
         */
        @Override
        public Row processRow(TextReader info, Row row, String line) {
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
     * @version        $version$, Fri, Jan 16, '15
     * @author         Enter your name here...
     */
    public static class ColumnChanger extends Converter {

        /* */

        /** _more_ */
        private boolean isRegex;

        /* */

        /** _more_ */
        private String pattern;

        /* */

        /** _more_ */
        private String value;


        /**
         *
         *
         *
         *
         * @param cols _more_
         * @param pattern _more_
         * @param value _more_
         */
        public ColumnChanger(List<String> cols, String pattern,
                             String value) {
            super(cols);
            this.pattern = pattern;
            this.isRegex = StringUtil.containsRegExp(pattern);
            //            if(!isRegex)
            //                this.pattern = ".*" + this.pattern +".*";
            this.value = value;
        }

        /**
         *
         *
         *
         *
         *
         * @param info _more_
         * @param row _more_
         * @param line _more_
         *
         * @return _more_
         */
        @Override
        public Row processRow(TextReader info, Row row, String line) {
            //Don't process the first row
            if (rowCnt++ == 0) {
                if ( !info.getAllData()) {
                    return row;
                }
            }
            List<Integer> indices = getIndices(info);
            for (Integer idx : indices) {
                int index = idx.intValue();
                if ((index >= 0) && (index < row.size())) {
                    String s  = row.getString(index).trim();
                    String os = s;
                    if (isRegex) {
                        s = s.replaceAll(pattern, value);
                    } else {
                        s = s.replaceAll(pattern, value);
                    }
                    //              System.err.println("P:"  + pattern +" os:" + os +" s:" + s);
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
         *
         *
         *
         *
         *
         * @param info _more_
         * @param row _more_
         * @param line _more_
         *
         * @return _more_
         */
        @Override
        public Row processRow(TextReader info, Row row, String line) {
            //Don't process the first row
            if (rowCnt++ == 0) {
                if ( !info.getAllData()) {
                    return row;
                }
            }
            List<Integer> indices = getIndices(info);
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
     * @version        $version$, Sat, Mar 14, '20
     * @author         Enter your name here...
     */
    public static class ColumnTrimmer extends Converter {

        /**
         *
         * @param cols _more_
         */
        public ColumnTrimmer(List<String> cols) {
            super(cols);
        }

        /**
         *
         *
         *
         *
         *
         * @param info _more_
         * @param row _more_
         * @param line _more_
         *
         * @return _more_
         */
        @Override
        public Row processRow(TextReader info, Row row, String line) {
            //Don't process the first row
            if (rowCnt++ == 0) {
                return row;
            }
            List<Integer> indices = getIndices(info);
            for (Integer idx : indices) {
                int index = idx.intValue();
                if ((index >= 0) && (index < row.size())) {
                    String s = row.getString(index);
                    //This is not an underscore but something else that shows up in web pages
                    s = s.replaceAll(" ", "");
                    s = s.trim();
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
     * @version        $version$, Mon, Oct 14, '19
     * @author         Enter your name here...
     */
    public static class DateConverter extends Converter {

        /* */

        /** _more_ */
        private int col;

        /* */

        /** _more_ */
        private SimpleDateFormat sdf1;

        /* */

        /** _more_ */
        private SimpleDateFormat sdf2;


        /**
         *
         *
         *
         *
         * @param col _more_
         * @param sdf1 _more_
         * @param sdf2 _more_
         */
        public DateConverter(int col, SimpleDateFormat sdf1,
                             SimpleDateFormat sdf2) {
            this.col  = col;
            this.sdf1 = sdf1;
            this.sdf2 = sdf2;
        }

        /**
         *
         *
         *
         *
         *
         * @param info _more_
         * @param row _more_
         * @param line _more_
         *
         * @return _more_
         */
        @Override
        public Row processRow(TextReader info, Row row, String line) {
            //Don't process the first row
            if (rowCnt++ == 0) {
                return row;
            }
            try {
                String s = row.get(col).toString();
                Date   d = sdf1.parse(s);
                //              System.err.println(s + " D:" + d  +" " + sdf2.format(d));
                row.set(col, sdf2.format(d));
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }

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
    public static class DateExtracter extends Converter {

        /* */

        /** _more_ */
        private int col;

        /* */

        /** _more_ */
        private SimpleDateFormat sdf;

        /* */

        /** _more_ */
        private TimeZone tz;

        /* */

        /** _more_ */
        private String whatLabel = "Hour";

        /* */

        /** _more_ */
        private int what = GregorianCalendar.HOUR;



        /**
         *
         *
         * @param col _more_
         * @param sdf _more_
         * @param tz _more_
         * @param what _more_
         */
        public DateExtracter(int col, String sdf, String tz, String what) {
            this.col = col;
            if (sdf.length() > 0) {
                this.sdf = new SimpleDateFormat(sdf);
                this.sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
            }


            if (tz.length() > 0) {
                tz      = tz.toUpperCase();
                this.tz = TimeZone.getTimeZone(tz);

                System.err.println(tz + " tz:" + this.tz);

            }
            whatLabel = StringUtil.camelCase(what);
            what      = what.toUpperCase();
            if (what.equals("ERA")) {
                this.what = GregorianCalendar.ERA;
            } else if (what.equals("YEAR")) {
                this.what = GregorianCalendar.YEAR;
            } else if (what.equals("MONTH")) {
                this.what = GregorianCalendar.MONTH;
            } else if (what.equals("DAY_OF_MONTH")) {
                this.what = GregorianCalendar.DAY_OF_MONTH;
            } else if (what.equals("DAY_OF_WEEK")) {
                this.what = GregorianCalendar.DAY_OF_WEEK;
            } else if (what.equals("WEEK_OF_MONTH")) {
                this.what = GregorianCalendar.WEEK_OF_MONTH;
            } else if (what.equals("DAY_OF_WEEK_IN_MONTH")) {
                this.what = GregorianCalendar.DAY_OF_WEEK_IN_MONTH;
            } else if (what.equals("AM_PM")) {
                this.what = GregorianCalendar.AM_PM;
            } else if (what.equals("HOUR")) {
                this.what = GregorianCalendar.HOUR;
            } else if (what.equals("HOUR_OF_DAY")) {
                this.what = GregorianCalendar.HOUR_OF_DAY;
            } else if (what.equals("MINUTE")) {
                this.what = GregorianCalendar.MINUTE;
            } else if (what.equals("SECOND")) {
                this.what = GregorianCalendar.SECOND;
            } else if (what.equals("MILLISECOND")) {
                this.what = GregorianCalendar.MILLISECOND;
            }
        }

        /**
         *
         *
         *
         *
         * @param info _more_
         * @param row _more_
         * @param line _more_
         *
         * @return _more_
         */
        public Row processRow(TextReader info, Row row, String line) {
            //Don't process the first row
            if (rowCnt++ == 0) {
                add(info, row, whatLabel);

                return row;
            }
            try {
                String            s   = row.get(col).toString();
                Date              d   = (sdf == null)
                                        ? Utils.parseDate(s)
                                        : sdf.parse(s);
                GregorianCalendar cal = new GregorianCalendar();
                cal.setTimeZone(TimeZone.getTimeZone("GMT"));
                cal.setTime(d);
                if (this.tz != null) {
                    cal.setTimeZone(this.tz);
                }
                String v = "NA";
                v = "" + cal.get(what);
                add(info, row, v);
            } catch (Exception exc) {
                throw new RuntimeException(exc);
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
    public static class DateBefore extends Converter {

        /* */

        /** _more_ */
        private int col;

        /* */

        /** _more_ */
        private Date date;

        /* */

        /** _more_ */
        private SimpleDateFormat sdf1;

        /**
         *
         *
         *
         *
         * @param col _more_
         * @param sdf1 _more_
         * @param date _more_
         */
        public DateBefore(int col, SimpleDateFormat sdf1, Date date) {
            this.col  = col;
            this.sdf1 = sdf1;
            this.date = date;
        }

        /**
         *
         *
         *
         *
         *
         * @param info _more_
         * @param row _more_
         * @param line _more_
         *
         * @return _more_
         */
        @Override
        public Row processRow(TextReader info, Row row, String line) {
            //Don't process the first row
            if (rowCnt++ == 0) {
                return row;
            }
            try {
                String s = row.get(col).toString().trim();
                if (s.length() == 0) {
                    return null;
                }
                Date d = sdf1.parse(s);
                if (d.getTime() > date.getTime()) {
                    return null;
                }

                return row;
            } catch (Exception exc) {
                exc.printStackTrace();

                throw new RuntimeException(exc);
            }
        }

    }


    /**
     * Class description
     *
     *
     * @version        $version$, Mon, Oct 14, '19
     * @author         Enter your name here...
     */
    public static class DateAfter extends Converter {

        /* */

        /** _more_ */
        private int col;

        /* */

        /** _more_ */
        private Date date;

        /* */

        /** _more_ */
        private SimpleDateFormat sdf1;

        /**
         *
         *
         *
         *
         * @param col _more_
         * @param sdf1 _more_
         * @param date _more_
         */
        public DateAfter(int col, SimpleDateFormat sdf1, Date date) {
            this.col  = col;
            this.sdf1 = sdf1;
            this.date = date;
        }

        /**
         *
         *
         *
         *
         *
         * @param info _more_
         * @param row _more_
         * @param line _more_
         *
         * @return _more_
         */
        @Override
        public Row processRow(TextReader info, Row row, String line) {
            //Don't process the first row
            if (rowCnt++ == 0) {
                return row;
            }
            try {
                String s = row.get(col).toString().trim();
                if (s.length() == 0) {
                    return null;
                }

                Date d = sdf1.parse(s);
                if (d.getTime() < date.getTime()) {
                    return null;
                }

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
     * @version        $version$, Thu, Sep 5, '19
     * @author         Enter your name here...
     */
    public static class ColumnExtracter extends Converter {

        /* */

        /** _more_ */
        private int col;

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
         *
         *
         *
         *
         * @param col _more_
         * @param pattern _more_
         * @param replace _more_
         * @param name _more_
         */
        public ColumnExtracter(int col, String pattern, String replace,
                               String name) {
            this.col     = col;
            this.pattern = pattern;
            this.replace = replace;
            this.name    = name;
        }

        /**
         *
         *
         *
         *
         *
         * @param info _more_
         * @param row _more_
         * @param line _more_
         *
         * @return _more_
         */
        @Override
        public Row processRow(TextReader info, Row row, String line) {
            //Don't process the first row
            if (rowCnt++ == 0) {
                add(info, row, name);

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
            add(info, row, newValue);
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
         *
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
         *
         * @param info _more_
         * @param row _more_
         * @param line _more_
         *
         * @return _more_
         */
        @Override
        public Row processRow(TextReader info, Row row, String line) {
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
    public static class RowChanger extends Converter {


        /* */

        /** _more_ */
        private String pattern;

        /* */

        /** _more_ */
        private String value;

        /* */

        /** _more_ */
        private HashSet<Integer> rows;

        /**
         *
         *
         *
         *
         *
         * @param rowList _more_
         * @param cols _more_
         * @param pattern _more_
         * @param value _more_
         */
        public RowChanger(List<Integer> rowList, List<String> cols,
                          String pattern, String value) {
            super(cols);
            rows = new HashSet<Integer>();
            for (int row : rowList) {
                rows.add(row);
            }
            this.pattern = pattern;
            this.value   = value;
        }

        /**
         *
         *
         *
         *
         *
         * @param info _more_
         * @param row _more_
         * @param line _more_
         *
         * @return _more_
         */
        @Override
        public Row processRow(TextReader info, Row row, String line) {
            //Don't process the first row
            if ( !rows.contains(rowCnt++)) {
                if ( !info.getAllData()) {
                    return row;
                }
            }
            List<Integer> indices = getIndices(info);
            for (Integer idx : indices) {
                int    index = idx.intValue();
                String s     = row.getString(index);
                s = s.replaceAll(pattern, value);
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
         *
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
         * @param info _more_
         * @param row _more_
         * @param line _more_
         *
         * @return _more_
         */
        @Override
        public Row processRow(TextReader info, Row row, String line) {
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
         *
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
         *
         * @param info _more_
         * @param row _more_
         * @param line _more_
         *
         * @return _more_
         */
        @Override
        public Row processRow(TextReader info, Row row, String line) {
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
         *
         *
         *
         *
         * @param cols _more_
         * @param pattern _more_
         */
        public ColumnDebugger(List<String> cols, String pattern) {
            super(cols);
            this.pattern = pattern;
        }

        /**
         *
         *
         *
         *
         *
         * @param info _more_
         * @param row _more_
         * @param line _more_
         *
         * @return _more_
         */
        @Override
        public Row processRow(TextReader info, Row row, String line) {
            //Don't process the first row
            if (rowCnt++ == 0) {
                if ( !info.getAllData()) {
                    return row;
                }
            }
            List<Integer> indices = getIndices(info);
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
     * @version        $version$, Sat, Jan 26, '19
     * @author         Enter your name here...
     */
    public static class DateFormatter extends Converter {

        /* */

        /** _more_ */
        private SimpleDateFormat from;

        /* */

        /** _more_ */
        private SimpleDateFormat to;

        /**
         *
         *
         *
         *
         *
         * @param cols _more_
         * @param from _more_
         * @param to _more_
         *
         * @throws Exception _more_
         */
        public DateFormatter(List<String> cols, String from, String to)
                throws Exception {
            super(cols);
            this.from = new SimpleDateFormat(from);
            if (to.length() == 0) {
                to = "yyyyMMdd'T'HHmmss Z";
            }
            this.to = new SimpleDateFormat(to);
        }

        /**
         *
         *
         *
         *
         *
         * @param info _more_
         * @param row _more_
         * @param line _more_
         *
         * @return _more_
         */
        @Override
        public Row processRow(TextReader info, Row row, String line) {
            //Don't process the first row
            if (rowCnt++ == 0) {
                if ( !info.getAllData()) {
                    return row;
                }
            }

            List<Integer> indices = getIndices(info);
            for (Integer idx : indices) {
                int    index = idx.intValue();
                String value = row.getString(index);
                Date   dttm  = null;
                try {
                    dttm = from.parse(value);
                } catch (java.text.ParseException exc) {
                    throw new IllegalArgumentException(
                        "Bad parse date format:" + value);
                }
                String toDate = to.format(dttm);
                row.set(index, toDate);
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
         *
         *
         *
         *
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
         *
         *
         *
         *
         *
         * @param info _more_
         * @param row _more_
         * @param line _more_
         *
         * @return _more_
         */
        @Override
        public Row processRow(TextReader info, Row row, String line) {
            if (rowCnt++ == 0) {
                if (name.length() > 0) {
                    row.getValues().add(name);
                }

                return row;
            }
            List<Integer> indices = getIndices(info);
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
         *
         *
         *
         *
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
         *
         *
         *
         *
         *
         * @param info _more_
         * @param row _more_
         * @param line _more_
         *
         * @return _more_
         */
        @Override
        public Row processRow(TextReader info, Row row, String line) {
            int index = getIndex(info);
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
            List<String> toks = StringUtil.split(row.get(index), delimiter);
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
         *
         *
         *
         *
         *
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
         *
         *
         *
         *
         *
         * @param info _more_
         * @param row _more_
         * @param line _more_
         *
         * @return _more_
         */
        @Override
        public Row processRow(TextReader info, Row row, String line) {
            List<Integer> indices = getIndices(info);
            if (rowCnt++ == 0) {
                if (name.length() > 0) {
                    if (inPlace) {
                        row = filterValues(info, row);
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
                row = filterValues(info, row);
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
    public static class Geocoder extends Converter {

        /* */

        /** _more_ */
        private HashSet seen = new HashSet();

        /* */

        /** _more_ */
        private boolean writeForDb = false;

        /* */

        /** _more_ */
        private int badCnt = 0;

        /* */

        /** _more_ */
        private int nameIndex;

        /* */

        /** _more_ */
        private int latIndex;

        /* */

        /** _more_ */
        private int lonIndex;

        /* */

        /** _more_ */
        private Hashtable<String, double[]> map;

        /* */

        /** _more_ */
        private boolean doneHeader = false;

        /* */

        /** _more_ */
        private boolean doAddress = false;

        /* */

        /** _more_ */
        private String prefix;

        /* */

        /** _more_ */
        private String suffix;

        /* */

        /** _more_ */
        private String latLabel = "Latitude";

        /* */

        /** _more_ */
        private String lonLabel = "Longitude";

        /**
         *
         *
         *
         *
         *
         * @param col _more_
         * @param mapFile _more_
         * @param nameIndex _more_
         * @param latIndex _more_
         * @param lonIndex _more_
         * @param writeForDb _more_
         *
         * @throws Exception _more_
         */
        public Geocoder(String col, String mapFile, int nameIndex,
                        int latIndex, int lonIndex, boolean writeForDb)
                throws Exception {
            super(col);
            this.nameIndex  = nameIndex;
            this.latIndex   = latIndex;
            this.lonIndex   = lonIndex;
            this.writeForDb = writeForDb;
            this.map        = makeMap(mapFile);
        }



        /**
         *
         *
         *
         *
         *
         * @param cols _more_
         * @param prefix _more_
         * @param suffix _more_
         *
         * @throws Exception _more_
         */
        public Geocoder(List<String> cols, String prefix, String suffix)
                throws Exception {
            super(cols);
            this.prefix     = prefix;
            this.suffix     = suffix;
            this.writeForDb = false;
            doAddress       = true;
        }

        /**
         *
         *
         *
         *
         *
         * @param cols _more_
         * @param prefix _more_
         * @param suffix _more_
         * @param forDb _more_
         *
         * @throws Exception _more_
         */
        public Geocoder(List<String> cols, String prefix, String suffix,
                        boolean forDb)
                throws Exception {
            super(cols);
            this.prefix     = prefix;
            this.suffix     = suffix;
            this.writeForDb = forDb;
            doAddress       = true;
        }

        /**
         *
         *
         *
         *
         *
         * @param filename _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        private Hashtable<String, double[]> makeMap(String filename)
                throws Exception {
            Hashtable<String, double[]> map = new Hashtable<String,
                                                  double[]>();
            long t1 = System.currentTimeMillis();
            //            System.err.println("Reading file:" + filename);
            BufferedReader br = new BufferedReader(
                                    new InputStreamReader(
                                        getInputStream(filename)));
            //            System.err.println("Done Reading file:" + filename);
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.length() == 0) {
                    continue;
                }
                if (line.startsWith("#")) {
                    continue;
                }
                List<String> toks = StringUtil.split(line, ",");
                String       key  = toks.get(nameIndex).trim();
                try {
                    double[] v = new double[2];
                    v[0] = Double.parseDouble(toks.get(latIndex).trim());
                    v[1] = Double.parseDouble(toks.get(lonIndex).trim());
                    //                    map.put(key, GeoUtils.getBounds(v));
                    map.put(key, v);
                } catch (Exception exc) {
                    //                    exc.printStackTrace();
                }
            }
            long t2 = System.currentTimeMillis();

            //            Utils.printTimes("map time:",t1,t2);
            return map;
        }

        /**
         *
         *
         *
         *
         *
         * @param info _more_
         * @param row _more_
         * @param line _more_
         *
         * @return _more_
         */
        @Override
        public Row processRow(TextReader info, Row row, String line) {
            List values = row.getValues();
            if ( !doneHeader) {
                if (writeForDb) {
                    add(info, row, "Location");
                } else {
                    add(info, row, latLabel, lonLabel);
                }
                doneHeader = true;

                return row;
            }

            List<Integer> indices = getIndices(info);
            StringBuilder key     = new StringBuilder();
            if ((prefix != null) && (prefix.length() > 0)) {
                key.append(prefix);
                key.append(" ");
            }
            boolean didOne = false;
            for (int i : indices) {
                Object value = values.get(i);
                if (didOne) {
                    key.append(", ");
                }
                didOne = true;
                key.append(value);
            }

            if ((suffix != null) && (suffix.length() > 0)) {
                key.append(" ");
                key.append(suffix);
            }

            double lat = Double.NaN;
            double lon = Double.NaN;
            if (key != null) {
                Place place = null;
                if (doAddress) {
                    place = GeoUtils.getLocationFromAddress(key.toString());
                } else {
                    String   tok    = key.toString();
                    double[] bounds = map.get(tok);
                    if (bounds == null) {
                        bounds = map.get(tok.replaceAll("-.*$", ""));
                    }
                    if (bounds == null) {
                        List<String> toks = StringUtil.splitUpTo(tok, ",", 2);
                        bounds = map.get(toks.get(0));
                    }
                    if (bounds == null) {
                        List<String> toks = StringUtil.splitUpTo(tok, " ", 2);
                        bounds = map.get(toks.get(0));
                    }
                    if (bounds == null) {
                        if (key.toString().length() > 0) {
                            badCnt++;
                            if ( !seen.contains(key)) {
                                System.err.println("No bounds:" + key + " "
                                        + badCnt);
                                seen.add(key);
                            }
                        }
                    }
                    if (bounds != null) {
                        place = new Place("", bounds[0], bounds[1]);
                    }
                }
                if (place != null) {
                    lat = place.getLatitude();
                    lon = place.getLongitude();
                }
            }
            if (writeForDb) {
                add(info, row, lat + ";" + lon);
            } else {
                add(info, row, new Double(lat), new Double(lon));
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
    public static class Populator extends Converter {

        /* */

        /** _more_ */
        private HashSet seen = new HashSet();

        /** _more_ */
        private int badCnt = 0;

        /* */

        /** _more_ */
        private int nameIndex;

        /* */

        /** _more_ */
        private int latIndex;

        /* */

        /** _more_ */
        private int lonIndex;

        /* */

        /** _more_ */
        private Hashtable<String, double[]> map;

        /* */

        /** _more_ */
        private boolean doneHeader = false;

        /** _more_ */
        private String prefix;

        /* */

        /** _more_ */
        private String suffix;



        /*
         * @param cols _more_
         * @param prefix _more_
         * @param suffix _more_
         *
         * @throws Exception _more_
         */

        /**
         * _more_
         *
         * @param cols _more_
         * @param prefix _more_
         * @param suffix _more_
         *
         * @throws Exception _more_
         */
        public Populator(List<String> cols, String prefix, String suffix)
                throws Exception {
            super(cols);
            this.prefix = prefix;
            this.suffix = suffix;
        }



        /**
         *
         *
         *
         *
         *
         * @param info _more_
         * @param row _more_
         * @param line _more_
         *
         * @return _more_
         */
        @Override
        public Row processRow(TextReader info, Row row, String line) {
            List values = row.getValues();
            if ( !doneHeader) {
                add(info, row, "Population");
                doneHeader = true;

                //              System.err.println("pop row:" + row);
                return row;
            }

            List<Integer> indices = getIndices(info);
            StringBuilder key     = new StringBuilder();
            if ((prefix != null) && (prefix.length() > 0)) {
                key.append(prefix);
                key.append(" ");
            }
            boolean didOne = false;
            for (int i : indices) {
                Object value = values.get(i);
                if (didOne) {
                    key.append(", ");
                }
                didOne = true;
                //A hack for US
                if (value.equals("US")
                        || value.toString().startsWith("United States")) {
                    add(info, row, new Integer(327000000));

                    return row;
                }
                key.append(value);
            }

            if ((suffix != null) && (suffix.length() > 0)) {
                key.append(" ");
                key.append(suffix);
            }


            Place place = GeoUtils.getLocationFromAddress(key.toString());
            if (place != null) {
                add(info, row, new Integer(place.getPopulation()));
            } else {
                //              System.out.println("NOT:" + key);
                add(info, row, new Integer(0));
            }

            //      System.err.println("pop row:" + row);
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

        /* */

        /** _more_ */
        private int column;

        /**
         *
         *
         *
         * @param col _more_
         *
         * @throws Exception _more_
         */
        public Genderizer(int col) throws Exception {
            this.column = col;
        }

        /**
         *
         *
         *
         *
         *
         * @param info _more_
         * @param row _more_
         * @param line _more_
         *
         * @return _more_
         */
        @Override
        public Row processRow(TextReader info, Row row, String line) {
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
         *
         *
         *
         *
         *
         * @param mapFile _more_
         * @param col1 _more_
         * @param col2 _more_
         * @param col _more_
         * @param newName _more_
         * @param mode _more_
         *
         * @throws Exception _more_
         */
        public Denormalizer(String mapFile, int col1, int col2, int col,
                            String newName, String mode)
                throws Exception {
            makeMap(mapFile, col1, col2);
            this.destCol    = col;
            this.newColName = newName;
            this.mode       = mode;
            this.doDelete   = mode.endsWith("delete");
        }


        /**
         *
         *
         *
         *
         * @param filename _more_
         * @param col1 _more_
         * @param col2 _more_
         *
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
         *
         *
         *
         *
         *
         * @param info _more_
         * @param row _more_
         * @param line _more_
         *
         * @return _more_
         */
        @Override
        public Row processRow(TextReader info, Row row, String line) {
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
         *
         *
         *
         *
         * @param indices _more_
         */
        public ColumnDeleter(List<String> indices) {
            super(indices);
        }

        /**
         *
         *
         *
         *
         *
         * @param info _more_
         * @param row _more_
         * @param line _more_
         *
         * @return _more_
         */
        @Override
        public Row processRow(TextReader info, Row row, String line) {
            row = filterValues(info, row);

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
         *
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
         *
         *
         *
         *
         *
         * @param info _more_
         * @param row _more_
         * @param line _more_
         *
         * @return _more_
         */
        @Override
        public Row processRow(TextReader info, Row row, String line) {
            List<Integer> indices = getIndices(info);
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

        /**
         *
         *
         *
         *
         * @param cols _more_
         * @param decimals _more_
         */
        public Decimals(List<String> cols, int decimals) {
            super(cols);
            this.tens = (int) Math.pow(10, decimals);
        }

        /**
         *
         *
         *
         *
         *
         * @param info _more_
         * @param row _more_
         * @param line _more_
         *
         * @return _more_
         */
        @Override
        public Row processRow(TextReader info, Row row, String line) {
            List<Integer> indices = getIndices(info);
            for (int i = 0; i < indices.size(); i++) {
                try {
                    int index = indices.get(i);
                    if ((index < 0) || (index >= row.size())) {
                        continue;
                    }
                    double value =
                        Double.parseDouble(row.get(index).toString());
                    value = (double) Math.round(value * tens) / tens;
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
         *
         *
         * @param col _more_
         * @param name _more_
         */
        public ColumnCopier(String col, String name) {
            super(col);
            this.name = name;
        }

        /**
         *
         *
         *
         *
         *
         * @param info _more_
         * @param row _more_
         * @param line _more_
         *
         * @return _more_
         */
        @Override
        public Row processRow(TextReader info, Row row, String line) {
            int index = getIndex(info);
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


        /**
         *
         *
         *
         * @param indices _more_
         * @param delimiter _more_
         */
        public ColumnNewer(List<String> indices, String delimiter) {
            super(indices);
            this.delimiter = delimiter;
        }

        /**
         *
         *
         *
         *
         *
         * @param info _more_
         * @param row _more_
         * @param line _more_
         *
         * @return _more_
         */
        @Override
        public Row processRow(TextReader info, Row row, String line) {
            List<Integer> indices = getIndices(info);
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
    public static class ColumnMathOperator extends Converter {

        /* */

        /** _more_ */
        private String name;

        /* */

        /** _more_ */
        private String op;

        /** _more_          */
        private Row prevRow;

        /**
         *
         * @param indices _more_
         * @param name _more_
         * @param op _more_
         */
        public ColumnMathOperator(List<String> indices, String name,
                                  String op) {
            super(indices);
            this.name = name;
            this.op   = op;
        }

        /**
         * @param info _more_
         * @param row _more_
         * @param line _more_
         *
         * @return _more_
         */
        @Override
        public Row processRow(TextReader info, Row row, String line) {
            if (rowCnt++ == 0) {
                if (op.equals("delta")) {
                    for (Integer idx : indices) {
                        int index = idx.intValue();
                        row.getValues().add("Difference " + row.get(index));
                    }
                } else {
                    row.getValues().add(name);
                }

                return row;
            }
            List<Integer> indices = getIndices(info);
            if (op.equals("delta") && (prevRow == null)) {
                prevRow = row;
                for (int i : indices) {
                    prevRow.add("0");
                }

                return row;
            }
            double value = 0;
            int    cnt   = 0;
            for (Integer idx : indices) {
                int index = idx.intValue();
                if ((index < 0) || (index >= row.size())) {
                    continue;
                }
                if (op.equals("delta")) {
                    double v1 =
                        Double.parseDouble(prevRow.get(index).toString());
                    double v2 = Double.parseDouble(row.get(index).toString());
                    row.add((v2 - v1) + "");

                    continue;
                }
                String s = row.getValues().get(index).toString();
                double v = (s.length() == 0)
                           ? 0
                           : Double.parseDouble(s.replaceAll(",", ""));
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
                        } else if (op.equals("/")) {
                            value = value / v;
                        }
                    }
                }
                cnt++;
            }
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
    public static class Mercator extends Converter {


        /**
         *
         *
         *
         *
         * @param indices _more_
         */
        public Mercator(List<String> indices) {
            super(indices);

        }

        /**
         *
         *
         *
         *
         *
         * @param info _more_
         * @param row _more_
         * @param line _more_
         *
         * @return _more_
         */
        @Override
        public Row processRow(TextReader info, Row row, String line) {
            if (rowCnt++ == 0) {
                row.getValues().add("Latitude");
                row.getValues().add("Longitude");

                return row;
            }
            List<Integer> indices = getIndices(info);
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
         *
         *
         *
         * @param indices _more_
         */
        public ColumnRounder(List<String> indices) {
            super(indices);
        }

        /**
         *
         *
         *
         *
         *
         * @param info _more_
         * @param row _more_
         * @param line _more_
         *
         * @return _more_
         */
        @Override
        public Row processRow(TextReader info, Row row, String line) {
            if (rowCnt++ == 0) {
                if ( !info.getAllData()) {
                    return row;
                }
            }
            List<Integer> indices = getIndices(info);
            for (Integer idx : indices) {
                int index = idx.intValue();
                if ((index < 0) || (index >= row.size())) {
                    continue;
                }
                String s = (String) row.getValues().get(index);
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
     * @version        $version$, Wed, Dec 2, '15
     * @author         Enter your name here...
     */
    public static class Case extends Converter {

        /* */

        /** _more_ */
        String action;

        /**
         *
         *
         *
         *
         * @param indices _more_
         * @param action _more_
         */
        public Case(List<String> indices, String action) {
            super(indices);
            this.action = action;
        }

        /**
         *
         *
         *
         *
         *
         * @param info _more_
         * @param row _more_
         * @param line _more_
         *
         * @return _more_
         */
        @Override
        public Row processRow(TextReader info, Row row, String line) {
            List<Integer> indices = getIndices(info);
            for (Integer idx : indices) {
                int index = idx.intValue();
                if ((index < 0) || (index >= row.size())) {
                    continue;
                }
                String s = (String) row.getValues().get(index);
                if (s == null) {
                    return row;
                }
                if (action.equals("lower")) {
                    s = s.toLowerCase();
                } else if (action.equals("upper")) {
                    s = s.toUpperCase();
                } else if (action.equals("camel")) {
                    s = Utils.upperCaseFirst(s);
                }
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
    public static class ColumnInserter extends Converter {

        /** _more_ */
        private int col;

        /** _more_ */
        private List<String> values;

        /**
         *
         *
         *
         *
         * @param col _more_
         * @param value _more_
         */
        public ColumnInserter(int col, String value) {
            this.col    = col;
            this.values = StringUtil.split(value, ",", false, false);
        }

        /**
         * @param info _more_
         * @param row _more_
         * @param line _more_
         *
         * @return _more_
         */
        @Override
        public Row processRow(TextReader info, Row row, String line) {
            String v = "";
            if (rowCnt < values.size()) {
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
         *
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
         *
         *
         *
         *
         *
         * @param info _more_
         * @param row _more_
         * @param line _more_
         *
         * @return _more_
         */
        @Override
        public Row processRow(TextReader info, Row row, String line) {
            if (rowCnt++ == 0) {
                add(info, row, label);

                return row;
            }
            if (value == (int) value) {
                add(info, row, "" + ((int) value));
            } else {
                add(info, row, "" + value);
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
         *
         *
         *
         *
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
         *
         *
         *
         *
         *
         * @param info _more_
         * @param row _more_
         * @param line _more_
         *
         * @return _more_
         */
        @Override
        public Row processRow(TextReader info, Row row, String line) {
            if ((rowCnt++ == 0) && !label.equals("none")) {
                row.getValues().add(label);

                return row;
            }
            if (value == null) {
                for (String hline : info.getHeaderLines()) {
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
         *
         *
         *
         *
         *
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
         *
         *
         *
         *
         *
         * @param info _more_
         * @param row _more_
         * @param line _more_
         *
         * @return _more_
         */
        @Override
        public Row processRow(TextReader info, Row row, String line) {
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
         *
         *
         *
         *
         * @param row _more_
         * @param cols _more_
         */
        public ColumnUnNudger(int row, List<String> cols) {
            this.cols   = Utils.toInt(cols);
            this.rowIdx = row;
        }

        /**
         *
         *
         *
         *
         *
         * @param info _more_
         * @param row _more_
         * @param line _more_
         *
         * @return _more_
         */
        @Override
        public Row processRow(TextReader info, Row row, String line) {
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
         *
         *
         *
         *
         *
         * @param cols _more_
         * @param rows _more_
         * @param value _more_
         */
        public ColumnSetter(List<String> cols, List<String> rows,
                            String value) {
            this.cols  = Utils.toInt(cols);
            this.rows  = Utils.toInt(rows);
            this.value = value;
        }

        /**
         *
         *
         *
         *
         *
         * @param info _more_
         * @param row _more_
         * @param line _more_
         *
         * @return _more_
         */
        @Override
        public Row processRow(TextReader info, Row row, String line) {
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
         *
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
         * @param info _more_
         * @param row _more_
         * @param line _more_
         *
         * @return _more_
         */
        @Override
        public Row processRow(TextReader info, Row row, String line) {
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
         *
         *
         */
        public Letter() {}

        /**
         *
         *
         *
         *
         *
         * @param info _more_
         * @param row _more_
         * @param line _more_
         *
         * @return _more_
         */
        @Override
        public Row processRow(TextReader info, Row row, String line) {
            cnt++;
            if (cnt == 1) {
                add(info, row, "label");

                return row;

            }
            String letter = getLabel(cnt - 2);
            add(info, row, letter);

            return row;
        }

        /**
         *
         *
         *
         *
         * @param n _more_
         *
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
     * @version        $version$, Wed, Dec 2, '15
     * @author         Enter your name here...
     */
    public static class ColumnPatternSetter extends Converter {

        /* */

        /** _more_ */
        private int patternCol;

        /* */

        /** _more_ */
        private String pattern;

        /* */

        /** _more_ */
        private int writeCol;

        /* */

        /** _more_ */
        private String what;



        /**
         *
         *
         *
         *
         *
         * @param col1 _more_
         * @param pattern _more_
         * @param col2 _more_
         * @param what _more_
         */
        public ColumnPatternSetter(int col1, String pattern, int col2,
                                   String what) {
            this.patternCol = col1;
            this.pattern    = pattern;
            this.writeCol   = col2;
            this.what       = what;
        }

        /**
         *
         *
         *
         *
         *
         * @param info _more_
         * @param row _more_
         * @param line _more_
         *
         * @return _more_
         */
        @Override
        public Row processRow(TextReader info, Row row, String line) {
            if (rowCnt++ == 0) {
                return row;
            }
            String v = row.get(patternCol).toString();
            if (v.matches(pattern) || (v.indexOf(pattern) >= 0)) {
                row.set(writeCol, what);
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
        String              s       = "1*x+y";
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine        engine  = manager.getEngineByName("nashorn");
        engine.put("x", 1);
        engine.put("y", 2);
        // evaluate JavaScript code
        System.err.println(engine.eval(s));
    }


}
