/*
* Copyright (c) 2008-2021 Geode Systems LLC
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

package org.ramadda.data.docs;



import org.ramadda.repository.*;

import org.ramadda.util.HtmlUtils;

import ucar.unidata.util.StringUtil;

import java.util.ArrayList;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;


/**
 */
public class TabularVisitInfo {

    /** _more_ */
    private int skipRows = 0;

    /** _more_ */
    private int maxRows = -1;

    /** _more_ */
    private List<TabularSearchField> searchFields;

    /** _more_ */
    private String searchText;

    /** _more_ */
    private String searchTextWithPattern;


    /** _more_ */
    private HashSet<Integer> sheetsToShow;

    /** _more_ */
    private List props = new ArrayList();




    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     */
    public TabularVisitInfo(Request request, Entry entry) {
        if (TabularTypeHandler.isTabular(entry)) {
            searchFields = new ArrayList<TabularSearchField>();
            for (String line :
                    StringUtil.split(
                        entry.getValue(
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
        }
    }


    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     */
    public void addTableProperty(String name, String value) {
        props.add(name);
        props.add(value);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List getTableProperties() {
        return props;
    }

    /**
     * _more_
     *
     * @param cols _more_
     *
     * @return _more_
     */
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


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     * @param skipRows _more_
     * @param maxRows _more_
     * @param sheetsToShow _more_
     * @param searchFields _more_
     */
    public TabularVisitInfo(Request request, Entry entry, int skipRows,
                            int maxRows, HashSet<Integer> sheetsToShow) {
        this(request, entry);
        this.skipRows     = skipRows;
        this.maxRows      = maxRows;
        this.sheetsToShow = sheetsToShow;
    }


    /**
     * _more_
     *
     * @param sheetIdx _more_
     *
     * @return _more_
     */
    public boolean okToShowSheet(int sheetIdx) {
        if ((sheetsToShow != null) && !sheetsToShow.contains(sheetIdx)) {
            return false;
        }

        return true;
    }


    /**
     * Set the SkipRows property.
     *
     * @param value The new value for SkipRows
     */
    public void setSkipRows(int value) {
        skipRows = value;
    }

    /**
     * Get the SkipRows property.
     *
     * @return The SkipRows
     */
    public int getSkipRows() {
        return skipRows;
    }

    /**
     * Set the MaxRows property.
     *
     * @param value The new value for MaxRows
     */
    public void setMaxRows(int value) {
        maxRows = value;
    }

    /**
     * Get the MaxRows property.
     *
     * @return The MaxRows
     */
    public int getMaxRows() {
        return maxRows;
    }

    /**
     * Set the SearchFields property.
     *
     * @param value The new value for SearchFields
     */
    public void setSearchFields(List<TabularSearchField> value) {
        searchFields = value;
    }

    /**
     * Get the SearchFields property.
     *
     * @return The SearchFields
     */
    public List<TabularSearchField> getSearchFields() {
        return searchFields;
    }

    /**
     *  Set the SheetsToShow property.
     *
     *  @param value The new value for SheetsToShow
     */
    public void setSheetsToShow(HashSet<Integer> value) {
        sheetsToShow = value;
    }

    /**
     *  Get the SheetsToShow property.
     *
     *  @return The SheetsToShow
     */
    public HashSet<Integer> getSheetsToShow() {
        return sheetsToShow;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getSearchText() {
        return searchText;
    }




}
