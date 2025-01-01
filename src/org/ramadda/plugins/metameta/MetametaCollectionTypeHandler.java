/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.metameta;


import org.ramadda.repository.*;
import org.ramadda.repository.type.*;

import org.w3c.dom.*;

import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.util.ArrayList;
import java.util.List;


/**
 * This represents some collection of metameta field entries
 *
 * @author RAMADDA Development Team
 */
public class MetametaCollectionTypeHandler extends MetametaGroupTypeHandler {

    /**
     * ctor
     *
     * @param repository repo
     * @param entryNode from types.xml
     *
     * @throws Exception on badness
     */
    public MetametaCollectionTypeHandler(Repository repository,
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
        return MetametaDictionaryTypeHandler.TYPE;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param parent _more_
     *
     * @return _more_
     *
     * @throws Exception on badness
     */
    @Override
    public Result getHtmlDisplay(Request request, Entry parent, Entries children)
            throws Exception {
        if ( !getEntryManager().canAddTo(request, parent)) {
            return null;
        }

        StringBuffer sb = new StringBuffer();
        getPageHandler().entrySectionOpen(request, parent, sb, null);

        addListForm(request, parent, children.get(), sb);

        getPageHandler().entrySectionClose(request, parent, sb);

        return getEntryManager().addEntryHeader(request, parent,
                new Result("Metameta Collection", sb));
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param xml _more_
     * @param parent _more_
     * @param children _more_
     *
     * @throws Exception on badness
     */
    public void generateDbXml(Request request, StringBuffer xml,
                              Entry parent, List<Entry> children)
            throws Exception {
        xml.append(XmlUtil.openTag("tables", ""));
        for (Entry defEntry : children) {
            MetametaDictionaryTypeHandler defTypeHandler =
                (MetametaDictionaryTypeHandler) defEntry.getTypeHandler();
            List<Entry> fields = getEntryManager().getChildrenAll(request,
                                     defEntry, null);
            defTypeHandler.generateDbXml(request, xml, defEntry, fields);
        }
        xml.append(XmlUtil.closeTag("tables"));
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param xml _more_
     * @param parent _more_
     * @param children _more_
     *
     * @throws Exception on badness
     */
    public void generateEntryXml(Request request, StringBuffer xml,
                                 Entry parent, List<Entry> children)
            throws Exception {
        xml.append(XmlUtil.openTag(TAG_TYPES, ""));
        for (Entry defEntry : children) {
            MetametaDictionaryTypeHandler defTypeHandler =
                (MetametaDictionaryTypeHandler) defEntry.getTypeHandler();
            List<Entry> fields = getEntryManager().getChildrenAll(request,
                                     defEntry, null);
            defTypeHandler.generateEntryXml(request, xml, defEntry, fields);
        }
        xml.append(XmlUtil.closeTag(TAG_TYPES));
    }






}
