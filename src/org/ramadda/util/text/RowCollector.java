/*
 * Copyright (c) 2008-2021 Geode Systems LLC
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



import org.ramadda.util.IO;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Json;
import org.ramadda.util.MapProvider;
import org.ramadda.util.Utils;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.io.*;


import java.text.SimpleDateFormat;

import java.util.function.Consumer;
import java.util.function.BiConsumer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
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
public  class RowCollector extends Processor {

    /** _more_ */
    private List<Row> rows = new ArrayList<Row>();


    /**
     * _more_
     *
     */
    public RowCollector() {}

    /**
     * _more_
     *
     * @param col _more_
     */
    public RowCollector(String col) {
	super(col);
    }

    /**
     * _more_
     *
     * @param cols _more_
     */
    public RowCollector(List<String> cols) {
	super(cols);
    }


    public void finish(TextReader ctx) throws Exception {
	Processor nextProcessor = getNextProcessor();
	//	System.err.println("RowCollector.finish:" + rows.size() +" next:" + nextProcessor.getClass().getSimpleName());
	List<Row>rows =  finish(ctx, this.rows);
	//	System.err.println("RowCollector.finish finished rows:" + rows.size());
	if(nextProcessor!=null) {
	    for(Row row: rows) {
		row  = nextProcessor.handleRow(ctx, row);
	    }
	    nextProcessor.finish(ctx);
	}
    }
	    

    public List<Row> finish(TextReader ctx, List<Row> rows) throws Exception {
	return rows;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public boolean buffersRows() {
	return true;
    }

    /**
     * _more_
     */
    @Override
    public void reset() {
	super.reset();
	rows = new ArrayList<Row>();
    }

    public Row handleRow(TextReader ctx, Row row) throws Exception {
	//Here we don't call nextProcessor.handleRow
	setHeaderIfNeeded(row);
	row = processRow(ctx, row);
	return row;
    }


    /**
     * _more_
     *
     *
     * @param info _more_
     * @param row _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public Row processRow(TextReader info, Row row) throws Exception {
	rows.add(row);
	return row;
    }

    /**
     * _more_
     *
     * @param row _more_
     */
    public void addRow(Row row) {
	rows.add(row);
    }


    /**
     *  Set the Rows property.
     *
     *  @param value The new value for Rows
     */
    public void setRows(List<Row> value) {
	rows = value;
    }

    /**
     *  Get the Rows property.
     *
     *  @return The Rows
     */
    public List<Row> getRows() {
	return rows;
    }

    /**
     * _more_
     *
     * @param incoming _more_
     *
     * @return _more_
     */
    public List<Row> getRows(List<Row> incoming) {
	if (incoming != null) {
	    return incoming;
	}

	return rows;
    }

    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Jan 9, '15
     * @author         Jeff McWhirter
     */
    public static class RowOperator extends RowCollector {

        /** _more_ */
        public static final int OP_SUM = 0;

        /** _more_ */
        public static final int OP_MIN = 1;

        /** _more_ */
        public static final int OP_MAX = 2;

        /** _more_ */
        public static final int OP_AVERAGE = 3;

        /** _more_ */
        public static final int OP_COUNT = 3;


        /** _more_ */
        private int op = OP_SUM;

        /** _more_ */
        private String opLabel;

        /** _more_ */
        private List<String> valueCols;


        /**
         * _more_
         *
         *
         * @param keys _more_
         * @param values _more_
         * @param op _more_
         */
        public RowOperator(List<String> keys, List<String> values,
                           String op) {
            super(keys);
            this.opLabel = op;
            if (op.equals("sum")) {
                this.op = OP_SUM;
            } else if (op.equals("average")) {
                this.op = OP_AVERAGE;
            } else if (op.equals("min")) {
                this.op = OP_MIN;
            } else if (op.equals("max")) {
                this.op = OP_MAX;
            } else if (op.equals("count")) {
                this.op = OP_COUNT;
            }
            this.valueCols = values;
        }

        /**
         * _more_
         *
         * @param info _more_
         * @param r _more_
         *
         *
         * @return _more_
         * @throws Exception On badness
         */
        @Override
        public List<Row> finish(TextReader info, List<Row> r)
	    throws Exception {
            List          keys         = new ArrayList();
            List<Integer> valueIndices = getIndices(info, valueCols);
            List<Row>     rows         = new ArrayList<Row>();
            List<Row>     allRows      = getRows();
            Row           headerRow    = allRows.get(0);
            allRows.remove(0);
            Hashtable<Object, List<Row>> groups = groupRows(allRows,
							    getIndices(info), keys);
            for (int idx : valueIndices) {
                if (idx >= headerRow.size()) {
                    continue;
                }
                headerRow.set(idx, headerRow.get(idx) + " " + opLabel);
            }
            rows.add(headerRow);
            List<double[]> tuples = new ArrayList<double[]>();
            for (int i = 0; i < valueIndices.size(); i++) {
                tuples.add(new double[] { 0, 0, 0, 0 });
            }
            for (Object key : keys) {
                for (int i = 0; i < valueIndices.size(); i++) {
                    double[] tuple = tuples.get(i);
                    tuple[0] = 0;
                    tuple[1] = 0;
                    tuple[2] = 0;
                    tuple[3] = 0;
                }
                Row       aggRow = null;
                List<Row> group  = groups.get(key);
                int       count  = 0;
                for (Row row : group) {
                    boolean first = false;
                    if (aggRow == null) {
                        aggRow = new Row(row);
                        rows.add(aggRow);
                        first = true;
                    }
                    for (int i = 0; i < valueIndices.size(); i++) {
                        int idx = valueIndices.get(i);
                        if (idx >= row.size()) {
                            continue;
                        }
                        double v =
                            Double.parseDouble(row.get(idx).toString());
                        if (Double.isNaN(v)) {
                            continue;
                        }
                        double[] tuple = tuples.get(i);
                        tuple[0]++;
                        tuple[1] = first
			    ? v
			    : Math.min(v, tuple[0]);
                        tuple[2] = first
			    ? v
			    : Math.max(v, tuple[1]);
                        tuple[3] += v;
                    }
                }
                for (int i = 0; i < valueIndices.size(); i++) {
                    int idx = valueIndices.get(i);
                    if (idx >= aggRow.size()) {
                        continue;
                    }
                    double[] tuple = tuples.get(i);
                    if (op == OP_SUM) {
                        aggRow.set(idx, new Double(tuple[3]));
                    } else if (op == OP_COUNT) {
                        aggRow.set(idx, group.size());
                    } else if (op == OP_MIN) {
                        aggRow.set(idx, tuple[1]);
                    } else if (op == OP_MAX) {
                        aggRow.set(idx, tuple[2]);
                    } else if (op == OP_AVERAGE) {
                        if (tuple[0] == 0) {
                            aggRow.set(idx, Double.NaN);
                        } else {
                            aggRow.set(idx, tuple[3] / tuple[0]);
                        }
                    }
                }
            }

            return rows;
        }

    }


    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Jan 9, '15
     * @author         Jeff McWhirter
     */
    public static class Rotator extends RowCollector {

        /**
         * _more_
         *
         */
        public Rotator() {}

        /**
         * _more_
         *
         * @param info _more_
         * @param rows _more_
         *
         *
         * @return _more_
         * @throws Exception On badness
         */
        @Override
        public List<Row> finish(TextReader info, List<Row> rows)
	    throws Exception {
            List<Row> newRows = new ArrayList<Row>();
            for (Row row : getRows()) {
                List values = row.getValues();
                while (values.size() > newRows.size()) {
                    newRows.add(new Row(new ArrayList()));
                }
                for (int i = 0; i < values.size(); i++) {
                    Object value = values.get(i);
                    newRows.get(i).getValues().add(value);
                }
            }

            return newRows;
        }

    }

    /**
     * Class description
     *
     *
     * @version        $version$, Mon, Oct 14, '19
     * @author         Enter your name here...
     */
    public static class MaxValue extends RowCollector {

        /** _more_ */
        String key;

        /** _more_ */
        String value;

        /**
         * _more_
         *
         *
         * @param key _more_
         * @param value _more_
         */
        public MaxValue(String key, String value) {
            this.key   = key;
            this.value = value;
        }

        /**
         * _more_
         *
         * @param info _more_
         * @param rows _more_
         *
         *
         * @return _more_
         * @throws Exception On badness
         */
        @Override
        public List<Row> finish(TextReader info, List<Row> rows)
	    throws Exception {
            Hashtable<String, Row> map      = new Hashtable<String, Row>();
            int                    keyIdx   = getIndex(this.key);
            int                    valueIdx = getIndex(this.value);
            List<Row>              newRows  = new ArrayList<Row>();
            int                    cnt      = 0;
            for (Row row : getRows()) {
                if (cnt++ == 0) {
                    newRows.add(row);

                    continue;
                }
                List   values = row.getValues();
                String v      = (String) row.get(keyIdx);
                Row    maxRow = map.get(v);
                if (maxRow == null) {
                    map.put(v, row);

                    continue;
                }
                String v1      = (String) maxRow.get(valueIdx);
                String v2      = (String) row.get(valueIdx);
                int    compare = v1.compareTo(v2);
                if (compare < 0) {
                    map.put(v, row);
                }
            }
            for (Enumeration keys = map.keys(); keys.hasMoreElements(); ) {
                String key = (String) keys.nextElement();
                newRows.add(map.get(key));
            }

            return newRows;
        }

    }




    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Jan 9, '15
     * @author         Jeff McWhirter
     */
    public static class Flipper extends RowCollector {

        /**
         * ctor
         */
        public Flipper() {}

        /**
         * _more_
         *
         * @param info _more_
         * @param rows _more_
         *
         *
         * @return _more_
         * @throws Exception On badness
         */
        @Override
        public List<Row> finish(TextReader info, List<Row> rows)
	    throws Exception {
            List<Row> newRows = new ArrayList<Row>();
            newRows.add(rows.get(0));
            for (int i = rows.size() - 1; i >= 1; i--) {
                Row row = rows.get(i);
                newRows.add(row);
            }

            return newRows;
        }

    }

        /**
     * Class description
     *
     *
     * @version        $version$, Fri, Jan 16, '15
     * @author         Enter your name here...
     */
    public static class TclWrapper extends RowCollector {


        /** _more_ */
        private String prefix;


        /**
         * _more_
         * @param prefix _more_
         */
        public TclWrapper(String prefix) {
            super();
            this.prefix = prefix;
        }

        /**
         * _more_
         *
         *
         * @param info _more_
         * @param rows _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        public List<Row> finish(TextReader info, List<Row> rows)
	    throws Exception {
            PrintWriter writer = info.getWriter();
            for (Row row : rows) {
                List values = row.getValues();
                writer.print(prefix);
                for (Object o : values) {
                    writer.print(" {" + o + "} ");
                }
                writer.print("\n");
            }

            return rows;
        }

    }



    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Jan 16, '15
     * @author         Enter your name here...
     */
    public static class ToXml extends RowCollector {

        /** _more_ */
        Row header = null;

        /** _more_ */
        String tag;

	String tag2;

        /**
         * _more_
         *
         * @param tag _more_
         */
        public ToXml(String tag, String tag2) {
            super();
	    if(tag==null || tag.trim().length()==0) tag = "rows";
	    if(tag2==null || tag2.trim().length()==0) tag2 = "row";
            this.tag = tag;
	    this.tag2 = tag2;
        }


        /**
         * _more_
         *
         * @param info _more_
         * @param rows _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        @Override
        public List<Row> finish(TextReader info, List<Row> rows)
	    throws Exception {
            PrintWriter writer = info.getWriter();
            writer.println("</" + tag + ">");
            return rows;
        }

        /**
         * _more_
         *
         * @param info _more_
         * @param row _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        @Override
        public Row processRow(TextReader info, Row row) throws Exception {
            PrintWriter writer = info.getWriter();
            if (header == null) {
                header = row;
                writer.println("<" + tag + ">");

                return row;
            }
            writer.println("<" + tag2+">");
            List values = row.getValues();
            for (int i = 0; i < values.size(); i++) {
                Object v = values.get(i);
                String h = (String) header.get(i);
                h = h.trim().toLowerCase().replaceAll(" ",
						      "_").replaceAll("/", "_");
                if (h.length() == 0) {
                    continue;
                }
                writer.print("<" + h + ">");
                writer.print("<![CDATA[");
                writer.print(v.toString().trim());
                writer.print("]]>");
                writer.println("</" + h + ">");
            }
            writer.println("</" + tag2+">");

            return row;
        }

    }





    /**
     * Class description
     *
     *
     * @version        $version$, Wed, Jul 8, '15
     * @author         Enter your name here...
     */
    public static class Exploder extends RowCollector {


        /**
         * ctor
         *
         *
         * @param col _more_
         */
        public Exploder(String col) {
            super(col);
        }



        /**
         * _more_
         *
         *
         * @param info _more_
         * @param rows _more_
         *
         *
         * @return _more_
         * @throws Exception On badness
         */
        @Override
        public List<Row> finish(TextReader info, List<Row> rows)
	    throws Exception {
            int column = getIndex(info);
            if (rows.size() == 0) {
                return rows;
            }
            Hashtable<String, List<Row>> map = new Hashtable<String,
		List<Row>>();
            List<String> keys   = new ArrayList<String>();
            Row          header = rows.get(0);
            for (int i = 1; i < rows.size(); i++) {
                Row       row    = rows.get(i);
                List      values = row.getValues();
                String    value  = values.get(column).toString();
                List<Row> myRows = map.get(value);
                if (myRows == null) {
                    myRows = new ArrayList<Row>();
                    myRows.add(header);
                    map.put(value, myRows);
                    keys.add(value);
                }
                myRows.add(row);
            }


            for (String v : keys) {
                List<Row> myRows = map.get(v);
                String s =
                    IOUtil.cleanFileName(v).toLowerCase().replaceAll(" ",
								     "_").replaceAll("\\.",
										     "_").replaceAll("\\(",
												     "_").replaceAll("\\)", "_");
                s = s.replaceAll("-", "_");
                s = s.replaceAll(",", "_");
                s = s.replaceAll("_+_", "_");
                s = s.replaceAll("\\.", "_");
                s = Utils.removeNonAscii(s);
                String filename = s + ".csv";
                filename = filename.replace("_.", ".");

                //                System.err.println("writing:" + filename);
                PrintWriter writer = new PrintWriter(
						     new FileOutputStream(
									  info.getFilepath(filename)));
                Processor.Printer p = new Processor.Printer(false);
                p.writeCsv(info, writer, myRows);
                writer.close();
            }

            return rows;

        }

    }



    /**
     * Class description
     *
     *
     * @version        $version$, Wed, Jan 17, '18
     * @author         Enter your name here...
     */
    public static class Html extends RowCollector {

        /** _more_ */
        protected int cnt = 0;

        /**
         * _more_
         *
         */
        public Html() {}


        /** _more_ */
        int maxCount = 0;

        /**
         * _more_
         *
         * @param info _more_
         * @param row _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        @Override
        public Row processRow(TextReader info, Row row) throws Exception {
            if (info.getDebug()) {
                return row;
            }
            printRow(info, row, true);

            return row;
        }

        /**
         * _more_
         *
         * @param info _more_
         * @param row _more_
         * @param addCnt _more_
         *
         * @throws Exception _more_
         */
        public void printRow(TextReader info, Row row, boolean addCnt)
	    throws Exception {
            List values = row.getValues();
            if (cnt == 0) {
                info.getWriter().println(
					 "<table  class='stripe hover ramadda-table ramadda-csv-table' >");
            }
            maxCount = Math.max(maxCount, values.size());
            String open  = "<td>";
            String close = "</td>";

            if (cnt == 0) {
                info.getWriter().println("<thead>");
                info.getWriter().println("<tr valign=top>");
                open  = "<th>";
                close = "</th>";
            } else {
                info.getWriter().println("<tr  valign=top>");
            }


            String style = "white-space:nowrap;overflow-x:auto;";
            //Check for the width
            int    lineWidth = 0;
            String s         = "";
            for (int i = 0; i < values.size(); i++) {
                Object value = values.get(i);
                if (value != null) {
                    lineWidth += value.toString().length();
                    s         += " " + values.get(i);
                } else {
                    s += " " + "";
                }
            }
            if (lineWidth > 200) {
                style += "max-width:120px;";
            }

            for (int i = 0; i < values.size(); i++) {
                if ((i == 0) && addCnt) {
                    info.getWriter().print(open);
                    info.getWriter().print("<div style='" + style + "'>");
                    if (cnt == 0) {
                        info.getWriter().print("&nbsp;");
                    } else {
                        info.getWriter().print("#" + cnt);
                    }
                    info.getWriter().print("");
                    info.getWriter().print("</div>");
                    info.getWriter().print(close);
                }
                info.getWriter().print(open);
                info.getWriter().print("<div style='" + style + "'>");
                if (cnt == 0) {
                    info.getWriter().print("#" + i + "&nbsp;");
                    info.getWriter().print("");
                    String label = Utils.makeLabel(""
						   + values.get(i)).replaceAll(" ",
									       "&nbsp;");
                    info.getWriter().print(HU.span(label,
						   HU.attr("title",
							   label.replaceAll("\"",
									    "&quot;"))));
                } else {
                    Object value = values.get(i);
                    String label = ((value == null)
                                    ? ""
                                    : value.toString());
		    //Check for images, hrefs, etc
		    if(label.indexOf("<")>=0) {
			info.getWriter().print(label);
		    } else {
			info.getWriter().print(HU.span(label,
						       HU.attr("title", label)));
		    }
                }
                info.getWriter().print("</div>");
                info.getWriter().print(close);
            }
            info.getWriter().print("\n");
            if (cnt == 0) {
                info.getWriter().println("</tr>");
                info.getWriter().println("</thead>");
                info.getWriter().println("<tbody>");
            } else {
                for (int i = values.size(); i < maxCount; i++) {
                    info.getWriter().print("<td></td>");
                }
                info.getWriter().println("</tr>");
            }
            cnt++;
        }

        /**
         * _more_
         *
         * @param info _more_
         * @param rows _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        @Override
        public List<Row> finish(TextReader info, List<Row> rows)
	    throws Exception {
            if (info.getDebug()) {
                info.getWriter().print("");
                return rows;
            }
            info.getWriter().println("</tbody>");
            info.getWriter().print("</table>");
            return rows;
        }


    }




    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Jan 9, '15
     * @author         Jeff McWhirter
     */
    public static class Unfurler extends RowCollector {

        /** _more_ */
        private int unfurlIndex = -1;

        /** _more_ */
        private String unfurlCol;

        /** _more_ */
        private int uniqueIndex = -1;

        /** _more_ */
        private String uniqueCol;

        /** _more_ */
        //        private int valueIndex;

        private List<Integer> valueIndices;

        /** _more_ */
        private List<String> valueCols;

        /** _more_ */
        private int unitIndex = -1;

        /** _more_ */
        private HashSet<String> seenValue = new HashSet<String>();

        /**
         * _more_
         *
         *
         *
         * @param unfurlIndex _more_
         * @param valueCols _more_
         * @param uniqueIndex _more_
         * @param uniqueCol _more_
         * @param extraCols _more_
         */
        public Unfurler(String unfurlIndex, List<String> valueCols,
                        String uniqueCol, List<String> extraCols) {
            super(extraCols);
            this.unfurlCol = unfurlIndex;
            this.valueCols = valueCols;
            this.unfurlCol = unfurlCol;
            this.uniqueCol = uniqueCol;
        }

        /**
         * _more_
         *
         * @param index _more_
         */
        public void setUnitIndex(int index) {
            unitIndex = index;
        }


        /**
         * _more_
         *
         * @param info _more_
         * @param rows _more_
         *
         *
         * @return _more_
         * @throws Exception On badness
         */
        @Override
        public List<Row> finish(TextReader info, List<Row> rows)
	    throws Exception {

            if (valueIndices == null) {
                valueIndices     = getIndices(info, valueCols);
                this.unfurlIndex = getIndex(unfurlCol);
                this.uniqueIndex = getIndex(uniqueCol);
            }

            List<Integer>   includes     = getIndices(info);
            HashSet<String> seen         = new HashSet<String>();
            int             rowIndex     = 0;
            HashSet<String> newColumnMap = new HashSet<String>();
            List<String>    newColumns   = new ArrayList<String>();
            List<String>    uniques      = new ArrayList<String>();
            Hashtable<String, List<Row>> rowMap = new Hashtable<String,
		List<Row>>();
            List<Row> allRows   = getRows();
            Row       headerRow = allRows.get(0);
            allRows.remove(0);
            Hashtable<String, Integer> indexMap = new Hashtable<String,
		Integer>();
            for (Row row : allRows) {
                List   values      = row.getValues();
                String value       = values.get(unfurlIndex).toString();
                String unfurlValue = value;
                if (unitIndex >= 0) {
                    String unit = values.get(unitIndex).toString();
                    unfurlValue = unfurlValue + ":unit:" + unit;
                }
                if ( !newColumnMap.contains(unfurlValue)) {
                    newColumnMap.add(unfurlValue);
                    if (valueIndices.size() > 1) {
                        for (int valueIdx : valueIndices) {
                            String label = unfurlValue + " - "
				+ headerRow.get(valueIdx);
                            newColumns.add(label);
                        }
                    } else {
                        newColumns.add(unfurlValue);
                    }
                }

                String    uniqueValue = values.get(uniqueIndex).toString();
                List<Row> rowGroup    = rowMap.get(uniqueValue);
                if (rowGroup == null) {
                    rowMap.put(uniqueValue, rowGroup = new ArrayList<Row>());
                    uniques.add(uniqueValue);
                }
                rowGroup.add(row);
                if ( !seenValue.contains(unfurlValue)) {
                    seenValue.add(unfurlValue);
                }
                rowIndex++;
            }


            Collections.sort(newColumns);
            for (int i = 0; i < newColumns.size(); i++) {
                String       v    = newColumns.get(i);
                List<String> toks = Utils.split(v, ":unit:");
                indexMap.put(toks.get(0), new Integer(i));
                indexMap.put(v, new Integer(i));
            }
            List      header  = headerRow.getValues();
            List<Row> newRows = new ArrayList<Row>();
            newColumns.add(0, header.get(uniqueIndex).toString());
            int cnt = 0;
            for (int i : includes) {
                newColumns.add(1 + cnt, (String) header.get(i));
                cnt++;
            }
            for (String u : uniques) {
                Object[] array = new Object[newColumns.size()];
                for (int i = 0; i < array.length; i++) {
                    array[i] = null;
                }
                array[0] = u;
                int  includeCnt = 0;
                List rowValues  = null;
                List firstRow   = null;
                cnt = 0;
                for (Row row : rowMap.get(u)) {
                    rowValues = row.getValues();
                    if (firstRow == null) {
                        firstRow = rowValues;
                    }
                    String colname = rowValues.get(unfurlIndex).toString();
                    if (valueIndices.size() > 1) {
                        for (int valueIndex : valueIndices) {
                            String label = colname + " - "
				+ headerRow.get(valueIndex);
                            Integer idx = indexMap.get(label);
                            if (idx == null) {
                                continue;
                            }
                            String value =
                                rowValues.get(valueIndex).toString();
                            array[1 + includes.size() + idx] = value;
                        }
                    } else {
                        Integer idx = indexMap.get(colname);
                        if (idx == null) {
                            continue;
                        }
                        int    valueIndex = valueIndices.get(0);
                        String value = rowValues.get(valueIndex).toString();
                        array[1 + includes.size() + idx] = value;
                    }
                    cnt++;
                }
                for (int i : includes) {
                    array[1 + includeCnt] = firstRow.get(i);
                    includeCnt++;
                }
                if (newRows.size() == 0) {
                    newRows.add(new Row(newColumns));
                }
                //                System.err.println(new Row(array));
                newRows.add(new Row(array));
                cnt++;
            }

            return newRows;
        }
    }



    /**
     * Class description
     *
     *
     * @version        $version$, Tue, Nov 19, '19
     * @author         Enter your name here...
     */
    public static class Furler extends RowCollector {

        /** _more_ */
        private List<Integer> indices;

        /** _more_ */
        private List<String> cols;

        /** _more_ */
        private Row header;


        /** _more_ */
        private String label1;

        /** _more_ */
        private String label2;

        /**
         * _more_
         *
         * @param cols _more_
         * @param label1 _more_
         * @param label2 _more_
         */
        public Furler(List<String> cols, String label1, String label2) {
            super(cols);
            this.label1 = label1;
            this.label2 = label2;
        }



        /**
         * _more_
         *
         * @param info _more_
         * @param rows _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        public List<Row> finish(TextReader info, List<Row> rows)
	    throws Exception {
            List<Integer>    indices   = getIndices(info);
            HashSet<Integer> indexMap  = Utils.makeHashSet(indices);
            List<Row>        newRows   = new ArrayList<Row>();
            Row              header    = rows.get(0);
            Row              newHeader = new Row();
            newRows.add(newHeader);
            for (int col = 0; col < header.size(); col++) {
                if ( !indexMap.contains(col)) {
                    newHeader.add(header.get(col));
                }
            }
            newHeader.add(label1);
            newHeader.add(label2);
            for (int rowIdx = 1; rowIdx < rows.size(); rowIdx++) {
                Row row = rows.get(rowIdx);
                for (int i : indices) {
                    if ((i < 0) || (i >= row.size())) {
                        continue;
                    }
                    Row newRow = new Row();
                    newRows.add(newRow);
                    for (int col = 0; col < row.size(); col++) {
                        if ( !indexMap.contains(col)) {
                            newRow.add(row.get(col));
                        }
                    }
                    newRow.add(header.get(i));
                    newRow.add(row.get(i));
                }
            }

            return newRows;
        }
    }



    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Jan 9, '15
     * @author         Jeff McWhirter
     */
    public static class Dups extends RowCollector {

        /** _more_ */
        private List<String> columns;

        /**
         * _more_
         *
         * @param columns _more_
         */
        public Dups(List<String> columns) {
            this.columns = columns;
        }


        /**
         * _more_
         *
         * @param info _more_
         * @param rows _more_
         *
         *
         * @return _more_
         * @throws Exception On badness
         */
        @Override
        public List<Row> finish(TextReader info, List<Row> rows)
	    throws Exception {
            List<Row>     newRows = new ArrayList<Row>();
            List<Row>     allRows = getRows();
            List<Integer> cols    = Utils.toInt(columns);
            newRows.add(allRows.get(0));
            Hashtable<String, Row> seen     = new Hashtable<String, Row>();
            HashSet                seenKeys = new HashSet();
            for (int i = 1; i < allRows.size(); i++) {
                Row    row = allRows.get(i);
                String key = "";
                for (int idx : cols) {
                    key += row.get(idx) + "__";
                }
                if (seenKeys.contains(key)) {
                    newRows.add(row);

                    continue;
                }
                Row seenRow = seen.get(key);
                if (seenRow == null) {
                    seen.put(key, row);

                    continue;
                }
                newRows.add(seenRow);
                newRows.add(row);
                seenKeys.add(key);
            }

            return newRows;
        }
    }



    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Jan 9, '15
     * @author         Jeff McWhirter
     */
    public static class Splatter extends RowCollector {

        /** _more_ */
        private String key;

        /** _more_ */
        private String value;

        /** _more_ */
        private String delimiter;

        /** _more_ */
        private String name;

        /**
         * _more_
         * @param key _more_
         * @param value _more_
         * @param delim _more_
         * @param name _more_
         */
        public Splatter(String key, String value, String delim, String name) {
            this.key       = key;
            this.value     = value;
            this.delimiter = delim;
            this.name      = name;
        }


        /**
         * _more_
         *
         * @param info _more_
         * @param rows _more_
         *
         *
         * @return _more_
         * @throws Exception On badness
         */
        @Override
        public List<Row> finish(TextReader info, List<Row> rows)
	    throws Exception {
            List<Row> newRows = new ArrayList<Row>();
            int keyIndex = getIndices(info,
                                      Utils.split(key, ",", true,
						  true)).get(0);
            int valueIndex = getIndices(info,
                                        Utils.split(value, ",", true,
						    true)).get(0);
            List<Row> allRows   = rows;
            Row       headerRow = allRows.get(0);
            headerRow.add(name);
            newRows.add(headerRow);
            allRows.remove(0);
            Hashtable<Object, Row> map = new Hashtable<Object, Row>();
            for (Row row : allRows) {
                Object key      = row.get(keyIndex);
                Row    existing = map.get(key);
                Object value    = row.get(valueIndex);
                if (existing == null) {
                    existing = new Row(row.getValues());
                    map.put(key, existing);
                    newRows.add(existing);
                    existing.add(value);
                } else {
                    for (int i = 0; i < row.size(); i++) {
                        existing.set(i, row.get(i));
                    }
                    existing.set(existing.size() - 1,
                                 existing.get(existing.size() - 1)
                                 + delimiter + value);
                }
                //              System.out.println("key:" + key +" row:" + existing);
            }

            return newRows;
        }
    }


    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Aug 8, '19
     * @author         Enter your name here...
     */
    public static class Breaker extends RowCollector {

        /** _more_ */
        private String label1;

        /** _more_ */
        private String label2;

        /**
         * _more_
         *
         * @param label1 _more_
         * @param label2 _more_
         * @param cols _more_
         */
        public Breaker(String label1, String label2, List<String> cols) {
            super(cols);
            this.label1 = label1;
            this.label2 = label2;
        }



        /**
         * _more_
         *
         * @param info _more_
         * @param rows _more_
         *
         *
         * @return _more_
         * @throws Exception On badness
         */
        @Override
        public List<Row> finish(TextReader info, List<Row> rows)
	    throws Exception {
            HashSet       cols    = new HashSet();
            List<Row>     newRows = new ArrayList<Row>();
            List<Integer> indices = getIndices(info);
            for (int i : indices) {
                cols.add(i);
            }
            Row           headerRow    = rows.get(0);
            List          headerValues = headerRow.getValues();
            Row           newHeader    = new Row();
            List          newValues    = new ArrayList();
            List<Integer> keepIndices  = new ArrayList<Integer>();
            for (int i = 0; i < headerValues.size(); i++) {
                if ( !cols.contains(i)) {
                    newHeader.add(headerValues.get(i));
                    keepIndices.add(i);
                } else {
                    newValues.add(headerValues.get(i));
                }
            }
            newHeader.add(label1);
            newHeader.add(label2);
            newRows.add(newHeader);

            rows.remove(0);
            //state,region_reg,region_census,2015,2014,2013,2012,2011,2010,2009,2008,2007,2006,2005,2004,2003
            //Alabama,Southeast,South,0.356,0.335,0.324,0.33,0.32,0.323,0.316,0.312,0.301,0.294,0.287,0.277,0.284

            for (Row row : rows) {
                List values   = row.getValues();
                int  valueIdx = 0;
                for (Object newValue : newValues) {
                    Row newRow = new Row();
                    newRows.add(newRow);
                    for (int i : keepIndices) {
                        newRow.add(values.get(i));
                    }
                    newRow.add(values.get(indices.get(valueIdx)));
                    newRow.add(newValue);
                    valueIdx++;
                }
            }

            return newRows;
        }
    }



    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Jan 9, '15
     * @author         Jeff McWhirter
     */
    public static class Sorter extends RowCollector {


        /** _more_ */
        private boolean asc = true;

        /**
         * _more_
         *
         *
         * @param index _more_
         *
         * @param col _more_
         * @param asc _more_
         */
        public Sorter(String col, boolean asc) {
            super(col);
            this.asc = asc;
        }


        /**
         * _more_
         *
         * @param info _more_
         * @param rows _more_
         *
         *
         * @return _more_
         * @throws Exception On badness
         */
        @Override
        public List<Row> finish(TextReader info, List<Row> rows)
	    throws Exception {
            int index = getIndex(info);
            rows = new ArrayList<Row>(getRows(rows));
            if (rows.size() == 0) {
                return rows;
            }
            Row headerRow = rows.get(0);
            rows.remove(0);
            Collections.sort(rows, new Row.RowCompare(index, asc));
            rows.add(0, headerRow);

            return rows;
        }
    }


    /**
     * Class description
     *
     *
     * @version        $version$, Wed, Jul 8, '15
     * @author         Enter your name here...
     */
    public static class Stats extends Html {

        /** _more_          */
        private static SimpleDateFormat fmtSdf =
            new SimpleDateFormat("yyyy-MM-dd hh:mm");

        /** _more_          */
        private CsvUtil util;

	private boolean justStats;

        /** _more_          */
        private List<ColStat> cols;

        /** _more_          */
        private Row headerRow;

        /** _more_          */
        int rowCnt = 0;

        /** _more_          */
        private boolean interactive;

        /**
         * ctor
         *
         * @param util _more_
         */
        public Stats(CsvUtil util,boolean justStats) {
            this.util   = util;
	    this.justStats = justStats;
            interactive = util.getInteractive();
        }


        /**
         * _more_
         *
         * @param info _more_
         * @param row _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        @Override
        public Row processRow(TextReader info, Row row) throws Exception {
            rowCnt++;
            if (headerRow == null) {
                headerRow = row;
                return row;
            }
            if (cols == null) {
                cols = new ArrayList<ColStat>();
                for (int i = 0; i < row.size(); i++) {
                    cols.add(new ColStat(util, i<headerRow.size()?headerRow.getString(i):"",
                                         row.getString(i)));
                }
            }
            for (int i = 0; i < row.size(); i++) {
		if(i<cols.size())
		    cols.get(i).addValue(row.getString(i));
            }

            addRow(row);

            return row;
        }


        /**
         * _more_
         *
         * @param info _more_
         * @param rows _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        @Override
        public List<Row> finish(TextReader info, List<Row> rows)
	    throws Exception {

            PrintWriter w = info.getWriter();
	    BiConsumer<String,String> layout = (label,value) -> {
		w.print(HU.tag("table",
			       HU.attrs("class", "left_right_table",
					"cellpadding", "0", "cellspacing",
					"0"), HU.row(HU.col(label)
						     + HU.col(value,
							      HU.attrs("align","right","valign","top")))));
	    };

	    Consumer<ColStat> printUniques = (col) -> {
                        List<Object[]> values = new ArrayList<Object[]>();
                        for (Enumeration keys = col.uniques.keys();
			     keys.hasMoreElements(); ) {
                            Object  key = keys.nextElement();
                            Integer cnt = col.uniques.get(key);
                            values.add(new Object[] { key, cnt });
                        }
                        Comparator comp = new Comparator() {
				public int compare(Object o1, Object o2) {
				    Object[] t1 = (Object[]) o1;
				    Object[] t2 = (Object[]) o2;

				    return ((int) t2[1]) - ((int) t1[1]);
				}
			    };

                        Object[] array = values.toArray();
                        Arrays.sort(array, comp);
                        values = (List<Object[]>) Misc.toList(array); 
			w.print(Utils.plural(values.size(), "unique value"));
			if(justStats)
			    w.print("<div style='margin-right:5px;max-height:300px;overflow-y:auto;'>");			
			else
			    w.print("<div style='margin-right:5px;max-height:100px;overflow-y:auto;'>");			
			w.print("<table width=100% border=0 cellpadding=0 cellspacing=0>");
			int tupleCnt=0;
			String td = "<td style='border:none;padding:0px;padding-left:0px;padding-right:5px;' ";
			for (Object[] tuple : values) {
			    tupleCnt++;
			    if((justStats && tupleCnt>50) || (!justStats && tupleCnt>20)) {
				w.print("<tr>" + td+" colspan=2>...</td></tr>");
				break;
			    }
			    Object key = tuple[0];
			    int    cnt = (Integer) tuple[1];
			    double percent = Math.round(1000.0 * cnt
							/ (double) (rowCnt-1)) / 10;

			    w.print("<tr valign=bottom title='" + percent+"%'>");
			    w.print(td+ " width=1%>" + key+"</td>");
			    w.print(td+" width=1% align=right>" + cnt +"</td>");
			    w.print("<td style='border:none;padding:0px;' ><div style='margin-top:3px;display:inline-block;background:blue;height:1em;width:" + percent +"%;'></div></td>");

			    w.print("</tr>");			    
			}
			w.print("</table>");
			w.print("</div>");

	    };


	    w.println("#rows:" + rowCnt);
	    w.println(HU.SPACE2);
	    w.println("<span id=header></span>");
            if (interactive) {
                w.println(
			  "<table  width='100%' class='stripe hover display nowrap ramadda-table ramadda-csv-table' >");
                w.println("<thead>");
                w.println("<tr valign=top>");
                for (ColStat col : cols) {
                    String typeIcon = "";
                    String tt       = "";
                    if (col.type.equals("string")) {
                        typeIcon = "fas fa-font";
                    } else if (col.type.equals("date")) {
                        typeIcon = "fas fa-calendar";
                    } else if (col.type.equals("enumeration")) {
                        typeIcon = "fas fa-list";
                    } else if (col.type.equals("image")) {
                        typeIcon = "fas fa-image";
                    } else if (col.type.equals("url")) {
                        typeIcon = "fas fa-link";						
                    } else {
                        typeIcon = "fas fa-hashtag";
                    }
                    String type = HU.faIcon(typeIcon, "title", "type: " + col.type, "style", "font-size:10pt;");
                    String name = col.name;
		    String label = Utils.makeLabel(name);
		    String id = Utils.makeID(name);
                    w.println("<th class=csv-id fieldid='" + id
                              + "' nowrap>" + type + "&nbsp;" + label
                              + "</th>");
                }
                w.println("</tr>");
                w.println("<tr valign=top class=th2>");
		for(int i=0;i<cols.size();i++) {
		    ColStat col =  cols.get(i);
		    if (Utils.equalsOne(col.name.trim().toLowerCase(), "latitude","longitude")) {
			ColStat next = i<cols.size()-1?cols.get(i+1):null;
			if(next!=null && Utils.equalsOne(next.name.toLowerCase(),"longitude","latitude")) {
			    ColStat lat = col.name.equalsIgnoreCase("latitude")?col:next;
			    ColStat lon = col.name.equalsIgnoreCase("longitude")?col:next;			   
			    i++;
			    w.println("<th colspan=2>");
			    StringBuilder map = new StringBuilder();
			    MapProvider mp  = util.getMapProvider();
			    if(mp!=null) {
				List<double[]> pts = new ArrayList<double[]>();
				for(int ptIdx=0;ptIdx<lat.pts.size();ptIdx++)
				    pts.add(new double[]{lat.pts.get(ptIdx),lon.pts.get(ptIdx)});
				Hashtable<String,String>props = new Hashtable<String,String>();
				props.put("simple","true");
				props.put("radius","3");				
				mp.makeMap(map,"100%",justStats?"300px":"100px",pts,props);
				w.print(map.toString());
			    }
			    w.println("</th>");
			    continue;
			}
		    }
		    w.println("<th>");
		    if (col.type.equals("numeric")) {
                        layout.accept("min:", "" + col.min);
			layout.accept("max:", "" + col.max);
                        if (col.numMissing > 0) {
                            layout.accept("#missing:",
					  "" + col.numMissing);
                        }
                        if (col.numErrors > 0) {
                            layout.accept("#errors:",   "" + col.numErrors);
                            if (col.sampleError != null) {
                                layout.accept("eg:"
					      + ((col.sampleError.trim().length()
						  == 0)
						 ? "<blank>"
						 : col.sampleError), "");
                            }
                        }
			printUniques.accept(col);
                    } else if (col.type.equals("date")) {
                        if (col.minDate != null) {
			    layout.accept("min:", fmtSdf.format(col.minDate));
                        }
                        if (col.maxDate != null) {
                            layout.accept("max:",fmtSdf.format(col.maxDate));
                        }
                    } else if (col.type.equals("image")) {
                    } else if (col.type.equals("url")) {			
                    } else {
			printUniques.accept(col);
                    }
                    w.println("</th>");
                }
                w.println("</tr>");
                w.println("</thead>");
                w.println("<tbody>");

                rows = getRows(rows);

                for (Row row : rows) {
		    if(!justStats) {
			if (cnt++ > 200) {
			    w.println("<tr><td colspan=" + row.size()
				      + ">...</td></tr>");
			    break;
			}
		    }
                    Row r = new Row();
                    for (int i = 0; i < cols.size(); i++) {
                        ColStat col = cols.get(i);
			if(i<row.size()) 
			    r.add(col.format(row.get(i)));
                    }
		    if(!justStats)
			printRow(info, r, false);
                }
                w.println("</tbody>");
                w.println("</table>");
            } else {
                for (ColStat col : cols) {
                    cnt++;
		    info.getWriter().print("#" + cnt + " ");
                    col.finish(info.getWriter());
                }
            }

            return rows;

        }


        /**
         * Class description
         *
         *
         * @version        $version$, Sat, Apr 3, '21
         * @author         Enter your name here...    
         */
        private static class ColStat {

            /** _more_          */
            String name;

            /** _more_          */
            String label;

            /** _more_          */
            String type;

            /** _more_          */
            String sample;

            /** _more_          */
            SimpleDateFormat sdf;

            /** _more_          */
            String format;

            /** _more_          */
            String sampleError;

            /** _more_          */
            double min = Double.NaN;

            /** _more_          */
            double max = Double.NaN;

            /** _more_          */
            int numErrors = 0;

            /** _more_          */
            int numMissing = 0;

            /** _more_          */
            Hashtable<Object, Integer> uniques = new Hashtable<Object,
		Integer>();

	    List<Double> pts = new ArrayList<Double>();


            /** _more_          */
            Date minDate;

            /** _more_          */
            Date maxDate;

            /**
             * _more_
             *
             * @param util _more_
             * @param n _more_
             * @param sample _more_
             */
            public ColStat(CsvUtil util, String n, String sample) {
                label = name;
		name = n;
                if (name.startsWith("#fields=")) {
                    name = name.substring("#fields=".length());
                }
                String args = StringUtil.findPattern(name, "\\[(.*)\\]");
                name = name.replaceAll("\\[.*\\]", "");
                name  = name.toLowerCase().trim();
                try {
                    Double.parseDouble(sample);
                    this.type = "numeric";
                } catch (Exception ignore) {
		    if(sample.startsWith("http")) {
			if(name.equals("image")) this.type = "image";
			else this.type = "url";
		    } else {
			this.type = "string";
		    }
                }
                if (args != null) {
                    Hashtable props = Utils.parseKeyValue(args);
                    this.type  = Utils.getProperty(props, "type", this.type);
                    this.label = Utils.getProperty(props, "label",
						   this.label);
                    this.format = Utils.getProperty(props, name +".format",Utils.getProperty(props, "format",
											     this.format));
                    if (this.format != null) {
                        sdf = new SimpleDateFormat(this.format);

                    }
                }
            }

            /**
             * _more_
             *
             * @param v _more_
             *
             * @return _more_
             */
            public Date getDate(Object v) {
                try {
                    if (format.equals("SSS")) {
                        return new Date(Long.parseLong(v.toString()) * 1000);
                    }

                    return sdf.parse(v.toString());
                } catch (Exception exc) {
                    return null;
                }
            }

            /**
             * _more_
             *
             * @param v _more_
             *
             * @return _more_
             */
            public Object format(Object v) {
                if (type.equals("date") && (format != null)) {
                    Date date = getDate(v);
                    if (date != null) {
                        return fmtSdf.format(date);
                    }

                    return "bad date:" + v;
                } else if (type.equals("image")) {
		    String url = v.toString().trim();
		    if(url.length()!=0) {
			return HU.href(url, HU.image(url,"width","100"), "target=_image");
		    }
		}
                return v;
            }

            /**
             * _more_
             *
             * @param v _more_
             */
            public void addValue(String v) {
                if (sample == null) {
                    sample = v;
                }
		if(name.equals("latitude") || name.equals("longitude")) {
		    try {
			pts.add(Double.parseDouble(v));
		    } catch (Exception exc) {
                        numErrors++;
                        if (sampleError == null) {
                            sampleError = v;
                        }
                    }
		}
                if (type.equals("numeric")) {
                    try {
                        double d = Double.parseDouble(v);
                        min = Utils.min(min, d);
                        max = Utils.max(max, d);
                        if (Double.isNaN(d)) {
                            numMissing++;
                        }
                    } catch (Exception exc) {
                        numErrors++;
                        if (sampleError == null) {
                            sampleError = v;
                        }
                    }
                } else if (type.equals("date")) {
                    Date date = getDate(v);
                    if (date != null) {
                        minDate = Utils.min(minDate, date);
                        maxDate = Utils.max(maxDate, date);
                    }
                } 
		Integer cnt = uniques.get(v);
		if (cnt == null) {
		    cnt = new Integer(0);
		}
		cnt = new Integer(cnt + 1);
		uniques.put(v, cnt);
            }

            /**
             * _more_
             *
             * @param writer _more_
             */
            public void finish(PrintWriter writer) {
                writer.print("<pre>");
                writer.print(name + " [" + type + "]  ");
                writer.print("sample:" + sample + "  ");
                if (type.equals("numeric")) {
                    writer.print("min:" + min + "  max:" + max);
                    writer.print("  #missing:" + numMissing + "  #errors:"
                                 + numErrors);
                    if (sampleError != null) {
                        writer.print("  eg:"
                                     + ((sampleError.trim().length() == 0)
                                        ? "<blank>"
                                        : sampleError));
                    }
                } else {
                    writer.print("#uniques:" + uniques.size());
                }
                writer.print("\n");
                writer.print("</pre>");
            }

        }
	


    }



    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Jan 9, '15
     * @author         Jeff McWhirter
     */
    public static class Summer extends RowCollector {

        /** _more_ */
        private List<Integer> uniqueIndices;

        /** _more_ */
        private List<Integer> valueIndices;

        /** _more_ */
        private List<Integer> extraIndices;

        /** _more_ */
        private List<String> keys;

        /** _more_ */
        private List<String> values;

        /** _more_ */
        private List<String> extra;

        /**
         * _more_
         *
         *
         *
         * @param keys _more_
         * @param values _more_
         * @param extra _more_
         */
        public Summer(List<String> keys, List<String> values,
                      List<String> extra) {
            this.keys   = keys;
            this.values = values;
            this.extra  = extra;
        }


        /**
         * _more_
         *
         * @param info _more_
         * @param rows _more_
         *
         *
         * @return _more_
         * @throws Exception On badness
         */
        @Override
        public List<Row> finish(TextReader info, List<Row> rows)
	    throws Exception {

            uniqueIndices = getIndices(info, keys);
            valueIndices  = getIndices(info, values);
            valueIndices  = getIndices(info, values);
            extraIndices  = getIndices(info, extra);
            List<Integer> allIndices = new ArrayList<Integer>();
            allIndices.addAll(uniqueIndices);
            allIndices.addAll(valueIndices);
            HashSet<Integer> allIndicesMap =
                (HashSet<Integer>) Utils.makeHashSet(allIndices);

            int          rowIndex = 0;
            List<String> keys     = new ArrayList<String>();
            Hashtable<String, List<Row>> rowMap = new Hashtable<String,
		List<Row>>();
            Hashtable<String, Row> origMap   = new Hashtable<String, Row>();
            List<Row>              allRows   = getRows(rows);
            Row                    headerRow = allRows.get(0);
            Row                    firstRow  = allRows.get(0);
            allRows.remove(0);
            //            Collections.sort(allRows,new Row.RowCompare(uniqueIndex));
            Object[] array = null;
            for (Row row : allRows) {
                List          values = row.getValues();
                StringBuilder keySB  = new StringBuilder();
                for (int i : uniqueIndices) {
                    if ((i >= -0) && (i < values.size())) {
                        keySB.append(values.get(i));
                        keySB.append("_");
                    }
                }
                String    key      = keySB.toString();
                List<Row> rowGroup = rowMap.get(key);
                if (rowGroup == null) {
                    origMap.put(key, row);
                    rowMap.put(key, rowGroup = new ArrayList<Row>());
                    keys.add(key);
                }
                rowGroup.add(row);
                if (array == null) {
                    array = new Object[row.getValues().size()];
                }
            }


            List<Row> newRows   = new ArrayList<Row>();
            Row       newHeader = new Row();
            for (int i = 0; i < headerRow.size(); i++) {
                if (allIndicesMap.contains(new Integer(i))) {
                    newHeader.add(headerRow.get(i));
                }
            }

            if (valueIndices.size() == 0) {
                newHeader.add("Count");
            }
            for (int j : extraIndices) {
                newHeader.add(firstRow.get(j));
            }
            newRows.add(newHeader);
            for (String key : keys) {
                Row orig = origMap.get(key);
                if (valueIndices.size() == 0) {
                    //just count them
                    List<Row> rowGroup = rowMap.get(key);
                    Row       row      = rowGroup.get(0);
                    Row       newRow   = new Row();
                    newRows.add(newRow);
                    for (int i : uniqueIndices) {
                        newRow.add(row.get(i));
                    }
                    newRow.add(rowGroup.size());
                    for (int j : extraIndices) {
                        newRow.add(orig.get(j));
                    }

                    continue;
                }
                for (int i = 0; i < array.length; i++) {
                    array[i] = null;
                }
                Row newRow = null;
                for (int i = 0; i < valueIndices.size(); i++) {
                    int    valueIdx = valueIndices.get(i);
                    double sum      = 0;
                    for (Row row : rowMap.get(key)) {
                        if (newRow == null) {
                            newRow = new Row();
                            for (int u = 0; u < uniqueIndices.size(); u++) {
                                newRow.add(row.get(uniqueIndices.get(u)));
                            }
                            newRows.add(newRow);
                        }
                        Object value = row.get(valueIdx);
                        if (value == null) {
                            continue;
                        }
                        sum += Double.parseDouble(value.toString());
                    }
                    newRow.add(new Double(sum));
                }
                for (int j : extraIndices) {
                    newRow.add(orig.get(j));
                }
            }

            return newRows;


        }




    }


    /**
     * Class description
     *
     *
     * @version        $version$, Sun, Apr 5, '20
     * @author         Enter your name here...
     */
    public static class GroupFilter extends RowCollector {

        /** _more_ */
        private int op;

        /** _more_ */
        private String value;

        /** _more_ */
        private int valueIdx;

        /**
         * _more_
         *
         * @param cols _more_
         * @param valueIdx _more_
         * @param op _more_
         * @param value _more_
         */
        public GroupFilter(List<String> cols, int valueIdx, int op,
                           String value) {
            super(cols);
            this.op       = op;
            this.valueIdx = valueIdx;
            this.value    = value;
        }


        /**
         * _more_
         *
         * @param v _more_
         *
         * @return _more_
         */
        private double getValue(String v) {
            try {
                return Double.parseDouble(v);
            } catch (Exception e) {
                return Double.NaN;
            }
        }


        /**
         * _more_
         *
         * @param info _more_
         * @param finalRows _more_
         *
         *
         * @return _more_
         * @throws Exception On badness
         */
        @Override
        public List<Row> finish(TextReader info, List<Row> finalRows)
	    throws Exception {
            List<Integer> indices   = getIndices(info);
            List<Row>     allRows   = getRows();
            List<Row>     newRows   = new ArrayList<Row>();
            Row           headerRow = allRows.get(0);
            newRows.add(headerRow);
            allRows.remove(0);
            List keys = new ArrayList();
            Hashtable<Object, List<Row>> rowMap = new Hashtable<Object,
		List<Row>>();
            for (Row row : allRows) {
                List          values = row.getValues();
                StringBuilder key    = new StringBuilder();
                for (int idx : indices) {
                    key.append(values.get(idx).toString());
                    key.append("-");
                }
                String    k    = key.toString();
                List<Row> rows = rowMap.get(k);
                if (rows == null) {
                    keys.add(k);
                    rows = new ArrayList<Row>();
                    rowMap.put(k, rows);
                }
                rows.add(row);
            }
            double dv = 0;
            try {
                dv = Double.parseDouble(value);
            } catch (Exception ignore) {}
            ;
            int cnt = 0;
            for (Object key : keys) {
                List<Row> rows = rowMap.get(key);
                //              System.out.println(key +" " + rows.size());
                boolean ok = false;
                for (Row row : rows) {
                    cnt++;
                    String v = row.getString(valueIdx);
                    if (op == OP_LT) {
                        ok = getValue(v) < dv;
                    } else if (op == OP_LE) {
                        ok = getValue(v) <= dv;
                    } else if (op == OP_GT) {
                        ok = getValue(v) > dv;
                    } else if (op == OP_GE) {
                        ok = getValue(v) >= dv;
                    } else if (op == OP_EQUALS) {
                        ok = getValue(v) == dv;
                    } else if (op == OP_NOTEQUALS) {
                        ok = getValue(v) != dv;
                    } else if (op == OP_DEFINED) {
                        ok = !Double.isNaN(getValue(v));
                    } else if (op == OP_MATCH) {
                        ok = v.matches(value);
                    } else {}
                    if (ok) {
                        break;
                    }
                }
                if (ok) {
                    newRows.addAll(rows);
                }


            }

            return newRows;
        }
    }



    public static class Slicer extends RowCollector {

	String sdest;
	int dest;
	List<String> fill;



        /**
         * _more_
         *
         * @param cols _more_
         * @param colName _more_
         * @param sdf _more_
         */
        public Slicer(List<String> cols, String sdest,List<String> fill) {
	    super(cols);
            this.sdest = sdest;
	    this.fill = fill;
        }

        /**
         * _more_
         *
         * @param info _more_
         * @param row _more_
         *
         * @return _more_
         */
        @Override
        public Row processRow(TextReader info, Row row) throws Exception {
	    super.processRow(info, row);
	    if(sdest!=null) dest = getColumnIndex(sdest);
	    sdest = null;
	    return null;
        }


        /**
         * _more_
         *
         * @param info _more_
         * @param tmp _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        @Override
        public List<Row> finish(TextReader info, List<Row> tmp)
                throws Exception {
	    tmp = getRows(tmp);
	    List<Row> result = new ArrayList<Row>();
	    for(Row row: tmp) {
		result.add(row);
	    }
	    System.err.println("TMP: " + tmp.size());
	    Row sample = tmp.get(1);
	    for(int index: getIndices(info)) {
		for(Row row: tmp) {
		    Object v = row.get(index);
		    row.set(index,"");
		    Row newRow =new Row();
		    for(int i=0;i<dest;i++) newRow.add("");
		    //		    for(Object o : sample.getValue()) newRow.add(o);
		    newRow.add(v);
		    for(String f: fill) newRow.add(f);
		    result.add(newRow);
		}
	    }
	    System.err.println("DONE:" + result.size());
	    return result;
        }

    }




    /**
     * Class description
     *
     *
     * @version        $version$, Sat, Jul 18, '20
     * @author         Enter your name here...
     */
    public static class DateLatest extends RowCollector {

        /** _more_ */
        List<String> keys;

        /** _more_ */
        private Hashtable<String, Row> rows = new Hashtable<String, Row>();

        /** _more_ */
        private List<Integer> indices;

        /** _more_ */
        private List<Integer> keyindices;

        /** _more_ */
        private List<String> keyValues = new ArrayList<String>();

        /** _more_ */
        private String colName;

        /** _more_ */
        private int col = -1;

        /** _more_ */
        private SimpleDateFormat sdf;

        /** _more_ */
        private Row header;

        /**
         * _more_
         *
         * @param cols _more_
         * @param colName _more_
         * @param sdf _more_
         */
        public DateLatest(List<String> cols, String colName,
                          SimpleDateFormat sdf) {
            this.keys    = cols;
            this.colName = colName;
            this.sdf     = sdf;
        }

        /**
         * _more_
         *
         * @param info _more_
         * @param row _more_
         *
         * @return _more_
         */
        @Override
        public Row processRow(TextReader info, Row row) {
            if (rowCnt++ == 0) {
                header = row;
                return null;
            }
            if (keyindices == null) {
                keyindices = getIndices(info, keys);
            }
            debug("date latest.processRow");
            String key = "";
            for (Integer idx : keyindices) {
                int index = idx.intValue();
                if ((index < 0) || (index >= row.size())) {
                    continue;
                }
                key += row.get(index).toString() + "_";
            }
            Row prevRow = rows.get(key);
            if (prevRow == null) {
                prevRow = row;
                rows.put(key, prevRow);
                keyValues.add(key);

                return null;
            }
            try {
                if (col == -1) {
                    col = getColumnIndex(colName);
                }
                Date d1 = sdf.parse(prevRow.get(col).toString());
                Date d2 = sdf.parse(row.get(col).toString());
                if (d2.getTime() >= d1.getTime()) {
                    //              System.err.println("update date:" + row.get(col)+" d:" + d2);
                    rows.put(key, row);
                }

            } catch (Exception exc) {
                System.err.println("Error:" + exc + "\nrow1:" + prevRow
                                   + "\nrow2:" + row);
            }

            return null;
        }



        /**
         * _more_
         *
         * @param info _more_
         * @param tmp _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        @Override
        public List<Row> finish(TextReader info, List<Row> tmp)
                throws Exception {
            debug("date latest.finish");
            List<Row> result = new ArrayList<Row>();
            result.add(header);
            for (String key : keyValues) {
                result.add(rows.get(key));
            }

            return result;
        }

    }




}


