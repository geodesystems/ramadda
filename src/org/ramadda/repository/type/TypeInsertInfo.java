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

package org.ramadda.repository.type;



import org.ramadda.repository.*;
import org.ramadda.repository.database.*;

import java.sql.PreparedStatement;
import java.sql.Statement;



/**
 *
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
public class TypeInsertInfo {

    /** _more_ */
    private String sql;

    /** _more_ */
    private PreparedStatement statement;

    /** _more_ */
    private TypeHandler typeHandler;

    /**
     * _more_
     *
     * @param typeHandler _more_
     * @param sql _more_
     */
    public TypeInsertInfo(TypeHandler typeHandler, String sql) {
        this.sql         = sql;
        this.typeHandler = typeHandler;
    }


    /**
     * Set the Sql property.
     *
     * @param value The new value for Sql
     */
    public void setSql(String value) {
        sql = value;
    }

    /**
     * Get the Sql property.
     *
     * @return The Sql
     */
    public String getSql() {
        return sql;
    }

    /**
     * Set the Statement property.
     *
     * @param value The new value for Statement
     */
    public void setStatement(PreparedStatement value) {
        statement = value;
    }

    /**
     * Get the Statement property.
     *
     * @return The Statement
     */
    public PreparedStatement getStatement() {
        return statement;
    }

    /**
     * Set the TypeHandler property.
     *
     * @param value The new value for TypeHandler
     */
    public void setTypeHandler(TypeHandler value) {
        typeHandler = value;
    }

    /**
     * Get the TypeHandler property.
     *
     * @return The TypeHandler
     */
    public TypeHandler getTypeHandler() {
        return typeHandler;
    }




}
