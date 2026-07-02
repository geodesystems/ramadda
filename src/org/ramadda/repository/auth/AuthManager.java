/**
   Copyright (c) 2008-2026 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.auth;

import org.ramadda.repository.*;

import org.ramadda.util.IO;

import org.ramadda.util.Utils;
import org.ramadda.util.TTLCache;
import ucar.unidata.util.IOUtil;
import org.ramadda.util.JsonUtil;
import ucar.unidata.util.Misc;

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
    public static final String PROP_ISHUMAN_COOKIE_VALUE = "ramadda.ishuman.cookie";
    public static final String PROP_ISHUMAN_CHECK = "ramadda.ishuman.check";    
    public static final String PROP_ISHUMAN_MESSAGE = "ramadda.ishuman.message";
    public static final String ATTR_ISHUMAN = "ishuman";
    public static final String COOKIE_ISHUMAN= "ramadda_ishuman";

    public static final String PROP_TURNSTILE_SITEKEY = "cloudflare.turnstile.sitekey";
    public static final String PROP_TURNSTILE_SECRETKEY = "cloudflare.turnstile.secret";    

    public static final String PROP_RECAPTCHA_SITEKEY = "google.recaptcha.sitekey";
    public static final String PROP_RECAPTCHA_SECRETKEY = "google.recaptcha.secret";    

    public static final String TOKEN_NO_SESSION = "nosession";
    private static final String ARG_EXTRA_PASSWORD = "extrapassword";
    private static final String DEFAULT_MESSAGE = "For verification please enter your password";

    private String humanCookie;
    private boolean checkHuman = false;
    private TTLCache<String, Integer> humanIPs =
	new TTLCache<String,Integer>(Utils.minutesToMillis(30));



    private boolean doCaptcha;
    private boolean doPassword;

    private String recaptchaSiteKey;
    private String recaptchaSecretKey;
    private String turnstileSiteKey;
    private String turnstileSecretKey;    



    /** store the number of bad captcha attempts for each user. clear every hour */
    private TTLCache<String,Integer> badCaptchaCount =
	new TTLCache<String,Integer>(60*60*1000);

    private static int MAX_BAD_CAPTCHA_COUNT = 20;    

    private static String hashStringSalt = null;
    private static Object HASH_MUTEX = new Object();
    private Properties captchaMap;

    public AuthManager(Repository repository) {
        super(repository);
	doPassword = repository.getLocalProperty("ramadda.auth.dopassword",false);	
	doCaptcha = repository.getLocalProperty("ramadda.auth.dorecaptcha",false);
    }



    public void initAttributes() {
        super.initAttributes();
	checkHuman  = getRepository().getProperty(PROP_ISHUMAN_CHECK,false);

	recaptchaSiteKey =  getRepository().getProperty(PROP_RECAPTCHA_SITEKEY,null);
	recaptchaSecretKey = getRepository().getProperty(PROP_RECAPTCHA_SECRETKEY,null);		

	turnstileSiteKey =  getRepository().getProperty(PROP_TURNSTILE_SITEKEY,null);
	turnstileSecretKey = getRepository().getProperty(PROP_TURNSTILE_SECRETKEY,null);		

    }



    public boolean  getCheckIfHuman() {
	return checkHuman;
    }


    public String getIsHumanCookieValue() throws Exception {
	if(humanCookie==null) {
	    humanCookie = getRepository().getProperty(PROP_ISHUMAN_COOKIE_VALUE,null);
	    if(humanCookie==null) {
		synchronized(this) {
		    if(humanCookie==null) {
			humanCookie = getRepository().getGUID();
			getRepository().writeGlobal(PROP_ISHUMAN_COOKIE_VALUE,humanCookie);
			getLogManager().logInfoAndPrint("Human check:","created cookie value:"  + humanCookie);
		    }
		}
	    }
	}
	return humanCookie;
    }

    public boolean isRecaptchaEnabled() {
	if(stringDefined(recaptchaSiteKey) && stringDefined(recaptchaSecretKey)) {
	    return true;
	}
	return false;
    }

    public String getRecaptcha(Request request,String ...extra) {
	StringBuilder sb = new StringBuilder();
	if(isRecaptchaEnabled()) {
	    sb.append("<script src='https://www.google.com/recaptcha/api.js' async defer></script>");
	    HU.div(sb,"",HU.attrs("class","g-recaptcha ramadda-recaptcha","data-sitekey",recaptchaSiteKey)+
		   HU.attrs(extra));
	}
	return sb.toString();
    }

    public boolean checkRecaptcha(Request request, Appendable response)
	throws Exception {
	if(isRecaptchaEnabled()) {
	    String recaptchaResponse = request.getString("g-recaptcha-response",null);
	    if(recaptchaResponse==null) return false;
	    String url = HU.url("https://www.google.com/recaptcha/api/siteverify",
				"secret",recaptchaSecretKey,
				"response",recaptchaResponse);
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

    public boolean isTurnstileEnabled() {
	if(stringDefined(turnstileSiteKey) && stringDefined(turnstileSecretKey)) {
	    return true;
	}
	return false;
    }

    public String getTurnstile(Request request,String ...extra) {
	StringBuilder sb = new StringBuilder();
	if(isTurnstileEnabled()) {
	    sb.append("<script   src='https://challenges.cloudflare.com/turnstile/v0/api.js'    async defer></script>\n");
	    HU.div(sb,"",HU.attrs("class","cf-turnstile",
				  "data-sitekey",turnstileSiteKey)+
		   HU.attrs(extra));
	}
	return sb.toString();
    }

    public boolean checkTurnstile(Request request, Appendable response)
	throws Exception {
	if(isTurnstileEnabled()) {
	    String turnstileResponse = request.getString("cf-turnstile-response",null);
	    if(turnstileResponse==null) return false;
	    String url = "https://challenges.cloudflare.com/turnstile/v0/siteverify";
	    String body = JU.mapAndQuote(Utils.toList("secret",turnstileSecretKey,
						      "response",turnstileResponse));
	    String json = IO.doPost(new URL(url),body,"Content-Type","application/json");
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
            if ( !getUserManager().isPasswordValid(request,request.getUser(), password)) {
		sb.append(HU.center(getPageHandler().showDialogError(msg("Incorrect verification password")+"<br>"+msg("Please enter your password"))));
		return false;
	    }
	    if(forcePassword) return true;
	}

	if(doCaptcha && !checkRecaptcha(request, sb)) {
	    sb.append(HU.center(getPageHandler().showDialogError(msg("The recaptcha failed"))));
	    return false;
	}

	ensureAuthToken(request);
	return true;
    }

    public Result checkForHuman(Request request) throws Exception  {
	if(request.get("overidehuman",false)) {
	    return null;
	}


	if(!checkHuman) {
	    //	    logSpecial("human: not enabled");
	    return null;

	}
	if(!request.isAnonymous()) {
	    //	    logSpecial("human: not anon");
	    return null;
	}
	    
	List<String> cookies = getSessionManager().getCookies(request,COOKIE_ISHUMAN);
	if(cookies.size()!=0) {
	    if(cookies.contains(getIsHumanCookieValue()))  {
		//		logSpecial("human: has cookie");
		return null;
	    }
	}

	//Special exception for google bot
	if(getRepository().acceptGoogleBot() && getRepository().isGoogleBot(request)) {
	    //	    logSpecial("human: is google bot");
	    return null;
	}
	StringBuilder sb = new StringBuilder();

	StringBuilder messageSB = new StringBuilder();
	boolean formSubmitted = request.get("humanform",false);
	boolean isHuman = false;
	if(formSubmitted) {
	    if(isTurnstileEnabled()) {
		isHuman  = checkTurnstile(request, messageSB);
	    } else     if(getAuthManager().isRecaptchaEnabled()) {
		isHuman  = getAuthManager().checkRecaptcha(request, messageSB);
	    } else {
		String isHumanResponse = request.getString(ATTR_ISHUMAN,null);
		isHuman = isHumanResponse!=null && isHumanResponse.equals("yes");
	    }
	    if(isHuman) {
		getLogManager().logInfoAndPrint("Human check:", "verified: " + request.getOriginalIp() +" user:" + request.getUserAgent());
		request.addCookie(COOKIE_ISHUMAN, getRepository().makeCookie(request, "/",getIsHumanCookieValue(),false,false));
		return null;
	    }
	}

	Integer count = null;
	synchronized(humanIPs) {
	    String ip = request.getOriginalIp();
	    count = humanIPs.get(ip);
	    if(count==null) {
		count = new Integer(0);
	    }
	    count = new Integer(count.intValue()+1);
	    humanIPs.put(ip,count);
	}
	boolean barebones = true;
	if(request.isMobile()) {
	    barebones=false;
	}

	if(barebones) {
	    sb.append("<!DOCTYPE html><html><body>");
	    HU.cssLink(sb, getPageHandler().getCdnPath("/style.css"));
	    String logo= getPageHandler().getLogoImage(null);
	    getPageHandler().sectionOpen(request,sb,getRepository().getRepositoryName(),false);
	    if(Utils.stringDefined(logo)) sb.append(HU.center(HU.img(logo,"",HU.attrs("width","120px"))));
	} else {
	    getPageHandler().sectionOpen(request,sb,"Please prove you are a human",false);
	}
	//	sb.append(messageSB);
	String message = getRepository().getProperty(PROP_ISHUMAN_MESSAGE,"");
	if(Utils.stringDefined(message)) {
	    message = message.replace("\\n","<br>");
	    if(barebones) {
		sb.append(HU.div(message,HU.attrs("class","human-message")));
	    } else {
		sb.append(getPageHandler().showDialogNote(message));
	    }
	}

	if(formSubmitted) {
	    getLogManager().logInfoAndPrint("Human check:", "failed: " + request.getOriginalIp());
	    sb.append(getPageHandler().showDialogWarning("Sorry, we could not verify that you are a human"));
	}

	String formID = "checkhumanform";
	HU.script(sb,"function onHumanCheckSuccess(token) {document.getElementById(" + HU.squote(formID)+").submit();}");
	sb.append(HU.formPost(request.getRequestPath(),
			      HU.attrs("id",formID)));
	sb.append(HU.hidden("humanform","true"));

	if(isTurnstileEnabled()) {
	    sb.append(getTurnstile(request,"data-callback","onHumanCheckSuccess"));
	} else if(isRecaptchaEnabled()) {
	    sb.append("<div class=ramadda-verification>");
	    sb.append("</div>");
	    sb.append(getAuthManager().getRecaptcha(request,
						    "data-callback","onHumanCheckSuccess"));
	} else {
	    sb.append(HU.submitClass("Yes, I am a human","submit","button-submit"));
	    sb.append(HU.hidden(ATTR_ISHUMAN,"",HU.attrs("id",ATTR_ISHUMAN)));
	    HU.importJS(sb, getPageHandler().getCdnPath("/human.js"));
	}
	request.addFormHiddenArguments(sb,Utils.makeHashSet(ATTR_ISHUMAN));
	sb.append(HU.formClose());
	sb.append("\n");

	String message2 = "This site uses a necessary security cookie to remember that your browser has passed human verification. The cookie is required to protect the site from automated abuse.";

	sb.append(HU.div(message2,HU.attrs("class","human-message")));

	


	getPageHandler().sectionClose(request,sb);
	if(barebones)
	    sb.append("</body></html>");
	String logMessage = "checking:" + " IP:" + request.getOriginalIp() +" count: " +count;
	String entryId = request.getString(ARG_ENTRYID,null);
	if(entryId!=null) logMessage+=" entry:" + entryId;
	getLogManager().logInfoAndPrint("Human check:",logMessage);
	Result result =  new Result("Prove you are a human",sb);
	result.setResponseCode(Result.RESPONSE_UNAUTHORIZED);
	if(count>5) {
	    Misc.sleepSeconds(5);
	}
	if(barebones) 
	    result.setShouldDecorate(false);
	return result;
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

    public String getVerificationWithPassword(Request request, String msg) {
	return getVerification(request,msg,true);
    }

    private String getVerification(Request request, String msg, boolean forcePassword) {
	StringBuilder  sb = new StringBuilder("<div class=ramadda-verification>");
	if(msg==null) msg = DEFAULT_MESSAGE;
	msg = msg(msg);
	if(doPassword||forcePassword) {
	    String passwordInput =
		HU.password(ARG_EXTRA_PASSWORD,"",HU.attrs("placeholder","Your password"));
	    HU.div(sb,msg+ "<br>" +  passwordInput,
		   HU.clazz("ramadda-verification-password"));
	} else 	if(doCaptcha) {
	    sb.append(getRecaptcha(request));
	}
	sb.append("</div>");
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
