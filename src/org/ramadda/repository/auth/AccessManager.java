/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.auth;


import org.json.*;


import org.ramadda.repository.*;

import org.ramadda.repository.database.*;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.TTLCache;
import org.ramadda.util.Utils;

import org.ramadda.util.sql.Clause;
import org.ramadda.util.sql.SqlUtil;

import org.w3c.dom.*;

import ucar.unidata.util.Misc;

import ucar.unidata.util.StringUtil;

import ucar.unidata.util.TwoFacedObject;

import java.io.*;

import java.io.File;
import java.io.InputStream;

import java.lang.reflect.*;

import java.net.*;

import java.sql.ResultSet;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;

import java.util.regex.*;
import java.util.zip.*;




/**
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
@SuppressWarnings("unchecked")
public class AccessManager extends RepositoryManager {


    /**  */
    public static final String ARG_DATAPOLICY = "datapolicy";

    /** _more_ */
    public RequestUrl URL_ACCESS_FORM = new RequestUrl(getRepository(),
                                            "/access/form", "Access");


    /** _more_ */
    public RequestUrl URL_ACCESS_CHANGE = new RequestUrl(getRepository(),
                                              "/access/change");


    /** _more_ */
    private Object MUTEX_PERMISSIONS = new Object();


    /** _more_ */
    private TTLCache<String, Object[]> recentPermissions =
        new TTLCache<String, Object[]>(5 * 60 * 1000,
                     "Access Manager Permissions");


    /** _more_ */
    public static final String PROP_STOPATFIRSTROLE =
        "ramadda.auth.stopatfirstrole";


    /**  */
    private boolean stopAtFirstRole = true;


    /**
     * _more_
     *
     * @param repository _more_
     *
     */
    public AccessManager(Repository repository) {
        super(repository);
        Misc.run(new Runnable() {
            public void run() {
                try {
                    runDataPolicyFetch();
                } catch (Exception exc) {
                    getLogManager().logError(
                        "Error running runDataPolicyFetch");
                }
            }
        });

    }



    /**
     *
     * @throws Exception _more_
     */
    private void runDataPolicyFetch() throws Exception {
        //Pause for a minute
        //      Misc.sleepSeconds(60);
        Misc.sleepSeconds(5);
        while (true) {
            doDataPolicyFetch();
            //Sleep 5 minutes
            Misc.sleepSeconds(60 * 5);
        }

    }

    /**  */
    private List<DataPolicy> dataPolicies = new ArrayList<DataPolicy>();

    /**  */
    private Hashtable<String, DataPolicy> dataPoliciesMap =
        new Hashtable<String, DataPolicy>();

    /**
     *
     * @throws Exception _more_
     */
    private void doDataPolicyFetch() throws Exception {
        boolean debug = false;
        List<String> urls =
            Utils.split(getRepository().getProperty("ramadda.datapolicy.urls",
                "https://ramadda.org/repository/v1/datapolicy/list"), ",",
                    true, true);
        List<DataPolicy> dataPolicies = new ArrayList<DataPolicy>();
        Hashtable<String, DataPolicy> dataPoliciesMap = new Hashtable<String,
                                                            DataPolicy>();
        for (String url : urls) {
            if (debug) {
                System.err.println("Fetching data policy:" + url);
            }
            try {
                String     json = IO.readContents(url);
                JSONObject dp   = new JSONObject(json);
                String     name = dp.optString("name", "");
                if (debug) {
                    System.err.println("Processing data policy:"
                                       + dp.getString("name"));
                }
                JSONArray jpolicies = dp.getJSONArray("policies");
                for (int i = 0; i < jpolicies.length(); i++) {
                    JSONObject policy     = jpolicies.getJSONObject(i);
                    DataPolicy dataPolicy = new DataPolicy(url,policy.optString("url",null),
							   name, policy);
                    dataPolicies.add(dataPolicy);
                    dataPoliciesMap.put(dataPolicy.getId(), dataPolicy);
                }
                this.dataPolicies    = dataPolicies;
                this.dataPoliciesMap = dataPoliciesMap;
            } catch (Exception exc) {
                getLogManager().logError("Error reading data policy:" + url,
                                         exc);
            }
        }
    }


    /**
     * _more_
     */
    public synchronized void clearCache() {
        recentPermissions.clearCache();
    }


    /**
     */
    @Override
    public void initAttributes() {
        super.initAttributes();
        stopAtFirstRole = getRepository().getProperty(PROP_STOPATFIRSTROLE,
                true);
    }


    /**
     * _more_
     *
     * @param mainEntry _more_
     *
     * @throws Exception _more_
     */
    public void initTopEntry(Entry mainEntry) throws Exception {
        mainEntry.addPermission(new Permission(Permission.ACTION_VIEW,
                Role.ROLE_ANY));
        mainEntry.addPermission(
            new Permission(Permission.ACTION_VIEWCHILDREN, Role.ROLE_ANY));
        mainEntry.addPermission(new Permission(Permission.ACTION_FILE,
                Role.ROLE_ANY));
        mainEntry.addPermission(new Permission(Permission.ACTION_EXPORT,
                Role.ROLE_NONE));
        mainEntry.addPermission(new Permission(Permission.ACTION_EDIT,
                Role.ROLE_NONE));

        mainEntry.addPermission(new Permission(Permission.ACTION_NEW,
                Role.ROLE_NONE));
        mainEntry.addPermission(new Permission(Permission.ACTION_DELETE,
                Role.ROLE_NONE));
        mainEntry.addPermission(new Permission(Permission.ACTION_COMMENT,
                Role.ROLE_ANY));
        insertPermissions(null, mainEntry, mainEntry.getPermissions());
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param action _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean canDoAction(Request request, String action)
            throws Exception {
        if (getRepository().isReadOnly()) {
            if ( !(action.equals(Permission.ACTION_VIEW)
                    || action.equals(Permission.ACTION_VIEWCHILDREN)
                    || action.equals(Permission.ACTION_FILE))) {
                return false;
            }
        }
        User user = request.getUser();
        //The admin can do anything
        if (user.getAdmin()) {
            return true;
        }

        if (request.exists(ARG_ENTRYID)) {
            Entry entry = getEntryManager().getEntry(request,
                              request.getString(ARG_ENTRYID, ""), false);
            if (entry == null) {
                throw new RepositoryUtil.MissingEntryException(
                    "Could not find entry:"
                    + request.getString(ARG_ENTRYID, ""));
            }

            return canDoAction(request, entry, action);
        }

        if (request.exists(ARG_ENTRYIDS)) {
            for (String id :
                    Utils.split(request.getString(ARG_ENTRYIDS, ""), ",",
                                true, true)) {
                Entry entry = getEntryManager().getEntry(request, id, false);
                if (entry == null) {
                    throw new RepositoryUtil.MissingEntryException(
                        "Could not find entry:" + id);
                }
                if ( !canDoAction(request, entry, action)) {
                    return false;
                }
            }

            return true;
        }

        if (request.exists(ARG_GROUP)) {
            Entry group = getEntryManager().findGroup(request);
            if (group == null) {
                throw new RepositoryUtil.MissingEntryException(
                    "Could not find folder:"
                    + request.getString(ARG_GROUP, ""));
            }
            boolean canDo = canDoAction(request, group, action);

            //            System.err.println ("action:" + action +" found folder:" + group + " canDo:" + canDo);
            return canDo;
        }

        if (request.exists(ARG_ASSOCIATION)) {
            Clause clause = Clause.eq(Tables.ASSOCIATIONS.COL_ID,
                                      request.getString(ARG_ASSOCIATION, ""));
            List<Association> associations =
                getAssociationManager().getAssociations(request, clause);
            if (associations.size() == 1) {
                Entry fromEntry = getEntryManager().getEntry(request,
                                      associations.get(0).getFromId());
                Entry toEntry = getEntryManager().getEntry(request,
                                    associations.get(0).getToId());
                if (canDoAction(request, fromEntry, action)) {
                    return true;
                }
                if (canDoAction(request, toEntry, action)) {
                    return true;
                }

                return false;


            }
        }

        throw new RepositoryUtil.MissingEntryException(
            "Could not find entry or folder");
        //        return false;
    }







    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param action _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean canDoAction(Request request, Entry entry, String action)
            throws Exception {
        return canDoAction(request, entry, action, false);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param action _more_
     * @param log _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean canDoAction(Request request, Entry entry, String action,
                               boolean log)
            throws Exception {

        if (getRepository().isReadOnly()) {
            if ( !(action.equals(Permission.ACTION_VIEW)
                    || action.equals(Permission.ACTION_EXPORT)
                    || action.equals(Permission.ACTION_VIEWCHILDREN)
                    || action.equals(Permission.ACTION_FILE))) {
                return false;
            }
        }


        String requestIp = null;
        User   user      = null;
        if (request == null) {
            user = getUserManager().getAnonymousUser();
        } else {
            user      = request.getUser();
            requestIp = request.getIp();
        }

        return canDoAction(request, requestIp, user, log, entry, action);
    }

    /**
     *
     * @param request _more_
     * @param requestIp _more_
     * @param user _more_
     * @param log _more_
     * @param entry _more_
     * @param action _more_
      * @return _more_
     *
     * @throws Exception _more_
     */
    private boolean canDoAction(Request request, String requestIp, User user,
                                boolean log, Entry entry, String action)
            throws Exception {

        if (entry == null) {
            return false;
        }

        if (entry.getIsLocalFile()) {
            if (action.equals(Permission.ACTION_NEW)) {
                return false;
            }
            if (action.equals(Permission.ACTION_DELETE)) {
                if (getStorageManager().isProcessFile(entry.getFile())) {
                    return true;
                }

                return false;
            }
        }

        if (log) {
            logInfo("Upload:canDoAction:" + action);
        }

        if ( !action.equals(Permission.ACTION_VIEW)) {
            boolean okToView = canDoAction(request, entry,
                                           Permission.ACTION_VIEW, log);
            if (log) {
                logInfo("Upload:action isn't view. view permission="
                        + okToView);
            }
            if ( !okToView) {
                return false;
            }
        }



        if (user == null) {
            logInfo("Upload:canDoAction: user is null");

            return false;
        }


        //The admin can do anything
        if (user.getAdmin()) {
            if (log) {
                logInfo("Upload:user is admin");
            }

            //            System.err.println("user is admin");
            return true;
        }



        //If user is owner then they can do anything
        if ( !user.getAnonymous() && Misc.equals(user, entry.getUser())) {
            if (log) {
                logInfo("Upload:user is owner");
            }

            //            System.err.println("user is owner of entry");
            return true;
        }

        String key = "a:" + action + "_u:" + user.getId() + "_roles:"
                     + user.getRoles() + "_ip:" + requestIp + "_e:"
                     + entry.getId();
        Object[] pastResult = recentPermissions.get(key);
        Date     now        = new Date();
        if (pastResult != null) {
            Date    then = (Date) pastResult[0];
            Boolean ok   = (Boolean) pastResult[1];
            //If we have checked this in the last 60 seconds then return the result
            //TODO - Do we really need the time threshold
            if (true || (now.getTime() - then.getTime() < 60000)) {
                if (log) {
                    logInfo("Upload:getting result from cache");
                }

                //            logInfo("Upload:canDoAction: cache");
                return ok.booleanValue();
            } else {
                recentPermissions.remove(key);
            }
        }

        boolean debug = false;
        //      debug = action.equals("view") && entry.getName().indexOf("test")>=0; 
        boolean result = canDoActionInner(request, requestIp, user, log,
                                          entry, action);
        if (debug) {
            //      System.err.println("CANDO:" + entry +" " + result);
            //      System.err.println(Utils.getStack(10));
        }
        //        logInfo("Upload:canDoAction:  result= " + result);
        if (recentPermissions.size() > 10000) {
            clearCache();
        }
        recentPermissions.put(key, new Object[] { now, new Boolean(result) });

        return result;


    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     */
    public boolean canSetAccess(Request request, Entry entry) {
        User user = request.getUser();
        if (user.getAdmin()
                || ( !user.getAnonymous()
                     && Misc.equals(user, entry.getUser()))) {
            return true;
        }

        return false;

    }



    /**
     * _more_
     *
     * @param request _more_
     * @param log _more_
     * @param entry _more_
     * @param action _more_
     * @param user _more_
     * @param requestIp _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private boolean canDoActionInner(Request request, String requestIp,
                                     User user, boolean log, Entry entry,
                                     String action)
            throws Exception {
        if (entry == null) {
            return false;
        }
        //        boolean stop = stopAtFirstRole;
        boolean debug = false;
        //      debug = action.equals("view") && entry.getName().indexOf("test")>=0; 

        boolean    hadInherit = false;
        boolean    hadAny     = false;
        List<Role> roles      = getRoles(entry, action);
        if (debug && (roles != null) && (roles.size() > 0)) {
            System.err.println("canDoAction:  user=" + user + " action="
                               + action + " entry=" + entry + " roles="
                               + roles);
        }
        if ((roles != null) && (roles.size() > 0)) {
            /*
              ip:222
              user
              none
            */
            for (Role role : roles) {
                if (role.isComment()) {
                    continue;
                }
                hadAny = true;
                boolean negated = role.getNegated();
                if (debug) {
                    System.err.println("\tROLE:" + role.getBaseRole()
                                       + " negated:" + negated);
                }
                if (role.getIsIp()) {
                    if (requestIp != null) {
                        if (requestIp.startsWith(role.getBaseRole())) {
                            if (debug) {
                                System.err.println("\tIP negated:" + negated
                                        + " " + requestIp);
                            }

                            return !negated;
                        }
                    }
                    continue;
                }

                if (Role.ROLE_INHERIT.isRole(role)) {
                    hadInherit = true;
                    continue;
                }
                if (Role.ROLE_ANY.isRole(role)) {
                    if (debug) {
                        System.err.println("\tIs ANY negated: " + negated);
                    }

                    return !negated;
                }
                if (Role.ROLE_NONE.isRole(role)) {
                    if (debug) {
                        System.err.println("\tIs NONE negated:" + negated);
                    }

                    return negated;
                }
                if (user.isRole(role)) {
                    if (debug) {
                        System.err.println("\tuser.isRole: " + role);
                    }

                    return !negated;
                }
            }
            //If we had an access grant here then block access
            /*
            if(hadAny) {
                if (!hadInherit) {
                    if(stop) {
                        if(debug)
                            System.err.println("\thadAny=true stop=true returning FALSE");
                        return false;
                    }
                }
                }*/
        }

        //LOOK: make sure we pass in false here which says do not check for access control
        entry = getEntryManager().getParent(request, entry, false);
        if (entry != null) {
            return canDoAction(request, requestIp, user, log, entry, action);
        }
        if (debug) {
            System.err.println("\tparent is NULL");
        }

        return false;
    }






    /**
     * _more_
     *
     * @param entry _more_
     * @param action _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Role> getRoles(Entry entry, String action) throws Exception {
        //Make sure we call getPermissions first which forces the instantation of the roles
        getPermissions(entry);

        return (List<Role>) entry.getRoles(action);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean canAccessFile(Request request, Entry entry)
            throws Exception {
        //Check if its a crawler
        if ((request != null) && request.getIsRobot()) {
            return false;
        }

        return canDoAction(request, entry, Permission.ACTION_FILE, false);
    }

    /**
     * Can we view this Entry's file?
     *
     * @param request the request
     * @param entry the entry
     *
     * @return true if we can
     *
     * @throws Exception  problem figuring it out
     */
    public boolean canViewFile(Request request, Entry entry)
            throws Exception {
        //Check if its a crawler
        if ((request != null) && request.getIsRobot()) {
            return false;
        }

        return canDoAction(request, entry, Permission.ACTION_VIEW, false);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean canDownload(Request request, Entry entry)
            throws Exception {
        if ( !getRepository().getDownloadOk()) {
            return false;
        }
        entry = filterEntry(request, entry);
        if (entry == null) {
            return false;
        }


        //        System.err.println ("type: " + entry.getTypeHandler().getClass().getName());
        if ( !entry.getTypeHandler().canDownload(request, entry)) {
            return false;
        }

        if ( !canDoAction(request, entry, Permission.ACTION_FILE)) {
            return false;
        }

        return getStorageManager().canDownload(request, entry);
    }


    /** _more_ */
    public static boolean debug = false;

    /**
     * _more_
     *
     * @param msg _more_
     */
    public void debug(String msg) {
        if (debug) {
            logInfo(msg);
        }
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry filterEntry(Request request, Entry entry) throws Exception {
        if (entry.getIsRemoteEntry()) {
            return entry;
        }
        if (entry.getResource() != null) {
            if (entry.getResource().isFileType()) {
                if ( !entry.getResource().getTheFile().exists()) {
                    //                    System.err.println ("filterEntry: file is missing");
                    entry = getEntryManager().handleMissingFileEntry(request,
                            entry);
                    if (entry == null) {
                        //                        System.err.println ("filterEntry: missing entry got deleted");
                        return null;
                    }
                }
            }
        }
        //        System.err.println ("filter:" + entry.getFullName());
        long t1 = System.currentTimeMillis();
        if ( !canDoAction(request, entry, Permission.ACTION_VIEW)) {
            return null;
        }
        long t2 = System.currentTimeMillis();
        //        System.err.println ("time to filter:" + (t2-t1));


        Entry parent = entry.getParentEntry();
        if ((parent != null)
                && !canDoAction(request, parent,
                                Permission.ACTION_VIEWCHILDREN)) {
            return null;
        }

        if (getEntryManager().isAnonymousUpload(entry)) {
            if ( !canDoAction(request, parent, Permission.ACTION_NEW)) {
                return null;
            }
        }


        return entry;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Entry> filterEntries(Request request, List entries)
            throws Exception {
        List<Entry> filtered = new ArrayList();
        for (int i = 0; i < entries.size(); i++) {
            Entry entry = (Entry) entries.get(i);
            entry = filterEntry(request, entry);
            if (entry != null) {
                filtered.add(entry);
            }
        }

        return filtered;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean canDoEdit(Request request, Entry entry) throws Exception {
        //        if(entry.getIsLocalFile()) return false;
        return canDoAction(request, entry, Permission.ACTION_EDIT);
    }

    /**
     *
     * @param request _more_
     * @param entry _more_
      * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean canDoFile(Request request, Entry entry) throws Exception {
        return canDoAction(request, entry, Permission.ACTION_FILE);
    }

    /**
     *
     * @param request _more_
     * @param entry _more_
      * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean canDoType1(Request request, Entry entry) throws Exception {
        return canDoAction(request, entry, Permission.ACTION_TYPE1);
    }

    /**
     *
     * @param request _more_
     * @param entry _more_
      * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean canDoType2(Request request, Entry entry) throws Exception {
        return canDoAction(request, entry, Permission.ACTION_TYPE2);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean canDoExport(Request request, Entry entry)
            throws Exception {
        return canDoAction(request, entry, Permission.ACTION_EXPORT);
    }


    /**
     *
     * @param request _more_
     * @param entry _more_
      * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean canDoComment(Request request, Entry entry)
            throws Exception {
        return canDoAction(request, entry, Permission.ACTION_COMMENT);
    }

    /**
     *
     * @param request _more_
     * @param entry _more_
      * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean canDoView(Request request, Entry entry) throws Exception {
        return canDoAction(request, entry, Permission.ACTION_VIEW);
    }

    /**
     *
     * @param request _more_
     * @param entry _more_
      * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean canDoNew(Request request, Entry entry) throws Exception {
        return canDoAction(request, entry, Permission.ACTION_NEW);
    }

    /**
     *
     * @param request _more_
     * @param entry _more_
      * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean canDoUpload(Request request, Entry entry)
            throws Exception {
        return canDoAction(request, entry, Permission.ACTION_UPLOAD);
    }

    /**
     *
     * @param request _more_
     * @param entry _more_
      * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean canDoDelete(Request request, Entry entry)
            throws Exception {
        return canDoAction(request, entry, Permission.ACTION_DELETE);
    }

    /**
     *
     * @param request _more_
     * @param entry _more_
      * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean canDoViewChildren(Request request, Entry entry)
            throws Exception {
        return canDoAction(request, entry, Permission.ACTION_VIEWCHILDREN);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean canDoType1Action(Request request, Entry entry)
            throws Exception {
        return canDoAction(request, entry, Permission.ACTION_TYPE1);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean canDoType2Action(Request request, Entry entry)
            throws Exception {
        return canDoAction(request, entry, Permission.ACTION_TYPE2);
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void listAccess(Request request, Entry entry, StringBuffer sb)
            throws Exception {
        if (entry == null) {
            return;
        }
        List<Permission> permissions = getPermissions(entry);
        String entryUrl = HtmlUtils.href(request.makeUrl(URL_ACCESS_FORM,
                              ARG_ENTRYID, entry.getId()), entry.getName());

        Hashtable map = new Hashtable();
        for (Permission permission : permissions) {
            List<Role> roles = (List<Role>) map.get(permission.getAction());
            if (roles == null) {
                map.put(permission.getAction(),
                        roles = new ArrayList<Role>());
            }
            roles.addAll(permission.getRoles());
        }

        StringBuffer cols = new StringBuffer(HtmlUtils.cols(entryUrl));
        for (int i = 0; i < Permission.ACTIONS.length; i++) {
            List<Role> roles = (List<Role>) map.get(Permission.ACTIONS[i]);
            if (roles == null) {
                cols.append(HtmlUtils.cols("&nbsp;"));
            } else {
                StringBuilder tmp = null;
                for (Role role : roles) {
                    if (tmp == null) {
                        tmp = new StringBuilder();
                    } else {
                        tmp.append("<br>");
                    }
                    tmp.append(HU.span(role.toString(),
                                       role.getDecoration()));
                }
                cols.append(HtmlUtils.cols(tmp.toString()));
            }
        }
        sb.append(
            HtmlUtils.row(
                cols.toString(),
                HU.attr("valign", "top")
                + HtmlUtils.cssClass("ramadda-access-summary")));
        listAccess(request,
                   getEntryManager().getEntry(request,
                       entry.getParentEntryId()), sb);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param permissions _more_
     *
     * @throws Exception _more_
     */
    public void insertPermissions(Request request, Entry entry,
                                  List<Permission> permissions)
            throws Exception {
        clearCache();
        getDatabaseManager().delete(
            Tables.PERMISSIONS.NAME,
            Clause.eq(Tables.PERMISSIONS.COL_ENTRY_ID, entry.getId()));

        for (Permission permission : permissions) {
            List<Role> roles = permission.getRoles();
            for (Role role : roles) {
                getDatabaseManager().executeInsert(Tables.PERMISSIONS.INSERT,
                        new Object[] { entry.getId(),
                                       permission.getAction(),
                                       role.getRole() });
            }
        }
        entry.setPermissions(permissions);
    }


    /**
     * _more_
     *
     * @param entry _more_
     * @param permission _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean hasPermissionSet(Entry entry, String permission)
            throws Exception {
        for (Permission p : getPermissions(entry)) {
            if (Misc.equals(p.getAction(), permission)) {
                return true;
            }
        }

        return false;
    }


    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Permission> getPermissions(Entry entry) throws Exception {
        if (entry.isGroup() && entry.isDummy()) {
            return new ArrayList<Permission>();
        }
        List<Permission> permissions = entry.getPermissions();
        if (permissions != null) {
            return permissions;
        }
        //            if(!entry.isGroup()) 
        //                System.err.println ("getPermissions for entry:" + entry.getId());
        SqlUtil.Iterator iter = getDatabaseManager().getIterator(
                                    getDatabaseManager().select(
                                        Tables.PERMISSIONS.COLUMNS,
                                        Tables.PERMISSIONS.NAME,
                                        Clause.eq(
                                            Tables.PERMISSIONS.COL_ENTRY_ID,
                                            entry.getId())));

        permissions = new ArrayList<Permission>();

        ResultSet results;
        Hashtable<String, List<Role>> actions = new Hashtable<String,
                                                    List<Role>>();
        while ((results = iter.getNext()) != null) {
            int        col    = 1;
            String     id     = results.getString(col++);
            String     action = results.getString(col++);
            String     role   = results.getString(col++);
            List<Role> roles  = actions.get(action);
            if (roles == null) {
                actions.put(action, roles = new ArrayList<Role>());
                permissions.add(new Permission(action, roles));
            }
            roles.add(new Role(role));
        }
        entry.setPermissions(permissions);

        return permissions;
    }




    /**
     *
     * @param request _more_
      * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processDataPolicyInfo(Request request) throws Exception {
        StringBuilder sb = new StringBuilder();
        getPageHandler().sectionOpen(request, sb, "Data Policies", false);
        if (dataPolicies.size() == 0) {
            sb.append("No loaded data policies");
            getPageHandler().sectionClose(request, sb);

            return new Result("Data Policies", sb);
        }
        sb.append("Loaded data policies:");
        LinkedHashMap<String, StringBuilder> map = new LinkedHashMap<String,
                                                       StringBuilder>();
        for (DataPolicy dataPolicy : dataPolicies) {
            String href = dataPolicy.getFromName() + " - "
                          + HU.href(dataPolicy.getMainUrl(), dataPolicy.getMainUrl());
            StringBuilder buff = map.get(href);
            if (buff == null) {
                map.put(href, buff = new StringBuilder());
            }
            String label = dataPolicy.getLabel();
	    if(Utils.stringDefined(dataPolicy.getMyUrl())) {
		label = HU.href(dataPolicy.getMyUrl(), label);
	    }
            buff.append("<li>");
            buff.append(HU.italics(label));
            if (Utils.stringDefined(dataPolicy.getDescription())) {
                buff.append("<br>");
                buff.append(dataPolicy.getDescription());
            }
            //If they are logged then show the access
            if ( !request.isAnonymous()) {
                buff.append("<br>");
                buff.append(HU.b("Permissions:"));
                buff.append("<ul>");
                for (Permission permission : dataPolicy.getPermissions()) {
                    buff.append("<li>");
                    buff.append(permission);
                }
                buff.append("</ul>");
            }

        }

        sb.append("<ul>");
        for (String key : map.keySet()) {
            StringBuilder buff = map.get(key);
            sb.append("<li> ");
            sb.append(key);
            sb.append("<ul>");
            sb.append(buff);
            sb.append("</ul>");
        }
        sb.append("</ul>");

        sb.append("</ul>");
        getPageHandler().sectionClose(request, sb);

        return new Result("Data Policies", sb);
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
    public Result processAccessForm(Request request) throws Exception {


        StringBuffer sb    = new StringBuffer();
        Entry        entry = getEntryManager().getEntry(request);

        if ( !canSetAccess(request, entry)) {
            throw new AccessException("Can't set access", request);
        }

        getPageHandler().entrySectionOpen(request, entry, sb,
                                          "Define Access Rights");

        request.appendMessage(sb);

        StringBuffer currentAccess = new StringBuffer();
        currentAccess.append(HtmlUtils.open(HtmlUtils.TAG_TABLE));
        StringBuffer header =
            new StringBuffer(HtmlUtils.cols(HtmlUtils.bold(msg("Entry"))));
        for (int i = 0; i < Permission.ACTIONS.length; i++) {
            header.append(HtmlUtils.cols(msg(Permission.ACTION_NAMES[i])));
        }
        currentAccess.append(
            HtmlUtils.row(
                header.toString(),
                HU.attr("valign", "top")
                + HtmlUtils.cssClass("ramadda-access-summary-header")));

        listAccess(request, entry, currentAccess);
        currentAccess.append(HtmlUtils.close(HtmlUtils.TAG_TABLE));



        Hashtable        map         = new Hashtable();
        List<Permission> permissions = getPermissions(entry);
        HashSet<String>  dpMap       = new HashSet<String>();
        for (Permission permission : permissions) {
            if (permission.hasDataPolicy()) {
                dpMap.add(permission.getDataPolicy().getId());
            }
        }

        for (Permission permission : permissions) {
            List roles = permission.getRoles();
            if (roles.contains(Role.ROLE_NONE)) {
                map.put(permission.getAction() + ".hasnone", "true");
            }
            map.put(permission.getAction(), StringUtil.join("\n", roles));
        }
        request.formPostWithAuthToken(sb, URL_ACCESS_CHANGE, "");

        sb.append(HtmlUtils.hidden(ARG_ENTRYID, entry.getId()));
        sb.append("<p>");
        sb.append(HtmlUtils.submit("Change Access"));
        sb.append("<p>");
        if (dataPolicies.size() > 0) {
            List         items    = new ArrayList();
            List<String> selected = new ArrayList<String>();
            System.err.println(dpMap);
            for (DataPolicy dataPolicy : dataPolicies) {
                if (dpMap.contains(dataPolicy.getId())) {
                    selected.add(dataPolicy.getId());
                }
                items.add(new TwoFacedObject(dataPolicy.getLabel(),
                                             dataPolicy.getId()));
            }
            String extraSelect = HtmlUtils.attr(HtmlUtils.ATTR_MULTIPLE,
                                     "true") + HtmlUtils.attr("size",
                                         "" + (Math.min(items.size(), 4)));
            String select = HU.select(ARG_DATAPOLICY, items, selected,
                                      extraSelect);
            sb.append(HU.b("Data Policy:") + " " + select);
            sb.append(HU.href(getRepository().getUrlBase()
                              + "/access/datapolicies", "View Data Policies",
                                  HU.attr("target", "_datapolicies")));
        }

        sb.append("<table id='accessform' style=''><tr valign=top>");
        sb.append("<td>");
        sb.append("<table style='margin-right:10px;' >");
        List opts = new ArrayList();
        Utils.add(
            opts, new TwoFacedObject("Add role", ""),
            new TwoFacedObject("User ID", "user:&lt;user id&gt;"),
            new TwoFacedObject("Logged in user", Role.ROLE_USER.getRole()),
            new TwoFacedObject("No one", Role.ROLE_NONE.getRole()),
            new TwoFacedObject("IP Address", "ip:&lt;ip address&gt;"),
            new TwoFacedObject(
                "Anonymous users",
                Role.ROLE_ANONYMOUS.getRole()), Role.ROLE_ANY.getRole(),
                    new TwoFacedObject(
                        "Guest user", Role.ROLE_GUEST.getRole()));
        opts.addAll(getUserManager().getUserRoles());

        sb.append("<tr valign=top>");
        sb.append(HtmlUtils.cols(HtmlUtils.bold(msg("Action"))));
        sb.append("<td colspan=2>");
        sb.append(HtmlUtils.bold("Role")
                  + ". One per line. Prefix with \"!\" for negation");
        sb.append("</td>");
        sb.append("</tr>");
        for (int i = 0; i < Permission.ACTIONS.length; i++) {
            String roles = (String) map.get(Permission.ACTIONS[i]);
            boolean hasNoneRole = map.get(Permission.ACTIONS[i] + ".hasnone")
                                  != null;
            if (roles == null) {
                roles = "";
            }
            String actionName = Permission.ACTION_NAMES[i];
            String action     = Permission.ACTIONS[i];
            if (action.equals(Permission.ACTION_TYPE1)) {
                actionName = entry.getTypeHandler().getTypePermissionName(
                    Permission.ACTION_TYPE1);
            } else if (action.equals(Permission.ACTION_TYPE2)) {
                actionName = entry.getTypeHandler().getTypePermissionName(
                    Permission.ACTION_TYPE2);
            }
            String label =
                HtmlUtils.href(
                    getRepository().getUrlBase() + "/userguide/access.html#"
                    + action, HtmlUtils.img(
                        getRepository().getIconUrl(
                            ICON_HELP)), HtmlUtils.attr(
                                HtmlUtils.ATTR_TARGET,
                                "_help")) + HtmlUtils.space(1)
                                          + msg(actionName);
            String extra = "";
            if (i == 0) {
                extra = HU.select("", opts, (String) null, HU.id("roles"));
            }
            extra = HU.div(extra, HU.id("holder_" + i));
            sb.append(HtmlUtils.rowTop(HtmlUtils.cols(label,
                    HtmlUtils.textArea(ARG_ROLES + "."
                                       + Permission.ACTIONS[i], roles, 5, 20,
                                           HU.attr("roleindex", "" + i)
                                           + HU.id("textarea_"
                                               + i)), extra)));
        }
        sb.append("</tr></table></td>");
        sb.append("<td rowspan=6><b>" + msgLabel("Current settings")
                  + "</b><i><br>");
        sb.append(currentAccess.toString());
        sb.append("</i></td>");
        sb.append(HtmlUtils.formTableClose());
        HU.importJS(sb, getRepository().getUrlBase() + "/accessform.js");
        HU.script(sb, "Ramadda.initAccessForm();");



        //        sb.append("</td><td>&nbsp;&nbsp;&nbsp;</td><td>");
        //        sb.append("All Roles:<br>");
        //        sb.append(StringUtil.join("<br>",getUserManager().getStandardRoles()));
        //        sb.append("</td></tr></table>");
        sb.append(HtmlUtils.submit(msg("Change Access")));
        sb.append(HtmlUtils.formClose());

        getPageHandler().entrySectionClose(request, entry, sb);

        return getEntryManager().makeEntryEditResult(request, entry,
                msg("Edit Access"), sb);


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
    public Result processAccessChange(Request request) throws Exception {



        request.ensureAuthToken();
        Entry entry = getEntryManager().getEntry(request);

        if ( !canSetAccess(request, entry)) {
            throw new AccessException("Can't set access", request);
        }
        List<Permission> permissions = new ArrayList<Permission>();
        for (int i = 0; i < Permission.ACTIONS.length; i++) {
            List<String> roles =
                Utils.split(request.getString(ARG_ROLES + "."
                    + Permission.ACTIONS[i], ""), "\n", true, true);
            if (roles.size() > 0) {
                permissions.add(new Permission(Permission.ACTIONS[i],
                        Role.makeRoles(roles)));
            }
        }

        synchronized (MUTEX_PERMISSIONS) {
            insertPermissions(request, entry, permissions);
        }

        return new Result(request.makeUrl(URL_ACCESS_FORM, ARG_ENTRYID,
                                          entry.getId(), ARG_MESSAGE,
                                          getRepository().translate(request,
                                              MSG_ACCESS_CHANGED)));

    }

    /** _more_ */
    private TwoFactorAuthenticator twoFactorAuthenticator;

    /**
     * _more_
     *
     * @param tfa _more_
     */
    public void setTwoFactorAuthenticator(TwoFactorAuthenticator tfa) {
        twoFactorAuthenticator = tfa;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public TwoFactorAuthenticator getTwoFactorAuthenticator() {
        return twoFactorAuthenticator;
    }

    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Dec 6, '18
     * @author         Enter your name here...
     */
    public static class TwoFactorAuthenticator {

        /**
         * _more_
         *
         * @param request _more_
         * @param user _more_
         * @param sb _more_
         *
         * @throws Exception _more_
         */
        public void addAuthForm(Request request, User user, Appendable sb)
                throws Exception {}

        /**
         * _more_
         *
         * @param request _more_
         * @param user _more_
         * @param sb _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        public boolean userHasBeenAuthenticated(Request request, User user,
                Appendable sb)
                throws Exception {
            return true;
        }

        /**
         * _more_
         *
         * @param user _more_
         *
         * @return _more_
         */
        public boolean userCanBeAuthenticated(User user) {
            return false;
        }
    }



}
