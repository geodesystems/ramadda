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
import org.ramadda.util.JsonUtil;
import org.ramadda.util.Utils;
//import ucar.nc2.ft.point.writer.CFPointObWriter;
//import ucar.nc2.ft.point.writer.PointObVar;

import java.io.OutputStream;
import java.io.PrintWriter;

import java.util.ArrayList;
import java.util.List;

public class JsonVisitor extends BridgeRecordVisitor {
    private static final String COMMA_NL = ",\n";
    private static final String COMMA = ",";    
    private static final String QUOTE = "\"";
    private static final String VALUES_OPEN = JsonUtil.MAP_OPEN+ QUOTE+JsonUtil.FIELD_VALUES+"\":"+JsonUtil.LIST_OPEN;
    private static final String VALUES_CLOSE = JsonUtil.LIST_CLOSE+JsonUtil.MAP_CLOSE;
    private int rowCnt = 0;
    private List<RecordField> fields;
    private List<RecordField> fieldsToUse;
    private Appendable pw;
    private OutputStream os;
    private boolean initParams = false;
    private boolean addElevation;
    private boolean addGeo;
    private boolean addTime;

    public JsonVisitor(RecordOutputHandler handler, Request request,
                       Object processId, Entry mainEntry, String suffix) {
        super(handler, request, processId, mainEntry, suffix);
    }

    private void write(String s) throws Exception {
	pw.append(s);
    }

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
	//	if(rowCnt==2) System.err.println(Utils.getStack(10));
	//	if((rowCnt%5000)==0) System.err.println("cnt:" + rowCnt);
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
                    JsonUtil.quote(pw, record.getStringValue(field.getParamId()));
                } else if (field.isTypeDate()) {
                    write(QUOTE);
                    write(record.getStringValue(field.getParamId()));
                    write(QUOTE);
                } else {
                    d = record.getValue(field.getParamId());
                    if (JsonUtil.isNullNumber(d)) {
                        write(JsonUtil.NULL);
                    } else {
                        write(Double.toString(d));
                    }
                }
            } else {
                if (field.isTypeString() || field.isTypeDate()) {
		    JsonUtil.quote(pw,  getter.getStringValue(record, field,visitInfo));
                } else {
                    d = getter.getValue(record, field, visitInfo);
                    if (JsonUtil.isNullNumber(d)) {
			write(JsonUtil.NULL);
                    } else {
			write(Double.toString(d));
                    }
                }
            }
        }

        if (addGeo) {
            write(COMMA);
            d = pointRecord.getLatitude();
            if (JsonUtil.isNullNumber(d)) {
                write(JsonUtil.NULL);
            } else {
                write(Double.toString(d));
            }
            write(COMMA);
            d = pointRecord.getLongitude();
            if (JsonUtil.isNullNumber(d)) {
                write(JsonUtil.NULL);
            } else {
                write(Double.toString(d));
            }
        }

        if (addElevation) {
            write(COMMA);
            d = pointRecord.getAltitude();
            if (JsonUtil.isNullNumber(d)) {
                write(JsonUtil.NULL);
            } else {
                write(Double.toString(d));
            }
        }

        if (addTime) {
            write(COMMA);
            //Just use the milliseconds
            write(JsonUtil.formatNumber(pointRecord.getRecordTime()));
        }
        write(VALUES_CLOSE);
        return true;
    }

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
            pw.append(JsonUtil.map(Utils.makeListFromValues("warning", JsonUtil.quote("No data available"),
						  "errorcode", JsonUtil.quote(code))));

        }
    }

}
