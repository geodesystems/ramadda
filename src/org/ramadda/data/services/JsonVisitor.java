/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.data.services;


import org.ramadda.data.point.PointRecord;


import org.ramadda.data.point.text.*;
import org.ramadda.data.record.BaseRecord;
import org.ramadda.data.record.RecordField;
import org.ramadda.data.record.RecordFile;
import org.ramadda.data.record.ValueGetter;
import org.ramadda.data.record.VisitInfo;
import org.ramadda.repository.Entry;
import org.ramadda.repository.Request;
import org.ramadda.util.Json;
//import ucar.nc2.ft.point.writer.CFPointObWriter;
//import ucar.nc2.ft.point.writer.PointObVar;


import java.io.PrintWriter;

import java.util.List;


/**
 * Class description
 *
 *
 * @version        $version$, Thu, Jan 2, '14
 * @author         Enter your name here...
 */
public class JsonVisitor extends BridgeRecordVisitor {


    /** _more_ */
    private static final String COMMA = ",\n";

    /** _more_ */
    private static final String QUOTE = "\"";

    /** _more_ */
    private int cnt = 0;

    /** _more_ */
    private List<RecordField> fields;

    /** _more_ */
    private PrintWriter pw;

    /** _more_ */
    static int _cnt = 0;

    /** _more_ */
    int mycnt = _cnt++;

    /** _more_ */
    private boolean initParams = false;

    /** _more_ */
    private boolean addElevation;

    /** _more_ */
    private boolean addGeo;

    /** _more_ */
    private boolean addTime;



    /**
     * _more_
     *
     * @param handler _more_
     * @param request _more_
     * @param processId _more_
     * @param mainEntry _more_
     * @param suffix _more_
     */
    public JsonVisitor(RecordOutputHandler handler, Request request,
                       Object processId, Entry mainEntry, String suffix) {
        super(handler, request, processId, mainEntry, suffix);
    }

    /**
     * _more_
     *
     * @param file _more_
     * @param visitInfo _more_
     * @param record _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean doVisitRecord(RecordFile file, VisitInfo visitInfo,
                                 BaseRecord record)
            throws Exception {

        boolean debug = false;
        if (debug) {
            System.err.println("JsonVisitor.doVisitRecord");
        }
        if ( !getHandler().jobOK(getProcessId())) {
            if (debug) {
                System.err.println("\tjob not OK");
            }

            return false;
        }
        PointRecord pointRecord = (PointRecord) record;

        if ( !initParams) {
            initParams = true;
            addElevation = file.getProperty("output.elevation", true)
                           && pointRecord.isValidAltitude();

            addGeo = file.getProperty("output.latlon", true)
                     && pointRecord.isValidPosition();

            addTime = file.getProperty("output.time", true)
                      && pointRecord.hasRecordTime();
        }
        if (fields == null) {
            pw     = getThePrintWriter();
            fields = record.getFields();
            RecordField.addJsonHeader(pw, mainEntry.getName(), fields,
                                      addGeo, addElevation, addTime);
        }

        int fieldCnt = 0;
        if (cnt > 0) {
            pw.append(COMMA);
        }

        pw.append(Json.MAP_OPEN);
        pw.append(QUOTE);
        pw.append(Json.FIELD_VALUES);
        pw.append("\":");
        pw.append(Json.LIST_OPEN);
        double d = 0;
        for (RecordField field : fields) {
            if (field.getIsLatitude()) {
                addGeo = false;
            }
            if (field.getIsAltitude()) {
                addElevation = false;
            }
            if (field.getSynthetic()) {
                continue;
            }
            if (field.getArity() > 1) {
                continue;
            }
            ValueGetter getter = field.getValueGetter();
            if (fieldCnt > 0) {
                pw.append(COMMA);
            }

            if (getter == null) {
                if (field.isTypeString()) {
                    Json.quote(pw, record.getStringValue(field.getParamId()));
                    //                    pw.append(QUOTE);
                    //                    pw.append(record.getStringValue(field.getParamId()));
                    //                    pw.append(QUOTE);
                } else if (field.isTypeDate()) {
                    pw.append(QUOTE);
                    pw.append(record.getStringValue(field.getParamId()));
                    pw.append(QUOTE);
                } else {
                    d = record.getValue(field.getParamId());
                    if (Json.isNullNumber(d)) {
                        pw.append("null");
                    } else {
                        pw.append(Double.toString(d));
                    }
                }
            } else {
                if (field.isTypeString() || field.isTypeDate()) {
                    Json.quote(pw,
                               getter.getStringValue(record, field,
                                   visitInfo));
                    //                    pw.append(QUOTE);
                    //                    pw.append(getter.getStringValue(record, field,
                    //                            visitInfo));
                    //                    pw.append(QUOTE);
                } else {
                    d = getter.getValue(record, field, visitInfo);
                    if (Json.isNullNumber(d)) {
                        pw.append("null");
                    } else {
                        pw.append(Double.toString(d));
                    }
                }
            }
            fieldCnt++;
        }

        if (addGeo) {
            pw.append(COMMA);
            d = pointRecord.getLatitude();
            if (Json.isNullNumber(d)) {
                pw.append("null");
            } else {
                pw.append(Double.toString(d));
            }
            pw.append(COMMA);
            d = pointRecord.getLongitude();
            if (Json.isNullNumber(d)) {
                pw.append("null");
            } else {
                pw.append(Double.toString(d));
            }
        }

        if (addElevation) {
            pw.append(COMMA);
            d = pointRecord.getAltitude();
            if (Json.isNullNumber(d)) {
                pw.append("null");
            } else {
                pw.append(Double.toString(d));
            }
        }

        if (addTime) {
            pw.append(COMMA);
            //Just use the milliseconds
            pw.append(Json.formatNumber(pointRecord.getRecordTime()));
        }

        pw.append(Json.LIST_CLOSE);
        pw.append(Json.MAP_CLOSE);
        cnt++;

        if (debug) {
            System.err.println("\tdone");
        }

        return true;

    }

    /**
     * _more_
     *
     * @param file _more_
     * @param visitInfo _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void finished(RecordFile file, VisitInfo visitInfo)
            throws Exception {
        super.finished(file, visitInfo);
        if (pw != null) {
            RecordField.addJsonFooter(pw);
        } else if (fields == null) {
            pw = getThePrintWriter();
            String       code = "nodata";
            StringBuffer json = new StringBuffer();
            pw.append(Json.map("warning", Json.quote("No data available"),
                               "errorcode", Json.quote(code)));

        }
    }

}
