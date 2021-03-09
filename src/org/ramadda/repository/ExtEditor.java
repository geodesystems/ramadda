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

package org.ramadda.repository;


import org.ramadda.repository.auth.AccessException;
import org.ramadda.repository.auth.AuthorizationMethod;
import org.ramadda.repository.auth.Permission;
import org.ramadda.repository.auth.User;
import org.ramadda.repository.database.DatabaseManager;
import org.ramadda.repository.database.Tables;
import org.ramadda.repository.auth.AccessException;

import org.ramadda.repository.metadata.*;
import org.ramadda.repository.metadata.ContentMetadataHandler;
import org.ramadda.repository.metadata.Metadata;
import org.ramadda.repository.metadata.JpegMetadataHandler;
import org.ramadda.repository.output.OutputHandler;
import org.ramadda.repository.output.OutputType;
import org.ramadda.repository.output.XmlOutputHandler;
import org.ramadda.repository.type.Column;
import org.ramadda.repository.type.ProcessFileTypeHandler;
import org.ramadda.repository.type.TypeHandler;
import org.ramadda.repository.type.TypeInsertInfo;
import org.ramadda.repository.util.SelectInfo;
import org.ramadda.util.CategoryBuffer;
import org.ramadda.util.CategoryList;
import org.ramadda.util.FormInfo;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Json;
import org.ramadda.util.Utils;


import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlNodeList;
import ucar.unidata.xml.XmlUtil;


import java.awt.Image;
import java.awt.geom.Rectangle2D;

import java.io.File;
import java.io.FileFilter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import java.net.URL;
import java.net.URLConnection;

import java.nio.file.Files;
import java.nio.file.Path;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.ramadda.util.sql.Clause;
import org.ramadda.util.sql.SqlUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;




/**
 * This class does most of the work of managing repository content
 */
public class ExtEditor extends RepositoryManager {

    /**
     * _more_
     *
     * @param repository _more_
     */
    public ExtEditor(Repository repository) {
        super(repository);
    }


    /** _more_ */
    public static final String SESSION_ENTRIES = "entries";

    /** _more_ */
    public static final String SESSION_TYPES = "types";


    /** _more_ */
    public static final String ARG_EXTEDIT_EDIT = "extedit.edit";

    public static final String ARG_EXTEDIT_THUMBNAIL= "extedit.thumbnail";    


    /** _more_ */
    public static final String ARG_EXTEDIT_URL_TO = "extedit.url.to";

    /** _more_ */
    public static final String ARG_EXTEDIT_URL_PATTERN =
        "extedit.url.pattern";

    /** _more_ */
    public static final String ARG_EXTEDIT_URL_CHANGE = "extedit.url";

    public static final String ARG_EXTEDIT_JS = "extedit.js";

    public static final String ARG_EXTEDIT_JS_CONFIRM =
        "extedit.js.confirm";
    public static final String ARG_EXTEDIT_SOURCE = "extedit.source";    


    /** _more_ */
    public static final String ARG_EXTEDIT_SPATIAL = "extedit.spatial";

    /** _more_ */
    public static final String ARG_EXTEDIT_TEMPORAL = "extedit.temporal";

    /** _more_ */
    public static final String ARG_EXTEDIT_MD5 = "extedit.md5";

    /** _more_ */
    public static final String ARG_EXTEDIT_REPORT = "extedit.report";

    /** _more_ */
    public static final String ARG_EXTEDIT_REPORT_MISSING =
        "extedit.report.missing";

    /** _more_ */
    public static final String ARG_EXTEDIT_REPORT_FILES =
        "extedit.report.files";

    /** _more_ */
    public static final String ARG_EXTEDIT_REPORT_EXTERNAL =
        "extedit.report.external";

    /** _more_ */
    public static final String ARG_EXTEDIT_REPORT_INTERNAL =
        "extedit.report.internal";

    /** _more_ */
    public static final String ARG_EXTEDIT_SETPARENTID =
        "extedit.setparentid";

    /** _more_ */
    public static final String ARG_EXTEDIT_NEWTYPE = "extedit.newtype";

    /** _more_ */
    public static final String ARG_EXTEDIT_NEWTYPE_PATTERN =
        "extedit.newtype.pattern";

    /** _more_ */
    public static final String ARG_EXTEDIT_OLDTYPE = "extedit.oldtype";

    /** _more_ */
    public static final String ARG_EXTEDIT_RECURSE = "extedit.recurse";

    /** _more_ */
    public static final String ARG_EXTEDIT_CHANGETYPE = "extedit.changetype";

    /** _more_ */
    public static final String ARG_EXTEDIT_CHANGETYPE_RECURSE =
        "extedit.changetype.recurse";

    /** _more_ */
    public static final String ARG_EXTEDIT_CHANGETYPE_RECURSE_CONFIRM =
        "extedit.changetype.recurse.confirm";


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processEntryExtEdit(final Request request)
            throws Exception {

	String[] what = new String[]{
	    ARG_EXTEDIT_EDIT,
	    ARG_EXTEDIT_REPORT,
	    ARG_EXTEDIT_CHANGETYPE,
	    ARG_EXTEDIT_CHANGETYPE_RECURSE,
	    ARG_EXTEDIT_URL_CHANGE,
	    ARG_EXTEDIT_JS,
	};
	

        final StringBuilder sb = new StringBuilder();
        final StringBuilder prefix = new StringBuilder();
        final StringBuilder suffix = new StringBuilder();
	Object actionId=null;



        Entry              entry        = getEntryManager().getEntry(request);
        final Entry        finalEntry   = entry;
        final boolean      recurse = request.get(ARG_EXTEDIT_RECURSE, false);
        final EntryManager entryManager = getEntryManager();

        if (request.exists(ARG_EXTEDIT_EDIT)) {
	    final JpegMetadataHandler jpegMetadataHandler = (JpegMetadataHandler) getMetadataManager().getHandler(JpegMetadataHandler.class);
            final boolean doMd5     = request.get(ARG_EXTEDIT_MD5, false);
            final boolean doSpatial = request.get(ARG_EXTEDIT_SPATIAL, false);
            final boolean doThumbnail = request.get(ARG_EXTEDIT_THUMBNAIL, false);
            final boolean doTemporal = request.get(ARG_EXTEDIT_TEMPORAL,
                                           false);
            ActionManager.Action action = new ActionManager.Action() {
                public void run(Object actionId) throws Exception {
                    EntryVisitor walker = new EntryVisitor(request,
                                              getRepository(), actionId,
                                              recurse) {
                        public boolean processEntry(Entry entry,
                                List<Entry> children)
                                throws Exception {
                            boolean changed = false;
                            if (doThumbnail) {
				List<String> urls = new ArrayList<String>();
				getMetadataManager().getThumbnailUrls(request, entry, urls);
				//Only add a thumbnail if there isn't one
				if(urls.size()==0 && entry.isImage()) {
				    Metadata thumbnailMetadata = jpegMetadataHandler.getThumbnail(request, entry);
				    if(thumbnailMetadata!=null) {
					getMetadataManager().addMetadata(entry,thumbnailMetadata);
                                        changed = true;
				    }
				}
			    }

                            if (doSpatial) {
                                Rectangle2D.Double rect = getEntryManager().getBounds(children);
                                if (rect != null) {
                                    if ( !Misc.equals(rect,
						      entry.getBounds())) {
                                        entry.setBounds(rect);
                                        changed = true;
                                    }
                                }
                            }
                            if (doTemporal) {
                                if (setTimeFromChildren(getRequest(), entry,
                                        children)) {
                                    changed = true;
                                }
                            }
                            if (changed) {
                                incrementProcessedCnt(1);
                                append(getPageHandler().getConfirmBreadCrumbs(
                                    getRequest(), entry));
                                append(HtmlUtils.br());
                                getEntryManager().updateEntry(getRequest(), entry);
                            }
                            return true;
                        }
                    };
                    walker.walk(finalEntry);
                    getActionManager().setContinueHtml(actionId,
                            walker.getMessageBuffer().toString());
                }
            };

	    actionId = getActionManager().runAction(action,"extendededitjs","");
	    what = new String[]{ARG_EXTEDIT_EDIT};
	    //            return getActionManager().doAction(request, action, "Walking the tree", "", entry);
        } else  if (request.exists(ARG_EXTEDIT_CHANGETYPE)) {
            TypeHandler newTypeHandler = getRepository().getTypeHandler(
                                             request.getString(
                                                 ARG_EXTEDIT_NEWTYPE, ""));

            entry = changeType(request, entry, newTypeHandler);
            prefix.append(
                getPageHandler().showDialogNote(
                    msg("Entry type has been changed")));
        } else if (request.exists(ARG_EXTEDIT_CHANGETYPE_RECURSE)) {
            final boolean forReal =
                request.get(ARG_EXTEDIT_CHANGETYPE_RECURSE_CONFIRM, false);
            final TypeHandler newTypeHandler = getRepository().getTypeHandler(
                                                   request.getString(
                                                       ARG_EXTEDIT_NEWTYPE,
                                                       ""));
            final String oldType = request.getString(ARG_EXTEDIT_OLDTYPE, "");
            final String pattern =
                request.getString(ARG_EXTEDIT_NEWTYPE_PATTERN, (String) null);
            ActionManager.Action action = new ActionManager.Action() {
                public void run(final Object actionId) throws Exception {
                    EntryVisitor walker = new EntryVisitor(request,
                                              getRepository(), actionId,
                                              true) {
                        public boolean processEntry(Entry entry,
                                List<Entry> children)
                                throws Exception {
                            if ( !oldType.equals(TypeHandler.TYPE_ANY)
                                    && !entry.getTypeHandler().isType(
                                        oldType)) {
                                System.err.println("\tdoesn't match type:"
                                        + oldType);

                                return true;
                            }
                            if ((pattern != null) && (pattern.length() > 0)) {
                                boolean matches =
                                    entry.getName().matches(pattern);
                                if ( !matches
                                        && (entry.getResource().getPath()
                                            != null)) {
                                    matches =
                                        entry.getResource().getPath().matches(
                                            pattern);
                                }

                                if ( !matches) {
                                    System.err.println(
                                        "\tdoesn't match pattern:" + pattern);

                                    return true;
                                }
                            }
                            if (forReal) {
                                System.err.println("\tchanging type:"
                                        + entry.getName());
                                append("Changing type:" + entry.getName()
                                       + "<br>");
                                entry = changeType(request, entry,
                                        newTypeHandler);
                            } else {
                                System.err.println(
                                    "\twould be changing type:"
                                    + entry.getName());
                                append("We would be changing type:"
                                       + entry.getName() + "<br>");
                            }

                            return true;
                        }
                    };
                    walker.walk(finalEntry);
                    getActionManager().setContinueHtml(actionId,
                            walker.getMessageBuffer().toString());
                }
            };

	    actionId = getActionManager().runAction(action,"extendededitjs","");
	    what = new String[]{ARG_EXTEDIT_CHANGETYPE_RECURSE};
        } else if (request.exists(ARG_EXTEDIT_JS)) {
            final boolean forReal =
                request.get(ARG_EXTEDIT_JS_CONFIRM, false);
	    final String js = request.getString(ARG_EXTEDIT_SOURCE,"");
            ActionManager.Action action = new ActionManager.Action() {
                public void run(final Object actionId) throws Exception {
		    final org.mozilla.javascript.Context cx =
			org.mozilla.javascript.Context.enter();
		    final org.mozilla.javascript.Scriptable scope =
			cx.initSafeStandardObjects();
		    final StringBuilder buffer = new StringBuilder();
		    cx.evaluateString(scope, "var confirmed = "+ forReal+";", "<cmd>", 1, null);
		    final org.mozilla.javascript.Script script = cx.compileString(js, "code", 0, null);
		    final List<Entry> allEntries = new ArrayList<Entry>();
		    //Do the holder because the walker needs a final JsContext but the
		    //JsContext needs the walker
		    final Request theRequest =  request;
		    final JsContext[] holder = new JsContext[1];
                    EntryVisitor walker = new EntryVisitor(request,
                                              getRepository(), actionId,
                                              true) {
			    public boolean processEntry(Entry entry,
							List<Entry> children)
                                throws Exception {
				allEntries.add(entry);
				try {
				    scope.put("entry", scope, entry);
				    Object o = script.exec(cx, scope);
				} catch(Exception exc) {
				    append("An error occurred processing entry:" + entry+"\n" + exc);
				    return false;
				}
				return true;
			    }
			    public void finished() {
				super.finished();
				for(Entry entry: allEntries) {
				    getEntryManager().removeFromCache(entry);
				}				    
				if(forReal) {
				    resetMessageBuffer();
				    try {
					for(Entry entry:  holder[0].getChangedEntries()) {
					    append("Updated entry:" + entry+"\n");
					    incrementProcessedCnt(1);
					    getEntryManager().updateEntry(request, entry);
					}
				    } catch(Exception exc) {
					append("An error occurred updating entry\n" + exc);
				    }					
				}
			    }
			};
		    final JsContext jsContext = new JsContext(walker,forReal);
		    holder[0] = jsContext;
		    scope.put("ctx", scope, jsContext);
                    walker.walk(finalEntry);
                    getActionManager().setContinueHtml(actionId,
						       walker.getMessageBuffer().toString());
                }
            };
	    actionId = getActionManager().runAction(action,"extendededitjs","");
	    what = new String[]{ARG_EXTEDIT_JS};

        } else if (request.exists(ARG_EXTEDIT_URL_CHANGE)) {
            final String pattern = request.getString(ARG_EXTEDIT_URL_PATTERN,
                                       (String) null);
            final String to = request.getString(ARG_EXTEDIT_URL_TO,
                                  (String) null);
            ActionManager.Action action = new ActionManager.Action() {
                public void run(final Object actionId) throws Exception {
                    EntryVisitor walker = new EntryVisitor(request,
                                              getRepository(), actionId,
                                              true) {
                        public boolean processEntry(Entry entry,
                                List<Entry> children)
                                throws Exception {
                            String path = entry.getResource().getPath();
                            if (path == null) {
                                return true;
                            }
                            String newPath = path.replaceAll(pattern, to);
                            if ( !path.equals(newPath)) {
                                entry.getResource().setPath(newPath);
                                getEntryManager().updateEntry(request, entry);
                                append("Changing URL:" + entry.getName()
                                       + "<br>");
                            }

                            return true;
                        }
                    };
                    walker.walk(finalEntry);
                    getActionManager().setContinueHtml(actionId,
                            walker.getMessageBuffer().toString());
                }
            };

	    actionId = getActionManager().runAction(action,"","");
	    what = new String[]{ARG_EXTEDIT_URL_CHANGE};
        } else  if (request.exists(ARG_EXTEDIT_REPORT)) {
            final long[] size     = { 0 };
            final int[]  numFiles = { 0 };
            final boolean showMissing =
                request.get(ARG_EXTEDIT_REPORT_MISSING, false);
            final boolean showFiles = request.get(ARG_EXTEDIT_REPORT_FILES,
                                          false);
            EntryVisitor walker = new EntryVisitor(request, getRepository(),
                                      null, true) {
                @Override
                public boolean processEntry(Entry entry, List<Entry> children)
                        throws Exception {
                    for (Entry child : children) {
                        String url =
                            request.entryUrl(getRepository().URL_ENTRY_SHOW,
                                             child);
                        if (child.isFileType()) {
                            boolean exists = child.getResource().fileExists();
                            if ( !exists && !showMissing) {
                                continue;
                            }
                            if (exists && !showFiles) {
                                continue;
                            }
                            append("<tr><td>");
                            append(getPageHandler().getBreadCrumbs(request,
                                    child, entry));
                            append("</td><td align=right>");
                            if (exists) {
                                File file = child.getFile();
                                size[0] += file.length();
                                numFiles[0]++;
                                append("" + file.length());
                            } else {
                                append("Missing:" + child.getResource());
                            }
                            append("</td>");
                            append("<td>");
                            if (child.getResource().isStoredFile()) {
                                append("***");
                            }
                            append("</td>");
                            append("</tr>");
                        } else if (child.isGroup()) {}
                        else {}
                    }

                    return true;
                }
            };
            walker.walk(entry);
	    suffix.append(HtmlUtils.openInset(5, 30, 20, 0));
            suffix.append("<table><tr><td><b>" + msg("File") + "</b></td><td><b>"
                      + msg("Size") + "</td><td></td></tr>");
            suffix.append(walker.getMessageBuffer());
            suffix.append("<tr><td><b>" + msgLabel("Total")
                      + "</td><td align=right>"
                      + HtmlUtils.b(formatFileLength(size[0]))
                      + "</td></tr>");
            suffix.append("</table>");
            suffix.append("**** - File managed by RAMADDA");
	    suffix.append(HtmlUtils.closeInset());
	    what = new String[]{ARG_EXTEDIT_REPORT};
        }

        /*
	  else if(request.exists(ARG_EXTEDIT_SETPARENTID)) {
            ActionManager.Action action = new ActionManager.Action() {
                public void run(Object actionId) throws Exception {
                    StringBuilder sb = new StringBuilder();
                    setParentId(request, actionId, sb, recurse, entry.getId(), new int[]{0}, new int[]{0});
                    getActionManager().setContinueHtml(actionId,
                                                       sb.toString());
                }
            };
            return getActionManager().doAction(request, action,
                                               "Setting parent ids", "", entry);

        }
        */

        /*
	  else if(request.exists(ARG_EXTEDIT_MD5)) {
            ActionManager.Action action = new ActionManager.Action() {
                public void run(Object actionId) throws Exception {
                    StringBuilder sb = new StringBuilder();
                    setMD5(request, actionId, sb, recurse, entry.getId(), new int[]{0}, new int[]{0});
                    if(sb.length()==0) sb.append("No checksums set");
                    getActionManager().setContinueHtml(actionId,
                                                       sb.toString());
                }
            };
            return getActionManager().doAction(request, action,
                                               "Setting MD5 Checksum", "", entry);

        }
        */





        getPageHandler().entrySectionOpen(request, entry, sb,
                                          "Extended Edit", true);


	sb.append(prefix);
	if(actionId!=null) {
	    String url = getRepository().getUrlBase() +"/status?actionid=" + actionId +"&output=json";
	    sb.append(HU.b("Results"));
	    HU.div(sb,"",HU.attrs("class","ramadda-action-results", "id",actionId.toString()));
	    HU.script(sb,"Utils.handleActionResults('" + actionId +"','" + url+"');\n");
	}




	Consumer<String> opener = label->{
	    sb.append(formHeader(label));
	    sb.append(HU.openInset(5, 30, 20, 0));
	};
        BiConsumer<String, String> closer= (id,label) -> {
	    sb.append(HU.p());
	    sb.append(HU.submit(label, id));
	    sb.append(HU.closeInset());
	};


	List<HtmlUtils.Selector> tfos = getTypeHandlerSelectors(request,true, true, entry);

	for(String form: what) {
	    sb.append(request.form(getRepository().URL_ENTRY_EXTEDIT,
				   HU.attr("name", "entryform")));
	    sb.append(HU.hidden(ARG_ENTRYID, entry.getId()));
	    if(form.equals(ARG_EXTEDIT_EDIT)) {
		opener.accept("Spatial and Temporal Metadata");
		sb.append(HU.labeledCheckbox(ARG_EXTEDIT_SPATIAL, "true",
						    false, "Set spatial metadata"));
		sb.append(HU.br());
		sb.append(HU.labeledCheckbox(ARG_EXTEDIT_TEMPORAL, "true",
						    false, "Set temporal metadata"));
		sb.append(HU.br());
		sb.append(HU.labeledCheckbox(ARG_EXTEDIT_THUMBNAIL, "true",
						    false, "Add image thumbnails"));	
		sb.append(HU.br());
		sb.append(HU.labeledCheckbox(ARG_EXTEDIT_RECURSE, "true",
						    true, "Recurse"));
		closer.accept(form,"Set metadata");
	    } else if(form.equals(ARG_EXTEDIT_REPORT)){
		opener.accept("File Listing");
		sb.append(HU.checkbox(ARG_EXTEDIT_REPORT_MISSING, "true",
					     true) + " " + msg("Show missing files")  + "<br>");
		sb.append(HU.checkbox(ARG_EXTEDIT_REPORT_FILES, "true", true)
			  + " " + msg("Show OK files") + "<p>");
		closer.accept(form, "Generate File Listing");
	    }  else if(form.equals(ARG_EXTEDIT_CHANGETYPE)){
		opener.accept("Change Entry Type");
		sb.append(msgLabel("New type"));
		sb.append(HU.space(1));
		sb.append(HU.select(ARG_EXTEDIT_NEWTYPE, tfos));
		sb.append(HU.p());
		List<Column> columns = entry.getTypeHandler().getColumns();
		if ((columns != null) && (columns.size() > 0)) {
		    StringBuilder note = new StringBuilder();
		    for (Column col : columns) {
			if (note.length() > 0) {
			    note.append(", ");
			}
			note.append(col.getLabel());
		    }
		    sb.append(msgLabel("Note: this metadata would be lost") + note);
		}

		closer.accept(form, "Change type of this entry");
	    }  else if(form.equals(ARG_EXTEDIT_CHANGETYPE_RECURSE)){
		opener.accept("Change Descendents Entry Type");
		sb.append(HU.formTable());
		HU.formEntry(sb, msgLabel("Old type"),
			     HU.select(ARG_EXTEDIT_OLDTYPE,
				       tfos));

		HU.formEntry(sb, msgLabel("Regexp Pattern"),
			     HU.input(ARG_EXTEDIT_NEWTYPE_PATTERN, "") + " "
			     + msg("Only change type for entries that match this pattern"));

		HU.formEntry(sb, msgLabel("New type"),
			     HU.select(ARG_EXTEDIT_NEWTYPE,
				       tfos));
		HU.formEntry(sb, "",
			     HU.checkbox(ARG_EXTEDIT_CHANGETYPE_RECURSE_CONFIRM, "true",
					 false) + " " + msg("Yes, change them"));
		sb.append(HU.formTableClose());
		closer.accept(form,"Change the type of all descendent entries");
	    }	else if(form.equals(ARG_EXTEDIT_URL_CHANGE)){		
		opener.accept("Change Descendents URL Path");
		sb.append(HU.formTable());
		HU.formEntry(sb, msgLabel("Pattern"),
			     HU.input(ARG_EXTEDIT_URL_PATTERN, request.getString(ARG_EXTEDIT_URL_PATTERN,"")));
		HU.formEntry(sb,msgLabel("To"),  HU.input(ARG_EXTEDIT_URL_TO, request.getString(ARG_EXTEDIT_URL_TO,"")));
		sb.append(HU.formTableClose());
		closer.accept(form,"Change URLs");
	    } else if(form.equals(ARG_EXTEDIT_JS)){
		opener.accept("Process with Javascript");
		sb.append(HU.formTable());
		String ex =  "ctx.print('Processing: ' + entry.getName());\nif(confirmed) {\n\tctx.entryChanged(entry);\n}";
		ex = request.getString(ARG_EXTEDIT_SOURCE, ex);
		HU.formEntry(sb,  msgLabel("Javascript"),
			     HU.textArea(ARG_EXTEDIT_SOURCE, ex,10,80));
		
		HU.formEntry(sb, "",
			     HU.checkbox(
					 ARG_EXTEDIT_JS_CONFIRM, "true",
					 request.get(ARG_EXTEDIT_JS_CONFIRM,false)) + " " + msg("Yes, apply the Javascript"));
		sb.append(HU.formTableClose());
		closer.accept(form,"Apply Javascript");
	    }
	    sb.append(HU.formClose());
	}
	sb.append(suffix);


        return getEntryManager().makeEntryEditResult(request, entry, "Extended Edit", sb);
    }


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param fileType _more_
     * @param nonFileType _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<HtmlUtils.Selector> getTypeHandlerSelectors(Request request,
            boolean fileType, boolean nonFileType, Entry entry)
            throws Exception {
        List<String> sessionTypes =
            (List<String>) getSessionManager().getSessionProperty(request,
                SESSION_TYPES);

        List<HtmlUtils.Selector> tfos  = new ArrayList<HtmlUtils.Selector>();

        HashSet<String>          first = new HashSet<String>();
        if (sessionTypes != null) {
            first.addAll(sessionTypes);
        }

        CategoryList<HtmlUtils.Selector> cats =
            new CategoryList<HtmlUtils.Selector>();
        for (String preload : EntryManager.PRELOAD_CATEGORIES) {
            cats.get(preload);
        }



        for (TypeHandler typeHandler : getRepository().getTypeHandlers()) {
            if ( !typeHandler.getForUser()) {
                continue;
            }
            if ( !fileType && !typeHandler.isGroup()) {
                continue;
            }
            if ( !nonFileType && typeHandler.isGroup()) {
                continue;
            }

            if ((entry != null)
                    && !entry.getTypeHandler().canChangeTo(typeHandler)) {
                continue;
            }

            HtmlUtils.Selector tfo =
                new HtmlUtils.Selector(
                    HtmlUtils.space(2) + typeHandler.getLabel(),
                    typeHandler.getType(), typeHandler.getTypeIconUrl());
            //Add the seen ones first
            if (first.contains(typeHandler.getType())) {
                if (tfos.size() == 0) {
                    tfos.add(new HtmlUtils.Selector("Recent", "", null, 0,
                            true));
                }
                tfos.add(tfo);
            }

            cats.add(typeHandler.getCategory(), tfo);
        }
        for (String cat : cats.getCategories()) {
            List<HtmlUtils.Selector> selectors = cats.get(cat);
            if (selectors.size() > 0) {
                tfos.add(new HtmlUtils.Selector(cat, "",
                        getRepository().getIconUrl("/icons/blank.gif"), 0, 0,
                        true));
                tfos.addAll(selectors);
            }
        }

        return tfos;
    }

    /*
        private void setMD5(Request request, StringBuilder sb, boolean recurse, String entryId, int []totalCnt, int[] setCnt) throws Exception {
            if(!getRepository().getActionManager().getActionOk(actionId)) {
                return;
            }
            Statement stmt = getDatabaseManager().select(SqlUtil.comma(new String[]{Tables.ENTRIES.COL_ID,
                                                                                    Tables.ENTRIES.COL_TYPE,
                                                                                    Tables.ENTRIES.COL_MD5,
                                                                                    Tables.ENTRIES.COL_RESOURCE}),
                Tables.ENTRIES.NAME,
                Clause.eq(Tables.ENTRIES.COL_PARENT_GROUP_ID, entryId));
            SqlUtil.Iterator iter = getDatabaseManager().getIterator(stmt);
            ResultSet        results;

            while ((results = iter.getNext()) != null) {
                totalCnt[0]++;
                int col = 1;
                String id = results.getString(col++);
                String type= results.getString(col++);
                String md5 = results.getString(col++);
                String resource = results.getString(col++);
                if(new File(resource).exists() && !Utils.stringDefined(md5)) {
                    setCnt[0]++;
                    Entry entry = getEntry(request, id);
                    if(!getAccessManager().canDoAction(request, entry,
                                                       Permission.ACTION_EDIT)) {
                        continue;
                    }
                    md5 = ucar.unidata.util.IOUtil.getMd5(resource);
                    getDatabaseManager().update(Tables.ENTRIES.NAME,
                                                Tables.ENTRIES.COL_ID,
                                                id, new String[]{Tables.ENTRIES.COL_MD5},
                                                new String[]{md5});
                    sb.append(getPageHandler().getConfirmBreadCrumbs(request, entry));
                    sb.append(HtmlUtils.br());
                }
                getActionManager().setActionMessage(actionId,
                                                    "Checked " + totalCnt[0] +" entries<br>Changed " + setCnt[0] +" entries");

                if(recurse) {
                    TypeHandler typeHandler = getRepository().getTypeHandler(type);
                    if(typeHandler.isGroup()) {
                        setMD5(request, actionId,  sb, recurse, id, totalCnt, setCnt);
                    }
                }
            }
            getDatabaseManager().closeStatement(stmt);
        }
        */




    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processEntryTypeChange(Request request) throws Exception {

        String      fromIds = request.getString(ARG_FROM, "");
        List<Entry> entries = new ArrayList<Entry>();
        for (String id : StringUtil.split(fromIds, ",", true, true)) {
            Entry entry = getEntryManager().getEntry(request, id, false);
            if (entry == null) {
                throw new RepositoryUtil.MissingEntryException(
                    "Could not find entry:" + id);
            }
            if (entry.isTopEntry()) {
                StringBuilder sb = new StringBuilder();
                sb.append(
                    getPageHandler().showDialogNote(
                        msg("Cannot copy top-level folder")));

                return new Result(msg("Entry Delete"), sb);
            }
            entries.add(entry);
        }


        if (entries.size() == 0) {
            throw new IllegalArgumentException("No entries specified");
        }

        if (request.exists(ARG_CANCEL)) {
            return new Result(
                request.entryUrl(
                    getRepository().URL_ENTRY_SHOW, entries.get(0)));
        }


        StringBuffer sb = new StringBuffer();
        if (request.exists(ARG_CONFIRM)) {
            TypeHandler newTypeHandler = getRepository().getTypeHandler(
                                             request.getString(
                                                 ARG_EXTEDIT_NEWTYPE, ""));
            request.ensureAuthToken();

            sb.append(msgLabel("The following entries have been changed"));
            sb.append("<ul>");
            for (Entry entry : entries) {
                if ( !getAccessManager().canDoAction(request, entry,
                        Permission.ACTION_EDIT)) {
                    throw new IllegalArgumentException(
                        "Whoa dude, you can't edit this entry:"
                        + entry.getName());
                }
                entry = changeType(request, entry, newTypeHandler);
                String icon = newTypeHandler.getIconProperty(null);
                if (icon != null) {
                    icon = newTypeHandler.getIconUrl(icon);
                }
                sb.append(HtmlUtils.href(getEntryManager().getEntryURL(request, entry),
                                         HtmlUtils.img(icon) + " "
                                         + entry.getName()));
                sb.append("<br>");
            }
            sb.append("</ul>");





            return new Result(msg(""), sb);
        }



        List<HtmlUtils.Selector> tfos = getTypeHandlerSelectors(request,
                                            true, true, null);

        request.formPostWithAuthToken(sb,
                                      getRepository().URL_ENTRY_TYPECHANGE);
        sb.append(HtmlUtils.hidden(ARG_FROM, fromIds));

        sb.append(HtmlUtils.p());

        StringBuffer inner = new StringBuffer();
        inner.append(msg("Are you sure you want to change the entry types?"));
        inner.append(HtmlUtils.p());
        inner.append(HtmlUtils.formTable());
        inner.append(
            HtmlUtils.formEntry(
                msgLabel("New type"),
                HtmlUtils.select(ARG_EXTEDIT_NEWTYPE, tfos)));

        HtmlUtils.formTableClose(inner);

        sb.append(
            getPageHandler().showDialogQuestion(
                inner.toString(),
                HtmlUtils.buttons(
                    HtmlUtils.submit(
                        msg("Yes, change the entry types"),
                        ARG_CONFIRM), HtmlUtils.submit(
                            msg("Cancel"), ARG_CANCEL))));
        sb.append(HtmlUtils.formClose());
        sb.append("<table>");
        sb.append("<tr><td><b>Entry</b></td><td><b>Type</b></td></tr>");
        for (Entry entry : entries) {
            sb.append("<tr><td>");
            sb.append(HtmlUtils.img(entry.getTypeHandler().getTypeIconUrl()));
            sb.append(" ");
            sb.append(entry.getName());
            sb.append("</td><td>");
            sb.append(entry.getTypeHandler().getLabel());
            sb.append("</td></tr>");
        }
        sb.append("</table>");


        return new Result(msg(""), sb);

    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param newTypeHandler _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Entry changeType(Request request, Entry entry,
                             TypeHandler newTypeHandler)
            throws Exception {
        if ( !getAccessManager().canDoAction(request, entry,
                                             Permission.ACTION_EDIT)) {
            throw new AccessException("Cannot edit:" + entry.getLabel(),
                                      request);
        }
        getEntryManager().addSessionType(request, newTypeHandler.getType());

        Connection connection = getDatabaseManager().getConnection();
        try {
            Statement extraStmt = connection.createStatement();
            entry.getTypeHandler().deleteEntry(request, extraStmt,
                    entry.getId(), entry.getParentEntry(),
                    entry.getTypeHandler().getEntryValues(entry));
        } finally {
            getDatabaseManager().closeConnection(connection);
        }

        getDatabaseManager().update(Tables.ENTRIES.NAME,
                                    Tables.ENTRIES.COL_ID, entry.getId(),
                                    new String[] { Tables.ENTRIES.COL_TYPE },
                                    new String[] {
                                        newTypeHandler.getType() });
        getEntryManager().removeFromCache(entry);
        entry = newTypeHandler.changeType(request, entry);

        return entry;
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param groups _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result changeType(Request request, List<Entry> groups,
                             List<Entry> entries)
            throws Exception {
        /*
        if ( !request.getUser().getAdmin()) {
            return null;
        }
        TypeHandler typeHandler =
            getRepository().getTypeHandler(TypeHandler.TYPE_HOMEPAGE);


        List<Entry> changedEntries = new ArrayList<Entry>();

        entries.addAll(groups);

        for(Entry entry: entries) {
            if(entry.isGroup()) {
                entry.setTypeHandler(typeHandler);
                changedEntries.add(entry);
            }
        }
        insertEntries(request, changedEntries, false);*/
        return new Result("Metadata", new StringBuilder("OK"));
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param children _more_
     *
     *
     * @return _more_
     * @throws Exception _more_
     */
    public boolean setTimeFromChildren(Request request, Entry entry,
                                       List<Entry> children)
            throws Exception {
        if (children == null) {
            children = getEntryManager().getChildren(request, entry);
        }
        long minTime = Long.MAX_VALUE;
        long maxTime = Long.MIN_VALUE;
        for (Entry child : children) {
            minTime = Math.min(minTime, child.getStartDate());
            maxTime = Math.max(maxTime, child.getEndDate());
        }
        boolean changed = false;

        if (minTime != Long.MAX_VALUE) {
            long diffStart = minTime - entry.getStartDate();
            long diffEnd   = maxTime - entry.getEndDate();
            //We seem to lose some time resolution when we store so only assume a change
            //when the time differs by more than 5 seconds
            changed = (diffStart < -10000) || (diffStart > 10000)
                      || (diffEnd < -10000) || (diffEnd > 10000);
            entry.setStartDate(minTime);
            entry.setEndDate(maxTime);

        }

        return changed;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    public void setBoundsFromChildren(Request request, Entry entry)
            throws Exception {
        if (entry == null) {
            return;
        }
        Rectangle2D.Double rect = getEntryManager().getBounds(getEntryManager().getChildren(request, entry));
        if (rect != null) {
            entry.setBounds(rect);
            getEntryManager().updateEntry(request, entry);
        }
    }


    public static class JsContext {
	private StringBuilder msg = new StringBuilder();
	private List<Entry> changedEntries = new ArrayList<Entry>();	
	private EntryVisitor visitor;
	private boolean confirm;
	
	public JsContext(EntryVisitor visitor, boolean confirm) {
	    this.visitor= visitor;
	    this.confirm = confirm;
	}
	public void entryChanged(Entry e) {
	    if(confirm) {
		changedEntries.add(e);
		visitor.incrementProcessedCnt(1);
	    }
	}
	public List<Entry> getChangedEntries() {
	    return changedEntries;
	}

	public Date getDate(String d) throws Exception {
	    return DateUtil.parse(d);
	}
	public void print(Object msg) {
	    visitor.append(msg+"\n");
	}
    }


    public static void main(String[]args) {
	String name = "Admin Settings";
	String text = "admin";
	System.err.println(name.regionMatches(true,0,text,0,name.length()));
	System.err.println(name.regionMatches(true,0,text,0,text.length()));	
    }


}

