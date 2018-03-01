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

package org.ramadda.data.record;


import org.ramadda.data.record.filter.*;
import org.ramadda.util.IsoUtil;
import org.ramadda.util.Utils;

import java.io.*;

import java.net.URL;
import java.net.URLConnection;

import java.util.ArrayList;

import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.zip.*;



/**
 * Class description
 *
 *
 * @version        $version$, Tue, Feb 14, '12
 * @author         Enter your name here...
 */
public class CsvVisitor extends RecordVisitor {

    /** _more_ */
    private PrintWriter pw;

    /** _more_ */
    private List<RecordField> fields;

    /** _more_ */
    private boolean printedHeader = false;

    /** _more_ */
    private String altHeader = null;

    /** _more_ */
    private boolean fullHeader = false;

    /**
     * _more_
     *
     * @param pw _more_
     * @param fields _more_
     */
    public CsvVisitor(PrintWriter pw, List<RecordField> fields) {
        this.pw     = pw;
        this.fields = fields;
    }

    /** _more_ */
    public static final String PROP_FIELDS = "fields";

    /** _more_ */
    public static final String PROP_GENERATOR = "generator";

    /** _more_ */
    public static final String PROP_CREATE_DATE = "create_date";

    /** _more_ */
    public static final String PROP_CRS = "crs";

    /** _more_ */
    public static final String PROP_DELIMITER = "delimiter";

    /** _more_ */
    public static final String PROP_MISSING = "missing";

    /** _more_ */
    public static final String PROP_SOURCE = "source";

    /** _more_ */
    private static final String LINE_DELIMITER = "\n";

    /** _more_ */
    private static final String COLUMN_DELIMITER = ",";

    /** _more_ */
    private static final String MISSING = "NaN";

    /**
     * _more_
     *
     * @param comment _more_
     */
    private void comment(String comment) {
        pw.append("#");
        pw.append(comment);
        pw.append(LINE_DELIMITER);
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     */
    private void property(String name, String value) {
        pw.append("#");
        pw.append(name);
        pw.append("=");
        pw.append(value);
        pw.append(LINE_DELIMITER);
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
    public boolean visitRecord(RecordFile file, VisitInfo visitInfo,
                               Record record)
            throws Exception {
        if (fields == null) {
            fields = record.getFields();
        }
        int cnt = 0;
        if ( !printedHeader) {
            printedHeader = true;
            cnt           = 0;
            if (altHeader != null) {
                if ( !altHeader.equals("none") && (altHeader.length() > 0)) {
                    pw.append(altHeader);
                    pw.append("\n");
                }
            } else if ( !fullHeader) {
                for (RecordField field : fields) {
                    //Skip the fake ones
                    if (field.getSynthetic()) {
                        continue;
                    }
                    //Skip the arrays
                    if (field.getArity() > 1) {
                        continue;
                    }
                    if (cnt > 0) {
                        pw.append(COLUMN_DELIMITER);
                    }
                    cnt++;
                    pw.append(field.getName());
                }
                pw.append("\n");
            } else {
                comment("");
                pw.append("#" + PROP_FIELDS + "=");
                for (RecordField field : fields) {
                    //Skip the fake ones
                    if (field.getSynthetic()) {
                        continue;
                    }
                    //Skip the arrays
                    if (field.getArity() > 1) {
                        continue;
                    }
                    if (cnt > 0) {
                        pw.append(COLUMN_DELIMITER);
                    }
                    cnt++;
                    field.printCsvHeader(visitInfo, pw);
                }
                pw.append("\n");
                String source = (String) visitInfo.getProperty(PROP_SOURCE);
                if (source != null) {
                    property(PROP_SOURCE, source);
                }
                property(PROP_GENERATOR, "Ramadda http://ramadda.org");
                property(PROP_CREATE_DATE, IsoUtil.format(new Date()));
                property(PROP_DELIMITER, COLUMN_DELIMITER);
                property(PROP_MISSING, MISSING);
            }
        }
        String encodedDelimiter = Utils.hexEncode(COLUMN_DELIMITER);
        cnt = 0;
        for (RecordField field : fields) {

            //Skip the fake ones
            if (field.getSynthetic()) {
                continue;
            }

            //Skip the arrays
            if (field.getArity() > 1) {
                continue;
            }
            if (cnt > 0) {
                pw.append(COLUMN_DELIMITER);
            }
            cnt++;

            ValueGetter getter = field.getValueGetter();
            if (getter == null) {
                if (field.isTypeString()) {
                    String svalue = record.getStringValue(field.getParamId());
                    svalue = svalue.replaceAll(COLUMN_DELIMITER,
                            encodedDelimiter);
                    pw.append(svalue);
                } else {
                    double value = record.getValue(field.getParamId());
                    pw.append("" + value);
                }
            } else {
                //                System.err.println("field: "+ field.getName() + " " +getter.getClass().getName());
                String svalue = getter.getStringValue(record, field,
                                    visitInfo);
                svalue = svalue.replaceAll(COLUMN_DELIMITER,
                                           encodedDelimiter);
                pw.append(svalue);
            }
        }
        pw.append("\n");

        return true;
    }

    /**
     * _more_
     *
     * @param s _more_
     */
    public void setAltHeader(String s) {
        altHeader = s;
    }

    /**
     *  Set the FullHeader property.
     *
     *  @param value The new value for FullHeader
     */
    public void setFullHeader(boolean value) {
        fullHeader = value;
    }

    /**
     *  Get the FullHeader property.
     *
     *  @return The FullHeader
     */
    public boolean getFullHeader() {
        return fullHeader;
    }




}
