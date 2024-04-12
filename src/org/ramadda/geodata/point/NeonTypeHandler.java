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
import org.ramadda.util.WikiUtil;

import ucar.unidata.util.StringUtil;

import org.json.*;
import org.w3c.dom.*;
import java.net.URL;
import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.HashSet;

public class NeonTypeHandler extends BaseNeonTypeHandler {
    private static JSONObject siteInfo;
    private static JSONObject siteImages;    
    private static final String URL_TEMPLATE ="https://data.neonscience.org/api/v0/data/${product_code}/${site_code}/${year}-${month}";

    private static int IDX = PointTypeHandler.IDX_LAST + 1;
    private static int IDX_SITE_CODE = IDX++;
    private static int IDX_PRODUCT_CODE = IDX++;
    private static int IDX_YEAR = IDX++;
    private static int IDX_MONTH = IDX++;        
    private static int IDX_DOMAIN = IDX++;        
    private static int IDX_STATE = IDX++;    
    private static int IDX_SITE_TYPE = IDX++;
    private static int IDX_MAXPOINTS = IDX++;
    private static int IDX_FILEPATTERN = IDX++;            

    public NeonTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }

    @Override
    public void initializeNewEntry(Request request, Entry entry, boolean fromImport)
	throws Exception {
	if(fromImport) return;
	initializeNewEntryInner(request, entry,null);

	String  bulkFile = request.getUploadedFile(ARG_BULKUPLOAD,true);
	if(!stringDefined(bulkFile) || !new File(bulkFile).exists()) return;
	HashSet<String> seen = new HashSet<String>();
	List<Entry> entries = handleBulkUpload(request, entry.getParentEntry(),bulkFile,"sitecode",seen,null,null);
	for(Entry newEntry: entries) {
	    initializeNewEntryInner(request,newEntry,entry);
	}
	getEntryManager().insertEntriesIntoDatabase(request,  entries,true, true);	
    }


    private void initializeNewEntryInner(Request request, Entry entry,Entry base)
	throws Exception {	
	String id = ("" + entry.getValue(IDX_SITE_CODE)).trim();
	if(!stringDefined(id)) return;
	if(siteInfo==null) {
	    siteInfo= new JSONObject(getStorageManager().readUncheckedSystemResource("/org/ramadda/geodata/point/resources/neonsites.json"));
	}
	JSONObject site = siteInfo.optJSONObject(id);
	if(site==null) return;
	//Clone from the base but reset the sitecode
	if(base!=null) {
	    entry.setValues(base.getValues().clone());
	    entry.setValue(IDX_SITE_CODE,id);
	}

    	entry.setLatitude(site.getDouble("latitude"));
    	entry.setLongitude(site.getDouble("longitude"));	
	entry.setValue(IDX_SITE_TYPE,site.getString("siteType"));
	entry.setValue(IDX_DOMAIN,site.getString("domainName"));
	entry.setValue(IDX_STATE,site.getString("stateName"));		
	String product = (String)entry.getValue(IDX_PRODUCT_CODE);

	if(!stringDefined(entry.getName())) {
	    Column siteColumn = findColumn("sitecode");
	    Column productColumn = findColumn("productcode");	    
	    entry.setName(siteColumn.getEnumLabel(id)+"-" + productColumn.getEnumLabel(product));
	}
    }


    /**
     *
     * @param wikiUtil _more_
     * @param request _more_
     * @param originalEntry _more_
     * @param entry _more_
     * @param tag _more_
     * @param props _more_
      * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props)
            throws Exception {
	if(!tag.equals("neon.siteimages")) {
            return super.getWikiInclude(wikiUtil, request, originalEntry,
                                        entry, tag, props);
        }
	if(siteImages==null) {
	    siteImages= new JSONObject(getStorageManager().readUncheckedSystemResource("/org/ramadda/geodata/point/resources/neonimages.json"));
	}	
	String siteId = (String)entry.getValue(IDX_SITE_CODE);
	if(!stringDefined(siteId)) return "";
	String images = siteImages.optString(siteId.trim(),null);
	if(images==null) return "";
	StringBuilder sb = new StringBuilder();
	List<String> toks = Utils.split(images,",",true,true);
	for(int i=0;i<toks.size();i+=2) {
	    sb.append(HU.div(HU.b(HU.center(toks.get(i))),""));
	    sb.append(HU.image(toks.get(i+1),"width","90%"));
	}
	return sb.toString();

    }






	    

    @Override
    public RecordFile doMakeRecordFile(Request request, Entry entry,
                                       Hashtable properties,
                                       Hashtable requestProperties)
            throws Exception {
        return new CsvFile(getPathForRecordEntry(request,entry,requestProperties), properties);
    }


    @Override
    public String getPathForEntry(Request request, Entry entry, boolean forRead)
            throws Exception {
	if(entry.isFile()) {
	    return entry.getResource().getPath();
	}
	return  getPathForEntry(request, entry,
				(String)entry.getValue(IDX_SITE_CODE),
				(String)entry.getValue(IDX_PRODUCT_CODE),
				(String)entry.getValue(IDX_YEAR),
				(String)entry.getValue(IDX_MONTH),
				(String)entry.getValue(IDX_FILEPATTERN));				
    }

    public Result processEntryAction(Request request, Entry entry)
            throws Exception {
        String action = request.getString("action", "");
        if (!action.equals("getfile")) {
            return super.processEntryAction(request, entry);
	}
	String url = getPathForEntry(request,  entry,true);
	return new Result(url);
    }



}
