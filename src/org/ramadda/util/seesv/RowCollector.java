/**
   Copyright (c) 2008-2023 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util.seesv;


import org.ramadda.util.HtmlUtils;



import org.ramadda.util.IO;
import org.ramadda.util.MyDateFormat;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.MapProvider;
import org.ramadda.util.Utils;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.io.*;


import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.*;


/**
 * Class description
 *
 *
 * @version        $version$, Fri, Jan 9, '15
 * @author         Jeff McWhirter
 */
@SuppressWarnings("unchecked")
public class RowCollector extends Processor {

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


    /**
     *
     * @param ctx _more_
     *
     * @throws Exception _more_
     */
    public void finish(TextReader ctx) throws Exception {
        Processor nextProcessor = getNextProcessor();
        //      System.err.println("RowCollector.finish:" + rows.size() +" next:" + nextProcessor.getClass().getSimpleName());
        List<Row> rows = finish(ctx, this.rows);
        //      System.err.println("RowCollector.finish finished rows:" + rows.size());
        if (nextProcessor != null) {
            for (Row row : rows) {
                row = nextProcessor.handleRow(ctx, row);
            }
            nextProcessor.finish(ctx);
        }
    }


    /**
     *
     * @param ctx _more_
     * @param rows _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
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
    public void reset(boolean force) {
        super.reset(force);
        rows = new ArrayList<Row>();
    }

    /**
     *
     * @param ctx _more_
     * @param row _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
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
     * @param ctx _more_
     * @param row _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public Row processRow(TextReader ctx, Row row) throws Exception {
        rows.add(row);
        return row;
    }


    /**
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
        public static final int OP_COUNT = 4;


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
            if (op.equals(OPERAND_SUM)) {
                this.op = OP_SUM;
            } else if (op.equals(OPERAND_AVERAGE)) {
                this.op = OP_AVERAGE;
            } else if (op.equals(OPERAND_MIN)) {
                this.op = OP_MIN;
            } else if (op.equals(OPERAND_MAX)) {
                this.op = OP_MAX;
            } else if (op.equals(OPERAND_COUNT)) {
                this.op = OP_COUNT;
            } else {
                throw new RuntimeException("unknown operator:" + op);
            }
            this.valueCols = values;
        }


        /**
         * _more_
         *
         * @param ctx _more_
         * @param r _more_
         *
         *
         * @return _more_
         * @throws Exception On badness
         */
        @Override
        public List<Row> finish(TextReader ctx, List<Row> r)
	    throws Exception {
            List          keys         = new ArrayList();
            List<Integer> valueIndices = getIndices(ctx, valueCols);
            List<Row>     rows         = new ArrayList<Row>();
            List<Row>     allRows      = getRows();
            Row           headerRow    = allRows.get(0);
            allRows.remove(0);
            Hashtable<Object, List<Row>> groups = groupRows(allRows,
							    getIndices(ctx), keys);

            for (int idx : valueIndices) {
                if (idx >= headerRow.size()) {
                    continue;
                }
                headerRow.set(idx, headerRow.get(idx) + " " + opLabel);
            }
            rows.add(headerRow);
            List<Tuple> tuples = new ArrayList<Tuple>();
            for (int i = 0; i < valueIndices.size(); i++) {
                tuples.add(new Tuple());
            }
            for (Object key : keys) {
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
                        Tuple tuple = tuples.get(i);
			tuple.add(v);
                    }
                }
                for (int i = 0; i < valueIndices.size(); i++) {
                    int idx = valueIndices.get(i);
                    if (idx >= aggRow.size()) {
                        continue;
                    }
                    Tuple tuple = tuples.get(i);
                    if (op == OP_SUM) {
                        aggRow.set(idx, Double.valueOf(tuple.sum));
                    } else if (op == OP_COUNT) {
                        aggRow.set(idx, group.size());
                    } else if (op == OP_MIN) {
                        aggRow.set(idx, tuple.min);
                    } else if (op == OP_MAX) {
                        aggRow.set(idx, tuple.max);
                    } else if (op == OP_AVERAGE) {
                        if (tuple.count == 0) {
                            aggRow.set(idx, Double.NaN);
                        } else {
                            aggRow.set(idx, tuple.sum / tuple.count);
                        }
                    } else {}

                }
            }

            return rows;
        }

    }





    private static  class Count {
	Row sample;
	double min;
	double max;
	int count;
	double[]totals;
	double[]mins;
	double[]maxs; 	    	    

	Count() {}
	Count(Row sample) {
	    this.sample = sample;
	}
	public void addValues(List<Integer> indices, Row row) {
	    count++;
	    if(indices.size()==0) return;
	    if(totals==null)  {
		totals = new double[indices.size()];
		for(int i=0;i<totals.length;i++) totals[i]=0;
		mins = new double[indices.size()];
		for(int i=0;i<mins.length;i++) mins[i]=Double.POSITIVE_INFINITY;
		maxs = new double[indices.size()];
		for(int i=0;i<maxs.length;i++) maxs[i]=Double.NEGATIVE_INFINITY;	    
	    }
	    for(int i=0;i<indices.size();i++) {
		if(!row.indexOk(indices.get(i))) continue;
		double v = Double.parseDouble(row.getString(indices.get(i)));
		if(Double.isNaN(v)) continue;
		totals[i]+=v;
		mins[i] =Math.min(v,mins[i]);
		maxs[i] =Math.max(v,maxs[i]);		    
	    }
	}
    }

    private  static class Bin extends Count {
	String label;


	Bin(double min,double max) {this.min = min;this.max = max;}
	public boolean firstBin() {
	    return (min==Double.NEGATIVE_INFINITY);
	}

	public boolean lastBin() {
	    return (max==Double.POSITIVE_INFINITY);
	}
	    
	public boolean inRange(double v) {
	    if(firstBin()) {
		return v<max;
	    }
	    return v>=min && v<max;
	}

	public String getLabel() {
	    if(label!=null) return label;
	    if(min==Double.NEGATIVE_INFINITY)
		return "<" + Utils.format(max);
	    if(max==Double.POSITIVE_INFINITY)
		return ">" + Utils.format(min);		
	    return Utils.format(min) +" - " + Utils.format(max);		
	}
	public String toString() {
	    return "bin min:" + min + " max:" + max +" count:" + count;
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
        public Rotator(TextReader ctx) {}

        /**
         * _more_
         *
         * @param ctx _more_
         * @param rows _more_
         *
         *
         * @return _more_
         * @throws Exception On badness
         */
        @Override
        public List<Row> finish(TextReader ctx, List<Row> rows)
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
     * @version        $version$, Fri, Jan 9, '15
     * @author         Jeff McWhirter
     */
    public static class RowShuffler extends RowCollector {

        /**  */
        boolean atStart;

        /**  */
        String pattern;

        /**
         * _more_
         *
         *
         * @param atStart _more_
         * @param cols _more_
         * @param pattern _more_
         */
        public RowShuffler(TextReader ctx, boolean atStart, List<String> cols,
                           String pattern) {
            super(cols);
            this.atStart = atStart;
            this.pattern = pattern;
        }

        /**
         * _more_
         *
         * @param ctx _more_
         * @param rows _more_
         *
         *
         * @return _more_
         * @throws Exception On badness
         */
        @Override
        public List<Row> finish(TextReader ctx, List<Row> rows)
	    throws Exception {
            List<Row>     newRows       = new ArrayList<Row>();
            List<Row>     matchedRows   = new ArrayList<Row>();
            List<Row>     unmatchedRows = new ArrayList<Row>();
            List<Integer> indices       = getIndices(ctx);
            Row           header        = null;
            for (Row row : getRows()) {
                if (header == null) {
                    header = row;
                    newRows.add(row);
                    continue;
                }
                boolean matches = false;
                for (int col : indices) {
                    if (row.getString(col).matches(pattern)) {
                        matches = true;

                        break;
                    }
                }
                if (matches) {
                    matchedRows.add(row);
                } else {
                    unmatchedRows.add(row);
                }
            }
            if (atStart) {
                newRows.addAll(matchedRows);
                newRows.addAll(unmatchedRows);
            } else {
                newRows.addAll(unmatchedRows);
                newRows.addAll(matchedRows);
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
        public MaxValue(TextReader ctx, String key, String value) {
            this.key   = key;
            this.value = value;
        }

        /**
         * _more_
         *
         * @param ctx _more_
         * @param rows _more_
         *
         *
         * @return _more_
         * @throws Exception On badness
         */
        @Override
        public List<Row> finish(TextReader ctx, List<Row> rows)
	    throws Exception {
            Hashtable<String, Row> map      = new Hashtable<String, Row>();
            int                    keyIdx   = getIndex(ctx, this.key);
            int                    valueIdx = getIndex(ctx, this.value);
            List<Row>              newRows  = new ArrayList<Row>();
            int                    cnt      = 0;
            for (Row row : getRows()) {
                if (cnt++ == 0) {
                    newRows.add(row);

                    continue;
                }
                List   values = row.getValues();
                String v      = keyIdx<0?"":(String) row.get(keyIdx);
                Row    maxRow = map.get(v);
                if (maxRow == null) {
                    map.put(v, row);

                    continue;
                }
                String v1      = (String) maxRow.get(valueIdx);
                String v2      = (String) row.get(valueIdx);
                int    compare = v1.compareTo(v2);
                if (compare > 0) {
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
     * @version        $version$, Mon, Oct 14, '19
     * @author         Enter your name here...
     */
    public static class MinValue extends RowCollector {

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
        public MinValue(TextReader ctx, String key, String value) {
            this.key   = key;
            this.value = value;
        }

        /**
         * _more_
         *
         * @param ctx _more_
         * @param rows _more_
         *
         *
         * @return _more_
         * @throws Exception On badness
         */
        @Override
        public List<Row> finish(TextReader ctx, List<Row> rows)
	    throws Exception {
            Hashtable<String, Row> map      = new Hashtable<String, Row>();
            int                    keyIdx   = getIndex(ctx, this.key);
            int                    valueIdx = getIndex(ctx, this.value);
            List<Row>              newRows  = new ArrayList<Row>();
            int                    cnt      = 0;
            for (Row row : getRows()) {
                if (cnt++ == 0) {
                    newRows.add(row);
                    continue;
                }
                List   values = row.getValues();
                String v      = keyIdx<0?"":(String) row.get(keyIdx);
                Row    minRow = map.get(v);
                if (minRow == null) {
                    map.put(v, row);

                    continue;
                }
                String v1      = (String) minRow.get(valueIdx);
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
        public Flipper(TextReader ctx) {}

        /**
         * _more_
         *
         * @param ctx _more_
         * @param rows _more_
         *
         *
         * @return _more_
         * @throws Exception On badness
         */
        @Override
        public List<Row> finish(TextReader ctx, List<Row> rows)
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
        public TclWrapper(TextReader ctx, String prefix) {
            super();
            this.prefix = prefix;
        }

        /**
         * _more_
         *
         *
         * @param ctx _more_
         * @param rows _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        public List<Row> finish(TextReader ctx, List<Row> rows)
	    throws Exception {
            PrintWriter writer = ctx.getWriter();
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
        public Exploder(TextReader ctx, String col) {
            super(col);
        }



        /**
         * _more_
         *
         *
         * @param ctx _more_
         * @param rows _more_
         *
         *
         * @return _more_
         * @throws Exception On badness
         */
        @Override
        public List<Row> finish(TextReader ctx, List<Row> rows)
	    throws Exception {
            int column = getIndex(ctx);
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
									  ctx.getFilepath(filename)));
                Processor.Printer p = new Processor.Printer(false);
                p.writeCsv(ctx, writer, myRows);
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
        public Html(TextReader ctx) {
	}


        /** _more_ */
        int maxCount = 0;

        /**
         * _more_
         *
         * @param ctx _more_
         * @param row _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) throws Exception {
            printRow(ctx, row, true,true);
            return row;
        }

        /**
         * _more_
         *
         * @param ctx _more_
         * @param row _more_
         * @param addCnt _more_
         *
         * @throws Exception _more_
         */
        public void printRow(TextReader ctx, Row row, boolean addCnt, boolean even)
	    throws Exception {
            List values = row.getValues();
            if (cnt == 0) {
                ctx.getWriter().println("<table  class='stripe hover ramadda-table ramadda-csv-table' >");
            }
            maxCount = Math.max(maxCount, values.size());
            String open  = "<td>";
            String close = "</td>";
            if (cnt == 0) {
                ctx.getWriter().println("<thead>");
                ctx.getWriter().println("<tr valign=top>");
                open  = "<th>";
                close = "</th>";
            } else {
                ctx.getWriter().println("<tr  valign=top class=" + (even?"ramadda-row-even":"ramadda-row-odd") +">");
            }

            String style = "";
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

	    String cellOpen = HU.open("div",HU.cssClass("seesv-table-cell")+HU.style(style));
            for (int i = 0; i < values.size(); i++) {
                if ((i == 0) && addCnt) {
                    ctx.getWriter().print(open);
                    ctx.getWriter().print(cellOpen);
                    if (cnt == 0) {
                        ctx.getWriter().print("&nbsp;");
                    } else {
                        ctx.getWriter().print("#" + cnt);
                    }
                    ctx.getWriter().print("");
                    ctx.getWriter().print("</div>");
                    ctx.getWriter().print(close);
                }
                if (cnt == 0) {
		    ctx.getWriter().print(open);
		    ctx.getWriter().print(cellOpen);
                    ctx.getWriter().print("#" + i + "&nbsp;");
                    ctx.getWriter().print("");
                    String label = Utils.makeLabel(""
						   + values.get(i)).replaceAll(" ",
									       "&nbsp;");
                    ctx.getWriter().print(HU.span(label,
						  HU.attr("title",
							  label.replaceAll("\"", "&quot;"))));
                } else {
                    Object value = values.get(i);
		    boolean alignRight = false;
		    if(value!=null) {
			alignRight = Utils.isNumber(value.toString());
		    }
		    if(alignRight)
			ctx.getWriter().print("<td align=right>");
		    else
			ctx.getWriter().print(open);		    
		    ctx.getWriter().print(cellOpen);
                    String label = ((value == null)
                                    ? ""
                                    : value.toString());
                    //Check for images, hrefs, etc
                    if (label.indexOf("<") >= 0) {
                        ctx.getWriter().print(label);
                    } else {
			String contents = label;
			if(!Utils.stringDefined(contents)) contents="&nbsp;";
                        ctx.getWriter().print(HU.span(contents,
						      HU.attr("title", label)));
                    }
                }
                ctx.getWriter().print("</div>");
                ctx.getWriter().print(close);
            }
            ctx.getWriter().print("\n");
            if (cnt == 0) {
                ctx.getWriter().println("</tr>");
                ctx.getWriter().println("</thead>");
                ctx.getWriter().println("<tbody>");
            } else {
                for (int i = values.size(); i < maxCount; i++) {
                    ctx.getWriter().print("<td></td>");
                }
                ctx.getWriter().println("</tr>");
            }
            cnt++;
        }

        /**
         * _more_
         *
         * @param ctx _more_
         * @param rows _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        @Override
        public List<Row> finish(TextReader ctx, List<Row> rows)
	    throws Exception {
	    System.err.println("finish");
            if (ctx.getDebug()) {
                ctx.getWriter().print("");

                return rows;
            }
            ctx.getWriter().println("</tbody>");
            ctx.getWriter().print("</table>");

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
        public Unfurler(TextReader ctx, String unfurlIndex, List<String> valueCols,
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
         * @param ctx _more_
         * @param rows _more_
         *
         *
         * @return _more_
         * @throws Exception On badness
         */
        @Override
        public List<Row> finish(TextReader ctx, List<Row> rows)
	    throws Exception {

            if (valueIndices == null) {
                valueIndices     = getIndices(ctx, valueCols);
                this.unfurlIndex = getIndex(ctx, unfurlCol);
                this.uniqueIndex = getIndex(ctx, uniqueCol);
            }

            List<Integer>   includes     = getIndices(ctx);
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
                indexMap.put(toks.get(0), Integer.valueOf(i));
                indexMap.put(v, Integer.valueOf(i));
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

    public static class CountUnique extends RowCollector {

        /**  */
        private Row header;

        /**  */
        private List<String> keys = new ArrayList<String>();

        /**  */
        private Hashtable<String, Row> rowMap = new Hashtable<String, Row>();

        /**  */
        private Hashtable<String, Integer> countMap = new Hashtable<String,
	    Integer>();

        /**
         *
         * @param cols _more_
         */
        public CountUnique(TextReader ctx, List<String> cols) {
            super(cols);
        }

        /**
         *
         * @param ctx _more_
         * @param row _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
	@Override
        public Row processRow(TextReader ctx, Row row) throws Exception {
            if (rowCnt++ == 0) {
                header = row;
                return null;
            }
            List<Integer> indices = getIndices(ctx);
            String        key     = "";
            for (int idx : indices) {
                key += "_" + row.getString(idx);
            }
            Row     sample = rowMap.get(key);
            Integer cnt    = countMap.get(key);
            if (sample == null) {
                keys.add(key);
                rowMap.put(key, row);
                countMap.put(key, 0);
                cnt = 0;
            }
            cnt++;
            countMap.put(key, cnt);

            return null;
        }



        /**
         * _more_
         *
         * @param ctx _more_
         * @param rows _more_
         *
         *
         * @return _more_
         * @throws Exception On badness
         */
        @Override
        public List<Row> finish(TextReader ctx, List<Row> rows)
	    throws Exception {
            rows = new ArrayList<Row>();
            header.add("Count");
            rows.add(header);
            for (String key : keys) {
                Row sample = rowMap.get(key);
                int cnt    = countMap.get(key);
                sample.add(cnt);
                rows.add(sample);
            }

            return rows;
        }
    }

    public static class Normal extends RowCollector {

        /**
         *
         * @param cols _more_
         */
        public Normal(TextReader ctx, List<String> cols) {
            super(cols);
        }

        /**
         * _more_
         *
         * @param ctx _more_
         * @param rows _more_
         *
         *
         * @return _more_
         * @throws Exception On badness
         */
        @Override
        public List<Row> finish(TextReader ctx, List<Row> rows)
	    throws Exception {
            rows = getRows();
            for (int i : getIndices(ctx)) {
                List<KeyValue> values = new ArrayList<KeyValue>();
                for (int rowIdx = 0; rowIdx < rows.size(); rowIdx++) {
                    Row    row = rows.get(rowIdx);
                    String v   = row.getString(i);
                    String key = cleanName(v);
                    values.add(new KeyValue(rowIdx, key, v));
                }
                for (int rowIdx1 = 1; rowIdx1 < rows.size(); rowIdx1++) {
                    Row      row = rows.get(rowIdx1);
                    KeyValue kv1 = values.get(rowIdx1);
                    if (kv1.matched) {
                        continue;
                    }
                    boolean matched = false;
                    for (int rowIdx2 = 1; rowIdx2 < rows.size(); rowIdx2++) {
                        if (rowIdx1 == rowIdx2) {
                            continue;
                        }
                        KeyValue kv2 = values.get(rowIdx2);
                        if (kv2.key.equals(kv1.key)) {
                            matched = true;
                        } else if (kv2.key.startsWith(kv1.key)) {
                            matched = true;
                        } else {
                            if (kv1.key.length() <= kv2.key.length()) {
                                int score = similarScore(kv1.key, kv2.key);
                                if (score > 90) {
                                    //  System.err.println("score:" + score +" " + kv1.key +" " + kv2.key);
                                    matched = true;
                                }
                            }
                        }
                        if (matched) {
                            row.set(i, kv2.value);
                            if ( !kv1.value.equals(kv2.value)) {
                                //System.err.println("match:k1:" + kv1.key
				//						   + " k2:" + kv2.key + " v:"
				//						   + kv1.value + " v2:" + kv2.value);
                            }
                            kv1.value   = kv2.value;
                            kv1.key     = cleanName(kv1.value);
                            kv2.matched = true;

                            break;
                        }
                    }
                    if ( !matched) {
                        //                      System.err.println("not match:" + kv1.value);
                    }
                }

            }

            return rows;
        }

        /**
         * Class description
         *
         *
         * @version        $version$, Thu, Nov 4, '21
         * @author         Enter your name here...    
         */
        private static class KeyValue {

            /**  */
            int index;

            /**  */
            String key;

            /**  */
            String value;

            /**  */
            boolean matched = false;

            /**
             
             *
             * @param index _more_
             * @param key _more_
             * @param value _more_
             */
            KeyValue(int index, String key, String value) {
                this.index = index;
                this.key   = key;
                this.value = value;
            }
        }

        /**  */
        private static String[] replace = new String[] {
            "ÁĂẮẶẰẲẴǍÂẤẬẦẨẪÄǞȦǠẠȀÀẢȂĀĄÅǺḀÃǼǢ", "A", "ḂḄḆ", "B", "ĆČÇḈĈĊ", "C",
            "ĎḐḒḊḌḎ", "D", "ÉĔĚȨḜÊẾỆỀỂỄḘËĖẸȄÈẺȆĒḖḔĘẼḚÉ", "E", "Ḟ", "F",
            "ǴĞǦĢĜĠḠ", "G", "ḪȞḨĤḦḢḤẖ", "H", "ÍĬǏÎÏḮİỊȈÌỈȊĪĮĨḬ", "I", "ǰĴ",
            "J", "ḰǨĶḲḴ", "K", "ĹĽĻḼḶḸḺ", "L", "ḾṀṂ", "M", "ŃŇŅṊṄṆǸṈÑ", "N",
            "ÓŎǑÔỐỘỒỔỖÖȪȮȰỌŐȌÒỎƠỚỢỜỞỠȎŌṒṐǪǬÕṌṎȬǾØ", "O", "ṔṖ", "P",
            "ŔŘŖṘṚṜȐȒṞ", "R", "ŚṤŠṦŞŜȘṠẛṢṨ", "S", "ŤŢṰȚẗṪṬṮ", "T",
            "ÚŬǓÛṶÜǗǙǛǕṲỤŰȔÙỦƯỨỰỪỬỮȖŪṺŲŮŨṸṴ", "U", "ṾṼ", "V", "ẂŴẄẆẈẀẘ", "W",
            "ẌẊ", "X", "ÝŶŸẎỴỲỶȲẙỸ", "Y", "ŹŽẐŻẒẔ", "Z",
            "(\\bTHE\\b|\\bAND\\b)", "", "(\\W|\\d)", "",
            //      "  +"," "
        };

        /**
         *
         * @param s _more_
         *
         * @return _more_
         */
        public static String cleanName(String s) {
            s = s.toUpperCase();
            for (int i = 0; i < replace.length; i += 2) {
                s = s.replaceAll(replace[i], replace[i + 1]);
            }

            return s;
        }

    }



    /**
     * Class description
     *
     *
     * @version        $version$, Tue, Nov 19, '19
     * @author         Enter your name here...
     */
    public static class Melter extends RowCollector {

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
        public Melter(TextReader ctx, List<String> cols, String label1, String label2) {
            super(cols);
            this.label1 = label1;
            this.label2 = label2;
        }



        /**
         * _more_
         *
         * @param ctx _more_
         * @param rows _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        public List<Row> finish(TextReader ctx, List<Row> rows)
	    throws Exception {
            List<Integer>    indices   = getIndices(ctx);
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
        public Dups(TextReader ctx, List<String> columns) {
            this.columns = columns;
        }


        /**
         * _more_
         *
         * @param ctx _more_
         * @param rows _more_
         *
         *
         * @return _more_
         * @throws Exception On badness
         */
        @Override
        public List<Row> finish(TextReader ctx, List<Row> rows)
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
        public Splatter(TextReader ctx, String key, String value, String delim, String name) {
            this.key       = key;
            this.value     = value;
            this.delimiter = delim;
            this.name      = name;
        }


        /**
         * _more_
         *
         * @param ctx _more_
         * @param rows _more_
         *
         *
         * @return _more_
         * @throws Exception On badness
         */
        @Override
        public List<Row> finish(TextReader ctx, List<Row> rows)
	    throws Exception {
            List<Row> newRows = new ArrayList<Row>();
            int keyIndex = getIndices(ctx,
                                      Utils.split(key, ",", true,
						  true)).get(0);
            int valueIndex = getIndices(ctx,
                                        Utils.split(value, ",", true,
						    true)).get(0);
            List<Row> allRows   = rows;
            Row       headerRow = allRows.get(0);
            headerRow.add(name);
            newRows.add(headerRow);
            allRows.remove(0);
            Hashtable<Object, Row> map = new Hashtable<Object, Row>();
            for (Row row : allRows) {
		if(!row.indexOk(keyIndex)) continue;
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
        public Breaker(TextReader ctx, String label1, String label2, List<String> cols) {
            super(cols);
            this.label1 = label1;
            this.label2 = label2;
        }



        /**
         * _more_
         *
         * @param ctx _more_
         * @param rows _more_
         *
         *
         * @return _more_
         * @throws Exception On badness
         */
        @Override
        public List<Row> finish(TextReader ctx, List<Row> rows)
	    throws Exception {
            HashSet       cols    = new HashSet();
            List<Row>     newRows = new ArrayList<Row>();
            List<Integer> indices = getIndices(ctx);
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

	private String how="";

        /**
         * _more_
         *
         *
         * @param index _more_
         *
         * @param col _more_
         * @param asc _more_
         */
        public Sorter(TextReader ctx, List<String> cols, boolean asc) {
            super(cols);
            this.asc = asc;
        }

        public Sorter(TextReader ctx, List<String> cols, boolean asc, String how) {
            super(cols);
            this.asc = asc;
	    this.how = how;
        }	


        /**
         * _more_
         *
         * @param ctx _more_
         * @param rows _more_
         *
         *
         * @return _more_
         * @throws Exception On badness
         */
        @Override
        public List<Row> finish(TextReader ctx, List<Row> rows)
	    throws Exception {
	    List<Integer>indices = getIndices(ctx);
            rows = new ArrayList<Row>(getRows(rows));
            if (rows.size() == 0) {
                return rows;
            }
            Row headerRow = rows.get(0);
            rows.remove(0);
            Collections.sort(rows, new Row.RowCompare(ctx,indices, asc,how));
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

        /** _more_ */
        private static SimpleDateFormat fmtSdf =
            new SimpleDateFormat("yyyy-MM-dd hh:mm");

        /** _more_ */
        private Seesv util;

        /**  */
        private boolean justStats;

        /** _more_ */
        private List<ColStat> cols;

        /** _more_ */
        private Row headerRow;

        /** _more_ */
        int rowCnt = 0;

        /** _more_ */
        private boolean interactive;

        /**
         * ctor
         *
         * @param util _more_
         * @param justStats _more_
         */
        public Stats(TextReader ctx, Seesv util, boolean justStats) {
	    super(ctx);
            this.util      = util;
            this.justStats = justStats;
            interactive    = util.getInteractive();
	    //	    interactive=true;
        }


        /**
         * _more_
         *
         * @param ctx _more_
         * @param row _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) throws Exception {
            rowCnt++;
            if (headerRow == null) {
                headerRow = row;

                return row;
            }
            if (cols == null) {
                cols = new ArrayList<ColStat>();
                for (int i = 0; i < row.size(); i++) {
                    cols.add(new ColStat(util, interactive,
                                         (i < headerRow.size())
                                         ? headerRow.getString(i)
                                         : "", row.getString(i)));
                }
            }
            for (int i = 0; i < row.size(); i++) {
                if (i < cols.size()) {
                    cols.get(i).addValue(row.getString(i));
                }
            }
            addRow(row);

            return row;
        }


        /**
         * _more_
         *
         * @param ctx _more_
         * @param rows _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        @Override
        public List<Row> finish(TextReader ctx, List<Row> rows)
	    throws Exception {
            PrintWriter w = ctx.getWriter();
	    //J-
            BiFunction<String,String,String> layout = (label,value) -> {
                return HU.tag("table",
                              HU.attrs("class", "left_right_table",
                                       "cellpadding", "0", "cellspacing",
                                       "0"), HU.row(HU.col(label)
                                                    + HU.col(value,
                                                             HU.attrs("align","right","valign","top"))));
            };

            Function<ColStat,String> printUniques = (col) -> {
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
                String html = values.size() +" " + Utils.plural(values.size(), "unique value");
                if(justStats)
                    html+="<div style='margin-right:5px;max-height:300px;overflow-y:auto;'>";
                else
                    html+="<div style='margin-right:5px;max-height:100px;overflow-y:auto;'>";
                html+="<table width=100% border=0 cellpadding=0 cellspacing=0>";
                int tupleCnt=0;
                String td = "<td style='border:none;padding:0px;padding-left:0px;padding-right:5px;' ";
                for (Object[] tuple : values) {
                    tupleCnt++;
                    if((justStats && tupleCnt>50) || (!justStats && tupleCnt>20)) {
                        html+="<tr>" + td+" colspan=2>...</td></tr>";
                        break;
                    }
                    Object key = tuple[0];
                    int    cnt = (Integer) tuple[1];
                    double percent = Math.round(1000.0 * cnt
                                                / (double) (rowCnt-1)) / 10;

                    String title = key.toString().replaceAll("'","\\'") +" " + percent+"%";
                    html+="<tr valign=bottom title='" + title +"'>";
                    key = HU.div(key.toString(),HU.attrs("style","max-width:100px;overflow-x:auto;"));
                    html+=td+ " width=1%>" + key+"</td>";
                    html+=td+" width=1% align=right>" + cnt +"</td>";
                    html+="<td style='border:none;padding:0px;' ><div style='margin-top:3px;display:inline-block;background:blue;height:1em;width:" + percent +"%;'></div></td>";

                    html+="</tr>";
                }
                html+="</table>";
                html+="</div>";
                return html;

            };

            w.println("#rows:" + rowCnt);
            if(cols ==null) cols = new ArrayList<ColStat>();
            if (interactive) {
		StringBuilder summary = new StringBuilder();
                w.println(HU.SPACE2);
                w.println("<span id=header></span>");
                w.println("<table width='100%' class='stripe hover display nowrap ramadda-table ramadda-csv-table' >");
                w.println("<thead>");
                w.println("<tr valign=top class=csv-header>");
                summary.append("<tr valign=top class=seesv-table-summary style='display:none;'>");	                for(int i=0;i<cols.size();i++) {
                    ColStat col =  cols.get(i);
		    //		    if(col.skip) continue;
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
			col.alignRight = true;
                    }
                    String type = HU.faIcon(typeIcon, "title", "type: " + col.type, "style", "font-size:10pt;");
                    String name = col.name;
                    String label = Utils.makeLabel(name);
                    String id = Utils.makeID(name);
                    label = HU.div(type + "&nbsp;" + label,HU.attrs("style","text-align:center;width:100%","class","csv-id","fieldid",id));
                    String extra = "";
		    String extraAttrs = "";
                    if (!col.skip && Utils.equalsOne(col.name.trim().toLowerCase(), "latitude","longitude")) {
                        ColStat next = i<cols.size()-1?cols.get(i+1):null;
                        if(next!=null && Utils.equalsOne(next.name.toLowerCase(),"longitude","latitude")) {
			    next.skip = true;
                            ColStat lat = col.name.equalsIgnoreCase("latitude")?col:next;
                            ColStat lon = col.name.equalsIgnoreCase("longitude")?col:next;
                            StringBuilder map = new StringBuilder();	
                            MapProvider mp  = util.getMapProvider();
                            if(mp!=null) {
                                List<double[]> pts = new ArrayList<double[]>();
                                for(int ptIdx=0;ptIdx<lat.pts.size() && ptIdx<lon.pts.size();ptIdx++)
                                    pts.add(new double[]{lat.pts.get(ptIdx),lon.pts.get(ptIdx)});
                                Hashtable<String,String>props = new Hashtable<String,String>();
                                props.put("simple","true");
                                props.put("radius","3");
                                mp.makeMap(map,"100%",justStats?"300px":"130px",pts,props);
                                extra = HU.div(map.toString(),HU.style("margin:5px;border:1px solid #aaa;"));
				extraAttrs=" colspan=2 ";
                            }
                        }
                    } else {
                        if (col.type.equals("numeric")) {
                            extra+=layout.apply("min:", "" + col.min);
                            extra+=layout.apply("max:", "" + col.max);
                            if (col.numMissing > 0) {
                                extra+=layout.apply("#missing:",
						    "" + col.numMissing);
                            }
                            if (col.numErrors > 0) {
                                extra+=layout.apply("#errors:",   "" + col.numErrors);
                                if (col.sampleError != null) {
                                    extra+=layout.apply("eg:"
							+ ((col.sampleError.trim().length()
							    == 0)
							   ? "<blank>"
							   : col.sampleError), "");
                                }
                            }
                            extra+=printUniques.apply(col);
                        } else if (col.type.equals("date")) {
                            if (col.minDate != null) {
                                extra+=layout.apply("min:", fmtSdf.format(col.minDate));
                            }
                            if (col.maxDate != null) {
                                extra+=layout.apply("max:",fmtSdf.format(col.maxDate));
                            }
                        } else if (col.type.equals("image")) {
                        } else if (col.type.equals("url")) {
                        } else {
                            extra+=printUniques.apply(col);
                        }
                    }
                    extra = HU.div(extra,"");
                    w.println(HU.th(label," nowrap " +HU.attr("align","center")+HU.style("padding:2px !important;")));
		    if(!col.skip) 
			summary.append(HU.td(extra,extraAttrs+" nowrap " +HU.cssClass("seesv-table-summary-cell") + HU.style("padding:2px !important;")));
                }
                w.println("</tr>");
		w.println("</thead>");
		w.println("<tbody>");
                summary.append("</tr>");		
		w.println(summary);


		boolean even= false;
		for (Row row : rows) {
		    even = !even;
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
			if(i<row.size()) {
			    if(col.mergeNext) {
				r.add(col.format(ctx,row.get(i))+"," + cols.get(i+1).format(ctx,row.get(i+1)));
			    } else {
				r.add(col.format(ctx,row.get(i)));
			    }
			}
		    }
		    if(!justStats) {
			printRow(ctx, r, false,even);
		    }
		}
		w.println("</tbody>");
		w.println("</table>");
	    } else {
		for (ColStat col : cols) {
		    cnt++;
		    ctx.getWriter().print("#" + cnt + " ");
		    col.finish(ctx.getWriter());
		}
	    }


	    //J+
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

            /** _more_ */
            String name;

            /** _more_ */
            String label;

            /** _more_ */
            String type;

	    boolean alignRight = false;
	    
            /** _more_ */
            String sample;

            /** _more_ */
            MyDateFormat sdf;

            /** _more_ */
            String format;

            /** _more_ */
            String sampleError;

            double total = 0;

            /** _more_ */
            double min = Double.NaN;

            /** _more_ */
            double max = Double.NaN;

            /** _more_ */
            int numErrors = 0;

            /** _more_ */
            int numMissing = 0;

            /**  */
            boolean interactive;


            /** _more_ */
            Hashtable<Object, Integer> uniques = new Hashtable<Object,
		Integer>();

            /**  */
            List<Double> pts = new ArrayList<Double>();


            /** _more_ */
            Date minDate;

            /** _more_ */
            Date maxDate;

	    boolean skip = false;
	    boolean mergeNext = false;

            /**
             * _more_
             *
             * @param util _more_
             * @param interactive _more_
             * @param n _more_
             * @param sample _more_
             */
            public ColStat(Seesv util, boolean interactive, String n,
                           String sample) {
                this.interactive = interactive;
                label            = name;
                name             = n;
                if (name.startsWith("#fields=")) {
                    name = name.substring("#fields=".length());
                }
                String args = StringUtil.findPattern(name, "\\[(.*)\\]");
                name = name.replaceAll("\\[.*\\]", "");
                name = name.toLowerCase().trim();
                try {
                    Double.parseDouble(sample);
                    this.type = "numeric";
                } catch (Exception ignore) {
                    if (sample.startsWith("http")) {
                        if (name.equals("image")) {
                            this.type = "image";
                        } else {
                            this.type = "url";
                        }
                    } else {
                        this.type = "string";
                    }
                }
                if (args != null) {
                    Hashtable props = Utils.parseKeyValue(args);
                    this.type  = Utils.getProperty(props, "type", this.type);
                    this.label = Utils.getProperty(props, "label",
						   this.label);
                    this.format = Utils.getProperty(props, name + ".format",
						    Utils.getProperty(props, "format", this.format));
                    if (this.format != null) {
			//                        sdf = new SimpleDateFormat(this.format);
                    }
                }
            }

	    private MyDateFormat getSdf() {
		if(sdf==null)
		    sdf = new MyDateFormat(this.format);
		return sdf;
		    
	    }
	    public String toString() {
		return name;
	    }

	    boolean loggedError = false;
            /**
             * _more_
             *
             * @param v _more_
             *
             * @return _more_
             */
            public Date getDate(Object v) {
                try {
                    return getSdf().parse(v.toString());
                } catch (Exception exc) {
		    if(!loggedError) {
			System.err.println("Unable to parse date:" + v +" with format:" + this.format);
			loggedError = true;
		    }
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
            public Object format(TextReader ctx,Object v) {
                if (type.equals("date") && (format != null)) {
                    Date date = getDate(v);
                    if (date != null) {
			return ctx.formatDate(date);
			//                        return fmtSdf.format(date);
                    }

                    return "bad date(3):" + v;
                } else if (type.equals("image")) {
                    String url = v.toString().trim();
                    if (url.length() != 0) {
                        return HU.href(url, HU.image(url, "width", "100"),
                                       "target=_image");
                    }
                }

		//Clean up any tags
		if(v!=null) {
		    String sv = v.toString();
		    sv = sv.replaceAll("<","&lt;");
		    sv = sv.replaceAll(">","&gt;");
		    return sv;
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
                if (name.equals("latitude") || name.equals("longitude")) {
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
			total+=d;
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
                    cnt = Integer.valueOf(0);
                }
                cnt = Integer.valueOf(cnt + 1);
                uniques.put(v, cnt);
            }

            /**
             * _more_
             *
             * @param writer _more_
             */
            public void finish(PrintWriter writer) {
                if (interactive) {
                    writer.print("<pre>");
                }
                writer.print(name + " [" + type + "]  ");
                writer.print("sample:" + sample + "  ");
                if (type.equals("numeric")) {
                    writer.print("min:" + min + "  max:" + max);
                    writer.print(" total:" + total);
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
                if (interactive) {
                    writer.print("</pre>");
                }
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
    public static class Summary extends RowCollector {

	private List<String> what;
	
        /** _more_ */
        private List<Integer> uniqueIndices;

        /** _more_ */
        private List<Integer> valueIndices;

        /** _more_ */
        private List<Integer> extraIndices;

        /** _more_ */
        private List<String> keyCols;

        /** _more_ */
        private List<String> values;

        /** _more_ */
        private List<String> extra;

	private Object[] array = null;

	List<String> keys     = new ArrayList<String>();
	Hashtable<String, Count> counts = new Hashtable<String,Count>();
	Hashtable<String, Row> origMap   = new Hashtable<String, Row>();
	Row                    headerRow;
	Row                    firstRow ;

        /**
         * _more_
         *
         *
         *
         * @param keys _more_
         * @param values _more_
         * @param extra _more_
         */
        public Summary(TextReader ctx, List<String> what,List<String> keys, List<String> values,
		       List<String> extra) {
	    
	    if(what.size()==0) {
		what =new ArrayList<String>();
		what.add(OPERAND_COUNT);
	    }
	    this.what = what;
            this.keyCols   = keys;
            this.values = values;
            this.extra  = extra;
        }


        public Row processRow(TextReader ctx, Row row) throws Exception {
	    //            super.processRow(ctx, row);
	    if(uniqueIndices==null) {
		uniqueIndices = getIndices(ctx, keyCols);
		valueIndices  = getIndices(ctx, values);
		valueIndices  = getIndices(ctx, values);
		extraIndices  = getIndices(ctx, extra);
	    }
	    if(rowCnt++==0) {
		headerRow = row;
		firstRow =row ;
		return null;
	    }
	    List          values = row.getValues();
	    if (array == null) {
		array = new Object[values.size()];
	    }

	    String    key;
	    if(uniqueIndices.size()>1) {
		StringBuilder keySB  = new StringBuilder();
		for (int i : uniqueIndices) {
		    if ((i >= -0) && (i < values.size())) {
			keySB.append(values.get(i));
			keySB.append("_");
		    }
		}
		key = keySB.toString();
	    } else if(uniqueIndices.size()==1) {
		key  = values.get(uniqueIndices.get(0)).toString();
	    } else {
		key  ="";
	    }

	    Count count = counts.get(key);
	    if (count==null) {
		count = new Count(row);
		counts.put(key, count);
		keys.add(key);
	    }
	    count.addValues(valueIndices, row);
	    return null;
	}


        /**
         * _more_
         *
         * @param ctx _more_
         * @param rows _more_
         *
         *
         * @return _more_
         * @throws Exception On badness
         */
        @Override
        public List<Row> finish(TextReader ctx, List<Row> rows)
	    throws Exception {
	    
            List<Row> newRows   = new ArrayList<Row>();
            Row       newHeader = new Row();
	    for(int i:uniqueIndices) {
		newHeader.add(headerRow.get(i));
	    }
	    for(int i:valueIndices) {
		for(String w: what) {
		    if(w.equals(OPERAND_COUNT))
			newHeader.add(headerRow.get(i)+" Count");
		    else
			newHeader.add(headerRow.get(i)+" " + w);
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
                Count count = counts.get(key);
                if (valueIndices.size() == 0) {
                    //just count them
                    Row       row      = count.sample;
                    Row       newRow   = new Row();
                    newRows.add(newRow);
                    for (int i : uniqueIndices) {
                        newRow.add(row.get(i));
                    }
                    newRow.add(count.count);
                    for (int j : extraIndices) {
                        newRow.add(row.get(j));
                    }
                    continue;
                }
		
                Row newRow = null;
                for (int i = 0; i < valueIndices.size(); i++) {
                    int    valueIdx = valueIndices.get(i);
		    if (newRow == null) {
			newRow = new Row();
			for (int u = 0; u < uniqueIndices.size(); u++) {
			    newRow.add(count.sample.get(uniqueIndices.get(u)));
			}
			newRows.add(newRow);
		    }

		    for(String w: what) {
			if(w.equals(OPERAND_SUM)) {
			    newRow.add(Double.valueOf(count.totals[i]));
 			} else if(w.equals(OPERAND_MIN)) {
			    newRow.add(Double.valueOf(count.mins[i]));
 			} else if(w.equals(OPERAND_MAX)) {
			    newRow.add(Double.valueOf(count.maxs[i]));
 			} else if(w.equals(OPERAND_AVG) || w.equals(OPERAND_AVERAGE)) {
			    newRow.add(Double.valueOf(count.totals[i]/count.count));
 			} else if(w.equals(OPERAND_COUNT)) {
			    newRow.add(Integer.valueOf(count.count));
			}
		    }
		}
                for (int j : extraIndices) {
                    newRow.add(count.sample.get(j));
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
    public static class Pivot extends RowCollector {
	LinkedHashMap<String,String> seenColumns = new LinkedHashMap<String,String>();
	LinkedHashMap<String,String> seenRows = new LinkedHashMap<String,String>();	
	Hashtable<String, Tuple> values = new Hashtable<String,Tuple>();
        LinkedHashMap<String, StringBuilder> map = new LinkedHashMap<String,
	    StringBuilder>();


        /** _more_ */
        private List<String> keyCols;
        private List<String> columnCols;	
	private List<String> operators;
	private String valueColumn;
	
        /** _more_ */
        private List<Integer> keyIndices;

        /** _more_ */
        private List<Integer> columnIndices;

        /** _more_ */
        private int valueIndex;


	private Row  headerRow;


        /**
         *
         * @param keys _more_
         * @param values _more_
         * @param extra _more_
         */
        public Pivot(TextReader ctx, List<String> keys,List<String> columns, String valueColumn,
		     List<String> operators) {
	    if(operators.size()==0) {
		operators.add(OPERAND_COUNT);
	    }
            this.keyCols   = keys;
            this.columnCols = columns;
	    this.valueColumn = valueColumn;
            this.operators  = operators;
        }


        public Row processRow(TextReader ctx, Row row) throws Exception {
	    if(rowCnt++==0) {
		headerRow= row;
		return null;
	    }

	    if(keyIndices==null) {
		keyIndices = getIndices(ctx, keyCols);
		columnIndices  = getIndices(ctx, columnCols);
		valueIndex  = getIndex(ctx, valueColumn);
	    }
	    StringBuilder columnValue = null;
	    for(int index: columnIndices) {
		if(!row.indexOk(index)) continue;
		String v = row.getString(index);
		if(columnValue==null)
		    columnValue = new StringBuilder();
		else
		    columnValue.append(" - ");
		columnValue.append(v);
	    }
	    if(columnValue==null) {
		return null;
	    }
	    String colKey = columnValue.toString();
	    if(seenColumns.get(colKey)==null) {
		seenColumns.put(colKey,"seen");
		//		System.err.println("C:" + colKey);
	    }


	    StringBuilder keyValue = null;
	    for(int index: keyIndices) {
		if(!row.indexOk(index)) continue;
		String v = row.getString(index);
		if(keyValue==null)
		    keyValue = new StringBuilder();
		else
		    keyValue.append(" - ");
		keyValue.append(v);
	    }
	    if(keyValue==null) {
		return null;
	    }

	    String rowKey= keyValue.toString();
	    if(seenRows.get(rowKey)==null) {
		seenRows.put(rowKey,"seen");
		//		System.err.println("K:" + rowKey);
	    }
	    String valueKey = getValueKey(rowKey,colKey);
	    Tuple tuple = values.get(valueKey);
	    if(tuple==null) values.put(valueKey,tuple=new Tuple());
	    double value = row.getDouble(valueIndex);
	    tuple.add(value);
	    return null;
	}


	public String getValueKey(Object row,Object col) {
	    return row+"----" + col;
	}

        /**
         * _more_
         *
         * @param ctx _more_
         * @param rows _more_
         *
         *
         * @return _more_
         * @throws Exception On badness
         */
        @Override
        public List<Row> finish(TextReader ctx, List<Row> rows)
	    throws Exception {
	    Set<String> cols = seenColumns.keySet();
	    List<Row> newRows = new ArrayList<Row>();

	    StringBuilder keyValue = null;
	    for(int index: keyIndices) {
		if(!headerRow.indexOk(index)) continue;
		String v = headerRow.getString(index);
		if(keyValue==null)
		    keyValue = new StringBuilder();
		else
		    keyValue.append(" - ");
		keyValue.append(v);
	    }
	    Row newHeaderRow = new Row();
	    newRows.add(newHeaderRow);
	    newHeaderRow.add(keyValue.toString());

	    for(String colKey: cols) {
		if(operators.size()==1)
		    newHeaderRow.add(colKey);
		else {
		    for(String op: operators) {
			newHeaderRow.add(colKey +" - " + op);
		    }
		}
	    }


	    for (String rowKey : seenRows.keySet()) {
		Row row = new Row();
		row.add(rowKey);
		newRows.add(row);
		for(String colKey: cols) {
		    String valueKey = getValueKey(rowKey,colKey);
		    Tuple t= values.get(valueKey);
		    if(t==null) {
			for(String op: operators) {
			    row.add("NaN");
			}
			continue;
		    } 
		    for(String op: operators) {
			row.add(t.getValue(op));
		    }
		}
	    }

	    return newRows;
	    /*
            List<Row> newRows   = new ArrayList<Row>();
            Row       newHeader = new Row();
	    for(int i:uniqueIndices) {
		newHeader.add(headerRow.get(i));
	    }
	    for(int i:valueIndices) {
		for(String w: what) {
		    if(w.equals("count"))
		       newHeader.add(headerRow.get(i)+" Count");
		    else
			newHeader.add(headerRow.get(i)+" " + w);
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
                Count count = counts.get(key);
                if (valueIndices.size() == 0) {
                    //just count them
                    Row       row      = count.sample;
                    Row       newRow   = new Row();
                    newRows.add(newRow);
                    for (int i : uniqueIndices) {
                        newRow.add(row.get(i));
                    }
                    newRow.add(count.count);
                    for (int j : extraIndices) {
                        newRow.add(row.get(j));
                    }
                    continue;
                }
		
                Row newRow = null;
                for (int i = 0; i < valueIndices.size(); i++) {
                    int    valueIdx = valueIndices.get(i);
		    if (newRow == null) {
			newRow = new Row();
			for (int u = 0; u < uniqueIndices.size(); u++) {
			    newRow.add(count.sample.get(uniqueIndices.get(u)));
			}
			newRows.add(newRow);
		    }

		    for(String w: what) {
			if(w.equals(OPERAND_SUM)) {
			    newRow.add(Double.valueOf(count.totals[i]));
 			} else if(w.equals(OPERAND_MIN)) {
			    newRow.add(Double.valueOf(count.mins[i]));
 			} else if(w.equals(OPERAND_MAX)) {
			    newRow.add(Double.valueOf(count.maxs[i]));
	      } else if(w.equals(OPERAND_AVERAGE)) {
	      newRow.add(Double.valueOf(count.totals[i]/count.count));
	      } else if(w.equals("count")) {
	      newRow.add(Integer.valueOf(count.count));
	      }
	      }
	      }
	      for (int j : extraIndices) {
	      newRow.add(count.sample.get(j));
	      }
	      }

	      return newRows;

	    */
	}
    }



    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Jan 9, '15
     * @author         Jeff McWhirter
     */
    public static class Histogram extends RowCollector {


	List<Bin> bins;
	List<String> what;
	String scolumn;
	int column;

        /**
         * _more_
         *
         */
        public Histogram(TextReader ctx, String column, String bins,List<String> cols,String what) {
	    super(cols);
	    this.scolumn = column;
	    this.what = Utils.split(what,",",true,true);
	    if(cols.size()>0 && this.what.size()==0) this.what.add(OPERAND_SUM);
	    this.bins = new ArrayList<Bin>();
	    double prevValue = Double.NEGATIVE_INFINITY;
	    for(double v: Utils.getDoubles(bins)) {
		this.bins.add(new Bin(prevValue,v));
		prevValue =v;
	    }
	    this.bins.add(new Bin(prevValue,Double.POSITIVE_INFINITY));
        }

        /**
         * _more_
         *
         * @param ctx _more_
         * @param rows _more_
         *
         *
         * @return _more_
         * @throws Exception On badness
         */
        @Override
        public List<Row> finish(TextReader ctx, List<Row> rows)
	    throws Exception {
	    int column = getIndex(ctx,scolumn);
            List<Integer> indices   = getIndices(ctx);
            int          rowIndex = 0;
            List<Row>              allRows   = getRows(rows);
            Row                    headerRow = allRows.get(0);
            Row                    firstRow  = allRows.get(0);
            allRows.remove(0);
            List<Row> newRows   = new ArrayList<Row>();
            Row       newHeader = new Row();
	    newHeader.add(headerRow.getString(column) +" range");
	    for (int i: indices) {
		String label = Utils.makeLabel(headerRow.getString(i));
		for(String op: this.what) {
		    newHeader.add(label +" " + op);
		}
	    }

	    newRows.add(newHeader);
	    final int total = allRows.size();
            Function<Integer,Integer> percent = (col) -> {
		return (int)((col/(double)total)*100);
	    };


            for (Row row : allRows) {
                List          values = row.getValues();
		double value = Double.parseDouble(row.getString(column));
		for(Bin bin: bins) {
		    if(bin.inRange(value)) {
			bin.addValues(indices,row);
			break;
		    }
		}
            }

	    //Check if they're single value
	    boolean allSingle = true;
	    for(Bin bin: bins) {
		if(bin.count==0) continue;
		if(bin.firstBin() || bin.lastBin()) {
		    continue;
		}
		allSingle = Utils.isInt(bin.min) && Utils.isInt(bin.max) && ((int)bin.max) == ((int)bin.min)+1;
		if(!allSingle) {
		    break;
		}
	    }

	    if(allSingle) {
		for(Bin bin: bins) {
		    if(!bin.firstBin() && !bin.lastBin()) {
			bin.label = ""+((int)bin.min);
		    }
		}
		
	    }

	    for(Bin bin: bins) {
		if(bin.totals==null) continue;
		if(bin.firstBin() && bin.count==0) continue;
		Row    row = new Row();
		newRows.add(row);
		row.add(bin.getLabel());
		for (int i=0;i<indices.size();i++) {
		    for(String op: this.what) {
			if(op.equals(OPERAND_SUM)) row.add(bin.totals[i]);
			else if(op.equals(OPERAND_COUNT)) row.add(bin.count);
			else if(op.equals(OPERAND_PERCENT)) row.add(percent.apply(bin.count));
			else if(op.equals(OPERAND_MIN)) {
			    row.add(bin.mins[i]==Double.POSITIVE_INFINITY?"NaN":Double.toString(bin.mins[i]));
			} else if(op.equals(OPERAND_MAX)) {
			    row.add(bin.maxs[i]==Double.NEGATIVE_INFINITY?"NaN":Double.toString(bin.maxs[i]));
			} else if(op.equals(OPERAND_AVERAGE)) {
			    if(bin.count==0) row.add("NaN");
			    else row.add(bin.totals[i]/bin.count);
			} else {
			    fatal(ctx, "Unknown histogram operator:"+ op);
			}
		    }
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
        public GroupFilter(TextReader ctx, List<String> cols, int valueIdx, int op,
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
         * @param ctx _more_
         * @param finalRows _more_
         *
         *
         * @return _more_
         * @throws Exception On badness
         */
        @Override
        public List<Row> finish(TextReader ctx, List<Row> finalRows)
	    throws Exception {
            List<Integer> indices   = getIndices(ctx);
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



    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Nov 4, '21
     * @author         Enter your name here...    
     */
    public static class Slicer extends RowCollector {

        /**  */
        String sdest;

        /**  */
        int dest;

        /**  */
        List<String> fill;



        /**
         * _more_
         *
         * @param cols _more_
         * @param colName _more_
         * @param sdf _more_
         * @param sdest _more_
         * @param fill _more_
         */
        public Slicer(TextReader ctx, List<String> cols, String sdest, List<String> fill) {
            super(cols);
            this.sdest = sdest;
            this.fill  = fill;
        }

        /**
         * _more_
         *
         * @param ctx _more_
         * @param row _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) throws Exception {
            super.processRow(ctx, row);
            if (sdest != null) {
                dest = getColumnIndex(ctx, sdest);
            }
            sdest = null;

            return null;
        }


        /**
         * _more_
         *
         * @param ctx _more_
         * @param tmp _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        @Override
        public List<Row> finish(TextReader ctx, List<Row> tmp)
	    throws Exception {
            tmp = getRows(tmp);
            List<Row> result = new ArrayList<Row>();
            for (Row row : tmp) {
                result.add(row);
            }
            Row sample = tmp.get(1);
            for (int index : getIndices(ctx)) {
                for (Row row : tmp) {
                    Object v = row.get(index);
                    row.set(index, "");
                    Row newRow = new Row();
                    for (int i = 0; i < dest; i++) {
                        newRow.add("");
                    }
                    //              for(Object o : sample.getValue()) newRow.add(o);
                    newRow.add(v);
                    for (String f : fill) {
                        newRow.add(f);
                    }
                    result.add(newRow);
                }
            }
            return result;
        }

    }




    /**
     * Class description
     *a
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
        public DateLatest(TextReader ctx, List<String> cols, String colName,
                          SimpleDateFormat sdf) {
            this.keys    = cols;
            this.colName = colName;
            this.sdf     = sdf;
        }

        /**
         * _more_
         *
         * @param ctx _more_
         * @param row _more_
         *
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                header = row;

                return null;
            }
            if (keyindices == null) {
                keyindices = getIndices(ctx, keys);
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
                    col = getColumnIndex(ctx, colName);
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
         * @param ctx _more_
         * @param tmp _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        @Override
        public List<Row> finish(TextReader ctx, List<Row> tmp)
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

    /**
     * Class description
     *
     *
     * @version        $version$, Wed, Feb 20, '19
     * @author         Enter your name here...
     */
    public static class Crosser extends RowCollector {

        /** _more_ */
        private String file;
	private List<Row> rows2;


        /**
         * _more_
         * @param file _more_
         */
        public Crosser(TextReader ctx,  String file) {
            this.file    = file;
            try {
                init(ctx);
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }
        }


        /**
         * _more_
         *
         *
         * @param ctx _more_
         * @throws Exception _more_
         */
        private void init(TextReader ctx) throws Exception {
            if ( !IO.okToReadFrom(file)) {
                throw new RuntimeException("Cannot read file:" + file);
            }
            BufferedReader br = new BufferedReader(
                                    new InputStreamReader(
                                        getInputStream(file)));
            SeesvOperator operator = null;
            TextReader  reader   = new TextReader(br);
            rows2        = new ArrayList<Row>();
            String delimiter = null;
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                line = line.trim();
                if (line.length() == 0) {
                    continue;
                }
                if (delimiter == null) {
                    if (line.indexOf("\t") >= 0) {
                        delimiter = "\t";
                    } else {
                        delimiter = ",";
                    }
                }
                List<String> cols = Utils.tokenizeColumns(line, delimiter);
                Row row = new Row(cols);
		rows2.add(row);
            }
        }


	private Row makeRow(Row row1,Row row2) {
	    Row newRow = new Row();
	    for(Object o: row1.getValues()) newRow.add(o);
	    for(Object o: row2.getValues()) newRow.add(o);		    
	    return newRow;
	}

        @Override
        public List<Row> finish(TextReader ctx, List<Row> tmp)
	    throws Exception {
            List<Row> result = new ArrayList<Row>();
            tmp = getRows(tmp);
	    result.add(makeRow(tmp.get(0), rows2.get(0)));
	    for(int i=1;i<tmp.size();i++) {
		Row row1= tmp.get(i);
		for(int j=1;j<rows2.size();j++) {
		    Row row2= rows2.get(j);
		    result.add(makeRow(row1,row2));
		}
	    }
            return result;
        }
    }


    public static class Cloner extends Converter {

        /** _more_ */
        private int count;


        /**
         * @param row _more_
         * @param col _more_
         * @param value _more_
         */
        public Cloner(int cnt) {
            this.count = cnt;
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++==0) return row;
	    Processor nextProcessor = getNextProcessor();
	    if (nextProcessor != null) {
		try {
		    for(int i=0;i<count;i++) {
			nextProcessor.handleRow(ctx, new Row(row));
		    }
		} catch(Exception exc) {throw new RuntimeException(exc);}
	    }
            return row;
        }
    }


}
