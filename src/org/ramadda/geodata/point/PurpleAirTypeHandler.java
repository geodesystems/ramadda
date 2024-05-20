/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.point;


import org.json.*;

import org.ramadda.data.point.text.*;
import org.ramadda.data.record.*;
import org.ramadda.data.services.PointTypeHandler;
import org.ramadda.repository.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.IO;
import org.ramadda.util.Utils;
import org.ramadda.util.seesv.Seesv;

import org.w3c.dom.*;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.TwoFacedObject;
import java.io.*;

import java.net.URL;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;


/**
 */
public class PurpleAirTypeHandler extends PointTypeHandler {


    private static String FIELDS_DEFAULT="default";
    private static String FIELDS_SHORT="short";
    private static String FIELDS_ALL="all";

    /**  */
    private static boolean testMode = false;


    /**  */
    private static boolean debug = false;



    /**  */
    private String apiKey;

    /** _more_ */
    private SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");

    /** _more_ */
    private static int IDX = PointTypeHandler.IDX_LAST + 1;

    /**  */
    public static final int IDX_SENSOR_ID = IDX++;

    /**  */
    public static final int IDX_PRIVATE_KEY = IDX++;

    /**  */
    public static final int IDX_ACTIVE = IDX++;

    public static final int IDX_FIELDS = IDX++;    

    /**  */
    public static final int IDX_MODEL = IDX++;

    /**  */
    public static final int IDX_HARDWARE = IDX++;

    /**  */
    public static final int IDX_LOCATION_TYPE = IDX++;

    /**  */
    private static final String FIELDS_STRING_DEFAULT =
        "humidity,temperature,pressure,voc,ozone1,pm1.0,pm2.5,pm10.0";



    /**  */
    private static final String FIELDS_STRING_SHORT =
        "humidity,temperature,pressure,voc,ozone1,pm1.0,pm2.5,pm10.0,0.3_um_count,0.5_um_count,1.0_um_count,2.5_um_count,5.0_um_count,10.0_um_count";


    private static final String FIELDS_DOWNLOAD_ALL = "humidity,humidity_a,humidity_b,temperature,temperature_a,temperature_b,pressure,pressure_a,pressure_b,voc,voc_a,voc_b,analog_input,pm1.0_atm,pm1.0_atm_a,pm1.0_atm_b,pm1.0_cf_1,pm1.0_cf_1_a,pm1.0_cf_1_b,pm2.5_alt,pm2.5_alt_a,pm2.5_alt_b,pm2.5_atm,pm2.5_atm_a,pm2.5_atm_b,pm2.5_cf_1,pm2.5_cf_1_a,pm2.5_cf_1_b,pm10.0_atm,pm10.0_atm_a,pm10.0_atm_b,pm10.0_cf_1,pm10.0_cf_1_a,pm10.0_cf_1_b,scattering_coefficient,scattering_coefficient_a,scattering_coefficient_b,deciviews,deciviews_a,deciviews_b,visual_range,visual_range_a,visual_range_b,0.3_um_count,0.3_um_count_a,0.3_um_count_b,0.5_um_count,0.5_um_count_a,0.5_um_count_b,1.0_um_count,1.0_um_count_a,1.0_um_count_b,2.5_um_count,2.5_um_count_a,2.5_um_count_b,5.0_um_count,5.0_um_count_a,5.0_um_count_b,10.0_um_count,10.0_um_count_a,10.0_um_count_b";



    /**  */
    private static final String FIELDS_STRING_ALL =
        "humidity,humidity_a,humidity_b,temperature,temperature_a,temperature_b,pressure,pressure_a,pressure_b,voc,voc_a,voc_b,ozone1,analog_input,pm1.0,pm1.0_a,pm1.0_b,pm1.0_atm,pm1.0_atm_a,pm1.0_atm_b,pm1.0_cf_1,pm1.0_cf_1_a,pm1.0_cf_1_b,pm2.5_alt,pm2.5_alt_a,pm2.5_alt_b,pm2.5,pm2.5_a,pm2.5_b,pm2.5_atm,pm2.5_atm_a,pm2.5_atm_b,pm2.5_cf_1,pm2.5_cf_1_a,pm2.5_cf_1_b,pm2.5_10minute,pm2.5_10minute_a,pm2.5_10minute_b,pm2.5_30minute,pm2.5_30minute_a,pm2.5_30minute_b,pm2.5_60minute,pm2.5_60minute_a,pm2.5_60minute_b,pm2.5_6hour,pm2.5_6hour_a,pm2.5_6hour_b,pm2.5_24hour,pm2.5_24hour_a,pm2.5_24hour_b,pm2.5_1week,pm2.5_1week_a,pm2.5_1week_b,pm10.0,pm10.0_a,pm10.0_b,pm10.0_atm,pm10.0_atm_a,pm10.0_atm_b,pm10.0_cf_1,pm10.0_cf_1_a,pm10.0_cf_1_b,scattering_coefficient,scattering_coefficient_a,scattering_coefficient_b,deciviews,deciviews_a,deciviews_b,visual_range,visual_range_a,visual_range_b,0.3_um_count,0.3_um_count_a,0.3_um_count_b,0.5_um_count,0.5_um_count_a,0.5_um_count_b,1.0_um_count,1.0_um_count_a,1.0_um_count_b,2.5_um_count,2.5_um_count_a,2.5_um_count_b,5.0_um_count,5.0_um_count_a,5.0_um_count_b,10.0_um_count,10.0_um_count_a,10.0_um_count_b";

    private static final List<String> FIELDS_LIST_DEFAULT =
        Utils.split(FIELDS_STRING_DEFAULT, ",");


    /**  */
    private static final List<String> FIELDS_LIST_SHORT =
        Utils.split(FIELDS_STRING_SHORT, ",");

    /**  */
    private static final List<String> FIELDS_LIST_ALL =
        Utils.split(FIELDS_STRING_ALL, ",");


    private static String FIELDS_PROPERTY_DEFAULT;
    private static String FIELDS_PROPERTY_SHORT;
    private static String FIELDS_PROPERTY_ALL;    


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
        apiKey = repository.getProperty("purpleair.api.key", (String) null);
        if (apiKey != null) {
            Misc.run(new Runnable() {
                public void run() {
                    try {
                        //Wait a minute for RAMADDA to start up
                        if (testMode) {
                            Misc.sleepSeconds(5);
                        } else {
                            Misc.sleepSeconds(60);
                        }
                        runInBackground();
                    } catch (Exception exc) {
                        getLogManager().logError(
                            "Error running purple air fetch loop", exc);
                    }
                }
            });
        }
    }


    private String getFieldsType(Entry entry) {
	return (String)entry.getValue(IDX_FIELDS);
    }




    private String makeFieldsProperty(Entry entry,List<String>fields) {
	StringBuilder sb = new StringBuilder();
	sb.append("date[type=date format=\"yyyy-MM-dd'T'HH:mm:ssZ\"]");
	for(String field: fields) {
	    field = field.replace(".","_");
	    sb.append(",");
	    sb.append(field);	    
	    sb.append("[type=double ");
	    if(field.indexOf("humidity")>=0) sb.append(" unit=\"%\" ");
	    else if(field.indexOf("temperature")>=0) sb.append(" offset1=\"-32\" scale=\"0.555555\" unit=\"celsius\" ");	    
	    else if(field.indexOf("pressure")>=0) sb.append(" unit=\"mb\" ");
	    else if(field.indexOf("ozone")>=0) sb.append(" unit=\"ppb\" ");	    
	    sb.append("]");
	}
	return sb.toString();
    }	

    private String getFieldsProperty(Entry entry) {
	if(FIELDS_PROPERTY_SHORT==null) {
	    FIELDS_PROPERTY_DEFAULT = makeFieldsProperty(entry,FIELDS_LIST_DEFAULT);
	    FIELDS_PROPERTY_SHORT = makeFieldsProperty(entry,FIELDS_LIST_SHORT);
	    FIELDS_PROPERTY_ALL = makeFieldsProperty(entry,FIELDS_LIST_ALL);	    
	}
	if(Utils.equals(FIELDS_DEFAULT, getFieldsType(entry)))
	    return FIELDS_PROPERTY_DEFAULT;
	if(Utils.equals(FIELDS_ALL, getFieldsType(entry)))
	    return FIELDS_PROPERTY_ALL;
	return FIELDS_PROPERTY_SHORT;	
    }

    /**
     *
     * @param entry _more_
     * @return _more_
     */
    private String getDataFields(Entry entry) {
	if(Utils.equals(FIELDS_DEFAULT, getFieldsType(entry)))
	    return FIELDS_STRING_ALL;
	if(Utils.equals(FIELDS_ALL, getFieldsType(entry)))
	    return FIELDS_STRING_ALL;
	return FIELDS_STRING_SHORT;
    }

    /**
     *
     * @param entry _more_
     * @return _more_
     */
    private List<String> getFieldsList(Entry entry) {
	if(Utils.equals(FIELDS_DEFAULT, getFieldsType(entry)))
	    return FIELDS_LIST_DEFAULT;

	if(Utils.equals(FIELDS_ALL, getFieldsType(entry)))
	    return FIELDS_LIST_ALL;

        return FIELDS_LIST_SHORT;
    }


    /**
     *
     * @param entry _more_
      * @return _more_
     */
    private String getFileHeader(Entry entry) {
	return "date," + getDataFields(entry)   + "\n";
    }


    /**
     */
    private void sleepUntil() {
        if (testMode) {
            System.err.println("PurpleAir test sleeping for:" + 10);
            Misc.sleepSeconds(10);

            return;
        }
        int freq = getRepository().getProperty("purpleair.frequency", 15);
        Utils.sleepUntil(freq, testMode || debug);
    }


    /**
     *
     * @throws Exception _more_
     */
    private void runInBackground() throws Exception {
        Request searchRequest = getRepository().getAdminRequest();
        sleepUntil();
        while (true) {
            if (debug) {
                System.err.println("PurpleAair fetching data");
            }
            StringBuilder tmp = new StringBuilder();
            List<Entry> entries =
                getEntryManager().getEntriesWithType(searchRequest,
                    "type_point_purpleair");
            for (Entry entry : entries) {
                if ( !entry.getValue(IDX_ACTIVE).toString().equals("true")) {
                    if (debug) {
                        System.err.println("\tskipping:" + entry);
                    }
                    continue;
                }
                try {
                    fetchData(searchRequest, entry);
                } catch (Exception exc) {
                    getLogManager().logError("Error fetching purple air data:" + entry + " id:" +entry.getId()+"\n" + exc.getMessage());
                }
            }
            //            Misc.sleepSeconds(10);
            sleepUntil();
        }
    }



    /**
     *
     * @param request _more_
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    private void fetchData(Request request, Entry entry) throws Exception {
        Sensor sensor = readSensor(entry, getDataFields(entry));
        if (sensor == null) {
            System.err.println("\tfetching failed to read sensor data:"
                               + entry);

            return;
        }
        int points = entry.getIntValue(IDX_RECORD_COUNT, 0);
        if (points == 0) {
            //If its the first record
            entry.setStartDate(sensor.date.getTime());
        } else {
            //Check if there has been no new records
            if (entry.getEndDate() == sensor.date.getTime()) {
                if (debug) {
                    System.err.println(
                        "\tPurpleAir: no data change for entry:"
                        + entry.getName());
                }

                return;
            }
        }

        entry.setEndDate(sensor.date.getTime());
        StringBuilder row = new StringBuilder(Utils.formatIso(sensor.date));
        if (debug) {
            System.err.println("\tfetching:" + entry + " dttm:"
                               + sensor.date);
        }
        for (String field : getFieldsList(entry)) {
            double d = sensor.data.optDouble(field);
            row.append(",");
            row.append(d);
        }
        row.append("\n");

        File             file = entry.getFile();
        FileOutputStream fos  = new FileOutputStream(file, true);
        fos.write(row.toString().getBytes());
        fos.close();
        entry.getResource().setFileSize(file.length());


        entry.setValue(IDX_RECORD_COUNT, points + 1);
        getEntryManager().updateEntry(request, entry);

    }

    @Override
    public void initializeNewEntry(Request request, Entry entry,NewType newType)
            throws Exception {
        super.initializeNewEntry(request, entry, newType);
	if(!isNew(newType)) return;
        String id = (String) entry.getStringValue(IDX_SENSOR_ID, "");
        File newFile = getStorageManager().getTmpFile(request,
                           id + "_purpleair.csv");
        IOUtil.writeTo(
            new ByteArrayInputStream(getFileHeader(entry).getBytes()),
            new FileOutputStream(newFile));
        newFile = getStorageManager().moveToStorage(request, newFile);
        entry.setResource(new Resource(newFile.toString(),
                                       Resource.TYPE_STOREDFILE));
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
        Sensor sensor =
            readSensor(
                entry,
                "name,model,hardware,location_type,latitude,longitude,altitude");
        if (sensor == null) {
            return;
        }
        String name = sensor.data.getString("name");
        if ( !Utils.stringDefined(entry.getName())) {
            entry.setName(name);
        }
        String alias = "purpleair_" + Utils.makeID(name);
	getMetadataManager().addMetadataAlias(request,entry, alias);
        entry.setLatitude(sensor.data.getDouble("latitude"));
        entry.setLongitude(sensor.data.getDouble("longitude"));
        entry.setAltitude(sensor.data.getDouble("altitude"));
        entry.setValue(IDX_MODEL, sensor.data.getString("model"));
        entry.setValue(IDX_HARDWARE, sensor.data.getString("hardware"));
        entry.setValue(IDX_LOCATION_TYPE,
                       (sensor.data.getInt("location_type") == 0)
                       ? "outside"
                       : "inside");
    }


    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Feb 3, '22
     * @author         Enter your name here...
     */
    private static class Sensor {

        /**  */
        Date date;

        /**  */
        JSONObject data;

        /**
         *
         *
         * @param date _more_
         * @param data _more_
         */
        public Sensor(Date date, JSONObject data) {
            this.date = date;
            this.data = data;
        }
    }

    private String getPrivateKey(Entry entry) {
	String privateKey = (String) entry.getStringValue(IDX_PRIVATE_KEY, "");
        if (Utils.stringDefined(privateKey)) {
            privateKey = privateKey.trim();
            String tmp = getRepository().getProperty(privateKey,
                             (String) null);
            if (Utils.stringDefined(tmp)) {
                privateKey = tmp;
            }
	    return privateKey;
        }
	return null;
    }


    /**
     *
     * @param entry _more_
     * @param fields _more_
     *  @return _more_
     *
     * @throws Exception _more_
     */
    private Sensor readSensor(Entry entry, String fields) throws Exception {
        if (apiKey == null) {
	    System.err.println("PurpleAir: no apikey specified");
            return null;
        }
        String id = (String) entry.getStringValue(IDX_SENSOR_ID, "");
        if ( !Utils.stringDefined(id)) {
	    System.err.println("PurpleAir: no sensor id in entry");
            return null;
        }

        String url = "https://api.purpleair.com/v1/sensors/" + id + "?"
                     + HU.arg("api_key", apiKey) + "&"
                     + HU.arg("fields", fields);
	String privateKey = getPrivateKey(entry);
        if (privateKey!=null) {
            privateKey = privateKey.trim();
            url += "&" + HU.arg("read_key", privateKey);
        }

	IO.Result result = IO.doGetResult(new URL(url));
	if(result.getError()) {
	    String error = result.getResult();
	    try {
		System.err.println("PURPLE AIR:" + result.getCode() +" " + error);
		error = new JSONObject(error).getString("description");
	    } catch(Exception ignore) {}
	    error = Utils.stripTags(error);
	    getLogManager().logError("PurpleAir: Error reading PurpleAir for site:" + id+" entry:" + entry.getName() +" id:" + entry.getId() +" error:" +error);
	    throw new RuntimeException("Error accessing PurpleAir API for site:" + id+" error:" + error);
	}

	String json = result.getResult();
	JSONObject obj           = new JSONObject(json);
	long       dataTimeStamp = obj.getLong("data_time_stamp");
	return new Sensor(new Date(1000 * dataTimeStamp),
			  obj.getJSONObject("sensor"));
    }


    /**
     *
     * @param request _more_
     * @param entry _more_
     *  @return _more_
     *
     * @throws Exception _more_
     */
    public Result processEntryAction(Request request, Entry entry)
            throws Exception {
        String action = request.getString("action", "");
        if (action.equals("gethistory")) {
            return processGetHistory(request, entry);
	}

        if ( !action.equals("clearfile")) {
            return super.processEntryAction(request, entry);
        }
        boolean       canEdit = getAccessManager().canDoEdit(request, entry);
        StringBuilder sb      = new StringBuilder();
        getPageHandler().entrySectionOpen(request, entry, sb, "");
        if ( !canEdit) {
            sb.append(
                getPageHandler().showDialogError(
                    "You don't have permission to clear the file"));
        } else if (request.get("confirm", false)) {
            File             file = entry.getFile();
            FileOutputStream fos  = new FileOutputStream(file);
            fos.write(getFileHeader(entry).getBytes());
            fos.close();
            entry.setValue(IDX_RECORD_COUNT, 0);
            entry.getResource().setFileSize(file.length());
            getEntryManager().updateEntry(request, entry);
            sb.append(
                getPageHandler().showDialogNote("OK, file has been cleared"));
        } else {
            sb.append(
                getPageHandler().showDialogQuestion(
                    "Are you sure you want to clear the file?",
                    HU.div(HU.href(
                        getEntryActionUrl(request, entry, "clearfile")
                        + "&confirm=true", "Yes, clear the file"), HU.cssClass(
                            "ramadda-button"))));
        }

        getPageHandler().entrySectionClose(request, entry, sb);

        return getEntryManager().addEntryHeader(request, entry,
                new Result("Clear File", sb));
    }

    private static final String ARG_GETHISTORY="gethistory";
    private static final String ARG_START_TIMESTAMP="start_timestamp";
    private static final String ARG_END_TIMESTAMP="end_timestamp";
    private static final String ARG_AVERAGE ="average";
    private static final String ARG_FIELDS ="fields";
    private static final String ARG_DOWNLOAD = "download";


    private static final String DOWNLOAD_TITLE="PurpleAir Download";
    public Result processGetHistory(Request request, Entry entry)
            throws Exception {
        StringBuilder sb      = new StringBuilder();
        if (apiKey == null || request.isAnonymous() ) {
	    getPageHandler().entrySectionOpen(request, entry, sb, DOWNLOAD_TITLE);
            sb.append(getPageHandler().showDialogWarning(request.isAnonymous() ?
							 "Purple Air download is only available to logged in users":"No API Key"));
	    getPageHandler().entrySectionClose(request, entry, sb);
	    return getEntryManager().addEntryHeader(request, entry, new Result(DOWNLOAD_TITLE, sb));
	}

	Date startTimestamp=null;
	Date endTimestamp=null;
	StringBuilder messageSB = new StringBuilder();
	boolean inError = false;
	if(request.defined(ARG_START_TIMESTAMP)) {
	    try  {
		startTimestamp = request.getDate(ARG_START_TIMESTAMP,null);
	    } catch(Exception exc) {
		messageSB.append(getPageHandler().showDialogError("Bad start date:" + request.getString(ARG_START_TIMESTAMP,"")));
		inError = true;
	    }		
	}
	if(request.defined(ARG_END_TIMESTAMP)) {
	    try {
		endTimestamp = request.getDate(ARG_END_TIMESTAMP,null);
	    } catch(Exception exc) {
		messageSB.append(getPageHandler().showDialogError("Bad end date:" + request.getString(ARG_END_TIMESTAMP,"")));
		inError = true;
	    }
	} 
	if (!inError && request.exists(ARG_DOWNLOAD)) {
	    String fields = request.getString(ARG_FIELDS,"");
	    if(!stringDefined(fields)) fields = FIELDS_DOWNLOAD_ALL;
	    String url = HU.url("https://api.purpleair.com/v1/sensors/" +
				entry.getStringValue(IDX_SENSOR_ID,"")+"/history/csv",
				ARG_FIELDS,fields,
				ARG_AVERAGE,request.getString(ARG_AVERAGE,""));
	    String privateKey = getPrivateKey(entry);
	    if (privateKey!=null) {
		privateKey = privateKey.trim();
		url=HU.url(url, "read_key",privateKey);
	    }

	    
	    if(startTimestamp!=null) 
		url=HU.url(url,ARG_START_TIMESTAMP,Utils.format(sdf,startTimestamp.getTime())+"T00:00:00Z");
	    //Always have an end time
	    Date endTime = endTimestamp!=null?endTimestamp:new Date();;
	    if(endTime!=null) url=HU.url(url,
					 ARG_END_TIMESTAMP,Utils.format(sdf,endTime.getTime())+"T23:59:59Z");
		try {
		IO.Result result  = IO.getInputStreamFromGet(new URL(url),"X-API-Key",apiKey);
		if(result.getError()) {
		    String message = result.getResult();
		    try {
			JSONObject json = new JSONObject(message.trim());
			message = json.getString("description");
		    } catch(Exception ignore) {
			System.err.println(ignore);
		    }
		    message = Utils.stripTags(message);
		    messageSB.append(getPageHandler().showDialogError("An error has occurred: " + message));
		} else {
		    InputStream bis = result.getInputStream();
		    request.setReturnFilename(entry.getName()+".csv");
		    request.setHeader("Cache-Control","no-cache, no-store, must-revalidate").setHeader("Pragma","no-cache").setHeader("Expires","0");

		    //		    if(true) return  new Result(bis,"text/csv");

		    PipedInputStream      in   = new PipedInputStream();
		    PipedOutputStream     out  = new PipedOutputStream(in);
		    final Seesv seesv = new Seesv(new String[]{"-sort","time_stamp","-p"},
					    bis,out);		    


		    Result theResult = new Result(in,"text/csv");
		    Misc.run(new Runnable() {
			    public void run()  {
				try {
				    seesv.run(null);
				} catch(Exception exc) {
				    System.err.println("Error:" + exc);
				    exc.printStackTrace();
				    throw new RuntimeException(exc);
				}
			    }
			});


		    return theResult;
		}
	    } catch(Exception exc) {
		messageSB.append(getPageHandler().showDialogError("An error has occurred: " + exc));
	    }
        }
	
	getPageHandler().entrySectionOpen(request, entry, sb, DOWNLOAD_TITLE);

	sb.append(getWikiManager().wikify(request,"+note\nThis downloads historic sensor data from PurpleAir\n-note"));


	if(messageSB.length()>0) {
	    sb.append(messageSB);
	}
	sb.append(request.form(getRepository().URL_ENTRY_ACTION));
	sb.append(HU.hidden(ARG_ENTRYID,entry.getId()));
	sb.append(HU.hidden(ARG_ACTION,"gethistory"));	
	sb.append(HU.formTable());
	sb.append(HU.formEntry("Start Date:",
                getDateHandler().makeDateInput(request, ARG_START_TIMESTAMP,
					       "entryform", startTimestamp,null,false)));
	sb.append(HU.formEntry("End Date:",
                getDateHandler().makeDateInput(request, ARG_END_TIMESTAMP,
					       "entryform", endTimestamp,null,false)));	

	List<TwoFacedObject> averages  = new ArrayList<TwoFacedObject>();
	averages.add(new TwoFacedObject("Real time","0"));
	averages.add(new TwoFacedObject("10 minutes","10"));
	averages.add(new TwoFacedObject("30 minutes","30"));
	averages.add(new TwoFacedObject("60 minutes","60"));
	averages.add(new TwoFacedObject("6 hour","360"));
	averages.add(new TwoFacedObject("1 day","1440"));	    			
	sb.append(HU.formEntry("Average:",HU.select(ARG_AVERAGE,averages,request.getString(ARG_AVERAGE,"10"))));
	sb.append(HU.formEntry("Fields:",HU.input(ARG_FIELDS,request.getString(ARG_FIELDS,""),
						  HU.style("width:800px;"))));
	sb.append(HU.formEntry("",HU.submit("Download",ARG_DOWNLOAD)));	
	sb.append(HU.formTableClose());
	sb.append(HU.formClose());
	sb.append("Fields default to:<br>" +
		  FIELDS_DOWNLOAD_ALL.replace(",",", "));
	getPageHandler().entrySectionClose(request, entry, sb);
	return getEntryManager().addEntryHeader(request, entry,
						new Result(DOWNLOAD_TITLE, sb));
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
        return new PurpleAirRecordFile(getRepository(), this,entry,
                                       new IO.Path(getPathForEntry(request, entry, true)));
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

	PurpleAirTypeHandler typeHandler;

        /** _more_ */
        Entry entry;

        /**
         * _more_
         *
         *
         * @param repository _more_
         * @param entry _more_
         *
         * @throws IOException _more_
         */
        public PurpleAirRecordFile(Repository repository, PurpleAirTypeHandler typeHandler, Entry entry,
                                   IO.Path path)
                throws IOException {
            super(path);
	    this.typeHandler = typeHandler;
            this.repository = repository;
            this.entry      = entry;
        }

	@Override
	public String getFieldsProperty() {
	    return typeHandler.getFieldsProperty(entry);
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
