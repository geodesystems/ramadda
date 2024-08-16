/**
Copyright (c) 2008-2024 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.point;


import org.ramadda.data.point.text.*;
import org.ramadda.data.record.*;
import org.ramadda.data.services.PointTypeHandler;
import org.ramadda.data.services.RecordConstants;
import org.ramadda.data.services.RecordTypeHandler;
import org.ramadda.repository.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.JsonUtil;



import org.ramadda.util.IO;
import org.ramadda.util.Utils;
import org.ramadda.util.seesv.Seesv;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.StringUtil;

import org.w3c.dom.*;

import org.json.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.List;
import java.io.*;
import java.net.URL;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.GregorianCalendar;



public class WaggleTypeHandler extends PointTypeHandler {

    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.S'Z'";

    private static int IDX = PointTypeHandler.IDX_LAST + 1;

    private static int IDX_STRIDE = IDX++;

    public WaggleTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }



    @Override
    public RecordFile doMakeRecordFile(Request request, Entry entry,
                                       Hashtable properties,
                                       Hashtable requestProperties)
            throws Exception {
        return new WaggleRecordFile(request,getRepository(), this,entry);
    }


    @Override
    public void initializeNewEntry(Request request, Entry entry, NewType newType)
            throws Exception {
	if(!isNew(newType)) return;
	initializeNewEntryInner(request,  entry);
	String  bulkFile = request.getUploadedFile(ARG_BULKUPLOAD,true);
	if(!stringDefined(bulkFile) || !new File(bulkFile).exists()) return;
	HashSet<String> seen = new HashSet<String>();
	List<Entry> entries = handleBulkUpload(request, entry.getParentEntry(),bulkFile,"nodeid",seen,null,null);
	for(Entry newEntry: entries) {
	    initializeNewEntryInner(request,newEntry);
	}
	getEntryManager().insertEntriesIntoDatabase(request,  entries,true, true);	
    }

    private void initializeNewEntryInner(Request request, Entry entry)
            throws Exception {
	String baseUrl  =(String) entry.getValue(request,"serverroot");
	if(!stringDefined(baseUrl)) return;
	String nodeId  =(String) entry.getValue(request,"nodeid");
	if(!stringDefined(nodeId)) return;	
	nodeId = nodeId.trim();
	String url = "https://api." + baseUrl +"/production";
	String json = IO.readUrl(new URL(url));
	JSONArray array   = new JSONArray(json);
	JSONObject vsn=null;
	for(int i=0;i<array.length();i++) {
	    JSONObject obj = array.getJSONObject(i);
	    if(nodeId.equals(obj.getString("vsn"))) {
		vsn=obj;
		break;
	    }
	}
	if(vsn==null) {
	    getSessionManager().addSessionMessage(request,"Could not find node:" + nodeId + " from: " + url);
	    return;
	}
	if(!stringDefined(entry.getName())) entry.setName(nodeId +" - "+ vsn.getString("address"));
	entry.setValue("focus",vsn.getString("focus"));
	entry.setValue("project",vsn.getString("project"));	
	String location = vsn.getString("location");
	int idx = location.lastIndexOf(",");
	if(idx>=0) {
	    String state = location.substring(idx+1).trim();
	    entry.setValue("state",state);
	}
	    
	    
	entry.setValue("location",location);
	entry.setValue("node_type",vsn.getString("node_type"));	
	entry.setValue("notes",vsn.getString("notes").replace("\n","<br>"));		

	URL gpsUrl = getDataUrl(request,entry);
	String payload = "{\"start\":\"-365d\",\"filter\":{\"vsn\":\"" + nodeId+"\",\"name\":\"sys.gps.*\"},\"tail\":1}";
	String gps = IO.doPost(gpsUrl,payload);
	for(WaggleRecord record: parseData(gps)) {
	    if(record.name.equals("sys.gps.lat")) entry.setLatitude(record.value);
	    else if(record.name.equals("sys.gps.lon")) entry.setLongitude(record.value);
	    else if(record.name.equals("sys.gps.alt")) entry.setAltitude(record.value);    	    
	}





	if(!entry.hasLocationDefined(request)) {
	    URL manifestUrl = new URL("https://auth."+ baseUrl+"/manifests/"+ nodeId);
	    String manifest=IO.doGet(manifestUrl,"Accept","*/*");
	    JSONObject obj = new JSONObject(manifest);
	    entry.setLatitude(obj.optDouble("gps_lat",Double.NaN));
	    entry.setLongitude(obj.optDouble("gps_lon",Double.NaN));	    
	}
	


    }

    public List<WaggleRecord> parseData(String data) {
	List<WaggleRecord> records = new ArrayList<WaggleRecord>();
	for(String line:Utils.split(data,"\n",true,true)) {
	    JSONObject obj = new JSONObject(line);
	    records.add(new WaggleRecord(obj.getString("timestamp"),
					 obj.getString("name"),
					 obj.optDouble("value",Double.NaN)));					 
	}
	return records;
    }

    public URL getDataUrl(Request request,Entry entry) throws Exception {
	return getDataUrl((String)entry.getValue(request,"serverroot"));
    }

    public URL getDataUrl(String base) throws Exception {
	return new URL("https://data." + base +"/api/v1/query");
    }


    private static String format(Date d) {
	if(d==null) return null;
	return new SimpleDateFormat(DATE_FORMAT).format(d);
    }


    public static class WaggleRecordFile extends CsvFile {

	Request request;
        Repository repository;

	WaggleTypeHandler typeHandler;

        Entry entry;

        public WaggleRecordFile(Request request,Repository repository, WaggleTypeHandler typeHandler, Entry entry)
	    throws IOException {
	    this.request=request;
            this.repository = repository;
	    this.typeHandler = typeHandler;
            this.entry      = entry;
        }

	private long  parseStep(String start) {
	    String num = StringUtil.findPattern(start,"^([\\-\\d]+)");
	    String unit = StringUtil.findPattern(start,"(d|h|m|s)$");
	    if(num!=null && unit!=null) {
		long base=0;
		if(unit.equals("d")) base=DateUtil.daysToMillis(1);
		else if(unit.equals("h")) base=DateUtil.hoursToMillis(1);		    
		else if(unit.equals("m")) base=DateUtil.minutesToMillis(1);
		else if(unit.equals("s")) base=1000;
		return Integer.parseInt(num)*base;
	    }
	    return 0;
	}
        @Override
	public String getFieldsProperty() {
	    String variable = (String) entry.getValue(request,"variable");
	    return "date[format=\"" + DATE_FORMAT+"\"]," +variable;
	}

	/**
	   Override this since when we have record.last set we know how to skip
	 */
        @Override 
	public int  getSkipToLast(int last) throws Exception {
	    return 0;
	}


        @Override
        public InputStream doMakeInputStream(VisitInfo visitInfo,boolean buffered)
                throws Exception {
	    //	    System.err.println("waggle");
	    //	    System.err.println(Utils.getStack(10));
	    String nodeId = (String) entry.getValue(request,"nodeid");
	    final String variable = (String) entry.getValue(request,"variable");
	    final String sensor = (String) entry.getValue(request,"sensor");

	    String start = (String)entry.getValue(request,"date_offset");
	    String end =null;
	    Date startDate = DateHandler.checkDate(new Date(entry.getStartDate()));
	    Date endDate = DateHandler.checkDate(new Date(entry.getEndDate()));

	    if(startDate!=null && endDate!=null) {
		start=format(startDate);
		end = format(endDate);
	    } else if(endDate!=null && Utils.stringDefined(start)) {
		start=start.trim();
		long step = parseStep(start);
		if(step!=0) {
		    start=format(new Date(endDate.getTime()+step));
		    end = format(endDate);
		} else {
		    System.err.println("Waggle: could not parse start:" + start);
		}		    
	    }

	    if(!Utils.stringDefined(start)) {
		start="-60m";
	    }

	    List<String> items = new ArrayList<String>();
	    List<String> filter = new ArrayList<String>();
	    Utils.add(filter,"vsn",JU.quote(nodeId),"name",JU.quote(variable));
	    if(Utils.stringDefined(sensor))
		Utils.add(filter,"sensor",JU.quote(sensor));
	    Utils.add(items,"filter",JU.map(filter));
	    Utils.add(items,"start",JU.quote(start));
	    int last = visitInfo.getQuickScan()?1:request.get(RecordConstants.ARG_RECORD_LAST,-1);
	    if(last>0)
		Utils.add(items,"tail",""+last);
	    if(Utils.stringDefined(end)) {
		Utils.add(items,"end",JU.quote(end));
	    }
	    String stride = (String) entry.getValue(request,"stride");
	    final long delta = Utils.stringDefined(stride)?parseStep(stride):0;
	    String payload = JU.map(items);
	    //	    System.err.println("query:"+payload.replace("\n"," "));
	    final IO.Path path = new IO.Path(typeHandler.getDataUrl(request,entry).toString(),IO.HTTP_METHOD_POST,payload,null);
	    

	    InputStream pipedStream =IO.pipeIt(new IO.PipedThing(){
		    int cnt=0;
		    public void run(OutputStream os) {
			PrintStream           pw  = new PrintStream(os);
			try {
			    long lastTime=0;
			    InputStream dataStream = IO.doMakeInputStream(path,false);
			    BufferedReader reader = new BufferedReader(new InputStreamReader(dataStream));
			    pw.append("#fields=date[format=\"" + DATE_FORMAT+"\"],");
			    pw.append(variable);
			    pw.append("\n");
			    pw.flush();
			    String line;
			    WaggleRecord record = new WaggleRecord();
			    while((line=reader.readLine())!=null) {
				cnt++;
				record.read(line);
				if(delta>0) {
				    Date d = new SimpleDateFormat(DATE_FORMAT).parse(record.timestamp);
				    if(lastTime>0 && (d.getTime()-lastTime)<delta) continue;
				    lastTime = d.getTime();
				}
				pw.append(record.timestamp);
				pw.append(",");
				pw.print(record.value);
				pw.append("\n");
				pw.flush();
			    }
			    System.err.println("Waggle records cnt:" + cnt);
			} catch(Exception exc) {
			    typeHandler.getLogManager().logError("Filtering geojson:" + entry,exc);
			    pw.println("Reading Waggle:" + exc);
			    return;
			}
		    }});
	    
	    return pipedStream;
        }
    }


    public static class WaggleRecord {
	String timestamp;
	String raw;
	String name;
	double value;
	public WaggleRecord() {
	}
	public WaggleRecord(String line) {
	    read(line);

	}

	public WaggleRecord(String timestamp,	String name,double value) {
	    this.timestamp=convertTime(timestamp);
	    this.name = name;
	    this.value=value;
	}

	public String convertTime(String t) {
	    raw = t;
	    int idx = t.indexOf(".");
	    if(idx>0) t= t.substring(0,idx+2)+"Z";
	    return t;
	}

	public void read(String line) {
	    JSONObject obj = new JSONObject(line);
	    timestamp=convertTime(obj.getString("timestamp"));
	    name = obj.getString("name");
	    value=obj.optDouble("value",Double.NaN);
	}


    }

}
