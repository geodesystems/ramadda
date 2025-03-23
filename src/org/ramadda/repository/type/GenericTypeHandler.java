/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.type;

import org.ramadda.repository.*;

import org.ramadda.repository.database.*;

import org.ramadda.repository.output.*;

import org.ramadda.repository.util.FileWriter;
import org.ramadda.util.FormInfo;
import org.ramadda.util.GroupedBuffers;
import org.ramadda.util.NamedBuffer;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;

import org.ramadda.util.sql.Clause;
import org.ramadda.util.sql.SqlUtil;

import org.w3c.dom.*;

import ucar.unidata.util.DateUtil;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.util.WrapperException;
import ucar.unidata.xml.XmlUtil;

import java.io.File;

import java.lang.reflect.*;

import java.sql.PreparedStatement;

import java.sql.ResultSet;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

@SuppressWarnings("unchecked")
public class GenericTypeHandler extends TypeHandler {

    public static final String ATTR_CLASS = "class";
    public static final String COL_ID = "id";
    private List<Column> myColumns = new ArrayList<Column>();
    private List<Column> allColumns;
    private Column categoryColumn;
    List<String> colNames = new ArrayList<String>();

    /** If true then place this types edit form elements at the beginning */
    private boolean meFirst = false;

    public GenericTypeHandler() {
        super(null);
    }

    public GenericTypeHandler(Repository repository, String type,
                              String description) {
        super(repository, type, description);
    }

    public GenericTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
        if (entryNode != null) {
            initGenericTypeHandler(entryNode);
        }
    }

    public void initGenericTypeHandler(Element entryNode) throws Exception {
        if (getType().indexOf(".") >= 0) {
            //Were screwed - too may types had a . in them
            //            throw new IllegalArgumentException ("Cannot have a '.' in the type name: "+ getType());
        }

        meFirst = XmlUtil.getAttribute(entryNode, "mefirst", meFirst);
        setDefaultCategory(XmlUtil.getAttribute(entryNode, ATTR_CATEGORY,
                (String) null));

        List columnNodes = XmlUtil.findChildren(entryNode, TAG_COLUMN);
        if (columnNodes.size() == 0) {
            return;
        }
	boolean ignoreErrors = XmlUtil.getAttribute(entryNode,"ignoreerrors",true);
        initColumns((List<Element>) columnNodes,ignoreErrors);
    }

    private boolean getMeFirst() {
        if (meFirst) {
            return true;
        }
        if ((getParent() != null)
                && (getParent() instanceof GenericTypeHandler)) {
            return ((GenericTypeHandler) getParent()).getMeFirst();
        }

        return false;
    }

    public void initColumns(List<Element> columnNodes) throws Exception {
	initColumns(columnNodes,true);
    }

    private void initColumns(List<Element> columnNodes, boolean ignoreErrors) throws Exception {
        Statement statement = getDatabaseManager().createStatement();
        colNames.add(COL_ID);
        StringBuilder tableDef = new StringBuilder("CREATE TABLE "
						   + getTableName() + " (\n");

	if(debug) System.err.println("Generic type:" + getTableName());
        tableDef.append(COL_ID + " varchar(200))");
        try {
            getDatabaseManager().executeAndClose(tableDef.toString());
        } catch (Throwable exc) {
            if (exc.toString().indexOf("already exists") < 0) {
                //TODO:
                //                throw new WrapperException(exc);
            }
        }

        StringBuilder indexDef = new StringBuilder();
        indexDef.append("CREATE INDEX " + getTableName() + "_INDEX_" + COL_ID
                        + "  ON " + getTableName() + " (" + COL_ID + ");\n");

        try {
            getDatabaseManager().loadSql(indexDef.toString(), true, false);
        } catch (Throwable exc) {
            //TODO:
            //            throw new WrapperException(exc);
        }

        int     valuesOffset = getValuesOffset();

        boolean showColumns  = false;
        //        showColumns =getType().indexOf("_metadata_")>=0;

	boolean debug = false;
	//	debug = getType().equals("db_boulder_county_voters");
        for (int colIdx = 0; colIdx < columnNodes.size(); colIdx++) {
            Element columnNode = (Element) columnNodes.get(colIdx);
            String className = XmlUtil.getAttribute(columnNode, ATTR_CLASS,
                                   Column.class.getName());
            Class c = Misc.findClass(className);
            Constructor ctor = Misc.findConstructor(c,
                                   new Class[] { getClass(),
                    Element.class, Integer.TYPE });
            Column column = (Column) ctor.newInstance(new Object[] { this,
                    columnNode,
                    Integer.valueOf(valuesOffset + colNames.size() - 1) });
            myColumns.add(column);
            column.setColumnIndex(myColumns.size() - 1);
	    if(debug) System.err.println("column:" +column+" offset:"+ column.getOffset());
            if ((categoryColumn == null) && column.getIsCategory()) {
                categoryColumn = column;
            }
            colNames.addAll(column.getColumnNames());
            column.createTable(statement,ignoreErrors);
            if (showColumns) {
                String NAME = column.getName().toUpperCase();
                System.out.println("public static final int IDX_" + NAME
                                   + "  = " + colIdx + ";");
            }
        }
        getDatabaseManager().closeAndReleaseConnection(statement);
        //TODO: Run through the table and delete any columns and indices that aren't defined anymore
    }

    public List<String> getColumnNames() {
        return colNames;
    }

    public List<Column> getMyColumns() {
        return myColumns;
    }

    @Override
    public List<Column> getColumns() {
        if (allColumns == null) {
            List<Column> tmp = new ArrayList<Column>();
            if (getParent() != null) {
                List<Column> parentColumns = getParent().getColumns();
                if (parentColumns != null) {
                    tmp.addAll(parentColumns);
                }
            }
            if (myColumns != null) {
                tmp.addAll(myColumns);
            }
            allColumns = tmp;
        }

        return allColumns;
    }

    @Override
    public TwoFacedObject getCategory(Request request,Entry entry, String categoryType) {
	Column column = findColumn(categoryType);
	if(column==null) column = categoryColumn;
        if (column != null) {
	    Object s = entry.getValue(request,column);
	    if (s != null) {
		String label = column.getEnumLabel(s.toString());
		return new TwoFacedObject(label, s);
            }
        }

        return super.getCategory(request,entry,categoryType);
    }

    /**
     * Find the Column with the given name
     *
     * @param columnName column name
     *
     * @return Column  or throws exception
     */
    @Override
    public Column findColumn(String columnName) {
        for (Column column : getColumns()) {
            if (column.getName().equals(columnName)) {
                return column;
            }
        }

        return null;
    }

    @Override
    public Column findColumn(int index) {
	List<Column> columns = getColumns();
	if(index<0 || index>=columns.size()) return null;
	return columns.get(index);
    }

    /**
     * create  the entry value array and populate it with any column values stored in the map argument
     *
     * @param map column values
     *
     * @return entry vales array
     */
    @Override
    public Object[] makeEntryValues(Hashtable map) {
        Object[] values = makeEntryValueArray();
        if (map != null) {
            for (Column column : getColumns()) {
                Object data = map.get(column.getName());
                if (data == null) {
                    continue;
                }
                values[column.getOffset()] = data;
            }
        }

        return values;
    }

    public Object convert(String columnName, String value) {
        Column column = findColumn(columnName);
        if (column == null) {
            System.err.println("typeHandler:" + this + " can't find column:"
                               + columnName);

            return value;
        }

        return column.convert(value);
    }

    private boolean haveDatabaseTable() {
        return colNames.size() > 0;
    }

    public int getNumberOfMyValues() {
        if ( !haveDatabaseTable()) {
            return 0;
        }

        return colNames.size() - 1;
    }

    public Object[] makeEntryValueArray() {
        int numberOfValues = getTotalNumberOfValues();

        return new Object[numberOfValues];
    }

    @Override
    public void initializeEntryFromForm(Request request, Entry entry,
                                        Entry parent, boolean newEntry)
            throws Exception {
        Object[] values = getEntryValues(entry);
        if (haveDatabaseTable()) {
            for (Column column : getMyColumns()) {
		setColumnValue(request, entry,  column,values);
            }
        }
        super.initializeEntryFromForm(request, entry, parent, newEntry);
    }

    public void setColumnValue(Request request, Entry entry, Column column,Object[]values)  
            throws Exception {
	column.setValue(request, entry, values);
    }    

    @Override
    public void initializeEntryFromXml(Request request, Entry entry,
                                       Element node,
                                       Hashtable<String, File> files)
            throws Exception {
        //Always call getEntryValues here so we get create the correct size array
        Object[] values = getEntryValues(entry);
        super.initializeEntryFromXml(request, entry, node, files);

        Hashtable<String, Element> nodes    = new Hashtable<String,
                                                  Element>();

        NodeList                   elements = XmlUtil.getElements(node);
        for (int i = 0; i < elements.getLength(); i++) {
            Element child = (Element) elements.item(i);
            nodes.put(child.getTagName(), child);
        }

        for (Column column : getMyColumns()) {
            String  value = null;
            Element child = nodes.get(column.getName());
            if (child != null) {
                value = XmlUtil.getChildText(child);
                if (XmlUtil.getAttribute(child, "encoded", false)) {
                    value = new String(Utils.decodeBase64(value));
                }
            }
            if (value == null) {
                value = XmlUtil.getAttribute(node, column.getName(),
                                             (String) null);
            }
            if (value == null) {
                //                System.err.println (" could not find column value:" + column);
                continue;
            }

            column.setValue(entry, values, value);
        }
    }

    public int matchValue(String arg, Object value, Entry entry) {
        for (Column column : getColumns()) {
            int match = column.matchValue(arg, value, ((entry == null)
                    ? null
                   : entry.getValues()));
            if (match == MATCH_FALSE) {
                return MATCH_FALSE;
            }
            if (match == MATCH_TRUE) {
                return MATCH_TRUE;
            }
        }

        return MATCH_UNKNOWN;
    }

    public List<TwoFacedObject> getListTypes(boolean longName) {
        List<TwoFacedObject> list = super.getListTypes(longName);
        for (Column column : getColumns()) {
            if (column.getCanList()) {
                list.add(new TwoFacedObject((longName
                                             ? (getDescription() + " - ")
                                             : "") + column
                                             .getDescription(), column
                                             .getFullName()));
            }
        }

        return list;
    }

    public Result processList(Request request, String what) throws Exception {
        Column theColumn = null;
        for (Column column : getColumns()) {
            if (column.getCanList() && column.getFullName().equals(what)) {
                theColumn = column;

                break;
            }
        }

        if (theColumn == null) {
            return super.processList(request, what);
        }

        String       column = theColumn.getFullName();
        String       tag    = theColumn.getName();
        String       title  = theColumn.getDescription();
        List<Clause> where  = assembleWhereClause(request);
        Statement statement = select(request, SqlUtil.distinct(column),
                                     where, "");

        String[] values =
            SqlUtil.readString(getDatabaseManager().getIterator(statement),
                               1);
        StringBuilder sb     = new StringBuilder();
        OutputType    output = request.getOutput();
        if (output.equals(OutputHandler.OUTPUT_HTML)) {
            sb.append(RepositoryUtil.header(title));
            sb.append("<ul>");
        } else if (output.equals(XmlOutputHandler.OUTPUT_XML)) {
            sb.append(XmlUtil.XML_HEADER + "\n");
            sb.append(XmlUtil.openTag(tag + "s"));
        }

        Properties properties =
            repository.getFieldProperties(theColumn.getPropertiesFile());
        for (int i = 0; i < values.length; i++) {
            if (values[i] == null) {
                continue;
            }
            String longName = theColumn.getLabel(values[i]);
            if (output.equals(OutputHandler.OUTPUT_HTML)) {
                sb.append("<li>");
                sb.append(longName);
            } else if (output.equals(XmlOutputHandler.OUTPUT_XML)) {
                String attrs = XmlUtil.attrs(ATTR_ID, values[i]);
                if (properties != null) {
                    for (Enumeration keys = properties.keys();
                            keys.hasMoreElements(); ) {
                        String key = (String) keys.nextElement();
                        if (key.startsWith(values[i] + ".")) {
                            String value = (String) properties.get(key);
                            value = value.replace("${value}", values[i]);
                            key   = key.substring((values[i] + ".").length());
                            attrs = attrs + XmlUtil.attr(key, value);
                        }
                    }
                }
                sb.append(XmlUtil.tag(tag, attrs));
            } else if (output.equals(CsvOutputHandler.OUTPUT_CSV)) {
                sb.append(SqlUtil.comma(values[i], longName));
                sb.append("\n");
            }
        }
        if (output.equals(OutputHandler.OUTPUT_HTML)) {
            sb.append("</ul>");
        } else if (output.equals(XmlOutputHandler.OUTPUT_XML)) {
            sb.append(XmlUtil.closeTag(tag + "s"));
        }

        return new Result(
            title, sb,
            repository.getOutputHandler(request).getMimeType(output));
    }

    public boolean equals(Object obj) {
        if ( !super.equals(obj)) {
            return false;
        }

        //TODO
        return true;
    }

    public void deleteEntry(Request request, Statement statement, Entry entry)
            throws Exception {
        deleteEntryFromDatabase(request, statement, entry.getId());
        super.deleteEntry(request, statement, entry);
    }

    @Override
    public void deleteEntry(Request request, Statement statement, String id,
                            Entry parent, Object[] values)
            throws Exception {
        deleteEntryFromDatabase(request, statement, id);
        super.deleteEntry(request, statement, id, parent, values);
    }

    private void deleteEntryFromDatabase(Request request,
                                         Statement statement, String id)
            throws Exception {
        if ( !haveDatabaseTable()) {
            return;
        }
        String query = SqlUtil.makeDelete(getTableName(), COL_ID,
                                          SqlUtil.quote(id));
        statement.execute(query);
    }

    @Override
    public List<Clause> assembleWhereClause(Request request,
                                            Appendable searchCriteria)
            throws Exception {
        List<Clause> where = super.assembleWhereClause(request,
                                 searchCriteria);

        int originalSize = where.size();
        for (Column column : getMyColumns()) {

            /**
             *  For now always check every column as cansearch is used to not show the search field to the user
             *  but may be used internally
             * if ( !column.getCanSearch()) {
             *   continue;
             *   }
             */
            column.assembleWhereClause(request, where, searchCriteria);
        }
        //If I added any here then also add a join on the column "id"
        if ((originalSize != where.size()) && (originalSize > 0)) {
            where.add(Clause.join(Tables.ENTRIES.COL_ID,
                                  getTableName() + ".id"));
        }

        return where;
    }

    @Override
    public void getInsertSql(boolean isNew,
                             List<TypeInsertInfo> typeInserts) {
        super.getInsertSql(isNew, typeInserts);
        if ( !haveDatabaseTable()) {
            return;
        }
        if (isNew) {
            typeInserts.add(
                new TypeInsertInfo(
                    this,
                    SqlUtil.makeInsert(
                        getTableName(), SqlUtil.comma(colNames),
                        SqlUtil.getQuestionMarks(colNames.size()))));
        } else {
            typeInserts.add(
                new TypeInsertInfo(
                    this,
                    SqlUtil.makeUpdate(
                        getTableName(), COL_ID,
                        StringUtil.listToStringArray(colNames))));
        }
    }

    @Override
    public void setStatement(Entry entry, PreparedStatement stmt,
                             boolean isNew)
            throws Exception {
        super.setStatement(entry, stmt, isNew);
        setStatement(entry, getEntryValues(entry), stmt, isNew);
    }

    public int setStatement(Entry entry, Object[] values,
                            PreparedStatement stmt, boolean isNew)
            throws Exception {

        //        System.err.println("setStatement:" + values.length);
        //        for(Object o: values)   System.err.println("  value::" + o);
        int stmtIdx = 1;
        stmt.setString(stmtIdx++, entry.getId());
        if (values != null) {
            for (Column column : getMyColumns()) {
                stmtIdx = column.setValues(stmt, values, stmtIdx);
            }
        }
        if ( !isNew) {
            stmt.setString(stmtIdx, entry.getId());
            stmtIdx++;
        }

        return stmtIdx;
    }

    @Override
    public void initializeEntryFromDatabase(Entry entry) throws Exception {
        //Always call getEntryValues here so we get create the correct size array
        Object[] values = getEntryValues(entry);
        super.initializeEntryFromDatabase(entry);
        if ( !haveDatabaseTable()) {
            return;
        }
        readValuesFromDatabase(entry, values);
    }

    private Object[] readValuesFromDatabase(Entry entry, Object[] values)
            throws Exception {
        Clause clause = Clause.eq(COL_ID, entry.getId());
        Statement stmt = getDatabaseManager().select(SqlUtil.comma(colNames),
                             getTableName(), clause);

        try {
            ResultSet results2 = stmt.getResultSet();
            if (results2.next()) {
                //We start at 2, skipping 1, because the first one is the id
                int valueIdx = 2;
                for (Column column : getMyColumns()) {
		    try {
			valueIdx = column.readValues(entry, results2, values, valueIdx);
		    } catch(Exception exc) {
			String msg = "Error reading column value:" + column +" for entry: " + entry.getName()+" error:" + exc.getMessage();
			throw new IllegalStateException(msg);
		    }
                }
            } else {
                //If we didn't get anything and we have  a db table that means that the entry was created
                //using an old types.xml that did not have any columns defined. 
                if (haveDatabaseTable()) {
                    String sql = SqlUtil.makeInsert(getTableName(), COL_ID,
                                     SqlUtil.getQuestionMarks(1));
                    getLogManager().logInfo(
                        "GenericTypeHandler: inserting id into database:"
                        + getTableName());
                    getDatabaseManager().executeInsert(sql,
                            new Object[] { entry.getId() });
                }
            }
        } finally {
            getDatabaseManager().closeAndReleaseConnection(stmt);
        }

        return values;
    }

    public Object[] getValues(Entry entry, Clause clause) throws Exception {
        return getValues(entry, clause, makeEntryValueArray());
    }

    public Object[] getValues(Entry entry, Clause clause, Object[] values)
            throws Exception {
        Statement stmt = getDatabaseManager().select(SqlUtil.comma(colNames),
                             getTableName(), clause);

        try {
            ResultSet results2 = stmt.getResultSet();
            if (results2.next()) {
                //We start at 2, skipping 1, because the first one is the id
                int valueIdx = 2;
                for (Column column : getMyColumns()) {
                    valueIdx = column.readValues(entry, results2, values,
                            valueIdx);
                }
            }
        } finally {
            getDatabaseManager().closeAndReleaseConnection(stmt);
        }

        return values;
    }

    public void formatColumnHtmlValue(Request request, Entry entry,
                                      Column column, Appendable tmpSb,
                                      Object[] values)
            throws Exception {
        column.formatValue(request, entry, tmpSb, Column.OUTPUT_HTML, values,
                           false);
    }

    @Override
    public void getTextCorpus(Entry entry, Appendable sb, boolean...args) throws Exception {
        super.getTextCorpus(entry, sb,args);
	if(args.length>0 && !args[0]) return;
	Request request = getAdminRequest();
        for (Column column : getMyColumns()) {
            if (column.isPrivate()) {
                continue;
            }
            if (column.isString()) {
                Object s = entry.getValue(request,column);
		if(s!=null) {
		    sb.append(s.toString());
		    sb.append("\n");
		}
            }
        }
    }

    public boolean shouldShowInHtml(Request requst, Entry entry,
                                    OutputType output) {
        return output.equals(OutputHandler.OUTPUT_HTML)
               || output.equals(HtmlOutputHandler.OUTPUT_INLINE);
    }

    @Override
    public String getFieldHtml(Request request, Entry entry, Hashtable props,
                               String name, boolean raw)
            throws Exception {
        Object[] values = getEntryValues(entry);
        if (values != null) {
            for (Column column : getColumns()) {
		if(column.isSynthetic()) {
		    continue;
		}
                if (column.isField(name)) {
                    if (column.isPrivate()) {
                        return null;
                    }
                    if (raw) {
                        Object o = entry.getValue(request, column);
                        if (o != null) {
                            return o.toString();
                        }
                        return null;
                    }
                    StringBuilder tmpSB = new StringBuilder();
                    formatColumnHtmlValue(request, entry, column, tmpSB, values);
                    return tmpSB.toString();
                }
            }
        }

        return super.getFieldHtml(request, entry, props, name, raw);
    }

    @Override
    public void getInnerEntryContent(Entry entry, Request request,
				     TypeHandler typeHandler, OutputType output,
				     boolean showDescription, boolean showResource,
				     boolean linkToDownload, Hashtable props,HashSet<String> seen,
				     boolean forOutput,
				     List<NamedBuffer> contents )
            throws Exception {
        if (typeHandler == null) {
            typeHandler = this;
        }

	if(props==null) props = new Hashtable();
	if(seen==null) seen=new HashSet<String>();

        boolean justBasic =Utils.getProperty(props,"justBasic",false);
        super.getInnerEntryContent(entry, request,
				   typeHandler, output, showDescription,
				   showResource, linkToDownload, props,seen,forOutput,contents);
        if (Misc.equals(props.get("showDetails"), "false") || justBasic) {
            return;
        }

	addColumnsToHtml(request,typeHandler, entry, contents,seen);
    }

    @Override
    public void addColumnsToHtml(Request request, TypeHandler typeHandler,Entry entry,
				 List<NamedBuffer> contents,
				 HashSet<String> seen) throws Exception {
	Object[]      values = getEntryValues(entry);
	if (values != null) {
	    NamedBuffer buff=contents.size()>0?contents.get(0):null;
	    String lastGroup = buff!=null?buff.getName():"";

	    for (Column column : getMyColumns()) {
		if ( !column.getCanShow()) {
		    continue;
		}
		if(seen.contains(column.getName())) {
		    continue;
		}
		if (column.getDisplayGroup() != null 
		    && !Misc.equals(lastGroup, column.getDisplayGroup())) {
		    if(!column.getAdminOnly() ||
		       (request.isAdmin() ||request.isOwner(entry))) {
			lastGroup = column.getDisplayGroup();
			contents.add(buff = new NamedBuffer(lastGroup));
			//			sb.append(HU.row(HU.col(HU.div(lastGroup," class=\"formgroupheader\" "), " colspan=2 ")));
		    }
		}
		if(buff==null) {
		    contents.add(buff=new NamedBuffer(""));
		}
		addColumnToTable(request, entry,column,buff);
	    }

	}

    }

    @Override
    public String addColumnToHtml(Request request, TypeHandler typeHandler,Entry entry,String columnName, Appendable sb, String group) throws Exception {
	Column column =findColumn(columnName);
	if(column==null) return group;
	Object[]      values = getEntryValues(entry);
	if ((column.getDisplayGroup() != null)
	    && !Misc.equals(group, column.getDisplayGroup())) {
	    group = column.getDisplayGroup();
	    sb.append(HU.row(HU.col(HU.div(group," class=\"formgroupheader\" "), " colspan=2 ")));
	}
	addColumnToTable(request, entry,column,sb);
	return group;
    }

    public void addColumnToTable(Request request, Entry entry,Column column,Appendable sb,String...searchArgs) throws Exception {
	if(column==null) return;
	StringBuilder tmpSb = new StringBuilder();
	Object[]      values = getEntryValues(entry);
	if(column.getAdminOnly()) {
	    if(!request.isAdmin() &&!request.isOwner(entry)) return;
	}

	if (values != null) {
	    formatColumnHtmlValue(request, entry, column, tmpSb, values);
	}
	if ( !column.getShowEmpty() && (tmpSb.length() == 0)) {
	    return;
	}

	String subGroup = column.getSubGroup();
	if(subGroup!=null) {
	    HU.formEntry(sb,HU.div(subGroup,HU.clazz("ramadda-entry-subgroup")));
	}

	if (column.getShowLabel()) {
	    String label = tmpSb.toString();
	    if(column.getCanSearch() && (column.isEnumeration()/*||column.isString()*/)&& values!=null) {
		String s = entry.getStringValue(request,column,null);
		if(stringDefined(s)) {
		    String searchUrl = getSearchManager().URL_SEARCH_TYPE + "/"
			+ entry.getTypeHandler().getType();
		    searchUrl = HU.url(searchUrl,"search." +entry.getTypeHandler().getType() +"."+ column.getName(),s);
		    for(int i=0;i<searchArgs.length;i+=2) {
			searchUrl = HU.url(searchUrl,searchArgs[i],searchArgs[i+1]);
		    }

		    label  =HU.href(searchUrl,label);
		}
	    }
	    addEntryProperty(request, sb,column.getDisplayLabel(), label);
	} else {
	    sb.append(HU.row(HU.col(tmpSb.toString(), " colspan=2 ")));
	}
    }

    public String processDisplayTemplate(Request request, Entry entry,
                                         String html)
            throws Exception {
        html = super.processDisplayTemplate(request, entry, html);
        Object[]   values = getEntryValues(entry);
        OutputType output = request.getOutput();
        if (values != null) {
            for (Column column : getMyColumns()) {
                StringBuilder tmpSb = new StringBuilder();
                column.formatValue(request, entry, tmpSb, Column.OUTPUT_HTML,
                                   values, false);
                html = html.replace("${" + column.getName() + ".content}",
                                    tmpSb.toString());
                html = html.replace("${" + column.getName() + ".label}",
                                    column.getLabel());
            }
        }

        return html;
    }

    @Override
    public String getPathForEntry(Request request, Entry entry, boolean forReading)
            throws Exception {
        String path = super.getPathForEntry(request, entry, forReading);
        if (path == null) {
            return path;
        }
        Object[] values = getEntryValues(entry);
        if (values != null) {
            for (Column column : getMyColumns()) {
                StringBuilder tmpSb = new StringBuilder();
                Object        o     = entry.getValue(request,column);
                String        v     = "";
                if (o != null) {
                    v = o.toString();
                }
                path = path.replace("${" + column.getName() + "}", v);
            }
        }

        return path;
    }

    protected List getTablesForQuery(Request request, List initTables) {
        super.getTablesForQuery(request, initTables);
        for (Column column : getMyColumns()) {
            if ( !column.getCanSearch()) {
                continue;
            }
            if (request.defined(column.getFullName())) {
                initTables.add(getTableName());

                break;
            }
        }

        return initTables;
    }

    @Override
    public String getTableName() {
        String typeName = getType();
        //TODO  - clean up the table name
        //        typeName = typeName.replaceAll("\\.","_");
        return typeName;
    }

    @Override
    public void addSpecialToEntryForm(Request request, GroupedBuffers formBuffer,
                                      Entry parentEntry,Entry entry,
                                      FormInfo formInfo,
                                      TypeHandler sourceTypeHandler,HashSet seen)
            throws Exception {
        super.addSpecialToEntryForm(request, formBuffer, parentEntry,entry,
                                    formInfo, sourceTypeHandler,seen);
	addColumnsToEntryForm(request, formBuffer, parentEntry,entry, formInfo,
                              sourceTypeHandler, seen);
    }

    public void addColumnsToEntryForm(Request request, Appendable formBuffer,
                                      Entry parentEntry, Entry entry, FormInfo formInfo,
                                      TypeHandler sourceTypeHandler, HashSet seen)
            throws Exception {
        addColumnsToEntryForm(request, formBuffer, parentEntry,entry, ((entry == null)
                ? null
							   : getEntryValues(entry)), formInfo, sourceTypeHandler,seen);
    }

    public void addColumnsToEntryForm(Request request, Appendable formBuffer,
                                      Entry parentEntry, Entry entry, Object[] values,
                                      FormInfo formInfo,
                                      TypeHandler sourceTypeHandler, HashSet seen)
            throws Exception {
        Hashtable state = new Hashtable();
        for (Column column : getMyColumns()) {
	    String key = "column_"+column.getName();
	    if(seen.contains(key)) continue;
	    seen.add(key);
            addColumnToEntryForm(request, column, formBuffer, parentEntry, entry, values,
                                 state, formInfo, sourceTypeHandler);

        }
    }

    @Override
    public void addToEntryNode(Request request, Entry entry,
                               FileWriter fileWriter, Element node,boolean encode)
            throws Exception {
        super.addToEntryNode(request, entry, fileWriter, node,encode);

        if ( !haveDatabaseTable()) {
            return;
        }
        Object[] values = getEntryValues(entry);
        for (Column column : getMyColumns()) {
            column.addToEntryNode(request,entry, values, node,encode);
        }
    }

    @Override
    public void addToSpecialSearchForm(Request request,
                                       Appendable formBuffer,
                                       HashSet<String> fieldsToShow)
            throws Exception {
        super.addToSpecialSearchForm(request, formBuffer, fieldsToShow);
        List<String> titles   = new ArrayList<String>();
        List<String> contents = new ArrayList<String>();

        addColumnsToSearchForm(request, titles, contents,
                               new ArrayList<Clause>(), false, fieldsToShow);
        if (contents.size() > 0) {
            formBuffer.append(contents.get(0));
        }

    }

    @Override
    public void addToSearchForm(Request request, List<String> titles,
                                List<String> contents, List<Clause> where,
                                boolean advancedForm, boolean showText)
            throws Exception {
        super.addToSearchForm(request, titles, contents, where, advancedForm,
                              showText);
        addColumnsToSearchForm(request, titles, contents, where,
                               advancedForm, null);
    }

    public void addColumnsToSearchForm(Request request, List<String> titles,
                                       List<String> contents,
                                       List<Clause> where,
                                       boolean advancedForm,
                                       HashSet<String> fieldsToShow)
            throws Exception {
        Appendable typeSB = new StringBuilder();
        for (Column column : getMyColumns()) {
            if ((fieldsToShow != null)
                    && !fieldsToShow.contains(column.getName())) {

                continue;
            }
            column.addToSearchForm(request, typeSB, where);
        }

        if (typeSB.toString().length() > 0) {
            if (advancedForm) {
                typeSB = new StringBuilder(HU.formTable() + typeSB
                                           + HU.formTableClose());
            }
            titles.add(msg(getLabel()));
            contents.add(typeSB.toString());
        }
    }

}
