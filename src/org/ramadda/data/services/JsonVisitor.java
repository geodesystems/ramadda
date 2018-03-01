/*
* Copyright (c) 2008-2018 Geode Systems LLC
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

package org.ramadda.data.services;


import org.ramadda.data.point.PointRecord;
import org.ramadda.data.record.Record;
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
    private int cnt = 0;

    /** _more_ */
    private List<RecordField> fields;

    /** _more_ */
    private PrintWriter pw;


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
                                 Record record)
            throws Exception {

        if ( !getHandler().jobOK(getProcessId())) {
            return false;
        }
        PointRecord pointRecord = (PointRecord) record;

        boolean addElevation = file.getProperty("output.elevation", true)
                               && pointRecord.isValidAltitude();

        boolean addGeo = file.getProperty("output.latlon", true)
                         && pointRecord.isValidPosition();

        boolean addTime = file.getProperty("output.time", true)
                          && pointRecord.hasRecordTime();
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

        pw.append(Json.mapOpen());

        pw.append(Json.mapKey(Json.FIELD_VALUES));
        pw.append(Json.listOpen());
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
            String      svalue;
            ValueGetter getter = field.getValueGetter();
            if (getter == null) {
                if (field.isTypeString()) {
                    svalue = record.getStringValue(field.getParamId());
                    svalue = Json.quote(svalue);
                } else if (field.isTypeDate()) {
                    svalue = record.getStringValue(field.getParamId());
                    svalue = Json.quote(svalue);
                } else {
                    double value = record.getValue(field.getParamId());
                    svalue = Json.formatNumber(value);
                }
            } else {
                if (field.isTypeString() || field.isTypeDate()) {
                    svalue = getter.getStringValue(record, field, visitInfo);
                    svalue = Json.quote(svalue);
                } else {
                    svalue = Json.formatNumber(getter.getValue(record, field,
                            visitInfo));
                }
            }
            if (fieldCnt > 0) {
                pw.append(COMMA);
            }
            pw.append(svalue);
            fieldCnt++;
        }


        if (addGeo) {
            pw.append(COMMA);
            pw.append(Json.formatNumber(pointRecord.getLatitude()));
            pw.append(COMMA);
            pw.append(Json.formatNumber(pointRecord.getLongitude()));
        }
        if (addElevation) {
            pw.append(COMMA);
            pw.append(Json.formatNumber(pointRecord.getAltitude()));
        }
        if (addTime) {
            pw.append(COMMA);
            //                pw.append(Json.quote(DateUtil.getTimeAsISO8601(pointRecord.getRecordTime())));
            //Just use the milliseconds
            pw.append(Json.formatNumber(pointRecord.getRecordTime()));
        }



        pw.append(Json.listClose());
        pw.append(Json.mapClose());
        cnt++;

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
        }
    }

}
