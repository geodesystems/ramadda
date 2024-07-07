/**
Copyright (c) 2008-2024 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.auth;


import java.util.ArrayList;
import java.util.List;


public class Permission {

    public static final String ACTION_VIEW = "view";
    public static final String ACTION_VIEWCHILDREN = "viewchildren";
    public static final String ACTION_GEO = "geo";
    public static final String ACTION_EDIT = "edit";
    public static final String ACTION_NEW = "new";
    public static final String ACTION_UPLOAD = "upload";
    public static final String ACTION_DELETE = "delete";
    public static final String ACTION_COMMENT = "comment";
    public static final String ACTION_FILE = "file";
    public static final String ACTION_EXPORT = "export";
    public static final String ACTION_BLANK = "blank";
    public static final String ACTION_TYPE1 = "type1";
    public static final String ACTION_TYPE2 = "type2";

    public static final String[] ACTIONS = {
        ACTION_VIEW, ACTION_FILE, ACTION_EXPORT,
	ACTION_GEO,
	ACTION_EDIT, ACTION_NEW,
        ACTION_DELETE, ACTION_UPLOAD, 
        //ACTION_COMMENT,
	ACTION_VIEWCHILDREN, 
	//ACTION_TYPE1, ACTION_TYPE2
    };


    public static final String[] ACTION_NAMES = {
        "View", "File", "Export",
	"Geographic",
	"Edit", "New", "Delete", "Anon. Upload",
        //"Comment",
        "View Children", 
	//"Type specific 1", "Type specific 2"
    };


    private String dataPolicyId;
    private String action;
    private List<Role> roles;

    public Permission(String action, Role role) {
        this.action = action;
        roles       = new ArrayList<Role>();
        roles.add(role);
    }

    public Permission(String action, List<Role> roles) {
        this.action = action;
        this.roles  = roles;
    }

    public Permission(String dataPolicyId, String action, List<Role> roles) {
        this(action, roles);
        this.dataPolicyId = dataPolicyId;
    }



    
    public static boolean isValidActions(List actions) {
        for (int i = 0; i < actions.size(); i++) {
            if ( !isValidAction((String) actions.get(i))) {
                return false;
            }
        }

        return true;
    }


    
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

    
    public String toString() {
        return "action:" + action + " roles:" + roles + " data policy:"
               + dataPolicyId;
    }


    
    public void setDataPolicyId(String value) {
        dataPolicyId = value;
    }

    
    public String getDataPolicyId() {
        return dataPolicyId;
    }



    
    public void setAction(String value) {
        action = value;
    }

    
    public String getAction() {
        return action;
    }

    
    public String getLabel() {
        return action + " " + roles;
    }

    
    public boolean isAction(String action) {
        return this.action.equals(action);
    }

    
    public List<Role> getRoles() {
        return roles;
    }

}
