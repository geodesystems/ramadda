/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository;

import org.ramadda.repository.admin.Admin;
import org.ramadda.repository.auth.SessionManager;
import org.ramadda.repository.auth.User;
import org.ramadda.repository.auth.UserManager;
import org.ramadda.repository.database.DatabaseManager;
import org.ramadda.repository.database.Tables;
import org.ramadda.util.HtmlUtils;

import org.ramadda.util.MyTrace;

import org.ramadda.util.Utils;
import org.ramadda.util.sql.Clause;
import org.ramadda.util.sql.SqlUtil;

import ucar.unidata.util.IOUtil;

import java.io.File;
import java.io.FileOutputStream;

import java.sql.ResultSet;
import java.sql.Statement;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.TimeZone;

/**
 */
public class LocalRepositoryManager extends RepositoryManager {

    public static final String PROP_MASTER_ENABLED = "ramadda.master.enabled";

    public static final String ARG_LOCAL_NEW = "local.new";

    public static final String ARG_LOCAL_SURE = "local.sure";

    public static final String ARG_LOCAL_NAME = "local.name";

    public static final String ARG_LOCAL_CONTACT = "local.contact";

    public static final String ARG_LOCAL_ID = "local.id";

    public static final String ARG_LOCAL_ADMIN = "local.admin";

    public static final String ARG_LOCAL_CHANGE = "local.change";

    public static final String STATUS_ACTIVE = "active";

    public static final String STATUS_STOPPED = "stopped";

    public static final String STATUS_DELETED = "deleted";

    public static final String ARG_LOCAL_DELETE = "local_delete";

    public static final String ARG_LOCAL_STOP = "local_stop";

    public static final String ARG_LOCAL_START = "local_start";

    /*
     * Holds the currently running repositories
     */

    private Hashtable<String, Repository> children = new Hashtable<String,
                                                         Repository>();

    private List<String> childrenIds = new ArrayList<String>();

    public LocalRepositoryManager(Repository repository) {
        super(repository);
    }

    @Override
    public void shutdown() throws Exception {
        if (children == null) {
            return;
        }
        super.shutdown();
        for (String childId : childrenIds) {
            Repository childRepository = children.get(childId);
            childRepository.shutdown();
        }
        children = null;
    }

    public void initializeLocalRepositories() throws Exception {
        if ( !getRepository().isMaster()) {
            return;
        }
        for (Local local : readLocals()) {
            if (local.status.equals(STATUS_ACTIVE)) {
                Repository childRepository = startLocalRepository(local.id,
                                                 new Properties());
            }
        }
    }

    private List<Local> readLocals() throws Exception {
        List<Local> locals = new ArrayList<Local>();
        Statement stmt =
            getDatabaseManager().select(Tables.LOCALREPOSITORIES.COLUMNS,
                                        Tables.LOCALREPOSITORIES.NAME,
                                        (Clause) null);
        SqlUtil.Iterator iter = getDatabaseManager().getIterator(stmt);
        ResultSet        results;
        while ((results = iter.getNext()) != null) {
            locals.add(new Local(results
                .getString(Tables.LOCALREPOSITORIES.COL_NODOT_ID), results
                .getString(Tables.LOCALREPOSITORIES.COL_NODOT_EMAIL), results
                .getString(Tables.LOCALREPOSITORIES.COL_NODOT_STATUS)));

        }

        return locals;
    }

    public boolean hasServer(String otherServer) throws Exception {
        return getDatabaseManager().tableContains(Tables.LOCALREPOSITORIES.NAME,
						  Tables.LOCALREPOSITORIES.COL_ID,otherServer);
    }

    public void addChildRepository(Request request, StringBuffer sb,
                                   String repositoryId)
            throws Exception {
        repositoryId = repositoryId.trim();

        //clean up the name
        repositoryId = repositoryId.replaceAll("[^a-zA-Z_0-9]+", "");
        if (repositoryId.length() == 0) {
            throw new IllegalArgumentException("Bad id:" + repositoryId);
        }
        if (hasServer(repositoryId)) {
            throw new IllegalArgumentException(
                "Already have a repository with id:" + repositoryId);
        }

        String password = request.getString(UserManager.ARG_USER_PASSWORD1,
                                            "").trim();
        if (password.length() == 0) {
            sb.append(
                getPageHandler().showDialogError("Password is required"));

            return;
        }

        String     name       = request.getString(ARG_LOCAL_NAME, "");
        String     contact = request.getString(ARG_LOCAL_CONTACT, "").trim();
        Properties properties = new Properties();
        properties.put(PROP_REPOSITORY_NAME, name);
        properties.put(PROP_MASTER_ENABLED, "" + true);
        File ramaddaHomeDir = getHomeDir(repositoryId);
        boolean existedBefore = new File(IOUtil.joinDir(ramaddaHomeDir,
                                    "storage")).exists();

        Repository childRepository = startLocalRepository(repositoryId,
							  properties);
        childRepository.getAdmin().setInstallationComplete(true);
        childRepository.writeGlobal(
            PROP_HOSTNAME, getRepository().getProperty(PROP_HOSTNAME, ""));
        childRepository.writeGlobal(PROP_PORT,
                                    "" + getRepository().getPort());
        childRepository.writeGlobal(
            PROP_ADMIN_SMTP,
            getRepository().getProperty(PROP_ADMIN_SMTP, ""));
        childRepository.writeGlobal(
            PROP_ADMIN_EMAIL,
            getRepository().getProperty(PROP_ADMIN_EMAIL, ""));

        StringBuffer msg            = new StringBuffer();
        String       childUrlPrefix = getChildUrlBase(repositoryId);

        msg.append("Created repository: "
                   + HtmlUtils.href(childUrlPrefix, childUrlPrefix));
        msg.append("<br>");

        String adminId = request.getString(ARG_LOCAL_ADMIN, "").trim();
        if (adminId.length() == 0) {
            adminId = repositoryId + "_admin";
        }

        User user = childRepository.getUserManager().findUser(adminId);
        if (user == null) {
            user = new User(
			    adminId, User.STATUS_ACTIVE,"Administrator", "", "", "","","",
			    childRepository.getUserManager().hashPassword(password), "",
			    true, "", "", false, null,null);
            childRepository.getUserManager().makeOrUpdateUser(user, false);
            msg.append("Created admin: " + user.getId());
            msg.append(HtmlUtils.p());
            if ( !existedBefore) {
                childRepository.getAdmin().addInitEntries(user);
		Entry topEntry = childRepository.getEntryManager().getRootEntry();
		String description =childRepository.getAdmin().getInitDescription(topEntry);
		topEntry.setDescription(description);
		childRepository.getEntryManager().updateEntry(request, topEntry);
            }
        }

        if (msg.length() > 0) {
            sb.append(getPageHandler().showDialogNote(msg.toString()));
        }

        getDatabaseManager().executeInsert(Tables.LOCALREPOSITORIES.INSERT,
                                           new Object[] { repositoryId,
                contact, STATUS_ACTIVE });
        getRepository().addChildRepository(childRepository);
    }

    private String getChildUrlBase(String childId) {
        return getRepository().getUrlBase() + "/repos/" + childId;
    }

    private File getHomeDir(String repositoryId) throws Exception {
        String repositoriesDir =
            getRepository().getProperty("ramadda.master.dir",
                                        "%repositorydir%/repositories");
        repositoriesDir = repositoriesDir.replace("%repositorydir%",
                getStorageManager().getRepositoryDir().toString());

        return new File(IOUtil.joinDir(repositoriesDir,
                                       "repository_" + repositoryId));
    }

    private Repository startLocalRepository(String repositoryId,
                                            Properties properties)
            throws Exception {
        System.err.println("RAMADDA: starting local repository:"
                           + repositoryId);
        File ramaddaHomeDir = getHomeDir(repositoryId);
        ramaddaHomeDir.mkdirs();
        File otherPluginDir = new File(IOUtil.joinDir(ramaddaHomeDir,
                                  "plugins"));
        //TODO: Do we always copy the plugins on start up or just the first time
        //        if(!otherPluginDir.exists()) {
        otherPluginDir.mkdirs();
        for (File currentPluginFile : otherPluginDir.listFiles()) {
            currentPluginFile.delete();
        }

        for (File myPluginFile :
                getStorageManager().getPluginsDir().listFiles()) {
            if ( !myPluginFile.isFile()) {
                continue;
            }
            IOUtil.copyFile(myPluginFile, otherPluginDir);
        }
        //}

        //Write out a random seed and iteration if we haven't done so already
        File passwordPropertiesFile = new File(IOUtil.joinDir(ramaddaHomeDir,
                                          "password.properties"));
        if ( !passwordPropertiesFile.exists()) {
            StringBuffer propsSB = new StringBuffer();
            String alpha =
                "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
            Random random = new Random();
            String seed1  = "";
            String seed2  = "";
            for (int i = 0; i < 20; i++) {
                seed1 += alpha.charAt(random.nextInt(alpha.length()));
                seed2 += alpha.charAt(random.nextInt(alpha.length()));
            }
            propsSB.append(
                "#generated password salts\n#do not change these or your passwords will be invalidated\n\n");
            propsSB.append(UserManager.PROP_PASSWORD_SALT + "=" + seed1
                           + "\n");
            IOUtil.writeFile(passwordPropertiesFile, propsSB.toString());
        }

        //Copy the keystore and the ssl.properties??
        properties.put(PROP_HTML_URLBASE, getChildUrlBase(repositoryId));
        properties.put(PROP_REPOSITORY_HOME, ramaddaHomeDir.toString());

        //We let the children be masters for now
        properties.put(PROP_MASTER_ENABLED, "true");

        File propertiesFile = new File(IOUtil.joinDir(ramaddaHomeDir,
                                  "repository.properties"));
        properties.store(new FileOutputStream(propertiesFile),
                         "Generated by RAMADDA and company");
        Repository childRepository = new Repository(getRepository(),
                                         new String[] {},
                                         getRepository().getPort());
        childRepository.init(properties);
        childRepository.setActive(true);
        getRepository().addChildRepository(childRepository);
        children.put(repositoryId, childRepository);
        childrenIds.add(repositoryId);

        return childRepository;
    }

    public Result handleRepos(Request request) throws Exception {
        if ( !getRepository().isMaster()) {
            throw new IllegalArgumentException("Not a master repo");
        }
        String path = request.getRequestPath();
        //        child:/repository/repos/testitout5/:
        //        child:/repository/repos/test6/:

        Repository theRepository = null;
        //        System.err.println("path:" + path+":");
        for (Repository childRepository :
                getRepository().getChildRepositories()) {
            String childUrl = childRepository.getUrlBase() + "/";
            //            System.err.println ("child:" + childUrl+":");
            if (path.startsWith(childUrl)
                    || childRepository.getUrlBase().equals(path)) {
                Request originalRequest = request;
                request = request.cloneMe(childRepository);
		request.setCloned(false);
                originalRequest.setSessionHasBeenHandled(true);
                //                request.setRequestPath(suffix);
                request.setUser(null);
                request.setSessionId(null);
                //                System.err.println ("RAMADDA.Dispatch:" + originalRequest + " " + originalRequest.getSecure() + " new:" +
                //                                    request + " "  + request.getSecure());
                //                System.err.println (getRepository().getUrlBase() + ": Local dispatching:" + request.getRequestPath());
                Result result = childRepository.handleRequest(request);
                //                System.err.println (getRepository().getUrlBase() + ": done dispatching:" + request.getSessionId());

                result.setShouldDecorate(false);

                return result;
            }
        }

        StringBuffer sb = new StringBuffer();
        sb.append(
            getPageHandler().showDialogWarning(
                "Could not find requested repository.<br>Perhaps it hasn't started up yet"));

        return new Result("", sb);
    }

    public Result adminLocal(Request request) throws Exception {

        if ( !getRepository().isMaster()) {
            throw new IllegalArgumentException("Not a master repo");
        }

        StringBuffer sb = new StringBuffer();
        sb.append(HtmlUtils.sectionOpen());

        if (request.defined(ARG_LOCAL_NEW)) {
            getAuthManager().ensureAuthToken(request);
            if (request.get(ARG_LOCAL_SURE, false)) {
                processLocalNew(request, sb);
            } else {
                sb.append(
                    getPageHandler().showDialogWarning(
                        "You didn't select \"I'm sure\""));
            }
        }

        if (request.defined(ARG_LOCAL_CHANGE)) {
            getAuthManager().ensureAuthToken(request);
            processLocalChange(request, sb);
        }

        sb.append("<table width=80% border=0>");
        boolean didone = false;
        for (Local local : readLocals()) {
            if ( !didone) {
                sb.append(
                    HtmlUtils.row(
                        HtmlUtils.cols(
                            HtmlUtils.insetDiv(
                                HtmlUtils.b("Repository"), 10, 10, 10,
                                10), HtmlUtils.insetDiv(
                                    HtmlUtils.b(""), 10, 10, 10,
                                    10)), " valign=bottom "));
            }
            didone = true;
            StringBuffer statusSB = new StringBuffer();

            request.formPostWithAuthToken(statusSB,
                                          getAdmin().URL_ADMIN_LOCAL);
            statusSB.append(msgLabel("Status") + " " + local.status);
            statusSB.append(HtmlUtils.br());
            statusSB.append(HtmlUtils.hidden(ARG_LOCAL_ID, local.id));
            statusSB.append(HtmlUtils.hidden(ARG_LOCAL_CHANGE, "true"));

            if (local.status.equals(STATUS_ACTIVE)) {
                statusSB.append(HtmlUtils.submit("Stop Repository",
                        ARG_LOCAL_STOP));
            } else if (local.status.equals(STATUS_STOPPED)) {
                statusSB.append(HtmlUtils.submit("Start Repository",
                        ARG_LOCAL_START));
            }

            if (local.status.equals(STATUS_STOPPED)) {
                statusSB.append(HtmlUtils.space(1));
                statusSB.append(HtmlUtils.hidden(ARG_LOCAL_ID, local.id));
                statusSB.append(HtmlUtils.submit("Remove Repository",
                        ARG_LOCAL_DELETE));
                statusSB.append(HtmlUtils.checkbox(ARG_LOCAL_SURE, "true",
                        false) + " " + msg("Yes, remove this repository"));
            }
            statusSB.append(HtmlUtils.formClose());

            String link =
                HtmlUtils.insetDiv(HtmlUtils.href(getChildUrlBase(local.id),
                    local.id), 10, 10, 10, 10);
            String form = HtmlUtils.insetDiv(statusSB.toString(), 10, 10, 10,
                                             10);
            sb.append(HtmlUtils.row(HtmlUtils.cols(link, form),
                                    " valign=top "));
        }
        sb.append(HtmlUtils.formTableClose());
        sb.append(HtmlUtils.p());

        request.formPostWithAuthToken(sb, getAdmin().URL_ADMIN_LOCAL);
        sb.append(formHeader(msg("New repository")));
        String required =
            " "
            + HtmlUtils.span("* required",
                             HtmlUtils.cssClass(CSS_CLASS_REQUIRED));
        sb.append(HtmlUtils.formTable());
        sb.append(HtmlUtils.formEntry(msgLabel("ID"),
                                      HtmlUtils.input(ARG_LOCAL_ID,
                                          request.getString(ARG_LOCAL_ID,
                                              "")) + required));
        sb.append(HtmlUtils.formEntry(msgLabel("Repository Name"),
                                      HtmlUtils.input(ARG_LOCAL_NAME,
                                          request.getString(ARG_LOCAL_NAME,
                                              ""))));
        sb.append(
            HtmlUtils.formEntry(
                msgLabel("Contact"),
                HtmlUtils.input(
                    ARG_LOCAL_CONTACT,
                    request.getString(ARG_LOCAL_CONTACT, ""))));

        sb.append(
            HtmlUtils.formEntry(
                msgLabel("Admin ID"),
                HtmlUtils.input(ARG_LOCAL_ADMIN)
                + " Default is &lt;repository id&gt;_admin"));
        sb.append(
            HtmlUtils.formEntry(
                msgLabel("Admin Password"),
                HtmlUtils.input(UserManager.ARG_USER_PASSWORD1) + required));

        sb.append(HtmlUtils.formEntry("",
                                      HtmlUtils.checkbox(ARG_LOCAL_SURE,
                                          "true", false) + " "
                                              + msg("I'm sure")));

        sb.append(
            HtmlUtils.formEntry(
                "",
                HtmlUtils.submit("Create new repository", ARG_LOCAL_NEW)));

        sb.append(HtmlUtils.formTableClose());
        sb.append(HtmlUtils.formClose());
        sb.append(HtmlUtils.sectionClose());

        return getAdmin().makeResult(request, "RAMADDA-Admin-Repositories", sb);

    }

    private void processLocalChange(Request request, StringBuffer sb)
            throws Exception {

        String status = null;
        String id     = request.getString(ARG_LOCAL_ID, "");
        if (request.exists(ARG_LOCAL_START)) {
            Repository child = startLocalRepository(id, new Properties());
            status = STATUS_ACTIVE;
        } else if (request.exists(ARG_LOCAL_STOP)) {
            Repository child = children.get(id);
            if (child == null) {
                sb.append(
                    getPageHandler().showDialogError(
                        "Could not find running server with id: " + id));

                return;
            }
            child.close();
            getRepository().removeChildRepository(child);
            children.remove(id);
            childrenIds.remove(id);
            status = STATUS_STOPPED;
        } else if (request.exists(ARG_LOCAL_DELETE)) {
            if ( !request.get(ARG_LOCAL_SURE, false)) {
                sb.append(
                    getPageHandler().showDialogError(
                        "Check on the 'Yes, remove this repository' box"));

                return;
            }
            //TODO: make sure status is stopped
            getDatabaseManager().delete(
                Tables.LOCALREPOSITORIES.NAME,
                Clause.eq(Tables.LOCALREPOSITORIES.COL_ID, id));

            return;
        } else {
            sb.append(getPageHandler().showDialogError("Unknown action"));

            return;
        }
        getDatabaseManager().update(
            Tables.LOCALREPOSITORIES.NAME, Tables.LOCALREPOSITORIES.COL_ID,
            id, new String[] { Tables.LOCALREPOSITORIES.COL_STATUS },
            new String[] { status });
    }

    private void processLocalNew(Request request, StringBuffer sb)
            throws Exception {
        String id = request.getString(ARG_LOCAL_ID, "");
        if ( !Utils.stringDefined(id)) {
            sb.append(getPageHandler().showDialogError("No ID given"));

            return;
        }

        if (hasServer(id)) {
            sb.append(
                getPageHandler().showDialogError(
                    "Server with id already exists"));

            return;
        }
        try {
            addChildRepository(request, sb, id);
        } catch (Exception exc) {
            sb.append(getPageHandler().showDialogError("Error:" + exc));
            logError("adding repository:" + id, exc);
        }

    }

    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Aug 23, '13
     * @author         Enter your name here...
     */
    private static class Local {

        String id;

        String contact;

        String status;

        public Local(String id, String contact, String status) {
            this.id      = id;
            this.contact = contact;
            this.status  = status;
        }
    }

}
