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
import java.util.GregorianCalendar;


/**
 */
public class DaymetTypeHandler extends PointTypeHandler {


    /** _more_ */
    private SimpleDateFormat dateSDF;

    /** _more_ */
    private static int IDX = PointTypeHandler.IDX_LAST + 1;

    /** _more_ */
    private static int IDX_STRIDE = IDX++;


    /**
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     * @throws Exception _more_
     */
    public DaymetTypeHandler(Repository repository, Element node)
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
        return new DaymetRecordFile(getRepository(), entry,
                                    getPathForEntry(request, entry));
    }


    /** _more_ */
    private static final String URL_TEMPLATE =
        "https://daymet.ornl.gov/single-pixel/api/data?lat=${lat}&lon=${lon}&vars=dayl,prcp,srad,swe,tmax,tmin,vp&start=${start}&end=${end}";

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
        url = url.replace("${lat}", "" + entry.getLatitude());
        url = url.replace("${lon}", "" + entry.getLongitude());
        Date              now = new Date();
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(now);
        if (dateSDF == null) {
            dateSDF = RepositoryUtil.makeDateFormat("yyyy-MM-dd");
        }
        String startDate = "1980-01-01";
        String endDate   = dateSDF.format(cal.getTime());
        if (entry.getStartDate() < entry.getEndDate()) {
            startDate = dateSDF.format(new Date(entry.getStartDate()));
            endDate   = dateSDF.format(new Date(entry.getEndDate()));
        }
        url = url.replace("${start}", startDate);
        url = url.replace("${end}", endDate);

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
    public static class DaymetRecordFile extends CsvFile {

        /** _more_ */
        Repository repository;

        /** _more_ */
        Entry entry;

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
        public DaymetRecordFile(Repository repository, Entry entry,
                                String filename)
                throws IOException {
            super(filename);
            this.repository = repository;
            this.entry      = entry;
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
                String filename = "daymet_" + entry.getId() + "_"
                                  + entry.getChangeDate() + ".csv";
                File file = repository.getEntryManager().getCacheFile(entry,
                                filename);
                if ( !file.exists()) {
                    ByteArrayOutputStream bos  = new ByteArrayOutputStream();
                    FileOutputStream      fos  = new FileOutputStream(file);
                    int stride                 = entry.getValue(IDX_STRIDE,
                                                     7);
                    String[]              args = new String[] {
                        "-skip", "8", "-decimate", "0", "" + stride,
                        "-change", "0", "\\.0$", "", "-change", "1", "\\.0$",
                        "", "-combine", "0,1", "-", "", "-scale", "3", "0",
                        "0.0393700787", "0", "-format", "3", "#0.00",
                        "-columns", "9,2-8", "-print"
                    };
                    CsvUtil csvUtil = new CsvUtil(args,
                                          new BufferedOutputStream(fos),
                                          null);
                    csvUtil.setInputStream(super.doMakeInputStream(buffered));
                    csvUtil.run(null);
                    fos.close();
                }

                return new BufferedInputStream(new FileInputStream(file));
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
            //            putProperty(PROP_HEADER_STANDARD, "true");
            super.prepareToVisit(visitInfo);
            //            utc,co ug/m^3,no2 ug/m^3,o3 ug/m^3,pm10 ug/m^3,so2 ug/m^3
            putFields(new String[] {
                makeField(FIELD_DATE, attrType("date"), attrFormat("yyyy-D")),
                makeField("day_length", attrType("double"), attrChartable(),
                          attrUnit("seconds"), attrLabel("Day Length")),
                makeField("precipitation", attrType("double"),
                          attrChartable(), attrUnit("inches"),
                          attrLabel("Precipitation")),
                makeField("srad", attrType("double"), attrChartable(),
                          attrUnit("W/m^2"),
                          attrLabel("Shortwave Radiation")),
                makeField("swe", attrType("double"), attrChartable(),
                          attrUnit("kg/m^2"),
                          attrLabel("Snow Water Equivalent")),
                makeField("tmax", attrType("double"), attrChartable(),
                          attrUnit("degrees C"),
                          attrLabel("Max Temperature")),
                makeField("tmin", attrType("double"), attrChartable(),
                          attrUnit("degrees C"),
                          attrLabel("Min Temperature")),
                makeField("vp", attrType("double"), attrChartable(),
                          attrUnit("Pa"), attrLabel("Pressure"))
            });

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
