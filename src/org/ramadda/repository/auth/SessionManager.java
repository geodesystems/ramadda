/**
Copyright (c) 2008-2025 Geode Systems LLC
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
	boolean debug  =false;
	List<SessionMessage> messages=(List<SessionMessage>)
	    getSessionProperty(request,SessionManager.SESSION_PROPERTY_MESSAGES);
	if(messages==null) return null;
	List<SessionMessage> tmp = new ArrayList<SessionMessage>();
	if(debug)System.err.println("#messages:" + messages.size());
	for(SessionMessage message: messages) {
	    if(message.key==null) {
		if(debug)System.err.println("\tno message key");
		tmp.add(message);
		continue;
	    }
	    if(key==null) {
		if(debug)System.err.println("\tno arg key");
		tmp.add(message);
		continue;		
	    }
	    if(message.match(key)) {
		if(debug)System.err.println("\tmatch key");
		tmp.add(message);
	    }
	}
	return tmp;
    }




    private void clearSessionMessages(Request request, Object key, String msg)  {
	try {
	    boolean debug  =false;
	    List<SessionMessage> messages=(List<SessionMessage>)
		getSessionProperty(request,SessionManager.SESSION_PROPERTY_MESSAGES);
	    if(messages==null) return;
	    List<SessionMessage> tmp = new ArrayList<SessionMessage>();
	    if(debug) System.err.println("clear:" + " key:" + key +" msg:" + msg);
	    for(SessionMessage message: messages) {
		if(message.sticky) {
		    //always keep sticky messages
		    tmp.add(message);
		    continue;
		}
	
		if(key==null && msg ==null) {
		    //clear all
		    continue;
		}

		if(key!=null && msg !=null) {
		    if(message.match(key) && message.message.equals(msg)) {
			continue;
		    }
		    tmp.add(message);
		    continue;
		}
		if(key!=null) {
		    if(message.match(key)) {
			continue;
		    }
		    tmp.add(message);
		    continue;		    
		}

		if(msg!=null) {
		    if(message.message.equals(msg)) {
			continue;
		    }
		}
		tmp.add(message);
	    }
	    if(tmp.size()==0) {
		removeSessionProperty(request, SESSION_PROPERTY_MESSAGES);
	    } else {
		if(debug)		System.err.println("# messages:" + tmp.size());
		putSessionProperty(request, SESSION_PROPERTY_MESSAGES,tmp);
	    }
	} catch(Exception exc) {
	    getLogManager().logError("Clearing session messages",exc);
	}
    }

    public synchronized void clearSessionMessage(Request request, SessionMessage sessionMessage) {
	try {
	    List<SessionMessage> messages=(List<SessionMessage>)
		getSessionProperty(request,SessionManager.SESSION_PROPERTY_MESSAGES);
	    if(messages==null) return;
	    messages.remove(sessionMessage);
	} catch(Exception ignore) {
	}
    }


    public void clearSessionMessage(Request request, Object key,String msg)   {
	clearSessionMessages(request, key,msg);
    }

    public void clearSessionMessages(Request request) {
	clearSessionMessages(request, null,null);
    }


    public SessionMessage addSessionMessage(Request request, String message) {
	return addSessionMessage(request, message, null,false,false);
    }

    public SessionMessage addSessionMessage(Request request, String message,Object key) {
	return addSessionMessage(request, message, key,false,false);
    }    

    public SessionMessage addRawSessionMessage(Request request, String message) {
	return addSessionMessage(request, message, null,true,false);
    }


    public SessionMessage addRawSessionMessage(Request request, String message,Object key) {
	return addSessionMessage(request, message, key,true,false);
    }

    public SessionMessage addStickySessionMessage(Request request, String message) {
	return addSessionMessage(request, message, null,true,true);
    }    

    public SessionMessage addStickySessionMessage(Request request, Object key,String message) {
	return addSessionMessage(request, message, key,true,true);
    }    

    public synchronized SessionMessage
	addSessionMessage(Request request, String message,Object key,boolean raw,boolean sticky)  {
	    //Don't add session messages if the request was from an API call
	    if(request.responseAsJson())
		return  new SessionMessage("","");
	try {
	    if(!raw) {
		message = HU.strictSanitizeString(message);
	    }
	    List<SessionMessage> messages=(List<SessionMessage>)
		getSessionProperty(request,SessionManager.SESSION_PROPERTY_MESSAGES);
	    if(messages==null) {
		messages= new ArrayList<SessionMessage>();
		putSessionProperty(request,SessionManager.SESSION_PROPERTY_MESSAGES,
				   messages);
	    }
	    SessionMessage sessionMessage = new SessionMessage(message,key);
	    sessionMessage.sticky = sticky;
	    messages.add(sessionMessage);
	    return sessionMessage;
	} catch(Exception ignore) {
	    getLogManager().logError("Error putting session error message:" + message,ignore);
	    return null;
	}
    }

    private void throwRequestError(Request request,String msg) {
	if ( !request.responseAsData()) {
	    request.put(ARG_RESPONSE, RESPONSE_JSON);
	}
	throw new IllegalArgumentException(msg);
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

    public void removeUserSession(Request request, User user) throws Exception {
        List<UserSession> sessions = getSessions();
        for (UserSession session : sessions) {
	    if(session.getUser().equals(user)) {
		removeSession(request, session.getId());
	    }
	}
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

	String userArg = null;
	String passwordArg = null;
        if (user == null) {
            userArg   = request.getString(ARG_AUTH_USER, null);
            passwordArg = request.getString(ARG_AUTH_PASSWORD, null);
	    if(userArg==null) {
		String auth = request.getHeaderArg("Authorization");
		if(auth!=null && auth.startsWith("Basic ")) {
		    try {
			auth = new String(Utils.decodeBase64(auth.substring("Basic ".length())));
		    } catch(Exception exc) {
			throwRequestError(request,"Badly formed Authorization:" + exc);
		    }
		    List<String>toks = Utils.splitUpTo(auth,":",2);
		    if(toks.size()!=2) {
			throwRequestError(request,"Badly formed Authorization");
		    }
		    userArg = toks.get(0);
		    passwordArg = toks.get(1);		    
		}
	    }
	}

        //Check for url auth
        if (user == null && userArg!=null && passwordArg!=null) {
            request.remove(ARG_AUTH_USER);
            request.remove(ARG_AUTH_PASSWORD);
            user = getUserManager().findUser(userArg, false);
            if (user == null) {
		throwRequestError(request,"Unknown user:" + userArg);
            }
            if ( !getUserManager().isPasswordValid(user, passwordArg)) {
		throwRequestError(request,"Incorrect password");
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

    public List<String> getCookies(Request request) throws Exception {
	return getCookies(request,getSessionCookieName());
    }

    public List<String> getCookies(Request request,String lookForCookieName) throws Exception {	
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
            if (lookForCookieName == null || cookieName.equals(lookForCookieName)) {
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
            sessionHtml.append(HU.row(HU.cols(
					      HU.href(url,HU.img(getIconUrl(ICON_DELETE)),HU.attrs("title","Delete Session"))
					      + " "
					      + session.getUser().getLabel(), formatDate(request,
											 session.getCreateDate()), formatDate(request,
															      session.getLastActivity()), session.getId())));
        }
        sessionHtml.append(HtmlUtils.formTableClose());

        return sessionHtml;
    }

}
