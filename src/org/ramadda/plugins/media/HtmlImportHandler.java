/**
Copyright (c) 2008-2024 Geode Systems LLC
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@SuppressWarnings("unchecked")
public class HtmlImportHandler extends ImportHandler {
    public static final String ARG_IMPORT_PATTERN = "import.pattern";
    public static final String ARG_IMPORT_RECURSE = "import.recurse";
    public static final String ARG_IMPORT_RECURSE_PATTERN =
        "import.recurse.pattern";

    public static final String ARG_IMPORT_RECURSE_DEPTH =
        "import.recurse.depth";

    public static final String ARG_IMPORT_DOIT = "import.doit";
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

    private boolean importHtmlInner(Request request, Object actionId,
				    int[]cnt,
                                    int depth, StringBuilder sb, URL url,
				    List<String> okLinks,
                                    Entry parentEntry, String recursePattern,
                                    String pattern)
            throws Exception {

        if (depth <= 0) {
            return true;
        }
        List<String> errors = new ArrayList<String>();
        boolean addFile = request.getString(ARG_IMPORT_HANDLE,
                                            "").equals("file");
        boolean uncompress    = request.get(ARG_IMPORT_UNCOMPRESS, false);
        boolean addProvenance = request.get(ARG_IMPORT_PROVENANCE, false);
        if ( !getActionManager().getActionOk(actionId)) {
            return false;
        }

        String html = Utils.readUrl(url.toString());
        final List<HtmlUtils.Link> links = HU.extractLinks(url, html,
                                               pattern);
        List<HtmlUtils.Link> pageLinks = null;
        if (recursePattern != null) {
            pageLinks = HU.extractLinks(url, html, recursePattern);
            for (HtmlUtils.Link link : pageLinks) {
                if (links.contains(link)) {
                    continue;

                }
                if ( !getActionManager().getActionOk(actionId)) {
                    return false;
                }
                String name = link.getLabel().trim();
                name = name.replaceAll("/$", "").replaceAll("^/", "");
                Entry child = getEntryManager().findEntryWithName(request,
                                  parentEntry, name);
                if (child == null) {
                    child = getEntryManager().makeEntry(
                        request, new Resource(), parentEntry, name, "",
                        request.getUser(),
                        getRepository().getTypeHandler(
                            TypeHandler.TYPE_GROUP), null);
                    getEntryManager().addNewEntry(request, child);
                }
                sb.append("<li>");
                sb.append(
                    HU.href(
                        request.entryUrl(
                            getRepository().URL_ENTRY_SHOW,
                            child), child.getName()));
                sb.append("<ul>");
                boolean ok = importHtmlInner(request, actionId, cnt,depth - 1,
                                             sb, link.getUrl(),
					     okLinks,child,
                                             recursePattern, pattern);
                sb.append("</ul>");
                getActionManager().setActionMessage(actionId,
                        "<h2>Imported entries</h2>" + sb.toString());
                if ( !ok) {
                    return false;
                }
            }
        }

        for (HtmlUtils.Link link : links) {
	    if(okLinks.size()>0) {
		if(!okLinks.contains(link.getUrl().toString())) {
		    continue;
		}
	    }
	    

            if ((pageLinks != null) && pageLinks.contains(link)) {
                continue;
            }
            Resource    resource    = null;
            TypeHandler typeHandler = getRepository().getTypeHandler(request);
            String      name        = link.getLabel();
            if (request.get("useurl", false) || (name.length() < 4)) {
                name = IOUtil.stripExtension(
                    IOUtil.getFileTail(link.getUrl().toString()));
                name = Utils.makeLabel(name);
                name = name.replaceAll("^[0-9]+", "").trim();
            }

            //TODO: check if we have a entry already
            Entry existing = getEntryManager().findEntryWithName(request,
                                 parentEntry, name);
            if (existing == null) {
                String tmp = IOUtil.stripExtension(name);
                existing = getEntryManager().findEntryWithName(request,
                        parentEntry, tmp);
            }
            if (existing != null) {
                sb.append("<li> ");
                sb.append(msgLabel("Entry already exists"));
                sb.append(" ");
                sb.append(link.getUrl());
                sb.append("\n");
                continue;
            }


            try {
                if (addFile) {
                    File tmpFile =
                        getStorageManager().getTmpFile(request,
                            IOUtil.getFileTail(link.getUrl().toString()));
                    FileOutputStream fos = new FileOutputStream(tmpFile);
                    if (IOUtil.writeTo(
                            IO.getInputStream(link.getUrl().toString()),
                            fos) == 0) {
                        errors.add("Failed to read url:" + link.getUrl());
                        IOUtil.close(fos);

                        continue;
                    }
                    IOUtil.close(fos);

                    if (request.get(ARG_IMPORT_UNCOMPRESS, false)) {
                        tmpFile =
                            getStorageManager().uncompressIfNeeded(request,
                                tmpFile);
                        if (tmpFile == null) {
                            errors.add("Failed to uncompress file:"
                                       + tmpFile);

                            continue;
                        }
                        name = RepositoryUtil.getFileTail(tmpFile.getName());
                        //                                System.err.println("NAME:" + name);
                    }
                    tmpFile = getStorageManager().moveToStorage(request,
                            tmpFile);
                    if ((typeHandler == null)
                            || typeHandler.getType().equals(
                                TypeHandler.TYPE_FINDMATCH)) {
                        typeHandler =
                            getEntryManager().findDefaultTypeHandler(
                                tmpFile.toString());
                    }
                    resource = new Resource(tmpFile,
                                            Resource.TYPE_STOREDFILE);
                } else {
                    resource = new Resource(link.getUrl().toString(),
                                            Resource.TYPE_URL);
                    if ((typeHandler == null)
                            || typeHandler.getType().equals(
                                TypeHandler.TYPE_FINDMATCH)) {
                        typeHandler =
                            getEntryManager().findDefaultTypeHandler(
                                link.getUrl().toString());
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
                                         false, link.getUrl().toString(),
                                         "RAMADDA entry import", null, null,
                                         null));
                }


                entry.getTypeHandler().initializeEntryFromHarvester(request,
                        entry, true);
                getEntryManager().addNewEntry(request, entry);


                sb.append("<li> ");
                sb.append(
                    HU.href(
                        request.entryUrl(
                            getRepository().URL_ENTRY_SHOW,
                            entry), entry.getName()));
                sb.append(HU.br());

            } catch (Exception exc) {
                sb.append("<li> ");
                sb.append(msgLabel("Error fetching URL"));
                sb.append(" ");
                sb.append(link.getUrl());
                sb.append("\n");
                sb.append("" + exc);
            }
	    String message =   "Processed: " + cnt[0]+"/" + links.size() +" links<br>";
	    message+=sb;
            getActionManager().setActionMessage(actionId,
						message);
	}
        if (errors.size() > 0) {
            sb.append(
                getPageHandler().showDialogError(
                    StringUtil.join("<br>", errors)));

            return false;
        }

        return true;

    }

    private Result importHtml(final Request request,
                              final Repository repository, URL url,
			      final List<String> okLinks,
                              final Entry parentEntry,
                              final String recursePattern,
                              final String pattern)
            throws Exception {
        //IMPORTANT!
        getAuthManager().ensureAuthToken(request);
        ActionManager.Action action = new ActionManager.Action() {
            public void run(Object actionId) throws Exception {
		int []cnt={0};
                StringBuilder sb    = new StringBuilder("");
                int           depth = request.get(ARG_IMPORT_RECURSE_DEPTH,
                                          1);
                sb.append("<ul>");
                importHtmlInner(request, actionId, cnt,depth, sb, url,okLinks,
                                parentEntry, recursePattern, pattern);
                sb.append("</ul>");
                getActionManager().setActionMessage(actionId, sb.toString());
                getActionManager().setContinueHtml(actionId, sb.toString());
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
        StringBuilder sb = new StringBuilder();
	List  <String> okLinks = (List<String>) request.get("linkok",new ArrayList<String>());
	boolean anySelected = okLinks.size()>0;


        getPageHandler().entrySectionOpen(request, parentEntry, sb, "HTML Import");


        String  pattern = request.getString(ARG_IMPORT_PATTERN, "");

        boolean doit    = request.exists(ARG_IMPORT_DOIT);
        String recursePattern = request.getString(ARG_IMPORT_RECURSE_PATTERN,
                                    "");
        boolean              recurse = request.get(ARG_IMPORT_RECURSE, false);
        List<HtmlUtils.Link> pageLinks = null;
        List<HtmlUtils.Link> links     = null;
        URL                  rootUrl   = new URL(url);
        if (doit) {
            return importHtml(request, repository, rootUrl, okLinks,
			      parentEntry,
                              recurse
                              ? recursePattern
                              : null, pattern);
        }
        if (recurse) {
            pageLinks = HU.extractLinks(rootUrl, recursePattern);
            if ((pageLinks.size() > 0) && !doit) {
                links = HU.extractLinks(pageLinks.get(0).getUrl(),
                        pattern);
            }
        } else {
            links = HU.extractLinks(rootUrl, pattern);
        }

        TypeHandler typeHandler = getRepository().getTypeHandler(request.getString(ARG_TYPE,TypeHandler.TYPE_FINDMATCH),false);


        String buttons =
            HU.buttons(HU.submit("Test it out"),
                              HU.submit("Create entries",
                                  ARG_IMPORT_DOIT));
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



        HU.formEntry(sb, "",
		     HU.labeledCheckbox(ARG_IMPORT_RECURSE,
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


	HU.formEntry(sb, "",HU.div("Link regular expression - e.g. \".*\\.pdf\"",HU.cssClass("ramadda-form-help")));

	HU.formEntry(sb, msgLabel("Entry Pattern"),
		     HU.input(ARG_IMPORT_PATTERN, pattern, HU.attrs("size","50","placeholder","e.g. .*\\.pdf")));


        boolean addFile = request.getString(ARG_IMPORT_HANDLE,
                                            "file").equals("file");
	HU.formEntry(sb,
		     msgLabel("What to do"),
		     HU.labeledRadio(ARG_IMPORT_HANDLE, "file", addFile,
				     msg("Download the file"))
		     + HU.space(3)
		     + HU.labeledRadio(ARG_IMPORT_HANDLE, "url", !addFile,
				       msg("Add the link")));


	HU.formEntry(sb,"",
                HU.labeledCheckbox(ARG_IMPORT_UNCOMPRESS, "true",
				   request.get(ARG_IMPORT_UNCOMPRESS, false),
				   msg("Uncompress file")));

        HU.formEntry(sb,"",
		     HU.labeledCheckbox("useurl", "true",
					request.get("useurl",false),
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
	mtdSb.append(HU.labeledCheckbox(ARG_INDEX_IMAGE, "true", false,"Extract text from images"));
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
	//        HU.formEntry(sb,"", buttons);


        sb.append(HU.p());

        if (pageLinks != null) {
            if (pageLinks.size() > 0) {
                sb.append(msgHeader("Page Imports"));
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
                    sb.append(HU.br());
                }
                sb.append("</ul>");
            } else {
                sb.append(
                    getPageHandler().showDialogNote(
                        "No recursed pages found. Maybe add \".*\" before and after pattern"));
            }

        }

        if (links != null) {
            if (links.size() > 0) {
                if ((pageLinks != null) && (pageLinks.size() > 0)) {
                    sb.append(
                        msgHeader(
                            "Example links import for recursed page: "
                            + pageLinks.get(0).getHref(true)));
                } else {
                    sb.append(msgHeader("Links Import"));
                }
                sb.append("<ul>");
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
        }
        sb.append(HU.formClose());

        getPageHandler().entrySectionClose(request, parentEntry, sb);

        return getEntryManager().makeEntryEditResult(request, parentEntry,
                "", sb);
        //        return getEntryManager().addEntryHeader(request, parentEntry, new Result("",sb));
    }

    public static void test(String file) throws Exception {
        String html = IOUtil.readContents(file, HtmlUtils.class);
        //        String pattern = "(?i)<\\s*a href\\s*=\\s*\"?([^\">]+)\"?>(.+?)</a>";

        String pattern =
            "(?i)<\\s*a href\\s*=\\s*(\"|')?([^\"'>]+)(\"|')?[^>]*>(.+)</a>";
        Matcher matcher = Pattern.compile(pattern).matcher(html);
        while (matcher.find()) {
            String href = matcher.group(1);
            //            String label = matcher.group(2);
            System.err.println(href);
        }
    }



}
