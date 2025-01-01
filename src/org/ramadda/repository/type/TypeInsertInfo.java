/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
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
