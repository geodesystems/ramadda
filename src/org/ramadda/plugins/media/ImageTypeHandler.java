/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.media;


import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;
import org.ramadda.util.WikiUtil;

import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;

import java.util.Hashtable;
import org.ramadda.util.ImageUtils;
import java.awt.Image;

import java.io.*;
import java.util.List;


/**
 *
 *
 */
public class ImageTypeHandler extends GenericTypeHandler {


    public static final String ARG_IMAGE_RESIZE="imageresize";
    public static final String ARG_STRIP_METADATA="stripmetadata";    
    public static final String ARG_IMAGE_WIDTH="imagewidth";

    /**  */
    public static int IDX = 0;

    /**  */
    public static final int IDX_PROXY = IDX++;

    /**  */
    public static final int IDX_FILENAME = IDX++;

    /**  */
    public static final int IDX_LAST = IDX_FILENAME;


    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public ImageTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }

    @Override
    public void getFileExtras(Request request, Entry entry, StringBuilder sb)
            throws Exception {
	sb.append(HU.b("Image:"));
	sb.append("<div style='margin-left:30px;'>");
        sb.append(HU.labeledCheckbox(ARG_IMAGE_RESIZE, "true", false,"Resize image"));
	sb.append(HU.space(2));
	sb.append("Width:");
	sb.append(HU.space(1));
	sb.append(HU.input(ARG_IMAGE_WIDTH,"600",HU.SIZE_5));
	sb.append("<br>");
        sb.append(HU.labeledCheckbox(ARG_STRIP_METADATA, "true", false,"Strip metadata"));
	sb.append("<br>");
	if(getRepository().getSearchManager().isImageIndexingEnabled()) {
	    sb.append(HU.labeledCheckbox(ARG_INDEX_IMAGE, "true", false,"Extract text from image"));
	}
	sb.append("</div>");



        super.getFileExtras(request, entry,sb);

    }


    @Override
    public void initializeNewEntry(Request request, Entry entry,NewType newType)
            throws Exception {
        super.initializeNewEntry(request, entry, newType);
	if(!isNew(newType)) return;
	if(!request.get(ARG_IMAGE_RESIZE,false) &&
	   !request.get(ARG_STRIP_METADATA,false)) return;
	if(!entry.getResource().isStoredFile()) return;

	String theFile = entry.getResource().getPath();
	Image image = ImageUtils.readImage(theFile);
	int width = request.get(ARG_IMAGE_WIDTH,600);
	if(request.get(ARG_IMAGE_RESIZE,false)) {
	    if (image.getWidth(null) > width) {
		image = ImageUtils.resize(image, width, -1);
		ImageUtils.waitOnImage(image);
	    }
	}
	ImageUtils.writeImageToFile(image, theFile);
	File f = new File(theFile);
	entry.getResource().setFileSize(f.length());
    }
    


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public String getPathForEntry(Request request, Entry entry, boolean forRead)
            throws Exception {
        Resource resource = entry.getResource();
        String   path     = Utils.normalizeTemplateUrl(resource.getPath());
        boolean  useProxy = entry.getBooleanValue(0, false);
        if (useProxy) {
            String filename = entry.getStringValue(1, (String) null);
            String tail     = IOUtil.getFileTail(path);
            if (Utils.stringDefined(filename)) {
                tail = filename;
            }
            path = getRepository().getUrlBase() + "/proxy/" + tail
                   + "?entryid=" + entry.getId();
        }

        return path;
    }

    /**
     *
     * @param wikiUtil _more_
     * @param request _more_
     * @param originalEntry _more_
     * @param entry _more_
     * @param tag _more_
     * @param props _more_
     *  @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props)
            throws Exception {

        if (tag.equals("360image")) {
            StringBuilder sb = new StringBuilder();
            sb.append("\n\n");
            request.putExtraProperty("aframejs", "true");
            HU.importJS(sb, "https://aframe.io/releases/0.9.0/aframe.min.js");
            String imgUrl =
                entry.getTypeHandler().getEntryResourceUrl(request, entry);
            String width  = Utils.getProperty(props, "width", "600px");
            String height = Utils.getProperty(props, "height", "200px");
            sb.append("\n");
            sb.append(HtmlUtils.importCss("a-scene {height: " + height
                                          + ";width:" + width + ";}"));
            sb.append("\n<a-scene embedded>\n");
            List<Metadata> metadataList =
                getMetadataManager().findMetadata(request, entry, "3d_label",
                    true);
            if ((metadataList != null) && (metadataList.size() > 0)) {
                for (Metadata metadata : metadataList) {
                    sb.append("<a-text  value='" + metadata.getAttr1()
                              + "' width='" + metadata.getAttr2()
                              + "' position='" + metadata.getAttr3()
                              + "' rotation='" + metadata.getAttr4()
                              + "'></a-text>\n");
                }
            }
            String rotation = (String) entry.getStringValue(IDX_LAST + 1, "");
            sb.append("<a-sky src='" + imgUrl + "'");
            if ((rotation != null) && (rotation.trim().length() > 0)) {
                sb.append(" rotation='" + rotation + "' ");
            }
            sb.append(" ></a-sky>\n ");
	    sb.append("<a-entity camera look-controls position=\"0 1.6 0\" id=\"camera-id\"></a-entity>\n");

            sb.append("</a-scene>\n ");

            return sb.toString();
        }

        return super.getWikiInclude(wikiUtil, request, originalEntry, entry,
                                    tag, props);

    }
}
