/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.admin;


import org.ramadda.repository.*;

import org.ramadda.repository.auth.*;
import org.ramadda.repository.database.*;

import org.ramadda.repository.harvester.*;

import org.ramadda.repository.output.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;
import org.ramadda.util.seesv.Seesv;

import org.ramadda.util.JQuery;
import org.ramadda.util.TTLCache;
import org.ramadda.util.Utils;
import org.ramadda.util.WikiUtil;

import org.ramadda.util.sql.Clause;

import org.ramadda.util.sql.SqlUtil;


import org.w3c.dom.*;

import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.StringUtil;



import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.lang.management.*;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import java.text.DecimalFormat;

import java.text.SimpleDateFormat;
import java.util.function.Consumer;
import java.util.function.BiConsumer;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;


import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;



/**
 * Class Admin
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
@SuppressWarnings("unchecked")
public class Admin extends RepositoryManager {

    /**  */
    private static boolean debugInitialization = false;

    /** _more_ */
    public static final String ACTION_SHUTDOWN = "action.shutdown";

    /**  */
    public static final String ACTION_LISTMISSING = "action.listmissing";

    public static final String ACTION_LISTORPHANS = "action.listorphans";


    /** _more_ */
    public static final String ACTION_CLEARCACHE = "action.clearcache";

    /** _more_ */
    public static final String ACTION_NEWDB = "action.newdb";

    /** _more_ */
    public static final String ACTION_DUMPDB = "action.dumpb";


    /**  */
    public static final String ACTION_REINDEX = "action.reindex";

    /** _more_ */
    public static final String ACTION_CHANGEPATHS = "action.changepaths";

    /** _more_ */
    public static final String ARG_CHANGEPATHS_CONFIRM = "changepathsconfirm";

    /** _more_ */
    public static final String ARG_CHANGEPATHS_PATTERN = "changepathspattern";

    /** _more_ */
    public static final String ARG_CHANGEPATHS_TO = "changepathsto";


    /** _more_ */
    public static final String ARG_REPLACE_PATTERN = "replacepattern";

    public static final String ARG_DELETE_INDEX="deleteindex";

    /** _more_ */
    public static final String ARG_REPLACE_WITH = "replacewith";


    /** _more_ */
    public static final String ARG_ADMIN_ADMINCREATED = "admin.admincreated";


    /** _more_ */
    public static final String ARG_ADMIN_INSTALLCOMPLETE =
        "admin.installcomplete";

    /** _more_ */
    public static final String ARG_ADMIN_INSTALLNOTICESHOWN =
        "admin.installnoticeshown";

    /** _more_ */
    public static final String ARG_ADMIN_INSTALLPLUGIN =
        "admin.installplugin";

    /** _more_ */
    public static final String ARG_ADMIN_LICENSEREAD = "admin.licenseread";



    /** _more_ */
    public RequestUrl URL_ADMIN_LOCAL = new RequestUrl(this, "/admin/local",
                                            "Local Repositories");


    /** _more_ */
    public RequestUrl URL_ADMIN_SQL = new RequestUrl(this, "/admin/sql", "Database");



    /** _more_ */
    public RequestUrl URL_ADMIN_MAINTENANCE = new RequestUrl(this,
                                              "/admin/maintenance",
                                              "Maintenance");

    /** _more_ */
    public RequestUrl URL_ADMIN_SNAPSHOTS = new RequestUrl(this,
                                                "/admin/snapshots",
                                                "Snapshots");



    /** _more_ */
    public RequestUrl URL_ADMIN_STARTSTOP = new RequestUrl(this,
                                                "/admin/startstop",
                                                "Start/Stop");


    /** _more_ */
    public RequestUrl URL_ADMIN_SETTINGS = new RequestUrl(this,
                                               "/admin/settings", "Settings");

    /** _more_ */
    public RequestUrl URL_ADMIN_MONITORS = new RequestUrl(this,
                                               "/admin/monitors", "Monitors");    


    /** _more_ */
    public RequestUrl URL_ADMIN_PLUGIN_UPLOAD = new RequestUrl(this,
                                                    "/admin/plugin/upload",
                                                    "Upload Plugin");

    /** _more_ */
    public RequestUrl URL_ADMIN_SETTINGS_DO = new RequestUrl(this,
                                                  "/admin/settings/do",
                                                  "Settings");

    /** _more_ */
    public RequestUrl URL_ADMIN_TABLES = new RequestUrl(this,
                                             "/admin/tables", "View Schema");


    /** _more_ */
    public RequestUrl URL_ADMIN_DUMPDB = new RequestUrl(this,
                                             "/admin/dumpdb",
                                             "Export Database");

    /** _more_ */
    public RequestUrl URL_ADMIN_STATS = new RequestUrl(this, "/admin/stats",
                                            "System");

    /** _more_ */
    public RequestUrl URL_ADMIN_ACCESS = new RequestUrl(this,
                                             "/admin/access", "Permissions");


    /** _more_ */
    public RequestUrl URL_ADMIN_LOG = new RequestUrl(this, "/admin/log",
                                          "Logs");

    /** _more_ */
    public RequestUrl URL_ADMIN_STACK = new RequestUrl(this, "/admin/stack",
                                            "Stack");


    /** _more_ */
    private List<RequestUrl> adminUrls = RequestUrl.toList(new RequestUrl[] {
	    URL_ADMIN_SETTINGS, getRepositoryBase().URL_USER_LIST,
	    URL_ADMIN_STATS, URL_ADMIN_ACCESS,
	    getHarvesterManager().URL_HARVESTERS_LIST,
	    URL_ADMIN_MONITORS,	
	    getRegistryManager().URL_REGISTRY_REMOTESERVERS,
	    /*URL_ADMIN_STARTSTOP,*/
	    /*URL_ADMIN_TABLES, */
	    URL_ADMIN_LOG, URL_ADMIN_STACK, URL_ADMIN_SNAPSHOTS, URL_ADMIN_MAINTENANCE
	});

    private List<RequestUrl> sqlUrls = RequestUrl.toList(new RequestUrl[] {
	    URL_ADMIN_SQL,URL_ADMIN_TABLES,URL_ADMIN_DUMPDB
	});


    /** _more_ */
    public static final String BLOCK_SITE = "block.site";

    /** _more_ */
    public static final String BLOCK_ACCESS = "block.access";

    /** _more_ */
    public static final String BLOCK_DISPLAY = "block.display";



    /** _more_ */
    int cleanupTS = 0;

    /** _more_ */
    boolean runningCleanup = false;

    /** _more_ */
    StringBuffer cleanupStatus = new StringBuffer();

    /** _more_ */
    private List<AdminHandler> adminHandlers = new ArrayList<AdminHandler>();

    /** _more_ */
    private Hashtable<String, AdminHandler> adminHandlerMap =
        new Hashtable<String, AdminHandler>();

    /** _more_ */
    private Boolean installationComplete;

    /** _more_ */
    private boolean isRegistered = true;

    /** _more_ */
    private int numberUsers = 5;

    /** _more_ */
    private String orgType = "";

    /** _more_ */
    private String regId = "";


    /**
     * _more_
     *
     * @param repository _more_
     */
    public Admin(Repository repository) {
        super(repository);
    }


    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public void doFinalInitialization() throws Exception {
        getRepository().getInstallPassword();

        if (getRepository().getLocalProperty(PROP_ADMIN_INCLUDESQL, false)) {
            adminUrls.add(URL_ADMIN_SQL);
        }

        if (getRepository().isMaster()) {
            int idx =
                adminUrls.indexOf(getHarvesterManager().URL_HARVESTERS_LIST);
            adminUrls.add(idx + 1, URL_ADMIN_LOCAL);
        }

	/*
        Misc.run(new Runnable() {
            public void run() {
                while (true) {
                    printMemory();
                    Misc.sleepSeconds(5);
                }
            }
        });
	*/
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getInstallationComplete() {
        if (installationComplete == null) {
            installationComplete = Boolean.valueOf(
                getRepository().getDbProperty(
                    ARG_ADMIN_INSTALLCOMPLETE, false));
        }

        return installationComplete.booleanValue();
    }



    /**
     * _more_
     *
     * @param v _more_
     *
     * @throws Exception _more_
     */
    public void setInstallationComplete(boolean v) throws Exception {
        installationComplete = Boolean.valueOf(v);
        getRepository().writeGlobal(Admin.ARG_ADMIN_INSTALLCOMPLETE, "" + v);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isRegistered() {
        //        return true;
        return isRegistered;
    }


    /**
     * _more_
     */
    public void checkRegistration() {
        isRegistered = false;
        numberUsers  = 5;
        orgType      = "";
        regId        = "";
        try {
            String registrationKey =
                getRepository().getProperty(PROP_REGISTER_KEY, "");
            if ( !Utils.stringDefined(registrationKey)) {
                //No key defined
                return;
            }
            List<String> toks = Utils.split(registrationKey, ":");
            //id:keyword:version:date:orgtype:users:                                                               
            if (toks.size() < 6) {
                System.err.println(
                    "Repository.checkRegistration: incorrect registration key:  "
                    + registrationKey);

                return;
            }
            int idx = 0;
            regId = toks.get(idx++);
            String key = Utils.unobfuscate(toks.get(idx++), true);
            double version =
                Double.parseDouble(Utils.unobfuscate(toks.get(idx++), true));
            String date = Utils.unobfuscate(toks.get(idx++), true);
            isRegistered = key.trim().equals("buenobueno");
            if (isRegistered) {
                orgType = Utils.unobfuscate(toks.get(idx++), true);
                numberUsers =  Integer.parseInt(Utils.unobfuscate(toks.get(idx++),
                        true));
            }
        } catch (Exception exc) {
            System.err.println("Repository.checkRegistration: error:" + exc);
            exc.printStackTrace();

            return;
        }
    }


    /**
     * _more_
     *
     * @param id _more_
     *
     * @return _more_
     */
    public AdminHandler getAdminHandler(String id) {
        return adminHandlerMap.get(id);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List<AdminHandler> getAdminHandlers() {
        return adminHandlers;
    }

    /**
     * _more_
     *
     * @param adminHandler _more_
     */
    public void addAdminHandler(AdminHandler adminHandler) {
        if (adminHandlers.contains(adminHandler)) {
            return;
        }
        if (adminHandlerMap.get(adminHandler.getId()) != null) {
            return;
        }
        adminHandlers.add(adminHandler);
        adminHandlerMap.put(adminHandler.getId(), adminHandler);
        List<RequestUrl> urls = adminHandler.getAdminUrls();
        if (urls != null) {
            adminUrls.addAll(urls);
        }
    }


    /**  */
    private StringBuilder debugSB = new StringBuilder();

    /**
     *
     * @param msg _more_
     */
    private void debugInit(String msg) {
        if (debugInitialization) {
            debugSB.append(msg);
            debugSB.append("\n");
        }
    }

    /**
     *  we have this here in memory as the using the db to store this is randomly giving bad results
     */
    private Hashtable installState = new Hashtable();

    /**
     * _more_
     *
     * @param what _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private boolean haveDoneInstallStep(String what) throws Exception {
        Object tmp = installState.get(what);
        if (Misc.equals(tmp, "true")) {
            return true;
        }
        boolean haveDone = getRepository().getDbProperty(what, false);
        debugInit("\thaveDone:" + what + " " + haveDone);

        return haveDone;
    }

    /**
     * _more_
     *
     * @param what _more_
     *
     * @throws Exception _more_
     */
    private void installStep(String what) throws Exception {
        debugInit("\tinstallStep:" + what);
        getRepository().writeGlobal(what, "true");
        installState.put(what, "true");
    }

    /**
     * _more_
     *
     * @param what _more_
     *
     * @throws Exception _more_
     */
    private void undoInstallStep(String... what) throws Exception {
        for (String w : what) {
            debugInit("\tundo:" + w);
            installState.put(what, "false");
            getRepository().writeGlobal(w, "false");
        }
    }





    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getLicenseForm() throws Exception {
        StringBuffer sb = new StringBuffer();
        String license =
            getStorageManager().readSystemResource(
                "/org/ramadda/repository/resources/ramadda_license.txt");

        license = license.replace("(C)", "&copy;");
        license = license.replace("(c)", "&copy;");
        sb.append(HU.textArea("", license, 10, 120));
        sb.append(HU.br());

        sb.append(HU.open(HU.TAG_DIV,
                                 HU.cssClass("registration-agree")));
        sb.append(
            HU.labeledCheckbox(
                ARG_AGREE, "true", false,
                "I agree to the above license and conditions of use for the RAMADDA software"));
        sb.append(HU.close(HU.TAG_DIV));

        return sb.toString();
    }


    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    private String note(String s) {
        return "<div class='ramadda-box-outer'><div class='ramadda-block ramadda-box ramadda-box-yellow  '>"
               + s + "</div></div>";
        //        return "<div class=\"ramadda-box-yellow\">" + s + "</div>\n";
    }




    /**
     *
     * @param plugin _more_
      * @return _more_
     */
    private String makePluginID(String plugin) {
        plugin = "plugin." + Utils.makeID(plugin);
        plugin = plugin.replace("org_ramadda_repository_resources_plugins_",
                                "").replace("_jar", "");

        return plugin;
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
    public synchronized Result doInstall(Request request) throws Exception {

        debugInit("doInitialization");
        String installPassword = getRepository().getInstallPassword();
        if ( !Utils.stringDefined(installPassword)) {
            return new Result(
                "Install Error",
                new StringBuffer(
                    getPageHandler().showDialogError(
                        "Error: No installation password has been specified.<br>You need to add a some_name.properties file to your RAMADDA home directory ("
                        + getStorageManager().getRepositoryDir()
                        + ") with the following set and then restart your server<br>&nbsp;<br><pre>\n"
                        + PROP_INSTALL_PASSWORD
                        + "=some password\n\n</pre>")));
        }

        String givenPassword = request.getString(PROP_INSTALL_PASSWORD,
                                   "").trim();
        StringBuffer sb    = new StringBuffer();
        String       title = "";

        if (request.get(ARG_AGREE, false)) {
            installStep(ARG_ADMIN_LICENSEREAD);
        }

        if (request.exists(ARG_ADMIN_INSTALLNOTICESHOWN)) {
            installStep(ARG_ADMIN_INSTALLNOTICESHOWN);
        }

        boolean firstTime =
            !haveDoneInstallStep(ARG_ADMIN_INSTALLNOTICESHOWN);


        //Always check the password
        if ( !firstTime) {
            if ( !Utils.passwordOK(installPassword, givenPassword)) {
                debugInit("\nBad password:" + givenPassword);
                System.err.println("RAMADDA: bad install password:"
                                   + givenPassword + " installPassword:"
                                   + installPassword);
                System.err.println("RAMADDA: request:" + request);
                undoInstallStep(ARG_ADMIN_LICENSEREAD,
                                ARG_ADMIN_INSTALLNOTICESHOWN,
                                ARG_ADMIN_ADMINCREATED);
                sb.append(
                    getPageHandler().showDialogError(
                        "Error: Incorrect installation password"));
                firstTime = true;
            } else {
                sb.append(HU.hidden(PROP_INSTALL_PASSWORD,
                                           givenPassword));
            }
        }

        if (firstTime) {
            title = "Installation";
            String msg =
                "Listed below is the home directory and database information.";
            msg += " Consult the documentation to:<ul style='margin-bottom:0px;'>";
            msg += "<li> Set the <a target=\"_help\" href=\"https://geodesystems.com/repository/userguide/installing.html#home\">home directory</a> ";
            msg += "<li> Specify the <a target=_help href='https://geodesystems.com/repository/userguide/database.html'>database</a> ";
            msg += "<li> Configure <a target=_help href='https://geodesystems.com/repository/userguide/installing.html#ssl'>SSL</a> ";
            msg += "</ul>";
            msg += "If you change any of these settings be sure to restart your RAMADDA";
            sb.append(note(msg));
            sb.append(HU.formTable());

            sb.append(
                HU.formEntry(
                    "Home Directory:",
                    getStorageManager().getRepositoryDir().toString()));
            getDatabaseManager().addInfo(sb);
            sb.append(
                HU.colspan(
                    note("Since this configuration is web-based we use the install password to verify your identity."),
                    2));
            sb.append(
                HU.formEntry(
                    msgLabel("Install Password"),
                    HU.input(PROP_INSTALL_PASSWORD, "") + " "
                    + "Specified in "
                    + getStorageManager().getRepositoryDir().toString()
                    + "/install.propertes"));
            sb.append(HU.formTableClose());
            sb.append(HU.submit("Next", ARG_ADMIN_INSTALLNOTICESHOWN));
        } else if ( !haveDoneInstallStep(ARG_ADMIN_LICENSEREAD)) {
            title = "License and Conditions of Use";
            sb.append(getLicenseForm());
            sb.append(HU.br());
            sb.append(HU.submit("Next"));
        } else if ( !haveDoneInstallStep(ARG_ADMIN_ADMINCREATED)) {
            title = "Configuration";
            String       id          = "admin";
            String       name        = "Administrator";

            boolean      triedOnce   = false;
            StringBuffer errorBuffer = new StringBuffer();
            if (request.exists(UserManager.ARG_USER_ID)) {
                triedOnce = true;
                id = request.getString(UserManager.ARG_USER_ID, "").trim();
                name = request.getString(UserManager.ARG_USER_NAME,
                                         "").trim();
                String password1 =
                    request.getString(UserManager.ARG_USER_PASSWORD1,
                                      "").trim();
                String password2 =
                    request.getString(UserManager.ARG_USER_PASSWORD2,
                                      "").trim();
                boolean okToAdd = true;
                if (id.length() == 0) {
                    okToAdd = false;
                    errorBuffer.append(HU.space(2));
                    errorBuffer.append(msg("Please enter an ID"));
                    errorBuffer.append(HU.br());
                }

                if ((password1.length() == 0)
                        || !password1.equals(password2)) {
                    okToAdd = false;
                    errorBuffer.append(HU.space(2));
                    errorBuffer.append(msg("Invalid password"));
                    errorBuffer.append(HU.br());
                }


                if (okToAdd) {
                    User user = new User(id, User.STATUS_ACTIVE,name,
					 request.getString(
							   UserManager.ARG_USER_EMAIL,
							   "").trim(),
					 UserManager.DEFAULT_INSTITUTION,
					 UserManager.DEFAULT_COUNTRY,
					 "",
					 "",
					 getUserManager().hashPassword(
								       password1), "", true, "", "",
				    false, new Date(),null);
                    getUserManager().makeOrUpdateUser(user, false);
                    installStep(ARG_ADMIN_ADMINCREATED);
                    installStep(ARG_ADMIN_INSTALLCOMPLETE);
                    //Make  sure to clear this so it gets read again
                    installationComplete = null;

                    String[] propArgs = new String[] { PROP_REPOSITORY_NAME,
                            PROP_HOSTNAME, PROP_PORT, PROP_REPOSITORY_NAME,
                            PROP_REPOSITORY_DESCRIPTION };


                    for (String propArg : propArgs) {
                        if (request.defined(propArg)) {
                            getRepository().writeGlobal(propArg,
                                    request.getString(propArg, "").trim());
                        }
                    }

                    if (request.defined(UserManager.ARG_USER_EMAIL)) {
                        getRepository().writeGlobal(PROP_ADMIN_EMAIL,
                                request.getString(UserManager.ARG_USER_EMAIL,
                                    ""));
                    }

                    //NOT NOW:
                    //                    getRegistryManager().applyInstallForm(request);

                    sb.append(
                        note("Initial configuration process is complete. Please login."));

                    Entry topEntry = request.getRootEntry();
                    topEntry.setName(request.getString(PROP_REPOSITORY_NAME,
                            topEntry.getName()));
                    String description = null;
                    File initDescFile =
                        new File(
                            IOUtil.joinDir(
                                getStorageManager().getRepositoryDir(),
                                "initdescription.txt"));
                    if (initDescFile.exists()) {
                        FileInputStream fis =
                            new FileInputStream(initDescFile);
                        description = IOUtil.readContents(fis);
                        IO.close(fis);
                    }

                    //Make sure we do this now before we do the final init entries
                    boolean didPlugin = false;
                    for (String plugin : PluginManager.PLUGINS) {
                        if (request.get(makePluginID(plugin), false)) {
                            didPlugin = true;
                            getRepository().getPluginManager().installPlugin(
                                plugin);
                        }
                    }
                    if (didPlugin) {
                        getRepository().loadPluginResources();
                    }

		    //Load the metadata
		    getMetadataManager().loadMetadataHandlers(getPluginManager());

                    if (description == null) {
			String resourcePath = "/org/ramadda/repository/resources/install/initdescription.txt";
			resourcePath = getRepository().getProperty("ramadda.install.initdescription",resourcePath);
                        description = getRepository().getResource(resourcePath);
                    }

                    description = description.replace("${topid}",
                            topEntry.getId());
                    description = description.replace("${root}",
                            getRepository().getUrlBase());
                    topEntry.setDescription(description);
                    getEntryManager().updateEntry(null, topEntry);

		    


                    addInitEntries(user);
                    sb.append(getUserManager().makeLoginForm(request));
                    if (errorBuffer.length() > 0) {
                        sb.append(
                            getPageHandler().showDialogError(
                                msg("Error") + "<br>" + errorBuffer));
                    }

                    StringBuilder html = new StringBuilder();
                    getPageHandler().sectionOpen(request, html,
                            "RAMADDA Install", false);
                    html.append(sb);
                    getPageHandler().sectionClose(request, html);

                    return new Result("Repository Initialization", html);
                }
            }

            if (errorBuffer.length() > 0) {
                sb.append(getPageHandler().showDialogError(msg("Error")
                        + "<br>" + errorBuffer));
            }
            sb.append(
                note("Please enter the following information. This information is used to configure your RAMADDA server and is not sent anywhere."));
            String required1 =
                " <span class=\"ramadda-required-field\">* required</span>";
            String required2 =
                " <span class=\"ramadda-required-field\">*</span>";
            sb.append(HU.formTable());
            sb.append(
                HU.row(
                    HU.colspan(msgHeader("Administrator Login"), 2)));
            sb.append(
                HU.formEntry(
                    msgLabel("ID"),
                    HU.input(UserManager.ARG_USER_ID, id)
                    + required1));
            sb.append(
                HU.formEntry(
                    msgLabel("Name"),
                    HU.input(UserManager.ARG_USER_NAME, name)));
            sb.append(
                HU.formEntry(
                    msgLabel("Email"),
                    HU.input(
                        UserManager.ARG_USER_EMAIL,
                        request.getString(UserManager.ARG_USER_EMAIL, ""))));
            sb.append(
                HU.formEntry(
                    msgLabel("Password"),
                    HU.password(UserManager.ARG_USER_PASSWORD1)
                    + required2));
            sb.append(
                HU.formEntry(
                    msgLabel("Password Again"),
                    HU.password(UserManager.ARG_USER_PASSWORD2)
                    + required2));

            sb.append(
                HU.row(
                    HU.colspan(msgHeader("Server Information"), 2)));
            String hostname = "";
            String port     = "";
            if (request.getHttpServletRequest() != null) {
                hostname = request.getHttpServletRequest().getServerName();
                //Don't do this because the install can be running under https
                //and this port is the httpport
                //port = "" + request.getHttpServletRequest().getServerPort();
            }
            hostname = request.getString(PROP_HOSTNAME, hostname);
            port     = request.getString(PROP_PORT, port);

            sb.append(
                HU.formEntry(
                    msgLabel("Repository Name"),
                    HU.input(
                        PROP_REPOSITORY_NAME,
                        request.getString(
                            PROP_REPOSITORY_NAME,
                            getRepository().getProperty(
                                PROP_REPOSITORY_NAME,
                                "RAMADDA Repository")), HU.SIZE_60)));

            sb.append(
                HU.formEntry(
                    "",
                    "RAMADDA comes with a set of plugins that add functionality. You can install them now or later if you wish."));
            //TODO: read the plugins.xml file and offer more plugins
            //than the hard coded all plugin
            sb.append(HU.hidden("hascbx", "true"));
            boolean hascbx = request.get("hascbx", false);
            for (String plugin : PluginManager.PLUGINS) {
                String pluginName =
                    IO.stripExtension(IO.getFileTail(plugin));
                String  cbxName = makePluginID(plugin);
                boolean dflt    = !pluginName.equals("bioplugins");
                boolean value   = request.get(cbxName, hascbx
                        ? false
                        : dflt);
                sb.append(HU.formEntry("",
                        HU.labeledCheckbox(cbxName, "true", value,
                            "Install plugin: " + pluginName)));
            }
            sb.append(HU.formTableClose());
            sb.append(HU.br());
            sb.append(HU.submit("Initialize Server"));
        } else {
            //Should never get here
            title = "Error";
            sb.append(
                getPageHandler().showDialogError("Install is finished"));
        }


        StringBuffer finalSB = new StringBuffer();
        finalSB.append(request.formPost(getRepository().URL_INSTALL));
        getPageHandler().sectionOpen(request, finalSB, title, false);
        finalSB.append(sb);
        getPageHandler().sectionClose(request, finalSB);
        finalSB.append(HU.formClose());

        return new Result(msg(title), finalSB);



    }




    /**
     * _more_
     *
     * @param user _more_
     *
     * @throws Exception _more_
     */
    public void addInitEntries(User user) throws Exception {
        String initEntriesXml = null;
        File initFile =
            new File(IOUtil.joinDir(getStorageManager().getRepositoryDir(),
                                    "initentries.xml"));
        if (initFile.exists()) {
            FileInputStream fis = new FileInputStream(initFile);
            initEntriesXml = IOUtil.readContents(fis);
            IO.close(fis);
        }

        if (initEntriesXml == null) {
	    String resourcePath = "/org/ramadda/repository/resources/install/initentries.xml";
	    resourcePath = getRepository().getProperty("ramadda.install.initentries",resourcePath);
            initEntriesXml = getRepository().getResource(resourcePath);
        }
        Element root       = XmlUtil.getRoot(initEntriesXml);
        Request tmpRequest = getRepository().getRequest(user);
        //        System.err.println("entry xml");
        List<Entry> newEntries =
            getEntryManager().processEntryXml(tmpRequest, root, null,
                new Hashtable<String, File>(), new StringBuilder());
        //        System.err.println("after entry xml");
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
    private Result processShutdown(Request request) throws Exception {
        if ( !getRepository().getShutdownEnabled()) {
            throw new IllegalStateException("Shutdown not enabled");
        }

        Misc.runInABit(5000, new Runnable() {
            public void run() {
                getRepository().shutdown();
                System.err.println("RAMADDA: exiting");
                System.exit(0);
            }
        });

	return makeResult(request, "RAMADDA-Admin-Shutdown",
                          new StringBuffer("Shutting down in 5 seconds"));
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
    public Result adminPluginUpload(Request request) throws Exception {
        getAuthManager().ensureAuthToken(request);

        return getRepository().getPluginManager().adminPluginUpload(request);
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
    public Result adminDbStartStop(Request request) throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.append(header("Database Administration"));
        String what = request.getString(ARG_ADMIN_WHAT, "nothing");
        if (what.equals("shutdown")) {
            if ( !getDatabaseManager().hasConnection()) {
                sb.append("Not connected to database");
            } else {
                getRepository().getDatabaseManager().shutdown();
                sb.append("Database is shut down");
            }
        } else if (what.equals("restart")) {
            if (getDatabaseManager().hasConnection()) {
                sb.append("Already connected to database");
            } else {
                //TODO:                getRepository().getDatabaseManager().makeConnection();
                sb.append("Database is restarted");
            }
        }
        sb.append(HU.vspace());
        request.formPostWithAuthToken(sb, URL_ADMIN_STARTSTOP,
                                      " name=\"admin\"");

        if ( !getDatabaseManager().hasConnection()) {
            sb.append(HU.hidden(ARG_ADMIN_WHAT, "restart"));
            sb.append(HU.submit("Restart Database"));
        } else {
            sb.append(HU.hidden(ARG_ADMIN_WHAT, "shutdown"));
            sb.append(HU.submit("Shut Down Database"));
        }
        sb.append("</form>");

        return makeResult(request, "RAMADDA-Admin-DB", sb);

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
    public Result adminActions(Request request) throws Exception {
        StringBuffer sb = new StringBuffer();
        List<ApiMethod> apiMethods =
            getRepository().getApiManager().getApiMethods();
        sb.append(HU.formTable());
        sb.append(HU.row(HU.cols("Name", "Admin", "Actions")));
        for (ApiMethod apiMethod : apiMethods) {
            sb.append(HU.row(HU.cols(apiMethod.getName(),
                    "" + apiMethod.getMustBeAdmin(),
                    StringUtil.join(",", apiMethod.getActions()))));
        }
        sb.append(HU.formTableClose());

        return makeResult(request, "RAMADDA-Admin-Actions", sb);
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
    public Result adminDbTables(Request request) throws Exception {
        StringBuffer sb = new StringBuffer();
        getPageHandler().makeLinksHeader(request, sb, sqlUrls, "");
	//        sb.append(header("Database Tables"));
        sb.append(getDatabaseManager().getDbMetaData());
        return makeResult(request, "RAMADDA-Admin-DB", sb);
    }

    /** _more_ */
    private boolean amDumpingDb = false;

    /**  */
    private boolean amReindexing = false;

    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result adminDbDump(Request request) throws Exception {
        //Only do one at a time
	StringBuffer sb = new StringBuffer();
        if (amDumpingDb) {
            sb.append(messageWarning("Currently exporting the database"));
            return makeResult(request, msg("RAMADDA-Admin-DB Export"), sb);
        }

	boolean doIt = request.exists(ACTION_DUMPDB);

	if(doIt) {
	    doIt  =getAuthManager().verify(request,sb,true);
	}

	if(!doIt) {
	    sb.append(request.formPost(URL_ADMIN_MAINTENANCE));
	    sb.append(messageNote("You can write out the database for backup or transfer to a new database"));
	    sb.append(HU.submit("Export the database", ACTION_DUMPDB));
	    getAuthManager().addVerification(request,sb,null,true);
	    sb.append(HU.formClose());
            return makeResult(request, msg("RAMADDA-Admin-DB Export"), sb);
	}	    


        ActionManager.Action action = new ActionManager.Action() {
            public void run(Object actionId) throws Exception {
                dumpDatabase(actionId);
            }
        };
        String href = HU.href(request.makeUrl(URL_ADMIN_MAINTENANCE),
                                     "Continue");

        Result result = getActionManager().doAction(request, action,
                            "Exporting database", href);

        return result;
    }

    /**
     *
     * @param request _more_
     * @param all _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result adminReindex(final Request request)
            throws Exception {
        //Only do one at a time
        if (amReindexing) {
            StringBuffer sb = new StringBuffer(
                                  getPageHandler().showDialogWarning(
                                      "Currently reindexing"));

            return makeResult(request, msg("RAMADDA-Admin-Reindex"), sb);
        }


        ActionManager.Action action = new ActionManager.Action() {
            public void run(Object actionId) throws Exception {
                try {
                    getSearchManager().reindexLucene(request,actionId, request.getString(ARG_TYPE,null),
						     request.get(ARG_DELETE_INDEX,false));
                } catch (Exception exc) {
                    System.err.println("Error reindexing:" + exc);
                    throw exc;
                } finally {
                    amReindexing = false;
                }
            }
        };
        String href = HU.href(request.makeUrl(URL_ADMIN_MAINTENANCE),
                                     "Continue");

        Result result = getActionManager().doAction(request, action,
                            "Reindexing", href);

        return result;
    }



    /**
     * _more_
     *
     * @param actionId _more_
     *
     * @throws Exception _more_
     */
    private void dumpDatabase(Object actionId) throws Exception {
        amDumpingDb = true;
        String msg = "";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_HH_mm");
            File tmp = new File(getStorageManager().getBackupsDir() + "/"
                                + "dbdump." + sdf.format(new Date())
                                + ".rdb");
            FileOutputStream     fos = new FileOutputStream(tmp);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            getDatabaseManager().makeDatabaseCopy(bos, true, actionId);
            IO.close(bos);
            IO.close(fos);

            msg = "Database has been exported to: " + tmp;
            StringBuffer sb = new StringBuffer(
                                  getPageHandler().showDialogNote(
                                      "Database has been exported to:<br>"
                                      + tmp));
            //            return makeResult(request, msg("Database export"), sb);
        } finally {
            if (actionId != null) {
                getActionManager().setContinueHtml(actionId, msg);
            }
            amDumpingDb = false;
        }
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param title _more_
     * @param sb _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result makeResult(Request request, String title, Appendable sb)
            throws Exception {
        StringBuilder headerSB = new StringBuilder();
        getPageHandler().sectionOpen(request, headerSB, "Admin", false);
        getPageHandler().makeLinksHeader(request, headerSB, adminUrls, "");
        headerSB.append(sb);
        getPageHandler().sectionClose(request, headerSB);

        return new Result(title, headerSB);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void xaddHeader(Request request, Appendable sb) throws Exception {}




    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result adminHome(Request request) throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.append(header("Repository Administration"));
        sb.append("<ul>\n");
        sb.append("<li> ");
        sb.append(HU.href(request.makeUrl(URL_ADMIN_STARTSTOP),
                                 "Administer Database"));
        sb.append("<li> ");
        sb.append(HU.href(request.makeUrl(URL_ADMIN_TABLES),
                                 "Show Tables"));
        sb.append("<li> ");
        sb.append(HU.href(request.makeUrl(URL_ADMIN_STATS), "System"));
        sb.append("<li> ");
        sb.append(HU.href(request.makeUrl(URL_ADMIN_SQL),
                                 "Execute SQL"));
        sb.append("</ul>");

        return makeResult(request, "RAMADDA-Admin-DB", sb);

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
    public Result adminSettings(Request request) throws Exception {
	return adminSettings(request, "");
    }


    public Result adminSettings(Request request,String extraTop) throws Exception {


        StringBuilder sb = new StringBuilder();
	sb.append(extraTop);
        String size = HU.SIZE_60;
        sb.append(request.formPost(URL_ADMIN_SETTINGS_DO));
        sb.append(HU.sectionOpen(null, false));
        sb.append(HU.submit("Change Settings"));
	getAuthManager().addVerification(request,sb);
        sb.append(HU.vspace());
        StringBuffer csb = new StringBuffer();
        csb.append(HU.formTable());
        csb.append(
            HU.row(
                HU.colspan(msgHeader("Site Information"), 2)));
        String allSslCbx =
            HU.space(3)
            + HU.labeledCheckbox(
                PROP_ACCESS_ALLSSL, "true",
                formPropValue(request,PROP_ACCESS_ALLSSL, false),
		"Force all connections to be secure");

        String sslMsg =
            "Note: To enable ssl see the <a target=\"_help\" href=\"http://ramadda.org/repository/userguide/installing.html#ssl\">installation guide</a>";
        csb.append(
            HU.formEntryTop(
                msgLabel("SSL"),
                getPageHandler().showDialogNote(sslMsg) + HU.br()
                + allSslCbx));


	HU.formEntry(csb,
			    msgLabel("Hostname"),
			    HU.input(PROP_HOSTNAME,formPropValue(request,PROP_HOSTNAME,""), HU.SIZE_40));

	HU.formEntry(csb,
			    msgLabel("HTTP Port"),
			    HU.input(PROP_PORT, formPropValue(request,PROP_PORT,""), HU.SIZE_5));

        String cbx = HU.labeledCheckbox(
					       PROP_USE_FIXED_HOSTNAME, "true",
					       formPropValue(request, PROP_USE_FIXED_HOSTNAME, false),
					       "Use the fixed hostname:port in absolute URLs instead of the request's info");
	HU.formEntry(csb, msgLabel("Absolute URLs"),  cbx);

        //Force the creation of some of the managers
        getRepository().getMailManager();
        //        getRepository().getFtpManager();
        getRepository().getMapManager();


        for (RepositoryManager manager :
                getRepository().getRepositoryManagers()) {
            manager.addAdminSettings(request, csb);
        }


	HU.row(csb, HU.colspan(msgHeader("Extra Properties"), 2));
        csb.append(
            HU.formEntryTop(
                msgLabel("Properties"),
                HU.textArea(
                    PROP_PROPERTIES,
                    formPropValue(request,PROP_PROPERTIES,
				 "#add extra properties\n#name=value\n#ramadda.html.template.default=fixedmapheader\n\n"), 10, 80)));



        StringBuffer dsb = new StringBuffer();

        dsb.append(HU.formTable());
        dsb.append(
            HU.formEntry(
                msgLabel("Title"),
                HU.input(
                    PROP_REPOSITORY_NAME,
                    formPropValue(request, PROP_REPOSITORY_NAME, "RAMADDA Repository"), size)));
        dsb.append(
            HU.formEntry(
                msgLabel("Slug"),
                HU.input(
                    PROP_REPOSITORY_SLUG,
                    formPropValue(request, PROP_REPOSITORY_SLUG, ""), HU.SIZE_10) +HU.space(1) +
		msg("i.e., short name")));
        dsb.append(
            HU.formEntryTop(
                msgLabel("Description"),
                HU.textArea(
                    PROP_REPOSITORY_DESCRIPTION,
		    formPropValue(request,PROP_REPOSITORY_DESCRIPTION, ""), 5, 60)));

        dsb.append(
            HU.formEntryTop(
                msgLabel("Footer"),
                HU.textArea(
                    PROP_HTML_FOOTER,
		    formPropValue(request,PROP_HTML_FOOTER, ""), 5,
                    60)));

	HU.formEntry(dsb, msgLabel("Logo Image Location"),
                HU.input(PROP_LOGO_IMAGE,
			 formPropValue(request,PROP_LOGO_IMAGE, ""), size));
	HU.formEntry(dsb, msgLabel("Logo URL"),
		     HU.input(PROP_LOGO_URL,
			      formPropValue(request,PROP_LOGO_URL, ""), size));


        String systemMessage = getRepository().getSystemMessage();
	if(systemMessage == null)systemMessage="";
	HU.formEntry(dsb,
		     msgLabel("System Message"),
		     HU.textArea(PROP_SYSTEM_MESSAGE, request.getString(PROP_SYSTEM_MESSAGE,systemMessage),
				 5, 60));



        dsb.append(
            HU.formEntryTop(
                msgLabel("Ignore Page Styles"),
                HU.checkbox(
                    PROP_NOSTYLE, "true",
                    formPropValue(request,PROP_NOSTYLE, false))));

        dsb.append(
            HU.formEntryTop(
                msgLabel("Translations"),
                HU.textArea(PROP_ADMIN_PHRASES,
			    formPropValue(request, PROP_ADMIN_PHRASES,"#label=new label to use\n#e.g.: Foo=Bar"),
			    5, 60)));


	/*** not needed
        dsb.append(HU.formEntryTop(msgLabel("Google Maps Keys"), "<table><tr valign=top><td>"
                + HU.textArea(PROP_GOOGLEAPIKEYS, getRepository().getProperty(PROP_GOOGLEAPIKEYS, ""), 5, 80)
                + "</td><td>One per line:<br><i>host domain;apikey</i><br>e.g.:<i>www.yoursite.edu;google api key</i></table>"));
	****/


        StringBuffer asb = new StringBuffer();
        asb.append(HU.formTable());


        asb.append(HU.row(HU.colspan(msgHeader("Site Access"),
                2)));
        asb.append(
            HU.formEntry(
                "",
                HU.labeledCheckbox(
					  PROP_ACCESS_ADMINONLY, "true",
					  formPropValue(request, PROP_ACCESS_ADMINONLY, false),
					  "Only allows administrators to access the site")));
        asb.append(
            HU.formEntry(
                "",
                HU.labeledCheckbox(
                    PROP_ACCESS_REQUIRELOGIN, "true",
                    formPropValue(request,PROP_ACCESS_REQUIRELOGIN, false),
		    "Require login to access the site")));

        asb.append(
            HU.formEntry(
                "",
                HU.labeledCheckbox(
                    PROP_ACCESS_NOBOTS, "true",
                    formPropValue(request,PROP_ACCESS_NOBOTS, false),
		    "Disallow robots (except for Google)")));

        asb.append(
            HU.formEntry(
                "",
                HU.labeledCheckbox(
                    PROP_ACCESS_NOGOOGLEBOT, "true",
                    formPropValue(request,PROP_ACCESS_NOGOOGLEBOT, false),
		    "Disallow Googlebot")));
	


        asb.append(HU.colspan(msgHeader("Anonymous Uploads"), 2));
        asb.append(
            HU.formEntry(
                msgLabel("Max directory size"),
                HU.input(
                    PROP_UPLOAD_MAXSIZEGB,
                    formPropValue(request,PROP_UPLOAD_MAXSIZEGB,"10.0"),
		    HU.SIZE_10) + " (GBytes)"));


        asb.append(HU.colspan(msgHeader("Cache Size"), 2));
        asb.append(
            HU.formEntry(
                msgLabel("Size"),
                HU.input(
                    PROP_CACHE_MAXSIZEGB,
                    formPropValue(request,PROP_CACHE_MAXSIZEGB,"10.0"),
		    HU.SIZE_10) + " (GBytes)"));



        asb.append(HU.colspan(msgHeader("File Access"), 2));
        String fileWidget = HU.textArea(
                                PROP_LOCALFILEPATHS,
                                formPropValue(request, PROP_LOCALFILEPATHS, ""), 5, 40);
        String fileLabel =
            msg("Enter one server file system directory per line")
            + HU.br()
            + msg("Directories that RAMADDA is allowed to serve files from")
            + " " + "(e.g., from harvesters or the server file view entries)";
        asb.append(HU.formEntryTop(msgLabel("File system access"),
                                          "<table><tr valign=top><td>"
                                          + fileWidget + "</td><td>"
                                          + fileLabel
                                          + "</td></tr></table>"));









        for (AdminHandler adminHandler : adminHandlers) {
            adminHandler.addToAdminSettingsForm(BLOCK_SITE, csb);
            adminHandler.addToAdminSettingsForm(BLOCK_DISPLAY, dsb);
            adminHandler.addToAdminSettingsForm(BLOCK_ACCESS, asb);
        }
        csb.append(HU.formTableClose());
        dsb.append(HU.formTableClose());
        asb.append(HU.formTableClose());


        StringBuffer osb = new StringBuffer();
        osb.append(HU.formTable());


        StringBuffer     outputSB         = new StringBuffer();
        List<OutputType> types            = getRepository().getOutputTypes();
        String           lastCategoryName = null;
        for (OutputType type : types) {
            if ( !type.getForUser()) {
                System.out.println("not for user:" + type);
                continue;
            }
            boolean ok = getRepository().isOutputTypeOK(type);
            //            System.out.println("type:" + type +" " + ok);
            if ( !Misc.equals(lastCategoryName, type.getGroupName())) {
                if (lastCategoryName != null) {
                    outputSB.append("</div>\n");
                    outputSB.append(HU.vspace());
                }
                lastCategoryName = type.getGroupName();
                if (lastCategoryName != null) {
                    HU.div(outputSB, lastCategoryName,
                                  HU.cssClass(CSS_CLASS_HEADING_2));
                }
                outputSB.append("\n<div style=\"margin-left:20px\">");
            }
            outputSB.append(HU.labeledCheckbox("outputtype." + type.getId(),
						      "true", ok,type.getLabel()));
        }
        outputSB.append("</div>\n");
        String outputDiv = HU.div(outputSB.toString(),
                                         HU.cssClass("scrollablediv"));
        osb.append("\n");
        String doAllOutput = HU.labeledCheckbox("outputtype.all", "true",
						       false, "Use all");
        osb.append(HU.formEntryTop("", doAllOutput + outputDiv));
        osb.append("\n");
        StringBuffer handlerSB = new StringBuffer();
        List<OutputHandler> outputHandlers =
            getRepository().getOutputHandlers();
        for (OutputHandler outputHandler : outputHandlers) {
            outputHandler.addToSettingsForm(handlerSB);
        }

        String extra = handlerSB.toString();
        if (extra.length() > 0) {
            osb.append(tableSubHeader(msg("Output")));
            osb.append(extra);
        }

        osb.append(HU.formEntry("&nbsp;<p>", ""));
        osb.append(HU.formTableClose());


        List<String> titles = new ArrayList<String>();
        List<String> tabs   = new ArrayList<String>();

        titles.add("Site and Contact Information");
        tabs.add(csb.toString());
        titles.add("Access");
        tabs.add(asb.toString());
        titles.add("Display");
        tabs.add(dsb.toString());

	//        titles.add("Available Output Types");
	//        tabs.add(osb.toString());



        HU.makeAccordion(sb, titles, tabs, true, "ramadda-accordion",
                                null);
        sb.append(HU.formClose());
        sb.append(HU.sectionClose());

        return makeResult(request, msg("RAMADDA-Admin-Settings"), sb);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void addInfo(Request request, Appendable sb) throws Exception {
        /*
        if (isRegistered()) {
            sb.append(HU.formEntry("Registered", regId));
        }
        */
    }


    /**
     * _more_
     *
     * @param title _more_
     * @param contents _more_
     *
     * @return _more_
     */
    private String makeConfigBlock(String title, String contents) {
        return HU.makeShowHideBlock(
            msg(title),
            HU.div(contents, HU.cssClass("admin-block-inner")),
            false, HU.cssClass(CSS_CLASS_HEADING_2),
            HU.cssClass("admin-block"));
    }


    /**
     * _more_
     *
     * @param to _more_
     * @param subject _more_
     * @param contents _more_
     * @param asHtml _more_
     *
     * @throws Exception _more_
     */
    public void sendEmail(String to, String subject, String contents,
                          boolean asHtml)
            throws Exception {
        getRepository().getMailManager().sendEmail(to, subject, contents,
                asHtml);
    }


    /**
     * _more_
     *
     * @param to _more_
     * @param from _more_
     * @param subject _more_
     * @param contents _more_
     * @param asHtml _more_
     *
     * @throws Exception _more_
     */
    public void sendEmail(String to, String from, String subject,
                          String contents, boolean asHtml)
            throws Exception {
        getRepository().getMailManager().sendEmail(to, from, subject,
                contents, asHtml);
    }


    /**
     * _more_
     *
     * @param to _more_
     * @param from _more_
     * @param subject _more_
     * @param contents _more_
     * @param bcc _more_
     * @param asHtml _more_
     *
     * @throws Exception _more_
     */
    public void sendEmail(List<Address> to, InternetAddress from,
                          String subject, String contents, boolean bcc,
                          boolean asHtml)
            throws Exception {
        getRepository().getMailManager().sendEmail(to, from, subject,
                contents, bcc, asHtml, null);
    }

    public Result processMonitors(Request request) throws Exception {
	return getRepository().getMonitorManager().processMonitors(request);
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
    public Result adminSettingsDo(Request request) throws Exception {

	StringBuilder sb = new StringBuilder();
	if(!getAuthManager().verify(request,sb)) {
	    return adminSettings(request, sb.toString());
	}

        for (RepositoryManager manager :
                getRepository().getRepositoryManagers()) {
            manager.applyAdminSettings(request);
        }

        getRepository().writeGlobal(request, PROP_PROPERTIES, true);
        getRepository().writeGlobal(request, PROP_LOGO_URL, true);
        getRepository().writeGlobal(request, PROP_LOGO_IMAGE, true);
        getRepository().writeGlobal(request, PROP_REPOSITORY_NAME, true);
        getRepository().writeGlobal(request, PROP_REPOSITORY_SLUG, true);
        getRepository().writeGlobal(request, PROP_REPOSITORY_DESCRIPTION,
                                    true);
        getRepository().writeGlobal(request, PROP_ADMIN_PHRASES);
        getRepository().writeGlobal(request, PROP_HTML_FOOTER);


        getRepository().writeGlobal(request, PROP_GOOGLEAPIKEYS, true);
        //        getRepository().writeGlobal(request, PROP_FACEBOOK_CONNECT_KEY);
        getRepository().writeGlobal(PROP_NOSTYLE,
                                    "" + request.get(PROP_NOSTYLE, false));


        String ratings = "" + request.get(PROP_RATINGS_ENABLE, false);
        getRepository().writeGlobal(PROP_RATINGS_ENABLE, ratings);



        getRepository().writeGlobal(request, PROP_HOSTNAME);
        getRepository().writeGlobal(request, PROP_PORT);

        String useFixed = "" + request.get(PROP_USE_FIXED_HOSTNAME, false);

        getRepository().writeGlobal(PROP_USE_FIXED_HOSTNAME, useFixed);

        getRepository().writeGlobal(PROP_ACCESS_ALLSSL,
                                    "" + request.get(PROP_ACCESS_ALLSSL,
                                        false));




        getRepository().writeGlobal(PROP_UPLOAD_MAXSIZEGB,
                                    request.getString(PROP_UPLOAD_MAXSIZEGB,
                                        "10").trim());


        getRepository().writeGlobal(PROP_CACHE_MAXSIZEGB,
                                    request.getString(PROP_CACHE_MAXSIZEGB,
                                        "10").trim());

        getRepository().writeGlobal(PROP_SYSTEM_MESSAGE,
                                    request.getString(PROP_SYSTEM_MESSAGE,
                                        ""));




        getRepository().writeGlobal(PROP_LOCALFILEPATHS,
                                    request.getString(PROP_LOCALFILEPATHS,
                                        ""));

        getRepository().writeGlobal(request, PROP_REGISTER_KEY);



        List<OutputHandler> outputHandlers =
            getRepository().getOutputHandlers();
        for (OutputHandler outputHandler : outputHandlers) {
            outputHandler.applySettings(request);
        }

        List<OutputType> types = getRepository().getOutputTypes();
        boolean          doAll = request.get("outputtype.all", false);
	doAll = true;
        for (OutputType type : types) {
            if ( !type.getForUser()) {
                continue;
            }
            boolean ok = doAll
                         || request.get("outputtype." + type.getId(), false);
            //            if(!ok)System.err.println("TYPE:" + type + " " + ok);
            getRepository().setOutputTypeOK(type, ok);
        }

        getRepository().writeGlobal(PROP_ACCESS_ADMINONLY,
                                    request.get(PROP_ACCESS_ADMINONLY,
                                        false));
        getRepository().writeGlobal(PROP_ACCESS_REQUIRELOGIN,
                                    request.get(PROP_ACCESS_REQUIRELOGIN,
                                        false));
        getRepository().writeGlobal(PROP_ACCESS_NOBOTS,
                                    request.get(PROP_ACCESS_NOBOTS, false));
        getRepository().writeGlobal(PROP_ACCESS_NOGOOGLEBOT,
                                    request.get(PROP_ACCESS_NOGOOGLEBOT, false));	

        for (AdminHandler adminHandler : adminHandlers) {
            adminHandler.applyAdminSettingsForm(request);
        }


        //Now re-read all of the globals so we pick up any deletes
        getRepository().readDatabaseProperties();

        getRepository().setLocalFilePaths();
        getRepository().initAttributes();
        getRepository().clearCache();


        //Tell the other repositoryManagers that the settings changed
        for (RepositoryManager repositoryManager :
                getRepository().getRepositoryManagers()) {
            repositoryManager.adminSettingsChanged();
        }

	return adminSettings(request, getPageHandler().showDialogNote("Settings have changed"));

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
    public Result adminAccess(Request request) throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.append(HU.sectionOpen(null, false));
        sb.append(msgHeader("Permissions Overview"));

        Statement statement =
            getDatabaseManager().execute(
                "select "
                + SqlUtil.comma(
                    Tables.PERMISSIONS.COL_ENTRY_ID,
                    Tables.PERMISSIONS.COL_ACTION,
                    Tables.PERMISSIONS.COL_ROLE) + " from "
                        + Tables.PERMISSIONS.NAME, 10000000, 0);

        Hashtable<String, List> idToPermissions = new Hashtable<String,
                                                      List>();

        SqlUtil.Iterator iter = getDatabaseManager().getIterator(statement);
        ResultSet        results;
        List<String>     ids = new ArrayList<String>();
        while ((results = iter.getNext()) != null) {
            String id          = results.getString(1);
            String action      = results.getString(2);
            String role        = results.getString(3);
            List   permissions = idToPermissions.get(id);
            if (permissions == null) {
                idToPermissions.put(id, permissions = new ArrayList());
                ids.add(id);
            }
            permissions.add(new Permission(action, new Role(role)));
        }

        sb.append("<table cellspacing=\"0\" cellpadding=\"0\">");
        sb.append(HU.row(HU.cols(HU.space(10),
                HU.b(msg("Action")) + HU.space(3),
                HU.b(msg("Role")))));
        for (String id : ids) {
            Entry entry = getEntryManager().getEntry(request, id);
            if (entry == null) {
                continue;
            }
            sb.append(
                HU.row(
                    HU.colspan(
                        getPageHandler().getBreadCrumbs(
                            request, entry, null,
                            getRepository().URL_ACCESS_FORM, 80,-1), 3)));
            List<Permission> permissions =
                (List<Permission>) idToPermissions.get(id);
            for (Permission permission : permissions) {
                Role role = permission.getRoles().get(0);
                String row = HU.cols("", permission.getAction(),
                                            role.getRole());
                String clazz = role.getCssClass();
                sb.append(HU.row(row, HU.cssClass(clazz)));
            }
            //      System.err.println(sb+"\n");
            sb.append(HU.row(HU.colspan("<hr>", 3)));
        }
        sb.append("</table>");
        sb.append(HU.sectionClose());

        return makeResult(request, msg("RAMADDA-Admin-Access Overview"), sb);
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
    public Result adminStats(Request request) throws Exception {

        DecimalFormat fmt     = new DecimalFormat("#0");


        StringBuffer  stateSB = new StringBuffer();
        stateSB.append(HU.formTable());
        getStorageManager().addInfo(stateSB);
        getDatabaseManager().addInfo(stateSB);
        stateSB.append(HU.formTableClose());



        StringBuilder statusSB = new StringBuilder();
        statusSB.append(HU.formTable());
        statusSB.append(
            HU.formEntry(
                msgLabel("Version"),
                getRepository().getProperty(PROP_VERSION, "1.0")));
        statusSB.append(
            HU.formEntry(
                msgLabel("Build Date"),
                getRepository().getProperty(PROP_BUILD_DATE, "N/A")));
        statusSB.append(
            HU.formEntry(
                msgLabel("Java Version"),
                getRepository().getProperty(PROP_JAVA_VERSION, "N/A")));


        if (request.get("gc", false)) {
            Misc.gc();
        }

        appendMemory(statusSB);
	getSearchManager().addStats(statusSB);
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
        /*
        statusSB.append(HU.formEntry(msgLabel("Up Time"),
                                           fmt.format((double) (uptime / 1000
                                               / 60)) + " "
                                                   + msg("minutes")));
        */
        statusSB.append(HU.formEntry(msgLabel("Total # Requests"),
                                            getLogManager().getRequestCount()
                                            + ""));


        StringBuilder tmp = new StringBuilder();
        TTLCache.getInfo(tmp);
        HU.formEntry(statusSB, msgLabel("Caches"), tmp.toString());
        //        getEntryManager().addStatusInfo(statusSB);

        statusSB.append(HU.formTableClose());



        StringBuffer outputSB = new StringBuffer();
        outputSB.append(HU.formTable());
        List<OutputHandler> outputHandlers =
            getRepository().getOutputHandlers();

        for (OutputHandler outputHandler : outputHandlers) {
            try {
                outputHandler.getSystemStats(outputSB);
            } catch (Exception exc) {
                outputSB.append("Error getting stats:" + exc);
                getLogManager().logError("Error getting stats:", exc);
            }
        }
        outputSB.append(HU.formTableClose());


        String         props  = getRepository().getPropertiesListing();


        StringBuffer   apiSB  = new StringBuffer();
        List<Object[]> tuples = new ArrayList<Object[]>();
        apiSB.append(HU.formTable());
        for (ApiMethod apiMethod :
                getRepository().getApiManager().getApiMethods()) {
            if (apiMethod.getNumberOfCalls() < 1) {
                continue;
            }
            tuples.add(new Object[] {
		    Integer.valueOf(apiMethod.getNumberOfCalls()),
                apiMethod });

        }
        tuples = (List<Object[]>) Misc.sortTuples(tuples, false);
        for (Object[] tuple : tuples) {
            ApiMethod apiMethod = (ApiMethod) tuple[1];
            apiSB.append(HU.formEntry(apiMethod.getName(),
                                             "# " + msgLabel("Calls")
                                             + apiMethod.getNumberOfCalls()));
        }


        apiSB.append(HU.formTableClose());


        StringBuffer dbSB = new StringBuffer();

        getDatabaseManager().addStatistics(request, dbSB,true);


        StringBuffer sb = new StringBuffer();
        sb.append(HU.sectionOpen(null, false));
        List<String> titles = new ArrayList<String>();
        List<String> tabs   = new ArrayList<String>();

        titles.add(msg("System Status"));
        tabs.add(HU.section(statusSB.toString()));

        titles.add(msg("System Disk"));
        tabs.add(HU.section(stateSB.toString()));

        StringBuffer pluginsSB = new StringBuffer();
        getRepository().getPluginManager().addStatusInfo(request, pluginsSB);

        titles.add(msg("Plugins"));
        tabs.add(HU.section(pluginsSB.toString()));

        titles.add(msg("API"));
        tabs.add(HU.section(apiSB.toString()));

        titles.add(msg("Properties"));
        tabs.add(HU.section(props));

        titles.add(msg("Output Handlers"));
        tabs.add(HU.section(outputSB.toString()));

        titles.add(msg("Database Statistics"));
        tabs.add(HU.section(dbSB.toString()));

        HU.makeAccordion(sb, titles, tabs, true, "ramadda-accordion",
                                null);

        sb.append(HU.sectionClose());

        return makeResult(request, msg("RAMADDA-Admin-System"), sb);
    }


    /**
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result adminSnapshots(Request request) throws Exception {
        StringBuffer sb = new StringBuffer("");
        sb.append(HU.sectionOpen(null, false));
        File dir = new File(getStorageManager().getHtdocsDir()
                            + "/snapshots/pages");
        File[] files = dir.listFiles();
        if (files.length == 0) {
            sb.append("No snapshot files");
        }
        sb.append("<ul>");
        for (File f : files) {
            sb.append("<li> "
                      + HU.href(getRepository().getUrlBase()
                                + "/snapshots/pages/"
                                + f.getName(), f.getName()));
        }
        sb.append("</ul>");

        return makeResult(request, msg("RAMADDA-Admin-Snapshots"), sb);
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
    public Result adminSql(Request request) throws Exception {

        if ( !getRepository().getLocalProperty(PROP_ADMIN_INCLUDESQL, false)) {
            return new Result("", new StringBuffer("Not enabled"));
        }

        boolean bulkLoad = false;
        String  query    = null;
        String  sqlFile  = request.getUploadedFile(ARG_SQLFILE);
        if ((sqlFile != null) && (sqlFile.length() > 0)
                && new File(sqlFile).exists()) {
            query = getStorageManager().readSystemResource(sqlFile);
            if ((query != null) && (query.trim().length() > 0)) {
                bulkLoad = true;
            }
        }
        if ( !bulkLoad) {
            query = (String) request.getUnsafeString(ARG_QUERY,
                    (String) null);
            if ((query != null) && query.trim().startsWith("file:")) {
                query = getStorageManager().readSystemResource(
                    query.trim().substring(5));
                bulkLoad = true;
            }
        }


        StringBuffer formSB = new StringBuffer();
        formSB.append(request.uploadForm(URL_ADMIN_SQL));
        formSB.append(HU.submit("Execute"));
	getAuthManager().addVerification(request,formSB,
					 "For verification enter your password. Be very careful what you do here! This evaluates arbitrary SQL and you can really screw up your RAMADDA if you do something wrong here.",true);
        formSB.append(HU.br());
        formSB.append(HU.textArea(ARG_QUERY, (bulkLoad
                ? ""
                : (query == null)
                  ? BLANK
                  : query), 5, 80));
        formSB.append(HU.vspace());
        formSB.append(HU.labeledCheckbox("ascsv","true",false,"Download CSV"));
	formSB.append("<br>");

        formSB.append("SQL File: ");
        formSB.append(HU.fileInput(ARG_SQLFILE, HU.SIZE_60));
        formSB.append(HU.formClose());

        StringBuffer sb = new StringBuffer();
        getPageHandler().makeLinksHeader(request, sb, sqlUrls, "");	


        if (!stringDefined(query)) {
	    sb.append(formSB);
            return makeResult(request, msg("RAMADDA-Admin-SQL"), sb);
        }

	StringBuilder vsb = new StringBuilder();
	if(!getAuthManager().verify(request,vsb,true)) {
	    sb.append(vsb);
	    sb.append(formSB);
            return makeResult(request, msg("RAMADDA-Admin-SQL"), sb);
	}


	sb.append(formSB);
        long t1 = System.currentTimeMillis();
        if ((query.indexOf(";") > 0) || bulkLoad) {
            getDatabaseManager().loadSql(query, false, true);
            return makeResult(request, msg("RAMADDA-Admin-SQL"),
                              new StringBuffer("Executed SQL" + HU.vspace() +
					       HU.space(1) + sb.toString()));
        } else {
            Statement statement = null;
            try {
                statement = getDatabaseManager().execute(query, -1, 10000);
            } catch (Exception exc) {
                exc.printStackTrace();
                throw exc;
            }

            SqlUtil.Iterator iter =
                getDatabaseManager().getIterator(statement);
            ResultSet         results;
            int               cnt    = 0;
            Hashtable         map    = new Hashtable();
            int               unique = 0;
            ResultSetMetaData rsmd   = null;
            StringBuilder     raw    = null;
            StringBuilder     table  = new StringBuilder();
            StringBuilder     csv  = new StringBuilder();	    
	    boolean asCsv = request.get("ascsv",false);
            StringBuilder     resultsSB  = new StringBuilder();
            while ((results = iter.getNext()) != null) {
                if (rsmd == null) {
                    rsmd = results.getMetaData();
                }
                cnt++;
                if (cnt > 1000) {
                    continue;
                }
                int colcnt = 0;
                if (cnt == 1) {
                    table.append("<table class='ramadda-table stripe' table-ordering='true' table-height='400'><thead><tr>");
                    for (int i = 0; i < rsmd.getColumnCount(); i++) {
                        String col = rsmd.getColumnLabel(i + 1);
                        if (col.equals("QUERY PLAN")) {
                            raw = new StringBuilder();
                        }
                        table.append(HU.th(HU.bold(HU.div(col,HU.style("margin-left:5px;")))));
			if(asCsv) {
			    if(csv.length()>0) csv.append(",");
			    csv.append(col);
			}
                    }
		    if(asCsv) {
			csv.append("\n");
		    }
                    table.append("</tr></thead><tbody>");
                }
                table.append("<tr valign=\"top\">");
                while (colcnt < rsmd.getColumnCount()) {
                    colcnt++;
		    if(asCsv && colcnt>1) {
			csv.append(",");
		    }
                    if (rsmd.getColumnType(colcnt)
	
                            == java.sql.Types.TIMESTAMP) {
			synchronized(Repository.calendar) {
			    Date dttm = results.getTimestamp(colcnt,
							     Repository.calendar);
			    String date = formatDate(request,   dttm);
			    table.append(HU.col(date));
			    if(asCsv) csv.append(date);
			}
                    } else {
                        String s = results.getString(colcnt);
			if(asCsv) {
			    if(s==null)
				csv.append(Seesv.cleanColumnValue(""));
			    else
				csv.append(Seesv.cleanColumnValue(s));
			}
                        if (s == null) {
                            s = "_null_";
                        }
                        s = HU.entityEncode(s);
                        if (raw != null) {
                            raw.append(s);
                            raw.append("\n");
                        }

                        if (s.length() > 100) {
                            table.append(
                                HU.col(
                                    HU.textArea("dummy", s, 5, 50)));
                        } else {
                            table.append(HU.col(HU.pre(s,HU.cssClass("ramadda-pre-undecorated"))));
                        }
                    }
                }
                table.append("</tr>\n");
		if(asCsv) csv.append("\n");
                //                if (cnt++ > 1000) {
                //                    table.append(HU.row("..."));
                //                    break;
                //                }
            }
            table.append("</tbody></table>");
            if (raw != null) {
                resultsSB.append(HU.pre(raw.toString()));
            } else {
                resultsSB.append(table);
            }
            long t2 = System.currentTimeMillis();
            getRepository().clearCache();
            getRepository().readDatabaseProperties();
	    sb.append(msgLabel("Fetched rows") + cnt
		      + HU.space(1) + msgLabel("in")
		      + (t2 - t1) + "ms");
	    sb.append(HU.vspace());
	    sb.append(resultsSB);
	    if(asCsv) {
		request.setReturnFilename("db.csv");
		return new Result("", csv, "text/csv");
	    }


            return makeResult(request, msg("RAMADDA-Admin-SQL"),  sb);
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
    public Result adminScanForBadParents(Request request) throws Exception {
        boolean      delete = request.get("delete", false);
        StringBuffer sb     = new StringBuffer();
        Statement statement = getDatabaseManager().execute("select "
                                  + Tables.ENTRIES.COL_ID + ","
                                  + Tables.ENTRIES.COL_PARENT_GROUP_ID
                                  + " from " + Tables.ENTRIES.NAME, 10000000,
                                      0);
        SqlUtil.Iterator iter = getDatabaseManager().getIterator(statement);
        ResultSet        results;
        int              cnt        = 0;
        List<Entry>      badEntries = new ArrayList<Entry>();
        while ((results = iter.getNext()) != null) {
            String id       = results.getString(1);
            String parentId = results.getString(2);
            cnt++;
            if (parentId != null) {
                Entry parent = getEntryManager().findGroup(request, parentId);
                if (parent == null) {
                    Entry entry = getEntryManager().getEntry(request, id);
                    sb.append("bad parent:" + entry.getName() + " parent id="
                              + parentId + "<br>");
                    badEntries.add(entry);
                }
            }
        }
        sb.append("Scanned " + cnt + " entries");
        if (delete) {
            getEntryManager().deleteEntries(request, badEntries, null);

            return makeResult(request, msg("RAMADDA-Admin-Scan"),
                              new StringBuffer("Deleted"));
        }

        return makeResult(request, msg("RAMADDA-Admin-Scan"), sb);
    }

    /**
     * _more_
     *
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    private void appendMemory(Appendable sb) throws Exception {
        Runtime.getRuntime().gc();
        DecimalFormat fmt        = new DecimalFormat("#0");
        double        maxMemory  = (double) Runtime.getRuntime().maxMemory();
        double        freeMemory = (double) Runtime.getRuntime().freeMemory();
        double totalMemory       =
            (double) Runtime.getRuntime().totalMemory();
        double        usedMemory = (totalMemory - freeMemory);
        sb.append(HU.formEntry("Max Memory:",
                                      fmt.format(maxMemory / 1000000)
                                      + " (MB)"));
        sb.append(HU.formEntry("Total Memory:",
                                      fmt.format(totalMemory / 1000000)
                                      + " (MB)"));
        sb.append(HU.formEntry("Free Memory:",
                                      fmt.format(freeMemory / 1000000)
                                      + " (MB)"));
        sb.append(HU.formEntry("Used Memory:",
                                      fmt.format(usedMemory / 1000000)
                                      + " (MB)"));

    }

    /**
     *
     * @return _more_
     */
    public int getUsedMemory() {
        double freeMemory  = (double) Runtime.getRuntime().freeMemory();
        double totalMemory = (double) Runtime.getRuntime().totalMemory();
        double usedMemory  = (totalMemory - freeMemory);

        return (int) (usedMemory / 1000000);
    }

    /**
     */
    private void printMemory() {
        try {
            if (true) {
                return;
            }
            Runtime.getRuntime().gc();
            DecimalFormat fmt        = new DecimalFormat("#0");
            double maxMemory = (double) Runtime.getRuntime().maxMemory();
            double freeMemory = (double) Runtime.getRuntime().freeMemory();
            double totalMemory = (double) Runtime.getRuntime().totalMemory();
            double        usedMemory = (totalMemory - freeMemory);
            String cache = " cache:"
                           + getEntryManager().getEntryCache().size();
            usedMemory = usedMemory / 1000000;
            if (usedMemory > 300) {
                //              System.err.println("printMemory: Clearing cache");
                //              getEntryManager().clearCache();
            }
            System.err.println("Used Memory:" + fmt.format(usedMemory)
                               + " (MB)" + cache);


        } catch (Exception exc) {
            throw new RuntimeException(exc);
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
    public Result adminPrintStack(Request request) throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.append(HU.formTable());
        appendMemory(sb);
        sb.append(HU.formEntry("Start Time:",
                                      "" + getRepository().getStartTime()));


        long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
        //sb.append(HU.formEntry("Up Time:",
        //                             fmt.format((double) (uptime / 1000
        //                                 / 60)) + " " + msg("minutes")));
        sb.append(HU.formEntry("Up Time:", formatUptime(uptime)));

        sb.append(HU.formTableClose());
        sb.append(HU.makeShowHideBlock(msg("Stack"),
                "<pre>" + LogUtil.getStackDump(true) + "</pre>", false));

        return makeResult(request, msg("RAMADDA-Admin-Stack Trace"), sb);
    }

    /**
     * Format the uptime
     *
     * @param milliseconds  time in milliseconds
     *
     * @return formatted time
     */
    private String formatUptime(long milliseconds) {
        StringBuffer buf     = new StringBuffer();
        int          seconds = (int) (milliseconds / 1000) % 60;
        int          minutes = (int) ((milliseconds / (1000 * 60)) % 60);
        int          hours   = (int) ((milliseconds / (1000 * 60 * 60)) % 24);
        int          days    = (int) ((milliseconds / (1000 * 60 * 60 * 24)));
        if (days > 0) {
            buf.append(days);
            buf.append(" day");
            if (days > 1) {
                buf.append("s");
            }
            buf.append(", ");
        }
        if (hours > 0) {
            buf.append(hours);
            buf.append(" hour");
            if (hours > 1) {
                buf.append("s");
            }
            buf.append(", ");
        }
        buf.append(minutes);
        buf.append(" minute");
        if (minutes != 1) {
            buf.append("s");
        }

        return buf.toString();
    }


    /**
     *
     * @param request _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void listMissingFiles(Request request, Appendable sb)
            throws Exception {
        String pattern = request.getString("pattern", "");
        Statement statement =
            getDatabaseManager().select(
                SqlUtil.comma(
                    Tables.ENTRIES.COL_ID, Tables.ENTRIES.COL_RESOURCE,
                    Tables.ENTRIES.COL_TYPE), Tables.ENTRIES.NAME,
                        Clause.or(
                            new Clause[] {
                                Clause.eq(
                                    Tables.ENTRIES.COL_RESOURCE_TYPE,
                                    Resource.TYPE_LOCAL_FILE),
                                Clause.eq(Tables.ENTRIES
                                    .COL_RESOURCE_TYPE, Resource
                                    .TYPE_FILE), Clause
                                        .eq(Tables.ENTRIES
                                            .COL_RESOURCE_TYPE, Resource
                                            .TYPE_STOREDFILE) }), getDatabaseManager()
                                                .makeOrderBy(Tables.ENTRIES
                                                    .COL_CREATEDATE, true));

        SqlUtil.Iterator iter = getDatabaseManager().getIterator(statement);
        ResultSet        results;
        int              cnt        = 0;
        int              missingCnt = 0;
        StringBuilder    buff       = new StringBuilder();
        buff.append(
            "<table><tr><td><b>Entry</b></td><td><b>Missing File</b></td></tr>");

        boolean even = true;
        while ((results = iter.getNext()) != null) {
            cnt++;
            if ((cnt % 1000) == 0) {
                System.err.println("check missing cnt:" + cnt);
            }
            int    col = 1;
            String id  = results.getString(col++);
            String resource =
                getStorageManager().resourceFromDB(results.getString(col++));
            File f = new File(resource);
            if (f.exists()) {
                continue;
            }
            if ((pattern != null) && (pattern.length() > 0)) {
                if (resource.matches(pattern)) {
                    continue;
                }
            }

            even = !even;
            missingCnt++;
            Entry  entry = getEntryManager().getEntry(request, id);
            String clazz = even
                           ? "ramadda-row-even"
                           : "ramadda-row-odd";
            if (entry == null) {
                buff.append("<tr class=" + clazz
                            + "  valign=top><td>NULL Entry " + id
                            + "</td><td>" + f + "</td></tr>");
            } else {
                buff.append("<tr class=" + clazz + " valign=top><td>"
                            + getEntryManager().getEntryLink(request, entry,
                                true, "") + "</td><td>" + f + "</td></tr>");
            }
        }
        buff.append("</table>");

        sb.append("Total entries: #" + cnt + "<br>");
        if (missingCnt > 0) {
            sb.append("Missing: #" + missingCnt + "<br>");
        }

        sb.append(buff);

    }

    public void listOrphans(Request request, Appendable sb)
            throws Exception {
        Statement statement =
            getDatabaseManager().select(
					SqlUtil.comma(Tables.ENTRIES.COL_ID,
						      Tables.ENTRIES.COL_PARENT_GROUP_ID),
					Tables.ENTRIES.NAME,
					null, getDatabaseManager().makeOrderBy(Tables.ENTRIES.COL_CREATEDATE, true));

        SqlUtil.Iterator iter = getDatabaseManager().getIterator(statement);
        ResultSet        results;
        int              cnt        = 0;
        int              orphanCnt        = 0;
        int              deleteCnt        = 0;		
	List<Entry> orphans = new ArrayList<Entry>();
	Entry root = getEntryManager().getRootEntry();
        boolean even = true;
        while ((results = iter.getNext()) != null) {
            cnt++;
            if ((cnt % 1000) == 0) {
                System.err.println("check orphan cnt:" + cnt);
            }
            int    col = 1;
            String id  = results.getString(col++);
	    if(id.equals(root.getId())) continue;
            String parentId  = results.getString(col++);	    
            Entry  parent = getEntryManager().getEntry(request, parentId);
	    if(parent!=null) continue;
            even = !even;
            Entry  entry = getEntryManager().getEntry(request, id);
	    orphans.add(entry);
	    orphanCnt++;
        }
	sb.append("Total entries: #" + cnt + "<br>");
	sb.append("Total orphan entries: #" + orphans.size() + "<br>");
	sb.append(getWikiManager().makeTableTree(request, new WikiUtil(),new Hashtable(), orphans));
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
    public Result adminMaintenance(Request request) throws Exception {

        StringBuffer  sb         = new StringBuffer();

	int[] cnt = {0};
	BiConsumer<StringBuffer, String> header= (buff,label) -> {
	    if(cnt[0]>0) buff.append("<hr>");
	    cnt[0]++;
	    buff.append(HU.center(HU.div(label,HU.cssClass("ramadda-heading"))));
	};

        StringBuffer  topSB         = new StringBuffer();

	header.accept(topSB, "Caches");
	request.formPostWithAuthToken(topSB, URL_ADMIN_MAINTENANCE, "");
	topSB.append(HU.submit("Clear all caches", ACTION_CLEARCACHE));
	topSB.append(HU.formClose());
	

	header.accept(topSB, "Clear Passwords");
	topSB.append(request.formPost(URL_ADMIN_MAINTENANCE, ""));
	topSB.append(messageNote("Note:  All users including you will have to reset their passwords. If you do not have email enabled then only the admin will be able to reset the passwords. So, if you do this then right away, while your session is active, go and change your password. If things go bad and you can't login at all see the  <a href=\"http://ramadda.org/repository/userguide/faq.html#faq1_cat1_6\">FAQ</a> post."));

	topSB.append(HU.submit("Clear all passwords", ACTION_PASSWORDS_CLEAR));
	getAuthManager().addVerification(request,topSB,null,true);
	topSB.append(HU.formClose());

	
	header.accept(topSB, "Export Database");
	topSB.append(request.formPost(URL_ADMIN_MAINTENANCE));
	topSB.append(messageNote("You can write out the database for backup or transfer to a new database"));
	topSB.append(HU.submit("Export the database", ACTION_DUMPDB));
	getAuthManager().addVerification(request,topSB,null,true);
	topSB.append(HU.formClose());

	header.accept(topSB, "Reindex Lucene Index");
	request.formPostWithAuthToken(topSB, URL_ADMIN_MAINTENANCE, "");
	topSB.append(messageNote("This runs through all of entries of the selected type and reindexes them"));
        topSB.append(HU.formTable());
	List types= Utils.makeListFromValues(new TwoFacedObject("All Entry Types",""));
	HU.formEntry(topSB,
		     msgLabel("Type"),
		     getRepository().makeTypeSelect(types, request, ARG_TYPE,"",false,null,false,null,false));
	HU.formEntry(topSB,"",HU.labeledCheckbox(ARG_DELETE_INDEX,"true",false,"Delete entire index"));
	HU.formEntry(topSB,"",
		     HU.submit("Reindex", ACTION_REINDEX));
        topSB.append(HU.formTableClose());
	topSB.append(HU.formClose());



        StringBuffer filePathSB = new StringBuffer();
	header.accept(filePathSB,"Change file paths");
        request.formPostWithAuthToken(filePathSB, URL_ADMIN_MAINTENANCE, "");
        filePathSB.append(messageNote("Change the stored file path for all entries that match the following pattern"));
        filePathSB.append(HU.formTable());
        filePathSB.append(HU.formEntry(msgLabel("File Pattern"),
                HU.input(ARG_CHANGEPATHS_PATTERN,
                                request.getString(ARG_CHANGEPATHS_PATTERN,
                                    ""), HU.SIZE_50)));
        filePathSB.append(HU.formEntry(msgLabel("Change to"),
                HU.input(ARG_CHANGEPATHS_TO,
                                request.getString(ARG_CHANGEPATHS_TO, ""),
                                HU.SIZE_50)));

        filePathSB.append(HU.formTableClose());
        filePathSB.append(HU.submit("Change file paths", ACTION_CHANGEPATHS));
        filePathSB.append(HU.space(2));
        filePathSB.append(HU.labeledCheckbox(ARG_CHANGEPATHS_CONFIRM, "true",
						    false,"Yes, really change all of the file paths"));
        filePathSB.append(HU.formClose());


        StringBuffer missingSB = new StringBuffer();
	header.accept(missingSB,"List missing files");
        request.formPostWithAuthToken(missingSB, URL_ADMIN_MAINTENANCE, "");
        missingSB.append(HU.formTable());
        HU.formEntry(missingSB,msgLabel("Skip pattern"),
		     HU.input("pattern",
				     request.getString("pattern", ""),
				     HU.SIZE_50));
        missingSB.append(HU.formTableClose());
        missingSB.append(HU.submit("List missing files", ACTION_LISTMISSING));
        StringBuffer orphansSB = new StringBuffer();
	header.accept(orphansSB,"List orphan entries");
	orphansSB.append(messageNote("This lists all entries that don't have a parent entry. Normally this shouldn't happen but to due an occasional bug there can be entries that aren't part of the main hierarchy."));
        request.formPostWithAuthToken(orphansSB, URL_ADMIN_MAINTENANCE, "");
	orphansSB.append(HU.submit("List orphans", ACTION_LISTORPHANS));
        orphansSB.append(HU.formClose());



        if (request.defined(ACTION_STOP)) {
            runningCleanup = false;
            cleanupTS++;

            return new Result(request.makeUrl(URL_ADMIN_MAINTENANCE));
        } else if (request.defined(ACTION_START)) {
            //            Misc.run(this, "runDatabaseCleanUp", request);
            Misc.run(this, "runDatabaseOrphanCheck", request);

            return new Result(request.makeUrl(URL_ADMIN_MAINTENANCE));
        } else if (request.defined(ACTION_DUMPDB)) {
            return adminDbDump(request);
        } else if (request.defined(ACTION_REINDEX)) {
            return adminReindex(request);
        } else if (request.defined(ACTION_NEWDB)) {
            getDatabaseManager().reInitialize();

            return new Result(request.makeUrl(URL_ADMIN_MAINTENANCE));
        } else if (request.defined(ACTION_CLEARCACHE)) {
            getRepository().clearAllCaches();
        } else if (request.defined(ACTION_LISTMISSING)) {
            sb.append(missingSB);
            listMissingFiles(request, sb);
            return makeResult(request, "RAMADDA-Admin-Missing Files", sb);
        } else if (request.defined(ACTION_LISTORPHANS)) {
            sb.append(orphansSB);
	    sb.append("<div style=margin-left:20px;>");
            listOrphans(request, sb);
	    sb.append("</div>");
            return makeResult(request, "RAMADDA-Admin-Orphan Entries", sb);
        } else if (request.defined(ACTION_CHANGEPATHS)) {
            if (request.defined(ARG_CHANGEPATHS_PATTERN)) {
                StringBuilder tmp = new StringBuilder();
                getRepository().getEntryManager().changeResourcePaths(
                    request, request.getString(ARG_CHANGEPATHS_PATTERN, ""),
                    request.getString(ARG_CHANGEPATHS_TO, ""), tmp,
                    request.get(ARG_CHANGEPATHS_CONFIRM, false));
                sb.append(
                    HU.div(
                        tmp.toString(),
                        HU.style(
                            "max-height:300px; overflow-y: auto;")));
            } else {
                sb.append("Change paths fields required");
            }
            sb.append(filePathSB);

            return makeResult(request, msg("RAMADDA-Admin-Change File Paths"), sb);
        } else if (request.defined(ACTION_SHUTDOWN)) {
            getAuthManager().ensureAuthToken(request);
            if (getRepository().getShutdownEnabled()) {
		if(getAuthManager().verify(request,sb,true)) {
                    return processShutdown(request);
                }
            }
        } else if (request.defined(ACTION_PASSWORDS_CLEAR)) {
	    if(getAuthManager().verify(request,sb,true)) {
                clearAllPasswords();
                sb.append("All passwords have been cleared. Make sure you go and change your password immediately");
                return makeResult(request, msg("RAMADDA-Admin--Maintenance"), sb);
            }
        }


        String status = cleanupStatus.toString();
        if (runningCleanup) {
            sb.append(msg("Database clean up is running"));
            sb.append(HU.vspace());
            sb.append(HU.submit("Stop cleanup", ACTION_STOP));
        } else {
            sb.append(HU.vspace());
            sb.append(topSB);
            sb.append(filePathSB);
            sb.append(missingSB);
            sb.append(orphansSB);	    

            if (getRepository().getShutdownEnabled()) {
                request.formPostWithAuthToken(sb, URL_ADMIN_MAINTENANCE, "");
		header.accept(sb, "Shutdown");
                sb.append(HU.submit("Shutdown server", ACTION_SHUTDOWN));
		getAuthManager().addVerification(request,sb,"To verify shutdown please enter your password",true);
                sb.append(HU.formClose());
            }


        }
        sb.append("</form>");
        if (status.length() > 0) {
            sb.append(msgHeader("Cleanup Status"));
            sb.append(status);
        }

        //        sb.append(cnt +" files do not exist in " + (t2-t1) );
        return makeResult(request, msg("RAMADDA-Admin-Maintenance"), sb);

    }


    /**
     * _more_
     *
     * @throws Exception _more_
     */
    private void clearAllPasswords() throws Exception {
        String sql = "update " + Tables.USERS.NAME + " set "
                     + Tables.USERS.COL_NODOT_PASSWORD + " = ''";
        Statement statement = getDatabaseManager().execute(sql, -1, 10000);
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @throws Exception _more_
     */
    public void runDatabaseCleanUp(Request request) throws Exception {
        if (runningCleanup) {
            return;
        }
        runningCleanup = true;
        cleanupStatus  = new StringBuffer();
        int myTS = ++cleanupTS;
        try {
            Statement statement =
                getDatabaseManager().select(
                    SqlUtil.comma(
                        Tables.ENTRIES.COL_ID, Tables.ENTRIES.COL_RESOURCE,
                        Tables.ENTRIES.COL_TYPE), Tables.ENTRIES.NAME,
                            Clause.eq(
                                Tables.ENTRIES.COL_RESOURCE_TYPE,
                                Resource.TYPE_FILE));

            SqlUtil.Iterator iter =
                getDatabaseManager().getIterator(statement);
            ResultSet   results;
            int         cnt       = 0;
            int         deleteCnt = 0;
            long        t1        = System.currentTimeMillis();
            List<Entry> entries   = new ArrayList<Entry>();
            while ((results = iter.getNext()) != null) {
                if ((cleanupTS != myTS) || !runningCleanup) {
                    runningCleanup = false;

                    break;
                }
                int    col = 1;
                String id  = results.getString(col++);
                String resource = getStorageManager().resourceFromDB(
                                      results.getString(col++));
                Entry entry = getRepository().getTypeHandler(
                                  results.getString(col++)).createEntry(id);
                File f = new File(resource);
                if (f.exists()) {
                    continue;
                }
                //TODO: differentiate the entries that are not files
                entries.add(entry);
                if (entries.size() % 1000 == 0) {
                    System.err.print(".");
                }
                if (entries.size() > 1000) {
                    getEntryManager().deleteEntries(request, entries, null);
                    entries   = new ArrayList<Entry>();
                    deleteCnt += 1000;
                    cleanupStatus = new StringBuffer("Removed " + deleteCnt
                            + " entries from database");
                }
                if ((cleanupTS != myTS) || !runningCleanup) {
                    runningCleanup = false;

                    break;
                }
            }
            if (runningCleanup) {
                getEntryManager().deleteEntries(request, entries, null);
                deleteCnt += entries.size();
                cleanupStatus = new StringBuffer(msg("Done running cleanup")
                        + "<br>" + msg("Removed") + HU.space(1)
                        + deleteCnt + " entries from database");
            }
        } catch (Exception exc) {
            logError("Running cleanup", exc);
            cleanupStatus.append("An error occurred running cleanup<pre>");
            cleanupStatus.append(LogUtil.getStackTrace(exc));
            cleanupStatus.append("</pre>");
        }
        runningCleanup = false;
        long t2 = System.currentTimeMillis();
    }



    /**
     * _more_
     *
     * @param request _more_
     *
     * @throws Exception _more_
     */
    public void runDatabaseOrphanCheck(Request request) throws Exception {
        if (runningCleanup) {
            return;
        }
        runningCleanup = true;
        cleanupStatus  = new StringBuffer();
        int             myTS  = ++cleanupTS;
        List<String[]>  ids   = new ArrayList<String[]>();
        HashSet<String> idMap = new HashSet<String>();

        try {
            Statement statement =
                getDatabaseManager()
                    .select(SqlUtil
                        .comma(Tables.ENTRIES.COL_ID,
                               Tables.ENTRIES.COL_PARENT_GROUP_ID), Tables
                                   .ENTRIES.NAME, (Clause) null);
            SqlUtil.Iterator iter =
                getDatabaseManager().getIterator(statement);
            ResultSet results;
            long      t1 = System.currentTimeMillis();
            while ((results = iter.getNext()) != null) {
                if ((cleanupTS != myTS) || !runningCleanup) {
                    runningCleanup = false;

                    break;
                }
                String id       = results.getString(1);
                String parentId = results.getString(2);
                ids.add(new String[] { id, parentId });
                idMap.add(id);
            }

            for (String[] tuples : ids) {
                String id       = tuples[0];
                String parentId = tuples[1];
                if (parentId == null) {
                    Entry entry = getEntryManager().getEntry(request, id);
                    System.out.println("root:" + id + " " + entry);

                    continue;
                }
                if ( !idMap.contains(parentId)) {
                    //                    System.out.println("bad parent:" + id +" " + parentId);
                }
            }


            if (runningCleanup) {
                cleanupStatus = new StringBuffer(msg("Done running cleanup"));
            }
        } catch (Exception exc) {
            logError("Running cleanup", exc);
            cleanupStatus.append("An error occurred running cleanup<pre>");
            cleanupStatus.append(LogUtil.getStackTrace(exc));
            cleanupStatus.append("</pre>");
        }
        runningCleanup = false;
        long t2 = System.currentTimeMillis();
    }



    /** _more_ */
    int ccnt = 0;



    /**
     * _more_
     *
     * @param msg _more_
     */
    public void checkMemory(String msg) {
        //        Misc.gc();
        Runtime.getRuntime().gc();
        double freeMemory    = (double) Runtime.getRuntime().freeMemory();
        double highWaterMark = (double) Runtime.getRuntime().totalMemory();
        double usedMemory    = (highWaterMark - freeMemory);
        usedMemory = usedMemory / 1000000.0;
        System.err.println(msg + ((int) usedMemory));

    }



}
