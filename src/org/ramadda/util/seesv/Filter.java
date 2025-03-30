/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util.seesv;

import org.ramadda.util.IO;
import org.ramadda.util.Utils;
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

@SuppressWarnings("unchecked")
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
        protected boolean negate = false;

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
         *
         *
         * @param cols _more_
         * @param negate _more_
         */
        public ColumnFilter(List<String> cols, boolean negate) {
            super(cols);
            this.negate = negate;
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
         *
         *
         * @param negate _more_
         */
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
            getColumnIndex(ctx, tmp, scol, new HashSet());
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
         *
         * @param ctx _more_
         */
        public FilterGroup(TextReader ctx) {}

        /**
         * _more_
         *
         *
         * @param ctx _more_
         * @param andLogic _more_
         */
        public FilterGroup(TextReader ctx, boolean andLogic) {
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
    public static class IfIn extends Filter {

        /**  */
        private boolean in;

        /**  */
        private HashSet seen;

        /**  */
        private int idx1;

        /**  */
        private int idx2;

        /**  */
        private String column2;

        /**
         * _more_
         *
         *
         * @param ctx _more_
         * @param in _more_
         * @param column1 _more_
         * @param file _more_
         * @param column2 _more_
         */
        public IfIn(TextReader ctx, boolean in, String column1, String file,
                    String column2) {
            this.in = in;
            if ( !IO.okToReadFrom(file)) {
                fatal(ctx, "Cannot read file:" + file);
            }
            this.column2 = column2;
            try {
                init(file, column1);
            } catch (Exception exc) {
                fatal(ctx, "Reading file:" + file, exc);
            }
        }

        /**
         *
         * @param file _more_
         * @param col1 _more_
         *
         * @throws Exception _more_
         */
        private void init(String file, String col1) throws Exception {
            BufferedReader br = new BufferedReader(
                                    new InputStreamReader(
                                        getInputStream(file)));
            SeesvOperator operator = null;
            TextReader  reader   = new TextReader(br);
            seen = new HashSet();
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
                if (operator == null) {
                    operator = new SeesvOperator();
                    operator.setHeader(cols);
                    idx1 = operator.getColumnIndex(reader, col1);
                }
                String v = cols.get(idx1);
                seen.add(v);
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
                idx2 = getColumnIndex(ctx, column2);

                return true;
            }

            Object v = row.get(idx2);
            if (seen.contains(v)) {
                return in;
            } else {
                return !in;
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
    public static class Length extends Filter {

        /**  */
        private boolean greater;

        /**  */
        private int length;

        /**
         * _more_
         *
         * @param in _more_
         * @param column1 _more_
         * @param file _more_
         * @param column2 _more_
         *
         * @param ctx _more_
         * @param greater _more_
         * @param cols _more_
         * @param length _more_
         */
        public Length(TextReader ctx, boolean greater, List<String> cols,
                      int length) {
            super(cols);
            this.greater = greater;
            this.length  = length;
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
                if ((idx < 0) || (idx >= row.size())) {
                    continue;
                }
                String  s  = row.getString(idx);
                boolean ok = true;
                if (s.length() > length) {
                    if ( !greater) {
                        ok = false;
                    }
                } else {
                    if (greater) {
                        ok = false;
                    }
                }
                if ( !ok) {
                    return false;
                }
            }

            return true;
        }

    }

    public static class Has extends Filter {
	List<String> cols;

	boolean has = false;

        /**
         * _more_
         *
         * @param in _more_
         * @param column1 _more_
         * @param file _more_
         * @param column2 _more_
         *
         * @param ctx _more_
         * @param greater _more_
         * @param cols _more_
         * @param length _more_
         */
        public Has(TextReader ctx,  List<String> cols) {
	    this.cols  = cols;
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
		has  = true;
		for(String my: cols) {
		    HashSet<String> myCols = new HashSet<String>();
		    myCols.add(my);
		    myCols.add(makeID(my));
		    boolean ok = false;
		    for(Object c: row.getValues())  {
			String s = c.toString();
			if(myCols.contains(s)) {
			    ok = true;
			} else if(myCols.contains(makeID(s))) {
			    ok = true;
			}
		    }
		    if(!ok) {
			has = false;
			break;
		    }
		}
                return has;
            }
	    return has;
	}

    }

    public static class IfNumColumns extends Filter {
	int operator;
	int num;

        public IfNumColumns(TextReader ctx,  String op, int num) {
	    this.operator = getOperator(op);
	    this.num=num;
        }

        @Override
        public boolean rowOk(TextReader ctx, Row row) {
            if (cnt++ == 0) return true;
	    boolean ok  = true;
	    int cnt = row.size();
	    return checkOperator(operator, cnt,num);
	}

    }

    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Jan 9, '15
     * @author         Jeff McWhirter
     */
    public static class MatchesFile extends Filter {

        /**  */
        private boolean in;

        /**  */
        private List<String> strings;

        /**  */
        private List<Pattern> patterns;

        /**  */
        private int idx1;

        /**  */
        private int idx2;

        /**  */
        private String column2;

        /**
         * _more_
         *
         *
         * @param ctx _more_
         * @param in _more_
         * @param pattern _more_
         * @param column1 _more_
         * @param file _more_
         * @param column2 _more_
         */
        public MatchesFile(TextReader ctx, boolean in, String pattern,
                           String column1, String file, String column2) {
            this.in = in;
            if ( !IO.okToReadFrom(file)) {
                fatal(ctx, "Cannot read file:" + file);
            }
            this.column2 = column2;
            try {
                init(file, pattern, column1);
            } catch (Exception exc) {
                fatal(ctx, "Reading file:" + file, exc);
            }
        }

        /**
         *
         * @param file _more_
         * @param pattern _more_
         * @param col1 _more_
         *
         * @throws Exception _more_
         */
        private void init(String file, String pattern, String col1)
                throws Exception {
            BufferedReader br = new BufferedReader(
                                    new InputStreamReader(
                                        getInputStream(file)));
            SeesvOperator operator = null;
            TextReader  reader   = new TextReader(br);
            strings  = new ArrayList<String>();
            patterns = new ArrayList<Pattern>();
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
                if (operator == null) {
                    operator = new SeesvOperator();
                    operator.setHeader(cols);
                    idx1 = operator.getColumnIndex(reader, col1);
                }
                String v = cols.get(idx1);
                if (pattern.length() > 0) {
                    v = pattern.replaceAll("\\$\\{value\\}", v);
                }
                //              System.err.println(v);
                patterns.add(Pattern.compile(v));
                strings.add(v);
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
                idx2 = getColumnIndex(ctx, column2);

                return true;
            }

            String  v       = row.getString(idx2);
            boolean matches = false;
            for (Pattern p : patterns) {
                if (p.matcher(v).matches()) {
                    matches = true;

                    break;
                }
            }

            if (matches) {
                return in;
            } else {
                return !in;
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
    public static class PatternFilter extends ColumnFilter {

        /**  */
        List<String> strings;

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

	boolean isEquals = false;

        /**
         * _more_
         *
         *
         * @param ctx _more_
         * @param cols _more_
         * @param pattern _more_
         * @param negate _more_
         */
        public PatternFilter(TextReader ctx, List<String> cols,
                             String pattern, boolean negate) {
            super(cols, negate);
            setPattern(pattern);
            if ((cols.size() == 1) && cols.get(0).equals("-1")) {
                setIndex(-1);
            }

        }

        /**
         * _more_
         *
         *
         * @param ctx _more_
         * @param cols _more_
         * @param pattern _more_
         */
        public PatternFilter(TextReader ctx, List<String> cols,
                             String pattern) {
            super(cols);
            setPattern(pattern);
            if ((cols.size() == 1) && cols.get(0).equals("-1")) {
                setIndex(-1);
            }
        }

        /**
         *
         *
         *
         * @param ctx _more_
         * @param idx _more_
         * @param pattern _more_
         */
        public PatternFilter(TextReader ctx, int idx, String pattern) {
            super(idx);
            setPattern(pattern);
            setIndex(idx);
        }

        /**
         *
         * @param idx _more_
         */
        private void setIndex(int idx) {
            List<Integer> indices = new ArrayList<Integer>();
            indices.add(idx);
            setIndices(indices);
        }

        /**
         * _more_
         *
         * @param pattern _more_
         */
        public void setPattern(String pattern) {
            spattern = pattern;
            if (pattern.startsWith("=")) {
		isEquals=true;
		spattern = spattern.substring(1);
		return;
	    }

            if (pattern.startsWith("includes:")) {
                strings =
                    Utils.split(pattern.substring("includes:".length()), ",");

                return;
            }

            blank    = pattern.equals("");
            pattern  = Utils.convertPattern(pattern);
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
            boolean ok    = true;
            boolean debug = false;  //cnt<3;
            if (debug) {
                System.err.println("rowOk:" + row);
            }
            for (int idx : getIndices(ctx)) {
                if (debug) {
                    System.err.println("\tidx:" + idx);
                }
                if ( !ok) {
                    if (debug) {
                        System.err.println("\tbreak1:" + ok);
                    }
                    break;
                }
                if (idx >= row.size()) {
                    ok = doNegate(false);
                    if ( !ok) {
                        if (debug) {
                            System.err.println("\tbreak2:" + ok);
                        }

                        break;
                    }
                }
                Pattern pattern = this.pattern;
                if (isTemplate) {
                    String tmp = spattern;
                    for (int i = 0; i < row.size(); i++) {
                        tmp = tmp.replace("${" + i + "}",
                                          (String) row.get(i));
                    }
                    pattern = Pattern.compile(tmp);
                }
                if (idx < 0) {
                    ok = false;
                    for (int i = 0; i < row.size(); i++) {
                        String v = row.getString(i);
			if (strings != null) {
                            boolean any = false;
                            for (String s : strings) {
                                if (v.indexOf(s) >= 0) {
                                    any = true;

                                    break;
                                }
                            }
                            ok = doNegate(any);
                        } else if (blank) {
                            ok = doNegate(v.equals(""));
                        } else if (pattern.matcher(v).find()) {
                            ok = doNegate(true);
                        }
                        if (ok) {
                            break;
                        }
                    }

                    return ok;
                }
                if (idx >= row.size()) {
                    continue;
                }
                String v = row.getString(idx);
		if(isEquals) {
		    ok = doNegate(v.equalsIgnoreCase(spattern));
		    continue;
		}

                if (strings != null) {
                    boolean any = false;
                    for (String s : strings) {
                        if (v.indexOf(s) >= 0) {
                            any = true;

                            break;
                        }
                    }
                    ok = doNegate(any);
                    continue;
                }
                if (blank) {
                    ok = doNegate(v.equals(""));
                    if (debug) {
                        System.err.println("\tcontinue2:" + ok);
                    }
                    continue;
                }
                if (pattern.matcher(v).find()) {
                    if (debug) {
                        System.out.println("\tR3:" + doNegate(true) + " "
                                           + row);
                    }
                    ok = doNegate(true);
                } else {
                    ok = doNegate(false);
                }
            }
            if (debug) {
                System.err.println("\tfinal:" + ok);
            }

            return ok;

        }

    }

    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Nov 4, '21
     * @author         Enter your name here...
     */
    public static class FuzzyFilter extends ColumnFilter {

        /**  */
        int threshold;

        /** _more_ */
        String spattern;

        /** _more_ */
        boolean not = false;

        /** _more_ */
        boolean debug = false;

        /**
         * _more_
         *
         *
         * @param ctx _more_
         * @param threshold _more_
         * @param cols _more_
         * @param pattern _more_
         * @param negate _more_
         */
        public FuzzyFilter(TextReader ctx, int threshold, List<String> cols,
                           String pattern, boolean negate) {
            super(cols, negate);
            this.threshold = threshold;
            spattern       = pattern;
            if ((cols.size() == 1) && cols.get(0).equals("-1")) {
                setIndex(-1);
            }
        }

        /**
         *
         * @param s _more_
         *
         * @return _more_
         */
        private boolean matches(String s) {
            int score = me.xdrop.fuzzywuzzy.FuzzySearch.ratio(spattern, s);
            if (score > threshold) {
                return true;
            }

            return false;
        }

        /**
         *
         * @param idx _more_
         */
        private void setIndex(int idx) {
            List<Integer> indices = new ArrayList<Integer>();
            indices.add(idx);
            setIndices(indices);
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
            boolean ok    = true;
            boolean debug = false;  //cnt<3;
            if (debug) {
                System.err.println("rowOk");
            }
            for (int idx : getIndices(ctx)) {
                if (debug) {
                    System.err.println("\tidx:" + idx);
                }
                if ( !ok) {
                    if (debug) {
                        System.err.println("\tbreak1:" + ok);
                    }

                    break;
                }
                if (idx >= row.size()) {
                    ok = doNegate(false);
                    if ( !ok) {
                        if (debug) {
                            System.err.println("\tbreak2:" + ok);
                        }

                        break;
                    }
                }
                if (idx < 0) {
                    ok = false;
                    for (int i = 0; i < row.size(); i++) {
                        String v = row.getString(i);
                        if (matches(v)) {
                            ok = doNegate(true);
                        }
                        if (ok) {
                            break;
                        }
                    }

                    return ok;
                }
                String v = row.getString(idx);
                if (matches(v)) {
                    if (debug) {
                        System.out.println("\tR3:" + doNegate(true) + " "
                                           + row);
                    }
                    ok = doNegate(true);
                } else {
                    ok = doNegate(false);
                }
            }
            if (debug) {
                System.err.println("\tfinal:" + ok);
            }

            return ok;
        }

    }

    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Nov 4, '21
     * @author         Enter your name here...
     */
    public static class Same extends ColumnFilter {

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
         *
         *
         * @param ctx _more_
         * @param col1 _more_
         * @param col2 _more_
         * @param negate _more_
         */
        public Same(TextReader ctx, String col1, String col2,
                    boolean negate) {
            super(negate);
            scol1 = col1;
            scol2 = col2;
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
                col1 = getIndex(ctx, scol1);
                col2 = getIndex(ctx, scol2);

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
         *
         * @param ctx _more_
         * @param col _more_
         * @param count _more_
         */
        public CountValue(TextReader ctx, String col, int count) {
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
                count = Integer.valueOf(0);
                map.put(v, count);
            }
            if (count.intValue() >= this.count) {
                return false;
            }
            map.put(v, Integer.valueOf(count.intValue() + 1));

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
         *
         * @param ctx _more_
         * @param start _more_
         * @param skip _more_
         */
        public Decimate(TextReader ctx, int start, int skip) {
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
         *
         * @param ctx _more_
         * @param pattern _more_
         */
        public Stop(TextReader ctx, String pattern) {
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

	private boolean isRegexp = false;

        /**
         * _more_
         *
         *
         * @param ctx _more_
         * @param pattern _more_
         */
        public Start(TextReader ctx, String pattern) {
            this.pattern   = pattern;
            this.seenStart = false;
	    isRegexp  =  StringUtil.containsRegExp(pattern);
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
	    if(isRegexp) {
		System.err.println("ROW:" + row.toString());
		if (row.toString().matches(pattern)) {
		    System.err.println("MATCH");
		    seenStart = true;

		}
	    } else {
		if (row.toString().indexOf(pattern)>=0) {
		    seenStart = true;
		}
	    }
	    return seenStart;
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
         *
         * @param ctx _more_
         * @param cnt _more_
         */
        public MinColumns(TextReader ctx, int cnt) {
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
	    if(cnt<0) {
		cnt = row.size();
		return true;
	    }
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
         *
         * @param ctx _more_
         * @param cnt _more_
         */
        public MaxColumns(TextReader ctx, int cnt) {
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
	    if(cnt<0) {
		cnt = row.size();
		return true;
	    }
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
         *
         * @param ctx _more_
         * @param cols _more_
         * @param op _more_
         * @param value _more_
         */
        public ValueFilter(TextReader ctx, List<String> cols, int op,
                           double value) {
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
                    String v = row.getString(idx).trim();
                    if (v.length() == 0) {
                        return false;
                    }
                    double value = Double.parseDouble(v);
                    if (Double.isNaN(value)) {
                        return false;
                    }
		    if(!checkOperator(op, value,this.value)) return false;
                } catch (Exception exc) {
                    return false;
                }
            }

            return true;
        }
    }

    public static class EnsureNumeric extends Filter {

        /**
         * _more_
         *
         *
         * @param ctx _more_
         * @param cols _more_
         * @param op _more_
         * @param value _more_
         */
        public EnsureNumeric(TextReader ctx, List<String> cols) {
            super(cols);
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
                if (!row.indexOk(idx)) {
                    continue;
                }
		parse(row,row.getString(idx));
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
    public static class RangeFilter extends ColumnFilter {

        /**  */
        private boolean between;

        /** _more_ */
        private double min;

        /**  */
        private double max;

        /**
         * _more_
         *
         *
         *
         * @param ctx _more_
         * @param between _more_
         * @param cols _more_
         * @param min _more_
         * @param max _more_
         */
        public RangeFilter(TextReader ctx, boolean between,
                           List<String> cols, double min, double max) {
            super(cols);
            this.between = between;
            this.min     = min;
            this.max     = max;
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
            if (rowCnt++ == 0) {
                return true;
            }
            boolean ok    = true;
            for (int idx : getIndices(ctx)) {
                if (idx >= row.size()) {
                    continue;
                }
                try {
                    String v = row.getString(idx).trim();
                    if (v.length() == 0) {
                        return false;
                    }
                    double value = Double.parseDouble(v);
                    if (Double.isNaN(value)) {
                        return false;
                    }
                    boolean inRange = ((value >= min) && (value <= max));
                    if (inRange && !between) {
                        ok = false;
                    } else if ( !inRange && between) {
                        ok = false;
                    }
                    if ( !ok) {
                        break;
                    }
                } catch (Exception exc) {
                    return false;
                }
            }

            return ok;
        }
    }

    /**
     * Class description
     *
     *
     * @version        $version$, Mon, Jan 12, '15
     * @author         Enter your name here...
     */
    public static class BetweenString extends ColumnFilter {

	private boolean not;

        /**  */
        private boolean between;

        /** _more_ */
        private String start;

        /**  */
        private String end;
	private boolean inside = false;

        /**
         * _more_
         *
         *
         *
         * @param ctx _more_
         * @param between _more_
         * @param cols _more_
         * @param min _more_
         * @param max _more_
         */
        public BetweenString(TextReader ctx,  boolean not,
			     String col, String start, String end) {
            super(col);
	    this.not = not;
            this.start = start;
            this.end = end;
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
            if (rowCnt++ == 0) {
                return true;
            }

	    int idx = getIndex(ctx);
	    if (idx >= row.size()) {
		return false;
	    }
	    boolean nextInside = inside;

	    String v = row.getString(idx).trim();
	    if(!inside) {
		nextInside = v.equals(start);
		if(nextInside) inside = true;
	    } else {
		if(end.length()!=0) {
		    if(v.equals(end)) nextInside=false;
		}
	    }

	    boolean ok = not? !inside:inside;
	    inside = nextInside;
	    return ok;

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
         *
         * @param ctx _more_
         * @param rows _more_
         */
        public RowCutter(TextReader ctx, List<Integer> rows) {
            this.rows = rows;
        }

        /**
         * _more_
         *
         *
         * @param ctx _more_
         * @param rows _more_
         * @param cut _more_
         */
        public RowCutter(TextReader ctx, List<Integer> rows, boolean cut) {
            this(ctx, rows);
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
    	private static final int MODE_EXACT = 0;
	private static final int MODE_CLEAN = 1;
	private static final int MODE_FUZZY = 2;	

	private int mode;
	private int fuzzyThreshold=100;
	private boolean debug = false;
        /** _more_ */
        private HashSet<String> seen = new HashSet<String>();
        private List<String> past = new ArrayList<String>();

        /**
         * _more_
         *
         *
         * @param ctx _more_
         * @param toks _more_
         */
        public Unique(TextReader ctx, List<String> toks, String mode) {
            super(toks);
	    if(mode.equals("") || mode.equals("exact")) this.mode = MODE_EXACT;    
	    else if(mode.equals("clean")) this.mode = MODE_CLEAN;
	    else if(mode.startsWith("fuzzy")) {
		this.mode = MODE_FUZZY;
		if(mode.startsWith("fuzzy:")) {
		    mode = mode.substring("fuzzy:".length());
		    if(mode.equals("?")) debug = true;
		    else fuzzyThreshold = Integer.parseInt(mode);
		}
	    } else {
		throw new IllegalArgumentException("-unique: unknown mode:"  + mode + " valid values: exact fuzzy"); 
	    }

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
	    if(rowCnt++==0) return true;
            StringBuilder sb      = new StringBuilder();
            for (int i : getIndices(ctx)) {
                if ((i < 0) || (i >= row.size())) {
                    continue;
                }
                Object value = row.getString(i);
		if(sb.length()>0) sb.append("---");
                sb.append(value);
            }
            String s = sb.toString();
	    if(mode==MODE_CLEAN || mode==MODE_FUZZY) {
		s = s.toLowerCase().replaceAll("  +"," ");
	    }
	    if(mode==MODE_FUZZY) {
		if(debug) System.out.println("value:" + s);
		for(String p:past) {
		    int score = me.xdrop.fuzzywuzzy.FuzzySearch.ratio(s,p);
		    if(debug && !s.equals(p)) System.out.println("\t" + score+" " +p);
		    if(score>=fuzzyThreshold) {
			return false;
		    }
		}
		past.add(s);
		if(debug) return false;
		return true;
	    }
            if (seen.contains(s)) {
                return false;
            }
            seen.add(s);
            return true;
        }
    }

    public static class NonUnique extends Filter {
        private HashSet<String> seen = new HashSet<String>();
        private List<String> past = new ArrayList<String>();

        public NonUnique(TextReader ctx, List<String> toks) {
            super(toks);
        }

        @Override
        public boolean rowOk(TextReader ctx, Row row) {
	    if(rowCnt++==0) return true;
            StringBuilder sb      = new StringBuilder();
            for (int i : getIndices(ctx)) {
                if ((i < 0) || (i >= row.size())) {
                    continue;
                }
                Object value = row.getString(i);
		if(sb.length()>0) sb.append("---");
                sb.append(value);
            }
            String s = sb.toString();
            if (seen.contains(s)) {
                return true;
            }
            seen.add(s);
            return false;
        }
    }

    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Nov 4, '21
     * @author         Enter your name here...
     */
    public static class Sample extends Filter {

        /**  */
        private double prob;

        /**
         * _more_
         *
         *
         * @param ctx _more_
         * @param prob _more_
         */
        public Sample(TextReader ctx, double prob) {
            this.prob = prob;
        }

        /**
         * _more_
         *
         *
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public boolean rowOk(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                return true;
            }
            double r = Math.random();

            return r <= prob;
        }
    }

}
