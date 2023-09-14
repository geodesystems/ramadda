/**
   Copyright (c) 2008-2023 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.point;

import org.ramadda.repository.*;
import org.ramadda.repository.type.*;


import org.ramadda.data.point.text.*;
import org.ramadda.data.record.*;
import org.ramadda.data.services.PointTypeHandler;
import org.ramadda.data.services.RecordTypeHandler;


import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;
import org.ramadda.util.Utils;
import org.ramadda.util.text.Seesv;


import org.json.*;
import org.w3c.dom.*;
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
public class OpenAQTypeHandler extends PointTypeHandler {

        /** _more_ */
    private static int IDX = RecordTypeHandler.IDX_LAST + 1;

    /** _more_ */
    private static int IDX_LOCATION_ID = IDX++;

    /** _more_ */
    private static int IDX_COUNTRY = IDX++;

    /** _more_ */
    private static int IDX_CITY = IDX++;

    private static int IDX_SENSOR = IDX++;    

    private static int IDX_STATIONARY = IDX++;    

    /** _more_ */
    private static int IDX_HOURS_OFFSET = IDX++;


    /** _more_ */
    private SimpleDateFormat dateSDF;

    private static final String[]
	SEESV_ARGS = new String[] {
	"-indateformat", "yyyy-MM-dd'T'HH:mm:ss", "",
	"-columns", "utc,parameter,value,latitude,longitude",
	"-unfurl", "parameter", "value", "utc","latitude,longitude",
	"-set","utc", "0","date",
	//normalize the params
	"-changerow","0","*","^pm10$","pm10.0",	
	"-changerow","0","*","^pm1$","pm1.0",
	"-changerow","0","*","^pm.*25$","pm2.5",
	"-sortby","date","up","date",
	"-addheader","date.format yyyy-MM-dd'T'HH:mm:ss",
	"-print"
    };

    /**
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     * @throws Exception _more_
     */
    public OpenAQTypeHandler(Repository repository, Element node)
	throws Exception {
        super(repository, node);
    }


    @Override
    public void initializeNewEntry(Request request, Entry entry,
                                   boolean fromImport)
	throws Exception {
        super.initializeNewEntry(request, entry, fromImport);
        if (fromImport) {
            return;
        }
	String id = (String)entry.getValue(IDX_LOCATION_ID);
	String url = HU.url("https://api.openaq.org/v2/locations/" + id,"limit","100","page","1","offset","0");
	IO.Result result = IO.doGetResult(new URL(url));
	if(result.getError()) {
	    getLogManager().logError("OpenAQTypeHandler: Error reading location:" +url+" error:" + result.getResult());
	    throw new IllegalArgumentException("Error reading location:" + result.getResult());
	}
	JSONObject obj           = new JSONObject(result.getResult());
	JSONObject location = obj.getJSONArray("results").getJSONObject(0);
	JSONObject coords = location.getJSONObject("coordinates");
	
	entry.setName(location.getString("name"));
	entry.setValue(IDX_COUNTRY,location.optString("country",""));
	entry.setValue(IDX_CITY,location.optString("city",""));	
	entry.setValue(IDX_STATIONARY,new Boolean(!location.optBoolean("isMobile",false)));
	entry.setLatitude(coords.getDouble("latitude"));
	entry.setLongitude(coords.getDouble("longitude"));	

	JSONArray manu = location.optJSONArray("manufacturers");	
	if(manu!=null && manu.length()>0) {
	    entry.setValue(IDX_SENSOR,manu.getJSONObject(0).optString("modelName",""));
	}
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
        return new OpenAQRecordFile(new IO.Path(getPathForEntry(request, entry,true)));
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
        String location = entry.getStringValue(IDX_LOCATION_ID, (String) null);
        if ( !Utils.stringDefined(location)) {
            System.err.println("no location");
            return null;
        }
        Date now = new Date();
        Integer hoursOffset = (Integer) entry.getIntValue(IDX_HOURS_OFFSET,
							  Integer.valueOf(24*7));

        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(now);
        if (dateSDF == null) {
            dateSDF = Utils.makeDateFormat("yyyy-MM-dd'T'HH:mm");
        }
        String endDate = dateSDF.format(cal.getTime());
        cal.add(cal.HOUR_OF_DAY, -hoursOffset.intValue());
        String startDate = dateSDF.format(cal.getTime());
	String url  = HU.url("https://api.openaq.org/v2/measurements",
			     "format","csv",
			     "limit","1000",
			     "page","1",
			     "offset","0",
			     "sort","desc",
			     "order_by","datetime",
			     "date_from", startDate,
			     "date_to", endDate,			     
			     "location_id",location);

        return url;
    }

    /**
     * Class description
     *
     *
     * @version        $version$, Sat, Dec 8, '18
     * @author         Enter your name here...
     */
    public static class OpenAQRecordFile extends CsvFile {

        /**
         * _more_
         *
         *
         * @throws IOException _more_
         */
        public OpenAQRecordFile(IO.Path path) throws IOException {
            super(path);
        }



        /**
         * _more_
         *
         * @param buffered _more_
         *
         * @return _more_
         *
         *
         * @throws Exception _more_
         */
        @Override
        public InputStream doMakeInputStream(boolean buffered)
	    throws Exception {
	    final InputStream is = super.doMakeInputStream(buffered);
	    return IO.pipeIt(new IO.PipedThing(){
		    public void run(OutputStream os)  {
			try {
			    Seesv csvUtil = new Seesv(SEESV_ARGS, new BufferedOutputStream(os),null);
			    csvUtil.setInputStream(is);
			    csvUtil.run(null);
			} catch(Exception exc) {
			    System.err.println("Error reading OpenAQ:" + getFilename()+"\nError:" + exc);
			    exc.printStackTrace();
			}
			IO.close(is);
		    }});
	}
    }
}
