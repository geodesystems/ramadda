/**                                                                                                
Copyright (c) 2008-2026 Geode Systems LLC                                                          
SPDX-License-Identifier: Apache-2.0                                                                
*/

package org.ramadda.geodata.point.eol;


import org.ramadda.util.IO;
import org.ramadda.data.point.*;
import org.ramadda.data.point.text.*;
import org.ramadda.data.record.*;

import java.io.*;

import java.text.SimpleDateFormat;

import java.util.Date;


/**
 */
public class ShebaPointFile extends CsvFile {

    /**
     * The constructor
     *
     * @throws IOException On badness
     */
    public ShebaPointFile(IO.Path path) throws IOException {
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
        putProperty(PROP_DELIMITER, "tab");
        //Set the fields. the method reads the file ShebaPointFile.fields.txt
        putProperty(PROP_FIELDS, getFieldsFileContents());
        super.prepareToVisit(visitInfo);

        return visitInfo;
    }

    /**
     * _more_
     *
     * @param args _more_
     */
    public static void main(String[] args) {
        PointFile.test(args, ShebaPointFile.class);
    }

}
