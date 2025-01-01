/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.lidar;


import org.ramadda.util.IO;
import org.ramadda.data.point.*;

import org.ramadda.data.record.*;
import org.ramadda.data.record.filter.*;


import org.ramadda.geodata.lidar.las.LasConverter;



import java.io.*;


import java.util.Hashtable;


/**
 */
public abstract class LidarFile extends PointFile {


    /**
     * _more_
     */
    public LidarFile() {}




    /**
     * ctor
     *
     *
     * @param filename _more_
     * @throws IOException _more_
     */
    public LidarFile(IO.Path path) throws IOException {
        super(path);
    }


    /**
     * ctor
     *
     *
     * @param filename lidar data file
     * @param properties _more_
     *
     * @throws IOException _more_
     */
    public LidarFile(IO.Path path, Hashtable properties)
            throws IOException {
        super(path, properties);
    }


    /**
     * _more_
     *
     * @param action _more_
     *
     * @return _more_
     */
    @Override
    public boolean isCapable(String action) {
        if (action.equals(ACTION_GRID)) {
            return true;
        }
        if (action.equals(ACTION_DECIMATE)) {
            return true;
        }
        if (action.equals(ACTION_MAPINCHART)) {
            return true;
        }

        return super.isCapable(action);
    }


    /**
     * _more_
     *
     * @param tempFile _more_
     * @param os _more_
     * @param filter _more_
     *
     * @throws Exception _more_
     */
    public void writeToLas(File tempFile, OutputStream os,
                           RecordFilter filter)
            throws Exception {
        LasConverter converter = new LasConverter(this);
        converter.doConvert(tempFile, new RecordIO(os), filter);
    }


}
