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

package org.ramadda.geodata.lidar.geotiff;


import org.ramadda.data.record.*;

import ucar.unidata.gis.epsg.CoordinateOperationMethod;
import ucar.unidata.gis.epsg.CoordinateOperationParameter;
import ucar.unidata.gis.epsg.Pcs;
import ucar.unidata.gis.geotiff.GeoKeys;

import java.io.*;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;



/**
 * Class description
 *
 *
 * @version        $version$, Sat, Mar 2, '19
 * @author         Enter your name here...    
 */
public class GeoKeyDirectory {

    /** _more_          */
    public static final int NOVALUE = -1;

    /** _more_          */
    public static final boolean BIGENDIAN = false;

    /** _more_          */
    private double[] doubles;

    /** _more_          */
    private List<GeoKey> keys = new ArrayList<GeoKey>();

    /** _more_          */
    private Hashtable<Integer, GeoKey> keyMap = new Hashtable<Integer,
                                                    GeoKey>();

    /**
     * _more_
     *
     * @param geoKeyDirectory _more_
     * @param geoKeyAsciiParams _more_
     * @param geoKeyDoubleParams _more_
     *
     * @throws Exception _more_
     */
    public GeoKeyDirectory(byte[] geoKeyDirectory, byte[] geoKeyAsciiParams,
                           byte[] geoKeyDoubleParams)
            throws Exception {
        InputStream  bis      = new ByteArrayInputStream(geoKeyDirectory);
        RecordIO     recordIO = new RecordIO(bis);
        GeoKeyHeader header   = new GeoKeyHeader(null, BIGENDIAN);
        header.read(recordIO);
        //        header.print();
        int numDoubles = ((geoKeyDoubleParams == null)
                          ? 0
                          : geoKeyDoubleParams.length / 8);
        doubles = new double[numDoubles];
        if (numDoubles > 0) {
            GeoKeyDouble geoKeyDouble = new GeoKeyDouble(null, BIGENDIAN);
            InputStream  dbis = new ByteArrayInputStream(geoKeyDoubleParams);
            RecordIO     dIO          = new RecordIO(dbis);
            for (int i = 0; i < numDoubles; i++) {
                geoKeyDouble.read(dIO);
                doubles[i] = geoKeyDouble.getValue();
            }
        }

        for (int i = 0; i < header.getNumberOfKeys(); i++) {
            GeoKey key = new GeoKey(null, BIGENDIAN, geoKeyAsciiParams,
                                    doubles);
            keys.add(key);
            key.read(recordIO);
            keyMap.put(key.getKeyId(), key);
            //            key.print();
            //            System.err.println("   string:" +key.getStringValue());
        }

    }

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public void printKeys() throws Exception {
        for (GeoKey key : keys) {
            StringBuffer sb = new StringBuffer();
            key.print(sb);
            System.err.println(sb);
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getModelType() {
        return getGeoKey(GeoKeys.Geokey.GTModelTypeGeoKey,
                         GeoKeys.ModelType.Projected);
    }

    /**
     * Get the projected CoordinateSystem type
     * @return  the projected CoordinateSystem type
     */
    public int getProjectedCSType() {
        return getGeoKey(GeoKeys.Geokey.ProjectedCSTypeGeoKey, NOVALUE);
    }

    /**
     * _more_
     *
     * @param key _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public int getGeoKey(int key, int dflt) {
        GeoKey geoKey = keyMap.get(key);
        if (geoKey == null) {
            return dflt;
        }

        return (int) geoKey.getValue();
    }


    /**
     * _more_
     *
     * @param key _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public double getGeoKey(int key, double dflt) {
        GeoKey geoKey = keyMap.get(key);
        if (geoKey == null) {
            return dflt;
        }

        return geoKey.getValue();
    }


    /**
     * _more_
     *
     * @param key _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public String getGeoKey(int key, String dflt) {
        GeoKey geoKey = keyMap.get(key);
        if (geoKey == null) {
            return dflt;
        }

        return geoKey.getStringValue();
    }




}
