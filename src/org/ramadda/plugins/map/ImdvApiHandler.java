/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.map;


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
public class ImdvApiHandler extends RepositoryManager implements RequestHandler {

    /** _more_ */
    private static final long WAIT_DELAY = 1000 * 60;

    /** _more_ */
    private static final int MESSAGE_HISTORY = 50;

    /** _more_ */
    private static final int SAVE_SIZE = 10;

    /** _more_ */
    private Hashtable<String, ImdvRoom> rooms = new Hashtable<String,
                                                    ImdvRoom>();


    /**
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     * @param props _more_
     *
     * @throws Exception _more_
     */
    public ImdvApiHandler(Repository repository, Element node,
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
            ImdvRoom room = (ImdvRoom) rooms.get(key);
            try {
		//                saveRoom(room);
            } catch (Exception exc) {
                System.err.println("Error saving Imdv room");
                exc.printStackTrace();
                throw exc;
            }
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
    public synchronized ImdvRoom getRoom(Entry entry) throws Exception {
        ImdvRoom room = rooms.get(entry.getId());
        if (room == null) {
            rooms.put(entry.getId(), room = new ImdvRoom(entry));
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
    public Result processImdvInput(Request request) throws Exception {
        Entry entry = getEntryManager().getEntry(request,
                          request.getString(ARG_ENTRYID, ""));
        String        input   = request.getString("input", "");
        JSONObject    obj     = new JSONObject(new JSONTokener(input));
        String        command = obj.getString("command");
	StringBuilder sb      = new StringBuilder();
        if (command.equals("action")) {
	    String glyph = obj.getString("glyph");
	    ImdvRoom room = getRoom(entry);
	    room.addMessage(new ImdvMessage(command,glyph));
	    synchronized (room) {
		room.notifyAll(command, getUserName(request.getUser()));
	    }
	    sb.append(JsonUtil.mapAndQuote(Utils.makeList("code", "ok", "message",
						      "output sent")));
	    return returnJson(request, sb);
        }

        if (command.equals("connect")) {
            ImdvRoom room = getRoom(entry);
            synchronized (room) {
                room.notifyAll("New user: " + getUserName(request.getUser()),
                               "imdv");
            }

            List<String> messageList = new ArrayList<String>();
            for (ImdvMessage imdvMessage : room.messages) {
                String message = getWikiManager().wikifyEntry(request, entry,
                                     imdvMessage.message);
                messageList.add(JsonUtil.mapAndQuote(Utils.makeList("message", message, "user",
								    imdvMessage.user)));
            }
            String messages = JsonUtil.list(messageList);
            sb.append(JsonUtil.map(Utils.makeList("code", "\"ok\"", "messages", messages)));

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
    public Result processImdvOutput(Request request) throws Exception {
        Entry entry = getEntryManager().getEntry(request,
                          request.getString(ARG_ENTRYID, ""));
        ImdvRoom room = getRoom(entry);
        synchronized (room) {
            room.wait(WAIT_DELAY);
        }
        StringBuilder sb   = new StringBuilder();
        String        user = room.latestUser;
        String messages = JsonUtil.list(JsonUtil.mapAndQuote(Utils.makeList("message",
									    room.latestInput, "user", user)));
        sb.append(JsonUtil.map(Utils.makeList("code", "\"ok\"", "messages", messages)));

        return returnJson(request, sb);
    }

    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Feb 28, '19
     * @author         Enter your name here...
     */
    static class ImdvRoom {

        /** _more_ */
        Entry entry;

        /** _more_ */
        String latestInput;

        /** _more_ */
        String latestUser;

        /** _more_ */
        List<ImdvMessage> messages = new ArrayList<ImdvMessage>();

        /** _more_ */
        int saveCnt = 0;

        /**
         * _more_
         *
         * @param entry _more_
         */
        public ImdvRoom(Entry entry) {
            this.entry = entry;
        }

        /**
         */
        public void clearAll() {
            messages    = new ArrayList<ImdvMessage>();
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
        public void addMessage(ImdvMessage message) {
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
    static class ImdvMessage {

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
        public ImdvMessage(String msg, String user, Date date) {
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
        public ImdvMessage(String msg, String user) {
            this(msg, user, new Date());
        }

    }

}
