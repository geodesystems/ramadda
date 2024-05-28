/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.point;

import org.ramadda.util.IO;
import org.ramadda.util.Utils;



import org.ramadda.data.record.*;
import org.ramadda.data.point.text.*;
import org.ramadda.data.services.*;
import org.ramadda.repository.Entry;
import org.ramadda.repository.Repository;
import org.ramadda.repository.Request;

import java.net.URL;
import java.util.Hashtable;
import java.util.List;

import java.io.*;
import org.w3c.dom.Element;
import org.json.*;


/**
 * TypeHandler for Aviation Weather Center METARS
 * https://www.aviationweather.gov/adds/dataserver
 *
 *
 */
public class NwsObsTypeHandler extends NwsStationTypeHandler {

    /** _more_ */
    public static final String URL =
        "https://api.weather.gov/stations/{station}/observations";


    /** _more_ */
    private static int IDX =
        org.ramadda.data.services.RecordTypeHandler.IDX_LAST + 1;


    /** _more_ */
    public static final int IDX_SITE_ID = IDX++;


    /**
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     *
     * @throws Exception On badnes
     */
    public NwsObsTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }


    @Override
    public void initializeNewEntry(Request request, Entry entry,NewType newType)throws Exception {
        super.initializeNewEntry(request, entry, newType);                                      
	if(!isNew(newType)) return;
	initializeStation(request, entry,  (String) entry.getStringValue(IDX_SITE_ID, ""));
   }



    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception On badnes
     */
    @Override
    public String getPathForEntry(Request request, Entry entry, boolean forRead)
            throws Exception {
        if (entry.isFile()) {
            return super.getPathForEntry(request, entry,forRead);
        }
        String siteId = entry.getStringValue(IDX_SITE_ID, "");
        String url = URL.replace("{station}", siteId);
        return url;
    }

    public RecordFile doMakeRecordFile(Request request, Entry entry,
                                       Hashtable properties,
                                       Hashtable requestProperties)
            throws Exception {
        return new NwsObsRecordFile(request,getRepository(), entry,
				    new IO.Path(getPathForEntry(request, entry,true)));
    }



    /**
     * Class description
     *
     *
     * @version        $version$, Sat, Dec 8, '18
     * @author         Enter your name here...
     */
    public static class NwsObsRecordFile extends CsvFile {

        /** _more_ */
        Repository repository;

	Request request;

        /** _more_ */
        Entry entry;

        /**
         * _more_
         *
         *
         * @param repository _more_
         * @param entry _more_
         * @param filename _more_
         *
         * @throws IOException _more_
         */
        public NwsObsRecordFile(Request request,Repository repository, Entry entry,
				IO.Path path)
                throws IOException {
            super(path);
            this.repository = repository;
	    this.request=request;
            this.entry      = entry;
        }

	public InputStream doMakeInputStream(boolean buffered) throws Exception {
	    final PipedOutputStream pos = new PipedOutputStream();
	    final PipedInputStream  pis = new PipedInputStream(pos);
	    final PrintWriter pw = new PrintWriter(pos);
	    ucar.unidata.util.Misc.run(new Runnable() {
		    public void run()  {
			try {
			    String  json = IO.doGet(new URL(getFilename()), "accept",   "application/geo+json");
			    JSONObject obj   = new JSONObject(json);
			    JSONArray features   = obj.getJSONArray("features");
			    String fields = "timestamp,latitude,longitude,temperature,dewpoint,windDirection,windSpeed,windGust,barometricPressure,seaLevelPressure,visibility,maxTemperatureLast24Hours,minTemperatureLast24Hours,precipitationLast3Hours,precipitationLast6Hours,relativeHumidity,windChill,heatIndex";
			    pw.print(fields);
			    pw.print("\n");
			    List<String> fieldList = Utils.split(fields,",",true,true);
			    String lastTime = "NONE";
			    int rows = 0;
			    //Run through the features in reverse order as it is most recent first
			    for (int i = features.length()-1;i>=0; i--) {
				JSONObject feature = features.getJSONObject(i);
				JSONObject properties= feature.getJSONObject("properties");		
				StringBuilder row = new StringBuilder();
				boolean gotOneGoodOne = false;
				rows++;
				for(int j=0;j<fieldList.size();j++) {
				    if(j>0) row.append(",");
				    String f = fieldList.get(j);
				    if(f.equals("latitude")) {
					row.append(entry.getLatitude(request));
					continue;
				    }
				    if(f.equals("longitude")) {
					row.append(entry.getLongitude(request));
					continue;
				    }		    
				    if(f.equals("timestamp")) {
					String time = properties.getString(f);
					time = time.replace("+00:00","");
					lastTime = time;
					row.append(time);
					continue;
				    }

				    JSONObject fobj = properties.optJSONObject(f);
				    if(fobj==null) {
					row.append("NaN");
					continue;
				    }
				    double d =fobj.optDouble("value"); 
				    if(!Double.isNaN(d)) gotOneGoodOne = true;
				    //convert km/h to m/s
				    if(f.equals("windSpeed") || f.equals("windGust")) {
					d = d*0.277778;
				    }
				    row.append(d);
				}		    
				if(gotOneGoodOne) {
				    pw.print(row);
				    pw.print("\n");
				}
			    }
			    pw.flush();
			    pw.close();
			    //			    System.err.println("***** fetching:" +getFilename() +" last time:" + lastTime +" #rows:" + rows);
			} catch(Exception exc) {
			    System.err.println("Error reading NwsObs:" + getFilename()+"\nError:" + exc);
			    exc.printStackTrace();
			}
		    }});
	    return pis;
	}
    }

}
