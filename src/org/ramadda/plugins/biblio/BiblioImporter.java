/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.biblio;


import org.ramadda.repository.*;
import org.ramadda.util.Utils;


import org.w3c.dom.*;

import ucar.unidata.util.DateUtil;

import ucar.unidata.util.IOUtil;


import ucar.unidata.util.StringUtil;

import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;

import java.util.List;


/**
 * Class description
 *
 *
 * @version        $version$, Thu, Nov 25, '10
 * @author         Enter your name here...
 */
public class BiblioImporter extends ImportHandler implements BiblioConstants {

    /** _more_ */
    public static final String TYPE_BIBLIO = "biblio";


    /**
     * ctor
     *
     * @param repository _more_
     */
    public BiblioImporter(Repository repository) {
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
        importTypes.add(new TwoFacedObject("Bibliography bibtex format",
                                           TYPE_BIBLIO));
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param repository _more_
     * @param uploadedFile _more_
     * @param parentEntry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public Result handleRequest(Request request, Repository repository,
                                String uploadedFile, Entry parentEntry)
            throws Exception {
        if ( !request.getString(ARG_IMPORT_TYPE, "").equals(TYPE_BIBLIO)) {
            return null;
        }
        List<Entry> entries    = null;
        List<File>  files      = new ArrayList<File>();
        String      biblioText = null;
        if (uploadedFile.endsWith(".zip")) {
            List<File> unzippedFiles =
                getStorageManager().unpackZipfile(request, uploadedFile);
            for (File f : unzippedFiles) {
                if (f.getName().endsWith(".txt")) {
                    biblioText = new String(
                        IOUtil.readBytes(
                            getStorageManager().getFileInputStream(f)));
                    //                    break;
                } else {
                    files.add(f);
                    //                    System.err.println ("FILE:" + getStorageManager().getOriginalFilename(f.getName()));
                }
            }
        } else {
            biblioText = new String(
                IOUtil.readBytes(
                    getStorageManager().getFileInputStream(uploadedFile)));
        }

        StringBuffer sb = new StringBuffer();
        if (biblioText == null) {
            sb.append(
                getPageHandler().showDialogError(
                    msg("No biblio '.txt' file provided")));

            return getEntryManager().addEntryHeader(request, parentEntry,
                    new Result("", sb));
        }

	getPageHandler().entrySectionOpen(request, parentEntry, sb, "Bibliography Import");
        entries = process(request, parentEntry, biblioText, files,
                          sb);

        for (Entry entry : entries) {
            entry.setUser(request.getUser());
        }
        getEntryManager().addNewEntries(request, entries);
        boolean didone = false;
        for (Entry entry : entries) {
            if (entry.isFile()) {
                if ( !didone) {
                    sb.append(msgHeader("Imported entries with files"));
                    sb.append("<ul>");
                    didone = true;
                }
                sb.append("<li> ");
                sb.append(getPageHandler().getBreadCrumbs(request, entry,
                        parentEntry));
            }
        }

        if (didone) {
            sb.append("</ul>");
        }
        didone = false;
        for (Entry entry : entries) {
            if ( !entry.isFile()) {
                if ( !didone) {
                    sb.append(msgHeader("Imported entries without files"));
                    sb.append("<ul>");
                    didone = true;
                }
                sb.append("<li> ");
                sb.append(getPageHandler().getBreadCrumbs(request, entry,
                        parentEntry));
            }
        }
        if (didone) {
            sb.append("</ul>");
        }
        if (files.size() > 0) {
            sb.append("<ul>");
            sb.append(msgHeader("Files without entries"));
            for (File f : files) {
                sb.append("<li> ");
                sb.append(
                    getStorageManager().getOriginalFilename(f.getName()));
            }
            sb.append("</ul>");
        }

        sb.append("</ul> ");
	getPageHandler().entrySectionClose(request, parentEntry, sb);
	getEntryManager().parentageChanged(parentEntry,true);
        getSearchManager().entriesCreated(request, entries);	
        return getEntryManager().addEntryHeader(request, parentEntry,
                new Result("", sb));
    }



    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Aug 23, '13
     * @author         Enter your name here...
     */
    private static class EntryInfo {

        /** _more_ */
        Entry entry;

        /** _more_ */
        String author;

        /** _more_ */
        String file;

        /**
         * _more_
         *
         * @param entry _more_
         * @param author _more_
         * @param file _more_
         */
        public EntryInfo(Entry entry, String author, String file) {
            this.entry  = entry;
            this.author = author;
            this.file   = file;
        }
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param parentId _more_
     * @param s _more_
     * @param files _more_
     * @param sb _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private List<Entry> process(Request request, Entry parentEntry, String s,
                                List<File> files, StringBuffer sb)
            throws Exception {

        boolean         inKeyword          = false;
        List<EntryInfo> entryInfos         = new ArrayList<EntryInfo>();
        List<String>    keywords           = new ArrayList<String>();
        List<String>    authors            = new ArrayList<String>();
        Entry           entry              = null;
        Object[]        values             = new Object[10];
        String          filenameFromBiblio = null;
        List<String>    lines = StringUtil.split(s, "\n", true, false);
        //Add a dummy line so we pick up the last biblio entry in the loop
        lines.add("%0 dummy");
        boolean lastLineBlank = true;
        for (String line : lines) {
            if (line.trim().length() == 0) {
                lastLineBlank = true;

                continue;
            }

            List<String> toks = StringUtil.splitUpTo(line, " ", 2);
            if (toks.get(0).trim().startsWith("%") && (toks.size() == 2)) {
                String tag   = toks.get(0);
                String value = toks.get(1);
                value = value.replaceAll("[^ -~]", "-");
                value = value.trim();
                if (lastLineBlank || (entry == null)) {
                    if (entry != null) {
                        values[IDX_OTHER_AUTHORS] = StringUtil.join("\n",
                                authors);
                        for (int idx = 0; idx < TAGS.length; idx++) {
                            if (values[idx] == null) {
                                values[idx] = "";
                            }
                        }
                        entry.setValues(values);
                        String nameToMatch =
                            ("" + values[IDX_PRIMARY_AUTHOR]).trim();
                        if (nameToMatch.length() > 0) {
                            nameToMatch = StringUtil.splitUpTo(nameToMatch,
                                    ",", 2).get(0);
                        }

                        if (nameToMatch.indexOf(" ") >= 4) {
                            nameToMatch = StringUtil.splitUpTo(nameToMatch,
                                    " ", 2).get(0);
                        }
                        nameToMatch = nameToMatch.toLowerCase().trim();
                        nameToMatch = nameToMatch.replaceAll(" ", "");
                        nameToMatch =
                            nameToMatch.replaceAll("[^a-zA-Z0-9_]+", "");
                        entryInfos.add(new EntryInfo(entry, nameToMatch,
                                filenameFromBiblio));
                    }
                    keywords           = new ArrayList<String>();
                    authors            = new ArrayList<String>();
                    filenameFromBiblio = null;
                    entry = getRepository().getTypeHandler(
                        TYPE_BIBLIO).createEntry(getRepository().getGUID());
		    entry.setCreateDate(new Date().getTime());
                    entry.setParentEntry(parentEntry);
                    values = entry.getTypeHandler().getEntryValues(entry);
                }
                lastLineBlank = false;

                if (tag.equals(TAG_BIBLIO_TYPE)) {
                    values[IDX_TYPE] = value;

                    continue;
                }

                if (tag.equals(TAG_BIBLIO_AUTHOR)) {
                    if ( !Utils.stringDefined(
                            (String) values[IDX_PRIMARY_AUTHOR])) {
                        values[IDX_PRIMARY_AUTHOR] = value;
                    } else {
                        authors.add(value);
                    }

                    continue;
                }

                if (tag.equals(TAG_BIBLIO_TITLE)) {
                    if (value.length() > Entry.MAX_NAME_LENGTH) {
                        value = value.substring(0, Entry.MAX_NAME_LENGTH - 1);
                    }
                    entry.setName(value);

                    continue;
                }
                if (tag.equals(TAG_BIBLIO_DESCRIPTION)) {
                    entry.setDescription(value);

                    continue;
                }

                if (tag.equals(TAG_BIBLIO_URL)) {
                    entry.setResource(new Resource(value, Resource.TYPE_URL));

                    continue;
                }

                if (tag.equals(TAG_BIBLIO_DATE)) {
                    Date date = Utils.parseDate(value);
                    entry.setStartDate(date.getTime());
                    entry.setEndDate(date.getTime());

                    continue;
                }

                if (tag.equals(TAG_BIBLIO_KEYWORD)) {
                    inKeyword = true;
                    keywords.add(value);

                    continue;
                }
                if (tag.equals(TAG_BIBLIO_EXTRA)) {
                    if (value.toLowerCase().startsWith("file:")) {
                        filenameFromBiblio = new File(
                            value.substring(
                                "file:".length()).trim()).getName();
                        //                        System.err.println("FILE:" + filenameFromBiblio);
                    } else {
                        //                        System.err.println ("UNK:" + value);
                    }

                    continue;
                }

                boolean gotone = false;
                for (int idx = 0; (idx < TAGS.length) && !gotone; idx++) {
                    if (tag.equals(TAGS[idx])) {
                        values[INDICES[idx]] = value;
                        gotone               = true;
                    }
                }

                if (gotone) {
                    continue;
                }
            } else if (inKeyword) {
                keywords.add(line);

                continue;
            } else {
                System.err.println("LINE:" + line + " toks:" + toks.size()
                                   + " " + toks);
            }
        }

        List<Entry> entries = new ArrayList<Entry>();
        for (EntryInfo entryInfo : entryInfos) {
            entries.add(entryInfo.entry);
        }

        for (EntryInfo entryInfo : entryInfos) {
            File theFile = null;
            entry = entryInfo.entry;
            if (entryInfo.file != null) {
                for (File f : files) {
                    String filename = getStorageManager().getOriginalFilename(
                                          f.getName()).toLowerCase().trim();
                    if (entryInfo.file.equalsIgnoreCase(filename)) {
                        System.err.println("GOT IT:" + entryInfo.file);
                        theFile = f;

                        break;
                    }
                }
                if (theFile != null) {
                    files.remove(theFile);
                    String targetName =
                        getStorageManager().getOriginalFilename(
                            theFile.toString());
                    targetName =
                        getStorageManager().getStorageFileName(targetName);
                    theFile = getStorageManager().moveToStorage(request,
                            theFile, targetName);
                    entry.setResource(new Resource(theFile,
                            Resource.TYPE_STOREDFILE));
                }
            }
        }

        for (int attempt = 1; attempt <= 2; attempt++) {
            for (EntryInfo entryInfo : entryInfos) {
                File theFile = null;
                entry = entryInfo.entry;
                if (entry.isFile()) {
                    continue;
                }
                for (File f : files) {
                    String filename = getStorageManager().getOriginalFilename(
                                          f.getName()).toLowerCase().trim();
                    filename = filename.replaceAll("[^a-zA-Z0-9_.]+", "");
                    if (entryInfo.author.length() > 0) {
                        boolean matches = false;
                        if (entryInfo.author.length() <= 2) {
                            matches = filename.startsWith(entryInfo.author
                                    + "_");
                        } else {
                            matches = filename.startsWith(entryInfo.author);
                        }
                        if (matches) {
                            if (attempt == 1) {
                                String yearPattern =
                                    StringUtil.findPattern(filename,
                                        "(19\\d{2})");
                                if (yearPattern == null) {
                                    yearPattern =
                                        StringUtil.findPattern(filename,
                                            "(20\\d{2})");
                                }

                                if ((yearPattern != null)
                                        && (entry.getStartDate()
                                            != entry.getCreateDate())) {
                                    GregorianCalendar cal =
                                        new GregorianCalendar();
                                    cal.setTime(
                                        new Date(entry.getStartDate()));
                                    if ( !yearPattern.equals(""
                                            + cal.get(cal.YEAR))) {
                                        System.out.println(
                                            "skipping year:" + yearPattern
                                            + " cal:" + cal.get(cal.YEAR)
                                            + "   " + entry.getName() + " "
                                            + filename);

                                        continue;
                                    }
                                }
                            }
                            theFile = f;
                            System.out.println("from author:"
                                    + entryInfo.author + " entry:"
                                    + entry.getName() + " file:" + filename);

                            break;
                        }
                    }
                    if (filename.startsWith(entry.getName().toLowerCase())) {
                        theFile = f;

                        break;
                    }
                }

                if (theFile != null) {
                    files.remove(theFile);
                    String targetName =
                        getStorageManager().getOriginalFilename(
                            theFile.toString());
                    System.out.println("found: " + entry.getName() + " file="
                                       + targetName + " author="
                                       + entryInfo.author);
                    targetName =
                        getStorageManager().getStorageFileName(targetName);

                    theFile = getStorageManager().moveToStorage(request,
                            theFile, targetName);
                    entry.setResource(new Resource(theFile,
                            Resource.TYPE_STOREDFILE));
                } else {
                    if (attempt == 2) {
			//                        System.out.println("not found: " + entry.getName() + " name=" + entryInfo.author);
                    }
                }
            }
        }

        return entries;

    }

}
