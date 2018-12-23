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

package org.ramadda.repository.output;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.type.*;

import org.ramadda.util.sql.SqlUtil;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Json;
import org.ramadda.util.Utils;


import org.w3c.dom.*;

import ucar.unidata.ui.ImageUtils;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;


import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.awt.Color;

import java.awt.Image;
import java.awt.image.*;


import java.io.*;

import java.io.File;


import java.net.*;


import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;



import java.util.regex.*;

import java.util.zip.*;


/**
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
public class ImageOutputHandler extends OutputHandler {

    /** _more_ */
    public static final String ARG_IMAGE_STYLE = "image.style";

    /** _more_ */
    public static final String ARG_IMAGE_EDIT = "image.edit";

    /** _more_ */
    public static final String ARG_CAPTION = "caption";

    /** _more_ */
    public static final String ARG_CAPTION_TOP = "caption.top";

    /** _more_ */
    public static final String ARG_CAPTION_BOTTOM = "caption.bottom";

    /** _more_ */
    public static final String ARG_IMAGE_APPLY_TO_GROUP =
        "image.applytogroup";

    /** _more_ */
    public static final String ARG_IMAGE_UNDO = "image.undo";

    /** _more_ */
    public static final String ARG_IMAGE_EDIT_RESIZE = "image.edit.resize";

    /** _more_ */
    public static final String ARG_IMAGE_EDIT_WIDTH = "image.edit.width";

    /** _more_ */
    public static final String ARG_IMAGE_EDIT_CROP = "image.edit.crop";

    /** _more_ */
    public static final String ARG_IMAGE_EDIT_MATTE = "image.edit.matte";

    /** _more_ */
    public static final String ARG_IMAGE_EDIT_REDEYE = "image.edit.redeye";

    /** _more_ */
    public static final String ARG_IMAGE_EDIT_TRANSPARENT =
        "image.edit.transparent";


    /** _more_ */
    public static final String ARG_IMAGE_CROPX1 = "image.edit.cropx1";

    /** _more_ */
    public static final String ARG_IMAGE_CROPY1 = "image.edit.cropy1";

    /** _more_ */
    public static final String ARG_IMAGE_CROPX2 = "image.edit.cropx2";

    /** _more_ */
    public static final String ARG_IMAGE_CROPY2 = "image.edit.cropy2";

    /** _more_ */
    public static final String ARG_IMAGE_EDIT_ROTATE_LEFT =
        "image.edit.rotate.left";

    /** _more_ */
    public static final String ARG_IMAGE_EDIT_ROTATE_LEFT_X =
        "image.edit.rotate.left.x";

    /** _more_ */
    public static final String ARG_IMAGE_EDIT_ROTATE_LEFT_Y =
        "image.edit.rotate.left.y";

    /** _more_ */
    public static final String ARG_IMAGE_EDIT_ROTATE_RIGHT =
        "image.edit.rotate.right";

    /** _more_ */
    public static final String ARG_IMAGE_EDIT_ROTATE_RIGHT_X =
        "image.edit.rotate.right.x";


    /** _more_ */
    public static final String ARG_IMAGE_EDIT_ROTATE_RIGHT_Y =
        "image.edit.rotate.right.y";

    /** _more_ */
    public static final OutputType OUTPUT_GALLERY =
        new OutputType("Gallery", "image.gallery",
                       OutputType.TYPE_VIEW | OutputType.TYPE_FORSEARCH, "",
                       ICON_IMAGES);

    /** _more_ */
    public static final OutputType OUTPUT_VIDEO =
        new OutputType("Play Video", "image.video", OutputType.TYPE_VIEW, "",
                       ICON_IMAGES);

    /** _more_ */
    public static final OutputType OUTPUT_PLAYER =
        new OutputType("Image Player", "image.player", OutputType.TYPE_VIEW,
                       "", ICON_IMAGES);

    /** _more_ */
    public static final OutputType OUTPUT_SLIDESHOW =
        new OutputType("Slideshow", "image.slideshow", OutputType.TYPE_VIEW,
                       "", ICON_IMAGES);


    /** _more_ */
    public static final OutputType OUTPUT_EDIT = new OutputType("Edit Image",
                                                     "image.edit",
                                                     OutputType.TYPE_VIEW,
                                                     "", ICON_IMAGES);

    /** _more_ */
    public static final OutputType OUTPUT_CAPTION =
        new OutputType("Caption Image", "image.caption",
                       OutputType.TYPE_VIEW, "", ICON_IMAGES);


    /** _more_ */
    public static final OutputType OUTPUT_STREETVIEW =
        new OutputType("Caption Image", "streetview",
                       OutputType.TYPE_INTERNAL, "", ICON_IMAGES);


    /** _more_ */
    private String streetviewKey;


    /**
     * _more_
     *
     *
     * @param repository _more_
     * @param element _more_
     * @throws Exception _more_
     */
    public ImageOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        addType(OUTPUT_GALLERY);
        addType(OUTPUT_PLAYER);
        //        addType(OUTPUT_SLIDESHOW);
        addType(OUTPUT_CAPTION);
        addType(OUTPUT_EDIT);
        addType(OUTPUT_VIDEO);
        addType(OUTPUT_STREETVIEW);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param state _more_
     * @param links _more_
     *
     *
     * @throws Exception _more_
     */

    public void getEntryLinks(Request request, State state, List<Link> links)
            throws Exception {


        if (state.entry != null) {
            if (state.entry.isFile()) {
                //                if (state.entry.isImage()) {
                //                    links.add(makeLink(request, state.getEntry(), OUTPUT_CAPTION));
                //                }
                String extension =
                    IOUtil.getFileExtension(
                        state.entry.getResource().getPath()).toLowerCase();
                if (extension.equals(".mp3") || extension.equals(".mp4")
                        || extension.equals(".mpg")) {
                    links.add(makeLink(request, state.getEntry(),
                                       OUTPUT_VIDEO));
                }
            }
            if (getAccessManager().canDoAction(request, state.entry,
                    Permission.ACTION_EDIT)) {
                if (state.entry.getResource().isEditableImage()) {
                    File f = state.entry.getFile();
                    if ((f != null) && f.canWrite()) {
                        Link link = makeLink(request, state.getEntry(),
                                             OUTPUT_EDIT);
                        link.setLinkType(OutputType.TYPE_EDIT);
                        links.add(link);
                    }
                }
            }

            return;
        }


        List<Entry> entries = state.getAllEntries();
        if (entries.size() == 0) {
            return;
        }

        if (entries.size() > 0) {
            boolean ok = false;
            for (Entry entry : entries) {
                if (entry.isImage()) {
                    ok = true;

                    break;
                }
            }
            if ( !ok) {
                return;
            }
        }

        if (state.getEntry() != null) {
            //            links.add(makeLink(request, state.getEntry(), OUTPUT_SLIDESHOW));
            links.add(makeLink(request, state.getEntry(), OUTPUT_GALLERY));
            links.add(makeLink(request, state.getEntry(), OUTPUT_PLAYER));
        }
    }




    /** _more_ */
    private Hashtable<String, Image> imageCache = new Hashtable<String,
                                                      Image>();

    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    private Image getImage(Entry entry) {
        Image image = imageCache.get(entry.getId());
        if (image == null) {
            image = Utils.readImage(entry.getResource().getPath());
            //Keep the cache size low
            if (imageCache.size() > 5) {
                imageCache = new Hashtable<String, Image>();
            }
            imageCache.put(entry.getId(), image);
        }

        return image;
    }

    /**
     * _more_
     *
     * @param entry _more_
     * @param image _more_
     */
    private void putImage(Entry entry, Image image) {
        imageCache.put(entry.getId(), image);
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param outputType _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputEntry(Request request, OutputType outputType,
                              Entry entry)
            throws Exception {

        StringBuffer sb = new StringBuffer();



        if (outputType.equals(OUTPUT_STREETVIEW)) {
            return outputEntryStreetview(request, entry);
        }

        if (outputType.equals(OUTPUT_CAPTION)) {
            return outputEntryCaption(request, entry);
        }

        if (outputType.equals(OUTPUT_VIDEO)) {
            Link link = entry.getTypeHandler().getEntryDownloadLink(request,
                            entry);
            if (link == null) {
                sb.append("Not available");
            } else {
                sb.append(HtmlUtils.p());
                String html =
                    "<OBJECT CLASSID=\"clsid:02BF25D5-8C17-4B23-BC80-D3488ABDDC6B\" CODEBASE=\"http://www.apple.com/qtactivex/qtplugin.cab\"  > <PARAM NAME=\"src\" VALUE=\""
                    + link.getUrl()
                    + "\" > <PARAM NAME=\"autoplay\" VALUE=\"true\" > <EMBED SRC=\""
                    + link.getUrl()
                    + "\" TYPE=\"image/x-macpaint\" PLUGINSPAGE=\"http://www.apple.com/quicktime/download\"  AUTOPLAY=\"true\"></EMBED> </OBJECT>";

                System.err.println(html);
                sb.append(html);
            }


            return new Result("Video", sb);
        }

        String  url            = getImageUrl(request, entry, true);
        Image   image          = null;
        boolean shouldRedirect = false;

        boolean applyToGroup   = request.get(ARG_IMAGE_APPLY_TO_GROUP, false);

        if ( !applyToGroup) {
            image          = getImage(entry);
            shouldRedirect = processImage(request, entry, image);
        } else {
            List<Entry> entries = getEntryManager().getChildren(request,
                                      entry.getParentEntry());
            for (Entry childEntry : entries) {
                if ( !childEntry.getResource().isEditableImage()) {
                    continue;
                }
                image          = getImage(childEntry);
                shouldRedirect = processImage(request, childEntry, image);
            }
        }


        if (shouldRedirect) {
            request.remove(ARG_IMAGE_EDIT_RESIZE);
            request.remove(ARG_IMAGE_EDIT_REDEYE);
            request.remove(ARG_IMAGE_EDIT_TRANSPARENT);
            request.remove(ARG_IMAGE_EDIT_CROP);
            request.remove(ARG_IMAGE_EDIT_ROTATE_LEFT);
            request.remove(ARG_IMAGE_EDIT_ROTATE_RIGHT);
            request.remove(ARG_IMAGE_EDIT_ROTATE_LEFT_X);
            request.remove(ARG_IMAGE_EDIT_ROTATE_RIGHT_X);
            request.remove(ARG_IMAGE_EDIT_ROTATE_LEFT_Y);
            request.remove(ARG_IMAGE_EDIT_ROTATE_RIGHT_Y);
            request.remove(ARG_IMAGE_UNDO);

            return new Result(request.getUrl());
        }



        if (image == null) {
            image = getImage(entry);
        }
        int imageWidth  = image.getWidth(null);
        int imageHeight = image.getHeight(null);

        getPageHandler().entrySectionOpen(request, entry, sb, "Image Edit");
        sb.append(request.formPost(getRepository().URL_ENTRY_SHOW));


        sb.append(HtmlUtils.hidden(ARG_ENTRYID, entry.getId()));
        sb.append(HtmlUtils.hidden(ARG_OUTPUT, OUTPUT_EDIT));
        sb.append(HtmlUtils.submit(msgLabel("Change width"),
                                   ARG_IMAGE_EDIT_RESIZE));
        sb.append(HtmlUtils.input(ARG_IMAGE_EDIT_WIDTH, "" + imageWidth,
                                  HtmlUtils.SIZE_5));
        sb.append(HtmlUtils.space(2));

        sb.append(HtmlUtils.submit(msg("Crop"), ARG_IMAGE_EDIT_CROP));
        sb.append(HtmlUtils.submit(msg("Remove Redeye"),
                                   ARG_IMAGE_EDIT_REDEYE));
        sb.append(HtmlUtils.submit(msg("Make Transparent"),
                                   ARG_IMAGE_EDIT_TRANSPARENT));
        sb.append(HtmlUtils.hidden(ARG_IMAGE_CROPX1, "",
                                   HtmlUtils.SIZE_3
                                   + HtmlUtils.id(ARG_IMAGE_CROPX1)));
        sb.append(HtmlUtils.hidden(ARG_IMAGE_CROPY1, "",
                                   HtmlUtils.SIZE_3
                                   + HtmlUtils.id(ARG_IMAGE_CROPY1)));
        sb.append(HtmlUtils.hidden(ARG_IMAGE_CROPX2, "",
                                   HtmlUtils.SIZE_3
                                   + HtmlUtils.id(ARG_IMAGE_CROPX2)));
        sb.append(HtmlUtils.hidden(ARG_IMAGE_CROPY2, "",
                                   HtmlUtils.SIZE_3
                                   + HtmlUtils.id(ARG_IMAGE_CROPY2)));
        sb.append(HtmlUtils.div("",
                                HtmlUtils.cssClass("image_edit_box")
                                + HtmlUtils.id("image_edit_box")));



        sb.append(HtmlUtils.space(2));
        sb.append(HtmlUtils.submitImage(getIconUrl(ICON_ANTIROTATE),
                                        ARG_IMAGE_EDIT_ROTATE_LEFT,
                                        msg("Rotate Left"), ""));
        sb.append(HtmlUtils.space(2));
        sb.append(HtmlUtils.submitImage(getIconUrl(ICON_ROTATE),
                                        ARG_IMAGE_EDIT_ROTATE_RIGHT,
                                        msg("Rotate Right"), ""));
        File entryDir = getStorageManager().getEntryDir(entry.getId(), false);
        File original = new File(entryDir + "/" + "originalimage");
        if (original.exists()) {
            sb.append(HtmlUtils.space(2));
            sb.append(HtmlUtils.submit(msg("Undo all edits"),
                                       ARG_IMAGE_UNDO));
        }

        sb.append(HtmlUtils.space(20));
        sb.append(HtmlUtils.checkbox(ARG_IMAGE_APPLY_TO_GROUP, "true",
                                     applyToGroup));
        sb.append(HtmlUtils.space(1));
        sb.append(msg("Apply to siblings"));


        sb.append(HtmlUtils.formClose());


        String clickParams =
            "event,'imgid',"
            + HtmlUtils.comma(HtmlUtils.squote(ARG_IMAGE_CROPX1),
                              HtmlUtils.squote(ARG_IMAGE_CROPY1),
                              HtmlUtils.squote(ARG_IMAGE_CROPX2),
                              HtmlUtils.squote(ARG_IMAGE_CROPY2));

        sb.append(
            HtmlUtils.importJS(getRepository().getFileUrl("/editimage.js")));

        String call = HtmlUtils.onMouseClick(HtmlUtils.call("editImageClick",
                          clickParams));
        sb.append(HtmlUtils.img(url, "", HtmlUtils.id("imgid") + call));

        getPageHandler().entrySectionClose(request, entry, sb);

        return new Result("Image Edit", sb);

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
    public Result outputEntryCaption(Request request, Entry entry)
            throws Exception {
        StringBuffer sb = new StringBuffer();
        if (request.exists(ARG_CAPTION)) {}
        sb.append(request.formPost(getRepository().URL_ENTRY_SHOW));
        sb.append(HtmlUtils.formTable());
        sb.append(HtmlUtils.hidden(ARG_ENTRYID, entry.getId()));
        sb.append(HtmlUtils.formEntry(msgLabel("Top Caption"),
                                      HtmlUtils.input(ARG_CAPTION_TOP,
                                          entry.getName())));
        sb.append(HtmlUtils.formEntry(msgLabel("Bottom Caption"),
                                      HtmlUtils.input(ARG_CAPTION_TOP,
                                          entry.getDescription())));
        sb.append(HtmlUtils.hidden(ARG_OUTPUT, OUTPUT_CAPTION));
        sb.append(HtmlUtils.submit(ARG_CAPTION, "Make Caption"));
        sb.append(HtmlUtils.formTableClose());
        sb.append(HtmlUtils.formClose());

        return new Result("Image Caption", sb);
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
    public Result outputEntryStreetview(Request request, Entry entry)
            throws Exception {
        if (isStreetviewEnabled()) {
            String googleUrl =
                "https://maps.googleapis.com/maps/api/streetview?size=600x300&location={lat},{lon}&heading={heading}&pitch=0&key={key}";
            double[] ll      = entry.getCenter();
            String   heading = request.getString("heading", "0");
            String url =
                googleUrl.replace("{lat}", "" + ll[0]).replace("{lon}",
                                  "" + ll[1]).replace("{key}",
                                      streetviewKey).replace("{heading}",
                                          heading);
            URLConnection connection = new URL(url).openConnection();
            InputStream   is         = connection.getInputStream();

            return request.returnStream(is);
        }

        return new Result("", new StringBuffer("Streetview not enabled"));
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isStreetviewEnabled() {
        if (streetviewKey == null) {
            streetviewKey = repository.getProperty("google.streetview.key",
                    "");
        }

        return streetviewKey.length() > 0;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param image _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private boolean processImage(Request request, Entry entry, Image image)
            throws Exception {
        if ( !getAccessManager().canDoAction(request, entry,
                                             Permission.ACTION_EDIT)) {
            throw new AccessException("Cannot edit image", null);
        }

        int   imageWidth  = image.getWidth(null);
        int   imageHeight = image.getHeight(null);
        Image newImage    = null;
        if (request.exists(ARG_IMAGE_UNDO)) {
            File f = entry.getFile();
            if ((f != null) && f.canWrite()) {
                File entryDir =
                    getStorageManager().getEntryDir(entry.getId(), true);
                File original = new File(entryDir + "/" + "originalimage");
                if (original.exists()) {
                    imageCache.remove(entry.getId());
                    IOUtil.copyFile(original, f);

                    return true;
                }
            }
        } else if (request.exists(ARG_IMAGE_EDIT_RESIZE)) {
            newImage = ImageUtils.resize(image,
                                         request.get(ARG_IMAGE_EDIT_WIDTH,
                                             imageWidth), -1);

        } else if (request.exists(ARG_IMAGE_EDIT_REDEYE)) {
            int x1 = request.get(ARG_IMAGE_CROPX1, 0);
            int y1 = request.get(ARG_IMAGE_CROPY1, 0);
            int x2 = request.get(ARG_IMAGE_CROPX2, 0);
            int y2 = request.get(ARG_IMAGE_CROPY2, 0);
            if ((x1 < x2) && (y1 < y2)) {
                newImage = ImageUtils.removeRedeye(image, x1, y1, x2, y2);
            }
        } else if (request.exists(ARG_IMAGE_EDIT_TRANSPARENT)) {
            BufferedImage bi    = ImageUtils.toBufferedImage(image);
            int           x     = request.get(ARG_IMAGE_CROPX1, 0);
            int           y     = request.get(ARG_IMAGE_CROPY1, 0);
            int           clr   = bi.getRGB(x, y);
            int           red   = (clr & 0x00ff0000) >> 16;
            int           green = (clr & 0x0000ff00) >> 8;
            int           blue  = clr & 0x000000ff;
            newImage = ImageUtils.makeColorTransparent(bi,
                    new Color(red, green, blue));

        } else if (request.exists(ARG_IMAGE_EDIT_CROP)) {
            int x1 = request.get(ARG_IMAGE_CROPX1, 0);
            int y1 = request.get(ARG_IMAGE_CROPY1, 0);
            int x2 = request.get(ARG_IMAGE_CROPX2, 0);
            int y2 = request.get(ARG_IMAGE_CROPY2, 0);
            if ((x1 < x2) && (y1 < y2)) {
                newImage = ImageUtils.clip(ImageUtils.toBufferedImage(image),
                                           new int[] { x1,
                        y1 }, new int[] { x2, y2 });
            }
        } else if (request.exists(ARG_IMAGE_EDIT_ROTATE_LEFT)
                   || request.exists(ARG_IMAGE_EDIT_ROTATE_LEFT_X)) {
            newImage = ImageUtils.rotate90(ImageUtils.toBufferedImage(image),
                                           true);

        } else if (request.exists(ARG_IMAGE_EDIT_ROTATE_RIGHT)
                   || request.exists(ARG_IMAGE_EDIT_ROTATE_RIGHT_X)) {
            newImage = ImageUtils.rotate90(ImageUtils.toBufferedImage(image),
                                           false);

        }
        if (newImage != null) {
            ImageUtils.waitOnImage(newImage);
            putImage(entry, newImage);
            File f = entry.getFile();
            getStorageManager().checkReadFile(f);
            if ((f != null) && f.canWrite()) {
                File entryDir =
                    getStorageManager().getEntryDir(entry.getId(), true);
                File original = new File(entryDir + "/" + "originalimage");
                if ( !original.exists()) {
                    IOUtil.copyFile(f, original);
                }
                ImageUtils.writeImageToFile(newImage, f);
            }

            return true;
        }

        return false;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param outputType _more_
     * @param group _more_
     * @param subGroups _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputGroup(Request request, OutputType outputType,
                              Entry group, List<Entry> subGroups,
                              List<Entry> entries)
            throws Exception {
        Result result = makeResult(request, group, entries);
        addLinks(request, result, new State(group, subGroups, entries));

        return result;
    }

    /**
     * _more_
     *
     * @param output _more_
     *
     * @return _more_
     */
    public String getMimeType(OutputType output) {
        if (output.equals(OUTPUT_GALLERY) || output.equals(OUTPUT_PLAYER)
                || output.equals(OUTPUT_SLIDESHOW)) {
            return repository.getMimeTypeFromSuffix(".html");
        }

        return super.getMimeType(output);
    }





    /**
     * _more_
     *
     * @param request _more_
     * @param group _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Result makeResult(Request request, Entry group,
                              List<Entry> entries)
            throws Exception {

        StringBuilder sb     = new StringBuilder();
        OutputType    output = request.getOutput();
        if (entries.size() == 0) {
            sb.append("<b>Nothing Found</b><p>");

            return new Result("Query Results", sb, getMimeType(output));
        }

        if (output.equals(OUTPUT_GALLERY)) {
            boolean useAttachment = request.get("useAttachment", false);
            getWikiManager().makeGallery(
                request,
                getWikiManager().getImageEntries(
                    request, entries, useAttachment), new Hashtable(), sb);

            return new Result(group.getName(), sb, getMimeType(output));

        }

        String playerPrefix = "";
        String playerVar    = "";
        int    col          = 0;
        String firstImage   = "";

        if (output.equals(OUTPUT_PLAYER)) {
            sb = new StringBuilder();
            makePlayer(request, group, entries, sb, true, true);
            //            sb.append(tmp);
            //            sb.append(HtmlUtils.leftRight(getSortLinks(request), fullUrl));
        } else if (output.equals(OUTPUT_SLIDESHOW)) {
            for (int i = entries.size() - 1; i >= 0; i--) {
                Entry entry = entries.get(i);
                if ( !entry.isImage()) {
                    continue;
                }
                String url = HtmlUtils.url(
                                 request.makeUrl(repository.URL_ENTRY_GET)
                                 + "/"
                                 + getStorageManager().getFileTail(
                                     entry), ARG_ENTRYID, entry.getId());
                String thumburl =
                    HtmlUtils.url(
                        request.makeUrl(repository.URL_ENTRY_GET) + "/"
                        + getStorageManager().getFileTail(
                            entry), ARG_ENTRYID, entry.getId(),
                                    ARG_IMAGEWIDTH, "" + 100);
                String entryUrl = getEntryLink(request, entry);
                request.put(ARG_OUTPUT, OutputHandler.OUTPUT_HTML);
                String title =
                    entry.getTypeHandler().getEntryContent(request, entry,
                        true, false).toString();
                request.put(ARG_OUTPUT, output);
                title = title.replace("\"", "\\\"");
                title = title.replace("\n", " ");
                sb.append("addImage(" + HtmlUtils.quote(url) + ","
                          + HtmlUtils.quote(thumburl) + ","
                          + HtmlUtils.quote(title) + ");\n");

            }
            String template =
                repository.getResource(
                    "/org/ramadda/repository/resources/web/slideshow.html");
            template = template.replace("${imagelist}", sb.toString());
            template = StringUtil.replace(template, "${root}",
                                          repository.getUrlBase());
            sb = new StringBuilder(template);
        }


        StringBuilder finalSB = new StringBuilder();
        showNext(request, new ArrayList<Entry>(), entries, finalSB);

        finalSB.append(HtmlUtils.p());
        finalSB.append(sb);

        return new Result(group.getName(), finalSB, getMimeType(output));

    }


    /**
     * _more_
     *
     * @param request _more_
     * @param mainEntry _more_
     * @param entries _more_
     * @param finalSB _more_
     * @param addHeader _more_
     * @param checkSort _more_
     *
     * @throws Exception _more_
     */
    public void makePlayer(Request request, Entry mainEntry,
                           List<Entry> entries, Appendable finalSB,
                           boolean addHeader, boolean checkSort)
            throws Exception {

        boolean       useAttachment = request.get("useAttachment", false);
        String        playerPrefix  = "imageplayer_" + HtmlUtils.blockCnt++;
        String        playerVar     = playerPrefix + "Var";

        StringBuilder sb            = new StringBuilder();
        if (entries.size() == 0) {
            finalSB.append("<b>Nothing Found</b><p>");

            return;
        }

        if (checkSort && !request.exists(ARG_ASCENDING)) {
            entries = getEntryUtil().sortEntriesOnDate(entries, true);
        }

        int    col        = 0;
        String firstImage = "";

        int    cnt        = 0;
        for (int i = entries.size() - 1; i >= 0; i--) {
            Entry  entry = entries.get(i);
            String url   = getImageUrl(request, entry);
            if (url == null) {
                if (useAttachment) {
                    List<String> imageUrls =
                        getMetadataManager().getImageUrls(request, entry);
                    if (imageUrls.size() > 0) {
                        url = imageUrls.get(0);
                    }
                }
            }


            if (url == null) {
                continue;
            }
            if (cnt == 0) {
                firstImage = url;
            }
            String entryUrl = getEntryLink(request, entry);
            String dttm     = getEntryUtil().formatDate(request, entry);
            String title =
                "<table width=\"100%\" cellspacing=\"0\" cellpadding=\"0\">";
            title +=
                "<tr><td><div class=\"imageplayer-title\">" + entryUrl
                + "</div></td><td align=right><div class=\"imageplayer-title-date\">"
                + dttm + "</div></td></table>";
            title = title.replace("\"", "\\\"");
            sb.append(playerVar + ".addImage(" + HtmlUtils.quote(url) + ","
                      + HtmlUtils.quote(title) + ", " + HtmlUtils.quote(dttm)
                      + ");\n");
            cnt++;
        }

        String playerTemplate =
            repository.getResource(
                "/org/ramadda/repository/resources/web/imageplayer.html");
        playerTemplate = playerTemplate.replaceAll("\\$\\{imagePlayerVar\\}",
                playerVar);
        playerTemplate =
            playerTemplate.replaceAll("\\$\\{imagePlayerPrefix\\}",
                                      playerPrefix);


        List<String> playerArgs = new ArrayList<String>();

        if (request.get("loopstart", false)) {
            playerArgs.add("autostart");
            playerArgs.add("true");
        } else {
            Object v = mainEntry.getTypeHandler().getEntryValue(mainEntry,
                           "autostart");
            if ((v != null) && v.toString().equals("true")) {
                playerArgs.add("autostart");
                playerArgs.add("true");
            }
        }




        if (request.get("loopdelay", 0) > 0) {
            playerArgs.add("delay");
            playerArgs.add("" + request.get("loopdelay", 0));
        } else {
            Object v = mainEntry.getTypeHandler().getEntryValue(mainEntry,
                           "delay");
            if (v != null) {
                int delay = new Integer(v.toString()).intValue();
                if (delay > 0) {
                    playerArgs.add("delay");
                    playerArgs.add("" + delay);
                }
            }
        }

        playerTemplate = playerTemplate.replaceAll("\\$\\{imageArgs\\}",
                Json.map(playerArgs));

        String widthAttr = "";
        int    width     = request.get(ARG_WIDTH, 600);
        if (width > 0) {
            widthAttr = HtmlUtils.attr(HtmlUtils.ATTR_WIDTH, "" + width);
        }
        String imageHtml = "<IMG class=\"imageplayer-image\" id=\""
                           + playerPrefix + "animation\" BORDER=\"0\" "
                           + widthAttr + HtmlUtils.attr("SRC", firstImage)
                           + " ALT=\"Loading image\">";

        String tmp = playerTemplate.replace("${imagelist}", sb.toString());
        tmp = tmp.replace("${imagehtml}", imageHtml);
        tmp = StringUtil.replace(tmp, "${root}", repository.getUrlBase());
        sb  = new StringBuilder();



        sb.append(tmp);
        if (addHeader) {
            String fullUrl       = "";
            String originalWidth = request.getString(ARG_WIDTH, null);
            if (width > 0) {
                request.put(ARG_WIDTH, "0");
                fullUrl = HtmlUtils.href(request.getUrl(),
                                         msg("Use image width"));
            } else {
                request.put(ARG_WIDTH, "600");
                fullUrl = HtmlUtils.href(request.getUrl(),
                                         msg("Use fixed width"));
            }
            if (originalWidth != null) {
                request.put(ARG_WIDTH, originalWidth);
            } else {
                request.remove(ARG_WIDTH);
            }
            sb.append(HtmlUtils.leftRight(getSortLinks(request), fullUrl));
        }
        finalSB.append(sb);

    }




    /**
     *
     * public void makeSlideshow(Request request, List<Entry> entries,
     *                      StringBuilder finalSB, boolean addHeader)
     *       throws Exception {
     *   StringBuilder sb = new StringBuilder();
     *   if (entries.size() == 0) {
     *       finalSB.append("<b>Nothing Found</b><p>");
     *       return;
     *   }
     *
     *   if ( !request.exists(ARG_ASCENDING)) {
     *       entries = getEntryUtil().sortEntriesOnDate(entries, true);
     *   }
     *   finalSB.append(
     *       HtmlUtils.importJS(getRepository().getFileUrl("/lib/slides/js/slides.min.jquery.js")));
     *   String slidesTemplate = repository.getResource("ramadda.html.slides");
     *   System.out.println(slidesTemplate);
     *   finalSB.append(slidesTemplate);
     *   for (int i = entries.size() - 1; i >= 0; i--) {
     *       Entry  entry = entries.get(i);
     *       String url   = getImageUrl(request, entry);
     *       if (url == null) {
     *           continue;
     *       }
     *       String entryUrl = getEntryLink(request, entry);
     *       String title = entry.getName();
     *       //            title += "<tr><td><b>Image:</b> " + entryUrl
     *       //                     + "</td><td align=right>"
     *       //                     + new Date(entry.getStartDate());
     *       sb.append("addImage(" + HtmlUtils.quote(url) + ","
     *                 + HtmlUtils.quote(title) + ");\n");
     *       cnt++;
     *   }
     *
     *   String playerTemplate = repository.getResource(PROP_HTML_IMAGEPLAYER);
     *   String widthAttr      = "";
     *   int    width          = request.get(ARG_WIDTH, 600);
     *   if (width > 0) {
     *       widthAttr = HtmlUtils.attr(HtmlUtils.ATTR_WIDTH, "" + width);
     *   }
     *   String imageHtml = "<IMG id=\"animation\" BORDER=\"0\" "
     *                      + widthAttr + HtmlUtils.attr("SRC", firstImage)
     *                      + " ALT=\"image\">";
     *
     *   String tmp = playerTemplate.replace("${imagelist}", sb.toString());
     *   tmp = tmp.replace("${imagehtml}", imageHtml);
     *   tmp = StringUtil.replace(tmp, "${root}", repository.getUrlBase());
     *   if (addHeader) {
     *       String fullUrl = "";
     *       if (width > 0) {
     *           request.put(ARG_WIDTH, "0");
     *           fullUrl = HtmlUtils.href(request.getUrl(),
     *                                   msg("Use image width"));
     *       } else {
     *           request.put(ARG_WIDTH, "600");
     *           fullUrl = HtmlUtils.href(request.getUrl(),
     *                                   msg("Use fixed width"));
     *       }
     *       sb = new StringBuilder(HtmlUtils.leftRight(getSortLinks(request),
     *               fullUrl));
     *   } else {
     *       sb = new StringBuilder();
     *   }
     *   sb.append(tmp);
     *   finalSB.append(sb);
     * }
     *
     */


}
