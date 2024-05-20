/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.data.docs;


import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;

import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;


import ucar.unidata.xml.XmlUtil;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.Date;
import java.util.Hashtable;
import java.util.List;

import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


/**
 *
 *
 */
public class MsDocTypeHandler extends GenericTypeHandler {


    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public MsDocTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     * @param fromImport _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void initializeNewEntry(Request request, Entry entry,NewType newType)
            throws Exception {
        super.initializeNewEntry(request, entry, newType);
	if(newType!=NewType.NEW) return;
        initializeDocEntry(request, entry);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    private void initializeDocEntry(Request request, Entry entry)
            throws Exception {
        File file = entry.getFile();
        if ( !file.exists()) {
            return;
        }
        String filename = file.toString().toLowerCase();
        if ( !(filename.endsWith(".pptx") || filename.endsWith(".docx")
	       || filename.endsWith(".xlsx"))) {
            return;
        }
	if(entry.getTransientProperty("msinit")!=null) return;
	entry.putTransientProperty("msinit","true");
        try {
            try(InputStream    fis = getStorageManager().getFileInputStream(file)) {
		ZipInputStream zin = new ZipInputStream(fis);
		ZipEntry       ze  = null;
		while ((ze = zin.getNextEntry()) != null) {
		    if (ze.isDirectory()) {
			continue;
		    }
		    String  path        = ze.getName();
		    String  lcpath      = path.toLowerCase();
		    boolean isImage     = false;
		    boolean isThumbnail = false;
		    if (lcpath.endsWith("thumbnail.jpeg")) {
			isThumbnail = isImage = true;
		    } else if (lcpath.endsWith(".jpeg")
			       || lcpath.endsWith(".jpg")
			       || lcpath.endsWith(".png")
			       || lcpath.endsWith(".gif")) {
			isImage = true;
		    }

		    //For now just extract the thumbnails, not all of the images
		    if (isThumbnail) {
			String thumbFile = IOUtil.getFileTail(path);
			File   f = getStorageManager().getTmpFile(thumbFile);
			OutputStream   fos = getStorageManager().getFileOutputStream(f);
			try {
			    IOUtil.writeTo(zin, fos);
			} finally {
			    IO.close(fos);
			}
			String fileName =
			    getStorageManager().copyToEntryDir(entry,
							       f).getName();
			Metadata metadata =
			    new Metadata(getRepository().getGUID(),
					 entry.getId(), (isThumbnail
							 ? ContentMetadataHandler.TYPE_THUMBNAIL
							 : ContentMetadataHandler.TYPE_ATTACHMENT), false,
					 fileName, null, null, null, null);

			getMetadataManager().addMetadata(request,entry, metadata);
			break;
		    }
                }
            }
        } catch (Exception exc) {
            System.err.println("oops:" + exc);
        }
    }

    @Override
    public Result getHtmlDisplay(Request request, Entry entry)
	throws Exception {
        Column c = getColumn("display_inline");
        Boolean b = (Boolean) entry.getValue(c.getOffset());
	if(b==null || !b) return super.getHtmlDisplay(request,entry);
	StringBuffer sb = new StringBuffer();
	getPageHandler().entrySectionOpen(request,  entry,sb, "");
	StringBuilder wiki = new StringBuilder("{{description wikify=true}}\n+toggle Document Information\n{{information details=true showTitle=false}}\n-toggle\n");
	wiki.append("+fullscreen\n{{embedms}}\n-fullscreen\n");
	sb.append(getWikiManager().wikifyEntry(request, entry, wiki.toString()));
	getPageHandler().entrySectionClose(request,  entry, sb);
	return getEntryManager().addHeaderToAncillaryPage(request,
							  new Result(BLANK, sb));
    }





}
