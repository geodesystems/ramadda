/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.power;


import org.ramadda.data.point.*;
import org.ramadda.data.point.text.*;
import org.ramadda.data.record.*;


import org.ramadda.util.IO;
import org.ramadda.repository.RepositoryUtil;

import org.w3c.dom.Element;

import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.List;

import java.util.TimeZone;


/**
 */
public class MisoForecastFile extends CsvFile {

    /** _more_ */
    private boolean windForecast = false;


    /**
     * ctor
     *
     *
     * @throws IOException _more_
     */
    public MisoForecastFile(IO.Path path) throws IOException {
        super(path);
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
    public InputStream doMakeInputStream(boolean buffered) throws Exception {
        InputStream source = super.doMakeInputStream(buffered);
        Element     root   = XmlUtil.getRoot(source);
        //            System.err.println (XmlUtil.toString(root));
        windForecast = root.getTagName().equals("WindForecastDayAhead");

        StringBuilder s     = new StringBuilder("#converted stream\n");

        List          nodes = XmlUtil.findChildren(root, windForecast
                ? "Forecast"
                : "instance");

        for (int i = 0; i < nodes.size(); i++) {
            Element node = (Element) nodes.get(i);
            String dttm = XmlUtil.getGrandChildText(node,
                              "ForecastDateTimeEST", null);
            String hour = XmlUtil.getGrandChildText(node,
                              "ForecastHourEndingEST", null);
            String forecastValue = XmlUtil.getGrandChildText(node,
                                       "ForecasetValue", "NaN");
            String value = XmlUtil.getGrandChildText(node, "ActualValue",
                               "NaN");
            s.append(dttm + "," + hour + "," + forecastValue + "," + value
                     + "\n");
        }


        ByteArrayInputStream bais =
            new ByteArrayInputStream(s.toString().getBytes());

        return bais;
    }

    /**
     * Gets called when first reading the file. Parses the header
     *
     * @param visitInfo visit info
     *
     * @return the visit info
     *
     *
     * @throws Exception _more_
     */
    @Override
    public VisitInfo prepareToVisit(VisitInfo visitInfo) throws Exception {
        super.prepareToVisit(visitInfo);
        String format;
        String varName;
        String varLabel;
        if (windForecast) {
            format   = "yyyy-MM-dd h:mm:ss a";
            varName  = "wind_forecast";
            varLabel = "Wind Forecast";
        } else {
            format   = "yyyy-MM-dd h:mm:ss a";
            varName  = "wind_generation";
            varLabel = "Wind Generation";
            //            <DateTimeEST>Feb 27 2014  8:00PM</DateTimeEST>
        }
        putFields(new String[] {
            makeField(FIELD_DATE, attr("timezone", "EST"), attrType("date"),
                      attrFormat(format)),
            makeField("hour_ending", attrType("string"),
                      attrLabel("Hour Ending")),
            makeField(varName, attrLabel(varLabel), attrChartable()),
            makeField("actual_value", attrLabel("Actual Value"),
                      attrChartable()), });

        return visitInfo;
    }




    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        PointFile.test(args, MisoForecastFile.class);
    }

}
