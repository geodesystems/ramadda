/**
   Copyright (c) 2008-2025 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.point.netcdf;

import org.ramadda.data.point.*;

import org.ramadda.data.record.*;
import org.ramadda.util.IO;
import org.ramadda.util.Utils;

import ucar.ma2.DataType;

import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ft.*;
import ucar.nc2.jni.netcdf.Nc4Iosp;
import ucar.nc2.time.Calendar;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.nc2.time.CalendarDateRange;

import ucar.unidata.util.IOUtil;

import ucar.unidata.util.StringUtil;

import java.io.*;

import java.util.ArrayList;

import java.util.Formatter;
import java.util.List;

/**
 * Class description
 *
 *
 * @version        $version$, Fri, Aug 23, '13
 * @author         Enter your name here...
 */
public class NetcdfTrajectoryFile extends NetcdfPointFile {

    /**
     * ctor
     */
    public NetcdfTrajectoryFile() {}

    /**
     * ctor
     *
     * @throws IOException On badness
     */
    public NetcdfTrajectoryFile(IO.Path path) throws IOException {
        super(path);
    }

    @Override
    public boolean isCapable(String action) {

        if (action.equals(ACTION_MAPINCHART)) {
            return true;
        }
        if (action.equals(ACTION_TRAJECTORY)) {
            return true;
        }

        return super.isCapable(action);
    }

    public VisitInfo prepareToVisit(VisitInfo visitInfo) throws Exception {
        super.prepareToVisit(visitInfo);
        String platform = "";
        //LOOK: this needs to be in the same order as the oceantypes.xml defines in the point plugin
        setFileMetadata(new Object[] { platform });

        return visitInfo;
    }

    public static void main(String[] args) throws Exception {
        PointFile.test(args, NetcdfTrajectoryFile.class);
    }

}
