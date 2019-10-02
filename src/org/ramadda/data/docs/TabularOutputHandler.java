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

package org.ramadda.data.docs;


import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.*;

import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.output.*;
import org.ramadda.service.*;
import org.ramadda.util.FileInfo;
import org.ramadda.util.GoogleChart;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Json;
import org.ramadda.util.Utils;

import org.ramadda.util.XlsUtil;

import org.ramadda.util.text.CsvUtil;
import org.ramadda.util.text.Filter;
import org.ramadda.util.text.Processor;
import org.ramadda.util.text.SearchField;
import org.ramadda.util.text.TextReader;


import org.w3c.dom.*;

import ucar.unidata.ui.ImageUtils;



import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.awt.Color;
import java.awt.Font;
import java.awt.Image;

import java.awt.image.*;

import java.io.*;
import java.io.File;

import java.net.*;


import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;



/**
 */
public class TabularOutputHandler extends OutputHandler {

    /** _more_ */
    public static final int MAX_ROWS = 100;

    /** _more_ */
    public static final int MAX_COLS = 100;

    /** _more_ */
    public static final OutputType OUTPUT_XLS_JSON =
        new OutputType("XLS to JSON", "xls_json", OutputType.TYPE_FEEDS, "",
                       "/media/xls.png");

    /** _more_ */
    public static final OutputType OUTPUT_XLS_HTML =
        new OutputType("Show Spreadsheet", "xls_html", OutputType.TYPE_VIEW,
                       "", "fa-file-excel");


    /** _more_ */
    public static final OutputType OUTPUT_CONVERT_FORM =
        new OutputType("Convert Spreadsheet", "csv_convert_form",
                       OutputType.TYPE_VIEW, "", "fa-file-excel");

    /** _more_ */
    public static final OutputType OUTPUT_CONVERT_PROCESS =
        new OutputType("Convert Spreadsheet", "csv_convert_process",
                       OutputType.TYPE_VIEW, "", "fa-file-excel");


    /**
     * _more_
     */
    public TabularOutputHandler() {}

    /**
     * _more_
     *
     *
     * @param repository _more_
     * @param element _more_
     * @throws Exception _more_
     */
    public TabularOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        addType(OUTPUT_XLS_HTML);
        addType(OUTPUT_XLS_JSON);
        addType(OUTPUT_CONVERT_FORM);
        addType(OUTPUT_CONVERT_PROCESS);
    }





    /**
     * _more_
     *
     * @param request _more_
     * @param state _more_
     * @param links _more_
     *
     *
     * @throws Exception _more_
     */
    public void getEntryLinks(Request request, State state, List<Link> links)
            throws Exception {
        Entry entry = state.getEntry();
        if (entry == null) {
            return;
        }
        boolean isTabular = isTabular(entry);
        if ( !isTabular) {
            if ( !entry.isFile()) {
                return;
            }
            String path = entry.getResource().getPath();
            if (path.endsWith(".xls") || path.endsWith(".xlsx")
                    || path.endsWith(".csv")) {
                isTabular = true;
            }
        }

        if (isTabular) {
            links.add(makeLink(request, state.getEntry(), OUTPUT_XLS_HTML));
            links.add(makeLink(request, state.getEntry(),
                               OUTPUT_CONVERT_FORM));

            links.add(makeLink(request, state.getEntry(), OUTPUT_XLS_JSON));
        }
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param outputType _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public Result outputEntry(Request request, OutputType outputType,
                              Entry entry)
            throws Exception {
        if (outputType.equals(OUTPUT_CONVERT_FORM)) {
            return outputConvertForm(request, entry);
        }
        if (outputType.equals(OUTPUT_CONVERT_PROCESS)) {
            return outputConvertProcess(request, entry);
        }


        if (outputType.equals(OUTPUT_XLS_JSON)) {
            try {
                return outputEntryJson(request, outputType, entry);
            } catch (org.apache.poi.hssf.OldExcelFormatException exc) {
                StringBuilder sb = new StringBuilder();
                sb.append(
                    Json.map(
                        "error",
                        Json.quote("Old Excel format not supported")));
                request.setReturnFilename(entry.getName() + ".json");
                Result result = new Result("", sb);
                result.setShouldDecorate(false);
                result.setMimeType("application/json");

                return result;
            }
        }

        //        System.err.println("TabularOutputHandler.outputEntry");

        return new Result("",
                          new StringBuffer(getHtmlDisplay(request,
                              new Hashtable(), entry)));
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputConvertForm(Request request, Entry entry)
            throws Exception {
        StringBuilder sb = new StringBuilder();
        getPageHandler().entrySectionOpen(request, entry, sb, "");
        StringBuilder js = new StringBuilder();
        js.append("var convertCsvEntry = '" + entry.getId() + "';\n");
        String lastInput =
            (String) getSessionManager().getSessionProperty(request,
                "csv.lastinput." + entry.getId());
        if ((lastInput == null)
                && entry.getTypeHandler().isType("type_document_tabular")) {
            lastInput =
                (String) entry.getValue(TabularTypeHandler.IDX_CONVERT);
        }
        if (lastInput != null) {
            lastInput = lastInput.replaceAll("\n", "_newline_");
            lastInput = lastInput.replaceAll("\r", "_newline_");
	    //	    System.err.println("before:" + lastInput);
            lastInput = lastInput.replaceAll("\\\\", "\\\\\\");
	    lastInput = lastInput.replaceAll("\"", "\\\\\"");
	    //	    System.err.println("after:" + lastInput);
            //            System.err.println("last input:"+ lastInput);
            js.append("var convertCsvLastInput =\"" + lastInput + "\";\n");
        } else {
            js.append("var convertCsvLastInput =null;\n");
        }
        HtmlUtils.script(sb, js.toString());
        sb.append(HtmlUtils.div("", HtmlUtils.id("convertcsv_div")));
        HtmlUtils.importJS(sb,
                           getRepository().getUrlBase()
                           + "/media/convertcsv.js");
        getPageHandler().entrySectionClose(request, entry, sb);

        return new Result("", sb);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param s _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Result makeHtmlResult(Request request, String s)
            throws Exception {
        s = new String(Utils.encodeBase64(s));
        s = Json.mapAndQuote("html", s);

        return new Result(s, "application/json");
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     * @param f _more_
     *
     * @throws Exception _more_
     */
    private void addFileLink(Request request, StringBuffer sb, File f)
            throws Exception {
        String id = getEntryManager().getProcessFileTypeHandler().getSynthId(
                        getEntryManager().getProcessEntry(),
                        getStorageManager().getProcessDir().toString(), f);
        String url = HtmlUtils.url(
                         request.getAbsoluteUrl(
                             getRepository().URL_ENTRY_SHOW), ARG_ENTRYID,
                                 id);
        sb.append(HtmlUtils.href(url, f.getName(), "target=_output"));
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputConvertProcess(Request request, Entry entry)
            throws Exception {

        File destDir = getCurrentProcessingDir(request, true,
                           "CSV Processing");

        if (request.get("clearoutput", false)) {
            for (File f : destDir.listFiles()) {
                if (f.isDirectory()) {
                    for (File f2 : f.listFiles()) {
                        f2.delete();
                    }
                }
                if ( !f.getName().startsWith(".")) {
                    f.delete();
                }
            }

            return makeHtmlResult(request, "OK, files cleared");
        }

        if (request.get("listoutput", false)) {
            StringBuffer sb  = new StringBuffer("<ul>");
            int          cnt = 0;
            for (File f : destDir.listFiles()) {
                if (f.getName().startsWith(".")) {
                    continue;
                }
                sb.append("<li>");
                addFileLink(request, sb, f);
                cnt++;
                if (f.isDirectory()) {
                    sb.append("<ul>");
                    for (File f2 : f.listFiles()) {
                        sb.append("<li>");
                        addFileLink(request, sb, f2);
                    }
                    sb.append("</ul>");
                }
            }
            sb.append("</ul>");
            if (cnt == 0) {
                sb = new StringBuffer("No files");
            }

            return makeHtmlResult(request, sb.toString());
        }


        if (request.get("stop", false)) {
            CsvUtil csvUtil =
                (CsvUtil) getSessionManager().getSessionProperty(request,
                    "csvutil");
            if (csvUtil != null) {
                csvUtil.stopRunning();
            }
            String s = Json.mapAndQuote("message", "ok");

            return new Result(s, "application/json");
        }

        boolean download  = request.get("download", false);
        boolean save      = request.get("save", false);
        String  lastInput = request.getString("lastinput", (String) null);
        if (lastInput != null) {
            getSessionManager().putSessionProperty(request,
                    "csv.lastinput." + entry.getId(), lastInput);
            if (save && getAccessManager().canEditEntry(request, entry)
                    && entry.getTypeHandler().isType(
                        "type_document_tabular")) {
                entry.setValue(TabularTypeHandler.IDX_CONVERT, lastInput);
                getEntryManager().updateEntry(request, entry);
            }
        }



        try {
            String processEntryId =
                getStorageManager().getProcessDirEntryId(destDir.getName());

            List<String> newFiles   = new ArrayList<String>();
            String       lastResult = "";
            Entry        theEntry   = entry;
            String commandString    = request.getString("commands", "");

            List<StringBuilder> lines =
                Utils.parseMultiLineCommandLine(commandString);

            if ( !download) {
                StringBuilder sb = new StringBuilder();
                for (StringBuilder lineSB : lines) {
                    String line = lineSB.toString().trim();
                    if (line.length() == 0) {
                        continue;
                    }
                    if (line.startsWith("#")) {
                        continue;
                    }
                    sb.append(line);
                    sb.append(" ");
                }
                lines = new ArrayList<StringBuilder>();
                lines.add(sb);
            }

            if (lines.size() == 0) {
                lines.add(new StringBuilder());
            }
            if (request.defined("csvoutput")) {
                lines.get(lines.size() - 1).append(" "
                          + request.getString("csvoutput"));
            }
            CsvUtil prevCsvUtil = null;


            for (StringBuilder lineSB : lines) {
                String line = lineSB.toString();
                if (line.startsWith("#")) {
                    continue;
                }

                List<String> args1 = Utils.parseCommandLine(line);
                if (line.startsWith("-stop")) {
                    break;
                }
                String runDirPrefix = request.getString("rundir", "run");
                //                System.err.println("line:"+ line);
                //                System.err.println("args:"+ args1);
                List<String> args = new ArrayList<String>();
                for (int i = 0; i < args1.size(); i++) {
                    String arg = args1.get(i);
                    if (arg.startsWith("entry:")) {
                        Entry fileEntry =
                            getEntryManager().getEntry(request,
                                arg.substring("entry:".length()));
                        if (fileEntry == null) {
                            throw new IllegalArgumentException(
                                "Could not find " + arg);
                        }
                        if (fileEntry.getFile() == null) {
                            throw new IllegalArgumentException(
                                "Entry not a file  " + arg);
                        }
                        arg = fileEntry.getFile().toString();
                    } else if (arg.equals("-run")) {
                        runDirPrefix = args1.get(++i);

                        continue;
                    }
                    args.add(arg);
                }
                if (download) {
                    if ( !args.contains("-print")
                            && !args.contains("-explode")
                            && !args.contains("-db")) {
                        args.add("-print");
                    }
                }

                File runDir = null;
                for (int i = 0; true; i++) {
                    runDir = new File(IOUtil.joinDir(destDir, ((i == 0)
                            ? runDirPrefix
                            : (runDirPrefix + i))));
                    if ( !runDir.exists()) {
                        //                        runDir.mkdir();
                        break;
                    }
                }

                List<Entry> entries = new ArrayList<Entry>();
                if (request.get("applysiblings", false)) {
                    entries.add(theEntry);
                    entries.addAll(getWikiManager().getEntries(request, null,
                            theEntry, WikiManager.ID_SIBLINGS, null));
                } else {
                    entries.add(theEntry);
                }
                if (download) {
                    request.put("applysiblings", "true");
                }
                newFiles = new ArrayList<String>();
                //                System.err.println("args:" + args + " entries:"+ entries);
                CsvUtil csvUtil = new CsvUtil(args, runDir);
                if (prevCsvUtil != null) {
                    csvUtil.initWith(prevCsvUtil);
                }
                prevCsvUtil = csvUtil;
                getSessionManager().putSessionProperty(request, "csvutil",
                        csvUtil);
                for (Entry e : entries) {
                    lastResult = outputConvertProcessInner(request, e,
                            csvUtil, destDir, runDir, args, newFiles);
                    if ( !csvUtil.getOkToRun()) {
                        break;
                    }
                }
                if ( !csvUtil.getOkToRun()) {
                    lastResult = "stopped";
                    String s = new String(
					  Utils.encodeBase64(lastResult));
                    s = Json.mapAndQuote("result", s);

                    return new Result(s, "application/json");
                }
                //                System.err.println(" new files:" + newFiles);
                if (newFiles.size() > 0) {
                    File f = new File(newFiles.get(0));
                    String id =
                        getEntryManager().getProcessFileTypeHandler()
                            .getSynthId(getEntryManager().getProcessEntry(),
                                        getStorageManager().getProcessDir()
                                            .toString(), f);

                    theEntry = getEntryManager().getEntry(request, id);
                }
            }

            if (newFiles.size() > 0) {
                StringBuffer html = new StringBuffer();
                for (String newFile : newFiles) {
                    File f = new File(newFile);
                    /*
                    File dir = f.getParentFile();
                    String xml = "<entry name=\"foo\"><description><![CDATA[Hello]]></description></entry>";
                    IOUtil.writeFile(IOUtil.joinDir(dir,"." + f.getName() +".ramadda.xml"), xml);
                    System.err.println(IOUtil.joinDir(dir,"." + f.getName() +".ramadda.xml"));
                    */

                    String id =
                        getEntryManager().getProcessFileTypeHandler()
                            .getSynthId(getEntryManager().getProcessEntry(),
                                        getStorageManager().getProcessDir()
                                            .toString(), f);
                    String url =
                        HtmlUtils.url(
                            request.getAbsoluteUrl(
                                getRepository().URL_ENTRY_SHOW), ARG_ENTRYID,
                                    id);


                    if (newFile.endsWith(".csv")) {
                        //                        url += "&output=" + OUTPUT_CONVERT_FORM;
                    }
                    html.append(HtmlUtils.href(url, f.getName(), "target=_output"));
                    String getUrl =   HtmlUtils.url(
                                                    request.makeUrl(getRepository().URL_ENTRY_GET) + "/"
                                                    + f.getName(), ARG_ENTRYID, id);
                    html.append("  ");
                    html.append(HtmlUtils.href(getUrl,"Download"));
                    if(request.getUser().getAdmin()) {
                        html.append(" File on server: " +HtmlUtils.input("",f));
                    }
                    //If they are creating point data then add an add entry link
                    //                    if (newFile.endsWith(".csv") && args.contains("-addheader")) {
                    //                        url += "&output=" + OUTPUT_CONVERT_FORM;
                    //                    }
                    html.append("<br>");
                }
                String urlParent =
                    HtmlUtils.url(
                        request.getAbsoluteUrl(
                            getRepository().URL_ENTRY_SHOW), ARG_ENTRYID,
                                getStorageManager().getEncodedProcessDirEntryId(destDir.getName()));
                html.append(HtmlUtils.href(urlParent, "View All",
                                           "target=_output"));
                String s = Json.mapAndQuote("url",
                                            html.toString().replaceAll("\"",
                                                "\\\""));

                return new Result(s, "application/json");
            }
            String s = new String(Utils.encodeBase64(lastResult==null?"":lastResult));
            s = Json.mapAndQuote("result", s);

            return new Result(s, "application/json");
        } catch (Exception exc) {
            String s = new String(
				  Utils.encodeBase64(exc.toString()));
            s = Json.mapAndQuote("error", s);
            exc.printStackTrace();

            return new Result(s, "application/json");

        }

    }

    /**
     * _more_
     *
     * @param request _more_
     *         @param entry _more_
     * @param csvUtil _more_
     * @param destDir _more_
     * @param runDir _more_
     * @param args _more_
     * @param newFiles _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String outputConvertProcessInner(Request request, Entry entry,
                                            CsvUtil csvUtil, File destDir,
                                            File runDir, List<String> args,
                                            List<String> newFiles)
            throws Exception {

        try {
            List<String> files = new ArrayList<String>();
            files.add(entry.getResource().getPath());
            OutputStream          os       = null;
            ByteArrayOutputStream bos      = null;
            File                  f        = null;
            boolean               download = request.get("download", false);
            if (download) {
                f = getStorageManager().makeTmpFile(
                    runDir,
                    IOUtil.stripExtension(
                        getStorageManager().getFileTail(
                            entry.getResource().getPath())) + ".csv");
            } else {
                os = bos = new ByteArrayOutputStream();
            }

            //        System.err.println("file:" + f + " os:" + os);
            csvUtil.setOutputStream(os);
            csvUtil.setOutputFile(f);
            csvUtil.run(files);
            if ( !csvUtil.getOkToRun()) {
                return "stopped";
            }
            newFiles.addAll(csvUtil.getNewFiles());
            if (csvUtil.getNukeDb()) {
                request.ensureAdmin();
                String sql = "drop table db_" + csvUtil.getDbId();
                try {
                    getRepository().getDatabaseManager().executeAndClose(sql);
                } catch (Exception exc) {}
            }

            if (csvUtil.getInstallPlugin()) {
                request.ensureAdmin();
                for (String file : csvUtil.getNewFiles()) {
                    if (file.endsWith("db.xml")) {
                        getRepository().getPluginManager().installPlugin(
                            file);
                        getRepository().clearCache();
                    }
                }
            }
            IOUtil.close(os);
            if (download) {
                return null;
            }

            return new String(bos.toByteArray());
        } finally {
            getSessionManager().removeSessionProperty(request, "csvutil");
        }
    }


    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */

    public HashSet<Integer> getSheetsToShow(String s) {
        HashSet<Integer> sheetsToShow = null;
        if (Utils.stringDefined(s)) {
            List<String> sheetsStr = StringUtil.split(s, ",", true, true);
            if (sheetsStr.size() > 0) {
                sheetsToShow = new HashSet<Integer>();
                for (String tok : sheetsStr) {
                    sheetsToShow.add(Integer.parseInt(tok));
                }
            }
        }

        return sheetsToShow;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param outputType _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Result outputEntryJson(Request request, OutputType outputType,
                                   Entry entry)
            throws Exception {
        StringBuilder sb   = new StringBuilder();

        String        file = null;
        if (entry.isFile()) {
            file = entry.getFile().toString();
        }


        HashSet<Integer> sheetsToShow = null;
        if (isTabular(entry)) {
            TabularTypeHandler typeHandler =
                (TabularTypeHandler) entry.getTypeHandler();
            sheetsToShow = getSheetsToShow(typeHandler.getSheets(entry));
        }

        final List<String> sheets         = new ArrayList<String>();
        TabularVisitor     tabularVisitor = new TabularVisitor() {
            @Override
            public boolean visit(TextReader textReader, String sheet,
                                 List<List<Object>> rows) {
                List<String> jrows = new ArrayList<String>();
                for (List<Object> cols : rows) {
                    List<String> quoted = new ArrayList<String>();
                    for (Object col : cols) {
                        if (col == null) {
                            col = "null";
                        }
                        String s = col.toString();
                        s = s.replaceAll("\"", "&quot;");
                        quoted.add(Json.quote(s));
                    }
                    jrows.add(Json.list(quoted));
                }
                sheets.add(Json.map("name", Json.quote(sheet), "rows",
                                    Json.list(jrows)));

                return true;
            }
        };

        List       props      = new ArrayList();

        TextReader textReader = new TextReader();
        textReader.setSkip(getSkipRows(request, entry));
        textReader.setMaxRows(getRowCount(request, entry, MAX_ROWS));

        String delimiter = getDelimiter(entry);
        if (delimiter != null) {
            textReader.setDelimiter(delimiter);
        }

        //        TabularVisitInfo visitInfo = new TabularVisitInfo(request, entry, sheetsToShow);
        visit(request, entry, textReader, tabularVisitor);
        props.addAll(textReader.getTableProperties());
        props.add("sheets");
        props.add(Json.list(sheets));
        sb.append(Json.map(props));

        request.setReturnFilename(entry.getName() + ".json");
        Result result = new Result("", sb);
        result.setShouldDecorate(false);
        result.setMimeType("application/json");

        return result;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param harvester _more_
     * @param args _more_
     * @param buffer _more_
     * @param files _more_
     *
     *
     * @return _more_
     * @throws Exception _more_
     */
    public boolean processCommandView(final org.ramadda.repository.harvester
            .CommandHarvester.CommandRequest request, final Entry entry,
                final org.ramadda.repository.harvester
                    .CommandHarvester harvester, final List<String> args,
                        final Appendable buffer, final List<FileInfo> files)
            throws Exception {

        if (args.contains("-help") || args.contains("?")) {
            buffer.append(
                "For tabular data:\n\t-maxrows <# rows to show>\n\t-columns <comma separated columns to show e.g., 1,3,4,6> \n\t-startcol col# -endcol col# \n\t-startrow row # -endrow row #\n");

            return false;
        }



        final boolean justHeader = args.contains("-header");
        final boolean doText     = args.contains("-text");
        final boolean doFile     = args.contains("-file");
        final boolean doImage    = args.contains("-image");
        final boolean doHtml     = !doText && !doFile && !doImage;
        List<String> columnsArg = StringUtil.split(Utils.getArg("-columns",
                                      args, ""), ",", true, true);
        final List<Integer> selectedColumns = (columnsArg.size() > 0)
                ? new ArrayList<Integer>()
                : null;


        //User indexes are 1 based
        if (columnsArg.size() > 0) {
            for (String col : columnsArg) {
                if (col.indexOf("-") >= 0) {
                    List<String> toks = StringUtil.split(col, "-", true,
                                            true);
                    if (toks.size() == 2) {
                        int start = Integer.parseInt(toks.get(0));
                        int end   = Integer.parseInt(toks.get(1));
                        for (int i = start; i <= end; i++) {
                            selectedColumns.add(new Integer(i - 1));
                        }
                    }
                } else {
                    selectedColumns.add(new Integer(Integer.parseInt(col)
                            - 1));
                }
            }
        }


        final int startCol = Math.max(0, Utils.getArg("-startcol", args, 1)
                                      - 1);
        final int endCol  = Utils.getArg("-endcol", args, 1000) - 1;
        final int maxCols = Utils.getArg("-maxcols", args, 1000);

        final int maxRows = Utils.getArg("-maxrows", args, 50);
        final int startRow = Math.max(0, Utils.getArg("-startrow", args, 1)
                                      - 1);
        final int endRow = Utils.getArg("-endrow", args, startRow + 1000) - 1;

        //        System.err.println ("max rows:" + maxRows + "  startRow: " + startRow +" end row:" + endRow);

        final StringBuilder html           = new StringBuilder("");

        final StringBuilder sb             = new StringBuilder("");
        final String        colDelimiter   = doFile
                                             ? ","
                                             : " | ";

        TabularVisitor      tabularVisitor = new TabularVisitor() {
            private List<Integer> dfltCols = new ArrayList<Integer>();
            @Override
            public boolean visit(TextReader info, String sheet,
                                 List<List<Object>> rows) {
                try {
                    return visitInner(info, sheet, rows);
                } catch (Exception exc) {
                    throw new RuntimeException(exc);
                }
            }

            private boolean visitInner(TextReader info, String sheet,
                                       List<List<Object>> rows)
                    throws Exception {

                int padMaxCols = 1;
                for (List<Object> cols : rows) {
                    padMaxCols = Math.max(cols.size(), padMaxCols);
                }
                for (List<Object> cols : rows) {
                    while (cols.size() < padMaxCols) {
                        cols.add("");
                    }
                }

                List<List<Object>> rowsToUse = new ArrayList<List<Object>>();

                int                rowCnt    = 0;
                for (int rowIdx = startRow;
                        (rowIdx < rows.size()) && (rowIdx <= endRow);
                        rowIdx++) {
                    if (rowCnt++ > maxRows) {
                        break;
                    }

                    List<Object>  cols   = rows.get(rowIdx);
                    int           colCnt = 0;
                    StringBuilder lineSB = new StringBuilder();

                    List<Object>  tmp    = new ArrayList<Object>();
                    dfltCols = new ArrayList<Integer>();
                    for (int colIdx = startCol;
                            (colIdx < cols.size()) && (colIdx <= endCol);
                            colIdx++) {
                        if (colCnt++ > maxCols) {
                            break;
                        }
                        tmp.add(cols.get(colIdx));
                        dfltCols.add(new Integer(colIdx));
                    }
                    rowsToUse.add(tmp);
                }

                harvester.displayTabularData(request, entry, args, files,
                                             rowsToUse);

                return false;
            }
        };

        TextReader info = new TextReader();
        info.setSkip(0);
        info.setMaxRows(100);
        for (String s : args) {
            if (s.matches("(<|<=|>|>=|=|<>|!=)")) {
                info.addSearchExpression(s);
            }
        }
        visit(request.getRequest(), entry, info, tabularVisitor);



        if (doImage) {
            File imageFile = getRepository().getStorageManager().getTmpFile(
                                 request.getRequest(),
                                 entry.getName() + "_table.png");

            Font font = new Font("Dialog", Font.PLAIN, 12);
            Image image = ImageUtils.renderHtml(html.toString(), 1200, null,
                              font);
            ImageUtils.writeImageToFile(image, imageFile);
            FileInfo fileInfo = new FileInfo(imageFile);

            if ( !entry.getTypeHandler().isWikiText(entry.getDescription())) {
                fileInfo.setDescription(entry.getDescription());
            }
            fileInfo.setTitle("Table - " + entry.getName());
            files.add(fileInfo);
        }

        if (doFile) {
            File csvFile = getRepository().getStorageManager().getTmpFile(
                               request.getRequest(),
                               IOUtil.stripExtension(entry.getName())
                               + ".csv");
            IOUtil.writeFile(csvFile.toString(), sb.toString());
            FileInfo fileInfo = new FileInfo(csvFile);
            if ( !entry.getTypeHandler().isWikiText(entry.getDescription())) {
                fileInfo.setDescription(entry.getDescription());
            }
            fileInfo.setTitle("Table - "
                              + IOUtil.stripExtension(entry.getName())
                              + ".csv");
            files.add(fileInfo);
        }

        return true;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private int getSkipRows(Request request, Entry entry) throws Exception {
        int dflt = 0;
        if (isTabular(entry)) {
            TabularTypeHandler tth =
                (TabularTypeHandler) entry.getTypeHandler();
            dflt = tth.getSkipRows(entry);
        }

        return (int) request.get("table.skiprows", dflt);
    }

    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private String getDelimiter(Entry entry) throws Exception {
        if (isTabular(entry)) {
            TabularTypeHandler tth =
                (TabularTypeHandler) entry.getTypeHandler();

            return tth.getDelimiter(entry);
        }

        return null;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param dflt _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private int getRowCount(Request request, Entry entry, int dflt)
            throws Exception {
        int v = (int) request.get("table.rows", dflt);

        return v;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param visitInfo _more_
     * @param visitor _more_
     *
     * @throws Exception _more_
     */
    public void visit(Request request, Entry entry, TextReader visitInfo,
                      TabularVisitor visitor)
            throws Exception {

        File file = entry.getFile();
        //        System.err.println("visit:" + visitInfo);
        InputStream inputStream = null;
        String      suffix      = "";

        if ((file != null) && file.exists()) {
            inputStream = new BufferedInputStream(
                getStorageManager().getFileInputStream(file));
            suffix = IOUtil.getFileExtension(file.toString()).toLowerCase();
            if (suffix.equals(".xlsx") || suffix.equals(".xls")) {
                if (file.length() > 10 * 1000000) {
                    throw new IllegalArgumentException("File too big");
                }
            }

        }

        if (suffix.equals(".xlsx") || suffix.equals(".xls")) {
            //            System.err.println ("Visit xls");
            visitXls(request, entry, suffix, inputStream, visitInfo, visitor);
        } else if (suffix.endsWith(".csv") || suffix.endsWith(".txt")) {
            visitCsv(request, entry, inputStream, visitInfo, visitor);
        } else {
            if (isTabular(entry)) {
                TabularTypeHandler tth =
                    (TabularTypeHandler) entry.getTypeHandler();
                //                System.err.println ("Visit tabular");
                tth.visit(request, entry, inputStream, visitInfo, visitor);
            } else {
                throw new IllegalStateException("Unknown file type:"
                        + suffix);
            }
        }
        IOUtil.close(inputStream);
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param inputStream _more_
     * @param textReader _more_
     * @param visitor _more_
     *
     * @throws Exception _more_
     */
    public void visitCsv(Request request, Entry entry,
                         InputStream inputStream,
                         final TextReader textReader, TabularVisitor visitor)
            throws Exception {
        BufferedReader br =
            new BufferedReader(new InputStreamReader(inputStream));
        final List<List<Object>> rows = new ArrayList<List<Object>>();

        ByteArrayOutputStream    bos  = new ByteArrayOutputStream();

        textReader.setInput(new BufferedInputStream(inputStream));
        textReader.setOutput(bos);
        textReader.getProcessor().addProcessor(new Processor() {
            @Override
            public org.ramadda.util.text.Row processRow(
                    TextReader textReader, org.ramadda.util.text.Row row,
                    String line)
                    throws Exception {
                //                System.err.println("TabularOutputHandler.processRow:" + line);
                List obj = new ArrayList();
                obj.addAll(row.getValues());
                rows.add((List<Object>) obj);

                return row;
            }
        });

        if (textReader.getSearchFields() != null) {
            for (SearchField searchField : textReader.getSearchFields()) {
                String id = "table." + searchField.getName();
                if (request.defined(id)) {
                    //Columns are 1 based to the user
                    if (searchField.getName().startsWith("column")) {
                        int column = Integer.parseInt(
                                         searchField.getName().substring(
                                             "column".length()).trim()) - 1;
                        String s = request.getString(id, "");
                        s = s.trim();
                        //                        System.err.println("column:" + column + " s:" + s);
                        String operator = StringUtil.findPattern(s,
                                              "^([<>=]+).*");
                        if (operator != null) {
                            System.err.println("operator:" + operator);
                            s = s.replace(operator, "").trim();
                            double value = Double.parseDouble(s);
                            int op = Filter.ValueFilter.getOperator(operator);
                            textReader.getFilter().addFilter(
                                new Filter.ValueFilter(column, op, value));

                            continue;
                        }
                        //                        if(s.

                        textReader.getFilter().addFilter(
                            new Filter.PatternFilter(
                                column, request.getString(id, "")));

                    }
                }

            }
        }

        String searchText = request.getString("table.text", (String) null);
        if (Utils.stringDefined(searchText)) {
            //match all
            textReader.getFilter().addFilter(new Filter.PatternFilter(-1,
                    "(?i:.*" + searchText + ".*)"));
        }
        new CsvUtil(new ArrayList<String>()).process(textReader);
        visitor.visit(textReader, entry.getName(), rows);
    }



    /**
     * _more_
     *
     * @param suffix _more_
     * @param inputStream _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private static Workbook makeWorkbook(String suffix,
                                         InputStream inputStream)
            throws Exception {
        return (suffix.equals(".xls")
                ? new HSSFWorkbook(inputStream)
                : new XSSFWorkbook(inputStream));

    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param suffix _more_
     * @param inputStream _more_
     * @param visitInfo _more_
     * @param visitor _more_
     *
     * @throws Exception _more_
     */
    private void visitXls(Request request, Entry entry, String suffix,
                          InputStream inputStream, TextReader visitInfo,
                          TabularVisitor visitor)
            throws Exception {
        //        System.err.println("visitXls: making workbook");
        Workbook wb = makeWorkbook(suffix, inputStream);
        //        System.err.println("visitXls:" + visitInfo.getSkip() + " max rows:" + visitInfo.getMaxRows()+ " #sheets:" + wb.getNumberOfSheets());
        int maxRows = visitInfo.getMaxRows();
        for (int sheetIdx = 0; sheetIdx < wb.getNumberOfSheets();
                sheetIdx++) {
            if ( !visitInfo.okToShowSheet(sheetIdx)) {
                continue;
            }
            Sheet sheet = wb.getSheetAt(sheetIdx);
            //            System.err.println("\tsheet:" + sheet.getSheetName() + " #rows:" + sheet.getLastRowNum());
            List<List<Object>> rows      = new ArrayList<List<Object>>();
            int                sheetSkip = visitInfo.getSkip();
            for (int rowIdx = sheet.getFirstRowNum();
                    (rows.size() < maxRows)
                    && (rowIdx <= sheet.getLastRowNum());
                    rowIdx++) {
                if (sheetSkip-- > 0) {
                    continue;
                }

                Row row = sheet.getRow(rowIdx);
                if (row == null) {
                    continue;
                }
                List<Object> cols     = new ArrayList<Object>();
                short        firstCol = row.getFirstCellNum();
                for (short col = firstCol;
                        (col < MAX_COLS) && (col < row.getLastCellNum());
                        col++) {
                    Cell cell = row.getCell(col);
                    if (cell == null) {
                        break;
                    }
                    Object value = null;
                    int    type  = cell.getCellType();
                    if (type == cell.CELL_TYPE_NUMERIC) {
                        value = new Double(cell.getNumericCellValue());
                    } else if (type == cell.CELL_TYPE_BOOLEAN) {
                        value = new Boolean(cell.getBooleanCellValue());
                    } else if (type == cell.CELL_TYPE_ERROR) {
                        value = "" + cell.getErrorCellValue();
                    } else if (type == cell.CELL_TYPE_BLANK) {
                        value = "";
                    } else if (type == cell.CELL_TYPE_FORMULA) {
                        value = cell.getCellFormula();
                    } else {
                        value = cell.getStringCellValue();
                    }
                    cols.add(value);
                }

                /**
                 * ** TODO
                 * org.ramadda.util.text.Row row = new Row(cols);
                 *
                 * if ( !visitInfo.rowOk(row)) {
                 *   if (rows.size() == 0) {
                 *       //todo: check for the header line
                 *   } else {
                 *       continue;
                 *   }
                 * }
                 */
                rows.add(cols);
            }
            if ( !visitor.visit(visitInfo, sheet.getSheetName(), rows)) {
                break;
            }
        }
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param requestProps _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getHtmlDisplay(Request request, Hashtable requestProps,
                                 Entry entry)
            throws Exception {

        if (isTabular(entry)) {
            TabularTypeHandler handler =
                (TabularTypeHandler) entry.getTypeHandler();
            if ( !handler.okToShowTable(request, entry)) {
                return null;
            }
        }



        boolean showTable = entry.getValue(TabularTypeHandler.IDX_SHOWTABLE,
                                           true);
        boolean showChart = entry.getValue(TabularTypeHandler.IDX_SHOWCHART,
                                           true);

        boolean useFirstRowAsHeader =
            Misc.equals("true",
                        entry.getValue(TabularTypeHandler.IDX_USEFIRSTROW,
                                       "true"));


        boolean colHeader =
            Misc.equals("true",
                        entry.getValue(TabularTypeHandler.IDX_COLHEADER,
                                       "false"));
        boolean rowHeader =
            Misc.equals("true",
                        entry.getValue(TabularTypeHandler.IDX_ROWHEADER,
                                       "false"));
        List<String> widths =
            StringUtil.split(entry.getValue(TabularTypeHandler.IDX_WIDTHS,
                                            ""), ",", true, true);



        List<String> sheetsStr =
            StringUtil.split(entry.getValue(TabularTypeHandler.IDX_SHEETS,
                                            ""), ",", true, true);


        List propsList = new ArrayList();


        propsList.add("useFirstRowAsHeader");
        propsList.add("" + useFirstRowAsHeader);
        propsList.add("showTable");
        propsList.add("" + showTable);
        propsList.add("showChart");
        propsList.add("" + showChart);


        propsList.add("rowHeaders");
        propsList.add("" + rowHeader);

        propsList.add("skipRows");
        propsList.add(entry.getValue(TabularTypeHandler.IDX_SKIPROWS, "0"));
        propsList.add("skipColumns");
        propsList.add(entry.getValue(TabularTypeHandler.IDX_SKIPCOLUMNS,
                                     "0"));

        List<String> header =
            StringUtil.split(entry.getValue(TabularTypeHandler.IDX_HEADER,
                                            ""), ",", true, true);
        if (header.size() > 0) {
            propsList.add("colHeaders");
            propsList.add(Json.list(header, true));
        } else {
            propsList.add("colHeaders");
            propsList.add("" + colHeader);
        }


        if (widths.size() > 0) {
            propsList.add("colWidths");
            propsList.add(Json.list(widths));
        }

        String jsonUrl =
            request.entryUrl(getRepository().URL_ENTRY_SHOW, entry,
                             ARG_OUTPUT,
                             TabularOutputHandler.OUTPUT_XLS_JSON.getId());

        List<String> charts = new ArrayList<String>();
        for (String line :
                StringUtil.split(
                    entry.getValue(TabularTypeHandler.IDX_CHARTS, ""), "\n",
                    true, true)) {

            List<String>              chart = new ArrayList<String>();
            Hashtable<String, String> map   = new Hashtable<String, String>();
            for (String tok : StringUtil.split(line, ",")) {
                List<String> subtoks = StringUtil.splitUpTo(tok, "=", 2);
                String       key     = subtoks.get(0);
                if (subtoks.size() < 2) {
                    chart.add("type");
                    chart.add(Json.quote(key));

                    continue;
                }
                String value = subtoks.get(1);
                chart.add(key);
                chart.add(Json.quote(value));
            }
            charts.add(Json.map(chart));
        }
        if (charts.size() > 0) {
            propsList.add("defaultCharts");
            propsList.add(Json.list(charts));
        }

        propsList.add("url");
        propsList.add(Json.quote(jsonUrl));
        propsList.add("layoutHere");
        propsList.add("false");

        /**
         * TabularVisitInfo visitInfo = new TabularVisitInfo(request, entry);
         * if (visitInfo.getSearchFields() != null) {
         *   propsList.add("searchFields");
         *   List<String> names = new ArrayList<String>();
         *   for (SearchField searchField :
         *           visitInfo.getSearchFields()) {
         *
         *       List<String> props = new ArrayList<String>();
         *       props.add("name");
         *       props.add(Json.quote(searchField.getName()));
         *       props.add("label");
         *       props.add(Json.quote(searchField.getLabel()));
         *       names.add(Json.map(props));
         *   }
         *   propsList.add(Json.list(names));
         * }
         */

        String props = Json.map(propsList);
        //        System.err.println(props);

        StringBuilder sb = new StringBuilder();
        //        sb.append(HtmUltils.pre(tmp.toString()));


        getRepository().getWikiManager().addDisplayImports(request, sb);

        getPageHandler().entrySectionOpen(request, entry, sb, null, true);


        if ( !request.get(ARG_EMBEDDED, false)) {
            sb.append(entry.getDescription());
        }

        String divId = HtmlUtils.getUniqueId("div_");
        sb.append(HtmlUtils.div("", HtmlUtils.id(divId)));
        StringBuilder js = new StringBuilder();
        js.append("var displayManager = getOrCreateDisplayManager(\"" + divId
                  + "\",");
        js.append(Json.map("showMap", "false", "showMenu", "false",
                           "showTitle", "false", "layoutType",
                           Json.quote("table"), "layoutColumns", "1"));
        js.append(",true);\n");
        js.append("displayManager.createDisplay('xls'," + props + ");\n");
        sb.append(HtmlUtils.script(js.toString()));

        getPageHandler().entrySectionClose(request, entry, sb);

        return sb.toString();


    }


    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    public static boolean isTabular(Entry entry) {
        if (entry == null) {
            return false;
        }

        return entry.getTypeHandler().isType(TabularTypeHandler.TYPE_TABULAR);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param service _more_
     * @param input _more_
     * @param args _more_
     *
     *
     * @return _more_
     * @throws Exception _more_
     */
    public boolean extractSheet(Request request, Service service,
                                ServiceInput input, List args)
            throws Exception {
        Entry entry = null;
        for (Entry e : input.getEntries()) {
            if (isTabular(e)) {
                entry = e;

                break;
            }
        }
        if (entry == null) {
            throw new IllegalArgumentException("No tabular entry found");
        }

        HashSet<Integer> sheetsToShow = getSheetsToShow((String) args.get(0));

        final SXSSFWorkbook wb        = new SXSSFWorkbook(100);
        //        final Workbook   wb           = new XSSFWorkbook();

        String name = getStorageManager().getFileTail(entry);
        if ( !Utils.stringDefined(name)) {
            name = entry.getName();
        }
        name = IOUtil.stripExtension(name);

        File newFile = new File(IOUtil.joinDir(input.getProcessDir(),
                           name + ".xlsx"));

        TabularVisitor visitor = new TabularVisitor() {
            public boolean visit(TextReader info, String sheetName,
                                 List<List<Object>> rows) {
                sheetName = sheetName.replaceAll("[/]+", "-");
                Sheet sheet  = wb.createSheet(sheetName);
                int   rowCnt = 0;
                for (List<Object> cols : rows) {
                    Row row = sheet.createRow(rowCnt++);
                    for (int colIdx = 0; colIdx < cols.size(); colIdx++) {
                        Object col  = cols.get(colIdx);
                        Cell   cell = row.createCell(colIdx);
                        if (col instanceof Double) {
                            cell.setCellValue(((Double) col).doubleValue());
                        } else if (col instanceof Date) {
                            cell.setCellValue((Date) col);
                        } else if (col instanceof Boolean) {
                            cell.setCellValue(((Boolean) col).booleanValue());
                        } else {
                            cell.setCellValue(col.toString());
                        }
                    }
                }

                return true;
            }
        };

        TabularVisitInfo visitInfo =
            new TabularVisitInfo(
                request, entry, getSkipRows(request, entry),
                getRowCount(request, entry, Integer.MAX_VALUE), sheetsToShow);

        TextReader info = new TextReader();
        info.setSkip(getSkipRows(request, entry));
        info.setMaxRows(getRowCount(request, entry, MAX_ROWS));
        //        http:://localhost:8080/repository/entry/show?entryid=740ae258-805d-4a1f-935d-289d0a6e5519&output=media_tabular_extractsheet&serviceform=true&execute=Execute

        visit(request, entry, info, visitor);

        FileOutputStream fileOut = new FileOutputStream(newFile);
        wb.write(fileOut);
        fileOut.close();
        wb.dispose();

        return true;

    }



    /**
     * _more_
     *
     * @param request _more_
     * @param service _more_
     * @param input _more_
     * @param args _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean csv(Request request, Service service, ServiceInput input,
                       List args)
            throws Exception {
        return true;
    }

    /*
        Entry entry = null;
        for (Entry e : input.getEntries()) {
            if (isTabular(e)) {
                entry = e;

                break;
            }
        }
        if (entry == null) {
            throw new IllegalArgumentException("No tabular entry found");
        }

        HashSet<Integer> sheetsToShow = getSheetsToShow((String) args.get(0));
        String name = getStorageManager().getFileTail(entry);
        if ( !Utils.stringDefined(name)) {
            name = entry.getName();
        }
        name = IOUtil.stripExtension(name);

        File newFile = new File(IOUtil.joinDir(input.getProcessDir(),
                           name + ".csv"));

        String file = "";
        InputStream inputStream = new BufferedInputStream(
                                                          getStorageManager().getFileInputStream(file));
        final TextReader info =
            new TextReader(new BufferedInputStream(inputStream), new FileOutputStream(newFile));


        TabularVisitor visitor = new TabularVisitor() {
            public boolean visit(TabularVisitInfo info, String sheetName,
                                 List<List<Object>> rows) {
                return true;
            }
        };

        TabularVisitInfo visitInfo =
            new TabularVisitInfo(
                request, entry, getSkipRows(request, entry),
                getRowCount(request, entry, Integer.MAX_VALUE), sheetsToShow);


        visit(request, entry, visitInfo, visitor);


        return true;

    }
    */

    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        Workbook wb = makeWorkbook(IOUtil.getFileExtension(args[0]),
                                   new FileInputStream(args[0]));
        for (int sheetIdx = 0; sheetIdx < wb.getNumberOfSheets();
                sheetIdx++) {
            Sheet sheet = wb.getSheetAt(sheetIdx);
            System.err.println(sheet.getSheetName());
            for (int rowIdx = sheet.getFirstRowNum();
                    rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null) {
                    continue;
                }
                short firstCol = row.getFirstCellNum();
                int   colCnt   = 0;
                for (short col = firstCol; col < row.getLastCellNum();
                        col++) {
                    Cell cell = row.getCell(col);
                    if (cell == null) {
                        break;
                    }
                    Object value = null;
                    int    type  = cell.getCellType();
                    if (type == cell.CELL_TYPE_NUMERIC) {
                        value = new Double(cell.getNumericCellValue());
                    } else if (type == cell.CELL_TYPE_BOOLEAN) {
                        value = new Boolean(cell.getBooleanCellValue());
                    } else if (type == cell.CELL_TYPE_ERROR) {
                        value = "" + cell.getErrorCellValue();
                    } else if (type == cell.CELL_TYPE_BLANK) {
                        value = "";
                    } else if (type == cell.CELL_TYPE_FORMULA) {
                        value = cell.getCellFormula();
                    } else {
                        value = cell.getStringCellValue();
                    }
                    if (colCnt++ > 0) {
                        System.out.print(",");
                    }
                    System.out.print(value);
                }
                System.out.println("");
            }
        }
    }



}
