/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.slack;


import org.json.*;


import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;
import org.ramadda.repository.util.SelectInfo;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.TTLCache;
import org.ramadda.util.Utils;

import org.w3c.dom.*;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.StringUtil;

import java.net.URL;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;


/**
 */
public class SlackTeamTypeHandler extends ExtensibleGroupTypeHandler {

    /** _more_ */
    private TTLCache<String, Entry> entryCache;


    /** _more_ */
    public static final int IDX_TEAM_ID = 0;

    /** _more_ */
    public static final int IDX_TEAM_DOMAIN = 1;

    /** _more_ */
    public static final int IDX_TOKEN = 2;

    /** _more_ */
    public static final int IDX_CHANNELS = 3;

    /** _more_ */
    private SimpleDateFormat displaySdf =
        new SimpleDateFormat("MMMMM dd - HH:mm");

    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public SlackTeamTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
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
    private String getToken(Request request, Entry entry) throws Exception {

        String token = (String) entry.getValue(request,IDX_TOKEN);
        if ( !Utils.stringDefined(token)) {
            return null;
        }
        String fromProp = getRepository().getProperty(token.trim(),
                              (String) null);
        if (fromProp != null) {
            return fromProp;
        }

        return token;
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param parent _more_
     * @param newEntry _more_
     *
     * @throws Exception _more_
     */
    public void initializeEntryFromForm(Request request, Entry entry,
                                        Entry parent, boolean newEntry)
            throws Exception {
        super.initializeEntryFromForm(request, entry, parent, newEntry);
        /*
        if ( !newEntry) {
            return;
        }
        */

        String token = getToken(request, entry);
        if ( !Utils.stringDefined(token)) {
            return;
        }


        JSONObject result = Slack.call(getRepository(), Slack.API_TEAM_INFO,
                                       token);
        if (result == null) {
            return;
        }

        entry.setName(JsonUtil.readValue(result, "team.name", entry.getName()));
        Object[] values = entry.getTypeHandler().getEntryValues(entry);
        values[IDX_TEAM_ID] = JsonUtil.readValue(result, "team.id", "");

        String domain = JsonUtil.readValue(result, "team.domain", "");
        values[IDX_TEAM_DOMAIN] = domain;
        entry.setResource(new Resource(Slack.getTeamUrl(domain)));
    }





    /**
     * _more_
     *
     * @param request _more_
     * @param teamEntry _more_
     * @param parentEntry _more_
     * @param synthId _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public List<String> getSynthIds(Request request, SelectInfo select, Entry teamEntry,
                                    Entry parentEntry, String synthId)
            throws Exception {

        //        System.err.println("SlackTeamTypeHandler.getSynthIds:" + synthId +" parent:" + parentEntry.getName()  +" team: " + teamEntry.getName());
        List<String> ids = parentEntry.getChildIds();
        if (ids != null) {
            return ids;
        }
        ids = new ArrayList<String>();

        String teamId = (String) teamEntry.getValue(request,IDX_TEAM_ID);
        if ( !Utils.stringDefined(teamId)) {
            return ids;
        }
        String token = getToken(request, teamEntry);
        if ( !Utils.stringDefined(token)) {
            return ids;
        }

        HashSet<String> channelsToShow = null;
        String          channelIds =
            (String) teamEntry.getValue(request,IDX_CHANNELS);
        if (Utils.stringDefined(channelIds)) {
            channelsToShow = new HashSet<String>(StringUtil.split(channelIds,
                    "\n", true, true));
        }


        if (synthId == null) {
            //do channel listing
            JSONObject result = Slack.call(getRepository(),
                                           Slack.API_CHANNELS_LIST, token);
            if (result == null) {
                return ids;
            }
            JSONArray channels = result.getJSONArray("channels");
            for (int i = 0; i < channels.length(); i++) {
                JSONObject channel = channels.getJSONObject(i);
                Entry channelEntry = createChannelEntry(request,teamEntry, channel,
                                         channelsToShow);
                if (channelEntry == null) {
                    continue;
                }
                getEntryManager().cacheSynthEntry(channelEntry);
                ids.add(channelEntry.getId());
            }
        } else {
            //do message listing
            //            Slack.debug = true;
            JSONObject result =
                Slack.call(getRepository(), Slack.API_SEARCH_MESSAGES, token,
                           HtmlUtils.arg(Slack.ARG_QUERY,
                                         Slack.in(parentEntry.getName())));
            Slack.debug = false;
            if (result == null) {
                return ids;
            }
            String channelId =
                parentEntry.getStringValue(request,SlackChannelTypeHandler.IDX_CHANNEL_ID,
                                     "");
            JSONObject  messages       = result.getJSONObject("messages");
            JSONArray   matches        = messages.getJSONArray("matches");
            List<Entry> messageEntries = new ArrayList<Entry>();
            for (int i = 0; i < matches.length(); i++) {
                JSONObject message = matches.getJSONObject(i);
                Entry messageEntry = createMessageEntry(request, teamEntry,
                                         channelId, message);
                getEntryManager().cacheSynthEntry(messageEntry);
                messageEntries.add(messageEntry);
            }
            messageEntries =
                getEntryManager().getEntryUtil().sortEntriesOnDate(
                    messageEntries, true);

            for (Entry messageEntry : messageEntries) {
                ids.add(messageEntry.getId());
            }

        }
        parentEntry.setChildIds(ids);

        return ids;
    }



    /**
     * _more_
     *
     * @param teamEntry _more_
     * @param channel _more_
     * @param channelsToShow _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Entry createChannelEntry(Request request,Entry teamEntry, JSONObject channel,
                                     HashSet<String> channelsToShow)
            throws Exception {
        /*
          "id": "C024BE91L",
            "name": "fun",
            "created": 1360782804,
            "creator": "U024BE7LH",
            "is_archived": false,
            "is_member": false,
            "num_members": 6,
            "topic": {
                "value": "Fun times",
                "creator": "U024BE7LV",
                "last_set": 1369677212
            },
            "purpose": {
                "value": "This channel is for fun",
                "creator": "U024BE7LH",
                "last_set": 1360782804
            }
        */

        String channelId = JsonUtil.readValue(channel, "id", "");
        String name      = JsonUtil.readValue(channel, "name", "");
        if (channelsToShow != null) {
            if ( !(channelsToShow.contains(channelId)
                    || channelsToShow.contains(name))) {
                return null;
            }
        }
        Date dttm = Slack.getDate(JsonUtil.readValue(channel, "created", ""));
        String id = getEntryManager().createSynthId(teamEntry, channelId);

        String topic = JsonUtil.readValue(channel, "topic.value", "");
        String purpose = JsonUtil.readValue(channel, "purpose.value", "");
        TypeHandler channelTypeHandler =
            getRepository().getTypeHandler("slack_channel");
        Entry channelEntry = new Entry(id, channelTypeHandler);
        //        https://geodesystems.slack.com/messages/general/

        String desc       = "";
        String teamDomain = (String) teamEntry.getStringValue(request,IDX_TEAM_DOMAIN, "");
        Resource resource =
            new Resource(new Resource(Slack.getChannelUrl(teamDomain, name)));
        Object[] values = channelTypeHandler.makeEntryValues(null);

        values[SlackChannelTypeHandler.IDX_CHANNEL_ID]      = channelId;
        values[SlackChannelTypeHandler.IDX_CHANNEL_PURPOSE] = purpose;

        channelEntry.initEntry(name, desc, teamEntry, teamEntry.getUser(),
                               resource, "", Entry.DEFAULT_ORDER,
                               dttm.getTime(), dttm.getTime(),
                               dttm.getTime(), dttm.getTime(), values);

        channelEntry.setMasterTypeHandler(this);

        return channelEntry;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param teamEntry _more_
     * @param channelId _more_
     * @param message _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Entry createMessageEntry(Request request, Entry teamEntry,
                                     String channelId, JSONObject message)
            throws Exception {

        String token    = getToken(request, teamEntry);
        String userId   = JsonUtil.readValue(message, "user", "");
        String userName = JsonUtil.readValue(message, "username", userId);

        if ( !Utils.stringDefined(userId)) {
            userId = userName;
        }

        SlackUser slackUser = getSlackUser(token, userId, userName);
        if (slackUser != null) {
            userName = slackUser.getName();
        }
        String ts          = JsonUtil.readValue(message, "ts", "");
        Date   dttm        = Slack.getDate(ts);
        String desc        = JsonUtil.readValue(message, "text", "");

        String pattern     = ".*?<([^>|]+)|([^>]+)>";
        String embeddedUrl = StringUtil.findPattern(desc, pattern);
        if ((embeddedUrl != null) && Utils.isImage(embeddedUrl)) {
            //            desc = desc + HtmlUtils.br() +HtmlUtils.href(embeddedUrl, embeddedUrl);
        }
        //        System.err.println("desc:" + desc);
        //xxxx

        String link  = JsonUtil.readValue(message, "permalink", (String) null);
        String dttms = displaySdf.format(dttm);
        String name  = userName + ": " + dttms;
        channelId = JsonUtil.readValue(message, "channel.id", channelId);

        String ramaddaChannelId = getEntryManager().createSynthId(teamEntry,
                                      channelId);
        Entry channelEntry = getEntryManager().getEntry(request,
                                 ramaddaChannelId);

        String id = getEntryManager().createSynthId(teamEntry,
                        channelId + ":" + ts);

        TypeHandler messageTypeHandler =
            getRepository().getTypeHandler("slack_message");
        Entry    messageEntry = new Entry(id, messageTypeHandler);
        Resource resource     = (link == null)
                                ? new Resource()
                                : new Resource(new URL(link));
        Object[] values       = messageTypeHandler.makeEntryValues(null);


        values[SlackMessageTypeHandler.IDX_USER_ID]   = userId;
        values[SlackMessageTypeHandler.IDX_USER_NAME] = userName;

        if ((slackUser != null)
                && Utils.stringDefined(slackUser.getImage48())) {
            values[SlackMessageTypeHandler.IDX_USER_IMAGE] =
                slackUser.getImage48();
        } else {
            values[SlackMessageTypeHandler.IDX_USER_IMAGE] =
                getRepository().getIconUrl("/slack/slack.png");
        }


        messageEntry.initEntry(name, desc, channelEntry, teamEntry.getUser(),
                               resource, "", Entry.DEFAULT_ORDER,
                               dttm.getTime(), dttm.getTime(),
                               dttm.getTime(), dttm.getTime(), values);

        messageEntry.setMasterTypeHandler(this);

        return messageEntry;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param teamEntry _more_
     * @param id _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public Entry makeSynthEntry(Request request, Entry teamEntry, String id)
            throws Exception {
        //        System.err.println("SlackTeam.makeSynthEntry id = " + id +" team:" + teamEntry.getName());
        String token = getToken(request, teamEntry);
        if ( !Utils.stringDefined(token)) {
            return null;
        }



        List<String> toks      = StringUtil.split(id, ":", true, true);
        String       channelId = toks.get(0);

        JSONObject result = Slack.call(getRepository(),
                                       Slack.API_CHANNELS_INFO, token,
                                       HtmlUtils.arg(Slack.ARG_CHANNEL,
                                           channelId));
        if (result == null) {
            return null;
        }

        HashSet<String> channelsToShow = null;
        String          channels = (String) teamEntry.getValue(request,IDX_CHANNELS);
        if (Utils.stringDefined(channels)) {
            channelsToShow = new HashSet<String>(StringUtil.split(channels,
                    "\n", true, true));
        }


        JSONObject channel = result.getJSONObject("channel");
        Entry channelEntry = createChannelEntry(request, teamEntry, channel,
                                 channelsToShow);
        if (channelEntry == null) {
            return null;
        }
        if (toks.size() == 1) {
            return channelEntry;
        }
        String msgId = toks.get(1);


        //        Slack.debug = true;
        result = Slack.call(getRepository(), Slack.API_CHANNELS_HISTORY,
                            token, HtmlUtils.args(new String[] {
            Slack.ARG_CHANNEL, channelId, Slack.ARG_LATEST, msgId,
            Slack.ARG_OLDEST, msgId, Slack.ARG_INCLUSIVE, "1"
        }));
        Slack.debug = false;
        if (result == null) {
            return null;
        }

        JSONArray messages = result.getJSONArray("messages");
        if (messages.length() == 0) {
            System.err.println("no messages");

            return null;
        }
        JSONObject message = messages.getJSONObject(0);

        return createMessageEntry(request, teamEntry, channelId, message);

    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isSynthType() {
        return true;
    }



    /**
     * _more_
     *
     * @param id _more_
     *
     * @return _more_
     */
    public Entry createEntry(String id) {
        return new Entry(id, this, true);
    }

    /**
     * _more_
     *
     * @param token _more_
     * @param userId _more_
     * @param userName _more_
     *
     * @return _more_
     */
    public SlackUser getSlackUser(String token, String userId,
                                  String userName) {
        SlackUser slackUser = SlackUser.getUser(token, userId);
        if (slackUser != null) {
            return slackUser;
        }


        JSONObject result = null;
        if ( !userId.equals(Slack.DFLT_USER_NAME)) {
            result = Slack.call(getRepository(), Slack.API_USERS_INFO, token,
                                HtmlUtils.args(new String[] { Slack.ARG_USER,
                    userId }));
        }
        if (result == null) {
            return new SlackUser(token, userId, userName, null, null);
        }

        JSONObject profile = JsonUtil.readObject(result, "user.profile");
        String     name    = JsonUtil.readValue(profile, "real_name", userId);
        String     image24 = JsonUtil.readValue(profile, "image_24", null);
        String     image48 = JsonUtil.readValue(profile, "image_48", null);

        return new SlackUser(token, userId, name, image24, image48);
    }

    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static final void main(String[] args) throws Exception {
        String text =
            "uplaoded file: <https://geodesystems.slack.com/files/jeffmc/F04RL38SR/real_gross_national_product_timeseries.png|Time series - Real Gross National Product> and commented: BEA Account Code: A001RL1  For more information about this series, please see <http://www.bea.gov/national/>.";

        String pattern = ".*?<([^>|]+)|([^>]+)>";
        System.err.println(StringUtil.findPattern(text, pattern));


    }


}
