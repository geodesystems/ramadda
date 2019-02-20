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

package org.ramadda.plugins.incident;


import org.ramadda.repository.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.GeoUtils;


import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Place;
import org.ramadda.util.Utils;


import org.w3c.dom.*;

import ucar.unidata.util.StringUtil;

import java.util.Date;
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
            name = entry.getValue(0, "");
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
     *
     * @throws Exception _more_
     */
    public void initializeEntryFromXml(Request request, Entry entry,
                                       Element node)
            throws Exception {
        initializeEntryFromXml(request, entry, node);
        georeferenceEntry(request, entry);
    }


    /**
     *
     * @param request _more_
     * @param entry _more_
     */
    private void georeferenceEntry(Request request, Entry entry) {
        if (entry.isGeoreferenced()) {
            return;
        }
        //TODO: if the entry has a location then don't do this?
        String address = entry.getValue(0, (String) null);
        String city    = entry.getValue(1, (String) null);
        String state   = entry.getValue(2, (String) null);
        if ( !Utils.stringDefined(address)) {
            return;
        }
        String fullAddress = address + "," + city + "," + state;
        Place  place       = GeoUtils.getLocationFromAddress(fullAddress);
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
        double depth = entry.getValue(4, 0.0);
        if (depth == 0) {
            return getIconUrl("/incident/flag_green.png");
        }
        if (depth <= 2) {
            return getIconUrl("/incident/flag_blue.png");
        }

        return getIconUrl("/incident/flag_red.png");
    }



}
