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

public class BaseNeonTypeHandler extends PointTypeHandler {
    private static final String URL_TEMPLATE ="https://data.neonscience.org/api/v0/data/${product_code}/${site_code}/${year}-${month}";

    public BaseNeonTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }



    @Override
    public RecordFile doMakeRecordFile(Request request, Entry entry,
                                       Hashtable properties,
                                       Hashtable requestProperties)
            throws Exception {
        return new CsvFile(getPathForRecordEntry(request,entry,requestProperties), properties);
    }


    private static String productCodes;

    @Override
    public Object getWikiProperty(Entry entry, String id)  {
	if(!id.equals("neon.productcodes")) return super.getWikiProperty(entry,id);
	if(productCodes==null) {
	    StringBuilder sb = new StringBuilder();
	    try {
		for(String line:Utils.split(getStorageManager().readUncheckedSystemResource("/org/ramadda/geodata/point/resources/neonproducts.txt"),"\n",true,true)) {
		    if(line.startsWith("#")) continue;
		    if(sb.length()>0) sb.append(",");
		    sb.append(line);
		}
		productCodes = sb.toString();
	    } catch(Exception exc) {
		throw new RuntimeException(exc);
	    }
	}
	return productCodes;
    }

    public String getPathForEntry(Request request, Entry entry, boolean forRead)
            throws Exception {
	return getPathForEntry(request, entry, "ABBY","DP1.00001.001","latest","latest",null);
    }


    public String getPathForEntry(Request request, Entry entry, String site, String product, String year, String month,
				  String pattern) 
            throws Exception {
	if(entry.isFile()) {
	    return entry.getResource().getPath();
	}
	GregorianCalendar cal=null;
	boolean addOffset=true;
	String siteCode = request.getString("sitecode","");
	if(!stringDefined(siteCode)) siteCode = site;
	String productCode =request.getString("productcode","");
	if(!stringDefined(productCode)) productCode = product;

	if(request.defined("date")) {
	    cal = new GregorianCalendar();
	    cal.setTime(getRepository().getDateHandler().parseDate(request.getString("date")));
	} else 	if(year.equals("latest") || month.equals("latest")) {
	    cal = new GregorianCalendar();
	    cal.setTime(new Date());
	    addOffset=false;
	}
	if(cal!=null) {
	    year = ""+cal.get(cal.YEAR);
	    month=""+StringUtil.padZero(cal.get(cal.MONTH)+(addOffset?1:0), 2);
	}

        String url = URL_TEMPLATE;
	url = url.replace("${site_code}",siteCode.trim());
	url = url.replace("${product_code}",productCode.trim());
	url = url.replace("${year}",year);
	url = url.replace("${month}",month);
	String baseUrl = url;
	System.err.println("neon url:" +url);
	String key = getRepository().getProperty("neon.api.key",null);
	if(key!=null) url=HU.url(url,"apiToken",key);

	long t1 = System.currentTimeMillis();
	IO.Result result = IO.doGetResult(new URL(url));
	long t2 = System.currentTimeMillis();
	//	Utils.printTimes("getting manifest",t1,t2);
	if(result.getError()) {
	    throw new RuntimeException("Error: reading NEON URL:" + url);
	}
	JSONObject obj = new JSONObject(result.getResult());
	JSONObject data = obj.getJSONObject("data");
	JSONArray files = data.getJSONArray("files");
	String fileUrl = null;
	if(!stringDefined(pattern)) pattern=null;
	StringBuilder names = new StringBuilder();
	for (int i = 0; fileUrl==null && i < files.length(); i++) {
	    JSONObject file = files.getJSONObject(i);
	    String name = file.getString("name");
	    if(name.indexOf("basic")>=0  || name.indexOf("expanded")>=0) {
		names.append(name);
		names.append("\n");
		if(pattern!=null) {
		    if(name.indexOf(pattern)>=0 || name.matches(pattern)) {
			fileUrl = file.getString("url");	    
		    }
		} else  if(name.indexOf("basic")>=0) {		
		    fileUrl = file.getString("url");	    
		}
	    }
	}
	if(fileUrl==null) {
	    String error = "Error: could not find data file in manifest:\n" + baseUrl;
	    if(names.length()>0)
		error+="\nNames:\n" + names;
	    throw new RuntimeException(error);
	}
	/*
	System.err.println("file url:" +fileUrl);
	long tt1 = System.currentTimeMillis();
	IO.doGetResult(new URL(fileUrl));
	long tt2 = System.currentTimeMillis();
	Utils.printTimes("getting data",tt1,tt2);
	*/


        return fileUrl;
    }



}
