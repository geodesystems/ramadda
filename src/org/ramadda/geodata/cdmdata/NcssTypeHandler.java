/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.cdmdata;


import org.ramadda.data.point.PointFile;
import org.ramadda.data.point.text.CsvFile;
import org.ramadda.data.record.*;
import org.ramadda.data.services.PointTypeHandler;
import org.ramadda.data.services.RecordTypeHandler;
import org.ramadda.repository.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;
import org.ramadda.util.Utils;

import org.w3c.dom.*;

import ucar.unidata.util.StringUtil;

import java.io.*;

import java.text.SimpleDateFormat;

import java.util.ArrayList;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;


/**
 */
public class NcssTypeHandler extends PointTypeHandler {


    /** _more_ */
    private static int IDX = PointTypeHandler.IDX_LAST + 1;

    /** _more_ */
    private static int IDX_END_TIME_OFFSET = IDX++;

    /** _more_ */
    private static int IDX_DEFAULT_FIELDS = IDX++;



    /**
     * _more_
     */
    public NcssTypeHandler() {
        super(null, "", "");
    }




    /**
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     * @throws Exception _more_
     */
    public NcssTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param wikiText _more_
     *
     * @return _more_
     */
    @Override
    public String preProcessWikiText(Request request, Entry entry,
                                     String wikiText) {
        String fields = entry.getStringValue(IDX_DEFAULT_FIELDS, "");
        String chart =
            "{{display type=\"linechart\" showTitle=\"false\" layoutHere=\"false\" #fields=\"tmax,tmin\" }}";
        if ( !Utils.stringDefined(fields)) {
            wikiText = wikiText.replace("${charts}", chart);
        } else {
            String chart2 =
                "{{display type=\"linechart\" showTitle=\"false\" layoutHere=\"false\" fields=\"{field}\" }}";
            StringBuilder charts = new StringBuilder();
            for (String line : StringUtil.split(fields, ",", true, true)) {
                charts.append(chart2.replace("{field}", line));
            }
            wikiText = wikiText.replace("${charts}", charts.toString());
        }

        return wikiText;
    }


    /**
     * _more_
     *
     * @param url _more_
     * @param loc _more_
     * @param vars _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private String parseUrl(String url, double[] loc, List<String> vars)
            throws Exception {
        String   lat = null,
                 lon = null;
        String[] toks;
        String   latPattern = "latitude=([^&]+)&";
        String   lonPattern = "longitude=([^&]+)&";
        toks = Utils.findPatterns(url, latPattern);
        if (toks == null) {
            throw new IllegalArgumentException("Could not find latitude:"
                    + url);
        }
        lat  = toks[0];
        url  = url.replaceAll(latPattern, "latitude=\\${latitude}&");
        toks = Utils.findPatterns(url, lonPattern);
        if (toks == null) {
            throw new IllegalArgumentException("Could not find longitude:"
                    + url);
        }
        lon = toks[0];
        url = url.replaceAll(lonPattern, "longitude=\\${longitude}&");
        if (loc != null) {
            loc[0] = Double.parseDouble(lat);
            loc[1] = Double.parseDouble(lon);
        }


        String rest = url;
        while (true) {
            toks = Utils.findPatterns(rest, "var=([^&]+)&(.*)");
            if (toks == null) {
                break;
            }
            vars.add(toks[0]);
            rest = toks[1];

        }




        //time_start=2019-01-07T00%3A00%3A00Z&time_end=2019-02-22T06%3A00%3A00Z&vertCoord=&accept=csv";
	//latitude=${latitude}&longitude=${longitude}&time_start=${date  format=yyyy-MM-dd}&time_end=${date date=now offset="${endTimeOffset}" format=yyyy-MM-dd}&vertCoord=&accept=csv
        String timeStartPattern = "time_start=([^&]+)&";
        String timeEndPattern   = "time_end=([^&]+)&";
        toks = Utils.findPatterns(url, timeStartPattern);
        if (toks != null) {
            //            throw new IllegalArgumentException("Could not find time_start:"  + url);
            String timeStart = toks[0];
           url = url.replaceAll(
                timeStartPattern,
                "time_start=\\${date format=yyyy-MM-dd}&");

            toks = Utils.findPatterns(url, timeEndPattern);
            if (toks == null) {
                throw new IllegalArgumentException("Could not find time_end:"
                        + url);
            }
            String timeEnd = toks[0];
            url = url.replaceAll(
                timeEndPattern,
                "\\time_end=\\${date offset=\"\\${endTimeOffset}\"}&");

            timeStart = timeStart.replace("%3A", ":");
            timeEnd   = timeEnd.replace("%3A", ":");
        }

        //        System.err.println("lat:" + lat +" lon:" + lon);
        //        System.err.println("vars:" + vars);
        return url;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public boolean okToSetNewNameDefault() {
        return false;
    }

    /**
     * _more_
     *
     * @param path _more_
     *
     * @return _more_
     */
    @Override
    public String getDefaultEntryName(String path) {
        return "";
    }

    
    public void initializeEntryFromForm(Request request, Entry entry,
                                        Entry parent, boolean newEntry)
            throws Exception {
	super.initializeEntryFromForm(request,  entry,parent,newEntry);
	if(newEntry) return;
	checkLatLon(request, entry);
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
    public void initializeNewEntry(Request request, Entry entry,  boolean fromImport)
            throws Exception {
        if (fromImport) {
            return;
        }

	if(!checkLatLon(request, entry)) return;
        super.initializeNewEntry(request, entry, fromImport);
        String url = entry.getResource().getPath();

        if ( !Utils.stringDefined(entry.getName())) {
            String[] toks = Utils.findPatterns(url, "/(.*)/(.*)/[^/]+\\?");
            if (toks == null) {
                System.err.println("Could not extract name from:" + url);
            } else {
                String name = toks[1];
                entry.setName(Utils.makeLabel(name));
            }
        }

        //http://thredds.ucar.edu/thredds/ncss/grib/NCEP/GFS/Global_onedeg/Best?var=Pressure_surface&var=Snow_depth_surface&var=Temperature_surface&var=Visibility_surface&var=Wind_speed_gust_surface&latitude=40&longitude=-105.2&time_start=2019-01-07T00%3A00%3A00Z&time_end=2019-02-22T06%3A00%3A00Z&vertCoord=&accept=csv

        String                    sample        = Utils.readUrl(url);
        int                       index         = sample.indexOf("\n");
        Hashtable<String, String> units = new Hashtable<String, String>();
        boolean                   addVertCoord  = false;
        String                    vertCoordUnit = "m";
        if (index >= 0) {
            String            header = sample.substring(0, index);
            List<RecordField> fields = new CsvFile().doMakeFields(header);
            for (RecordField field : fields) {
                if (field.getName().equals("vertCoord")) {
                    addVertCoord = true;
                    if (Utils.stringDefined(field.getUnit())) {
                        vertCoordUnit = field.getUnit();
                    }

                }
                if (Utils.stringDefined(field.getUnit())) {
                    units.put(field.getName(), field.getUnit());
                }
            }
        }

        List<String> vars = new ArrayList<String>();
        double[]     loc  = new double[] { 0, 0 };
        url = parseUrl(url, loc, vars);
        entry.getResource().setPath(url);


        StringBuilder properties = new StringBuilder("skiplines=1\n");
        properties.append(
            "fields=time[type=date format=\"yyyy-MM-dd'T'HH:mm:ss\"],station[type=string],latitude[unit=\"degrees_north\"],longitude[unit=\"degrees_east\"]");
        if (addVertCoord) {
            vars.add(0, "vertCoord");
            units.put("vertCoord", vertCoordUnit);
            //            properties.append(",vertCoord[type=double unit=\"" + vertCoordUnit + "\"]");
        }

        for (int i = 0; i < vars.size(); i++) {
            properties.append(",");
            String extra   = "";
            String origVar = vars.get(i);
            String unit    = units.get(origVar);
            String alias     = CdmOutputHandler.getAlias(origVar);
	    if(origVar.equals("station")) {
		extra += " type=string ";
	    }
            String label   = Utils.makeLabel(origVar);
            label = CdmOutputHandler.getProperty(alias + ".label",
						 CdmOutputHandler.getProperty(origVar + ".label", label));
            if (unit != null) {
                String prefix = "unit." + unit + ".";
                String scale = CdmOutputHandler.getProperty(prefix + "scale", null);
                String offset1 = CdmOutputHandler.getProperty(prefix + "offset1", null);
                String offset2 = CdmOutputHandler.getProperty(prefix + "offset2", null);
                unit = CdmOutputHandler.getProperty(prefix + "unit", unit);
		if(scale!=null || offset1!=null || offset2!=null) {
		    unit = CdmOutputHandler.getProperty("unit." + unit,unit);
		}
		//		System.err.println("var:" + origVar +" unit:" + unit+" scale:" + scale+ " " + offset1 +" " + offset2);
                if (unit != null) {
                    extra += " unit=\"" + unit + "\"";
                }
                if (scale != null) {
                    extra += " scale=\"" + scale + "\"";
                }
                if (offset1 != null) {
                    extra += " offset1=\"" + offset1 + "\"";
                }
                if (offset2 != null) {
                    extra += " offset2=\"" + offset2 + "\"";
                }
            }
            properties.append(origVar + "[label=\"" + label + "\"  " + extra
                              + "]");
        }
        properties.append("\n");
        entry.setLocation(loc[0], loc[1]);
        entry.setValue(IDX_PROPERTIES, properties.toString());
    }





    private boolean checkLatLon(Request request, Entry entry) {
	//Do this before initializeNewEntry since that sets the lat/lon from the URL
        String url = entry.getResource().getPath();
	if(url.indexOf("${latitude")>0) return false;
	if(entry.hasLocationDefined()) {
	    url = url.replaceAll("latitude=[0-9\\.\\-]+\\&","latitude="+entry.getLatitude()+"&");
	    url = url.replaceAll("longitude=[0-9\\.\\-]+\\&","longitude="+entry.getLongitude()+"&");
	    entry.getResource().setPath(url);
	}
	return true;

    }

    /**
     * _more_
     *
     * @param entry _more_
     * @param requestProperties _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public IO.Path getPathForRecordEntry(Request request, Entry entry,
					 Hashtable requestProperties)
            throws Exception {
        String url = entry.getResource().getPath();
        //subst the times
        url = url.replace("${endTimeOffset}",
                          (String) entry.getStringValue(IDX_END_TIME_OFFSET,
                              "+10 days"));
        //subst the location
        url = super.convertPath(entry, url, requestProperties);
        url = Utils.normalizeTemplateUrl(url);
	return new IO.Path(url);
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
        return getPathForRecordEntry(request, entry, request.getDefinedProperties()).getPath();
    }


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     * @param pointProps _more_
     * @param requestProperties _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public RecordFile doMakeRecordFile(Request request, Entry entry,
                                       Hashtable pointProps,
                                       Hashtable requestProperties)
            throws Exception {
        Hashtable props = getRecordProperties(entry);
        return new CsvFile(getPathForRecordEntry(request, entry, requestProperties), props);
    }


    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        String url =
            "http://thredds.ucar.edu/thredds/ncss/grib/NCEP/GFS/Global_onedeg/Best?var=Pressure_surface&var=Snow_depth_surface&var=Temperature_surface&var=Visibility_surface&var=Wind_speed_gust_surface&latitude=40&longitude=-105.2&time_start=2019-01-07T00%3A00%3A00Z&time_end=2019-02-22T06%3A00%3A00Z&vertCoord=&accept=csv";

        NcssTypeHandler type = new NcssTypeHandler();
        type.parseUrl(url, new double[] { 0, 0 }, new ArrayList<String>());

    }

}
