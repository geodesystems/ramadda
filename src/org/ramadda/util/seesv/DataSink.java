/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util.seesv;


import org.ramadda.util.JsonUtil;
import org.ramadda.util.IO;
import ucar.unidata.util.IOUtil;
import org.ramadda.util.Utils;
import org.ramadda.util.sql.SqlUtil;
import ucar.unidata.xml.XmlUtil;
import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;


/**
 *
 * @author Jeff McWhirter
 */
public abstract  class DataSink extends Processor implements SeesvPlugin {

    public static class ToJson extends Processor {

        /** _more_ */
        private Row headerRow;

	String objCol;
	int objIndex=-1;

        /**
         * ctor
         */
        public ToJson(String objCol) {
	    this.objCol = objCol.trim();
	    if(this.objCol.length()==0) {
		this.objCol=null;
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
            if (headerRow == null) {
                headerRow = row;
                ctx.getWriter().println("[");
                return row;
            }
	    if(objCol!=null) {
		objIndex = getIndex(ctx,objCol);
		objCol=null;
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
		if(i == objIndex) continue;
		if(!headerRow.indexOk(i)) continue;
		if(!row.indexOk(i)) continue;
                String field = headerRow.getString(i);
                String value = row.getString(i);
                attrs.add(field);
                attrs.add(value);
            }
	    String obj=	JsonUtil.mapAndGuessType(attrs);
	    if(objIndex>=0) {
		obj=	JsonUtil.map(row.getString(objIndex), obj);
	    }
	    writer.print(obj);
        }

    }



    /**
     * Class description
     *
     *
     * @version        $version$, Sat, Apr 3, '21
     * @author         Enter your name here...
     */
    public static class Write extends Processor {

        /** _more_ */
        private Row headerRow;

	private String fileNameTemplate;

	private  String contentTemplate;

	private Seesv seesv;

        /**
         * ctor
         */
        public Write(Seesv seesv, String fileNameTemplate, String contentTemplate) {
	    this.seesv = seesv;
	    this.fileNameTemplate = fileNameTemplate;
	    this.contentTemplate = contentTemplate;	    
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
            if (headerRow == null) {
                headerRow = row;
                return row;
            }
	    String file = replaceMacros(fileNameTemplate, headerRow, row);
	    String v = replaceMacros(contentTemplate, headerRow, row);	    
	    file = file.replace("${row_index}",""+rowCnt);
	    v = v.replace("${row_index}",""+rowCnt);
	    rowCnt++;
	    if(!IO.okToWriteTo(file)) {
		throw new IllegalArgumentException("Cannot write to file:"
						   + file);
	    }

	    OutputStream fos = seesv.makeOutputStream(file);
	    IOUtil.write(fos,v);
	    fos.close();
	    System.err.println(file);
            return row;
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
            writer.print(JsonUtil.mapAndGuessType(attrs));
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

	List<String> ids;
	
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
		ids = new ArrayList<String>();
		for(int i=0;i<header.size();i++) {
		    String id = (String) header.get(i);
		    id = Utils.makeID(id).replace(".","_");
		    if(id.matches("^[^a-zA-Z]+.*")) {
			id = "tag_" + id;
		    }
		    ids.add(id);
		}
                writer.println("<" + tag + ">");
		for(String comment: ctx.getComments()) {
		    writer.append("<!--");
		    writer.append(comment);
		    writer.append("-->\n");
		}

                return row;
            }
            writer.println("<" + tag2 + ">");
            List values = row.getValues();
            for (int i = 0; i < values.size(); i++) {
                Object v = values.get(i);
                String id = ids.get(i);
                if (id.length() == 0) {
                    continue;
                }
                writer.print("<" + id + ">");
		String sv = v.toString().trim();
		boolean isNumber = Utils.isNumber(sv);
		if(!isNumber)
		    writer.print("<![CDATA[");
                writer.print(sv);
		if(!isNumber)
		    writer.print("]]>");
                writer.println("</" + id + ">");
            }
            writer.println("</" + tag2 + ">");

            return row;
        }

    }

    public static class ToGeojson extends Processor {

	private Row header;
	private int latitudeIndex = -1;
	private int longitudeIndex = -1;	
	private String slat;
	private String slon;	
	private boolean all;
	private List<Integer> indices;

        /**
         */
        public ToGeojson(String slat, String slon,List<String> cols) {
            super(cols);
	    all = cols.size()==1 && cols.get(0).equals("*");
	    this.slat = slat;
	    this.slon = slon;
        }


        /**
         */
        @Override
        public void finish(TextReader ctx) throws Exception {
            PrintWriter writer = ctx.getWriter();
	    writer.print("]}\n");
	    writer.flush();
	    writer.close();
        }

	private String qt(String s) {
	    s = s.replace("\\"," ");
	    s = s.replace("\"","\\\"");
	    return JsonUtil.quote(s);
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
	    if(rowCnt++==0) {
		header = row;
		latitudeIndex = getIndex(ctx,slat);
		longitudeIndex = getIndex(ctx,slon);		    
		if(latitudeIndex<0  || longitudeIndex<0) {
		    throw new IllegalArgumentException("Could not find lat/lon index in:" +row);
		}
		writer.print("{\"type\":\"FeatureCollection\",\"features\":[\n");
		return row;
            }
	    if(rowCnt>2) writer.print(",\n");
	    writer.print("{ \"type\": \"Feature\", \"properties\": {");
	    int cnt = 0;
	    if(indices==null) {
		if(all) {
		    indices = new ArrayList<Integer>();
		    for(int i=0;i<header.size();i++) {
			if(i!=latitudeIndex && i!=longitudeIndex) indices.add(i);
		    }
		} else {
		    indices = getIndices(ctx);
		}
	    }

	    for(int i: indices) {
		if(!row.indexOk(i)) continue;
		if(cnt>0) writer.print(",");
		writer.print(HU.quote(header.getString(i)) +":");
		String v = row.getString(i);
		if(Utils.isNumber(v)) {
		    writer.print(v);
		} else {
		    writer.print(qt(v));
		}
		cnt++;
	    }
	    writer.print("},\n");
	    writer.print("\"geometry\":{ \"type\": \"Point\", \"coordinates\": [" +
			 row.get(longitudeIndex) +"," + row.get(latitudeIndex)+"]}");
	    writer.print("}");
            return row;
        }

    }




    public static class ToDb extends Processor {

        /**  */
        Connection connection;

        /**  */
        PreparedStatement statement;

        /**  */
        Hashtable<String, String> info;

        /**  */
        Row header;

        /**  */
        String db;

        /**  */
        String table;

        /**  */
        Dictionary<String, String> props;

        /**  */
        List<String[]> columns;

        /**  */
        List<String> dbColumns;

        /**  */
        String insert;


        /**
         * _more_
         *
         * @param tag _more_
         * @param tag2 _more_
         * @param db _more_
         * @param columns _more_
         *
         * @param seesv _more_
         * @param table _more_
         * @param props _more_
         */
        public ToDb(Seesv seesv, String db, String table, String columns,
                    Dictionary<String, String> props) {
            super(seesv);
            //_default_
            //c1,c2,c3
            //from1:c2,
            this.columns = new ArrayList<String[]>();
            for (String tok : Utils.split(columns, ",", true, true)) {
                List<String> tok2 = Utils.split(tok, ":", true, true);
                this.columns.add(new String[] { tok2.get(0).toLowerCase(),
                        (tok2.size() == 1)
                        ? tok2.get(0)
                        : tok2.get(1) });
            }

            this.db    = db;
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
            if (statement != null) {
                statement.close();
            }
            if (connection != null) {
                connection.close();
            }
        }

        /**
         *
         * @param ctx _more_
         * @param row _more_
         *  @return _more_
         *
         * @throws Exception _more_
         */
        private void init(TextReader ctx, Row row) throws Exception {
            this.header = row;
            this.connection = seesv.getDbConnection(ctx, this, props, db,
                    table);
            //"" -> db1,db2,...
            if (columns.size() == 0) {
                try {
                    List values = row.getValues();
                    dbColumns = SqlUtil.getColumnNames(connection, table,
                            false);
                    if (values.size() != dbColumns.size()) {
                        fatal(ctx,
                              "Mismatch between row:" + values
                              + " and database:" + dbColumns
                              + " You need to specify columns");
                    }
                    for (int i = 0; i < values.size(); i++) {
                        columns.add(new String[] {
                            values.get(i).toString().toLowerCase(),
                            dbColumns.get(i) });
                    }

                } catch (Exception exc) {
                    fatal(ctx, "Error reading columns:" + exc);
                }
            } else if ((columns.size() == 1)
                       && columns.get(0)[0].equals("_default_")) {
                dbColumns = Utils.split(Utils.join(row.getValues(), ","),
                                        ",");
            } else {
                dbColumns = new ArrayList<String>();
                for (String[] tuple : columns) {
                    dbColumns.add(tuple[1]);
                }
            }

            insert = SqlUtil.makeInsert(table, dbColumns);
            //      System.err.println("Q: "+ insert);
            try {
                info      = SqlUtil.getColumnInfo(connection, table);
                statement = connection.prepareStatement(insert);
            } catch (Exception exc) {
                StringBuilder sb = new StringBuilder();
                List<String> dbcols = SqlUtil.getColumnNames(connection,
                                          table, false);
                sb.append("\ndata columns:"
                          + Utils.join(row.getValues(), ",") + "\n");
                sb.append("db columns:" + Utils.join(dbcols, ",") + "\n");
                StringBuilder sb2    = new StringBuilder();
                List          values = row.getValues();
                for (int i = 0; i < values.size(); i++) {
                    if (i > 0) {
                        sb2.append(",");
                    }
                    sb2.append(values.get(i) + ":'db col'");
                }
                sb.append("try: -todb " + db + " " + table + " \"" + sb2
                          + "\" \n");
                fatal(ctx,
                      "Error creating statement:\n" + insert + " error:"
                      + exc + sb);
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
            if (rowCnt++ == 0) {
                init(ctx, row);

                return row;
            }
            if (row.size() != header.size()) {
                fatal(ctx,
                      "#data columns:" + row.size() + " != # header columns:"
                      + header.size());
            }
            Hashtable<String, Object> valueMap = new Hashtable<String,
                                                     Object>();
            for (int i = 0; i < row.size(); i++) {
                valueMap.put(header.getString(i).toLowerCase(), row.get(i));
            }

            System.err.println("db:" + insert);
            for (int i = 0; i < columns.size(); i++) {
                String[] tuple  = columns.get(i);
                String   column = tuple[1];
                String   type   = info.get(column);
                if (type == null) {
                    type = info.get(column.toUpperCase());
                }
                if (type == null) {
                    type = info.get(column.toLowerCase());
                }
                if (type == null) {
                    fatal(ctx,
                          "No column in table:" + column + " types:" + info);
                }
                Object o = valueMap.get(tuple[0]);
                if (o == null) {
                    fatal(ctx, "No value found for column:" + tuple[0]);
                }
                System.err.println("\tdata:" + tuple[0] + " value:" + o
                                   + " column:" + column + " type:" + type);
                if (type.equals("integer")) {
                    o =  Integer.valueOf((int) Double.parseDouble(o.toString()));
                } else if (type.equals("double")) {
                    o = Double.parseDouble(o.toString());
                }
                SqlUtil.setValue(statement, o, i + 1);
            }
            statement.execute();

            return row;
        }

    }

    public static class ToUrl extends Processor {
	List<String> ids;


        /**
         * _more_
         *
         */
        public ToUrl() {
        }

        public void finish(TextReader ctx) throws Exception {
            super.finish(ctx);
            PrintWriter writer = ctx.getWriter();
	    writer.flush();
	    writer.close();
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
            if (rowCnt++ == 0) {
		ids= new ArrayList<String>();
		for(Object id:row.getValues()) {
		    ids.add(makeID(id.toString()));
		}
                return row;
            }

            PrintWriter writer = ctx.getWriter();
	    for(int i=0;i<ids.size();i++) {
		if(row.indexOk(i)) {
		    if(i>0)writer.print("&");
		    writer.print(HU.arg(ids.get(i),row.getString(i)));
		}
	    }
	    writer.println("");
            return row;
        }

    }
    





}
