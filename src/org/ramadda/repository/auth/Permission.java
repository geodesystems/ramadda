/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.auth;


import java.util.ArrayList;
import java.util.List;


/**
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
public class Permission {

    /** _more_ */
    public static final String ACTION_VIEW = "view";

    /** _more_ */
    public static final String ACTION_VIEWCHILDREN = "viewchildren";

    /** _more_ */
    public static final String ACTION_GEO = "geo";


    /** _more_ */
    public static final String ACTION_EDIT = "edit";

    /** _more_ */
    public static final String ACTION_NEW = "new";

    /** _more_ */
    public static final String ACTION_UPLOAD = "upload";

    /** _more_ */
    public static final String ACTION_DELETE = "delete";

    /** _more_ */
    public static final String ACTION_COMMENT = "comment";

    /** _more_ */
    public static final String ACTION_FILE = "file";

    /** _more_ */
    public static final String ACTION_EXPORT = "export";

    /**  */
    public static final String ACTION_BLANK = "blank";

    /** _more_ */
    public static final String ACTION_TYPE1 = "type1";

    /** _more_ */
    public static final String ACTION_TYPE2 = "type2";


    /** _more_ */
    public static final String[] ACTIONS = {
        ACTION_VIEW, ACTION_FILE, ACTION_EXPORT, ACTION_EDIT, ACTION_NEW,
        ACTION_DELETE, ACTION_UPLOAD, ACTION_GEO,
        //ACTION_COMMENT,
	ACTION_VIEWCHILDREN, 
	//	ACTION_TYPE1, ACTION_TYPE2
    };

    /** _more_ */
    public static final String[] ACTION_NAMES = {
        "View", "File", "Export", "Edit", "New", "Delete", "Anon. Upload","Geo Location",
        //      "Comment",
        "View Children", 
	//	"Type specific 1", "Type specific 2"
    };


    /**  */
    private String dataPolicyId;

    /** _more_ */
    private String action;

    /** _more_ */
    private List<Role> roles;


    /**
     * _more_
     *
     * @param action _more_
     * @param role _more_
     */
    public Permission(String action, Role role) {
        this.action = action;
        roles       = new ArrayList<Role>();
        roles.add(role);
    }


    /**
     * _more_
     *
     * @param action _more_
     * @param roles _more_
     */
    public Permission(String action, List<Role> roles) {
        this.action = action;
        this.roles  = roles;
    }


    /**
     *
     *
     * @param dataPolicy _more_
     *
     * @param dataPolicyId _more_
     * @param action _more_
     * @param roles _more_
     */
    public Permission(String dataPolicyId, String action, List<Role> roles) {
        this(action, roles);
        this.dataPolicyId = dataPolicyId;
    }



    /**
     * _more_
     *
     * @param actions _more_
     *
     * @return _more_
     */
    public static boolean isValidActions(List actions) {
        for (int i = 0; i < actions.size(); i++) {
            if ( !isValidAction((String) actions.get(i))) {
                return false;
            }
        }

        return true;
    }


    /**
     * _more_
     *
     * @param action _more_
     *
     * @return _more_
     */
    public static boolean isValidAction(String action) {
        for (int i = 0; i < ACTIONS.length; i++) {
            if (ACTIONS[i].equals(action)) {
                return true;
            }
        }
        //Since we comment out the "comment" action in the list of actions handle it here
        if (action.equals("comment")) {
            return true;
        }

        return false;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return "action:" + action + " roles:" + roles + " data policy:"
               + dataPolicyId;
    }


    /**
     * Set the DataPolicyId property.
     *
     * @param value The new value for DataPolicyId
     */
    public void setDataPolicyId(String value) {
        dataPolicyId = value;
    }

    /**
     * Get the DataPolicyId property.
     *
     * @return The DataPolicyId
     */
    public String getDataPolicyId() {
        return dataPolicyId;
    }



    /**
     * Set the Action property.
     *
     * @param value The new value for Action
     */
    public void setAction(String value) {
        action = value;
    }

    /**
     * Get the Action property.
     *
     * @return The Action
     */
    public String getAction() {
        return action;
    }

    /**
      * @return _more_
     */
    public String getLabel() {
        return action + " " + roles;
    }

    /**
     *
     * @param action _more_
     *  @return _more_
     */
    public boolean isAction(String action) {
        return this.action.equals(action);
    }

    /**
     * Get the Roles property.
     *
     * @return The Roles
     */
    public List<Role> getRoles() {
        return roles;
    }

}
