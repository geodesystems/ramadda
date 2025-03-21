/**
   Copyright (c) 2008-2025 Geode Systems LLC
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
import org.ramadda.util.Utils;
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
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.HashSet;

@SuppressWarnings("unchecked")
public class UsgsGaugeTypeHandler extends PointTypeHandler {

    //agency_cd	site_no	datetime	tz_cd	129346_00060	129346_00060_cd	283298_00065	283298_00065_cd
    private static String CSV_HEADER_DV = "agency\\,station_id\\,date\\,discharge\\,skip1\\,gauge_height\\,skip2";

    //agency_cd	site_no	datetime	tz_cd	129346_00060	129346_00060_cd	283298_00065	283298_00065_cd
    private static String CSV_HEADER_IV = "agency\\,station_id\\,date\\,timezone\\,discharge\\,skip1\\,gauge_height\\,skip2";

    private static String CSV_HEADER_UV = CSV_HEADER_IV;

    private static String[] xxxCSV_COMMANDS = {
	"-tab,-header,${header}",
	"-skip,3,-notcolumns,?skip1\\,?skip2",
	"-change,discharge\\,gauge_height,(?i)(^$|dis|ice|ssn|eqp|rat),0",
	"-integrate,discharge,date,second,volume",
	"-debugrows,10",
	//	"-debugrows,4",
	"-scale,volume,0,0.00002295684,0",
	"-decimals,volume,2",
	"-runningsum,volume,-set,sum_volume,0,total_volume",
	"-addheader, station_id.type string   discharge.type double   gauge_height.type double total_volume.unit {acre feet} total_volume.type double   volume.type double date.format {${format}}"
    };

    private static String[] CSV_COMMANDS = {
	"-tab,-cut,1,-notcolumns,(?i).*cd",
	"-changerow,0,0-10,.*date.*,date",
	"-changerow,0,0-10,.*00065.*,Gauge Height",
	"-changerow,0,0-10,.*00060.*,Discharge",
	"-change,?discharge_comma_?gauge_height,(?i)(^$|dis|ice|ssn|eqp|rat),0,-if",
	"-has,discharge,-integrate,?discharge,date",
	"second,volume,-scale,volume,0",
	"0.00002295684,0,-runningsum,volume,Total volume",
	"-decimals,volume_comma_total_volume,2,-endif,-addheader",
	"site_no.type string discharge.type double gauge_height.type double total_volume.unit {acre feet} total_volume.type double volume.type double date.format {${format}yyyy-MM-dd}"
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

    private static final String FIELDS_IV="agency[label=\"Agency\" type=string],station_id[label=\"Station ID\" type=string],date[type=date format=\"yyyy-MM-dd HH:mm\" label=\"Date\"],timezone[label=\"Timezone\" type=string],discharge[unit=\"cfs\" label=\"Discharge\" type=double],gauge_height[unit=\"feet\" label=\"Gauge Height\" type=double]";

    //agency_cd	site_no	datetime	128646_00060_00003	128646_00060_00003_cd	238320_00065_00003	238320_00065_00003_cd
    //USGS	06447000	2024-04-01	168	A	4.71	A
    private static final String FIELDS_DV="agency[label=\"Agency\" type=string],station_id[label=\"Station ID\" type=string],date[type=date format=\"yyyy-MM-dd\" label=\"Date\"],discharge[unit=\"cfs\" label=\"Discharge\" type=double],gauge_height[unit=\"feet\" label=\"Gauge Height\" type=double]";        

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
	    String header;
	    String format= "yyyy-MM-dd HH:mm";
	    if(!useDateRange(request,entry)) {
		header = CSV_HEADER_UV;
	    } else if(useDailyValue(request, entry)) {
		header =CSV_HEADER_DV;
		format = "yyyy-MM-dd";
	    }  else {
		header = CSV_HEADER_IV;
	    }
	    for(int i=0;i<CSV_COMMANDS.length;i++) {
		String command = CSV_COMMANDS[i];
		command = command.replace("${header}",header);
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

        public boolean isMissingValue(BaseRecord record, RecordField field,
                                      String s) {
	    s = s.toLowerCase();
            if (s.equals("dis") ||
		s.equals("ice") || s.equals("ssn") || s.equals("eqp") || s.equals("rat")) {
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
	System.err.println("URL: "+ url);
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
	IO.Result result = IO.doGetResult(new URL(url));
	if(result.getError()) {
	    getLogManager().logSpecial("Failed to read USGS station:" + url);
	    return;
	}	    
	String html = result.getResult();
	String title = StringUtil.findPattern(html,"<title>(.*?)</title>");
	if(title!=null) {
	    title = title.replaceAll("^USGS ","").replace(id,"").trim();
	    entry.setName(Utils.nameCase(title));
	}

	String siteType= StringUtil.findPattern(html,"<h3>(.*?)</h3>");
	if(siteType!=null) {
	    siteType=siteType.replace("&nbsp;","").trim();
	    entry.setValue("site_type",siteType);
	} 

	String drainageArea = StringUtil.findPattern(html,"(?i)drainage area: *([^ ]+) ");
	if(drainageArea!=null) {
	    drainageArea=drainageArea.replace(",","");
	    try {
		entry.setValue("drainage_area",new Double(drainageArea));
	    } catch(Exception exc) {
		System.err.println("Error parsing drainage area:" + drainageArea);
	    }
	}

	drainageArea = StringUtil.findPattern(html,"(?i)contributing drainage area: *([^ ]+) ");
	if(drainageArea!=null) {
	    drainageArea=drainageArea.replace(",","");
	    try {
		entry.setValue("contributing_drainage_area",new Double(drainageArea));
	    } catch(Exception exc) {
		System.err.println("Error parsing contributing drainage area:" + drainageArea);
	    }
	}
	String block = StringUtil.findPattern(html,"(?s)<div +id=\"stationTable\">(.*?)<table");
	if(block==null) {
	    System.err.println("Failed to read text block from:" + url);
	    return;
	}
	String ll = StringUtil.findPattern(block,"(?s)<dd>(.*?)</dd");
	String lat = null;
	String lon = null;
	if(ll!=null) {
	    //Latitude  38&#176;47'50", &nbsp; Longitude 109&#176;11'40" &nbsp; NAD27<br /></dd>
	    lat = StringUtil.findPattern(ll,"Latitude\\s+([^,]+),");
	    lon = StringUtil.findPattern(ll,"Longitude\\s+([^, ]+) ");	    
	    if(lat!=null && lon!=null) {
		lat = lat.replace("&#176;",":").replace("'",":").replace("\"","").trim();
		lon = "-"+lon.replace("&#176;",":").replace("'",":").replace("\"","").trim();		
		try {
		    entry.setLatitude(GeoUtils.decodeLatLon(lat));
		    entry.setLongitude(GeoUtils.decodeLatLon(lon));
		} catch(Exception exc) {
		    getLogManager().logError("USGS reading lat/lon:" + url, exc);
		}
	    }		
	}

	block = block.replace("\n", " ");
	String huc = StringUtil.findPattern(block,"(?s)Hydrologic\\s+Unit\\s+([^<]+)(<|\n)");
	if(huc!=null) entry.setValue("huc",huc.trim());
	String county = StringUtil.findPattern(block,"(?s)>([^,<>]+)\\s+County");
	if(county!=null) entry.setValue("county",county.trim());
	String state = StringUtil.findPattern(block,"(?s)County,([^,&]+)(,|&)");
	if(state!=null) entry.setValue("state",state.trim());
	entry.setValue("homepage",url);
	//	    Datum of gage: 4,168.32 feet above   NAVD88.
	//<dd>Drainage area: 16,100 square miles</dd><dd>Contributing drainage area: 13,160 square miles,</dd><dd>Datum of gage:  5,115.73 feet above &nbsp; NGVD29.</dd></dl><dl><dt>AVAILABLE DATA:</dt><dd>

	String elev = StringUtil.findPattern(block,"(?s)Datum of gage: +([^ ]+) ");
	if(elev==null)
	    elev = StringUtil.findPattern(block,"Land surface altitude:  (.*?) feet");

	String datum = StringUtil.findPattern(block, "(?i)feet above &nbsp; *([^<\\.]+)(\\.|<)");
	if(datum!=null) entry.setValue("gage_datum", datum);

	if(lat==null || lon == null ||huc==null || county==null || state==null || elev==null) {
	    //	    System.err.println("huc:" + huc +" county:" + county + " state:" + state +" elev:"+ elev + " lat:" + lat+" lon:" + lon +" site type:" + siteType);
	}

	if(stringDefined(elev)) {
	    elev = elev.replace(",","").trim();
	    try {
		entry.setAltitude(Double.parseDouble(elev));
	    } catch(Exception exc) {
		getLogManager().logError("USGS reading elevation:" + url, exc);
	    }
	}

	if(request.get(ARG_DOWNLOAD_FILE, false)) {
	    URL trendUrl = new URL(getPathForEntry(request,  entry, true));
	    downloadUrlAndSaveAsEntryFile(request, entry, trendUrl,id+"_usgs.dat");
	}

    }

}
