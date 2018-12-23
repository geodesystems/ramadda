/*
* Copyright (c) 2008-2019 Geode Systems LLC
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
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;
import org.ramadda.util.WikiUtil;

import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;

import java.io.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;


/**
 */
public abstract class MetametaGroupTypeHandler extends OrderedGroupTypeHandler {


    /** _more_ */
    public static final String ARG_METAMETA_GENERATE_DB =
        "metameta.generate.db";

    /** _more_ */
    public static final String ARG_METAMETA_GENERATE_ENTRY =
        "metameta.generate.entry";



    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception on badness
     */
    public MetametaGroupTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }


    /**
     * _more_
     *
     * @param entry _more_
     * @param index _more_
     *
     * @return _more_
     *
     * @throws Exception on badness
     */
    public Hashtable getProperties(Entry entry, int index) throws Exception {
        String s = (String) getEntryValue(entry, index);
        if (s == null) {
            s = "";
        }
        Properties props = new Properties();
        props.load(new ByteArrayInputStream(s.getBytes()));
        Hashtable table = new Hashtable();
        table.putAll(props);

        return table;
    }



    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     */
    @Override
    public boolean canBeCreatedBy(Request request) {
        return request.getUser().getAdmin();
    }



    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public String getListTitle() {
        return "Field Definitions";
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param link _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void addListLink(Request request, Entry entry, EntryLink link,
                            Appendable sb)
            throws Exception {
        if ( !entry.getTypeHandler().isType(MetametaFieldTypeHandler.TYPE)) {
            sb.append(link.getLink());
            sb.append(link.getFolderBlock());

            return;
        }
        MetametaFieldTypeHandler field =
            (MetametaFieldTypeHandler) entry.getTypeHandler();
        String fieldId = (String) field.getEntryValue(entry,
                             field.INDEX_FIELD_ID);

        String datatype = (String) field.getEntryValue(entry,
                              field.INDEX_DATATYPE);

        sb.append(
            "<table cellpadding=0 cellspacing=0 border=0 style=\"min-width:600px;\" width=100%><tr valign=top>");
        sb.append("<td width=33%>");
        sb.append(link.getLink());
        sb.append("</td>");

        sb.append("<td width=33%>");
        sb.append(fieldId);
        sb.append("</td>");

        sb.append("<td width=33%>");
        sb.append(datatype);
        sb.append("</td>");

        sb.append("</tr></table>");
        sb.append(link.getFolderBlock());
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param buttons _more_
     */
    public void addEntryButtons(Request request, Entry entry,
                                List<String> buttons) {
        super.addEntryButtons(request, entry, buttons);
        buttons.add(HtmlUtils.submit("Generate entries types.xml",
                                     ARG_METAMETA_GENERATE_ENTRY));
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception on badness
     */
    @Override
    public Result processEntryAccess(Request request, Entry entry)
            throws Exception {


        if ( !getEntryManager().canAddTo(request, entry)) {
            return null;
        }

        if (request.exists(ARG_METAMETA_GENERATE_DB)) {
            StringBuffer xml = new StringBuffer();
            generateDbXml(request, xml, entry,
                          getChildrenEntries(request, entry));
            String filename =
                IOUtil.stripExtension(IOUtil.getFileTail(entry.getName()))
                + "_db.xml";
            request.setReturnFilename(filename);

            return new Result("Query Results", xml, "text/xml");
        }


        if (request.exists(ARG_METAMETA_GENERATE_ENTRY)) {
            StringBuffer xml = new StringBuffer();
            generateEntryXml(request, xml, entry,
                             getChildrenEntries(request, entry));
            String filename =
                IOUtil.stripExtension(IOUtil.getFileTail(entry.getName()))
                + "_types.xml";
            request.setReturnFilename(filename);

            return new Result("Query Results", xml, "text/xml");
        }

        return super.processEntryAccess(request, entry);
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
    public abstract void generateDbXml(Request request, StringBuffer xml,
                                       Entry parent, List<Entry> children)
     throws Exception;



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
    public abstract void generateEntryXml(Request request, StringBuffer xml,
                                          Entry parent, List<Entry> children)
     throws Exception;





}
