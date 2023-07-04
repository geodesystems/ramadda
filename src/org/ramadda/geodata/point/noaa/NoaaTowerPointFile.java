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

package org.ramadda.geodata.point.noaa;

import org.ramadda.util.IO;

import org.ramadda.data.point.*;
import org.ramadda.data.point.text.*;

import org.ramadda.data.record.*;

import ucar.unidata.util.StringUtil;

import java.io.*;


/**
 */
public class NoaaTowerPointFile extends NoaaPointFile {

    /** _more_ */
    private static int IDX = 1;

    /** _more_ */
    public static final int IDX_SITE_CODE = IDX++;

    /** _more_ */
    public static final int IDX_YEAR = IDX++;

    /** _more_ */
    public static final int IDX_MONTH = IDX++;

    /** _more_ */
    public static final int IDX_DAY = IDX++;

    /** _more_ */
    public static final int IDX_HOUR = IDX++;

    /** _more_ */
    public static final int IDX_MINUTE = IDX++;

    /** _more_ */
    public static final int IDX_SECOND = IDX++;

    /** _more_ */
    public static final int IDX_LATITUDE = IDX++;

    /** _more_ */
    public static final int IDX_LONGITUDE = IDX++;

    /** _more_ */
    public static final int IDX_ELEVATION = IDX++;

    /** _more_ */
    public static final int IDX_INTAKE_HEIGHT = IDX++;

    /** _more_ */
    public static final int IDX_MEASURED_VALUE = IDX++;

    /** _more_ */
    public static final int IDX_TOTAL_UNCERTAINTY_ESTIMATE = IDX++;

    /** _more_ */
    public static final int IDX_ATMOSPHERIC_VARIABILTY = IDX++;

    /** _more_ */
    public static final int IDX_MEASUREMENT_UNCERTAINTY = IDX++;

    /** _more_ */
    public static final int IDX_SCALE_UNCERTAINTY = IDX++;

    /** _more_ */
    public static final int IDX_QC_FLAG = IDX++;

    /** _more_ */
    public static final double MISSING1 = -999.0;

    /** _more_ */
    public static final double MISSING2 = -999.99;


    /**
     * ctor
     *
     * @throws IOException On badness
     */
    public NoaaTowerPointFile(IO.Path path) throws IOException {
        super(path);
    }


    /**
     * This  gets called before the file is visited. It reads the header and pulls out metadata
     *
     * @param visitInfo visit info
     *
     * @return possible new visitinfo
     *
     *
     * @throws Exception _more_
     */
    public VisitInfo prepareToVisit(VisitInfo visitInfo) throws Exception {
        super.prepareToVisit(visitInfo);
        String filename  = getOriginalFilename(getFilename());
        String siteId    = StringUtil.findPattern(filename, "^(.*)_.*");
        String parameter = StringUtil.findPattern(filename, ".*\\.(.*)");
        //LOOK: this needs to be in the same order as the amrctypes.xml defines in the point plugin
        setFileMetadata(new Object[] { siteId, });

        putFields(new String[] {
            makeField(FIELD_SITE_ID, attrType(RecordField.TYPE_STRING)),
            makeField(FIELD_YEAR, ""), makeField(FIELD_MONTH, ""),
            makeField(FIELD_DAY, ""),
            makeField(FIELD_HOUR, attrType(RecordField.TYPE_STRING)),
            makeField(FIELD_MINUTE, attrType(RecordField.TYPE_STRING)),
            makeField(FIELD_SECOND, attrType(RecordField.TYPE_STRING)),
            makeField(FIELD_LATITUDE), makeField(FIELD_LONGITUDE),
            makeField(FIELD_INTAKE_HEIGHT),
            makeField(parameter, attrChartable(), attrMissing(MISSING1)),
            makeField("total_uncertainty_estimate", attrChartable(),
                      attrMissing(MISSING1)),
            makeField("atmospheric_variablitility", attrMissing(MISSING2)),
            makeField("measurement_uncertainty", attrChartable(),
                      attrMissing(MISSING2)),
            makeField("scale_uncertainty", attrChartable(),
                      attrMissing(MISSING2)),
            makeField(FIELD_QC_FLAG, attrType(RecordField.TYPE_STRING)),
        });
        setYMDHMSIndices(new int[] {
            IDX_YEAR, IDX_MONTH, IDX_DAY, IDX_HOUR, IDX_MINUTE, IDX_SECOND
        });

        return visitInfo;
    }

    /**
     * _more_
     *
     * @param args _more_
     */
    public static void main(String[] args) {
        PointFile.test(args, NoaaTowerPointFile.class);
    }

}
