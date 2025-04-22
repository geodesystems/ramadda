/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.output;

import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.job.JobManager;
import org.ramadda.repository.type.*;
import org.ramadda.repository.util.FileWriter;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;

import org.ramadda.util.JsonUtil;
import org.ramadda.util.Utils;
import org.ramadda.util.ImageUtils;

import org.ramadda.util.sql.SqlUtil;

import org.w3c.dom.*;

import ucar.unidata.ui.AnimatedGifEncoder;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.awt.Color;
import java.awt.Graphics;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Toolkit;

import java.awt.geom.Rectangle2D;
import java.awt.image.*;

import java.io.*;

import java.io.File;

import java.net.*;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

import java.util.ArrayList;

import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import java.util.regex.*;

import java.util.zip.*;

import javax.imageio.*;
import javax.imageio.stream.ImageOutputStream;

/**
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */

@SuppressWarnings("unchecked")
public class ImageOutputHandler extends OutputHandler {

    public static final String ARG_IMAGE_STYLE = "image.style";

    public static final String ARG_IMAGE_EDIT = "image.edit";

    public static final String ARG_IMAGE_RESIZE = "image.resize";    

    public static final String ARG_CAPTION = "caption";

    public static final String ARG_CAPTION_TOP = "caption.top";

    public static final String ARG_CAPTION_BOTTOM = "caption.bottom";

    public static final String ARG_IMAGE_APPLY_TO_GROUP =
        "image.applytogroup";

    public static final String ARG_IMAGE_UNDO = "image_undo";

    public static final String ARG_IMAGE_EDIT_RESIZE = "image.edit.resize";

    public static final String ARG_IMAGE_EDIT_WIDTH = "image.edit.width";

    public static final String ARG_IMAGE_EDIT_CROP = "image.edit.crop";

    public static final String ARG_IMAGE_EDIT_MATTE = "image.edit.matte";

    public static final String ARG_IMAGE_EDIT_REDEYE = "image.edit.redeye";

    public static final String ARG_IMAGE_EDIT_TRANSPARENT =
        "image.edit.transparent";

    public static final String ARG_IMAGE_CROPX1 = "image.edit.cropx1";

    public static final String ARG_IMAGE_CROPY1 = "image.edit.cropy1";

    public static final String ARG_IMAGE_CROPX2 = "image.edit.cropx2";

    public static final String ARG_IMAGE_CROPY2 = "image.edit.cropy2";

    public static final String ARG_IMAGE_EDIT_ROTATE_LEFT =
        "image.edit.rotate.left";

    public static final String ARG_IMAGE_EDIT_ROTATE_LEFT_X =
        "image.edit.rotate.left.x";

    public static final String ARG_IMAGE_EDIT_ROTATE_LEFT_Y =
        "image.edit.rotate.left.y";

    public static final String ARG_IMAGE_EDIT_ROTATE_RIGHT =
        "image.edit.rotate.right";

    public static final String ARG_IMAGE_EDIT_ROTATE_RIGHT_X =
        "image.edit.rotate.right.x";

    public static final String ARG_IMAGE_EDIT_ROTATE_RIGHT_Y =
        "image.edit.rotate.right.y";

    public static final String CHANGE_RESIZE = "resize";
    public static final String CHANGE_GRAYSCALE = "grayscale";
    public static final String CHANGE_THUMBNAIL = "thumbnail";		

    public static final OutputType OUTPUT_GALLERY = new OutputType("Gallery",
                                                        "image.gallery",
                                                        OutputType.TYPE_VIEW,
                                                        "", ICON_IMAGES);

    public static final OutputType OUTPUT_ZOOM = new OutputType("Image Zoom",
                                                     "image.zoom",
                                                     OutputType.TYPE_VIEW,
                                                     "", ICON_IMAGES);

    public static final OutputType OUTPUT_FLIPCARDS = new OutputType("Flip Cards",
                                                     "image.flipcards",
                                                     OutputType.TYPE_VIEW,
                                                     "", ICON_IMAGES);    

    public static final OutputType OUTPUT_COLLAGE =
        new OutputType("Make Collage", "image.collage", OutputType.TYPE_VIEW,
                       "", ICON_IMAGES);

    public static final OutputType OUTPUT_LABELER =
        new OutputType("Make Labeled Images", "image.labeler",
                       OutputType.TYPE_VIEW, "", ICON_IMAGES);

    public static final OutputType OUTPUT_ANIMATEDGIF =
        new OutputType("Make Animated Gif", "image.animatedgif",
                       OutputType.TYPE_VIEW, "", ICON_IMAGES);

    public static final OutputType OUTPUT_B64 =
        new OutputType("Base64 Image", "image.b64", OutputType.TYPE_FILE, "",
                       ICON_IMAGES);

    public static final OutputType OUTPUT_VIDEO =
        new OutputType("Play Video", "image.video", OutputType.TYPE_VIEW, "",
                       ICON_IMAGES);

    public static final OutputType OUTPUT_PLAYER =
        new OutputType("Image Player", "image.player", OutputType.TYPE_VIEW,
                       "", ICON_IMAGES);

    public static final OutputType OUTPUT_SLIDESHOW =
        new OutputType("Slideshow", "image.slideshow", OutputType.TYPE_VIEW,
                       "", ICON_IMAGES);

    public static final OutputType OUTPUT_EDIT = new OutputType("Edit Image",
                                                     "image.edit",
                                                     OutputType.TYPE_VIEW,
                                                     "", ICON_IMAGES);

    public static final OutputType OUTPUT_CHANGE = new OutputType("Change Image",
                                                     "image.change",
                                                     OutputType.TYPE_EDIT,
                                                     "", ICON_IMAGES);    

    public static final OutputType OUTPUT_CAPTION =
        new OutputType("Caption Image", "image.caption",
                       OutputType.TYPE_VIEW, "", ICON_IMAGES);

    public static final OutputType OUTPUT_STREETVIEW =
        new OutputType("Caption Image", "streetview",
                       OutputType.TYPE_INTERNAL, "", ICON_IMAGES);

    private String streetviewKey;

    public ImageOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        addType(OUTPUT_GALLERY);
        addType(OUTPUT_ZOOM);
	addType(OUTPUT_FLIPCARDS);
        addType(OUTPUT_PLAYER);
        //        addType(OUTPUT_SLIDESHOW);
        addType(OUTPUT_CAPTION);
        addType(OUTPUT_EDIT);
        addType(OUTPUT_CHANGE);	
        addType(OUTPUT_VIDEO);
        addType(OUTPUT_COLLAGE);
        addType(OUTPUT_LABELER);
        addType(OUTPUT_ANIMATEDGIF);
        addType(OUTPUT_B64);

        addType(OUTPUT_STREETVIEW);
    }

    public void getEntryLinks(Request request, State state, List<Link> links)
            throws Exception {

        if (state.entry != null) {
            if (state.entry.isFile()) {
                //                if (state.entry.isImage()) {
                //                    links.add(makeLink(request, state.getEntry(), OUTPUT_CAPTION));
                //                }
                String extension =
                    IO.getFileExtension(
                        state.entry.getResource().getPath()).toLowerCase();
                if (extension.equals(".mp3") || extension.equals(".mp4")
                        || extension.equals(".mpg")) {
                    links.add(makeLink(request, state.getEntry(),
                                       OUTPUT_VIDEO));
                }
            }
	    if (state.entry.getResource().isEditableImage() &&
		getAccessManager().canDoEdit(request, state.entry)) {		
                File f = state.entry.getFile();
                if ((f != null) && f.canWrite()) {
		    Link hr = new Link(true);
                    hr.setLinkType(OutputType.TYPE_EDIT);		    
		    links.add(hr);
                    Link link = new Link(repository.getUrlBase()
                                         + "/lib/tui/tui?entryid="
                                         + state.entry.getId(), ICON_IMAGES,
                                             "Edit Image");
                    link.setLinkType(OutputType.TYPE_EDIT);
                    links.add(link);

                    links.add(makeLink(request, state.getEntry(),
                                       OUTPUT_CHANGE));
                }
            }
        }

        List<Entry> entries = state.getAllEntries();
        if (entries.size() == 0) {
            return;
        }

        boolean ok = false;
        if (entries.size() > 0) {
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
        boolean ok2 = (state.getEntry() != null)
                      && state.getEntry().isImage();
        if (ok || ok2) {
            //            links.add(makeLink(request, state.getEntry(), OUTPUT_SLIDESHOW));
	    Link hr = new Link(true);
	    hr.setLinkType(OutputType.TYPE_VIEW);		    
	    links.add(hr);
            links.add(makeLink(request, state.getEntry(), OUTPUT_GALLERY));
            links.add(makeLink(request, state.getEntry(), OUTPUT_ZOOM));
            links.add(makeLink(request, state.getEntry(), OUTPUT_PLAYER));
            links.add(makeLink(request, state.getEntry(), OUTPUT_FLIPCARDS));	    
            links.add(makeLink(request, state.getEntry(), OUTPUT_COLLAGE));
            if (repository.getProperty("service.imagemagick") != null) {
                links.add(makeLink(request, state.getEntry(),
                                   OUTPUT_ANIMATEDGIF));
            }
        }
        if (ok2) {
            links.add(makeLink(request, state.getEntry(), OUTPUT_LABELER));
            links.add(makeLink(request, state.getEntry(), OUTPUT_B64));
        }
    }

    private Hashtable<String, Image> imageCache = new Hashtable<String,
                                                      Image>();

    private Image getImage(Entry entry) {
        Image image = imageCache.get(entry.getId());
        if (image == null) {
            image = ImageUtils.readImage(entry.getResource().getPath());
            //Keep the cache size low
            if (imageCache.size() > 5) {
                imageCache = new Hashtable<String, Image>();
            }
            imageCache.put(entry.getId(), image);
        }

        return image;
    }

    private void putImage(Entry entry, Image image) {
        imageCache.put(entry.getId(), image);
    }

    public Result outputEntry(Request request, OutputType outputType,
                              Entry entry)
            throws Exception {

        StringBuffer sb = new StringBuffer();

        if (outputType.equals(OUTPUT_B64)) {
            return makeB64(request, entry);
        }

        if (outputType.equals(OUTPUT_LABELER)) {
            List<Entry> entries = new ArrayList<Entry>();
            entries.add(entry);

            return makeLabels(request, entry, entries);
        }

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
                sb.append(HU.p());
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

        if (outputType.equals(OUTPUT_CHANGE)) {
            return processChange(request, entry);
        }

        //        if (true || output.equals(OUTPUT_GALLERY)) {
        List<Entry> entries = new ArrayList<Entry>();
        entries.add(entry);

        return makeResult(request, entry, entries);
        //  }

        //        return new Result("", new StringBuilder("NA"));
    }

    public Result processChange(Request request, Entry entry) throws Exception {
        if ( !getAccessManager().canDoEdit(request, entry)) {
            throw new AccessException("Cannot edit image", null);
        }
        StringBuilder sb = new StringBuilder();
        getPageHandler().entrySectionOpen(request, entry, sb, "Image Resize");

	String theFile = entry.getResource().getPath();
	Image image = ImageUtils.readImage(theFile);

	if(request.exists(CHANGE_RESIZE)) {
	    sb.append(messageNote("Image resized"));
	    int width = request.get("imagewidth",600);
	    image = ImageUtils.resize(image, width, -1);
	    ImageUtils.waitOnImage(image);
	    ImageUtils.writeImageToFile(image, theFile);
	    getEntryManager().entryFileChanged(request, entry);
	} else if(request.exists(CHANGE_GRAYSCALE)) {
	    sb.append(messageNote("Image gray scaled"));
	    Image gimage = ImageUtils.grayscaleImage(image);
	    ImageUtils.writeImageToFile(gimage, theFile);
	    getEntryManager().entryFileChanged(request, entry);
	} else if(request.exists(CHANGE_THUMBNAIL)) {
	    sb.append(messageNote("Thumbnail made"));
	    getMetadataManager().addThumbnail(request,entry,request.get("deletethumbnail",false));
	} 
	sb.append(request.formPost(getRepository().URL_ENTRY_SHOW));
	sb.append(HU.formTable());
	sb.append(HU.hidden(ARG_ENTRYID, entry.getId()));
	sb.append(HU.hidden(ARG_OUTPUT, OUTPUT_CHANGE));
	sb.append(HU.formEntry(msgLabel("Width"),
			       HU.input("imagewidth","600",HU.SIZE_5) +
			       HU.space(2) +
			       HU.submit("Resize",CHANGE_RESIZE)));
	sb.append(HU.formTableClose());
	sb.append(HtmlUtils.formClose());
	sb.append("<p>");
	sb.append(request.formPost(getRepository().URL_ENTRY_SHOW));
	sb.append(HU.hidden(ARG_ENTRYID, entry.getId()));
	sb.append(HU.hidden(ARG_OUTPUT, OUTPUT_CHANGE));
	sb.append(HU.submit("Convert to gray scale",CHANGE_GRAYSCALE));
	sb.append(HtmlUtils.formClose());
	sb.append("<p>");
	sb.append(request.formPost(getRepository().URL_ENTRY_SHOW));
	sb.append(HU.hidden(ARG_ENTRYID, entry.getId()));
	sb.append(HU.hidden(ARG_OUTPUT, OUTPUT_CHANGE));

	sb.append(HU.submit("Make Thumbnail",CHANGE_THUMBNAIL));
	sb.append(HU.space(1));
	sb.append(HU.labeledCheckbox("deletethumbnail","true",true,
				     "Delete any existing thumbnails"));
	sb.append(HtmlUtils.formClose());
	sb.append("<hr>");
	sb.append("Image width: ");
	sb.append(image.getWidth(null));
	sb.append("<br>");
	String imgUrl = entry.getTypeHandler().getEntryResourceUrl(request, entry,false,true);
	sb.append(HU.img(imgUrl, "", ""));
        getPageHandler().entrySectionClose(request, entry, sb);
        Result result = new Result("", sb);
        getEntryManager().addEntryHeader(request, entry, result);
	return result;
    }

    public Result processTui(Request request) throws Exception {

        Entry entry = getEntryManager().getEntry(request,
                          request.getString(ARG_ENTRYID, ""));

        if (request.exists(ARG_IMAGE_UNDO)) {
            try {
                if ( !getAccessManager().canDoEdit(request, entry)) {
                    return new Result(
                        new StringBuilder(
                            "{\"code\":\"error\",\"message\":\"Cannot edit image\"}"), "text/plain", false);
                }
                File f = entry.getFile();
                if ((f != null) && f.canWrite()) {
                    File entryDir =
                        getStorageManager().getEntryDir(entry.getId(), true);
                    int    version     = 0;
                    File   versionFile = null;
                    String extension   =
                        IO.getFileExtension(f.toString());
                    while (true) {
                        File file = new File(entryDir + "/" + "version"
                                             + version + "." + extension);
                        if ( !file.exists()) {
                            break;
                        }
                        versionFile = file;
                        version++;
                    }
                    if (versionFile != null) {
                        IOUtil.copyFile(versionFile, f);
                        versionFile.delete();
			getEntryManager().updateEntry(request, entry);
                    }
                }

                return new Result(
                    new StringBuilder(
                        "{\"code\":\"ok\",\"message\":\"Image undone\"}"), "text/plain", false);
            } catch (Exception e) {
                return new Result(
                    new StringBuilder(
                        "{\"code\":\"error\",\"message\":\"" + e.getMessage()
                        + "\"}"), "text/plain", false);
            }
        }

        if (request.exists("imagecontents")) {
            try {
                if ( !getAccessManager().canDoEdit(request, entry)) {
                    return new Result(
                        new StringBuilder(
                            "{\"code\":\"error\",\"message\":\"Cannot edit image\"}"), "text/plain", false);
                }
                String contents = request.getString("imagecontents", "");
                int    index    = contents.indexOf("base64,");
                contents = contents.substring(index + 7);
                byte[] bytes = Utils.decodeBase64(contents);
                File   f     = entry.getFile();
                getStorageManager().checkReadFile(f);
                if ((f != null) && f.canWrite()) {
                    File entryDir =
                        getStorageManager().getEntryDir(entry.getId(), true);

                    int    version     = 0;
                    File   versionFile = null;
                    String extension   =
                        IO.getFileExtension(f.toString());
                    while (true) {
                        File file = new File(entryDir + "/" + "version"
                                             + version + "." + extension);
                        if ( !file.exists()) {
                            versionFile = file;

                            break;
                        }
                        version++;
                    }
                    IOUtil.copyFile(f, versionFile);
                    IOUtil.writeBytes(f, bytes);
		    getEntryManager().updateEntry(request, entry);
                }

                return new Result(
                    new StringBuilder(
                        "{\"code\":\"ok\",\"message\":\"Image saved\"}"), "text/plain", false);
            } catch (Exception e) {
                System.err.println("Error: " + e);

                return new Result(
                    new StringBuilder(
                        "{\"code\":\"error\",\"message\":\"" + e.getMessage()
                        + "\"}"), "text/plain", false);
            }
        }

        StringBuilder sb = new StringBuilder();
        getPageHandler().entrySectionOpen(request, entry, sb, "");

        int versions = 0;
        String extension =
            IO.getFileExtension(entry.getResource().getPath());
        File entryDir = getStorageManager().getEntryDir(entry.getId(), true);
        while (true) {
            File file = new File(entryDir + "/" + "version" + versions + "."
                                 + extension);
            if ( !file.exists()) {
                break;
            }
            versions++;
        }
        if (getAccessManager().canDoEdit(request, entry)) {
            String save =
                "<div style='display:inline-block;' class='ramadda-button' onclick='imageEditorSave();'>Save Image</div>"
                + "&nbsp;&nbsp;<div style='display:inline-block;' id='imageeditor_message'></div>";
            String undo = HtmlUtils.span(((versions > 0)
                                          ? (versions + " version"
                                             + ((versions > 1)
                    ? "s "
                    : " "))
                                          : ""), HtmlUtils.id(
                                              "imageversions")) + " <div style='display:inline-block;' class='ramadda-button' onclick='imageEditorUndo();'>Undo</div>";

            sb.append(HtmlUtils.leftRight(save, undo));
        }

        String url = getImageUrl(request, entry, true);
        sb.append(
            HtmlUtils.formPost(
                getRepository().getUrlBase() + "/lib/tui/tui",
                HtmlUtils.id("imageeditform")));
        sb.append(HtmlUtils.hidden(ARG_ENTRYID, entry.getId()));
        //        sb.append(HtmlUtils.hidden(ARG_OUTPUT, OUTPUT_EDIT));
        //        sb.append(HtmlUtils.hidden("imagecontents", "", HtmlUtils.id("imagecontents")));

        String template =
            repository.getResource(
                "/org/ramadda/repository/resources/web/imageeditor.html");

        /*
        template = IOUtil.readContents(
            "/org/ramadda/repository/resources/web/imageeditor.html",
            getClass());
        */
        template =
            template.replace("${imageurl}", url).replace("${imagename}",
                             entry.getName()).replace("${versions}",
                                 "" + versions);
        template = getPageHandler().applyBaseMacros(template);
        sb.append(template);
        sb.append(HtmlUtils.formClose());

        getPageHandler().entrySectionClose(request, entry, sb);
        Result result = new Result("", sb);
        getEntryManager().addEntryHeader(request, entry, result);

        return result;

    }

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

    public Result outputEntryStreetview(Request request, Entry entry)
            throws Exception {
        if (isStreetviewEnabled()) {
            String googleUrl =
                "https://maps.googleapis.com/maps/api/streetview?size=600x300&location={lat},{lon}&heading={heading}&pitch=0&key={key}";
            double[] ll      = entry.getCenter(request);
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

    public boolean isStreetviewEnabled() {
        if (streetviewKey == null) {
            streetviewKey = repository.getProperty("google.streetview.key",
                    "");
        }

        return streetviewKey.length() > 0;
    }

    private boolean processImage(Request request, Entry entry, Image image)
            throws Exception {
        if ( !getAccessManager().canDoEdit(request, entry)) {
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

    @Override
    public Result outputGroup(Request request, OutputType outputType,
                              Entry group, List<Entry> children)
            throws Exception {
        Result result = makeResult(request, group, children);
        addLinks(request, result, new State(group, children));

        return result;
    }

    public String getMimeType(OutputType output) {
        if (output.equals(OUTPUT_GALLERY) || output.equals(OUTPUT_ZOOM)
	    || output.equals(OUTPUT_FLIPCARDS)
	    || output.equals(OUTPUT_PLAYER)
	    || output.equals(OUTPUT_SLIDESHOW)) {
            return repository.getMimeTypeFromSuffix(".html");
        }

        return super.getMimeType(output);
    }

    private Result makeResult(Request request, Entry group,
                              List<Entry> entries)
            throws Exception {

        StringBuilder sb     = new StringBuilder();
        OutputType    output = request.getOutput();
        if (entries.size() == 0) {
            sb.append("<b>No image entries found</b><p>");

            return new Result("Query Results", sb);
        }

        if (output.equals(OUTPUT_ZOOM)) {
            getPageHandler().entrySectionOpen(request, group, sb,
                    "Image Zoom");
            String zoomTemplate =
                "{{display_imagezoom height=\"300\" doEntries=\"true\" addImages=\"true\" }}";
            sb.append(getWikiManager().wikifyEntry(request, group,
                    zoomTemplate));
            getPageHandler().entrySectionClose(request, group, sb);
        } else if (output.equals(OUTPUT_FLIPCARDS)) {
            getPageHandler().entrySectionOpen(request, group, sb,
                    "Flip Cards");
            String template ="{{flipcards  tag=card  height=150  width=150  includeChildren=false  addTags=false showSnippet=false  showSnippetHover=true  showLink=false  showHeading=true  showDate=true}}"; 
            sb.append(getWikiManager().wikifyEntry(request, group,
                    template));
            getPageHandler().entrySectionClose(request, group, sb);
        } else if (output.equals(OUTPUT_GALLERY)) {
            boolean useAttachment = request.get("useAttachment", false);
            getPageHandler().entrySectionOpen(request, group, sb, "Gallery");

            getWikiManager().makeGallery(
                request, null,
                getEntryManager().getImageEntries(request, entries, useAttachment), new Hashtable(), sb);
            getPageHandler().entrySectionClose(request, group, sb);
            return new Result(group.getName(), sb, getMimeType(output));
        }

        if (output.equals(OUTPUT_COLLAGE)) {
            return makeCollage(request, group, entries);
        }
        if (output.equals(OUTPUT_LABELER)) {
            return makeLabels(request, group, entries);
        }
        if (output.equals(OUTPUT_ANIMATEDGIF)) {
            return makeAnimatedGif(request, group, entries);
        }

        String playerVar    = "";
        int    col          = 0;
        String firstImage   = "";

        if (output.equals(OUTPUT_PLAYER)) {
            sb = new StringBuilder();
            getPageHandler().entrySectionOpen(request, group, sb,
                    "Image Player");
            makePlayer(request, group, entries, null, sb,  true);
            getPageHandler().entrySectionClose(request, group, sb);
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
		StringBuilder tb  = new StringBuilder();
		entry.getTypeHandler().getEntryContent(request, entry,
						       true, false, null,false,tb);
                request.put(ARG_OUTPUT, output);
		String title = tb.toString();
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
        showNext(request, entries, finalSB);

        finalSB.append(HtmlUtils.p());
        finalSB.append(sb);

        return new Result(group.getName(), finalSB, getMimeType(output));

    }

    private static final String FILL_FLOW = "flow";
    //    private static final String FILL_FLOW = "flow";

    private Result makeCollage(Request request, Entry entry,
                               List<Entry> entries)
            throws Exception {
        if (request.exists(ARG_SUBMIT)) {
            return processCollage(request, entry, entries);
        }

        return makeCollageForm(request, entry, entries, null);
    }

    private Result makeCollageForm(Request request, Entry entry,
                                   List<Entry> entries, String message)
            throws Exception {

        StringBuilder sb = new StringBuilder();
        getPageHandler().entrySectionOpen(request, entry, sb,
                                          "Image Collage");
        if (message != null) {
            sb.append(message);
        }
        sb.append(request.formPost(getRepository().URL_ENTRY_SHOW));
        sb.append(HtmlUtils.hidden(ARG_ENTRYID, entry.getId()));
        sb.append(HtmlUtils.hidden(ARG_OUTPUT, OUTPUT_COLLAGE));
        sb.append(HtmlUtils.submit("Make Collage", ARG_SUBMIT));
        sb.append("<table><tr valign=top><td>");
        sb.append(HtmlUtils.formTable());
        sb.append(HtmlUtils.formEntry("Columns:",
                                      HtmlUtils.input("columns",
                                          request.getString("columns",
                                              "3"))));
        sb.append(HtmlUtils.formEntry("Width:",
                                      HtmlUtils.input("width",
                                          request.getString("width",
                                              "1000"))));
        sb.append(HtmlUtils.formEntry("Pad X:",
                                      HtmlUtils.input("padx",
                                          request.getString("padx", "15"))));
        sb.append(HtmlUtils.formEntry("Pad Y:",
                                      HtmlUtils.input("pady",
                                          request.getString("pady", "15"))));
        sb.append(HtmlUtils.formEntry("Background:",
                                      HtmlUtils.input("background",
                                          request.getString("background",
                                              "white"))));
        sb.append(HtmlUtils.formEntry("Top Label:",
                                      HtmlUtils.input("toplabel",
                                          request.getString("toplabel",
                                              ""))));
        sb.append(HtmlUtils.formEntry("Bottom Label:",
                                      HtmlUtils.input("bottomlabel",
                                          request.getString("bottomlabel",
                                              ""))));
        sb.append(HtmlUtils.formEntry("Label Color:",
                                      HtmlUtils.input("foreground",
                                          request.getString("foreground",
                                              "black"))));
        sb.append(HtmlUtils.formEntry("Matte:",
                                      HtmlUtils.input("matte",
                                          request.getString("matte", "5"))));
        sb.append(HtmlUtils.formEntry("Crop Height:",
                                      HtmlUtils.input("cropheight",
                                          request.getString("cropheight", ""))));	
        sb.append(HtmlUtils.formEntry("Matte Color:",
                                      HtmlUtils.input("mattecolor",
                                          request.getString("mattecolor",
                                              "white"))));
        sb.append(HtmlUtils.formEntry("Crop:",
                                      HtmlUtils.input("crop",
                                          request.getString("crop", ""),
                                          HtmlUtils.attr("placeholder",
                                              "top,left,bottom,right"))));
        sb.append(HtmlUtils.formEntry("",
                                      HtmlUtils.labeledCheckbox("addlabels", "true",
                                          request.get("addlabels",
						      true),"Add labels")));
        sb.append(
            HtmlUtils.formEntry(
                "",
                HtmlUtils.labeledCheckbox(
                    "sortimages", "true",
                    request.get(
				"sortimages", true),"Sort images by height")));
        sb.append(HtmlUtils.formTableClose());
        sb.append("</td><td>&nbsp;&nbsp;&nbsp;&nbsp;<td><td>");
        StringBuilder esb        = new StringBuilder();
        boolean       anyChecked = false;
        int           entryCnt   = 0;
        for (int i = 0; i < entries.size(); i++) {
            Entry child = entries.get(i);
            if ( !child.isImage()) {
                continue;
            }
            String cbxId = ARG_ENTRYID + "_" + entryCnt;
            if (request.defined(cbxId)) {
                anyChecked = true;

                break;
            }
            entryCnt++;
        }

        entryCnt = 0;
        for (int i = 0; i < entries.size(); i++) {
            Entry child = entries.get(i);
            if ( !child.isImage()) {
                continue;
            }
            List<String> urls = new ArrayList<String>();
            getMetadataManager().getThumbnailUrls(request, child, urls);
            String img = "";
            if (urls.size() > 0) {
                img = HtmlUtils.img(urls.get(0), "",
                                    HtmlUtils.attr(HtmlUtils.ATTR_WIDTH,
                                        "100"));
            } else if (child.isFile()) {
                String thumburl =
                    HtmlUtils.url(
                        request.makeUrl(repository.URL_ENTRY_GET) + "/"
                        + getStorageManager().getFileTail(
                            child), ARG_ENTRYID, child.getId(),
                                    ARG_IMAGEWIDTH, "" + 100);
                img = HtmlUtils.img(thumburl, "",
                                    HtmlUtils.attr(HtmlUtils.ATTR_WIDTH,
                                        "100"));
            }
            if (img.length() > 0) {
                img = HtmlUtils.href(
                    request.entryUrl(getRepository().URL_ENTRY_SHOW, child),
                    img);
            }
            String  cbxId   = ARG_ENTRYID + "_" + entryCnt;
            boolean checked = anyChecked
                              ? request.defined(cbxId)
                              : true;
            esb.append(HtmlUtils.checkbox(cbxId, child.getId(), checked));
            entryCnt++;
            esb.append(" ");
            esb.append(img);
            esb.append(" ");
            esb.append(child.getName());
            esb.append("<p>");
        }
        sb.append(HtmlUtils.hidden("entrycnt", entryCnt + ""));

        sb.append("<b>Images:</b><br>");
        sb.append(esb.toString());
        sb.append("</td></tr></table>");
        sb.append(HtmlUtils.hidden(ARG_OUTPUT, OUTPUT_COLLAGE));
        sb.append(HtmlUtils.formClose());
        getPageHandler().entrySectionClose(request, entry, sb);

        return new Result("", sb);

    }

    private Result processCollage(Request request, Entry entry,
                                  List<Entry> entries)
            throws Exception {

        int cropTop    = 0;
        int cropLeft   = 0;
        int cropBottom = 0;
        int cropRight  = 0;
        List<String> crops = Utils.split(request.getString("crop", ""), ",",
                                         true, true);
        if (crops.size() > 0) {
            cropTop = Integer.parseInt(crops.get(0));
        }
        if (crops.size() > 1) {
            cropLeft = Integer.parseInt(crops.get(1));
        }
        if (crops.size() > 2) {
            cropBottom = Integer.parseInt(crops.get(2));
        }
        if (crops.size() > 3) {
            cropRight = Integer.parseInt(crops.get(3));
        }
        final int[] cropArray = new int[] { cropTop, cropLeft, cropBottom,
                                            cropRight };
        int columns = request.get("columns", 3);
        int matte   = request.get("matte", 0);
        Color matteColor =
            HtmlUtils.decodeColor(request.getString("mattecolor", "white"),
                                  Color.white);
        columns = Math.min(columns, 1000);
        int width = request.get("width", 1000);
        width = Math.min(width, 10000);
        int         padx        = request.get("padx", 0);
        int         pady        = request.get("pady", 0);
        String      topLabel    = request.getString("toplabel", "");
        String      bottomLabel = request.getString("bottomlabel", "");
	int cropHeight=request.get("cropheight",-1);

        int         labelPad    = 0;
        boolean     addLabels   = request.get("addlabels", false);
        Font        labelFont   = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        FontMetrics labelFM     = null;
        if (addLabels) {
            BufferedImage dummy = new BufferedImage(1, 1,
                                      BufferedImage.TYPE_INT_ARGB);
            Graphics2D g1 = dummy.createGraphics();
            g1.setFont(labelFont);
            labelFM = g1.getFontMetrics();
            Rectangle2D rect = labelFM.getStringBounds("XXXXXX", g1);
            labelPad = ((int) rect.getHeight()) + 5;
        }
        int               entryCnt = request.get("entrycnt", 0);
        List<String>      ids      = new ArrayList<String>();
        final List<Entry> selected = new ArrayList<Entry>();
        for (int i = 0; i < entryCnt; i++) {
            if (request.defined(ARG_ENTRYID + "_" + i)) {
                String id    = request.getString(ARG_ENTRYID + "_" + i, "");
                Entry  child = getEntryManager().getEntry(request, id);
                if (child != null) {
                    selected.add(child);
                }
            }
        }

        final int[]   done       = { 0 };
        final Image[] imageArray = new Image[selected.size()];
        for (int i = 0; i < selected.size(); i++) {
            final Entry child = selected.get(i);
            final int   idx   = i;
            Misc.run(new Runnable() {
                public void run() {
                    try {
                        byte[] imageBytes =
                            IOUtil.readBytes(
					     IO.doMakeInputStream(
                                    child.getResource().getPath(), true));
                        if (imageBytes == null) {
                            System.err.println("no image:" + child);
                            return;
                        }
                        Image image = ImageIO.read(
                                          new ByteArrayInputStream(
                                              imageBytes));
                        if (image != null) {
                            if ((cropArray[0] != 0) || (cropArray[1] != 0)
                                    || (cropArray[2] != 0)
                                    || (cropArray[3] != 0)) {
                                image = ImageUtils.crop(
                                    ImageUtils.toBufferedImage(image),
                                    cropArray[0], cropArray[1], cropArray[2],
                                    cropArray[3]);
                            }
			    if(cropHeight>0) {
				int imageHeight = image.getHeight(null);
				if(imageHeight>cropHeight) {
				    image = ImageUtils.crop(
						       ImageUtils.toBufferedImage(image),
						       0,0,imageHeight-cropHeight,0);
				} else {
				}

			    }
                            imageArray[idx] = image;
                        }
                    } catch (Exception exc) {
                        System.err.println("error:" + exc);
                    } finally {
                        synchronized (done) {
                            done[0]++;
                        }
                    }
                }
            });
        }

        int tries = 0;
        while (done[0] != selected.size()) {
            if (tries++ > 60) {
                break;
            }
            Misc.sleep(500);
        }

        boolean       anyBad = false;
        StringBuilder sb     = new StringBuilder();
        for (int i = 0; i < imageArray.length; i++) {
            if (imageArray[i] == null) {
                anyBad = true;
                Entry child = selected.get(i);
                sb.append(
                    HtmlUtils.href(
                        request.entryUrl(
                            getRepository().URL_ENTRY_SHOW,
                            child), child.getName()));
                sb.append("<br>");
            }
        }
        if (anyBad) {
            return makeCollageForm(
                request, entry, entries,
                getPageHandler().showDialogError(
                    "Unable to read the images from:<br>" + sb, false));
        }

        List<Image>  images = new ArrayList<Image>();
        List<String> labels = new ArrayList<String>();
        for (int i = 0; i < selected.size(); i++) {
            Entry child = selected.get(i);
            if (imageArray[i] != null) {
                images.add(imageArray[i]);
                labels.add(child.getName());
            }
        }

        int scaledWidth = width / columns;

        if (request.get("sortimages", false)) {
            List<Utils.ObjectSorter> sort =
                new ArrayList<Utils.ObjectSorter>();
            for (int i = 0; i < images.size(); i++) {
                Image  image        = images.get(i);
                String label        = labels.get(i);
                int    iheight      = image.getHeight(null);
                int    iwidth       = image.getWidth(null);
                int    scaledHeight = scaledWidth * iheight / iwidth;
                sort.add(new Utils.ObjectSorter(new Object[] { image,
                        label }, scaledHeight, false));
            }
            Collections.sort(sort);
            images.clear();
            labels.clear();
            for (Utils.ObjectSorter o : sort) {
                Object[] pair = (Object[]) o.getObject();
                images.add((Image) pair[0]);
                labels.add((String) pair[1]);
            }
        }

        int[] rowMax     = new int[images.size() / columns + 1];
        int[] maxHeights = new int[columns];
        int[] extraPad   = new int[columns];
        for (int i = 0; i < rowMax.length; i++) {
            rowMax[i] = 0;
        }
        for (int i = 0; i < maxHeights.length; i++) {
            maxHeights[i] = pady;
            extraPad[i]   = 0;
        }
        int row = 0;
        int cnt = 0;
        for (int i = 0; i < images.size(); i++) {
            Image image   = images.get(i);
            int   iheight = image.getHeight(null);
            int   iwidth  = image.getWidth(null);
            if ((iwidth <= 0) || (iheight < 0)) {
                continue;
            }
            int scaledHeight = scaledWidth * iheight / iwidth;
            rowMax[row] = Math.max(rowMax[row], scaledHeight);
            int idx = (i % columns);
            maxHeights[idx] += pady + labelPad + scaledHeight;
            if (++cnt >= columns) {
                cnt = 0;
                row++;
            }
        }
        int maxHeight = 0;
        for (int i = 0; i < maxHeights.length; i++) {
            maxHeight = Math.max(maxHeight, maxHeights[i]);
        }
        maxHeight = 0;
        for (int i = 0; i < rowMax.length; i++) {
            maxHeight += rowMax[i];
        }
        maxHeight += pady + rowMax.length * (pady + labelPad);
        for (int i = 0; i < maxHeights.length; i++) {
            maxHeights[i] = pady;
        }
        int totalWidth  = width + (columns + 1) * padx;
        int totalHeight = maxHeight;
        BufferedImage collage = new BufferedImage(totalWidth, totalHeight,
                                    BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = collage.createGraphics();
        g.setFont(labelFont);
        labelFM = g.getFontMetrics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                           RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Color bg = HtmlUtils.decodeColor(request.getString("background",
                       "white"), Color.white);
        Color fg = HtmlUtils.decodeColor(request.getString("foreground",
                       "black"), Color.black);
        g.setColor(bg);
        g.fillRect(0, 0, totalWidth, totalHeight);
        row = 0;
        int col = 0;
        int numberOfRemainder = columns
                                - (rowMax.length * columns - images.size());
        for (int i = 0; i < images.size(); i++) {
            Image  image          = images.get(i);
            String label          = labels.get(i);
            int    iheight        = image.getHeight(null);
            int    iwidth         = image.getWidth(null);
            int    scaledHeight   = scaledWidth * iheight / iwidth;
            int    maxHeightInRow = rowMax[row];
            int    yoff           = 0;
            if (scaledHeight < maxHeightInRow) {
                yoff = (maxHeightInRow - scaledHeight) / 2;
            }
            int     x       = padx + col * (padx + scaledWidth);
            boolean lastRow = rowMax.length == row + 1;
            if (lastRow && (numberOfRemainder > 0)) {
                int delta = (columns - numberOfRemainder)
                            * (padx + scaledWidth) / 2;
                x += delta;
            }
            if (matte > 0) {
                g.setColor(matteColor);
                g.fillRect(x, maxHeights[col] + yoff, scaledWidth,
                           scaledHeight);
                if (matteColor.equals(bg)) {
                    g.setColor(Color.LIGHT_GRAY);
                    g.drawRect(x, maxHeights[col] + yoff, scaledWidth,
                               scaledHeight);
                }
            }
            int imageX      = x + matte;
            int imageY      = maxHeights[col] + yoff + matte;
            int imageWidth  = scaledWidth - 2 * matte;
            int imageHeight = scaledHeight - 2 * matte;
            g.drawImage(image, imageX, imageY, imageWidth, imageHeight, null);

            if (addLabels) {
                g.setColor(fg);
                Rectangle2D rect = labelFM.getStringBounds(label, g);
                g.drawString(label,
                             imageX + imageWidth / 2
                             - (int) (rect.getWidth() / 2), (int) (imageY
                                      + imageHeight + rect.getHeight()
                                      + matte + 5));
            }
            //                maxHeights[col]+=pady+scaledHeight;
            maxHeights[col] += pady + labelPad + maxHeightInRow;
            if (++col >= columns) {
                col = 0;
                row++;
            }
        }

        Font f   = new Font(Font.SANS_SERIF, Font.PLAIN, 24);
        Date now = new Date();
        if (Utils.stringDefined(topLabel)) {
            topLabel = topLabel.replace("${timestamp}",
                                        getDateHandler().formatDate(now));
            g.setFont(f);
            FontMetrics fm      = g.getFontMetrics();
            Rectangle2D rect    = fm.getStringBounds(topLabel, g);
            int         twidth  = (int) rect.getWidth();
            int         theight = (int) (rect.getHeight());
            collage = ImageUtils.matte(collage, theight + labelPad * 2, 0, 0,
                                       0, bg);
            g = collage.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                               RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setFont(f);
            g.setColor(fg);
            int tx = totalWidth / 2 - twidth / 2;
            g.drawString(topLabel, tx, labelPad + theight);
        }

        if (Utils.stringDefined(bottomLabel)) {
            bottomLabel = bottomLabel.replace("${timestamp}",
                    getDateHandler().formatDate(now));
            g.setFont(f);
            FontMetrics fm      = g.getFontMetrics();
            Rectangle2D rect    = fm.getStringBounds(bottomLabel, g);
            int         twidth  = (int) rect.getWidth();
            int         theight = (int) (rect.getHeight());
            int         buffer  = theight + labelPad * 2;
            collage = ImageUtils.matte(collage, 0, buffer, 0, 0, bg);
            g       = collage.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                               RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setFont(f);
            g.setColor(fg);
            int tx = totalWidth / 2 - twidth / 2;
            g.drawString(bottomLabel, tx, collage.getHeight(null) - labelPad);
        }
        File file = getStorageManager().getTmpFile("collage.png");
        ImageUtils.writeImageToFile(collage, file.toString());
        Result result = new Result(new FileInputStream(file), "image/png");
        result.setReturnFilename("collage.png");

        return result;
    }

    private Result makeLabels(Request request, Entry entry,
                              List<Entry> entries)
            throws Exception {

        List<Entry> tmp = new ArrayList<Entry>();
        for (Entry e : entries) {
            if (e.isImage()) {
                tmp.add(e);
            }
        }
        entries = tmp;
        if (entries.size() == 0) {
            return new Result("", new StringBuilder("No image files"));
        }

        OutputStream os         = null;
        FileWriter   fileWriter = null;
        for (int i = 0; i < entries.size(); i++) {
            Entry  child = entries.get(i);
            String path  = child.getResource().getPath();
            byte[] imageBytes =
                IOUtil.readBytes(IO.doMakeInputStream(path, true));
            if (imageBytes == null) {
                System.err.println("no image:" + child);

                continue;
            }
            BufferedImage image =
                ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (image == null) {
                continue;
            }
            int iwidth = image.getWidth(null);
            if (iwidth <= 0) {
                continue;
            }
            int    fontSize = (iwidth < 800)
                              ? 18
                              : (iwidth < 1200)
                                ? 24
                                : (iwidth < 1800)
                                  ? 36
                                  : (iwidth < 2400)
                                    ? 48
                                    : 56;

            String label    = child.getName();
            Font labelFont = new Font(Font.SANS_SERIF, Font.PLAIN, fontSize);
            BufferedImage dummy = new BufferedImage(1, 1,
                                      BufferedImage.TYPE_INT_ARGB);
            Graphics2D g1 = dummy.createGraphics();
            g1.setFont(labelFont);
            FontMetrics fm   = g1.getFontMetrics();

            Rectangle2D rect = fm.getStringBounds(label, g1);
            int         pad  = 5;
            image = ImageUtils.matte(image, 0,
                                     (int) rect.getHeight() + pad * 2, 0, 0,
                                     Color.white);
            int iheight = image.getHeight(null);
            if (iheight < 0) {
                continue;
            }
            Graphics2D g = image.createGraphics();
            g.setFont(labelFont);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                               RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Color fg = Color.black;
            g.setColor(fg);
            g.drawString(label, iwidth / 2 - (int) (rect.getWidth() / 2),
                         (int) (iheight - fm.getDescent() - pad));
            String name = getStorageManager().getOriginalFilename(path);
            File   file = getStorageManager().getTmpFile(name);
            String ext =
                IO.getFileExtension(path).toLowerCase().replace(".", "");
            if (ext.equals("jpg")) {
                ext = "jpeg";
            }
            if ( !ImageIO.write(image, ext, file)) {
                throw new IllegalArgumentException("Unable to write file:"
                        + file.getName() + " type:" + ext);
            }
            if (entries.size() == 1) {
                Result result = new Result(new FileInputStream(file),
                                           "image/" + ext);
                result.setReturnFilename(IO.getFileTail(path));

                return result;
            }
            if (os == null) {
                request.setReturnFilename("images.zip");
                os = request.getHttpServletResponse().getOutputStream();
                request.getHttpServletResponse().setContentType(
                    "application/zip");
                fileWriter = new FileWriter(new ZipOutputStream(os));
                fileWriter.setCompressionOn();
            }
            fileWriter.writeFile(name, new FileInputStream(file));
        }
        Result result = new Result();
        result.setNeedToWrite(false);
        fileWriter.close();

        return result;

    }

    private Result makeB64(Request request, Entry entry) throws Exception {
        String path = entry.getResource().getPath();
        byte[] bytes =
            IOUtil.readBytes(getStorageManager().getInputStream(path));
        if (bytes == null) {
            return new Result("",
                              new StringBuilder("error reading image:"
                                  + entry));
        }

        path = getStorageManager().getOriginalFilename(path);
        String ext = IO.getFileExtension(path).toLowerCase();
        path = IO.stripExtension(path) + ".b64";

        StringBuilder sb = new StringBuilder();
        sb.append("url(data:image/" + ext + ";base64,");
        sb.append(Utils.encodeBase64Bytes(bytes));
        sb.append(")");
        Result result = new Result(sb.toString(), "text");
        result.setReturnFilename(path);

        return result;
    }

    private Result makeAnimatedGif(Request request, Entry entry,
                                   List<Entry> entries)
            throws Exception {
        if (request.exists(ARG_SUBMIT)) {
            return processAnimatedGif(request, entry, entries);
        }

        return makeAnimatedGifForm(request, entry, entries, null);
    }

    private Result processAnimatedGif(final Request request, Entry entry,
                                      List<Entry> entries)
            throws Exception {

        int          entryCnt     = request.get("entrycnt", 0);
        List<String> ids          = new ArrayList<String>();
        List<Entry>  imageEntries = new ArrayList<Entry>();
        for (int i = 0; i < entryCnt; i++) {
            if (request.defined(ARG_ENTRYID + "_" + i)) {
                String id    = request.getString(ARG_ENTRYID + "_" + i, "");
                Entry  child = getEntryManager().getEntry(request, id);
                if (child != null) {
                    imageEntries.add(child);
                }
            }
        }
        if (request.get("reverse", false)) {
            List<Entry> tmp = new ArrayList<Entry>();
            for (int i = imageEntries.size() - 1; i >= 0; i--) {
                tmp.add(imageEntries.get(i));
            }
            imageEntries = tmp;
        }

        final List<Entry> selected   = imageEntries;
        final int         width      = request.get("width", 0);
        final int         maxHeight  = request.get("maxheight", 0);
        final int[]       done       = { 0 };
        final String[]    imageArray = new String[selected.size()];
        for (int i = 0; i < selected.size(); i++) {
            final Entry child = selected.get(i);
            final int   idx   = i;
            Misc.run(new Runnable() {
                public void run() {
                    try {
                        String imageFile = null;

                        if (child.isFile()) {
                            imageFile = child.getResource().getPath();
                        } else if (child.getResource().isUrl()) {
                            String tail = IO.getFileTail(
                                              child.getResource().getPath());
                            File file =
                                getStorageManager().getTmpFile(tail);
                            InputStream is =
                                IO.doMakeInputStream(
                                    child.getResource().getPath(), true);
                            BufferedOutputStream bos =
                                new BufferedOutputStream(
                                    new FileOutputStream(file));
                            IOUtil.writeTo(is, bos);
                            IO.close(bos);
                            imageFile = file.toString();
                        }

                        if (imageFile == null) {
                            return;
                        }

                        BufferedImage image = null;
                        if ((maxHeight > 0) || (width > 0)) {
                            image = ImageIO.read(new File(imageFile));
                        }

                        if (width > 0) {
                            Image newImage = image.getScaledInstance(width,
                                                 -1,
                                                 Image.SCALE_AREA_AVERAGING);
                            String newFile =
                                IO.getFileTail(imageFile).toLowerCase();
                            if ( !newFile.endsWith(".gif")
                                    || !newFile.endsWith(".png")
                                    || !newFile.endsWith(".jpg")
                                    || !newFile.endsWith(".jpeg")) {
                                newFile = IO.stripExtension(newFile)
                                          + ".png";
                            }
                            File tmp =
                                getStorageManager().getTmpFile(newFile);
                            ImageUtils.writeImageToFile(newImage,
                                    tmp.toString());
                            imageFile = tmp.toString();
                            image     = ImageUtils.toBufferedImage(newImage);
                        }

                        if ((maxHeight > 0)
                                && (image.getHeight(null) > maxHeight)) {
                            image =
                                ImageUtils.crop(ImageUtils.toBufferedImage(image),
                                           0, 0,
                                           image.getHeight(null) - maxHeight,
                                           0);
                            String newFile =
                                IO.getFileTail(imageFile).toLowerCase();
                            if ( !newFile.endsWith(".gif")
                                    || !newFile.endsWith(".png")
                                    || !newFile.endsWith(".jpg")
                                    || !newFile.endsWith(".jpeg")) {
                                newFile = IO.stripExtension(newFile)
                                          + ".png";
                            }
                            File tmp =
                                getStorageManager().getTmpFile(newFile);
                            ImageUtils.writeImageToFile(image,
                                    tmp.toString());
                            imageFile = tmp.toString();
                        }
                        imageArray[idx] = imageFile;

                    } catch (Exception exc) {
                        System.err.println("Error:" + exc);
                    } finally {
                        synchronized (done) {
                            done[0]++;
                        }
                    }
                }
            });
        }

        int tries = 0;
        while (done[0] != selected.size()) {
            if (tries++ > 60) {
                break;
            }
            Misc.sleep(500);
        }

        boolean       anyBad = false;
        StringBuilder sb     = new StringBuilder();
        for (int i = 0; i < imageArray.length; i++) {
            if (imageArray[i] == null) {
                anyBad = true;
                Entry child = selected.get(i);
                sb.append(
                    HtmlUtils.href(
                        request.entryUrl(
                            getRepository().URL_ENTRY_SHOW,
                            child), child.getName()));
                sb.append("<br>");
            }
        }
        if (anyBad) {
            return makeAnimatedGifForm(
                request, entry, entries,
                getPageHandler().showDialogError(
                    "Unable to read images from:<br>" + sb, false));
        }

        List<String> files = new ArrayList<String>();
        for (String f : imageArray) {
            files.add(f);
        }

        String       tail                  = entry.getName() + ".gif";
        File         file = getStorageManager().getTmpFile(tail);
        boolean      useGlobalPaletteValue = true;
        double       endPause              = request.get("endpause", 1.0);
        int          delay                 = request.get("delay", 100);
        int          loopCount             = request.get("loopcount", 0);
        List<String> commands              = new ArrayList<String>();
        commands.add(getRepository().getScriptPath("service.imagemagick")
                     + "/convert");
        commands.addAll(Utils.makeListFromValues("-loop", loopCount + "", "-delay",
                                       delay + "", "-dispose", "Background"));
        commands.addAll(files);
        commands.addAll(Utils.makeListFromValues("-coalesce"));
        commands.add(file.toString());
        JobManager.CommandResults results =
            getRepository().getJobManager().executeCommand(commands,
                getStorageManager().getRepositoryDir());
        Result result = new Result(new FileInputStream(file), "image/gif");
        result.setReturnFilename(tail);

        return result;
    }

    private Result makeAnimatedGifForm(Request request, Entry entry,
                                       List<Entry> entries, String message)
            throws Exception {

        StringBuilder sb = new StringBuilder();
        getPageHandler().entrySectionOpen(request, entry, sb, "Animated Gif");
        if (message != null) {
            sb.append(message);
        }
        sb.append(request.form(getRepository().URL_ENTRY_SHOW));
        sb.append(HtmlUtils.hidden(ARG_ENTRYID, entry.getId()));
        sb.append(HtmlUtils.hidden(ARG_OUTPUT, OUTPUT_ANIMATEDGIF));
        sb.append(HtmlUtils.submit("Make Animated Gif", ARG_SUBMIT));
        sb.append("<table><tr valign=top><td>");
        sb.append(HtmlUtils.formTable());
        sb.append(
            HtmlUtils.formEntry(
                "Delay:",
                HtmlUtils.input(
                    "delay", request.getString("delay", "100"),
                    HtmlUtils.SIZE_5) + " hundredths of a second"));
        /*
        sb.append(HtmlUtils.formEntry("End Pause:",
                                      HtmlUtils.input("endpause",
                                          request.getString("endpause", "1"),
                                         HtmlUtils.SIZE_5) + "  seconds"));
        */

        sb.append(
            HtmlUtils.formEntry(
                "Loop Count:",
                HtmlUtils.input(
                    "loopcount", request.getString("loopcount", "0"),
                    HtmlUtils.SIZE_5) + " 0=forever"));

        sb.append(HtmlUtils.formEntry("Image Width:",
                                      HtmlUtils.input("width",
                                          request.getString("width", ""),
                                          HtmlUtils.SIZE_5)));
        sb.append(HtmlUtils.formEntry("Max Height:",
                                      HtmlUtils.input("maxheight",
                                          request.getString("maxheight", ""),
                                          HtmlUtils.SIZE_5)));
        sb.append(
            HtmlUtils.formEntry(
                "",
                HtmlUtils.checkbox(
                    "reverse", "true",
                    request.get(
                        "reverse", false)) + " Reverse sort the images"));
        sb.append(HtmlUtils.formTableClose());
        sb.append("</td><td>&nbsp;&nbsp;&nbsp;&nbsp;<td><td>");
        StringBuilder esb        = new StringBuilder();
        boolean       anyChecked = false;
        int           entryCnt   = 0;
        for (int i = 0; i < entries.size(); i++) {
            Entry child = entries.get(i);
            if ( !child.isImage()) {
                continue;
            }
            String cbxId = ARG_ENTRYID + "_" + entryCnt;
            if (request.defined(cbxId)) {
                anyChecked = true;

                break;
            }
            entryCnt++;
        }

        entryCnt = 0;
        for (int i = 0; i < entries.size(); i++) {
            Entry child = entries.get(i);
            if ( !child.isImage()) {
                continue;
            }
            List<String> urls = new ArrayList<String>();
            getMetadataManager().getThumbnailUrls(request, child, urls);
            String img = "";
            if (urls.size() > 0) {
                img = HtmlUtils.img(urls.get(0), "",
                                    HtmlUtils.attr(HtmlUtils.ATTR_WIDTH,
                                        "100"));
            } else if (child.isFile()) {
                String thumburl =
                    HtmlUtils.url(
                        request.makeUrl(repository.URL_ENTRY_GET) + "/"
                        + getStorageManager().getFileTail(
                            child), ARG_ENTRYID, child.getId(),
                                    ARG_IMAGEWIDTH, "" + 100);
                img = HtmlUtils.img(thumburl, "",
                                    HtmlUtils.attr(HtmlUtils.ATTR_WIDTH,
                                        "100"));
            }
            if (img.length() > 0) {
                img = HtmlUtils.href(
                    request.entryUrl(getRepository().URL_ENTRY_SHOW, child),
                    img);
            }
            String  cbxId   = ARG_ENTRYID + "_" + entryCnt;
            boolean checked = anyChecked
                              ? request.defined(cbxId)
                              : true;
            esb.append(HtmlUtils.checkbox(cbxId, child.getId(), checked));
            entryCnt++;
            esb.append(" ");
            esb.append(img);
            esb.append(" ");
            esb.append(child.getName());
            esb.append("<p>");
        }
        sb.append(HtmlUtils.hidden("entrycnt", entryCnt + ""));

        sb.append("<b>Images:</b><br>");
        sb.append(esb.toString());
        sb.append("</td></tr></table>");

        sb.append(HtmlUtils.formClose());
        getPageHandler().entrySectionClose(request, entry, sb);

        return new Result("", sb);

    }

    public void makePlayer(Request request, Entry mainEntry,
                           List<Entry> entries, Hashtable props,Appendable finalSB,
                           boolean checkSort)
            throws Exception {
	if(props==null)props = new Hashtable();
	String dateFormat = Utils.getProperty(props,"dateFormat",null);
	SimpleDateFormat sdf =null;
	String serverImageWidth = Utils.getProperty(props,"serverImageWidth",null);
        boolean       useAttachment = request.get("useAttachment",
						  Utils.getProperty(props,"useAttachment",false));
        String        playerId  = HU.getUniqueId("imageplayer_");
        String        playerVar     = playerId + "Var";

        StringBuilder sb            = new StringBuilder();
        if (entries.size() == 0) {
            finalSB.append("<b>Nothing Found</b><p>");
            return;
        }

	String labelTemplate = Utils.getProperty(props,"labelTemplate",null);
        if (checkSort && !request.exists(ARG_ASCENDING)) {
            entries = getEntryUtil().sortEntriesOnDate(entries, true);
        }

        int    col        = 0;
        String firstImage = "";

        int    cnt        = 0;
	List<String> images = new ArrayList<String>();
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
	    if(serverImageWidth!=null)
		url=HU.url(url,ARG_SERVERIMAGEWIDTH,serverImageWidth);
            if (cnt == 0) {
                firstImage = url;
            }
	    String label = getEntryManager().getEntryDisplayName(entry);
	    if(labelTemplate!=null) label = labelTemplate.replace("${name}",label);

	    String entryUrl  =  HU.href(getEntryManager().getEntryURL(request, entry), label);
	    if(dateFormat!=null) {
		sdf = new SimpleDateFormat(dateFormat);
		String tz = getEntryUtil().getTimezone(request,entry);
		if(tz!=null) {
		    sdf.setTimeZone(TimeZone.getTimeZone(tz));
		}

		dateFormat=null;
	    }
            String dttm     = sdf!=null?sdf.format(entry.getStartDate()):
		getEntryUtil().formatDate(request, entry);
            entryUrl = entryUrl.replace("\"", "\\\"");
	    images.add(JsonUtil.map("url",JsonUtil.quote(url),
				    "label",JsonUtil.quote(entryUrl),
				    "date",JsonUtil.quote(dttm)));
            cnt++;
	}

        String playerTemplate =
            repository.getResource(
                "/org/ramadda/repository/htdocs/imageplayer/imageplayer.html");
        playerTemplate = playerTemplate.replace("${imagePlayerVar}", playerVar);
        playerTemplate = playerTemplate.replace("${imagePlayerId}", playerId);

        List<String> playerArgs = new ArrayList<String>();
	Utils.add(playerArgs,"id",JsonUtil.quote(playerId));

        if (request.get("loopdelay", 0) > 0) {
            Utils.add(playerArgs,"delay", request.getString("loopdelay", "0"));
        } else {
            Object v = mainEntry.getValue(request, "delay");
            if (v != null) {
                int delay =  Integer.parseInt(v.toString());
                if (delay > 0) {
		    Utils.add(playerArgs,"delay","" + delay);
                }
            }
        }

	String autoPlay = null;
	Object entryAutoPlay  = mainEntry.getValue(request,  "autostart");
	if (entryAutoPlay != null)  autoPlay = entryAutoPlay.toString();

	String small = Utils.getProperty(props,"smallButtons",null);
	if(small!=null)
	    Utils.add(playerArgs,"smallButtons",small);
	for(String[] attr:new String[][]{
		{"autoPlay",autoPlay},
		{"showControls","true"},
		{"showButtons","true"},
		{"boxesPosition",null},
		{"boxHeight",null},		
		{"showLabel","true"},
		{"showDate","true"},
		{"compact","false"},
		{"lazyLoading",null},
		{"currentImage",null},		
		{"imageHeight",null}
	    }) {
	    String key = attr[0];
	    String v  = request.getString(key,null);
	    if(v==null)
		v  = Utils.getProperty(props,key,attr[1]);
	    if(v!=null)
		Utils.add(playerArgs,key,JsonUtil.quote(v));
	}
	Utils.add(playerArgs,"images",JsonUtil.list(images));

        playerTemplate = playerTemplate.replaceAll("\\$\\{imageArgs\\}",
                JsonUtil.map(playerArgs));

        String width     = Utils.getProperty(props,"width",request.getString(ARG_WIDTH, ""));
	String imageHtml = HU.div("",HU.attr("id",playerId+"animation") +
				  HU.cssClass("imageplayer-image"));

        String tmp = playerTemplate.replace("${imagehtml}", imageHtml);
        tmp = StringUtil.replace(tmp, "${root}", repository.getUrlBase());
	if(stringDefined(width))
	    tmp = HU.div(tmp,HU.style("width:" + HU.makeDim(width,"px")));
        sb  = new StringBuilder();
	HtmlUtils.cssLink(sb,
			  getPageHandler().getCdnPath("/imageplayer/imageplayer.css"));
	HU.importJS(sb,getPageHandler().getCdnPath("/imageplayer/imageplayer.js"));
        sb.append(tmp);
        finalSB.append(sb);
    }

    /**
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        List<String> files = new ArrayList<String>();
        for (String f : args) {
            files.add(f);
        }
        boolean useGlobalPaletteValue = true;
        double  endPause              = 1.0;
        double  displayRate           = 1.0;
        double  rate                  = 1.0 / displayRate;
        int     loopCount             = 0;
        System.err.println("encoding");
        AnimatedGifEncoder.createGif("test.gif", files, loopCount,
                                     (int) (rate * 1000),
                                     (int) ((endPause == -1)
                                            ? -1
                                            : endPause
                                            * 1000), useGlobalPaletteValue);
    }

}
