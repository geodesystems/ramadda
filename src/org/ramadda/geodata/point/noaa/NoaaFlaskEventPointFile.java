/**                                                                                                
Copyright (c) 2008-2026 Geode Systems LLC                                                          
SPDX-License-Identifier: Apache-2.0                                                                
*/



package org.ramadda.geodata.point.noaa;

import org.ramadda.util.IO;
import org.ramadda.data.point.*;
import org.ramadda.data.point.text.*;


import org.ramadda.data.record.*;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.io.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;



/**
 */

public class NoaaFlaskEventPointFile extends NoaaPointFile {


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

    /**
     * ctor
     *
     *
     *
     * @throws IOException On badness
     */
    public NoaaFlaskEventPointFile(IO.Path path) throws IOException {
        super(path);
    }



    /**
     * _more_
     *
     * @param visitInfo _more_
     *
     * @return _more_
     *
     *
     * @throws Exception _more_
     */
    @Override
    public VisitInfo prepareToVisit(VisitInfo visitInfo) throws Exception {
        super.prepareToVisit(visitInfo);
        String fields   = getFieldsFileContents();
        String filename = getOriginalFilename(getFilename());
        //[parameter]_[site]_[project]_[lab ID number]_[measurement group]_[optional qualifiers].txt
        List<String> toks = StringUtil.split(filename, "_", true, true);
        String       siteId           = toks.get(1);
        String       parameter        = toks.get(0);
        String       project          = toks.get(2);
        String       labIdNumber      = toks.get(3);
        String       measurementGroup = toks.get(4);
        setFileMetadata(new Object[] { siteId, parameter, project,
                                       labIdNumber, measurementGroup, });
        fields = fields.replace("${parameter}", parameter);
        putProperty(PROP_FIELDS, fields);
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
        PointFile.test(args, NoaaFlaskEventPointFile.class);
    }


}
