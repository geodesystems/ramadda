/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.cdmdata;


import org.ramadda.data.util.CdmUtil;
import org.ramadda.repository.Entry;
import org.ramadda.repository.Repository;
import org.ramadda.repository.Request;
import org.ramadda.util.Utils;

import org.w3c.dom.Element;

import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.units.DateUnit;

import ucar.unidata.util.Misc;

import java.io.File;

import java.util.Date;
import java.util.Hashtable;
import java.util.List;


/**
 * Created with IntelliJ IDEA.
 * User: opendap
 * Date: 5/8/13
 * Time: 6:14 PM
 * To change this template use File | Settings | File Templates.
 */
public class DoradeRadarTypeHandler extends RadarTypeHandler {

    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public DoradeRadarTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }


    @Override
    public void initializeNewEntry(Request request, Entry entry,  boolean fromImport)
            throws Exception {
        Object[]   values = entry.getTypeHandler().getEntryValues(entry);
        File       f      = entry.getFile();
        NetcdfFile ncf    = NetcdfFile.open(f.toString());
        float[]    elev, aziv, disv, lonv, altv, latv;
        double[]   timev;
        //Merge all of the attributes into one map
        Hashtable<String, String> attrMap = new Hashtable<String, String>();
        List<Attribute>           attrs   = ncf.getGlobalAttributes();
        for (Attribute attr : attrs) {
            String name  = attr.getFullName();
            String value = attr.getStringValue();
            if (value == null) {
                value = "" + attr.getNumericValue();
            }
            attrMap.put(name, value);
        }

        elev = (float[]) ncf.findVariable("elevation").read().get1DJavaArray(
            Float.TYPE);
        aziv = (float[]) ncf.findVariable("azimuth").read().get1DJavaArray(
            Float.TYPE);
        altv = (float[]) ncf.findVariable(
            "altitudes_1").read().get1DJavaArray(Float.TYPE);
        lonv = (float[]) ncf.findVariable(
            "longitudes_1").read().get1DJavaArray(Float.TYPE);
        latv = (float[]) ncf.findVariable(
            "latitudes_1").read().get1DJavaArray(Float.TYPE);
        disv = (float[]) ncf.findVariable("distance_1").read().get1DJavaArray(
            Float.TYPE);
        timev = (double[]) ncf.findVariable(
            "rays_time").read().get1DJavaArray(Double.TYPE);

        String stationId   = Misc.getProperty(attrMap, "radar_name",
                                 "Dorade");
        String stationName = "Dorade Radar";
        double radarLat    = latv[0];
        double radarLon    = lonv[0];
        double altitude    = elev[0];

        String product = Misc.getProperty(attrMap,
                                          CdmUtil.ATTR_KEYWORDS_VOCABULARY,
                                          "");

        Date startDate = new Date((long) timev[0]);

        float latMin = Float.parseFloat(Misc.getProperty(attrMap,
                           CdmUtil.ATTR_MINLAT, "0.0f"));
        float latMax = Float.parseFloat(Misc.getProperty(attrMap,
                           CdmUtil.ATTR_MAXLAT, "0.0f"));
        float lonMin = Float.parseFloat(Misc.getProperty(attrMap,
                           CdmUtil.ATTR_MINLON, "0.0f"));
        float lonMax = Float.parseFloat(Misc.getProperty(attrMap,
                           CdmUtil.ATTR_MAXLON, "0.0f"));

        //Flip them
        if (lonMin > lonMax) {
            float tmp = lonMin;
            lonMin = lonMax;
            lonMax = tmp;
        }



        if ( !Utils.stringDefined(entry.getDescription())) {
            entry.setDescription(Misc.getProperty(attrMap,
                    CdmUtil.ATTR_SUMMARY, ""));
        }

        values[IDX_STATION_ID]      = stationId;
        values[IDX_STATION_NAME]    = stationName;
        values[IDX_STATION_LAT]     = radarLat;
        values[IDX_STATION_LON]     = radarLon;
        values[IDX_STATION_PRODUCT] = product;

        entry.setStartDate(startDate.getTime());
        entry.setEndDate(startDate.getTime());
        entry.setAltitude(altitude);
        entry.setSouth(latMin);
        entry.setNorth(latMax);
        entry.setEast(lonMax);
        entry.setWest(lonMin);

    }

}
