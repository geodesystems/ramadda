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

package org.ramadda.geodata.point.noaa;

import org.ramadda.util.IO;

import org.ramadda.data.point.*;
import org.ramadda.data.point.text.*;



import org.ramadda.data.record.*;

import org.ramadda.util.Station;

import ucar.unidata.util.StringUtil;

import java.io.*;

import java.util.List;



/**
 */

public class NoaaPointFile extends CsvFile {

    /** _more_ */
    public static final String FIELD_NUMBER_OF_MEASUREMENTS =
        "number_of_measurements";

    /** _more_ */
    public static final String FIELD_QC_FLAG = "qc_flag";

    /** _more_ */
    public static final String FIELD_INTAKE_HEIGHT = "intake_height";

    /** _more_ */
    public static final String FIELD_INSTRUMENT = "instrument";

    /**
     * ctor
     *
     *
     * @throws IOException On badness
     */
    public NoaaPointFile(IO.Path path) throws IOException {
        super(path);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getStationsPath() {
        return "/org/ramadda/data/point/noaa/stations.txt";
    }



    /**
     * This is used by RAMADDA to determine what kind of services are available for this type of point data
     *
     * @param action _more_
     * @return is this file capable of the action
     */
    public boolean isCapable(String action) {
        if (action.equals(ACTION_BOUNDINGPOLYGON)) {
            return false;
        }
        if (action.equals(ACTION_GRID)) {
            return false;
        }

        return super.isCapable(action);
    }


    /*
     * Get the delimiter (space)
     *      @return the column delimiter
     */

    /**
     * _more_
     *
     * @return _more_
     */
    public String getDelimiter() {
        return " ";
    }


    /**
     * There are  2 header lines
     *
     * @param visitInfo file visit info
     *
     * @return how many lines to skip
     */
    public int getSkipLines(VisitInfo visitInfo) {
        return 0;
    }

}
