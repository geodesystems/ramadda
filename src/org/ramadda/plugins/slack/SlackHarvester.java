/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.slack;


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
public class SlackHarvester extends CommandHarvester {



    /**
     * _more_
     *
     * @param repository _more_
     * @param id _more_
     *
     * @throws Exception _more_
     */
    public SlackHarvester(Repository repository, String id) throws Exception {
        super(repository, id);
    }


    /**
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     *
     * @throws Exception _more_
     */
    public SlackHarvester(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }



    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public String getHelpUrl() {
        return getRepository().getUrlBase() + "/slack/index.html";
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getDescription() {
        return "Slack Harvester";
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getCommandTypeName() {
        return "Slack Harvester";

    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getMessageLimit() {
        return Slack.MAX_MESSAGE_LENGTH;
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public String getChannelId(CommandRequest request) throws Exception {
        return request.getRequest().getString(Slack.SLACK_CHANNEL_ID, "");
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public String getAuthToken(CommandRequest request) throws Exception {
        return request.getRequest().getString(Slack.SLACK_TOKEN, "none");
    }

    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getText(CommandRequest request) throws Exception {
        return Slack.getSlackText(request.getRequest());
    }





    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     */
    public String getStateKey(CommandRequest request) {
        return Slack.getSlackUserId(request.getRequest()) + "_"
               + Slack.getSlackChannelName(request.getRequest());
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
     * @throws Exception _more_
     */
    @Override
    public Result makeEntryResult(Repository repository,
                                  CommandRequest request, String message,
                                  List<Entry> entries, String webHook,
                                  boolean showChildren)
            throws Exception {

        return Slack.makeEntryResult(repository, request.getRequest(),
                                     message, entries, webHook, showChildren);


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

        if ( !Utils.stringDefined(getApiToken())) {
            return message("RAMADDA get command not enabled.");
        }


        CloseableHttpClient   client   = HttpClients.createDefault();
        CloseableHttpResponse response = null;

        try {
            HttpPost post =
                new HttpPost(Slack.getSlackApiUrl(Slack.API_FILES_UPLOAD));
            FileBody               filePart = new FileBody(file);
            MultipartEntityBuilder mpe      = MultipartEntityBuilder.create();

            mpe.addPart(Slack.ARG_FILE, filePart);
            mpe.addPart(
                Slack.ARG_FILENAME,
                new StringBody(
                    getRepository().getStorageManager().getOriginalFilename(
                        file.getName()), ContentType.TEXT_PLAIN));
            mpe.addPart(Slack.ARG_TOKEN,
                        new StringBody(getApiToken(),
                                       ContentType.TEXT_PLAIN));
            if (Utils.stringDefined(title)) {
                mpe.addPart(Slack.ARG_TITLE,
                            new StringBody(title, ContentType.TEXT_PLAIN));
            }
            if (desc != null) {
                mpe.addPart(Slack.ARG_INITIAL_COMMENT,
                            new StringBody(desc, ContentType.TEXT_PLAIN));

            }
            if (channel != null) {
                mpe.addPart(Slack.ARG_CHANNELS, new StringBody(channel,ContentType.TEXT_PLAIN));
            }

            HttpEntity requestEntity = mpe.build();
            post.setEntity(requestEntity);
            //            System.out.println("executing request " + post.getRequestLine());
            response = client.execute(post);

            HttpEntity entity = response.getEntity();
            if (entity == null) {
                return message("Error: no http entity?");
            }
            String json = EntityUtils.toString(entity);
            EntityUtils.consume(entity);
            //            System.err.println("post response:" + json);
            try {
                JSONObject obj = new JSONObject(json);
                if ( !JsonUtil.readValue(obj, Slack.JSON_OK,
                                     "false").equals("true")) {
                    String error = JsonUtil.readValue(obj, Slack.JSON_ERROR, "");

                    return message("Oops, got an error posting the file:"
                                   + error);
                }

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


    /**
     *
     *               colCnt = 0;
     *               List<Integer> columnsToUse = ((selectedColumns != null)
     *                       ? selectedColumns
     *                       : dfltCols);
     *               for (int colIdx : columnsToUse) {
     *                   String s = "" + cols.get(colIdx);
     *
     *
     *
     *                   if (colCnt++ == 0) {
     *                   } else {
     *                       lineSB.append(colDelimiter);
     *                   }
     *                   int width = colWidth;
     *                   if (s.length() > width) {
     *                       s = s.substring(0, width - 1 - 3) + "...";
     *                   }
     *                   s = s.replace("&", "&amp;").replace("<",
     *                                 "&lt;").replace(">", "&gt;");
     *                   if ( !doFile) {
     *                       if (s.matches("[-\\+0-9\\.]+")) {
     *                           lineSB.append(StringUtil.padLeft(s, width));
     *                       } else {
     *                           lineSB.append(StringUtil.padRight(s, width));
     *                       }
     *                   } else {
     *                       lineSB.append(s);
     *                   }
     *                   html.append("<td>");
     *                   html.append(s);
     *                   html.append("</td>\n");
     *               }
     *
     *               //Strip trailing whitespace
     *               sb.append(lineSB.toString().replaceAll("\\s+$", ""));
     *               sb.append("\n");
     *           }
     *
     *           return false;
     *       }
     */




}
