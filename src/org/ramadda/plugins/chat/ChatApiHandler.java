/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.chat;


import org.json.*;



import org.ramadda.repository.*;
import org.ramadda.repository.auth.User;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;
import org.ramadda.repository.util.DateArgument;
import org.ramadda.service.Service;
import org.ramadda.service.ServiceArg;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.Utils;

import org.w3c.dom.Element;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.StringUtil;


import java.io.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;




/**
 */
public class ChatApiHandler extends RepositoryManager implements RequestHandler {

    /** _more_ */
    private static final long WAIT_DELAY = 1000 * 60;

    /** _more_ */
    private static final int MESSAGE_HISTORY = 50;

    /** _more_ */
    private static final int SAVE_SIZE = 10;

    /** _more_ */
    private Hashtable<String, ChatRoom> rooms = new Hashtable<String,
                                                    ChatRoom>();


    /**
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     * @param props _more_
     *
     * @throws Exception _more_
     */
    public ChatApiHandler(Repository repository, Element node,
                          Hashtable props)
            throws Exception {
        super(repository);
    }




    /**
     * _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void shutdown() throws Exception {
        super.shutdown();
        for (Enumeration keys = rooms.keys(); keys.hasMoreElements(); ) {
            String   key  = (String) keys.nextElement();
            ChatRoom room = (ChatRoom) rooms.get(key);
            try {
                saveRoom(room);
            } catch (Exception exc) {
                System.err.println("Error saving chat room");
                exc.printStackTrace();

                throw exc;
            }
        }
    }

    /**
     * _more_
     *
     * @param room _more_
     *
     * @throws Exception _more_
     */
    private void saveRoom(ChatRoom room) throws Exception {
        room.saveCnt = 0;
        Request request = getRepository().getTmpRequest();
        Entry   entry   = room.entry;
        List<Metadata> metadataList =
            getMetadataManager().findMetadata(request, entry, "chat_file",
                false);

        List<String> messageList = new ArrayList<String>();
        for (ChatMessage chatMessage : room.messages) {
            messageList.add(
                JsonUtil.mapAndQuote(Utils.makeListFromValues(
						    "message", chatMessage.message, "user", chatMessage.user,
						    "date", DateUtil.getTimeAsISO8601(chatMessage.date))));
        }
        String   messages = JsonUtil.list(messageList);
        String   json     = JsonUtil.map(Utils.makeListFromValues("messages", messages));
        Metadata metadata = null;
        if ((metadataList != null) && (metadataList.size() > 0)) {
            metadata = metadataList.get(0);
        }
        if (metadata == null) {
            File f = getStorageManager().getTmpFile(request, "chat.json");
            IOUtil.writeFile(f, json);
            String theFile = getStorageManager().moveToEntryDir(entry,
                                 f).getName();
            getMetadataManager().addMetadata(request,
                entry,
                new Metadata(
			     getRepository().getGUID(), entry.getId(), getMetadataManager().findType("chat_file"),
                    false, theFile, "", "", "", ""));
            getEntryManager().updateEntry(null, entry);
        } else {
            File file = getMetadataManager().getFile(request, entry,
                            metadata, 1);
            getStorageManager().writeFile(file, json);
        }
    }



    /**
     * _more_
     *
     * @param room _more_
     *
     * @throws Exception _more_
     */
    private void loadRoom(ChatRoom room) throws Exception {
        Request request = getRepository().getTmpRequest();
        Entry   entry   = room.entry;
        List<Metadata> metadataList =
            getMetadataManager().findMetadata(request, entry, "chat_file",
                false);

        if ((metadataList == null) || (metadataList.size() == 0)) {
            return;
        }
        Metadata   metadata = metadataList.get(0);
        File f = getMetadataManager().getFile(request, entry, metadata, 1);
        String     json     = IOUtil.readContents(f);
        JSONObject obj      = new JSONObject(new JSONTokener(json));
        JSONArray  messages = obj.getJSONArray("messages");
        for (int i = 0; i < messages.length(); i++) {
            JSONObject message = messages.getJSONObject(i);
            Date dttm = getDateHandler().parseDate(message.optString("date"));
            String     m       = message.optString("message");
            m = getWikiManager().wikifyEntry(request, entry, m);
            room.addMessage(new ChatMessage(m, message.optString("user"),
                                            dttm));
        }
    }


    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public synchronized ChatRoom getRoom(Entry entry) throws Exception {
        ChatRoom room = rooms.get(entry.getId());
        if (room == null) {
            rooms.put(entry.getId(), room = new ChatRoom(entry));
            loadRoom(room);
        }

        return room;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param json _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Result returnJson(Request request, StringBuilder json)
            throws Exception {
        Result result = new Result("", json, JsonUtil.MIMETYPE);
        request.setCORSHeaderOnResponse();

        return result;
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
    public Result processChatInput(Request request) throws Exception {
        Entry entry = getEntryManager().getEntry(request,
                          request.getString(ARG_ENTRYID, ""));
        String        input   = request.getString("input", "");
        JSONObject    obj     = new JSONObject(new JSONTokener(input));
        String        command = obj.getString("command");
        StringBuilder sb      = new StringBuilder();
        if (command.equals("message")) {
            String message = obj.getString("message");
            String output = getWikiManager().wikifyEntry(request, entry,
                                message);
            ChatRoom room = getRoom(entry);
            room.addMessage(new ChatMessage(message,
                                            getUserName(request.getUser())));
            if (room.saveCnt > SAVE_SIZE) {
                saveRoom(room);
            }
            synchronized (room) {
                room.notifyAll(output, getUserName(request.getUser()));
            }
            sb.append(JsonUtil.mapAndQuote(Utils.makeListFromValues("code", "ok", "message",
							  "output sent")));

            return returnJson(request, sb);
        }


        if (command.equals("clearall")) {
            boolean canEdit = getAccessManager().canDoEdit(request, entry);
            if ( !canEdit) {
                sb.append(JsonUtil.mapAndQuote(Utils.makeListFromValues("code", "notok", "message",
							      "no permissions to clear all")));

                return returnJson(request, sb);
            }

            ChatRoom room = getRoom(entry);
            room.clearAll();
            saveRoom(room);
            sb.append(JsonUtil.mapAndQuote(Utils.makeListFromValues("code", "ok", "message", "cleared")));

            return returnJson(request, sb);
        }

        if (command.equals("connect")) {
            ChatRoom room = getRoom(entry);
            synchronized (room) {
                room.notifyAll("New user: " + getUserName(request.getUser()),
                               "chat");
            }

            List<String> messageList = new ArrayList<String>();
            for (ChatMessage chatMessage : room.messages) {
                String message = getWikiManager().wikifyEntry(request, entry,
                                     chatMessage.message);
                messageList.add(JsonUtil.mapAndQuote(Utils.makeListFromValues("message", message, "user",
								    chatMessage.user)));
            }
            String messages = JsonUtil.list(messageList);
            sb.append(JsonUtil.map(Utils.makeListFromValues("code", "\"ok\"", "messages", messages)));

            return returnJson(request, sb);
        }


        return returnJson(request, sb);
    }

    /**
     * _more_
     *
     * @param user _more_
     *
     * @return _more_
     */
    private String getUserName(User user) {
        String name = user.getName().trim();
        if (name.length() == 0) {
            name = user.getId();
        }

        return name;
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
    public Result processChatOutput(Request request) throws Exception {
        Entry entry = getEntryManager().getEntry(request,
                          request.getString(ARG_ENTRYID, ""));
        ChatRoom room = getRoom(entry);
        synchronized (room) {
            room.wait(WAIT_DELAY);
        }
        StringBuilder sb   = new StringBuilder();
        String        user = room.latestUser;
        String messages = JsonUtil.list(JsonUtil.mapAndQuote(Utils.makeListFromValues("message",
									    room.latestInput, "user", user)));
        sb.append(JsonUtil.map(Utils.makeListFromValues("code", "\"ok\"", "messages", messages)));

        return returnJson(request, sb);
    }

    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Feb 28, '19
     * @author         Enter your name here...
     */
    static class ChatRoom {

        /** _more_ */
        Entry entry;

        /** _more_ */
        String latestInput;

        /** _more_ */
        String latestUser;

        /** _more_ */
        List<ChatMessage> messages = new ArrayList<ChatMessage>();

        /** _more_ */
        int saveCnt = 0;

        /**
         * _more_
         *
         * @param entry _more_
         */
        public ChatRoom(Entry entry) {
            this.entry = entry;
        }

        /**
         */
        public void clearAll() {
            messages    = new ArrayList<ChatMessage>();
            saveCnt     = 0;
            latestInput = null;
            latestUser  = null;
        }

        /**
         * _more_
         *
         * @param input _more_
         * @param user _more_
         */
        public synchronized void notifyAll(String input, String user) {
            this.latestInput = input;
            this.latestUser  = user;
            this.notifyAll();
        }

        /**
         * _more_
         *
         * @param message _more_
         */
        public void addMessage(ChatMessage message) {
            saveCnt++;
            messages.add(message);
            if (messages.size() > MESSAGE_HISTORY) {
                messages.remove(0);
            }
        }
    }


    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Feb 28, '19
     * @author         Enter your name here...
     */
    static class ChatMessage {

        /** _more_ */
        String message;

        /** _more_ */
        String user;

        /** _more_ */
        Date date;

        /**
         * _more_
         *
         * @param msg _more_
         * @param user _more_
         * @param date _more_
         */
        public ChatMessage(String msg, String user, Date date) {
            this.message = msg;
            this.user    = user;
            this.date    = date;
        }

        /**
         * _more_
         *
         * @param msg _more_
         * @param user _more_
         */
        public ChatMessage(String msg, String user) {
            this(msg, user, new Date());
        }

    }

}
