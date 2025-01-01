/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
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
     * @param repository _more_
     * @param tableName _more_
     * @param tableNode _more_
     * @param desc _more_
     *
     * @throws Exception _more_
     */
    public EquipmentDbTypeHandler(Repository repository, String tableName,
                                  Element tableNode, String desc)
            throws Exception {
        super(repository, tableName, tableNode, desc);
    }

}
