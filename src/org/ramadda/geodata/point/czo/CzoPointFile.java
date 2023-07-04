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

package org.ramadda.geodata.point.czo;


import org.ramadda.util.IO;
import org.ramadda.data.point.*;
import org.ramadda.data.point.text.*;

import org.ramadda.data.record.*;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.io.*;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.List;



/**
 */
public class CzoPointFile extends CsvFile {


    /**
     * The constructor
     *
     * @throws IOException On badness
     */
    public CzoPointFile(IO.Path path) throws IOException {
        super(path);
    }


    /**
     * This  gets called before the file is visited. It reads the header and defines the fields
     *
     * @param visitInfo visit info
     * @return possible new visitinfo
     *
     * @throws Exception _more_
     */
    @Override
    public VisitInfo prepareToVisit(VisitInfo visitInfo) throws Exception {
        //Se the delimiter and how many lines in the header to skip
        putProperty(PROP_DELIMITER, ",");
        putProperty(PROP_SKIPLINES, "1");
        super.prepareToVisit(visitInfo);

        return visitInfo;
    }



    /**
     * This gets called after a record has been read
     * _more_
     *
     * @param visitInfo _more_
     * @param record _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean processAfterReading(VisitInfo visitInfo, BaseRecord record)
            throws Exception {
        if ( !super.processAfterReading(visitInfo, record)) {
            return false;
        }
        TextRecord textRecord = (TextRecord) record;
        /*
                //Get the date from the values
                String dttm = ((int) textRecord.getValue(IDX_YEAR)) + "-"
                              + ((int) textRecord.getValue(IDX_MONTH)) + "-"
                              + ((int) textRecord.getValue(IDX_DAY)) + " "
                              + textRecord.getStringValue(IDX_TIME);
                Date date = sdf.parse(dttm);
                record.setRecordTime(date.getTime());
        */

        return true;
    }

    /**
     * _more_
     *
     * @param args _more_
     */
    public static void main(String[] args) {
        PointFile.test(args, CzoPointFile.class);
    }

}
