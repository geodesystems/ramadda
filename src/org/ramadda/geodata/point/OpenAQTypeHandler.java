/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
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
import org.ramadda.util.text.Seesv;


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

    /** _more_ */
    private static int IDX_COUNTRY = IDX++;

    /** _more_ */
    private static int IDX_CITY = IDX++;

    /** _more_ */
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
        return new OpenAQRecordFile(getPathForEntry(request, entry,true));
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
        String location = entry.getStringValue(IDX_LOCATION, (String) null);
        if ( !Utils.stringDefined(location)) {
            System.err.println("no location");

            return null;
        }
        Date now = new Date();
        Integer hoursOffset = (Integer) entry.getIntValue(IDX_HOURS_OFFSET,
                                  Integer.valueOf(24));

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

        //      System.err.println(url);
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
         *
         * @throws Exception _more_
         */
        @Override
        public InputStream doMakeInputStream(boolean buffered)
                throws Exception {
            PipedInputStream      in   = new PipedInputStream();
            PipedOutputStream     out  = new PipedOutputStream(in);
            ByteArrayOutputStream bos  = new ByteArrayOutputStream();
            String[]              args = new String[] {
                "-columns", "location,utc,parameter,value,latitude,longitude",
                "-unfurl", "parameter", "value", "utc",
                "location,latitude,longitude", "-addheader",
                "utc.id date date.type date date.format yyyy-MM-dd'T'HH:mm:ss.SSS date.label \"Date\" ",
                "-print"
            };
            Seesv csvUtil = new Seesv(args,
                                          new BufferedOutputStream(bos),
                                          null);
            csvUtil.setInputStream(super.doMakeInputStream(buffered));
            csvUtil.run(null);

            return new BufferedInputStream(
                new ByteArrayInputStream(bos.toByteArray()));
        }
    }
}
