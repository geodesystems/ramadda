/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.point;


import org.ramadda.data.point.text.*;
import org.ramadda.data.record.*;
import org.ramadda.data.services.PointTypeHandler;
import org.ramadda.data.services.RecordTypeHandler;
import org.ramadda.repository.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.IO;
import org.ramadda.util.Json;
import org.ramadda.util.Utils;
import org.ramadda.util.text.CsvUtil;

import org.json.*;
import org.w3c.dom.*;

import java.io.*;
import java.net.URL;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.GregorianCalendar;

import java.util.Hashtable;


/**
 */
public class PurpleAirTypeHandler extends PointTypeHandler {

    private String apiKey;

    /** _more_ */
    private SimpleDateFormat dateSDF;

    /** _more_ */
    private static int IDX = PointTypeHandler.IDX_LAST + 1;

    public static final int SENSOR_ID_IDX = IDX++;
    public static final int PRIVATE_KEY_IDX = IDX++;
    public static final int MODEL_IDX = IDX++;
    public static final int HARDWARE_IDX = IDX++;
    public static final int LOCATION_TYPE_IDX = IDX++;

    private static final String DATA_FIELDS  =  "humidity,temperature,pressure,voc,ozone1,pm1.0,pm2.5,pm10.0,0.3_um_count,0.5_um_count,1.0_um_count,2.5_um_count,5.0_um_count,10.0_um_count";

    private static final String HEADER  =  "#date,humidity,temperature,pressure,voc,ozone1,pm1.0,pm2.5,pm10.0,0.3_um_count,0.5_um_count,1.0_um_count,2.5_um_count,5.0_um_count,10.0_um_count\n";

    /**
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     * @throws Exception _more_
     */
    public PurpleAirTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
	apiKey = repository.getProperty("purpleair.api.key",(String)null);
	if(apiKey!=null) {
	    Misc.run(new Runnable() {
		    public void run() {
			runInBackground();
		    }
		});
	}
    }

    private void runInBackground() {
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
	super.initializeNewEntry(request, entry,fromImport);
	if(fromImport) return;
	String id = (String) entry.getValue(SENSOR_ID_IDX,"");
	File newFile = getStorageManager().getTmpFile(request, id+"_purpleair.csv");	
	IOUtil.writeTo(new ByteArrayInputStream(HEADER.getBytes()), new FileOutputStream(newFile));
	newFile = getStorageManager().moveToStorage(request, newFile);
	entry.setResource(new Resource(newFile.toString(),Resource.TYPE_STOREDFILE));

	/*
	  {
	  "api_version" : "V1.0.10-0.0.12",
	  "time_stamp" : 1643779008,
	  "data_time_stamp" : 1643778984,
	  "sensor" : {
	  "sensor_index" : 72931,
	  "name" : "AZTEC CENTRAL",
	  "model" : "PA-II",
	  "hardware" : "2.0+BME280+PMSX003-B+PMSX003-A",
	  "location_type" : 0,
	  "latitude" : 39.997868,
	  "longitude" : -105.227875,
	  "altitude" : 5298
	  }
	  }
	*/
	JSONObject sensor = readSensor(entry, "name,model,hardware,location_type,latitude,longitude,altitude");
	if(sensor==null) return;
	if(!Utils.stringDefined(entry.getName())) {
	    entry.setName(sensor.getString("name"));
	}
	entry.setLatitude(sensor.getDouble("latitude"));
	entry.setLongitude(sensor.getDouble("longitude"));
	entry.setAltitude(sensor.getDouble("altitude"));	        	    
	entry.setValue(MODEL_IDX, sensor.getString("model"));
	entry.setValue(HARDWARE_IDX, sensor.getString("hardware")); 
	entry.setValue(LOCATION_TYPE_IDX, sensor.getInt("location_type")==0?"outside":"inside");
    }


    private JSONObject readSensor(Entry entry, String fields)
	throws Exception {
	if(apiKey==null) return null;
	String id = (String) entry.getValue(SENSOR_ID_IDX,"");
	if(!Utils.stringDefined(id)) return null;
	String privateKey = (String) entry.getValue(PRIVATE_KEY_IDX,"");
	String url = "https://api.purpleair.com/v1/sensors/" + id+"?" +
	    HU.arg("api_key",apiKey)+
	    "&"+HU.arg("fields",fields);
	if(Utils.stringDefined(privateKey)) url+="&"+HU.arg("read_key",  privateKey);
	try {
	    String json = IO.doGet(new URL(url));
	    JSONObject obj     = new JSONObject(json);
	    return  obj.getJSONObject("sensor");
	} catch(Exception exc) {
	    getLogManager().logError("Error reading purpleair URL:" + url,exc);
	    throw new RuntimeException("Error accessing purpleair API for site:"+ id);
	}
    }
    

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param properties _more_
     * @param requestProperties _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public RecordFile doMakeRecordFile(Request request, Entry entry,
                                       Hashtable properties,
                                       Hashtable requestProperties)
            throws Exception {
        return new PurpleAirRecordFile(getRepository(), entry,
                                    getPathForEntry(request, entry));
    }


    /**
     * Class description
     *
     *
     * @version        $version$, Sat, Dec 8, '18
     * @author         Enter your name here...
     */
    public static class PurpleAirRecordFile extends CsvFile {

        /** _more_ */
        Repository repository;

        /** _more_ */
        Entry entry;

        /**
         * _more_
         *
         *
         * @param repository _more_
         * @param entry _more_
         * @param filename _more_
         *
         * @throws IOException _more_
         */
        public PurpleAirRecordFile(Repository repository, Entry entry,
                                String filename)
                throws IOException {
            super(filename);
            this.repository = repository;
            this.entry      = entry;
        }

        /**
         * _more_
         *
         * @param visitInfo _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        public VisitInfo prepareToVisit(VisitInfo visitInfo)
                throws Exception {
            super.prepareToVisit(visitInfo);
            putFields(new String[] {
                makeField(FIELD_DATE, attrType("date"), attrFormat("yyyy-D")),
                makeField("day_length", attrType("double"), attrChartable(),
                          attrUnit("seconds"), attrLabel("Day Length")),
                makeField("precipitation", attrType("double"),
                          attrChartable(), attrUnit("inches"),
                          attrLabel("Precipitation")),
                makeField("srad", attrType("double"), attrChartable(),
                          attrUnit("W/m^2"),
                          attrLabel("Shortwave Radiation")),
                makeField("swe", attrType("double"), attrChartable(),
                          attrUnit("kg/m^2"),
                          attrLabel("Snow Water Equivalent")),
                makeField("tmax", attrType("double"), attrChartable(),
                          attrUnit("degrees C"),
                          attrLabel("Max Temperature")),
                makeField("tmin", attrType("double"), attrChartable(),
                          attrUnit("degrees C"),
                          attrLabel("Min Temperature")),
                makeField("vp", attrType("double"), attrChartable(),
                          attrUnit("Pa"), attrLabel("Pressure"))
            });

            return visitInfo;
        }
    }


}
