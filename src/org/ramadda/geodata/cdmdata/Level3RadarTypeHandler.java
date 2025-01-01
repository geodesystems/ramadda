/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.cdmdata;


import org.ramadda.repository.*;

import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;

import org.ramadda.util.Utils;

import org.ramadda.util.sql.SqlUtil;


import org.w3c.dom.*;


import ucar.nc2.NetcdfFile;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.xml.XmlUtil;


import java.io.File;



import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;


/**
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
public class Level3RadarTypeHandler extends RadarTypeHandler {


    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public Level3RadarTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }





}
