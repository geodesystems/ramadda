/*
* Copyright (c) 2008-2018 Geode Systems LLC
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
     * @param subGroups _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception on badness
     */
    @Override
    public Result getHtmlDisplay(Request request, Entry parent,
                                 List<Entry> subGroups, List<Entry> entries)
            throws Exception {
        if ( !getEntryManager().canAddTo(request, parent)) {
            return null;
        }

        StringBuffer sb = new StringBuffer();
        subGroups.addAll(entries);
        addListForm(request, parent, subGroups, sb);

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
