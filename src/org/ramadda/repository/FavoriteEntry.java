/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository;

import org.ramadda.repository.Entry;
import org.ramadda.util.HtmlUtils;

import org.w3c.dom.*;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import ucar.unidata.xml.XmlUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

/**
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
public class FavoriteEntry {

    /** _more_ */
    private String id;

    /** _more_ */
    private Entry entry;

    /** _more_ */
    private String name;

    /** _more_ */
    private String category;

    /**
     * _more_
     *
     * @param id _more_
     * @param entry _more_
     * @param name _more_
     * @param category _more_
     */
    public FavoriteEntry(String id, Entry entry, String name,
                         String category) {
        this.id       = id;
        this.entry    = entry;
        this.name     = name;
        this.category = category;
    }

    /**
     * _more_
     *
     * @param favorites _more_
     *
     * @return _more_
     */
    public static List<Entry> getEntries(List<FavoriteEntry> favorites) {
        List<Entry> entries = new ArrayList<Entry>();
        for (FavoriteEntry favorite : favorites) {
            entries.add(favorite.entry);
        }

        return entries;
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
     *  Set the Entry property.
     *
     *  @param value The new value for Entry
     */
    public void setEntry(Entry value) {
        entry = value;
    }

    /**
     *  Get the Entry property.
     *
     *  @return The Entry
     */
    public Entry getEntry() {
        return entry;
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
     *  Set the Category property.
     *
     *  @param value The new value for Category
     */
    public void setCategory(String value) {
        category = value;
    }

    /**
     *  Get the Category property.
     *
     *  @return The Category
     */
    public String getCategory() {
        return category;
    }

}
