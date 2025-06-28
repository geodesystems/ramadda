/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.data.docs;


import org.ramadda.repository.*;
import org.ramadda.repository.PluginManager;
import org.ramadda.repository.output.*;
import org.ramadda.data.services.RecordTypeHandler;
import org.ramadda.data.services.PointTypeHandler;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.Utils;
import org.ramadda.util.seesv.SeesvContext;
import org.ramadda.util.seesv.SeesvException;
import org.ramadda.util.seesv.Seesv;

import org.w3c.dom.*;

import ucar.unidata.ui.ImageUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import java.io.*;

import java.net.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;



/**
 */
@SuppressWarnings("unchecked")
public class ConvertibleOutputHandler extends OutputHandler {


    /** _more_ */
    public static final OutputType OUTPUT_CONVERT_FORM =
        new OutputType("Convert Data", "convert_form", OutputType.TYPE_FILE,
                       "", "fa-file-csv");

    /** _more_ */
    public static final OutputType OUTPUT_CONVERT_PROCESS =
        new OutputType("Convert Data", "convert_process",
                       OutputType.TYPE_FILE, "", "fa-file-csv");


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
        makeConvertForm(request, entry, sb, new Hashtable());
        getPageHandler().entrySectionClose(request, entry, sb);

        return new Result(entry.getName() +" - Seesv Form" , sb);
    }


    /**
     *
     * @param request _more_
     * @param entry _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void makeConvertForm(Request request, Entry entry,
                                StringBuilder sb, Hashtable props)
            throws Exception {
	

	boolean canEdit = Utils.getProperty(props,"canEdit",
					    getAccessManager().canDoEdit(request, entry));
        String lastInput = Utils.getProperty(props,"commands",null);

        if (lastInput != null) {
	    canEdit = false;
	    lastInput  = lastInput.replace("\\n","\n").replace("\\\\","\"");
	} else {
	    lastInput = 
		(String) getSessionManager().getSessionProperty(request,
								"csv.lastinput." + entry.getId());
	}

        if ((lastInput == null)
                && entry.getTypeHandler().isType(ConvertibleTypeHandler.TYPE_CONVERTIBLE)) {
            lastInput =
                (String) entry.getValue(request,ConvertibleTypeHandler.IDX_COMMANDS);

	}
	/*
	if (!Utils.stringDefined(lastInput)) {
	    if (entry.getTypeHandler().isType(TabularTypeHandler.TYPE_TABULAR)) {
		lastInput = (String) entry.getValue(request,"TabularTypeHandler.IDX_CONVERT);
	    }
	    }*/
	if (!Utils.stringDefined(lastInput)) {
	    lastInput = (String) entry.getValue(request,"convert_commands");
	}


        if (lastInput != null) {
            //A hack but escaping the escapes in java is a pain
            lastInput = lastInput.replaceAll("\r\n", "_escnl_");
            lastInput = lastInput.replaceAll("\r", "_escnl_");
            lastInput = lastInput.replaceAll("\n", "_escnl_");
            lastInput = lastInput.replaceAll("\"", "_escquote_");
            lastInput = lastInput.replaceAll("\\\\", "_escslash_");
            lastInput = lastInput.replaceAll("<", "_esclt_");
            lastInput = lastInput.replaceAll(">", "_escgt_");	    	    
        }
        String id = HtmlUtils.getUniqueId("convert");

        if (lastInput != null) {
            sb.append(HtmlUtils.pre(lastInput,
                                    "style='display:none;' "
                                    + HU.id(id + "_lastinput")));
        }
        sb.append(HtmlUtils.div("", HtmlUtils.id(id)));
	getRepository().getWikiManager().initWikiEditor(request, sb);
	sb.append(getRepository().getMapManager().getHtmlImports(request));
        HtmlUtils.importJS(sb,
                           getRepository().getUrlBase()
                           + "/media/seesv.js");


	if(canEdit) {
	    props.put("canEdit","true");
	} else {
	    props.put("canEdit","false");
	}

	List params = Utils.makeListFromDictionary(props);
	String jsparams = JsonUtil.mapAndQuote(params);
        HU.script(sb,
		  "var convertParams = " + jsparams +";\n" +
                  "new SeesvForm(" + HU.comma(HU.squote(id),HU.squote(entry.getId()),"convertParams")+");");


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
        s = JsonUtil.mapAndQuote(Utils.makeListFromValues("html", s));

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
    public PointTypeHandler getTypeHandler(Entry entry) {
        return (PointTypeHandler) entry.getTypeHandler();
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
                           "SeeSV Processing");

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
            Seesv csvUtil =
                (Seesv) getSessionManager().getSessionProperty(request,
                    "csvutil");
            if (csvUtil != null) {
                csvUtil.stopRunning();
            }
            String s = JsonUtil.mapAndQuote(Utils.makeListFromValues("message", "ok"));

            return new Result(s, "application/json");
        }

        boolean process   = request.get("process", false);
        boolean save      = request.get("save", false);
        String  lastInput = request.getString("lastinput", (String) null);
        if (save && (lastInput != null)) {
            getSessionManager().putSessionProperty(request,
                    "csv.lastinput." + entry.getId(), lastInput);
            if (getAccessManager().canDoEdit(request, entry)) {
		//		&& (entry.getTypeHandler().isType(ConvertibleTypeHandler.TYPE_CONVERTIBLE))) {
                entry.setValue("convert_commands", lastInput);
                getEntryManager().updateEntry(request, entry);
            }
        }


        List    currentArgs = null;
        Seesv csvUtil     = null;
        try {
            String processEntryId =
                getStorageManager().getProcessDirEntryId(destDir.getName());

            List<String> newFiles      = new ArrayList<String>();
            Entry        theEntry      = entry;
            String       commandString = request.getString("commands", "");
            //A hack because the Request changes any incoming "script" to "_script_"
            commandString = commandString.replaceAll("_script_", "script");
	    List<List<String>> llines  =  Seesv.tokenizeCommands(commandString,process);
            if (request.defined("csvoutput")) {
                String       output   = request.getString("csvoutput");
                List<String> lastLine = llines.get(llines.size() - 1);
                if ( !lastLine.contains(output)) {
                    lastLine.add(output);
                }
            }
            Seesv prevSeesv = null;
            for (int i = 0; i < llines.size(); i++) {
                List<String> args1        = llines.get(i);
                String       runDirPrefix = request.getString("rundir",
                                                "run");
		args1 = getTypeHandler(entry).preprocessCsvCommands(request, args1);
                List<String> args         = new ArrayList<String>();
                for (int j = 0; j < args1.size(); j++) {
                    String arg = args1.get(j);
		    if (arg.equals("-run")) {
			runDirPrefix = args1.get(++j);
			continue;
		    }			
                    args.add(arg);
                }
                boolean hasOutput = args.contains("-tojson")
                                    || args.contains("-toxml")
                                    || args.contains("-db");
                //              System.err.println("args:" + args);
                if ( !args.contains("-print")
		     && !args.contains("-p")
		     && !args.contains("-explode")
		     && !args.contains("-script") && !hasOutput
		     && !args.contains("-printheader")
		     && !args.contains("-template")
		     && !args.contains("-raw")
		     && !args.contains("-stats")
		     && !args.contains("-record")
		     && !args.contains("-torecord")
		     && !args.contains("-fields")
		     && !args.contains("-tojson")
		     && !args.contains("-table")
		     && !args.contains("-cols")		     
		     && !args.contains("-db")) {
                    //              System.err.println("adding print");
                    args.add("-print");
                }
                if (hasOutput) {
                    //              System.err.println("removing print");
                    args.remove("-print");
                    args.remove("-p");
                }

                currentArgs = args;
		//		System.err.println("args:" + args);
                //              for(String arg: args)

                //                  System.err.println("arg:" + arg+":");

                File runDir = null;
                if (runDirPrefix != null) {
                    runDirPrefix = IO.cleanFileName(runDirPrefix).trim();
                }
                if (runDirPrefix.length() == 0) {
                    runDirPrefix = "run";
                }
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
                csvUtil  = new Seesv(args, runDir);
		csvUtil.setIsVerifiedUser(!request.isAnonymous());
                csvUtil.setInteractive(true);
                csvUtil.setPropertyProvider(getRepository());
                csvUtil.setSeesvContext(new SeesvContext() {
                    public List<Class> getClasses() {
                        return getRepository().getPluginManager()
                            .getSeesvClasses();
                    }
                    public String getProperty(String key, String dflt) {
                        return getRepository().getProperty(key, dflt);
                    }
                    public File getTmpFile(String name) {
                        return null;
                    }
                });
                csvUtil.setMapProvider(getRepository().getMapManager());
                if (prevSeesv != null) {
                    csvUtil.initWith(prevSeesv);
                }
                prevSeesv = csvUtil;
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
                    s = JsonUtil.mapAndQuote(Utils.makeListFromValues("result", s));
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
                    StringBuffer html = new StringBuffer("");
		    html.append(HU.formTable());
                    for (String newFile : newFiles) {
                        File f = new File(newFile);
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

                        String getUrl =
                            HtmlUtils.url(
                                request.makeUrl(
                                    getRepository().URL_ENTRY_GET) + "/"
                                        + f.getName(), ARG_ENTRYID, id);
                        html.append(HU.row(HU.td(HtmlUtils.href(getUrl, "<span><i class='fas fa-download'></i></span>"+" Download",
								HU.attrs("class", "ramadda-button")))));

                        if (request.getUser().getAdmin()) {
                            HU.formEntry(html,"File on server:",
					 HtmlUtils.input("", f, "size=80"));
                        }
                        HU.formEntry(html,"View temp file:",
				     HtmlUtils.href(url, f.getName(),
						    "target=_output"));

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
                    HU.formEntry(html,"",HtmlUtils.href(urlParent, "View All",
						       "target=_output"));

		    html.append(HU.formTableClose());

		    String shtml = HU.insetDiv(html, 0,20,0,0);
		    String s = JsonUtil.mapAndQuote(Utils.makeListFromValues("url",shtml.replaceAll("\"", "\\\"")));
                    return new Result(s, "application/json");
                }
            }
            String s = new String(Utils.encodeBase64((lastResult == null)
                    ? ""
                    : lastResult));
            s = JsonUtil.mapAndQuote(Utils.makeListFromValues("result", s));

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
            s = JsonUtil.mapAndQuote(Utils.makeListFromValues("error", s));
            if (inner instanceof SeesvException) {
                s          = ((SeesvException) inner).getFullMessage();
                printStack = false;
                s          = new String(Utils.encodeBase64(s));
                s          = JsonUtil.mapAndQuote(Utils.makeListFromValues("message", s));
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
                                          Entry entry, Seesv csvUtil,
                                          File destDir, File runDir,
                                          List<String> args,
                                          List<String> newFiles)
            throws Exception {

        try {
            List<IO.Path> files = new ArrayList<IO.Path>();
	    String path;
	    String fileSuffix = ".csv";
	    if(args.contains("-tojson")) fileSuffix = ".json";
	    else if(args.contains("-toxml")) fileSuffix = ".xml";	    
	    if(entry.isFile()) {
		path =  getStorageManager().getEntryFile(entry).toString();
	    } else {
		path = entry.getTypeHandler().getPathForEntry(request, entry, true);
	    }

	    if(entry.getTypeHandler() instanceof RecordTypeHandler) {
		
		files.add(((RecordTypeHandler)entry.getTypeHandler()).getPathForRecordEntry(request, entry,  new Hashtable()));
	    } else {
		files.add(new IO.Path(path));
	    }
            File f = getStorageManager().makeTmpFile(
                         runDir,
                         IOUtil.stripExtension(
                             getStorageManager().getFileTail(
                                 entry.getResource().getPath())) + fileSuffix);

            csvUtil.setOutputFile(f);
	    //	    System.err.println("\tcalling csvUtil.run:" + f);
            csvUtil.run(files);
            if ( !csvUtil.getOkToRun()) {
                return;
            }
            newFiles.addAll(csvUtil.getNewFiles());
            if (Misc.equals("true",
                            csvUtil.getContext().getProperty("db.droptable")) &&
		Misc.equals("true",
                            csvUtil.getContext().getProperty("db.yesreallydroptable"))) {
                request.ensureAdmin();
                String sql = "drop table db_" + csvUtil.getDbId();
                try {
		    System.err.println("Seesv: dropping the table:" + sql);
                    getRepository().getDatabaseManager().executeAndClose(sql);
                } catch (Exception exc) {

		}
            }

            if (Misc.equals(
                    "true",
                    csvUtil.getContext().getProperty("installPlugin"))) {
                request.ensureAdmin();
                for (String file : csvUtil.getNewFiles()) {
                    if (file.endsWith("db.xml")) {
			System.err.println("Seesv: installing plugin:" +file);
			getRepository().getPluginManager().installPlugin(file, true);
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

        return entry.getTypeHandler().isType(ConvertibleTypeHandler.TYPE_CONVERTIBLE) ||
	    entry.getTypeHandler().getTypeProperty("iscsvconvertible",false);
    }



}
