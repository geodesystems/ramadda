/**
   Copyright (c) 2008-2026 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.point;

import org.json.*;
import org.ramadda.util.JsonUtil;

import org.ramadda.data.point.text.*;
import org.ramadda.data.record.*;
import org.ramadda.data.services.PointTypeHandler;
import org.ramadda.data.services.RecordTypeHandler;
import org.ramadda.repository.*;

import org.ramadda.repository.type.*;
import org.ramadda.util.IO;
import org.ramadda.util.Utils;
import org.ramadda.util.WikiUtil;
import org.ramadda.util.geo.GeoUtils;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.sql.ResultSet;
import java.sql.Statement;

import org.w3c.dom.*;

import java.net.URL;
import java.io.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Hashtable;
import java.util.HashSet;

@SuppressWarnings("unchecked")
public class UsgsGaugeTypeHandler extends PointTypeHandler {

    private static String[] CSV_COMMANDS = {
	"-tab,-cut,1,-notcolumns,(?i).*cd",
	"-changerow,0,0-10,.*date.*,date",
	"-changerow,0,0-10,.*00065.*,Gauge Height",
	"-changerow,0,0-10,.*00060.*,Discharge",
	"-change,0-10,\\*\\*\\*,NaN",
	"-change,?discharge_comma_?gauge_height,(?i)(^$|dis|ice|ssn|eqp|rat),0,-if",
	"-has,discharge,-integrate,?discharge,date",
	"second,volume,-scale,volume,0",
	"0.00002295684,0",
	"-setting,runningsum.reset.yearly_total_volume,date:year",
	"-runningsum,volume,Total volume",
	"-runningsum,volume,yearly_total_volume",
	"-decimals,volume_comma_total_volume,2,-endif,-addheader",
	"datetime.type date site_no.type string discharge.type double gauge_height.type double total_volume.unit {acre feet} total_volume.type double volume.type double date.format {${format}yyyy-MM-dd}"
    };

    private static final String OLD_URL_FLOW =
        "https://waterdata.usgs.gov/nwis/uv?cb_00060=on&cb_00065=on&format=rdb&site_no=${station_id}&period=${period}";

    private static final String URL_FLOW =
	"https://nwis.waterdata.usgs.gov/usa/nwis/uv/?cb_00060=on&cb_00065=on&format=rdb&site_no=${station_id}&period=${period}";

    private static final String URL_FLOW_DATE_DV =
	"https://waterservices.usgs.gov/nwis/dv/?format=rdb&site=${station_id}&startDT=${startDate}&endDT=${endDate}&parameterCd=00060,00065";
    private static final String URL_FLOW_DATE_IV =
	"https://waterservices.usgs.gov/nwis/iv/?format=rdb&site=${station_id}&startDT=${startDate}&endDT=${endDate}&parameterCd=00060,00065";    

    private static final String URL_PEAK =
	"https://nwis.waterdata.usgs.gov/nwis/peak?site_no=${station_id}&agency_cd=USGS&format=rdb";

    public UsgsGaugeTypeHandler(Repository repository, Element node)
	throws Exception {
        super(repository, node);
	/*
	  Don't do this all the time. This is just here to fix the extra spaces in the
	  fields on ramadda.org
	  Misc.runInABit(5000,new Runnable() {public void run() {doCleanup();}});
	*/
    }

    private void doCleanup() {
	System.err.println("USGS: cleanup");
	/*
	  TYPE_USGS_GAUGE
	  ID (VARCHAR 200)
	  STATION_ID (VARCHAR 200)
	  PERIOD (INTEGER 10)
	  STATE (VARCHAR 200)
	  HUC (VARCHAR 200)
	  HOMEPAGE (VARCHAR 200)
	  COUNTY (VARCHAR 200)
	*/

	try {
	    Request request = getRepository().getAdminRequest();
	    String table = "TYPE_USGS_GAUGE";
            Statement statement =
                getDatabaseManager().select("ID,STATE,HUC,COUNTY",
					    table,
                                            new org.ramadda.util.sql.Clause[] {});
            ResultSet  results = statement.getResultSet();

            while (results.next()) {
                String id  = results.getString(1);
                String state = results.getString(2);
                String huc = results.getString(3);		
                String county = results.getString(4);
		getDatabaseManager().update(table, "ID",id, new String[]{"STATE","HUC","COUNTY"},
					    new Object[]{state!=null?state.trim():"",
							 huc!=null?huc.trim():"",
							 county!=null?county.trim():""});
		System.err.println("usgs:" + id +" state:" + state +" huc:" + huc +" county:" + county);
		Entry entry = getEntryManager().getEntry(request,id);
		if(entry==null) {
		    System.err.println("usgs: could not find entry:" + id);
		    continue;
		}
		//Force the reindex
		List<Entry> entries = new ArrayList<Entry>();
		entries.add(entry);
		getSearchManager().entriesModified(request,  entries);
            }
            getDatabaseManager().closeAndReleaseConnection(statement);
	}catch(Exception exc) {
	    exc.printStackTrace();
	}

    }

    public static boolean useDailyValue(Request request, Entry entry) {
	return Misc.equals(entry.getStringValue(request,"use_daily_value",""),"true");
    }

    public static boolean isPeakFlow(Entry entry) {
	return entry.getTypeHandler().isType("type_usgs_gauge_peak");
    }

    public static boolean useDateRange(Request request, Entry entry) {
	return Misc.equals(entry.getStringValue(request,"use_date_range",null),"true");
    }

    @Override
    public RecordFile doMakeRecordFile(Request request, Entry entry,
                                       Hashtable properties,
                                       Hashtable requestProperties)
	throws Exception {
	if(!isPeakFlow(entry))  {
	    String format= "yyyy-MM-dd HH:mm";
	    if(!useDateRange(request,entry)) {
	    } else if(useDailyValue(request, entry)) {
		format = "yyyy-MM-dd";
	    }  
	    for(int i=0;i<CSV_COMMANDS.length;i++) {
		String command = CSV_COMMANDS[i];
		command = command.replace("${format}",format);
		properties.put("csvcommands" + (i+1),command);
		//		System.err.println(command);
	    }
	}
        return new UsgsGaugeRecordFile(request, entry, getPathForRecordEntry(request,entry,requestProperties), properties);
    }

    public static class UsgsGaugeRecordFile extends CsvFile {
	Request request;
	Entry entry;

        public UsgsGaugeRecordFile(Request request,Entry entry,IO.Path path, Hashtable properties)
	    throws IOException {
            super(path, properties);
	    this.request= request;
	    this.entry = entry;
        }

	@Override
        public boolean isMissingValue(BaseRecord record, RecordField field,
                                      String s) {
	    s = s.toLowerCase();
            if (s.equals("dis") ||
		s.equals("ice") || s.equals("ssn") || s.equals("eqp") || s.equals("rat")) {
                return true;
            }
	    if(s.equals("***")) {
		return true;
	    }

            return super.isMissingValue(record, field, s);
        }

    }

    @Override
    public String getPathForEntry(Request request, Entry entry, boolean forRead)
	throws Exception {
	if(entry.isFile()) {
	    return entry.getResource().getPath();
	}
        String url = URL_FLOW;
	if(isPeakFlow(entry)) {
	    url = URL_PEAK;
	} else {
	    if(useDateRange(request,entry)) {
		url =  useDailyValue(request, entry)?URL_FLOW_DATE_DV:URL_FLOW_DATE_IV;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Date start = DateHandler.checkDate(new Date(entry.getStartDate()));
		Date end = DateHandler.checkDate(new Date(entry.getEndDate()));		
		if(start==null || end==null) {
		    throw new IllegalStateException("The entry: " + entry +" does not have a date range set");
		}
		url = url.replace("${startDate}", sdf.format(start));
		url = url.replace("${endDate}", sdf.format(end));		
	    } else {
		url = url.replace("${period}", ("" + entry.getValue(request,"period")).trim());
	    }
	}
	url = url.replace("${station_id}",("" + entry.getValue(request,"station_id")).trim());
	//	System.err.println("URL: "+ url);
        return url;
    }

    @Override
    public void initializeNewEntry(Request request, Entry entry,NewType newType)
	throws Exception {
	if(!isNew(newType)) return;
	initializeNewEntryInner(request, entry);
	String  bulkFile = request.getUploadedFile(ARG_BULKUPLOAD,true);
	if(!stringDefined(bulkFile) || !new File(bulkFile).exists()) return;
	HashSet<String> seen = new HashSet<String>();
	List<Entry> entries = handleBulkUpload(request, entry.getParentEntry(),bulkFile,"station_id",seen,"^\\d+$",null);
	int cnt=0;
	for(Entry newEntry: entries) {
	    cnt++;
	    System.err.println("UsgsGaugeTypeHandler: bulk entry: #" + cnt+" station:"+ newEntry.getValue(request,"station_id"));
	    initializeNewEntryInner(request,newEntry);
	}
	getEntryManager().insertEntriesIntoDatabase(request,  entries,true, true);
    }

    private    void initializeNewEntryInner(Request request, Entry entry)
	throws Exception {
	String id = ("" + entry.getValue(request,"station_id")).trim();
	if(!stringDefined(id)) return;
	String url = "https://waterdata.usgs.gov/nwis/inventory?site_no="+id;
	if(!id.startsWith("USGS-")) id = "USGS-" + id;
	String metadataUrl = "https://api.waterdata.usgs.gov/ogcapi/v0/collections/monitoring-locations/items?f=json&id=" + id;
	//	System.err.println(url);
	IO.Result result = IO.doGetResult(new URL(metadataUrl));
	if(result.getError()) {
	    getLogManager().logSpecial("Failed to read USGS station:" + metadataUrl);
	    return;
	}	    

	if(request.get(ARG_DOWNLOAD_FILE, false)) {
	    URL trendUrl = new URL(getPathForEntry(request,  entry, true));
	    downloadUrlAndSaveAsEntryFile(request, entry, trendUrl,id+"_usgs.dat");
	}

	String json = result.getResult();
	JSONObject root = new JSONObject(json);
	JSONArray features = root.optJSONArray("features");
	if(features==null || features.length()==0) {
	    getLogManager().logSpecial("No features in site metadata:" + metadataUrl);
	    return;
	}
	JSONObject feature = features.getJSONObject(0);
	JSONObject props = feature.getJSONObject("properties");
	JSONObject geo = feature.getJSONObject("geometry");		
	JSONArray coords = geo.optJSONArray("coordinates");
	if(coords!=null && coords.length()>0) {
	    entry.setLongitude(coords.getDouble(0));
	    entry.setLatitude(coords.getDouble(1));	    
	}

	entry.setValue("homepage",url);
	entry.setValue("drainage_area",new Double(props.optDouble("drainage_area",Double.NaN)));
	entry.setValue("contributing_drainage_area",new Double(props.optDouble("contributing_drainage_area",Double.NaN)));	
	entry.setAltitude(props.optDouble("altitude",Double.NaN));
	entry.setName(props.optString("monitoring_location_name",""));
	entry.setValue("site_type",props.optString("site_type",""));
	entry.setValue("huc",props.optString("hydrologic_unit_code",""));
	entry.setValue("county",props.optString("county_name",""));
	entry.setValue("state",props.optString("state_name",""));	
	entry.setValue("gage_datum",props.optString("vertical_datum",""));	
    }

    @Override
    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props)
            throws Exception {
	if(!tag.equals("usgs_gauge_charts")) {
            return super.getWikiInclude(wikiUtil, request, originalEntry,
                                        entry, tag, props);
        }
	String fields  = entry.getStringValue(request,"chart_fields","discharge,total_volume,gauge_height");
	fields = Column.escapeComma(fields);
	StringBuilder wiki = new StringBuilder("\n");
	for(String line:Utils.split(fields,",",true,true)) {
	    line = Column.unescapeComma(line);
	    List<String> fieldList = new ArrayList<String>();
	    StringBuilder args = new StringBuilder();
	    for(String tok:Utils.split(line,",",true,true)) {
		if(tok.startsWith("height:")) {
		    args.append(" height=\"" +tok.substring("height:".length()) +"\" ");
		} else if(tok.startsWith("fixed:")) {
		    List<String> subToks = Utils.split(tok,":");
		    if(subToks.size()>2) {
			fieldList.add(subToks.get(1));
			args.append(" convertData=\"addFixed(id=" + subToks.get(1)+", value=" + subToks.get(2)+");\" ");
		    }
		} else {
		    fieldList.add(tok);
		}
	    }

	    wiki.append("{{display_linechart max=30000 showMenu=true " + args+" fields=\"" + Utils.join(fieldList,",")+"\"}}\n");
	}
	return getWikiManager().wikifyEntry(request, entry,wiki.toString());
    }

}
