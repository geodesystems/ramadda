/**
Copyright (c) 2008-2026 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.data.docs;

import org.ramadda.repository.*;

import org.ramadda.util.HtmlUtils;

import ucar.unidata.util.StringUtil;

import java.util.ArrayList;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

@SuppressWarnings("unchecked")
public class TabularVisitInfo {
    private int skipRows = 0;
    private int maxRows = -1;
    private List<TabularSearchField> searchFields;
    private String searchText;
    private String searchTextWithPattern;
    private HashSet<Integer> sheetsToShow;
    private List props = new ArrayList();

    public TabularVisitInfo(Request request, Entry entry) {
        if (TabularTypeHandler.isTabular(entry)) {
	    /*
            searchFields = new ArrayList<TabularSearchField>();
            for (String line :
                    StringUtil.split(
                        entry.getStringValue(
                            TabularTypeHandler.IDX_SEARCHINFO, ""), "\n",
                                true, true)) {

                String       label = null;
                List<String> toks  = StringUtil.splitUpTo(line, " ", 2);
                if (toks.size() > 1) {
                    line = toks.get(0);
                    Hashtable props =
                        HtmlUtils.parseHtmlProperties(toks.get(1));
                    label = (String) props.get("label");
                }

                TabularSearchField sf = new TabularSearchField(line);
                if (label != null) {
                    sf.setLabel(label);
                }
                sf.setValue(request.getString(sf.getUrlArg(), (String) null));
                searchFields.add(sf);
            }
            if (searchFields.size() == 0) {
                searchFields = null;
            }
	    */
        }
    }

    public void addTableProperty(String name, String value) {
        props.add(name);
        props.add(value);
    }

    public List getTableProperties() {
        return props;
    }

    public boolean rowOk(List cols) {
        if (searchText != null) {
            for (Object o : cols) {
                if (o == null) {
                    continue;
                }
                if (o.toString().matches(searchTextWithPattern)) {
                    return true;
                }
            }

            return false;
        }

        return true;
    }

    public TabularVisitInfo(Request request, Entry entry, int skipRows,
                            int maxRows, HashSet<Integer> sheetsToShow) {
        this(request, entry);
        this.skipRows     = skipRows;
        this.maxRows      = maxRows;
        this.sheetsToShow = sheetsToShow;
    }

    public boolean okToShowSheet(int sheetIdx) {
        if ((sheetsToShow != null) && !sheetsToShow.contains(sheetIdx)) {
            return false;
        }

        return true;
    }

    public void setSkipRows(int value) {
        skipRows = value;
    }

    public int getSkipRows() {
        return skipRows;
    }

    public void setMaxRows(int value) {
        maxRows = value;
    }

    public int getMaxRows() {
        return maxRows;
    }

    public void setSearchFields(List<TabularSearchField> value) {
        searchFields = value;
    }

    public List<TabularSearchField> getSearchFields() {
        return searchFields;
    }


    public void setSheetsToShow(HashSet<Integer> value) {
        sheetsToShow = value;
    }

    public HashSet<Integer> getSheetsToShow() {
        return sheetsToShow;
    }

    public String getSearchText() {
        return searchText;
    }

}
