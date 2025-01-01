/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.incident;


import org.ramadda.repository.*;
import org.ramadda.repository.type.*;


import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;
import org.ramadda.util.geo.GeoUtils;
import org.ramadda.util.geo.Place;


import org.w3c.dom.*;

import ucar.unidata.util.StringUtil;

import java.io.File;

import java.util.Date;
import java.util.Hashtable;
import java.util.List;


/**
 *
 *
 */
public class LocationTypeHandler extends ExtensibleGroupTypeHandler {



    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public LocationTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }


    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    @Override
    public String getEntryName(Entry entry) {
        String name = super.getEntryName(entry);
        if ( !Utils.stringDefined(name)) {
            name = entry.getStringValue(getRepository().getAdminRequest(),0, "");
        }

        //        System.err.println("NAME:" + name);
        return name;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param parent _more_
     * @param newEntry _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void initializeEntryFromForm(Request request, Entry entry,
                                        Entry parent, boolean newEntry)
            throws Exception {
        super.initializeEntryFromForm(request, entry, parent, newEntry);
        georeferenceEntry(request, entry);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param node _more_
     * @param files _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void initializeEntryFromXml(Request request, Entry entry,
                                       Element node,
                                       Hashtable<String, File> files)
            throws Exception {
        initializeEntryFromXml(request, entry, node, files);
        georeferenceEntry(request, entry);
    }


    /**
     *
     * @param request _more_
     * @param entry _more_
     */
    private void georeferenceEntry(Request request, Entry entry) {
        if (entry.isGeoreferenced(request)) {
            return;
        }
        //TODO: if the entry has a location then don't do this?
        String address = entry.getStringValue(request,0, (String) null);
        String city    = entry.getStringValue(request,1, (String) null);
        String state   = entry.getStringValue(request,2, (String) null);
        if ( !Utils.stringDefined(address)) {
            return;
        }
        String fullAddress = address + "," + city + "," + state;
        Place  place       = GeoUtils.getLocationFromAddress(fullAddress,null);
        if (place == null) {
            System.err.println("no geo for address:" + fullAddress);
        } else {
            System.err.println("got geo for address:" + fullAddress);
            entry.setLatitude(place.getLatitude());
            entry.setLongitude(place.getLongitude());
        }
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public String getEntryIconUrl(Request request, Entry entry)
            throws Exception {
        double depth = entry.getDoubleValue(request,4, 0.0);
        if (depth == 0) {
            return getIconUrl("/incident/flag_green.png");
        }
        if (depth <= 2) {
            return getIconUrl("/incident/flag_blue.png");
        }

        return getIconUrl("/incident/flag_red.png");
    }



}
