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
