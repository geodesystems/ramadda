/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.type;

import org.ramadda.repository.*;
import org.ramadda.repository.type.*;

import org.ramadda.repository.util.SelectInfo;
import org.ramadda.util.HtmlUtils;

import org.ramadda.util.Utils;
import org.ramadda.util.WikiUtil;

import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.util.ArrayList;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

public abstract class OrderedGroupTypeHandler extends ExtensibleGroupTypeHandler {
    public static final String ARG_MOVE_UP = "move.up";
    public static final String ARG_MOVE_DOWN = "move.down";

    public OrderedGroupTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }

    public abstract String getChildType();

    public String getListTitle() {
        return "Entries";
    }

    @Override
    public List<Entry> postProcessEntries(Request request,
                                          List<Entry> entries)
            throws Exception {
        List<Entry> sorted =
            getEntryManager().getEntryUtil().sortEntriesOnField(entries,
                false, getChildType(), getSortIndex());

        return sorted;
    }

    public int getSortIndex() {
        return 0;
    }

    public void addListForm(Request request, Entry parent,
                            List<Entry> entries, Appendable sb)
            throws Exception {

        sb.append(request.form(getRepository().URL_ENTRY_ACCESS));
        sb.append(HtmlUtils.hidden(ARG_ENTRYID, parent.getId()));
        if (entries.size() > 0) {
            sb.append(msgHeader(getListTitle()));
        }
        sb.append(HtmlUtils.formTable());
        int cnt = 0;
        for (Entry entry : entries) {
            sb.append("<tr valign=top><td>");
            if (cnt > 0) {
                sb.append(HtmlUtils.submitImage(getIconUrl("/icons/updart.gif"),
                        ARG_MOVE_UP + "." + entry.getId(), msg("Move up"),
                        ""));
            }
            sb.append("</td><td>");
            if (cnt < entries.size() - 1) {
                sb.append(HtmlUtils.submitImage(getIconUrl("/icons/downdart.gif"),
                        ARG_MOVE_DOWN + "." + entry.getId(),
                        msg("Move down"), ""));
            }
            sb.append("</td><td>");
            cnt++;

            String url = null;
            if (getAccessManager().canDoEdit(request, entry)) {
                url = request.entryUrl(getRepository().URL_ENTRY_FORM, entry);
            }
            EntryLink link = getEntryManager().getAjaxLink(request, entry,
                                 getEntryDisplayName(entry), url);
            addListLink(request, entry, link, sb);
            sb.append("</td></tr>");
        }

        sb.append(HtmlUtils.formTableClose());
        sb.append(HtmlUtils.p());
        List<String> buttons = new ArrayList<String>();
        addEntryButtons(request, parent, buttons);
        sb.append(HtmlUtils.buttons(buttons));
        sb.append(HtmlUtils.formClose());
    }

    public void addListLink(Request request, Entry entry, EntryLink link,
                            Appendable sb)
            throws Exception {
        sb.append(link.getLink());
        sb.append(link.getFolderBlock());
    }

    public void addEntryButtons(Request request, Entry entry,
                                List<String> buttons) {}

    public List<Entry> getChildrenEntries(Request request, Entry entry)
            throws Exception {
        return getEntryUtil().getEntriesWithType(
            getEntryManager().getChildrenAll(request, entry, null),
            getChildType());

    }

    @Override
    public Result processEntryAccess(Request request, Entry entry)
            throws Exception {

        if ( !getEntryManager().canAddTo(request, entry)) {
            return null;
        }

        List<Entry> children = getChildrenEntries(request, entry);
        boolean     didMove  = false;
        for (int i = 0; i < children.size(); i++) {
            Entry child = children.get(i);
            if ( !child.getTypeHandler().isType(getChildType())) {
                continue;
            }
            if (request.exists(ARG_MOVE_UP + "." + child.getId() + ".x")) {
                didMove = true;
                if (i > 0) {
                    children.remove(child);
                    children.add(i - 1, child);
                }

                break;
            }
            if (request.exists(ARG_MOVE_DOWN + "." + child.getId() + ".x")) {
                didMove = true;
                if (i < children.size() - 1) {
                    children.remove(child);
                    children.add(i + 1, child);
                }

                break;
            }
        }

        if (didMove) {
            int index = 0;
            for (int i = 0; i < children.size(); i++) {
                Entry child = children.get(i);
                index++;
                child.getTypeHandler().setEntryValue(child, 0,
                        Integer.valueOf(index));
            }
            getEntryManager().updateEntries(request, children);
        }

        String url = request.entryUrl(getRepository().URL_ENTRY_SHOW, entry);

        return new Result(url);
    }

}
