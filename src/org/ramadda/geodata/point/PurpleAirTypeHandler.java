/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.point;


import org.ramadda.repository.auth.Permission;
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

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;

import java.text.SimpleDateFormat;

import java.util.GregorianCalendar;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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

    public static final int IDX_SENSOR_ID = IDX++;
    public static final int IDX_PRIVATE_KEY = IDX++;
    public static final int IDX_ACTIVE = IDX++;    
    public static final int IDX_MODEL = IDX++;
    public static final int IDX_HARDWARE = IDX++;
    public static final int IDX_LOCATION_TYPE = IDX++;

    private static final String DATA_FIELDS  =  "humidity,temperature,pressure,voc,ozone1,pm1.0,pm2.5,pm10.0,0.3_um_count,0.5_um_count,1.0_um_count,2.5_um_count,5.0_um_count,10.0_um_count";


    private static final List<String> FIELDS_LIST =  Utils.split(DATA_FIELDS,",");

    private static final String FILE_HEADER  =  "date,"+  DATA_FIELDS+"\n";

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
			try {
			    runInBackground();
			} catch(Exception exc) {
			    getLogManager().logError("Error running purple air fetch loop",exc);
			}
		    }
		});
	}
    }

    private void runInBackground() throws Exception {
	Request searchRequest = getRepository().getAdminRequest();
	searchRequest.put(ARG_TYPE, "type_point_purpleair");
	Date now = new Date();
	GregorianCalendar cal = new GregorianCalendar();
	cal.setTime(now);
	int minute = cal.get(cal.MINUTE);
	int sec = cal.get(cal.SECOND);
	int diff = 60-minute;
	//Sleep until the next hour
	//	Misc.sleepSeconds(diff*60 - sec);
	Misc.sleepSeconds(10);
	while(true) {
	    StringBuilder tmp = new StringBuilder();
	    List<Entry> entries =
		getEntryManager().getEntries(searchRequest, tmp);
	    for(Entry entry: entries) {
		if(!entry.getValue(IDX_ACTIVE).toString().equals("true")) {
		    System.err.println("skipping:"+ entry);
		    continue;
		}
		try {
		    System.err.println("fetching:" + entry);
		    fetchData(entry);
		} catch(Exception exc) {
		    getLogManager().logError("Error fetching purple air data:" + entry,exc);
		}
	    }
	    //For now sleep for 60 minutes
	    Misc.sleepSeconds(60*60);
	}
    }


    private void fetchData(Entry entry) throws Exception {
	Sensor sensor = readSensor(entry, DATA_FIELDS);
	if(sensor==null) return;
	StringBuilder row=new StringBuilder(DateUtil.getTimeAsISO8601(sensor.date.getTime()));
	for(String field: FIELDS_LIST) {
	    double d =sensor.data.optDouble(field);
	    row.append(",");
	    row.append(d);
	}
	row.append("\n");
	File file = entry.getFile();
	FileOutputStream fos = new FileOutputStream(file, true);
	fos.write(row.toString().getBytes());
	fos.close();
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
	String id = (String) entry.getValue(IDX_SENSOR_ID,"");
	File newFile = getStorageManager().getTmpFile(request, id+"_purpleair.csv");	
	IOUtil.writeTo(new ByteArrayInputStream(FILE_HEADER.getBytes()), new FileOutputStream(newFile));
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
	Sensor sensor = readSensor(entry, "name,model,hardware,location_type,latitude,longitude,altitude");
	if(sensor==null) return;
	if(!Utils.stringDefined(entry.getName())) {
	    entry.setName(sensor.data.getString("name"));
	}
	entry.setLatitude(sensor.data.getDouble("latitude"));
	entry.setLongitude(sensor.data.getDouble("longitude"));
	entry.setAltitude(sensor.data.getDouble("altitude"));	        	    
	entry.setValue(IDX_MODEL, sensor.data.getString("model"));
	entry.setValue(IDX_HARDWARE, sensor.data.getString("hardware")); 
	entry.setValue(IDX_LOCATION_TYPE, sensor.data.getInt("location_type")==0?"outside":"inside");
    }


    private static class Sensor {
	Date date;
	JSONObject data;
	public Sensor(Date date, JSONObject data) {
	    this.date = date;
	    this.data = data;
	}
    }


    private Sensor readSensor(Entry entry, String fields)
	throws Exception {
	if(apiKey==null) return null;
	String id = (String) entry.getValue(IDX_SENSOR_ID,"");
	if(!Utils.stringDefined(id)) return null;
	String privateKey = (String) entry.getValue(IDX_PRIVATE_KEY,"");
	String url = "https://api.purpleair.com/v1/sensors/" + id+"?" +
	    HU.arg("api_key",apiKey)+
	    "&"+HU.arg("fields",fields);
	if(Utils.stringDefined(privateKey)) url+="&"+HU.arg("read_key",  privateKey);
	try {
	    String json = IO.doGet(new URL(url));
	    //	    System.err.println("fields:" + fields);
	    //	    System.err.println(json);
	    JSONObject obj     = new JSONObject(json);
	    long dataTimeStamp = obj.getLong("data_time_stamp");
	    return new Sensor(new Date(1000*dataTimeStamp), obj.getJSONObject("sensor"));
	} catch(Exception exc) {
	    getLogManager().logError("Error reading purpleair URL:" + url,exc);
	    throw new RuntimeException("Error accessing purpleair API for site:"+ id);
	}
    }
    

    public Result processEntryAction(Request request, Entry entry)
            throws Exception {
	String action = request.getString("action","");
	if(!action.equals("clearfile")) return super.processEntryAction(request, entry);
	boolean canEdit = getAccessManager().canDoAction(request, entry,
							 Permission.ACTION_EDIT);
	StringBuilder sb = new StringBuilder();
	getPageHandler().entrySectionOpen(request,entry, sb,"");
	if(!canEdit) {
	    sb.append(getPageHandler().showDialogError("You don't have permission to clear the file"));
	} else 	if(request.get("confirm", false)) {
	    File file = entry.getFile();
	    FileOutputStream fos = new FileOutputStream(file);
	    fos.write(FILE_HEADER.getBytes());
	    fos.close();
	    sb.append(getPageHandler().showDialogNote("OK, file has been cleared"));
 	} else {
	    sb.append(getPageHandler().showDialogQuestion("Are you sure you want to clear the file?",
							  HU.href(getEntryActionUrl(request, entry, "clearfile")+"&confirm=true","Yes, clear the file")));
	}

	getPageHandler().entrySectionClose(request,entry, sb);
	return getEntryManager().addEntryHeader(request, entry,
						new Result("Clear File",
							   sb));
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
            return visitInfo;
        }
    }


}
