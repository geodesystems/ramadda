/**
Copyright (c) 2008-2024 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
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



@SuppressWarnings("unchecked")
public class SessionManager extends RepositoryManager {



    public static final String SESSION_PROPERTY_MESSAGES = "messages";

    /** The number of days a session is active in the database */
    private static final double SESSION_DAYS = 2.0;

    private String cookieName;

    private boolean topRepository = false;

    private Hashtable<String, UserSession> sessionMap = new Hashtable<String,
                                                            UserSession>();

    //This holds sessions for anonymous users. The timeout is 24 hours. Max size is 1000

    private TTLCache<String, UserSession> anonymousSessionMap =
        new TTLCache<String, UserSession>(1000 * 3600 * 24, 1000,
                     "Anonymous Session Map");



    private Cache<Object, Object> sessionExtra = new Cache<Object,
                                                     Object>(5000);


    private boolean addAnonymousCookie  = true;

    public SessionManager(Repository repository) {
        super(repository);
        this.topRepository = (repository.getParentRepository() == null);
        if (topRepository) {}
        else {}
        this.cookieName = "ramadda"
                          + repository.getUrlBase().replaceAll("/", "_")
                          + "_session";

    }

    public void init() {
        Misc.run(new Runnable() {
            public void run() {
                cullSessions();
            }
        });
    }



    @Override
    public void initAttributes() {
	addAnonymousCookie = getRepository().getProperty("ramadda.session.anonymouscookie",true);
        super.initAttributes();
    }

    public boolean addAnonymousCookie(Request request) {
	return addAnonymousCookie;
    }

    public void debugSession(Request request, String msg) {
        getRepository().debugSession(request, "\t"+msg);
    }

    private void deleteAllSessions() throws Exception {
        debugSession(null, "RAMADDA.deleteAllSessions");
        sessionMap = new Hashtable<String, UserSession>();
        anonymousSessionMap.clearCache();
    }

    public String putSessionExtra(Object value) {
        String id = "${" + getRepository().getGUID() + "}";
        putSessionExtra(id, value);

        return id;
    }

    public void putSessionExtra(Object key, Object value) {
        sessionExtra.put(key, value);
    }

    public Object getSessionExtra(Object key) {
        return sessionExtra.get(key);
    }


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
            debugSession(null,
                         "RAMADDA.cullSessions: removing old session:"
                         + session);
            removeSession(null, session.getId());
        }

    }

    public Entry getLastEntry(Request request) throws Exception {
        return (Entry) getSessionProperty(request, "lastentry");
    }

    public void setLastEntry(Request request, Entry entry) throws Exception {
        if ((entry != null) && (request != null)) {
            putSessionProperty(request, "lastentry", entry);
        }
    }

    public void setLocation(Request request, double latitude, double longitude)
	throws Exception {
        if ( !Double.isNaN(latitude) && !Double.isNaN(longitude)) {
            putSessionProperty(request, ARG_LOCATION_LATITUDE,
                               latitude + ";" + longitude);
        }
    }



    public void setArea(Request request, double north, double west,
                        double south, double east)
            throws Exception {
        if ( !Double.isNaN(north) && !Double.isNaN(west)
                && !Double.isNaN(south) && !Double.isNaN(east)) {
            putSessionProperty(request, ARG_AREA,
                               north + ";" + west + ";" + south + ";" + east
                               + ";");
        }
    }

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
        UserSession session = getSession(request, id);
        if (session == null) {
            request.putExtraProperty(key, value);

            return;
        }
        session.putProperty(key, value);
    }

    public void removeSessionProperty(Request request, Object key)
            throws Exception {

        if (request == null) {
            return;
        }

        String id = request.getSessionId();
        if (id == null) {
            return;
        }
        UserSession session = getSession(request, id);
        if (session == null) {
            return;
        }
        session.removeProperty(key);
    }

    public Object getSessionProperty(Request request, Object key)
            throws Exception {
        return getSessionProperty(request, key, null);
    }

    public Object getSessionProperty(Request request, Object key, Object dflt)
            throws Exception {
        //JIC
        if (request == null) {
            return dflt;
        }

        String id = request.getSessionId();
        if (id == null) {
            Object obj = request.getExtraProperty(key);
            if (obj != null) {
                return obj;
            }

            return dflt;
        }
        UserSession session = getSession(request, id);
        if (session == null) {
            Object obj = request.getExtraProperty(key);
            if (obj != null) {
                return obj;
            }

            return dflt;
        }

        return session.getProperty(key);
    }


    public List<SessionMessage> getSessionMessages(Request request,Object key) throws Exception {
	List<SessionMessage> messages=(List<SessionMessage>)
	    getSessionProperty(request,SessionManager.SESSION_PROPERTY_MESSAGES);
	if(messages==null) return null;
	List<SessionMessage> tmp = new ArrayList<SessionMessage>();
	for(SessionMessage message: messages) {
	    if(message.error) {
		tmp.add(message);
		continue;
	    }
	    if(key==null) {
		if(message.key==null)
		    tmp.add(message);
	    } else if(message.match(key)) {
		tmp.add(message);
	    }
	}
	return tmp;
    }

    private void clearSessionMessages(Request request, boolean justErrors,
				      Object key, String msg)  {
	try {
	    List<SessionMessage> messages=(List<SessionMessage>)
		getSessionProperty(request,SessionManager.SESSION_PROPERTY_MESSAGES);
	    if(messages==null) return;
	    List<SessionMessage> tmp = new ArrayList<SessionMessage>();
	    for(SessionMessage message: messages) {
		if(justErrors && message.error) {
		    continue;
		}
		if(message.match(key)) {
		    if(Misc.equals(msg,message.message)) continue;
		}
		tmp.add(message);
	    }
	    if(tmp.size()==0) {
		removeSessionProperty(request, SESSION_PROPERTY_MESSAGES);
	    } else {
		putSessionProperty(request, SESSION_PROPERTY_MESSAGES,tmp);
	    }
	} catch(Exception exc) {
	    getLogManager().logError("Clearing session messages",exc);
	}
    }


    public void clearSessionMessage(Request request, Object key,String msg)   {
	clearSessionMessages(request, false,key,msg);
    }


    public void clearSessionMessages(Request request) {
	clearSessionMessages(request, true,null,null);
    }

    public static class SessionMessage {
	boolean error = true;
	Object key;
	String message;
	public SessionMessage(String message,Object key, boolean error) {
	    this.message = message;
	    this.key = key;
	    this.error=error;
	}

	public boolean match(Object k) {
	    return Misc.equals(k,key);
	}

	public String toString() {
	    return message;
	}
    }

    public void addSessionMessage(Request request, String message) {
	addSessionMessage(request, message, null,true);
    }

    public void addSessionMessage(Request request, String message,Object key,boolean error)  {
	try {
	    List<SessionMessage> messages=(List<SessionMessage>)
		getSessionProperty(request,SessionManager.SESSION_PROPERTY_MESSAGES);
	    if(messages==null) {
		messages= new ArrayList<SessionMessage>();
		putSessionProperty(request,SessionManager.SESSION_PROPERTY_MESSAGES,
				   messages);
	    }
	    messages.add(new SessionMessage(message,key,error));
	} catch(Exception ignore) {
	    getLogManager().logError("Error putting session error message:" + message,ignore);
	}
    }

    public UserSession getSession(Request request, String sessionId)
            throws Exception {
        return getSession(request, sessionId, true);
    }

    public UserSession getSession(Request request, String sessionId,
                                  boolean checkAnonymous)
            throws Exception {
        return getSession(request, sessionId, checkAnonymous, false);
    }

    private UserSession getSession(Request request, String sessionId,
                                   boolean checkAnonymous, boolean debug)
            throws Exception {
        UserSession session = sessionMap.get(sessionId);
        if (session != null) {
	    //            debugSession(request, "getSession: got session from session map:"+ session);

            return session;
        }
        session = anonymousSessionMap.get(sessionId);
        if (session != null) {
	    if(Repository.debugSession)
		debugSession(request,"getSession: got session from anon session map: " + session);

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
                session.setLastActivity(new Date());
                //Remove it from the DB and then re-add it so we update the lastActivity
                debugSession(request,  "getSession: got session from database:" + session);
                removeSession(request, session.getId());
                addSession(session);

                break;
            }
        } finally {
            getDatabaseManager().closeAndReleaseConnection(stmt);
        }

        if (session == null) {
            debugSession(request, "getSession: could not find session");
        }

        return session;
    }

    
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


    
    public void removeSession(Request request, String sessionId)
            throws Exception {
        debugSession(request, "removeSession:" + sessionId);
        sessionMap.remove(sessionId);
        anonymousSessionMap.remove(sessionId);
        getDatabaseManager().delete(Tables.SESSIONS.NAME,
                                    Clause.eq(Tables.SESSIONS.COL_SESSION_ID,
                                        sessionId));
    }

    
    private void addSession(UserSession session) throws Exception {
        debugSession(null, "addSession:" + session);
        sessionMap.put(session.getId(), session);
        getDatabaseManager().executeInsert(Tables.SESSIONS.INSERT,
                                           new Object[] { session.getId(),
                session.getUserId(), session.getCreateDate(),
                session.getLastActivity(),
                "" });
    }


    
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




    
    public void checkSession(Request request) throws Exception {

        //        debugSession(request, "RAMADDA: checkSession");
        User         user    = request.getUser();
        List<String> cookies = getCookies(request);
        if (Repository.debugSession) {
            debugSession(request, "checkSession cookies:" + cookies);
        }
        for (String cookieValue : cookies) {
            if (user == null) {
                if (Repository.debugSession) {
		    getRepository().debugSession(request, "checkSession: cookie:"+Utils.clip(cookieValue,10,"..."));
                }
                UserSession session = getSession(request, cookieValue, false);
                if (session != null) {
                    session.setLastActivity(new Date());
                    user = getUserManager().getCurrentUser(session.getUser());
                    session.setUser(user);
                    request.setSessionId(cookieValue);
                    if (Repository.debugSession) {
                        debugSession(request,   "checkSession: got session from cookie:" + session);
                    }
                    break;
                }
            }
        }


        if (request.defined(ARG_SESSIONID)) {
            if (user != null) {
                debugSession(
                    request,
                    "RAMADDA: has sessionid argument but also has a user:"
                    + user);
            }
            String sessionId = request.getString(ARG_SESSIONID);
            debugSession(request,
                         "RAMADDA: has sessionid argument:" + sessionId);
            UserSession session = getSession(request, sessionId, false, true);
            if (session != null) {
                session.setLastActivity(new Date());
                user = getUserManager().getCurrentUser(session.getUser());
                session.setUser(user);
                debugSession(request,
                             "RAMADDA: found sesssion user =" + user);
            } else {
                debugSession(request,
                             "RAMADDA: could not find session:" + sessionId);

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
            String authToken = getAuthManager().getAuthToken(sessionId);
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
                        Utils.decodeBase64(auth.substring(5).trim()));
                    String[] toks = Utils.split(auth, ":", 2);
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
	    if(addAnonymousCookie(request)) {
		request.setSessionId(createSessionId());
	    }
        }

        if (user == null) {
            user = getUserManager().getAnonymousUser();
            //Create a temporary session
	    if(addAnonymousCookie(request)) {
		UserSession session =  anonymousSessionMap.get(request.getSessionId());
		if (session == null) {
                    if (Repository.debugSession) 
			getRepository().debugSession(request,
						     "\t**** checkSession: adding anonymous session:"
						     + Utils.clip(request.getSessionId(),10,"..."));
		    session = new UserSession(request.getSessionId(), user, new Date());
		    anonymousSessionMap.put(request.getSessionId(), session);
		}
	    }
        }
        request.setUser(user);
    }



    
    private List<String> getCookies(Request request) throws Exception {
        List<String> cookies = new ArrayList<String>();
        String       cookie  = request.getHeaderArg("Cookie");
        if (cookie == null) {
            return cookies;
        }

        //        System.err.println ("Cookie:" + cookie);
        List toks = Utils.split(cookie, ";", true, true);
        for (int i = 0; i < toks.size(); i++) {
            String tok     = (String) toks.get(i);
            List   subtoks = Utils.split(tok, "=", true, true);
            if (subtoks.size() != 2) {
                continue;
            }
            String cookieName  = (String) subtoks.get(0);
            String cookieValue = (String) subtoks.get(1);
            if (cookieName.equals(getSessionCookieName())) {
                cookies.add(cookieValue);
            }
        }


        return cookies;
    }



    
    public String getSessionCookieName() {
        return cookieName;
    }





    
    public String createSessionId() {
	String session = getRepository().getGUID() + "_" + Math.random();
	//	System.err.println("create session id:" + Utils.clip(session,10,"..."));
	//	System.err.println(Utils.getStack(10));
	return session;
    }


    
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


    
    public void removeUserSession(Request request) throws Exception {
        if (request.getSessionId() != null) {
            removeSession(request, request.getSessionId());
        }
        List<String> cookies = getCookies(request);
        for (String cookieValue : cookies) {
            removeSession(request, cookieValue);
        }
        request.setUser(getUserManager().getAnonymousUser());
    }

    
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
