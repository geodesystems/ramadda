/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.type;


import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.util.SelectInfo;
import org.ramadda.repository.output.OutputHandler;
import org.ramadda.util.FormInfo;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.TTLCache;
import org.ramadda.util.Utils;


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


    /** 5 minute cache */
    private TTLCache<String, List<String>> cachedIds = new TTLCache<String,
                                                           List<String>>(5
                                                               * 60 * 1000);

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
     */
    public void clearCache() {
        super.clearCache();
        cachedIds.clearCache();
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
                                     Appendable formBuffer, Entry parentEntry,Entry entry,
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
            String textAreaId = HU.getUniqueId("input_");
            String widget = HU.textArea(urlArg, value, 10, 120,
                                HU.id(textAreaId));
            formInfo.addMaxSizeValidation(column.getLabel(), textAreaId,
                                          10000);
            String suffix =
                "entry ids - one per row<br>#use &quot;#&quot; to comment a line<br><a target=_help href=\"/repository/userguide/virtualgroup.html\">View help</a>";
            String buttons = OutputHandler.getSelect(request, textAreaId,
                                 "Add entry id", true, "entryid", entry,
						     false,false);

	    HU.formEntry(formBuffer,
			 HU.b(msgLabel(column.getLabel()))+" " +
			 HU.span(buttons,HU.cssClass("ramadda-clickable")) +
			 "<br><table cellspacing=0 cellpadding=0 border=0>"
			 + HU.row(HU.cols(widget, suffix),"valign=top")
			 + "</table>");
        } else {
            super.addColumnToEntryForm(request, column, formBuffer, parentEntry, entry,
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
    public List<String> getSynthIds(Request request, SelectInfo select, Entry mainEntry,
                                    Entry parentEntry, String synthId)
            throws Exception {
        List<String> ids = getEntryManager().getChildIdsFromDatabase(request,
								     mainEntry, null);

        String idString = (String) mainEntry.getStringValue(0, "").replace(",",
									   "_COMMA_");
        String  by         = request.getString(ARG_ORDERBY, (String) null);
        boolean descending = !request.get(ARG_ASCENDING, false);
	//TODO:This doesn't work
	//	if(by!=null)    idString += "by:" + by + " desc:" + descending;
	System.err.println("virtual:" + idString);
        List<String> fromCache = cachedIds.get(idString);

        if (fromCache != null && fromCache.size()==0) {
	    System.err.println("from cache is empty");
	}

        if (fromCache == null) {
            fromCache = new ArrayList<String>();
            //Don't cache for now
            cachedIds.put(idString, fromCache);
            List<String> lines = new ArrayList<String>();
	    String unescaped = Utils.unescapeNL(idString);
	    for (String line : Utils.split(unescaped, "\n", true, true)) {
                if (line.startsWith("#")) {
                    continue;
                }
                lines.add(line);
            }
            idString = StringUtil.join(",", lines);
            List<Entry> entries = getWikiManager().getEntries(request, null,
							      mainEntry, mainEntry, idString, null,
							      false, "");
	    System.err.println("creating entries ID String:" + idString +" entries:" + entries.size());

            if (by == null) {
                Metadata sortMetadata =
                    getMetadataManager().getSortOrderMetadata(request,
							      mainEntry,true);
                if (sortMetadata != null) {
                    by = sortMetadata.getAttr1();
		    descending=!sortMetadata.getAttr2().equals("true");
                }
            }

            if (by == null) {
                by = ORDERBY_FROMDATE;
            }


            if (by.equals(ORDERBY_NAME)) {
                entries = getEntryManager().getEntryUtil().sortEntriesOnName(
                    entries, descending);
            } else {
                entries = getEntryManager().getEntryUtil().sortEntriesOnDate(
                    entries, descending);
            }

            for (Entry entry : entries) {
                fromCache.add(entry.getId());
            }
        }
        ids.addAll(fromCache);
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
            int index = Integer.parseInt(topEntryName);
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
