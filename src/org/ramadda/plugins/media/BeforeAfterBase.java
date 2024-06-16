/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.media;


import org.ramadda.repository.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;


import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JQuery;
import org.ramadda.util.Utils;
import org.ramadda.util.WikiUtil;

import org.w3c.dom.*;

import ucar.unidata.util.Misc;

import java.awt.Dimension;
import java.awt.Image;

import java.io.FileInputStream;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

import javax.imageio.ImageIO;


/**
 *
 *
 */
public class BeforeAfterBase extends GenericTypeHandler {

    /**  */
    public static final String ARG_IMAGELAYOUT = "imagelayout";

    /**  */
    public static final String TAG_IMAGEPAIR_DEFAULT = "imagepair_default";

    /**  */
    public static final String TAG_IMAGEOVERLAY = "imageoverlay";

    /**  */
    public static final String TAG_BEFOREAFTER = "beforeafter";

    /**  */
    public static final String TAG_LEFTRIGHT = "leftright";


    /** _more_ */
    private Hashtable<String, Dimension> dimensions = new Hashtable<String,
                                                          Dimension>();


    /** _more_ */
    private static int cnt = 0;

    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public BeforeAfterBase(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isGroup() {
        return true;
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
    public Dimension getDimensions(Entry entry) throws Exception {
        Dimension dim = dimensions.get(entry.getId());
        if (dim == null) {
            Image image = ImageIO.read(new FileInputStream(entry.getFile()));
            dim = new Dimension(image.getWidth(null), image.getHeight(null));
            if ((dim.width > 0) && (dim.height > 0)) {
                dimensions.put(entry.getId(), dim);
            }
        }

        return dim;
    }


    /**
     *
     * @param sb _more_
     * @param request _more_
     * @param props _more_
     * @param entry _more_
     * @param tag _more_
     *
     * @throws Exception _more_
     */
    public void getLinks(Appendable sb, Request request, Hashtable props,
                         Entry entry, String tag)
            throws Exception {

        if ( !Utils.getProperty(props, "showLinks", false)) {
            return;
        }
        if (request.isEmbedded()) {
            return;
        }

        @SuppressWarnings("unchecked") HashSet<String> except =
            (HashSet<String>) Utils.makeHashSet(ARG_IMAGELAYOUT);
        String url;
        if ( !tag.equals(TAG_BEFOREAFTER)) {
            url = request.getUrl(except, null);
            url += "&" + ARG_IMAGELAYOUT + "=" + TAG_BEFOREAFTER;
            sb.append(HtmlUtils.href(url, "Switch to Before/After"));
            sb.append(HtmlUtils.space(2));
        }

        if ( !tag.equals(TAG_IMAGEOVERLAY)) {
            url = request.getUrl(except, null);
            url += "&" + ARG_IMAGELAYOUT + "=" + TAG_IMAGEOVERLAY;
            sb.append(HtmlUtils.href(url, "Switch to Overlay"));
            sb.append(HtmlUtils.space(2));
        }

        if ( !tag.equals(TAG_LEFTRIGHT)) {
            url = request.getUrl(except, null);
            url += "&" + ARG_IMAGELAYOUT + "=" + TAG_LEFTRIGHT;
            sb.append(HtmlUtils.href(url, "Switch to Side by Side"));
            sb.append(HtmlUtils.space(2));
        }

    }

    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props)
            throws Exception {

        String beforeAfterTag = request.getString(ARG_IMAGELAYOUT, null);
        if ( !Utils.stringDefined(beforeAfterTag)) {
            beforeAfterTag = tag;
        }
        if (beforeAfterTag.equals(TAG_IMAGEPAIR_DEFAULT)) {
            beforeAfterTag = entry.getStringValue(request,0, TAG_BEFOREAFTER);
        }
        if ( !Utils.stringDefined(beforeAfterTag)) {
            beforeAfterTag = TAG_IMAGEOVERLAY;
        }


        if (beforeAfterTag.equals(TAG_IMAGEOVERLAY)) {
            return getImageOverlay(wikiUtil, request, originalEntry, entry,
                                   tag, props);
        }
        if (beforeAfterTag.equals(TAG_BEFOREAFTER)) {
            return getBeforeAfter(wikiUtil, request, originalEntry, entry,
                                  tag, props);
        }
        if (beforeAfterTag.equals(TAG_LEFTRIGHT)) {
            return getLeftRight(wikiUtil, request, originalEntry, entry, tag,
                                props);
        }

        return super.getWikiInclude(wikiUtil, request, originalEntry, entry,
                                    tag, props);
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
    private List<Entry> getEntries(Request request, Entry entry)
            throws Exception {
        List<Entry> entries = getEntryManager().getChildren(request, entry);
        List<Entry> entriesToUse = new ArrayList<Entry>();
        for (Entry child : entries) {
            if ( !child.isImage()) {
                continue;
            }
            entriesToUse.add(child);
        }
        entriesToUse = EntryUtil.sortEntriesOn(entriesToUse,
                "entryorder,createdate", false);

        return entriesToUse;
    }

    /**
     * _more_
     *
     * @param wikiUtil _more_
     * @param request _more_
     * @param originalEntry _more_
     * @param entry _more_
     * @param tag _more_
     * @param props _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getImageOverlay(WikiUtil wikiUtil, Request request,
                                  Entry originalEntry, Entry entry,
                                  String tag, Hashtable props)
            throws Exception {

        String        overrideWidth = (String) props.get("imageWidth");
        StringBuilder sb            = new StringBuilder();
        HtmlUtils.importJS(
            sb,
            getPageHandler().makeHtdocsUrl("/beforeafter/imageoverlay.js"));
        String width = entry.getStringValue(request,1, "800").trim();
        if (width.length() == 0) {
            width = "800";
        }
        if (Utils.stringDefined(overrideWidth)) {
            width = overrideWidth;
        }

        double dwidth = Double.parseDouble(width);
        width += "px";
        List<Entry> entries = getEntries(request, entry);
        for (int i = 0; i < entries.size(); i += 2) {
            String baseId = getRepository().getGUID();
            if (i >= entries.size() - 1) {
                break;
            }
            Entry entry1 = entries.get(i);
            Entry entry2 = entries.get(i + 1);
            String url1 = HtmlUtils.url(
                              request.makeUrl(repository.URL_ENTRY_GET) + "/"
                              + getStorageManager().getFileTail(
                                  entry1), ARG_ENTRYID, entry1.getId());
            String url2 = HtmlUtils.url(
                              request.makeUrl(repository.URL_ENTRY_GET) + "/"
                              + getStorageManager().getFileTail(
                                  entry2), ARG_ENTRYID, entry2.getId());


            Dimension dim1 = getDimensions(entry1);
            Dimension dim2 = getDimensions(entry2);
            Dimension dim = new Dimension(Math.max(dim1.width, dim2.width),
                                          Math.max(dim1.height, dim2.height));
            double ratio1 = dim1.width / (double) dim1.height;
            double ratio2 = dim2.width / (double) dim2.height;

            //Put a min height for when showing in tabs. add 20 for the slider
            int h1 = (int) (dwidth / ratio1) + 20;
            int h2 = (int) (dwidth / ratio2) + 20;
            int h  = Math.max(h1, h2);
            HtmlUtils.open(sb, "div", "style",
                           "position:relative;width:" + width
                           + ";min-height:" + h + "px;");
            HtmlUtils.center(sb, HtmlUtils.div("", " id='slider_" + baseId
                    + "' style='margin-bottom:2px;width:200px;' "));
            sb.append(HtmlUtils.img(url1, "",
                                    "id='before_" + baseId + "' width="
                                    + width + " style='position:absolute;'"));
            sb.append(
                HtmlUtils.img(
                    url2, "",
                    "id='after_" + baseId + "' width=" + width
                    + " style='opacity:0.5;position:absolute;'"));
            sb.append("</div>\n");
            String script = "imageOverlayInit('" + baseId + "');\n";
            HtmlUtils.script(sb, script);
        }

        getLinks(sb, request, props, entry, TAG_IMAGEOVERLAY);

        return sb.toString();
    }


    /**
     * _more_
     *
     * @param wikiUtil _more_
     * @param request _more_
     * @param originalEntry _more_
     * @param entry _more_
     * @param tag _more_
     * @param props _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getBeforeAfter(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props)
            throws Exception {
        StringBuffer sb   = new StringBuffer();
        StringBuffer divs = new StringBuffer();
        int          col  = 1;
        sb.append("\n");
        sb.append(HtmlUtils.importJS(getRepository().getUrlBase()
                                     + "/beforeafter/jquery.beforeafter.js"));

        StringBuffer jq      = new StringBuffer();
        List<Entry>  entries = getEntries(request, entry);
        for (int i = 0; i < entries.size(); i += 2) {
            if (i >= entries.size() - 1) {
                break;
            }
            Entry  entry1 = entries.get(i);
            Entry  entry2 = entries.get(i + 1);
            String width  = "800";
            String height = "366";
            String swidth = (String) entry.getStringValue(request,1, "800");
            if (swidth != null) {
                swidth = swidth.trim();
            }
            if ( !Utils.stringDefined(swidth)) {
                swidth = "800";
            }
            Dimension dim = getDimensions(entry1);
            if (Misc.equals(swidth, "default")) {
                width  = "" + dim.width;
                height = "" + dim.height;
            } else {
                if ((dim.width > 0) && (dim.height > 0)) {
                    if (dim.height > dim.width) {
                        height = "600";
                        width = Integer.toString(600 * dim.width
                                / dim.height);
                    } else {
                        width = swidth;
                        height = Integer.toString((int) (dim.height
                                * Integer.parseInt(width) / (float) dim.width));
                    }
                }
            }
            String id = "bandacontainer" + (cnt++);
            divs.append("<div id=\"" + id + "\">\n");
            String url1 = HtmlUtils.url(
                              request.makeUrl(repository.URL_ENTRY_GET) + "/"
                              + getStorageManager().getFileTail(
                                  entry1), ARG_ENTRYID, entry1.getId());
            String url2 = HtmlUtils.url(
                              request.makeUrl(repository.URL_ENTRY_GET) + "/"
                              + getStorageManager().getFileTail(
                                  entry2), ARG_ENTRYID, entry2.getId());


            divs.append("<img src=\"" + url1 + "\""
                        + HtmlUtils.attr(HtmlUtils.ATTR_WIDTH, width)
                        + HtmlUtils.attr(HtmlUtils.ATTR_HEIGHT, height)
                        + ">\n");

            divs.append("<img src=\"" + url2 + "\""
                        + HtmlUtils.attr(HtmlUtils.ATTR_WIDTH, width)
                        + HtmlUtils.attr(HtmlUtils.ATTR_HEIGHT, height)
                        + ">\n");

            divs.append("</div>\n");
            String path = getRepository().getUrlBase() + "/beforeafter/";
            String args = "{";
            args += "showFullLinks:false,\n";
            args += "imagePath:'" + path + "'}";
            sb.append("\n");
            jq.append(HtmlUtils.script(JQuery.ready("\n$(function(){$('#"
                    + id + "').beforeAfter(" + args + ");});\n")));
        }
        sb.append("\n");
        sb.append(divs);
        sb.append("\n");
        sb.append(jq);
        sb.append("\n");

        getLinks(sb, request, props, entry, TAG_BEFOREAFTER);

        return sb.toString();
    }



    /**
     * _more_
     *
     * @param wikiUtil _more_
     * @param request _more_
     * @param originalEntry _more_
     * @param entry _more_
     * @param tag _more_
     * @param props _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getLeftRight(WikiUtil wikiUtil, Request request,
                               Entry originalEntry, Entry entry, String tag,
                               Hashtable props)
            throws Exception {
        StringBuffer sb      = new StringBuffer();
        List<Entry>  entries = getEntries(request, entry);
        for (int i = 0; i < entries.size(); i += 2) {
            if (i >= entries.size() - 1) {
                break;
            }
            Entry  entry1 = entries.get(i);
            Entry  entry2 = entries.get(i + 1);
            String width  = "50%";
            String url1 = HtmlUtils.url(
                              request.makeUrl(repository.URL_ENTRY_GET) + "/"
                              + getStorageManager().getFileTail(
                                  entry1), ARG_ENTRYID, entry1.getId());
            String url2 = HtmlUtils.url(
                              request.makeUrl(repository.URL_ENTRY_GET) + "/"
                              + getStorageManager().getFileTail(
                                  entry2), ARG_ENTRYID, entry2.getId());


            sb.append("<table width=100%><tr valign=top>\n");
            sb.append("<td width=50%>");
            sb.append("<img src=\"" + url1 + "\""
                      + HtmlUtils.attr(HtmlUtils.ATTR_WIDTH, "100%") + ">\n");
            sb.append("</td>");
            sb.append("<td width=50%>");
            sb.append("<img src=\"" + url2 + "\""
                      + HtmlUtils.attr(HtmlUtils.ATTR_WIDTH, "100%") + ">\n");
            sb.append("</td>");
            sb.append("</tr></table>\n");
        }
        getLinks(sb, request, props, entry, TAG_LEFTRIGHT);

        return sb.toString();
    }


}
