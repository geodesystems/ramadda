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


import org.ramadda.data.point.*;

import org.ramadda.data.record.*;
import org.ramadda.data.record.filter.*;
import org.ramadda.data.services.*;


import org.ramadda.repository.*;
import org.ramadda.util.grid.IdwGrid;
import org.ramadda.util.grid.LatLonGrid;

/*
For netcdf export sometime
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;
import ucar.nc2.ft.point.writer.CFPointObWriter;
*/






import ucar.unidata.util.Misc;

import java.awt.*;

import java.io.*;

import java.util.ArrayList;
import java.util.List;




/**
 * A BaseRecord visitor that holds the IdwGrid
 *
 *
 */
public class GridVisitor extends BridgeRecordVisitor {

    /** the request */
    Request request;

    /** the grid that does the real work */
    IdwGrid llg;

    /** number of columns */
    double gridWidth;

    /** number of rows */
    double gridHeight;

    /** are we gridding another attribute instead of altitude */
    int valueAttr = -1;

    /** _more_ */
    private List<Integer> divisors;

    /** are we using altitude as the gridded value */
    boolean usingAltitude;

    /** _more_ */
    boolean usingPointcount;

    /** how big an image */
    int imageHeight;

    /** how big an image */
    int imageWidth;

    /**
     * ctor
     *
     *
     * @param handler the output handler
     * @param request the request
     * @param llg the grid
     */
    public GridVisitor(RecordOutputHandler handler, Request request,
                       IdwGrid llg) {
        super(handler);
        this.request = request;
        this.llg     = llg;
        gridWidth    = llg.getGridWidth();
        gridHeight   = llg.getGridHeight();
        imageHeight  = llg.getHeight();
        imageWidth   = llg.getWidth();
        if (request.defined(RecordOutputHandler.ARG_PARAMETER)) {
            if (request.getString(RecordOutputHandler.ARG_PARAMETER,
                                  "").equals("_pointcount_")) {
                usingPointcount = true;
            } else {
                valueAttr = request.get(RecordOutputHandler.ARG_PARAMETER,
                                        -1);
            }

        }
        usingAltitude = valueAttr == -1;
        divisors      = new ArrayList<Integer>();
        for (String tok :
                (List<String>) request.get(RecordOutputHandler.ARG_DIVISOR,
                                           new ArrayList<String>())) {
            divisors.add(new Integer(tok));
        }

    }

    /**
     * get the grid
     *
     * @return the grid
     */
    public IdwGrid getGrid() {
        return llg;
    }

    /**
     * visit the record
     *
     * @param file record file
     * @param visitInfo visit info
     * @param record the record
     *
     * @return should continue
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
                                 BaseRecord record) {

        PointRecord pointRecord = (PointRecord) record;
        double      value;
        if (valueAttr != -1) {
            value = (double) pointRecord.getValue(valueAttr);
        } else if (usingPointcount) {
            value = 1;
        } else {
            value = (double) pointRecord.getAltitude();
        }
        if (divisors.size() > 0) {
            double values = 0;
            for (int i : divisors) {
                values += (double) pointRecord.getValue(i);
            }
            double o = value;
            if (values == 0) {
                value = Double.NaN;
            } else {
                value = value / values;
            }
        }


        double lat = pointRecord.getLatitude();
        double lon = pointRecord.getLongitude();
        synchronized (MUTEX) {
            //If first time then reset the grid
            if (cnt == 0) {
                llg.resetGrid();
            }
            cnt++;
            llg.addValue(lat, lon, value);
        }

        return true;
    }


    /**
     * Done. Tell the llg to average its values
     *
     *
     * @throws Exception _more_
     */
    public void finishedWithAllFiles() throws Exception {
        llg.doAverageValues();
        if (request.get(PointOutputHandler.ARG_FILLMISSING, false)) {
            llg.fillMissing();
        }
    }

}
