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
    public static final String FIELD_CITATIONS = "citations";
    public static final String FIELD_LICENSES = "licenses";
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
    private List<String> citations;

    /**  */
    private List<String> licenses;

    /**  */
    private String licenseName;

    /**  */
    private List<Permission> permissions = new ArrayList<Permission>();

    private AccessManager accessManager;
	
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
    public DataPolicy(AccessManager accessManager, String mainUrl, String myUrl, String fromName,
                      JSONObject policy) {

	this.accessManager = accessManager;
        this.mainUrl  = mainUrl;
        this.myUrl    = myUrl;
        this.fromName = fromName;
        boolean debug = false;
        id          = policy.getString(FIELD_ID);
        description = policy.optString(FIELD_DESCRIPTION, null);
        name        = policy.optString(FIELD_NAME, Utils.makeLabel(id));
	citations=new ArrayList<String>();
	if(policy.has(FIELD_CITATIONS)) {
	    JSONArray jcitations = policy.getJSONArray(FIELD_CITATIONS);
	    for (int j = 0; j < jcitations.length(); j++) {
		citations.add(jcitations.getString(j));
	    }
	}
	licenses= new ArrayList<String>();
	if(policy.has(FIELD_LICENSES)) {
	    JSONArray jlicenses = policy.getJSONArray(FIELD_LICENSES);
	    for (int j = 0; j < jlicenses.length(); j++) {
		String     license = jlicenses.getString(j);
		licenses.add(license);
	    }
	}

        if (debug) {
            System.err.println("\tid:" + id + " licenses:" + licenses);
        }

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
     *  Get the Citation property.
     *
     *  @return The Citation
     */
    public List<String> getCitations() {
        return citations;
    }

    /**
     *  Get the License property.
     *
     *  @return The License
     */
    public List<String> getLicenses() {
        return licenses;
    }

    /**
     *  Get the License property.
     *
     *  @return The License
     */
    public String getLicenseName() {
        return licenseName;
    }



    /**
     *  @return _more_
     */
    public String getLabel() throws Exception {
        String label = getName();
	for(String license: licenses) {
	    String licenseName = accessManager.getMetadataManager().getLicenseName(license);
	    if(!Utils.stringDefined(licenseName)) licenseName = license;
	    label += " - " + licenseName;
        }

        return label;
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
