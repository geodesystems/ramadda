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
