/**
Copyright (c) 2008-2026 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.media;

import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;
import org.ramadda.repository.output.WikiManager;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;
import org.ramadda.util.Utils;

import org.json.*;
import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.util.Date;
import java.util.Hashtable;
import java.util.List;


public class YouTubeVideoTypeHandler extends MediaTypeHandler {
    private static final String ACTION_EXTRACT_ANNOTATIONS="extractannotations";
    public static final String COL_ID = "video_id";
    public static final String COL_START = "video_start";
    public static final String COL_END = "video_end";
    public static final String COL_DISPLAY = "display";
    public static final String COL_AUTOPLAY = "autoplay";
    private static int idCnt = 0;

    public YouTubeVideoTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }

    @Override
    public void addAction(Action action) {
	if(action.getId().equals(ACTION_EXTRACT_ANNOTATIONS)) {
	    if(!getRepository().getLLMManager().isGeminiEnabled()) {
		return;
	    }
	}
	super.addAction(action);
    }

    //Right now gemini fails at this. For some reason it is processing a different YT video
    @Override
    public Result processEntryAction(Request request, Entry entry)
	throws Exception {
        String action = request.getString(ARG_ACTION, "");
	if(!action.equals(ACTION_EXTRACT_ANNOTATIONS)) {
	    return super.processEntryAction(request,entry);
	}
	StringBuilder sb = new StringBuilder();
        getPageHandler().entrySectionOpen(request, entry, sb, "Extract Annotations");

	if(request.exists(ARG_OK)) {
	    String youtubeUrl  = entry.getResourcePath(request);
	    String text = "**URL_INPUT_START**\n";
	    text+="URL: " + youtubeUrl+"\n";
	    text+="**URL_INPUT_END**\n";
	    //" + entry.getStringValue(request,"video_id","") + "\n"; 	    
	    text+="**TASK:** Analyze the video content provided in the URL_INPUT section.\n";
	    text+="**OUTPUT_FORMAT:** Extract the key moments from the video. Return valid JSON and **only valid JSON** of the form:\n[{time:<seconds>, title:<title>, synopsis:<synopsis>},...]\n";

		/*
		It is imperative that this prompt is applied to the above youtube video URL and Video ID. You are a master at analyzing youtube videos. I want you to extract the key moments from the below youtube video. You should extract the timestamp of the moment, a title for the moment and a short synopsis. You should return  valid JSON and only valid JSON of the form [{time:<time>,title:<title>,synopsis:<synopsis>},....]. The time should be the number of seconds into the video.  It is imperative that you only use the video referenced by the above Youtube URL.";
		*/
	    System.out.println(text);
	    IO.Result result = getLLMManager().callGemini(getLLMManager().getModel("gemini-2.5-flash"),text);
	    if(result.getError()) {
		sb.append(HU.b("An error has occurred:"));
		sb.append(HU.pre(result.getResult()));
	    } else {
		String jsonText = result.getResult();
		try {
		    JSONObject json = new JSONObject(result.getResult());
		    jsonText = getLLMManager().readGeminiResult(json);
		    if(jsonText==null) {
			sb.append(HU.b("Could not extract results:"));
			sb.append(HU.pre(jsonText));
		    } else {
			jsonText = jsonText.replace("```json","").trim();
			jsonText = jsonText.replace("```","").trim();
			JSONArray array = new JSONArray(jsonText);
			sb.append(HU.div("OK"));
			sb.append(HU.pre(jsonText));
			entry.setValue("transcriptions_json",jsonText);
			getEntryManager().updateEntry(request, entry);
		    }
		} catch(Exception exc) {
		    sb.append(HU.b("There was an error handling the result:"));
		    sb.append(HU.pre(jsonText));
		}
	    }
	    getPageHandler().entrySectionClose(request, entry, sb);
	    return getEntryManager().addEntryHeader(request, entry,
						    new Result("Extract Annotations", sb));
	}

	sb.append(HU.pre(entry.getStringValue(request,"transcriptions_json","")));
	String url = getEntryActionUrl(request,  entry,ACTION_EXTRACT_ANNOTATIONS);
	sb.append(HU.formPost(url));
	sb.append(HU.hidden(ARG_ACTION,ACTION_EXTRACT_ANNOTATIONS));
	sb.append(HU.hidden(ARG_ENTRYID,entry.getId()));
	sb.append(HU.div("Do you want to submit the Youtube URL to Gemini to extract video annotations?"));
	sb.append(HU.div(HU.b("Note: this will overwrite any existing annotations")));	
	sb.append(HU.buttons(HU.submit(LABEL_OK,    ARG_OK),
			     HU.submit(LABEL_CANCEL, ARG_CANCEL)));
	sb.append(HU.formClose());

        getPageHandler().entrySectionClose(request, entry, sb);

	return getEntryManager().addEntryHeader(request, entry,new Result("Extract Annotations", sb));
    }



    @Override
    public String embedYoutube(Request request, Entry entry,Hashtable props, StringBuilder sb, List attrs,
                               String mediaUrl) {
        boolean autoPlay = entry.getBooleanValue(request,COL_AUTOPLAY, false);
        double start  = entry.getDoubleValue(request,COL_START, 0.0);
        double end    = entry.getDoubleValue(request,COL_END, -1);
	Utils.add(attrs,"autoplay",autoPlay?"1":"0");
	if(start>0)
	    Utils.add(attrs,"start",""+(int) start);
	if(end>0)
	    Utils.add(attrs,"end",""+(int) end);	
	return 	super.embedYoutube(request, entry, props, sb, attrs, mediaUrl);
    }

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

    @Override
    public void initializeNewEntry(Request request, Entry entry,NewType newType)
            throws Exception {
        super.initializeNewEntry(request, entry, newType);
	if(!isNew(newType)) {
	    return;
	}
        String url = entry.getResource().getPath();
        String id  = StringUtil.findPattern(url, "v=([^&]+)&");
        if (id == null) {
            id = StringUtil.findPattern(url, "v=([^&]+)");
        }
        if (id == null) {
            id = StringUtil.findPattern(url, "youtu.be/([^&]+)");
        }
        if (id != null) {
            entry.setValue(COL_ID, id);
        }

        String html  = IOUtil.readContents(url, "");
        String title = StringUtil.findPattern(html, "<title>(.*)</title>");
        if (title == null) {
            title = StringUtil.findPattern(html, "<TITLE>(.*)</TITLE>");
        }
        if (title != null) {
            title = title.replace("- YouTube", "").trim();
        }

	if(!stringDefined(entry.getName())) {
	    if(!stringDefined(title)) {
		title = "Youtube Video";
	    } 
            entry.setName(title);
	}

        if (id != null) {
            addYoutubeThumbnail(getRepository(), request, entry, id);
        }
    }

    public static void addYoutubeThumbnail(Repository repository, Request request,
                                    Entry entry, String id) {
        //      String thumbUrl = "https://i.ytimg.com/vi/" + id + "/default.jpg";
        String thumbUrl = "https://i.ytimg.com/vi/" + id + "/hq3.jpg";
        repository.getMetadataManager().addThumbnailUrl(request, entry,
                thumbUrl, "youtubethumb.jpg");
    }

    @Override
    public String getInnerWikiContent(Request request, Entry entry,
                                      String wikiTemplate)
            throws Exception {
        return getSimpleDisplay(request, null, entry);
    }

    public static void main(String[] args) {
        String pattern = "^(http|https)://www.youtube.com/(watch\\?v=|v/).*";
        String url     = "https://www.youtube.com/v/q2H_fLuGZgo";
        //            "http://www.youtube.com/watch?v=sOU2WXaDEs0&feature=g-vrec";
        System.err.println(url.matches(pattern));
    }

}
