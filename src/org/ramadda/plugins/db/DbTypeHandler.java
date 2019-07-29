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
import org.ramadda.repository.output.RssOutputHandler;
import org.ramadda.repository.output.WikiConstants;
import org.ramadda.repository.type.*;
import org.ramadda.util.Bounds;
import org.ramadda.util.FormInfo;
import org.ramadda.util.GoogleChart;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JQuery;
import org.ramadda.util.JQuery;
import org.ramadda.util.Json;

import org.ramadda.util.KmlUtil;
import org.ramadda.util.RssUtil;
import org.ramadda.util.Utils;
import org.ramadda.util.WikiUtil;
import org.ramadda.util.XlsUtil;
import org.ramadda.util.XmlUtils;
import org.ramadda.util.sql.*;

import org.ramadda.util.text.CsvUtil;
import org.ramadda.util.text.Filter;
import org.ramadda.util.text.Processor;
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

import java.util.TimeZone;


/**
 *
 */

public class DbTypeHandler extends PointTypeHandler implements DbConstants /* BlobTypeHandler*/ {


    /** _more_ */
    public static final int IDX_DBID = 0;

    /** _more_ */
    public static final int IDX_DBUSER = 1;

    /** _more_ */
    public static final int IDX_DBCREATEDATE = 2;

    /** _more_ */
    public static final int IDX_DBPROPS = 3;

    /** _more_ */
    public static final int IDX_MAX_INTERNAL = 3;

    /** _more_ */
    private DbInfo dbInfo;


    /** _more_ */
    protected GenericTypeHandler tableHandler;

    /** _more_ */
    protected Entry myEntry;

    /** _more_ */
    private List<String> icons;

    /** _more_ */
    private String tableIcon = "";

    /** _more_ */
    protected List<TwoFacedObject> viewList = new ArrayList<TwoFacedObject>();


    /** _more_ */
    SimpleDateFormat rssSdf =
        new SimpleDateFormat("EEE dd, MMM yyyy HH:mm:ss z");

    /** _more_ */
    private DecimalFormat ifmt = new DecimalFormat("#,##0");

    /** _more_ */
    private DecimalFormat dfmt = new DecimalFormat("#,##0.#");

    /** _more_ */
    private DecimalFormat pfmt = new DecimalFormat("0.00");



    /** _more_ */
    XmlEncoder xmlEncoder = new XmlEncoder();



    /** _more_ */
    private String[] namesArray;


    /** _more_ */
    private String labelColumnNames;


    /** _more_ */
    private List<DbTemplate> templates = new ArrayList<DbTemplate>();

    /**
     * _more_
     *
     *
     * @param repository _more_
     * @param tableName _more_
     * @param tableNode _more_
     * @param desc _more_
     *
     * @throws Exception _more_
     */
    public DbTypeHandler(Repository repository, String tableName,
                         Element tableNode, String desc)
            throws Exception {
        super(repository, tableName, desc);

        this.tableIcon = XmlUtil.getAttribute(tableNode, "icon",
                "/db/database.png");

        this.labelColumnNames = XmlUtil.getAttribute(tableNode,
                "labelColumns", "");
        //Initialize this type handler with a string blob
        Element root = XmlUtil.getRoot("<type></type>");
        root.setAttribute(ATTR_SUPER, "type_point");
        Element node = XmlUtil.create("column", root, new String[] {
            "name", "contents", Column.ATTR_TYPE, "clob", Column.ATTR_SIZE,
            "256000", Column.ATTR_SHOWINFORM, "false", Column.ATTR_SHOWINHTML,
            "false"
        });
        List<Element> nodes = new ArrayList<Element>();
        nodes.add(node);
        super.initTypeHandler(root);
        super.initColumns(nodes);

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


    /**
     * _more_
     *
     * @param template _more_
     */
    public void addTemplate(DbTemplate template) {
        templates.add(template);
        viewList.add(new TwoFacedObject(template.name, template.id));
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public DbInfo getDbInfo() {
        if (dbInfo == null) {
            dbInfo = new DbInfo(this, IDX_MAX_INTERNAL);
        }

        return dbInfo;
    }


    /**
     * _more_
     *
     * @param columnNodes _more_
     *
     * @throws Exception _more_
     */
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

        viewList.add(new TwoFacedObject("Sticky Notes", VIEW_STICKYNOTES));
        if (dbInfo.getHasDate()) {
            viewList.add(new TwoFacedObject("Calendar", VIEW_CALENDAR));
            viewList.add(new TwoFacedObject("Timeline", VIEW_TIMELINE));
            viewList.add(new TwoFacedObject("ICAL", VIEW_ICAL));
        }
        if (dbInfo.getHasLocation()) {
            putProperty("form.area.show", "true");
            viewList.add(new TwoFacedObject("Map", VIEW_MAP));
            //            viewList.add(new TwoFacedObject("KML", VIEW_KML));
        }
        if (dbInfo.getNumberColumns().size() > 0) {
            viewList.add(new TwoFacedObject("Chart", VIEW_CHART));
        }
        for (Column gridColumn : dbInfo.getCategoryColumns()) {
            viewList.add(new TwoFacedObject(gridColumn.getLabel() + " "
                                            + "Category", VIEW_CATEGORY
                                                + gridColumn.getName()));
        }
        viewList.add(new TwoFacedObject("RSS", VIEW_RSS));
        viewList.add(new TwoFacedObject("CSV", VIEW_CSV));
        viewList.add(new TwoFacedObject("JSON", VIEW_JSON));
    }


    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public int getValuesIndex() {
        return PointTypeHandler.IDX_LAST + 1;
    }


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     * @param node _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void addToEntryNode(Request request, Entry entry, Element node)
            throws Exception {
        super.addToEntryNode(request, entry, node);
        if (getAccessManager().canDoAction(request, entry,
                                           Permission.ACTION_FILE)) {
            List<Object[]> valueList = readValues(request,
                                           Clause.eq(COL_ID, entry.getId()),
                                           "", -1);
            Element dbvalues = XmlUtil.create(TAG_DBVALUES, node);
            XmlUtil.createCDataNode(dbvalues,
                                    xmlEncoder.toXml(valueList, false));
        }
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param node _more_
     *
     * @throws Exception _more_
     */
    public void initializeEntryFromXml(Request request, Entry entry,
                                       Element node)
            throws Exception {
        super.initializeEntryFromXml(request, entry, node);
        String values = XmlUtil.getGrandChildText(node, TAG_DBVALUES,
                            (String) null);
        if (values == null) {
            String csv = XmlUtil.getGrandChildText(node, TAG_CSV,
                             (String) null);
            if (csv == null) {
                return;
            }
            csv = csv.trim();

            handleBulkUpload(request, entry,
                             new ByteArrayInputStream(csv.getBytes()));

            return;

        }
        List<Object[]> valueList = (List<Object[]>) xmlEncoder.toObject(
                                       new String(
                                           RepositoryUtil.decodeBase64(
                                               values)));
        if (valueList == null) {
            throw new IllegalArgumentException(
                "Could not read database value list");
        }
        String sql = makeInsertOrUpdateSql(entry, null);
        PreparedStatement insertStmt =
            getRepository().getDatabaseManager().getPreparedStatement(sql);
        try {
            for (Object[] tuple : valueList) {
                tuple[IDX_DBID] = getRepository().getGUID();
                tableHandler.setStatement(entry, tuple, insertStmt, true);
                insertStmt.execute();
            }
        } finally {
            getRepository().getDatabaseManager().closeAndReleaseConnection(
                insertStmt);
        }
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getTableName() {
        return "properties_" + super.getTableName();
    }



    /**
     * _more_
     *
     * @param newEntry _more_
     * @param oldEntry _more_
     *
     * @throws Exception _more_
     */
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
            getRepository().getDatabaseManager().closeAndReleaseConnection(
                stmt);
            getRepository().getDatabaseManager().closeAndReleaseConnection(
                insertStmt);
        }
    }


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     * @return _more_
     */
    public String getTitle(Request request, Entry entry) {
        return entry.getName();
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public List<Column> getColumns() {
        return getDbInfo().getColumns();
    }

    /**
     * _more_
     *
     * @param sorted _more_
     *
     * @return _more_
     */
    public List<Column> getColumns(boolean sorted) {
        return getDbInfo().getColumns(sorted);
    }

    /**
     * _more_
     *
     * @param name _more_
     *
     * @return _more_
     */
    public Column getColumn(String name) {
        return getDbInfo().getColumn(name);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected GenericTypeHandler getTableTypeHandler() {
        return tableHandler;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getInlineHtml(Request request, Entry entry)
            throws Exception {
        if ( !getRepository().getAccessManager().canAccessFile(request,
                entry)) {
            return null;
        }

        request.remove(ARG_OUTPUT);
        List<Object[]> valueList = readValues(request, entry,
                                       Clause.eq(COL_ID, entry.getId()));
        StringBuilder sb = new StringBuilder();
        addStyleSheet(sb);
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



    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     */
    private String getWhatToShow(Request request) {
        String what = request.getString(ARG_WHAT, null);
        if (what != null) {
            return what;
        }

        return request.getString(ARG_DB_VIEW,
                                 request.getString(ARG_VIEW, VIEW_TABLE));
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result getHtmlDisplay(Request request, Entry entry)
            throws Exception {

        StringBuilder sb = new StringBuilder();
        if ( !getRepository().getAccessManager().canAccessFile(request,
                entry)) {
            sb.append(
                getPageHandler().showDialogWarning(
                    msg("You do not have permission to view database")));

            return new Result(getTitle(request, entry), sb);
        }


        Hashtable props = getProperties(entry);

        boolean doAnonForm = Misc.getProperty(props, PROP_ANONFORM_ENABLED,
                                 false);
        if (doAnonForm && !getAccessManager().canEditEntry(request, entry)) {
            if (request.exists(ARG_DB_CREATE)) {
                return handleNewOrEdit(request, entry, null, doAnonForm);
            }

            return handleForm(request, entry, null, true, doAnonForm);
        }



        boolean      canEdit = getAccessManager().canEditEntry(request,
                                   entry);

        List<String> colNames = tableHandler.getColumnNames();
        String       view     = getWhatToShow(request);




        if (request.get(ARG_DB_SETPOS, false)) {
            if ( !canEdit) {
                throw new IllegalArgumentException(
                    "You cannot change the position");
            }
            String posx = request.getString("posx", "").replace("px", "");
            String posy = request.getString("posy", "").replace("px", "");
            if (request.exists(ARG_DB_STICKYLABEL)) {
                String label = request.getString(ARG_DB_STICKYLABEL, "");
                props.put(PROP_STICKY_POSX + "." + label, posx);
                props.put(PROP_STICKY_POSY + "." + label, posy);
                setProperties(entry, props);
                getEntryManager().updateEntry(request, entry);
            } else {
                Object[] values = getValues(entry,
                                            request.getString(ARG_DBID, ""));
                putProp("posx", values, new Integer(posx));
                putProp("posy", values, new Integer(posy));
                doStore(entry, values, false);
            }

            return new Result("",
                              new StringBuilder("<contents>ok</contents>"),
                              "text/xml");
        }


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


        if (request.exists(ARG_DB_SEARCH) || request.isEmbedded()) {
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


        if (request.exists(ARG_DB_DELETECONFIRM)) {
            if ( !canEdit) {
                throw new AccessException("You cannot edit this database",
                                          request);
            }

            return handleDeleteConfirm(request, entry);
        }



        if (request.exists(ARG_DB_DELETE) || action.equals(ACTION_DELETE)
                || action.equals(ACTION_DELETEALL)) {
            if ( !canEdit) {
                throw new AccessException("You cannot edit this database",
                                          request);
            }

            return handleDeleteAsk(request, entry, action);
        }


        if (request.defined(ARG_DB_VIEW)) {
            return handleList(request, entry, action);
        }

        return handleSearchForm(request, entry);


    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void addViewFooter(Request request, Entry entry, Appendable sb)
            throws Exception {
        if ( !request.get(ARG_EMBEDDED, false)) {
            getPageHandler().entrySectionClose(request, entry, sb);
        }
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param sb _more_
     * @param view _more_
     * @param numValues _more_
     * @param fromSearch _more_
     *
     * @throws Exception _more_
     */
    public void addViewHeader(Request request, Entry entry, Appendable sb,
                              String view, int numValues, boolean fromSearch)
            throws Exception {

        addViewHeader(request, entry, sb, view, numValues, fromSearch, null);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param sb _more_
     * @param view _more_
     * @param numValues _more_
     * @param fromSearch _more_
     * @param extraLinks _more_
     *
     * @throws Exception _more_
     */
    public void addViewHeader(Request request, Entry entry, Appendable sb,
                              String view, int numValues, boolean fromSearch,
                              String extraLinks)
            throws Exception {

       if ( !request.get(ARG_DB_SHOWHEADER, true)) {
            return;
        }
        Hashtable props = getProperties(entry);
        boolean doAnonForm = Misc.getProperty(props, PROP_ANONFORM_ENABLED,
                                 false);
        if (doAnonForm) {
            if ( !getAccessManager().canEditEntry(request, entry)) {
                addStyleSheet(sb);

                return;
            }
        }

        boolean embedded = request.get(ARG_EMBEDDED, false);
        if (embedded) {
            addStyleSheet(sb);

            return;
        }

        addStyleSheet(sb);
        if ( !request.get(ARG_EMBEDDED, false)) {
            getPageHandler().entrySectionOpen(request, entry, sb, "", true);
        }


        if (Utils.stringDefined(entry.getDescription())) {
            sb.append(getWikiManager().wikifyEntry(request, entry,
                    entry.getDescription()));
        }

        List<String> headerToks = new ArrayList<String>();
        String baseUrl =
            HtmlUtils.url(request.makeUrl(getRepository().URL_ENTRY_SHOW),
                          new String[] { ARG_ENTRYID,
                                         entry.getId() });
        boolean[] addNext = { false };
        addHeaderItems(request, entry, view, headerToks, baseUrl, addNext);
        if (headerToks.size() > 1) {
            sb.append(HtmlUtils.div(StringUtil.join("&nbsp;|&nbsp;",
                    headerToks), HtmlUtils.cssClass(CSS_DB_HEADER)));
        }

        List<Metadata> savedSearchMetadata =
            getMetadataManager().getMetadata(entry, METADATA_SAVEDSEARCH);
        if (savedSearchMetadata.size() > 0) {
            String searchId = request.getString(ARG_DB_SEARCHID, "");
            headerToks = new ArrayList<String>();
            for (Metadata m : savedSearchMetadata) {
                StringBuilder link = new StringBuilder();
                if (m.getId().equals(searchId)) {
                    request.remove(ARG_DB_SEARCHNAME);
                    headerToks.add(HtmlUtils.b(m.getAttr1()));
                } else {
                    String url = baseUrl + "&"
                                 + HtmlUtils.args(ARG_DB_SEARCH, "true",
                                     ARG_DB_SEARCHID, m.getId());

                    headerToks.add(HtmlUtils.href(url, m.getAttr1()));
                }
            }
            sb.append(HtmlUtils.div(StringUtil.join("&nbsp;|&nbsp;",
                    headerToks), HtmlUtils.cssClass(CSS_DB_HEADER)));
        }

        if (request.defined(ARG_MESSAGE)) {
            sb.append(
                getPageHandler().showDialogNote(
                    request.getString(ARG_MESSAGE, "")));
            request.remove(ARG_MESSAGE);
        }

        if (extraLinks != null) {
            sb.append(HtmlUtils.div(extraLinks,
                                    HtmlUtils.cssClass(CSS_DB_HEADER)));
        }

        if (request.defined(ARG_DB_SEARCHNAME)) {
            HtmlUtils.sectionHeader(sb,
                                    request.getString(ARG_DB_SEARCHNAME, ""));
        }

        if (fromSearch) {
            /*
            sb.append(HtmlUtils.makeShowHideBlock(msg("Search again"
                    + ( !fromSearch
                        ? ""
                        : " -- " + numValues
                          + " results")), getSearchForm(request,
                              entry).toString(), false));
            */
            sb.append(HtmlUtils.makeShowHideBlock(msg("Search again"),
                    getSearchForm(request, entry).toString(), false));
        }




        if (addNext[0]) {
            if (numValues > 0) {
                if ((numValues == getMax(request))
                        || request.defined(ARG_SKIP)) {
                    getRepository().getHtmlOutputHandler().showNext(request,
                            numValues, sb);
                } else {
                    sb.append(numValues + ((numValues == 1)
                                           ? " result"
                                           : " results"));
                }
            }
        }

    }




    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param view _more_
     * @param headerToks _more_
     * @param baseUrl _more_
     * @param addNext _more_
     *
     * @throws Exception _more_
     */
    public void addHeaderItems(Request request, Entry entry, String view,
                               List<String> headerToks, String baseUrl,
                               boolean[] addNext)
            throws Exception {

        DbInfo  dbInfo  = getDbInfo();
        boolean canEdit = getAccessManager().canEditEntry(request, entry);
        boolean canDoNew = getAccessManager().canDoAction(request, entry,
                               Permission.ACTION_NEW);

        if (showInHeader(VIEW_SEARCH,true)) {
            if (view.equals(VIEW_SEARCH)) {
                headerToks.add(HtmlUtils.b(msg("Search")));
            } else {
                headerToks.add(HtmlUtils.href(baseUrl + "&" + ARG_DB_VIEW
                        + "=" + VIEW_SEARCH, msg("Search")));
            }
        }

        if (showInHeader(VIEW_TABLE,true)) {
            if (view.equals(VIEW_TABLE)) {
                addNext[0] = true;
                headerToks.add(HtmlUtils.b(msg("List")));
            } else {
                headerToks.add(HtmlUtils.href(baseUrl + "&" + ARG_DB_VIEW
                        + "=" + VIEW_TABLE, msg("List")));
            }
        }

        if (canDoNew && showInHeader(VIEW_NEW,true)) {
            if (view.equals(VIEW_NEW)) {
                headerToks.add(HtmlUtils.b(msg("New")));
            } else {
                headerToks.add(HtmlUtils.href(baseUrl + "&" + ARG_DB_VIEW
                        + "=" + VIEW_NEW, msg("New")));
            }
        }



        /*
          if(showInHeader(VIEW_STICKYNOTES)) {
          if(view.equals(VIEW_STICKYNOTES)) {
          addNext[0] = true;
          headerToks.add(HtmlUtils.b(msg("Sticky Notes")));
          } else {
          headerToks.add(HtmlUtils.href(baseUrl+"&" +ARG_DB_VIEW +"=" + VIEW_STICKYNOTES,
          msg("Sticky Notes")));
          }
          }
        */

        if (dbInfo.getHasDate()) {
            if (showInHeader(VIEW_CALENDAR)) {
                if (view.equals(VIEW_CALENDAR)) {
                    headerToks.add(HtmlUtils.b(msg("Calendar")));
                } else {
                    headerToks.add(HtmlUtils.href(baseUrl + "&" + ARG_DB_VIEW
                            + "=" + VIEW_CALENDAR, msg("Calendar")));
                }
            }
            /*
              if(showInHeader(VIEW_TIMELINE)) {
              if(view.equals(VIEW_TIMELINE)) {
              addNext[0] = true;
              headerToks.add(HtmlUtils.b(msg("Timeline")));
              } else {
              headerToks.add(HtmlUtils.href(baseUrl+"&" +ARG_DB_VIEW +"=" + VIEW_TIMELINE,
              msg("Timeline")));
              }
              if(showInHeader(VIEW_)) {
              if(showInHeader(VIEW_ICAL)) {
              if(view.equals(VIEW_ICAL)) {
              headerToks.add(HtmlUtils.b(msg("ICAL")));
              } else {
              String icalUrl = HtmlUtils.url(request.makeUrl(getRepository().URL_ENTRY_SHOW)+"/" + entry.getName()+".ics",
              new String[]{
              ARG_ENTRYID, entry.getId()});
              headerToks.add(HtmlUtils.href(icalUrl+"&" +ARG_DB_VIEW +"=" + VIEW_ICAL,
              msg("ICAL")));
              }
              }*/
        }

        if (dbInfo.getHasLocation()) {
            if (showInHeader(VIEW_MAP, true)) {
                if (view.equals(VIEW_MAP)) {
                    addNext[0] = true;
                    headerToks.add(HtmlUtils.b(msg("Map")));
                } else {
                    headerToks.add(HtmlUtils.href(baseUrl + "&" + ARG_DB_VIEW
                            + "=" + VIEW_MAP, msg("Map")));
                }
            }
            /*
            if (showInHeader(VIEW_KML)) {
                if (view.equals(VIEW_KML)) {
                    //                addNext[0] = true;
                    //                headerToks.add(HtmlUtils.b(msg("Map")));
                } else {
                    String kmlUrl =
                        HtmlUtils.url(
                            request.makeUrl(getRepository().URL_ENTRY_SHOW) + "/"
                            + entry.getName()
                            + ".kml", new String[] { ARG_ENTRYID,
                            entry.getId() });

                    headerToks.add(HtmlUtils.href(kmlUrl + "&" + ARG_DB_VIEW
                            + "=" + VIEW_KML, msg("KML")));
                }
            }
            */
        }

        if (dbInfo.getHasNumber()) {
            if (showInHeader(VIEW_CHART)) {
                addNext[0] = true;
                if (view.equals(VIEW_CHART)) {
                    headerToks.add(HtmlUtils.b(msg("Chart")));
                } else {
                    headerToks.add(HtmlUtils.href(baseUrl + "&" + ARG_DB_VIEW
                            + "=" + VIEW_CHART, msg("Chart")));
                }
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
                    if (view.equals(VIEW_CATEGORY + column.getName())) {
                        headerToks.add(HtmlUtils.b(label));
                    } else {
                        headerToks.add(HtmlUtils.href(baseUrl + "&"
                                + ARG_DB_VIEW + "=" + VIEW_CATEGORY
                                + column.getName() + "&" + ARG_DB_COLUMN
                                + "=" + column.getName(), label));
                    }
                }
            }
        }
    }


    /**
     * _more_
     *
     * @param view _more_
     *
     * @return _more_
     */
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

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param action _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Result handleList(Request request, Entry entry, String action)
            throws Exception {
        return handleList(request, entry, Clause.eq(COL_ID, entry.getId()),
                          action, false);
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param clause _more_
     * @param action _more_
     * @param fromSearch _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Result handleList(Request request, Entry entry, Clause clause,
                              String action, boolean fromSearch)
            throws Exception {
        DbInfo dbInfo = getDbInfo();
        String view   = getWhatToShow(request);
        if (view.equals(VIEW_CHART)) {
            return handleListChart(request, entry, fromSearch);
        }

        List<Object[]> valueList;

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

        if (doingGeo) {
            List<Clause> geoClauses = new ArrayList<Clause>();
            for (Column column : getColumns()) {
                column.addGeoExclusion(geoClauses);
            }
            if (geoClauses.size() > 0) {
                clause = Clause.and(clause, Clause.and(geoClauses));
            }
        }
        if (view.equals(VIEW_KML) && !request.defined(ARG_MAX)) {
            request.put(ARG_MAX, "10000");
        }
        valueList = readValues(request, entry, clause);

        return makeListResults(request, entry, view, action, fromSearch,
                               valueList);
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     */
    private boolean isGroupBy(Request request) {
        return Utils.stringDefined(request.getString(ARG_GROUPBY, ""));
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param view _more_
     * @param action _more_
     * @param fromSearch _more_
     * @param valueList _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result makeListResults(Request request, Entry entry, String view,
                                  String action, boolean fromSearch,
                                  List<Object[]> valueList)
            throws Exception {
        boolean doGroupBy = isGroupBy(request);
        if (action.equals(ACTION_CSV) || view.equals(VIEW_CSV)) {
            return handleListCsv(request, entry, valueList, doGroupBy);
        }

        if (action.equals(ACTION_JSON) || view.equals(VIEW_JSON)) {
            return handleListJson(request, entry, valueList);
        }

        if ( !doGroupBy) {
            if (view.equals(VIEW_RSS)) {
                return handleListRss(request, entry, valueList);
            }

            if (action.equals(ACTION_EMAIL)) {
                return handleListEmail(request, entry, valueList);
            }
            if (view.equals(VIEW_SEARCH)) {
                return handleSearchForm(request, entry);
            }
            if (view.equals(VIEW_MAP)) {
                return handleListMap(request, entry, valueList, fromSearch);
            }
            if (view.equals(VIEW_STICKYNOTES)) {
                return handleListStickyNotes(request, entry, valueList,
                                             fromSearch);
            }

            if (view.equals(VIEW_KML)) {
                return handleListKml(request, entry, valueList, fromSearch);
            }

            if (view.startsWith(VIEW_GRID)) {
                return handleListGrid(request, entry, valueList, fromSearch);
            }

            if (view.startsWith(VIEW_CATEGORY)) {
                return handleListCategory(request, entry, valueList,
                                          fromSearch);
            }

            if (view.equals(VIEW_TIMELINE)) {
                return handleListTimeline(request, entry, valueList,
                                          fromSearch);
            }

            if (view.equals(VIEW_CHART)) {
                //                return handleListChart(request, entry, valueList, fromSearch);
            }

            if (view.equals(VIEW_CALENDAR)) {
                return handleListCalendar(request, entry, valueList,
                                          fromSearch);
            }


            if (view.equals(VIEW_ICAL)) {
                return handleListIcal(request, entry, valueList, fromSearch);
            }

            for (DbTemplate template : templates) {
                if (view.equals(template.id)) {
                    return handleListTemplate(request, entry, template,
                            valueList);
                }
            }


        }

        return handleListTable(request, entry, valueList, fromSearch, true);
    }








    /**
     * _more_
     *
     * @param request _more_
     * @param formBuffer _more_
     * @param parentEntry _more_
     * @param entry _more_
     * @param formInfo _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void addToEntryForm(Request request, Appendable formBuffer,
                               Entry parentEntry, Entry entry,
                               FormInfo formInfo)
            throws Exception {
        DbInfo dbInfo = getDbInfo();
        if ((dbInfo.getUrlColumn() != null) && (entry != null)) {
            String baseUrl =
                request.getAbsoluteUrl(
                    HtmlUtils.url(
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

            String href = HtmlUtils.href(jsUrl,
                                         " Add URL to " + entry.getName());
            formBuffer.append(
                HtmlUtils.row(
                    HtmlUtils.colspan(
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

        formBuffer.append(
            HtmlUtils.row(
                HtmlUtils.colspan(
                    HtmlUtils.div(
                        msg("Sticky Notes"),
                        " class=\"formgroupheader\" "), 2)));


        String stickyLabelString = (String) props.get(PROP_STICKY_LABELS);
        if (stickyLabelString == null) {
            stickyLabelString = "";
        }
        formBuffer.append(formEntry(request, msg("Labels"),
                                    HtmlUtils.textArea(PROP_STICKY_LABELS,
                                        stickyLabelString, 5, 30)));


    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param formBuffer _more_
     *
     * @throws Exception _more_
     */
    public void addToEditForm(Request request, Entry entry,
                              Appendable formBuffer)
            throws Exception {
        Hashtable props = getProperties(entry);


        formBuffer.append(
            HtmlUtils.formEntry(
                "",
                HtmlUtils.checkbox(
                    PROP_ANONFORM_ENABLED, "true",
                    Misc.getProperty(
                        props, PROP_ANONFORM_ENABLED, false)) + " "
                            + msg("Allow anonymous form submission")));
        formBuffer.append(HtmlUtils.formEntry(msgLabel("Message"),
                HtmlUtils.input(PROP_ANONFORM_MESSAGE,
                    Misc.getProperty(props, PROP_ANONFORM_MESSAGE, ""),
                        HtmlUtils.SIZE_80) + " "
                            + msg("What to show the user after they create an item")));
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param formBuffer _more_
     *
     * @throws Exception _more_
     */
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
            getRepository().getResource("/org/ramadda/plugins/db/icons.txt"),
            "\n", true, true);

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
            List<TwoFacedObject> tfos = getEnumValues(request, entry, col);
            if ((tfos != null) && (tfos.size() < 150) && (tfos.size() > 0)) {
                formBuffer.append(
                    HtmlUtils.row(
                        HtmlUtils.colspan(
                            HtmlUtils.div(
                                msg("Settings for") + " " + col.getLabel(),
                                HtmlUtils.cssClass("formgroupheader")), 2)));

                for (TwoFacedObject tfo : tfos) {
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
                    colorSB.append(HtmlUtils.radio(colorArg, "",
                            currentColor.equals("")));
                    colorSB.append(msg("None"));
                    colorSB.append(" ");
                    for (String c : colors) {
                        colorSB.append(
                            HtmlUtils.span(
                                HtmlUtils.radio(
                                    colorArg, c,
                                    currentColor.equals(c)), HtmlUtils.style(
                                        "margin-left:2px; margin-right:2px; padding-left:5px; padding-right:7px; border:1px solid #000; background-color:"
                                        + c)));
                    }
                    StringBuilder iconSB = new StringBuilder();
                    iconSB.append(HtmlUtils.radio(iconArg, "",
                            currentIcon.equals("")));
                    iconSB.append(msg("None"));
                    iconSB.append("  ");
                    iconSB.append(msg("Custom:"));
                    iconSB.append(" ");
                    iconSB.append(HtmlUtils.input(iconArg + "_custom",
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
                        iconSB.append(HtmlUtils.radio(iconArg, icon,
                                currentIcon.equals(icon)));
                        iconSB.append(HtmlUtils.img(getDbIconUrl(icon),
                                IOUtil.getFileTail(icon), "width=24"));
                        iconSB.append(" ");
                    }
                    formBuffer.append(HtmlUtils.formEntry(msgLabel("Value"),
                            value));
                    formBuffer.append(
                        HtmlUtils.formEntryTop(
                            msgLabel("Color"), colorSB.toString()));
                    String iconMsg = "";
                    if (currentIcon.length() > 0) {
                        iconMsg = HtmlUtils.img(getDbIconUrl(currentIcon),
                                currentIcon, "width=16");
                    }
                    formBuffer.append(
                        HtmlUtils.formEntryTop(
                            msgLabel("Icon"),
                            HtmlUtils.makeShowHideBlock(
                                iconMsg, iconSB.toString(), false)));
                    formBuffer.append(HtmlUtils.formEntry("", "<hr>"));
                }
                formBuffer.append(HtmlUtils.formEntry("", sb.toString()));
            }
        }


    }


    /**
     * _more_
     *
     * @param icon _more_
     *
     * @return _more_
     */
    private String getDbIconUrl(String icon) {
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


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param parent _more_
     * @param newEntry _more_
     *
     * @throws Exception _more_
     */
    public void initializeEntryFromForm(Request request, Entry entry,
                                        Entry parent, boolean newEntry)
            throws Exception {
        super.initializeEntryFromForm(request, entry, parent, newEntry);
        Hashtable props = getProperties(entry);


        props.put(PROP_ANONFORM_ENABLED,
                  request.get(PROP_ANONFORM_ENABLED, false) + "");
        props.put(PROP_ANONFORM_MESSAGE,
                  request.getString(PROP_ANONFORM_MESSAGE, ""));

        String stickyLabelString = request.getString(PROP_STICKY_LABELS, "");
        props.put(PROP_STICKY_LABELS,
                  StringUtil.join("\n",
                                  StringUtil.split(stickyLabelString, "\n",
                                      true, true)));

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
            List<TwoFacedObject> enumValues = getEnumValues(request, entry,
                                                  col);
            if (enumValues != null) {
                for (TwoFacedObject tfo : enumValues) {
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

                for (TwoFacedObject tfo : enumValues) {
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


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void addToProcessingForm(Request request, Entry entry,
                                    Appendable sb)
            throws Exception {
        super.addToProcessingForm(request, entry, sb);
        StringBuilder inner = new StringBuilder();
        inner.append(HtmlUtils.formTable());
        getSearchFormInner(request, entry, inner, false);
        inner.append(HtmlUtils.formTableClose());
        org.ramadda.data.services.PointFormHandler.formGroup(request, sb,
                "Database Search",
                HtmlUtils.makeShowHideBlock("", inner.toString(), false));
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public StringBuilder getSearchForm(Request request, Entry entry)
            throws Exception {

        StringBuilder sb     = new StringBuilder();
        String formUrl       =
            request.makeUrl(getRepository().URL_ENTRY_SHOW);
        String        formId = HtmlUtils.getUniqueId("form_");
        sb.append(HtmlUtils.formPost(formUrl, HtmlUtils.id(formId)));
        sb.append(HtmlUtils.hidden(ARG_ENTRYID, entry.getId()));
        sb.append(HtmlUtils.formTable());
        String buttons = HtmlUtils.submit(msg("Search"), ARG_DB_SEARCH)
                         + HtmlUtils.space(2)
                         + HtmlUtils.submit(msg("Cancel"), ARG_DB_LIST);

        sb.append(formEntry(request, "", buttons));
        getSearchFormInner(request, entry, sb, true);
        sb.append(formEntry(request, "", buttons));
        sb.append(HtmlUtils.formTableClose());
        StringBuilder js = new StringBuilder();
        js.append("HtmlUtil.initSelect('.search-select');\n");
        HtmlUtils.script(sb, js.toString());
        sb.append(HtmlUtils.formClose());
        OutputHandler.addUrlShowingForm(sb, entry, formId,
                                        "[\".*OpenLayers_Control.*\"]",
                                        request.isAnonymous()
                                        ? null
                                        : "dbAddUrlShowingForm");


        return sb;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param sb _more_
     * @param normalForm _more_
     *
     * @throws Exception _more_
     */
    private void getSearchFormInner(Request request, Entry entry,
                                    Appendable sb, boolean normalForm)
            throws Exception {

        sb.append(formEntry(request, "", HtmlUtils.div("Search For",HtmlUtils.cssClass("ramadda-form-header"))));
        DbInfo        dbInfo   = getDbInfo();
        StringBuilder advanced = new StringBuilder();
        List<Clause>  where    = new ArrayList<Clause>();
        for (Column column : getColumns(true)) {
            if ( !normalForm && column.isType(column.DATATYPE_LATLON)) {
                continue;
            }
            if (column.getCanSearch()) {
                if (column.getAdvancedSearch()) {
                    column.addToSearchForm(request, advanced, where, entry);
                } else {
                    column.addToSearchForm(request, sb, where, entry);
                }
            }
        }



        String count = msgLabel("Count") + " "
                       + HtmlUtils.input(ARG_MAX, getMax(request),
                                         HtmlUtils.SIZE_5
                                         + HtmlUtils.attr("default",
                                             "" + DEFAULT_MAX));
        List<TwoFacedObject> tfos    = new ArrayList<TwoFacedObject>();
        List<TwoFacedObject> aggtfos = new ArrayList<TwoFacedObject>();
        tfos.add(new TwoFacedObject("----", ""));
        aggtfos.add(new TwoFacedObject("----", ""));
        for (Column column : dbInfo.getColumnsToUse()) {
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
            if (tfos.size() > 0) {
                sb.append(formEntry(request, "", HtmlUtils.div("Group By",HtmlUtils.cssClass("ramadda-form-header"))));
                sb.append(
                    formEntry(
                        request, msgLabel("Group By"),
                        HtmlUtils.select(
                            ARG_GROUPBY, tfos,
                            (List<String>) request.get(
                                ARG_GROUPBY,
                                new ArrayList()), " multiple size=4 ")));

                List<TwoFacedObject> aggTypes =
                    new ArrayList<TwoFacedObject>();
                aggTypes.add(new TwoFacedObject("----", ""));
                aggTypes.add(new TwoFacedObject("Sum", "sum"));
                aggTypes.add(new TwoFacedObject("Count", "count"));
                aggTypes.add(new TwoFacedObject("Average", "avg"));
                aggTypes.add(new TwoFacedObject("Min", "min"));
                aggTypes.add(new TwoFacedObject("Max", "max"));

                StringBuilder aggSB = new StringBuilder();
                for (int i = 0; i < 3; i++) {
                    aggSB.append(
                        HtmlUtils.select(
                            ARG_AGG
                            + i, aggtfos, request.getString(
                                ARG_AGG + i, ""), HtmlUtils.cssClass(
                                "search-select")) + HtmlUtils.space(2)
                                    + HtmlUtils.select(
                                        ARG_AGG_TYPE
                                        + i, aggTypes, request.getString(
                                            ARG_AGG_TYPE
                                            + i, ""), HtmlUtils.cssClass(
                                                "search-select")));
                    aggSB.append("<br>");
                }
                aggSB.append(HtmlUtils.checkbox(ARG_AGG_PERCENT, "true",
                        request.get(ARG_AGG_PERCENT,
                                    false)) + " show percentage");
                sb.append(formEntry(request, msgLabel("Aggregate"),
                                    aggSB.toString()));

            }
        }

        sb.append(formEntry(request, "", HtmlUtils.div("Options",HtmlUtils.cssClass("ramadda-form-header"))));
        StringBuilder viewSB      = new StringBuilder();
        boolean       defaultShow = false;
        viewSB.append(HtmlUtils.checkbox(ARG_DB_SHOW + "toggleall", "true",
                                         false) + " " + "Toggle All");
        viewSB.append("<br>");
        for (Column column : dbInfo.getColumnsToUse()) {
            String arg = ARG_DB_SHOW + "_" + column.getName();
            viewSB.append(HtmlUtils.checkbox(arg, "true",
                                             request.get(arg,
                                                 defaultShow)) + " "
                                                     + column.getLabel());
            viewSB.append("<br>");
        }
        sb.append(formEntry(request, msgLabel("Display"),
                            HtmlUtils.makeShowHideBlock("",
                                viewSB.toString(), false)));


        sb.append(HtmlUtils.script("dbToggleAllInit();"));

        if (aggtfos.size() > 0) {
            String orderBy = "";
            String dir = request.getString(ARG_DB_SORTDIR,
                                           dbInfo.getDfltSortAsc()
                                           ? "asc"
                                           : "desc");
            if (dbInfo.getDfltSortColumn() != null) {
                orderBy = dbInfo.getDfltSortColumn().getName();
            }
            sb.append(
                formEntry(
                    request, msgLabel("Order By"),
                    HtmlUtils.select(
                        ARG_DB_SORTBY, aggtfos,
                        request.getString(ARG_DB_SORTBY, orderBy),
                        HtmlUtils.cssClass(
                            "search-select")) + HtmlUtils.space(2)
                                + HtmlUtils.radio(
                                    ARG_DB_SORTDIR, "asc", dir.equals("asc"),
                                    " default='asc' ") + " Ascending "
                                        + HtmlUtils.radio(
                                            ARG_DB_SORTDIR, "desc",
                                            dir.equals("desc"),
                                            " default='asc' ") + " Descending"));

        }
        if (normalForm) {
            sb.append(
                HtmlUtils.formEntry(
                    msgLabel("Search Type"),
                    HtmlUtils.checkbox(
                        ARG_DB_OR, "true",
                        request.get(ARG_DB_OR, false)) + " "
                            + msg("Use OR logic")));


            sb.append(
                formEntry(
                    request, msgLabel("View As"),
                    HtmlUtils.select(
                        ARG_DB_VIEW, viewList,
                        request.getString(ARG_DB_VIEW, ""),
                        HtmlUtils.attr("default", VIEW_TABLE)
                        + HtmlUtils.cssClass(
                            "search-select")) + HtmlUtils.space(2) + count));
            String suffix = getAccessManager().canEditEntry(request, entry)
                            ? HtmlUtils.space(2)
                              + HtmlUtils.checkbox(ARG_DB_DOSAVESEARCH,
                                  "true",
                                  request.get(ARG_DB_DOSAVESEARCH,
                                      false)) + " Save Search"
                            : "";

            sb.append(formEntry(request, msgLabel("Search Name"),
                                HtmlUtils.input(ARG_DB_SEARCHNAME,
                                    request.getString(ARG_DB_SEARCHNAME, ""),
                                    HtmlUtils.SIZE_50) + suffix));
        }


        /*
        if (false && request.getUser().getAdmin()) {
            advanced.append(
                formEntry(
                    request, "",
                    HtmlUtils.checkbox(ARG_DB_ALL, "true", false) + " "
                    + msg("Search across all databases")));
        }
        */
        /*
          advanced.append(HtmlUtils.formTableClose());
          sb.append("<tr><td colspan=3>");
          HtmlUtils.makeAccordian(sb, msg("Advanced..."),
          HtmlUtils.inset(advanced.toString(), 0, 20,
          10, 0));
          sb.append("</td></tr>");
        */

        sb.append(advanced);
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result handleSearchForm(Request request, Entry entry)
            throws Exception {
        StringBuilder sb = new StringBuilder();
        addViewHeader(request, entry, sb, VIEW_SEARCH, 0, false);
        sb.append(getSearchForm(request, entry));
        addViewFooter(request, entry, sb);

        return new Result(getTitle(request, entry), sb);
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result handleSearch(Request request, Entry entry)
            throws Exception {
        boolean canEdit = getAccessManager().canEditEntry(request, entry);
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
                                             METADATA_SAVEDSEARCH, false,
                                             name, args, null, null, null);
            getMetadataManager().addMetadata(entry, metadata);
            request.put(ARG_DB_SEARCHID, metadata.getId());
            getEntryManager().updateEntry(request, entry);
        }



        if (request.exists(ARG_DB_SEARCHID)) {
            List<Metadata> savedSearchMetadata =
                getMetadataManager().getMetadata(entry, METADATA_SAVEDSEARCH);
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
                        value = HtmlUtils.urlDecode(value);
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

        for (Column column : getColumns()) {
            column.assembleWhereClause(request, where, searchCriteria);
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

        return handleList(request, entry, mainClause, "", true);
    }





    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result handleDeleteConfirm(Request request, Entry entry)
            throws Exception {
        String    action = request.getString(ARG_DB_ACTION, ACTION_DELETE);
        boolean   deleteSelected = action.equals(ACTION_DELETE);
        List      dbids = request.get(ARG_DBID_SELECTED, new ArrayList());
        Statement statement      = getDatabaseManager().createStatement();


        try {
            if (deleteSelected) {
                for (Object dbid : dbids) {
                    String query =
                        SqlUtil.makeDelete(tableHandler.getTableName(),
                                           COL_DBID,
                                           SqlUtil.quote(dbid.toString()));
                    statement.execute(query);
                }
            } else {
                String query = SqlUtil.makeDelete(
                                   tableHandler.getTableName(), COL_ID,
                                   SqlUtil.quote(entry.getId().toString()));
                statement.execute(query);
            }
        } finally {
            getRepository().getDatabaseManager().closeAndReleaseConnection(
                statement);
        }




        String url =
            HtmlUtils.url(request.makeUrl(getRepository().URL_ENTRY_SHOW),
                          new String[] { ARG_ENTRYID,
                                         entry.getId(), ARG_MESSAGE,
                                         "Entries deleted" });

        return new Result(url);
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param action _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result handleDeleteAsk(Request request, Entry entry, String action)
            throws Exception {
        StringBuilder sb    = new StringBuilder();
        List          dbids = request.get(ARG_DBID_SELECTED, new ArrayList());

        if (request.exists(ARG_DB_DELETE)) {
            action = ACTION_DELETE;
        }
        boolean deleteSelected = action.equals(ACTION_DELETE);
        if (deleteSelected && (dbids.size() == 0)) {
            sb.append(
                getPageHandler().showDialogWarning(
                    msg("No entries were selected")));
        } else {
            String formUrl = request.makeUrl(getRepository().URL_ENTRY_SHOW);
            sb.append(HtmlUtils.formPost(formUrl));
            sb.append(HtmlUtils.hidden(ARG_ENTRYID, entry.getId()));
            sb.append(HtmlUtils.hidden(ARG_DB_ACTION, action));


            for (Object dbid : dbids) {
                sb.append(HtmlUtils.hidden(ARG_DBID_SELECTED,
                                           dbid.toString()));
            }

            String msg =
                "Are you sure you want to delete the selected entries?";
            if ( !deleteSelected) {
                msg = "Are you sure you want to delete the entire database?";
            }
            addViewHeader(request, entry, sb, "", 0, false);
            sb.append(getPageHandler().showDialogQuestion(msg(msg),
                    HtmlUtils.submit(msg("Yes"), ARG_DB_DELETECONFIRM)
                    + HtmlUtils.space(2)
                    + HtmlUtils.submit(msg("Cancel"), ARG_DB_LIST)));
            addViewFooter(request, entry, sb);
        }

        sb.append(HtmlUtils.formClose());

        return new Result(getTitle(request, entry), sb);
    }


    /**
     * _more_
     *
     * @param prop _more_
     * @param values _more_
     * @param obj _more_
     *
     * @throws Exception _more_
     */
    private void putProp(String prop, Object[] values, Object obj)
            throws Exception {
        Hashtable props = getProps(values);
        if (props == null) {
            props = new Hashtable();
        }
        props.put(prop, obj);
        values[IDX_DBPROPS] = xmlEncoder.toXml(props);
    }

    /**
     * _more_
     *
     * @param prop _more_
     * @param values _more_
     * @param dflt _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
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



    /**
     * _more_
     *
     * @param prop _more_
     * @param values _more_
     * @param dflt _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
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

    /**
     * _more_
     *
     * @param values _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Hashtable getProps(Object[] values) throws Exception {
        String value = (String) values[IDX_DBPROPS];
        if ((value == null) || (value.length() == 0)) {
            return null;
        }

        return (Hashtable) xmlEncoder.toObject(value);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param source _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result handleBulkUpload(final Request request, final Entry entry,
                                   InputStream source)
            throws Exception {

        String delimiter = request.getString(ARG_DB_BULK_DELIMITER, ",");
        int                       skip = request.get(ARG_DB_BULK_SKIP, 1);
        final DbInfo              dbInfo     = getDbInfo();
        final ArrayList<Object[]> valueList  = new ArrayList<Object[]>();
        final int[]               cnt        = { 0 };
        TextReader                textReader = new TextReader();
        final int[]               scnt       = { 0 };
        final Bounds bounds = new Bounds(Double.NaN, Double.NaN, Double.NaN,
                                         Double.NaN);

        textReader.setInput(new BufferedInputStream(source));
        textReader.setSkip(skip);
        textReader.setDelimiter(delimiter);
        Processor.ProcessorGroup myProcessor =
            new Processor.ProcessorGroup() {
            @Override
            public org.ramadda.util.text.Row processRow(
                    TextReader textReader, org.ramadda.util.text.Row row,
                    String line) {
                try {
                    Object[] values = tableHandler.makeEntryValueArray();
                    cnt[0]++;
                    initializeValueArray(request, null, values);

                    List<String> toks = row.getValues();
                    if (toks.size() != dbInfo.getColumnsToUse().size()) {
                        System.err.println("bad count: " + toks.size()
                                           + " line length:" + ((line != null)
                                ? "" + line.length()
                                : "null") + " " + toks);
                        System.err.println("line:" + line);

                        throw new IllegalArgumentException(
                            "Wrong number of values. Given line has: "
                            + toks.size() + " Expected:"
                            + dbInfo.getColumnsToUse().size() + "<br>"
                            + line);
                    }
                    String keyValue = null;

                    for (int colIdx = 0; colIdx < toks.size(); colIdx++) {
                        Column column = dbInfo.getColumnsToUse().get(colIdx);
                        String value  = (String) toks.get(colIdx).trim();
                        if (column == dbInfo.getKeyColumn()) {
                            keyValue = value;
                        }
                        try {
                            column.setValue(entry, values, value);
                        } catch (Exception exc) {
                            throw new IllegalArgumentException(
                                "Error setting column value:"
                                + column.getName() + "\nvalues:" + toks
                                + "\nError:" + exc);
                        }
                    }



                    if (dbInfo.getHasLocation()) {
                        double[] ll  = getLocation(values);
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
                        if (false) {
                            for (Object[] tuple : valueList) {
                                scnt[0]++;
                                if ((scnt[0] % 1000) == 0) {
                                    System.err.println(
                                        "DbTypeHandler.bulkUpload: stored: "
                                        + scnt[0]);
                                }
                                doStore(entry, tuple, true);
                            }
                        } else {
                            scnt[0] += valueList.size();
                            long t1 = System.currentTimeMillis();
                            doStore(entry, valueList, true);
                            long t2 = System.currentTimeMillis();
                            Utils.printTimes(
                                "DbTypeHandler.bulkUpload: stored: "
                                + scnt[0], t1, t2);
                        }
                        valueList.clear();
                    }

                    return row;
                } catch (Exception exc) {
                    throw new RuntimeException(exc);
                }
            }
        };
        textReader.setProcessor(myProcessor);
        CsvUtil csvUtil = new CsvUtil(new ArrayList<String>());
        csvUtil.process(textReader);

        if (false) {
            for (Object[] tuple : valueList) {
                scnt[0]++;
                if ((scnt[0] % 1000) == 0) {
                    System.err.println("DbTypeHandler.bulkUpload: stored: "
                                       + scnt[0]);
                }
                doStore(entry, tuple, true);
            }
        } else {
            scnt[0] += valueList.size();
            doStore(entry, valueList, true);
            System.err.println("DbTypeHandler.bulkUpload: stored: "
                               + scnt[0]);
        }

        if ( !entry.hasAreaDefined() && !Double.isNaN(bounds.getNorth())) {
            entry.setBounds(bounds);
            getEntryManager().updateEntry(request, entry);
        }
        //Remove these so any links that get made with the request don't point to the BULK upload
        request.remove(ARG_DB_NEWFORM);
        request.remove(ARG_DB_BULK_TEXT);
        request.remove(ARG_DB_BULK_FILE);
        request.remove(ARG_DB_BULK_LOCALFILE);

        tableHandler.clearCache();

        if (valueList.size() < 1000) {
            return handleListTable(request, entry, valueList, false, false);
        } else {
            StringBuilder sb = new StringBuilder();
            getPageHandler().entrySectionOpen(request, entry, sb, "Upload",
                    true);
            sb.append("Added " + scnt[0] + " entries");
            getPageHandler().entrySectionClose(request, entry, sb);

            return new Result("", sb);

        }

    }

    /**
     * _more_
     *
     * @param request _more_
     * @param checkCanList _more_
     *
     * @return _more_
     */
    List<Column> getColumnsToUse(Request request, boolean checkCanList) {
        List<Column> columns         = dbInfo.getColumnsToUse();
        List<Column> selectedColumns = new ArrayList<Column>();
        for (Column column : columns) {
            String arg = ARG_DB_SHOW + "_" + column.getName();
            if (request.get(arg, false)) {
                selectedColumns.add(column);
            }
        }
        if ((selectedColumns.size() > 0)
                && (selectedColumns.size() != columns.size())) {
            return selectedColumns;
        }
        if (checkCanList) {
            selectedColumns = new ArrayList<Column>();
            for (Column column : columns) {
                if (column.getCanList()) {
                    selectedColumns.add(column);
                }
            }

            return selectedColumns;
        }

        return columns;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param dbid _more_
     * @param fromAnonForm _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result handleNewOrEdit(Request request, Entry entry, String dbid,
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
            InputStream source = null;
            String      bulkContent;

            if (request.exists(ARG_DB_BULK_FILE)) {
                File f = new File(request.getUploadedFile(ARG_DB_BULK_FILE));
                if ( !f.exists()) {
                    throw new IllegalArgumentException(
                        "Uploaded file does not exist");
                }
                if (f.toString().toLowerCase().endsWith(".xls")) {
                    source = new ByteArrayInputStream(
                        XlsUtil.xlsToCsv(f.toString()).getBytes());
                } else if (f.toString().toLowerCase().endsWith(".xlsx")) {
                    source = new ByteArrayInputStream(
                        XlsUtil.xlsxToCsv(f.toString()).getBytes());
                } else {
                    source = getStorageManager().getFileInputStream(f);
                }
            } else if (request.exists(ARG_DB_BULK_LOCALFILE)) {
                request.ensureAdmin();
                source = getStorageManager().getFileInputStream(
                    new File(
                        request.getString(ARG_DB_BULK_LOCALFILE, "").trim()));
            } else {
                source = new ByteArrayInputStream(
                    request.getString(ARG_DB_BULK_TEXT, "").getBytes());
            }

            return handleBulkUpload(request, entry, source);
        }


        StringBuilder sb       = new StringBuilder();
        List<String>  colNames = tableHandler.getColumnNames();
        Object[]      values   = getValues(entry, dbid);
        initializeValueArray(request, dbid, values);
        for (Column column : getColumns()) {
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
            HtmlUtils.url(request.makeUrl(getRepository().URL_ENTRY_SHOW),
                          new String[] {
            ARG_ENTRYID, entry.getId(), ARG_DBID, (String) values[IDX_DBID],
            ARG_DB_EDITFORM, "true"
        });

        return new Result(url);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param dbid _more_
     * @param values _more_
     */
    protected void initializeValueArray(Request request, String dbid,
                                        Object[] values) {
        initializeValueArray(request, dbid, request.getUser().getId(),
                             values);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param dbid _more_
     * @param user _more_
     * @param values _more_
     */
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

    /**
     * _more_
     *
     * @param entry _more_
     * @param values _more_
     * @param isNew _more_
     *
     * @throws Exception _more_
     */
    protected void doStore(Entry entry, Object[] values, boolean isNew)
            throws Exception {
        List valueList = new ArrayList<Object[]>();
        valueList.add(values);
        doStore(entry, valueList, isNew);
    }

    /**
     * _more_
     *
     * @param entry _more_
     * @param valueList _more_
     * @param isNew _more_
     *
     * @throws Exception _more_
     */
    protected void doStore(Entry entry, List<Object[]> valueList,
                           boolean isNew)
            throws Exception {
        if (valueList.size() == 0) {
            return;
        }
        String            dbid       = (String) valueList.get(0)[IDX_DBID];
        String            sql        = makeInsertOrUpdateSql(entry, (isNew
                ? null
                : dbid));
        Connection        connection = getDatabaseManager().getConnection();
        PreparedStatement stmt       = connection.prepareStatement(sql);
        connection.setAutoCommit(false);
        //            getRepository().getDatabaseManager().getPreparedStatement(sql);

        try {
            for (Object[] values : valueList) {
                int stmtIdx = tableHandler.setStatement(entry, values, stmt,
                                  isNew);
                if ( !isNew) {
                    stmt.setString(stmtIdx, dbid);
                }
                stmt.execute();
            }
            connection.commit();
            connection.setAutoCommit(true);
        } finally {
            getRepository().getDatabaseManager().closeAndReleaseConnection(
                stmt);
        }

    }



    /**
     * _more_
     *
     * @param entry _more_
     * @param dbid _more_
     *
     * @return _more_
     */
    public Clause makeClause(Entry entry, String dbid) {
        return Clause.and(Clause.eq(COL_ID, entry.getId()),
                          Clause.eq(COL_DBID, dbid));

    }


    /**
     * _more_
     *
     * @param entry _more_
     * @param dbid _more_
     *
     * @return _more_
     */
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


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param valueList _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result handleListEmail(Request request, Entry entry,
                                  List<Object[]> valueList)
            throws Exception {

        Column theColumn = null;
        for (Column column : getColumns()) {
            if (column.getType().equals(Column.DATATYPE_EMAIL)) {
                theColumn = column;
            }
        }
        if (theColumn == null) {
            throw new IllegalStateException("No email data found");
        }
        StringBuilder sb = new StringBuilder();
        makeForm(request, entry, sb);
        sb.append(HtmlUtils.hidden(ARG_DB_ACTION, ACTION_EMAIL));
        sb.append(HtmlUtils.submit(msg("Send Message")));
        sb.append(HtmlUtils.space(2));
        sb.append(HtmlUtils.submit(msg("Cancel"), ARG_DB_LIST));
        sb.append(HtmlUtils.formTable());

        for (Object[] values : valueList) {
            String toId = (String) values[IDX_DBID];
            sb.append(HtmlUtils.hidden(ARG_DBID_SELECTED, toId));
        }


        sb.append(HtmlUtils.hidden(ARG_ENTRYID, entry.getId()));
        sb.append(formEntry(request, msgLabel("From name"),
                            HtmlUtils.input(ARG_EMAIL_FROMNAME,
                                            request.getUser().getName(),
                                            HtmlUtils.SIZE_40)));
        sb.append(formEntry(request, msgLabel("From email"),
                            HtmlUtils.input(ARG_EMAIL_FROMADDRESS,
                                            request.getUser().getEmail(),
                                            HtmlUtils.SIZE_40)));
        String bcc = HtmlUtils.checkbox(ARG_EMAIL_BCC, "true", false)
                     + HtmlUtils.space(1) + msg("Send as BCC");

        sb.append(
            formEntry(
                request, msgLabel("Subject"),
                HtmlUtils.input(ARG_EMAIL_SUBJECT, "", HtmlUtils.SIZE_40)
                + HtmlUtils.space(2) + bcc));
        sb.append(
            HtmlUtils.formEntryTop(
                msgLabel("Message"),
                HtmlUtils.textArea(ARG_EMAIL_MESSAGE, "", 30, 60)));
        sb.append(HtmlUtils.formTableClose());
        sb.append(HtmlUtils.submit(msg("Send Message")));


        sb.append(HtmlUtils.formTableClose());
        sb.append(HtmlUtils.formClose());

        return new Result(getTitle(request, entry), sb);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param valueList _more_
     * @param doGroupBy _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result handleListCsv(Request request, Entry entry,
                                List<Object[]> valueList, boolean doGroupBy)
            throws Exception {
        DbInfo        dbInfo = getDbInfo();
        StringBuilder sb     = new StringBuilder();
        for (int cnt = 0; cnt < valueList.size(); cnt++) {
            Object[] values = valueList.get(cnt);
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

                continue;
            }

            List<Column> columns = getColumnsToUse(request, false);
            for (int i = 0; i < columns.size(); i++) {
                StringBuilder cb = new StringBuilder();
                columns.get(i).formatValue(entry, cb, Column.OUTPUT_CSV,
                            values, true);
                String colValue = cb.toString();
                colValue = colValue.replaceAll("\n", " ");
                if (i > 0) {
                    sb.append(",");
                }
                sb.append(colValue);
            }
            sb.append("\n");
        }

        Result result = new Result("", sb, "text/csv");
        if (request.defined(ARG_DB_SEARCHNAME)) {
            result.setReturnFilename(request.getString(ARG_DB_SEARCHNAME)
                                     + ".csv");
        } else {
            result.setReturnFilename(entry.getName() + ".csv");
        }

        return result;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param valueList _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result handleListJson(Request request, Entry entry,
                                 List<Object[]> valueList)
            throws Exception {
        DbInfo dbInfo = getDbInfo();
        /*
          {
          columns: [
          {name:...,
          label:...
          ],
          results: [
          name:value,
          ]
          }
         */
        List<String> cols    = new ArrayList<String>();
        List<String> results = new ArrayList<String>();
        List<Column> columns = getColumnsToUse(request, false);

        for (int i = 0; i < columns.size(); i++) {
            Column c = columns.get(i);
            cols.add(c.getJson(request));
        }


        StringBuilder cb = new StringBuilder();
        for (int cnt = 0; cnt < valueList.size(); cnt++) {
            Object[]     values = valueList.get(cnt);
            List<String> attrs  = new ArrayList<String>();

            for (int i = 0; i < columns.size(); i++) {
                cb.setLength(0);
                columns.get(i).formatValue(entry, cb, Column.OUTPUT_CSV,
                            values, true);
                String colValue = cb.toString();
                attrs.add(columns.get(i).getName());
                attrs.add(Json.quote(colValue));
            }
            results.add(Json.map(attrs));
        }

        StringBuilder sb    = new StringBuilder();
        List<String>  items = new ArrayList<String>();
        if (request.get("includeColumns", true)) {
            items.add("columns");
            items.add(Json.list(cols));
        }
        items.add("results");
        items.add(Json.list(results));
        Json.map(sb, items, false);

        return new Result("", sb, Json.MIMETYPE);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param valueList _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result handleListRss(Request request, Entry entry,
                                List<Object[]> valueList)
            throws Exception {
        DbInfo        dbInfo = getDbInfo();
        StringBuilder sb     = new StringBuilder();

        sb.append(XmlUtil.XML_HEADER + "\n");
        sb.append(XmlUtil.openTag(RssUtil.TAG_RSS,
                                  XmlUtil.attrs(ATTR_RSS_VERSION, "2.0")));
        sb.append(XmlUtil.openTag(RssUtil.TAG_CHANNEL));
        sb.append(XmlUtil.tag(RssUtil.TAG_TITLE, "", entry.getName()));
        SimpleDateFormat sdf = getDateFormat(entry);
        for (int cnt = 0; cnt < valueList.size(); cnt++) {
            Object[] values = valueList.get(cnt);
            String   label  = getLabel(entry, values, null);
            Date     date   = null;
            if (dbInfo.getDateColumns().size() > 0) {
                date = (Date) values[dbInfo.getDateColumn().getOffset()];
            } else {
                date = (Date) values[IDX_DBCREATEDATE];
            }
            String dbid = (String) values[IDX_DBID];

            String info = getHtml(request, entry, dbid, getColumns(), values,
                                  sdf);
            sb.append(XmlUtil.openTag(RssUtil.TAG_ITEM));
            sb.append(XmlUtil.tag(RssUtil.TAG_PUBDATE, "",
                                  rssSdf.format(date)));
            sb.append(XmlUtil.tag(RssUtil.TAG_TITLE, "", label));


            String url = getViewUrl(request, entry, "" + values[IDX_DBID]);
            url = request.getAbsoluteUrl(url);
            sb.append(XmlUtil.tag(RssUtil.TAG_LINK, "",
                                  XmlUtil.getCdata(url)));


            sb.append(XmlUtil.tag(RssUtil.TAG_GUID, "",
                                  XmlUtil.getCdata(url)));
            sb.append(XmlUtil.openTag(RssUtil.TAG_DESCRIPTION, ""));
            XmlUtils.appendCdata(sb, info);
            sb.append(XmlUtil.closeTag(RssUtil.TAG_DESCRIPTION));
            if (dbInfo.getHasLocation()) {
                double[] ll = getLocation(values);
                sb.append(XmlUtil.tag(RssUtil.TAG_GEOLAT, "", "" + ll[0]));
                sb.append(XmlUtil.tag(RssUtil.TAG_GEOLON, "", "" + ll[1]));
            }
            sb.append(XmlUtil.closeTag(RssUtil.TAG_ITEM));
        }
        sb.append(XmlUtil.closeTag(RssUtil.TAG_CHANNEL));
        sb.append(XmlUtil.closeTag(RssUtil.TAG_RSS));

        return new Result("", sb, "application/rss+xml");
    }



    /**
     * _more_
     *
     * @param values _more_
     *
     * @return _more_
     */
    private double[] getLocation(Object[] values) {
        DbInfo dbInfo = getDbInfo();
        if (dbInfo.getLatLonColumn() != null) {
            return dbInfo.getLatLonColumn().getLatLon(values);
        } else if ((dbInfo.getLatLonColumn() != null)
                   && (dbInfo.getLonColumn() != null)) {
            return new double[] { dbInfo.getLatColumn().getDouble(values),
                                  dbInfo.getLonColumn().getDouble(values) };
        }

        return null;

    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param values _more_
     * @param rowId _more_
     * @param divId _more_
     *
     * @return _more_
     */
    public String getEventJS(Request request, Entry entry, Object[] values,
                             String rowId, String divId) {
        String xmlUrl = getViewUrl(request, entry, "" + values[IDX_DBID])
                        + "&result=xml";
        rowId = HtmlUtils.squote(rowId);
        divId = HtmlUtils.squote(divId);
        String popupId   = HtmlUtils.squote("dbrowpopup_" + entry.getId());
        StringBuilder sb = new StringBuilder();
        sb.append(HtmlUtils.onMouseOver(HtmlUtils.call("dbRowOver", rowId)));
        sb.append(HtmlUtils.onMouseOut(HtmlUtils.call("dbRowOut", rowId)));
        sb.append(HtmlUtils.onMouseClick(HtmlUtils.call("dbRowClick",
                HtmlUtils.comma("event", rowId, popupId,
                                HtmlUtils.squote(xmlUrl)))));

        return sb.toString();
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param valueList _more_
     * @param fromSearch _more_
     * @param showHeaderLinks _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result handleListTable(Request request, Entry entry,
                                  List<Object[]> valueList,
                                  boolean fromSearch, boolean showHeaderLinks)
            throws Exception {
        return handleListTable(request, entry, valueList, fromSearch,
                               showHeaderLinks, new StringBuilder());
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param valueList _more_
     * @param fromSearch _more_
     * @param showHeaderLinks _more_
     * @param sb _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result handleListTable(Request request, Entry entry,
                                  List<Object[]> valueList,
                                  boolean fromSearch,
                                  boolean showHeaderLinks, Appendable sb)
            throws Exception {
        List<String> links = new ArrayList<String>();

        /*
        if (showHeaderLinks) {
            if (showInHeader(VIEW_STICKYNOTES)) {
                //                links.add(getHref(request, entry, VIEW_STICKYNOTES,msg("Sticky Notes")));
            }
            if (showInHeader(VIEW_RSS)) {
                links.add(getHref(request, entry, VIEW_RSS, msg("RSS"),
                                  "/" + entry.getName() + ".rss"));
            }
            if (showInHeader(VIEW_CSV)) {
                links.add(getHref(request, entry, VIEW_CSV, msg("CSV"),
                                  "/" + entry.getName() + ".csv"));
            }
            if (showInHeader(VIEW_JSON)) {
                links.add(getHref(request, entry, VIEW_JSON, msg("JSON"),
                                  "/" + entry.getName() + ".json"));
            }
        }
        */
        boolean embedded = request.get(ARG_EMBEDDED, false);
        if ( !embedded) {
            addViewHeader(request, entry, sb, VIEW_TABLE, valueList.size(),
                          fromSearch,
                          "" /*StringUtil.join("&nbsp;|&nbsp;", links)*/);
        } else {
            addStyleSheet(sb);
        }
        boolean doGroupBy = isGroupBy(request);
        if (doGroupBy) {
            makeGroupByTable(request, entry, valueList, sb,
                             showHeaderLinks
                             && !request.get(ARG_EMBEDDED, false));
        } else {
            makeTable(request, entry, valueList, fromSearch, sb,
                      !request.isAnonymous(),
                      showHeaderLinks && !request.get(ARG_EMBEDDED, false));
        }

        if ( !embedded) {
            addViewFooter(request, entry, sb);
        }

        return new Result(getTitle(request, entry), sb);
    }


    /**
     * _more_
     *
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    private void addStyleSheet(Appendable sb) throws Exception {
        HtmlUtils.cssLink(sb,
                          getPageHandler().makeHtdocsUrl("/db/dbstyle.css"));
        HtmlUtils.importJS(sb, getPageHandler().makeHtdocsUrl("/db/db.js"));
    }

    /**
     * _more_
     *
     * @param sb _more_
     * @param contents _more_
     * @param extraAttrs _more_
     *
     * @throws Exception _more_
     */
    private void makeTableHeader(Appendable sb, String contents,
                                 String... extraAttrs)
            throws Exception {
        StringBuilder attrs = new StringBuilder();
        HtmlUtils.cssClass(attrs, CSS_DB_TABLEHEADER);
        for (String attr : extraAttrs) {
            attrs.append(" ");
            attrs.append(attr);
        }
        HtmlUtils.col(
            sb, HtmlUtils.div(
                contents, HtmlUtils.cssClass(
                    CSS_DB_TABLEHEADER_INNER)), attrs.toString());
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param valueList _more_
     * @param fromSearch _more_
     * @param sb _more_
     * @param doForm _more_
     * @param showHeaderLinks _more_
     *
     * @throws Exception _more_
     */
    public void makeTable(Request request, Entry entry,
                          List<Object[]> valueList, boolean fromSearch,
                          Appendable sb, boolean doForm,
                          boolean showHeaderLinks)
            throws Exception {

        DbInfo           dbInfo       = getDbInfo();
        List<Column>     columnsToUse = getColumnsToUse(request, true);
        SimpleDateFormat sdf          = getDateFormat(entry);
        Hashtable        entryProps   = getProperties(entry);

        StringBuilder    chartJS      = new StringBuilder();
        StringBuilder    hb           = new StringBuilder();
        if (doForm) {
            String formUrl = request.makeUrl(getRepository().URL_ENTRY_SHOW);
            hb.append(HtmlUtils.form(formUrl));
            hb.append(HtmlUtils.hidden(ARG_ENTRYID, entry.getId()));
        }
        boolean canEdit = getAccessManager().canEditEntry(request, entry);
        HashSet<String> except = new HashSet<String>();
        except.add(ARG_DB_SORTBY);
        except.add(ARG_DB_SORTDIR);

        String baseUrl = request.getUrl(except, null);
        boolean asc = request.getString(ARG_DB_SORTDIR,
                                        (dbInfo.getDfltSortAsc()
                                         ? "asc"
                                         : "desc")).equals("asc");
        String sortBy = request.getString(ARG_DB_SORTBY,
                                          ((dbInfo.getDfltSortColumn()
                                            == null)
                                           ? ""
                                           : dbInfo.getDfltSortColumn()
                                               .getName()));

        if (valueList.size() > 0) {
            List<TwoFacedObject> actions = new ArrayList<TwoFacedObject>();
            //TODO uncomment            if(dbInfo.getHasEmail() && getRepository().getAdmin().isEmailCapable()) {
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
                    hb.append(HtmlUtils.submit(msgLabel("Do"), ARG_DB_DO));
                    hb.append(HtmlUtils.select(ARG_DB_ACTION, actions));
                }
            }
            HtmlUtils.open(hb, "table", "class", "dbtable", "border", "1",
                           "cellspacing", "0", "cellpadding", "0", "width",
                           "100%");
            HtmlUtils.open(hb, "tr", "valign", "top");
            makeTableHeader(hb, "&nbsp;");
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
                //                GoogleChart.DataTable.addColumn(chartJS, type, column.getLabel());

                String label = column.getLabel();
                if ( !showHeaderLinks) {
                    makeTableHeader(hb, label);

                    continue;
                }
                String sortColumn = column.getName();
                String extra;
                if (sortColumn.equals(sortBy)) {
                    if (asc) {
                        extra = " "
                                + HtmlUtils.img(
                                    getRepository().getIconUrl(ICON_UPDART));
                    } else {
                        extra = " "
                                + HtmlUtils.img(
                                    getRepository().getIconUrl(
                                        ICON_DOWNDART));
                    }
                    asc = !asc;
                } else {
                    extra = " "
                            + HtmlUtils.img(
                                getRepository().getIconUrl(ICON_BLANK), "",
                                HtmlUtils.attr("width", "10"));
                }

                String link = HtmlUtils.href(baseUrl + "&" + ARG_DB_SORTBY
                                             + "=" + sortColumn + "&"
                                             + ARG_DB_SORTDIR + (asc
                        ? "=asc"
                        : "=desc"), label) + extra;
                makeTableHeader(hb, link);
            }
            HtmlUtils.close(hb, "tr");
        }


        Hashtable<String, Hashtable<Object, Integer>> uniques =
            new Hashtable<String, Hashtable<Object, Integer>>();

        String popupId = "dbrowpopup_" + entry.getId();
        hb.append(HtmlUtils.div("",
                                HtmlUtils.id(popupId)
                                + HtmlUtils.cssClass("ramadda-popup")));



        double[] sum  = new double[columnsToUse.size()];
        double[] min  = new double[columnsToUse.size()];
        double[] max  = new double[columnsToUse.size()];


        boolean  even = true;
        for (int cnt = 0; cnt < valueList.size(); cnt++) {
            Object[] values = valueList.get(cnt);

            String   dbid   = (String) values[IDX_DBID];
            String   cbxId  = ARG_DBID + (cnt);
            String   rowId  = "row_" + dbid;
            String   divId  = "div_" + dbid;
            String   event  = getEventJS(request, entry, values, rowId,
                                         divId);
            hb.append("\n");
            hb.append(HtmlUtils.open(HtmlUtils.TAG_TR, "valign", "top",
                                     "class", (even
                    ? " ramadda-row-even "
                    : " ramadda-row-odd ") + " dbrow ", "id", rowId));

            even = !even;
            HtmlUtils.open(hb, "td", "width", "10", "style",
                           "white-space:nowrap;");
            HtmlUtils.open(hb, "div", "class", "ramadda-db-div", "id", divId);
            if (doForm) {
                String call =
                    HtmlUtils.attr(
                        HtmlUtils.ATTR_ONCLICK,
                        HtmlUtils.call(
                            "checkboxClicked",
                            HtmlUtils.comma(
                                "event", HtmlUtils.squote(ARG_DBID_SELECTED),
                                HtmlUtils.squote(cbxId))));

                hb.append(HtmlUtils.checkbox(ARG_DBID_SELECTED, dbid, false,
                                             HtmlUtils.id(cbxId) + call));
            }
            if (canEdit) {
                String editUrl = getEditUrl(request, entry, dbid);
                hb.append(
                    HtmlUtils.href(
                        editUrl,
                        HtmlUtils.img(
                            getRepository().getUrlBase()
                            + "/db/database_edit.png", msg("Edit entry"))));
            }

            String viewUrl = getViewUrl(request, entry, dbid);
            hb.append(
                HtmlUtils.href(
                    viewUrl,
                    HtmlUtils.img(
                        getRepository().getUrlBase() + "/db/database_go.png",
                        msg("View entry"))));

            HtmlUtils.close(hb, "div", "td");

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
                    HtmlUtils.open(hb, "td",
                                   event
                                   + HtmlUtils.attr("class", "dbtablecell"));
                } else if (column.isNumeric()) {
                    HtmlUtils.open(hb, "td", event + "align=right");
                } else {
                    HtmlUtils.open(hb, "td", event);
                }

                HtmlUtils.open(hb, HtmlUtils.TAG_DIV, "class", "dbtablecell");

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
                                        getDbIconUrl(icon), "",
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
                            hb.append(prefix.toString());
                        }
                    }
                }



                String label = formatTableValue(request, entry, hb, column,
                                   values, sdf);
                hb.append("&nbsp;");
                HtmlUtils.close(hb, HtmlUtils.TAG_DIV);
                HtmlUtils.close(hb, HtmlUtils.TAG_TD);


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
            hb.append("</tr>");
        }


        if (valueList.size() > 0) {
            HtmlUtils.comment(hb, "summmary");
            HtmlUtils.open(hb, "tr", "valign", "top");
            HtmlUtils.tag(hb, "td", HtmlUtils.attrs("align", "right"),
                          "#" + valueList.size());
            for (int i = 0; i < columnsToUse.size(); i++) {
                Column column = columnsToUse.get(i);
                if (column.isNumeric() && column.getDoStats()) {
                    double  avg   = sum[i] / valueList.size();
                    boolean round = column.isInteger();
                    HtmlUtils.open(hb, "td", "class", "dbtable-summary");
                    hb.append(HtmlUtils.formTable());
                    hb.append(HtmlUtils.formEntry("Average:",
                            format(avg, round)));
                    hb.append(HtmlUtils.formEntry("Minimum:",
                            format(min[i], round)));
                    hb.append(HtmlUtils.formEntry("Maximum:",
                            format(max[i], round)));
                    hb.append(HtmlUtils.formEntry("Total:",
                            format(sum[i], round)));
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
            HtmlUtils.close(hb, "table");
        } else {
            if ( !fromSearch) {
                hb.append(HtmlUtils.br());
                hb.append(
                    getPageHandler().showDialogNote(
                        msgLabel("No entries in")
                        + getTitle(request, entry)));
            } else {
                hb.append(
                    getPageHandler().showDialogNote(msg("Nothing found")));
            }
        }
        hb.append(HtmlUtils.formClose());
        sb.append(hb.toString());
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param valueList _more_
     * @param sb _more_
     * @param showHeaderLinks _more_
     *
     * @throws Exception _more_
     */
    public void makeGroupByTable(Request request, Entry entry,
                                 List<Object[]> valueList, Appendable sb,
                                 boolean showHeaderLinks)
            throws Exception {

        List<Column> groupByColumns = new ArrayList<Column>();
        List<String> args = (List<String>) (request.get(ARG_GROUPBY,
                                new ArrayList()));
        for (int i = 0; i < args.size(); i++) {
            String col = args.get(i);
            groupByColumns.add(getColumn(col));
        }
        StringBuilder hb = new StringBuilder();
        if (valueList.size() > 0) {
            HtmlUtils.open(hb, "table", "class", "dbtable", "border", "1",
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
                HtmlUtils.open(hb, "tr", "valign", "top", "class", "dbrow");
                int cnt2 = 0;
                for (Object obj : values) {
                    makeTableHeader(hb, "" + obj);
                    if (addPercent && (cnt2 > 0)) {
                        makeTableHeader(hb, "Percent");
                    }
                    cnt2++;
                }
                HtmlUtils.close(hb, "tr");
            } else {
                HtmlUtils.open(cb, "tr", "valign", "top", "class", "dbrow");
                int col = 0;
                for (int i = 0; i < values.length; i++) {
                    Object obj           = values[i];
                    Column groupByColumn = (i < groupByColumns.size())
                                           ? groupByColumns.get(i)
                                           : null;
                    if (obj instanceof Double) {
                        double d = (Double) obj;
                        HtmlUtils.td(cb, format((Double) obj, false),
                                     "align=right");
                        if (addPercent) {
                            if (sum[col] != 0) {
                                HtmlUtils.td(cb,
                                             pfmt.format(100 * d / sum[col])
                                             + "%", "align=right");
                            } else {
                                HtmlUtils.td(cb, "NA");
                            }
                        }
                    } else if (obj instanceof Integer) {
                        int d = (Integer) obj;
                        HtmlUtils.td(cb, obj.toString(), "align=right");
                        if (addPercent) {
                            if (sum[col] != 0) {
                                HtmlUtils.td(cb,
                                             pfmt.format(100 * d / sum[col])
                                             + "%", "align=right");
                            } else {
                                HtmlUtils.td(cb, "NA");
                            }
                        }
                    } else {
                        HtmlUtils.open(cb, "td");
                        HtmlUtils.open(
                            cb, "div",
                            HtmlUtils.cssClass("db-group-table-cell"));
                        String s = "" + obj;
                        if (s.length() == 0) {
                            s = "&lt;blank&gt;";
                        } else if (groupByColumn != null) {
                            String value     = s;
                            String searchArg = groupByColumn.getSearchArg();
                            if ( !groupByColumn.isEnumeration()) {
                                value = "\"" + value + "\"";
                            } else {
                                value = groupByColumn.getEnumValue(value);
                            }
                            String url =
                                HtmlUtils
                                    .url(request
                                        .makeUrl(getRepository()
                                            .URL_ENTRY_SHOW), new String[] {
                                ARG_ENTRYID, entry.getId(), ARG_DB_SEARCH,
                                "true", searchArg, value, ARG_DB_SORTDIR,
                                dbInfo.getDfltSortAsc()
                                ? "asc"
                                : "desc"
                            });
                            if (dbInfo.getDfltSortColumn() != null) {
                                url += "&" + ARG_DB_SORTBY + "="
                                       + dbInfo.getDfltSortColumn().getName();
                            }
                            s = HtmlUtils.href(url, s,
                                    HtmlUtils.cssClass("ramadda-db-link"));
                        }
                        cb.append(s);
                        HtmlUtils.close(cb, "div");
                        HtmlUtils.close(cb, "td");
                    }
                    col++;
                }
                cb.append("</tr>");
            }
        }

        if (valueList.size() > 0) {
            hb.append(cb);
            HtmlUtils.open(hb, "tr", "valign", "top", "class", "dbrow");
            for (int i = 0; i < sum.length; i++) {
                if (i == 0) {
                    HtmlUtils.td(hb, "Total", "style=\"background:#eee;\"");
                } else {
                    if (Double.isNaN(sum[i])) {
                        HtmlUtils.td(hb, "NA", "style=\"background:#eee;\"");
                    } else {
                        HtmlUtils.td(
                            hb, format(sum[i], false),
                            "align=right  style=\"background:#eee;\"");
                    }
                    if (addPercent) {
                        HtmlUtils.td(hb, "&nbsp;",
                                     "style=\"background:#eee;\"");
                    }
                }
            }
            HtmlUtils.close(hb, "tr");
            HtmlUtils.close(hb, "table");
        } else {
            hb.append(getPageHandler().showDialogNote(msg("Nothing found")));
        }
        sb.append(hb.toString());
    }


    /**
     * _more_
     *
     * @param v _more_
     * @param round _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private String format(double v, boolean round) throws Exception {
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



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param sb _more_
     * @param column _more_
     * @param values _more_
     * @param sdf _more_
     *
     *
     * @return _more_
     * @throws Exception _more_
     */
    public String formatTableValue(Request request, Entry entry,
                                   Appendable sb, Column column,
                                   Object[] values, SimpleDateFormat sdf)
            throws Exception {
        StringBuilder htmlSB = new StringBuilder();


        column.formatValue(entry, htmlSB, Column.OUTPUT_HTML, values, sdf,
                           false);
        String html = htmlSB.toString();

        if (column.getCanSearch() && column.isString()) {
            String value     = (String) values[column.getOffset()];
            String searchArg = column.getSearchArg();
            if ( !column.isEnumeration()) {
                value = "\"" + value + "\"";
            }
            String url = HtmlUtils.url(
                             request.makeUrl(getRepository().URL_ENTRY_SHOW),
                             new String[] {
                ARG_ENTRYID, entry.getId(), ARG_DB_SEARCH, "true", searchArg,
                value, ARG_DB_SORTDIR,
                dbInfo.getDfltSortAsc()
                ? "asc"
                : "desc"
            });
            if (dbInfo.getDfltSortColumn() != null) {
                url += "&" + ARG_DB_SORTBY + "="
                       + dbInfo.getDfltSortColumn().getName();
            }
            html = HtmlUtils.href(url, html,
                                  HtmlUtils.cssClass("ramadda-db-link"));
        }

        sb.append(html);

        return html;
    }

    /**
     * _more_
     *
     * @param entry _more_
     * @param entryProps _more_
     * @param values _more_
     *
     * @return _more_
     */
    public String getIconFor(Entry entry, Hashtable entryProps,
                             Object[] values) {
        for (Column column : getDbInfo().getEnumColumns()) {
            String value    = column.getString(values);
            String attrIcon = getIconFor(entry, entryProps, column, value);
            if (attrIcon != null) {
                return attrIcon;
            }
        }

        return null;
    }



    /**
     * _more_
     *
     * @param entry _more_
     * @param entryProps _more_
     * @param column _more_
     * @param value _more_
     *
     * @return _more_
     */
    public String getIconFor(Entry entry, Hashtable entryProps,
                             Column column, String value) {
        return getAttributeFor(entry, entryProps, column, value,
                               PROP_CAT_ICON);
    }

    /**
     * _more_
     *
     * @param entry _more_
     * @param entryProps _more_
     * @param column _more_
     * @param value _more_
     *
     * @return _more_
     */
    private String getColorFor(Entry entry, Hashtable entryProps,
                               Column column, String value) {
        return getAttributeFor(entry, entryProps, column, value,
                               PROP_CAT_COLOR);
    }


    /**
     * _more_
     *
     * @param entry _more_
     * @param entryProps _more_
     * @param column _more_
     * @param value _more_
     * @param type _more_
     *
     * @return _more_
     */
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


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param dbid _more_
     *
     * @return _more_
     */
    private String getEditUrl(Request request, Entry entry, String dbid) {
        return HtmlUtils.url(request.makeUrl(getRepository().URL_ENTRY_SHOW),
                             new String[] {
            ARG_ENTRYID, entry.getId(), ARG_DBID, dbid, ARG_DB_EDITFORM,
            "true"
        });
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param dbid _more_
     *
     * @return _more_
     */
    private String getViewUrl(Request request, Entry entry, String dbid) {
        return HtmlUtils.url(request.makeUrl(getRepository().URL_ENTRY_SHOW),
                             new String[] {
            ARG_ENTRYID, entry.getId(), ARG_DBID, dbid, ARG_DB_ENTRY, "true"
        });
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param valueList _more_
     * @param fromSearch _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result handleListMap(Request request, Entry entry,
                                List<Object[]> valueList, boolean fromSearch)
            throws Exception {

        DbInfo        dbInfo     = getDbInfo();
        Hashtable     entryProps = getProperties(entry);
        boolean canEdit = getAccessManager().canEditEntry(request, entry);
        StringBuilder sb         = new StringBuilder();

        String links = getHref(request, entry, VIEW_KML,
                               msg("Google Earth KML"));
        if ( !request.get(ARG_EMBEDDED, false)) {
            addViewHeader(request, entry, sb, VIEW_MAP, valueList.size(),
                          fromSearch, links);
        }



        Column  theColumn = null;
        boolean bbox      = true;
        for (Column column : tableHandler.getColumns()) {
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
            throw new IllegalStateException("No geodata data found");
        }

        int    width        = 0;
        int    height       = 500;
        String mapDisplayId = "mapDisplay_" + Utils.getGuid();

        Hashtable props = Utils.makeHashtable("displayDiv", mapDisplayId,
                              "style", "");
        if (request.defined("mapLayer")) {
            props.put("defaultMapLayer", request.getString("mapLayer", ""));
        }
        if (request.defined("mapBounds")) {
            props.put("initialBounds", request.getString("mapBounds", ""));
        }

        MapInfo map = getRepository().getMapManager().createMap(request,
                          entry, width, height, false, props);
        boolean       makeRectangles = valueList.size() <= 20;

        String        leftWidth      = "300";
        String        icon           = getMapIcon(request, entry);
        StringBuilder entryList      = new StringBuilder();
        sb.append(
            HtmlUtils.cssBlock(
                "\n.db-map-list-inner {max-height: " + height
                + "px; overflow-y: auto; overflow-x:auto; }\n\n"));
        HtmlUtils.open(entryList, "div", "class", "db-map-list-outer");
        HtmlUtils.open(entryList, "div", "class", "db-map-list-inner");
        SimpleDateFormat                 sdf    = getDateFormat(entry);
        Hashtable<String, StringBuilder> catMap = null;
        List<String>                     cats   = null;
        if (getDbInfo().getMapCategoryColumn() != null) {
            catMap = new Hashtable<String, StringBuilder>();
            cats   = new ArrayList<String>();
        }


        for (Object[] values : valueList) {
            String dbid  = (String) values[IDX_DBID];
            double lat   = 0;
            double lon   = 0;
            double north = 0,
                   west  = 0,
                   south = 0,
                   east  = 0;

            if (theColumn == null) {
                lat  = dbInfo.getLatColumn().getDouble(values);
                lon  = dbInfo.getLonColumn().getDouble(values);
                bbox = false;
            } else {
                if ( !bbox) {
                    //Check if the lat/lon is defined
                    if ( !theColumn.hasLatLon(values)) {
                        continue;
                    }
                    double[] ll = theColumn.getLatLon(values);
                    lat = ll[0];
                    lon = ll[1];
                } else {
                    if ( !theColumn.hasLatLonBox(values)) {
                        continue;
                    }
                    double[] ll = theColumn.getLatLonBbox(values);
                    north = ll[0];
                    west  = ll[1];
                    south = ll[2];
                    east  = ll[3];
                }
            }

            if (bbox) {
                map.addBox("", "", "", new MapBoxProperties("red", false),
                           north, west, south, east);
            }
            StringBuilder theSB = entryList;
            if (getDbInfo().getMapCategoryColumn() != null) {
                String cat =
                    getDbInfo().getMapCategoryColumn().getString(values);
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
            String attrIcon  = getIconFor(entry, entryProps, values);
            if (attrIcon != null) {
                iconToUse = getDbIconUrl(attrIcon);
                //                theSB.append(HtmlUtils.img(iconToUse,"", "width=16"));
            }
            if (false && canEdit) {
                String editUrl = getEditUrl(request, entry, dbid);
                theSB.append(HtmlUtils.href(editUrl, HtmlUtils.img(iconToUse
                /*                            getRepository().getUrlBase()
                                              + "/db/database_edit.png"*/
                , msg("Edit entry"))));
            }
            String viewUrl = getViewUrl(request, entry, dbid);
            theSB.append(HtmlUtils.href(viewUrl, HtmlUtils.img(iconToUse,
            //                        getRepository().getUrlBase() + "/db/database_go.png",
            msg("View entry"), "width=16")));
            theSB.append(" ");
            theSB.append(map.getHiliteHref(dbid,
                                           getMapLabel(entry, values, sdf)));


            theSB.append("</div>");
            //            theSB.append(HtmlUtils.br());
            String info = getHtml(request, entry, dbid, getColumns(), values,
                                  sdf);
            info = info.replace("\r", " ");
            info = info.replace("\n", " ");
            info = info.replace("\"", "\\\"");
            if ( !bbox) {
                map.addMarker(dbid, new LatLonPointImpl(lat, lon), iconToUse,
                              "", info);
            } else {
                if ( !makeRectangles) {
                    map.addMarker(dbid, new LatLonPointImpl(south, east),
                                  iconToUse, "", info);
                } else {
                    map.addMarker(dbid, new LatLonPointImpl(south
                            + (north - south) / 2, west
                                + (east - west) / 2), "", icon, info);
                }
            }
        }

        boolean simpleMap = request.get("simpleMap", false);
        if (catMap != null) {
            boolean open = true;
            for (String cat : cats) {
                StringBuilder theSB = catMap.get(cat);
                String content = HtmlUtils.insetLeft(theSB.toString(), 20);
                entryList.append(HtmlUtils.makeShowHideBlock(cat, content,
                        open));
                open = false;
            }
        }
        entryList.append(HtmlUtils.close(HtmlUtils.TAG_DIV));
        entryList.append(HtmlUtils.close(HtmlUtils.TAG_DIV));

        if ( !simpleMap) {
            HtmlUtils.open(sb, "table", "class", "db-map-table",
                           "cellpadding", "0", "border", "0", "width",
                           "100%");
            HtmlUtils.open(sb, "tr", "valign", "top");
            map.center();
            sb.append(HtmlUtils.col(entryList.toString(),
                                    " class=\"db-map-column\" "
                                    + HtmlUtils.attr("width",
                                        "250" /*leftWidth + "px"*/)));
            sb.append(HtmlUtils.col(map.getHtml(),
                                    "  class=\"db-map-column\" "  //HtmlUtils.attr(HtmlUtils.ATTR_WIDTH, "60%")
                ));
            sb.append(
                HtmlUtils.col(
                    "<div id=\"" + mapDisplayId
                    + "\" style=\"width:250px;max-width:250px;overflow-x:hidden;max-height:"
                    + height
                    + "px; overflow-y:hidden;\"></div>", " class=\"db-map-column\"  width=250"));
            HtmlUtils.close(sb, "tr", "table");
        } else {
            sb.append(map.getHtml());
        }


        String js =
            "highlightMarkers('.db-map-list-outer .db-map-list-entry', "
            + map.getVariableName() + ", '#ffffcc', 'white');";
        sb.append(HtmlUtils.script(JQuery.ready(js)));

        if ( !request.get(ARG_EMBEDDED, false)) {
            addViewFooter(request, entry, sb);
        }

        return new Result(getTitle(request, entry), sb);

    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     */
    public String getEntryIcon(Request request, Entry entry) {
        return getRepository().getUrlBase() + tableIcon;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     */
    public String getMapIcon(Request request, Entry entry) {
        return getEntryIcon(request, entry);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param valueList _more_
     * @param fromSearch _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result handleListKml(Request request, Entry entry,
                                List<Object[]> valueList, boolean fromSearch)
            throws Exception {

        DbInfo  dbInfo    = getDbInfo();
        Column  theColumn = null;
        boolean bbox      = true;
        for (Column column : tableHandler.getColumns()) {
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

        Element root   = KmlUtil.kml(entry.getName());
        Element folder = KmlUtil.folder(root, entry.getName(), false);
        KmlUtil.open(folder, true);
        if (entry.getDescription().length() > 0) {
            KmlUtil.description(folder, entry.getDescription());
        }



        for (Object[] values : valueList) {
            String dbid = (String) values[IDX_DBID];
            double lat  = 0;
            double lon  = 0;

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
            String label = getKmlLabel(entry, values, null);
            String viewUrl = request.getAbsoluteUrl(getViewUrl(request,
                                 entry, dbid));
            String        href = HtmlUtils.href(viewUrl, label);
            StringBuilder desc = new StringBuilder(href + "<br>");
            getHtml(request, desc, entry, values);
            Element placemark = KmlUtil.placemark(folder, label,
                                    desc.toString(), lat, lon, 0, null);
            if (dbInfo.getDateColumn() != null) {
                Date date = (Date) dbInfo.getDateColumn().getObject(values);
                if (date != null) {
                    KmlUtil.timestamp(placemark, date);
                }
            }
        }
        StringBuilder sb = new StringBuilder(XmlUtil.toString(root));

        return new Result("", sb, "text/kml");
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getDefaultDateFormatString() {
        return "yyyy/MM/dd";
    }

    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    public SimpleDateFormat getDateFormat(Entry entry) {
        return getDateFormat(entry, getDefaultDateFormatString());
    }

    /**
     * _more_
     *
     * @param entry _more_
     * @param format _more_
     *
     * @return _more_
     */
    public SimpleDateFormat getDateFormat(Entry entry, String format) {
        SimpleDateFormat sdf      = new SimpleDateFormat(format);
        String           timezone = getEntryUtil().getTimezone(entry);
        if (timezone != null) {
            sdf.setTimeZone(TimeZone.getTimeZone(timezone));
        }

        return sdf;
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param tag _more_
     * @param props _more_
     *
     * @return _more_
     */
    public int getDefaultMax(Request request, Entry entry, String tag,
                             Hashtable props) {
        return 1000;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param tag _more_
     * @param props _more_
     *
     * @return _more_
     */
    @Override
    public String getUrlForWiki(Request request, Entry entry, String tag,
                                Hashtable props) {
        if (tag.equals(WikiConstants.WIKI_TAG_CHART)
                || tag.equals(WikiConstants.WIKI_TAG_DISPLAY)) {
            try {
                if (props.get("max") == null) {
                    props.put("max",
                              "" + getDefaultMax(request, entry, tag, props));
                }
                String url =
                    ((PointOutputHandler) getRecordOutputHandler())
                        .getJsonUrl(request, entry, props);
                url += "&"
                       + request.getUrlArgs(
                           (HashSet<String>) Utils.makeHashSet(
                               ARG_ENTRYID, ARG_DB_SEARCH,
                               ARG_DB_DOSAVESEARCH), null, null);
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }
        }

        return super.getUrlForWiki(request, entry, tag, props);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param fromSearch _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result handleListChart(Request request, Entry entry,
                                  boolean fromSearch)
            throws Exception {

        DbInfo        dbInfo = getDbInfo();
        boolean canEdit      = getAccessManager().canEditEntry(request,
                                   entry);
        StringBuilder sb     = new StringBuilder();
        addViewHeader(request, entry, sb, VIEW_CHART, 100, false);

        String wikiText =
            "{{display type=\"linechart\" showTitle=\"false\" layoutHere=\"true\" showMenu=\"true\" xxfields=\"point_altitude,speed\"  errorMessage=\" \"  }}";
        sb.append(getWikiManager().wikifyEntry(request, entry, wikiText));

        /*
        GregorianCalendar cal = new GregorianCalendar(DateUtil.TIMEZONE_GMT);
        SimpleDateFormat  sdf = getDateFormat(entry);

        sb.append("\n\n");
        int height = valueList.size() * 30;
        String fillerIcon = getRepository().getUrlBase()
                            + "/db/bluesquare.png";
        for (Column column :  getColumns()) {
            if (column.isNumeric()) {
                List data   = new ArrayList();
                List labels = new ArrayList();
                boolean isPercentage =
                    column.getType().equals(Column.DATATYPE_PERCENTAGE);
                boolean isInt = column.getType().equals(Column.DATATYPE_INT);
                double  maxValue = Double.NaN;
                for (Object[] values : valueList) {
                    if (isPercentage) {
                        maxValue = 100;

                        break;
                    } else if (isInt) {
                        int v =
                            ((Integer) values[column.getOffset()]).intValue();
                        if (maxValue != maxValue) {
                            maxValue = v;
                        }
                        maxValue = Math.max(v, maxValue);
                    } else {
                        double v =
                            ((Double) values[column.getOffset()])
                                .doubleValue();
                        if (maxValue != maxValue) {
                            maxValue = v;
                        } else {
                            maxValue = Math.max(v, maxValue);
                        }
                    }
                }

                sb.append(column.getLabel());
                sb.append("\n");
                sb.append(HtmlUtils.br());
                sb.append("\n");
                sb.append(
                    "<table xwidth=100% border=1 cellspacing=0 cellpadding=1>\n");
                for (Object[] values : valueList) {
                    double value;
                    if (isPercentage) {
                        Double d = (Double) values[column.getOffset()];
                        value = (int) (d.doubleValue() * 100);
                    } else if (isInt) {
                        value =
                            ((Integer) values[column.getOffset()]).intValue();
                    } else {
                        value =
                            ((Double) values[column.getOffset()])
                                .doubleValue();
                    }
                    String url = canEdit
                                 ? getEditUrl(request, entry,
                                     (String) values[IDX_DBID])
                                 : getViewUrl(request, entry,
                                     (String) values[IDX_DBID]);

                    String href = HtmlUtils.href(url,
                                      getLabel(entry, values, sdf));
                    String rowId = "row_" + values[IDX_DBID];
                    String divId = "div_" + values[IDX_DBID];
                    String event = getEventJS(request, entry, values, rowId,
                                       divId);
                    sb.append("<tr " + HtmlUtils.id(rowId)
                              + "> <td  width=10% " + HtmlUtils.id(divId)
                              + event + ">" + HtmlUtils.space(2) + href
                              + "&nbsp;</td>");
                    sb.append("<td align=right width=5%>" + value
                              + "&nbsp;</td><td> ");
                    double percentage = value / maxValue;
                    sb.append("<img src=" + fillerIcon + " height=10 width="
                              + (int) (8 * percentage * 100) + "></td>");
                    sb.append("</tr>\n");
                }
                sb.append("</table>\n");
                sb.append(HtmlUtils.p());
            }
        }
        */

        addViewFooter(request, entry, sb);

        return new Result(getTitle(request, entry), sb);

    }


    /**
     * _more_
     *
     * @param wikiUtil _more_
     * @param request _more_
     * @param originalEntry _more_
     * @param entry _more_
     * @param tag _more_
     * @param props _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
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
            newRequest.put(ARG_EMBEDDED, "true");
            StringBuilder sb = new StringBuilder();
            addStyleSheet(sb);
            String layer = (String) props.get("layer");
            if (layer != null) {
                newRequest.put("mapLayer", layer);
            }
            String bounds = (String) props.get("mapBounds");
            if (bounds != null) {
                newRequest.put("mapBounds", bounds);
            }
            if (newRequest.defined(ARG_DB_SEARCHNAME)) {
                HtmlUtils.sectionHeader(
                    sb, newRequest.getString(ARG_DB_SEARCHNAME, ""));
            }
            Result result = handleSearch(newRequest, entry);

            sb.append(result.getStringContent());

            return sb.toString();
        } catch (Exception exc) {
            return getPageHandler().showDialogError("An error occurred:<br>"
                    + exc.toString(), false);
        }
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param valueList _more_
     * @param fromSearch _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result handleListGrid(Request request, Entry entry,
                                 List<Object[]> valueList, boolean fromSearch)
            throws Exception {

        DbInfo           dbInfo     = getDbInfo();
        SimpleDateFormat sdf        = getDateFormat(entry);
        boolean canEdit = getAccessManager().canEditEntry(request, entry);
        StringBuilder    sb         = new StringBuilder();
        String           view       = getWhatToShow(request);
        Column           gridColumn = null;
        for (Column column : dbInfo.getCategoryColumns()) {
            if (Misc.equals(view, VIEW_GRID + column.getName())) {
                gridColumn = column;

                break;
            }
        }
        if (gridColumn == null) {
            throw new IllegalStateException("No grid columns defined");
        }
        String links = getHref(request, entry,
                               VIEW_CATEGORY + gridColumn.getName(),
                               msg("Category View"));
        addViewHeader(request, entry, sb, VIEW_GRID + gridColumn.getName(),
                      valueList.size(), fromSearch, links);

        List<TwoFacedObject> enumValues = getEnumValues(request, entry,
                                              gridColumn);


        sb.append(
            HtmlUtils.cssBlock(
                ".gridtable td {padding:5px;padding-bottom:0px;padding-top:8px;}\n.gridon {background: #88C957;}\n.gridoff {background: #eee;}"));
        sb.append(
            "<table cellspacing=0 cellpadding=0 border=1 width=100% class=\"gridtable\">\n");
        sb.append("<tr>");
        int width = 100 / (enumValues.size() + 1);
        makeTableHeader(sb, "&nbsp;",
                        HtmlUtils.attr(HtmlUtils.ATTR_WIDTH, width + "%"));
        String key = tableHandler.getTableName() + "." + gridColumn.getName();
        for (TwoFacedObject tfo : enumValues) {
            String value = tfo.getId().toString();
            String searchUrl =
                HtmlUtils.url(
                    request.makeUrl(getRepository().URL_ENTRY_SHOW),
                    new String[] {
                ARG_ENTRYID, entry.getId(), ARG_DB_SEARCH, "true", key, value
            });

            makeTableHeader(sb, "&nbsp;" + HtmlUtils.href(searchUrl, value),
                            HtmlUtils.attr(HtmlUtils.ATTR_WIDTH,
                                           width + "%"));
        }
        for (Object[] valuesArray : valueList) {
            sb.append("<tr>\n");
            String url = canEdit
                         ? getEditUrl(request, entry,
                                      (String) valuesArray[IDX_DBID])
                         : getViewUrl(request, entry,
                                      (String) valuesArray[IDX_DBID]);

            String rowId = "row_" + valuesArray[IDX_DBID];
            String event = getEventJS(request, entry, valuesArray, rowId,
                                      rowId);
            String href = HtmlUtils.href(url,
                                         getLabel(entry, valuesArray, sdf));
            //            href= HtmlUtils.span(href,HtmlUtils.cssClass("xdbcategoryrow")+);
            sb.append(HtmlUtils.col("&nbsp;" + href,
                                    HtmlUtils.id(rowId) + event
                                    + HtmlUtils.cssClass("dbcategoryrow")));
            String rowValue = (String) valuesArray[gridColumn.getOffset()];
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
        HtmlUtils.close(sb, "table");
        addViewFooter(request, entry, sb);

        return new Result(getTitle(request, entry), sb);
    }


    /**
     * _more_
     *
     * @param column _more_
     *
     * @return _more_
     */
    public String getSearchUrlArgument(Column column) {
        return tableHandler.getTableName() + "." + column.getName();
    }


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     * @param column _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private List<TwoFacedObject> getEnumValues(Request request, Entry entry,
            Column column)
            throws Exception {
        if (column.getType().equals(Column.DATATYPE_ENUMERATION)) {
            List<TwoFacedObject> enums = column.getValues();
            if (enums.size() > 0) {
                return enums;
            }
        }

        return tableHandler.getEnumValues(request, column, entry);

    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param view _more_
     * @param label _more_
     *
     * @return _more_
     */
    public String getHref(Request request, Entry entry, String view,
                          String label) {
        return HtmlUtils.href(getUrl(request, entry, view), label);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param view _more_
     * @param label _more_
     * @param suffix _more_
     *
     * @return _more_
     */
    public String getHref(Request request, Entry entry, String view,
                          String label, String suffix) {
        return HtmlUtils.href(getUrl(request, entry, view, suffix), label);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param view _more_
     *
     * @return _more_
     */
    public String getUrl(Request request, Entry entry, String view) {
        return getUrl(request, entry, view, "");
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param view _more_
     * @param suffix _more_
     *
     * @return _more_
     */
    public String getUrl(Request request, Entry entry, String view,
                         String suffix) {
        return HtmlUtils.url(request.makeUrl(getRepository().URL_ENTRY_SHOW)
                             + suffix, new String[] { ARG_ENTRYID,
                entry.getId(), ARG_DB_VIEW, view });
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param valueList _more_
     * @param fromSearch _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result handleListCategory(Request request, Entry entry,
                                     List<Object[]> valueList,
                                     boolean fromSearch)
            throws Exception {
        DbInfo           dbInfo     = getDbInfo();
        SimpleDateFormat sdf        = getDateFormat(entry);
        boolean canEdit = getAccessManager().canEditEntry(request, entry);
        StringBuilder    sb         = new StringBuilder();
        String           view       = getWhatToShow(request);
        Column           gridColumn = null;
        for (Column column : dbInfo.getCategoryColumns()) {
            if (Misc.equals(view, VIEW_CATEGORY + column.getName())) {
                gridColumn = column;

                break;
            }
        }
        if (gridColumn == null) {
            throw new IllegalStateException("No grid columns defined");
        }
        String links = getHref(request, entry,
                               VIEW_GRID + gridColumn.getName(),
                               msg("Grid View"));
        addViewHeader(request, entry, sb,
                      VIEW_CATEGORY + gridColumn.getName(), valueList.size(),
                      fromSearch, links);


        Hashtable<String, StringBuilder> map = new Hashtable<String,
                                                   StringBuilder>();
        List<String> rowValues = new ArrayList<String>();
        int          cnt       = 0;


        for (Object[] valuesArray : valueList) {
            String url = canEdit
                         ? getEditUrl(request, entry,
                                      (String) valuesArray[IDX_DBID])
                         : getViewUrl(request, entry,
                                      (String) valuesArray[IDX_DBID]);
            String label    = getLabel(entry, valuesArray, sdf);
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
            String event = getEventJS(request, entry, valuesArray, rowId,
                                      rowId);
            buffer.append(HtmlUtils.div(href,
                                        HtmlUtils.cssClass("dbcategoryrow")
                                        + HtmlUtils.id(rowId) + event));
            cnt++;
        }

        List<String> titles = new ArrayList<String>();
        List<String> tabs   = new ArrayList<String>();

        for (String rowValue : rowValues) {
            titles.add(rowValue);
            tabs.add(HtmlUtils.insetDiv(map.get(rowValue).toString(), 0, 20,
                                        10, 0));
        }
        HtmlUtils.makeAccordian(sb, titles, tabs, false);
        addViewFooter(request, entry, sb);

        return new Result(getTitle(request, entry), sb);
    }


    /**
     * _more_
     *
     * @param column _more_
     *
     * @return _more_
     */
    private boolean isDataColumn(Column column) {
        if (column.getName().equals(COL_DBID)
                || column.getName().equals(COL_DBUSER)
                || column.getName().equals(COL_DBCREATEDATE)
                || column.getName().equals(COL_DBPROPS)) {
            return false;
        }

        return true;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param valueList _more_
     * @param fromSearch _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result handleListMotionChart(Request request, Entry entry,
                                        List<Object[]> valueList,
                                        boolean fromSearch)
            throws Exception {

        DbInfo        dbInfo = getDbInfo();
        StringBuilder sb     = new StringBuilder();
        addViewHeader(request, entry, sb, VIEW_CHART, valueList.size(),
                      fromSearch);

        getPageHandler().addGoogleJSImport(request, sb);
        sb.append(
            "<script type=\"text/javascript\">\ngoogle.load('visualization', '1', {'packages':['motionchart']});\ngoogle.setOnLoadCallback(drawChart);\nfunction drawChart() {\n        var data = new google.visualization.DataTable();\n");
        StringBuilder init      = new StringBuilder();

        int           columnCnt = 0;
        init.append("data.addColumn('string', 'Name');\n");
        init.append("data.addColumn('date', 'Date');\n");

        GregorianCalendar cal = new GregorianCalendar(DateUtil.TIMEZONE_GMT);
        SimpleDateFormat  sdf        = getDateFormat(entry);

        Column            dateColumn = null;


        for (Column column : dbInfo.getColumnsToUse()) {
            if ( !isDataColumn(column)) {
                continue;
            }
            if (column.isDate()) {
                if (dateColumn == null) {
                    dateColumn = column;
                }

                continue;
            }
            String varName = column.getName();
            if (column.isNumeric()) {
                init.append("data.addColumn('number', '" + varName + "');\n");
            } else if (column.isString()) {
                init.append("data.addColumn('string', '" + varName + "');\n");
            } else if (column.getType().equals(Column.DATATYPE_DATE)) {
                init.append("data.addColumn('date', '" + varName + "');\n");
            }
        }
        sb.append("data.addRows(" + valueList.size() + ");\n");
        sb.append(init);
        int row = 0;

        for (Object[] values : valueList) {
            columnCnt = 0;
            String label = getLabel(entry, values, sdf);
            sb.append("data.setValue(" + row + ", " + columnCnt + ","
                      + HtmlUtils.squote(label) + ");\n");
            columnCnt++;

            Date date = (Date) values[dateColumn.getOffset()];
            cal.setTime(date);
            sb.append("theDate = new Date(" + cal.get(cal.YEAR) + ","
                      + cal.get(cal.MONTH) + "," + cal.get(cal.DAY_OF_MONTH)
                      + ");\n");

            sb.append("theDate.setHours(" + cal.get(cal.HOUR) + ","
                      + cal.get(cal.MINUTE) + "," + cal.get(cal.SECOND) + ","
                      + cal.get(cal.MILLISECOND) + ");\n");

            sb.append("data.setValue(" + row + ", " + columnCnt
                      + ", theDate);\n");
            columnCnt++;

            for (Column column : dbInfo.getColumnsToUse()) {
                if ( !isDataColumn(column)) {
                    continue;
                }
                if (column == dateColumn) {
                    continue;
                }
                if (column.isNumeric()) {
                    sb.append("data.setValue(" + row + ", " + columnCnt + ","
                              + values[column.getOffset()] + ");\n");
                    columnCnt++;
                } else if (column.isString()) {
                    sb.append("data.setValue(" + row + ", " + columnCnt + ","
                              + HtmlUtils.squote(""
                                  + values[column.getOffset()]) + ");\n");
                    columnCnt++;
                } else if (column.isDate()) {
                    columnCnt++;
                }
            }
            row++;
        }



        sb.append(
            "var chart = new google.visualization.MotionChart(document.getElementById('chart_div'));\n");
        sb.append("chart.draw(data, {width: 800, height:500});\n");
        sb.append("}\n");
        sb.append("</script>\n");
        sb.append(
            "<div id=\"chart_div\" style=\"width: 800px; height: 500px;\"></div>\n");

        addViewFooter(request, entry, sb);

        return new Result(getTitle(request, entry), sb);

    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param valueList _more_
     * @param fromSearch _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result handleListTimeline(Request request, Entry entry,
                                     List<Object[]> valueList,
                                     boolean fromSearch)
            throws Exception {

        DbInfo dbInfo = getDbInfo();
        if (dbInfo.getDateColumn() == null) {
            throw new IllegalStateException("No date data found");
        }



        StringBuilder sb = new StringBuilder();
        addViewHeader(request, entry, sb, VIEW_TIMELINE, valueList.size(),
                      fromSearch);
        //        String links = getHref(request, entry,VIEW_CALENDAR, msg("Calendar"));
        //        sb.append(HtmlUtils.center(links));
        String timelineAppletTemplate =
            getRepository().getResource(
                "/org/ramadda/repository/resources/web/timelineapplet.html");
        List times  = new ArrayList();
        List labels = new ArrayList();
        List ids    = new ArrayList();
        for (Object[] values : valueList) {
            times.add(
                SqlUtil.format(
                    (Date) values[dbInfo.getDateColumn().getOffset()]));
            String label = getLabel(entry, values, null).trim();
            if (label.length() == 0) {
                label = "NA";
            }
            label = label.replaceAll(",", "_");
            label = label.replaceAll("\"", " ");
            labels.add(label);
            ids.add(values[IDX_DBID]);
        }
        String tmp = StringUtil.replace(timelineAppletTemplate, "${times}",
                                        StringUtil.join(",", times));
        tmp = StringUtil.replace(tmp, "${root}",
                                 getRepository().getUrlBase());
        tmp = StringUtil.replace(tmp, "${labels}",
                                 StringUtil.join(",", labels));
        tmp = StringUtil.replace(tmp, "${ids}", StringUtil.join(",", ids));
        String formUrl = request.makeUrl(getRepository().URL_ENTRY_SHOW);
        String url =
            HtmlUtils.url(request.makeUrl(getRepository().URL_ENTRY_SHOW),
                          new String[] { ARG_ENTRYID,
                                         entry.getId(), ARG_DBIDS, "%ids%" });

        tmp = StringUtil.replace(tmp, "${loadurl}", url);
        sb.append(tmp);

        addViewFooter(request, entry, sb);

        return new Result("", sb);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param valueList _more_
     * @param fromSearch _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result handleListCalendar(Request request, Entry entry,
                                     List<Object[]> valueList,
                                     boolean fromSearch)
            throws Exception {
        DbInfo        dbInfo = getDbInfo();
        boolean canEdit      = getAccessManager().canEditEntry(request,
                                   entry);
        StringBuilder sb     = new StringBuilder();
        String links = getHref(request, entry, VIEW_TIMELINE,
                               msg("Timeline")) + "&nbsp;|&nbsp;"
                                   + getHref(request, entry, VIEW_ICAL,
                                             msg("ICAL"));
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
        SimpleDateFormat sdf = getDateFormat(entry);
        for (Object[] values : valueList) {
            String        dbid  = (String) values[IDX_DBID];
            Date date = (Date) values[dbInfo.getDateColumn().getOffset()];
            String        url   = getViewUrl(request, entry, dbid);
            String        label = getCalendarLabel(entry, values, sdf).trim();
            StringBuilder html  = new StringBuilder();
            if (label.length() == 0) {
                label = "NA";
            }
            String href = HtmlUtils.href(url, label);

            /*
            if (canEdit) {
                String editUrl = getEditUrl(request, entry, dbid);
                href = HtmlUtils.href(
                    editUrl,
                    HtmlUtils.img(
                        getRepository().getUrlBase()
                        + "/db/database_edit.png", msg("Edit entry"))) + " "
                            + href;
            }
            */
            //            html.append(href);
            String rowId = "row_" + values[IDX_DBID];
            String event = getEventJS(request, entry, values, rowId, rowId);
            href = HtmlUtils.div(href,
                                 HtmlUtils.cssClass("dbcategoryrow")
                                 + HtmlUtils.id(rowId) + event);
            //            getHtml(request, html, entry, values);
            String block = HtmlUtils.makeShowHideBlock(href, html.toString(),
                               false);
            calEntries.add(new CalendarOutputHandler.CalendarEntry(date,
                    href, href));
        }

        calendarOutputHandler.outputCalendar(request, calEntries, sb, false);

        addViewFooter(request, entry, sb);

        return new Result(getTitle(request, entry), sb);
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param valueList _more_
     * @param fromSearch _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result handleListStickyNotes(Request request, Entry entry,
                                        List<Object[]> valueList,
                                        boolean fromSearch)
            throws Exception {

        Hashtable entryProps     = getProperties(entry);
        String stickyLabelString =
            (String) entryProps.get(PROP_STICKY_LABELS);


        boolean canEdit  = getAccessManager().canEditEntry(request, entry);
        StringBuilder sb = new StringBuilder();
        StringBuilder js = new StringBuilder();
        addViewHeader(request, entry, sb, VIEW_STICKYNOTES, valueList.size(),
                      fromSearch);


        //        String links = getHref(request, entry,VIEW_TABLE, msg("Table"));
        //        sb.append(HtmlUtils.center(links));


        int cnt    = 0;
        int poscnt = 0;
        sb.append(
            HtmlUtils.importJS(
                getRepository().getFileUrl("/lib/dom-drag.js")));
        SimpleDateFormat sdf = getDateFormat(entry);
        for (Object[] values : valueList) {
            Hashtable props = getProps(values);
            String    dbid  = (String) values[IDX_DBID];
            String    url   = getViewUrl(request, entry, dbid);
            String    label = getLabel(entry, values, sdf).trim();
            if (label.length() == 0) {
                label = "NA";
            }
            String href = HtmlUtils.href(url, label);


            if (false && canEdit) {
                String editUrl = getEditUrl(request, entry, dbid);
                href = HtmlUtils.href(
                    editUrl,
                    HtmlUtils.img(
                        getRepository().getUrlBase()
                        + "/db/database_edit.png", msg("Edit entry"))) + " "
                            + href;
            }
            int top  = Misc.getProperty(props, "posy", 150 + poscnt * 40);
            int left = Misc.getProperty(props, "posx", 50);
            if ((props == null) || (props.get("posx") == null)) {
                poscnt++;
            }
            String info = getHtml(request, entry, dbid, getColumns(), values,
                                  sdf);
            String contents = href
                              + HtmlUtils.makeShowHideBlock("...", info,
                                  false);
            sb.append(HtmlUtils.div(contents,
                                    HtmlUtils.cssClass("dbstickynote")
                                    + HtmlUtils.id(dbid) + " style=\"top:"
                                    + top + "px;  left:" + left + "px;\""));

            cnt++;
            String jsid = "id" + cnt;
            js.append("var " + jsid + " = '" + dbid + "';\n");
            js.append("var draggableDiv = document.getElementById(" + jsid
                      + ");\n");
            js.append("Drag.init(draggableDiv);\n");
            if (canEdit) {
                String posUrl =
                    HtmlUtils.url(
                        request.makeUrl(getRepository().URL_ENTRY_SHOW),
                        new String[] {
                    ARG_ENTRYID, entry.getId(), ARG_DBID, dbid, ARG_DB_SETPOS,
                    "true"
                });

                js.append(
                    "draggableDiv.onDragEnd  = function(x,y){stickyDragEnd("
                    + jsid + "," + HtmlUtils.squote(posUrl) + ");}\n");
            }
        }

        if (stickyLabelString != null) {
            int labelCnt = 0;
            for (String label :
                    StringUtil.split(stickyLabelString, "\n", true, true)) {
                String id   = "label_" + label;
                int    top  = 100;
                int    left = 150 + labelCnt * 30;
                String posx = (String) entryProps.get(PROP_STICKY_POSX + "."
                                  + label);
                String posy = (String) entryProps.get(PROP_STICKY_POSY + "."
                                  + label);
                if ((posx != null) && (posy != null)) {
                    top  = Integer.parseInt(posy);
                    left = Integer.parseInt(posx);
                } else {
                    labelCnt++;
                }
                String text = label;
                sb.append(HtmlUtils.div(text,
                                        HtmlUtils.cssClass("dbstickylabel")
                                        + HtmlUtils.id(id) + " style=\"top:"
                                        + top + "px;  left:" + left
                                        + "px;\""));

                js.append("var draggableDiv = document.getElementById("
                          + HtmlUtils.squote(id) + ");\n");
                js.append("Drag.init(draggableDiv);\n");
                if (canEdit) {
                    String posUrl =
                        HtmlUtils.url(
                            request.makeUrl(getRepository().URL_ENTRY_SHOW),
                            new String[] {
                        ARG_ENTRYID, entry.getId(), ARG_DB_STICKYLABEL, label,
                        ARG_DB_SETPOS, "true"
                    });

                    js.append(
                        "draggableDiv.onDragEnd  = function(x,y){stickyDragEnd("
                        + HtmlUtils.squote(id) + ","
                        + HtmlUtils.squote(posUrl) + ");}\n");
                }
            }
        }

        sb.append(HtmlUtils.script(js.toString()));

        addViewFooter(request, entry, sb);

        return new Result(getTitle(request, entry), sb);
    }





    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param valueList _more_
     * @param fromSearch _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result handleListIcal(Request request, Entry entry,
                                 List<Object[]> valueList, boolean fromSearch)
            throws Exception {
        DbInfo dbInfo = getDbInfo();
        SimpleDateFormat sdf =
            RepositoryUtil.makeDateFormat("yyyyMMdd'T'HHmmss");
        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCALENDAR\n");
        sb.append("PRODID:-//Unidata/UCAR//RAMADDA Calendar//EN\n");
        sb.append("VERSION:2.0\n");
        sb.append("CALSCALE:GREGORIAN\n");
        sb.append("METHOD:PUBLISH\n");
        for (Object[] values : valueList) {
            String dbid  = (String) values[IDX_DBID];
            Date   date1 = (Date) values[dbInfo.getDateColumn().getOffset()];
            Date   date2 = (Date) values[(dbInfo.getDateColumns().size() > 1)
                                         ? dbInfo.getDateColumns().get(1).getOffset()
                                         : dbInfo.getDateColumns().get(0).getOffset()];
            String dateString1 = sdf.format(date1) + "Z";
            String dateString2 = sdf.format(date2) + "Z";
            String url         = getViewUrl(request, entry, dbid);
            url = request.getAbsoluteUrl(url);
            String label = getLabel(entry, values, null).trim();

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
        sb.append("END:VCALENDAR\n");

        return new Result("", sb, "text/calendar");
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param template _more_
     * @param valueList _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result handleListTemplate(Request request, Entry entry,
                                     DbTemplate template,
                                     List<Object[]> valueList)
            throws Exception {
        SimpleDateFormat sdf    = getDateFormat(entry);
        DbInfo           dbInfo = getDbInfo();
        StringBuilder    sb     = new StringBuilder();
        sb.append(template.prefix);
        StringBuilder tmp = new StringBuilder();
        for (int cnt = 0; cnt < valueList.size(); cnt++) {
            Object[] values = valueList.get(cnt);
            String   t      = template.entry;
            for (Column column : dbInfo.getColumnsToUse()) {
                column.formatValue(entry, tmp, Column.OUTPUT_HTML, values,
                                   sdf, false);

                t = t.replace("${" + column.getName() + "}", tmp.toString());
                tmp.setLength(0);
            }
            sb.append(t);
        }
        sb.append(template.suffix);
        if (template.mime != null) {
            return new Result("", sb, template.mime);
        }

        return new Result("", sb);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param clause _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Object[]> readValues(Request request, Entry entry,
                                     Clause clause)
            throws Exception {
        if (request.exists(ARG_DBIDS)) {
            List<Object[]> result = new ArrayList<Object[]>();
            String         ids    = request.getString(ARG_DBIDS, "");
            request.remove(ARG_DBIDS);
            for (String id : StringUtil.split(ids, ",", true, true)) {
                result.addAll(readValues(request, entry,
                                         makeClause(entry, id)));
            }
            request.put(ARG_DBIDS, ids);

            return result;
        }



        List<String> colNames = tableHandler.getColumnNames();
        String       extra    = "";

        if ((dbInfo.getDfltSortColumn() != null)
                && !request.defined(ARG_DB_SORTBY)) {
            request.put(ARG_DB_SORTBY, dbInfo.getDfltSortColumn().getName());
            request.put(ARG_DB_SORTDIR, dbInfo.getDfltSortAsc()
                                        ? "asc"
                                        : "desc");
        }


        boolean doGroupBy = isGroupBy(request);
        if ( !doGroupBy) {
            if (request.defined(ARG_DB_SORTBY)) {
                String by     = request.getString(ARG_DB_SORTBY, "");
                Column column = getColumn(by);
                if (column != null) {
                    by = column.getSortByColumn();
                    boolean asc = request.getString(ARG_DB_SORTDIR,
                                      "asc").equals("asc");
                    extra += " order by " + by + (asc
                            ? " asc "
                            : " desc ");
                }
            }
        }
        int max  = getMax(request);
        int skip = request.get(ARG_SKIP, 0);
        extra += getDatabaseManager().getLimitString(skip, max);

        return readValues(request, clause, extra, max);
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     */
    private int getMax(Request request) {
        return request.get(ARG_MAX, DEFAULT_MAX);
    }


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param clause _more_
     * @param extra _more_
     * @param max _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private List<Object[]> readValues(Request request, Clause clause,
                                      String extra, int max)
            throws Exception {

        List<Object[]> result    = new ArrayList<Object[]>();

        boolean        doGroupBy = isGroupBy(request);
        List<String>   colNames;
        List<Column>   groupByColumns = null;
        List<String>   aggColumns     = null;
        List<String>   aggLabels      = null;
        List<String>   aggSelectors   = null;

        if (doGroupBy) {
            colNames       = new ArrayList<String>();
            groupByColumns = new ArrayList();
            extra          = " GROUP BY ";
            String       orderBy = null;

            List<String> cols    = new ArrayList<String>();
            List<String> labels  = new ArrayList<String>();
            List<String> args = (List<String>) (request.get(ARG_GROUPBY,
                                    new ArrayList()));
            for (int i = 0; i < args.size(); i++) {
                String  col           = args.get(i);
                Column  groupByColumn = getColumn(col);
                boolean doYear        = false;
                if ((groupByColumn == null) && col.startsWith("year(")) {
                    String tmp = col.substring(5);
                    tmp           = tmp.substring(0, tmp.length() - 1);
                    groupByColumn = getColumn(tmp);
                    doYear        = true;
                    col = getRepository().getDatabaseManager().getExtractYear(
                        tmp);
                    //                    extract (year from col)
                    orderBy = " ORDER BY " + col + " "
                              + request.getEnum(ARG_DB_SORTDIR, "asc", "asc",
                                  "desc");
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
            for (int i = 0; i < 3; i++) {
                Column aggColumn = getColumn(request.getString(ARG_AGG + i,
                                       ""));
                if (aggColumn != null) {
                    aggColumns.add(aggColumn.getName());
                    aggLabels.add(aggColumn.getLabel());
                    aggSelectors.add(request.getEnum(ARG_AGG_TYPE + i, "sum",
                            "count", "min", "max", "avg"));
                }
            }
            if (aggColumns.size() == 0) {
                aggColumns.add(colNames.get(0));
                aggLabels.add(labels.get(0));
                aggSelectors.add("sum");
            }

            for (int i = 0; i < aggColumns.size(); i++) {
                String aggColumn   = aggColumns.get(i);
                String agg         = aggSelectors.get(i);
                String aggSelector = agg + "(" + aggColumn + ") ";
                colNames.add(aggSelector);
                if (orderBy == null) {
                    orderBy = " ORDER BY " + aggSelector
                              + (request.getString(ARG_DB_SORTDIR,
                                  "asc").equals("asc")
                                 ? " asc "
                                 : " desc ");
                }
                String label = agg;
                if (label.equals("avg")) {
                    label = "average";
                }
                label = StringUtil.camelCase(label);
                if(!label.equals("Count")) 
                    label = label + " of " + aggLabels.get(i);
                labels.add(label);
            }
            result.add(labels.toArray());
            extra += StringUtil.join(",", cols);
            extra += orderBy;
        } else {
            colNames = tableHandler.getColumnNames();
        }
        boolean forTable = request.getString(ARG_DB_VIEW,
                                             VIEW_TABLE).equals(VIEW_TABLE);

        //        System.err.println("Clause:" + clause);
        //        System.err.println("cols:" + SqlUtil.comma(colNames));
        Statement stmt = getDatabaseManager().select(SqlUtil.comma(colNames),
                             Misc.newList(tableHandler.getTableName()),
                             clause, extra, max);

        try {
            SqlUtil.Iterator iter = getDatabaseManager().getIterator(stmt);
            ResultSet        results;

            while ((results = iter.getNext()) != null) {
                int valueIdx = 2;
                if (doGroupBy) {
                    Object[] values =
                        new Object[aggColumns.size() + groupByColumns.size()];
                    for (int i = 0; i < groupByColumns.size(); i++) {
                        Column column = groupByColumns.get(i);
                        String v      = results.getString(i + 1);
                        if (forTable && column.isEnumeration()) {
                            //xxxxx
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
                    result.add(values);
                } else {
                    Object[] values = tableHandler.makeEntryValueArray();
                    for (Column column : getColumns()) {
                        valueIdx = column.readValues(myEntry, results,
                                values, valueIdx);
                    }
                    result.add(values);
                }
            }
        } finally {
            getRepository().getDatabaseManager().closeAndReleaseConnection(
                stmt);
        }

        return result;

    }



    /**
     * _more_
     *
     * @param request _more_
     * @param statement _more_
     * @param id _more_
     * @param parent _more_
     * @param values _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void deleteEntry(Request request, Statement statement, String id,
                            Entry parent, Object[] values)
            throws Exception {
        super.deleteEntry(request, statement, id, parent, values);
        String query = SqlUtil.makeDelete(tableHandler.getTableName(),
                                          COL_ID, SqlUtil.quote(id));

        statement.execute(query);
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param sb _more_
     *
     * @return _more_
     */
    public String makeForm(Request request, Entry entry, Appendable sb) {
        String formId  = HtmlUtils.getUniqueId("entryform_");
        String formUrl = request.makeUrl(getRepository().URL_ENTRY_SHOW);
        Utils.append(sb, HtmlUtils.uploadForm(formUrl, HtmlUtils.id(formId)));
        Utils.append(sb, HtmlUtils.hidden(ARG_ENTRYID, entry.getId()));

        return formId;
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param dbid _more_
     * @param forEdit _more_
     * @param doAnonForm _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result handleForm(Request request, Entry entry, String dbid,
                             boolean forEdit, boolean doAnonForm)
            throws Exception {

        List<String>  colNames = tableHandler.getColumnNames();
        StringBuilder sb       = new StringBuilder();
        addViewHeader(request, entry, sb, ((dbid == null)
                                           ? VIEW_NEW
                                           : ""), 0, false);

        StringBuilder formBuffer = new StringBuilder();

        String        formId     = makeForm(request, entry, formBuffer);


        Object[]      values     = null;
        if (dbid != null) {
            values = getValues(entry, dbid);
            formBuffer.append(HtmlUtils.hidden(ARG_DBID, dbid));
            formBuffer.append(HtmlUtils.hidden(ARG_DBID_SELECTED, dbid));
        }



        StringBuilder buttons = new StringBuilder();
        if (doAnonForm) {
            buttons.append(HtmlUtils.submit(msg("Submit"), ARG_DB_CREATE));
        } else if (forEdit) {
            if (dbid == null) {
                buttons.append(HtmlUtils.submit(msg("Create entry"),
                        ARG_DB_CREATE));
                buttons.append(HtmlUtils.buttonSpace());
            } else {
                buttons.append(HtmlUtils.submit(msg("Edit entry"),
                        ARG_DB_EDIT));
                buttons.append(HtmlUtils.buttonSpace());
                buttons.append(HtmlUtils.submit(msg("Copy entry"),
                        ARG_DB_COPY));
                buttons.append(HtmlUtils.buttonSpace());
                buttons.append(HtmlUtils.submit(msg("Delete entry"),
                        ARG_DB_DELETE));
                buttons.append(HtmlUtils.buttonSpace());
            }
            buttons.append(HtmlUtils.submit(msg("Cancel"), ARG_DB_LIST));
            buttons.append(HtmlUtils.buttonSpace());
        }



        formBuffer.append(buttons);
        formBuffer.append(HtmlUtils.formTable());
        FormInfo formInfo = new FormInfo(formId);
        tableHandler.addColumnsToEntryForm(request, formBuffer, entry,
                                           values, formInfo, this);

        formBuffer.append(HtmlUtils.formTableClose());
        formBuffer.append(buttons);
        formBuffer.append(HtmlUtils.formClose());

        StringBuilder validateJavascript = new StringBuilder("");
        formInfo.addJavascriptValidation(validateJavascript);
        String script = JQuery.ready(JQuery.submit(JQuery.id(formId),
                            validateJavascript.toString()));
        formBuffer.append(HtmlUtils.script(script));

        if (doAnonForm) {
            sb.append(entry.getDescription() + HtmlUtils.p() + formBuffer);
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


    /**
     * _more_
     *
     * @param entry _more_
     * @param dbid _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Object[] getValues(Entry entry, String dbid) throws Exception {
        Object[] values = ((dbid != null)
                           ? tableHandler.getValues(myEntry,
                               makeClause(entry, dbid))
                           : tableHandler.makeEntryValueArray());

        return values;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param sb _more_
     * @param formBuffer _more_
     */
    public void createBulkForm(Request request, Entry entry, Appendable sb,
                               Appendable formBuffer) {
        DbInfo        dbInfo = getDbInfo();
        StringBuilder bulkSB = new StringBuilder();
        makeForm(request, entry, bulkSB);
        StringBuilder bulkButtons = new StringBuilder();
        bulkButtons.append(HtmlUtils.submit(msg("Create entries"),
                                            ARG_DB_CREATE));
        bulkButtons.append(HtmlUtils.submit(msg("Cancel"), ARG_DB_LIST));
        bulkSB.append(bulkButtons);
        bulkSB.append(HtmlUtils.p());
        bulkSB.append(msgLabel("Upload a file"));
        bulkSB.append(HtmlUtils.formTable());
        bulkSB.append(
            HtmlUtils.formEntry(
                msgLabel("File"),
                HtmlUtils.fileInput(ARG_DB_BULK_FILE, HtmlUtils.SIZE_60)));
        if (request.getUser().getAdmin()) {
            bulkSB.append(
                HtmlUtils.formEntry(
                    msgLabel("Or server side file"),
                    HtmlUtils.input(
                        ARG_DB_BULK_LOCALFILE, "", HtmlUtils.SIZE_60)));
        }
        bulkSB.append(
            HtmlUtils.formEntry(
                msgLabel("Delimiter"),
                HtmlUtils.input(ARG_DB_BULK_DELIMITER, ",", HtmlUtils.SIZE_5)
                + HtmlUtils.space(2) + msgLabel("# Header Lines")
                + HtmlUtils.input(ARG_DB_BULK_SKIP, "1", HtmlUtils.SIZE_5)));

        bulkSB.append(HtmlUtils.formTableClose());
        bulkSB.append(HtmlUtils.p());
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
        bulkSB.append(HtmlUtils.br());
        bulkSB.append(HtmlUtils.textArea(ARG_DB_BULK_TEXT, "", 10, 80));
        bulkSB.append(HtmlUtils.p());
        bulkSB.append(bulkButtons);
        bulkSB.append(HtmlUtils.formClose());
        List<String> tabTitles = (List<String>) Misc.newList(msg("Form"),
                                     msg("Bulk Create"));
        List<String> tabContents =
            (List<String>) Misc.newList(formBuffer.toString(),
                                        bulkSB.toString());
        String contents = OutputHandler.makeTabs(tabTitles, tabContents,
                              true);
        Utils.append(sb, contents);
    }


    /**
     * _more_
     *
     * @param html _more_
     *
     * @return _more_
     */
    private String insetHtml(Object html) {
        return HtmlUtils.insetDiv(html.toString(), 0, 10, 0, 10);
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param dbid _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result handleView(Request request, Entry entry, String dbid)
            throws Exception {
        boolean       asXml = request.getString("result", "").equals("xml");
        StringBuilder sb    = new StringBuilder();
        if ( !asXml) {
            addViewHeader(request, entry, sb, "", 0, false);
        }


        Object[] values = getValues(entry, dbid);

        getHtml(request, sb, entry, values);
        if ( !asXml) {
            addViewFooter(request, entry, sb);
        }
        if (asXml) {
            StringBuilder xml = new StringBuilder("<contents>\n");
            XmlUtils.appendCdata(xml, sb.toString());
            xml.append("</contents>");

            return new Result("", xml, "text/xml");
        }

        return new Result(getTitle(request, entry), sb);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     * @param entry _more_
     * @param values _more_
     *
     * @throws Exception _more_
     */
    public void getHtml(Request request, StringBuilder sb, Entry entry,
                        Object[] values)
            throws Exception {
        sb.append(HtmlUtils.formTable());
        SimpleDateFormat sdf = getDateFormat(entry);
        for (Column column : getColumns(true)) {
            if ( !isDataColumn(column)) {
                continue;
            }
            if ( !column.getCanDisplay()) {
                continue;
            }
            StringBuilder tmpSb = new StringBuilder();
            formatTableValue(request, entry, tmpSb, column, values, sdf);
            String tmp = tmpSb.toString();
            tmp = tmp.replaceAll("'", "&apos;");
            sb.append(formEntry(request, column.getLabel() + ":", tmp));
        }
        sb.append(HtmlUtils.formTableClose());

    }

    /**
     * _more_
     *
     * @param entry _more_
     * @param values _more_
     * @param sdf _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getLabel(Entry entry, Object[] values, SimpleDateFormat sdf)
            throws Exception {
        String lbl = getLabelInner(entry, values, sdf);
        if ( !Utils.stringDefined(lbl)) {
            lbl = "---";
        }

        return lbl;
    }

    /**
     * _more_
     *
     * @param entry _more_
     * @param values _more_
     * @param sdf _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getKmlLabel(Entry entry, Object[] values,
                              SimpleDateFormat sdf)
            throws Exception {
        return getLabel(entry, values, sdf);
    }

    /**
     * _more_
     *
     * @param entry _more_
     * @param values _more_
     * @param sdf _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getMapLabel(Entry entry, Object[] values,
                              SimpleDateFormat sdf)
            throws Exception {
        return getLabel(entry, values, sdf);
    }


    /**
     * _more_
     *
     * @param entry _more_
     * @param values _more_
     * @param sdf _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getCalendarLabel(Entry entry, Object[] values,
                                   SimpleDateFormat sdf)
            throws Exception {
        return getLabel(entry, values, sdf);
    }

    /**
     * _more_
     *
     * @param entry _more_
     * @param values _more_
     * @param sdf _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getLabelInner(Entry entry, Object[] values,
                                SimpleDateFormat sdf)
            throws Exception {
        DbInfo        dbInfo = getDbInfo();
        StringBuilder sb     = new StringBuilder();
        if (dbInfo.getLabelColumns() != null) {
            for (Column labelColumn : dbInfo.getLabelColumns()) {
                labelColumn.formatValue(entry, sb, Column.OUTPUT_HTML,
                                        values, sdf, false);
                sb.append(" ");
            }

            return sb.toString().trim();
        }
        for (Column column : getColumns()) {
            if ( !isDataColumn(column)) {
                continue;
            }
            String type = column.getType();
            if (type.equals(Column.DATATYPE_STRING)
                    || type.equals(Column.DATATYPE_ENUMERATION)
                    || type.equals(Column.DATATYPE_URL)
                    || type.equals(Column.DATATYPE_EMAIL)
                    || type.equals(Column.DATATYPE_ENUMERATIONPLUS)) {
                column.formatValue(entry, sb, Column.OUTPUT_HTML, values,
                                   false);
                String label = sb.toString();
                if (label.length() > 0) {
                    return label;
                }
            }
        }

        return "";

    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param dbid _more_
     * @param columns _more_
     * @param values _more_
     * @param sdf _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private String getHtml(Request request, Entry entry, String dbid,
                           List<Column> columns, Object[] values,
                           SimpleDateFormat sdf)
            throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append(HtmlUtils.formTable());
        int valueIdx = 0;
        //        String url = getViewUrl(request,  entry,  dbid);
        //        sb.append(HtmlUtils.formEntry(HtmlUtils.href(url,
        //                                                   HtmlUtils.img(getRepository().getUrlBase()+"/db/database_go.png",msg("View entry"))),""));

        for (Column column : columns) {
            if ( !isDataColumn(column)) {
                continue;
            }
            StringBuilder tmpSb = new StringBuilder();
            formatTableValue(request, entry, tmpSb, column, values, sdf);
            //            column.formatValue(entry, tmpSb, Column.OUTPUT_HTML, values);
            String tmp = tmpSb.toString();
            tmp = tmp.replaceAll("'", "&apos;");
            sb.append(formEntry(request, column.getLabel() + ":", tmp));
        }
        sb.append(HtmlUtils.formTableClose());

        return sb.toString();
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public RecordFile doMakeRecordFile(Request request, Entry entry)
            throws Exception {
        return new DbRecordFile(request, entry);
    }


    /**
     * Class description
     *
     *
     * @version        $version$, Sat, Dec 8, '18
     * @author         Enter your name here...
     */
    public class DbRecordFile extends CsvFile {

        /** _more_ */
        private Request request;

        /** _more_ */
        private Entry entry;

        /**
         * _more_
         *
         * @param request _more_
         * @param entry _more_
         *
         * @throws IOException _more_
         */
        public DbRecordFile(Request request, Entry entry) throws IOException {
            this.request = request;
            this.entry   = entry;
        }

        /**
         * _more_
         *
         * @param buffered _more_
         *
         * @return _more_
         *
         * @throws IOException _more_
         */
        @Override
        public InputStream doMakeInputStream(boolean buffered)
                throws IOException {
            try {
                SimpleDateFormat sdf =
                    RepositoryUtil.makeDateFormat("yyyyMMdd'T'HHmmss");
                StringBuilder s     =
                    new StringBuilder("#converted stream\n");
                List<Clause>  where = new ArrayList<Clause>();
                where.add(Clause.eq(COL_ID, entry.getId()));
                StringBuilder searchCriteria = new StringBuilder();
                for (Column column : getColumns()) {
                    column.assembleWhereClause(request, where,
                            searchCriteria);
                }

                List<Object[]> valueList = readValues(request, entry,
                                               Clause.and(where));
                for (Object[] list : valueList) {
                    int cnt = 0;
                    for (int i = IDX_MAX_INTERNAL + 1; i < list.length; i++) {
                        if (cnt > 0) {
                            s.append(",");
                        }
                        cnt++;
                        Object o = list[i];
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
                                s.append('"');
                                s.append(str);
                                s.append('"');
                            } else {
                                s.append(str);
                            }
                        } else if (o instanceof Date) {
                            Date dttm = (Date) o;
                            s.append(sdf.format(dttm));
                        } else {
                            s.append(o);
                        }
                    }
                    s.append("\n");
                }
                //                System.err.println(s);
                ByteArrayInputStream bais =
                    new ByteArrayInputStream(s.toString().getBytes());

                return bais;
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }
        }

        /**
         * _more_
         *
         * @param visitInfo _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        public VisitInfo prepareToVisit(VisitInfo visitInfo)
                throws Exception {
            super.prepareToVisit(visitInfo);

            DbInfo        dbInfo = getDbInfo();
            StringBuilder fields = new StringBuilder();
            for (int i = 0; i < dbInfo.getColumnsToUse().size(); i++) {
                if (i > 0) {
                    fields.append(",");
                }
                Column column  = dbInfo.getColumnsToUse().get(i);
                String colType = column.getType();
                String type    = colType.equals(Column.DATATYPE_INT)
                                 ? RecordField.TYPE_INT
                                 : column.isNumeric()
                                   ? RecordField.TYPE_DOUBLE
                                   : colType.equals(Column.DATATYPE_DATE)
                                     ? RecordField.TYPE_DATE
                                     : RecordField.TYPE_STRING;
                String extra   = "";
                if (column.isNumeric()) {
                    extra += attrChartable();
                } else if (type.equals(RecordField.TYPE_DATE)) {
                    extra += " " + attrFormat("yyyyMMdd'T'HHmmss");
                } else if (column.getName().equals("latitude")) {
                    type = RecordField.TYPE_DOUBLE;
                } else if (column.getName().equals("longitude")) {
                    type = RecordField.TYPE_DOUBLE;
                }
                if (colType.equals(Column.DATATYPE_LATLON)) {
                    fields.append(
                        makeField(
                            "latitude", attrType(RecordField.TYPE_DOUBLE),
                            attrLabel("Latitude")));
                    fields.append(",");
                    fields.append(
                        makeField(
                            "longitude", attrType(RecordField.TYPE_DOUBLE),
                            attrLabel("Longitude")));
                } else {
                    fields.append(makeField(column.getName(), attrType(type),
                                            attrLabel(column.getLabel()),
                                            extra));
                }
                /*
                makeField("point_altitude", attrType(RecordField.TYPE_DOUBLE),
                          attrChartable(),
                          attrUnit("feet"),
                          ("Elevation")),
                makeField("grade", attrType("RecordField.TYPE_DOUBLE),
                          attrChartable(),
                          attrUnit("%"),
                          attrLabel("Grade")),
                makeField("elevation_gain", attrType(RecordField.TYPE_DOUBLE),
                          attrChartable(),
                          attrUnit("feet"),
                          attrLabel("Elevation Gain")),
                */
                //            makeField("latitude", attrType(RecordField.TYPE_DOUBLE),
                //                      attrLabel("Latitude")),
                //            makeField("longitude", attrType(RecordField.TYPE_DOUBLE),
                //                      attrLabel("Longitude")),
            }
            putProperty(PROP_FIELDS, fields.toString());

            return visitInfo;
        }




    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param prop _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    @Override
    public String getChartProperty(Request request, Entry entry, String prop,
                                   String dflt) {
        if (prop.equals("chart.type")) {
            return "table";
        }

        return getTypeProperty(prop, dflt);
    }


    /**
     * Class description
     *
     *
     * @version        $version$, Wed, Jul 17, '19
     * @author         Enter your name here...
     */
    public static class DbTemplate {

        /** _more_ */
        String id;

        /** _more_ */
        String name;

        /** _more_ */
        String entry;

        /** _more_ */
        String prefix;

        /** _more_ */
        String suffix;

        /** _more_ */
        String mime;

        /**
         * _more_
         *
         * @param node _more_
         */
        public DbTemplate(Element node) {
            id     = XmlUtil.getAttribute(node, "id");
            name   = XmlUtil.getAttribute(node, "name", id);
            entry  = XmlUtil.getGrandChildText(node, "contents");
            prefix = XmlUtil.getGrandChildText(node, "prefix");
            suffix = XmlUtil.getGrandChildText(node, "suffix");
            mime   = XmlUtil.getAttribute(node, "mimetype", (String) null);
        }
    }

}
