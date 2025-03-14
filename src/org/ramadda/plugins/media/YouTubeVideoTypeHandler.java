/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.media;


import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;
import org.ramadda.repository.output.WikiManager;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;

import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;


import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.util.Date;
import java.util.Hashtable;
import java.util.List;


/**
 *
 *
 */
public class YouTubeVideoTypeHandler extends MediaTypeHandler {

    /** _more_ */
    private static int IDX = MediaTypeHandler.IDX_LAST+1;

    /** _more_ */
    public static final int IDX_ID = IDX++;

    /** _more_ */
    public static final int IDX_START = IDX++;

    /** _more_ */
    public static final int IDX_END = IDX++;

    /** _more_ */
    public static final int IDX_DISPLAY = IDX++;

    /** _more_ */
    public static final int IDX_AUTOPLAY = IDX++;

    /** _more_ */
    private static int idCnt = 0;

    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public YouTubeVideoTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }





    @Override
    public String embedYoutube(Request request, Entry entry,Hashtable props, StringBuilder sb, List attrs,
                               String mediaUrl) {
        boolean autoPlay = entry.getBooleanValue(request,IDX_AUTOPLAY, false);
        double start  = entry.getDoubleValue(request,IDX_START, 0.0);
        double end    = entry.getDoubleValue(request,IDX_END, -1);
	Utils.add(attrs,"autoplay",autoPlay?"1":"0");
	if(start>0)
	    Utils.add(attrs,"start",""+(int) start);
	if(end>0)
	    Utils.add(attrs,"end",""+(int) end);	
	return 	super.embedYoutube(request, entry, props, sb, attrs, mediaUrl);
    }

    /**
     *
     * @param sb _more_
     * @param id _more_
     * @param width _more_
     * @param height _more_
     * @param start _more_
     * @param end _more_
     * @param autoPlay _more_
     *  @return _more_
     *
     * @throws Exception _more_
     */
    public static String embedPlayer(Appendable sb, String id, String width,
                                     String height, double start, double end,
                                     boolean autoPlay)
            throws Exception {
        sb.append(
            "<iframe id=\"ytplayer\" type=\"text/html\" frameborder=\"0\" ");
        sb.append(XmlUtil.attr("width", width));
        sb.append(XmlUtil.attr("height", height));
        String playerId = "video_" + (idCnt++);
        String embedUrl = "//www.youtube.com/embed/" + id;
        embedUrl += "?enablejsapi=1";
        embedUrl += "&autoplay=" + (autoPlay
                                    ? 1
                                    : 0);
        embedUrl += "&playerapiid=" + playerId;
        if (start > 0) {
            embedUrl += "&start=" + ((int) (start * 60));
        }
        if (end > 0) {
            embedUrl += "&end=" + ((int) (end * 60));
        }
        sb.append("\n");
        sb.append("src=\"" + embedUrl + "\"");
        sb.append(">\n");
        sb.append("</iframe>\n");

        return playerId;
    }


    /**
     *
     * @param url _more_
     *  @return _more_
     */
    public static String getYouTubeId(String url) {
        String id = id = StringUtil.findPattern(url, "v=([^&]+)&");
        if (id == null) {
            id = StringUtil.findPattern(url, "v=([^&]+)");
        }
        if (id == null) {
            id = StringUtil.findPattern(url, "youtu.be/([^&]+)");
        }

        if (id == null) {
            id = StringUtil.findPattern(url, ".*/watch/([^&/]+)");
        }
        if (id == null) {
            id = StringUtil.findPattern(url, ".*/v/([^&/]+)");
        }

        return id;
    }


    /**
     * _more_
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
        String url = entry.getResource().getPath();
        String id  = StringUtil.findPattern(url, "v=([^&]+)&");
        if (id == null) {
            id = StringUtil.findPattern(url, "v=([^&]+)");
        }
        if (id == null) {
            id = StringUtil.findPattern(url, "youtu.be/([^&]+)");
        }
        if (id != null) {
            entry.setValue(IDX_ID, id);
        }

	System.err.println("url:" + url);

        String html  = IOUtil.readContents(url, "");
	System.err.println("html:" + Utils.clip(html,2000,"..."));
        String title = StringUtil.findPattern(html, "<title>(.*)</title>");
	System.err.println("title:" + title);
        if (title == null) {
            title = StringUtil.findPattern(html, "<TITLE>(.*)</TITLE>");
        }
        if (title != null) {
            title = title.replace("- YouTube", "");
            entry.setName(title);
        }


        if (id != null) {
            addThumbnail(getRepository(), request, entry, id);
        }
    }

    /**
     *
     * @param repository _more_
     * @param request _more_
     * @param entry _more_
     * @param id _more_
     */
    public static void addThumbnail(Repository repository, Request request,
                                    Entry entry, String id) {
        //      String thumbUrl = "https://i.ytimg.com/vi/" + id + "/default.jpg";
        String thumbUrl = "https://i.ytimg.com/vi/" + id + "/hq3.jpg";
        repository.getMetadataManager().addThumbnailUrl(request, entry,
                thumbUrl, "youtubethumb.jpg");
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param wikiTemplate _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public String getInnerWikiContent(Request request, Entry entry,
                                      String wikiTemplate)
            throws Exception {
        return getSimpleDisplay(request, null, entry);
    }


    /**
     * _more_
     *
     * @param args _more_
     */
    public static void main(String[] args) {
        String pattern = "^(http|https)://www.youtube.com/(watch\\?v=|v/).*";
        String url     = "https://www.youtube.com/v/q2H_fLuGZgo";
        //            "http://www.youtube.com/watch?v=sOU2WXaDEs0&feature=g-vrec";
        System.err.println(url.matches(pattern));
    }

}
