/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.db;


import org.ramadda.data.point.text.*;

import org.ramadda.repository.*;
import org.ramadda.repository.map.*;
import org.ramadda.repository.metadata.*;

import org.ramadda.repository.output.CalendarOutputHandler;
import org.ramadda.repository.output.MapOutputHandler;
import org.ramadda.repository.output.OutputHandler;
import org.ramadda.repository.output.OutputType;
import org.ramadda.repository.output.RssOutputHandler;
import org.ramadda.repository.output.WikiConstants;
import org.ramadda.repository.type.*;

import org.ramadda.repository.util.FileWriter;
import org.ramadda.util.Bounds;
import org.ramadda.util.FormInfo;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JQuery;
import org.ramadda.util.Json;

import org.ramadda.util.KmlUtil;
import org.ramadda.util.NamedBuffer;

import org.ramadda.util.NamedInputStream;
import org.ramadda.util.RssUtil;
import org.ramadda.util.Utils;
import org.ramadda.util.WikiUtil;
import org.ramadda.util.XlsUtil;
import org.ramadda.util.XmlUtils;
import org.ramadda.util.sql.*;
import org.ramadda.util.text.CsvUtil;
import org.ramadda.util.text.DataProvider;
import org.ramadda.util.text.Filter;
import org.ramadda.util.text.Processor;
import org.ramadda.util.text.Row;
import org.ramadda.util.text.TextReader;


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
    SimpleDateFormat sdf;


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
        sdf          = db.getDateFormat(entry);
        forPrint     = request.get(ARG_FOR_PRINT, false);
	canEdit = db.getAccessManager().canEditEntry(request, entry);
	embedded = request.get(ARG_EMBEDDED, false);

    }

    private String viewHeaderId;
    public void addViewHeader(Request request, Entry entry, String view,  String extraLinks) throws Exception {
	Appendable sb       = getBuffer();
	System.err.println("addViewHeader:" + embedded);
	if (embedded) {
	    db.addStyleSheet(sb);
	} else {
	    int max = db.getMax(request);
	    db.addViewHeader(request, entry, sb,view,extraLinks);
	    db.addSearchAgain(request, entry,sb);
	    viewHeaderId = Utils.getGuid();
	    HU.div(sb, "",  HtmlUtils.id(viewHeaderId));
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
	    HU.div(sb, tmp.toString(),  HtmlUtils.id(tmpId));
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
                groupByColumns.addAll(aggColumns);
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
            if (doGroupBy) {
                for (int i = 0; i < values.length; i++) {
                    if (i > 0) {
                        sb.append(",");
                    }
                    String s = values[i].toString();
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
            makeResult(".json", Json.MIMETYPE);
            columnNames = new ArrayList<String>();
            columns     = db.getColumnsToUse(request, false);
            for (int i = 0; i < columns.size(); i++) {
                Column c = columns.get(i);
                columnNames.add(c.getJson(request));
            }
            Appendable sb = getBuffer();
            sb.append(Json.MAP_OPEN);
            if (request.get("includeColumns", true)) {
                sb.append(Json.quote("columns"));
                sb.append(":");
                sb.append(Json.list(columnNames));
                sb.append(",\n");
            }
            sb.append(Json.quote("values"));
            sb.append(":");
            sb.append(Json.LIST_OPEN);
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
                attrs.add(Json.quote(colValue));
            }
            sb.append(Json.map(attrs));
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
            sb.append(Json.LIST_CLOSE);
            sb.append(Json.MAP_CLOSE);
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

            String info = db.getHtml(request, entry, dbid, db.getColumns(),
                                     values, sdf);
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
                double[] ll = db.getLocation(values);
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
            makeResult(".json", "");
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
            makeResult(".html", "text/html");
	    if(!embedded) {
		db.getEntryManager().addEntryHeader(request, entry, result);
		Appendable sb = getBuffer();
		String header = db.getPageHandler().decorateResult(request,
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
                    sb.append(HtmlUtils.br());
                    sb.append(
                        db.getPageHandler().showDialogNote(
                            db.msgLabel("No entries in")
                            + db.getTitle(request, entry)));
                } else {
                    sb.append(
                        db.getPageHandler().showDialogNote(
                            db.msg("Nothing found")));
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
            editImg = HtmlUtils.img(
                getRepository().getUrlBase() + "/db/database_edit.png",
                db.msg("View entry"));
            viewImg =
                HtmlUtils.img(getRepository().getUrlBase()
                              + "/db/database_go.png", db.msg("View entry"));
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
            extraCols = Utils.split(request.getString(ARG_EXTRA_COLUMNS, ""),
                                    "\n", true, true);
            columns     = db.getColumnsToUse(request, false);
            columnNames = Column.getNames(columns);
	    addViewHeader(request, entry, VIEW_TABLE,null);
        }

        /**
         * _more_
         *
         * @param request _more_
         *
         * @throws Exception _more_
         */
        private void initializeTable(Request request) throws Exception {

            Appendable sb       = getBuffer();
            if (doForm) {
                String formUrl =
                    request.makeUrl(db.getRepository().URL_ENTRY_SHOW);
                HtmlUtils.form(sb, formUrl);
                HtmlUtils.hidden(sb, ARG_ENTRYID, entry.getId(), "");
            }

            int entriesPerPage = request.get(ARG_ENTRIES_PER_PAGE, 8);
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
                actions.add(new TwoFacedObject("Delete entire database",
                        ACTION_DELETEALL));
            }
            if ( !embedded && (actions.size() > 0)) {
                if (doForm) {
                    sb.append(HtmlUtils.submit(db.msgLabel("Do"), ARG_DB_DO));
                    sb.append(HtmlUtils.select(ARG_DB_ACTION, actions));
                }
            }
            HtmlUtils.open(tableHeader, "table", "entryid", entry.getId(),
                           "id", tableId, "class", "dbtable", "border", "1",
                           "cellspacing", "0", "cellpadding", "0", "width",
                           "100%");
            HtmlUtils.open(tableHeader, "tr", "valign", "top");
            if ( !forPrint) {
                db.makeTableHeader(tableHeader, "&nbsp;");
            }
            for (int i = 0; i < columns.size(); i++) {
                Column column = columns.get(i);
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
                                + HtmlUtils.img(
                                    db.getRepository().getIconUrl(
                                        ICON_UPDART));
                    } else {
                        extra = " "
                                + HtmlUtils.img(
                                    db.getRepository().getIconUrl(
                                        ICON_DOWNDART));
                    }
                    asc = !asc;
                } else {
                    extra = " "
                            + HtmlUtils.img(
                                db.getRepository().getIconUrl(ICON_BLANK),
                                "", HtmlUtils.attr("width", "10"));
                }
                String link = HtmlUtils.href(baseUrl + "&" + ARG_DB_SORTBY1
                                             + "=" + sortColumn + "&"
                                             + ARG_DB_SORTDIR1 + (asc
                        ? "=asc"
                        : "=desc"), label) + extra;
                db.makeTableHeader(tableHeader, link);
            }
            for (String col : extraCols) {
                db.makeTableHeader(tableHeader, col);
            }
            HtmlUtils.close(tableHeader, "tr");



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
            sb.append(HtmlUtils.div("",
                                    HtmlUtils.id(popupId)
                                    + HtmlUtils.cssClass("ramadda-popup")));
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
		//                double mem1 = Utils.getUsedMemory();
                initializeTable(request);
		//                double mem2 = Utils.getUsedMemory();
		//                System.err.println("initializeTable Memory:"
		//                                   + Utils.decimals(mem2 - mem1, 1));
            }

            lineCnt++;
            if (forPrint && (lineCnt >= entriesPerPage)) {
                lineCnt = 0;
                sb.append("</table>");
                sb.append("<div class=pagebreak></div>");
                sb.append(tableHeader);
            }

            String dbid  = (String) values[IDX_DBID];
            String cbxId = ARG_DBID + (rowCnt);
            String divId = "div_" + dbid;
            sb.append("\n");
            HU.open(sb, "tr", "dbrowid", dbid);
            if ( !forPrint) {
                HtmlUtils.open(sb, "td", "width", "10", "style",
                               "white-space:nowrap;");
                HtmlUtils.open(sb, "div", "id", divId);
                if (doForm) {
                    String call =
                        HtmlUtils.attr(
                            HtmlUtils.ATTR_ONCLICK,
                            HtmlUtils.call(
                                "checkboxClicked",
                                HtmlUtils.comma(
                                    "event",
                                    HtmlUtils.squote(ARG_DBID_SELECTED),
                                    HtmlUtils.squote(cbxId))));

                    call = "";
                    HtmlUtils.checkbox(sb, ARG_DBID_SELECTED, dbid, false,
                                       "id=" + cbxId + " " + call);
                }
                if (canEdit) {
                    if (editUrl == null) {
                        editUrl = db.getEditUrl(request, entry, "_DBROWID_");
                    }
                    HtmlUtils.href(sb, editUrl.replace("_DBROWID_", dbid),
                                   editImg);
                }
                if (viewUrl == null) {
                    viewUrl = db.getViewUrl(request, entry, "_DBROWID_");
                }
                HtmlUtils.href(sb, viewUrl.replace("_DBROWID_", dbid),
                               viewImg);
                HtmlUtils.close(sb, "div", "td");
            }


            for (int i = 0; i < columns.size(); i++) {
                //              if(true) continue;
                Column column = columns.get(i);
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
                    HtmlUtils.open(sb, "td");
                } else if (column.isNumeric()) {
                    HtmlUtils.open(sb, "td", "align", "right");
                } else {
                    HtmlUtils.open(sb, "td");
                }

                HtmlUtils.open(sb, HtmlUtils.TAG_DIV, "class", "dbcell");

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
                                    HtmlUtils.img(
                                        db.getDbIconUrl(icon), "",
                                        HtmlUtils.attr("width", "16")));
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
                                prefix.append(HtmlUtils.span(content,
                                        HtmlUtils.style(style)));
                            }
                        }
                        if (prefix != null) {
                            sb.append(prefix.toString());
                        }
                    }
                }

                String label = db.formatTableValue(request, entry, sb,
                                   column, values, sdf, !forPrint);
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
                HtmlUtils.close(sb, HtmlUtils.TAG_DIV);
                HtmlUtils.close(sb, HtmlUtils.TAG_TD);

                if (column.isEnumeration() && column.getDoStats()) {
                    Hashtable<Object, Integer> numUniques =
                        uniques.get(column.getName());
                    if (numUniques == null) {
                        numUniques = new Hashtable<Object, Integer>();
                        uniques.put(column.getName(), numUniques);
                    }
                    Integer uniqueCnt = numUniques.get(label);
                    if (uniqueCnt == null) {
                        uniqueCnt = new Integer(0);
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
            Appendable    sb = getBuffer();
            StringBuilder hb = new StringBuilder();
            if ( !forPrint && (rowCnt > 0)) {
                HtmlUtils.comment(hb, "summmary");
                HtmlUtils.open(hb, "tr", "valign", "top");
                HtmlUtils.tag(hb, "td", HtmlUtils.attrs("align", "right"),
                              "#" + rowCnt);
                for (int i = 0; i < columns.size(); i++) {
                    Column column = columns.get(i);
                    if (column.isNumeric() && column.getDoStats()) {
                        double  avg   = sum[i] / rowCnt;
                        boolean round = column.isInteger();
                        HtmlUtils.open(hb, "td", "class", "dbtable-summary");
                        hb.append(HtmlUtils.formTable());
                        hb.append(HtmlUtils.formEntry("Average:",
                                db.format(avg, round)));
                        hb.append(HtmlUtils.formEntry("Minimum:",
                                db.format(min[i], round)));
                        hb.append(HtmlUtils.formEntry("Maximum:",
                                db.format(max[i], round)));
                        hb.append(HtmlUtils.formEntry("Total:",
                                db.format(sum[i], round)));
                        HtmlUtils.close(hb, "table", "td");
                    } else if (column.isEnumeration()
                               && column.getDoStats()) {
                        Hashtable<Object, Integer> numUniques =
                            uniques.get(column.getName());
                        if (numUniques == null) {
                            continue;
                        }
                        HtmlUtils.open(hb, "td", "class", "dbtable-summary");
                        hb.append(HtmlUtils.formTable());
                        int cnt = 0;
                        for (Enumeration keys = numUniques.keys();
                                keys.hasMoreElements(); ) {
                            if (cnt++ > 10) {
                                hb.append(HtmlUtils.formEntry("", "..."));

                                break;
                            }
                            Object key   = keys.nextElement();
                            Object value = numUniques.get(key);
                            if (key.toString().length() == 0) {
                                key = "&lt;blank&gt;";
                            }
                            hb.append(HtmlUtils.formEntry(key + ":",
                                    value.toString()));
                        }
                        HtmlUtils.close(hb, "table", "td");
                    } else {
                        HtmlUtils.tag(hb, "td", "", "&nbsp;");
                    }
                }
                for (String col : extraCols) {
                    HtmlUtils.open(hb, HtmlUtils.TAG_TD, "");
                    HtmlUtils.close(hb, HtmlUtils.TAG_TD);
                }
                HtmlUtils.close(hb, "tr");
                HtmlUtils.close(hb, "table");
		sb.append(HU.script("DB.initTable('" + tableId + "')"));
            } 
		hb.append(HtmlUtils.formClose());
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
                                   values, sdf, false);

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
                lat = dbInfo.getLatColumn().getDouble(values);
                lon = dbInfo.getLonColumn().getDouble(values);
            } else {
                if ( !bbox) {
                    double[] ll = theColumn.getLatLon(values);
                    lat = ll[0];
                    lon = ll[1];
                } else {
                    double[] ll = theColumn.getLatLonBbox(values);
                    //Lower right
                    lat = ll[2];
                    lon = ll[3];
                }
            }
            String label = getKmlLabel(request, entry, values, null);
            String viewUrl = request.getAbsoluteUrl(db.getViewUrl(request,
                                 entry, dbid));
            String        href = HtmlUtils.href(viewUrl, label);
            StringBuilder desc = new StringBuilder(href + "<br>");
            db.getHtml(request, desc, entry, values, false);
            Element placemark = KmlUtil.placemark(folder, label,
                                    desc.toString(), lat, lon, 0, null);
            if (dbInfo.getDateColumn() != null) {
                Date date = (Date) dbInfo.getDateColumn().getObject(values);
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
	    HtmlUtils.cssLink(sb,
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
            String label = db.applyTemplate(request, entry, values, sdf,
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
	List<TwoFacedObject> enumValues;

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
	    addViewHeader(request, entry, VIEW_GRID + gridColumn.getName(),links);

	    enumValues = db.getEnumValues(request, entry, gridColumn);
	    sb.append(
		      HtmlUtils.cssBlock(
					 ".gridtable td {padding:5px;padding-bottom:0px;padding-top:8px;}\n.gridon {background: #88C957;}\n.gridoff {background: #eee;}"));
	    sb.append(
		      "<table cellspacing=0 cellpadding=0 border=1 width=100% class=\"gridtable\">\n");
	    sb.append("<tr>");
	    int width = 100 / (enumValues.size() + 1);
	    db.makeTableHeader(sb, "&nbsp;",
			       HtmlUtils.attr(HtmlUtils.ATTR_WIDTH, width + "%"));
	    String key = db.getTableHandler().getTableName() + "." + gridColumn.getName();
	    for (TwoFacedObject tfo : enumValues) {
		String value = tfo.getId().toString();
		String searchUrl =
		    HtmlUtils.url(
				  request.makeUrl(getRepository().URL_ENTRY_SHOW),
				  new String[] {
				      ARG_ENTRYID, entry.getId(), ARG_DB_SEARCH, "true", key, value
				  });

		db.makeTableHeader(sb, "&nbsp;" + HtmlUtils.href(searchUrl, value),
				   HtmlUtils.attr(HtmlUtils.ATTR_WIDTH,
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
            String href = HtmlUtils.href(url,
                                         db.getLabel(request, entry,
						  values, sdf));
            sb.append(HtmlUtils.col("&nbsp;" + href,
                                    HtmlUtils.id(rowId) + event
                                    + HtmlUtils.cssClass("dbcategoryrow")));
            String rowValue = (String) values[gridColumn.getOffset()];
            for (TwoFacedObject tfo : enumValues) {
                String value = tfo.getId().toString();
                if (Misc.equals(value, rowValue)) {
                    sb.append(HtmlUtils.col("&nbsp;",
                                            HtmlUtils.cssClass("dbgridon")));
                } else {
                    sb.append(HtmlUtils.col("&nbsp;",
                                            HtmlUtils.cssClass("dbgridoff")));
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
	    HtmlUtils.close(sb, "table");
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
	    addViewHeader(request, entry,   VIEW_CATEGORY + gridColumn.getName(),links);
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
            String label    = db.getLabel(request, entry, valuesArray, sdf);
            String href     = HtmlUtils.href(url, label);

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
            buffer.append(HtmlUtils.div(href,
                                        HtmlUtils.cssClass("dbcategoryrow")
                                        + HtmlUtils.id(rowId) + event));
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
		tabs.add(HtmlUtils.insetDiv(map.get(rowValue).toString(), 0, 20,
					    10, 0));
	    }
	    HtmlUtils.makeAccordion(sb, titles, tabs, false);
            super.finish(request);
        }
    }
    


}
