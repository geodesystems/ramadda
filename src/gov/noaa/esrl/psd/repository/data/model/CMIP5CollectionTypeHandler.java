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
 * @version        $version$, Thu, Apr 2, '15
 * @author         Enter your name here...
 */
public class CMIP5CollectionTypeHandler extends ClimateCollectionTypeHandler {

    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public CMIP5CollectionTypeHandler(Repository repository,
                                      Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }

}
