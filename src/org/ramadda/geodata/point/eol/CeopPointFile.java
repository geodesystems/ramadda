/**                                                                                                
Copyright (c) 2008-2026 Geode Systems LLC                                                          
SPDX-License-Identifier: Apache-2.0                                                                
*/


package org.ramadda.geodata.point.eol;

import org.ramadda.util.IO;
import org.ramadda.util.MyDateFormat;

import org.ramadda.data.point.*;
import org.ramadda.data.point.text.*;
import org.ramadda.data.record.*;

import java.io.*;

import java.text.SimpleDateFormat;

import java.util.Date;


/**
 */
public class CeopPointFile extends CsvFile {

    /**
     * The constructor
     *
     * @throws IOException On badness
     */
    public CeopPointFile(IO.Path path) throws IOException {
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
        //Set the delimiter
        putProperty(PROP_DELIMITER, " ");
        //Set the fields. the method reads the file CeopPointFile.fields.txt
        putProperty(PROP_FIELDS, getFieldsFileContents());
        super.prepareToVisit(visitInfo);

        return visitInfo;
    }


    /** _more_ */
    private MyDateFormat sdf = makeDateFormat("yyyy/MM/dd HH:mm");

    /**
     * This gets called after a record has been read.
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
        //concatenate the year and hour fields and set the date
        String dttm = textRecord.getStringValue(1) + " "
                      + textRecord.getStringValue(2);
        Date date = sdf.parse(dttm);
        record.setRecordTime(date.getTime());

        return true;
    }

    /**
     * _more_
     *
     * @param args _more_
     */
    public static void main(String[] args) {
        PointFile.test(args, CeopPointFile.class);
    }

}
