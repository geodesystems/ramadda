/*
* Copyright (c) 2008-2019 Geode Systems LLC
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


import org.ramadda.util.HtmlUtils;


import org.ramadda.util.sql.SqlUtil;

import ucar.unidata.util.DateUtil;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;


import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.io.File;
import java.io.InputStream;

import java.lang.reflect.*;



import java.net.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;



import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;



import java.util.regex.*;


/**
 *
 *
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

    /** _more_ */
    public static final String ACTION_TYPE1 = "type1";

    /** _more_ */
    public static final String ACTION_TYPE2 = "type2";


    /** _more_ */
    public static final String[] ACTIONS = {
        ACTION_VIEW, ACTION_VIEWCHILDREN, ACTION_FILE, ACTION_EXPORT,
        ACTION_EDIT, ACTION_NEW, ACTION_UPLOAD, ACTION_DELETE, ACTION_COMMENT,
        ACTION_TYPE1, ACTION_TYPE2
    };

    /** _more_ */
    public static final String[] ACTION_NAMES = {
        "View", "View Children", "File", "Export", "Edit", "New",
        "Anon. Upload", "Delete", "Comment", "Type specific 1",
        "Type specific 2"
    };

    /*
for phrase extraction
        msg("View")
        msg("View Children")
        msg("File")
        msg("Edit")
        msg("New")
        msg("Anon. Upload")
        msg("Delete")
        msg("Comment")
        msg("Type specific 1")
        msg("Type specific 2")
     */



    /** _more_ */
    String action;

    /** _more_ */
    List<String> roles;

    /**
     * _more_
     *
     * @param action _more_
     * @param role _more_
     */
    public Permission(String action, String role) {
        this.action = action;
        roles       = new ArrayList<String>();
        roles.add(role);
    }


    /**
     * _more_
     *
     * @param action _more_
     * @param roles _more_
     */
    public Permission(String action, List<String> roles) {
        this.action = action;
        this.roles  = roles;
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

        return false;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return "action:" + action + " roles:" + roles;
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
     * Set the Roles property.
     *
     * @param value The new value for Roles
     */
    public void setRoles(List<String> value) {
        roles = value;
    }

    /**
     * Get the Roles property.
     *
     * @return The Roles
     */
    public List<String> getRoles() {
        return roles;
    }

}
