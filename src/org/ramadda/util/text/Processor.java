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
import ucar.unidata.xml.XmlUtil;

import java.io.*;
import java.net.URL;

import java.text.DateFormat;
import java.text.DecimalFormat;
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
 *
 * @author Jeff McWhirter
 */

public abstract class Processor extends CsvOperator {

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



    /** the processor chain*/
    private Processor nextProcessor;


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
       Set the NextProcessor property.

       @param value The new value for NextProcessor
    **/
    public void setNextProcessor (Processor value) {
	nextProcessor = value;
    }

    /**
       Get the NextProcessor property.

       @return The NextProcessor
    **/
    public Processor getNextProcessor () {
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

    private Row extraRow;
    private boolean consumeColumns = false;

    public Row handleRow(TextReader ctx, Row row) throws Exception {
	setHeaderIfNeeded(row);
	row = processRow(ctx, row);
	if(row!=null && nextProcessor!=null) {
	    row = nextProcessor.handleRow(ctx,row);
	}
	return row;
    }


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

    public void finish(TextReader ctx) throws Exception {
	if(nextProcessor!=null) nextProcessor.finish(ctx);
    }


    /**
     * _more_
     */
    public void reset() {
	if(nextProcessor!=null) nextProcessor.reset();
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





    public static class Dissector extends Processor {
	Pattern pattern;
	List<String> patternNames;
	int index;

	public Dissector(String col, String pattern) {
	    super(col);
	    patternNames = new ArrayList<String>();
            pattern = Utils.extractPatternNames(pattern,
						patternNames);
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
		for(String name: patternNames)
		    row.add(name);
		return row;
            }
	    String v = row.getString(index);
	    Matcher matcher = pattern.matcher(v);
            if ( !matcher.find()) {
		for(String name: patternNames)
		    row.add("");
	    } else {
		for(int i=0;i<patternNames.size();i++) {
		    Object value = matcher.group(i+ 1);
		    if(value==null) value="";
		    row.add(value);
		}
	    }

            return row;
        }
    }


    public static class Downloader extends Processor {
	private CsvUtil csvUtil;
	private String suffix;
	private int index;
	public Downloader(CsvUtil csvUtil, String col, String suffix) {
	    super(col);
	    this.csvUtil = csvUtil;
	    this.suffix = suffix;
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
	    if(suffix.length()==0) suffix = IOUtil.getFileTail(value);
	    File tmpFile = csvUtil.getTmpFile(suffix);
	    if(tmpFile==null) {
		row.add("");
		return row;
	    }
	    if(value.length()==0) {
		row.add("");
		return row;
	    }	    
	    try {
		URL url = new URL(value);
		InputStream input = IO.getInputStream(url);
		FileOutputStream fos = new FileOutputStream(tmpFile);
		IOUtil.writeTo(input, fos);
		IOUtil.close(input);
		IOUtil.close(fos);
		row.add(tmpFile.getName());
	    } catch(Exception exc) {
		System.err.println("Error downloading URL:" + value +" error:" + exc);
		row.add("");
	    }
	    return row;

	}
    }


    public static class KeyValue extends Processor {
	private List<String> keys;
	private int index =-1;
	private String extraId;
	public KeyValue(String col) {
	    super(col);
	}


        /**
         * @param ctx _more
         * @param row _more_
         *
         * @return _more_
         */
        @Override
	public Row processRow(TextReader ctx, Row row)
            throws Exception {
	    if(Misc.equals(row.getId(), extraId)) return row;
	    rowCnt++;
	    if(index==-1) index = getIndex(ctx);
	    if(rowCnt==1) return null;
	    String v = row.getString(index);
	    Hashtable<String,String> props = Utils.parseKeyValue(v);
	    Row header = null;
	    if(keys==null) {
		keys = new ArrayList<String>();
		for (Enumeration k = props.keys(); k.hasMoreElements(); ) {
		    String key = (String) k.nextElement();
		    keys.add(key);
		}
		header =  new Row();
		Collections.sort(keys);
		for(String key: keys)
		    header.add(key);
	    }

	    Row newRow = new Row();
	    for(String key: keys) {
		String value = props.get(key);
		if(value==null) value = "";
		newRow.add(value);
	    }
	    if(header!=null) {
		extraId = newRow.getId();
		ctx.setExtraRow(newRow);
		Row tmp = header;
		header = null;
		return tmp;
	    }
	    return newRow;
        }
    }
    
    public static class First extends Processor {
	private int index = -1;
	private String name;
	private int number;
	public First(String col,String name, String num) {
	    super(col);
	    this.name = name;
	    number = Integer.parseInt(num);
	}


        /**
         * @param ctx _more
         * @param row _more_
         *
         * @return _more_
         */
        @Override
	public Row processRow(TextReader ctx, Row row)
            throws Exception {
	    if(rowCnt++==0) {
		row.add(name);
		return row;
	    }
	    if(index==-1) index =  getIndex(ctx);
	    String v = row.getString(index);
	    if(v.length()<number) row.add(v);
	    else row.add(v.substring(0,number));
	    return row;
        }
    }

    public static class Last extends Processor {
	private int index = -1;
	private String name;
	private int number;
	public Last(String col,String name, String num) {
	    super(col);
	    this.name = name;
	    number = Integer.parseInt(num);
	}


        /**
         * @param ctx _more
         * @param row _more_
         *
         * @return _more_
         */
        @Override
	public Row processRow(TextReader ctx, Row row)
            throws Exception {
	    if(rowCnt++==0) {
		row.add(name);
		return row;
	    }
	    if(index==-1) index =  getIndex(ctx);
	    String v = row.getString(index);
	    int idx = v.length()-number;
	    if(idx<0) row.add(v);
	    else {
		row.add(v.substring(idx));
	    }
	    return row;
        }
    }


    public static class Between extends Processor {
	private int index = -1;
	private String name;
	private int start;
	private int end;    

	public Between(String col,String name, String start, String end) {
	    super(col);
	    this.name = name;
	    this.start = Integer.parseInt(start);
	    this.end = Integer.parseInt(end);	    
	}


        /**
         * @param ctx _more
         * @param row _more_
         *
         * @return _more_
         */
        @Override
	public Row processRow(TextReader ctx, Row row)
            throws Exception {
	    if(rowCnt++==0) {
		row.add(name);
		return row;
	    }
	    if(index==-1) index =  getIndex(ctx);
	    String v = row.getString(index);
	    if(start>=v.length()) {
		row.add("");
	    } else {
		if(end>=v.length()) {
		    row.add(v.substring(start));
		} else {
		    row.add(v.substring(start,end+1));
		}
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
    public static class Dots extends Processor {
	int every;

        /**
         * _more_
         */
        public Dots(int every) {
	    this.every = every;
	}

	@Override
        public void finish(TextReader ctx) 
	    throws Exception {
	    super.finish(ctx);
	    System.err.print("\n");
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
	    if(every==0) {
		System.err.println(rowCnt);
	    } else  if((rowCnt)%every == 0) {
		System.err.print("."); 
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
         */
        public Verifier() {}


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
         * @param ctx _more_
         * @param row _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) throws Exception {
            debug("processRow");
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
         * @param ctx _more_
         * @param rows _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        @Override
        public void finish(TextReader ctx)
	    throws Exception {
	    super.finish(ctx);
            debug("finish");
            if (suffix != null) {
                ctx.getWriter().print(suffix);
            }
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
        private void handleRow(TextReader ctx, PrintWriter writer, Row row)
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
				&& (ctx.getCommentChar() != null)
				&& sv.startsWith(
						 ctx.getCommentChar()))) {
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
                handleRow(ctx, writer, row);
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
     * @version        $version$, Sat, Apr 3, '21
     * @author         Enter your name here...    
     */
    public static class ToJson extends Processor {

        /** _more_          */
        private Row headerRow;

        /**
         * ctor
         */
        public ToJson() {}

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
            if (headerRow == null) {
                headerRow = row;
                ctx.getWriter().println("[");
                return row;
            }
	    handleRow(ctx, ctx.getWriter(), row);
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
        public void finish(TextReader ctx) 
	    throws Exception {
	    super.finish(ctx);
            ctx.getWriter().println("]");
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
        private void handleRow(TextReader ctx, PrintWriter writer, Row row)
	    throws Exception {
            rowCnt++;
            if (rowCnt > 1) {
                writer.println(",");
            }
            List<String> attrs = new ArrayList<String>();
            for (int i = 0; i < headerRow.size(); i++) {
                String field = headerRow.getString(i);
                String value = row.getString(i);
                attrs.add(field);
                attrs.add(value);
            }
            writer.print(Json.mapAndGuessType(attrs));
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

	private List<String[]> patternProps = new ArrayList<String[]>();

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
	    for (Enumeration k = props.keys(); k.hasMoreElements(); ) {
		String key = (String) k.nextElement();
		if(containsRegExp(key)) 
		    patternProps.add(new String[]{key, props.get(key)});
	    }
        }

	private  static boolean containsRegExp(String patternString) {
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

	public  String getDbProp(String colId, String prop, String dflt) {
	    String v =  CsvUtil.getDbProp(props, colId,prop,(String) null);
	    if(v!=null) return v;
	    String key   = (colId == null)
		? prop
		: colId + "." + prop;
	    for(String[]tuple:patternProps) {
		if(key.matches(tuple[0])) return tuple[1];
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
            name = getDbProp( "table", "name", name);

            String label = Utils.makeLabel(name);
            label   = getDbProp( "table", "label", label);
            label   = label.replaceAll("\n", " ").replaceAll("\r", " ");
            tableId = Utils.makeLabel(name).toLowerCase().replaceAll(" ",
								     "_");
            String defaultView   = getDbProp( "table", "defaultView", (String)null);

	    String defaultOrder= (String)props.get("defaultOrder");
            tableId = getDbProp( "table", "id", tableId);

            String labels = getDbProp( "table", "labelColumns",
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


	    String tableAttrs = XmlUtil.attrs("id", tableId, "name", label, "labelColumns", labels,
					      "icon",getDbProp( "table", "icon", "/db/database.png"));
	    String addressTemplate = getDbProp( "table", "addressTemplate", (String)null);
	    if(addressTemplate!=null)
		tableAttrs+=XmlUtil.attr("addressTemplate", addressTemplate);
	    if(defaultView!=null)
		tableAttrs+=XmlUtil.attr("defaultView", defaultView);
	    if(defaultOrder!=null)
		tableAttrs+=XmlUtil.attr("defaultOrder", defaultOrder);
            writer.println(
			   XmlUtil.openTag(
					   "table", tableAttrs));

	    String formjs = (String)props.get("table.formjs");
	    if(formjs!=null) {
		writer.println("<formjs><![CDATA[");
		if(formjs.startsWith("file:")) {
		    String jsfile = formjs.substring("file:".length());
		    if(!IO.okToReadFrom(jsfile)) {
			throw new RuntimeException("Cannot read file:" + jsfile);
		    }
		    formjs = IO.readContents(jsfile);
		}
		writer.println(formjs);
		writer.println("]]></formjs>");

	    }


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

            boolean dfltDoStats = getDbProp( "table",
						    "dostats", "false").equals("true");
            boolean dfltCanSearch = getDbProp( "table",
						      "cansearch", "true").equals("true");
            boolean dfltCanSort = getDbProp( "table",
						      "cansort", "false").equals("true");	    
            boolean dfltCanList = getDbProp( "table",
						    "canlist", "true").equals("true");
            String dfltChangeType = getDbProp( "table",
						      "changetype", "false");

            String format = getDbProp( "table", "format",
					      "yyyy-MM-dd HH:mm");
            for (int colIdx = 0; colIdx < row1.getValues().size(); colIdx++) {
                Object col   = row1.getValues().get(colIdx);
                String colId = Utils.makeLabel(col.toString());
                colId = colId.toLowerCase().replaceAll(" ",
						       "_").replaceAll("[^a-z0-9]", "_");
                colId = colId.replaceAll("_+_", "_");
                colId = colId.replaceAll("_$", "");
                colId = getDbProp( colId, "id", colId);
                label = Utils.makeLabel(colId);


                String suffix = getDbProp( colId, "suffix",(String)null);
                String help = getDbProp( colId, "help",(String)null);		
		

                label = getDbProp( colId, "label", label);
                label = label.replaceAll("\n", " ").replaceAll("\r", " ");

                if (getDbProp( colId, "skip",
                                      "false").equals("true")) {
                    continue;
                }



                boolean isNumber = isNumeric[colIdx];
                String type = getDbProp( "table", "type",
						"string");
                if (isNumber) {
                    type = "double";
                }

                StringBuilder attrs     = new StringBuilder();
                boolean       canList   = dfltCanList;
                boolean       canSearch = dfltCanSearch;
                boolean       canSort = dfltCanSort;		



                attrs.append(XmlUtil.attrs(new String[] { "name", colId }));
		if(suffix!=null)
		    attrs.append(XmlUtil.attrs(new String[] { "suffix", suffix }));
		if(help!=null)
		    attrs.append(XmlUtil.attrs(new String[] { "help", help }));		

                String placeholderMin = getDbProp( colId, "placeholderMin",(String)null);		
		if(placeholderMin!=null)
		    attrs.append(XmlUtil.attrs(new String[] { "placeholderMin", placeholderMin }));
                String placeholderMax = getDbProp( colId, "placeholderMax",(String)null);		
		if(placeholderMax!=null)
		    attrs.append(XmlUtil.attrs(new String[] { "placeholderMax", placeholderMax }));
                String placeholder = getDbProp( colId, "placeholder",(String)null);		
		if(placeholder!=null)
		    attrs.append(XmlUtil.attrs(new String[] { "placeholder", placeholder }));						
                String numberOfSearchWidgets = getDbProp( colId, "numberOfSearchWidgets", (String)null);
		if(numberOfSearchWidgets!=null) {
                    attrs.append(XmlUtil.attrs(new String[] { "numberOfSearchWidgets",
							      numberOfSearchWidgets }));

		}
                if (getDbProp( colId, "changetype",
                                      dfltChangeType).equals("true")) {
                    attrs.append(XmlUtil.attrs(new String[] { "changetype",
							      "true" }));
                }
                String size = getDbProp( colId, "size", null);
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

                type = getDbProp( colId, "type", type);
                String values = getDbProp( colId, "values",
						  null);
                String searchRows = getDbProp( colId,
						      "searchrows", "");
                String defaultsort = getDbProp( colId,
						       "defaultsort", (String) null);
                if ((defaultsort != null) && defaultsort.equals("true")) {
                    attrs.append(XmlUtil.attrs(new String[] { "defaultsort",
							      "true" }));
                    String asc = getDbProp( colId, "ascending",
						   (String) null);
                    if (asc != null) {
                        attrs.append(XmlUtil.attrs(new String[] { "ascending",
								  asc }));
                    }
                }



                canSearch = "true".equals(getDbProp( colId,
							    "cansearch", canSearch + ""));
                canSort = "true".equals(getDbProp( colId,
							    "cansort", canSort + ""));		
                canList = "true".equals(getDbProp( colId,
							  "canlist", canList + ""));
                attrs.append(XmlUtil.attrs(new String[] {
			    "type", type, "label", label, "cansearch", "" + canSearch,
			    "canlist", "" + canList
			}));
                String group = getDbProp( colId, "group", (String) null);
		if(group!=null)
                    attrs.append(XmlUtil.attrs(new String[] { "group",
							      group}));

		if(canSort) {
                    attrs.append(XmlUtil.attrs(new String[] { "cansort",
							      "true"}));
		}
                if (values != null) {
                    attrs.append(XmlUtil.attrs(new String[] { "values",
							      values }));
                }
                String lookupDB = getDbProp( colId,
						    "lookupdb", (String)null);

		if(lookupDB!=null) {
                    attrs.append(XmlUtil.attrs(new String[] { "lookupdb",
							      lookupDB}));
		}
                if (searchRows.length() > 0) {
                    attrs.append(XmlUtil.attrs(new String[] { "searchrows",
							      searchRows }));
                }
                if (type.equals("date")) {
                    attrs.append(XmlUtil.attrs(new String[] { "format",
							      getDbProp( colId, "format",
										format) }));
                }

                StringBuffer inner = new StringBuffer();
                boolean doStats = "true".equals(getDbProp(
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
						     "islabel", "value", "true" })));
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


    public static class DbProps extends Processor {

	String idPattern;
	String suffixPattern;	

        /**
         * _more_
         *
         * @param props _more_
         */
        public DbProps(String idPattern,String suffixPattern) {
	    this.idPattern = idPattern;
	    if(this.idPattern.length()>0) this.idPattern = ".*" + this.idPattern +".*";
	    this.suffixPattern = suffixPattern;
	    if(this.suffixPattern.length()>0) this.suffixPattern = ".*" + this.suffixPattern +".*";	    
        }

	

	private boolean print(String id, String suffix, String  value) {
	    if(Utils.stringDefined(idPattern)) {
		if(!id.matches(idPattern)) return false;
	    }
	    if(Utils.stringDefined(suffixPattern)) {
		if(!suffix.matches(suffixPattern)) return false;
	    }	    
	    System.out.print(id+"." + suffix +" " + value +"  ");
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
	    if(rowCnt++>0) return null;
	    boolean didone = false;
	    for(Object o: row.getValues()) {
		boolean p = false;
		String id = Utils.makeID(o.toString());
		p|= print(id,"type","enumeration");
		p|= 		print(id,"cansearch","true");
		p|= 		print(id,"canlist","true" );
		p|= 		print(id,"cansort","true");
		didone|=p;
		if(p)
		    System.out.print("\n");
	    }
	    if(!didone) {
		for(Object o: row.getValues()) {
		    String id = Utils.makeID(o.toString());
		    if(Utils.stringDefined(idPattern)) {
			if(!id.matches(idPattern)) continue;
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
         *   _more_
         *
         *   @param ctx _more_
         * @param rows _more_
         *
         *
         * @return _more_
         *   @throws Exception On badness
         */
        @Override
        public void finish(TextReader ctx) 
	    throws Exception {
	    super.finish(ctx);
            if (contains == null) {
                ctx.getWriter().print("-0");
            } else {
                for (int i = 0; i < values.size(); i++) {
                    List uniqueValues = values.get(i);
                    for (int j = 0; j < uniqueValues.size(); j++) {
                        if (j > 0) {
                            //                            ctx.getWriter().print(",");
                        }
                        ctx.getWriter().println(uniqueValues.get(j));
                    }
                }
            }
            ctx.getWriter().flush();
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
         *   @param ctx _more_
         * @param rows _more_
         *
         *
         * @return _more_
         *   @throws Exception On badness
         */
        @Override
        public void finish(TextReader ctx)
	    throws Exception {
	    super.finish(ctx);
            if (strict) {
                return;
            }

            ctx.getWriter().println("Rows:" + rowCount);
            for (Integer cnt : counts) {
                int value = uniqueCounts.get(cnt);
                ctx.getWriter().println("\tcolumns:" + cnt + " #:" + value);
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
         */
        public Logger() {}

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
                for(Object obj: row.getValues()) {
		    String name = obj.toString();
		    if (name.startsWith("#fields=")) {
			name = name.substring("#fields=".length());
		    }
		    String args = StringUtil.findPattern(name, "\\[(.*)\\]");
		    name = name.replaceAll("\\[.*\\]", "");
		    headerValues.add(name);
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
            ctx.getWriter().println("#" + cnt);
            for (int i = 0; i < values.size(); i++) {
                String label = (i < headerValues.size())
		    ? headerValues.get(i).toString()
		    : "NA";
                label = StringUtil.padLeft(label, 20);
                ctx.getWriter().println(label + ":" + values.get(i));
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
            sb.append(CsvUtil.columnsToString(row.getValues(), ","));
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

	private String dflt;
	
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
                      List<String> keys2, String dflt) {
            this.keys1   = keys1;
            this.values1 = values1;
            this.keys2   = keys2;
            this.file    = file;
	    this.dflt = dflt;
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
            List<Integer> keys1Indices = null;
	    //	    System.err.println("key:" + keys1 +" " + keys1Indices);
            BufferedReader br = new BufferedReader(
						   new InputStreamReader(
									 getInputStream(file)));
	    CsvOperator operator = null;
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
		if(operator==null) {
		    operator = new CsvOperator();
		    operator.setHeader(cols);
		    keys1Indices = operator.getIndices(reader, keys1);	    
		    values1Indices = operator.getIndices(reader, values1);
		}

                String       key  = "";
                for (int i : keys1Indices) {
                    key += cols.get(i) + "_";
                }
                Row row = new Row(cols);
                if (headerRow1 == null) {
                    headerRow1 = row;
                }
		//		System.err.println("key:" + key +" row:" + row);
                map.put(key, row);
            }
        }


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

	    if(keys2Indices==null)
		keys2Indices = getIndices(ctx, keys2);
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
            Row other = map.get(key);
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

    public static class Doit extends Processor {
	private HashSet genres = new HashSet();
	private SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy");
	private SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd");	

	public Doit() {
	}

        /**
         * @param ctx _more_
         * @param row _more_
         *
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
	    try {
		return processRowInner(ctx,row);
	    } catch(Exception exc) {
		throw new RuntimeException(exc);
	    }
	}

	private String makeTags(Object v,String tag) {
	    List<String> toks = Utils.split(v.toString(),",");
	    StringBuilder sb = new StringBuilder();
	    for(String value: toks) {
		value = value.trim();
		if(value.length()==0) continue;
		String contents  = XmlUtil.tag("attr",XmlUtil.attr("encoded","false")+XmlUtil.attr("index","1"),
					       XmlUtil.getCdata(value));
		sb.append(XmlUtil.tag("metadata",XmlUtil.attr("type","netflix_" + tag),contents));
		sb.append("\n");
	    }
	    return sb.toString();
	}


	private String makeTag(String tag, Object v) {
	    return XmlUtil.tag(tag,"",XmlUtil.getCdata(v.toString().trim()));
	}

        public Row processRowInner(TextReader ctx, Row row) throws Exception {	    

	    PrintWriter pw = ctx.getWriter();
            if (rowCnt++ == 0) {
		pw.println("<entries>");
		return row;
            }
	    if(rowCnt>50) return null;
	    String dttm = ((String)row.get(18)).trim();
	    List<String> genreToks = Utils.split((String)row.get(1),",");
	    String genre  = genreToks.size()>0?genreToks.get(0):"Film";
	    genre = genre.trim();
	    if(genre.length()==0) genre="Miscellaneous";
	    if(!genres.contains(genre)) {
		pw.print(XmlUtil.tag("entry",XmlUtil.attr("type","group")+XmlUtil.attr("id",genre)+XmlUtil.attr("name",genre),""));
		genres.add(genre);
	    }

	    pw.print("<entry " + XmlUtil.attr("type","type_netflix_movie") + XmlUtil.attr("parent",genre));
	    if(dttm.length()>0) {
		Date date = sdf.parse(dttm);
		pw.print(XmlUtil.attr("fromdate",sdf2.format(date)));
	    }
	    pw.println(">");
	    pw.print(makeTag("name",row.get(0)));
	    pw.print(makeTag("description","<snippet>" + row.get(23)+"</snippet>"));
	    pw.print(makeTag("series_or_movie",row.get(4)));
	    pw.print(makeTag("hidden_gem_score",row.get(5))); 
	    pw.print(makeTag("runtime",row.get(7))); 
	    pw.print(makeTag("view_rating",row.get(11))); 
	    pw.print(makeTag("imdb_score",row.get(12))); 
	    pw.print(makeTag("rotten_tomatoes_score",row.get(13))); 
	    pw.print(makeTag("metacritic_score",row.get(14))); 
	    pw.print(makeTag("awards_received",row.get(15))); 
	    pw.print(makeTag("awards_nominated_for",row.get(16))); 
	    pw.print(makeTag("boxoffice",row.get(17))); 
	    pw.print(makeTag("release_date",row.get(18))); 
	    pw.print(makeTag("netflix_release_date",row.get(19))); 
	    pw.print(makeTag("netflix_link",row.get(21))); 
	    pw.print(makeTag("imdb_link",row.get(22))); 
	    pw.print(makeTag("imdb_votes",row.get(24)));
	    String thumb = row.getString(25);
	    if(thumb.trim().length()>0) {
		pw.print("<metadata type=\"content.thumbnail\"><attr index=\"1\" encoded=\"false\">" +thumb +"</attr></metadata>");
	    }
	    pw.print(makeTag("poster",row.get(26)));
	    pw.print(makeTag("tmdb_trailer",row.get(27)));
	    pw.print(makeTag("trailer_site",row.get(28))); 
	    pw.print(makeTags(row.get(1),"genre"));
	    pw.print(makeTags(row.get(2),"tag"));
	    pw.print(makeTags(row.get(3),"language"));
	    pw.print(makeTags(row.get(6),"country_availability"));
	    pw.print(makeTags(row.get(10),"actor"));
	    pw.print(makeTags(row.get(20),"production_house"));
	    pw.print(makeTags(row.get(8),"director"));
	    pw.print(makeTags(row.get(9),"writer"));
	    pw.println("</entry>");
            return row;
        }

        public void finish(TextReader ctx) 
	    throws Exception {
	    super.finish(ctx);
	    ctx.getWriter().println("</entries>");
	}

    }

}
