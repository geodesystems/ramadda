/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.media;


import org.ramadda.repository.*;
import org.ramadda.repository.output.WikiManager;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;

import org.w3c.dom.*;
import org.json.*;

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
public class TikTokTypeHandler extends MediaTypeHandler {

    /** _more_ */
    private static int IDX = MediaTypeHandler.IDX_LAST+1;

    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public TikTokTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }


    public static String embedPlayer(Appendable sb, String id, String width,
                                     String height, double start, double end,
                                     boolean autoPlay)
            throws Exception {
	/*
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

        return playerId;*/
	return "";
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
	url  = "https://www.tiktok.com/oembed?url=" + url;
        JSONObject json  = new JSONObject(IOUtil.readContents(url, ""));
	List<String> tags = Utils.split(json.getString("title"),"#",true,true);
        String title =  tags.get(0);
	tags.remove(0);
	for(String tag: tags) {
	    getMetadataManager().addMetadataTag(request,entry, tag);
	}
	entry.setName(title);
	String thumbnail = json.optString("thumbnail_url");
	if(thumbnail!=null) {
	    getMetadataManager().addThumbnailUrl(request, entry, thumbnail,title+".png");
	}
	String desc = "+note\nBy " + HU.href(json.getString("author_url"),json.getString("author_name")) +"\n-note";
	entry.setDescription(desc);
    }

    @Override
    public void getMediaHtml(Request request, Entry entry, Hashtable props,
                              Appendable sb)
            throws Exception {
        String url = entry.getResource().getPath();
	String id = StringUtil.findPattern(url,"/video/([0-9]+)");
	sb.append("<blockquote class='tiktok-embed' cite='" +
		  url +"'data-video-id='" + id+"' data-embed-from='oembed' style='max-width: 605px;min-width: 325px;' ><section></section></blockquote><script async src='https://www.tiktok.com/embed.js'></script>");
    }

}
