/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/
//jeffmc: change the package name to a ramadda package
package org.ramadda.plugins.ldap;


import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.NoSuchAttributeException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;


/**
 * Does the work of communicating with the ldap server
 *
 */
@SuppressWarnings("unchecked")
public class LDAPManager {

    /** The LDAPManager instance object */
    private static Map instances = new HashMap();

    /** _more_ */
    private String ldapUrl;

    /** _more_ */
    private String mainUserId;

    /** _more_ */
    private String mainPassword;


    /** _more_ */
    private String userPath;

    /** _more_ */
    private String groupsPath;



    /** The connection, through a <code>DirContext</code>, to LDAP */
    private DirContext theContext;



    /**
     * _more_
     *
     *
     * @param ldapUrl _more_
     * @param userPath _more_
     * @param groupsPath _more_
     * @param username _more_
     * @param password _more_
     *
     * @throws NamingException _more_
     */
    protected LDAPManager(String ldapUrl, String userPath, String groupsPath,
                          String username, String password)
            throws NamingException {
        this.ldapUrl      = ldapUrl;
        this.userPath     = userPath;
        this.groupsPath   = groupsPath;
        this.mainUserId   = username;
        this.mainPassword = password;
        //Try it
        if (ldapUrl != null) {
            getContext();
        } else {
            log("No ldap properties defined");
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isEnabled() {
        if (ldapUrl == null) {
            return false;
        }

        return true;
    }

    /**
     * _more_
     *
     * @return _more_
     *
     * @throws NamingException _more_
     */
    private DirContext getContext() throws NamingException {
        DirContext localContext = theContext;

        //Create the first time
        if (localContext == null) {
            log("Creating initial context:" + ldapUrl);
            localContext = getInitialContext(ldapUrl, mainUserId,
                                             mainPassword);
        }

        //Try to connect with a dummy path
        try {
            localContext.getAttributes("dummypath");
        } catch (InvalidNameException ignoreThis) {
            //            System.err.println ("Connection OK with dummy path");
        } catch (Exception badConnection) {
            //            System.err.println ("bad connection:" + badConnection);
            //Maybe the connection got dropped so we'll try again
            localContext = null;
        }

        if (localContext == null) {
            //            System.err.println ("Trying again");
            localContext = getInitialContext(ldapUrl, mainUserId,
                                             mainPassword);
            //            System.err.println ("OK");
        }
        theContext = localContext;

        return localContext;
    }


    /**
     * _more_
     *
     *
     * @param ldapUrl _more_
     * @param userPath _more_
     * @param groupPath _more_
     * @param username _more_
     * @param password _more_
     *
     * @return _more_
     *
     * @throws NamingException _more_
     */
    public static LDAPManager getInstance(String ldapUrl, String userPath,
                                          String groupPath, String username,
                                          String password)
            throws NamingException {

        // Construct the key for the supplied information
        String key =
            new StringBuffer().append(ldapUrl).append("|").append(((username
                == null)
                ? ""
                : username)).append("|").append(((password == null)
                ? ""
                : password)).toString();

        if ( !instances.containsKey(key)) {
            synchronized (LDAPManager.class) {
                if ( !instances.containsKey(key)) {
                    LDAPManager instance = new LDAPManager(ldapUrl, userPath,
                                               groupPath, username, password);
                    instances.put(key, instance);

                    return instance;
                }
            }
        }

        return (LDAPManager) instances.get(key);
    }


    /**
     * _more_
     *
     * @param username _more_
     * @param password _more_
     *
     * @return _more_
     */
    public boolean isValidUser(String username, String password) {
        String userDN = getUserDN(username);
        try {
            DirContext tmpContext = getInitialContext(ldapUrl, userDN,
                                        password);

            return true;
        } catch (javax.naming.NameNotFoundException e) {
            log("Name or password not found:" + userDN);

            return false;
        } catch (NamingException e) {
            log("Error validating user:" + userDN + " " + e);

            return false;
        }
    }

    /**
     * _more_
     *
     * @param username _more_
     * @param groupName _more_
     *
     * @return _more_
     *
     * @throws NamingException _more_
     */
    public boolean userInGroup(String username, String groupName)
            throws NamingException {
        // Set up attributes to search for
        String[] searchAttributes = new String[1];
        searchAttributes[0] = "uniqueMember";
        String groupDN = getGroupDN(groupName);
        Attributes attributes = getContext().getAttributes(groupDN,
                                    searchAttributes);
        if (attributes == null) {
            log("Could not find group attributes:" + groupDN);

            return false;
        }

        Attribute memberAtts = attributes.get("uniqueMember");
        if (memberAtts == null) {
            log("Could not find group member attributes:" + groupDN);

            return false;
        }

        for (NamingEnumeration vals = memberAtts.getAll();
                vals.hasMoreElements(); ) {
            if (username.equalsIgnoreCase(
                    getUserUID((String) vals.nextElement()))) {
                return true;
            }
        }

        return false;
    }

    /*
     * Get the attributes defined by the variable userAttributes and their values
     *
     * @param username          Name of the user
     *
     * @return userAttributes   Hashtable keys are the attributes names and
     *                          hashtable values are the attribute values
     *
     */

    /**
     * _more_
     *
     * @param username _more_
     *
     * @return _more_
     *
     * @throws NamingException _more_
     */
    public Hashtable<String, List<String>> getUserAttributes(String username)
            throws NamingException {
        Hashtable<String, List<String>> userAttributes =
            new Hashtable<String, List<String>>();
        Attributes attributes =
            getContext().getAttributes(getUserDN(username));
        if (attributes == null) {
            return userAttributes;
        }
        for (NamingEnumeration ae = attributes.getAll(); ae.hasMore(); ) {
            Attribute    attr            = (Attribute) ae.next();
            List<String> attributeValues = new ArrayList<String>();
            for (NamingEnumeration e = attr.getAll(); e.hasMore(); ) {
                Object o = e.next();
                if (o instanceof String) {
                    attributeValues.add((String) o);
                }
            }
            if (attributeValues.size() > 0) {
                userAttributes.put((String) attr.getID(), attributeValues);
            }
        }

        return userAttributes;
    }


    /**
     * _more_
     *
     * @param username _more_
     * @param groupMemberAttribute _more_
     *  @return _more_
     *
     * @throws Exception _more_
     */
    public List<String> getGroupsForUser(String username,
                                         String groupMemberAttribute)
            throws Exception {

        try {
            List<String>                     groups =
                new LinkedList<String>();
            DirContext                       context = getContext();
            NamingEnumeration<NameClassPair> enums = context.list(groupsPath);
            String[] searchAttributes = new String[] { groupMemberAttribute };
            while (enums.hasMoreElements()) {
                NameClassPair key     = enums.nextElement();
                String        id      = key.getName();
                String        groupId = getGroupCN(id);
                Attributes attributes =
                    context.getAttributes(getGroupDN(groupId),
                                          searchAttributes);
                if (attributes == null) {
                    continue;
                }
                Attribute memberAtts = attributes.get(groupMemberAttribute);
                if (memberAtts == null) {
                    //                    log("Failed to find group member attribute:" + groupMemberAttribute);
                    continue;
                }
                for (NamingEnumeration vals = memberAtts.getAll();
                        vals.hasMoreElements(); ) {
                    if (username.equalsIgnoreCase(
                            getUserUID((String) vals.nextElement()))) {
                        groups.add(groupId);

                        break;
                    }
                }
            }
            if (groups.size() == 0) {
                log("No groups found for user:" + username + " groups path:"
                    + groupsPath + " member attr:" + groupMemberAttribute);
            }

            return groups;
        } catch (Exception exc) {
            log("Error reading groups:" + groupsPath + " member attribute:"
                + groupMemberAttribute + " error:" + exc.toString());

            throw exc;
        }


    }




    /** _more_ */
    private List<String> allGroups;

    /**
     * _more_
     *
     * @return _more_
     *
     * @throws NamingException _more_
     */
    public List<String> getAllGroups() throws NamingException {
        if (allGroups == null) {
            List<String> groups = new ArrayList();
            NamingEnumeration<NameClassPair> enums =
                getContext().list(groupsPath);
            while (enums.hasMoreElements()) {
                NameClassPair key = enums.nextElement();
                String        id  = key.getName();
                groups.add(getGroupCN(id));
            }
            allGroups = groups;
        }

        return allGroups;
    }

    /**
     * _more_
     *
     * @param username _more_
     *
     * @return _more_
     */
    private String getUserDN(String username) {
        return userPath.replace("${id}", username);
    }

    /**
     * _more_
     *
     * @param userDN _more_
     *
     * @return _more_
     */
    private String getUserUID(String userDN) {
        int start = userDN.indexOf("=");
        int end   = userDN.indexOf(",");

        if (end == -1) {
            end = userDN.length();
        }

        return userDN.substring(start + 1, end);
    }


    /**
     * _more_
     *
     * @param name _more_
     *
     * @return _more_
     */
    private String getGroupDN(String name) {
        StringBuffer sb = new StringBuffer();
        sb.append("cn=");
        sb.append(name);
        sb.append(",");
        sb.append(groupsPath);

        return sb.toString();
    }

    /**
     * _more_
     *
     * @param groupDN _more_
     *
     * @return _more_
     */
    private String getGroupCN(String groupDN) {
        int start = groupDN.indexOf("=");
        int end   = groupDN.indexOf(",");
        if (end == -1) {
            end = groupDN.length();
        }

        return groupDN.substring(start + 1, end);
    }


    /**
     * _more_
     *
     * @param msg _more_
     */
    public static void debug(String msg) {
        log(msg);
        //        System.err.println("LDAP:" + msg);
    }

    /**
     * _more_
     *
     * @param msg _more_
     */
    public static void log(String msg) {
	//        System.err.println("LDAP:" + msg);
    }

    /**
     *
     * @param ldapUrl _more_
     * @param username _more_
     * @param password _more_
     *  @return _more_
     * @throws NamingException _more_
     */
    private static DirContext getInitialContext(String ldapUrl,
            String username, String password)
            throws NamingException {

        log("Connecting to:" + ldapUrl);
        Properties props = new Properties();
        props.put(Context.INITIAL_CONTEXT_FACTORY,
                  "com.sun.jndi.ldap.LdapCtxFactory");
        props.put(Context.PROVIDER_URL, ldapUrl);

        if ((username != null) && ( !username.equals(""))) {
            log("User path:" + username);
            props.put(Context.SECURITY_AUTHENTICATION, "simple");
            props.put(Context.SECURITY_PRINCIPAL, username);
            props.put(Context.SECURITY_CREDENTIALS, ((password == null)
                    ? ""
                    : password));
        }

        DirContext context = new InitialDirContext(props);

        //        String suffix  = "dc=ldap,dc=int,dc=unavco,dc=org";
        //        walk(context, suffix, "");
        /*
        NamingEnumeration<NameClassPair> enums = context.list(suffix);
        while(enums.hasMoreElements()) {
            NameClassPair key = enums.nextElement();
            String id = key.getName();
            System.err.println("id:" + id);
        }
        */
        return context;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return ldapUrl;
    }


    /**
     * _more_
     *
     * @param context _more_
     * @param path _more_
     * @param tab _more_
     *
     * @throws NamingException _more_
     */
    private void walk(DirContext context, String path, String tab)
            throws NamingException {
        Attributes attributes = context.getAttributes(path);
        if (attributes != null) {
            for (NamingEnumeration ae = attributes.getAll(); ae.hasMore(); ) {
                Attribute attr = (Attribute) ae.next();
                System.out.print(tab + "attr: " + attr.getID());
                for (NamingEnumeration e = attr.getAll(); e.hasMore(); ) {
                    Object o = e.next();
                    if (o instanceof String) {
                        String value = (String) o;
                        System.out.print("   value: " + o);
                    } else {
                        System.out.print("   ?value: "
                                         + o.getClass().getName());
                    }
                }
                System.out.println("");
            }
        }

        NamingEnumeration<NameClassPair> enums = context.list(path);
        while (enums.hasMoreElements()) {
            NameClassPair key = enums.nextElement();
            String        id  = key.getName();
            System.out.println(tab + "id:" + id);
            walk(context, id + "," + path, tab + "  ");
        }
    }



}
