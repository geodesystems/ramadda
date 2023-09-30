/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.point;


import org.ramadda.data.point.text.*;
import org.ramadda.data.record.*;
import org.ramadda.data.services.PointTypeHandler;
import org.ramadda.data.services.RecordTypeHandler;
import org.ramadda.repository.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.IO;
import org.ramadda.util.Utils;

import org.w3c.dom.*;

import java.io.*;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Hashtable;


/**
 */
public class UsgsGaugeTypeHandler extends PointTypeHandler {



    /** _more_ */
    private static int IDX = PointTypeHandler.IDX_LAST + 1;

    /** _more_ */
    private static int IDX_STATION_ID = IDX++;

    /** _more_ */
    private static int IDX_PERIOD = IDX++;

    /** _more_ */
    private static int IDX_STATE = IDX++;

    /** _more_ */
    private static int IDX_HUC = IDX++;

    /** _more_ */
    private static int IDX_HOMEPAGE = IDX++;

    /**
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     * @throws Exception _more_
     */
    public UsgsGaugeTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }


    /** _more_ */
    private static final String URL_TEMPLATE =
        "https://waterdata.usgs.gov/nwis/uv?cb_00060=on&cb_00065=on&format=rdb&site_no=${station_id}&period=${period}";


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     * @param properties _more_
     * @param requestProperties _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public RecordFile doMakeRecordFile(Request request, Entry entry,
                                       Hashtable properties,
                                       Hashtable requestProperties)
            throws Exception {
        return new UsgsGaugeRecordFile(getPathForRecordEntry(entry,requestProperties), properties);
    }

    /**
     * Class description
     *
     *
     * @version        $version$, Mon, Feb 17, '20
     * @author         Enter your name here...
     */
    public static class UsgsGaugeRecordFile extends CsvFile {

        /**
         * _more_
         *
         * @param properties _more_
         *
         * @throws IOException _more_
         */
        public UsgsGaugeRecordFile(IO.Path path, Hashtable properties)
                throws IOException {
            super(path, properties);
        }

        /**
         * _more_
         *
         * @param record _more_
         * @param field _more_
         * @param s _more_
         *
         * @return _more_
         */
        public boolean isMissingValue(BaseRecord record, RecordField field,
                                      String s) {
            if (s.equals("Ice") || s.equals("Ssn")) {
                return true;
            }

            return super.isMissingValue(record, field, s);
        }

    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public String getPathForEntry(Request request, Entry entry, boolean forRead)
            throws Exception {
	if(entry.isFile()) {
	    return entry.getResource().getPath();
	}
        String url = URL_TEMPLATE;
        url = url.replace("${station_id}",
                          "" + entry.getValue(IDX_STATION_ID));
        url = url.replace("${period}", "" + entry.getValue(IDX_PERIOD));

        return url;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param fromImport _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void initializeNewEntry(Request request, Entry entry,
                                   boolean fromImport)
            throws Exception {}


}
