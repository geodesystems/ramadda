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

package org.ramadda.data.docs;


import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;

import org.ramadda.util.XlsUtil;


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
     *
     * @throws Exception _more_
     */
    @Override
    public void initializeNewEntry(Request request, Entry entry, boolean fromImport)
            throws Exception {
        super.initializeNewEntry(request, entry, fromImport);
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
    public void initializeDocEntry(Request request, Entry entry)
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
        try {
            InputStream    fis = getStorageManager().getFileInputStream(file);
            OutputStream   fos = null;
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
                    File   f = getStorageManager().getTmpFile(null,
                                   thumbFile);
                    fos = getStorageManager().getFileOutputStream(f);
                    try {
                        IOUtil.writeTo(zin, fos);
                    } finally {
                        IOUtil.close(fos);
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

                    getMetadataManager().addMetadata(entry, metadata);
                }
            }
        } catch (Exception exc) {
            System.err.println("oops:" + exc);
        }
    }

}
