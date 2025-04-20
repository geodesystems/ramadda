/**
   Copyright (c) 2008-2025 Geode Systems LLC
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
import java.util.Date;
import java.util.List;
import java.util.function.Function;

@SuppressWarnings("unchecked")
public class DbAdminHandler extends AdminHandlerImpl implements RequestHandler, DbConstants {

    public static final String TAG_TABLES = "tables";
    public static final String TAG_TABLE = "table";
    public static final String TAG_TEMPLATE = "template";
    public static final String TAG_COLUMN = "column";
    public static final String TAG_PROPERTY = "property";
    public static final String ATTR_HANDLER = "handler";
    public static final String ATTR_ICON = "icon";
    public static final String ATTR_ID = "id";
    public static final String ATTR_NAME = "name";
    public static final String ATTR_CANLIST = "canlist";
    public static final String ATTR_LABEL = "label";
    public static final String ATTR_TYPE = "type";
    public static final String ATTR_CANSEARCH = "cansearch";
    public static final String ATTR_VALUES = "values";
    public static final String ATTR_VALUE = "value";
    public static final String ATTR_ROWS = "rows";
    public static final String ATTR_SIZE = "size";

    public DbAdminHandler(Repository repository) throws Exception {
        super(repository);
    }

    public boolean loadPluginFile(String pluginFile) throws Exception {
        if ( !pluginFile.endsWith("db.xml")) {
            return false;
        }
	//	System.out.println("DbAdminHandler.init - plugin file:" + pluginFile);
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

	    Element baseType = XmlUtil.findChild(tableNode,"basetype");	    
	    if(baseType!=null) {
		baseType.setAttribute("name",tableId);
		typeHandler.initTypeHandler(baseType);
		typeHandler.initGenericTypeHandler(baseType);
	    }	    

        }

        return true;
    }

    private Result handleApiError(Request request, String msg, String...args) throws Exception {
	StringBuilder sb = new StringBuilder();
	List<String> values = new ArrayList<String>();
	values.add("error");
	values.add(JsonUtil.quote(msg));
	for(int i=0;i<args.length;i+=2) {
	    values.add(args[i]);
	    values.add(JsonUtil.quote(args[i+1]));
	}
	sb.append(JsonUtil.map(values));

	Result result = new Result("", new StringBuilder(sb), JsonUtil.MIMETYPE);
        result.setResponseCode(Result.RESPONSE_BADRREQUEST);
	return result;
    }

    public Result processUploadData(Request request) throws Exception {
	String id  = request.getString("instrument_id",null);
	if(id==null) id = request.getString(ARG_ENTRYID,null);
	if(id==null) {
	    return handleApiError(request, "No ID provided");
	}
	//Use the admin request because we do our own access checking
	Request adminRequest = getRepository().getAdminRequest();
	Entry entry = getEntryManager().getEntry(adminRequest,id);
	if(entry==null) {
	    return handleApiError(request,"Cannot find entry:" + id);
	}
	if(!(entry.getTypeHandler() instanceof DbTypeHandler)) {
	    return handleApiError(request, "Entry is not a database:" + id);
	}
	String key = request.getString("key",null);
	if(key==null) {
	    return handleApiError(request,"No api key provided");
	}

	String keyValue = getRepository().getProperty("db.apikey." + key,(String) null);
	if(keyValue==null) {
	    return handleApiError(request, "Invalid API key:" + key);
	}
	boolean ok = false;

	//If there is no value set on the property then this api key can write anywhere
	keyValue = keyValue.trim();
	if(keyValue.length()==0) {
	    ok = true;
	} else {
	    //If there is a value then it is a comma separated list of entry IDs of which
	    //this entry has to be one of those IDs or it has to have an ancestor entry
	    List<String> ids = Utils.split(keyValue,",",true,true);
	    ok = checkAccess(request, entry, ids);
	}

	if(!ok) {
	    return handleApiError(request,"Incorrect access");
	}

	DbTypeHandler dbt = (DbTypeHandler) entry.getTypeHandler();
        Object[]      values   = dbt.getValues(entry, (String)null);
        dbt.initializeValueArray(request, null, values);
	List<Column> columns =dbt.getDbInfo().getColumnsToUse(); 
	boolean didOne = false;

	for (Column column : columns) {
	    String v = request.getString(column.getName(),null);
	    if(v!=null) {
		column.setValue(entry, values,v);
		//		adminRequest.put(column.getEditArg(),v);
		didOne=true;
	    }
	}

	if(!didOne) {
	    StringBuilder params = new StringBuilder();
	    for (Column column : columns) {
		if(params.length()>0) params.append(",");
		params.append(column.getName());
	    }
	    return handleApiError(request,"No parameters provider",
				  "parameters", params.toString());
	}

	Date date =null;
	for(Object o:values) {
	    //Get the last date
	    if(o instanceof Date) {
		date = (Date)o;
	    }
	}
	try {
	    dbt.doStore(entry, values, true);
	    //If a date hasn't been set then 
	    if(date!=null) {
		/*
		System.err.println("have date:" + date);
		System.err.println("create date:" +new Date(entry.getCreateDate()));
		System.err.println("start date:" +new Date(entry.getStartDate()));		
		System.err.println("end date:" +new Date(entry.getEndDate()));
		*/
		if(entry.getCreateDate() == entry.getStartDate() &&
		   entry.getStartDate()==entry.getEndDate()) {
		    //		    System.err.println("setting range");
		    entry.setStartDate(date.getTime());
		    entry.setEndDate(date.getTime());		    
		} else {
		    if(date.getTime()<entry.getStartDate()) {
			//			System.err.println("setting start");
			entry.setStartDate(date.getTime());
		    }
		    if(date.getTime()>entry.getEndDate()) {
			//			System.err.println("setting end");
			entry.setEndDate(date.getTime());
		    }
		}
	    }
	    entry.setChangeDate(new Date().getTime());
	    getEntryManager().updateEntry(request, entry);
	} catch(Exception exc) {
	    getLogManager().logError("Error handling /db/upload on entry:" + entry.getName(),exc);
	    return handleApiError(request,exc.getMessage());
	}
	StringBuilder sb = new StringBuilder();
	sb.append(JsonUtil.map("ok",JsonUtil.quote("Values added")));
	sb.append("\n");
	return new Result("", sb, JsonUtil.MIMETYPE);
    }

    private boolean  checkAccess(Request request, Entry entry, List<String>ids) {
	if(entry==null) return false;
	for(String id: ids) {
	    if(entry.getId().equals(id)) return true;
	}
	return checkAccess(request, entry.getParentEntry(), ids);

    }

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
        sb.append(HU.submit(LABEL_SEARCH, ARG_OK));
        sb.append(HtmlUtils.formClose());
        getPageHandler().sectionClose(request, sb);

        return new Result("", sb);
    }

    public String getTableName(String type) {
        return "db_" + type;
    }

    public String getId() {
        return "dbadmin";
    }

}
