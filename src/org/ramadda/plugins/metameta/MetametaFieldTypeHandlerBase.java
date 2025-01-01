/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.metameta;



import org.ramadda.repository.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;

import org.w3c.dom.*;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;


/**
 * Generated TypeHandler code. Do not edit
 *
 *
 * @author RAMADDA Development Team
 */
public abstract class MetametaFieldTypeHandlerBase extends ExtensibleGroupTypeHandler {

    /** _more_ */
    public static final String TYPE = "type_metameta_field";

    /** _more_ */
    private static int INDEX_BASE = 0;

    /** _more_ */
    public static final int INDEX_FIELD_INDEX = INDEX_BASE + 0;

    /** _more_ */
    public static final String FIELD_FIELD_INDEX = "field_index";

    /** _more_ */
    public static final int INDEX_FIELD_ID = INDEX_BASE + 1;

    /** _more_ */
    public static final String FIELD_FIELD_ID = "field_id";

    /** _more_ */
    public static final int INDEX_DATATYPE = INDEX_BASE + 2;

    /** _more_ */
    public static final String FIELD_DATATYPE = "datatype";

    /** _more_ */
    public static final int INDEX_ENUMERATION_VALUES = INDEX_BASE + 3;

    /** _more_ */
    public static final String FIELD_ENUMERATION_VALUES =
        "enumeration_values";

    /** _more_ */
    public static final int INDEX_PROPERTIES = INDEX_BASE + 4;

    /** _more_ */
    public static final String FIELD_PROPERTIES = "properties";

    /** _more_ */
    public static final int INDEX_DATABASE_COLUMN_SIZE = INDEX_BASE + 5;

    /** _more_ */
    public static final String FIELD_DATABASE_COLUMN_SIZE =
        "database_column_size";

    /** _more_ */
    public static final int INDEX_MISSING = INDEX_BASE + 6;

    /** _more_ */
    public static final String FIELD_MISSING = "missing";

    /** _more_ */
    public static final int INDEX_UNIT = INDEX_BASE + 7;

    /** _more_ */
    public static final String FIELD_UNIT = "unit";




    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public MetametaFieldTypeHandlerBase(Repository repository,
                                        Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }





    /**
     * If this entry type is a group then this method gets called to create the default HTML display
     *
     * @param request request
     * @param parent the parent entry
     *
     * @return result
     *
     * @throws Exception on badness
     */
    @Override
    public Result getHtmlDisplay(Request request, Entry parent,  Entries children)
            throws Exception {
        return super.getHtmlDisplay(request, parent, children);
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
    public Result getHtmlDisplay(Request request, Entry entry)
            throws Exception {
        return super.getHtmlDisplay(request, entry);
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
        return super.processEntryAccess(request, entry);
    }




}
