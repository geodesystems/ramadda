/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.metameta;


import org.ramadda.repository.*;
import org.ramadda.repository.output.OutputHandler;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;

import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;


/**
 * Holds a collection of fields
 *
 *
 * @author RAMADDA Development Team
 */
public class MetametaDictionaryTypeHandler extends MetametaDictionaryTypeHandlerBase {


    /** _more_ */
    public static final String ARG_METAMETA_BULK = "metameta.bulk";

    /** _more_ */
    public static final String ARG_METAMETA_ENTRY_ADD = "metameta.entry.add";

    /** _more_ */
    public static final String ARG_METAMETA_GENERATE_JAVA =
        "metameta.generate.java";

    /** _more_ */
    public static final String ARG_METAMETA_GENERATE_POINT =
        "metameta.generate.point";



    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public MetametaDictionaryTypeHandler(Repository repository,
                                         Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getChildType() {
        return MetametaFieldTypeHandler.TYPE;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param parent _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public Result getHtmlDisplay(Request request, Entry parent,  Entries children) 
            throws Exception {
        if ( !getEntryManager().canAddTo(request, parent)) {
	    //            return null;
        }

        List<String> titles   = new ArrayList<String>();
        List<String> contents = new ArrayList<String>();

        StringBuffer sb       = new StringBuffer();
        addListForm(request, parent, children.get(), sb);

        titles.add(msg("Fields"));
        contents.add(sb.toString());

        sb.setLength(0);
        sb.append(getBulkForm(request, parent));
        titles.add(msg("Create new fields"));
        contents.add(sb.toString());

        StringBuffer formSB = new StringBuffer();
        getEntryManager().addEntryForm(request, parent, formSB);
        titles.add(msg("Settings"));
        contents.add(formSB.toString());

        sb.setLength(0);
        sb.append(getWikiManager().wikifyEntry(request, parent,
                "<div class=wiki-h2>{{name}} -- {{field name=\"short_name\"}}</div><p>{{description}} <p>\n"));


        StringBuilder html = new StringBuilder();
        getPageHandler().entrySectionOpen(request, parent, html, null);

	html.append(getWikiManager().wikifyEntry(request, parent, parent.getDescription()));
	

        html.append(OutputHandler.makeTabs(titles, contents, false));
        getPageHandler().entrySectionClose(request, parent, html);

        return getEntryManager().addEntryHeader(request, parent,
                new Result("Metameta Dictionary", html));
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
    public Result processEntryAccess(Request request, Entry entry)
            throws Exception {
        if ( !getEntryManager().canAddTo(request, entry)) {
            return null;
        }

        if (request.exists(ARG_METAMETA_GENERATE_POINT)) {
            return handleGeneratePoint(request, entry);

        }


        if (request.exists(ARG_METAMETA_GENERATE_JAVA)) {
            return handleGenerateEntryJava(request, entry);

        }

        if (request.exists(ARG_METAMETA_ENTRY_ADD)) {
            List<Entry> children = getChildrenEntries(request, entry);

            return handleTypeHandlerAdd(request, entry);
        }


        if (request.exists(ARG_METAMETA_BULK)) {
            return handleBulkCreate(request, entry);
        }


        return super.processEntryAccess(request, entry);
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
    public Result handleTypeHandlerAdd(Request request, Entry entry)
            throws Exception {
        //        Repository.debugTypeHandler = true;
        List<Entry>  children = getChildrenEntries(request, entry);

        StringBuffer xml      = new StringBuffer();
        generateEntryXml(request, xml, entry, children);

        Element root = XmlUtil.getRoot(xml.toString());
        //true says to reload the typehandler if its already loaded
        TypeHandler newTypeHandler = getRepository().loadTypeHandlers(root,
								      true,false).get(0);

        request.put(ARG_TYPE, newTypeHandler.getType());

        StringBuffer formSB = new StringBuffer();
        formSB.append(getWikiManager().wikifyEntry(request, entry,
                "<div class=wiki-h2>{{name}} -- {{field name=\"short_name\"}}</div><p>{{description}} <p>\n"));
        String url = getEntryManager().getEntryURL(request, entry);
        formSB.append("You can try this out or "
                      + HtmlUtils.href(url, "keep editing the dictionary"));
        formSB.append(HtmlUtils.p());
        getEntryManager().addEntryForm(request, null, formSB);

        return getEntryManager().addEntryHeader(request, entry,
                new Result("Metameta Dictionary", formSB));


        //        return new Result(url);

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
    public Result handleBulkCreate(Request request, Entry entry)
            throws Exception {
        StringBuffer xml = new StringBuffer(XmlUtil.openTag(TAG_ENTRIES));
        xml.append("\n");
        for (String line :
                StringUtil.split(request.getString(ARG_METAMETA_BULK, ""),
                                 "\n", true, true)) {
            if (line.startsWith("#")) {
                continue;
            }
            List<String> toks = StringUtil.split(line, ",");
            if (toks.size() < 1) {
                continue;
            }
            String id    = toks.get(0);
            String label = ((toks.size() > 1)
                            ? toks.get(1)
                            : id);
            String type  = ((toks.size() > 2)
                            ? toks.get(2)
                            : "string");
            label = label.replace("_", " ");

            StringBuffer properties = new StringBuffer();
            for (int i = 3; i < toks.size(); i++) {
                properties.append(toks.get(i));
                properties.append("\n");
            }

            StringBuffer inner = new StringBuffer();
            inner.append(XmlUtil.tag(MetametaFieldTypeHandler.FIELD_FIELD_ID,
                                     "", XmlUtil.getCdata(id)));
            inner.append(
                XmlUtil.tag(
                    FIELD_PROPERTIES, "",
                    XmlUtil.getCdata(properties.toString())));
            inner.append(XmlUtil.tag(MetametaFieldTypeHandler.FIELD_DATATYPE,
                                     "", XmlUtil.getCdata(type)));
            xml.append(XmlUtil.tag(TAG_ENTRY,
                                   XmlUtil.attrs(ATTR_NAME, label, ATTR_TYPE,
                                       MetametaFieldTypeHandler.TYPE,
                                       ATTR_PARENT,
                                       entry.getId()), inner.toString()));
            xml.append("\n");
        }
        xml.append(XmlUtil.closeTag(TAG_ENTRIES));

        //Create them from XML
        List<Entry> newEntries = getEntryManager().processEntryXml(request,
                                     XmlUtil.getRoot(xml.toString()), entry,
                                     null, new StringBuilder());

        //Now tell them to update again to update their sort order
        for (Entry newEntry : newEntries) {
            if (newEntry.getTypeHandler()
                    instanceof MetametaFieldTypeHandler) {
                ((MetametaFieldTypeHandler) newEntry.getTypeHandler())
                    .setSortOrder(request, newEntry, entry);
                //Insert the updates
                getEntryManager().updateEntry(request, newEntry);
            }
        }

        //Redirect
        String url = request.entryUrl(getRepository().URL_ENTRY_SHOW, entry);

        return new Result(url);

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
    public Result handleGenerateEntryJava(Request request, Entry entry)
            throws Exception {
        String java =
            getStorageManager().readSystemResource(
                "/org/ramadda/plugins/metameta/resources/TypeHandler.template");
        StringBuffer defines = new StringBuffer();
        StringBuffer methods = new StringBuffer();
        String handlerClass = (String) entry.getValue(request,INDEX_HANDLER_CLASS);
        String shortName = (String) entry.getValue(request, INDEX_SHORT_NAME);
        boolean isGroup = ((Boolean) entry.getValue(request, INDEX_ISGROUP)).booleanValue();
        int    idx       = handlerClass.lastIndexOf('.');
        String pkg       = handlerClass.substring(0, idx);
        String className = handlerClass.substring(idx + 1) + "Base";

        java = java.replace("${package}", pkg);
        java = java.replace("${classname}", className);
        java = java.replace("${parentclassname}", isGroup
                ? "ExtensibleGroupTypeHandler"
                : "GenericTypeHandler");


        defines.append("\tpublic static final String TYPE = "
                       + HtmlUtils.quote(shortName) + ";\n");

        defines.append("\tprivate static int INDEX_BASE = 0;\n");
        int cnt = 0;
        for (Entry child : getChildrenEntries(request, entry)) {
            MetametaFieldTypeHandler field =
                (MetametaFieldTypeHandler) child.getTypeHandler();
            String fieldId = (String) child.getValue(request,   field.INDEX_FIELD_ID);
            String FIELDID = fieldId.toUpperCase();
            defines.append("\tpublic static final int INDEX_" + FIELDID
                           + " = INDEX_BASE + " + cnt + ";\n");
            defines.append("\tpublic static final String FIELD_" + FIELDID
                           + " = " + HtmlUtils.quote(fieldId) + ";\n");
            //            methods.append("\tprivate static INDEX_" + FIELDID +" = INDEX_BASE + " + cnt +";\n");
            cnt++;
        }

        java = java.replace("${defines}", defines.toString());
        java = java.replace("${methods}", methods.toString());




        request.setReturnFilename(className + ".java");

        return new Result("Java", new StringBuffer(java), "text/java");
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
    public Result handleGeneratePoint(Request request, Entry entry)
            throws Exception {
        Hashtable    props  = getProperties(request,entry, INDEX_PROPERTIES);
        StringBuffer propSB = getPointProperties(request, entry);
        request.setReturnFilename(entry.getName() + ".properties");

        return new Result("", propSB, "text/properties");
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
    public StringBuffer getPointProperties(Request request, Entry entry)
            throws Exception {
        Hashtable props = getProperties(request,entry, INDEX_PROPERTIES);
        StringBuffer propSB = new StringBuffer("#\n#Generated from "
                                  + entry.getName() + " data dictionary\n");
        int          cnt    = 0;
        StringBuffer fields = new StringBuffer();

        propSB.append(entry.getValue(request, INDEX_PROPERTIES));
        for (Entry child : getChildrenEntries(request, entry)) {
            MetametaFieldTypeHandler field =
                (MetametaFieldTypeHandler) child.getTypeHandler();
            String fieldId = (String) child.getValue(request,  field.INDEX_FIELD_ID);
            String dataType = (String) child.getValue(request,  field.INDEX_DATATYPE);
            String missing = (String) child.getValue(request, field.INDEX_MISSING);
            String unit = (String) child.getValue(request, field.INDEX_UNIT);

            Hashtable fprops  = field.getProperties(request,child);

            String    FIELDID = fieldId.toUpperCase();
            if (cnt > 0) {
                fields.append(", ");
            }
            fields.append(fieldId);
            fields.append("[");
            fields.append(HtmlUtils.attr("type", dataType));
            fields.append(HtmlUtils.attr("label", child.getName()));
            if (Utils.stringDefined(missing)) {
                fields.append(HtmlUtils.attr("missing", missing));
            }
            if (Utils.stringDefined(unit)) {
                fields.append(HtmlUtils.attr("unit", unit));
            }

            for (String propAttr : new String[] { "format", "chartable",
                    "searchable", "value", "pattern" }) {
                String value = (String) fprops.get(propAttr);
                if (value != null) {
                    fields.append(XmlUtil.attr(propAttr, value));
                }
            }


            fields.append("]");

            cnt++;
        }
        propSB.append("fields=" + fields);
        propSB.append("\n");

        return propSB;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param xml _more_
     * @param parent _more_
     * @param children _more_
     *
     * @throws Exception _more_
     */
    public void generateDbXml(Request request, StringBuffer xml,
                              Entry parent, List<Entry> children)
            throws Exception {
        boolean first = xml.length() == 0;
        if (first) {
            xml.append(XmlUtil.openTag("tables", ""));
	    xml.append("\n");
	    xml.append("<!-- This is a generated plugin that defines a database.\nCopy it into the plugins directory in your RAMADDA home directory -->\n");
        }
        String shortName = (String) parent.getValue(request, INDEX_SHORT_NAME);
        String handlerClass = (String) parent.getValue(request, INDEX_HANDLER_CLASS);

        Hashtable props = getProperties(request,parent, INDEX_PROPERTIES);
        String    icon  = Misc.getProperty(props, "icon", "/db/tasks.gif");
        props.remove("icon");


        if ( !Utils.stringDefined(shortName)) {
            shortName = parent.getName();
        }
        if ( !Utils.stringDefined(handlerClass)) {
            handlerClass = "org.ramadda.plugins.db.DbTypeHandler";
        }
	String tableAttrs = XmlUtil.attrs("id", shortName, "name",
					  parent.getName(), ATTR_HANDLER,
					  handlerClass, "icon", icon);
	
	String tmp;
	for(String prop:new String[]{"cansearch","canlist"}) {
	    if((tmp = Misc.getProperty(props, prop, (String) null))!=null) {
		tableAttrs+=XmlUtil.attrs(prop,tmp);
		props.remove(prop);
	    }
	}

        xml.append(XmlUtil.openTag("table", tableAttrs));
	


	xml.append("\n");
        String  wikiText  = (String) parent.getValue(request, INDEX_WIKI_TEXT);
	if(Utils.stringDefined(wikiText)) {
	    xml.append("\n");
            xml.append(XmlUtil.tag("wiki", "", XmlUtil.getCdata(wikiText)));
            xml.append("\n");
	}

        String  basetype = (String) parent.getValue(request,  INDEX_BASETYPE);
	if(Utils.stringDefined(basetype)) {
	    xml.append("<!-- The base type defined the columns and properties for the entry type -->\n");
	    xml.append("<basetype>\n");
	    xml.append(basetype.trim());
	    xml.append("</basetype>\n");

	}


        for (Enumeration keys = props.keys(); keys.hasMoreElements(); ) {
            String key   = (String) keys.nextElement();
            String value = (String) props.get(key);
            xml.append(propertyTag(key, value));
	    xml.append("\n");
        }

	xml.append("<!-- These are the columns of the database -->\n");
        for (Entry recordFieldEntry : children) {
            MetametaFieldTypeHandler field =
                (MetametaFieldTypeHandler) recordFieldEntry.getTypeHandler();
            field.generateDbXml(request, xml, recordFieldEntry);
        }
        xml.append(XmlUtil.closeTag("table"));

        if (first) {
            xml.append(XmlUtil.closeTag("tables"));
        }
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param xml _more_
     * @param parent _more_
     * @param children _more_
     *
     * @throws Exception _more_
     */
    public void generateEntryXml(Request request, StringBuffer xml,
                                 Entry parent, List<Entry> children)
            throws Exception {

        boolean first = xml.length() == 0;
        if (first) {
            xml.append(XmlUtil.openTag(TAG_TYPES, ""));
        }
        boolean isPoint   = isPoint(request, parent);


        String  shortName = (String) parent.getValue(request, INDEX_SHORT_NAME);
        String  superType = (String) parent.getValue(request, INDEX_SUPER_TYPE);
        String  wikiText  = (String) parent.getValue(request, INDEX_WIKI_TEXT);
        String handlerClass = (String) parent.getValue(request,INDEX_HANDLER_CLASS);
        boolean isGroup = ((Boolean) parent.getValue(request,  INDEX_ISGROUP)).booleanValue();
        String propertiesString = (String) parent.getValue(request,     INDEX_PROPERTIES);
        Hashtable props = getProperties(request,parent, INDEX_PROPERTIES);
        if ( !Utils.stringDefined(shortName)) {
            shortName = parent.getName();
        }
        if ( !Utils.stringDefined(handlerClass)) {
            if (isPoint) {
                handlerClass = "org.ramadda.data.services.PointTypeHandler";
            } else {
                if ( !isGroup) {
                    handlerClass =
                        "org.ramadda.repository.type.GenericTypeHandler";
                } else {
                    handlerClass =
                        "org.ramadda.repository.type.ExtensibleGroupTypeHandler";
                }
            }
        }

        StringBuffer attrs = new StringBuffer();
        StringBuffer inner = new StringBuffer();
        if (Utils.stringDefined(wikiText)) {
            inner.append(XmlUtil.tag("wiki", "", XmlUtil.getCdata(wikiText)));
            inner.append("\n");
        }

        if (Utils.stringDefined(superType)) {
            attrs.append(XmlUtil.attrs(ATTR_SUPER, superType));
        }


        attrs.append(XmlUtil.attrs(ATTR_NAME, shortName, ATTR_DESCRIPTION,
                                   parent.getName(), ATTR_HANDLER,
                                   handlerClass));




        String[] attrProps = { ATTR_CHILDTYPES, };
        for (String attrProp : attrProps) {
            String v = (String) getAndRemoveProperty(props, attrProp, null);
            if (v != null) {
                attrs.append(XmlUtil.attr(attrProp, v));
            }
        }


        xml.append(XmlUtil.openTag(TAG_TYPE, attrs.toString()));

        if (isPoint) {
            StringBuffer propSB = getPointProperties(request, parent);
            inner.append(propertyTextTag("record.properties",
                                         propSB.toString()));
            inner.append(propertyTag("record.file.class",
                                     "org.ramadda.data.point.text.CsvFile"));
        } else {
            for (Enumeration keys = props.keys(); keys.hasMoreElements(); ) {
                String key   = (String) keys.nextElement();
                String value = (String) props.get(key);
                inner.append(propertyTag(key, value));
            }
            for (Entry recordFieldEntry : children) {
                MetametaFieldTypeHandler field =
                    (MetametaFieldTypeHandler) recordFieldEntry
                        .getTypeHandler();
                field.generateDbXml(request, xml, recordFieldEntry);
                inner.append("\n");
            }
        }

        xml.append(inner);

        xml.append(XmlUtil.closeTag(TAG_TYPE));


        if (first) {
            xml.append(XmlUtil.closeTag(TAG_TYPES));
        }

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
    public boolean isPoint(Request request, Entry entry) throws Exception {
        if (entry == null) {
            return false;
        }
        String type = (String) entry.getValue(request, INDEX_DICTIONARY_TYPE);

        return Misc.equals(type, "datafile");
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
    public boolean isDatabase(Request request, Entry entry) throws Exception {
        if (entry == null) {
            return false;
        }
        String type = (String) entry.getValue(request, INDEX_DICTIONARY_TYPE);
        return Misc.equals(type, "database");
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
    public boolean isEntry(Request request, Entry entry) throws Exception {
        if (entry == null) {
            return false;
        }
        String type = (String) entry.getValue(request, INDEX_DICTIONARY_TYPE);

        return Misc.equals(type, "entry");
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param column _more_
     * @param widget _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public String getFormWidget(Request request, Entry entry, Column column,
                                String widget)
            throws Exception {
        if (column.getName().equals("properties")) {
            String suffix = "";
            if (isPoint(request, entry)) {
                suffix =
                    "<a href=\"http://ramadda.org/repository/pointdocs/textdata.html\" target=_help>Point data properties</a><br>skiplines=<i>num lines in header to skip</i><br>delimiter=<i> column delimiter</i><br>crs=<i>coordinate reference system</i>";
            } else if (isEntry(request, entry)) {
                suffix =
                    "icon=<i>/path/to/icon</i><br>category=<i>type category</i><br>form.(date,area,location,resource,file,url).show=<i>true|false</i><br>childtypes=<i>preferred child entry types</i><br>";
            }

            return HtmlUtils.hbox(widget, HtmlUtils.inset(suffix, 5));
        }

        return super.getFormWidget(request, entry, column, widget);
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param buttons _more_
     */
    @Override
    public void addEntryButtons(Request request, Entry entry,
                                List<String> buttons) {
        super.addEntryButtons(request, entry, buttons);

        try {
            String handlerClass = (String) entry.getValue(request,  INDEX_HANDLER_CLASS);
            String shortName = (String) entry.getValue(request,  INDEX_SHORT_NAME);
            String type = (String) entry.getValue(request,  INDEX_DICTIONARY_TYPE);
            boolean isPoint    = isPoint(request, entry);
            boolean isEntry    = isEntry(request, entry);
            boolean isDatabase = isDatabase(request, entry);
            if (isPoint || isEntry) {
                TypeHandler typeHandler =
                    getRepository().getTypeHandler(shortName);
                if (typeHandler == null) {
                    buttons.add(HtmlUtils.submit("Add new entry type",
                            ARG_METAMETA_ENTRY_ADD));
                } else {
                    buttons.add(HtmlUtils.submit("Update entry type",
                            ARG_METAMETA_ENTRY_ADD));
                }
            }
            if (isPoint) {
                buttons.add(
                    HtmlUtils.submit(
                        "Generate point data dictionary",
                        ARG_METAMETA_GENERATE_POINT));

            }
            if (isEntry) {
                if (Utils.stringDefined(handlerClass)) {
                    buttons.add(HtmlUtils.submit("Generate Java base class",
                            ARG_METAMETA_GENERATE_JAVA));
                }
            }

            if (isDatabase) {
                buttons.add(HtmlUtils.submit("Generate db.xml",
                                             ARG_METAMETA_GENERATE_DB));
            }

        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }



    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     *
     * @return _more_
     */
    public String propertyTag(String name, String value) {
        return XmlUtil.tag(TAG_PROPERTY,
                           XmlUtil.attrs(ATTR_NAME, name, ATTR_VALUE, value));
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     *
     * @return _more_
     */
    public String propertyTextTag(String name, String value) {
        return XmlUtil.tag(TAG_PROPERTY, XmlUtil.attrs("name", name),
                           XmlUtil.getCdata(value));
    }

    /**
     * _more_
     *
     * @param props _more_
     * @param key _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    private String getAndRemoveProperty(Hashtable props, String key,
                                        String dflt) {
        String value = Misc.getProperty(props, key, dflt);
        props.remove(key);

        return value;
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
    private String getBulkForm(Request request, Entry entry)
            throws Exception {
        if ( !getEntryManager().canAddTo(request, entry)) {
	    //            return null;
        }

        StringBuffer sb = new StringBuffer();
        sb.append(request.form(getRepository().URL_ENTRY_ACCESS));
        sb.append(HtmlUtils.hidden(ARG_ENTRYID, entry.getId()));
        sb.append(HtmlUtils.p());
        sb.append("<table><tr valign=top><td>");
        sb.append(
            HtmlUtils.italics(
                "column_id, label, type,any number of name=value properties"));
        sb.append(HtmlUtils.br());
        sb.append(HtmlUtils.textArea(ARG_METAMETA_BULK, "", 5, 70));
        sb.append("</td><td>");
        sb.append("type: string, int, double<br>");
        sb.append("properties:<br>");
        if (isPoint(request, entry)) {
            sb.append(MetametaFieldTypeHandler.HELP_POINT);
        } else {
            sb.append(MetametaFieldTypeHandler.HELP_ENTRY);
        }
        sb.append("</td></tr></table>");
        sb.append(HtmlUtils.br());
        sb.append(HtmlUtils.submit("Add Fields", "submit"));
        sb.append(HtmlUtils.formClose());

        return sb.toString();
    }




}
