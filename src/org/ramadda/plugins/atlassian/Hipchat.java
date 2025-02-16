/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.atlassian;


import org.json.*;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.harvester.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.ChunkedAppendable;


import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.Utils;

import ucar.unidata.ui.HttpFormEntry;
import ucar.unidata.util.IOUtil;

import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;

import java.io.*;

import java.net.*;


import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;




/**
 */
public class Hipchat {

    /** _more_ */
    public static final String MACRO_ROOM_ID = "{roomid}";

    /** _more_ */
    public static final String MACRO_MESSAGE_ID = "{message}";


    /** _more_ */
    public static final String API_ROOM = "/v2/room";

    /** _more_ */
    public static final String API_ROOM_LATEST = API_ROOM + "/"
                                                 + MACRO_ROOM_ID
                                                 + "/history/latest";

    /** _more_ */
    public static final String API_ROOM_GET = API_ROOM + "/" + MACRO_ROOM_ID;

    /** _more_ */
    public static final String API_ROOM_UPLOAD_FILE = API_ROOM + "/"
                                                      + MACRO_ROOM_ID
                                                      + "/share/file";



    /** _more_ */
    public static final String API_MESSAGE_GET = API_ROOM + "/"
                                                 + MACRO_ROOM_ID
                                                 + "/history/"
                                                 + MACRO_MESSAGE_ID;


    /** _more_ */
    public static boolean debug = false;


    /** _more_ */
    private static final Object MUTEX = new Object();

    /** _more_ */
    private static long lastCall = 0;


    /** A bit less than 10000 */
    public static final int MAX_MESSAGE_LENGTH = 9999;


    /** _more_ */
    public static final String ARG_AUTH_TOKEN = "auth_token";


    /** _more_ */
    public static final String ARG_COLOR = "color";

    /** _more_ */
    public static final String ARG_MESSAGE = "message";

    /** _more_ */
    public static final String ARG_NOTIFY = "notify";

    /** _more_ */
    public static final String ARG_MESSAGE_FORMAT = "message_format";


    /**
     * _more_
     *
     *
     * @param repository _more_
     * @param request _more_
     * @param message _more_
     * @param entries _more_
     * @param webHook _more_
     * @param showChildren _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static Result makeEntryResult(Repository repository,
                                         Request request, String message,
                                         List<Entry> entries, String webHook,
                                         boolean showChildren)
            throws Exception {
        //Don't hammer hipchat
        long now = System.currentTimeMillis();
        if (now - lastCall < 1000) {
            Misc.sleep(1000);
        }
        lastCall = System.currentTimeMillis();

        synchronized (MUTEX) {
            ChunkedAppendable ab =
                new ChunkedAppendable(Hipchat.MAX_MESSAGE_LENGTH);
            ab.append(message);
            ab.append(HtmlUtils.br());
            if (entries != null) {
                int[] imageCnt = { 0 };
                int   cnt      = 0;
                for (Entry entry : entries) {
                    StringBuilder tmp = new StringBuilder();
                    makeEntryLink(repository, request, tmp, entry,
                                  showChildren, cnt++, imageCnt);
                    ab.append(tmp.toString());
                }
            }
            for (Object buffer : ab.getBuffers()) {
                System.err.println("Post:" + buffer.toString().length());
                postMessage(webHook, buffer.toString());
            }

            return new Result("", new StringBuilder(""));
        }
    }


    /**
     * _more_
     *
     * @param message _more_
     * @param args _more_
     *
     * @return _more_
     */
    public static String encodeMessage(String message, String... args) {
        message = message.replaceAll("\n", "<br>");
        String   color = (args.length > 0)
                         ? args[0]
                         : "green";
        List  attrs =  Utils.makeListFromValues( "color", color, "message", message, "notify", "false",
				       "message_format", "html");

        message = JsonUtil.mapAndQuote(attrs);
        message = clean(message);

        return message;
    }


    /**
     * _more_
     *
     * @param webHook _more_
     * @param s _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static boolean postMessage(String webHook, String s)
            throws Exception {
        s = clean(s);

        List<HttpFormEntry> formEntries = new ArrayList<HttpFormEntry>();
        formEntries.add(HttpFormEntry.hidden(ARG_COLOR, "green"));
        formEntries.add(HttpFormEntry.hidden(ARG_MESSAGE, s));
        formEntries.add(HttpFormEntry.hidden(ARG_NOTIFY, "false"));
        formEntries.add(HttpFormEntry.hidden(ARG_MESSAGE_FORMAT, "html"));
        System.err.println("HipchatHarvester: posting to hipchat:" + webHook);
        try {
            String[] result = HttpFormEntry.doPost(formEntries, webHook);
            if (result[0] != null) {
                System.err.println("HipchatHarvester: error:" + result[0]
                                   + " url:" + webHook);

                return false;
            } else {
                //                System.err.println("HipchatHarvester: results:" + result[1]+ " url:" + webHook);
            }
        } catch (Exception exc) {
            //Uggh, but for now ignore this. I think hipchat doesn't return anything and 
            //this pukes down in the unidata code
            Throwable inner = LogUtil.getInnerException(exc);
            if (inner instanceof java.lang.NullPointerException) {}
            else {
                throw exc;
            }
        }

        return true;

    }

    /**
     * _more_
     *
     *
     * @param repository _more_
     * @param request _more_
     * @param sb _more_
     * @param entry _more_
     * @param showChildren _more_
     * @param cnt _more_
     * @param imageCnt _more_
     *
     *
     * @throws Exception _more_
     */
    public static void makeEntryLink(Repository repository, Request request,
                                     Appendable sb, Entry entry,
                                     boolean showChildren, int cnt,
                                     int[] imageCnt)
            throws Exception {

        List<String> imageUrls = new ArrayList<String>();
        if (entry.isImage()) {
            if (entry.getResource().isUrl()) {
                imageUrls.add(entry.getResource().getPath());
            } else if (request != null) {
                imageUrls
                    .add(request
                        .getAbsoluteUrl(request.getRepository()
                            .getHtmlOutputHandler()
                            .getImageUrl(request, entry, true)));
            }
        }
        List<Metadata> metadataList = repository.getMetadataManager().getMetadataList(request,entry);
        if (metadataList != null) {
            for (Metadata metadata : metadataList) {
                //A hack for urls
                if (Misc.equals(metadata.getType(),
                                ContentMetadataHandler.TYPE_THUMBNAIL)) {
                    imageUrls.add(metadata.getAttr1());
                } else if (Misc
                        .equals(metadata.getType(),
                                ContentMetadataHandler
                                    .TYPE_ATTACHMENT) && (metadata.getAttr1()
                                        != null) && metadata.getAttr1()
                                            .startsWith("http")) {
                    if (Utils.isImage(metadata.getAttr1())
                            || Misc.equals(metadata.getAttr2(), "image")) {
                        imageUrls.add(metadata.getAttr1());
                    }
                }
            }
        }


        String icon = request.getAbsoluteUrl(
                          repository.getPageHandler().getIconUrl(
                              request, entry));
        sb.append(HtmlUtils.img(icon));
        if ( !showChildren) {
            sb.append(" #");
            sb.append((cnt + 1) + "");
            sb.append(" ");
        }
        sb.append(HtmlUtils.href(getEntryUrl(repository, request, entry),
                                 entry.getName()));
        makeDownloadLink(request, entry, sb);
        sb.append(HtmlUtils.br());

        /*
        if (repository.getEntryManager().isSynthEntry(entry.getId())
                && entry.getResource().isUrl()) {
            map.add("title_link");
            map.add(JsonUtil.quote(entry.getResource().getPath()));
        } else {
            map.add("title_link");
            map.add(JsonUtil.quote(getEntryUrl(repository, request, entry)));
            }*/

        StringBuilder desc = new StringBuilder();
        String snippet =
            request.getRepository().getWikiManager().getSnippet(request,
                entry, false, null);
        if (Utils.stringDefined(snippet)) {
            snippet = snippet.replaceAll("<div[^>]+>", "");
            snippet = snippet.replaceAll("</div>", "");
            desc.append(snippet);
            desc.append("<br>");
        }


        if (showChildren) {
            int childCnt = 0;
            List<Entry> children =
                repository.getEntryManager().getChildren(request, entry);
            entry.setChildren(children);
            for (Entry child : children) {
                if (childCnt == 0) {
                    //                    desc.append("<ul> ");
                }
                childCnt++;
                desc.append("&nbsp;&nbsp;&nbsp;&nbsp; #" + (childCnt));
                //                desc.append("<li> #" + (childCnt));
                desc.append(" ");
                String childIcon = request.getAbsoluteUrl(
                                       repository.getPageHandler().getIconUrl(
                                           request, child));
                desc.append(HtmlUtils.img(childIcon));
                desc.append(" ");
                desc.append(HtmlUtils.href(getEntryUrl(repository, request,
                        child), child.getName()));
                makeDownloadLink(request, child, desc);
                desc.append("<br>");
            }
            if (childCnt > 0) {
                //                desc.append("</ul> ");
            }
        }
        sb.append(desc);
        //            map.add("fields");
        //            map.add(JsonUtil.list(fields));

        for (String imageUrl : imageUrls) {
            //Only include images for the first 20 entries
            if (imageCnt[0]++ < 20) {
                sb.append(HtmlUtils.img(imageUrl, "",
                                        " style=\"max-width:300px;\" "));
                sb.append(HtmlUtils.p());
            }
        }
        sb.append("\n");

    }


    /**
     * _more_
     *
     * @param args _more_
     */
    public static void main(String[] args) {}


    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public static String clean(String s) {
        try {
            byte[] b = s.getBytes("UTF-8");
            s = new String(b, "UTF-8");
            //just the utf-8 conversion doesn't clean up some bad chars
            s = Utils.removeNonAscii(s);

            return s;
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry the entry
     * @param sb buffer
     *
     * @throws Exception _more_
     */
    public static void makeDownloadLink(Request request, Entry entry,
                                        Appendable sb)
            throws Exception {

        if (entry.getResource().isUrl()) {
            sb.append(" ");
            String linkIcon =
                request.getAbsoluteUrl(
                    request.getRepository().getPageHandler().getIconUrl(
                        "/icons/link.png"));
            sb.append(HtmlUtils.href(entry.getResource().getPath(),
                                     HtmlUtils.img(linkIcon, "Download")));

            return;
        }
        if ( !request.getRepository().getAccessManager().canDownload(request,
                entry)) {
            return;
        }
        String url =
            request.getRepository().getEntryManager().getEntryResourceUrl(
                request, entry);
        url = url.replace(" ", "+");
        String size = entry.getTypeHandler().formatFileLength(
                          entry.getResource().getFileSize());
        String label = "(Download - " + size + ")";
        sb.append(" ");
        sb.append(HtmlUtils.href(request.getAbsoluteUrl(url), label));
    }



    /**
     * _more_
     *
     *
     * @param repository _more_
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static String getEntryUrl(Repository repository, Request request,
                                     Entry entry)
            throws Exception {
        return request.getAbsoluteUrl(
            repository.getEntryManager().getEntryURL(request, entry));
    }



    /**
     * _more_
     *
     * @param repository _more_
     * @param url _more_
     * @param token _more_
     * @param args _more_
     *
     * @return _more_
     */
    public static JSONObject call(Repository repository, String url,
                                  String token, String args) {
        try {
            if ( !Utils.stringDefined(token)) {
                System.err.println("Hipchat.call:" + url + " no token");

                return null;
            }

            url += "?" + HtmlUtils.arg(ARG_AUTH_TOKEN, token);
            if (Utils.stringDefined(args)) {
                url += "&" + args;
            }
            //            System.err.println ("Hipchat api call:" + url);
            //            System.err.println("Hipchat call:" + url);
            String json = IOUtil.readContents(new URL(url));
            if (json == null) {
                return null;
            }
            JSONObject obj = new JSONObject(json);

            //            System.out.println(json);
            return obj;
        } catch (Exception exc) {
            repository.getLogManager().logError("Error calling Hipchat API:"
                    + url, exc);

            return null;
        }
    }





}
