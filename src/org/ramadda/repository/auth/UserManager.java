/**
   Copyright (c) 2008-2025 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.auth;


import org.ramadda.repository.*;
import org.ramadda.repository.database.*;
import org.ramadda.repository.search.SearchManager;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.FormInfo;

import org.ramadda.util.HtmlTemplate;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.Utils;
import org.ramadda.util.TTLCache;
import org.ramadda.util.ImageUtils;

import org.ramadda.util.sql.Clause;
import org.ramadda.util.sql.SqlUtil;


import org.json.*;
import java.net.URL;
import javax.mail.*;
import javax.mail.internet.*;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;

import java.awt.Color;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.*;

import java.io.*;
import java.util.Base64;
import java.util.Comparator;


import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


import java.security.SignatureException;

import java.sql.ResultSet;
import java.sql.Statement;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;




/**
 * Handles user stuff
 *
 * @author Jeff McWhirter
 */
@SuppressWarnings("unchecked")
public class UserManager extends RepositoryManager {


    public static final String DEFAULT_INSTITUTION = "";
    public static final String DEFAULT_COUNTRY = "";
    public static final String DEFAULT_QUESTION = "";
    public static final String DEFAULT_ANSWER = "";        

    public static final String LABEL_LOGIN  ="Login";    
    public static final String LABEL_NEW_USER  ="New User";

    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final String ARG_HUMAN_QUESTION = "human_question";
    public static final String ARG_HUMAN_ANSWER = "human_answer";
    public static final String ARG_USERAGREE = "agree";
    private static List<String> QUESTIONS;
    private static List<Integer> ANSWERS;
    public static final String ARG_REGISTER_PASSPHRASE = "passphrase";
    public static final String ARG_USER_APPLY = "user_apply";
    public static final String ARG_USER_ACTION = "user_action";
    public static final String ARG_SEND = "send";


    public static final String ACTION_DELETE = "delete";
    public static final String ACTION_CSV = "csv";
    public static final String ACTION_EMAIL = "email";


    public static final String ARG_USER_STATUS = "user_status";
    public static final String ARG_USER_ADMIN = "user_admin";
    public static final String ARG_USER_ISGUEST = "user_isguest";
    public static final String ARG_USER_ANSWER = "user_answer";
    public static final String ARG_USER_AVATAR = "user_avatar";    
    public static final String ARG_USER_AVATAR_DELETE = "user_avatar_delete";    

    public static final String ARG_USER_BULK = "user_bulk";
    public static final String ARG_USER_CANCEL = "user_cancel";
    public static final String ARG_USER_CHANGE = "user_change";
    public static final String ARG_USER_DELETE = "user_delete";
    public static final String ARG_USER_DOWNLOAD = "user_download";    
    public static final String ARG_USER_DELETE_CONFIRM =    "user_delete_confirm";
    public static final String ARG_USER_EMAIL = "user_email";
    public static final String ARG_USER_INSTITUTION = "user_institution";
    public static final String ARG_USER_COUNTRY = "user_country";        
    public static final String ARG_USER_LANGUAGE = "user_language";
    public static final String ARG_USER_NAME = "user_name";
    public static final String ARG_USER_DESCRIPTION = "user_description";
    public static final String ARG_USER_NEW = "user_new";
    public static final String ARG_USER_IMPORT = "userimport";
    public static final String ARG_USER_EXPORT = "userexport";
    public static final OutputType OUTPUT_FAVORITE =
        new OutputType("Add as Favorite", "user.addfavorite",
                       OutputType.TYPE_OTHER, "", ICON_FAVORITE);



    public static final String PROP_REGISTER_OK = "ramadda.register.ok";
    public static final String PROP_RECAPTCHA_SITEKEY = "google.recaptcha.sitekey";
    public static final String PROP_RECAPTCHA_SECRETKEY = "google.recaptcha.secret";    
    public static final String PROP_REGISTER_PASSPHRASE = "ramadda.register.passphrase";
    public static final String PROP_REGISTER_EMAIL = "ramadda.register.email";
    public static final String PROP_LOGIN_ALLOWEDIPS =  "ramadda.login.allowedips";
    public static final String PROP_PASSWORD_DIGEST =   "ramadda.password.hash.digest";
    public static final String PROP_PASSWORD_ITERATIONS =    "ramadda.password.hash.iterations";

    /** Note: we don't actively use the SALT properties anymore but we keep them around for backwards compatibilty */
    public static final String PROP_PASSWORD_SALT =
        "ramadda.password.hash.salt";

    public static final String PROP_PASSWORD_SALT1 =     "ramadda.password.hash.salt1";
    public static final String PROP_PASSWORD_SALT2 =    "ramadda.password.hash.salt2";
    public static final String PROP_USER_AGREE = "ramadda.user.agree";

    public static final String ACTIVITY_LOGIN = "login";
    public static final String ACTIVITY_LOGOUT = "logout";
    public static final String ACTIVITY_PASSWORD_CHANGE = "password.change";

    private static final String USER_DEFAULT = "default";
    public static final String USER_ANONYMOUS = "anonymous";
    public static final String USER_LOCALFILE = "localuser";

    public final RequestUrl URL_USER_NEW_FORM = new RequestUrl(this,
							       "/user/new/form");

    public final RequestUrl URL_USER_NEW_DO = new RequestUrl(this,
							     "/user/new/do");

    public final RequestUrl URL_USER_SELECT_DO = new RequestUrl(this,
								"/user/select/do");


    /** urls to use when the user is logged in */
    protected List<RequestUrl> userUrls =
        RequestUrl.toList(new RequestUrl[] {
		getRepositoryBase().URL_USER_SETTINGS,
		getRepositoryBase().URL_USER_PASSWORD,		
		getRepositoryBase().URL_USER_HOME});

    protected List<RequestUrl> remoteUserUrls =
        RequestUrl.toList(new RequestUrl[] {
		getRepositoryBase().URL_USER_HOME});


    /** urls to use with no user */
    protected List<RequestUrl> anonUserUrls =
        RequestUrl.toList(new RequestUrl[] {});


    /** List of ip addresses (or prefixes) that control where users can login from */
    private List<String> allowedIpsForLogin;

    private Hashtable<String, User> userMap = new Hashtable<String, User>();

    /** any external user authenticators from plugins */
    private List<UserAuthenticator> userAuthenticators =
        new ArrayList<UserAuthenticator>();

    /** holds password reset information */
    private Hashtable<String, PasswordReset> passwordResets =
        new Hashtable<String, PasswordReset>();


    private boolean debug = false;
    private String salt;
    private String salt1;
    private String salt2;
    private String userPreface;
    private String userAgree;    

    /** store the number of bad login attempts for each user */
    private static Hashtable<String, Integer> badPasswordCount =
        new Hashtable<String, Integer>();

    /** how many login tries before we blow up */
    private static int MAX_BAD_PASSWORD_COUNT = 50;

    public UserManager(Repository repository) {
        super(repository);
    }


    public void debugLogin(String msg) {
        if (debug) {
            //System.err.println(getRepository().debugPrefix() + ":" + msg);
            System.err.println(msg);
        }
    }

    /**
     * add the user authenticator
     *
     * @param userAuthenticator user authenticator
     */
    public void addUserAuthenticator(UserAuthenticator userAuthenticator) {
        userAuthenticators.add(userAuthenticator);
        if (userAuthenticator instanceof UserAuthenticatorImpl) {
            ((UserAuthenticatorImpl) userAuthenticator).setRepository(
								      getRepository());
        }
    }



    /**
     * Is login allowed for the given request. This checks the allowed ip addresses
     *
     * @param request the request
     *
     * @return can do login
     */
    public boolean canDoLogin(Request request) {
        if (getRepository().isReadOnly()) {
            return false;
        }

        if (allowedIpsForLogin.size() > 0) {
            String requestIp = request.getIp();
            if (requestIp == null) {
                return false;
            }
            for (String ip : allowedIpsForLogin) {
                if (requestIp.startsWith(ip)) {
                    return true;
                }
            }

            //If there were any ips and none matched then return false
            return false;
        }

        return true;
    }


    private Result addHeader(Request request, Result result) {
        try {
            return addHeaderToAncillaryPage(request, result);
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    private Result addHeader(Request request, Appendable sb, String title)
	throws Exception {
        Appendable html = new StringBuilder();
        HU.titleSectionOpen(html, title);
        html.append(sb.toString());
        html.append(HU.sectionClose());
        Result result = new Result(title, html);

        return addHeader(request, result);
    }

    public Result makeResult(Request request, String title, Appendable sb)
	throws Exception {
        StringBuilder headerSB = new StringBuilder();
        addUserHeader(request, headerSB);
        headerSB.append(sb);
        getPageHandler().sectionClose(request, headerSB);

        return addHeader(request, new Result(title, headerSB));
    }

    public void addUserHeader(Request request, Appendable sb)
	throws Exception {
        User             user    = request.getUser();
        boolean          useAnon = user.getAnonymous() || user.getIsGuest();
        List<RequestUrl> links   = userUrls;
        if (user.getAnonymous() || user.getIsGuest()) {
            links = anonUserUrls;
        } else if ( !user.getIsLocal()) {
            links = remoteUserUrls;
        }
        getPageHandler().sectionOpen(request, sb, msgLabel("User")+" " + getUserTitle(user), false);
        getPageHandler().makeLinksHeader(request, sb, links, "");
    }

    private String getUserTitle(User user) {
	String title=user.getId();
	if(stringDefined(user.getName())) title +=" - " + user.getName();
	return title;
    }

    /**
     * initial the list of users from the command line
     *
     *
     * @param cmdLineUsers users to initialize
     * @throws Exception On badness
     */
    public void initUsers(List<User> cmdLineUsers) throws Exception {
        debug = getRepository().getProperty("ramadda.debug.login", false);
        salt = getRepository().getProperty(PROP_PASSWORD_SALT, "");
        salt1 = getRepository().getProperty(PROP_PASSWORD_SALT1, "");
        salt2 = getRepository().getProperty(PROP_PASSWORD_SALT2, "");
        allowedIpsForLogin =
            Utils.split(getRepository().getProperty(PROP_LOGIN_ALLOWEDIPS,
						    ""), ",", true, true);

        userPreface = getRepository().getProperty("ramadda.user.preface",
						(String) null);

        userAgree = getRepository().getProperty(PROP_USER_AGREE,
						(String) null);


        makeUserIfNeeded(new User(USER_DEFAULT, "Default User"));
        makeUserIfNeeded(new User(USER_ANONYMOUS, "Anonymous"));
        makeUserIfNeeded(new User(USER_LOCALFILE, "Local Files"));

        for (User user : cmdLineUsers) {
            //If it was from the cmd line then the password is not hashed
            user.setPassword(hashPassword(user.getPassword()));
            makeOrUpdateUser(user, true);
        }


        //If we have an admin property then it is of the form userid:password           
        //and is used to set the password of the admin                                  
        //Use localProperties so plugins can't slide in an admin password
        String adminFromProperties =
            getRepository().getLocalProperty(PROP_ADMIN, null);


	//Check if we should generate an admin password
        if (adminFromProperties == null && getRepository().getProperty("ramadda.admin.setpassword",false)) {
	    adminFromProperties = "admin:" + Utils.generatePassword(8);
	    File file = new File(IOUtil.joinDir(getStorageManager().getRepositoryDir(),
						"admin.properties"));

	    try (FileOutputStream fos = new FileOutputStream(file)) {
		IOUtil.write(fos, "#This is the generated password for the admin account\n#You can either: \n#change the password here and it will be set to this every time RAMADDA starts up \n#or:\n#login with this password, change your password through RAMADDA then delete this file\n" +
			     "ramadda.admin=" + adminFromProperties+"\n");
		logInfo("RAMADDA: the admin user password has been generated and written to:" + file);
	    }
	}
								       

        if (adminFromProperties != null) {
            List<String> toks = Utils.split(adminFromProperties, ":");
            if (toks.size() != 2) {
                getLogManager().logError("Error: The " + PROP_ADMIN
                                         + " property is incorrect");

                return;
            }
            User   user        = new User(toks.get(0).trim(), "", true);
            String rawPassword = toks.get(1).trim();
            if (rawPassword.equals("random")) {
                rawPassword = getRepository().getGUID();
                System.err.println("New admin password:" + rawPassword);
            }
            user.setPassword(hashPassword(rawPassword));
            if ( !userExistsInDatabase(user)) {
                logInfo("RAMADDA: Creating new admin user:" + user);
                makeOrUpdateUser(user, true);
            } else {
                //                System.err.println("Updating password for admin user:" + user);
                changePassword(user);
                //And set the admin flag to true
                getDatabaseManager().update(
					    Tables.USERS.NAME, Tables.USERS.COL_ID, user.getId(),
					    new String[] { Tables.USERS.COL_ADMIN },
					    new Object[] { Boolean.valueOf(true) });
            }
            logInfo("RAMADDA: password for:" + user.getId()
                    + " has been updated");
        }

        for (UserAuthenticator userAuthenticator : userAuthenticators) {
            userAuthenticator.initUsers();
        }
    }

    private String getPasswordToUse(String password) throws Exception {
        password = password.trim();
        //If we have a salt then use a generated hmac as the password to hash
        if (salt.length() != 0) {
            debugLogin("Has SALT:" + salt);

            return calculateRFC2104HMAC(password, salt);
        }

        return password;
    }

    //From: http://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/AuthJavaSampleHMACSignature.html


    public static String calculateRFC2104HMAC(String data, String key)
	throws java.security.SignatureException {
        try {
            String HMAC_SHA1_ALGORITHM = "HmacSHA1";
            // get an hmac_sha1 key from the raw key bytes
            SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(),
							 HMAC_SHA1_ALGORITHM);


            // get an hmac_sha1 Mac instance and initialize with the signing key
            Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
            mac.init(signingKey);

            // compute the hmac on input data bytes
            byte[] rawHmac = mac.doFinal(data.getBytes());

            // base64-encode the hmac
            return Utils.encodeBase64Bytes(rawHmac);
        } catch (Exception e) {
            throw new SignatureException("Failed to generate HMAC : "
                                         + e.getMessage());
        }
    }



    /**
     * hash the given raw text password for storage into the database
     *
     * @param password raw text password
     *
     * @return hashed password
     */
    public String hashPassword(String password) {
        try {
            return PasswordHash.createHash(getPasswordToUse(password));
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    public List<FavoriteEntry> getFavorites(Request request)
	throws Exception {
	return getFavorites(request, request.getUser());
    }

    public List<FavoriteEntry> getFavorites(Request request, User user)
	throws Exception {
        if (user ==null ||user.getAnonymous()) {
            return new ArrayList<FavoriteEntry>();
        }
        List<FavoriteEntry> favorites = user.getFavorites();
        if (favorites == null) {
            favorites = new ArrayList<FavoriteEntry>();
            Statement statement = getDatabaseManager().select(
							      Tables.FAVORITES.COLUMNS,
							      Tables.FAVORITES.NAME,
							      Clause.eq(Tables.FAVORITES.COL_USER_ID,
									user.getId()));
            SqlUtil.Iterator iter =
                getDatabaseManager().getIterator(statement);
            ResultSet results;
            //COL_ID,COL_USER_ID,COL_ENTRY_ID,COL_NAME,COL_CATEGORY
            while ((results = iter.getNext()) != null) {
                int    col    = 1;
                String id     = results.getString(col++);
                String userId = results.getString(col++);
                Entry entry = getEntryManager().getEntry(request,
							 results.getString(col++));
                String name     = results.getString(col++);
                String category = results.getString(col++);
                if (entry == null) {
                    getDatabaseManager().delete(
						Tables.FAVORITES.NAME,
						Clause.and(			   Clause.eq(
								     Tables.FAVORITES.COL_USER_ID,
								     user.getId()), Clause.eq(
											      Tables.FAVORITES.COL_ID, id)));

                    continue;
                }
                favorites.add(new FavoriteEntry(id, entry, name, category));
            }
            user.setUserFavorites(favorites);
        }

        return favorites;
    }

    public User getCurrentUser(User user) {
        if (user == null) {
            return null;
        }
        User currentUser = userMap.get(user.getId());
        if (currentUser != null) {
            return currentUser;
        }
        return user;
    }

    public boolean isRequestOk(Request request) {
        User user = request.getUser();
        if (getRepository().getAdminOnly() && !user.getAdmin()) {
            getRepository().debugSession(request, "isRequestOK: Admin only");
            if ( !request.getRequestPath().startsWith(
						      getRepository().getUrlBase() + "/user/")) {
                return false;
            }
        }

        if (getRepository().getRequireLogin() && user.getAnonymous()) {
            if ( !request.getRequestPath().startsWith(
						      getRepository().getUrlBase() + "/user/")) {
                getRepository().debugSession(
					     request, "isRequestOk: login is required ");

                return false;
            }
        }

        return true;
    }

    public String makeLoginForm(Request request) {
        return makeLoginForm(request, "");
    }


    public String makeLoginForm(Request request, String extra) {
	return makeLoginForm(request, extra, true);
    }

    public String makeLoginForm(Request request, String extra,boolean includeForget) {	
        StringBuilder sb = new StringBuilder();
	sb.append("<center>");
        request.appendMessage(sb);
	makeLoginForm(sb,request, extra,includeForget,"");
	sb.append("</center>");
	return sb.toString();
    }

    public void  makeLoginForm(StringBuilder sb, Request request, String extra,boolean includeForget,String user) {
        if ( !canDoLogin(request)) {
            sb.append(messageWarning("Login is not allowed"));
            return;
        }

        String id = request.getString(ARG_USER_ID, user);
        sb.append(HU.formPost(getRepository().getUrlPath(request,
							 getRepositoryBase().URL_USER_LOGIN)));

        if (request.defined(ARG_REDIRECT)) {
            String redirect = request.getBase64String(ARG_REDIRECT, "");
            //Um, a bit of a hack
            if (redirect.indexOf("logout") < 0) {
                sb.append(HU.hidden(ARG_REDIRECT, Utils.encodeBase64(redirect)));
            }
        }

        if (stringDefined(userPreface)) {
            sb.append(messageNote(userPreface));
        }

        sb.append(HU.formTable());
        sb.append(
		  formEntry(
			    request, msgLabel("User"),
			    HU.input(
				     ARG_USER_ID, id,
				     HU.cssClass(CSS_CLASS_USER_FIELD)
				     + " autofocus=autofocus")));
        sb.append(formEntry(request, msgLabel("Password"),
                            HU.password(ARG_USER_PASSWORD)));
        if (userAgree != null) {
            sb.append(formEntry(request, "",
                                HU.labeledCheckbox(ARG_USERAGREE, "true",
						   request.get(ARG_USERAGREE,
							       false),userAgree)));
        }
        sb.append(extra);

        sb.append(formEntry(request, "", HU.submit(LABEL_LOGIN)));
        sb.append(HU.formClose());

        if (includeForget && getMailManager().isEmailEnabled()) {
            sb.append(HU.formEntry("<p>", ""));
	    HU.formEntry(sb,  "",
			 HU.button(HU.href( request.makeUrl(
							    getRepositoryBase().URL_USER_FINDUSERID), msg(
													  "Forget your user ID?"))));
	    sb.append(HU.space(2));
	    HU.formEntry(sb, "",
			 HU.button(HU.href(request.makeUrl(getRepositoryBase().URL_USER_RESETPASSWORD),
					   msg("Forget your password?"))));
	}
        sb.append(HU.formTableClose());
    }

    public User getDefaultUser() throws Exception {
        return findUser(USER_DEFAULT);
    }

    public User getAdminUser() throws Exception {
        User user = new User("admin", true);
        return user;
    }

    public User getAnonymousUser() throws Exception {
        return findUser(USER_ANONYMOUS);
    }

    public User getLocalFileUser() throws Exception {
        return findUser(USER_LOCALFILE);
    }

    public User findUser(String id) throws Exception {
        return findUser(id, false);
    }

    public User findUser(String id, boolean userDefaultIfNotFound)
	throws Exception {
        //        debugLogin("RAMADDA.findUser: " + id);
        if (id == null) {
            return null;
        }
        User user = userMap.get(id);
        if (user != null) {
            //System.err.println ("got from user map:" + id +" " + user);
            return user;
        }


        Statement statement =
            getDatabaseManager().select(Tables.USERS.COLUMNS,
                                        Tables.USERS.NAME,
                                        Clause.eq(Tables.USERS.COL_ID, id));
        ResultSet results = statement.getResultSet();
        if (results.next()) {
            user = getUser(results);
            debugLogin("RAMADDA.findUser: from database:" + user);
        } else {
            for (UserAuthenticator userAuthenticator : userAuthenticators) {
                debugLogin("RAMADDA.findUser: calling authenticator:"
                           + userAuthenticator);
                user = userAuthenticator.findUser(getRepository(), id);
                debugLogin("RAMADDA.findUser: from authenticator:" + user);
                if (user != null) {
                    user.setIsLocal(false);

                    break;
                }
            }
        }

        getDatabaseManager().closeAndReleaseConnection(statement);

        if (user == null) {
            if (userDefaultIfNotFound) {
                return getDefaultUser();
            }
            return null;
        }
	updateUser(user);
        return user;
    }


    private void updateUser(User user) {
	if(user!=null) userMap.put(user.getId(), user);
    }


    /**
     * _more_
     *
     * @param email _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public User findUserFromEmail(String email) throws Exception {
        Statement statement =
            getDatabaseManager().select(Tables.USERS.COLUMNS,
                                        Tables.USERS.NAME,
                                        Clause.eq(Tables.USERS.COL_EMAIL, email));
        ResultSet results = statement.getResultSet();
        if ( !results.next()) {
            return null;
        }

        return getUser(results);
    }




    /**
     * _more_
     *
     * @param user _more_
     *
     * @return _more_
     *
     * @throws Exception on badness
     */
    public boolean userExistsInDatabase(User user) throws Exception {
        return getDatabaseManager().tableContains(Tables.USERS.NAME, Tables.USERS.COL_ID,
						  user.getId());						  
    }





    /**
     * _more_
     *
     * @param user The user
     *
     *
     * @return _more_
     * @throws Exception On badness
     */
    public User makeUserIfNeeded(User user) throws Exception {
        if ( !userExistsInDatabase(user)) {
            makeOrUpdateUser(user, false);
        }

        return user;
    }


    /**
     * _more_
     *
     * @param user _more_
     *
     * @throws Exception on badness
     */
    public void changePassword(User user) throws Exception {
        getDatabaseManager().update(
				    Tables.USERS.NAME, Tables.USERS.COL_ID, user.getId(),
				    new String[] { Tables.USERS.COL_PASSWORD },
				    new Object[] { user.getHashedPassword() });
    }


    /**
     * _more_
     *
     * @param user The user
     * @param updateIfNeeded _more_
     *
     * @throws Exception On badness
     */
    public void makeOrUpdateUser(User user, boolean updateIfNeeded)
	throws Exception {
        if ( !userExistsInDatabase(user)) {
	    //Note: these have to be lined up with the database/Tables.USERS class defs
	    //COL_ID,COL_NAME,COL_EMAIL,COL_INSTITUTION,COL_COUNTRY,COL_QUESTION,COL_ANSWER,COL_PASSWORD,COL_DESCRIPTION,COL_ADMIN,COL_LANGUAGE,COL_TEMPLATE,COL_ISGUEST,COL_ACCOUNT_CREATION_DATE,COL_PROPERTIES
            getDatabaseManager().executeInsert(Tables.USERS.INSERT,
					       new Object[] {
						   user.getId(),
						   user.getStatus(),
						   user.getName(),
						   user.getEmail(),
						   user.getInstitution(),
						   user.getCountry(),
						   user.getQuestion(),
						   user.getAnswer(),
						   user.getHashedPassword(),
						   user.getDescription(),
						   Boolean.valueOf(user.getAdmin()),
						   user.getLanguage(),
						   user.getTemplate(),
						   Boolean.valueOf(user.getIsGuest()),
						   user.getAccountCreationDate(),
						   user.getPropertiesBlob()
					       });
	    updateUser(user);
            return;
        }

        if ( !updateIfNeeded) {
            throw new IllegalArgumentException(
					       "Database already contains user:" + user.getId());
        }

        getDatabaseManager().update(Tables.USERS.NAME, Tables.USERS.COL_ID,
                                    user.getId(), new String[] {
					Tables.USERS.COL_STATUS,
					Tables.USERS.COL_NAME,
					Tables.USERS.COL_PASSWORD,
					Tables.USERS.COL_DESCRIPTION,
					Tables.USERS.COL_EMAIL,
					Tables.USERS.COL_INSTITUTION,
					Tables.USERS.COL_COUNTRY,
					Tables.USERS.COL_QUESTION,
					Tables.USERS.COL_ANSWER,
					Tables.USERS.COL_ADMIN,
					Tables.USERS.COL_LANGUAGE,
					Tables.USERS.COL_TEMPLATE,
					Tables.USERS.COL_ISGUEST,
					Tables.USERS.COL_ACCOUNT_CREATION_DATE,
					Tables.USERS.COL_PROPERTIES
				    }, new Object[] {
					user.getStatus(),
					user.getName(), 
					user.getHashedPassword(),
					user.getDescription(),
					user.getEmail(),
					user.getInstitution(),
					user.getCountry(),
					user.getQuestion(),
					user.getAnswer(),
					user.getAdmin()
					? Integer.valueOf(1)
					: Integer.valueOf(0),
					user.getLanguage(),
					user.getTemplate(),
					Boolean.valueOf(user.getIsGuest()),
					user.getAccountCreationDate(),
					user.getPropertiesBlob()
				    });
        userMap.remove(user.getId());


    }




    /**
     * _more_
     *
     * @param user The user
     *
     * @throws Exception On badness
     */
    public void deleteUser(User user) throws Exception {
	getSessionManager().removeUserSession(getRepository().getAdminRequest(), user);
        userMap.remove(user.getId());
        deleteRoles(user);
        getDatabaseManager().delete(Tables.USERS.NAME,
                                    Clause.eq(Tables.USERS.COL_ID,
					      user.getId()));

    }

    /**
     * _more_
     *
     * @param user The user
     *
     * @throws Exception On badness
     */
    public void deleteRoles(User user) throws Exception {
        getDatabaseManager().delete(Tables.USERROLES.NAME,
                                    Clause.eq(Tables.USERROLES.COL_USER_ID,
					      user.getId()));
    }


    /**
     * This checks the PASSWORD1 and PASSWORD2 URL arguments for equality.
     * If they are defined and are equal then the hashed password is set for the user
     * and this returns true.
     *
     * If the passwords are not equal then false
     *
     * @param request the request
     * @param user The user
     *
     * @return Are the passwords equal and did the user's password get set
     *
     * @throws Exception _more_
     */
    private boolean checkAndSetNewPassword(Request request, User user)
	throws Exception {
        String password1 = request.getString(ARG_USER_PASSWORD1, "").trim();
        String password2 = request.getString(ARG_USER_PASSWORD2, "").trim();
        if (Utils.stringDefined(password1)
	    || Utils.stringDefined(password2)) {
            if (password1.equals(password2)) {
                user.setPassword(hashPassword(password1));
                addActivity(request, user, ACTIVITY_PASSWORD_CHANGE, "");
                return true;
            }
            return false;
        }

        return true;
    }


    /**
     * set the user state from the request
     *
     * @param request the request
     * @param user The user
     * @param doAdmin _more_
     *
     * @throws Exception On badness
     */
    private void applyUserProperties(Request request, User user,
                                     boolean doAdmin)
	throws Exception {
        user.setName(request.getReallyStrictSanitizedString(ARG_USER_NAME, user.getName()));
        user.setDescription(request.getReallyStrictSanitizedString(ARG_USER_DESCRIPTION,
					      user.getDescription()));
        user.setEmail(request.getReallyStrictSanitizedString(ARG_USER_EMAIL, user.getEmail()));
        user.setInstitution(getInstitution(request,user.getInstitution()));
        user.setCountry(getCountry(request,user.getCountry()));	
        user.setTemplate(request.getReallyStrictSanitizedString(ARG_USER_TEMPLATE,
                                           user.getTemplate()));
        user.setLanguage(request.getReallyStrictSanitizedString(ARG_USER_LANGUAGE,
                                           user.getLanguage()));
        user.setQuestion(request.getReallyStrictSanitizedString(ARG_USER_QUESTION,
                                           user.getQuestion()));
        user.setAnswer(request.getReallyStrictSanitizedString(ARG_USER_ANSWER, user.getAnswer()));
	if(request.get(ARG_USER_AVATAR_DELETE,false)) {
	    File f= getUserAvatarFile(user);
	    if(f!=null) f.delete();
	    user.setAvatar(null);
	} else {
	    String avatar = request.getUploadedFile(ARG_USER_AVATAR);
	    if(avatar!=null) {
		//Get rid of the old one
		File f= getUserAvatarFile(user);
		if(f!=null) f.delete();
		String ext = IO.getFileExtension(avatar);
		File userDir = getStorageManager().getUserDir(user.getId(),true);
		File upload = new File(avatar);
		File dest = new File(IOUtil.joinDir(userDir, "avatar" + ext));
		getStorageManager().moveFile(upload, dest);
		user.setAvatar(dest.getName());
	    }
	}


        String phone = request.getReallyStrictSanitizedString("phone",
						  (String) user.getProperty("phone"));
        if (phone != null) {
            user.putProperty("phone", phone);
        }

        if (doAdmin) {
            applyAdminState(request, user);
        }


        makeOrUpdateUser(user, true);
    }


    /**
     * _more_
     *
     * @param request the request
     * @param user _more_
     *
     * @throws Exception on badness
     */
    private void applyAdminState(Request request, User user)
	throws Exception {
        if ( !request.getUser().getAdmin()) {
            throw new IllegalArgumentException("Need to be admin");
        }
	user.setStatus(request.getString(ARG_USER_STATUS, user.getStatus()));
	if(!user.isActive()) {
	    getSessionManager().removeUserSession(request, user);
	}

        if ( !request.defined(ARG_USER_ADMIN)) {
            user.setAdmin(false);
        } else {
            user.setAdmin(request.get(ARG_USER_ADMIN, user.getAdmin()));
        }
        user.setIsGuest(request.get(ARG_USER_ISGUEST, false));

        List<String> roles = Utils.split(request.getReallyStrictSanitizedString(ARG_USER_ROLES,
							   ""), "\n", true, true);

        user.setRoles(Role.makeRoles(roles));
        setRoles(request, user);
    }


    /**
     * _more_
     *
     * @param request the request
     * @param user The user
     *
     * @throws Exception On badness
     */
    private void setRoles(Request request, User user) throws Exception {
        deleteRoles(user);
        if (user.getRoles() == null) {
            return;
        }
        for (Role role : user.getRoles()) {
            getDatabaseManager().executeInsert(Tables.USERROLES.INSERT,
					       new Object[] { user.getId(),
							      role.getRole() });
        }
    }

    /**
     * _more_
     *
     * @param request the request
     *
     * @return The result
     *
     * @throws Exception On badness
     */
    public Result adminUserEdit(Request request) throws Exception {
        String userId = request.getString(ARG_USER_ID, "");
        User   user   = findUser(userId);
        if (user == null) {
            throw new IllegalArgumentException(msgLabel("Could not find user") + userId);
        }
        StringBuffer sb = new StringBuffer();
        StringBuffer sb2 = new StringBuffer();
	boolean verified = false;
	if(request.defined(ARG_USER_DELETE_CONFIRM) ||
	   request.defined(ARG_USER_CHANGE)) {
	    verified = getAuthManager().verify(request,sb2);
	}

        if (verified && request.defined(ARG_USER_DELETE_CONFIRM)) {
            deleteUser(user);
            return new Result(request.makeUrl(getRepositoryBase().URL_USER_LIST));
        }

	boolean doDelete = request.defined(ARG_USER_DELETE)||
	    request.defined(ARG_USER_DELETE_CONFIRM);
	HU.titleSectionOpen(sb, (doDelete?"Delete User: ":"Edit User: ") + getUserTitle(user));

        if (verified && request.defined(ARG_USER_CHANGE)) {
	    boolean havePassword =stringDefined(request.getString(ARG_USER_PASSWORD1, "").trim());
            if(havePassword && !checkAndSetNewPassword(request, user)) {
		sb.append(messageError(msg("Incorrect new passwords given for user")));
	    } else {
		String message = havePassword?"User settings and password have been changed":
		    "User settings have been changed";
		sb.append(messageNote(msg(message)));
		applyUserProperties(request, user, true);
		updateUser(user);
	    }
        }


	sb.append(sb2);
        sb.append(request.uploadForm(getRepositoryBase().URL_USER_EDIT));
	//        sb.append(request.formPost(getRepositoryBase().URL_USER_EDIT));
        sb.append(HU.hidden(ARG_USER_ID, user.getId()));
        if (doDelete) {
            sb.append(messageQuestion( msg("Are you sure you want to delete the user?"),
				       HU.buttons(HU.submit("Yes",ARG_USER_DELETE_CONFIRM),
						  HU.submit(LABEL_CANCEL, ARG_USER_CANCEL),
						  getAuthManager().getVerification(request))));
	           
	    sb.append("<br>");
	} else {
            String buttons =
                HU.submit("Change User", ARG_USER_CHANGE)
                + HU.space(2)
                + HU.submit("Delete User", ARG_USER_DELETE)
                + HU.space(2)
                + HU.submit(LABEL_CANCEL, ARG_CANCEL);
            sb.append(buttons);
	    getAuthManager().addVerification(request,sb);
            makeUserForm(request, user, sb, true);
	    //            if (user.canChangePassword()) {
	    sb.append(HU.vspace());
	    //	    sb.append(RepositoryUtil.header(msgLabel("Password")));
	    makePasswordForm(request, user, sb,"Password");
	    //            }
            sb.append(HU.vspace());
            //            sb.append(buttons);
        }
        sb.append(HU.formClose());
        sb.append(HU.sectionClose());
        return getAdmin().makeResult(request,"User:" + user.getLabel(), sb);
    }


    /**
     * _more_
     *
     * @param request the request
     * @param user The user
     * @param sb _more_
     * @param includeAdmin _more_
     *
     * @throws Exception On badness
     */
    private void makeUserForm(Request request, User user, Appendable sb,
                              boolean includeAdmin)
	throws Exception {
	boolean isAdmin = request.getUser().getAdmin();
        if (includeAdmin&& !isAdmin) {
	    throw new IllegalArgumentException("Need to be admin");
	}

	String size = HU.SIZE_40;
        sb.append(HU.formTable());
	sb.append(formEntry(request, msgLabel("ID"), user.getId()));
        if (isAdmin || user.canChangeNameAndEmail()) {
            sb.append(formEntry(request, msgLabel("Name"),
                                HU.input(ARG_USER_NAME,request.getReallyStrictSanitizedString(ARG_USER_NAME,user.getName()), size)));
        }
        if (includeAdmin) {
            sb.append(HU.formHelp("Note: An administrator can do anything on this RAMADDA",true));
	    List status = Utils.makeListFromValues(new HtmlUtils.Selector("Active",User.STATUS_ACTIVE),
					 new HtmlUtils.Selector("Inactive",User.STATUS_INACTIVE),
					 new HtmlUtils.Selector("Pending",User.STATUS_PENDING));
            sb.append(formEntry(request, "Status:",
                                HU.select(ARG_USER_STATUS, status,
					  request.getString(ARG_USER_STATUS,user.getStatus()))));

            sb.append(HU.formHelp("Note: An administrator can do anything on this RAMADDA",true));
            sb.append(formEntry(request, "",
                                HU.labeledCheckbox(ARG_USER_ADMIN, "true",
						   request.get(ARG_USER_ADMIN,user.getAdmin()),
						   "Is Administrator")));

            sb.append(HU.formHelp("A guest user can login but cannot change their password",true));
            sb.append(formEntry(request, "",
                                HU.labeledCheckbox(ARG_USER_ISGUEST, "true",
						   request.get(ARG_USER_ISGUEST,user.getIsGuest()),
						   "Is Guest User")));
            String       userRoles = user.getRolesAsString("\n");
            StringBuffer allRoles  = new StringBuffer();
            List<Role>   roles     = getUserRoles();
            allRoles.append(
			    "<table border=0 cellspacing=0 cellpadding=0><tr valign=\"top\"><td><b>e.g.:</b></td><td>&nbsp;&nbsp;</td><td>");
            int cnt = 0;
            allRoles.append("</td><td>&nbsp;&nbsp;</td><td>");
            for (int i = 0; i < roles.size(); i++) {
                if (cnt++ > 4) {
                    allRoles.append("</td><td>&nbsp;&nbsp;</td><td>");
                    cnt = 0;
                }
                allRoles.append("<i>");
                allRoles.append(roles.get(i).getRole());
                allRoles.append("</i><br>");
            }
            allRoles.append("</table>\n");

            sb.append(formEntry(request, "",
				HU.span("Roles are used in entry permissions",
					HU.clazz("ramadda-form-help"))));


            String roleEntry =  HU.hbox(HU.textArea(ARG_USER_ROLES, request.getReallyStrictSanitizedString(ARG_USER_ROLES,userRoles),
						    5, 20), allRoles.toString());
            sb.append(formEntryTop(request, msgLabel("Roles"), roleEntry));
        }

        if (includeAdmin || user.canChangeNameAndEmail()) {
            sb.append(HU.formHelp("User Information",true));
            sb.append(formEntry(request, msgLabel("Email"),
                                HU.input(ARG_USER_EMAIL,
					 request.getReallyStrictSanitizedString(ARG_USER_EMAIL,user.getEmail()), size)));
	    addInstitutionWidget(request, sb,user.getInstitution());
	    addCountryWidget(request, sb,user.getCountry());	    


            sb.append(formEntry(request, msgLabel("Description"),
                                HU.textArea(ARG_USER_DESCRIPTION,
					    request.getReallyStrictSanitizedString(ARG_USER_DESCRIPTION,user.getDescription()), 5, 30)));

	    /*
	      sb.append(formEntry(request, msgLabel("Phone"),
	      HU.input("phone",
	      request.getReallyStrictSanitizedString("phone", (String) user.getProperty("phone", "")),
	      size)));
	    */
	    String file  = HU.fileInput(ARG_USER_AVATAR, "");
	    String avatar =   getUserAvatar(request,  user, true,-1,null);
	    if(avatar!=null) {
		file+="<p>"+avatar +" " + HU.labeledCheckbox(ARG_USER_AVATAR_DELETE,"true",false,"Delete");
	    }
	    sb.append(formEntry(request,msgLabel("Avatar"), file));
        }

        List<TwoFacedObject> templates =
            getPageHandler().getTemplateSelectList();
	sb.append(HU.formHelp("Preferences",true));
        sb.append(formEntry(request, msgLabel("Page Style"),
                            HU.select(ARG_USER_TEMPLATE, templates,
				      request.getReallyStrictSanitizedString(ARG_USER_TEMPLATE,user.getTemplate()))));

        List languages = new ArrayList(getPageHandler().getLanguages());
        languages.add(0, new TwoFacedObject("-default-", ""));
        sb.append(formEntry(request, msgLabel("Language"),
                            HU.select(ARG_USER_LANGUAGE, languages,
				      request.getReallyStrictSanitizedString(ARG_USER_LANGUAGE,user.getLanguage()))));
        sb.append(HU.formTableClose());
    }



    /**
     * _more_
     *
     * @param request the request
     * @param user The user
     * @param sb _more_
     *
     * @throws Exception On badness
     */
    private void makePasswordForm(Request request, User user, Appendable sb,String...header)
	throws Exception {
        sb.append(HU.formTable());
	if(header.length>0) {
            sb.append(HU.formHelp(header[0],true));	    
	}
        sb.append(formEntry(request, msgLabel("New Password"),
                            HU.password(ARG_USER_PASSWORD1)));

        sb.append(formEntry(request, msgLabel("New Password Again"),
                            HU.password(ARG_USER_PASSWORD2)));

        sb.append(HU.formTableClose());
    }


    /**
     * _more_
     *
     * @param request the request
     *
     * @return The result
     *
     * @throws Exception On badness
     */
    public Result adminUserNewForm(Request request) throws Exception {
        StringBuffer sb = new StringBuffer();
        makeNewUserForm(request, sb);

        return getAdmin().makeResult(request, LABEL_NEW_USER, sb);
    }

    private String cleanUserId(String id) {
	id = id.trim().toLowerCase().replace(" ","_");
	return id;
    }

    public Result adminUserNewDo(Request request) throws Exception {
        StringBuffer sb          = new StringBuffer();

	request.ensureAdmin();
	if(!getAuthManager().verify(request,sb)) {
	    makeNewUserForm(request, sb);
	    return getAdmin().makeResult(request, LABEL_NEW_USER, sb);
	}


        StringBuffer errorBuffer = new StringBuffer();
        List<User>   users       = new ArrayList<User>();
        boolean      ok          = true;

        String       importFile  = request.getUploadedFile(ARG_USER_IMPORT);
        if (importFile != null) {
            List<User> importUsers =
                (List<User>) getRepository().decodeObject(
							  IOUtil.readInputStream(
										 getStorageManager().getFileInputStream(
															new File(importFile))));
            for (User user : importUsers) {
                if (findUser(user.getId()) != null) {
                    sb.append("<li> Imported user already exists:"
                              + user.getId() + "<br>");

                    continue;
                }
                users.add(user);
            }
        }


        if (importFile == null) {
            for (String line :
		     (List<String>) Utils.split(
						request.getString(ARG_USER_BULK, ""), "\n", true,
						true)) {
                if (line.startsWith("#")) {
                    continue;
                }
                List<String> toks = (List<String>) Utils.split(line, ",",
							       true, true);
                if (toks.size() == 0) {
                    continue;
                }
                if (toks.size() < 2) {
                    ok = false;
                    sb.append(messageError("Bad line:" + line));

                    break;
                }
                String id        = cleanUserId(toks.get(0));
                String password1 = toks.get(1);
                String name      = ((toks.size() >= 3)
                                    ? toks.get(2)
                                    : id);
                String email     = ((toks.size() >= 4)
                                    ? toks.get(3)
                                    : "");
                if (findUser(id) != null) {
                    ok = false;
                    sb.append(messageError("User already exists:" + id));
                    break;
                }
                User user = new User(id, User.STATUS_ACTIVE,
				     name, email, DEFAULT_INSTITUTION,
				     DEFAULT_COUNTRY,
				     "", "",
                                     hashPassword(password1), "", false, "",
                                     "", false, new Date(),
				     null);
                users.add(user);
            }


            if ( !ok) {
                makeNewUserForm(request, sb);
                return getAdmin().makeResult(request, LABEL_NEW_USER, sb);
            }
        }


        if (users.size() == 0) {
            String  id        = "";
            String  name      = "";
            String  desc      = "";
            String  email     = "";
            String  institution     = "";
            String  country     = "";	    	    
            String  password1 = "";
            String  password2 = "";
            boolean admin     = false;
            boolean guest     = false;	    
            if (request.defined(ARG_USER_ID)) {
                id        = cleanUserId(request.getString(ARG_USER_ID, ""));
                name      = request.getReallyStrictSanitizedString(ARG_USER_NAME, name).trim();
                desc = request.getReallyStrictSanitizedString(ARG_USER_DESCRIPTION, name).trim();
                email     = request.getReallyStrictSanitizedString(ARG_USER_EMAIL, "").trim();
                institution     = getInstitution(request,"");
                country     = getCountry(request,"");		
                password1 = request.getString(ARG_USER_PASSWORD1, "").trim();
                password2 = request.getString(ARG_USER_PASSWORD2, "").trim();
                admin     = request.get(ARG_USER_ADMIN, false);
                guest     = request.get(ARG_USER_ISGUEST, false);		

                boolean okToAdd = true;
                if (id.length() == 0) {
                    okToAdd = false;
                    errorBuffer.append(msg("Please enter an ID"));
                    errorBuffer.append(HU.br());
                }

                if ((password1.length() == 0) && (password2.length() == 0)) {
                    password1 = password2 = getRepository().getGUID() + "."
			+ Math.random();
                }


                if ( !Utils.passwordOK(password1, password2)) {
                    okToAdd = false;
                    errorBuffer.append(msg("Invalid password"));
                    errorBuffer.append(HU.br());
                }

                if (findUser(id) != null) {
                    okToAdd = false;
                    errorBuffer.append(msg("User with given id already exists"));
                    errorBuffer.append(HU.br());
                }

                if (okToAdd) {
                    User newUser = new User(id, User.STATUS_ACTIVE,
					    name, email, institution,
					    country,
					    DEFAULT_QUESTION,DEFAULT_ANSWER,
                                            hashPassword(password1), desc,
                                            admin, "", "", false, new Date(),
					    null);
		    newUser.setIsGuest(guest);
                    users.add(newUser);
                }
            }
        }
        if (users.size() > 0) {
	    List<Role> newUserRoles =
		Role.makeRoles(Utils.split(request.getReallyStrictSanitizedString(ARG_USER_ROLES, ""),
					   "\n", true, true));

	    String homeGroupId = request.getString(ARG_USER_HOME + "_hidden", "");
	    HU.titleSectionOpen(sb, "Create New Users");
	    sb.append("<ul>");
	    for (User newUser : users) {
		if (importFile == null) {
		    newUser.setRoles(newUserRoles);
		}
		makeOrUpdateUser(newUser, false);
		setRoles(request, newUser);
		sb.append("<li> ");
		sb.append(msgLabel("Created user"));
		sb.append(HU.space(1));
		sb.append(HU.href(request.makeUrl(getRepositoryBase().URL_USER_EDIT, ARG_USER_ID,
						  newUser.getId()), newUser.getId()));
		StringBuffer msg =
		    new StringBuffer(request.getString(ARG_USER_MESSAGE, ""));
		msg.append("<p>User id: " + newUser.getId() + "<p>");
		msg.append(
			   "Click on this link to send a password reset link to your registered email address:<br>");
		String resetUrl =
		    HU.url(
			   getRepositoryBase().URL_USER_RESETPASSWORD.toString(),
			   ARG_USER_NAME, newUser.getId());

		if ( !resetUrl.startsWith("http")) {
		    resetUrl = request.getAbsoluteUrl(resetUrl);
		}
		msg.append(HU.href(resetUrl,
				   "Send Password Reset Message"));
		msg.append("<p>");

		if (homeGroupId.length() > 0) {
		    Entry parent = getEntryManager().findGroup(request, homeGroupId);
		    String name = newUser.getName();
		    if ( !Utils.stringDefined(name)) {
			name = newUser.getId();
		    }
		    Entry home = getEntryManager().makeNewGroup(request,parent, name,
								newUser, null, TypeHandler.TYPE_HOMEPAGE);
		    msg.append("A home folder has been created for you: ");
		    String homeUrl =
			HU.url(
			       getRepositoryBase().URL_ENTRY_SHOW.toString(),
			       ARG_ENTRYID, home.getId());
		    msg.append(HU.href(request.getAbsoluteUrl(homeUrl),
				       home.getFullName()));
		    addFavorites(request, newUser,
				 (List<Entry>) Misc.newList(home));
		}

		if ((newUser.getEmail().length() > 0)
		    && request.get(ARG_USER_SENDMAIL, false)
		    && getMailManager().isEmailEnabled()) {
		    getRepository().getMailManager().sendEmail(
							       newUser.getEmail(), "RAMADDA User Account",
							       msg.toString(), true);

		    sb.append(" sent mail to:" + newUser.getEmail());
		}
	    }
	    sb.append("</ul>");
	    sb.append(HU.sectionClose());
	    return getAdmin().makeResult(request, LABEL_NEW_USER, sb);
	    //            return addHeader(request, sb, "");
        }
        if (errorBuffer.toString().length() > 0) {
            sb.append(messageError(errorBuffer.toString()));
        }
        makeNewUserForm(request, sb);
        return getAdmin().makeResult(request, LABEL_NEW_USER, sb);


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
    public Result adminUserSelectDo(Request request) throws Exception {
        StringBuffer sb = new StringBuffer();
        HU.titleSectionOpen(sb, "User Actions");
        List<User> users = new ArrayList<User>();

        Hashtable  args  = request.getArgs();
        for (Enumeration keys = args.keys(); keys.hasMoreElements(); ) {
            String arg = (String) keys.nextElement();
            if ( !arg.startsWith("user_")) {
                continue;
            }

            if ( !request.get(arg, false)) {
                continue;
            }
            String userId = arg.substring("user_".length());
            User   user   = findUser(userId);
            users.add(user);
        }

        if (request.defined(ARG_USER_CANCEL)) {
            sb.append(HU.sectionClose());
            return new Result(request.makeUrl(getRepositoryBase().URL_USER_LIST));
        }

        if (request.defined(ARG_USER_DELETE_CONFIRM) &&
	    getAuthManager().verify(request,sb)) {
	    sb.append(HU.vspace());
	    for (User user : users) {
		deleteUser(user);
		sb.append("Deleted user: " + user.getId());
		sb.append(HU.vspace());
	    }
	    sb.append(HU.sectionClose());
	    return getAdmin().makeResult(request, "Delete Users", sb);
	}
	if(users.size()==0) {
	    sb.append(messageWarning("No users selected"));
	    return getAdmin().makeResult(request, "Apply to Users", sb);
	}

	String action = request.getString(ARG_USER_ACTION,"");
	if(action.equals(ACTION_DELETE)) {
	    sb.append(request.formPost(URL_USER_SELECT_DO));
	    sb.append(HU.vspace());
	    sb.append(messageNote(msg("Are you sure you want to delete these users?")));
	    sb.append(HU.submit("Yes, really delete these users",
				ARG_USER_DELETE_CONFIRM));
	    sb.append(HU.space(2));
	    sb.append(HU.submit(LABEL_CANCEL, ARG_USER_CANCEL));
	    getAuthManager().addVerification(request,sb);
	    sb.append(HU.vspace());
	    for (User user : users) {
		String userCbx = HU.checkbox("user_" + user.getId(),
					     "true", true, "");
		sb.append(userCbx);
		sb.append(HU.space(1));
		sb.append(user.getId());
		sb.append(HU.space(1));
		sb.append(user.getName());
		sb.append(HU.br());
	    }
	    sb.append(HU.formClose());
	} else if(action.equals(ACTION_EMAIL)) {
	    boolean sent = false;
	    List<Address> to = new ArrayList<Address>();
	    for(User user: users) {
		String email = user.getEmail();
		if(!stringDefined(email)) continue;
		to.add(new InternetAddress(email, user.getName()));
	    }

	    if(request.exists(ARG_SEND)) {
		String from = request.getString("from","");
		String subject = request.getString("subject","");
		String contents = request.getString("contents","");		
		sent = true;
		if(!stringDefined(from)) {
		    sent=false;
		    sb.append(messageWarning("No from email provided"));
		}
		if(!stringDefined(subject)) {
		    sent=false;
		    sb.append(messageWarning("No subject provided"));
		}		
		if(!stringDefined(contents)) {
		    sent=false;
		    sb.append(messageWarning("No message provided"));
		}
		if(to.size()==0) {
		    sb.append(messageWarning("No emails available"));
		    sent = false;
		}
		if(sent) {
		    getMailManager().sendEmail(to,new  InternetAddress(from),
					       subject,
					       contents, true, false,null);
		    sb.append(messageNote("Email sent"));
		}
	    }

	    if(!sent) {
	     	if(to.size()==0) {
		    sb.append(messageWarning("No emails available"));
		} else {
		    sb.append(request.formPost(URL_USER_SELECT_DO));
		    sb.append(HU.hidden(ARG_USER_ACTION,ACTION_EMAIL));
		    sb.append(HU.formTable());
		    sb.append(HU.formEntry("",HU.submit("Send Mail", ARG_SEND)+ HU.space(2) +
					   HU.submit("Cancel","cancel")));
		    sb.append(HU.formEntry("From:",HU.input("from",request.getUser().getEmail(),HU.SIZE_50)));
		    sb.append(HU.formEntry("Subject:",HU.input("subject","",HU.SIZE_50)));
		    sb.append(HU.formEntryTop("Message:",HU.textArea("contents","",8,80)));
		    StringBuilder tmp  = new StringBuilder();
		    for(User user: users) {
			tmp.append(HU.hidden("user_"+ user.getId(),true));
			tmp.append(HU.div(user.getId() +" - " + user.getEmail(),""));
		    }
		    sb.append(HU.formEntryTop("To:", tmp.toString()));
		    sb.append(HU.formTableClose());
		    sb.append(HU.formClose());
		}
	    }
	} else if(action.equals(ACTION_CSV)) {
	    StringBuilder csv = new StringBuilder();
	    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	    csv.append("id,name,email,instituion,create date\n");
	    for(User user: users) {
		csv.append(user.getId());
		csv.append(",");
		csv.append(user.getName());
		csv.append(",");		
		csv.append(user.getEmail());
		csv.append(",");
		csv.append(user.getInstitution());
		csv.append(",");		
		csv.append(sdf.format(user.getAccountCreationDate()));
		csv.append("\n");		
	    }
	    request.setReturnFilename("users.csv",true);
	    return new Result("", csv,"text/csv");
	} else {
	    sb.append(messageWarning("No action selected"));
	    
	}
	sb.append(HU.sectionClose());
	return getAdmin().makeResult(request, "Delete Users", sb);
    }



    /**
     * _more_
     *
     * @param request the request
     * @param sb _more_
     *
     * @throws Exception on badness
     */
    private void makeNewUserForm(Request request, StringBuffer sb)
	throws Exception {
        HU.titleSectionOpen(sb, "Create New Users");
	String size = HU.SIZE_30;
	int cols = 30;

        sb.append(request.uploadForm(URL_USER_NEW_DO));
        StringBuffer formSB = new StringBuffer();
        String       id     = request.getString(ARG_USER_ID, "").trim();
        String       name   = request.getReallyStrictSanitizedString(ARG_USER_NAME, "").trim();
        String       desc = request.getReallyStrictSanitizedString(ARG_USER_DESCRIPTION, "").trim();
        String       email  = request.getReallyStrictSanitizedString(ARG_USER_EMAIL, "").trim();
        String       institution = getInstitution(request,"");
        String       country = getCountry(request,"");	
        boolean      admin  = request.get(ARG_USER_ADMIN, false);
        boolean      guest  = request.get(ARG_USER_ISGUEST, false);	

        formSB.append(msgHeader("Create a single user"));
        formSB.append(HU.formTable());
        formSB.append(HU.formEntry("",HU.span("ID - Lower case, no spaces, no punctuation",
					      HU.clazz("ramadda-form-help"))));
        formSB.append(formEntry(request, msgLabel("ID"),
                                HU.input(ARG_USER_ID, id,
					 size)));
        formSB.append(formEntry(request, msgLabel("Name"),
                                HU.input(ARG_USER_NAME, name,
					 size)));

	formSB.append(HU.formHelp("Note: An administrator can do anything on this RAMADDA",true));
	HU.formEntry(formSB, "",  HU.labeledCheckbox(ARG_USER_ADMIN, "true",
						     admin,"Is Administrator"));

	formSB.append(HU.formHelp("A guest user can login but cannot change their password",true));
	HU.formEntry(formSB,"",    HU.labeledCheckbox(ARG_USER_ISGUEST, "true",
						      guest,"Is Guest User"));

	formSB.append(HU.formHelp("User Information",true));
        formSB.append(formEntry(request, msgLabel("Email"),
                                HU.input(ARG_USER_EMAIL, email, size)));
	addInstitutionWidget(request, formSB,institution);
	addCountryWidget(request, formSB,country);	
        formSB.append(formEntry(request, msgLabel("Description"),
                                HU.textArea(ARG_USER_DESCRIPTION,
					    desc, 5, cols)));
	
        formSB.append(HU.formEntryTop(msgLabel("Roles"),
				      HU.textArea(ARG_USER_ROLES, request.getReallyStrictSanitizedString(ARG_USER_ROLES, ""), 3, 25)));

	formSB.append(HU.formHelp("Password",true));
        formSB.append(formEntry(request, msgLabel("Enter Password"), HU.password(ARG_USER_PASSWORD1)));
        formSB.append(formEntry(request, msgLabel("Password Again"), HU.password(ARG_USER_PASSWORD2)));

	formSB.append(HU.formHelp("Create a folder using the user's name under this folder",true));        formSB.append(
		      HU.formEntry(
				   msgLabel("Home Folder"),
				   OutputHandler.makeEntrySelect(
								 request, ARG_USER_HOME, false, "", null)));

        StringBuffer msgSB = new StringBuffer();
        String       msg   =request.getReallyStrictSanitizedString(ARG_USER_MESSAGE,
					      "A new RAMADDA account has been created for you.");
        msgSB.append(HU.checkbox(ARG_USER_SENDMAIL, "true", false));
        msgSB.append(HU.space(1));
        msgSB.append(msgLabel("Send an email to the new user with message"));
        msgSB.append(HU.br());
        msgSB.append(HU.textArea(ARG_USER_MESSAGE, msg, 5, cols));
        if (getMailManager().isEmailEnabled()) {
            formSB.append(HU.formEntryTop(msgLabel("Notification"),
					  msgSB.toString()));
        }

        formSB.append(HU.formTableClose());
        StringBuffer bulkSB = new StringBuffer();
        bulkSB.append(msgHeader("Or create a number of users"));
        bulkSB.append(
		      "one user per line<br><i>user id, password, name, email</i><br>");
        bulkSB.append(HU.textArea(ARG_USER_BULK,
				  request.getString(ARG_USER_BULK,
						    ""), 10, 50));



	/**
	   bulkSB.append(HU.vspacea());
	   bulkSB.append(HU.b("User Import:"));
	   bulkSB.append(HU.space(1));
	   bulkSB.append(HU.fileInput(ARG_USER_IMPORT, ""));
	**/


        StringBuffer top = new StringBuffer();
        top.append(HU.table(HU.rowTop(HU.cols(formSB.toString(),  HU.div(bulkSB.toString(),HU.style("margin-left:8px;"))))));


        sb.append(HU.vspace());
        sb.append(top);
        sb.append(HU.vspace());
        sb.append(HU.submit("Create User", ARG_USER_NEW));
	sb.append("\n");
	getAuthManager().addVerification(request,sb);
	sb.append("\n");
        sb.append(HU.formClose());

        sb.append(HU.sectionClose());
    }



    public Result processSearch(Request request) throws Exception {
        List<String> ids     = new ArrayList<String>();
        String       suggest = request.getString("text", "").trim() + "%";
	Clause clause = Clause.or(Clause.like(Tables.USERS.COL_ID, suggest),
				  Clause.like(Tables.USERS.COL_NAME, suggest));
        Statement statement =
            getDatabaseManager().select(Tables.USERS.COL_ID, Tables.USERS.NAME,
					clause,	" order by " + Tables.USERS.COL_ID);
        SqlUtil.Iterator iter = getDatabaseManager().getIterator(statement);
        ResultSet        results;
        while ((results = iter.getNext()) != null) {
            ids.add(JsonUtil.quote(results.getString(1)));
        }
        StringBuilder sb = new StringBuilder(JsonUtil.list(ids));

        return new Result("", sb, JsonUtil.MIMETYPE);
    }


    private String getUserSortLink(Request request, String what,boolean ascending, String label) {
	String url = getRepositoryBase().URL_USER_LIST+"?sortby=" + what;
	if(request.getString("sortby","").equals(what)) {
	    url+="&ascending=" + (!ascending);
	}
	return HU.href(url, label);
    }
	


    /**
     * _more_
     *
     * @param request the request
     *
     * @return The result
     *
     * @throws Exception On badness
     */
    public Result adminUserList(Request request) throws Exception {

        if (request.exists(ARG_REMOVESESSIONID)) {
            getSessionManager().debugSession(
					     request,
					     "RAMADDA.adminUserList: removing session:"
					     + request.getString(ARG_REMOVESESSIONID));
            getSessionManager().removeSession(request,
					      request.getString(ARG_REMOVESESSIONID));

            return new Result(
			      request.makeUrl(
					      getRepositoryBase().URL_USER_LIST, ARG_SHOWTAB, "2"));
        }


        Hashtable<String, StringBuffer> rolesMap = new Hashtable<String,
	    StringBuffer>();
        List<Role>   rolesList = new ArrayList<Role>();
        StringBuffer usersHtml = new StringBuffer();
        StringBuffer rolesHtml = new StringBuffer();

        StringBuffer sb        = new StringBuffer();


        sb.append(request.form(URL_USER_NEW_FORM));
        HU.sectionOpen(sb, null, false);
        sb.append(HU.submit("Create New User"));
        sb.append(HU.formClose());
        sb.append(HU.vspace());

        Statement statement =
            getDatabaseManager().select(Tables.USERS.COLUMNS,
                                        Tables.USERS.NAME, new Clause(),
                                        " order by " + Tables.USERS.COL_ID);

        SqlUtil.Iterator iter  = getDatabaseManager().getIterator(statement);


        List<User>       users = new ArrayList();
        ResultSet        results;
        while ((results = iter.getNext()) != null) {
            users.add(getUser(results));
        }

	String sortBy = request.getString("sortby","admin");
	boolean ascending = request.get("ascending",true);
	Comparator comp = new Comparator() {
		public int compare(Object o1, Object o2) {
		    User u1 = (User)o1;
		    User u2 = (User)o2;			
		    int ids = Utils.compareIgnoreCase(u1.getId(),u2.getId(),0);
		    if(sortBy.equals("admin")) {
			if(u1.getAdmin() && !u2.getAdmin()) {
			    return -1;
			}
			if(!u1.getAdmin() && u2.getAdmin()) {
			    return 1;
			}
		    }
		    if(sortBy.equals("guest")) {
			if(u1.getIsGuest() && !u2.getIsGuest()) {
			    return -1;
			}
			if(!u1.getIsGuest() && u2.getIsGuest()) {
			    return 1;
			}
		    }		    
		    

		    if(sortBy.equals("date")) {
			Date d1 = u1.getAccountCreationDate();
			Date d2 = u2.getAccountCreationDate();			
			if(d1==null && d2!=null)
			    return -1;
			if(d1!=null && d2==null)
			    return 1;
			if(d1==null && d2==null)
			    return ids;						
			if(d1.equals(d2)) return ids;
			return d1.compareTo(d2);
		    }


		    if(sortBy.equals("institution")) {
			return Utils.compareIgnoreCase(u1.getInstitution(),
						       u2.getInstitution(),
						       ids);
		    }

		    if(sortBy.equals("country")) {
			return Utils.compareIgnoreCase(u1.getCountry(),
						       u2.getCountry(),
						       ids);
		    }
		    
		    if(sortBy.equals("email")) {
			return Utils.compareIgnoreCase(u1.getEmail(),
						       u2.getEmail(),
						       ids);
		    }
		    if(sortBy.equals("roles")) {
			return Utils.compareIgnoreCase(u1.getRoleText(null,null),
						       u2.getRoleText(null,null),
						       ids);
		    }		    

		    
		    if(sortBy.equals("name")) {
			return Utils.compareIgnoreCase(u1.getName(),
					     u2.getName(),
					     ids);
		    }
		    

		    return ids;
		}
	    };
	Object[] array = users.toArray();
	Arrays.sort(array, comp);
	users = (List<User>) Misc.toList(array);
	if(!ascending) {
	    List<User> tmp =new ArrayList<User>();
	    for(int i=users.size()-1;i>=0;i--)
		tmp.add(users.get(i));
	    users = tmp;
	}
	
	

        usersHtml.append(request.formPost(URL_USER_SELECT_DO));


	List actions =new ArrayList();
	actions.add(new HtmlUtils.Selector("Select Action",""));
	actions.add(new HtmlUtils.Selector("Delete",ACTION_DELETE));
	actions.add(new HtmlUtils.Selector("Download CSV",ACTION_CSV));
	if(getRepository().getMailManager().isEmailEnabled()) {
	    actions.add(new HtmlUtils.Selector("Send Mail",ACTION_EMAIL));
	}

        usersHtml.append(HU.submit("Apply to Selected Users:", ARG_USER_APPLY));
	usersHtml.append(" ");
	usersHtml.append(HU.select(ARG_USER_ACTION,actions,""));
        usersHtml.append(
			 HU.open(
				 "table", HU.attrs("width","100%","class","ramadda-user-table")));

	usersHtml.append("<br>");
	String searchButtons ="[{\"label\":\"Status:\"},{\"label\":\"Active\", \"value\":\"status:active\"},{\"label\":\"Inactive\", \"value\":\"status:inactive\"},{\"label\":\"Pending\", \"value\":\"status:pending\"},{\"label\":\"&nbsp;&nbsp;Type:\"},	{\"label\":\"Admin\", \"value\":\"admin\"},{\"label\":\"Guest\", \"value\":\"guest\"},{\"label\":\"&nbsp;&nbsp;\"},{\"label\":\"Show all\",\"clear\":true}]";
	String args = JU.map("focus","true","buttons",searchButtons);

	HU.script(usersHtml,
		  HU.call("HtmlUtils.initPageSearch",
			  "'.ramadda-user-row'",
			  "null",
			  //				 "'#" + uid +" .type-list-container'",
			  "'Find user'",
			  "false",args));
  

	    
	
	String idHeader = getUserSortLink(request, "id",ascending,"ID");
	String statusHeader = getUserSortLink(request, "status",ascending,"Status");
	String nameHeader = getUserSortLink(request, "name",ascending,"Name");
	String adminHeader = getUserSortLink(request, "admin",ascending,"Admin");		
	String guestHeader = getUserSortLink(request, "guest",ascending,"Guest");		
	String instHeader = getUserSortLink(request, "institution",ascending,"Institution");
	String countryHeader = getUserSortLink(request, "country",ascending,"Country");	
	String emailHeader = getUserSortLink(request, "email",ascending,"Email");
	String rolesHeader = getUserSortLink(request, "roles",ascending,"Roles");			
	String dateHeader = getUserSortLink(request, "date",ascending,"Create Date");		
	String allCbx = HU.checkbox("",	 "true", false, HU.attrs("id","userall","title","Toggle all"));
        usersHtml.append(HU.row(HU.cols(allCbx,
					HU.bold(msg("Edit")) + HU.space(2),
					HU.bold(idHeader) + HU.space(2),
					HU.bold(nameHeader) + HU.space(2),
					HU.bold(statusHeader) + HU.space(2),
					HU.bold(adminHeader) + HU.space(2),
					HU.bold(guestHeader) + HU.space(2),
					HU.bold(rolesHeader) + HU.space(2),
					HU.bold(emailHeader) + HU.space(2),
					HU.bold(instHeader) + HU.space(2),
					HU.bold(countryHeader) + HU.space(2),										
					HU.bold(dateHeader) + HU.space(2),					

					HU.bold(msg("Log")))));

	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        for (User user : users) {
            String userEditLink = HU.button(HU.href(request.makeUrl(
								    getRepositoryBase().URL_USER_EDIT,
								    ARG_USER_ID,
								    user.getId()), "Edit", HU.title("Edit user")));

            String userProfileLink =
                HU.href(
			HU.url(
			       request.makeUrl(getRepository().URL_USER_PROFILE),
			       ARG_USER_ID, user.getId()), user.getId(),
			"title=\"View user profile\"");

            String userLogLink =
                HU.href(
			request.makeUrl(
					getRepositoryBase().URL_USER_ACTIVITY, ARG_USER_ID,
					user.getId()), HU.getIconImage(
								       getRepository().getIconUrl(ICON_LOG), "title",
								       "View user log"));

            String userCbx = HU.checkbox("user_" + user.getId(),
					 "true", false, HU.attrs("class","ramadda-user-select"));




	    StringBuilder corpus = new StringBuilder();
	    corpus.append(user.getName() +" " + user.getId());
	    if(user.getIsGuest()) corpus.append(" guest ");
	    if(user.getAdmin()) corpus.append(" admin ");
	    corpus.append(" status:" + user.getStatus()+":");
	    if(stringDefined(user.getInstitution())) {
		corpus.append(" inst:" + user.getInstitution()+":");
		corpus.append(" hasinst ");
	    } else {
		corpus.append(" noinst ");
	    }
	    if(stringDefined(user.getCountry())) {
		corpus.append(" country:" + user.getCountry()+":");
		corpus.append(" hascountry ");
	    } else {
		corpus.append(" nocountry ");
	    }	    
	    if(stringDefined(user.getEmail())) {
		corpus.append(" email:" + user.getEmail()+":");
		corpus.append(" hasemail ");
	    } else {
		corpus.append(" noemail ");
	    }
	    String roleText= user.getRoleText(" role:",": ");
	    if(stringDefined(roleText)) {
		corpus.append(" " +roleText);
		corpus.append(" hasrole ");
	    } else {
		corpus.append(" norole ");
	    }
		    
	    String dttm = "NA";
	    if(user.getAccountCreationDate()!=null) {
		dttm = sdf.format(user.getAccountCreationDate());
	    }
	    StringBuilder rolesTD  = new StringBuilder();
	    List<Role> roles = user.getRoles();
	    if(roles!=null) {
		for(Role role: roles) {
                    rolesTD.append(HU.div(role.getRole(),""));
		}
	    }


            String row = HU.row(HU.cols(userCbx, userEditLink,
					userProfileLink, user.getName(),
					user.getStatus(), 
					"" + user.getAdmin(), 
					"" + user.getIsGuest(),
					rolesTD.toString(),
					user.getEmail(),
					user.getInstitution(),
					user.getCountry(),					
					dttm,
					userLogLink),
				HU.attrs("data-corpus",corpus.toString(),
					 "valign","top","class",
					 "ramadda-user-row " + (user.getAdmin()
								? "ramadda-user-admin"
								: user.getIsGuest()?"ramadda-user-guest":"")));
            usersHtml.append(row);
            if (roles != null) {
                for (Role role : roles) {
                    StringBuffer rolesSB = rolesMap.get(role.getRole());
                    if (rolesSB == null) {
                        rolesSB = new StringBuffer("");
                        rolesList.add(role);
                        rolesMap.put(role.getRole(), rolesSB);
                    }
                    rolesSB.append(HU.row(HU.cols("<li>",
						  userEditLink, user.getId(), user.getName(),
						  user.getEmail())));
                }
            }
        }
        usersHtml.append("</table>");
	HU.script(usersHtml,"HU.initToggleAll('userall','.ramadda-user-select',true);\n");

        usersHtml.append(HU.formClose());

        List<String> rolesContent = new ArrayList<String>();
        for (Role role : rolesList) {
            StringBuffer rolesSB = rolesMap.get(role.getRole());
            rolesContent.add("<table class=formtable>\n" + rolesSB.toString()
                             + "\n</table>");
        }
        if (rolesList.size() == 0) {
            rolesHtml.append(msg("No roles"));
        } else {
            HU.makeAccordion(rolesHtml, rolesList, rolesContent);
        }



        List tabTitles  = new ArrayList();
        List tabContent = new ArrayList();

        int  showTab    = request.get(ARG_SHOWTAB, 0);
        tabTitles.add(msg("User List"));
        tabContent.add(usersHtml.toString());

        tabTitles.add(msg("Roles"));
        tabContent.add(rolesHtml.toString());


        tabTitles.add(msg("Current Sessions"));
        tabContent.add(
		       getSessionManager().getSessionList(request).toString());


        tabTitles.add(msg("Recent User Activity"));
        tabContent.add(getUserActivities(request, null));


        tabTitles.set(showTab, tabTitles.get(showTab));
        sb.append(HU.vspace());
        sb.append(OutputHandler.makeTabs(tabTitles, tabContent, true));
        sb.append(HU.sectionClose());

        return getAdmin().makeResult(request, "RAMADDA-Admin-Users", sb);
    }


    /**
     * _more_
     *
     * @param results _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public User getUser(ResultSet results) throws Exception {
        int col = 1;
        //id, name, email, question, answer, hashedPassword, description
        //admin, language, template, isGuest, propertiesBlob

        User user = new User(results.getString(col++), //ID
                             results.getString(col++), //status
                             results.getString(col++), //Name
                             results.getString(col++),//email
                             results.getString(col++),//institution
                             results.getString(col++),//country			     
                             results.getString(col++),//question
                             results.getString(col++),//answer
                             results.getString(col++),//hashed password
                             results.getString(col++),//description
                             results.getBoolean(col++),//admin
                             results.getString(col++),//language
                             results.getString(col++),//page template
                             results.getBoolean(col++),//is guest
			     getDatabaseManager().getDate(results, col++,null), //acct creation date
                             results.getString(col++)//properties blob
			     );

        Statement statement = getDatabaseManager().select(
							  Tables.USERROLES.COL_ROLE,
							  Tables.USERROLES.NAME,
							  Clause.eq(
								    Tables.USERROLES.COL_USER_ID,
								    user.getId()));

        String[] array =
            SqlUtil.readString(getDatabaseManager().getIterator(statement),
                               1);
        List<String> roles = new ArrayList<String>(Misc.toList(array));
        user.setRoles(Role.makeRoles(roles));

        return user;
    }


    public String getUserSearchLink(Request request, User user) {
	String linkMsg ="Search for entries by this user";
	String userLinkId = HU.getUniqueId("userlink_");
	String label = user.getLabel();
	return  HU.href(getSearchManager().URL_ENTRY_SEARCH + "?"
			+ ARG_USER_ID + "=" + user.getId()
			+ "&" + SearchManager.ARG_SEARCH_SUBMIT
			+ "=true", label,HU.attr("title",linkMsg));
    }
	


    public String  getUserAvatar(Request request, User user, boolean checkIfExists,
				 int width, String imageArgs) {
	if(width<0) width=40;
	File avatarFile = user==null?null:getUserAvatarFile(user);
	if(checkIfExists && (user==null || avatarFile == null)) return null;
	if(imageArgs == null) imageArgs = "";
	if(imageArgs.indexOf("width=")<0) imageArgs+=" width=" + width+"px ";
	imageArgs+=HU.cssClass("ramadda-user-avatar");
	imageArgs+=" loading=lazy ";
	String url = getRepository().getUrlBase()+"/user/avatar";
	if(avatarFile!=null) {
	    url+="?ts=" +avatarFile.lastModified();
	}


	if(user!=null) url+="&user="+ user.getId();
	return HU.img(url,null,imageArgs);
    }

    private File getUserAvatarFile(User user)  {
	if(user==null) return null;
	String avatar = user.getAvatar();
	if(!stringDefined(avatar)) return null;
	File f = new File(IOUtil.joinDir(getStorageManager().getUserDir(user.getId(),false), avatar));
	if(!f.exists()) return null;
	return f;
    }	


    public Result processAvatar(Request request) throws Exception {
	String userId = request.getString("user",null);
	String file = null;
        InputStream inputStream = null;
	if(userId!=null) {
	    File f= getUserAvatarFile(findUser(userId));
	    if(f!=null) {
		file = f.toString();
		inputStream = getStorageManager().getFileInputStream(f);
	    }
	}
	if(inputStream==null) {
	    file = "/org/ramadda/repository/htdocs/images/avatar.png";
	    inputStream = Utils.getInputStream(file,getClass());
	}
        String mimeType = getRepository().getMimeTypeFromSuffix(
								IO.getFileExtension(file));
        Result      result      = new Result(inputStream, mimeType);
	result.setCacheOk(true);
        return result;
    }


    /**
     * _more_
     *
     * @param request the request
     *
     * @return The result
     *
     * @throws Exception On badness
     */
    public Result processFavorite(Request request) throws Exception {
        String message = "";
        User   user    = request.getUser();

        if ( !request.getUser().canEditFavorites()) {
            return addHeader(
			     request,
			     new StringBuffer(messageError("Favorites not allowed")), "Favorites");
        }
        String entryId = request.getString(ARG_ENTRYID, BLANK);

        if (request.get(ARG_FAVORITE_ADD, false)) {
            Entry entry = getEntryManager().getEntry(request, entryId);
            if (entry == null) {
                return addHeader(
				 request, new StringBuffer(messageError("Cannot find or access entry")), 																		 "Favorites");
            }

            addFavorites(request, user, (List<Entry>) Misc.newList(entry));
            message = "Favorite added";
        } else if (request.get(ARG_FAVORITE_DELETE, false)) {
            getDatabaseManager().delete(
					Tables.FAVORITES.NAME,
					Clause.and(
						   Clause.eq(
							     Tables.FAVORITES.COL_ID,
							     request.getString(ARG_FAVORITE_ID, "")), Clause.eq(
														Tables.FAVORITES.COL_USER_ID, user.getId())));
            message = "Favorite deleted";
            user.setUserFavorites(null);
        } else {
            message = "Unknown favorite command";
        }

        String redirect = getRepositoryBase().URL_USER_HOME.toString();

        return new Result(HU.url(redirect, ARG_MESSAGE, message));

    }


    /**
     * _more_
     *
     * @param request the request
     * @param user The user
     * @param entries _more_
     *
     * @throws Exception On badness
     */
    private void addFavorites(Request request, User user, List<Entry> entries)
	throws Exception {
        List<Entry> favorites =
            FavoriteEntry.getEntries(getFavorites(request, user));
        if (user.getAnonymous()) {
            throw new IllegalArgumentException(
					       "Need to be logged in to add favorites");
        }
        if ( !request.getUser().canEditFavorites()) {
            throw new IllegalArgumentException("Cannot add favorites");
        }

        for (Entry entry : entries) {
            if (favorites.contains(entry)) {
                continue;
            }
            //COL_ID,COL_USER_ID,COL_ENTRY_ID,COL_NAME
            String name     = "";
            String category = "";
            getDatabaseManager().executeInsert(Tables.FAVORITES.INSERT,
					       new Object[] { getRepository().getGUID(),
							      user.getId(), entry.getId(), name,
							      category });
        }
        user.setUserFavorites(null);
    }


    /**
     * _more_
     *
     * @param request the request
     *
     * @return The result
     *
     * @throws Exception On badness
     */
    public Result processHome(Request request) throws Exception {
        boolean       responseAsXml  = request.responseAsXml();
        boolean       responseAsJson = request.responseAsJson();

        StringBuilder sb             = new StringBuilder();
        sb.append(HU.sectionOpen(null, false));
        User user = request.getUser();
        if (user.getAnonymous()) {
            if (responseAsXml) {
                return new Result(XmlUtil.tag(TAG_RESPONSE,
					      XmlUtil.attr(ATTR_CODE, CODE_ERROR),
					      "No user defined"), MIME_XML);
            }
            String msg = msg("You are not logged in");
            if (request.exists(ARG_FROMLOGIN)) {
                msg = msg + HU.vspace()
		    + msg("If you had logged in perhaps you have cookies turned off?");
            }
	    
            sb.append(HU.center(messageWarning(msg)));
            sb.append(makeLoginForm(request));
            sb.append(HU.sectionClose());

            return addHeader(request, sb, "User Home");
        } else {
            request.appendMessage(sb);
        }

        if (responseAsXml) {
            return new Result(XmlUtil.tag(TAG_RESPONSE,
                                          XmlUtil.attr(ATTR_CODE, "ok"),
                                          user.getId()), MIME_XML);
        }


        sb.append(HU.vspace());
        sb.append(HU.open("div", HU.cssClass("ramadda-links")));
        int cnt = 0;
        for (FavoriteEntry favorite : getFavorites(request, user)) {
            cnt++;
            //TODO: Use the categories
            String removeLink =
                HU.href(
			request.makeUrl(
					getRepositoryBase().URL_USER_FAVORITE,
					ARG_FAVORITE_ID, favorite.getId(),
					ARG_FAVORITE_DELETE, "true"), HU.img(
									     getRepository().getIconUrl(ICON_DELETE),
									     msg("Delete this favorite")));
            sb.append(removeLink);
            sb.append(HU.space(1));
            sb.append(getPageHandler().getBreadCrumbs(request,
						      favorite.getEntry()));
            sb.append(HU.br());
        }

        sb.append(HU.close("div"));

        if (request.getUser().canEditSettings() && (cnt == 0)) {
            sb.append(messageNote(
				  "You have no favorite entries defined.<br>When you see an  entry or folder just click on the "
				  + HU.img(getIconUrl(ICON_FAVORITE))
				  + " icon to add it to your list of favorites"));
        }
        sb.append(HU.sectionClose());

        return makeResult(request, "Favorites", sb);
    }



    /**
     * _more_
     *
     * @param request the request
     *
     * @return The result
     *
     * @throws Exception On badness
     */
    public Result processProfile(Request request) throws Exception {
        StringBuffer sb   = new StringBuffer();
        User         user = findUser(request.getString(ARG_USER_ID, ""));
        HU.titleSectionOpen(sb, "User Profile");
        if (user == null) {
            sb.append(msgLabel("Unknown user"));
            sb.append(request.getStrictSanitizedString(ARG_USER_ID, ""));
            sb.append(HU.sectionClose());
            return new Result("User Profile", sb);
        }

        sb.append(msgHeader("User Profile"));
        String searchLink =
            HU
	    .href(HU
		  .url(request
		       .makeUrl(
				getRepository().getSearchManager()
                                .URL_ENTRY_SEARCH), ARG_USER_ID,
		       user.getId()), HU
		  .img(getRepository()
		       .getIconUrl(ICON_SEARCH), msg(
						     "Search for entries created by this user")));

        sb.append(HU.formTable());
        sb.append(formEntry(request, msgLabel("ID"),
                            user.getId() + HU.space(2) + searchLink));
        sb.append(formEntry(request, msgLabel("Name"), user.getLabel()));
        String email = user.getEmail();
        if (stringDefined(email)) {
            email = email.replace("@", " _AT_ ");
            sb.append(formEntry(request, msgLabel("Email"), email));
        }

        String inst = user.getInstitution();
	sb.append(formEntry(request, msgLabel("Institution"), inst));
        String country = user.getCountry();
	sb.append(formEntry(request, msgLabel("Country"), country));	
	String desc = user.getDescription();
	if(stringDefined(desc)) {
	    sb.append(formEntryTop(request, msgLabel("Description"), desc));	
	}
	
        sb.append(HU.formTableClose());

        sb.append(HU.sectionClose());

        return new Result("User Profile", sb);
    }





    private static class PasswordReset {
        String user;
        Date dttm;

        public PasswordReset(String user, Date dttm) {
            this.user = user;
            this.dttm = dttm;
        }
    }

    public Result processFindUserId(Request request) throws Exception {
        StringBuffer sb    = new StringBuffer();
        String       title = "Find User ID";

        if ( !getMailManager().isEmailEnabled()) {
            return addHeader(
			     request,
			     new StringBuffer(messageWarning(
							     msg(
								 "This RAMADDA server has not been configured to send email"))), title);
        }

        String email = request.getString(ARG_USER_EMAIL, "").trim();
        if (email.length() > 0) {
            User user = findUserFromEmail(email);
            if (user != null) {
                String userIdMailTemplate =
                    getRepository().getProperty(PROP_USER_RESET_ID_TEMPLATE,
						"${userid}");
                String contents = userIdMailTemplate.replace("${userid}",
							     user.getId());
                contents = contents.replace(
					    "${url}",
					    request.getAbsoluteUrl(getRepository().URL_USER_LOGIN));
                String subject =
                    getRepository().getProperty(PROP_USER_RESET_ID_SUBJECT,
						"Your RAMADDA ID");
                getRepository().getMailManager().sendEmail(user.getEmail(),
							   subject, contents.toString(), true);
                String message =
                    "You user id has been sent to your registered email address";

                return new Result(
				  request.makeUrl(
						  getRepositoryBase().URL_USER_LOGIN, ARG_MESSAGE, message));
            }
            sb.append(messageError("No user is registered with the given email address"));
        }

        sb.append(messageNote(
			      "Please enter your registered email address"));
        sb.append(HU.vspace());
        sb.append(request.form(getRepositoryBase().URL_USER_FINDUSERID));
        sb.append(HU.formTable());
        sb.append(HU.formEntry("Your Email:",
			       HU.input(ARG_USER_EMAIL, email,
					HU.SIZE_30
					+ " autofocus=autofocus")));

        sb.append(HU.formEntry("", HU.submit("Submit")));
        sb.append(HU.formTableClose());
        sb.append(HU.formClose());

        return addHeader(request, sb, title);
    }




    /**
     * _more_
     *
     * @param request the request
     *
     * @return The result
     *
     * @throws Exception On badness
     */
    public Result processResetPassword(Request request) throws Exception {

        if ( !canDoLogin(request)) {
            return new Result("Password Reset",
			      new StringBuffer(messageWarning(
							      msg("Login is not allowed"))));
        }


        String key = request.getString(ARG_USER_PASSWORDKEY, (String) null);
        PasswordReset resetInfo = null;
        StringBuilder sb        = new StringBuilder();
        if (key != null) {
            resetInfo = passwordResets.get(key);
            if (resetInfo != null) {
                if (new Date().getTime() > resetInfo.dttm.getTime()) {
                    sb.append(messageError("Password reset has timed out" + "<br>"
					   +  "Please try again"));
                    resetInfo = null;
                    passwordResets.remove(key);
                }
            } else {
                sb.append(messageError("Password reset has timed out" + "<br>"  + "Please try again"));
            }
        }

        User user = ((resetInfo != null)
                     ? findUser(resetInfo.user, false)
                     : null);
        if (user != null) {
            if (request.exists(ARG_USER_PASSWORD1)) {
                if (checkAndSetNewPassword(request, user)) {
                    applyUserProperties(request, user, false);
		    changePassword(user);
                    sb.append(messageNote(msg("Your password has been reset")));
                    sb.append(makeLoginForm(request));
                    addActivity(request, request.getUser(),
                                ACTIVITY_PASSWORD_CHANGE, "");

                    return addHeader(request, sb, "Password Reset");
                }
                sb.append(messageError("Incorrect passwords"));
            }

            sb.append(request.formPost(getRepositoryBase().URL_USER_RESETPASSWORD));
            sb.append(HU.hidden(ARG_USER_PASSWORDKEY, key));
            sb.append(HU.formTable());
            sb.append(formEntry(request, msgLabel("User"), user.getId()));
            sb.append(formEntry(request, msgLabel("Password"),
                                HU.password(ARG_USER_PASSWORD1)));
            sb.append(formEntry(request, msgLabel("Password Again"),
                                HU.password(ARG_USER_PASSWORD2)));
            sb.append(formEntry(request, "", HU.submit("Submit")));

            sb.append(HU.formTableClose());
            sb.append(HU.formClose());

            return addHeader(request, sb, "Password Reset");
        }

        if ( !getMailManager().isEmailEnabled()) {
            return addHeader(
			     request, new StringBuffer(messageWarning(
								      msg(
									  "This RAMADDA server has not been configured to send email"))),  "Password Reset");
        }


        if (user == null) {
            user = findUser(request.getString(ARG_USER_NAME, ""), false);
        }
        if (user == null) {
            if (request.exists(ARG_USER_NAME)) {
                sb.append(messageError("Not a registered user"));
                sb.append(HU.vspace());
            }
            addPasswordResetForm(request, sb,
                                 request.getString(ARG_USER_NAME, ""));

            return addHeader(request, sb, "Password Reset");
        }



        if ( !request.getUser().canEditSettings()
	     && !request.getUser().getAnonymous()) {
            return addHeader(request,
                             new StringBuffer(msg("Cannot reset password")),
                             "Password Reset");
        }

        key = getRepository().getGUID() + "_" + Math.random();
        //Time out is 1 hour
        resetInfo = new PasswordReset(user.getId(),
                                      new Date(new Date().getTime()
					       + 1000 * 60 * 60));
        passwordResets.put(key, resetInfo);
        String toUser = user.getEmail();
        String url =
            getRepository().getHttpsUrl(
					request,
					getRepository().getUrlBase()
					+ getRepository().URL_USER_RESETPASSWORD.getPath()) + "?"
	    + ARG_USER_PASSWORDKEY + "=" + key;


        String template =
            getRepository().getProperty(PROP_USER_RESET_PASSWORD_TEMPLATE,
                                        "");
        template = template.replace("${url}", url);
        template = template.replace("${userid}", user.getId());
        String subject =
            getRepository().getProperty(PROP_USER_RESET_PASSWORD_SUBJECT,
                                        "Your RAMADDA Password");
        getRepository().getMailManager().sendEmail(toUser, subject, template,
						   true);
        StringBuffer message = new StringBuffer();
        message.append(messageNote(
				   "Instructions on how to reset your password have been sent to your registered email address."));

        return addHeader(request, message, "Password Reset");
    }


    /**
     * _more_
     *
     * @param request the request
     * @param sb _more_
     * @param name _more_
     */
    private void addPasswordResetForm(Request request, StringBuilder sb,
                                      String name) {
        sb.append(messageNote("Please enter your user ID"));
        request.formPostWithAuthToken(
				      sb, getRepositoryBase().URL_USER_RESETPASSWORD);
        sb.append(HU.formTable());
        sb.append(
		  HU.formEntry(
			       "User ID:",
			       HU.input(
					ARG_USER_NAME, name,
					HU.SIZE_20
					+ HU.cssClass(CSS_CLASS_USER_FIELD)
					+ " autofocus=autofocus")));
        sb.append(
		  HU.formEntry("", HU.submit("Reset your password")));
        sb.append(HU.formTableClose());
        sb.append(HU.formClose());
    }




    /**
     * _more_
     *
     * @param user _more_
     * @param rawPassword raw password
     *
     * @return _more_
     *
     * @throws Exception on badness
     */
    public boolean isPasswordValid(User user, String rawPassword)
	throws Exception {
        return isPasswordValid(user.getId(), rawPassword);
    }


    /**
     * _more_
     *
     * @param userId the user id
     * @param rawPassword raw (unhashed) password
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public boolean isPasswordValid(String userId, String rawPassword)
	throws Exception {
        User user = authenticateUser(null, userId, rawPassword,
                                     new StringBuffer());
        if (user == null) {
            return false;
        }

        return true;
    }


    /**
     * _more_
     *
     * @param request the request
     *
     * @return The result
     *
     * @throws Exception On badness
     */
    public Result processLogin(Request request) throws Exception {

        debugLogin("RAMADDA.processLogin");

        if ( !canDoLogin(request)) {
            return new Result(LABEL_LOGIN,
			      new StringBuffer(messageWarning(msg("Login is not allowed"))));
        }

        boolean responseAsData      = request.responseAsData();
        StringBuilder sb            = new StringBuilder();
        User          user          = null;
        String output = request.getString(ARG_OUTPUT, "");
        StringBuffer loginFormExtra = new StringBuffer();

        AccessManager.TwoFactorAuthenticator tfa =
            getAccessManager().getTwoFactorAuthenticator();
        tfa = null;

        if (request.exists(ARG_USER_ID)) {
            String name = request.getString(ARG_USER_ID, "").trim();
            if (name.equals(USER_DEFAULT) || name.equals(USER_ANONYMOUS)) {
                name = "";
            }

            boolean keepChecking = true;
            if (tfa != null) {
                user = findUser(request.getString(ARG_USER_ID, ""), false);
                if (user != null) {
                    if ( !tfa.userHasBeenAuthenticated(request, user, sb)) {
                        user = null;
                    }
                }
            }
            String loginExtra = "";
            if (user == null) {
                String password = request.getString(ARG_USER_PASSWORD,
						    "").trim();

                if ((name.length() > 0) && (password.length() > 0)) {
                    user = authenticateUser(request, name, password,
                                            loginFormExtra);
                }

                if ((user != null) && (userAgree != null)) {
                    if ( !request.get(ARG_USERAGREE, false)) {
                        user         = null;
                        keepChecking = false;
                        if (responseAsData) {
                            return getRepository().makeErrorResult(request,
								   "You must agree to the terms");
                        }
                        sb.append(HU.center(messageWarning(msg("You must agree to the terms"))));
                    } else {
                        loginExtra = "User agreed to terms and conditions";
                    }
                }
            }

            if (user != null) {
                if ((tfa != null) && tfa.userCanBeAuthenticated(user)) {
                    tfa.addAuthForm(request, user, sb);
                    keepChecking = false;
                }
            }

	    //Check status
	    if(user!=null) {
		if(!user.getStatus().equals(User.STATUS_ACTIVE)) {
		    sb.append(HU.center(messageWarning(msg("Could not login. User status is not active"))));
		    keepChecking = false;
		    user=null;
		}
	    }



            if (keepChecking) {
                if (user != null) {
                    addActivity(request, user, ACTIVITY_LOGIN, loginExtra);
                    debugLogin("RAMADDA.processLogin: login OK. user="
                               + user);
                    getSessionManager().createSession(request, user);
                    debugLogin("RAMADDA.processLogin: after create session:"
                               + request.getUser());

                    if (responseAsData) {
                        return getRepository().makeOkResult(request,
							    request.getSessionId());
                    }
                    String       destUrl;
                    String       destMsg;
                    StringBuffer response = new StringBuffer();
                    response.append(messageNote(msg("You are logged in")));
                    if (request.exists(ARG_REDIRECT)) {
                        destUrl = request.getBase64String(ARG_REDIRECT, "");
                        //Gack  - make sure we don't redirect to the logout page
                        if (destUrl.indexOf("logout") < 0) {
                            return new Result(destUrl);
                        }
                        response
                            .append(HU
				    .href(getRepositoryBase().URL_ENTRY_SHOW
					  .toString(), msg("Continue")));
                    } else if ( !user.canEditSettings()) {
                        response.append(
					HU.href(
						getRepository().getUrlBase(),
						msg("Continue")));
                    } else {
                        //Redirect to the top-level entry
                        if (true) {
                            return new Result(getRepositoryBase()
					      .URL_ENTRY_SHOW.toString());
                        }
                        response.append(
					HU.href(
						getRepositoryBase().URL_ENTRY_SHOW.toString(),
						msg(
						    "Continue to the top level of the repository")));
                        response.append("<p>");
                        response.append(
					HU.href(
						getRepositoryBase().URL_USER_HOME.toString(),
						msg("Continue to user home")));
                    }

                    return addHeader(request, response, LABEL_LOGIN);
                } else {
                    if (responseAsData) {
                        return getRepository().makeErrorResult(request,
							       "Incorrect user name or password");
                    }

                    if (name.length() > 0) {
                        //Check if they have a blank password
                        Statement statement = getDatabaseManager().select(
									  Tables.USERS.COL_PASSWORD,
									  Tables.USERS.NAME,
									  Clause.eq(
										    Tables.USERS.COL_ID,
										    name));
                        ResultSet results = statement.getResultSet();
                        if (results.next()) {
                            String password = results.getString(1);
                            if ((password == null)
				|| (password.length() == 0)) {
                                if (getMailManager().isEmailEnabled()) {
                                    sb.append(messageNote("Sorry, we were doing some cleanup and have reset your password"));
                                    addPasswordResetForm(request, sb, name);
                                } else {
                                    sb.append(messageNote("Sorry, we were doing some cleanup and your password has been reset. Please contact the RAMADDA administrator to reset your password."));
                                }
                                getDatabaseManager()
                                    .closeAndReleaseConnection(statement);

                                return addHeader(request, sb, LABEL_LOGIN);
                            }
                        }
                        getDatabaseManager().closeAndReleaseConnection(
								       statement);
                    }
                    sb.append(HU.center(messageWarning(msg("Incorrect user name or password"))));
                }
            }
        }


        if (user == null) {
            sb.append(makeLoginForm(request, loginFormExtra.toString()));
        }
        return addHeader(request, sb, LABEL_LOGIN);

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
    public Result processHumanQuestion(Request request) throws Exception {
        makeHumanAnswers();
        int    idx = request.get(ARG_HUMAN_QUESTION, 0);
        String s   = QUESTIONS.get(idx) + "=";

        BufferedImage image = new BufferedImage(50, 20,
						BufferedImage.TYPE_INT_RGB);
        Graphics2D g = (Graphics2D) image.getGraphics();
        g.setColor(Color.white);
        g.fillRect(0, 0, 100, 100);
        g.setColor(Color.black);
        g.drawString(s, 2, 17);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageUtils.writeImageToFile(image, "question.gif", bos, 1.0f);

        return new Result(BLANK, new ByteArrayInputStream(bos.toByteArray()),
                          "image/gif");
    }

    public boolean isRegistrationEnabled() {
        return getRepository().getProperty(PROP_REGISTER_OK, false);
    }

    public Result processRegister(Request request) throws Exception {

        if ( !isRegistrationEnabled()) {
            return new Result(
			      "New User Registration",
			      new StringBuffer(HU.center(messageWarning(msg("Registration is not allowed")))));
        }
        String mainKey = getRepository().getProperty(PROP_REGISTER_PASSPHRASE,
						     (String) null);

        String emailKey = getRepository().getProperty(PROP_REGISTER_EMAIL,
						      (String) null);


        StringBuffer sb   = new StringBuffer();
        String       name = request.getReallyStrictSanitizedString(ARG_USER_NAME, "").trim();
        String       id   = request.getReallyStrictSanitizedString(ARG_USER_ID, "").trim();
        if (id.equals(USER_DEFAULT) || id.equals(USER_ANONYMOUS)) {
            id = "";
        }

        if ( !Utils.stringDefined(name)) {
            name = id;
        }

        if (request.exists(ARG_USER_ID)) {
            boolean ok = true;
            if (ok && (id.length() == 0)) {
                ok = false;
                sb.append(HU.center(messageWarning(msg("Bad user id."))));
            }

            if (ok) {
                User user = findUser(id);
                if (user != null) {
                    ok = false;
                    sb.append(HU.center(messageWarning(msg("Sorry, the user ID already exists"))));
                }
            }

            String password1 = request.getString(ARG_USER_PASSWORD1, "").trim();
            String password2 = request.getString(ARG_USER_PASSWORD2, "").trim();	    
            if (ok) {
		if(password1.length() < MIN_PASSWORD_LENGTH || password2.length() < MIN_PASSWORD_LENGTH) {
		    ok = false;
		    sb.append(HU.center(messageWarning(msg("Bad password. Length must be at least "
							   + MIN_PASSWORD_LENGTH + " characters"))));
		}
		if(!password1.equals(password2)) {
		    ok = false;
		    sb.append(HU.center(messageWarning(msg("Passwords do not match"))));
		}
            }


            if (ok && Utils.stringDefined(mainKey)) {
                if ( !Misc.equals(mainKey,
                                  request.getString(ARG_REGISTER_PASSPHRASE,
						    null))) {
                    ok = false;
                    sb.append(HU.center(messageWarning(msg("Incorrect pass phrase"))));
                }
            }


            String email = request.getReallyStrictSanitizedString(ARG_USER_EMAIL, "").trim();
            if (ok && (email.length() < 0)) {
                ok = false;
                sb.append(HU.center(messageWarning(msg("Email required"))));

            }

            if (ok && Utils.stringDefined(emailKey)) {
                if ( !Misc.equals(email, emailKey)) {
                    ok = false;
                    sb.append(HU.center(messageWarning(msg("Your email does not match the required pattern"))));
                }
            }

            if (ok) {
                ok = isHuman(request, sb);
            }

            if (ok) {
		User user = new User(id,name );
		user.setPassword(hashPassword(password1));
		user.setEmail(email);
		user.setInstitution(getInstitution(request,""));
		user.setAccountCreationDate(new Date());
		makeOrUpdateUser(user, false);
		sb.append(HU.center(messageNote("You are now registered. Please login")));
		sb.append(makeLoginForm(request));
		return addHeader(request, sb, "New User Registration");

            }

	}
	



        String   formId   = HU.getUniqueId("entryform_");
        FormInfo formInfo = new FormInfo(formId);
        sb.append(
		  HU.formPost(
			      getRepository().getUrlPath(
							 request,
							 getRepositoryBase().URL_USER_REGISTER), HU.id(
												       formId)));
        sb.append(HU.formTable());

        formInfo.addRequiredValidation("User ID", ARG_USER_ID);
        formInfo.addRequiredValidation("Email", ARG_USER_EMAIL);
        formInfo.addMinSizeValidation("Password", ARG_USER_PASSWORD1,
                                      MIN_PASSWORD_LENGTH);
        formInfo.addMinSizeValidation("Password", ARG_USER_PASSWORD2,
                                      MIN_PASSWORD_LENGTH);	

        sb.append(formEntry(request, msgLabel("User ID"),
                            HU.input(ARG_USER_ID, id,
				     HU.id(ARG_USER_ID)
				     + HU.SIZE_20)));
        sb.append(formEntry(request, msgLabel("Name"),
                            HU.input(ARG_USER_NAME, name,
				     HU.SIZE_20)));
        sb.append(
		  formEntry(
			    request, msgLabel("Email"),
			    HU.input(
				     ARG_USER_EMAIL, request.getString(ARG_USER_EMAIL, ""),
				     HU.id(ARG_USER_EMAIL) + HU.SIZE_20)));
	addInstitutionWidget(request, sb,"");

        sb.append(
		  formEntry(
			    request, msgLabel("Enter Password"),
			    HU.password(
					ARG_USER_PASSWORD1, "",
					HU.id(ARG_USER_PASSWORD1)) + HU.space(1)
			    + "Minimum " + MIN_PASSWORD_LENGTH + " "
			    + "characters"));

        sb.append(
		  formEntry(
			    request, msgLabel("Enter Password Again"),
			    HU.password(
					ARG_USER_PASSWORD2, "",
					HU.id(ARG_USER_PASSWORD2))));

        if (Utils.stringDefined(mainKey)) {
            sb.append(
		      formEntry(
				request, msgLabel("Pass Phrase"),
				HU.password(ARG_REGISTER_PASSPHRASE)
				+ HU.space(1)
				+ "You should have been given a pass phrase to register"));

        }
        makeHumanForm(request, sb, formInfo);
        sb.append(formEntry(request, "", HU.submit("Register")));
        formInfo.addToForm(sb);


        sb.append(HU.formClose());
        sb.append(HU.formTableClose());

        return addHeader(request, sb, "New User Registration");


    }


    /**
     * _more_
     *
     * @param request the request
     * @param name _more_
     * @param password _more_
     * @param loginFormExtra _more_
     *
     * @return _more_
     *
     * @throws Exception on badness
     */
    private User authenticateUser(Request request, String name,
                                  String password,
                                  StringBuffer loginFormExtra)
	throws Exception {

        User user = authenticateUserInner(request, name, password,
                                          loginFormExtra);
        if (user == null) {
            handleBadPassword(request, name);
        } else {
            handleGoodPassword(request, name);
        }







        return user;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param user _more_
     */
    private void handleGoodPassword(Request request, String user) {
        badPasswordCount.remove(user);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param user _more_
     */
    private void checkUserPasswordAttempts(Request request, String user) {
        Integer count = badPasswordCount.get(user);
        if (count == null) {
            return;
        }
        count = Integer.valueOf(count.intValue() + 1);
        if (count.intValue() > MAX_BAD_PASSWORD_COUNT) {
            throw new IllegalArgumentException("Number of login attempts ("
					       + count + ") for user " + user
					       + " has exceeded the maximum allowed");
        }

    }


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param user _more_
     */
    private void handleBadPassword(Request request, String user) {
        Integer count = badPasswordCount.get(user);
        if (count == null) {
            count = Integer.valueOf(0);
        }
        count =  Integer.valueOf(count.intValue() + 1);
        badPasswordCount.put(user, count);
        if (count.intValue() > MAX_BAD_PASSWORD_COUNT) {
            throw new IllegalArgumentException("Number of login attempts ("
					       + count + ") for user " + user
					       + " has exceeded the maximum allowed");
        }
        //If the login failed then sleep for 1 second. This will keep bots 
        //from repeatedly trying passwords though maybe not needed with the above checks
        Misc.sleepSeconds(1);
    }

    /**
     * _more_
     *
     * @param request the request
     * @param name _more_
     * @param password _more_
     * @param loginFormExtra _more_
     *
     * @return _more_
     *
     * @throws Exception on badness
     */
    private User authenticateUserInner(Request request, String name,
                                       String password,
                                       StringBuffer loginFormExtra)
	throws Exception {


        checkUserPasswordAttempts(request, name);

        debugLogin("RAMADDA.authenticateUser:" + name);

        User user = authenticateUserFromDatabase(request, name, password);
        if (user != null) {
            debugLogin(
		       "RAMADDA.authenticateUser: authenticated from database:"
		       + user);

            return user;
        }

        //Try the authenticators
        for (UserAuthenticator userAuthenticator : userAuthenticators) {
            debugLogin("RAMADDA.authenticateUser: trying:"
                       + userAuthenticator);
            user = userAuthenticator.authenticateUser(getRepository(),
						      request, loginFormExtra, name, password);
            if (user != null) {
                user.setIsLocal(false);
                debugLogin(
			   "RAMADDA.authenticateUser: authenticated from external authenticator: "
			   + user + " " + userAuthenticator);

                return user;
            }
        }


        //
        //!!IMPORTANT!!
        //Chain up to the parent
        //This allows anyone in a parent repository to have a login in the child repository
        //If that user is an admin then they have admin rights here
        //
        if (getRepository().getParentRepository() != null) {
            if (name.startsWith("parent:")) {
                name = name.replace("parent:", "");
            }
            debugLogin("RAMADDA. authenticating user with parent repository");
            user = getRepository().getParentRepository().getUserManager()
                .authenticateUserInner(request, name, password,
                                       loginFormExtra);
            if (user != null) {
                debugLogin("RAMADDA. got user from parent repository");
                String userName = user.getName();
                if (userName.length() == 0) {
                    userName = user.getId();
                }
                //Change the name to denote this user comes from above
                user.setName(getRepository().getParentRepository()
			     .getUrlBase().substring(1) + ":" + userName);

                return user;
            }
        }

        return user;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param name _more_
     * @param password _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private User authenticateUserFromDatabase(Request request, String name,
					      String password)
	throws Exception {

        Statement statement =
            getDatabaseManager().select(Tables.USERS.COLUMNS,
                                        Tables.USERS.NAME,
                                        Clause.eq(Tables.USERS.COL_ID, name));

        ResultSet results = statement.getResultSet();

        try {
            //User is not in the database
            if ( !results.next()) {
                debugLogin("RAMADDA: No user in database:" + name);

                return null;
            }

            String storedHash =
                results.getString(Tables.USERS.COL_NODOT_PASSWORD);
            if ( !Utils.stringDefined(storedHash)) {
                debugLogin("RAMADDA: No stored hash");

                return null;
            }

            String passwordToUse = getPasswordToUse(password);
            //Call getPasswordToUse to add the system salt
            boolean userOK = PasswordHash.validatePassword(passwordToUse,
							   storedHash);

            //Check for old formats of hashes
            if ( !userOK) {
                userOK = storedHash.equals(hashPassword_oldway(password));
                //            System.err.println ("trying the old way:" + userOK);
            }

            if ( !userOK) {
                userOK = storedHash.equals(hashPassword_oldoldway(password));
                //            System.err.println ("trying the old old way:" + userOK);
            }


            if (userOK) {
                return getUser(results);
            }

            return null;
        } finally {
            getDatabaseManager().closeAndReleaseConnection(statement);
        }
    }

    /**
     * _more_
     *
     * @param request the request
     *
     * @return The result
     *
     * @throws Exception On badness
     */
    public Result processLogout(Request request) throws Exception {
        addActivity(request, request.getUser(), ACTIVITY_LOGOUT, "");
        getSessionManager().debugSession(request,
                                         "RAMADDA.processLogout: "
                                         + request.getSessionId());
        getSessionManager().removeUserSession(request);
        request.setSessionId(getSessionManager().createSessionId());

        StringBuilder sb = new StringBuilder();
        sb.append(HU.center(messageNote(msg("You are logged out"))));
        sb.append(makeLoginForm(request));

        return addHeader(request, sb, "Logout");
    }




    /**
     * _more_
     *
     * @throws Exception On badness
     */
    public void initOutputHandlers() throws Exception {
        OutputHandler outputHandler = new OutputHandler(getRepository(),
							"Favorites") {
		public void getEntryLinks(Request request, State state,
					  List<Link> links)
                    throws Exception {
		    if (state.getEntry() != null) {
			Link link;
			if ( !request.getUser().getAnonymous()) {
			    link = makeLink(request, state.getEntry(),
					    OUTPUT_FAVORITE);
			    links.add(0,link);
			}
		    }
		}

		public boolean canHandleOutput(OutputType output) {
		    return output.equals(OUTPUT_FAVORITE);
		}

		@Override
		public Result outputGroup(Request request, OutputType outputType,
					  Entry group, List<Entry> children)
                    throws Exception {
		    OutputType output = request.getOutput();
		    User       user   = request.getUser();
		    if (group.isDummy()) {
			addFavorites(request, user, children);
		    } else {
			addFavorites(request, user,
				     (List<Entry>) Misc.newList(group));
		    }
		    String redirect =
			getRepositoryBase().URL_USER_HOME.toString();

		    return new Result(HU.url(redirect, ARG_MESSAGE, "Favorites Added"));
		}
	    };

        outputHandler.addType(OUTPUT_FAVORITE);
        getRepository().addOutputHandler(outputHandler);
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public List<Role> getUserRoles() throws Exception {
        String[] roleArray =
            SqlUtil.readString(
			       getDatabaseManager().getIterator(
								getDatabaseManager().select(
											    SqlUtil.distinct(Tables.USERROLES.COL_ROLE),
											    Tables.USERROLES.NAME, new Clause())), 1);
        List<String> roles = new ArrayList<String>(Misc.toList(roleArray));
        for (UserAuthenticator userAuthenticator : userAuthenticators) {
            List<String> authenticatorRoles = userAuthenticator.getAllRoles();
            if (authenticatorRoles != null) {
                roles.addAll(authenticatorRoles);
            }
        }

        return Role.makeRoles(roles);
    }

    public void   addInstitutionWidget(Request request, Appendable sb,String value) throws Exception {
	String v = getInstitution(request, value);
	String sel = HU.select(ARG_USER_INSTITUTION,
			       getInstitutions(),
			       value);
	sb.append(formEntry(request, msgLabel("Institution"),
			    sel +HU.space(2) +"Or: " +
			    HU.input(ARG_USER_INSTITUTION+"_extra",
				     request.getString(ARG_USER_INSTITUTION+"_extra",""), HU.SIZE_30)));
    }

    public String getInstitution(Request request,String dflt) {
	String inst =request.getReallyStrictSanitizedString(ARG_USER_INSTITUTION+"_extra",null);
	if(!stringDefined(inst)) {
	    inst =request.getReallyStrictSanitizedString(ARG_USER_INSTITUTION,dflt);
	}
	return HU.sanitizeString(inst);
    }


    public void   addCountryWidget(Request request, Appendable sb,String value) throws Exception {
	String v = getCountry(request, value);
	String sel = HU.select(ARG_USER_COUNTRY,
			       getCountrys(),
			       value);
	sb.append(formEntry(request, msgLabel("Country"),
			    sel +HU.space(2) +"Or: " +
			    HU.input(ARG_USER_COUNTRY+"_extra",
				     request.getString(ARG_USER_COUNTRY+"_extra",""), HU.SIZE_30)));
    }
    

    public String getCountry(Request request,String dflt) {
	String inst =request.getReallyStrictSanitizedString(ARG_USER_COUNTRY+"_extra",null);
	if(!stringDefined(inst)) {
	    inst =request.getReallyStrictSanitizedString(ARG_USER_COUNTRY,dflt);
	}
	return HU.sanitizeString(inst);
    }


    public List getInstitutions() throws Exception {
        String[] array =
            SqlUtil.readString(
			       getDatabaseManager().getIterator(
								getDatabaseManager().select(
											    SqlUtil.distinct(Tables.USERS.COL_INSTITUTION),
											    Tables.USERS.NAME, new Clause())), 1);
        List l=  new ArrayList<String>(Misc.toList(array));
	l.add(0,new HtmlUtils.Selector("None specified",""));
	return l;
    }

    public List getCountrys() throws Exception {
        String[] array =
            SqlUtil.readString(
			       getDatabaseManager().getIterator(
								getDatabaseManager().select(
											    SqlUtil.distinct(Tables.USERS.COL_COUNTRY),
											    Tables.USERS.NAME, new Clause())), 1);
        List l=  new ArrayList<String>(Misc.toList(array));
	l.add(0,new HtmlUtils.Selector("None specified",""));
	return l;
    }
    


    /**
     *  @return _more_
     *
     * @throws Exception _more_
     */
    public List<Role> getStandardRoles() throws Exception {
        List<Role> roles = getUserRoles();
        roles.add(0, Role.ROLE_GUEST);
        roles.add(0, Role.ROLE_ANONYMOUS);
        roles.add(0, Role.ROLE_NONE);
        roles.add(0, Role.ROLE_ANY);
        roles.add(0, Role.ROLE_USER);

        return roles;
    }




    /**
     * _more_
     *
     * @param request the request
     * @param user The user
     * @param what _more_
     * @param extra _more_
     *
     * @throws Exception On badness
     */
    private void addActivity(Request request, User user, String what,
                             String extra)
	throws Exception {
        getDatabaseManager().executeInsert(Tables.USER_ACTIVITY.INSERT,
                                           new Object[] { user.getId(),
					       new Date(), what, extra, request.getIp() });
    }





    /**
     * _more_
     *
     * @param request the request
     *
     * @return The result
     *
     * @throws Exception On badness
     */
    public Result processActivityLog(Request request) throws Exception {
        StringBuffer sb   = new StringBuffer();

        User         user = findUser(request.getString(ARG_USER_ID, ""));

        if (user == null) {
            sb.append(messageError("Could not find user"));
        } else {
            sb.append(getUserActivities(request, user));
        }

        return getAdmin().makeResult(request, "User Log", sb);
    }



    /**
     * _more_
     *
     * @param request the request
     * @param theUser The user
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    private String getUserActivities(Request request, User theUser)
	throws Exception {
        StringBuffer sb          = new StringBuffer();
        Clause       clause      = null;
        String       limitString = "";
        if (theUser != null) {
            clause = Clause.eq(Tables.USER_ACTIVITY.COL_USER_ID,
                               theUser.getId());
        } else {
            limitString = getDatabaseManager().getLimitString(0,
							      request.get(ARG_LIMIT, 100));
        }


        Statement statement =
            getDatabaseManager().select(Tables.USER_ACTIVITY.COLUMNS,
                                        Tables.USER_ACTIVITY.NAME, clause,
                                        " order by "
                                        + Tables.USER_ACTIVITY.COL_DATE
                                        + " desc " + limitString);

        SqlUtil.Iterator iter = getDatabaseManager().getIterator(statement);
        ResultSet        results;
        HU.titleSectionOpen(sb, "User Log");
        if (theUser != null) {
            getWikiManager().makeCallout(sb, request,
                                         "<b>" + "User: "
                                         + theUser.getLabel() + "</b>");

        }
        sb.append(HU.vspace());
        sb.append(HU.open(HU.TAG_TABLE));
        sb.append(HU.row(HU.cols(((theUser == null)
				  ? HU.b(msg("User"))
				  : ""), HU.b(msg("Activity")),
				 HU.b(msg("Date")),
				 HU.b(msg("IP Address")),
				 HU.b(msg("Note")))));

        int cnt = 0;
        while ((results = iter.getNext()) != null) {
            int    col      = 1;
            String userId   = results.getString(col++);
            String firstCol = "";
            if (theUser == null) {
                User user = findUser(userId);
                if (user == null) {
                    firstCol = "No user:" + userId;
                } else {
                    firstCol =
                        HU.href(
				request.makeUrl(
						getRepositoryBase().URL_USER_ACTIVITY,
						ARG_USER_ID,
						user.getId()), HU.getIconImage(
									       getRepository().getIconUrl(ICON_LOG),
									       "title", "View user log") + HU.SPACE
				+ user.getLabel());
                }

            }
            Date   dttm  = getDatabaseManager().getDate(results, col++);
            String what  = results.getString(col++);
            String extra = results.getString(col++);
            String ip    = results.getString(col++);
            sb.append(HU.row(HU.cols(firstCol, what,
				     getDateHandler().formatDate(dttm), ip,
				     extra), HU.cssClass("ramadda-user-activity")));

            cnt++;
        }
        sb.append(HU.close(HU.TAG_TABLE));
        if (cnt == 0) {
            sb.append(msg("No activity"));
        }
        sb.append(HU.sectionClose());

        return sb.toString();
    }




    /**
     * _more_
     *
     * @param request the request
     *
     * @return The result
     *
     * @throws Exception On badness
     */
    public Result processSettingsForm(Request request) throws Exception {
        Result result = checkIfUserCanChangeSettings(request);
        if (result != null) {
            return result;
        }
	return makeUserSettingsForm(request,"");
    }



    /**
     * _more_
     *
     * @param request the request
     *
     * @return The result
     *
     * @throws Exception on badness
     */
    public Result processChangeSettings(Request request) throws Exception {
        Result result = checkIfUserCanChangeSettings(request);
        if (result != null) {
            return result;
        }
        if ( !request.exists(ARG_USER_CHANGE)) {
            return new Result(getRepositoryBase().URL_USER_SETTINGS.toString());
        }
        User         user = request.getUser();
        StringBuffer sb   = new StringBuffer();
	if(user.getIsGuest()) {
	    sb.append(messageWarning("Guest users cannot change settings"));
	    return new Result("",sb);
	}


	if(getAuthManager().verify(request,sb)) {
	    applyUserProperties(request, user, false);
	    sb.append(messageNote("Your settings have been changed"));		
	} 
	return  makeUserSettingsForm(request,sb.toString());
    }




    public Result  makeUserSettingsForm(Request request,String extra) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append(HU.sectionOpen(null, false));
        User user = request.getUser();
        request.appendMessage(sb);
	sb.append(extra);
        sb.append(request.uploadForm(getRepositoryBase().URL_USER_CHANGE_SETTINGS));
	String buttons = HU.submit("Change Settings", ARG_USER_CHANGE);
        sb.append(buttons);
	getAuthManager().addVerification(request,sb);
        makeUserForm(request, user, sb, false);
        sb.append(HU.formClose());
        sb.append(HU.vspace());
        String roles = user.getRolesAsString("<br>").trim();
        if (roles.length() == 0) {
            roles = "--none--";
        } else {
            sb.append(msgHeader("Your Roles"));
        }
        sb.append(HU.formTable());
        sb.append(formEntryTop(request, msgLabel("Roles"), roles));
        sb.append(HU.formTableClose());
        sb.append(HU.sectionClose());
        return makeResult(request, "User Settings", sb);
    }


    public Result processPasswordForm(Request request) throws Exception {
        Result result = checkIfUserCanChangeSettings(request);
        if (result != null) {
            return result;
        }
	return makeUserPasswordForm(request,"");
    }    

    public Result  makeUserPasswordForm(Request request,String extra) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append(HU.sectionOpen(null, false));
        User user = request.getUser();
        request.appendMessage(sb);
	sb.append(extra);
	if (user.canChangePassword()) {
            sb.append(request.formPost(getRepositoryBase().URL_USER_CHANGE_PASSWORD));
            makePasswordForm(request, user, sb);
            sb.append(HU.submit("Change Password", ARG_USER_CHANGE));
	    getAuthManager().addVerification(request,sb);
            sb.append(HU.formClose());
        } else {
            sb.append(messageWarning("You are not allowed to change the password"));
	}
        sb.append(HU.sectionClose());
        return makeResult(request, "User Password", sb);
    }

    


    /**
     * _more_
     *
     * @param request the request
     *
     * @return The result
     *
     * @throws Exception on badness
     */
    public Result checkIfUserCanChangeSettings(Request request)
	throws Exception {
        User user = request.getUser();
        if (user.getAnonymous()) {
            StringBuffer sb = new StringBuffer();
            sb.append(messageWarning(msg("You need to be logged in to change user settings")));
            sb.append(makeLoginForm(request));

            return addHeader(request, sb, "User Settings");
        }

        if ( !user.canEditSettings()) {
            StringBuffer sb = new StringBuffer();
            sb.append(messageWarning(msg("You cannot edit your settings")));
            return addHeader(request, sb,"User Settings");
        }

        return null;
    }




    public Result processChangePassword(Request request) throws Exception {
        Result result = checkIfUserCanChangeSettings(request);
        if (result != null) {
            return result;
        }
        if ( !request.exists(ARG_USER_CHANGE)) {
            return new Result(getRepositoryBase().URL_USER_PASSWORD.toString());
        }
        User         user = request.getUser();
        StringBuffer sb   = new StringBuffer();
	if(user.getIsGuest()) {
	    sb.append(messageWarning("Guest users cannot change their password"));
	    return new Result("",sb);
	}

	if(!getAuthManager().verify(request,sb)) {
	    return  makeUserPasswordForm(request,sb.toString());
	}

        boolean settingsOk = true;
        if (request.exists(ARG_USER_PASSWORD1)) {
            settingsOk = checkAndSetNewPassword(request, user);
            if ( !settingsOk) {
		//		sb.append("Incorrect passwords");
		sb.append(messageError(msg("Incorrect passwords")));
            } else {
		//                sb.append(messageNote("Your password has been changed"));
		changePassword(user);
                sb.append(messageNote("Your password has been changed"));		
                addActivity(request, request.getUser(), ACTIVITY_PASSWORD_CHANGE, "");
            }
	}
	if(true)
	    return makeUserPasswordForm(request,sb.toString());
	String formUrl = getRepository().getUrlPath(request,
						    getRepositoryBase().URL_USER_PASSWORD);
	return new Result(HU.url(formUrl, ARG_MESSAGE, sb.toString()));
    }





    /**
     * hash the given raw text password for storage into the database
     *
     * @param password raw text password
     *
     * @return hashed password
     */
    public String hashPassword_oldoldway(String password) {
        if (getRepository().getProperty(PROP_PASSWORD_OLDMD5, false)) {
            return RepositoryUtil.hashPasswordForOldMD5(password);
        } else {
            return RepositoryUtil.hashPassword(password);
        }
    }



    /**
     * hash the given raw text password for storage into the database
     *
     * @param password raw text password
     *
     * @return hashed password
     */
    private String hashPassword_oldway(String password) {
        //See, e.g. http://www.jasypt.org/howtoencryptuserpasswords.html
        try {
            //having a single salt repository wide isn't a great way to do this
            //It really should be a per user/password salt that gets stored in the db as well
            if (salt1.length() > 0) {
                password = salt1 + password;
            }
            int hashIterations =
                getRepository().getProperty(PROP_PASSWORD_ITERATIONS, 1);
            byte[] bytes = password.getBytes("UTF-8");
            for (int i = 0; i < hashIterations; i++) {
                bytes = doHashPassword(bytes);
            }
            if (salt2.length() > 0) {
                byte[] prefix   = salt2.getBytes("UTF-8");
                byte[] newBytes = new byte[prefix.length + bytes.length];
                for (int i = 0; i < prefix.length; i++) {
                    newBytes[i] = prefix[i];
                }
                for (int i = 0; i < bytes.length; i++) {
                    newBytes[prefix.length + i] = bytes[i];
                }
                bytes = newBytes;
            }
            String result = Utils.encodeBase64Bytes(bytes);

            return result.trim();
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }


    /**
     * _more_
     *
     * @param bytes _more_
     *
     * @return _more_
     */
    private byte[] doHashPassword(byte[] bytes) {
        try {
            String digest = getRepository().getProperty(PROP_PASSWORD_DIGEST,
							"SHA-512");
            MessageDigest md = MessageDigest.getInstance(digest);
            md.update(bytes);

            return md.digest();
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    public boolean isRecaptchaEnabled() {
	String siteKey = getRepository().getProperty(PROP_RECAPTCHA_SITEKEY,null);
	String secretKey = getRepository().getProperty(PROP_RECAPTCHA_SECRETKEY,null);	
	if(stringDefined(siteKey) && stringDefined(secretKey)) {
	    return true;
	}
	return false;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param response _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean isHuman(Request request, Appendable response)
	throws Exception {
	String siteKey = getRepository().getProperty(PROP_RECAPTCHA_SITEKEY,null);
	String secretKey = getRepository().getProperty(PROP_RECAPTCHA_SECRETKEY,null);	
	if(stringDefined(siteKey) && stringDefined(secretKey)) {
	    String recaptchaResponse = request.getString("g-recaptcha-response",null);
	    if(recaptchaResponse==null) return false;
	    String url = HU.url("https://www.google.com/recaptcha/api/siteverify","secret",secretKey,"response",recaptchaResponse);
	    String json = IO.readUrl(new URL(url));
            JSONObject  obj   = new JSONObject(json);
	    if(!obj.getBoolean("success")) {
                response.append(HU.center(messageWarning("Sorry, you were not verified to be a human")));
		return false;
	    } else {
		return true;
	    }
	}


        makeHumanAnswers();
        int idx    = request.get(ARG_HUMAN_QUESTION, 0);
        int answer = request.get(ARG_HUMAN_ANSWER, -111111);
        if ((idx < 0) || (idx >= ANSWERS.size())) {
            response.append("Bad answer");

            return false;
        } else {
            if (ANSWERS.get(idx).intValue() != answer) {
                response.append(
				"Sorry, but you got the answer wrong. Are you a human?<br>");

                return false;
            }
        }

        return true;

    }

    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     * @param formInfo _more_
     *
     * @throws Exception _more_
     */
    public void makeHumanForm(Request request, Appendable sb,
                              FormInfo formInfo)
	throws Exception {
	if(isRecaptchaEnabled()) {
	    sb.append("<script src='https://www.google.com/recaptcha/api.js' async defer></script>");
	    sb.append(formEntry(request,"",
				HU.div("",HU.attrs("class","g-recaptcha","data-sitekey","6Ld7zsgSAAAAABXOc291vy9MxoxG2D2Xuc1ONF4a"))));
	    return;
	}



        makeHumanAnswers();

        int idx = (int) (Math.random() * QUESTIONS.size());
        if (idx >= QUESTIONS.size()) {
            idx = QUESTIONS.size() - 1;
        }

        String image =
            HU.img(getRepository().getUrlBase()
		   + "/user/humanquestion/image.gif?human_question="
		   + idx);

        formInfo.addRequiredValidation("Human answer", ARG_HUMAN_ANSWER);
	sb.append(formEntry(request,
			    msgLabel("Please verify that you are human"),
			    image
			    + HU.input(ARG_HUMAN_ANSWER, "",
				       HU.id(ARG_HUMAN_ANSWER)
				       + HU.SIZE_5)));
	sb.append(HU.hidden(ARG_HUMAN_QUESTION, idx));
    }

    /**
     * _more_
     */
    private void makeHumanAnswers() {
        if (ANSWERS == null) {
            List<String>  questions = new ArrayList<String>();
            List<Integer> answers   = new ArrayList<Integer>();
            for (int i = 0; i < 1000; i++) {
                int v1 = (int) (Math.random() * 12);
                int v2 = (int) (Math.random() * 12);
                questions.add(v1 + "+" + v2);
                answers.add(Integer.valueOf(v1 + v2));
            }
            ANSWERS   = answers;
            QUESTIONS = questions;
        }
    }

}
