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


import java.io.OutputStream;
import java.io.PrintWriter;

import java.util.ArrayList;
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

    private static final String VALUES_OPEN = Json.MAP_OPEN+ QUOTE+Json.FIELD_VALUES+"\":"+Json.LIST_OPEN;

    private static final String VALUES_CLOSE = Json.LIST_CLOSE+Json.MAP_CLOSE;


    /** _more_ */
    private int rowCnt = 0;

    /** _more_ */
    private List<RecordField> fields;

    private List<RecordField> fieldsToUse;

    /** _more_ */
    private Appendable pw;

    private OutputStream os;
    
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

    private void write(String s) throws Exception {
	pw.append(s);
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
        if ( !getHandler().jobOK(getProcessId())) {
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
	    fieldsToUse = new ArrayList<RecordField>();
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
		fieldsToUse.add(field);
	    }
        }

	if (rowCnt > 0) {
	    write(COMMA);
        }
        rowCnt++;
        write(VALUES_OPEN);
        double d = 0;
        int fieldCnt = 0;
        for (RecordField field : fieldsToUse) {
            ValueGetter getter = field.getValueGetter();
            if (fieldCnt > 0) {
		write(COMMA);
            }
            fieldCnt++;
            if (getter == null) {
                if (field.isTypeString()) {
                    Json.quote(pw, record.getStringValue(field.getParamId()));
                } else if (field.isTypeDate()) {
                    write(QUOTE);
                    write(record.getStringValue(field.getParamId()));
                    write(QUOTE);
                } else {
                    d = record.getValue(field.getParamId());
                    if (Json.isNullNumber(d)) {
                        write(Json.NULL);
                    } else {
                        write(Double.toString(d));
                    }
                }
            } else {
                if (field.isTypeString() || field.isTypeDate()) {
		    Json.quote(pw,  getter.getStringValue(record, field,visitInfo));
                } else {
                    d = getter.getValue(record, field, visitInfo);
                    if (Json.isNullNumber(d)) {
			write(Json.NULL);
                    } else {
			write(Double.toString(d));
                    }
                }
            }
        }


        if (addGeo) {
            write(COMMA);
            d = pointRecord.getLatitude();
            if (Json.isNullNumber(d)) {
                write(Json.NULL);
            } else {
                write(Double.toString(d));
            }
            write(COMMA);
            d = pointRecord.getLongitude();
            if (Json.isNullNumber(d)) {
                write(Json.NULL);
            } else {
                write(Double.toString(d));
            }
        }

        if (addElevation) {
            write(COMMA);
            d = pointRecord.getAltitude();
            if (Json.isNullNumber(d)) {
                write(Json.NULL);
            } else {
                write(Double.toString(d));
            }
        }

        if (addTime) {
            write(COMMA);
            //Just use the milliseconds
            write(Json.formatNumber(pointRecord.getRecordTime()));
        }
        write(VALUES_CLOSE);
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
