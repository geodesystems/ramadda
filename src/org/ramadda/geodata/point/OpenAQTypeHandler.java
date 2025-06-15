/**
   Copyright (c) 2008-2025 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.point;

import org.ramadda.repository.*;
import org.ramadda.repository.type.*;

import org.ramadda.data.point.text.*;
import org.ramadda.data.record.*;
import org.ramadda.data.services.PointTypeHandler;
import org.ramadda.data.services.RecordTypeHandler;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;
import org.ramadda.util.Utils;
import org.ramadda.util.seesv.Seesv;

import org.json.*;
import org.w3c.dom.*;
import java.io.*;

import java.net.URL;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.Date;

import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;

public class OpenAQTypeHandler extends PointTypeHandler {

    private static int IDX = RecordTypeHandler.IDX_LAST + 1;
    private static int IDX_LOCATION_ID = IDX++;
    private static int IDX_LOCALITY = IDX++;
    private static int IDX_COUNTRY = IDX++;
    private static int IDX_PROVIDER = IDX++;    
    private static int IDX_SENSORS = IDX++;
    private static int IDX_SENSOR_INDEX = IDX++;        
    private static int IDX_STATIONARY = IDX++;    
    private static int IDX_HOURS_OFFSET = IDX++;
    private SimpleDateFormat dateSDF;

    private static final String[]
	SEESV_ARGS = new String[] {
	"-json", "results", "value,period.datetimeFrom.utc",
	"-set","value", "0","${name}",
	"-set","period_datetimefrom_utc", "0","date",
	//normalize the params
	"-sortby","date","up","date",
	"-addheader","${name}.label {${displayname}} ${name}.unit {${unit}} ${name}.type double date.format yyyy-MM-dd'T'HH:mm:ss",
	"-print"
    };
    
    public OpenAQTypeHandler(Repository repository, Element node)
	throws Exception {
        super(repository, node);
    }

    @Override
    public void initializeNewEntry(Request request, Entry entry,NewType newType)
	throws Exception {
        super.initializeNewEntry(request, entry, newType);
	if(!isNew(newType)) return;
	String id = (String)entry.getValue(request,IDX_LOCATION_ID);
	if(Utils.stringDefined(id)) {
	    initializeNewEntryInner(request, entry);
	}
	String  bulkFile = request.getUploadedFile(ARG_BULKUPLOAD,true);
	if(!stringDefined(bulkFile) || !new File(bulkFile).exists()) return;
	HashSet<String> seen = new HashSet<String>();
	List<Entry> entries = handleBulkUpload(request, entry.getParentEntry(),bulkFile,"location_id",seen,null,null);
	for(Entry newEntry: entries) {
	    initializeNewEntryInner(request,newEntry);
	}
	getEntryManager().insertEntriesIntoDatabase(request,  entries,true, true);	

    }

    private void initializeNewEntryInner(Request request, Entry entry) throws Exception {
	String id = (String)entry.getValue(request,IDX_LOCATION_ID);
	if(!Utils.stringDefined(id)) {
	    return;
	}
	String key = getRepository().getProperty("openaq.api.key",null);
	if(key==null) {
	    throw new IllegalStateException("No OpenAQ API key is defined");
	}

	String url = HU.url("https://api.openaq.org/v3/locations/" + id,"limit","100","page","1","offset","0");
	IO.Path path  =new IO.Path(url);
	path.setRequestArgs(new String[]{"X-API-Key",key});
	IO.Result result = IO.getHttpResult(path);
	if(result.getError()) {
	    getLogManager().logError("OpenAQTypeHandler: Error reading location:" +url+" error:" + result.getResult());
	    throw new IllegalArgumentException("Error reading location:" + result.getResult());
	}
	JSONObject obj           = new JSONObject(result.getResult());
	JSONObject location = obj.getJSONArray("results").getJSONObject(0);
	JSONObject coords = location.getJSONObject("coordinates");

	entry.setName(location.getString("name"));
	entry.setValue(IDX_LOCALITY,location.optString("locality",""));	
	entry.setValue(IDX_COUNTRY,JU.readValue(location,"country.name",""));
	entry.setValue(IDX_PROVIDER,JU.readValue(location,"provider.name",""));	
	entry.setValue(IDX_STATIONARY,new Boolean(!location.optBoolean("isMobile",false)));
	entry.setLatitude(coords.getDouble("latitude"));
	entry.setLongitude(coords.getDouble("longitude"));	

	List<String>ss=new ArrayList<String>();
	JSONArray sensors = location.getJSONArray("sensors");
	for(int i=0;i<sensors.length();i++)  {
	    JSONObject sensor = sensors.getJSONObject(i);
	    JSONObject parameter = sensor.getJSONObject("parameter");
	    ss.add(sensor.getInt("id")+
		   ";"+parameter.getString("name")+
		   ";"+parameter.getString("units")+
		   ";"+parameter.getString("displayName"));
													     
	}
	entry.setValue(IDX_SENSORS,Utils.join(ss,","));
    }

    @Override
    public RecordFile doMakeRecordFile(Request request, Entry entry,
                                       Hashtable properties,
                                       Hashtable requestProperties)
	throws Exception {
	IO.Path path = getPathForRecordEntry(request, entry,requestProperties);
	String key = getRepository().getProperty("openaq.api.key",null);
	if(key==null) {
	    throw new IllegalStateException("No OpenAQ API key is defined");
	}
	return new OpenAQRecordFile(entry, path,getSensor(request, entry,request.getString("instrument",null)));
    }

    @Override
    public IO.Path getPathForRecordEntry(Request request,Entry entry,  Hashtable requestProperties)
	throws Exception {
        String location = entry.getStringValue(request,IDX_LOCATION_ID, (String) null);
        if ( !Utils.stringDefined(location)) {
            System.err.println("no location");
            return null;
        }
        Date now = new Date();
        Integer hoursOffset = (Integer) entry.getIntValue(request,IDX_HOURS_OFFSET,
							  Integer.valueOf(24*7));

        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(now);
        if (dateSDF == null) {
	    //            dateSDF = Utils.makeDateFormat("yyyy-MM-dd'T'HH:mm");
	    dateSDF = Utils.makeDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        }
        String endDate = Utils.format(dateSDF,cal.getTime());
        cal.add(cal.HOUR_OF_DAY, -hoursOffset.intValue());
        String startDate = Utils.format(dateSDF,cal.getTime());
	String key = getRepository().getProperty("openaq.api.key",null);
	if(key==null) {
	    throw new IllegalStateException("No OpenAQ API key is defined");
	}
	Sensor sensor =getSensor(request, entry,request.getString("instrument",null));
	String url  = HU.url("https://api.openaq.org/v3/sensors/" + sensor.id+"/measurements",
			     "limit","1000",
			     "page","1",
			     //			     "offset","0",
			     //			     "sort","desc",
			     //			     "order_by","datetime",
			     "datetime_from", startDate,
			     "datetime_to", endDate);

	IO.Path path = new IO.Path(url);
	//	System.err.println("URL:"+url);
	path.setRequestArgs(new String[]{"X-API-Key",key});
        return path;
    }

    @Override
    public Object getWikiProperty(Request request,Entry entry, String id,Hashtable props)  {
	if(!id.equals("openaq.instruments")) {
	    return super.getWikiProperty(request, entry, id,props);
	}
	StringBuilder sb = new StringBuilder();
	for(Sensor sensor:getSensors(request,  entry)) {
	    if(sb.length()>0) sb.append(",");
	    sb.append(sensor.id+":" + sensor.name);
	}
	System.err.println(sb);
	return sb.toString();
    }


    public Sensor getSensor(Request request, Entry entry,String id) {
	if(id!=null)  {
	    for(Sensor sensor: getSensors(request, entry)) {
		if(sensor.id.equals(id)) return sensor;
	    }
	}
	String s = entry.getStringValue(request, "sensors","");
	List<String> toks = Utils.split(s,",",true,true);
	if(toks.size()==0) throw new IllegalStateException("No sensors defined");
	int index = Integer.parseInt(entry.getStringValue(request,"sensorindex","0"));
	List<String> tuple = Utils.split(toks.get(index),";",true,true);
	return new Sensor(tuple.get(0),tuple.get(1),tuple.get(2),tuple.get(3));
    }

    public List<Sensor> getSensors(Request request, Entry entry) {
	String s = entry.getStringValue(request, "sensors","");
	List<Sensor> sensors = new ArrayList<Sensor>();
	List<String> toks = Utils.split(s,",",true,true);
	if(toks.size()==0) return sensors;
	for(String t: toks) {
	    List<String> tuple = Utils.split(t,";",true,true);
	    sensors.add(new Sensor(tuple.get(0),tuple.get(1),tuple.get(2),tuple.get(3)));
	}
	return sensors;
    }

    public static class Sensor {
	String id;
	String name;
	String unit;
	String displayName;
	Sensor(String id, String name,String unit, String displayName) {
	    this.id=id;
	    this.name  = name;
	    this.unit=unit;
	    this.displayName=displayName;
	}
    }

    public static class OpenAQRecordFile extends CsvFile {
	Entry entry;
	Sensor sensor;
        public OpenAQRecordFile(Entry _entry, IO.Path path,Sensor sensor) throws IOException {
            super(path);
	    this.entry  = _entry;
	    this.sensor = sensor;
        }

        @Override
        public InputStream doMakeInputStream(boolean buffered)
	    throws Exception {
	    String[]args = new String[SEESV_ARGS.length];
	    for(int i=0;i<args.length;i++) {
		args[i]=SEESV_ARGS[i].replace("${name}",sensor.name).replace("${unit}",sensor.unit).replace("${displayname}",sensor.displayName);
		
	    }
	    return applySeesv(entry,args);
	}
    }
}
