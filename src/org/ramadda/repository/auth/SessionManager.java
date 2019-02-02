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

package org.ramadda.repository.auth;


import org.ramadda.repository.*;


import org.ramadda.repository.database.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.TTLCache;
import org.ramadda.util.Utils;


import org.ramadda.util.sql.Clause;


import org.ramadda.util.sql.SqlUtil;

import ucar.unidata.util.Cache;
import ucar.unidata.util.DateUtil;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;

import java.io.File;


import java.io.UnsupportedEncodingException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


import java.sql.ResultSet;
import java.sql.Statement;


import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;





/**
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
public class SessionManager extends RepositoryManager {



    /** The number of days a session is active in the database */
    private static final double SESSION_DAYS = 2.0;


    /** _more_ */
    public static final String COOKIE_NAME = "repositorysession";


    /** _more_ */
    private String cookieName;

    /** _more_ */
    private boolean topRepository = false;



    /** _more_ */
    private Hashtable<String, UserSession> sessionMap = new Hashtable<String,
                                                            UserSession>();

    //This holds sessions for anonymous users. The timeout is 24 hours. Max size is 1000

    /** _more_ */
    private TTLCache<String, UserSession> anonymousSessionMap =
        new TTLCache<String, UserSession>(1000 * 3600 * 24, 1000);




    /** _more_ */
    private Cache<Object, Object> sessionExtra = new Cache<Object,
                                                     Object>(5000);


    /**
     * _more_
     *
     * @param repository _more_
     */
    public SessionManager(Repository repository) {
        super(repository);
        this.topRepository = (repository.getParentRepository() == null);
        if (topRepository) {}
        else {}
        this.cookieName = "ramadda"
                          + repository.getUrlBase().replaceAll("/", "_")
                          + "_session";

    }

    /**
     * _more_
     */
    public void init() {
        Misc.run(new Runnable() {
            public void run() {
                cullSessions();
            }
        });
    }


    /**
     * _more_
     *
     * @param msg _more_
     */
    public void debugSession(String msg) {
        getRepository().debugSession(msg);
    }

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    private void deleteAllSessions() throws Exception {
        debugSession("RAMADDA.deleteAllSessions");
        sessionMap = new Hashtable<String, UserSession>();
        anonymousSessionMap.clearCache();
    }


    /**
     * _more_
     *
     * @param value _more_
     *
     * @return _more_
     */
    public String putSessionExtra(Object value) {
        String id = "${" + getRepository().getGUID() + "}";
        putSessionExtra(id, value);

        return id;
    }


    /**
     * _more_
     *
     * @param key _more_
     * @param value _more_
     */
    public void putSessionExtra(Object key, Object value) {
        sessionExtra.put(key, value);
    }


    /**
     * _more_
     *
     * @param key _more_
     *
     * @return _more_
     */
    public Object getSessionExtra(Object key) {
        return sessionExtra.get(key);
    }



    /**
     * _more_
     */
    private void cullSessions() {
        //Wait a while before starting
        Misc.sleepSeconds(60);
        //        Misc.sleepSeconds(5);
        while (true) {
            try {
                cullSessionsInner();
            } catch (Exception exc) {
                logException("Culling sessions", exc);

                return;
            }
            //Wake up every minute to see if we're shutdown
            //but do the cull every hour
            for (int minuteIdx = 0; minuteIdx < 60; minuteIdx++) {
                Misc.sleepSeconds(60);
                if ( !getActive()) {
                    return;
                }
            }
        }
    }



    /**
     * _more_
     *
     * @throws Exception _more_
     */
    private void cullSessionsInner() throws Exception {
        List<UserSession> sessionsToDelete = new ArrayList<UserSession>();
        long              now              = new Date().getTime();
        Statement stmt = getDatabaseManager().select(Tables.SESSIONS.COLUMNS,
                             Tables.SESSIONS.NAME, (Clause) null);
        SqlUtil.Iterator iter = getDatabaseManager().getIterator(stmt);
        ResultSet        results;
        double           timeDiff = DateUtil.daysToMillis(SESSION_DAYS);
        while ((results = iter.getNext()) != null) {
            UserSession session        = makeSession(results);
            Date        lastActiveDate = session.getLastActivity();
            //Check if the last activity was > 24 hours ago
            if ((now - lastActiveDate.getTime()) > timeDiff) {
                sessionsToDelete.add(session);
            } else {}
        }
        for (UserSession session : sessionsToDelete) {
            debugSession("RAMADDA.cullSessions: removing old session:"
                         + session);
            removeSession(session.getId());
        }

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
    public Entry getLastEntry(Request request) throws Exception {
        return (Entry) getSessionProperty(request, "lastentry");
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    public void setLastEntry(Request request, Entry entry) throws Exception {
        if ((entry != null) && (request != null)) {
            putSessionProperty(request, "lastentry", entry);
        }
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param north _more_
     * @param west _more_
     * @param south _more_
     * @param east _more_
     *
     * @throws Exception _more_
     */
    public void setArea(Request request, double north, double west,
                        double south, double east)
            throws Exception {
        putSessionProperty(request, ARG_AREA,
                           north + ";" + west + ";" + south + ";" + east
                           + ";");
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param key _more_
     * @param value _more_
     *
     * @throws Exception _more_
     */
    public void putSessionProperty(Request request, Object key, Object value)
            throws Exception {

        //JIC
        if (request == null) {
            return;
        }

        String id = request.getSessionId();
        if (id == null) {
            request.putExtraProperty(key, value);

            return;
        }
        UserSession session = getSession(id);
        if (session == null) {
            request.putExtraProperty(key, value);

            return;
        }
        session.putProperty(key, value);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param key _more_
     *
     * @throws Exception _more_
     */
    public void removeSessionProperty(Request request, Object key)
            throws Exception {

        if (request == null) {
            return;
        }

        String id = request.getSessionId();
        if (id == null) {
            return;
        }
        UserSession session = getSession(id);
        if (session == null) {
            return;
        }
        session.removeProperty(key);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param key _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Object getSessionProperty(Request request, Object key)
            throws Exception {
        return getSessionProperty(request, key, null);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param key _more_
     * @param dflt _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Object getSessionProperty(Request request, Object key, Object dflt)
            throws Exception {
        //JIC
        if (request == null) {
            return dflt;
        }

        //        System.err.println("getSession:" + key);
        String id = request.getSessionId();
        if (id == null) {
            Object obj = request.getExtraProperty(key);
            if (obj != null) {
                return obj;
            }

            return dflt;
        }
        UserSession session = getSession(id);
        if (session == null) {
            Object obj = request.getExtraProperty(key);
            if (obj != null) {
                return obj;
            }

            return dflt;
        }

        return session.getProperty(key);
    }


    //TODO: we need to clean out old sessions every once in a while


    /**
     * _more_
     *
     * @param sessionId _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public UserSession getSession(String sessionId) throws Exception {
        return getSession(sessionId, true);
    }

    /**
     * _more_
     *
     * @param sessionId _more_
     * @param checkAnonymous _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public UserSession getSession(String sessionId, boolean checkAnonymous)
            throws Exception {
        return getSession(sessionId, checkAnonymous, false);
    }

    /**
     * _more_
     *
     * @param sessionId _more_
     * @param checkAnonymous _more_
     * @param debug _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public UserSession getSession(String sessionId, boolean checkAnonymous,
                                  boolean debug)
            throws Exception {
        UserSession session = sessionMap.get(sessionId);
        if (session != null) {
            //            debugSession("RAMADDA.getSession got session from session map:" + session);
            return session;
        }
        session = anonymousSessionMap.get(sessionId);
        if (session != null) {
            debugSession(
                "RAMADDA.getSession got session from anonymous session map: "
                + session);

            return session;
        }

        Statement stmt = getDatabaseManager().select(Tables.SESSIONS.COLUMNS,
                             Tables.SESSIONS.NAME,
                             Clause.eq(Tables.SESSIONS.COL_SESSION_ID,
                                       sessionId));
        try {
            SqlUtil.Iterator iter = getDatabaseManager().getIterator(stmt);
            ResultSet        results;
            while ((results = iter.getNext()) != null) {
                session = makeSession(results);
                debugSession("RAMADDA.getSession got session from database:"
                             + session);
                session.setLastActivity(new Date());
                //Remove it from the DB and then re-add it so we update the lastActivity
                removeSession(session.getId());
                addSession(session);

                break;
            }
        } finally {
            getDatabaseManager().closeAndReleaseConnection(stmt);
        }

        return session;
    }

    /**
     * _more_
     *
     * @param results _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private UserSession makeSession(ResultSet results) throws Exception {
        int    col       = 1;
        String sessionId = results.getString(col++);
        String userId    = results.getString(col++);
        User   user      = getUserManager().findUser(userId);
        if (user == null) {
            user = getUserManager().getAnonymousUser();
        }
        //See if we have it in the map
        UserSession session = sessionMap.get(sessionId);
        if (session != null) {
            return session;
        }
        Date createDate     = getDatabaseManager().getDate(results, col++);
        Date lastActiveDate = getDatabaseManager().getDate(results, col++);

        return new UserSession(sessionId, user, createDate, lastActiveDate);
    }


    /**
     * _more_
     *
     * @param sessionId _more_
     *
     * @throws Exception _more_
     */
    public void removeSession(String sessionId) throws Exception {
        debugSession("RAMADDA.removeSession:" + sessionId);
        sessionMap.remove(sessionId);
        anonymousSessionMap.remove(sessionId);
        getDatabaseManager().delete(Tables.SESSIONS.NAME,
                                    Clause.eq(Tables.SESSIONS.COL_SESSION_ID,
                                        sessionId));
    }

    /**
     * _more_
     *
     * @param session _more_
     *
     * @throws Exception _more_
     */
    private void addSession(UserSession session) throws Exception {
        //        debugSession("RAMADDA.addSession:" + session);
        sessionMap.put(session.getId(), session);
        getDatabaseManager().executeInsert(Tables.SESSIONS.INSERT,
                                           new Object[] { session.getId(),
                session.getUserId(), session.getCreateDate(),
                session.getLastActivity(),
                "" });
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private List<UserSession> getSessions() throws Exception {
        List<UserSession> sessions = new ArrayList<UserSession>();
        Statement stmt = getDatabaseManager().select(Tables.SESSIONS.COLUMNS,
                             Tables.SESSIONS.NAME, (Clause) null);
        SqlUtil.Iterator iter = getDatabaseManager().getIterator(stmt);
        ResultSet        results;
        while ((results = iter.getNext()) != null) {
            sessions.add(makeSession(results));
        }

        return sessions;
    }




    /**
     * _more_
     *
     * @param request _more_
     *
     * @throws Exception _more_
     */
    public void checkSession(Request request) throws Exception {

        debugSession("RAMADDA: checkSession");
        User         user    = request.getUser();
        List<String> cookies = getCookies(request);
        for (String cookieValue : cookies) {
            if (Repository.debugSession) {
                getRepository().debugSession("checkSession: cookie:"
                                             + cookieValue);
            }
            if (user == null) {
                UserSession session = getSession(cookieValue, false);
                if (session != null) {
                    session.setLastActivity(new Date());
                    user = getUserManager().getCurrentUser(session.getUser());
                    session.setUser(user);
                    request.setSessionId(cookieValue);
                    getRepository().debugSession(
                        "checkSession: got session from cookie");

                    break;
                }
            }
        }


        if (request.defined(ARG_SESSIONID)) {
            if (user != null) {
                debugSession(
                    "RAMADDA: has sessionid argument but also has a user:"
                    + user);
            }
            String sessionId = request.getString(ARG_SESSIONID);
            debugSession("RAMADDA: has sessionid argument:" + sessionId);
            UserSession session = getSession(sessionId, false, true);
            if (session != null) {
                session.setLastActivity(new Date());
                user = getUserManager().getCurrentUser(session.getUser());
                session.setUser(user);
                debugSession("RAMADDA: found sesssion user =" + user);
            } else {
                debugSession("RAMADDA: could not find session:" + sessionId);

                //Puke out of here
                throw new IllegalStateException("Invalid session:"
                        + sessionId);
                //                user = getUserManager().getAnonymousUser();
                //                session.setUser(user);
                //                request.setSessionId(createSessionId());
            }
        }


        //Check for url auth
        if ((user == null) && request.exists(ARG_AUTH_USER)
                && request.exists(ARG_AUTH_PASSWORD)) {
            String userId   = request.getString(ARG_AUTH_USER, "");
            String password = request.getString(ARG_AUTH_PASSWORD, "");
            request.remove(ARG_AUTH_USER);
            request.remove(ARG_AUTH_PASSWORD);
            user = getUserManager().findUser(userId, false);
            if (user == null) {
                if ( !request.responseAsData()) {
                    request.put(ARG_RESPONSE, RESPONSE_JSON);
                }

                throw new IllegalArgumentException(msgLabel("Unknown user")
                        + userId);
            }
            if ( !getUserManager().isPasswordValid(user, password)) {
                if ( !request.responseAsData()) {
                    request.put(ARG_RESPONSE, RESPONSE_JSON);
                }

                throw new IllegalArgumentException(msg("Incorrect password"));
            }

            String      sessionId = "auth.session." + user.getId();
            UserSession session   = sessionMap.get(sessionId);
            if (session == null) {
                Date now = new Date();
                session = new UserSession(sessionId, user, now, now);
                sessionMap.put(sessionId, session);
            }
            request.setSessionId(sessionId);
            request.setUser(user);
            String authToken = getRepository().getAuthToken(sessionId);
            request.put(ARG_AUTHTOKEN, authToken);
        }

        //Check for basic auth
        if (user == null) {
            String auth =
                (String) request.getHttpHeaderArgs().get("Authorization");
            if (auth == null) {
                auth = (String) request.getHttpHeaderArgs().get(
                    "authorization");
            }

            if (auth != null) {
                auth = auth.trim();
                //Authorization: Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==
                if (auth.startsWith("Basic")) {
                    auth = new String(
                        RepositoryUtil.decodeBase64(
                            auth.substring(5).trim()));
                    String[] toks = StringUtil.split(auth, ":", 2);
                    if (toks.length == 2) {
                        user = getUserManager().findUser(toks[0], false);
                        if (user == null) {}
                        else if ( !getUserManager().isPasswordValid(user,
                                toks[1])) {
                            user = null;
                        } else {}
                    }
                    if (user != null) {
                        createSession(request, user);
                    }
                }
            }
        }


        //Make sure we have the current user state
        user = getUserManager().getCurrentUser(user);

        if ((request.getSessionId() == null)
                && !request.defined(ARG_SESSIONID)) {
            request.setSessionId(createSessionId());
        }

        if (user == null) {
            user = getUserManager().getAnonymousUser();
            //Create a temporary session
            UserSession session =
                anonymousSessionMap.get(request.getSessionId());
            if (session == null) {
                getRepository().debugSession(
                    "checkSession: adding anonymous session:"
                    + request.getSessionId());
                session = new UserSession(request.getSessionId(), user,
                                          new Date());
                anonymousSessionMap.put(request.getSessionId(), session);
            }
        }


        request.setUser(user);


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
    private List<String> getCookies(Request request) throws Exception {
        List<String> cookies = new ArrayList<String>();
        String       cookie  = request.getHeaderArg("Cookie");
        if (cookie == null) {
            return cookies;
        }

        //        System.err.println ("Cookie:" + cookie);
        List toks = StringUtil.split(cookie, ";", true, true);
        for (int i = 0; i < toks.size(); i++) {
            String tok     = (String) toks.get(i);
            List   subtoks = StringUtil.split(tok, "=", true, true);
            if (subtoks.size() != 2) {
                continue;
            }
            String cookieName  = (String) subtoks.get(0);
            String cookieValue = (String) subtoks.get(1);
            if (cookieName.equals(getSessionCookieName())) {
                cookies.add(cookieValue);
            } else if (cookieName.equals(COOKIE_NAME)) {
                //For backwards compatability
                cookies.add(cookieValue);
            }
        }


        return cookies;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public String getSessionCookieName() {
        return cookieName;
        //        return COOKIE_NAME;
    }





    /**
     * _more_
     *
     * @return _more_
     */
    public String createSessionId() {
        return getRepository().getGUID() + "_" + Math.random();
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param user _more_
     *
     *
     * @return _more_
     * @throws Exception _more_
     */
    public UserSession createSession(Request request, User user)
            throws Exception {
        if (request.getSessionId() == null) {
            request.setSessionId(createSessionId());
        }
        UserSession session = new UserSession(request.getSessionId(), user,
                                  new Date());
        addSession(session);
        request.setUser(user);

        return session;
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @throws Exception _more_
     */
    public void removeUserSession(Request request) throws Exception {
        if (request.getSessionId() != null) {
            removeSession(request.getSessionId());
        }
        List<String> cookies = getCookies(request);
        for (String cookieValue : cookies) {
            removeSession(cookieValue);
        }
        request.setUser(getUserManager().getAnonymousUser());
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
    public StringBuffer getSessionList(Request request) throws Exception {
        List<UserSession> sessions = getSessions();
        StringBuffer sessionHtml   = new StringBuffer(HtmlUtils.formTable());
        sessionHtml.append(
            HtmlUtils.row(
                HtmlUtils.cols(
                    HtmlUtils.bold(msg("User")),
                    HtmlUtils.bold(msg("Since")),
                    HtmlUtils.bold(msg("Last Activity")),
                    HtmlUtils.bold(msg("Session ID")))));
        for (UserSession session : sessions) {
            String url = request.makeUrl(getRepositoryBase().URL_USER_LIST,
                                         ARG_REMOVESESSIONID,
                                         session.getId());
            sessionHtml.append(HtmlUtils.row(HtmlUtils.cols(HtmlUtils.href(url,
                    HtmlUtils.img(getIconUrl(ICON_DELETE))) + " "
                        + session.getUser().getLabel(), formatDate(request,
                            session.getCreateDate()), formatDate(request,
                                session.getLastActivity()), session.getId())));
        }
        sessionHtml.append(HtmlUtils.formTableClose());

        return sessionHtml;
    }






}
