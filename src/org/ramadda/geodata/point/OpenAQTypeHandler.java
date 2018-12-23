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
import org.ramadda.data.record.*;


import org.ramadda.data.services.PointTypeHandler;
import org.ramadda.data.services.RecordTypeHandler;


import org.ramadda.repository.*;
import org.ramadda.repository.map.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;


import org.ramadda.util.Utils;
import org.ramadda.util.text.CsvUtil;


import org.w3c.dom.*;

import ucar.unidata.geoloc.Bearing;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;


import java.io.*;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;

import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;


/**
 */
public class OpenAQTypeHandler extends PointTypeHandler {


    /** _more_ */
    private SimpleDateFormat dateSDF;

    /** _more_ */
    private static int IDX = RecordTypeHandler.IDX_LAST + 1;

    /** _more_ */
    private static int IDX_LOCATION = IDX++;

    /** _more_          */
    private static int IDX_COUNTRY = IDX++;

    /** _more_          */
    private static int IDX_CITY = IDX++;

    /** _more_          */
    private static int IDX_HOURS_OFFSET = IDX++;




    /**
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     * @throws Exception _more_
     */
    public OpenAQTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
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
    public RecordFile doMakeRecordFile(Request request, Entry entry)
            throws Exception {
        return new OpenAQRecordFile(getPathForEntry(request, entry));
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
        String location = entry.getValue(IDX_LOCATION, (String) null);
        if ( !Utils.stringDefined(location)) {
            System.err.println("no location");

            return null;
        }
        Date now = new Date();
        Integer hoursOffset = (Integer) entry.getValue(IDX_HOURS_OFFSET,
                                  new Integer(24));

        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(now);
        if (dateSDF == null) {
            dateSDF = RepositoryUtil.makeDateFormat("yyyy-MM-dd'T'HH:mm");
        }
        cal.add(cal.HOUR_OF_DAY, -hoursOffset.intValue());

        String startDate = dateSDF.format(cal.getTime());
        String url = "https://api.openaq.org/v1/measurements?format=csv&"
                     + HtmlUtils.arg("date_from", startDate) + "&"
                     + HtmlUtils.arg("location", location);
        System.err.println(url);

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

    /**
     * Class description
     *
     *
     * @version        $version$, Sat, Dec 8, '18
     * @author         Enter your name here...
     */
    public static class OpenAQRecordFile extends CsvFile {

        /**
         * _more_
         *
         * @param filename _more_
         *
         * @throws IOException _more_
         */
        public OpenAQRecordFile(String filename) throws IOException {
            super(filename);
        }



        /**
         * _more_
         *
         * @param buffered _more_
         *
         * @return _more_
         *
         * @throws IOException _more_
         */
        @Override
        public InputStream doMakeInputStream(boolean buffered)
                throws IOException {
            try {
                PipedInputStream      in   = new PipedInputStream();
                PipedOutputStream     out  = new PipedOutputStream(in);
                ByteArrayOutputStream bos  = new ByteArrayOutputStream();
                String[]              args = new String[] {
                    "-columns", "3,5,6,7,8,9", "-combineinplace", "1,3", " ",
                    "parameter", "-unfurl", "1", "2", "0", "3,4",
                    "-addheader",
                    "date.type date date.format yyyy-MM-dd'T'HH:mm:ss.SSS date.label \"Date\" utc.id date",
                    "-print"
                };
                CsvUtil csvUtil = new CsvUtil(args,
                                      new BufferedOutputStream(bos), null);
                csvUtil.setInputStream(super.doMakeInputStream(buffered));
                csvUtil.run(null);

                return new BufferedInputStream(
                    new ByteArrayInputStream(bos.toByteArray()));
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }

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
        public VisitInfo prepareToVisit(VisitInfo visitInfo)
                throws Exception {
            //            putProperty(PROP_SKIPLINES, "1");
            putProperty(PROP_HEADER_STANDARD, "true");

            super.prepareToVisit(visitInfo);

            //            utc,co ug/m^3,no2 ug/m^3,o3 ug/m^3,pm10 ug/m^3,so2 ug/m^3
            /*

            putFields(new String[] {
                makeField(FIELD_DATE, attrType("date"),
                          attrFormat("yyyy-MM-dd'T'HH:mm:ss")),
                makeField("latitude", attrType("double"),
                          attrLabel("Latitude")),
                makeField("longitude", attrType("double"),
                          attrLabel("Longitude")),
                makeField("co", attrType("double"),
                          attrChartable(), attrUnit("ug/m^3"),
                          attrLabel("co")),
                makeField("no2", attrType("double"),
                          attrChartable(), attrUnit("ug/m^3"),
                          attrLabel("no2")),
                makeField("o3", attrType("double"),
                          attrChartable(), attrUnit("ug/m^3"),
                          attrLabel("o3")),
                makeField("pm10", attrType("double"),
                          attrChartable(), attrUnit("ug/m^3"),
                          attrLabel("pm10")),
                makeField("so2", attrType("double"),
                          attrChartable(), attrUnit("ug/m^3"),
                          attrLabel("so2"))
            });
            */
            return visitInfo;
        }




    }

    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        //        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        SimpleDateFormat sdf =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        SimpleDateFormat sdf2 =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        //        String value = "2018-11-01T04:45:00.000Z";
        String value = "2018-11-01T04:45:00.000Z";
        System.err.println("date:" + sdf2.format(sdf.parse(value)));
    }




}
