/**
   Copyright (c) 2008-2026 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.media;

import org.ramadda.repository.*;
import org.ramadda.repository.metadata.Metadata;
import org.ramadda.repository.type.TypeHandler;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;
import org.ramadda.util.Utils;

import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import java.net.URL;

import java.util.ArrayList;
import java.util.List;

import java.util.List;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("unchecked")
public class HtmlImportHandler extends ImportHandler {
    public static boolean debug = true;
    public static final String ARG_IMPORT_USEURL  = "useurl";
    public static final String ARG_IMPORT_NAMETEMPLATE  = "nametemplate";    
    public static final String ARG_IMPORT_PATTERN = "import.pattern";
    public static final String ARG_IMPORT_NOTPATTERN = "import.notpattern";    
    public static final String ARG_IMPORT_RECURSE = "import.recurse";
    public static final String ARG_IMPORT_RECURSE_PATTERN =     "import.recurse.pattern";
    public static final String ARG_IMPORT_RECURSE_DEPTH =   "import.recurse.depth";
    public static final String ARG_IMPORT_ADDINTERMEDIATE="addintermediate";
    public static final String ARG_IMPORT_TEST_PARTIAL = "import.testpartial";
    public static final String ARG_IMPORT_TEST_FULL = "import.testfull";    
    public static final String ARG_IMPORT_CREATEENTRIES = "import.createentries";
    public static final String ARG_IMPORT_PROVENANCE = "import.addprovenance";
    public static final String ARG_IMPORT_UNCOMPRESS = "import.uncompress";
    public static final String ARG_IMPORT_HANDLE = "import.handle";
    public static final String TYPE_HTML = "html";

    public HtmlImportHandler() {
        super(null);
    }

    public HtmlImportHandler(Repository repository) {
        super(repository);
    }

    @Override
    public void addImportTypes(List<TwoFacedObject> importTypes,
                               Appendable formBuffer) {
        super.addImportTypes(importTypes, formBuffer);
        importTypes.add(new TwoFacedObject("Links in an HTML Page",
                                           TYPE_HTML));
    }



    private boolean importHtmlInner(Request request,
				    Object actionId,
				    HashSet seen,
				    List<Entry> entries,
				    int[]cnt,
                                    StringBuilder sb,
				    HtmlUtils.Link link,
                                    Entry parentEntry)   throws Exception {
        if ( !getActionManager().getActionOk(actionId)) {
            return false;
        }

        List<String> errors = new ArrayList<String>();
        boolean addFile = request.getString(ARG_IMPORT_HANDLE,  "").equals("file");
        boolean uncompress    = request.get(ARG_IMPORT_UNCOMPRESS, false);
        boolean addProvenance = request.get(ARG_IMPORT_PROVENANCE, false);
	boolean addIntermediate = request.get(ARG_IMPORT_ADDINTERMEDIATE,false);
	String  recursePattern = request.getString(ARG_IMPORT_RECURSE_PATTERN, "");
	String  notPattern = request.getString(ARG_IMPORT_NOTPATTERN, "");	
        List<HtmlUtils.Link> pageLinks = null;
	for (HtmlUtils.Link childLink : link.getChildren()) {
	    if (seen.contains(childLink)) {
		continue;
	    }
	    seen.add(childLink);
	    if ( !getActionManager().getActionOk(actionId)) {
		return false;
	    }
	    boolean isIntermediate = Utils.equals(childLink.getPattern(), recursePattern);

	    if(isIntermediate) {
		Entry newParent;
		String name = childLink.getLabel().trim();
		name = name.replaceAll("/$", "").replaceAll("^/", "");
		if(!addIntermediate) {
		    newParent = parentEntry;
		} else {
		    newParent = getEntryManager().findEntryWithName(request,  parentEntry, name);
		    if (newParent == null) {
			newParent = getEntryManager().makeEntry(
								request, new Resource(), parentEntry, name, "",
								request.getUser(),
								getRepository().getTypeHandler(
											       TypeHandler.TYPE_GROUP), null);
			entries.add(newParent);
			getEntryManager().addNewEntry(request, newParent);
		    }
		}
		boolean ok = importHtmlInner(request, actionId, seen,entries,cnt,sb, childLink, newParent);
		if(!ok) return false;
		continue;
	    }

	    if(stringDefined(notPattern)) {
		if(childLink.matches(notPattern)) {
		    continue;
		}
	    }


	    Resource    resource    = null;
	    TypeHandler typeHandler = getRepository().getTypeHandler(request);
	    String      name        = childLink.getLabel();
	    String template   = request.getString(ARG_IMPORT_NAMETEMPLATE,"");
	    if (request.get(ARG_IMPORT_USEURL, false) || (name.length() < 4)) {
		name = IOUtil.stripExtension(
					     IOUtil.getFileTail(childLink.getUrl().toString()));
		name = Utils.makeLabel(name);
		name = name.replaceAll("^[0-9]+", "").trim();
	    }

	    if(stringDefined(template)) {
		String filename = IOUtil.stripExtension(IOUtil.getFileTail(childLink.getUrl().toString()));
		name = template.replace("${parent}",parentEntry.getName()).replace("${filename}",filename).replace("${filename_label}",
														   Utils.makeLabel(filename));
										   
	    }


	    //TODO: check if we have a entry already
	    Entry existing = getEntryManager().findEntryWithName(request, parentEntry, name);
	    if (existing == null) {
		String tmp = IOUtil.stripExtension(name);
		existing = getEntryManager().findEntryWithName(request, parentEntry, tmp);
	    }
	    if (existing != null) {
		sb.append("<li> ");
		sb.append(msgLabel("Entry already exists"));
		sb.append(" ");
		sb.append(childLink.getUrl());
		continue;
	    }

	    try {
		if (addFile) {
		    File tmpFile =
			getStorageManager().getTmpFile(request,
						       IOUtil.getFileTail(childLink.getUrl().toString()));
		    FileOutputStream fos = new FileOutputStream(tmpFile);
		    if (IOUtil.writeTo(
				       IO.getInputStream(childLink.getUrl().toString()),
				       fos) == 0) {
			errors.add("Failed to read url:" + childLink.getUrl());
			IOUtil.close(fos);

			continue;
		    }
		    IOUtil.close(fos);

		    if (request.get(ARG_IMPORT_UNCOMPRESS, false)) {
			tmpFile =  getStorageManager().uncompressIfNeeded(request, tmpFile);
			if (tmpFile == null) {
			    errors.add("Failed to uncompress file:" + tmpFile);
			    continue;
			}
			name = RepositoryUtil.getFileTail(tmpFile.getName());
		    }
		    tmpFile = getStorageManager().moveToStorage(request, tmpFile);
		    if ((typeHandler == null)
			|| typeHandler.getType().equals(
							TypeHandler.TYPE_FINDMATCH)) {
			typeHandler =
			    getEntryManager().findDefaultTypeHandler(request, tmpFile.toString());
		    }
		    resource = new Resource(tmpFile, Resource.TYPE_STOREDFILE);
		} else {
		    resource = new Resource(childLink.getUrl().toString(),Resource.TYPE_URL);
		    if ((typeHandler == null)
			|| typeHandler.getType().equals(
							TypeHandler.TYPE_FINDMATCH)) {
			typeHandler =
			    getEntryManager().findDefaultTypeHandler(request,
								     childLink.getUrl().toString());
		    }
		}

		cnt[0]++;
		Entry entry = getEntryManager().makeEntry(request, resource,
							  parentEntry, name, "", request.getUser(),
							  typeHandler, null);

		entry.setEntryOrder(cnt[0]*5);
		if (addProvenance) {
		    getMetadataManager().addMetadata(request,entry,
						     new Metadata(getRepository().getGUID(),
								  entry.getId(), getMetadataManager().findType("metadata_source"),
								  false, childLink.getUrl().toString(),
								  childLink.getUrl().toString(),
								  null, null,
								  null));
		}

		entry.getTypeHandler().initializeEntryFromHarvester(request, entry, true);
		entries.add(entry);
		getEntryManager().addNewEntry(request, entry);
	    } catch (Exception exc) {
		sb.append("<li> ");
		sb.append(msgLabel("Error fetching URL"));
		sb.append(" ");
		sb.append(childLink.getUrl());
		sb.append("\n");
		sb.append("" + exc);
	    }
	    String message =   "Created " + entries.size() +" entries<br>";
	    message+=sb;
	    getActionManager().setActionMessage(actionId,message);
	}
	if (errors.size() > 0) {
	    sb.append(getPageHandler().showDialogError(StringUtil.join("<br>", errors)));
	    return false;
	}
	return true;
    }

    private Result importHtml(final Request request,
			      final Repository repository,
			      final HtmlUtils.Link topLink,
			      final Entry parentEntry,
			      final boolean createEntries,
			      final String recursePattern,
			      final String pattern)
	throws Exception {
	//IMPORTANT!
	getAuthManager().ensureAuthToken(request);
	ActionManager.Action action = new ActionManager.Action() {
		StringBuilder resultsSB    = new StringBuilder("");
		List<Entry> entries = new ArrayList<Entry>();
		public void run(Object actionId) throws Exception {
		    HashSet seen = new HashSet();
		    int []cnt={0};
		    int           depth = request.get(ARG_IMPORT_RECURSE_DEPTH, 1);
		    boolean recurse = request.get(ARG_IMPORT_RECURSE, false);
		    boolean testFull    = request.exists(ARG_IMPORT_TEST_FULL);	
		    int[]pageCnt={0};
		    int[]linkCnt={0};
		    extractLinks(request, topLink, recurse, recursePattern, pattern, depth,createEntries?true:testFull,actionId,pageCnt,linkCnt);
		    if(!createEntries) {
			if (Utils.listEmpty(topLink.getChildren())) {
			    resultsSB.append(getPageHandler().showDialogNote(
									     "No pages found. Maybe add \".*\" before and after pattern"));
			} else {
			    resultsSB.append(msgHeader("Scanned links"));
			    displayLinks(request,resultsSB,topLink.getChildren());
			    resultsSB.append("<p>");
			}
		    } else {
			resultsSB.append("<div  style='max-height:400px;overflow-y:auto;margin-bottom:1em;'>");
			resultsSB.append("<ul>");
			importHtmlInner(request, actionId, seen, entries,cnt,resultsSB, topLink, parentEntry);
			resultsSB.append("</ul>");
			resultsSB.append("</div>");

		    }
		}
		@Override
		public Result finishAction(ActionManager.ActionInfo info, StringBuffer messages) throws Exception {
		    StringBuilder sb = new StringBuilder();
		    getPageHandler().entrySectionOpen(request, parentEntry, sb, "HTML Import");
		    if(info.getError()!=null) {
			sb.append(getPageHandler().showDialogError("An error has occurred"
								   + "<p>" + info.getError()));
		    } else if(info.getCancelled()) {
			sb.append(getPageHandler().showDialogNote("Processing has been cancelled"));
			if(!createEntries) {
			    resultsSB.append(msgHeader("Scanned links"));
			    displayLinks(request,resultsSB,topLink.getChildren());
			}

		    } else if(!info.getRunning()) {
			sb.append(getPageHandler().showDialogNote("Processing complete"));
		    }
		    sb.append(resultsSB);
		    if(entries.size()>0) {
			if(entries.size()==1) {
			    sb.append(msgHeader("1 entry has been created"));
			} else {
			    sb.append(msgHeader(entries.size() +" entries have been created"));
			}
			sb.append("<ul style='max-height:400px;overflow-y:auto;margin-bottom:1em;'>");
			for(Entry entry: entries) {
			    sb.append("<li>");
			    sb.append(HU.href(request.entryUrl(getRepository().URL_ENTRY_SHOW,entry), entry.getName()));
			}
			sb.append("</ul><p>");
		    }
		    makeForm(request, parentEntry, topLink.getUrl().toString(), sb);
		    getPageHandler().entrySectionClose(request, parentEntry, sb);
		    return getEntryManager().makeEntryEditResult(request, parentEntry, "", sb);
		}
	    };
	return getActionManager().doAction(request, action, "Importing HTML",
					   "", parentEntry);
    }

    public Result handleUrlRequest(Request request, Repository repository,
				   String url, Entry parentEntry)
	throws Exception {

	if ( !request.getString(ARG_IMPORT_TYPE, "").equals(TYPE_HTML)) {
	    return null;
	}
	boolean recurse = request.get(ARG_IMPORT_RECURSE, false);
	String  pattern = request.getString(ARG_IMPORT_PATTERN, "");
	String  recursePattern = request.getString(ARG_IMPORT_RECURSE_PATTERN, "");

	boolean testPartial    = request.exists(ARG_IMPORT_TEST_PARTIAL);
	boolean testFull    = request.exists(ARG_IMPORT_TEST_FULL);	
	boolean createEntries    = request.exists(ARG_IMPORT_CREATEENTRIES);
	URL                  rootUrl   = new URL(url);
	HtmlUtils.Link topLink = new HtmlUtils.Link(rootUrl,"Main page");
	if(!testPartial && !testFull && !createEntries) {
	    StringBuilder sb = new StringBuilder();
	    getPageHandler().entrySectionOpen(request, parentEntry, sb, "HTML Import");
	    makeForm(request, parentEntry, topLink.getUrl().toString(), sb);
	    getPageHandler().entrySectionClose(request, parentEntry, sb);
	    return getEntryManager().makeEntryEditResult(request, parentEntry, "", sb);
	}


	return importHtml(request, repository, topLink, 
			  parentEntry,
			  createEntries,
			  recurse
			  ? recursePattern
			  : null, pattern);

	/*
	if (Utils.listEmpty(topLink.getChildren())) {
	    sb.append(getPageHandler().showDialogNote(
						      "No pages found. Maybe add \".*\" before and after pattern"));
	} else {
	    sb.append(msgHeader(testPartial?"Partial Imports":"Full Imports"));
	    displayLinks(request,sb,topLink.getChildren());
	    }*/	    
	

    }



    private void makeForm(Request request, Entry parentEntry, String url,
			  StringBuilder sb) throws Exception {
	boolean recurse = request.get(ARG_IMPORT_RECURSE, false);
	String  recursePattern = request.getString(ARG_IMPORT_RECURSE_PATTERN, "");
	String  pattern = request.getString(ARG_IMPORT_PATTERN, "");

	TypeHandler typeHandler = getRepository().getTypeHandler(request.getString(ARG_TYPE,TypeHandler.TYPE_FINDMATCH),false);
	String buttons =
	    HU.buttons(HU.submit("Partial Test",ARG_IMPORT_TEST_PARTIAL),
		       HU.submit("Full Test",ARG_IMPORT_TEST_FULL),		       
		       HU.submit("Create entries", ARG_IMPORT_CREATEENTRIES));
	sb.append(msgHeader("HTML Import"));
	request.uploadFormWithAuthToken(sb,
					getRepository().URL_ENTRY_XMLCREATE,
					makeFormSubmitDialog(sb,
							     msg("Importing HTML")));
	sb.append(HU.hidden(ARG_GROUP, parentEntry.getId()));
	sb.append(HU.hidden(ARG_IMPORT_TYPE, TYPE_HTML));
	sb.append(HU.formTable());
	sb.append(HU.formEntry(msgLabel("URL"),
			       HU.input(ARG_URL, url,
					HU.SIZE_70)));

	HU.formEntry(sb, "",   HU.labeledCheckbox(ARG_IMPORT_RECURSE,
						  "true", recurse,"Recurse"));

	HU.formEntry(sb, "",HU.div("Regular expression pattern to match on links to other pages",
				   HU.cssClass("ramadda-form-help")));

	HU.formEntry(sb,msgLabel("Recurse Pattern"),
		     HU.input(ARG_IMPORT_RECURSE_PATTERN, recursePattern,
			      HU.SIZE_50));

	HU.formEntry(sb, msgLabel("Recurse Depth"),
		     HU.input(ARG_IMPORT_RECURSE_DEPTH,
			      request.getString(ARG_IMPORT_RECURSE_DEPTH, "1"),
			      HU.SIZE_5));

	HU.formEntry(sb,"",
		     HU.labeledCheckbox(ARG_IMPORT_ADDINTERMEDIATE, "true",
					request.get(ARG_IMPORT_ADDINTERMEDIATE, false),
					msg("Add intermediate folders")));



	HU.formEntry(sb, "",HU.div("Link regular expression - e.g. \".*\\.pdf\"",HU.cssClass("ramadda-form-help")));

	HU.formEntry(sb, msgLabel("Entry Pattern"),
		     HU.input(ARG_IMPORT_PATTERN, pattern, HU.attrs("size","50","placeholder","e.g. .*\\.pdf")));

	HU.formEntry(sb, msgLabel("Not Pattern"),
		     HU.input(ARG_IMPORT_NOTPATTERN, request.getString(ARG_IMPORT_NOTPATTERN,""),
			      HU.attrs("size","50")));	

	boolean addFile = request.getString(ARG_IMPORT_HANDLE,
					    "file").equals("file");
	HU.formEntry(sb,
		     msgLabel("What to do"),
		     HU.labeledRadio(ARG_IMPORT_HANDLE, "file", addFile,
				     msg("Download the file"))
		     + HU.space(3)
		     + HU.labeledRadio(ARG_IMPORT_HANDLE, "url", !addFile,
				       msg("Add the link")));

	HU.formEntry(sb, msgLabel("Name template"),
		     HU.input(ARG_IMPORT_NAMETEMPLATE, request.getString(ARG_IMPORT_NAMETEMPLATE,""),
			      HU.attrs("size","50")) +" " +
		     "${parent}, ${filename}, ${filename_label}");


	HU.formEntry(sb,"",
		     HU.labeledCheckbox(ARG_IMPORT_UNCOMPRESS, "true",
					request.get(ARG_IMPORT_UNCOMPRESS, false),
					msg("Uncompress file")));

	HU.formEntry(sb,"",
		     HU.labeledCheckbox(ARG_IMPORT_USEURL, "true",
					request.get(ARG_IMPORT_USEURL,false),
					"Use URL for name"));

	HU.formEntry(sb, "",HU.div("What type of entry to create",HU.cssClass("ramadda-form-help")));
	HU.formEntry(sb, msgLabel("Entry type"),
		     getPageHandler().makeFileTypeSelector(request, typeHandler, false));

	sb.append(HU.formTableClose());
	StringBuilder mtdSb = new StringBuilder();
	mtdSb.append(HU.labeledCheckbox(ARG_IMPORT_PROVENANCE, "true",
					request.get(ARG_IMPORT_PROVENANCE, false),
					"Add the source URL as provenance metadata"));

	mtdSb.append("<br>");
	mtdSb.append(HU.labeledCheckbox(ARG_DOOCR, "true", false,"Extract text from images"));
	mtdSb.append("<br>");

	String extract = getLLMManager().getNewEntryExtract(request);
	if(stringDefined(extract)) {
	    mtdSb.append("\n");
	    mtdSb.append(HU.b("Extract metadata using GPT") +":<br>");
	    mtdSb.append(extract);
	    mtdSb.append("\n");
	    String mtd = HU.insetDiv(mtdSb.toString(),0,30,0,0);
	    sb.append(HU.makeShowHideBlock("Metadata extraction",mtd,false));
	} else {
	    sb.append(mtdSb);
	}
	sb.append("<br>");
	sb.append(buttons);
	sb.append(HU.formClose());
    }

    private void extractLinks(Request request, HtmlUtils.Link topLink,
			      boolean recurse, String recursePattern,
			      String pattern, int depth,
			      boolean full,Object actionId,int[]pageCnt,int[]linkCnt) throws Exception {
	topLink.setChildren(HU.extractLinks(topLink.getUrl(), recursePattern,pattern));
	pageCnt[0]++;
	linkCnt[0]+=topLink.getChildren().size();

	if(actionId!=null) {
	    if(!getActionManager().getActionOk(actionId)) {
		return;
	    }
	    getActionManager().setActionMessage(actionId,"Processing: " + topLink.getLabel() +" Total pages: " + pageCnt[0] +" Total links: " + linkCnt[0]);
	}

	if (recurse && depth>0) {
	    for(HtmlUtils.Link pageLink: topLink.getChildren()) {
		extractLinks(request, pageLink,recurse,recursePattern, pattern, depth-1,full,actionId,pageCnt,linkCnt);
		if(!getActionManager().getActionOk(actionId)) {
		    return;
		}
		pageLink.setChildren(HU.extractLinks(pageLink.getUrl(), recursePattern,pattern));
		if(!full) {
		    break;
		}
	    }
	}
    }

    private void displayLinks(Request request,StringBuilder sb,List<HtmlUtils.Link>pageLinks) {
	if (Utils.listEmpty(pageLinks))  return;
	sb.append("<div  style='max-height:400px;overflow-y:auto;'>");
	sb.append("<ul>");
	for (HtmlUtils.Link link : pageLinks) {
	    sb.append("<li> ");
	    sb.append(link.getHref(true));
	    if (link.getSize() > 0) {
		sb.append("  --  ");
		sb.append(
			  " "
			  + RepositoryManager.formatFileLength(
							       link.getSize()));
	    }
	    sb.append("  --  ");
	    sb.append(link.getUrl());
	    displayLinks(request, sb, link.getChildren());
	}
	sb.append("</ul>");
	sb.append("</div>");	

	/*
	  for (HtmlUtils.Link link : links) {
	  sb.append("<li> ");
	  String lurl = link.getUrl().toString();
	  String cbxId = HU.getUniqueId("cbx");
	  String cbxCall =
	  HU.attr(HU.ATTR_ONCLICK,
	  HU.call("HU.checkboxClicked",
	  HU.comma("event",
	  HU.squote("linkok"),
	  HU.squote(cbxId))));
	  sb.append(HU.labeledCheckbox("linkok",lurl,
	  anySelected?okLinks.contains(lurl):true,
	  cbxCall + HU.id(cbxId),
	  link.getLabel()));
	  sb.append(HU.space(1));
	  if (link.getSize() > 0) {
	  sb.append("  --  ");
	  sb.append(
	  " "
	  + RepositoryManager.formatFileLength(
	  link.getSize()));
	  }
	  sb.append("  --  ");
	  sb.append(HU.href(link.getUrl().toString(),link.getUrl().toString(),HU.attrs("target","link")));
	  sb.append(HU.br());
	  }
	  sb.append("</ul>");
	  } else {
	  sb.append(
	  getPageHandler().showDialogNote(
	  "No links found. Maybe add \".*\" before and after pattern"));
	  }

	*/
    }



}
