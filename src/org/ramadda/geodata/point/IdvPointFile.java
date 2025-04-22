/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.point;

import org.ramadda.util.IO;
import org.ramadda.data.point.*;
import org.ramadda.data.point.text.*;
import org.ramadda.data.record.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class IdvPointFile extends CsvFile {

    public IdvPointFile() {}
    public IdvPointFile(IO.Path path) throws IOException {
        super(path);
    }
    public IdvPointFile(IO.Path path, Hashtable properties)
            throws IOException {
        super(path, properties);
    }
    @Override
    public int getSkipLines(VisitInfo visitInfo) {
        String skipFromProperties = getProperty(PROP_SKIPLINES,
                                        (String) null);
        if (skipFromProperties != null) {
            return Integer.parseInt(skipFromProperties);
        }
        return 3;
    }

    @Override
    public RecordIO readHeader(RecordIO recordIO) throws Exception {
        return recordIO;
    }

    @Override
    public VisitInfo prepareToVisit(VisitInfo visitInfo) throws Exception {
        super.prepareToVisit(visitInfo);
        List<String> headerLines = getHeaderLines();
        if (headerLines.size() != 3) {
            throw new IllegalArgumentException("Bad number of header lines:"
                    + headerLines.size());
        }
        String fields = headerLines.get(1);
        putProperty(PROP_FIELDS, fields);
        return visitInfo;
    }

    @Override
    public String getProperty(RecordField field, Hashtable properties,
                              String prop, String dflt) {
        if (prop.equals("chartable") || prop.equals("searchable")) {
            String fieldName = field.getName().toLowerCase();
            if ( !fieldName.equals("latitude")
                    && !fieldName.equals("longitude")
                    && !fieldName.equals("time")) {
                return super.getProperty(field, properties, prop, "true");
            }
        }

        return super.getProperty(field, properties, prop, dflt);
    }

    @Override
    public List<RecordField> doMakeFields(boolean failureOk) {
        String fieldString = getProperty(PROP_FIELDS, null);
        if (fieldString == null) {
            try {
                VisitInfo visitInfo = new VisitInfo();
                RecordIO  recordIO  = doMakeInputIO(visitInfo);
                visitInfo.setRecordIO(recordIO);
                visitInfo = prepareToVisit(visitInfo);
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }
        }

        return super.doMakeFields(failureOk);

    }

    @Override
    public PointRecord getRecord(int index) throws Exception {
        throw new IllegalArgumentException("Not implemented");
    }

}
