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

package org.ramadda.plugins.biz;


import org.ramadda.data.point.*;
import org.ramadda.data.point.text.*;
import org.ramadda.data.record.*;


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
public class FredFile extends CsvFile {


    /** _more_ */
    private byte[] buffer;


    /**
     *     ctor
     *
     *     @param filename _more_
     *
     *     @throws IOException _more_
     */
    public FredFile(String filename) throws IOException {
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
            if (buffer == null) {
                //                System.err.println("Reading FRED time series");
                StringBuilder sb     = new StringBuilder();
                InputStream   source = super.doMakeInputStream(buffered);
                Element       root   = XmlUtil.getRoot(source);

                //            System.err.println("Root:" + XmlUtil.toString(root));

                String format = "yyyy-MM-dd";
                String unit = XmlUtil.getAttribute(root, Fred.ATTR_UNITS, "");
                putFields(new String[] {
                    makeField(FIELD_DATE, attrType("date"),
                              attrFormat(format)),
                    makeField("value", attrUnit(unit), attrLabel("Value"),
                              attrChartable(), attrMissing(-999999.99)), });


                List nodes = XmlUtil.findChildren(root, Fred.TAG_OBSERVATION);
                for (int i = 0; i < nodes.size(); i++) {
                    Element node = (Element) nodes.get(i);
                    String value = XmlUtil.getAttribute(node,
                                       Fred.ATTR_VALUE, "").trim();
                    String dttm = XmlUtil.getAttribute(node, Fred.ATTR_DATE,
                                      (String) null);
                    if (value.equals("") || value.equals(".")) {
                        value = "-999999.99";
                    }
                    sb.append(dttm + "," + value + "\n");
                }
                buffer = sb.toString().getBytes();
            }

            ByteArrayInputStream bais = new ByteArrayInputStream(buffer);

            return bais;
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
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
        if (getProperty(PROP_FIELDS, (String) null) == null) {
            String format = "yyyy-MM-dd";
            putFields(new String[] {
                makeField(FIELD_DATE, attrType("date"), attrFormat(format)),
                makeField("value", attrLabel("Value"), attrChartable(),
                          attrMissing(-999999.99)), });
        }

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
        PointFile.test(args, FredFile.class);
    }

}
