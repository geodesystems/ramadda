/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.point;


import org.ramadda.data.point.text.*;
import org.ramadda.data.record.*;
import org.ramadda.data.services.PointTypeHandler;
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
import java.util.List;
import java.io.*;
import java.net.URL;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.GregorianCalendar;

import java.util.Hashtable;


public class WaggleTypeHandler extends PointTypeHandler {

    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSS'Z'";

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
        return new WaggleRecordFile(getRepository(), this,entry);
    }


    private static final String URL ="https://data.sagecontinuum.org/api/v1/query";
    //https://data.sagecontinuum.org

    @Override
    public void initializeNewEntry(Request request, Entry entry,
                                   boolean fromImport)
            throws Exception {
	if(fromImport) return;
	String baseUrl  =(String) entry.getValue("serverroot");
	if(!stringDefined(baseUrl)) return;
	String nodeId  =(String) entry.getValue("nodeid");
	if(!stringDefined(nodeId)) return;	
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
	    getSessionManager().addSessionErrorMessage(request,"Could not find node:" + nodeId + " from: " + url);
	    return;
	}
	if(!stringDefined(entry.getName())) entry.setName(vsn.getString("address"));
	entry.setValue("focus",vsn.getString("focus"));
	String location = vsn.getString("location");
	int idx = location.lastIndexOf(",");
	if(idx>=0) {
	    String state = location.substring(idx+1).trim();
	    entry.setValue("state",state);
	}
	    
	    
	entry.setValue("location",location);
	entry.setValue("node_type",vsn.getString("node_type"));	
	entry.setValue("notes",vsn.getString("notes").replace("\n","<br>"));		
	//	System.err.println(vsn);

	URL gpsUrl = getDataUrl(baseUrl);
	String payload = "{\"start\":\"-6m\",\"filter\":{\"vsn\":\"" + nodeId+"\",\"name\":\"sys.gps.*\"},\"tail\":1}";
	String gps = IO.doPost(gpsUrl,payload);
	for(WaggleRecord record: parseData(gps)) {
	    if(record.name.equals("sys.gps.lat")) entry.setLatitude(record.value);
	    else if(record.name.equals("sys.gps.lon")) entry.setLongitude(record.value);
	    else if(record.name.equals("sys.gps.alt")) entry.setAltitude(record.value);    	    
	}


    }

    public List<WaggleRecord> parseData(String data) {
	List<WaggleRecord> records = new ArrayList<WaggleRecord>();
	for(String line:Utils.split(data,"\n",true,true)) {
	    if(records.size()<10) System.err.println(line);
	    JSONObject obj = new JSONObject(line);
	    records.add(new WaggleRecord(obj.getString("timestamp"),
					 obj.getString("name"),
					 obj.optDouble("value",Double.NaN)));					 
	}
	return records;
    }

    public URL getDataUrl(Entry entry) throws Exception {
	return getDataUrl((String)entry.getValue("serverroot"));
    }

    public URL getDataUrl(String base) throws Exception {
	return new URL("https://data." + base +"/api/v1/query");
    }


    private static String format(Date d) {
	if(d==null) return null;
	return new SimpleDateFormat(DATE_FORMAT).format(d);
    }


    public static class WaggleRecordFile extends CsvFile {

        Repository repository;

	WaggleTypeHandler typeHandler;

        Entry entry;

        public WaggleRecordFile(Repository repository, WaggleTypeHandler typeHandler, Entry entry)
	    throws IOException {
            this.repository = repository;
	    this.typeHandler = typeHandler;
            this.entry      = entry;
        }

        @Override
        public InputStream doMakeInputStream(boolean buffered)
                throws Exception {
	    String nodeId = (String) entry.getValue("nodeid");
	    final String variable = (String) entry.getValue("variable");

	    String start = (String)entry.getValue("date_offset");
	    String end =null;
	    Date startDate = DateHandler.checkDate(new Date(entry.getStartDate()));
	    Date endDate = DateHandler.checkDate(new Date(entry.getEndDate()));

	    if(startDate!=null && endDate!=null) {
		start=format(startDate);
		end = format(endDate);
	    } else if(endDate!=null && Utils.stringDefined(start)) {
		start=start.trim();
		String num = StringUtil.findPattern(start,"^([\\-\\d]+)");
		String unit = StringUtil.findPattern(start,"(d|h|m|s)$");
		if(num!=null && unit!=null) {
		    long base=0;
		    if(unit.equals("d")) base=DateUtil.daysToMillis(1);
		    else if(unit.equals("h")) base=DateUtil.hoursToMillis(1);		    
		    else if(unit.equals("m")) base=DateUtil.minutesToMillis(1);
		    else if(unit.equals("s")) base=1000;
		    long ms = Integer.parseInt(num)*base;
		    start=format(new Date(endDate.getTime()+ms));
		    end = format(endDate);
		} else {
		    System.err.println("Waggle: could not parse start:" + start);
		}		    
	    }

	    if(!Utils.stringDefined(start)) {
		start="-60m";
	    }

	    List<String> items = new ArrayList<String>();
	    Utils.add(items,"filter",JU.map("vsn",JU.quote(nodeId),"name",JU.quote(variable)));
	    Utils.add(items,"start",JU.quote(start));
	    if(Utils.stringDefined(end)) {
		Utils.add(items,"end",JU.quote(end));
	    }

	    String payload = JU.map(items);
	    System.err.println("query:"+payload.replace("\n"," "));
	    final IO.Path path = new IO.Path(typeHandler.getDataUrl(entry).toString(),IO.HTTP_METHOD_POST,payload,null);
	    InputStream pipedStream =IO.pipeIt(new IO.PipedThing(){
		    public void run(OutputStream os) {
			PrintStream           pw  = new PrintStream(os);
			try {
			    InputStream dataStream = IO.doMakeInputStream(path,false);
			    BufferedReader reader = new BufferedReader(new InputStreamReader(dataStream));
			    pw.append("date[format=\"" + DATE_FORMAT+"\"],");
			    pw.append(variable);
			    pw.append("\n");
			    String line;
			    WaggleRecord record = new WaggleRecord();
			    while((line=reader.readLine())!=null) {
				record.read(line);
				pw.append(record.timestamp);
				pw.append(",");
				pw.print(record.value);
				pw.append("\n");
			    }
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
	String name;
	double value;
	public WaggleRecord() {
	}
	public WaggleRecord(String line) {
	    read(line);

	}

	public WaggleRecord(String timestamp,	String name,double value) {
	    this.timestamp=timestamp;
	    this.name = name;
	    this.value=value;
	}

	public void read(String line) {
	    JSONObject obj = new JSONObject(line);
	    timestamp=obj.getString("timestamp");
	    name = obj.getString("name");
	    value=obj.optDouble("value",Double.NaN);
	}


    }

}
