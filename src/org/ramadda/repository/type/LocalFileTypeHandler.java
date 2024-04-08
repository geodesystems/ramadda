/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.type;


import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.util.SelectInfo;
import org.ramadda.util.IO;
import org.ramadda.util.Utils;

import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.StringUtil;

import java.io.File;
import java.io.FilenameFilter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * Class TypeHandler _more_
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
public class LocalFileTypeHandler extends ExtensibleGroupTypeHandler {

    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public LocalFileTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }


    @Override
    public void initializeEntryFromForm(Request request, Entry entry,
                                        Entry parent, boolean newEntry)
            throws Exception {
	super.initializeEntryFromForm(request, entry, parent, newEntry);
	getEntryManager().clearCache();
    }

    /**
       Override to use the entry type select menu
    */
    @Override
    public String getFormWidget(Request request, Entry entry, Column column,
                                String widget)
            throws Exception {
	if(column.getName().equals("directory_type")) {
	    String selected = "";
	    if(entry!=null) selected = (String) entry.getValue(LocalFileInfo.COL_DIRECTORY_TYPE);
	    String select =   getRepository().makeTypeSelect(null, request,column.getEditArg(),
							     null,true,selected,true,null, true);
	    return select;
	}
	return super.getFormWidget(request, entry,column, widget);
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     */
    public boolean canBeCreatedBy(Request request) {
        return request.getUser().getAdmin();
    }

    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public String getEntryIconUrl(Request request, Entry entry)
            throws Exception {
        if (entry.isGroup()) {
            return getIconUrl(ICON_SYNTH_FILE);
        }

        return super.getIconUrl(request, entry);
    }



    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public boolean isSynthType() {
        return true;
    }

    /**
     * _more_
     *
     * @param id _more_
     * @param baseFile _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public File getFileFromId(String id, File baseFile) throws Exception {
        boolean debug = false;
        if (debug) {
            System.err.println("getFileFromId:" + id + " base:" + baseFile);
        }
        if ((id == null) || (id.length() == 0)) {
            if (debug) {
                System.err.println("returning baseFile");
            }

            return baseFile;
        }
        String subPath = new String(Utils.decodeBase64(id));
        if (debug) {
            System.err.println("subpath:" + subPath);
        }
        File file = new File(IOUtil.joinDir(baseFile, subPath));

        if ( !file.exists()) {
            file = new File(IOUtil.joinDir(baseFile, id));
            if (debug) {
                System.err.println("trying:" + file);
            }
        }


        if ( !IO.isADescendentNonCanonical(baseFile, file)) {
            if (debug) {
                System.err.println("File:" + file
                                   + " is not a descendent of base dir:"
                                   + baseFile);
            }

            throw new IllegalArgumentException("Bad file path:" + subPath);
        }

        getStorageManager().checkLocalFile(file);

        return file;
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param select _more_
     * @param mainEntry _more_
     * @param parentEntry _more_
     * @param synthId _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public List<String> getSynthIds(Request request, SelectInfo select,
                                    Entry mainEntry, Entry parentEntry,
                                    String synthId)
            throws Exception {

        boolean debug = false;
        if (debug) {
            System.err.println("LocalFileTypeHandler.getSynthIds: entry:"
                               + mainEntry.getName() + " "
                               + mainEntry.getId() + " synthid:" + synthId);
        }
        //      System.err.println (Utils.getStack(10));

        List<String>  ids           = new ArrayList<String>();
        LocalFileInfo localFileInfo = doMakeLocalFileInfo(mainEntry);
        if ( !localFileInfo.isDefined()) {
            if (debug) {
                System.err.println("\tlocalFileInfo not defined");
            }

            return ids;
        }

        int    max         = select.getMax();
        int    skip        = select.getSkip();
        long   t1          = System.currentTimeMillis();

	File rootDir = localFileInfo.getRootDir();
	if(rootDir==null) return ids;
        String rootDirPath = rootDir.toString();
        File   childPath = getFileFromId(synthId, localFileInfo.getRootDir());
        //        System.err.println ("synthId:" + synthId);
        if (debug) {
            System.err.println("\tchild path:" + childPath + " max:" + max
                               + " skip:" + skip);
        }

        if ( !childPath.exists()) {
            getLogManager().logWarning(
                "Server side files:  file does not exist:" + childPath);

            if (debug) {
                System.err.println("\tchild path does not exist");
            }

            return new ArrayList<String>();
        }

        File[] files = childPath.listFiles();
        if (files == null) {
            getLogManager().logWarning(
                "Server side files:  got a null file listing for:"
                + childPath);

            if (debug) {
                System.err.println("\tnull file listing");
            }

            return new ArrayList<String>();
        }
        boolean descending = !select.getAscending();
        String  by         = select.getOrderBy();
        if (by.equals(ORDERBY_NAME)) {
            files = IO.sortFilesOnName(files, descending);
        } else if (by.equals(ORDERBY_SIZE)) {
            files = Utils.sortFilesOnSize(files, descending);
        } else if (by.equals(ORDERBY_NUMBER)) {
            files = IO.sortFilesOnNumber(files, descending);
        } else if (by.equals(ORDERBY_MIXED)) {
            List<File> filesByDate = new ArrayList<File>();
            List<File> filesByName = new ArrayList<File>();
            for (File f : files) {
                String name = f.getName();
                if (name.matches(
                        ".*(\\d\\d\\d\\d\\d\\d|\\d\\d\\d\\d_\\d\\d).*")) {
                    filesByDate.add(f);
                } else {
                    filesByName.add(f);
                }
            }

            File[] byDate = IOUtil.sortFilesOnAge(toArray(filesByDate),
                                descending);
            File[] byName = IOUtil.sortFilesOnAge(toArray(filesByName),
                                descending);
            int cnt = 0;
            for (int i = 0; i < byName.length; i++) {
                files[cnt++] = byName[i];
            }
            for (int i = 0; i < byDate.length; i++) {
                files[cnt++] = byDate[i];
            }
        } else {
            files = IOUtil.sortFilesOnAge(files, descending);
        }

	
        List<String> includes = localFileInfo.getIncludes();
        List<String> excludes = localFileInfo.getExcludes();
        long         age = (long) (1000 * (localFileInfo.getAgeLimit() * 60));
        long         now      = System.currentTimeMillis();
        int          start    = Math.max(0,skip);
        int          cnt      = 0;
        for (int i = start; i < files.length; i++) {
            File childFile = files[i];
            if (childFile.isHidden()) {
                continue;
            }
            if ((age > 0) && (now - childFile.lastModified()) < age) {
                continue;
            }
            if ( !match(childFile, includes, true)) {
                continue;
            }
            if (match(childFile, excludes, false)) {
                continue;
            }
            ids.add(getSynthId(mainEntry, rootDirPath, childFile));
            cnt++;
            if (cnt >= max) {
                break;
            }
        }
        long t2 = System.currentTimeMillis();

        //        System.err.println ("Time:" + (t2-t1) + " ids:" + ids.size());
        //        System.err.println ("IDS:" + ids);

        return ids;



    }

    /**
     * _more_
     *
     * @param files _more_
     *
     * @return _more_
     */
    private static File[] toArray(List<File> files) {
        File[] a = new File[files.size()];
        for (int i = 0; i < a.length; i++) {
            a[i] = files.get(i);
        }

        return a;
    }







    /**
     * _more_
     *
     * @param pattern _more_
     *
     * @return _more_
     */
    private String getRegexp(String pattern) {
        if ( !pattern.startsWith("regexp:")) {
            pattern = StringUtil.wildcardToRegexp(pattern);
        } else {
            pattern = pattern.substring("regexp:".length());
        }

        return pattern;
    }

    /**
     * _more_
     *
     * @param file _more_
     * @param patterns _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    private boolean match(File file, List<String> patterns, boolean dflt) {
        String  value      = file.toString();
        boolean hadPattern = false;
        for (String pattern : patterns) {
            pattern = pattern.trim();
            if (pattern.startsWith("dir:")) {
                if (file.isDirectory()) {
                    hadPattern = true;
                    pattern =
                        getRegexp(pattern.substring("dir:".length()).trim());
                    if (StringUtil.stringMatch(value, pattern, true, false)) {
                        return true;
                    }
                }
            } else {
                if ( !file.isDirectory()) {
                    hadPattern = true;
                    if (StringUtil.stringMatch(value, getRegexp(pattern),
                            true, false)) {
                        return true;
                    }
                }
            }
        }
        if (hadPattern) {
            return false;
        }

        return dflt;
    }

    /**
     * _more_
     *
     * @param parentEntry _more_
     * @param rootDirPath _more_
     * @param childFile _more_
     *
     * @return _more_
     */
    public String getSynthId(Entry parentEntry, String rootDirPath,
                             File childFile) {
        String subId    = getFileComponentOfSynthId(rootDirPath, childFile);
        String parentId = parentEntry.getId();
        String prefix   = getPrefix(parentId);

        //        System.err.println("parentId:" + parentId +" prefix:" + prefix);
        return prefix + ":" + subId;
    }




    /**
     * _more_
     *
     * @param parentId _more_
     *
     * @return _more_
     */
    private String getPrefix(String parentId) {
        if (parentId.startsWith(Repository.ID_PREFIX_SYNTH)) {
            return parentId;
        }

        return Repository.ID_PREFIX_SYNTH + parentId;

    }

    /**
     * _more_
     *
     * @param rootDirPath _more_
     * @param childFile _more_
     *
     * @return _more_
     */
    private String getFileComponentOfSynthId(String rootDirPath,
                                             File childFile) {
        String subId = childFile.toString().substring(rootDirPath.length());
        subId = Utils.encodeBase64(subId).replace("\n", "");

        return subId;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param parentEntry _more_
     * @param id _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public Entry makeSynthEntry(Request request, Entry parentEntry, String id)
            throws Exception {

        LocalFileInfo localFileInfo = doMakeLocalFileInfo(parentEntry);
        if ( !localFileInfo.isDefined()) {
            //            System.err.println ("\tnnot defined");
            return null;
        }

        List<Metadata> metadataList =
            getMetadataManager().getMetadata(request,parentEntry);
        //Prune out the aliases
        if (metadataList != null) {
            List<Metadata> tmp = new ArrayList<Metadata>();
            for (Metadata metadata : metadataList) {
                if (metadata.getType().equals(
                        ContentMetadataHandler.TYPE_ALIAS)) {
                    continue;
                }
                tmp.add(metadata);
            }
            metadataList = tmp;
        }


        File targetFile = getFileFromId(id, localFileInfo.getRootDir());
        //        System.err.println ("\tntarget file:" + targetFile);

        if ( !targetFile.exists()) {
            //            System.err.println ("\tnnot exist");
            return null;
        }

        long t1 = System.currentTimeMillis();
        //TODO: Check the time since last change here

        if ( !match(targetFile, localFileInfo.getIncludes(), true)) {
            throw new IllegalArgumentException("File cannot be accessed");
        }
        if (match(targetFile, localFileInfo.getExcludes(), false)) {
            throw new IllegalArgumentException("File cannot be accessed");
        }

        String synthId;
        if (id.startsWith(Repository.ID_PREFIX_SYNTH)) {
            synthId = id;
        } else {
            synthId = parentEntry.getId() + ":" + id;
            if ( !synthId.startsWith(Repository.ID_PREFIX_SYNTH)) {
                synthId = Repository.ID_PREFIX_SYNTH + synthId;
            }

        }
        //        System.err.println("*** synth id:" + synthId);

        TypeHandler handler;
	if(targetFile.isDirectory()) {
	    String directoryType = localFileInfo.getDirectoryType();
	    if(!stringDefined(directoryType))
		directoryType = TypeHandler.TYPE_GROUP;
	    handler = getRepository().getTypeHandler(directoryType);
	} else {
	    handler = getEntryManager().findDefaultTypeHandler(targetFile.toString());
	    if(handler==null)
		handler = getRepository().getTypeHandler(TypeHandler.TYPE_FILE);
	}


        Entry templateEntry = getEntryManager().getTemplateEntry(targetFile,
								 null,null);
        Entry entry = null;


        if (templateEntry != null) {
            entry = templateEntry;
            entry.setId(synthId);
        }


        if (entry == null) {
            entry = (targetFile.isDirectory()
                     ? (Entry) new Entry(synthId, handler, true)
                     : new Entry(synthId, handler));
            if (targetFile.isDirectory()) {
                entry.setIcon(ICON_SYNTH_FILE);
            }
        }



        String name = null;
        for (String pair : localFileInfo.getNames()) {
            boolean doPath = false;
            if (pair.startsWith("path:")) {
                pair   = pair.substring("path:".length());
                doPath = true;
            } else if (pair.startsWith("name:")) {
                pair   = pair.substring("name:".length());
                doPath = false;
            }
            if (name == null) {
                if (doPath) {
                    name = targetFile.toString();
                } else {
                    name = IO.getFileTail(targetFile.toString());
                }
            }
            String[] tuple = Utils.split(pair, ":", 2);
            if ((tuple == null) || (tuple.length != 2)) {
                continue;
            }
            name = name.replaceAll(".*" + tuple[0] + ".*", tuple[1]);
        }
        if (name == null) {
            name = IO.getFileTail(targetFile.toString());
        }
        entry.setIsLocalFile(true);
        Entry parent;
        //        System.err.println ("Entry:" + entry);
        if (targetFile.getParentFile().equals(localFileInfo.getRootDir())) {
            parent = (Entry) parentEntry;
            //            System.err.println ("\tUsing parent entry:" + parentEntry +" grandparent:" + parent.getParentEntry());
        } else {
            String parentId =
                getSynthId(parentEntry,
                           localFileInfo.getRootDir().toString(),
                           targetFile.getParentFile());
            //            System.err.println ("\tGetting parent:" + parentId);
            parent = (Entry) getEntryManager().getEntry(request, parentId,
                    false, false);
            //            System.err.println ("\tUsing other parent entry:" + parent);
        }



        String   desc   = "";
        Object[] values = null;
        if (templateEntry != null) {
            if (Utils.stringDefined(templateEntry.getDescription())) {
                desc = templateEntry.getDescription();
            }
            if (Utils.stringDefined(templateEntry.getName())) {
                name = templateEntry.getName();
            }
            values = entry.getTypeHandler().getEntryValues(entry);
        }
	long fileDate =  targetFile.lastModified();
	/*
	if(targetFile.toString().toLowerCase().indexOf("1316697.jpg")>=0) {
	    getLogManager().logSpecial("ServerSideFile:" + targetFile+
				       " file last modified date:" +
				       getDateHandler().formatDate(new Date(fileDate)));
				       
				       }*/



	long fromDate = fileDate;
	String datePatterns = localFileInfo.getDatePatterns();
	if(datePatterns!=null) {
	    Date tmpDate = Utils.extractDate(datePatterns, targetFile.getName());
	    if(tmpDate!=null) {
		fromDate = tmpDate.getTime();
	    }
	}
	Integer delta = (Integer)parentEntry.getValue(LocalFileInfo.COL_DATE_OFFSET);
	if(delta!=null && delta.intValue()!=0) {
	    fromDate += delta.intValue()*1000;
	}


        entry.initEntry(name, desc, parent,
                        getUserManager().getLocalFileUser(),
                        new Resource(targetFile, (targetFile.isDirectory()
                ? Resource.TYPE_LOCAL_DIRECTORY
                : Resource.TYPE_LOCAL_FILE)), "", Entry.DEFAULT_ORDER,
			fileDate, fileDate,
			fromDate, fromDate, values);


        if (templateEntry != null) {
            entry.initWith(templateEntry);
        } else {
            //Tack on the metadata
            entry.setMetadata(metadataList);
        }
        long t2 = System.currentTimeMillis();

        //        System.err.println ("makeSynthEntry: " + entry + " " +(t2-t1));
        return entry;

    }





    /**
     * _more_
     *
     * @param request _more_
     * @param mainEntry _more_
     * @param entryNames _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry makeSynthEntry(Request request, Entry mainEntry,
                                List<String> entryNames)
            throws Exception {
        LocalFileInfo localFileInfo = doMakeLocalFileInfo(mainEntry);
        if ( !localFileInfo.isDefined()) {
            //            System.err.println ("not defined");
            return null;
        }
        File           file       = localFileInfo.getRootDir();
        final String[] nameHolder = { "" };
        FilenameFilter fnf        = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return nameHolder[0].equals(name);
            }
        };


        for (String filename : entryNames) {
            nameHolder[0] = filename;
            File[] files = file.listFiles(fnf);
            if (files.length == 0) {
                return null;
            }
            file = files[0];
        }


        if ( !IO.isADescendentNonCanonical(localFileInfo.getRootDir(), file)) {
            throw new IllegalArgumentException("Bad file path:" + entryNames);
        }
        String subId =
            getFileComponentOfSynthId(localFileInfo.getRootDir().toString(),
                                      file);
        Entry entry = makeSynthEntry(request, mainEntry, subId);

        return entry;
    }




    /**
     * _more_
     *
     * @param id _more_
     *
     * @return _more_
     */
    public Entry createEntry(String id) {
        //Make the top level entry act like a group
        return new Entry(id, this, true);
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
    public LocalFileInfo doMakeLocalFileInfo(Entry entry) throws Exception {
        return new LocalFileInfo(getRepository(), entry);
    }

    /**
     *
     * @param request _more_
     * @param entry _more_
     * @param formBuffer _more_
     * @param column _more_
     * @param values _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void addWidgetHelp(Request request, Entry entry,
                              Appendable formBuffer, Column column,
                              Object[] values)
            throws Exception {
        if ((entry != null) && column.getName().equals("localfilepath")) {
            String path  = column.getString(values);
            String error = null;
            if (Utils.stringDefined(path)) {
                File f = new File(path);
                if ( !f.exists()) {
                    error = "Error: directory does not exist";
                } else if ( !f.isDirectory()) {
                    error = "Error: file is not a directory";
                } else if ( !getStorageManager().isLocalFileOk(
                        new File(path))) {
                    error =
                        "Error: directory is not under one of the allowable file system areas. Set this under the Admin-&gt;Access settings area";
                }
            }
            if (error != null) {
                formBuffer.append(
                    formEntry(
                        request, "",
                        HU.span(error, HU.cssClass("ramadda-error"))));
            }
        }
        super.addWidgetHelp(request, entry, formBuffer, column, values);
    }





}
