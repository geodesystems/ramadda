/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.atlassian;


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
public class HipchatGroupTypeHandler extends ExtensibleGroupTypeHandler {

    /** _more_ */
    private TTLCache<String, Entry> entryCache;


    /** _more_ */
    public static final int IDX_TOKEN = 0;

    /** _more_ */
    public static final int IDX_ROOMS = 1;

    /** _more_ */
    private SimpleDateFormat parseSdf =
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    /** _more_ */
    private SimpleDateFormat displaySdf = new SimpleDateFormat("HH:mm");



    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public HipchatGroupTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
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
    }


    /**
     * _more_
     *
     * @param groupEntry _more_
     * @param api _more_
     *
     * @return _more_
     */
    public String getUrl(Entry groupEntry, String api) {
        return groupEntry.getResource().getPath() + api;
    }


    /**
     * _more_
     *
     * @param groupEntry _more_
     * @param api _more_
     * @param roomId _more_
     *
     * @return _more_
     */
    public String roomUrl(Entry groupEntry, String api, String roomId) {
        return getUrl(groupEntry, api.replace(Hipchat.MACRO_ROOM_ID, roomId));
    }

    /**
     * _more_
     *
     * @param groupEntry _more_
     * @param api _more_
     * @param roomId _more_
     * @param messageId _more_
     *
     * @return _more_
     */
    public String messageUrl(Entry groupEntry, String api, String roomId,
                             String messageId) {
        return getUrl(groupEntry,
                      api.replace(Hipchat.MACRO_ROOM_ID,
                                  roomId).replace(Hipchat.MACRO_MESSAGE_ID,
                                      messageId));
    }

    /**
     * _more_
     *
     * @param groupEntry _more_
     *
     * @return _more_
     */
    private HashSet<String> getRoomsToShow(Request request,Entry groupEntry) {
        HashSet<String> roomsToShow = null;
        String          rooms       = (String) groupEntry.getValue(request,IDX_ROOMS);
        if (Utils.stringDefined(rooms)) {
            roomsToShow = new HashSet<String>(StringUtil.split(rooms, "\n",
                    true, true));
        }

        return roomsToShow;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param groupEntry _more_
     * @param parentEntry _more_
     * @param synthId _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public List<String> getSynthIds(Request request, SelectInfo select, Entry groupEntry,
                                    Entry parentEntry, String synthId)
            throws Exception {

        //        System.err.println("HipchatGroupTypeHandler.getSynthIds:" + synthId
        //                           + " parent:" + parentEntry.getName() + " group: "
        //                           + groupEntry.getName());
        List<String> ids = parentEntry.getChildIds();
        if (ids != null) {
            return ids;
        }
        ids = new ArrayList<String>();

        String token = getToken(request, groupEntry);
        if ( !Utils.stringDefined(token)) {
            return ids;
        }

        HashSet<String> roomsToShow = null;
        String          roomIds     = (String) groupEntry.getValue(request,IDX_ROOMS);
        if (Utils.stringDefined(roomIds)) {
            roomsToShow = new HashSet<String>(StringUtil.split(roomIds, "\n",
                    true, true));
        }


        if (synthId == null) {
            //do room listing

            JSONObject result = Hipchat.call(getRepository(),
                                             getUrl(groupEntry,
                                                 Hipchat.API_ROOM), token,
                                                     "");
            JSONArray rooms = result.getJSONArray("items");
            if (rooms == null) {
                return ids;
            }


            //https://ramadda.hipchat.com/v2/room?auth_token=...
            //this.from = JsonUtil.readValue(obj, "item.message.from.id", "");
            for (int i = 0; i < rooms.length(); i++) {
                JSONObject room = rooms.getJSONObject(i);
                Entry roomEntry = createRoomEntry(groupEntry, room,
                                      roomsToShow);
                if (roomEntry == null) {
                    continue;
                }
                getEntryManager().cacheSynthEntry(roomEntry);
                ids.add(roomEntry.getId());
            }
        } else {
            //do message listing

            String roomId =
                parentEntry.getStringValue(request,HipchatRoomTypeHandler.IDX_ROOM_ID, "");

            String url =
                getUrl(groupEntry,
                       Hipchat.API_ROOM_LATEST.replace(Hipchat.MACRO_ROOM_ID,
                           roomId));

            JSONObject result = Hipchat.call(getRepository(), url, token, "");

            if (result == null) {
                return ids;
            }
            JSONArray   messages       = result.getJSONArray("items");
            List<Entry> messageEntries = new ArrayList<Entry>();
            for (int i = 0; i < messages.length(); i++) {
                JSONObject message = messages.getJSONObject(i);
                Entry messageEntry = createMessageEntry(request, groupEntry,
                                         roomId, message);
                if (messageEntry != null) {
                    getEntryManager().cacheSynthEntry(messageEntry);
                    messageEntries.add(messageEntry);
                }
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
     *
     * @param groupEntry _more_
     * @param room _more_
     * @param roomsToShow _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Entry createRoomEntry(Entry groupEntry, JSONObject room,
                                  HashSet<String> roomsToShow)
            throws Exception {
        String roomId = JsonUtil.readValue(room, "id", "");
        String name   = JsonUtil.readValue(room, "name", "");
        if (roomsToShow != null) {
            if ( !(roomsToShow.contains(roomId)
                    || roomsToShow.contains(name))) {
                return null;
            }
        }
        Date   dttm = new Date();
        String id = getEntryManager().createSynthId(groupEntry, roomId);
        TypeHandler roomTypeHandler =
            getRepository().getTypeHandler("hipchat_room");
        Entry  roomEntry = new Entry(id, roomTypeHandler);
        String desc      = "";
        Resource resource = new Resource(new Resource(getUrl(groupEntry,
                                "/chat/room/" + roomId)));

        Object[] values = roomTypeHandler.makeEntryValues(null);
        values[HipchatRoomTypeHandler.IDX_ROOM_ID] = roomId;
        roomEntry.initEntry(name, desc, groupEntry, groupEntry.getUser(),
                            resource, "", Entry.DEFAULT_ORDER,
                            dttm.getTime(), dttm.getTime(), dttm.getTime(),
                            dttm.getTime(), values);
        roomEntry.setMasterTypeHandler(this);

        return roomEntry;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param groupEntry _more_
     * @param roomId _more_
     * @param message _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Entry createMessageEntry(Request request, Entry groupEntry,
                                     String roomId, JSONObject message)
            throws Exception {

        String messageId = JsonUtil.readValue(message, "id", "");
        String color     = JsonUtil.readValue(message, "color", "");
        String date      = JsonUtil.readValue(message, "date", "");

        String from      = JsonUtil.readValue(message, "from.name", null);
        if (from == null) {
            from = JsonUtil.readValue(message, "from", null);
        }

        String desc = JsonUtil.readValue(message, "message", "");
        Date   now  = new Date();
        Date   dttm = parseSdf.parse(date);
        String name = from + " - " + displaySdf.format(dttm);

        //        System.err.println ("Message  date: " + date +" dttm:" + dttm);
        String ramaddaRoomId = getEntryManager().createSynthId(groupEntry,
                                   roomId);
        Entry roomEntry = getEntryManager().getEntry(request, ramaddaRoomId);


        String id = getEntryManager().createSynthId(groupEntry,
                        roomId + ":" + messageId);

        TypeHandler messageTypeHandler =
            getRepository().getTypeHandler("hipchat_message");
        Entry    messageEntry = new Entry(id, messageTypeHandler);
        Resource resource     = new Resource();
        Object[] values       = messageTypeHandler.makeEntryValues(null);
        values[HipchatMessageTypeHandler.IDX_FROM]    = from;
        values[HipchatMessageTypeHandler.IDX_ROOM_ID] = roomId;
        values[HipchatMessageTypeHandler.IDX_COLOR]   = color;

        messageEntry.initEntry(name, desc, roomEntry, groupEntry.getUser(),
                               resource, "", Entry.DEFAULT_ORDER,
                               now.getTime(), now.getTime(), dttm.getTime(),
                               dttm.getTime(), values);

        messageEntry.setMasterTypeHandler(this);

        if (message.has("message_links")) {
            JSONArray links = message.getJSONArray("message_links");
            for (int i = 0; i < links.length(); i++) {
                JSONObject link = links.getJSONObject(i);
                String     url  = JsonUtil.readValue(link, "url", null);
                String thumbnailUrl = JsonUtil.readValue(link,
                                          "video.thumbnailUrl", null);
                String title = JsonUtil.readValue(link, "video.title", null);
                //                System.err.println("url:" + url + " " + title + " thumb:"  + thumbnailUrl);
                //TODO:add this as metadata
                getMetadataManager().addMetadata(request,messageEntry,
                        new Metadata(getRepository().getGUID(),
                                     messageEntry.getId(),
                                     getMetadataManager().findType(ContentMetadataHandler.TYPE_URL), false,
                                     url, title, null, null, null));
                if (thumbnailUrl != null) {
                    Metadata thumbnailMetadata =
                        new Metadata(getRepository().getGUID(),
                                     messageEntry.getId(),
                                     getMetadataManager().findType(ContentMetadataHandler.TYPE_THUMBNAIL),
                                     false, thumbnailUrl, null, null, null,
                                     null);
                    getMetadataManager().addMetadata(request,messageEntry,
                            thumbnailMetadata);
                }
            }
        }



        return messageEntry;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param groupEntry _more_
     * @param id _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public Entry makeSynthEntry(Request request, Entry groupEntry, String id)
            throws Exception {
        //System.err.println("HipchatGroup.makeSynthEntry id = " + id
        //                           + " group:" + groupEntry.getName());
        String token = getToken(request, groupEntry);
        if ( !Utils.stringDefined(token)) {
            return null;
        }


        List<String> toks   = StringUtil.split(id, ":", true, true);
        String       roomId = toks.get(0);

        JSONObject result = Hipchat.call(getRepository(),
                                         roomUrl(groupEntry,
                                             Hipchat.API_ROOM_GET,
                                             roomId), token, "");
        if (result == null) {
            return null;
        }

        Entry roomEntry = createRoomEntry(groupEntry, result,
                                          getRoomsToShow(request,groupEntry));
        if (roomEntry == null) {
            return null;
        }
        if (toks.size() == 1) {
            return roomEntry;
        }
        result = Hipchat.call(getRepository(),
                              messageUrl(groupEntry, Hipchat.API_MESSAGE_GET,
                                         roomId, toks.get(1)), token, "");
        if ((result == null) || !result.has("message")) {
            return null;
        }

        return createMessageEntry(request, groupEntry, roomId,
                                  result.getJSONObject("message"));
    }



}
