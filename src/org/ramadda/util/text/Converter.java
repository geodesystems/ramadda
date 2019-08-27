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


import org.ramadda.util.GeoUtils;
import org.ramadda.util.Place;


import org.json.*;
import org.ramadda.util.Utils;


import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.io.*;

import java.text.DateFormat;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;


import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import java.util.regex.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Class description
 *
 *
 * @version        $version$, Fri, Jan 9, '15
 * @author         Jeff McWhirter
 */
public abstract class Converter extends Processor {





    /**
     * _more_
     */
    public Converter() {}

    /**
     * _more_
     *
     * @param col _more_
     */
    public Converter(String col) {
        super(col);
    }

    /**
     * _more_
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
         * _more_
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
         * _more_
         *
         * @param cols _more_
         */
        public ColumnSelector(List<String> cols) {
            super(cols);
        }



        /**
         * _more_
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

	    //	    System.out.println("i:" + indices +" before:" + row.size() + " result:" + result);
            return new Row(result);
        }

    }




    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Jan 9, '15
     * @author         Jeff McWhirter
     */
    public static class ImageSearch extends Converter {
	private String suffix;

        /**
         * _more_
         *
         * @param cols _more_
         */
        public ImageSearch(List<String> cols,String suffix) {
            super(cols);
	    this.suffix = suffix;
        }


        /**
         * _more_
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
		row.add("image");
		return row;
	    }

            List<Integer> indices = getIndices(info);
	    String s="";
            for (Integer idx : indices) {
		s+= row.getString(idx)+" ";
            }
	    s+=suffix;
	    //hack, hack
	    String script = "/Users/jeffmc/bin/imagesearch.sh";
	    try {
		s = s.replace(" ","%s");
		Process p = Runtime.getRuntime()
		    .exec(new String[]{"sh", script,s});
		String result = IOUtil.readContents(p.getInputStream()).trim();
		System.err.println("image:" + s);
		JSONObject obj      = new JSONObject(result);
		JSONArray  values = obj.getJSONArray("value");
		if(values.length()==0) {
		    row.add("");
		    return row;
		}
		JSONObject value = values.getJSONObject(0);
		row.add(value.optString("contentUrl",""));
		//		System.err.println(row);
		//		System.exit(0);
	    } catch(Exception exc) {
		throw new RuntimeException(exc);
	    }
	    return row;
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

        /** _more_ */
        int size;

        /**
         * _more_
         *
         * @param cols _more_
         * @param size _more_
         */
        public ColumnWidth(List<String> cols, int size) {
            super(cols);
            this.size = size;
        }



        /**
         * _more_
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

        /** _more_ */
        private DecimalFormat format;

        /**
         * _more_
         *
         * @param cols _more_
         * @param fmt _more_
         */
        public ColumnFormatter(List<String> cols, String fmt) {
            super(cols);
            format = new DecimalFormat(fmt);
        }



        /**
         * _more_
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

        /** _more_ */
        private int count;

        /** _more_ */
        private String pad;

        /**
         * _more_
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
         * _more_
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



        /** _more_ */
        private String pad;

        /**
         * _more_
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
         * _more_
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

        /** _more_ */
        private String pad;

        /**
         * _more_
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
         * _more_
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

        /** _more_ */
        private boolean asPoint = false;

        /**
         * _more_
         *
         */
        public PrintHeader() {}

        /**
         * _more_
         *
         * @param asPoint _more_
         */
        public PrintHeader(boolean asPoint) {
            this.asPoint = asPoint;
        }


        /**
         * _more_
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

        /** _more_ */
        Hashtable<String, String> props;

        /** _more_ */
        String defaultType = "string";

        /** _more_ */
        boolean defaultChartable = true;


        /** _more_ */
        boolean makeLabel = true;

        /** _more_ */
        boolean toStdOut = false;

        /** _more_ */
        Row firstRow;

        /**
         * _more_
         *
         *
         * @param props _more_
         *
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
         * _more_
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
            rowCnt++;
            if (rowCnt > 2) {
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
                String id =
                    col.replaceAll("\\([^\\)]+\\)", "").replaceAll("-",
                                   "_").trim().toLowerCase().replaceAll(" ",
                                       "_").replaceAll(":", "_");

                id = id.replaceAll("<", "_").replaceAll(">", "_");
                id = id.replaceAll("\\+", "_").replaceAll(
                    "\"", "_").replaceAll("%", "_").replaceAll(
                    "\'", "_").replaceAll("/+", "_").replaceAll(
                    "\\.", "_").replaceAll("_+_", "_").replaceAll(
                    "_+$", "").replaceAll("^_+", "").replaceAll("\\^", "_");

                id = CsvUtil.getDbProp(props, id, "id", id);


                if (label == null) {
                    label = CsvUtil.getDbProp(props, id, "label",
                            (String) null);
                }
                if (makeLabel && (label == null)) {
                    label = Utils.makeLabel(col.replaceAll("\\([^\\)]+\\)",
                            ""));
                }
                String unit = StringUtil.findPattern(col,
                                  ".*?\\(([^\\)]+)\\).*");
                //                    System.err.println ("COL:" + col +" unit: " + unit);
                StringBuffer attrs = new StringBuffer();
                if (label != null) {
                    attrs.append("label=\"" + label + "\" ");
                }
                if (desc != null) {
                    attrs.append(" description=\"" + desc + "\" ");
                }
                if (unit == null) {
                    unit = CsvUtil.getDbProp(props, id, "unit",
                                             (String) null);
                }
                if (unit != null) {
                    attrs.append("unit=\"" + unit + "\" ");

                }
                String  format = null;
                String  type   = defaultType;
                boolean isGeo  = false;

                boolean chartable = CsvUtil.getDbProp(props, id, "chartable",
                                        defaultChartable);
                if (id.indexOf("date") >= 0) {
                    type = "date";
                } else if (id.equals("year")) {
                    type   = "date";
                    format = "yyyy";
                } else if (id.equals("latitude") || id.equals("longitude")) {
                    type      = "double";
                    isGeo     = true;
                    chartable = false;
                } else {
                    try {
                        if (_sample.equals("nan") || _sample.equals("na")) {
                            type = "double";
                        } else if (sample.matches("^(\\+|-)?\\d+$")) {
                            //                            System.out.println(label+" match int");
                            type = "integer";
                        } else if (sample.matches(
                                "^[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?$")) {
                            //                            System.out.println(label+" match double");
                            type = "double";
                        } else {
                            //                            System.out.println(label +" no match:" + sample);
                        }
                    } catch (Exception exc) {}
                }

                type   = CsvUtil.getDbProp(props, id, "type", type);
                format = CsvUtil.getDbProp(props, id, "format", format);

                attrs.append(" type=\"" + type + "\"");
                if (format != null) {
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
            firstRow.setValues(values);
            info.setExtraRow(row);
            row.setSkipTo(this);
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
         * _more_
         *
         * @param cols _more_
         *
         */
        public ColumnPercenter(List<String> cols) {
            super(cols);
        }


        /**
         * _more_
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
     * @version        $version$, Wed, Jan 17, '18
     * @author         Enter your name here...
     */
    public static class ColumnOperator extends Converter {

        /**
         * _more_
         */
        public ColumnOperator() {}

        /**
         * _more_
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

        /** _more_ */
        private boolean isRegex;

        /** _more_ */
        private String pattern;

        /** _more_ */
        private String value;

        /**
         * _more_
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
         * _more_
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
                    row.set(index, s);

                }
            }

            return row;
        }

    }


    public static class ColumnExtracter extends Converter {

	private int col;

        /** _more_ */
        private String pattern;

        private String replace;

        /** _more_ */
        private String name;

        /**
         * _more_
         *
         * @param cols _more_
         * @param pattern _more_
         * @param value _more_
         */
        public ColumnExtracter(int col,  String pattern, String replace,
                             String name) {
	    this.col = col;
	    this.pattern = pattern;
	    this.replace = replace;
	    this.name = name;
        }

        /**
         * _more_
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
		row.add(name);
		return row;
            }
	    String value = row.getString(col);
	    String newValue  = StringUtil.findPattern(value,pattern);
	    if(newValue == null) newValue = "";
	    row.add(newValue);
	    value = value.replaceAll(pattern,replace);
	    row.set(col,value);
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

        /** _more_ */
        int row;

        /** _more_ */
        private String pattern;

        /** _more_ */
        private String value;

        /**
         * _more_
         *
         *
         * @param row _more_
         * @param pattern _more_
         * @param value _more_
         */
        public RowChanger(int row, String pattern, String value) {
            this.pattern = pattern;
            this.value   = value;
            this.row     = row;
        }

        /**
         * _more_
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
            if (rowCnt++ != this.row) {
                if ( !info.getAllData()) {
                    return row;
                }
            }
            for (int i = 0; i < row.size(); i++) {
                String s = row.getString(i);
                s = s.replaceAll(pattern, value);
                row.set(i, s);
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
    public static class RowMerger extends Converter {

        /** _more_ */
        HashSet<Integer> rows = new HashSet<Integer>();

        /** _more_ */
        private String delimiter;

        /** _more_ */
        private String close;

        /** _more_ */
        private Row firstRow;

        /**
         * _more_
         *
         * @param close _more_
         *
         * @param rows _more_
         * @param delimiter _more_
         */
        public RowMerger(List<Integer> rows, String delimiter, String close) {
            this.delimiter = delimiter;
            this.close     = close;
            for (int i = 0; i < rows.size(); i++) {
                this.rows.add(rows.get(i));
            }
        }

        /**
         * _more_
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
                String s  = row.getString(i);
                String ss = firstRow.getString(i);
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

        /** _more_ */
        private String pattern;


        /**
         * _more_
         *
         * @param cols _more_
         * @param pattern _more_
         */
        public ColumnDebugger(List<String> cols, String pattern) {
            super(cols);
            this.pattern = pattern;
        }

        /**
         * _more_
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

        /** _more_ */
        private SimpleDateFormat from;

        /** _more_ */
        private SimpleDateFormat to;

        /**
         * _more_
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
         * _more_
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

        /** _more_ */
        private String name;

        /** _more_ */
        private Hashtable<String, String> map = new Hashtable();


        /**
         * _more_
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
         * _more_
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


        /** _more_ */
        private String delimiter;


        /**
         * _more_
         *
         *
         * @param col _more_
         * @param delimiter _more_
         */
        public ColumnSplitter(String col, String delimiter) {
            super(col);
            this.delimiter = delimiter;
        }

        /**
         * _more_
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
            row.remove(index);
            int colOffset = 0;
            for (String tok : StringUtil.split(row.get(index), delimiter)) {
                row.insert(index + (colOffset++), tok);
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

        /** _more_ */
        private String delimiter;

        /** _more_ */
        private String name;

        /** _more_ */
        private boolean inPlace;

        /**
         * _more_
         *
         *
         * @param name _more_
         * @param inPlace _more_
         *
         * @param indices _more_
         * @param delimiter _more_
         */
        public ColumnConcatter(List<String> indices, String delimiter,
                               String name, boolean inPlace) {
            super(indices);
            this.delimiter = delimiter;
            this.name      = name;
            this.inPlace   = inPlace;
        }

        /**
         * _more_
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
                sb.append(row.getString(i));
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

        /** _more_ */
        private HashSet seen = new HashSet();

        /** _more_ */
        private boolean writeForDb = false;

        /** _more_ */
        private int badCnt = 0;

        /** _more_ */
        private int nameIndex;

        /** _more_ */
        private int latIndex;

        /** _more_ */
        private int lonIndex;

        /** _more_ */
        private Hashtable<String, double[]> map;

        /** _more_ */
        private boolean doneHeader = false;

        /** _more_ */
        private boolean doAddress = false;

        /** _more_ */
        private String prefix;

        /** _more_ */
        private String suffix;

        /** _more_ */
        private String latLabel = "Latitude";

        /** _more_ */
        private String lonLabel = "Longitude";

        /**
         * _more_
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
         * _more_
         *
         * @param cols _more_
         * @param lat _more_
         * @param lon _more_
         * @param prefix _more_
         * @param suffix _more_
         *
         * @throws Exception _more_
         */
        public Geocoder(List<String> cols, String lat, String lon,
                        String prefix, String suffix)
                throws Exception {
            super(cols);
            this.prefix     = prefix;
            this.suffix     = suffix;
            this.writeForDb = false;
            if (lat.length() > 0) {
                latLabel = lat;
            }
            if (lon.length() > 0) {
                lonLabel = lon;
            }
            doAddress = true;
        }

        /**
         * _more_
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
         * _more_
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
         * _more_
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
            if ((info.getRow() % 100) == 0) {
                //                System.err.println ("processed:" + info.getRow());
            }
            List values = row.getValues();
            if ( !doneHeader) {
                if (writeForDb) {
                    values.add("Location");
                } else {
                    values.add(latLabel);
                    values.add(lonLabel);
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
                values.add(lat + ";" + lon);
            } else {
                values.add(new Double(lat));
                values.add(new Double(lon));
            }

            return row;
        }

    }





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

        /** _more_ */
        private boolean doneHeader = false;

        /** _more_ */
        private int column;

        /**
         * _more_
         *
         * @param col _more_
         * @throws Exception _more_
         */
        public Genderizer(int col) throws Exception {
            this.column = col;
        }

        /**
         * _more_
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

        /** _more_ */
        private Hashtable map = new Hashtable();

        /** _more_ */
        int destCol;

        /** _more_ */
        String newColName;


        /** _more_ */
        String mode;

        /** _more_ */
        boolean doDelete;

        /**
         * _more_
         *
         *
         * @param col1 _more_
         * @param col2 _more_
         * @param col _more_
         * @param mapFile _more_
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
         * _more_
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
         * _more_
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
         * _more_
         *
         *
         * @param indices _more_
         */
        public ColumnDeleter(List<String> indices) {
            super(indices);
        }

        /**
         * _more_
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

        /** _more_ */
        private double delta1;

        /** _more_ */
        private double delta2;

        /** _more_ */
        private double scale;


        /**
         * _more_
         *
         * @param col _more_
         * @param delta1 _more_
         * @param scale _more_
         * @param delta2 _more_
         */
        public ColumnScaler(String col, double delta1, double scale,
                            double delta2) {
            super(col);
            this.delta1 = delta1;
            this.delta2 = delta2;
            this.scale  = scale;
        }

        /**
         * _more_
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
            try {
                double value = Double.parseDouble(row.get(index).toString());
                row.set(index,
                        new Double((value + delta1) * scale
                                   + delta2).toString());
            } catch (NumberFormatException nfe) {}

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

        /** _more_ */
        private int tens;

        /**
         * _more_
         *
         * @param cols _more_
         * @param decimals _more_
         */
        public Decimals(List<String> cols, int decimals) {
            super(cols);
            this.tens = (int) Math.pow(10, decimals);
        }

        /**
         * _more_
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

        /** _more_ */
        private String name;


        /**
         * _more_
         *
         * @param col _more_
         * @param name _more_
         */
        public ColumnCopier(String col, String name) {
            super(col);
            this.name = name;
        }

        /**
         * _more_
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

        /** _more_ */
        private String delimiter;


        /**
         * _more_
         *
         * @param indices _more_
         * @param delimiter _more_
         */
        public ColumnNewer(List<String> indices, String delimiter) {
            super(indices);
            this.delimiter = delimiter;
        }

        /**
         * _more_
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

        /** _more_ */
        private String name;

        /** _more_ */
        private String op;


        /**
         * _more_
         *
         * @param op _more_
         *
         * @param indices _more_
         * @param name _more_
         */
        public ColumnMathOperator(List<String> indices, String name,
                                  String op) {
            super(indices);
            this.name = name;
            this.op   = op;
        }

        /**
         * _more_
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
                row.getValues().add(name);

                return row;
            }
            List<Integer> indices = getIndices(info);
            double        value   = 0;
            int           cnt     = 0;
            for (Integer idx : indices) {
                int index = idx.intValue();
                if ((index < 0) || (index >= row.size())) {
                    continue;
                }
                String s = (String) row.getValues().get(index);
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
     * @version        $version$, Wed, Feb 20, '19
     * @author         Enter your name here...
     */
    public static class ColumnRounder extends Converter {



        /**
         * _more_
         *
         * @param indices _more_
         */
        public ColumnRounder(List<String> indices) {
            super(indices);
        }

        /**
         * _more_
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

        /** _more_ */
        String action;

        /**
         * _more_
         *
         * @param action _more_
         *
         * @param indices _more_
         */
        public Case(List<String> indices, String action) {
            super(indices);
            this.action = action;
        }

        /**
         * _more_
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
         * _more_
         *
         * @param col _more_
         *
         * @param value _more_
         */
        public ColumnInserter(int col, String value) {
            this.col    = col;
            this.values = StringUtil.split(value, ",", false, false);
        }

        /**
         * _more_
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
            String v = "";
            if (rowCnt < values.size()) {
                v = values.get(rowCnt);
            } else {
                v = values.get(values.size() - 1);
            }
            rowCnt++;
            if (col < 0) {
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
     * @version        $version$, Mon, Jul 29, '19
     * @author         Enter your name here...
     */
    public static class ColumnMacro extends Converter {

        /** _more_ */
        private Pattern pattern;

        /** _more_ */
        private String template;

        /** _more_ */
        private String label;

        /** _more_ */
        private String value;

        /**
         * _more_
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
         * _more_
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

        /** _more_ */
        private int col;

        /** _more_ */
        private int rowIdx;

        /** _more_ */
        private String value;


        /**
         * _more_
         *
         *
         * @param row _more_
         * @param col _more_
         *
         * @param value _more_
         */
        public ColumnNudger(int row, int col, String value) {
            this.rowIdx = row;
            this.col    = col;
            this.value  = value;
        }

        /**
         * _more_
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

        /** _more_ */
        private List<Integer> cols;

        /** _more_ */
        private int rowIdx;



        /**
         * _more_
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
         * _more_
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

        /** _more_ */
        private List<Integer> cols;

        /** _more_ */
        private List<Integer> rows;

        /** _more_ */
        private String value;


        /**
         * _more_
         *
         * @param cols _more_
         * @param rows _more_
         *
         * @param value _more_
         */
        public ColumnSetter(List<String> cols, List<String> rows,
                            String value) {
            this.cols  = Utils.toInt(cols);
            this.rows  = Utils.toInt(rows);
            this.value = value;
        }

        /**
         * _more_
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
            for (int col : cols) {
                row.getValues().set(col, value);
            }
            //            System.err.println("-set:" + row.getValues());

            return row;

        }

    }

    /**
     * _more_
     *
     * @param args _more_
     */
    public static void main(String[] args) {
        String s = "hello (there) and (here) end";
        s = s.replaceAll("\\(.*?\\)", "");
        System.err.println(s);
    }


}
