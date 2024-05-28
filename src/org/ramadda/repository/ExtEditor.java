/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository;


import org.ramadda.repository.auth.AccessException;
import org.ramadda.repository.database.DatabaseManager;
import org.ramadda.repository.database.Tables;

import org.ramadda.repository.metadata.*;
import org.ramadda.repository.metadata.Metadata;
import org.ramadda.repository.output.OutputHandler;
import org.ramadda.repository.output.OutputType;
import org.ramadda.repository.type.Column;
import org.ramadda.repository.type.TypeHandler;
import org.ramadda.repository.util.SelectInfo;

import org.ramadda.util.CategoryBuffer;
import org.ramadda.util.CategoryList;
import org.ramadda.util.FormInfo;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;
import org.ramadda.util.Utils;

import org.ramadda.util.ImageUtils;
import java.awt.Image;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;

import java.awt.geom.Rectangle2D;

import java.io.File;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.ramadda.util.sql.Clause;
import org.ramadda.util.sql.SqlUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;


import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;



/**
 * This class handles the extended entry edit functions
 */
@SuppressWarnings("unchecked")
public class ExtEditor extends RepositoryManager {


    /** _more_ */
    public static final String SESSION_ENTRIES = "entries";

    /** _more_ */
    public static final String SESSION_TYPES = "types";


    /** _more_ */
    public static final String ARG_EXTEDIT_EDIT = "extedit.edit";

    public static final String ARG_EXTEDIT_REINDEX = "extedit.reindex";


    public static final String ARG_EXTEDIT_EXCLUDE = "excludeentries";

    public static final String ARG_EXTEDIT_THUMBNAIL= "extedit.thumbnail";    


    public static final String ARG_EXTEDIT_TYPE= "extedit.type";

    public static final String ARG_EXTEDIT_THISONE= "extedit.thisone";    


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
    public static final String ARG_EXTEDIT_METADATA = "extedit.metadata";    

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


    public static final String ARG_EXTEDIT_ADDALIAS = "extedit.addalias";
    public static final String ARG_EXTEDIT_ADDALIAS_NOTFIRST = "extedit.addalias.notfirst";    

    public static final String ARG_EXTEDIT_ADDALIAS_CONFIRM = "extedit.addalias.confirm";

    public static final String ARG_EXTEDIT_ADDALIAS_TEMPLATE = "extedit.addalias.template";        

    /**
     * _more_
     *
     * @param repository _more_
     */
    public ExtEditor(Repository repository) {
        super(repository);
    }


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
	    //	    ARG_EXTEDIT_REINDEX,
	    ARG_EXTEDIT_ADDALIAS,	    
	    ARG_EXTEDIT_CHANGETYPE,
	    ARG_EXTEDIT_CHANGETYPE_RECURSE,
	    ARG_EXTEDIT_URL_CHANGE,
	    ARG_EXTEDIT_REPORT,
	    ARG_EXTEDIT_JS,
	};
	



        final StringBuilder sb = new StringBuilder();
        final StringBuilder prefix = new StringBuilder();
        final StringBuilder suffix = new StringBuilder();
        final StringBuilder formSuffix = new StringBuilder();


	Object actionId=null;
	boolean canCancel = false;

        Entry              entry        = getEntryManager().getEntry(request);
        final Entry        finalEntry   = entry;
        final boolean      recurse = request.get(ARG_EXTEDIT_RECURSE, false);
        final EntryManager entryManager = getEntryManager();

        if (request.exists(ARG_EXTEDIT_EDIT)) {
            getAuthManager().ensureAuthToken(request);
	    final JpegMetadataHandler jpegMetadataHandler = (JpegMetadataHandler) getMetadataManager().getHandler(JpegMetadataHandler.class);
            final boolean doMd5     = request.get(ARG_EXTEDIT_MD5, false);
            final boolean doSpatial = request.get(ARG_EXTEDIT_SPATIAL, false);
            final boolean doThumbnail = request.get(ARG_EXTEDIT_THUMBNAIL, false);
            final boolean doTemporal = request.get(ARG_EXTEDIT_TEMPORAL, false);
	    final boolean doMetadata = request.get(ARG_EXTEDIT_METADATA, false);
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
				    Metadata thumbnailMetadata = jpegMetadataHandler.getThumbnail(request, entry,null);
				    if(thumbnailMetadata!=null) {
					getMetadataManager().addMetadata(request,entry,thumbnailMetadata);
                                        changed = true;
				    }
				}
			    }

			    if(doMetadata) {
				List<Entry> entries = new ArrayList<Entry>();
				entries.add(entry);
				List<Entry> changedEntries = getEntryManager().addInitialMetadata(request,
								     entries,
								     false,false);
				entry.getTypeHandler().addInitialMetadata(request, entry,true);
				changed = true;
			    }



                            if (doSpatial) {
                                Rectangle2D.Double rect = getEntryUtil().getBounds(request,children);
                                if (rect != null) {
                                    if ( !Misc.equals(rect,
						      entry.getBounds(request))) {
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
                                append(HU.br());
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

	    actionId = getActionManager().runAction(action,"Change Metadata","",finalEntry);
	    what = new String[]{ARG_EXTEDIT_EDIT};
	    //            return getActionManager().doAction(request, action, "Walking the tree", "", entry);
	} else  if (request.exists(ARG_EXTEDIT_REINDEX)) {
	    //	    final boolean doMetadata = request.get(ARG_EXTEDIT_METADATA, false);
	    /* not implemented yet
            ActionManager.Action action = new ActionManager.Action() {
                public void run(Object actionId) throws Exception {
		    try {
			getSearchManager().reindexLuceneTreeFields(actionId, finalEntry);
			getActionManager().setContinueHtml(actionId,
							   "Reindexing finished");
		    } catch(Throwable thr) {
			getActionManager().setContinueHtml(actionId,
							   "An error occurred reindexing entry\n" + thr);
			thr.printStackTrace();
			return;
		    }
                }
            };

	    actionId = getActionManager().runAction(action,"Reindex","",finalEntry);
	    canCancel = true;
	    what = new String[]{ARG_EXTEDIT_REINDEX};
	    */
        } else  if (request.exists(ARG_EXTEDIT_CHANGETYPE)) {
            getAuthManager().ensureAuthToken(request);
            TypeHandler newTypeHandler = getRepository().getTypeHandler(
                                             request.getString(
                                                 ARG_EXTEDIT_NEWTYPE, ""));

            entry = changeType(request, entry, newTypeHandler);
            prefix.append(
                getPageHandler().showDialogNote(
                    msg("Entry type has been changed")));
        } else  if (request.exists(ARG_EXTEDIT_ADDALIAS) || request.exists(ARG_EXTEDIT_ADDALIAS_CONFIRM)) {
	    what = new String[]{ARG_EXTEDIT_ADDALIAS};
	    boolean firstTime = !request.exists(ARG_EXTEDIT_ADDALIAS_NOTFIRST);
            boolean confirmed =  request.exists(ARG_EXTEDIT_ADDALIAS_CONFIRM);
	    if(confirmed) {
		getAuthManager().ensureAuthToken(request);
	    }
	    boolean defaultChecked = firstTime;
	    String template = request.getString(ARG_EXTEDIT_ADDALIAS_TEMPLATE,"").trim();
	    formSuffix.append(HU.hidden(ARG_EXTEDIT_ADDALIAS_NOTFIRST,"true"));
	    StringBuilder list = new StringBuilder();
	    list.append("<ul>");
	    int cnt = 0;
	    List<String> isSelected = request.get("aliasentry",new ArrayList<String>());
	    List<Entry> children = getEntryManager().getChildren(request, entry);
	    list.append("<table class=table-spaced>");
	    String thStyle=HU.style("font-weight:bold;");
	    list.append(HU.tr(HU.td("")+
			      HU.td("Entry",thStyle)+
			      HU.td("Alias",thStyle)));
            for(int entryIdx=0;entryIdx<children.size();entryIdx++) {
		Entry child = children.get(entryIdx);
		if(!getAccessManager().canDoEdit(request, child)) {
		    continue;
		} 
		List<Metadata> metadataList =
		    getMetadataManager().findMetadata(request, child,
						      new String[]{ContentMetadataHandler.TYPE_ALIAS},false);
		boolean selected =
		    isSelected.contains(child.getId());

		String alias = Utils.makeID(IO.stripExtension(child.getName()));
		alias = Utils.makeID(template.replace("${name}",alias)).toLowerCase();
		boolean checked  = defaultChecked || selected;
		if(metadataList!=null && metadataList.size()>0) {
		    checked = false;
		}		    

		list.append("<tr>");
		HU.td(list,HU.checkbox("aliasentry",child.getId(),checked),HU.attr("width","20px"));
		HU.td(list,HU.href(getEntryManager().getEntryURL(request, child), child.getName()));
		if(metadataList!=null && metadataList.size()>0) {
		    HU.td(list,"already has an alias: " + HU.italics(metadataList.get(0).getAttr1()),HU.attr("colspan","2"));
		    list.append("</tr>");		    
		    continue;
		}

		HU.td(list,HU.italics(alias));
		List<Entry> entries =  getEntryManager().getEntriesFromAlias(request,  alias);
		if(entries.size()>0) {
		    list.append(HU.td("alias already exists"));
		    list.append("</tr>");		    
		    continue;
		}


		if(selected && confirmed) {
		    cnt++;
		    getRepository().getMetadataManager().addMetadata(request,child,
								     new Metadata(request.getRepository().getGUID(),
										  entry.getId(),
										  ContentMetadataHandler.TYPE_ALIAS, false,
										  alias, null, null, null, null));
		    getEntryManager().updateEntry(request, child);
		    list.append(HU.td("alias added"));
		} else {
		    list.append(HU.td(""));
		}
		list.append("</tr>");
	    }
	    list.append("</table>");
	    if(confirmed) {
		if(cnt==0)
		    prefix.append(
				  getPageHandler().showDialogNote(
								  msg("No aliases have been added")));
		else
		    prefix.append(
				  getPageHandler().showDialogNote(
								  msg("Aliases have been added")));	    	    
	    }
	    formSuffix.append(list);
        } else if (request.exists(ARG_EXTEDIT_CHANGETYPE_RECURSE)) {
            getAuthManager().ensureAuthToken(request);
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
                            if (!oldType.equals("") &&
				!oldType.equals(TypeHandler.TYPE_ANY) &&
				!entry.getTypeHandler().isType(oldType)) {
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
				    //System.err.println("\tdoesn't match pattern:" + pattern);
                                    return true;
                                }
                            }
                            if (forReal) {
                                append("Changing type:" + entry.getName() + "<br>");
                                entry = changeType(request, entry,
                                        newTypeHandler);
                            } else {
				String label = "Would change: " + entry.getName();
				if(Utils.stringDefined(entry.getResource().getPath())) {
				    label+=HU.space(2)+"file: " + getStorageManager().getOriginalFilename(entry.getResource().getPathName());
				}
                                append(label + "<br>");
                            }
                            return true;
                        }
                    };
                    walker.walk(finalEntry);
                    getActionManager().setContinueHtml(actionId,
                            walker.getMessageBuffer().toString());
                }
            };

	    actionId = getActionManager().runAction(action,"Change Type","",finalEntry);
	    what = new String[]{ARG_EXTEDIT_CHANGETYPE_RECURSE};
        } else if (request.exists(ARG_EXTEDIT_JS)) {
            getAuthManager().ensureAuthToken(request);
            final boolean forReal =
                request.get(ARG_EXTEDIT_JS_CONFIRM, false);
	    final String js = request.getString(ARG_EXTEDIT_SOURCE,"");
	    getSessionManager().putSessionProperty(request,"extedit",js);

            final String type = request.getString(ARG_EXTEDIT_TYPE, (String) null);
	    final boolean thisOne = request.get(ARG_EXTEDIT_THISONE,false);
	    final boolean anyFile = Misc.equals(TypeHandler.TYPE_ANY, type);
	    String exc = request.getString(ARG_EXTEDIT_EXCLUDE,"");
	    final HashSet<String> not = new HashSet<String>();
	    for(String id:Utils.split(exc,"\n",true,true)) {
		not.add(id);
	    }

            ActionManager.Action action = new ActionManager.Action() {
                public void run(final Object actionId) throws Exception {
		    final org.mozilla.javascript.Context ctx =
			org.mozilla.javascript.Context.enter();
		    final org.mozilla.javascript.Scriptable scope =
			ctx.initSafeStandardObjects();
		    final StringBuilder buffer = new StringBuilder();
		    ctx.evaluateString(scope, "var confirmed = "+ forReal+";", "<cmd>", 1, null);
		    final org.mozilla.javascript.Script script = ctx.compileString(js, "code", 0, null);
		    //Do the holder because the walker needs a final JsContext but the
		    //JsContext needs the walker
		    final Request theRequest =  request;
		    final JsContext[] holder = new JsContext[1];
		    final List<EntryWrapper> wrappers = new ArrayList<EntryWrapper>();
		    final int[]cnt={0};
                    EntryVisitor walker = new EntryVisitor(request,
							   getRepository(), actionId,
							   true) {
			    int errorCount = 0;
			    @Override
			    public boolean entryOk(Entry entry) {
				if(!super.entryOk(entry)) return false;
				if(not.contains(entry.getId())) return false;
				return true;
			    }
			    public boolean processEntry(Entry entry,
							List<Entry> children)
                                throws Exception {
				if(!thisOne && entry.getId().equals(finalEntry.getId())) {
				    return true;
				}

				//				Misc.sleepSeconds(1);				System.err.println("process:" + entry);
				if(anyFile) {
				    if(!entry.getResource().isFile()) {
					return true;
				    }					
				} else  if(Utils.stringDefined(type) && !entry.getTypeHandler().isType(type)) {
				    return true;
				}

				if(!getActionManager().getActionOk(actionId)) {
				    return false;
				}

				try {
				    cnt[0]++;
				    EntryWrapper wrapper = new EntryWrapper(request,getRepository(),holder[0],entry);
				    wrappers.add(wrapper);
				    scope.put("entry", scope, wrapper);
				    script.exec(ctx, scope);
				    if(!holder[0].okToRun) {
					return false;
				    }
				} catch(Exception exc) {
				    holder[0].cancel = true;
				    append("An error occurred processing entry:" + entry+" " + entry.getId()+"\n" + exc);
				    System.err.println("An error occurred processing entry:" + entry+" " + entry.getId()+"\n" + exc);
				    if(errorCount++>100) {
					append("Too many errors");
					System.err.println("Too many errors");
					return false;
				    }
				}
				return true;
			    }
			    public void finished() {
				super.finished();
				if(holder[0].cancel) return;
				if(forReal) {
				    boolean haveReset = false;
				    try {
					for(EntryWrapper wrapper:  wrappers) {
					    Entry entry = wrapper.entry;
					    boolean changed = wrapper.getChanged();
					    if(wrapper.name!=null) {
						changed = true;
						entry.setName(wrapper.name);
					    }
					    if(wrapper.description!=null) {
						changed = true;
						entry.setDescription(wrapper.description);
					    }					    
					    if(wrapper.url!=null) {
						changed = true;
						entry.getResource().setPath(wrapper.url);
					    }					    
					    if(wrapper.startDateChanged) {
						changed = true;
						entry.setStartDate(wrapper.startDate);
					    }
					    if(wrapper.endDateChanged) {
						changed = true;
						entry.setEndDate(wrapper.endDate);
					    }



					    if(changed) {
						getEntryManager().removeFromCache(entry);
						if(!haveReset) {
						    resetMessageBuffer();
						    haveReset = true;
						}
						append("Updated entry:" + entry+"\n");
						incrementProcessedCnt(1);
						getEntryManager().updateEntry(request, entry);
					    }
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
		    jsContext.print("* Done\nProcessed:#" +cnt[0]);
                    getActionManager().setContinueHtml(actionId,
						       walker.getMessageBuffer().toString());
                }
		};
	    actionId = getActionManager().runAction(action,"extendededitjs","",finalEntry);
	    what = new String[]{ARG_EXTEDIT_JS};
	    canCancel = true;
        } else if (request.exists(ARG_EXTEDIT_URL_CHANGE)) {
            getAuthManager().ensureAuthToken(request);
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

	    actionId = getActionManager().runAction(action,"","",finalEntry);
	    what = new String[]{ARG_EXTEDIT_URL_CHANGE};
        } else  if (request.exists(ARG_EXTEDIT_REPORT)) {
            getAuthManager().ensureAuthToken(request);
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

			    String resource = IO.getFileTail(child.getResource().getPath());
			    String name = getStorageManager().getOriginalFilename(resource);
			    append(HU.td(HU.div(name,HU.style("margin-left:10px;margin-right:10px;"))));
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
	    walker.walk(finalEntry);
	    suffix.append(HU.openInset(5, 30, 20, 0));
            suffix.append("<table><tr><td><b>" + msg("File") + "</b></td><td><b>"
                      + msg("Size") + "</td><td></td></tr>");
            suffix.append(walker.getMessageBuffer());
            suffix.append("<tr><td><b>" + msgLabel("Total")
                      + "</td><td></td><td align=right>"
                      + HU.b(formatFileLength(size[0]))
                      + "</td></tr>");
            suffix.append("</table>");
            suffix.append("**** - File managed by RAMADDA");
	    suffix.append(HU.closeInset());
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





        getPageHandler().entrySectionOpen(request, entry, sb, "Extended Edit");


	sb.append(prefix);
	if(actionId!=null) {
	    String url = getRepository().getUrlBase() +"/status?actionid=" + actionId +"&output=json";

	    sb.append(HU.b("Results"));
	    HU.div(sb,"",HU.attrs("class","ramadda-action-results", "id",actionId.toString()));
	    if(canCancel) {
		String cancelUrl = getRepository().getUrlBase() +"/status?actionid=" + actionId +"&" + ARG_CANCEL+"=true";
		sb.append(HU.button(HU.href(cancelUrl,LABEL_CANCEL)));
	    }
	    HU.script(sb,"Utils.handleActionResults('" + actionId +"','" + url+"',"+ canCancel+");\n");
	}



	Consumer<String> opener = label->{
	    sb.append(formHeader(HU.span(label,HU.style("font-size:120%;"))));
	    sb.append(HU.openInset(5, 30, 20, 0));
	};
        Utils.VarArgsConsumer<String> closer= (args) -> {
	    sb.append(formSuffix);
	    sb.append(HU.p());
	    for(int i=0;i<args.length;i+=2) {
		sb.append(HU.submit(args[i+1], args[i]));
		sb.append(HU.space(2));
	    }
	    sb.append(HU.closeInset());
	};


	List<HtmlUtils.Selector> tfos = getTypeHandlerSelectors(request,true, true, entry);
	tfos.add(0,new HtmlUtils.Selector("Select one","",""));

	for(String form: what) {
	    request.formPostWithAuthToken(sb, getRepository().URL_ENTRY_EXTEDIT,
					  HU.attr("name", "entryform"));
	    sb.append(HU.hidden(ARG_ENTRYID, entry.getId()));
	    if(form.equals(ARG_EXTEDIT_EDIT)) {
		opener.accept("Spatial and Temporal Metadata");
		sb.append(HU.labeledCheckbox(ARG_EXTEDIT_SPATIAL, "true",
					     request.get(ARG_EXTEDIT_SPATIAL,false), "Set spatial metadata"));
		sb.append(HU.br());
		sb.append(HU.labeledCheckbox(ARG_EXTEDIT_TEMPORAL, "true",
					     request.get(ARG_EXTEDIT_TEMPORAL,false), "Set temporal metadata"));
		sb.append(HU.br());
		sb.append(HU.labeledCheckbox(ARG_EXTEDIT_METADATA, "true",
					     request.get(ARG_EXTEDIT_METADATA,false), "Set other metadata"));		

		sb.append(HU.br());
		sb.append(HU.labeledCheckbox(ARG_EXTEDIT_THUMBNAIL, "true",
					     request.get(ARG_EXTEDIT_THUMBNAIL,false), "Add image thumbnails"));	
		sb.append(HU.br());
		sb.append(HU.labeledCheckbox(ARG_EXTEDIT_RECURSE, "true",
						    true, "Recurse"));
		closer.accept(form,"Set metadata");
	    } else if(form.equals(ARG_EXTEDIT_REINDEX)){
		opener.accept("Reindex");
		sb.append(HU.labeledCheckbox(ARG_EXTEDIT_REPORT_MISSING, "true",
					     true, "Show missing files")  + "<br>");
		closer.accept(form, "Reindex Search");
	    } else if(form.equals(ARG_EXTEDIT_REPORT)){
		opener.accept("File Listing");
		sb.append(HU.labeledCheckbox(ARG_EXTEDIT_REPORT_MISSING, "true",
					     request.get(ARG_EXTEDIT_REPORT_MISSING,false),"Show missing files")  + "<br>");
		sb.append(HU.labeledCheckbox(ARG_EXTEDIT_REPORT_FILES, "true", request.get(ARG_EXTEDIT_REPORT_FILES,false),"Show OK files") + "<p>");
		closer.accept(form, "Generate File Listing");
	    } else if(form.equals(ARG_EXTEDIT_ADDALIAS)){
		opener.accept("Add aliases to children entries");
		sb.append("Use the macro \"${name}\" in the template to create the alias<br>");
		sb.append(HU.b("Template") +": "+
			       HU.input(ARG_EXTEDIT_ADDALIAS_TEMPLATE,request.getString(ARG_EXTEDIT_ADDALIAS_TEMPLATE,"${name}"),
					HU.attr("size","40")) +" e.g., somealias_${name}");
		

		if(request.exists(ARG_EXTEDIT_ADDALIAS) || request.exists(ARG_EXTEDIT_ADDALIAS_CONFIRM)) 
		    closer.accept(ARG_EXTEDIT_ADDALIAS,"Test aliases",
				  ARG_EXTEDIT_ADDALIAS_CONFIRM,"Add aliases to selected entries");				

	
		else
		    closer.accept(ARG_EXTEDIT_ADDALIAS,"Test aliases");
	    }  else if(form.equals(ARG_EXTEDIT_CHANGETYPE)){
		opener.accept("Change Entry Type");
		sb.append(msgLabel("New type"));
		sb.append(HU.space(1));
		sb.append(HU.select(ARG_EXTEDIT_NEWTYPE, tfos, request.getString(ARG_EXTEDIT_NEWTYPE,"")));
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
		opener.accept("Change Descendants Entry Type");
		sb.append(HU.formTable());
		HU.formEntry(sb, msgLabel("Old type"),
			     HU.select(ARG_EXTEDIT_OLDTYPE,
				       tfos,request.getString(ARG_EXTEDIT_OLDTYPE,"")));

		HU.formEntry(sb, msgLabel("Regexp Pattern"),
			     HU.input(ARG_EXTEDIT_NEWTYPE_PATTERN, request.getString(ARG_EXTEDIT_NEWTYPE_PATTERN,"")) + " "
			     + msg("Only change type for entries that match this pattern"));

		HU.formEntry(sb, msgLabel("New type"),
			     HU.select(ARG_EXTEDIT_NEWTYPE,
				       tfos,request.getString(ARG_EXTEDIT_NEWTYPE,"")));
		HU.formEntry(sb, "",
			     HU.labeledCheckbox(ARG_EXTEDIT_CHANGETYPE_RECURSE_CONFIRM, "true",
						false, "Yes, change them"));
		sb.append(HU.formTableClose());
		closer.accept(form,"Change the type of all descendant entries");
	    }	else if(form.equals(ARG_EXTEDIT_URL_CHANGE)){		
		opener.accept("Change Descendants URL Path");
		sb.append(HU.formTable());
		HU.formEntry(sb, msgLabel("Pattern"),
			     HU.input(ARG_EXTEDIT_URL_PATTERN, request.getString(ARG_EXTEDIT_URL_PATTERN,"")));
		HU.formEntry(sb,msgLabel("To"),  HU.input(ARG_EXTEDIT_URL_TO, request.getString(ARG_EXTEDIT_URL_TO,"")));
		sb.append(HU.formTableClose());
		closer.accept(form,"Change URLs");
	    } else if(form.equals(ARG_EXTEDIT_JS)){
		opener.accept("Process with Javascript");
		closer.accept(form,"Apply Javascript");
		sb.append(HU.formTable());
		HU.formEntry(sb, HU.b("Only apply to entries of type")+": "+
			     HU.select(ARG_EXTEDIT_TYPE, tfos,request.getString(ARG_EXTEDIT_TYPE,null))
			     + HU.space(1) +
			     HU.labeledCheckbox(ARG_EXTEDIT_THISONE, "true",
						request.get(ARG_EXTEDIT_THISONE,true), "Apply to this entry"));


		HU.formEntry(sb,HU.labeledCheckbox(ARG_EXTEDIT_JS_CONFIRM, "true",
						   request.get(ARG_EXTEDIT_JS_CONFIRM,false), "Save changes to entries"));


		String eg =   "entry.getName() entry.setName()\n" +
		    "entry.getType()\n"+
		    "entry.getDescription();  entry.setDescription(String)\n" +
		    "entry.getStartDate(); entry.getEndDate()\n" +
		    "entry.setStartDate(String); entry.setEndDate(String)\n" +
		    "entry.hasLocationDefined(); entry.setLocation(lat,lon);\n"+
		    "entry.getChildren();\n" +
		    "entry.setColumnValue(name,value);\n" +
		    "entry.isImage(); entry.resizeImage(400); entry.grayscaleImage();\n" +
		    "entry.makeThumbnail(deleteExisting:boolean);\n" +
		    "entry.getValue('column_name');\n" +
		    "entry.applyCommand('addthumbnail');\n" +
		    "//apply llm. true=>skip if there is a description\n" +
		    "//title,summary, etc are varargs\n" +
		    "entry.applyLLM(true,'title','summary','keywords','model:gpt4');\n" +
		    "//ctx is the context object\n" +
		    "ctx.print() prints output\n" +
		    "//stop processing but still apply any changes\n" +
		    "ctx.stop() \n"+
		    "//cancel processing and no changes will be applied\n"+
		    "ctx.cancel() \n";

		String ex =  (String) getSessionManager().getSessionProperty(request,"extedit");
		if(ex==null)
		    ex = "//Include any javascript here\n" +
			"ctx.print('Processing: ' + entry.getName());\n";
		ex = request.getString(ARG_EXTEDIT_SOURCE, ex);
		String exclude = "<br>"+HU.b("Exclude entries") +":<br>"+
		    HU.textArea(ARG_EXTEDIT_EXCLUDE, request.getString(ARG_EXTEDIT_EXCLUDE,""),5,40,HU.attr("placeholder","entry ids, one per line"));

		HU.formEntry(sb,  HU.b("Javascript:")+
			     HU.table(HU.rowTop(HU.cols(HU.textArea(ARG_EXTEDIT_SOURCE, ex,10,60) +
							exclude,
						     HU.pre(eg)))));
		
		sb.append(HU.formTableClose());


		sb.append("<br>");
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



        List<TypeHandler> typeHandlers = getRepository().getTypeHandlersForDisplay(true);

        for (TypeHandler typeHandler : typeHandlers) {
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
				       HU.space(2) + typeHandler.getLabel(),
				       typeHandler.getType(), typeHandler.getTypeIconUrl(),20);
            //Add the seen ones first
            if (first.contains(typeHandler.getType())) {
                if (tfos.size() == 0) {
		    tfos.add(new HtmlUtils.Selector("Recent", "",
						    getRepository().getIconUrl("/icons/blank.gif"), 0, 0,
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
                    if(!getAccessManager().canDoEdit(request, entry)) {
                        continue;
                    }
                    md5 = ucar.unidata.util.IOUtil.getMd5(resource);
                    getDatabaseManager().update(Tables.ENTRIES.NAME,
                                                Tables.ENTRIES.COL_ID,
                                                id, new String[]{Tables.ENTRIES.COL_MD5},
                                                new String[]{md5});
                    sb.append(getPageHandler().getConfirmBreadCrumbs(request, entry));
                    sb.append(HU.br());
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

        Entry parent = getEntryManager().getEntryFromRequest(request, ARG_ENTRYID,
					   getRepository().URL_ENTRY_GET,true);
        String      fromIds = request.getString(ARG_FROM, "");
        List<Entry> entries = new ArrayList<Entry>();
        for (String id : Utils.split(fromIds, ",", true, true)) {
            Entry entry = getEntryManager().getEntry(request, id, false);
            if (entry == null) {
                throw new RepositoryUtil.MissingEntryException(
                    "Could not find entry:" + id);
            }
            if (entry.isTopEntry()) {
                StringBuilder sb = new StringBuilder();
                sb.append(
                    getPageHandler().showDialogNote(
                        msg("Cannot change top-level folder")));

                return new Result(msg("Entry Type Change"), sb);
            }
            entries.add(entry);
        }


        if (entries.size() == 0) {
	    return new Result("",getPageHandler().makeEntryPage(request, parent,"Entry Type Change",
								getPageHandler().showDialogError("No entries specified")));
        }

        if (request.exists(ARG_CANCEL)) {
            return new Result(
                request.entryUrl(
				 getRepository().URL_ENTRY_SHOW, parent!=null?parent:entries.get(0)));
        }


        StringBuffer sb = new StringBuffer();
        if (request.exists(ARG_CONFIRM)) {
            TypeHandler newTypeHandler = getRepository().getTypeHandler(
                                             request.getString(
                                                 ARG_EXTEDIT_NEWTYPE, ""));
            getAuthManager().ensureAuthToken(request);

            sb.append(msgLabel("The following entries have been changed"));
            sb.append("<ul>");
            for (Entry entry : entries) {
                if ( !getAccessManager().canDoEdit(request, entry)) {
                    throw new IllegalArgumentException(
                        "Whoa dude, you can't edit this entry:"
                        + entry.getName());
                }
                entry = changeType(request, entry, newTypeHandler);
                String icon = newTypeHandler.getIconProperty(null);
                if (icon != null) {
                    icon = newTypeHandler.getIconUrl(icon);
                }
                sb.append(HU.href(getEntryManager().getEntryURL(request, entry),
                                         HU.img(icon) + " "
                                         + entry.getName()));
                sb.append("<br>");
            }
            sb.append("</ul>");
            return new Result("", getPageHandler().makeEntryPage(request, parent,"Entry Type Change",sb.toString()));
        }



        List<HtmlUtils.Selector> tfos = getTypeHandlerSelectors(request,
                                            true, true, null);

        request.formPostWithAuthToken(sb,
                                      getRepository().URL_ENTRY_TYPECHANGE);
        sb.append(HU.hidden(ARG_FROM, fromIds));
	if(parent!=null)
	    sb.append(HU.hidden(ARG_ENTRYID, parent.getId()));
        sb.append(HU.p());
	
        StringBuffer inner = new StringBuffer();
        inner.append(msg("Are you sure you want to change the entry types?"));
        inner.append(HU.p());
        inner.append(HU.formTable());
        inner.append(
            HU.formEntry(
                msgLabel("New type"),
                HU.select(ARG_EXTEDIT_NEWTYPE, tfos,request.getString(ARG_EXTEDIT_NEWTYPE,""))));

        HU.formTableClose(inner);

        sb.append(
            getPageHandler().showDialogQuestion(
                inner.toString(),
                HU.buttons(
                    HU.submit(
                        msg("Yes, change the entry types"),
                        ARG_CONFIRM), HU.submit(
                            msg(LABEL_CANCEL), ARG_CANCEL))));
        sb.append(HU.formClose());
        sb.append("<table>");
        sb.append("<tr><td><b>Entry</b></td><td><b>Type</b></td></tr>");
        for (Entry entry : entries) {
            sb.append("<tr><td>");
            sb.append(HU.img(entry.getTypeHandler().getTypeIconUrl()));
            sb.append(" ");
            sb.append(entry.getName());
            sb.append("</td><td>");
            sb.append(entry.getTypeHandler().getLabel());
            sb.append("</td></tr>");
        }
        sb.append("</table>");

	return new Result("", getPageHandler().makeEntryPage(request, parent,"Entry Type Change",sb.toString()));
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
        if ( !getAccessManager().canDoEdit(request, entry)) {
            throw new AccessException("Cannot edit:" + entry.getLabel(),
                                      request);
        }
        getEntryManager().addSessionType(request, newTypeHandler.getType());

	String extraDesc =   entry.getTypeHandler().getExtraText(entry);
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

	if(extraDesc!=null) {
	    //This case shows up when converting a wiki page to something else
	    //The wiki page has its main text in another field, not the description
	    entry.setDescription(entry.getDescription()+extraDesc);
            getEntryManager().updateEntry(request, entry);
	}
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
        Rectangle2D.Double rect = getEntryUtil().getBounds(request,getEntryManager().getChildren(request, entry));
        if (rect != null) {
            entry.setBounds(rect);
            getEntryManager().updateEntry(request, entry);
        }
    }


    public static class EntryWrapper {
	private Entry entry;
	private Request request;
	private Repository repository;
	boolean changed = false;
	String name;
	String description;
	boolean startDateChanged=false;
	boolean endDateChanged=false;	
	Date startDate;
	Date endDate;
	String url;
	List<EntryWrapper> children;
	JsContext ctx;


	public EntryWrapper(Request request, Repository repository, JsContext ctx, Entry entry) {
	    this.repository = repository;
	    this.request= request;
	    this.entry = entry;
	    this.ctx=ctx;
	}

	public List<EntryWrapper> getChildren() throws Exception {
	    if(children==null) {
		children  = new ArrayList<EntryWrapper>();
		for(Entry e:repository.getEntryManager().getChildren(request, entry)) {
		    children.add(new EntryWrapper(request, repository, ctx,entry));
		}
	    }
	    return children;
	}

	public boolean isImage() {
	    return  entry.isImage();
	}

	public long getFileSize() {
	    if(!entry.isFile()) return -1;
	    return entry.getResource().getTheFile().length();
	}

	public void makeThumbnail(boolean deleteExisting) throws Exception {
	    if(entry.getTypeHandler().addThumbnail(request,entry,deleteExisting)) {
		changed=true;
		ctx.print("Thumnbail added:" + entry.getName());
	    }
	}

	public void grayscaleImage() throws Exception {
	    if(!isImage()) throw new IllegalArgumentException("Not an image:" + entry.getName());
	    String theFile = entry.getResource().getPath();
	    Image image = ImageUtils.readImage(theFile);
	    Image gimage = ImageUtils.grayscaleImage(image);
	    ImageUtils.writeImageToFile(gimage, theFile);
	    repository.getEntryManager().entryFileChanged(request, entry);
	    ctx.print("Image changed:" + entry.getName());
	}




	public void resizeImage(int width) throws Exception {
	    if(!isImage()) throw new IllegalArgumentException("Not an image:" + entry.getName());
	    String theFile = entry.getResource().getPath();
	    long size = getFileSize();
	    Image image = ImageUtils.readImage(theFile);
	    if(image.getWidth(null)<width) {
		ctx.print("Skipping image resize:" + entry.getName() +" width:" + 
			  image.getWidth(null));
		return;
	    }


	    image = ImageUtils.resize(image, width, -1);
	    ImageUtils.waitOnImage(image);
	    ImageUtils.writeImageToFile(image, theFile);
	    repository.getEntryManager().entryFileChanged(request, entry);
	    ctx.print("Image resize:" + entry.getName() +" orig size:"+
		      size +" new size:" +  getFileSize());
	}

	public Object getValue(String col) {
	    return entry.getValue(col);
	}

	public String getType() {
	    return entry.getTypeHandler().getType();
	}

	public void reindex() {
	    List<Entry> entries = new ArrayList<Entry>();
	    entries.add(entry);
	    repository.getSearchManager().entriesModified(request, entries);
	}


	public void applyLLM(boolean ifDescEmpty,String...args)  throws Exception {
	    if(ifDescEmpty && Utils.stringDefined(entry.getDescription())) {
		ctx.print("Has description:" + getName());
		return;
	    }
	    repository.getLLMManager().processArgs(request,args);
	    ctx.print("applying llm:" + getName());
	    changed = repository.getLLMManager().applyLLMToEntry(request,entry, new StringBuilder());
	    if(changed) {
		this.description = entry.getDescription();
	    }
	}

	public void applyCommand(String command,String...args)  throws Exception {
	    changed = entry.getTypeHandler().applyEditCommand(request,entry, command,args);
	    if(changed) {
		ctx.print("command: " + command +" applied to:" + getName());
	    }
	}


	public boolean getChanged() {
	    return changed;
	}

	public String getName() {
	    return entry.getName();
	}

	public String getId() {
	    return entry.getId();
	}

	public boolean hasLocationDefined() {
	    return entry.hasLocationDefined();
	}

	public void setLocation(double lat,double lon) {
	    entry.setLocation(lat,lon);
	}	


	public void setName(String name) {
	    this.name = name;
	}	

	public String getDescription() {
	    return entry.getDescription();
	}


	public String getCorpus() throws Exception {
	    String corpus =  entry.getTypeHandler().getDescriptionCorpus(entry);
	    return corpus;
	}
	
	public String getFullCorpus() throws Exception {
	    StringBuilder sb = new StringBuilder();
	    entry.getTypeHandler().getTextCorpus(entry, sb, true,true);
	    String corpus =  sb.toString();
	    return corpus;
	}

	public void setDescription(String description) {
	    this.description = description;
	}	

	public void setColumnValue(String key, Object value) {
	    entry.setValue(key, value);
	    changed=true;
	}	

	public String getFile() {
	    return entry.getResource().getPath();
	}

	public String getUrl() {
	    return entry.getResource().getPath();
	}	

	public void setUrl(String url) {
	    this.url = url;
	}


	public Date getStartDate() {
	    return new Date(entry.getStartDate());
	}

	public void setStartDate(String date) throws Exception {
	    if(date==null) this.startDate = null;
	    else 	    this.startDate = Utils.parseDate(date);
	    startDateChanged = true;
	}	


	public Date getEndDate() {
	    return new Date(entry.getEndDate());
	}

	public void setEndDate(String date) throws Exception {
	    if(date==null) this.endDate = null;
	    else this.endDate = Utils.parseDate(date);
	    endDateChanged = true;
	}	
	
	public String toString() {
	    return getName();
	}
	
    }
    
	    

	    
	

    public static class JsContext {
	private StringBuilder msg = new StringBuilder();
	private List<EntryWrapper> changedEntries = new ArrayList<EntryWrapper>();	
	private EntryVisitor visitor;
	private boolean confirm;
	private boolean okToRun = true;
	private boolean cancel = false;	
	private int count=0;

	public JsContext(EntryVisitor visitor, boolean confirm) {
	    this.visitor= visitor;
	    this.confirm = confirm;
	}

	public void stop() {
	    okToRun = false;
	}

	public void cancel() {
	    okToRun = false;
	    cancel = true;
	}	

	public Date getDate(String d) throws Exception {
	    return Utils.parseDate(d);
	}
	public void print(Object msg) {
	    visitor.append(msg+"\n");
	}
	


	public int log(int cnt,Object msg) {
	    if((count++%cnt)==0) {
		String s = msg.toString();
		s = s.replace("${count}",""+count);
		System.err.println("#"  +count+" " +s);
		visitor.append(s+"\n");
	    }
	    return count;
	}	
    }


}

