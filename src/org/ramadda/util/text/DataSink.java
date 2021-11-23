/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util.text;


import org.ramadda.util.Json;


import org.ramadda.util.Utils;
import org.ramadda.util.sql.SqlUtil;

import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.sql.*;


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;


/**
 *
 * @author Jeff McWhirter
 */

public abstract class DataSink extends Processor implements Cloneable,
        CsvPlugin {


    /**
     * _more_
     */
    public DataSink() {}

    /**
     
     *
     * @param csvUtil _more_
     */
    public DataSink(CsvUtil csvUtil) {
        this.csvUtil = csvUtil;
    }




    /**
     *
     * @param csvUtil _more_
     * @param arg _more_
     *
     * @return _more_
     */
    public abstract boolean canHandle(CsvUtil csvUtil, String arg);


    /**
     *
     * @param csvUtil _more_
     * @param args _more_
     * @param index _more_
     *
     * @return _more_
     */
    public int processArgs(CsvUtil csvUtil, List<String> args, int index) {
        return index;
    }


    /**
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public DataSink cloneMe() throws Exception {
        return (DataSink) this.clone();
    }

    /**
     * _more_
     *
     * @param ctx _more_
     * @param row _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Row processRow(TextReader ctx, Row row) throws Exception {
        return row;
    }


    /**
     * _more_
     *
     * @param ctx _more_
     * @param rows _more_
     *
     *
     * @return _more_
     * @throws Exception On badness
     */
    public void finish(TextReader ctx) throws Exception {}



    /**
     * Class description
     *
     *
     * @version        $version$, Sat, Apr 3, '21
     * @author         Enter your name here...
     */
    public static class ToJson extends Processor {

        /** _more_ */
        private Row headerRow;

        /**
         * ctor
         */
        public ToJson() {}

        /**
         * _more_
         *
         * @param ctx _more_
         * @param row _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) throws Exception {
            if (headerRow == null) {
                headerRow = row;
                ctx.getWriter().println("[");

                return row;
            }
            handleRow(ctx, ctx.getWriter(), row);

            return row;
        }



        /**
         * _more_
         *
         *
         * @param ctx _more_
         * @throws Exception _more_
         */
        @Override
        public void finish(TextReader ctx) throws Exception {
            super.finish(ctx);
            ctx.getWriter().println("]");
        }


        /**
         * _more_
         *
         *
         * @param ctx _more_
         * @param writer _more_
         * @param row _more_
         *
         * @throws Exception _more_
         */
        private void handleRow(TextReader ctx, PrintWriter writer, Row row)
                throws Exception {
            rowCnt++;
            if (rowCnt > 1) {
                writer.println(",");
            }
            List<String> attrs = new ArrayList<String>();
            for (int i = 0; i < headerRow.size(); i++) {
                String field = headerRow.getString(i);
                String value = row.getString(i);
                attrs.add(field);
                attrs.add(value);
            }
            writer.print(Json.mapAndGuessType(attrs));
        }

    }






    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Jan 16, '15
     * @author         Enter your name here...
     */
    public static class ToXml extends Processor {

        /** _more_ */
        Row header = null;

        /** _more_ */
        String tag;

        /**  */
        String tag2;

        /**
         * _more_
         *
         * @param tag _more_
         * @param tag2 _more_
         */
        public ToXml(String tag, String tag2) {
            super();
            if ((tag == null) || (tag.trim().length() == 0)) {
                tag = "rows";
            }
            if ((tag2 == null) || (tag2.trim().length() == 0)) {
                tag2 = "row";
            }
            this.tag  = tag;
            this.tag2 = tag2;
        }


        /**
         * _more_
         *
         * @param ctx _more_
         * @param rows _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        @Override
        public void finish(TextReader ctx) throws Exception {
            PrintWriter writer = ctx.getWriter();
            writer.println("</" + tag + ">");
        }

        /**
         * _more_
         *
         * @param ctx _more_
         * @param row _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) throws Exception {
            PrintWriter writer = ctx.getWriter();
            if (header == null) {
                header = row;
                writer.println("<" + tag + ">");

                return row;
            }
            writer.println("<" + tag2 + ">");
            List values = row.getValues();
            for (int i = 0; i < values.size(); i++) {
                Object v = values.get(i);
                String h = (String) header.get(i);
                h = h.trim().toLowerCase().replaceAll(" ",
                        "_").replaceAll("/", "_");
                if (h.length() == 0) {
                    continue;
                }
                writer.print("<" + h + ">");
                writer.print("<![CDATA[");
                writer.print(v.toString().trim());
                writer.print("]]>");
                writer.println("</" + h + ">");
            }
            writer.println("</" + tag2 + ">");

            return row;
        }

    }



    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Jan 16, '15
     * @author         Enter your name here...
     */
    public static class ToDb extends Processor {

        /**  */
        Connection connection;

        PreparedStatement statement;

	Hashtable<String,String> info;	

	Row header;
	
	String db;
	
        /**  */
        String table;

        /**  */
        Hashtable<String, String> props;

	List<String[]> columns;
	List<String> dbColumns;

	String insert;

	
        /**
         * _more_
         *
         * @param tag _more_
         * @param tag2 _more_
         *
         * @param csvUtil _more_
         * @param table _more_
         * @param props _more_
         */
        public ToDb(CsvUtil csvUtil, String db, String table, String columns,
                    Hashtable<String, String> props) {
            super(csvUtil);
	    //_default_
	    //c1,c2,c3
	    //from1:c2,
	    this.columns = new ArrayList<String[]>();
	    for(String tok:Utils.split(columns,",",true,true)) {
		List<String> tok2 = Utils.split(tok,":",true,true);
		this.columns.add(new String[]{tok2.get(0).toLowerCase(),tok2.size()==1?tok2.get(0):tok2.get(1)});
	    }
	    
	    this.db  = db;
            this.props = props;
            this.table = table;
        }


        /**
         * _more_
         *
         * @param ctx _more_
         * @param rows _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        @Override
        public void finish(TextReader ctx) throws Exception {
            super.finish(ctx);
	    if(statement!=null) {
		statement.close();
	    }
            if (connection != null) {
                connection.close();
            }
        }

        /**
          * @return _more_
         */
        private void init(TextReader ctx, Row row) throws Exception {
	    this.header = row;
	    this.connection = csvUtil.getDbConnection(ctx, this, props,db, table);
	    //"" -> db1,db2,...
	    if(columns.size()==0) {
		try {
		    List values = row.getValues();
		    dbColumns = SqlUtil.getColumnNames(connection, table, false);
		    if(values.size()!=dbColumns.size()) {
			fatal(ctx, "Mismatch between row:" + values +" and database:" + dbColumns +" You need to specify columns");
		    }
		    for(int i=0;i<values.size();i++)
			columns.add(new String[]{values.get(i).toString().toLowerCase(),dbColumns.get(i)});
		    
		} catch(Exception exc) {
		    fatal(ctx, "Error reading columns:" + exc);
		}
	    }  else if(columns.size()==1 && columns.get(0)[0].equals("_default_")) {
		dbColumns = Utils.split(Utils.join(row.getValues(),","),",");
	    } else {
		dbColumns=new ArrayList<String>();
		for(String[] tuple: columns) {
		    dbColumns.add(tuple[1]);
		}
	    }

	    insert = SqlUtil.makeInsert(table, dbColumns);
	    //	    System.err.println("Q: "+ insert);
	    try {
		info =  SqlUtil.getColumnInfo(connection, table);
		statement = connection.prepareStatement(insert);
	    } catch(Exception exc) {
		StringBuilder sb = new StringBuilder();
		List<String> dbcols = SqlUtil.getColumnNames(connection, table, false);
		sb.append("\ndata columns:" + Utils.join(row.getValues(),",")+"\n");
		sb.append("db columns:" + Utils.join(dbcols,",") +"\n");
		StringBuilder sb2  = new StringBuilder();
		List values = row.getValues();
		for(int i=0;i<values.size();i++) {
		    if(i>0) sb2.append(",");
		    sb2.append(values.get(i) +":'db col'");
		}
		sb.append("try: -todb " + db +" " + table + " \"" + sb2 +"\" \n");
		fatal(ctx, "Error creating statement:\n"+ insert +" error:" + exc +sb);
	    }
        }


        /**
         * _more_
         *
         * @param ctx _more_
         * @param row _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) throws Exception {
	    if(rowCnt++==0) {
		init(ctx, row);
		return row;
	    }
	    if(row.size()!=header.size()) fatal(ctx, "#data columns:" + row.size() +" != # header columns:" + header.size());
	    Hashtable<String,Object> valueMap = new Hashtable<String,Object>();
	    for(int i=0;i<row.size();i++) {
		valueMap.put(header.getString(i).toLowerCase(),row.get(i));
	    }

	    System.err.println("db:" + insert);
	    for(int i=0;i<columns.size();i++) {
		String[]tuple  = columns.get(i);
		String column = tuple[1];
		String type  = info.get(column);
		if(type==null) {
		    type  = info.get(column.toUpperCase());
		}
		if(type==null) {
		    type  = info.get(column.toLowerCase());
		}		
		if(type==null) {
		    fatal(ctx, "No column in table:" + column +" types:" + info);
		}
		Object o = valueMap.get(tuple[0]);
		if(o==null) fatal(ctx, "No value found for column:" + tuple[0]);
		System.err.println("\tdata:" + tuple[0] +" value:" + o + " column:"  + column +" type:" + type);
		if(type.equals("integer")) o = new Integer((int)Double.parseDouble(o.toString()));
		else if(type.equals("double")) o = Double.parseDouble(o.toString());		
		SqlUtil.setValue(statement,o,i+1);
	    }
	    statement.execute();
            return row;
        }

    }






}
