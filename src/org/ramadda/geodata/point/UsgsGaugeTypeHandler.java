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


/**
 */
public class UsgsGaugeTypeHandler extends PointTypeHandler {



    /** _more_ */
    private static int IDX = PointTypeHandler.IDX_LAST + 1;

    /** _more_ */
    private static int IDX_STATION_ID = IDX++;

    private static int IDX_SITE_TYPE = IDX++;
    /** _more_ */
    private static int IDX_PERIOD = IDX++;
    /** _more_ */
    private static int IDX_COUNTY = IDX++;


    /** _more_ */
    private static int IDX_STATE = IDX++;

    /** _more_ */
    private static int IDX_HUC = IDX++;

    /** _more_ */
    private static int IDX_HOMEPAGE = IDX++;

    /**
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     * @throws Exception _more_
     */
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

    /** _more_ */
    private static final String URL_TEMPLATE =
        "https://waterdata.usgs.gov/nwis/uv?cb_00060=on&cb_00065=on&format=rdb&site_no=${station_id}&period=${period}";


    /**
     * _more_
     *
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
        return new UsgsGaugeRecordFile(getPathForRecordEntry(request,entry,requestProperties), properties);
    }

    /**
     * Class description
     *
     *
     * @version        $version$, Mon, Feb 17, '20
     * @author         Enter your name here...
     */
    public static class UsgsGaugeRecordFile extends CsvFile {

        /**
         * _more_
         *
         * @param properties _more_
         *
         * @throws IOException _more_
         */
        public UsgsGaugeRecordFile(IO.Path path, Hashtable properties)
                throws IOException {
            super(path, properties);
        }

        /**
         * _more_
         *
         * @param record _more_
         * @param field _more_
         * @param s _more_
         *
         * @return _more_
         */
        public boolean isMissingValue(BaseRecord record, RecordField field,
                                      String s) {
	    s = s.toLowerCase();
            if (s.equals("ice") || s.equals("ssn") || s.equals("eqp") || s.equals("rat")) {
                return true;
            }

            return super.isMissingValue(record, field, s);
        }

    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public String getPathForEntry(Request request, Entry entry, boolean forRead)
            throws Exception {
	if(entry.isFile()) {
	    return entry.getResource().getPath();
	}
        String url = URL_TEMPLATE;
	url = url.replace("${station_id}",("" + entry.getValue(IDX_STATION_ID)).trim());
        url = url.replace("${period}", ("" + entry.getValue(IDX_PERIOD)).trim());
        return url;
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
	if(fromImport) return;
	initializeNewEntryInner(request, entry);
	String id = ("" + entry.getValue(IDX_STATION_ID)).trim();
	String  bulkFile = request.getUploadedFile(ARG_BULKUPLOAD,true);
	if(!stringDefined(bulkFile) || !new File(bulkFile).exists()) return;
	HashSet<String> seen = new HashSet<String>();
	List<Entry> entries = handleBulkUpload(request, entry.getParentEntry(),bulkFile,IDX_STATION_ID,seen,"^\\d+$",null);
	for(Entry newEntry: entries) {
	    System.err.println("UsgsGaugeTypeHandler: bulk entry:" + newEntry.getValue(IDX_STATION_ID));
	    initializeNewEntryInner(request,newEntry);
	}
	getEntryManager().insertEntriesIntoDatabase(request,  entries,true, true);
    }

    private    void initializeNewEntryInner(Request request, Entry entry)
	throws Exception {

	String id = ("" + entry.getValue(IDX_STATION_ID)).trim();
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
	    entry.setValue(IDX_SITE_TYPE,siteType);
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
	if(huc!=null) entry.setValue(IDX_HUC,huc.trim());
	String county = StringUtil.findPattern(block,"(?s)>([^,<>]+)\\s+County");
	if(county!=null) entry.setValue(IDX_COUNTY,county.trim());
	String state = StringUtil.findPattern(block,"(?s)County,([^,&]+)(,|&)");
	if(state!=null) entry.setValue(IDX_STATE,state.trim());
	entry.setValue(IDX_HOMEPAGE,url);
	//	    Datum of gage: 4,168.32 feet above   NAVD88.
	//<dd>Drainage area: 16,100 square miles</dd><dd>Contributing drainage area: 13,160 square miles,</dd><dd>Datum of gage:  5,115.73 feet above &nbsp; NGVD29.</dd></dl><dl><dt>AVAILABLE DATA:</dt><dd>

	String elev = StringUtil.findPattern(block,"(?s)Datum of gage: +([^ ]+) ");
	if(elev==null)
	    elev = StringUtil.findPattern(block,"Land surface altitude:  (.*?) feet");


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




    }


}
