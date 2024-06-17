/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.type;


import org.ramadda.repository.*;

import org.ramadda.repository.database.*;

import org.ramadda.repository.output.*;

import org.ramadda.repository.util.FileWriter;
import org.ramadda.util.FormInfo;
import org.ramadda.util.GroupedBuffers;
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


/**
 * Class TypeHandler _more_
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
@SuppressWarnings("unchecked")
public class GenericTypeHandler extends TypeHandler {

    /** _more_ */
    public static final String ATTR_CLASS = "class";

    /** _more_ */
    public static final String COL_ID = "id";

    /** _more_ */
    private List<Column> myColumns = new ArrayList<Column>();

    /** _more_ */
    private List<Column> allColumns;

    /** _more_ */
    private Column categoryColumn;

    /** _more_ */
    List<String> colNames = new ArrayList<String>();


    /** If true then place this types edit form elements at the beginning */
    private boolean meFirst = false;


    /**
     * ctor
     */
    public GenericTypeHandler() {
        super(null);
    }

    /**
     * _more_
     *
     * @param repository The repository
     * @param type _more_
     * @param description _more_
     */
    public GenericTypeHandler(Repository repository, String type,
                              String description) {
        super(repository, type, description);
    }


    /**
     * _more_
     *
     * @param repository The repository
     * @param entryNode _more_
     *
     * @throws Exception on badness
     */
    public GenericTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
        if (entryNode != null) {
            initGenericTypeHandler(entryNode);
        }
    }



    /**
     * _more_
     *
     * @param entryNode _more_
     *
     * @throws Exception on badness
     */
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
        initColumns((List<Element>) columnNodes);
    }


    /**
     * _more_
     *
     * @return _more_
     */
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


    /**
     * _more_
     *
     * @param columnNodes _more_
     *
     * @throws Exception on badness
     */
    public void initColumns(List<Element> columnNodes) throws Exception {
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
            column.createTable(statement);
            if (showColumns) {
                String NAME = column.getName().toUpperCase();
                System.out.println("public static final int IDX_" + NAME
                                   + "  = " + colIdx + ";");
            }
        }
        getDatabaseManager().closeAndReleaseConnection(statement);
        //TODO: Run through the table and delete any columns and indices that aren't defined anymore
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public List<String> getColumnNames() {
        return colNames;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public List<Column> getMyColumns() {
        return myColumns;
    }



    /**
     * _more_
     *
     * @return _more_
     */
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


    /**
     * _more_
     *
     * @param columnName _more_
     * @param value _more_
     *
     * @return _more_
     */
    public Object convert(String columnName, String value) {
        Column column = findColumn(columnName);
        if (column == null) {
            System.err.println("typeHandler:" + this + " can't find column:"
                               + columnName);

            return value;
        }

        return column.convert(value);
    }





    /**
     * _more_
     *
     * @return _more_
     */
    private boolean haveDatabaseTable() {
        return colNames.size() > 0;
    }


    /**
     * _more_
     *
     * @return _more_
     */
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
        //        System.err.println ("GenericTypeHandler.initFromForm");
        //Always call getEntryValues here so we get create the correct size array
        Object[] values = getEntryValues(entry);
        if (haveDatabaseTable()) {
            for (Column column : getMyColumns()) {
                column.setValue(request, entry, values);
            }
        }
        super.initializeEntryFromForm(request, entry, parent, newEntry);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param node _more_
     * @param files _more_
     *
     * @throws Exception on badness
     */
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



    /**
     * _more_
     *
     * @param arg _more_
     * @param value _more_
     * @param entry _more_
     *
     * @return _more_
     */
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


    /**
     * _more_
     *
     * @param longName _more_
     *
     * @return _more_
     */
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


    /**
     * _more_
     *
     * @param request _more_
     * @param what _more_
     *
     * @return _more_
     *
     * @throws Exception on badness
     */
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




    /**
     * _more_
     *
     * @param obj _more_
     *
     * @return _more_
     */
    public boolean equals(Object obj) {
        if ( !super.equals(obj)) {
            return false;
        }

        //TODO
        return true;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param statement _more_
     * @param entry _more_
     *
     * @throws Exception on badness
     */
    public void deleteEntry(Request request, Statement statement, Entry entry)
            throws Exception {
        deleteEntryFromDatabase(request, statement, entry.getId());
        super.deleteEntry(request, statement, entry);
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
     * @throws Exception on badness
     */
    @Override
    public void deleteEntry(Request request, Statement statement, String id,
                            Entry parent, Object[] values)
            throws Exception {
        deleteEntryFromDatabase(request, statement, id);
        super.deleteEntry(request, statement, id, parent, values);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param statement _more_
     * @param id _more_
     *
     * @throws Exception on badness
     */
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



    /**
     * _more_
     *
     * @param request _more_
     * @param searchCriteria _more_
     *
     * @return _more_
     *
     * @throws Exception on badness
     */
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


    /**
     * _more_
     *
     *
     * @param isNew _more_
     * @param typeInserts _more_
     */
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



    /**
     * _more_
     *
     * @param entry _more_
     * @param stmt _more_
     * @param isNew _more_
     *
     * @throws Exception on badness
     */
    @Override
    public void setStatement(Entry entry, PreparedStatement stmt,
                             boolean isNew)
            throws Exception {
        super.setStatement(entry, stmt, isNew);
        setStatement(entry, getEntryValues(entry), stmt, isNew);
    }


    /**
     * _more_
     *
     * @param entry _more_
     * @param values _more_
     * @param stmt _more_
     * @param isNew _more_
     *
     * @return _more_
     *
     * @throws Exception on badness
     */
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


    /**
     * _more_
     *
     * @param entry _more_
     *
     * @throws Exception on badness
     */
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



    /**
     * _more_
     *
     * @param entry _more_
     * @param values _more_
     *
     * @return _more_
     *
     * @throws Exception on badness
     */
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
                    valueIdx = column.readValues(entry, results2, values,
                            valueIdx);
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

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param column _more_
     * @param tmpSb _more_
     * @param values _more_
     *
     * @throws Exception on badness
     */
    public void formatColumnHtmlValue(Request request, Entry entry,
                                      Column column, Appendable tmpSb,
                                      Object[] values)
            throws Exception {
        column.formatValue(request, entry, tmpSb, Column.OUTPUT_HTML, values,
                           false);
    }


    /**
     * _more_
     *
     * @param entry _more_
     * @param sb _more_
     *
     * @throws Exception on badness
     */
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


    /**
     * _more_
     *
     * @param requst _more_
     * @param entry _more_
     * @param output _more_
     *
     * @return _more_
     */
    public boolean shouldShowInHtml(Request requst, Entry entry,
                                    OutputType output) {
        return output.equals(OutputHandler.OUTPUT_HTML)
               || output.equals(HtmlOutputHandler.OUTPUT_INLINE);
    }


    /**
     *
     *
     * @param request _more_
     * @param entry _more_
     * @param props _more_
     * @param name _more_
     * @param raw _more_
     *
     * @return _more_
     *
     * @throws Exception on badness
     */
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
                    formatColumnHtmlValue(request, entry, column, tmpSB,
                                          values);

                    return tmpSB.toString();
                }
            }
        }

        return super.getFieldHtml(request, entry, props, name, raw);
    }

    /**
     * _more_
     *
     * @param entry _more_
     * @param request _more_
     * @param typeHandler _more_
     * @param output _more_
     * @param showDescription on badness
     * @param showResource _more_
     * @param linkToDownload _more_
     * @param props _more_
     *
     * @return _more_
     *
     * @throws Exception on badness
     */
    @Override
    public void getInnerEntryContent(Entry entry, Request request,
				     TypeHandler typeHandler, OutputType output,
				     boolean showDescription, boolean showResource,
				     boolean linkToDownload, Hashtable props,HashSet<String> seen,
				     boolean forOutput,
				     Appendable sb )
            throws Exception {
        if (typeHandler == null) {
            typeHandler = this;
        }

	if(props==null) props = new Hashtable();
	if(seen==null) seen=new HashSet<String>();

        boolean justBasic =Utils.getProperty(props,"justBasic",false);
        super.getInnerEntryContent(entry, request,
				   typeHandler, output, showDescription,
				   showResource, linkToDownload, props,seen,forOutput,sb);
        if (Misc.equals(props.get("showDetails"), "false") || justBasic) {
            return;
        }

	addColumnsToHtml(request,typeHandler, entry, sb,seen);
    }

    @Override
    public void addColumnsToHtml(Request request, TypeHandler typeHandler,Entry entry, Appendable sb,HashSet<String> seen) throws Exception {
	Object[]      values = getEntryValues(entry);
	if (values != null) {
	    String lastGroup = "";
	    for (Column column : getMyColumns()) {
		if ( !column.getCanShow()) {
		    continue;
		}
		if(seen.contains(column.getName())) {
		    continue;
		}
		if ((column.getDisplayGroup() != null)
		    && !Misc.equals(lastGroup, column.getDisplayGroup())) {
		    lastGroup = column.getDisplayGroup();
		    sb.append(
				  HU.row(
						HU.col(
							      HU.div(
									    lastGroup,
									    " class=\"formgroupheader\" "), " colspan=2 ")));
		}
		addColumnToTable(request, entry,column,sb);
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
	if (values != null) {
	    formatColumnHtmlValue(request, entry, column, tmpSb, values);
	}
	if ( !column.getShowEmpty() && (tmpSb.length() == 0)) {
	    return;
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

	    sb.append(formEntry(request, column.getLabel() + ":", label));
	} else {
	    sb.append(HU.row(HU.col(tmpSb.toString(), " colspan=2 ")));
	}
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param html _more_
     *
     * @return _more_
     *
     * @throws Exception on badness
     */
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


    /**
     * _more_
     *
     * @param request _more_
     * @param initTables _more_
     *
     * @return _more_
     */
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

    /**
     * _more_
     *
     * @return _more_
     */
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


    /**
     * _more_
     *
     * @param request _more_
     * @param formBuffer _more_
     * @param entry _more_
     * @param formInfo _more_
     * @param sourceTypeHandler _more_
     *
     * @throws Exception on badness
     */
    public void addColumnsToEntryForm(Request request, Appendable formBuffer,
                                      Entry parentEntry, Entry entry, FormInfo formInfo,
                                      TypeHandler sourceTypeHandler, HashSet seen)
            throws Exception {
        addColumnsToEntryForm(request, formBuffer, parentEntry,entry, ((entry == null)
                ? null
							   : getEntryValues(entry)), formInfo, sourceTypeHandler,seen);
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param formBuffer _more_
     * @param entry _more_
     * @param values _more_
     * @param formInfo _more_
     * @param sourceTypeHandler _more_
     *
     * @throws Exception on badness
     */
    public void addColumnsToEntryForm(Request request, Appendable formBuffer,
                                      Entry parentEntry, Entry entry, Object[] values,
                                      FormInfo formInfo,
                                      TypeHandler sourceTypeHandler, HashSet seen)
            throws Exception {
        Hashtable state = new Hashtable();
        for (Column column : getMyColumns()) {
	    if(seen.contains(column.getName())) continue;
	    seen.add(column.getName());
            addColumnToEntryForm(request, column, formBuffer, parentEntry, entry, values,
                                 state, formInfo, sourceTypeHandler);

        }
    }



    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     * @param fileWriter _more_
     * @param node _more_
     *
     * @throws Exception on badness
     */
    @Override
    public void addToEntryNode(Request request, Entry entry,
                               FileWriter fileWriter, Element node)
            throws Exception {
        super.addToEntryNode(request, entry, fileWriter, node);


        if ( !haveDatabaseTable()) {
            return;
        }
        Object[] values = getEntryValues(entry);
        for (Column column : getMyColumns()) {
            column.addToEntryNode(request,entry, values, node);
        }
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param formBuffer _more_
     * @param fieldsToShow _more_
     *
     * @throws Exception on badness
     */
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


    /**
     * _more_
     *
     * @param request _more_
     * @param titles _more_
     * @param contents _more_
     * @param where _more_
     * @param advancedForm _more_
     * @param showText _more_
     *
     * @throws Exception on badness
     */
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


    /**
     * _more_
     *
     * @param request _more_
     * @param titles _more_
     * @param contents _more_
     * @param where _more_
     * @param advancedForm _more_
     * @param fieldsToShow _more_
     *
     * @throws Exception on badness
     */
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
