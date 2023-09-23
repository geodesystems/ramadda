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

import java.io.*;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.GregorianCalendar;

import java.util.Hashtable;


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
        return new DaymetRecordFile(getRepository(), entry,
                                    new IO.Path(getPathForEntry(request, entry,true)));
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
    public String getPathForEntry(Request request, Entry entry, boolean forRead)
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
        String endDate   = Utils.format(dateSDF,cal.getTime());
        if (entry.getStartDate() < entry.getEndDate()) {
	    startDate = Utils.format(dateSDF,new Date(entry.getStartDate()));
	    endDate   = Utils.format(dateSDF,new Date(entry.getEndDate()));
        }
        url = url.replace("${start}", startDate);
        url = url.replace("${end}", endDate);
	//	System.err.println("daymet url:" + url);
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
	 *
         * @throws IOException _more_
         */
        public DaymetRecordFile(Repository repository, Entry entry,
                                IO.Path path)
                throws IOException {
            super(path);
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
        @Override
        public InputStream doMakeInputStream(boolean buffered)
                throws Exception {
	    int                   stride = entry.getIntValue(IDX_STRIDE, 7);
	    String[]              args   = new String[] {
		"-skip", "8", "-decimate", "0", "" + stride, "-change",
		"0", "\\.0$", "", "-change", "1", "\\.0$", "", "-combine",
		"0,1", "-", "", "-scale", "3", "0", "0.0393700787", "0",
		"-format", "3", "#0.00", "-columns", "9,2-8", "-print"
	    };
	    return applySeesv(entry,args);
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




}
