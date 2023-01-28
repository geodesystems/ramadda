/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.db;


import org.ramadda.repository.*;
import org.ramadda.repository.admin.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.OutputHandler;
import org.ramadda.repository.output.OutputType;
import org.ramadda.repository.type.*;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.Utils;


import org.w3c.dom.*;

import ucar.unidata.util.Misc;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;

import java.lang.reflect.*;

import java.util.ArrayList;
import java.util.List;


/**
 *
 */

@SuppressWarnings("unchecked")
public class DbAdminHandler extends AdminHandlerImpl implements RequestHandler,
        DbConstants {

    /** _more_ */
    public static final String TAG_TABLES = "tables";

    /** _more_ */
    public static final String TAG_TABLE = "table";

    /** _more_ */
    public static final String TAG_TEMPLATE = "template";

    /** _more_ */
    public static final String TAG_COLUMN = "column";

    /** _more_ */
    public static final String TAG_PROPERTY = "property";

    /** _more_ */
    public static final String ATTR_HANDLER = "handler";

    /** _more_ */
    public static final String ATTR_ICON = "icon";

    /** _more_ */
    public static final String ATTR_ID = "id";

    /** _more_ */
    public static final String ATTR_NAME = "name";

    /** _more_ */
    public static final String ATTR_CANLIST = "canlist";

    /** _more_ */
    public static final String ATTR_LABEL = "label";

    /** _more_ */
    public static final String ATTR_TYPE = "type";

    /** _more_ */
    public static final String ATTR_CANSEARCH = "cansearch";

    /** _more_ */
    public static final String ATTR_VALUES = "values";

    /** _more_ */
    public static final String ATTR_VALUE = "value";

    /** _more_ */
    public static final String ATTR_ROWS = "rows";

    /** _more_ */
    public static final String ATTR_SIZE = "size";



    /**
     * _more_
     *
     *
     *
     * @param repository _more_
     * @throws Exception _more_
     */
    public DbAdminHandler(Repository repository) throws Exception {
        super(repository);
    }


    /**
     * _more_
     *
     * @param pluginFile _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean loadPluginFile(String pluginFile) throws Exception {
        if ( !pluginFile.endsWith("db.xml")) {
            return false;
        }
        //        System.out.println("DbAdminHandler.init - plugin file:" + pluginFile);
        Element root = XmlUtil.getRoot(pluginFile, getClass());
        if (root == null) {
            System.err.println(
                "DbAdminHandler.init - xml is null for plugin file:"
                + pluginFile);

            return false;
        }
        List children = XmlUtil.findChildren(root, TAG_TABLE);
        for (int i = 0; i < children.size(); i++) {
            Element tableNode = (Element) children.get(i);
            String  tableId   = XmlUtil.getAttribute(tableNode, ATTR_ID);
            Class handlerClass =
                Misc.findClass(XmlUtil.getAttribute(tableNode, ATTR_HANDLER,
                    "org.ramadda.plugins.db.DbTypeHandler"));
            //            System.err.println("class:" + handlerClass);
            Constructor ctor = Utils.findConstructor(handlerClass,
                                   new Class[] { Repository.class,
                    String.class, tableNode.getClass(), String.class });
            if (ctor == null) {
                System.err.println("failed to get ctor:"
                                   + handlerClass.getName() + " "
                                   + XmlUtil.toString(tableNode));

                continue;
            }


            DbTypeHandler typeHandler =
                (DbTypeHandler) ctor.newInstance(new Object[] {
                    getRepository(),
                    tableId, tableNode,
                    XmlUtil.getAttribute(tableNode, ATTR_NAME) });


            TypeHandler baseTypeHandler =
                getRepository().getTypeHandler("type_db_base");
            typeHandler.setParentTypeHandler(baseTypeHandler);
            baseTypeHandler.addChildTypeHandler(typeHandler);

            List<Element> columnNodes =
                (List<Element>) XmlUtil.findChildren(tableNode, TAG_COLUMN);
            Element idNode = XmlUtil.create(TAG_COLUMN, tableNode,
                                            new String[] {
                "name", DbTypeHandler.COL_DBID, Column.ATTR_ISINDEX, "true",
                Column.ATTR_TYPE, "string", Column.ATTR_SHOWINFORM, "false",
                Column.ATTR_CANLIST, "false",Column.ATTR_CANSEARCH,"false"
            });
            Element userNode = XmlUtil.create(TAG_COLUMN, tableNode,
                                   new String[] {
                "name", DbTypeHandler.COL_DBUSER,
                //Column.ATTR_ISINDEX, "true",
                Column.ATTR_TYPE, "string", Column.ATTR_SHOWINFORM, "false",
                Column.ATTR_CANLIST, "false",Column.ATTR_CANSEARCH,"false"
            });

            Element createDateNode = XmlUtil.create(TAG_COLUMN, tableNode,
                                         new String[] {
                "name", DbTypeHandler.COL_DBCREATEDATE,
                //Column.ATTR_ISINDEX,  "true", 
                Column.ATTR_TYPE, "datetime", Column.ATTR_SHOWINFORM, "false",
                Column.ATTR_CANLIST, "false",Column.ATTR_CANSEARCH,"false"
            });

            Element propsNode = XmlUtil.create(TAG_COLUMN, tableNode,
                                    new String[] {
                "name", DbTypeHandler.COL_DBPROPS, Column.ATTR_ISINDEX,
                "false", Column.ATTR_SIZE, "5000", Column.ATTR_TYPE, "string",
                Column.ATTR_SHOWINFORM, "false", 
		Column.ATTR_CANLIST, "false",Column.ATTR_CANSEARCH,"false"
            });


            columnNodes.add(0, propsNode);
            columnNodes.add(0, createDateNode);
            columnNodes.add(0, userNode);
            columnNodes.add(0, idNode);


            List<Element> templates =
                (List<Element>) XmlUtil.findChildren(tableNode, TAG_TEMPLATE);
            for (Element element : templates) {
                typeHandler.addTemplate(new DbTemplate(element));
            }



            //            System.out.println("\tDb:" + typeHandler);
            getRepository().addTypeHandler(tableId, typeHandler, true);
            typeHandler.initDbColumns(columnNodes);

        }

        return true;
    }


    public Result processUploadData(Request request) throws Exception {
	StringBuilder sb = new StringBuilder();
	//44cc70be-fcdb-4488-8775-f7aed132b672

	String id  = request.getString("instrument_id","");
	//Use the admin request because we do our own access checking
	Request adminRequest = getRepository().getAdminRequest();
	Entry entry = getEntryManager().getEntry(adminRequest,id);
	if(entry==null) {
	    sb.append(JsonUtil.map("error",JsonUtil.quote("Cannot find entry:" + id)));
	    return new Result("", new StringBuilder(sb), JsonUtil.MIMETYPE);
	}
	if(!(entry.getTypeHandler() instanceof DbTypeHandler)) {
	    sb.append(JsonUtil.map("error",JsonUtil.quote("Entry is not a database:" + id)));
	    return new Result("", new StringBuilder(sb), JsonUtil.MIMETYPE);
	}
	String key = request.getString("key",null);
	if(key==null) {
	    sb.append(JsonUtil.map("error",JsonUtil.quote("No api key provided")));
	    return new Result("", new StringBuilder(sb), JsonUtil.MIMETYPE);
	}



	DbTypeHandler dbt = (DbTypeHandler) entry.getTypeHandler();
        Object[]      values   = dbt.getValues(entry, (String)null);
        dbt.initializeValueArray(request, null, values);
	List<Column> columns =dbt.getDbInfo().getColumnsToUse(); 
	for (Column column : columns) {
	    String v = request.getString(column.getName(),null);
	    if(v!=null) {
		adminRequest.put(column.getEditArg(),v);
	    }
	    System.err.println(column.getName()+"=" +v);
	}

        for (Column column : columns) {
            column.setValue(adminRequest, entry, values);
        }

	for(Object o:values) {
	    System.err.println("O:" + o);
	}

        dbt.doStore(entry, values, true);
	return new Result("", new StringBuilder(sb), JsonUtil.MIMETYPE);
    }

    /**
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processList(Request request) throws Exception {
        StringBuilder sb = new StringBuilder();
        getPageHandler().sectionOpen(request, sb, "Select Database", false);
        //      let url = HtmlUtils.getUrl("/db/search/list",["type", otherTable,"widgetId",widgetId,"column",column,"otherColumn",otherColumn]);
        List<Entry> entries = getEntryManager().getEntriesFromDb(request);
        if (entries.size() == 0) {
            sb.append("No databases found");
        }
        String searchFrom = request.getString("sourceName", "") + ";"
                            + request.getString("column", "") + ";"
                            + request.getString("widgetId", "") + ";"
                            + request.getString("otherColumn", "");

        if (entries.size() == 1) {
            return new Result(
                HtmlUtils.url(
                    request.makeUrl(getRepository().URL_ENTRY_SHOW),
                    new String[] { ARG_ENTRYID,
                                   entries.get(0).getId(), ARG_SEARCH_FROM,
                                   searchFrom }));
        }
        sb.append(HtmlUtils.formTable());
        List<TwoFacedObject> items = new ArrayList<TwoFacedObject>();
        sb.append(request.form(getRepository().URL_ENTRY_SHOW));
        sb.append(HtmlUtils.hidden(ARG_SEARCH_FROM, searchFrom));
        for (Entry entry : entries) {
            items.add(new TwoFacedObject(entry.getName(), entry.getId()));
        }
        HU.formEntry(sb, msgLabel("Database Entry"),
                     HU.select(ARG_ENTRYID, items));
        sb.append(HtmlUtils.formTableClose());
        sb.append(HU.submit(msg("Search"), ARG_OK));
        sb.append(HtmlUtils.formClose());
        getPageHandler().sectionClose(request, sb);

        return new Result("", sb);
    }


    /**
     * _more_
     *
     * @param type _more_
     *
     * @return _more_
     */
    public String getTableName(String type) {
        return "db_" + type;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getId() {
        return "dbadmin";
    }

}
