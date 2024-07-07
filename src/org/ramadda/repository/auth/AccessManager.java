/**
   Copyright (c) 2008-2023 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.auth;


import org.json.*;

import org.ramadda.repository.*;
import org.ramadda.repository.database.*;
import org.ramadda.repository.metadata.License;

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

    public static final String TAG_PERMISSIONS = "permissions";
    public static final String TAG_PERMISSION = "permission";
    public static final String TAG_ROLE = "role";
    public static final String ATTR_ACTION = "action";
    public static final String ATTR_ROLE = "role";
    public static final String ATTR_DATAPOLICY = "datapolicy";    




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
    public static final String DATAPOLICY_PREFIX = "datapolicy:";

    /**  */
    public static final String PROP_DATAPOLICY_URLS =
        "ramadda.datapolicy.urls";

    /**  */
    public static final String PROP_DATAPOLICY_DEBUG =
        "ramadda.datapolicy.debug";

    /**  */
    public static final String PROP_DATAPOLICY_SLEEP =
        "ramadda.datapolicy.sleepminutes";

    /**  */
    public static final String ARG_DATAPOLICY = "datapolicy";

    /**  */
    private boolean haveDoneDataPolicyFetch = false;

    /**  */
    private boolean stopAtFirstRole = true;

    /**  */
    private List<DataPolicy> dataPolicies = new ArrayList<DataPolicy>();

    /**  */
    private Hashtable<String, DataPolicy> dataPoliciesMap =
        new Hashtable<String, DataPolicy>();

    /**  */
    private boolean debugDataPolicy = false;

    /** _more_ */
    public static boolean debug = false;
    public static boolean debugAction = false;

    public static boolean debugAll = false;


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
     */
    public void updateLocalDataPolicies() {
        Misc.runInABit(5000, new Runnable() {
		public void run() {
		    try {
			doDataPolicyFetch();
		    } catch (Exception exc) {
			getLogManager().logError("calling doDataPolicyFetch",
						 exc);
		    }
		}
	    });
    }





    /**
     *
     * @throws Exception _more_
     */
    private void runDataPolicyFetch() throws Exception {
        //Pause for a a bit before we get started
        //        Misc.sleepSeconds(10);
        Misc.sleepSeconds(2);
        debugDataPolicy = getRepository().getProperty(PROP_DATAPOLICY_DEBUG,
						      false);
        int minutes = getRepository().getProperty(PROP_DATAPOLICY_SLEEP, 5);
        while (true) {
            haveDoneDataPolicyFetch = true;
            try {
                if ( !doDataPolicyFetch()) {
                    return;
                }
            } catch (Exception exc) {
                getLogManager().logError("calling doDataPolicyFetch", exc);
            }
            Misc.sleepSeconds(60 * minutes);
        }
    }


    /**
     * @return _more_
     *
     * @throws Exception _more_
     */
    private synchronized boolean doDataPolicyFetch() throws Exception {
        boolean debug = debugDataPolicy;
        List<String> urls = new ArrayList<String>();
        List<Entry> entries = getEntryManager().getEntriesWithType(
								   getRepository().getAdminRequest(),
								   "type_datapolicy_source");
        for (Entry entry : entries) {
            String url = entry.getResource().getPath();
            if (Utils.stringDefined(url)) {
                urls.add(url);
            }
        }

        List<DataPolicy> tmpDataPolicies = new ArrayList<DataPolicy>();
        Hashtable<String, DataPolicy> tmpDataPoliciesMap =
            new Hashtable<String, DataPolicy>();
        if (debug) {
            System.err.println("AccessManager.doDataPolicyFetch");
        }
        for (String url : urls) {
            if (debug) {
                System.err.println("\tfetching data policy:" + url);
            }
            try {
                url = url.replace(
				  "${this}",
				  getRepository().getTmpRequest().getAbsoluteUrl(""));
            } catch (Exception exc) {
                getLogManager().logError("Error getting self url", exc);
                continue;
            }

            try {
                String     json = IO.readContents(url);
                JSONObject dp   = new JSONObject(json);
                if (dp.has("unique_id") && dp.has("providers_id")) {
                    if (debug) {
                        System.err.println("\tprocessing localcontexts");
                    }
                    List<DataPolicy> lcp = loadLocalContexts(dp, url);
                    tmpDataPolicies.addAll(lcp);
                    for (DataPolicy dataPolicy : lcp) {
                        tmpDataPoliciesMap.put(dataPolicy.getId(),
					       dataPolicy);
                    }
                    continue;
                }
                String    name      = dp.optString("name", "");
                JSONArray jpolicies = dp.getJSONArray("policies");
                if (debug) {
                    System.err.println("\tprocessing data policy:" + name
                                       + " # policies:" + jpolicies.length());
                }
                for (int i = 0; i < jpolicies.length(); i++) {
                    JSONObject policy = jpolicies.getJSONObject(i);
                    DataPolicy dataPolicy = new DataPolicy(this, url,
							   policy.optString("url",
									    null), name, policy);
                    if (debug) {
                        System.err.println("\tpolicy:" + dataPolicy);
                    }
                    tmpDataPolicies.add(dataPolicy);
                    tmpDataPoliciesMap.put(dataPolicy.getId(), dataPolicy);
                }
            } catch (Exception exc) {
                getLogManager().logError("Error reading data policy:" + url,
                                         exc);
            }
        }
        this.dataPolicies    = tmpDataPolicies;
        this.dataPoliciesMap = tmpDataPoliciesMap;

        return true;

    }

    /**
     *
     * @param a _more_
     * @param licenses _more_
     */
    private void loadLocalContextsLabels(JSONArray a,
                                         List<License> licenses) {
        for (int i = 0; i < a.length(); i++) {
            JSONObject label = a.getJSONObject(i);
            String     id    = label.getString("label_type");
            id = id.replaceAll("_", "-");

            licenses.add(
			 new License(
				     "localcontexts-label-" + id, label.getString("name"),
				     "https://localcontexts.org/labels/traditional-knowledge-labels/",
				     label.getString("img_url"),
				     label.getString("default_text")));
        }

    }

    /**
     *
     * @param obj _more_
     * @param fromUrl _more_
     *  @return _more_
     *
     * @throws Exception _more_
     */
    private List<DataPolicy> loadLocalContexts(JSONObject obj, String fromUrl)
	throws Exception {
        List<DataPolicy> dps      = new ArrayList<DataPolicy>();
        List<License>    licenses = new ArrayList<License>();
        for (String type : new String[] { "bc_labels", "tk_labels" }) {
            if (obj.has(type)) {
                loadLocalContextsLabels(obj.getJSONArray(type), licenses);
            }
        }
        DataPolicy dp = new DataPolicy(this, fromUrl, fromUrl,
                                       "Local Contexts Hub",
                                       obj.getString("unique_id"),
                                       obj.getString("title"), licenses);
        dps.add(dp);

        return dps;
    }

    private boolean isActionExportable(String action) {
	return action.equals(Permission.ACTION_VIEW) ||
	    action.equals(Permission.ACTION_VIEWCHILDREN) ||
	    action.equals(Permission.ACTION_FILE)||
	    action.equals(Permission.ACTION_EXPORT);
    }



    public void applyEntryXml(Entry entry,  Element node) throws Exception {
        List<Permission> permissions = new ArrayList<Permission>();
	for(Element permissionNode: (List<Element>) XmlUtil.findChildren(node,TAG_PERMISSION)) {
	    String dataPolicyId = XmlUtil.getAttribute(permissionNode, ATTR_DATAPOLICY,(String)null);
	    List<Role> roles = new ArrayList<Role>();
	    if(stringDefined(dataPolicyId)) {
		DataPolicy dataPolicy = getDataPolicy(dataPolicyId);
		if(dataPolicy==null) {
		    System.err.println("AccessManager.applyEntryXml: unable to find data policy:" + dataPolicyId);
		    continue;
		}
		permissions.addAll(dataPolicy.getPermissions());
		continue;
	    } 
	    String action = XmlUtil.getAttribute(permissionNode, ATTR_ACTION,(String)null);
	    if(!stringDefined(action) || !isActionExportable(action)) {
		System.err.println("AccessManager.applyEntryXml: action not exportable:" + action);
		continue;
	    }
	    for(Element roleNode: (List<Element>) XmlUtil.findChildren(permissionNode,TAG_ROLE)) {

		roles.add(new Role(XmlUtil.getAttribute(roleNode, ATTR_ROLE,(String) null)));
	    }
	    permissions.add(new  Permission(action, roles));
	}
        entry.setPermissions(permissions);
        insertPermissions(null, entry, permissions);
    }


    public void addEntryXml(Entry entry, Document doc, Element node) throws Exception {
	Element permissionsNode = null;

        for(Permission permission: getPermissions(entry)) {
	    String action = permission.getAction();
	    String dataPolicyId = permission.getDataPolicyId();	    
	    if(!stringDefined(dataPolicyId) && !isActionExportable(action)) continue;

	    if(permissionsNode==null) {
		permissionsNode= XmlUtil.create(doc, TAG_PERMISSIONS, node);
	    }


	    Element actionNode = XmlUtil.create(doc, TAG_PERMISSION, permissionsNode);

	    if(stringDefined(dataPolicyId)) {
		actionNode.setAttribute(ATTR_DATAPOLICY, dataPolicyId);
		continue;
	    }

            actionNode.setAttribute(ATTR_ACTION, action);
	    /*
	      <permission action="action"><role>cdata role</role></permission>
	    */
	    List<Role> roles = permission.getRoles();
	    for(Role role: roles) {
		Element roleNode = XmlUtil.create(doc, TAG_ROLE, actionNode);
		roleNode.setAttribute(ATTR_ROLE, role.getRole());
		
	    }
	}
    }



    /**
     * _more_
     */
    public synchronized void clearCache() {
        recentPermissions.clearCache();
        //Since clearCache gets called at startup  we only want to
        //fetch the data policies  when we have already started to fetch
        //them since we might also be fetching them from ourselves
        if (haveDoneDataPolicyFetch) {
            updateLocalDataPolicies();
        }

    }


    /**
     */
    @Override
    public void initAttributes() {
        super.initAttributes();
        stopAtFirstRole = getRepository().getProperty(PROP_STOPATFIRSTROLE,  true);
        debugAction = getRepository().getProperty("ramadda.debugaction",   false);
    }


    /**
     * _more_
     *
     * @param mainEntry _more_
     *
     * @throws Exception _more_
     */
    public void initTopEntry(Entry mainEntry) throws Exception {
        List<Permission> permissions = new ArrayList<Permission>();
        Utils.add(
		  permissions,
		  new Permission(Permission.ACTION_VIEW, Role.ROLE_ANY),
		  new Permission(Permission.ACTION_VIEWCHILDREN, Role.ROLE_ANY),
		  new Permission(Permission.ACTION_FILE, Role.ROLE_ANY),
		  new Permission(Permission.ACTION_GEO, Role.ROLE_ANY),		  
		  new Permission(Permission.ACTION_EXPORT, Role.ROLE_NONE),
		  new Permission(Permission.ACTION_EDIT, Role.ROLE_NONE),
		  new Permission(Permission.ACTION_NEW, Role.ROLE_NONE),
		  new Permission(Permission.ACTION_DELETE, Role.ROLE_NONE),
		  new Permission(Permission.ACTION_COMMENT, Role.ROLE_ANY));
        mainEntry.setPermissions(permissions);
        insertPermissions(null, mainEntry, permissions);
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
        boolean debug =false;
	if(debug) System.err.println("canDoAction:" + action);
        if (getRepository().isReadOnly()) {
            if ( !(action.equals(Permission.ACTION_VIEW)
		   || action.equals(Permission.ACTION_VIEWCHILDREN)
		   || action.equals(Permission.ACTION_FILE))) {
		if(debug) System.err.println("\tisReadOnly and action isn't allowed");
                return false;
            }
        }
        User user = request.getUser();
        //The admin can do anything
        if (user.getAdmin()) {
	    if(debug) System.err.println("\tisAdmin");
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


    public boolean canDoAction(Request request, Entry entry, String action,
                               boolean log)
	throws Exception {

        boolean debug =debugAll;
        boolean debugFail =debugAction || debug;

	if(debug) System.err.println("canDoAction:" + action+" entry:" + entry);
        if (getRepository().isReadOnly()) {
            if ( !(action.equals(Permission.ACTION_VIEW)
		   || action.equals(Permission.ACTION_EXPORT)
		   || action.equals(Permission.ACTION_VIEWCHILDREN)
		   || action.equals(Permission.ACTION_FILE))) {
		if(debugFail) System.err.println("AccessManager: repository isReadOnly");
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

    private boolean canDoAction(Request request, String requestIp, User user,
                                boolean log, Entry entry, String action)
	throws Exception {

        boolean debug =debugAll;
        boolean debugFail =debugAction || debug;
	if(debug) System.err.println("canDoAction: user:" + user +" entry:" + entry +" action:" + action);
        if (entry == null) {
	    if(debugFail) System.err.println("** AccessManager.canDoAction: no entry:" + entry);
            return false;
        }

        if (entry.getIsLocalFile()) {
            if (action.equals(Permission.ACTION_NEW)) {
		if(debugFail) System.err.println("** AccessManager.canDoAction: newing a local file:" + entry);
                return false;
            }
            if (action.equals(Permission.ACTION_DELETE)) {
                if (getStorageManager().isProcessFile(entry.getFile())) {
                    return true;
                }

		if(debugFail) System.err.println("** AccessManager.canDoAction: isLocalFile and action is delete:" + entry);
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
		if(debugFail) System.err.println("** AccessManager: cannot view:" + entry);
                return false;
            }
        }



        if (user == null) {
            logInfo("Upload:canDoAction: user is null");
	    if(debugFail) System.err.println("** AccessManager: no user specified:" + entry);
            return false;
        }


        //The admin can do anything
        if (user.getAdmin()) {
            if (log) {
                logInfo("Upload:user is admin");
            }
	    if(debugFail) System.err.println("AccessManager: ok: user is admin");
            return true;
        }


        //If user is owner then they can do anything
        if ( !user.getAnonymous() && Misc.equals(user, entry.getUser())) {
            if (log) {
                logInfo("Upload:user is owner");
            }

	    if(debugFail) System.err.println("AccessManager: ok: user is owner of entry");
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
		if(debug) System.err.println("\tcached:" + ok);
                return ok.booleanValue();
            } else {
                recentPermissions.remove(key);
            }
        }

	//        boolean debug = false;
        //      debug = action.equals("view") && entry.getName().indexOf("test")>=0; 
        boolean result = canDoActionInner(request, requestIp, user, log,
                                          entry, action);
	if(!result) {
	    debug = true;
	    canDoActionInner(request, requestIp, user, log, entry, action);
	    debug = false;
	}

	if(debug) System.err.println("\tresult:" + result);
	    
        if (debug) {
            //      System.err.println("CANDO:" + entry +" " + result);
            //      System.err.println(Utils.getStack(10));
        }
        //        logInfo("Upload:canDoAction:  result= " + result);
        if (recentPermissions.size() > 10000) {
            clearCache();
        }
        recentPermissions.put(key, new Object[] { now, Boolean.valueOf(result) });

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
	    //A hack so the default for accessing GEO is true
	    if(action.equals(Permission.ACTION_GEO))  return true;
            return false;
        }
        List<Role> roles      = getRoles(entry, action);
        boolean debug =debugAll;
	//	debug = action.equals(Permission.ACTION_VIEW) && entry.getId().equals("53e607ef-5593-4ca9-adc0-618425e0ea98");
	if(debug)
	    System.err.println("canDoActionInner:" + user +" entry:"+ entry +" id:" + entry +" action:" + action +" roles:" + roles);
        boolean    hadInherit = false;
        boolean    hadAny     = false;
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
		    //                    System.err.println("\tROLE:" + role.getBaseRole() + " negated:" + negated);
                }
		if(role.isDate()) {
		    if(!role.dateOk()) {
			return negated;
		    } else {
			return !negated;
		    }
		}
                if (role.getIsIp()) {
                    if (requestIp != null) {
                        if (requestIp.startsWith(role.getBaseRole())) {
                            if (debug) {
				//                                System.err.println("\tIP negated:" + negated   + " " + requestIp);
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
			//                        System.err.println("\tIs ANY negated: " + negated);
                    }

                    return !negated;
                }
                if (Role.ROLE_NONE.isRole(role)) {
                    if (debug) {
			//                        System.err.println("\tIs NONE negated:" + negated);
                    }

                    return negated;
                }
                if (user.isRole(role)) {
                    if (debug) {
			//                        System.err.println("\tuser.isRole: " + role);
                    }

                    return !negated;
                }
            }
        }


        //LOOK: make sure we pass in false here which says do not check for access control
        Entry parent = getEntryManager().getParent(request, entry, false);
        if (parent != null) {
            return canDoAction(request, requestIp, user, log, parent, action);
        }
	//A hack so the default for accessing GEO is true
	if(action.equals(Permission.ACTION_GEO))  return true;



	if(entry.isFile() && getStorageManager().isProcessFile(entry.getFile())) {
	    if(debug)
		System.err.println("isProcessFile:" + entry);
	    return true;
	}


	if(entry.getTypeHandler().equals(getRepository().getProcessFileTypeHandler())) {
	    if(debug)
		System.err.println("isProcess:" + entry);
	    return true;
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
        List<Role>       roles       = new ArrayList<Role>();
        List<Permission> permissions = entry.getPermissions();
        if (permissions == null) {
            return roles;
        }
        for (Permission permission : permissions) {
            if (permission.isAction(action)) {
                roles.addAll(permission.getRoles());
            }
        }

        return roles;
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
	    if(debugAction) {
		System.err.println("AccessManager: robots cannot view files:" + entry);
	    }
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
	return canDownload(request, entry, false);
    }

    public boolean canDownload(Request request, Entry entry,boolean debug)
	throws Exception {	
	if(debug) System.err.println("canDownload:" + entry); 
        if ( !getRepository().getDownloadOk()) {
	    if(debug) System.err.println("\tcanDownload: repository disallows");
            return false;
        }
        entry = filterEntry(request, entry);
        if (entry == null) {
	    if(debug) System.err.println("\tcanDownload: filterEntry=null");
            return false;
        }


        //        System.err.println ("type: " + entry.getTypeHandler().getClass().getName());
        if ( !entry.getTypeHandler().canDownload(request, entry)) {
	    if(debug) System.err.println("\tcanDownload: typeHandler disallows:" +
					 entry.getTypeHandler());
            return false;
        }

        if ( !canDoAction(request, entry, Permission.ACTION_FILE)) {
	    if(debug) System.err.println("\tcanDownload: no file permission");
            return false;
        }

        boolean can =  getStorageManager().canDownload(request, entry,debug);
	if(debug) System.err.println("\tcanDownload: storage manager:" + can);
	return can;
    }



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
	//	debug = true;
	if(debug) System.err.println("filterEntry:" + entry);
        if (entry.getIsRemoteEntry()) {
	    if(debug) System.err.println("\tok: is Remote");
            return entry;
        }
        if (entry.getResource() != null) {
            if (!entry.getResource().isS3() && entry.getResource().isFileType()) {
                if ( !entry.getResource().getTheFile().exists()) {
                    if(debug)System.err.println ("\tfile is missing:" +
						 entry.getResource().getTheFile());
                    entry = getEntryManager().handleMissingFileEntry(request,
								     entry);
                    if (entry == null) {
			if(debug)
			    System.err.println ("\tnot ok: missing entry got deleted");
                        return null;
                    }
                }
            }
        }
        //        System.err.println ("filter:" + entry.getFullName());
        long t1 = System.currentTimeMillis();
        if ( !canDoAction(request, entry, Permission.ACTION_VIEW)) {
	    if(debug) System.err.println("\tnot ok: cannot do view");
            return null;
        }
        long t2 = System.currentTimeMillis();
        //        System.err.println ("time to filter:" + (t2-t1));


        Entry parent = entry.getParentEntry();
        if ((parent != null)
	    && !canDoAction(request, parent,
			    Permission.ACTION_VIEWCHILDREN)) {
	    if(debug) System.err.println("\tnot ok: parent cannot view children");
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
	if(entry!=null && getEntryManager().isSynthEntry(entry.getId())) return false;
        return canDoAction(request, entry, Permission.ACTION_EDIT);
    }


    public boolean canDoGeo(Request request, Entry entry)  {
        //        if(entry.getIsLocalFile()) return false;
	try {
	    return canDoAction(request, entry, Permission.ACTION_GEO);
	} catch(Exception exc) {
	    throw new RuntimeException(exc);
	}
    }    

    /**
     *
     * @param request _more_
     * @param entry _more_
     *  @return _more_
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
     *  @return _more_
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
     *  @return _more_
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
     *  @return _more_
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
     *  @return _more_
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
     *  @return _more_
     *
     * @throws Exception _more_
     */
    public boolean canDoNew(Request request, Entry entry) throws Exception {
	if(entry!=null && getEntryManager().isSynthEntry(entry.getId())) return false;
        return canDoAction(request, entry, Permission.ACTION_NEW);
    }

    /**
     *
     * @param request _more_
     * @param entry _more_
     *  @return _more_
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
     *  @return _more_
     *
     * @throws Exception _more_
     */
    public boolean canDoDelete(Request request, Entry entry)
	throws Exception {
	if(entry!=null && getEntryManager().isSynthEntry(entry.getId())) return false;
        return canDoAction(request, entry, Permission.ACTION_DELETE);
    }

    /**
     *
     * @param request _more_
     * @param entry _more_
     *  @return _more_
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
    public void listAccess(Request request, Entry entry, StringBuilder sb,boolean even)
	throws Exception {
        if (entry == null) {
            return;
        }
        List<Permission> permissions = getPermissions(entry);
        String entryUrl = HU.href(request.makeUrl(URL_ACCESS_FORM,
							 ARG_ENTRYID, entry.getId()), entry.getName());

        Hashtable<String, List<Permission>> map = new Hashtable<String,
	    List<Permission>>();
        for (Permission permission : permissions) {
            List<Permission> p =
                (List<Permission>) map.get(permission.getAction());
            if (p == null) {
                map.put(permission.getAction(),
                        p = new ArrayList<Permission>());
            }
            p.add(permission);
        }

        StringBuffer cols = new StringBuffer(HU.cols(HU.div(entryUrl)));
        for (int i = 0; i < Permission.ACTIONS.length; i++) {
            List<Permission> p = map.get(Permission.ACTIONS[i]);
            if (p == null) {
                cols.append(HU.cols("&nbsp;"));
            } else {
                StringBuilder tmp = null;
                for (Permission permission : p) {
                    for (Role role : permission.getRoles()) {
                        if (tmp == null) {
                            tmp = new StringBuilder();
                        }
                        String label = role.toString();
                        String clazz = role.getCssClass();
                        if (getDataPolicy(permission) != null) {
                            clazz = "ramadda-role-datapolicy " + clazz;
                            DataPolicy dp    = getDataPolicy(permission);
                            String     title = "data policy:" + dp.getName();
                            String     url   = dp.getMyUrl();
                            if ( !Utils.stringDefined(url)) {
                                url = dp.getMainUrl();
                            }
                            label = HU.href(url, label,
                                            HU.attrs("target", "_datapolicy",
						     "title", title)) + "&nbsp;*";

                        }
                        tmp.append(HU.div(label,
                                          HU.cssClass("ramadda-role "
						      + clazz)));
                    }
                }
                cols.append(HU.cols(tmp.toString()));
            }
        }
        sb.append(HU.row(
			 cols.toString(),
			 HU.attr("valign", "top")
			 + HU.cssClass("ramadda-access-summary "+(even?"ramadda-row-even":"ramadda-row-odd"))));
        listAccess(request,
                   getEntryManager().getEntry(request,
					      entry.getParentEntryId()), sb,!even);
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
    private void insertPermissions(Request request, Entry entry,
                                   List<Permission> permissions)
	throws Exception {
        clearCache();
        getDatabaseManager().delete(
				    Tables.PERMISSIONS.NAME,
				    Clause.eq(Tables.PERMISSIONS.COL_ENTRY_ID, entry.getId()));

        for (Permission permission : permissions) {
            List<Role> roles        = permission.getRoles();
            String     dataPolicyId = permission.getDataPolicyId();
            for (Role role : roles) {
                getDatabaseManager().executeInsert(Tables.PERMISSIONS.INSERT,
						   new Object[] { entry.getId(),
								  permission.getAction(),
								  role.getRole(), (dataPolicyId == null)
								  ? ""
								  : dataPolicyId });
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
     *
     * @param entry _more_
     * @param inherited _more_
     *  @return _more_
     *
     * @throws Exception _more_
     */
    public List<DataPolicy> getDataPolicies(Entry entry, boolean inherited)
	throws Exception {
        List<DataPolicy> dataPolicies = new ArrayList<DataPolicy>();
        if (this.dataPolicies.size() == 0) {
            return dataPolicies;
        }

        HashSet seen = new HashSet();
        while (entry != null) {
            List<Permission> permissions = getPermissions(entry);
            for (Permission permission : permissions) {
                if (Utils.stringDefined(permission.getDataPolicyId())) {
                    if (seen.contains(permission.getDataPolicyId())) {
                        continue;
                    }
                    seen.add(permission.getDataPolicyId());
                    DataPolicy dataPolicy =
                        dataPoliciesMap.get(permission.getDataPolicyId());
                    if (dataPolicy != null) {
                        dataPolicies.add(dataPolicy);
                    }
                }
            }
            if ( !inherited) {
                break;
            }
            entry = entry.getParentEntry();
        }

        return dataPolicies;
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
            return getUpdatedPermissions(entry, permissions);
        }
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
            int    col          = 1;
            String id           = results.getString(col++);
            String action       = results.getString(col++);
            String role         = results.getString(col++);
            String dataPolicyId = results.getString(col++);
            //      System.err.println("action:" + action +" dp:" + dataPolicyId);
            String key = action;
            if (Utils.stringDefined(dataPolicyId)) {
                key = key + "-" + dataPolicyId;
            }

            List<Role> roles = actions.get(key);
            if (roles == null) {
                roles = new ArrayList<Role>();
                Permission permission = new Permission(dataPolicyId, action,
						       roles);
                permissions.add(permission);
                actions.put(key, roles);
            }
            roles.add(new Role(role));
        }

        //Update the permissions with any changed datapolicies
        return getUpdatedPermissions(entry, permissions);
    }



    /**
     *
     * @param entry _more_
     * @param permissions _more_
     *  @return _more_
     */
    private List<Permission> getUpdatedPermissions(Entry entry,
						   List<Permission> permissions) {
        boolean hasDataPolicy = false;
        boolean debug =false;

        if (debug) {
            System.err.println("getUpdatePermissions:" + entry);
        }
        for (Permission permission : permissions) {
            if (Utils.stringDefined(permission.getDataPolicyId())) {
                if (debug) {
                    System.err.println("\thas data policy:" + permission);
                }
                hasDataPolicy = true;

                break;
            }
            if (debug) {
                System.err.println("\tno dataPolicy:" + permission);
            }
        }

        if ( !hasDataPolicy) {
            entry.setPermissions(permissions);

            return permissions;
        }
        List<Permission> result                = new ArrayList<Permission>();
        HashSet          seenDataPolicies      = new HashSet();
        List<DataPolicy> dataPolicyPermissions = new ArrayList<DataPolicy>();
        for (Permission permission : permissions) {
            String dataPolicyId = permission.getDataPolicyId();
            if ( !Utils.stringDefined(dataPolicyId)) {
                result.add(permission);
                continue;
            }
            DataPolicy dataPolicy = dataPoliciesMap.get(dataPolicyId);
            if (dataPolicy == null) {
                result.add(permission);
                continue;
            }

            //Keep track of the unique permissions per policy
            if ( !seenDataPolicies.contains(dataPolicyId)) {
                seenDataPolicies.add(dataPolicyId);
                dataPolicyPermissions.add(dataPolicy);
            }
        }
        //Add the permissions associated with the data policy. Put them at the end so they are lesser priority
        for (DataPolicy dataPolicy : dataPolicyPermissions) {
            result.addAll(dataPolicy.getPermissions());
        }
        entry.setPermissions(result);

        return result;
    }

    /**
     *
     * @param permission _more_
     *  @return _more_
     */
    private DataPolicy getDataPolicy(Permission permission) {
	return getDataPolicy(permission.getDataPolicyId());
    }

    private DataPolicy getDataPolicy(String id) {	
        if ( !Utils.stringDefined(id)) {
            return null;
        }
        return dataPoliciesMap.get(id);
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
        formatDataPolicies(request, sb, dataPolicies, true, true);
        getPageHandler().sectionClose(request, sb);

        return new Result("Data Policies", sb);
    }



    /**
     *
     * @param request _more_
     * @param sb _more_
     * @param dataPolicies _more_
     * @param includeCollection _more_
     * @param includePermissions _more_
     *
     * @throws Exception _more_
     */
    public void formatDataPolicies(Request request, Appendable sb,
                                   List<DataPolicy> dataPolicies,
                                   boolean includeCollection,
                                   boolean includePermissions)
	throws Exception {
        LinkedHashMap<String, StringBuilder> map = new LinkedHashMap<String,
	    StringBuilder>();
        for (DataPolicy dataPolicy : dataPolicies) {
            String fromLabel = dataPolicy.getFromName();
            String url       = dataPolicy.getMainUrl();
            try {
                URL u = new URL(url);
                fromLabel = fromLabel + " - " + u.getHost();
            } catch (Exception exc) {}
            String        href = HU.href(url, fromLabel);
            StringBuilder buff = map.get(href);
            if (buff == null) {
                map.put(href, buff = new StringBuilder());
            }
            String label = dataPolicy.getName();
            if (Utils.stringDefined(dataPolicy.getMyUrl())) {
                label = HU.href(dataPolicy.getMyUrl(), label,
                                HU.attrs("target", "_datapolicy"));
            }

            buff.append(
			HU.div(HU.img(
					     getRepository().getIconUrl(
									"fa-solid fa-building-shield")) + " "
			       + HU.italics(label)));
            if (Utils.stringDefined(dataPolicy.getDescription())) {
                buff.append(
			    HU.div(dataPolicy.getDescription(),
				   HU.cssClass("ramadda-datapolicy-description")));
            }
            buff.append("<ul>");
            for (String citation : dataPolicy.getCitations()) {
                if (Utils.stringDefined(citation)) {
                    buff.append(
				HU.div(citation.replaceAll("\n", "<br>"),
				       HU.cssClass("ramadda-datapolicy-citation")));
                }
            }
            boolean       didLicenses = false;
            StringBuilder lbuff       = new StringBuilder();
            for (License license : dataPolicy.getLicenses()) {
                didLicenses = true;
                lbuff.append(
			     HU.div(getMetadataManager().getLicenseHtml(
									license, null,false)));
            }
            if (didLicenses) {
                String tmp = HU.div(lbuff.toString(),
                                    HU.style("margin-left:20px;"));
                buff.append(HU.makeShowHideBlock("Licenses", tmp, true));
            }


            //If they are logged then show the access
            if (includePermissions && !request.isAnonymous()) {
                if (dataPolicy.getPermissions().size() > 0) {
                    StringBuilder permissionsSB = new StringBuilder();
                    permissionsSB.append("<ul>");
                    for (Permission permission :
			     dataPolicy.getPermissions()) {
                        permissionsSB.append("<li>");
                        permissionsSB.append("Action: ");
                        permissionsSB.append(permission.getAction());
                        permissionsSB.append("<br>Roles:<ul> ");
                        for (Role role : permission.getRoles()) {
                            permissionsSB.append("<li>");
                            permissionsSB.append(HU.span(role.toString(),
							 HU.cssClass(role.getCssClass())));
                        }
                        permissionsSB.append("</ul>");
                    }
                    permissionsSB.append("</ul>");
                    buff.append(HU.makeShowHideBlock("Permissions",
						     permissionsSB.toString(), false));
                }
            }
            buff.append("</ul>");

        }

        sb.append("<ul>");
        for (String key : map.keySet()) {
            StringBuilder buff = map.get(key);
            if (includeCollection) {
                sb.append("<li>");
                sb.append(key);
                sb.append("<ul>");
            }
            sb.append(buff);
            if (includeCollection) {
                sb.append("</ul>");
            }
        }
        sb.append("</ul>");
    }


    public void getCurrentAccess(Request request, Entry entry,StringBuilder currentAccess)
	throws Exception {
        currentAccess.append(HU.open(HU.TAG_TABLE,
                                            HU.attrs("celladding", "0",
						     "cellspacing", "0")));
        StringBuffer header =
            new StringBuffer(HU.cols(HU.bold(msg("Entry"))));
        for (int i = 0; i < Permission.ACTIONS.length; i++) {
            header.append(HU.cols(msg(Permission.ACTION_NAMES[i])));
        }
        currentAccess.append(
			     HU.row(
					   header.toString(),
					   HU.attr("valign", "top")
					   + HU.cssClass("ramadda-access-summary-header")));

        listAccess(request, entry, currentAccess,true);
        currentAccess.append(HU.close(HU.TAG_TABLE));

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

        boolean debug =debugAll;
        StringBuffer sb    = new StringBuffer();
        Entry        entry = getEntryManager().getEntry(request);

        if ( !canSetAccess(request, entry)) {
            throw new AccessException("Can't set access", request);
        }

        getPageHandler().entrySectionOpen(request, entry, sb, "Define Permissions");
        request.appendMessage(sb);

        StringBuilder currentAccess = new StringBuilder();
	getCurrentAccess(request, entry,currentAccess);
        Hashtable        map         = new Hashtable();
        List<Permission> permissions = getPermissions(entry);
        if (debug) {
            System.err.println("\n*** making access form");
        }
        HashSet<String> dpMap = new HashSet<String>();
        for (Permission permission : permissions) {
            if (getDataPolicy(permission) != null) {
                if (debug) {
                    System.err.println("data policy permission:"
                                       + permission);
                }
                dpMap.add(permission.getDataPolicyId());
                continue;
            }
            if (debug) {
                System.err.println("regular permission:" + permission);
            }
            List roles = permission.getRoles();
            map.put(permission.getAction(), StringUtil.join("\n", roles));
        }
        if (debug) {
            System.err.println("dp map:" + dpMap);
        }
	
	HU.center(sb,getWikiManager().wikifyEntry(request, entry,"{{access_status}}"));
        request.formPostWithAuthToken(sb, URL_ACCESS_CHANGE, "");
        sb.append(HU.hidden(ARG_ENTRYID, entry.getId()));
        sb.append("<br>");
        sb.append(HU.submit("Change Access"));
        sb.append("<br>");
        if (dataPolicies.size() > 0) {
            List         items    = new ArrayList();
            List<String> selected = new ArrayList<String>();
            if (debug) {
                System.err.println("making data policy menu");
            }
            for (DataPolicy dataPolicy : dataPolicies) {
                if (debug) {
                    System.err.println("\tdata policy:" + dataPolicy);
                }
                if (dpMap.contains(dataPolicy.getId())) {
                    if (debug) {
                        //                        System.err.println("is selected:" + dataPolicy);
                    }
                    selected.add(dataPolicy.getId());
                } else {
                    if (debug) {
                        //System.err.println("is not selected:" + dataPolicy);
                    }
                }
                //                items.add(new TwoFacedObject(dataPolicy.getName(),  dataPolicy.getId()));
                items.add(new HtmlUtils.Selector(dataPolicy.getName(),
						 dataPolicy.getId(), dataPolicy.getLabel(), null, 0,
						 0, false));
            }
            String extraSelect = HU.cssClass("ramadda-pulldown")
		+ HU.attr(HU.ATTR_MULTIPLE,
				 "true") + HU.attr("size",
							  "" + (Math.min(items.size(), 4)));
            if (debug) {
                System.err.println("items:" + items);
            }
            String select = HU.select(ARG_DATAPOLICY, items, selected,
                                      extraSelect, 100);
	    String help = HU.href(getRepository().getUrlBase()    + "/access/datapolicies",
				  HU.getIconImage("fas fa-binoculars"),
				  HU.attrs("title","View data policies",
					   "target", "_datapolicies"));
	    HU.span(sb,HU.b("Data Policy") + " " + help +"<br>" + select,"");
        }

        sb.append("<table id='accessform' style=''><tr valign=top>");
        sb.append("<td>");
        sb.append("<table style='margin-right:20px;' >");
        List opts = new ArrayList();
        Utils.add(
		  opts, new TwoFacedObject("Add role", ""),
		  new TwoFacedObject("Anyone",Role.ROLE_ANY.getRole()),
		  new TwoFacedObject("No one", Role.ROLE_NONE.getRole()),
		  new TwoFacedObject("Logged in user", Role.ROLE_USER.getRole()),
		  new TwoFacedObject("Anonymous users", Role.ROLE_ANONYMOUS.getRole()),
		  new TwoFacedObject("User ID", "user:&lt;user id&gt;"),
		  new TwoFacedObject("Guest user", Role.ROLE_GUEST.getRole()),
		  new TwoFacedObject("IP Address", "ip:&lt;ip address&gt;"),
		  new TwoFacedObject("Date","date:yyyy-MM-dd"));

	for(Role role:getUserManager().getUserRoles()) {
	    opts.add(new TwoFacedObject("User Role:" + role,role));
	}
        sb.append("<tr valign=top>");
        sb.append(HU.cols(HU.bold(msg("Action"))));
        sb.append("<td colspan=2>");
        sb.append(HU.bold("Role")
                  + ". One per line. Prefix with \"!\" for negation");
        sb.append("</td>");
        sb.append("</tr>");
        for (int i = 0; i < Permission.ACTIONS.length; i++) {
            String roles = (String) map.get(Permission.ACTIONS[i]);
            if (roles == null) {
                roles = "";
            }
            String actionName = Permission.ACTION_NAMES[i];
            String action     = Permission.ACTIONS[i];
            if (action.equals(Permission.ACTION_TYPE1)) {
                actionName = entry.getTypeHandler().getTypePermissionName(Permission.ACTION_TYPE1);
            } else if (action.equals(Permission.ACTION_TYPE2)) {
                actionName = entry.getTypeHandler().getTypePermissionName(Permission.ACTION_TYPE2);
            }
	    if(actionName==null) continue;
            String label =
                HU.href(
			       getRepository().getUrlBase() + "/userguide/access.html#"
			       + action, HU.img(
						       getRepository().getIconUrl(
										  "fas fa-question-circle")), HU.attr(
															     HU.ATTR_TARGET,
															     "_help")) + HU.space(1)
		+ msg(actionName);
            String extra = "";
            if (i == 0) {
                extra = HU.select("", opts, (String) null, HU.id("roles"));
            }
            extra = HU.div(extra, HU.id("holder_" + i));
            sb.append(HU.rowTop(HU.cols(label,
						      HU.textArea(ARG_ROLES + "."
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
        sb.append(HU.formTableClose());
        HU.importJS(sb, getRepository().getUrlBase() + "/accessform.js");
        HU.script(sb, "Ramadda.initAccessForm();");



        //        sb.append("</td><td>&nbsp;&nbsp;&nbsp;</td><td>");
        //        sb.append("All Roles:<br>");
        //        sb.append(StringUtil.join("<br>",getUserManager().getStandardRoles()));
        //        sb.append("</td></tr></table>");
        sb.append(HU.submit("Change Access"));
        sb.append(HU.formClose());

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
        getAuthManager().ensureAuthToken(request);
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

        if (request.exists(ARG_DATAPOLICY)) {
            for (String id :
		     (List<String>) request.get(ARG_DATAPOLICY,
						new ArrayList<String>())) {
                DataPolicy dataPolicy = dataPoliciesMap.get(id);
                if (dataPolicy != null) {
                    permissions.addAll(dataPolicy.getPermissions());
                }
            }
        }
        synchronized (MUTEX_PERMISSIONS) {
            insertPermissions(request, entry, permissions);
        }

        return new Result(request.makeUrl(URL_ACCESS_FORM, ARG_ENTRYID,
                                          entry.getId(), ARG_MESSAGE,MSG_ACCESS_CHANGED));

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
