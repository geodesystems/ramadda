/**
Copyright (c) 2008-2021 Geode Systems LLC
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
public class AwcMetarTypeHandler extends PointTypeHandler {

    /** _more_ */
    public static final String URL =
        "https://www.aviationweather.gov/adds/dataserver_current/httpparam?dataSource=metars&requestType=retrieve&format=csv&stationString={station}&hoursBeforeNow={offset}";


    /** _more_ */
    private static int IDX =
        org.ramadda.data.services.RecordTypeHandler.IDX_LAST + 1;


    /** _more_ */
    public static final int IDX_SITE_ID = IDX++;

    /** _more_ */
    public static final int IDX_TIME_OFFSET = IDX++;

    /**
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     *
     * @throws Exception On badnes
     */
    public AwcMetarTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }

    private Hashtable<String,JSONObject> stations;


    private JSONObject getStation(String id) throws Exception {
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

    public void initializeNewEntry(Request request, Entry entry,                                    
                                   boolean fromImport)                                              
            throws Exception {                                                                      
        super.initializeNewEntry(request, entry, fromImport);                                       
        if (fromImport) {                                                                           
            return;                                                                                 
        }
        String id = (String) entry.getStringValue(IDX_SITE_ID, "");
	if(!Utils.stringDefined(id)) return;
	JSONObject station = getStation(id);
	if(station==null) return;
	if(!Utils.stringDefined(entry.getName())) entry.setName(station.getString("name"));
    	entry.setLatitude(station.getDouble("lat"));
    	entry.setLongitude(station.getDouble("lon"));	
   }




    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception On badnes
     */
    @Override
    public String getPathForEntry(Request request, Entry entry, boolean forRead)
            throws Exception {
        if (entry.isFile()) {
            return super.getPathForEntry(request, entry,forRead);
        }
        String siteId = entry.getStringValue(IDX_SITE_ID, "");
        int    offset = (int) entry.getIntValue(IDX_TIME_OFFSET, 24);
        String url = URL.replace("{station}", siteId).replace("{offset}",
                                 "" + offset);

        return url;
    }

}
