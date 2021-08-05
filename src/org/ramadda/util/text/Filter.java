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

public class Filter extends Processor {


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
     * @param cols _more_
     */
    public Filter(List<String> cols) {
        super(cols);
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
        ctx.setCurrentOperator(this);
        if (rowOk(ctx, row)) {
            return row;
        } else {
            return null;
        }
    }




    /**
     * _more_
     *
     *
     * @param ctx _more_
     * @param row _more_
     *
     * @return _more_
     */
    public boolean rowOk(TextReader ctx, Row row) {
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
         * @param cols _more_
         */
        public ColumnFilter(List<String> cols) {
            super(cols);
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

        public ColumnFilter(boolean negate) {
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
         * @param ctx _more_
         *
         * @return _more_
         */
        public int getIndex(TextReader ctx) {
            if (col >= 0) {
                return col;
            }
            if (scol == null) {
                return -11;
            }
            ArrayList<Integer> tmp = new ArrayList<Integer>();
            getColumnIndex(ctx,tmp, scol,new HashSet());
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
         * @param ctx _more_
         * @param row _more_
         *
         * @return _more_
         */
        @Override
        public boolean rowOk(TextReader ctx, Row row) {
            if (filters.size() == 0) {
                return true;
            }
            for (Filter filter : filters) {
                if ( !filter.rowOk(ctx, row)) {
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
        String spattern;

        /** _more_ */
        Pattern pattern;

        /** _more_ */
        boolean not = false;

        /** _more_ */
        boolean isTemplate = false;

        /** _more_ */
        boolean blank;

        /** _more_ */
        boolean debug = false;

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

        public PatternFilter(int col, String pattern) {
            super(col);
            setPattern(pattern);
        }	

        /**
         * _more_
         *
         * @param pattern _more_
         */
        public void setPattern(String pattern) {
            blank    = pattern.equals("");
            pattern  = Utils.convertPattern(pattern);
            spattern = pattern;
            if (pattern.startsWith("!")) {
                pattern = pattern.substring(1);
                not     = true;
            }
            if (pattern.indexOf("${") >= 0) {
                isTemplate = true;
            } else {
                isTemplate   = false;
                this.pattern = Pattern.compile(pattern);
            }
        }


        /**
         * _more_
         *
         *
         * @param ctx _more_
         * @param row _more_
         *
         * @return _more_
         */
        @Override
        public boolean rowOk(TextReader ctx, Row row) {
            if (cnt++ == 0) {
                return true;
            }
            int idx = getIndex(ctx);
            if (idx >= row.size()) {
                return doNegate(false);
            }
            Pattern pattern = this.pattern;
            if (isTemplate) {
                String tmp = spattern;
                for (int i = 0; i < row.size(); i++) {
                    tmp = tmp.replace("${" + i + "}", (String) row.get(i));
                }
                pattern = Pattern.compile(tmp);
            }

            if (idx < 0) {
                for (int i = 0; i < row.size(); i++) {
                    String v = row.getString(i);
                    if (blank) {
                        return doNegate(v.equals(""));
                    }
                    if (pattern.matcher(v).find()) {
                        return doNegate(true);
                    }
                }

                return doNegate(false);
            }

            String v = row.getString(idx);
            if (blank) {
                return doNegate(v.equals(""));
            }
            if (pattern.matcher(v).find()) {
                if (debug) {
		    System.out.println("R3:" + doNegate(true) + " " + row);
                }
                return doNegate(true);
            }

            //            if(debug) System.err.println ("R4:" + doNegate(false) +" " +row);
            return doNegate(false);
        }

    }

    public static class Same extends ColumnFilter {

        /** _more_ */
        String scol1;
        String scol2;	
	int col1 = -1;
	int col2 = -1;

        /**
         * _more_
         */
        public Same(String col1, String col2, boolean negate) {
            super(negate);
	    scol1 =col1;
	    scol2 =col2;	    
        }

        /**
         * _more_
         *
         *
         * @param ctx _more_
         * @param row _more_
         *
         * @return _more_
         */
        @Override
        public boolean rowOk(TextReader ctx, Row row) {
            if (cnt++ == 0) {
		col1 = getIndex(ctx,scol1);
		col2 = getIndex(ctx,scol2);		
                return true;
            }
	    String s1 = row.getString(col1);
	    String s2 = row.getString(col2);	    
	    return doNegate(s1.equals(s2));
        }

    }
    

    /**
     * Class description
     *
     *
     * @version        $version$, Wed, Jan 23, '19
     * @author         Enter your name here...
     */
    public static class CountValue extends ColumnFilter {

        /** _more_ */
        int count;

        /** _more_ */
        Hashtable<Object, Integer> map = new Hashtable<Object, Integer>();

        /**
         * _more_
         *
         * @param col _more_
         * @param count _more_
         */
        public CountValue(String col, int count) {
            super(col);
            this.count = count;
        }


        /**
         * _more_
         *
         *
         * @param ctx _more_
         * @param row _more_
         *
         * @return _more_
         */
        @Override
        public boolean rowOk(TextReader ctx, Row row) {
            if (cnt++ == 0) {
                return true;
            }
            int     idx   = getIndex(ctx);
            String  v     = row.getString(idx);
            Integer count = map.get(v);
            if (count == null) {
                count = new Integer(0);
                map.put(v, count);
            }
            if (count.intValue() >= this.count) {
                return false;
            }
            map.put(v, new Integer(count.intValue() + 1));

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
    public static class Decimate extends Filter {

        /** _more_ */
        private int start;

        /** _more_ */
        private int skip;


        /**
         * _more_
         *
         * @param start _more_
         * @param skip _more_
         */
        public Decimate(int start, int skip) {
            this.start = start;
            this.skip  = skip;
        }



        /**
         * _more_
         *
         *
         * @param ctx _more_
         * @param row _more_
         *
         * @return _more_
         */
        @Override
        public boolean rowOk(TextReader ctx, Row row) {
            if (start > 0) {
                start--;

                return true;
            }
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
     * @version        $version$, Fri, Mar 22, '19
     * @author         Enter your name here...
     */
    public static class Stop extends Filter {

        /** _more_ */
        private String pattern;

        /** _more_ */
        private boolean seenStop;


        /**
         * _more_
         *
         * @param pattern _more_
         */
        public Stop(String pattern) {
            this.pattern  = pattern;
            this.seenStop = false;
        }



        /**
         * _more_
         *
         *
         * @param ctx _more_
         * @param row _more_
         *
         * @return _more_
         */
        @Override
        public boolean rowOk(TextReader ctx, Row row) {
            if (seenStop) {
                return false;
            }
            String line = row.toString();
            if (line.matches(pattern) || (line.indexOf(pattern) >= 0)) {
                seenStop = true;
                ctx.stopRunning();

                return false;
            }

            return true;
        }
    }


    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Mar 22, '19
     * @author         Enter your name here...
     */
    public static class Start extends Filter {

        /** _more_ */
        private String pattern;

        /** _more_ */
        private boolean seenStart;


        /**
         * _more_
         *
         * @param pattern _more_
         */
        public Start(String pattern) {
            this.pattern   = pattern;
            this.seenStart = false;
        }



        /**
         * _more_
         *
         *
         * @param ctx _more_
         * @param row _more_
         *
         * @return _more_
         */
        @Override
        public boolean rowOk(TextReader ctx, Row row) {
            if (seenStart) {
                return true;
            }
            if (row.toString().matches(pattern)) {
                seenStart = true;

                return false;
            }

            return false;
        }
    }



    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Mar 22, '19
     * @author         Enter your name here...
     */
    public static class MinColumns extends Filter {

        /** _more_ */
        private int cnt;


        /**
         * _more_
         *
         * @param cnt _more_
         */
        public MinColumns(int cnt) {
            this.cnt = cnt;
        }



        /**
         * _more_
         *
         *
         * @param ctx _more_
         * @param row _more_
         *
         * @return _more_
         */
        @Override
        public boolean rowOk(TextReader ctx, Row row) {
            if (row.size() < cnt) {
                return false;
            }

            return true;
        }
    }

    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Mar 22, '19
     * @author         Enter your name here...
     */
    public static class MaxColumns extends Filter {

        /** _more_ */
        private int cnt;


        /**
         * _more_
         * @param cnt _more_
         */
        public MaxColumns(int cnt) {
            this.cnt = cnt;
        }



        /**
         * _more_
         *
         *
         * @param ctx _more_
         * @param row _more_
         *
         * @return _more_
         */
        @Override
        public boolean rowOk(TextReader ctx, Row row) {
            if (row.size() > cnt) {
                return false;
            }

            return true;
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
        private int op;

        /** _more_ */
        private double value;




        /**
         * _more_
         *
         * @param cols _more_
         * @param op _more_
         * @param value _more_
         */
        public ValueFilter(List<String> cols, int op, double value) {
            super(cols);
            this.op    = op;
            this.value = value;
        }




        /**
         * _more_
         *
         *
         * @param ctx _more_
         * @param row _more_
         *
         * @return _more_
         */
        @Override
        public boolean rowOk(TextReader ctx, Row row) {
            if (cnt++ == 0) {
                return true;
            }
            for (int idx : getIndices(ctx)) {
                if (idx >= row.size()) {
                    continue;
                }
                try {
                    String v     = row.getString(idx).trim();
		    if(v.length()==0) return false;
                    double value = Double.parseDouble(v);
		    if(Double.isNaN(value)) return false;
                    if (op == OP_LT) {
                        if ( !(value < this.value)) {
                            return false;
                        }
                    } else if (op == OP_LE) {
                        if ( !(value <= this.value)) {
                            return false;
                        }
                    } else if (op == OP_GT) {
                        if ( !(value > this.value)) {
                            return false;
                        }
                    } else if (op == OP_GE) {
                        if ( !(value >= this.value)) {
                            return false;
                        }
                    } else if (op == OP_EQUALS) {
                        if (value != this.value) {
                            return false;
                        }
                    } else if (op == OP_NOTEQUALS) {
                        if (value == this.value) {
                            return false;
                        }
                    } else if (op == OP_DEFINED) {
                        if (Double.isNaN(value)) {
                            return false;
                        }
                    }
                } catch (Exception exc) {
		    return false;
		}		    
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
    public static class RowCutter extends Filter {

        /** _more_ */
        private boolean cut = true;

        /** _more_ */
        private List<Integer> rows;

        /** _more_ */
        private boolean cutOne = false;

        /**
         * _more_
         * @param rows _more_
         */
        public RowCutter(List<Integer> rows) {
            this.rows = rows;
        }

        /**
         * _more_
         *
         * @param rows _more_
         * @param cut _more_
         */
        public RowCutter(List<Integer> rows, boolean cut) {
            this(rows);
            this.cut = cut;
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
        public boolean rowOk(TextReader ctx, Row row) {
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
        private HashSet<String> seen = new HashSet<String>();

        /**
         * _more_
         *
         * @param toks _more_
         */
        public Unique(List<String> toks) {
	    super(toks);
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
        public boolean rowOk(TextReader ctx, Row row) {
            boolean       inRange = false;
            StringBuilder sb      = new StringBuilder();
            for (int i : getIndices(ctx)) {
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
