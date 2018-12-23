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

package org.ramadda.repository.database;


import ucar.unidata.util.StringUtil;

import java.sql.PreparedStatement;

import java.util.ArrayList;
import java.util.List;


/**
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
public class TableInfo {

    /** _more_ */
    private String name;

    /** _more_ */
    private List<IndexInfo> indices;

    /** _more_ */
    private List<ColumnInfo> columns;

    /** _more_ */
    public PreparedStatement statement;

    /** _more_ */
    public int batchCnt = 0;

    /**
     * _more_
     */
    public TableInfo() {}

    /**
     * _more_
     *
     * @param name _more_
     * @param indices _more_
     * @param columns _more_
     */
    public TableInfo(String name, List<IndexInfo> indices,
                     List<ColumnInfo> columns) {
        this.name    = name;
        this.indices = indices;
        this.columns = columns;
    }

    /**
     * _more_
     *
     * @param infos _more_
     *
     * @return _more_
     */
    public static List<String> getTableNames(List<TableInfo> infos) {
        List<String> names = new ArrayList<String>();
        for (TableInfo info : infos) {
            names.add(info.getName());
        }

        return names;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String[] getColumnNames() {
        String[] names = new String[columns.size()];
        int      cnt   = 0;
        for (ColumnInfo columnInfo : columns) {
            names[cnt++] = columnInfo.getName();
        }

        return names;
    }

    /**
     *  Set the Name property.
     *
     *  @param value The new value for Name
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     *  Get the Name property.
     *
     *  @return The Name
     */
    public String getName() {
        return this.name;
    }


    /**
     *  Set the Columns property.
     *
     *  @param value The new value for Columns
     */
    public void setColumns(List<ColumnInfo> value) {
        this.columns = value;
    }

    /**
     *  Get the Columns property.
     *
     *  @return The Columns
     */
    public List<ColumnInfo> getColumns() {
        return this.columns;
    }


    /**
     *  Set the Indices property.
     *
     *  @param value The new value for Indices
     */
    public void setIndices(List<IndexInfo> value) {
        this.indices = value;
    }

    /**
     *  Get the Indices property.
     *
     *  @return The Indices
     */
    public List<IndexInfo> getIndices() {
        return this.indices;
    }


}
