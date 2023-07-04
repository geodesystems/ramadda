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

package org.ramadda.geodata.point.ocean;


import org.ramadda.data.point.*;
import org.ramadda.geodata.point.netcdf.*;



import org.ramadda.data.record.*;
import org.ramadda.util.IO;
import org.ramadda.util.Utils;


import ucar.unidata.util.Misc;

import java.io.*;

import java.util.ArrayList;
import java.util.List;



/**
 * Class description
 *
 *
 * @version        $version$, Wed, Nov 13, '13
 * @author         Enter your name here...
 */
public class NetcdfOceanTrajectoryFile extends NetcdfTrajectoryFile {

    /**
     * ctor
     */
    public NetcdfOceanTrajectoryFile() {}




    /**
     * ctor
     * @throws IOException On badness
     */
    public NetcdfOceanTrajectoryFile(IO.Path path) throws IOException {
        super(path);
    }


    /**
     * _more_
     *
     * @param visitInfo _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public VisitInfo prepareToVisit(VisitInfo visitInfo) throws Exception {
        super.prepareToVisit(visitInfo);
        //Need to find the platform or instrument property
        String platform = Misc.getProperty(getFileProperties(), "platform",
                                           "");

        //LOOK: this needs to be in the same order as the oceantypes.xml defines in the point plugin
        setFileMetadata(new Object[] { platform });

        return visitInfo;
    }


    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        PointFile.test(args, NetcdfOceanTrajectoryFile.class);
    }





}
