/**
Copyright (c) 2008-2023 Geode Systems LLC
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


/**
 */
public class HtmlImportHandler extends ImportHandler {

    /** _more_ */
    public static final String ARG_IMPORT_PATTERN = "import.pattern";

    /** _more_ */
    public static final String ARG_IMPORT_RECURSE = "import.recurse";

    /** _more_ */
    public static final String ARG_IMPORT_RECURSE_PATTERN =
        "import.recurse.pattern";

    /** _more_ */
    public static final String ARG_IMPORT_RECURSE_DEPTH =
        "import.recurse.depth";

    /** _more_ */
    public static final String ARG_IMPORT_DOIT = "import.doit";

    /** _more_ */
    public static final String ARG_IMPORT_PROVENANCE = "import.addprovenance";

    /** _more_ */
    public static final String ARG_IMPORT_UNCOMPRESS = "import.uncompress";

    /** _more_ */
    public static final String ARG_IMPORT_HANDLE = "import.handle";

    /** _more_ */
    public static final String TYPE_HTML = "html";

    /**
     * _more_
     */
    public HtmlImportHandler() {
        super(null);
    }

    /**
     * _more_
     *
     * @param repository _more_
     */
    public HtmlImportHandler(Repository repository) {
        super(repository);
    }

    /**
     * _more_
     *
     * @param importTypes _more_
     * @param formBuffer _more_
     */
    @Override
    public void addImportTypes(List<TwoFacedObject> importTypes,
                               Appendable formBuffer) {
        super.addImportTypes(importTypes, formBuffer);
        importTypes.add(new TwoFacedObject("Links in an HTML Page",
                                           TYPE_HTML));
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param actionId _more_
     * @param depth _more_
     * @param sb _more_
     * @param url _more_
     * @param parentEntry _more_
     * @param recursePattern _more_
     * @param pattern _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private boolean importHtmlInner(Request request, Object actionId,
                                    int depth, StringBuilder sb, URL url,
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
                boolean ok = importHtmlInner(request, actionId, depth - 1,
                                             sb, link.getUrl(), child,
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

                Entry entry = getEntryManager().makeEntry(request, resource,
                                  parentEntry, name, "", request.getUser(),
                                  typeHandler, null);

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
            getActionManager().setActionMessage(actionId,
                    "<h2>Imported entries</h2>" + sb.toString());
        }
        if (errors.size() > 0) {
            sb.append(
                getPageHandler().showDialogError(
                    StringUtil.join("<br>", errors)));

            return false;
        }

        return true;

    }



    /**
     * _more_
     *
     * @param request _more_
     * @param repository _more_
     * @param url _more_
     * @param parentEntry _more_
     * @param recursePattern _more_
     * @param pattern _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Result importHtml(final Request request,
                              final Repository repository, URL url,
                              final Entry parentEntry,
                              final String recursePattern,
                              final String pattern)
            throws Exception {
        //IMPORTANT!
        getAuthManager().ensureAuthToken(request);
        ActionManager.Action action = new ActionManager.Action() {
            public void run(Object actionId) throws Exception {
                StringBuilder sb    = new StringBuilder("");
                int           depth = request.get(ARG_IMPORT_RECURSE_DEPTH,
                                          1);
                sb.append("<ul>");
                importHtmlInner(request, actionId, depth, sb, url,
                                parentEntry, recursePattern, pattern);
                sb.append("</ul>");
                getActionManager().setActionMessage(actionId, sb.toString());
                getActionManager().setContinueHtml(actionId, sb.toString());
            }
        };

        return getActionManager().doAction(request, action, "Importing HTML",
                                           "", parentEntry);
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param repository _more_
     * @param url _more_
     * @param parentEntry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result handleUrlRequest(Request request, Repository repository,
                                   String url, Entry parentEntry)
            throws Exception {


        if ( !request.getString(ARG_IMPORT_TYPE, "").equals(TYPE_HTML)) {
            return null;
        }
        StringBuilder sb = new StringBuilder();

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
            return importHtml(request, repository, rootUrl, parentEntry,
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

        TypeHandler typeHandler = getRepository().getTypeHandler(request);


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

        HU.formEntry(sb,msgLabel("Recurse Pattern"),
		     HU.input(ARG_IMPORT_RECURSE_PATTERN, recursePattern,
			      HU.SIZE_50) + " "
		     + "Regular expression pattern to match on links to other pages.");

	HU.formEntry(sb, msgLabel("Recurse Depth"),
		     HU.input(ARG_IMPORT_RECURSE_DEPTH,
			      request.getString(ARG_IMPORT_RECURSE_DEPTH, "1"),
			      HU.SIZE_5));


	HU.formEntry(sb, msgLabel("Entry Pattern"),
		     HU.input(ARG_IMPORT_PATTERN, pattern, HU.SIZE_50) + " "
		     + msg("regular expression - add .*"));

        boolean addFile = request.getString(ARG_IMPORT_HANDLE,
                                            "").equals("file");
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



	HU.formEntry(sb, msgLabel("Entry type"),
		     getPageHandler().makeFileTypeSelector(request, typeHandler, true));

        sb.append(HU.formTableClose());
	StringBuilder mtdSb = new StringBuilder();
	mtdSb.append(HU.labeledCheckbox(ARG_IMPORT_PROVENANCE, "true",
					request.get(ARG_IMPORT_PROVENANCE, false),
					"Add the source URL as provenance metadata"));

	mtdSb.append("<br>");

	String extract = getLLMManager().getNewEntryExtract(request);
	if(stringDefined(extract)) {
	    mtdSb.append(HU.b("Extract metadata using GPT") +":<br>");
	    mtdSb.append(extract);
	    sb.append(HU.makeShowHideBlock("Metadata extraction",mtdSb.toString(),false));
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
                    sb.append(link.getHref());
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
                            + pageLinks.get(0).getHref()));
                } else {
                    sb.append(msgHeader("Links Import"));
                }
                sb.append("<ul>");
                for (HtmlUtils.Link link : links) {
                    sb.append("<li> ");
                    sb.append(link.getHref());
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
                        "No links found. Maybe add \".*\" before and after pattern"));
            }
        }
        sb.append(HU.formClose());

        getPageHandler().entrySectionClose(request, parentEntry, sb);

        return getEntryManager().makeEntryEditResult(request, parentEntry,
                "", sb);
        //        return getEntryManager().addEntryHeader(request, parentEntry, new Result("",sb));
    }


    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        String url =
            "https://www.aoncadis.org/download/fileDownload.htm?logicalFileId=71073a32-dbb1-11e3-85a2-00c0f03d5b7c";
        url = args[0];
        url = "ftp://n5eil01u.ecs.nsidc.org/SAN2/ICEBRIDGE/ILATM1B.002/2013.03.21";
        System.out.println(IOUtil.readContents(url, HtmlUtils.class));
        //        List<HU.Link> links = HU.extractLinks(new URL(url), args.length>1?args[1]:null);
        //        System.err.println ("Links:"  + links);
    }


    /**
     * _more_
     *
     * @param file _more_
     *
     * @throws Exception _more_
     */
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
