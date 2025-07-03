/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.db;

import org.ramadda.data.point.text.*;

import org.ramadda.data.record.*;
import org.ramadda.data.services.PointOutputHandler;
import org.ramadda.data.services.PointTypeHandler;
import org.ramadda.data.services.RecordTypeHandler;

import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.map.*;
import org.ramadda.repository.metadata.*;

import org.ramadda.repository.output.CalendarOutputHandler;
import org.ramadda.repository.output.MapOutputHandler;
import org.ramadda.repository.output.OutputHandler;
import org.ramadda.repository.output.OutputType;
import org.ramadda.repository.output.WikiConstants;
import org.ramadda.repository.type.*;

import org.ramadda.repository.util.FileWriter;
import org.ramadda.util.geo.Bounds;
import org.ramadda.util.geo.GeoUtils;
import org.ramadda.util.FormInfo;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JQuery;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.NamedBuffer;
import org.ramadda.util.NamedInputStream;
import org.ramadda.util.IO;
import org.ramadda.util.Utils;
import org.ramadda.util.WikiUtil;
import org.ramadda.util.XlsUtil;
import org.ramadda.util.XmlUtils;
import org.ramadda.util.sql.Clause;

import org.ramadda.util.sql.SqlUtil;

import org.ramadda.util.seesv.Seesv;
import org.ramadda.util.seesv.DataProvider;
import org.ramadda.util.seesv.Filter;
import org.ramadda.util.seesv.Processor;
import org.ramadda.util.seesv.Row;
import org.ramadda.util.seesv.TextReader;

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
import java.util.TimeZone;
import java.util.zip.*;

@SuppressWarnings("unchecked")
public class DbTypeHandler extends PointTypeHandler implements DbConstants /* BlobTypeHandler*/ {
    private static boolean debugQuery  = false;
    public static final boolean debugTimes = false;
    public static final String ARG_SAMPLE = "sample";
    public static final String ATTR_SHOWSUMMARY = "search_showsummary";    
    private static final String ORDER_DESC = "desc";
    private static final String ORDER_ASC = "asc";    
    private Element tableNode;
    private DbInfo dbInfo;
    protected GenericTypeHandler tableHandler;
    protected Entry myEntry;
    private List<String> icons;
    private boolean showSummary = true;
    private String tableIcon = "";
    protected List<TwoFacedObject> viewList = new ArrayList<TwoFacedObject>();
    private List<List<String>> dfltOrder;
    private List<TwoFacedObject> orderTfos;
    protected int numOrders = 3;
    private String formJS;
    private String defaultView;
    private boolean showEntryCreate = true;
    private boolean showChartView = true;
    private boolean showFeedView = true;
    private boolean showDateView = true;
    private DecimalFormat ifmt = new DecimalFormat("#,##0");
    private DecimalFormat dfmt = new DecimalFormat("#,##0.#");
    private DecimalFormat pfmt = new DecimalFormat("0.00");
    XmlEncoder xmlEncoder = new XmlEncoder();
    private String[] namesArray;
    private String labelColumnNames;
    private String labelTemplate;
    protected String addressTemplate;
    private String mapLabelTemplate;
    private String mapLabelTemplatePrint;
    private int mapDotLimit=100;
    private boolean mapMarkersShow = true;
    private boolean mapPolygonsShow = true;    
    private  String defaultMapProperties;
    private String searchForLabel = "Search For";
    private String htmlHeader = null;
    private List<DbTemplate> templates = new ArrayList<DbTemplate>();
    private List<MacroStatement> macros = new ArrayList<MacroStatement>();

    public DbTypeHandler(Repository repository, String tableName,
                         Element tableNode, String desc)
            throws Exception {
        super(repository, tableName, desc);
        this.tableNode = tableNode;
        String dfltOrderProp = XmlUtil.getAttribute(tableNode,
                                   "defaultOrder", (String) null);

	showSummary = XmlUtil.getAttribute(tableNode,ATTR_SHOWSUMMARY,true);
        if (dfltOrderProp != null) {
            dfltOrder = new ArrayList<List<String>>();
            for (String tok : Utils.split(dfltOrderProp, ";")) {
                List<String> toks = Utils.split(tok, ",");
                dfltOrder.add(toks);
            }
        }
        String wikiTemplate  = XmlUtil.getGrandChildText(tableNode, "wiki",(String) null);
	if(wikiTemplate!=null) setWikiTemplate(wikiTemplate);

        htmlHeader  = XmlUtil.getGrandChildText(tableNode, "header",(String) null);
        searchForLabel = XmlUtil.getAttribute(tableNode, "searchForLabel",
                XmlUtil.getGrandChildText(tableNode, "searchForLabel",
                                          searchForLabel));
        defaultView = XmlUtil.getAttribute(tableNode, "defaultView",
                                           VIEW_TABLE);
        showEntryCreate = XmlUtil.getAttribute(tableNode, "showEntryCreate",
                true);

        showFeedView = XmlUtil.getAttribute(tableNode, "showFeedView", true);
        showDateView = XmlUtil.getAttribute(tableNode, "showDateView", true);

        numOrders = XmlUtil.getAttribute(tableNode, "numberOrderBy",
                                         numOrders);

        formJS = XmlUtil.getGrandChildText(tableNode, "formjs",
                                           (String) null);
        this.tableIcon = XmlUtil.getAttribute(tableNode, "icon",
                "/db/database.png");

        this.labelColumnNames = XmlUtil.getAttribute(tableNode,
                "labelColumns", "");
        this.addressTemplate = Utils.getAttributeOrTag(tableNode,
                "addressTemplate", (String) null);
        this.labelTemplate = Utils.getAttributeOrTag(tableNode,
                "labelTemplate", (String) null);
        this.mapLabelTemplate = Utils.getAttributeOrTag(tableNode,
                "mapLabelTemplate", null);
        this.mapLabelTemplatePrint = Utils.getAttributeOrTag(tableNode,
                "mapLabelTemplatePrint", null);

	mapDotLimit = Utils.getAttributeOrTag(tableNode,"mapDotLimit",mapDotLimit);
	defaultMapProperties = Utils.getAttributeOrTag(tableNode,"mapProperties","").replace("\\n","\n");

	mapMarkersShow = Utils.getAttributeOrTag(tableNode,"mapMarkersShow",mapMarkersShow);
	mapPolygonsShow = Utils.getAttributeOrTag(tableNode,"mapPolygonsShow",mapPolygonsShow);		
        //Initialize this type handler with a string blob
        Element root = XmlUtil.getRoot("<type></type>");
        XmlUtil.create("action", root, new String[] {"name","dbsearchform","label","Search Form","icon","fas fa-search"});
        XmlUtil.create("action", root, new String[] {"name","dblist","label","DB List","icon","fas fa-list"});	

        root.setAttribute(ATTR_SUPER, "type_point");
        Element node = XmlUtil.create("column", root, new String[] {
            "name", "contents", Column.ATTR_TYPE, "clob", Column.ATTR_SIZE,
            "256000", Column.ATTR_SHOWINFORM, "false", Column.ATTR_SHOWINHTML,
            "false"
        });

        List<Element> nodes = new ArrayList<Element>();
        nodes.add(node);
	super.initTypeHandler(root);
	//Don't call this for now since I don't think it is needed and if we have a basetype defined
	//for this db then it breaks things
	//        super.initColumns(nodes);
        this.setDescription(desc);
        List props = XmlUtil.findChildren(tableNode, TAG_PROPERTY);
        for (int j = 0; j < props.size(); j++) {
            Element propNode = (Element) props.get(j);
            setTypeProperty(XmlUtil.getAttribute(propNode, "name"),
                            XmlUtil.getAttribute(propNode, "value"));
            //            System.err.println ("db:" +XmlUtil.getAttribute(propNode,"name") +":" + XmlUtil.getAttribute(propNode,"value"));
        }

        setCategory(XmlUtil.getAttributeFromTree(tableNode,
                TypeHandler.ATTR_CATEGORY, "Database"));
        setSuperCategory(XmlUtil.getAttributeFromTree(tableNode,
                "supercategory", "Basic"));

        myEntry = new Entry(this, true);

        tableHandler = new GenericTypeHandler(repository, "db_" + tableName,
                desc) {
            protected String getEnumValueKey(Column column, Entry entry) {
                if (entry != null) {
                    return entry.getId() + "_" + column.getName();
                }

                return column.getName();
            }

            public Clause getEnumValuesClause(Column column, Entry entry)
                    throws Exception {
                if (entry == null) {
                    return null;
                }

                return Clause.eq(COL_ID, entry.getId());
            }
        };

    }

    private List<TwoFacedObject> getOrderTfos() {
        if (orderTfos == null) {
            orderTfos = new ArrayList<TwoFacedObject>();
            orderTfos.add(new TwoFacedObject("Down", ORDER_DESC));
            orderTfos.add(new TwoFacedObject("Up", ORDER_ASC));
        }

        return orderTfos;
    }

    public void addTemplate(DbTemplate template) {
        templates.add(template);
        viewList.add(new TwoFacedObject(template.name, template.id));
    }

    public DbInfo getDbInfo() {
        if (dbInfo == null) {
            dbInfo = new DbInfo(this, IDX_MAX_INTERNAL);
        }

        return dbInfo;
    }

    public GenericTypeHandler getTableHandler() {
        return tableHandler;
    }

    public void initDbColumns(List<Element> columnNodes) throws Exception {

        putProperty("icon", tableIcon);
        putProperty("form.date.show", "false");
        putProperty("form.area.show", "false");
        putProperty("form.resource.show", "false");
        putProperty("form.datatype.show", "false");
        tableHandler.initColumns(columnNodes);
        List<String> columnNames =
            new ArrayList<String>(tableHandler.getColumnNames());
        namesArray = StringUtil.listToStringArray(columnNames);

        DbInfo dbInfo = getDbInfo();
        dbInfo.initColumns(tableHandler.getColumns(), labelColumnNames);

        viewList.add(new TwoFacedObject("Table", VIEW_TABLE));

        if (dbInfo.getHasLocation()) {
            putProperty("form.area.show", "true");
            viewList.add(new TwoFacedObject("Map", VIEW_MAP));
        }

        if (addressTemplate != null) {
            viewList.add(new TwoFacedObject("Address Labels",
                                            VIEW_ADDRESSLABELS));
        }

        viewList.add(new TwoFacedObject("CSV", VIEW_CSV));

        if (showDateView && dbInfo.getHasDate()) {
	    /*
            viewList.add(new TwoFacedObject("Calendar", VIEW_CALENDAR));
            //            viewList.add(new TwoFacedObject("Timeline", VIEW_TIMELINE));
            viewList.add(new TwoFacedObject("ICAL", VIEW_ICAL));
	    */
        }

        for (Column gridColumn : dbInfo.getCategoryColumns()) {
            viewList.add(new TwoFacedObject(gridColumn.getLabel() + " "
                                            + "Category", VIEW_CATEGORY
                                                + gridColumn.getName()));
            viewList.add(new TwoFacedObject(gridColumn.getLabel() + " "
                                            + "Grid", VIEW_GRID
                                                + gridColumn.getName()));
        }
        if (showFeedView) {
            viewList.add(new TwoFacedObject("RSS", VIEW_RSS));
            viewList.add(new TwoFacedObject("JSON", VIEW_JSON));
        }

        List stmts = XmlUtil.findChildren(tableNode, "macro");
        for (int j = 0; j < stmts.size(); j++) {
            Element macroNode = (Element) stmts.get(j);
            String  stmt      = XmlUtil.getChildText(macroNode);
            String desc = XmlUtil.getGrandChildText(macroNode, "description",
                              "");
            List<Column> columns = new ArrayList<Column>();
            String name = XmlUtil.getAttribute(macroNode, "name", "Select");
            String type = XmlUtil.getAttribute(macroNode, "type",
                              MacroStatement.TYPE_NOTIN);
            String incolumn = XmlUtil.getAttribute(macroNode, "column",
                                  (String) null);
            List<String> cols = Utils.split(XmlUtil.getAttribute(macroNode,
                                    "columns"), ",");
            for (int i = 0; i < cols.size(); i++) {
                String col   = cols.get(i);
                String label = "";
                if (col.indexOf(":") >= 0) {
                    List<String> toks = StringUtil.splitUpTo(col, ":", 2);
                    col = toks.get(0);
                    if (toks.size() > 1) {
                        label = toks.get(1);
                    }
                }

                Column column = dbInfo.getColumn(col);
                if (column == null) {
                    throw new IllegalStateException(
                        "Unknown column in macro:" + cols.get(i));
                }
                if (label.trim().length() == 0) {
                    label = column.getLabel();
                }
                column = column.cloneColumn();
                column.setSearchArg(column.getSearchArg() + "_macro" + j
                                    + "_" + i);
                column.setLabel(label);
                columns.add(column);
            }
            macros.add(new MacroStatement(name, type, desc, incolumn,
                                          columns, stmt));
        }
    }

    @Override
    public int getValuesIndex() {
        return RecordTypeHandler.IDX_PROPERTIES;
    }

    /*
    @Override
    public boolean haveDatabaseTable() {
        return true;
    }
    */

    /**
     * Called by lucene index to get non file contents
     *
     * @param entry _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void getTextContents(Entry entry, StringBuilder sb)
            throws Exception {
        List<String> colNames = new ArrayList<String>();
        for (Column column : tableHandler.getColumns()) {
            if (column.isString()) {
                colNames.add(column.getName());
            }
        }

        Statement stmt = getDatabaseManager().select(SqlUtil.comma(colNames),
                             Misc.newList(tableHandler.getTableName()),
                             Clause.eq(COL_ID, entry.getId()), "", -1);
        try {
            SqlUtil.Iterator iter = getDatabaseManager().getIterator(stmt);
            ResultSet        results;
            while ((results = iter.getNext()) != null) {
                for (String col : colNames) {
                    String s = results.getString(col);
                    sb.append(s);
                    sb.append(" ");
                }
                sb.append("\n");
                if (sb.length() > 1000 * 1000 * 3) {
                    break;
                }
            }
        } finally {
            getRepository().getDatabaseManager().closeAndReleaseConnection(
                stmt);
        }
    }

    @Override
    public void addToEntryNode(Request request, Entry entry,
                               final FileWriter fileWriter, Element node,boolean encode)
            throws Exception {
        super.addToEntryNode(request, entry, fileWriter, node,encode);
        if ( !getAccessManager().canDoFile(request, entry)) {
            return;
        }
        if ((fileWriter == null)
                || (fileWriter.getZipOutputStream() == null)) {
            return;
        }
        ZipOutputStream zos      = fileWriter.getZipOutputStream();
        ZipEntry        zipEntry = new ZipEntry(entry.getId() + ".dbvalues");
        zos.putNextEntry(zipEntry);
        PrintWriter pw = new PrintWriter(zos);
        readValues(request, Clause.eq(COL_ID, entry.getId()), "", -1, pw,
                   null);
        pw.flush();
        zos.closeEntry();
        Element dbvalues = XmlUtil.create(TAG_DBVALUES, node,
                                          entry.getId() + ".dbvalues");

    }

    @Override
    public void initializeEntryFromXml(Request request, Entry entry,
                                       Element node,
                                       Hashtable<String, File> files)
            throws Exception {
        super.initializeEntryFromXml(request, entry, node, files);
        String valuesFile = XmlUtil.getGrandChildText(node, TAG_DBVALUES,
                                (String) null);
        if (valuesFile == null) {
            return;
        }
        File file = files.get(valuesFile);
        if (file == null) {
            System.err.println("DbTypeHandler: could not find file:"
                               + valuesFile);

            return;
        }
        BufferedReader br = new BufferedReader(
                                new InputStreamReader(
                                    new FileInputStream(file)));
        String line;
        String sql = makeInsertOrUpdateSql(entry, null);
        PreparedStatement insertStmt =
            getRepository().getDatabaseManager().getPreparedStatement(sql);
        try {
            while ((line = br.readLine()) != null) {
                Object[] tuple = (Object[]) xmlEncoder.toObject(
                                     new String(Utils.decodeBase64(line)));
                tuple[IDX_DBID] = getRepository().getGUID();
                //              System.err.println("READ:" + tuple[3]);
                //              if(true) continue;
                tableHandler.setStatement(entry, tuple, insertStmt, true);
                insertStmt.execute();
            }
        } finally {
            getRepository().getDatabaseManager().closeAndReleaseConnection(insertStmt);
            dbChanged(entry);
        }
    }

    @Override
    public void initializeCopiedEntry(Entry newEntry, Entry oldEntry)
            throws Exception {
        //        System.err.println("init copied entry " + oldEntry + " " + newEntry);
        super.initializeCopiedEntry(newEntry, oldEntry);
        DbInfo       dbInfo   = getDbInfo();
        List<String> colNames = tableHandler.getColumnNames();
        Statement stmt = getDatabaseManager().select(SqlUtil.comma(colNames),
                             Misc.newList(tableHandler.getTableName()),
                             Clause.eq(COL_ID, oldEntry.getId()), "", -1);

        String sql = makeInsertOrUpdateSql(newEntry, null);
        PreparedStatement insertStmt =
            getRepository().getDatabaseManager().getPreparedStatement(sql);

        try {
            SqlUtil.Iterator iter = getDatabaseManager().getIterator(stmt);
            ResultSet        results;
            while ((results = iter.getNext()) != null) {
                Object[] values   = tableHandler.makeEntryValueArray();
                int      valueIdx = 2;
                for (Column column : dbInfo.getColumns()) {
                    valueIdx = column.readValues(myEntry, results, values,
                            valueIdx);
                }
                //Just set a new id and a new create date
                values[IDX_DBID]         = getRepository().getGUID();
                values[IDX_DBCREATEDATE] = new Date();
                tableHandler.setStatement(newEntry, values, insertStmt, true);
                insertStmt.execute();
            }
        } finally {
            getRepository().getDatabaseManager().closeAndReleaseConnection(stmt);
            getRepository().getDatabaseManager().closeAndReleaseConnection(insertStmt);
            dbChanged(newEntry);
        }
    }

    public String getTitle(Request request, Entry entry) {
        return entry.getName();
    }

    public List<Column> getDbColumns() {
        return getDbInfo().getColumns();
    }

    public List<Column> getDbColumns(boolean sorted) {
        return getDbInfo().getColumns(sorted);
    }

    public Column getDbColumn(String name) {
        return getDbInfo().getColumn(name);
    }

    protected GenericTypeHandler getTableTypeHandler() {
        return tableHandler;
    }

    public String getInlineHtml(Request request, Entry entry)
            throws Exception {
        if ( !getRepository().getAccessManager().canAccessFile(request,
                entry)) {
            return null;
        }

        request.remove(ARG_OUTPUT);
        List<Object[]> valueList = readValues(request, entry,
                                       Clause.eq(COL_ID, entry.getId()),
                                       null);
        StringBuilder sb = new StringBuilder();
        addStyleSheet(request, sb);
        makeTable(request, entry, valueList, false, sb, false, true);

        /*
          if (showInHeader(VIEW_RSS)) {
          sb.append(getHref(request, entry, VIEW_RSS, msg("RSS"),
          "/" + entry.getName() + ".rss"));
          }
          if (showInHeader(VIEW_CSV)) {
          sb.append(getHref(request, entry, VIEW_CSV, msg("CSV"),
          "/" + entry.getName() + ".csv"));
          }
          if (showInHeader(VIEW_JSON)) {
          sb.append(getHref(request, entry, VIEW_JSON, msg("JSON"),
          "/" + entry.getName() + ".json"));
          }
        */

        return sb.toString();
    }

    protected String getWhatToShow(Request request) {
        String what = request.getString(ARG_WHAT, null);
        if (what != null) {
            return what;
        }

        return request.getString(ARG_DB_VIEW,
                                 request.getString(ARG_VIEW, VIEW_TABLE));
    }

    public boolean isEmbedded(Request request) {
	if(!request.isEmbedded()) return false;
	if(request.get("showForm",false)) return false;
	return true;
    }

    @Override
    public Result getHtmlDisplay(Request request, Entry entry)
            throws Exception {

        if ( !getRepository().getAccessManager().canAccessFile(request,
                entry)) {
            StringBuilder sb = new StringBuilder();
            getPageHandler().entrySectionOpen(request, entry, sb, "Database");
            sb.append(HU.center(
                getPageHandler().showDialogWarning(
						   msg("You do not have permission to view the database"))));
	    sb.append(getUserManager().makeLoginForm(request,
						     HU.hiddenBase64(ARG_REDIRECT, request.getUrl())));
            getPageHandler().entrySectionClose(request, entry, sb);
            return new Result(getTitle(request, entry), sb);
        }

	if(!request.exists(ARG_DB_VIEW) && !request.exists(ARG_DB_CREATE) && !request.exists("result")) {
	    String template =  getWikiTemplate(request,  entry);
	    if(Utils.stringDefined(template)) {
		return null;
	    }
	}

	String desc = entry.getDescription();
	if (stringDefined(desc) && isWikiText(desc) && !desc.trim().equals("<wiki>")) {
	    if(!request.anyDefined(ARG_DB_CONFIRM,ARG_DB_APPLY,ARG_DB_CREATE,ARG_DB_DELETE,ARG_DB_COPY,ARG_DB_EDIT,ARG_DB_EDITFORM,ARG_MESSAGE,ARG_DB_ACTION,ARG_WHAT,ARG_DB_VIEW,ARG_DB_ENTRY)) {
		return null;
	    }
	}

        Hashtable props = getProperties(entry);
        boolean doAnonForm = Misc.getProperty(props, PROP_ANONFORM_ENABLED,
                                 false);
        if (doAnonForm && !getAccessManager().canDoEdit(request, entry)) {
            if (request.exists(ARG_DB_CREATE)) {
                return handleNewOrEdit(request, entry, null, doAnonForm);
            }

            return handleForm(request, entry, null, true, doAnonForm);
        }

        boolean      canEdit = getAccessManager().canDoEdit(request,
                                   entry);

        List<String> colNames = tableHandler.getColumnNames();
        String       view     = getWhatToShow(request);
        if (request.exists(ARG_DB_EDITFORM)) {
            if ( !canEdit) {
                throw new AccessException("You cannot edit this database",
                                          request);
            }

            return handleForm(request, entry,
                              request.getString(ARG_DBID, (String) null),
                              true, false);
        }

        if (request.exists(ARG_DB_NEWFORM) || view.equals(VIEW_NEW)) {
            if ( !canEdit) {
                throw new AccessException("You cannot edit this database",
                                          request);
            }

            return handleForm(request, entry, null, true, false);
        }

        if (request.exists(ARG_DB_EDITSQL) || view.equals(VIEW_EDIT)) {
            if ( !canEdit) {
                throw new AccessException("You cannot edit this database",
                                          request);
            }

            return handleEdit(request, entry);
        }

        if (request.exists(ARG_DB_CREATE)) {
            if ( !canEdit) {
                throw new AccessException("You cannot edit this database",
                                          request);
            }

            return handleNewOrEdit(request, entry, null, false);
        }

        if (request.exists(ARG_DB_EDIT) || request.exists(ARG_DB_COPY)) {
            if ( !canEdit) {
                throw new AccessException("You cannot edit this database",
                                          request);
            }

            return handleNewOrEdit(request, entry,
                                   request.getString(ARG_DBID,
                                       (String) null), false);
        }

        if (request.exists(ARG_DB_SEARCHFORM)) {
            return handleSearchForm(request, entry);
        }

        if (request.exists(ARG_DB_SEARCH)) {
	    //        if (request.exists(ARG_DB_SEARCH) || isEmbedded(request) ) {
            return handleSearch(request, entry);
        }

        if (request.exists(ARG_DB_ENTRY)) {
            return handleView(request, entry,
                              request.getString(ARG_DBID, (String) null));
        }

        String action = "";
        if (request.exists(ARG_DB_DO)) {
            action = request.getString(ARG_DB_ACTION, "");
        }

        if (request.exists(ARG_DB_ACTION_CONFIRM)) {
            if ( !canEdit) {
                throw new AccessException("You cannot edit this database",
                                          request);
            }
            return handleActionConfirm(request, entry);
        }

        if (request.exists(ARG_DB_DELETE)
	    || action.equals(ACTION_DELETE)
	    || action.equals(ACTION_SET_LATLON)
	    || action.equals(ACTION_DELETEALL)) {
            if ( !canEdit) {
                throw new AccessException("You cannot edit this database",
                                          request);
            }
            return handleActionAsk(request, entry, action);
        }

        if (request.defined(ARG_DB_VIEW)) {
            return handleQuery(request, entry, action,
                               Clause.eq(COL_ID, entry.getId()), false);
        }
        return handleSearchForm(request, entry);
    }

    public void addViewFooter(Request request, Entry entry, Appendable sb)
            throws Exception {
        boolean forPrint = request.get(ARG_FOR_PRINT, false);
        if (forPrint) {
            getPageHandler().entrySectionClose(request, entry, sb);
            return;
        }

        if ( !isEmbedded(request)) {
            getPageHandler().entrySectionClose(request, entry, sb);
        }
    }

    public void addViewHeader(Request request, Entry entry, Appendable sb,
                              String view, String extraLinks)
            throws Exception {

        boolean forPrint = request.get(ARG_FOR_PRINT, false);
        if (forPrint) {
            String name = request.getString(ARG_DB_SEARCHNAME, (String) null);
            getPageHandler().entrySectionOpen(request, entry,  sb,name);
            addStyleSheet(request,sb);
            request.put(ARG_TEMPLATE, "empty");
            return;
        }

        if ( !request.get(ARG_DB_SHOWHEADER, true)) {
            return;
        }

        Hashtable props = getProperties(entry);
        boolean doAnonForm = Misc.getProperty(props, PROP_ANONFORM_ENABLED,
                                 false);
        if (doAnonForm) {
            if ( !getAccessManager().canDoEdit(request, entry)) {
                addStyleSheet(request,sb);

                return;
            }
        }

        addStyleSheet(request,sb);
        boolean embedded = isEmbedded(request);
        if (embedded) {
            return;
        }

        if ( !isEmbedded(request)) {
            getPageHandler().entrySectionOpen(request, entry, sb, "");
        }

        if (Utils.stringDefined(htmlHeader)) {
	    sb.append(getWikiManager().wikifyEntry(request, entry,htmlHeader));
        }

        if (Utils.stringDefined(entry.getDescription())) {
	    //only include the description if it is just regular text and not the wiki text override
	    if (!isWikiText(entry.getDescription())) {
		sb.append(getWikiManager().wikifyEntry(request, entry,
						       entry.getDescription()));
	    }
        }

        List<String> headerToks = new ArrayList<String>();
        String baseUrl =
            HU.url(request.makeUrl(getRepository().URL_ENTRY_SHOW),
                          new String[] { ARG_ENTRYID,
                                         entry.getId() });
        boolean[] addNext = { false };

        addHeaderItems(request, entry, view, headerToks, baseUrl, addNext);

        if (headerToks.size() > 1) {
            HU.div(sb, StringUtil.join("&nbsp;|&nbsp;", headerToks),
                          HU.cssClass(CSS_DB_HEADER));
        }

        List<Metadata> savedSearchMetadata =
            getMetadataManager().getMetadata(request,entry, METADATA_SAVEDSEARCH);
        if (savedSearchMetadata.size() > 0) {
            String searchId = request.getString(ARG_DB_SEARCHID, "");
            headerToks = new ArrayList<String>();
            for (Metadata m : savedSearchMetadata) {
                StringBuilder link = new StringBuilder();
                if (m.getId().equals(searchId)) {
                    request.remove(ARG_DB_SEARCHNAME);
                    headerToks.add(HU.b(m.getAttr1()));
                } else {
                    String url = baseUrl + "&"
                                 + HU.args(ARG_DB_SEARCH, "true",
                                     ARG_DB_SEARCHID, m.getId());

                    headerToks.add(HU.href(url, m.getAttr1()));
                }
            }
            sb.append(HU.div(StringUtil.join("&nbsp;|&nbsp;",
                    headerToks), HU.cssClass(CSS_DB_HEADER)));
        }

        if (request.defined(ARG_MESSAGE)) {
            sb.append(
                getPageHandler().showDialogNote(
                    request.getString(ARG_MESSAGE, "")));
            request.remove(ARG_MESSAGE);
        }

        if (extraLinks != null) {
            HU.div(sb, extraLinks, HU.cssClass(CSS_DB_HEADER));
        }

        if (request.defined(ARG_DB_SEARCHNAME)) {
            HU.sectionHeader(sb,
                                    request.getString(ARG_DB_SEARCHNAME, ""));
        }
        if (request.defined(ARG_DB_SEARCHDESC)) {
            sb.append(getWikiManager().wikifyEntry(request, entry,
                    request.getString(ARG_DB_SEARCHDESC, "")));
        }

        String searchFrom = request.getString(ARG_SEARCH_FROM, null);
        if (searchFrom != null) {
            List<String> toks = Utils.split(searchFrom, ";");
            sb.append("<div style='text-align:center;font-weight:bold;'>"
                      + "Look up " + toks.get(3) + " for " + toks.get(0)
                      + "</div>");
        }

    }

    public String addViewHeader(Request request, Entry entry, Appendable sb,
                                String view, int numValues,
                                boolean fromSearch, String extraLinks)
            throws Exception {
        addViewHeader(request, entry, sb, view, extraLinks);
        String formId = null;
        if (fromSearch) {
            formId = addSearchAgain(request, entry, sb,true);
        }
        addPrevNext(request, entry, sb, numValues);

        return formId;
    }

    public String addSearchAgain(Request request, Entry entry, Appendable sb, boolean inToggle)
            throws Exception {
        boolean forPrint = request.get(ARG_FOR_PRINT, false);
	if(forPrint) return "";

	//For now don't add the next/prev links if it is a bot
	if(request.getIsRobot() || request.getIsGoogleBot()) return "";

        String formId     = HU.getUniqueId("form_");
        String searchForm = getSearchForm(request, entry, formId).toString();
	if(inToggle) {
	    sb.append(HU.makeShowHideBlock(msg("Search again"),
						  searchForm, false));
	} else {
	    sb.append(searchForm);
	}	    

        return formId;
    }

    public void addPrevNext(Request request, Entry entry, Appendable sb,
                            int numValues)
            throws Exception {
        boolean forPrint = request.get(ARG_FOR_PRINT, false);
	if(forPrint) return;

	//For now don't add the next/prev links if it is a bot
	if(request.getIsRobot() || request.getIsGoogleBot()) return;

        if (numValues > 0) {
            if (isGroupBy(request)) {
                numValues--;
            }
            int max = getMax(request);
            if ((numValues == max) || request.defined(ARG_SKIP)) {
                getRepository().getHtmlOutputHandler().showNext(request,
								numValues, max, sb);
            } else {
                sb.append(numValues + ((numValues == 1)
                                       ? " result"
                                       : " results"));
            }
        }
    }

    public Result handleSearchForm(Request request, Entry entry)
            throws Exception {
        StringBuilder sb = new StringBuilder();
        addViewHeader(request, entry, sb, VIEW_SEARCH, null);
        sb.append(getSearchForm(request, entry));
        addViewFooter(request, entry, sb);

        return new Result(getTitle(request, entry), sb);
    }

    public void addHeaderItems(Request request, Entry entry, String view,
                               List<String> headerToks, String baseUrl,
                               boolean[] addNext)
            throws Exception {

        DbInfo dbInfo = getDbInfo();

        if (showInHeader(VIEW_SEARCH, true)) {
            headerToks.add(HU.href(baseUrl + "&" + ARG_DB_VIEW + "="
                                          + VIEW_SEARCH, msg("Search"),
                                              view.equals(VIEW_SEARCH)
                    ? HU.style("font-weight:bold;")
                    : ""));
        }

        if (showInHeader(VIEW_TABLE, true)) {
            if (view.equals(VIEW_TABLE)) {
                addNext[0] = true;
            }
            headerToks.add(HU.href(baseUrl + "&" + ARG_DB_VIEW + "="
                                          + VIEW_TABLE, msg("List"),
                                              view.equals(VIEW_TABLE)
                    ? HU.style("font-weight:bold;")
                    : ""));
        }

        if (dbInfo.getHasDate()) {
            if (showInHeader(VIEW_CALENDAR)) {
                headerToks.add(HU.href(baseUrl + "&" + ARG_DB_VIEW
                        + "=" + VIEW_CALENDAR, msg("Calendar"),
                            view.equals(VIEW_CALENDAR)
                            ? HU.style("font-weight:bold;")
                            : ""));
            }
        }

        if (dbInfo.getHasLocation()) {
            if (showInHeader(VIEW_MAP, true)) {
                if (view.equals(VIEW_MAP)) {
                    addNext[0] = true;
                }
                headerToks.add(HU.href(baseUrl + "&" + ARG_DB_VIEW
                        + "=" + VIEW_MAP, msg("Map"), view.equals(VIEW_MAP)
                        ? HU.style("font-weight:bold;")
                        : ""));
            }
        }

        if (dbInfo.getCategoryColumns().size() > 0) {
            String theColumn = request.getString(
                                   ARG_DB_COLUMN,
                                   dbInfo.getCategoryColumns().get(
                                       0).getName());
            for (Column column : dbInfo.getCategoryColumns()) {
                String label = column.getLabel();
                if (showInHeader(VIEW_CATEGORY + column.getName())) {
                    headerToks.add(HU.href(baseUrl + "&" + ARG_DB_VIEW
                            + "=" + VIEW_CATEGORY + column.getName() + "&"
                            + ARG_DB_COLUMN + "=" + column.getName(), label,
                                view.equals(VIEW_CATEGORY + column.getName())
                                ? HU.style("font-weight:bold;")
                                : ""));
                }
            }
        }

        boolean canEdit = getAccessManager().canDoEdit(request, entry);
        boolean canDoNew = getAccessManager().canDoNew(request, entry);

        if (canDoNew && showInHeader(VIEW_NEW, true)) {
            headerToks.add(HU.href(baseUrl + "&" + ARG_DB_VIEW + "="
                                          + VIEW_NEW, msg("New"),
                                              view.equals(VIEW_NEW)
                    ? HU.style("font-weight:bold;")
                    : ""));
        }

        if (canEdit) {
            headerToks.add(HU.href(baseUrl + "&" + ARG_DB_VIEW + "="
                                          + VIEW_EDIT, msg("Edit"),
                                              view.equals(VIEW_EDIT)
                    ? HU.style("font-weight:bold;")
                    : ""));
        }

    }

    public boolean showInHeader(String view) {
        return showInHeader(view, false);
    }

    public boolean showInHeader(String view, boolean dflt) {
        String prop = getTypeProperty("header.show." + view, (String) null);
        //        System.err.println ("show in? header.show." + view +"=" + prop);
        if (prop != null) {
            return prop.equals("true");
        }

        return dflt;
    }

    private Result handleQuery(Request request, Entry entry, String action,
                               Clause clause, boolean fromSearch)
            throws Exception {

        DbInfo         dbInfo    = getDbInfo();
        List<Object[]> valueList = null;

        if (request.defined(ARG_DB_ITERATE)
                && request.defined(ARG_DB_ITERATE_VALUES)) {
            StringBuilder sb = new StringBuilder();
            addViewHeader(request, entry, sb, VIEW_TABLE, null);
            if ( !isEmbedded(request)) {
                sb.append(HU.makeShowHideBlock(msg("Search again"),
                        getSearchForm(request, entry).toString(), false));
            }
            Column iterateColumn = null;
            for (Column column : dbInfo.getColumnsToUse()) {
                if (column.getName().equals(request.getString(ARG_DB_ITERATE,
                        ""))) {
                    iterateColumn = column;

                    break;
                }
            }
            String selection = request.getString(ARG_DB_ITERATE_VALUES, "");
            selection = selection.replaceAll("_nl_", "\n");
            List<String> values = StringUtil.split(selection, "\n", true,
                                      true);
            if (values.size() == 0) {
                sb.append("Need to specify a set of values");
                getPageHandler().entrySectionClose(request, entry, sb);

                return new Result("", sb);
            }
            if (iterateColumn == null) {
                sb.append("Incorrect column");
                getPageHandler().entrySectionClose(request, entry, sb);

                return new Result("", sb);
            }

            request.put(ARG_DB_SHOWHEADER, "false");
            request.setEmbedded(true);
            for (String value : values) {
                String label = value;
                if (iterateColumn.isEnumeration()) {
                    //In case the user entered a enum label
                    value = iterateColumn.getEnumValue(value);
                    label = iterateColumn.getEnumLabel(value);
                }

                List<Clause> clauses = new ArrayList<Clause>();
                clauses.add(clause);
                String searchArg = iterateColumn.getSearchArg();
                request.put(searchArg, value);
                iterateColumn.assembleWhereClause(request, clauses,
                        new StringBuilder());
                valueList =
                    (List<Object[]>) getStorageManager().getCacheObject(
                        entry.getId(), request);
                if (valueList == null) {
                    valueList = readValues(request, entry,
                                           Clause.and(clauses), null);
                    getStorageManager().putCacheObject(entry.getId(),
                            request, valueList);
                }
                StringBuilder tmpSB = new StringBuilder();
                tmpSB.append(iterateColumn.getLabel() + ": " + label);
                if (valueList.size() == 0) {
                    tmpSB.append("<br>Nothing found");
                } else {
                    handleListTable(request, entry, valueList, false, false,
                                    tmpSB);
                }
                sb.append(
                    HU.div(
                        tmpSB.toString(),
                        HU.cssClass("db_iterate_block")));

            }
            getPageHandler().entrySectionClose(request, entry, sb);

            return new Result("", sb);
        }

        String  view      = getWhatToShow(request);
        boolean doGroupBy = isGroupBy(request);
        if (view.equals(VIEW_SEARCH)) {
            return handleSearchForm(request, entry);
        }

        if ((dbInfo.getDateColumns().size() > 0) && request.defined(ARG_YEAR)
                && request.defined(ARG_MONTH)) {
            int year  = request.get(ARG_YEAR, 0);
            int month = request.get(ARG_MONTH, 0) + 1;
            GregorianCalendar cal =
                new GregorianCalendar(DateUtil.TIMEZONE_GMT);
            SimpleDateFormat sdf   = new SimpleDateFormat("yyyy/MM/dd");
            Date             date1 = sdf.parse(year + "/" + month + "/1");
            cal.setTime(date1);
            cal.add(GregorianCalendar.MONTH, 1);
            Date date2 = cal.getTime();
            clause = Clause.and(
                clause,
                Clause.and(
                    Clause.ge(
                        dbInfo.getDateColumns().get(0).getName(),
                        date1), Clause.le(
                            dbInfo.getDateColumns().get(0).getName(),
                            date2)));

        }
        boolean doingGeo = view.equals(VIEW_KML) || view.equals(VIEW_MAP);
        boolean forPrint = request.get(ARG_FOR_PRINT, false);

        //Only add the geo clauses if we aren't also doing the print output
        if ( !forPrint && doingGeo) {
            List<Clause> geoClauses = new ArrayList<Clause>();
            for (Column column : getDbColumns()) {
                column.addGeoExclusion(geoClauses);
            }
            if (geoClauses.size() > 0) {
                clause = Clause.and(clause, Clause.and(geoClauses));
            }
        }
        if (view.equals(VIEW_KML) && !request.defined(ARG_MAX)) {
            request.put(ARG_MAX, "10000");
        }
        long t1 = System.currentTimeMillis();
        //Only cache the group by as these can be slow
        boolean doCache = isGroupBy(request);

        if (doCache) {
            valueList = (List<Object[]>) getStorageManager().getCacheObject(
                entry.getId(), request);
            //      System.err.println (valueList!=null?"In cache":"Not in cache");
            if ((valueList != null) && debugTimes) {
                Utils.printTimes("DbTypeHandler.getCacheObject: "
                                 + valueList.size(), t1,
                                     System.currentTimeMillis());
            }
        }

        ValueIterator iterator = makeValueIterator(request, entry, view,
						   action, fromSearch);

        if (valueList == null) {
            if (debugTimes && doCache) {
                System.err.println("Not in cache");
                SqlUtil.debug = true;
            }
            valueList     = readValues(request, entry, clause, iterator);
            SqlUtil.debug = false;
            long t2 = System.currentTimeMillis();
            if (debugTimes) {
                Utils.printTimes("DbTypeHandler.readValues: ", t1, t2);
            }
            if (doCache) {
                if (debugTimes) {
                    System.err.println("Writing to cache:"
                                       + valueList.size());
                }
                getStorageManager().putCacheObject(entry.getId(), request,
                        valueList);
            }
            //      System.err.println("results:" + valueList.size());
        } else {
            //If we have an iterator and the values were cached then run through them
            if (iterator != null) {
                iterator.initialize(request, doGroupBy);
                for (Object[] values : valueList) {
                    iterator.processRow(request, values);
                }
                iterator.finish(request);
            }
        }

        if (iterator != null) {
            return iterator.getResult();
        }

        return makeListResults(request, entry, view, action, fromSearch,
                               valueList);

    }

    protected boolean isGroupBy(Request request) {
        if (request == null) {
            return false;
        }

        return Utils.stringDefined(request.getString(ARG_GROUPBY, ""));
    }

    @Override
    public void  getInnerEntryContent(Entry entry, Request request,
				      TypeHandler typeHandler, OutputType output,
				      boolean showDescription, boolean showResource,
				      boolean linkToDownload,
				      Hashtable props,
				      HashSet<String> seen,
				      boolean forOutput,
				      List<NamedBuffer> contents)
            throws Exception {
	super.getInnerEntryContent(entry, request,
				   typeHandler, output,
				   showDescription, showResource,
				   linkToDownload, props,seen,forOutput,contents);

        DbInfo              dbInfo     = getDbInfo();
	String url =request.getAbsoluteUrl("/db/upload?entryid=" + entry.getId());
	for(Column c:dbInfo.getColumnsToUse()) {
	    String sample = "0.0";
	    if(c.isDate()) sample="yyyy-MM-dd";
	    else if(c.isString()) sample="value";	    
	    url+="&amp;" +c.getName()+"=" + sample;
	}
	url+="&amp;key=HIDDEN";
	contents.get(contents.size()-1).append(HU.formEntry("Upload URL:",url));
    }

    public Result processEntryAction(Request request, Entry entry)
            throws Exception {
        String action = request.getString("action", "");
	if(action.equals("dbsearchform")) {
	    return getEntryManager().addEntryHeader(request, entry,handleSearchForm(request, entry));
	}
	if(action.equals("dblist")) {
	    request.put(ARG_DB_VIEW,VIEW_TABLE);
	    return getHtmlDisplay(request, entry);
	}	
	return super.processEntryAction(request,entry);
    }

    public Result makeListResults(Request request, Entry entry, String view,
                                  String action, boolean fromSearch,
                                  List<Object[]> valueList)
            throws Exception {
        boolean doGroupBy = isGroupBy(request);

        if ( !doGroupBy) {
            if (action.equals(ACTION_EMAIL)) {
                return handleListEmail(request, entry, valueList);
            }

            if (view.equals(VIEW_SEARCH)) {
                return handleSearchForm(request, entry);
            }
            if (view.equals(VIEW_MAP)) {
                return handleListMap(request, entry, valueList, fromSearch);
            }
            if (view.equals(VIEW_CALENDAR)) {
                return handleListCalendar(request, entry, valueList,
                                          fromSearch);
            }
        }

        return handleListTable(request, entry, valueList, fromSearch, true);
    }

    public ValueIterator makeValueIterator(Request request, Entry entry,
                                           String view, String action,
                                           boolean fromSearch)
            throws Exception {
        boolean doGroupBy = isGroupBy(request);
        if (action.equals(ACTION_CSV) || view.equals(VIEW_CSV)) {
            return new ValueIterator.CsvIterator(request, this, entry);
        }
        if (action.equals(ACTION_JSON) || view.equals(VIEW_JSON)) {
            return new ValueIterator.JsonIterator(request, this, entry);
        }
        if (view.equals(VIEW_RSS)) {
            return new ValueIterator.RssIterator(request, this, entry);
        }

        if (view.equals(VIEW_KML)) {
            return new ValueIterator.KmlIterator(request, this, entry);
        }

        if (view.equals(VIEW_ICAL)) {
            return new ValueIterator.IcalIterator(request, this, entry);
        }

        for (DbTemplate template : templates) {
            if (view.equals(template.id)) {
                return new ValueIterator.TemplateIterator(request, this,
                        entry, template);
            }
        }

        if (view.equals(VIEW_ADDRESSLABELS)) {
            return new ValueIterator.LabelsIterator(request, this, entry);
        }

        if (view.startsWith(VIEW_GRID)) {
            return new ValueIterator.GridIterator(request, this, entry);
        }

        if (view.startsWith(VIEW_CATEGORY)) {
            return new ValueIterator.CategoryIterator(request, this, entry);
        }

        if ( !doGroupBy) {
            if (view.equals(VIEW_TABLE) || view.equals("")) {
                return new ValueIterator.TableIterator(request, this, entry,
                        fromSearch);
            }
        }

        /*
        if ( !doGroupBy) {
            if (action.equals(ACTION_EMAIL)) {
                return handleListEmail(request, entry, valueList);
            }
            if (view.equals(VIEW_SEARCH)) {
                return handleSearchForm(request, entry);
            }
            if (view.equals(VIEW_MAP)) {
                return handleListMap(request, entry, valueList, fromSearch);
            }
            if (view.equals(VIEW_TIMELINE)) {
                return handleListTimeline(request, entry, valueList,
                                          fromSearch);
            }

            if (view.equals(VIEW_CALENDAR)) {
                return handleListCalendar(request, entry, valueList,
                                          fromSearch);
            }

        }
        */
        return null;
    }

    @Override
    public void addToEntryForm(Request request, Appendable formBuffer,
                               Entry parentEntry, Entry entry,
                               FormInfo formInfo)
            throws Exception {
        DbInfo dbInfo = getDbInfo();
        if ((dbInfo.getUrlColumn() != null) && (entry != null)) {
            String baseUrl =
                request.getAbsoluteUrl(
                    HU.url(
                        request.makeUrl(getRepository().URL_ENTRY_SHOW),
                        new String[] { ARG_ENTRYID,
                                       entry.getId() }));
            String url = baseUrl + "&" + ARG_DB_VIEW + "=" + VIEW_NEW;
            String jsUrl = "javascript:document.location='" + url + "'+'&"
                           + dbInfo.getUrlColumn().getEditArg() + "='+"
                           + "document.location";

            if (dbInfo.getLabelColumns() != null) {
                jsUrl = jsUrl + "+'&"
                        + dbInfo.getLabelColumns().get(0).getEditArg()
                        + "='+document.title";
            }

            if (dbInfo.getDescColumn() != null) {
                String selected =
                    "(window.getSelection? window.getSelection():document.getSelection?document.getSelection():document.selection?document.selection:'')";

                jsUrl = jsUrl + "+'&" + dbInfo.getDescColumn().getEditArg()
                        + "='+" + selected;
            }

            String href = HU.href(jsUrl,
                                         " Add URL to " + entry.getName());
            formBuffer.append(
                HU.row(
                    HU.colspan(
                        "Bookmark this link to add new items to the database: "
                        + href, 2)));
        }

        super.addToEntryForm(request, formBuffer, parentEntry, entry,
                             formInfo);
        Hashtable props = getProperties(entry);

        if (entry != null) {
            addToEditForm(request, entry, formBuffer);
            addEnumerationAttributes(request, entry, formBuffer);
        }

    }

    public void addToEditForm(Request request, Entry entry,
                              Appendable formBuffer)
            throws Exception {
        Hashtable props = getProperties(entry);

        formBuffer.append(
            HU.formEntry(
                "",
                HU.checkbox(
                    PROP_ANONFORM_ENABLED, "true",
                    Misc.getProperty(
                        props, PROP_ANONFORM_ENABLED, false)) + " "
                            + msg("Allow anonymous form submission")));
        formBuffer.append(HU.formEntry(msgLabel("Message"),
                HU.input(PROP_ANONFORM_MESSAGE,
                    Misc.getProperty(props, PROP_ANONFORM_MESSAGE, ""),
                        HU.SIZE_80) + " "
                            + msg("What to show the user after they create an item")));
    }

    private void addEnumerationAttributes(Request request, Entry entry,
                                          Appendable formBuffer)
            throws Exception {

        if (getDbInfo().getEnumColumns().size() == 0) {
            return;
        }
        Hashtable props  = getProperties(entry);

        String[]  colors = {
            "#fff", "#000", "#444", "#888", "#eee", "red", "orange", "yellow",
            "green", "blue", "cyan", "purple",
        };

        //        if(icons == null) {
        icons = StringUtil.split(
            getRepository().getResource(
                "/org/ramadda/plugins/db/resources/icons.txt"), "\n", true,
                    true);

        HashSet baseIcons = Utils.makeHashSet(icons);

        //        }

        for (Column col : getDbInfo().getEnumColumns()) {
            String colorID = PROP_CAT_COLOR + "." + col.getName();
            String iconID = PROP_CAT_ICON + "." + col.getName();
            Hashtable<String, String> colorMap =
                (Hashtable<String, String>) props.get(colorID);
            if (colorMap == null) {
                colorMap = new Hashtable<String, String>();
            }
            Hashtable<String, String> iconMap =
                (Hashtable<String, String>) props.get(iconID);
            if (iconMap == null) {
                iconMap = new Hashtable<String, String>();
            }
            StringBuilder        sb   = new StringBuilder("");
            List<HtmlUtils.Selector> tfos = getEnumValues(request, entry, col);
            if ((tfos != null) && (tfos.size() < 150) && (tfos.size() > 0)) {
                formBuffer.append(
                    HU.row(
                        HU.colspan(
                            HU.div(
                                msg("Settings for") + " " + col.getLabel(),
                                HU.cssClass("formgroupheader")), 2)));

                for (HtmlUtils.Selector tfo : tfos) {
                    String value        = tfo.getId().toString();
                    String currentColor = colorMap.get(value);
                    String currentIcon  = iconMap.get(value);
                    if (currentColor == null) {
                        currentColor = "";
                    }
                    if (currentIcon == null) {
                        currentIcon = "";
                    }
                    String        colorArg = colorID + "." + value;
                    String        iconArg  = iconID + "." + value;
                    StringBuilder colorSB  = new StringBuilder();
                    colorSB.append(HU.radio(colorArg, "",
                            currentColor.equals("")));
                    colorSB.append(msg("None"));
                    colorSB.append(" ");
                    for (String c : colors) {
                        colorSB.append(
                            HU.span(
                                HU.radio(
                                    colorArg, c,
                                    currentColor.equals(c)), HU.style(
                                        "margin-left:2px; margin-right:2px; padding-left:5px; padding-right:7px; border:1px solid #000; background-color:"
                                        + c)));
                    }
                    StringBuilder iconSB = new StringBuilder();
                    iconSB.append(HU.radio(iconArg, "",
                            currentIcon.equals("")));
                    iconSB.append(msg("None"));
                    iconSB.append("  ");
                    iconSB.append(msg("Custom:"));
                    iconSB.append(" ");
                    iconSB.append(HU.input(iconArg + "_custom",
                            baseIcons.contains(currentIcon)
                            ? ""
                            : currentIcon, 20));
                    iconSB.append("<br>");
                    for (String icon : icons) {
                        if (icon.startsWith("#")) {
                            continue;
                        }
                        if (icon.equals("br")) {
                            iconSB.append("<br>");

                            continue;
                        }
                        iconSB.append(HU.radio(iconArg, icon,
                                currentIcon.equals(icon)));
                        iconSB.append(HU.img(getDbIconUrl(icon),
                                IOUtil.getFileTail(icon), "width=24"));
                        iconSB.append(" ");
                    }
                    formBuffer.append(HU.formEntry(msgLabel("Value"),
                            value));
                    formBuffer.append(
                        HU.formEntryTop(
                            msgLabel("Color"), colorSB.toString()));
                    String iconMsg = "";
                    if (currentIcon.length() > 0) {
                        iconMsg = HU.img(getDbIconUrl(currentIcon),
                                currentIcon, "width=16");
                    }
                    formBuffer.append(
                        HU.formEntryTop(
                            msgLabel("Icon"),
                            HU.makeShowHideBlock(
                                iconMsg, iconSB.toString(), false)));
                    formBuffer.append(HU.formEntry("", "<hr>"));
                }
                formBuffer.append(HU.formEntry("", sb.toString()));
            }
        }

    }

    protected String getDbIconUrl(String icon) {
        if (icon.startsWith("http:")) {
            return icon;
        }

        String base = getRepository().getUrlBase();
        if (icon.startsWith(base)) {
            return icon;
        }

        String path = icon;
        if ( !path.startsWith("/")) {
            path = "/" + path;

        }

        return getRepository().getUrlBase() + path;
    }

    public void initializeEntryFromForm(Request request, Entry entry,
                                        Entry parent, boolean newEntry)
            throws Exception {
        super.initializeEntryFromForm(request, entry, parent, newEntry);
        Hashtable props = getProperties(entry);

        props.put(PROP_ANONFORM_ENABLED,
                  request.get(PROP_ANONFORM_ENABLED, false) + "");
        props.put(PROP_ANONFORM_MESSAGE,
                  request.getString(PROP_ANONFORM_MESSAGE, ""));

        for (Column col : getDbInfo().getEnumColumns()) {
            String colorID = PROP_CAT_COLOR + "." + col.getName();
            Hashtable<String, String> colorMap =
                (Hashtable<String, String>) props.get(colorID);
            if (colorMap == null) {
                colorMap = new Hashtable<String, String>();
            }

            String iconID = PROP_CAT_ICON + "." + col.getName();
            Hashtable<String, String> iconMap =
                (Hashtable<String, String>) props.get(iconID);
            if (iconMap == null) {
                iconMap = new Hashtable<String, String>();
            }
            List<HtmlUtils.Selector> enumValues = getEnumValues(request, entry,
                                                  col);
            if (enumValues != null) {
                for (HtmlUtils.Selector tfo : enumValues) {
                    String value     = tfo.getId().toString();
                    String iconArg   = iconID + "." + value;
                    String iconValue = request.defined(iconArg + "_custom")
                                       ? request.getString(iconArg
                                           + "_custom", "")
                                       : request.getString(iconArg, "");
                    if (iconValue.equals("")) {
                        iconMap.remove(value);
                    } else {
                        iconMap.put(value, iconValue);
                    }
                }

                for (HtmlUtils.Selector tfo : enumValues) {
                    String value      = tfo.getId().toString();
                    String colorArg   = colorID + "." + value;
                    String colorValue = request.getString(colorArg, "");
                    if (colorValue.equals("")) {
                        colorMap.remove(value);
                    } else {
                        colorMap.put(value, colorValue);
                    }
                }
            }
            props.put(colorID, colorMap);
            props.put(iconID, iconMap);
        }
        setProperties(entry, props);
    }

    @Override
    public void addToProcessingForm(Request request, Entry entry,
                                    Appendable sb)
            throws Exception {
        super.addToProcessingForm(request, entry, sb);
        StringBuilder inner = new StringBuilder();
        inner.append(HU.formTable());
        getSearchFormInner(request, entry, inner, false,null);
        inner.append(HU.formTableClose());
        org.ramadda.data.services.PointFormHandler.formGroup(request, sb,
                "Database Search",
                HU.makeShowHideBlock("", inner.toString(), false));
    }

    public StringBuilder getSearchForm(Request request, Entry entry)
            throws Exception {
        String formId = HU.getUniqueId("form_");

        return getSearchForm(request, entry, formId);
    }

    public StringBuilder getSearchForm(Request request, Entry entry,
                                       String formId)
            throws Exception {

        StringBuilder sb = new StringBuilder();
        String formUrl   = request.makeUrl(getRepository().URL_ENTRY_SHOW);
        sb.append(HU.comment("search form open"));
        sb.append(HU.uploadForm(formUrl, HU.id(formId)));
        sb.append("\n");
        sb.append(HU.hidden(ARG_ENTRYID, entry.getId()));
        HU.open(sb, "div", HU.cssClass("ramadda-form-block"));
        String buttons = HU.submit("Search", ARG_DB_SEARCH);
        sb.append(buttons);
        getSearchFormInner(request, entry, sb, true,formId);
        if (formJS != null) {
            HU.div(sb, "", HU.id("formjs_div"));
            sb.append(HU.script(formJS));
        }
        sb.append(buttons);
        HU.close(sb, "div");
        StringBuilder js = new StringBuilder();
        js.append("$( document ).ready(function() {HtmlUtil.initSelect('.search-select');\n});");
        HU.script(sb, js.toString());
        sb.append(HU.comment("search form close"));
        sb.append(HU.formClose());

	if(request.get("showLinks",true)) {
	    StringBuilder formSB=new StringBuilder();
	    OutputHandler.addUrlShowingForm(formSB, entry, formId,
					    "[\".*OpenLayers_Control.*\"]",
					    request.isAnonymous()
					    ? null
					    : "DB.addUrlShowingForm");

	    String links = HU.makeShowHideBlock("Links",formSB.toString(), false);
	    sb.append(HU.insetDiv(links,0,40,0,0));

	}

        return sb;
    }

    private void getSearchFormInner(Request request, Entry entry,
                                    Appendable sb, boolean normalForm, String formId)
            throws Exception {

        if (normalForm) {
            String count = msgLabel("Count") + " "
                           + HU.input(ARG_MAX, getMax(request),
                                             HU.SIZE_5
                                             + HU.attr("default",
                                                 "" + DEFAULT_MAX));

            sb.append(HU.space(2));
            sb.append("View As: ");
            sb.append(
                HU.select(
                    ARG_DB_VIEW, viewList, request.getString(
                        ARG_DB_VIEW, defaultView),
		    /*HU.attr("default", VIEW_TABLE)+*/  HU.cssClass(
                        "search-select")) + HU.space(2) + count);
        }

        List<Clause>        where      = null;
        DbInfo              dbInfo     = getDbInfo();
        List<DbNamedBuffer> buffers    = new ArrayList<DbNamedBuffer>();
        String              formHeader = HU.formTable(true);
        DbNamedBuffer buffer = null;
	String currentGroup = null;
        for (Column column : getDbColumns(true)) {
            if ( !normalForm && column.isType(column.DATATYPE_LATLON)) {
                continue;
            }
            if ( !column.getCanSearch()) {
                continue;
            }
            String group = column.getDisplayGroup();
            if (group != null && !Utils.equals(currentGroup,group)) {
		currentGroup = group;
                buffers.add(buffer = new DbNamedBuffer(group, formHeader,
                        Utils.makeID(group)));
            }
	    if(buffer==null) {
		buffer = new DbNamedBuffer(searchForLabel, formHeader,
					   Utils.makeID(searchForLabel));
		buffers.add(buffer);
	    }
            String help = column.getHelp();
            if (Utils.stringDefined(help)) {
                buffer.getBuffer().append(formEntry(request, "", help));
            }
            column.addToSearchForm(request, buffer.getBuffer(), where, entry,true);

        }

        //      if(true) return;

        for (MacroStatement macro : macros) {
            buffer = new DbNamedBuffer(macro.name, formHeader,
                                       Utils.makeID(macro.name));
            buffers.add(buffer);
            if (Utils.stringDefined(macro.desc)) {
                buffer.append(HU.colspan(macro.desc, 2));
            }
            for (int i = 0; i < macro.columns.size(); i++) {
                Column column = macro.columns.get(i);
                column.addToSearchForm(request, buffer.getBuffer(), where,
                                       entry);
            }
        }

        //      if(true) return;

        List<TwoFacedObject> tfos     = new ArrayList<TwoFacedObject>();
        List<TwoFacedObject> aggtfos  = new ArrayList<TwoFacedObject>();
        List<TwoFacedObject> sorttfos = new ArrayList<TwoFacedObject>();
        tfos.add(new TwoFacedObject("----", ""));
        aggtfos.add(new TwoFacedObject("----", ""));
        sorttfos.add(new TwoFacedObject("----", ""));
        for (Column column : dbInfo.getColumnsToUse()) {
            if (column.getCanSearch() || column.getCanSort()) {
                sorttfos.add(new TwoFacedObject(column.getLabel(),
                        column.getName()));
            }

            if (column.getCanSearch()) {
                tfos.add(new TwoFacedObject(column.getLabel(),
                                            column.getName()));
                if (column.isDate()) {
                    tfos.add(new TwoFacedObject("Year of "
                            + column.getLabel(), "year(" + column.getName()
                                + ")"));
                }
                aggtfos.add(new TwoFacedObject(column.getLabel(),
                        column.getName()));
            }
        }

        if (normalForm) {
            if (tfos.size() > 0 && showSummary) {
                buffers.add(buffer = new DbNamedBuffer("Summary", formHeader,
                        "summary"));
                buffer.append(
                    formEntry(
                        request, msgLabel("Group By"),
                        HU.select(
                            ARG_GROUPBY, tfos,
                            (List<String>) request.get(
                                ARG_GROUPBY,
                                new ArrayList()), " multiple size=4 ")));

                List<TwoFacedObject> aggTypes =
                    new ArrayList<TwoFacedObject>();
                aggTypes.add(new TwoFacedObject("----", ""));
                aggTypes.add(new TwoFacedObject("Count", "count"));
                aggTypes.add(new TwoFacedObject("Sum", "sum"));
                aggTypes.add(new TwoFacedObject("Average", "avg"));
                aggTypes.add(new TwoFacedObject("Min", "min"));
                aggTypes.add(new TwoFacedObject("Max", "max"));

                StringBuilder aggSB = new StringBuilder();
                for (int i = 0; i < 3; i++) {
                    aggSB.append(
                        HU.select(
                            ARG_AGG
                            + i, aggtfos, request.getString(
                                ARG_AGG + i, ""), HU.cssClass(
                                "search-select")) + HU.space(2)
                                    + HU.select(
                                        ARG_AGG_TYPE
                                        + i, aggTypes, request.getString(
                                            ARG_AGG_TYPE
                                            + i, ""), HU.cssClass(
                                                "search-select")));
                    aggSB.append("<br>");
                }
                aggSB.append(HU.labeledCheckbox(ARG_AGG_PERCENT,
                        "true", request.get(ARG_AGG_PERCENT, false),
                        "Show percentage"));
                buffer.append(formEntry(request, msgLabel("Aggregate"),
                                        aggSB.toString()));

                String dfltDir = ORDER_DESC;
                String groupOrder = HU.select(
                                        ARG_DB_GROUP_SORTBY, sorttfos,
                                        request.getString(
                                            ARG_DB_GROUP_SORTBY,
                                            ""), HU.cssClass(
                                                "search-select"));
                groupOrder += HU.space(2);
                groupOrder += HU.select(
                    ARG_DB_GROUP_SORTDIR, getOrderTfos(),
                    request.getString(ARG_DB_GROUP_SORTDIR, dfltDir),
                    HU.cssClass("search-select"));
                buffer.append(formEntry(request, msgLabel("Order"),
                                        groupOrder));
                if (aggtfos.size() > 0) {
                    buffer.append(
                        formEntry(
                            request, msgLabel("Iterate"),
                            HU.select(
                                ARG_DB_ITERATE, aggtfos,
                                request.getString(ARG_DB_ITERATE, ""),
                                HU.cssClass(
                                    "search-select")) + HU.space(1)
                                        + "Enter values to iterate search on:"
                                        + HU.br()
                                        + HU.textArea(
                                            ARG_DB_ITERATE_VALUES,
                                            request.getString(
                                                ARG_DB_ITERATE_VALUES,
                                                    ""), 4, 50)));
                }

            }
        }

        buffers.add(buffer = new DbNamedBuffer("Order By/Display/Uniques",
                formHeader, "orderby"));
        if (sorttfos.size() > 0) {
            String orderBy = "";
            if (dbInfo.getDfltSortColumn() != null) {
                orderBy = dbInfo.getDfltSortColumn().getName();
            }
            String order = "";
            for (int i = 1; i <= numOrders; i++) {
                String dfltCol = orderBy;
                String dfltDir = ORDER_DESC;
                if ((dfltOrder != null) && (i - 1 < dfltOrder.size())) {
                    List<String> toks = dfltOrder.get(i - 1);
                    if (toks.size() > 0) {
                        dfltCol = toks.get(0);
                    }
                    if (toks.size() > 1) {
                        dfltDir = toks.get(1);
                    }
                }

                order += HU.select(
                    ARG_DB_SORTBY + i, sorttfos,
                    request.getString(ARG_DB_SORTBY + i, dfltCol),
                    HU.cssClass("search-select")) + HU.select(
                        ARG_DB_SORTDIR + i, getOrderTfos(),
                        request.getString(ARG_DB_SORTDIR + i, dfltDir),
			HU.attr("default-value",ORDER_DESC) +
                        HU.cssClass("search-select")) + HU.space(2);
		order+="\n";
            }
            buffer.append(formEntry(request, msgLabel("Order By"), order));
        }

        StringBuilder viewSB      = new StringBuilder();
        boolean       defaultShow = false;
        viewSB.append(HU.labeledCheckbox(ARG_DB_SHOW + "toggleall",
                "true", false, "Toggle All"));
        viewSB.append("<br>");
        for (Column column : dbInfo.getColumnsToUse()) {
            String arg = ARG_DB_SHOW + "_" + column.getName();
            viewSB.append(HU.labeledCheckbox(arg, "true",
                    request.get(arg, defaultShow), column.getLabel()));
            viewSB.append("<br>");
        }
        buffer.append(formEntry(request, msgLabel("Display"),
                                HU.makeShowHideBlock("",
                                    viewSB.toString(), false)));

	if(formId!=null)
	    buffer.append(HU.script("DB.toggleAllInit("+HU.squote(formId)+");"));
	else
	    buffer.append(HU.script("DB.toggleAllInit();"));	

        StringBuilder uniqueSB = new StringBuilder();
        for (Column column : dbInfo.getColumnsToUse()) {
            String arg = ARG_DB_UNIQUE + "_" + column.getName();
            uniqueSB.append(HU.labeledCheckbox(arg, "true",
                    request.get(arg, false), column.getLabel()));
            uniqueSB.append("<br>");
        }
        buffer.append(formEntry(request, msgLabel("Uniques"),
                                HU.makeShowHideBlock("",
                                    uniqueSB.toString(), false)));

        buffers.add(buffer = new DbNamedBuffer("Advanced Options",
                formHeader, "advanced"));
        if (normalForm) {
            buffer.append(HU.formEntry(msgLabel("Search Type"),
                    HU.labeledCheckbox(ARG_DB_OR, "true",
                        request.get(ARG_DB_OR, false),
                        "Match any of the above search criteria (OR logic)")));
            buffer.append(HU.formEntry(msgLabel("Sample"),
					      HU.input(ARG_SAMPLE,request.getString(ARG_SAMPLE,""),HU.SIZE_5) +"% 1-100 Only show the percentage of the results"));

            String suffix = getAccessManager().canDoEdit(request, entry)
                            ? HU.space(2)
                              + HU.labeledCheckbox(
                                  ARG_DB_DOSAVESEARCH, "true",
                                  request.get(ARG_DB_DOSAVESEARCH, false),
                                  "Save Search")
                            : "";

            buffer.append(
                HU.formEntryTop(
                    "", "Add extra columns in output, one per line:"));
            buffer.append(
                HU.formEntry(
                    msgLabel("Extra Columns"),
                    HU.textArea(
                        ARG_EXTRA_COLUMNS,
                        request.getString(ARG_EXTRA_COLUMNS, ""), 5, 40)));
	    buffer.append(formEntry(request, msgLabel("Search Name"),
		      HU.input(
			       ARG_DB_SEARCHNAME,
			       request.getString(ARG_DB_SEARCHNAME, ""),
			       HU.SIZE_50) + suffix));

	    buffer.append(formEntry(request, msgLabel("Subtitle"),
		      HU.input(
			       ARG_DB_SUBTITLE,
			       request.getString(ARG_DB_SUBTITLE, ""),
			       HU.SIZE_50) ));

            buffer.append(formEntry(request, msgLabel("Description"),
                                    HU.textArea(ARG_DB_SEARCHDESC,
                                        request.getString(ARG_DB_SEARCHDESC,
                                            ""), 5, 40)));
            String searchFrom = request.getString(ARG_SEARCH_FROM, null);
            if (searchFrom != null) {
                buffer.append(HU.hidden(ARG_SEARCH_FROM, searchFrom));
            }

            String print = HU.labeledCheckbox(
                               ARG_FOR_PRINT, "true",
                               request.get(ARG_FOR_PRINT, false),
                               "Printable") + HU.space(4)
                                            + "Entries per page: "
                                            + HU.input(
                                                ARG_ENTRIES_PER_PAGE,
                                                request.getString(
                                                    ARG_ENTRIES_PER_PAGE,
                                                    ""), HU.SIZE_5) +
		HU.space(4) +
		HU.labeledCheckbox(ARG_NUMBER_ENTRIES,"true",request.get(ARG_NUMBER_ENTRIES,false),
				   "Number results") +
		HU.space(4) +
		HU.labeledCheckbox(ARG_TILE_ENTRIES,"true",request.get(ARG_TILE_ENTRIES,false),
				   "Tile map results");		

            if (addressTemplate != null) {
                print += "<br>" + HU.b("Address label skip: ")
                         + HU
                         .input("addresslabelskip", request
                             .getString("addresslabelskip", ""), HU
                             .SIZE_5) + " Use Avery 8160 or 5160. Print with top margin: 0.5in, left: 0.19in";
            }
            buffer.append(formEntry(request, msgLabel("Printing"), print));

	    if (dbInfo.getHasLocation()) {
		String simpleMap = HU.labeledCheckbox(
						      ARG_DB_SIMPLEMAP, "true",
							     request.get(ARG_DB_SIMPLEMAP, false),
							     "Simple");

		buffer.append(formEntry(request, "Map:",simpleMap+HU.space(2)+"Height:" +
					HU.input("mapheight",request.getString("mapheight","500"),HU.SIZE_5)));
		buffer.append(formEntry(request, "Map Properties:",
					HU.hbox(
						HU.textArea(ARG_DB_MAPPROPS, request.getString(ARG_DB_MAPPROPS,defaultMapProperties), 5, 20),
						"e.g.:<pre>strokeColor=red\nstrokeWidth=4\nfillColor=transparent\n</pre>")));

	    }
	}

        int    cnt         = 0;
        String formSection = request.getString("formsection", null);
        for (DbNamedBuffer b : buffers) {
	    if(b.getBuffer().length()==0) {
		continue;
	    }
            b.append(HU.formTableClose());
            if (b.anchor != null) {
                sb.append(HU.anchorName(b.anchor));
            }
            String label = HU.div(b.getName(),
                                  HU.cssClass("ramadda-form-header"));
            String contents = b.getBuffer().toString();
            //      sb.append(label);
            boolean show = cnt++ == 0;
            if (formSection != null) {
                if (b.anchor == null) {
                    show = false;
                } else {
                    show = b.anchor.equals(formSection);
                }
            }
            HU.div(sb,
                   HU.makeShowHideBlock(b.getName(), contents, show),
                   HU.cssClass("ramadda-form-block"));
        }

        sb.append(HU.script("Utils.initRangeSelect()"));

        /*
          if (false && request.getUser().getAdmin()) {
          advanced.append(
          formEntry(
          request, "",
          HU.checkbox(ARG_DB_ALL, "true", false) + " "
          + msg("Search across all databases")));
          }
        */
        /*
          advanced.append(HU.formTableClose());
          sb.append("<tr><td colspan=3>");
          HU.makeAccordion(sb, msg("Advanced..."),
          HU.inset(advanced.toString(), 0, 20,
          10, 0));
          sb.append("</td></tr>");
        */

    }

    public Result handleSearch(Request request, Entry entry)
            throws Exception {
        boolean canEdit = getAccessManager().canDoEdit(request, entry);
        if (canEdit && request.exists(ARG_DB_DOSAVESEARCH)) {
            request.remove(ARG_DB_DOSAVESEARCH);
            String args = request.getUrlArgs(
                              (HashSet<String>) Utils.makeHashSet(
                                  ARG_ENTRYID, ARG_DB_SEARCH,
                                  ARG_DB_DOSAVESEARCH), null, null);
            String name = request.getString(ARG_DB_SEARCHNAME, null);
            if ( !Utils.stringDefined(name)) {
                name = "Saved Search";
            }
            Metadata metadata = new Metadata(getRepository().getGUID(),
                                             entry.getId(),
                                             getMetadataManager().findType(METADATA_SAVEDSEARCH), false,
                                             name, args, null, null, null);
            getMetadataManager().addMetadata(request,entry, metadata);
            request.put(ARG_DB_SEARCHID, metadata.getId());
            getEntryManager().updateEntry(request, entry);
        }

        if (request.exists(ARG_DB_SEARCHID)) {
            List<Metadata> savedSearchMetadata =
                getMetadataManager().getMetadata(request,entry, METADATA_SAVEDSEARCH);
            String id = request.getString(ARG_DB_SEARCHID, "");
            for (Metadata m : savedSearchMetadata) {
                if (m.getId().equals(id)) {
                    Request r = request.cloneMe();
                    r.clearUrlArgs();
                    r.put(ARG_ENTRYID, entry.getId());
                    r.put(ARG_DB_SEARCHID, id);
                    String[] toks = m.getAttr2().split("&");
                    for (String tok : toks) {
                        List<String> pair  = StringUtil.splitUpTo(tok, "=",
                                                 2);
                        String       value = ((pair.size() > 1)
                                ? pair.get(1)
                                : "");
                        value = value.replaceAll("\\+", " ");
                        value = HU.urlDecode(value);
                        //false means not singular
                        r.put(pair.get(0), value, false);
                    }
                    r.remove(ARG_DB_SEARCHNAME);
                    request = r;

                    break;
                }
            }
        }

        StringBuilder sb             = new StringBuilder();
        Clause        idClause       = null;
        List<Clause>  where          = new ArrayList<Clause>();
        StringBuilder searchCriteria = new StringBuilder();

        if (request.get(ARG_DB_ALL, false) && request.getUser().getAdmin()) {
            System.err.println("searching all");
        } else {
            idClause = Clause.eq(COL_ID, entry.getId());
        }
        String textToSearch = (String) request.getString(ARG_TEXT, "").trim();
        if (textToSearch.length() > 0) {
            addTextDbSearch(request, textToSearch, searchCriteria, where,
                            false, false, false);
        }

        for (Column column : getDbColumns()) {
            column.assembleWhereClause(request, where, searchCriteria);
        }

	//	System.err.println("CLAUSES:" + where);
        //candidate = '${value1}' AND full_name   not in (select full_name from db_boulder_campaign_contributions where
        //candidate = '${value2}')

        for (MacroStatement macro : macros) {
            String       stmt   = macro.statement;
            boolean      allSet = true;
            List<Clause> ands   = new ArrayList<Clause>();
            //      System.err.println(macro.name);
            for (int i = 0; i < macro.columns.size(); i++) {
                Column       c            = macro.columns.get(i);
                List<Clause> macroClauses = new ArrayList<Clause>();
                c.assembleWhereClause(request, macroClauses, searchCriteria);
                //              System.err.println("\t" + macroClauses);
                if (macroClauses.size() == 0) {
                    allSet = false;
                    continue;
                }
                if (macro.type.equals(macro.TYPE_NOTIN)) {
                    ands.add(Clause.and(macroClauses));
                    //xxxx

                } else {
                    if ( !allSet) {
                        break;
                    }
                    Clause and = Clause.and(macroClauses);
                    and.sanitizeValues();
                    stmt = stmt.replace("${expr" + (i + 1) + "}",
                                        and.toString());
                }
            }

            if (macro.type.equals(macro.TYPE_NOTIN)) {
                if (ands.size() > 0) {
		    //                    System.err.println(macro.name + " clause:" + ands);
                    where.add(Clause.notin(macro.column, macro.column,
                                           tableHandler.getTableName(),
                                           Clause.and(ands)));
                }
            } else if (allSet) {
                where.add(new Clause(stmt.trim()));
            }

        }
        Clause mainClause = null;
        if (where.size() > 0) {
            if (request.get(ARG_DB_OR, false)) {
                mainClause = Clause.or(where);
            } else {
                mainClause = Clause.and(where);
            }
        }
        if (idClause != null) {
            if (mainClause == null) {
                mainClause = idClause;
            } else {
                mainClause = Clause.and(idClause, mainClause);
            }
        }

        //        Runtime.getRuntime().gc();
        //        double mem1   = Utils.getUsedMemory();
        Result result = handleQuery(request, entry, "", mainClause, true);

        //        double mem2   = Utils.getUsedMemory();
        //      Utils.printMemory("handleQuery:",mem1,mem2);
        return result;
    }

    private void deleteEntireDatabase(Request request, Entry entry)
            throws Exception {
        if ( !getAccessManager().canDoEdit(request, entry)) {
            throw new RuntimeException(
                "Don't have permission to delete entire database");
        }
        Statement statement = getDatabaseManager().createStatement();
        try {
            String query = SqlUtil.makeDelete(tableHandler.getTableName(),
                               COL_ID,
                               SqlUtil.quote(entry.getId().toString()));
            statement.execute(query);
        } finally {
            getRepository().getDatabaseManager().closeAndReleaseConnection(
                statement);
            entryChanged(request, entry);
        }
    }

    public Result handleActionConfirm(Request request, Entry entry)
            throws Exception {
        String  action = request.getString(ARG_DB_ACTION, ACTION_DELETE);
        boolean deleteSelected = action.equals(ACTION_DELETE);
        boolean deleteAll = action.equals(ACTION_DELETEALL);
        boolean setLatLon = action.equals(ACTION_SET_LATLON);	
        List    dbids = request.get(ARG_DBID_SELECTED, new ArrayList());
	String msg = "Unknown action";
        if (deleteSelected) {
            Statement statement = getDatabaseManager().createStatement();
            try {
                for (Object dbid : dbids) {
                    String query =
                        SqlUtil.makeDelete(tableHandler.getTableName(),
                                           COL_DBID,
                                           SqlUtil.quote(dbid.toString()));
                    statement.execute(query);
                }
            } finally {
                getRepository().getDatabaseManager()
                    .closeAndReleaseConnection(statement);
            }
	    msg = "Entries deleted";
        } else if(deleteAll) {
            deleteEntireDatabase(request, entry);
	    msg = "Entire database deleted";
        } else if(setLatLon) {
	    DbInfo              dbInfo     = getDbInfo();
	    Column latColumn = dbInfo.getLatColumn();
	    Column lonColumn = dbInfo.getLonColumn();
	    Column latLonColumn = dbInfo.getLatLonColumn();	    	    
	    if(latColumn==null || lonColumn == null) {
		if(latLonColumn==null)
		    throw new IllegalStateException("No lat/lon columns");
	    }
	    Connection        connection = getDatabaseManager().getConnection();
	    String[] cols  =latLonColumn!=null?
		new String[]{latLonColumn.getName()+"_lat", latLonColumn.getName()+"_lon"}:
		new String[]{latColumn.getName(),lonColumn.getName()};

	    try {
		for (Object dbid : dbids) {
		    Clause clause = makeClause(entry, dbid.toString());
		    Object[]values = new Object[]{request.get(ARG_LOCATION+".latitude", Entry.NONGEO),
						  request.get(ARG_LOCATION+".longitude", Entry.NONGEO)};
		    System.err.println(values[0] +" " + values[1]);
		    SqlUtil.update(connection,tableHandler.getTableName(),clause,cols,values);
		}
	    } finally {
		getRepository().getDatabaseManager().closeConnection(connection);
	    }
	    msg = "Locations set";
	} else {
	    throw new IllegalArgumentException("Unknown action");
	}
	entryChanged(request, entry);

        String url =
            HU.url(request.makeUrl(getRepository().URL_ENTRY_SHOW),
                          new String[] { ARG_ENTRYID,
                                         entry.getId(), ARG_MESSAGE,
                                         msg});

        return new Result(url);
    }

    public void dbChanged(Entry entry) throws Exception {
        getStorageManager().clearCacheGroup(entry.getId());
    }

    private void entryChanged(Request request, Entry entry) throws Exception {
	dbChanged(entry);
	entry.setChangeDate(new Date().getTime());
	getEntryManager().updateEntry(request, entry);
    }

    public Result handleActionAsk(Request request, Entry entry, String action)
            throws Exception {
        StringBuilder sb    = new StringBuilder();
        List          dbids = request.get(ARG_DBID_SELECTED, new ArrayList());

        if (request.exists(ARG_DB_DELETE)) {
            action = ACTION_DELETE;
        }
        boolean deleteSelected = action.equals(ACTION_DELETE);
        boolean deleteAll = action.equals(ACTION_DELETEALL);
        boolean setLatLon = action.equals(ACTION_SET_LATLON);		
        if (deleteSelected && (dbids.size() == 0)) {
            sb.append(
                getPageHandler().showDialogWarning(
                    msg("No entries were selected")));
        } else {
            String formUrl = request.makeUrl(getRepository().URL_ENTRY_SHOW);
            sb.append(HU.formPost(formUrl));
            sb.append(HU.hidden(ARG_ENTRYID, entry.getId()));
            sb.append(HU.hidden(ARG_DB_ACTION, action));
            for (Object dbid : dbids) {
                sb.append(HU.hidden(ARG_DBID_SELECTED,
                                           dbid.toString()));
            }

            String msg ="Unknown action";
	    String extra = "";
	    if(deleteSelected)
                msg  ="Are you sure you want to delete the selected entries?";
            else if (deleteAll) 
                msg = "Are you sure you want to delete the entire database? Like, the entire database!";
            else if (setLatLon)  {
                msg = "Are you sure you want to set the lat/lons to:<br>";
		String[] nwse = new String[] { "",""};
		MapInfo map = getMapManager().createMap(request, entry, true,
							getMapManager().getMapProps(request, entry, null));
		extra = map.makeSelector(ARG_LOCATION, true, nwse,
					 "", "");

	    }

            addViewHeader(request, entry, sb, "", null);
            sb.append(getPageHandler().showDialogQuestion(msg(msg),
                    HU.submit(LABEL_YES, ARG_DB_ACTION_CONFIRM)
                    + HU.space(2)
                    + HU.submit(LABEL_CANCEL, ARG_DB_LIST)));
	    sb.append(extra);
            addViewFooter(request, entry, sb);
        }

        sb.append(HU.formClose());

        return new Result(getTitle(request, entry), sb);
    }

    private void putProp(String prop, Object[] values, Object obj)
            throws Exception {
        Hashtable props = getProps(values);
        if (props == null) {
            props = new Hashtable();
        }
        props.put(prop, obj);
        values[IDX_DBPROPS] = xmlEncoder.toXml(props);
    }

    private double getProp(String prop, Object[] values, double dflt)
            throws Exception {
        Hashtable props = getProps(values);
        if (props == null) {
            return dflt;
        }
        Double d = (Double) props.get(prop);
        if (d == null) {
            return dflt;
        }

        return d.doubleValue();
    }

    private int getProp(String prop, Object[] values, int dflt)
            throws Exception {
        Hashtable props = getProps(values);
        if (props == null) {
            return dflt;
        }
        Integer d = (Integer) props.get(prop);
        if (d == null) {
            return dflt;
        }

        return d.intValue();
    }

    private Hashtable getProps(Object[] values) throws Exception {
        String value = (String) values[IDX_DBPROPS];
        if ((value == null) || (value.length() == 0)) {
            return null;
        }

        return (Hashtable) xmlEncoder.toObject(value);
    }

    private Result handleBulkUpload(final Request request, final Entry entry,
				    List<NamedInputStream> sources)
            throws Exception {

        String delimiter = request.getString(ARG_DB_BULK_DELIMITER, ",");
        if ( !Utils.stringDefined(delimiter)) {
            delimiter = ",";
        }
        int                       skip = request.get(ARG_DB_BULK_SKIP, 1);
        final DbInfo              dbInfo     = getDbInfo();
	final List<Column> columns = dbInfo.getRealColumnsToUse();

        final int[]               totalCnt       = { 0 };
	final int[]               cnt        = { 0 };
	final ArrayList<Object[]> valueList  = new ArrayList<Object[]>();

        final Bounds bounds = new Bounds(Double.NaN, Double.NaN, Double.NaN,
                                         Double.NaN);

        if (request.get(ARG_DB_BULK_NUKEIT, false)) {
            deleteEntireDatabase(request, entry);
        }

	Processor myProcessor = new Processor() {
		int myCnt=0;
		@Override
		public org.ramadda.util.seesv.Row handleRow(TextReader textReader,
							   org.ramadda.util.seesv.Row row) {
		    try {
			myCnt++;
			if(myCnt%1000==0) System.err.println("count:" + myCnt);
			Object[] values = tableHandler.makeEntryValueArray();
			cnt[0]++;
			initializeValueArray(request, null, values);

			List<String> toks = row.getValues();
			if (toks.size() != columns.size()) {
			    String error =
				"Wrong number of values. Given line has: "
				+ toks.size() + " Expected:"
				+ columns.size() + "\n"
				+ "Columns:" + columns + "\n"
				+ "Data:" + toks;
			    for(int i=0;i<Math.max(toks.size(),columns.size());i++) {
				String column = i<columns.size()?columns.get(i).getName():"MISSING";
				String value= i<toks.size()?toks.get(i):"MISSING";				
				error+="\n\t" + i+":"+  column+"="+value;
			    }
			    error+="\n";
			    System.err.println(error);
			    throw new IllegalArgumentException(error);
			}
			String keyValue = null;

			for (int colIdx = 0; colIdx < toks.size(); colIdx++) {
			    Column column = columns.get(colIdx);
			    String value  = (String) toks.get(colIdx).trim();
			    if (column == dbInfo.getKeyColumn()) {
				keyValue = value;
			    }
			    try {
				column.setValue(entry, values, value);
			    } catch (Exception exc) {
				exc.printStackTrace();

				throw new RuntimeException(
							   "Error setting column value:"
							   + column.getName() + "\nvalues:" + toks
							   + "\nError:" + exc, exc);
			    }
			}

			if (dbInfo.getHasLocation()) {
			    double[] ll  = getLocation(request,values);
			    double   lat = ll[0];
			    double   lon = ll[1];
			    if ( !Double.isNaN(lat) && !Double.isNaN(lon)) {
				bounds.expand(lat, lon);
			    }
			}

			if (keyValue != null) {
			    Clause clause =
				Clause.and(
					   Clause.eq(
						     GenericTypeHandler.COL_ID,
						     entry.getId()), Clause.eq(
									       dbInfo.getKeyColumn().getName(),
									       keyValue));
			    System.err.println("drop:" + clause);
			    getDatabaseManager().delete(
							tableHandler.getTableName(), clause);
			}

			valueList.add(values);
			if (valueList.size() > 10000) {
			    totalCnt[0] += valueList.size();
			    long t1 = System.currentTimeMillis();
			    //			    Connection        connection = getDatabaseManager().getConnection();
			    //			    connection.setAutoCommit(false);
			    doStore(entry, valueList, true);
			    //			    connection.commit();   connection.setAutoCommit(true);
			    long t2 = System.currentTimeMillis();
			    Utils.printTimes("DbTypeHandler.bulkUpload: stored: "
					     + totalCnt[0], t1, t2);
			    valueList.clear();
			}

			return row;
		    } catch (Exception exc) {
			fatal(textReader, "Loading db", exc);
			return null;
		    }
		}
	    };

	for(NamedInputStream input: sources) {
	    System.err.println("DbTypeHandler.bulkUpload: processing:" + input.getName());
	    InputStream  source = input.getInputStream();
	    if(input.getName().toLowerCase().endsWith(".zip")) {
		ZipInputStream zin = new ZipInputStream(source);
		ZipEntry       ze  = null;
		while ((ze = zin.getNextEntry()) != null) {
		    if (ze.isDirectory()) {
			continue;
		    }
		    String p = ze.getName().toLowerCase();
		    if (p.endsWith(".csv")) {
			source = zin;
			break;
		    }
		}
	    }		
	    valueList.clear();
	    cnt[0]=0;
	    TextReader                textReader = new TextReader();
	    textReader.setInput(new NamedInputStream("input",
						     new BufferedInputStream(source)));
	    textReader.setSkipRows(skip);
	    textReader.setDelimiter(delimiter);
	    textReader.addProcessor(myProcessor);
	    Seesv csvUtil = new Seesv(new ArrayList<String>());
	    DataProvider.CsvDataProvider provider =
		new DataProvider.CsvDataProvider(textReader,0);
	    csvUtil.process(textReader, provider,0);
	    totalCnt[0] += valueList.size();
	    doStore(entry, valueList, true);
	    source.close();
	}

        System.err.println("DbTypeHandler.bulkUpload: final stored: "
                           + totalCnt[0]);

        if ( !entry.hasAreaDefined(request) && !Double.isNaN(bounds.getNorth())) {
            entry.setBounds(bounds);
        }
	entryChanged(request, entry);

        //Remove these so any links that get made with the request don't point to the BULK upload
        request.remove(ARG_DB_NEWFORM, ARG_DB_BULK_TEXT,
		       ARG_DB_BULK_FILE,
		       ARG_DB_BULK_LOCALFILE);
        tableHandler.clearCache();
        StringBuilder sb = new StringBuilder();
	addViewHeader(request, entry, sb,"","");
        sb.append(getPageHandler().showDialogNote("Added " + totalCnt[0] + " entries"));
        getPageHandler().entrySectionClose(request, entry, sb);
        return new Result("", sb);
    }

    List<Column> getColumnsToUse(Request request, boolean checkCanList) {
        HashSet only   = null;
        String  select = request.getString("dbSelect");
        if ((select != null) && (select.length() > 0)) {
            only = Utils.makeHashSet(
                SqlUtil.sanitize(StringUtil.split(select, ",", true, true)));
        }
        List<Column> columns         = dbInfo.getColumnsToUse();
        List<Column> selectedColumns = new ArrayList<Column>();
        List<Column> columnsToUse;

        boolean      anyFromArgs = false;
        for (Column column : columns) {
            String arg = ARG_DB_SHOW + "_" + column.getName();
            if (request.get(arg, false)) {
                anyFromArgs = true;
                selectedColumns.add(column);
            }
        }

        if (anyFromArgs) {
            columnsToUse = selectedColumns;
        } else {
            columnsToUse = columns;
        }
        if (only != null) {
            List<Column> tmp = new ArrayList<Column>();
            for (Column c : columnsToUse) {
                if (only.contains(c.getName())) {
                    tmp.add(c);
                }
            }
            columnsToUse = tmp;
        }

        if (anyFromArgs) {
            return columnsToUse;
        }
        if (checkCanList) {
            List<Column> tmp = new ArrayList<Column>();
            for (Column column : columnsToUse) {
                if (column.getCanList()) {
                    tmp.add(column);
                }
            }
            columnsToUse = tmp;
        }

        return columnsToUse;
    }

    private List<Column> getSelectedColumns(Request request, boolean all) {
        List<Column> columns = new ArrayList<Column>();
        if (all) {
            List<Column> allColumns = tableHandler.getColumns();
            for (int i = 0; i <= IDX_MAX_INTERNAL; i++) {
                columns.add(allColumns.get(i));
            }
        }
        columns.addAll(getColumnsToUse(request, false));

        return columns;
    }

    public Result handleNewOrEdit(Request request, Entry entry, String dbid,
                                  boolean fromAnonForm)
            throws Exception {
	try {
	    return  handleNewOrEditInner(request, entry,  dbid,
					 fromAnonForm);

	} catch(Exception exc) {
	    StringBuilder sb = new StringBuilder();
	    addViewHeader(request, entry, sb, VIEW_EDIT, null);
	    sb.append(getPageHandler().showDialogError("An error has occurred: " + exc));
	    getLogManager().logError("Error creating new data for  DB entry:" + entry, exc);
	    return new Result(getTitle(request, entry), sb);
	}	
    }

    private Result handleNewOrEditInner(Request request, Entry entry, String dbid,
                                  boolean fromAnonForm)
            throws Exception {

        if (request.exists(ARG_DB_COPY)) {
            dbid = null;
        }

        boolean isNew = dbid == null;
        if ( !fromAnonForm
                && (request.exists(ARG_DB_BULK_TEXT)
                    || request.exists(ARG_DB_BULK_FILE)
                    || request.exists(ARG_DB_BULK_LOCALFILE))) {
	    List<NamedInputStream> sources = new ArrayList<NamedInputStream>();
            NamedInputStream source = null;
            String      bulkContent;

            if (request.exists(ARG_DB_BULK_FILE)) {
                File f = new File(request.getUploadedFile(ARG_DB_BULK_FILE));
                if ( !f.exists()) {
                    throw new IllegalArgumentException(
                        "Uploaded file does not exist");
                }
                if (f.toString().toLowerCase().endsWith(".xls")) {
                    source = new NamedInputStream(f.toString(),XlsUtil.xlsToCsv(new IO.Path(f.toString())));
                } else if (f.toString().toLowerCase().endsWith(".xlsx")) {
                    source = new NamedInputStream(f.toString(),XlsUtil.xlsxToCsv(new IO.Path(f.toString())));
                } else {
                    source = new NamedInputStream(f.toString(),getStorageManager().getFileInputStream(f));
                }
            } else if (request.exists(ARG_DB_BULK_LOCALFILE)) {
                request.ensureAdmin();
		File localFile = new File(request.getString(ARG_DB_BULK_LOCALFILE, "").trim());
		if(localFile.isDirectory()) {
		    //Only look for csv
		    for(File child: localFile.listFiles()) {
			if(!child.toString().toLowerCase().endsWith(".csv")) continue;
			sources.add(new NamedInputStream(child.toString(),getStorageManager().getFileInputStream(child)));
		    }
		} else {
		    source = new NamedInputStream(localFile.toString(),getStorageManager().getFileInputStream(localFile));
		}
            } else {
                source = new NamedInputStream("inline text",new ByteArrayInputStream(
										     request.getString(ARG_DB_BULK_TEXT, "").getBytes()));
            }
	    if(source!=null && sources.size()==0) sources.add(source);

            return handleBulkUpload(request, entry, sources);
        }

        StringBuilder sb       = new StringBuilder();
        List<String>  colNames = tableHandler.getColumnNames();
        Object[]      values   = getValues(entry, dbid);
        initializeValueArray(request, dbid, values);
        for (Column column : getDbColumns()) {
            if ( !isNew && !column.getEditable()) {
                continue;
            }
            column.setValue(request, entry, values);
        }

        doStore(entry, values, dbid == null);

        if (fromAnonForm) {
            Hashtable props = getProperties(entry);
            String message = Misc.getProperty(props, PROP_ANONFORM_MESSAGE,
                                 "");
            if ( !Utils.stringDefined(message)) {
                message = "Thank you for submitting an entry";
            }

            return new Result("", new StringBuffer(insetHtml(message)));
        }

        String url =
            HU.url(request.makeUrl(getRepository().URL_ENTRY_SHOW),
                          new String[] {
            ARG_ENTRYID, entry.getId(), ARG_DBID, (String) values[IDX_DBID],
            ARG_DB_EDITFORM, "true"
        });

        return new Result(url);
    }

    protected void initializeValueArray(Request request, String dbid,
                                        Object[] values) {
        initializeValueArray(request, dbid, request.getUser().getId(),
                             values);
    }

    protected void initializeValueArray(Request request, String dbid,
                                        String user, Object[] values) {
        //The first entry is the db_id
        values[IDX_DBID] = ((dbid == null)
                            ? getRepository().getGUID()
                            : dbid);

        if (dbid == null) {
            values[IDX_DBUSER]       = user;
            values[IDX_DBCREATEDATE] = new Date();
            values[IDX_DBPROPS]      = "";
        }
    }

    protected void doStore(Entry entry, Object[] values, boolean isNew)
            throws Exception {
        List valueList = new ArrayList<Object[]>();
        valueList.add(values);
        doStore(entry, valueList, isNew);
    }

    protected void doStore(Entry entry, List<Object[]> valueList,
                           boolean isNew)
            throws Exception {
        Connection        connection = getDatabaseManager().getConnection();
	try {
	    connection.setAutoCommit(false);
	    doStore(entry, valueList,isNew,connection);
	    connection.commit();
	    connection.setAutoCommit(true);
        } finally {
	    connection.close();
            dbChanged(entry);
        }
    }

    protected void doStore(Entry entry, List<Object[]> valueList,
                           boolean isNew,Connection connection)
            throws Exception {
        if (valueList.size() == 0) {
            return;
        }
        String            dbid       = (String) valueList.get(0)[IDX_DBID];
        String            sql        = makeInsertOrUpdateSql(entry, (isNew
                ? null
                : dbid));
	//	System.err.println(sql);
        PreparedStatement stmt       = connection.prepareStatement(sql);
	for (Object[] values : valueList) {
	    int stmtIdx = tableHandler.setStatement(entry, values, stmt,
						    isNew);
	    if ( !isNew) {
		stmt.setString(stmtIdx, dbid);
	    }
	    stmt.execute();
	}
	stmt.close();
    }

    public Clause makeClause(Entry entry, String dbid) {
        return Clause.and(Clause.eq(COL_ID, entry.getId()),
                          Clause.eq(COL_DBID, dbid));

    }

    public String makeInsertOrUpdateSql(Entry entry, String dbid) {
        if (dbid == null) {
            return SqlUtil.makeInsert(
                tableHandler.getTableName(),
                SqlUtil.comma(tableHandler.getColumnNames()),
                SqlUtil.getQuestionMarks(
                    tableHandler.getColumnNames().size()));
        } else {
            Clause clause = makeClause(entry, dbid);
            return SqlUtil.makeUpdate(tableHandler.getTableName(), clause,
                                      namesArray);

        }

    }

    public Result handleListEmail(Request request, Entry entry,
                                  List<Object[]> valueList)
            throws Exception {

        Column theColumn = null;
        for (Column column : getDbColumns()) {
            if (column.getType().equals(Column.DATATYPE_EMAIL)) {
                theColumn = column;
            }
        }
        if (theColumn == null) {
            throw new IllegalStateException("No email data found");
        }
        StringBuilder sb = new StringBuilder();
        makeForm(request, entry, sb);
        sb.append(HU.hidden(ARG_DB_ACTION, ACTION_EMAIL));
        sb.append(HU.submit(("Send Message")));
        sb.append(HU.space(2));
        sb.append(HU.submit(LABEL_CANCEL, ARG_DB_LIST));
        sb.append(HU.formTable());

        for (Object[] values : valueList) {
            String toId = (String) values[IDX_DBID];
            sb.append(HU.hidden(ARG_DBID_SELECTED, toId));
        }

        sb.append(HU.hidden(ARG_ENTRYID, entry.getId()));
        sb.append(formEntry(request, msgLabel("From name"),
                            HU.input(ARG_EMAIL_FROMNAME,
                                            request.getUser().getName(),
                                            HU.SIZE_40)));
        sb.append(formEntry(request, msgLabel("From email"),
                            HU.input(ARG_EMAIL_FROMADDRESS,
                                            request.getUser().getEmail(),
                                            HU.SIZE_40)));
        String bcc = HU.checkbox(ARG_EMAIL_BCC, "true", false)
                     + HU.space(1) + msg("Send as BCC");

        sb.append(
            formEntry(
                request, msgLabel("Subject"),
                HU.input(ARG_EMAIL_SUBJECT, "", HU.SIZE_40)
                + HU.space(2) + bcc));
        sb.append(
            HU.formEntryTop(
                msgLabel("Message"),
                HU.textArea(ARG_EMAIL_MESSAGE, "", 30, 60)));
        sb.append(HU.formTableClose());
        sb.append(HU.submit("Send Message"));
        sb.append(HU.formTableClose());
        sb.append(HU.formClose());

        return new Result(getTitle(request, entry), sb);
    }

    protected double[] getLocation(Request request,Object[] values) {
        DbInfo dbInfo = getDbInfo();
        if (dbInfo.getLatLonColumn() != null) {
            return dbInfo.getLatLonColumn().getLatLon(request,values);
        } else if ((dbInfo.getLatColumn() != null)
                   && (dbInfo.getLonColumn() != null)) {
            return new double[] { dbInfo.getLatColumn().uncheckedGetDouble(request,values),
                                  dbInfo.getLonColumn().uncheckedGetDouble(request,values) };
        }

        return null;

    }

    public String getEventJS(Request request, Entry entry, Object dbRowId,
                             String rowId, String divId) {
        StringBuilder sb = new StringBuilder();
        String xmlUrl = getViewUrl(request, entry, dbRowId.toString())
                        + "&result=xml";
        rowId = HU.squote(rowId);
        divId = HU.squote(divId);
        String popupId = HU.squote("dbrowpopup_" + entry.getId());
        sb.append(HU.onMouseClick(HU.call("DB.rowClick",
                HU.comma("event", rowId, popupId,
                                HU.squote(xmlUrl)))));

        return sb.toString();
    }

    public Result handleListTable(Request request, Entry entry,
                                  List<Object[]> valueList,
                                  boolean fromSearch, boolean showHeaderLinks)
            throws Exception {
        return handleListTable(request, entry, valueList, fromSearch,
                               showHeaderLinks, new StringBuilder());
    }

    public Result handleListTable(Request request, Entry entry,
                                  List<Object[]> valueList,
                                  boolean fromSearch,
                                  boolean showHeaderLinks, Appendable sb)
            throws Exception {
        List<String> links    = new ArrayList<String>();
        boolean      embedded = isEmbedded(request);
        if ( !embedded) {
            addViewHeader(request, entry, sb, VIEW_TABLE, valueList.size(),
                          fromSearch, null);
        } else {
            addStyleSheet(request,sb);
        }
        boolean doGroupBy = isGroupBy(request);
        if (doGroupBy) {
            makeGroupByTable(request, entry, valueList, sb,
                             showHeaderLinks
                             && !isEmbedded(request));
        } else {
            makeTable(request, entry, valueList, fromSearch, sb,
                      !request.isAnonymous(),
                      showHeaderLinks && !isEmbedded(request));
        }

        if ( !embedded) {
            addViewFooter(request, entry, sb);
        }

        return new Result(getTitle(request, entry), sb);
    }

    protected void addStyleSheet(Request request, Appendable sb) throws Exception {
	if(request.getExtraProperty("added dbjs")==null) {
	    request.putExtraProperty("added dbjs","true");
	    HU.cssLink(sb,
			      getPageHandler().makeHtdocsUrl("/db/dbstyle.css"));
	    HU.importJS(sb, getPageHandler().makeHtdocsUrl("/db/db.js"));
	}
    }

    protected void makeTableHeader(Appendable sb, String contents,
                                   String... extraAttrs)
            throws Exception {
        StringBuilder attrs = new StringBuilder();
        HU.cssClass(attrs, CSS_DB_TABLEHEADER);
        for (String attr : extraAttrs) {
            attrs.append(" ");
            attrs.append(attr);
        }
        HU.col(
            sb, HU.div(
                contents, HU.cssClass(
                    CSS_DB_TABLEHEADER_INNER)), attrs.toString());
    }

    public void makeTable(Request request, Entry entry,
                          List<Object[]> valueList, boolean fromSearch,
                          Appendable sb, boolean doForm,
                          boolean showHeaderLinks)
            throws Exception {
        DbInfo           dbInfo       = getDbInfo();
        List<Column>     columnsToUse = getColumnsToUse(request, true);

        SimpleDateFormat sdf          = getDateFormat(request, entry);
        SimpleDateFormat dateTimeSdf  = getDateTimeFormat(request, entry);
        Hashtable        entryProps   = getProperties(entry);
        StringBuilder    hb           = new StringBuilder();
        if (doForm) {
            String formUrl = request.makeUrl(getRepository().URL_ENTRY_SHOW);
            hb.append(HU.form(formUrl));
            hb.append(HU.hidden(ARG_ENTRYID, entry.getId()));
        }
        boolean canEdit = getAccessManager().canDoEdit(request, entry);
        boolean forPrint       = request.get(ARG_FOR_PRINT, false);
        int     entriesPerPage = request.get(ARG_ENTRIES_PER_PAGE, 8);
	boolean numberEntries = request.get(ARG_NUMBER_ENTRIES,false);
        if (forPrint) {
            canEdit = false;
        }
        HashSet<String> except = new HashSet<String>();
        for (int i = 1; i <= numOrders; i++) {
            except.add(ARG_DB_SORTBY + i);
            except.add(ARG_DB_SORTDIR + i);
        }

        String baseUrl = request.getUrl(except, null);
        boolean asc = request.getString(ARG_DB_SORTDIR1,
                                        (dbInfo.getDfltSortAsc()
                                         ? ORDER_ASC
                                         : ORDER_DESC)).equals(ORDER_ASC);

        String sortBy = request.getString(ARG_DB_SORTBY1,
                                          ((dbInfo.getDfltSortColumn()
                                            == null)
                                           ? ""
                                           : dbInfo.getDfltSortColumn()
                                               .getName()));

        List<String> extraCols =
            Utils.split(request.getString(ARG_EXTRA_COLUMNS, ""), "\n", true,
                        true);
        StringBuilder tableHeader = new StringBuilder();
        if (valueList.size() > 0) {
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
                actions.add(new TwoFacedObject("Set lat/lon",
                        ACTION_SET_LATLON));		
            }

            if ( !isEmbedded(request) && (actions.size() > 0)) {
                if (doForm) {
                    hb.append(HU.submit("Do:", ARG_DB_DO));
                    hb.append(HU.select(ARG_DB_ACTION, actions));
                }
            }

            HU.open(tableHeader, "table", "class", "dbtable",
                           "border", "1", "cellspacing", "0", "cellpadding",
                           "0", "width", "100%");
            HU.open(tableHeader, "tr", "valign", "top");
            if ( !forPrint) {
                makeTableHeader(tableHeader, "&nbsp;");
            } else {
		if(numberEntries)
		    makeTableHeader(tableHeader, "#");
	    }
            for (int i = 0; i < columnsToUse.size(); i++) {
                Column column = columnsToUse.get(i);
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
                    makeTableHeader(tableHeader, label);
                    continue;
                }
                String sortColumn = column.getName();
                String extra;
                if (sortColumn.equals(sortBy)) {
                    if (asc) {
                        extra = " "
                                + HU.img(
                                    getRepository().getIconUrl(ICON_UPDART));
                    } else {
                        extra = " "
                                + HU.img(
                                    getRepository().getIconUrl(
                                        ICON_DOWNDART));
                    }
                    asc = !asc;
                } else {
                    extra = " "
                            + HU.img(
                                getRepository().getIconUrl(ICON_BLANK), "",
                                HU.attr("width", "10"));
                }

                String link = HU.href(baseUrl + "&" + ARG_DB_SORTBY1
                                             + "=" + sortColumn + "&"
                                             + ARG_DB_SORTDIR1 + (asc
                        ? "=asc"
								  : "=desc"), label,HU.attrs(HU.ATTR_REL,"nofollow")) + extra;
                makeTableHeader(tableHeader, link);
            }
            for (String col : extraCols) {
                makeTableHeader(tableHeader, col);
            }

            HU.close(tableHeader, "tr");
        }

        String searchFrom   = request.getString(ARG_SEARCH_FROM, null);
        String searchColumn = null;
        String sourceName   = null;
        String sourceColumn = null;
        if (searchFrom != null) {
            List<String> toks = Utils.split(searchFrom, ";");
            sourceName   = toks.get(0);
            sourceColumn = toks.get(1);
            searchColumn = toks.get(3);
        }
        if (searchColumn != null) {
            String href = HU.href(
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
                                                  "Select all for "
                                                  + sourceName + ":"
                                                  + sourceColumn));
            String clear = HU.href("#",
                                   "Clear all " + sourceName + ":"
                                   + sourceColumn, HU.attr("onclick",
                                       "return DB.doDbClearAll(event,'"
                                       + searchFrom
                                       + "')") + HU.attr("class",
                                           "ramadda-clickable"));
            hb.append(HU.space(2) + href + HU.space(2) + clear);
        }

        hb.append(tableHeader);

        Hashtable<String, Hashtable<Object, Integer>> uniques =
            new Hashtable<String, Hashtable<Object, Integer>>();

        String popupId = "dbrowpopup_" + entry.getId();
        hb.append(HU.div("",
                                HU.id(popupId)
                                + HU.cssClass("ramadda-popup")));

        double[] sum     = new double[columnsToUse.size()];
        double[] min     = new double[columnsToUse.size()];
        double[] max     = new double[columnsToUse.size()];

        boolean  even    = true;
        int      lineCnt = 0;
        for (int cnt = 0; cnt < valueList.size(); cnt++) {
            lineCnt++;  
          if (forPrint && (lineCnt > entriesPerPage)) {
                lineCnt = 0;
                hb.append("</table>");
                hb.append("<div class=pagebreak></div>");
                hb.append(tableHeader);
            }
            Object[] values = valueList.get(cnt);

            String   dbid   = (String) values[IDX_DBID];
            String   cbxId  = ARG_DBID + (cnt);
            String   rowId  = "row_" + dbid;
            String   divId  = "div_" + dbid;
            String event = getEventJS(request, entry, values[IDX_DBID],
                                      rowId, divId);
            hb.append("\n");
            hb.append(HU.open(HU.TAG_TR, "valign", "top",
                                     "class", (even
                    ? " ramadda-row-even "
                    : " ramadda-row-odd ") + " dbrow ", "id", rowId, "title",
                    "Click to view details"));

	    if(numberEntries) {
                HU.td(hb, "#"+(cnt+1),HU.attr("width","10px"));
	    }

            even = !even;
            if ( !forPrint) {
                HU.open(hb, "td", "width", "10", "style",
                               "white-space:nowrap;");
                HU.open(hb, "div", "class", "ramadda-db-div", "id",
                               divId);
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

                    hb.append(HU.checkbox(ARG_DBID_SELECTED, dbid,
                            false, HU.id(cbxId) + call));
                }
                if (canEdit) {
                    String editUrl = getEditUrl(request, entry, dbid);
                    hb.append(
                        HU.href(
                            editUrl,
                            HU.img(
                                getRepository().getUrlBase()
                                + "/db/database_edit.png", msg(
                                    "Edit entry"))));
                }
                String viewUrl = getViewUrl(request, entry, dbid);
                hb.append(
                    HU.href(
                        viewUrl,
                        HU.img(
                            getRepository().getUrlBase()
                            + "/db/database_go.png","View entry")));
                HU.close(hb, "div", "td");
            }

            for (int i = 0; i < columnsToUse.size(); i++) {
                Column column = columnsToUse.get(i);
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
                    HU.open(hb, "td",
                                   event
                                   + HU.attr("class", "dbtablecell"));
                } else if (column.isNumeric()) {
                    HU.open(hb, "td", event + "align=right");
                } else {
                    HU.open(hb, "td", event);
                }

                HU.open(hb, HU.TAG_DIV, "class", "dbtablecell");

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
                                        getDbIconUrl(icon), "",
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
                            hb.append(prefix.toString());
                        }
                    }
                }

                String label = formatTableValue(request, entry, hb, column,
						values, sdf, dateTimeSdf,!forPrint);
                hb.append("&nbsp;");

                boolean addSelect = (searchColumn != null)
                                    && column.getName().equals(searchColumn);
                if (addSelect) {
                    String value = "" + values[column.getOffset()];
                    hb.append(HU.space(1));
                    hb.append(
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
                    hb.append("&nbsp;");
                }
                HU.close(hb, HU.TAG_DIV);
                HU.close(hb, HU.TAG_TD);

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
            for (String col : extraCols) {
                HU.open(hb, HU.TAG_TD, "");
                HU.close(hb, HU.TAG_TD);
            }
            hb.append("</tr>");
        }

        if ( !forPrint && (valueList.size() > 0)) {
            HU.comment(hb, "summmary");
            HU.open(hb, "tr", "valign", "top");
            HU.tag(hb, "td", HU.attrs("align", "right"),
                          "#" + valueList.size());
            for (int i = 0; i < columnsToUse.size(); i++) {
                Column column = columnsToUse.get(i);
                if (column.isNumeric() && column.getDoStats()) {
                    double  avg   = sum[i] / valueList.size();
                    boolean round = column.isInteger();
                    HU.open(hb, "td", "class", "dbtable-summary");
                    hb.append(HU.formTable());
                    hb.append(HU.formEntry("Average:",
                            format(avg, round)));
                    hb.append(HU.formEntry("Minimum:",
                            format(min[i], round)));
                    hb.append(HU.formEntry("Maximum:",
                            format(max[i], round)));
                    hb.append(HU.formEntry("Total:",
                            format(sum[i], round)));
                    HU.close(hb, "table", "td");
                } else if (column.isEnumeration() && column.getDoStats()) {
                    Hashtable<Object, Integer> numUniques =
                        uniques.get(column.getName());
                    if (numUniques == null) {
                        continue;
                    }
                    HU.open(hb, "td", "class", "dbtable-summary");
                    hb.append(HU.formTable());
                    int rowCnt = 0;
                    for (Enumeration keys = numUniques.keys();
                            keys.hasMoreElements(); ) {
                        if (rowCnt++ > 10) {
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
        } else {
            if ( !fromSearch) {
                hb.append(HU.br());
                hb.append(
                    getPageHandler().showDialogNote(
                        msgLabel("No entries in")
                        + getTitle(request, entry)));
            } else {
                hb.append(
                    getPageHandler().showDialogNote(msg("Nothing found")));
            }
        }
        hb.append(HU.formClose());
        sb.append(hb.toString());
    }

    protected List<Column> getGroupByColumns(Request request,
                                             boolean includeAgg)
            throws Exception {
        List<Column> groupByColumns = new ArrayList<Column>();
        List<String> args = (List<String>) (request.get(ARG_GROUPBY,
                                new ArrayList()));
        for (int i = 0; i < args.size(); i++) {
            String col    = args.get(i);
            Column column = getDbColumn(col);
            if (column == null) {
                System.err.println("DbTypeHandler: could not find column:"
                                   + col);

                continue;
            }
            groupByColumns.add(column.cloneColumn());
        }
        if (includeAgg) {
            List<Column> aggColumns = getAggColumns(request);
            groupByColumns.addAll(aggColumns);
            if (aggColumns.size() == 0) {
                Column aggColumn = groupByColumns.get(0).cloneColumn();
                groupByColumns.add(aggColumn);
                String aggType = "count";
                String name    = aggType + "_of_" + aggColumn.getName();
                aggColumn.setName(name);
                aggColumn.setLabel(Utils.makeLabel(name));
                aggColumn.setType(aggColumn.DATATYPE_DOUBLE);
            }
        }

        for (int i = 0; i < groupByColumns.size(); i++) {
            Column c = groupByColumns.get(i);
            c.setOffset(i);
        }

        return groupByColumns;
    }

    protected List<Column> getAggColumns(Request request) throws Exception {

        List<Column> aggColumns = new ArrayList<Column>();
        for (String col :
                (List<String>) request.get(ARG_AGG,
                                           new ArrayList<String>())) {
            Column aggColumn = getDbColumn(col);
            if (aggColumn == null) {
                continue;
            }
            aggColumn = aggColumn.cloneColumn();
            aggColumns.add(aggColumn);
            String aggType = request.getEnum(ARG_AGG_TYPE, "count", "sum",
                                             "min", "max", "avg");
            String name = aggType + "_of_" + aggColumn.getName();
            //              aggColumn.setName(name);
            aggColumn.setLabel(Utils.makeLabel(name));
            aggColumn.setType(aggColumn.DATATYPE_DOUBLE);
        }
        for (int i = 0; i < 3; i++) {
            Column aggColumn = getDbColumn(request.getString(ARG_AGG + i, ""));
            if (aggColumn == null) {
                continue;
            }
            aggColumn = aggColumn.cloneColumn();
            aggColumns.add(aggColumn);
            String aggType = request.getEnum(ARG_AGG_TYPE + i, "count",
                                             "sum", "min", "max", "avg");
            String name = aggType + "_of_" + aggColumn.getName();
            //              aggColumn.setName(name);
            aggColumn.setLabel(Utils.makeLabel(name));
            aggColumn.setType(aggColumn.DATATYPE_DOUBLE);

        }

        return aggColumns;
    }

    public void makeGroupByTable(Request request, Entry entry,
                                 List<Object[]> valueList, Appendable sb,
                                 boolean showHeaderLinks)
            throws Exception {

        List<Column>  groupByColumns    = getGroupByColumns(request, false);
        int           numGroupByColumns = groupByColumns.size();
        StringBuilder hb                = new StringBuilder();
        if (valueList.size() > 0) {
            HU.open(hb, "table", "class", "dbtable", "border", "1",
                           "cellspacing", "0", "cellpadding", "0");
        }
        boolean       addPercent = request.get(ARG_AGG_PERCENT, false);
        double[]      sum        = null;
        StringBuilder cb         = new StringBuilder();
        for (int cnt = 0; cnt < valueList.size(); cnt++) {
            Object[] values = valueList.get(cnt);
            if (sum == null) {
                sum = new double[values.length];
                for (int i = 0; i < sum.length; i++) {
                    sum[i] = 0;
                }
            }
            if (cnt != 0) {
                int col = 0;
                for (Object obj : values) {
                    if (obj instanceof Double) {
                        double d = (Double) obj;
                        if ( !Double.isNaN(d)) {
                            sum[col] += d;
                        }
                    } else if (obj instanceof Integer) {
                        double d = (Integer) obj;
                        if ( !Double.isNaN(d)) {
                            sum[col] += d;
                        }
                    }
                    col++;
                }
            }
        }

        for (int cnt = 0; cnt < valueList.size(); cnt++) {
            Object[] values = valueList.get(cnt);
            if (cnt == 0) {
                HU.open(hb, "tr", "valign", "top", "class", "dbrow");
                for (int cnt2 = 0; cnt2 < values.length; cnt2++) {
                    Object obj = values[cnt2];
                    makeTableHeader(hb, "" + obj);
                    if (addPercent && (cnt2 >= numGroupByColumns  /*&& (cnt2 ==values.length-1)*/
                            )) {
                        makeTableHeader(hb, "Percent");
                    }
                }
                HU.close(hb, "tr");
            } else {
                HU.open(cb, "tr", "valign", "top", "class", "dbrow");
                int col = 0;
                for (int i = 0; i < values.length; i++) {
                    Object obj           = values[i];
                    Column groupByColumn = (i < groupByColumns.size())
                                           ? groupByColumns.get(i)
                                           : null;
                    //              String unit = groupByColumn!=null?groupByColumn.getUnit():null;
                    //              String prefix = unit!=null&&unit.equals("$")?"$":"";
                    //              System.err.println(groupByColumn + " " + unit +" " + prefix);
                    if (obj instanceof Double) {
                        double d = (Double) obj;
                        HU.td(cb, format((Double) obj, false),
                                     "align=right");
                        if (addPercent) {
                            if (sum[col] != 0) {
                                HU.td(cb,
                                             pfmt.format(100 * d / sum[col])
                                             + "%", "align=right");
                            } else {
                                HU.td(cb, "NA");
                            }
                        }
                    } else if (obj instanceof Integer) {
                        int d = (Integer) obj;
                        HU.td(cb, obj.toString(), "align=right");
                        if (addPercent) {
                            if (sum[col] != 0) {
                                HU.td(cb,
                                             pfmt.format(100 * d / sum[col])
                                             + "%", "align=right");
                            } else {
                                HU.td(cb, "NA");
                            }
                        }
                    } else {
                        HU.open(cb, "td");
                        HU.open(
                            cb, "div",
                            HU.cssClass("db-group-table-cell"));
                        String s = "" + obj;
                        if (s.length() == 0) {
                            s = "&lt;blank&gt;";
                        } else if (groupByColumn != null) {
                            String value     = s;
                            String searchArg = groupByColumn.getSearchArg();
                            if ( !groupByColumn.isEnumeration()) {
                                //                                value = "\"" + value + "\"";
                            } else {
                                value = groupByColumn.getEnumValue(value);
                            }
                            String url =
                                HU
                                    .url(request
                                        .makeUrl(getRepository()
                                            .URL_ENTRY_SHOW), new String[] {
                                ARG_ENTRYID, entry.getId(), ARG_DB_SEARCH,
                                "true", searchArg, value, ARG_DB_SORTDIR1,
                                dbInfo.getDfltSortAsc()
                                ? ORDER_ASC
                                : ORDER_DESC
                            });

                            if (dbInfo.getDfltSortColumn() != null) {
                                url += "&" + ARG_DB_SORTBY1 + "="
                                       + dbInfo.getDfltSortColumn().getName();
                            }
                            s = HU.href(
                                url, s,
                                HU.attrs("title", "Click to search",HU.ATTR_REL,"nofollow")
                                + HU.cssClass("ramadda-db-link"));
                        }
                        cb.append(s);
                        HU.close(cb, "div");
                        HU.close(cb, "td");
                    }
                    col++;
                }
                cb.append("</tr>");
            }
        }

        if (valueList.size() > 0) {
            hb.append(cb);
            HU.open(hb, "tr", "valign", "top", "class", "dbrow");
            for (int i = 0; i < sum.length; i++) {
                if (i < numGroupByColumns) {
                    HU.td(hb, "Total", "style=\"background:#eee;\"");
                } else {
                    if (Double.isNaN(sum[i])) {
                        HU.td(hb, "NA", "style=\"background:#eee;\"");
                    } else {
                        HU.td(
                            hb, format(sum[i], false),
                            "align=right  style=\"background:#eee;\"");
                    }
                    if ((i >= numGroupByColumns) && addPercent) {
                        HU.td(hb, "", "style=\"background:#eee;\"");
                    }
                }
            }
            //      if (addPercent) {
            //          HU.td(hb, "&nbsp;",  "style=\"background:#eee;\"");
            //      }
            HU.close(hb, "tr");
            HU.close(hb, "table");
        } else {
            hb.append(getPageHandler().showDialogNote(msg("Nothing found")));
        }
        sb.append(hb.toString());
    }

    protected String format(double v, boolean round) throws Exception {
        if (Math.abs(v) > 1000) {
            round = true;
        }
        if ( !round) {
            return Utils.formatComma(v);
        }
        DecimalFormat fmt = (round
                             ? ifmt
                             : dfmt);

        return fmt.format(v);
    }

    public String formatTableValue(Request request, Entry entry,
                                   Appendable sb, Column column,
                                   Object[] values, SimpleDateFormat sdf,
				   SimpleDateFormat dateTimeSdf,
                                   boolean addLink)
            throws Exception {
        StringBuilder htmlSB = new StringBuilder();
	//Don't use the sdf if it is datetime
        column.formatValue(request, entry, htmlSB, Column.OUTPUT_HTML,
                           values, column.isType(column.DATATYPE_DATETIME)?dateTimeSdf:sdf, false);
        String html  = htmlSB.toString();
        String value = null;
        if (column.getCanSearch() && column.isString()) {
            value = (String) values[column.getOffset()];
            if (value == null) {
                value = "";
            }
            String searchArg = column.getSearchArg();
            //Only do the search link if its short text
            if (value.length() < 50) {
                if ( !column.isEnumeration()) {
                    //                    value = "\"" + value + "\"";
                }
                String url =
                    HU.url(
                        request.makeUrl(getRepository().URL_ENTRY_SHOW),
                        new String[] {
                    ARG_ENTRYID, entry.getId(), ARG_DB_SEARCH, "true",
                    searchArg, value, ARG_DB_SORTDIR1, dbInfo.getDfltSortAsc()
                            ? ORDER_ASC
                            : ORDER_DESC, ARG_DB_VIEW,
                    request.getString(ARG_DB_VIEW, VIEW_TABLE)
                });
                if (dbInfo.getDfltSortColumn() != null) {
                    url += "&" + ARG_DB_SORTBY1 + "="
                           + dbInfo.getDfltSortColumn().getName();
                }

                if (addLink) {
                    html = HU.href(
                        url, html,
                        HU.attrs("title", "Click to search",HU.ATTR_REL,"nofollow")
                        + HU.cssClass("ramadda-db-link"));
                }
            }
        }

        sb.append(html);

        if (value == null) {
            Object tmp = values[column.getOffset()];
            value = (tmp == null)
                    ? ""
                    : tmp.toString();
        }

        return value;
    }

    public String getIconFor(Request request,Entry entry, Hashtable entryProps,
                             Object[] values) {
        for (Column column : getDbInfo().getEnumColumns()) {
            String value    = column.uncheckedGetString(request,values);
            String attrIcon = getIconFor(request,entry, entryProps, column, value);
            if (attrIcon != null) {
                return attrIcon;
            }
        }

        return null;
    }

    public String getIconFor(Request request,Entry entry, Hashtable entryProps,
                             Column column, String value) {
        return getAttributeFor(entry, entryProps, column, value,
                               PROP_CAT_ICON);
    }

    private String getColorFor(Entry entry, Hashtable entryProps,
                               Column column, String value) {
        return getAttributeFor(entry, entryProps, column, value,
                               PROP_CAT_COLOR);
    }

    private String getAttributeFor(Entry entry, Hashtable entryProps,
                                   Column column, String value, String type) {
        if ( !column.isEnumeration() || (value == null)) {
            return null;
        }
        String iconID = type + "." + column.getName();
        Hashtable<String, String> map = (Hashtable<String,
                                            String>) entryProps.get(iconID);
        if (map != null) {
            return map.get(value);
        }

        return null;
    }

    protected String getEditUrl(Request request, Entry entry, String dbid) {
        return HU.url(request.makeUrl(getRepository().URL_ENTRY_SHOW),
                             new String[] {
            ARG_ENTRYID, entry.getId(), ARG_DBID, dbid, ARG_DB_EDITFORM,
            "true"
        });
    }

    protected String getViewUrl(Request request, Entry entry, String dbid) {
        return HU.url(request.makeUrl(getRepository().URL_ENTRY_SHOW),
                             new String[] {
            ARG_ENTRYID, entry.getId(), ARG_DBID, dbid, ARG_DB_ENTRY, "true"
        });
    }

    private static class Location {
	boolean bbox             = true;
	boolean hasLocation = true;
	double latitude;
	double longitude;
	double north, west, south, east;

	public void init() {
	    latitude = longitude = north = west = east=south   = Double.NaN;
	}
	public boolean hasLocation() {
	    return hasPoint()||hasBounds();
	}
	public boolean hasPoint() {
	    return !Double.isNaN(latitude) && !Double.isNaN(longitude);
	}
	public boolean hasBounds() {
	    return !Double.isNaN(north) && !Double.isNaN(west)&& !Double.isNaN(south)
		&& !Double.isNaN(east);
	}		
    }

    public Result handleListMap(final Request request, Entry entry,
                                final List<Object[]> valueList, boolean fromSearch)
            throws Exception {

        final boolean       forPrint   = request.get(ARG_FOR_PRINT, false);
	final boolean simpleMap = request.get(ARG_DB_SIMPLEMAP, false);
        boolean       doTile = request.get(ARG_TILE_ENTRIES,false);
        DbInfo        dbInfo     = getDbInfo();
        Hashtable     entryProps = getProperties(entry);
        boolean canEdit = getAccessManager().canDoEdit(request, entry);
        final StringBuilder sb         = new StringBuilder();
        sb.append(
            "<meta id='request-method' name='request-method' content='POST'></meta>");

        String links = getHref(request, entry, VIEW_KML,
                               msg("Google Earth KML"));
        links = "";
        String formId = null;
	StringBuilder header = new StringBuilder();

        Column  theColumn        = null;
        Column  searchColumn     = null;
	final Location location = new Location();

        String  searchFrom       = request.getString(ARG_SEARCH_FROM, null);
        String  searchColumnName = null;
        String  sourceName       = null;
        String  sourceColumn     = null;
        if (searchFrom != null) {
            List<String> toks = Utils.split(searchFrom, ";");
            sourceName       = toks.get(0);
            sourceColumn     = toks.get(1);
            searchColumnName = toks.get(3);
        }

        if (sourceName != null) {
            String href = HU.href(
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
                                                  "Select all for "
                                                  + sourceName + ":"
                                                  + sourceColumn));
            String clear = HU.href("#",
                                   "Clear all " + sourceName + ":"
                                   + sourceColumn, HU.attr("onclick",
                                       "return DB.doDbClearAll(event,'"
                                       + searchFrom
                                       + "')") + HU.attr("class",
                                           "ramadda-clickable"));

            sb.append(HU.space(2) + href + HU.space(2) + clear);
	}

        for (Column column : tableHandler.getColumns()) {
            if ((searchColumnName != null)
                    && searchColumnName.equals(column.getName())) {
                searchColumn = column;
            }
            if (column.getType().equals(Column.DATATYPE_LATLONBBOX)) {
                theColumn = column;
                break;
            }
            if (column.getType().equals(Column.DATATYPE_LATLON)) {
                theColumn = column;
                location.bbox      = false;
                break;
            }
        }

	Utils.BiConsumer<Column,Object[]>  getLocation =  new Utils.BiConsumer<Column,Object[]>() {
		public void accept(Column theColumn, Object[] values){
		    location.init();
		    if (theColumn == null) {
			location.latitude  = dbInfo.getLatColumn().uncheckedGetDouble(request,values);
			location.longitude  = dbInfo.getLonColumn().uncheckedGetDouble(request,values);
		    } else {
			if ( !location.bbox) {
			    double[] ll = theColumn.getLatLon(request,values);
			    if(!Double.isNaN(ll[0]) && !Double.isNaN(ll[1])) {
				location.latitude = ll[0];
				location.longitude = ll[1];
			    }
			} else {
			    double[] ll = theColumn.getLatLonBbox(request,values);
			    if(!Double.isNaN(ll[0]) && !Double.isNaN(ll[1])
			       && !Double.isNaN(ll[2])
			       && !Double.isNaN(ll[3])) {
				location.north = ll[0];
				location.west  = ll[1];
				location.south = ll[2];
				location.east  = ll[3];
			    }
			}
		    }
		}

	    };

        if ((theColumn == null) && (dbInfo.getLatColumn() == null)
                && (dbInfo.getLonColumn() == null)) {
            throw new IllegalStateException("No geodata data found");
        }

        String width  = "";
        String height = request.getString("mapheight","500");
	if(simpleMap) {
            width  = "90%";
	} else if (forPrint) {
            width  = "300";
            height = "300";
        }
        Hashtable props = Utils.makeHashtable("style", "");
        if (request.defined("mapLayer")) {
            props.put("defaultMapLayer", request.getString("mapLayer", ""));
        }
        if (request.defined("mapBounds")) {
            props.put("initialBounds", request.getString("mapBounds", ""));
        }
        if (request.defined("zoomLevel")) {
            props.put("zoomLevel", request.getString("zoomLevel", ""));
        }
        if (request.defined("mapCenter")) {
            props.put("mapCenter", request.getString("mapCenter", ""));
        }

	props.put("singlePointZoom", "14");
        boolean makeRectangles = valueList.size() <= 20;
        String  leftWidth      = "300px";
        String  mapAttrs       = "";
        if (forPrint) {
            mapAttrs  = " width=50% ";
            leftWidth = "50%";
        }

	//        String           icon          = getMapIcon(request, entry);
        String           icon          = getDbIconUrl("/db/icons/blue-dot.png");
        SimpleDateFormat sdf           = getDateFormat(request, entry);
        SimpleDateFormat dateTimeSdf           = getDateTimeFormat(request, entry);	
        Column           polygonColumn = getDbInfo().getPolygonColumn();
        //      int rowCnt = 0;
        int entriesPerPage = request.get(ARG_ENTRIES_PER_PAGE, 30);
	boolean numberEntries = request.get(ARG_NUMBER_ENTRIES,false);
        int lineCnt        = 0;
        List<String> extraCols =
            Utils.split(request.getString(ARG_EXTRA_COLUMNS, ""), "\n", true,
                        true);
        List<List> lists;
	if (forPrint) {
	    if(doTile) {
		List<GeoUtils.TiledObject> tiledObjects = new ArrayList<GeoUtils.TiledObject>();
		for (Object[] value : valueList) {
		    getLocation.accept(theColumn, value);
		    if(location.hasPoint()) {
			tiledObjects.add(new GeoUtils.TiledObject(value,location.latitude,location.longitude));
		    } else 	if(location.hasBounds()) {
			//TODO
			//tiledObjects.add(new Utils.TiledObject(value,location.latitude,location.longitude);
		    }
		}
		List<GeoUtils.Tile> tiles = GeoUtils.tile(tiledObjects,5);
		lists = new ArrayList<List>();
		for(GeoUtils.Tile tile: tiles)  {
		    List l = new ArrayList();
		    for(GeoUtils.TiledObject obj: tile.getObjects()) {
			l.add(obj.getObject());
		    }
		    lists.addAll((List<List>) Utils.splitList(l, entriesPerPage));
		}
	    } else {
		lists = (List<List>) Utils.splitList(valueList, entriesPerPage);
	    }
        } else {
            lists = new ArrayList<List>();
            lists.add(valueList);
        }

        int cnt=0;
	int listCnt = 0;
	final int[]pageCnt = {0};
	final int[]pages = {lists.size()};

	Date now = new Date();
	String dttm = getDateFormat(request, entry).format(now);

	Utils.UniConsumer<Integer>  footer =  (page)->{
	    sb.append(HU.center("Page #" + (page)+"/" + (pages[0])));
	};

	Utils.UniConsumer<Integer>  hdr =  (page)->{
	    String name = request.getString(ARG_DB_SEARCHNAME, (String) null);
	    String subTitle = request.getString(ARG_DB_SUBTITLE,null);
	    String title = HU.div(name,HU.attr("style","text-align:center;font-weight:bold;font-size:150%;"));
	    if(pageCnt[0]>0) {
		footer.accept(page);
		getPageHandler().sectionClose(request, sb);
                sb.append("<div class=pagebreak></div>");
	    }
	    String h = "";
	    if(simpleMap) {
		h+=HU.center(title);
		if(stringDefined(subTitle)) {
		    h+=HU.div(subTitle, HU.style("text-align:center"));
		}
	    } else  if(!stringDefined(subTitle)) {
		h+="<table width=100%><tr valign=bottom>";
		h+=HU.col(!simpleMap && pageCnt[0]==0?"Total: " + valueList.size():"", HU.attr("width","15%"));
		h+=HU.col(title, HU.attr("width","70%"));
		h+=HU.col(dttm,HU.attrs("align","right","width","15%"));
		h+="</tr></table>";
	    } else {
		h+=HU.center(title);
		h+="<table width=100%><tr valign=bottom>";
		h+=HU.col(!simpleMap && pageCnt[0]==0?"Total: " + valueList.size():"", HU.attr("width","15%"));
		h+=HU.col(subTitle, HU.attrs("align","center","width","70%"));
		h+=HU.col(dttm,HU.attrs("align","right","width","15%"));
		h+="</tr></table>";
	    }
	    getPageHandler().sectionOpen(request, sb,null,false);
	    sb.append(h);
	    sb.append("<div style='border-bottom:1px solid #ccc;margin-bottom:8px;'></div>\n");
	    pageCnt[0]++;
	};

        if ( !isEmbedded(request)) {
	    if(forPrint) {
		addStyleSheet(request,sb);
		request.put(ARG_TEMPLATE, "empty");
		hdr.accept(0);
	    } else {
		formId = addViewHeader(request, entry, header, VIEW_MAP,
				       valueList.size(), fromSearch, links);
		sb.append(header);
	    }
        }

	MapProperties mapProperties =   new MapProperties(request.getString(ARG_DB_MAPPROPS,defaultMapProperties));
	for (List listValues : lists) {
	    if(forPrint && listCnt>0) {
		hdr.accept(listCnt);
		//		sb.append(header);
		//		addViewHeader(request, entry, sb, VIEW_MAP,    valueList.size(), fromSearch, links);
	    }
	    listCnt++;

	    String mapDisplayId = "mapDisplay_" + Utils.getGuid();
	    props.put("displayDiv", mapDisplayId);
            MapInfo map = getRepository().getMapManager().createMap(request,
								    entry, width, height, false, props);

            StringBuilder entryList = new StringBuilder();
	    String listId = HU.getUniqueId("list_");
	    //Add the search bounding box if defined
	    for (Column column : tableHandler.getColumns()) {
		if(column.isType(column.DATATYPE_LATLON)) {
		    String    searchArg = column.getSearchArg();
		    if(request.defined(searchArg+"_north") &&
		       request.defined(searchArg+"_west") &&
		       request.defined(searchArg+"_south") &&
		       request.defined(searchArg+"_east")) {
			map.addBox("", "", "",
				   new MapProperties("red", false), 
				   request.get(searchArg+"_north",90.0),
				   request.get(searchArg+"_west",-180.0),
				   request.get(searchArg+"_south",-90.0),
				   request.get(searchArg+"_east",180.0));			       
		    }
		}
	    }

            if ( !forPrint) {
                sb.append(
                    HU.cssBlock(
                        "\n.db-map-list-inner {max-height: " + height
                        + "px; overflow-y: auto; overflow-x:auto; }\n\n"));
            } 
	    entryList.append("\n");
            HU.open(entryList, "div", "class", "db-map-list-outer","id",listId);
            HU.open(entryList, "div", "class", "db-map-list-inner");
	    entryList.append("\n");
            StringBuilder                    theSB  = entryList;
            Hashtable<String, StringBuilder> catMap = null;
            List<String>                     cats   = null;
            if (getDbInfo().getMapCategoryColumn() != null) {
                catMap = new Hashtable<String, StringBuilder>();
                cats   = new ArrayList<String>();
            }

	    boolean useDot = forPrint || listValues.size()>mapDotLimit;
	    int radius = 6;
	    if(simpleMap) {
		useDot = false;
	    }
	    if(listValues.size()>1000) {
		radius = 2;
	    } else  if(listValues.size()>500) {
		radius = 4;
	    } else  if(forPrint) {
		radius=4;
	    }

	    int xcnt=0;
	    for (Object obj : listValues) {
		//		if(xcnt++>3) break;
                Object[] values = (Object[]) obj;
                String   dbid   = (String) values[IDX_DBID];
		getLocation.accept(theColumn, values);
		//		if(!location.hasLocation()) continue;
                if (location.hasBounds()) {
                    map.addBox("", "", "",
                               new MapProperties("red", false), location.north,
                               location.west, location.south, location.east);
                }
                if (getDbInfo().getMapCategoryColumn() != null) {
                    String cat =
                        getDbInfo().getMapCategoryColumn().uncheckedGetString(request,values);
                    if (cat == null) {
                        cat = "";
                    }
                    theSB = catMap.get(cat);
                    if (theSB == null) {
                        theSB = new StringBuilder();
                        catMap.put(cat, theSB);
                        cats.add(cat);
                    }
                }

                theSB.append("\n");
                theSB.append("<div class=\"db-map-list-entry\" data-mapid=\""
                             + dbid + "\">");
                String iconToUse = icon;
                String attrIcon  = getIconFor(request,entry, entryProps, values);
                if (attrIcon != null) {
                    iconToUse = getDbIconUrl(attrIcon);
                    //                theSB.append(HU.img(iconToUse,"", "width=16"));
                }
                String extraLabel = "";
                if (searchColumn != null) {
                    theSB.append("&nbsp;");
                    String value = searchColumn.uncheckedGetString(request,values);
                    String href =
                        HU.href(
                            "#",
                            getRepository().getIconImage(
                                "fas fa-external-link-alt"), HU.attr(
                                "onclick",
                                "return DB.doDbSelect(event,'" + searchFrom
                                + "','" + value + "')") + HU.attr(
                                    "select-value", value) + HU.attr(
                                    "class",
                                    "db-select-link ramadda-clickable") + HU.attr(
                                        "title",
                                        "Select for " + sourceName + ":"
                                        + sourceColumn));
                    theSB.append(href);
                    theSB.append("&nbsp;");
                    extraLabel = href + "<br>";
                }
                String viewUrl = getViewUrl(request, entry, dbid);
                if ( !forPrint) {
                    theSB.append(HU.href(viewUrl,HU.img(iconToUse, "View entry", "width=16")));
                }
                theSB.append(" ");
                String label = getMapLabel(request, entry, values, sdf,
                                           forPrint,numberEntries?"#" +(++cnt)+" ":"");

		if(!forPrint) 
		    theSB.append(map.getHiliteHref(dbid, label));
		else
		    theSB.append(label);
                theSB.append("</div>");
		//limit the size to 20 columns
                String mapInfo;
		if(extraLabel.length()>0) {
		    mapInfo = extraLabel+getHtml(request, entry, dbid, getDbColumns(), values, sdf,dateTimeSdf,true,40);
		    mapInfo = mapInfo.replace("\r", " ");
		    mapInfo = mapInfo.replace("\n", " ");
		    mapInfo = mapInfo.replace("\"", "\\\"");
		} else {
		    mapInfo="url:" + viewUrl + "&result=plain";
		}
                String mapLabel = label;
                if (forPrint) {
                    mapLabel = "";
		    mapInfo="";
                }

                if (location.hasPoint()) {
		    if(useDot) {
			map.addCircle(dbid,  location.latitude,  location.longitude, radius,
				      0,"#fff",
				      "blue",mapInfo);
		    } else {
			if(mapPolygonsShow && polygonColumn!=null) {
			    map.addPolygon(dbid,
					   polygonColumn.uncheckedGetString(request,values),mapInfo,null,mapProperties);
			}
			if(mapMarkersShow)
			    map.addMarker(dbid, location.latitude, location.longitude, null,
					  useDot ? "dot": iconToUse, mapLabel, mapInfo, null);
		    }
                } else if(location.hasBounds()) {
                    if ( !makeRectangles) {
                        map.addMarker(dbid, new LatLonPointImpl(location.south, location.east),
                                      iconToUse, mapLabel, mapInfo);
                    } else {
                        map.addMarker(dbid, new LatLonPointImpl(location.south
                                + (location.north - location.south) / 2, location.west
                                    + (location.east - location.west)
                                      / 2), mapLabel, icon, mapInfo);
                    }
                }
	    }
            if (catMap != null) {
                boolean open = true;
                for (String cat : cats) {
                    StringBuilder catSB = catMap.get(cat);
                    String content = HU.insetLeft(catSB.toString(),
                                         20);
                    entryList.append(HU.makeShowHideBlock(cat,
                            content, open));
                    open = false;
                }
            }
	    entryList.append("\n");
            entryList.append(HU.close(HU.TAG_DIV));
            entryList.append(HU.close(HU.TAG_DIV));
	    entryList.append("\n");
	    map.center();
            if (simpleMap) {
                sb.append(HU.center(map.getHtml()));
	    } else {
                HU.open(sb, "table", "class",
                               " db-map-table " + (forPrint
                        ? " db-map-table-print "
                        : ""), "cellpadding", "0", "border", "0", "width",
                               "100%");
		sb.append("\n");
                HU.open(sb, "tr", "valign", "top");
                HU.col(sb, entryList.toString(),
			       HU.clazz("db-map-column")
			       + HU.attr("width",
						leftWidth));
		HU.col(sb,map.getHtml(),  HU.clazz("db-map-column") + mapAttrs+HU.attr("align","center"));
                if ( !forPrint) {
                    String searchLink = "";
                    if (formId != null) {
                        String mapVar = map.getVariableName();
                        searchLink =
                            HU.center(HU.href("javascript:DB.applyMapSearch("
                                + mapVar + ",'" + formId
                                + "')", "Search in this area"));
                    }
                    String rightDiv =
                        searchLink + "<div id=\"" + mapDisplayId
                        + "\" style=\"width:250px;max-width:250px;overflow-x:hidden;max-height:"
                        + height + "px; overflow-y:auto;\"></div>";
                    sb.append(HU.col(rightDiv, HU.clazz("db-map-column") +HU.attr("width","250")));
                }
                HU.close(sb, "tr", "table");
	    }

	    String js =	map.getVariableName()+  ".highlightMarkers( '#" + listId+"  .db-map-list-entry');";
	    sb.append(HU.script(JQuery.ready(js)));
	}

	if(forPrint && !simpleMap) 
	    footer.accept(listCnt);

        if ( !isEmbedded(request) && !forPrint) {
            addViewFooter(request, entry, sb);
        }

        return new Result(getTitle(request, entry), sb);

    }

    public String getEntryIcon(Request request, Entry entry) {
        return getRepository().getUrlBase() + tableIcon;
    }

    public String getMapIcon(Request request, Entry entry) {
        return getEntryIcon(request, entry);
    }

    public String getDefaultDateFormatString() {
        return "yyyy-MM-dd";
    }

    public String getDefaultDateTimeFormatString() {
        return "yyyy-MM-dd HH:mm z";
    }    

    public SimpleDateFormat getDateFormat(Request request, Entry entry) {
        return getDateFormat(request,entry, getDefaultDateFormatString());
    }

    public SimpleDateFormat getDateTimeFormat(Request request, Entry entry) {
        return getDateFormat(request,entry, getDefaultDateTimeFormatString());
    }    

    public SimpleDateFormat getDateFormat(Request request, Entry entry, String format) {
        String           timezone = getEntryUtil().getTimezone(request,entry);
	return getRepository().getDateHandler().getSDF(format, timezone,false);

    }

    public int getDefaultMax(Request request, Entry entry, String tag,
                             Hashtable props) {
        return 1000;
    }

    private void addProp(String key, String value, Hashtable props,
                         List<String> displayProps) {
        if ((props.get(key) == null) && !displayProps.contains(key)) {
            displayProps.add(key);
            displayProps.add(value);
        }
    }

    @Override
    public String getUrlForWiki(Request request, Entry entry, String tag,
                                Hashtable props, List<String> displayProps) {

        if (tag.equals(WikiConstants.WIKI_TAG_CHART)
                || tag.startsWith("display_")
                || tag.equals(WikiConstants.WIKI_TAG_DISPLAY)) {
            try {
                if (props.get("max") == null) {
                    props.put("max",
                              "" + getDefaultMax(request, entry, tag, props));
                }
                String url =
                    ((PointOutputHandler) getRecordOutputHandler())
                        .getJsonUrl(request, entry, props, displayProps);
                url += "&"
                       + request.getUrlArgs(
                           (HashSet<String>) Utils.makeHashSet(
                               ARG_ENTRYID, ARG_DB_SEARCH,
                               ARG_DB_DOSAVESEARCH), null, null);
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }
        }

	String requestFields = Utils.getProperty(props,"requestFields",null);
	if(!stringDefined(requestFields) && !Utils.getProperty(props,"addRequestFields",false)) {
	    return super.getUrlForWiki(request, entry, tag, props, displayProps);
	}

        String       all                = "";
        String       prefix             = "request.";
        DbInfo       dbInfo             = getDbInfo();
        List<Column> columns            = dbInfo.getColumnsToUse();

        String       groupBy            = null;
        String       aggBy              = null;
        Column       firstColumn        = null;
        boolean      includeNumericAggs = true;
        for (Column column : columns) {
            if (column.getCanSearch()) {
                if (firstColumn == null) {
                    firstColumn = column;
                }
                groupBy = Utils.appendList(groupBy,
                                           column.getName() + ":"
                                           + column.getLabel());
                if (column.isNumeric()) {
                    aggBy = Utils.appendList(aggBy,
                                             column.getName() + ":"
                                             + column.getLabel());
                }
            }
        }

        if ((aggBy == null) && (firstColumn != null)) {
            includeNumericAggs = false;
            aggBy = Utils.appendList(aggBy,
                                     firstColumn.getName() + ":"
                                     + firstColumn.getLabel());
        }

        if ( !Misc.equals("false", props.get("showGroupBy"))
                && (groupBy != null)) {
            groupBy = ":None," + groupBy;
            all     = Utils.appendList(all, "group_by");
            all     = Utils.appendList(all, "group_agg");
            all     = Utils.appendList(all, "group_agg_type");
            addProp(prefix + "group_by.values", JsonUtil.quote(groupBy), props,
                    displayProps);
            addProp(prefix + "group_agg.values", JsonUtil.quote(aggBy), props,
                    displayProps);
            addProp(prefix + "group_agg.label", JsonUtil.quote("Aggregate"),
                    props, displayProps);
            addProp(prefix + "group_agg.multiple", "true", props,
                    displayProps);
            addProp(prefix + "group_agg_type.values",
                    JsonUtil.quote( !includeNumericAggs
                                ? "count:Count"
                                : "sum:Sum,max:Max,count:Count,min:Min,avg:Average"), props,
                                displayProps);
            addProp(prefix + "group_agg_type.label", JsonUtil.quote("Type"),
                    props, displayProps);
        }

        Hashtable recordProps = null;
        try {
            recordProps = getRecordProperties(entry);
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
        //      System.err.println("getWiki:" + recordProps);

        boolean includeAll = true;
        for (Column column : columns) {
            if ((column.getCanSearch()
                    && (recordProps.get(column.getName() + ".display")
                        != null)) || (column.getProperty("isDisplayProperty")
                                      != null)) {
                includeAll = false;

                break;
            }
        }

        for (Column column : columns) {
            if ( !column.getCanSearch()) {
                continue;
            }
            if ( !includeAll) {
                if ( !Misc.equals(
                        recordProps.get(column.getName() + ".display"),
                        "true") && !Misc.equals(
                            column.getProperty("isDisplayProperty"),
                            "true")) {
                    continue;
                }
            }
            String type = column.isEnumeration()
                          ? "enumeration"
                          : column.isNumeric()
                            ? "numeric"
                            : column.isDate()
                              ? "date"
                              : "string";

            all = Utils.appendList(all, column.getName());
            addProp(prefix + column.getName() + ".label",
                    JsonUtil.quote(column.getLabel()), props, displayProps);
            addProp(prefix + column.getName() + ".urlarg",
                    JsonUtil.quote(column.getSearchArg()), props, displayProps);
            addProp(prefix + column.getName() + ".type", JsonUtil.quote(type),
                    props, displayProps);
            if (column.isEnumeration()) {
                StringBuilder enums = new StringBuilder("_all_:All");
                List<HtmlUtils.Selector> tfos = getEnumValues(request, entry,
                                                column);
                for (HtmlUtils.Selector tfo : tfos) {
                    if (enums != null) {
                        enums.append(",");
                    } else {
			//                        enums = "";
                    }
                    enums.append(tfo.getId().replace(",","\\,"));
		    if(!tfo.getId().equals(tfo.getLabel())) {
			enums.append(":");
			enums.append(tfo.getLabel().replace(",","\\"));
		    }
                }
                if (enums != null) {
                    addProp(prefix + column.getName() + ".values",
                            JsonUtil.quote(enums.toString()), props, displayProps);
                    addProp(prefix + column.getName() + ".default",
                            JsonUtil.quote("_all_"), props, displayProps);
                }

            }
        }

        addProp("requestFields", JsonUtil.quote(all), props, displayProps);

        return super.getUrlForWiki(request, entry, tag, props, displayProps);

    }

    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props)
            throws Exception {
        if ( !tag.equals("db")) {
            return super.getWikiInclude(wikiUtil, request, originalEntry,
                                        entry, tag, props);
        }
        //        {{db entry="e6a54dbb-c310-47ae-8f49-597f32aa9f4d" args="search.db_bolder_rental_housing.propaddr1:illini,Boxes:Boxes,db.view:map,searchname:Name" }}
        try {
            String args = (String) props.get("args");
            if (args == null) {
                args = "";
            }
            Hashtable newArgs = new Hashtable();
            newArgs.put(ARG_ENTRYID, entry.getId());
            newArgs.put(ARG_DB_SEARCH, "true");
            Request newRequest = request.cloneMe(request.getRepository());
            newRequest.clearUrlArgs();

            for (String pair : StringUtil.split(args, ",", true, true)) {
                List<String> toks = StringUtil.splitUpTo(pair, ":", 2);
                //false-> not singular
                String arg = toks.get(0);
                newRequest.put(arg, toks.get(1), false);
            }
            newRequest.putAll(newArgs);
            newRequest.put(ARG_DB_SHOWHEADER, "false");
            newRequest.setEmbedded(true);
            StringBuilder sb = new StringBuilder();
            addStyleSheet(request,sb);
            String zoom = (String) props.get("zoomLevel");
            if (zoom != null) {
                newRequest.put("zoomLevel", zoom);
            }
            String mapCenter = (String) props.get("mapCenter");
            if (mapCenter != null) {
                newRequest.put("mapCenter", mapCenter);
            }

            String layer = (String) props.get("layer");
            if (layer != null) {
                newRequest.put("mapLayer", layer);
            }
            String bounds = (String) props.get("mapBounds");
            if (bounds != null) {
                newRequest.put("mapBounds", bounds);
            }
            if (newRequest.defined(ARG_DB_SEARCHNAME)) {
                HU.sectionHeader(
                    sb, newRequest.getString(ARG_DB_SEARCHNAME, ""));
            }
            if (newRequest.defined(ARG_DB_SEARCHDESC)) {
                sb.append(getWikiManager().wikifyEntry(newRequest, entry,
                        newRequest.getString(ARG_DB_SEARCHDESC, "")));

            }

	    Result result = handleSearch(newRequest, entry);
	    sb.append(result.getStringContent());

            return sb.toString();
        } catch (Exception exc) {
            exc.printStackTrace();

            return getPageHandler().showDialogError("An error occurred:<br>"
                    + exc.toString(), false);
        }
    }

    public String getSearchUrlArgument(Column column) {
        return tableHandler.getTableName() + "." + column.getName();
    }

    protected List<HtmlUtils.Selector> getEnumValues(Request request,
            Entry entry, Column column) {
        try {
            if (column.getType().equals(Column.DATATYPE_ENUMERATION)) {
                List<HtmlUtils.Selector> enums = column.getValues();
                if (enums.size() > 0) {
                    return enums;
                }
            }
            List<HtmlUtils.Selector> enums = tableHandler.getEnumValues(request,
                                             column, entry);

            return enums;
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }

    }

    public String getHref(Request request, Entry entry, String view,
                          String label) {
        return HU.href(getUrl(request, entry, view), label);
    }

    public String getHref(Request request, Entry entry, String view,
                          String label, String suffix) {
        return HU.href(getUrl(request, entry, view, suffix), label);
    }

    public String getUrl(Request request, Entry entry, String view) {
        return getUrl(request, entry, view, "");
    }

    public String getUrl(Request request, Entry entry, String view,
                         String suffix) {
        return HU.url(request.makeUrl(getRepository().URL_ENTRY_SHOW)
                             + suffix, new String[] { ARG_ENTRYID,
                entry.getId(), ARG_DB_VIEW, view });
    }

    private boolean isDataColumn(Column column) {
	if(column.isSynthetic())  {
	    return false;
	}

        if (column.getName().equals(COL_DBID)
                || column.getName().equals(COL_DBUSER)
                || column.getName().equals(COL_DBCREATEDATE)
                || column.getName().equals(COL_DBPROPS)) {
            return false;
        }

        return true;
    }

    public Result handleListCalendar(Request request, Entry entry,
                                     List<Object[]> valueList,
                                     boolean fromSearch)
            throws Exception {
        DbInfo        dbInfo = getDbInfo();
        boolean canEdit      = getAccessManager().canDoEdit(request,
                                   entry);
        StringBuilder sb     = new StringBuilder();
        String        links  = getHref(request, entry, VIEW_ICAL, "ICAL");
        addViewHeader(request, entry, sb, VIEW_CALENDAR, valueList.size(),
                      fromSearch, links);

        CalendarOutputHandler calendarOutputHandler =
            (CalendarOutputHandler) getRepository().getOutputHandler(
                CalendarOutputHandler.OUTPUT_CALENDAR);

        List<CalendarOutputHandler.CalendarEntry> calEntries =
            new ArrayList<CalendarOutputHandler.CalendarEntry>();

        if (dbInfo.getDateColumn() == null) {
            throw new IllegalStateException("No date data found");
        }
        SimpleDateFormat sdf = getDateFormat(request,entry);
        for (Object[] values : valueList) {
            String dbid = (String) values[IDX_DBID];
            Date   date = (Date) values[dbInfo.getDateColumn().getOffset()];
            String url  = getViewUrl(request, entry, dbid);
            String label = getCalendarLabel(request, entry, values,
                                            sdf).trim();
            StringBuilder html = new StringBuilder();
            if (label.length() == 0) {
                label = "NA";
            }
            String href = HU.href(url, label);

            /*
              if (canEdit) {
              String editUrl = getEditUrl(request, entry, dbid);
              href = HU.href(
              editUrl,
              HU.img(
              getRepository().getUrlBase()
              + "/db/database_edit.png", msg("Edit entry"))) + " "
              + href;
              }
            */
            //            html.append(href);
            String rowId = "row_" + values[IDX_DBID];
            String event = getEventJS(request, entry, values[IDX_DBID],
                                      rowId, rowId);
            href = HU.div(href,
                                 HU.cssClass("dbcategoryrow")
                                 + HU.id(rowId) + event);
            //            getHtml(request, html, entry, values);
            String block = HU.makeShowHideBlock(href, html.toString(),
                               false);
            calEntries.add(new CalendarOutputHandler.CalendarEntry(date,
                    href, href));
        }

        calendarOutputHandler.outputCalendar(request, calEntries, sb, false);

        addViewFooter(request, entry, sb);

        return new Result(getTitle(request, entry), sb);
    }

    public List<Object[]> readValues(Request request, Entry entry,
                                     Clause clause, ValueIterator iterator)
            throws Exception {
        if (request.exists(ARG_DBIDS)) {
            List<Object[]> result = new ArrayList<Object[]>();
            String         ids    = request.getString(ARG_DBIDS, "");
            request.remove(ARG_DBIDS);
            for (String id : StringUtil.split(ids, ",", true, true)) {
                result.addAll(readValues(request, entry,
                                         makeClause(entry, id), iterator));
            }
            request.put(ARG_DBIDS, ids);

            return result;
        }

        List<String> colNames = tableHandler.getColumnNames();

        if ((dbInfo.getDfltSortColumn() != null)
                && !request.defined(ARG_DB_SORTBY1)) {
            request.put(ARG_DB_SORTBY1, dbInfo.getDfltSortColumn().getName());
            request.put(ARG_DB_SORTDIR1, dbInfo.getDfltSortAsc()
                                         ? ORDER_ASC
                                         : ORDER_DESC);
        }

        String  order     = "";
        String  extra     = "";

        boolean doGroupBy = isGroupBy(request);
        if ( !doGroupBy) {
            for (int i = 1; i <= numOrders; i++) {
                if (request.defined(ARG_DB_SORTBY + i)) {
                    String by     = request.getString(ARG_DB_SORTBY + i, "");
                    Column column = getDbColumn(by);
                    if (column != null) {
                        by = column.getSortByColumn();
                        boolean asc = request.getString(ARG_DB_SORTDIR + i,
                                          ORDER_ASC).equals(ORDER_ASC);
                        if (order.length() > 0) {
                            order += ",";
                        }
                        order += SqlUtil.sanitize(by) + " " + (asc
                                ? SqlUtil.ORDER_ASC
                                : SqlUtil.ORDER_DESC);
                    }
                }
            }
        }
        if (order.length() > 0) {
            order = "ORDER BY " + order;
        }
        int max  = getMax(request);
        int skip = request.get(ARG_SKIP, 0);
        extra = order;
        extra += getDatabaseManager().getLimitString(skip, max);
	if(debugQuery) {
	    System.err.println("DbTypeHandler.readValues:" + clause +" extra:" + extra);
	}
        return readValues(request, clause, extra, max, null, iterator);
    }

    protected int getMax(Request request) {
        return request.get(ARG_MAX, DEFAULT_MAX);
    }

    private List<Object[]> readValues(Request request, Clause clause,
                                      String limitString, int max)
            throws Exception {
        return readValues(request, clause, limitString, max, null, null);
    }

    private List<Object[]> readValues(Request request, Clause clause,
                                      String limitString, int max,
                                      PrintWriter pw, ValueIterator iterator)
            throws Exception {

	boolean myDebug = isType("campaign_donors");
	myDebug = false;

        //For now don't check for isPostgres which is used below for making unique requests
        boolean isPostgres = getDatabaseManager().isDatabasePostgres();
        isPostgres = false;
        String         extra     = "";
        List<Object[]> result    = new ArrayList<Object[]>();
        boolean        doGroupBy = isGroupBy(request);
        List<String>   colNames;
        List<Column>   selectedColumns = null;
        List<Column>   uniqueCols      = null;
        List<Column>   groupByColumns  = null;
        List<String>   aggColumns      = null;
        List<String>   aggLabels       = null;
        List<String>   aggSelectors    = null;
        boolean forMap = request.getString(ARG_DB_VIEW, "").equals(VIEW_MAP);
        boolean forTable = request.getString(ARG_DB_VIEW,
                                             VIEW_TABLE).equals(VIEW_TABLE);

        if (iterator != null) {
            iterator.initialize(request, doGroupBy);
        }

        if (doGroupBy) {
            colNames       = new ArrayList<String>();
            groupByColumns = new ArrayList<Column>();
            String       orderBy = null;
            List<String> cols    = new ArrayList<String>();
            List<String> labels  = new ArrayList<String>();
            List<String> args = (List<String>) (request.get(ARG_GROUPBY,
                                    new ArrayList()));
            for (int i = 0; i < args.size(); i++) {
                String  col           = args.get(i);
                Column  groupByColumn = getDbColumn(col);
                boolean doYear        = false;
                if ((groupByColumn == null) && col.startsWith("year(")) {
                    String tmp = col.substring(5);
                    tmp           = tmp.substring(0, tmp.length() - 1);
                    groupByColumn = getDbColumn(tmp);
                    doYear        = true;
                    col = getRepository().getDatabaseManager().getExtractYear(
                        tmp);
                    //                    extract (year from col)
                    orderBy = SqlUtil.orderBy(col,
                            request.getEnum(ARG_DB_GROUP_SORTDIR, ORDER_DESC,
                                            ORDER_DESC, ORDER_ASC).equals(ORDER_DESC));
                }

                if (groupByColumn != null) {
                    if (doYear) {
                        cols.add(0, col);
                        groupByColumns.add(0, groupByColumn);
                        colNames.add(0, col);
                        labels.add(0, "Year of " + groupByColumn.getLabel());
                    } else {
                        cols.add(col);
                        groupByColumns.add(groupByColumn);
                        colNames.add(col);
                        labels.add(groupByColumn.getLabel());
                    }
                }
            }

            aggColumns   = new ArrayList<String>();
            aggLabels    = new ArrayList<String>();
            aggSelectors = new ArrayList<String>();
            for (int i = -1; i < 3; i++) {
                List<String> aggColNames =
                    (ArrayList<String>) request.get(ARG_AGG + ((i < 0)
                        ? ""
                        : i), new ArrayList<String>());
                for (String c : aggColNames) {
                    Column aggColumn = getDbColumn(c);
                    if (aggColumn != null) {
                        aggColumns.add(aggColumn.getName());
                        aggLabels.add(aggColumn.getLabel());
                        aggSelectors.add(request.getEnum(ARG_AGG_TYPE
                                + ((i < 0)
                                   ? ""
                                   : i), "count", "sum", "min", "max",
                                         "avg"));
                    }
                }
            }
            if (aggColumns.size() == 0) {
                if (colNames.size() > 0) {
                    aggColumns.add(colNames.get(0));
                }
                if (labels.size() > 0) {
                    aggLabels.add(labels.get(0));
                }
                aggSelectors.add("count");
            }

            for (int i = 0; i < aggColumns.size(); i++) {
                String aggColumn   = aggColumns.get(i);
                String agg         = aggSelectors.get(i);
                String aggSelector = agg + "(" + aggColumn + ") ";
                colNames.add(aggSelector);
                if (orderBy == null) {
                    String sort = request.getString(ARG_DB_GROUP_SORTBY,
                                      aggSelector);
                    if ( !Utils.stringDefined(sort)) {
                        sort = aggSelector;
                    }
                    orderBy = SqlUtil.orderBy(sort,
                            request.getString(ARG_DB_GROUP_SORTDIR,
                                ORDER_DESC).equals(ORDER_DESC));
                }

                String label = agg;
                if (label.equals("avg")) {
                    label = "average";
                }
                label = StringUtil.camelCase(label);
                if ( !label.equals("Count")) {
                    label = label + " of " + aggLabels.get(i);
                }
		label = request.getString("agglabel" + i,label);
                labels.add(label);
            }
	    if(iterator!=null) {
		iterator.setLabels(labels);
	    }
	    result.add(labels.toArray());
            if (cols.size() > 0) {
                extra += SqlUtil.groupBy(StringUtil.join(",", cols));
            }
            if (orderBy != null) {
                extra += orderBy;
            }
        } else {
            //If for map then make sure the latlon column is one of the ones selected
            selectedColumns = getSelectedColumns(request, true);
            if (forMap) {
                Column theColumn = null;
                for (Column column : tableHandler.getColumns()) {
                    if (column.getType().equals(Column.DATATYPE_LATLONBBOX)) {
                        theColumn = column;
                        break;
                    }
                    if (column.getType().equals(Column.DATATYPE_LATLON)) {
                        theColumn = column;
                        break;
                    }
                }
                if (theColumn == null) {
                    theColumn = dbInfo.getLatLonColumn();
                }
                if (theColumn != null) {
                    if ( !selectedColumns.contains(theColumn)) {
                        selectedColumns.add(theColumn);
                    }
                }
            }

            colNames = Column.getNames(selectedColumns);
            List<String> uniqueNames = new ArrayList<String>();
            for (Column column : selectedColumns) {
                if (request.get(ARG_DB_UNIQUE + "_" + column.getName(),
                                false)) {
                    if (uniqueCols == null) {
                        uniqueCols = new ArrayList<Column>();
                    }
                    uniqueCols.add(column);
                    uniqueNames.add(column.getName());
                }
            }
	    /*
            if (isPostgres && (uniqueCols != null)) {
                colNames.add(0, "distinct(concat("
                             + Utils.join(uniqueNames, ",") + "))");
            }
	    */
	}

        Statement stmt = null;
        extra += limitString;
        try {
	    //            SqlUtil.debug = true;
            if (SqlUtil.debug || debugQuery) {
                System.err.println("table:" + tableHandler.getTableName());
                System.err.println("clause:" + clause);
                System.err.println("cols:" + SqlUtil.comma(colNames));
                System.err.println("extra:" + extra);
                //                System.err.println("max:" + max  + " limit:" + limitString);
            }
            long t1 = System.currentTimeMillis();
            //      SqlUtil.debug = true;
            SqlUtil.debug = false;
            stmt = getDatabaseManager().select(SqlUtil.comma(colNames),
                    Misc.newList(tableHandler.getTableName()), clause, extra,
                    max);
            SqlUtil.debug = false;
            long t2 = System.currentTimeMillis();
	    if(myDebug)
		Utils.printTimes("DbTypeHandler select: "+ clause + " extra:" + extra +" time:" ,t1,t2);
        } catch (Exception exc) {
            System.err.println("Error in select:");
            System.err.println("table:" + tableHandler.getTableName());
            System.err.println("clause:" + clause);
            System.err.println("cols:" + SqlUtil.comma(colNames));
            System.err.println("extra:" + extra);
            exc.printStackTrace();
        } finally {
            SqlUtil.debug = false;
        }

	Column latLonColumn = dbInfo.getLatLonColumn();
	java.awt.Polygon polygon=null;
	if(request.defined(ARG_SEARCH_POLYGON)) {
	    String poly = request.getString(ARG_SEARCH_POLYGON,"").trim();
	    List<Double> d = Utils.getDoubles(poly);
	    if(d.size()>2)
		polygon= GeoUtils.makePolygon(d);
	}

        HashSet seenValue = new HashSet();
        try {
            long t1 = System.currentTimeMillis();
            SqlUtil.Iterator iter = getDatabaseManager().getIterator(stmt);
            ResultSet        results;
            Object[]         values = null;
	    double samplePercent = request.get(ARG_SAMPLE,0.0);
	    if(samplePercent>0) {
		samplePercent = Math.min(samplePercent,100)/100;
	    }
	    int cnt=0;
            while ((results = iter.getNext()) != null) {
                int valueIdx = 1;
                //                int valueIdx = 2;
		cnt++;
                if (doGroupBy) {
                    values = new Object[aggColumns.size() + groupByColumns.size()];
                    for (int i = 0; i < groupByColumns.size(); i++) {
                        Column column = groupByColumns.get(i);
                        String v      = results.getString(i + 1);
                        if (forTable && column.isEnumeration()) {
                            v = column.getEnumLabel(v);
                        }
                        values[i] = v;
                    }
                    for (int i = 0; i < aggColumns.size(); i++) {
                        //   Column aggColumn = aggColumns.get(i);
                        String agg   = aggSelectors.get(i);
                        int    index = groupByColumns.size() + i;
                        if (agg.equals("count")) {
                            values[index] = results.getInt(index + 1);
                        } else {
                            values[index] = results.getDouble(index + 1);
                        }
                    }
                    if (pw != null) {
                        pw.println(
                            Utils.encodeBase64(xmlEncoder.toXml(values)));
                    } else {
                        if (iterator != null) {
                            iterator.processRow(request, values);
                        }
                        //Add the values when we are doing groupBy so the caching works
			result.add(values);
                    }
                } else {
                    if (values == null) {
			values = tableHandler.makeEntryValueArray();
                    }

                    for (Column column : selectedColumns) {
                        valueIdx = column.readValues(myEntry, results,
                                values, valueIdx);
		    }
		    if(polygon!=null && latLonColumn!=null) {
			Double lat = (Double)values[latLonColumn.getOffset()];
			Double lon = (Double)values[latLonColumn.getOffset()+1];
			if(lat==null || lon==null) continue;
			if(!GeoUtils.polygonContains(polygon,  lat, lon)) {
			    System.err.println("skipping:" + lat +"  " + lon);
			    continue;
			}
		    }

		    if ( !isPostgres && (uniqueCols != null)) {
			String key = "";
			for (Column c : uniqueCols) {
			    Object o = c.uncheckedGetObject(request,values);
			    key = key + "_" + o;
			}
			if (seenValue.contains(key)) {
			    continue;
			}
			seenValue.add(key);
		    }

		    if(samplePercent>0) {
			if(Math.random()>samplePercent){
			    continue;
			}
		    }
                    if (pw != null) {
                        pw.println(
                            Utils.encodeBase64(xmlEncoder.toXml(values)));
                    } else if (iterator != null) {
                        iterator.processRow(request, values);
                    } else {
                        result.add(values);
                        values = null;
                    }
                }
	    }
            long t2 = System.currentTimeMillis();
	    if(myDebug)
		Utils.printTimes("DbTypeHandler processing: #" + cnt+" time: ",t1,t2);
        } finally {
            getRepository().getDatabaseManager().closeAndReleaseConnection(
                stmt);
        }

        if (iterator != null) {
            iterator.finish(request);
        }
        return result;
    }

    @Override
    public void deleteEntry(Request request, Statement statement, String id,
                            Entry parent, Object[] values)
            throws Exception {
        super.deleteEntry(request, statement, id, parent, values);
        String query = SqlUtil.makeDelete(tableHandler.getTableName(),
                                          COL_ID, SqlUtil.quote(id));

        statement.execute(query);
    }

    public String makeForm(Request request, Entry entry, Appendable sb) {
        String formId  = HU.getUniqueId("entryform_");
        String formUrl = request.makeUrl(getRepository().URL_ENTRY_SHOW);
        Utils.append(sb, HU.uploadForm(formUrl, HU.id(formId)));
        Utils.append(sb, HU.hidden(ARG_ENTRYID, entry.getId()));
        return formId;
    }

    public Result handleForm(Request request, Entry entry, String dbid,
                             boolean forEdit, boolean doAnonForm)
            throws Exception {

        List<String>  colNames = tableHandler.getColumnNames();
        StringBuilder sb       = new StringBuilder();
        addViewHeader(request, entry, sb, ((dbid == null)
                                           ? VIEW_NEW
                                           : ""), null);

        StringBuilder formBuffer = new StringBuilder();

        String        formId     = makeForm(request, entry, formBuffer);
        Object[]      values     = null;
        if (dbid != null) {
            values = getValues(entry, dbid);
            formBuffer.append(HU.hidden(ARG_DBID, dbid));
            formBuffer.append(HU.hidden(ARG_DBID_SELECTED, dbid));
        }

        StringBuilder buttons = new StringBuilder();
        if (doAnonForm) {
            buttons.append(HU.submit(LABEL_SUBMIT, ARG_DB_CREATE));
        } else if (forEdit) {
            if (dbid == null) {
                buttons.append(HU.submit("Create entry",
                        ARG_DB_CREATE));
                buttons.append(HU.buttonSpace());
            } else {
                buttons.append(HU.submit("Edit entry",
                        ARG_DB_EDIT));
                buttons.append(HU.buttonSpace());
                buttons.append(HU.submit("Copy entry",
                        ARG_DB_COPY));
                buttons.append(HU.buttonSpace());
                buttons.append(HU.submit("Delete entry",
                        ARG_DB_DELETE));
                buttons.append(HU.buttonSpace());
            }
            buttons.append(HU.submit("Cancel", ARG_DB_LIST));
            buttons.append(HU.buttonSpace());
        }

        formBuffer.append(buttons);
        formBuffer.append(HU.formTable());
        FormInfo formInfo = new FormInfo(formId);
        tableHandler.addColumnsToEntryForm(request, formBuffer, entry.getParentEntry(),entry,
                                           values, formInfo, this, new HashSet());

        formBuffer.append(HU.formTableClose());
        formBuffer.append(buttons);
        formBuffer.append(HU.formClose());

        StringBuilder validateJavascript = new StringBuilder("");
        formInfo.addJavascriptValidation(validateJavascript);
        String script = JQuery.ready(JQuery.submit(JQuery.id(formId),
                            validateJavascript.toString()));
        formBuffer.append(HU.script(script));

        if (doAnonForm) {
            sb.append(entry.getDescription() + HU.p() + formBuffer);
        } else {
            if (forEdit && (dbid == null)) {
                createBulkForm(request, entry, sb, formBuffer);
            } else {
                sb.append(formBuffer);
            }
        }

        addViewFooter(request, entry, sb);

        return new Result(getTitle(request, entry), sb);
    }

    public Result handleEdit(Request request, Entry entry) throws Exception {
	try {
	    return handleEditInner(request, entry);
	} catch(Exception exc) {
	    StringBuilder sb = new StringBuilder();
	    addViewHeader(request, entry, sb, VIEW_EDIT, null);
	    sb.append(getPageHandler().showDialogError("An error has occurred: " + exc));
	    getLogManager().logError("Error editing DB entry:" + entry, exc);
	    return new Result(getTitle(request, entry), sb);
	}
    }

    private Result handleEditInner(Request request, Entry entry) throws Exception {
        StringBuilder sb = new StringBuilder();
        addViewHeader(request, entry, sb, VIEW_EDIT, null);
        List<Column>         columns = dbInfo.getColumnsToUse();
        List<TwoFacedObject> tfos    = new ArrayList<TwoFacedObject>();
        List<TwoFacedObject> ops     = new ArrayList<TwoFacedObject>();
        List<TwoFacedObject> tfos1   = new ArrayList<TwoFacedObject>();
        ops.add(new TwoFacedObject("=", "="));
        ops.add(new TwoFacedObject("!=", "!="));
        ops.add(new TwoFacedObject("like", "like"));
        ops.add(new TwoFacedObject("&lt;", "<"));
        ops.add(new TwoFacedObject("&gt;", ">"));
        tfos1.add(new TwoFacedObject("----", ""));
        for (Column column : dbInfo.getColumnsToUse()) {
            tfos.add(new TwoFacedObject(column.getLabel(), column.getName()));
            tfos1.add(new TwoFacedObject(column.getLabel(),
                                         column.getName()));
        }
        String formId = makeForm(request, entry, sb);
        sb.append(HU.hidden(ARG_DB_EDITSQL, "true"));

        boolean        showApply = true;
        List<Object[]> values    = null;
        if (request.exists(ARG_DB_APPLY) || request.exists(ARG_DB_TEST)
                || request.exists(ARG_DB_CONFIRM)) {
            String       column     = request.getString(ARG_DB_COLUMN, "");
            String       setValue   = request.getString(ARG_DB_SETVALUE, "");

            Clause       clause     = Clause.eq(COL_ID, entry.getId());
            List<Clause> subClauses = new ArrayList<Clause>();
            for (int i = 1; i <= 3; i++) {
                String whereColumn = request.getString(ARG_DB_WHERECOLUMN
                                         + i, "");
                if (whereColumn.length() == 0) {
                    continue;
                }
                String op = request.getString(ARG_DB_WHEREOP + i, "=");
                String whereValue = request.getString(ARG_DB_WHEREVALUE + i,
                                        "");
                Clause subClause = null;
                if (whereColumn.length() > 0) {
                    if (op.equals("like")) {
                        subClause = Clause.like(whereColumn,
                                "%" + whereValue + "%");
                    } else if (op.equals("=")) {
                        subClause = Clause.eq(whereColumn, whereValue);
                    } else if (op.equals("!=")) {
                        subClause = Clause.neq(whereColumn, whereValue);
                    } else if (op.equals("<")) {
                        subClause = Clause.lt(whereColumn,
					      Double.parseDouble(whereValue));
                    } else if (op.equals(">")) {
                        subClause = Clause.gt(whereColumn,
					      Double.parseDouble(whereValue));
                    } else {
                        throw new RuntimeException("Unknown operator:" + op);
                    }
                }
                if (subClause != null) {
                    subClauses.add(subClause);
                }
            }
            if (subClauses.size() > 0) {
                clause = Clause.and(clause, Clause.and(subClauses));
            }
            System.err.println("Clause:" + clause);
            if ( !request.exists(ARG_DB_CONFIRM)) {
                if (request.exists(ARG_DB_APPLY)) {
                    sb.append(
                        getPageHandler().showDialogQuestion(
                            "Are you sure you want to apply the update?",
                            HU.submit(LABEL_YES, ARG_DB_CONFIRM)
                            + " "
                            + HU.submit(LABEL_CANCEL, ARG_CANCEL)));
                    showApply = false;
                }
                try {
                    //read the sample
                    values = readValues(request, entry, clause, null);
                } catch (Exception exc) {
                    System.err.println("Error reading db sample:" + exc);
                }
            } else {
                //              if(false)
                int cnt =
                    getDatabaseManager().update(tableHandler.getTableName(),
                        clause, new String[] { column },
                        new Object[] { setValue });

                getStorageManager().clearCacheGroup(entry.getId());
                sb.append(getPageHandler().showDialogNote(cnt
                        + " rows updated"));
            }
        }

        sb.append(HU.formTable());
        if (showApply) {
            sb.append(
                HU.formEntry(
                    "",
                    HU.submit("Apply", ARG_DB_APPLY) + " "
                    + HU.submit("Test", ARG_DB_TEST)));
        }
        sb.append(
            HU.formEntry(
                "Set:",
                HU.select(
                    ARG_DB_COLUMN, tfos,
                    request.getString(ARG_DB_COLUMN, "")) + " = "
                        + HU.input(
                            ARG_DB_SETVALUE,
                            request.getString(ARG_DB_SETVALUE, ""),
                            HU.SIZE_20)));

        for (int i = 1; i <= 3; i++) {
            sb.append(HU.formEntry((i == 1)
                                          ? "Where:"
                                          : "", HU.select(ARG_DB_WHERECOLUMN
                                          + i, tfos1, request.getString(ARG_DB_WHERECOLUMN
                                              + i, "")) + HU.select(ARG_DB_WHEREOP
                                                  + i, ops, request.getString(ARG_DB_WHEREOP
                                                      + i, "=")) + HU.input(ARG_DB_WHEREVALUE
                                                          + i, request.getString(ARG_DB_WHEREVALUE
                                                              + i, ""), HU.SIZE_20)));
        }
        sb.append(HU.formTableClose());
        sb.append(HU.formClose());

        if (values != null) {
            makeTable(request, entry, values, false, sb, false, true);
        }

        addViewFooter(request, entry, sb);

        return new Result(getTitle(request, entry), sb);

    }

    public Object[] getValues(Entry entry, String dbid) throws Exception {
        Object[] values = ((dbid != null)
                           ? tableHandler.getValues(myEntry,
                               makeClause(entry, dbid))
                           : tableHandler.makeEntryValueArray());

        return values;
    }

    public void createBulkForm(Request request, Entry entry, Appendable sb,
                               Appendable formBuffer) {
        DbInfo        dbInfo = getDbInfo();
        StringBuilder bulkSB = new StringBuilder();
        makeForm(request, entry, bulkSB);
        StringBuilder bulkButtons = new StringBuilder();
        bulkButtons.append(HU.buttons(
				      HU.submit("Create entries",ARG_DB_CREATE),
				      HU.submit(LABEL_CANCEL, ARG_DB_LIST)));
        bulkSB.append(bulkButtons);
        bulkSB.append(HU.p());
        bulkSB.append(msgLabel("Upload a file"));
        bulkSB.append(HU.formTable());
        bulkSB.append(
            HU.formEntry(
                msgLabel("File"),
                HU.fileInput(ARG_DB_BULK_FILE, HU.SIZE_60)));
        if (request.getUser().getAdmin()) {
            bulkSB.append(
                HU.formEntry(
                    msgLabel("Or server side file"),
                    HU.input(
                        ARG_DB_BULK_LOCALFILE, "", HU.SIZE_60)));
        }
        bulkSB.append(
            HU.formEntry(
                msgLabel("Delimiter"),
                HU.input(ARG_DB_BULK_DELIMITER, ",", HU.SIZE_5)
                + HU.space(2) + msgLabel("# Header Lines")
                + HU.input(ARG_DB_BULK_SKIP, "1", HU.SIZE_5)));

        bulkSB.append(HU
            .formEntry("", HU
                .labeledCheckbox(ARG_DB_BULK_NUKEIT, "true", false, "Delete existing database") + HU
                .space(2) + HU
                .b("!!! Important: this will delete everything in the existing database before doing the bulk load !!!")));

        bulkSB.append(HU.formTableClose());
        bulkSB.append(HU.p());
        bulkSB.append(msgLabel("Or enter text"));
        List colIds = new ArrayList();
        for (Column column : dbInfo.getColumnsToUse()) {
            colIds.add(new TwoFacedObject(column.getLabel(),
                                          column.getName()));
        }
        int cnt = 0;
        for (Column column : dbInfo.getColumnsToUse()) {
            if (cnt > 0) {
                bulkSB.append(", ");
            }
            bulkSB.append(column.getLabel());
            cnt++;
        }
        bulkSB.append(HU.br());
        bulkSB.append(HU.textArea(ARG_DB_BULK_TEXT, "", 10, 80));
        bulkSB.append(HU.p());
        bulkSB.append(bulkButtons);
        bulkSB.append(HU.formClose());
        List<String> tabTitles   = new ArrayList<String>();
        List<String> tabContents = new ArrayList<String>();
       if (showEntryCreate) {
            tabTitles.add(msg("Form"));
            tabContents.add(formBuffer.toString());
        }
        tabTitles.add(msg("Bulk Create"));
        tabContents.add(bulkSB.toString());
        String contents = OutputHandler.makeTabs(tabTitles, tabContents,
                              true);
        Utils.append(sb, contents);
    }

    private String insetHtml(Object html) {
        return HU.insetDiv(html.toString(), 0, 10, 0, 10);
    }

    public Result handleView(Request request, Entry entry, String dbid)
            throws Exception {
        boolean       asXml = request.getString("result", "").equals("xml");
        boolean       plain = request.getString("result", "").equals("plain");
        StringBuilder sb    = new StringBuilder();
        if ( !asXml && !plain) {
            addViewHeader(request, entry, sb, "", null);
        }

        Object[] values = getValues(entry, dbid);
        getHtml(request, sb, entry, values, asXml,plain);
        if ( !asXml && !plain) {
            addViewFooter(request, entry, sb);
        }
        if (asXml) {
            StringBuilder xml = new StringBuilder("<contents>\n");
            XmlUtils.appendCdata(xml, sb.toString());
            xml.append("</contents>");
            return new Result("", xml, "text/xml");
        }
	if(plain) {
            return new Result("", sb, "text/plain");
	}

        return new Result(getTitle(request, entry), sb);
    }

    public void getHtml(Request request, StringBuilder sb, Entry entry,
                        Object[] values, boolean asXml,boolean plain)
            throws Exception {
        if (asXml) {
            sb.append(HU.formTable("formtable_tight"));
        } else {
            sb.append(HU.formTable());
        }

        SimpleDateFormat sdf = getDateFormat(request,entry);
        SimpleDateFormat dateTimeSdf = getDateTimeFormat(request,entry);	
        for (Column column : getDbColumns(true)) {
	    if(column.isSynthetic()){
		continue;
	    }
	    if(plain && column.isGeo()) continue;
            if ( !isDataColumn(column)) {
                continue;
            }
            if ( !column.getCanDisplay()) {
                continue;
            }
            StringBuilder tmpSb = new StringBuilder();
            formatTableValue(request, entry, tmpSb, column, values, sdf,dateTimeSdf,
                             true);
            String tmp = tmpSb.toString();
	    tmp = tmp.replaceAll("'", "&apos;");
            sb.append(formEntry(request, column.getLabel() + ":", tmp));
        }
        sb.append(HU.formTableClose());

    }

    public String getLabel(Request request, Entry entry, Object[] values,
                           SimpleDateFormat sdf,String...prefix)
            throws Exception {
        String lbl = getLabelInner(request, entry, values, sdf);
        if ( !Utils.stringDefined(lbl)) {
            lbl = "---";
        }

	if(prefix.length>0 && prefix[0]!=null) 
	    lbl = prefix[0]+lbl;
        return lbl;
    }

    public String getMapLabel(Request request, Entry entry, Object[] values,
                              SimpleDateFormat sdf, boolean forPrint,String prefix)
            throws Exception {
        if (forPrint && (mapLabelTemplatePrint != null)) {
            return applyTemplate(request, entry, values, sdf,
                                 mapLabelTemplatePrint,prefix);
        }
        if (mapLabelTemplate != null) {
            return applyTemplate(request, entry, values, sdf,
                                 mapLabelTemplate,prefix);
        }

        return getLabel(request, entry, values, sdf,prefix);
    }

    public String getCalendarLabel(Request request, Entry entry,
                                   Object[] values, SimpleDateFormat sdf)
            throws Exception {
        return getLabel(request, entry, values, sdf);
    }

    public String applyTemplate(Request request, Entry entry,
                                Object[] values, SimpleDateFormat sdf,
                                String template,String...prefix)
            throws Exception {
        String        label = template;
        StringBuilder sb    = new StringBuilder();
        for (Column column : getDbColumns()) {
            sb.setLength(0);
            column.formatValue(request, entry, sb, Column.OUTPUT_HTML,
                               values, sdf, false);
            label = label.replace("${" + column.getName() + "}",
                                  sb.toString());
        }

	label = label.replace("${_prefix}", (prefix.length>0 && prefix[0]!=null)?prefix[0]:"");
        return label;
    }

    public String getLabelInner(Request request, Entry entry,
                                Object[] values, SimpleDateFormat sdf)
            throws Exception {
        DbInfo dbInfo = getDbInfo();
        if (labelTemplate != null) {
            return applyTemplate(request, entry, values, sdf, labelTemplate);
        }
        StringBuilder sb = new StringBuilder();

        if (dbInfo.getLabelColumns() != null) {
            for (Column labelColumn : dbInfo.getLabelColumns()) {
                labelColumn.formatValue(request, entry, sb,
                                        Column.OUTPUT_HTML, values, sdf,
                                        false);
                sb.append(" ");
            }
            return sb.toString().trim();
        }
        for (Column column : getDbColumns()) {
            if ( !isDataColumn(column)) {
                continue;
            }
            String type = column.getType();
            if (type.equals(Column.DATATYPE_STRING)
                    || type.equals(Column.DATATYPE_ENUMERATION)
                    || type.equals(Column.DATATYPE_URL)
                    || type.equals(Column.DATATYPE_EMAIL)
                    || type.equals(Column.DATATYPE_ENUMERATIONPLUS)) {
                column.formatValue(request, entry, sb, Column.OUTPUT_HTML,
                                   values, false);
                String label = sb.toString();
                if (label.length() > 0) {
                    return label;
                }
            }
        }

        return "";

    }

    protected String getHtml(Request request, Entry entry, String dbid,
                             List<Column> columns, Object[] values,
                             SimpleDateFormat sdf,SimpleDateFormat dateTimeSdf,
			     boolean skipLocation,int limit)
            throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append(HU.formTable());
        int valueIdx = 0;
	int cnt=0;
        for (Column column : columns) {
	    if(skipLocation && column.isGeo()) continue;
            if ( !isDataColumn(column)) {
                continue;
            }
            if (column.getName().equals("polygon")) {
                continue;
            }
	    cnt++;
	    if(limit>=0 &&cnt>limit) break;

            StringBuilder tmpSb = new StringBuilder();
            formatTableValue(request, entry, tmpSb, column, values, sdf,dateTimeSdf,
                             true);
            //            column.formatValue(request, entry, tmpSb, Column.OUTPUT_HTML, values);
            String tmp = tmpSb.toString();
            tmp = tmp.replaceAll("'", "&apos;");
            sb.append(formEntry(request, column.getLabel() + ":", tmp));
        }
        sb.append(HU.formTableClose());

        return sb.toString();
    }

    @Override
    public RecordFile doMakeRecordFile(Request request, Entry entry,
                                       Hashtable properties,
                                       Hashtable requestProperties)
            throws Exception {
        return new DbRecordFile(request, this, entry);
    }

}
