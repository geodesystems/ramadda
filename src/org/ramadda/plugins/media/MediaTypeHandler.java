/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.media;

import org.ramadda.repository.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.Utils;
import org.ramadda.util.WikiUtil;

import org.w3c.dom.*;

import ucar.unidata.util.StringUtil;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;


/**
 */
public class MediaTypeHandler extends GenericTypeHandler {


    /** _more_ */
    private static int IDX = 0;

    /**  */
    public static final String AUDIO_HEIGHT = "40";

    /** _more_ */
    public static final int IDX_WIDTH = IDX++;

    /** _more_ */
    public static final int IDX_HEIGHT = IDX++;

    /**  */
    public static final int IDX_TRANSCRIPTIONS = IDX++;

    /**  */
    public static final int IDX_LAST = IDX - 1;

    //These need to match up with what is used in media.js
    
    /**  */
    public static final String MEDIA_VIMEO = "vimeo";

    /**  */
    public static final String MEDIA_YOUTUBE = "youtube";

    public static final String MEDIA_TIKTOK = "tiktok";

    /**  */
    public static final String MEDIA_SOUNDCLOUD = "soundcloud";

    /**  */
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


    /**
     *
     * @param wikiUtil _more_
     * @param request _more_
     * @param originalEntry _more_
     * @param entry _more_
     * @param tag _more_
     * @param props _more_
      * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props)
            throws Exception {
        if ( !tag.equals("video") && !tag.equals("annotated_media")) {
            return super.getWikiInclude(wikiUtil, request, originalEntry,
                                        entry, tag, props);
        }

        StringBuilder sb = new StringBuilder();
        getMediaHtml(request, entry, props, sb);

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



    /**
     *
     * @param request _more_
     * @param entry _more_
     * @param props _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void getMediaHtml(Request request, Entry entry, Hashtable props,
                              Appendable sb)
            throws Exception {
        String url    = entry.getResource().getPath();
        String width  = entry.getStringValue(IDX_WIDTH, "640");
        String height = entry.getStringValue(IDX_HEIGHT, "390");
        if (width.equals("0")) {
            width = "640";
        }
        if (height.equals("0")) {
            height = "390";
        }
        if (entry.getResource().isFile()) {
           url = getEntryManager().getEntryResourceUrl(request, entry);
        }
        String embed = addMedia(request, entry, props,
                                getMediaType(request, entry), null, url,
                                null);
        sb.append(embed);
    }


    /**
     *
     * @param request _more_
     * @param entry _more_
     * @param props _more_
      * @return _more_
     */
    public String getWidth(Request request, Entry entry, Hashtable props) {
        String width = Utils.getProperty(props, "width",
                                         entry.getStringValue(IDX_WIDTH, "640"));
        if (!Utils.stringDefined(width) || width.equals("0")) {
            width = "640";
        }

        return width;
    }

    /**
     *
     * @param request _more_
     * @param entry _more_
     * @param props _more_
      * @return _more_
     */
    public String getHeight(Request request, Entry entry, Hashtable props) {
        String height = Utils.getProperty(props, "height",
                                          entry.getStringValue(IDX_HEIGHT, "360"));
        if (!Utils.stringDefined(height) || height.equals("0")) {
            height = "360";
        }

        return height;
    }

    /**
     *
     * @param request _more_
     * @param entry _more_
     * @param props _more_
     * @param mediaType _more_
     * @param embed _more_
     * @param mediaUrl _more_
     * @param points _more_
      * @return _more_
     *
     * @throws Exception _more_
     */
    public String addMedia(Request request, Entry entry, Hashtable props,
                           String mediaType, String embed, String mediaUrl,
                           List<String> points)
            throws Exception {
        String        player      = "";
        String        id          = HtmlUtils.getUniqueId("player_");
        String        pointsDivId = "pointsdiv_" + id;
        String        searchId    = "search_" + id;
        String        var         = "points_" + id;
        StringBuilder sb          = new StringBuilder();
        sb.append(HtmlUtils.cssLink(getHtdocsUrl("/media/media.css")));
        sb.append(HtmlUtils.importJS(getHtdocsUrl("/media/media.js")));
        StringBuilder js             = new StringBuilder();
        String        transcriptions = getTranscriptions(request, entry);
        if ( !Utils.stringDefined(transcriptions)) {
            transcriptions = "[]";
        }
	//In case the transcriptions is broken we first define the var in one block of JS
	//then we set it again in another block
        HtmlUtils.script(sb,"var " + var + "=[];\n");
        HtmlUtils.script(sb,var + "=" + transcriptions + ";\n");
        List    attrs = new ArrayList<String>();
        boolean canEdit = getAccessManager().canDoEdit(request, entry);
        boolean canAddTranscription = canEdit
                                      && canAddTranscription(request, entry);

        Utils.add(attrs, "canEdit", "" + canEdit, "canAddTranscription",
                  "" + canAddTranscription, "entryId",
                  JU.quote(entry.getId()));
        if (canAddTranscription) {
            Utils.add(attrs, "authToken", JU.quote(request.getAuthToken()));
        }

        Utils.add(attrs, "id", JU.quote(id), "div", JU.quote(pointsDivId),
                  "points", var, "searchId", JU.quote(searchId));
        if (Utils.stringDefined(mediaUrl)) {
            Utils.add(attrs, "mediaUrl", JU.quote(mediaUrl));
        }
        String width  = getWidth(request, entry, props);
        String height = getHeight(request, entry, props);
	boolean vertical = Utils.getProperty(props,"vertical",false);
        Utils.add(attrs, "width", JU.quote(width), "height", JU.quote(height));

	//	System.err.println("U:" + mediaType+" " + mediaUrl +" " + embed);
        if (mediaType.equalsIgnoreCase(MEDIA_VIMEO)) {
            player = embedVimeo(request, entry, props, sb, attrs, embed,
                                mediaUrl);
        } else if (mediaType.equalsIgnoreCase(MEDIA_YOUTUBE)) {
            player = embedYoutube(request, entry, props, sb, attrs, mediaUrl);
        } else if (mediaType.equalsIgnoreCase(MEDIA_SOUNDCLOUD)) {
            player = embedSoundcloud(request, entry, props, sb, attrs,
                                     mediaUrl);
        } else if (mediaType.equalsIgnoreCase(MEDIA_OTHER)) {
            player = embedMedia(request, entry, props, sb, attrs, embed,
                                mediaUrl);
        } else {
            sb.append("Unknown media");
            return sb.toString();
        }
        if (player == null) {
            return sb.toString();
        }
        js.append("new RamaddaMediaTranscript(" + JU.map(attrs) + ");\n");
        String searchDiv = HU.div("", HU.attrs("style", "vertical-align:top;",
					       "id", searchId));

        String pointsDiv = HU.div("", HU.attrs("id", pointsDivId));
	String bottom = HU.div(searchDiv + pointsDiv,HU.style("margin-top","5px","width",HU.makeDim(width,"px")));
	String style = "width:" + HU.makeDim(width,"px")+";";
	style="";
	if(vertical)
	    style+="display:flex;justify-content:center;";
	else
	    //	    style+="display:flex;justify-content:right;";
	    style+="text-align:right;";
	player = HU.div("\n"+player+"\n", HU.attrs("id", id,"style",style));
	if(!vertical) {
	    sb.append("<div  class='row'  >");
	    sb.append("<div  class='col-md-6 ramadda-col wiki-col ramadda-media-player'  >");
	}
	sb.append(player);
	if(!vertical) {
	    sb.append("</div>");
	    sb.append("<div  class='col-md-6 ramadda-col wiki-col'  >");
	}
	sb.append("<div class=ramadda-media-annotations style=''>" +  bottom    +"</div>");
	if(!vertical) {
	    sb.append("</div></div>");
	}
        sb.append(HtmlUtils.script(js.toString()));

        return sb.toString();

    }


    /**
     *
     * @param request _more_
     * @param entry _more_
      * @return _more_
     */
    public boolean canAddTranscription(Request request, Entry entry) {
        return true;
    }

    /**
     *
     * @param request _more_
     * @param entry _more_
      * @return _more_
     *
     * @throws Exception _more_
     */
    public String getTranscriptions(Request request, Entry entry)
            throws Exception {
        return (String) entry.getValue(IDX_TRANSCRIPTIONS);
    }


    /**
     *
     * @param request _more_
     * @param entry _more_
      * @return _more_
     */
    public String getMediaType(Request request, Entry entry) {
        String _path = entry.getResource().getPath().toLowerCase();
        //https://soundcloud.com/the-wisdom-project/004-martin-luther-king-jr-malcolm-x-and-robert-penn-warren
        if (_path.indexOf("soundcloud.com") >= 0) {
            return MEDIA_SOUNDCLOUD;
        }
        if (_path.indexOf("vimeo.com") >= 0) {
            return MEDIA_VIMEO;
        }

        return MEDIA_OTHER;
    }

    /**
     *
     *
     * @param request _more_
     * @param entry _more_
     * @param props _more_
     * @param sb _more_
     * @param attrs _more_
     * @param embed _more_
     * @param mediaUrl _more_
     *  @return _more_
     */
    public String embedVimeo(Request request, Entry entry, Hashtable props,
                             StringBuilder sb, List attrs, String embed,
                             String mediaUrl) {
        if ( !Utils.stringDefined(embed) && !Utils.stringDefined(mediaUrl)) {
            sb.append("No Vimeo embed");

            return null;
        }
        if ( !Utils.stringDefined(embed)) {
            String id = StringUtil.findPattern(mediaUrl,
                            "https://vimeo.com/([0-9]+)");
            if (id == null) {
                sb.append("Could not find Vimeo id:" + mediaUrl);

                return null;
            }
            String url = "https://player.vimeo.com/video/" + id;
            embed =
                "<iframe src='" + url
                + "' width='640' height='351' frameborder='0' webkitallowfullscreen mozallowfullscreen allowfullscreen></iframe>";
        }
        sb.append(
            "<script src='https://player.vimeo.com/api/player.js'></script>");
        Utils.add(attrs, "media", JU.quote("vimeo"));

        return embed;
    }


    /**
     *
     *
     * @param request _more_
     * @param entry _more_
     * @param props _more_
     * @param sb _more_
     * @param attrs _more_
     * @param mediaUrl _more_
     *  @return _more_
     */
    public String embedYoutube(Request request, Entry entry, Hashtable props,
                               StringBuilder sb, List attrs,
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
     *
     * @param request _more_
     * @param entry _more_
     * @param props _more_
     * @param sb _more_
     * @param attrs _more_
     * @param mediaUrl _more_
     *  @return _more_
     */
    public String embedSoundcloud(Request request, Entry entry,
                                  Hashtable props, StringBuilder sb,
                                  List attrs, String mediaUrl) {
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
     *
     * @param request _more_
     * @param entry _more_
     * @param props _more_
     * @param sb _more_
     * @param attrs _more_
     * @param embed _more_
     * @param mediaUrl _more_
     *  @return _more_
     */
    public String embedMedia(Request request, Entry entry, Hashtable props,
                             StringBuilder sb, List attrs, String embed,
                             String mediaUrl) {
        String player = "";
        String _path  = entry.getResource().getPath().toLowerCase();
        if (Utils.stringDefined(embed)) {
            player = embed;
        } else if (Utils.stringDefined(mediaUrl)) {
	    String _mediaUrl = mediaUrl.toLowerCase();
            String width   = getWidth(request, entry, props);
            String height  = getHeight(request, entry, props);
            String mediaId = HtmlUtils.getUniqueId("media_");
            Utils.add(attrs, "mediaId", JU.quote(mediaId));
            if (_mediaUrl.endsWith(".mp3") || _path.endsWith(".mp3") ||
		_path.endsWith(".m4a") || _mediaUrl.endsWith(".webm") ||
		_path.endsWith(".webm")	 || _path.endsWith("ogg") || _path.endsWith("wav")) {
                player = HU.tag("audio", HU.attrs(new String[] {
                    "controls", "", "id", mediaId, "style",
                    HU.css("height", HU.makeDim(AUDIO_HEIGHT, "px"), "width",
                           HU.makeDim(width, "px"))
                }), HU.tag("source", HU.attrs(new String[] { "src", mediaUrl,
                        "type",
                        "audio/mpeg" }), "Your browser does not support the audio tag."));
                Utils.add(attrs, "media", JU.quote("media"));
            } else if (_mediaUrl.endsWith(".m4v") ||
		       _mediaUrl.endsWith(".mp4")
                       || _path.endsWith(".m4v")) {
                player = HU.tag("video", HU.attrs(new String[] {
                    "id", mediaId, "controls", "", "preload", "metadata",
                    "height", height, "style","max-width:100%",
		    "width", width
                }), HU.tag("source", HU.attrs(new String[] { "src", mediaUrl,
                        "type", "video/mp4" })));
                Utils.add(attrs, "media", JU.quote("media"));
            } else if (_mediaUrl.endsWith(".mov") || _path.endsWith(".mov") ||
		_path.endsWith(".mp4") ||_mediaUrl.endsWith(".mp4")) {
		/*
		player = HU.tag("embed", HU.attrs("src",mediaUrl,
						  "width", width,
						  "height", height,
						  "controller","true",
						  "autoplay","false",
						  "loop","false"));
		*/
                player = HtmlUtils.tag("video", HtmlUtils.attrs(new String[] {
                    "id", mediaId, HtmlUtils.ATTR_SRC, mediaUrl,
                    HtmlUtils.ATTR_CLASS, "ramadda-video-embed",
                    HtmlUtils.ATTR_WIDTH, width, HtmlUtils.ATTR_HEIGHT,
                    height,"style","max-width:100%;",
                }) + " controls ", HtmlUtils.tag("source",
                        HtmlUtils.attrs(new String[] { HtmlUtils.ATTR_SRC,
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
