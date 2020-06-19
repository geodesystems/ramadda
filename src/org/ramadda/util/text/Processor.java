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
        info.setCurrentOperator(null);

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
    public List<Row> processRowReturnList(TextReader info, Row row,
                                          String line)
            throws Exception {
        List<Row> l = new ArrayList<Row>();
        Row       r = processRow(info, row, line);
        if (r != null) {
            l.add(r);
        }

        return l;
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
        int xcnt = 0;

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
            info.setCurrentOperator(this);
            Object  skipTo      = row.getSkipTo();
            boolean sawBufferer = false;
            if (remainderProcessors == null) {
                remainderProcessors = new ArrayList<Processor>();
                firstProcessors     = new ArrayList<Processor>();
                for (int i = 0; i < processors.size(); i++) {
                    Processor processor = processors.get(i);
                    if (skipTo != null) {
                        if (skipTo == processor) {
                            System.err.println("clearing skip to");
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

            boolean   firstRow = rowCnt++ == 0;
            List<Row> rows     = new ArrayList<Row>();
            rows.add(row);
            for (Processor processor : firstProcessors) {
                if (skipTo != null) {
                    if (skipTo == processor) {
                        //                      System.err.println("skipping:" + processor);
                        skipTo = null;
                    }

                    continue;
                }
                if (firstRow) {
                    processor.setHeader(row.getValues());
                }
                //Always do this here so the indexes don't get screwed up 
                processor.getIndices(info);
                if (Row.xcnt == 5) {
                    System.out.println("row: " + row);
                }
                info.setCurrentOperator(processor);
                //              System.err.println("calling:" +  processor.getClass().getName() +" with :" + row);
                row = processor.processRow(info, row, line);
                //              System.err.println("got:" +   row);
                info.setCurrentOperator(this);
                if (row == null) {
                    return null;
                }
            }

            return row;
        }

        /*
rotate -> pass -> pass -> rotate -> pass
         */

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

            Object skipTo = row.getSkipTo();

            for (Processor processor : firstProcessors) {
                //              System.out.println("before: " + processor.getClass().getName().replace("org.ramadda.util.text.","") +" row:" + row.myx +" size:" + row.size());

                if (skipTo != null) {
                    if (skipTo == processor) {
                        //                      System.err.println("skipping:" + processor);
                        skipTo = null;
                    }

                    continue;
                }


                row = processor.processRow(info, row, line);
                if (row == null) {
                    return null;
                }
                //              System.out.println("after: " +" row:" + row.myx +" size:" + row.size());
            }

            return row;
        }

        public List<Row> finish(TextReader textReader, List<Row> inputRows)
                throws Exception {
	    //	    return finishOld(textReader, inputRows);
	    return finishNew(textReader, inputRows);	    
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
        public List<Row> finishOld(TextReader textReader, List<Row> inputRows)
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
         *
         * @param textReader _more_
         * @param inputRows _more_
         *
         *
         * @return _more_
         * @throws Exception On badness
         */
        public List<Row> finishNew(TextReader textReader, List<Row> inputRows)
                throws Exception {
	    //            if (inputRows != null) {
                while ((remainderProcessors != null)
                        && (remainderProcessors.size() > 0)) {
                    if (firstProcessors != null) {
                        for (Processor processor : firstProcessors) {
                            inputRows = processor.finish(textReader,
                                    inputRows);
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
		//            }
            if (firstProcessors != null) {
                for (Processor processor : firstProcessors) {
                    List<Row> tmp = processor.finish(textReader, inputRows);
                    if (tmp != null) {
                        inputRows = tmp;
                        //                        return null;
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
            List<Integer> valueIndices = getIndices(valueCols);
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
     * @version        $version$, Sun, Apr 5, '20
     * @author         Enter your name here...
     */
    public static class Propper extends Processor {

        /** _more_ */
        public static final int FLAG_NONE = -1;

        /** _more_ */
        public static final int FLAG_POSITION = 0;

        /** _more_ */
        private int flag;

        /** _more_ */
        private boolean value;

        /**
         * _more_
         *
         * @param flag _more_
         * @param value _more_
         */
        public Propper(String flag, String value) {
            this.flag = flag.equals("position")
                        ? FLAG_POSITION
                        : FLAG_NONE;
            if (this.flag == FLAG_POSITION) {
                this.value = value.equals("start");
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
            if (flag == FLAG_POSITION) {
                info.setPositionStart(value);
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
    public static class Pass extends Processor {

        /**
         * _more_
         */
        public Pass() {}


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
            System.err.println("#" + (rowCnt++) + " row:" + row);

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
    public static class Verifier extends Processor {

        /** _more_ */
        private int cnt = -1;

        /**
         * _more_
         */
        public Verifier() {}


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
            if (cnt == -1) {
                cnt = row.size();

                return row;
            }
            if (row.size() != cnt) {
                throw new IllegalArgumentException("Bad column count:"
                        + row.size() + " row:" + row);
            }

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
            rows = getRows(rows);
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
     * @version        $version$, Wed, Jul 8, '15
     * @author         Enter your name here...
     */
    public static class Printer extends Processor {

        /** _more_ */
        private String template;

        /** _more_ */
        private String prefix;

        /** _more_ */
        private String delimiter;

        /** _more_ */
        private String suffix;

        /** _more_ */
        private boolean addPointHeader = false;

        /** _more_ */
        private boolean trim = false;


        /**
         * ctor
         *
         *
         * @param prefix _more_
         * @param template _more_
         * @param delimiter _more_
         * @param suffix _more_
         */
        public Printer(String prefix, String template, String delimiter,
                       String suffix) {
            this.prefix    = prefix;
            this.template  = template;
            this.delimiter = delimiter;
            this.suffix    = suffix;
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
            if (suffix != null) {
                info.getWriter().print(suffix);
            }

            return rows;
        }



        /**
         * _more_
         *
         *
         * @param info _more_
         * @param writer _more_
         * @param row _more_
         *
         * @throws Exception _more_
         */
        private void handleRow(TextReader info, PrintWriter writer, Row row)
                throws Exception {
            boolean first = rowCnt++ == 0;
            if (first && (prefix != null)) {
                writer.print(prefix);
            }
            if ( !first && (delimiter != null)) {
                writer.print(delimiter);
            }
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
                        if ((first && sv.startsWith("#"))
                                || ((colIdx == 0)
                                    && (info.getCommentChar() != null)
                                    && sv.startsWith(
                                        info.getCommentChar()))) {
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
         *
         * @param info _more_
         * @param writer _more_
         * @param rows _more_
         *
         * @throws Exception _more_
         */
        public void writeCsv(TextReader info, PrintWriter writer,
                             List<Row> rows)
                throws Exception {
            if (prefix != null) {
                writer.print(prefix);
            }

            if (addPointHeader) {
                Row header = rows.get(0);
                rows.remove(0);
                List exValues = ((rows.size() > 0)
                                 ? rows.get(0).getValues()
                                 : null);

                handleHeaderRow(writer, header, exValues);
            }

            for (int i = 0; i < rows.size(); i++) {
                if ((i > 0) && (delimiter != null)) {
                    writer.print(delimiter);
                }
                Row row = rows.get(i);
                handleRow(info, writer, row);
            }
            if (suffix != null) {
                writer.print(suffix);
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
     * @version        $version$, Fri, Jan 16, '15
     * @author         Enter your name here...
     */
    public static class ToXml extends RowCollector {

        /** _more_ */
        Row header = null;

        /** _more_ */
        String tag;

        /**
         * _more_
         *
         * @param tag _more_
         */
        public ToXml(String tag) {
            super();
            this.tag = tag;
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
         * @param line _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        @Override
        public Row processRow(TextReader info, Row row, String line)
                throws Exception {
            PrintWriter writer = info.getWriter();
            if (header == null) {
                header = row;
                writer.println("<" + tag + ">");

                return row;
            }
            writer.println("<entry>");
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
            writer.println("</entry>");

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
        int xcnt = 0;

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
                    "<table  class='stripe hover ramadda-table ramadda-csv-table' >");
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


            String style = "white-space:nowrap;overflow-x:auto;";
            //Check for the width
            int    lineWidth = 0;
            String s         = "";
            for (int i = 0; i < values.size(); i++) {
                lineWidth += values.get(i).toString().length();
                s         += " " + values.get(i);
            }
            if (lineWidth > 200) {
                style += "max-width:120px;";
            }

            for (int i = 0; i < values.size(); i++) {
                if (i == 0) {
                    info.getWriter().print(open);
                    info.getWriter().print("<div style='" + style + "'>");
                    if (cnt == 0) {
                        info.getWriter().print("&nbsp;");
                    } else {
                        info.getWriter().print("#" + cnt);
                    }
                    info.getWriter().print("</div>");
                    info.getWriter().print(close);
                }
                info.getWriter().print(open);
                info.getWriter().print("<div style='" + style + "'>");
                if (cnt == 0) {
                    info.getWriter().print("#" + i + "&nbsp;");
                    String label = Utils.makeLabel(""
                                       + values.get(i)).replaceAll(" ",
                                           "&nbsp;");
                    info.getWriter().print(HtmlUtils.span(label,
                            HtmlUtils.attr("title",
                                           label.replaceAll("\"",
                                               "&quot;"))));
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
            rows = getRows();
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
            int keyIndex = getIndices(StringUtil.split(key, ",", true,
                               true)).get(0);
            int valueIndex = getIndices(StringUtil.split(value, ",", true,
                                 true)).get(0);
            List<Row> allRows   = getRows();
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

            uniqueIndices = getIndices(keys);
            valueIndices  = getIndices(values);
            valueIndices  = getIndices(values);
            extraIndices  = getIndices(extra);
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
                    keySB.append(values.get(i));
                    keySB.append("_");
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
     * @version        $version$, Wed, Feb 20, '19
     * @author         Enter your name here...
     */
    public static class Joiner extends Processor {

        /** _more_ */
        private List<String> keys1;

        /** _more_ */
        private List<String> values1;

        /** _more_ */
        private List<String> keys2;

        /** _more_ */

        /** _more_ */
        private String file;

        /** _more_ */
        private Hashtable<String, Row> map;

        /** _more_ */
        private Row headerRow1;

        /** _more_ */
        private List<Integer> values1Indices;

        /** _more_ */
        private Row headerRow2;

        /**
         * _more_
         *
         *
         * @param keys1 _more_
         * @param values1 _more_
         * @param file _more_
         * @param keys2 _more_
         */
        public Joiner(List<String> keys1, List<String> values1, String file,
                      List<String> keys2) {
            this.keys1   = keys1;
            this.values1 = values1;
            this.keys2   = keys2;
            this.file    = file;
            try {
                init();
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }
        }


        /**
         * _more_
         *
         * @throws Exception _more_
         */
        private void init() throws Exception {
            List<Integer> keys1Indices = getIndices(keys1);
            values1Indices = getIndices(values1);
            BufferedReader br = new BufferedReader(
                                    new InputStreamReader(
                                        getInputStream(file)));
            TextReader reader = new TextReader(br);
            map        = new Hashtable<String, Row>();
            headerRow1 = null;
            String delimiter = null;
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                if (delimiter == null) {
                    if (line.indexOf("\t") >= 0) {
                        delimiter = "\t";
                    } else {
                        delimiter = ",";
                    }
                }
                List<String> cols = Utils.tokenizeColumns(line, delimiter);
                String       key  = "";
                for (int i : keys1Indices) {
                    key += cols.get(i) + "_";
                }
                Row row = new Row(cols);
                if (headerRow1 == null) {
                    headerRow1 = row;
                }
                map.put(key, row);
            }
        }



        /**
         * _more_
         *
         * @param info _more_
         * @param row _more_
         * @param line _more_
         *
         *
         * @return _more_
         * @throws Exception On badness
         */
        @Override
        public Row processRow(TextReader info, Row row, String line)
                throws Exception {
            List<Integer> keys2Indices = getIndices(keys2);
            if (headerRow2 == null) {
                headerRow2 = row;
                System.err.println("ROW:" + headerRow1);
                for (int j : values1Indices) {
                    System.err.println("idx:" + j);
                    row.add(headerRow1.get(j));
                }

                return row;
            }
            String key = "";
            for (int i : keys2Indices) {
                key += row.getString(i) + "_";
            }
            Row other = map.get(key);
            if (other == null) {
                //              System.err.println("no join:" + " key=" + key + " row:"+ row);
                for (int j : values1Indices) {
                    row.add("");
                }

                return null;
                //              return row;
            }
            for (int j : values1Indices) {
                row.add(other.get(j));
            }

            return row;
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


}
