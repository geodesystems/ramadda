/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.data.point.text;

import org.ramadda.data.point.*;

import org.ramadda.data.record.*;

import org.ramadda.util.MyDateFormat;
import org.ramadda.util.Station;

import ucar.unidata.util.StringUtil;

import java.io.*;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

/**
 * Class description
 *
 *
 * @version        $version$, Wed, Nov 20, '13
 * @author         Enter your name here...
 */
public class MultiMonthRecord extends TextRecord {

    // 1948 -99.90 -99.90 -99.90 -99.90 -99.90 -99.90 -99.90 -99.90 -99.90 -99.90 -99.90 -99.90

    private MyDateFormat sdf;

    List<String> toks;

    List<String> noDataLines = new ArrayList<String>();

    int currentMonth;

    double missingValue = 0;

    private List<RecordField> fields;

    public MultiMonthRecord(MultiMonthRecord that) {
        super(that);
    }

    public MultiMonthRecord(MultiMonthFile file, String shortName,
                            String longName, String unit,
                            double missingValue) {
        super(file);
        this.missingValue = missingValue;
        sdf               = file.makeDateFormat("yyyy-MM");
        fields            = new ArrayList<RecordField>();
        RecordField dateField = new RecordField("date", "Date", "Date", 1,
                                    "");
        dateField.setType(RecordField.TYPE_DATE);
        DataRecord.initField(dateField);

        RecordField valueField = new RecordField(shortName, longName,
                                     longName, 2, unit);
        DataRecord.initField(valueField);
        valueField.setChartable(true);
        valueField.setSearchable(true);

        fields.add(dateField);
        fields.add(valueField);
        initFields(fields);
    }

    public List<RecordField> getFields() {
        return fields;
    }

    @Override
    public void checkIndices() {}

    public boolean skip(VisitInfo visitInfo, BaseRecord record, int howMany)
            throws Exception {
        RecordIO recordIO = visitInfo.getRecordIO();
        while (howMany-- > 0) {
            ReadStatus status = read(recordIO);
            if (status != ReadStatus.OK) {
                return false;
            }
        }

        return true;
    }

    @Override
    public ReadStatus read(RecordIO recordIO) throws Exception {
        if ((toks == null) || (currentMonth >= 12)) {
            currentMonth = 0;
            String line = readNextLine(recordIO);
            if (line == null) {
                return ReadStatus.EOF;
            }
            //            System.err.println ("LINE:" + line);
            toks = StringUtil.split(line, " ", true, true);
            //If there aren't 13 columns (year and 12 months of data) then append the line and skip
            if (toks.size() != (12 + 1)) {
                toks = null;
                noDataLines.add(line);

                return ReadStatus.SKIP;
            }
        }
        try {
            Date dttm = sdf.parse(toks.get(0) + "-" + (currentMonth + 1));
            setValue(1, dttm);
            double value = Double.parseDouble(toks.get(currentMonth + 1));
            if (value == missingValue) {
                value = Double.NaN;
            }
            setValue(2, value);
            currentMonth++;
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }

        return ReadStatus.OK;
    }

    public boolean needsValidPosition() {
        return false;
    }

}
