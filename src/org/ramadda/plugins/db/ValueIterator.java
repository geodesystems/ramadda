/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.db;


import org.ramadda.data.point.text.*;

import org.ramadda.repository.*;
import org.ramadda.repository.map.*;
import org.ramadda.repository.metadata.*;

import org.ramadda.repository.output.OutputHandler;
import org.ramadda.repository.type.*;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.geo.KmlUtil;
import org.ramadda.util.RssUtil;
import org.ramadda.util.Utils;
import org.ramadda.util.XmlUtils;
import org.ramadda.util.sql.*;



import org.w3c.dom.*;
import org.w3c.dom.*;

import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlEncoder;
import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

import java.util.function.*;

/**
 * Class description
 *
 *
 * @version        $version$, Tue, Nov 2, '21
 * @author         Enter your name here...    
 */
@SuppressWarnings("unchecked")
public abstract class ValueIterator implements DbConstants {

    /** _more_          */
    public static final HtmlUtils HU = null;


    /** _more_          */
    Request request;

    /** _more_          */
    DbTypeHandler db;

    /** _more_          */
    Result result;

    /** _more_          */
    Entry entry;

    /** _more_          */
    PrintWriter printWriter;

    /** _more_          */
    Appendable buffer;

    StringBuilder  sb;

    protected boolean addedHeader =false;

    /** _more_          */
    boolean doGroupBy;

    /** _more_          */
    boolean forPrint;

    boolean embedded;

    /** _more_          */
    boolean canEdit;

    /** _more_          */
    List<String> columnNames = null;

    /** _more_          */
    List<Column> columns = null;

    /** _more_          */
    int rowCnt = 0;

    /** _more_          */
    DbInfo dbInfo;

    /** _more_          */
    SimpleDateFormat dateSdf;

    SimpleDateFormat dateTimeSdf;    


    /** _more_          */
    boolean fromSearch = true;

    /**
     * _more_
     *
     * @param request _more_
     * @param db _more_
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    public ValueIterator(Request request, DbTypeHandler db, Entry entry)
            throws Exception {
        this.request = request;
        this.db      = db;
        this.entry   = entry;
        dbInfo       = db.getDbInfo();
        dateSdf          = db.getDateFormat(request,entry);
        dateTimeSdf          = db.getDateTimeFormat(request,entry);	
        forPrint     = request.get(ARG_FOR_PRINT, false);
	canEdit = db.getAccessManager().canDoEdit(request, entry);
	embedded = request.isEmbedded();

    }

    public void setLabels(List<String> labels) throws Exception  {
    }

    private String viewHeaderId;
    public void addViewHeader(Request request, Entry entry, String view,  String extraLinks, boolean nothingFound) throws Exception {
	addedHeader = true;
	Appendable sb       = getBuffer();
	if (embedded) {
	    db.addStyleSheet(request, sb);
	} else {
	    int max = db.getMax(request);
	    db.addViewHeader(request, entry, sb,view,extraLinks);
	    if(nothingFound) {
		sb.append(db.getPageHandler().showDialogNote(db.msg("Nothing found")));
	    }
	    db.addSearchAgain(request, entry,sb,!nothingFound);
	    viewHeaderId = Utils.getGuid();
	    HU.div(sb, "",  HU.id(viewHeaderId));
	    //	    db.addPrevNext(request, entry,sb,max);
	}
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public Repository getRepository() {
        return db.getRepository();
    }


    /**
     * _more_
     *
     * @param suffix _more_
     * @param mimeType _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected void makeResult(String suffix, String mimeType)
            throws Exception {
        if (result == null) {
	    if(embedded) {
		result = new Result("","");
	    } else {
		if (request.defined(ARG_DB_SEARCHNAME)) {
		    result = request.getOutputStreamResult(
							   request.getString(ARG_DB_SEARCHNAME) + suffix, mimeType);
		} else {
		    result = request.getOutputStreamResult(entry.getName()
							   + suffix, mimeType);
		}
            }
        }

    }

    /**
     * _more_
     *
     * @return _more_
     */
    public Result getResult() {
        return result;
    }

    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Appendable getBuffer() throws Exception {
        if (buffer == null) {
	    if(embedded) {
		buffer = sb = new StringBuilder();
	    } else  {
		buffer = printWriter = new PrintWriter(request.getOutputStream());
	    }
        }

        return buffer;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param doGroupBy _more_
     *
     * @throws Exception _more_
     */
    public void initialize(Request request, boolean doGroupBy)
            throws Exception {
        this.doGroupBy = doGroupBy;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param values _more_
     *
     * @throws Exception _more_
     */
    public abstract void processRow(Request request, Object[] values)
     throws Exception;


    /**
     * _more_
     *
     * @param request _more_
     *
     * @throws Exception _more_
     */
    public void finish(Request request) throws Exception {
	if(viewHeaderId!=null) {
	    StringBuilder tmp = new StringBuilder();
	    db.addPrevNext(request, entry,tmp,rowCnt);
	    String tmpId = Utils.getGuid();
	    Appendable sb       = getBuffer();
	    HU.div(sb, tmp.toString(),  HU.id(tmpId));
	    sb.append(HU.script("DB.initHeader('" + viewHeaderId + "','" + tmpId +"')"));
	}

        if (printWriter != null) {
            printWriter.flush();
        }

	if(embedded && sb!=null) {
	    result =  new Result("",sb);
	}


    }

    /**
     * Class description
     *
     *
     * @version        $version$, Tue, Nov 2, '21
     * @author         Enter your name here...    
     */
    public static class CsvIterator extends ValueIterator {

        /**
         * _more_
         *
         * @param request _more_
         * @param db _more_
         * @param entry _more_
         *
         * @throws Exception _more_
         */
        public CsvIterator(Request request, DbTypeHandler db, Entry entry)
                throws Exception {
            super(request, db, entry);
        }

        /**
         * _more_
         *
         * @param request _more_
         * @param doGroupBy _more_
         *
         * @throws Exception _more_
         */
        public void initialize(Request request, boolean doGroupBy)
                throws Exception {
            super.initialize(request, doGroupBy);
            makeResult(".csv", "text/csv");
            if ( !doGroupBy) {
                columns     = db.getColumnsToUse(request, false);
                columnNames = Column.getNames(columns);
		Appendable sb = getBuffer();
                for (int i = 0; i < columnNames.size(); i++) {
                    if (i > 0) {
                        sb.append(",");
                    }
                    sb.append(columnNames.get(i));
                }
                sb.append("\n");
            } else {
                //TODO: use these later for formatting
                List<Column> groupByColumns = db.getGroupByColumns(request,
                                                  false);
                List<Column> aggColumns = db.getAggColumns(request);
		//		System.err.println("GB:" + groupByColumns);
		//		System.err.println("agg:" + aggColumns);		
                groupByColumns.addAll(aggColumns);
            }
        }

	
	@Override
	public void setLabels(List<String> labels) throws Exception {
	    Appendable sb = getBuffer();
	    sb.append(Utils.columnsToString(labels,","));
	    sb.append("\n");
	}


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
            Appendable sb = getBuffer();
            if (doGroupBy) {
                for (int i = 0; i < values.length; i++) {
                    if (i > 0) {
                        sb.append(",");
                    }
		    Object o = values[i];
                    String s = o==null?"":values[i].toString();
                    s = s.replaceAll("\"", "\"\"\"");
                    if (s.indexOf(",") >= 0) {
                        s = "\"" + s + "\"";
                    }
                    sb.append(s);
                }
                sb.append("\n");
                return;
            }

            if (columns != null) {
                for (int i = 0; i < columns.size(); i++) {
                    StringBuilder cb = new StringBuilder();
                    columns.get(i).formatValue(request, entry, cb,
                                Column.OUTPUT_CSV, values, true);
                    String colValue = cb.toString();
                    colValue = colValue.replaceAll("\n", " ");
                    if (i > 0) {
                        sb.append(",");
                    }
                    sb.append(colValue);
                }
            } else {
                for (int i = 0; i < values.length; i++) {
                    String colValue = values[i].toString();
                    colValue = colValue.replaceAll("\n", " ");
                    if (i > 0) {
                        sb.append(",");
                    }
                    sb.append(colValue);
                }
            }
            sb.append("\n");
        }
    }

    /**
     * Class description
     *
     *
     * @version        $version$, Tue, Nov 2, '21
     * @author         Enter your name here...    
     */
    public static class JsonIterator extends ValueIterator {

        /**
         * _more_
         *
         * @param request _more_
         * @param db _more_
         * @param entry _more_
         *
         * @throws Exception _more_
         */
        public JsonIterator(Request request, DbTypeHandler db, Entry entry)
                throws Exception {
            super(request, db, entry);
        }

        /**
         * _more_
         *
         * @param request _more_
         * @param doGroupBy _more_
         *
         * @throws Exception _more_
         */
        public void initialize(Request request, boolean doGroupBy)
                throws Exception {
            super.initialize(request, doGroupBy);
            makeResult(".json", JsonUtil.MIMETYPE);
            columnNames = new ArrayList<String>();
            columns     = db.getColumnsToUse(request, false);
            for (int i = 0; i < columns.size(); i++) {
                Column c = columns.get(i);
                columnNames.add(c.getJson(request));
            }
            Appendable sb = getBuffer();
            sb.append(JsonUtil.MAP_OPEN);
            if (request.get("includeColumns", true)) {
                sb.append(JsonUtil.quote("columns"));
                sb.append(":");
                sb.append(JsonUtil.list(columnNames));
                sb.append(",\n");
            }
            sb.append(JsonUtil.quote("values"));
            sb.append(":");
            sb.append(JsonUtil.LIST_OPEN);
        }


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
            Appendable sb = getBuffer();
            if (rowCnt++ > 0) {
                sb.append(",\n");
            }
            StringBuilder cb    = new StringBuilder();
            List<String>  attrs = new ArrayList<String>();
            for (int i = 0; i < columns.size(); i++) {
                cb.setLength(0);
                columns.get(i).formatValue(request, entry, cb,
                            Column.OUTPUT_CSV, values, true);
                String colValue = cb.toString();
                attrs.add(columns.get(i).getName());
                attrs.add(JsonUtil.quote(colValue));
            }
            sb.append(JsonUtil.map(attrs));
        }

        /**
         * _more_
         *
         * @param request _more_
         *
         * @throws Exception _more_
         */
        public void finish(Request request) throws Exception {
            Appendable sb = getBuffer();
            sb.append(JsonUtil.LIST_CLOSE);
            sb.append(JsonUtil.MAP_CLOSE);
            super.finish(request);
        }




    }




    /**
     * Class description
     *
     *
     * @version        $version$, Tue, Nov 2, '21
     * @author         Enter your name here...    
     */
    public static class RssIterator extends ValueIterator {

        /** _more_ */
        SimpleDateFormat rssSdf =
            new SimpleDateFormat("EEE dd, MMM yyyy HH:mm:ss z");



        /**
         * _more_
         *
         * @param request _more_
         * @param db _more_
         * @param entry _more_
         *
         * @throws Exception _more_
         */
        public RssIterator(Request request, DbTypeHandler db, Entry entry)
                throws Exception {
            super(request, db, entry);
        }

        /**
         * _more_
         *
         * @param request _more_
         * @param doGroupBy _more_
         *
         * @throws Exception _more_
         */
        public void initialize(Request request, boolean doGroupBy)
                throws Exception {
            super.initialize(request, doGroupBy);
            makeResult(".rss", "application/rss+xml");
            columns     = db.getColumnsToUse(request, false);
            columnNames = Column.getNames(columns);
            Appendable sb = getBuffer();
            sb.append(XmlUtil.XML_HEADER + "\n");
            sb.append(XmlUtil.openTag(RssUtil.TAG_RSS,
                                      XmlUtil.attrs(ATTR_RSS_VERSION,
                                          "2.0")));
            sb.append(XmlUtil.openTag(RssUtil.TAG_CHANNEL));
            sb.append(XmlUtil.tag(RssUtil.TAG_TITLE, "", entry.getName()));
        }


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
            Appendable sb = getBuffer();
            rowCnt++;
            String label = db.getLabel(request, entry, values, null);
            Date   date  = null;
            if (dbInfo.getDateColumns().size() > 0) {
                date = (Date) values[dbInfo.getDateColumn().getOffset()];
            } else {
                date = (Date) values[IDX_DBCREATEDATE];
            }
            String dbid = (String) values[IDX_DBID];

            String info = db.getHtml(request, entry, dbid, db.getDbColumns(),
                                     values, dateSdf,dateTimeSdf,false,-1);
            sb.append(XmlUtil.openTag(RssUtil.TAG_ITEM));
            sb.append(XmlUtil.tag(RssUtil.TAG_PUBDATE, "",
                                  rssSdf.format(date)));
            sb.append(XmlUtil.tag(RssUtil.TAG_TITLE, "", label));


            String url = db.getViewUrl(request, entry,
                                       "" + values[IDX_DBID]);
            url = request.getAbsoluteUrl(url);
            sb.append(XmlUtil.tag(RssUtil.TAG_LINK, "",
                                  XmlUtil.getCdata(url)));


            sb.append(XmlUtil.tag(RssUtil.TAG_GUID, "",
                                  XmlUtil.getCdata(url)));
            sb.append(XmlUtil.openTag(RssUtil.TAG_DESCRIPTION, ""));
            XmlUtils.appendCdata(sb, info);
            sb.append(XmlUtil.closeTag(RssUtil.TAG_DESCRIPTION));
            if (dbInfo.getHasLocation()) {
                double[] ll = db.getLocation(request,values);
                sb.append(XmlUtil.tag(RssUtil.TAG_GEOLAT, "", "" + ll[0]));
                sb.append(XmlUtil.tag(RssUtil.TAG_GEOLON, "", "" + ll[1]));
            }
            sb.append(XmlUtil.closeTag(RssUtil.TAG_ITEM));
        }





        /**
         * _more_
         *
         * @param request _more_
         *
         * @throws Exception _more_
         */
        public void finish(Request request) throws Exception {
            Appendable sb = getBuffer();
            sb.append(XmlUtil.closeTag(RssUtil.TAG_CHANNEL));
            sb.append(XmlUtil.closeTag(RssUtil.TAG_RSS));
            super.finish(request);
        }
    }



    /**
     * Class description
     *
     *
     * @version        $version$, Tue, Nov 2, '21
     * @author         Enter your name here...    
     */
    public static class XxxIterator extends ValueIterator {

        /**
         * _more_
         *
         * @param request _more_
         * @param db _more_
         * @param entry _more_
         *
         * @throws Exception _more_
         */
        public XxxIterator(Request request, DbTypeHandler db, Entry entry)
                throws Exception {
            super(request, db, entry);
        }

        /**
         * _more_
         *
         * @param request _more_
         * @param doGroupBy _more_
         *
         * @throws Exception _more_
         */
        public void initialize(Request request, boolean doGroupBy)
                throws Exception {
            super.initialize(request, doGroupBy);
            makeResult(".json", "application/json");
            columns     = db.getColumnsToUse(request, false);
            columnNames = Column.getNames(columns);
            for (int i = 0; i < columns.size(); i++) {
                Column c = columns.get(i);
            }
            Appendable sb = getBuffer();
        }


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
            Appendable sb = getBuffer();
	    rowCnt++;
        }


        /**
         * _more_
         *
         * @param request _more_
         *
         * @throws Exception _more_
         */
        public void finish(Request request) throws Exception {
            Appendable sb = getBuffer();
            super.finish(request);
        }




    }


    /**
     * Class description
     *
     *
     * @version        $version$, Tue, Nov 2, '21
     * @author         Enter your name here...    
     */
    public abstract static class HtmlIterator extends ValueIterator {

	String header;

        /**
         * _more_
         *
         * @param request _more_
         * @param db _more_
         * @param entry _more_
         * @param fromSearch _more_
         *
         * @throws Exception _more_
         */
        public HtmlIterator(Request request, DbTypeHandler db, Entry entry, boolean fromSearch)
                throws Exception {
            super(request, db, entry);
	    this.fromSearch = fromSearch;
        }

        /**
         * _more_
         *
         * @param request _more_
         * @param doGroupBy _more_
         *
         * @throws Exception _more_
         */
        public void initialize(Request request, boolean doGroupBy)
                throws Exception {
            super.initialize(request, doGroupBy);
            if (forPrint) {
                request.put(ARG_TEMPLATE, "empty");
            }
            makeResult("", "text/html");
	    if(!embedded) {
		db.getEntryManager().addEntryHeader(request, entry, result);
		Appendable sb = getBuffer();
		header = db.getPageHandler().decorateResult(request,
							    result, true, false);
		if (header != null) {
		    sb.append(header);
		} else {
		    System.err.println("no header");
		}
	    }
        }


        /**
         * _more_
         *
         * @param request _more_
         *
         * @throws Exception _more_
         */
        public void finish(Request request) throws Exception {
            Appendable    sb = getBuffer();
	    if(rowCnt==0) {
                if ( !fromSearch) {
                    sb.append(HU.br());
                    sb.append(
                        db.getPageHandler().showDialogNote(
                            db.msgLabel("No entries in")
                            + db.getTitle(request, entry)));
                } else {
		    //                    sb.append(db.getPageHandler().showDialogNote(db.msg("Nothing found")));
                }
	    }

            if ( !embedded) {
                db.addViewFooter(request, entry, sb);
		String footer = db.getPageHandler().decorateResult(request,
								   result, false, true);
		if (footer != null) {
		    sb.append(footer);
		} else {}
	    }
            super.finish(request);
        }


    }





    /**
     * Class description
     *
     *
     * @version        $version$, Tue, Nov 2, '21
     * @author         Enter your name here...    
     */
    public static class TableIterator extends HtmlIterator {



        /** _more_          */
        String tableId = Utils.getGuid();


        /** _more_          */
        int lineCnt = 0;

        /** _more_          */
        String editUrl;

        /** _more_          */
        String viewUrl;

        /** _more_          */
        String editImg;

        /** _more_          */
        String viewImg;


        /** _more_          */
        boolean doForm = true;

        /** _more_          */
        boolean showHeaderLinks = true;

        /** _more_          */
        int entriesPerPage;

	boolean numberEntries = false;
	
        /** _more_          */
        String searchColumn;

        /** _more_          */
        String sourceName;

        /** _more_          */
        String sourceColumn;

        /** _more_          */
        String searchFrom;

        /** _more_          */
        StringBuilder tableHeader = new StringBuilder();

        /** _more_          */
        Hashtable entryProps;

        /** _more_          */
        double[] sum;

        /** _more_          */
        double[] min;

        /** _more_          */
        double[] max;

        /** _more_          */
        List<String> extraCols;

        /** _more_          */
        Hashtable<String, Hashtable<Object, Integer>> uniques =
            new Hashtable<String, Hashtable<Object, Integer>>();


        /**
         * _more_
         *
         * @param request _more_
         * @param db _more_
         * @param entry _more_
         * @param fromSearch _more_
         *
         * @throws Exception _more_
         */
        public TableIterator(Request request, DbTypeHandler db, Entry entry,
                             boolean fromSearch)
                throws Exception {
            super(request, db, entry,fromSearch);
            entryProps      = db.getProperties(entry);
            editImg = HU.img(
                getRepository().getUrlBase() + "/db/database_edit.png",
                "View entry");
            viewImg =
                HU.img(getRepository().getUrlBase()
                              + "/db/database_go.png", "View entry");
        }

        /**
         * _more_
         *
         * @param request _more_
         * @param doGroupBy _more_
         *
         * @throws Exception _more_
         */
        public void initialize(Request request, boolean doGroupBy)
                throws Exception {
            super.initialize(request, doGroupBy);
            entriesPerPage = request.get(ARG_ENTRIES_PER_PAGE, 8);
	    numberEntries = request.get(ARG_NUMBER_ENTRIES,false);
            extraCols = Utils.split(request.getString(ARG_EXTRA_COLUMNS, ""),
                                    "\n", true, true);
            columns     = db.getColumnsToUse(request, true);
            columnNames = Column.getNames(columns);
        }

        /**
         * _more_
         *
         * @param request _more_
         *
         * @throws Exception _more_
         */
        private void initializeTable(Request request) throws Exception {
	    if(!addedHeader) {
		addViewHeader(request, entry, VIEW_TABLE,null,false);
	    }
            Appendable sb       = getBuffer();
            if (doForm) {
                String formUrl =
                    request.makeUrl(db.getRepository().URL_ENTRY_SHOW);
                HU.form(sb, formUrl);
                HU.hidden(sb, ARG_ENTRYID, entry.getId(), "");
            }

            if (forPrint) {
                canEdit = false;
            }
            HashSet<String> except = new HashSet<String>();
            for (int i = 1; i <= db.numOrders; i++) {
                except.add(ARG_DB_SORTBY + i);
                except.add(ARG_DB_SORTDIR + i);
            }
            String baseUrl = request.getUrl(except, null);
            boolean asc = request.getString(ARG_DB_SORTDIR1,
                                            (dbInfo.getDfltSortAsc()
                                             ? "asc"
                                             : "desc")).equals("asc");

            String sortBy = request.getString(ARG_DB_SORTBY1,
                                ((dbInfo.getDfltSortColumn() == null)
                                 ? ""
                                 : dbInfo.getDfltSortColumn().getName()));

            List<TwoFacedObject> actions = new ArrayList<TwoFacedObject>();
            //TODO uncomment            if(dbInfo.getHasEmail() && getMailManager().isEmailEnabled()) {
            if (dbInfo.getHasEmail()) {
                actions.add(new TwoFacedObject("Send mail", ACTION_EMAIL));
            }
            if (canEdit) {
                actions.add(new TwoFacedObject("Delete selected",
                        ACTION_DELETE));
		if (dbInfo.getHasLocation()) {
		    actions.add(new TwoFacedObject("Set lat/lon on selected",
						   ACTION_SET_LATLON));
		}
                actions.add(new TwoFacedObject("Delete entire database",
                        ACTION_DELETEALL));
            }
            if ( !embedded && (actions.size() > 0)) {
                if (doForm) {
                    sb.append(HU.submit("Do:", ARG_DB_DO));
                    sb.append(HU.select(ARG_DB_ACTION, actions));
                }
            }
	    
            HU.open(tableHeader, "table", "entryid", entry.getId(),
                           "id", tableId, "class", "dbtable", "border", "1",
                           "cellspacing", "0", "cellpadding", "0", "width",
                           "100%");
            HU.open(tableHeader, "tr", "valign", "top");
            if ( !forPrint) {
		if(numberEntries)
		    db.makeTableHeader(tableHeader, "#");
                db.makeTableHeader(tableHeader, "&nbsp;");		
            } else {
		if(numberEntries)
		    db.makeTableHeader(tableHeader, "#",HU.attr("width","30px"));
	    }
            for (int i = 0; i < columns.size(); i++) {
                Column column = columns.get(i);
		if(column.isSynthetic()) {
		    continue;
		}
                String type;
                if (column.isNumeric()) {
                    type = "number";
                } else if (column.isBoolean()) {
                    type = "boolean";
                } else {
                    type = "string";
                }

                String label = column.getLabel();
                if ( !showHeaderLinks) {
                    db.makeTableHeader(tableHeader, label);
                    continue;
                }
                String sortColumn = column.getName();
                String extra;
                if (sortColumn.equals(sortBy)) {
                    if (asc) {
                        extra = " "
                                + HU.img(
                                    db.getRepository().getIconUrl(
                                        ICON_UPDART));
                    } else {
                        extra = " "
                                + HU.img(
                                    db.getRepository().getIconUrl(
                                        ICON_DOWNDART));
                    }
                    asc = !asc;
                } else {
                    extra = " "
                            + HU.img(
                                db.getRepository().getIconUrl(ICON_BLANK),
                                "", HU.attr("width", "10"));
                }
                String link = HU.href(baseUrl + "&" + ARG_DB_SORTBY1
                                             + "=" + sortColumn + "&"
                                             + ARG_DB_SORTDIR1 + (asc
                        ? "=asc"
								  : "=desc"), label,HU.attrs(HU.ATTR_REL,"nofollow")) + extra;
                db.makeTableHeader(tableHeader, link);
            }


            for (String col : extraCols) {
                db.makeTableHeader(tableHeader, col);
            }
            HU.close(tableHeader, "tr");



            searchFrom   = request.getString(ARG_SEARCH_FROM, null);
            searchColumn = null;
            sourceName   = null;
            sourceColumn = null;
            if (searchFrom != null) {
                List<String> toks = Utils.split(searchFrom, ";");
                sourceName   = toks.get(0);
                sourceColumn = toks.get(1);
                searchColumn = toks.get(3);
            }
            if (searchColumn != null) {
                String href =
                    HU.href(
                        "#",
                        getRepository().getIconImage(
                            "fas fa-external-link-alt") + " "
                                + "Select all for " + sourceName + ":"
                                + sourceColumn, HU.attr(
                                    "onclick",
                                    "return DB.doDbSelectAll(event,'"
                                    + searchFrom + "')") + HU.attr(
                                        "class",
                                        "ramadda-clickable") + HU.attr(
                                            "title",
                                            "Select all for " + sourceName
                                            + ":" + sourceColumn));
                String clear = HU.href("#",
                                       "Clear all " + sourceName + ":"
                                       + sourceColumn, HU.attr("onclick",
                                           "return DB.doDbClearAll(event,'"
                                           + searchFrom
                                           + "')") + HU.attr("class",
                                               "ramadda-clickable"));
                sb.append(HU.space(2) + href + HU.space(2) + clear);
            }



            sb.append(tableHeader);

            Hashtable<String, Hashtable<Object, Integer>> uniques =
                new Hashtable<String, Hashtable<Object, Integer>>();

            String popupId = "dbrowpopup_" + entry.getId();
            sb.append(HU.div("",
                                    HU.id(popupId)
                                    + HU.cssClass("ramadda-popup")));
            sum = new double[columns.size()];
            min = new double[columns.size()];
            max = new double[columns.size()];

        }



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

            Appendable sb = getBuffer();
            if (rowCnt++ == 0) {
                initializeTable(request);
            }

            lineCnt++;
            if (forPrint && (lineCnt >= entriesPerPage)) {
                lineCnt = 1;
                sb.append("</table>");
                sb.append("<div class=pagebreak></div>");
                sb.append(tableHeader);
            }
            String dbid  = (String) values[IDX_DBID];
            String cbxId = ARG_DBID + (rowCnt);
            String divId = "div_" + dbid;
            sb.append("\n");
            HU.open(sb, "tr", "dbrowid", dbid);
	    if(numberEntries)
                HU.td(sb, ""+(rowCnt),HU.attr("align","right"));
            if ( !forPrint) {
                HU.open(sb, "td", "width", "10", "style",
                               "white-space:nowrap;");
                HU.open(sb, "div", "id", divId);
                if (doForm) {
                    String call =
                        HU.attr(
                            HU.ATTR_ONCLICK,
                            HU.call(
					   "HU.checkboxClicked",
                                HU.comma(
                                    "event",
                                    HU.squote(ARG_DBID_SELECTED),
                                    HU.squote(cbxId))));

                    call = "";
                    HU.checkbox(sb, ARG_DBID_SELECTED, dbid, false,
                                       "id=" + cbxId + " " + call);
                }
                if (canEdit) {
                    if (editUrl == null) {
                        editUrl = db.getEditUrl(request, entry, "_DBROWID_");
                    }
                    HU.href(sb, editUrl.replace("_DBROWID_", dbid),
                                   editImg);
                }
                if (viewUrl == null) {
                    viewUrl = db.getViewUrl(request, entry, "_DBROWID_");
                }
                HU.href(sb, viewUrl.replace("_DBROWID_", dbid),
                               viewImg);
                HU.close(sb, "div", "td");
            }


            for (int i = 0; i < columns.size(); i++) {
                //              if(true) continue;
                Column column = columns.get(i);
		if(column.isSynthetic()) {
		    continue;
		}
                if (column.isNumeric()) {
                    Object o = values[column.getOffset()];
                    if (o != null) {
                        double v = ((o instanceof Integer)
                                    ? (double) ((Integer) o).intValue()
                                    : ((Double) o).doubleValue());
                        if (v == v) {
                            sum[i] += v;
                            min[i] = (rowCnt == 0)
                                     ? v
                                     : Math.min(min[i], v);
                            max[i] = (rowCnt == 0)
                                     ? v
                                     : Math.max(max[i], v);
                        }
                    }
                }

                if (column.isString()) {
                    HU.open(sb, "td");
                } else if (column.isNumeric()) {
                    HU.open(sb, "td", "align", "right");
                } else {
                    HU.open(sb, "td");
                }

                HU.open(sb, HU.TAG_DIV, "class", "dbcell");

                if (column.isEnumeration()) {
                    String value = (String) values[column.getOffset()];
                    if (value != null) {
                        StringBuilder prefix = null;
                        String iconID = PROP_CAT_ICON + "."
                                        + column.getName();
                        Hashtable<String, String> iconMap =
                            (Hashtable<String,
                                       String>) entryProps.get(iconID);
                        if (iconMap != null) {
                            String icon = iconMap.get(value);
                            if (icon != null) {
                                if (prefix == null) {
                                    prefix = new StringBuilder();
                                }
                                prefix.append(
                                    HU.img(
                                        db.getDbIconUrl(icon), "",
                                        HU.attr("width", "16")));
                                prefix.append(" ");

                            }
                        }
                        String style   = "";
                        String content = "&nbsp;&nbsp;&nbsp;&nbsp;";
                        String colorID = PROP_CAT_COLOR + "."
                                         + column.getName();
                        Hashtable<String, String> colorMap =
                            (Hashtable<String,
                                       String>) entryProps.get(colorID);
                        if (colorMap != null) {
                            String bgColor =
                                colorMap.get(
                                    (String) values[column.getOffset()]);
                            if (bgColor != null) {
                                style = style + "background-color:" + bgColor;
                                if (prefix == null) {
                                    prefix = new StringBuilder();
                                }
                                prefix.append(HU.span(content,
                                        HU.style(style)));
                            }
                        }
                        if (prefix != null) {
                            sb.append(prefix.toString());
                        }
                    }
                }

                String label = db.formatTableValue(request, entry, sb,
						   column, values, dateSdf, dateTimeSdf,!forPrint);
                sb.append("&nbsp;");
                boolean addSelect = (searchColumn != null)
                                    && column.getName().equals(searchColumn);
                if (addSelect) {
                    String value = "" + values[column.getOffset()];
                    sb.append("&nbsp;");
                    sb.append(
                        HU.href(
                            "#",
                            getRepository().getIconImage(
                                "fas fa-external-link-alt"), HU.attr(
                                "onclick",
                                "return DB.doDbSelect(event,'" + searchFrom
                                + "','" + label + "')") + HU.attr(
                                    "select-value", value) + HU.attr(
                                    "class",
                                    "db-select-link ramadda-clickable") + HU.attr(
                                        "title",
                                        "Select for " + sourceName + ":"
                                        + sourceColumn)));
                    sb.append("&nbsp;");
                }
                HU.close(sb, HU.TAG_DIV);
                HU.close(sb, HU.TAG_TD);

                if (column.isEnumeration() && column.getDoStats()) {
                    Hashtable<Object, Integer> numUniques =
                        uniques.get(column.getName());
                    if (numUniques == null) {
                        numUniques = new Hashtable<Object, Integer>();
                        uniques.put(column.getName(), numUniques);
                    }
                    Integer uniqueCnt = numUniques.get(label);
                    if (uniqueCnt == null) {
                        uniqueCnt = Integer.valueOf(0);
                    }
                    numUniques.put(label, uniqueCnt.intValue() + 1);
                }
            }



            sb.append("</tr>");



        }


        /**
         * _more_
         *
         * @param request _more_
         *
         * @throws Exception _more_
         */
        public void finish(Request request) throws Exception {
	    if(!addedHeader) {
		addViewHeader(request, entry, VIEW_TABLE,null,rowCnt==0);
	    }
            Appendable    sb = getBuffer();
            StringBuilder hb = new StringBuilder();
            if ( !forPrint && (rowCnt > 0)) {
                HU.comment(hb, "summmary");
                HU.open(hb, "tr", "valign", "top");
                HU.tag(hb, "td", HU.attrs("align", "right"),
                              "#" + rowCnt);
                for (int i = 0; i < columns.size(); i++) {
                    Column column = columns.get(i);
                    if (column.isNumeric() && column.getDoStats()) {
                        double  avg   = sum[i] / rowCnt;
                        boolean round = column.isInteger();
                        HU.open(hb, "td", "class", "dbtable-summary");
                        hb.append(HU.formTable());
                        hb.append(HU.formEntry("Average:",
                                db.format(avg, round)));
                        hb.append(HU.formEntry("Minimum:",
                                db.format(min[i], round)));
                        hb.append(HU.formEntry("Maximum:",
                                db.format(max[i], round)));
                        hb.append(HU.formEntry("Total:",
                                db.format(sum[i], round)));
                        HU.close(hb, "table", "td");
                    } else if (column.isEnumeration()
                               && column.getDoStats()) {
                        Hashtable<Object, Integer> numUniques =
                            uniques.get(column.getName());
                        if (numUniques == null) {
                            continue;
                        }
                        HU.open(hb, "td", "class", "dbtable-summary");
                        hb.append(HU.formTable());
                        int cnt = 0;
                        for (Enumeration keys = numUniques.keys();
                                keys.hasMoreElements(); ) {
                            if (cnt++ > 10) {
                                hb.append(HU.formEntry("", "..."));

                                break;
                            }
                            Object key   = keys.nextElement();
                            Object value = numUniques.get(key);
                            if (key.toString().length() == 0) {
                                key = "&lt;blank&gt;";
                            }
                            hb.append(HU.formEntry(key + ":",
                                    value.toString()));
                        }
                        HU.close(hb, "table", "td");
                    } else {
                        HU.tag(hb, "td", "", "&nbsp;");
                    }
                }
                for (String col : extraCols) {
                    HU.open(hb, HU.TAG_TD, "");
                    HU.close(hb, HU.TAG_TD);
                }
                HU.close(hb, "tr");
                HU.close(hb, "table");
		sb.append(HU.script("DB.initTable('" + tableId + "')"));
            } 
		hb.append(HU.formClose());
            sb.append(hb.toString());

            super.finish(request);
        }




    }



    /**
     * Class description
     *
     *
     * @version        $version$, Tue, Nov 2, '21
     * @author         Enter your name here...    
     */
    public static class TemplateIterator extends ValueIterator {

        /** _more_          */
        DbTemplate template;

        /**
         * _more_
         *
         * @param request _more_
         * @param db _more_
         * @param entry _more_
         * @param template _more_
         *
         * @throws Exception _more_
         */
        public TemplateIterator(Request request, DbTypeHandler db,
                                Entry entry,
                                DbTemplate template)
                throws Exception {
            super(request, db, entry);
            this.template = template;
        }

        /**
         * _more_
         *
         * @param request _more_
         * @param doGroupBy _more_
         *
         * @throws Exception _more_
         */
        public void initialize(Request request, boolean doGroupBy)
                throws Exception {
            super.initialize(request, doGroupBy);
            makeResult("", "");
            columns     = db.getColumnsToUse(request, false);
            columnNames = Column.getNames(columns);
            Appendable sb = getBuffer();
            sb.append(template.prefix);
        }


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
            Appendable    sb  = getBuffer();
            String        t   = template.entry;
            StringBuilder tmp = new StringBuilder();
            for (Column column : columns) {
                column.formatValue(request, entry, tmp, Column.OUTPUT_HTML,
                                   values, dateSdf, false);

                t = t.replace("${" + column.getName() + "}", tmp.toString());
                tmp.setLength(0);
            }
            sb.append(t);
        }


        /**
         * _more_
         *
         * @param request _more_
         *
         * @throws Exception _more_
         */
        public void finish(Request request) throws Exception {
            Appendable sb = getBuffer();
            sb.append(template.suffix);
            super.finish(request);
        }




    }








    /**
     * Class description
     *
     *
     * @version        $version$, Tue, Nov 2, '21
     * @author         Enter your name here...    
     */
    public static class IcalIterator extends ValueIterator {

        /** _more_          */
        SimpleDateFormat sdf =
            RepositoryUtil.makeDateFormat("yyyyMMdd'T'HHmmss");

        /**
         * _more_
         *
         * @param request _more_
         * @param db _more_
         * @param entry _more_
         *
         * @throws Exception _more_
         */
        public IcalIterator(Request request, DbTypeHandler db, Entry entry)
                throws Exception {
            super(request, db, entry);
        }

        /**
         * _more_
         *
         * @param request _more_
         * @param doGroupBy _more_
         *
         * @throws Exception _more_
         */
        public void initialize(Request request, boolean doGroupBy)
                throws Exception {
            super.initialize(request, doGroupBy);
            makeResult(".ical", "text/calendar");
            columns     = db.getColumnsToUse(request, false);
            columnNames = Column.getNames(columns);
            Appendable sb = getBuffer();
            sb.append("BEGIN:VCALENDAR\n");
            sb.append("PRODID:-//Unidata/UCAR//RAMADDA Calendar//EN\n");
            sb.append("VERSION:2.0\n");
            sb.append("CALSCALE:GREGORIAN\n");
            sb.append("METHOD:PUBLISH\n");

        }


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
            Appendable sb   = getBuffer();

            String     dbid = (String) values[IDX_DBID];
            Date date1 = (Date) values[dbInfo.getDateColumn().getOffset()];
            Date date2 = (Date) values[(dbInfo.getDateColumns().size() > 1)
                                       ? dbInfo.getDateColumns().get(1).getOffset()
                                       : dbInfo.getDateColumns().get(0).getOffset()];
            String dateString1 = sdf.format(date1) + "Z";
            String dateString2 = sdf.format(date2) + "Z";
            String url         = db.getViewUrl(request, entry, dbid);
            url = request.getAbsoluteUrl(url);
            String label = db.getLabel(request, entry, values, null).trim();

            if (label.length() == 0) {
                label = "NA";
            }

            sb.append("BEGIN:VEVENT\n");
            sb.append("UID:" + values[IDX_DBID] + "\n");
            sb.append("CREATED:" + dateString1 + "\n");
            sb.append("DTSTAMP:" + dateString1 + "\n");
            sb.append("DTSTART:" + dateString1 + "\n");
            sb.append("DTEND:" + dateString2 + "\n");
            sb.append("SUMMARY:" + label + "\n");
            sb.append("ATTACH:" + url + "\n");
            sb.append("END:VEVENT\n");
        }


        /**
         * _more_
         *
         * @param request _more_
         *
         * @throws Exception _more_
         */
        public void finish(Request request) throws Exception {
            Appendable sb = getBuffer();
            sb.append("END:VCALENDAR\n");
            super.finish(request);
        }




    }




    /**
     * Class description
     *
     *
     * @version        $version$, Tue, Nov 2, '21
     * @author         Enter your name here...    
     */
    public static class KmlIterator extends ValueIterator {

        /** _more_          */
        Element root;

        /** _more_          */
        Element folder;

        /** _more_          */
        Column theColumn;

        /** _more_          */
        boolean bbox = true;


        /**
         * _more_
         *
         * @param request _more_
         * @param db _more_
         * @param entry _more_
         *
         * @throws Exception _more_
         */
        public KmlIterator(Request request, DbTypeHandler db, Entry entry)
                throws Exception {
            super(request, db, entry);
        }

        /**
         * _more_
         *
         *
         * @param request _more_
         * @param entry _more_
         * @param values _more_
         * @param sdf _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        public String getKmlLabel(Request request, Entry entry,
                                  Object[] values, SimpleDateFormat sdf)
                throws Exception {
            return db.getLabel(request, entry, values, sdf);
        }


        /**
         * _more_
         *
         * @param request _more_
         * @param doGroupBy _more_
         *
         * @throws Exception _more_
         */
        public void initialize(Request request, boolean doGroupBy)
                throws Exception {
            super.initialize(request, doGroupBy);
            theColumn = null;
            for (Column column : db.tableHandler.getColumns()) {
                if (column.getType().equals(Column.DATATYPE_LATLONBBOX)) {
                    theColumn = column;

                    break;
                }
                if (column.getType().equals(Column.DATATYPE_LATLON)) {
                    theColumn = column;
                    bbox      = false;

                    break;
                }
            }

            if ((theColumn == null) && (dbInfo.getLatColumn() == null)
                    && (dbInfo.getLonColumn() == null)) {
                throw new IllegalStateException("No geo data found");
            }

            root   = KmlUtil.kml(entry.getName());
            folder = KmlUtil.folder(root, entry.getName(), false);
            KmlUtil.open(folder, true);
            if (entry.getDescription().length() > 0) {
                KmlUtil.description(folder, entry.getDescription());
            }
        }


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
            Appendable sb   = getBuffer();
            String     dbid = (String) values[IDX_DBID];
            double     lat  = 0;
            double     lon  = 0;

            if (theColumn == null) {
                lat = dbInfo.getLatColumn().uncheckedGetDouble(request,values);
                lon = dbInfo.getLonColumn().uncheckedGetDouble(request,values);
            } else {
                if ( !bbox) {
                    double[] ll = theColumn.getLatLon(request,values);
                    lat = ll[0];
                    lon = ll[1];
                } else {
                    double[] ll = theColumn.getLatLonBbox(request,values);
                    //Lower right
                    lat = ll[2];
                    lon = ll[3];
                }
            }
            String label = getKmlLabel(request, entry, values, null);
            String viewUrl = request.getAbsoluteUrl(db.getViewUrl(request,
                                 entry, dbid));
            String        href = HU.href(viewUrl, label);
            StringBuilder desc = new StringBuilder(href + "<br>");
            db.getHtml(request, desc, entry, values, false,false);
            Element placemark = KmlUtil.placemark(folder, label,
                                    desc.toString(), lat, lon, 0, null);
            if (dbInfo.getDateColumn() != null) {
                Date date = (Date) dbInfo.getDateColumn().uncheckedGetObject(request,values);
                if (date != null) {
                    KmlUtil.timestamp(placemark, date);
                }
            }

        }


        /**
         * _more_
         *
         * @param request _more_
         *
         * @throws Exception _more_
         */
        public void finish(Request request) throws Exception {
            Appendable sb = getBuffer();
            sb.append(XmlUtil.toString(root));
            super.finish(request);
        }




    }



    /**
     * Class description
     *
     *
     * @version        $version$, Tue, Nov 2, '21
     * @author         Enter your name here...    
     */
    public static class LabelsIterator extends ValueIterator {
        BiConsumer<Appendable,String> printer;

	int[]      A            = { 0, 0 };
        int        skip;
        boolean[]  putBreak     = { false };
        boolean[]  putPageBreak = { false };

        /**
         * _more_
         *
         * @param request _more_
         * @param db _more_
         * @param entry _more_
         *
         * @throws Exception _more_
         */
        public LabelsIterator(Request request, DbTypeHandler db, Entry entry)
                throws Exception {
            super(request, db, entry);
	    skip         = request.get("addresslabelskip", 0);


	    printer = (buffer,label) ->{
	    try {
            if(putBreak[0]) {
                buffer.append("<br>");
                putBreak[0] = false;
            }
            if(putPageBreak[0]) {
                buffer.append("<div class=db_address_pagebreak></div>");
                putPageBreak[0]=false;
            }
            buffer.append(label);
            A[0]++;
            if(A[0]==3) {
                A[0]=0;
                putBreak[0] = true;
                A[1]++;
                if(A[1]==10) {
                    putBreak[0] = false;
                    putPageBreak[0] = true;
                    A[1]=0;
                }
            }
	    } catch(Exception exc) {
		throw new RuntimeException(exc);
	    }
        };



        }

        /**
         * _more_
         *
         * @param request _more_
         * @param doGroupBy _more_
         *
         * @throws Exception _more_
         */
        public void initialize(Request request, boolean doGroupBy)
                throws Exception {
            super.initialize(request, doGroupBy);
            makeResult(".html", "text/html");
            columns     = db.getColumnsToUse(request, false);
            columnNames = Column.getNames(columns);
            for (int i = 0; i < columns.size(); i++) {
                Column c = columns.get(i);
            }
            Appendable sb = getBuffer();
	    HU.cssLink(sb,
			      db.getPageHandler().makeHtdocsUrl("/db/dbstyle.css"));
	    sb.append(
		      HU.importCss(
				   " body {height:initial; paddingL:0px; margin:0px; width: 8.5in; margin-top:0.0in;   margin-left:0.0in;margin-right:0.0in;}"));

	    for (int i = 0; i < skip; i++) {
		printer.accept(sb,"<div class=db_address_label style='border:0px'>"
			       + "</div>\n");
	    }

        }


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
            Appendable sb = getBuffer();
            String label = db.applyTemplate(request, entry, values, dateSdf,
					    db.addressTemplate);
            //Check for long lines
            List<String> lines  = Utils.split(label, "<br>");
            int          length = 0;
            for (String line : lines) {
                length = Math.max(length, line.length());
            }
            String extra = "";
            if (length > 25) {
                extra = "style='font-size:80%;' ";
            }
            printer.accept(sb,"<div class=db_address_label " + extra + ">"
                           + label + "</div>\n");

        }


        /**
         * _more_
         *
         * @param request _more_
         *
         * @throws Exception _more_
         */
        public void finish(Request request) throws Exception {
            Appendable sb = getBuffer();

            super.finish(request);
        }




    }



    /**
     * Class description
     *
     *
     * @version        $version$, Tue, Nov 2, '21
     * @author         Enter your name here...    
     */
    public static class GridIterator extends HtmlIterator {

        Column           gridColumn = null;
	List<HtmlUtils.Selector> enumValues;

        /**
         * _more_
         *
         * @param request _more_
         * @param db _more_
         * @param entry _more_
         *
         * @throws Exception _more_
         */
        public GridIterator(Request request, DbTypeHandler db, Entry entry)
                throws Exception {
            super(request, db, entry,true);
        }

        /**
         * _more_
         *
         * @param request _more_
         * @param doGroupBy _more_
         *
         * @throws Exception _more_
         */
        public void initialize(Request request, boolean doGroupBy)
                throws Exception {
            super.initialize(request, doGroupBy);
	    String           view       = db.getWhatToShow(request);
	    for (Column column : dbInfo.getCategoryColumns()) {
		if (Misc.equals(view, VIEW_GRID + column.getName())) {
		    gridColumn = column;
		    break;
		}
	    }
	    if (gridColumn == null) {
		throw new IllegalStateException("No grid columns defined");
	    }

            Appendable sb = getBuffer();
	    String links = db.getHref(request, entry,
				   VIEW_GRID + gridColumn.getName(),
				   "Grid View");
	    addViewHeader(request, entry, VIEW_GRID + gridColumn.getName(),links,false);

	    enumValues = db.getEnumValues(request, entry, gridColumn);
	    sb.append(
		      HU.cssBlock(
					 ".gridtable td {padding:5px;padding-bottom:0px;padding-top:8px;}\n.gridon {background: #88C957;}\n.gridoff {background: #eee;}"));
	    sb.append(
		      "<table cellspacing=0 cellpadding=0 border=1 width=100% class=\"gridtable\">\n");
	    sb.append("<tr>");
	    int width = 100 / (enumValues.size() + 1);
	    db.makeTableHeader(sb, "&nbsp;",
			       HU.attr(HU.ATTR_WIDTH, width + "%"));
	    String key = db.getTableHandler().getTableName() + "." + gridColumn.getName();
	    for (HtmlUtils.Selector tfo : enumValues) {
		String value = tfo.getId().toString();
		String searchUrl =
		    HU.url(
				  request.makeUrl(getRepository().URL_ENTRY_SHOW),
				  new String[] {
				      ARG_ENTRYID, entry.getId(), ARG_DB_SEARCH, "true", key, value
				  });

		db.makeTableHeader(sb, "&nbsp;" + HU.href(searchUrl, value),
				   HU.attr(HU.ATTR_WIDTH,
						  width + "%"));
	    }
        }


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
            Appendable sb = getBuffer();
            sb.append("<tr>\n");
            String url = canEdit
                         ? db.getEditUrl(request, entry,
                                      (String) values[IDX_DBID])
                         : db.getViewUrl(request, entry,
                                      (String) values[IDX_DBID]);

            String rowId = "row_" + values[IDX_DBID];
            String event = db.getEventJS(request, entry, values[IDX_DBID],
					 rowId, rowId);
            String href = HU.href(url,
                                         db.getLabel(request, entry,
						  values, dateSdf));
            sb.append(HU.col("&nbsp;" + href,
                                    HU.id(rowId) + event
                                    + HU.cssClass("dbcategoryrow")));
            String rowValue = (String) values[gridColumn.getOffset()];
            for (HtmlUtils.Selector tfo : enumValues) {
                String value = tfo.getId().toString();
                if (Misc.equals(value, rowValue)) {
                    sb.append(HU.col("&nbsp;",
                                            HU.cssClass("dbgridon")));
                } else {
                    sb.append(HU.col("&nbsp;",
                                            HU.cssClass("dbgridoff")));
                }
            }
        }


        /**
         * _more_
         *
         * @param request _more_
         *
         * @throws Exception _more_
         */
        public void finish(Request request) throws Exception {
            Appendable sb = getBuffer();
	    HU.close(sb, "table");
            super.finish(request);
        }
    }



    /**
     * Class description
     *
     *
     * @version        $version$, Tue, Nov 2, '21
     * @author         Enter your name here...    
     */
    public static class CategoryIterator extends HtmlIterator {

        Column           gridColumn = null;
	Hashtable<String, StringBuilder> map = new Hashtable<String,
	    StringBuilder>();
	List<String> rowValues = new ArrayList<String>();

        /**
         * _more_
         *
         * @param request _more_
         * @param db _more_
         * @param entry _more_
         *
         * @throws Exception _more_
         */
        public CategoryIterator(Request request, DbTypeHandler db, Entry entry)
                throws Exception {
            super(request, db, entry, true);
        }

        /**
         * _more_
         *
         * @param request _more_
         * @param doGroupBy _more_
         *
         * @throws Exception _more_
         */
        public void initialize(Request request, boolean doGroupBy)
                throws Exception {
            super.initialize(request, doGroupBy);
	    String           view       = db.getWhatToShow(request);
	    for (Column column : dbInfo.getCategoryColumns()) {
		if (Misc.equals(view, VIEW_CATEGORY + column.getName())) {
		    gridColumn = column;
		    break;
		}
	    }
	    if (gridColumn == null) {
		throw new IllegalStateException("No category columns defined");
	    }

	    String links = db.getHref(request, entry,
				   VIEW_CATEGORY + gridColumn.getName(),
				   "Category View");
	    addViewHeader(request, entry,   VIEW_CATEGORY + gridColumn.getName(),links,false);
        }


        /**
         * _more_
         *
         * @param request _more_
         * @param values _more_
         *
         * @throws Exception _more_
         */
        public void processRow(Request request, Object[] valuesArray)
                throws Exception {
            Appendable sb = getBuffer();
            String url = canEdit
                         ? db.getEditUrl(request, entry,
                                      (String) valuesArray[IDX_DBID])
                         : db.getViewUrl(request, entry,
                                      (String) valuesArray[IDX_DBID]);
            String label    = db.getLabel(request, entry, valuesArray, dateSdf);
            String href     = HU.href(url, label);

            String rowValue = (String) valuesArray[gridColumn.getOffset()];
            if (rowValue == null) {
                rowValue = "&lt;blank&gt;";
            }
            StringBuilder buffer = map.get(rowValue);
            if (buffer == null) {
                map.put(rowValue, buffer = new StringBuilder());
                rowValues.add(rowValue);
            }
            String rowId = "row_" + valuesArray[IDX_DBID];
            String event = db.getEventJS(request, entry, valuesArray[IDX_DBID],
                                      rowId, rowId);
            buffer.append(HU.div(href,
                                        HU.cssClass("dbcategoryrow")
                                        + HU.id(rowId) + event));
        }


        /**
         * _more_
         *
         * @param request _more_
         *
         * @throws Exception _more_
         */
        public void finish(Request request) throws Exception {
            Appendable sb = getBuffer();
	    List<String> titles = new ArrayList<String>();
	    List<String> tabs   = new ArrayList<String>();
	    
	    for (String rowValue : rowValues) {
		titles.add(rowValue);
		tabs.add(HU.insetDiv(map.get(rowValue).toString(), 0, 20,
					    10, 0));
	    }
	    HU.makeAccordion(sb, titles, tabs, false);
            super.finish(request);
        }
    }
    


}
