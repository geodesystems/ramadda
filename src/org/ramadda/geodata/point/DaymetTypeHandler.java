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
import org.ramadda.util.seesv.Seesv;

import org.w3c.dom.*;

import java.io.*;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.GregorianCalendar;

import java.util.Hashtable;

public class DaymetTypeHandler extends PointTypeHandler {
    private SimpleDateFormat dateSDF;
    private static int IDX = PointTypeHandler.IDX_LAST + 1;
    private static int IDX_STRIDE = IDX++;

    public DaymetTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }

    @Override
    public RecordFile doMakeRecordFile(Request request, Entry entry,
                                       Hashtable properties,
                                       Hashtable requestProperties)
            throws Exception {
        return new DaymetRecordFile(request,getRepository(), entry,getLocation(request,entry),
                                    new IO.Path(getPathForEntry(request, entry,true)));
    }

    private static final String URL_TEMPLATE =
        "https://daymet.ornl.gov/single-pixel/api/data?lat=${latitude}&lon=${longitude}&vars=prcp,srad,swe,tmax,tmin,vp&start=${start}&end=${end}";

    private String[] getLocation(Request request,Entry entry) {
	String lat = request.getString("latitude",null);
	String lon = request.getString("longitude",null);
	if(!stringDefined(lat) || lat.indexOf("$")>=0) lat= request.getString("defaultLatitude");
	if(!stringDefined(lon) || lon.indexOf("$")>=0) lon= request.getString("defaultLongitude");	
	if(!stringDefined(lat)) lat= "" + entry.getLatitude(request);
	if(!stringDefined(lon)) lon= "" + entry.getLongitude(request);
	return new String[]{lat,lon};
    }

    @Override
    public String getPathForEntry(Request request, Entry entry, boolean forRead)
            throws Exception {
        String url = URL_TEMPLATE;
	String[] loc = getLocation(request,entry);
	url = url.replace("${latitude}", loc[0]);
	url = url.replace("${longitude}", loc[1]);

        Date              now = new Date();
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(now);
        if (dateSDF == null) {
            dateSDF = RepositoryUtil.makeDateFormat("yyyy-MM-dd");
        }
        String startDate = "1980-01-01";
        String endDate   = Utils.format(dateSDF,cal.getTime());
        if (entry.getStartDate() < entry.getEndDate()) {
	    startDate = Utils.format(dateSDF,new Date(entry.getStartDate()));
	    endDate   = Utils.format(dateSDF,new Date(entry.getEndDate()));
        }
        url = url.replace("${start}", startDate);
        url = url.replace("${end}", endDate);
        return url;
    }

    public static class DaymetRecordFile extends CsvFile {
	Request request;
        Repository repository;
        Entry entry;
	String[]loc;

        public DaymetRecordFile(Request request, Repository repository, Entry entry,
				String[]loc,
                                IO.Path path)
                throws IOException {
            super(path);
	    this.request=request;
            this.repository = repository;
            this.entry      = entry;
	    this.loc = loc;
        }

        @Override
        public InputStream doMakeInputStream(boolean buffered)
                throws Exception {
	    int                   stride = entry.getIntValue(request,IDX_STRIDE, 7);
	    //The header is taken care of below
	    String latlon = loc[0]+","+loc[1];
	    String[]              args   = new String[] {
		"-skip", "8", "-decimate", "0", "" + stride, "-change",
		"0", "\\.0$", "", "-change", "1", "\\.0$", "", "-combine",
		"0,1", "-", "", "-scale", "3", "0", "0.0393700787", "0",
		"-format", "3", "#0.00", 
		"-columns", "8,2-7",
		"-add",latlon,latlon,
		"-print"
	    };
	    //	    System.err.println("daymet start");
	    long t1 = System.currentTimeMillis();
	    InputStream is =  applySeesv(entry,args);
	    long t2 = System.currentTimeMillis();
	    //	    Utils.printTimes("daymet:",t1,t2);
	    return is;
        }

        public VisitInfo prepareToVisit(VisitInfo visitInfo)
                throws Exception {
            super.prepareToVisit(visitInfo);
            //            utc,co ug/m^3,no2 ug/m^3,o3 ug/m^3,pm10 ug/m^3,so2 ug/m^3
            putFields(new String[] {
                makeField(FIELD_DATE, attrType("date"), attrFormat("yyyy-D")),
		//                makeField("day_length", attrType("double"), attrChartable(), attrUnit("seconds"), attrLabel("Day Length")),
                makeField("precipitation", attrType("double"), attrChartable(), attrUnit("inches"),
                          attrLabel("Precipitation")),
                makeField("srad", attrType("double"), attrChartable(), attrUnit("W/m^2"),
                          attrLabel("Shortwave Radiation")),
                makeField("swe", attrType("double"), attrChartable(), attrUnit("kg/m^2"),
                          attrLabel("Snow Water Equivalent")),
                makeField("tmax", attrType("double"), attrChartable(), attrUnit("degrees C"),
                          attrLabel("Max Temperature")),
                makeField("tmin", attrType("double"), attrChartable(), attrUnit("degrees C"),
                          attrLabel("Min Temperature")),
                makeField("vp", attrType("double"), attrChartable(),attrUnit("Pa"),
			  attrLabel("Pressure")),
                makeField("latitude", attrType("double")),
                makeField("longitude", attrType("double")),		
            });

            return visitInfo;
        }

    }

}
