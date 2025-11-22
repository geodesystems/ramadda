/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.data.point.text;

import org.ramadda.util.IO;
import org.ramadda.data.point.*;
import org.ramadda.data.point.text.*;

import org.ramadda.data.record.*;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.io.*;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.List;

/**
 */
public class MultiMonthFile extends CsvFile {

    String varName = null;

    String varDesc = null;

    double missing = -99.9;

    String unit = "";

    /**
     * The constructor
     *
     * @throws IOException On badness
     */
    public MultiMonthFile(IO.Path path) throws IOException {
        this(path, "index", "Index", "", -99.9);
    }

    /**
     * The constructor
     *
     * @param varName _more_
     * @param varDesc _more_
     * @param unit _more_
     * @param missing _more_
     * @throws IOException On badness
     */
    public MultiMonthFile(IO.Path path, String varName, String varDesc,
                          String unit, double missing)
            throws IOException {
        super(path);
        this.varName = varName;
        this.varDesc = varDesc;
        this.unit    = unit;
        this.missing = missing;
    }

    public List<RecordField> doMakeFields() {
        MultiMonthRecord record = new MultiMonthRecord(this, varName,
                                      varDesc, unit, missing);

        return record.getFields();
    }

    public BaseRecord doMakeRecord(VisitInfo visitInfo) {
        MultiMonthRecord record = new MultiMonthRecord(this, varName,
                                      varDesc, unit, missing);
        record.setFirstDataLine(firstDataLine);

        return record;
    }

    @Override
    public boolean skip(VisitInfo visitInfo, BaseRecord record, int howMany)
            throws Exception {
        MultiMonthRecord mmr = (MultiMonthRecord) record;

        return mmr.skip(visitInfo, record, howMany);
    }

    public static void main(String[] args) {
        PointFile.test(args, MultiMonthFile.class);
    }

}
