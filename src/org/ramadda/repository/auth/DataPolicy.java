/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.auth;


import org.json.*;

import org.ramadda.repository.*;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.Utils;

import java.util.ArrayList;
import java.util.List;


/**
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
@SuppressWarnings("unchecked")
public class DataPolicy {

    public static final String FIELD_ID = "id";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_CITATION = "citation";
    public static final String FIELD_LICENSE = "license";
    public static final String FIELD_LICENSE_DESCRIPTION = "license_description";
    public static final String FIELD_PERMISSIONS = "permissions";
    public static final String FIELD_URL="url";
    public static final String FIELD_ACTION = "action";
    public static final String FIELD_ROLES = "roles";    



    /**  */
    private String mainUrl;

    /**  */
    private String myUrl;

    /**  */
    private String fromName;

    /**  */
    private String id;

    /**  */
    private String description;

    /**  */
    private String name;

    /**  */
    private String citation;

    /**  */
    private String license;

    /**  */
    private String licenseDescription;

    /**  */
    private List<Permission> permissions = new ArrayList<Permission>();

    /**
     *
     *
     * @param url _more_
     *
     * @param mainUrl _more_
     * @param myUrl _more_
     * @param fromName _more_
     * @param policy _more_
     */
    public DataPolicy(String mainUrl, String myUrl, String fromName,
                      JSONObject policy) {
        this.mainUrl  = mainUrl;
        this.myUrl    = myUrl;
        this.fromName = fromName;
        boolean debug = false;
        id          = policy.getString(FIELD_ID);
        description = policy.optString(FIELD_DESCRIPTION, null);
        name        = policy.optString(FIELD_NAME, Utils.makeLabel(id));
        citation    = policy.optString(FIELD_CITATION, null);
        license     = policy.optString(FIELD_LICENSE, null);
        if (debug) {
            System.err.println("\tid:" + id + " license:" + license);
        }
        licenseDescription = policy.optString(FIELD_LICENSE_DESCRIPTION, null);
        JSONArray jpermissions = policy.getJSONArray(FIELD_PERMISSIONS);
        for (int j = 0; j < jpermissions.length(); j++) {
            JSONObject jpermission = jpermissions.getJSONObject(j);
            String     action = jpermission.getString(FIELD_ACTION);
            if ( !action.equals(Permission.ACTION_VIEW)
                    && !action.equals(Permission.ACTION_FILE)) {
                System.err.println("data policy with bad action:" + mainUrl
                                   + " action:" + action);
                continue;
            }


            if (debug) {
                System.err.println("\t\taction:" + action);
            }
            JSONArray  jroles = jpermission.getJSONArray(FIELD_ROLES);
            List<Role> roles  = new ArrayList<Role>();
            for (int k = 0; k < jroles.length(); k++) {
                String role = jroles.getString(k);
                roles.add(new Role(role));
                if (debug) {
                    System.err.println("\t\t\trole:" + role);
                }
            }
            permissions.add(new Permission(getId(), action, roles));
        }
    }

    /**
     *  @return _more_
     */
    public String toString() {
        return id + ":" + name;
    }


    /**
     *  Set the Id property.
     *
     *  @param value The new value for Id
     */
    public void setId(String value) {
        id = value;
    }

    /**
     *  Get the Id property.
     *
     *  @return The Id
     */
    public String getId() {
        return id;
    }

    /**
     *  Set the Name property.
     *
     *  @param value The new value for Name
     */
    public void setName(String value) {
        name = value;
    }

    /**
     *  Get the Name property.
     *
     *  @return The Name
     */
    public String getName() {
        return name;
    }


    /**
     *  Set the Citation property.
     *
     *  @param value The new value for Citation
     */
    public void setCitation(String value) {
        citation = value;
    }

    /**
     *  Get the Citation property.
     *
     *  @return The Citation
     */
    public String getCitation() {
        return citation;
    }

    /**
     *  Set the License property.
     *
     *  @param value The new value for License
     */
    public void setLicense(String value) {
        license = value;
    }

    /**
     *  Get the License property.
     *
     *  @return The License
     */
    public String getLicense() {
        return license;
    }

    /**
     *  @return _more_
     */
    public String getLabel() throws Exception {
        String label = getName();
        if (Utils.stringDefined(getLicense())) {
	    label += " - " + getLicense();
        } else if (Utils.stringDefined(getLicenseDescription())) {
            label += " - " + getLicenseDescription();
        }

        return label;
    }

    /**
     *  Set the LicenseDescription property.
     *
     *  @param value The new value for LicenseDescription
     */
    public void setLicenseDescription(String value) {
        licenseDescription = value;
    }

    /**
     *  Get the LicenseDescription property.
     *
     *  @return The LicenseDescription
     */
    public String getLicenseDescription() {
        return licenseDescription;
    }


    /**
     * Set the Permissions property.
     *
     * @param value The new value for Permissions
     */
    public void setPermissions(List<Permission> value) {
        permissions = value;
    }

    /**
     * Get the Permissions property.
     *
     * @return The Permissions
     */
    public List<Permission> getPermissions() {
        return permissions;
    }




    /**
     * Set the FromName property.
     *
     * @param value The new value for FromName
     */
    public void setFromName(String value) {
        fromName = value;
    }

    /**
     * Get the FromName property.
     *
     * @return The FromName
     */
    public String getFromName() {
        return fromName;
    }

    /**
     * Set the Description property.
     *
     * @param value The new value for Description
     */
    public void setDescription(String value) {
        description = value;
    }

    /**
     * Get the Description property.
     *
     * @return The Description
     */
    public String getDescription() {
        return description;
    }


    /**
     * Set the MainUrl property.
     *
     * @param value The new value for MainUrl
     */
    public void setMainUrl(String value) {
        mainUrl = value;
    }

    /**
     * Get the MainUrl property.
     *
     * @return The MainUrl
     */
    public String getMainUrl() {
        return mainUrl;
    }

    /**
     * Set the MyUrl property.
     *
     * @param value The new value for MyUrl
     */
    public void setMyUrl(String value) {
        myUrl = value;
    }

    /**
     * Get the MyUrl property.
     *
     * @return The MyUrl
     */
    public String getMyUrl() {
        return myUrl;
    }


}
