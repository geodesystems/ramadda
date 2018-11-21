/*
* Copyright (c) 2008-2018 Geode Systems LLC
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

package org.ramadda.plugins.map;


import org.ramadda.repository.Entry;
import org.ramadda.repository.Repository;
import org.ramadda.repository.Request;
import org.ramadda.repository.map.MapInfo;
import org.ramadda.repository.output.KmlOutputHandler;
import org.ramadda.repository.type.GenericTypeHandler;

import org.w3c.dom.Element;

import ucar.unidata.gis.GisPart;
import ucar.unidata.gis.shapefile.EsriShapefile;
import ucar.unidata.gis.shapefile.ProjFile;


import java.awt.geom.Rectangle2D;

import java.util.ArrayList;
import java.util.List;


/**
 */
public class ShapefileTypeHandler extends GenericTypeHandler {

    public static int  MAX_POINTS = 500000;

    /** _more_ */
    private static final int IDX_LON = 0;

    /** _more_ */
    private static final int IDX_LAT = 1;


    /**
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     * @throws Exception _more_
     */
    public ShapefileTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param parent _more_
     * @param newEntry _more_
     *
     * @throws Exception _more_
     */
    public void initializeEntryFromForm(Request request, Entry entry,
                                        Entry parent, boolean newEntry)
            throws Exception {
        if ( !entry.isFile()) {
            System.err.println("Shapefile not a file");

            return;
        }
        EsriShapefile shapefile = null;
        try {
            shapefile = new EsriShapefile(entry.getFile().toString());
        } catch (Exception exc) {
            System.err.println("Error opening shapefile:" + exc);
            return;
        }

        Rectangle2D bounds   = shapefile.getBoundingBox();
        double[][]  lonlat   = new double[][] {
            { bounds.getX() }, { bounds.getY() + bounds.getHeight() }
        };
        ProjFile    projFile = shapefile.getProjFile();
        if (projFile != null) {
            lonlat = projFile.convertToLonLat(lonlat);
        }
        entry.setNorth(lonlat[IDX_LAT][0]);
        entry.setWest(lonlat[IDX_LON][0]);
        lonlat[IDX_LAT][0] = bounds.getY();
        lonlat[IDX_LON][0] = bounds.getX() + bounds.getWidth();
        if (projFile != null) {
            lonlat = projFile.convertToLonLat(lonlat);
        }
        entry.setSouth(lonlat[IDX_LAT][0]);
        entry.setEast(lonlat[IDX_LON][0]);

    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param firstCall _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void initializeEntryFromHarvester(Request request, Entry entry,
                                             boolean firstCall)
            throws Exception {
        super.initializeEntryFromHarvester(request, entry, firstCall);
        if (firstCall) {
            initializeEntryFromForm(request, entry, null, true);
        }
    }

    /**
     *
     * @param request _more_
     * @param entry _more_
     * @param map _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public boolean addToMap(Request request, Entry entry, MapInfo map)
            throws Exception {
        if ( !entry.isFile()) {
            return true;
        }
        //TODO: stream through the shapes
        EsriShapefile shapefile = null;
        try {
            shapefile = new EsriShapefile(entry.getFile().toString());
        } catch (Exception exc) {
            map.setHeaderMessage("Error opening shapefile:" + exc);
            return true;
        }
        List<EsriShapefile.EsriFeature> features =
            (List<EsriShapefile.EsriFeature>) shapefile.getFeatures();
        int numpoints = 0;
        int numFeatures = 0;
        for (EsriShapefile.EsriFeature feature : features) {
            numpoints += feature.getNumPoints();
            numFeatures++;
        }

        //System.out.println("num points = " + numpoints);
        if (numpoints > MAX_POINTS) {
            map.setHeaderMessage("Not showing all features because there are too many points");
            //            return true;
        }
        map.addKmlUrl(
            entry.getName(),
            request.entryUrl(
                getRepository().URL_ENTRY_SHOW, entry, ARG_OUTPUT,
                ShapefileOutputHandler.OUTPUT_KML.toString()), true);

        /*  For testing
        map.addGeoJsonUrl(
            entry.getName(),
            request.entryUrl(
                getRepository().URL_ENTRY_SHOW, entry, ARG_OUTPUT,
                ShapefileOutputHandler.OUTPUT_GEOJSON.toString()), true);
        */
        return false;
    }


    /**
     * Add this entry to a map
     * @param request the request
     * @param entry   the entry
     * @param map     the mapinfo
     *
     * @return true to also display the bounding box
     * @Override
     * public boolean addToMap(Request request, Entry entry, MapInfo map) throws Exception  {
     *   try {
     *       if ( !entry.isFile()) {
     *           return true;
     *       }
     *       //TODO: stream through the shapes
     *       EsriShapefile shapefile = null;
     *       try {
     *           shapefile = new EsriShapefile(entry.getFile().toString());
     *       } catch (Exception exc) {
     *           return true;
     *       }
     *
     *       List features    = shapefile.getFeatures();
     *       int  totalPoints = 0;
     *       int  MAX_POINTS  = 10000;
     *       for (int i = 0; i < features.size(); i++) {
     *           if (totalPoints > MAX_POINTS) {
     *               break;
     *           }
     *           EsriShapefile.EsriFeature gf =
     *               (EsriShapefile.EsriFeature) features.get(i);
     *           java.util.Iterator pi = gf.getGisParts();
     *           while (pi.hasNext()) {
     *               if (totalPoints > MAX_POINTS) {
     *                   break;
     *               }
     *               GisPart        gp     = (GisPart) pi.next();
     *               double[]       xx     = gp.getX();
     *               double[]       yy     = gp.getY();
     *               List<double[]> points = new ArrayList<double[]>();
     *               for (int ptIdx = 0; ptIdx < xx.length; ptIdx++) {
     *                   points.add(new double[] { yy[ptIdx], xx[ptIdx] });
     *               }
     *               totalPoints += points.size();
     *               if (points.size() > 1) {
     *                   map.addLines(entry, "", points,null);
     *               } else if (points.size() == 1) {
     *                   map.addMarker("id", points.get(0)[0],
     *                                 points.get(0)[1], null, "");
     *               }
     *           }
     *       }
     *   } catch (Exception exc) {
     *       throw new RuntimeException(exc);
     *   }
     *
     *   return false;
     * }
     */




}
