/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

/*
 * Originally written by Kristian Sebastian Blalid Coastal Ocean Observing and Forecast System, Balearic Islands ICTS
 */

package org.ramadda.plugins.ldap;


import org.ramadda.repository.Repository;
import org.ramadda.repository.Request;
import org.ramadda.repository.auth.User;
import org.ramadda.repository.auth.Role;
import org.ramadda.repository.auth.UserAuthenticator;
import org.ramadda.repository.auth.UserAuthenticatorImpl;

import org.ramadda.util.TTLCache;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


import javax.naming.NamingException;


/**
 * This is a user authenticator to implement LDAP authentication
 *
 *
 * @author Kristian Sebastian Blalid Coastal Ocean Observing and Forecast System, Balearic Islands ICTS
 * @author Jeff McWhirter ramadda.org
 */
@SuppressWarnings("unchecked")
public class LDAPUserAuthenticator extends UserAuthenticatorImpl {

    /** property name */
    private static final String PROP_GROUP_ADMIN = "ldap.group.admin";

    /** default value for admin role */
    private static final String DFLT_GROUP_ADMIN = "ramaddaadmin";

    /** _more_ */
    private static final String PROP_GROUP_ATTR = "ldap.group.attribute";

    /** _more_ */
    private static final String DFLT_GROUP_ATTR = "memberUid";

    /** property name */
    private static final String PROP_ATTR_GIVENNAME = "ldap.attr.givenname";

    /** default value for given name */
    private static final String DFLT_ATTR_GIVENNAME = "givenName";

    /** property name */
    private static final String PROP_ATTR_SURNAME = "ldap.attr.surname";

    /** default value for  surname */
    private static final String DFLT_ATTR_SURNAME = "sn";

    /** Manager for Ldap conection */
    private LDAPManager manager = null;

    /** _more_ */
    private int lastLDAPServerVersion = -1;

    /** User cache. Keep in memory for 60 minutes */
    private TTLCache<String, User> userCache = new TTLCache<String,
                                                   User>(60 * 60 * 1000);

    /**
     * constructor.
     */
    public LDAPUserAuthenticator() {
        //        LDAPManager.log("LDAPUserAuthenticator created");
    }

    /**
     * ctor
     *
     * @param repository the repository
     */
    public LDAPUserAuthenticator(Repository repository) {
        super(repository);
        //        LDAPManager.log("LDAPUserAuthenticator created");
    }

    //    public String toString() {
    //        return "LDAPUserAuthenticator:" + getManager() ;
    //    }



    /**
     * If not already created then create and return the LDAPManager.
     * If manager fails then log the error and return null
     *
     * @return LDAPManager
     */
    private LDAPManager getManager() {
        LDAPAdminHandler adminHandler =
            LDAPAdminHandler.getLDAPHandler(getRepository());

        if (adminHandler == null) {
            return null;
        }
        //Check if the admin handler has changed its state
        if ((lastLDAPServerVersion != adminHandler.getVersion())
                || (manager == null)) {
            try {
                lastLDAPServerVersion = adminHandler.getVersion();
                manager = LDAPManager.getInstance(adminHandler.getLdapUrl(),
                        adminHandler.getUserDirectory(),
                        adminHandler.getGroupDirectory(),
                        adminHandler.getAdminID(),
                        adminHandler.getPassword());
            } catch (Exception e) {
                logError("LDAP Error: creating LDAPManager", e);
            }
        }

        return manager;
    }

    /**
     * do we have a valid manager
     *
     * @return has valid manager
     */
    public boolean isEnabled() {
        LDAPManager manager = getManager();
        if (manager == null) {
            return false;
        }

        return manager.isEnabled();
    }

    /**
     * this gets called when we want to  autheticate the given user/password
     * return null if user/password is unknown or incorrect
     *
     * @param repository the repository
     * @param request the  http request
     * @param extraLoginForm anything extra to put in the login form
     * @param userId the user id
     * @param password the password they provided
     *
     * @return The user if the user id and password are correct. Else return null
     */
    public User authenticateUser(Repository repository, Request request,
                                 StringBuffer extraLoginForm, String userId,
                                 String password) {
        if (userId.length() == 0) {
            return null;
        }
        LDAPManager.debug("Authenticate user: " + userId);
        if ( !isEnabled()) {
            LDAPManager.debug("LDAP not enabled");
            return null;
        }
        userCache.remove(userId);
        if (getManager().isValidUser(userId, password)) {
            return findUser(repository, userId);
        } else {
            return null;
        }
    }

    /**
     * this gets called when we want to just get a User object from the ID.
     * return null if user is unknown
     *
     * @param repository the repository.
     * @param userId The user to find
     *
     * @return The  non-local user that matches the given id or null
     */
    @Override
    public User findUser(Repository repository, String userId) {
        if ( !isEnabled()) {
            return null;
        }
        if (userId.length() == 0) {
            return null;
        }
        try {
            User user = userCache.get(userId);
            if (user != null) {
                return user;
            }
            LDAPManager.debug("findUser: " + userId);

            // Hashtable with attributes names and their values
            Hashtable<String, List<String>> userAttr =
                getManager().getUserAttributes(userId);

            List<String> values;
            StringBuffer userName = new StringBuffer();
            values = (ArrayList) userAttr.get(
                getRepository().getProperty(
                    PROP_ATTR_GIVENNAME, DFLT_ATTR_GIVENNAME));
            if (values != null) {
                userName.append(values.get(0));
                userName.append(" ");
            }
            values = (ArrayList) userAttr.get(
                getRepository().getProperty(
                    PROP_ATTR_SURNAME, DFLT_ATTR_SURNAME));
            if (values != null) {
                userName.append(values.get(0));
            }

            if (userName.length() == 0) {
                userName.append(userId);
            }

            // Create the user with admin priviligies if user is in group reposAdmin
            String adminGroup = getRepository().getProperty(PROP_GROUP_ADMIN,
                                    DFLT_GROUP_ADMIN);
            boolean      isAdmin = false;
            List<String> roles   = new ArrayList<String>();
            //Get the list of groups 
            String groupMemberAttribute =
                getRepository().getProperty(PROP_GROUP_ATTR, DFLT_GROUP_ATTR);

            List<String> groups = getManager().getGroupsForUser(userId,
                                      groupMemberAttribute);
            for (String group : groups) {
                //Is this user a member of the ramadda admin group?
                if (group.equals(adminGroup)) {
                    isAdmin = true;
                } else {
                    roles.add(group);
                }
            }
            user   = new User(userId, userName.toString().trim(), isAdmin);
            values = (ArrayList) userAttr.get("email");
            if (values == null) {
                values = (ArrayList) userAttr.get("mail");
            }
            if (values != null) {
                user.setEmail(values.get(0));
            }

            user.setRoles(Role.makeRoles(roles));
            userCache.put(userId, user);

            return user;
        } catch (javax.naming.NameNotFoundException nnfe) {
            LDAPManager.log("Could not find user: " + userId + " " + nnfe);

            return null;
        } catch (Exception exc) {
            logError("LDAP Error: finding user", exc);

            return null;
        }
    }



    /**
     * This is used to list out the roles in the access pages
     *
     * @return _more_
     */
    @Override
    public List<String> getAllRoles() {
        if ( !isEnabled()) {
            return null;
        }
        try {
            return getManager().getAllGroups();
        } catch (Exception exception) {
            LDAPManager.log("LDAPUserAuthenticatorgetAllRoles:" + exception);

            return null;
        }
    }

    /**
     * this can be used to list out all of the users and display them
     * in RAMADDA
     * It is not used by RAMADDA right now
     *
     * @return _more_
     */
    @Override
    public List<User> getAllUsers() {
        return new ArrayList<User>();
    }


    /**
     * This will be used to allow this authenticator to add options
     * to the admin config form
     * Its not used right now
     *
     * @param repository _more_
     * @param sb _more_
     */
    @Override
    public void addToConfigurationForm(Repository repository,
                                       StringBuffer sb) {}

    /**
     * This will be used to allow this authenticator to set the options from the config form
     * to the admin config form
     * Its not used right now
     *
     * @param repository _more_
     * @param request _more_
     */
    @Override
    public void applyConfigurationForm(Repository repository,
                                       Request request) {}

}
