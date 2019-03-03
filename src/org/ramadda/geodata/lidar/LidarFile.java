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

package org.ramadda.geodata.lidar;


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
     * _more_
     *
     * @param properties _more_
     */
    public LidarFile(Hashtable properties) {
        super(properties);
    }


    /**
     * ctor
     *
     *
     * @param filename lidar data file
     *
     * @throws Exception On badness
     *
     * @throws IOException _more_
     */
    public LidarFile(String filename) throws IOException {
        super(filename);
    }


    /**
     * ctor
     *
     *
     * @param filename lidar data file
     * @param properties _more_
     *
     * @throws Exception On badness
     *
     * @throws IOException _more_
     */
    public LidarFile(String filename, Hashtable properties)
            throws IOException {
        super(filename, properties);
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
