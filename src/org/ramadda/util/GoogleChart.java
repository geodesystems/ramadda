/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;


/**
 */
public class GoogleChart {


    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Oct 31, '13
     * @author         Enter your name here...
     */
    public static class DataTable {

        /**
         * _more_
         *
         * @param sb _more_
         */
        public static void init(StringBuffer sb) {
            sb.append("var data = new google.visualization.DataTable();\n");
        }

        /**
         * _more_
         *
         * @param sb _more_
         * @param type _more_
         * @param name _more_
         */
        public static void addColumn(StringBuffer sb, String type,
                                     String name) {
            //data.addColumn('string', 'Name');
            sb.append(HtmlUtils.call("data.addColumn",
                                     HtmlUtils.squote(type),
                                     HtmlUtils.squote(name)));
        }


    }



}
