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
    private String commentPrefix = "#";
    protected int cnt = 0;

    public Filter() {}

    public Filter(List<String> cols) {
        super(cols);
    }

    @Override
    public Row processRow(TextReader ctx, Row row) throws Exception {
        ctx.setCurrentOperator(this);
        if (rowOk(ctx, row)) {
            return row;
        } else {
            return null;
        }
    }

    public boolean rowOk(TextReader ctx, Row row) {
        return true;
    }

    public abstract static class ColumnFilter extends Filter {
        private int col = -1;
        private String scol;
        protected boolean negate = false;

        public ColumnFilter(int col) {
            this.col = col;
        }

        public ColumnFilter(String scol) {
            this.scol = scol;
        }

        public ColumnFilter(List<String> cols) {
            super(cols);
        }

        public ColumnFilter(List<String> cols, boolean negate) {
            super(cols);
            this.negate = negate;
        }

        public ColumnFilter(String scol, boolean negate) {
            this(scol);
            this.negate = negate;
        }

        public ColumnFilter(boolean negate) {
            this.negate = negate;
        }

        public boolean doNegate(boolean b) {
            if (negate) {
                return !b;
            }

            return b;
        }

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

    public static class FilterGroup extends Filter {
        List<Filter> filters = new ArrayList<Filter>();
        private boolean andLogic = true;

        public FilterGroup(TextReader ctx) {}

        public FilterGroup(TextReader ctx, boolean andLogic) {
            this.andLogic = andLogic;
        }

        public void addFilter(Filter filter) {
            filters.add(filter);
        }

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

    public static class IfIn extends Filter {
	private boolean in;
        private HashSet seen;
        private int idx1;
        private int idx2;
        private String column2;

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

    public static class Length extends Filter {
        private boolean greater;
        private int length;

        public Length(TextReader ctx, boolean greater, List<String> cols,
                      int length) {
            super(cols);
            this.greater = greater;
            this.length  = length;
        }

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

    public static class IsNumber extends Filter {

        public IsNumber(TextReader ctx, List<String> cols) {
            super(cols);
        }

        @Override
        public boolean rowOk(TextReader ctx, Row row) {
            if (cnt++ == 0) {
                return true;
            }

            for (int idx : getIndices(ctx)) {
		if(!row.indexOk(idx)) continue;
		String  s  = row.getString(idx).trim();
                if (s.length() ==0) return false;
		try {
		    Double.parseDouble(s);
		} catch(Exception exc) {
                    return false;
                }
	    }
            return true;
        }

    }

    public static class IsNotNumber extends Filter {

        public IsNotNumber(TextReader ctx, List<String> cols) {
            super(cols);
        }

        @Override
        public boolean rowOk(TextReader ctx, Row row) {
            if (cnt++ == 0) {
                return true;
            }

            for (int idx : getIndices(ctx)) {
		if(!row.indexOk(idx)) continue;
		String  s  = row.getString(idx).trim();
                if (s.length() ==0) return true;
		try {
		    Double.parseDouble(s);
		} catch(Exception exc) {
                    return true;
                }
	    }
            return false;
        }

    }

    public static class IsNan extends Filter {

        public IsNan(TextReader ctx, List<String> cols) {
            super(cols);
        }

        @Override
        public boolean rowOk(TextReader ctx, Row row) {
            if (cnt++ == 0) {
                return true;
            }

            for (int idx : getIndices(ctx)) {
		if(!row.indexOk(idx)) continue;
		String  s  = row.getString(idx).trim();
		if(!Double.isNaN(Double.parseDouble(s))) return false;
	    }
            return true;
        }

    }

    public static class IsNotNan extends Filter {

        public IsNotNan(TextReader ctx, List<String> cols) {
            super(cols);
        }

        @Override
        public boolean rowOk(TextReader ctx, Row row) {
            if (cnt++ == 0) {
                return true;
            }

            for (int idx : getIndices(ctx)) {
		if(!row.indexOk(idx)) continue;
		String  s  = row.getString(idx).trim();
		if(Double.isNaN(Double.parseDouble(s))) return false;
	    }
            return true;
        }

    }

    public static class Has extends Filter {
	List<String> cols;

	boolean has = false;

        public Has(TextReader ctx,  List<String> cols) {
	    this.cols  = cols;
        }

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

    public static class MatchesFile extends Filter {
        private boolean in;
        private List<String> strings;
        private List<Pattern> patterns;
        private int idx1;
        private int idx2;
        private String column2;

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

    public static class PatternFilter extends ColumnFilter {
        List<String> strings;
        String spattern;
        Pattern pattern;
        boolean not = false;
        boolean isTemplate = false;
        boolean blank;
        boolean debug = false;
	boolean isEquals = false;

        public PatternFilter(TextReader ctx, List<String> cols,
                             String pattern, boolean negate) {
            super(cols, negate);
            setPattern(pattern);
            if ((cols.size() == 1) && cols.get(0).equals("-1")) {
                setIndex(-1);
            }

        }

        public PatternFilter(TextReader ctx, List<String> cols,
                             String pattern) {
            super(cols);
            setPattern(pattern);
            if ((cols.size() == 1) && cols.get(0).equals("-1")) {
                setIndex(-1);
            }
        }

        public PatternFilter(TextReader ctx, int idx, String pattern) {
            super(idx);
            setPattern(pattern);
            setIndex(idx);
        }

        private void setIndex(int idx) {
            List<Integer> indices = new ArrayList<Integer>();
            indices.add(idx);
            setIndices(indices);
        }

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

    public static class FuzzyFilter extends ColumnFilter {
        int threshold;
        String spattern;
        boolean not = false;
        boolean debug = false;

        public FuzzyFilter(TextReader ctx, int threshold, List<String> cols,
                           String pattern, boolean negate) {
            super(cols, negate);
            this.threshold = threshold;
            spattern       = pattern;
            if ((cols.size() == 1) && cols.get(0).equals("-1")) {
                setIndex(-1);
            }
        }

        private boolean matches(String s) {
            int score = me.xdrop.fuzzywuzzy.FuzzySearch.ratio(spattern, s);
            if (score > threshold) {
                return true;
            }

            return false;
        }

        private void setIndex(int idx) {
            List<Integer> indices = new ArrayList<Integer>();
            indices.add(idx);
            setIndices(indices);
        }

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

    public static class Same extends ColumnFilter {
        String scol1;
        String scol2;
        int col1 = -1;
        int col2 = -1;

        public Same(TextReader ctx, String col1, String col2,
                    boolean negate) {
            super(negate);
            scol1 = col1;
            scol2 = col2;
        }

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

    public static class RowRange extends Filter {
	int  start;
	int  end;	

        public RowRange(TextReader ctx, int start, int end) {
            this.start=start;
	    this.end=end;
        }

        @Override
        public boolean rowOk(TextReader ctx, Row row) {
	    int index = rowCnt++;
	    return index>=start && index<=end;
        }

    }

    public static class CountValue extends ColumnFilter {

        int count;

        Hashtable<Object, Integer> map = new Hashtable<Object, Integer>();

        public CountValue(TextReader ctx, String col, int count) {
            super(col);
            this.count = count;
        }

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

    public static class Decimate extends Filter {
        private int start;
        private int skip;

        public Decimate(TextReader ctx, int start, int skip) {
            this.start = start;
            this.skip  = skip;
        }

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

    public static class Stop extends Filter {
        private String pattern;
        private boolean seenStop;

        public Stop(TextReader ctx, String pattern) {
            this.pattern  = pattern;
            this.seenStop = false;
        }

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

    public static class Start extends Filter {
        private String pattern;
        private boolean seenStart;
	private boolean isRegexp = false;

        public Start(TextReader ctx, String pattern) {
            this.pattern   = pattern;
            this.seenStart = false;
	    isRegexp  =  StringUtil.containsRegExp(pattern);
        }

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

    public static class MinColumns extends Filter {
        private int cnt;

        public MinColumns(TextReader ctx, int cnt) {
            this.cnt = cnt;
        }

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

    public static class MaxColumns extends Filter {
        private int cnt;

        public MaxColumns(TextReader ctx, int cnt) {
            this.cnt = cnt;
        }

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

    public static class ValueFilter extends ColumnFilter {
        private int op;
        private double value;

        public ValueFilter(TextReader ctx, List<String> cols, int op,
                           double value) {
            super(cols);
            this.op    = op;
            this.value = value;
        }

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

        public EnsureNumeric(TextReader ctx, List<String> cols) {
            super(cols);
        }

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

    public static class RangeFilter extends ColumnFilter {
        private boolean between;
        private double min;
        private double max;

        public RangeFilter(TextReader ctx, boolean between,
                           List<String> cols, double min, double max) {
            super(cols);
            this.between = between;
            this.min     = min;
            this.max     = max;
        }

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

    public static class BetweenString extends ColumnFilter {
	private boolean not;
        private boolean between;
        private String start;
        private String end;
	private boolean inside = false;

        public BetweenString(TextReader ctx,  boolean not,
			     String col, String start, String end) {
            super(col);
	    this.not = not;
            this.start = start;
            this.end = end;
        }

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

    public static class RowCutter extends Filter {
        private boolean cut = true;
        private List<Integer> rows;
        private boolean cutOne = false;

        public RowCutter(TextReader ctx, List<Integer> rows) {
            this.rows = rows;
        }

        public RowCutter(TextReader ctx, List<Integer> rows, boolean cut) {
            this(ctx, rows);
            this.cut = cut;
        }

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

    public static class Unique extends Filter {
    	private static final int MODE_EXACT = 0;
	private static final int MODE_CLEAN = 1;
	private static final int MODE_FUZZY = 2;	

	private int mode;
	private int fuzzyThreshold=100;
	private boolean debug = false;

        private HashSet<String> seen = new HashSet<String>();
        private List<String> past = new ArrayList<String>();

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

    public static class Sample extends Filter {
        private double prob;

        public Sample(TextReader ctx, double prob) {
            this.prob = prob;
        }

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
