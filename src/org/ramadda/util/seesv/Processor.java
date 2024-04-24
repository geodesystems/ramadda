/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util.seesv;


import org.apache.commons.codec.language.Soundex;
import org.apache.commons.lang3.text.StrTokenizer;
import org.apache.commons.text.StringTokenizer;
import org.apache.commons.text.similarity.JaroWinklerDistance;
import org.apache.commons.text.similarity.LevenshteinDistance;

import org.ramadda.util.HtmlUtils;



import org.ramadda.util.IO;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.MapProvider;
import org.ramadda.util.Utils;

import org.json.*;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.net.URL;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.function.BiConsumer;

import java.util.function.Consumer;
import java.util.regex.*;


/**
 *
 * @author Jeff McWhirter
 */

@SuppressWarnings("unchecked")
public abstract class Processor extends SeesvOperator {

    //These are copies from /org.ramadda.data.record.RecordField;

    /** _more_ */
    public static final String TYPE_STRING = "string";

    /** _more_ */
    public static final String TYPE_URL = "url";

    /** _more_ */
    public static final String TYPE_ENUMERATION = "enumeration";

    /** _more_ */
    public static final String TYPE_IMAGE = "image";

    /** _more_ */
    public static final String TYPE_DATE = "date";


    /** _more_ */
    public static final String TYPE_DOUBLE = "double";

    /** _more_ */
    public static final String TYPE_INT = "int";



    /** the processor chain */
    private Processor nextProcessor;


    /**
     * _more_
     */
    public Processor() {}






    /**
     
     *
     * @param seesv _more_
     */
    public Processor(Seesv seesv) {
        super(seesv);
    }


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
     *  Set the NextProcessor property.
     *
     *  @param value The new value for NextProcessor
     */
    public void setNextProcessor(Processor value) {
        nextProcessor = value;
    }

    /**
     *  Get the NextProcessor property.
     *
     *  @return The NextProcessor
     */
    public Processor getNextProcessor() {
        return nextProcessor;
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

        List<String>  toks   = Utils.split(name, ":unit:", true, true);
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

        String type = TYPE_DOUBLE;
        if (id.indexOf("year") >= 0) {
            type = TYPE_DATE;
            extra.append(" format=\"yyyy\" ");
        }

        boolean hasName = id.indexOf("name") >= 0;
        if (hasName) {
            type = TYPE_STRING;
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
                            sampledType = TYPE_INT;
                        }
                    } else if (exampleString.matches("^[\\d\\.]+$")) {
                        sampledType = TYPE_DOUBLE;
                    } else if (exampleString.length() == 0) {}
                    else {
                        sampledType = TYPE_STRING;

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
            type = TYPE_DOUBLE;
        } else if (id.indexOf("longitude") >= 0) {
            extra.append(" isLongitude=\"true\" ");
            type = TYPE_DOUBLE;
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

    /**  */
    private Row extraRow;

    /**  */
    private boolean consumeColumns = false;

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
        setHeaderIfNeeded(row);
	row = processRow(ctx, row);
        if ((row != null) && (nextProcessor != null)) {
            row = nextProcessor.handleRow(ctx, row);
        }

        return row;
    }


    /**
     *
     * @param b _more_
     */
    public void setConsumeColumns(boolean b) {
        consumeColumns = b;
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
    public Row processRow(TextReader ctx, Row row) throws Exception {
        return row;
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
    public List<Row> processRowReturnList(TextReader ctx, Row row)
            throws Exception {
        List<Row> l = new ArrayList<Row>();
        Row       r = processRow(ctx, row);
        if (r != null) {
            l.add(r);
        }

        return l;
    }

    /**
     *
     * @param ctx _more_
     *
     * @throws Exception _more_
     */
    public void finish(TextReader ctx) throws Exception {
        if (nextProcessor != null) {
            nextProcessor.finish(ctx);
        }
    }

    public void finishRows(TextReader ctx,List<Row> rows) throws Exception {
	//If this command collects rows then this is how we finish them
        Processor nextProcessor = getNextProcessor();
        if (nextProcessor != null) {
            for (Row row : rows) {
                row = nextProcessor.handleRow(ctx, row);
            }
            nextProcessor.finish(ctx);
        }
    }




    /**
     * _more_
     */
    public  void reset(boolean force) {
	if(force) {
	    rowCnt=0;
	}
        if (nextProcessor != null) {
            nextProcessor.reset(force);
        }
    }





    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Jan 16, '15
     * @author         Enter your name here...
     */
    public static class Expand extends Processor {

        /**  */
        private Seesv seesv;

        /**  */
        private List<String> args;

        /**  */
        private TextReader applyCtx;


        /**
         *
         * @param seesv _more_
         * @param ctx _more_
         * @param cols _more_
         * @param args _more_
         */
        public Expand(Seesv seesv, TextReader ctx, List<String> cols,
                      List<String> args) {
            super(cols);
            this.seesv = seesv;
            this.args    = args;
        }

        /**
         *
         * @param ctx _more_
         * @param row _more_
         */
        private void makeCommands(TextReader ctx, Row row) {
            boolean debug = false;
            //      debug =true;
            if (debug) {
                System.err.println("Make commands");
            }
            List<String>  cols    = new ArrayList<String>();
            List<Integer> indices = getIndices(ctx);
            for (Integer i : indices) {
		if(!row.indexOk(i)) continue;
                String column = row.getString(i);
                cols.add(column);
                if (debug) {
                    System.err.println("\tcolumn:" + column);
                }

            }
            applyCtx = new TextReader();
            for (String col : cols) {
                List<String> cvrtedArgs = new ArrayList<String>();
                for (String arg : args) {
		    col = col.replace("-","_");
                    String id = Utils.makeID(col, false);
                    arg = arg.replace("${column}", id);
                    arg = arg.replace("${column_name}", col);
                    cvrtedArgs.add(arg);
                }
		//		System.err.println("\tcvrted args:" + cvrtedArgs);
                for (int j = 0; j < cvrtedArgs.size(); j++) {
                    String                    arg  = cvrtedArgs.get(j);
                    Seesv.CsvFunctionHolder func = seesv.getFunction(arg);
                    if (func == null) {
                        throw new RuntimeException(
                            "Unknown function in -apply:" + cvrtedArgs);
                    }
                    int idx = 0;
                    try {
                        idx = func.run(applyCtx, cvrtedArgs, j);
                    } catch (Exception exc) {
                        throw new RuntimeException(exc);
                    }
                    if (idx == Seesv.SKIP_INDEX) {
                        continue;
                    }
                    if (idx < 0) {
                        throw new RuntimeException(
                            "Unknown function in -apply:" + args);
                    }
                    j = idx;
                }
            }

        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                makeCommands(ctx, row);
            }
            try {
                row = applyCtx.processRow(seesv, row);

                return row;
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }
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
         * @param ctx _more_
         * @param row _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) throws Exception {
            if (flag == FLAG_POSITION) {
                ctx.setPositionStart(value);
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
    public static class UniqueHeader extends Processor {

        /**
         * _more_
         *
         * @param flag _more_
         * @param value _more_
         */
        public UniqueHeader(TextReader ctx) {
        }


        @Override
        public Row processRow(TextReader ctx, Row row) throws Exception {
	    if(rowCnt++==0) {
		checkUniqueRow(row);
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
    public static class MathStats extends Processor {

	List<Stat> stats = new ArrayList<Stat>();

	Row header;

        /**
         * _more_
         *
         * @param flag _more_
         * @param value _more_
         */
        public MathStats() {
        }


	public static class Stat {
	    double min= Double.NaN;
	    double max =Double.NaN;	    
	    
	    public void check(double v) {
		if(Double.isNaN(min)) min = v;
		else min = Math.min(min, v);
		if(Double.isNaN(max)) max = v;
		else max = Math.max(max, v);
	    }

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
	    if(header == null) {
		header = row;
		for(int i=0;i<header.size();i++) {
		    stats.add(new Stat());
		}
		return row;
	    }
	    for(int i=0;i<row.size();i++) {
		try {
		    stats.get(i).check(row.getDouble(i));
		} catch(Exception exc) {}

	    }
            return row;
        }


	public void finish(TextReader ctx) throws Exception {
	    for(int i=0;i<header.size();i++) {
		Stat stat = stats.get(i);
		if(!Double.isNaN(stat.min)) {
		    System.out.println(header.get(i)+" min:" + Utils.formatComma(stat.min) +" max:"  + Utils.formatComma(stat.max));
		}
	    }
	}


    }

    public static class JsonValue extends Processor {

	String path;

        /**
         * _more_
         *
         * @param flag _more_
         * @param value _more_
         */
	
        public JsonValue(List<String> cols, String path) {
            super(cols);
	    this.path = path;
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
	    if(rowCnt++==0) return row;
            List<Integer> indices = getIndices(ctx);
	    for(int idx:indices) {
		String v = row.getString(idx);
		try {
		    JSONObject obj  = new JSONObject(v);
		    Object o = JsonUtil.readValue(obj, path,v);
		    row.set(idx,o.toString());
		} catch(Exception exc) {
		    System.err.println("Errow:" + exc +" value:" + v);
		}
	    }
            return row;
        }

    }

    


    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Nov 4, '21
     * @author         Enter your name here...
     */
    public static class Dissector extends Processor {

        /**  */
        Pattern pattern;

        /**  */
        List<String> patternNames;

        /**  */
        int index;

        /**
         *
         *
         * @param col _more_
         * @param pattern _more_
         */
        public Dissector(String col, String pattern) {
            super(col);
	    pattern = pattern.replace("_quote_","\"");
            patternNames = new ArrayList<String>();
            pattern      = Utils.extractPatternNames(pattern, patternNames);
            this.pattern = Pattern.compile(pattern);
        }


        /**
         * @param ctx _more_
         * @param row _more_
         *
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                index = getIndex(ctx);
                for (String name : patternNames) {
                    row.add(name);
                }

                return row;
            }
	    if(!row.indexOk(index)) {
		return row;
	    }
            String  v       = row.getString(index);
            Matcher matcher = pattern.matcher(v);
            if ( !matcher.find()) {
                for (String name : patternNames) {
                    row.add("");
                }
            } else {
                for (int i = 0; i < patternNames.size(); i++) {
                    Object value = matcher.group(i + 1);
                    if (value == null) {
                        value = "";
                    }
                    row.add(value);
                }
            }

            return row;
        }
    }


    public static class Scraper extends Processor {

        /**  */
        Pattern pattern;

        /**  */
        List<String> names;

        /**  */
        int index;

        /**
         *
         *
         * @param col _more_
         * @param pattern _more_
         */
        public Scraper(String col, String names,String pattern) {
            super(col);
	    pattern = pattern.replace("_quote_","\"");
            this.names = Utils.split(names);
            this.pattern = Pattern.compile(pattern);
        }


        /**
         * @param ctx _more_
         * @param row _more_
         *
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                index = getIndex(ctx);
                for (String name : names) {
                    row.add(name);
                }
                return row;
            }
	    if(!row.indexOk(index)) {
		return row;
	    }
            String  url       = row.getString(index);
	    try {
		String html = IO.readUrl(new URL(url));
	    } catch(Exception exc) {
		System.err.println("Error reading url:" + url);
		for(String name: names)
		    row.add("");
	    }


            return row;
        }
    }



    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Nov 4, '21
     * @author         Enter your name here...
     */
    public static class Downloader extends Processor {

        /**  */
        private Seesv seesv;

        /**  */
        private String suffix;

        /**  */
        private int index;

        /**
         *
         *
         *
         * @param ctx _more_
         * @param seesv _more_
         * @param col _more_
         * @param suffix _more_
         */
        public Downloader(TextReader ctx, Seesv seesv, String col,
                          String suffix) {
            super(col);
            this.seesv = seesv;
            this.suffix  = suffix;
        }


        /**
         * @param ctx _more_
         * @param row _more_
         *
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                index = getIndex(ctx);
                row.add("File");
                return row;
            }
            String value = row.getString(index);
	    String tail;
            if (suffix.length() == 0) {
                tail = IOUtil.getFileTail(value);
            } else {
		tail = suffix;
	    }
            File tmpFile = seesv.getTmpFile(tail);
            if (tmpFile == null) {
                row.add("");
                return row;
            }
            if (value.length() == 0) {
                row.add("");
                return row;
            }
            try {
                URL              url   = new URL(value);
                InputStream      input = IO.getInputStream(url);
                FileOutputStream fos   = new FileOutputStream(tmpFile);
                IOUtil.writeTo(input, fos);
                IOUtil.close(input);
                IOUtil.close(fos);
                row.add(tmpFile.getName());
            } catch (Exception exc) {
                System.err.println("Error downloading URL:" + value
                                   + " error:" + exc);
                row.add("");
            }

            return row;

        }
    }


    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Nov 4, '21
     * @author         Enter your name here...
     */
    public static class KeyValue extends Processor {

        /**  */
        private List<String> keys;

        /**  */
        private int index = -1;

        /**  */
        private String extraId;

        /**
         *
         *
         * @param col _more_
         */
        public KeyValue(String col) {
            super(col);
        }


        /**
         * @param ctx _more
         * @param row _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) throws Exception {
            if (Misc.equals(row.getId(), extraId)) {
                return row;
            }
            rowCnt++;
            if (index == -1) {
                index = getIndex(ctx);
            }
            if (rowCnt == 1) {
                return null;
            }
            String                    v      = row.getString(index);
            Hashtable<String, String> props  = Utils.parseKeyValue(v);
            Row                       header = null;
            if (keys == null) {
                keys = new ArrayList<String>();
                for (Enumeration k = props.keys(); k.hasMoreElements(); ) {
                    String key = (String) k.nextElement();
                    keys.add(key);
                }
                header = new Row();
                Collections.sort(keys);
                for (String key : keys) {
                    header.add(key);
                }
            }

            Row newRow = new Row();
            for (String key : keys) {
                String value = props.get(key);
                if (value == null) {
                    value = "";
                }
                newRow.add(value);
            }
            if (header != null) {
                extraId = "" + newRow.getId();
                ctx.setExtraRow(newRow);
                Row tmp = header;
                header = null;

                return tmp;
            }

            return newRow;
        }
    }

    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Nov 4, '21
     * @author         Enter your name here...
     */
    public static class First extends Processor {

        /**  */
        private int index = -1;

        /**  */
        private String name;

        /**  */
        private int number;

        /**
         *
         *
         * @param col _more_
         * @param name _more_
         * @param num _more_
         */
        public First(String col, String name, String num) {
            super(col);
            this.name = name;
            number    = Integer.parseInt(num);
        }


        /**
         * @param ctx _more
         * @param row _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) throws Exception {
            if (rowCnt++ == 0) {
                row.add(name);

                return row;
            }
            if (index == -1) {
                index = getIndex(ctx);
            }
            String v = row.getString(index);
            if (v.length() < number) {
                row.add(v);
            } else {
                row.add(v.substring(0, number));
            }

            return row;
        }
    }

    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Nov 4, '21
     * @author         Enter your name here...
     */
    public static class Last extends Processor {

        /**  */
        private int index = -1;

        /**  */
        private String name;

        /**  */
        private int number;

        /**
         *
         *
         * @param col _more_
         * @param name _more_
         * @param num _more_
         */
        public Last(String col, String name, String num) {
            super(col);
            this.name = name;
            number    = Integer.parseInt(num);
        }


        /**
         * @param ctx _more
         * @param row _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) throws Exception {
            if (rowCnt++ == 0) {
                row.add(name);

                return row;
            }
            if (index == -1) {
                index = getIndex(ctx);
            }
            String v   = row.getString(index);
            int    idx = v.length() - number;
            if (idx < 0) {
                row.add(v);
            } else {
                row.add(v.substring(idx));
            }

            return row;
        }
    }


    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Nov 4, '21
     * @author         Enter your name here...
     */
    public static class Between extends Processor {

        /**  */
        private int index = -1;

        /**  */
        private String name;

        /**  */
        private int start;

        /**  */
        private int end;

        /**
         *
         *
         * @param col _more_
         * @param name _more_
         * @param start _more_
         * @param end _more_
         */
        public Between(String col, String name, String start, String end) {
            super(col);
            this.name  = name;
            this.start = Integer.parseInt(start);
            this.end   = Integer.parseInt(end);
        }


        /**
         * @param ctx _more
         * @param row _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) throws Exception {
            if (rowCnt++ == 0) {
                row.add(name);

                return row;
            }
            if (index == -1) {
                index = getIndex(ctx);
            }
            String v = row.getString(index);
            if (start >= v.length()) {
                row.add("");
            } else {
                if (end >= v.length()) {
                    row.add(v.substring(start));
                } else {
                    row.add(v.substring(start, end + 1));
                }
            }

            return row;
        }
    }



    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Nov 4, '21
     * @author         Enter your name here...
     */
    public static class FromHeading extends Processor {

        /**  */
        private List<String> names;

        /**  */
        private List<String> values;

        /**  */
        Pattern pattern;



        /**
         *
         *
         * @param col _more_
         * @param name _more_
         * @param start _more_
         * @param end _more_
         *
         * @param cols _more_
         * @param names _more_
         * @param pattern _more_
         */
        public FromHeading(List<String> cols, String names, String pattern) {
            super(cols);
            this.names   = Utils.split(names, ",");
            this.pattern = Pattern.compile(pattern);
            values       = new ArrayList<String>();
            for (String s : this.names) {
                values.add("");
            }
        }


        /**
         * @param ctx _more
         * @param row _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) throws Exception {
            String        corpus;
            List<Integer> indices = getIndices(ctx);
            if (indices.size() == 1) {
                corpus = row.getString(indices.get(0));
            } else {
                StringBuilder sb = new StringBuilder();
                for (int i : indices) {
                    if ((i >= 0) && (i < row.size())) {
                        sb.append(row.getString(i));
                        sb.append(" ");
                    }
                }
                corpus = sb.toString().trim();
            }
            Matcher matcher = pattern.matcher(corpus);
            if ( !matcher.find()) {
                if (rowCnt++ == 0) {
                    for (String name : names) {
                        row.add(name);
                    }

                    return row;
                }
                for (String v : values) {
                    row.add(v);
                }

                return row;
            } else {
                //              System.out.println ("MATCH:" + corpus);
                values = new ArrayList<String>();
                for (int i = 0; i < matcher.groupCount(); i++) {
                    String v = matcher.group(i + 1);
                    values.add(v);
                }

                return null;
            }
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
         *
         * @param ctx _more_
         */
        public Pass(TextReader ctx) {}


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
            System.err.println("#" + (rowCnt++) + " row:" + row);

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
    public static class Progress extends Processor {

        /**  */
        int every;

	String prefix;
	
        /**  */
        int printCnt = 0;

	long t1;
	
        /**
         * _more_
         *
         * @param every _more_
         */
        public Progress(String prefix,int every) {
	    this.prefix = prefix;
            this.every = every;
        }


        /**
         *
         * @param ctx _more_
         *
         * @throws Exception _more_
         */
        @Override
        public void finish(TextReader ctx) throws Exception {
            super.finish(ctx);
            if (printCnt > 0) {
                System.err.print("\n");
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
         *
         * @throws Exception _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) throws Exception {
	    if(rowCnt++==0) {
		t1 = System.currentTimeMillis();
	    }
	    ctx.flush();
	    long t2 = System.currentTimeMillis();
            if (every == 0) {
                System.err.println(rowCnt);
            } else if ((rowCnt) % every == 0) {
                printCnt++;
                String pre = "\b\b\b\b\b\b\b\b\b\b\b"+prefix;
                System.err.print(pre
                                 + StringUtil.padRight("#" + rowCnt, 10,
                                     " "));
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
    public static class DebugRows extends Processor {

        /**  */
        int rows;

        /**
         * _more_
         *
         * @param every _more_
         *
         * @param rows _more_
         */
        public DebugRows(int rows) {
            this.rows = rows;
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
            rowCnt++;
            if (rowCnt <= rows) {
                System.err.println("row #" + rowCnt + " cols: " + row.size()
                                   + " data:" + row.getValues());
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
    public static class If extends Processor {

        /**  */
        Seesv seesv;

        /**  */
        TextReader predicate;

        /**  */
        TextReader ctx;

        /**
         * _more_
         *
         *
         * @param dummy _more_
         * @param seesv _more_
         * @param predicate _more_
         * @param ctx _more_
         */
        public If(TextReader dummy, Seesv seesv, TextReader predicate,
                  TextReader ctx) {
            this.seesv   = seesv;
            this.predicate = predicate;
            this.ctx       = ctx;
        }

        /**
         *
         * @param ctx _more_
         *
         * @throws Exception _more_
         */
        @Override
        public void finish(TextReader ctx) throws Exception {
            super.finish(ctx);
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
            Row tmp = predicate.processRow(seesv, row);
            if (tmp == null) {
                return row;
            }
            row = tmp;
            boolean debug = false;
            debug = rowCnt > 1;
            if (debug) {
                System.err.println("IF:" + row);
            }
            row = this.ctx.processRow(seesv, row);
            if (debug) {
                System.err.println("After:" + row);
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
    public static class Verifier extends Processor {

        /** _more_ */
        private int cnt = -1;

        /**
         * _more_
         *
         * @param ctx _more_
         */
        public Verifier(TextReader ctx) {}


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
     * @version        $version$, Wed, Apr 13, '22
     * @author         Enter your name here...    
     */
    public static class Ext extends Processor {

        /**  */
        Process process;

        /**  */
        OutputStream outputStream;

        /**  */
        InputStream inputStream;

        /**  */
        PrintWriter pw;

        /**  */
        BufferedReader reader;

        /**  */
        StringTokenizer tokenizer;


        /**
         * _more_
         *
         * @param seesv _more_
         * @param ctx _more_
         * @param id _more_
         * @param args _more_
         */
        public Ext(Seesv seesv, TextReader ctx, String id,
                   List<String> args) {

            String path = (String) seesv.getProperty("seesv_ext_" + id);
            if (path == null) {
                fatal(ctx, "Could not find path property seesv_ext_" + id);
            }
            List<String> commands = new ArrayList<String>();
            commands.add(path);
            commands.addAll(args);
            try {
                //              System.err.println(commands);
                tokenizer = StringTokenizer.getCSVInstance();
                tokenizer.setEmptyTokenAsNull(true);
                ProcessBuilder pb = new ProcessBuilder(commands);
                process      = pb.start();
                outputStream = process.getOutputStream();
                inputStream  = process.getInputStream();
                pw           = new PrintWriter(outputStream);
                InputStreamReader isr =
                    new InputStreamReader(
                        inputStream, java.nio.charset.StandardCharsets.UTF_8);
                reader = new BufferedReader(isr);
            } catch (Exception exc) {
                fatal(ctx, "Error creating external command:" + args, exc);
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
         *
         * @throws Exception _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) throws Exception {
            String output = Utils.columnsToString(row.getValues(), ",");
            pw.println(output);
            pw.flush();
            //      System.err.println("write:" + output);
            Row    r    = new Row();
            String line = reader.readLine();
            //      System.err.println("read:" + line);
            if (line == null) {
                return null;
            }
            if (line.trim().equals("_null_")) {
                return null;
            }
            List<String> toks = Utils.tokenizeColumns(line, tokenizer);
            for (String tok : toks) {
                r.add(tok);
            }

            return r;
        }

        /**
         *
         * @param ctx _more_
         *
         * @throws Exception _more_
         */
        public void finish(TextReader ctx) throws Exception {
            super.finish(ctx);
            process.destroy();
        }

    }



    /**
     * Class description
     *
     *
     * @version        $version$, Wed, Apr 13, '22
     * @author         Enter your name here...    
     */
    public static class Exec extends Processor {

	Row header;


	List<String> commands;

        /**
         * _more_
         *
         * @param seesv _more_
         * @param ctx _more_
         * @param id _more_
         * @param args _more_
         */
        public Exec(Seesv seesv, TextReader ctx, String id,
                   List<String> args) {

            String path = (String) seesv.getProperty("seesv_exec_" + id);
            if (path == null) {
		path = (String) seesv.getProperty("seesv_ext_" + id);
	    }
            if (path == null) {
                fatal(ctx, "Could not find path property seesv_ext_" + id);
            }
            commands = new ArrayList<String>();
            commands.add(path);
            commands.addAll(args);
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
	    List<String> args = new ArrayList<String>(commands);
            try {
		if(header==null) {
		    header = row;
		    header.add("Result");
		    return header;
		}
		for(int i=1;i<args.size();i++) {
		    String v = replaceMacros(args.get(i),header,row);
		    args.set(i,v);
		}
                ProcessBuilder pb = new ProcessBuilder(args);
                Process process      = pb.start();
                OutputStream outputStream = process.getOutputStream();
                InputStream inputStream  = process.getInputStream();
		String result = IO.readInputStream(inputStream);
		row.add(result); 
		inputStream.close();
		process.destroy();
		return row;
            } catch (Exception exc) {
                fatal(ctx, "Error creating external command:" + args, exc);
		return null;
            }
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

	private boolean haveWrittenPrefix = false;

        /** _more_ */
        private String suffix;

        /** _more_ */
        private String delimiter=null;

	private String columnDelimiter = ",";

        /** _more_ */
        private boolean addPointHeader = false;

        /** _more_ */
        private boolean trim = false;

        /**  */
        private String commentChar;

        /**  */
        private Row headerRow;

        public Printer() {
	}

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
            this.prefix   = initDelimiter(prefix);
            this.template = initDelimiter(template);
            this.delimiter = initDelimiter(delimiter);
            this.suffix = initDelimiter(suffix);
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
         * @param delimiter _more_
         */
        public Printer(boolean addHeader, boolean trim, String columnDelimiter) {
            this(addHeader);
            this.columnDelimiter = initDelimiter(columnDelimiter);
            this.trim = trim;
        }

	@Override
        public void reset(boolean force) {
	    super.reset(force);
	    if(force) {
		headerRow = null;
		haveWrittenPrefix = false;
	    }
        }


        /**
         *
         * @param delimiter _more_
         */
        private String initDelimiter(String s) {
            if (s == null) {return s;}
	    if (s.equals("tab")) {
		s = "\t";
	    }
	    s = s.replace("\\n","\n").replaceAll("_tab_","\t").replaceAll("_nl_","\n");
	    return s;
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
	    //            debug("processRow");
            if (addPointHeader) {
                addPointHeader = false;
                handleHeaderRow(ctx.getWriter(), row, null /*exValues*/);
                return row;
            }
            handleRow(ctx, ctx.getWriter(), row);
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
        public void handleHeaderRow(Appendable writer, Row header,
                                     List exValues)
                throws Exception {
	    //	    System.err.println("\theader row:" + header);
	    
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
            writer.append(sb.toString());
            writer.append("\n");
        }


        /**
         * _more_
         *
         *
         * @param ctx _more_
         * @throws Exception _more_
         */
        @Override
        public void finish(TextReader ctx) throws Exception {
            debug("finish");
            if (suffix != null) {
                ctx.print(suffix);
		ctx.flush();
            }
            ctx.flush();
            super.finish(ctx);
        }


        /**
         * _more_
         *
         *
         * @param ctx _more_
         * @param writer _more_
         * @param row _more_
         *
         * @throws Exception _more_
         */
        public void handleRow(TextReader ctx, PrintWriter writer, Row row)
                throws Exception {
	    handleRow(ctx,writer,row,true);
	}

	int x = 0;
	public void handleRow(TextReader ctx, PrintWriter writer, Row row, boolean checkFirst)
	    throws Exception {	    
	    //	    System.err.println("\tHandle row:" + row);
            String  theTemplate = template;
	    if(theTemplate!=null) {
		theTemplate = theTemplate.replace("${filename}",ctx.getCurrentFilename());
	    }
            boolean firstRow    = rowCnt++ == 0;
	    if(headerRow==null) headerRow =row;
	    //	    System.err.println("\tfirstRow:" + firstRow +" row.isFirstRowInData:" +row.isFirstRowInData()+" header:" + headerRow);
            if (firstRow && row.isFirstRowInData()) {
                commentChar = ctx.getCommentChar();
                if (theTemplate != null) {
                    return;
                }
            } else {
		//		System.err.println("ROW:" + rowCnt);
                if (rowCnt>2 && delimiter != null && theTemplate != null) {
		    writer.append(delimiter);
                }
            }
	    //handle multiple data sources
	    if(checkFirst  && !firstRow && row.isFirstRowInData()) {
		return;
	    }

	    String ctxPrefix = ctx.getOutputPrefix();
	    if(ctxPrefix!=null) {
		ctx.setOutputPrefix(null);
		ctxPrefix = ctxPrefix.replaceAll("_bom_","\ufeff").replaceAll("_nl_","\n");
		writer.append(ctxPrefix);
	    }
     
            List    values        = row.getValues();
            boolean escapeColumns = true;
            if (theTemplate == null) {
		int size = values.size();
		int colIdx = 0;
                for (colIdx = 0; colIdx < size; colIdx++) {
                    Object v = values.get(colIdx);
                    if (colIdx > 0) {
			writer.append(columnDelimiter);
                    }
                    if (v == null) {
			continue;
		    }
		    String sv = v.toString();
		    if (trim) {
			sv = sv.trim();
		    }
		    if ((firstRow && sv.startsWith("#"))
			|| ((colIdx == 0) && (commentChar != null)
			    && sv.startsWith(commentChar))) {
			escapeColumns = false;
		    }
		    boolean addQuote = false;
		    if (escapeColumns) {
			addQuote = (sv.indexOf(columnDelimiter) >= 0)
			    || sv.indexOf("\n") >= 0 
			    || sv.indexOf("\r") >= 0;
			if (sv.indexOf("\"") >= 0) {
			    addQuote = true;
			    sv       = sv.replaceAll("\"", "\"\"");
			}
			if (addQuote) {
			    writer.append("\"");
			}
		    }
		    writer.append(sv);
		    if (addQuote) {
			writer.append("\"");
		    }
                }
            } else {
                for (int colIdx = 0; colIdx < values.size(); colIdx++) {
		    if(!headerRow.indexOk(colIdx)) continue;
                    Object v     = values.get(colIdx);
                    String sv    = v.toString();
                    String field = headerRow.getString(colIdx);
                    String fieldId = makeID(headerRow.getString(colIdx));		    
                    theTemplate = theTemplate.replace("${" + colIdx + "}",
						      sv).replace("${" + field + "}", sv).replace("${" + fieldId + "}", sv);
                }
            }


            if (theTemplate == null) {
		writer.append("\n");
		if(firstRow) {
		    for(String comment: ctx.getComments()) {
			if(commentChar==null)
			    writer.append("#");
			else
			    writer.append(commentChar);			
			writer.append(comment);
			writer.append("\n");
		    }
		}
            } else {
		if (theTemplate!=null && prefix != null && !haveWrittenPrefix) {
		    writer.append(ctx.applyMacros(prefix));
		    haveWrittenPrefix = true;
                }
                writer.append(theTemplate);
            }
        }




        /**
         * _more_
         *
         *
         * @param ctx _more_
         * @param writer _more_
         * @param rows _more_
         *
         * @throws Exception _more_
         */
        public void writeCsv(TextReader ctx, PrintWriter writer,
                             List<Row> rows)
                throws Exception {
            if (prefix != null) {
                writer.append(prefix);
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
                    writer.append(delimiter);
                }
                Row row = rows.get(i);
                handleRow(ctx, writer, row);
            }
            if (suffix != null) {
		writer.append(ctx.applyMacros(suffix));
            }
        }

    }




    public static class Subd extends Printer {

	private  List<Integer> indices;
	private List<String> cols;

        /**  */
        private Seesv seesv;

        /**  */
        private String output;

	private List<Range> ranges;

	private  int[] rangeIndices;
	private Row header;



	private Hashtable<String,PrintWriter> writers = new Hashtable<String,PrintWriter>();

	private List<PrintWriter> pws = new ArrayList<PrintWriter>();

        /**
         *
         * @param seesv _more_
         * @param ctx _more_
         * @param cols _more_
         * @param args _more_
         */
        public Subd(Seesv seesv,  List<String> cols, String rangeDef, String output) {
	    super(false,false,",");
	    this.cols =cols;
            this.seesv = seesv;
	    ranges = new ArrayList<Range>();
	    this.output=  output;
	    for(String tuple:Utils.split(rangeDef,",",true,true)) {
		List<String> toks = Utils.splitUpTo(tuple,";",3);
		if(toks.size()<2 || toks.size()>3) fatal(null,"Bad range specification for -subd:" + tuple);
		ranges.add(new Range(Double.parseDouble(toks.get(0)),
				    Double.parseDouble(toks.get(1)),
				    toks.size()>2?Integer.parseInt(toks.get(2)):10));

	    }
	    if(ranges.size()!=cols.size()) {
		fatal(null,"-subd requires the # of ranges = # columns");
	    }
        }

	private static class Range {
	    double min;
	    double max;
	    int steps;
	    

	    public Range(double min, double max, int steps) {
		this.min = min;
		this.max = max;
		this.steps = steps;
	    }
	    //  [ ][   ][  ]
	    public int getIndex(double d) {
		if(d<=min) return 0;
		if(d>=max) return steps-1;
		return (int)Math.floor((d-min)/(max-min)*steps);
	    }
	    public double getMin(int index) {
		return min+(max-min)/steps*index;
	    }
	    public double getMax(int index) {
		return min+(max-min)/steps*(index+1);
	    }	    
	}

        public void finish(TextReader ctx) throws Exception {
	    for(PrintWriter pw:pws) {
		pw.flush();
		pw.close();

	    }
	}


	private String fmt(double d) {
	    if(d==(int)d) return ""+(int)d;
	    return ""+d;

	}

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
		header = row;
		return row;
            }
            try {
		if(indices==null) {
		    indices =getIndices(ctx, cols);
		}
		if(rangeIndices == null) {
		    rangeIndices = new int[indices.size()];
		}
		for(int i=0;i<indices.size();i++) {
		    int vidx = indices.get(i);
		    //		    System.err.println("v:" + vidx +" row:" + row);
		    double d = row.getDouble(vidx);
		    int idx = ranges.get(i).getIndex(d);
		    rangeIndices[i]  = idx;
		}
		StringBuilder key = null;
		StringBuilder vkey = null;		
		for(int i=0;i<rangeIndices.length;i++) {
		    int idx= rangeIndices[i];
		    Range range = ranges.get(i);
		    if(vkey==null)
			vkey= new StringBuilder();
		    else
			vkey.append("_");
		    if(key==null)
			key= new StringBuilder();
		    else
			key.append("_");
		    key.append(idx);
		    vkey.append(fmt(range.getMin(idx))+"_" + fmt(range.getMax(idx)));
		}
		PrintWriter pw = writers.get(key.toString());
		if(pw == null) {
		    String file = output.replace("${ikey}",key).replace("${vkey}",vkey);
		    seesv.checkOkToWrite(file);
		    boolean exists = new File(file).exists();
		    //Open in append mode
		    FileWriter fw = new FileWriter(file, true);
		    BufferedWriter bw = new BufferedWriter(fw);
		    pw = new PrintWriter(bw);
		    pws.add(pw);
		    writers.put(key.toString(),pw);
		    if(!exists) {
			handleRow(ctx, pw, header,false);
		    }
		}
		handleRow(ctx, pw, row,false);
                return row;
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }
        }

    }




    public static class Chunker extends Printer {

	private Seesv seesv;
	private int numRows;

        /** _more_ */
        private String template;

	private int count;

	private Row header;

	int fileCnt  = 0;

	PrintWriter pw;

        /**
         */
        public Chunker(Seesv seesv, String template,int numRows) {
            this.seesv = seesv;
            this.template = template;
            this.numRows = numRows;
        }

        public void finish(TextReader ctx) throws Exception {
	    if(pw!=null) {
		pw.close();
	    }
	}

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
	    try {
		if (rowCnt++ == 0) {
		    header = row;
		    return row;
		}

		if(count>=numRows) {
		    if(pw!=null) {
			pw.close();
		    }
		    pw = null;
		    count=0;
		}
		if(pw == null) {
		    fileCnt++;
		    String file = template.replace("${number}",""+fileCnt);
		    seesv.checkOkToWrite(file);
		    FileWriter fw = new FileWriter(file);
		    BufferedWriter bw = new BufferedWriter(fw);
		    pw = new PrintWriter(bw);
		    handleRow(ctx, pw, header,false);
		}
		count++;
		handleRow(ctx, pw, row,false);
		return row;
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }
        }
    }

    /**
     * Class description
     *
     *
     * @version        $version$, Wed, Apr 13, '22
     * @author         Enter your name here...    
     */
    public static class Cols extends Processor {

        /**  */
        private int width;

        /**
         * ctor
         *
         * @param width _more_
         */
        public Cols(int width) {
            this.width = width;
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
            for (int i = 0; i < row.size(); i++) {
                String s = row.getString(i);
                if (s.length() < width) {
                    s = StringUtil.padLeft(s, width);
                } else if (s.length() > width) {
                    s = s.substring(0, width);
                }
                ctx.print(s);
            }
            ctx.println("");
            return row;
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
        private Dictionary<String, String> props;

        /**  */
        private List<String[]> patternProps = new ArrayList<String[]>();

        /** _more_ */
        private Row row1;

        /** _more_ */
        private String tableId;


        /**
         * _more_
         *
         *
         * @param ctx _more_
         * @param props _more_
         */
        public DbXml(TextReader ctx, Dictionary<String, String> props) {
            this.props = props;
            for (Enumeration k = props.keys(); k.hasMoreElements(); ) {
                String key = (String) k.nextElement();
                if (containsRegExp(key)) {
                    patternProps.add(new String[] { key, props.get(key) });
                }
            }
        }

        /**
         *
         * @param patternString _more_
         *
         * @return _more_
         */
        private static boolean containsRegExp(String patternString) {
            return ((patternString.indexOf('^') >= 0)
                    || (patternString.indexOf('*') >= 0)
                    || (patternString.indexOf('|') >= 0)
                    || (patternString.indexOf('(') >= 0)
                    || (patternString.indexOf('$') >= 0)
                    || (patternString.indexOf('?') >= 0)
                    || ((patternString.indexOf('[') >= 0)
                        && (patternString.indexOf(']')
                            >= 0)) || (patternString.indexOf('+') >= 0));
        }


        /**
         * Get the TableId property.
         *
         * @return The TableId
         */
        public String getTableId() {
            return tableId;
        }

        public boolean getDbProp(String colId, String prop, boolean dflt) {
	    String v = getDbProp(colId,prop,null);
	    if(v==null) return dflt;
	    return v.equals("true");
	}

        /**
         *
         * @param colId _more_
         * @param prop _more_
         * @param dflt _more_
         *
         * @return _more_
         */
        public String getDbProp(String colId, String prop, String dflt) {
            String v = Seesv.getDbProp(props, colId, prop, (String) null);
            if (v != null) {
                return v;
            }
            String key = (colId == null)
                         ? prop
                         : colId + "." + prop;
            for (String[] tuple : patternProps) {
                if (key.matches(tuple[0])) {
                    return tuple[1];
                }
            }

            return dflt;
        }


        /**
         * _more_
         *
         * @param reader _more_
         * @param row _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        @Override
        public Row processRow(TextReader reader, Row row) throws Exception {

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
            name = getDbProp("table", "name", name);

            String label = Utils.makeLabel(name);
            label   = getDbProp("table", "label", label);
            label   = label.replaceAll("\n", " ").replaceAll("\r", " ");
            tableId = Utils.makeLabel(name).toLowerCase().replaceAll(" ",
                                      "_");

            tableId = getDbProp("table", "id", tableId);

            String labels = getDbProp("table", "labelColumns", "");

            File   output = reader.getOutputFile();
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


            String tableAttrs = XmlUtil.attrs("id", tableId, "name", label);
            if (labels.length() > 0) {
                tableAttrs += XmlUtil.attrs("labelColumns", labels);
            }

            for (String prop : new String[] {
		    "defaultView", "mapProperties",
		    "defaultOrder", "icon", "showEntryCreate",
                "showFeedView", "showDateView", "showChartView",
		"mapMarkersShow","mapPolygonsShow","mapDotLimit"
            }) {
                String v = getDbProp("table", prop, (String) null);
                if (v != null) {
                    tableAttrs += XmlUtil.attr(prop, v);
                }
            }





            writer.println(XmlUtil.openTag("table", tableAttrs));

            for (String prop : new String[] { "searchForLabel",
                    "addressTemplate", "mapLabelTemplate",
                    "mapLabelTemplatePrint" }) {
                String v = getDbProp("table", prop, (String) null);
                if (v != null) {
                    writer.println("<" + prop + "><![CDATA[" + v + "]]></"
                                   + prop + ">");
                }
            }

            String header = (String) props.get("table.header");
	    if(header!=null) {
                writer.println("<header><![CDATA[");
                writer.print(header.replace("\\n","\n"));
                writer.println("]]></header>");
	    }
            String formjs = (String) props.get("table.formjs");
            if (formjs != null) {
                writer.println("<formjs><![CDATA[");
                if (formjs.startsWith("file:")) {
                    String jsfile = formjs.substring("file:".length());
                    if ( !IO.okToReadFrom(jsfile)) {
                        throw new RuntimeException("Cannot read file:"
                                + jsfile);
                    }
                    formjs = IO.readContents(jsfile);
                }
                writer.println(formjs);
                writer.println("]]></formjs>");

            }

            String include = (String) props.get("table.include");
            if (include != null) {
                if (include.startsWith("file:")) {
                    String jsfile = include.substring("file:".length());
                    if ( !IO.okToReadFrom(jsfile)) {
                        throw new RuntimeException("Cannot read file:"
                                + jsfile);
                    }
                    include = IO.readContents(jsfile);
                }
                writer.println(include);
            }



            List<Row> samples = new ArrayList<Row>();
            samples.add(row);
            boolean[] isNumeric = new boolean[row1.getValues().size()];
            boolean[] isInt = new boolean[row1.getValues().size()];	    
            for (int i = 0; i < isNumeric.length; i++) {
                isNumeric[i] = false;
                isInt[i] = false;		
            }

            for (Row sample : samples) {
                //                System.err.println("sample:" + sample);
                for (int colIdx = 0; colIdx < sample.getValues().size();
                        colIdx++) {
                    Object value = sample.getValues().get(colIdx);
                    try {
			double d = Double.parseDouble(value.toString());
                        //                        System.err.println("OK: " + row1.getValues().get(colIdx));
                        isNumeric[colIdx] = true;
			if(d == (int) d) {
			    isInt[colIdx]=true;
			} else {
			    isInt[colIdx]=false;
			}
                    } catch (Exception ignore) {}
                }
            }

            boolean dfltDoStats = getDbProp("table", "dostats",
                                            "false").equals("true");
            boolean dfltCanSearch = getDbProp("table", "cansearch",
                                        "true").equals("true");
            boolean dfltAddRawInput = getDbProp("table", "addrawinput",
                                        "false").equals("true");	    
            boolean dfltCanSort = getDbProp("table", "cansort",
                                            "false").equals("true");
            boolean dfltCanList = getDbProp("table", "canlist",
                                            "true").equals("true");
            String dfltChangeType = getDbProp("table", "changetype", "false");

            String format = getDbProp("table", "format", "yyyy-MM-dd HH:mm");
            String displayFormat = getDbProp("table", "displayFormat", (String)null);
            String numberFormat = getDbProp("table", "numberFormat", (String)null);
            for (int colIdx = 0; colIdx < row1.getValues().size(); colIdx++) {
                Object col   = row1.getValues().get(colIdx);
                String colId = makeID(col);
                if (colId.length() == 0) {
                    continue;
                }
                colId = getDbProp(colId, "id", colId);
                label = Utils.makeLabel(colId);


                String suffix = getDbProp(colId, "suffix", (String) null);
                String help   = getDbProp(colId, "help", (String) null);


                label = getDbProp(colId, "label", label);
                label = label.replaceAll("\n", " ").replaceAll("\r", " ");

                if (getDbProp(colId, "skip", "false").equals("true")) {
                    continue;
                }


                String  type     = getDbProp("table", "type", null);
		if(type==null) {
		    if (isInt[colIdx]) {
			type="int";
		    } else if (isNumeric[colIdx]) {
			type = "double";
		    }
                }

                StringBuilder attrs     = new StringBuilder();
                boolean       canList   = dfltCanList;
                boolean       canSearch = dfltCanSearch;
		boolean addRawInput =dfltAddRawInput;
                boolean       canSort   = dfltCanSort;

                attrs.append(XmlUtil.attrs(new String[] { "name", colId }));
                if (suffix != null) {
                    attrs.append(XmlUtil.attrs(new String[] { "suffix",
                            suffix }));
                }
                if (help != null) {
                    attrs.append(XmlUtil.attrs(new String[] { "help",
                            help }));
                }

                String placeholderMin = getDbProp(colId, "placeholderMin",
                                            (String) null);
                if (placeholderMin != null) {
                    attrs.append(XmlUtil.attrs(new String[] {
                        "placeholderMin",
                        placeholderMin }));
                }
                String placeholderMax = getDbProp(colId, "placeholderMax",
                                            (String) null);
                if (placeholderMax != null) {
                    attrs.append(XmlUtil.attrs(new String[] {
                        "placeholderMax",
                        placeholderMax }));
                }
                for (String prop : new String[] { "addnot", "unit",
                        "addfiletosearch" }) {
                    String v = getDbProp(colId, prop, (String) null);
                    if (v != null) {
                        attrs.append(XmlUtil.attrs(new String[] { prop, v }));
                    }
                }

                String placeholder = getDbProp(colId, "placeholder",
                                         (String) null);
                if (placeholder != null) {
                    attrs.append(XmlUtil.attrs(new String[] { "placeholder",
                            placeholder }));
                }
                String numberOfSearchWidgets = getDbProp(colId,
                                                   "numberOfSearchWidgets",
                                                   (String) null);
                if (numberOfSearchWidgets != null) {
                    attrs.append(XmlUtil.attrs(new String[] {
                        "numberOfSearchWidgets",
                        numberOfSearchWidgets }));

                }
                if (getDbProp(colId, "changetype",
                              dfltChangeType).equals("true")) {
                    attrs.append(XmlUtil.attrs(new String[] { "changetype",
                            "true" }));
                }
                String size = getDbProp(colId, "size", null);
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

                String tmp = getDbProp(colId, "type", type);
		if(tmp==null) tmp="string";
                if (tmp.length() == 0) {
                    tmp = type;
                }
                type = tmp;
                String values     = getDbProp(colId, "values", null);
                String searchRows = getDbProp(colId, "searchrows", "");
                String defaultsort = getDbProp(colId, "defaultsort",
                                         (String) null);
                if ((defaultsort != null) && defaultsort.equals("true")) {
                    attrs.append(XmlUtil.attrs(new String[] { "defaultsort",
                            "true" }));
                    String asc = getDbProp(colId, "ascending", (String) null);
                    if (asc != null) {
                        attrs.append(XmlUtil.attrs(new String[] { "ascending",
                                asc }));
                    }
                }


		boolean doPolygonSearch ="true".equals(getDbProp(colId, "dopolygonsearch","false"));
		if(doPolygonSearch)
		    attrs.append(XmlUtil.attrs(new String[] {"dopolygonsearch","true"}));

		boolean showMultiples ="true".equals(getDbProp(colId, "show_multiples","true"));
		if(!showMultiples)
		    attrs.append(XmlUtil.attrs(new String[] {"enumeration_multiples","false"}));

                canSearch = "true".equals(getDbProp(colId, "cansearch",
                        canSearch + ""));
                addRawInput = "true".equals(getDbProp(colId, "addrawinput",
						      addRawInput + ""));		
                canSort = "true".equals(getDbProp(colId, "cansort",
                        canSort + ""));
                canList = "true".equals(getDbProp(colId, "canlist",   canList + ""));
		if(getDbProp(colId, "after.canlist",dfltCanList)!=dfltCanList) {
		    dfltCanList=!dfltCanList;
		}

                attrs.append(XmlUtil.attrs(new String[] {
                    "type", type, "label", label,
		    "cansearch", "" + canSearch,
                    "canlist", "" + canList
                }));
		if(addRawInput) {
                attrs.append(XmlUtil.attrs(new String[] {
			    "addrawinput", "" + addRawInput
                }));

		}
                String preamble = getDbProp(colId, "preamble", (String) null);
		if(preamble!=null) {
                    writer.println(preamble);
		}

                String group = getDbProp(colId, "group", (String) null);
                if (group != null) {
                    attrs.append(XmlUtil.attrs(new String[] { "group",
                            group }));
                }

                if (canSort) {
                    attrs.append(XmlUtil.attrs(new String[] { "cansort",
                            "true" }));
                }
                if (values != null) {
                    attrs.append(XmlUtil.attrs(new String[] { "values",
                            values }));
                }
                String lookupDB = getDbProp(colId, "lookupdb", (String) null);

                if (lookupDB != null) {
                    attrs.append(XmlUtil.attrs(new String[] { "lookupdb",
                            lookupDB }));
                }
                if (searchRows.length() > 0) {
                    attrs.append(XmlUtil.attrs(new String[] { "searchrows",
                            searchRows }));
                }
                if (type.equals("date") || type.equals("datetime")) {
                    attrs.append(XmlUtil.attrs(new String[] { "format",
                            getDbProp(colId, "format", format) }));
		    String fmt = getDbProp(colId, "displayFormat", displayFormat);
		    if(fmt!=null) 
			attrs.append(XmlUtil.attrs(new String[] { "displayFormat",
								 fmt}));
	   
                }

		String numFmt = getDbProp(colId, "numberFormat", numberFormat);
		if(numFmt!=null) 
		    attrs.append(XmlUtil.attrs(new String[] { "numberFormat",
								 numFmt}));

                StringBuffer inner = new StringBuffer();
                boolean isindex = "true".equals(getDbProp(colId, "isindex",
							  "false"));

                if (isindex) {
		    attrs.append(XmlUtil.attrs(new String[] { "isindex","true"}));

                }

                boolean doStats = "true".equals(getDbProp(colId, "dostats",
                                      dfltDoStats + ""));


                if (doStats) {
		    attrs.append(XmlUtil.attrs(new String[] { "dostats","true"}));
                }
                if (Seesv.getDbProp(props, colId, "iscategory", false)) {
                    inner.append(XmlUtil.tag("property",
                                             XmlUtil.attrs(new String[] {
                                                 "name",
                            "iscategory", "value", "true" })));
                }
                if (Seesv.getDbProp(props, colId, "formap", false)) {
                    inner.append(XmlUtil.tag("property",
                                             XmlUtil.attrs(new String[] {
                                                 "name",
                            "formap", "value", "true" })));
                }

                if (Seesv.getDbProp(props, colId, "islabel", false)) {
                    inner.append(XmlUtil.tag("property",
                                             XmlUtil.attrs(new String[] {
                                                 "name",
                            "islabel", "value", "true" })));
                }

                if (inner.length() > 0) {
                    writer.println(XmlUtil.tag("column", attrs.toString(),
                            inner.toString()));
                } else {
                    writer.println(XmlUtil.tag("column", attrs.toString()));
                }
            }
            writer.print(XmlUtil.closeTag("table"));
            writer.print(XmlUtil.closeTag("tables"));	    
            writer.flush();
	    writer.close();
            return null;
        }
    }


    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Nov 4, '21
     * @author         Enter your name here...
     */
    public static class DbProps extends Processor {

        /**  */
        String idPattern;

        /**  */
        String suffixPattern;

        /**
         * _more_
         *
         *
         * @param ctx _more_
         * @param idPattern _more_
         * @param suffixPattern _more_
         */
        public DbProps(TextReader ctx, String idPattern,
                       String suffixPattern) {
            this.idPattern = idPattern;
            if (this.idPattern.length() > 0) {
                this.idPattern = ".*" + this.idPattern + ".*";
            }
            this.suffixPattern = suffixPattern;
            if (this.suffixPattern.length() > 0) {
                this.suffixPattern = ".*" + this.suffixPattern + ".*";
            }
        }



        /**
         *
         * @param id _more_
         * @param suffix _more_
         * @param value _more_
         *
         * @return _more_
         */
        private boolean print(String id, String suffix, String value) {
            if (Utils.stringDefined(idPattern)) {
                if ( !id.matches(idPattern)) {
                    return false;
                }
            }
           if (Utils.stringDefined(suffixPattern)) {
                if ( !suffix.matches(suffixPattern)) {
                    return false;
                }
            }
            System.out.print(id + "." + suffix + " " + value + "  ");

            return true;
        }

        /**
         * _more_
         *
         * @param reader _more_
         * @param row _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        @Override
        public Row processRow(TextReader reader, Row row) throws Exception {
            if (rowCnt++ > 0) {
                return null;
            }
            boolean didone = false;
            for (Object o : row.getValues()) {
                boolean p     = false;
                String  colId = makeID(o.toString());
                if (colId.length() == 0) {
                    continue;
                }
                String type = "{}";
                if ((colId.indexOf("type") >= 0)
                        || (colId.indexOf("category") >= 0)) {
                    type = "enumerationplus";
                } else if (colId.indexOf("date") >= 0) {
                    type = "date";
                }
                p      |= print(colId, "type", type);
                p      |= print(colId, "cansearch", "true");
                p      |= print(colId, "canlist", "true");
                p      |= print(colId, "cansort", "true");
                didone |= p;
                if (p) {
                    System.out.print(" \\\n");
                }
            }
            if ( !didone) {
                for (Object o : row.getValues()) {
                    String id = Utils.makeID(o.toString());
                    if (Utils.stringDefined(idPattern)) {
                        if ( !id.matches(idPattern)) {
                            continue;
                        }
                    }
                    System.out.println(id);
                }
            }

            return row;
        }
    }

    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Nov 4, '21
     * @author         Enter your name here...
     */
    public static class Fields extends Processor {


        /**
         * _more_
         *
         * @param ctx _more_
         */
        public Fields(TextReader ctx) {}


        /**
         *
         *
         * @param reader _more_
         * @param row _more_
         *  @return _more_
         * @throws Exception _more_
         */
        @Override
        public Row processRow(TextReader reader, Row row) throws Exception {
            if (rowCnt++ == 0) {
                int cnt = 0;
                for (Object id : row.getValues()) {
                    String sid = Utils.makeID(id.toString());
                    if (sid.length() > 0) {
                        if (cnt++ > 0) {
                            System.out.print(",");
                        }
                        System.out.print(sid);
                    }
                }
                System.out.print("\n");

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
	@Override
        public void reset(boolean force) {
	    super.reset(force);
            contains = null;
            values   = null;
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
         *
         * @param ctx _more_
         *   @throws Exception On badness
         */
        @Override
        public void finish(TextReader ctx) throws Exception {
            super.finish(ctx);
            if (contains == null) {
                ctx.print("-0");
            } else {
                for (int i = 0; i < values.size(); i++) {
                    List uniqueValues = values.get(i);
                    for (int j = 0; j < uniqueValues.size(); j++) {
                        if (j > 0) {
                            //                            ctx.print(",");
                        }
                        ctx.println(uniqueValues.get(j));
                    }
                }
            }
            ctx.flush();
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
         *
         * @param ctx _more_
         */
        public Counter(TextReader ctx) {}

        /**
         * _more_
         *
         *
         * @param ctx _more_
         * @param strict _more_
         */
        public Counter(TextReader ctx, boolean strict) {
            this.strict = strict;
        }


        /**
         * _more_
         *
         *
         * @param ctx _more_
         * @param strict _more_
         * @param error _more_
         */
        public Counter(TextReader ctx, boolean strict, boolean error) {
            this.strict = strict;
            this.error  = error;
        }

        /**
         * _more_
         */
	@Override
        public void reset(boolean force) {
	    super.reset(force);
            rowCount     = 0;
            uniqueCounts = new Hashtable<Integer, Integer>();
            counts       = new ArrayList<Integer>();
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
            int size = row.size();

            if (currentNumCols < 0) {
                currentNumCols = size;
            }


            if (strict) {
                if (size != currentNumCols) {
                    if (error) {
                        throw new IllegalArgumentException("Bad line:" + row);
                    }
                    System.err.println("skipping:" + row);

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
         * @param ctx _more_
         * @throws Exception On badness
         */
        @Override
        public void finish(TextReader ctx) throws Exception {
            super.finish(ctx);
            if (strict) {
                return;
            }

            ctx.println("Rows:" + rowCount);
            for (Integer cnt : counts) {
                int value = uniqueCounts.get(cnt);
                ctx.println("\tcolumns:" + cnt + " #:" + value);
            }
            ctx.flush();
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
         *
         * @param ctx _more_
         */
        public Logger(TextReader ctx) {}

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

        /**  */
        private int maxWidth = 0;

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
         * @param ctx _more_
         * @param row _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) throws Exception {
            if (headerValues == null) {
                headerValues = new ArrayList();
                for (Object obj : row.getValues()) {
                    String name = obj.toString();
                    if (name.startsWith("#fields=")) {
                        name = name.substring("#fields=".length());
                    }
                    String args = StringUtil.findPattern(name, "\\[(.*)\\]");
                    name = name.replaceAll("\\[.*\\]", "");
                    headerValues.add(name);
                    maxWidth = Math.min(40,
                                        Math.max(maxWidth, name.length()));
                }

                return row;
            }
            printRow(ctx, row);

            return row;
        }


        /**
         * _more_
         *
         * @param ctx _more_
         * @param row _more_
         *
         * @throws Exception _more_
         */
        public void printRow(TextReader ctx, Row row) throws Exception {
            if (headerValues == null) {
                headerValues = row.getValues();
                return;
            }
            List values = row.getValues();
            cnt++;
            ctx.println("#" + cnt);
            for (int i = 0; i < values.size(); i++) {
                String label = (i < headerValues.size())
                               ? headerValues.get(i).toString()
                               : "NA";
                label = StringUtil.padLeft(label, maxWidth);
                ctx.println(label + ": " + values.get(i));
            }
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
            sb.append(Utils.columnsToString(row.getValues(), ","));
            sb.append("\n");
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

        /**  */
        private String dflt;
        private List<String> dflts;	

        /**
         * _more_
         *
         *
         *
         * @param ctx _more_
         * @param keys1 _more_
         * @param values1 _more_
         * @param file _more_
         * @param keys2 _more_
         * @param dflt _more_
         */
        public Joiner(TextReader ctx, List<String> keys1,
                      List<String> values1, String file, List<String> keys2,
                      String dflt) {
            this.keys1   = keys1;
            this.values1 = values1;
            this.keys2   = keys2;
            this.file    = file;
            this.dflt    = dflt;
	    this.dflts = Utils.split(this.dflt,",",false,false);
            try {
                init(ctx);
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }
        }

	int xcnt = 0;
	int ycnt = 0;	
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
            List<Integer> keys1Indices = null;
            //      System.err.println("key:" + keys1 +" " + keys1Indices);
            BufferedReader br = new BufferedReader(
                                    new InputStreamReader(
                                        getInputStream(file)));
            SeesvOperator operator = null;
            TextReader  reader   = new TextReader(br);
            map        = new Hashtable<String, Row>();
            headerRow1 = null;
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
                    keys1Indices   = operator.getIndices(reader, keys1);
                    values1Indices = operator.getIndices(reader, values1);
                }

                String key = "";
                for (int i : keys1Indices) {
                    if ((i < 0) || (i >= cols.size())) {
                        fatal(ctx,
                              "Mismatch between columns and keys. Columns:"
                              + cols + " key index:" + i);
                    }
                    key += cols.get(i) + "_";
                }
                Row row = new Row(cols);
                if (headerRow1 == null) {
                    headerRow1 = row;
                }
		//		if(xcnt++<10)   System.err.println("key:" + key +" row:" + row);
                map.put(key, row);
            }
            if (operator == null) {
                fatal(ctx, "Unable to read any data from:" + file);
            }
        }


        /**  */
        List<Integer> keys2Indices;

        /**
         * _more_
         *
         * @param ctx _more_
         * @param row _more_
         *
         *
         * @return _more_
         * @throws Exception On badness
         */
        @Override
        public Row processRow(TextReader ctx, Row row) throws Exception {

            if (keys2Indices == null) {
                keys2Indices = getIndices(ctx, keys2);
            }
            if (headerRow2 == null) {
                headerRow2 = row;
                //                System.err.println("ROW:" + headerRow1);
                for (int j : values1Indices) {
                    //                    System.err.println("idx:" + j);
                    row.add(headerRow1.get(j));
                }

                return row;
            }
            String key = "";
            for (int i : keys2Indices) {
                key += row.getString(i) + "_";
            }
	    //	    if(ycnt++<10)System.err.println("value:" + key);
            Row other = map.get(key);
            if (other == null) {
                for (int j=0;j<values1Indices.size();j++)  {
		    if(j<dflts.size()) {
			row.add(dflts.get(j));
		    } else {
			row.add(dflts.get(dflts.size()-1));
		    }
                }

                return row;
            }
            for (int j : values1Indices) {
		//		if(xcnt++<50) System.err.println("J:" +j +" " +other.get(j));
                row.add(other.get(j));
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
    public static class FuzzyJoiner extends Processor {

        /**  */
        private JaroWinklerDistance distance = new JaroWinklerDistance();

        /**  */
        private int threshold = 85;

        /**  */
        private String dflt;

        /** _more_ */
        private List<String> keys1;

        /**  */
        private List<Integer> keys2Indices;

        /** _more_ */
        private List<String> values1;

        /** _more_ */
        private List<String> keys2;

        /** _more_ */
        private List<KeyRow> rows;

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
         *
         *
         * @param ctx _more_
         * @param threshold _more_
         * @param keys1 _more_
         * @param values1 _more_
         * @param file _more_
         * @param keys2 _more_
         * @param dflt _more_
         */
        public FuzzyJoiner(TextReader ctx, int threshold, List<String> keys1,
                           List<String> values1, String file,
                           List<String> keys2, String dflt) {
            this.threshold = threshold;
            this.keys1     = keys1;
            this.values1   = values1;
            this.keys2     = keys2;
            this.dflt      = dflt;
            try {
                init(ctx, file);
            } catch (Exception exc) {
                fatal(ctx, "Error opening file:" + file, exc);
            }
        }


        /**
         * Class description
         *
         *
         * @version        $version$, Thu, Nov 4, '21
         * @author         Enter your name here...
         */
        static class KeyRow {

            /**  */
            String key;

            /**  */
            Row row;

            /**
             *
             *
             * @param key _more_
             * @param row _more_
             */
            KeyRow(String key, Row row) {
                this.key = key;
                this.row = row;
            }
        }


        /**
         * _more_
         *
         *
         *
         * @param ctx _more_
         * @param file _more_
         * @throws Exception _more_
         */
        private void init(TextReader ctx, String file) throws Exception {
            List<Integer> keys1Indices = null;
            BufferedReader br = new BufferedReader(
                                    new InputStreamReader(
                                        getInputStream(file)));
            SeesvOperator operator = null;
            TextReader  reader   = new TextReader(br);
            rows       = new ArrayList<KeyRow>();
            headerRow1 = null;
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
                    keys1Indices   = operator.getIndices(reader, keys1);
                    values1Indices = operator.getIndices(reader, values1);
                }

                String key = "";
                for (int i = 0; i < keys1Indices.size(); i++) {
                    if (i > 0) {
                        key += "_";
                    }
                    int index = keys1Indices.get(i);
                    if ((index < 0) || (index >= cols.size())) {
                        fatal(ctx,
                              "Mismatch between columns and keys. Columns:"
                              + cols + " key index:" + index);
                    }
                    key += cols.get(index);
                }
                Row row = new Row(cols);
                if (headerRow1 == null) {
                    headerRow1 = row;
                }
                rows.add(new KeyRow(key, row));
            }
            if (operator == null) {
                fatal(ctx, "Unable to read any data from:" + file);
            }
        }


        /**
         * _more_
         *
         * @param ctx _more_
         * @param row _more_
         *
         * @return _more_
         * @throws Exception On badness
         */
        @Override
        public Row processRow(TextReader ctx, Row row) throws Exception {
            if (keys2Indices == null) {
                keys2Indices = getIndices(ctx, keys2);
            }
            if (headerRow2 == null) {
                headerRow2 = row;
                for (int j : values1Indices) {
                    row.add(headerRow1.get(j));
                }

                return row;
            }
            String key = "";
            for (int i = 0; i < keys2Indices.size(); i++) {
                if (i > 0) {
                    key += "_";
                }
                key += row.getString(keys2Indices.get(i));
            }
            int bestScore = -1;
            Row bestMatch = null;
            //      System.err.println("key:" + key);
            for (KeyRow keyRow : rows) {
                //              int score = me.xdrop.fuzzywuzzy.FuzzySearch.ratio(key,keyRow.key);
                int score = ((int) (100 * distance.apply(key, keyRow.key)));
                if (score < threshold) {
                    continue;
                }
                if ((bestMatch == null) || (score > bestScore)) {
                    bestScore = score;
                    bestMatch = keyRow.row;
                }
            }

            Row other = bestMatch;
            if (other == null) {
                for (int j : values1Indices) {
                    row.add(dflt);
                }

                return row;
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
     * @version        $version$, Thu, Nov 4, '21
     * @author         Enter your name here...
     */
    public static class Doit extends Processor {

        /**  */
        private HashSet genres = new HashSet();

        /**  */
        private SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy");

        /**  */
        private SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd");

        /**
         *
         */
        public Doit() {}

        /**
         * @param ctx _more_
         * @param row _more_
         *
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            try {
                return processRowInner(ctx, row);
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }
        }

        /**
         *
         * @param v _more_
         * @param tag _more_
         *
         * @return _more_
         */
        private String makeTags(Object v, String tag) {
            List<String>  toks = Utils.split(v.toString(), ",");
            StringBuilder sb   = new StringBuilder();
            for (String value : toks) {
                value = value.trim();
                if (value.length() == 0) {
                    continue;
                }
                String contents = XmlUtil.tag("attr",
                                      XmlUtil.attr("encoded", "false")
                                      + XmlUtil.attr("index",
                                          "1"), XmlUtil.getCdata(value));
                sb.append(XmlUtil.tag("metadata",
                                      XmlUtil.attr("type", "netflix_" + tag),
                                      contents));
                sb.append("\n");
            }

            return sb.toString();
        }


        /**
         *
         * @param tag _more_
         * @param v _more_
         *
         * @return _more_
         */
        private String makeTag(String tag, Object v) {
            return XmlUtil.tag(tag, "",
                               XmlUtil.getCdata(v.toString().trim()));
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
        public Row processRowInner(TextReader ctx, Row row) throws Exception {

            if (rowCnt++ == 0) {
                ctx.println("<entries>");

                return row;
            }
            if (rowCnt > 50) {
                return null;
            }
            String       dttm      = ((String) row.get(18)).trim();
            List<String> genreToks = Utils.split((String) row.get(1), ",");
            String       genre     = (genreToks.size() > 0)
                                     ? genreToks.get(0)
                                     : "Film";
            genre = genre.trim();
            if (genre.length() == 0) {
                genre = "Miscellaneous";
            }
            if ( !genres.contains(genre)) {
                ctx.print(XmlUtil.tag("entry",
                                     XmlUtil.attr("type", "group")
                                     + XmlUtil.attr("id", genre)
                                     + XmlUtil.attr("name", genre), ""));
                genres.add(genre);
            }

            ctx.print("<entry " + XmlUtil.attr("type", "type_netflix_movie")
                     + XmlUtil.attr("parent", genre));
            if (dttm.length() > 0) {
                Date date = sdf.parse(dttm);
                ctx.print(XmlUtil.attr("fromdate", sdf2.format(date)));
            }
            ctx.println(">");
            ctx.print(makeTag("name", row.get(0)));
            ctx.print(makeTag("description",
                             "<snippet>" + row.get(23) + "</snippet>"));
            ctx.print(makeTag("series_or_movie", row.get(4)));
            ctx.print(makeTag("hidden_gem_score", row.get(5)));
            ctx.print(makeTag("runtime", row.get(7)));
            ctx.print(makeTag("view_rating", row.get(11)));
            ctx.print(makeTag("imdb_score", row.get(12)));
            ctx.print(makeTag("rotten_tomatoes_score", row.get(13)));
            ctx.print(makeTag("metacritic_score", row.get(14)));
            ctx.print(makeTag("awards_received", row.get(15)));
            ctx.print(makeTag("awards_nominated_for", row.get(16)));
            ctx.print(makeTag("boxoffice", row.get(17)));
            ctx.print(makeTag("release_date", row.get(18)));
            ctx.print(makeTag("netflix_release_date", row.get(19)));
            ctx.print(makeTag("netflix_link", row.get(21)));
            ctx.print(makeTag("imdb_link", row.get(22)));
            ctx.print(makeTag("imdb_votes", row.get(24)));
            String thumb = row.getString(25);
            if (thumb.trim().length() > 0) {
                ctx.print(
                    "<metadata type=\"content.thumbnail\"><attr index=\"1\" encoded=\"false\">"
                    + thumb + "</attr></metadata>");
            }
            ctx.print(makeTag("poster", row.get(26)));
            ctx.print(makeTag("tmdb_trailer", row.get(27)));
            ctx.print(makeTag("trailer_site", row.get(28)));
            ctx.print(makeTags(row.get(1), "genre"));
            ctx.print(makeTags(row.get(2), "tag"));
            ctx.print(makeTags(row.get(3), "language"));
            ctx.print(makeTags(row.get(6), "country_availability"));
            ctx.print(makeTags(row.get(10), "actor"));
            ctx.print(makeTags(row.get(20), "production_house"));
            ctx.print(makeTags(row.get(8), "director"));
            ctx.print(makeTags(row.get(9), "writer"));
            ctx.println("</entry>");

            return row;
        }

        /**
         *
         * @param ctx _more_
         *
         * @throws Exception _more_
         */
        public void finish(TextReader ctx) throws Exception {
            super.finish(ctx);
            ctx.println("</entries>");
        }

    }




    /**
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        String s = args[0];
        org.apache.commons.text.similarity.JaroWinklerDistance distance =
            new org.apache.commons.text.similarity.JaroWinklerDistance();
        org.apache.commons.text.similarity.FuzzyScore fuzzy =
            new org.apache.commons.text.similarity.FuzzyScore(
                java.util.Locale.getDefault());
        LevenshteinDistance lev     = new LevenshteinDistance(10);
        Soundex             soundex = new Soundex();
        for (int i = 1; i < args.length; i++) {
            int fuzzyScore = fuzzy.fuzzyScore(s, args[i]).intValue();
            int levenshteinScore = me.xdrop.fuzzywuzzy.FuzzySearch.ratio(s,
                                       args[i]);
            //      int fuzzy = me.xdrop.fuzzywuzzy.FuzzySearch.ratio(s,args[i]);
            int    sound = 25 * soundex.difference(s, args[i]);
            double jaro  = distance.apply(s, args[i]);
            System.err.println("lev:" + lev.apply(s, args[i]));
            //      System.err.println("lev:" + levenshteinScore +" fuzzy:" + fuzzyScore +" sound:" + sound +" jaro:" + ((int)(100*jaro)) +" " + args[i]);
        }
    }




}
