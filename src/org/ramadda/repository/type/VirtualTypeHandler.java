/**
Copyright (c) 2008-2025 Geode Systems LLC
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

    private boolean debug  =false;
    /** 5 minute cache */
    private TTLCache<String, List<String>> cachedIds = new TTLCache<String,
                                                           List<String>>(5
                                                               * 60 * 1000);

    public VirtualTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }

    public void clearCache() {
        super.clearCache();
        cachedIds.clearCache();
    }

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
                value = entry!=null?entry.getStringValue(request,column,""):"";
            }
            String urlArg     = column.getEditArg();
            String textAreaId = HU.getUniqueId("input_");
            String widget = HU.textArea(urlArg, value, 10, 120,
                                HU.id(textAreaId));
            formInfo.addMaxSizeValidation(column.getLabel(), textAreaId,
                                          10000);
            String suffix =
                "entry ids - one per row<br>#use &quot;#&quot; to comment a line<br><a target=_help href=\"/repository/userguide/virtualgroup.html\">View help</a>";
            OutputHandler.EntrySelect buttons = OutputHandler.getSelect(request, textAreaId,
									"Add entry id", true, "entryid", entry,
									false,false);
	    HU.formEntry(formBuffer,
			 HU.b(msgLabel(column.getLabel()))+" " +
			 HU.span(buttons.toString(),HU.cssClass("ramadda-clickable")) +
			 "<br><table cellspacing=0 cellpadding=0 border=0>"
			 + HU.row(HU.cols(widget, suffix),"valign=top")
			 + "</table>");
        } else {
            super.addColumnToEntryForm(request, column, formBuffer, parentEntry, entry,
                                       values, state, formInfo,
                                       sourceTypeHandler);
        }

    }

    @Override
    public boolean isSynthType() {
        return true;
    }

    private String debugLine(String s) {
	String debugLine = s.replace("\\"," ").replaceAll("(\n|\r)"," ");
	return  Utils.clip(debugLine,50,"...");
    }

    @Override
    public List<String> getSynthIds(Request request, SelectInfo select, Entry mainEntry,
                                    Entry parentEntry, String synthId)
            throws Exception {
	boolean debug =false;
        List<String> ids = getEntryManager().getChildIdsFromDatabase(request,   mainEntry, null);
        String idString = (String) mainEntry.getStringValue(request,0, "").replace(",", "_COMMA_");
	if(debug)
	    getLogManager().logSpecial("virtual.getSynthIds:" +Utils.clip(idString.replace("\n"," "),100,""));

        String  orderBy         = request.getString(ARG_ORDERBY, (String) null);
        boolean descending = !request.get(ARG_ASCENDING, false);
	if (orderBy == null) {
	    Metadata sortMetadata =
		getMetadataManager().getSortOrderMetadata(request, mainEntry,true);
	    if (sortMetadata != null) {
		orderBy = sortMetadata.getAttr1();
		descending=!sortMetadata.getAttr2().equals("true");
	    }
	}
	if (orderBy == null) {
	    orderBy = ORDERBY_FROMDATE;
	}

	String cacheKey = idString +"_" + orderBy +"_" + descending;
        List<String> fromCache = cachedIds.get(cacheKey);
        if (fromCache != null && fromCache.size()==0) {
	    if(debug)
		getLogManager().logSpecial("virtual from cache is empty:" +mainEntry.getId() +" " + debugLine(idString));
	}

        if (fromCache == null) {
	    if(debug)
		getLogManager().logSpecial("virtual creating:" +mainEntry.getId() +" "+debugLine(idString));
            fromCache = new ArrayList<String>();
	    List<String> lines = new ArrayList<String>();
	    String unescaped = Utils.unescapeNL(idString);
	    for (String line : Utils.split(unescaped, "\n", true, true)) {
                if (line.startsWith("#")) {
                    continue;
                }
                lines.add(line);
            }
            idString = StringUtil.join(",", lines);
	    Hashtable props = Utils.makeHashtable("sort",ORDERBY_NONE);
            List<Entry> entries = getWikiManager().getEntries(request, null,
							      mainEntry, mainEntry, idString, props,
							      false, "");

	    if(debug)
		getLogManager().logSpecial("\tcreating entries:"  + mainEntry.getId() +" " + entries.size()+" ID:" +debugLine(idString));

	    entries = getEntryManager().getEntryUtil().sortEntries(entries, orderBy,descending);
            for (Entry entry : entries) {
                fromCache.add(entry.getId());
            }
            cachedIds.put(cacheKey, fromCache);
	}
        ids.addAll(fromCache);
        mainEntry.setChildIds(ids);
        return ids;
    }

    @Override
    public Entry makeSynthEntry(Request request, Entry parentEntry, String id)
            throws Exception {
        return getEntryManager().getEntry(request, id);
    }

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

    @Override
    public Entry createEntry(String id) {
        //Make the top level entry act like a group
        return new Entry(id, this, true);
    }

}
