/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.output;

import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;

import java.util.HashSet;

/**
 *
 *
 */
public class PageStyle {

    public static final String MENU_FILE = "file";

    public static final String MENU_EDIT = "edit";

    public static final String MENU_VIEW = "view";

    public static final String MENU_IMPORTANT = "important";

    public static final String MENU_FEEDS = "feeds";

    public static final String MENU_OTHER = "other";

    public static final String MENU_SERVICE = "service";

    private String wikiTemplate;

    private HashSet<String> menus = new HashSet<String>();

    private boolean showMenubar = true;

    private boolean showToolbar = true;

    private boolean showEntryHeader = true;

    private boolean showBreadcrumbs = true;

    private boolean showLayoutToolbar = true;

    public PageStyle() {}

    public void setMenu(String menu) {
        menus.add(menu);
    }

    public boolean okToShowMenu(Entry entry, String menu) {
        if (menus.size() == 0) {
            return true;
        }

        return menus.contains(menu);
    }

    /**
     *  Set the WikiTemplate property.
     *
     *  @param value The new value for WikiTemplate
     */
    public void setWikiTemplate(String value) {
        wikiTemplate = value;
    }

    /**
     *  Get the WikiTemplate property.
     *
     *
     * @param entry _more_
     *  @return The WikiTemplate
     */
    public String getWikiTemplate(Entry entry) {
        //If its a fake entry (e.g, from search results) then
        //don't use the wiki template
        if (entry.isDummy()) {
            return null;
        }

        return wikiTemplate;
    }

    /**
     *  Set the ShowMenubar property.
     *
     *  @param value The new value for ShowMenubar
     */
    public void setShowMenubar(boolean value) {
        showMenubar = value;
    }

    /**
     *  Get the ShowMenubar property.
     *
     *
     * @param entry _more_
     *  @return The ShowMenubar
     */
    public boolean getShowMenubar(Entry entry) {
        //        if(true) return false;
        return showMenubar;
    }

    /**
     *  Set the ShowToolbar property.
     *
     *  @param value The new value for ShowToolbar
     */
    public void setShowToolbar(boolean value) {
        showToolbar = value;
    }

    /**
     *  Get the ShowToolbar property.
     *
     *
     * @param entry _more_
     *  @return The ShowToolbar
     */
    public boolean getShowToolbar(Entry entry) {
        //        if(true) return false;
        return showToolbar;
    }

    /**
     *  Set the ShowLayoutToolbar property.
     *
     *  @param value The new value for ShowLayoutToolbar
     */
    public void setShowLayoutToolbar(boolean value) {
        showLayoutToolbar = value;
    }

    /**
     *  Get the ShowLayoutToolbar property.
     *
     *
     * @param entry _more_
     *  @return The ShowLayoutToolbar
     */
    public boolean getShowLayoutToolbar(Entry entry) {
        //        if(true) return false;
        return showLayoutToolbar;
    }

    /**
     *  Set the ShowEntryHeader property.
     *
     *  @param value The new value for ShowEntryHeader
     */
    public void setShowEntryHeader(boolean value) {
        showEntryHeader = value;
    }

    /**
     *  Get the ShowEntryHeader property.
     *
     *
     * @param entry _more_
     *  @return The ShowEntryHeader
     */
    public boolean getShowEntryHeader(Entry entry) {
        return showEntryHeader;
    }

    /**
     *  Set the ShowBreadcrumbs property.
     *
     *  @param value The new value for ShowBreadcrumbs
     */
    public void setShowBreadcrumbs(boolean value) {
        showBreadcrumbs = value;
    }

    /**
     *  Get the ShowBreadcrumbs property.
     *
     *
     * @param entry _more_
     *  @return The ShowBreadcrumbs
     */
    public boolean getShowBreadcrumbs(Entry entry) {
        //        if(true) return false;
        return showBreadcrumbs;
    }

}
