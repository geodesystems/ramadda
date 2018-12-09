/*
* Copyright (c) 2008-2018 Geode Systems LLC
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

package org.ramadda.plugins.map;


import org.json.*;

import org.ramadda.repository.Entry;
import org.ramadda.repository.Repository;
import org.ramadda.repository.Request;
import org.ramadda.repository.map.MapInfo;
import org.ramadda.repository.output.KmlOutputHandler;

import org.ramadda.repository.output.WikiConstants;
import org.ramadda.repository.type.GenericTypeHandler;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Json;
import org.ramadda.util.Utils;


import org.w3c.dom.Element;

import ucar.unidata.util.IOUtil;


import java.awt.geom.Rectangle2D;

import java.util.ArrayList;
import java.util.List;


/**
 */
public class GeoJsonTypeHandler extends GenericTypeHandler implements WikiConstants {

    /**
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     * @throws Exception _more_
     */
    public GeoJsonTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
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
    public void initializeEntryFromForm(Request request, Entry entry,
                                        Entry parent, boolean newEntry)
            throws Exception {
        if ( !newEntry) {
            return;
        }
        if ( !entry.isFile()) {
            return;
        }

        String json = IOUtil.readContents(entry.getResource().toString(),
                                          getClass());
        JSONObject  root     = new JSONObject(new JSONTokener(json));
        JSONArray   features = Json.readArray(root, "features");
        Rectangle2D bounds   = null;
        for (int i = 0; i < features.length(); i++) {
            JSONObject feature  = features.getJSONObject(i);
            JSONObject geometry = Json.readObject(feature, "geometry");
            String     type     = geometry.getString("type");
            JSONArray  coords   = Json.readArray(geometry, "coordinates");
            if (type.equals("Polygon")) {
                for (int j = 0; j < coords.length(); j++) {
                    JSONArray coords2 = coords.getJSONArray(j);
                    for (int k = 0; k < coords2.length(); k++) {
                        JSONArray coords3 = coords2.getJSONArray(k);
                        double    lon     = coords3.getDouble(0);
                        double    lat     = coords3.getDouble(1);
                        if (bounds == null) {
                            bounds = new Rectangle2D.Double(lon, lat, 0, 0);
                        } else {
                            bounds.add(lon, lat);
                        }
                    }
                }
            }
        }
        if (bounds != null) {
            entry.setNorth(bounds.getY() + bounds.getHeight());
            entry.setWest(bounds.getX());
            entry.setSouth(bounds.getY());
            entry.setEast(bounds.getX() + bounds.getWidth());
        }
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
    @Override
    public void initializeEntryFromXml(Request request, Entry entry,
                                       Element node)
            throws Exception {
        initializeEntryFromForm(request, entry, null, true);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param firstCall _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void initializeEntryFromHarvester(Request request, Entry entry,
                                             boolean firstCall)
            throws Exception {
        super.initializeEntryFromHarvester(request, entry, firstCall);
        if (firstCall) {
            initializeEntryFromForm(request, entry, null, true);
        }
    }


    /**
     *
     * @param request _more_
     * @param entry _more_
     * @param map _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public boolean addToMap(Request request, Entry entry, MapInfo map)
            throws Exception {
        if ( !entry.isFile()) {
            return true;
        }

        String url = getEntryManager().getEntryResourceUrl(request, entry);
        map.addGeoJsonUrl(entry.getName(), url, true,
                          ShapefileOutputHandler.makeMapStyle(request,
                              entry));

        return false;
    }




}
