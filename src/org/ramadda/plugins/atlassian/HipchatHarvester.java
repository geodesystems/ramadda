/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.atlassian;


import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;



import org.json.*;

import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.harvester.*;
import org.ramadda.repository.metadata.*;

import org.ramadda.repository.search.SearchManager;
import org.ramadda.repository.search.SearchProvider;
import org.ramadda.repository.type.*;
import org.ramadda.util.FileInfo;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.Utils;


import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

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
public class HipchatHarvester extends CommandHarvester {

    /**
     * ctor
     *
     * @param repository the repository
     * @param id _more_
     *
     * @throws Exception On badness
     */
    public HipchatHarvester(Repository repository, String id)
            throws Exception {
        super(repository, id);
    }


    /**
     * ctor
     *
     * @param repository _more_
     * @param node _more_
     *
     * @throws Exception On badness
     */
    public HipchatHarvester(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }


    /**
     * factory method for the request
     *
     * @param request incoming request
     *
     * @return command request
     *
     * @throws Exception On badness
     */
    @Override
    public CommandRequest doMakeCommandRequest(Request request)
            throws Exception {
        return new HipchatRequest(request);
    }




    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public String getHelpUrl() {
        return null;
        //Not now
        //        return getRepository().getUrlBase() +"/atlassian/index.html";
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getDescription() {
        return "Hipchat";
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getCommandTypeName() {
        return "Hipchat";

    }

    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public int getMessageLimit() {
        return Hipchat.MAX_MESSAGE_LENGTH;
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    @Override
    public String getChannelId(CommandRequest request) throws Exception {
        HipchatRequest hr = (HipchatRequest) request;

        return hr.getChannelId();
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    @Override
    public String getAuthToken(CommandRequest request) throws Exception {
        HipchatRequest hr = (HipchatRequest) request;

        return super.getAuthToken(request);
        //        return request.getString(Hipchat.HIPCHAT_TOKEN, "none");
    }

    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    @Override
    public String getText(CommandRequest request) throws Exception {
        HipchatRequest hr = (HipchatRequest) request;

        return hr.getText();
    }





    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     */
    @Override
    public String getStateKey(CommandRequest request) {
        HipchatRequest hr = (HipchatRequest) request;

        return hr.room + "-" + hr.fromId;
    }



    /**
     * _more_
     *
     * @param msg _more_
     *
     * @return _more_
     */
    @Override
    public String encodeMessage(String msg) {
        //        try {postMessage(msg.replaceAll("\n", "<br>"));} catch(Exception ignore) {}
        return Hipchat.encodeMessage(msg);
    }

    /**
     * _more_
     *
     * @param title _more_
     * @param msg _more_
     * @param type _more_
     *
     * @return _more_
     */
    public String encodeMessage(String title, String msg, String type) {
        title = HtmlUtils.tag("p", HtmlUtils.cssClass("title"),
                              HtmlUtils.tag("strong", "", title));

        msg = HtmlUtils.tag("p", "", msg);

        return HtmlUtils.div(title + msg,
                             HtmlUtils.cssClass("aui-message aui-message-"
                                 + type));

    }

    /**
     * _more_
     *
     * @param title _more_
     * @param msg _more_
     *
     * @return _more_
     */
    @Override
    public String encodeWarning(String title, String msg) {
        //For now
        if (true) {
            return super.encodeWarning(title, msg);
        }

        return encodeMessage(title, msg, "warning");
    }


    /**
     * _more_
     *
     * @param title _more_
     * @param msg _more_
     *
     * @return _more_
     */
    @Override
    public String encodeInfo(String title, String msg) {
        //For now
        if (true) {
            return super.encodeInfo(title, msg);
        }

        return encodeMessage(title, msg, "hint");
    }







    /**
     * _more_
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
     * @throws Exception On badness
     */
    @Override
    public Result makeEntryResult(Repository repository,
                                  CommandRequest request, String message,
                                  List<Entry> entries, String webHook,
                                  boolean showChildren)
            throws Exception {

        if ((entries != null) && (entries.size() == 0)) {
            String msg = encodeWarning("Search Results", "Nothing found");

            return new Result("",
                              new StringBuilder(Hipchat.encodeMessage(msg,
                                  "red")));
        }

        return Hipchat.makeEntryResult(repository, request.getRequest(),
                                       message, entries, webHook,
                                       showChildren);

    }


    /**
     * _more_
     *
     * @param message _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void postMessage(String message) throws Exception {
        Hipchat.postMessage(getWebHook(), message);
    }


    /**
     * Class description
     *
     *
     * @version        $version$, Tue, Oct 6, '15
     * @author         Enter your name here...
     */
    public static class HipchatRequest extends CommandRequest {

        /** _more_ */
        private String json;

        /** _more_ */
        private String fromId;

        /** _more_ */
        private String fromName;

        /** _more_ */
        private String room;


        /** _more_ */
        private String command;

        /** _more_ */
        private String text;

        /**
         * _more_
         *
         * @param request _more_
         *
         * @throws Exception On badness
         */
        public HipchatRequest(Request request) throws Exception {
            super(request);

            BufferedReader reader =
                request.getHttpServletRequest().getReader();
            StringBuilder json = new StringBuilder();
            String        line;
            while ((line = reader.readLine()) != null) {
                json.append(line);
                json.append("\n");
            }
            //            System.err.println("Hipchat request:" + request);

            System.err.println("HipChat Json:" + json);
            this.json = json.toString();
            JSONObject obj = new JSONObject(new JSONTokener(json.toString()));
            if (obj.has("item")) {
                this.fromId = JsonUtil.readValue(obj, "item.message.from.id", "");
                this.fromName = JsonUtil.readValue(obj, "item.message.from.name",
                        "");
                this.room = JsonUtil.readValue(obj, "item.room.id", "");

                String message = JsonUtil.readValue(obj, "item.message.message",
                                     null);
                //                System.err.println("message:" + message);
                List<String> toks = StringUtil.splitUpTo(message, " ", 2);
                this.command = toks.get(0);
                if (toks.size() > 1) {
                    this.text = toks.get(1);
                } else {
                    this.text = "";
                }
            }
        }

        /**
         * _more_
         *
         * @return _more_
         */
        @Override
        public String getFrom() {
            return fromName;
        }


        /**
         * _more_
         *
         * @return _more_
         */
        @Override
        public String getCommand() {
            return command;
        }



        /**
         * _more_
         *
         * @return _more_
         */
        public String getText() {
            return text;
        }


        /**
         * _more_
         *
         * @return _more_
         */
        @Override
        public int getSearchLimit() {
            return 100;
        }


        /**
         * _more_
         *
         * @return _more_
         */
        public String getChannelId() {
            return "room";
        }
    }



    /**
     * _more_
     *
     *
     * @param cmdRequest _more_
     * @param entry _more_
     * @param file _more_
     * @param channel _more_
     * @param title _more_
     * @param desc _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public Result sendFile(CommandHarvester.CommandRequest cmdRequest,
                           Entry entry, File file, String channel,
                           String title, String desc)
            throws Exception {

        String token = getApiToken();
        if ( !Utils.stringDefined(token)) {
            System.err.println(
                "HipchatHarvester.sendFile: no API token defined");

            return null;
        }

        HipchatRequest        hr       = (HipchatRequest) cmdRequest;
        CloseableHttpClient   client   = HttpClients.createDefault();
        CloseableHttpResponse response = null;

        try {
            String base = "https://" + new URL(getWebHook()).getHost();
            channel = hr.room;
            String url =
                base
                + Hipchat.API_ROOM_UPLOAD_FILE.replace(Hipchat.MACRO_ROOM_ID,
                    channel);
            url += "?" + HtmlUtils.arg(Hipchat.ARG_AUTH_TOKEN, token);
            System.err.println("HipchatHarvester.sendFile URL = " + url);
            HttpPost post = new HttpPost(url);
            post.addHeader("Content-Type",
                           "multipart/related; boundary=boundary123456");
            //TODO: get the mime type of the image
            FileBody filePart =
                new FileBody(
                    file, ContentType.create("image/png", "UTF-8"),
                    getRepository().getStorageManager().getOriginalFilename(
                        file.getName()));


            MultipartEntityBuilder mpe = MultipartEntityBuilder.create();
            mpe.setBoundary("boundary123456");
            String message = "{\"message\": \"" + title + "\"}";
            mpe.addPart("file", filePart);
            mpe.addPart("metadata",
                        new StringBody(message,
                                       ContentType.create("application/json",
                                           "UTF-8")));
            HttpEntity requestEntity = mpe.build();
            post.setEntity(requestEntity);
            response = client.execute(post);

            HttpEntity entity = response.getEntity();
            if (entity == null) {
                //Not sure if entity=null also means an error?
                return getNoop(cmdRequest);
            }
            String json = EntityUtils.toString(entity);
            //            System.err.println("json:" + json);
            EntityUtils.consume(entity);
            try {
                return message("Ok, file is on its way");
            } catch (Exception exc) {
                return message("Error:" + json);
            }
        } finally {
            if (response != null) {
                response.close();
            }
            client.close();
        }
    }




}
