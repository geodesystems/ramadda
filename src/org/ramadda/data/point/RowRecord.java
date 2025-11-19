/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.data.point;

import org.ramadda.data.record.*;

import org.ramadda.util.Station;
import org.ramadda.util.Utils;
import org.ramadda.util.seesv.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public abstract class RowRecord extends DataRecord {
    public static final int ATTR_FIRST =
        org.ramadda.data.point.PointRecord.ATTR_LAST;

    public RowRecord() {}

    public RowRecord(RowRecord that) {
        super(that);
    }

    public RowRecord(RecordFile file, List<RecordField> fields) {
        super(file, fields);
        if (fields != null) {
            initFields(fields);
        }
    }

    public RowRecord(RecordFile file) {
        super(file);
    }

    @Override
    public ReadStatus read(RecordIO recordIO) throws Exception {
        Row row = readNextRow(recordIO);
        if (row == null) {
            return ReadStatus.EOF;
        }
        //      System.err.println("ROW:" + row.size());
        for (int colIdx = 0; colIdx < row.size(); colIdx++) {
            Object      o     = row.get(colIdx);
            RecordField field = fields.get(colIdx);
            //      System.err.println("\tf:" + field.getName() +" field type:" + field.getType() +" v:" + o);
            if (field.isTypeString()) {
                objectValues[colIdx] = o;

                continue;
            }
            if (field.isTypeDate()) {
                objectValues[colIdx] = o;

                continue;
            }
            values[colIdx] = ((Double) o).doubleValue();
        }

        return ReadStatus.OK;
    }

    public abstract Row readNextRow(RecordIO recordIO) throws Exception;

}
