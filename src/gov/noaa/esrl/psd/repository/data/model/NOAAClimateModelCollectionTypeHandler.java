/**
* Copyright (c) 2008-2015 Geode Systems LLC
* This Software is licensed under the Geode Systems RAMADDA License available in the source distribution in the file 
* ramadda_license.txt. The above copyright notice shall be included in all copies or substantial portions of the Software.
*/

/**
 * Copyright (c) 2008-2015 Geode Systems LLC
 * This Software is licensed under the Geode Systems RAMADDA License available in the source distribution in the file
 * ramadda_license.txt. The above copyright notice shall be included in all copies or substantial portions of the Software.
 */

package gov.noaa.esrl.psd.repository.data.model;


import org.ramadda.geodata.model.ClimateCollectionTypeHandler;
import org.ramadda.repository.Repository;

import org.w3c.dom.Element;


/**
 * Class description
 *
 *
 */
public class NOAAClimateModelCollectionTypeHandler extends ClimateCollectionTypeHandler {

    /**
     * Construct a new NOAAClimateModelCollectionTypeHandler
     *
     * @param repository  the Repository
     * @param entryNode   the XML declaration
     *
     * @throws Exception  problems creating type
     */
    public NOAAClimateModelCollectionTypeHandler(Repository repository,
            Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }

}
