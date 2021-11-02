/*
* Copyright (c) 2008-2019 Geode Systems LLC
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
import org.ramadda.util.NamedBuffer;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JQuery;
import org.ramadda.util.Json;

import org.ramadda.util.KmlUtil;
import org.ramadda.util.RssUtil;
import org.ramadda.util.Utils;
import org.ramadda.util.WikiUtil;
import org.ramadda.util.XlsUtil;
import org.ramadda.util.XmlUtils;
import org.ramadda.util.sql.*;

import org.ramadda.util.NamedInputStream;
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


public abstract class ValueIterator  implements DbConstants {
    public static final HtmlUtils HU = null;
    

    Request request;
    DbTypeHandler db;
    Result result;
    Entry entry;
    PrintWriter pw;
    boolean doGroupBy;
    List<String> columnNames=null;
    List<Column> columns = null;
    int cnt=0;
    DbInfo        dbInfo;
    SimpleDateFormat sdf;
    

    public ValueIterator(Request request, DbTypeHandler db,Entry entry) throws Exception {
	this.request = request;
	this.db = db;
	this.entry = entry;
	dbInfo = db.getDbInfo();
	sdf =  db.getDateFormat(entry);
    }

    public Repository getRepository()    {
	return db.getRepository();
    }
    

	
    protected Result makeResult(String suffix, String mimeType) throws Exception {
	if(result==null) {
	    if (request.defined(ARG_DB_SEARCHNAME)) {
		result = request.getOutputStreamResult(request.getString(ARG_DB_SEARCHNAME) + suffix,mimeType);
	    } else {
		result = request.getOutputStreamResult(entry.getName() + suffix,mimeType);
	    }
	}
	return result;
    }	

    public Result getResult() {
	return result;
    }

    protected PrintWriter getPrintWriter() throws Exception {
	if(pw==null) pw = new PrintWriter(request.getOutputStream());
	return pw;
    }


    public void initialize(Request request, boolean doGroupBy) throws Exception {
	this.doGroupBy = doGroupBy;
    }
    public abstract void processRow(Request request,Object[]values) throws Exception;

    public void finish(Request request) throws Exception {
	if(pw!=null) {
	    pw.flush();
	    //	    pw.close();
	}
    }	

    public static class CsvIterator extends ValueIterator {
	public CsvIterator(Request request,DbTypeHandler db, Entry entry) throws Exception {
	    super(request,db,entry);
	}
	public void initialize(Request request, boolean doGroupBy) throws Exception {
	    super.initialize(request, doGroupBy);
	    makeResult(".csv", "text/csv");
	    if(!doGroupBy) {
		columns = db.getColumnsToUse(request, false);
		columnNames        = Column.getNames(columns);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < columnNames.size(); i++) {
		    if(i>0) sb.append(",");
		    sb.append(columnNames.get(i));
		}
		sb.append("\n");
		getPrintWriter().print(sb.toString());
	    } else  {
		//TODO: use these later for formatting
		List<Column>  groupByColumns = db.getGroupByColumns(request, false);
		List<Column> aggColumns = db.getAggColumns(request);
		groupByColumns.addAll(aggColumns);
	    }
	}

	public void processRow(Request request,Object[]values) throws Exception {
	    PrintWriter pw = getPrintWriter();
	    Appendable sb = pw;
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

	    if(columns!=null) {
		for (int i = 0; i < columns.size(); i++) {
		    StringBuilder cb = new StringBuilder();
		    columns.get(i).formatValue(request, entry, cb, Column.OUTPUT_CSV,
					       values, true);
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

    public static class JsonIterator extends ValueIterator {
	public JsonIterator(Request request,DbTypeHandler db, Entry entry) throws Exception {
	    super(request,db,entry);
	}
	public void initialize(Request request, boolean doGroupBy) throws Exception {
	    super.initialize(request, doGroupBy);
	    makeResult(".json", Json.MIMETYPE);
	    columnNames    = new ArrayList<String>();
	    columns = db.getColumnsToUse(request, false);
	    for (int i = 0; i < columns.size(); i++) {
		Column c = columns.get(i);
		columnNames.add(c.getJson(request));
	    }
	    Appendable sb =  getPrintWriter();
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
	

	public void processRow(Request request,Object[]values) throws Exception {
	    Appendable sb =  getPrintWriter();
	    if(cnt++>0) sb.append(",\n");
	    StringBuilder cb = new StringBuilder();
            List<String> attrs  = new ArrayList<String>();
            for (int i = 0; i < columns.size(); i++) {
                cb.setLength(0);
                columns.get(i).formatValue(request, entry, cb, Column.OUTPUT_CSV,
                            values, true);
                String colValue = cb.toString();
                attrs.add(columns.get(i).getName());
                attrs.add(Json.quote(colValue));
            }
            sb.append(Json.map(attrs));
	}

	public void finish(Request request) throws Exception {
	    Appendable sb =  getPrintWriter();
	    sb.append(Json.LIST_CLOSE);
	    sb.append(Json.MAP_CLOSE);	    
	    super.finish(request);
	}
	



    }




    public static class RssIterator extends ValueIterator {
	/** _more_ */
	SimpleDateFormat rssSdf =
	    new SimpleDateFormat("EEE dd, MMM yyyy HH:mm:ss z");


	
	public RssIterator(Request request,DbTypeHandler db, Entry entry) throws Exception {
	    super(request,db,entry);
	}
	public void initialize(Request request, boolean doGroupBy) throws Exception {
	    super.initialize(request, doGroupBy);
	    makeResult(".rss", "application/rss+xml");
	    columns = db.getColumnsToUse(request, false);
	    columnNames        = Column.getNames(columns);
	    Appendable sb =  getPrintWriter();
	    sb.append(XmlUtil.XML_HEADER + "\n");
	    sb.append(XmlUtil.openTag(RssUtil.TAG_RSS,
				      XmlUtil.attrs(ATTR_RSS_VERSION, "2.0")));
	    sb.append(XmlUtil.openTag(RssUtil.TAG_CHANNEL));
	    sb.append(XmlUtil.tag(RssUtil.TAG_TITLE, "", entry.getName()));
	}
	

	public void processRow(Request request,Object[]values) throws Exception {
	    Appendable sb =  getPrintWriter();
	    if(cnt++>0)  {
	    }
            String   label  = db.getLabel(request, entry, values, null);
            Date     date   = null;
            if (dbInfo.getDateColumns().size() > 0) {
                date = (Date) values[dbInfo.getDateColumn().getOffset()];
            } else {
                date = (Date) values[db.IDX_DBCREATEDATE];
            }
            String dbid = (String) values[db.IDX_DBID];

            String info = db.getHtml(request, entry, dbid, db.getColumns(), values,
                                  sdf);
            sb.append(XmlUtil.openTag(RssUtil.TAG_ITEM));
            sb.append(XmlUtil.tag(RssUtil.TAG_PUBDATE, "",
                                  rssSdf.format(date)));
            sb.append(XmlUtil.tag(RssUtil.TAG_TITLE, "", label));


            String url = db.getViewUrl(request, entry, "" + values[db.IDX_DBID]);
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
	


	

	public void finish(Request request) throws Exception {
	    Appendable sb =  getPrintWriter();
	    sb.append(XmlUtil.closeTag(RssUtil.TAG_CHANNEL));
	    sb.append(XmlUtil.closeTag(RssUtil.TAG_RSS));
	    super.finish(request);
	}
    }



    public static class XxxIterator extends ValueIterator {
	public XxxIterator(Request request,DbTypeHandler db, Entry entry) throws Exception {
	    super(request,db,entry);
	}
	public void initialize(Request request, boolean doGroupBy) throws Exception {
	    super.initialize(request, doGroupBy);
	    makeResult(".json", "");
	    columns = db.getColumnsToUse(request, false);
	    columnNames        = Column.getNames(columns);
	    for (int i = 0; i < columns.size(); i++) {
		Column c = columns.get(i);
	    }
	    Appendable sb =  getPrintWriter();
	}
	

	public void processRow(Request request,Object[]values) throws Exception {
	    Appendable sb =  getPrintWriter();
	    if(cnt++>0)  {
	    }
	    StringBuilder cb = new StringBuilder();
	}
	

	public void finish(Request request) throws Exception {
	    Appendable sb =  getPrintWriter();

	    super.finish(request);
	}
	



    }
    

    public static class TableIterator extends ValueIterator {
	String tableId = Utils.getGuid();
	boolean fromSearch;
	int lineCnt=0;
	String editUrl;
	String viewUrl;	
	String editImg;
	String viewImg;	
	boolean forPrint;
	boolean canEdit;
	boolean doForm =  true;
	boolean showHeaderLinks =  true;	    
	int     entriesPerPage;
	String searchColumn;
	String sourceName;
	String sourceColumn;
	String searchFrom;
	
	StringBuilder tableHeader = new StringBuilder();
	
	
        Hashtable        entryProps;
	double[] sum;
	double[] min;
	double[] max;
	List<String> extraCols;
        Hashtable<String, Hashtable<Object, Integer>> uniques =
            new Hashtable<String, Hashtable<Object, Integer>>();
	
	
	
	public TableIterator(Request request,DbTypeHandler db, Entry entry, boolean fromSearch) throws Exception {
	    super(request,db,entry);
	    this.fromSearch = fromSearch;
	    entryProps   = db.getProperties(entry);
	    editImg = HtmlUtils.img(
			  getRepository().getUrlBase() + "/db/database_edit.png",
			  db.msg("View entry"));
	    viewImg = HtmlUtils.img(
			  getRepository().getUrlBase() + "/db/database_go.png",
			  db.msg("View entry"));	    
	}

	public void initialize(Request request, boolean doGroupBy) throws Exception {
	    super.initialize(request, doGroupBy);
	    canEdit = db.getAccessManager().canEditEntry(request, entry);
	    forPrint = request.get(ARG_FOR_PRINT,false);
	    entriesPerPage = request.get(ARG_ENTRIES_PER_PAGE,8);
	    extraCols = Utils.split(request.getString(ARG_EXTRA_COLUMNS,""),"\n",true,true);
	    if(forPrint) {
		request.put(ARG_TEMPLATE,"empty");
	    }
	    makeResult(".html", "text/html");
	    db.getEntryManager().addEntryHeader(request, entry,  result);

	    columns = db.getColumnsToUse(request, false);
	    columnNames        = Column.getNames(columns);
	    Appendable sb =  getPrintWriter();
	    String header = db.getPageHandler().decorateResult(request, result,true,false);
	    if(header!=null) {
		sb.append(header);
	    } else {
		System.err.println("no header");
	    }
	}

	private void initializeTable(Request request) throws Exception 	{
	    Appendable sb =  getPrintWriter();
	    boolean embedded = request.get(ARG_EMBEDDED, false);
	    if ( !embedded) {
		db.addViewHeader(request, entry, sb, VIEW_TABLE, /*valueList.size()*/100,
				 fromSearch,
				 "" /*StringUtil.join("&nbsp;|&nbsp;", links)*/);
	    } else {
		db.addStyleSheet(sb);
	    }
	    if (doForm) {
		String formUrl = request.makeUrl(db.getRepository().URL_ENTRY_SHOW);
		HtmlUtils.form(sb, formUrl);
		HtmlUtils.hidden(sb, ARG_ENTRYID, entry.getId(),"");
	    }

	    int     entriesPerPage = request.get(ARG_ENTRIES_PER_PAGE,8);
	    if(forPrint) canEdit= false;
	    HashSet<String> except = new HashSet<String>();
	    for(int i=1;i<=db.numOrders;i++) {
		except.add(ARG_DB_SORTBY+i);
		except.add(ARG_DB_SORTDIR+i);
	    }
	    String baseUrl = request.getUrl(except, null);
	    boolean asc = request.getString(ARG_DB_SORTDIR1,
					    (dbInfo.getDfltSortAsc()
					     ? "asc"
					     : "desc")).equals("asc");

	    String sortBy = request.getString(ARG_DB_SORTBY1,
					      ((dbInfo.getDfltSortColumn()
						== null)
					       ? ""
					       : dbInfo.getDfltSortColumn()
                                               .getName()));

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
	    if ( !request.get(ARG_EMBEDDED, false) && (actions.size() > 0)) {
		if (doForm) {
		    sb.append(HtmlUtils.submit(db.msgLabel("Do"), ARG_DB_DO));
		    sb.append(HtmlUtils.select(ARG_DB_ACTION, actions));
		}
	    }
	    HtmlUtils.open(tableHeader, "table", "entryid",entry.getId(),"id",tableId,"class", "dbtable", "border", "1",
			   "cellspacing", "0", "cellpadding", "0", "width",
			   "100%");
	    HtmlUtils.open(tableHeader, "tr", "valign", "top");
	    if(!forPrint) {
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
					    db.getRepository().getIconUrl(ICON_UPDART));
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
					db.getRepository().getIconUrl(ICON_BLANK), "",
					HtmlUtils.attr("width", "10"));
		}
		String link = HtmlUtils.href(baseUrl + "&" + ARG_DB_SORTBY1
					     + "=" + sortColumn + "&"
					     + ARG_DB_SORTDIR1 + (asc
								  ? "=asc"
								  : "=desc"), label) + extra;
		db.makeTableHeader(tableHeader, link);
	    }
	    for(String col: extraCols) {
		db.makeTableHeader(tableHeader, col);
	    }
	    HtmlUtils.close(tableHeader, "tr");



	    searchFrom = request.getString(ARG_SEARCH_FROM,null);
	    searchColumn = null;
	    sourceName=null;
	    sourceColumn=null;
	    if(searchFrom!=null) {
		List<String> toks = Utils.split(searchFrom,";");
		sourceName = toks.get(0);
		sourceColumn = toks.get(1);
		searchColumn = toks.get(3);
	    }
	    if(searchColumn!=null) {
		String href = HU.href("#",getRepository().getIconImage("fas fa-external-link-alt")+ " " + "Select all for " + sourceName+":" + sourceColumn,
				      HU.attr("onclick", "return DB.doDbSelectAll(event,'" +searchFrom+"')")+ HU.attr("class","ramadda-clickable") +HU.attr("title","Select all for " + sourceName+":" + sourceColumn));
		String clear = HU.href("#","Clear all " + sourceName+":" + sourceColumn,
				       HU.attr("onclick", "return DB.doDbClearAll(event,'" +searchFrom+"')")+ HU.attr("class","ramadda-clickable"));
		sb.append(HU.space(2)+href + HU.space(2) + clear);
	    }



	    sb.append(tableHeader);

	    Hashtable<String, Hashtable<Object, Integer>> uniques =
		new Hashtable<String, Hashtable<Object, Integer>>();

	    String popupId = "dbrowpopup_" + entry.getId();
	    sb.append(HtmlUtils.div("",
				    HtmlUtils.id(popupId)
				    + HtmlUtils.cssClass("ramadda-popup")));
	    sum  = new double[columns.size()];
	    min  = new double[columns.size()];
	    max  = new double[columns.size()];
	}
	
	    

	public void processRow(Request request,Object[]values) throws Exception {
	    Appendable sb =  getPrintWriter();
	    if(cnt++==0)  {
		double mem1 = Utils.getUsedMemory();
		initializeTable(request);
		double mem2 = Utils.getUsedMemory();
		System.err.println("initializeTable Memory:" + Utils.decimals(mem2-mem1,1));
	    }

	    lineCnt++;
	    if(forPrint && lineCnt>=entriesPerPage) {
		lineCnt=0;
		sb.append("</table>");
		sb.append("<div class=pagebreak></div>");
		sb.append(tableHeader);
	    }

            String   dbid   = (String) values[db.IDX_DBID];
            String   cbxId  = ARG_DBID + (cnt);
            String   divId  = "div_" + dbid;
            sb.append("\n");
	    HU.open(sb, "tr","dbrowid",dbid);
	    if(!forPrint) {
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
								      "event", HtmlUtils.squote(ARG_DBID_SELECTED),
								      HtmlUtils.squote(cbxId))));

		    call="";
		    HtmlUtils.checkbox(sb,ARG_DBID_SELECTED, dbid, false,
				       "id=" + cbxId+" " + call);
		}
		if (canEdit) {
		    if(editUrl==null) {
			editUrl = db.getEditUrl(request, entry, "_DBROWID_");
		    }
		    HtmlUtils.href(sb,  editUrl.replace("_DBROWID_",dbid),  editImg);
		}
		if(viewUrl==null) {
		    viewUrl = db.getViewUrl(request, entry, "_DBROWID_");
		}
		HtmlUtils.href(sb, viewUrl.replace("_DBROWID_",dbid),viewImg);
		HtmlUtils.close(sb, "div", "td");
	    }


            for (int i = 0; i < columns.size(); i++) {
		//		if(true) continue;
                Column column = columns.get(i);
                if (column.isNumeric()) {
                    Object o = values[column.getOffset()];
                    if (o != null) {
                        double v = ((o instanceof Integer)
                                    ? (double) ((Integer) o).intValue()
                                    : ((Double) o).doubleValue());
                        if (v == v) {
                            sum[i] += v;
                            min[i] = (cnt == 0)
                                     ? v
                                     : Math.min(min[i], v);
                            max[i] = (cnt == 0)
                                     ? v
                                     : Math.max(max[i], v);
                        }
                    }
                }

                if (column.isString()) {
                    HtmlUtils.open(sb, "td");
                } else if (column.isNumeric()) {
                     HtmlUtils.open(sb, "td", "align","right");
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



		String label = db.formatTableValue(request, entry, sb, column,
						   values, sdf,!forPrint);
                sb.append("&nbsp;");
		boolean addSelect = searchColumn!=null && column.getName().equals(searchColumn);
		if(addSelect) {
		    String value = ""+ values[column.getOffset()];
		    sb.append("&nbsp;");
		    sb.append(HU.href("#",getRepository().getIconImage("fas fa-external-link-alt"),HU.attr("onclick","return DB.doDbSelect(event,'" +searchFrom +"','" + label+"')") +  HU.attr("select-value",value) +HU.attr("class","db-select-link ramadda-clickable") +HU.attr("title","Select for " + sourceName+":" + sourceColumn)));
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
	

	public void finish(Request request) throws Exception {
	    Appendable sb =  getPrintWriter();
	    boolean forPrint = request.get(ARG_FOR_PRINT,false);
	    StringBuilder    hb           = new StringBuilder();
	    if (!forPrint && cnt > 0) {
		HtmlUtils.comment(hb, "summmary");
		HtmlUtils.open(hb, "tr", "valign", "top");
		HtmlUtils.tag(hb, "td", HtmlUtils.attrs("align", "right"),
			      "#" + cnt);
		for (int i = 0; i < columns.size(); i++) {
		    Column column = columns.get(i);
		    if (column.isNumeric() && column.getDoStats()) {
			double  avg   = sum[i] / cnt;
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
		    } else if (column.isEnumeration() && column.getDoStats()) {
			Hashtable<Object, Integer> numUniques =
			    uniques.get(column.getName());
			if (numUniques == null) {
			    continue;
			}
			HtmlUtils.open(hb, "td", "class", "dbtable-summary");
			hb.append(HtmlUtils.formTable());
			int rowCnt = 0;
			for (Enumeration keys = numUniques.keys();
			     keys.hasMoreElements(); ) {
			    if (rowCnt++ > 10) {
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
		for(String col: extraCols) {
		    HtmlUtils.open(hb, HtmlUtils.TAG_TD, "");
		    HtmlUtils.close(hb, HtmlUtils.TAG_TD);
		}
		HtmlUtils.close(hb, "tr");
		HtmlUtils.close(hb, "table");
	    } else {
		if ( !fromSearch) {
		    hb.append(HtmlUtils.br());
		    hb.append(
			      db.getPageHandler().showDialogNote(
							      db.msgLabel("No entries in")
							      + db.getTitle(request, entry)));
		} else {
		    hb.append(
			      db.getPageHandler().showDialogNote(db.msg("Nothing found")));
		}
	    }
	    hb.append(HtmlUtils.formClose());
	    sb.append(hb.toString());

	    sb.append(HU.script("DB.initTable('" + tableId+"')"));

	    boolean embedded = request.get(ARG_EMBEDDED, false);
	    if ( !embedded) {
		db.addViewFooter(request, entry, sb);
	    }
	    String footer = db.getPageHandler().decorateResult(request, result,false,true);
	    if(footer!=null) {
		sb.append(footer);
	    } else {
	    }
	    
	    
	    super.finish(request);
	}
	



    }
    
    







}






