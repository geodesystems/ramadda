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
import org.ramadda.util.text.Seesv;

import org.w3c.dom.*;

import ucar.unidata.util.StringUtil;

import java.io.*;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;


/**
 */
public class TmyTypeHandler extends PointTypeHandler {


    /** _more_ */
    private SimpleDateFormat dateSDF;

    /** _more_ */
    private static int IDX = PointTypeHandler.IDX_LAST + 1;

    /** _more_ */
    private static int IDX_STATE = IDX++;


    /**
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     * @throws Exception _more_
     */
    public TmyTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public String getContextNamespace() {
        return "tmy";
    }

    /**
     * _more_
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
        return new TmyRecordFile(getRepository(), entry,
                                 new IO.Path(entry.getResource().getPath()), this);
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
            throws Exception {
        initializeRecordEntry(entry, entry.getFile(), true);

        FileInputStream fis =
            new FileInputStream(entry.getResource().getPath());
        BufferedReader br   = new BufferedReader(new InputStreamReader(fis));
        String         line = br.readLine();
        //690150,"TWENTYNINE PALMS",CA,-8.0,34.300,-116.167,626
        List<String> toks = StringUtil.split(line, ",");
        entry.setName(toks.get(1).replaceAll("\"", ""));
        entry.setValue(IDX_STATE, toks.get(2));
        entry.setLocation(Double.parseDouble(toks.get(4)),
                          Double.parseDouble(toks.get(5)));
        fis.close();
    }




    /**
     * Class description
     *
     *
     * @version        $version$, Sat, Dec 8, '18
     * @author         Enter your name here...
     */
    public static class TmyRecordFile extends CsvFile {

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
         * @param context _more_
         *
         * @throws IOException _more_
         */
        public TmyRecordFile(Repository repository, Entry entry,
                             IO.Path path, RecordFileContext context)
                throws IOException {
            super(path, context, null);
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
         *
         * @throws Exception _more_
         */
        public InputStream xxxdoMakeInputStream(boolean buffered)
                throws Exception {
            String filename = "tmy_" + entry.getId() + "_"
                              + entry.getChangeDate() + ".csv";
            File file = repository.getEntryManager().getCacheFile(entry,
                            filename);
            if ( !file.exists()) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                FileOutputStream      fos = new FileOutputStream(file);
                //                    int stride                 = entry.getValue(IDX_STRIDE,   7);
                String[] args = new String[] {
                    "-skip", "1",
                    //                        "-pattern", "1", "12:00",
                    "-pattern", "1", "12:00", "-change", "0",
                    "(..)/(..)/(....)", "2000-$1-$2", "-combineinplace",
                    "0,1", " ", "Date", "-columns",
                    "0-3,6,9,12,15,18,21,24,27,30,33,36,39,42,45,48,51,54,57,60,63",
                    "-addheader",
                    "makeLabel false date.format _quote_yyyy-MM-dd HH:mm_quote_",
                    "-print"
                };
                Seesv csvUtil = new Seesv(args,
                                      new BufferedOutputStream(fos), null);
                csvUtil.setInputStream(super.doMakeInputStream(buffered));
                csvUtil.run(null);
                fos.close();
            }

            return new BufferedInputStream(new FileInputStream(file));

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
        /*
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

        */


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
