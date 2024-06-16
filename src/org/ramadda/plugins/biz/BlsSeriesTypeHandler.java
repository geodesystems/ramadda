/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.biz;


import org.ramadda.data.point.text.*;
import org.ramadda.data.record.*;
import org.ramadda.data.record.*;

import org.ramadda.data.services.PointTypeHandler;

import org.ramadda.repository.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;
import org.ramadda.util.Utils;

import org.w3c.dom.*;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.StringUtil;

import java.io.*;

import java.net.URL;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;


import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 */
public class BlsSeriesTypeHandler extends PointTypeHandler {


    //NOTE: This starts at 2 because the point type has a number of points field

    /** _more_ */
    private static int IDX_BASE = 2;

    /** _more_ */
    public static final int IDX_SERIESID = IDX_BASE++;

    /** _more_ */
    public static final int IDX_SURVEY = IDX_BASE++;

    /** _more_ */
    public static final int IDX_MEASURE = IDX_BASE++;

    /** _more_ */
    public static final int IDX_INDUSTRY = IDX_BASE++;

    /** _more_ */
    public static final int IDX_SECTOR = IDX_BASE++;

    /** _more_ */
    public static final int IDX_AREA = IDX_BASE++;

    /** _more_ */
    public static final int IDX_ITEM = IDX_BASE++;


    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public BlsSeriesTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }

    /**
     * _more_
     *
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
        //wget -O test.html --post-data="permalinkURL=&selectedSeriesIds=CES0000000001&startYear=2004&endYear=2018&dv-submit=Update" "https://beta.bls.gov/dataViewer/view/timeseries/CES0000000001"
        //permalinkURL=&selectedSeriesIds=CES0000000001&startYear=2004&endYear=2018&dv-submit=Update

        String seriesId = entry.getStringValue(request,IDX_SERIESID, (String) null);
        if (seriesId == null) {
            return null;
        }
        String url = "https://beta.bls.gov/dataViewer/view/timeseries/"
                     + seriesId;

        return url;
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
        return new BlsRecordFile(request,getRepository(),
                                 new IO.Path(getPathForEntry(request, entry,true)), entry);
    }


    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Jan 3, '19
     * @author         Enter your name here...
     */
    public static class BlsRecordFile extends CsvFile {

	private Request request;

        private Entry entry;

        /** _more_ */
        private Repository repository;

        /**
         * _more_
         *
         *
         * @param repository _more_
         * @param filename _more_
         * @param entry _more_
         *
         * @throws IOException _more_
         */
        public BlsRecordFile(Request request,Repository repository, IO.Path path,
                             Entry entry)
                throws IOException {
            super(path);
	    this.request = request;
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
         * @throws Exception _more_
         */
        @Override
        public InputStream doMakeInputStream(boolean buffered)
                throws Exception {

            File file = repository.getEntryManager().getCacheFile(entry,
                            "bls.csv");
            if ( !file.exists()) {
                String seriesId = entry.getStringValue(request,IDX_SERIESID, (String) null);
                GregorianCalendar cal =
                    new GregorianCalendar(DateUtil.TIMEZONE_GMT);
                String start;
                String end;
                if (entry.getStartDate() == entry.getEndDate()) {
                    cal.setTime(new Date());
                    start = "2000";
                    end   = "" + cal.get(cal.YEAR);
                    //For now fix this to 2018
                    end = "2018";
                } else {
                    cal.setTime(new Date(entry.getStartDate()));
                    start = "" + cal.get(cal.YEAR);
                    cal.setTime(new Date(entry.getEndDate()));
                    end = "" + cal.get(cal.YEAR);
                }

                String body = "permalinkURL=&selectedSeriesIds=" + seriesId
                              + "&startYear=" + start + "&endYear=" + end
                              + "&dv-submit=Update";
                /*
                  var dqtChart = {"categories":["Qtr1 2016","Qtr2 2016","Qtr3 2016","Qtr4 2016","Qtr1 2017","Qtr2 2017","Qtr3 2017","Qtr4 2017","Qtr1 2018","Qtr2 2018","Qtr3 2018"],"tickInterval":1,"chart":{"title":"Office of Productivity And Technology and Percent/Rate/Ratio and Productivity : Nonfarm Business","subTitle":"","yAxis":"Labor productivity (output per hour)"},"series":[{"data":[0.3,0.9,1.3,1.3,0.4,1.6,2.3,-0.3,0.3,3.0,2.3],"name":"PRS85006092"}]};
                */
                String   html = Utils.doPost(new URL(getFilename()), body);
                String   dataPattern       = "\"data\":\\[(.*?)\\]";
                String   categoriesPattern = ".*\"categories\":\\[(.*?)\\]";
                String[] result            = null;
                String   categories        = null;
                String   data              = null;

                result = Utils.findPatterns(html, categoriesPattern);
                if ((result != null) && (result.length == 1)) {
                    categories = result[0];
                }

                result = Utils.findPatterns(html, dataPattern);
                if ((result != null) && (result.length == 1)) {
                    data = result[0];
                }

                if ((categories == null) || (data == null)) {
                    throw new IllegalArgumentException(
                        "Could not extract data");
                }
                StringBuilder sb      = new StringBuilder("#date,value\n");
                List<String>  times   = new ArrayList<String>();
                List<String>  periods = new ArrayList<String>();

                List<String>  values = StringUtil.split(data, ",", true,
                                           true);
                Hashtable months = Utils.makeHashtable("M01", "Jan", "M02",
                                       "Feb", "M03", "Mar", "M04", "Apr",
                                       "M05", "May", "M06", "Jun", "M07",
                                       "Jul", "M08", "Aug", "M09", "Sep",
                                       "M10", "Oct", "M11", "Nov", "M12",
                                       "Dec", "Q01", "Jan", "Q02", "Apr",
                                       "Q03", "Jul", "Q04", "Oct");
                for (String time :
                        StringUtil.split(categories, ",", true, true)) {
                    periods.add(time);
                    time = time.replace("\"", "").replace("Qtr1",
                                        "Jan").replace("Qtr2",
                                            "Apr").replace("Qtr3",
                                                "Jul").replace("Qtr4", "Oct");
                    //Check for just a year
                    if (time.matches("^\\d\\d\\d\\d$")) {
                        time = "Jan " + time;
                    }
                    List<String> toks = StringUtil.split(time, " ", true,
                                            true);
                    if (toks.size() == 3) {
                        if (toks.get(2).startsWith("M")
                                && toks.get(1).matches("^\\d\\d\\d\\d$")) {
                            String month = (String) months.get(toks.get(2));
                            time = month + " " + toks.get(1);
                        } else if (toks.get(2).startsWith("A")
                                   && toks.get(1).matches("^\\d\\d\\d\\d$")) {
                            time = "Jan" + " " + toks.get(1);
                        } else if (toks.get(2).startsWith("Q")
                                   && toks.get(1).matches("^\\d\\d\\d\\d$")) {
                            String month = (String) months.get(toks.get(2));
                            time = month + " " + toks.get(1);
                        }
                    }
                    times.add(time);
                }
                for (int i = 0; i < times.size(); i++) {
                    sb.append(times.get(i));
                    sb.append(",");
                    sb.append(periods.get(i));
                    sb.append(",");
                    sb.append(values.get(i));
                    sb.append("\n");
                }
                IOUtil.writeFile(file, sb.toString());
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
        public VisitInfo prepareToVisit(VisitInfo visitInfo)
                throws Exception {
            putProperty(PROP_SKIPLINES, "1");
            putProperty(PROP_HEADER_STANDARD, "true");
            super.prepareToVisit(visitInfo);
            String valueLabel = entry.getStringValue(request,IDX_MEASURE, entry.getName());
            putFields(new String[] {
                makeField(FIELD_DATE, attrType("date"), attrLabel("Date"),
                          attrFormat("MMM yyyy")),
                makeField("period", attrType("string"), attrLabel("Period")),
                makeField("value", attrType("double"),
                          attrLabel(valueLabel)) });

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
        String html = IOUtil.readContents(args[0],
                                          BlsSeriesTypeHandler.class);
        /*            "        var dqtChart = {\"categories\":[\"Qtr1 2016\",\"Qtr2 2016\",\"Qtr3 2016\",\"Qtr4 2016\",\"Qtr1 2017\",\"Qtr2 2017\",\"Qtr3 2017\",\"Qtr4 2017\",\"Qtr1 2018\",\"Qtr2 2018\",\"Qtr3 2018\"],\"tickInterval\":1,\"chart\":{\"title\":\"Office of Productivity And Technology and Percent/Rate/Ratio and Productivity : Nonfarm Business\",\"subTitle\":\"\",\"yAxis\":\"Labor productivity (output per hour)\"},\"series\":[{\"data\":[0.3,0.9,1.3,1.3,0.4,1.6,2.3,-0.3,0.3,3.0,2.3],\"name\":\"PRS85006092\"}]};";
         */
        String   dataPattern       = "\"data\":\\[(.*?)\\]";
        String   categoriesPattern = ".*\"categories\":\\[(.*?)\\]";
        String[] result            = null;
        String   categories        = null;
        String   data              = null;

        result = Utils.findPatterns(html, categoriesPattern);
        if ((result != null) && (result.length == 1)) {
            categories = result[0];
        }

        result = Utils.findPatterns(html, dataPattern);
        if ((result != null) && (result.length == 1)) {
            data = result[0];
        }
        System.err.println(categories);
        System.err.println(data);

        if ((categories == null) || (data == null)) {
            throw new IllegalArgumentException("Could not extract data");
        }
        SimpleDateFormat sdf   = new SimpleDateFormat("MMM yyyy");
        SimpleDateFormat sdf2  = new SimpleDateFormat("MMM yyyy");

        List<String>     times = new ArrayList<String>();
        for (String time : StringUtil.split(categories, ",", true, true)) {
            time = time.replace("\"", "").replace("Qtr1",
                                "Jan").replace("Qtr2", "Apr").replace("Qtr3",
                                    "Jul").replace("Qtr4", "Oct");

            List<String> toks = StringUtil.split(time, " ", true, true);
            if (toks.size() == 3) {}

            times.add(time);
            Date dttm = sdf.parse(time);
            System.err.println("dttm:" + dttm);
        }




    }

}
