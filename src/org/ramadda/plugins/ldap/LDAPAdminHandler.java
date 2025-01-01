/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.ldap;


import org.ramadda.repository.*;
import org.ramadda.repository.admin.*;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;

import ucar.unidata.xml.XmlUtil;

import java.util.List;


/**
 * And example class to add ldap configuration options to the admin screen
 *
 */
public class LDAPAdminHandler extends AdminHandlerImpl {

    /** _more_ */
    private int version = 0;

    /** property id */
    public static final String LDAP_HANDLER_ID = "ldaphandler";


    /** property id */
    public static final String PROP_LDAP_URL = "ldap.url";

    /** property id */
    public static final String PROP_LDAP_USER_DIRECTORY =
        "ldap.user.directory";

    /** property id */
    public static final String PROP_LDAP_GROUP_DIRECTORY =
        "ldap.group.directory";

    /** property id */
    public static final String PROP_LDAP_ADMIN = "ldap.admin";

    /** property id */
    public static final String PROP_LDAP_PASSWORD = "ldap.password";

    /** property label */
    public static final String LABEL_LDAP_USER_DIRECTORY = "User Directory";

    /** property label */
    public static final String LABEL_LDAP_GROUP_DIRECTORY = "Group Directory";

    /** property label */
    public static final String LABEL_LDAP_ADMIN = "Admin ID";

    /** property label */
    public static final String LABEL_LDAP_PASSWORD = "Password";


    /** list of property ids */
    public static final String[] PROPERTY_IDS = { PROP_LDAP_URL,
            PROP_LDAP_USER_DIRECTORY, PROP_LDAP_GROUP_DIRECTORY,
            PROP_LDAP_ADMIN, PROP_LDAP_PASSWORD, };

    /** list of property labels */
    public static final String[] PROPERTY_LABELS = { "LDAP URL",
            LABEL_LDAP_USER_DIRECTORY, LABEL_LDAP_GROUP_DIRECTORY,
            LABEL_LDAP_ADMIN, LABEL_LDAP_PASSWORD, };

    /**
     * ctor.
     *
     * @param repository _more_
     */
    public LDAPAdminHandler(Repository repository) {
        super(repository);
    }


    /**
     * helper method to find the ldap admin instance
     *
     * @param repository the repository
     *
     * @return this object
     */
    public static LDAPAdminHandler getLDAPHandler(Repository repository) {
        return (LDAPAdminHandler) repository.getAdmin().getAdminHandler(
            LDAP_HANDLER_ID);
    }

    /**
     * Used to uniquely identify this admin handler
     *
     * @return unique id for this admin handler
     */
    public String getId() {
        return LDAP_HANDLER_ID;
    }

    /**
     * This adds the fields into the admin Settings->Access form section
     *
     * @param blockId which section
     * @param sb form buffer to append to
     */
    @Override
    public void addToAdminSettingsForm(String blockId, StringBuffer sb) {
        //For now don't do this
        if (true) {
            return;
        }
        //Are we in the access section
        if ( !blockId.equals(Admin.BLOCK_ACCESS)) {
            return;
        }
        sb.append(
            HtmlUtils.row(
                HtmlUtils.colspan(msgHeader("LDAP Configuration"), 2)));
        for (int i = 0; i < PROPERTY_IDS.length; i++) {
            String prop  = PROPERTY_IDS[i];
            String value = getRepository().getProperty(PROPERTY_IDS[i], "");
            //If its the password then we store it obfuscated in the db so its not just plain text
            boolean isPassword = isPassword(prop);
            if ((value != null) && isPassword) {
                value = deobfuscate(value);
            }
            if (isPassword) {
                sb.append(HtmlUtils.formEntry(msgLabel(PROPERTY_LABELS[i]),
                        HtmlUtils.password(prop, value, HtmlUtils.SIZE_40)));
            } else {
                sb.append(HtmlUtils.formEntry(msgLabel(PROPERTY_LABELS[i]),
                        HtmlUtils.input(prop, value, HtmlUtils.SIZE_40)));
            }
        }
    }


    /**
     * is this property  id the password
     *
     * @param prop property if
     *
     * @return is password
     */
    private boolean isPassword(String prop) {
        return prop.equals(PROP_LDAP_PASSWORD);
    }

    /**
     * Returns a integer timestamp to indicate whether anything has changed
     * since the last access to the server info
     *
     * @return _more_
     */
    public int getVersion() {
        return version;
    }

    /**
     * apply the form submit
     *
     * @param request the request
     *
     * @throws Exception On badness
     */
    @Override
    public void applyAdminSettingsForm(Request request) throws Exception {
        //For now don't do this
        if (true) {
            return;
        }
        version++;
        for (int i = 0; i < PROPERTY_IDS.length; i++) {
            String prop  = PROPERTY_IDS[i];
            String value = request.getString(PROPERTY_IDS[i], "");
            if (isPassword(prop)) {
                //If its the password then we store it obfuscated in the db so its not just plain text
                value = obfuscate(value);
            }
            getRepository().writeGlobal(PROPERTY_IDS[i], value);
        }
    }

    /**
     * get the server
     *
     * @return the server
     */
    public String getLdapUrl() {
        return getRepository().getProperty(PROP_LDAP_URL, (String) null);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getUserDirectory() {
        return getRepository().getProperty(PROP_LDAP_USER_DIRECTORY,
                                           (String) null);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getGroupDirectory() {
        return getRepository().getProperty(PROP_LDAP_GROUP_DIRECTORY,
                                           (String) null);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getAdminID() {
        return getRepository().getProperty(PROP_LDAP_ADMIN, (String) null);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getPassword() {
        //If its the password then we store it obfuscated in the db so its not just plain text
        return deobfuscate(getRepository().getProperty(PROP_LDAP_PASSWORD,
                (String) null));
    }

    /**
     * helper method to obfuscate password in db
     *
     * @param value password value
     *
     * @return obfuscated value
     */
    private String obfuscate(String value) {
        if (value == null) {
            return null;
        }
        if (value.length() == 0) {
            return value;
        }

        return new String(Utils.encodeBase64(value));
    }

    /**
     * helper method to deobfuscate password from db
     *
     * @param value _more_
     *
     * @return unobfuscated value
     */

    private String deobfuscate(String value) {
        if (value == null) {
            return null;
        }
        if (value.length() == 0) {
            return value;
        }

        return new String(Utils.decodeBase64(value));
    }



}
