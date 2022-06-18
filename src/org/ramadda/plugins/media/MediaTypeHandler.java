/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.media;


import org.ramadda.repository.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.Utils;
import org.ramadda.util.WikiUtil;

import ucar.unidata.util.StringUtil;
import org.w3c.dom.*;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;


/**
 */
public class MediaTypeHandler extends GenericTypeHandler {


    /** _more_ */
    private static int IDX = 0;

    public static final String AUDIO_HEIGHT="40";

    /** _more_ */
    public static final int IDX_WIDTH = IDX++;

    /** _more_ */
    public static final int IDX_HEIGHT = IDX++;

    public static final int IDX_TRANSCRIPTIONS = IDX++;

    public static final int IDX_LAST = IDX-1;    


    public static final String MEDIA_VIMEO = "vimeo";
    public static final String MEDIA_YOUTUBE = "youtube";    
    public static final String MEDIA_SOUNDCLOUD = "soundcloud";
    public static final String MEDIA_OTHER = "other";




    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public MediaTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }


    @Override
    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props)
            throws Exception {
        if ( !tag.equals("video") && !tag.equals("media")) {
            return super.getWikiInclude(wikiUtil, request, originalEntry,
                                        entry, tag, props);
        }
	
        StringBuilder sb = new StringBuilder();
	getMediaHtml(request, entry, props,sb);
	return sb.toString();
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param props _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public String getSimpleDisplay(Request request, Hashtable props,
                                   Entry entry)
            throws Exception {
        return getWikiInclude(null, request, entry, entry, "media",
                              new Hashtable());
    }

    

    private void getMediaHtml(Request request, Entry entry, Hashtable props, Appendable sb) throws Exception {
	String url = entry.getResource().getPath();
        String width  = entry.getValue(IDX_WIDTH, "640");
        String height = entry.getValue(IDX_HEIGHT, "390");
	if(width.equals("0")) width = "640";
	if(height.equals("0")) height = "390";
	if(entry.getResource().isFile()) {
	    url = getEntryManager().getEntryResourceUrl(request, entry);
	}
	String embed = addMedia(request, entry, props, MEDIA_OTHER,null,url,null);
	sb.append(embed);
    }


    public String getWidth(Request request, Entry entry, Hashtable props) {
        String width  = Utils.getProperty(props, "width", entry.getValue(IDX_WIDTH, "640"));
	if(width.equals("0")) width = "640";
	return width;
    }

    public String getHeight(Request request, Entry entry, Hashtable props) {
        String height  = Utils.getProperty(props, "height", entry.getValue(IDX_HEIGHT, "360"));
	if(height.equals("0")) height = "360";
	return height;
    }    

    public String addMedia(Request request, Entry entry, Hashtable props, String mediaType, String embed, String mediaUrl,
			   List<String>  points) throws Exception {
        String player    = "";
        String id        = HtmlUtils.getUniqueId("player_");
        String pointsDivId = "pointsdiv_" + id;
        String searchId  = "search_" + id;
        String var       = "points_" + id;
        StringBuilder sb     = new StringBuilder();
        sb.append(HtmlUtils.cssLink(getHtdocsUrl("/media/media.css")));
        sb.append(HtmlUtils.importJS(getHtdocsUrl("/media/media.js")));
        StringBuilder js      = new StringBuilder();
        String transcriptions = getTranscriptions(request, entry);
	if(!Utils.stringDefined(transcriptions)) {
	    transcriptions = "[]";
	}
        js.append("var " + var + "=" + transcriptions+";\n");
	List attrs = new ArrayList<String>();
	boolean  canEdit = getAccessManager().canDoEdit(request, entry);
	boolean canAddTranscription = canEdit && canAddTranscription(request, entry);

	Utils.add(attrs, "canEdit",""+canEdit,"canAddTranscription",""+canAddTranscription,"entryId",JU.quote(entry.getId()));
	if(canAddTranscription) {
	    Utils.add(attrs,"authToken",JU.quote(request.getAuthToken()));
	}

	Utils.add(attrs, "id",JU.quote(id), "div",JU.quote(pointsDivId),
		  "points",var, "searchId",JU.quote(searchId));
	if(Utils.stringDefined(mediaUrl)) {
	    Utils.add(attrs,"mediaUrl",JU.quote(mediaUrl));
	}
        String width  = getWidth(request, entry, props);
        String height  = getHeight(request, entry,props);	
	Utils.add(attrs, "width",width,"height",height);

        if (mediaType.equalsIgnoreCase(MEDIA_VIMEO)) {
	    player = embedVimeo(request, entry,props,sb,attrs,embed,mediaUrl);
        } else if (mediaType.equalsIgnoreCase(MEDIA_YOUTUBE)) {
	    player = embedYoutube(request, entry,props,sb,attrs,mediaUrl);
        } else if (mediaType.equalsIgnoreCase(MEDIA_SOUNDCLOUD)) {
	    player = embedSoundcloud(request, entry, props, sb,attrs,mediaUrl);
        } else if (mediaType.equalsIgnoreCase(MEDIA_OTHER)) {
	    player = embedMedia(request, entry, props, sb, attrs, embed,mediaUrl);
	} else {
	    sb.append("Unknown media");
	    return sb.toString();
	}
	if (player == null) {
	    return sb.toString();
	}
	js.append("new RamaddaMediaTranscript(" + JU.map(attrs)+");\n");
        String searchDiv =
            HU.div("",
                   HU.attrs("style",
                            "vertical-align:top;", "id",
                            searchId));

	String pointsDiv= HU.div("", HU.attrs("id", pointsDivId));
        String playerDiv = HtmlUtils.centerDiv(HU.div(player,
						      HU.attrs("id", id))+searchDiv+pointsDiv);
	sb.append(playerDiv);
        sb.append(HtmlUtils.script(js.toString()));
        return sb.toString();
	
    }	


    public boolean canAddTranscription(Request request, Entry entry) {
	return true;
    }

    public String getTranscriptions(Request request, Entry entry) throws Exception {
	return (String) entry.getValue(IDX_TRANSCRIPTIONS);
    }


    public String getMediaType(Request request, Entry entry) {
	return  MEDIA_OTHER;
    }

    /**
     *
     * @param sb _more_
     * @param attrs _more_
     * @param embed _more_
     *  @return _more_
     */
    public String embedVimeo(Request request, Entry entry, Hashtable props,StringBuilder sb, List attrs, String embed,String mediaUrl) {
        if (!Utils.stringDefined(embed) && !Utils.stringDefined(mediaUrl)) {
            sb.append("No Vimeo embed");
            return null;
        }
       if (!Utils.stringDefined(embed)) {
	   String id = StringUtil.findPattern(mediaUrl, "https://vimeo.com/([0-9]+)");
	   if(id==null) {
	       sb.append("Could not find Vimeo id:" + mediaUrl);
	       return null;
	   }
	   String url = "https://player.vimeo.com/video/" + id;
	   embed = "<iframe src='" + url+"' width='640' height='351' frameborder='0' webkitallowfullscreen mozallowfullscreen allowfullscreen></iframe>";
       }
       sb.append(
		 "<script src='https://player.vimeo.com/api/player.js'></script>");
        Utils.add(attrs, "media", JU.quote("vimeo"));
        return embed;
    }


    /**
     *
     * @param sb _more_
     * @param attrs _more_
     * @param mediaUrl _more_
     *  @return _more_
     */
    public String embedYoutube(Request request, Entry entry, Hashtable props, StringBuilder sb, List attrs,
                               String mediaUrl) {
        if ( !Utils.stringDefined(mediaUrl)) {
            sb.append("No YouTube media url");
            return null;
        }
        sb.append(
            "<script src='https://www.youtube.com/iframe_api'></script>\n");
        String youTubeId = YouTubeVideoTypeHandler.getYouTubeId(mediaUrl);
        Utils.add(attrs, "media", JU.quote("youtube"), "videoId",
                  JU.quote(youTubeId));

        return "";
    }

    /**
     *
     * @param sb _more_
     * @param attrs _more_
     * @param mediaUrl _more_
     *  @return _more_
     */
    public String embedSoundcloud(Request request, Entry entry, Hashtable props,StringBuilder sb, List attrs,
                                  String mediaUrl) {
        if ( !Utils.stringDefined(mediaUrl)) {
            sb.append("No SoundCloud media url");

            return null;
        }
        String url = HU.urlEncode(mediaUrl);

        sb.append(
            "<script src='https://w.soundcloud.com/player/api.js'></script>\n");
        Utils.add(attrs, "media", JU.quote("soundcloud"));

        return "<iframe scrolling='no' src='https://w.soundcloud.com/player/?visual=true&amp;url="
               + url
               + "&maxwidth=450' width='450' height='390' frameborder='no'></iframe>";
    }


    /**
     *
     * @param sb _more_
     * @param attrs _more_
     * @param embed _more_
     * @param mediaUrl _more_
     *  @return _more_
     */
    public String embedMedia(Request request, Entry entry, Hashtable props,StringBuilder sb, List attrs, String embed,
                             String mediaUrl) {
        String player = "";
	String _path = entry.getResource().getPath().toLowerCase();
        if (Utils.stringDefined(embed)) {
            player = embed;
        } else if (Utils.stringDefined(mediaUrl)) {
	    String width  = getWidth(request, entry,props);
	    String height  = getHeight(request, entry,props);	
            String mediaId = HtmlUtils.getUniqueId("media_");
            Utils.add(attrs, "mediaId", JU.quote(mediaId));
            if (mediaUrl.toLowerCase().endsWith(".mp3") || _path.endsWith(".mp3") || _path.endsWith(".m4a")|| _path.endsWith("ogg")|| _path.endsWith("wav")) {
                player =
                    HU.tag("audio",HU.attrs(new String[]{"controls","","id", mediaId,
							 "style",
							 HU.css("height",HU.makeDim(AUDIO_HEIGHT,"px"),"width",HU.makeDim(width,"px"))}),
			HU.tag("source",HU.attrs(new String[]{"src", mediaUrl,				
							      "type","audio/mpeg"}),
			       "Your browser does not support the audio tag."));
                Utils.add(attrs, "media", JU.quote("media"));
            } else if (mediaUrl.toLowerCase().endsWith(".m4v") || _path.endsWith(".m4v")) {
                player =
                    HU.tag("video",HU.attrs(new String[]{"id", mediaId,"controls","","height",height,"width",width}),
			   HU.tag("source",HU.attrs(new String[]{
				       "src",mediaUrl,"type","video/mp4"})));
                Utils.add(attrs, "media", JU.quote("media"));
            } else if (mediaUrl.toLowerCase().endsWith(".mov") || _path.endsWith(".mov")) {
		player = HtmlUtils.tag("video", HtmlUtils.attrs(new String[] {
			    "id",mediaId,
			    HtmlUtils.ATTR_SRC, mediaUrl, HtmlUtils.ATTR_CLASS,
			    "ramadda-video-embed", HtmlUtils.ATTR_WIDTH, width,
			    HtmlUtils.ATTR_HEIGHT, height,
			}) + " controls ",
		    HtmlUtils.tag("source",
				  HtmlUtils.attrs(new String[] {
					  HtmlUtils.ATTR_SRC,
					  mediaUrl })));
                Utils.add(attrs, "media", JU.quote("media"));		
            } else {
                sb.append("Unknown media URL:" + mediaUrl);
                return null;
            }
        } else {
            sb.append("Unknown media");
            return null;
        }
        return player;
    }

}
