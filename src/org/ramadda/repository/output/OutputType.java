/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.output;

import org.ramadda.repository.*;

import ucar.unidata.util.Counter;
import ucar.unidata.util.Misc;

import java.util.List;

public class OutputType {

    public static final int TYPE_NONE =0;

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

    /** The extra categorized menu */
    public static final int TYPE_CHILDREN = 1 << 5;

    public static final int TYPE_CATEGORY = TYPE_OTHER;

    public static final int TYPE_CONNECT = TYPE_FEEDS;

    /** for the  toolbar */
    public static final int TYPE_TOOLBAR = 1 << 6;

    /** A general action. Shows up in the action list */
    public static final int TYPE_ACTION = 1 << 7;

    /** for internal uses */
    public static final int TYPE_INTERNAL = 1 << 8;

    /** Shows up in the search result format list */
    public static final int TYPE_FORSEARCH = 1 << 9;

    public static final int TYPE_IMPORTANT = 1 << 10;

    public static final int TYPE_SERVICE = 1 << 11;    

    /** All types */
    public static final int TYPE_ALL = TYPE_VIEW | TYPE_ACTION | TYPE_FEEDS
                                       | TYPE_FILE | TYPE_EDIT | TYPE_TOOLBAR
                                       | TYPE_OTHER | TYPE_IMPORTANT;

    /** types for the entry menu */
    public static final int TYPE_MENU = TYPE_VIEW | TYPE_FILE | TYPE_EDIT
                                        | TYPE_OTHER | TYPE_CHILDREN;

    public static String ICON_NULL = null;

    public static String SUFFIX_NONE = "";

    private String suffix = SUFFIX_NONE;

    private String id;

    private String label;

    private boolean forUser = true;

    private String groupName = "";

    private String category = "";

    private String icon;

    private int type = TYPE_VIEW;

    private Counter numberOfCalls = new Counter();

    private boolean okToUse = true;

    public OutputType(String id, int type) {
        this(id, id, type);
    }

    public OutputType(String label, String id, int type) {
        this(label, id, type, SUFFIX_NONE, ICON_NULL);
    }

    public OutputType(String label, String id, int type, String suffix,
                      String icon) {
        this(label, id, type, suffix, icon, null);
    }

    public OutputType(String label, String id, int type, String suffix,
                      String icon, String category) {
        this.label    = label;
        this.id       = id;
        this.type     = type;
        this.suffix   = suffix;
        this.icon     = icon;
        this.category = category;
    }

    public OutputType(OutputType that) {
        this.icon   = that.icon;
        this.label  = that.label;
        this.id     = that.id;
        this.suffix = that.suffix;
        this.type   = that.type;
    }

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
            } else if (menu.equals(PageStyle.MENU_SERVICE)) {
                type |= TYPE_SERVICE;		
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

    public String getIcon() {
        return icon;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

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

    public String getCategory() {
        return category;
    }

    public int getType() {
        return type;
    }

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

    public boolean getIsAction() {
        return isType(TYPE_ACTION);
    }

    public boolean getIsNonHtml() {
        return isType(TYPE_FEEDS);
    }

    public boolean getIsInternal() {
        return isType(TYPE_INTERNAL);
    }

    public boolean getIsFile() {
        return isType(TYPE_FILE);
    }

    public boolean getIsEdit() {
        return isType(TYPE_EDIT);
    }

    public int getNumberOfCalls() {
        return numberOfCalls.getCount();
    }

    public void incrNumberOfCalls() {
        numberOfCalls.incr();
    }

}
