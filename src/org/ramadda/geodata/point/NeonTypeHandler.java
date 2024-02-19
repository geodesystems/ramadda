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

import org.json.*;



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
public class NeonTypeHandler extends PointTypeHandler {



    /** _more_ */
    private static int IDX = PointTypeHandler.IDX_LAST + 1;

    /** _more_ */
    private static int IDX_SITE_CODE = IDX++;

    private static int IDX_PRODUCT_CODE = IDX++;
    private static int IDX_YEAR = IDX++;
    private static int IDX_MONTH = IDX++;        



    private static int IDX_DOMAIN = IDX++;        

    private static int IDX_STATE = IDX++;    

    private static int IDX_SITE_TYPE = IDX++;

    private static int IDX_MAXPOINTS = IDX++;
    private static int IDX_FILEPATTERN = IDX++;            

    private static JSONObject siteInfo;


    /**
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     * @throws Exception _more_
     */
    public NeonTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }



    /** _more_ */
    private static final String URL_TEMPLATE ="https://data.neonscience.org/api/v0/data/${product_code}/${site_code}/${year}-${month}";



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
	String id = ("" + entry.getValue(IDX_SITE_CODE)).trim();
	if(!stringDefined(id)) return;
	String product = ("" + entry.getValue(IDX_PRODUCT_CODE)).trim();
	if(siteInfo==null) {
	    siteInfo= new JSONObject(getStorageManager().readUncheckedSystemResource("/org/ramadda/geodata/point/resources/neonsites.json"));
	}

	JSONObject site = siteInfo.getJSONObject(id);
    	entry.setLatitude(site.getDouble("latitude"));
    	entry.setLongitude(site.getDouble("longitude"));	
	entry.setValue(IDX_SITE_TYPE,site.getString("siteType"));
	entry.setValue(IDX_DOMAIN,site.getString("domainName"));
	entry.setValue(IDX_STATE,site.getString("stateName"));		

	if(!stringDefined(entry.getName())) {
	    Column siteColumn = findColumn("sitecode");
	    Column productColumn = findColumn("productcode");	    
	    entry.setName(siteColumn.getEnumLabel(id)+"-" + productColumn.getEnumLabel(product));
	}


    }



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
        return new NeonRecordFile(getPathForRecordEntry(entry,requestProperties), properties);
    }

    /**
     * Class description
     *
     *
     * @version        $version$, Mon, Feb 17, '20
     * @author         Enter your name here...
     */
    public static class NeonRecordFile extends CsvFile {

        /**
         * _more_
         *
         * @param properties _more_
         *
         * @throws IOException _more_
         */
        public NeonRecordFile(IO.Path path, Hashtable properties)
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
	url = url.replace("${site_code}",("" + entry.getValue(IDX_SITE_CODE)).trim());
	url = url.replace("${product_code}",("" + entry.getValue(IDX_PRODUCT_CODE)).trim());
	String year = ("" + entry.getValue(IDX_YEAR)).trim();
	String month = ("" + entry.getValue(IDX_MONTH)).trim();	
	if(year.equals("latest") || month.equals("latest")) {
	    GregorianCalendar cal = new GregorianCalendar();
	    cal.setTime(new Date());
	    year = ""+cal.get(cal.YEAR);
	    month=""+StringUtil.padZero(cal.get(cal.MONTH), 2);
	}
	url = url.replace("${year}",year);
	url = url.replace("${month}",month);
	//	System.err.println(url);
	IO.Result result = IO.doGetResult(new URL(url));
	if(result.getError()) {
	    throw new RuntimeException("Error: reading NEON URL:" + url);
	}
	JSONObject obj = new JSONObject(result.getResult());
	JSONObject data = obj.getJSONObject("data");
	JSONArray files = data.getJSONArray("files");
	String fileUrl = null;
	String pattern = (String)entry.getValue(IDX_FILEPATTERN);
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
	    String error = "Error: could not find data file:" + url;
	    if(names.length()>0)
		error+="\nNames:\n" + names;
	    throw new RuntimeException(error);
	}
	//	System.err.println("neon file url:" +fileUrl);
        return fileUrl;
    }



}
