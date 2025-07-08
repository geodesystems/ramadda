/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.output;

import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.util.SelectInfo;
import org.ramadda.repository.auth.*;

import org.ramadda.repository.util.FileWriter;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;
import org.ramadda.util.IO;
import org.ramadda.util.sql.SqlUtil;

import org.w3c.dom.*;

import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.net.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import java.util.regex.*;

import java.util.zip.*;

/**
 *
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
@SuppressWarnings("unchecked")
public class ZipOutputHandler extends OutputHandler {

    private boolean debug = false;

    private static final boolean isSynthOk = true;

    private static final String ARG_WRITETODISK = "writetodisk";

    public static final OutputType OUTPUT_ZIP =
        new OutputType("Zip and Download File", "zip.zip",
                       OutputType.TYPE_OTHER, "", ICON_ZIP);

    public static final OutputType OUTPUT_THUMBNAILS =
        new OutputType("Zip thumbnails", "zip.thumbnails",
                       OutputType.TYPE_OTHER, "", ICON_ZIP);

    public static final OutputType OUTPUT_ZIPTREE =
        new OutputType("Zip and Download Tree", "zip.tree",
                       OutputType.TYPE_ACTION | OutputType.TYPE_OTHER, "",
                       ICON_ZIP);

    public static final OutputType OUTPUT_ZIPGROUP =
        new OutputType("Zip and Download Files", "zip.zipgroup",
                       OutputType.TYPE_OTHER, "", ICON_ZIP);

    public static final OutputType OUTPUT_CORPUS =
        new OutputType("Download Text Corpus", "text.corpus",
                       OutputType.TYPE_OTHER, "", ICON_TEXT);    

    public static final OutputType OUTPUT_EXPORT =
        new OutputType("Export Entries", "zip.export",
                       OutputType.TYPE_FILE | OutputType.TYPE_ACTION, "",
                       "fa-file-export");

    public static final OutputType OUTPUT_EXPORT_SHALLOW =
        new OutputType("Shallow Export", "zip.export.shallow",
                       OutputType.TYPE_FILE | OutputType.TYPE_ACTION, "",
                       "fa-file-export");

    public static final OutputType OUTPUT_EXPORT_DEEP =
        new OutputType("Deep Export", "zip.export.deep",
                       OutputType.TYPE_FILE | OutputType.TYPE_ACTION, "",
                       "fa-file-export");        

    public ZipOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        addType(OUTPUT_ZIP);
        addType(OUTPUT_ZIPGROUP);
        addType(OUTPUT_ZIPTREE);
        addType(OUTPUT_CORPUS);
        addType(OUTPUT_THUMBNAILS);
        addType(OUTPUT_EXPORT);
        addType(OUTPUT_EXPORT_SHALLOW);
        addType(OUTPUT_EXPORT_DEEP);		
    }

    public AuthorizationMethod getAuthorizationMethod(Request request) {
        return AuthorizationMethod.AUTH_HTTP;
    }

    public void getEntryLinks(Request request, State state, List<Link> links)
            throws Exception {

        if (state.entry != null) {
            if (getAccessManager().canDownload(request, state.entry)
                    && getAccessManager().canDoExport(request, state.entry)) {
                links.add(makeLink(request, state.entry, OUTPUT_ZIP));
            }

            //      if (getAccessManager().canDoExport(request, state.entry)) {
            //          links.add(makeLink(request, state.entry, OUTPUT_EXPORT));
            //      }
            return;
        }

        if ((state.group != null) && state.group.isDummy()) {
            if ( !request.isAnonymous()) {
                links.add(makeLink(request, state.entry, OUTPUT_EXPORT));
                links.add(makeLink(request, state.entry, OUTPUT_EXPORT_SHALLOW));
                links.add(makeLink(request, state.entry, OUTPUT_EXPORT_DEEP));
            }
        }

        boolean hasFile  = false;
        boolean hasGroup = false;
        for (Entry child : state.getAllEntries()) {
            if (getAccessManager().canDownload(request, child)) {
                hasFile = true;

                break;
            }
            if (child.isGroup()) {
                hasGroup = true;
            }
        }

        if (hasFile) {
            if (state.group != null) {
                links.add(makeLink(request, state.group, OUTPUT_ZIPGROUP));
                links.add(makeLink(request, state.group, OUTPUT_THUMBNAILS));
            } else {
                links.add(makeLink(request, state.group, OUTPUT_ZIP));
            }
        }

        if ((state.group != null) && (hasGroup || hasFile)
                && ( !state.group.isTopEntry() || state.group.isDummy())) {
            links.add(makeLink(request, state.group, OUTPUT_ZIPTREE));
        }

        if (hasFile) {
	    links.add(makeLink(request, state.group, OUTPUT_CORPUS));
	}

    }

    public Result outputEntry(Request request, OutputType outputType,
                              Entry entry)
            throws Exception {
        List<Entry> entries = new ArrayList<Entry>();
        entries.add(entry);
        OutputType output = request.getOutput();
        if (output.equals(OUTPUT_CORPUS)) {
	    List<Entry> children  = new ArrayList<Entry>();
	    children.add(entry);
            return toCorpus(request, entry.getName(), children);
	}

        return toZip(request, entry.getName(), entries, false, false,false);
    }

    @Override
    public Result outputGroup(Request request, OutputType outputType,
                              Entry group, List<Entry> children)
            throws Exception {

        OutputType output = request.getOutput();
        if (output.equals(OUTPUT_CORPUS)) {
	    children.add(group);
            return toCorpus(request, group.getName(), children);
	}
        if (output.equals(OUTPUT_ZIPTREE)) {
            getLogManager().logInfo("Doing zip tree");
            return toZip(request, group.getName(), children, true, false,false);
        }
        if (output.equals(OUTPUT_THUMBNAILS)) {
            getLogManager().logInfo("Doing zip thumbnails");
            return toZip(request, group.getName(), children, false, false,true);
        }	
        if (output.equals(OUTPUT_EXPORT)) {
            return toZip(request, group.getName(), children, true, true,false);
	} else  if (output.equals(OUTPUT_EXPORT_SHALLOW)) {
	    return toZip(request, group.getName(), children, false, true,false);
	} else  if (output.equals(OUTPUT_EXPORT_DEEP)) {
	    return toZip(request, group.getName(), children, false, true,false);	    
        } else {
            return toZip(request, group.getName(), children, false, false,false);
        }
    }

    public String getMimeType(OutputType output) {
        if (output.equals(OUTPUT_ZIP) || output.equals(OUTPUT_ZIPGROUP) || output.equals(OUTPUT_THUMBNAILS)) {
            return repository.getMimeTypeFromSuffix(".zip");
        } else {
            return super.getMimeType(output);
        }
    }

    public Result toZip(Request request, String prefix, List<Entry> entries,
                        boolean recurse, boolean forExport,boolean thumbnails,boolean...deep)
            throws Exception {
        OutputStream os         = null;
        boolean      doingFile  = false;
	boolean doDeep = deep.length>0?deep[0]:false;

        File         tmpFile    = null;
        boolean      isInternal = false;
        Element      root       = null;
        boolean      ok         = true;
        //First recurse down without a zos to check the size
        try {
            processZip(request, entries, recurse, 0, null, prefix, 0,
                       new int[] { 0 }, forExport, thumbnails,null,doDeep,new HashSet<String>());
        } catch (IllegalArgumentException iae) {
            ok = false;
        }
        if ( !ok) {
            return new Result(
                "Error",
                new StringBuffer(
                    getPageHandler().showDialogError(
                        "Size of request has exceeded maximum size")));
        }

        //Now set the return file name
        if (prefix.length() == 0) {
            request.setReturnFilename("entry.zip");
        } else {
            request.setReturnFilename(prefix + ".zip");
        }

        Result     result         = new Result();
        FileWriter fileWriter     = null;

        boolean    writeToDisk    = request.get(ARG_WRITETODISK, false);
        File       writeToDiskDir = null;
        if (writeToDisk) {
            //IMPORTANT: Make sure that the user is an admin when handling the write to disk 
            request.ensureAdmin();
            forExport = true;
            writeToDiskDir =
                getStorageManager().makeTempDir(getRepository().getGUID(),
                    false).getDir();
            fileWriter = new FileWriter(writeToDiskDir);
        } else {
            tmpFile = (File) request.getExtraProperty("zipfile");
            if ((tmpFile == null)
                    && (request.getHttpServletResponse() != null)) {
                os = request.getHttpServletResponse().getOutputStream();
                request.getHttpServletResponse().setContentType(
                    getMimeType(OUTPUT_ZIP));
            } else {
                if (tmpFile == null) {
                    tmpFile = getRepository().getStorageManager().getTmpFile(".zip");
                } else {
                    isInternal = true;
                }
                os = getStorageManager().getUncheckedFileOutputStream(
                    tmpFile);
                doingFile = true;
            }
            fileWriter = new FileWriter(new ZipOutputStream(os));
            result.setNeedToWrite(false);
            if (request.get(ARG_COMPRESS, true) == false) {
                //You would think that setting the method to stored would work
                //but it throws an error wanting the crc to be set on the ZipEntry
                //            zos.setMethod(ZipOutputStream.STORED);
                fileWriter.setCompressionOn();
            }
        }

        Hashtable seen = new Hashtable();
        try {
            if (forExport) {
                Document doc = XmlUtil.makeDocument();
                root = XmlUtil.create(doc, TAG_ENTRIES, null,
                                      new String[] {});

            }
            processZip(request, entries, recurse, 0, fileWriter, prefix, 0,
                       new int[] { 0 }, forExport, thumbnails,root,doDeep,new HashSet<String>());

            if (root != null) {
                String xml = XmlUtil.toString(root);
                fileWriter.writeFile("entries.xml", xml.getBytes());
            }
        } finally {
            fileWriter.close();
        }
        if (doingFile) {
            IO.close(os);

            return new Result(
                "", getStorageManager().getFileInputStream(tmpFile),
                getMimeType(OUTPUT_ZIP));

        }
        getLogManager().logInfo("Zip File ended");

        if (writeToDisk) {
            return new Result("Export",
                              new StringBuffer("<p>Exported to:<br>"
                                  + writeToDiskDir));
        }

        return result;

    }

    public Result toCorpus(final Request request, String prefix, final List<Entry> entries)
            throws Exception {
	InputStream is =IO.pipeIt(new IO.PipedThing(){
		public void run(OutputStream os) {
		    PrintStream           pw  = new PrintStream(os);
		    try {
			long t1 = System.currentTimeMillis();
			for(Entry e: entries) {
			    if(!getAccessManager().canDoFile(request, e)){
				continue;
			    }
			    pw.println("Document: " + e.getName());
			    if(e.isFile()) {
				String tail = getStorageManager().getFileTail(e);
				String url = request.getAbsoluteUrl(getRepository().URL_ENTRY_SHOW) +"?" + HU.arg(ARG_ENTRYID,e.getId());
				pw.println("["+ tail+"](" + url+")");
			    }
			    pw.println(e.getDescription());
			    if(e.isFile()) {
				String corpus =getSearchManager().extractCorpus(request,e,e.getResource().getPath(),null);
				if(corpus!=null) {
				    corpus = corpus.trim();
				    corpus = corpus.replaceAll("[^\\x00-\\x7F]", "");
				    pw.println(corpus);
				}
			    }
			    pw.println("\n\n");
			}
			//			    makeJson(request, allEntries, pw);
			long t2 = System.currentTimeMillis();
			//			    Utils.printTimes("makeJson",t1,t2);
		    } catch(Exception exc) {
			getLogManager().logError("Making Corpus",exc);
		    }
		}});
	request.setReturnFilename(prefix+".txt",false);
	return request.returnStream("corpus.txt", "text/plain",is);
    }

    protected long processZip(Request request, List<Entry> entries,
                              boolean recurse, int level,
                              FileWriter fileWriter, String prefix,
                              long sizeSoFar, int[] counter,
                              boolean forExport, boolean thumbnails,
			      Element entriesRoot,boolean deep,
			      HashSet<String>seenEntry)
            throws Exception {

        long      sizeProcessed = 0;
        HashSet seen          = new HashSet();
        long      sizeLimit;
        if (request.isAnonymous()) {
            sizeLimit = MEGA
                        * getRepository().getProperty(
                            request.PROP_ZIPOUTPUT_ANONYMOUS_MAXSIZEMB, 4000);
        } else {
            sizeLimit = MEGA
                        * getRepository().getProperty(
                            request.PROP_ZIPOUTPUT_REGISTERED_MAXSIZEMB,
                            8000);
        }
	if(debug)
	    System.err.println("toZip: recurse:" + recurse +" entries: " + entries);
        for (Entry entry : entries) {
	    if(seenEntry.contains(entry.getId())) continue;
	    seenEntry.add(entry.getId());
	    if(debug)
		System.err.println("\tentry:" + entry +" is Group:" + entry.isGroup());
            //Check for access
            if (forExport) {
                if ( !getAccessManager().canDoExport(request, entry)) {
                    continue;
                }
            }
	    //Check for synthetic entries beyond the top most level
	    if(!isSynthOk || forExport) {
		if(getEntryManager().isSynthEntry(entry.getId())) {
		    if(level>0) {
			if(debug)System.err.println("\tskipping synth entry:" + entry);
			continue;
		    }
		}
	    }

            counter[0]++;
            //Don't get big files

            if (!thumbnails && request.defined(ARG_MAXFILESIZE) && entry.isFile()) {
		long length = getStorageManager().getEntryFileLength(entry);
                if (length   >= request.get(ARG_MAXFILESIZE, 0)) {
                    continue;
                }
            }

	    //Not sure why we were using the tmp request as this can block reading lat/lon etc
	    //since it is an anonymous request
	    //            Request tmpRequest = getRepository().getTmpRequest();
            Request tmpRequest = request;
            Element entryNode  = null;
            if (forExport && (entriesRoot != null)) {
		boolean includeParentId = level != 0;
		//		System.err.println("entry:" + entry +" level:" + level +" includ:" + includeParentId);
                entryNode = getRepository().getXmlOutputHandler().getEntryTag(
                    tmpRequest, entry, fileWriter,
                    entriesRoot.getOwnerDocument(), entriesRoot, true,
                    includeParentId);
                //                System.err.println ("exporting:" + XmlUtil.toString(entryNode));
            }


            if (entry.isGroup() && recurse) {
		SelectInfo info = new SelectInfo(request, entry,isSynthOk&&!forExport);
                List<Entry> children = getEntryManager().getChildren(request, entry,info);
                String path = entry.getName();
                if (prefix.length() > 0) {
                    path = prefix + "/" + path;
                }
                sizeProcessed += processZip(request, children, recurse,
                                            level + 1, fileWriter, path,
                                            sizeProcessed + sizeSoFar,
                                            counter, forExport, thumbnails,entriesRoot,deep,seenEntry);
            }

	    if(forExport && deep) {
		List<Entry> deepEntries = new ArrayList<Entry>();
		//Hard code hack for now
		if(entry.getTypeHandler().isType("geo_imdv")) {
		    try {
			String json = getStorageManager().readEntry(entry);
			deepEntries.addAll(getEntryManager().getEntries(request, getEntryUtil().extractIDs(json),seenEntry));
		    } catch(Exception exc) {
			getLogManager().logError("reading deep entries from imdv:" + entry,exc);
		    }
		}
		deepEntries.addAll(getEntryManager().getEntries(request, getEntryUtil().extractIDs(entry.getDescription()),seenEntry));
		if(deepEntries.size()>0) {
		    String path = entry.getName();
		    if (prefix.length() > 0) {
			path = prefix + "/" + path;
		    }
		    //Pass in level=0 so the parent ID of these deep entries doesn't get set in the entry.xml
		    sizeProcessed += processZip(request, deepEntries, recurse,
						0, fileWriter, path,
						sizeProcessed + sizeSoFar,
						counter, forExport, thumbnails,entriesRoot,deep,seenEntry);
		}
	    }

            if (!thumbnails &&  !getAccessManager().canDownload(request, entry,debug)) {
		if(debug)   System.err.println("No download:" + entry);
                continue;
            }

	    String path=null;
            String name=null;

	    if(thumbnails) {
		List<Metadata> metadataList =
		    getMetadataManager().findMetadata(request, entry,
                         new String[] { ContentMetadataHandler.TYPE_THUMBNAIL},
						      false);

		if(metadataList!=null && metadataList.size()>0) {
		    String[]tuple=  getMetadataManager().getFileUrl(request, entry, metadataList.get(0));
		    if(tuple!=null) {
			name = getStorageManager().getOriginalFilename(tuple[0]);
			path = tuple[2];
			if(debug)
			    System.err.println("File:" + entry +" " + name);
		    } else {
			if(debug)
			    System.err.println("No tuple:" + entry);
		    }
		} else {
		    if(debug)
			System.err.println("No thumbnail:" + entry);
		}
	    } else {
		path = getStorageManager().getEntryResourcePath(entry);
		name = getStorageManager().getFileTail(entry);
	    }
	    if(path==null) continue;

            int    cnt  = 1;
            if ( !forExport) {
		boolean dup =false;
                while (seen.contains(name)) {
                    name = (cnt++) + "_" + name;
		    dup=true;
                }
                seen.add(name);
		if(dup) name = "dup_" + name;
                if (!thumbnails && prefix.length() > 0) {
                    name = prefix + "/" + name;
                }
            }
            File f = new File(path);
            sizeProcessed += f.length();

            //check for size limit
            if (sizeSoFar + sizeProcessed > sizeLimit) {
                throw new IllegalArgumentException(
                    "Size of request has exceeded maximum size");
            }

            if (fileWriter != null) {
                InputStream fis =
                    getStorageManager().getFileInputStream(path);
                if ((entryNode != null) && forExport) {
                    fileWriter.writeFile(entry.getId(), fis);
                    XmlUtil.setAttributes(entryNode, new String[] { ATTR_FILE,
                            entry.getId(), ATTR_FILENAME, name });

                } else {
                    fileWriter.writeFile(name, fis);
                }
            }
        }

        return sizeProcessed;

    }

}
