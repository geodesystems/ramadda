/**
   Copyright (c) 2008-2023 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.point;

import org.json.*;
import org.ramadda.data.services.PointTypeHandler;
import org.ramadda.repository.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.IO;
import org.ramadda.util.Utils;
import org.w3c.dom.*;
import java.io.*;
import java.net.URL;

/**
 */
public class SnotelTypeHandler extends PointTypeHandler {

    /**  */
    private static int IDX = PointTypeHandler.IDX_LAST + 1;


    private static int IDX_SITE_NUMBER = IDX++;
    private static int IDX_SITE_ID = IDX++;
    private static int IDX_STATE = IDX++;
    private static int IDX_COUNTY = IDX++;
    private static int IDX_NETWORK = IDX++;
    private static int IDX_HUC_NAME = IDX++;
    private static int IDX_HUC_ID = IDX++;



    /**
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     * @throws Exception _more_
     */
    public SnotelTypeHandler(Repository repository, Element node)
	throws Exception {
        super(repository, node);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param fromImport _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void initializeNewEntry(Request request, Entry entry,
                                   boolean fromImport)
	throws Exception {
        if (fromImport) {
            return;
        }
        String id = (String) entry.getStringValue(IDX_SITE_NUMBER, "");
        if ( !Utils.stringDefined(id)) {
	    super.initializeNewEntry(request, entry, fromImport);
            return;
        }


	id =id.trim();
	String url = "https://wcc.sc.egov.usda.gov/awdbRestApi/services/v1/stations?activeOnly=true&returnForecastPointMetadata=false&returnReservoirMetadata=false&returnStationElements=false&stationTriplets="+ id+"%3A*%3A*";

	//https://www.wcc.nrcs.usda.gov/siteimages/663.jpg

        String json = null;
        try {
            json = IO.doGet(new URL(url));
	    /*
	      [
	      {
	      "stationTriplet": "1137:NV:SNTL",
	      "stationId": "1137",
	      "stateCode": "NV",
	      "networkCode": "SNTL",
	      "name": "Vacarro Springs",
	      "dcoCode": "UT",
	      "countyName": "Eureka",
	      "huc": "160600051503",
	      "elevation": 7890,
	      "latitude": 39.4495,
	      "longitude": -115.9834,
	      "dataTimeZone": -8,
	      "shefId": "VCSN2",
	      "beginDate": "2011-10-01 00:00:00.0",
	      "endDate": "2100-01-01 00:00:00.0"
	      }
	      ]
	    */
	    //	    System.err.println(json);
            JSONArray  array   = new JSONArray(json);
            JSONObject obj  = array.getJSONObject(0);
            double lon = obj.getDouble("longitude");
            double lat = obj.getDouble("latitude");	    
            entry.setLocation(lat, lon,obj.getDouble("elevation"));
	    entry.setDescription("[https://wcc.sc.egov.usda.gov/nwcc/site?sitenum=" + id+" NRCS Site]");
	    //            entry.setValue(IDX_LOCATION_TYPE, attrs.getString("locationTypeName"));
            if (!Utils.stringDefined(entry.getName())) {
                entry.setName(obj.getString("name"));
            }
	    String state = obj.getString("stateCode");
	    entry.setValue(IDX_STATE,state);
	    entry.setValue(IDX_COUNTY,obj.getString("countyName"));
	    entry.setValue(IDX_HUC_ID,obj.getString("huc"));
	    String network = obj.getString("networkCode");
	    entry.setValue(IDX_NETWORK,network);
	    String siteId  =id+":"+ state+":" + network;
	    entry.setValue(IDX_SITE_ID,siteId);
	    String dataUrl = "http://www.wcc.nrcs.usda.gov/reportGenerator/view_csv/customSingleStationReport/daily/" + siteId+"|name/-14,0/WTEQ::value,WTEQ::delta,SNWD::value,SNWD::delta,PREC::value,TOBS::value,TMIN::value,TMAX::value";
	    entry.setResource(new Resource(dataUrl));
	    super.initializeNewEntry(request, entry, fromImport);
        } catch (Exception exc) {
            getLogManager().logError("Error reading SNOTEL URL:" + url, exc);
            getLogManager().logError("JSON:" + json);
            throw new RuntimeException("Error accessing SNOTEL API for site:"+ id);
        }
    }
}