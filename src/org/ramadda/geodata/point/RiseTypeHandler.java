/**
Copyright (c) 2008-2024 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.point;
import org.json.*;
import org.ramadda.data.record.*;
import org.ramadda.data.services.PointTypeHandler;
import org.ramadda.repository.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.IO;
import org.ramadda.util.Utils;
import org.w3c.dom.*;
import java.io.*;
import java.net.URL;

public class RiseTypeHandler extends PointTypeHandler {
    private static int IDX = PointTypeHandler.IDX_LAST + 1;
    public static final int IDX_RISE_ID = IDX++;
    public static final int IDX_LOCATION_TYPE = IDX++;

    public RiseTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }

    @Override
    public void initializeNewEntry(Request request, Entry entry,NewType newType)
            throws Exception {
        super.initializeNewEntry(request, entry, newType);
	if(!isNew(newType)) return;
        String id = (String) entry.getStringValue(request,IDX_RISE_ID, "");
        if ( !Utils.stringDefined(id)) {
            return;
        }
	id =id.trim();
        String url  = "https://data.usbr.gov/rise/api/location/" + id;
        String json = null;
        try {
            json = IO.doGet(new URL(url), "accept",
                            "application/vnd.api+json");
            JSONObject obj   = new JSONObject(json);
            JSONObject data  = obj.getJSONObject("data");
            JSONObject attrs = data.getJSONObject("attributes");
            JSONArray coords = attrs.getJSONObject(
                                   "locationCoordinates").getJSONArray(
                                   "coordinates");
            double lon = coords.getDouble(0);
            double lat = coords.getDouble(1);
            entry.setLocation(lat, lon);
            entry.setAltitude(attrs.getDouble("elevation"));
            entry.setValue(IDX_LOCATION_TYPE,
                           attrs.getString("locationTypeName"));
            if (Utils.stringDefined(entry.getName())) {
                entry.setName(attrs.getString("locationName"));
            }
            if (attrs.has("locationRegionNames")) {
                JSONArray a = attrs.getJSONArray("locationRegionNames");
                for (int i = 0; i < a.length(); i++) {
                    getMetadataManager().addMetadataTag(request,entry,
                            "Region: " + a.getString(i));
                }
            }
            if (attrs.has("locationTags")) {
                JSONArray a = attrs.getJSONArray("locationTags");
                for (int i = 0; i < a.length(); i++) {
                    getMetadataManager().addMetadataTag(request,entry,
                            "Location: "
                            + a.getJSONObject(i).getString("tag"));
                }
            }
        } catch (Exception exc) {
            getLogManager().logError("Error reading RISE URL:" + url, exc);
            getLogManager().logError("JSON:" + json);
            throw new RuntimeException("Error accessing RISE API for site:"+ id);
        }
    }
}
