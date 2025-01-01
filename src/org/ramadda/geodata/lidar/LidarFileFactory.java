/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.lidar;


import org.ramadda.data.record.*;


/**
 * Extends the RecordFileFactory. Just passes the lidarfiles.txt to the super class
 * This file contains the class names of the available LidarFiles
 */
public class LidarFileFactory extends RecordFileFactory {

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public LidarFileFactory() throws Exception {
        super("/org/ramadda/geodata/lidar/lidarfiles.txt");
    }

    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        new LidarFileFactory().test(args);
    }

}
