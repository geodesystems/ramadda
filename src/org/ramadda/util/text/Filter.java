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
 *
 * @author Jeff McWhirter
 */

public class Filter extends Converter {

    /** _more_ */
    private String commentPrefix = "#";


    /** _more_ */
    protected int cnt = 0;

    /**
     * _more_
     */
    public Filter() {}

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
        if (rowOk(info, row)) {
            return row;
        } else {
            return null;
        }
    }

    /**
     * _more_
     *
     *
     * @param info _more_
     * @param row _more_
     *
     * @return _more_
     */
    public boolean rowOk(TextReader info, Row row) {
        return true;
    }

    /**
     * _more_
     *
     * @param info _more_
     * @param line _more_
     *
     * @return _more_
     */
    public boolean lineOk(TextReader info, String line) {
        if ((commentPrefix != null) && line.startsWith(commentPrefix)) {
            return false;
        }

        return true;
    }



    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Jan 9, '15
     * @author         Jeff McWhirter
     */
    public abstract static class ColumnFilter extends Filter {

        /** _more_ */
        private int col = -1;

        /** _more_ */
        private String scol;

        /** _more_ */
        private boolean negate = false;

        /**
         * _more_
         *
         * @param col _more_
         */
        public ColumnFilter(int col) {
            this.col = col;
        }


        /**
         * _more_
         *
         * @param scol _more_
         */
        public ColumnFilter(String scol) {
            this.scol = scol;
        }

        /**
         * _more_
         *
         * @param scol _more_
         * @param negate _more_
         */
        public ColumnFilter(String scol, boolean negate) {
            this(scol);
            this.negate = negate;
        }

        /**
         * _more_
         *
         * @param b _more_
         *
         * @return _more_
         */
        public boolean doNegate(boolean b) {
            if (negate) {
                return !b;
            }

            return b;
        }

        /**
         * _more_
         *
         * @param info _more_
         *
         * @return _more_
         */
        public int getIndex(TextReader info) {
            if (col >= 0) {
                return col;
            }
            if (scol == null) {
                return -11;
            }
            ArrayList<Integer> tmp = new ArrayList<Integer>();
            getColumnIndex(tmp, scol);
            col = tmp.get(0);

            return col;
        }


    }

    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Jan 9, '15
     * @author         Jeff McWhirter
     */
    public static class FilterGroup extends Filter {

        /** _more_ */
        List<Filter> filters = new ArrayList<Filter>();

        /** _more_ */
        private boolean andLogic = true;


        /**
         * _more_
         */
        public FilterGroup() {}

        /**
         * _more_
         *
         * @param andLogic _more_
         */
        public FilterGroup(boolean andLogic) {
            this.andLogic = andLogic;
        }

        /**
         * _more_
         *
         * @param filter _more_
         */
        public void addFilter(Filter filter) {
            filters.add(filter);
        }

        /**
         * _more_
         *
         *
         * @param info _more_
         * @param row _more_
         *
         * @return _more_
         */
        @Override
        public boolean rowOk(TextReader info, Row row) {
            if (filters.size() == 0) {
                return true;
            }
            for (Filter filter : filters) {
                if ( !filter.rowOk(info, row)) {
                    if (andLogic) {
                        return false;
                    }
                } else {
                    if ( !andLogic) {
                        return true;
                    }
                }
            }

            if ( !andLogic) {
                return false;
            }

            return true;
        }
    }




    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Jan 9, '15
     * @author         Jeff McWhirter
     */
    public static class PatternFilter extends ColumnFilter {

        /** _more_ */
        Pattern pattern;

        /** _more_ */
        boolean not = false;

        /**
         *
         * _more_
         *
         * @param col _more_
         * @param pattern _more_
         */
        public PatternFilter(int col, String pattern) {
            super(col);
            setPattern(pattern);
        }


        /**
         * _more_
         *
         * @param col _more_
         * @param pattern _more_
         * @param negate _more_
         */
        public PatternFilter(String col, String pattern, boolean negate) {
            super(col, negate);
            setPattern(pattern);
        }


        /**
         * _more_
         *
         * @param col _more_
         * @param pattern _more_
         */
        public PatternFilter(String col, String pattern) {
            super(col);
            setPattern(pattern);
        }

        /**
         * _more_
         *
         * @param pattern _more_
         */
        public void setPattern(String pattern) {
            if (pattern.startsWith("!")) {
                pattern = pattern.substring(1);
                not     = true;
            }
            this.pattern = Pattern.compile(pattern);

            this.pattern = Pattern.compile(pattern);
        }


        /**
         * _more_
         *
         *
         * @param info _more_
         * @param row _more_
         *
         * @return _more_
         */
        @Override
        public boolean rowOk(TextReader info, Row row) {
            if (cnt++ == 0) {
                return true;
            }
            int idx = getIndex(info);
            if (idx >= row.size()) {
                return doNegate(false);
            }
            if (idx < 0) {
                for (int i = 0; i < row.size(); i++) {
                    String v = row.getString(i);
                    if (pattern.matcher(v).find()) {
                        return doNegate(true);
                    }
                }

                return doNegate(false);
            }

            String v = row.getString(idx);
            //            System.out.println("v:" + v);
            if (pattern.matcher(v).find()) {
                //                System.out.println("OK");
                return doNegate(true);
            }

            return doNegate(false);
        }

    }

    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Jan 9, '15
     * @author         Jeff McWhirter
     */
    public static class Decimate extends Filter {

        /** _more_ */
        private int skip;


        /**
         * _more_
         * @param skip _more_
         */
        public Decimate(int skip) {
            this.skip = skip;
        }




        /**
         * _more_
         *
         *
         * @param info _more_
         * @param row _more_
         *
         * @return _more_
         */
        @Override
        public boolean rowOk(TextReader info, Row row) {
            if ((cnt++) % skip == 0) {
                return true;
            }

            return false;
        }
    }





    /**
     * Class description
     *
     *
     * @version        $version$, Mon, Jan 12, '15
     * @author         Enter your name here...
     */
    public static class ValueFilter extends ColumnFilter {

        /** _more_ */
        public static int OP_LT = 0;

        /** _more_ */
        public static int OP_LE = 1;

        /** _more_ */
        public static int OP_GT = 2;

        /** _more_ */
        public static int OP_GE = 3;

        /** _more_ */
        public static int OP_EQUALS = 4;

        /** _more_ */
        public static int OP_DEFINED = 5;

        /** _more_ */
        private int op;

        /** _more_ */
        private double value;


        /**
         * _more_
         *
         * @param col _more_
         * @param op _more_
         * @param value _more_
         */
        public ValueFilter(int col, int op, double value) {
            super(col);
            this.op    = op;
            this.value = value;
        }



        /**
         * _more_
         *
         * @param col _more_
         * @param op _more_
         * @param value _more_
         */
        public ValueFilter(String col, int op, double value) {
            super(col);
            this.op    = op;
            this.value = value;
        }



        /**
         * _more_
         *
         * @param s _more_
         *
         * @return _more_
         */
        public static int getOperator(String s) {
            s = s.trim();
            if (s.equals("<")) {
                return OP_LT;
            }
            if (s.equals("<=")) {
                return OP_LE;
            }
            if (s.equals(">")) {
                return OP_GT;
            }
            if (s.equals(">=")) {
                return OP_GE;
            }
            if (s.equals("=")) {
                return OP_EQUALS;
            }

            return -1;
        }


        /**
         * _more_
         *
         *
         * @param info _more_
         * @param row _more_
         *
         * @return _more_
         */
        @Override
        public boolean rowOk(TextReader info, Row row) {
            if (cnt++ == 0) {
                return true;
            }
            int idx = getIndex(info);
            if (idx >= row.size()) {
                return false;
            }
            try {
                String v     = row.getString(idx);
                double value = Double.parseDouble(v);
                if (op == OP_LT) {
                    return value < this.value;
                }
                if (op == OP_LE) {
                    return value <= this.value;
                }
                if (op == OP_GT) {
                    return value > this.value;
                }
                if (op == OP_GE) {
                    return value >= this.value;
                }
                if (op == OP_EQUALS) {
                    return value == this.value;
                }
                if (op == OP_DEFINED) {
                    return value == value;
                }

                return false;
            } catch (Exception exc) {}

            return false;

        }

    }

    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Jan 9, '15
     * @author         Jeff McWhirter
     */
    public static class Cutter extends Filter {

        /** _more_ */
        private boolean cut = true;

        /** _more_ */
        private List<Integer> rows;

        /** _more_ */
        private boolean cutOne = false;

        /**
         * _more_
         * @param toks _more_
         */
        public Cutter(List<String> toks) {
            this.rows = Utils.toInt(toks);
        }

        /**
         * _more_
         *
         * @param toks _more_
         * @param cut _more_
         */
        public Cutter(List<String> toks, boolean cut) {
            this(toks);
            this.cut = cut;
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
        public boolean rowOk(TextReader info, Row row) {
            boolean inRange = false;
            for (int rowIdx : rows) {
                if ((rowIdx == -1) && cutOne) {
                    inRange = true;

                    break;
                }
                if (rowIdx == rowCnt) {
                    inRange = true;

                    break;
                }
            }
            //            System.err.println ("row cnt:" + rowCnt + " in range:" + inRange +" " + cut +" " + rows + " " +(rowCnt<3?row.getValues():(Object)""));
            rowCnt++;
            boolean rowOk = true;
            if (cut) {
                rowOk = !inRange;
            } else {
                rowOk = inRange;
            }
            if ( !rowOk) {
                cutOne = true;
            }

            return rowOk;
        }
    }




    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Jan 9, '15
     * @author         Jeff McWhirter
     */
    public static class Unique extends Filter {


        /** _more_ */
        private List<Integer> cols;

        /** _more_ */
        private HashSet<String> seen = new HashSet<String>();

        /**
         * _more_
         *
         * @param toks _more_
         */
        public Unique(List<String> toks) {
            this.cols = Utils.toInt(toks);
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
        public boolean rowOk(TextReader info, Row row) {
            boolean       inRange = false;
            StringBuilder sb      = new StringBuilder();
            for (int i : cols) {
                Object value = row.getValues().get(i);
                sb.append(value);
                sb.append("--");
            }
            String s = sb.toString();
            if (seen.contains(s)) {
                return false;
            }
            seen.add(s);

            return true;
        }
    }




}
