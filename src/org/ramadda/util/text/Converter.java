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

import java.util.regex.*;



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
                    //                    if(s.indexOf("ROOFING") >=0) {
                    //                        System.err.println("Line:" + theLine);
                    //                        System.err.println("Cols:" + cols);
                    //                    }
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
     * @version        $version$, Thu, Dec 27, '18
     * @author         Enter your name here...    
     */
    public static class ColumnFormatter extends Converter {

        /** _more_          */
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
     * @version        $version$, Wed, Dec 2, '15
     * @author         Enter your name here...
     */
    public static class HeaderMaker extends Converter {

        /** _more_ */
        private int count = 0;

        /** _more_ */
        Hashtable<String, String> props;

        /** _more_ */
        String defaultType = "double";

        /** _more_ */
        boolean defaultChartable = true;

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
            if (rowCnt++ != 0) {
                return row;
            }
            StringBuffer sb = new StringBuffer();
            sb.append("#fields=");
            List values = new ArrayList<String>();
            for (int i = 0; i < row.getValues().size(); i++) {
                String col = (String) row.getValues().get(i);
                col = col.replaceAll("\u00B5", "u").replaceAll("\u00B3",
                                     "^3").replaceAll("\n", " ");
                String id =
                    col.replaceAll("\\([^\\)]+\\)", "").replaceAll("-",
                                   "_").trim().toLowerCase().replaceAll(" ",
                                       "_").replaceAll(":", "_");

                id = id.replaceAll("\"", "_").replaceAll("\'",
                                   "_").replaceAll("/+",
                                       "_").replaceAll("\\.",
                                           "_").replaceAll("_+_",
                                               "_").replaceAll("_+$",
                                                   "").replaceAll("^_+",
                                                       "").replaceAll("\\^",
                                                           "_");

                id = CsvUtil.getDbProp(props, id, "id", id);


                String label = CsvUtil.getDbProp(props, id, "label",
                                   (String) null);
                if (label == null) {
                    label = Utils.makeLabel(col.replaceAll("\\([^\\)]+\\)",
                            ""));
                }
                String unit = StringUtil.findPattern(col,
                                  ".*?\\(([^\\)]+)\\).*");
                //                    System.err.println ("COL:" + col +" unit: " + unit);
                StringBuffer attrs = new StringBuffer();
                attrs.append("label=\"" + label + "\" ");
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
                } else if (id.equals("latitude") || id.equals("longitude")) {
                    type      = "double";
                    isGeo     = true;
                    chartable = false;
                }

                type   = CsvUtil.getDbProp(props, id, "type", type);
                format = CsvUtil.getDbProp(props, id, "format", format);

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
                attrs.append(" type=\"" + type + "\"");
                String field = id + "[" + attrs + "] ";
                if (i == 0) {
                    field = "#fields=" + field;
                }
                values.add(field);
            }
            row.setValues(values);

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
            this.value   = value;
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
                return row;
            }
            List<Integer> indices = getIndices(info);
            for (Integer idx : indices) {
                int index = idx.intValue();
                if ((index >= 0) && (index < row.size())) {
                    String s = row.getString(index);
                    s = s.replaceAll(pattern, value);
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
                row.getValues().add(name);

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
                    if (newValue != null) {
                        row.getValues().add(newValue);
                    } else {
                        row.getValues().add(na);
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
                row.add(index + (colOffset++), tok);
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
                        row.add(indices.get(0), name);
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

            if (inPlace) {
                row = filterValues(info, row);
                row.add(indices.get(0), sb.toString());
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
        private String suffix;


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
         * @param suffix _more_
         *
         * @throws Exception _more_
         */
        public Geocoder(List<String> cols, String suffix) throws Exception {
            super(cols);
            this.suffix     = suffix;
            this.writeForDb = false;
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
                                        new FileInputStream(filename)));

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
                    values.add("Latitude");
                    values.add("Longitude");
                }
                doneHeader = true;

                return row;
            }

            List<Integer> indices = getIndices(info);
            StringBuilder key     = new StringBuilder();
            for (int i : indices) {
                Object value = values.get(i);
                if (key.length() > 0) {
                    key.append(", ");
                }
                key.append(value);
            }
            if (suffix != null) {
                key.append(" ");
                key.append(suffix);
            }

            double lat = Double.NaN;
            double lon = Double.NaN;
            if (key != null) {
                double[] bounds = null;
                if (doAddress) {
                    bounds = GeoUtils.getLocationFromAddress(key.toString());
                    System.err.println("key:" + key + " b:" + bounds);
                } else {
                    String tok = key.toString();
                    bounds = map.get(tok);
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
                }
                if (bounds != null) {
                    if (bounds.length == 2) {
                        lat = bounds[0];
                        lon = bounds[1];
                    } else {
                        lat = bounds[2] + (bounds[0] - bounds[2]) / 2.0;
                        lon = bounds[1] + (bounds[3] - bounds[1]) / 2.0;
                    }
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

        /**
         * _more_
         *
         *
         * @param col _more_
         * @param mapFile _more_
         *
         * @throws Exception _more_
         */
        public Denormalizer(String mapFile, int col) throws Exception {
            makeMap(mapFile);
            this.destCol = col;
        }


        /**
         * _more_
         *
         * @param filename _more_
         *
         * @throws Exception _more_
         */
        private void makeMap(String filename) throws Exception {
            BufferedReader br = new BufferedReader(
                                    new InputStreamReader(
                                        new FileInputStream(filename)));

            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.length() == 0) {
                    continue;
                }
                if (line.startsWith("#")) {
                    continue;
                }
                List<String> toks = StringUtil.splitUpTo(line, ",", 2);
                map.put(toks.get(0), toks.get(1).replaceAll("\"", ""));
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
            Object key      = values.get(destCol);
            String newValue = (String) map.get(key);
            if (newValue != null) {
                //                if(newValue.indexOf(",")>=0) newValue = "\"" + newValue+"\"";
                values.set(destCol, newValue);
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
     * @version        $version$, Fri, Jan 16, '15
     * @author         Enter your name here...
     */
    public static class ColumnInserter extends Converter {

        /** _more_ */
        private String value;


        /**
         * _more_
         *
         * @param col _more_
         * @param value _more_
         */
        public ColumnInserter(String col, String value) {
            super(col);
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
            int index = getIndex(info);
            if ((index < 0) || (index >= row.size())) {
                return row;
            }
            row.insert(index, value);

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

        /** _more_          */
        private double delta2;

        /** _more_          */
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
                row.add(index + 1, name);
            } else {
                row.add(index + 1, row.getValues().get(index));
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


}
