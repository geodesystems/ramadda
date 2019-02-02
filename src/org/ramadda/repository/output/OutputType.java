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

package org.ramadda.repository.output;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.util.HtmlUtils;

import org.ramadda.util.sql.SqlUtil;


import org.w3c.dom.*;

import ucar.unidata.util.Counter;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;


import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;


import java.io.*;

import java.io.File;


import java.net.*;




import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;



import java.util.regex.*;

import java.util.zip.*;


/**
 *
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
public class OutputType {

    /** for the file menu */
    public static final int TYPE_FILE = 1 << 0;

    /** for the  edit menu */
    public static final int TYPE_EDIT = 1 << 1;

    /** for the  connect menu */
    public static final int TYPE_FEEDS = 1 << 2;

    /** for the  view menu */
    public static final int TYPE_VIEW = 1 << 3;

    /** The extra categorized menu */
    public static final int TYPE_OTHER = 1 << 4;

    /** _more_ */
    public static final int TYPE_CATEGORY = TYPE_OTHER;

    /** _more_ */
    public static final int TYPE_CONNECT = TYPE_FEEDS;

    /** for the  toolbar */
    public static final int TYPE_TOOLBAR = 1 << 5;

    /** A general action. Shows up in the action list */
    public static final int TYPE_ACTION = 1 << 6;

    /** for internal uses */
    public static final int TYPE_INTERNAL = 1 << 7;

    /** Shows up in the search result format list */
    public static final int TYPE_FORSEARCH = 1 << 8;

    /** _more_ */
    public static final int TYPE_IMPORTANT = 1 << 9;

    /** All types */
    public static final int TYPE_ALL = TYPE_VIEW | TYPE_ACTION | TYPE_FEEDS
                                       | TYPE_FILE | TYPE_EDIT | TYPE_TOOLBAR
                                       | TYPE_OTHER | TYPE_IMPORTANT;


    /** _more_ */
    public static String ICON_NULL = null;

    /** _more_ */
    public static String SUFFIX_NONE = "";

    /** _more_ */
    private String suffix = SUFFIX_NONE;

    /** _more_ */
    private String id;

    /** _more_ */
    private String label;

    /** _more_ */
    private boolean forUser = true;

    /** _more_ */
    private String groupName = "";

    /** _more_ */
    private String category = "";



    /** _more_ */
    private String icon;

    /** _more_ */
    private int type = TYPE_VIEW;

    /** _more_ */
    private Counter numberOfCalls = new Counter();


    /** _more_ */
    private boolean okToUse = true;

    /**
     * _more_
     *
     * @param id _more_
     * @param type _more_
     */
    public OutputType(String id, int type) {
        this(id, id, type);
    }



    /**
     * _more_
     *
     * @param label _more_
     * @param id _more_
     * @param type _more_
     */
    public OutputType(String label, String id, int type) {
        this(label, id, type, SUFFIX_NONE, ICON_NULL);
    }


    /**
     * _more_
     *
     * @param label _more_
     * @param id _more_
     * @param type _more_
     * @param suffix _more_
     * @param icon _more_
     */
    public OutputType(String label, String id, int type, String suffix,
                      String icon) {
        this(label, id, type, suffix, icon, null);
    }

    /**
     * _more_
     *
     * @param label _more_
     * @param id _more_
     * @param type _more_
     * @param suffix _more_
     * @param icon _more_
     * @param category _more_
     */
    public OutputType(String label, String id, int type, String suffix,
                      String icon, String category) {
        this.label    = label;
        this.id       = id;
        this.type     = type;
        this.suffix   = suffix;
        this.icon     = icon;
        this.category = category;
    }




    /**
     * _more_
     *
     * @param that _more_
     */
    public OutputType(OutputType that) {
        this.icon   = that.icon;
        this.label  = that.label;
        this.id     = that.id;
        this.suffix = that.suffix;
        this.type   = that.type;
    }

    /**
     * _more_
     *
     * @param that _more_
     * @param suffix _more_
     */
    public OutputType(OutputType that, String suffix) {
        this(that);
        this.suffix = suffix;
    }

    /**
     *  Set the OkToUse property.
     *
     *  @param value The new value for OkToUse
     */
    public void setOkToUse(boolean value) {
        okToUse = value;
    }

    /**
     *  Get the OkToUse property.
     *
     *  @return The OkToUse
     */
    public boolean getOkToUse() {
        return okToUse;
    }



    /**
     * _more_
     *
     * @param menuIds _more_
     *
     * @return _more_
     */
    public static int getTypeMask(List<String> menuIds) {
        if (menuIds.size() == 0) {
            return TYPE_ALL;
        }
        int type = 0;
        for (String menu : menuIds) {
            if (menu.equals(PageStyle.MENU_FILE)) {
                type |= TYPE_FILE;
            } else if (menu.equals(PageStyle.MENU_EDIT)) {
                type |= TYPE_EDIT;
            } else if (menu.equals(PageStyle.MENU_IMPORTANT)) {
                type |= TYPE_IMPORTANT;
            } else if (menu.equals(PageStyle.MENU_VIEW)) {
                type |= TYPE_VIEW;
            } else if (menu.equals(PageStyle.MENU_FEEDS)) {
                type |= TYPE_FEEDS;
            } else if (menu.equals(PageStyle.MENU_OTHER)) {
                type |= TYPE_OTHER;
            }
        }

        return type;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getIcon() {
        return icon;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getId() {
        return id;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getLabel() {
        return label;
    }

    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     */
    public String assembleUrl(Request request) {
        return request.getRequestPath() + getSuffix() + "?"
               + request.getUrlArgs();
    }


    /**
     * Set the Suffix property.
     *
     * @param value The new value for Suffix
     */
    public void setSuffix(String value) {
        suffix = value;
    }

    /**
     * Get the Suffix property.
     *
     * @return The Suffix
     */
    public String getSuffix() {
        return suffix;
    }


    /**
     * String representation of this object.
     * @return toString() method of label.
     */
    public String toString() {
        return id;
    }


    /**
     * _more_
     *
     * @param other _more_
     *
     * @return _more_
     */
    public boolean equals(Object other) {
        if ( !(other instanceof OutputType)) {
            return false;
        }
        OutputType that = (OutputType) other;

        return Misc.equals(id, that.id);
    }

    /**
     *  Set the ForUser property.
     *
     *  @param value The new value for ForUser
     */
    public void setForUser(boolean value) {
        forUser = value;
    }

    /**
     *  Get the ForUser property.
     *
     *  @return The ForUser
     */
    public boolean getForUser() {
        return forUser;
    }


    /**
     * Set the GroupName property.
     *
     * @param value The new value for GroupName
     */
    public void setGroupName(String value) {
        groupName = value;
    }

    /**
     * Get the GroupName property.
     *
     * @return The GroupName
     */
    public String getGroupName() {
        return groupName;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getCategory() {
        return category;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public int getType() {
        return type;
    }

    /**
     * _more_
     *
     * @param flag _more_
     *
     * @return _more_
     */
    public boolean isType(int flag) {
        return (flag & type) != 0;
    }

    /**
     *  Get the IsHtml property.
     *
     *  @return The IsHtml
     */
    public boolean getIsView() {
        return isType(TYPE_VIEW);
    }


    /**
     *
     *  @return The IsHtml
     */
    public boolean getIsForSearch() {
        return isType(TYPE_FORSEARCH);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getIsAction() {
        return isType(TYPE_ACTION);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getIsNonHtml() {
        return isType(TYPE_FEEDS);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getIsInternal() {
        return isType(TYPE_INTERNAL);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getIsFile() {
        return isType(TYPE_FILE);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getIsEdit() {
        return isType(TYPE_EDIT);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getNumberOfCalls() {
        return numberOfCalls.getCount();
    }

    /**
     * _more_
     */
    public void incrNumberOfCalls() {
        numberOfCalls.incr();
    }




}
