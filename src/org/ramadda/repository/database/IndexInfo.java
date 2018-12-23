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


/**
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
public class IndexInfo {

    /** _more_ */
    private String name;

    /** _more_ */
    private String columnName;


    /**
     * _more_
     */
    public IndexInfo() {}

    /**
     * _more_
     *
     * @param name _more_
     * @param columnName _more_
     */
    public IndexInfo(String name, String columnName) {
        this.name       = name;
        this.columnName = columnName;

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
     * Set the ColumnName property.
     *
     * @param value The new value for ColumnName
     */
    public void setColumnName(String value) {
        this.columnName = value;
    }

    /**
     * Get the ColumnName property.
     *
     * @return The ColumnName
     */
    public String getColumnName() {
        return this.columnName;
    }


}
