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


import org.json.*;


import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Json;

import org.ramadda.util.Utils;

import org.ramadda.util.XlsUtil;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import ucar.unidata.xml.XmlUtil;

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

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 *
 * @author Jeff McWhirter
 */

public class CsvUtil {


    /** a hack for debugging */
    private String theLine;

    /** _more_ */
    private List<String> args;

    /** _more_ */
    private OutputStream outputStream = System.out;

    /** _more_ */
    private InputStream inputStream;

    /** _more_ */
    private File destDir = new File(".");

    /** _more_ */
    private TextReader textReader;

    /** _more_ */
    private File outputFile;

    /** _more_ */
    private boolean installPlugin = false;

    /** _more_ */
    private boolean nukeDb = false;

    /** _more_ */
    private Processor.DbXml dbXml;

    /** _more_ */
    private boolean okToRun = true;

    /** _more_ */
    private boolean verbose = false;

    /** _more_ */
    private int rawLines = 0;

    /** _more_ */
    private String delimiter;

    /** _more_ */
    private String comment;

    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public CsvUtil(String[] args) throws Exception {
        this.args = new ArrayList<String>();
        for (String arg : args) {
            this.args.add(arg);
        }
    }

    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public CsvUtil(List<String> args) throws Exception {
        this.args = args;
    }

    /**
     * _more_
     *
     * @param args _more_
     * @param destDir _more_
     *
     * @throws Exception _more_
     */
    public CsvUtil(List<String> args, File destDir) throws Exception {
        this(args);
        this.destDir = destDir;
    }


    /**
     * _more_
     *
     * @param args _more_
     * @param out _more_
     * @param destDir _more_
     *
     * @throws Exception _more_
     */
    public CsvUtil(List<String> args, OutputStream out, File destDir)
            throws Exception {
        this(args, destDir);
        this.outputStream = out;
    }

    /**
     * _more_
     *
     * @param args _more_
     * @param out _more_
     * @param destDir _more_
     *
     * @throws Exception _more_
     */
    public CsvUtil(String[] args, OutputStream out, File destDir)
            throws Exception {
        this(args);
        this.destDir      = destDir;
        this.outputStream = out;
    }


    /**
     * _more_
     *
     * @param csvUtil _more_
     */
    public void initWith(CsvUtil csvUtil) {
        this.comment = csvUtil.comment;
        //        this.delimiter = csvUtil.delimiter;
    }

    /**
     * _more_
     *
     * @param inputStream _more_
     */
    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getOkToRun() {
        return okToRun;
    }

    /**
     * _more_
     */
    public void stopRunning() {
        okToRun = false;
        if (textReader != null) {
            textReader.stopRunning();
        }
    }


    /**
     * Set the DestDir property.
     *
     * @param value The new value for DestDir
     */
    public void setDestDir(File value) {
        destDir = value;
    }

    /**
     * Get the DestDir property.
     *
     * @return The DestDir
     */
    public File getDestDir() {
        destDir.mkdir();

        return destDir;
    }

    /**
     * _more_
     *
     * @param file _more_
     */
    public void setOutputFile(File file) {
        this.outputFile = file;
        if (file != null) {
            this.outputStream = null;
        }
    }

    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public OutputStream getOutputStream() throws Exception {
        if (this.outputStream == null) {
            this.outputStream = new FileOutputStream(this.outputFile);
        }

        return this.outputStream;
    }

    /**
     * _more_
     *
     * @param out _more_
     */
    public void setOutputStream(OutputStream out) {
        this.outputStream = out;
    }

    /**
     * _more_
     *
     * @param files _more_
     *
     * @throws Exception _more_
     */
    public void run(List<String> files) throws Exception {

        if (files == null) {
            files = new ArrayList<String>();
        }
        boolean      doConcat = false;
        boolean      doHeader = false;
        boolean      doRaw    = false;
        Hashtable    dbProps  = new Hashtable<String, String>();
        boolean      doPoint  = false;
        boolean      htmlPattern;
        String       iterateColumn = null;
        List<String> iterateValues = new ArrayList<String>();

        String       prepend       = null;
        textReader = new TextReader(destDir, outputFile, outputStream);

        List<String> extra = new ArrayList<String>();
        for (int i = 0; i < args.size(); i++) {
            String arg = args.get(i);
            if (arg.equals("-help")) {
                usage("", null, false, false);

                return;
            }
            if (arg.equals("-genhelp")) {
                genHelp();

                return;
            }
            if (arg.equals("-helpraw")) {
                usage("", null, false, true);

                return;
            }
            if (arg.startsWith("-help:")) {
                usage("", arg.substring("-help:".length()), false, false);

                return;
            }
            if (arg.equals("-alldata")) {
                textReader.setAllData(true);

                continue;
            }

            if (arg.equals("-verbose")) {
                verbose = true;

                continue;
            }


            if (arg.equals("-cat")) {
                doConcat = true;

                continue;
            }

            if (arg.equals("-raw")) {
                doRaw = true;

                continue;
            }

            if (arg.startsWith("-header")) {
                textReader.setFirstRow(
                    new Row(StringUtil.split(args.get(++i), ",")));

                continue;
            }

            if (arg.startsWith("-iter")) {
                iterateColumn = args.get(++i);
                iterateValues = StringUtil.split(args.get(++i), ",");

                continue;
            }
            extra.add(arg);
        }

        List<List<Row>> rows = new ArrayList<List<Row>>();
        if ( !parseArgs(extra, textReader, files, rows)) {
            return;
        }
        if (rows.size() > 0) {
            textReader.setRows(rows.get(0));
        }

        if (doConcat) {
            concat(files, getOutputStream());
        } else if (doHeader) {
            header(files, textReader, doPoint);
        } else if (doRaw) {
            raw(files, textReader);
        } else {
            if (files.size() == 0) {
                files.add("stdin");
            }
            Filter.PatternFilter iteratePattern = null;
            if (iterateColumn == null) {
                iterateValues.add("dummy");
            } else {
                iteratePattern = new Filter.PatternFilter(iterateColumn, "");
                textReader.getFilter().addFilter(iteratePattern);
            }
            for (int i = 0; i < iterateValues.size(); i++) {
                String pattern = iterateValues.get(i);
                if (iteratePattern != null) {
                    iteratePattern.setPattern(pattern);
                }
                for (String file : files) {
                    //                    System.err.println("FILE:" + file);
                    textReader.getProcessor().reset();
                    InputStream is      = null;
                    boolean     closeIS = true;
                    if (this.inputStream != null) {
                        is = this.inputStream;
                    } else {
                        if (file.equals("stdin")) {
                            closeIS = false;
                            is      = System.in;
                        } else {
                            is = getInputStream(file);
                        }
                    }
                    process(textReader.cloneMe(is, file, outputFile,
                            outputStream));
                    if (closeIS) {
                        IOUtil.close(is);
                    }
                    if (this.inputStream != null) {
                        break;
                    }
                }
            }
        }
    }


    /**
     * Merge each row in the given files out. e.g., if file1 has
     *  1,2,3
     *  4,5,6
     * and file2 has
     * 8,9,10
     * 11,12,13
     * the result would be
     * 1,2,3,8,9,10
     * 4,5,6,11,12,13
     * Gotta figure out how to handle different numbers of rows
     *
     * @param files files
     * @param out output
     *
     * @throws Exception On badness
     */
    public void concat(List<String> files, OutputStream out)
            throws Exception {
        PrintWriter          writer    = new PrintWriter(out);
        String               delimiter = ",";
        List<BufferedReader> readers   = new ArrayList<BufferedReader>();
        for (String file : files) {
            readers.add(
                new BufferedReader(
                    new InputStreamReader(new FileInputStream(file))));
        }
        while (true) {
            int nullCnt = 0;
            for (int i = 0; i < readers.size(); i++) {
                BufferedReader br   = readers.get(i);
                String         line = br.readLine();
                if (line == null) {
                    nullCnt++;

                    continue;
                }
                if (i > 0) {
                    writer.print(delimiter);
                }
                writer.print(line);
                writer.flush();
            }
            if (nullCnt == readers.size()) {
                break;
            }
            writer.println("");
        }

    }


    /**
     * _more_
     *
     * @return _more_
     */
    public List<String> getNewFiles() {
        return textReader.getFiles();
    }

    /**
     * _more_
     *
     * @param file _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public InputStream getInputStream(String file) throws Exception {
        if (file.endsWith(".xls") || file.endsWith(".xlsx")) {
            String csv = XlsUtil.xlsToCsv(file);

            return new BufferedInputStream(
                new ByteArrayInputStream(csv.getBytes()));
        } else {
            try {
                return new BufferedInputStream(new FileInputStream(file));
            } catch (Exception exc) {
                System.err.println("Error opening file:" + file);

                throw exc;
            }
        }
    }

    /**
     * _more_
     *
     * @param files _more_
     * @param info _more_
     * @param asPoint _more_
     *
     * @throws Exception _more_
     */
    public void header(List<String> files, TextReader info, boolean asPoint)
            throws Exception {
        PrintWriter   writer    = info.getWriter();
        List<Integer> widths    = info.getWidths();
        String        delimiter = info.getDelimiter();
        if ((widths == null) && (delimiter == null)) {
            delimiter = ",";
        }
        List<BufferedReader> readers = new ArrayList<BufferedReader>();
        for (String file : files) {
            readers.add(
                new BufferedReader(
                    new InputStreamReader(getInputStream(file))));
        }
        for (BufferedReader br : readers) {
            String line = new TextReader(br).readLine();
            if (line == null) {
                continue;
            }
            List<String> cols = (widths != null)
                                ? Utils.tokenizeColumns(line, widths)
                                : Utils.tokenizeColumns(line, delimiter);
            if (asPoint) {
                writer.println("skiplines=1");
                writer.print("fields=");
            }
            for (int i = 0; i < cols.size(); i++) {
                String col = cols.get(i).trim();
                col = col.replaceAll("\n", " ");
                if (asPoint) {
                    if (i > 0) {
                        writer.print(", ");
                    }
                    String label =
                        Utils.makeLabel(col.replaceAll("\\([^\\)]+\\)", ""));
                    String unit = StringUtil.findPattern(col,
                                      ".*?\\(([^\\)]+)\\).*");
                    //                    System.err.println ("COL:" + col +" unit: " + unit);
                    StringBuffer attrs = new StringBuffer();
                    attrs.append("label=\"" + label + "\" ");
                    if (unit != null) {
                        attrs.append("unit=\"" + unit + "\" ");

                    }
                    String id = label.replaceAll(
                                    "\\([^\\)]+\\)", "").replaceAll(
                                    "-", "_").trim().toLowerCase().replaceAll(
                                    " ", "_").replaceAll(":", "_");
                    id = id.replaceAll("/+", "_");
                    id = id.replaceAll("\\.", "_");
                    id = id.replaceAll("_+_", "_");
                    id = id.replaceAll("_+$", "");
                    id = id.replaceAll("^_+", "");
                    if (id.indexOf("date") >= 0) {
                        attrs.append("type=\"date\" format=\"\" ");
                    }

                    writer.print(id + "[" + attrs + "] ");
                } else {
                    writer.println("#" + i + " " + col);
                }
            }
            if (asPoint) {
                writer.println("");
            }
        }
        System.err.println("CsvUtil.done");
        writer.flush();
        writer.close();
    }
    //            System.err.println("files:" + files + " os:" + outputStream.getClass());

    /**
     * _more_
     *
     * @param file _more_
     * @param props _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Row> tokenizeHtml(String file,
                                  Hashtable<String, String> props)
            throws Exception {

        int    skip  = 0;
        String skips = props.get("skip");
        if (skips != null) {
            skip = Integer.parseInt(skips);
        }
        String pattern       = props.get("pattern");
        String skipAttr      = props.get("skipAttr");
        String removePattern = props.get("removePattern");
        if (removePattern != null) {
            removePattern = removePattern.replaceAll("_leftparen_",
                    "\\\\(").replaceAll("_rightparen_", "\\\\)");
            removePattern = removePattern.replaceAll("_leftbracket_",
                    "\\\\[").replaceAll("_rightbracket_", "\\\\]");
            removePattern = removePattern.replaceAll("_dot_", "\\\\.");
        }
        String removePattern2 = props.get("removePattern2");
        if (removePattern2 != null) {
            removePattern2 = removePattern2.replaceAll("_leftparen_",
                    "\\\\(").replaceAll("_rightparen_", "\\\\)");
            removePattern2 = removePattern2.replaceAll("_leftbracket_",
                    "\\\\[").replaceAll("_rightbracket_", "\\\\]");
            removePattern2 = removePattern2.replaceAll("_dot_", "\\\\.");
        }


        boolean removeEntity = Misc.equals(props.get("removeEntity"), "true");

        Pattern attrPattern  = null;
        if (skipAttr != null) {
            skipAttr    = skipAttr.replaceAll("_quote_", "\"");
            attrPattern = Pattern.compile(skipAttr, Pattern.MULTILINE);
        }
        //        System.out.println(skipAttr);
        boolean   debug = false;
        List<Row> rows  = new ArrayList<Row>();
        String    s     = IOUtil.readContents(file);
        //        System.err.println("HTML:" + file);
        //        System.out.println("TABLE:" + s);

        String[] toks;
        if (Utils.stringDefined(pattern)) {
            int idx = s.indexOf(pattern);
            if (idx < 0) {
                return rows;
            }
            s = s.substring(idx + pattern.length());
        }

        while (true) {
            toks = Utils.tokenizeChunk(s, "<table", "</table");
            if (toks == null) {
                break;
            }
            String table = toks[0];
            s = toks[1];
            if (skip > 0) {
                skip--;

                continue;
            }
            if (debug) {
                System.out.println("table");
            }
            while (true) {
                toks = Utils.tokenizeChunk(table, "<tr", "</tr");
                if (toks == null) {
                    break;
                }
                String tr = toks[0];
                table = toks[1];
                if (debug) {
                    System.out.println("\trow: " + tr);
                }
                Row row = new Row();
                rows.add(row);
                while (true) {
                    toks = Utils.tokenizeChunk(tr, "<td", "</td");
                    if (toks == null) {
                        break;
                    }
                    String td = toks[0];
                    tr = toks[1];
                    int idx = td.indexOf(">");
                    if (idx < 0) {
                        //                        System.out.println("return-2");
                        //                        return rows;
                    }
                    if ((attrPattern != null) && (idx >= 0)) {
                        String attrs = td.substring(0, idx).toLowerCase();
                        if (attrPattern.matcher(attrs).find()) {
                            System.out.println("skipping:"
                                    + td.replaceAll("\n", " "));

                            continue;
                        }
                        //                        System.out.println("not skipping:" +td );
                    }
                    td = td.substring(idx + 1);
                    td = StringUtil.stripTags(td);
                    if (removeEntity) {
                        td = td.replaceAll("&[^;]+;", "");
                    } else {
                        td = HtmlUtils.unescapeHtml3(td);
                    }
                    if (removePattern != null) {
                        td = td.replaceAll(removePattern, "");
                    }
                    if (removePattern2 != null) {
                        td = td.replaceAll(removePattern2, "");
                    }
                    td = td.replaceAll("&nbsp;", " ");
                    td = td.replaceAll("&quot;", "\"");
                    td = td.replaceAll("&lt;", "<");
                    td = td.replaceAll("&gt;", ">");
                    td = td.replaceAll("\n", " ");
                    row.insert(td.trim());
                    if (debug) {
                        System.out.println("\t\ttd:" + td);
                    }
                }
            }

            break;
        }

        return rows;

    }



    /**
     * _more_
     *
     * @param file _more_
     * @param props _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Row> tokenizeJson(String file,
                                  Hashtable<String, String> props)
            throws Exception {
        List<Row>    rows  = new ArrayList<Row>();
        String       s     = IOUtil.readContents(file);
        JSONArray    array = new JSONArray(s);
        List<String> names = null;
        for (int i = 0; i < array.length(); i++) {
            JSONObject jrow = array.getJSONObject(i);
            if (names == null) {
                String[] tmp = JSONObject.getNames(jrow);
                Row      row = new Row();
                rows.add(row);
                names = new ArrayList<String>();
                for (String name : tmp) {
                    Object obj = jrow.opt(name);
                    if (obj != null) {
                        if ((obj instanceof JSONObject)
                                || (obj instanceof JSONArray)) {
                            continue;
                        }
                    }
                    names.add(name);
                    row.add(name);
                }
            }
            Row row = new Row();
            rows.add(row);
            for (String name : names) {
                Object obj = jrow.opt(name);
                if (obj == null) {
                    obj = "";
                } else if ((obj instanceof JSONObject)
                           || (obj instanceof JSONArray)) {
                    continue;
                }
                row.add(obj.toString());
            }

        }

        return rows;

    }


    /**
     *     _more_
     *
     *     @param files _more_
     *     @param info _more_
     *
     *     @throws Exception _more_
     */
    public void raw(List<String> files, TextReader info) throws Exception {
        int         numLines = info.getMaxRows();
        PrintWriter writer   = info.getWriter();
        String      prepend  = info.getPrepend();
        for (String file : files) {
            int                  lineCnt = 0;
            List<BufferedReader> readers = new ArrayList<BufferedReader>();
            if (info.getPrepend() != null) {
                readers.add(
                    new BufferedReader(
                        new InputStreamReader(
                            new ByteArrayInputStream(
                                info.getPrepend().getBytes()))));
            }

            readers.add(
                new BufferedReader(
                    new InputStreamReader(new FileInputStream(file))));
            for (BufferedReader br : readers) {
                while (lineCnt < numLines) {
                    String line = br.readLine();
                    if (line == null) {
                        break;
                    }
                    writer.println(line);
                    lineCnt++;
                }
                br.close();
            }

        }
        writer.flush();
        writer.close();
    }






    /**
     * _more_
     *
     * @param props _more_
     * @param colId _more_
     * @param prop _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public static String getDbProp(Hashtable<String, String> props,
                                   String colId, String prop, String dflt) {
        String key   = (colId == null)
                       ? prop
                       : colId + "." + prop;
        String value = props.get("-" + key);
        if (value == null) {
            value = props.get(key);
        }
        if (value != null) {
            return value;
        }

        return dflt;
    }


    /**
     * _more_
     *
     * @param props _more_
     * @param colId _more_
     * @param prop _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public static boolean getDbProp(Hashtable<String, String> props,
                                    String colId, String prop, boolean dflt) {
        String value = getDbProp(props, colId, prop, (String) null);
        if (value == null) {
            return dflt;
        }

        return value.equals("true");
    }

    /**
     * Run through the csv file in the TextReader
     *
     * @param textReader Holds input, output, skip, delimiter, etc
     *
     * @throws Exception On badness
     */
    public void process(TextReader textReader) throws Exception {
        int       rowCnt   = 0;
        int       cnt      = 0;
        List<Row> rows     = textReader.getRows();
        Row       firstRow = textReader.getFirstRow();
        textReader.setFirstRow(null);
        if (firstRow != null) {
            processRow(textReader, firstRow, "");
            rowCnt++;
        }
        if (rows != null) {
            for (Row row : rows) {
                rowCnt++;
                if (rowCnt <= textReader.getSkip()) {
                    continue;
                }
                if ( !processRow(textReader, row, "")) {
                    break;
                }
            }
        } else {
            while (okToRun) {
                String line = textReader.readLine();
                //            System.err.println("line:" + line);
                if (line == null) {
                    break;
                }
                line = line.replaceAll("\\u000d", " ");
                if (rawLines > 0) {
                    textReader.getWriter().println(line);
                    rawLines--;

                    continue;
                }
                if (verbose) {
                    if (((++cnt) % 1000) == 0) {
                        System.err.println("processed:" + cnt);
                    }
                }
                theLine = line;
                if ( !textReader.lineOk(textReader, line)) {
                    continue;
                }

                rowCnt++;
                if (rowCnt <= textReader.getSkip()) {
                    textReader.addHeaderLine(line);

                    continue;
                }



                List<Integer> widths = textReader.getWidths();
                if ((widths == null) && (textReader.getDelimiter() == null)) {
                    String delimiter = ",";
                    //Check for the bad separator
                    int i1 = line.indexOf(",");
                    int i2 = line.indexOf("|");
                    if ((i2 >= 0) && ((i1 < 0) || (i2 < i1))) {
                        delimiter = "|";
                    }
                    //                System.err.println("CsvUtil.delimiter is null new one is:" + delimiter);
                    textReader.setDelimiter(delimiter);
                }

                if (line.length() == 0) {
                    continue;
                }
                Row row = ((widths != null)
                           ? new Row(Utils.tokenizeColumns(line, widths))
                           : new Row(line, textReader.getDelimiter()));
                if ( !processRow(textReader, row, line)) {
                    break;
                }
            }
        }

        if (okToRun) {
            if (textReader.getProcessor() != null) {
                textReader.getProcessor().finish(textReader, null);
            }
        }
        textReader.flush();
        textReader.close();
    }

    /**
     * _more_
     *
     * @param textReader _more_
     * @param row _more_
     * @param line _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private boolean processRow(TextReader textReader, Row row, String line)
            throws Exception {
        if ((textReader.getFilter() != null)
                && !textReader.getFilter().rowOk(textReader, row, line)) {
            return true;
        }


        textReader.initRow(row);
        if ((textReader.getMaxRows() >= 0)
                && (textReader.getVisitedRows() > textReader.getMaxRows())) {
            return false;
        }
        if (textReader.getProcessor() != null) {
            row = textReader.getProcessor().processRow(textReader, row, null);
            if ( !textReader.getOkToRun()) {
                return false;
            }
            if (textReader.getExtraRow() != null) {
                row = textReader.getProcessor().processRow(textReader,
                        textReader.getExtraRow(), null);
                textReader.setExtraRow(null);
            }
            if ( !textReader.getOkToRun()) {
                return false;
            }
        } else {
            textReader.getWriter().println(columnsToString(row.getValues(),
                    textReader.getOutputDelimiter()));
            textReader.getWriter().flush();
        }
        textReader.incrRow();


        return true;
    }


    /**
     * _more_
     *
     * @param cols _more_
     * @param delimiter _more_
     *
     * @return _more_
     */
    public static String columnsToString(List cols, String delimiter) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cols.size(); i++) {
            String s = cols.get(i).toString();
            if (i > 0) {
                sb.append(delimiter);
            }
            boolean needToQuote = false;
            if (s.indexOf("\n") >= 0) {
                needToQuote = true;
            } else if (s.indexOf(delimiter) >= 0) {
                needToQuote = true;
            }

            if (s.indexOf("\"") >= 0) {
                s           = s.replaceAll("\"", "\"\"");
                needToQuote = true;
            }

            if (needToQuote) {
                sb.append('"');
                sb.append(s);
                sb.append('"');
            } else {
                sb.append(s);
            }
        }

        return sb.toString();

    }


    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public static String cleanColumnValue(String s) {
        return cleanColumnValue(s, ",");
    }

    /**
     * _more_
     *
     * @param s _more_
     * @param delimiter _more_
     *
     * @return _more_
     */
    public static String cleanColumnValue(String s, String delimiter) {
        boolean needToQuote = false;
        if (s.indexOf("\n") >= 0) {
            needToQuote = true;
        } else if (s.indexOf(delimiter) >= 0) {
            needToQuote = true;
        }

        if (s.indexOf("\"") >= 0) {
            s           = s.replaceAll("\"", "\"\"");
            needToQuote = true;
        }

        if (needToQuote) {
            return '"' + s + '"';
        }

        return s;
    }

    /**
     * Class description
     *
     *
     * @version        $version$, Wed, Feb 20, '19
     * @author         Enter your name here...
     */
    public static class Cmd {

        /** _more_ */
        String cmd;

        /** _more_ */
        String args;

        /** _more_ */
        String desc;

        /**
         * _more_
         *
         * @param cmd _more_
         */
        public Cmd(String cmd) {
            this.cmd  = cmd;
            this.args = "";
            this.desc = "";
        }

        /**
         * _more_
         *
         * @param cmd _more_
         * @param args _more_
         */
        public Cmd(String cmd, String args) {
            this.cmd  = cmd;
            this.args = args;
            this.desc = "";
        }

        /**
         * _more_
         *
         * @param cmd _more_
         * @param args _more_
         * @param desc _more_
         */
        public Cmd(String cmd, String args, String desc) {
            this.cmd  = cmd;
            this.args = args;
            this.desc = desc;
        }

        /**
         * _more_
         *
         * @param s _more_
         *
         * @return _more_
         */
        public boolean match(String s) {
            return cmd.indexOf(s) >= 0;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public String getLine() {
            return cmd + " " + args + " " + desc;
        }

    }

    /** _more_ */
    private static final Cmd[] commands = {
        new Cmd("-help", "", "(print this help)"),
        new Cmd("-help:<topic search string>", "",
                "(print help that matches topic)"),
        new Cmd(
            "-columns", "<e.g., 0,1,2,7-10,12>",
            "(A comma separated list of columns #s or column range, 0-based. Extract the given columns)"),
        new Cmd("-skip", "<how many lines to skip>"),
        new Cmd("-start", "<start pattern>"),
        new Cmd("-stop", "<stop pattern>"),
        new Cmd("-min", "<min # columns>"),
        new Cmd("-max", "<max # columns>"),
        new Cmd("-rawlines", "<how many lines to pass through unprocesed>"),
        new Cmd("-cut", "<one or more rows. -1 to the end>"),
        new Cmd("-mergerows", "<2 or more rows> <delimiter> <close>"),
        new Cmd("-include", "<one or more rows, -1 to the end>"),
        new Cmd("-pattern", "<col #> <regexp pattern>",
                "(extract rows that match the pattern)"),
        new Cmd("-notpattern", "<col #> <regexp pattern>",
                "(extract rows that don't match the pattern)"),
        new Cmd("<column>=~<value>", "", "(same as -pattern)"),
        new Cmd("<-gt|-ge|-lt|-le>", "<col #> <value>",
                "(extract rows that pass the expression)"),
        new Cmd("-decimate", "<# of start rows to include> <skip factor>",
                "(only include every <skip factor> row)"),
        new Cmd("-countvalue", "<col #> <count>"),
        new Cmd("-copy", "<col #> <name>"),
        new Cmd("-delete", "<col #>", "(remove the columns)"),
        new Cmd("-insert", "<col #> <value>", "(insert a new column value)"),
        new Cmd("-insert", "<col #> <comma separated values>"),
        new Cmd("-addcell", "<row #>  <col #>  <value>"),
        new Cmd("-deletecell", "<row #> <col #>"),
        new Cmd(
            "-macro",
            "<pattern> <template> <column label> (Look for the pattern in the header and apply the template to make a new column, template: '{1} {2} ...', use 'none' for column name for no header)"),
        new Cmd("-set", "<col #s> <row #s> <value>",
                "(write the value into the cells)"),
        new Cmd("-case", "<lower|upper|camel> <col #>",
                "(change case of column)"),
        new Cmd("-width", "<columns>  <size>",
                "(limit the string size of the columns)"),
        new Cmd(
            "-prepend", "<text>",
            "(add the text to the beginning of the file. use _nl_ to insert newlines)"),
        new Cmd("-pad", "<col #> <pad string>",
                "(pad out or cut columns to achieve the count)"),
        new Cmd("-prefix", "<col #> <prefix>", "(add prefix to column)"),
        new Cmd("-suffix", "<col #> <suffix>", "(add suffix to column)"),
        new Cmd("-change", "<col #s> <pattern> <substitution string>"),
        new Cmd("-changerow", "<row> <pattern> <substitution string>"),
        new Cmd("-formatdate",
                "<col #s> <intial date format> <target date format>"),
        new Cmd("-map", "<col #> <new columns name> <value newvalue ...>",
                "(change values in column to new values)"),
        new Cmd("-combine", "<col #s> <delimiter> <new column name>",
                "(combine columns with the delimiter. deleting columns)"),
        new Cmd("-combineinplace", "<col #s> <delimiter> <new column name>",
                "(combine columns with the delimiter.)"),
        new Cmd(
            "-html", "\"name value properties\"",
            "(parse the table in the input html file, properties: skip <tables to skip> pattern <pattern to skip to>)"),
        new Cmd("-json", "\"name value properties\"",
                "(parse the input as json)"),
        new Cmd("-concat", "<col #s>  <delimiter>",
                "(create a new column from the given columns)"),
        new Cmd("-scale", "<col #> <delta1> <scale> <delta2>",
                "(set value={value+delta1}*scale+delta2)"),
        new Cmd("-decimals", "<col #> <how many decimals to round to>", ""),
        new Cmd(
            "-operator", "<col #s>  <new col name> <operator +,-,*,/>",
            "(apply the operator to the given columns and create new one)"),
        new Cmd("-round", "<columns>", "round the values"),
        new Cmd("-sum", "<key columns> <value columns>",
                "sum values keying on name column value"),
        new Cmd(
            "-join",
            "<key columns> <value columns> <file> <key 2 columns> <value 2 columns>",
            "Join the 2 files together"),
        new Cmd("-format", "<columns> <decimal format, e.g. '##0.00'>"),
        new Cmd("-unique", "<columns>", "(pass through unique values)"),
        new Cmd("-percent", "<columns to add>"),
        new Cmd("-sort", "<column sort>"),
        new Cmd(
            "-denormalize",
            "<from csv file> <from id idx> <from value idx> <to idx>    <new col name> <mode replace add>",
            "(read the id,value from file and substitute the value in the dest file col idx)"),
        new Cmd("-explode", "<col #> ",
                "(make separate files based on value of column)"),
        new Cmd(
            "-unfurl",
            "<col to get new column header#> <value columns> <unique col>  <other columns>",
            "(make columns from data values)"),
        new Cmd("-geocode",
                "<col idx> <csv file> <name idx> <lat idx> <lon idx>"),
        new Cmd("-geocodeaddress",
                "<col indices> Latitude Longitude <prefix> <suffix> "),
        new Cmd("-geocodeaddressdb", "<col indices> <prefix> <suffix> "),
        new Cmd("-gender", "<column>"), new Cmd("-count", "", "(show count)"),
        new Cmd("-maxrows", "<max rows to print>"),
        new Cmd("-skipline", " <pattern>",
                "(skip any line that matches the pattern)"),
        new Cmd("-changeline", "<from> <to>", "(change the line)"),
        new Cmd("-prune", "<number of leading bytes to remove>"),
        new Cmd(
            "-strict", "",
            "(be strict on columns. any rows that are not the size of the other rows are dropped)"),
        new Cmd(
            "-flag", "",
            " (be strict on columns. any rows that are not the size of the other rows are shown)"),
        new Cmd("-rotate"), new Cmd("-flip"),
        new Cmd("-delimiter", "", "(specify an alternative delimiter)"),
        new Cmd("-widths", "w1,w2,...,wN", "(columns are fixed widths)"),
        new Cmd("-comment", "<string>"),
        new Cmd("-print", "", "(print to stdout)"),
        new Cmd("-raw", "", "(print the file raw)"),
        new Cmd("-record", "", " (print records)"),
        new Cmd("-printheader", "", "(print the first line)"),
        new Cmd("-pointheader", "",
                "(generate the RAMADDA point properties)"),
        new Cmd("-addheader", "<name1 value1 ... nameN valueN>",
                "(add the RAMADDA point properties)"),
        new Cmd(
            "-db", "{<props>}",
            "(generate the RAMADDA db xml from the header, props are a set of name value pairs:)\n\ttable.id <new id> table.name <new name> table.cansearch <true|false> table.canlist <true|false> table.icon <icon, e.g., /db/database.png>\n\t<column name>.id <new id for column> <column name>.label <new label>\n\t<column name>.type <string|enumeration|double|int|date>\n\t<column name>.format <yyyy MM dd HH mm ss format for dates>\n\t<column name>.canlist <true|false> <column name>.cansearch <true|false>\n\tinstall <true|false install the new db table>\n\tnukedb <true|false careful! this deletes any prior created dbs>"),
        new Cmd("-run", "<name of process directory>"),
        new Cmd("-cat", " <*.csv>", "(one or more csv files)"),
    };


    /**
     * _more_
     *
     * @param msg _more_
     * @param match _more_
     *
     * @throws Exception _more_
     */
    public void usage(String msg, String match) throws Exception {
        usage(msg, match, false, false);
    }

    /**
     * _more_
     *
     * @param msg _more_
     * @param match _more_
     * @param exact _more_
     * @param raw _more_
     *
     * @throws Exception _more_
     */
    public void usage(String msg, String match, boolean exact, boolean raw)
            throws Exception {
        PrintWriter pw = new PrintWriter(getOutputStream());
        if (msg.length() > 0) {
            pw.println(msg);
        }
        pw.println("Usage:");
        for (Cmd c : commands) {
            String cmd = c.getLine();
            if (match != null) {
                if (exact && !c.cmd.equals(match)) {
                    continue;
                }
                if ( !exact && (cmd.indexOf(match) < 0)) {
                    continue;
                }
            }
            if ( !raw) {
                cmd = cmd.replaceAll("_nl_", "\n").replaceAll("_tab_", "\n");
            }
            pw.println(cmd);
            if (raw && cmd.startsWith("-db")) {
                break;
            }
        }
        pw.flush();
    }


    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public void genHelp() throws Exception {
        PrintWriter pw = new PrintWriter(getOutputStream());

        for (Cmd c : commands) {
            pw.println("[etl {" + c.cmd + "} {" + c.args + "} {" + c.desc
                       + "}]");
        }
        pw.flush();
    }

    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception On badness
     */
    public static void main(String[] args) throws Exception {
        CsvUtil csvUtil = new CsvUtil(args);
        csvUtil.run(null);
        System.exit(0);
    }





    /**
     * _more_
     *
     * @param args _more_
     * @param i _more_
     * @param cnt _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private boolean ensureArg(List args, int i, int cnt) throws Exception {
        if (args.size() <= (i + cnt)) {
            String arg = (String) args.get(i);
            usage("Bad argument count for:" + arg, arg, true, false);

            return false;
        }

        return true;
    }

    /**
     * _more_
     *
     * @param args _more_
     * @param info _more_
     * @param files _more_
     * @param tokenizedRows _more_
     *
     *
     * @return _more_
     * @throws Exception _more_
     */
    public boolean parseArgs(List<String> args, TextReader info,
                             List<String> files,
                             List<List<Row>> tokenizedRows)
            throws Exception {


        //        System.err.println("ARGS:" + args);
        boolean            addFiles      = files.size() == 0;
        boolean            trim          = false;
        boolean            printFields   = false;

        Filter.FilterGroup subFilter     = null;
        Filter.FilterGroup filterToAddTo = null;
        //info.getFilter();

        boolean doHtml    = false;
        String  htmlProps = null;
        boolean doJson    = false;
        String  jsonProps = null;
        if (comment != null) {
            info.setComment(comment);
        }
        if (delimiter != null) {
            info.setDelimiter(delimiter);
        }


        for (int i = 0; i < args.size(); i++) {
            String arg = args.get(i);

            if (arg.equals("-html")) {
                if ( !ensureArg(args, i, 1)) {
                    return false;
                }
                doHtml    = true;
                htmlProps = args.get(++i);

                continue;
            }
            if (arg.equals("-json")) {
                if ( !ensureArg(args, i, 1)) {
                    return false;
                }
                doJson    = true;
                jsonProps = args.get(++i);

                continue;
            }


            if (arg.equals("-skip")) {
                if ( !ensureArg(args, i, 1)) {
                    return false;
                }
                info.setSkip(Integer.parseInt(args.get(++i)));

                continue;
            }


            if (arg.equals("-skipline")) {
                if ( !ensureArg(args, i, 1)) {
                    return false;
                }
                info.setSkipPattern(args.get(++i));

                continue;
            }

            if (arg.equals("-changeline")) {
                if ( !ensureArg(args, i, 2)) {
                    return false;
                }
                info.setChangeString(args.get(++i), args.get(++i));

                continue;
            }

            if (arg.equals("-maxrows")) {
                if ( !ensureArg(args, i, 1)) {
                    return false;
                }
                info.setMaxRows(Integer.parseInt(args.get(++i)));

                continue;
            }

            if (arg.equals("-prune")) {
                if ( !ensureArg(args, i, 1)) {
                    return false;
                }
                info.setPruneBytes(Integer.parseInt(args.get(++i)));

                continue;
            }

            /*
            if (arg.equals("-prefix")) {
                new PrintWriter(getOutputStream()).println(args.get(++i));
                continue;
                }*/


            if (arg.equals("-start")) {
                if ( !ensureArg(args, i, 1)) {
                    return false;
                }
                info.getFilter().addFilter(new Filter.Start(args.get(++i)));

                continue;
            }


            if (arg.equals("-stop")) {
                if ( !ensureArg(args, i, 1)) {
                    return false;
                }
                info.getFilter().addFilter(new Filter.Stop(args.get(++i)));

                continue;
            }

            if (arg.equals("-min")) {
                if ( !ensureArg(args, i, 1)) {
                    return false;
                }
                info.getFilter().addFilter(
                    new Filter.MinColumns(new Integer(args.get(++i))));

                continue;
            }

            if (arg.equals("-max")) {
                if ( !ensureArg(args, i, 1)) {
                    return false;
                }
                info.getFilter().addFilter(
                    new Filter.MaxColumns(new Integer(args.get(++i))));

                continue;
            }



            if (arg.equals("-decimate")) {
                if ( !ensureArg(args, i, 2)) {
                    return false;
                }
                int start = Integer.parseInt(args.get(++i));
                int skip  = Integer.parseInt(args.get(++i));
                if (skip > 0) {
                    info.getFilter().addFilter(new Filter.Decimate(start,
                            skip));
                }

                continue;
            }

            if (arg.equals("-db")) {
                if ( !ensureArg(args, i, 1)) {
                    return false;
                }
                Hashtable<String, String> props = parseProps(args.get(++i));
                this.installPlugin = Misc.equals(props.get("-install"),
                        "true") || Misc.equals(props.get("install"), "true");
                this.nukeDb = Misc.equals(props.get("-nukedb"), "true")
                              || Misc.equals(props.get("nukedb"), "true");
                info.getProcessor().addProcessor(dbXml =
                    new Processor.DbXml(props));

                continue;
            }


            if (arg.equals("-unfurl")) {
                if ( !ensureArg(args, i, 5)) {
                    return false;
                }
                int          idx1      = Integer.parseInt(args.get(++i));
                List<String> valueCols = getCols(args.get(++i));
                //                int          idx2 = Integer.parseInt(args.get(++i));
                int          idx3      = Integer.parseInt(args.get(++i));
                List<String> extraCols = getCols(args.get(++i));
                info.getProcessor().addProcessor(new Processor.Unfurler(idx1,
                        valueCols, idx3, extraCols));

                continue;
            }

            if (arg.equals("-sort")) {
                if ( !ensureArg(args, i, 1)) {
                    return false;
                }
                int idx = Integer.parseInt(args.get(++i));
                info.getProcessor().addProcessor(new Processor.Sorter(idx,
                        true));

                continue;
            }

            if (arg.equals("-join")) {
                if ( !ensureArg(args, i, 4)) {
                    return false;
                }
                List<String> keys1   = getCols(args.get(++i));
                List<String> values1 = getCols(args.get(++i));
                String       file    = args.get(++i);
                List<String> keys2   = getCols(args.get(++i));
                List<String> values2 = getCols(args.get(++i));

                info.getProcessor().addProcessor(new Processor.Joiner(keys1,
                        values1, file, keys2, values2));

                continue;
            }

            if (arg.equals("-sum")) {
                if ( !ensureArg(args, i, 2)) {
                    return false;
                }
                List<String> keys   = getCols(args.get(++i));
                List<String> values = getCols(args.get(++i));
                info.getProcessor().addProcessor(new Processor.Summer(keys,
                        values));

                continue;
            }


            if (arg.equals("-unique")) {
                if ( !ensureArg(args, i, 1)) {
                    return false;
                }
                List<String> toks = getCols(args.get(++i));
                info.getProcessor().addProcessor(new Filter.Unique(toks));

                continue;
            }

            if (arg.equals("-count")) {
                info.getProcessor().addProcessor(new Processor.Counter());

                continue;
            }


            if (arg.equals("-log")) {
                info.getProcessor().addProcessor(new Processor.Logger());

                continue;
            }

            if (arg.equals("-strict")) {
                info.getProcessor().addProcessor(new Processor.Counter(true));

                continue;
            }

            if (arg.equals("-flag")) {
                info.getProcessor().addProcessor(new Processor.Counter(true,
                        true));

                continue;
            }

            if (arg.equals("-sum")) {
                info.getProcessor().addProcessor(
                    new Processor.RowOperator(Processor.RowOperator.OP_SUM));

                continue;
            }

            if (arg.equals("-rotate")) {
                info.getProcessor().addProcessor(new Processor.Rotator());

                continue;
            }

            if (arg.equals("-flip")) {
                info.getProcessor().addProcessor(new Processor.Flipper());

                continue;
            }



            if (arg.equals("-rawlines")) {
                if ( !ensureArg(args, i, 1)) {
                    return false;
                }
                rawLines = Integer.parseInt(args.get(++i));

                continue;
            }


            if (arg.equals("-delimiter")) {
                if ( !ensureArg(args, i, 1)) {
                    return false;
                }
                info.setDelimiter(delimiter = args.get(++i));

                continue;
            }

            if (arg.equals("-widths")) {
                if ( !ensureArg(args, i, 1)) {
                    return false;
                }
                List<Integer> widths = new ArrayList<Integer>();
                for (String tok :
                        StringUtil.split(args.get(++i), ",", true, true)) {
                    widths.add(Integer.parseInt(tok));
                }
                info.setWidths(widths);

                continue;
            }

            if (arg.equals("-comment")) {
                if ( !ensureArg(args, i, 1)) {
                    return false;
                }
                info.setComment(comment = args.get(++i));

                continue;
            }

            if (arg.equals("-outputdelimiter")) {
                if ( !ensureArg(args, i, 1)) {
                    return false;
                }
                String s = args.get(++i);
                if (s.equals("tab")) {
                    s = "\t";
                }
                info.setOutputDelimiter(s);

                continue;
            }

            if (arg.equals("-cut") || arg.equals("-include")) {
                if ( !ensureArg(args, i, 1)) {
                    return false;
                }
                String r = args.get(++i);
                info.getFilter().addFilter(new Filter.Cutter(getNumbers(r),
                        arg.equals("-cut")));

                continue;
            }



            if (arg.equals("-max")) {
                info.getProcessor().addProcessor(
                    new Processor.RowOperator(Processor.RowOperator.OP_MAX));

                continue;
            }

            if (arg.equals("-min")) {
                info.getProcessor().addProcessor(
                    new Processor.RowOperator(Processor.RowOperator.OP_MIN));

                continue;
            }

            if (arg.equals("-average")) {
                info.getProcessor().addProcessor(
                    new Processor.RowOperator(
                        Processor.RowOperator.OP_AVERAGE));

                continue;
            }
            if (arg.equals("-processor")) {
                if ( !ensureArg(args, i, 1)) {
                    return false;
                }
                Class c = Misc.findClass(args.get(++i));
                info.getProcessor().addProcessor((Processor) c.newInstance());

                continue;
            }

            if (arg.equals("-fields")) {
                printFields = true;

                continue;
            }

            if (arg.equals("-trim")) {
                trim = true;

                continue;
            }

            if (arg.equals("-output")) {
                if ( !ensureArg(args, i, 1)) {
                    return false;
                }
                String out = args.get(++i);
                this.outputStream = new FileOutputStream(out);
                info.setWriter(new PrintWriter(this.outputStream));
                info.getProcessor().addProcessor(
                    new Processor.Printer(printFields, trim));

                continue;

            }

            if (arg.equals("-print")) {
                info.getProcessor().addProcessor(
                    new Processor.Printer(printFields, trim));

                continue;
            }

            if (arg.equals("-table")) {
                info.getProcessor().addProcessor(new Processor.Html());

                continue;
            }


            if (arg.equals("-dump")) {
                info.getProcessor().addProcessor(
                    new Processor.Printer(printFields, trim));

                continue;
            }

            if (arg.equals("-record")) {
                info.getProcessor().addProcessor(new Processor.Prettifier());

                continue;
            }

            if (arg.equals("-template")) {
                if ( !ensureArg(args, i, 1)) {
                    return false;
                }
                String template = args.get(++i);
                if (new File(template).exists()) {
                    template = IOUtil.readContents(new File(template));
                }
                info.getProcessor().addProcessor(
                    new Processor.Printer(template));

                continue;
            }

            if (arg.equals("-columns")) {
                if ( !ensureArg(args, i, 1)) {
                    return false;
                }
                List<String> cols = getCols(args.get(++i));
                info.setSelector(new Converter.ColumnSelector(cols));
                info.getProcessor().addProcessor(info.getSelector());

                continue;
            }

            if (arg.equals("-mergerows")) {
                if ( !ensureArg(args, i, 1)) {
                    return false;
                }
                String r = args.get(++i);
                info.getProcessor().addProcessor(
                    new Converter.RowMerger(
                        getNumbers(r), args.get(++i), args.get(++i)));

                continue;
            }




            /*
            if (arg.equals("-addrange")) {
                if(!ensureArg(args, i,3)) return false;
                List<String> cols = getCols(args.get(++i));
                int from = new Integer(args.get(++i)).intValue();
                int to  = new Integer(args.get(++i)).intValue();
                info.getProcessor().addProcessor(new Converter.AddRange(cols,from,to));
                continue;
            }
            */

            if (arg.equals("-addheader")) {
                if ( !ensureArg(args, i, 1)) {
                    return false;
                }
                info.getProcessor().addProcessor(
                    new Converter.HeaderMaker(parseProps(args.get(++i))));

                continue;
            }


            if (arg.equals("-printheader")) {
                info.getProcessor().addProcessor(new Converter.PrintHeader());

                continue;
            }

            if (arg.equals("-pointheader")) {
                info.getProcessor().addProcessor(
                    new Converter.PrintHeader(true));

                continue;
            }

            if (arg.equals("-prepend")) {
                if ( !ensureArg(args, i, 1)) {
                    return false;
                }
                String text = args.get(++i);
                text = text.replaceAll("_nl_", "\n");
                textReader.setPrepend(text);

                continue;
            }



            if (arg.equals("-percent")) {
                if ( !ensureArg(args, i, 1)) {
                    return false;
                }
                List<String> cols = getCols(args.get(++i));
                info.getProcessor().addProcessor(
                    new Converter.ColumnPercenter(cols));

                continue;
            }



            if (arg.equals("-sumrow")) {
                info.getProcessor().addProcessor(
                    new Converter.ColumnOperator());

                continue;
            }

            if (arg.equals("-pad")) {
                if ( !ensureArg(args, i, 2)) {
                    return false;
                }
                int    count = new Integer(args.get(++i)).intValue();
                String pad   = args.get(++i);
                info.getProcessor().addProcessor(new Converter.Padder(count,
                        pad));

                continue;
            }

            if (arg.equals("-prefix")) {
                if ( !ensureArg(args, i, 2)) {
                    return false;
                }
                List<String> cols = getCols(args.get(++i));
                String       s    = args.get(++i);
                info.getProcessor().addProcessor(new Converter.Prefixer(cols,
                        s));

                continue;
            }

            if (arg.equals("-suffix")) {
                if ( !ensureArg(args, i, 1)) {
                    return false;
                }
                List<String> cols = getCols(args.get(++i));
                String       s    = args.get(++i);
                info.getProcessor().addProcessor(new Converter.Suffixer(cols,
                        s));

                continue;
            }


            if (arg.equals("-explode")) {
                if ( !ensureArg(args, i, 1)) {
                    return false;
                }
                String col = args.get(++i);
                info.getProcessor().addProcessor(
                    new Processor.Exploder(Integer.parseInt(col)));

                continue;
            }

            if (arg.equals("-gender")) {
                if ( !ensureArg(args, i, 1)) {
                    return false;
                }
                String col = args.get(++i);

                info.getProcessor().addProcessor(
                    new Converter.Genderizer(new Integer(col)));

                continue;
            }


            if (arg.equals("-geocode")) {
                if ( !ensureArg(args, i, 5)) {
                    return false;
                }
                String col      = args.get(++i);
                String filename = args.get(++i);
                info.getProcessor().addProcessor(new Converter.Geocoder(col,
                        filename, Integer.parseInt(args.get(++i)),
                        Integer.parseInt(args.get(++i)),
                        Integer.parseInt(args.get(++i)), false));

                continue;
            }

            if (arg.equals("-geocodeaddress")) {
                if ( !ensureArg(args, i, 5)) {
                    return false;
                }
                List<String> cols   = getCols(args.get(++i));
                String       lat    = args.get(++i);
                String       lon    = args.get(++i);
                String       prefix = args.get(++i).trim();
                String       suffix = args.get(++i).trim();
                info.getProcessor().addProcessor(new Converter.Geocoder(cols,
                        lat, lon, prefix, suffix));

                continue;
            }
            if (arg.equals("-geocodeaddressdb")) {
                if ( !ensureArg(args, i, 3)) {
                    return false;
                }
                List<String> cols   = getCols(args.get(++i));
                String       prefix = args.get(++i).trim();
                String       suffix = args.get(++i).trim();
                info.getProcessor().addProcessor(new Converter.Geocoder(cols,
                        prefix, suffix, true));

                continue;
            }

            if (arg.equals("-geocodedb")) {
                if ( !ensureArg(args, i, 5)) {
                    return false;
                }
                String col      = args.get(++i);
                String filename = args.get(++i);
                info.getProcessor().addProcessor(new Converter.Geocoder(col,
                        filename, Integer.parseInt(args.get(++i)),
                        Integer.parseInt(args.get(++i)),
                        Integer.parseInt(args.get(++i)), true));

                continue;
            }

            if (arg.equals("-change")) {
                if ( !ensureArg(args, i, 3)) {
                    return false;
                }
                List<String> cols    = getCols(args.get(++i));
                String       pattern = args.get(++i);
                pattern =
                    pattern.replaceAll("_leftparen_",
                                       "\\\\(").replaceAll("_rightparen_",
                                           "\\\\)");
                pattern =
                    pattern.replaceAll("_leftbracket_",
                                       "\\\\[").replaceAll("_rightbracket_",
                                           "\\\\]");
                pattern = pattern.replaceAll("_dot_", "\\\\.");
                //                pattern = pattern.replaceAll("_leftparen_","\\\\(").replaceAll("_rightparen_","\\\\)");
                info.getProcessor().addProcessor(
                    new Converter.ColumnChanger(
                        cols, pattern, args.get(++i)));

                continue;
            }


            if (arg.equals("-changerow")) {
                if ( !ensureArg(args, i, 3)) {
                    return false;
                }
                int    row     = Integer.parseInt(args.get(++i));
                String pattern = args.get(++i);
                pattern =
                    pattern.replaceAll("_leftparen_",
                                       "\\\\(").replaceAll("_rightparen_",
                                           "\\\\)");
                pattern =
                    pattern.replaceAll("_leftbracket_",
                                       "\\\\[").replaceAll("_rightbracket_",
                                           "\\\\]");
                pattern = pattern.replaceAll("_dot_", "\\\\.");
                info.getProcessor().addProcessor(
                    new Converter.RowChanger(row, pattern, args.get(++i)));

                continue;
            }


            if (arg.equals("-debug")) {
                System.err.println("CsvUtil args:" + this.args);

                continue;
            }


            if (arg.equals("-columndebug")) {
                if ( !ensureArg(args, i, 2)) {
                    return false;
                }
                List<String> cols    = getCols(args.get(++i));
                String       pattern = args.get(++i);
                info.getProcessor().addProcessor(
                    new Converter.ColumnDebugger(cols, pattern));

                continue;
            }


            if (arg.equals("-formatdate")) {
                if ( !ensureArg(args, i, 2)) {
                    return false;
                }
                List<String> cols = getCols(args.get(++i));
                info.getProcessor().addProcessor(
                    new Converter.DateFormatter(
                        cols, args.get(++i), args.get(++i)));

                continue;
            }

            if (arg.equals("-map")) {
                if ( !ensureArg(args, i, 3)) {
                    return false;
                }
                List<String> cols = getCols(args.get(++i));
                info.getProcessor().addProcessor(
                    new Converter.ColumnMapper(
                        cols, args.get(++i),
                        Utils.parseCommandLine(args.get(++i))));

                continue;
            }


            if (arg.equals("-tcl")) {
                if ( !ensureArg(args, i, 1)) {
                    return false;
                }
                i++;
                info.getProcessor().addProcessor(
                    new Processor.TclWrapper(args.get(i)));

                continue;
            }

            if (arg.equals("-split")) {
                if ( !ensureArg(args, i, 2)) {
                    return false;
                }
                info.getProcessor().addProcessor(
                    new Converter.ColumnSplitter(
                        args.get(++i), args.get(++i)));

                continue;
            }

            if (arg.equals("-delete")) {
                if ( !ensureArg(args, i, 1)) {
                    return false;
                }
                info.getProcessor().addProcessor(
                    new Converter.ColumnDeleter(getCols(args.get(++i))));

                continue;
            }

            if (arg.equals("-insert")) {
                if ( !ensureArg(args, i, 2)) {
                    return false;
                }
                info.getProcessor().addProcessor(
                    new Converter.ColumnInserter(
                        Integer.parseInt(args.get(++i)), args.get(++i)));

                continue;
            }



            if (arg.equals("-macro")) {
                if ( !ensureArg(args, i, 3)) {
                    return false;
                }
                info.getProcessor().addProcessor(
                    new Converter.ColumnMacro(
                        args.get(++i), args.get(++i), args.get(++i)));

                continue;
            }


            if (arg.equals("-format")) {
                if ( !ensureArg(args, i, 2)) {
                    return false;
                }
                List<String> cols = getCols(args.get(++i));
                info.getProcessor().addProcessor(
                    new Converter.ColumnFormatter(cols, args.get(++i)));

                continue;

            }


            if (arg.equals("-scale")) {
                if ( !ensureArg(args, i, 3)) {
                    return false;
                }
                info.getProcessor().addProcessor(
                    new Converter.ColumnScaler(
                        args.get(++i), Double.parseDouble(args.get(++i)),
                        Double.parseDouble(args.get(++i)),
                        Double.parseDouble(args.get(++i))));

                continue;
            }


            if (arg.equals("-decimals")) {
                if ( !ensureArg(args, i, 2)) {
                    return false;
                }
                List<String> idxs = getCols(args.get(++i));
                info.getProcessor().addProcessor(new Converter.Decimals(idxs,
                        new Integer(args.get(++i))));

                continue;
            }

            if (arg.equals("-copy")) {
                if ( !ensureArg(args, i, 2)) {
                    return false;
                }
                info.getProcessor().addProcessor(
                    new Converter.ColumnCopier(args.get(++i), args.get(++i)));

                continue;
            }

            if (arg.equals("-concat")) {
                if ( !ensureArg(args, i, 2)) {
                    return false;
                }
                List<String> idxs = getCols(args.get(++i));
                info.getProcessor().addProcessor(
                    new Converter.ColumnNewer(idxs, args.get(++i)));

                continue;
            }

            if (arg.equals("-operator")) {
                if ( !ensureArg(args, i, 3)) {
                    return false;
                }
                List<String> idxs = getCols(args.get(++i));
                String       name = args.get(++i);
                String       op   = args.get(++i);
                info.getProcessor().addProcessor(
                    new Converter.ColumnMathOperator(idxs, name, op));

                continue;
            }

            if (arg.equals("-round")) {
                if ( !ensureArg(args, i, 1)) {
                    return false;
                }
                List<String> idxs = getCols(args.get(++i));
                info.getProcessor().addProcessor(
                    new Converter.ColumnRounder(idxs));

                continue;
            }

            if (arg.equals("-case")) {
                if ( !ensureArg(args, i, 2)) {
                    return false;
                }
                String       action = args.get(++i);
                List<String> idxs   = getCols(args.get(++i));
                info.getProcessor().addProcessor(new Converter.Case(idxs,
                        action));

                continue;
            }



            if (arg.equals("-addcell")) {
                if ( !ensureArg(args, i, 3)) {
                    return false;
                }
                int row = Integer.parseInt(args.get(++i));
                int col = Integer.parseInt(args.get(++i));
                info.getProcessor().addProcessor(
                    new Converter.ColumnNudger(row, col, args.get(++i)));

                continue;
            }

            if (arg.equals("-deletecell")) {
                if ( !ensureArg(args, i, 2)) {
                    return false;
                }
                int row = Integer.parseInt(args.get(++i));
                info.getProcessor().addProcessor(
                    new Converter.ColumnUnNudger(
                        row, getCols(args.get(++i))));

                continue;
            }

            if (arg.equals("-set")) {
                if ( !ensureArg(args, i, 3)) {
                    return false;
                }
                List<String> cols = getCols(args.get(++i));
                List<String> rows = getCols(args.get(++i));
                info.getProcessor().addProcessor(
                    new Converter.ColumnSetter(cols, rows, args.get(++i)));

                continue;
            }

            if (arg.equals("-width")) {
                if ( !ensureArg(args, i, 2)) {
                    return false;
                }
                List<String> cols = getCols(args.get(++i));
                int          size = Integer.parseInt(args.get(++i));
                info.getProcessor().addProcessor(
                    new Converter.ColumnWidth(cols, size));

                continue;
            }

            /*
            if (arg.equals("-addrow")) {
            if(!ensureArg(args, i,2)) return false;
                int row = Integer.parseInt(args.get(++i));
                info.getProcessor().addProcessor(
                    new Converter.RowInserter(
                        row, args.get(++i)));

                continue;
                }*/


            if (arg.equals("-combine")) {
                if ( !ensureArg(args, i, 3)) {
                    return false;
                }
                List<String> cols      = getCols(args.get(++i));
                String       separator = args.get(++i);
                String       name      = args.get(++i);
                info.getProcessor().addProcessor(
                    new Converter.ColumnConcatter(
                        cols, separator, name, false));

                continue;
            }


            if (arg.equals("-combineinplace")) {
                if ( !ensureArg(args, i, 3)) {
                    return false;
                }
                List<String> cols      = getCols(args.get(++i));
                String       separator = args.get(++i);
                String       name      = args.get(++i);
                info.getProcessor().addProcessor(
                    new Converter.ColumnConcatter(
                        cols, separator, name, true));

                continue;
            }

            if (arg.equals("-denormalize")) {
                if ( !ensureArg(args, i, 6)) {
                    return false;
                }
                String file = args.get(++i);
                int    col1 = Integer.parseInt(args.get(++i));
                int    col2 = Integer.parseInt(args.get(++i));
                int    col3 = Integer.parseInt(args.get(++i));
                String name = args.get(++i);
                String mode = args.get(++i);
                info.getProcessor().addProcessor(
                    new Converter.Denormalizer(
                        file, col1, col2, col3, name, mode));

                continue;
            }


            if (arg.equals("-or")) {
                filterToAddTo = new Filter.FilterGroup(false);
                info.getProcessor().addProcessor(filterToAddTo);

                continue;
            }

            if (arg.equals("-and")) {
                filterToAddTo = new Filter.FilterGroup(true);
                info.getProcessor().addProcessor(filterToAddTo);

                continue;
            }

            if (arg.equals("-pattern")) {
                if ( !ensureArg(args, i, 2)) {
                    return false;
                }
                String col     = args.get(++i);
                String pattern = args.get(++i);
                handlePattern(info, filterToAddTo,
                              new Filter.PatternFilter(col, pattern));

                continue;
            }

            if (arg.equals("-notpattern")) {
                if ( !ensureArg(args, i, 2)) {
                    return false;
                }
                String col     = args.get(++i);
                String pattern = args.get(++i);
                handlePattern(info, filterToAddTo,
                              new Filter.PatternFilter(col, pattern, true));

                continue;
            }

            if (arg.equals("-countvalue")) {
                if ( !ensureArg(args, i, 2)) {
                    return false;
                }
                String col = args.get(++i);
                int    cnt = Integer.parseInt(args.get(++i));
                handlePattern(info, filterToAddTo,
                              new Filter.CountValue(col, cnt));

                continue;
            }

            if (arg.equals("-lt")) {
                if ( !ensureArg(args, i, 2)) {
                    return false;
                }
                handlePattern(info, filterToAddTo,
                              new Filter.ValueFilter(args.get(++i),
                                  Filter.ValueFilter.OP_LT,
                                  Double.parseDouble(args.get(++i))));

                continue;
            }

            if (arg.equals("-gt")) {
                if ( !ensureArg(args, i, 2)) {
                    return false;
                }
                handlePattern(info, filterToAddTo,
                              new Filter.ValueFilter(args.get(++i),
                                  Filter.ValueFilter.OP_GT,
                                  Double.parseDouble(args.get(++i))));

                continue;
            }


            if (arg.equals("-defined")) {
                if ( !ensureArg(args, i, 1)) {
                    return false;
                }
                handlePattern(info, filterToAddTo,
                              new Filter.ValueFilter(args.get(++i),
                                  Filter.ValueFilter.OP_DEFINED, 0));

                continue;
            }
            if (arg.equals("-quit")) {
                String last = args.get(args.size() - 1);
                if (last.equals("-print")) {
                    info.getProcessor().addProcessor(
                        new Processor.Printer(printFields, trim));

                } else if (last.equals("-table")) {
                    info.getProcessor().addProcessor(new Processor.Html());
                }

                break;
            }
            if (arg.startsWith("-")) {
                throw new IllegalArgumentException("Unknown arg:" + arg);
            }

            int idx;

            idx = arg.indexOf("!=");
            if (idx >= 0) {
                handlePattern(info, filterToAddTo,
                              new Filter.PatternFilter(arg.substring(0,
                                  idx).trim(), arg.substring(idx + 2).trim(),
                                      true));

                continue;
            }


            idx = arg.indexOf("=~");
            if (idx >= 0) {
                handlePattern(info, filterToAddTo,
                              new Filter.PatternFilter(arg.substring(0,
                                  idx).trim(), arg.substring(idx
                                  + 2).trim()));

                continue;
            }


            boolean didone = false;
            for (String op : new String[] { "<=", ">=", "<", ">", "=" }) {
                idx = arg.indexOf(op);
                if (idx >= 0) {
                    handlePattern(
                        info, filterToAddTo,
                        new Filter.ValueFilter(
                            arg.substring(0, idx).trim(),
                            Filter.ValueFilter.getOperator(op),
                            Double.parseDouble(
                                arg.substring(idx + op.length()).trim())));
                    didone = true;

                    break;
                }
            }
            if (didone) {
                continue;
            }

            if (addFiles) {
                files.add(arg);
            }
        }

        if (doHtml) {
            Hashtable<String, String> props = parseProps(htmlProps);
            tokenizedRows.add(tokenizeHtml(files.get(0), props));
        } else if (doJson) {
            Hashtable<String, String> props = parseProps(jsonProps);
            tokenizedRows.add(tokenizeJson(files.get(0), props));
        }

        return true;
    }


    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    private List<String> getCols(String s) {
        if (true) {
            return StringUtil.split(s, ",", true, true);
        }

        List<String> cols = new ArrayList<String>();
        for (String tok : StringUtil.split(s, ",", true, true)) {
            if ((tok.indexOf("-") >= 0) && !tok.startsWith("-")) {
                int from = new Integer(StringUtil.split(tok, "-", true,
                               true).get(0)).intValue();
                int to = new Integer(StringUtil.split(tok, "-", true,
                             true).get(1)).intValue();
                for (int i = from; i <= to; i++) {
                    cols.add("" + i);
                }

                continue;
            }
            cols.add(tok);
        }

        return cols;
    }



    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    private List<Integer> getNumbers(String s) {
        List<Integer> cols = new ArrayList<Integer>();
        for (String tok : StringUtil.split(s, ",", true, true)) {
            if ((tok.indexOf("-") >= 0) && !tok.startsWith("-")) {
                int from = new Integer(StringUtil.split(tok, "-", true,
                               true).get(0)).intValue();
                int to = new Integer(StringUtil.split(tok, "-", true,
                             true).get(1)).intValue();
                for (int i = from; i <= to; i++) {
                    cols.add(i);
                }

                continue;
            }
            cols.add(Integer.parseInt(tok));
        }

        return cols;
    }


    /**
     * _more_
     *
     * @param info _more_
     * @param filterToAddTo _more_
     * @param converter _more_
     */
    private void handlePattern(TextReader info,
                               Filter.FilterGroup filterToAddTo,
                               Filter converter) {
        if (filterToAddTo != null) {
            filterToAddTo.addFilter(converter);
        } else {
            info.getProcessor().addProcessor(converter);
        }
    }

    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    private Hashtable<String, String> parseProps(String s) {
        s = s.replaceAll("_quote_", "\"");
        List<String> toks = Utils.parseCommandLine(s);
        //        System.err.println("s:" + s);
        //        System.err.println("toks:" + toks);
        Hashtable<String, String> props = new Hashtable<String, String>();
        for (int j = 0; j < toks.size(); j += 2) {
            if (j >= toks.size() - 1) {
                throw new IllegalArgumentException(
                    "Error: Odd number of arguments:" + toks);
            }
            props.put(toks.get(j), toks.get(j + 1));
        }

        return props;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getInstallPlugin() {
        return installPlugin;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getNukeDb() {
        return nukeDb;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getDbId() {
        return dbXml.getTableId();
    }


}
