/**
Copyright (c) 2008-2024 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.point;

import org.ramadda.data.point.text.*;
import org.ramadda.data.record.*;
import org.ramadda.data.services.PointTypeHandler;
import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.WikiUtil;
import org.ramadda.util.IO;
import org.ramadda.util.Utils;
import ucar.unidata.util.StringUtil;

import org.w3c.dom.*;
import org.json.*;
import java.net.URL;
import java.io.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.List;


public class NoaaTidesTypeHandler extends PointTypeHandler {
    private static final String PRODUCT_WATER_LEVEL="water_level";
    private static final String PRODUCT_HOURLY_HEIGHT="hourly_height";
    private static final String PRODUCT_HIGH_LOW="high_low";        
    private static final String PRODUCT_MONTHLY_MEAN="monthly_mean";
    private static int IDX = PointTypeHandler.IDX_LAST + 1;
    private static int IDX_STATION_ID = IDX++;

    public NoaaTidesTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }


    @Override
    public void initializeNewEntry(Request request, Entry entry,
                                   boolean fromImport)
            throws Exception {
	if(fromImport) return;
	initializeNewEntryInner(request, entry);
	String  bulkFile = request.getUploadedFile(ARG_BULKUPLOAD,true);
	if(!stringDefined(bulkFile) || !new File(bulkFile).exists()) return;
	HashSet<String> seen = new HashSet<String>();
	List<Entry> entries = handleBulkUpload(request, entry.getParentEntry(),bulkFile,"station_id",seen,"^\\d+$",null);
	for(Entry newEntry: entries) {
	    initializeNewEntryInner(request,newEntry);

	}
	getEntryManager().insertEntriesIntoDatabase(request,  entries,true, true);	
    }

    private  void initializeNewEntryInner(Request request, Entry entry)
	throws Exception {	
	String id = (String)  entry.getValue(IDX_STATION_ID);
	if(!stringDefined(id)) {
	    //try to extract it from the file name
	    String resource = entry.getResource().getPath();
	    if(stringDefined(resource)) {
		id = StringUtil.findPattern(resource,"(\\d{7})\\.csv");
		System.err.println(id);

	    }
	}


	if(!stringDefined(id)) return;

	String url = "https://api.tidesandcurrents.noaa.gov/mdapi/prod/webapi/stations/" + id+".json?expand=datums,floodlevels,disclaimers,notices,details,benchmarks&units=english";

	try {
	    //https://api.tidesandcurrents.noaa.gov/mdapi/prod/webapi/stations/8575512.json?expand=datums,floodlevels,disclaimers,notices,details,benchmarks&units=english
	    String json = IO.readUrl(new URL(url));
            JSONObject  obj   = new JSONObject(json);

	    JSONArray stations = obj.getJSONArray("stations");	    
	    JSONObject station = stations.getJSONObject(0);
	    JSONObject flood = station.getJSONObject("floodlevels");
	    JSONArray datums = station.getJSONObject("datums").getJSONArray("datums");	    	    
	    String stationName =station.getString("name");
	    entry.setValue("station_name",stationName);
	    entry.setValue("tidal",station.getBoolean("tidal"));	    
	    entry.setValue("state",station.getString("state"));	    

	    for(int i=0;i<datums.length();i++) {
		JSONObject datum = datums.getJSONObject(i);
		//We don't have all of these but just try them all
		if(datum.getString("name").toLowerCase().matches("(msl|mlw|mhw)")) {
		    entry.setValue("datum_"+datum.getString("name").toLowerCase(),
				   datum.getDouble("value"));
		}
	    }


	    entry.setLatitude(station.optDouble("lat",Double.NaN));
	    entry.setLongitude(station.optDouble("lng",Double.NaN));	    
	    String name = entry.getName().replace(id,"").replace(stationName,"").replace("Monthly Mean","").trim();
	    name = name.replaceAll("^- *","").replaceAll(" - "," ").replaceAll("  +"," ").trim();
	    List<String> names = new ArrayList<String>();
	    Column productColumn = findColumn("product");
	    names.add(stationName);
	    names.add(id);	    
	    if(productColumn !=null) {
		String product = productColumn.formatValue(request,  entry, entry.getValues());
		if(stringDefined(product))
		    names.add(product);
	    }
	    if(entry.getTypeHandler().getType().equals("type_noaa_tides_monthly")) {
		names.add("Monthly Mean");
	    }

	    if(stringDefined(name)) names.add(name);	    
	    entry.setName(Utils.join(names," - "));

	    entry.setValue("flood_stage_minor",new Double(flood.optDouble("nos_minor",Double.NaN)));
	    entry.setValue("flood_stage_moderate",new Double(flood.optDouble("nos_moderate",Double.NaN)));
	    entry.setValue("flood_stage_major",new Double(flood.optDouble("nos_major",Double.NaN)));

	    try {
		String imageUrl = "https://cdn.tidesandcurrents.noaa.gov/assets/stationphotos/" + id+"A.jpg";
		File tmpFile = getStorageManager().getTmpFile(request,"thumbnail.jpg");
		IO.writeFile(new URL(imageUrl),new FileOutputStream(tmpFile));
		String fileName = getStorageManager().copyToEntryDir(entry,
								     tmpFile).getName();
		Metadata thumbnailMetadata =
		    new Metadata(getRepository().getGUID(), entry.getId(),
				 ContentMetadataHandler.TYPE_THUMBNAIL, false,
				 fileName, null, null, null, null);
		
		getMetadataManager().addMetadata(request,entry, thumbnailMetadata);
	    } catch(Exception imageExc){
	    }
	} catch(Exception exc) {
	    getSessionManager().addSessionErrorMessage(request,"Error reading station metadata for station:" +  id +"<br>Error:" +exc.getMessage());
	    System.err.println("Error:" + exc +" url:" + url);
	    exc.printStackTrace();
	}

	//If it is the trend data and there isn't already a file then download the file and add it
	if(entry.getTypeHandler().getType().equals("type_noaa_tides_trend") && !entry.isFile()) {
	    URL trendUrl = new URL("https://tidesandcurrents.noaa.gov/sltrends/data/" + id+"_meantrend.csv");
	    downloadUrlAndSaveAsEntryFile(request, entry, trendUrl,id+"_trends.csv");
	}

    }

    @Override
    public String getPathForEntry(Request request, Entry entry, boolean forRead)
            throws Exception {
	if(entry.isFile()) {
	    return super.getPathForEntry(request,entry,forRead);
	}
	String id = (String)  entry.getValue(IDX_STATION_ID);
	if(!Utils.stringDefined(id)) {
	    throw new IllegalStateException("No station defined for NOAA Tide data:" + entry);
	}

	String product;
	if(entry.getTypeHandler().getType().equals("type_noaa_tides_monthly")) {
	    product=PRODUCT_MONTHLY_MEAN;
	} else {
	    product = (String)entry.getValue("product");
	}
	if(!Utils.stringDefined(product)) {
	    throw new IllegalStateException("No product defined for NOAA Tide data:" + entry);
	}
	SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
	Integer dateOffset=(Integer)entry.getValue("days");
	int days = dateOffset==null?-1:dateOffset.intValue();
	Date startDate = DateHandler.checkDate(new Date(entry.getStartDate()));
	Date endDate = DateHandler.checkDate(new Date(entry.getEndDate()));	

	if(endDate==null) endDate = new Date();
	if(product.equals(PRODUCT_HIGH_LOW)) {
	    if(startDate==null) startDate = new Date(endDate.getTime()-Utils.daysToMillis(days>0?days:364));
	} else 	if(product.equals(PRODUCT_HOURLY_HEIGHT)) {
	    if(startDate==null) startDate = new Date(endDate.getTime()-Utils.daysToMillis(days>0?days:364));
	} else 	if(product.equals(PRODUCT_WATER_LEVEL)) {
	    if(startDate==null) startDate = new Date(endDate.getTime()-Utils.daysToMillis(days>0?days:31));
	} else if(product.equals(PRODUCT_MONTHLY_MEAN)) {
	    if(startDate==null) startDate =
				    new Date(endDate.getTime()-Utils.daysToMillis(days>0?days:365*40));

	}

	String url = HU.url("https://api.tidesandcurrents.noaa.gov/api/prod/datagetter",
			    "application","NOS.COOPS.TAC.WL",
			    "time_zone","GMT",
			    "units","english",
			    "format","csv",
			    "datum",(String)entry.getValue("datum","MLLW"),
			    "station",id,
			    "product",product,
			    "begin_date",sdf.format(startDate),
			    "end_date",sdf.format(endDate));

	return url;
    }

    @Override
    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props)
            throws Exception {
	if(!tag.equals("noaa.tides.download")) {
            return super.getWikiInclude(wikiUtil, request, originalEntry,
                                        entry, tag, props);
        }
	if(entry.isFile()) {
	    return getEntryManager().getEntryResourceUrl(request, entry);
	}
	return  getPathForEntry(request,  entry,false);
    }


}
