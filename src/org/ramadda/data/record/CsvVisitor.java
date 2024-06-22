/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
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

    private String extraHeader;
    private String extraLine;


    /** _more_ */
    private String altHeader = null;

    /** _more_ */
    private boolean fullHeader = false;


    /**
     * Interface description
     *
     *
     * @author         Enter your name here...
     */
    public interface HeaderPrinter {

        /**
         * _more_
         *
         *
         * @param visitor _more_
         * @param pw _more_
         * @param fields _more_
         */
        public void call(CsvVisitor visitor, PrintWriter pw,
                         List<RecordField> fields);
    }

    /**
     * Interface description
     *
     *
     * @author         Enter your name here...
     */
    public interface LineEnder {

        /**
         * _more_
         *
         * @param visitor _more_
         * @param pw _more_
         * @param fields _more_
         * @param record _more_
         * @param cnt _more_
         */
        public void call(CsvVisitor visitor, PrintWriter pw,
                         List<RecordField> fields, BaseRecord record,
                         int cnt);
    }



    /**  */
    private HeaderPrinter headerPrinter;

    /**  */
    private LineEnder lineEnder;

    /**
     * _more_
     *
     * @param pw _more_
     * @param fields _more_
     */
    public CsvVisitor(PrintWriter pw, List<RecordField> fields) {
        this(pw, fields, null, null);
    }

    /**
     *
     *
     * @param pw _more_
     * @param fields _more_
     * @param headerPrinter _more_
     * @param lineEnder _more_
     */
    public CsvVisitor(PrintWriter pw, List<RecordField> fields,
                      HeaderPrinter headerPrinter, LineEnder lineEnder) {
        this.headerPrinter = headerPrinter;
        this.lineEnder     = lineEnder;
        this.pw            = pw;
        this.fields        = fields;
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

    /**  */
    private String encodedDelimiter = Utils.hexEncode(COLUMN_DELIMITER);

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



    /**  */
    private int recordCnt = 0;

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
                               BaseRecord record)
            throws Exception {

        recordCnt++;
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
            } else if (headerPrinter != null) {
                headerPrinter.call(this, pw, fields);
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
		if(extraHeader!=null) {
		    pw.append(",");
		    pw.append(extraHeader);
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
		if(extraHeader!=null) {
		    pw.append(",");
		    pw.append(extraHeader);
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
                    if (svalue.indexOf(COLUMN_DELIMITER) > 0) {
                        //                      svalue = svalue.replaceAll(COLUMN_DELIMITER,
                        //                                                 encodedDelimiter);
                        pw.append("\"");
                        pw.append(svalue);
                        pw.append("\"");
                    } else {
                        pw.append(svalue);
                    }
                } else if (field.isTypeDate()) {
                    Object object = record.getObjectValue(field.getParamId());
                    if (object != null) {
                        pw.append(Utils.formatIso((Date) object));
                    }
                } else {
                    double value = record.getValue(field.getParamId());
                    pw.append(Double.toString(value));
                }
            } else {
                String svalue = getter.getStringValue(record, field,
                                    visitInfo);
                if (svalue.indexOf(COLUMN_DELIMITER) > 0) {
                    pw.append("\"");
                    pw.append(svalue);
                    pw.append("\"");
                } else {
                    pw.append(svalue);
                }
            }
        }
        if (lineEnder != null) {
            lineEnder.call(this, pw, fields, record, cnt);
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

    /**
       Set the ExtraHeader property.

       @param value The new value for ExtraHeader
    **/
    public void setExtraHeader (String value) {
	extraHeader = value;
    }

    /**
       Get the ExtraHeader property.

       @return The ExtraHeader
    **/
    public String getExtraHeader () {
	return extraHeader;
    }

    /**
       Set the ExtraLine property.

       @param value The new value for ExtraLine
    **/
    public void setExtraLine (String value) {
	extraLine = value;
    }

    /**
       Get the ExtraLine property.

       @return The ExtraLine
    **/
    public String getExtraLine () {
	return extraLine;
    }




}
