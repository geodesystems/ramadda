/**
   Copyright (c) 2008-2025 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.point.netcdf;

import org.ramadda.data.point.DataRecord;
import org.ramadda.data.record.BaseRecord;
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

    private GridDataset dataset;

    private GridAsPointDataset gapd;

    private double lat;

    private double lon;

    private List<CalendarDate> dates;

    private Iterator<CalendarDate> timeIterator;

    private List<RecordField> dataFields = new ArrayList<RecordField>();

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

    @Override
    public BaseRecord.ReadStatus read(RecordIO recordIO) throws IOException {
        if ( !timeIterator.hasNext()) {
            return BaseRecord.ReadStatus.EOF;
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

        return BaseRecord.ReadStatus.OK;
    }

}
