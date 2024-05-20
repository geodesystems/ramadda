/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.point;

import org.ramadda.util.Utils;



import org.ramadda.data.services.*;
import org.ramadda.repository.Entry;
import org.ramadda.repository.Repository;
import org.ramadda.repository.Request;

import java.util.Hashtable;

import org.w3c.dom.Element;
import org.json.*;


/**
 * TypeHandler for Aviation Weather Center METARS
 * https://www.aviationweather.gov/adds/dataserver
 *
 *
 */
public class NwsStationTypeHandler extends PointTypeHandler {


    /**
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     *
     * @throws Exception On badnes
     */
    public NwsStationTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }

    private Hashtable<String,JSONObject> stations;


    public JSONObject getStation(String id) throws Exception {
	if(stations==null) {
	    JSONArray a= new JSONArray(getStorageManager().readUncheckedSystemResource("/org/ramadda/geodata/point/resources/weatherstations.json"));
	    Hashtable<String,JSONObject> tmp  =new Hashtable<String,JSONObject>();
	    for (int i = 0; i < a.length(); i++) {
		JSONObject station = a.getJSONObject(i);
		tmp.put(station.getString("id"),station);
	    }
	    stations = tmp;
	}
	return stations.get(id);
    }


    public void initializeNewEntry(Request request, Entry entry, String id)
            throws Exception {
	if(!Utils.stringDefined(id)) return;
	JSONObject station = getStation(id);
	if(station==null) return;
	if(!Utils.stringDefined(entry.getName()))
	    entry.setName(station.getString("name"));
    	entry.setLatitude(station.getDouble("lat"));
    	entry.setLongitude(station.getDouble("lon"));	
	entry.setValue("state",station.optString("st",""));
   }



}
