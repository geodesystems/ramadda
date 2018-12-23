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

package org.ramadda.data.services;


import au.gov.bom.aifs.osa.analysis.Barnes;


import org.ramadda.data.point.*;

import org.ramadda.data.record.*;
import org.ramadda.data.record.filter.*;
import org.ramadda.data.services.*;

import org.ramadda.repository.*;
import org.ramadda.util.grid.IdwGrid;
import org.ramadda.util.grid.LatLonGrid;


import ucar.unidata.util.Misc;


import java.awt.*;

import java.awt.geom.Rectangle2D;

import java.io.*;

import java.util.ArrayList;
import java.util.List;




/**
 * A Record visitor that holds the IdwGrid
 *
 *
 */
public class BarnesVisitor extends BridgeRecordVisitor {

    /** the request */
    Request request;

    /** are we gridding another attribute instead of altitude */
    private int valueAttr = -1;

    /** _more_ */
    private List<Integer> divisors;

    /** _more_ */
    private List<String> divisorToks = new ArrayList<String>();

    /** how big an image */
    private int imageHeight;

    /** how big an image */
    private int imageWidth;

    /** _more_ */
    private Rectangle2D.Double bounds;

    /** _more_ */
    private float minLat;

    /** _more_ */
    private float maxLat;

    /** _more_ */
    private float minLon;

    /** _more_ */
    private float maxLon;

    /** _more_ */
    private List<float[]> points;

    /** _more_ */
    private IdwGrid grid;

    /**
     * ctor
     *
     *
     * @param handler the output handler
     * @param request the request
     * @param width _more_
     * @param height _more_
     * @param bounds _more_
     */
    public BarnesVisitor(RecordOutputHandler handler, Request request,
                         int width, int height, Rectangle2D.Double bounds) {
        super(handler);
        this.request     = request;
        this.imageWidth  = width;
        this.imageHeight = height;
        if (request.defined(RecordOutputHandler.ARG_PARAMETER)) {
            valueAttr = request.get(RecordOutputHandler.ARG_PARAMETER, -1);
        }
        divisors = new ArrayList<Integer>();
        for (String tok :
                (List<String>) request.get(RecordOutputHandler.ARG_DIVISOR,
                                           new ArrayList<String>())) {
            divisors.add(new Integer(tok));
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public IdwGrid getGrid() {
        return grid;
    }

    /**
     * _more_
     *
     * @param file _more_
     * @param visitInfo _more_
     * @param record _more_
     *
     * @return _more_
     */
    int cnt = 0;

    /**
     * _more_
     *
     * @param file _more_
     * @param visitInfo _more_
     * @param record _more_
     *
     * @return _more_
     */
    public boolean doVisitRecord(RecordFile file, VisitInfo visitInfo,
                                 Record record) {

        PointRecord pointRecord = (PointRecord) record;
        float       lat         = (float) pointRecord.getLatitude();
        float       lon         = (float) pointRecord.getLongitude();
        if (Float.isNaN(lat) || Float.isNaN(lon)) {
            return true;
        }

        float value;
        if (valueAttr != -1) {
            value = (float) pointRecord.getValue(valueAttr);
        } else {
            value = (float) pointRecord.getAltitude();
        }
        if (divisors.size() > 0) {
            float values = 0;
            for (int i : divisors) {
                values += (float) pointRecord.getValue(i);
            }
            float o = value;
            if (values == 0) {
                value = Float.NaN;
            } else {
                value = value / values;
            }
            //            System.err.println("orig:" + o +" %:" + value +" total:" + values);
        }


        if (points == null) {
            points = new ArrayList<float[]>();
            minLat = maxLat = lat;
            minLon = maxLon = lon;
        } else {
            minLat = Float.isNaN(lat)
                     ? minLat
                     : (float) Math.min(minLat, lat);
            maxLat = Float.isNaN(lat)
                     ? maxLat
                     : (float) Math.max(maxLat, lat);
            minLon = Float.isNaN(lon)
                     ? minLon
                     : (float) Math.min(minLon, lon);
            maxLon = Float.isNaN(lon)
                     ? maxLon
                     : (float) Math.max(maxLon, lon);
        }

        //        System.err.println("point:" + lat +" " + lon +" " + value);
        points.add(new float[] { lon, lat, value });

        return true;
    }


    /**
     * Done. Tell the llg to average its values
     *
     *
     * @throws Exception _more_
     */
    public void finishedWithAllFiles() throws Exception {
        int       numPasses = 1;
        float     gain      = 1.0f;
        float[][] data3D    = new float[3][points.size()];
        int       cnt       = 0;
        for (float[] tuple : points) {
            data3D[0][cnt] = tuple[0];
            data3D[1][cnt] = tuple[1];
            data3D[2][cnt] = tuple[2];
            cnt++;
        }
        Barnes.AnalysisParameters ap =
            Barnes.getRecommendedParameters(minLon, minLat, maxLon, maxLat,
                                            data3D);

        float[] lons = ap.getGridXArray();
        float[] lats = ap.getGridYArray();
        float[][] faaGrid = Barnes.point2grid(lons, lats, data3D,
                                (float) ap.getScaleLengthGU(), gain,
                                numPasses);


        grid = new IdwGrid(faaGrid.length, faaGrid[0].length, maxLat, minLon,
                           minLat, maxLon);
        for (int lon = 0; lon < lons.length; lon++) {
            float[] col = faaGrid[lon];
            for (int lat = 0; lat < lats.length; lat++) {
                grid.setValue(lats[lat], lons[lon], col[lat]);
            }
        }
        //        grid.writeImage("test.png");
    }

}
