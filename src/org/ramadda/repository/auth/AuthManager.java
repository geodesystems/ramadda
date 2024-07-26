/**
   Copyright (c) 2008-2023 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.auth;

import org.ramadda.repository.*;

import org.ramadda.util.Utils;
import org.ramadda.util.TTLCache;
import ucar.unidata.util.IOUtil;


import java.io.*;
import java.util.Base64;
import java.util.ArrayList;
import java.util.List;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Random;

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
    public static final String TOKEN_NO_SESSION = "nosession";

    private static final String ARG_EXTRA_PASSWORD = "extrapassword";

    private boolean doCaptcha;
    private boolean doPassword;

    private Captcha defaultCaptcha;
    private List<Captcha> captchas;

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
	doCaptcha = repository.getLocalProperty("ramadda.auth.docaptcha",false);
	defaultCaptcha = new Captcha(this,-1,"","");
	if(doCaptcha && !doPassword) {
	    initCaptchas();
	}
    }
    private static final int IMAGE_WIDTH = 140;
    private static final int IMAGE_HEIGHT =70;
    private static final int TEXTSIZE=24;



    private void drawWord(Graphics2D g, Font font, String word,double jiggleY) {
	int x = 5;
	int y = IMAGE_HEIGHT / 2 + TEXTSIZE / 2;
	char[] chars = word.toCharArray();
	for (int j = 0; j < chars.length; j++) {
	    char ch = chars[j];
	    int _x =  x + font.getSize() * j;
	    //	    int _y =y+ (int) Math.pow(-1, j) * (TEXTSIZE / 6);
	    double dy = (Math.random()-0.5)*(IMAGE_HEIGHT*jiggleY);
	    int _y =y+ (int)dy;
	    String text = String.valueOf(ch);
	    //-0.5 - 0.5
	    double angle = (Math.random()-0.5)*45;
	    g.translate((float)_x,(float)_y);
	    g.rotate(Math.toRadians(angle));
	    g.drawString(text,0,0);
	    g.rotate(-Math.toRadians(angle));
	    g.translate(-(float)_x,-(float)_y);
	}
    }
    
	
    private void initCaptchas() {
	try {
	    captchas  =new ArrayList<Captcha>();
	    String alpha = "abcdefghjkmnopqrstuvwxyz";
	    Random random = new Random();
	    File indexFile = new File(getStorageManager().getResourceDir(),"captcha.txt");
	    captchaMap= new Properties();
	    if(indexFile.exists()) {
		try(FileInputStream fis = new FileInputStream(indexFile)) {
		    captchaMap.load(new InputStreamReader(fis, Charset.forName("UTF-8")));
		}
	    }
	    File captchaDir = new File(IOUtil.joinDir(getStorageManager().getHtdocsDir(),"captchas"));
	    captchaDir.mkdirs();
	    for(int index=0;index<1000;index++) {
		String fileName =(String) captchaMap.get("file"+index);
		String word = (String) captchaMap.get("word"+index);		
		if(fileName==null) {
		    fileName = "captcha" + ((int)(Math.random()*1000000)) +".png";
		    captchaMap.put("file"+index,fileName);
		}
		File imageFile = new File(captchaDir,fileName);
		if(true || !imageFile.exists() || word==null) {
		    //		    System.err.println("new file:" + fileName);
		    word = "";
		    String bgword = "";
		    for(int j=0;j<5;j++ ) {
			word+=alpha.charAt(random.nextInt(alpha.length()));
			bgword+=alpha.charAt(random.nextInt(alpha.length()));
		    }
		    BufferedImage image = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
		    Graphics2D g = image.createGraphics();
		    g.setColor(new Color(240,240,240));
		    g.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
		    g.setColor(Color.BLACK);
		    g.drawRect(0, 0, IMAGE_WIDTH-1, IMAGE_HEIGHT-1);
		    Font font = new Font("Arial", Font.BOLD, TEXTSIZE);
		    g.setFont(font);
		    g.setColor(Color.LIGHT_GRAY);
		    drawWord(g,font,bgword,0.6);
		    g.setColor(Color.BLACK);
		    drawWord(g,font,word,0.5);
		    g.dispose();
		    FileOutputStream fos = new FileOutputStream(imageFile);
		    ImageIO.write(image, "png", fos);
		    captchaMap.put("word"+index,word);
		    /*		ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ImageIO.write(image, "png", baos);
				byte[] imageBytes = baos.toByteArray();
				String base64Image = Base64.getEncoder().encodeToString(imageBytes);
				String dataUrl = "data:image/png;base64," + base64Image;
		    */
		    //		System.out.println(HU.image(dataUrl));
		    //		System.out.println(HU.div(word));
		}
		captchaMap.put("file"+index,fileName);
		captchas.add(new Captcha(this,index,word,fileName));
	    }
	    try(FileOutputStream fos = new FileOutputStream(indexFile)) {
		captchaMap.store(new OutputStreamWriter(fos, Charset.forName("UTF-8")),null);
	    }
	} catch(Exception exc) {
	    System.err.println("error making captchas");
	    exc.printStackTrace();
	}
    }


    public Captcha getCaptcha() {
	if(!doCaptcha) return defaultCaptcha;
	Random random = new Random();
        int randomIndex = random.nextInt(captchas.size());
        return captchas.get(randomIndex);
    }


    /**
       this checks if the user has had too many captcha requests
       if ok, then it checks that the captch text entereed by the user matches
       with the captcha specified by the captch index from the form
       if ok, then it checks that the auth token on the form (which is a hash of
       the session id and the captcha index) matches
       if the auth token does not match then an exception is thrown
    */
    public boolean verify(Request request,Appendable sb) throws Exception {
	return verify(request, sb, false);
    }

    public boolean verify(Request request,Appendable sb, boolean forcePassword) throws Exception {	
	if(doPassword || forcePassword) {
	    String password = request.getString(ARG_EXTRA_PASSWORD,"");
	    request.remove(ARG_EXTRA_PASSWORD);
            if ( !getUserManager().isPasswordValid(request.getUser(), password)) {
		sb.append(getPageHandler().showDialogError(msg("Incorrect verification password")+"<br>"+msg("Please enter your password")));
		return false;
	    }
	    ensureAuthToken(request);
	    return true;
	}


	if(!doCaptcha) {
	    ensureAuthToken(request);
	    return true;
	}

	User user = request.getUser();
	String userName  = user==null?"null":user.getId();
	Integer count = badCaptchaCount.get(userName);
	if (count == null) {
	    count = new Integer(0);
	    badCaptchaCount.put(userName,count);
	}

	if (count.intValue() > MAX_BAD_CAPTCHA_COUNT) {
	    sb.append(getPageHandler().showDialogError("Too many CAPTCHA attempts. You will have to wait for a while or restart the server."));
	    return false;
	}

	Captcha captcha = null;
	boolean ok = true;
	try {
	    int index = request.get(ARG_CAPTCHA_INDEX,-1);
	    String value = request.getString(ARG_CAPTCHA_RESPONSE,"").trim().toLowerCase();
	    if(index<0 || index>=captchas.size()) {
		ok = false;
	    } else  {
		captcha = captchas.get(index);
		ok =  captcha.value.equals(value);
	    }
	} catch(Exception ignore) {
	    ok  =false;
	}

	if(ok) {
	    badCaptchaCount.remove(userName);
	} else {
	    count = Integer.valueOf(count.intValue() + 1);
	    badCaptchaCount.put(userName,count);
	}


	if(!ok && sb!=null) {
	    sb.append(getPageHandler().showDialogError("Bad CAPTCHA response." +
						       (MAX_BAD_CAPTCHA_COUNT-count.intValue() <5?" You only have a few more tries":"")));
	    
	}
	if(!ok) return false;
        captcha.ensureAuthToken(request);
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
	StringBuilder  sb = new StringBuilder();
	addVerification(request, sb);
	return sb.toString();
    }

    private static final String DEFAULT_MESSAGE = "For verification please enter your current password";

    public void addVerification(Request request, Appendable sb)  {
	addVerification(request, sb, null,false);
    }

    public void addVerification(Request request, Appendable sb, String msg)  {
	addVerification(request, sb,msg,false);
    }

    public void addVerification(Request request, Appendable sb, String msg, boolean forcePassword)  {	
	try {
	    sb.append(getCaptcha().getHtml(request,msg,forcePassword));
        } catch (java.io.IOException ioe) {
            throw new RuntimeException(ioe);
        }
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


    public static class Captcha {
	private AuthManager authManager;
	private int index;
	private String value;
	private String image;
	Captcha(AuthManager _authManager,int _index,String _value,String _image) {
	    this.authManager =_authManager;
	    this.index = _index;
	    this.value = _value;
	    this.image = _image;
	}

	public void ensureAuthToken(Request request) {
	    authManager.ensureAuthToken(request,getAuthTokenExtra());
	}

	public void addAuthToken(Request request, Appendable sb) {
	    authManager.addAuthToken(request,sb,getAuthTokenExtra());
	}

	public String getAuthTokenExtra() {
	    return ARG_CAPTCHA_INDEX+"="+index; 
	}
	public String getHtml(Request request, String msg,boolean forcePassword) {
	    if(msg==null) msg = DEFAULT_MESSAGE;
	    msg = msg(msg);
	    StringBuilder sb = new StringBuilder();
	    if(authManager.doPassword||forcePassword) {
		authManager.addAuthToken(request, sb);
		HU.div(sb,
		       msg+ "<br>" +
		       HU.password(ARG_EXTRA_PASSWORD),
		       HU.clazz("ramadda-verification"));
		
	    } else if(authManager.doCaptcha) {
		authManager.addAuthToken(request, sb,getAuthTokenExtra());
		String url = authManager.getPageHandler().makeHtdocsUrl("/captchas/" + this.image);
		sb.append(HU.hidden(ARG_CAPTCHA_INDEX,""+index));
		HU.div(sb,"To verify this action please type in the word<br>"+
		       HU.image(url)+
		       HU.space(2) +
		       HU.input(ARG_CAPTCHA_RESPONSE,"",
				HU.attrs("onkeydown","return Utils.preventSubmit(event)",
					 "placeholder","word","size","5")),HU.clazz("ramadda-verification"));
		
		sb.append("<br>");
	    } else {
		authManager.addAuthToken(request, sb);
	    }
	    return sb.toString();
	}
    }

}
