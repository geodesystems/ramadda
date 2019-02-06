/*
* Copyright (c) 2008-2019 Geode Systems LLC
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
*     http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.ramadda.geodata.cdmdata;


import org.ramadda.data.point.PointFile;
import org.ramadda.data.point.text.CsvFile;
import org.ramadda.data.record.*;
import org.ramadda.data.services.PointTypeHandler;
import org.ramadda.data.services.RecordTypeHandler;
import org.ramadda.repository.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.Utils;
import org.ramadda.util.text.CsvUtil;

import org.w3c.dom.*;

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
        //${date:now}time_start=${format:yyyy-MM-dd}&${date:+10 days}time_end=${format:yyyy-MM-dd}

        String timeStartPattern = "time_start=([^&]+)&";
        toks = Utils.findPatterns(url, timeStartPattern);
        if (toks == null) {
            throw new IllegalArgumentException("Could not find time_start:"
                    + url);
        }
        String timeStart = toks[0];
        url = url.replaceAll(
            timeStartPattern,
            "\\${date:now}time_start=\\${format:yyyy-MM-dd}&");

        String timeEndPattern = "time_end=([^&]+)&";
        toks = Utils.findPatterns(url, timeEndPattern);
        if (toks == null) {
            throw new IllegalArgumentException("Could not find time_end:"
                    + url);
        }
        String timeEnd = toks[0];
        url = url.replaceAll(
            timeEndPattern,
            "\\${date:+\\${endTimeOffset}}time_end=\\${format:yyyy-MM-dd}&");

        if (loc != null) {
            loc[0] = Double.parseDouble(lat);
            loc[1] = Double.parseDouble(lon);
        }
        timeStart = timeStart.replace("%3A", ":");
        timeEnd   = timeEnd.replace("%3A", ":");

        //        System.err.println("lat:" + lat +" lon:" + lon);
        //        System.err.println("vars:" + vars);
        //        System.err.println("url:" + url);
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

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    public void initializeNewEntry(Request request, Entry entry)
            throws Exception {
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
            "fields=time[type=date format=\"yyyy-MM-dd'T'HH:mm:ss\"],latitude[unit=\"degrees_north\"],longitude[unit=\"degrees_east\"]");
        if (addVertCoord) {
            properties.append(",vertCoord[type=string unit=\""
                              + vertCoordUnit + "\"]");
        }

        for (int i = 0; i < vars.size(); i++) {
            properties.append(",");
            String extra   = "";
            String origVar = vars.get(i);
            String unit    = units.get(origVar);
            String var     = GridPointOutputHandler.getAlias(origVar);
            String label   = Utils.makeLabel(origVar);
            label = GridPointOutputHandler.getProperty(origVar + ".label",
                    GridPointOutputHandler.getProperty(origVar + ".label",
                        label));
            if (unit != null) {
                String prefix = "unit." + unit + ".";
                String scale = GridPointOutputHandler.getProperty(prefix
                                   + "scale", null);
                String offset1 = GridPointOutputHandler.getProperty(prefix
                                     + "offset1", null);
                String offset2 = GridPointOutputHandler.getProperty(prefix
                                     + "offset2", null);
                unit = GridPointOutputHandler.getProperty(prefix + "unit",
                        unit);
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
            properties.append(var + "[label=\"" + label + "\"  " + extra
                              + "]");
        }
        properties.append("\n");
        entry.setLocation(loc[0], loc[1]);
        entry.setValue(IDX_PROPERTIES, properties.toString());
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
    public String getPathForRecordEntry(Entry entry,
                                        Hashtable requestProperties)
            throws Exception {
        String url = entry.getResource().getPath();
        //subst the times
        url = url.replace("${endTimeOffset}",
                          (String) entry.getValue(IDX_END_TIME_OFFSET,
                              "+10 days"));
        url = Utils.normalizeTemplateUrl(url);
        //subst the location
        url = super.getPathForRecordEntry(entry, url, requestProperties);

        return url;
    }

    /**
     * _more_
     *
     * @param entry _more_
     * @param pointProps _more_
     * @param requestProperties _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public RecordFile doMakeRecordFile(Entry entry, Hashtable pointProps,
                                       Hashtable requestProperties)
            throws Exception {
        Hashtable props = getRecordProperties(entry);

        return new CsvFile(getPathForRecordEntry(entry, requestProperties),
                           props);
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
