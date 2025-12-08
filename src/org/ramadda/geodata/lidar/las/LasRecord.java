/**
   Copyright (c) 2008-2026 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/


package org.ramadda.geodata.lidar.las;


import org.ramadda.data.record.*;
import org.ramadda.geodata.lidar.*;

import java.io.*;


/**
 * Class description
 *
 *
 * @version        Enter version here..., Fri, May 21, '10
 * @author         Enter your name here...
 */
public abstract class LasRecord extends LidarRecord {

    /**
     * _more_
     *
     * @param recordFile _more_
     */
    public LasRecord(RecordFile recordFile) {
        super(recordFile);
    }



}
