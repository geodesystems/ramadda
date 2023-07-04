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

package org.ramadda.geodata.point.above;

import org.ramadda.util.IO;

import org.ramadda.data.point.*;
import org.ramadda.data.point.text.*;

import org.ramadda.data.record.*;

import java.io.*;


import java.util.Hashtable;



/**
 */
public class CmcSweMonthlyClimate extends CsvFile {


    /**
     * _more_
     */
    public CmcSweMonthlyClimate() {}

    /**
     * ctor
     *
     *
     *
     * @throws IOException _more_
     */
    public CmcSweMonthlyClimate(IO.Path path) throws IOException {
        super(path);
    }

    /**
     * _more_
     *
     * @param properties _more_
     *
     * @throws IOException _more_
     */
    public CmcSweMonthlyClimate(IO.Path path, Hashtable properties)
            throws IOException {
        super(path, properties);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getDelimiter() {
        return ",";
    }


    /**
     * _more_
     *
     * @param visitInfo _more_
     *
     * @return _more_
     */
    @Override
    public int getSkipLines(VisitInfo visitInfo) {
        return 1;
    }

    //LATITUDE,LONGITUDE,OCT,NOV,DEC,JAN,FEB,MAR,APR,MAY,JUN

    /**
     *
     * @param visitInfo holds visit info
     *
     * @return visit info
     *
     *
     * @throws Exception _more_
     */
    @Override
    public VisitInfo prepareToVisit(VisitInfo visitInfo) throws Exception {
        super.prepareToVisit(visitInfo);
        String unit   = attrUnit("feet");
        String fields = makeFields(new String[] {
            makeField(FIELD_LATITUDE), makeField(FIELD_LONGITUDE),
            makeField("october", unit), makeField("november", unit),
            makeField("december", unit), makeField("january", unit),
            makeField("february", unit), makeField("march", unit),
            makeField("april", unit), makeField("may", unit),
            makeField("june", unit),
        });
        putProperty(PROP_FIELDS, fields);

        return visitInfo;
    }


    /**
     * _more_
     *
     * @param args _more_
     */
    public static void main(String[] args) {
        PointFile.test(args, CmcSweMonthlyClimate.class);
    }

}
