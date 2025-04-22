/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.output;

import com.drew.imaging.jpeg.*;
import com.drew.lang.*;

import com.drew.metadata.*;
import com.drew.metadata.exif.*;

import org.ramadda.repository.*;

import org.ramadda.util.HtmlUtils;

import org.w3c.dom.*;

import ucar.unidata.util.Misc;

import ucar.unidata.util.StringUtil;

import java.awt.Image;
import java.awt.image.*;

import java.io.*;

import java.io.File;

import java.net.*;

import java.util.Iterator;
import java.util.List;

/**
 *
 *
 * @author RAMADDA Development Team
 */
public class JpegMetadataOutputHandler extends OutputHandler {

    public static final OutputType OUTPUT_JPEG_METADATA =
        new OutputType("JPEG Metadata", "jpeg.metadata",
                       OutputType.TYPE_VIEW, "", ICON_IMAGES);

    public JpegMetadataOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        addType(OUTPUT_JPEG_METADATA);
    }

    public void getEntryLinks(Request request, State state, List<Link> links)
            throws Exception {
        if (state.entry != null) {
            String path = state.entry.getResource().getPath().toLowerCase();
            if ( !(path.endsWith(".jpg") || path.endsWith(".jpeg")
                    || path.endsWith(".tiff"))) {
                return;
            }
            links.add(makeLink(request, state.getEntry(),
                               OUTPUT_JPEG_METADATA));
        }
    }

    public Result outputEntry(Request request, OutputType outputType,
                              Entry entry)
            throws Exception {
        StringBuffer sb       = new StringBuffer();
        File         jpegFile = new File(entry.getResource().getPath());
        outputTags(request, entry, sb, jpegFile, true);

        return new Result("JPEG Metadata", sb);
    }

    private void outputTags(Request request, Entry entry, StringBuffer sb,
                            File jpegFile, boolean forHtml)
            throws Exception {

        com.drew.metadata.Metadata metadata =
            com.drew.imaging.ImageMetadataReader.readMetadata(jpegFile);
        //            JpegMetadataReader.readMetadata(jpegFile);

        if (forHtml) {
            getPageHandler().entrySectionOpen(request, entry, sb,
                    entry.getName());
            sb.append("<ul>");
        }
        //        java.lang.Iterable<Directory> directories = metadata.getDirectories();
        Iterator directories = metadata.getDirectories().iterator();
        while (directories.hasNext()) {
            Directory directory = (Directory) directories.next();
            if (forHtml) {
                sb.append("<li> ");
            }
            sb.append(directory.getName());
            sb.append("\n");
            if (forHtml) {
                sb.append("<ul>");
            }
            //            Iterator tags = directory.getTagIterator();
            //while (tags.hasNext()) {
            //                Tag tag = (Tag) tags.next();
            for (Tag tag : directory.getTags()) {
                if (tag.getTagName().indexOf("Unknown") >= 0) {
                    continue;
                }
                if (forHtml) {
                    sb.append("<li> ");
                }
                sb.append(tag.getTagName());
                sb.append(":");
                sb.append(tag.getDescription());
                sb.append("\n");
            }
            if (forHtml) {
                sb.append("</ul>");
            }
        }
        if (forHtml) {
            getPageHandler().entrySectionClose(request, entry, sb);
            sb.append("</ul>");
        }
    }

    public static void main(String[] args) throws Exception {
        for (String file : args) {
            StringBuffer sb = new StringBuffer();
            //outputTags(sb, new File(file), false);
            System.out.println(sb);
        }
    }

}
