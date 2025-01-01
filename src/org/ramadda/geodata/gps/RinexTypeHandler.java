/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.gps;


import org.ramadda.repository.*;
import org.ramadda.repository.type.*;

import org.w3c.dom.*;

import java.io.File;

import java.util.List;


/**
 *
 *
 * @author Jeff McWhirter
 */
public class RinexTypeHandler extends GpsTypeHandler {

    /**
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     * @throws Exception On badness
     */
    public RinexTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }
}
