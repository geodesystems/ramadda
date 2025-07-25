/**
   Copyright (c) 2008-2025 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository;

import org.ramadda.repository.auth.AccessException;
import org.ramadda.repository.auth.User;
import org.ramadda.repository.database.DatabaseManager;
import org.ramadda.repository.database.Tables;

import org.ramadda.repository.monitor.*;
import org.ramadda.repository.metadata.*;

import org.ramadda.repository.metadata.Metadata;
import org.ramadda.repository.output.OutputHandler;
import org.ramadda.repository.output.OutputType;
import org.ramadda.repository.type.Column;
import org.ramadda.repository.type.DataTypes;
import org.ramadda.repository.type.TypeHandler;
import org.ramadda.repository.util.SelectInfo;

import org.ramadda.util.seesv.Seesv;
import org.ramadda.util.CategoryBuffer;
import org.ramadda.util.CategoryList;
import org.ramadda.util.FormInfo;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.NamedBuffer;
import org.ramadda.util.IO;
import org.ramadda.util.Utils;

import org.ramadda.util.ImageUtils;
import java.awt.Image;

import org.json.*;

import ucar.unidata.util.LogUtil;
import ucar.unidata.xml.XmlUtil;
import ucar.unidata.util.IOUtil;
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

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

/**
 * This class handles the extended entry edit functions
 */
@SuppressWarnings("unchecked")
public class ExtEditor extends RepositoryManager {

    public final RequestUrl URL_EXTEDIT_ENTRIES = new RequestUrl(this, "/entry/exteditentries");
    public static final String SESSION_ENTRIES = "entries";
    public static final String SESSION_TYPES = "types";
    public static final String ARG_EXTEDIT_EDIT = "extedit_edit";
    public static final String ARG_EXTEDIT_REINDEX = "extedit_reindex";
    public static final String ARG_EXTEDIT_EXCLUDE = "excludeentries";
    public static final String ARG_EXTEDIT_THUMBNAIL= "extedit_thumbnail";    
    public static final String ARG_EXTEDIT_TYPE= "extedit_type";
    public static final String ARG_EXTEDIT_APPLYMONITOR= "extedit_applymonitor";
    public static final String ARG_EXTEDIT_MONITOR= "extedit_monitor";    
    public static final String ARG_EXTEDIT_THISONE= "extedit_thisone";
    public static final String ARG_EXTEDIT_CHILDREN= "extedit_children";        
    public static final String ARG_EXTEDIT_URL_TO = "extedit_url_to";
    public static final String ARG_EXTEDIT_URL_PATTERN =   "extedit_url_pattern";
    public static final String ARG_EXTEDIT_URL_CHANGE = "extedit_url";
    public static final String ARG_EXTEDIT_JS = "extedit_js";
    public static final String ARG_EXTEDIT_JS_CONFIRM =    "extedit_js_confirm";
    public static final String ARG_EXTEDIT_SOURCE = "extedit_source";    
    public static final String ARG_EXTEDIT_SPATIAL = "extedit_spatial";
    public static final String ARG_EXTEDIT_TEMPORAL = "extedit_temporal";
    public static final String ARG_EXTEDIT_METADATA = "extedit_metadata";    
    public static final String ARG_EXTEDIT_MD5 = "extedit_md5";
    public static final String ARG_EXTEDIT_REPORT = "extedit_report";
    public static final String ARG_EXTEDIT_REPORT_MISSING =  "extedit_report_missing";
    public static final String ARG_EXTEDIT_REPORT_FILES =  "extedit_report_files";
    public static final String ARG_EXTEDIT_REPORT_EXTERNAL =   "extedit_report_external";
    public static final String ARG_EXTEDIT_REPORT_INTERNAL =  "extedit_report_internal";
    public static final String ARG_EXTEDIT_SETPARENTID = "extedit_setparentid";
    public static final String ARG_EXTEDIT_NEWTYPE = "extedit_newtype";
    public static final String ARG_EXTEDIT_NEWTYPE_PATTERN = "extedit_newtype_pattern";
    public static final String ARG_EXTEDIT_OLDTYPE = "extedit_oldtype";
    public static final String ARG_EXTEDIT_RECURSE = "extedit_recurse";
    public static final String ARG_EXTEDIT_CHANGETYPE = "extedit_changetype";
    public static final String ARG_EXTEDIT_CHANGETYPE_RECURSE = "extedit_changetype_recurse";
    public static final String ARG_EXTEDIT_CHANGETYPE_RECURSE_CONFIRM = "extedit_changetype_recurse_confirm";
    public static final String ARG_EXTEDIT_ADDALIAS = "extedit_addalias";
    public static final String ARG_EXTEDIT_ADDALIAS_NOTFIRST = "extedit_addalias_notfirst";    
    public static final String ARG_EXTEDIT_ADDALIAS_CONFIRM = "extedit_addalias_confirm";
    public static final String ARG_EXTEDIT_ADDALIAS_TEMPLATE = "extedit_addalias_template";        

    public ExtEditor(Repository repository) {
        super(repository);
    }

    public Result processEntryExtEditEntries(Request request) throws Exception {
	List<Entry> entries = new ArrayList<Entry>();
	for(Object id: request.get(ARG_ENTRYID, new ArrayList<String>())) {
	    Entry entry = getEntryManager().getEntry(request, id.toString());
	    if(entry!=null) entries.add(entry);
	}
	Entry group = getRepository().getEntryManager().getDummyGroup();
	return processEntryExtEdit(request, group,entries);
    }

    public Result processEntryExtEdit(final Request request)
	throws Exception {
        Entry              entry        = getEntryManager().getEntry(request);
	return processEntryExtEdit(request, entry,null);

    }

    private void visit(Entry entry,List<Entry> extEntries, EntryVisitor visitor) throws Exception {
	if(extEntries!=null)
	    visitor.walk(extEntries);
	else
	    visitor.walk(entry);
    }

    public Result processEntryExtEdit(final Request request,Entry entry,final List<Entry> extEntries)
	throws Exception {


	final boolean dummy = entry.isDummy();
	String[] what = new String[]{
	    ARG_EXTEDIT_EDIT,
	    //	    ARG_EXTEDIT_REINDEX,
	    ARG_EXTEDIT_CHANGETYPE,
	    ARG_EXTEDIT_CHANGETYPE_RECURSE,
	    ARG_EXTEDIT_APPLYMONITOR,
	    ARG_EXTEDIT_JS,
	    ARG_EXTEDIT_URL_CHANGE,
	    ARG_EXTEDIT_ADDALIAS,	    
	    ARG_EXTEDIT_REPORT
	};

	StringBuilder _extraFormArgs=new StringBuilder();
	if(dummy) {
	    what = new String[]{
		ARG_EXTEDIT_EDIT,
		ARG_EXTEDIT_CHANGETYPE_RECURSE,
		ARG_EXTEDIT_JS,
		ARG_EXTEDIT_URL_CHANGE,
		ARG_EXTEDIT_ADDALIAS};
	    if(extEntries!=null) {
		for(Entry child: extEntries) {
		    _extraFormArgs.append(HU.hidden(ARG_ENTRYID, child.getId()));
		}
	    }
	}
	final String extraFormArgs=_extraFormArgs.toString();
        final StringBuilder sb = new StringBuilder();
        final StringBuilder prefix = new StringBuilder();
        final StringBuilder suffix = new StringBuilder();
        final StringBuilder formSuffix = new StringBuilder();
	String cancelAction = request.getString(ARG_CANCEL,null);
	if(cancelAction!=null) {
	    request.put(ARG_ACTION_ID,cancelAction);
	    getActionManager().processStatus(request);
	    prefix.append(getPageHandler().showDialogNote("Action cancelled"));
	}




	Object actionId=null;
	boolean canCancel = false;

	boolean dfltRecurse = false;
	if (request.exists(ARG_EXTEDIT_JS)) {
	    dfltRecurse = true;
	}
        final Entry        finalEntry   = entry;
        final boolean      recurse = request.getCheckboxValue(ARG_EXTEDIT_RECURSE, dfltRecurse);
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
				@Override
				public boolean processEntry(Entry entry,  List<Entry> children)
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
			visit(finalEntry,extEntries, walker);
			getActionManager().setContinueHtml(actionId,
							   walker.getMessageBuffer().toString());
		    }
		};

	    actionId = getActionManager().runAction(action,"Change Metadata","",finalEntry);
	    what = new String[]{ARG_EXTEDIT_EDIT};
	    //            return getActionManager().doAction(request, action, "Walking the tree", "", entry);
	} else  if (request.exists(ARG_EXTEDIT_APPLYMONITOR)) {
	    List<EntryMonitor> monitors = getRepository().getMonitorManager().getEntryMonitors(false);
	    String id = request.getString(ARG_EXTEDIT_MONITOR,"");
	    EntryMonitor monitor = null;
	    for(EntryMonitor m: monitors) {
		if(m.getId().equals(id)) {
		    monitor=m;
		    break;
		}
	    }
	    if(monitor==null) {
		prefix.append(getPageHandler().showDialogError("Could not find selected monitor"));
	    } else {
		final EntryMonitor theMonitor = monitor;
		final List<Entry> children = new ArrayList<Entry>();
		for(Entry child:getEntryManager().getChildren(request, entry,new SelectInfo(request, 5000))) {
		    //true->force
		    if(theMonitor.checkEntry(child,false,true)) {
			children.add(child);
		    }
		}
		if(children.size()==0) {
		    prefix.append(getPageHandler().showDialogError("No entries passed the monitor filter"));
		} else {
		    prefix.append(getPageHandler().showDialogNote(msg(children.size()+" entries have been sent to the monitors. Check the Admin-&gt;Logs-&gt;Monitors")));
		    canCancel = true;
		    ActionManager.Action action = new ActionManager.Action() {
			    public void run(final Object actionId) throws Exception {
				try {
				    theMonitor.applyActions(request,children,false,actionId);
				} catch(Exception exc) {
				    getLogManager().logMonitorError("Error processing monitor:" +theMonitor,exc);
				}
			    }};

		    String link = HU.href(request.entryUrl(getRepository().URL_ENTRY_EXTEDIT,entry),entry.getName()+ " extended edit");
		    actionId = getActionManager().runAction(action,"Process Monitor",link);
		}
	    }
	    what = new String[]{ARG_EXTEDIT_APPLYMONITOR};
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
            prefix.append(getPageHandler().showDialogNote(msg("Entry type has been changed")));
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
	    List<Entry> children = extEntries!=null?extEntries:getEntryManager().getChildren(request, entry);
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
										  getMetadataManager().findType(ContentMetadataHandler.TYPE_ALIAS),
										  false,
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
				@Override
				public boolean processEntry(Entry entry, List<Entry> children)
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
			visit(finalEntry,extEntries, walker);
			getActionManager().setContinueHtml(actionId,
							   walker.getMessageBuffer().toString());
		    }
		};

	    actionId = getActionManager().runAction(action,"Change Type","",finalEntry);
	    what = new String[]{ARG_EXTEDIT_CHANGETYPE_RECURSE};
        } else if (request.exists(ARG_EXTEDIT_JS)) {
            getAuthManager().ensureAuthToken(request);
            final boolean forReal =  request.getCheckboxValue(ARG_EXTEDIT_JS_CONFIRM, false);
	    String javascript = request.getString(ARG_EXTEDIT_SOURCE,"");
	    final String js = "function extEditEvaluate() {\n"+javascript+"\n}\nextEditEvaluate();\n";
	    getSessionManager().putSessionProperty(request,"extedit",javascript);

            final String type = request.getString(ARG_EXTEDIT_TYPE, (String) null);
	    final boolean doThisOne = request.getCheckboxValue(ARG_EXTEDIT_THISONE,false);
	    final boolean doChildren = request.getCheckboxValue(ARG_EXTEDIT_CHILDREN,false);	    
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
			int depth=0;
			boolean recurseDown = true;
			if(recurse) {
			    depth = -1;
			}  else if(doChildren) {
			    depth = 1;
			} else if(doThisOne) {
			    recurseDown = false;
			    depth=1;
			}

			EntryVisitor walker = new EntryVisitor(request,
							       getRepository(), actionId,
							       recurseDown,depth) {
				int errorCount = 0;
				@Override
				public boolean entryOk(Entry entry) {
				    if(!super.entryOk(entry)) return false;
				    if(not.contains(entry.getId())) return false;
				    return true;
				}
				@Override
				public boolean processEntry(Entry entry, List<Entry> children)
				    throws Exception {
				    if(!doThisOne && entry.getId().equals(finalEntry.getId())) {
					return true;
				    }

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
					EntryWrapper wrapper = new EntryWrapper(request,getRepository(),holder[0],entry,forReal);
					wrappers.add(wrapper);
					scope.put("entry", scope, wrapper);
					script.exec(ctx, scope);
					if(!holder[0].okToRun) {
					    return false;
					}
				    } catch(Exception exc) {
					holder[0].cancel = true;
					append("<div class=ramadda-action-result-error>An error occurred processing entry:" + entry+" " + entry.getId()+"\n" + exc+"</div>");
					System.err.println("An error occurred processing entry:" + entry+" " + entry.getId()+"\n" + exc);
					//Always exit on the first error
					if(true) return false;
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
					    holder[0].entryProcessed(wrappers);
					    for(EntryWrapper wrapper:  holder[0].processedEntries) {
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
							haveReset = true;
						    }
						    append("Updated entry: " + holder[0].getString(entry)+"\n");
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
			scope.put("topLat", scope, new Double(finalEntry.getLatitude(request)));
			scope.put("topLon", scope, new Double(finalEntry.getLongitude(request)));
			visit(finalEntry,extEntries, walker);
			jsContext.print("Done - Processed: #" +cnt[0] +" entries");
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
				@Override
				public boolean processEntry(Entry entry, List<Entry> children)
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
			visit(finalEntry,extEntries, walker);
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
				    append(formatFileLength(file.length()));
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
	    visit(finalEntry,extEntries, walker);
	    suffix.append(HU.openInset(5, 30, 20, 0));
            suffix.append("<table><tr><td><b>" + msg("Entry") + "</b></td><td><b>" + msg("File")+"</b></td><td><b>"
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

	String help = HU.href(getRepository().getUrlBase()+
			      "/userguide/extendededit.html", "Help",
			      HU.attrs("target","_help","class","ramadda-clickable"));
	String extEditUrl = request.entryUrl(getRepository().URL_ENTRY_EXTEDIT,entry);
	String link = HU.href(extEditUrl,"Extended Edit");
        getPageHandler().entrySectionOpen(request, entry, sb, link+" - " + help);

	sb.append(prefix);
	if(actionId!=null) {
	    String url = getRepository().getUrlBase() +"/status?actionid=" + actionId +"&output=json";
	    sb.append(HU.b("Results"));
	    HU.div(sb,"",HU.attrs("class","ramadda-action-results", "id",actionId.toString()));
	    if(canCancel) {
		//		String cancelUrl = getRepository().getUrlBase() +"/status?actionid=" + actionId +"&" + ARG_CANCEL+"=true";
		String cancelUrl = HU.url(extEditUrl,ARG_CANCEL,actionId.toString());
		sb.append(HU.button(HU.href(cancelUrl,LABEL_CANCEL)));
	    }
	    HU.script(sb,"Utils.handleActionResults('" + actionId +"','" + url+"',"+ canCancel+");\n");
	}

	final StringBuilder[] buff={null};
	List<String> titles=  new ArrayList<String>();
	List<StringBuilder> contents=  new ArrayList<StringBuilder>();	

	Consumer<String> opener = label->{
	    titles.add(label);
	    buff[0] = new StringBuilder();
	    contents.add(buff[0]);
	    if(dummy) {
		request.formPostWithAuthToken(buff[0], URL_EXTEDIT_ENTRIES,HU.attr("name", "entryform"));
	    } else {
		request.formPostWithAuthToken(buff[0], getRepository().URL_ENTRY_EXTEDIT,HU.attr("name", "entryform"));
	    }

	    buff[0].append(HU.hidden(ARG_ENTRYID, finalEntry.getId()));	    
	    buff[0].append(extraFormArgs);

	    buff[0].append(HU.openInset(5, 30, 20, 0));
	};
        Utils.VarArgsConsumer<String> closer= (args) -> {
	    buff[0].append(formSuffix);
	    buff[0].append(HU.p());
	    for(int i=0;i<args.length;i+=2) {
		buff[0].append(HU.submit(args[i+1], args[i]));
		buff[0].append(HU.space(2));
	    }
	    buff[0].append(HU.closeInset());
	};

	List<HtmlUtils.Selector> tfos = getTypeHandlerSelectors(request,true, true, entry);
	tfos.add(0,new HtmlUtils.Selector("Select one","",""));

	String popupArgs = "{label:'Select entry type',makeButtons:false,after:true,single:true}";

	for(String form: what) {
	    if(form.equals(ARG_EXTEDIT_EDIT)) {
		opener.accept("Spatial and Temporal Metadata");
		buff[0].append(HU.labeledCheckbox(ARG_EXTEDIT_SPATIAL, "true",
						  request.get(ARG_EXTEDIT_SPATIAL,false), "Set spatial metadata"));
		buff[0].append(HU.br());
		buff[0].append(HU.labeledCheckbox(ARG_EXTEDIT_TEMPORAL, "true",
						  request.get(ARG_EXTEDIT_TEMPORAL,false), "Set temporal metadata"));
		buff[0].append(HU.br());
		buff[0].append(HU.labeledCheckbox(ARG_EXTEDIT_METADATA, "true",
						  request.get(ARG_EXTEDIT_METADATA,false), "Set other metadata"));		

		buff[0].append(HU.br());
		buff[0].append(HU.labeledCheckbox(ARG_EXTEDIT_THUMBNAIL, "true",
						  request.get(ARG_EXTEDIT_THUMBNAIL,false), "Add image thumbnails"));	
		buff[0].append(HU.br());
		buff[0].append(HU.labeledCheckbox(ARG_EXTEDIT_RECURSE, "true",  true, "Recurse"));
		closer.accept(form,"Set metadata");
	    } else if(form.equals(ARG_EXTEDIT_REINDEX)){
		opener.accept("Reindex");
		buff[0].append(HU.labeledCheckbox(ARG_EXTEDIT_REPORT_MISSING, "true",
						  true, "Show missing files")  + "<br>");
		closer.accept(form, "Reindex Search");
	    } else if(form.equals(ARG_EXTEDIT_REPORT)){
		opener.accept("File Listing");
		buff[0].append(HU.labeledCheckbox(ARG_EXTEDIT_REPORT_MISSING, "true",
						  request.get(ARG_EXTEDIT_REPORT_MISSING,false),"Show missing files")  + "<br>");
		buff[0].append(HU.labeledCheckbox(ARG_EXTEDIT_REPORT_FILES, "true", request.get(ARG_EXTEDIT_REPORT_FILES,false),"Show OK files") + "<p>");
		closer.accept(form, "Generate File Listing");
	    } else if(form.equals(ARG_EXTEDIT_ADDALIAS)){
		opener.accept("Add aliases to children entries");
		buff[0].append("Use the macro \"${name}\" in the template to create the alias<br>");
		buff[0].append(HU.b("Template") +": "+
			       HU.input(ARG_EXTEDIT_ADDALIAS_TEMPLATE,request.getString(ARG_EXTEDIT_ADDALIAS_TEMPLATE,"${name}"),
					HU.attr("size","40")) +" e.g., somealias_${name}");

		if(request.exists(ARG_EXTEDIT_ADDALIAS) || request.exists(ARG_EXTEDIT_ADDALIAS_CONFIRM)) 
		    closer.accept(ARG_EXTEDIT_ADDALIAS,"Test aliases",
				  ARG_EXTEDIT_ADDALIAS_CONFIRM,"Add aliases to selected entries");				

		else
		    closer.accept(ARG_EXTEDIT_ADDALIAS,"Test aliases");
	    }  else if(form.equals(ARG_EXTEDIT_CHANGETYPE)){
		opener.accept("Change Entry Type");
		buff[0].append(msgLabel("New type"));
		buff[0].append(HU.space(1));
		String id = HU.getUniqueId("select");
		buff[0].append(HU.select(ARG_EXTEDIT_NEWTYPE, tfos,
					 request.getString(ARG_EXTEDIT_NEWTYPE,""),
					 HU.attrs("id",id)));
		buff[0].append(HU.p());
		List<Column> columns = entry.getTypeHandler().getColumns();
		if ((columns != null) && (columns.size() > 0)) {
		    StringBuilder note = new StringBuilder();
		    for (Column col : columns) {
			if (note.length() > 0) {
			    note.append(", ");
			}
			note.append(col.getLabel());
		    }
		    buff[0].append(msgLabel("Note: this metadata would be lost") + note);
		}
		buff[0].append(HU.script(HU.call("HtmlUtils.makeSelectTagPopup",
						 HU.quote("#"+id),
						 popupArgs)));
		closer.accept(form, "Change type of this entry");
	    }  else if(form.equals(ARG_EXTEDIT_CHANGETYPE_RECURSE)){
		opener.accept("Change Descendants Entry Type");
		buff[0].append(HU.formTable());
		String id1 = HU.getUniqueId("select");
		String id2 = HU.getUniqueId("select");
		HU.formEntry(buff[0], msgLabel("Old type"),
			     HU.select(ARG_EXTEDIT_OLDTYPE,
				       tfos,request.getString(ARG_EXTEDIT_OLDTYPE,""),
				       HU.attrs("id",id1)));
		HU.formEntry(buff[0], msgLabel("Regexp Pattern"),
			     HU.input(ARG_EXTEDIT_NEWTYPE_PATTERN, request.getString(ARG_EXTEDIT_NEWTYPE_PATTERN,"")) + " "
			     + msg("Only change type for entries that match this pattern"));

		HU.formEntry(buff[0], msgLabel("New type"),
			     HU.select(ARG_EXTEDIT_NEWTYPE,
				       tfos,request.getString(ARG_EXTEDIT_NEWTYPE,""),
				       HU.attrs("id",id2)));
		buff[0].append(HU.script(HU.call("HtmlUtils.makeSelectTagPopup",
						 HU.quote("#"+id1),
						 popupArgs)));
		buff[0].append(HU.script(HU.call("HtmlUtils.makeSelectTagPopup",
						 HU.quote("#"+id2),
						 popupArgs)));		

		HU.formEntry(buff[0], "",
			     HU.labeledCheckbox(ARG_EXTEDIT_CHANGETYPE_RECURSE_CONFIRM, "true",
						false, "Yes, change them"));
		buff[0].append(HU.formTableClose());
		closer.accept(form,"Change the type of all descendant entries");
	    }	else if(form.equals(ARG_EXTEDIT_URL_CHANGE)){		
		opener.accept("Change Descendants URL Path");
		buff[0].append(HU.formTable());
		HU.formEntry(buff[0], msgLabel("Pattern"),
			     HU.input(ARG_EXTEDIT_URL_PATTERN, request.getString(ARG_EXTEDIT_URL_PATTERN,"")));
		HU.formEntry(buff[0],msgLabel("To"),  HU.input(ARG_EXTEDIT_URL_TO, request.getString(ARG_EXTEDIT_URL_TO,"")));
		buff[0].append(HU.formTableClose());
		closer.accept(form,"Change URLs");
	    } else if(form.equals(ARG_EXTEDIT_APPLYMONITOR)){
		List<EntryMonitor> monitors = getRepository().getMonitorManager().getEntryMonitors(false);
		if(monitors.size()>0) {
		    opener.accept("Send to monitors");
		    List<HtmlUtils.Selector> monitorOptions = new ArrayList<HtmlUtils.Selector>();
		    for(EntryMonitor monitor: monitors) {
			monitorOptions.add(new HtmlUtils.Selector(monitor.getName(),monitor.getId()));
		    }
		    //		buff[0].append(HU.submit("Send to monitors",form));
		    buff[0].append(HU.formTable());
		    HU.formEntry(buff[0],"Select Monitor:",
				 HU.select(ARG_EXTEDIT_MONITOR,monitorOptions,request.getString(ARG_EXTEDIT_MONITOR,"")));
		    buff[0].append(HU.formTableClose());		    
		    closer.accept(form,"Apply monitor");
		}
	    } else if(form.equals(ARG_EXTEDIT_JS)){
		opener.accept("Process with Javascript");
		String saveCbx = request.addCheckbox(buff[0],ARG_EXTEDIT_JS_CONFIRM,"Save changes to entries",true);		
		String space = HU.space(2);
		String helpLink = HU.href(getRepository().getUrlBase()+"/userguide/extendededitjs.html","Help", "target=_help");
		buff[0].append(HU.submit("Apply Javascript",form) +space + saveCbx + space + helpLink);
		buff[0].append(HU.formTable());
		String cbx = request.addCheckbox(buff[0],ARG_EXTEDIT_THISONE,"Apply to this entry",true);
		String cbx1 = request.addCheckbox(buff[0],ARG_EXTEDIT_CHILDREN,"Apply to children",true);		
		String cbx2 = request.addCheckbox(buff[0],ARG_EXTEDIT_RECURSE,"Apply to all descendents",true);
		String id = HU.getUniqueId("select");

		HU.formEntry(buff[0], HU.b("Only apply to entries of type")+": "+
			     HU.select(ARG_EXTEDIT_TYPE, tfos,request.getString(ARG_EXTEDIT_TYPE,null),
				       HU.attrs("id",id))+
			     "<br>" +cbx + space + cbx1 + space + cbx2);	
		buff[0].append(HU.script(HU.call("HtmlUtils.makeSelectTagPopup",
						 HU.quote("#"+id),
						 popupArgs)));		

		List<String> helpTitles= new ArrayList<String>();
		List<String> helpTabs = new ArrayList<String>();		
		String divOpen = "<div class=exteg style='min-width:500px;min-height:350px;'>";
		String basic =
		    divOpen +
		    "<span>entry.getName()</span>\n"+
		    "<span>entry.setName()</span>\n" +
		    "<span>entry.getType()</span>\n"+
		    "<span>entry.getDescription()</span>\n" +
		    "<span>entry.setDescription(String)</span>\n" +
		    "<span>entry.getStartDate()</span>\n"+
		    "<span>entry.getEndDate()</span>\n" +
		    "<span>entry.setStartDate('yyyy-MM-dd')</span>\n" +
		    "<span>entry.setEndDate('yyyy-MM-dd')</span>\n" +
		    "<span>entry.hasLocationDefined()</span>\n"+
		    "<span>entry.setLocation(lat,lon)</span>\n"+
		    "<span>entry.setOwner('username')</span>\n" +
		    "<span>entry.getChildren()</span>\n" +
		    "</div>";
		String metadataCommands =
		    divOpen +
		    "<span>entry.setColumnValue('name',value)</span>\n" +
		    "<span>entry.getColumnValue('column_name')</span>\n" +
		    "<span>entry.getMetadata('type','optional value1','opt v2')</span>\n" +
		    "<span>entry.addMetadata('type','value1','value 2')</span>\n" +
		    "<span data-copy-prefix='//metadataObject is either a string metadata type or use entry.getMetadata(...)\n'>entry.changeMetadata(metadataObject,'pattern','with')</span>\n" +		    		    
		    "<span data-copy-prefix='//metadataObject is either a string metadata type or use entry.getMetadata(...)\n'>entry.setMetadataPermission(metadataObject,\"admin\")</span>\n" +		    		    
		    "<span data-copy-prefix='//metadataObject is either a string metadata type or use entry.getMetadata(...)\n'>entry.deleteMetadata(metadataObject)</span>\n" +		    		    
		    "<span>entry.indexEntry()</span>\n" +

		    HU.href(getRepository().getUrlPath("/metadata/types.html"),"List metadata types",HU.attrs("target","_types")) +

		    "</div>";

		String image = 		    divOpen +
		    "<span>entry.isImage()</span>\n<span>entry.resizeImage(400 /*width*/)</span>\n<span>entry.grayscaleImage()</span>\n" +
		    "<span>entry.makeThumbnail(true /*true = delete existing*/)</span>\n" +
		    "</div>";

		String llm =    divOpen +
		    "//Set the LLM to be used for subsequent calls\n" +
		    "<span>entry.setLLM('one of gpt3.5 gpt4 gemini claude')</span>\n" +
		    "\n//apply llm. true=>skip if there is a description\n" +
		    "//title,summary, etc are varargs\n" +
		    "<span>entry.applyLLM(true,'title','summary','keywords','latlon','extract_date','include_date','authors')</span>\n" +
		    "\n//extract a metadata value using the LLM\n" +
		    "//for multiples ask the LLM to delimit the results with a semi-colon\n" +
		    "//true -&gt; *check if the entry has the metadata element already\n" +
		    "<span>entry.addLLMMetadata('metadata_type'  , 'prompt' , true )</span>\n" +
		    "//e.g.: entry.addLLMMetadata('tribe_name','Extract the tribe name from the document');\n" +
		    "\n//extract lat/lon\n"+
		    "<span>entry.addLLMGeo('optional prompt')</span>\n" +		    		    
		    "</div>\n";

		String control =   divOpen +
		    "//ctx is the context object\n" +
		    "//prints output:\n" +
		    "<span>ctx.print('message')</span>\n" +
		    "<span>ctx.warning('warning message')</span>\n" +
		    "<span>ctx.error('error message')</span>\n" +		    
		    "<span>ctx.pause(seconds)</span> \n"+
		    "//stop processing but still apply any changes\n" +
		    "<span>ctx.stop()</span> \n"+
		    "//cancel processing and no changes will be applied\n"+
		    "<span>ctx.cancel()</span></div>\n";

		helpTitles.add("Basic");
		helpTabs.add(HU.pre(basic));
		helpTitles.add("Metadata");
		helpTabs.add(HU.pre(metadataCommands));		
		helpTitles.add("Image");
		helpTabs.add(HU.pre(image));		
		helpTitles.add("LLM");
		helpTabs.add(HU.pre(llm));
		helpTitles.add("Control");
		helpTabs.add(HU.pre(control));				
		StringBuilder helpSB = new StringBuilder();
		HU.makeTabs(helpSB,helpTitles,helpTabs);
		helpSB.append(HU.script("Utils.initCopyable('.exteg span',{addNL:true,textArea:'" +ARG_EXTEDIT_SOURCE+"'});"));

		String ex =  (String) getSessionManager().getSessionProperty(request,"extedit");
		if(ex==null)
		    ex = "//Include any javascript here\n" +
			"ctx.print('Processing:', entry);\n";
		ex = request.getString(ARG_EXTEDIT_SOURCE, ex);
		String exclude = "<br>"+HU.b("Exclude entries") +":<br>"+
		    HU.textArea(ARG_EXTEDIT_EXCLUDE, request.getString(ARG_EXTEDIT_EXCLUDE,""),5,40,HU.attr("placeholder","entry ids, one per line"));

		HU.formEntry(buff[0],  HU.table(HU.rowTop(HU.cols(HU.textArea(ARG_EXTEDIT_SOURCE, ex,10,60,HU.attr("id",ARG_EXTEDIT_SOURCE)) +
								  exclude,
								  helpSB.toString()))));

		buff[0].append(HU.formTableClose());
		buff[0].append("<br>");
		closer.accept(form,"Apply Javascript");
	    }
	    buff[0].append(HU.formClose());
	}

	if(titles.size()==1) {
	    String label = titles.get(0);
	    sb.append(formHeader(HU.span(label,HU.style("font-size:120%;"))));
	    sb.append(contents.get(0));
	}else {
	    HU.makeAccordion(sb,titles,contents);
	}

	sb.append(suffix);

        return getEntryManager().makeEntryEditResult(request, entry, "Extended Edit", sb);
    }

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
	boolean confirmed;
	String name;
	String description;
	boolean startDateChanged=false;
	boolean endDateChanged=false;	
	Date startDate;
	Date endDate;
	String url;
	List<EntryWrapper> children;
	JsContext ctx;

	public EntryWrapper(Request request, Repository repository, JsContext ctx, Entry entry,boolean confirmed) {
	    this.repository = repository;
	    this.request= request;
	    this.entry = entry;
	    this.ctx=ctx;
	    this.confirmed = confirmed;
	}

	public List<EntryWrapper> getChildren(String...args) throws Exception {
	    List<Entry> entries = repository.getEntryManager().getChildren(request, entry);
	    boolean descending = true;
	    for(String arg: args) {
		if(arg.startsWith("type:")) {
		    String type = arg.substring("type:".length());
		    entries = repository.getEntryUtil().getEntriesOfType(entries, type);
		} else if(arg.startsWith("sort:")) {
		    String sort = arg.substring("sort:".length());
		    if(sort.endsWith("_descending")) {
			sort = sort.replace("_descending","");
			descending =true;
		    } else if(sort.endsWith("_ascending")) {
			sort = sort.replace("_ascending","");
			descending =false;
		    } else if(sort.endsWith("_up")) {
			sort = sort.replace("_up","");
			descending =false;
		    } else if(sort.endsWith("_down")) {
			sort = sort.replace("_down","");
			descending =true;
		    }
		    //		    System.err.println("sort:" + sort +" descending:" + descending);
		    entries = repository.getEntryUtil().sortEntriesOn(entries, sort,descending);
		}  else {
		    throw new IllegalArgumentException("Unknown sort argument:" + arg);
		}

	    }

	    List<EntryWrapper> children  = new ArrayList<EntryWrapper>();
	    for(Entry child:entries) {
		children.add(new EntryWrapper(request, repository, ctx,child,confirmed));
	    }
	    ctx.entryProcessed(children);
	    return children;
	}


	private List<Metadata> getMetadataList(Object obj) throws Exception {
	    List<Metadata> list=null;
	    if(obj instanceof String) {
		list = repository.getMetadataManager().findMetadata(request,  entry,(String)obj,false);
	    } else  if(obj instanceof Metadata) {
		list = new ArrayList<Metadata>();
		list.add((Metadata)obj);
	    } else {
		list=(List<Metadata>)obj;
	    }
	    return list;
	}


	@Override
	public int hashCode() {
	    return entry.hashCode();
	}
	@Override
	public boolean equals(Object o) {
	    if(o instanceof EntryWrapper) {
		EntryWrapper that = (EntryWrapper) o;
		return this.entry.equals(that.entry);
	    }
	    return false;
	}

	public void deleteMetadata(Object obj) throws Exception {
	    List<Metadata> list= getMetadataList(obj);
	    if(list.size()==0) return;
	    if(!confirmed) {
		int cnt = 1;
		String msg = list.size() + " metadata items would be deleted: " + ctx.getString(entry);
		for(Metadata m:list)
		    msg+=" #"+(cnt++)+" " + m.getAttr1();
		ctx.print(msg);
		return;
	    }	    
	    for(Metadata metadata: list) {
		ctx.print("Metadata deleted:" + ctx.getString(entry) +" " + metadata.getAttr1());
		repository.getMetadataManager().deleteMetadata(entry, metadata);
	    }
	    //	    changed=true;
	    //	    entry.setMetadataChanged(true);
	}

	public List<Metadata> getMetadata(String...values) throws Exception {
	    String type = values.length>0?values[0]:null;
	    List<Metadata> source =   repository.getMetadataManager().findMetadata(request,  entry,type,false);
	    System.err.println("type:"  +type +" mtd:" + source);

	    if(values.length<=1) return source;
	    List<Metadata> dest = new ArrayList<Metadata>();
	    for(Metadata metadata: source) {
		boolean ok = true;
		for(int i=1;i<values.length;i++) {
		    String v = metadata.getAttr(i);
		    if(!Misc.equals(v,values[i])) {
			ok = false;
			break;
		    }
		}
		if(ok) dest.add(metadata);
	    }
	    return dest;
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
		ctx.print("Thumbnail added:" + entry.getName());
	    } else {
		ctx.print("Thumbnail not added:" + entry.getName());
	    }
	}

	public void setOwner(String userID) throws Exception {
	    User user = repository.getUserManager().findUser(userID);
	    if(user==null) throw new IllegalArgumentException("Could not find user:" + userID);
	    entry.setUser(user);
	    changed= true;
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

	public Object getColumnValue(String col) {
	    return entry.getValue(request, col);
	}

	public String getType() {
	    return entry.getTypeHandler().getType();
	}

	public void reindex() {
	    List<Entry> entries = new ArrayList<Entry>();
	    entries.add(entry);
	    repository.getSearchManager().entriesModified(request, entries);
	}


	public boolean matchMetadata(String match,String value) throws Exception {
	    boolean regexp = StringUtil.containsRegExp(match);
	    if(regexp) {
		return value.matches(match);
	    }
	    String _match = match.toLowerCase();
	    return value.indexOf(_match)>0;
	}

	public void indexEntry() throws Exception {
	    List<Entry> entries = new ArrayList<Entry>();
	    entries.add(entry);
	    repository.getSearchManager().entriesModified(request,entries);
	}

	/*
	public void indexEntry() {
	    changed=true;
	    request.put(ARG_DOOCR,true);
	}
	*/



	public void addMetadata(String type,String...values) throws Exception {
	    repository.getMetadataManager().addMetadata(request,entry,  type, true,values);
	    ctx.print("Added metadata to:" + entry.getName());
	    changed=true;
	}



	public void changeMetadata(Object object,String from, String to) throws Exception {
	    List<Metadata> list =  getMetadataList(object);
	    if(list==null || list.size()==0) return;
	    for(Metadata mtd:list) {
		String v= mtd.getAttr1();
		String newV = v.replaceAll(from,to);
		if(!v.equals(newV)) {
		    changed = true;
		    entry.setMetadataChanged(true);
		    mtd.setAttr1(newV);
		    ctx.print("Entry:" + entry.getName() +" changed: " + v +" to: " + newV);
		}
	    }
	}

	public void setMetadataPermission(Object object,String to) throws Exception {
	    List<Metadata> list =  getMetadataList(object);
	    if(list==null || list.size()==0) return;
	    for(Metadata mtd:list) {
		if(!confirmed) {
		    int cnt = 1;
		    String msg =  ctx.getString(entry) + " would change " + list.size()+": ";
		    for(Metadata m:list)
			msg+=" #"+(cnt++)+" " + ctx.getString(m);
		    ctx.print(msg);
		    return;
		}	    

		changed = true;
		mtd.setAccess(to);
		entry.setMetadataChanged(true);
		ctx.print("Entry: " + ctx.getString(entry) +" changed permissions: "+ctx.getString(mtd));
	    }
	}
	
	public void addLLMMetadata(String type,String prompt,boolean...check)  throws Exception {
	    try {
		Column column = entry.getTypeHandler().getColumn(type);
		if(column == null && (check.length==0 || check[0])) {
		    List<Metadata> list = repository.getMetadataManager().findMetadata(request,  entry,type,false);

		    if(list!=null && list.size()>0) {
			ctx.print("Already has metadata:" + entry.getName());
			return;
		    }
		}

		String r = repository.getLLMManager().applyPromptToDocument(request,
									    entry,
									    true,
									    prompt,null);

		if(r!=null && column!=null) {
		    entry.setValue(column,r);
		    changed=true;
		}
		if(!Utils.stringDefined(r)) {
		    ctx.warning("No results for entry:" + entry.getName());
		} else {
		    ctx.print("Metadata added for entry:" + entry.getName() +"="  + r);
		    for(String tok:Utils.split(r,";",true,true)) {
			repository.getMetadataManager().addMetadata(request,entry,  type, true,tok);
		    }
		    changed=true;
		}
	    } catch(Exception exc) {
		repository.getLogManager().logError("Extended edit error:" + exc,exc);
		exc.printStackTrace();
		ctx.error("An error occurred processing:" + entry +" " + exc);
	    }
	}

	public void addLLMGeo(String prompt)  throws Exception {
	    if(entry.isGeoreferenced(request)) {
		ctx.print("Already has location:" + entry.getName());
		return;
	    }
	    if(!Utils.stringDefined(prompt)) {
		prompt="Give the latitude and longitude of the area that this document describes. Just give the 2 numbers, nothing else. Give it in the form <latitude>,<longitude></longitude></latitude>";
	    }
	    try {
		String r = repository.getLLMManager().applyPromptToDocument(request,
									    entry,
									    true,
									    prompt,null);
		if(!Utils.stringDefined(r)) {
		    ctx.warning("No results for entry:" + entry.getName());
		    return;
		} 
		List<String> toks = Utils.split(r,",",true,true);
		if(toks.size()!=2) {
		    ctx.print("Could not parse results for:" + entry.getName() +" r:" + r);
		    return;
		}
		double lat  =Double.parseDouble(toks.get(0));
		double lon  =Double.parseDouble(toks.get(1));		
		if(lat<-90 || lat>90 || lon<-180 || lon>180) {
		    ctx.print("invalid lat/lon for entry:" + entry.getName()+" lat/lon:" + lat +" " +lon);
		    return;
		}
		entry.setLatitude(lat);
		entry.setLongitude(lon);
		ctx.print("entry changed:" + entry.getName()+" lat/lon:" + lat +" " +lon);
		changed=true;
	    } catch(Exception exc) {
		repository.getLogManager().logError("Extended edit error:" + exc,exc);
		exc.printStackTrace();
		ctx.print("An error occurred processing:" + entry +" " + exc);
	    }
	}

	public void setLLM(String model) {
	    repository.getLLMManager().setModel(request,model);
	}

	public void applyLLM(boolean ifDescEmpty,String...args)  throws Exception {
	    if(ifDescEmpty && Utils.stringDefined(entry.getDescription())) {
		ctx.print("Entry has description:" + getName());
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

	public boolean isType(String s) {
	    return entry.getTypeHandler().isType(s);
	}



	public String getName() {
	    return entry.getName();
	}

	public String getId() {
	    return entry.getId();
	}

	public boolean hasLocationDefined() {
	    return entry.hasLocationDefined(request);
	}



	public void setLocation(double lat,double lon) {
	    entry.setLocation(lat,lon);
	    changed=true;
	}

	public void setAltiude(double alt) {
	    entry.setAltitude(alt);
	    changed=true;
	}		

	public void setLocationFromParent() {
	    Entry parent = entry.getParentEntry();
	    if(parent.hasLocationDefined(request)) {
		entry.setNorth(parent.getNorth(request));
		entry.setWest(parent.getWest(request));
		entry.setSouth(parent.getSouth(request));				
		entry.setEast(parent.getEast(request));		
		changed=true;
	    }
	}
	public void setName(String name) {
	    this.name = name;
	    changed=true;
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
	    changed=true;
	}	

	public void setColumnValue(String key, Object value) throws Exception {
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
	    changed=true;
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
	private HashSet<EntryWrapper> processedEntries = new HashSet<EntryWrapper>();
	private EntryVisitor visitor;
	private boolean confirm;
	private boolean okToRun = true;
	private boolean cancel = false;	
	private int count=0;

	public JsContext(EntryVisitor visitor, boolean confirm) {
	    this.visitor= visitor;
	    this.confirm = confirm;
	}

	public void entryProcessed(EntryWrapper wrapper) {
	    processedEntries.add(wrapper);
	}

	public void entryProcessed(List<EntryWrapper> wrappers) {
	    processedEntries.addAll(wrappers);
	}

	public void pause(int seconds) {
	    Misc.sleepSeconds(seconds);
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

	public String getString(Object msg) throws Exception {
	    Request request = visitor.getRequest();
	    if(msg==null) return "null";
	    if((msg instanceof EntryWrapper)) {
		msg =((EntryWrapper) msg).entry;
	    }
	    if((msg instanceof Entry)) {
		Entry e  = (Entry) msg;
		String tt = "View type definition: " + e.getTypeHandler().getLabel();
		String        entryIcon = HU.img(visitor.getRepository().getPageHandler().getIconUrl(request, e),
						 "",  HU.attrs("title",tt,
							       HU.ATTR_WIDTH,ICON_WIDTH));
		String typeUrl=HU.href(visitor.getRepository().getUrlBase()+"/entry/types.html?type="+e.getTypeHandler().getType(),
				       entryIcon,"target=_types");
		return typeUrl+HU.space(1) +
		    HU.href(visitor.getRepository().getEntryManager().getEntryURL(request, e),
			    e.getName(),
			    HU.attrs("target","_entry"));
	    }
	    if(msg instanceof Metadata) {
		Metadata mtd = (Metadata)msg;
		MetadataType mtdType = mtd.getMetadataType();
		List<MetadataElement> children = mtdType.getChildren();
		String s = mtd.getAttr1();
		if(children.size()>1) {
		    String v = mtd.getAttr2();
		    if(v.length()>30) v = v.substring(0,29)+"...";
		    s+=" - " +v; 
		}
		return  HU.b(mtdType.getName())+": "+ HU.italics(s);
	    }
	    if(!(msg instanceof List)) return msg.toString();
	    StringBuilder sb = new StringBuilder();
	    List l = (List) msg;
	    if(l.size()==0) sb.append("&lt;empty list&gt; ");
	    for(Object o: l) {
		sb.append(getString(o));
		sb.append(" ");
	    }
	    return sb.toString();
	}

	public void print(Object ...msgs) throws Exception {
	    for(Object msg: msgs)
		visitor.append(getString(msg)+" ");
	    visitor.append("\n");
	}

	public void warning(Object ...msgs) throws Exception {
	    StringBuilder sb = new StringBuilder();
	    for(Object msg: msgs)
		sb.append(HU.div(getString(msg)));
	    visitor.append(HU.div(sb.toString(),HU.cssClass("ramadda-action-result-warning")));
	}	

	public void error(Object ...msgs) throws Exception {
	    StringBuilder sb = new StringBuilder();
	    for(Object msg: msgs)
		sb.append(HU.div(getString(msg)));
	    visitor.append(HU.div(sb.toString(),HU.cssClass("ramadda-action-result-error")));
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

    public static final String ARG_TYPEID = "typeid";
    public static final String ARG_TYPENAME = "typename";    
    public static final String ARG_HANDLER = "handler";
    public static final String ARG_CATEGORY = "category";
    public static final String ARG_SUPERCATEGORY = "supercategory";    
    public static final String ARG_HANDLER_EXTRA = "handler_extra";    
    public static final String ARG_JSON_CONTENTS= "json_contents";
    public static final String ARG_FORIMPORT = "forimport";
    public static final String ARG_DROPTABLE = "droptable";
    public static final String ARG_INSTALL = "install";
    public static final String ARG_CREATE = "create";
    public static final String ARG_SAVE = "save";        
    public static final String ARG_COMMENT = "comment";

    public boolean createTypeOK(Request request) {
	return !request.isAnonymous();
    }

    /**
       this is the main entry point
    */
    public Result outputCreateType(Request request, Entry entry) throws Exception {
	if ( !createTypeOK(request)) {
	    throw new AccessException("Create type not enabled", request);
	}
	if(!getAccessManager().canDoEdit(request, entry)) {
	    throw new AccessException("You do not have access to Create Type for this entry", request);
	}

	if(request.exists(ARG_CREATE)|| request.exists(ARG_SAVE) || request.exists(ARG_INSTALL)) {
	    StringBuilder sb = new StringBuilder();
	    try {
		Result result= doOutputCreateType(request,  entry,sb,request.exists(ARG_CREATE));
		if(result!=null) return result;
	    } catch(Exception exc) {
		return outputCreateType(request, entry,
					getPageHandler().showDialogError("An error occurred: " + exc.getMessage(),false));
	    }
	}

	return outputCreateType(request, entry, null);
    }

    private Result outputCreateType(Request request, Entry entry,String msg) throws Exception {
	if ( !createTypeOK(request)) {
	    throw new AccessException("Create type not enabled", request);
	}

	StringBuilder sb = new StringBuilder();
	getPageHandler().entrySectionOpen(request, entry, sb, "Create Entry Type");
	String callout = "";
	if(request.isAdmin()) {
	    callout+="+note\nIf you are installing the entry type it is best to do this on a development server as the database schema is changed, etc. Otherwise, make sure you know what you are doing. ";
	}
	callout+=HU.href(getRepository().getUrlBase()+"/userguide/entrytypes.html#create_entry_type_form","View Help", "target=_help");
	callout+="\n-note";
	sb.append(getWikiManager().wikify(request,callout));
	//	getWikiManager().makeCallout(sb,request,callout);

	sb.append(HU.importJS(getHtdocsUrl("/createtype.js")));
	if(msg!=null) sb.append(msg);

	String formId = HU.getUniqueId("form_");
	sb.append(request.formPost(getRepository().URL_ENTRY_SHOW,HU.attrs("id",formId)));
	sb.append(HU.hidden(ARG_ENTRYID, entry.getId()));
	sb.append(HU.hidden(ARG_OUTPUT, getRepository().OUTPUT_CREATETYPE));
        sb.append(HU.hidden(ARG_JSON_CONTENTS,""));
	sb.append(HU.buttons(
			     HU.submit("Download Type",ARG_CREATE,HU.title("Create and download the entry type plugin file")),
			     request.isAdmin()?
			     HU.submit("Install Type",ARG_INSTALL,HU.title("Create and temporarily install the type")):null,
			     HU.submit("Save",ARG_SAVE)));
	sb.append(HU.vspace());
	StringBuilder main = new StringBuilder();
        main.append(HU.formTable());
        main.append(HU.formEntry(msgLabel("Type ID"),
				 HU.input(ARG_TYPEID,request.getString(ARG_TYPEID,""),HU.attrs("size","30")) +
				 " Lower case, no spaces and start with type_, e.g., type_your_type"));

        main.append(HU.formEntry(msgLabel("Name"),
				 HU.input(ARG_TYPENAME,request.getString(ARG_TYPENAME,""),HU.attrs("size", "30")) +" e.g., My Type"));

	String typeHelp=HU.href(getRepository().getUrlBase()+"/entry/types.html","View all types","target=_types");
	String superType="";
	TypeHandler typeHandler = entry.getTypeHandler();
	if(request.defined("supertype")) {
	    superType = request.getString("supertype","");
	} else {
	    TypeHandler parentTypeHandler = typeHandler.getParent();
	    if(parentTypeHandler!=null) {
		superType = parentTypeHandler.getType();
	    }
	}
        main.append(HU.formEntry(msgLabel("Super Type"),
				 HU.input("supertype",superType,
					  HU.attrs("size","30")) +
				 " Optional. e.g., type_point. "+typeHelp));
	List typesSel = new ArrayList();
	for(String tuple:Utils.split("Default:---,TypeHandler - for basic types:TypeHandler,GenericTypeHandler - For columns:GenericTypeHandler,ExtensibleGroupTypeHandler - for columns with groups:ExtensibleGroupTypeHandler,PointTypeHandler - for point data:PointTypeHandler",",")) {
	    typesSel.add(new HtmlUtils.Selector(tuple));
	}

	String extraType = request.getString(ARG_HANDLER_EXTRA,"");
        main.append(HU.formEntry(msgLabel("Java Handler"),
				 HU.select(ARG_HANDLER,typesSel,
					   stringDefined(extraType)?"---":request.getString(ARG_HANDLER,"TypeHandler"),HU.attrs("width","450px")) +HU.space(1)+
				 HU.input(ARG_HANDLER_EXTRA,extraType,
					  HU.attrs("size","40","placeholder","Or enter custom, e.g. org.ramadda.YourType"))));

	/*
	  main.append(HU.formEntry(msgLabel("Use"),
	  "GenericTypeHandler if there are columns<br>ExtensibleGroupTypeHandler for columns that can also be a group<br>PointTypeHandler if this is CSV data"));
	*/

	String catHelp=HU.href(getRepository().getUrlBase()+"/search/type",
			       "View all Categories","target=_cats");
	
	String superCategory = request.getString(ARG_SUPERCATEGORY,"");
	if(!stringDefined(superCategory))
	    superCategory = typeHandler.getSuperCategory();
	String category = request.getString(ARG_CATEGORY,"");
	if(!stringDefined(category))
	    category = typeHandler.getCategory();	
        main.append(HU.formEntryTop(msgLabel("Super Category"),
				    HU.input(ARG_SUPERCATEGORY,superCategory,
					     HU.attrs("placeholder","e.g. Geoscience","size","30")) +
				    HU.space(1) + catHelp));
        main.append(HU.formEntryTop(msgLabel("Category"),
				    HU.input(ARG_CATEGORY,category,
					     HU.attrs("placeholder","e.g., Point Data","size","30"))));

        main.append(HU.formEntryTop(msgLabel("Icon"),
				    HU.input("icon",request.getString("icon",""),
					     HU.attrs("placeholder","/icons/chart.png","size","40"))));	

        main.append(HU.formTableClose());

	StringBuilder properties =  new StringBuilder();
	HU.div(properties,"Must be valid XML attributes, e.g. name=\"value\"",  HU.clazz("ramadda-form-help"));
	properties.append("<br>");
	StringBuilder attrs = addTypeProps(request,"/org/ramadda/repository/resources/attrs.txt","extraattributes",8);
	properties.append(attrs);
	StringBuilder propsSection = addTypeProps(request,"/org/ramadda/repository/resources/props.txt","properties",16);
	HU.div(properties,"Properties, e.g. name=value",  HU.clazz("ramadda-form-help"));
	properties.append("<br>");
	properties.append(propsSection);

	StringBuilder extra = new StringBuilder();
	extra.append(HU.formTable());
	//		     HU.textArea("extraattributes",request.getString("extraattributes",""),4,50));

	HU.formEntry(extra,"",HU.div("Must be valid XML",
				     HU.clazz("ramadda-form-help")));
	HU.formEntry(extra,"Extra XML:",
		     HU.textArea("extraxml",request.getString("extraxml",""),4,50));

	HU.formEntry(extra,"",HU.div("Wiki text for map popup",HU.clazz("ramadda-form-help")));
	HU.formEntry(extra,"Map Popup:",
		     HU.textArea("mappopup",request.getString("mappopup",""),4,50));

        extra.append(HU.formTableClose());	

	StringBuilder props = processTypeProps(request,
					       "/org/ramadda/repository/resources/colattrs.txt","colattributes",10);
	StringBuilder cols = new StringBuilder();
	cols.append(HU.span("",HU.attrs("id","colbuttons")));
	cols.append(HU.space(1));
	cols.append(HU.span("The name needs to be a valid database table ID so all lower case, no spaces or special characters",HU.clazz("ramadda-form-help")));
	cols.append(HU.div(HU.div(props.toString(),HU.clazz("ramadda-dialog")),
			   HU.attrs("id","colattrs","style",HU.css("display","none"))));
        cols.append("<table width=100%>\n\n");

	

	StringBuilder js = new StringBuilder();
	js.append("function showColumnAttrs(){\n");
	js.append("let dialog = HU.makeDialog({contentId:'colattrs',anchor:jqid('colattrsheader'),title:'Column Attributes',	draggable:true,header:true});\n");
	js.append("}\n");
	js.append("Utils.initCopyable('.props_colattributes .prop',{input:'.typecreate-column-extra'});");

	String ex = "e.g. -  size=\"500\" values=\"v1,v2,v3\" ";
	cols.append(HU.tr(HU.td("<b>Name</b>")+HU.td("<b>Label</b>")+HU.td("<b>Type</b>")
			  //+HU.td("<b  title='Size for strings'>Size</b>")+HU.td("<b>Enum Values</b>")
			  +HU.td("<b id=colattrsheader>Extra</b> " + ex+" " + HU.href("javascript:showColumnAttrs()","Select property"))));
	String w1  =HU.attr("width","20%");
	String w2  =HU.attr("width","50%");
	String inputSize  =HU.style("width:98%;");

	List<String> types =
	    Utils.split("string,enumeration,enumerationplus,multienumeration,double,int,boolean,datetime,date,list,password,clob,url,latlon,email",",");

	types.add(0,"");
	for(int i=0;i<50;i++) {
	    cols.append(HU.tr(HU.td(HU.input("column_name_" +i,request.getString("column_name_"+i,""),
					     HU.attrs("column-index",""+i, "class", "ramadda-entry-column")+inputSize),w1)+
			      HU.td(HU.input("column_label_" +i,request.getString("column_label_"+i,""),inputSize),w1)+			    
			      HU.td(HU.select("column_type_" +i,types,request.getString("column_type_"+i,"")),"")+
			      HU.td(HU.textArea("column_extra_" +i,request.getString("column_extra_"+i,""),1,50,
						inputSize+HU.clazz("typecreate-column-extra")),w2),"valign=top"));
	}

        cols.append("</table>\n");
	cols.append(HU.script(js.toString()));

	StringBuilder admin = new StringBuilder();
	Utils.append(admin,HU.div(HU.b("Comment:")),
		     HU.textArea(ARG_COMMENT,request.getString(ARG_COMMENT,""),4,50));

	HU.div(admin, "If For Import is checked then the generated file will not end in \"types.xml\" so it can be included in a plugin and be imported by some other types.xml",HU.clazz("ramadda-form-help"));
	admin.append(HU.insetDiv(HU.labeledCheckbox(ARG_FORIMPORT,"true",request.get(ARG_FORIMPORT,false),"For Import"),
				 0,30,0,0));
	if(request.isAdmin()) {
	    HU.div(admin,"<b>Be careful!</b>. If this entry has database columns and you have changed the types, etc., you may need to drop the database table when installing a new version of this entry type. If you do this then any entries you have created of this type will be removed..",HU.clazz("ramadda-form-help"));
	    admin.append(HU.insetDiv(
				     HU.labeledCheckbox(ARG_DROPTABLE,"true",false,"Yes, drop the database table")+
				     HU.div(getAuthManager().getVerification(request,
									     "To ensure the drop table is OK please enter your password",true,false)),0,30,0,0));
	}

	String basicLabel = "Basic Configuration";
	HU.makeAccordion(sb,
			 new Object[]{HU.span(basicLabel, HU.attrs("id","basic_tab_label")),
				      "Properties",
				      "Advanced Configuration",
				      "Columns",
				      "Admin"},
			 new Object[]{main,properties,extra,cols,admin});
	sb.append(HtmlUtils.formClose());

	List<Metadata> metadataList =
	    getMetadataManager().findMetadata(request, entry,
					      new String[]{AdminMetadataHandler.TYPE_ENTRY_TYPE},false);

	Metadata jsonMetadata = null;
	if(metadataList!=null && metadataList.size()>0) {
	    jsonMetadata = metadataList.get(0);
	}

	sb.append(HU.script("var entryTypeCreateJson = null;\n"));
	if(jsonMetadata!=null) {
	    String json = jsonMetadata.getAttr1();
	    if(Utils.stringDefined(json)) {
		sb.append(HU.script("var entryTypeCreateJson = " + json+";"));
	    }
	} 

	sb.append(HU.script(HU.call("CreateType.init",HU.squote(formId),HU.squote(entry.getId()),"entryTypeCreateJson")));

	getPageHandler().entrySectionClose(request, entry, sb);
	Result result =  new Result("Create Type - " + entry.getName(),sb);
        return getEntryManager().addEntryHeader(request, entry, result);
    }



    private StringBuilder addTypeProps(Request request, String resource, String arg,int rows) throws Exception {
	StringBuilder dfltProps = processTypeProps(request, resource,arg,rows);
	String clazz = "props_" +arg;
	StringBuilder propsSection =new StringBuilder();
	propsSection.append(HU.hbox(
				    HU.textArea(arg,request.getString(arg,""),rows,50,
						HU.attrs("id",arg)),
				    dfltProps));
	propsSection.append(HU.script("Utils.initCopyable('." + clazz+" .prop',{addNL:true,textArea:'" +arg+"'});"));
	return propsSection;
    }

    private StringBuilder processTypeProps(Request request, String resource,String arg,int rows)
	throws Exception {
	List<NamedBuffer>props = new ArrayList<NamedBuffer>();
	String style = HU.style("min-width:600px;width:600px;height:" + (rows*20)+"px;max-height:300px;overflow-y:auto;");
	for(String line: Utils.split(IOUtil.readContents(resource,getClass()),"\n",true,true)) {
	    if(line.startsWith("cat:")) {
		if(props.size()>0)
		    props.get(props.size()-1).append("</div>");
		props.add(new NamedBuffer(line.substring("cat:".length())));
		props.get(props.size()-1).append("<div " + style+">");
		continue;
	    }
	    if(props.size()==0) {
 		props.add(new NamedBuffer(""));
		props.get(props.size()-1).append("<div " + style+">");
	    }
	    NamedBuffer buff= props.get(props.size()-1);
	    if(line.startsWith("##")) {
		buff.append("<div class=searchprop style='font-weight:bold;);'>" + line.substring(2)+"</div>");
		continue;
	    } else    if(line.startsWith("#")) {
		buff.append("<div class=searchprop style='padding:4px;background:#eee;font-style:italic;);'>" + line.substring(1)+"</div>");
		continue;
	    }
	    buff.append(HU.div(line,HU.clazz("searchprop prop")));
	}

	props.get(props.size()-1).append("</div>");
	String clazz = "props_" +arg;
	StringBuilder dfltProps=new StringBuilder("<div class=" + clazz+">");
	HU.addPageSearch(dfltProps,"." +clazz+" .searchprop",null,"Search");

	if(props.size()==1) {
	    dfltProps.append(props.get(0).getBuffer());
	} else {
	    HU.makeTabs(dfltProps,props);
	}
	dfltProps.append("</div>");
	return dfltProps;
    }

    private Metadata getDefineTypeMetadata(Request request, Entry entry) throws Exception {
	List<Metadata> metadataList =
	    getMetadataManager().findMetadata(request, entry,
					      new String[]{AdminMetadataHandler.TYPE_ENTRY_TYPE},false);

	if(metadataList!=null && metadataList.size()>0) {
	    return metadataList.get(0);
	}
	return null;
    }	

    public Result doOutputCreateType(Request request, Entry entry,StringBuilder sb,boolean create)
	throws Exception {
	boolean applyMetadata = request.get("applymetadata",false);
	Metadata jsonMetadata = getDefineTypeMetadata(request, entry);
	if(applyMetadata) {
	    if(jsonMetadata==null) throw new IllegalArgumentException("No entry type metadata available:" + entry);
	    String json = jsonMetadata.getAttr1();
	    JSONArray a  =new JSONArray(json);
	    for (int i = 0; i < a.length(); i++) {
		JSONObject param= a.getJSONObject(i);
		request.put(param.getString("n"),param.getString("v"));
	    }
	}
	String id = request.getString(ARG_TYPEID,"").trim();
	if(!Utils.stringDefined(id)) {
	    sb.append(getPageHandler().showDialogError("No ID specified"));
	    return null;
	}
	id = Utils.makeID(id);
	if(!id.startsWith("type_")) {
	    return outputCreateType(request,  entry,getPageHandler().showDialogError("Bad format for type ID. Needs to start with &quot;type_&quot;",false));
	}

	sb = new StringBuilder();

	String superType = request.getString("supertype","").trim();
	String name = request.getString(ARG_TYPENAME,"");
	if(!Utils.stringDefined(name)) name = Utils.makeLabel(id);
	String handler = request.getString(ARG_HANDLER,"").trim();
	String extraHandler = request.getString(ARG_HANDLER_EXTRA,"").trim();
	if(stringDefined(extraHandler)) handler =extraHandler;
	if(!Utils.stringDefined(handler) || handler.equals("---")) {
	    if(superType.equals("type_point"))
		handler="PointTypeHandler";
	    else
		handler="org.ramadda.repository.type.TypeHandler";
	}
	if(handler.equals("---")) handler="TypeHandler";
	if(handler.equals("PointTypeHandler")) handler="org.ramadda.data.services." +handler;
	else if(handler.matches("^(TypeHandler|GenericTypeHandler|ExtensibleGroupTypeHandler)$")) {
	    handler = "org.ramadda.repository.type."+handler;
	}
	String json = request.getString(ARG_JSON_CONTENTS,null);
	if(json!=null && !applyMetadata) {
	    if(jsonMetadata==null) {
		jsonMetadata = new Metadata(getRepository().getGUID(), entry.getId(),
					    getMetadataManager().findType(AdminMetadataHandler.TYPE_ENTRY_TYPE),
					    false, json, "", "", "","");
		getMetadataManager().addMetadata(request,entry,jsonMetadata);
	    }
	    jsonMetadata.setAttr1(json);
	    entry.setMetadataChanged(true);
	    getEntryManager().updateEntry(null, entry);
	}

	String filename;
	if(request.get(ARG_FORIMPORT,false)) {
	    filename = id+".xml";
	} else {
	    filename = id+"_types.xml";
	}
	String comment;

	if(request.get(ARG_FORIMPORT,false)) {
	    comment ="\nSince this is for import add a:\n<import resource=\"" + filename+"\"/>\ninto some types.xml file\n"; 
	} else {
	    comment = "\nCopy this into your ramadda home/plugins directory and restart RAMADDA\n";
	}

	if(stringDefined(request.getString(ARG_COMMENT,null))) {
	    comment += request.getString(ARG_COMMENT,null);
	    comment+="\n";
	}

	int columnCnt=0;
	StringBuilder colSB = new StringBuilder();
	for(int i=0;i<50;i++) {
	    String cname = request.getString("column_name_"+i,"").trim();
	    if(!Utils.stringDefined(cname)) continue;
	    cname= Utils.makeID(cname);
	    if(columnCnt++==0) {
		colSB.append("\n");
		colSB.append(XU.comment("Columns"));
	    }

	    String label = request.getString("column_label_"+i,"");
	    if(!Utils.stringDefined(label)) label  = Utils.makeLabel(cname);
	    String type = request.getString("column_type_"+i,"");
	    if(!Utils.stringDefined(type)) type= "string";
	    String size = request.getString("column_size_"+i,"");
	    String attrs = XU.attrs("name",cname,
				    "label",label,
				    "type",type);
	    if(request.defined("column_help_"+i)) {
		attrs+=XU.attrs("help",request.getString("column_help_"+i,""));
	    }

	    if(Utils.stringDefined(size)) 
		attrs+=XU.attr("size",size);
	    if(type.startsWith("enum") && request.defined("column_values_"+i)) {
		attrs+=XU.attr("values",request.getString("column_values_"+i,""));
	    }
	    //	    String group = request.getString("column_group_"+i,"");
	    String cextra = request.getString("column_extra_"+i,"");
	    if(Utils.stringDefined(cextra))  {
		cextra = cextra.replace("\r"," ");
		cextra = cextra.replace("\n"," ");		
		attrs+=" " + cextra+" ";
	    }
	    colSB.append(XU.tag("column",attrs));
	    colSB.append("\n");
	}

	if(columnCnt>0 && handler.equals("org.ramadda.repository.type.TypeHandler")) {
	    handler = "org.ramadda.repository.type.GenericTypeHandler";
	}

	sb.append(XmlUtil.comment(comment));
	sb.append("<type ");
	sb.append(XU.attrs("name",id));
	sb.append("\n");
	sb.append(XU.attrs("description",name));
	sb.append("\n");
	sb.append(XU.attrs("handler",handler));	
	sb.append("\n");
	if(request.defined("supertype")) {
	    sb.append(XU.attr("super",superType));
	    sb.append("\n");
	}

	if(request.defined(ARG_SUPERCATEGORY)) {
	    sb.append(XU.attr("supercategory",request.getString(ARG_SUPERCATEGORY,"").trim()));
	    sb.append("\n");
	}

	if(request.defined(ARG_CATEGORY)) {
	    sb.append(XU.attr("category",request.getString(ARG_CATEGORY,"").trim()));
	    sb.append("\n");
	}

	String extraAttributes = request.getString("extraattributes","");
	if(Utils.stringDefined(extraAttributes)) {
	    for(String line:Utils.split(extraAttributes,"\n",true,true)) {
		if(line.startsWith("#")) continue;
		sb.append(line);
		if(line.indexOf("=")<=0) {
		    throw new IllegalArgumentException("Error processing Advanced Configuration Extra Attributes:<br>Bad attribute:" + line+"<br>Must be of the format<pre>attribute=\"value\"</pre>");
		}
		sb.append("\n");
	    }
	}
	sb.append(">\n");	
	sb.append(colSB);

	String mappopup = request.getString("mappopup","");
	if(Utils.stringDefined(mappopup)) {
	    sb.append("<property name=\"map.popup\">\n<![CDATA[");
	    sb.append(mappopup);
	    sb.append("]]></property>\n");
	}

	String extra = request.getString("extraxml","");
	if(Utils.stringDefined(extra)) {
	    sb.append(extra.replace("\r\n","\n"));	    
	    sb.append("\n");	    
	}
	sb.append(XU.comment("Properties"));
	sb.append("<property name=\"record.file.class\" value=\"org.ramadda.data.point.text.CsvFile\"/>\n");

	if(request.defined("icon"))  {
	    sb.append(XU.tag("property",XU.attrs("name","icon","value",request.getString("icon",""))));
	    sb.append("\n");
	}

	for(String line:Utils.split(request.getString("properties",""),"\n",true,true)) {
	    if(line.startsWith("#")) continue;
	    List<String> toks = Utils.splitUpTo(line,"=",2);
	    if(toks.size()!=2) continue;
	    sb.append(XU.tag("property",XU.attrs("name",toks.get(0).trim(),"value",toks.get(1).trim())));
	    sb.append("\n");
	}

	String seesv = (String)entry.getValue(request, "convert_commands");
	if(Utils.stringDefined(seesv)) {
	    seesv+="\n-args";
            List<String> lines  =  Seesv.tokenizeCommands(seesv,false).get(0);
	    String csvCommands = Seesv.makeCsvCommands(lines);
	    if(Utils.stringDefined(csvCommands)) {
		sb.append("\n");
		sb.append(XU.comment("SeeSV commands"));
		sb.append(XU.tag("property",XU.attr("name","record.properties"),XU.getCdata("\n"+csvCommands+"\n")));

	    }
	}

	String desc = entry.getDescription();
	if (Utils.stringDefined(desc) && entry.getTypeHandler().isWikiText(desc)) {
	    desc = desc.replace("{skip{","{{");
	    sb.append("\n");
	    sb.append(XU.comment("Wiki text"));
	    desc=desc.replaceAll("^<wiki>","");
	    desc = desc.replace("\r\n","\n");	    
	    sb.append(XU.tag("wiki","",XU.getCdata(desc)));

	}

	sb.append("</type>\n");

	Element root =null;
	String xml = sb.toString();
	try {
	    //check for syntax validity
            root = XU.getRoot(xml);
	    if(request.isAdmin() && request.exists(ARG_INSTALL)) {
		getEntryManager().clearCache();
		root.setAttribute("ignoreerrors","false");
		try {
		    TypeHandler typeHandler = getRepository().loadTypeHandler(root,true);
		} catch(Exception exc) {
		    Throwable thr = LogUtil.getInnerException(exc);
		    String msg = thr.toString();
		    if(msg.indexOf("type of a column may not be changed")>=0 ||
		       msg.indexOf("cannot be cast automatically")>=0) {
			msg+="<br>It appears that the type of a column was changed";
		    }
		    return  outputCreateType(request, entry,
					     getPageHandler().showDialogError("There was an error loading the entry type: " + msg,false));
		}
	    }
	}  catch(Exception exc) {
            Throwable thr = LogUtil.getInnerException(exc);
	    String msg = thr.toString();
	    String line = StringUtil.findPattern(msg, "line:([0-9]+) ");
	    if(line!=null) {
		List<String> lines = Utils.split(xml,"\n");
		int lineIdx = Integer.parseInt(line);
		msg+="<pre>";
		for(int i=lineIdx-4;(i<lineIdx+4) && (i<lines.size());i++) {
		    if(i>=0 && i<lines.size()-1) {
			msg+=lines.get(i);
			msg+="\n";
		    }
		}
		msg+="</pre>";
	    }
	    return  outputCreateType(request, entry,
				     getPageHandler().showDialogError("There was an error in the XML:" + msg));
	}

	String theMessage ="";
	/*
	  if(root!=null && request.isAdmin() && request.exists(ARG_INSTALL)) {
	  try {
	  getRepository().loadTypeHandler(root,true);
	  theMessage = HU.div("The Entry type has been temporarily installed. The plugin file still needs to be downloaded and installed in the RAMADDA plugins directory.");
	  }  catch(Exception exc) {
	  return  outputCreateType(request, entry,
	  getPageHandler().showDialogError("There was an error loading the entry type:" + exc));
	  }		
	  }*/

	TypeHandler typeHandler =getRepository().getTypeHandler(id);
	if(typeHandler!=null && columnCnt>0) {
	    String tableName = typeHandler.getTableName();
	    if(!stringDefined(tableName)) {
		String msg = "You have defined columns but there is no database table defined. Perhaps you did not set the Java Handler to GenericTypeHandler?";
		return  outputCreateType(request, entry,
					 getPageHandler().showDialogError(msg,false));
	    }
	}

	if(request.isAdmin() && request.get(ARG_DROPTABLE,false)) {
	    StringBuilder buff = new StringBuilder();
	    if(!getAuthManager().verify(request,buff, true)) {
		return  outputCreateType(request, entry,buff.toString());
	    }
	    if(typeHandler==null) {
		String msg = getPageHandler().showDialogError("The Entry Type has not been installed: "  + id);
		return  outputCreateType(request, entry,msg);
	    }

	    String tableName = typeHandler.getTableName();
	    if(!stringDefined(tableName)) {
		String msg = getPageHandler().showDialogError("No table name defined for type handler: "  + typeHandler);
		return  outputCreateType(request, entry,msg);
	    }

	    String sql = "drop table " +tableName;
	    try {
		getDatabaseManager().loadSql(sql, false,false);
		theMessage = HU.div("The database table has been dropped.") +
		    theMessage;
	    } catch(Exception exc) {
		String msg = getPageHandler().showDialogError("There was an error dropping the table: " + id +"<br>Error:" + exc,false);
		return  outputCreateType(request, entry,msg);
	    }
	}

	if(stringDefined(theMessage)) {
	    theMessage  = getPageHandler().showDialogNote(theMessage);
	    return  outputCreateType(request, entry,theMessage);
	}

	if(!create) {
	    return outputCreateType(request,  entry,"");
	}

	request.setReturnFilename(filename, false);	    

	return new Result("", sb, MIME_XML);
    }

}

