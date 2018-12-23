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

package org.ramadda.geodata.fieldproject;


import org.ramadda.plugins.db.*;



import org.ramadda.repository.*;


import org.w3c.dom.*;



/**
 *
 */

public class EquipmentDbTypeHandler extends DbTypeHandler {

    /**
     * _more_
     *
     *
     * @param dbAdmin _more_
     * @param repository _more_
     * @param tableName _more_
     * @param tableNode _more_
     * @param desc _more_
     *
     * @throws Exception _more_
     */
    public EquipmentDbTypeHandler(DbAdminHandler dbAdmin,
                                  Repository repository, String tableName,
                                  Element tableNode, String desc)
            throws Exception {
        super(dbAdmin, repository, tableName, tableNode, desc);
    }

    /**
     * _more_
     *
     * @param view _more_
     *
     * @return _more_
     */
    public boolean showInHeader(String view) {
        if (view.equals(VIEW_CHART) || view.equals(VIEW_STICKYNOTES)
                || view.equals(VIEW_RSS)) {
            return false;
        }

        return super.showInHeader(view);
    }

}
