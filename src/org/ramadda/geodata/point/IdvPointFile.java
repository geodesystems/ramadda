/**
Copyright (c) 2008-2023 Geode Systems LLC
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



/**
 */
public class IdvPointFile extends CsvFile {

    /**
     * _more_
     */
    public IdvPointFile() {}

    /**
     * ctor
     *
     *
     * @throws IOException _more_
     */
    public IdvPointFile(IO.Path path) throws IOException {
        super(path);
    }

    /**
     * _more_
     *
     * @param filename _more_
     * @param properties _more_
     *
     * @throws IOException _more_
     */
    public IdvPointFile(IO.Path path, Hashtable properties)
            throws IOException {
        super(path, properties);
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
        String skipFromProperties = getProperty(PROP_SKIPLINES,
                                        (String) null);
        if (skipFromProperties != null) {
            return Integer.parseInt(skipFromProperties);
        }
        return 3;
    }

    /**
     * _more_
     *
     * @param recordIO _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public RecordIO readHeader(RecordIO recordIO) throws Exception {
        return recordIO;
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
        List<String> headerLines = getHeaderLines();
        if (headerLines.size() != 3) {
            throw new IllegalArgumentException("Bad number of header lines:"
                    + headerLines.size());
        }
        String fields = headerLines.get(1);
        putProperty(PROP_FIELDS, fields);
        return visitInfo;
    }

    /**
     * _more_
     *
     * @param field _more_
     * @param properties _more_
     * @param prop _more_
     * @param dflt _more_
     *
     * @return _more_
     */
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


    /**
     * _more_
     *
     *
     * @param failureOk _more_
     * @return _more_
     */
    public List<RecordField> doMakeFields(boolean failureOk) {
        String fieldString = getProperty(PROP_FIELDS, null);
        if (fieldString == null) {
            try {
                VisitInfo visitInfo = new VisitInfo();
                RecordIO  recordIO  = doMakeInputIO(visitInfo, true);
                visitInfo.setRecordIO(recordIO);
                visitInfo = prepareToVisit(visitInfo);
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }
        }

        return super.doMakeFields(failureOk);

    }





    /**
     * _more_
     *
     * @param index _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public PointRecord getRecord(int index) throws Exception {
        throw new IllegalArgumentException("Not implemented");
    }

    /**
     * _more_
     *
     * @param args _more_
     */
    public static void main(String[] args) {
        PointFile.test(args, IdvPointFile.class);
    }

}
