/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.wiki;


import org.ramadda.util.sql.SqlUtil;


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
