/*
* Copyright (c) 2008-2018 Geode Systems LLC
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

package org.ramadda.data.point.text;


import org.ramadda.data.point.*;




import org.ramadda.data.record.*;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Station;
import org.ramadda.util.Utils;
import org.ramadda.util.XlsUtil;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.StringUtil;

import java.awt.*;
import java.awt.image.*;

import java.io.*;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.*;


/**
 *
 */
public abstract class TextFile extends PointFile {

    /** _more_ */
    static int cnt = 0;

    /** _more_ */
    int mycnt = cnt++;

    /** _more_ */
    public static final String PROP_FIELDS = "fields";


    /** _more_ */
    public static final String TYPE_STRING = "string";

    /** _more_ */
    public static final String TYPE_DATE = "date";

    /** _more_ */
    public static final String DFLT_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm Z";

    /** _more_ */
    public static final String PROP_SKIPLINES = "skiplines";

    /** _more_ */
    public static final String PROP_DATEFORMAT = "dateformat";

    /** _more_ */
    public static final String PROP_HEADER_DELIMITER = "header.delimiter";

    /** _more_ */
    public static final String PROP_HEADER_STANDARD = "header.standard";

    /** _more_ */
    public static final String PROP_DELIMITER = "delimiter";

    /** _more_ */
    protected String firstDataLine = null;



    /** _more_ */
    private List<String> headerLines = new ArrayList<String>();



    /** _more_ */
    private boolean headerStandard = false;

    /** _more_ */
    String commentLineStart = null;


    /**
     * _more_
     */
    public TextFile() {}

    /**
     * ctor
     *
     *
     * @param filename _more_
     *
     * @throws IOException _more_
     */
    public TextFile(String filename) throws IOException {
        super(filename);
    }

    /**
     * _more_
     *
     * @param filename _more_
     * @param properties _more_
     *
     * @throws IOException _more_
     */
    public TextFile(String filename, Hashtable properties)
            throws IOException {
        super(filename, properties);
    }



    /**
     * _more_
     *
     * @param buffered _more_
     *
     * @return _more_
     *
     * @throws IOException _more_
     */
    public RecordIO doMakeInputIO(boolean buffered) throws IOException {
        String file = getFilename();
        if (file.endsWith(".xls")) {
            return new RecordIO(
                new BufferedReader(new StringReader(XlsUtil.xlsToCsv(file))));

        }

        return super.doMakeInputIO(buffered);
    }


    /** _more_ */
    private static Hashtable<String, String> fieldsMap =
        new Hashtable<String, String>();

    /**
     * _more_
     *
     * @param path _more_
     *
     * @return _more_
     *
     * @throws IOException _more_
     */
    public String getFieldsFileContents(String path) throws IOException {
        String fields = fieldsMap.get(path);
        if (fields == null) {
            fields = IOUtil.readContents(path, getClass()).trim();
            fields = fields.replaceAll("\n", " ");
            fieldsMap.put(path, fields);
        }

        return fields;
    }

    /**
     * _more_
     *
     * @return _more_
     *
     * @throws IOException _more_
     */
    public String getFieldsFileContents() throws IOException {
        String path = getClass().getCanonicalName();
        path = path.replaceAll("\\.", "/");
        path = "/" + path + ".fields.txt";

        //        System.err.println ("path:" + path);
        return getFieldsFileContents(path);
    }


    /**
     * _more_
     *
     * @param visitInfo _more_
     *
     * @return _more_
     */
    public int getSkipLines(VisitInfo visitInfo) {
        int skipLines = Integer.parseInt(getProperty(PROP_SKIPLINES, "0"));

        return skipLines;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getHeaderDelimiter() {
        return getProperty(PROP_HEADER_DELIMITER, (String) null);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isHeaderStandard() {
        return headerStandard;
    }

    /**
     * _more_
     *
     * @param v _more_
     */
    public void setIsHeaderStandard(boolean v) {
        headerStandard = v;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean readHeader() {
        return false;
    }


    /**
     * _more_
     *
     * @param recordIO _more_
     *
     * @return _more_
     *
     *
     * @throws Exception _more_
     *
     */
    @Override
    public RecordIO readHeader(RecordIO recordIO) throws Exception {
        return recordIO;
    }

    /**
     * _more_
     *
     * @param recordIO _more_
     *
     * @throws Exception _more_
     */
    public void writeHeader(RecordIO recordIO) throws Exception {
        for (String line : headerLines) {
            recordIO.getPrintWriter().println(line);
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List<String> getHeaderLines() {
        return headerLines;
    }

    /**
     * _more_
     *
     * @param record _more_
     * @param field _more_
     * @param tok _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public double parseValue(TextRecord record, RecordField field, String tok)
            throws Exception {
        try {
            return Double.parseDouble(tok);
        } catch (NumberFormatException nfe1) {
            if (tok.startsWith("(") && tok.endsWith(")")) {
                tok = tok.substring(1, tok.length()).substring(0,
                                    tok.length() - 2);

                return -Double.parseDouble(tok);
            }

            if (tok.endsWith("%")) {
                tok = tok.substring(0, tok.length() - 1);
            } else if (tok.startsWith("$")) {
                tok = tok.substring(1);
            }
            tok = tok.replaceAll(",", "");
            try {
                return Double.parseDouble(tok);
            } catch (NumberFormatException nfe2) {
                throw new IllegalArgumentException(
                    "Bad number format for field:" + field.getName()
                    + " value=" + tok);
            }
        }
    }



    /**
     * _more_
     *
     * @param siteId _more_
     * @param record _more_
     *
     * @return _more_
     */
    public Station setLocation(String siteId, TextRecord record) {
        Station station = setLocation(siteId);
        if (station != null) {
            record.setLocation(station);
        }

        return station;
    }



    /**
     * _more_
     *
     * @param lines _more_
     */
    public void setHeaderLines(List<String> lines) {
        headerLines = lines;
    }

    /**
     * _more_
     */
    public void initAfterClone() {
        super.initAfterClone();
        headerLines = new ArrayList<String>();
    }

    /**
     * _more_
     *
     * @param line _more_
     *
     * @return _more_
     */
    public boolean isHeaderLine(String line) {
        return line.startsWith("#");
    }

    /**
     * _more_
     *
     * @param visitInfo _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public VisitInfo prepareToVisit(VisitInfo visitInfo) throws Exception {

        boolean haveReadHeader  = headerLines.size() > 0;
        String  headerDelimiter = getHeaderDelimiter();
        boolean isStandard      = getProperty(PROP_HEADER_STANDARD, false);

        boolean firstLineFields = getProperty("firstLineDefinesFields",
                                      false);
        if (firstLineFields) {
            int    fieldRow = Integer.parseInt(getProperty("fieldRow", "1"));
            String line     = null;
            while (true) {
                line = visitInfo.getRecordIO().readLine();
                fieldRow--;
                if (fieldRow <= 0) {
                    break;
                }
            }

            if (line != null) {
                List<String> toks    = Utils.tokenizeColumns(line, ",");
                List<String> cleaned = new ArrayList<String>();
                //                System.err.println ("\nline:" + line);
                boolean didDate = false;
                for (int tokIdx = 0; tokIdx < toks.size(); tokIdx++) {
                    String tok = toks.get(tokIdx);
                    tok = tok.replaceAll("\"", "");
                    String name = tok;
                    String id = tok.trim().replaceAll(" ",
                                    "_").replaceAll(",", "_");
                    id = id.toLowerCase();

                    StringBuilder attrs = new StringBuilder();

                    name = name.replaceAll(",", "&#44;");
                    attrs.append(attrLabel(name));
                    boolean isDate =
                        id.matches(
                            "^(week_ended|date|month|year|as_of|end_date|per_end_date|obs_date|quarter|time)$");
                    //                    System.err.println("id:" + id +" isDate:" + isDate);
                    if (isDate) {
                        if ( !didDate) {

                            String format = id.equals("time")
                                            ? "yyyy-MM-dd'T'HH:mm:ss"
                                            : "yyyy-MM-dd";
                            attrs.append(attrType(TYPE_DATE));
                            attrs.append(attrFormat(format));
                            didDate = true;
                        } else {
                            attrs.append(attrType(TYPE_STRING));
                        }
                    } else {
                        attrs.append(attrChartable());
                    }
                    cleaned.add(id + "[" + attrs + "]");
                }
                String f = makeFields(cleaned);

                //                System.err.println("fields:" + f);
                putProperty(PROP_FIELDS, f);
            }
        } else if (headerDelimiter != null) {
            boolean starts = headerDelimiter.startsWith("starts:");
            if (starts) {
                headerDelimiter =
                    headerDelimiter.substring("starts:".length());
            }
            while (true) {
                String line = visitInfo.getRecordIO().readLine();
                if (line == null) {
                    break;
                }
                line = line.trim();
                if (starts && line.startsWith(headerDelimiter)) {
                    break;
                }
                if (line.equals(headerDelimiter)) {
                    break;
                }
                if ( !haveReadHeader) {
                    headerLines.add(line);
                }
                //Don't go crazy if we miss the header delimiter
                if (headerLines.size() > 500) {
                    throw new IllegalStateException(
                        "Reading way too many header lines");
                }
            }
        } else if (isStandard || isHeaderStandard()) {
            while (true) {
                String line = visitInfo.getRecordIO().readLine();
                if (line == null) {
                    break;
                }
                line = line.trim();
                if (line.length() == 0) {
                    break;
                }
                if ( !isHeaderLine(line)) {
                    visitInfo.getRecordIO().putBackLine(line);

                    break;
                }
                if ( !haveReadHeader) {
                    headerLines.add(line);
                    line = line.substring(1);
                    int idx = line.indexOf("=");
                    if (idx >= 0) {
                        List<String> toks = StringUtil.splitUpTo(line, "=",
                                                2);
                        putProperty(toks.get(0), toks.get(1));
                    }
                }
            }
        } else {
            int skipCnt = getSkipLines(visitInfo);
            commentLineStart = getProperty("commentLineStart", null);
            //            System.err.println("Skip:" + skipCnt +" " + commentLineStart);
            for (int i = 0; i < skipCnt; ) {
                String line = visitInfo.getRecordIO().readLine();
                //                System.err.println("Line:" + line);
                if ((commentLineStart != null)
                        && line.startsWith(commentLineStart)) {
                    //                    System.err.println("is comment line:" + line);
                    continue;
                }
                if ( !haveReadHeader) {
                    headerLines.add(line);
                }
                i++;
            }
            if (headerLines.size() != skipCnt) {
                throw new IllegalArgumentException(
                    "Bad number of header lines:" + headerLines.size());
            }
        }


        initProperties();

        return visitInfo;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public String getTextHeader() {
        if (getHeaderLines().size() == 0) {
            doQuickVisit();
        }
        StringBuffer textHeader = new StringBuffer();
        for (String line : getHeaderLines()) {
            List<String> toks = StringUtil.splitUpTo(line, "=", 2);
            if (toks.size() == 2) {
                if (toks.get(0).trim().indexOf(" ") < 0) {
                    continue;
                }
            }
            textHeader.append(line);
            textHeader.append("\n");
        }

        return textHeader.toString();
    }



    /**
     * _more_
     *
     * @param fields _more_
     */
    public void putFields(String[] fields) {
        String f = makeFields(fields);
        putProperty(PROP_FIELDS, f);
    }


    /**
     * _more_
     *
     * @param line _more_
     *
     * @return _more_
     */
    public boolean isLineValidData(String line) {
        if ((commentLineStart != null) && line.startsWith(commentLineStart)) {
            return false;
        }

        return true;
    }


    /**
     * _more_
     *
     * @param fields _more_
     *
     * @return _more_
     */
    public String makeFields(String[] fields) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < fields.length; i++) {
            if (fields[i] == null) {
                continue;
            }
            if (i > 0) {
                sb.append(",");
            }
            sb.append(fields[i]);
        }

        return sb.toString();
    }

    /**
     * _more_
     *
     * @param fields _more_
     *
     * @return _more_
     */
    public String makeFields(List<String> fields) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < fields.size(); i++) {
            if (fields.get(i) == null) {
                continue;
            }
            if (i > 0) {
                sb.append(",");
            }
            sb.append(fields.get(i));
        }

        return sb.toString();
    }



    /**
     * _more_
     *
     * @param id _more_
     * @param attrs _more_
     *
     * @return _more_
     */
    public static String makeField(String id, String... attrs) {
        StringBuffer asb = new StringBuffer();
        for (String attr : attrs) {
            asb.append(attr);
            asb.append(" ");
        }

        return id + "[" + asb + "]";
    }

    /**
     * _more_
     *
     * @param index _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public PointRecord getRecord(int index) throws Exception {
        throw new IllegalArgumentException("Not implemented");
    }

    /**
     * _more_
     *
     * @param visitInfo _more_
     * @param record _more_
     * @param howMany _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public boolean skip(VisitInfo visitInfo, Record record, int howMany)
            throws Exception {
        TextRecord textRecord = (TextRecord) record;
        for (int i = 0; i < howMany; i++) {
            String line = textRecord.readNextLine(visitInfo.getRecordIO());
            if (line == null) {
                return false;
            }
        }

        return true;
    }

    /**
     * _more_
     *
     * @param d _more_
     *
     * @return _more_
     */
    public static String attrValue(double d) {
        return attrValue("" + d);
    }

    /**
     * _more_
     *
     * @param d _more_
     *
     * @return _more_
     */
    public static String attrSortOrder(int d) {
        return HtmlUtils.attr(ATTR_SORTORDER, "" + d);
    }


    /**
     * _more_
     *
     * @param v _more_
     *
     * @return _more_
     */
    public static String attrValue(String v) {
        return HtmlUtils.attr(ATTR_VALUE, v);
    }


    /**
     * _more_
     *
     * @param pattern _more_
     *
     * @return _more_
     */
    public static String attrPattern(String pattern) {
        return HtmlUtils.attr(ATTR_PATTERN, pattern);
    }


    /**
     * _more_
     *
     * @param n _more_
     * @param v _more_
     *
     * @return _more_
     */
    public static String attr(String n, String v) {
        return HtmlUtils.attr(n, v);
    }

    /**
     * _more_
     *
     * @param v _more_
     *
     * @return _more_
     */
    public static String attrType(String v) {
        return HtmlUtils.attr(ATTR_TYPE, v);
    }


    /**
     * _more_
     *
     * @param v _more_
     *
     * @return _more_
     */
    public static String attrWidth(int v) {
        return HtmlUtils.attr("width", "" + v);
    }

    /**
     * _more_
     *
     * @param v _more_
     *
     * @return _more_
     */
    public static String attrLabel(String v) {
        v = v.replaceAll(",", " ");

        return HtmlUtils.attr(ATTR_LABEL, v);
    }

    /**
     * _more_
     *
     * @param v _more_
     *
     * @return _more_
     */
    public static String attrMissing(double v) {
        return HtmlUtils.attr(ATTR_MISSING, "" + v);
    }

    /**
     * _more_
     *
     * @param v _more_
     *
     * @return _more_
     */
    public static String attrMissing(String v) {
        return HtmlUtils.attr(ATTR_MISSING, v);
    }

    /**
     * _more_
     *
     * @param v _more_
     *
     * @return _more_
     */
    public static String attrFormat(String v) {
        return HtmlUtils.attr(ATTR_FORMAT, v);
    }

    /**
     * _more_
     *
     * @param v _more_
     *
     * @return _more_
     */
    public static String attrUnit(String v) {
        return HtmlUtils.attr(ATTR_UNIT, v);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public static String attrChartable() {
        return HtmlUtils.attr(ATTR_CHARTABLE, "true");
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public static String attrSearchable() {
        return HtmlUtils.attr(ATTR_SEARCHABLE, "true");
    }
}
