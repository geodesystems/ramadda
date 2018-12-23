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

package org.ramadda.data.point.netcdf;


import org.ramadda.data.point.DataRecord;
import org.ramadda.data.record.Record.ReadStatus;
import org.ramadda.data.record.RecordField;
import org.ramadda.data.record.RecordFile;
import org.ramadda.data.record.RecordIO;
import org.ramadda.data.util.CdmUtil;

import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridAsPointDataset;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.ft.PointFeatureIterator;
import ucar.nc2.time.CalendarDate;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;


/**
 * Class description
 *
 *
 * @version        $version$, Wed, Feb 19, '14
 * @author         Enter your name here...
 */
public class NetcdfSinglePointGridRecord extends DataRecord {

    /** _more_ */
    private GridDataset dataset;

    /** _more_ */
    private GridAsPointDataset gapd;

    /** _more_ */
    private double lat;

    /** _more_ */
    private double lon;

    /** _more_ */
    private List<CalendarDate> dates;

    /** _more_ */
    private Iterator<CalendarDate> timeIterator;

    /** _more_ */
    private List<RecordField> dataFields = new ArrayList<RecordField>();

    /**
     * _more_
     *
     * @param file _more_
     * @param fields _more_
     * @param iterator _more_
     * @param gds _more_
     */
    public NetcdfSinglePointGridRecord(RecordFile file,
                                       List<RecordField> fields,
                                       GridDataset gds) {
        super(file, fields);
        this.dataset = gds;
        initFields(fields);
        for (int i = 3; i < fields.size(); i++) {
            dataFields.add(fields.get(i));
        }
        List<GridDatatype> grids = gds.getGrids();
        gapd = new GridAsPointDataset(gds.getGrids());
        GridDatatype     firstGrid = grids.get(0);
        GridCoordSystem  gcs       = firstGrid.getCoordinateSystem();
        CoordinateAxis1D lons      = (CoordinateAxis1D) gcs.getXHorizAxis();
        lon = lons.getCoordValue(0);
        CoordinateAxis1D lats = (CoordinateAxis1D) gcs.getYHorizAxis();
        lat          = lats.getCoordValue(0);
        dates        = gapd.getDates();
        timeIterator = dates.iterator();
    }

    /**
     * _more_
     *
     * @param recordIO _more_
     *
     * @return _more_
     *
     * @throws IOException _more_
     */
    @Override
    public ReadStatus read(RecordIO recordIO) throws IOException {
        if ( !timeIterator.hasNext()) {
            return ReadStatus.EOF;
        }
        setLocation(lon, lat, 0);
        CalendarDate date = timeIterator.next();
        int          cnt  = 0;
        values[cnt++] = lon;
        values[cnt++] = lat;
        Date dttm = CdmUtil.makeDate(date);
        objectValues[cnt++] = dttm;
        setRecordTime(dttm.getTime());
        for (RecordField field : dataFields) {
            GridDatatype grid = dataset.findGridDatatype(field.getName());
            GridAsPointDataset.Point point = gapd.readData(grid, date, lat,
                                                 lon);
            values[cnt] = point.dataValue;
            cnt++;
        }

        return ReadStatus.OK;
    }


}
