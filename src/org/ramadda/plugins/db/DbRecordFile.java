/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.db;


import org.ramadda.data.point.text.*;




import org.ramadda.data.record.*;
import org.ramadda.data.services.PointOutputHandler;
import org.ramadda.data.services.PointTypeHandler;
import org.ramadda.data.services.RecordTypeHandler;

import org.ramadda.repository.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.Utils;
import org.ramadda.util.sql.*;

import ucar.unidata.util.Misc;


import java.io.*;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.TimeZone;


/**
 * Class description
 *
 *
 * @version        $version$, Sat, Dec 8, '18
 * @author         Enter your name here...
 */
public class DbRecordFile extends CsvFile implements DbConstants {

    /** _more_ */
    private Request request;

    /** _more_ */
    private DbTypeHandler typeHandler;

    /** _more_ */
    private Entry entry;

    /**
     * _more_
     *
     * @param request _more_
     * @param typeHandler _more_
     * @param entry _more_
     *
     * @throws IOException _more_
     */
    public DbRecordFile(Request request, DbTypeHandler typeHandler,
                        Entry entry)
            throws IOException {
        this.request     = request;
        this.typeHandler = typeHandler;
        this.entry       = entry;
    }

    /**
     * _more_
     *
     * @param visitInfo _more_
     * @param record _more_
     * @param howMany _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public boolean skip(VisitInfo visitInfo, BaseRecord record, int howMany)
            throws Exception {
        //noop as the DB call does this
        return true;
    }


    /*
      @Override
      public BaseRecord doMakeRecord(VisitInfo visitInfo) {
      RowRecord record = new RowRecord(this, getFields()) {
      public Row readNextRow(RecordIO recordIO) {
      return null;
      }

      };
      return record;
      }*/


    /**
     * _more_
     *
     * @param buffered _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public InputStream doMakeInputStream(boolean buffered) throws Exception {
        boolean debug = false;
        makeFields(request);
        StringBuilder s     = new StringBuilder("#converted stream\n");
        List<Clause>  where = new ArrayList<Clause>();
        where.add(Clause.eq(DbTypeHandler.COL_ID, entry.getId()));
        StringBuilder searchCriteria = new StringBuilder();
        Hashtable     recordProps    = null;
        try {
            recordProps = typeHandler.getRecordProperties(entry);
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
        for (Column column : typeHandler.getDbColumns()) {
            String dflt = (String) ((recordProps == null)
                                    ? null
                                    : recordProps.get(column.getName()
                                        + ".default"));
            if (dflt != null) {
                String arg = column.getSearchArg();
                if ( !request.exists(arg)) {
                    request.put(arg, dflt);
                }
            }
            column.assembleWhereClause(request, where, searchCriteria);
        }


        boolean      doGroupBy = typeHandler.isGroupBy(request);
        List<Column> columns;
        if (doGroupBy) {
            columns = typeHandler.getGroupByColumns(request, true);
        } else {
            columns = typeHandler.getColumnsToUse(request, false);
        }


        final PipedOutputStream pos = new PipedOutputStream();
        final PipedInputStream  pis = new PipedInputStream(pos);
        final BridgeIterator bridge = new BridgeIterator(request,
							 typeHandler, entry, pos, columns);

        Misc.run(new Runnable() {
            public void run() {
                try {
                    typeHandler.readValues(request, entry, Clause.and(where),
                                           bridge);
                } catch (Exception exc) {
                    System.err.println("db bridge error:" + exc);
                    exc.printStackTrace();
                }
            }
        });

        return pis;
    }

    /**
     * _more_
     *
     * @param request _more_
     *
     * @throws Exception _more_
     */
    private void makeFields(Request request) throws Exception {
        boolean      debug     = false;
        boolean      doGroupBy = typeHandler.isGroupBy(request);
        List<Column> columns;
        if (doGroupBy) {
            columns = typeHandler.getGroupByColumns(request, true);
        } else {
            columns = typeHandler.getColumnsToUse(request, false);
        }
        StringBuilder fields = new StringBuilder();
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) {
                fields.append(",");
            }
            Column column  = columns.get(i);
            String colType = column.getType();
            String type    = colType.equals(Column.DATATYPE_INT)
                             ? RecordField.TYPE_INT
                             : column.isNumeric()
		? RecordField.TYPE_DOUBLE
		: column.isDate()
                                 ? RecordField.TYPE_DATE
                                 : RecordField.TYPE_STRING;
            String extra   = "";
            if (column.isNumeric()) {
                extra += attrChartable();
            } else if (type.equals(RecordField.TYPE_DATE)) {
                extra += " " + attrFormat("yyyy-MM-dd'T'HH:mm:ssZ");
            } else if (column.getName().equals("latitude")) {
                type = RecordField.TYPE_DOUBLE;
            } else if (column.getName().equals("longitude")) {
                type = RecordField.TYPE_DOUBLE;
            }
            if (colType.equals(Column.DATATYPE_LATLON)) {
                fields.append(makeField("latitude",
                                        attrType(RecordField.TYPE_DOUBLE),
                                        attrLabel("Latitude")));
                fields.append(",");
                fields.append(makeField("longitude",
                                        attrType(RecordField.TYPE_DOUBLE),
                                        attrLabel("Longitude")));
            } else if (colType.equals(Column.DATATYPE_LATLONBBOX)) {
                fields.append(makeField("north",
                                        attrType(RecordField.TYPE_DOUBLE),
                                        attrLabel("North")));
                fields.append(",");
                fields.append(makeField("west",
                                        attrType(RecordField.TYPE_DOUBLE),
                                        attrLabel("West")));
                fields.append(",");
                fields.append(makeField("south",
                                        attrType(RecordField.TYPE_DOUBLE),
                                        attrLabel("South")));
                fields.append(",");
                fields.append(makeField("east",
                                        attrType(RecordField.TYPE_DOUBLE),
                                        attrLabel("East")));

            } else {
                fields.append(makeField(column.getName(), attrType(type),
                                        attrLabel(column.getLabel()), extra));
            }
        }
        if (debug) {
            System.err.println("fields:" + fields.toString());
        }
        putProperty(PROP_FIELDS, fields.toString());
    }

    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Nov 4, '21
     * @author         Enter your name here...
     */
    public static class BridgeIterator extends ValueIterator {

        /** _more_ */
        List<Column> columns;

        /** _more_ */
        OutputStream os;

        /** _more_ */
        PrintWriter pw;

        /**
         * _more_
         *
         * @param request _more_
         * @param db _more_
         * @param entry _more_
         * @param os _more_
         * @param columns _more_
         *
         * @throws Exception _more_
         */
        public BridgeIterator(Request request, DbTypeHandler db, Entry entry,
                              OutputStream os, List<Column> columns)
                throws Exception {
            super(request, db, entry);
            this.os      = os;
            pw           = new PrintWriter(os);
            this.columns = columns;
        }

	int rowCnt = 0;

        /**
         * _more_
         *
         * @param request _more_
         * @param values _more_
         *
         * @throws Exception _more_
         */
        public void processRow(Request request, Object[] values)
                throws Exception {
            int cnt = 0;
	    rowCnt++;
            for (Column c : columns) {
                List<String> names = c.getColumnNames();
                for (int idx = 0; idx < names.size(); idx++) {
                    if (cnt > 0) {
                        pw.append(",");
                    }
                    cnt++;
                    Object o = values[c.getOffset() + idx];
                    if (o instanceof String) {
                        String  str         = (String) o;
                        boolean needToQuote = false;
                        if (str.indexOf("\n") >= 0) {
                            needToQuote = true;
                        } else if (str.indexOf(",") >= 0) {
                            needToQuote = true;
                        }
                        if (str.indexOf("\"") >= 0) {
                            str         = str.replaceAll("\"", "\"\"");
                            needToQuote = true;
                        }
                        if (needToQuote) {
                            pw.append('"');
                            pw.append(str);
                            pw.append('"');
                        } else {
                            pw.append(str);
                        }
                    } else if (o instanceof Date) {
                        Date dttm = (Date) o;
			//			if(rowCnt<3)  System.err.println("\ttype:date "+ Utils.formatIso(dttm));
			String s = Utils.formatIso(dttm);
			//			System.err.println("\t" +  c + " date:" + o +" formatted:" + s);
                        pw.append(s);
                    } else {
                        pw.append(o.toString());
                    }
                }
            }
            pw.append("\n");
        }

        /**
         *
         * @param request _more_
         *
         * @throws Exception _more_
         */
        public void finish(Request request) throws Exception {
            pw.flush();
            os.close();
        }



    }



}
