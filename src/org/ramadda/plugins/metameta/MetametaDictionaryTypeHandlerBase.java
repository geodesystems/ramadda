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
public abstract class MetametaDictionaryTypeHandlerBase extends MetametaGroupTypeHandler {

    /** _more_ */
    public static final String TYPE = "type_metameta_dictionary";

    /** _more_ */
    private static int INDEX_BASE = 0;

    /** _more_ */
    public static final int INDEX_FIELD_INDEX = INDEX_BASE + 0;

    /** _more_ */
    public static final String FIELD_FIELD_INDEX = "field_index";

    /** _more_ */
    public static final int INDEX_DICTIONARY_TYPE = INDEX_BASE + 1;

    /** _more_ */
    public static final String FIELD_DICTIONARY_TYPE = "dictionary_type";

    /** _more_ */
    public static final int INDEX_SHORT_NAME = INDEX_BASE + 2;

    /** _more_ */
    public static final String FIELD_SHORT_NAME = "short_name";

    /** _more_ */
    public static final int INDEX_SUPER_TYPE = INDEX_BASE + 3;

    /** _more_ */
    public static final String FIELD_SUPER_TYPE = "super_type";

    /** _more_ */
    public static final int INDEX_ISGROUP = INDEX_BASE + 4;

    /** _more_ */
    public static final String FIELD_ISGROUP = "isgroup";

    /** _more_ */
    public static final int INDEX_HANDLER_CLASS = INDEX_BASE + 5;

    /** _more_ */
    public static final String FIELD_HANDLER_CLASS = "handler_class";

    /** _more_ */
    public static final int INDEX_PROPERTIES = INDEX_BASE + 6;

    /** _more_ */
    public static final String FIELD_PROPERTIES = "properties";

    /** _more_ */
    public static final int INDEX_WIKI_TEXT = INDEX_BASE + 7;


    /** _more_ */
    public static final int INDEX_BASETYPE = INDEX_BASE + 8;
    
    /** _more_ */
    public static final String FIELD_WIKI_TEXT = "wiki_text";




    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public MetametaDictionaryTypeHandlerBase(Repository repository,
                                             Element entryNode)
            throws Exception {
        super(repository, entryNode);
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
