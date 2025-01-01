/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.lidar;


import org.ramadda.data.point.*;


import org.ramadda.data.record.*;


import java.io.*;



/**
 * Class description
 *
 *
 * @version        Enter version here..., Fri, May 21, '10
 * @author         Enter your name here...
 */
public abstract class LidarRecord extends PointRecord {


    /**
     * _more_
     *
     * @param recordFile _more_
     */
    public LidarRecord(RecordFile recordFile) {
        super(recordFile);
    }

    /**
     * _more_
     *
     * @param that _more_
     */
    public LidarRecord(LidarRecord that) {
        super(that);
    }

    /**
     * _more_
     *
     *
     * @param recordFile _more_
     * @param bigEndian _more_
     */
    public LidarRecord(RecordFile recordFile, boolean bigEndian) {
        super(recordFile, bigEndian);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public LidarFile getLidarFile() {
        return (LidarFile) getRecordFile();
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public short[] getRgb() {
        return null;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public double getLidarIntensity() {
        return 0;
    }


}
