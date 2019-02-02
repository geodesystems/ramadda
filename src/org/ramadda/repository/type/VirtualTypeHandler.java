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

package org.ramadda.repository.type;


import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.OutputHandler;

import org.ramadda.util.FormInfo;
import org.ramadda.util.HtmlUtils;


import org.w3c.dom.*;

import ucar.unidata.util.StringUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;



/**
 * Class TypeHandler _more_
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
public class VirtualTypeHandler extends ExtensibleGroupTypeHandler {




    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public VirtualTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param column _more_
     * @param formBuffer _more_
     * @param entry _more_
     * @param values _more_
     * @param state _more_
     * @param formInfo _more_
     * @param sourceTypeHandler _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void addColumnToEntryForm(Request request, Column column,
                                     Appendable formBuffer, Entry entry,
                                     Object[] values, Hashtable state,
                                     FormInfo formInfo,
                                     TypeHandler sourceTypeHandler)
            throws Exception {

        if (column.getOffset() == 0) {
            String value = "";
            if (values != null) {
                value = column.toString(values, column.getOffset());
            }
            String urlArg     = column.getEditArg();
            String textAreaId = HtmlUtils.getUniqueId("input_");
            String widget = HtmlUtils.textArea(urlArg, value, 10, 60,
                                HtmlUtils.id(textAreaId));
            formInfo.addMaxSizeValidation(column.getLabel(), textAreaId,
                                          5000);
            String suffix =
                "entry ids - one per row<br>Or use the  <a target=_help href=\"http://ramadda.org/repository/userguide/wikitext.html#collection\">entry shortcut and search</a> services";
            String buttons = OutputHandler.getSelect(request, textAreaId,
                                 "Add entry id", true, "entryid", entry,
                                 false);

            formBuffer.append(
                HtmlUtils.formEntryTop(
                    msgLabel(column.getLabel()),
                    buttons + "<table cellspacing=0 cellpadding=0 border=0>"
                    + HtmlUtils.row(HtmlUtils.cols(widget, suffix))
                    + "</table>"));
            formBuffer.append("\n");
        } else {
            super.addColumnToEntryForm(request, column, formBuffer, entry,
                                       values, state, formInfo,
                                       sourceTypeHandler);
        }

    }


    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public boolean isSynthType() {
        return true;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param mainEntry _more_
     * @param parentEntry _more_
     * @param synthId _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public List<String> getSynthIds(Request request, Entry mainEntry,
                                    Entry parentEntry, String synthId)
            throws Exception {

        List<String> ids = getEntryManager().getChildIdsFromDatabase(request,
                               mainEntry, null);
        String idString = (String) mainEntry.getValue(0, "").replace(",",
                              "_COMMA_");
        List<String> lines = new ArrayList<String>();


        for (String line : StringUtil.split(idString, "\n", true, true)) {
            if (line.startsWith("#")) {
                continue;
            }
            lines.add(line);
        }
        idString = StringUtil.join(",", lines);

        List<Entry> entries = getWikiManager().getEntries(request, null,
                                  mainEntry, mainEntry, idString, null,
                                  false, "");


        String by = request.getString(ARG_ORDERBY, (String) null);

        if (by == null) {
            Metadata sortMetadata =
                getMetadataManager().getSortOrderMetadata(request, mainEntry);
            if (sortMetadata != null) {
                by = sortMetadata.getAttr1();
            }
        }

        if (by == null) {
            by = SORTBY_FROMDATE;
        }

        boolean descending = !request.get(ARG_ASCENDING, false);
        if (by.equals(SORTBY_NAME)) {
            entries =
                getEntryManager().getEntryUtil().sortEntriesOnName(entries,
                    descending);
            //        } else if (by.equals(SORTBY_SIZE)) {
        } else {
            entries =
                getEntryManager().getEntryUtil().sortEntriesOnDate(entries,
                    descending);
        }

        for (Entry entry : entries) {
            ids.add(entry.getId());
        }


        mainEntry.setChildIds(ids);

        return ids;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param parentEntry _more_
     * @param id _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public Entry makeSynthEntry(Request request, Entry parentEntry, String id)
            throws Exception {
        return getEntryManager().getEntry(request, id);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param parentEntry _more_
     * @param entryNames _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public Entry makeSynthEntry(Request request, Entry parentEntry,
                                List<String> entryNames)
            throws Exception {
        if (entryNames.size() == 0) {
            return null;
        }
        String topEntryName = entryNames.get(0);
        List<Entry> children = getEntryManager().getChildren(request,
                                   parentEntry);
        if (topEntryName.matches("\\d+")) {
            int index = new Integer(topEntryName).intValue();
            index--;
            if ((index >= 0) && (index < children.size())) {
                return children.get(index);
            }

            return null;
        }

        for (Entry child : children) {
            if (child.getName().equals(topEntryName)) {
                entryNames.remove(0);

                return getEntryManager().findEntryFromPath(request, child,
                        StringUtil.join(Entry.PATHDELIMITER, entryNames));
            }
        }


        for (Entry child : children) {
            if (topEntryName.matches(child.getName())) {
                entryNames.remove(0);

                return getEntryManager().findEntryFromPath(request, child,
                        StringUtil.join(Entry.PATHDELIMITER, entryNames));
            }
        }


        return null;
    }


    /**
     * _more_
     *
     * @param id _more_
     *
     * @return _more_
     */
    @Override
    public Entry createEntry(String id) {
        //Make the top level entry act like a group
        return new Entry(id, this, true);
    }



}
