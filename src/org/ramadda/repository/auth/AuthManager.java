/**
   Copyright (c) 2008-2023 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.auth;

import org.ramadda.repository.*;

import org.ramadda.util.IO;

import org.ramadda.util.Utils;
import org.ramadda.util.TTLCache;
import ucar.unidata.util.IOUtil;
import org.ramadda.util.JsonUtil;

import org.ramadda.util.FormInfo;

import org.json.*;
import java.io.*;
import java.util.Base64;
import java.util.ArrayList;
import java.util.List;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Random;
import java.net.URL;

import java.nio.charset.*;

import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Font;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;



/**
 * Handles auth stuff
 *
 * @author Jeff McWhirter
 */
@SuppressWarnings("unchecked")
public class AuthManager extends RepositoryManager {
    public static final String PROP_RECAPTCHA_SITEKEY = "google.recaptcha.sitekey";
    public static final String PROP_RECAPTCHA_SECRETKEY = "google.recaptcha.secret";    
    public static final String TOKEN_NO_SESSION = "nosession";
    private static final String ARG_EXTRA_PASSWORD = "extrapassword";
    private static final String DEFAULT_MESSAGE = "For verification please enter your current password";

    private boolean doCaptcha;
    private boolean doPassword;


    /** store the number of bad captcha attempts for each user. clear every hour */
    private TTLCache<String,Integer> badCaptchaCount =
	new TTLCache<String,Integer>(60*60*1000);

    private static int MAX_BAD_CAPTCHA_COUNT = 20;    

    private static String hashStringSalt = null;
    private static Object HASH_MUTEX = new Object();

    private Properties captchaMap;


    /**
     * ctor
     *
     * @param repository the repository
     */
    public AuthManager(Repository repository) {
        super(repository);
	doPassword = repository.getLocalProperty("ramadda.auth.dopassword",false);	
	doCaptcha = repository.getLocalProperty("ramadda.auth.dorecaptcha",false);
    }

    private static final int IMAGE_WIDTH = 140;
    private static final int IMAGE_HEIGHT =70;
    private static final int TEXTSIZE=24;



    public boolean isRecaptchaEnabled() {
	String siteKey = getRepository().getProperty(PROP_RECAPTCHA_SITEKEY,null);
	String secretKey = getRepository().getProperty(PROP_RECAPTCHA_SECRETKEY,null);	
	if(stringDefined(siteKey) && stringDefined(secretKey)) {
	    return true;
	}
	return false;
    }

    public String getRecaptcha(Request request) {
	StringBuilder sb = new StringBuilder();
	if(isRecaptchaEnabled()) {
	    sb.append("<script src='https://www.google.com/recaptcha/api.js' async defer></script>");
	    String siteKey = getRepository().getProperty(PROP_RECAPTCHA_SITEKEY,null);
	    HU.div(sb,"",HU.attrs("class","g-recaptcha ramadda-recaptcha","data-sitekey",siteKey));
	}
	return sb.toString();
    }

    public boolean checkRecaptcha(Request request, Appendable response)
	throws Exception {
	if(isRecaptchaEnabled()) {
	    String siteKey = getRepository().getProperty(PROP_RECAPTCHA_SITEKEY,null);
	    String secretKey = getRepository().getProperty(PROP_RECAPTCHA_SECRETKEY,null);	
	    String recaptchaResponse = request.getString("g-recaptcha-response",null);
	    if(recaptchaResponse==null) return false;
	    String url = HU.url("https://www.google.com/recaptcha/api/siteverify","secret",secretKey,"response",recaptchaResponse);
	    String json = IO.readUrl(new URL(url));
            JSONObject  obj   = new JSONObject(json);
	    if(!obj.getBoolean("success")) {
                response.append(HU.center(messageError("Sorry, you were not verified to be a human")));
		return false;
	    } else {
		return true;
	    }
	}
        return true;

    }

    public boolean verify(Request request,Appendable sb) throws Exception {
	return verify(request, sb, false);
    }

    public boolean verify(Request request,Appendable sb, boolean forcePassword) throws Exception {	
	if(doPassword || forcePassword) {
	    String password = request.getString(ARG_EXTRA_PASSWORD,"");
	    request.remove(ARG_EXTRA_PASSWORD);
            if ( !getUserManager().isPasswordValid(request.getUser(), password)) {
		sb.append(HU.center(getPageHandler().showDialogError(msg("Incorrect verification password")+"<br>"+msg("Please enter your password"))));
		return false;
	    }
	}

	if(doCaptcha && !checkRecaptcha(request, sb)) {
	    return false;
	}

	ensureAuthToken(request);
	return true;
    }


    /**
     *  Convert the sessionId into a authorization token that is used to verify form
     *  submissions, etc.
     *
     * @param sessionId _more_
     *
     * @return _more_
     */
    public String getAuthToken(String s, String ...ids) {
	if(hashStringSalt==null) {
	    synchronized(HASH_MUTEX) {
		hashStringSalt = getRepository().getDbProperty("authtoken_salt",(String)null);
		if(hashStringSalt==null) {
		    synchronized(HASH_MUTEX) {
			hashStringSalt = ""+Math.random();
			try {
			    getRepository().writeGlobal("authtoken_salt",hashStringSalt);
			} catch(Exception exc) {
			    throw new RuntimeException(exc);
			}
		    }
		}
	    }
	}
	if(s==null) s="";
	if(ids.length>0) {
	    s = s+"_"+Utils.join("_",ids);
	}
	s = hashStringSalt+"_"+s;
	//	return s;
	return RepositoryUtil.hashString(s);
    }


    public String getVerification(Request request)  {
	return getVerification(request, null, false);
    }


    public String getVerification(Request request, String msg, boolean forcePassword,boolean...addRecaptcha) {
	StringBuilder  sb = new StringBuilder();
	if(msg==null) msg = DEFAULT_MESSAGE;
	msg = msg(msg);
	if(doPassword||forcePassword) {
	    String div =    HU.div(msg+ "<br>" +
		   HU.password(ARG_EXTRA_PASSWORD),
		   HU.clazz("ramadda-verification"));
	    Utils.append(sb,div);
	}
	if(!forcePassword && doCaptcha && Utils.isTrue(addRecaptcha,true))
	    sb.append(getRecaptcha(request));
	addAuthToken(request, sb);
	return sb.toString();
    }


    public void addAuthToken(Request request, Appendable sb,String ...extra) {	
        try {
            String sessionId = request.getSessionId();
            if (sessionId != null) {
                String authToken = getAuthToken(sessionId,extra);
		//		System.err.println("AUTHTOKEN:" + authToken);
                sb.append(HU.hidden(ARG_AUTHTOKEN, authToken));
            } else {
                sb.append(HU.hidden(ARG_AUTHTOKEN, TOKEN_NO_SESSION));
	    }
        } catch (java.io.IOException ioe) {
            throw new RuntimeException(ioe);
        }

    }

    /**
     * _more_
     *
     * @param request _more_
     */
    public void addAuthToken(Request request) {
        String sessionId = request.getSessionId();
        if (sessionId != null) {
            String authToken = getAuthToken(sessionId);
            request.put(ARG_AUTHTOKEN, authToken);
        }
    }
    

    /**
     * _more_
     */
    public void ensureAuthToken(Request request, String...extra) {
	ensureAuthToken(request,false,extra);
    }	

    public void ensureAuthToken(Request request, boolean checkSessionId,String ...extra) {	
        boolean debug        = false;
	//	debug=true;
        String  authToken    = request.getString(ARG_AUTHTOKEN, (String) null);
        String  mySessionId  = request.getSessionId();
        String  argSessionId = request.getString(ARG_SESSIONID, (String) null);
	//	debug=true;
        if (mySessionId == null) {
            mySessionId = argSessionId;
        }

        if (debug) {
            System.err.println("ensureAuthToken: " +
			       " check session id:" + checkSessionId+
			       " authToken:" + Utils.clip(authToken,10,"...") +
			       " arg session:" + Utils.clip(argSessionId,10,"...")+
			       " mySessionId:" + Utils.clip(mySessionId,10,"..."));
        }

	if(mySessionId==null && authToken!=null) {
	    if(authToken.equals(TOKEN_NO_SESSION))  return;
	}
        if (authToken != null && mySessionId != null) {
            String sessionAuth = getAuthToken(mySessionId,extra);
            if (authToken.trim().equals(sessionAuth)) {
                if (debug) {
                    System.err.println("\tauth token is ok");
                }
                return;
            }
            if (debug) {
                System.err.println("\tauth token is no ok");
            }
        }

        if (checkSessionId &&  argSessionId!=null && mySessionId!=null) {
	    if(argSessionId.equals(mySessionId)) {
                if (debug) {
                    System.err.println("\tOK - arg session id == session id");
                }
                return;
            }
            if (debug) {
                System.err.println("\tnot OK arg session id != session id");
            }
        }
	

        //If we are publishing anonymously then don't look for a auth token
        if (request.get(ARG_ANONYMOUS, false) && request.isAnonymous()) {
            return;
        }

        if (debug) {
            System.err.println("Bad auth token");
        }

        getRepository().getLogManager().logError("Request.ensureAuthToken: failed:" + "\n\tsession:"
						 + mySessionId + "\n\targ session:" + argSessionId +"\n\tauth token:" + authToken, null);

        throw new IllegalArgumentException("Bad authentication token:" + authToken);
    }



}
