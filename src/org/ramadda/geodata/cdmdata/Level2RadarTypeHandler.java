/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.cdmdata;


import org.ramadda.repository.*;
import org.ramadda.repository.type.*;


import org.ramadda.util.Utils;

import org.w3c.dom.*;

import ucar.nc2.NetcdfFile;
import ucar.nc2.units.DateUnit;

import java.io.File;

import java.util.Date;


/**
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
public class Level2RadarTypeHandler extends RadarTypeHandler {


    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public Level2RadarTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }


}
