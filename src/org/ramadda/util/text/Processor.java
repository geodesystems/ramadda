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


import org.ramadda.data.record.RecordField;


import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;


import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.text.DateFormat;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

import java.util.regex.*;


/**
 *
 * @author Jeff McWhirter
 */

public abstract class Processor extends CsvOperator {


    /**
     * _more_
     */
    public Processor() {}


    /**
     * _more_
     *
     * @param col _more_
     */
    public Processor(String col) {
        super(col);
    }


    /**
     * _more_
     *
     * @param cols _more_
     */
    public Processor(List<String> cols) {
        super(cols);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean buffersRows() {
        return false;
    }

    /**
     *     _more_
     *
     *     @param name _more_
     *
     *     @return _more_
     */
    public static String cleanName(String name) {
        name = name.toLowerCase().replaceAll("\\s+", "_").replaceAll(",",
                                             "_");
        name = name.replaceAll("__+", "_");

        return name;
    }


    /**
     * _more_
     *
     * @param name _more_
     * @param sb _more_
     * @param index _more_
     * @param seen _more_
     * @param rows _more_
     *
     * @throws Exception _more_
     */
    public static void addFieldDescriptor(String name, Appendable sb,
                                          int index, HashSet<String> seen,
                                          List<Row> rows)
            throws Exception {

        List<String>  toks   = StringUtil.split(name, ":unit:", true, true);
        String        id     = cleanName(toks.get(0));
        StringBuilder extra  = new StringBuilder();
        String        suffix = null;
        if (toks.size() > 1) {
            String unit = toks.get(1);
            unit = unit.replaceAll(" / ", "/").replaceAll(",",
                                   "_").replaceAll("\"", "_");
            suffix = cleanName(unit);
            extra.append(" unit=\"");
            extra.append(unit);
            extra.append("\" ");
            name = toks.get(0);
        }

        id = id.replaceAll("\\(", "").replaceAll("\\)", "");

        if (seen.contains(id)) {
            if (suffix == null) {
                suffix = "" + seen.size();
            }
            id = id + "_" + suffix;
        }
        seen.add(id);

        String type = RecordField.TYPE_DOUBLE;
        if (id.indexOf("year") >= 0) {
            type = RecordField.TYPE_DATE;
            extra.append(" format=\"yyyy\" ");
        }

        boolean hasName = id.indexOf("name") >= 0;
        if (hasName) {
            type = RecordField.TYPE_STRING;
        } else {
            String sampledType = null;
            if (rows != null) {
                for (Row exampleRow : rows) {
                    Object example = exampleRow.get(index);
                    if (example == null) {
                        continue;
                    }
                    String exampleString = example.toString();
                    if (exampleString.matches("^[\\d,]+$")) {
                        if (sampledType == null) {
                            sampledType = RecordField.TYPE_INT;
                        }
                    } else if (exampleString.matches("^[\\d\\.]+$")) {
                        sampledType = RecordField.TYPE_DOUBLE;
                    } else if (exampleString.length() == 0) {}
                    else {
                        sampledType = RecordField.TYPE_STRING;

                        break;
                    }
                }
            }

            if (sampledType != null) {
                type = sampledType;
            }
        }

        if (id.indexOf("latitude") >= 0) {
            extra.append(" isLatitude=\"true\" ");
            type = RecordField.TYPE_DOUBLE;
        } else if (id.indexOf("longitude") >= 0) {
            extra.append(" isLongitude=\"true\" ");
            type = RecordField.TYPE_DOUBLE;
        }

        sb.append(id);
        sb.append("[");
        sb.append(" label=\"");
        sb.append(name.replaceAll(",", " "));
        sb.append("\" ");
        sb.append(" chartable=\"true\" ");
        sb.append(" type=\"" + type + "\" ");
        sb.append(extra);
        sb.append("  ]");
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
     *
     * @throws Exception _more_
     */
    public Row processRow(TextReader info, Row row, String line)
            throws Exception {
        return row;
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
    public List<Row> finish(TextReader info, List<Row> rows)
            throws Exception {
        return rows;
    }

    /**
     * _more_
     */
    public void reset() {}




    /**
     * Class description
     *
     *
     * @version        $version$, Sat, Jan 10, '15
     * @author         Jeff McWhirter
     */
    public static class ProcessorGroup extends Processor {

        /** _more_ */
        private List<Row> rows;


        /** _more_ */
        private List<Processor> processors = new ArrayList<Processor>();

        /** _more_ */
        private List<Processor> firstProcessors;

        /** _more_ */
        private List<Processor> remainderProcessors;

        /**
         * _more_
         */
        public ProcessorGroup() {}

        /**
         * _more_
         *
         * @param processor _more_
         */
        public void addProcessor(Processor processor) {
            processors.add(processor);
        }



        /**
         * _more_
         */
        public void reset() {
            for (Processor processor : processors) {
                processor.reset();
            }
        }

        /**
         * _more_
         *
         * @param info _more_
         * @param row _more_
         * @param line _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        @Override
        public Row processRow(TextReader info, Row row, String line)
                throws Exception {
            if (processors.size() == 0) {
                if (info.getRow() == 0) {
                    //not now
                    for (String header : info.getHeaderLines()) {
                        //                        info.getWriter().println(header);
                    }
                }
                /*
                info.getWriter().println(
                                         CsvUtil.columnsToString(
                                                                 row.getValues(), info.getOutputDelimiter()));
                info.getWriter().flush();
                */
            }

            Object  skipTo      = row.getSkipTo();
            boolean sawBufferer = false;
            if (remainderProcessors == null) {
                remainderProcessors = new ArrayList<Processor>();
                firstProcessors     = new ArrayList<Processor>();
                for (int i = 0; i < processors.size(); i++) {
                    Processor processor = processors.get(i);
                    if (skipTo != null) {
                        if (skipTo == processor) {
                            skipTo = null;
                        }

                        continue;
                    }
                    if (sawBufferer) {
                        remainderProcessors.add(processor);
                    } else {
                        firstProcessors.add(processor);
                    }
                    if (processor.buffersRows()) {
                        sawBufferer = true;
                    }
                }
            }

            boolean firstRow = rowCnt++ == 0;
            for (Processor processor : firstProcessors) {
                if (skipTo != null) {
                    if (skipTo == processor) {
                        skipTo = null;
                    }

                    continue;
                }
                if (firstRow) {
                    processor.setHeader(row.getValues());
                }
                //Always do this here so the indexes don't get screwed up 
                processor.getIndices(info);
                row = processor.processRow(info, row, line);
                if (row == null) {
                    return null;
                }
            }
            return row;
        }

        /**
         * _more_
         *
         * @param info _more_
         * @param row _more_
         * @param line _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        private Row processRowInner(TextReader info, Row row, String line)
                throws Exception {
            boolean sawBufferer = false;
            if (remainderProcessors == null) {
                remainderProcessors = new ArrayList<Processor>();
                firstProcessors     = new ArrayList<Processor>();
                for (int i = 0; i < processors.size(); i++) {
                    Processor processor = processors.get(i);
                    if (sawBufferer) {
                        remainderProcessors.add(processor);
                    } else {
                        firstProcessors.add(processor);
                    }
                    if (processor.buffersRows()) {
                        sawBufferer = true;
                    }
                }
            }

            for (Processor processor : firstProcessors) {
		//		System.out.println("before: " + processor.getClass().getName().replace("org.ramadda.util.text.","") +" row:" + row.myx +" size:" + row.size());

                row = processor.processRow(info, row, line);
                if (row == null) {
                    return null;
                }
		//		System.out.println("after: " +" row:" + row.myx +" size:" + row.size());
            }
            return row;
        }

        /**
         * _more_
         *
         *
         * @param textReader _more_
         * @param inputRows _more_
         *
         *
         * @return _more_
         * @throws Exception On badness
         */
        @Override
        public List<Row> finish(TextReader textReader, List<Row> inputRows)
                throws Exception {
            while ((remainderProcessors != null)
                    && (remainderProcessors.size() > 0)) {
                if (firstProcessors != null) {
                    for (Processor processor : firstProcessors) {
                        inputRows = processor.finish(textReader, inputRows);
                    }
                }
                processors          = remainderProcessors;
                remainderProcessors = null;
                firstProcessors     = null;
                for (Row row : inputRows) {
                    row = processRowInner(textReader, row, "");
                    if ( !textReader.getOkToRun()) {
                        break;
                    }
                    if (textReader.getExtraRow() != null) {
			row = processRowInner(textReader,
					      textReader.getExtraRow(), null);
                        textReader.setExtraRow(null);
                    }
                    if ( !textReader.getOkToRun()) {
                        break;
                    }
                }
            }
            if (firstProcessors != null) {
                for (Processor processor : firstProcessors) {
                    inputRows = processor.finish(textReader, inputRows);
                    if (inputRows == null) {
                        return null;
                    }
                }
            }

            textReader.flush();
            this.rows = inputRows;

            return this.rows;
        }


        /**
         * _more_
         *
         * @return _more_
         */
        public List<Row> getRows() {
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
    public static class RowOperator extends Processor {

        /** _more_ */
        public static final int OP_SUM = 0;

        /** _more_ */
        public static final int OP_MIN = 1;

        /** _more_ */
        public static final int OP_MAX = 2;

        /** _more_ */
        public static final int OP_AVERAGE = 3;


        /** _more_ */
        private int op = OP_SUM;

        /** _more_ */
        private List<Double> values;

        /** _more_ */
        private List<Integer> counts;

        /**
         * _more_
         */
        public RowOperator() {}

        /**
         * _more_
         *
         * @param op _more_
         */
        public RowOperator(int op) {
            this.op = op;
        }

        /**
         * _more_
         */
        public void reset() {
            values = null;
            counts = null;
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
         *
         * @throws Exception _more_
         */
        @Override
        public Row processRow(TextReader info, Row row, String line)
                throws Exception {
            boolean first = false;
            if (values == null) {
                values = new ArrayList<Double>();
                counts = new ArrayList<Integer>();
                first  = true;
            }
            for (int i = 0; i < row.size(); i++) {
                if (i >= values.size()) {
                    values.add(new Double(0));
                    counts.add(new Integer(0));
                }
                try {
                    String s            = row.getString(i).trim();
                    double value        = (s.length() == 0)
                                          ? 0
                                          : new Double(s).doubleValue();
                    double currentValue = values.get(i).doubleValue();
                    double newValue     = 0;
                    if (op == OP_SUM) {
                        newValue = currentValue + value;
                    } else if (op == OP_MIN) {
                        newValue = first
                                   ? value
                                   : Math.min(value, currentValue);
                    } else if (op == OP_MAX) {
                        newValue = first
                                   ? value
                                   : Math.max(value, currentValue);
                    } else if (op == OP_AVERAGE) {
                        newValue = currentValue + value;
                    } else {
                        System.err.println("NA:" + op);
                    }
                    values.set(i, newValue);
                    counts.set(i, new Integer(counts.get(i).intValue() + 1));
                } catch (Exception exc) {
                    //                    System.err.println("err:" + exc);
                    //                    System.err.println("line:" + theLine);
                }
            }

            return row;
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
            if (values == null) {
                Converter.ColumnSelector selector = info.getSelector();
                if ((selector != null)
                        && (selector.getIndices(info) != null)) {
                    for (int i = 0; i < selector.getIndices(info).size();
                            i++) {
                        if (i > 0) {
                            info.getWriter().print(",");
                        }
                        info.getWriter().print("-0");
                    }
                } else {
                    info.getWriter().print("-0");
                }
                //                System.err.println("no values");
            } else {
                for (int i = 0; i < values.size(); i++) {
                    double value = values.get(i);
                    if (op == OP_AVERAGE) {
                        value = value / counts.get(i);
                    }
                    if (i > 0) {
                        info.getWriter().print(",");
                    }
                    info.getWriter().print(info.formatValue(value));
                }
            }
            info.getWriter().flush();

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
    public static class RowCollector extends Processor {

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
         * @param cols _more_
         */
        public RowCollector(List<String> cols) {
            super(cols);
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
            rows = new ArrayList<Row>();
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
         *
         * @throws Exception _more_
         */
        @Override
        public Row processRow(TextReader info, Row row, String line)
                throws Exception {
            rows.add(row);

            return row;
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



    public static class MaxValue extends RowCollector {
	String key;
	String value;

        /**
         * _more_
         *
         */
        public MaxValue(String key, String value) {
	    this.key =key;
	    this.value  = value;
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
	    Hashtable<String,Row> map = new Hashtable<String,Row>();
            int keyIdx  =getIndex(this.key);
            int valueIdx  =getIndex(this.value);
            List<Row> newRows = new ArrayList<Row>();
	    int cnt =0;
            for (Row row : getRows()) {
		if(cnt++==0) {
		    newRows.add(row);
		    continue;
		}
                List values = row.getValues();
		String v = (String) row.get(keyIdx);
		Row maxRow = map.get(v);
		if(maxRow == null) {
		    map.put(v,row);
		    continue;
		}
		String v1  = (String)maxRow.get(valueIdx);
		String v2  = (String)row.get(valueIdx);
		int compare = v1.compareTo(v2);
		if(compare<0) map.put(v,row);
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
            rows = getRows(rows);
            for (int i = rows.size() - 1; i >= 0; i--) {
                Row row = rows.get(i);
                info.getWriter().println(
                    CsvUtil.columnsToString(row.getValues(), ","));
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
    public static class Printer extends Processor {

        /** _more_ */
        private String template;

        /** _more_ */
        private boolean addPointHeader = false;

        /** _more_ */
        private boolean trim = false;


        /**
         * ctor
         *
         * @param template _more_
         */
        public Printer(String template) {
            this.template = template;
        }

        /**
         * _more_
         *
         * @param template _more_
         * @param trim _more_
         */
        public Printer(String template, boolean trim) {
            this.template = template;
            this.trim     = trim;
        }

        /**
         * _more_
         *
         * @param addHeader _more_
         */
        public Printer(boolean addHeader) {
            this.addPointHeader = addHeader;
        }


        /**
         * _more_
         *
         * @param addHeader _more_
         * @param trim _more_
         */
        public Printer(boolean addHeader, boolean trim) {
            this(addHeader);
            this.trim = trim;
        }


        /**
         * _more_
         *
         * @param info _more_
         * @param row _more_
         * @param line _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        @Override
        public Row processRow(TextReader info, Row row, String line)
                throws Exception {
            if (addPointHeader) {
                addPointHeader = false;
                handleHeaderRow(info.getWriter(), row, null /*exValues*/);

                return row;
            }

            handleRow(info, info.getWriter(), row);

            return row;
        }

        /** _more_ */
        int xxcnt = 0;

        /**
         * _more_
         *
         * @param writer _more_
         * @param header _more_
         * @param exValues _more_
         *
         * @throws Exception _more_
         */
        private void handleHeaderRow(PrintWriter writer, Row header,
                                     List exValues)
                throws Exception {
            StringBuilder   sb     = new StringBuilder();
            HashSet<String> seen   = new HashSet<String>();
            List            values = header.getValues();
            for (int i = 0; i < values.size(); i++) {
                Object headerValue = values.get(i);
                if (i > 0) {
                    sb.append(",");
                } else {
                    sb.append("#fields=");
                }
                String name = (headerValue == null)
                              ? "field"
                              : headerValue.toString();
                addFieldDescriptor(name, sb, i, seen, null /*rows*/);
            }
            writer.println(sb.toString());
        }




        /**
         * _more_
         *
         * @param writer _more_
         * @param row _more_
         *
         * @throws Exception _more_
         */
        private void handleRow(TextReader info, PrintWriter writer, Row row) throws Exception {
	    boolean first = rowCnt++==0;
            String  theTemplate   = template;
            List    values        = row.getValues();
            boolean escapeColumns = true;
            for (int colIdx = 0; colIdx < values.size(); colIdx++) {
                Object v = values.get(colIdx);
                if (theTemplate == null) {
                    if (colIdx > 0) {
                        writer.print(",");
                    }
                    if (v != null) {
                        String sv = v.toString();
                        if (trim) {
                            sv = sv.trim();
                        }
                        if ((first && sv.startsWith("#"))  ||  ((colIdx == 0) && info.getCommentChar()!=null  && sv.startsWith(info.getCommentChar()))) {
                            escapeColumns = false;
                        }
                        boolean addQuote = false;
                        if (escapeColumns) {
                            addQuote = (sv.indexOf(",") >= 0)
                                       || (sv.indexOf("\n") >= 0);
                            if (sv.indexOf("\"") >= 0) {
                                addQuote = true;
                                sv       = sv.replaceAll("\"", "\"\"");
                            }
                            if (addQuote) {
                                writer.print("\"");
                            }
                        }
                        writer.print(sv);
                        if (addQuote) {
                            writer.print("\"");
                        }
                    } else {
                        writer.print("");
                    }
                } else {
                    theTemplate = theTemplate.replace("${" + colIdx + "}",
                            v.toString());
                }
            }
            if (theTemplate == null) {
                writer.print("\n");
            } else {
                writer.print(theTemplate);
            }
        }


        /**
         * _more_
         *
         * @param writer _more_
         * @param rows _more_
         *
         * @throws Exception _more_
         */
        public void writeCsv(TextReader info, PrintWriter writer, List<Row> rows)
                throws Exception {
            if (addPointHeader) {
                Row header = rows.get(0);
                rows.remove(0);
                List exValues = ((rows.size() > 0)
                                 ? rows.get(0).getValues()
                                 : null);

                handleHeaderRow(writer, header, exValues);
            }

            for (int i = 0; i < rows.size(); i++) {
                Row row = rows.get(i);
                handleRow(info, writer, row);

            }
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
            rows = getRows(rows);
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
     * @version        $version$, Wed, Jul 8, '15
     * @author         Enter your name here...
     */
    public static class Exploder extends RowCollector {

        /** _more_ */
        private int column;



        /**
         * ctor
         *
         *
         * @param col _more_
         */
        public Exploder(int col) {
            this.column = col;
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
            rows = getRows(rows);
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
     * @version        $version$, Tue, Jan 30, '18
     * @author         Enter your name here...
     */
    public static class DbXml extends Processor {

        /** _more_ */
        private Hashtable<String, String> props;

        /** _more_ */
        private Row row1;

        /** _more_ */
        private String tableId;


        /**
         * _more_
         *
         * @param props _more_
         */
        public DbXml(Hashtable<String, String> props) {
            this.props = props;
        }


        /**
         * Get the TableId property.
         *
         * @return The TableId
         */
        public String getTableId() {
            return tableId;
        }


        /**
         * _more_
         *
         * @param reader _more_
         * @param row _more_
         * @param line _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        @Override
        public Row processRow(TextReader reader, Row row, String line)
                throws Exception {

            if (row1 == null) {
                row1 = row;
                rowCnt++;

                return row;
            }
            rowCnt++;
            if (rowCnt > 2) {
                return null;
            }

            String      file   = reader.getInputFile();
            PrintWriter writer = null;
            String      name = IOUtil.stripExtension(Utils.getFileTail(file));
            name = CsvUtil.getDbProp(props, "table", "name", name);

            String label = Utils.makeLabel(name);
            label   = CsvUtil.getDbProp(props, "table", "label", label);
            label   = label.replaceAll("\n", " ").replaceAll("\r", " ");
            tableId = Utils.makeLabel(name).toLowerCase().replaceAll(" ",
                                      "_");
            tableId = CsvUtil.getDbProp(props, "table", "id", tableId);

            String labels = CsvUtil.getDbProp(props, "table", "labelColumns",
                                "");

            File output = reader.getOutputFile();
            if (output != null) {
                reader.setOutputFile(
                    new File(
                        IOUtil.joinDir(
                            output.getParentFile(), tableId + "db.xml")));
            }

            if (writer == null) {
                writer = reader.getWriter();
                writer.println("<tables>");
            }


            writer.println(
                XmlUtil.openTag(
                    "table",
                    XmlUtil.attrs(
                        "id", tableId, "name", label, "labelColumns", labels,
                        "icon",
                        CsvUtil.getDbProp(
                            props, "table", "icon", "/db/database.png"))));
            List<Row> samples = new ArrayList<Row>();
            samples.add(row);
            boolean[] isNumeric = new boolean[row1.getValues().size()];
            for (int i = 0; i < isNumeric.length; i++) {
                isNumeric[i] = false;
            }

            for (Row sample : samples) {
                //                System.err.println("sample:" + sample);
                for (int colIdx = 0; colIdx < sample.getValues().size();
                        colIdx++) {
                    Object value = sample.getValues().get(colIdx);
                    try {
                        Double.parseDouble(value.toString());
                        //                        System.err.println("OK: " + row1.getValues().get(colIdx));
                        isNumeric[colIdx] = true;
                    } catch (Exception ignore) {}
                }
            }

            boolean dfltDoStats = CsvUtil.getDbProp(props, "table",
                                      "dostats", "false").equals("true");
            boolean dfltCanSearch = CsvUtil.getDbProp(props, "table",
                                        "cansearch", "true").equals("true");
            boolean dfltCanList = CsvUtil.getDbProp(props, "table",
                                      "canlist", "true").equals("true");
            String dfltChangeType = CsvUtil.getDbProp(props, "table",
                                        "changetype", "false");

            String format = CsvUtil.getDbProp(props, "table", "format",
                                "yyyy-MM-dd HH:mm");
            for (int colIdx = 0; colIdx < row1.getValues().size(); colIdx++) {
                Object col   = row1.getValues().get(colIdx);
                String colId = Utils.makeLabel(col.toString());
                colId = colId.toLowerCase().replaceAll(" ",
                        "_").replaceAll("[^a-z0-9]", "_");
                colId = colId.replaceAll("_+_", "_");
                colId = colId.replaceAll("_$", "");
                colId = CsvUtil.getDbProp(props, colId, "id", colId);
                label = Utils.makeLabel(colId);
                label = CsvUtil.getDbProp(props, colId, "label", label);
                label = label.replaceAll("\n", " ").replaceAll("\r", " ");

                if (CsvUtil.getDbProp(props, colId, "skip",
                                      "false").equals("true")) {
                    continue;
                }



                boolean isNumber = isNumeric[colIdx];
                String type = CsvUtil.getDbProp(props, "table", "type",
                                  "string");
                if (isNumber) {
                    type = "double";
                }

                StringBuilder attrs     = new StringBuilder();
                boolean       canList   = dfltCanList;
                boolean       canSearch = dfltCanSearch;



                attrs.append(XmlUtil.attrs(new String[] { "name", colId }));

                if (CsvUtil.getDbProp(props, colId, "changetype",
                                      dfltChangeType).equals("true")) {
                    attrs.append(XmlUtil.attrs(new String[] { "changetype",
                            "true" }));
                }
                String size = CsvUtil.getDbProp(props, colId, "size", null);
                if (size != null) {
                    attrs.append(XmlUtil.attrs(new String[] { "size",
                            size }));
                }
                if ((colId.indexOf("type") >= 0)
                        || (colId.indexOf("category") >= 0)) {
                    type = "enumerationplus";
                } else if (colId.equals("date")) {
                    type = "date";
                    if (props.get("-" + colId + ".type") != null) {
                        type = props.get("-" + colId + ".type");
                    }
                }

                type = CsvUtil.getDbProp(props, colId, "type", type);
                String values = CsvUtil.getDbProp(props, colId, "values",
                                    null);
                String searchRows = CsvUtil.getDbProp(props, colId,
                                        "searchrows", "");
                String defaultsort = CsvUtil.getDbProp(props, colId,
                                         "defaultsort", (String) null);
                if ((defaultsort != null) && defaultsort.equals("true")) {
                    attrs.append(XmlUtil.attrs(new String[] { "defaultsort",
                            "true" }));
                    String asc = CsvUtil.getDbProp(props, colId, "ascending",
                                     (String) null);
                    if (asc != null) {
                        attrs.append(XmlUtil.attrs(new String[] { "ascending",
                                asc }));
                    }
                }


                canSearch = "true".equals(CsvUtil.getDbProp(props, colId,
                        "cansearch", canSearch + ""));
                canList = "true".equals(CsvUtil.getDbProp(props, colId,
                        "canlist", canList + ""));
                attrs.append(XmlUtil.attrs(new String[] {
                    "type", type, "label", label, "cansearch", "" + canSearch,
                    "canlist", "" + canList
                }));
                if (values != null) {
                    attrs.append(XmlUtil.attrs(new String[] { "values",
                            values }));
                }
                if (searchRows.length() > 0) {
                    attrs.append(XmlUtil.attrs(new String[] { "searchrows",
                            searchRows }));
                }
                if (type.equals("date")) {
                    attrs.append(XmlUtil.attrs(new String[] { "format",
                            CsvUtil.getDbProp(props, colId, "format",
                            format) }));
                }

                StringBuffer inner = new StringBuffer();
                boolean doStats = "true".equals(CsvUtil.getDbProp(props,
                                      colId, "dostats", dfltDoStats + ""));

                if (doStats) {
                    inner.append(XmlUtil.tag("property",
                                             XmlUtil.attrs(new String[] {
                                                 "name",
                            "dostats", "value", "true" })));
                }
                if (CsvUtil.getDbProp(props, colId, "iscategory", false)) {
                    inner.append(XmlUtil.tag("property",
                                             XmlUtil.attrs(new String[] {
                                                 "name",
                            "iscategory", "value", "true" })));
                }
                if (CsvUtil.getDbProp(props, colId, "formap", false)) {
                    inner.append(XmlUtil.tag("property",
                                             XmlUtil.attrs(new String[] {
                                                 "name",
                            "formap", "value", "true" })));
                }

                if (CsvUtil.getDbProp(props, colId, "islabel", false)) {
                    inner.append(XmlUtil.tag("property",
                                             XmlUtil.attrs(new String[] {
                                                 "name",
                            "label", "value", "true" })));
                }

                if (inner.length() > 0) {
                    writer.println(XmlUtil.tag("column", attrs.toString(),
                            inner.toString()));
                } else {
                    writer.println(XmlUtil.tag("column", attrs.toString()));
                }
            }

            writer.println(XmlUtil.closeTag("table"));
            writer.println("</tables>");
            writer.flush();

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
    public static class Uniquifier extends Processor {

        /** _more_ */
        private List<HashSet> contains;

        /** _more_ */
        private List<List> values;

        /**
         * _more_
         */
        public Uniquifier() {}

        /**
         * _more_
         */
        public void reset() {
            contains = null;
            values   = null;
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
         *
         * @throws Exception _more_
         */
        @Override
        public Row processRow(TextReader info, Row row, String line)
                throws Exception {
            boolean first = false;
            if (contains == null) {
                contains = new ArrayList<HashSet>();
                values   = new ArrayList<List>();
            }
            for (int i = 0; i < row.size(); i++) {
                if (i >= values.size()) {
                    contains.add(new HashSet());
                    values.add(new ArrayList());
                }
                String s = row.getString(i).trim();
                if (contains.get(i).contains(s)) {
                    continue;
                }
                contains.get(i).add(s);
                values.get(i).add(s);
            }

            return row;
        }

        /**
         *   _more_
         *
         *   @param info _more_
         * @param rows _more_
         *
         *
         * @return _more_
         *   @throws Exception On badness
         */
        @Override
        public List<Row> finish(TextReader info, List<Row> rows)
                throws Exception {
            if (contains == null) {
                info.getWriter().print("-0");
            } else {
                for (int i = 0; i < values.size(); i++) {
                    List uniqueValues = values.get(i);
                    for (int j = 0; j < uniqueValues.size(); j++) {
                        if (j > 0) {
                            //                            info.getWriter().print(",");
                        }
                        info.getWriter().println(uniqueValues.get(j));
                    }
                }
            }
            info.getWriter().flush();

            return rows;
        }


    }


    /**
     * Class description
     *
     *
     * @version        $version$, Mon, Jan 12, '15
     * @author         Enter your name here...
     */
    public static class Counter extends Processor {

        /** _more_ */
        private int rowCount;

        /** _more_ */
        private int currentNumCols = -1;

        /** _more_ */
        private boolean strict = false;

        /** _more_ */
        private boolean error = false;

        /** _more_ */
        private Hashtable<Integer, Integer> uniqueCounts =
            new Hashtable<Integer, Integer>();

        /** _more_ */
        private List<Integer> counts = new ArrayList<Integer>();

        /**
         * _more_
         */
        public Counter() {}

        /**
         * _more_
         *
         * @param strict _more_
         */
        public Counter(boolean strict) {
            this.strict = strict;
        }


        /**
         * _more_
         *
         * @param strict _more_
         * @param error _more_
         */
        public Counter(boolean strict, boolean error) {
            this.strict = strict;
            this.error  = error;
        }

        /**
         * _more_
         */
        public void reset() {
            rowCount     = 0;
            uniqueCounts = new Hashtable<Integer, Integer>();
            counts       = new ArrayList<Integer>();
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
         *
         * @throws Exception _more_
         */
        @Override
        public Row processRow(TextReader info, Row row, String line)
                throws Exception {
            int size = row.size();

            if (currentNumCols < 0) {
                currentNumCols = size;
            }


            if (strict) {
                if (size != currentNumCols) {
                    if (error) {
                        throw new IllegalArgumentException("Bad line:"
                                + line);
                    }
                    System.err.println("skipping:" + line);

                    return null;
                }
            }

            currentNumCols = row.size();
            rowCount++;

            Integer count = uniqueCounts.get(size);
            if (count == null) {
                uniqueCounts.put(size, 0);
                counts.add(size);
                count = 0;
            }
            uniqueCounts.put(size, count + 1);

            return row;
        }

        /**
         *   _more_
         *
         *   @param info _more_
         * @param rows _more_
         *
         *
         * @return _more_
         *   @throws Exception On badness
         */
        @Override
        public List<Row> finish(TextReader info, List<Row> rows)
                throws Exception {
            if (strict) {
                return rows;
            }

            info.getWriter().println("Rows:" + rowCount);
            for (Integer cnt : counts) {
                int value = uniqueCounts.get(cnt);
                info.getWriter().println("\tcolumns:" + cnt + " #:" + value);
            }
            info.flush();

            return rows;
        }

    }



    /**
     * Class description
     *
     *
     * @version        $version$, Mon, Jul 29, '19
     * @author         Enter your name here...
     */
    public static class Logger extends Processor {

        /** _more_ */
        private int total;

        /** _more_ */
        private int rowCount;

        /*
         * _more_
         */

        /**
         * _more_
         */
        public Logger() {}

        /**
         * _more_
         *
         *
         * @param info _more_
         * @param row _more_
         * @param line _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        @Override
        public Row processRow(TextReader info, Row row, String line)
                throws Exception {
            total++;
            if (++rowCount >= 1000) {
                System.err.println("count:" + total);
                rowCount = 0;
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
    public static class Prettifier extends Processor {

        /** _more_ */
        private List headerValues;

        /** _more_ */
        private int cnt = 0;


        /*
         * _more_
         *
         */

        /**
         * _more_
         */
        public Prettifier() {}




        /**
         * _more_
         *
         *
         * @param info _more_
         * @param row _more_
         * @param line _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        @Override
        public Row processRow(TextReader info, Row row, String line)
                throws Exception {
            if (headerValues == null) {
                headerValues = row.getValues();

                return row;
            }
            printRow(info, row);

            return row;
        }


        /**
         * _more_
         *
         * @param info _more_
         * @param row _more_
         *
         * @throws Exception _more_
         */
        public void printRow(TextReader info, Row row) throws Exception {
            if (headerValues == null) {
                headerValues = row.getValues();

                return;
            }
            List values = row.getValues();
            cnt++;
            info.getWriter().println("#" + cnt);
            for (int i = 0; i < values.size(); i++) {
                String label = (i < headerValues.size())
                               ? headerValues.get(i).toString()
                               : "NA";
                label = StringUtil.padLeft(label, 20);
                info.getWriter().println(label + ":" + values.get(i));
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
    public static class Html extends Processor {

        /** _more_ */
        private int cnt = 0;


        /**
         * _more_
         *
         */
        public Html() {}


        /**
         * _more_
         *
         *
         * @param info _more_
         * @param row _more_
         * @param line _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        @Override
        public Row processRow(TextReader info, Row row, String line)
                throws Exception {
            printRow(info, row);

            return row;
        }

        /**
         * _more_
         *
         * @param info _more_
         * @param row _more_
         *
         * @throws Exception _more_
         */
        public void printRow(TextReader info, Row row) throws Exception {
            if (cnt == 0) {
                info.getWriter().println(
                    "<table  class='stripe hover ramadda-table' xtable-height=400>");
            }
            List   values = row.getValues();
            String open   = "<td>";
            String close  = "</td>";

            if (cnt == 0) {
                info.getWriter().println("<thead>");
                info.getWriter().println("<tr valign=top>");
                open  = "<th>";
                close = "</th>";
            } else {
                info.getWriter().println("<tr  valign=top>");
            }



            for (int i = 0; i < values.size(); i++) {
                if (i == 0) {
                    info.getWriter().print(open);
                    info.getWriter().print(
                        "<div style='white-space:nowrap;max-width:120px;overflow-x:auto;'>");
                    if (cnt == 0) {
                        info.getWriter().print("&nbsp;");
                    } else {
                        info.getWriter().print("#" + cnt);
                    }
                    info.getWriter().print("</div>");
                    info.getWriter().print(close);
                }
                info.getWriter().print(open);
                info.getWriter().print(
                    "<div style='white-space:nowrap;max-width:120px;overflow-x:auto;'>");
                if (cnt == 0) {
                    info.getWriter().print("#" + i + "&nbsp;");
                    String label = Utils.makeLabel(""
                                       + values.get(i)).replaceAll(" ",
                                           "&nbsp;");
                    info.getWriter().print(HtmlUtils.span(label,
                            HtmlUtils.attr("title", label)));
                } else {
                    Object value = values.get(i);
                    String label = ((value == null)
                                    ? ""
                                    : value.toString());
                    info.getWriter().print(HtmlUtils.span(label,
                            HtmlUtils.attr("title", label)));
                }
                info.getWriter().print("</div>");
                info.getWriter().print(close);
            }
            if (cnt == 0) {
                info.getWriter().println("</tr>");
                info.getWriter().println("</thead>");
                info.getWriter().println("</tbody>");
            } else {
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
        private int unfurlIndex;

        /** _more_ */
        private int uniqueIndex;

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
         * @param unfurlIndex _more_
         * @param valueCols _more_
         * @param uniqueIndex _more_
         * @param extraCols _more_
         */
        public Unfurler(int unfurlIndex, List<String> valueCols,
                        int uniqueIndex, List<String> extraCols) {
            super(extraCols);
            this.unfurlIndex = unfurlIndex;
            this.valueCols   = valueCols;
            this.uniqueIndex = uniqueIndex;
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

            valueIndices = getIndices(valueCols);

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
            int mapCnt = 0;
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
                List<String> toks = StringUtil.split(v, ":unit:");
                indexMap.put(toks.get(0), new Integer(i));
                indexMap.put(v, new Integer(i));
            }
            List          header  = headerRow.getValues();
            StringBuilder sb      = new StringBuilder();


            List<Row>     newRows = new ArrayList<Row>();
            newColumns.add(0, header.get(uniqueIndex).toString());
            int cnt = 0;
            for (int i : includes) {
                newColumns.add(1 + cnt, (String) header.get(i));
                //                System.err.println(" -- " + i + " " + newColumns);
                cnt++;
            }
            Object[] array = new Object[newColumns.size()];


            for (String u : uniques) {
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
     * @version        $version$, Fri, Jan 9, '15
     * @author         Jeff McWhirter
     */
    public static class Dups extends RowCollector {

	private List<String> toks;



        /**
         * _more_
         *
         *
         * @param unfurlIndex _more_
         * @param valueCols _more_
         * @param uniqueIndex _more_
         * @param extraCols _more_
         */
        public Dups(List<String> toks) {
	    this.toks = toks;
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
            List<Row> allRows   = getRows();
	    List<Integer> cols = Utils.toInt(toks);
	    newRows.add(allRows.get(0));
            Hashtable<String,Row>     seen    = new Hashtable<String,Row>();
	    HashSet   seenKeys    = new HashSet();
	    
            for (int i=1;i<allRows.size();i++) {
		Row row = allRows.get(i);
		String key ="";
		for(int idx:cols)
		    key+=row.get(idx)+"__";
		if(seenKeys.contains(key)) {
		    newRows.add(row);
		    continue;
		}
		Row seenRow = seen.get(key);
		if(seenRow==null) {
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

	private String delimiter;

	private String name;

        /**
         * _more_
         *
         *
         * @param unfurlIndex _more_
         * @param valueCols _more_
         * @param uniqueIndex _more_
         * @param extraCols _more_
         */
        public Splatter(String key, String value, String delim, String name) {
            this.key = key; 
            this.value = value;
	    this.delimiter = delim;
	    this.name = name;
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
	    int keyIndex =  getIndices(StringUtil.split(key,",",true,true)).get(0);
	    int valueIndex =  getIndices(StringUtil.split(value,",",true,true)).get(0);
            List<Row> allRows   = getRows();
            Row       headerRow = allRows.get(0);
	    headerRow.add(name);
	    newRows.add(headerRow);
            allRows.remove(0);
            Hashtable<Object, Row> map = new Hashtable<Object, Row>();
            for (Row row : allRows) {
		Object key = row.get(keyIndex);
		Row existing = map.get(key);
		Object value = row.get(valueIndex);
		if(existing==null) {
		    existing = new Row(row.getValues());
		    map.put(key,existing);
		    newRows.add(existing);
		    existing.add("X:" + value);
		    System.out.println("splat:" + existing.myx);
		} else {
		    for(int i=0;i<row.size();i++) {
			existing.set(i,row.get(i));
		    }
		    existing.set(existing.size()-1,existing.get(existing.size()-1)+delimiter+value);
		}
		//		System.out.println("key:" + key +" row:" + existing);
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

        /** _more_          */
        private String label1;

        /** _more_          */
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
            rows = getRows();
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
        private int index;

        /** _more_ */
        private boolean asc = true;

        /**
         * _more_
         *
         *
         * @param index _more_
         * @param asc _more_
         */
        public Sorter(int index, boolean asc) {
            this.index = index;
            this.asc   = asc;
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
     * _more_
     *
     * @param rows _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void toCsv(List<Row> rows, Appendable sb) throws Exception {
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            sb.append(CsvUtil.columnsToString(row.getValues(), ","));
            sb.append("\n");
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
        private List<String> keys;

        /** _more_ */
        private List<String> values;

        /**
         * _more_
         *
         *
         *
         * @param keys _more_
         * @param values _more_
         */
        public Summer(List<String> keys, List<String> values) {
            this.keys   = keys;
            this.values = values;
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
            uniqueIndices = getIndices(keys);
            valueIndices  = getIndices(values);
            List<Integer> allIndices = new ArrayList<Integer>();
            allIndices.addAll(uniqueIndices);
            allIndices.addAll(valueIndices);
            HashSet<Integer> allIndicesMap =
                (HashSet<Integer>) Utils.makeHashSet(allIndices);

            int          rowIndex = 0;
            List<String> keys     = new ArrayList<String>();
            Hashtable<String, List<Row>> rowMap = new Hashtable<String,
                                                      List<Row>>();
            List<Row> allRows   = getRows(rows);
            Row       headerRow = allRows.get(0);
            allRows.remove(0);
            //            Collections.sort(allRows,new Row.RowCompare(uniqueIndex));
            Object[] array = null;
            for (Row row : allRows) {
                List          values = row.getValues();
                StringBuilder keySB  = new StringBuilder();
                for (int i : uniqueIndices) {
                    keySB.append(values.get(i));
                    keySB.append("_");
                }
                String    key      = keySB.toString();
                List<Row> rowGroup = rowMap.get(key);
                if (rowGroup == null) {
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

	    if(valueIndices.size()==0) {
		newHeader.add("Count");
	    }
            newRows.add(newHeader);
            for (String key : keys) {
		if(valueIndices.size()==0) {
		    //just count them
		    List<Row> rowGroup = rowMap.get(key);
		    Row row = rowGroup.get(0);
		    Row newRow = new Row();
		    newRows.add(newRow);
		    for (int i : uniqueIndices) {
			newRow.add(row.get(i));
		    }
		    newRow.add(rowGroup.size());
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
            }

            return newRows;

        }




    }




    /**
     * Class description
     *
     *
     * @version        $version$, Wed, Feb 20, '19
     * @author         Enter your name here...
     */
    public static class Joiner extends RowCollector {

        /** _more_ */
        private List<String> keys1;

        /** _more_ */
        private List<String> values1;

        /** _more_ */
        private List<String> keys2;

        /** _more_ */
        private List<String> values2;

        /** _more_ */
        private String file;

        /**
         * _more_
         *
         *
         * @param keys1 _more_
         * @param values1 _more_
         * @param file _more_
         * @param keys2 _more_
         * @param values2 _more_
         */
        public Joiner(List<String> keys1, List<String> values1, String file,
                      List<String> keys2, List<String> values2) {
            this.keys1   = keys1;
            this.values1 = values1;
            this.keys2   = keys2;
            this.values2 = values2;
            this.file    = file;
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
            List<Integer> keys1Indices   = getIndices(keys1);
            List<Integer> values1Indices = getIndices(values1);
            List<Integer> keys2Indices   = getIndices(keys2);
            List<Integer> values2Indices = getIndices(values2);
            List<Row>     newRows        = new ArrayList<Row>();
            BufferedReader br = new BufferedReader(
                                    new InputStreamReader(
                                        getInputStream(file)));
            TextReader reader = new TextReader(br);
	    Hashtable<String,Row> map = new Hashtable<String,Row>();
	    
            List<Row>  rows2  = new ArrayList<Row>();
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                List<String> cols = Utils.tokenizeColumns(line, ",");
		String key = "";
		for(int i:keys1Indices) 
		    key += cols.get(i);
		map.put(key,new Row(cols));
            }

            for (Row row : getRows()) {
		String key = "";
		for(int i:keys2Indices) {
		    key += row.getString(i);
		}
		Row other = map.get(key);
		if(other==null) {
		    System.err.println("no join:" + row);
		    continue;
		}
		for(int j: values1Indices) {
		    row.add(other.get(j));
		}
		newRows.add(row);
	    }
            return newRows;
        }




    }




}
