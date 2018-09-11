/*
* Copyright (c) 2008-2018 Geode Systems LLC
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

package org.ramadda.repository.auth;


import org.ramadda.repository.*;

import org.ramadda.repository.database.*;



import org.ramadda.sql.Clause;


import org.ramadda.sql.SqlUtil;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JQuery;
import org.ramadda.util.TTLCache;


import org.w3c.dom.*;

import ucar.unidata.util.DateUtil;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.StringUtil;

import ucar.unidata.xml.XmlUtil;



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
import java.util.Hashtable;
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
public class AccessManager extends RepositoryManager {


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
        new TTLCache<String, Object[]>(5 * 60 * 1000);


    /** _more_ */
    public static final String PROP_STOPATFIRSTROLE =
        "ramadda.auth.stopatfirstrole";


    /**
     * _more_
     *
     * @param repository _more_
     *
     */
    public AccessManager(Repository repository) {
        super(repository);
    }


    /**
     * _more_
     */
    public void clearCache() {
        recentPermissions = new TTLCache<String, Object[]>(5 * 60 * 1000);
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
                UserManager.ROLE_ANY));
        mainEntry.addPermission(
            new Permission(
                Permission.ACTION_VIEWCHILDREN, UserManager.ROLE_ANY));
        mainEntry.addPermission(new Permission(Permission.ACTION_FILE,
                UserManager.ROLE_ANY));
        mainEntry.addPermission(new Permission(Permission.ACTION_EXPORT,
                UserManager.ROLE_NONE));
        mainEntry.addPermission(new Permission(Permission.ACTION_EDIT,
                UserManager.ROLE_NONE));

        mainEntry.addPermission(new Permission(Permission.ACTION_NEW,
                UserManager.ROLE_NONE));
        mainEntry.addPermission(new Permission(Permission.ACTION_DELETE,
                UserManager.ROLE_NONE));
        mainEntry.addPermission(new Permission(Permission.ACTION_COMMENT,
                UserManager.ROLE_ANY));
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
                    StringUtil.split(request.getString(ARG_ENTRYIDS, ""),
                                     ",", true, true)) {
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
                    || action.equals(Permission.ACTION_VIEWCHILDREN)
                    || action.equals(Permission.ACTION_FILE))) {
                return false;
            }
        }


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


        String requestIp = null;
        User   user      = null;
        if (request == null) {
            user = getUserManager().getAnonymousUser();
        } else {
            user      = request.getUser();
            requestIp = request.getIp();
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

        String key = "a:" + action + "_u:" + user.getId() + "_ip:"
                     + requestIp + "_e:" + entry.getId();
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

        boolean result = canDoActionInner(request, entry, action, user,
                                          requestIp);
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
     * @param entry _more_
     * @param action _more_
     * @param user _more_
     * @param requestIp _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private boolean canDoActionInner(Request request, Entry entry,
                                     String action, User user,
                                     String requestIp)
            throws Exception {
        boolean stop = getRepository().getProperty(PROP_STOPATFIRSTROLE,
                           true);
        //System.err.println("canDoAction:  user=" + user +" action=" + action +" entry=" + entry);
        while (entry != null) {
            boolean      hadInherit     = false;
            boolean      hadAccessGrant = false;
            List<String> roles = (List<String>) getRoles(entry, action);
            if (roles != null) {
                if (requestIp != null) {
                    boolean hadIp = false;
                    for (String role : roles) {
                        boolean negated = false;
                        if (role.startsWith("!")) {
                            negated = true;
                            role    = role.substring(1);
                        }
                        if ( !role.startsWith("ip:")) {
                            continue;
                        }
                        if ( !negated) {
                            hadIp = true;
                        }
                        String ip = role.substring(3);
                        if (requestIp.startsWith(ip)) {
                            return !negated;
                        }
                    }
                    if (hadIp) {
                        return false;
                    }
                }

                for (String role : roles) {
                    boolean negated = false;
                    if (UserManager.ROLE_INHERIT.equals(role)) {
                        hadInherit = true;

                        continue;
                    }
                    if (role.startsWith("!")) {
                        negated = true;
                        role    = role.substring(1);
                    }
                    if (role.startsWith("ip:")) {
                        continue;
                    }
                    if ( !negated) {
                        hadAccessGrant = true;
                    }
                    if (UserManager.ROLE_ANY.equals(role)) {
                        return true;
                    }
                    if (UserManager.ROLE_NONE.equals(role)) {
                        return false;
                    }
                    if (user.isRole(role)) {
                        return !negated;
                    }
                }
                //If we had an access grant here (i.e., a non negated role)
                //and the user did not fall under that role then block access
                if ( !hadInherit && stop && hadAccessGrant) {
                    return false;
                }
            }
            //LOOK: make sure we pass in false here which says do not check for access control

            //            System.err.println ("auth:" + entry.getId() +" parent:" + entry.getParentEntryId());
            entry = getEntryManager().getParent(request, entry, false);
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
    public List getRoles(Entry entry, String action) throws Exception {
        //Make sure we call getPermissions first which forces the instantation of the roles
        getPermissions(entry);

        return entry.getRoles(action);
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
        if ( !getRepository().getProperty(PROP_DOWNLOAD_OK, false)) {
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
    public boolean canEditEntry(Request request, Entry entry)
            throws Exception {
        //        if(entry.getIsLocalFile()) return false;
        return canDoAction(request, entry, Permission.ACTION_EDIT);
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
    public boolean canExportEntry(Request request, Entry entry)
            throws Exception {
        return canDoAction(request, entry, Permission.ACTION_EXPORT);
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
            List roles = (List) map.get(permission.getAction());
            if (roles == null) {
                map.put(permission.getAction(), roles = new ArrayList());
            }
            roles.addAll(permission.getRoles());
        }

        StringBuffer cols = new StringBuffer(HtmlUtils.cols(entryUrl));
        for (int i = 0; i < Permission.ACTIONS.length; i++) {
            List roles = (List) map.get(Permission.ACTIONS[i]);
            if (roles == null) {
                cols.append(HtmlUtils.cols("&nbsp;"));
            } else {
                cols.append(HtmlUtils.cols(StringUtil.join("<br>", roles)));
            }
        }
        sb.append(
            HtmlUtils.row(
                cols.toString(),
                HtmlUtils.cssClass("ramadda-access-summary")));
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
            List roles = permission.getRoles();
            for (int i = 0; i < roles.size(); i++) {
                getDatabaseManager().executeInsert(Tables.PERMISSIONS.INSERT,
                        new Object[] { entry.getId(),
                                       permission.getAction(),
                                       roles.get(i) });
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
        Hashtable actions = new Hashtable();
        while ((results = iter.getNext()) != null) {
            String id     = results.getString(1);
            String action = results.getString(2);
            String role   = results.getString(3);
            List   roles  = (List) actions.get(action);
            if (roles == null) {
                actions.put(action, roles = new ArrayList());
                permissions.add(new Permission(action, roles));
            }
            roles.add(role);
        }
        entry.setPermissions(permissions);

        return permissions;
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
                HtmlUtils.cssClass("ramadda-access-summary-header")));

        listAccess(request, entry, currentAccess);
        currentAccess.append(HtmlUtils.close(HtmlUtils.TAG_TABLE));



        Hashtable        map         = new Hashtable();
        List<Permission> permissions = getPermissions(entry);
        for (Permission permission : permissions) {
            List roles = permission.getRoles();
            if (roles.contains(UserManager.ROLE_NONE)) {
                map.put(permission.getAction() + ".hasnone", "true");
            }
            map.put(permission.getAction(), StringUtil.join("\n", roles));
        }
        request.formPostWithAuthToken(sb, URL_ACCESS_CHANGE, "");

        sb.append(HtmlUtils.hidden(ARG_ENTRYID, entry.getId()));
        sb.append("<p>");
        sb.append(HtmlUtils.submit("Change Access"));
        sb.append("<p>");
        //        sb.append("<table><tr valign=\"top\"><td>");
        //        sb.append(HtmlUtils.formTable());
        sb.append("<table>");
        sb.append("<tr valign=top>");
        sb.append(HtmlUtils.cols(HtmlUtils.bold(msg("Action")),
                                 HtmlUtils.bold(msg("Role")) + " ("
                                 + msg("one per line") + ")"));
        sb.append(HtmlUtils.cols(HtmlUtils.space(5)));
        sb.append("<td rowspan=6><b>" + msg("All Roles")
                  + "</b><i><br>user:&lt;userid&gt;<br>");
        sb.append(StringUtil.join("<br>", getUserManager().getRoles()));
        sb.append("</i></td>");

        sb.append(HtmlUtils.cols(HtmlUtils.space(5)));

        sb.append("<td rowspan=6><b>" + msgLabel("Current settings")
                  + "</b><i><br>");
        sb.append(currentAccess.toString());
        sb.append("</i></td>");

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
                        getRepository().iconUrl(ICON_HELP)), HtmlUtils.attr(
                        HtmlUtils.ATTR_TARGET, "_help")) + HtmlUtils.space(1)
                            + msg(actionName);

            String message = "";
            //If there isn't a none defined here and there are roles then
            //tell te user what's up
            if ( !hasNoneRole && (roles.length() > 0)) {
                //                message=msg("No block defined");
            }
            sb.append(HtmlUtils.rowTop(HtmlUtils.cols(label,
                    HtmlUtils.textArea(ARG_ROLES + "."
                                       + Permission.ACTIONS[i], roles, 5,
                                           20), message)));
        }
        sb.append(HtmlUtils.formTableClose());
        //        sb.append("</td><td>&nbsp;&nbsp;&nbsp;</td><td>");
        //        sb.append("All Roles:<br>");
        //        sb.append(StringUtil.join("<br>",getUserManager().getRoles()));
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
            List roles = StringUtil.split(request.getString(ARG_ROLES + "."
                             + Permission.ACTIONS[i], ""), "\n", true, true);
            if (roles.size() > 0) {
                permissions.add(new Permission(Permission.ACTIONS[i], roles));
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

    private TwoFactorAuthenticator twoFactorAuthenticator;

    public void setTwoFactorAuthenticator(TwoFactorAuthenticator tfa) {
        twoFactorAuthenticator  = tfa;
    }

    public TwoFactorAuthenticator getTwoFactorAuthenticator() {
        return twoFactorAuthenticator;
    }

    public static class TwoFactorAuthenticator {
        public void addAuthForm(Request request, User user, Appendable sb) throws Exception  {
        }

        public boolean userHasBeenAuthenticated(Request request, User user, Appendable sb) throws Exception  {
            return true;
        }
        public boolean userCanBeAuthenticated(User user) {
            return false;
        }
    }



}
