/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.cdmdata;


import org.ramadda.repository.Repository;

import org.w3c.dom.Element;


/**
 * Created with IntelliJ IDEA.
 * User: opendap
 * Date: 5/4/13
 * Time: 9:08 AM
 * To change this template use File | Settings | File Templates.
 */
public class UFRadarTypeHandler extends RadarTypeHandler {


    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public UFRadarTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }


}
