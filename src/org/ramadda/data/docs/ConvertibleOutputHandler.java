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

package org.ramadda.data.docs;

import org.ramadda.repository.*;
import org.ramadda.repository.PluginManager;
import org.ramadda.repository.output.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;
import org.ramadda.util.Json;
import org.ramadda.util.Utils;
import org.ramadda.util.text.CsvUtil;
import org.ramadda.util.text.CsvContext;

import org.w3c.dom.*;

import ucar.unidata.ui.ImageUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import java.io.*;

import java.net.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;



/**
 */
public class ConvertibleOutputHandler extends OutputHandler {


    /** _more_ */
    public static final OutputType OUTPUT_CONVERT_FORM =
        new OutputType("Convert Data", "convert_form", OutputType.TYPE_VIEW,
                       "", "fa-file-excel");

    /** _more_ */
    public static final OutputType OUTPUT_CONVERT_PROCESS =
        new OutputType("Convert Data", "convert_process",
                       OutputType.TYPE_VIEW, "", "fa-file-excel");


    /**
     * _more_
     */
    public ConvertibleOutputHandler() {}

    /**
     * _more_
     *
     *
     * @param repository _more_
     * @param element _more_
     * @throws Exception _more_
     */
    public ConvertibleOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
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
        boolean isConvertible = isConvertible(entry);
        if ( !isConvertible) {
            if ( !entry.isFile()) {
                return;
            }
            String path = entry.getResource().getPath();
            if (path.endsWith(".xls") || path.endsWith(".xlsx")
                    || path.endsWith(".csv")) {
                isConvertible = true;
            }
        }

        if (isConvertible) {
            links.add(makeLink(request, state.getEntry(),
                               OUTPUT_CONVERT_FORM));
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

        return new Result("", "Unknown request");
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
	makeConvertForm(request, entry, sb);
        getPageHandler().entrySectionClose(request, entry, sb);
        return new Result("", sb);
    }


    public void makeConvertForm(Request request, Entry entry, StringBuilder sb)
            throws Exception {
        StringBuilder js = new StringBuilder();
        js.append("var convertCsvEntry = '" + entry.getId() + "';\n");
        String lastInput =
            (String) getSessionManager().getSessionProperty(request,
                "csv.lastinput." + entry.getId());
        if ((lastInput == null)
                && entry.getTypeHandler().isType("type_convertible")) {
            lastInput =
                (String) entry.getValue(ConvertibleTypeHandler.IDX_COMMANDS);

            if ((lastInput == null) || (lastInput.length() == 0)) {
                if (entry.getTypeHandler().isType(
                        TabularTypeHandler.TYPE_TABULAR)) {
                    lastInput = (String) entry.getValue(
                        TabularTypeHandler.IDX_CONVERT);
                }
            }
        }
        if (lastInput != null) {
            //A hack but escaping the escapes in java is a pain
            lastInput = lastInput.replaceAll("\r\n", "_escnl_");
            lastInput = lastInput.replaceAll("\r", "_escnl_");
            lastInput = lastInput.replaceAll("\n", "_escnl_");
            lastInput = lastInput.replaceAll("\"", "_escquote_");
            lastInput = lastInput.replaceAll("\\\\", "_escslash_");
            js.append("var convertCsvLastInput =\"" + lastInput + "\";\n");
        } else {
            js.append("var convertCsvLastInput =null;\n");
        }
        HtmlUtils.script(sb, js.toString());
        sb.append(HtmlUtils.div("", HtmlUtils.id("convertcsv_div")));
        HtmlUtils.importJS(
            sb, getRepository().getHtdocsUrl("/lib/ace/src-min/ace.js"));
        HtmlUtils.importJS(sb,
                           getRepository().getUrlBase()
                           + "/media/convertcsv.js");
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
    protected Result makeHtmlResult(Request request, String s)
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
     * @param entry _more_
     *
     * @return _more_
     */
    public ConvertibleTypeHandler getTypeHandler(Entry entry) {
        return (ConvertibleTypeHandler) entry.getTypeHandler();
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

        boolean process   = request.get("process", false);
        boolean save      = request.get("save", false);
        String  lastInput = request.getString("lastinput", (String) null);
        if (save && (lastInput != null)) {
            getSessionManager().putSessionProperty(request,
                    "csv.lastinput." + entry.getId(), lastInput);
            if (getAccessManager().canEditEntry(request, entry)
                    && entry.getTypeHandler().isType("type_convertible")) {
                entry.setValue(ConvertibleTypeHandler.IDX_COMMANDS,
                               lastInput);
                getEntryManager().updateEntry(request, entry);
            }
        }



        List    currentArgs = null;
        CsvUtil csvUtil     = null;
        try {
            String processEntryId =
                getStorageManager().getProcessDirEntryId(destDir.getName());

            List<String> newFiles      = new ArrayList<String>();
            Entry        theEntry      = entry;
            String       commandString = request.getString("commands", "");
            //A hack because the Request changes any incoming "script" to "_script_"
            commandString = commandString.replaceAll("_script_", "script");
            List<StringBuilder> toks =
                CsvUtil.tokenizeCommands(commandString);

            //      System.err.println("TOKS:" + toks);
            List<List<String>> llines  = new ArrayList<List<String>>();
            List<String>       current = null;
            for (StringBuilder sb : toks) {
                String s = sb.toString();
                //If we are doing the full process then keep the line separation
                if (s.equals(Utils.MULTILINE_END)) {
                    if (process) {
                        current = null;
                    }

                    continue;
                }
                if (current == null) {
                    current = new ArrayList<String>();
                    llines.add(current);
                }
                current.add(s);
            }
            if (llines.size() == 0) {
                List<String> l = new ArrayList<String>();
                llines.add(l);
            }
            if (request.defined("csvoutput")) {
		String output = request.getString("csvoutput");
		List<String> lastLine  = llines.get(llines.size()-1);
		if(!lastLine.contains(output)) lastLine.add(output);
            }
            CsvUtil prevCsvUtil = null;

            for (int i = 0; i < llines.size(); i++) {
                List<String> args1        = llines.get(i);
                String       runDirPrefix = request.getString("rundir",
							      "run");
                List<String> args         = new ArrayList<String>();
                for (int j = 0; j < args1.size(); j++) {
                    String arg = args1.get(j);
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
                        runDirPrefix = args1.get(++j);
                        continue;
                    }
                    args.add(arg);
                }
		boolean hasOutput = args.contains("-tojson")  || args.contains("-toxml")|| args.contains("-db");
		//		System.err.println("args:" + args);
                if ( !args.contains("-print") && !args.contains("-p")
		     && !args.contains("-explode")
		     && !args.contains("-script")
		     && !hasOutput
		     && !args.contains("-printheader")
		     && !args.contains("-template")
		     && !args.contains("-raw")
		     && !args.contains("-stats")
		     && !args.contains("-record")
		     && !args.contains("-table")
		     && !args.contains("-db")) {
		    //		    System.err.println("adding print");
                    args.add("-print");
                }
		if(hasOutput) {
		    //		    System.err.println("removing print");
		    args.remove("-print");
		    args.remove("-p");
		}

                currentArgs = args;
		//		System.err.println("args:" + args);
                //              for(String arg: args)

                //                  System.err.println("arg:" + arg+":");
		
                File runDir = null;
		if(runDirPrefix!=null)   runDirPrefix = IO.cleanFileName(runDirPrefix).trim();
		if(runDirPrefix.length()==0) runDirPrefix = "run";
                for (int j = 0; true; j++) {
                    runDir = new File(IOUtil.joinDir(destDir, ((j == 0)
                            ? runDirPrefix
                            : (runDirPrefix + j))));
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
                if (process) {
                    request.put("applysiblings", "true");
                }
                newFiles = new ArrayList<String>();
                csvUtil  = new CsvUtil(args, runDir);
		csvUtil.setInteractive(true);
                csvUtil.setPropertyProvider(getRepository());
		csvUtil.setCsvContext(new CsvContext() {
			public List<Class> getClasses() {
			    return getRepository().getPluginManager().getCsvClasses();
			}
			public String getProperty(String key, String dflt) {
			    return getRepository().getProperty(key, dflt);
			}
		    });
                csvUtil.setMapProvider(getRepository().getMapManager());		
                if (prevCsvUtil != null) {
                    csvUtil.initWith(prevCsvUtil);
                }
                prevCsvUtil = csvUtil;
                getSessionManager().putSessionProperty(request, "csvutil",
                        csvUtil);
                //              System.err.println("RUN:");
                for (Entry e : entries) {
                    //              System.err.println("\tentry:" + e);
                    outputConvertProcessInner(request, process, e, csvUtil,
                            destDir, runDir, args, newFiles);
                    if ( !csvUtil.getOkToRun()) {
                        break;
                    }
                }
                if ( !csvUtil.getOkToRun()) {
                    String r = "stopped";
                    String s = new String(Utils.encodeBase64(r));
                    s = Json.mapAndQuote("result", s);

                    return new Result(s, "application/json");
                }

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

            String lastResult = "";
            if (newFiles.size() > 0) {
                if ( !process) {
                    lastResult = IO.readContents(newFiles.get(0));
                } else {
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
                                .getSynthId(getEntryManager()
                                    .getProcessEntry(), getStorageManager()
                                    .getProcessDir().toString(), f);
                        String url =
                            HtmlUtils
                                .url(request
                                    .getAbsoluteUrl(getRepository()
                                        .URL_ENTRY_SHOW), ARG_ENTRYID, id);

                        if (newFile.endsWith(".csv")) {
                            //                        url += "&output=" + OUTPUT_CONVERT_FORM;
                        }
                        String getUrl =
                            HtmlUtils.url(
                                request.makeUrl(
                                    getRepository().URL_ENTRY_GET) + "/"
                                        + f.getName(), ARG_ENTRYID, id);
                        html.append("  ");
                        html.append(HtmlUtils.href(getUrl, "Download", HU.attrs("class","ramadda-button")));
                        if (request.getUser().getAdmin()) {
                            html.append(" File on server: "
                                        + HtmlUtils.input("", f,"size=80"));
                        }
			html.append("<br>");
			html.append("View temp file: ");
                        html.append(HtmlUtils.href(url,  f.getName(),
                                "target=_output"));

                        //If they are creating point data then add an add entry link
                        //                    if (newFile.endsWith(".csv") && args.contains("-addheader")) {
                        //                        url += "&output=" + OUTPUT_CONVERT_FORM;
                        //                    }
                        html.append("<br>");
                    }
                    String urlParent =
                        HtmlUtils
                            .url(request
                                .getAbsoluteUrl(
                                    getRepository()
                                        .URL_ENTRY_SHOW), ARG_ENTRYID,
                                            getStorageManager()
                                                .getEncodedProcessDirEntryId(
                                                    destDir.getName()));
                    html.append(HtmlUtils.href(urlParent, "View All",
                            "target=_output"));
                    String s = Json.mapAndQuote("url",
                                   html.toString().replaceAll("\"", "\\\""));

                    return new Result(s, "application/json");
                }
            }
            String s = new String(Utils.encodeBase64((lastResult == null)
                    ? ""
                    : lastResult));
            s = Json.mapAndQuote("result", s);

            return new Result(s, "application/json");
        } catch (Exception exc) {
            Throwable inner = LogUtil.getInnerException(exc);
            String    s     = inner.getMessage();
            //Better messaging
            boolean printStack = true;
            if (inner instanceof NumberFormatException) {
                s = "Number format error " + s.replace("For", "for");
            }
            if ((csvUtil != null)
                    && (csvUtil.getErrorDescription() != null)) {
                s = csvUtil.getErrorDescription() + " " + s;
            }
            if (s == null) {
                s = "Error: " + inner;
            }
            s = new String(Utils.encodeBase64(s));
            s = Json.mapAndQuote("error", s);
            if (inner instanceof CsvUtil.MessageException) {
                s          = ((CsvUtil.MessageException) inner).getMessage();
                printStack = false;
                s          = new String(Utils.encodeBase64(s));
                s          = Json.mapAndQuote("message", s);
            }
            if (printStack) {
                inner.printStackTrace();
            }

            return new Result(s, "application/json");

        }
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param process _more_
     * @param entry _more_
     * @param csvUtil _more_
     * @param destDir _more_
     * @param runDir _more_
     * @param args _more_
     * @param newFiles _more_
     *
     * @throws Exception _more_
     */
    public void outputConvertProcessInner(Request request, boolean process,
                                          Entry entry, CsvUtil csvUtil,
                                          File destDir, File runDir,
                                          List<String> args,
                                          List<String> newFiles)
            throws Exception {

        try {
            List<String> files = new ArrayList<String>();
            files.add(entry.getResource().getPath());
            File f = getStorageManager().makeTmpFile(
                         runDir,
                         IOUtil.stripExtension(
                             getStorageManager().getFileTail(
                                 entry.getResource().getPath())) + ".csv");

            csvUtil.setOutputFile(f);
            //      System.err.println("\tcalling csvUtil.run");
            csvUtil.run(files);
            if ( !csvUtil.getOkToRun()) {
                return;
            }
            newFiles.addAll(csvUtil.getNewFiles());
            if (Misc.equals("true",
                            csvUtil.getContext().getProperty("nukeDb"))) {
                request.ensureAdmin();
                String sql = "drop table db_" + csvUtil.getDbId();
                try {
                    getRepository().getDatabaseManager().executeAndClose(sql);
                } catch (Exception exc) {}
            }

            if (Misc.equals(
                    "true",
                    csvUtil.getContext().getProperty("installPlugin"))) {
                request.ensureAdmin();
                for (String file : csvUtil.getNewFiles()) {
                    if (file.endsWith("db.xml")) {
                        getRepository().getPluginManager().installPlugin(
                            file);
                        getRepository().clearCache();
                    }
                }
            }
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
            List<String> sheetsStr = Utils.split(s, ",", true, true);
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
     * @param entry _more_
     *
     * @return _more_
     */
    public static boolean isConvertible(Entry entry) {
        if (entry == null) {
            return false;
        }

        return entry.getTypeHandler().isType("type_convertible");
    }



}
