/**                                                                                                
Copyright (c) 2008-2026 Geode Systems LLC                                                          
SPDX-License-Identifier: Apache-2.0                                                                
*/

package org.ramadda.geodata.point.gcnet;


import org.ramadda.util.IO;
import org.ramadda.data.point.*;
import org.ramadda.data.point.text.*;
import org.ramadda.data.record.*;


import org.ramadda.util.Station;

import java.io.*;


/**
 */

public class GCNetPointFile extends CsvFile {


    /**
     * ctor
     *
     * @throws IOException On badness
     */
    public GCNetPointFile(IO.Path path) throws IOException {
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
    @Override
    public VisitInfo prepareToVisit(VisitInfo visitInfo) throws Exception {
        //Put the delimiter first so we can read the header in the parent method
        putProperty(PROP_HEADER_DELIMITER, "");
        putProperty(PROP_DELIMITER, " ");
        super.prepareToVisit(visitInfo);
        putProperty(PROP_FIELDS, getFieldsFileContents());

        return visitInfo;
    }


    /*
     * This gets called after a record has been read
     */

    /**
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
        setLocation("" + (int) textRecord.getValue(1), textRecord);
        int    year      = (int) textRecord.getValue(5);
        double julianDay = textRecord.getValue(6);
        record.setRecordTime(getDateFromJulianDay(year, julianDay).getTime());

        return true;
    }


    /**
     * _more_
     *
     * @param args _more_
     */
    public static void main(String[] args) {
        PointFile.test(args, GCNetPointFile.class);
    }

}
