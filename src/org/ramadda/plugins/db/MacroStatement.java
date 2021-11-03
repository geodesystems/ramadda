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

package org.ramadda.plugins.db;

import java.util.List;
import org.ramadda.repository.type.*;



/**
 * Class description
 *
 *
 * @version        $version$, Tue, Nov 2, '21
 * @author         Enter your name here...    
 */
public class MacroStatement {

    /** _more_          */
    public static final String TYPE_TEMPLATE = "template";

    /** _more_          */
    public static final String TYPE_NOTIN = "notin";

    /** _more_          */
    String type;

    /** _more_          */
    String name;

    /** _more_          */
    String desc;

    /** _more_          */
    String column;

    /** _more_          */
    List<Column> columns;

    /** _more_          */
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



