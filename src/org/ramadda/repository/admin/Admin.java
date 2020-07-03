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

package org.ramadda.repository.admin;


import org.ramadda.repository.*;

import org.ramadda.repository.auth.*;
import org.ramadda.repository.database.*;

import org.ramadda.repository.harvester.*;

import org.ramadda.repository.output.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JQuery;
import org.ramadda.util.Utils;

import org.ramadda.util.sql.Clause;

import org.ramadda.util.sql.SqlUtil;


import org.w3c.dom.*;

import ucar.unidata.util.Counter;
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
public class Admin extends RepositoryManager {

    /** _more_ */
    public static final String PROP_INSTALL_PASSWORD =
        "ramadda.install.password";


    /** _more_ */
    public static final String ACTION_SHUTDOWN = "action.shutdown";


    /** _more_ */
    public static final String ACTION_CLEARCACHE = "action.clearcache";

    /** _more_ */
    public static final String ACTION_NEWDB = "action.newdb";

    /** _more_ */
    public static final String ACTION_DUMPDB = "action.dumpb";

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
    public RequestUrl URL_ADMIN_SQL = new RequestUrl(this, "/admin/sql",
                                          "SQL");



    /** _more_ */
    public RequestUrl URL_ADMIN_CLEANUP = new RequestUrl(this,
                                              "/admin/cleanup",
                                              "Maintenance");


    /** _more_ */
    public RequestUrl URL_ADMIN_STARTSTOP = new RequestUrl(this,
                                                "/admin/startstop",
                                                "Database");


    /** _more_ */
    public RequestUrl URL_ADMIN_SETTINGS = new RequestUrl(this,
                                               "/admin/settings", "Settings");


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
                                             "/admin/tables", "Database");


    /** _more_ */
    public RequestUrl URL_ADMIN_DUMPDB = new RequestUrl(this,
                                             "/admin/dumpdb",
                                             "Dump Database");

    /** _more_ */
    public RequestUrl URL_ADMIN_STATS = new RequestUrl(this, "/admin/stats",
                                            "System");

    /** _more_ */
    public RequestUrl URL_ADMIN_ACCESS = new RequestUrl(this,
                                             "/admin/access", "Access");


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
        getRegistryManager().URL_REGISTRY_REMOTESERVERS,
        /*URL_ADMIN_STARTSTOP,*/
        /*URL_ADMIN_TABLES, */
        URL_ADMIN_LOG, URL_ADMIN_STACK, URL_ADMIN_CLEANUP
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
     */
    public void doFinalInitialization() {
        if (getRepository().getProperty(PROP_ADMIN_INCLUDESQL, false)) {
            adminUrls.add(URL_ADMIN_SQL);
        }

        if (getRepository().isMaster()) {
            int idx =
                adminUrls.indexOf(getHarvesterManager().URL_HARVESTERS_LIST);
            adminUrls.add(idx + 1, URL_ADMIN_LOCAL);
        }

    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getInstallationComplete() {
        if (installationComplete == null) {
            installationComplete = new Boolean(
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
        installationComplete = new Boolean(v);
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
            List<String> toks = StringUtil.split(registrationKey, ":");
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
                numberUsers = new Integer(Utils.unobfuscate(toks.get(idx++),
                        true)).intValue();
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
        return getRepository().getDbProperty(what, false);
    }

    /**
     * _more_
     *
     * @param what _more_
     *
     * @throws Exception _more_
     */
    private void installStep(String what) throws Exception {
        getRepository().writeGlobal(what, "true");
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
        sb.append(HtmlUtils.textArea("", license, 10, 120));
        sb.append(HtmlUtils.br());

        sb.append(HtmlUtils.open(HtmlUtils.TAG_DIV,
                                 HtmlUtils.cssClass("registration-agree")));
        sb.append(HtmlUtils.checkbox(ARG_AGREE, "true", false));
        sb.append(HtmlUtils.space(2));
        sb.append(
            "I agree to the above license and conditions of use for the RAMADDA software");
        sb.append(HtmlUtils.close(HtmlUtils.TAG_DIV));

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
        return "<div class=\"ramadda-admin-note\">" + s + "</div>\n";
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
    public Result doInitialization(Request request) throws Exception {

        String installPassword =
            getRepository().getProperty(PROP_INSTALL_PASSWORD, "").trim();
        if (installPassword.length() == 0) {
            return new Result(
                "Install Error",
                new StringBuffer(
                    getPageHandler().showDialogError(
                        "Error: No installation password has been specified.<br>You need to add a .properties file to your RAMADDA home directory with the following set and then restart your server<br>&nbsp;<br><pre>\n\n" + PROP_INSTALL_PASSWORD + "=some password\n\n</pre>")));
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

        //Always check the password
        if (haveDoneInstallStep(ARG_ADMIN_INSTALLNOTICESHOWN)) {
            if ( !Utils.passwordOK(installPassword, givenPassword)) {
                undoInstallStep(ARG_ADMIN_LICENSEREAD,
                                ARG_ADMIN_INSTALLNOTICESHOWN,
                                ARG_ADMIN_ADMINCREATED);
                sb.append(
                    getPageHandler().showDialogError(
                        "Error: Incorrect installation password"));
            } else {
                sb.append(HtmlUtils.hidden(PROP_INSTALL_PASSWORD,
                                           givenPassword));
            }
        }


        if ( !haveDoneInstallStep(ARG_ADMIN_INSTALLNOTICESHOWN)) {
            title = "Installation";
            sb.append(
                note("Thank you for trying out the Geode Systems RAMADDA Repository. Listed below is the RAMADDA home directory and database information. If you want to change these settings please consult the <a target=\"other\" href=\"" + HELP_ROOT + "/userguide/installing.html#home\">documentation</a> before continuing with the installation process."));
            sb.append(HtmlUtils.formTable());

            getStorageManager().addInfo(sb);
            getDatabaseManager().addInfo(sb);
            sb.append(
                HtmlUtils.formEntry(
                    msgLabel("Install Password"),
                    HtmlUtils.input(PROP_INSTALL_PASSWORD, "")));
            sb.append(HtmlUtils.formTableClose());
            sb.append(HtmlUtils.submit(msg("Next"),
                                       ARG_ADMIN_INSTALLNOTICESHOWN));
        } else if ( !haveDoneInstallStep(ARG_ADMIN_LICENSEREAD)) {
            title = "License and Conditions of Use";
            sb.append(getLicenseForm());
            sb.append(HtmlUtils.br());
            sb.append(HtmlUtils.submit(msg("Next")));
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
                    errorBuffer.append(HtmlUtils.space(2));
                    errorBuffer.append(msg("Please enter an ID"));
                    errorBuffer.append(HtmlUtils.br());
                }

                if ((password1.length() == 0)
                        || !password1.equals(password2)) {
                    okToAdd = false;
                    errorBuffer.append(HtmlUtils.space(2));
                    errorBuffer.append(msg("Invalid password"));
                    errorBuffer.append(HtmlUtils.br());
                }


                if (okToAdd) {
                    User user = new User(
                                    id, name,
                                    request.getString(
                                        UserManager.ARG_USER_EMAIL,
                                        "").trim(), "", "",
                                            getUserManager().hashPassword(
                                                password1), "", true, "", "",
                                                    false, null);
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
                        note("Initial configuration process is complete."));

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
                        IOUtil.close(fis);
                    }


                    if (description == null) {
                        description = getRepository().getResource(
                            "/org/ramadda/repository/resources/install/initdescription.txt");
                    }

                    description = description.replace("${topid}",
                            topEntry.getId());
                    description = description.replace("${root}",
                            getRepository().getUrlBase());
                    topEntry.setDescription(description);
                    getEntryManager().updateEntry(null, topEntry);

                    //NOT NOW
                    //getRegistryManager().doFinalInitialization();

                    //Make sure we do this now before we do the final init entries

                    boolean didPlugin = false;
                    for (String plugin : PluginManager.PLUGINS) {
                        if (request.get("plugin." + plugin, false)) {
                            didPlugin = true;
                            getRepository().getPluginManager().installPlugin(
                                plugin);
                        }
                    }
                    if (didPlugin) {
                        getRepository().loadPluginResources();
                    }

                    //                    System.err.println("Adding init entries");
                    addInitEntries(user);
                    //                    System.err.println("done Adding init entries");
                    sb.append(getUserManager().makeLoginForm(request));
                    if (errorBuffer.length() > 0) {
                        sb.append(
                            getPageHandler().showDialogError(
                                msg("Error") + "<br>" + errorBuffer));
                    }

                    StringBuilder html = new StringBuilder();
                    html.append(HtmlUtils.sectionOpen(null, false));
                    html.append(HtmlUtils.h2("RAMADDA Install"));
                    html.append(sb);
                    html.append(HtmlUtils.sectionClose());

                    return new Result("Repository Initialization", html);
                }
            }

            if (errorBuffer.length() > 0) {
                sb.append(getPageHandler().showDialogError(msg("Error")
                        + "<br>" + errorBuffer));
            }
            sb.append(
                "Please enter the following information. This information is used to configure your RAMADDA server and is not sent anywhere.");
            String required1 =
                " <span class=\"ramadda-required-field\">* required</span>";
            String required2 =
                " <span class=\"ramadda-required-field\">*</span>";
            sb.append(HtmlUtils.formTable());
            sb.append(
                HtmlUtils.row(
                    HtmlUtils.colspan(msgHeader("Administrator Login"), 2)));
            sb.append(
                HtmlUtils.formEntry(
                    msgLabel("ID"),
                    HtmlUtils.input(UserManager.ARG_USER_ID, id)
                    + required1));
            sb.append(
                HtmlUtils.formEntry(
                    msgLabel("Name"),
                    HtmlUtils.input(UserManager.ARG_USER_NAME, name)));
            sb.append(
                HtmlUtils.formEntry(
                    msgLabel("Email"),
                    HtmlUtils.input(
                        UserManager.ARG_USER_EMAIL,
                        request.getString(UserManager.ARG_USER_EMAIL, ""))));
            sb.append(
                HtmlUtils.formEntry(
                    msgLabel("Password"),
                    HtmlUtils.password(UserManager.ARG_USER_PASSWORD1)
                    + required2));
            sb.append(
                HtmlUtils.formEntry(
                    msgLabel("Password Again"),
                    HtmlUtils.password(UserManager.ARG_USER_PASSWORD2)
                    + required2));

            sb.append(
                HtmlUtils.row(
                    HtmlUtils.colspan(msgHeader("Server Information"), 2)));
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
                HtmlUtils.formEntry(
                    msgLabel("Repository Name"),
                    HtmlUtils.input(
                        PROP_REPOSITORY_NAME,
                        request.getString(
                            PROP_REPOSITORY_NAME,
                            getRepository().getProperty(
                                PROP_REPOSITORY_NAME,
                                "RAMADDA Repository")), HtmlUtils.SIZE_60)));

            sb.append(
                HtmlUtils.formEntry(
                    "",
                    "RAMADDA comes with a set of plugins that add functionality. You can install them now or later if you wish."));
            //TODO: read the plugins.xml file and offer more plugins
            //than the hard coded all plugin
            for (String plugin : PluginManager.PLUGINS) {
                sb.append(HtmlUtils.formEntry("",
                        HtmlUtils.checkbox("plugin." + plugin, "true", true)
                        + " " + "Install plugin: "
                        + IOUtil.stripExtension(IOUtil.getFileTail(plugin))));
            }
            sb.append(HtmlUtils.formTableClose());
            sb.append(HtmlUtils.br());
            sb.append(HtmlUtils.submit(msg("Initialize Server")));
        }


        StringBuffer finalSB = new StringBuffer();
        finalSB.append(request.formPost(getRepository().URL_INSTALL));
        finalSB.append(HtmlUtils.sectionOpen(null, false));
        finalSB.append(HtmlUtils.h2(title));
        finalSB.append(sb);
        finalSB.append(HtmlUtils.sectionClose());
        finalSB.append(HtmlUtils.formClose());

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
            IOUtil.close(fis);
        }

        if (initEntriesXml == null) {
            initEntriesXml = getRepository().getResource(
                "/org/ramadda/repository/resources/install/initentries.xml");
        }
        Element root       = XmlUtil.getRoot(initEntriesXml);
        Request tmpRequest = getRepository().getRequest(user);
        //        System.err.println("entry xml");
        List<Entry> newEntries =
            getEntryManager().processEntryXml(tmpRequest, root, null,
                new Hashtable<String, File>());
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

        return makeResult(request, "Administration",
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
        request.ensureAuthToken();

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
        sb.append("<p>");
        request.formPostWithAuthToken(sb, URL_ADMIN_STARTSTOP,
                                      " name=\"admin\"");

        if ( !getDatabaseManager().hasConnection()) {
            sb.append(HtmlUtils.hidden(ARG_ADMIN_WHAT, "restart"));
            sb.append(HtmlUtils.submit("Restart Database"));
        } else {
            sb.append(HtmlUtils.hidden(ARG_ADMIN_WHAT, "shutdown"));
            sb.append(HtmlUtils.submit("Shut Down Database"));
        }
        sb.append("</form>");

        return makeResult(request, "Administration", sb);

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
        sb.append(HtmlUtils.formTable());
        sb.append(HtmlUtils.row(HtmlUtils.cols("Name", "Admin", "Actions")));
        for (ApiMethod apiMethod : apiMethods) {
            sb.append(HtmlUtils.row(HtmlUtils.cols(apiMethod.getName(),
                    "" + apiMethod.getMustBeAdmin(),
                    StringUtil.join(",", apiMethod.getActions()))));
        }
        sb.append(HtmlUtils.formTableClose());

        return makeResult(request, "Administration", sb);
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
        sb.append(header("Database Tables"));
        sb.append(getDatabaseManager().getDbMetaData());

        return makeResult(request, "Administration", sb);
    }

    /** _more_ */
    private boolean amDumpingDb = false;

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
        if (amDumpingDb) {
            StringBuffer sb = new StringBuffer(
                                  getPageHandler().showDialogWarning(
                                      "Currently exporting the database"));

            return makeResult(request, msg("Database export"), sb);
        }


        ActionManager.Action action = new ActionManager.Action() {
            public void run(Object actionId) throws Exception {
                dumpDatabase(actionId);
            }
        };
        String href = HtmlUtils.href(request.makeUrl(URL_ADMIN_CLEANUP),
                                     "Continue");

        Result result = getActionManager().doAction(request, action,
                            "Dumping database", href);

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
            IOUtil.close(bos);
            IOUtil.close(fos);

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
        getPageHandler().makeLinksHeader(request, headerSB, adminUrls, "");
        headerSB.append(sb);

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
        sb.append(HtmlUtils.href(request.makeUrl(URL_ADMIN_STARTSTOP),
                                 "Administer Database"));
        sb.append("<li> ");
        sb.append(HtmlUtils.href(request.makeUrl(URL_ADMIN_TABLES),
                                 "Show Tables"));
        sb.append("<li> ");
        sb.append(HtmlUtils.href(request.makeUrl(URL_ADMIN_STATS), "System"));
        sb.append("<li> ");
        sb.append(HtmlUtils.href(request.makeUrl(URL_ADMIN_SQL),
                                 "Execute SQL"));
        sb.append("</ul>");

        return makeResult(request, "Administration", sb);

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

        StringBuilder sb = new StringBuilder();
        request.formPostWithAuthToken(sb, URL_ADMIN_SETTINGS_DO, null);
        String size = HtmlUtils.SIZE_60;
        sb.append(HtmlUtils.sectionOpen(null, false));
        sb.append(HtmlUtils.submit(msg("Change Settings")));
        sb.append(HtmlUtils.p());
        StringBuffer csb = new StringBuffer();
        csb.append(HtmlUtils.formTable());


        /*
        csb.append(HtmlUtils.row(HtmlUtils.colspan(msgHeader("Registration"),
                2)));


        String regKey = getRepository().getProperty(PROP_REGISTER_KEY, "");
        csb.append(HtmlUtils.formEntry(msgLabel("Key"),
                                       HtmlUtils.input(PROP_REGISTER_KEY,
                                           regKey, HtmlUtils.SIZE_90)));
        if (isRegistered()) {
            csb.append(HtmlUtils.formEntry("",
                                           "Registered for " + numberUsers
                                           + " users"));
        } else {
            if (Utils.stringDefined(regKey)) {
                csb.append(HtmlUtils.formEntry("", "Invalid registration"));
            } else {
                csb.append(HtmlUtils.formEntry("", "Not registered"));
            }
            }
*/

        csb.append(
            HtmlUtils.row(
                HtmlUtils.colspan(msgHeader("Site Information"), 2)));
        String allSslCbx =
            HtmlUtils.space(3)
            + HtmlUtils.checkbox(
                PROP_ACCESS_ALLSSL, "true",
                getRepository().getProperty(PROP_ACCESS_ALLSSL, false)) + " "
                    + msg("Force all connections to be secure");

        String sslMsg =
            "Note: To enable ssl see the <a target=\"_help\" href=\"http://ramadda.org/repository/userguide/installing.html#ssl\">installation guide</a>";
        csb.append(
            HtmlUtils.formEntryTop(
                msgLabel("SSL"),
                getPageHandler().showDialogNote(sslMsg) + HtmlUtils.br()
                + allSslCbx));



        csb.append(
            HtmlUtils.formEntry(
                msgLabel("Hostname"),
                HtmlUtils.input(
                    PROP_HOSTNAME,
                    getRepository().getProperty(PROP_HOSTNAME, ""),
                    HtmlUtils.SIZE_40)));

        csb.append(
            HtmlUtils.formEntry(
                msgLabel("HTTP Port"),
                HtmlUtils.input(
                    PROP_PORT, getRepository().getProperty(PROP_PORT, ""),
                    HtmlUtils.SIZE_5)));

        String cbx = HtmlUtils.checkbox(
                         PROP_USE_FIXED_HOSTNAME, "true",
                         getRepository().getProperty(
                             PROP_USE_FIXED_HOSTNAME, false));


        csb.append(
            HtmlUtils.formEntry(
                msgLabel("Absolute URLs"),
                cbx + HtmlUtils.space(2)
                + "Use the fixed hostname:port in absolute URLs instead of the request's info"));

        //Force the creation of some of the managers
        getRepository().getMailManager();
        getRepository().getFtpManager();
        getRepository().getMapManager();


        for (RepositoryManager manager :
                getRepository().getRepositoryManagers()) {
            manager.addAdminSettings(request, csb);
        }


        csb.append(
            HtmlUtils.row(
                HtmlUtils.colspan(msgHeader("Extra Properties"), 2)));
        csb.append(
            HtmlUtils.formEntryTop(
                msgLabel("Properties"),
                HtmlUtils.textArea(
                    PROP_PROPERTIES,
                    getRepository().getProperty(
                        PROP_PROPERTIES,
                        "#add extra properties\n#name=value\n#ramadda.html.template.default=fixedmapheader\n\n"), 5, 60)));






        StringBuffer dsb = new StringBuffer();

        dsb.append(HtmlUtils.formTable());
        dsb.append(
            HtmlUtils.formEntry(
                msgLabel("Title"),
                HtmlUtils.input(
                    PROP_REPOSITORY_NAME,
                    getRepository().getProperty(
                        PROP_REPOSITORY_NAME, "Repository"), size)));
        dsb.append(
            HtmlUtils.formEntryTop(
                msgLabel("Description"),
                HtmlUtils.textArea(
                    PROP_REPOSITORY_DESCRIPTION,
                    getRepository().getProperty(
                        PROP_REPOSITORY_DESCRIPTION, ""), 5, 60)));

        dsb.append(
            HtmlUtils.formEntryTop(
                msgLabel("Footer"),
                HtmlUtils.textArea(
                    PROP_HTML_FOOTER,
                    getRepository().getProperty(PROP_HTML_FOOTER, ""), 5,
                    60)));

        dsb.append(
            HtmlUtils.formEntry(
                msgLabel("Logo Image Location"),
                HtmlUtils.input(
                    PROP_LOGO_IMAGE,
                    getRepository().getProperty(PROP_LOGO_IMAGE, ""), size)));
        dsb.append(
            HtmlUtils.formEntry(
                msgLabel("Logo URL"),
                HtmlUtils.input(
                    PROP_LOGO_URL,
                    getRepository().getProperty(PROP_LOGO_URL, ""), size)));



        dsb.append(HtmlUtils.formEntry("", msg("System Message")));
        String systemMessage = getRepository().getSystemMessage();

        dsb.append(
            HtmlUtils.formEntry(
                msgLabel("Message"),
                HtmlUtils.textArea(
                    PROP_SYSTEM_MESSAGE, ((systemMessage == null)
                                          ? ""
                                          : systemMessage), 5, 60)));


        String phrases = getRepository().getProperty(PROP_ADMIN_PHRASES,
                             (String) null);
        if (phrases == null) {
            phrases = "#label=new label to use\n#e.g.: Foo=Bar";
        }

        dsb.append(
            HtmlUtils.formEntryTop(
                msgLabel("Ignore Page Styles"),
                HtmlUtils.checkbox(
                    PROP_NOSTYLE, "true",
                    getRepository().getProperty(PROP_NOSTYLE, false))));


        dsb.append(
            HtmlUtils.formEntryTop(
                msgLabel("Translations"),
                HtmlUtils.textArea(PROP_ADMIN_PHRASES, phrases, 5, 60)));


        /*
        dsb.append(
            HtmlUtils.formEntryTop(
                msgLabel("Facebook Comments API Key"),
                HtmlUtils.input(
                    PROP_FACEBOOK_CONNECT_KEY,
                    getRepository().getProperty(PROP_FACEBOOK_CONNECT_KEY, ""), size)));
        dsb.append(
            HtmlUtils.formEntryTop(
                msgLabel("Enable Ratings"),
                HtmlUtils.checkbox(
                    PROP_RATINGS_ENABLE, "true",
                    getRepository().getProperty(PROP_RATINGS_ENABLE, false))));

        */

        dsb.append(HtmlUtils.formEntryTop(msgLabel("Google Maps Keys"), "<table><tr valign=top><td>"
                + HtmlUtils.textArea(PROP_GOOGLEAPIKEYS, getRepository().getProperty(PROP_GOOGLEAPIKEYS, ""), 5, 80)
                + "</td><td>One per line:<br><i>host domain;apikey</i><br>e.g.:<i>www.yoursite.edu;google api key</i></table>"));





        StringBuffer asb = new StringBuffer();
        asb.append(HtmlUtils.formTable());


        asb.append(HtmlUtils.row(HtmlUtils.colspan(msgHeader("Site Access"),
                2)));
        asb.append(
            HtmlUtils.formEntry(
                "",
                HtmlUtils.checkbox(
                    PROP_ACCESS_ADMINONLY, "true",
                    getRepository().getProperty(
                        PROP_ACCESS_ADMINONLY, false)) + HtmlUtils.space(2)
                            + msg("Only allows administrators to access the site")));
        asb.append(
            HtmlUtils.formEntry(
                "",
                HtmlUtils.checkbox(
                    PROP_ACCESS_REQUIRELOGIN, "true",
                    getRepository().getProperty(
                        PROP_ACCESS_REQUIRELOGIN, false)) + HtmlUtils.space(
                            2) + msg("Require login to access the site")));

        asb.append(
            HtmlUtils.formEntry(
                "",
                HtmlUtils.checkbox(
                    PROP_ACCESS_NOBOTS, "true",
                    getRepository().getProperty(
                        PROP_ACCESS_NOBOTS, false)) + HtmlUtils.space(2)
                            + msg("Disallow robots")));



        asb.append(HtmlUtils.colspan(msgHeader("Anonymous Uploads"), 2));
        asb.append(
            HtmlUtils.formEntry(
                msgLabel("Max directory size"),
                HtmlUtils.input(
                    PROP_UPLOAD_MAXSIZEGB,
                    "" + getRepository().getProperty(
                        PROP_UPLOAD_MAXSIZEGB,
                        10.0), HtmlUtils.SIZE_10) + " (GBytes)"));


        asb.append(HtmlUtils.colspan(msgHeader("Cache Size"), 2));
        asb.append(
            HtmlUtils.formEntry(
                msgLabel("Size"),
                HtmlUtils.input(
                    PROP_CACHE_MAXSIZEGB,
                    "" + getRepository().getProperty(
                        PROP_CACHE_MAXSIZEGB,
                        10.0), HtmlUtils.SIZE_10) + " (GBytes)"));



        asb.append(HtmlUtils.colspan(msgHeader("File Access"), 2));
        String fileWidget = HtmlUtils.textArea(
                                PROP_LOCALFILEPATHS,
                                getRepository().getProperty(
                                    PROP_LOCALFILEPATHS, ""), 5, 40);
        String fileLabel =
            msg("Enter one server file system directory per line")
            + HtmlUtils.br()
            + msg("Directories that RAMADDA is allowed to serve files from")
            + " " + "(e.g., from harvesters or the server file view entries)";
        asb.append(HtmlUtils.formEntryTop(msgLabel("File system access"),
                                          "<table><tr valign=top><td>"
                                          + fileWidget + "</td><td>"
                                          + fileLabel
                                          + "</td></tr></table>"));









        for (AdminHandler adminHandler : adminHandlers) {
            adminHandler.addToAdminSettingsForm(BLOCK_SITE, csb);
            adminHandler.addToAdminSettingsForm(BLOCK_DISPLAY, dsb);
            adminHandler.addToAdminSettingsForm(BLOCK_ACCESS, asb);
        }
        csb.append(HtmlUtils.formTableClose());
        dsb.append(HtmlUtils.formTableClose());
        asb.append(HtmlUtils.formTableClose());


        StringBuffer osb = new StringBuffer();
        osb.append(HtmlUtils.formTable());


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
                    outputSB.append(HtmlUtils.p());
                }
                lastCategoryName = type.getGroupName();
                outputSB.append(
                    HtmlUtils.div(
                        lastCategoryName, HtmlUtils.cssClass(
                            CSS_CLASS_HEADING_2)) + "\n<div style=\"margin-left:20px\">");
            }
            outputSB.append(HtmlUtils.checkbox("outputtype." + type.getId(),
                    "true", ok));
            outputSB.append(HtmlUtils.space(1));
            outputSB.append(type.getLabel());
            outputSB.append(HtmlUtils.space(3));
        }
        outputSB.append("</div>\n");
        String outputDiv = HtmlUtils.div(outputSB.toString(),
                                         HtmlUtils.cssClass("scrollablediv"));
        osb.append("\n");
        String doAllOutput = HtmlUtils.checkbox("outputtype.all", "true",
                                 false) + HtmlUtils.space(1) + msg("Use all");
        osb.append(HtmlUtils.formEntryTop("", doAllOutput + outputDiv));
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

        osb.append(HtmlUtils.formEntry("&nbsp;<p>", ""));
        osb.append(HtmlUtils.formTableClose());


        List<String> titles = new ArrayList<String>();
        List<String> tabs   = new ArrayList<String>();

        titles.add("Site and Contact Information");
        tabs.add(csb.toString());
        titles.add("Access");
        tabs.add(asb.toString());
        titles.add("Display");
        tabs.add(dsb.toString());
        titles.add("Available Output Types");
        tabs.add(osb.toString());
        HtmlUtils.makeAccordion(sb, titles, tabs, true, "ramadda-accordion",
                                null);
        sb.append(HtmlUtils.formClose());
        sb.append(HtmlUtils.sectionClose());

        return makeResult(request, msg("Settings"), sb);
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
            sb.append(HtmlUtils.formEntry("Registered", regId));
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
        return HtmlUtils.makeShowHideBlock(
            msg(title),
            HtmlUtils.div(contents, HtmlUtils.cssClass("admin-block-inner")),
            false, HtmlUtils.cssClass(CSS_CLASS_HEADING_2),
            HtmlUtils.cssClass("admin-block"));
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

        request.ensureAuthToken();

        for (RepositoryManager manager :
                getRepository().getRepositoryManagers()) {
            manager.applyAdminSettings(request);
        }

        getRepository().writeGlobal(request, PROP_PROPERTIES, true);
        getRepository().writeGlobal(request, PROP_LOGO_URL, true);
        getRepository().writeGlobal(request, PROP_LOGO_IMAGE, true);
        getRepository().writeGlobal(request, PROP_REPOSITORY_NAME, true);
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
        /*
        checkRegistration();
        */

        getRepository().setLocalFilePaths();
        getRepository().initAttributes();
        getRepository().clearCache();


        List<OutputHandler> outputHandlers =
            getRepository().getOutputHandlers();
        for (OutputHandler outputHandler : outputHandlers) {
            outputHandler.applySettings(request);
        }

        List<OutputType> types = getRepository().getOutputTypes();
        boolean          doAll = request.get("outputtype.all", false);
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


        for (AdminHandler adminHandler : adminHandlers) {
            adminHandler.applyAdminSettingsForm(request);
        }

        //Now re-read all of the globals so we pick up any deletes
        getRepository().readDatabaseProperties();

        //Tell the other repositoryManagers that the settings changed
        for (RepositoryManager repositoryManager :
                getRepository().getRepositoryManagers()) {
            repositoryManager.adminSettingsChanged();
        }


        return new Result(request.makeUrl(URL_ADMIN_SETTINGS));

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
        sb.append(HtmlUtils.sectionOpen(null, false));
        sb.append(msgHeader("Access Overview"));

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
            permissions.add(new Permission(action, role));
        }

        sb.append("<table cellspacing=\"0\" cellpadding=\"0\">");
        sb.append(HtmlUtils.row(HtmlUtils.cols(HtmlUtils.space(10),
                HtmlUtils.b(msg("Action")) + HtmlUtils.space(3),
                HtmlUtils.b(msg("Role")))));
        for (String id : ids) {
            Entry entry = getEntryManager().getEntry(request, id);
            sb.append(
                HtmlUtils.row(
                    HtmlUtils.colspan(
                        getPageHandler().getBreadCrumbs(
                            request, entry, null,
                            getRepository().URL_ACCESS_FORM, 80), 3)));
            List<Permission> permissions =
                (List<Permission>) idToPermissions.get(id);
            for (Permission permission : permissions) {
                sb.append(HtmlUtils.row(HtmlUtils.cols("",
                        permission.getAction(),
                        permission.getRoles().get(0))));

            }
            sb.append(HtmlUtils.row(HtmlUtils.colspan("<hr>", 3)));
        }
        sb.append("</table>");
        sb.append(HtmlUtils.sectionClose());

        return makeResult(request, msg("Access Overview"), sb);
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
        stateSB.append(HtmlUtils.formTable());
        getStorageManager().addInfo(stateSB);
        getDatabaseManager().addInfo(stateSB);
        stateSB.append(HtmlUtils.formTableClose());



        StringBuffer statusSB = new StringBuffer();
        statusSB.append(HtmlUtils.formTable());
        statusSB.append(
            HtmlUtils.formEntry(
                msgLabel("Version"),
                getRepository().getProperty(PROP_BUILD_VERSION, "1.0")));
        statusSB.append(
            HtmlUtils.formEntry(
                msgLabel("Build Date"),
                getRepository().getProperty(PROP_BUILD_DATE, "N/A")));
        statusSB.append(
            HtmlUtils.formEntry(
                msgLabel("Java Version"),
                getRepository().getProperty(PROP_JAVA_VERSION, "N/A")));


        if (request.get("gc", false)) {
            Misc.gc();
        }

        appendMemory(statusSB);
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
        /*
        statusSB.append(HtmlUtils.formEntry(msgLabel("Up Time"),
                                           fmt.format((double) (uptime / 1000
                                               / 60)) + " "
                                                   + msg("minutes")));
        */
        statusSB.append(HtmlUtils.formEntry(msgLabel("Total # Requests"),
                                            getLogManager().getRequestCount()
                                            + ""));


        getEntryManager().addStatusInfo(statusSB);

        statusSB.append(HtmlUtils.formTableClose());



        StringBuffer outputSB = new StringBuffer();
        outputSB.append(HtmlUtils.formTable());
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
        outputSB.append(HtmlUtils.formTableClose());


        String         props  = getRepository().getPropertiesListing();


        StringBuffer   apiSB  = new StringBuffer();
        List<Object[]> tuples = new ArrayList<Object[]>();
        apiSB.append(HtmlUtils.formTable());
        for (ApiMethod apiMethod :
                getRepository().getApiManager().getApiMethods()) {
            if (apiMethod.getNumberOfCalls() < 1) {
                continue;
            }
            tuples.add(new Object[] {
                new Integer(apiMethod.getNumberOfCalls()),
                apiMethod });

        }
        tuples = (List<Object[]>) Misc.sortTuples(tuples, false);
        for (Object[] tuple : tuples) {
            ApiMethod apiMethod = (ApiMethod) tuple[1];
            apiSB.append(HtmlUtils.formEntry(apiMethod.getName(),
                                             "# " + msgLabel("Calls")
                                             + apiMethod.getNumberOfCalls()));
        }


        apiSB.append(HtmlUtils.formTableClose());


        StringBuffer dbSB = new StringBuffer();

        getDatabaseManager().addStatistics(request, dbSB);


        StringBuffer sb = new StringBuffer();
        sb.append(HtmlUtils.sectionOpen(null, false));
        List<String> titles = new ArrayList<String>();
        List<String> tabs   = new ArrayList<String>();

        titles.add(msg("System Status"));
        tabs.add(HtmlUtils.section(statusSB.toString()));

        titles.add(msg("System Disk"));
        tabs.add(HtmlUtils.section(stateSB.toString()));

        StringBuffer pluginsSB = new StringBuffer();
        getRepository().getPluginManager().addStatusInfo(request, pluginsSB);

        titles.add(msg("Plugins"));
        tabs.add(HtmlUtils.section(pluginsSB.toString()));

        titles.add(msg("API"));
        tabs.add(HtmlUtils.section(apiSB.toString()));

        titles.add(msg("Properties"));
        tabs.add(HtmlUtils.section(props));

        titles.add(msg("Output Handlers"));
        tabs.add(HtmlUtils.section(outputSB.toString()));

        titles.add(msg("Database Statistics"));
        tabs.add(HtmlUtils.section(dbSB.toString()));

        HtmlUtils.makeAccordion(sb, titles, tabs, true, "ramadda-accordion",
                                null);

        sb.append(HtmlUtils.sectionClose());

        return makeResult(request, msg("System"), sb);
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

        if ( !getRepository().getProperty(PROP_ADMIN_INCLUDESQL, false)) {
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

        StringBuffer sb = new StringBuffer();
        sb.append(HtmlUtils.sectionOpen(null, false));
        //        sb.append(msgHeader("SQL"));
        sb.append(HtmlUtils.p());
        sb.append(HtmlUtils.href(request.makeUrl(URL_ADMIN_TABLES),
                                 msg("View Schema")));
        sb.append(HtmlUtils.bold("&nbsp;|&nbsp;"));
        sb.append(HtmlUtils.href(request.makeUrl(URL_ADMIN_DUMPDB),
                                 msg("Dump Database")));
        sb.append(HtmlUtils.p());
        request.uploadFormWithAuthToken(sb, URL_ADMIN_SQL);

        sb.append(HtmlUtils.submit(msg("Execute")));
        sb.append(HtmlUtils.b("Note: be careful what you do here!"));
        sb.append(HtmlUtils.br());
        sb.append(HtmlUtils.textArea(ARG_QUERY, (bulkLoad
                ? ""
                : (query == null)
                  ? BLANK
                  : query), 10, 100));
        sb.append(HtmlUtils.p());
        sb.append("SQL File: ");
        sb.append(HtmlUtils.fileInput(ARG_SQLFILE, HtmlUtils.SIZE_60));
        sb.append(HtmlUtils.formClose());
        sb.append(HtmlUtils.sectionClose());
        sb.append("<table>");
        if (query == null) {
            return makeResult(request, msg("SQL"), sb);
        }

        long t1 = System.currentTimeMillis();

        request.ensureAuthToken();
        if (bulkLoad) {
            getDatabaseManager().loadSql(query, false, true);

            return makeResult(request, msg("SQL"),
                              new StringBuffer("Executed SQL" + "<P>"
                                  + HtmlUtils.space(1) + sb.toString()));

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
                    sb.append("<table><tr>");
                    for (int i = 0; i < rsmd.getColumnCount(); i++) {
                        sb.append(
                            HtmlUtils.col(
                                HtmlUtils.bold(rsmd.getColumnLabel(i + 1))));
                    }
                    sb.append("</tr>");
                }
                sb.append("<tr valign=\"top\">");
                while (colcnt < rsmd.getColumnCount()) {
                    colcnt++;
                    if (rsmd.getColumnType(colcnt)
                            == java.sql.Types.TIMESTAMP) {
                        Date dttm = results.getTimestamp(colcnt,
                                        Repository.calendar);
                        sb.append(HtmlUtils.col(formatDate(request, dttm)));
                    } else {
                        String s = results.getString(colcnt);
                        if (s == null) {
                            s = "_null_";
                        }
                        s = HtmlUtils.entityEncode(s);
                        if (s.length() > 100) {
                            sb.append(
                                HtmlUtils.col(
                                    HtmlUtils.textArea("dummy", s, 5, 50)));
                        } else {
                            sb.append(HtmlUtils.col(HtmlUtils.pre(s)));
                        }
                    }
                }
                sb.append("</tr>\n");
                //                if (cnt++ > 1000) {
                //                    sb.append(HtmlUtils.row("..."));
                //                    break;
                //                }
            }
            sb.append("</table>");
            long t2 = System.currentTimeMillis();
            getRepository().clearCache();
            getRepository().readDatabaseProperties();

            return makeResult(request, msg("SQL"),
                              new StringBuffer(msgLabel("Fetched rows") + cnt
                                  + HtmlUtils.space(1) + msgLabel("in")
                                  + (t2 - t1) + "ms <p>" + sb.toString()));
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

            return makeResult(request, msg("Scan"),
                              new StringBuffer("Deleted"));
        }

        return makeResult(request, msg("Scan"), sb);
    }

    /**
     * _more_
     *
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    private void appendMemory(Appendable sb) throws Exception {
        DecimalFormat fmt        = new DecimalFormat("#0");
        double        maxMemory  = (double) Runtime.getRuntime().maxMemory();
        double        freeMemory = (double) Runtime.getRuntime().freeMemory();
        double totalMemory       =
            (double) Runtime.getRuntime().totalMemory();
        double        usedMemory = (totalMemory - freeMemory);
        sb.append(HtmlUtils.formEntry("Max Memory:",
                                      fmt.format(maxMemory / 1000000)
                                      + " (MB)"));
        sb.append(HtmlUtils.formEntry("Total Memory:",
                                      fmt.format(totalMemory / 1000000)
                                      + " (MB)"));
        sb.append(HtmlUtils.formEntry("Free Memory:",
                                      fmt.format(freeMemory / 1000000)
                                      + " (MB)"));
        sb.append(HtmlUtils.formEntry("Used Memory:",
                                      fmt.format(usedMemory / 1000000)
                                      + " (MB)"));

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
        sb.append(HtmlUtils.formTable());
        appendMemory(sb);
        sb.append(HtmlUtils.formEntry("Start Time:",
                                      "" + getRepository().getStartTime()));


        long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
        //sb.append(HtmlUtils.formEntry("Up Time:",
        //                             fmt.format((double) (uptime / 1000
        //                                 / 60)) + " " + msg("minutes")));
        sb.append(HtmlUtils.formEntry("Up Time:", formatUptime(uptime)));

        sb.append(HtmlUtils.formTableClose());
        sb.append(HtmlUtils.makeShowHideBlock(msg("Stack"),
                "<pre>" + LogUtil.getStackDump(true) + "</pre>", false));

        return makeResult(request, msg("Stack Trace"), sb);
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
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result adminCleanup(Request request) throws Exception {

        StringBuffer  sb         = new StringBuffer();

        StringBuilder filePathSB = new StringBuilder();
        filePathSB.append(HtmlUtils.sectionOpen(null, false));
        filePathSB.append(HtmlUtils.h3("Change file paths"));

        request.formPostWithAuthToken(filePathSB, URL_ADMIN_CLEANUP, "");
        filePathSB.append(
            "Change the stored file path for all entries that match the following pattern");
        filePathSB.append(HtmlUtils.formTable());
        filePathSB.append(HtmlUtils.formEntry(msgLabel("File Pattern"),
                HtmlUtils.input(ARG_CHANGEPATHS_PATTERN,
                                request.getString(ARG_CHANGEPATHS_PATTERN,
                                    ""), HtmlUtils.SIZE_50)));
        filePathSB.append(HtmlUtils.formEntry(msgLabel("Change to"),
                HtmlUtils.input(ARG_CHANGEPATHS_TO,
                                request.getString(ARG_CHANGEPATHS_TO, ""),
                                HtmlUtils.SIZE_50)));

        filePathSB.append(HtmlUtils.formTableClose());
        filePathSB.append(HtmlUtils.submit(msg("Change file paths"),
                                           ACTION_CHANGEPATHS));
        filePathSB.append(HtmlUtils.space(2));
        filePathSB.append(HtmlUtils.checkbox(ARG_CHANGEPATHS_CONFIRM, "true",
                                             false));

        filePathSB.append(HtmlUtils.space(2));
        filePathSB.append(
            "Yes, I really want to change all of the file paths");
        filePathSB.append(HtmlUtils.sectionClose());

        filePathSB.append(HtmlUtils.formClose());



        if (request.defined(ACTION_STOP)) {
            runningCleanup = false;
            cleanupTS++;

            return new Result(request.makeUrl(URL_ADMIN_CLEANUP));
        } else if (request.defined(ACTION_START)) {
            //            Misc.run(this, "runDatabaseCleanUp", request);
            Misc.run(this, "runDatabaseOrphanCheck", request);

            return new Result(request.makeUrl(URL_ADMIN_CLEANUP));
        } else if (request.defined(ACTION_DUMPDB)) {
            return adminDbDump(request);
        } else if (request.defined(ACTION_NEWDB)) {
            getDatabaseManager().reInitialize();

            return new Result(request.makeUrl(URL_ADMIN_CLEANUP));
        } else if (request.defined(ACTION_CLEARCACHE)) {
            getRepository().clearAllCaches();
        } else if (request.defined(ACTION_CHANGEPATHS)) {
            if (request.defined(ARG_CHANGEPATHS_PATTERN)) {
                StringBuilder tmp = new StringBuilder();
                getRepository().getEntryManager().changeResourcePaths(
                    request, request.getString(ARG_CHANGEPATHS_PATTERN, ""),
                    request.getString(ARG_CHANGEPATHS_TO, ""), tmp,
                    request.get(ARG_CHANGEPATHS_CONFIRM, false));
                sb.append(
                    HtmlUtils.div(
                        tmp.toString(),
                        HtmlUtils.style(
                            "max-height:300px; overflow-y: auto;")));
            } else {
                sb.append("Change paths fields required");
            }
            sb.append(filePathSB);

            return makeResult(request, msg("Change File Paths"), sb);
        } else if (request.defined(ACTION_SHUTDOWN)) {
            request.ensureAuthToken();
            if (getRepository().getShutdownEnabled()) {
                if (request.get(ARG_SHUTDOWN_CONFIRM, false)) {
                    return processShutdown(request);
                }
            }
        } else if (request.defined(ACTION_PASSWORDS_CLEAR)) {
            request.ensureAuthToken();
            if (request.get(ARG_PASSWORDS_CLEAR_CONFIRM, false)) {
                clearAllPasswords();
                sb.append(
                    "All passwords have been cleared. Make sure you go and change your password immediately");

                return makeResult(request, msg("Cleanup"), sb);
            }
        }


        String status = cleanupStatus.toString();
        if (runningCleanup) {
            sb.append(msg("Database clean up is running"));
            sb.append("<p>");
            sb.append(HtmlUtils.submit(msg("Stop cleanup"), ACTION_STOP));
        } else {
            sb.append(HtmlUtils.p());
            //            sb.append(
            //                msg(
            //                "Cleanup allows you to remove all file entries from the repository database that do not exist on the local file system"));
            //            sb.append("<p>");
            //            sb.append(HtmlUtils.submit(msg("Start cleanup"), ACTION_START));



            request.formPostWithAuthToken(sb, URL_ADMIN_CLEANUP, "");

            sb.append(
                HtmlUtils.section(
                    HtmlUtils.h3(msg("Caches"))
                    + HtmlUtils.submit(
                        msg("Clear all caches"), ACTION_CLEARCACHE)));
            sb.append(HtmlUtils.formClose());




            StringBuffer tmp = new StringBuffer();
            tmp.append(HtmlUtils.submit(msg("Clear all passwords"),
                                        ACTION_PASSWORDS_CLEAR));
            tmp.append(HtmlUtils.space(2));
            tmp.append(HtmlUtils.checkbox(ARG_PASSWORDS_CLEAR_CONFIRM,
                                          "true", false));
            tmp.append(HtmlUtils.space(1));
            tmp.append(msg("Yes, I really want to delete all passwords"));
            tmp.append(HtmlUtils.br());
            tmp.append(
                getPageHandler().showDialogNote(
                    "Note:  All users including you will have to reset their passwords. If you do not have email enabled then only the admin will be able to reset the passwords. So, if you do this then right away, while your session is active, go and change your password. If things go bad and you can't login at all see the  <a href=\"http://ramadda.org/repository/userguide/faq.html#faq1_cat1_6\">FAQ</a> post."));

            request.formPostWithAuthToken(sb, URL_ADMIN_CLEANUP, "");
            sb.append(HtmlUtils.section(HtmlUtils.h3(msg("Clear Passwords"))
                                        + tmp.toString()));
            sb.append(HtmlUtils.formClose());



            request.formPostWithAuthToken(sb, URL_ADMIN_CLEANUP, "");
            sb.append(
                HtmlUtils.section(
                    HtmlUtils.h3(msg("Export Database"))
                    + msg("You can write out the database for backup or transfer to a new database")
                    + "<p>"
                    + HtmlUtils.submit(
                        msg("Export the database"), ACTION_DUMPDB)));

            sb.append(HtmlUtils.formClose());

            sb.append(filePathSB);

            if (getRepository().getShutdownEnabled()) {
                request.formPostWithAuthToken(sb, URL_ADMIN_CLEANUP, "");
                sb.append(HtmlUtils.section(HtmlUtils.h3(msg("Shutdown"))
                        + HtmlUtils.submit(msg("Shutdown server"), ACTION_SHUTDOWN)
                        + HtmlUtils.space(2)
                        + HtmlUtils.checkbox(ARG_SHUTDOWN_CONFIRM, "true", false)
                        + HtmlUtils.space(1)
                        + msg("Yes, I really want to shutdown the server")));
                sb.append(HtmlUtils.formClose());
            }


        }
        sb.append("</form>");
        if (status.length() > 0) {
            sb.append(msgHeader("Cleanup Status"));
            sb.append(status);
        }

        //        sb.append(cnt +" files do not exist in " + (t2-t1) );
        return makeResult(request, msg("Cleanup"), sb);

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
                        + "<br>" + msg("Removed") + HtmlUtils.space(1)
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
