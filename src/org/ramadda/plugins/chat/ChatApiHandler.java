/*
* Copyright (c) 2008-2019 Geode Systems LLC
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
*     http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.ramadda.plugins.chat;



import org.ramadda.repository.*;
import org.ramadda.repository.auth.User;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;
import org.ramadda.repository.util.DateArgument;
import org.ramadda.service.Service;
import org.ramadda.service.ServiceArg;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Json;
import org.ramadda.util.Utils;
import org.json.*;

import org.w3c.dom.Element;


import ucar.unidata.util.StringUtil;


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;




/**
 */
public class ChatApiHandler extends RepositoryManager implements RequestHandler {

    private static final long WAIT_DELAY = 1000*60;

    private static final int MESSAGE_HISTORY  =10;
    private Hashtable<String,ChatRoom> rooms = new Hashtable<String,ChatRoom>();


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



    public synchronized ChatRoom getRoom(Entry entry) {
        ChatRoom room = rooms.get(entry.getId());
        if(room == null) rooms.put(entry.getId(),room = new ChatRoom(entry));
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
        Result result = new Result("", json, Json.MIMETYPE);
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
        Entry entry  = getEntryManager().getEntry(request, request.getString(ARG_ENTRYID,""));
        String input = request.getString("input","");
        JSONObject obj = new JSONObject(new JSONTokener(input));
        String  command = obj.getString("command");
        StringBuilder sb = new StringBuilder();
        if(command.equals("message")) {
            String message = obj.getString("message");
            String output  = getWikiManager().wikifyEntry(request, entry, message);
            output = getPageHandler().translate(request, output);
            ChatRoom room = getRoom(entry);
            room.addMessage(new ChatMessage(message, getUserName(request.getUser())));
            synchronized(room) {
                room.notifyAll(output, getUserName(request.getUser()));
            }
            sb.append(Json.mapAndQuote("code","ok","message","output sent"));
            return returnJson(request,sb);
        }
        if(command.equals("connect")) {
            ChatRoom room = getRoom(entry);
            synchronized(room) {
                room.notifyAll("New user: " + getUserName(request.getUser()), "chat");
            }

            List<String> messageList  = new ArrayList<String>();
            for(ChatMessage chatMessage: room.messages) {
                String message  = getWikiManager().wikifyEntry(request, entry, chatMessage.message);
                messageList.add(Json.mapAndQuote("message",message,"user", chatMessage.user));
            }
            String messages = Json.list(messageList);
            sb.append(Json.map("code","\"ok\"","messages",messages));
            return returnJson(request,sb);
        }


        return returnJson(request,sb);
    }

    private String getUserName(User user) {
        String name = user.getName().trim();
        if(name.length()==0) name = user.getId();
        return name;
    }

    public  Result processChatOutput(Request request) throws Exception {
        Entry entry  = getEntryManager().getEntry(request, request.getString(ARG_ENTRYID,""));
        ChatRoom room = getRoom(entry);
        synchronized(room) {
            room.wait();
        }
        StringBuilder sb = new StringBuilder();
        String user = room.latestUser;
        String messages = Json.list(Json.mapAndQuote("message",room.latestInput,"user", user));
        sb.append(Json.map("code","\"ok\"","messages",messages));
        return returnJson(request,sb);
    }

    static class ChatRoom {
        Entry entry;
        String latestInput;
        String latestUser;
        List<ChatMessage> messages = new ArrayList<ChatMessage>();

        public ChatRoom(Entry entry) {
            this.entry= entry;
        }

        public synchronized void notifyAll(String input, String user) {
            this.latestInput = input;
            this.latestUser = user;
            this.notifyAll();
        }

        public void addMessage(ChatMessage message) {
            messages.add(message);
            if(messages.size()>MESSAGE_HISTORY)
                messages.remove(0);
        }
    }


    static class ChatMessage {
        String message;
        String user;
        public ChatMessage(String msg, String user) {
            this.message = msg;
            this.user = user;
        }
    }

}
