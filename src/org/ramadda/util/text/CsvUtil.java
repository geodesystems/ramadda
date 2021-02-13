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
import org.ramadda.util.IO;
import org.ramadda.util.Json;
import org.ramadda.util.Utils;
import org.ramadda.util.XlsUtil;

import org.w3c.dom.*;

import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;


import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.*;
import java.util.zip.*;


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
    private String currentArg;

    /** _more_ */
    private Row currentRow;

    /** _more_ */
    private String errorDescription;

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

    /** _more_ */
    private List<String> changeFrom = new ArrayList<String>();

    /** _more_ */
    private List<String> changeTo = new ArrayList<String>();

    /** _more_ */
    private StringBuilder js = new StringBuilder();

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
        //      System.out.println("Args:" + this.args);
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
        this.js      = csvUtil.js;
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
     * _more_
     *
     * @return _more_
     */
    public String getErrorDescription() {
        return errorDescription;
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
            if (textReader != null) {
                textReader.getDestDir();
                textReader.addFile(outputFile.toString());
            }
            this.outputStream = new FileOutputStream(outputFile);
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
        try {
            runInner(files);
        } catch (Exception exc) {
            CsvOperator op = (textReader == null)
                             ? null
                             : textReader.getCurrentOperator();
            if (op != null) {
                errorDescription = "Error processing text with operator: "
                                   + op.getDescription();
            } else if (currentArg != null) {
                errorDescription = "Error processing argument:" + currentArg;
            }
            if (currentRow != null) {
                errorDescription += "\nRow:" + currentRow.getValues() + "\n";

            }

            throw exc;
        }
    }

    /**
     * _more_
     *
     * @param files _more_
     *
     * @throws Exception _more_
     */
    private void runInner(List<String> files) throws Exception {

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

        boolean      printArgs = false;
        List<String> extra     = new ArrayList<String>();
        for (int i = 0; i < args.size(); i++) {
            String arg = args.get(i);
            //      System.out.println("ARG:" + arg);
            //      if(true) continue;
            if (arg.equals("-printargs")) {
                System.out.print("java  org.ramadda.util.text.CsvUtil ");
                printArgs = true;

                continue;
            }
            if (printArgs) {
                if ( !arg.startsWith("\"") && !arg.startsWith("-")) {
                    System.out.print("\"" + arg + "\" ");
                } else {
                    System.out.print(arg + " ");
                }
            }
            if (arg.equals("-help")) {
                usage("", null);

                return;
            }
            if (arg.equals("-genhelp")) {
                genHelp();

                return;
            }
            if (arg.equals("-helpraw")) {
                usage("", null, "-raw", "true");

                return;
            }
            if (arg.equals("-helpjson")) {
                usage("", null, "-json", "true");

                return;
            }
            if (arg.startsWith("-help:")) {
                usage("", arg.substring("-help:".length()));

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

            if (arg.equals("-commentChar")) {
                textReader.setCommentChar(args.get(++i));

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
            currentArg = null;

            return;
        }
        currentArg = null;
        if (printArgs) {
            for (String f : files) {
                System.out.print(f + " ");
            }
            if (files.size() == 0) {
                System.out.print(" ${1} ");
            }
            System.out.println("");
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
                textReader.getProcessor().addProcessor(iteratePattern);
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
                        IO.close(is);
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
        } else if (file.toLowerCase().endsWith(".zip")) {
            InputStream    fis = IO.getInputStream(file.toString());
            ZipInputStream zin = new ZipInputStream(fis);
            ZipEntry       ze  = null;
            while ((ze = zin.getNextEntry()) != null) {
                if (ze.isDirectory()) {
                    continue;
                }
                String p = ze.getName().toLowerCase();
                if (p.endsWith(".csv") || p.endsWith(".tsv")) {
                    return zin;
                }
                //Apple health
                if (p.endsWith("export.xml")) {
                    return zin;
                }
            }
        } else {
            if (new File(file).exists()) {
                try {
                    return new BufferedInputStream(new FileInputStream(file));
                } catch (Exception exc) {
                    System.err.println("Error opening file:" + file);

                    throw exc;
                }
            }

            return IO.getInputStream(file);
        }

        return null;
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
                                : textReader.getSplitOnSpaces()
                                  ? StringUtil.split(line, " ", true, true)
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
     * @param header _more_
     * @param chunkPattern _more_
     * @param tokenPattern _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Row> tokenizeText(String file, String header,
                                  String chunkPattern, String tokenPattern)
            throws Exception {
        chunkPattern = convertPattern(chunkPattern);
        tokenPattern = convertPattern(tokenPattern);
        String    s         = IO.readContents(file);
        List<Row> rows      = new ArrayList<Row>();
        Row       headerRow = new Row();
        rows.add(headerRow);
        for (String tok : StringUtil.split(header, ",")) {
            headerRow.add(tok);
        }
        try {
            Pattern p1 = Pattern.compile(chunkPattern);
            while (true) {
                Matcher m1 = p1.matcher(s);
                if ( !m1.find()) {
                    break;
                }
                s = s.substring(m1.end());
                for (int i1 = 1; i1 <= m1.groupCount(); i1++) {
                    String chunk = m1.group(i1).trim();
                    //              System.err.println("CHUNK:");
                    Pattern p2  = Pattern.compile(tokenPattern);
                    int     cnt = 1;
                    while (true) {
                        Matcher m2 = p2.matcher(chunk);
                        if ( !m2.find()) {
                            break;
                        }
                        Row row = null;
                        if (cnt < rows.size()) {
                            row = rows.get(cnt);
                        } else {
                            row = new Row();
                            rows.add(row);
                        }
                        cnt++;
                        for (int i2 = 1; i2 <= m2.groupCount(); i2++) {
                            String tok = m2.group(i2).trim();
                            //                      System.err.println("\ttok:" + tok);
                            row.add(tok);
                        }
                        chunk = chunk.substring(m2.end());
                    }
                }
            }
        } catch (Exception exc) {
            System.err.println("error: pattern:\"" + chunkPattern + "\"  "
                               + exc);
        }

        return rows;
    }





    /**
     * _more_
     *
     * @param file _more_
     * @param sSkip _more_
     * @param pattern _more_
     * @param props _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Row> tokenizeHtml(String file, String sSkip, String pattern,
                                  Hashtable<String, String> props)
            throws Exception {

        int count = Utils.getProperty(props, "count", 1);
        int skip  = 0;
        if ((sSkip != null) && (sSkip.trim().length() > 0)) {
            skip = Integer.parseInt(sSkip.trim());
        }
        if ((pattern == null) || (pattern.trim().length() == 0)) {
            pattern = props.get("pattern");
        }
        String  skipAttr     = props.get("skipAttr");
        boolean removeEntity = Utils.equals(props.get("removeEntity"),
                                            "true");
        String  removePattern  = convertPattern(props.get("removePattern"));
        String  removePattern2 = convertPattern(props.get("removePattern2"));
        Pattern attrPattern    = null;
        if (skipAttr != null) {
            skipAttr    = skipAttr.replaceAll("_quote_", "\"");
            attrPattern = Pattern.compile(skipAttr, Pattern.MULTILINE);
        }
        boolean   debug = false;
        List<Row> rows  = new ArrayList<Row>();
        String    s     = IO.readContents(file);
        for (int i = 0; i < changeFrom.size(); i++) {
            s = s.replaceAll(changeFrom.get(i), changeTo.get(i));
        }

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
                Row     row         = new Row();
                boolean checkHeader = true;
                while (true) {
                    toks = Utils.tokenizePattern(tr, "(<td|<th)",
                            "(</td|</th)");
                    if (toks == null) {
                        break;
                    }
                    String td = toks[0].trim();
                    tr = toks[1];
                    String colspan = StringUtil.findPattern(td,
                                         "colspan *= *\"?([0-9]+)\"?");
                    int inserts = 1;
                    if (colspan != null) {
                        inserts = Integer.parseInt(colspan);
                    }

                    int idx = td.indexOf(">");
                    if ((attrPattern != null) && (idx >= 0)) {
                        String attrs = td.substring(0, idx).toLowerCase();
                        if (attrPattern.matcher(attrs).find()) {
                            System.out.println("skipping:"
                                    + td.replaceAll("\n", " "));

                            continue;
                        }
                        //                        System.out.println("not skipping:" +td );
                    }
                    //              System.err.println("td:" + td);
                    td = td.substring(idx + 1);
                    //              System.err.println("after TD:" + td);
                    td = Utils.stripTags(td);
                    //              System.err.println("after Strip:" + td);
                    td = td.replaceAll("\n", " ").replaceAll("  +", "");
                    td = HtmlUtils.unescapeHtml3(td);
                    //              System.err.println(td+"  stripped:" + td);
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
                    while (inserts-- > 0) {
                        row.insert(td.trim());
                    }
                    if (debug) {
                        System.out.println("\t\ttd:" + td);
                    }
                }
                checkHeader = false;
                if (row.size() > 0) {
                    rows.add(row);
                }
                //              if(rows.size()>2) break;
            }
            if (--count <= 0) {
                break;
            }
        }

        return rows;

    }


    /**
     * _more_
     *
     * @param s _more_
     * @param cols _more_
     * @param start _more_
     * @param end _more_
     * @param pattern _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Row> tokenizeHtmlPattern(String s, String cols, String start,
                                         String end, String pattern)
            throws Exception {

        List<Row> rows = new ArrayList<Row>();
        if (cols.length() > 0) {
            rows.add(new Row(StringUtil.split(cols, ",")));
        }

        //      System.err.println(start);
        if (start.length() != 0) {
            int index = s.indexOf(start);
            if (index >= 0) {
                s = s.substring(index);
            } else {
                throw new IllegalArgumentException("Missing start pattern:"
                        + start);
            }
        }
        //      System.out.println("***** START:"+s);
        if (end.length() != 0) {
            int index = s.indexOf(end);
            if (index >= 0) {
                s = s.substring(0, index - 1);
            } else {
                throw new IllegalArgumentException("Missing end pattern:"
                        + end);
            }
        }
        //      System.out.println("***** FINAL:"+s);
        Pattern p = Pattern.compile(pattern);
	int cnt = 0;
        while (true) {
            Matcher m = p.matcher(s);
            if ( !m.find()) {
                break;
            }
            Row row = new Row();
            rows.add(row);
            for (int i = 1; i <= m.groupCount(); i++) {
                String tok = m.group(i).trim();
                row.add(tok);
            }
            String s2 = s.substring(m.end());
            if (s.length() == s2.length()) {
                break;
            }
            s = s2;
            //      if(rows.size()>2) return rows;
        }

        return rows;
    }



    /**
     * _more_
     *
     * @param s _more_
     * @param arrayPath _more_
     * @param objectPath _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Row> tokenizeJson(String s, String arrayPath,
                                  String objectPath)
            throws Exception {

        int        xcnt  = 0;
        List<Row>  rows  = new ArrayList<Row>();
        JSONArray  array = null;
        JSONObject root  = null;
        try {
            root = new JSONObject(s);
            if (arrayPath != null) {
                array = Json.readArray(root, arrayPath);
            }
        } catch (Exception exc) {
            System.err.println("exc:" + exc);
        }
        if (array == null) {
            array = new JSONArray(s);
        }

        List<String> objectPathList = null;
        if (Utils.stringDefined(objectPath)) {
            objectPathList = StringUtil.split(objectPath, ",", true, true);
        }
        List<String> names = null;
        for (int arrayIdx = 0; arrayIdx < array.length(); arrayIdx++) {
            if (arrayIdx > 0) {
                break;
            }
            Hashtable       primary   = new Hashtable();
            List<Hashtable> secondary = new ArrayList<Hashtable>();
            if (objectPathList != null) {
                JSONObject jrow = array.getJSONObject(arrayIdx);
                for (String tok : objectPathList) {
                    if (tok.equals("*")) {
                        primary.putAll(Json.getHashtable(jrow, true));
                    } else if (tok.endsWith("[]")) {
                        JSONArray a = Json.readArray(jrow,
                                          tok.substring(0, tok.length() - 2));
                        for (int j = 0; j < a.length(); j++) {
                            secondary.add(
                                Json.getHashtable(a.getJSONObject(j), true));
                        }
                    } else {
                        try {
                            Object o = Json.readObject(jrow, tok);
                            if (o != null) {
                                primary.putAll(Json.getHashtable(o, true));
                            }
                        } catch (Exception exc) {
                            Object o = Json.readArray(jrow, tok);
                            if (o != null) {
                                primary.putAll(Json.getHashtable(o, true));
                            }
                        }
                    }
                }
            } else {
                try {
                    primary.putAll(
                        Json.getHashtable(
                            array.getJSONArray(arrayIdx), true));
                } catch (Exception exc) {
                    primary.putAll(
                        Json.getHashtable(
                            array.getJSONObject(arrayIdx), true));
                }
            }

            if (names == null) {
                if (secondary.size() == 0) {
                    secondary.add(primary);
                } else {
                    for (Hashtable h : secondary) {
                        h.putAll(primary);
                    }
                }
                names = new ArrayList<String>();
                Row row = new Row();
                rows.add(row);
                for (Enumeration keys = secondary.get(0).keys();
                        keys.hasMoreElements(); ) {
                    names.add((String) keys.nextElement());
                }
                names = (List<String>) Utils.sort(names);
                for (String name : names) {
                    row.add(name);
                }
            }
            /*
              JSONArray fields = root.optJSONArray("fields");
              if(fields!=null) {
              for(int k=0;k<fields.length();k++)
              row.add(fields.getString(k));
              } else  {
              for (int k = 0; k < jarray.length(); k++) {
              row.add("Index " + k);
              }
            ***/

            for (Hashtable h : secondary) {
                Row row = new Row();
                rows.add(row);
                for (String name : names) {
                    Object value = h.get(name);
                    if (value == null) {
                        value = "NULL";
                    }
                    row.add(value);
                }
            }
        }

        //      System.out.println(rows.get(0).print(null));
        //      System.out.println(rows.get(1).print(rows.get(0)));
        //      System.out.println(rows.get(2).print(rows.get(0)));
        return rows;



    }

    /**
     * _more_
     *
     * @param s _more_
     * @param arrayPath _more_
     * @param objectPath _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Row> tokenizeXml(String s, String arrayPath,
                                 String objectPath)
            throws Exception {

        Element   root   = XmlUtil.getRoot(s);
        List<Row> rows   = new ArrayList<Row>();
        Row       header = new Row();
        rows.add(header);
        List<Element> nodes =
            (List<Element>) Utils.findDescendantsFromPath(root, arrayPath);
        int                        cnt    = 0;
        int                        colCnt = 0;
        Hashtable<String, Integer> colMap = new Hashtable<String, Integer>();
        System.err.println("NODES:" + nodes.size());
        for (Element parent : nodes) {
            NodeList children = XmlUtil.getElements(parent);

            if (children.getLength() == 0) {
                //use attrs
                NamedNodeMap nnm = parent.getAttributes();
                if (nnm == null) {
                    continue;
                }
                for (int i = 0; i < nnm.getLength(); i++) {
                    Attr    attr = (Attr) nnm.item(i);
                    String  name = attr.getNodeName();
                    Integer idx  = colMap.get(name);
                    if (idx == null) {
                        colMap.put(name, colCnt++);
                        header.add(name);
                    }
                }

                continue;
            }
            for (int i = 0; i < children.getLength(); i++) {
                Element  child     = (Element) children.item(i);
                NodeList gchildren = XmlUtil.getElements(child);
                //go one level down
                String tag = child.getTagName();
                if (gchildren.getLength() > 0) {
                    for (int gi = 0; gi < gchildren.getLength(); gi++) {
                        Element gchild = (Element) gchildren.item(gi);
                        String  gtag   = tag + "." + gchild.getTagName();
                        Integer idx    = colMap.get(gtag);
                        if (idx == null) {
                            colMap.put(gtag, colCnt++);
                            header.add(gchild.getTagName());
                        }
                    }
                } else {
                    Integer idx = colMap.get(tag);
                    if (idx == null) {
                        colMap.put(child.getTagName(), colCnt++);
                        header.add(child.getTagName());
                    }
                }
            }
        }

        for (Element parent : nodes) {
            NodeList children = XmlUtil.getElements(parent);
            Row      row      = new Row();
            rows.add(row);
            for (int i = 0; i < colCnt; i++) {
                row.add("");
            }
            if (children.getLength() == 0) {
                NamedNodeMap nnm = parent.getAttributes();
                if (nnm == null) {
                    continue;
                }
                for (int i = 0; i < nnm.getLength(); i++) {
                    Attr    attr  = (Attr) nnm.item(i);
                    String  name  = attr.getNodeName();
                    String  value = attr.getNodeValue();
                    Integer idx   = colMap.get(name);
                    row.set(idx, value);
                }

                continue;
            }

            for (int i = 0; i < children.getLength(); i++) {
                Element  child     = (Element) children.item(i);
                NodeList gchildren = XmlUtil.getElements(child);
                //go one level down
                String tag = child.getTagName();
                if (gchildren.getLength() > 0) {
                    for (int gi = 0; gi < gchildren.getLength(); gi++) {
                        Element gchild = (Element) gchildren.item(gi);
                        String  gtag   = tag + "." + gchild.getTagName();
                        Integer idx    = colMap.get(gtag);
                        String  text   = XmlUtil.getChildText(gchild);
                        row.set(idx, text);
                    }
                } else {
                    Integer idx  = colMap.get(child.getTagName());
                    String  text = XmlUtil.getChildText(child);
                    row.set(idx, text);
                }
            }
            cnt++;
        }

        return rows;
    }


    /**
     * _more_
     *
     * @param file _more_
     * @param header _more_
     * @param pattern _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Row> tokenizePattern(String file, List<String> header,
                                     String pattern)
            throws Exception {
        String    s    = IO.readContents(file);
        List<Row> rows = new ArrayList<Row>();
        rows.add(new Row(header));
        if (pattern.length() == 0) {
            return rows;
        }
        Pattern p = Pattern.compile(pattern);
        while (true) {
            Matcher m = p.matcher(s);
            if ( !m.find()) {
                break;
            }
            Row row = new Row();
            rows.add(row);
            for (int i = 1; i <= m.groupCount(); i++) {
                row.add(m.group(i));
            }
            s = s.substring(m.end());
            //      System.err.println(s.length());
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
                    new InputStreamReader(getInputStream(file))));
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
     * @param index _more_
     * @param prop _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public static String getDbProp(Hashtable<String, String> props,
                                   String colId, int index, String prop,
                                   String dflt) {
        String value = getDbProp(props, colId, prop, null);
        if (value != null) {
            return value;
        }

        return getDbProp(props, index + "", prop, dflt);
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
        try {
            errorDescription = null;
            processInner(textReader);
        } catch (Exception exc) {
            CsvOperator op = (textReader == null)
                             ? null
                             : textReader.getCurrentOperator();
            System.err.println("error:" + op);
            if (op != null) {
                errorDescription = "Error processing text with operator: "
                                   + op.getDescription();
            }

            throw exc;
        }
    }


    /**
     * _more_
     *
     * @param textReader _more_
     *
     * @throws Exception _more_
     */
    private void processInner(TextReader textReader) throws Exception {
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

                //              System.out.println("\tLine:" + line);

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
                Row row = null;
                if (widths != null) {
                    row = new Row(Utils.tokenizeColumns(line, widths));
                } else if (textReader.getSplitOnSpaces()) {
                    row = new Row(StringUtil.split(line, " ", true, true));
                } else {
                    row = new Row(line, textReader.getDelimiter());
                }
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
        if ((textReader.getFilter() != null)) {
            if ( !textReader.getFilter().rowOk(textReader, row, line)) {
                return true;
            }
        }


        textReader.initRow(row);
        if ((textReader.getMaxRows() >= 0)
                && (textReader.getVisitedRows() > textReader.getMaxRows())) {
            return false;
        }
        if (textReader.getProcessor() != null) {
            textReader.setCurrentOperator(null);
            currentRow = row;
            row = textReader.getProcessor().processRow(textReader, row, line);
            currentRow = null;
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
        return columnsToString(cols, delimiter, false);
    }

    /**
     * _more_
     *
     * @param cols _more_
     * @param delimiter _more_
     * @param addNewLine _more_
     *
     * @return _more_
     */
    public static String columnsToString(List cols, String delimiter,
                                         boolean addNewLine) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cols.size(); i++) {
            Object o = cols.get(i);
            String s = ((o == null)
                        ? ""
                        : o.toString());
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
        if (addNewLine) {
            sb.append("\n");
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
     * @version        $version$, Tue, Mar 24, '20
     * @author         Enter your name here...
     */
    public static class Arg {

        /** _more_ */
        String id;

        /** _more_ */
        String desc;

        /** _more_ */
        String[] props;

        /**
         * _more_
         *
         * @param id _more_
         */
        public Arg(String id) {
            this(id, "");
        }

        /**
         * _more_
         *
         * @param id _more_
         * @param desc _more_
         * @param props _more_
         */
        public Arg(String id, String desc, String... props) {
            if ((desc.length() == 0) && id.equals("columns")) {
                desc = "Column indices, one per line.<br>Can include ranges, e.g. 0-5";
            }
            if ((desc.length() == 0) && id.equals("rows")) {
                desc = "Row indices, one per line.<br>Can include ranges, e.g. 0-5";
            }
            this.id    = id;
            this.desc  = desc;
            this.props = props;
        }
    }


    /**
     * Class description
     *
     *
     * @version        $version$, Sun, Apr 5, '20
     * @author         Enter your name here...
     */
    public static class Label {

        /** _more_ */
        String label;

        /**
         * _more_
         *
         * @param l _more_
         */
        public Label(String l) {
            this.label = l;
        }
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
        boolean category;

        /** _more_ */
        String cmd;

        /** _more_ */
        String label;

        /** _more_ */
        List<Arg> args = new ArrayList<Arg>();

        /** _more_ */
        String desc;

        /**
         * _more_
         *
         * @param isCat _more_
         * @param category _more_
         */
        public Cmd(boolean isCat, String category) {
            this.category = isCat;
            this.desc     = category;
        }


        /**
         * _more_
         *
         * @param cmd _more_
         * @param args _more_
         * @param desc _more_
         */
        public Cmd(String cmd, String desc, Object... args) {
            this(cmd, (Label) null, desc, args);
        }

        /**
         * _more_
         *
         * @param cmd _more_
         * @param label _more_
         * @param desc _more_
         * @param args _more_
         */
        public Cmd(String cmd, Label label, String desc, Object... args) {
            if (label != null) {
                this.label = label.label;
            }
            this.cmd  = cmd;
            this.desc = desc;
            for (Object obj : args) {
                if ( !(obj instanceof Arg)) {
                    obj = new Arg(obj.toString(), "");
                }
                this.args.add((Arg) obj);
            }
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
            StringBuilder sb = new StringBuilder();
            if (args != null) {
                for (Arg arg : args) {
                    sb.append("<" + arg.id + ((arg.desc.length() > 0)
                            ? " " + arg.desc
                            : "") + "> ");
                }
            }

            return cmd + " " + sb + " (" + desc + ")";
        }
    }

    /** _more_ */
    private static final Cmd[] commands = {
        new Cmd("-help", "print this help)"),
        new Cmd("-help:<topic search string>",
                "print help that matches topic"),

        /** *  Slice and dice * */
        new Cmd(true, "Slice and Dice"),
        new Cmd("-columns", new Label("Select columns"),
                "Only include the given columns",
                new Arg("columns", "", "type", "columns")),
        new Cmd("-notcolumns", new Label("Deselect columns"),
                "Don't include given columns",
                new Arg("columns", "", "type", "columns")),
        new Cmd("-delete", new Label("Delete columns"), "Remove the columns",
                new Arg("columns", "", "type", "columns")),
        new Cmd("-cut", new Label("Drop rows"), "",
                new Arg("rows",
                        "One or more rows. -1 to the end. e.g., 0-3,5,10,-1",
                        "type", "rows")),
        new Cmd("-include", new Label("Include rows"),
                "Only include specified rows",
                new Arg("rows", "one or more rows, -1 to the end", "type",
                        "rows")),
        new Cmd("-skip", "Skip number of rows",
                new Arg("rows", "How many rows to skip", "type", "number")),
        new Cmd("-copy", new Label("Copy column"), "",
                new Arg("column", "", "type", "column"), "name"),
        new Cmd(
            "-insert", new Label("Insert column"),
            "Insert new column values",
            new Arg("column", "Column to insert after", "type", "column"),
            new Arg(
                "values",
                "Single value or comma separated for multiple rows", "type",
                "list")),
        new Cmd("-concat", "Create a new column from the given columns",
                new Arg("columns", "", "type", "columns"), "delimiter"),
        new Cmd("-split", "Split the column",
                new Arg("column", "", "type", "column"),
                new Arg("delimiter", "What to split on"),
                new Arg("names", "Comma separated new column names", "type",
                        "list")),
        new Cmd("-splat",
                "Create a new column from the values in the given column",
                "key col", new Arg("column", "", "type", "column"),
                "delimiter", new Arg("name", "new column name")),
        new Cmd("-shift", new Label("Shift columns"),
                "Shift columns over by count for given rows",
                new Arg("rows", "Rows to apply to", "type", "rows"),
                new Arg("column", "Column to start at", "type", "column"),
                new Arg("count")),
        new Cmd("-addcell", new Label("Add cell"),
                "Add a new cell at row/column", new Arg("row"),
                new Arg("column", "", "type", "column"), "value"),
        new Cmd("-deletecell", new Label("Delete cell"),
                "Delete cell at row/column", new Arg("row"),
                new Arg("column", "", "type", "column")),
        new Cmd("-mergerows", new Label("Merge rows"), "",
                new Arg("rows", "2 or more rows", "type", "rows"),
                new Arg("delimiter"), new Arg("close")),
        new Cmd("-rowop", new Label("Row Operator"),
                "Apply an operator to columns and merge rows",
                new Arg("keys", "Key columns", "type", "columns"),
                new Arg("values", "Value columns", "type", "columns"),
                new Arg("operator", "Operator", "values",
                        "average,min,max,count")),
        new Cmd("-rotate", "Rotate the data"),
        new Cmd("-flip", "Reverse the order of the rows except the header"),
        new Cmd("-unfurl", "Make columns from data values",
                new Arg("column", "column to get new column header#", "type",
                    "column"), new Arg("value columns",
                        "Columns to get values from", "type",
                        "columns"), new Arg("unique column",
                            "The unique value, e.g. date", "type",
                            "column"), new Arg("other columns",
                                "Other columns to include", "type",
                                "columns")),
        new Cmd("-furl", "Use values in header to make new row",
                new Arg("columns", "", "type", "columns"), "header label",
                "value label"),
        new Cmd("-explode", "Make separate files based on value of column",
                new Arg("column", "", "type", "column")),
        new Cmd("-join", "Join the 2 files together",
                new Arg("key columns", "", "type", "columns"),
                new Arg("value_columns", "value columns"),
                new Arg("file", "File to join with", "type", "file"),
                new Arg("source_columns", "source key columns")),

        /** *  Filter * */
        new Cmd(true, "Filter"),
        new Cmd("-start", "Start at pattern in source file",
                new Arg("start pattern", "", "type", "pattern")),
        new Cmd("-stop", "End at pattern in source file",
                new Arg("stop pattern", "", "type", "pattern")),
        new Cmd("-rawlines", "",
                new Arg("lines",
                        "How many lines to pass through unprocesed")),
        new Cmd(
            "-min",
            "Only pass thorough lines that have at least this number of columns",
            new Arg("min # columns", "", "type", "number")),
        new Cmd(
            "-max",
            "Only pass through lines that have no more than this number of columns",
            new Arg("max # columns", "", "type", "number")),
        new Cmd("-pattern", "Pass through rows that match the pattern",
                new Arg("column", "", "type", "column"),
                new Arg("pattern", "", "type", "pattern")),
        new Cmd("-notpattern",
                "Pass through rows that don't match the pattern",
                new Arg("column", "", "type", "column"),
                new Arg("pattern", "", "type", "pattern")),
        new Cmd("-unique", "Pass through unique values", new Arg("columns")),
        new Cmd("-dups", new Label("Duplicate values"),
                "Pass through duplicate values", new Arg("columns")),
        new Cmd("-maxvalue", new Label("Max value"), "", "key column",
                "value column"),
        new Cmd("-eq", new Label("Equals"),
                "Extract rows that pass the expression",
                new Arg("column", "", "type", "column"), new Arg("value")),
        new Cmd("-ne", new Label("Not equals"),
                "Extract rows that pass the expression",
                new Arg("column", "", "type", "column"), new Arg("value")),
        new Cmd("-gt", new Label("Greater than"),
                "Extract rows that pass the expression",
                new Arg("column", "", "type", "column"), new Arg("value")),
        new Cmd("-ge", new Label("Greater than/equals"),
                "Extract rows that pass the expression",
                new Arg("column", "", "type", "column"), new Arg("value")),
        new Cmd("-lt", new Label("Less than"),
                "Extract rows that pass the expression",
                new Arg("column", "", "type", "column"), new Arg("value")),
        new Cmd("-le", new Label("Less than/equals"),
                "Extract rows that pass the expression",
                new Arg("column", "", "type", "column"), new Arg("value")),
        new Cmd("-groupfilter", new Label("Group filter"),
                "One row in each group has to match",
                new Arg("column", "", "type", "column"),
                new Arg("value_column", "Value column", "type", "column"),
                new Arg("operator", "", "values", "=,!=,~,<,<=,>,>="),
                new Arg("value")),
        new Cmd("-before", new Label("Before date"), "",
                new Arg("column", "", "type", "column"), new Arg("format"),
                new Arg("date"), new Arg("format2")),
        new Cmd("-after", new Label("After date"), "",
                new Arg("column", "", "type", "column"), new Arg("format"),
                new Arg("date"), new Arg("format2")),
        new Cmd("-latest", new Label("Latest date"), "",
                new Arg("columns", "Key columns", "type", "columns"),
                new Arg("column", "Date column", "type", "column"),
                new Arg("format")),
        new Cmd("-countvalue", new Label("Max unique values"),
                "No more than count unique values",
                new Arg("column", "", "type", "column"), new Arg("count")),
        new Cmd("-decimate", "only include every <skip factor> row",
                new Arg("rows", "# of start rows to include"),
                new Arg("skip", "skip factor")),
        new Cmd("-skipline", "Skip any line that matches the pattern",
                new Arg("pattern", "", "type", "pattern")),

        /** *  Change values * */
        new Cmd(true, "Change Values"),
        new Cmd("-change", "Change columns",
                new Arg("columns", "", "type", "columns"),
                new Arg("pattern", "", "type", "pattern"),
                new Arg("substitution string",
                        "use $1, $2, etc for pattern (...) matches")),
        new Cmd("-changerow", "Change the values in the row/cols",
                new Arg("rows"), new Arg("columns", "", "type", "columns"),
                new Arg("pattern", "", "type", "pattern"),
                new Arg("substitution string")),
        new Cmd("-set", "Write the value into the cells",
                new Arg("columns", "", "type", "columns"),
                new Arg("rows", "", "type", "list"), new Arg("value")),
        new Cmd(
            "-macro",
            "Look for the pattern in the header and apply the template to make a new column, template: '{1} {2} ...', use 'none' for column name for no header",
            new Arg("pattern", "", "type", "pattern"), new Arg("template"),
            new Arg("column label")),
        new Cmd(
            "-setcol",
            "Write the value into the write col for rows that match the pattern",
            new Arg("column", "match col #", "type", "column"),
            new Arg("pattern", "", "type", "pattern"),
            new Arg("write column", "", "type", "column"), new Arg("value")),
        new Cmd(
            "-priorprefix",
            "Append prefix from the previous element to rows that match pattern",
            new Arg("column", "", "type", "column"),
            new Arg("pattern", "", "type", "pattern"), new Arg("delimiter")),
        new Cmd("-case", "Change case of column",
                new Arg("type", "", "values",
                        "lower,upper,camel,capitalize"), new Arg("column",
                            "", "type", "column")),
        new Cmd("-width", "Limit the string size of the columns",
                new Arg("columns", "", "type", "columns"), new Arg("size")),
        new Cmd(
            "-prepend",
            "Add the text to the beginning of the file. use _nl_ to insert newlines",
            new Arg("text")),
        new Cmd("-pad", "Add or remove columns to achieve the count",
                new Arg("count"), new Arg("pad string")),
        new Cmd("-prefix", "Add prefix to column",
                new Arg("column", "", "type", "column"), new Arg("prefix")),
        new Cmd("-suffix", "Add suffix to column",
                new Arg("column", "", "type", "column"), "suffix"),
        new Cmd(
            "-js",
            "Define Javascript (e.g., functions) to use later in the -func call",
            new Arg("javascript", "", "rows", "6")),
        new Cmd("-func",
                "Apply the javascript function. Use _colname or _col#",
                new Arg("names", "New column names", "type", "list"),
                new Arg("javascript", "javascript expression", "size", "60")),
        new Cmd("-endswith", "Ensure that each column ends with the string",
                new Arg("column", "", "type", "column"), new Arg("string")),
        new Cmd("-trim", "Trim the string values",
                new Arg("columns", "", "type", "columns")),
        new Cmd("-truncate", "", new Arg("column", "", "type", "columns"),
                "max length", "suffix"),
        new Cmd("-extract", "Extract text from column and make a new column",
                new Arg("column", "", "type", "column"),
                new Arg("pattern", "", "type", "pattern"),
                new Arg("replace with", "use 'none' for no replacement"),
                "new column name"),
        new Cmd("-map", "Change values in column to new values",
                new Arg("column", "", "type", "columns"), "new columns name",
                "value newvalue ..."),
        new Cmd("-combine",
                "Combine columns with the delimiter. deleting columns",
                new Arg("column", "", "type", "columns"), "delimiter",
                "new column name"),
        new Cmd("-combineinplace", new Label("Combine in place"),
                "Combine columns with the delimiter",
                new Arg("column", "", "type", "columns"), "delimiter",
                "new column name"),
        new Cmd("-format", "", new Arg("columns", "", "type", "columns"),
                new Arg("format", "Decimal format  e.g. '##0.00'")),
        new Cmd(
            "-denormalize",
            "Read the id,value from file and substitute the value in the dest file col idx",
            new Arg("file", "From csv file", "type", "file"), "from id idx",
            "from value idx", "to idx", "new col name", "mode replace add"),
        new Cmd("-break", "Break apart column values and make new rows",
                "label1", "label2", "columns"),

        /** *  Add values * */
        new Cmd(true, "Add Values"),
        new Cmd("-md", "Make a message digest of the column values",
                new Arg("columns", "", "type", "columns"),
                new Arg("type", "", "values", "MD5,SHA-1,SHA-256")),
        new Cmd("-uuid", "Add a UUID field"),
        new Cmd("-number", "Add 1,2,3... as column"),
        new Cmd("-letter", "Add 'A','B', ... as column"),

        /** *  Dates * */
        new Cmd(true, "Dates"),
        new Cmd("-convertdate", new Label("Convert date"), "",
                new Arg("column", "", "type", "columns"),
                new Arg("sourceformat", "Source format"),
                new Arg("destformat", "Target format")),
        new Cmd(
            "-extractdate", new Label("Extract date"), "",
            new Arg("date column", "", "type", "column"),
            new Arg("format", "Date format"), new Arg("timezone"),
            new Arg(
                "what", "What to extract", "values",
                "era,year,month,day_of_month,day_of_week,week_of_month,day_of_week_in_month,am_pm,hour,hour_of_day,minute,second,millisecond")),
        new Cmd("-formatdate", new Label("Format date"), "",
                new Arg("columns"), "intial date format",
                "target date format"),

        /** *  Lookup * */
        new Cmd(true, "Lookup"),
        new Cmd("-wikidesc", "Add a description from wikipedia",
                new Arg("column", "", "type", "columns"), "suffix"),
        new Cmd("-image", "Search for an image",
                new Arg("column", "", "type", "columns"), "suffix"),
        new Cmd(
            "-imagefill",
            "Search for an image with the query column text if the given image column is blank. Add the given suffix to the search. ",
            new Arg("querycolumn", "", "type", "columns"), "suffix",
            new Arg("imagecolumn", "", "type", "column")),
        new Cmd("-gender", "Figure out the gender of the name in the column",
                new Arg("column", "", "type", "columns")),

        /** *  Numeric * */
        new Cmd(true, "Numeric"),
        new Cmd("-scale", "Set value={value+delta1}*scale+delta2",
                new Arg("column", "", "type", "columns"), "delta1", "scale",
                "delta2"),
        new Cmd("-generate", "Add row values", "label", "start", "step"),
        new Cmd("-decimals", "", new Arg("column", "", "type", "columns"),
                "how many decimals to round to"),
        new Cmd("-delta",
                "Add column that is the delta from the previous step",
                new Arg("key columns"), new Arg("columns")),
        new Cmd("-operator",
                "Apply the operator to the given columns and create new one",
                new Arg("columns"), "new col name", "operator +,-,*,/"),
        new Cmd("-round", "round the values", "columns"),
        new Cmd(
            "-sum",
            "Sum values keying on name column value. If no value columns specified then do a count",
            "key columns", "value columns", "carry over columns"),
        new Cmd("-percent", "", "columns to add"),
        new Cmd("-increase", "Calculate percent increase",
                new Arg("column", "", "type", "columns"), "how far back"),
        new Cmd("-average", "Calculate a moving average", "columns",
                "period", "label"),

        /** * Geocode  * */
        new Cmd(true, "Geocode"),
        new Cmd("-geocode", "", new Arg("columns", "", "type", "columns"),
                new Arg("prefix", "e.g., state: or county:"),
                new Arg("suffix")),
        new Cmd("-geocodeaddressdb", new Label("Geocode address for DB"), "",
                new Arg("columns"), "prefix", "suffix"),
        new Cmd("-geocodejoin", new Label("Geocode with file"),
                "Geocode with file",
                new Arg("column", "", "type", "columns"),
                new Arg("csv file", "File to get lat/lon from", "type",
                        "file"), "name idx", "lat idx", "lon idx"),
        new Cmd("-statename", "Add state name from state ID",
                new Arg("column")),
        new Cmd("-mercator", "Convert x/y to lon/lat", new Arg("columns")),
        new Cmd("-region", "Add the state's region",
                new Arg("columns", "", "type", "columns")),
        new Cmd("-population", "Add in population from address",
                new Arg("columns", "", "type", "columns"),
                new Arg("prefix", "e.g., state: or county:"), "suffix"),

        /** * Other  * */
        new Cmd(true, "Other Commands"),
        new Cmd("-sort", "",
                new Arg("column", "Column to sort on", "type", "column")),
        new Cmd("-descsort", "",
                new Arg("column", "Column to descending sort on", "type",
                        "column")),
        new Cmd("-count", "Show count"),
        new Cmd("-maxrows", "", "Max rows to print"),
        new Cmd("-changeline", new Label("Change line"), "Change the line",
                "from", "to"),
        new Cmd("-changeraw", new Label("Change input"), "Change input text",
                "from", "to"),
        new Cmd("-crop", new Label("Crop string"),
                "Crop last part of string after any of the patterns",
                "columns", "pattern1,pattern2"),
        new Cmd(
            "-strict",
            "Be strict on columns. any rows that are not the size of the other rows are dropped"),
        new Cmd(
            "-flag",
            "Be strict on columns. any rows that are not the size of the other rows are shown"),
        new Cmd("-verify",
                "Throw error if a row has a different number of columns",
                new Arg("# columns", "", "type", "number")),
        new Cmd("-prop", "Set a property",
                new Arg("property", "", "values", "position"),
                new Arg("value", "start, end, etc")),
        new Cmd("-comment", "", "string"),
        new Cmd("-verify",
                "Verify that all of the rows have the same # of columns"),

        /** * Input   * */
        new Cmd(true, "Input"),
        new Cmd("-delimiter", "Specify a delimiter",
                new Arg("delimiter", "Use 'space' for space, 'tab' for tab",
                        "size", "5")),
        new Cmd("-tab", "Use tabs"),
        new Cmd("-widths", "Columns are fixed widths",
                new Arg("widths", "w1,w2,...,wN")),
        new Cmd("-header", "Raw header",
                new Arg("header", "Column names", "type", "list")),
        new Cmd("-html", "Parse the table in the input html file",
                new Arg("skip", "Number of tables to skip", "type",
                    "number"), new Arg("pattern", "Pattern to skip to",
                        "type", "pattern", "size",
                        "40"), new Arg("properties",
                            "Other attributes - <br>&nbsp;&nbsp;removeEntity false removePattern pattern",
                            "rows", "6", "size", "40")),
        new Cmd("-htmlpattern", new Label("Extract from html"),
                "Parse the input html file",
                new Arg("columns", "Column names", "type", "columns"),
                new Arg("startPattern", "", "type", "pattern"),
                new Arg("endPattern", "", "type", "pattern"),
                new Arg("pattern", "Row pattern. Use (...) to match columns",
                        "type", "pattern")),
        new Cmd("-json", "Parse the input as json",
                new Arg("arrayPath",
                    "Path to the array e.g., obj1.arr[2].obj2", "size", "30",
                    "label", "Array path"), new Arg("objectPaths",
                        "One or more paths to the objects e.g. geometry,features",
                        "size", "30", "label", "Object paths", "type",
                        "list", "size", "30")),
        new Cmd("-xml", "Parse the input as xml",
                new Arg("path", "Path to the elements", "size", "60")),
        new Cmd("-text", "Extract rows from the text",
                new Arg("comma separated header"),
                new Arg("chunk pattern", "", "type", "pattern"),
                new Arg("token pattern", "", "type", "pattern")),
        new Cmd("-tokenize", "Tokenize the input from the pattern",
                new Arg("header", "header1,header2..."),
                new Arg("pattern", "", "type", "pattern")),
        new Cmd("-prune", "Prune out the first N bytes",
                new Arg("bytes", "Number of leading bytes to remove", "type",
                        "number")),

        /** * Output  * */
        new Cmd(true, "Output"), new Cmd("-print", "Output the rows"),
        new Cmd("-template", "Apply the template to make the output",
                new Arg("prefix", "", "size", "40"),
                new Arg("template", "Use ${0},${1}, etc for values", "rows",
                        "6"), new Arg("delimiter", "Output between rows",
                                      "size", "40"), new Arg("suffix", "",
                                          "size", "40")),
        new Cmd("-raw", "Print the file raw"),
        new Cmd("-record", "Print records"),
        new Cmd("-printheader", "Print the first line"),
        new Cmd("-pointheader", "Generate the RAMADDA point properties"),
        new Cmd("-addheader", new Label("Add header"),
                "Add the RAMADDA point properties",
                new Arg("properties", "name1 value1 ... nameN valueN",
                        "rows", "6")),
        new Cmd(
            "-db", "Generate the RAMADDA db xml from the header",
            new Arg(
                "props",
                "Name value pairs:\n\t\ttable.id <new id> table.name <new name> table.cansearch <true|false> table.canlist <true|false> table.icon <icon, e.g., /db/database.png>\n\t\t<column name>.id <new id for column> <column name>.label <new label>\n\t\t<column name>.type <string|enumeration|double|int|date>\n\t\t<column name>.format <yyyy MM dd HH mm ss format for dates>\n\t\t<column name>.canlist <true|false> <column name>.cansearch <true|false>\n\t\tinstall <true|false install the new db table>\n\t\tnukedb <true|false careful! this deletes any prior created dbs", "rows", "6")),
        new Cmd("-toxml", "Generate XML", new Arg("tag")),
        new Cmd("-run", "", "Name of process directory"),
        new Cmd("-cat", "One or more csv files", "*.csv"),
        new Cmd("-args", "Generate the CSV file commands"),
    };



    /**
     * _more_
     *
     *
     * @param msg _more_
     * @param match _more_
     * @param args _more_
     * @throws Exception _more_
     */
    public void usage(String msg, String match, String... args)
            throws Exception {

        boolean exact = false;
        boolean raw   = false;
        boolean json  = false;
        for (int i = 0; i < args.length; i += 2) {
            if (args[i].equals("-exact")) {
                exact = args[i + 1].equals("true");
            } else if (args[i].equals("-raw")) {
                raw = args[i + 1].equals("true");
            } else if (args[i].equals("-json")) {
                json = args[i + 1].equals("true");
            }
        }
        PrintWriter pw = new PrintWriter(getOutputStream());
        if (msg.length() > 0) {
            pw.println(msg);
        }
        if ( !json) {
            pw.println("Usage:");
        } else {
            pw.println("{\"commands\":[");
        }
        int     cnt             = 0;
        String  pad             = "\t";
        boolean matchedCategory = false;
        for (Cmd c : commands) {
            String cmd = c.getLine();
            if (match != null) {
                String text  = c.cmd;
                String desc  = null;
                String label = null;
                if (c.category) {
                    matchedCategory = false;
                    text            = c.desc;
                } else {
                    text  = c.cmd;
                    desc  = c.desc;
                    label = c.label;
                    if (desc != null) {
                        desc = desc.toLowerCase();
                    }
                    if (label != null) {
                        label = label.toLowerCase();
                    }
                }
                boolean ok = true;
                text = text.toLowerCase();
                if (exact && !text.equals(match)) {
                    ok = false;
                } else if ( !exact) {
                    if (text.indexOf(match) < 0) {
                        ok = false;
                    }
                    if ( !ok && (label != null)) {
                        ok = label.indexOf(match) >= 0;
                    }
                    if ( !ok && (desc != null)) {
                        ok = desc.indexOf(match) >= 0;
                    }
                }

                if (c.category) {
                    matchedCategory = ok;

                    continue;
                } else {
                    ok = ok || matchedCategory;
                }
                if ( !ok) {
                    continue;
                }
            }
            if ( !raw) {
                cmd = cmd.replaceAll("_nl_", "\n").replaceAll("_tab_", "\n");
            }
            if (json) {
                if (cnt > 0) {
                    pw.println(",");
                }
                if (c.category) {
                    pw.println(Json.mapAndQuote("isCategory", "true",
                            "description", c.desc));
                } else {
                    String argList = "[]";
                    if (c.args != null) {
                        List tmp = new ArrayList();
                        for (Arg arg : c.args) {
                            List<String> attrs = new ArrayList<String>();
                            attrs.add("id");
                            attrs.add(Json.quote(arg.id));
                            attrs.add("description");
                            attrs.add(Json.quote(arg.desc));
                            if (arg.props != null) {
                                for (int i = 0; i < arg.props.length;
                                        i += 2) {
                                    attrs.add(arg.props[i]);
                                    attrs.add(Json.quote(arg.props[i + 1]));
                                }
                            }
                            tmp.add(Json.map(attrs));

                        }
                        argList = Json.list(tmp);
                    }
                    pw.println(Json.map("command", Json.quote(c.cmd),
                                        "label", (c.label != null)
                            ? Json.quote(c.label)
                            : "null", "args", argList, "description",
                                      Json.quote(c.desc)));
                }
            } else {
                if (c.category) {
                    pw.println(c.desc);
                } else {
                    pw.println(pad + cmd);
                }
            }
            if (raw && cmd.startsWith("-db")) {
                break;
            }
            cnt++;
        }
        if (json) {
            pw.println("]}");
        }
        pw.flush();
        pw.close();

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
            usage("Bad argument count for:" + arg, arg, "-exact", "true");

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


        //      System.err.println("ARGS:" + args);
        boolean            addFiles      = files.size() == 0;
        boolean            trim          = false;
        boolean            printFields   = false;

        Filter.FilterGroup subFilter     = null;
        Filter.FilterGroup filterToAddTo = null;
        //info.getFilter();

        boolean      doText          = false;
        String       textHeader      = null;
        String       chunkPattern    = null;
        String       tokenPattern    = null;

        boolean      doArgs          = false;
        int          doArgsCnt       = 0;
        int          doArgsIndex     = 1;
        boolean      doHtml          = false;
        boolean      doHtml2         = false;
        String       startPattern    = null;
        String       endPattern      = null;
        String       htmlPattern     = null;
        String       htmlCols        = null;
        String       htmlSkip        = null;
        String       htmlProps       = null;
        boolean      doJson          = false;
        String       jsonArrayPath   = null,
                     jsonObjectPath  = null;
        boolean      doXml           = false;
        String       xmlArrayPath    = null,
                     xmlObjectPath   = null;

        boolean      doPattern       = false;
        List<String> tokenizeHeader  = null;
        String       tokenizePattern = null;
        if (comment != null) {
            info.setComment(comment);
        }
        if (delimiter != null) {
            info.setDelimiter(delimiter);
        }

        PrintWriter pw        = null;

        boolean     seenPrint = false;
        for (int i = 0; i < args.size(); i++) {
            String arg = args.get(i);
            currentArg = arg;
            try {
                if (arg.equals("-args")) {
                    doArgs = true;

                    continue;
                }
                if (doArgs) {
                    if (pw == null) {
                        pw = new PrintWriter(getOutputStream());
                        pw.print("csvcommands1=");
                    } else {
                        doArgsCnt++;
                        if (doArgsCnt > 4) {
                            pw.print("\n");
                            doArgsCnt = 0;
                            doArgsIndex++;
                            pw.print("csvcommands" + doArgsIndex + "=");
                        } else {
                            pw.print(",");
                        }
                    }
                    arg = arg.replaceAll(",", "\\\\,");
                    pw.print(arg);

                    continue;
                }
                if (arg.equals("-dummy")) {
                    continue;
                }
                if (arg.equals("-html")) {
                    if ( !ensureArg(args, i, 3)) {
                        return false;
                    }
                    doHtml      = true;
                    htmlSkip    = args.get(++i);
                    htmlPattern = args.get(++i);
                    htmlProps   = args.get(++i);

                    continue;
                }
                if (arg.equals("-htmlpattern")) {
                    if ( !ensureArg(args, i, 4)) {
                        return false;
                    }
                    doHtml2      = true;
                    htmlCols     = args.get(++i);
                    startPattern = args.get(++i);
                    endPattern   = args.get(++i);
                    htmlPattern  = args.get(++i);

                    continue;
                }

                if (arg.equals("-text")) {
                    if ( !ensureArg(args, i, 3)) {
                        return false;
                    }
                    doText       = true;
                    textHeader   = args.get(++i);
                    chunkPattern = args.get(++i);
                    tokenPattern = args.get(++i);

                    continue;
                }
                if (arg.equals("-json")) {
                    if ( !ensureArg(args, i, 2)) {
                        return false;
                    }
                    doJson         = true;
                    jsonArrayPath  = args.get(++i);
                    jsonObjectPath = args.get(++i);

                    continue;
                }
                if (arg.equals("-xml")) {
                    if ( !ensureArg(args, i, 1)) {
                        return false;
                    }
                    doXml        = true;
                    xmlArrayPath = args.get(++i);

                    //                    xmlObjectPath = args.get(++i);
                    continue;
                }

                if (arg.equals("-tokenize")) {
                    if ( !ensureArg(args, i, 2)) {
                        return false;
                    }
                    doPattern      = true;
                    tokenizeHeader = StringUtil.split(args.get(++i), ",");
                    tokenizePattern = args.get(++i).replaceAll("_quote_",
                            "\"");

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

                if (arg.equals("-pass")) {
                    info.getProcessor().addProcessor(new Processor.Pass());

                    continue;
                }

                if (arg.equals("-changeline")) {
                    if ( !ensureArg(args, i, 2)) {
                        return false;
                    }
                    info.setChangeString(args.get(++i), args.get(++i));

                    continue;
                }

                if (arg.equals("-changeraw")) {
                    if ( !ensureArg(args, i, 2)) {
                        return false;
                    }
                    changeFrom.add(args.get(++i));
                    changeTo.add(args.get(++i));

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
                    info.getProcessor().addProcessor(
                        new Filter.Start(args.get(++i)));

                    continue;
                }


                if (arg.equals("-stop")) {
                    if ( !ensureArg(args, i, 1)) {
                        return false;
                    }
                    info.getProcessor().addProcessor(
                        new Filter.Stop(args.get(++i)));

                    continue;
                }

                if (arg.equals("-min")) {
                    if ( !ensureArg(args, i, 1)) {
                        return false;
                    }
                    info.getProcessor().addProcessor(
                        new Filter.MinColumns(new Integer(args.get(++i))));

                    continue;
                }

                if (arg.equals("-max")) {
                    if ( !ensureArg(args, i, 1)) {
                        return false;
                    }
                    info.getProcessor().addProcessor(
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
                        info.getProcessor().addProcessor(
                            new Filter.Decimate(start, skip));
                    }

                    continue;
                }

                if (arg.equals("-db")) {
                    if ( !ensureArg(args, i, 1)) {
                        return false;
                    }
                    Hashtable<String, String> props =
                        parseProps(args.get(++i));
                    this.installPlugin = Utils.equals(props.get("-install"),
                            "true") || Utils.equals(props.get("install"),
                                "true");
                    this.nukeDb = Utils.equals(props.get("-nukedb"), "true")
                                  || Utils.equals(props.get("nukedb"),
                                      "true");
                    info.getProcessor().addProcessor(dbXml =
                        new Processor.DbXml(props));

                    info.setMaxRows(30);

                    continue;
                }


                if (arg.equals("-unfurl")) {
                    if ( !ensureArg(args, i, 5)) {
                        return false;
                    }
                    String       mainCol   = args.get(++i);
                    List<String> valueCols = getCols(args.get(++i));
                    String       uniqueCol = args.get(++i);
                    List<String> extraCols = getCols(args.get(++i));
                    info.getProcessor().addProcessor(
                        new Processor.Unfurler(
                            mainCol, valueCols, uniqueCol, extraCols));

                    continue;
                }

                if (arg.equals("-furl")) {
                    if ( !ensureArg(args, i, 3)) {
                        return false;
                    }
                    List<String> valueCols = getCols(args.get(++i));
                    info.getProcessor().addProcessor(
                        new Processor.Furler(
                            valueCols, args.get(++i), args.get(++i)));

                    continue;
                }




                if (arg.equals("-break")) {
                    if ( !ensureArg(args, i, 3)) {
                        return false;
                    }
                    String       label1 = args.get(++i);
                    String       label2 = args.get(++i);
                    List<String> cols   = getCols(args.get(++i));
                    info.getProcessor().addProcessor(
                        new Processor.Breaker(label1, label2, cols));

                    continue;
                }


                if (arg.equals("-sort")) {
                    if ( !ensureArg(args, i, 1)) {
                        return false;
                    }
                    info.getProcessor().addProcessor(
                        new Processor.Sorter(args.get(++i), true));

                    continue;
                }

                if (arg.equals("-descsort")) {
                    if ( !ensureArg(args, i, 1)) {
                        return false;
                    }
                    info.getProcessor().addProcessor(
                        new Processor.Sorter(args.get(++i), false));

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

                    info.getProcessor().addProcessor(
                        new Processor.Joiner(keys1, values1, file, keys2));

                    continue;
                }

                if (arg.equals("-sum")) {
                    if ( !ensureArg(args, i, 3)) {
                        return false;
                    }
                    List<String> keys   = getCols(args.get(++i));
                    List<String> values = getCols(args.get(++i));
                    List<String> extra  = getCols(args.get(++i));
                    info.getProcessor().addProcessor(
                        new Processor.Summer(keys, values, extra));

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

                if (arg.equals("-dups")) {
                    if ( !ensureArg(args, i, 1)) {
                        return false;
                    }
                    List<String> cols = getCols(args.get(++i));
                    info.getProcessor().addProcessor(
                        new Processor.Dups(cols));

                    continue;
                }

                if (arg.equals("-verify")) {
                    info.getProcessor().addProcessor(
                        new Processor.Verifier());

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
                    info.getProcessor().addProcessor(
                        new Processor.Counter(true));

                    continue;
                }

                if (arg.equals("-flag")) {
                    info.getProcessor().addProcessor(
                        new Processor.Counter(true, true));

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


                if (arg.equals("-tab")) {
                    info.setDelimiter(delimiter = "tab");

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
                            StringUtil.split(args.get(++i), ",", true,
                                             true)) {
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
                    info.getProcessor().addProcessor(
                        new Filter.RowCutter(
                            getNumbers(r), arg.equals("-cut")));

                    continue;
                }


                if (arg.equals("-prop")) {
                    if ( !ensureArg(args, i, 2)) {
                        return false;
                    }
                    String flag  = args.get(++i);
                    String value = args.get(++i);
                    info.getProcessor().addProcessor(
                        new Processor.Propper(flag, value));

                    continue;
                }



                if (arg.equals("-rowop")) {
                    if ( !ensureArg(args, i, 3)) {
                        return false;
                    }
                    List<String> keys   = getCols(args.get(++i));
                    List<String> values = getCols(args.get(++i));
                    String       op     = args.get(++i);

                    info.getProcessor().addProcessor(
                        new Processor.RowOperator(keys, values, op));

                    continue;
                }
                /*
                if (arg.equals("-processor")) {
                    if ( !ensureArg(args, i, 1)) {
                        return false;
                    }
                    Class c = Misc.findClass(args.get(++i));
                    info.getProcessor().addProcessor(
                        (Processor) c.newInstance());

                    continue;
                }
                */

                if (arg.equals("-fields")) {
                    printFields = true;

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

                if (arg.equals("-print") || arg.equals("-p")) {
                    if (seenPrint) {
                        continue;
                    }
                    seenPrint = true;
                    info.getProcessor().addProcessor(
                        new Processor.Printer(printFields, trim));

                    continue;
                }


                if (arg.equals("-toxml")) {
                    if ( !ensureArg(args, i, 1)) {
                        return false;
                    }
                    info.getProcessor().addProcessor(
                        new Processor.ToXml(args.get(++i)));

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
                    info.getProcessor().addProcessor(
                        new Processor.Prettifier());

                    continue;
                }

                if (arg.equals("-template")) {
                    if ( !ensureArg(args, i, 4)) {
                        return false;
                    }
                    String prefix   = args.get(++i).replaceAll("_nl_", "\n");
                    String template = args.get(++i).replaceAll("_nl_", "\n");
                    String delim    = args.get(++i).replaceAll("_nl_", "\n");
                    String suffix   = args.get(++i).replaceAll("_nl_", "\n");
                    if (new File(template).exists()) {
                        template = IO.readContents(new File(template));
                    }
                    info.getProcessor().addProcessor(
                        new Processor.Printer(
                            prefix, template, delim, suffix));

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

                if (arg.equals("-notcolumns")) {
                    if ( !ensureArg(args, i, 1)) {
                        return false;
                    }
                    List<String> cols = getCols(args.get(++i));
                    info.getProcessor().addProcessor(
                        new Converter.ColumnNotSelector(cols));

                    continue;
                }

                if (arg.equals("-mergerows")) {
                    if ( !ensureArg(args, i, 3)) {
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


                if (arg.equals("-printheader") || arg.equals("-ph")) {
                    info.getProcessor().addProcessor(
                        new Converter.PrintHeader());

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


                if (arg.equals("-average")) {
                    if ( !ensureArg(args, i, 3)) {
                        return false;
                    }
                    List<String> cols   = getCols(args.get(++i));
                    int          period = Integer.parseInt(args.get(++i));
                    String       label  = args.get(++i);
                    info.getProcessor().addProcessor(
                        new Converter.ColumnAverage(
                            Converter.ColumnAverage.MA, cols, period, label));

                    continue;
                }

                if (arg.equals("-increase")) {
                    if ( !ensureArg(args, i, 2)) {
                        return false;
                    }
                    String col  = args.get(++i);
                    int    step = Integer.parseInt(args.get(++i));
                    info.getProcessor().addProcessor(
                        new Converter.ColumnIncrease(col, step));

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
                    info.getProcessor().addProcessor(
                        new Converter.Padder(count, pad));

                    continue;
                }

                if (arg.equals("-prefix")) {
                    if ( !ensureArg(args, i, 2)) {
                        return false;
                    }
                    List<String> cols = getCols(args.get(++i));
                    String       s    = args.get(++i);
                    info.getProcessor().addProcessor(
                        new Converter.Prefixer(cols, s));

                    continue;
                }

                if (arg.equals("-suffix")) {
                    if ( !ensureArg(args, i, 1)) {
                        return false;
                    }
                    List<String> cols = getCols(args.get(++i));
                    String       s    = args.get(++i);
                    info.getProcessor().addProcessor(
                        new Converter.Suffixer(cols, s));

                    continue;
                }


                if (arg.equals("-explode")) {
                    if ( !ensureArg(args, i, 1)) {
                        return false;
                    }
                    String col = args.get(++i);
                    info.getProcessor().addProcessor(
                        new Processor.Exploder(col));

                    continue;
                }

                if (arg.equals("-gender")) {
                    if ( !ensureArg(args, i, 1)) {
                        return false;
                    }
                    String col = args.get(++i);
                    info.getProcessor().addProcessor(
                        new Converter.Genderizer(col));

                    continue;
                }

                if (arg.equals("-image")) {
                    if ( !ensureArg(args, i, 2)) {
                        return false;
                    }
                    List<String> cols   = getCols(args.get(++i));
                    String       suffix = args.get(++i);
                    info.getProcessor().addProcessor(
                        new Converter.ImageSearch(cols, suffix));

                    continue;
                }

                if (arg.equals("-imagefill")) {
                    if ( !ensureArg(args, i, 3)) {
                        return false;
                    }
                    List<String> cols        = getCols(args.get(++i));
                    String       suffix      = args.get(++i);
                    String       imageColumn = args.get(++i);
                    info.getProcessor().addProcessor(
                        new Converter.ImageSearch(cols, suffix, imageColumn));

                    continue;
                }

                if (arg.equals("-wikidesc") || arg.equals("-desc")) {
                    if ( !ensureArg(args, i, 2)) {
                        return false;
                    }
                    List<String> cols   = getCols(args.get(++i));
                    String       suffix = args.get(++i);
                    info.getProcessor().addProcessor(
                        new Converter.WikiDescSearch(cols, suffix));

                    continue;
                }

                if (arg.equals("-statename")) {
                    if ( !ensureArg(args, i, 1)) {
                        return false;
                    }
                    String col = args.get(++i);
                    info.getProcessor().addProcessor(
                        new Converter.StateNamer(col));

                    continue;
                }

                if (arg.equals("-geocode") || arg.equals("-geocodeaddress")) {
                    if ( !ensureArg(args, i, 3)) {
                        return false;
                    }
                    List<String> cols   = getCols(args.get(++i));
                    String       prefix = args.get(++i).trim();
                    String       suffix = args.get(++i).trim();
                    info.getProcessor().addProcessor(
                        new Converter.Geocoder(cols, prefix, suffix));

                    continue;
                }

                if (arg.equals("-geocodejoin")) {
                    if ( !ensureArg(args, i, 5)) {
                        return false;
                    }
                    String col      = args.get(++i);
                    String filename = args.get(++i);
                    info.getProcessor().addProcessor(
                        new Converter.Geocoder(
                            col, filename, Integer.parseInt(args.get(++i)),
                            Integer.parseInt(args.get(++i)),
                            Integer.parseInt(args.get(++i)), false));

                    continue;
                }



                if (arg.equals("-geocodeaddressdb")) {
                    if ( !ensureArg(args, i, 3)) {
                        return false;
                    }
                    List<String> cols   = getCols(args.get(++i));
                    String       prefix = args.get(++i).trim();
                    String       suffix = args.get(++i).trim();
                    info.getProcessor().addProcessor(
                        new Converter.Geocoder(cols, prefix, suffix, true));

                    continue;
                }

                if (arg.equals("-geocodedb")) {
                    if ( !ensureArg(args, i, 5)) {
                        return false;
                    }
                    String col      = args.get(++i);
                    String filename = args.get(++i);
                    info.getProcessor().addProcessor(
                        new Converter.Geocoder(
                            col, filename, Integer.parseInt(args.get(++i)),
                            Integer.parseInt(args.get(++i)),
                            Integer.parseInt(args.get(++i)), true));

                    continue;
                }

                if (arg.equals("-population")) {
                    if ( !ensureArg(args, i, 3)) {
                        return false;
                    }
                    List<String> cols   = getCols(args.get(++i));
                    String       prefix = args.get(++i).trim();
                    String       suffix = args.get(++i).trim();
                    info.getProcessor().addProcessor(
                        new Converter.Populator(cols, prefix, suffix));

                    continue;
                }

                if (arg.equals("-region")) {
                    if ( !ensureArg(args, i, 1)) {
                        return false;
                    }
                    List<String> cols = getCols(args.get(++i));
                    info.getProcessor().addProcessor(
                        new Converter.Regionator(cols));

                    continue;
                }


                if (arg.equals("-crop")) {
                    if ( !ensureArg(args, i, 2)) {
                        return false;
                    }
                    List<String> cols = getCols(args.get(++i));
                    List<String> patterns = StringUtil.split(args.get(++i),
                                                ",", true, true);
                    info.getProcessor().addProcessor(
                        new Converter.Cropper(cols, patterns));

                    continue;
                }

                if (arg.equals("-change")) {
                    if ( !ensureArg(args, i, 3)) {
                        return false;
                    }
                    List<String> cols    = getCols(args.get(++i));
                    String       pattern = args.get(++i);
                    pattern = convertPattern(pattern);
                    info.getProcessor().addProcessor(
                        new Converter.ColumnChanger(
                            cols, pattern, args.get(++i)));

                    continue;
                }


                if (arg.equals("-endswith")) {
                    if ( !ensureArg(args, i, 2)) {
                        return false;
                    }
                    List<String> cols = getCols(args.get(++i));
                    String       s    = args.get(++i);
                    info.getProcessor().addProcessor(
                        new Converter.ColumnEndsWith(cols, s));

                    continue;
                }

                if (arg.equals("-trim")) {
                    if ( !ensureArg(args, i, 1)) {
                        return false;
                    }
                    List<String> cols = getCols(args.get(++i));
                    info.getProcessor().addProcessor(
                        new Converter.ColumnTrimmer(cols));

                    continue;
                }


                if (arg.equals("-extract")) {
                    if ( !ensureArg(args, i, 4)) {
                        return false;
                    }
                    int    col     = new Integer(args.get(++i));
                    String pattern = args.get(++i);
                    String replace = args.get(++i);
                    String name    = args.get(++i);
                    pattern = convertPattern(pattern);
                    info.getProcessor().addProcessor(
                        new Converter.ColumnExtracter(
                            col, pattern, replace, name));

                    continue;
                }


                if (arg.equals("-truncate")) {
                    if ( !ensureArg(args, i, 3)) {
                        return false;
                    }
                    int    col    = new Integer(args.get(++i));
                    int    length = new Integer(args.get(++i));
                    String suffix = args.get(++i);
                    info.getProcessor().addProcessor(
                        new Converter.Truncater(col, length, suffix));

                    continue;
                }


                if (arg.equals("-changerow")) {
                    if ( !ensureArg(args, i, 4)) {
                        return false;
                    }
                    List<Integer> rows    = getNumbers(args.get(++i));
                    List<String>  cols    = getCols(args.get(++i));
                    String        pattern = args.get(++i);
                    pattern = convertPattern(pattern);
                    info.getProcessor().addProcessor(
                        new Converter.RowChanger(
                            rows, cols, pattern, args.get(++i)));

                    continue;
                }


                if (arg.equals("-convertdate")) {
                    if ( !ensureArg(args, i, 3)) {
                        return false;
                    }
                    String col  = args.get(++i);
                    String sdf1 = args.get(++i);
                    String sdf2 = args.get(++i);
                    info.getProcessor().addProcessor(
                        new Converter.DateConverter(
                            col, new SimpleDateFormat(sdf1),
                            new SimpleDateFormat(sdf2)));

                    continue;
                }

                if (arg.equals("-extractdate")) {
                    if ( !ensureArg(args, i, 4)) {
                        return false;
                    }
                    String col  = args.get(++i);
                    String sdf  = args.get(++i);
                    String tz   = args.get(++i);
                    String what = args.get(++i);
                    info.getProcessor().addProcessor(
                        new Converter.DateExtracter(col, sdf, tz, what));

                    continue;
                }



                if (arg.equals("-before")) {
                    if ( !ensureArg(args, i, 4)) {
                        return false;
                    }
                    int    col  = Integer.parseInt(args.get(++i));
                    String sdf1 = args.get(++i);
                    String date = args.get(++i);
                    String sdf2 = args.get(++i);
                    Date   dttm = null;
                    if (date.equals("now")) {
                        dttm = new Date();
                    } else if (sdf2.length() > 0) {
                        SimpleDateFormat sdf = new SimpleDateFormat(sdf2);
                        dttm = sdf.parse(date);
                    } else {
                        dttm = Utils.parseDate(date);
                    }
                    info.getProcessor().addProcessor(
                        new Converter.DateBefore(
                            col, new SimpleDateFormat(sdf1), dttm));

                    continue;
                }

                if (arg.equals("-after")) {
                    if ( !ensureArg(args, i, 4)) {
                        return false;
                    }
                    int    col  = Integer.parseInt(args.get(++i));
                    String sdf1 = args.get(++i);
                    String date = args.get(++i);
                    String sdf2 = args.get(++i);
                    Date   dttm = null;
                    if (date.equals("now")) {
                        dttm = new Date();
                    } else if (sdf2.length() > 0) {
                        SimpleDateFormat sdf = new SimpleDateFormat(sdf2);
                        dttm = sdf.parse(date);
                    } else {
                        dttm = Utils.parseDate(date);
                    }
                    info.getProcessor().addProcessor(
                        new Converter.DateAfter(
                            col, new SimpleDateFormat(sdf1), dttm));

                    continue;
                }

                if (arg.equals("-latest")) {
                    if ( !ensureArg(args, i, 3)) {
                        return false;
                    }
                    List<String> cols = getCols(args.get(++i));
                    String       col  = args.get(++i);
                    String       sdf  = args.get(++i);
                    info.getProcessor().addProcessor(
                        new Converter.DateLatest(
                            cols, col, new SimpleDateFormat(sdf)));

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
                    if ( !ensureArg(args, i, 3)) {
                        return false;
                    }
                    info.getProcessor().addProcessor(
                        new Converter.ColumnSplitter(
                            args.get(++i), args.get(++i),
                            StringUtil.split(args.get(++i), ",")));

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
                            args.get(++i), args.get(++i)));

                    continue;
                }


                if (arg.equals("-shift")) {
                    if ( !ensureArg(args, i, 3)) {
                        return false;
                    }
                    List<Integer> rows  = getNumbers(args.get(++i));
                    int           col   = Integer.parseInt(args.get(++i));
                    int           count = Integer.parseInt(args.get(++i));


                    info.getProcessor().addProcessor(
                        new Converter.Shifter(rows, col, count));

                    continue;
                }


                if (arg.equals("-generate")) {
                    if ( !ensureArg(args, i, 3)) {
                        return false;
                    }
                    info.getProcessor().addProcessor(
                        new Converter.Generator(
                            args.get(++i), Double.parseDouble(args.get(++i)),
                            Double.parseDouble(args.get(++i))));

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
                    List<String> cols = getCols(args.get(++i));
                    info.getProcessor().addProcessor(
                        new Converter.ColumnScaler(
                            cols, Double.parseDouble(args.get(++i)),
                            Double.parseDouble(args.get(++i)),
                            Double.parseDouble(args.get(++i))));

                    continue;
                }


                if (arg.equals("-decimals")) {
                    if ( !ensureArg(args, i, 2)) {
                        return false;
                    }
                    List<String> idxs = getCols(args.get(++i));
                    info.getProcessor().addProcessor(
                        new Converter.Decimals(
                            idxs, new Integer(args.get(++i))));

                    continue;
                }

                if (arg.equals("-copy")) {
                    if ( !ensureArg(args, i, 2)) {
                        return false;
                    }
                    info.getProcessor().addProcessor(
                        new Converter.ColumnCopier(
                            args.get(++i), args.get(++i)));

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

                if (arg.equals("-splat")) {
                    if ( !ensureArg(args, i, 4)) {
                        return false;
                    }
                    String key       = args.get(++i);
                    String value     = args.get(++i);
                    String delimiter = args.get(++i);
                    String name      = args.get(++i);
                    info.getProcessor().addProcessor(
                        new Processor.Splatter(key, value, delimiter, name));

                    continue;
                }
                if (arg.equals("-delta")) {
                    if ( !ensureArg(args, i, 2)) {
                        return false;
                    }
                    List<String> keyidxs = getCols(args.get(++i));
                    List<String> idxs    = getCols(args.get(++i));
                    info.getProcessor().addProcessor(
                        new Converter.Delta(keyidxs, idxs));

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

                if (arg.equals("-js")) {
                    if ( !ensureArg(args, i, 1)) {
                        return false;
                    }
                    js.append(args.get(++i));
                    js.append("\n");

                    continue;
                }

                if (arg.equals("-func")) {
                    if ( !ensureArg(args, i, 2)) {
                        return false;
                    }
                    info.getProcessor().addProcessor(
                        new Converter.ColumnFunc(
                            js.toString(), args.get(++i), args.get(++i)));

                    continue;
                }



                if (arg.equals("-mercator")) {
                    if ( !ensureArg(args, i, 1)) {
                        return false;
                    }
                    List<String> idxs = getCols(args.get(++i));
                    info.getProcessor().addProcessor(
                        new Converter.Mercator(idxs));

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
                if (arg.equals("-md")) {
                    if ( !ensureArg(args, i, 2)) {
                        return false;
                    }
                    List<String> idxs   = getCols(args.get(++i));
                    String       action = args.get(++i);
                    info.getProcessor().addProcessor(new Converter.MD(idxs,
                            action));

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

                if (arg.equals("-priorprefix")) {
                    if ( !ensureArg(args, i, 3)) {
                        return false;
                    }
                    int    col     = Integer.parseInt(args.get(++i));
                    String pattern = args.get(++i);
                    String delim   = args.get(++i);
                    info.getProcessor().addProcessor(
                        new Converter.PriorPrefixer(col, pattern, delim));

                    continue;
                }

                if (arg.equals("-set")) {
                    if ( !ensureArg(args, i, 3)) {
                        return false;
                    }
                    List<String> cols = getCols(args.get(++i));
                    List<String> rows = getCols(args.get(++i));
                    info.getProcessor().addProcessor(
                        new Converter.ColumnSetter(
                            cols, rows, args.get(++i)));

                    continue;
                }

                if (arg.equals("-number")) {
                    info.getProcessor().addProcessor(new Converter.Number());

                    continue;
                }


                if (arg.equals("-letter")) {
                    info.getProcessor().addProcessor(new Converter.Letter());

                    continue;
                }

                if (arg.equals("-uuid")) {
                    info.getProcessor().addProcessor(new Converter.UUID());

                    continue;
                }

                if (arg.equals("-setcol")) {
                    if ( !ensureArg(args, i, 4)) {
                        return false;
                    }
                    int    col1    = Integer.parseInt(args.get(++i));
                    String pattern = args.get(++i);
                    int    col2    = Integer.parseInt(args.get(++i));
                    String what    = args.get(++i);
                    info.getProcessor().addProcessor(
                        new Converter.ColumnPatternSetter(
                            col1, pattern, col2, what));

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
                                  new Filter.PatternFilter(col, pattern,
                                      true));

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

                if (arg.equals("-groupfilter")) {
                    if ( !ensureArg(args, i, 4)) {
                        return false;
                    }
                    List<String> cols = getCols(args.get(++i));
                    info.getProcessor().addProcessor(
                        new Processor.GroupFilter(
                            cols, Integer.parseInt(args.get(++i)),
                            CsvOperator.getOperator(args.get(++i)),
                            args.get(++i)));

                    continue;
                }


                if (arg.equals("-eq")) {
                    if ( !ensureArg(args, i, 2)) {
                        return false;
                    }
                    handlePattern(
                        info, filterToAddTo,
                        new Filter.ValueFilter(
                            getCols(args.get(++i)),
                            Filter.ValueFilter.OP_EQUALS,
                            Double.parseDouble(args.get(++i))));

                    continue;
                }

                if (arg.equals("-ne")) {
                    if ( !ensureArg(args, i, 2)) {
                        return false;
                    }
                    handlePattern(
                        info, filterToAddTo,
                        new Filter.ValueFilter(
                            getCols(args.get(++i)),
                            Filter.ValueFilter.OP_NOTEQUALS,
                            Double.parseDouble(args.get(++i))));

                    continue;
                }




                if (arg.equals("-lt")) {
                    if ( !ensureArg(args, i, 2)) {
                        return false;
                    }
                    handlePattern(
                        info, filterToAddTo,
                        new Filter.ValueFilter(
                            getCols(args.get(++i)), Filter.ValueFilter.OP_LT,
                            Double.parseDouble(args.get(++i))));

                    continue;
                }

                if (arg.equals("-gt")) {
                    if ( !ensureArg(args, i, 2)) {
                        return false;
                    }
                    handlePattern(
                        info, filterToAddTo,
                        new Filter.ValueFilter(
                            getCols(args.get(++i)), Filter.ValueFilter.OP_GT,
                            Double.parseDouble(args.get(++i))));

                    continue;
                }


                if (arg.equals("-defined")) {
                    if ( !ensureArg(args, i, 1)) {
                        return false;
                    }
                    handlePattern(
                        info, filterToAddTo,
                        new Filter.ValueFilter(
                            getCols(args.get(++i)),
                            Filter.ValueFilter.OP_DEFINED, 0));

                    continue;
                }
                if (arg.equals("-maxvalue")) {
                    if ( !ensureArg(args, i, 2)) {
                        return false;
                    }
                    String key   = args.get(++i);
                    String value = args.get(++i);
                    info.getProcessor().addProcessor(
                        new Processor.MaxValue(key, value));

                    continue;
                }



                if (arg.equals("-quit") || arg.equals("-q")) {
                    String last = args.get(args.size() - 1);
                    if (last.equals("-print") || last.equals("-p")) {
                        info.getProcessor().addProcessor(
                            new Processor.Printer(printFields, trim));

                    } else if (last.equals("-table")) {
                        info.getProcessor().addProcessor(
                            new Processor.Html());
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
                                      idx).trim(), arg.substring(idx
                                      + 2).trim(), true));

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


                if (arg.length() == 0) {
                    throw new IllegalArgumentException("Unknown argument:"
                            + arg);
                }
                if (addFiles) {
                    files.add(arg);
                } else {
                    throw new IllegalArgumentException("Unknown arg:" + arg);
                }
            } catch (Exception exc) {
                System.err.println("Error processing arg:" + arg);

                throw exc;
            }
        }

        if (doArgs) {
            if (pw != null) {
                pw.print("\n");
            }
            pw.close();

            return false;
        }

        if (doHtml) {
            Hashtable<String, String> props = parseProps(htmlProps);
            tokenizedRows.add(tokenizeHtml(files.get(0), htmlSkip,
                                           htmlPattern, props));
        } else if (doHtml2) {
            String s = null;
            if (files.size() > 0) {
                s = IO.readInputStream(getInputStream(files.get(0)));
            } else if (inputStream != null) {
                s = IO.readInputStream(inputStream);
            } else {
                throw new IllegalArgumentException("No file given");
            }
            tokenizedRows.add(tokenizeHtmlPattern(s, htmlCols, startPattern,
                    endPattern, htmlPattern));
        } else if (doText) {
            tokenizedRows.add(tokenizeText(files.get(0), textHeader,
                                           chunkPattern, tokenPattern));
        } else if (doJson) {
            //xxxx
            String s = null;
            if (files.size() > 0) {
                s = IO.readInputStream(getInputStream(files.get(0)));
            } else if (inputStream != null) {
                s = IO.readInputStream(inputStream);
            } else {
                throw new Exception("Error processing json: no file given");
            }
            tokenizedRows.add(tokenizeJson(s, jsonArrayPath, jsonObjectPath));
        } else if (doXml) {
            //xxxx
            String s = null;
            if (files.size() > 0) {
                s = IO.readInputStream(getInputStream(files.get(0)));
                //              System.err.println("xml file:" + files.get(0));
                //              System.err.println("xml:" + s.substring(0,100));
            } else if (inputStream != null) {
                s = IO.readInputStream(inputStream);
            } else {
                throw new Exception("Error processing xml: no file given");
            }
            tokenizedRows.add(tokenizeXml(s, xmlArrayPath, xmlObjectPath));

        } else if (doPattern) {
            tokenizedRows.add(tokenizePattern(files.get(0), tokenizeHeader,
                    tokenizePattern));
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

                int    step  = 1;
                String right = StringUtil.split(tok, "-", true, true).get(1);
                if (right.indexOf(":") >= 0) {
                    List<String> tmp = StringUtil.split(right, ":", true,
                                           true);
                    right = tmp.get(0);
                    if (tmp.size() > 1) {
                        step = Integer.parseInt(tmp.get(1));
                    }
                }
                int to = Integer.parseInt(right);
                for (int i = from; i <= to; i += step) {
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
        s = s.replaceAll("\n", " ");
        List<String> toks = Utils.parseCommandLine(s);
        //      System.err.println("s:" + s);
        //      System.err.println("toks:" + toks);
        Hashtable<String, String> props = new Hashtable<String, String>();
        for (int j = 0; j < toks.size(); j += 2) {
            if (j >= toks.size() - 1) {
                StringBuilder err = new StringBuilder();
                for (int k = 0; k < toks.size(); k += 2) {
                    if (k >= toks.size() - 1) {
                        err.append("\t" + toks.get(k) + "=NONE\n");
                    } else {
                        err.append("\t" + toks.get(k) + "=" + toks.get(k + 1)
                                   + "\n");
                    }
                }

                throw new IllegalArgumentException(
                    "Error: Odd number of arguments:\n" + err);
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

    /**
     * _more_
     *
     * @param p _more_
     *
     * @return _more_
     */
    public static String convertPattern(String p) {
        if (p == null) {
            return null;
        }
        p = p.replaceAll("_leftparen_", "\\\\(").replaceAll("_rightparen_",
                         "\\\\)");
        p = p.replaceAll("_leftbracket_",
                         "\\\\[").replaceAll("_rightbracket_", "\\\\]");
        p = p.replaceAll("_dot_", "\\\\.");
        p = p.replaceAll("_dollar_", "\\\\\\$");
        p = p.replaceAll("_dot_", "\\\\.");
        p = p.replaceAll("_star_", "\\\\*");
        p = p.replaceAll("_plus_", "\\\\+");
        p = p.replaceAll("_nl_", "\n");
        p = p.replaceAll("_quote_", "\"");

        return p;
    }


}
