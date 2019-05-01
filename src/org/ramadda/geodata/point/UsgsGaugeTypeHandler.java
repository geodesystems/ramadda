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

package org.ramadda.geodata.point;


import org.ramadda.data.point.text.*;
import org.ramadda.data.record.*;
import org.ramadda.data.services.PointTypeHandler;
import org.ramadda.data.services.RecordTypeHandler;
import org.ramadda.repository.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.Utils;
import org.ramadda.util.text.CsvUtil;

import org.w3c.dom.*;

import java.io.*;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.Hashtable;
import java.util.GregorianCalendar;


/**
 */
public class UsgsGaugeTypeHandler extends PointTypeHandler {



    /** _more_ */
    private static int IDX = PointTypeHandler.IDX_LAST + 1;

    /** _more_ */
    private static int IDX_STATION_ID = IDX++;
    private static int IDX_PERIOD = IDX++;
    private static int IDX_STATE = IDX++;
    private static int IDX_HUC = IDX++;
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
    private static final String URL_TEMPLATE ="https://waterdata.usgs.gov/nwis/uv?cb_00060=on&cb_00065=on&format=rdb&site_no=${station_id}&period=${period}";


    @Override
        public RecordFile doMakeRecordFile(Entry entry, Hashtable properties,
                                       Hashtable requestProperties)
        throws Exception {
        return new UsgsGaugeRecordFile(getPathForRecordEntry(entry, requestProperties), properties);
    }

    public static class UsgsGaugeRecordFile extends CsvFile {
        /**
         * _more_
         *
         *
         * @param repository _more_
         * @param entry _more_
         * @param filename _more_
         *
         * @throws IOException _more_
         */
        public UsgsGaugeRecordFile(String filename,Hashtable properties)
                throws IOException {
            super(filename, properties);

        }

        public boolean isMissingValue(Record record, RecordField field,
                                      String s) {
            if(s.equals("Ice")) return true;
            return super.isMissingValue(record,field,s);
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
    public String getPathForEntry(Request request, Entry entry)
            throws Exception {
        String url = URL_TEMPLATE;
        url = url.replace("${station_id}", "" + entry.getValue(IDX_STATION_ID));
        url = url.replace("${period}", "" + entry.getValue(IDX_PERIOD));
        return url;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void initializeNewEntry(Request request, Entry entry)
            throws Exception {}


}
