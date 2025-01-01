/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.metameta;


import org.ramadda.repository.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.CategoryBuffer;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;


import org.w3c.dom.Element;

import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.xml.XmlUtil;

import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;


/**
 * Class description
 *
 *
 * @version        $version$, Sat, Dec 28, '13
 * @author         Enter your name here...
 */
public class MetametaApiHandler extends RepositoryManager implements RequestHandler {


    /**
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     * @param props _more_
     *
     * @throws Exception _more_
     */
    public MetametaApiHandler(Repository repository, Element node,
                              Hashtable props)
            throws Exception {
        super(repository);
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processTypeList(Request request) throws Exception {
        CategoryBuffer cb = new CategoryBuffer();
        for (TypeHandler typeHandler :
                getRepository().getTypeHandlersForDisplay(false)) {
            if ( !typeHandler.getForUser()) {
                continue;
            }
            StringBuilder buff = new StringBuilder();
            String icon = typeHandler.getTypeProperty("icon", (String) null);
            if (icon != null) {
                buff.append(HtmlUtils.img(getRepository().getIconUrl(icon)));
                buff.append(HtmlUtils.space(1));
            }
            buff.append(
                HtmlUtils.href(
                    getRepository().getUrlBase() + "/metameta/type/"
                    + typeHandler.getType(), typeHandler.getLabel()));
            buff.append("<br>\n");
            cb.append(typeHandler.getCategory(), buff);
        }

        StringBuilder sb = new StringBuilder();

        getPageHandler().sectionOpen(request, sb, "Metameta Listing", false);
        sb.append(
            "Below are RAMADDA entry import files that allow you to view and modify the entry metadata.\n");
        sb.append(
            "Download the file or the URL and then do a File Import of the file. This will create a collection of entries based on the metadata of the selected entry type. You can then modify that metadata and regenerate an types.xml that defines a new entry type");
        sb.append("<hr>");

        getPageHandler().doTableLayout(request, sb, cb);

        getPageHandler().sectionClose(request, sb);

        return new Result("Metameta", sb);
    }


    /**
     *  This will be the entry point for querying on the types
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processTypeRequest(Request request) throws Exception {

        List<String> toks = StringUtil.split(request.getRequestPath(), "/",
                                             true, true);
        String        type        = toks.get(toks.size() - 1);
        TypeHandler   typeHandler = getRepository().getTypeHandler(type);
        StringBuilder xml = new StringBuilder(XmlUtil.openTag(TAG_ENTRIES));
        xml.append("\n");
        StringBuilder inner = new StringBuilder();
        inner.append(
            XmlUtil.tag(
                MetametaDictionaryTypeHandler.FIELD_PROPERTIES, "",
                XmlUtil.getCdata(
                    Utils.makeProperties(typeHandler.getProperties()))));


        inner.append(
            XmlUtil.tag(
                MetametaDictionaryTypeHandler.FIELD_SHORT_NAME, "",
                typeHandler.getType()));

        inner.append(
            XmlUtil.tag(
                MetametaDictionaryTypeHandler.FIELD_DICTIONARY_TYPE, "",
                "entry"));

        if (typeHandler.getParent() != null) {
            inner.append(
                XmlUtil.tag(
                    MetametaDictionaryTypeHandler.FIELD_SUPER_TYPE, "",
                    typeHandler.getParent().getType()));
        }
        if (typeHandler.isGroup()) {
            inner.append(
                XmlUtil.tag(
                    MetametaDictionaryTypeHandler.FIELD_ISGROUP, "", "true"));
        }
        inner.append(
            XmlUtil.tag(
                MetametaDictionaryTypeHandler.FIELD_HANDLER_CLASS, "",
                typeHandler.getClass().getName()));


        xml.append(
            XmlUtil.tag(
                TAG_ENTRY,
                XmlUtil.attrs(
                    ATTR_NAME, "Data Dictionary: " + typeHandler.getLabel(),
                    ATTR_TYPE, MetametaDictionaryTypeHandler.TYPE, ATTR_ID,
                    "dictionary"), inner.toString()));

        List<Column> columns = typeHandler.getColumns();
        if (columns != null) {
            int index = 0;
            for (Column column : columns) {
                index++;
                inner = new StringBuilder();
                StringBuilder             attrs = new StringBuilder();

                LinkedHashMap<String, String> enums = column.getEnumTable();
                if ((enums != null) && (enums.size() > 0)) {
                    inner.append(XmlUtil
                        .tag(MetametaFieldTypeHandler
                            .FIELD_ENUMERATION_VALUES, "",
                                XmlUtil.getCdata(Utils
                                    .makeProperties(enums))));
                }

                inner.append(
                    XmlUtil.tag(
                        MetametaFieldTypeHandler.FIELD_FIELD_INDEX, "",
                        "" + index));
                inner.append(
                    XmlUtil.tag(
                        MetametaFieldTypeHandler.FIELD_FIELD_ID, "",
                        column.getName()));
                inner.append(
                    XmlUtil.tag(
                        MetametaFieldTypeHandler.FIELD_DATATYPE, "",
                        column.getType()));

                StringBuilder props = new StringBuilder();
                String        dflt  = column.getDflt();
                if (Utils.stringDefined(dflt)) {
                    props.append(Column.ATTR_DEFAULT);
                    props.append("=");
                    props.append(dflt);
                    props.append("\n");
                }


                props.append(Utils.makeProperties(column.getProperties()));

                inner.append(
                    XmlUtil.tag(
                        MetametaFieldTypeHandler.FIELD_PROPERTIES, "",
                        XmlUtil.getCdata(props.toString())));

                inner.append(
                    XmlUtil.tag(
                        MetametaFieldTypeHandler.FIELD_DATABASE_COLUMN_SIZE,
                        "", "" + column.getSize()));


                attrs.append(XmlUtil.attrs(ATTR_NAME, column.getLabel(),
                                           ATTR_TYPE,
                                           MetametaFieldTypeHandler.TYPE,
                                           ATTR_PARENT, "dictionary"));

                xml.append(XmlUtil.tag(TAG_ENTRY, attrs.toString(),
                                       inner.toString()));

            }
        }

        xml.append(XmlUtil.closeTag(TAG_ENTRIES));

        return new Result("Type", xml, "text/xml");

    }

}
