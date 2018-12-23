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

package org.ramadda.plugins.wiki;


import org.ramadda.sql.SqlUtil;


/**
 */
public class Tables {

    /**
     * Class WIKIPAGEHISTORY _more_
     *
     *
     */
    public static class WIKIPAGEHISTORY {

        /** _more_ */
        public static final String NAME = "wikipagehistory";

        /** _more_ */
        public static final String COL_ENTRY_ID = NAME + ".entry_id";

        /** _more_ */
        public static final String COL_USER_ID = NAME + ".user_id";

        /** _more_ */
        public static final String COL_DATE = NAME + ".date";

        /** _more_ */
        public static final String COL_DESCRIPTION = NAME + ".description";

        /** _more_ */
        public static final String COL_WIKITEXT = NAME + ".wikitext";

        /** _more_ */
        public static final String[] ARRAY = new String[] { COL_ENTRY_ID,
                COL_USER_ID, COL_DATE, COL_DESCRIPTION, COL_WIKITEXT };


        /** _more_ */
        public static final String COLUMNS = SqlUtil.comma(ARRAY);

        /** _more_ */
        public static final String NODOT_COLUMNS = SqlUtil.commaNoDot(ARRAY);

        /** _more_ */
        public static final String INSERT =
            SqlUtil.makeInsert(NAME, NODOT_COLUMNS,
                               SqlUtil.getQuestionMarks(ARRAY.length));

    }

    ;



}
