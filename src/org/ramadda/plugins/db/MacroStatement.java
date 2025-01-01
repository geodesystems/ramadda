/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.db;


import org.ramadda.repository.type.*;

import java.util.List;



/**
 * Class description
 *
 *
 * @version        $version$, Tue, Nov 2, '21
 * @author         Enter your name here...
 */
public class MacroStatement {

    /** _more_ */
    public static final String TYPE_TEMPLATE = "template";

    /** _more_ */
    public static final String TYPE_NOTIN = "notin";

    /** _more_ */
    String type;

    /** _more_ */
    String name;

    /** _more_ */
    String desc;

    /** _more_ */
    String column;

    /** _more_ */
    List<Column> columns;

    /** _more_ */
    String statement;

    /**
     * _more_
     *
     * @param name _more_
     * @param type _more_
     * @param desc _more_
     * @param column _more_
     * @param columns _more_
     * @param statement _more_
     */
    MacroStatement(String name, String type, String desc, String column,
                   List<Column> columns, String statement) {
        this.type      = type;
        this.name      = name;
        this.desc      = desc;
        this.column    = column;
        this.columns   = columns;
        this.statement = statement;
    }
}
