/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.db;

import org.ramadda.repository.type.*;
import java.util.List;

public class MacroStatement {
    public static final String TYPE_TEMPLATE = "template";
    public static final String TYPE_NOTIN = "notin";

    String type;
    String name;
    String desc;
    String column;
    List<Column> columns;
    String statement;

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
